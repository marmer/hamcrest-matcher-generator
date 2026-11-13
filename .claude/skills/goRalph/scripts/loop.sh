# Ralph loop: fresh `claude -p "/implement-spec <spec>"` orchestrators, one per iteration.
# Run it through bash (no executable bit needed): bash loop.sh <spec> [--max-iterations N]
# Stdout carries only the monitor lines (iteration end, error, denial, END); everything else goes to the run directory.
# Environment: RALPH_HOME (default ~/.goRalph), RALPH_REPO (repository root, default: four levels above this script).
set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="${RALPH_REPO:-$(cd "$HERE/../../../.." && pwd)}"
HOME_DIR="${RALPH_HOME:-$HOME/.goRalph}"
mkdir -p "$HOME_DIR" && HOME_DIR="$(cd "$HOME_DIR" && pwd)"  # absolute: the script changes directory later

spec="${1:?usage: loop.sh <spec> [--max-iterations N]}"
shift
if ! [[ "$spec" =~ ^[0-9]+$ ]]; then echo "ERROR spec must be an issue number, got '$spec'"; exit 2; fi
max_iterations=""
while [ $# -gt 0 ]; do
  case "$1" in
    --max-iterations)
      max_iterations="${2:-}"
      if ! [[ "$max_iterations" =~ ^[0-9]+$ ]]; then echo "ERROR --max-iterations needs a number, got '$max_iterations'"; exit 2; fi
      shift 2 ;;
    *) echo "ERROR unknown argument $1"; exit 2 ;;
  esac
done
command -v jq >/dev/null || { echo "ERROR jq is required"; exit 2; }
command -v flock >/dev/null || { echo "ERROR flock not found"; exit 2; }

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

child=""
trap '[ -n "$child" ] && kill "$child" 2>/dev/null; exit 143' TERM INT

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
  jq -n --arg n "$max_iterations" \
    '{max_iterations: (if $n == "" then null else ($n | tonumber) end), max_parallel: 3, stop: null}' > "$control"
elif [ -n "$max_iterations" ]; then
  jq --argjson n "$max_iterations" '.max_iterations = $n' "$control" > "$control.tmp" && mv "$control.tmp" "$control"
fi

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
  max="$(jq -r '.max_iterations // empty' "$control")"
  stop="$(jq -r '.stop // empty' "$control")"
  if [ -n "$stop" ]; then finish "$iteration" "stop requested ($stop)"; fi
  if [ -n "$max" ] && [ "$iteration" -ge "$max" ]; then finish "$iteration" "max_iterations $max reached"; fi

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
  claude -p "/implement-spec $spec" --model sonnet \
    --settings "$settings" --allowedTools "$allowed" \
    --append-system-prompt "$(cat "$HERE/iteration-rules.md")" \
    --output-format stream-json --verbose \
    < /dev/null > "$stream" 2> "$run/stderr-$iteration.txt" 9>&- &
  child=$!
  wait "$child"
  rc=$?
  child=""

  jq -c 'select(.type == "result")' "$stream" 2>/dev/null | tail -1 > "$result"
  failure=""
  if [ ! -s "$result" ]; then
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
