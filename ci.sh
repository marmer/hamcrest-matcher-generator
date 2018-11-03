#!/bin/bash

set -e

echo
echo --- Building ---
echo
mvn clean install -DskipTests -Dmaven.javadoc.skip -Dmaven.source.skip -Dassembly.skipAssembly -B -V -U

#export JAVA_TOOL_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044"
echo
echo --- Testing ---
echo
cd hamcrest-matcher-generator-maven-plugin
mvn clean verify -V -Dmaven.home=/usr/bin/mvn -Dtest=MatchersMojoTest
