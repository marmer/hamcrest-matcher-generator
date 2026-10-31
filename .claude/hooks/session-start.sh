#!/bin/bash
# Pre-fetches Maven dependencies once per cloud session so parallel subagents can build offline (-o)
# instead of each hitting Maven Central (HTTP 429, see `docs/agents/build-pitfalls.md`).
set -uo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR"

# Keep a record of this run; `docs/agents/build-pitfalls.md` points agents here.
exec > >(tee /tmp/session-start.log) 2>&1
echo "session-start: $(date -u +%FT%TZ)"

# Maven may store a 429 response body as a .pom or .jar, which breaks every later build
# ("Non-parseable POM", NoClassDefFoundError). Drop the whole version directory so it is re-fetched.
remove_rate_limit_artifacts() {
  local dirs
  dirs=$(grep -rlZ --include='*.pom' --include='*.jar' 'exceeded rate limits' "$HOME/.m2/repository" 2>/dev/null \
    | xargs -0 -r -n1 dirname | sort -u)
  [ -z "$dirs" ] && return 1
  echo "$dirs" | xargs -r rm -rf
}

# Every JVM prints "Picked up JAVA_TOOL_OPTIONS: ..." (the proxy config) on stderr; keep those lines out of
# the agent's context. JAVA_TOOL_OPTIONS itself stays set. `|| true` keeps "no lines matched" from failing the
# pipeline, so pipefail still reports Maven's exit code.
quiet_jvm() { grep -v '^Picked up JAVA_TOOL_OPTIONS' || true; }

# Names the cause of a failed Maven run from its log: an HTTP 429 only when the log says so, else Maven's first error.
describe_maven_failure() { # <maven-log>
  if grep -qE 'status code: 429|Too Many Requests|exceeded rate limits' "$1"; then
    echo "HTTP 429 from Maven Central"
  else
    local first_error
    first_error=$(grep -m1 '^\[ERROR\]' "$1" | cut -c1-300 || true)
    echo "${first_error:-unknown (no [ERROR] line in the Maven output)}"
  fi
}

# Maven Central answers HTTP 429 under load (`docs/agents/build-pitfalls.md`), and one rate-limited download fails the
# whole pre-fetch. Try up to 4 times, pausing 2, 4 and 8 seconds between failed attempts, and log each failure.
# An attempt also fails when it had to delete rate-limit artifacts, because the next attempt must re-fetch them.
# `prefetch_failure_cause` keeps the reason of the last failed attempt, taken from Maven's own output.
prefetch_failure_cause=""
prefetch_maven_dependencies() {
  local attempt delay=2 mvn_log
  mvn_log=$(mktemp)
  for attempt in 1 2 3 4; do
    # A full install with one fast unit test and one integration test class: surefire and failsafe resolve
    # their test providers only when they actually run tests, which dependency:go-offline misses. The filters leave
    # most code untested, so the JaCoCo floor cannot hold (`jacoco.skip`); without the skip the first module fails
    # and the reactor never reaches the later ones, whose dependencies (assertj-core for the AssertJ backend) stay unfetched.
    if ./mvnw -B -q install -Djacoco.skip=true -Dtest=BeanPropertyMatcherTest -Dit.test=MatcherGenerationProcessorIT \
        -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false 2>&1 | quiet_jvm | tee "$mvn_log"; then
      if ! remove_rate_limit_artifacts; then
        rm -f "$mvn_log"
        return 0
      fi
      prefetch_failure_cause="HTTP 429 from Maven Central (rate-limit responses were stored as artifacts and deleted)"
    else
      prefetch_failure_cause=$(describe_maven_failure "$mvn_log")
    fi
    echo "session-start: Maven pre-fetch attempt $attempt of 4 failed: $prefetch_failure_cause" >&2
    [ "$attempt" -eq 4 ] && break
    sleep "$delay"
    delay=$((delay * 2))
    remove_rate_limit_artifacts
  done
  rm -f "$mvn_log"
  return 1
}

remove_rate_limit_artifacts
if prefetch_maven_dependencies; then
  echo "Maven dependencies pre-fetched; subagents can build with -o." >&2
else
  remove_rate_limit_artifacts
  echo "Maven pre-fetch failed (last cause: $prefetch_failure_cause); see the Maven output above. Builds fall back to online resolution." >&2
fi

# Jars for throwaway API prototypes (scripts/agents/prototype-classpath.sh reads ~/.m2 first).
# AssertJ is a decided 6.0.0 dependency; ByteBuddy backs its soft assertions.
for gav in org.assertj:assertj-core:3.27.3 net.bytebuddy:byte-buddy:1.15.11; do
  ./mvnw -B -q dependency:get -Dartifact="$gav" -Dtransitive=false 2>&1 | quiet_jvm \
    || echo "Prototype jar $gav not pre-fetched; prototype-classpath.sh falls back to curl." >&2
done
remove_rate_limit_artifacts

# Agent worktrees branch off origin/HEAD; without it they start on the stale master (4976680).
git remote set-head origin -a >/dev/null 2>&1 || true

# Tickets must be green on JDK 17 and on the newest GA JDK as pinned here; the container ships only JDK 21. Install
# pinned Temurin releases under ~/.jdks/17 and ~/.jdks/latest (CLAUDE.md "Build & Test Commands"). The pins are updated
# by hand, not by Renovate: MAINTAINING.md, "Update the pinned JDKs of the session-start hook". Update each URL and hash together.
install_jdk() { # <version> <target-dir> <download-url> <sha256>
  [ -x "$2/bin/java" ] && return 0
  local tmp; tmp=$(mktemp -d)
  if curl -fsSL "$3" -o "$tmp/jdk.tar.gz" \
      && printf '%s  %s\n' "$4" "$tmp/jdk.tar.gz" | sha256sum --check --status \
      && mkdir -p "$2" && tar -xzf "$tmp/jdk.tar.gz" -C "$2" --strip-components=1; then
    echo "JDK $1 installed at $2"
  else
    rm -rf "$2"; echo "JDK $1 not installed; builds on it fall back to CI." >&2
  fi
  rm -rf "$tmp"
}
install_jdk "17.0.21+9" "$HOME/.jdks/17" \
  "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.21_9.tar.gz" \
  "8b9b611d917e3dfd8419d3566b5949226f69b052fed7e109c1368add1dfe8711"
install_jdk "27.0.1.0.2+1" "$HOME/.jdks/latest" \
  "https://github.com/adoptium/temurin27-binaries/releases/download/jdk-27.0.1.0.2%2B1/OpenJDK27U-jdk_x64_linux_hotspot_27.0.1.0.2_1.tar.gz" \
  "30cb129f6cf299c852cec0feca9ea2a596d2ee9f26d8605d3a253951f6c8b20f"

# Non-fatal: warn when this branch carries changes that no merged PR has landed (see Merging in docs/agents/wayfinding.md).
# Compares content, not commits: after a squash merge the base is merged in, leaving old commits with an empty diff.
git fetch -q origin 2>/dev/null || true
changed=$(git diff --name-only origin/HEAD HEAD 2>/dev/null | wc -l)
if [ "${changed:-0}" -gt 0 ]; then
  echo "session-start: warning: $changed file(s) on this branch differ from the remote default branch (origin/HEAD); see the Merging section of docs/agents/wayfinding.md" >&2
fi
exit 0
