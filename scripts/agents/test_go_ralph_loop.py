"""Black-box tests for .claude/skills/goRalph/scripts/loop.sh (stdlib unittest only).

The script runs through bash from a scratch repository copy, with stub `claude` and `gh` programs first on PATH.
The stub `claude` answers from the file named by STUB_CLAUDE_STREAM and exits with STUB_CLAUDE_EXIT; the stub `gh`
serves one ready sub-issue of the spec, or none when STUB_GH_EMPTY is set.
"""
import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
REPO = HERE.parents[1]
SKILL_SCRIPTS = Path(".claude") / "skills" / "goRalph" / "scripts"

STUB_CLAUDE = """#!/usr/bin/env bash
echo "$*" >> "$STUB_CALLS"
cat "$STUB_CLAUDE_STREAM"
exit "${STUB_CLAUDE_EXIT:-0}"
"""

STUB_GH = """#!/usr/bin/env bash
case "$*" in
  *sub_issues*) if [ -n "${STUB_GH_EMPTY:-}" ]; then echo '[]'; else echo '[{"number": 7, "title": "Ticket", "state": "open", "labels": []}]'; fi ;;
  *dependencies*) echo '[]' ;;
esac
"""

SUCCESS = json.dumps({"type": "result", "subtype": "success", "is_error": False, "result": "done",
                      "num_turns": 3, "total_cost_usd": 0.25, "permission_denials": []})
ERROR_RESULT = json.dumps({"type": "result", "subtype": "success", "is_error": True, "result": "API Error",
                           "num_turns": 1, "total_cost_usd": 0.01, "permission_denials": []})
TURN_LIMIT = json.dumps({"type": "result", "subtype": "error_max_turns", "is_error": False,
                         "num_turns": 41, "total_cost_usd": 1.5, "permission_denials": []})
DENIED = json.dumps({"type": "result", "subtype": "success", "is_error": False, "result": "done",
                     "num_turns": 2, "total_cost_usd": 0.1, "permission_denials": [{"tool_name": "Bash"}]})


def write_executable(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    path.chmod(0o755)


class LoopScriptTest(unittest.TestCase):

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        self.repo = self.root / "repo"
        self.home = self.root / "home"
        shutil.copytree(REPO / SKILL_SCRIPTS, self.repo / SKILL_SCRIPTS)
        # The skill's script is not executable on purpose: it must run through bash.
        (self.repo / SKILL_SCRIPTS / "loop.sh").chmod(0o644)
        (self.repo / "scripts" / "agents").mkdir(parents=True)
        shutil.copy2(REPO / "scripts" / "agents" / "issue-status.py", self.repo / "scripts" / "agents")
        write_executable(self.root / "bin" / "claude", STUB_CLAUDE)
        write_executable(self.root / "bin" / "gh", STUB_GH)
        self.calls = self.root / "claude-calls.txt"
        self.stream = self.root / "stream.jsonl"

    def tearDown(self):
        self.directory.cleanup()

    def env(self, **extra):
        env = dict(os.environ, PATH=f"{self.root / 'bin'}:{os.environ['PATH']}", RALPH_HOME=str(self.home),
                   RALPH_REPO=str(self.repo), STUB_CALLS=str(self.calls), STUB_CLAUDE_STREAM=str(self.stream))
        env.update(extra)
        return env

    def start(self, *args, **extra):
        return subprocess.Popen(
            ["bash", str(self.repo / SKILL_SCRIPTS / "loop.sh"), "42", *args],
            cwd=self.repo, env=self.env(**extra), stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

    def run_loop(self, *args, **extra):
        process = self.start(*args, **extra)
        out, err = process.communicate(timeout=60)
        return process.returncode, out.splitlines(), err

    def only_run_dir(self):
        (run,) = (self.home / "runs").iterdir()
        return run

    def test_n_successful_iterations_then_end(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "2")

        self.assertEqual(0, code, err)
        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "iteration 2 ok: 3 turns, cost $0.25",
                          "END after 2 iterations: max_iterations 2 reached (2 ok, 0 failed)"], lines)
        self.assertEqual(2, self.calls.read_text(encoding="utf-8").count("-p /implement-spec 42"))
        self.assertIn("-p /implement-spec 42", self.calls.read_text(encoding="utf-8"))
        self.assertIn("--model sonnet", self.calls.read_text(encoding="utf-8"))

    def test_run_directory_lives_outside_the_repository_with_control_status_results_and_streams(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        run = self.only_run_dir()
        self.assertTrue(run.name.startswith("42-"), run.name)
        self.assertEqual(self.home / "runs", run.parent)
        control = json.loads((run / "control.json").read_text(encoding="utf-8"))
        self.assertEqual(1, control["max_iterations"])
        status = json.loads((run / "status.json").read_text(encoding="utf-8"))
        self.assertEqual({"spec": "42", "iteration": 1, "ok": 1, "failed": 0, "state": "ended"},
                         {key: status[key] for key in ("spec", "iteration", "ok", "failed", "state")})
        self.assertTrue((run / "result-1.json").exists())
        self.assertTrue((run / "stream-1.jsonl").exists())
        self.assertFalse((self.home / "locks" / "42.lock").exists(), "the lock outlived the run")

    def test_error_result_with_exit_zero_counts_as_failed(self):
        self.stream.write_text(ERROR_RESULT + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual("ERROR iteration 1 failed: is_error set, success", lines[0])
        self.assertEqual("END after 1 iterations: max_iterations 1 reached (0 ok, 1 failed)", lines[1])

    def test_turn_limit_result_without_result_field_counts_as_failed(self):
        self.stream.write_text(TURN_LIMIT + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual("ERROR iteration 1 failed: no result field, error_max_turns", lines[0])

    def test_permission_denials_produce_a_denial_line(self):
        self.stream.write_text(DENIED + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual('DENIAL iteration 1: ["Bash"]', lines[1])

    def test_lock_refuses_a_second_run_on_the_same_spec(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        lock = self.home / "locks" / "42.lock"
        lock.mkdir(parents=True)
        holder = subprocess.Popen(["sleep", "60"])
        self.addCleanup(holder.wait)
        self.addCleanup(holder.kill)
        (lock / "pid").write_text(str(holder.pid), encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertNotEqual(0, code)
        self.assertEqual(1, len(lines), lines)
        self.assertIn("already has a running loop", lines[0])
        self.assertFalse(self.calls.exists(), "an iteration started despite the lock")

    def test_stale_lock_of_a_dead_process_is_taken_over(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        lock = self.home / "locks" / "42.lock"
        lock.mkdir(parents=True)
        dead = subprocess.Popen(["true"])
        dead.wait()
        (lock / "pid").write_text(str(dead.pid), encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def test_run_ends_when_no_ticket_is_ready_for_an_agent(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "3", STUB_GH_EMPTY="1")

        self.assertEqual(0, code, err)
        self.assertEqual(["END after 0 iterations: no ticket ready for an agent (0 ok, 0 failed)"], lines)

    def test_stop_request_in_the_control_file_ends_the_run_before_the_next_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        run = self.home / "runs" / "42-20260101-000000"
        run.mkdir(parents=True)
        (run / "control.json").write_text(json.dumps({"max_iterations": 5, "stop": "after-step"}), encoding="utf-8")

        code, lines, err = self.run_loop()

        self.assertEqual(0, code, err)
        self.assertEqual(["END after 0 iterations: stop requested (after-step) (0 ok, 0 failed)"], lines)


if __name__ == "__main__":
    unittest.main()
