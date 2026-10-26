#!/usr/bin/env bash
# Tests for changelog-anchor.sh. Run: scripts/release/test-changelog-anchor.sh
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

cat > "$tmp/CHANGELOG.md" <<'MD'
# Changelog

## [6.0.0](https://example.org/compare/5.1.0...6.0.0) (2026-11-01)

### Features

* something

## [5.1.0](https://example.org/compare/e1514ce...5.1.0) (2023-08-28)

* old
MD

fail=0
expect() {
  local actual
  actual=$("$here/changelog-anchor.sh" "$1" "$tmp/CHANGELOG.md")
  if [ "$actual" != "$2" ]; then
    echo "FAIL $1: expected '$2', got '$actual'"
    fail=1
  else
    echo "ok   $1 -> $actual"
  fi
}
expect 6.0.0 600-2026-11-01
expect 5.1.0 510-2023-08-28

if "$here/changelog-anchor.sh" 9.9.9 "$tmp/CHANGELOG.md" 2>/dev/null; then
  echo "FAIL 9.9.9 should fail"
  fail=1
else
  echo "ok   9.9.9 fails"
fi
exit $fail
