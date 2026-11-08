"""Unit tests for the pure text helpers in issue.py (stdlib unittest only, no API calls)."""
import argparse
import io
import os
import sys
import unittest
from contextlib import redirect_stdout
from unittest.mock import patch

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import issue  # noqa: E402
from issue import (  # noqa: E402
    DECISIONS, NOTES, REPO, REPO_URL, DecisionError, add_decision, amend_decision, edit_notes, set_task_items,
)


def map_body(decision_lines):
    """A map body shaped like the real one: Decisions-so-far followed by Not-yet-specified."""
    lines = ["# Map", "", DECISIONS, ""]
    lines += decision_lines
    lines += ["", "## Not yet specified", "", "- something open", "", "---", "", "footer"]
    return "\n".join(lines) + "\n"


def link(ticket):
    return f"{REPO_URL}/{ticket}"


class AddDecisionTest(unittest.TestCase):

    def test_appends_bullet_to_section(self):
        body = map_body([f"- [First]({link(1)}): gist one"])
        result = add_decision(body, 2, "Second", "gist two")

        decisions, _, rest = result.partition("## Not yet specified")
        self.assertIn(f"- [Second]({link(2)}): gist two", decisions)
        self.assertLess(decisions.index(f"({link(1)})"), decisions.index(f"({link(2)})"))
        self.assertIn("- something open", rest)
        self.assertIn("footer", rest)

    def test_inline_link_in_other_bullet_gist_does_not_block(self):
        body = map_body([
            f"- [Namespace check]({link(60)}): settled via [the namespace check]({link(72)}) approach",
        ])
        result = add_decision(body, 72, "Namespace check", "the real decision")

        self.assertIn(f"- [Namespace check]({link(72)}): the real decision", result)

    def test_existing_leading_bullet_for_ticket_raises_and_leaves_body(self):
        body = map_body([f"- [Namespace check]({link(72)}): already decided"])
        original = body

        with self.assertRaises(DecisionError):
            add_decision(body, 72, "Namespace check", "again")

        self.assertEqual(original, body)

    def test_missing_section_raises(self):
        body = "# Map\n\n## Not yet specified\n\n- something open\n"

        with self.assertRaises(DecisionError):
            add_decision(body, 2, "Second", "gist")

    def test_ticket_7_not_blocked_by_bullet_for_72(self):
        body = map_body([f"- [Namespace check]({link(72)}): already decided"])

        result = add_decision(body, 7, "Seven", "gist seven")

        self.assertIn(f"- [Seven]({link(7)}): gist seven", result)


class AmendDecisionTest(unittest.TestCase):

    def test_replaces_existing_line_and_keeps_others(self):
        body = map_body([
            f"- [First]({link(1)}): gist one",
            f"- [Second]({link(2)}): old gist",
            f"- [Third]({link(3)}): gist three",
        ])

        result = amend_decision(body, 2, "Second renamed", "new gist")

        self.assertEqual(
            map_body([
                f"- [First]({link(1)}): gist one",
                f"- [Second renamed]({link(2)}): new gist",
                f"- [Third]({link(3)}): gist three",
            ]),
            result,
        )

    def test_missing_section_raises_and_leaves_body(self):
        body = "# Map\n\n## Not yet specified\n\n- something open\n"
        original = body

        with self.assertRaises(DecisionError):
            amend_decision(body, 2, "Second", "gist")

        self.assertEqual(original, body)

    def test_missing_line_raises_and_leaves_body(self):
        body = map_body([f"- [First]({link(1)}): gist one"])
        original = body

        with self.assertRaises(DecisionError):
            amend_decision(body, 2, "Second", "gist")

        self.assertEqual(original, body)

    def test_duplicate_line_raises_and_leaves_body(self):
        body = map_body([
            f"- [Second]({link(2)}): one",
            f"- [Second]({link(2)}): two",
        ])
        original = body

        with self.assertRaises(DecisionError):
            amend_decision(body, 2, "Second", "gist")

        self.assertEqual(original, body)

    def test_ticket_7_is_not_matched_by_line_for_72(self):
        body = map_body([f"- [Namespace check]({link(72)}): already decided"])

        with self.assertRaises(DecisionError):
            amend_decision(body, 7, "Seven", "gist seven")

    def test_inline_link_in_other_gist_does_not_count_as_match(self):
        body = map_body([
            f"- [Namespace check]({link(60)}): settled via [the namespace check]({link(72)}) approach",
        ])

        with self.assertRaises(DecisionError):
            amend_decision(body, 72, "Namespace check", "gist")

    def test_title_brackets_are_escaped(self):
        body = map_body([f"- [Old]({link(5)}): old"])

        result = amend_decision(body, 5, "Use [brackets]", "new")

        self.assertIn(f"- [Use \\[brackets\\]]({link(5)}): new", result)


