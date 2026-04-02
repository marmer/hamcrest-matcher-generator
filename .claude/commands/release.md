Guide me through preparing a release for hamcrest-matcher-generator.

Checklist:

1. **Verify branch**: Confirm we are on `development` and it is up to date with remote.

2. **Run full verification**:
   ```
   ./mvnw clean verify
   ```
   All tests must pass before proceeding.

3. **Check version**: Read the current version from the root `pom.xml`. Ask me what the new version should be (semantic versioning: MAJOR.MINOR.PATCH).

4. **Update version** in all module POMs:
   ```
   ./mvnw versions:set -DnewVersion=X.Y.Z
   ./mvnw versions:commit
   ```

5. **Update CHANGELOG** in `README.md` — add entry for the new version with the changes made.

6. **Run release build** (requires GPG key configured):
   ```
   ./mvnw clean install -P release
   ```

7. **Summarize** what was done and what the next manual steps are (tagging, pushing, Nexus staging).

Note: Do NOT push or create git tags without explicit confirmation from me.
