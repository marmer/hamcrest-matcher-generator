# SonarQube Cloud token, quality-gate check and bot PRs

Research done while grilling issue #62 ("Quality gates: what blocks a merge and what runs locally", part of map #52). Researched 2026-10-25 by a subagent against SonarSource docs, the SonarQube Cloud web API and GitHub docs; backfilled from the subagent's report.

**Context:** free SonarQube Cloud org `marmer-github`, CI-based analysis from GitHub Actions; the existing `SONAR_TOKEN` stopped being authorized. Dependency PRs come from the Mend-hosted Renovate app (same-repo branches).

## Short answer

- **Likely cause of the broken token:** tokens without an expiration date "are automatically removed after 60 days of inactivity"; new personal tokens must expire within at most 1 year. Which case applied could not be confirmed.
- **Free plan:** only personal access tokens. Scoped Organization Tokens are Team/Enterprise only.
- **Required check:** SonarQube Cloud posts the quality gate as GitHub check **"SonarCloud Code Analysis"**; `sonar.qualitygate.wait=true` makes the scanner step itself fail on a red gate.
- **Renovate PRs get Actions secrets** (same-repo branches, created by a GitHub App); only fork and Dependabot PRs don't.
- **Token health is checkable** via `api/authentication/validate` and `api/user_tokens/search` (`expirationDate`).

## 1. Token types

- **Personal access tokens (user tokens):** My account > Access Tokens > Personal Tokens (tab visible only with GitHub sign-in). Usable as `sonar.token` for analysis and web API. Available on the free plan ("Free plan users create a personal access token").
- **Scoped Organization Tokens (SOT):** whole org or specific projects, several permission scopes. Team and Enterprise only; announced 2025-09-25.
- **Agent-app tokens:** one-time, max 1 hour, for the SonarQube agent in GitHub agent apps; not for CI.
- **Project analysis tokens:** exist in SonarQube Server; not described in the SonarQube Cloud docs.
- **Free OSS plan** (announced 2026-01-27, public projects): unlimited members, feature-branch analysis, custom quality standards. Whether it includes SOTs is unanswered; migration of existing free orgs is contradictory in the forum (a staff reply says there is no direct conversion; create a new org).

Sources:
- https://docs.sonarsource.com/sonarqube-cloud/managing-your-account/managing-tokens/
- https://docs.sonarsource.com/sonarqube-cloud/administering-sonarcloud/managing-subscription/subscription-plans
- https://www.sonarsource.com/blog/introducing-scoped-organization-tokens-for-sonarqube-cloud/
- https://community.sonarsource.com/t/introducing-the-new-free-oss-plan-in-sonarqube-cloud/176923
- https://community.sonarsource.com/t/switch-sonarqube-cloud-organization-from-free-to-oss-plan/187709

## 2. Expiry and revocation

- **Personal token expiry choices:** 7, 30, 60 or 90 days, or a custom date from 1 day to 1 year. No "never" in the UI.
- **60-day rule:** "Tokens without an expiration date are automatically removed after 60 days of inactivity." The UI shows the removal date once within 7 days; such legacy tokens read "Never (if used)".
- `api/user_tokens/generate` takes an optional `expirationDate`; whether omitting it still creates a non-expiring token was not tested (if so, the 60-day rule applies).
- SOTs may have "no expiry" (not recommended); irrelevant on the free plan.
- **Other confirmed causes:** manual revoke; "When a user is deleted, their user access tokens are also deleted."
- **Not confirmed:** that removal from the org or GitHub re-authorization invalidates a token (losing membership would remove project permissions, not the token).

Source: https://docs.sonarsource.com/sonarqube-cloud/managing-your-account/managing-tokens/

## 3. GitHub check, required checks, `sonar.qualitygate.wait`

- SonarQube Cloud "adds the quality gate status as a GitHub check" named **"SonarCloud Code Analysis"**; the docs recommend a ruleset requiring it (plus "Require branches to be up to date"). Needs the project bound to the repo with PR integration. https://docs.sonarsource.com/sonarqube-cloud/managing-your-projects/administering-your-projects/devops-platform-integration/github
- SonarQube Server names it "SonarQube Code Analysis"; use the name that appears after the first run (GitHub lists only checks seen in the last week).
- **Not confirmed:** Checks API vs commit-status API; whether a check is posted for push-to-main analyses (docs only describe PR decoration).
- Official GitHub Actions example runs on `push` to main and `pull_request` (`opened`, `synchronize`, `reopened`). https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/ci-based-analysis/github-actions-for-sonarcloud/
- **Free plan:** branch analysis "only main branch"; PR analysis only if the target branch is the main branch (fine for PRs into `main`). https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/pull-request-analysis
- **`sonar.qualitygate.wait`** (default `false`) "Forces the analysis step to poll the server instance and wait for the Quality Gate status"; the scanner exits non-zero on a failed gate. `sonar.qualitygate.timeout` defaults to 300 s. https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/analysis-parameters/parameters-not-settable-in-ui/

## 4. Scanner version and invocation

- **sonar-maven-plugin 5.8.0.7211** (2026-09-07). https://repo1.maven.org/maven2/org/sonarsource/scanner/maven/sonar-maven-plugin/maven-metadata.xml
- Recommended single command: `mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.organization=<org> -Dsonar.projectKey=<key>`; token in env `SONAR_TOKEN`; pin the version in `<pluginManagement>`; default projectKey `${groupId}:${artifactId}`. https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/scanners/sonarscanner-for-maven/
- **JRE auto-provisioning** (default since scanner 5.0): host needs only Java 11. Without it (`sonar.scanner.skipJreProvisioning=true`) Java 21+ is required ("As of July 20, 2026, Java 21 or later is required"). https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/scanner-environment/general-requirements

## 5. Secrets in `pull_request` workflows

- Fork PRs: "secrets are not passed to the runner when a workflow is triggered from a forked repository".
- Dependabot PRs are "treated as though they are from a forked repository"; "Secrets are not available to workflows triggered by Dependabot events."
- PRs created with a GitHub App installation token run `pull_request` workflows automatically; only `GITHUB_TOKEN`-triggered events are suppressed.
- **Inference (no GitHub page names Renovate):** Renovate's same-repo branches are neither fork nor Dependabot PRs, so they get repository Actions secrets including `SONAR_TOKEN`. Renovate's docs imply the Mend app creates branches in the repo (vs "Forking Renovate").

Sources:
- https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow
- https://docs.renovatebot.com/getting-started/running/

## 6. Checking the token from a scheduled workflow

- **`GET https://sonarcloud.io/api/authentication/validate`** with `Authorization: Bearer <token>`. Tested live: a bogus token returns **HTTP 200 with `{"valid":false}`**, so check the body, not the status. It "Returns true for anonymous user": make sure the header is sent.
- **`GET https://sonarcloud.io/api/user_tokens/search`** (authenticated) lists the user's tokens with `name`, `createdAt`, `type`, `expirationDate` (when set), and `lastConnectionDate` (updated about hourly). Tested live: a bogus token gets HTTP 401.
- Regular authenticated use updates last-use, which would also prevent the 60-day removal of a non-expiring token (not tested).

Sources: https://sonarcloud.io/api/webservices/list, https://sonarcloud.io/api/webservices/response_example?controller=api/user_tokens&action=search

## Not confirmed

- Whether removal from the org or GitHub re-authorization revokes tokens.
- Checks API vs commit status, and whether a check is posted for push-to-main analyses.
- SOT and PR-analysis availability on the Free OSS plan; migration of existing free orgs.
- Whether `api/user_tokens/generate` without `expirationDate` still creates a non-expiring token.
