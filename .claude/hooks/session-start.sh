#!/bin/bash
# Pre-fetches Maven dependencies once per cloud session so parallel subagents can build offline (-o)
# instead of each hitting Maven Central (HTTP 429, see CLAUDE.md "Known Pitfalls").
set -uo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR"

# Keep a record of this run; CLAUDE.md "Known Pitfalls" points agents here.
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

remove_rate_limit_artifacts
# A full install with one fast unit test and one integration test class: surefire and failsafe resolve
# their test providers only when they actually run tests, which dependency:go-offline misses.
if ./mvnw -B -q install -Dtest=BeanPropertyMatcherTest -Dit.test=MatcherGenerationProcessorIT \
    -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false 2>&1 | quiet_jvm \
    && ! remove_rate_limit_artifacts; then
  echo "Maven dependencies pre-fetched; subagents can build with -o." >&2
else
  remove_rate_limit_artifacts
  echo "Maven pre-fetch failed; see the Maven output above (an HTTP 429 from Maven Central is a common cause). Builds fall back to online resolution." >&2
fi

# Jars for throwaway API prototypes (scripts/agents/prototype-classpath.sh reads ~/.m2 first).
# AssertJ is a decided 6.0.0 dependency; ByteBuddy backs its soft assertions.
for gav in org.assertj:assertj-core:3.27.3 net.bytebuddy:byte-buddy:1.15.11; do
  ./mvnw -B -q dependency:get -Dartifact="$gav" -Dtransitive=false 2>&1 | quiet_jvm \
    || echo "Prototype jar $gav not pre-fetched; prototype-classpath.sh falls back to curl." >&2
done
remove_rate_limit_artifacts

# Non-fatal: warn when this branch carries changes that no merged PR has landed (see Merging in docs/agents/wayfinding.md).
# Compares content, not commits: after a squash merge the base is merged in, leaving old commits with an empty diff.
git fetch -q origin development 2>/dev/null || true
changed=$(git diff --name-only origin/development HEAD 2>/dev/null | wc -l)
if [ "${changed:-0}" -gt 0 ]; then
  echo "session-start: warning: $changed file(s) on this branch differ from origin/development; see the Merging section of docs/agents/wayfinding.md" >&2
fi
exit 0
