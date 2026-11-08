#!/usr/bin/env bash
# Fast-forwards a clean local `main` to origin/main at session start, when that is safe. Called by the SessionStart hook
# (`.claude/hooks/session-start.sh`) in local and cloud sessions; see `docs/agents/build-pitfalls.md`.
#
# Usage: scripts/agents/fast-forward-main.sh     (run inside the checkout, in the current directory)
#
# Fetches origin (20 seconds at most, no credential prompt), then runs `git merge --ff-only origin/main` only when the
# current branch is `main`, `git status --porcelain` prints nothing and `main` is behind origin/main without commits of
# its own. Every other case changes nothing. It never merges, rebases, resets, stashes or touches another branch.
# Always exits 0 and prints exactly one status line on stdout; git's own output is kept out of the session.
set -uo pipefail

say() { echo "session-start: $*"; }

commits() { # <count>
  if [ "$1" -eq 1 ]; then echo "1 commit"; else echo "$1 commits"; fi
}

fetch_origin() {
  if command -v timeout >/dev/null 2>&1; then
    GIT_TERMINAL_PROMPT=0 timeout 20 git fetch --quiet origin
  else
    GIT_TERMINAL_PROMPT=0 git fetch --quiet origin
  fi
}

fast_forward_main() {
  if ! git rev-parse --git-dir >/dev/null 2>&1; then
    say "not a git checkout; nothing to fast-forward"
    return 0
  fi

  if ! fetch_origin >/dev/null 2>&1; then
    say "could not fetch origin (no network or no remote?); main not fast-forwarded"
    return 0
  fi

  if ! git rev-parse --verify --quiet origin/main >/dev/null; then
    say "origin has no main branch; main left untouched"
    return 0
  fi

  local branch
  branch=$(git symbolic-ref --quiet --short HEAD 2>/dev/null || true)
  if [ -z "$branch" ]; then
    say "HEAD is detached; main left untouched"
    return 0
  fi
  if [ "$branch" != "main" ]; then
    say "current branch is $branch, not main; left untouched"
    return 0
  fi

  local counts ahead behind
  if ! counts=$(git rev-list --left-right --count HEAD...origin/main 2>/dev/null); then
    say "could not compare main with origin/main; main left untouched"
    return 0
  fi
  read -r ahead behind <<<"$counts"
  if [ "$behind" -eq 0 ]; then
    if [ "$ahead" -eq 0 ]; then
      say "main is up to date with origin/main"
    else
      say "main is $(commits "$ahead") ahead of origin/main; nothing to fast-forward"
    fi
    return 0
  fi
  if [ "$ahead" -gt 0 ]; then
    say "main is $(commits "$ahead") ahead and $(commits "$behind") behind origin/main (diverged); not fast-forwarded"
    return 0
  fi

  # An unreadable status counts as dirty: a clean tree is the only condition that allows the merge.
  local dirty
  dirty=$(git status --porcelain 2>/dev/null) || dirty="unknown"
  if [ -n "$dirty" ]; then
    say "main is $(commits "$behind") behind origin/main; not fast-forwarded: working tree dirty"
    return 0
  fi

  if git merge --ff-only --quiet origin/main >/dev/null 2>&1; then
    say "main fast-forwarded to origin/main ($(commits "$behind"))"
  else
    say "main is $(commits "$behind") behind origin/main; not fast-forwarded: git merge --ff-only failed"
  fi
  return 0
}

fast_forward_main
exit 0
