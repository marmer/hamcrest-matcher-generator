# Shift-left static analysis and security tooling for a Java 11 Maven library

Research for issue #56 (part of map #52). Researched 2026-10-22 against official tool docs and repositories.

**Context:** 6.0.0 is a plain-Java rewrite with a Java 11 baseline (`--release 11`). The repo builds a Maven multi-module project with the wrapper. CI runs a JDK matrix of 11/17/25, SonarCloud via `mvn sonar:sonar`, CodeQL and JaCoCo. The goal is minimal owner effort, so bot PRs must pass the same gates. Only free OSS tooling is in scope, and visible quality signals (badges) are welcome.

> Note: `docs/modernization-plan-6.0.0.md` currently states a **Java 17** baseline. This ticket assumes Java 11, as briefed. The JDK-requirement facts below matter for either baseline. With a Java 17 baseline the "JDK 21+ only" constraints get milder but don't go away.

## Short answer

- **`mvn verify` can run locally:** Maven Enforcer (incl. bytecode level 11), Spotless check, Error Prone + NullAway, and JaCoCo coverage thresholds. All are deterministic, need no token and have a low false-positive burden once set up.
- **The main constraint is the build JDK, not the target:** Error Prone must run on **JDK 21+**, Spotless 3.x on **JRE 17+**, and google-java-format on **JDK 21+**. All three can still produce or check Java 11 code. Put them in a Maven profile activated on `[21,)` so the JDK 11/17 matrix legs only check that the code compiles and the tests pass.
- **CI only (needs GitHub/SaaS context):** SonarCloud, CodeQL, OpenSSF Scorecard, GitHub dependency review and OSV-Scanner's PR diff mode. Secret scanning and push protection are platform features with zero configuration.
- **Dependency vulnerability scanning:** prefer **OSV-Scanner** (no API key, PR mode reports only newly introduced vulnerabilities) plus **GitHub dependency review** over **OWASP Dependency-Check**. Dependency-Check strongly wants an NVD API key, downloads a large database, and is known for CPE false positives.
- **SonarCloud:** keep CI-based analysis. Automatic analysis has no coverage support and can't be combined with CI-based analysis. Locally, SonarQube for IDE in connected mode gives the same rules for free on public projects, but there's no `mvn verify` equivalent.
- **Bot-PR caveat:** Dependabot-triggered workflows get no Actions secrets. `SONAR_TOKEN` must also be stored as a *Dependabot secret*, or the Sonar step must be skipped or made non-blocking for Dependabot PRs.

## Facts per tool (with sources)

### Maven Enforcer (+ MojoHaus extra-enforcer-rules)

- Built-in rules include `dependencyConvergence`, `requireUpperBoundDeps`, `requireJavaVersion`, `requireMavenVersion`, `requirePluginVersions`, `banDuplicatePomDependencyVersions`, `banDynamicVersions` and `requireReleaseDeps`. Enforcer 3.6.3 is listed. <https://maven.apache.org/enforcer/enforcer-rules/index.html>
- `enforceBytecodeVersion` (extra-enforcer-rules 1.12.1) fails the build if a dependency's classes exceed `maxJdkVersion`. It checks transitive dependencies by default, and has `ignoredScopes`/`excludes`/`ignoreClasses`. Multi-release JAR and `module-info` classes are only checked with `strict=true`. <https://www.mojohaus.org/extra-enforcer-rules/enforceBytecodeVersion.html>
- **Local in `mvn verify`:** yes (binds to `validate`). **Maintenance:** low. **False positives:** `dependencyConvergence` can be noisy with test-only dependency trees, so prefer `requireUpperBoundDeps` or exclusions. `enforceBytecodeVersion` should ignore the `test` scope, since test tools such as compile-testing or Mockito may target newer bytecode.
- *Recommendation:* compile with `<maven.compiler.release>11</maven.compiler.release>`, which gives javac API checks against the Java 11 platform. Add `enforceBytecodeVersion maxJdkVersion=11` for runtime/compile scopes, so a bot bump to a Java-17-only dependency fails the gate.

### Error Prone + NullAway

