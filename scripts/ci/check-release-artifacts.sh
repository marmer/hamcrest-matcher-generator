#!/usr/bin/env bash
# Checks the result of the release dry run. CI runs the deploy lifecycle, the only place the CycloneDX skip shows up:
#   ./mvnw -B -s .github/ci-dummy-settings.xml -P release -DskipEndtoend deploy -DskipTests -Dgpg.skip=true -DskipPublishing=true 2>&1 | tee release-dry-run.log
# Locally (verify only, so the skip check is vacuous):
#   ./mvnw -B -P release verify -DskipTests -Dgpg.skip=true 2>&1 | tee release-dry-run.log
# Every published jar module needs its jar, sources jar, javadoc jar and CycloneDX BOM, and the build log must not
# say that CycloneDX skipped a module (the Central publishing extension replaces deploy, see skipNotDeployed in pom.xml).
# Usage: scripts/ci/check-release-artifacts.sh [build-log]   (default: release-dry-run.log)
set -uo pipefail

cd "$(dirname "$0")/../.."
log=${1:-release-dry-run.log}
version=$(sed -n 's|^    <version>\(.*\)</version>$|\1|p' pom.xml | head -1)
status=0

[ -n "$version" ] || { echo "::error::could not read the project version from pom.xml"; exit 1; }

fail() {
  echo "::error::$1"
  status=1
}

for module in matcher-generator-annotations matcher-generator-core hamcrest-matcher-generator hamcrest-matcher-generator-dependencies hamcrest-matcher-generator-annotationprocessor; do
  for suffix in "" -sources -javadoc; do
    file="$module/target/$module-$version$suffix.jar"
    [ -f "$file" ] || fail "missing $file"
  done
  [ -f "$module/target/bom.json" ] || fail "missing CycloneDX BOM $module/target/bom.json"
done

if [ ! -f "$log" ]; then
  fail "build log $log not found"
elif grep -q "Skipping CycloneDX goal" "$log"; then
  fail "CycloneDX skipped a module: $(grep -m1 'Skipping CycloneDX goal' "$log")"
fi

[ "$status" -eq 0 ] && echo "release artifacts complete for version $version"
exit "$status"
