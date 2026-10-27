# Implementer brief

The orchestrator hands this file, by path, to every implementer subagent, together with the ticket number `<n>`. You implement ONE ticket in your own git worktree. The flow around you is "One ticket, one review, one PR" in `docs/agents/implementation.md`.

1. **Base**: `git fetch origin main`, then confirm your worktree HEAD contains `origin/main` (`git merge-base --is-ancestor origin/main HEAD`); if not, `git checkout -B ticket-<n> origin/main`. Work on a local branch named `ticket-<n>`.
2. **Read**: the ticket (`gh api repos/marmer/hamcrest-matcher-generator/issues/<n>` plus `/comments`), its spec (long; grep it for the sections you need), `CLAUDE.md`, and any research or prototype branch or ADR the ticket names (`git show origin/<branch>:<path>`, `docs/adr/`). Build only what the ticket asks; every acceptance criterion must be met.
3. **Build**: call the Skill tool with `tdd` and work test-first where the change is testable. Commit after every green cycle: a container restart once killed an implementer, and only committed work survived.
4. **Verify**: only a full-reactor result counts. Run `./mvnw -o -B clean install -DskipTests` before `verify`; use `-pl` only together with `-am`. Then `./mvnw -o clean verify` (default JDK is 21) and `JAVA_HOME=~/.jdks/17 ./mvnw -o clean verify`; also `JAVA_HOME=~/.jdks/latest ./mvnw -o clean verify` when the change touches the build or JDK-specific code.
   - **CI parity**: CI's Build job runs `./mvnw -B clean install -DskipTests`, which `verify` does not cover (the jar-less docs module failed there first). When a `pom.xml` or a file in `.github/workflows/` changes, also run `./mvnw -o -B clean install -DskipTests` (it writes to the `~/.m2` shared with sibling worktrees, so run it alone, not alongside sibling builds).
   - **Release check**: when the change touches the release build, run `./mvnw -B -P release verify -DskipTests -Dgpg.skip=true 2>&1 | tee release-dry-run.log` online, then `scripts/ci/check-release-artifacts.sh`. Locally only `verify` runs (never `deploy`: the auto-mode classifier blocks it as a public surface), so the CycloneDX skip and the bundle contents are checked only by CI's deploy-lifecycle `Release dry run`; central-publishing 0.12.0 stages nothing with `skipPublishing`, so bundle contents are not inspectable anywhere before a real release.
   - If a dependency is missing offline, drop `-o` once to fetch it; on HTTP 429 see CLAUDE.md "Known Pitfalls".
   - No skipped, disabled or weakened tests, no suppressed checks. Never edit `hamcrest-matcher-generator-endtoend-5x-compatibility` or `.agents/skills/`. The code reads like the surrounding code.
   - If domain terms or architecture change, keep `GLOSSARY.md`, `docs/adr/` and the arc42 docs in sync; `python3 scripts/agents/check-domain-docs-sync.py` must pass.
5. **Commit** on `ticket-<n>` with Conventional Commit messages. End each commit message with the co-author trailer from your own context and the `Claude-Session:` line the orchestrator gives you.
   Never put a model name anywhere in the repo or commit text except that trailer.
6. **Before reporting**: `git fetch origin main && git merge --no-edit origin/main` (never rebase), resolve conflicts, re-run the verify builds if anything merged in; the JaCoCo floor then covers the merged code too. No push, no PR, no issue writes: the orchestrator does those.
7. **Report** only through your final answer, and only once every build you started has finished: no build left running. Include branch name, worktree path, head sha, what you built per acceptance criterion, the verify commands you ran and their result (back every claim about history or code with the command that shows it, so the orchestrator can re-run it), and anything left open. Leave session tools (`send_message`, `archive_session`, `create_session`) to the orchestrator.

## Sandbox gotchas

- This sandbox refuses shell commands that mix the literal text "github" with complex shell constructs (pipes, loops, substitutions, heredocs). Put such commands in a script file in your scratchpad and run the file.
- End with no background monitors or loops running: one implementer re-sent its report four times. Stop every command you started in the background before your final answer.

Shared notes from other agents may be in the orchestrator's scratchpad `notes/` directory; add a short `ticket-<n>.md` there if you learn something future tickets need (build pitfalls, where things live).
