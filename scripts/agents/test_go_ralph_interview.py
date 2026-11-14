"""Black-box tests for .claude/skills/goRalph/scripts/interview.py (stdlib unittest only).

The script runs with a stub `gh` first on PATH. The stub serves one JSON fixture per endpoint from STUB_GH_DIR:
sub_issues.json (the spec's sub-issues), retro-human.json (issues labelled retro and ready-for-human),
retro-agent.json (retro and ready-for-agent) and issue-<n>.json (one issue); each list is a list of pages and is
served page by page for `page=N` (`[]` beyond the last one); like the cloud proxy the stub refuses `--paginate`. A missing fixture makes the stub fail like a broken tracker
call; every call is appended to calls.log there.
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
SCRIPT = HERE.parents[1] / ".claude" / "skills" / "goRalph" / "scripts" / "interview.py"

STUB_GH = """#!/usr/bin/env bash
echo "$*" >> "$STUB_GH_DIR/calls.log"
case "$*" in
  *-X*sub_issues*) echo '{}'; exit 0 ;;
  *sub_issues*) f=sub_issues.json ;;
  *labels=retro,ready-for-human*) f=retro-human.json ;;
  *labels=retro,ready-for-agent*) f=retro-agent.json ;;
  *issues/[0-9]*) a="$*"; n="${a##*issues/}"; f="issue-${n%%[^0-9]*}.json" ;;
  *) f=unknown ;;
esac
[ -f "$STUB_GH_DIR/$f" ] || { echo "stub gh: no fixture $f for $*" >&2; exit 1; }
case "$*" in *--paginate*) echo "stub gh: --paginate follows repositories/{id} links, refused like the proxy" >&2; exit 1 ;; esac
case "${@: -1}" in
  *"&page="*) page="${@: -1}"; page="${page##*&page=}"; page="${page%%&*}"
           jq -c --argjson n "$page" '.[$n - 1] // []' "$STUB_GH_DIR/$f" ;;
  *) jq -c 'if type == "array" then .[0] else . end' "$STUB_GH_DIR/$f" ;;
