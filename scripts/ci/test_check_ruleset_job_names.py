"""Unit tests for the pure helpers in check-ruleset-job-names.py (stdlib unittest only, no API calls)."""
import importlib.util
import os
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("check_ruleset_job_names", HERE / "check-ruleset-job-names.py")
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)


def status_rule(*contexts):
    return {"type": "required_status_checks",
            "parameters": {"required_status_checks": [{"context": c} for c in contexts]}}


class MissingJobsTest(unittest.TestCase):

    def test_all_required_contexts_are_jobs(self):
        rules = [status_rule("Build", "Verify JDK 17")]
        self.assertEqual([], check.missing_jobs(rules, {"Build", "Verify JDK 17", "Other"}))

    def test_context_of_a_removed_job_is_reported(self):
        rules = [status_rule("Build", "Verify JDK 11")]
        self.assertEqual(["Verify JDK 11"], check.missing_jobs(rules, {"Build", "Verify JDK 17"}))

    def test_other_rule_types_and_missing_parameters_are_ignored(self):
        rules = [{"type": "deletion"}, {"type": "required_status_checks"}, {"type": "pull_request", "parameters": {}}]
        self.assertEqual([], check.missing_jobs(rules, set()))

    def test_contexts_of_several_rules_are_merged_without_duplicates(self):
        rules = [status_rule("A", "B"), status_rule("B", "C")]
        self.assertEqual(["A", "B", "C"], check.required_contexts(rules))

    def test_matrix_job_names_of_the_repository_are_expanded(self):
        names = check.workflow_job_names(HERE.parents[1])
        self.assertIn("Verify JDK 17", names)
        self.assertIn("Verify JDK latest", names)
        self.assertNotIn("Verify JDK ${{ matrix.role }}", names)


if __name__ == "__main__":
    unittest.main()
