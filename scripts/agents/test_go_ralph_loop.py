"""Black-box tests for .claude/skills/goRalph/scripts/loop.sh (stdlib unittest only).

The script runs through bash from a scratch repository copy, with stub `claude` and `gh` programs first on PATH.
The stub `claude` answers from the file named by STUB_CLAUDE_STREAM and exits with STUB_CLAUDE_EXIT (with
STUB_CLAUDE_SLEEP it first runs a `sleep` child for that many seconds, pids in STUB_CLAUDE_PID and
STUB_CLAUDE_PID.sleep); its arguments of the last call are written NUL-separated to STUB_CALLS.argv;
a call whose prompt starts with `/retro` is the retro iteration: it appears in STUB_CALLS but not in .times or the
numbering of the streams, its arguments go to STUB_CALLS.retro.argv, and it answers from STUB_CLAUDE_STREAM.retro
(STUB_CLAUDE_STREAM without it; exit code from STUB_CLAUDE_STREAM.retro.exit; with STUB_RETRO_SLEEP it first runs a
`sleep` child for that many seconds, pids in STUB_RETRO_PID and STUB_RETRO_PID.sleep); a file
STUB_CLAUDE_STREAM.<n> answers the n-th call instead (its exit code is the content of
STUB_CLAUDE_STREAM.<n>.exit, 0 without it), and each call's time in seconds on the monotonic clock (/proc/uptime, resolution 0.01 s) is appended to STUB_CALLS.times); the stub
`gh` serves one ready sub-issue of the spec, none when STUB_GH_EMPTY is set, or one claimed one (the number 7, so not
free) when STUB_GH_CLAIMED is set. Every other endpoint is answered from the file in STUB_GH_DIR named like the
endpoint without the repository prefix and query, slashes as underscores (`pulls`, `pulls_5`,
`commits_<sha>_check-runs`), `[]` when there is none; a file `fail_<name>` makes that endpoint fail, and every such
call is appended to `calls.log` there, and a file `hang_<name>` makes it hang. With `--paginate --slurp` the stub
answers the sub-issues as one page and every other file as it is, so the file of a list endpoint holds the list of its
pages (for check runs a list of the pages' objects).
A file STUB_CLAUDE_DATA (or its own STUB_CLAUDE_DATA.<n> for the n-th call) is copied by the stub iteration to the
iteration's work data file in the run directory, as the orchestrator writes it.
"""
import json
import os
import re
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
if [[ "$2" == "/retro"* ]]; then
  printf '%s\\0' "$@" > "$STUB_CALLS.retro.argv"
  echo "RALPH_RUN_DIR=${RALPH_RUN_DIR:-}" >> "$STUB_CALLS.retro"
  echo "stdin bytes: $(cat | wc -c)" >> "$STUB_CALLS"
  if [ -n "${STUB_RETRO_SLEEP:-}" ]; then
    echo $$ > "$STUB_RETRO_PID"
    sleep "$STUB_RETRO_SLEEP" &
    echo $! > "$STUB_RETRO_PID.sleep"
    wait
  fi
  if [ -e "$STUB_CLAUDE_STREAM.retro" ]; then cat "$STUB_CLAUDE_STREAM.retro"; else cat "$STUB_CLAUDE_STREAM"; fi
  exit "$(cat "$STUB_CLAUDE_STREAM.retro.exit" 2>/dev/null || echo 0)"
fi
printf '%s\\0' "$@" > "$STUB_CALLS.argv"
echo "stdin bytes: $(cat | wc -c)" >> "$STUB_CALLS"
cut -d" " -f1 /proc/uptime >> "$STUB_CALLS.times"
n="$(wc -l < "$STUB_CALLS.times")"
if [ -n "${STUB_CLAUDE_DATA:-}" ]; then  # the work data the iteration writes into the run directory (the n-th call's own file first)
  data="$STUB_CLAUDE_DATA.$n"
  [ -e "$data" ] || data="$STUB_CLAUDE_DATA"
  [ -e "$data" ] && cp "$data" "$RALPH_RUN_DIR/iteration-$RALPH_ITERATION.json"
fi
if [ -n "${STUB_CLAUDE_SLEEP:-}" ]; then
  echo $$ > "$STUB_CLAUDE_PID"
  sleep "$STUB_CLAUDE_SLEEP" &
  echo $! > "$STUB_CLAUDE_PID.sleep"
  wait
fi
if [ -e "$STUB_CLAUDE_STREAM.$n" ]; then
  cat "$STUB_CLAUDE_STREAM.$n"
  exit "$(cat "$STUB_CLAUDE_STREAM.$n.exit" 2>/dev/null || echo 0)"
fi
cat "$STUB_CLAUDE_STREAM"
exit "${STUB_CLAUDE_EXIT:-0}"
"""

STUB_GH = """#!/usr/bin/env bash
slurp=""
for argument in "$@"; do [ "$argument" = --slurp ] && slurp=1; done
endpoint="${@: -1}"
wrap() { if [ -n "$slurp" ]; then echo "[$(cat)]"; else cat; fi; }
case "$endpoint" in
  *sub_issues*)
    if [ -n "${STUB_GH_EMPTY:-}" ]; then echo '[]' | wrap
    elif [ -n "${STUB_GH_CLAIMED:-}" ]; then echo '[{"number": 7, "title": "Ticket", "state": "open", "labels": [], "assignees": [{"login": "x"}]}]' | wrap
    else echo '[{"number": 7, "title": "Ticket", "state": "open", "labels": []}]' | wrap; fi ;;
  *dependencies*) echo '[]' ;;
  *)
    name="${endpoint#repos/marmer/hamcrest-matcher-generator/}"
    name="${name%%\\?*}"
    name="${name//\\//_}"
    echo "$name" >> "$STUB_GH_DIR/calls.log"
    if [ -e "$STUB_GH_DIR/fail_$name" ]; then echo "stub gh: $name failed" >&2; exit 1; fi
    if [ -e "$STUB_GH_DIR/hang_$name" ]; then sleep 30; fi
    if [ -e "$STUB_GH_DIR/$name" ]; then cat "$STUB_GH_DIR/$name"; else echo '[]'; fi ;;
