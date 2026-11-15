# Wer und wie viele nutzen den hamcrest-matcher-generator?

Stand: 2026-11-15 (alle Abrufe an diesem Tag). Recherche ohne Aenderung am Repo ausser dieser Datei.

Kennzeichnung: **(gemessen)** direkt von der Primaerquelle abgerufen, **(geschaetzt)** abgeleitet, **(nicht ermittelbar)** oeffentlich nicht abrufbar. Es sind keine Zahlen erfunden; wo eine Quelle nichts liefert, steht das so da.

## Kurzantwort

1. Oeffentlich belegbare Nutzung ist **sehr klein**: genau ein externes Repo, das ein veroeffentlichtes Artefakt per pom einbindet (gemessen, Abschnitt 3). Echte Download-Zahlen sind **nicht ermittelbar**.
2. GitHub: 8 Sterne, 3 Forks, 1 Subscriber (gemessen). Es gibt keine GitHub-Releases, also auch keine Release-Downloads.
3. Maven Central: 29 Versionen von `hamcrest-matcher-generator` (2017-07-12 bis 2023-08-28); **seit 2023-08-28 keine neue Version** auf Central, obwohl das Repo aktiv ist (Commit heute, 2026-11-14/15).
4. Die Abhaengigen-Zaehler der Indexdienste liegen bei 0 bis 2 und enthalten teils das Repo selbst; sie decken nur oeffentliche, geparste Repos ab und sind eine Untergrenze, keine Nutzerzahl.

## 1. Maven Central

### Koordinaten (aus den poms im Repo, `pom.xml` und Modul-poms)

Gruppe: `io.github.marmer.testutils`. Parent-pom im Repo: `matcher-generator-parent` 5.1.0 (steht **nicht** auf Central, siehe unten).

Quelle fuer alles in diesem Abschnitt (gemessen): `https://repo1.maven.org/maven2/io/github/marmer/testutils/` (Verzeichnislisting und `maven-metadata.xml` je Artefakt), Abruf 2026-11-15.

Auf Central tatsaechlich vorhandene Artefakte der Gruppe:

| Artefakt | Versionen | Erste / letzte | `lastUpdated` laut Metadaten |
|---|---|---|---|
| `hamcrest-matcher-generator` | 29 (1.0.0 bis 5.1.0) | 2017-07-12 / 2023-08-28 | 2023-08-28 |
| `hamcrest-matcher-generator-dependencies` | 29 (1.0.0 bis 5.1.0) | wie oben | 2023-08-28 |
| `hamcrest-matcher-generator-annotationprocessor` | 17 (4.0.0 bis 5.1.0) | 2019-03-11 / 2023-08-28 | 2023-08-28 |
| `hamcrest-matcher-generator-core` (alt) | 13 (1.0.0 bis 3.2.0) | 2017-07-12 / 2018-11-11 | 2018-11-11 |
| `hamcrest-matcher-generator-maven-plugin` (alt) | 13 (1.0.0 bis 3.2.0) | 2017-07-12 / 2018-11-11 | 2018-11-11 |
| `hamcrest-matcher-generator-endtoend-plain` (alt) | 12 (4.0.0 bis 4.2.3, ohne 4.2.2) | 2019-03 / 2020-10-10 | 2020-10-10 |
| `JPojoAssert`, `JPojoAssert-core`, `JPojoAssert-processor` (Vorgaenger-/Schwesterprojekt, nur der Vollstaendigkeit halber) | 0.1.0 bis 0.7.0 | n/a / 2020-12-05 | 2020-12-05 |

Nicht auf Central (obwohl im Repo vorhanden): `matcher-generator-parent`, `matcher-generator-core`, `matcher-generator-annotations`, `hamcrest-matcher-generator-kotlin`, `assertj-assertion-generator` sowie die Example- und End-to-End-Module (gemessen: `maven-metadata.xml` liefert leer; Listing zeigt sie nicht). Das sind die Artefakte der geplanten/laufenden 6.x-Linie im Repo; sie sind noch nicht veroeffentlicht.

Die Verzeichnisse `core` und `maven-plugin` stammen aus der Zeit vor 4.0.0 (Maven-Plugin-Variante). Ab 4.0.0 gibt es `annotationprocessor` (Quelle: Listings oben; Changelog `CHANGELOG.md` im Repo).

