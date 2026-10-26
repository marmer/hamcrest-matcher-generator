# License compliance and snippet scanning for a FOSS Java library (2026)

Research ticket #94 (part of map #52). Researched 2026-10-26 by a subagent against official docs, READMEs, `action.yml` files and source of the tools named below; versions are the newest tags seen with `git ls-remote` / Maven Central metadata that day. No tool was run against this repository: everything about fit and quality is reasoning from the docs.

**Context:** Apache-2.0 Java 11 annotation-processor library (Hamcrest + AssertJ backends, JavaPoet), published to Maven Central, CI on GitHub Actions; only free-for-OSS tooling, no self-hosted infrastructure. Already decided: CycloneDX SBOM per release (cyclonedx-maven-plugin), GitHub dependency review as a required check, CodeQL, SonarCloud; OSV-Scanner rejected. The project license itself is ticket #95 and is not decided here.

Markers: **[Fact]** = stated by the cited primary source; **[Inference]** = my reading of facts; **[Rec]** = recommendation.

## Summary and recommendations

- **The lawyer's statement is still mostly true, with one change.** [Inference] Two kinds of tool are still confused:
  - *License-text detectors* (ScanCode Toolkit, FOSSology's Nomos/Monk/Ojo, Trivy `--license-full`, REUSE lint) find license **notices, headers and SPDX tags** in files. They catch pasted code only if its header or notice was pasted with it.
  - *Knowledge-base snippet matchers* fingerprint the code and compare it to a large corpus of open source: Black Duck, FossID, FOSSA's snippet add-on and **SCANOSS**. Black Duck, FossID and FOSSA's snippet scanning are paid, with no free OSS tier found. The change since "years ago" is **SCANOSS**: its engine and clients are open source, and the Software Transparency Foundation runs a **free public knowledge base (OSSKB)** that its GitHub Action uses by default. FOSSology's own snippet capability is in fact a **SCANOSS agent** that calls the same OSSKB.
- **[Rec] Dependency licenses (full transitive tree):** add **mojohaus `license-maven-plugin` 2.7.1** (`aggregate-add-third-party` with `includedLicenses`, `failOnBlacklist`, `failOnMissing`, `licenseMerges`) to the normal Maven build. It checks every transitive runtime dependency against an allow list on every build and writes a `THIRD-PARTY.txt` attribution file. Keep the **dependency-review `allow-licenses`** list as the PR gate; it only sees changes the PR brings in, and an undetected license does not fail it.
- **[Rec] Snippet / copy-paste detection:** add **`scanoss/gha-code-scan` (v1.7.0)** with the `copyleft` policy, on PRs and on a schedule. Make it **non-blocking at first** (`policies.halt_on_failure: false`): the OSSKB is "best-effort" with no availability guarantee, and its results are "hints of possible identification" that need human review. Only fingerprints are sent, never source (which does not matter much for a public repo anyway). Promote it to a required check only after a few weeks without false positives.
- **[Rec] Not recommended now:** ORT (strong, but a 5 GB-heap Docker pipeline and a rules DSL to maintain; worth it only if a full NOTICE/attribution pipeline becomes a goal), FOSSology server (self-hosted), Black Duck (no free SCA/snippet offer; its free **Coverity Scan** is SAST only), FOSSA (free plan has 5 projects, no snippet scanning; the snippet add-on is paid), ScanCode.io MatchCode matching (no public instance). Trivy license scanning duplicates the Maven plugin.
- **[Rec] Optional cheap complement:** **REUSE** (spec 3.3, `reuse-tool` 6.2.0, `fsfe/reuse-action` v6). It puts an explicit SPDX license and copyright on every file of our own, via headers or one `REUSE.toml`, and `reuse lint` fails on files with no declared license. That declares our own files cleanly. It does not detect foreign code. Decide it together with #95.
- **Generated code (question 3):** [Fact] the FSF position is that a program's output is not covered by the program's license, except for text the program copies into its output. [Fact] OpenAPI Generator and Swagger Codegen state explicitly that "Generated code is intentionally _not_ subject to the parent project license", and jOOQ lists codegen output as "Your license". [Fact] JavaPoet, MapStruct, Immutables, AutoValue and Lombok say nothing about generated output in their READMEs or license pages. **[Rec]** add a short statement in README/arc42 in the OpenAPI Generator style, keep the generated files free of a license header, and make sure no third-party code is copied into the generator's templates. The *runtime* artefact the generated code calls (`BeanPropertyMatcher` in `hamcrest-matcher-generator-dependencies`) is ordinary library use under the project license. #95 has to keep that license permissive.
- **Evidence (question 4):** the CycloneDX SBOM carries only *declared* POM licenses, mapped to SPDX IDs where possible. `THIRD-PARTY.txt` (mojohaus) is the attribution file. The SCANOSS raw JSON (convertible to CycloneDX/SPDX-Lite) is the copy-paste evidence. The dependency-review job log is the PR-level evidence. See the table in section 4.

## 1. Dependency license compliance in CI

### GitHub dependency review (`actions/dependency-review-action` v5.0.0)
- [Fact] Inputs `allow-licenses` (SPDX IDs; fails the PR on licenses not on the list), `deny-licenses` ("deprecated for possible removal in the next major release"), `license-check` (default `true`), `allow-dependencies-licenses` (purl exclusions). `allow-licenses` and `deny-licenses` are mutually exclusive. https://github.com/actions/dependency-review-action
- [Fact] "License data comes from ClearlyDefined". A human-confirmed but unidentified license shows as `LicenseRef-clearlydefined-OTHER`. **"If a dependency's license can't be detected, the action reports it but does not fail."** (README, same URL)
- [Fact] It scans PRs for *dependency changes* (the README says it flags licenses being "introduced"); it "also considers dependencies submitted via the dependency submission API" and is "available for all public repositories". https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/about-dependency-review
- [Fact] Maven in the dependency graph: `pom.xml` parsed, static transitive labels "Supported", automatic dependency submission "Supported". https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/dependency-graph-supported-package-ecosystems
- [Inference] Good as a PR gate and needs no extra build step. Two gaps: (a) dependencies that already exist are never re-checked, and (b) a dependency with an unknown license passes silently. Hence the Maven plugin below as the full-tree check.

### mojohaus `license-maven-plugin` (org.codehaus.mojo, 2.7.1, 2026-01-05)
- [Fact] Goals include `add-third-party` / `aggregate-add-third-party` (third-party file listing each dependency and its license, multi-module variant), `download-licenses`, `check-file-header`, `third-party-report`. https://www.mojohaus.org/license-maven-plugin/
- [Fact] `failOnBlacklist` (fail if a dependency is blacklisted; with `includedLicenses` set, any license outside the list fails), `failOnMissing` (fail if a dependency has no license), `excludedLicenses`, `licenseMerges` / `licenseMergesUrl` (merge name variants such as "Apache 2.0", "The Apache Software License, Version 2.0"), `missingFile` (`src/license/THIRD-PARTY.properties`) + `useMissingFile` for dependencies whose POM declares nothing, `includeTransitiveDependencies` default `true`, `excludedScopes` default `system`. `failIfWarning` is deprecated in favour of the two `failOn*` flags. https://www.mojohaus.org/license-maven-plugin/add-third-party-mojo.html
- [Inference] Runs inside the existing `./mvnw verify` with no network beyond normal resolution, sees the whole transitive tree of every module, and produces the attribution file. It works on **declared** POM licenses only (like the CycloneDX plugin). Set `excludedScopes` to `test,system` (or check test scope separately) so test-only dependencies of the end-to-end modules do not block anything. Matching works on license *names*, so the `licenseMerges` list needs some upkeep.

### mycila `license-maven-plugin` (com.mycila, 5.1.2, 2026-08-21)
- [Fact] Mainly a **source header** checker/formatter (`license:check`, `license:format`, `license:remove`); also lists "Dependency license enforcement — fail the build when dependencies violate your policies". Versions ≥ 5.0.0 run on Java 11; "Version `6.0.0` and later will require Java 17". https://github.com/mathieucarbou/license-maven-plugin
- [Inference] Pick this one if the owner wants per-file Apache headers enforced by Maven rather than REUSE. Its dependency enforcement is less documented than mojohaus' (parameters not on the README; see https://mathieu.carbou.me/license-maven-plugin/).

### CycloneDX Maven plugin (2.9.3, 2026-07-30)
- [Fact] License per component comes from the POM `<licenses>`: resolved to an SPDX ID by **name**, then by **URL** (`LicenseResolver` from cyclonedx-core-java), falling back to the plain name. `includeLicenseText` default `false`. https://github.com/CycloneDX/cyclonedx-maven-plugin/blob/master/src/main/java/org/cyclonedx/maven/DefaultModelConverter.java, https://cyclonedx.github.io/cyclonedx-maven-plugin/makeAggregateBom-mojo.html
- [Inference] The SBOM records *declared* licenses and does not enforce a policy. Gating on it needs another tool (Dependency-Track is self-hosted and therefore excluded; Trivy can read an SBOM, see below).

### Trivy (v0.76.1)
- [Fact] License scanner with categories Forbidden (CRITICAL), Restricted (HIGH), Reciprocal (MEDIUM), Notice/Permissive/Unencumbered (LOW), Unknown; `--license-full` also scans source, Markdown and text files and LICENSE documents; default classifier confidence 0.9; `--ignored-licenses`; custom categories via `trivy.yaml`; SBOM is a supported target. https://trivy.dev/latest/docs/scanner/license/
- [Fact] For `pom.xml`, license detection ✓, but it "Uses maven repository to get information about dependencies. Internet access required"; JAR scanning has no license column. https://trivy.dev/latest/docs/coverage/language/java/
- [Inference] Usable, but it duplicates the Maven plugin with a second license database and another network fetch. `--license-full` is a license-text detector, not a snippet matcher.

### OSS Review Toolkit (ORT 97.2.0; `ort-ci-github-action` v1.2.0)
- [Fact] Scanners: FossID (commercial snippet scanner) and ScanCode recommended; Licensee and SCANOSS (snippet scanner) as reference implementations. The scanner downloads dependency sources and scans them; results are cached per provenance (files, HTTP, PostgreSQL, ClearlyDefined read-only). https://oss-review-toolkit.org/ort/docs/tools/scanner
- [Fact] The GitHub Action defaults: image `ghcr.io/oss-review-toolkit/ort:latest`, `-Xmx5120m`, run list `cache-dependencies, labels, analyzer, evaluator, advisor, reporter, upload-results` (**no `scanner` by default**), report formats `CycloneDx,SpdxDocument,WebApp`, policy rules from `oss-review-toolkit/ort-config` by default, `fail-on: violations` option. PostgreSQL is optional. https://github.com/oss-review-toolkit/ort-ci-github-action/blob/main/action.yml
- [Fact] Reporters exist for CycloneDX, SPDX, static HTML, WebApp and FreeMarker templates (including a NOTICE template). https://github.com/oss-review-toolkit/ort/tree/main/plugins/reporters
- [Inference] The most complete free option (it combines analysis, ScanCode scans of dependency sources, policy-as-code, NOTICE and SBOM), but too heavy for a five-module library, and the Kotlin-script rules need upkeep.

## 2. Snippet / copy-paste detection

### SCANOSS (free OSSKB; `scanoss/gha-code-scan` v1.7.0, `scanoss.py` v1.54.3, engine v6.0.1)
- [Fact] Matching order: whole package (`url`), whole file (`file`), otherwise **snippet** comparison of snippet hashes (`snippet`). https://github.com/scanoss/engine/blob/master/README.md
- [Fact] Fingerprinting is an adaptation of the Winnowing algorithm (Schleimer/Wilkerson/Aiken), gram 30, window 64; the output is a `.wfp` file of hashes. https://github.com/scanoss/scanoss.py/blob/main/WINNOWING.md. SCANOSS docs: "A fingerprint identifies the content without being a copy of it" and source code is never sent to the API. https://docs.scanoss.com/
- [Fact] GitHub Action: default API `https://api.osskb.org/scan/direct`, API key optional; `scanFiles` default `true` (file + snippet scanning), `skipSnippets` default `false`; policies `copyleft` (OSADL copyleft checklist, includes weak copyleft and `-or-later`), `undeclared` (components not declared in `scanoss.json`), `depTrack`; `policies.halt_on_failure` default `true`; writes `scanoss-raw.json`, comments on PRs; MIT-licensed. https://github.com/scanoss/gha-code-scan
- [Fact] Conversion of raw results to CycloneDX, SPDX Lite, CSV and GitLab Code Quality: `scanoss-py cnv`. https://github.com/scanoss/scanoss.py/blob/main/CLIENT_HELP.md
- [Fact] OSSKB terms: "provided free of charge on best-effort basis, without any guarantee of availability or performance"; "STF may limit the number or frequency of transactions per user"; use only "to perform software identification"; results "should be understood as hints of possible identification". No numeric rate limit published. https://www.softwaretransparency.org/terms. The OSSKB is run by the Software Transparency Foundation (Madrid) and covers "millions of OSS components"; users can "identify even small snippets". https://www.softwaretransparency.org/
- [Fact] Commercial SCANOSS tiers start "From € 35K / yr". https://www.scanoss.com/pricing
- [Fact] Licenses: engine GPL-2.0, `scanoss.py` MIT. https://github.com/scanoss/engine, https://github.com/scanoss/scanoss.py
- [Inference] The only free, KB-backed snippet matcher that runs on a GitHub-hosted runner with no infrastructure. The quality of its Java coverage and its false-positive rate are not published (see the unconfirmed points). Typical false positives for this project would be boilerplate such as `equals`/`hashCode`, Maven wrapper scripts and builder-style code, so suppressions in `scanoss.json` will be needed.

### FOSSology (4.7.1, 2026-06-10; `fossology/fossology-action` v1)
- [Fact] Agents: Nomos (regex/heuristics license scanner), Monk (text similarity, Jaccard index), Ojo (SPDX-License-Identifier), Copyright, Keyword, ECC; reports include SPDX 2 and DEP5. https://www.fossology.org/features/. The changelog has a CycloneDX report agent since 4.4.0 and a ScanCode agent. https://github.com/fossology/fossology/blob/master/CHANGELOG.md
- [Fact] FOSSology's snippet capability is the **SCANOSS agent**: "search code snippets over more that 250M URLs … from free osskb.org service or by an on-premise server for SCANOSS Platform"; "**Only** fingerprints are sent". https://github.com/fossology/fossology/blob/master/src/scanoss/README.md
- [Fact] The CI action runs the scanner image (`fossology/fossology:scanner`) with Nomos/Ojo/Copyright/Keyword in `diff` (default), `repo` or `differential` mode; it supports an allowlist (`licenses` to ignore, `exclude` paths); report formats TEXT and SPDX JSON/YAML/RDF/tag-value. The docs do not say when the job fails. https://github.com/fossology/fossology-action, https://github.com/fossology/fossology/wiki/FOSSology-scanners-in-CI
- [Inference] Full FOSSology (with SCANOSS and human clearing) needs a server, which rules it out. The action is license-text detection only: no snippet matching, and the failure behaviour is undocumented.

### ScanCode Toolkit (v32.5.0, 2026-01-15; v33.0.0rc1) / ScanCode.io / `scancode-action`
- [Fact] Detects "the origin (copyrights), license and vulnerabilities of code, packages and dependencies"; license detection is a comparison against a database of license texts; outputs JSON, YAML, HTML, CycloneDX, SPDX. https://github.com/aboutcode-org/scancode-toolkit
- [Fact] ScanCode.io `match_to_matchcode`: "There is currently no public instance of MatchCode.io", and it "can only match for archives, directories, and files from Maven and npm Packages". A "Find Origins" addon can use MatchCode.io and/or SCANOSS. https://scancodeio.readthedocs.io/en/latest/built-in-pipelines.html
- [Fact] `aboutcode-org/scancode-action` (tag v0.1) runs ScanCode.io pipelines in a workflow, outputs JSON/xlsx/SPDX/CycloneDX, `check-compliance` fails on policy issues ("requires to provide Project policies"); "currently in the **beta stage**". https://github.com/aboutcode-org/scancode-action
- [Inference] The best free license-*text* detector, but not a snippet matcher without a self-hosted MatchCode.

### Black Duck
- [Fact] Black Duck was the Synopsys Software Integrity Group, sold in October 2024 to Clearlake Capital and Francisco Partners. The homepage names no free offer. https://www.blackduck.com/
- [Fact] Its free OSS service **Coverity Scan** is static analysis ("Static analysis is a set of processes for finding source code defects and vulnerabilities"); the FAQ mentions no license or snippet scanning. https://scan.coverity.com/faq
- [Inference] Black Duck snippet matching is still commercial only. Coverity Scan overlaps with CodeQL/SonarCloud and adds nothing for licenses.

### FOSSA
- [Fact] Free plan: 5 projects, 10 contributing developers, 5 dependency levels, 5 imported SBOMs, SBOM export. "Snippet Scanning Add-On" is a separate offering ("SBOMs and attribution notices that include snippets"); the page shows no price and no OSS programme. https://fossa.com/pricing.md
- [Inference] Not free for snippet scanning. The free plan's "5 dependency levels" also caps transitive depth.

### AI-written code (side note)
- [Fact] GitHub Copilot "code referencing" checks Copilot suggestions against public GitHub code and records matching URLs and licenses; it applies to Copilot inline suggestions and Copilot Chat only. https://docs.github.com/en/copilot/concepts/completions/code-referencing
- [Inference] This repo's code is largely written by Claude agents, which have no equivalent check. A repository-level snippet scan (SCANOSS) is the only control that covers code from any author or tool, which is another argument for it.

## 3. License status of generated code

- [Fact] FSF: "The output of a program is not, in general, covered by the copyright on the code of the program"; "when a program translates its input into some other form, the copyright status of the output inherits that of the input"; text the program copies into its output keeps its license (the Bison example, hence Bison's exception). https://www.gnu.org/licenses/gpl-faq.en.html#WhatCaseIsOutputGPL, #GPLOutput, #CanIUseGPLToolsForNF
- [Fact] OpenAPI Generator §3.4: "The templates included with this project are subject to the License", "Generated code is intentionally _not_ subject to the parent project license", it is "owned by the user of the software … subject to the licensing terms that you deem appropriate". Swagger Codegen has the same section. https://github.com/OpenAPITools/openapi-generator#34---license-information-on-generated-code, https://github.com/swagger-api/swagger-codegen#license-information-on-generated-code
- [Fact] jOOQ's licensing page lists "jOOQ codegen output: Your license" (codegen tool itself Apache-2.0). https://www.jooq.org/licensing (seen via search snippet; page not fetched in full)
- [Fact] No statement on generated output was found in the READMEs/license pages of JavaPoet (https://github.com/square/javapoet), MapStruct (https://mapstruct.org/development/license/), Immutables (https://github.com/immutables/immutables), AutoValue (https://github.com/google/auto/tree/main/value) or Lombok (https://github.com/projectlombok/lombok/blob/master/LICENSE). Apache's license FAQ does not address generator output either. https://www.apache.org/foundation/license-faq.html
- [Fact] Our generator adds `@javax.annotation.processing.Generated` and no file comment or license header (`MatcherGenerator.kt`, `getGeneratedAnnotation()`).
- [Inference] A generated `<Type>Matcher` is mostly derived from the **user's** input (type and property names) plus small fixed boilerplate from our generator. JavaPoet is only the library our code calls; none of JavaPoet's own text lands in the output. Under the FSF reasoning the boilerplate is our template text under the project license, and that license is permissive (Apache-2.0 today). In practice users have no obligation beyond the runtime dependency they already have on `hamcrest-matcher-generator-dependencies`. The real constraints are (a) #95 must not pick a license whose terms would follow the template text into users' code (e.g. GPL without an exception), and (b) no third-party code may be pasted into the templates.
- [Rec] Add one paragraph to README (and the arc42 "constraints" section) modelled on OpenAPI Generator: generator and templates under the project license; generated matchers belong to the user and are not subject to it; no warranty. Keep generated files free of a license header so that users' own REUSE/header checks do not flag them. Users who run REUSE would cover them via `REUSE.toml`; the `@Generated` annotation already marks them.

## 4. Evidence artefacts and fit with the CycloneDX SBOM

| Tool | Evidence artefact | Fits the CycloneDX SBOM? |
|---|---|---|
| cyclonedx-maven-plugin (decided) | `bom.json/xml` with declared licenses as SPDX IDs (fallback: names) | is the SBOM |
| dependency-review-action (decided) | job log / PR comment (ClearlyDefined data) | separate; GitHub's dependency graph can also export an SBOM |
| mojohaus license-maven-plugin | `THIRD-PARTY.txt` (+ optional downloaded license texts, site report) | complements: the attribution/NOTICE-style file the SBOM lacks; same declared-POM source |
| SCANOSS action | `scanoss-raw.json`, PR comment; `scanoss-py cnv` → CycloneDX / SPDX Lite | separate CycloneDX document (copy-paste findings); merging into the release SBOM is unconfirmed |
| FOSSology action | TEXT or SPDX (JSON/YAML/RDF/tag-value) | SPDX, not CycloneDX |
| ScanCode / scancode-action | JSON, HTML, xlsx, SPDX, CycloneDX | yes (own CycloneDX) |
| Trivy | table/JSON; can scan an SBOM | can read the CycloneDX SBOM |
| ORT | CycloneDX, SPDX, WebApp, NOTICE via templates | yes; would replace rather than add to the plugin SBOM |
| REUSE | `reuse lint` result; `reuse spdx` SPDX document of our own files | SPDX, about our own files, not dependencies |

[Rec] Attach to each release: the CycloneDX SBOM (decided), `THIRD-PARTY.txt`, and the SCANOSS result of the release commit. Together that is evidence for "what we depend on", "under which licenses" and "whether foreign code sits in our sources".

## Unconfirmed points

- SCANOSS OSSKB **coverage of Java/Maven sources**, minimum snippet size and false-positive rate: not published; needs a trial run on this repo.
- OSSKB **rate limits** for a free, keyless GitHub Action: the terms only say STF "may limit"; there are no numbers.
- Whether dependency review checks licenses of **transitive** Maven dependencies that the PR adds indirectly, given that this repo's versions live in the root `dependencyManagement`. Static POM parsing may resolve less than automatic dependency submission would; whether automatic dependency submission is enabled for this repo was not checked.
- Whether ORT's scanner scans the **project's own** source by default (the docs page only describes dependency scanning).
- The FOSSology action's failure semantics (exit code on findings) are not documented.
- FOSSA OSS programme and policy features on the free plan: not on the pricing page.
- jOOQ's "Your license" line was read from a search snippet, not a full page fetch.
- Whether SCANOSS's CycloneDX output can be merged into the plugin's SBOM (e.g. with `cyclonedx-cli merge`) without losing data: not checked.
