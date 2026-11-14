# Ralph loop: fresh `claude -p "/implement-spec <spec>"` orchestrators, one per iteration.
# Run it through bash (no executable bit needed):
#   bash loop.sh <spec> [--max-iterations N] [--max-duration MIN] [--max-iteration-duration MIN]
#                       [--until "YYYY-MM-DD HH:MM [Zone/Name]"] [--max-parallel N] [--limit-stop KIND] [--resume-only]
# A start continues the unfinished run of the spec (no `ended` marker in its run directory: iteration number and the
# ok and failed counts are recovered from the result and outcome files) or begins a new run when there is none.
# --resume-only never begins a run: it is for the main session's check-in, which restarts a dead script, and refuses
# with an ERROR line (exit 1) when the spec has no run or only finished ones.
# The options write the control file of the run (control.json); the limits and a stop request in that file are read
# before every iteration, so editing the file changes the running loop. Stop kinds: after-iteration, after-step, now.
# Stdout carries only the monitor lines (iteration end, retro end, ERROR, DENIAL, WAIT for a usage limit, STOP for a
# deliberate kill, END); the rest goes to the run directory.
# The last iteration of a run that ends on a stop after-step or after-iteration, a reached limit acting as one of them
# or an empty frontier is a retro: one `claude -p "/retro non-interactive <run dir>"` call (no retro after a stop now,
# a limit acting as now, a failed check or a usage limit that does not lift). It is no iteration of the run; it has
# the stop now and duration watcher of an iteration, and a usage limit during it means no retro (not waited out).
# Environment: RALPH_HOME (default ~/.goRalph), RALPH_REPO (repository root, default: four levels above this script),
# RALPH_MINUTE_SECONDS, RALPH_POLL_SECONDS, RALPH_GRACE_SECONDS, RALPH_PR_SLEEP_SECONDS, RALPH_GH_TIMEOUT_SECONDS,
# RALPH_LIMIT_RETRY_SECONDS, RALPH_LIMIT_CAP_SECONDS
# (shortened timings for tests, not for the owner; the PR sleep is the pause between two checks while PRs wait for CI,
# default 300, the gh timeout the limit of one `gh api` call of that check, default 60; the limit retry is the pause
# after an iteration that ended on a usage limit, default 600, the limit cap the most the run waits for such a limit to
# lift, default 5 h 15 min).
# An iteration whose result event is an error that names the usage limit (api_error_status 429, or a text with
# "hit your limit" or "usage limit")
# counts as neither ok nor failed: the script waits without a claude process (WAIT lines) and runs it again.
# Before each iteration the script asks `gh api` for work without a model: a free frontier ticket or a PR that needs
# action (red CI, merge conflict, review or review comment after its last commit) starts the iteration, PRs that only
# wait for CI with auto-merge make it sleep and check again, nothing at all ends the run.
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="${RALPH_REPO:-$(cd "$HERE/../../../.." && pwd)}"
HOME_DIR="${RALPH_HOME:-$HOME/.goRalph}"
mkdir -p "$HOME_DIR" && HOME_DIR="$(cd "$HOME_DIR" && pwd)"  # absolute: the script changes directory later

die() { echo "ERROR $1"; exit 2; }

# One control value (key, value): the same rules for the command line and for edits of the control file.
# The wall clock of `until` is read in Europe/Berlin unless a zone name follows it; until_epoch is its instant.
check_value() {
  local key="$1" value="$2"
  case "$key" in
    max_iterations|max_duration|max_iteration_duration)
      [[ "$value" =~ ^[0-9]+$ ]] && [ "$value" -ge 1 ] || die "$key needs a number of at least 1, got '$value'" ;;
    max_parallel)
      [[ "$value" =~ ^[0-9]+$ ]] && [ "$value" -ge 1 ] && [ "$value" -le 3 ] || die "max_parallel needs a number from 1 to 3, got '$value'" ;;
    limit_stop)
      case "$value" in after-iteration|after-step|now) ;; *) die "limit_stop must be after-iteration, after-step or now, got '$value'" ;; esac ;;
    stop)
      case "$value" in true|after-iteration|after-step|now) ;; *) die "stop must be true, after-iteration, after-step or now, got '$value'" ;; esac ;;
    until)
      local re='^([0-9]{4}-[0-9]{2}-[0-9]{2})[ T]([0-9]{2}:[0-9]{2}(:[0-9]{2})?)( +([A-Za-z0-9_+/-]+))?$'
      [[ "$value" =~ $re ]] || die "until must look like 'YYYY-MM-DD HH:MM' or 'YYYY-MM-DD HH:MM Zone/Name', got '$value'"
      local zone="${BASH_REMATCH[5]:-Europe/Berlin}" wall="${BASH_REMATCH[1]} ${BASH_REMATCH[2]}"
      # GNU date reads an unknown zone as UTC without a word, so the zone file has to exist.
      [ -f "${TZDIR:-/usr/share/zoneinfo}/$zone" ] || die "until names a time zone this machine does not know (tzdata installed?): '$zone'"
      until_epoch="$(TZ="$zone" date -d "$wall" +%s 2>/dev/null)" || die "until is not a valid date and time, got '$value'" ;;
  esac
}

