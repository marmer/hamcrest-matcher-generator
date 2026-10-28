#!/usr/bin/env bash
# Runs the implementer brief's verify step in order and stops at the first failure.
#
# Usage: scripts/agents/verify.sh [--latest] [--release] [--fresh] [--only <step>]
#   --latest        also run the verify build on ~/.jdks/latest
#   --release       also run the release dry run and scripts/ci/check-release-artifacts.sh (online)
#   --fresh         also run verify against a throw-away local repository without the project's own artifacts
#   --only <step>   run just that step
# Steps: spotless, install, verify, verify-jdk17, verify-latest, fresh-repo, domain-docs, release
# Each step logs to ${VERIFY_LOG_DIR:-<mktemp -d>}/<step>.log. Stdout: one line per step
# (`ok <step>` or `FAILED <step>: <log>`), after a failure the log's first 30 [ERROR] lines.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."
logdir="${VERIFY_LOG_DIR:-$(mktemp -d)}"
mkdir -p "$logdir"

usage() { sed -n '2,/^[^#]/{/^#/s/^# \{0,1\}//p}' "$0"; }

latest=false release=false fresh=false only=""
while [ $# -gt 0 ]; do
  case "$1" in
    --latest) latest=true ;;
    --release) release=true ;;
    --fresh) fresh=true ;;
    --only) only="${2:?--only needs a step}"; shift ;;
    *) usage; exit 2 ;;
  esac
  shift
done

if [ -n "$only" ] && { "$latest" || "$release" || "$fresh"; }; then
  echo "--only cannot be combined with --latest, --release or --fresh" >&2
  exit 2
fi

step_spotless() {
  local version
  version=$(sed -n 's|.*<spotless.version>\(.*\)</spotless.version>.*|\1|p' pom.xml | head -1)
  if [ -z "$version" ]; then
    echo "spotless.version not found in pom.xml" >&2
    return 1
  fi
  ./mvnw -o -B "com.diffplug.spotless:spotless-maven-plugin:${version}:apply"
}
step_install() { ./mvnw -o -B clean install -DskipTests; }
step_verify() { ./mvnw -o -B clean verify; }
step_verify-jdk17() { JAVA_HOME="$HOME/.jdks/17" ./mvnw -o -B clean verify; }
step_verify-latest() { JAVA_HOME="$HOME/.jdks/latest" ./mvnw -o -B clean verify; }
step_fresh-repo() {
  # CI verifies on a fresh runner; a shared ~/.m2 holds the artifacts of earlier installs and hides missing ones.
  # The throw-away repository is a hard-link copy of the seed minus the project's own artifacts.
  local seed="${VERIFY_SEED_REPO:-$HOME/.m2/repository}" repo status=0
  repo="$(mktemp -d "$(dirname "$seed")/fresh-repo.XXXXXX")"
  cp -al "$seed/." "$repo/" 2>/dev/null || cp -a "$seed/." "$repo/"
  rm -rf "$repo/io/github/marmer/testutils"
  # Offline while the seed has everything; a gap ("offline mode") repeats the build online.
  ./mvnw -o -B clean verify "-Dmaven.repo.local=$repo" 2>&1 | tee "$logdir/fresh-repo-offline.log" || status=$?
  if [ "$status" -ne 0 ] && grep -q "offline mode" "$logdir/fresh-repo-offline.log"; then
    status=0
    ./mvnw -B clean verify "-Dmaven.repo.local=$repo" 2>&1 || status=$?
  fi
  rm -rf "$repo"
  return "$status"
}
step_domain-docs() { python3 scripts/agents/check-domain-docs-sync.py; }
step_release() {
  # The steps run inside `if`, where set -e is off: pipefail plus `|| return 1` stops at a failed Maven build
  # (tee hides nothing) before the artifact check reads the log.
  ./mvnw -B -P release verify -DskipTests -Dgpg.skip=true 2>&1 | tee release-dry-run.log || return 1
  scripts/ci/check-release-artifacts.sh release-dry-run.log
}

steps=(spotless install verify verify-jdk17)
if [ -n "$only" ]; then
  steps=("$only")
else
  if "$latest"; then steps+=(verify-latest); fi
  if "$fresh"; then steps+=(fresh-repo); fi
  steps+=(domain-docs)
  if "$release"; then steps+=(release); fi
fi

for step in "${steps[@]}"; do
  log="$logdir/$step.log"
  if ! declare -F "step_$step" >/dev/null; then
    echo "unknown step: $step" >&2
    exit 2
  fi
  if "step_$step" >"$log" 2>&1; then
    echo "ok $step"
  else
    echo "FAILED $step: $log"
    grep -m 30 '\[ERROR\]' "$log" || tail -n 30 "$log"
    exit 1
  fi
done