- "Error Prone must be run on JDK 21 or newer." The last versions for older JDKs are 2.31.0 (JDK 11) and 2.42.0 (JDK 17). Code can still be built for older targets with `--release`. <https://errorprone.info/docs/installation>
- The JDK 21 minimum was introduced in v2.43.0. Since v2.46.0 the flag `-XDaddTypeAnnotationsToSymbol=true` is required on JDK 21. The latest version is 2.51.0. <https://github.com/google/error-prone/releases>
- Maven setup: put `error_prone_core` in `annotationProcessorPaths`, plus `-XDcompilePolicy=simple` and `-Xplugin:ErrorProne`. `.mvn/jvm.config` needs `--add-exports`/`--add-opens` entries (JEP 396). Once `annotationProcessorPaths` is set, processors are no longer discovered from the classpath, so every other processor must be listed explicitly. This matters for this repo, whose end-to-end modules run its own processor. <https://errorprone.info/docs/installation>
- NullAway "requires that you build your code with JDK 17 or higher and Error Prone, version 2.36.0 or higher". In practice this means JDK 21+ via current Error Prone. <https://github.com/uber/NullAway>
- **Local in `mvn verify`:** yes, as part of `compile`, but only on JDK 21+. **Maintenance:** medium. Error Prone releases often and occasionally adds required javac flags (see v2.46.0). `.mvn/jvm.config` flags must stay compatible with every JDK used, or be kept out of the JDK 11 path. **False positives:** low for Error Prone's default ERROR-level checks. NullAway needs `@Nullable` annotations (JSpecify) and an `AnnotatedPackages` setting. It has an upfront annotation cost and then low noise.
- *Recommendation:* put it in a `static-analysis` profile activated by `<jdk>[21,)</jdk>`. Error Prone ERRORs fail the build. Start NullAway on the main code only.

### Spotless (formatting)

- "Spotless requires Maven to be running on JRE 17+". For JRE 11, use spotless 2.46.1. "By default, spotless:check is bound to verify maven phase." `ratchetFrom` limits enforcement to files changed against a branch. <https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md>
- The latest spotless-maven-plugin is 3.10.4 (2026-10-08). <https://github.com/diffplug/spotless/blob/main/plugin-maven/CHANGES.md>
- google-java-format must run on a JDK (not a JRE) "equal to or newer than the Java language version of the files being formatted". Its minimum is currently Java 21. <https://github.com/google/google-java-format>
- **Local in `mvn verify`:** yes (`check`); `mvn spotless:apply` fixes violations. **Maintenance:** low. A one-time reformat commit is needed (add it to `.git-blame-ignore-revs`). **False positives:** none (deterministic), though a formatter version bump can reformat files. Pin versions and let bot PRs carry the reformat.
- *Recommendation:* same JDK 21+ profile as Error Prone. The JDK 25 matrix leg and the main build job (JDK 21) enforce it.

### SonarCloud (SonarQube Cloud)

- Automatic analysis supports Java with known limitations: XSS rules are missing, some rules are excluded, and `sonar.sources`/`sonar.tests`/`sonar.inclusions` are not supported. "Code coverage information is not supported." It "is not intended to be used in conjunction with CI-based analysis", and turning it on makes existing CI analyses fail. <https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/automatic-analysis/>
- Scanner for Maven: with JRE auto-provisioning (the default since 5.0) it runs on Java 11+, and the analysis itself uses a provided JDK 21. Without auto-provisioning it needs Java 21+. The scanner reads `sonar.java.source` from `maven.compiler.release`. It needs `SONAR_TOKEN`. The recommended form is a single command: `mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`. <https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/ci-based-analysis/sonarscanner-for-maven/>
- Local option: SonarQube for IDE connected mode uses the server's quality profile, rules and exclusions locally. It's free for public SonarQube Cloud projects and doesn't push issues to the server. <https://docs.sonarsource.com/sonarqube-for-ide/intellij/team-features/connected-mode/>
- **Local in `mvn verify`:** no (needs the server and token). **Maintenance:** low once configured; the pinned scanner 3.9.0 and the separate `sonar:sonar` invocation are outdated. **False positives:** medium. Code smells need occasional "won't fix" triage, but a Quality Gate on *new code* keeps this bounded. Quality Gate and coverage badges are available.
- *Recommendation:* keep CI-based analysis (coverage plus a quality gate badge). Treat the Quality Gate as a required check. For Dependabot PRs, add `SONAR_TOKEN` as a Dependabot secret (see bot caveat below).

