"""Unit tests for check-pr-title-ticket.py (stdlib unittest only; the title and author type come in as environment variables)."""
import importlib.util
import io
import os
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_pr_title_ticket", HERE / "check-pr-title-ticket.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

WORKFLOW = HERE.parents[1] / ".github" / "workflows" / "pr-title.yml"
EXAMPLE = "docs: add the badge (#284)"
TITLE_EXPRESSION = "${{ github.event.pull_request.title }}"


def run_main(title, author_type="User"):
    out = io.StringIO()
    code = check.main(environ={"PR_TITLE": title, "PR_AUTHOR_TYPE": author_type}, out=out)
    return code, out.getvalue().splitlines()


class TicketNumberTest(unittest.TestCase):
    def test_a_title_with_a_ticket_number_passes(self):
        self.assertIsNone(check.problem(EXAMPLE, "User"))

    def test_a_title_without_a_ticket_number_fails_and_names_the_example(self):
        problem = check.problem("docs: add the badge", "User")

        self.assertIsNotNone(problem)
        self.assertIn(EXAMPLE, problem)

    def test_several_ticket_numbers_pass(self):
        self.assertIsNone(check.problem("fix: handle nulls (#12) and empty lists (#13)", "User"))

    def test_a_ticket_number_in_the_middle_of_the_title_passes(self):
        self.assertIsNone(check.problem("fix: #284 handles nulls in the matcher", "User"))

    def test_digits_without_a_hash_are_not_a_ticket_number(self):
        self.assertIsNotNone(check.problem("fix: upgrade to 6 for issue 284", "User"))

    def test_a_hash_without_digits_is_not_a_ticket_number(self):
        self.assertIsNotNone(check.problem("fix: handle the # sign (#)", "User"))

    def test_a_hash_glued_to_letters_is_not_a_ticket_number(self):
        self.assertIsNotNone(check.problem("fix: handle issue#284abc", "User"))

    def test_an_empty_title_fails_for_a_human(self):
        self.assertIsNotNone(check.problem("", "User"))

    def test_a_missing_title_and_author_type_fail_for_a_human(self):
        self.assertIsNotNone(check.problem("", ""))


class BotExemptionTest(unittest.TestCase):
    def test_a_bot_passes_without_a_ticket_number(self):
        self.assertIsNone(check.problem("chore(deps): bump junit to 6.0.0", "Bot"))

    def test_the_release_please_title_passes_for_a_bot(self):
        self.assertIsNone(check.problem("chore(main): release 6.0.0", "Bot"))

    def test_a_bot_with_an_empty_title_passes(self):
        self.assertIsNone(check.problem("", "Bot"))

    def test_the_author_type_must_be_exactly_bot(self):
        self.assertIsNotNone(check.problem("chore(deps): bump junit to 6.0.0", "bot"))


class ReleaseStyleTitleTest(unittest.TestCase):
    def test_a_human_pr_with_a_release_style_title_still_fails(self):
        problem = check.problem("chore(main): release 6.0.0", "User")

        self.assertIsNotNone(problem)
        self.assertIn(EXAMPLE, problem)


class MainTest(unittest.TestCase):
    def test_a_title_with_a_ticket_number_exits_zero_and_says_so(self):
        code, lines = run_main(EXAMPLE)

        self.assertEqual(0, code)
        self.assertEqual(1, len(lines))
        self.assertFalse(lines[0].startswith("::error::"))

    def test_a_title_without_a_ticket_number_exits_one_with_an_error_annotation(self):
        code, lines = run_main("docs: add the badge")

        self.assertEqual(1, code)
        self.assertEqual(1, len(lines))
        self.assertTrue(lines[0].startswith("::error::"))
        self.assertIn("docs: add the badge", lines[0])
        self.assertIn(EXAMPLE, lines[0])

    def test_a_bot_pr_without_a_ticket_number_exits_zero(self):
        code, _ = run_main("chore(main): release 6.0.0", "Bot")

        self.assertEqual(0, code)

    def test_missing_environment_variables_count_as_a_human_pr_without_a_title(self):
        out = io.StringIO()

        code = check.main(environ={}, out=out)

        self.assertEqual(1, code)
        self.assertTrue(out.getvalue().startswith("::error::"))


class WorkflowTest(unittest.TestCase):
    """The title reaches the script only through `env:`, never inside a shell line."""

    def setUp(self):
        self.lines = WORKFLOW.read_text(encoding="utf-8").splitlines()

    def test_the_title_is_passed_through_env_and_never_interpolated_into_a_run_line(self):
        uses = [line for line in self.lines if TITLE_EXPRESSION in line]

        self.assertEqual([f"PR_TITLE: {TITLE_EXPRESSION}"], [line.strip() for line in uses])

    def test_the_author_type_is_passed_through_env(self):
        self.assertIn("PR_AUTHOR_TYPE: ${{ github.event.pull_request.user.type }}",
                      [line.strip() for line in self.lines])

    def test_the_workflow_runs_the_ticket_check_script(self):
        self.assertTrue(any("python3 scripts/ci/check-pr-title-ticket.py" in line for line in self.lines))

    def test_the_checkout_keeps_no_credentials_and_has_no_ref(self):
        checkout = next(index for index, line in enumerate(self.lines) if "actions/checkout@" in line)
        block = self.lines[checkout:checkout + 4]

        self.assertIn("persist-credentials: false", [line.strip() for line in block])
        self.assertFalse(any(line.strip().startswith("ref:") for line in self.lines))


if __name__ == "__main__":
    unittest.main()