### Alle Versionen von `hamcrest-matcher-generator` mit Datum (gemessen)

Quelle: `https://repo1.maven.org/maven2/io/github/marmer/testutils/hamcrest-matcher-generator/` (Listing, UTC-Zeit des Servers); gegengeprueft gegen deps.dev (`publishedAt`, z. B. 1.0.0 = 2017-07-12T10:00:18Z, 5.1.0 = 2023-08-28T09:41:04Z) via `https://api.deps.dev/v3/systems/maven/packages/io.github.marmer.testutils:hamcrest-matcher-generator`.

| Jahr | Versionen (Datum) |
|---|---|
| 2017 | 1.0.0 (07-12), 1.1.0 (07-18), 1.2.0 (07-19), 1.2.1 (07-20), 1.2.2 (07-20) |
| 2018 | 1.3.0 (11-03), 2.0.0 (11-05), 3.0.0 (11-06), 3.0.1 (11-07), 3.1.0 (11-08), 3.1.1 (11-09), 3.1.2 (11-10), 3.2.0 (11-11) |
| 2019 | 4.0.0 (03-11), 4.0.1 (03-12), 4.0.2 (03-16), 4.0.3 (03-21), 4.0.4 (03-22), 4.0.5 (03-23), 4.0.6 (04-05), 4.0.7 (04-19), 4.1.0 (04-27), 4.2.0 (05-02), 4.2.1 (07-18), 4.2.2 (10-17) |
| 2020 | 4.2.3 (10-10), 4.2.4 (11-19) |
| 2021 | 4.2.5 (03-17), 5.0.0 (06-25) |
| 2023 | 5.1.0 (08-28) |

Die Daten decken sich mit den Eintraegen in `CHANGELOG.md` (5.1.0: 2023-08-28, 5.0.0: 2021-06-25, 4.2.5: 2021-03-17, 4.2.4: 2020-11-18; der Changelog nennt 4.2.4 einen Tag frueher als das Central-Listing, vermutlich Zeitzone).

### Download-Zahlen

**Nicht ermittelbar.** Sonatype Central zeigt Download-Statistiken nur Namespace-Eigentuemern. Abgerufen: `https://central.sonatype.com/artifact/io.github.marmer.testutils/hamcrest-matcher-generator` zeigt weder Downloads noch "used by"-Zahlen (nur ein Label "Used in: components" ohne Zahl). Kein oeffentlicher Endpunkt liefert Downloads: ecosyste.ms-Paket-API gibt `downloads: null` zurueck (`https://packages.ecosyste.ms/api/v1/registries/repo1.maven.org/packages/io.github.marmer.testutils:hamcrest-matcher-generator`).

## 2. GitHub (REST, `gh api repos/marmer/hamcrest-matcher-generator`)

| Messwert | Wert | Art |
|---|---|---|
| Stars | 8 | gemessen |
| Forks | 3 (`Zhigool`, `JulienDeBerlin`, `kevinshale`; alle ohne Sterne, letzte Pushes 2023-08-27, 2022-05-27, 2021-02-04) | gemessen |
| Watchers / Subscribers | `watchers_count` 8 (ist bei GitHub identisch mit Stars), `subscribers_count` 1 | gemessen |
| Erstellt / letzter Push | 2017-06-12 / 2026-11-15T00:16:52Z | gemessen |
| Archiviert | nein | gemessen |
| Lizenz, Sprache | Apache-2.0, Java | gemessen |
| Offene Issues (inkl. PRs) | 30 (deps.dev meldet 11, ecosyste.ms 37; Stichtage und Zaehlweise unterschiedlich) | gemessen, uneinheitlich |
| Contributors | 7 Konten laut `contributors`-API: `marmer` (910 Commits), `renovate[bot]` (24), `claude` (17), je 1 Commit `JulienDeBerlin`, `kevinshale66`, `dependabot[bot]`, `github-agentic-autofix[bot]`. Menschliche Fremd-Contributors: 2, je 1 Commit. | gemessen |
| GitHub-Releases | 0 (Liste leer), daher **keine Release-Downloads**; 32 Git-Tags | gemessen |
| Traffic (Views, Clones, Referrer) | HTTP 403 "Resource not accessible by integration" ohne Push-Rechte | nicht ermittelbar |