### CodeQL

- For Java, default setup uses `build-mode: none` (accuracy rated "Good" vs "Best" for `manual`). Generated code isn't analyzed, and Kotlin isn't analyzed without a build (default setup falls back to autobuild if Kotlin is present). <https://docs.github.com/en/code-security/code-scanning/creating-an-advanced-setup-for-code-scanning/codeql-code-scanning-for-compiled-languages>
- CLI licence: automated or CI analysis is permitted for open source projects hosted on GitHub.com, and the CLI may be used on open source codebases. <https://github.com/github/codeql-cli-binaries/blob/main/LICENSE.md>
- **Local in `mvn verify`:** no (CI or a manual CLI run). **Maintenance:** very low with *default setup*. The current advanced workflow is outdated: its triggers point at `development` and it still sets up JDK 11 + autobuild. **False positives:** low for the default query suite.
- *Recommendation:* switch to CodeQL **default setup** and delete `codeql-analysis.yml`, leaving no workflow file to maintain. The plain-Java 6.0.0 makes `build-mode: none` a good fit.

### OpenSSF Scorecard

- The `ossf/scorecard-action` workflow can be added from the Security tab. `publish_results: true` together with `id-token: write` enables the badge, which "is updated on every run". The default `GITHUB_TOKEN` suffices except for classic branch protection; repository rulesets are readable with the default token. <https://github.com/ossf/scorecard-action>
- **Local:** no (CI, scheduled). **Maintenance:** very low. **False positives:** it's a posture score, not a gate. Some checks (e.g. CII best-practices badge, Fuzzing) will stay low for a small library and should be accepted rather than "fixed".
- *Recommendation:* add it as a weekly scheduled job plus push to the default branch, and show the badge (a portfolio signal). Don't make it a PR gate. Use rulesets instead of classic branch protection, so no PAT is needed.

### Dependency vulnerability scanning

**OSV-Scanner**

- Resolves transitive Maven dependencies from `pom.xml` via deps.dev (`--data-source=native` for Maven Central). Test dependencies are not yet supported in the computed graph. <https://google.github.io/osv-scanner/supported-languages-and-lockfiles/>
- There are reusable GitHub workflows: a PR scan that "will fail if there are new vulnerabilities introduced through the feature branch", and a scheduled full scan with SARIF upload to code scanning. <https://google.github.io/osv-scanner/github-action/>
- Ignores go in `osv-scanner.toml` as `[[IgnoredVulns]]` with `ignoreUntil`/`reason`. <https://google.github.io/osv-scanner/configuration/>
- **Local:** the CLI can be run manually, but there is no Maven plugin, so in practice it's CI. **Maintenance:** low; no key. **False positives:** low (OSV data is package-version based, not CPE-guessed).

**OWASP Dependency-Check**

- The repository moved to `dependency-check/DependencyCheck`. "Users of dependency-check are highly encouraged to obtain an NVD API Key"; without one, updates "will be extremely slow". A shared key across CI builds can hit NVD rate limits (403). An upgrade to ≥ 12.1.0 is mandatory because of NVD API changes. 11.0 changed the H2 DB and forces a full re-download. The minimum runtime is Java 11. The OSS Index analyzer is disabled without credentials, and Sonatype has required tokens since Sep 2025. <https://github.com/dependency-check/DependencyCheck>
- The first run "may take 20 minutes or more". It supports `failBuildOnCVSS` and suppression files. <https://jeremylong.github.io/DependencyCheck/dependency-check-maven/index.html>
- **Local in `mvn verify`:** technically yes (Maven plugin). **Maintenance:** high (API key secret, DB cache, forced upgrades, NVD outages). **False positives:** high-ish (CPE matching needs suppression files). **Not recommended here.**

**GitHub dependency review**

