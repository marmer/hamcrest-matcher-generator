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

    def run_script(self, *args):
        env = {"PATH": self.path, "STUB_DIR": str(self.root), "HOME": str(self.root)}
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

    def test_control_prints_the_control_file_of_the_run_directory(self):
        (self.run_dir / "control.json").write_text('{"max_parallel": 2, "stop": null}\n', encoding="utf-8")

        done = self.run_script("control", str(self.run_dir))

        self.assertEqual(0, done.returncode, done.stderr)
        self.assertEqual({"max_parallel": 2, "stop": None}, json.loads(done.stdout))

    def test_control_without_a_control_file_is_an_error(self):
        done = self.run_script("control", str(self.run_dir))

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR", done.stdout + done.stderr)

    def test_auto_merge_enables_squash_auto_merge_through_the_rest_route_of_the_pull_request(self):
        done = self.run_script("auto-merge", "460")

        self.assertEqual(0, done.returncode, done.stderr)
        self.assertEqual(["api -X PUT repos/marmer/hamcrest-matcher-generator/pulls/460/ccr/auto_merge "
                          "-f merge_method=squash"], self.gh_calls())

    def test_auto_merge_never_calls_the_merge_endpoint_or_graphql(self):
        self.run_script("auto-merge", "460")

        for call in self.gh_calls():
            self.assertNotIn("/merge", call.replace("/ccr/auto_merge", ""))
            self.assertNotIn("graphql", call)

    def test_failing_auto_merge_is_an_error_line_with_the_reason_and_no_hand_merge(self):
        (self.root / "gh.fail").write_text("HTTP 403 nope", encoding="utf-8")

        done = self.run_script("auto-merge", "460")

        self.assertEqual(1, done.returncode)
        self.assertIn("ERROR auto-merge for #460 failed: HTTP 403 nope", done.stdout + done.stderr)
        self.assertEqual(1, len(self.gh_calls()), "a fallback call was made")

    def test_auto_merge_refuses_a_pull_request_number_that_is_not_a_positive_number(self):
        done = self.run_script("auto-merge", "abc")

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR", done.stdout + done.stderr)
        self.assertEqual([], self.gh_calls())


if __name__ == "__main__":
    unittest.main()