# Timings in seconds: the length of a minute, the watcher's poll interval and the grace period before a kill.
minute_seconds="${RALPH_MINUTE_SECONDS:-60}"
poll_seconds="${RALPH_POLL_SECONDS:-5}"
grace_seconds="${RALPH_GRACE_SECONDS:-300}"
pr_sleep_seconds="${RALPH_PR_SLEEP_SECONDS:-300}"
gh_timeout_seconds="${RALPH_GH_TIMEOUT_SECONDS:-60}"
limit_retry_seconds="${RALPH_LIMIT_RETRY_SECONDS:-600}"
limit_cap_seconds="${RALPH_LIMIT_CAP_SECONDS:-$((315 * minute_seconds))}"  # 5 h 15 min
[[ "$minute_seconds" =~ ^[1-9][0-9]*$ ]] || die "RALPH_MINUTE_SECONDS needs a positive integer, got '$minute_seconds'"
[[ "$poll_seconds" =~ ^[0-9]+(\.[0-9]+)?$ ]] || die "RALPH_POLL_SECONDS needs a number, got '$poll_seconds'"
[[ "$grace_seconds" =~ ^[0-9]+$ ]] || die "RALPH_GRACE_SECONDS needs an integer, got '$grace_seconds'"
[[ "$pr_sleep_seconds" =~ ^[1-9][0-9]*$ ]] || die "RALPH_PR_SLEEP_SECONDS needs a positive integer, got '$pr_sleep_seconds'"
[[ "$gh_timeout_seconds" =~ ^[1-9][0-9]*$ ]] || die "RALPH_GH_TIMEOUT_SECONDS needs a positive integer, got '$gh_timeout_seconds'"
[[ "$limit_retry_seconds" =~ ^[1-9][0-9]*$ ]] || die "RALPH_LIMIT_RETRY_SECONDS needs a positive integer, got '$limit_retry_seconds'"
[[ "$limit_cap_seconds" =~ ^[1-9][0-9]*$ ]] || die "RALPH_LIMIT_CAP_SECONDS needs a positive integer, got '$limit_cap_seconds'"

spec="${1:-}"
[ -n "$spec" ] || die "usage: loop.sh <spec> [--max-iterations N] [--max-duration MIN] [--max-iteration-duration MIN] [--until \"YYYY-MM-DD HH:MM [Zone]\"] [--max-parallel N] [--limit-stop KIND] [--resume-only]"
shift
if ! [[ "$spec" =~ ^[0-9]+$ ]]; then die "spec must be an issue number, got '$spec'"; fi
declare -A cli=()
resume_only=""  # --resume-only: never begins a run, so it also refuses a finished one (the check-in must not restart those)
while [ $# -gt 0 ]; do
  case "$1" in
    --max-iterations|--max-duration|--max-iteration-duration|--max-parallel|--until|--limit-stop)
      [ $# -ge 2 ] || die "$1 needs a value"
      key="${1#--}"
      key="${key//-/_}"
      check_value "$key" "$2"
      cli[$key]="$2"
      shift 2 ;;
    --resume-only) resume_only=1; shift ;;
    *) die "unknown argument $1" ;;
  esac
done
command -v jq >/dev/null || die "jq is required"
command -v flock >/dev/null || die "flock not found"
command -v mktemp >/dev/null || die "mktemp is required"
command -v timeout >/dev/null || die "timeout is required"

# Lock: one active run per spec, held through flock on a file descriptor, so the kernel releases it when the
# process dies (a dead holder never blocks) and two starters cannot both get it.
lock="$HOME_DIR/locks/$spec.lock"
mkdir -p "$HOME_DIR/locks" "$HOME_DIR/runs"
exec 9>>"$lock"
if ! flock -n 9; then
  echo "ERROR spec #$spec already has a running loop (pid $(cat "$lock" 2>/dev/null)), lock $lock"
  exit 1
