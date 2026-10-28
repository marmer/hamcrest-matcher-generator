"""Unit tests for scripts/ci/pit-scores.py (stdlib unittest only, no Maven run)."""
import importlib.util
import os
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("pit_scores", HERE / "pit-scores.py")
pit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pit)

MUTATIONS = """<?xml version="1.0" encoding="UTF-8"?>
<mutations>
  <mutation detected='true' status='KILLED' numberOfTestsRun='2'><sourceFile>A.java</sourceFile></mutation>
  <mutation detected='true' status='KILLED' numberOfTestsRun='1'><sourceFile>A.java</sourceFile></mutation>
  <mutation detected='true' status='TIMED_OUT' numberOfTestsRun='3'><sourceFile>A.java</sourceFile></mutation>
  <mutation detected='false' status='SURVIVED' numberOfTestsRun='1'><sourceFile>B.java</sourceFile></mutation>
  <mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'><sourceFile>B.java</sourceFile></mutation>
</mutations>
"""


class CountTest(unittest.TestCase):

    def test_detected_and_total_come_from_every_mutation_element(self):
        self.assertEqual((3, 5), pit.mutation_counts(MUTATIONS))

    def test_empty_report_has_no_mutations(self):
        self.assertEqual((0, 0), pit.mutation_counts("<mutations/>"))


class ScoreTest(unittest.TestCase):

    def test_score_is_detected_over_total_in_percent(self):
        self.assertEqual(50.0, pit.score_percent(2, 4))

    def test_score_without_mutations_is_none(self):
        self.assertIsNone(pit.score_percent(0, 0))


class ModulesTest(unittest.TestCase):

    def write_report(self, root, module, text):
        report = root / module / "target" / "pit-reports"
        report.mkdir(parents=True)
        (report / "mutations.xml").write_text(text, encoding="utf-8")

    def test_every_module_with_a_report_is_measured_and_sorted(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.write_report(root, "zeta", MUTATIONS)
            self.write_report(root, "alpha", "<mutations/>")
            self.assertEqual({"alpha": (0, 0), "zeta": (3, 5)}, pit.measured_modules(root))

    def test_module_without_report_is_not_in_the_result(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "silent" / "target").mkdir(parents=True)
            self.assertEqual({}, pit.measured_modules(root))


class MarkdownTest(unittest.TestCase):

    def test_table_names_each_module_with_its_score_and_verdict(self):
        table = pit.markdown_table({"alpha": (9, 10), "zeta": (2, 4)}, threshold=90)
        self.assertIn("| `alpha` | 9 / 10 | 90.0 % | pass |", table)
        self.assertIn("| `zeta` | 2 / 4 | 50.0 % | below 90 |", table)

    def test_module_without_mutations_is_shown_not_as_a_pass(self):
        table = pit.markdown_table({"alpha": (0, 0)}, threshold=90)
        self.assertIn("| `alpha` | 0 / 0 | - | no mutations |", table)


class FailuresTest(unittest.TestCase):

    def test_below_threshold_and_missing_modules_are_failures(self):
        scores = {"alpha": (9, 10), "zeta": (2, 4)}
        self.assertEqual(["zeta"], pit.failing_modules(scores, ["alpha", "zeta"], threshold=90))

    def test_module_expected_but_not_measured_is_a_failure(self):
        self.assertEqual(["gamma"], pit.failing_modules({}, ["gamma"], threshold=90))


if __name__ == "__main__":
    unittest.main()