- "The action is available for all public repositories". It can fail on a severity threshold and allow or deny licences. It also considers dependencies submitted via the dependency submission API. <https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/about-dependency-review>
- The Maven `pom.xml` dependency graph supports static transitive labels and automatic dependency submission. <https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/dependency-graph-supported-package-ecosystems>
- **Local:** no (PR diff only). **Maintenance:** very low. **False positives:** low. Good as a PR gate.

### Secret scanning

- "Public repositories: Secret scanning runs automatically for free." <https://docs.github.com/en/code-security/secret-scanning/introduction/about-secret-scanning>
- Push protection for users "is enabled by default" and stops pushes of secrets to public repositories. <https://docs.github.com/en/code-security/secret-scanning/introduction/about-push-protection>
- **Local:** push protection acts at `git push` time; nothing to add to Maven. **Maintenance:** zero. A gitleaks job would be redundant for this repo.

### Bot PRs and secrets

- Dependabot-triggered workflows "receive a read-only `GITHUB_TOKEN` and do not have access to any secrets that are normally available". Only Dependabot secrets are available. <https://docs.github.com/en/code-security/dependabot/troubleshooting-dependabot/troubleshooting-dependabot-on-github-actions>
- Consequence: every gate that needs a secret (`SONAR_TOKEN`, an NVD key) either needs a duplicate Dependabot secret or must tolerate missing secrets. The token-free gates (Enforcer, Spotless, Error Prone, CodeQL default setup, OSV-Scanner, dependency review) work unchanged for bot PRs.

## Recommended toolset for this repo

| Tool | Where | Runs in `mvn verify` locally | JDK constraint | Gate on PR? | Maintenance | False-positive burden |
|---|---|---|---|---|---|---|
| `maven.compiler.release=11` + `-Xlint:all` | all builds | yes | any ≥ 11 | yes | none | low |
| Maven Enforcer (`requireJavaVersion`, `requireMavenVersion`, `requirePluginVersions`, `banDynamicVersions`, `requireUpperBoundDeps`, `enforceBytecodeVersion maxJdkVersion=11` excl. test scope) | all builds | yes (`validate`) | any | yes | low | low |
| Spotless + google-java-format (`check`) | profile `jdk [21,)` | yes (on JDK 21+) | JRE 17+ / JDK 21+ | yes | low | none |
| Error Prone (+ NullAway on main code) | profile `jdk [21,)` | yes (`compile`, on JDK 21+) | JDK 21+ | yes | medium | low |
| JaCoCo `check` (coverage floor) | all builds | yes | any | yes | low | none |
| SonarCloud CI-based (+ SonarQube for IDE locally) | CI build job | no | scanner auto-provisions JDK 21 | Quality Gate on new code | low | medium |
| CodeQL default setup (`build-mode: none`) | GitHub | no | n/a | yes (code scanning check) | very low | low |
| OSV-Scanner reusable PR + scheduled workflows | CI | no (CLI manually) | n/a | yes (new vulns only) | low | low |
| GitHub dependency review action | CI (PR) | no | n/a | yes (severity threshold) | very low | low |
| OpenSSF Scorecard (weekly, badge) | CI | no | n/a | no (signal only) | very low | n/a (posture score) |
| Secret scanning + push protection | GitHub platform | n/a | n/a | implicit | zero | low |
| OWASP Dependency-Check | — | — | — | **not adopted** | high (NVD key, DB) | high |

Notes on the recommendation:

- With the matrix kept at 11/17/25, the JDK 25 leg (and the JDK 21 main job) runs the full analysis profile. The JDK 11/17 legs check that the code compiles against the baseline and that the tests pass. A local `./mvnw verify` on a JDK 21+ dev machine runs exactly the same analysis gates as CI, and on JDK 11/17 it skips them.
- Badges worth showing: CI build, SonarCloud Quality Gate and coverage, OpenSSF Scorecard, and Maven Central version.
- Open the gates in the order: Enforcer + `release 11` → Spotless (one reformat commit) → Error Prone → NullAway, so that each step stays a small PR.

## Not researched / open

- SpotBugs and PMD were not evaluated (they overlap with Error Prone + Sonar). Add them only if Error Prone proves insufficient.
- Exact Error Prone/NullAway interaction with the repo's own annotation processor in the end-to-end modules (`annotationProcessorPaths` must list it) needs a spike.