Quellen: `https://api.github.com/repos/marmer/hamcrest-matcher-generator` sowie `/contributors`, `/releases`, `/tags`, `/forks`, `/traffic/views`; zur Gegenpruefung deps.dev `https://api.deps.dev/v3/projects/github.com%2Fmarmer%2Fhamcrest-matcher-generator` (8 Sterne, 3 Forks) und libraries.io `https://libraries.io/maven/io.github.marmer.testutils:hamcrest-matcher-generator` (zeigt 7 Sterne, 3 Forks, 4 Contributors, 29 Releases, SourceRank 10; aeltere Momentaufnahme, wie ueblich bei libraries.io).

### Dependents-Seite

`https://github.com/marmer/hamcrest-matcher-generator/network/dependents` (WebFetch): 1 abhaengiges Repo, 0 Pakete; das gelistete Repo ist `marmer/hamcrest-matcher-generator` selbst (gemessen, die Seite weist selbst auf Ungenauigkeit hin). Faktisch also **0 externe Dependents laut GitHub**.

### Code-Suche nach Nutzung (REST `search/code`, nur Fundstellen-Namen)

Suche nach `io.github.marmer.testutils` und `hamcrest-matcher-generator` ausserhalb `user:marmer`:

- `szachmati/product-api` (`spring-boot/`, `quarkus/`, `helidon/` je `pom.xml`): bindet `io.github.marmer.testutils:hamcrest-matcher-generator-annotationprocessor` ein, `hamcrest-matcher-generator.version` = 4.2.5 (im spring-boot-pom). Vergleichsprojekt dreier Frameworks. Das ist die einzige belegte echte externe Nutzung eines veroeffentlichten Artefakts (gemessen, aber Code-Suche ist unvollstaendig: GitHub indexiert nicht alles).
- `mintu2211/Restassured1`: laut ecosyste.ms Dependent von `...-annotationprocessor` (Repo-Stand 2023-08-15, 0 Sterne). Eigene pom nicht eingesehen (nur Metadaten; Zugriff auf das Repo ist in dieser Session nicht freigegeben). Daher: laut Indexdienst, nicht selbst verifiziert.
- `kinhoangnu/TCI`: laut ecosyste.ms Dependent von `...-core` (alt, 3.2.0), letzter Push 2018-12-04, 0 Sterne.
- `patrickf314/devx-project`: Treffer im Modul `hamcrest/hamcrest-matcher-generator`, aber mit eigener Gruppe `de.devx.project`; das ist ein gleichnamiges Eigen-Modul, **keine** Nutzung dieses Projekts (zumindest nicht belegt; Repo nicht eingesehen).
- Reine Erwaehnungen ohne Nutzung: Forschungsdaten zu Flaky-Tests (`UT-SE-Research/iDFlakies` und Kopien: CSV, die `marmer/hamcrest-matcher-generator` als eines von vielen untersuchten Projekten auflistet, Stand 2018-12) und Sitemap-Kopien von search.maven.org (`veerakrishna222/search-maven-org`, `ArjunDandagi/search-maven-org`). Das sind keine Nutzer.
- Suche in `build.gradle`: 0 externe Treffer. Die Treffer unter `marmer/JPojoAssert` gehoeren dem Eigentuemer.

Einschraenkung: Die Code-Suche zeigt nur oeffentliche, indexierte Dateien; private Repos und Firmenprojekte sind unsichtbar.

## 3. Indexdienste zu Abhaengigen (Maven-Pakete)

