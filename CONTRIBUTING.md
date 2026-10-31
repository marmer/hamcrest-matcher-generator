# Contributing

Thank you for helping. Bug reports, enhancement requests and pull requests are welcome, in English.

## Report a bug or ask for an enhancement

Open an issue in the
[issue tracker](https://github.com/marmer/hamcrest-matcher-generator/issues/new/choose). The bug report template asks
for what you need to reproduce the problem. Issues and pull requests are the project's discussion channel; the issue
tracker is public and searchable, so look for an existing issue first.

Do not report a vulnerability in a public issue. [SECURITY.md](SECURITY.md) describes the private way.

## Contribution process

1. Open an issue for anything larger than a typo fix, so the change is agreed before you write it.
2. Fork the repository and work on a branch.
3. Open a pull request against `main`. The maintainer reviews it. A pull request is squash-merged, so its **title** must
   follow [Conventional Commits](https://www.conventionalcommits.org/) (`fix: ...`, `feat: ...`, `docs: ...`): the
   check `PR title` enforces it, and release-please derives the version bump and the release notes from it.
4. The required checks run on every pull request (see below) and must be green before it is merged.

## Requirements for an acceptable contribution

- **Build and tests are green**: `./mvnw clean verify` passes on JDK 21 and on JDK 17 (`JAVA_HOME=<jdk 17> ./mvnw clean
  verify`). `scripts/agents/verify.sh` runs every check in the right order (Spotless, install, verify on both JDKs,
  docs sync).
- **Code style**: Spotless with google-java-format formats the Java code; `./mvnw install` checks it and fails on unformatted code (`scripts/agents/verify.sh` applies it first).
  Error Prone with NullAway runs on JDK 21+ and fails the build on findings; do not suppress a finding to get green.
- **No warnings**: the end-to-end and example modules compile with `-Xlint:all -Werror`, so generated code must
  produce no compiler warning.
- **Tests come with the change** (the test policy, below).
- **Documentation stays in sync**: a change to behaviour updates the user guide and, where it touches architecture or
  terms, `GLOSSARY.md`, `docs/adr/` and the arc42 chapters. README snippets are copied from the example modules by
  `python3 scripts/ci/sync-readme-snippets.py`; `python3 scripts/agents/check-domain-docs-sync.py` checks the domain
  documents. `CLAUDE.md` describes the module structure and the build in detail.
- **License**: your contribution is released under the project's [Apache-2.0 license](LICENSE).

## Test policy

Every major new functionality, and every bug fix, comes with tests of that functionality in the automated test suite,
in the same pull request. Write the test first and see it fail before the change makes it pass.

- Generated code is pinned by golden files (character for character) in the integration tests of each backend,
  for example `MatcherGenerationProcessorIT` and `AssertionGenerationIT`. Regenerate goldens with
  `-Dgolden.update=true` and review the diff; never edit them by hand.
- Runtime behaviour is covered by unit tests (`BeanPropertyMatcherTest`, golden mismatch messages).
- The end-to-end modules apply the annotation to real model classes and assert with the generated code.
- Coverage (JaCoCo, branch floor 0.99) and mutation testing (PIT, profile `mutation`) guard the suite in CI.

## Run the tests

```bash
./mvnw clean verify                                   # unit tests and integration tests, all modules
./mvnw test -pl hamcrest-matcher-generator -Dtest=ClassName          # one unit test class
./mvnw failsafe:integration-test -pl hamcrest-matcher-generator -Dit.test=ClassName   # one integration test class
```

The continuous integration workflow `.github/workflows/ci-build.yml` runs the same build on every pull request.
