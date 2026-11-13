"""Black-box tests for .claude/skills/goRalph/scripts/summary.py (stdlib unittest only).

The script runs on a scratch run directory (started file, status.json, ended marker) with a stub `gh` first on PATH.
The stub serves one JSON fixture per endpoint from the directory STUB_GH_DIR: pulls.json, sub_issues.json,
comments-<n>.json and retro.json, each a list of pages; a missing fixture makes it fail like a broken tracker call.
Like gh, it prints the first page only, and with `--paginate --slurp` all pages as one array of pages.
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
SCRIPT = HERE.parents[1] / ".claude" / "skills" / "goRalph" / "scripts" / "summary.py"

STUB_GH = """#!/usr/bin/env bash
case "$*" in
  *pulls*) f=pulls.json ;;
  *sub_issues*) f=sub_issues.json ;;
  *labels=retro*) f=retro.json ;;
  *issues/*/comments*) a="$*"; n="${a#*issues/}"; f="comments-${n%%/*}.json" ;;
  *) f=unknown ;;
esac
[ -f "$STUB_GH_DIR/$f" ] || { echo "stub gh: no fixture $f for $*" >&2; exit 1; }
case "$*" in
  *--paginate*--slurp*|*--slurp*--paginate*) cat "$STUB_GH_DIR/$f" ;;
  *) jq -c '.[0]' "$STUB_GH_DIR/$f" ;;
esac
"""

START = 1_700_000_000  # 2023-11-14T22:13:20Z
BEFORE = "2023-11-14T20:00:00Z"
AFTER = "2023-11-14T23:00:00Z"


class SummaryTest(unittest.TestCase):

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        root = Path(self.directory.name)
        self.run_dir = root / "runs" / "42-20231114-221320"
        self.run_dir.mkdir(parents=True)
        self.gh_dir = root / "fixtures"
        self.gh_dir.mkdir()
        stub = root / "bin" / "gh"
        stub.parent.mkdir()
        stub.write_text(STUB_GH, encoding="utf-8")
        stub.chmod(0o755)
        self.path = f"{stub.parent}:{os.environ['PATH']}"
        (self.run_dir / "started").write_text(f"{START}\n", encoding="utf-8")
        (self.run_dir / "status.json").write_text(json.dumps(
            {"spec": "42", "state": "ended", "iteration": 3, "ok": 2, "failed": 1, "run_dir": str(self.run_dir)}),
            encoding="utf-8")
        for name in ("pulls", "sub_issues", "retro"):
            self.fixture(name, [])

    def fixture(self, name, data):
        self.pages(name, [data])

    def pages(self, name, pages):
        (self.gh_dir / f"{name}.json").write_text(json.dumps(pages), encoding="utf-8")

    def summarize(self):
        env = dict(os.environ, PATH=self.path, STUB_GH_DIR=str(self.gh_dir))
        return subprocess.run([sys.executable, str(SCRIPT), str(self.run_dir)], env=env,
                              capture_output=True, text=True, timeout=60)

    def data(self):
        done = self.summarize()
        self.assertEqual(0, done.returncode, done.stderr)
        return json.loads(done.stdout)

    def test_lists_pull_requests_merged_since_the_run_started(self):
        self.fixture("pulls", [
            {"number": 5, "title": "feat: new", "html_url": "https://example.test/pull/5", "merged_at": AFTER},
            {"number": 4, "title": "feat: older", "html_url": "https://example.test/pull/4", "merged_at": BEFORE},
            {"number": 3, "title": "feat: closed unmerged", "html_url": "https://example.test/pull/3", "merged_at": None},
        ])
        self.assertEqual([{"number": 5, "title": "feat: new", "url": "https://example.test/pull/5"}],
                         self.data()["merged_prs"])

    def test_lists_open_needs_info_tickets_of_the_spec_as_open_questions(self):
        self.fixture("sub_issues", [
            {"number": 7, "title": "Asks", "state": "open", "html_url": "u7", "labels": [{"name": "needs-info"}]},
            {"number": 8, "title": "Plain", "state": "open", "html_url": "u8", "labels": [{"name": "ready-for-agent"}]},
            {"number": 9, "title": "Answered", "state": "closed", "html_url": "u9", "labels": [{"name": "needs-info"}]},
        ])
        for number in (7, 8, 9):
            self.fixture(f"comments-{number}", [])
        self.assertEqual([{"number": 7, "title": "Asks", "url": "u7"}], self.data()["open_questions"])

    def escalations_of(self, *comments):
        self.fixture("sub_issues", [{"number": 7, "title": "Hard", "state": "closed", "html_url": "u7", "labels": []}])
        self.fixture("comments-7", [{"body": body, "created_at": created, "html_url": f"c{i}"}
                                    for i, (body, created) in enumerate(comments)])
        return [e["comment"] for e in self.data()["escalations"]]

    def test_an_escalation_comment_made_during_the_run_is_listed_with_its_ticket(self):
        self.fixture("sub_issues", [{"number": 7, "title": "Hard", "state": "closed", "html_url": "u7", "labels": []}])
        self.fixture("comments-7", [{"body": "Escalated from haiku to sonnet: three failed attempts",
                                     "created_at": AFTER, "html_url": "c1"}])
        self.assertEqual([{"ticket": 7, "title": "Hard", "comment": "Escalated from haiku to sonnet: three failed attempts",
                           "url": "c1"}], self.data()["escalations"])

    def test_an_escalation_comment_from_before_the_run_is_not_listed(self):
        self.assertEqual([], self.escalations_of(("Escalated from sonnet to opus", BEFORE)))

    def test_a_model_transition_without_the_word_escalation_is_listed(self):
        self.assertEqual(["Rerun on sonnet: haiku -> sonnet, missed the bar", "haiku \u2192 sonnet, red evidence missing"],
                         self.escalations_of(("Rerun on sonnet: haiku -> sonnet, missed the bar", AFTER),
                                             ("haiku \u2192 sonnet, red evidence missing", AFTER)))

    def test_a_closing_model_line_naming_sonnet_or_opus_is_listed(self):
        self.assertEqual(["Model: sonnet, attempts: 4", "Model: opus, attempts: 2"],
                         self.escalations_of(("Model: sonnet, attempts: 4", AFTER), ("Model: opus, attempts: 2", AFTER)))

    def test_a_closing_model_line_naming_haiku_and_plain_comments_are_not_listed(self):
        self.assertEqual([], self.escalations_of(("Model: haiku, attempts: 1", AFTER), ("Looks good, sonnet reviewed it", AFTER)))

    def test_lists_retro_tickets_created_since_the_run_started_and_no_pull_requests(self):
        self.fixture("retro", [
            {"number": 60, "title": "Retro new", "html_url": "u60", "created_at": AFTER},
            {"number": 59, "title": "Retro old", "html_url": "u59", "created_at": BEFORE},
            {"number": 61, "title": "A pull request", "html_url": "u61", "created_at": AFTER, "pull_request": {}},
        ])
        self.assertEqual([{"number": 60, "title": "Retro new", "url": "u60"}], self.data()["retro_tickets"])

    def test_duration_of_an_ended_run_runs_from_the_started_file_to_the_ended_marker(self):
        ended = self.run_dir / "ended"
        ended.touch()
        os.utime(ended, (START + 5400, START + 5400))
        self.assertEqual(5400, self.data()["duration_seconds"])

    def test_counts_and_state_come_from_the_status_file(self):
        data = self.data()
        self.assertEqual((42, "ended", 3, 2, 1), (data["spec"], data["state"], data["iterations"], data["ok"], data["failed"]))

    def test_every_page_of_the_merged_pull_requests_is_read(self):
        self.pages("pulls", [
            [{"number": 5, "title": "first page", "html_url": "u5", "merged_at": AFTER}],
            [{"number": 4, "title": "second page", "html_url": "u4", "merged_at": AFTER}]])
        self.assertEqual([5, 4], [p["number"] for p in self.data()["merged_prs"]])

    def test_every_page_of_the_spec_tickets_is_read(self):
        self.pages("sub_issues", [
            [{"number": 7, "title": "One", "state": "open", "html_url": "u7", "labels": [{"name": "needs-info"}]}],
            [{"number": 8, "title": "Two", "state": "open", "html_url": "u8", "labels": [{"name": "needs-info"}]}]])
        self.fixture("comments-7", [])
        self.fixture("comments-8", [])
        self.assertEqual([7, 8], [q["number"] for q in self.data()["open_questions"]])

    def test_a_tracker_call_that_times_out_ends_with_one_message_and_no_traceback(self):
        stub = Path(self.path.split(":")[0]) / "gh"
        stub.write_text("#!/usr/bin/env bash\nsleep 5\n", encoding="utf-8")
        env = dict(os.environ, PATH=self.path, STUB_GH_DIR=str(self.gh_dir), RALPH_GH_TIMEOUT_SECONDS="0.2")
        done = subprocess.run([sys.executable, str(SCRIPT), str(self.run_dir)], env=env,
                              capture_output=True, text=True, timeout=60)
        self.assertEqual((1, ""), (done.returncode, done.stdout))
        self.assertNotIn("Traceback", done.stderr)

    def test_a_failing_tracker_call_ends_with_an_error_and_no_data(self):
        (self.gh_dir / "pulls.json").unlink()
        done = self.summarize()
        self.assertNotEqual(0, done.returncode)
        self.assertEqual("", done.stdout)
        self.assertIn("pulls.json", done.stderr)


if __name__ == "__main__":
    unittest.main()
