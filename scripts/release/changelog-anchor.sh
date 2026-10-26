#!/usr/bin/env bash
# Prints the GitHub heading anchor of a release's section in CHANGELOG.md, e.g. `600-2026-11-01` for
# `## [6.0.0](https://...) (2026-11-01)`. Used by the release workflow to link the CHANGELOG section.
#
# Usage: scripts/release/changelog-anchor.sh <version> [changelog-file]
set -euo pipefail

version="${1:?usage: changelog-anchor.sh <version> [changelog-file]}"
file="${2:-CHANGELOG.md}"

heading=$(grep -m1 -F -e "## [${version}]" -e "## ${version}" "$file") || {
  echo "no CHANGELOG section for ${version} in ${file}" >&2
  exit 1
}

# Heading text as rendered: drop "## ", turn "[text](url)" into "text"; then slugify like GitHub does:
# lowercase, keep letters, digits, "-", "_" and spaces, spaces become "-".
printf '%s\n' "$heading" |
  sed -E 's/^##[[:space:]]+//; s/\[([^]]*)\]\([^)]*\)/\1/g' |
  tr '[:upper:]' '[:lower:]' |
  sed -E 's/[^a-z0-9 _-]//g; s/ /-/g'