esac
"""

# /proc/uptime counts hundredths of a second, truncated: the script's pause and the logged call times each lose less
# than 0.01 s, and the pause starts after the first call began, so a pause that is not cut short measures at least
# its length minus that resolution (the wall clock would break this when it steps).
CLOCK_RESOLUTION = 0.02

SUCCESS = json.dumps({"type": "result", "subtype": "success", "is_error": False, "result": "done",
                      "num_turns": 3, "total_cost_usd": 0.25, "permission_denials": []})
ERROR_RESULT = json.dumps({"type": "result", "subtype": "success", "is_error": True, "result": "API Error",
                           "num_turns": 1, "total_cost_usd": 0.01, "permission_denials": []})
TURN_LIMIT = json.dumps({"type": "result", "subtype": "error_max_turns", "is_error": False,
                         "num_turns": 41, "total_cost_usd": 1.5, "permission_denials": []})
USAGE_LIMIT = json.dumps({"type": "result", "subtype": "success", "is_error": True,
                          "result": "You've hit your limit · resets 3pm (Europe/Berlin)", "api_error_status": 429,
                          "num_turns": 1, "total_cost_usd": 0, "permission_denials": []})
DENIED = json.dumps({"type": "result", "subtype": "success", "is_error": False, "result": "done",
                     "num_turns": 2, "total_cost_usd": 0.1, "permission_denials": [{"tool_name": "Bash"}]})


def write_executable(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    path.chmod(0o755)


class IterationRulesTest(unittest.TestCase):
    """The loop rules text handed to every iteration: it must name each loop rule and nothing about implementing."""

    @classmethod
    def setUpClass(cls):
        cls.rules = (REPO / SKILL_SCRIPTS / "iteration-rules.md").read_text(encoding="utf-8")
        cls.lines = [line.lower() for line in cls.rules.splitlines() if line.startswith("- ")]

    def rule_with(self, *words):
        """The one rule line holding all words (case-insensitive)."""
        found = [line for line in self.lines
                 if all(re.search(rf"(?<!\w){re.escape(word)}(?!\w)", line) for word in words)]
        self.assertTrue(found, f"no rule line names {words}; the rules text:\n{self.rules}")
        return found[0]

    def test_control_file_is_read_between_work_steps(self):
        self.rule_with("control.json", "between work steps", "this run")

    def test_max_parallel_is_a_hard_number_read_from_the_control_file_at_the_start_with_the_read_tool(self):
        self.rule_with("max_parallel", "hard", "control.json", "at the start", "read tool", "this run")

    def test_control_file_is_reread_before_each_implementer_after_each_merge_and_before_each_review_round(self):
        self.rule_with("control.json", "before each new implementer", "after each implementer has reported",
                       "after each merge", "before each review round")

    def test_handover_note_is_kept_on_the_ticket_branch_after_every_work_step(self):
        self.rule_with("notes/ticket-<n>.md", "ticket branch", "after every work step")

    def test_max_parallel_bounds_the_tickets_of_the_iteration(self):
        self.rule_with("max_parallel", "upper bound")

    def test_tickets_and_prs_are_written_to_the_iteration_file_through_the_wrapper(self):
        self.rule_with("scripts/agents/ralph-iteration.py", "data", "iteration-", "tickets", "prs")

    def test_auto_merge_is_enabled_through_the_wrapper_and_never_merged_by_hand(self):
        self.rule_with("scripts/agents/ralph-iteration.py", "auto-merge", "never merge", "by hand")

    def test_run_directory_and_iteration_number_are_literal_values_at_the_end_of_the_prompt_never_expanded(self):
        self.rule_with("literal", "this run", "never expand")

    def test_bash_calls_are_one_command_with_pipes_only_into_head_or_grep(self):
        self.rule_with("one command", "pipe", "head", "grep")

    def test_owner_question_is_a_ticket_comment_with_question_options_and_recommendation_and_needs_info(self):
        self.rule_with("never ask the owner interactively", "comment", "question", "options", "recommendation",
                       "needs-info")

    def test_prs_are_opened_with_squash_auto_merge(self):
        self.rule_with("pr", "squash", "auto-merge")

    def test_ci_is_waited_for_once_for_at_most_30_minutes_without_tokens(self):
        self.rule_with("ci", "once", "30 min", "no tokens")

    def test_red_ci_is_fixed_once_and_then_the_iteration_ends(self):
        self.rule_with("red ci", "one fix", "end")

    def test_escalation_path_is_a_pointer_to_the_implementation_doc_with_the_loop_model(self):
        self.rule_with("escalation", "docs/agents/implementation.md", "sonnet")

    def test_the_rules_hold_no_instructions_on_how_a_ticket_is_implemented(self):
        for word in ("mvnw", "tdd", "spotless", "verify", "golden", "red evidence", "worktree", "conventional commit",
                     "jacoco"):
            self.assertNotIn(word, self.rules.lower(), f"how-to content in the loop rules: {word}")

    def test_the_model_named_for_the_loop_matches_the_model_loop_sh_starts(self):
        script = (REPO / SKILL_SCRIPTS / "loop.sh").read_text(encoding="utf-8")
        self.assertIn("--model sonnet", script)


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
        self.work_data = self.root / "work-data.json"
        self.gh_dir = self.root / "gh"
        self.gh_dir.mkdir()

    def tearDown(self):
        self.directory.cleanup()

    def env(self, **extra):
        env = dict(os.environ, PATH=f"{self.root / 'bin'}:{os.environ['PATH']}", RALPH_HOME=str(self.home),
                   RALPH_REPO=str(self.repo), RALPH_MINUTE_SECONDS="1", RALPH_POLL_SECONDS="0.2",
                   RALPH_GRACE_SECONDS="1", STUB_CALLS=str(self.calls), STUB_CLAUDE_STREAM=str(self.stream),
                   STUB_GH_DIR=str(self.gh_dir))
        env.update(extra)
        return env

    def start(self, *args, **extra):
        process = subprocess.Popen(
            ["bash", str(self.repo / SKILL_SCRIPTS / "loop.sh"), "42", *args],
            cwd=self.repo, env=self.env(**extra), stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        self.addCleanup(process.kill)
        return process

    def run_loop(self, *args, seconds=60, **extra):
        process = self.start(*args, **extra)
        out, err = self.finish(process, seconds)
        return process.returncode, out.splitlines(), err

    def finish(self, process, seconds):
        """Waits for the loop; one that is still running after the given seconds is killed and fails the test with
        the monitor lines it had printed."""
        try:
            return process.communicate(timeout=seconds)
        except subprocess.TimeoutExpired:
            process.kill()
            out, err = process.communicate()
            self.fail(f"the loop did not end within {seconds} s; its monitor lines: {out.splitlines()}")

    @staticmethod
    def wait_until(condition, what, seconds=15):
        deadline = time.monotonic() + seconds
        while not condition():
            if time.monotonic() > deadline:
                raise AssertionError(f"timed out waiting for {what}")
            time.sleep(0.05)

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
        lines = self.without_retro(lines)

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

    def argv(self):
        """The arguments of the child's last start."""
        return self.calls.with_name(self.calls.name + ".argv").read_bytes().decode("utf-8").split("\0")[:-1]

    def child_option(self, name):
        """The value after the option `name` in the arguments of the child's last start."""
        arguments = self.argv()
        self.assertIn(name, arguments, f"the child was started without {name}; its arguments: {arguments}")
        return arguments[arguments.index(name) + 1]

    def test_child_is_allowed_exactly_git_maven_gh_api_head_grep_repo_python_scripts_and_the_agent_tools(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        self.assertEqual(["Agent", "Bash(./mvnw:*)", "Bash(gh api:*)", "Bash(git:*)", "Bash(grep:*)",
                          "Bash(head:*)", "Bash(python3 scripts/*)", "Edit", "Read", "Skill", "Write"],
                         sorted(self.child_option("--allowedTools").split(",")))

    def test_child_is_denied_force_push_push_to_main_hard_reset_and_branch_delete(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        deny = json.loads(self.child_option("--settings"))["permissions"]["deny"]
        for command in ("git push --force", "git push -f", "git push origin main", "git push origin HEAD:main",
                        "git reset --hard", "git branch -D"):
            self.assertIn(f"Bash({command}*)", deny, f"{command} is not denied; deny list: {deny}")

    def test_child_never_runs_with_bypassed_permissions(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        arguments = "\0".join(self.argv())
        for forbidden in ("bypassPermissions", "--dangerously-skip-permissions", "--permission-mode"):
            self.assertNotIn(forbidden, arguments, f"the child was started with {forbidden}")

    def test_child_gets_the_loop_rules_as_system_prompt_and_the_spec_as_its_task(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        rules = (REPO / SKILL_SCRIPTS / "iteration-rules.md").read_text(encoding="utf-8")
        self.assertTrue(self.child_option("--append-system-prompt").startswith(rules.strip()))
        self.assertEqual("/implement-spec 42", self.child_option("-p"))

    def test_child_is_told_the_literal_run_directory_and_its_iteration_number_in_the_system_prompt(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "2")

        prompt = self.child_option("--append-system-prompt")
        self.assertIn(f"Run directory: {self.only_run_dir()}\n", prompt)
        self.assertTrue(prompt.endswith("\nIteration: 2"), prompt[-120:])

    def test_every_iteration_with_denials_gets_its_own_denial_line_naming_each_denied_tool(self):
        denied = json.dumps({"type": "result", "subtype": "success", "is_error": False, "result": "done",
                             "num_turns": 2, "total_cost_usd": 0.1,
                             "permission_denials": [{"tool_name": "Bash"}, {"tool_name": "Write"}]})
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        Path(f"{self.stream}.2").write_text(denied + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "2")

        self.assertEqual(0, code, err)
        denials = [line for line in lines if line.startswith("DENIAL")]
        self.assertEqual(['DENIAL iteration 2: ["Bash","Write"]'], denials, f"monitor lines: {lines}")

    def test_iteration_without_denials_prints_no_denial_line(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual([], [line for line in lines if line.startswith("DENIAL")], f"monitor lines: {lines}")

    def test_denial_line_follows_the_error_line_of_a_failed_iteration(self):
        failed = json.dumps({"type": "result", "subtype": "success", "is_error": True, "result": "stuck",
                             "num_turns": 2, "total_cost_usd": 0.1, "permission_denials": [{"tool_name": "Bash"}]})
        self.stream.write_text(failed + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual(["ERROR iteration 1 failed: is_error set, success", 'DENIAL iteration 1: ["Bash"]'], lines[:2])

    def test_non_zero_exit_with_a_valid_result_counts_as_failed(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1", STUB_CLAUDE_EXIT="1")

        self.assertEqual(0, code, err)
        self.assertEqual("ERROR iteration 1 failed: exit 1, success", lines[0])

    def test_iteration_end_line_names_the_tickets_and_prs_the_iteration_wrote(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.work_data.write_text('{"tickets": [7, 8], "prs": [12]}', encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1", STUB_CLAUDE_DATA=str(self.work_data))

        self.assertEqual(0, code, err)
        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25, tickets #7 #8, PRs #12", lines[0])

    def test_iteration_end_line_says_none_for_an_empty_list_of_tickets_or_prs(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.work_data.write_text('{"tickets": [], "prs": [12]}', encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1", STUB_CLAUDE_DATA=str(self.work_data))

        self.assertEqual(0, code, err)
        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25, tickets none, PRs #12", lines[0])

    def test_iteration_end_line_without_a_data_file_is_the_line_without_tickets_and_prs(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def test_malformed_iteration_data_falls_back_to_the_line_without_tickets_and_prs_and_fails_nothing(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        for data in ("not json {", '{"tickets": "7", "prs": []}', '{"tickets": [7]}', '{"tickets": [0], "prs": []}',
                     '["7"]'):
            with self.subTest(data=data):
                shutil.rmtree(self.home, ignore_errors=True)
                self.work_data.write_text(data, encoding="utf-8")

                code, lines, err = self.run_loop("--max-iterations", "1", STUB_CLAUDE_DATA=str(self.work_data))

                self.assertEqual(0, code, err)
                self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                                  "END after 1 iterations: max_iterations 1 reached (1 ok, 0 failed)"],
                                 self.without_retro(lines))

    def test_each_iteration_names_only_the_tickets_and_prs_it_wrote(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        Path(f"{self.work_data}.1").write_text('{"tickets": [7], "prs": []}', encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "2", STUB_CLAUDE_DATA=str(self.work_data))

        self.assertEqual(0, code, err)
        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25, tickets #7, PRs none",
                          "iteration 2 ok: 3 turns, cost $0.25",
                          "END after 2 iterations: max_iterations 2 reached (2 ok, 0 failed)"],
                         self.without_retro(lines))

    def test_data_of_an_iteration_cut_off_by_the_usage_limit_is_not_read_by_its_retry(self):
        self.limit_then_success()
        Path(f"{self.work_data}.1").write_text('{"tickets": [7], "prs": [12]}', encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1", RALPH_LIMIT_RETRY_SECONDS="2",
                                         STUB_CLAUDE_DATA=str(self.work_data))

        self.assertEqual(0, code, err)
        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: max_iterations 1 reached (1 ok, 0 failed)"],
                         self.without_retro(lines)[2:])

    def test_missing_flock_is_reported_as_such(self):
        tools = self.root / "tools"
        tools.mkdir()
        # Only the programs loop.sh and its issue-status helper use, found where this machine keeps them, minus flock.
        for name in ("bash", "jq", "date", "mkdir", "cat", "dirname", "head", "tr", "grep", "tail", "touch", "rm", "mv",
                     "sleep", "python3", "mktemp"):
            found = shutil.which(name)
            self.assertIsNotNone(found, f"{name} is needed by the test")
            (tools / name).symlink_to(found)
        for stub in ("claude", "gh"):
            (tools / stub).symlink_to(self.root / "bin" / stub)
        process = subprocess.run(
            [str(tools / "bash"), str(self.repo / SKILL_SCRIPTS / "loop.sh"), "42", "--max-iterations", "1"],
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
        process, sleep_pid_file = self.start_sleeping(60, "--max-iterations", "1")
        child = int((self.root / "child.pid").read_text(encoding="utf-8").strip())

        process.terminate()
        process.communicate(timeout=30)

        self.assertEqual(0, subprocess.run(["flock", "-n", str(self.home / "locks" / "42.lock"), "true"]).returncode,
                         "the lock outlived the killed loop")
        self.wait_until(lambda: not self.is_running(child), "the child to end")
        self.assertFalse(self.is_running(int(sleep_pid_file.read_text(encoding="utf-8").strip())),
                         "the child's own child outlived the killed loop")

    def test_error_result_with_exit_zero_counts_as_failed(self):
        self.stream.write_text(ERROR_RESULT + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")
        lines = self.without_retro(lines)

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
        lock = self.home / "locks" / "42.lock"
        self.wait_until(lambda: subprocess.run(["flock", "-n", str(lock), "true"]).returncode != 0, "the lock holder")

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
        lines = self.without_retro(lines)

        self.assertEqual(0, code, err)
        self.assertEqual(["END after 0 iterations: no ticket ready for an agent (0 ok, 0 failed)"], lines)

    def test_retro_ticket_accepted_in_the_pre_run_interview_is_no_work_for_the_starting_run(self):
        """An accepted retro ticket is `retro` and `ready-for-agent` but no sub-issue of the spec until the next start
        attaches it (interview.py attach); the work check must not count it as free frontier work."""
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        accepted = [{"number": 61, "title": "Retro", "state": "open", "html_url": "u61",
                     "labels": [{"name": "retro"}, {"name": "ready-for-agent"}]}]
        (self.gh_dir / "issues").write_text(json.dumps([accepted]), encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "3", STUB_GH_EMPTY="1")
        lines = self.without_retro(lines)

        self.assertEqual(0, code, err)
        self.assertEqual(["END after 0 iterations: no ticket ready for an agent (0 ok, 0 failed)"], lines)
        started = self.calls.read_text(encoding="utf-8").count("-p /implement-spec") if self.calls.exists() else 0
        self.assertEqual(0, started, "an iteration started for the accepted retro ticket")

    def test_stop_request_in_the_control_file_ends_the_run_before_the_next_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        run = self.home / "runs" / "42-20260101-000000"
        run.mkdir(parents=True)
        (run / "control.json").write_text(json.dumps({"max_iterations": 5, "stop": "after-step"}), encoding="utf-8")

        code, lines, err = self.run_loop()
        lines = self.without_retro(lines)

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
        for option, value in (("--max-iterations", "0"), ("--max-duration", "0"), ("--max-duration", "1h"), ("--max-iteration-duration", "-5"),
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
        for key, value in (("max_duration", "soon"), ("max_iterations", 0), ("max_iteration_duration", 0), ("max_parallel", 5),
                           ("max_iterations", 1.5), ("limit_stop", "later"), ("stop", "whenever"),
                           ("until", "tomorrow")):
            self.write_control(**{key: value})
            code, lines, err = self.run_loop()
            self.assertEqual(2, code, (key, value))
            self.assertTrue(lines[0].startswith(f"ERROR {key}"), lines)
            self.assertFalse((self.home / "runs" / "42-20260101-000000" / "ended").exists(), key)
            shutil.rmtree(self.home / "runs")
        self.assertFalse(self.calls.exists())

    def test_missing_spec_is_reported_as_an_error_line(self):
        process = subprocess.run(["bash", str(self.repo / SKILL_SCRIPTS / "loop.sh")], cwd=self.repo, env=self.env(),
                                 capture_output=True, text=True, timeout=60)

        self.assertEqual(2, process.returncode)
        self.assertTrue(process.stdout.startswith("ERROR usage"), process.stdout)
        self.assertIn("--resume-only", process.stdout, "the usage line omits --resume-only")

    def test_until_without_time_zone_data_is_refused_instead_of_read_as_utc(self):
        empty = self.root / "no-tzdata"
        empty.mkdir()

        code, lines, err = self.run_loop("--until", "2999-01-01 06:00", TZDIR=str(empty), STUB_GH_EMPTY="1")

        self.assertEqual(2, code)
        self.assertTrue(lines[0].startswith("ERROR until names a time zone this machine does not know"), lines)

    def test_wrong_timing_knob_is_refused(self):
        code, lines, err = self.run_loop(RALPH_GRACE_SECONDS="soon")

        self.assertEqual(2, code)
        self.assertTrue(lines[0].startswith("ERROR RALPH_GRACE_SECONDS"), lines)

    def test_until_in_the_past_ends_the_run_before_the_first_iteration(self):
        self.write_control(until="2000-01-01 00:00")

        code, lines, err = self.run_loop()
        lines = self.without_retro(lines)

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
        lines = self.without_retro(lines)

        self.assertEqual(f"END after 0 iterations: until {past_in_berlin} reached (0 ok, 0 failed)", lines[0])

    def test_until_reads_the_wall_clock_in_the_named_zone(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.write_control(until=self.berlin_wall_clock(-30) + " America/New_York", max_iterations=1)

        code, lines, err = self.run_loop()

        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def test_max_duration_ends_the_run_before_the_next_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        process, _ = self.start_sleeping(6, "--max-duration", "1", RALPH_MINUTE_SECONDS="5")
        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: max_duration 1 reached (1 ok, 0 failed)"], out)

    def test_reached_limit_writes_the_configured_stop_kind_into_the_control_file(self):
        self.write_control(until="2000-01-01 00:00", limit_stop="after-iteration")

        self.run_loop()

        self.assertEqual("after-iteration", self.read_control()["stop"])

    def test_reached_limit_with_stop_kind_now_ends_the_run_between_iterations(self):
        self.write_control(until="2000-01-01 00:00", limit_stop="now")

        code, lines, err = self.run_loop()

        self.assertEqual(0, code, err)
        self.assertEqual(["END after 0 iterations: until 2000-01-01 00:00 reached (0 ok, 0 failed)"], lines)
        self.assertEqual("now", self.read_control()["stop"])

    def test_reached_limit_stops_after_the_step_by_default(self):
        self.write_control(until="2000-01-01 00:00")

        self.run_loop()

        self.assertEqual("after-step", self.read_control()["stop"])

    def test_max_iteration_duration_writes_the_configured_stop_kind_and_ends_the_run_after_the_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(4, "--max-iteration-duration", "1", "--limit-stop", "after-iteration",
                                         RALPH_GRACE_SECONDS="30")
        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertEqual("after-iteration", self.read_control()["stop"])
        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: stop requested (after-iteration) (1 ok, 0 failed)"], out)

    def test_max_iteration_duration_kills_the_iteration_after_the_grace_period(self):
        process, sleep_pid_file = self.start_sleeping(60, "--max-iteration-duration", "1")
        sleep_pid = int(sleep_pid_file.read_text(encoding="utf-8").strip())

        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertFalse(self.is_running(sleep_pid))
        self.assertEqual("STOP iteration 1 killed (max_iteration_duration 1 reached and grace period over)", out[0])
        self.assertEqual("after-step", self.read_control()["stop"])
        self.assertEqual("END after 1 iterations: stop requested (after-step) (0 ok, 1 failed)", out[1])

    def test_max_iteration_duration_with_stop_kind_now_kills_without_the_grace_period(self):
        process, sleep_pid_file = self.start_sleeping(60, "--max-iteration-duration", "1", "--limit-stop", "now",
                                                      RALPH_GRACE_SECONDS="600")
        sleep_pid = int(sleep_pid_file.read_text(encoding="utf-8").strip())

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertFalse(self.is_running(sleep_pid))
        self.assertEqual("STOP iteration 1 killed (stop now)", out[0])

    def test_stop_request_after_iteration_lets_the_running_iteration_finish(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(2, "--max-iterations", "5")
        self.edit_control(stop="after-iteration")

        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: stop requested (after-iteration) (1 ok, 0 failed)"], out)

    def test_stop_request_after_step_lets_the_running_iteration_finish(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(2, "--max-iterations", "5")
        self.edit_control(stop="after-step")

        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: stop requested (after-step) (1 ok, 0 failed)"], out)

    def test_stop_request_without_a_kind_means_after_step(self):
        self.write_control(stop=True)

        code, lines, err = self.run_loop()
        lines = self.without_retro(lines)

        self.assertEqual(["END after 0 iterations: stop requested (after-step) (0 ok, 0 failed)"], lines)

    def test_stop_request_now_kills_the_process_group_of_the_child(self):
        process, sleep_pid_file = self.start_sleeping(60, "--max-iterations", "5")
        sleep_pid = int(sleep_pid_file.read_text(encoding="utf-8").strip())
        stub_pid = int((self.root / "child.pid").read_text(encoding="utf-8").strip())
        self.edit_control(stop="now")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertFalse(self.is_running(sleep_pid), "the grandchild survived")
        self.assertFalse(self.is_running(stub_pid), "the child survived")
        self.assertEqual("STOP iteration 1 killed (stop now)", out[0])
        self.assertEqual("END after 1 iterations: stop requested (now) (0 ok, 1 failed)", out[1])

    # Work check before an iteration: free tickets and PRs that need action start one, PRs waiting for CI with
    # auto-merge make the script sleep and check again, nothing at all ends the run. The spec's only ticket (#7) is
    # claimed in these tests (not free) unless a test says otherwise.

    NO_WORK = ["END after 0 iterations: no ticket ready for an agent (0 ok, 0 failed)"]

    def pr(self, number=5, branch="ticket-7", sha="abc", auto_merge=True, state="clean", check_conclusions=("success",),
           comments=(), replies=(), reviews=(), commit_date="2026-01-01T00:00:00Z"):
        """An open PR as the stub gh serves it, every list as one page; a check conclusion of None is a run still
        going, none at all is a PR whose CI has not started; replies are review comments in reply to another."""
        pulls_file = self.gh_dir / "pulls"
        pulls = json.loads(pulls_file.read_text(encoding="utf-8"))[0] if pulls_file.exists() else []
        pulls.append({"number": number, "head": {"ref": branch, "sha": sha}})
        self.serve("pulls", [pulls])
        self.serve(f"pulls_{number}", {"number": number, "mergeable_state": state, "head": {"ref": branch, "sha": sha},
                                       "auto_merge": {"merge_method": "squash"} if auto_merge else None})
        self.serve(f"commits_{sha}_check-runs", [{"total_count": len(check_conclusions), "check_runs": [
            {"status": "completed" if c else "in_progress", "conclusion": c} for c in check_conclusions]}])
        self.serve(f"commits_{sha}", {"commit": {"committer": {"date": commit_date}}})
        self.serve(f"pulls_{number}_comments", [
            [{"created_at": c, "body": "please change", "in_reply_to_id": None} for c in comments]
            + [{"created_at": c, "body": "done", "in_reply_to_id": 1} for c in replies]])
        self.serve(f"pulls_{number}_reviews", [list(reviews)])

    def serve(self, name, data):
        (self.gh_dir / name).write_text(json.dumps(data), encoding="utf-8")

    def pr_checks(self):
        log = self.gh_dir / "calls.log"
        return log.read_text(encoding="utf-8").splitlines().count("pulls") if log.exists() else 0

    def start_with_pr_sleep(self, seconds="1", **extra):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        return self.start("--max-iterations", "1", STUB_GH_CLAIMED="1", RALPH_PR_SLEEP_SECONDS=seconds, **extra)

    def run_with_pr_sleep(self, **extra):
        process = self.start_with_pr_sleep(**extra)
        out, err = process.communicate(timeout=60)
        return process.returncode, self.without_retro(out.splitlines()), err

    def assert_iteration_started(self, outcome):
        code, lines, err = outcome
        self.assertEqual(0, code, err)
        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def assert_sleeping_without_claude(self, process, checks=2):
        self.wait_until(lambda: self.pr_checks() >= checks, f"{checks} checks of the PRs")
        self.assertIsNone(process.poll(), "the script ended instead of waiting")
        self.assertFalse(self.calls.exists(), "a claude process was started while PRs wait")
        process.terminate()
        process.communicate(timeout=30)

    def test_pr_with_red_ci_starts_an_iteration(self):
        self.pr(check_conclusions=("success", "failure"))

        self.assert_iteration_started(self.run_with_pr_sleep())

    def test_pr_with_a_merge_conflict_starts_an_iteration(self):
        self.pr(state="dirty")

        self.assert_iteration_started(self.run_with_pr_sleep())

    def test_pr_with_a_review_comment_after_its_last_commit_starts_an_iteration(self):
        self.pr(commit_date="2026-01-01T00:00:00Z", comments=["2026-01-01T00:05:00Z"])

        self.assert_iteration_started(self.run_with_pr_sleep())

    def test_pr_with_a_review_after_its_last_commit_starts_an_iteration(self):
        self.pr(commit_date="2026-01-01T00:00:00Z",
                reviews=[{"state": "CHANGES_REQUESTED", "body": "", "submitted_at": "2026-01-01T00:05:00Z"}])

        self.assert_iteration_started(self.run_with_pr_sleep())

    def test_review_comment_before_the_last_commit_needs_no_action(self):
        self.pr(commit_date="2026-01-01T00:10:00Z", comments=["2026-01-01T00:05:00Z"], check_conclusions=(None,))

        self.assert_sleeping_without_claude(self.start_with_pr_sleep())

    def test_approval_without_text_needs_no_action(self):
        self.pr(reviews=[{"state": "APPROVED", "body": "", "submitted_at": "2026-01-01T00:05:00Z"}],
                check_conclusions=(None,))

        self.assert_sleeping_without_claude(self.start_with_pr_sleep())

    def test_red_pr_of_a_ticket_outside_the_spec_is_ignored(self):
        self.pr(branch="ticket-99", check_conclusions=("failure",))

        self.assertEqual((0, self.NO_WORK), self.run_with_pr_sleep()[:2])

    def test_red_pr_of_a_branch_that_is_not_a_ticket_branch_is_ignored(self):
        self.pr(branch="feature-7", check_conclusions=("failure",))

        self.assertEqual((0, self.NO_WORK), self.run_with_pr_sleep()[:2])

    def test_pr_waiting_for_ci_with_auto_merge_makes_the_script_sleep_without_starting_claude(self):
        self.pr(check_conclusions=("success", None))

        self.assert_sleeping_without_claude(self.start_with_pr_sleep())

    def test_sleeping_script_reports_the_waiting_state(self):
        self.pr(check_conclusions=(None,))
        process = self.start_with_pr_sleep()

        def state():
            status = self.home.glob("runs/*/status.json")
            return next((json.loads(f.read_text(encoding="utf-8"))["state"] for f in status), None)

        self.wait_until(lambda: state() == "waiting", "the waiting state")
        process.terminate()

    def test_sleeping_script_checks_the_PRs_again_after_each_sleep(self):
        self.pr(check_conclusions=(None,))

        self.assert_sleeping_without_claude(self.start_with_pr_sleep(), checks=3)

    def test_run_ends_when_the_waiting_PR_is_merged(self):
        self.pr(check_conclusions=(None,))
        process = self.start_with_pr_sleep()
        self.wait_until(lambda: self.pr_checks() >= 2, "a second check of the PRs")
        self.serve("pulls", [[]])

        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertEqual(self.NO_WORK, out)
        self.assertNotIn("/implement-spec", self.calls.read_text(encoding="utf-8"), "an iteration was started")

    def test_waiting_PR_turning_red_starts_an_iteration(self):
        self.pr(check_conclusions=(None,))
        process = self.start_with_pr_sleep()
        self.wait_until(lambda: self.pr_checks() >= 2, "a second check of the PRs")
        self.serve("commits_abc_check-runs", [{"check_runs": [{"status": "completed", "conclusion": "failure"}]}])

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", out[0])

    def test_free_ticket_starts_an_iteration_although_a_PR_waits(self):
        self.pr(check_conclusions=(None,))
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        lines = self.run_loop("--max-iterations", "1", RALPH_PR_SLEEP_SECONDS="1")[1]

        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def test_green_PR_without_auto_merge_does_not_make_the_script_wait(self):
        self.pr(auto_merge=False, check_conclusions=("success",))

        self.assertEqual((0, self.NO_WORK), self.run_with_pr_sleep()[:2])

    def test_stop_request_ends_a_sleeping_script_before_the_sleep_is_over(self):
        self.pr(check_conclusions=(None,))
        process = self.start_with_pr_sleep("600")
        self.wait_until(lambda: self.pr_checks() >= 1, "the first check of the PRs")
        self.edit_control(stop="after-step")

        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertEqual(["END after 0 iterations: stop requested (after-step) (0 ok, 0 failed)"], out)

    def test_reached_limit_ends_a_sleeping_script_within_a_poll_slice(self):
        self.pr(check_conclusions=(None,))
        process = self.start("--max-duration", "2", STUB_GH_CLAIMED="1", RALPH_PR_SLEEP_SECONDS="600")

        out = process.communicate(timeout=30)[0].splitlines()
        out = self.without_retro(out)

        self.assertEqual(["END after 0 iterations: max_duration 2 reached (0 ok, 0 failed)"], out)

    def test_failing_PR_check_ends_the_run_with_an_error_line(self):
        (self.gh_dir / "fail_pulls").write_text("", encoding="utf-8")

        code, lines, err = self.run_with_pr_sleep()

        self.assertEqual(0, code, err)
        self.assertTrue(lines[0].startswith("ERROR PR check for #42 failed: "), lines)
        self.assertEqual("END after 0 iterations: PR check failed (0 ok, 0 failed)", lines[1])

    def test_green_PR_that_is_blocked_is_neither_action_nor_waiting(self):
        self.pr(state="blocked", check_conclusions=("success",))

        self.assertEqual((0, self.NO_WORK), self.run_with_pr_sleep()[:2])

    def test_green_PR_that_is_behind_is_neither_action_nor_waiting(self):
        self.pr(state="behind", check_conclusions=("success",))

        self.assertEqual((0, self.NO_WORK), self.run_with_pr_sleep()[:2])

    def test_PR_with_auto_merge_and_completed_checks_of_unknown_merge_state_is_not_waiting(self):
        self.pr(state=None, check_conclusions=("success",))

        self.assertEqual((0, self.NO_WORK), self.run_with_pr_sleep()[:2])

    def test_PR_with_auto_merge_and_pending_checks_of_unknown_merge_state_is_waiting(self):
        self.pr(state=None, check_conclusions=(None,))

        self.assert_sleeping_without_claude(self.start_with_pr_sleep())

    def test_PR_with_auto_merge_whose_checks_have_not_started_is_waiting(self):
        self.pr(check_conclusions=())

        self.assert_sleeping_without_claude(self.start_with_pr_sleep())

    def test_red_check_run_on_the_second_page_starts_an_iteration(self):
        self.pr()
        self.serve("commits_abc_check-runs", [{"check_runs": [{"status": "completed", "conclusion": "success"}]},
                                              {"check_runs": [{"status": "completed", "conclusion": "failure"}]}])

        self.assert_iteration_started(self.run_with_pr_sleep())

    def test_review_comment_on_the_second_page_starts_an_iteration(self):
        self.pr()
        self.serve("pulls_5_comments", [[{"created_at": "2025-12-31T00:00:00Z", "in_reply_to_id": None}],
                                        [{"created_at": "2026-01-01T00:05:00Z", "in_reply_to_id": None}]])

        self.assert_iteration_started(self.run_with_pr_sleep())

    def test_review_on_the_second_page_starts_an_iteration(self):
        self.pr()
        self.serve("pulls_5_reviews", [[], [{"state": "CHANGES_REQUESTED", "body": "", "submitted_at": "2026-01-01T00:05:00Z"}]])

        self.assert_iteration_started(self.run_with_pr_sleep())

    def test_red_PR_on_the_second_page_of_the_open_PRs_starts_an_iteration(self):
        self.pr(branch="feature-1", check_conclusions=("success",))
        self.pr(number=6, sha="def", check_conclusions=("failure",))
        pulls = json.loads((self.gh_dir / "pulls").read_text(encoding="utf-8"))[0]
        self.serve("pulls", [[pulls[0]], [pulls[1]]])

        self.assert_iteration_started(self.run_with_pr_sleep())

    def assert_pr_check_failed(self, outcome):
        code, lines, err = outcome
        self.assertEqual(0, code, err)
        self.assertTrue(lines[0].startswith("ERROR PR check for #42 failed"), lines)
        self.assertEqual("END after 0 iterations: PR check failed (0 ok, 0 failed)", lines[1])

    def test_check_runs_answer_without_check_runs_fails_the_check_instead_of_passing_it(self):
        self.pr()
        self.serve("commits_abc_check-runs", [{"message": "Server Error"}])

        self.assert_pr_check_failed(self.run_with_pr_sleep())

    def test_review_comments_answer_that_is_not_a_list_fails_the_check_instead_of_passing_it(self):
        self.pr()
        self.serve("pulls_5_comments", [{"message": "Server Error"}])

        self.assert_pr_check_failed(self.run_with_pr_sleep())

    def test_reviews_answer_that_is_not_a_list_fails_the_check_instead_of_passing_it(self):
        self.pr()
        self.serve("pulls_5_reviews", [{"message": "Server Error"}])

        self.assert_pr_check_failed(self.run_with_pr_sleep())

    def test_reply_to_a_review_comment_after_the_last_commit_needs_no_action(self):
        self.pr(replies=["2026-01-01T00:05:00Z"], check_conclusions=(None,))

        self.assert_sleeping_without_claude(self.start_with_pr_sleep())

    def test_hanging_gh_ends_the_run_with_an_error_line_after_the_timeout(self):
        (self.gh_dir / "hang_pulls").write_text("", encoding="utf-8")

        self.assert_pr_check_failed(self.run_with_pr_sleep(RALPH_GH_TIMEOUT_SECONDS="1"))

    def test_wrong_gh_timeout_knob_is_refused(self):
        code, lines, err = self.run_loop(RALPH_GH_TIMEOUT_SECONDS="0")

        self.assertEqual(2, code, err)
        self.assertEqual(["ERROR RALPH_GH_TIMEOUT_SECONDS needs a positive integer, got '0'"], lines)

    def test_wrong_pr_sleep_knob_is_refused(self):
        code, lines, err = self.run_loop(RALPH_PR_SLEEP_SECONDS="soon")

        self.assertEqual(2, code, err)
        self.assertEqual(["ERROR RALPH_PR_SLEEP_SECONDS needs a positive integer, got 'soon'"], lines)

    def call_times(self):
        times = Path(f"{self.calls}.times")
        return [float(line) for line in times.read_text(encoding="utf-8").split()] if times.exists() else []

    def limit_then_success(self, exit_code="1"):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        Path(f"{self.stream}.1").write_text(USAGE_LIMIT + "\n", encoding="utf-8")
        Path(f"{self.stream}.1.exit").write_text(exit_code, encoding="utf-8")

    def test_usage_limit_waits_for_the_retry_and_runs_the_iteration_again(self):
        self.limit_then_success()

        code, lines, err = self.run_loop("--max-iterations", "1", RALPH_LIMIT_RETRY_SECONDS="2")
        lines = self.without_retro(lines)

        self.assertEqual(0, code, err)
        self.assertEqual(4, len(lines), lines)
        self.assertRegex(lines[0], r"^WAIT usage limit hit in iteration 1: no claude process until the retry in 2 s")
        self.assertRegex(lines[1], r"^WAIT over, retrying")
        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25",
                          "END after 1 iterations: max_iterations 1 reached (1 ok, 0 failed)"], lines[2:])
        first, second = self.call_times()
        self.assertGreaterEqual(second - first, 2 - CLOCK_RESOLUTION, f"call times {first}, {second}")

    def test_usage_limit_is_recognised_from_the_result_not_the_exit_code(self):
        self.limit_then_success(exit_code="0")

        code, lines, err = self.run_loop("--max-iterations", "1", RALPH_LIMIT_RETRY_SECONDS="1")

        self.assertEqual(0, code, err)
        self.assertTrue(lines[0].startswith("WAIT usage limit hit"), lines)

    def test_failed_iteration_with_a_non_zero_exit_is_not_a_usage_limit(self):
        self.stream.write_text(ERROR_RESULT + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1", STUB_CLAUDE_EXIT="1", RALPH_LIMIT_RETRY_SECONDS="1",
                                         seconds=8)

        self.assertEqual(0, code, err)
        self.assertEqual("ERROR iteration 1 failed: exit 1, success", lines[0])
        self.assertEqual(1, len(self.call_times()))

    def test_usage_limit_counts_neither_as_an_iteration_nor_as_a_failure(self):
        self.limit_then_success()

        code, lines, err = self.run_loop("--max-iterations", "1", RALPH_LIMIT_RETRY_SECONDS="1")

        self.assertEqual("END after 1 iterations: max_iterations 1 reached (1 ok, 0 failed)", lines[-1])
        self.assertEqual(2, len(self.call_times()))

    def test_usage_limit_that_does_not_lift_ends_the_run_cleanly_at_the_cap(self):
        self.stream.write_text(USAGE_LIMIT + "\n", encoding="utf-8")

        code, lines, err = self.run_loop(RALPH_LIMIT_RETRY_SECONDS="1", RALPH_LIMIT_CAP_SECONDS="3", seconds=15)

        self.assertEqual(0, code, err)
        self.assertEqual("END after 0 iterations: usage limit not lifted within 3 s (0 ok, 0 failed)", lines[-1])
        self.assertEqual(3, len([line for line in lines if line.startswith("WAIT usage limit")]), lines)
        self.assertEqual(3, len([line for line in lines if line.startswith("WAIT over")]), lines)
        times = self.call_times()
        self.assertEqual(4, len(times), times)
        self.assertGreaterEqual(times[-1] - times[0], 3 - CLOCK_RESOLUTION, f"call times {times}")

    def test_cap_is_five_hours_and_fifteen_minutes_by_default(self):
        self.limit_then_success()

        code, lines, err = self.run_loop("--max-iterations", "1", RALPH_LIMIT_RETRY_SECONDS="1", RALPH_MINUTE_SECONDS="2")

        self.assertEqual(0, code, err)
        self.assertIn("(waited 0 of 630 s)", lines[0])

    def test_stop_request_ends_the_usage_limit_wait(self):
        self.stream.write_text(USAGE_LIMIT + "\n", encoding="utf-8")
        process = self.start(RALPH_LIMIT_RETRY_SECONDS="60")
        self.wait_until(lambda: self.call_times(), "the first iteration")
        self.wait_until(lambda: (self.home / "runs").exists() and any((self.home / "runs").glob("*/limit-*")),
                        "the usage limit wait")
        self.edit_control(stop="after-step")

        out, err = self.finish(process, 20)

        self.assertEqual(0, process.returncode, err)
        self.assertRegex(out, r"WAIT over, stop requested")
        self.assertIn("END after 0 iterations: stop requested (after-step)", out)

    def test_wrong_usage_limit_knobs_are_refused(self):
        code, lines, err = self.run_loop(RALPH_LIMIT_RETRY_SECONDS="0", seconds=8)
        self.assertEqual(2, code, err)
        self.assertEqual(["ERROR RALPH_LIMIT_RETRY_SECONDS needs a positive integer, got '0'"], lines)
        code, lines, err = self.run_loop(RALPH_LIMIT_CAP_SECONDS="soon", seconds=8)
        self.assertEqual(2, code, err)
        self.assertEqual(["ERROR RALPH_LIMIT_CAP_SECONDS needs a positive integer, got 'soon'"], lines)

    def seed_run(self, outcomes, ended=False, **control):
        """An earlier run of the spec: one result file per iteration, its outcome file where the outcome is given
        (ok or failed; None leaves it out, as a script that died between the result and the outcome would)."""
        run = self.home / "runs" / "42-20260101-000000"
        run.mkdir(parents=True)
        (run / "control.json").write_text(json.dumps(control), encoding="utf-8")
        (run / "started").write_text(str(int(time.time())), encoding="utf-8")
        for number, outcome in enumerate(outcomes, start=1):
            (run / f"result-{number}.json").write_text(SUCCESS + "\n", encoding="utf-8")
            if outcome:
                (run / f"outcome-{number}").write_text(outcome + "\n", encoding="utf-8")
        if ended:
            (run / "ended").touch()
        return run

    def read_status(self, run):
        return json.loads((run / "status.json").read_text(encoding="utf-8"))

    def test_new_start_continues_the_unfinished_run_without_repeating_finished_iterations(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        run = self.seed_run(["ok", "ok"], max_iterations=3)

        code, lines, err = self.run_loop()

        self.assertEqual(0, code, err)
        self.assertEqual(1, self.calls.read_text(encoding="utf-8").count("-p /implement-spec 42"),
                         f"finished iterations were repeated; monitor lines: {lines}")
        self.assertEqual(run, self.only_run_dir(), "the restart started a second run directory")
        self.assertTrue((run / "result-3.json").exists(), "the restart did not continue with iteration 3")

    def test_resume_recovers_the_ok_and_failed_counts_from_the_finished_iterations(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        run = self.seed_run(["ok", "failed"], max_iterations=3)

        code, lines, err = self.run_loop()

        self.assertEqual(0, code, err)
        self.assertEqual("END after 3 iterations: max_iterations 3 reached (2 ok, 1 failed)", lines[-1])
        status = self.read_status(run)
        self.assertEqual((3, 2, 1), (status["iteration"], status["ok"], status["failed"]),
                         f"status.json after the resume: {status}")

    def test_resume_counts_an_iteration_without_a_recorded_outcome_as_failed(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        run = self.seed_run(["ok", None], max_iterations=2)

        code, lines, err = self.run_loop()

        self.assertEqual(0, code, err)
        status = self.read_status(run)
        self.assertEqual(status["iteration"], status["ok"] + status["failed"], f"status.json: {status}")
        self.assertEqual("END after 2 iterations: max_iterations 2 reached (1 ok, 1 failed)", lines[-1])

    def test_outcomes_of_new_iterations_are_recorded_for_a_later_resume(self):
        self.stream.write_text(ERROR_RESULT + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        outcome = self.only_run_dir() / "outcome-1"
        self.assertTrue(outcome.exists(), "no outcome file after a failed iteration")
        self.assertEqual("failed", outcome.read_text(encoding="utf-8").strip())

    def test_start_after_a_finished_run_begins_a_new_run_and_leaves_the_finished_one_alone(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        finished = self.seed_run(["ok", "ok"], ended=True, max_iterations=2)

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        runs = sorted((self.home / "runs").iterdir())
        self.assertEqual(2, len(runs), f"runs: {runs}; monitor lines: {lines}")
        self.assertFalse((finished / "status.json").exists(), "the finished run was written to")
        self.assertEqual("iteration 1 ok: 3 turns, cost $0.25", lines[0])

    def test_resume_only_refuses_a_finished_run(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        finished = self.seed_run(["ok"], ended=True, max_iterations=3)

        code, lines, err = self.run_loop("--resume-only")

        self.assertEqual(1, code, f"{err}; monitor lines: {lines}")
        self.assertEqual(["ERROR run of #42 already ended, nothing to resume"], lines)
        self.assertFalse(self.calls.exists(), "an iteration started on a finished run")
        self.assertEqual([finished], list((self.home / "runs").iterdir()), "resume-only created a run directory")

    def test_resume_only_without_any_run_refuses(self):
        code, lines, err = self.run_loop("--resume-only")

        self.assertEqual(1, code, f"{err}; monitor lines: {lines}")
        self.assertEqual(["ERROR no run of #42 to resume"], lines)
        self.assertFalse((self.home / "runs").exists() and any((self.home / "runs").iterdir()),
                         "resume-only created a run directory")

    def test_resume_only_continues_the_unfinished_run(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        run = self.seed_run(["ok"], max_iterations=2)

        code, lines, err = self.run_loop("--resume-only")
        lines = self.without_retro(lines)

        self.assertEqual(0, code, err)
        self.assertEqual(["iteration 2 ok: 3 turns, cost $0.25",
                          "END after 2 iterations: max_iterations 2 reached (2 ok, 0 failed)"], lines)
        self.assertEqual(run, self.only_run_dir())

    def test_resume_only_is_refused_while_the_run_is_still_running(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, sleep_pid_file = self.start_sleeping(3, "--max-iterations", "1")

        code, lines, err = self.run_loop("--resume-only", seconds=20)

        self.assertEqual(1, code, f"{err}; monitor lines: {lines}")
        self.assertIn("already has a running loop", lines[0])
        self.finish(process, 30)

    # The retro iteration (#418): the last iteration of a run that ends on a stop after-step or after-iteration, a
    # reached limit acting as one of them, or an empty frontier; never after a stop now. It is not counted in the
    # iterations or the ok and failed counts of the run.

    RETRO_OK = "retro iteration ok: 3 turns, cost $0.25"

    @staticmethod
    def without_retro(lines):
        """The monitor lines of the run without those of the retro iteration, for the tests of other behaviour."""
        return [line for line in lines if not re.match(r"retro iteration ok|ERROR retro failed|DENIAL retro", line)]

    def retro_calls(self):
        return [line for line in self.calls.read_text(encoding="utf-8").splitlines() if line.startswith("-p /retro")] \
            if self.calls.exists() else []

    def retro_option(self, name):
        arguments = Path(f"{self.calls}.retro.argv").read_bytes().decode("utf-8").split("\0")[:-1]
        self.assertIn(name, arguments, f"the retro was started without {name}; its arguments: {arguments}")
        return arguments[arguments.index(name) + 1]

    def test_retro_runs_as_the_last_iteration_after_a_stop_after_step(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(2, "--max-iterations", "5")
        self.edit_control(stop="after-step")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25", self.RETRO_OK,
                          "END after 1 iterations: stop requested (after-step) (1 ok, 0 failed)"], out)
        self.assertEqual(1, len(self.retro_calls()), self.calls.read_text(encoding="utf-8"))

    def test_retro_runs_as_the_last_iteration_after_a_stop_after_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _ = self.start_sleeping(2, "--max-iterations", "5")
        self.edit_control(stop="after-iteration")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25", self.RETRO_OK,
                          "END after 1 iterations: stop requested (after-iteration) (1 ok, 0 failed)"], out)

    def test_retro_runs_after_an_empty_frontier(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop(STUB_GH_EMPTY="1")

        self.assertEqual(0, code, err)
        self.assertEqual([self.RETRO_OK, "END after 0 iterations: no ticket ready for an agent (0 ok, 0 failed)"],
                         lines)

    def test_retro_runs_after_a_reached_limit_acting_as_after_step_or_after_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1")

        self.assertEqual(0, code, err)
        self.assertEqual(["iteration 1 ok: 3 turns, cost $0.25", self.RETRO_OK,
                          "END after 1 iterations: max_iterations 1 reached (1 ok, 0 failed)"], lines)

    def test_retro_runs_after_a_reached_limit_that_acts_as_after_iteration(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        code, lines, err = self.run_loop("--max-iterations", "1", "--limit-stop", "after-iteration")

        self.assertEqual(self.RETRO_OK, lines[1], lines)
        self.assertEqual(1, len(self.retro_calls()))

    def test_no_retro_after_a_stop_now_in_the_control_file(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.write_control(stop="now")

        code, lines, err = self.run_loop()

        self.assertEqual(["END after 0 iterations: stop requested (now) (0 ok, 0 failed)"], lines)
        self.assertEqual([], self.retro_calls(), "a retro started after stop now")

    def test_no_retro_after_a_stop_now_that_killed_an_iteration(self):
        process, sleep_pid_file = self.start_sleeping(60, "--max-iterations", "5")
        self.edit_control(stop="now")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual("STOP iteration 1 killed (stop now)", out[0], out)
        self.assertEqual("END after 1 iterations: stop requested (now) (0 ok, 1 failed)", out[-1])
        self.assertEqual([], self.retro_calls(), "a retro started after stop now")

    def test_no_retro_after_a_reached_limit_acting_as_now(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.write_control(until="2000-01-01 00:00", limit_stop="now")

        code, lines, err = self.run_loop()

        self.assertEqual(["END after 0 iterations: until 2000-01-01 00:00 reached (0 ok, 0 failed)"], lines)
        self.assertEqual([], self.retro_calls(), "a retro started after a limit acting as stop now")

    def test_no_retro_when_the_usage_limit_does_not_lift(self):
        self.stream.write_text(USAGE_LIMIT + "\n", encoding="utf-8")

        code, lines, err = self.run_loop(RALPH_LIMIT_RETRY_SECONDS="1", RALPH_LIMIT_CAP_SECONDS="2", seconds=15)

        self.assertEqual("END after 0 iterations: usage limit not lifted within 2 s (0 ok, 0 failed)", lines[-1])
        self.assertEqual([], self.retro_calls(), "a retro started while the usage limit is on")

    def test_retro_is_started_non_interactively_with_the_loop_permissions_and_the_run_directory(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop(STUB_GH_EMPTY="1")

        run = self.only_run_dir()
        self.assertEqual(f"/retro non-interactive {run}", self.retro_option("-p"))
        self.assertEqual(f"RALPH_RUN_DIR={run}\n", Path(f"{self.calls}.retro").read_text(encoding="utf-8"))
        self.assertEqual("sonnet", self.retro_option("--model"))
        rules = (REPO / SKILL_SCRIPTS / "retro-rules.md").read_text(encoding="utf-8")
        self.assertTrue(self.retro_option("--append-system-prompt").startswith(rules.strip()))
        self.assertIn(f"Run directory: {run}\n", self.retro_option("--append-system-prompt"))
        self.assertTrue(self.retro_option("--append-system-prompt").endswith("\nLast iteration of the run: 0"))
        self.assertEqual("Agent,Bash(./mvnw:*),Bash(gh api:*),Bash(git:*),Bash(grep:*),Bash(head:*),"
                         "Bash(python3 scripts/*),Edit,Read,Skill,Write",
                         ",".join(sorted(self.retro_option("--allowedTools").split(","))))
        settings = json.loads(self.retro_option("--settings"))
        self.assertTrue(settings["disableAllHooks"])
        self.assertIn("Bash(git push origin main*)", settings["permissions"]["deny"])
        self.assertIn("stdin bytes: 0", self.calls.read_text(encoding="utf-8"))

    def test_retro_is_not_counted_as_an_iteration_and_keeps_its_own_files(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")

        self.run_loop("--max-iterations", "1")

        run = self.only_run_dir()
        status = self.read_status(run)
        self.assertEqual((1, 1, 0, "ended"), (status["iteration"], status["ok"], status["failed"], status["state"]))
        self.assertTrue((run / "retro-result.json").exists())
        self.assertTrue((run / "retro-stream.jsonl").exists())
        self.assertTrue((run / "retro-done").exists(), "the finished retro left no retro-done marker")
        self.assertFalse((run / "result-2.json").exists(), "the retro was filed as an iteration")

    def test_failed_retro_prints_an_error_line_and_the_run_still_ends(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        Path(f"{self.stream}.retro").write_text(ERROR_RESULT + "\n", encoding="utf-8")

        code, lines, err = self.run_loop(STUB_GH_EMPTY="1")

        self.assertEqual(0, code, err)
        self.assertEqual(["ERROR retro failed: is_error set, success",
                          "END after 0 iterations: no ticket ready for an agent (0 ok, 0 failed)"], lines)

    def test_denials_of_the_retro_get_a_denial_line(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        Path(f"{self.stream}.retro").write_text(DENIED + "\n", encoding="utf-8")

        code, lines, err = self.run_loop(STUB_GH_EMPTY="1")

        self.assertEqual(["retro iteration ok: 2 turns, cost $0.1", 'DENIAL retro: ["Bash"]'], lines[:2])

    def test_a_restart_after_a_finished_retro_does_not_run_it_again(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        run = self.seed_run(["ok"], max_iterations=1)
        (run / "retro-done").touch()

        code, lines, err = self.run_loop()

        self.assertEqual(["END after 1 iterations: max_iterations 1 reached (1 ok, 0 failed)"], lines)
        self.assertEqual([], self.retro_calls(), "the retro ran twice")

    def test_a_restart_after_a_killed_script_runs_the_retro_that_did_not_finish(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        self.seed_run(["ok"], max_iterations=1)

        code, lines, err = self.run_loop()

        self.assertEqual(self.RETRO_OK, lines[0], lines)
        self.assertEqual(1, len(self.retro_calls()))


    def start_sleeping_retro(self, seconds, *args, **extra):
        """(A sleep that ends by itself soon, so that a retro nobody kills ends the test with a line to compare.) Starts a loop whose frontier is empty, so its retro runs at once and sleeps; returns the process and the
        pids of the retro stub and of its sleep."""
        pid_file = self.root / "retro.pid"
        process = self.start(*args, STUB_GH_EMPTY="1", STUB_RETRO_SLEEP=str(seconds), STUB_RETRO_PID=str(pid_file),
                             **extra)
        sleep_pid_file = Path(f"{pid_file}.sleep")
        self.wait_until(lambda: sleep_pid_file.exists() and sleep_pid_file.read_text(encoding="utf-8").strip(),
                        "the retro to start")
        return process, int(pid_file.read_text(encoding="utf-8").strip()), \
            int(sleep_pid_file.read_text(encoding="utf-8").strip())

    NO_TICKET_END = "END after 0 iterations: no ticket ready for an agent (0 ok, 0 failed)"

    def test_stop_now_written_while_the_retro_runs_kills_its_process_group_and_ends_the_run(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, stub_pid, sleep_pid = self.start_sleeping_retro(5)
        self.edit_control(stop="now")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(["STOP retro killed (stop now)", self.NO_TICKET_END], out)
        self.assertFalse(self.is_running(sleep_pid), "the grandchild of the retro survived")
        self.assertFalse(self.is_running(stub_pid), "the retro survived")

    def test_retro_that_exceeds_max_iteration_duration_is_killed_after_the_grace_period(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, stub_pid, sleep_pid = self.start_sleeping_retro(5, "--max-iteration-duration", "1")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(["STOP retro killed (max_iteration_duration 1 reached and grace period over)",
                          self.NO_TICKET_END], out)
        self.assertFalse(self.is_running(sleep_pid), "the grandchild of the retro survived")
        self.assertFalse(self.is_running(stub_pid), "the retro survived")

    def test_retro_within_max_iteration_duration_is_left_alone(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _, _ = self.start_sleeping_retro(1, "--max-iteration-duration", "5", RALPH_GRACE_SECONDS="5")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual([self.RETRO_OK, self.NO_TICKET_END], out)

    def test_usage_limit_during_the_retro_is_not_waited_out_there_is_no_retro_and_the_run_ends(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        Path(f"{self.stream}.retro").write_text(USAGE_LIMIT + "\n", encoding="utf-8")
        Path(f"{self.stream}.retro.exit").write_text("1", encoding="utf-8")

        code, lines, err = self.run_loop(STUB_GH_EMPTY="1", RALPH_LIMIT_RETRY_SECONDS="1", seconds=15)

        self.assertEqual(0, code, err)
        self.assertEqual(["ERROR retro failed: usage limit hit, no retro", self.NO_TICKET_END], lines)
        self.assertEqual(1, len(self.retro_calls()), "the retro was started again")

    def test_max_duration_does_not_bound_the_retro(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        process, _, _ = self.start_sleeping_retro(3, "--max-duration", "1", RALPH_GRACE_SECONDS="1")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual(2, len(out), out)  # the limit may be reached before the empty frontier is seen: either END
        self.assertEqual(self.RETRO_OK, out[0], out)
        self.assertIn(out[1], (self.NO_TICKET_END, "END after 0 iterations: max_duration 1 reached (0 ok, 0 failed)"))

    def test_until_does_not_bound_the_retro(self):
        self.stream.write_text(SUCCESS + "\n", encoding="utf-8")
        until = f"{self.berlin_wall_clock(0)} Europe/Berlin"
        process, _, _ = self.start_sleeping_retro(3, "--until", until, RALPH_GRACE_SECONDS="1")

        out = process.communicate(timeout=30)[0].splitlines()

        self.assertEqual([self.RETRO_OK, f"END after 0 iterations: until {until} reached (0 ok, 0 failed)"], out)


LEARNINGS = Path("docs") / "agents" / "goRalph-learnings.md"
LEARNINGS_LINE_LIMIT = 40


class LearningsFileTest(unittest.TestCase):
    """The loop's learnings file: every iteration reads it, so it stays short (a retro condenses it; raising the
    limit is the owner's decision through a retro ticket, and then this constant)."""

    def test_the_file_exists_and_holds_lessons(self):
        text = (REPO / LEARNINGS).read_text(encoding="utf-8")
        self.assertGreaterEqual(sum(1 for line in text.splitlines() if line.startswith("- ")), 3,
                                f"{LEARNINGS} holds fewer than three lessons (lines starting with '- ')")

    def test_the_file_stays_at_or_under_its_line_limit(self):
        lines = (REPO / LEARNINGS).read_text(encoding="utf-8").splitlines()
        self.assertLessEqual(len(lines), LEARNINGS_LINE_LIMIT,
                             f"{LEARNINGS} has {len(lines)} lines, limit {LEARNINGS_LINE_LIMIT}: condense it (general "
                             f"lessons to docs/agents/implementer-brief.md, obsolete ones deleted); if it still does "
                             f"not fit, file a retro ticket that proposes raising the limit")

    def test_every_iteration_is_pointed_to_the_file_by_the_loop_rules(self):
        rules = (REPO / SKILL_SCRIPTS / "iteration-rules.md").read_text(encoding="utf-8")
        self.assertIn("docs/agents/goRalph-learnings.md", rules, "the loop rules do not point to the learnings file")

    def test_the_retro_rules_name_the_non_interactive_mode_its_allowed_changes_and_the_ticket_labels(self):
        skill = (REPO / SKILL_SCRIPTS / "retro-rules.md").read_text(encoding="utf-8")
        for expected in ("non-interactive", "docs/agents/implementer-brief.md", "docs/agents/goRalph-learnings.md",
                         "`retro`", "`ready-for-human`", "options", "recommendation"):
            self.assertIn(expected, skill, f"the retro rules do not mention {expected}")

    def test_the_retro_rules_make_every_bash_call_one_command_and_use_the_literal_run_directory(self):
        skill = (REPO / SKILL_SCRIPTS / "retro-rules.md").read_text(encoding="utf-8")
        for expected in ("one command", "head", "grep", "This run", "never expand"):
            self.assertIn(expected, skill, f"the retro rules do not mention {expected}")


if __name__ == "__main__":
    unittest.main()
