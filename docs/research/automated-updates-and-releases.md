# Automated dependency updates and releases with trunk-based development

Research for issue #54 (part of map #52). Researched 2026-10-22 against primary sources.

**Question.** Which free tooling gives this trunk-based Maven project on GitHub (single `main` branch) fully
automated dependency updates (Maven, Maven wrapper, GitHub Actions), auto-merge when all checks are green
(majors included), and automatic releases with version bump and changelog? What are the failure modes, and what
does the owner still have to do?

## Short answer

- **Dependency updates: Renovate (Mend-hosted app, free Community Cloud tier).** It covers Maven, the Maven
  wrapper and GitHub Actions in one config and has native auto-merge. Dependabot does not document Maven wrapper
  support, has no built-in auto-merge, and its runs do not see normal Actions secrets (the SonarCloud step here
  would fail).
- **Auto-merge: Renovate `automerge: true` for everything, majors included, using GitHub's native auto-merge.**
  This needs a ruleset on `main` with required status checks and the repo setting "Allow auto-merge". Set
  `minimumReleaseAge` and a weekly schedule so the bot does not churn.
- **Releases: release-please (`release-type: maven`) plus Conventional Commits and squash merges.** It bumps
  every `pom.xml`, writes `CHANGELOG.md`, tags and creates the GitHub release. Publishing to Maven Central runs
  in the same workflow, gated on `release_created`.
- **Leave out:** JReleaser (it takes the version as input and does not compute it from commits), semantic-release
  (a Node tool, Maven is not first class), and Maven CI-friendly `${revision}` (needs the flatten plugin, and
  release-please edits the POM versions directly).
- **The main trap is `GITHUB_TOKEN`.** Anything it creates (release PR, tag, push from a merge) triggers no
  workflows. Use a small GitHub App token for release-please and for auto-merging its PR.
- **Blocker found:** the `release` profile still deploys to OSSRH (`oss.sonatype.org`, `nexus-staging-maven-plugin`).
  OSSRH reached end-of-life on 2025-06-30, so it must move to `central-publishing-maven-plugin` before any
  automated release can work.

## 1. Current state of this repo (facts, observed 2026-10-22)

| Aspect | Observed | Source |
|---|---|---|
| Default branch | `development` (plus `master`) | GitHub REST `repos/marmer/hamcrest-matcher-generator` |
| `allow_auto_merge` | `false` | same |
| Merge methods | merge, squash and rebase all allowed | same |
| Rulesets | 0 | `repos/.../rulesets` |
| CI trigger | `on: push: branches-ignore: [master]`, no `pull_request` | `.github/workflows/ci-build.yml` |
| CI jobs | `Build` (build, test, JaCoCo, Sonar with `SONAR_TOKEN`), `Verify JDK 11/17/25` | same |
| Project version | `5.1.0` (no `${revision}`) | root `pom.xml` |
| Maven wrapper | takari wrapper 0.5.6, Maven 3.6.3 | `.mvn/wrapper/maven-wrapper.properties` |
| Publishing | `release` profile: `maven-gpg-plugin` 3.0.1, `nexus-staging-maven-plugin` 1.6.8 to `https://oss.sonatype.org/` | root `pom.xml` |
| Dependency bots | none | repo tree |

## 2. Dependency updates: Renovate vs Dependabot

### Facts

**Renovate**

- The Mend-hosted app has a free **Community Cloud** tier for "an unlimited number of public and private
  repositories". Its limits are 1 concurrent job per org, a job for active repos every 4 hours, and a 30-minute
  timeout. A free **Community (OSS) Cloud** plan with more resources exists for OSI-licensed projects, by request.
  <https://docs.renovatebot.com/mend-hosted/overview/>
- The `maven` manager extracts the depTypes `compile`, `provided`, `runtime`, `test`, `import`, `build`
  (plugins/extensions), `parent` and others, and scans `.mvn/extensions.xml`.
  <https://docs.renovatebot.com/modules/manager/maven/>
