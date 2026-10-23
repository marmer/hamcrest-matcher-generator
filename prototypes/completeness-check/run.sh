#!/usr/bin/env bash
# PROTOTYPE runner for "Shape of the completeness check" (wayfinder map #52, ticket #65).
# Needs JDK 11+ and network access to Maven Central for two jars.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p lib out
for u in org/hamcrest/hamcrest/2.2/hamcrest-2.2.jar org/assertj/assertj-core/3.27.3/assertj-core-3.27.3.jar net/bytebuddy/byte-buddy/1.15.11/byte-buddy-1.15.11.jar; do
  [ -f "lib/$(basename "$u")" ] || curl -sSf -o "lib/$(basename "$u")" "https://repo1.maven.org/maven2/$u"
done
CP="lib/*"
javac --release 11 -d out -cp "$CP" $(find src -name '*.java')
java -cp "out:$CP" proto.Demo
echo "=== H2 Hamcrest, compile-time staged builder: test written before phone existed -> javac says:"
javac --release 11 -d out -cp "out:$CP" before-phone/proto/OldStagedTest.java 2>&1 | grep -v '^Picked up' || true
