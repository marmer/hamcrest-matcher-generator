"""Black-box tests for scripts/agents/ralph-iteration.py (stdlib unittest only).

The script runs as a child process; a stub `gh` first on PATH records its arguments in `gh.log` (one call per line)
and fails with the message in `gh.fail` when that file exists.
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
SCRIPT = HERE / "ralph-iteration.py"

STUB_GH = """#!/usr/bin/env bash
echo "$*" >> "$STUB_DIR/gh.log"
if [ -e "$STUB_DIR/gh.hang" ]; then sleep 30; fi
if [ -e "$STUB_DIR/gh.fail" ]; then cat "$STUB_DIR/gh.fail" >&2; exit 1; fi
echo '{}'
"""


class RalphIterationTest(unittest.TestCase):

    def setUp(self):
        self.root = Path(tempfile.mkdtemp(prefix="ralph-iteration-test-"))
        self.addCleanup(shutil.rmtree, self.root, True)
        self.run_dir = self.root / "run"
        self.run_dir.mkdir()
        bin_dir = self.root / "bin"
        bin_dir.mkdir()
        gh = bin_dir / "gh"
        gh.write_text(STUB_GH, encoding="utf-8")
        gh.chmod(0o755)
        self.path = os.pathsep.join([str(bin_dir), str(Path(shutil.which("bash")).parent)])
        self.gh_log = self.root / "gh.log"

    def run_script(self, *args, path=None, **extra):
        env = {"PATH": path or self.path, "STUB_DIR": str(self.root), "HOME": str(self.root), **extra}
        return subprocess.run([sys.executable, str(SCRIPT), *args], capture_output=True, text=True, env=env,
                              timeout=30)

    def gh_calls(self):
        return self.gh_log.read_text(encoding="utf-8").splitlines() if self.gh_log.exists() else []

    def written(self, iteration):
        return json.loads((self.run_dir / f"iteration-{iteration}.json").read_text(encoding="utf-8"))

    def test_data_writes_the_tickets_and_prs_of_the_iteration_into_the_run_directory(self):
        done = self.run_script("data", str(self.run_dir), "3", "--tickets", "444", "445", "--prs", "460")

        self.assertEqual(0, done.returncode, done.stderr)
        self.assertEqual({"tickets": [444, 445], "prs": [460]}, self.written(3))

    def test_data_without_tickets_and_prs_writes_two_empty_lists(self):
        done = self.run_script("data", str(self.run_dir), "1")

        self.assertEqual(0, done.returncode, done.stderr)
        self.assertEqual({"tickets": [], "prs": []}, self.written(1))

    def test_data_replaces_the_file_of_an_earlier_attempt(self):
        self.run_script("data", str(self.run_dir), "2", "--tickets", "1")

        self.run_script("data", str(self.run_dir), "2", "--tickets", "9")

        self.assertEqual({"tickets": [9], "prs": []}, self.written(2))

    def test_data_refuses_a_ticket_that_is_not_a_positive_number(self):
        done = self.run_script("data", str(self.run_dir), "1", "--tickets", "0")

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR", done.stdout + done.stderr)
        self.assertFalse((self.run_dir / "iteration-1.json").exists())

    def test_data_refuses_an_iteration_that_is_not_a_positive_number(self):
        done = self.run_script("data", str(self.run_dir), "x")

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR", done.stdout + done.stderr)

    def test_data_refuses_a_run_directory_that_does_not_exist(self):
        done = self.run_script("data", str(self.root / "missing"), "1")

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR", done.stdout + done.stderr)
        self.assertFalse((self.root / "missing").exists())

    def test_auto_merge_enables_squash_auto_merge_through_the_rest_route_of_the_pull_request(self):
        done = self.run_script("auto-merge", "460")

        self.assertEqual(0, done.returncode, done.stderr)
        self.assertEqual(["api -X PUT repos/marmer/hamcrest-matcher-generator/pulls/460/ccr/auto_merge "
                          "-f merge_method=squash"], self.gh_calls())

    def test_auto_merge_calls_exactly_the_auto_merge_route_of_the_pull_request(self):
        self.run_script("auto-merge", "460")

        (call,) = self.gh_calls()
        self.assertEqual("repos/marmer/hamcrest-matcher-generator/pulls/460/ccr/auto_merge", call.split()[3], call)

    def test_failing_auto_merge_is_an_error_line_with_the_reason_and_no_hand_merge(self):
        (self.root / "gh.fail").write_text("HTTP 403 nope", encoding="utf-8")

        done = self.run_script("auto-merge", "460")

        self.assertEqual(1, done.returncode)
        self.assertIn("ERROR auto-merge for #460 failed:", done.stdout)
        self.assertIn("HTTP 403 nope", done.stdout)
        self.assertEqual(1, len(self.gh_calls()), "a fallback call was made")

    def test_hanging_gh_is_an_error_line_after_the_timeout_not_a_traceback(self):
        (self.root / "gh.hang").write_text("", encoding="utf-8")

        done = self.run_script("auto-merge", "460", RALPH_GH_TIMEOUT_SECONDS="1")

        self.assertEqual(1, done.returncode, done.stderr)
        self.assertIn("ERROR auto-merge for #460 failed:", done.stdout)
        self.assertNotIn("Traceback", done.stderr)

    def test_missing_gh_is_an_error_line_not_a_traceback(self):
        done = self.run_script("auto-merge", "460", path=str(self.root / "empty"))

        self.assertEqual(1, done.returncode, done.stderr)
        self.assertIn("ERROR auto-merge for #460 failed:", done.stdout)
        self.assertNotIn("Traceback", done.stderr)

    def test_wrong_gh_timeout_knob_is_refused_before_gh_is_called(self):
        done = self.run_script("auto-merge", "460", RALPH_GH_TIMEOUT_SECONDS="soon")

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR RALPH_GH_TIMEOUT_SECONDS", done.stdout)
        self.assertEqual([], self.gh_calls())

    def test_auto_merge_refuses_a_pull_request_number_that_is_not_a_positive_number(self):
        done = self.run_script("auto-merge", "abc")

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR", done.stdout + done.stderr)
        self.assertEqual([], self.gh_calls())


if __name__ == "__main__":
    unittest.main()