- The `maven-wrapper` manager matches `.mvn/wrapper/maven-wrapper.properties` and `mvnw(.cmd)` and updates the
  Maven version used by the wrapper. <https://docs.renovatebot.com/modules/manager/maven-wrapper/>
- Auto-merge: `automerge` (default `false`), `automergeType` (default `pr`), `platformAutomerge` (default `true`,
  uses GitHub's native auto-merge), `automergeSchedule`. <https://docs.renovatebot.com/configuration-options/>
- "By default, Renovate will not automerge until it sees passing status checks / check runs for the branch." With
  no checks at all it does not auto-merge unless `ignoreTests: true`. Branches must be up to date with the target
  before auto-merge. Required reviews, CODEOWNERS or committer restrictions block auto-merge.
  <https://docs.renovatebot.com/key-concepts/automerge/>
- `prHourlyLimit` defaults to 2 and `prConcurrentLimit` to 10. `minimumReleaseAge` holds back very new releases.
  `osvVulnerabilityAlerts` (default `false`) uses osv.dev. <https://docs.renovatebot.com/configuration-options/>
- `config:recommended` includes `:dependencyDashboard` and `:semanticPrefixFixDepsChoreOthers`.
  `config:best-practices` adds `helpers:pinGitHubActionDigests`, `:configMigration`, `abandonments:recommended`
  and more. <https://docs.renovatebot.com/presets-config/>
- `:semanticPrefixFixDepsChoreOthers` uses commit type `fix` for Maven depTypes `compile`, `provided`, `runtime`,
  `system`, `import` and `parent`, and `chore` for everything else (test deps, plugins, the wrapper, Actions).
  `:automergeAll` and `:automergeMajor` both just set `automerge: true`.
  <https://docs.renovatebot.com/presets-default/>

**Dependabot**

- Supported ecosystems include `maven` ("Dependabot doesn't run Maven but supports updates to `pom.xml` files")
  and `github-actions`. A Gradle wrapper is documented, a **Maven wrapper is not**.
  <https://docs.github.com/en/code-security/dependabot/ecosystems-supported-by-dependabot/supported-ecosystems-and-repositories>
- Options include `groups`, `cooldown` (default 3 days, not for security updates) and `commit-message`
  (`prefix`, `prefix-development`, `include: scope`). No auto-merge option is documented.
  <https://docs.github.com/en/code-security/dependabot/working-with-dependabot/dependabot-options-reference>
- Auto-merge needs your own workflow (`dependabot/fetch-metadata` plus `gh pr merge --auto`). Dependabot-triggered
  runs read **Dependabot secrets, not Actions secrets**. With a merge queue, `GITHUB_TOKEN` cannot enqueue PRs.
  <https://docs.github.com/en/code-security/dependabot/working-with-dependabot/automating-dependabot-with-github-actions>

### Comparison

| Criterion | Renovate (Mend app) | Dependabot |
|---|---|---|
| Cost | free | free |
| Maven `pom.xml` incl. plugins, parent | yes | yes |
| Maven wrapper | yes (`maven-wrapper` manager) | not documented |
| GitHub Actions | yes, can pin digests | yes |
| Native auto-merge incl. majors | yes (`automerge`, `platformAutomerge`) | no, needs a custom workflow |
| Conventional-commit titles per depType | yes (`fix` for runtime deps, `chore` otherwise) | prefix only (`prefix`/`prefix-development`) |
| Secrets in CI on bot PRs | normal Actions secrets (the app pushes branches in the repo) | Dependabot secrets only, so `SONAR_TOKEN` must be duplicated |
| Overview issue | Dependency Dashboard | none |
| Who runs it | Mend's infrastructure (third party) | GitHub |

### Recommendation

Use **Renovate via the Mend app**. Suggested `renovate.json`:

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": ["config:best-practices", ":semanticCommits"],
  "baseBranches": ["main"],
  "automerge": true,
  "platformAutomerge": true,
  "automergeStrategy": "squash",
  "minimumReleaseAge": "3 days",
  "schedule": ["before 6am on monday"],
  "osvVulnerabilityAlerts": true,
  "packageRules": [
    { "matchUpdateTypes": ["minor", "patch", "digest"], "groupName": "non-major dependencies" }
  ]
}
```

Rationale (recommendation, not fact):

- Grouping plus a weekly schedule keeps it to about one non-major PR and a few majors per week.
- Majors stay separate PRs, so a failing major does not block the rest.
- `minimumReleaseAge` guards against releases that are later pulled or compromised.
- Before switching on autopilot, regenerate the wrapper once with the Apache Maven Wrapper
  (`./mvnw wrapper:wrapper -Dmaven=<current 3.9.x>`) so Renovate maintains a current wrapper rather than takari 0.5.6.
  This is not verified against Renovate's takari handling.

## 3. Auto-merge on a protected trunk

### Facts

- A ruleset rule "Require status checks to pass" can be strict (branch must be up to date) or loose. You can pin
  the expected source app. A bypass list can contain roles, teams or GitHub Apps. "Require a pull request before
  merging" does not itself require approval.
  <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets>
- Auto-merge must be enabled for the repository. Anyone with write access can turn it on for a PR. "Auto-merge is
  disabled if someone without write permissions pushes new changes to the head branch."
  <https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/incorporating-changes-from-a-pull-request/automatically-merging-a-pull-request>
- "Events triggered by the `GITHUB_TOKEN` will not create a new workflow run", except `workflow_dispatch` and
  `repository_dispatch`. `pull_request` runs from such events wait for approval. The fix is a GitHub App
  installation token or a PAT.
  <https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow>
- In personal-account repos, `GITHUB_TOKEN` defaults to read-only `contents`, and "workflows are not allowed to
  create or approve pull requests" unless the setting is enabled.
  <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/enabling-features-for-your-repository/managing-github-actions-settings-for-a-repository>
- `actions/create-github-app-token` mints an installation token from an App client ID and private key.
  <https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/making-authenticated-api-requests-with-a-github-app-in-a-github-actions-workflow>

### Recommendation

**Ruleset on `main`:**

- Require a PR, with 0 required approvals. The repo has a single owner, and required reviews would block bot auto-merge.
- Require the status checks `Build`, `Verify JDK 11`, `Verify JDK 17` and `Verify JDK 25`, in strict mode.
- Block force pushes and deletion.
- Allow squash merge only.
- Leave the bypass list empty, or add only the owner as an emergency exit.

**Repo settings:** enable "Allow auto-merge" and "Automatically delete head branches".

**CI:** trigger on `pull_request` and on `push: branches: [main]`. Required check names must stay stable,
because renaming a job or matrix entry leaves every PR waiting on a check that never reports.

## 4. Automated releases

### Facts

- Conventional Commits: `fix` maps to PATCH, `feat` to MINOR, and `!` or a `BREAKING CHANGE:` footer to MAJOR.
  <https://www.conventionalcommits.org/en/v1.0.0/>
- release-please release types include `java` and `maven`. `maven` "generates SNAPSHOT version after each
  release and updates `pom.xml` automatically". <https://github.com/googleapis/release-please/blob/main/docs/customizing.md>
- The `maven` strategy updates every `pom.xml` recursively. It sets `/project/version`, or falls back to
  `/project/parent/version`. Snapshot bumps arrive as a separate PR labelled `autorelease: snapshot`.
  <https://github.com/googleapis/release-please/blob/main/docs/java.md>
- The config schema has `skip-snapshot` ("do not propose snapshot pull requests"), `extra-files` and `versioning`.
  <https://raw.githubusercontent.com/googleapis/release-please/main/schemas/config.json>
- The manifest config supports `bootstrap-sha`, `last-release-sha`, `changelog-sections`,
  `include-component-in-tag`, `bump-minor-pre-major` and more.
  <https://github.com/googleapis/release-please/blob/main/docs/manifest-releaser.md>
- `googleapis/release-please-action@v4` (runs on `node24`) takes a `token` input that defaults to `GITHUB_TOKEN`.
  Resources it creates with that token "will not trigger future GitHub actions workflows", and the README
  recommends a PAT. Outputs include `release_created`, `tag_name`, `version`, `prs_created`, `pr` and `prs`.
  <https://github.com/googleapis/release-please-action>,
  <https://raw.githubusercontent.com/googleapis/release-please-action/main/action.yml>
- JReleaser takes the version as input (`project.version` / `JRELEASER_PROJECT_VERSION`) and does not derive it
  from commits. <https://jreleaser.org/guide/latest/reference/project.html>
- semantic-release authenticates with `GH_TOKEN`/`GITHUB_TOKEN` and is driven by its Node plugins.
  <https://semantic-release.gitbook.io/semantic-release/usage/ci-configuration>
- Maven CI-friendly versions support only `${revision}`, `${sha1}` and `${changelist}` (Maven 3.5.0-beta-1 and
  later). Installing or deploying them on Maven 3 "you **have to use** the Flatten Maven Plugin".
  <https://maven.apache.org/guides/mini/guide-maven-ci-friendly.html>
- OSSRH reached end-of-life on 2025-06-30. The Central Publisher Portal replaces it.
  <https://central.sonatype.org/news/20250326_ossrh_sunset/>
- `org.sonatype.central:central-publishing-maven-plugin` (0.12.0 shown) has `autoPublish` (default `false`) and
  `waitUntil` (`validated`/`uploaded`/`published`). Credentials are a portal user token in `settings.xml` server
  `central`. <https://central.sonatype.org/publish/publish-portal-maven/>
- `actions/setup-java` can write the server credentials (`server-id`, `server-username-env-var`,
  `server-password-env-var`). It documents GPG signing with the Bouncy Castle signer (`-Dgpg.signer=bc`,
  `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSPHRASE`). <https://github.com/actions/setup-java/blob/main/docs/advanced-usage.md>,
  <https://maven.apache.org/plugins/maven-gpg-plugin/usage.html>

### Options compared (recommendation)

| Option | Computes version | Maven fit | Verdict |
|---|---|---|---|
| **release-please (`maven`)** | yes, from Conventional Commits | updates all POMs, no extra plugin | **use** |
| semantic-release | yes | Node tool, Maven only through community plugins | skip |
| JReleaser | no (version is an input) | strong at release and Maven Central deploy | optional later, not needed |
| CI-friendly `${revision}` set from the tag | external | needs flatten plugin, versions not in Git | skip |

### Recommended release flow

1. A PR (human, or Renovate with a `fix(deps)`/`chore(deps)` title) is squash-merged to `main`.
2. The `release.yml` workflow runs on `push: main`. It mints an App token and runs
   `release-please-action@v4` with `token: <app token>`. The Release PR it opens or updates therefore triggers CI.
3. In the same job, `gh pr merge --auto --squash <pr>` is run with the App token on the Release PR. The PR merges
   only once required checks are green. (Optional gate: auto-merge only for patch/minor, leave majors for the owner.)
4. The merge push (made by the App, not `GITHUB_TOKEN`) triggers `release.yml` again. Now `release_created == true`,
   so it checks out the tag and runs `./mvnw -B -P release deploy` with `central-publishing-maven-plugin`
   (`autoPublish=true`, `waitUntil=published`).
5. Configure `skip-snapshot: true`. This avoids a second bot PR after every release, and `main` simply carries the
   last released version until the next Release PR.

Effect on cadence: only `fix`/`feat`/breaking commits produce a release, and Renovate labels runtime-scope
updates `fix`. With the weekly grouped schedule, that means at most about one patch release per week caused by
dependencies. Test, plugin, wrapper and Actions updates (`chore`) do not release.

## 5. Failure modes

| # | Failure mode | Cause | Mitigation |
|---|---|---|---|
| 1 | Release PR never gets CI, auto-merge waits forever | Release PR created with `GITHUB_TOKEN` | Pass an App token to `release-please-action` |
| 2 | Nothing publishes after a release | A tag/release made with `GITHUB_TOKEN` does not fire `on: release`/`on: push: tags` | Publish in the same job, gated on `release_created` |
| 3 | Bot-merged update does not start a release | The merge was done by a `GITHUB_TOKEN` workflow (Dependabot-style auto-merge), so the push to `main` fires nothing | Renovate merges through its App, so the push does trigger workflows. Never merge with `GITHUB_TOKEN` |
| 4 | Every bot PR waits on a check | Required check renamed or removed, or CI skipped by path filters | Keep job names stable. No path filters on required workflows |
| 5 | Renovate never auto-merges | No checks reported, "Allow auto-merge" off, required reviews, or CODEOWNERS | Settings in §3. Watch the Dependency Dashboard |
| 6 | Bot PRs fail Sonar | Dependabot runs lack Actions secrets | Use Renovate. Also make Sonar non-blocking or skip it when the secret is absent |
| 7 | A major update passes tests but breaks consumers (for example a dependency now needs Java 17 bytecode) | Tests do not cover consumer environments | Keep the JDK 11 matrix job required. The end-to-end modules are the real gate. Optionally leave runtime-scope majors out of auto-merge |
| 8 | Wrong version bump | Non-conventional squash title, or a dependency major labelled `fix` | Squash-only merges. Optional PR-title lint check. Use `release-as` for manual overrides |
| 9 | Deploy fails | Expired portal token, GPG key expired or revoked, Central validation (Javadoc, sources, signatures) | Calendar reminder for key expiry. `waitUntil=published` surfaces errors in the run |
| 10 | Deploy fails today | `release` profile targets the dead OSSRH | Migrate to `central-publishing-maven-plugin` (prerequisite) |
| 11 | Renovate stalls | Free tier: 1 job per org every 4 h, 30-minute timeout, third-party outage | Acceptable for a portfolio project. Self-hosted Renovate in Actions is a fallback |
| 12 | Silent drift | Auto-merge disables itself or a PR fails without anyone noticing | Renovate assigns the owner when checks fail. Enable GitHub notifications for failed workflows |
| 13 | Supply-chain risk from auto-merged Actions/deps | A malicious release gets merged | `minimumReleaseAge`, digest pinning (`config:best-practices`), least-privilege `permissions:` |

## 6. What the owner still has to do

**One-time (needs owner privileges):**

1. Rename or consolidate to a single `main` branch, make it the default, and retire `development` and `master`.
2. Install the Mend Renovate app on the repo and merge the onboarding PR (or commit `renovate.json`).
3. Create a private GitHub App with Contents and Pull requests read/write (Workflows write only if it ever edits
   workflows), install it on the repo, and store `APP_CLIENT_ID` (variable) and `APP_PRIVATE_KEY` (secret).
4. Create a Central Portal user token and add the secrets `MAVEN_CENTRAL_USERNAME`/`MAVEN_CENTRAL_TOKEN`. Verify
   the `io.github.marmer` namespace (groupId `io.github.marmer.testutils`) in the portal.
5. Export or create a GPG signing key, publish it to a keyserver, and add `MAVEN_GPG_KEY` and `MAVEN_GPG_PASSPHRASE`.
6. Settings: enable "Allow auto-merge", squash only, delete head branches. Create the `main` ruleset (§3).
7. Seed release-please: `release-please-config.json` and `.release-please-manifest.json` set to `6.0.0` after the
   final feature release, or `bootstrap-sha`.

**Ongoing:**

- React to failure notifications: a red bot PR that stays open, or a failed deploy.
- Rotate secrets before they expire (GPG key expiry, Central token, App key).
- Occasionally decide on a major update Renovate could not merge (the Dependency Dashboard lists them).
- Make a breaking change (`feat!`) only intentionally.
