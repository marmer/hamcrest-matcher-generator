#!/usr/bin/env bash
# Fails when the frozen 5.x compatibility suite changed (QS-X2: the suite stays unchanged).
# Compares the git tree hash of the module in HEAD with the hash stored in <module>.tree at the repo root.
set -euo pipefail

cd "$(dirname "$0")/../.."
module=hamcrest-matcher-generator-endtoend-5x-compatibility
expected=$(tr -d '[:space:]' < "$module.tree")
actual=$(git rev-parse "HEAD:$module")

if [ "$expected" != "$actual" ]; then
  echo "::error::$module changed (tree $actual, expected $expected)."
  echo "The 5.x compatibility suite must stay unchanged (QS-X2). Revert the change; only update $module.tree with the owner's consent."
  exit 1
fi
echo "$module unchanged ($actual)"