def notes_body():
    """A map body with a Notes section holding a nested bullet, followed by Decisions-so-far."""
    lines = ["# Map", "", NOTES, "", "- **Planning only**: decisions first.", "- **Fixed decisions**:",
             "  - Java 11 minimum.", "  - Maven stays.", "- **Sonar is temporarily non-blocking**: token expired.",
             "", DECISIONS, "", "- [First](x): gist", "", "---", "", "footer"]
    return "\n".join(lines) + "\n"


class EditNotesTest(unittest.TestCase):
    def test_removes_bullet_with_its_sub_bullets(self):
        result = edit_notes(notes_body(), remove="**Fixed decisions**")

        self.assertNotIn("Fixed decisions", result)
        self.assertNotIn("Java 11 minimum", result)
        self.assertIn("- **Planning only**: decisions first.", result)
        self.assertIn("- **Sonar is temporarily non-blocking**", result)

    def test_replaces_stale_bullet_and_keeps_following_sections(self):
        result = edit_notes(notes_body(), remove="temporarily non-blocking", add="**Sonar blocks merges**.")

        self.assertNotIn("temporarily non-blocking", result)
        self.assertIn("  - Maven stays.\n- **Sonar blocks merges**.\n\n" + DECISIONS, result)
        self.assertTrue(result.endswith("---\n\nfooter\n"))

    def test_text_matching_only_a_sub_bullet_changes_nothing(self):
        body = notes_body()

        with self.assertRaises(DecisionError):
            edit_notes(body, remove="Maven stays")

    def test_ambiguous_text_changes_nothing(self):
        with self.assertRaises(DecisionError):
            edit_notes(notes_body(), remove="**")


TASKS = "\n".join([
    "## Acceptance criteria",
    "",
    "- [ ] First criterion",
    "- [ ] Second criterion",
    "- [x] Third criterion, already done",
    "- [ ] Fourth criterion",
    "",
    "footer",
]) + "\n"

FENCED = "\n".join([
    "- [ ] Outside one",
    "",
    "```markdown",
    "- [ ] Inside a backtick fence",
    "```",
    "",
    "~~~",
    "- [ ] Inside a tilde fence",
    "~~~",
    "",
    "- [ ] Outside two",
    "",
])


