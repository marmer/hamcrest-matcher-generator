"""Unit tests for the pure text helpers in issue.py (stdlib unittest only, no API calls)."""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import issue  # noqa: E402
from issue import DECISIONS, REPO_URL, DecisionError, add_decision  # noqa: E402


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


if __name__ == "__main__":
    unittest.main()
