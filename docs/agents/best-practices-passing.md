# OpenSSF Best Practices badge: "passing" criteria audit

Audit of the "passing" level of the OpenSSF Best Practices badge (ticket #281, spec #104). The owner registers the
project at https://www.bestpractices.dev and pastes the answers from this file (ticket #284).

- **Source of the criteria**: https://www.bestpractices.dev/en/criteria/0, read on 2026-10-31 (the page lists the
  criteria of the passing level; their wording changes, so re-read it before pasting). Criterion ids are in brackets.
- **Audited commit**: `main` after the merge of #280, plus the commits of #281.
- **Status values**: MET, UNMET, N/A, each with a justification. `owner to confirm` marks a fact an agent cannot read
  (repository settings, secrets, private reports). `assumption` marks a judgement that rests on something the agent
  could not verify. MUST criteria that are MET only on an assumption are not final until the owner confirms.
- **Paste text**: the "Justification" column is the text for the badge form. Links are relative to this repository; the
  form needs the full URL `https://github.com/marmer/hamcrest-matcher-generator/blob/main/<path>`.
- "Documentation site" below means https://marmer.github.io/hamcrest-matcher-generator/, built from
  [`docs/src/docs/asciidoc`](../src/docs/asciidoc). See [sites_https](#other) for its present state.

Summary of what needs the owner: [Owner to-do list](#owner-to-do-list) at the end.

## Basics

### Basic project website content

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| description_good | MET | The README states what the project does: it generates Hamcrest matchers and AssertJ assertions for model classes with an annotation processor at compile time, so production code stays free of test code. | [README.md](../../README.md) |
| interact | MET | The README and CONTRIBUTING.md explain how to obtain the software (Maven and Gradle coordinates, Maven Central badge), how to give feedback (GitHub issue tracker with bug and feature templates) and how to contribute. | [README.md](../../README.md), [CONTRIBUTING.md](../../CONTRIBUTING.md), [issue templates](../../.github/ISSUE_TEMPLATE) |
| contribution | MET | CONTRIBUTING.md explains the contribution process: open an issue, fork, open a pull request against main, squash merge with a Conventional Commits title, required checks. | [CONTRIBUTING.md](../../CONTRIBUTING.md) (added in #281) |
| contribution_requirements | MET | CONTRIBUTING.md lists the requirements for an acceptable contribution: green build on JDK 21 and 17, Spotless formatting, Error Prone and NullAway without suppression, no compiler warnings, tests with the change, documentation in sync. | [CONTRIBUTING.md](../../CONTRIBUTING.md) (added in #281) |

### FLOSS license

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| floss_license | MET | The software is released under the Apache License 2.0. | [LICENSE](../../LICENSE) |
| floss_license_osi | MET | Apache-2.0 is approved by the OSI. | [LICENSE](../../LICENSE) |
| license_location | MET | The license is in the file LICENSE at the root of the repository. | [LICENSE](../../LICENSE), [NOTICE](../../NOTICE) |

### Documentation

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| documentation_basics | MET | The README (quick start for Maven and Gradle) and the user guide (getting started, backends, configuration, IDE setup, Kotlin, migration) document the software. Owner to confirm that the documentation site is online (see sites_https). | [README.md](../../README.md), [guide](../src/docs/asciidoc/guide) |
| documentation_interface | MET | The user guide chapters describe the external interface: the `@GenerateMatchers` configuration, the generated Hamcrest and AssertJ API, the completeness check and the mismatch messages; the public API also carries Javadoc (published at javadoc.io). Assumption: the Javadoc is rendered at javadoc.io once 6.0.0 is on Maven Central. | [configuration](../src/docs/asciidoc/guide/configuration.adoc), [hamcrest-backend](../src/docs/asciidoc/guide/hamcrest-backend.adoc), [assertj-backend](../src/docs/asciidoc/guide/assertj-backend.adoc), [mismatch-messages](../src/docs/asciidoc/guide/mismatch-messages.adoc) |

### Other

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| sites_https | UNMET, owner to confirm | The GitHub repository and Maven Central are served over HTTPS. The repository's homepage field is `http://hamcrest-matcher-generator.marmer.online/` (plain HTTP); `https://marmer.github.io/hamcrest-matcher-generator/` redirects to that HTTP address, and on 2026-10-31 both the HTTP and the HTTPS address answered 404 from the agent's network. The owner must publish the documentation site on HTTPS (enable "Enforce HTTPS" in the GitHub Pages settings, or drop the custom domain) and set the homepage field to the HTTPS address, or leave the homepage empty so the repository URL is the project site. | `curl -sI https://marmer.github.io/hamcrest-matcher-generator/`; related tickets #277, #279 |
| discussion | MET | Discussion happens in GitHub issues and pull requests: searchable, addressable by URL, open to anyone with a GitHub account, no proprietary client needed. | [issues](https://github.com/marmer/hamcrest-matcher-generator/issues), [CONTRIBUTING.md](../../CONTRIBUTING.md) |
| english | MET | All documentation is in English and the project accepts bug reports and comments in English. | [README.md](../../README.md), [CONTRIBUTING.md](../../CONTRIBUTING.md) |
| maintained | MET | The project is maintained: `main` received 51 commits since 2026-08-01 (`git log --since=2026-08-01 --oneline \| wc -l`), dependencies are updated by Renovate and releases by release-please. | [MAINTAINING.md](../../MAINTAINING.md), [renovate.json](../../renovate.json) |

## Change Control

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| repo_public | MET | The source repository is public on GitHub with the URL https://github.com/marmer/hamcrest-matcher-generator. | `gh api repos/marmer/hamcrest-matcher-generator --jq .private` returns false |
| repo_track | MET | Git tracks every change with author and date. | `git log` |
| repo_interim | MET | Work lands on main through pull requests, one commit per change, not only as final releases. | `git log --oneline origin/main` |
| repo_distributed | MET | The project uses git. | `git log` |
| version_unique | MET | Every release has a unique version number (for example 4.2.4, 5.1.0), published to Maven Central under that version. | `git tag`, [CHANGELOG.md](../../CHANGELOG.md) |
| version_semver | MET | The project uses Semantic Versioning, stated in CHANGELOG.md; release-please derives the next version from Conventional Commits. | [CHANGELOG.md](../../CHANGELOG.md), [release-please-config.json](../../release-please-config.json) |
| version_tags | MET | Each release is a git tag named after its version (25 tags, latest 5.1.0). | `git tag --sort=version:refname` |
| release_notes | MET, assumption | Each release has human-readable notes in CHANGELOG.md (for example 5.1.0 and 5.0.0 list the user-visible changes and upgrade impact), not a git log dump. Assumption: from 6.0.0 on, release-please writes the CHANGELOG.md section and the GitHub release body from the Conventional Commit titles, which the `PR title` check enforces; the owner confirms the 6.0.0 notes read as a summary, not as a raw log, when the Release pull request is open. | [CHANGELOG.md](../../CHANGELOG.md), [release.yml](../../.github/workflows/release.yml), [pr-title.yml](../../.github/workflows/pr-title.yml) |
| release_notes_vulns | N/A | No release has fixed a run-time vulnerability in the project's own code that had a CVE. SECURITY.md commits to naming such identifiers in the release notes. Assumption: no CVE exists for the project; the agent cannot read private advisories (the public advisory list is empty). Owner to confirm. | [SECURITY.md](../../SECURITY.md) (statement added in #281) |

## Reporting

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| report_process | MET | Users report bugs in the GitHub issue tracker; the issue templates and CONTRIBUTING.md describe how. | [issue templates](../../.github/ISSUE_TEMPLATE), [CONTRIBUTING.md](../../CONTRIBUTING.md) |
| report_tracker | MET | The project uses the GitHub issue tracker for individual issues. | [issues](https://github.com/marmer/hamcrest-matcher-generator/issues) |
| report_responses | MET, assumption | In the last 12 months no bug report from an outside reporter was found among issues updated since 2025-10-31 (GitHub API, authors other than the maintainer and bots), so there is no unanswered report; the maintainer answers reports in the issue. Assumption: spam or deleted reports are not visible to the agent. Owner to confirm. | `gh api "repos/marmer/hamcrest-matcher-generator/issues?state=all&since=2025-10-31T00:00:00Z"` |
| enhancement_responses | MET, assumption | Same finding as report_responses: no outside enhancement request in the last 12 months is unanswered. Owner to confirm. | same query as report_responses |
| report_archive | MET | Issues and their responses stay public and searchable in the GitHub issue tracker. | [issues](https://github.com/marmer/hamcrest-matcher-generator/issues) |
| vulnerability_report_process | MET | SECURITY.md publishes the process for reporting vulnerabilities (GitHub private vulnerability reporting, no public issue, response targets). GitHub shows it as the repository's security policy. | [SECURITY.md](../../SECURITY.md) |
| vulnerability_report_private | MET, owner to confirm | Private reports go through GitHub's private vulnerability reporting, which is enabled (`gh api repos/marmer/hamcrest-matcher-generator/private-vulnerability-reporting` returns `{"enabled":true}`); SECURITY.md links the report form. | [SECURITY.md](../../SECURITY.md) |
| vulnerability_report_response | N/A, owner to confirm | SECURITY.md promises an acknowledgement within 7 days, inside the 14 days of the criterion. Assumption: no vulnerability report was received in the last 6 months; private reports are not visible to the agent. | [SECURITY.md](../../SECURITY.md) |

## Quality

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| build | MET | A Maven build with the Maven Wrapper rebuilds everything from source: `./mvnw clean install`. CI builds it on every push and pull request. | [pom.xml](../../pom.xml), [mvnw](../../mvnw), [ci-build.yml](../../.github/workflows/ci-build.yml) |
| build_common_tools | MET | Maven is the standard build tool for Java libraries (the Gradle example uses Gradle). | [pom.xml](../../pom.xml) |
| build_floss_tools | MET | The build uses only FLOSS tools: Maven, OpenJDK (Temurin) and the Maven plugins listed in the pom. | [pom.xml](../../pom.xml) |
| test | MET | JUnit unit tests, Failsafe integration tests with golden files and end-to-end modules form the automated test suite, released under the same Apache-2.0 license. CONTRIBUTING.md and CLAUDE.md show how to run it (`./mvnw clean verify`); CI runs it. | [CONTRIBUTING.md](../../CONTRIBUTING.md), [ci-build.yml](../../.github/workflows/ci-build.yml) |
| test_invocation | MET | The suite runs the standard Java way: `mvn verify` (Surefire for unit tests, Failsafe for integration tests). | [pom.xml](../../pom.xml) |
| test_most | MET | A JaCoCo branch coverage floor of 0.99 per measured module fails the build, and PIT mutation testing runs in CI. | [pom.xml](../../pom.xml), [ci-build.yml](../../.github/workflows/ci-build.yml) |
| test_continuous_integration | MET | GitHub Actions runs the build and tests on every pull request and push to main, on JDK 17, 21, 25 and 27. | [ci-build.yml](../../.github/workflows/ci-build.yml) |
| test_policy | MET | The project policy is that tests of new functionality are added to the automated test suite in the same pull request, test first; CONTRIBUTING.md states it. | [CONTRIBUTING.md](../../CONTRIBUTING.md) (added in #281), [implementation.md](implementation.md) |
| tests_are_added | MET | The recent feature changes carry tests: for example `feat(assertj): report generated class names claimed twice ...` (#257) changes `AssertionGenerationIT`, and `feat(core): make the mixin and isXLike failure modes ...` (#252) changes `CoreProcessorIT`, `AssertionGenerationIT` and `MatcherGenerationProcessorIT`. | `git show --name-only 563ae5e3 c02267ca` |
| tests_documented_added | MET | CONTRIBUTING.md documents the test policy in the instructions for change proposals. | [CONTRIBUTING.md](../../CONTRIBUTING.md) |
| warnings | MET | The build enables Error Prone with NullAway as errors on JDK 21+, and the end-to-end and example modules compile with `-Xlint:all -Werror`. SonarQube Cloud adds a quality gate. | [pom.xml](../../pom.xml), [endtoend pom](../../hamcrest-matcher-generator-endtoend/pom.xml) |
| warnings_fixed | MET, assumption | Compiler, Error Prone and NullAway findings fail the build, so none stay. Assumption: SonarQube Cloud issues are not all cleared; the backlog is tracked in #275 and the quality gate is a required check. Owner to confirm that the backlog holds no warning that the criterion would count. | [pom.xml](../../pom.xml), ticket #275 |
| warnings_strict | MET | NullAway runs at ERROR level over all project packages, `-Werror` is on for generated code, and the JaCoCo floor is 0.99. | [pom.xml](../../pom.xml) |

## Security

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| know_secure_design | owner to confirm | Only the maintainer can answer this (primary developer knowledge of secure design: least privilege, input validation, economy of mechanism and so on). The agent cannot judge a person. | none |
| know_common_errors | owner to confirm | Only the maintainer can answer this (common vulnerability kinds of Java libraries and annotation processors, such as injection into generated source, unsafe deserialization, path handling, and their mitigations). | none |
| crypto_published | N/A | The software uses no cryptography. | `grep -rl "MessageDigest\|javax.crypto\|SecureRandom" --include=*.java --include=*.kt .` finds nothing |
| crypto_call | N/A | The software implements no cryptography. | same |
| crypto_floss | N/A | No functionality depends on cryptography. | same |
| crypto_keylength | N/A | No security mechanism with keys. | same |
| crypto_working | N/A | No cryptographic algorithms. | same |
| crypto_weaknesses | N/A | No cryptographic algorithms. | same |
| crypto_pfs | N/A | No key agreement protocols. | same |
| crypto_password_storage | N/A | The software stores no passwords. | same |
| crypto_random | N/A | The software generates no keys or nonces. | same |
| delivery_mitm | MET | Source is delivered through GitHub over HTTPS, the artifacts through Maven Central over HTTPS, and the release artifacts are GPG-signed. | [release.yml](../../.github/workflows/release.yml), [pom.xml](../../pom.xml) |
| delivery_unsigned | MET | No hash is fetched over HTTP; the build downloads Maven and the wrapper over HTTPS only. | [maven-wrapper.properties](../../.mvn/wrapper/maven-wrapper.properties) |
| vulnerabilities_fixed_60_days | MET, owner to confirm | Dependency vulnerabilities are fixed promptly: Renovate raises security updates without the release-age delay (`vulnerabilityAlerts`), and the latest Scorecard findings (Jackson advisories) were fixed in #280; Dependabot alerts were cleared in #193. The agent cannot read the Dependabot alert list (HTTP 403), so the owner confirms that no medium or higher alert has been open for more than 60 days. | [renovate.json](../../renovate.json), tickets #193, #280 |
| vulnerabilities_critical_fixed | MET, assumption | Critical findings are fixed ahead of normal work (see #193, #280). Assumption: no critical vulnerability is currently open; owner to confirm with the alert list. | tickets #193, #280 |
| no_leaked_credentials | MET, assumption | Credentials live in GitHub repository and environment secrets, never in the repository ([MAINTAINING.md](../../MAINTAINING.md)); a search for private key blocks finds only a fake placeholder in a unit test. Assumption: no historical secret in the git history (no secret scanner result was available to the agent). | `grep -rn "BEGIN .*PRIVATE KEY" .` finds only [test_check_credential_expiry.py](../../scripts/ci/test_check_credential_expiry.py) |

## Analysis

| Id | Status | Justification (paste text) | Evidence |
|---|---|---|---|
| static_analysis | MET | Error Prone with NullAway runs in the compile phase of every build, and SonarQube Cloud analyses every pull request and main with a blocking quality gate before a release. | [pom.xml](../../pom.xml), [ci-build.yml](../../.github/workflows/ci-build.yml) |
| static_analysis_common_vulnerabilities | MET, assumption | SonarQube Cloud's Java and Kotlin rule sets include vulnerability and security hotspot rules; OpenSSF Scorecard runs weekly. Assumption: the default Sonar way profile is active (owner to confirm). | [ci-build.yml](../../.github/workflows/ci-build.yml), [scorecard.yml](../../.github/workflows/scorecard.yml) |
| static_analysis_fixed | MET, assumption | The Sonar quality gate is a required check, so new findings block merging. Assumption: no confirmed medium or higher exploitable vulnerability is open; the older backlog is tracked in #275. Owner to confirm. | ticket #275 |
| static_analysis_often | MET | The analyses run on every pull request and every push to main. | [ci-build.yml](../../.github/workflows/ci-build.yml) |
| dynamic_analysis | owner to confirm | SUGGESTED only. PIT mutation testing runs in CI, and the test suite runs with the project's code, but no fuzzer or other dynamic analysis tool is used (Scorecard's Fuzzing check is the subject of #282). Whether mutation testing counts as dynamic analysis is a judgement for the owner; answering "Unmet" does not block the badge. | [ci-build.yml](../../.github/workflows/ci-build.yml), ticket #282 |
| dynamic_analysis_unsafe | N/A | The project is written in Java and Kotlin, which are memory safe. | none |
| dynamic_analysis_enable_assertions | MET, assumption | The tests run with Java assertions enabled (Surefire and Failsafe default `enableAssertions=true`; the poms do not override it, `grep -rn enableAssertions pom.xml */pom.xml` finds nothing). | [pom.xml](../../pom.xml) |
| dynamic_analysis_fixed | N/A | No vulnerability was found by dynamic analysis. | none |

## Owner to-do list

1. **sites_https (UNMET, MUST)**: make the project site and the repository homepage field use HTTPS and answer (see the
   row above; related tickets #277, #279).
2. **know_secure_design, know_common_errors (MUST)**: only the owner can answer.
3. **vulnerabilities_fixed_60_days, vulnerabilities_critical_fixed**: check the Dependabot alerts (Security tab); no
   medium or higher alert open for more than 60 days.
4. **vulnerability_report_private, vulnerability_report_response, release_notes_vulns**: confirm private vulnerability
   reporting stays enabled, that no vulnerability report arrived in the last 6 months, and that no CVE exists for the
   project's own code.
5. **report_responses, enhancement_responses**: confirm that no outside report or request in the last 12 months went
   unanswered.
6. **release_notes**: when the 6.0.0 Release pull request is open, confirm the notes read as a summary.
7. **warnings_fixed, static_analysis_common_vulnerabilities, static_analysis_fixed**: confirm the SonarQube Cloud
   backlog (#275) holds no unfixed exploitable vulnerability and that the default rule set is active.
8. **no_leaked_credentials**: confirm that no secret was ever committed (for example through GitHub secret scanning).
9. **dynamic_analysis (SUGGESTED)**: decide whether mutation testing counts; not required for the badge.
10. **documentation_basics, documentation_interface**: confirm the documentation site and javadoc.io are online after
    6.0.0 is released.
