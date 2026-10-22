# Publishing to Maven Central via the Central Portal from GitHub Actions

Research for issue #53 (map #52). Researched 2026-10-22 against primary sources.
Labels: **[Fact]** = stated by the cited source; **[Inference]** = my reading of a source; **[Rec]** = recommendation for this repo.

## TL;DR

- Legacy OSSRH (`oss.sonatype.org`, `nexus-staging-maven-plugin`, `ossrh` server id) is **gone since 2025-06-30**; all OSSRH namespaces were migrated to the Central Portal automatically. The current `release` profile and `distributionManagement` in `pom.xml` cannot work anymore.
- **[Rec]** Use `org.sonatype.central:central-publishing-maven-plugin:0.12.0` with `autoPublish=true` and `waitUntil=published`. It is Sonatype's first-party plugin, needs no extra config files and runs as part of `mvn deploy`. JReleaser is a reasonable alternative but adds a second config surface for little gain here.
- The plugin **requires Maven >= 3.9.2**; the repo's Maven wrapper pins **3.6.3** and must be upgraded (to 3.9.x, not Maven 4: the plugin is documented to fail on Maven 4).
- **GPG (`.asc`) signatures are still mandatory.** Sigstore (`.sigstore.json`) is accepted and validated, but optional. In CI use `maven-gpg-plugin` >= 3.2.x with the pure-Java `bc` signer and the `MAVEN_GPG_KEY` / `MAVEN_GPG_PASSPHRASE` env vars; no `gpg` binary or keyring import step needed.
- **[Rec]** Supply-chain extras that are free and visible: CycloneDX SBOM attached to the deployment (`cyclonedx-maven-plugin`), Sigstore keyless signatures uploaded to Central (`sigstore-maven-plugin`), and GitHub build-provenance attestations (`actions/attest`, SLSA v1.0 Build L2, free for public repos).
- **Unavoidable human steps (one time):** log in to central.sonatype.com and confirm the namespace is listed, generate a Portal user token, create/publish a GPG key, store four secrets in GitHub. After that a release needs no manual step.

## 1. Namespace migration from legacy OSSRH