esac
"""


def issue(number, *labels, state="open", **extra):
    return dict({"number": number, "title": f"Title {number}", "state": state, "html_url": f"u{number}",
                 "labels": [{"name": name} for name in labels]}, **extra)


class InterviewTest(unittest.TestCase):

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        root = Path(self.directory.name)
        self.gh_dir = root / "fixtures"
        self.gh_dir.mkdir()
        stub = root / "bin" / "gh"
        stub.parent.mkdir()
        stub.write_text(STUB_GH, encoding="utf-8")
        stub.chmod(0o755)
        self.ralph_home = root / "ralph-home"
        self.path = f"{stub.parent}:{os.environ['PATH']}"
        for name in ("sub_issues", "retro-human", "retro-agent"):
            self.fixture(name, [])

    def fixture(self, name, data):
        self.pages(name, [data])

    def pages(self, name, pages):
        (self.gh_dir / f"{name}.json").write_text(json.dumps(pages), encoding="utf-8")

    def run_script(self, *args, **extra_env):
        env = dict(os.environ, PATH=self.path, STUB_GH_DIR=str(self.gh_dir), RALPH_HOME=str(self.ralph_home),
                   **extra_env)
        return subprocess.run([sys.executable, str(SCRIPT), *args], env=env, capture_output=True, text=True,
                              timeout=60)

    def listing(self):
        done = self.run_script("list", "42")
        self.assertEqual(0, done.returncode, done.stderr)
        return json.loads(done.stdout)

    def test_nothing_open_means_no_interview(self):
        self.assertEqual({"spec": 42, "interview": False, "needs_info": [], "retro": [], "carry_over": [],
                          "unfinished_run": False}, self.listing())

    def make_run(self, name, ended=False):
        run = self.ralph_home / "runs" / name
        run.mkdir(parents=True)
        if ended:
            (run / "ended").write_text("", encoding="utf-8")

    def test_a_run_directory_of_the_spec_without_an_ended_marker_is_an_unfinished_run(self):
        self.make_run("42-20260101-000000", ended=True)
        self.make_run("42-20260102-000000")
        self.assertTrue(self.listing()["unfinished_run"], "an unfinished run of #42 was not reported")

    def test_finished_runs_and_runs_of_other_specs_are_no_unfinished_run(self):
        self.make_run("42-20260101-000000", ended=True)
        self.make_run("420-20260102-000000")
        self.make_run("4-20260102-000000")
        self.assertFalse(self.listing()["unfinished_run"], "a run of another spec or a finished run counted")

    def test_open_needs_info_tickets_of_the_spec_are_listed(self):
        self.fixture("sub_issues", [issue(7, "needs-info"), issue(8, "ready-for-agent"),
                                    issue(9, "needs-info", state="closed")])
        data = self.listing()
        self.assertEqual([{"number": 7, "title": "Title 7", "url": "u7"}], data["needs_info"])
        self.assertTrue(data["interview"])

    def test_open_retro_tickets_waiting_for_the_owner_are_listed_and_pull_requests_are_not(self):
        self.fixture("retro-human", [issue(50, "retro", "ready-for-human"),
                                     issue(51, "retro", "ready-for-human", pull_request={}),
                                     issue(52, "retro", "ready-for-human", state="closed")])
        data = self.listing()
        self.assertEqual([{"number": 50, "title": "Title 50", "url": "u50"}], data["retro"])
        self.assertTrue(data["interview"])

    def test_retro_tickets_accepted_earlier_and_not_yet_in_the_spec_are_carried_over_without_an_interview(self):
        self.fixture("sub_issues", [issue(60, "retro", "ready-for-agent")])
        self.fixture("retro-agent", [issue(60, "retro", "ready-for-agent"), issue(61, "retro", "ready-for-agent")])
        data = self.listing()
        self.assertEqual([{"number": 61, "title": "Title 61", "url": "u61"}], data["carry_over"])
        self.assertFalse(data["interview"])

    def test_reads_every_page_of_a_list_longer_than_one_page(self):
        self.pages("retro-human", [[issue(n, "retro", "ready-for-human") for n in range(100, 200)],
                                   [issue(n, "retro", "ready-for-human") for n in range(200, 300)],
                                   [issue(n, "retro", "ready-for-human") for n in range(300, 350)]])
        self.assertEqual(list(range(100, 350)), [t["number"] for t in self.listing()["retro"]])

    def test_a_list_still_full_at_the_page_limit_is_an_error_naming_the_endpoint(self):
        full = [issue(n, "retro", "ready-for-human") for n in range(100)]
        self.pages("retro-human", [full] * 3)
        done = self.run_script("list", "42", RALPH_GH_MAX_PAGES="3")
        self.assertEqual(1, done.returncode, done.stderr)
        self.assertIn("labels=retro,ready-for-human", done.stderr)
        self.assertIn("RALPH_GH_MAX_PAGES", done.stderr)

    def test_a_retro_ticket_that_already_has_a_parent_is_not_carried_over(self):
        self.fixture("retro-agent", [issue(61, "retro", "ready-for-agent",
                                           parent_issue_url="https://api.example.test/repos/x/issues/30"),
                                     issue(62, "retro", "ready-for-agent")])
        self.assertEqual([62], [t["number"] for t in self.listing()["carry_over"]])

    def test_a_ticket_that_cannot_be_attached_is_reported_and_the_others_are_still_attached(self):
        self.fixture("issue-62", {"id": 9062, "number": 62})
        done = self.run_script("attach", "42", "61", "62")
        self.assertEqual(1, done.returncode)
        self.assertIn("interview.py: could not attach #61", done.stderr)
        calls = (self.gh_dir / "calls.log").read_text(encoding="utf-8")
        self.assertIn("sub_issue_id=9062", calls)

    def test_a_ticket_that_asks_and_is_a_retro_ticket_is_listed_once_as_a_question(self):
        self.fixture("sub_issues", [issue(7, "retro", "needs-info")])
        self.fixture("retro-human", [issue(7, "retro", "needs-info", "ready-for-human")])
        data = self.listing()
        self.assertEqual([7], [t["number"] for t in data["needs_info"]])
        self.assertEqual([], data["retro"])

    def test_a_failing_tracker_call_ends_with_one_message_and_no_output(self):
        (self.gh_dir / "retro-human.json").unlink()
        done = self.run_script("list", "42")
        self.assertEqual(1, done.returncode)
        self.assertEqual("", done.stdout)
        self.assertIn("interview.py:", done.stderr)

    def test_attach_adds_each_ticket_to_the_spec_by_database_id(self):
        self.fixture("issue-61", {"id": 9061, "number": 61})
        done = self.run_script("attach", "42", "61")
        self.assertEqual(0, done.returncode, done.stderr)
        calls = (self.gh_dir / "calls.log").read_text(encoding="utf-8")
        self.assertIn("-X POST repos/marmer/hamcrest-matcher-generator/issues/42/sub_issues -F sub_issue_id=9061", calls)

    def test_a_wrong_gh_timeout_is_refused_with_an_error_line_and_exit_2_like_in_loop_sh(self):
        for value in ("abc", "0", "1.5", "-3", ""):
            env = dict(os.environ, PATH=self.path, STUB_GH_DIR=str(self.gh_dir), RALPH_GH_TIMEOUT_SECONDS=value)
            done = subprocess.run([sys.executable, str(SCRIPT), "list", "42"], env=env, capture_output=True,
                                  text=True, timeout=60)
            self.assertEqual((2, ""), (done.returncode, done.stdout), f"value {value!r}: {done.stderr}")
            self.assertIn(f"ERROR RALPH_GH_TIMEOUT_SECONDS needs a positive integer, got '{value}'", done.stderr)

    def test_wrong_arguments_are_refused_with_a_usage_line_and_exit_2(self):
        for args in ([], ["list"], ["list", "x"], ["attach", "42"], ["attach", "42", "y"], ["bogus", "42"]):
            done = self.run_script(*args)
            self.assertEqual(2, done.returncode, args)
            self.assertIn("usage:", done.stderr)


if __name__ == "__main__":
    unittest.main()
