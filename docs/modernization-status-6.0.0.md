# Modernization Status — 6.0.0

Fortschrittsprotokoll zur Umsetzung von [modernization-plan-6.0.0.md](modernization-plan-6.0.0.md).
Dieses Dokument wird bei jedem Arbeitsschritt aktualisiert und ist die Grundlage,
um bei "continue" / "führe fort" an der richtigen Stelle weiterzumachen.

**Branch:** `claude/modernization-plan-6-0-0-1wix4t`

## Gesamtübersicht

| Phase | Beschreibung | Status |
|-------|--------------|--------|
| 1 | Build- & Release-Infrastruktur | ✅ abgeschlossen |
| 2 | Dependency-Modernisierung | ✅ abgeschlossen |
| 3 | Artefakt-Restrukturierung | ✅ abgeschlossen |
| 4 | Mismatch-Messages (Property-Diff) | ✅ abgeschlossen |
| 5 | Features (Referenzobjekt, Exclude, Strict) | ✅ abgeschlossen |
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

Teilschritte (Build nach jedem Schritt grün):

- [x] Hamcrest 2.2 → 3.0
- [x] `javax.annotation-api` entfernt (ungenutzt; Code nutzt JDK-eigenes `javax.annotation.processing.Generated`)
- [x] classgraph 4.8.108 → 4.8.184, compile-testing 0.21.0 → 0.23.0, truth → 1.4.5, guava → 33.6.0-jre, lombok → 1.18.46, JUnit 5.12.1 → 5.14.4 (Platform-Launcher 1.14.4)
- [x] Ungenutzte Dependencies entfernt: junit-extensions, auto-service- und javaparser-Einträge im dependencyManagement
- [x] Mockito 3.11.1 → 5.23.0, mockito-kotlin 3.2.0 → 6.3.0 (org.mockito.kotlin; kaum direkte Nutzung im Code)
- [x] JavaPoet 1.13.0 → `com.palantir.javapoet:javapoet:0.17.0` — reine Paketumbenennung in `MatcherGenerator.kt` (einzige Nutzungsstelle)

Anmerkungen:
- Bewusst NICHT auf JUnit 6.x gegangen (Kompatibilität mockito-junit-jupiter/kotlin-test-junit5); latest 5.x reicht.
- Kotlin bleibt auf 2.1.20 (Plan: KAPT beibehalten, kein Kotlin-Bump gefordert).
- Palantir-Fork: `TypeName.OBJECT` existiert nicht mehr → `WildcardTypeName.subtypeOf(Object::class.java)`; sonst 1:1-Paketumbenennung.

## Phase 3 — Artefakt-Restrukturierung

- [x] `@MatcherConfiguration` in den Annotation-Processor umgezogen — **neues Package:** `io.github.marmer.annotationprocessing.MatcherConfiguration` (vorher `io.github.marmer.testutils.generators.beanmatcher.dependencies`)
- [x] Generierte Matcher self-contained: neue `BeanPropertyMatcherTypeFactory.kt` emittiert die Runtime-Logik als `private static class BeanPropertyMatcher<T>` in jede generierte Top-Level-Matcher-Klasse; Referenz unqualifiziert (`ClassName.get("", …)`), kein Import mehr nötig
- [x] `hamcrest-matcher-generator-dependencies`-Modul entfernt (Verzeichnis, Root-Module-Liste, alle Dependency-Referenzen, CI-Sonar-Projektliste)
- [x] End-to-End-Module angepasst: Kotlin-Module haben AP jetzt als test-Dependency (für die Annotation) und `<proc>none</proc>` für javac (sonst FilerException, weil javac den via Service-Discovery gefundenen Processor nach KAPT nochmal ausführt)
- [x] IT-Erwartungen (24 Blöcke in `MatcherGenerationProcessorWorkerIT.kt`) per Skript umgebaut: Import-Sets vereinigt/sortiert, Kotlin-Konstante `embeddedBeanPropertyMatcher` wird vor der schließenden Klammer jedes erwarteten Outputs interpoliert → Phase 4/5 müssen nur die Konstante + Factory ändern
- [x] README: Single-Dependency-Setup, neues Annotation-Package, build-helper statt antrun-Hack, JDK-17-/Hamcrest-3.0-Anforderungen, 6.0.0-Changelog mit Migrationsguide

Anmerkung: `BeanPropertyMatcherTest.java` entfiel mit dem Modul; Verhaltensabdeckung liegt jetzt bei den E2E-Tests + compile-testing-ITs. Phase 4 ergänzt gezielte Tests fürs Mismatch-Format.

## Phase 4 — Mismatch-Messages

