"""Unit tests for scanoss-summary.py (stdlib unittest only, no network)."""
import contextlib
import importlib.util
import io
import os
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("scanoss_summary", HERE / "scanoss-summary.py")
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)


def match(component, license_name, file_name):
    return {"id": "snippet", "component": component, "file": file_name,
            "licenses": [{"name": license_name}]}


class SummarizeTest(unittest.TestCase):

    def test_files_without_matches_are_counted_as_unmatched(self):
        results = {"A.java": [{"id": "none"}], "B.java": [{"id": "none"}]}
        self.assertIn("Matched 0 of 2 scanned files", summary.summarize(results))

    def test_matched_files_are_counted_once_even_with_several_matches(self):
        results = {"A.java": [match("lib", "MIT", "A.java"), match("other", "MIT", "A.java")],
                   "B.java": [{"id": "none"}]}
        self.assertIn("Matched 1 of 2 scanned files", summary.summarize(results))

    def test_matches_are_grouped_by_component_and_license_with_their_files(self):
        results = {"A.java": [match("lib", "Apache-2.0", "A.java")],
                   "B.java": [match("lib", "Apache-2.0", "B.java")]}
        text = summary.summarize(results)
        self.assertIn("| lib | Apache-2.0 | 2 |", text)

    def test_missing_license_is_reported_as_undeclared(self):
        results = {"A.java": [{"id": "file", "component": "lib", "file": "A.java"}]}
        self.assertIn("| lib | undeclared | 1 |", summary.summarize(results))

    def test_no_results_say_the_scan_produced_nothing(self):
        self.assertIn("did not produce results", summary.summarize(None))

    def test_summary_is_markdown_with_a_heading(self):
        self.assertTrue(summary.summarize({}).startswith("## SCANOSS"))


class MainTest(unittest.TestCase):

    def test_missing_results_file_is_reported_and_does_not_fail_the_step(self):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            status = summary.main(["/nonexistent/scanoss-results.json"])
        self.assertEqual(0, status)
        self.assertIn("did not produce results", out.getvalue())

    def test_results_file_is_summarised_and_does_not_fail_the_step(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "results.json"
            path.write_text('{"A.java": [{"id": "none"}]}', encoding="utf-8")
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                status = summary.main([str(path)])
        self.assertEqual(0, status)
        self.assertIn("Matched 0 of 1 scanned files", out.getvalue())


if __name__ == "__main__":
    unittest.main()
