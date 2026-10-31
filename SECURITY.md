# Security Policy

## Supported Versions

Security fixes are provided for the latest patch release in each supported line.

| Version | Supported          |
| ------- | ------------------ |
| 5.1.x   | :white_check_mark: |
| 5.0.x   | :x:                |
| 4.0.x   | :white_check_mark: |
| < 4.0   | :x:                |

## Reporting a Vulnerability

Report vulnerabilities through
[GitHub's private vulnerability reporting page](https://github.com/marmer/hamcrest-matcher-generator/security/advisories/new).
Do not disclose vulnerability details in a public issue.

Maintainers will acknowledge a report within 7 days and aim to provide an
assessment within 30 days. If confirmed, a fix and coordinated disclosure are
targeted within 90 days. The reporter will be kept informed if investigation or
remediation takes longer; declined reports will include an explanation.

## Fixed vulnerabilities in release notes

When a release fixes a run-time vulnerability in the project's own code that already has a CVE or a GitHub security
advisory, the release notes (the GitHub release and `CHANGELOG.md`) name its identifier. Vulnerabilities in dependencies
are fixed by dependency updates (Renovate) and are not listed individually.
