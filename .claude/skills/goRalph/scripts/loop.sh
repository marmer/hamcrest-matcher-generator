# Ralph loop: fresh `claude -p "/implement-spec <spec>"` orchestrators, one per iteration.
# Run it through bash (no executable bit needed):
#   bash loop.sh <spec> [--max-iterations N] [--max-duration MIN] [--max-iteration-duration MIN]
#                       [--until "YYYY-MM-DD HH:MM [Zone/Name]"] [--max-parallel N] [--limit-stop KIND]
# The options write the control file of the run (control.json); the limits and a stop request in that file are read
# before every iteration, so editing the file changes the running loop. Stop kinds: after-iteration, after-step, now.
# Stdout carries only the monitor lines (iteration end, error, denial, END); everything else goes to the run directory.
# Environment: RALPH_HOME (default ~/.goRalph), RALPH_REPO (repository root, default: four levels above this script),
# RALPH_MINUTE_SECONDS, RALPH_POLL_SECONDS, RALPH_GRACE_SECONDS (shortened timings for tests, not for the owner).
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
    max_iterations) [[ "$value" =~ ^[0-9]+$ ]] || die "max_iterations needs a number, got '$value'" ;;
    max_duration|max_iteration_duration)
      [[ "$value" =~ ^[0-9]+$ ]] && [ "$value" -ge 1 ] || die "$key needs a number of minutes, at least 1, got '$value'" ;;
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
      if [ -d /usr/share/zoneinfo ] && [ ! -f "/usr/share/zoneinfo/$zone" ]; then die "until names an unknown time zone '$zone'"; fi
      until_epoch="$(TZ="$zone" date -d "$wall" +%s 2>/dev/null)" || die "until is not a valid date and time, got '$value'" ;;
  esac
}

# Timings in seconds: the length of a minute, the watcher's poll interval and the grace period before a kill.
minute_seconds="${RALPH_MINUTE_SECONDS:-60}"
poll_seconds="${RALPH_POLL_SECONDS:-5}"
grace_seconds="${RALPH_GRACE_SECONDS:-300}"
[[ "$minute_seconds" =~ ^[1-9][0-9]*$ ]] || die "RALPH_MINUTE_SECONDS needs a positive integer, got '$minute_seconds'"
[[ "$poll_seconds" =~ ^[0-9]+(\.[0-9]+)?$ ]] || die "RALPH_POLL_SECONDS needs a number, got '$poll_seconds'"
[[ "$grace_seconds" =~ ^[0-9]+$ ]] || die "RALPH_GRACE_SECONDS needs an integer, got '$grace_seconds'"

spec="${1:?usage: loop.sh <spec> [--max-iterations N] [--max-duration MIN] [--max-iteration-duration MIN] [--until \"YYYY-MM-DD HH:MM [Zone]\"] [--max-parallel N] [--limit-stop KIND]}"
shift
if ! [[ "$spec" =~ ^[0-9]+$ ]]; then die "spec must be an issue number, got '$spec'"; fi
declare -A cli=()
while [ $# -gt 0 ]; do
  case "$1" in
    --max-iterations|--max-duration|--max-iteration-duration|--max-parallel|--until|--limit-stop)
      [ $# -ge 2 ] || die "$1 needs a value"
      key="${1#--}"
      key="${key//-/_}"
      check_value "$key" "$2"
      cli[$key]="$2"
      shift 2 ;;
    *) die "unknown argument $1" ;;
  esac
done
command -v jq >/dev/null || die "jq is required"
command -v flock >/dev/null || die "flock not found"

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
trap '[ -n "$child" ] && kill -TERM -- "-$child" 2>/dev/null; [ -n "$watcher" ] && kill "$watcher" 2>/dev/null; exit 143' TERM INT

# Run directory: continue an unfinished run of this spec, else start a new one.
run=""
for candidate in "$HOME_DIR"/runs/"$spec"-*; do
  if [ -d "$candidate" ] && [ ! -e "$candidate/ended" ]; then run="$candidate"; fi
done
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
for key in "${!cli[@]}"; do
  jq --arg k "$key" --arg v "${cli[$key]}" \
    'if ($k == "until" or $k == "limit_stop") then .[$k] = $v else .[$k] = ($v | tonumber) end' \
    "$control" > "$control.tmp" && mv "$control.tmp" "$control"
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
  if [ -z "$(ctl stop)" ]; then
    jq --arg k "$1" '.stop = $k' "$control" > "$control.tmp" && mv "$control.tmp" "$control"
  fi
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