| Quelle | Ergebnis | Art |
|---|---|---|
| deps.dev `...:dependents` je Version (`https://api.deps.dev/v3alpha/systems/maven/packages/io.github.marmer.testutils:<artefakt>/versions/<v>:dependents`) | `hamcrest-matcher-generator`: 0 (geprueft 1.0.0, 3.2.0, 4.0.0, 4.2.2, 4.2.4, 5.0.0, 5.1.0). `-dependencies`: 5.1.0 = 0, 4.2.5 = 1, 4.2.3 = 1, 3.2.0 = 2 (1 direkt, 1 indirekt). `-annotationprocessor`: 0 (4.2.3, 4.2.5, 5.1.0). `-core` 3.2.0: 1. `-maven-plugin` 3.2.0: 0. `-endtoend-plain` 4.2.3: 0. | gemessen |
| ecosyste.ms Usage (`https://repos.ecosyste.ms/api/v1/usage/maven/io.github.marmer.testutils:<artefakt>`) | `-annotationprocessor`: 2 Dependent-Repos (das Repo selbst und `mintu2211/Restassured1`); `-dependencies`: 1 (das Repo selbst); `-core`: 1 (`kinhoangnu/TCI`); Hauptartefakt: "not found". Die `-dependencies`-Dependents bei deps.dev sind plausibel intern (der Annotationprocessor haengt laut seiner 4.2.5-pom an `-dependencies`), das ist aus der pom abgeleitet, nicht einzeln aufgeloest. | gemessen / teils geschaetzt |
| libraries.io | Dependents 0 (Pakete), 0 (Repos) | gemessen |
| mvnrepository.com ("Used By"-Zaehler) | HTTP 403 gegen WebFetch, **nicht ermittelbar**. Die Websuche zeigt nur, dass die Gruppenseite existiert und mehrere Module "last release Aug 28, 2023" zeigen; keine Zahl lesbar. | nicht ermittelbar |
| Sonatype OSS Index / Central | keine Nutzungszahlen | nicht ermittelbar |

Deps.dev erfasst Abhaengige nur aus veroeffentlichten Maven-Paketen, nicht aus Anwendungen oder reinen Test-Scopes in nicht veroeffentlichten Projekten. Ein Annotationprocessor fuer Tests wird typischerweise in Anwendungen genutzt, nicht in veroeffentlichten Bibliotheken; Null bei deps.dev ist deshalb erwartbar und **kein Beleg fuer Nichtnutzung**.

## 4. Weitere Spuren

- Blogposts, Stack-Overflow-Erwaehnungen: Die zwei Websuchen (`"hamcrest-matcher-generator" marmer`, `"io.github.marmer.testutils" hamcrest-matcher-generator`) lieferten nur Repo, Central, mvnrepository-Gruppenseite, OSS Index, ecosyste.ms und Spiegelseiten; keinen Blogpost und keinen Stack-Overflow-Beitrag. Das ist ein Suchergebnis, kein Beweis der Abwesenheit (Suchmaschine beschraenkt auf US, zwei Anfragen).
- Der Name ist mit anderen Projekten verwechselbar (`yandex-qatools/hamcrest-pojo-matcher-generator`, `org.hamcrest:hamcrest-generator`); deren Zahlen gehoeren nicht hierher.

## 5. Einordnung

- **Gemessen:** 8 Sterne, 3 Forks, 29 Central-Versionen ueber 6 Jahre, 1 verifizierte externe Nutzung (`szachmati/product-api`), 0 externe GitHub-Dependents, 0 Dependents des Hauptartefakts bei deps.dev und libraries.io.
- **Geschaetzt:** Die Nutzerbasis ist nach diesen Spuren einstellig bis niedrig zweistellig an oeffentlich sichtbaren Projekten; die Gesamtzahl inklusive privater Projekte laesst sich aus diesen Quellen nicht schaetzen. Die Aussage ist eine Einordnung, keine Zahl.
- **Nicht ermittelbar:** Download-Zahlen (Central, mvnrepository), GitHub-Traffic, private Nutzer, Used-By-Zaehler von mvnrepository.
- Wie man an echte Zahlen kommt: Der Namespace-Eigentuemer sieht Download-Statistiken in der Sonatype-Central-Oberflaeche (Account noetig); GitHub-Traffic zeigt `Insights > Traffic` mit Push-Rechten (nur 14 Tage Verlauf).

## Methodik und Grenzen

- Abgerufen per `curl`, `gh api` (REST) und WebFetch/WebSearch am 2026-11-15. Es wurde kein Fremd-Repo ueber GitHub-MCP gelesen; der `gh`-Zugriff auf Fremd-Repos (`szachmati`, `patrickf314`) wurde fuer Dateiinhalte vom Proxy abgelehnt, deshalb stuetzen sich diese Befunde auf die Code-Suche-Fragmente.
- Zugriffe auf `https://repo1.maven.org/maven2/io/github/marmer/` liefen einmal in HTTP 429 (Rate Limit) und wurden wiederholt.
- Keine personenbezogenen Daten ausser oeffentlichen GitHub-Konto- und Repo-Namen; keine E-Mail-Adressen.
