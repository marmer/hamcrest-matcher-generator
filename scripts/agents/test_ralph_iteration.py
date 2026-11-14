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
if [ -e "$STUB_DIR/gh.body" ]; then cat "$STUB_DIR/gh.body"; else echo '{}'; fi
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

    def run_script(self, *args, path=None, seconds=30, **extra):
        env = {"PATH": path or self.path, "STUB_DIR": str(self.root), "HOME": str(self.root), **extra}
        return subprocess.run([sys.executable, str(SCRIPT), *args], capture_output=True, text=True, env=env,
                              timeout=seconds)

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

        try:
            done = self.run_script("auto-merge", "460", seconds=10, RALPH_GH_TIMEOUT_SECONDS="1")
        except subprocess.TimeoutExpired:
            self.fail("ralph-iteration.py did not stop at RALPH_GH_TIMEOUT_SECONDS=1, still running after 10 s")

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

    def test_successful_call_with_an_empty_or_non_json_body_counts_as_success(self):
        for body in ("", "Accepted"):
            with self.subTest(body=body):
                (self.root / "gh.body").write_text(body, encoding="utf-8")

                done = self.run_script("auto-merge", "460")

                self.assertEqual(0, done.returncode, done.stderr)
                self.assertIn("auto-merge (squash) enabled for #460", done.stdout)

    def test_auto_merge_refuses_a_pull_request_number_that_is_not_a_positive_number(self):
        done = self.run_script("auto-merge", "abc")

        self.assertEqual(2, done.returncode)
        self.assertIn("ERROR", done.stdout + done.stderr)
        self.assertEqual([], self.gh_calls())


