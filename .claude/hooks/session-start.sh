#!/bin/bash
# Pre-fetches Maven dependencies once per cloud session so parallel subagents can build offline (-o)
# instead of each hitting Maven Central (HTTP 429, see CLAUDE.md "Known Pitfalls").
set -uo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR"

# Maven may store a 429 response body as a .pom, which breaks every later build ("Non-parseable POM").
remove_rate_limit_poms() {
  grep -rlZ --include='*.pom' 'exceeded rate limits' "$HOME/.m2/repository" 2>/dev/null | xargs -0 -r rm -f
}

remove_rate_limit_poms
# install (tests skipped) resolves the build plugins and dependencies and installs the reactor modules;
# go-offline then fetches remaining plugin dependencies such as the surefire/failsafe providers.
if ./mvnw -B -q -DskipTests install && ./mvnw -B -q dependency:go-offline; then
  echo "Maven dependencies pre-fetched; subagents can build with -o." >&2
else
  echo "Maven pre-fetch incomplete (often a Maven Central HTTP 429); builds fall back to online resolution." >&2
fi
remove_rate_limit_poms
exit 0