fi
echo $$ > "$lock"

# The child runs in its own process group (job control), so a stop `now` can kill it with everything it started.
child=""
watcher=""
sleeper=""
trap '[ -n "$child" ] && kill -TERM -- "-$child" 2>/dev/null; [ -n "$sleeper" ] && kill "$sleeper" 2>/dev/null; [ -n "$watcher" ] && kill "$watcher" 2>/dev/null; exit 143' TERM INT

# Run directory: continue an unfinished run of this spec, else start a new one.
run=""
for candidate in "$HOME_DIR"/runs/"$spec"-*; do
  if [ -d "$candidate" ] && [ ! -e "$candidate/ended" ]; then run="$candidate"; fi
done
if [ -z "$run" ] && [ -n "$resume_only" ]; then
  if compgen -G "$HOME_DIR/runs/$spec-*" >/dev/null; then
    echo "ERROR run of #$spec already ended, nothing to resume"
  else
    echo "ERROR no run of #$spec to resume"
  fi
  exit 1
fi
if [ -z "$run" ]; then
  run="$HOME_DIR/runs/$spec-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$run"
fi
export RALPH_RUN_DIR="$run"
control="$run/control.json"
if [ ! -e "$control" ]; then
  jq -n '{until: null, max_duration: null, max_iterations: null, max_iteration_duration: null, max_parallel: 3,
          stop: null, limit_stop: "after-step"}' > "$control"
fi
# Every write of the control file by this script: one writer at a time (flock on a lock file next to it), a
# temporary file of its own, then an atomic rename, so concurrent writes of the script are not lost.
update_control() { # jq arguments and program
  (
    flock 8
    tmp="$(mktemp "$control.XXXXXX")"
    if jq "$@" "$control" > "$tmp"; then mv "$tmp" "$control"; else rm -f "$tmp"; fi
  ) 8>>"$control.lock"
}
for key in "${!cli[@]}"; do
  update_control --arg k "$key" --arg v "${cli[$key]}" \
    'if ($k == "until" or $k == "limit_stop") then .[$k] = $v else .[$k] = ($v | tonumber) end'
done
[ -e "$run/started" ] || date +%s > "$run/started"  # max_duration counts from here, also after a restart
run_started="$(cat "$run/started")"

ctl() { jq -r --arg k "$1" '.[$k] // empty | tostring' "$control"; }  # a control value, empty when absent or null
effective_stop() { local s; s="$(ctl stop)"; if [ "$s" = true ]; then echo after-step; else echo "$s"; fi; }
limit_kind() { local k; k="$(ctl limit_stop)"; echo "${k:-after-step}"; }

# A hand edit with a wrong value ends the script with an ERROR line and exit 2; the run stays unfinished, so a
# start after the fix continues it.
validate_control() {
  jq -e 'type == "object"' "$control" >/dev/null 2>&1 || die "control file $control is not a JSON object"
  local key value
  for key in until max_duration max_iterations max_iteration_duration max_parallel limit_stop stop; do
    value="$(ctl "$key")"
    if [ -n "$value" ]; then check_value "$key" "$value"; fi
  done
}

request_stop() { # kind: written only when no stop is requested yet, so an owner's stop kind stays
  update_control --arg k "$1" 'if (.stop == null or .stop == "") then .stop = $k else . end'
}

# Watches one running iteration (child pid, marker file for the reason of a kill). A stop `now` kills the child's
# process group; max_iteration_duration writes the limit's stop kind into the control file and kills the group once
# the grace period is over.
watch_iteration() {
  local pid="$1" marker="$2" begin requested="" now max_iteration sleeper=""
  begin="$(date +%s)"
  trap '[ -n "$sleeper" ] && kill "$sleeper" 2>/dev/null; exit 0' TERM
  while true; do
    sleep "$poll_seconds" & sleeper=$!
    wait "$sleeper"
    kill -0 "$pid" 2>/dev/null || exit 0  # the iteration ended by itself: nothing to write, nothing to kill
    now="$(date +%s)"
    max_iteration="$(ctl max_iteration_duration)"
    if [ -z "$requested" ] && [ -n "$max_iteration" ] && [ $((now - begin)) -ge $((max_iteration * minute_seconds)) ]; then
      request_stop "$(limit_kind)"
      requested="$now"
    fi
    if [ "$(effective_stop)" = now ]; then
      echo "stop now" > "$marker"
      kill -KILL -- "-$pid" 2>/dev/null
      exit 0
    fi
    if [ -n "$requested" ] && [ $((now - requested)) -ge "$grace_seconds" ]; then
      echo "max_iteration_duration $max_iteration reached and grace period over" > "$marker"
      kill -KILL -- "-$pid" 2>/dev/null
      exit 0
    fi
  done
}