class DeleteBranchTest(unittest.TestCase):
    """`delete-branch <name>`: the one way a loop child deletes a branch on the remote (the loop denies the hand routes)."""

    MERGED = json.dumps([{"state": "closed", "merged_at": "2026-11-14T10:00:00Z"}])

    def setUp(self):
        self.root = Path(tempfile.mkdtemp(prefix="ralph-delete-branch-test-"))
        self.addCleanup(shutil.rmtree, self.root, True)
        bin_dir = self.root / "bin"
        bin_dir.mkdir()
        gh = bin_dir / "gh"
        gh.write_text(STUB_GH, encoding="utf-8")
        gh.chmod(0o755)
        self.path = os.pathsep.join([str(bin_dir), str(Path(shutil.which("git")).parent),
                                     str(Path(shutil.which("bash")).parent)])
        self.gh_log = self.root / "gh.log"
        self.origin = self.root / "origin.git"
        self.work = self.root / "work"
        self.git(self.root, "init", "-q", "--bare", "-b", "main", str(self.origin))
        self.git(self.root, "clone", "-q", str(self.origin), str(self.work))
        self.git(self.work, "-c", "user.email=a@b", "-c", "user.name=x", "commit", "-q", "--allow-empty", "-m", "init")
        self.git(self.work, "push", "-q", "origin", "main")
        for name in ("ticket-5", "session-end-2026-11-14", "retro-443-2026-11-13", "review-445-2", "feature-x"):
            self.git(self.work, "branch", name)
            self.git(self.work, "push", "-q", "origin", name)

    def git(self, cwd, *args):
        return subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True, check=True).stdout

    def remote_branches(self):
        return sorted(line.split("refs/heads/")[1] for line in self.git(self.work, "ls-remote", "--heads", "origin").splitlines())

    def local_branches(self):
        return self.git(self.work, "branch", "--format=%(refname:short)").split()

    def delete(self, name, body=None, **files):
        if body is not None:
            (self.root / "gh.body").write_text(body, encoding="utf-8")
        for file in files:
            (self.root / file.replace("_", ".")).write_text(files[file], encoding="utf-8")
        env = {"PATH": self.path, "STUB_DIR": str(self.root), "HOME": str(self.root)}
        return subprocess.run([sys.executable, str(SCRIPT), "delete-branch", name], capture_output=True, text=True,
                              env=env, cwd=self.work, timeout=30)

    def test_deletes_a_branch_of_each_allowed_family_locally_and_on_the_remote_when_its_pr_is_merged(self):
        for name in ("ticket-5", "session-end-2026-11-14", "retro-443-2026-11-13", "review-445-2"):
            with self.subTest(name=name):
                done = self.delete(name, self.MERGED)

                self.assertEqual(0, done.returncode, done.stdout + done.stderr)
                self.assertNotIn(name, self.remote_branches())
                self.assertNotIn(name, self.local_branches())
                self.assertIn(f"deleted branch {name}", done.stdout)

    def test_asks_for_the_pull_requests_of_exactly_that_head_branch(self):
        self.delete("ticket-5", self.MERGED)

        self.assertEqual(["api repos/marmer/hamcrest-matcher-generator/pulls?state=all&head=marmer:ticket-5&per_page=100"],
                         (self.root / "gh.log").read_text(encoding="utf-8").splitlines())

    def test_refuses_main_and_every_name_outside_the_allowed_families_without_a_call_or_a_deletion(self):
        before = self.remote_branches()
        for name in ("main", "master", "feature-x", "ticket-", "ticket-x", "ticket-5x", "ticket-5/../main",
                     "retro", "review", "origin/main", "HEAD", "-D", "--all", "ticket-5 main", "refs/heads/ticket-5"):
            with self.subTest(name=name):
                done = self.delete(name, self.MERGED)

                self.assertEqual(2, done.returncode, done.stdout + done.stderr)
                self.assertIn("error", (done.stdout + done.stderr).lower())
                self.assertEqual(before, self.remote_branches())
                self.assertIn("main", self.local_branches())
                self.assertFalse(self.gh_log.exists(), "gh was called for a refused name")

    def test_refuses_a_branch_without_a_merged_pull_request_and_deletes_nothing(self):
        for body in ("[]", json.dumps([{"state": "closed", "merged_at": None}])):
            with self.subTest(body=body):
                done = self.delete("ticket-5", body)

                self.assertEqual(1, done.returncode, done.stdout + done.stderr)
                self.assertIn("ERROR branch ticket-5 has no merged pull request", done.stdout)
                self.assertIn("ticket-5", self.remote_branches())
                self.assertIn("ticket-5", self.local_branches())

    def test_refuses_a_branch_that_still_has_an_open_pull_request(self):
        done = self.delete("ticket-5", json.dumps([{"state": "closed", "merged_at": "2026-11-14T10:00:00Z"},
                                                   {"state": "open", "merged_at": None}]))

        self.assertEqual(1, done.returncode, done.stdout + done.stderr)
        self.assertIn("open pull request", done.stdout)
        self.assertIn("ticket-5", self.remote_branches())

    def test_a_failing_gh_is_an_error_line_and_deletes_nothing(self):
        done = self.delete("ticket-5", gh_fail="HTTP 500")

        self.assertEqual(1, done.returncode, done.stdout + done.stderr)
        self.assertIn("ERROR", done.stdout)
        self.assertNotIn("Traceback", done.stderr)
        self.assertIn("ticket-5", self.remote_branches())

    def test_a_branch_that_is_already_gone_is_not_an_error(self):
        self.git(self.work, "push", "-q", "origin", "--delete", "ticket-5")
        self.git(self.work, "branch", "-D", "ticket-5")

        done = self.delete("ticket-5", self.MERGED)

        self.assertEqual(0, done.returncode, done.stdout + done.stderr)
        self.assertIn("branch ticket-5 is already gone", done.stdout)

    def test_the_checked_out_branch_is_not_deleted_and_the_remote_branch_stays(self):
        self.git(self.work, "checkout", "-q", "ticket-5")

        done = self.delete("ticket-5", self.MERGED)

        self.assertEqual(1, done.returncode, done.stdout + done.stderr)
        self.assertIn("ERROR", done.stdout)
        self.assertIn("ticket-5", self.remote_branches())


if __name__ == "__main__":
    unittest.main()