- **[Fact]** OSSRH reached end of life on June 30, 2025 and was shut down. "All OSSRH namespaces have been migrated to Central Publisher Portal." You log in at central.sonatype.com with the same username/password as for OSSRH. To publish, generate a new Portal user token; OSSRH tokens no longer work. — https://central.sonatype.org/pages/ossrh-eol/ and https://central.sonatype.org/news/20250326_ossrh_sunset/
- **[Fact]** A namespace cannot be active in OSSRH and the Portal at the same time. The namespaces page shows an "OSSRH Namespaces" section for migrated ones; if it is missing and you expected one, contact Central Support. — https://central.sonatype.org/register/namespace/
- **[Fact]** `io.github.<username>` namespaces are verified automatically when signing up with GitHub (username only, not orgs). — https://central.sonatype.org/register/namespace/
- **[Inference]** `io.github.marmer.testutils` is a sub-group of `io.github.marmer` and is covered by it ("any groupId starting with the reverse domain name and as many subsections as you desire"). — same page.
- **[Fact]** A compatibility shim, the OSSRH Staging API (`https://ossrh-staging-api.central.sonatype.com`), lets `nexus-staging-maven-plugin` keep working with a Portal token, but it is meant for migrants that cannot move to the Portal API, with limitations (per-user staging repos, IP isolation). — https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/
- **[Rec]** Do not use the shim. Remove `nexus-staging-maven-plugin`, the `ossrh` `distributionManagement` and also the dead `jcenter.bintray.com` plugin repository (Central discourages `<repositories>`/`<pluginRepositories>` in published POMs anyway: https://central.sonatype.org/publish/requirements/).

**Human step:** log in at https://central.sonatype.com, open Namespaces, confirm `io.github.marmer` is there and verified.

## 2. Publishing tokens

- **[Fact]** Generate at https://central.sonatype.com/usertoken ("Generate User Token"), give it a display name and an expiration. The token is shown once. — https://central.sonatype.org/publish/generate-portal-token/
- **[Fact]** The plugin reads it from a `settings.xml` `<server>` whose id defaults to `central`. — https://central.sonatype.org/publish/publish-portal-maven/
- **[Rec]** Store as GitHub secrets `CENTRAL_TOKEN_USERNAME` / `CENTRAL_TOKEN_PASSWORD` and let `actions/setup-java` write `settings.xml` (`server-id: central`, `server-username: CENTRAL_TOKEN_USERNAME`, `server-password: CENTRAL_TOKEN_PASSWORD`). Put the secrets on a GitHub **environment** (e.g. `maven-central`) so only the release job sees them. Choose a long expiration and put a reminder in the setup checklist: an expired token is the most likely future "release broke" cause. **[Inference]** I found no Sonatype docs for OIDC/trusted publishing to Central (as PyPI/npm have); a long-lived token is still required.

## 3. Publishing tool: `central-publishing-maven-plugin` vs alternatives

| | central-publishing-maven-plugin | JReleaser (`jreleaser-maven-plugin` 1.26.0) | OSSRH Staging API shim |
|---|---|---|---|
| Owner | Sonatype (first party) | Community | Sonatype (compat only) |
| How | Maven extension replaces `deploy`, uploads bundle to Portal API | Stage to local dir (`altDeploymentRepository`), then `jreleaser:deploy` to `https://central.sonatype.com/api/v1/publisher` | old nexus-staging plugin pointed at new URL |
| Signing | not done; you add `maven-gpg-plugin` | signs staged files with PGP itself, computes checksums | you add gpg |
| Extras | none | GitHub releases, changelog, announcements, SBOM/SLSA catalog | none |
| Verdict | **[Rec] use this** | viable alternative | not for new setups |

Facts:
- **[Fact]** Plugin `org.sonatype.central:central-publishing-maven-plugin`, current version **0.12.0** (released 2026-10-13 per `maven-metadata.xml` on repo1), declared with `<extensions>true</extensions>`. Options: `publishingServerId` (default `central`), `autoPublish` (default `false` = manual click in Portal), `waitUntil` = `validated` (default) / `uploaded` / `published` (requires `autoPublish`), `checksums` (default `all`). It does **not** create sources/javadoc jars or signatures and does not enforce all POM metadata. Snapshot publishing supported since 0.7.0. Known issue: "Publishing fails with Maven 4.x.x." — https://central.sonatype.org/publish/publish-portal-maven/
- **[Fact]** The plugin descriptor of 0.12.0 declares `requiredMavenVersion 3.9.2` and `requiredJavaVersion 1.8` (read from `META-INF/maven/plugin.xml` in https://repo1.maven.org/maven2/org/sonatype/central/central-publishing-maven-plugin/0.12.0/central-publishing-maven-plugin-0.12.0.jar). This repo's `.mvn/wrapper/maven-wrapper.properties` pins Maven **3.6.3** → must be bumped (latest 3.9.x on Central is 3.9.16).
- **[Fact]** JReleaser publishes to the Portal via its `mavenCentral` deployer (staging dir + `jreleaser:deploy`), PGP signing via `JRELEASER_GPG_*` env vars, token via `JRELEASER_MAVENCENTRAL_USERNAME`/`_PASSWORD`. — https://jreleaser.org/guide/latest/examples/maven/maven-central.html
- **[Fact]** JReleaser's SLSA catalog "does not generate provenance on its own, you must combine it with a SLSA builder." — https://jreleaser.org/guide/latest/reference/catalog/slsa.html
- **[Rec]** Use the Sonatype plugin. Reason: smallest config, `mvn deploy` stays the single release command, first-party support. The GitHub Release / changelog part that JReleaser would cover belongs to the release-flow ticket (e.g. release-please or semantic-release style tooling) and is independent of how bytes reach Central.

Other Central requirements the build must satisfy — **[Fact]** https://central.sonatype.org/publish/requirements/: `-sources.jar` and `-javadoc.jar` for each jar (placeholder allowed), `.md5`+`.sha1` checksums (plugin generates them), `.asc` per file, POM with `name`, `description`, `url`, `licenses`, `developers`, `scm` (connection, developerConnection, url), non-SNAPSHOT version.

Repo-specific notes:
- **[Inference]** Only `hamcrest-matcher-generator-dependencies`, `hamcrest-matcher-generator-annotationprocessor` and the parent POM are published today (end-to-end modules are excluded via `skipEndtoend=true` in the `release` profile). Keep that: the plugin uploads everything that reaches `deploy`, so the end-to-end modules must stay excluded (profile, or `maven.deploy.skip`/the plugin's `excludeArtifacts`).
- Dokka (`1.4.32`, from the jcenter era) produces the javadoc jar; verify it still runs on the CI JDK or fall back to `maven-javadoc-plugin`/a placeholder javadoc jar (allowed by Central).

## 4. Signing: GPG still required? Sigstore?

- **[Fact]** Every deployed file needs a GPG/PGP `.asc` signature. — https://central.sonatype.org/publish/requirements/
- **[Fact]** Key guidance: primary key with `[SC]` capability; signing subkeys cause trouble ("Maven and Nexus Repository Manager can only verify against a primary key"); publish the public key to `keyserver.ubuntu.com`, `keys.openpgp.org` or `pgp.mit.edu`. — https://central.sonatype.org/publish/requirements/gpg/
- **[Fact]** Sigstore: publishers may add `<file>.sigstore.json` next to each file; the Portal validates them. "Sigstore signature files are not required to publish at this time"; invalid ones produce warnings that will later become errors; Sonatype has "no intention of replacing PGP signatures with Sigstore signatures" and may eventually require both. — https://central.sonatype.org/news/20250128_sigstore_signature_validation_via_portal/
- **[Fact]** `maven-gpg-plugin` 3.2.0+ has `signer=bc` (pure-Java Bouncy Castle, no gpg binary/keyring), key from env `MAVEN_GPG_KEY` (armored secret key), passphrase from `MAVEN_GPG_PASSPHRASE`, and `bestPractices=true` that fails on insecure passphrase config (planned future default). Current version 3.2.8. — https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html, https://maven.apache.org/plugins/maven-gpg-plugin/usage.html
- **[Fact]** `dev.sigstore:sigstore-maven-plugin` (current 2.4.0 on Central) signs each file keyless with GitHub OIDC (`id-token: write`), writes `<file>.sigstore.json`; needs maven-gpg-plugin >= 3.2.5 so GPG does not sign the `.sigstore.json` files, and Maven >= 3.9.2 to avoid checksums of them; Java 11 minimum; each module re-authenticates; sessions last 10 minutes. — https://github.com/sigstore/sigstore-java/blob/main/sigstore-maven-plugin/README.md
- **[Rec]** Upgrade `maven-gpg-plugin` 3.0.1 → 3.2.8, set `<signer>bc</signer>`, `<bestPractices>true</bestPractices>`, drop `<useAgent>true</useAgent>`. Generate a dedicated release key (RSA 4096 or Ed25519, no signing subkey, e.g. 2–3 year expiry), upload public key to `keys.openpgp.org` and `keyserver.ubuntu.com`. Secrets: `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSPHRASE`. Optionally add `sigstore-maven-plugin` as a visible "modern" signature on top (it costs nothing and Central already validates it); since the release build runs on Java 21/25 in CI, the plugin's Java 11 minimum is no problem and the produced artifacts stay Java 11 compatible.

## 5. SBOM (CycloneDX)

- **[Fact]** `org.cyclonedx:cyclonedx-maven-plugin` (current 2.9.3). `makeAggregateBom` builds one BOM for the whole reactor (plus per-module), `makeBom` one per module. The BOM is attached as an extra artifact with classifier `cyclonedx` (`<artifactId>-<version>-cyclonedx.json`/`.xml`), opt-out via `cyclonedx.skipAttach`; README example binds to `package`. — https://github.com/cyclonedx/cyclonedx-maven-plugin
- **[Inference]** Because it is attached, the BOM goes through signing and the Central upload like any other artifact, so consumers can fetch it from Central next to the jar. Many Apache/major projects do exactly this.
- **[Rec]** Add `makeBom` at `package` in the release build (per module, `outputFormat=json` to keep the bundle small), and also feed the annotationprocessor BOM into an SBOM attestation (section 6).

## 6. Build provenance / SLSA attestations

- **[Fact]** GitHub artifact attestations: `actions/attest@v4` with `subject-path` (binaries) and optional `sbom-path` for SBOM attestations (SPDX and CycloneDX predicates supported). Permissions: `id-token: write`, `contents: read`, `attestations: write`. Verify with `gh attestation verify PATH -R ORG/REPO`. — https://docs.github.com/en/actions/how-tos/secure-your-work/use-artifact-attestations/use-artifact-attestations
- **[Fact]** `actions/attest-build-provenance` v4 is now a wrapper over `actions/attest`, which should be used for new implementations. Attestations are available for public repositories on all current GitHub plans (private repos need Enterprise Cloud). — https://github.com/actions/attest-build-provenance
- **[Fact]** Attestations alone give **SLSA v1.0 Build Level 2**; building in a reusable workflow shared across repos can reach Build L3. Public repos use the Sigstore Public Good instance with a public transparency log. — https://docs.github.com/en/actions/concepts/security/artifact-attestations
- **[Inference]** Maven Central itself does not host in-toto/SLSA attestations as a first-class artifact type (Sonatype only mentions in-toto as a future direction in the Sigstore post above). So provenance lives in GitHub's attestation store; consumers verify the jar they downloaded from Central with `gh attestation verify <jar> -R marmer/hamcrest-matcher-generator`. That works because the jar bytes on Central are identical to the ones attested in CI.
- **[Rec]** In the release job, after `mvn deploy`, run `actions/attest` on `*/target/*.jar` and `*.pom` (provenance) and once with `sbom-path` pointing at the CycloneDX JSON. Mention the verify command in the README. SLSA L3 via a shared reusable workflow is not worth it for a single-repo portfolio project; L2 plus Sigstore-signed artifacts plus SBOM is already a credible, visible supply-chain story. Also attach jars + SBOM to the GitHub Release.

## 7. Sketch of the release job (recommendation, not tested)

```yaml
jobs:
  publish:
    runs-on: ubuntu-latest
    environment: maven-central
    permissions:
      contents: write        # GitHub release / tag
      id-token: write        # Sigstore keyless + attestations
      attestations: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          server-id: central
          server-username: CENTRAL_TOKEN_USERNAME
          server-password: CENTRAL_TOKEN_PASSWORD
      - run: ./mvnw -B -P release deploy    # gpg(bc) + cyclonedx + sigstore + central-publishing(autoPublish, waitUntil=published)
        env:
          CENTRAL_TOKEN_USERNAME: ${{ secrets.CENTRAL_TOKEN_USERNAME }}
          CENTRAL_TOKEN_PASSWORD: ${{ secrets.CENTRAL_TOKEN_PASSWORD }}
          MAVEN_GPG_KEY: ${{ secrets.MAVEN_GPG_KEY }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PASSPHRASE }}
      - uses: actions/attest@v4
        with:
          subject-path: |
            hamcrest-matcher-generator-*/target/*.jar
```

Release-profile plugin config (sketch):

```xml
<plugin>
  <groupId>org.sonatype.central</groupId>
  <artifactId>central-publishing-maven-plugin</artifactId>
  <version>0.12.0</version>
  <extensions>true</extensions>
  <configuration>
    <publishingServerId>central</publishingServerId>
    <autoPublish>true</autoPublish>
    <waitUntil>published</waitUntil>
  </configuration>
</plugin>
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-gpg-plugin</artifactId>
  <version>3.2.8</version>
  <configuration>
    <signer>bc</signer>
    <bestPractices>true</bestPractices>
  </configuration>
  <executions><execution><id>sign</id><phase>verify</phase><goals><goal>sign</goal></goals></execution></executions>
</plugin>
```

**[Inference]** `waitUntil=published` can block for a while (Central publishing may take tens of minutes); set a job `timeout-minutes` accordingly, or use `waitUntil=validated` with `autoPublish=true` if job time matters more than knowing the result.

## 8. One-time human steps (unavoidable)

1. Log in at https://central.sonatype.com (same credentials as old OSSRH, or GitHub login), confirm namespace `io.github.marmer` is listed/verified; contact central-support@sonatype.com if not.
2. Generate a Portal user token (https://central.sonatype.com/usertoken), pick an expiration, copy it once.
3. Create a GPG release key locally (no signing subkey), upload the public key to `keys.openpgp.org` (confirm the email-verification mail) and `keyserver.ubuntu.com`, export the armored secret key.
4. In GitHub repo settings create environment `maven-central` with secrets `CENTRAL_TOKEN_USERNAME`, `CENTRAL_TOKEN_PASSWORD`, `MAVEN_GPG_KEY`, `MAVEN_GPG_PASSPHRASE` (optionally: required reviewer = owner if a manual gate is ever wanted; not needed for full automation).
5. Recurring: renew the Portal token and GPG key before they expire.

Everything else (sigstore signing, attestations, SBOM, checksums, upload, publish) needs no secrets beyond these and no manual Portal clicks once `autoPublish=true`.

## Facts other tickets depend on

- OSSRH is dead → current `release` profile is non-functional; a working release requires the build changes above.
- `central-publishing-maven-plugin` 0.12.0 requires **Maven >= 3.9.2** and **does not work with Maven 4** → bump the wrapper from 3.6.3 to 3.9.x and stay on 3.x.
- GPG signing is **mandatory**; Sigstore is optional extra.
- Portal token is long-lived with an expiry chosen at creation; no OIDC trusted publishing documented for Central.
- Server id must match `publishingServerId` (default `central`).
- GitHub attestations: free for public repos, need `id-token: write` + `attestations: write`; give SLSA Build L2.
- The release version must be non-SNAPSHOT (Central requirement); the release flow must set a release version before `deploy`.
