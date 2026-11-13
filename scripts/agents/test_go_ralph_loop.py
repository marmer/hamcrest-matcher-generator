"""Black-box tests for .claude/skills/goRalph/scripts/loop.sh (stdlib unittest only).

The script runs through bash from a scratch repository copy, with stub `claude` and `gh` programs first on PATH.
The stub `claude` answers from the file named by STUB_CLAUDE_STREAM and exits with STUB_CLAUDE_EXIT (with
STUB_CLAUDE_SLEEP it first runs a `sleep` child for that many seconds, pids in STUB_CLAUDE_PID and
STUB_CLAUDE_PID.sleep); the stub `gh`
serves one ready sub-issue of the spec, or none when STUB_GH_EMPTY is set.
"""
import json
import os
import shutil
import subprocess
import tempfile
import time
import unittest
from pathlib import Path

HERE = Path(os.path.dirname(os.path.abspath(__file__)))
REPO = HERE.parents[1]
SKILL_SCRIPTS = Path(".claude") / "skills" / "goRalph" / "scripts"

STUB_CLAUDE = """#!/usr/bin/env bash
echo "$*" >> "$STUB_CALLS"
echo "stdin bytes: $(cat | wc -c)" >> "$STUB_CALLS"
if [ -n "${STUB_CLAUDE_SLEEP:-}" ]; then
  echo $$ > "$STUB_CLAUDE_PID"
  sleep "$STUB_CLAUDE_SLEEP" &
  echo $! > "$STUB_CLAUDE_PID.sleep"
  wait
fi
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
                   RALPH_REPO=str(self.repo), RALPH_MINUTE_SECONDS="1", RALPH_POLL_SECONDS="0.2",
                   RALPH_GRACE_SECONDS="1", STUB_CALLS=str(self.calls), STUB_CLAUDE_STREAM=str(self.stream))
        env.update(extra)
        return env

    def start(self, *args, **extra):
        process = subprocess.Popen(
            ["bash", str(self.repo / SKILL_SCRIPTS / "loop.sh"), "42", *args],
            cwd=self.repo, env=self.env(**extra), stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        self.addCleanup(process.kill)
        return process

    def run_loop(self, *args, **extra):
        process = self.start(*args, **extra)
        out, err = process.communicate(timeout=60)
        return process.returncode, out.splitlines(), err

    @staticmethod
    def is_running(pid):
        try:  # a zombie without a reaper in the container is dead
            return Path(f"/proc/{pid}/stat").read_text(encoding="utf-8").rsplit(")", 1)[1].split()[0] != "Z"
        except FileNotFoundError:
            return False

    def start_sleeping(self, seconds, *args, **extra):
        """Starts the loop with a child that sleeps; returns the process and the pid file of the sleep."""
        pid_file = self.root / "child.pid"
        process = self.start(*args, STUB_CLAUDE_SLEEP=str(seconds), STUB_CLAUDE_PID=str(pid_file), **extra)
        sleep_pid_file = Path(f"{pid_file}.sleep")
        for _ in range(100):
            if sleep_pid_file.exists() and sleep_pid_file.read_text(encoding="utf-8").strip():
                return process, sleep_pid_file
            time.sleep(0.1)
        self.fail("the child never started")

    def edit_control(self, **values):
        control = self.only_run_dir() / "control.json"
        data = json.loads(control.read_text(encoding="utf-8"))
        data.update(values)
        temporary = control.with_suffix(".edit")
        temporary.write_text(json.dumps(data), encoding="utf-8")
        temporary.replace(control)

    def read_control(self):
        return json.loads((self.only_run_dir() / "control.json").read_text(encoding="utf-8"))

    def write_control(self, **values):
        run = self.home / "runs" / "42-20260101-000000"
        run.mkdir(parents=True)
        (run / "control.json").write_text(json.dumps(values), encoding="utf-8")

    @staticmethod
    def berlin_wall_clock(minutes_from_now):
        shifted = int(time.time()) + minutes_from_now * 60
        return subprocess.run(["date", "-d", f"@{shifted}", "+%Y-%m-%d %H:%M"], env=dict(os.environ, TZ="Europe/Berlin"),
                              capture_output=True, text=True, check=True).stdout.strip()

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

    def test_run_directory_is_one_per_spec_and_start_time_outside_the_repository(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        run = self.only_run_dir()
        self.assertTrue(run.name.startswith("42-"), run.name)
        self.assertEqual(self.home / "runs", run.parent)

    def test_control_file_holds_the_iteration_limit(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        control = json.loads((self.only_run_dir() / "control.json").read_text(encoding="utf-8"))
        self.assertEqual(1, control["max_iterations"])

    def test_status_file_reports_the_ended_run(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        status = json.loads((self.only_run_dir() / "status.json").read_text(encoding="utf-8"))
        self.assertEqual({"spec": "42", "iteration": 1, "ok": 1, "failed": 0, "state": "ended"},
                         {key: status[key] for key in ("spec", "iteration", "ok", "failed", "state")})

    def test_result_and_stream_files_are_kept_per_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        run = self.only_run_dir()
        self.assertTrue((run / "result-1.json").exists())
        self.assertTrue((run / "stream-1.jsonl").exists())

    def test_lock_is_released_when_the_run_ends(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        self.assertEqual(0, subprocess.run(["flock", "-n", str(self.home / "locks" / "42.lock"), "true"]).returncode,
                         "the lock outlived the run")

    def test_child_is_started_without_hooks_with_the_deny_list_allowed_tools_stream_output_and_closed_stdin(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        call = self.calls.read_text(encoding="utf-8")
        for expected in ("disableAllHooks", "git push --force", "git push origin main", "git reset --hard",
                         "git branch -D", "--allowedTools", "--output-format stream-json", "stdin bytes: 0"):
            self.assertIn(expected, call)

    def test_non_zero_exit_with_a_valid_result_counts_as_failed(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1", STUB_CLAUDE_EXIT="1")

        self.assertEqual(0, code, err)
        self.assertEqual("ERROR iteration 1 failed: exit 1, success", lines[0])

    def test_missing_flock_is_reported_as_such(self):
        tools = self.root / "tools"
        tools.mkdir()
        for directory in ("/usr/bin", "/bin"):
            for tool in Path(directory).iterdir():
                if tool.name != "flock" and not os.path.lexists(tools / tool.name):
                    (tools / tool.name).symlink_to(tool)
        for stub in ("claude", "gh"):
            (tools / stub).unlink(missing_ok=True)
            (tools / stub).symlink_to(self.root / "bin" / stub)
        process = subprocess.run(
            ["bash", str(self.repo / SKILL_SCRIPTS / "loop.sh"), "42", "--max-iterations", "1"],
            cwd=self.repo, env=self.env(PATH=str(tools)), capture_output=True, text=True, timeout=60)

        self.assertEqual(2, process.returncode)
        self.assertEqual(["ERROR flock not found"], process.stdout.splitlines())

    def test_relative_ralph_home_keeps_pointing_at_the_same_directory(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        process = subprocess.run(
            ["bash", str(self.repo / SKILL_SCRIPTS / "loop.sh"), "42", "--max-iterations", "1"],
            cwd=self.root, env=self.env(RALPH_HOME="relative-home"), capture_output=True, text=True, timeout=60)

        self.assertEqual(0, process.returncode, process.stderr)
        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", process.stdout.splitlines()[0])
        self.assertTrue((self.root / "relative-home" / "runs").is_dir())
        self.assertFalse((self.repo / "relative-home").exists())

    def test_spec_that_is_not_a_number_is_refused(self):
        for spec in ("../x", "*", "4 2"):
            process = subprocess.run(
                ["bash", str(self.repo / SKILL_SCRIPTS / "loop.sh"), spec, "--max-iterations", "1"],
                cwd=self.repo, env=self.env(), capture_output=True, text=True, timeout=60)
            self.assertEqual(2, process.returncode, spec)
            self.assertTrue(process.stdout.startswith("ERROR"), spec)
        self.assertFalse((self.home / "runs").exists() and any((self.home / "runs").iterdir()))

    def test_max_iterations_that_is_not_an_integer_is_refused(self):
        code, lines, err = self.run_loop("--max-iterations", "x")

        self.assertEqual(2, code)
        self.assertTrue(lines[0].startswith("ERROR"), lines)
        self.assertFalse(self.calls.exists())

    def test_terminated_loop_releases_the_lock_and_stops_its_child(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        pid_file = self.root / "child.pid"
        process = self.start("--max-iterations", "1", STUB_CLAUDE_SLEEP="60", STUB_CLAUDE_PID=str(pid_file))
        for _ in range(100):
            if pid_file.exists() and pid_file.read_text(encoding="utf-8").strip():
                break
            time.sleep(0.1)
        child = int(pid_file.read_text(encoding="utf-8").strip())

        process.terminate()
        process.communicate(timeout=30)

        self.assertEqual(0, subprocess.run(["flock", "-n", str(self.home / "locks" / "42.lock"), "true"]).returncode,
                         "the lock outlived the killed loop")
        time.sleep(0.5)
        self.assertFalse(self.is_running(child), "the child outlived the killed loop")

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
        (self.home / "locks").mkdir(parents=True)
        holder = subprocess.Popen(["flock", str(self.home / "locks" / "42.lock"), "sleep", "60"])
        self.addCleanup(holder.wait)
        self.addCleanup(holder.kill)
        time.sleep(0.5)

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertNotEqual(0, code)
        self.assertEqual(1, len(lines), lines)
        self.assertIn("already has a running loop", lines[0])
        self.assertFalse(self.calls.exists(), "an iteration started despite the lock")

    def test_two_concurrent_starters_never_both_run(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        starters = [self.start("--max-iterations", "1", STUB_CLAUDE_SLEEP="2", STUB_CLAUDE_PID=str(self.root / f"p{i}"))
                    for i in range(2)]
        outputs = [starter.communicate(timeout=60)[0] for starter in starters]

        self.assertEqual(1, sum("already has a running loop" in out for out in outputs), outputs)
        self.assertEqual(1, self.calls.read_text(encoding="utf-8").count("-p /implement-spec 42"))

    def test_stale_lock_of_a_dead_process_is_taken_over(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        (self.home / "locks").mkdir(parents=True)
        dead = subprocess.Popen(["true"])
        dead.wait()
        (self.home / "locks" / "42.lock").write_text(str(dead.pid), encoding="utf-8")

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

    def test_control_file_starts_with_the_defaults(self):
        self.run_loop(STUB_GH_EMPTY="1")

        self.assertEqual({"until": None, "max_duration": None, "max_iterations": None, "max_iteration_duration": None,
                          "max_parallel": 3, "stop": None, "limit_stop": "after-step"}, self.read_control())

    def test_limit_options_are_written_into_the_control_file(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1", "--max-duration", "90", "--max-iteration-duration", "30",
                      "--until", "2999-01-01 06:00 Asia/Tokyo", "--max-parallel", "2", "--limit-stop", "now")

        control = self.read_control()
        self.assertEqual((90, 30, "2999-01-01 06:00 Asia/Tokyo", 2, "now"),
                         (control["max_duration"], control["max_iteration_duration"], control["until"],
                          control["max_parallel"], control["limit_stop"]))

    def test_wrong_option_values_are_refused_before_a_run_is_created(self):
        for option, value in (("--max-duration", "0"), ("--max-duration", "1h"), ("--max-iteration-duration", "-5"),
                              ("--max-parallel", "4"), ("--max-parallel", "0"), ("--limit-stop", "soon"),
                              ("--until", "6 Uhr"), ("--until", "2999-13-45 25:61"),
                              ("--until", "2999-01-01 06:00 Nowhere/Land"), ("--until", "2999-01-01 06:00 ../x")):
            code, lines, err = self.run_loop(option, value)
            self.assertEqual(2, code, (option, value))
            self.assertTrue(lines[0].startswith("ERROR " + option[2:].replace("-", "_")), (option, value, lines))
        self.assertFalse((self.home / "runs").exists() and any((self.home / "runs").iterdir()))
        self.assertFalse(self.calls.exists())

    def test_wrong_value_in_the_control_file_is_refused_and_keeps_the_run_unfinished(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        for key, value in (("max_duration", "soon"), ("max_iteration_duration", 0), ("max_parallel", 5),
                           ("max_iterations", 1.5), ("limit_stop", "later"), ("stop", "whenever"),
                           ("until", "tomorrow")):
            self.write_control(**{key: value})
            code, lines, err = self.run_loop()
            self.assertEqual(2, code, (key, value))
            self.assertTrue(lines[0].startswith(f"ERROR {key}"), lines)
            self.assertFalse((self.home / "runs" / "42-20260101-000000" / "ended").exists(), key)
            shutil.rmtree(self.home / "runs")
        self.assertFalse(self.calls.exists())

    def test_wrong_timing_knob_is_refused(self):
        code, lines, err = self.run_loop(RALPH_GRACE_SECONDS="soon")

        self.assertEqual(2, code)
        self.assertTrue(lines[0].startswith("ERROR RALPH_GRACE_SECONDS"), lines)

    def test_until_in_the_past_ends_the_run_before_the_first_iteration(self):
        self.write_control(until="2000-01-01 00:00")

        code, lines, err = self.run_loop()

        self.assertEqual(0, code, err)
        self.assertEqual(["END after 0 iterations: until 2000-01-01 00:00 reached (0 ok, 0 failed)"], lines)

    def test_until_in_the_future_does_not_end_the_run(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.write_control(until="2999-01-01 06:00", max_iterations=1)

        code, lines, err = self.run_loop()

        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def test_until_reads_the_wall_clock_in_berlin_by_default(self):
        past_in_berlin = self.berlin_wall_clock(-30)
        self.write_control(until=past_in_berlin)

        code, lines, err = self.run_loop()

        self.assertEqual(f"END after 0 iterations: until {past_in_berlin} reached (0 ok, 0 failed)", lines[0])

    def test_until_reads_the_wall_clock_in_the_named_zone(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.write_control(until=self.berlin_wall_clock(-30) + " America/New_York", max_iterations=1)

        code, lines, err = self.run_loop()

        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def test_max_duration_ends_the_run_before_the_next_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        process, _ = self.start_sleeping(2, "--max-duration", "1")
        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: max_duration 1 reached (1 ok, 0 failed)"], out)

    def test_reached_limit_writes_the_configured_stop_kind_into_the_control_file(self):
        self.write_control(until="2000-01-01 00:00", limit_stop="after-iteration")

        self.run_loop()

        self.assertEqual("after-iteration", self.read_control()["stop"])

    def test_reached_limit_stops_after_the_step_by_default(self):
        self.write_control(until="2000-01-01 00:00")

        self.run_loop()

        self.assertEqual("after-step", self.read_control()["stop"])

    def test_max_iteration_duration_writes_the_configured_stop_kind_and_ends_the_run_after_the_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(4, "--max-iteration-duration", "1", "--limit-stop", "after-iteration",
                                         RALPH_GRACE_SECONDS="30")
        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual("after-iteration", self.read_control()["stop"])
        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: stop requested (after-iteration) (1 ok, 0 failed)"], out)

    def test_max_iteration_duration_kills_the_iteration_after_the_grace_period(self):
        process, sleep_pid_file = self.start_sleeping(60, "--max-iteration-duration", "1")
        sleep_pid = int(sleep_pid_file.read_text(encoding="utf-8").strip())

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertFalse(self.is_running(sleep_pid))
        self.assertEqual("ERROR iteration 1 failed: killed, max_iteration_duration 1 reached and grace period over", out[0])
        self.assertEqual("after-step", self.read_control()["stop"])
        self.assertEqual("END after 1 iterations: stop requested (after-step) (0 ok, 1 failed)", out[1])

    def test_max_iteration_duration_with_stop_kind_now_kills_without_the_grace_period(self):
        process, sleep_pid_file = self.start_sleeping(60, "--max-iteration-duration", "1", "--limit-stop", "now",
                                                      RALPH_GRACE_SECONDS="600")
        sleep_pid = int(sleep_pid_file.read_text(encoding="utf-8").strip())

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertFalse(self.is_running(sleep_pid))
        self.assertEqual("ERROR iteration 1 failed: killed, stop now", out[0])

    def test_stop_request_after_iteration_lets_the_running_iteration_finish(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(2, "--max-iterations", "5")
        self.edit_control(stop="after-iteration")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: stop requested (after-iteration) (1 ok, 0 failed)"], out)

    def test_stop_request_after_step_lets_the_running_iteration_finish(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(2, "--max-iterations", "5")
        self.edit_control(stop="after-step")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: stop requested (after-step) (1 ok, 0 failed)"], out)

    def test_stop_request_without_a_kind_means_after_step(self):
        self.write_control(stop=True)

        code, lines, err = self.run_loop()

        self.assertEqual(["END after 0 iterations: stop requested (after-step) (0 ok, 0 failed)"], lines)

    def test_stop_request_now_kills_the_process_group_of_the_child(self):
        process, sleep_pid_file = self.start_sleeping(60, "--max-iterations", "5")
        sleep_pid = int(sleep_pid_file.read_text(encoding="utf-8").strip())
        stub_pid = int((self.root / "child.pid").read_text(encoding="utf-8").strip())
        self.edit_control(stop="now")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertFalse(self.is_running(sleep_pid), "the grandchild survived")
        self.assertFalse(self.is_running(stub_pid), "the child survived")
        self.assertEqual("ERROR iteration 1 failed: killed, stop now", out[0])
        self.assertEqual("END after 1 iterations: stop requested (now) (0 ok, 1 failed)", out[1])


if __name__ == "__main__":
    unittest.main()