class SetTaskItemsTest(unittest.TestCase):

    def test_ticks_one_item(self):
        result, changed = set_task_items(TASKS, [2])

        self.assertEqual(TASKS.replace("- [ ] Second", "- [x] Second"), result)
        self.assertEqual([(2, "Second criterion")], changed)

    def test_ticks_several_items_and_reports_them_in_index_order(self):
        result, changed = set_task_items(TASKS, [4, 1])

        self.assertEqual(TASKS.replace("- [ ] First", "- [x] First").replace("- [ ] Fourth", "- [x] Fourth"), result)
        self.assertEqual([(1, "First criterion"), (4, "Fourth criterion")], changed)

    def test_no_indices_means_all_items(self):
        result, changed = set_task_items(TASKS)

        self.assertEqual(TASKS.replace("- [ ]", "- [x]"), result)
        self.assertEqual([(1, "First criterion"), (2, "Second criterion"), (4, "Fourth criterion")], changed)

    def test_unticks_one_item(self):
        result, changed = set_task_items(TASKS, [3], ticked=False)

        self.assertEqual(TASKS.replace("- [x] Third", "- [ ] Third"), result)
        self.assertEqual([(3, "Third criterion, already done")], changed)

    def test_unticks_all_items(self):
        ticked = TASKS.replace("- [ ]", "- [x]")

        result, changed = set_task_items(ticked, ticked=False)

        self.assertEqual(TASKS.replace("- [x]", "- [ ]"), result)
        self.assertEqual(4, len(changed))

    def test_out_of_range_index_raises_and_changes_nothing(self):
        for indices in ([5], [0], [-1], [1, 5]):
            with self.subTest(indices=indices), self.assertRaises(ValueError):
                set_task_items(TASKS, indices)

    def test_body_without_task_items_raises_for_an_index_and_is_unchanged_for_all(self):
        with self.assertRaises(ValueError):
            set_task_items("no items here\n", [1])

        self.assertEqual(("no items here\n", []), set_task_items("no items here\n"))

    def test_already_ticked_item_is_unchanged(self):
        result, changed = set_task_items(TASKS, [3])

        self.assertEqual(TASKS, result)
        self.assertEqual([], changed)

    def test_all_when_everything_is_ticked_changes_nothing(self):
        ticked = TASKS.replace("- [ ]", "- [x]")

        result, changed = set_task_items(ticked)

        self.assertEqual(ticked, result)
        self.assertEqual([], changed)

    def test_task_items_inside_fenced_code_are_ignored(self):
        result, changed = set_task_items(FENCED, [2])

        self.assertEqual(FENCED.replace("- [ ] Outside two", "- [x] Outside two"), result)
        self.assertEqual([(2, "Outside two")], changed)

    def test_fenced_items_do_not_count_towards_the_index_range(self):
        with self.assertRaises(ValueError):
            set_task_items(FENCED, [3])

    def test_all_skips_fenced_items(self):
        result, changed = set_task_items(FENCED)

        self.assertEqual(FENCED.replace("- [ ] Outside", "- [x] Outside"), result)
        self.assertEqual([(1, "Outside one"), (2, "Outside two")], changed)

    def test_fence_closes_only_with_the_same_marker_and_at_least_the_same_length(self):
        body = "````\n```\n- [ ] still fenced\n~~~~\n````\n- [ ] after\n"

        result, changed = set_task_items(body, [1])

        self.assertEqual(body.replace("- [ ] after", "- [x] after"), result)
        self.assertEqual([(1, "after")], changed)

    def test_only_column_zero_task_items_count(self):
        body = "- [ ] top\n  - [ ] nested\nText - [ ] mid-line\n"

        with self.assertRaises(ValueError):
            set_task_items(body, [2])

        result, changed = set_task_items(body, [1])
        self.assertEqual(body.replace("- [ ] top", "- [x] top"), result)
        self.assertEqual([(1, "top")], changed)

    def test_keeps_crlf_line_endings(self):
        body = "- [ ] one\r\n- [ ] two\r\n"

        result, changed = set_task_items(body, [2])

        self.assertEqual("- [ ] one\r\n- [x] two\r\n", result)
        self.assertEqual([(2, "two")], changed)

    def test_reported_text_is_cut_to_60_characters(self):
        body = "- [ ] " + "x" * 80 + "\n"

        _, changed = set_task_items(body, [1])

        self.assertEqual([(1, "x" * 60)], changed)


class CheckCommandTest(unittest.TestCase):
    """The `check` handler with the REST helper replaced: which calls it makes and what it prints or exits with."""

    BODY = "- [ ] First\n- [x] Second\n"

    def run_check(self, indices, all_items=False, uncheck=False, body=BODY):
        calls = []

        def fake_gh(*args, check=True):
            calls.append(args)
            return {"body": body}

        options = argparse.Namespace(number=12, indices=indices, all=all_items, uncheck=uncheck)
        output = io.StringIO()
        with patch.object(issue, "gh", side_effect=fake_gh), redirect_stdout(output):
            issue.check(options)
        return calls, output.getvalue()

    def test_patches_the_body_and_prints_each_changed_item(self):
        calls, output = self.run_check([1])

        self.assertEqual((f"{REPO}/issues/12",), calls[0])
        self.assertEqual(("-X", "PATCH", f"{REPO}/issues/12", "-f", "body=- [x] First\n- [x] Second\n"), calls[1])
        self.assertEqual("Ticked item 1 of #12: First\n", output)

    def test_uncheck_prints_unticked(self):
        calls, output = self.run_check([2], uncheck=True)

        self.assertEqual("Unticked item 2 of #12: Second\n", output)
        self.assertEqual("body=- [ ] First\n- [ ] Second\n", calls[1][-1])

    def test_no_patch_when_nothing_changed(self):
        calls, output = self.run_check([2])

        self.assertEqual([(f"{REPO}/issues/12",)], calls)
        self.assertIn("Nothing to change", output)

    def test_out_of_range_exits_with_an_error_and_patches_nothing(self):
        with self.assertRaises(SystemExit) as caught:
            self.run_check([3])

        self.assertIsInstance(caught.exception.code, str)
        self.assertIn("out of range", caught.exception.code)

    def test_index_and_all_together_are_an_error(self):
        with self.assertRaises(SystemExit):
            self.run_check([1], all_items=True)

    def test_neither_index_nor_all_is_an_error(self):
        with self.assertRaises(SystemExit):
            self.run_check([])


if __name__ == "__main__":
    unittest.main()
