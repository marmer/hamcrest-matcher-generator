#!/usr/bin/env bash
# PROTOTYPE runner for "Mismatch message format" (wayfinder map #52, ticket #66).
# Needs JDK 17+ (the record sample) and network access to Maven Central for three jars.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p lib out
for u in org/hamcrest/hamcrest/2.2/hamcrest-2.2.jar org/assertj/assertj-core/3.27.3/assertj-core-3.27.3.jar net/bytebuddy/byte-buddy/1.15.11/byte-buddy-1.15.11.jar; do
  [ -f "lib/$(basename "$u")" ] || curl -sSfL -o "lib/$(basename "$u")" "https://repo1.maven.org/maven2/$u"
done
CP="lib/*"
javac --release 17 -nowarn -d out -cp "$CP" $(find src -name '*.java') 2>&1 | grep -v '^Picked up\|^Note:' || true
java -cp "out:$CP" proto.Demo 2>&1 | grep -v '^Picked up'
