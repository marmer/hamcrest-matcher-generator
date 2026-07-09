# Modernization Status — 6.0.0

Fortschrittsprotokoll zur Umsetzung von [modernization-plan-6.0.0.md](modernization-plan-6.0.0.md).
Dieses Dokument wird bei jedem Arbeitsschritt aktualisiert und ist die Grundlage,
um bei "continue" / "führe fort" an der richtigen Stelle weiterzumachen.

**Branch:** `claude/modernization-plan-6-0-0-1wix4t`

## Gesamtübersicht

| Phase | Beschreibung | Status |
|-------|--------------|--------|
| 1 | Build- & Release-Infrastruktur | ✅ abgeschlossen |
| 2 | Dependency-Modernisierung | ⬜ offen |
| 3 | Artefakt-Restrukturierung | ⬜ offen |
| 4 | Mismatch-Messages (Property-Diff) | ⬜ offen |
| 5 | Features (Referenzobjekt, Exclude, Strict) | ⬜ offen |
| 6 | Release 6.0.0 | ⬜ offen |

Legende: ⬜ offen · 🔄 in Arbeit · ✅ abgeschlossen · ⚠️ blockiert/Anmerkung

## Phase 1 — Build- & Release-Infrastruktur

| Teilschritt | Status | Details |
|-------------|--------|---------|
| Baseline-Build verifizieren | ✅ | `./mvnw clean install` auf unverändertem Stand: grün (JDK 21 lokal) |
| JCenter aus `<repositories>`/`<pluginRepositories>` entfernen | ✅ | Beide Blöcke komplett entfernt (mavenCentral ist Default, Redeklaration unnötig) |
| OSSRH → Central Portal | ✅ | `central-publishing-maven-plugin` 0.11.0 (publishingServerId `central`, autoPublish, waitUntil published); `distributionManagement` entfernt; `server-settings.xml` nutzt jetzt `CENTRAL_TOKEN_USERNAME`/`CENTRAL_TOKEN_PASSWORD` — **Release-Secrets müssen als Central-Portal-Token neu hinterlegt werden!** |
| Maven-Plugins aktualisieren | ✅ | compiler 3.15.0, deploy 3.1.4, surefire/failsafe 3.5.6, gpg 3.2.8, source 3.4.0, javadoc 3.12.0, dokka 2.2.0, jacoco 0.8.15, sonar 5.7.0.6970; Maven-Wrapper 3.6.3 → 3.9.16 |
| Java-Baseline auf 17 | ✅ | `kotlin.compiler.jvmTarget` 11 → 17 (steuert auch javac `--release`) |
| Records-Modul in Default-Modulset | ✅ | `java16plus`-Profil entfernt, Modul im `dev`-Profil; `<release>16</release>`-Override entfernt |
| Version auf 6.0.0-SNAPSHOT | ✅ | Root-POM + alle Modul-POMs (Parent- und Dependency-Referenzen) |
| CI anpassen | ✅ | verify-matrix: 11/17/25 → 17/21/25; `ciManagement` auf GitHub Actions umgestellt; totes `ci.sh` gelöscht |
| Release-Flow prüfen | ⚠️ | Kein Release-Workflow in `.github/workflows` vorhanden — Release lief bisher offenbar manuell (`-P release` + `server-settings.xml`). Neuer Befehl: `./mvnw clean deploy -P release -s server-settings.xml`. GitHub-Actions-Release-Workflow wird in Phase 6 ergänzt. |
| Build grün nach allen Änderungen | ✅ | `./mvnw clean install` grün (alle 7 Module, JDK 21 lokal, Maven 3.9.16); Release-Profil bis `package` verifiziert (dokka-javadoc-, sources- und Haupt-JARs entstehen) |

### Notizen Phase 1