write_status() { # state, iteration
  jq -n --arg spec "$spec" --arg state "$1" --argjson iteration "$2" --argjson ok "$ok_count" \
    --argjson failed "$failed_count" --arg run "$run" \
    '{spec: $spec, state: $state, iteration: $iteration, ok: $ok, failed: $failed, run_dir: $run}' > "$run/status.json"
}

finish() { # iteration, reason
  touch "$run/ended"
  write_status ended "$1"
  echo "END after $1 iterations: $2 ($ok_count ok, $failed_count failed)"
  exit 0
}

settings='{"disableAllHooks":true,"permissions":{"deny":["Bash(git push --force*)","Bash(git push -f*)","Bash(git push origin main*)","Bash(git push origin HEAD:main*)","Bash(git reset --hard*)","Bash(git branch -D*)"]}}'
allowed='Bash(git:*),Bash(./mvnw:*),Bash(gh api:*),Bash(python3 scripts/:*),Read,Edit,Write,Agent,Skill'

cd "$REPO" || exit 2
iteration=0
for existing in "$run"/result-*.json; do
  [ -e "$existing" ] || continue
  n="${existing##*/result-}"
  n="${n%.json}"
  if [ "$n" -gt "$iteration" ]; then iteration="$n"; fi
done

while true; do
  validate_control
  stop="$(effective_stop)"
  if [ -n "$stop" ]; then finish "$iteration" "stop requested ($stop)"; fi

  # A reached limit acts as a stop request of the configured kind.
  now="$(date +%s)"
  max="$(ctl max_iterations)"
  max_duration="$(ctl max_duration)"
  until_value="$(ctl until)"
  reason=""
  if [ -n "$until_value" ] && [ "$now" -ge "$until_epoch" ]; then reason="until $until_value reached"
  elif [ -n "$max_duration" ] && [ $((now - run_started)) -ge $((max_duration * minute_seconds)) ]; then reason="max_duration $max_duration reached"
  elif [ -n "$max" ] && [ "$iteration" -ge "$max" ]; then reason="max_iterations $max reached"
  fi
  if [ -n "$reason" ]; then
    request_stop "$(limit_kind)"
    finish "$iteration" "$reason"
  fi

  if ! frontier="$(cd "$REPO" && python3 scripts/agents/issue-status.py "$spec" --frontier 2>"$run/frontier-err.txt")"; then
    echo "ERROR frontier check for #$spec failed: $(head -c 200 "$run/frontier-err.txt" | tr '\n' ' ')"
    finish "$iteration" "frontier check failed"
  fi
  if ! printf '%s\n' "$frontier" | grep -qv -e '^$' -e '^waiting for the owner:'; then
    finish "$iteration" "no ticket ready for an agent"
  fi

  iteration=$((iteration + 1))
  write_status running "$iteration"
  stream="$run/stream-$iteration.jsonl"
  result="$run/result-$iteration.json"
  killed="$run/killed-$iteration"
  rm -f "$killed"
  set -m  # own process group for the child (pgid = its pid)
  claude -p "/implement-spec $spec" --model sonnet \
    --settings "$settings" --allowedTools "$allowed" \
    --append-system-prompt "$(cat "$HERE/iteration-rules.md")" \
    --output-format stream-json --verbose \
    < /dev/null > "$stream" 2> "$run/stderr-$iteration.txt" 9>&- &
  child=$!
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
  if [ -s "$killed" ]; then
    failure="killed, $(cat "$killed")"
  elif [ ! -s "$result" ]; then
    failure="no result event (exit $rc)"
  elif ! jq -e . "$result" >/dev/null 2>&1; then
    failure="unreadable result event (exit $rc)"
  elif [ "$rc" -ne 0 ]; then
    failure="exit $rc, $(jq -r '.subtype // "no subtype"' "$result")"
  elif [ "$(jq -r '.is_error // false' "$result")" = "true" ]; then
    failure="is_error set, $(jq -r '.subtype // "no subtype"' "$result")"
  elif [ "$(jq 'has("result")' "$result")" != "true" ]; then
    failure="no result field, $(jq -r '.subtype // "no subtype"' "$result")"
  fi

  if [ -n "$failure" ]; then
    failed_count=$((failed_count + 1))
    echo "ERROR iteration $iteration failed: $failure"
  else
    ok_count=$((ok_count + 1))
    echo "iteration $iteration ok: $(jq -r '"\(.num_turns // 0) turns, cost $\(.total_cost_usd // 0)"' "$result")"
  fi
  if [ -s "$result" ] && jq -e '(.permission_denials // []) | length > 0' "$result" >/dev/null 2>&1; then
    echo "DENIAL iteration $iteration: $(jq -c '[.permission_denials[].tool_name]' "$result")"
  fi
  write_status between "$iteration"
done
