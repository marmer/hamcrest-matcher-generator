#!/bin/bash

set -e

echo
echo --- Building ---
echo
mvn clean install -DskipTests -Dmaven.javadoc.skip -Dmaven.source.skip -Dassembly.skipAssembly -B -V -U

echo
echo --- Testing ---
echo
mvn clean verify -V -Dmaven.home=/usr/bin/mvn

