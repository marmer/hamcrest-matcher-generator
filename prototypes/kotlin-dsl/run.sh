#!/usr/bin/env bash
# PROTOTYPE runner for "Shape and delivery of the Kotlin DSL" (wayfinder map #52, ticket #67).
# Needs JDK 17+ and the jars below in ~/.m2 (fetched by scripts/agents/prototype-classpath.sh).
set -euo pipefail
cd "$(dirname "$0")"
root=../..
KV=${KV:-2.2.21}  # Kotlin version; KV=2.1.20 shows K4 failing (Java getter references need 2.2)
CP=$("$root/scripts/agents/prototype-classpath.sh" org.hamcrest:hamcrest:2.2 org.assertj:assertj-core:3.27.3 \
  net.bytebuddy:byte-buddy:1.15.11 org.jspecify:jspecify:1.0.0 org.jetbrains.kotlin:kotlin-stdlib:${KV} 2>/dev/null)
KC=$("$root/scripts/agents/prototype-classpath.sh" org.jetbrains.kotlin:kotlin-compiler:${KV} \
  org.jetbrains.kotlin:kotlin-stdlib:${KV} org.jetbrains.kotlin:kotlin-script-runtime:${KV} \
  org.jetbrains.kotlin:kotlin-reflect:1.6.10 org.jetbrains.intellij.deps:trove4j:1.0.20200330 \
  org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0 org.jetbrains:annotations:13.0 2>/dev/null)
kotlinc() { java -cp "$KC" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 "$@" 2>&1 | grep -v '^Picked up\|^warning:' || true; }
rm -rf out && mkdir -p out
javac --release 17 -nowarn -d out -cp "$CP" $(find java -name '*.java') 2>&1 | grep -v '^Picked up\|^Note:' || true
kotlinc -cp "out:$CP" -d out $(find kotlin -name '*.kt')
java -cp "out:$CP" proto.demo.MainKt 2>&1 | grep -v '^Picked up'
# Compile-time checks: each file in compile/ must or must not compile; print kotlinc's verdict.
for f in compile/*.kt; do
  head=$(sed -n 's#^// @scenario ##p' "$f"); variant=$(sed -n 's#^// @variant ##p' "$f")
  [ -n "$head" ] && echo "##### COMPILE $head"
  echo "=== $variant"
  res=$(kotlinc -cp "out:$CP" -d /tmp/kotlin-dsl-neg "$f" | grep 'error:' | sed 's#^.*/compile/##' | head -3 || true)
  echo "${res:-compiles}"
done
