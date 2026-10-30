# Maintaining hamcrest-matcher-generator

This runbook lets anyone with admin rights on the repository, or an agent session working for the maintainer, act when
the autopilot turns red. The project runs on bots: Renovate merges green dependency pull requests, release-please keeps
a Release pull request up to date, and the release workflow publishes what was merged. The maintainer steps in when
something stays red.

Sources: the workflows in `.github/workflows/`, the scripts in `scripts/ci/`, `renovate.json`,
`release-please-config.json`, arc42 chapter 11 (`docs/src/docs/asciidoc/architecture/_chapters/11_technical_risks.adoc`)
and the decisions in `docs/adr/`. Where this file cannot confirm a fact from the repository, the text says
"(assumption)" and the point is listed under [Assumptions](#assumptions).

Never print, paste or commit a secret value, not into an issue, a pull request, a log or a chat. The credential scripts
never print them either.

## Credentials

Four credentials keep the autopilot running: the Sonar token, the GPG signing key, the Central Portal token and the
GitHub App key. The weekly credential reminder
(`.github/workflows/credential-reminder.yml`, Mondays 06:17 UTC, also on `workflow_dispatch`) checks them and opens or
updates the issue "Credential renewal due" 30 days before an expiry.

| Credential | Used by | Storage location | Expiry | Renewal |
|---|---|---|---|---|
| `SONAR_TOKEN` (SonarQube Cloud user token) | Sonar job in `ci-build.yml`, credential reminder | Repository secret | The token has no fixed validity period. SonarQube Cloud removes it after 60 days without use, so the real risk is the CI Sonar job and the weekly credential reminder both stopping for 60 days. The reminder reads `expirationDate` from the API; this token has none, so the reminder raises no Sonar item | SonarQube Cloud, My Account, Security: create a new user token, replace the `SONAR_TOKEN` secret, then run the Credential reminder with `workflow_dispatch` |
| `MAVEN_GPG_KEY` and `MAVEN_GPG_PASSPHRASE` (release signing key) | Publish job in `release.yml` (BC signer), credential reminder | Environment secrets of the environment `release` | Three years (arc42 ch. 11). The reminder reads the earliest expiry of the key's primary and subkeys | Generate a new signing key with a three-year expiry, publish it to the keyservers, replace both secrets, then run the Credential reminder with `workflow_dispatch` |
| `CENTRAL_USERNAME` and `CENTRAL_PASSWORD` (Maven Central Portal user token) | Publish job in `release.yml`, `check-central-token.py` | Environment secrets of the environment `release` | No API exposes it. The owner records the expiry date in the repository variable `CENTRAL_TOKEN_EXPIRES_ON` (YYYY-MM-DD) after each renewal; the token's validity period is an assumption | Central Portal, Account, Generate User Token (the namespace `io.github.marmer` is verified on the legacy username/password account). Replace both secrets, set `CENTRAL_TOKEN_EXPIRES_ON` to the new expiry, run the Credential reminder |
| `RELEASE_APP_PRIVATE_KEY` (GitHub App private key) and `RELEASE_APP_ID` (App ID) | release-please job, announce job in `release.yml` | Key: repository secret. ID: repository variable | No API exposes it. The owner records the expiry date in the repository variable `RELEASE_APP_KEY_EXPIRES_ON` (YYYY-MM-DD); the key's validity period is an assumption | GitHub App settings: generate a new private key, replace `RELEASE_APP_PRIVATE_KEY`, set `RELEASE_APP_KEY_EXPIRES_ON`, delete the old key in the App settings, run the Credential reminder |

Notes:

- The `GITHUB_TOKEN` of the workflows is issued per run and needs no renewal.
- Renovate runs as the Mend Renovate GitHub App, installed on the repository; its configuration is `renovate.json`. No
  repository secret holds a Renovate credential (assumption: the installation carries no secret of its own).
- A cloud agent session cannot list repository secrets (`build-pitfalls.md`). A person with admin rights renews
  the credentials in the external services and the secret stores; an agent session can check the result with
  `workflow_dispatch` and the run log.

## Playbooks

### Red Renovate pull request

Renovate merges a pull request on its own once its checks are green (`automerge`, platform auto-merge,
`minimumReleaseAge` 3 days). A red one waits, which is the design: nothing broken gets merged.

1. Open the failing job in the pull request's checks and read the first error.
2. Decide which kind of failure it is:
   - A test or end-to-end module fails on the new dependency version: the update changed behaviour. Fix the project's
     code in a normal pull request, or cap the version (step 3) if the change is not ours to fix now.
   - Maven Enforcer's bytecode check or the JDK 17 leg fails: a dependency needs a newer Java. Use the playbook
     [Dependency that needs a newer Java](#dependency-that-needs-a-newer-java).
   - A PIT, JaCoCo or Sonar scanner update fails: these have their own pull requests, so the cause is that update.
     A threshold is never lowered silently; a lower threshold is a decision for the maintainer.
3. To keep a dependency at its current version, add a `packageRules` entry to `renovate.json` with `allowedVersions`
   and a `description` that names the reason and the last working version. Do not disable checks, and do not merge the
   red pull request by hand.

### Red Release pull request

release-please (job `release-please` in `release.yml`) keeps one Release pull request open on `main`. It uses the
GitHub App token, so the pull request runs its checks. Merging it creates the tag and the GitHub Release; the release
jobs then build, publish and announce.

1. A red Release pull request usually means `main` is red. Fix the cause in a normal pull request to `main`.
   release-please updates the Release pull request on the next push to `main`.
2. Never merge a Release pull request with a red check, and never create a release tag by hand.
3. For 6.0.0 the owner merges the Release pull request by hand (`release-as` in `release-please-config.json`). Release
   pull request auto-merge is enabled only after 6.0.0, and then `release-as` is removed (comment in `release.yml`).

### Failed publish

The publish job runs only after `Verify` in the release build passed, and it signs and deploys with
`central-publishing-maven-plugin`. That plugin waits until Central reports `published`, so a green publish job is the
confirmation.

1. Open the failed job of the `Release` run and read the step that failed: `Deploy to Maven Central` (Central or
   signing), `Ensure the GitHub Release exists` (GitHub), or `Read the Maven coordinates`.
2. Check the Central Portal's deployments for the version. If a failed deployment is left there, drop it before the
   re-run (assumption: the Portal lets the publisher drop a deployment that did not reach `published`).
3. Re-run the release with the existing tag, through the Actions tab (Release, Run workflow, input `tag`, for example
   `6.0.0`), or with the API:

   ```bash
   gh api --method POST repos/marmer/hamcrest-matcher-generator/actions/workflows/release.yml/dispatches \
     -f ref=main -f inputs[tag]=6.0.0
   ```

   The run skips release-please, checks out `refs/tags/<tag>`, runs the build again and publishes that tag.
4. Never move or delete a tag and recreate it. The tag names the sources that were built and published. If a published
   version is wrong, release the next version (assumption: Maven Central does not accept the same version twice).
5. If only the `announce` job failed, the artifacts are already published. Post the comment on the merged Release
   pull request by hand (the text is in the `announce` job of `release.yml`).

### Expiring credential

1. The reminder opens or updates the issue "Credential renewal due", assigned to the owner. It lists each credential
   that expires within 30 days and its renewal steps. An invalid token fails the Credential reminder run red with a
   message that names the credential and links this runbook.
2. Renew it as the [Credentials](#credentials) table says, with a person who has admin rights.
3. Set the repository variable for the Central token or the App key (`CENTRAL_TOKEN_EXPIRES_ON`,
   `RELEASE_APP_KEY_EXPIRES_ON`) to the new expiry date, in `YYYY-MM-DD`. A malformed value fails the run; an unset one
   only warns.
4. Run the Credential reminder with `workflow_dispatch`. It closes nothing by itself: when no credential is due
   within 30 days, the run reports that and the issue is closed by hand.

### Dependency that needs a newer Java

The baseline is Java 17 for the published code ([ADR 0005](docs/adr/0005-java-17-baseline.md)). Build tools and
analysis-only tools may need more; they run in the JDK 21+ profile and do not change the baseline.

1. Confirm the cause in the failing check: Maven Enforcer's bytecode check, or the JDK 17 leg.
2. Choose one:
   - Cap the dependency at the last version that runs on Java 17: add a `packageRules` entry to `renovate.json` with
     `allowedVersions` and a `description` that names the reason and the Java version it needs. The description is the
     comment for the next person.
   - Raise the baseline in a new major version. This is a decision for the maintainer and needs a new ADR that
     supersedes ADR 0005.
3. Never lower the Java target or skip the Enforcer rule to get the pull request green.

### External service outage

The services are GitHub Actions and Pages, SonarQube Cloud, the Mend Renovate app, the Maven Central Portal and
release-please (arc42 ch. 11).

1. Find the service from the failing job. A validation step tells an outage from a bad credential: the token scripts
   report `could not validate SONAR_TOKEN: SonarQube Cloud answered HTTP <code>` (or the Central Portal equivalent)
   for a server error and `SONAR_TOKEN missing or invalid` only for a rejected token. An outage is not a reason to
   renew a credential.
2. Check the service's status page. Wait for recovery, then re-run the failed jobs.
3. The Sonar job is a required check, so a SonarQube Cloud outage blocks every merge. During an outage a person with
   admin rights may merge past the ruleset. Record the reason in the pull request.
4. `Example Gradle` is a required check too. It downloads the Gradle distribution and its wrapper checksums from
   `services.gradle.org` and the example's dependencies from Maven Central; an outage or HTTP 429 there is handled the
   same way: re-run, and only if it persists merge past the ruleset with the reason noted.
5. Known fallbacks (arc42 ch. 11, decisions for the maintainer): Renovate can run self-hosted in GitHub Actions, and
   the Sonar job can be made non-blocking. Both change the workflows or the ruleset, so they are not done in an
   incident without a pull request.

## Rules for every playbook

- Do not disable, skip or weaken a check, and do not lower a threshold. Those go to the maintainer.
- Do not move a tag. Do not publish by hand, and do not bypass a red check except during a service outage, with a
  note in the pull request.
- Keep the credentials out of issues, logs and pull requests.

## Assumptions

- The validity period of the Central Portal token and of the GitHub App private key is not in the repository. The
  repository variables hold the dates the owner records.
- Maven Central does not accept a version that was already published, and a failed deployment can be dropped in the
  Central Portal before a re-run.
- Renovate needs no repository secret of its own.
- The keyserver publication of the GPG key is the step that `check-credential-expiry.py` gives as renewal step; the
  repository does not show which keyserver is used.
