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

remove_rate_limit_artifacts
# A full install with one fast unit test and one integration test class: surefire and failsafe resolve
# their test providers only when they actually run tests, which dependency:go-offline misses.
if ./mvnw -B -q install -Dtest=BeanPropertyMatcherTest -Dit.test=MatcherGenerationProcessorIT \
    -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false \
    && ! remove_rate_limit_artifacts; then
  echo "Maven dependencies pre-fetched; subagents can build with -o." >&2
else
  remove_rate_limit_artifacts
  echo "Maven pre-fetch incomplete (often a Maven Central HTTP 429); builds fall back to online resolution." >&2
fi
exit 0