ok_count=0
failed_count=0
limit_waited=0  # seconds waited for the usage limit since the last iteration that was not cut off by it

REPO_API="repos/marmer/hamcrest-matcher-generator"

# One `gh api` call (arguments as for gh) with the timeout, so a hung gh cannot block the TERM trap for long; the
# lists are read with --paginate --slurp, i.e. as a JSON array holding one element per page.
gh_call() { timeout "$gh_timeout_seconds" gh api "$@" 2>>"$run/pr-err.txt"; }
gh_pages() { gh_call --paginate --slurp "$1"; }

# jq -e with its three outcomes kept apart: sets test_result to true or false (exit 0 or 1 of jq: the answer), and
# returns 1 when jq itself failed (exit 2 or more: invalid input, a missing field), so garbage never passes as green.
json_test() { # input, jq arguments
  local input="$1" status
  shift
  jq -e "$@" <<< "$input" > /dev/null 2>>"$run/pr-err.txt"
  status=$?
  [ "$status" -le 1 ] || return 1
  if [ "$status" -eq 0 ]; then test_result=true; else test_result=false; fi
}

# Looks at the open PRs of the spec's tickets (head branch ticket-<n>, n a sub-issue of the spec). Sets pr_action to
# the first PR needing action (red CI, merge conflict, review or review comment after the head commit; replies in a
# comment thread do not count) and pr_waiting to the number of PRs that need nothing but wait for CI with auto-merge
# (a check run not completed, or none yet); returns 1 when `gh` or its output fails.
check_prs() {
  pr_action=""
  pr_waiting=0
  local out children rows number sha detail checks reason head_date
  : > "$run/pr-err.txt"
  out="$(gh_pages "$REPO_API/issues/$spec/sub_issues?per_page=100")" || return 1
  children="$(jq -c '[.[][].number]' <<< "$out" 2>>"$run/pr-err.txt")" || return 1
  out="$(gh_pages "$REPO_API/pulls?state=open&per_page=100")" || return 1
  rows="$(jq -r --argjson children "$children" '.[][]
      | select(.head.ref | test("^ticket-[0-9]+$"))
      | select((.head.ref | ltrimstr("ticket-") | tonumber) as $n | $children | index($n))
      | [.number, .head.sha] | @tsv' <<< "$out" 2>>"$run/pr-err.txt")" || return 1
  while IFS=$'\t' read -r number sha; do
    [ -n "$number" ] || continue
    detail="$(gh_call "$REPO_API/pulls/$number")" || return 1
    out="$(gh_pages "$REPO_API/commits/$sha/check-runs?per_page=100")" || return 1
    checks="$(jq -c '[.[].check_runs[]]' <<< "$out" 2>>"$run/pr-err.txt")" || return 1
    reason=""
    json_test "$detail" '.mergeable_state == "dirty"' || return 1
    if [ "$test_result" = true ]; then
      reason="merge conflict"
    else
      json_test "$checks" 'any(.[]; .conclusion | IN("failure", "timed_out", "cancelled", "action_required", "startup_failure"))' || return 1
      if [ "$test_result" = true ]; then reason="red CI"; fi
    fi
    if [ -z "$reason" ]; then
      out="$(gh_call "$REPO_API/commits/$sha")" || return 1
      head_date="$(jq -r '.commit.committer.date' <<< "$out" 2>>"$run/pr-err.txt")" || return 1
      out="$(gh_pages "$REPO_API/pulls/$number/comments?per_page=100")" || return 1
      json_test "$out" --arg d "$head_date" 'any(.[][]; .created_at > $d and .in_reply_to_id == null)' || return 1
      if [ "$test_result" = true ]; then reason="new review comment"; fi
    fi
    if [ -z "$reason" ]; then
      out="$(gh_pages "$REPO_API/pulls/$number/reviews?per_page=100")" || return 1
      json_test "$out" --arg d "$head_date" \
        'any(.[][]; .submitted_at > $d and (.state == "CHANGES_REQUESTED" or (.body // "") != ""))' || return 1
      if [ "$test_result" = true ]; then reason="new review"; fi
    fi
    if [ -n "$reason" ]; then pr_action="#$number $reason"; return 0; fi
    json_test "$detail" '.auto_merge != null' || return 1
    if [ "$test_result" = true ]; then
      json_test "$checks" 'length == 0 or any(.[]; .status != "completed")' || return 1
      if [ "$test_result" = true ]; then pr_waiting=$((pr_waiting + 1)); fi
    fi
  done <<< "$rows"
  return 0
}

