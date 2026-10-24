#!/usr/bin/env bash
# Prints a javac/java classpath for throwaway API prototypes (wayfinder prototype tickets).
# Usage: CP=$(scripts/agents/prototype-classpath.sh org.hamcrest:hamcrest:2.2 org.assertj:assertj-core:3.27.3)
# Each groupId:artifactId:version is read from ~/.m2 (pre-fetched by the SessionStart hook), otherwise
# resolved with the Maven Wrapper, and as a last resort downloaded with curl and retries: through the
# session proxy Maven Central answers bursts of plain curl requests with sporadic HTTP 404/429.
set -euo pipefail
repo="$HOME/.m2/repository"
root="$(cd "$(dirname "$0")/../.." && pwd)"
cp=()
for gav in "$@"; do
  IFS=: read -r g a v <<<"$gav"
  rel="${g//.//}/$a/$v/$a-$v.jar"
  jar="$repo/$rel"
  if ! unzip -tq "$jar" >/dev/null 2>&1; then
    "$root/mvnw" -B -q -f "$root/pom.xml" dependency:get -Dartifact="$gav" -Dtransitive=false >&2 2>/dev/null || true
  fi
  if ! unzip -tq "$jar" >/dev/null 2>&1; then
    mkdir -p "$(dirname "$jar")"
    for delay in 2 4 8 16 0; do
      curl -sSfL -o "$jar.part" "https://repo.maven.apache.org/maven2/$rel" && unzip -tq "$jar.part" >/dev/null 2>&1 \
        && mv "$jar.part" "$jar" && break
      rm -f "$jar.part"
      [ "$delay" = 0 ] && { echo "prototype-classpath: could not fetch $gav" >&2; exit 1; }
      sleep "$delay"
    done
  fi
  cp+=("$jar")
done
(IFS=:; echo "${cp[*]}")