- [x] Multi-line Property-Diff in der emittierten `BeanPropertyMatcher`-Logik: Expectations werden jetzt als (property, valueMatcher, propertyMatcher)-Tripel (`Expectation`-Klasse) gespeichert; `describeMismatchSafely` listet nur fehlschlagende Properties, eine pro Zeile, Format `foo: expected "bar" but was "baz"`. Ist-Wert wird reflektiv über get/is/Record-Accessor gelesen; Fallback auf hasProperty-Mismatch, wenn nicht lesbar. `describeTo` bleibt kompakt (instanceOf + konfigurierte Expectations).
- [x] End-to-End-Tests fürs Ausgabeformat: `MismatchDescriptionTest` (plain-java-minimal) prüft Property-Diff (Werte- und Matcher-Expectations) sowie kompaktes `describeTo`
- [x] compile-testing-ITs decken die generierte Struktur ab (Konstante `embeddedBeanPropertyMatcher` aktualisiert; Import-Set unverändert)
- [x] README-Changelog um das neue Format ergänzt

## Phase 5 — Features

- [x] Referenzobjekt-Matcher: generierte Matcher haben `withAllPropertiesOf(other)` + statisches `is<Typ>EqualTo(other)`; Runtime-Seite `withAllEqualTo(other, props…)` liest Properties reflektiv (get/is/Record-Accessor) und setzt equalTo-Expectations. `class`-Property ist bewusst ausgenommen (instanceOf-Check deckt Typ ab; dokumentiert).
- [x] Exclude-Konfiguration: `@MatcherConfiguration(exclude = …)`-Attribut; Worker filtert Typen nach FQN-Gleichheit, Package-Gleichheit oder Subpackage-Präfix und loggt Note „Matcher generation skipped for excluded type: …".
- [x] Strict Mode: `strict()` am generierten Matcher übergibt die Property-Liste an die Runtime (`strict(props…)`); `matchesSafely` verlangt zusätzlich Expectations für alle Properties; Mismatch listet `foo: unchecked property (strict mode)` zeilenweise. `class` ausgenommen.
- [x] compile-testing-ITs: bestehende 26 Erwartungen per Skript um die 3 neuen Methoden je Matcher-Klasse (33 Klassen) erweitert; neuer IT „Matcher generation should skip excluded types and packages" (27 Tests grün).
- [x] E2E: `NewFeaturesTest` (plain-java-minimal) mit 6 Tests: Referenz-Matcher (match/diff/override), Strict (pass/fail+Meldung), Exclude (Matcher-Klasse existiert nicht).
- [x] README: Abschnitte für alle drei Features + Changelog-Einträge.

### CI-Fixes (aus PR-Checks, nebenbei)

- CodeQL-Workflow: JDK 11 → 21 (Autobuild scheiterte an der Java-17-Baseline).
- Sonar „0% Coverage on New Code": Failsafe-`argLine` im AP-POM überschrieb die JaCoCo-Agent-argLine → ITs liefen ohne Coverage. Fix: `@{argLine}`-Präfix (late binding) + leere Default-Property `argLine` im Root-POM.

## Phase 6 — Release 6.0.0

- [ ] Migrationsguide finalisieren (siehe Plan)
- [ ] Release über neue Central-Portal-Pipeline
- [ ] Danach separat: 6.1.0 Kotlin-DSL (eigene Design-Runde)

## Verlauf

- **2026-07-09** — Statusdokument angelegt; Phase 1 begonnen (Baseline-Build gestartet, Root-POM & CI analysiert).
- **2026-07-09** — Phase 5 abgeschlossen: Referenzobjekt-Matcher, Exclude-Konfiguration, Strict Mode inkl. IT-/E2E-Tests und README-Doku. CI-Fixes: CodeQL auf JDK 21, JaCoCo-Coverage für Failsafe-ITs repariert (Sonar-Quality-Gate-Ursache).
- **2026-07-09** — Phase 4 abgeschlossen: Multi-line Property-Diff im Mismatch (nur fehlschlagende Properties, `foo: expected "bar" but was "baz"`), E2E-Formattests, README-Changelog. Voller Build grün.
- **2026-07-09** — Phase 3 abgeschlossen: Single-Artifact-Struktur (`dependencies`-Modul entfernt), `@MatcherConfiguration` in `io.github.marmer.annotationprocessing`, generierte Matcher self-contained (nested `BeanPropertyMatcher`), README + Migrationsguide aktualisiert. Voller Build grün.
- **2026-07-09** — Phase 2 abgeschlossen: alle Dependencies modernisiert (Hamcrest 3.0, Mockito 5.23, Palantir JavaPoet 0.17, JUnit 5.14.4, …), Ungenutztes entfernt. Build nach jedem Teilschritt grün. Draft-PR #50 erstellt.
- **2026-07-09** — Phase 1 abgeschlossen: JCenter raus, Central-Portal-Publishing, alle Maven-Plugins + Wrapper aktuell, Java-17-Baseline, Records-Modul im Default-Set, Version 6.0.0-SNAPSHOT, CI-Matrix 17/21/25, KAPT-antrun-Hack durch build-helper ersetzt. Voller Build grün.