# The reason of the first reached limit (until, max_duration, max_iterations), empty when none is reached.
limit_reason() {
  local now max max_duration until_value
  now="$(date +%s)"
  max="$(ctl max_iterations)"
  max_duration="$(ctl max_duration)"
  until_value="$(ctl until)"
  if [ -n "$until_value" ] && [ "$now" -ge "$until_epoch" ]; then echo "until $until_value reached"
  elif [ -n "$max_duration" ] && [ $((now - run_started)) -ge $((max_duration * minute_seconds)) ]; then echo "max_duration $max_duration reached"
  elif [ -n "$max" ] && [ "$iteration" -ge "$max" ]; then echo "max_iterations $max reached"
  fi
}

# Milliseconds of the monotonic clock (/proc/uptime, 10 ms resolution, sets mono_now): a pause must not be cut short
# or stretched by a step of the wall clock, which `date` follows.
mono_ms() {
  local up
  read -r up _ < /proc/uptime
  mono_now=$((10#${up%.*} * 1000 + 10#${up#*.} * 10))
}

# Sleeps the given seconds in poll-sized slices, so a stop request or a reached limit ends the sleep early (status 1).
# Sets slept_seconds: the given seconds after a full sleep, the whole elapsed seconds after an early end.
sleep_unless_stopped() {
  local begin deadline  # in milliseconds: whole seconds would end the pause up to a second early
  mono_ms
  begin="$mono_now"
  deadline=$((begin + $1 * 1000))
  slept_seconds="$1"
  while mono_ms; [ "$mono_now" -lt "$deadline" ]; do
    if [ -n "$(effective_stop)" ] || [ -n "$(limit_reason)" ]; then slept_seconds=$(((mono_now - begin) / 1000)); return 1; fi
    sleep "$poll_seconds" & sleeper=$!
    wait "$sleeper"
    sleeper=""
  done
}

write_status() { # state, iteration
  jq -n --arg spec "$spec" --arg state "$1" --argjson iteration "$2" --argjson ok "$ok_count" \
    --argjson failed "$failed_count" --arg run "$run" \
    '{spec: $spec, state: $state, iteration: $iteration, ok: $ok, failed: $failed, run_dir: $run}' > "$run/status.json"
}

# Shared by the iterations and the retro: the failure of a finished child (empty when it is fine) from its result file
# and exit code, and the DENIAL line for its permission denials (label: "iteration <i>" or "retro").
result_failure() { # result file, exit code
  local file="$1" rc="$2"
  if [ ! -s "$file" ]; then echo "no result event (exit $rc)"
  elif ! jq -e . "$file" >/dev/null 2>&1; then echo "unreadable result event (exit $rc)"
  elif [ "$rc" -ne 0 ]; then echo "exit $rc, $(jq -r '.subtype // "no subtype"' "$file")"
  elif [ "$(jq -r '.is_error // false' "$file")" = "true" ]; then echo "is_error set, $(jq -r '.subtype // "no subtype"' "$file")"
  elif [ "$(jq 'has("result")' "$file")" != "true" ]; then echo "no result field, $(jq -r '.subtype // "no subtype"' "$file")"
  fi
}
# A usage limit is read from the result event (an error naming it), never from the exit code alone.
is_usage_limit() { # result file
  [ -s "$1" ] && jq -e '.is_error == true
    and (.api_error_status == 429 or ((.result // "") | test("hit your limit|usage limit"; "i")))' "$1" >/dev/null 2>&1
}
# The work data of an iteration: the orchestrator writes iteration-<i>.json into the run directory (the loop rules
# say how). Its suffix for the iteration's ok line is ", tickets #<n> ..., PRs #<n> ..." ("none" for an empty list);
# empty when the file is missing or malformed (both lists must be arrays of numbers from 1 on), so the line is then
# the one without them.
work_suffix() { # iteration
  local file="$run/iteration-$1.json" tickets prs
  [ -s "$file" ] || return 0
  jq -e '[.tickets, .prs] | all(type == "array" and all(.[]; type == "number" and . == floor and . >= 1))' "$file" \
    >/dev/null 2>&1 || return 0
  tickets="$(jq -r '.tickets | if length == 0 then "none" else map("#\(.)") | join(" ") end' "$file")"
  prs="$(jq -r '.prs | if length == 0 then "none" else map("#\(.)") | join(" ") end' "$file")"
  echo ", tickets $tickets, PRs $prs"
}
denial_line() { # label, result file
  if [ -s "$2" ] && jq -e '(.permission_denials // []) | length > 0' "$2" >/dev/null 2>&1; then
    echo "DENIAL $1: $(jq -c '[.permission_denials[].tool_name]' "$2")"
  fi
}

# The one claude call of an iteration and of the retro: starts it in the background (stdin from /dev/null, stdout to the stream file, stderr to
# its own file, the lock descriptor closed) and sets $child. The rules file is the system prompt addition.
start_claude() { # prompt, rules file name in $HERE, stream file, stderr file, extra "This run" line
  claude -p "$1" --model sonnet \
    --settings "$settings" --allowedTools "$allowed" \
    --append-system-prompt "$(cat "$HERE/$2")"$'\n\n'"## This run"$'\n\n'"Run directory: $run"$'\n'"$5" \
    --output-format stream-json --verbose \
    < /dev/null > "$3" 2> "$4" 9>&- &
  child=$!
}

# The retro iteration (#418): one `claude -p "/retro non-interactive <run dir>"` call, the last of a run that ends on
# a stop after-step or after-iteration, a reached limit acting as one of them, or an empty frontier. It is no
# iteration of the run (not counted in the iteration number, ok or failed); its files are retro-stream.jsonl,
# retro-result.json and retro-stderr.txt, and retro-done marks it as attempted, so a restart does not run it twice.
# The child gets the settings and tools of the iterations and retro-rules.md (the non-interactive mode) instead of the
# loop rules. It has the watcher of an iteration (#438): a stop `now` kills its process group, max_iteration_duration
# bounds it with the same grace period, and either gives one STOP retro line; until and max_duration do not bound it.
# A usage limit during the retro is not waited out: no retro, one ERROR line.
run_retro() {
  [ -e "$run/retro-done" ] && return 0
  write_status retro "$iteration"
  local rc=0 failure killed="$run/retro-killed"
  rm -f "$killed"
  set -m  # own process group for the child (pgid = its pid)
  start_claude "/retro non-interactive $run" retro-rules.md "$run/retro-stream.jsonl" "$run/retro-stderr.txt" "Last iteration of the run: $iteration"
  watch_iteration "$child" "$killed" > /dev/null 2>&1 9>&- &
  watcher=$!
  set +m
  wait "$child" || rc=$?
  kill "$watcher" 2>/dev/null
  wait "$watcher" 2>/dev/null
  child=""
  watcher=""
  jq -c 'select(.type == "result")' "$run/retro-stream.jsonl" 2>/dev/null | tail -1 > "$run/retro-result.json"
  if [ -s "$killed" ]; then
    echo "STOP retro killed ($(cat "$killed"))"
  elif is_usage_limit "$run/retro-result.json"; then
    echo "ERROR retro failed: usage limit hit, no retro"
  else
    failure="$(result_failure "$run/retro-result.json" "$rc")"
    if [ -n "$failure" ]; then
      echo "ERROR retro failed: $failure"
    else
      echo "retro iteration ok: $(jq -r '"\(.num_turns // 0) turns, cost $\(.total_cost_usd // 0)"' "$run/retro-result.json")"
    fi
    denial_line retro "$run/retro-result.json"
  fi
  touch "$run/retro-done"
}

# The end of the run; the retro decision is made by the caller's choice of function, never by a default.
end_run() { # iteration, reason
  touch "$run/ended"
  write_status ended "$1"
  echo "END after $1 iterations: $2 ($ok_count ok, $failed_count failed)"
  exit 0
}
finish_with_retro() { run_retro; end_run "$1" "$2"; }  # iteration, reason
finish_without_retro() { end_run "$1" "$2"; }  # iteration, reason
# A stop request or a reached limit acting as the given stop kind: no retro after `now`, one after the other kinds.
finish_for_stop_kind() { # stop kind, iteration, reason
  if [ "$1" = now ]; then finish_without_retro "$2" "$3"; else finish_with_retro "$2" "$3"; fi
}

settings='{"disableAllHooks":true,"permissions":{"deny":["Bash(git push --force*)","Bash(git push -f*)","Bash(git push origin main*)","Bash(git push origin HEAD:main*)","Bash(git reset --hard*)","Bash(git branch -D*)","Bash(gh api*-X PUT*)","Bash(gh api*-XPUT*)","Bash(gh api*--method PUT*)","Bash(gh api*--method=PUT*)","Bash(gh api*-X put*)","Bash(gh api*-Xput*)","Bash(gh api*--method put*)","Bash(gh api*--method=put*)","Bash(gh api*-X Put*)","Bash(gh api*-XPut*)","Bash(gh api*--method Put*)","Bash(gh api*--method=Put*)","Bash(gh api*mergePullRequest*)","Bash(gh pr merge*)","Bash(git push*--force*)","Bash(git push* -f*)","Bash(git push*--de*)","Bash(git push*-d *)","Bash(git push* :**)","Bash(git push* main)","Bash(git push* main *)","Bash(git push*:main)","Bash(git push*:main *)","Bash(git push*/main)","Bash(git push*/main *)","Bash(git push*+main)","Bash(git push*+main *)","Bash(git push*--m*)","Bash(git push*--pru*)","Bash(git push* +*)","Bash(git reset*--hard*)","Bash(git branch*-D*)","Bash(git branch* -f*)","Bash(git branch*--force*)","Bash(git branch*-df*)","Bash(git branch*-fd*)","Bash(git branch*-d*main*)","Bash(git branch*--delete*main*)","Bash(git update-ref*main*)","Bash(gh api*-X DELETE*)","Bash(gh api*-X delete*)","Bash(gh api*-X Delete*)","Bash(gh api*-XDELETE*)","Bash(gh api*-Xdelete*)","Bash(gh api*-XDelete*)","Bash(gh api*--method DELETE*)","Bash(gh api*--method delete*)","Bash(gh api*--method Delete*)","Bash(gh api*--method=DELETE*)","Bash(gh api*--method=delete*)","Bash(gh api*--method=Delete*)","Bash(gh api*git/refs/heads/main*)","Bash(gh api*branches/main*)","Bash(gh api*/merges*)","Bash(gh repo delete*)","Bash(gh repo archive*)","Bash(gh repo edit*)","Bash(gh repo rename*)","Bash(rm *r* /)","Bash(rm *r* ~)","Bash(rm *r* ~/)","Bash(rm *r* $HOME)","Bash(rm *r* $HOME/)","Bash(rm *r* ${HOME})","Bash(rm *r* .)","Bash(rm *r* ..)","Bash(rm *r* *.git)","Bash(rm *r* *.git/)","Bash(rm *r* *.goRalph)","Bash(rm *r* *.goRalph/)","Bash(rm *r* *.goRalph/runs)","Bash(rm *r* *.goRalph/runs/)","Bash(sudo*)","Bash(doas*)","Bash(su)","Bash(su *)","Bash(git config*credential*)","Bash(git config*--global*)","Bash(git config*--system*)","Bash(git credential*)","Bash(curl*|*sh*)","Bash(curl*|*python*)","Bash(wget*|*sh*)","Bash(wget*|*python*)","Bash(sh <(*)","Bash(bash <(*)","Bash(bash -c*)","Bash(sh -c*)","Bash(zsh -c*)","Bash(eval *)"]}}'
allowed='Bash,Read,Edit,Write,Agent,Skill'

cd "$REPO" || exit 2
iteration=0
# After a restart the iteration number and the counts come from the files of the finished iterations; an iteration
# whose outcome file is missing (the script died between its result and its outcome) counts as failed.
for existing in "$run"/result-*.json; do
  [ -e "$existing" ] || continue
  n="${existing##*/result-}"
  n="${n%.json}"
  if [ "$n" -gt "$iteration" ]; then iteration="$n"; fi
  if [ "$(cat "$run/outcome-$n" 2>/dev/null)" = ok ]; then ok_count=$((ok_count + 1)); else failed_count=$((failed_count + 1)); fi
done

while true; do
  validate_control
  stop="$(effective_stop)"
  if [ -n "$stop" ]; then
    finish_for_stop_kind "$stop" "$iteration" "stop requested ($stop)"
  fi

  # A reached limit acts as a stop request of the configured kind.
  reason="$(limit_reason)"
  if [ -n "$reason" ]; then
    request_stop "$(limit_kind)"
    finish_for_stop_kind "$(effective_stop)" "$iteration" "$reason"
  fi

  if ! frontier="$(cd "$REPO" && python3 scripts/agents/issue-status.py "$spec" --frontier 2>"$run/frontier-err.txt")"; then
    echo "ERROR frontier check for #$spec failed: $(head -c 200 "$run/frontier-err.txt" | tr '\n' ' ')"
    finish_without_retro "$iteration" "frontier check failed"
  fi
  if ! printf '%s\n' "$frontier" | grep -qv -e '^$' -e '^waiting for the owner:'; then
    if ! check_prs; then
      echo "ERROR PR check for #$spec failed: $(head -c 200 "$run/pr-err.txt" | tr '\n' ' ')"
      finish_without_retro "$iteration" "PR check failed"
    fi
    if [ -z "$pr_action" ]; then
      if [ "$pr_waiting" -eq 0 ]; then finish_with_retro "$iteration" "no ticket ready for an agent"; fi
      write_status waiting "$iteration"
      sleep_unless_stopped "$pr_sleep_seconds"
      continue
    fi
  fi

  iteration=$((iteration + 1))
  write_status running "$iteration"
  stream="$run/stream-$iteration.jsonl"
  result="$run/result-$iteration.json"
  killed="$run/killed-$iteration"
  rm -f "$killed"
  rm -f "$run/iteration-$iteration.json"  # work data of an earlier attempt, e.g. one cut off by a usage limit
  export RALPH_ITERATION="$iteration"  # kept for the child's tools; the prompt carries the number literally (iteration-rules.md)
  set -m  # own process group for the child (pgid = its pid)
  start_claude "/implement-spec $spec" iteration-rules.md "$stream" "$run/stderr-$iteration.txt" "Iteration: $iteration"
  watch_iteration "$child" "$killed" > /dev/null 2>&1 9>&- &
  watcher=$!
  set +m
  wait "$child"
  rc=$?
  kill "$watcher" 2>/dev/null
  wait "$watcher" 2>/dev/null
  child=""
  watcher=""

  jq -c 'select(.type == "result")' "$stream" 2>/dev/null | tail -1 > "$result"
  failure=""
  if [ ! -s "$killed" ] && is_usage_limit "$result"; then
    mv "$result" "$run/limit-result-$iteration-$(date +%s).json"
    iteration=$((iteration - 1))  # the cut-off iteration does not count
    if [ "$limit_waited" -ge "$limit_cap_seconds" ]; then
      finish_without_retro "$iteration" "usage limit not lifted within $limit_cap_seconds s"
    fi
    pause=$((limit_cap_seconds - limit_waited))
    if [ "$pause" -gt "$limit_retry_seconds" ]; then pause="$limit_retry_seconds"; fi
    echo "WAIT usage limit hit in iteration $((iteration + 1)): no claude process until the retry in $pause s (waited $limit_waited of $limit_cap_seconds s)"
    write_status waiting "$iteration"
    if sleep_unless_stopped "$pause"; then over="retrying"; else over="stop requested or limit reached"; fi
    limit_waited=$((limit_waited + slept_seconds))
    echo "WAIT over, $over (waited $limit_waited of $limit_cap_seconds s)"
    continue
  fi
  limit_waited=0
  if [ -s "$killed" ]; then
    : # a deliberate stop, reported as STOP below, not as a failure line
  else
    failure="$(result_failure "$result" "$rc")"
  fi

  if [ -s "$killed" ] || [ -n "$failure" ]; then outcome=failed; else outcome=ok; fi
  echo "$outcome" > "$run/outcome-$iteration"
  if [ "$outcome" = ok ]; then ok_count=$((ok_count + 1)); else failed_count=$((failed_count + 1)); fi
  if [ -s "$killed" ]; then
    echo "STOP iteration $iteration killed ($(cat "$killed"))"
  elif [ -n "$failure" ]; then
    echo "ERROR iteration $iteration failed: $failure"
  else
    echo "iteration $iteration ok: $(jq -r '"\(.num_turns // 0) turns, cost $\(.total_cost_usd // 0)"' "$result")$(work_suffix "$iteration")"
  fi
  denial_line "iteration $iteration" "$result"
  write_status between "$iteration"
done