- **KAPT-Fix nötig:** maven-compiler-plugin ≥3.13 registriert `target/generated-test-sources/test-annotations`
  nicht mehr automatisch als Test-Source-Root (Annotation Processing implizit deaktiviert).
  Der alte antrun-Move-Hack in beiden Kotlin-E2E-Modulen brach dadurch (`cannot find symbol …Matcher`).
  Fix wie in CLAUDE.md beschrieben: antrun entfernt, stattdessen `build-helper-maven-plugin:add-test-source`
  registriert `target/generated-sources/kapt/test` (Phase `generate-test-sources`), javac kompiliert die
  generierten Matcher direkt von dort.
- **Dokka 2.x:** Das `javadoc`-Goal braucht in Dokka 2.2.0 das `org.jetbrains.dokka:javadoc-plugin`
  explizit als `<dokkaPlugins>`-Eintrag (in pluginManagement konfiguriert). Außerdem funktionieren
  die Dokka-Goals nur, wenn das Plugin in `build/plugins` deklariert ist (wie im Release-Profil) —
  reine Ad-hoc-CLI-Aufrufe schlagen mit „Not found dokka plugin" fehl.
- **Für den nächsten Release nötig (manuell, außerhalb des Repos):** Central-Portal-Account/Namespace
  `io.github.marmer.testutils` verifizieren und Token als `CENTRAL_TOKEN_USERNAME`/`CENTRAL_TOKEN_PASSWORD`
  bereitstellen; alte OSSRH-Credentials sind obsolet.

- `ciManagement` im Root-POM verweist noch auf Travis CI (tot) — wird mitbereinigt.
- `ci.sh` referenziert ein nicht mehr existierendes Modul `hamcrest-matcher-generator-maven-plugin` — totes Skript.
- Aktuelle CI-Matrix testet JDK 11/17/25; Hauptbuild auf JDK 21.

## Phase 2 — Dependency-Modernisierung

Geplante Teilschritte (Build muss nach jedem Schritt grün bleiben):

- [ ] JavaPoet 1.13.0 → `com.palantir.javapoet` (Palantir-Fork)
- [ ] `javax.annotation-api` entfernen (ungenutzt)
- [ ] Hamcrest 2.2 → 3.0
- [ ] Mockito 3.11.1 → 5.x, mockito-kotlin 3.2.0 → aktuell
- [ ] compile-testing → aktuell, classgraph 4.8.108 → aktuell
- [ ] Restliche Test-Dependencies prüfen (junit-extensions, truth, guava, javaparser)

## Phase 3 — Artefakt-Restrukturierung

- [ ] `@MatcherConfiguration` in den Annotation-Processor umziehen
- [ ] Generierte Matcher self-contained machen (BeanPropertyMatcher als nested static class emittieren)
- [ ] `hamcrest-matcher-generator-dependencies`-Modul entfernen
- [ ] End-to-End-Module anpassen
- [ ] README-Setup-Anleitung anpassen

## Phase 4 — Mismatch-Messages

- [ ] Multi-line Property-Diff in der generierten Matcher-Logik
- [ ] Unit-Tests für das Ausgabeformat
- [ ] End-to-End-Tests für das Ausgabeformat

## Phase 5 — Features

- [ ] Referenzobjekt-Matcher (`isSomePojoEqualTo(expected)` / `withAllPropertiesOf`)
- [ ] Exclude-Konfiguration (`@MatcherConfiguration(exclude = …)`)
- [ ] Strict Mode (`.strict()`)
- [ ] Jeweils compile-testing-ITs + End-to-End-Abdeckung + README-Doku

## Phase 6 — Release 6.0.0

- [ ] Migrationsguide finalisieren (siehe Plan)
- [ ] Release über neue Central-Portal-Pipeline
- [ ] Danach separat: 6.1.0 Kotlin-DSL (eigene Design-Runde)

## Verlauf

- **2026-07-09** — Statusdokument angelegt; Phase 1 begonnen (Baseline-Build gestartet, Root-POM & CI analysiert).
- **2026-07-09** — Phase 1 abgeschlossen: JCenter raus, Central-Portal-Publishing, alle Maven-Plugins + Wrapper aktuell, Java-17-Baseline, Records-Modul im Default-Set, Version 6.0.0-SNAPSHOT, CI-Matrix 17/21/25, KAPT-antrun-Hack durch build-helper ersetzt. Voller Build grün.
