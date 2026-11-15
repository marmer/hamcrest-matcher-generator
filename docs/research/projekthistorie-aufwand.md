# Projekthistorie, Umfang und Aufwand der 6.0.0-Arbeit

Stand: 2026-11-15 (alle Abrufe an diesem Tag). Recherche ohne Aenderung am Repo ausser dieser Datei. Nicht committet.

Zweck: Grundlage fuer das LinkedIn-Grilling-Ticket #503. Der Owner will die Aussage "Bibliothek, der die letzte Ehre erwiesen wird: Qualitaet, Feature-Reichtum, Absicherung, fuer die in der Freizeit nie Zeit war, jetzt mit KI" pruefen und Aufwandsangaben stuetzen.

Kennzeichnung der Belegklassen (strikt getrennt):

- **(gemessen)** direkt aus einer Primaerquelle gezaehlt oder gelesen (git, Repo-Dateien, GitHub-REST). Der Befehl oder die Issue-/PR-Nummer steht dabei.
- **(abgeleitet)** aus gemessenen Zahlen berechnet, mit genannter Rechnung. Die Zahl ist exakt, ihre Deutung nicht.
- **(Schaetzung)** Annahme von mir, nicht gemessen. Immer mit offenen Annahmen.
- **(nicht ermittelbar)** aus den zugaenglichen Quellen nicht zu belegen. Nichts geraten.

Messbasis: Die lokale Branch (`ccr-bcc81c73-3tvsvy`, HEAD `f9fb25b4`) liegt hinter `origin/main`. Alle Zahlen unten beziehen sich auf `origin/main` = `391f79ca` (2026-11-15 12:44:48 +0100, "docs(readme): one Maven Central badge (#507)"), sofern nicht anders genannt. Das Repo war vollstaendig (`git rev-parse --is-shallow-repository` = false), `git rev-list --count origin/main` = 1033 Commits. Der Tag `6.0.0` (`19ec8184`, Release-PR #210) liegt 3 Commits vor `origin/main`. Der Unterschied ist fuer alle Zahlen unten vernachlaessigbar (Beispiel: `git diff --shortstat 5.1.0 6.0.0` = 62817 Einfuegungen, gegen 62791 auf `origin/main`).

## Kurzantwort

1. **Zeit (gemessen):** Das Repo hat seit 2017-06-12 Commits, aber zwischen dem Release 5.1.0 (2023-08-28) und dem 2026-04-27 gab es **973 Tage ohne einen Commit**. Die eigentliche 6.0.0-Arbeit lief von der Wayfinder-Map #52 (2026-10-22T21:51:54Z) bis zum Tag `6.0.0` (2026-11-15T10:46:57Z): **23 Tage, 12 Stunden, 55 Minuten Wallclock**. An 21 von 25 Kalendertagen (2026-10-22 bis 2026-11-15) gab es Commits.
2. **Umfang (gemessen):** `git diff --shortstat 5.1.0 origin/main` = 776 Dateien, +62791 / -5351 Zeilen. Der Stand 5.1.0 hatte 64 Dateien und 7120 Zeilen, `origin/main` hat 769 Dateien und 64560 Zeilen (Groessenverhaeltnis der Staende, keine Aussage ueber Aufwand). Davon 7360 Zeilen ausgelieferter Produktionscode, 28303 Zeilen Testcode, 7228 Zeilen Dokumentation, 14821 Zeilen Agenten-Werkzeug und Skripte (Abschnitt 2.2). 24 Karten in der Map #52, 100 Tickets im Spec #104, insgesamt 204 Issues seit 2026-04, 242 gemergte PRs seit 2026-10-22. 10 Reactor-Module in der Root-pom plus 8 Module im Profil `dev` (7 End-to-End-Module plus Aggregator) und das `docs`-Modul.
3. **Beitraege (gemessen, korrigierte Zaehlung):** Die Zahl "910 `marmer` / 17 `claude`" aus `docs/research/nutzung-und-verbreitung.md` **unterschlaegt die Trailer-Commits**. Sie stammt aus der Contributors-API, die nach Autor-Konto zaehlt; der Autor der Agenten-PRs ist das Owner-Konto (Squash-Merge ueber den Owner), die KI steht nur im Trailer. Im 6.0.0-Bereich `5.1.0..origin/main` (275 Commits) tragen **228 Commits (83 %) einen `Co-Authored-By: Claude`- oder `Claude-Session:`-Trailer**; nur 17 haben Claude als Autor. Nimmt man die 27 Bot-Commits (Renovate, Dependabot, Release-Bot, Autofix) heraus, sind es 227 von 248 (91,5 %; der Autofix-Bot-Commit traegt auch einen Trailer). Die Zahl sagt, wer in den Commits genannt ist, nicht, wer wie viele Zeilen geschrieben hat (Abschnitt 3.3).
4. **Motivation (gemessen, woertlich in Abschnitt 4):** In #52 steht: `"last honours": feature-complete, then automatically maintained with minimal owner effort ...` und `portfolio project`; `AI-assisted development may stay visible as a side effect.` Eine Aussage zur Freizeit oder Zeitnot steht nirgends im Repo oder in den Issues (Suche in Issue-Texten und Repo: 0 Treffer fuer "Freizeit", "hobby"). Dass KI das Thema ist, steht dort ausdruecklich nur als Nebeneffekt.
5. **Aufwand (Schaetzung, nicht gemessen):** Ein Mensch allein in der Freizeit braeuchte fuer denselben Umfang nach meinen Annahmen **grob 400 bis 7000 Arbeitsstunden**, je nach Methode und Tempo; die Mitte der Zeilenmethode liegt bei etwa 3500 Stunden, die Mitte der Ticketmethode bei etwa 740 Stunden. Bei 5 bis 15 Freizeitstunden pro Woche waeren das von einem halben Jahr bis zu vielen Jahren. Die Spanne ist so breit, weil beide Methoden auf unbelegten Tempo-Annahmen beruhen (Abschnitt 5). **Gemessen** ist nur die Wallclock-Dauer der 6.0.0-Arbeit mit KI (23,5 Tage); der Aufwand des Owners in dieser Zeit ist nicht ermittelbar.
6. **Zur Aussage:** Gedeckt sind "letzte Ehre" (als "last honours", mit Autopilot-Ziel), Qualitaets- und Absicherungs-Gates (Konfiguration belegt, Abschnitt 4.3) und der Feature-Zuwachs (Abschnitt 2.4). **Nicht belegt** ist "fuer die in der Freizeit nie Zeit war" (nur die 973-Tage-Luecke und ein Zeitmuster der Alt-Commits sind konsistent damit, beweisen es nicht). "Jetzt mit KI" ist gedeckt, aber die Map nennt KI als Nebeneffekt, nicht als Ziel (Abschnitt 6).

## 1. Zeitverlauf

### 1.1 Erster Commit, Tags, Releases

| Messwert | Wert | Quelle |
|---|---|---|
| Erster Commit | 2017-06-12 19:24:23 +0200, `6fc79b45` "Initial commit" | `git log origin/main --reverse --format='%ad %h %s' --date=iso \| head -1` (gemessen) |
| Letzter Commit auf `origin/main` | 2026-11-15 12:44:48 +0100, `391f79ca` | `git log -1 origin/main` (gemessen) |
| Commits gesamt | 1033 | `git rev-list --count origin/main` (gemessen) |
| Tage mit mindestens einem Commit | 131 | `git log origin/main --format=%ad --date=short \| sort -u \| wc -l` (gemessen) |
| Git-Tags | 26 Versions-Tags (1.0.0 bis 5.1.0, 6.0.0) und 6 Tags `archive/prototype-*` | `git tag` (gemessen) |
| GitHub-Releases | 1 (`6.0.0`, veroeffentlicht 2026-11-15T10:47:12Z) | `gh api repos/marmer/hamcrest-matcher-generator/releases` (gemessen); die aeltere Recherche nannte 0, das ist ueberholt |
| Maven Central | `hamcrest-matcher-generator` `latest`/`release` = 6.0.0, `lastUpdated` 20261115111802 | `curl https://repo1.maven.org/maven2/io/github/marmer/testutils/hamcrest-matcher-generator/maven-metadata.xml` (gemessen). Die Aussage in `nutzung-und-verbreitung.md`, seit 2023-08-28 gebe es keine neue Version und 6.x sei nicht veroeffentlicht, ist damit ueberholt |

Tag-Daten (`git for-each-ref refs/tags`): 1.0.0 2017-07-12; 1.2.2 2017-07-20; 2.0.0 2018-11-05; 3.2.0 2018-11-11; 4.0.0 2019-03-12; 4.2.2 2019-10-17; 4.2.4 2020-11-18; 5.1.0 2023-08-28; 6.0.0 2026-11-15. Die Versionen 1.2.0, 1.3.0, 3.1.2, 4.2.5 und 5.0.0 stehen auf Central, haben aber keinen Git-Tag (siehe `nutzung-und-verbreitung.md` fuer das Central-Listing).

### 1.2 Commits pro Jahr (gemessen)

Befehl: `git log origin/main --format=%ad --date=format:%Y | sort | uniq -c`. Das Datum ist das Autor-Datum mit der im Commit gespeicherten Zeitzone.

| Jahr | Commits |
|---|---|
| 2017 | 356 |
| 2018 | 93 |
| 2019 | 214 |
| 2020 | 7 |
| 2021 | 83 |
| 2022 | 2 |
| 2023 | 3 |
| 2024 | 0 |
| 2025 | 0 |
| 2026 | 275 |

### 1.3 Commits pro Monat (gemessen)

Befehl: `git log origin/main --format=%ad --date=format:%Y-%m | sort | uniq -c`. Nur Monate mit Commits:

2017-06: 231; 2017-07: 123; 2017-10: 2; 2018-10: 5; 2018-11: 88; 2019-01: 41; 2019-02: 69; 2019-03: 69; 2019-04: 24; 2019-05: 3; 2019-07: 4; 2019-10: 4; 2020-04: 1; 2020-10: 4; 2020-11: 2; 2021-02: 2; 2021-03: 2; 2021-06: 79; 2022-05: 2; 2023-08: 3; **2026-04: 18; 2026-07: 2; 2026-10: 144; 2026-11: 111** (bis 2026-11-15).

### 1.4 Pausen der Aktivitaet (abgeleitet aus den Commit-Tagen)

Luecken von mindestens 60 Tagen zwischen zwei Commit-Tagen (Skript ueber `git log --format=%ad --date=short`):

| Von | Bis | Tage ohne Commit-Tag |
|---|---|---|
| 2017-10-01 | 2018-10-26 | 390 |
| 2019-10-17 | 2020-04-01 | 167 |
| 2020-04-01 | 2020-10-10 | 192 |
| 2021-06-25 | 2022-05-27 | 336 |
| 2022-05-31 | 2023-08-27 | 453 |
| **2023-08-28** (5.1.0) | **2026-04-27** | **973** |
| 2026-04-30 | 2026-07-03 | 64 |
| 2026-07-04 | 2026-10-22 | 110 |

Weitere Luecken zwischen 60 und 91 Tagen sind in der Skriptausgabe (2017-07 bis 2017-10, 2018-11 bis 2019-01, 2019-05 bis 2019-10, 2020-11 bis 2021-02, 2021-03 bis 2021-06).

Einordnung: Die 973-Tage-Pause nach 5.1.0 ist die laengste. In ihr gibt es nur den Merge eines externen PRs und den Release am 2023-08-27/28 davor. Dass die Pause an Zeitmangel lag, ist aus den Daten **nicht ableitbar**; sie zeigt nur Inaktivitaet.

### 1.5 Zeitraum der 6.0.0-Arbeit

| Ereignis | Zeitpunkt | Quelle |
|---|---|---|
| Wiederbeginn nach der Pause (Claude-Init, arc42-Doku, Java-Records, Multiversion) | 2026-04-27 bis 2026-04-30, 18 Commits | `git log origin/main --since=2026-04-01 --until=2026-05-01` (gemessen) |
| "Add agreed modernization plan for 6.0.0" (Commit `4a492050`, spaeter ersetzt) | 2026-07-03 | `git log` (gemessen); in #52 steht, der alte Plan sei ersetzt und per `git show 4a49205:docs/modernization-plan-6.0.0.md` lesbar |
| Erste Agenten-Werkzeug-Commits (Skills, Implementierungsprozess) | 2026-10-22 21:23:21Z (`c3af8b8a`) | `git log` (gemessen) |
| **Wayfinder-Map #52 erstellt** | **2026-10-22T21:51:54Z** | `gh api repos/marmer/hamcrest-matcher-generator/issues/52` (gemessen) |
| Spec #104 erstellt | 2026-10-26T19:51:35Z | `.../issues/104` (gemessen) |
| Map #52 geschlossen | 2026-11-14T23:57:55Z | `.../issues/52` (gemessen) |
| **Tag `6.0.0`** (Release-PR #210) | **2026-11-15T10:46:57Z** | `git show -s 6.0.0`, Releases-API (gemessen) |
| Maven Central `lastUpdated` | 2026-11-15 11:18:02 UTC | Central `maven-metadata.xml` (gemessen) |
| Letzter Commit | 2026-11-15 12:44:48 +0100 | `git log -1 origin/main` (gemessen) |

Gemessene Wallclock-Dauer (Rechnung aus den Zeitstempeln):

- Map #52 erstellt bis Tag `6.0.0`: **23 Tage 12 Stunden 55 Minuten** (2026-10-22T21:51:54Z bis 2026-11-15T10:46:57Z).
- Map #52 erstellt bis Central-Veroeffentlichung (`lastUpdated` 11:18:02 UTC): 23 Tage 13 Stunden 26 Minuten.
- Spec #104 erstellt bis Tag `6.0.0`: 19 Tage 14 Stunden 55 Minuten.
- Vom Beginn der Agenten-Vorbereitung (2026-10-22 21:23Z) bis zum letzten Commit (2026-11-15 11:44Z): rund 23,5 Tage.
- Commit-Tage in diesem Fenster: 21 von 25 Kalendertagen (2026-10-22 bis 2026-11-15); ohne Commit: 2026-11-02, 11-03, 11-04, 11-07 (`git log origin/main --since=2026-10-22 --format=%ad --date=format:%Y-%m-%d | sort -u`).
- Commits pro Tag im Fenster (gemessen): 10-22: 10; 10-23: 7; 10-24: 8; 10-25: 7; 10-26: 8; 10-27: 29; 10-28: 23; 10-29: 41; 10-30: 5; 10-31: 6; 11-01: 4; 11-05: 3; 11-06: 1; 11-08: 31; 11-09: 5; 11-10: 2; 11-11: 14; 11-12: 1; 11-13: 19; 11-14: 24; 11-15: 7.
- Aktive Stunden-Buckets (Datum plus Stunde mit mindestens einem Commit): 138 (`git log --since=2026-10-22 --format=%ad --date=format:'%Y-%m-%d %H' | sort -u | wc -l`). Das ist **keine** Arbeitszeit des Owners und keine Rechenzeit der KI; es ist nur ein Aktivitaetsraster.

Grenze: Eine Wallclock-Dauer sagt nichts ueber den Zeitaufwand des Owners (Grilling, Freigaben, Reviews). Der Owner-Aufwand ist aus Git und GitHub **nicht ermittelbar** (Abschnitt 7).

## 2. Umfang der 6.0.0-Arbeit

### 2.1 Tickets, PRs, Module

| Messwert | Wert | Quelle |
|---|---|---|
| Karten der Wayfinder-Map #52 | 24, alle erledigt (7 research, 11 grilling, 4 prototype, 2 task) | Status-Abschnitt von #52: "**24/24 done**" (gemessen) |
| Tickets im Spec #104 | 100, alle erledigt | Status-Abschnitt von #104: "**100/100 done** (Done 100, In progress 0, Ready 0, Blocked 0)" (gemessen) |
| Issues seit 2026-04 (ohne PRs) | 204, davon 176 geschlossen und 28 offen | REST `repos/.../issues?state=all` seitenweise, `created_at >= 2026-04-01` (gemessen) |
| Labels dieser 204 Issues | 155 `ready-for-agent`, 16 `ready-for-human`, 13 `wayfinder:grilling`, 10 `retro`, 7 `wayfinder:research`, 4 `wayfinder:prototype`, 2 `wayfinder:task`, 2 `needs-info`, 1 `wayfinder:map` (Mehrfachlabels moeglich) | wie oben (gemessen) |
| Issues seit Map-Start (2026-10-22) | 204 (alle 204 sind seither erstellt) | wie oben (gemessen) |
| Gemergte PRs seit 2026-10-22 | 242: 215 ueber das Owner-Konto `marmer`, 24 Renovate, je 1 Dependabot, Autofix-Bot, Release-Bot | REST `repos/.../pulls?state=all`, `created_at >= 2026-10-22` und `merged_at` gesetzt (gemessen) |
| Ungemergt geschlossene PRs seit 2026-10-22 | 4 (#448, #449, #464, #468) | wie oben (gemessen) |
| PRs gesamt im Repo | 278 (alle geschlossen) | wie oben (gemessen) |
| Merge-Commits auf `main` | 6 seit 5.1.0 (Rest sind Squash-Merges) | `git rev-list --count --merges 5.1.0..origin/main` (gemessen) |
| PR-Laufzeit Erstellung bis Merge (Owner-Konto, 207 PRs mit Claude-Zeile im Text, seit 2026-10-22) | Median 5,1 Minuten, 90. Perzentil 37,5 Minuten, Mittel 106 Minuten | `created_at`/`merged_at` aus der PR-API (abgeleitet). Das ist PR-Lebensdauer (Auto-Merge nach gruenen Checks), **keine** Owner-Arbeitszeit |

Hinweis zu den Labels: Die Label-Zaehlung (13 grilling, 7 research, 4 prototype, 2 task) gilt ueber alle 204 Issues seit 2026-04 und enthaelt auch das spaetere Grilling #503 und weitere Tickets ausserhalb der Map. Die 24 Karten der Map #52 selbst sind nach dem Diagramm im Status-Abschnitt von #52: research #53 bis #58 und #94 (7), grilling #59 bis #64, #74, #89, #91, #95, #99 (11), prototype #65 bis #68 (4), task #72 und #83 (2) = 24 (eigene Zaehlung aus dem Diagramm).

Module (Root-pom `pom.xml` auf `origin/main`, `git show origin/main:pom.xml | grep '<module>'`):

- Reactor (immer): `matcher-generator-annotations`, `matcher-generator-core`, `hamcrest-matcher-generator`, `assertj-assertion-generator`, `hamcrest-matcher-generator-kotlin`, `hamcrest-matcher-generator-dependencies`, `hamcrest-matcher-generator-annotationprocessor`, `example-maven-java-hamcrest`, `example-maven-java-assertj`, `example-maven-kotlin` = 10 Module.
- Profil `dev`: Aggregator `hamcrest-matcher-generator-endtoend` mit 7 End-to-End-Modulen (`plain-java-minimal`, `mixed-kotlin-java`, `plain-kotlin`, `java-records`, `lombok`, `assertj`, `5x-compatibility-runner`; `5x-compatibility` liegt als eingefrorene Suite daneben).
- Profil `docs`: Modul `docs`.
- Ausserhalb des Reactors: `example-gradle-java` (eigener Gradle-Wrapper).
- Vorher (5.1.0): 5 Module-poms plus Root (`git ls-tree -r --name-only 5.1.0 | grep pom.xml`).
- 10 Workflows unter `.github/workflows/` (`git ls-tree`), 7 ADRs unter `docs/adr/`.

### 2.2 Zeilen nach Art (gemessen mit eigenem Skript)

Befehl (Skript im Scratchpad, nicht im Repo): fuer jede Datei von `git ls-tree -r --name-only <rev>` die Zeilen der Blobs ueber `git cat-file --batch` zaehlen; Binaerdateien (NUL im Kopf) zaehlen 0. Das Skript liest den Git-Baum, nicht den Arbeitsbaum, damit die lokalen Ordner `.claude/worktrees/` nicht mitzaehlen. Einordnung nach Pfad:

- "Produktionscode (ausgeliefert)": `*.java`/`*.kt` unter `src/main/` der sieben ausgelieferten Module (`matcher-generator-annotations`, `-core`, `hamcrest-matcher-generator`, `assertj-assertion-generator`, `hamcrest-matcher-generator-kotlin`, `-dependencies`, `-annotationprocessor`).
- "Testcode": `*.java`/`*.kt` unter `src/test/` oder `src/it/` in allen Modulen (inklusive End-to-End- und Beispielmodulen).
- "Testressourcen": Nicht-Code-Dateien unter `src/test/` oder `src/it/` (Golden Files, Fixtures, Test-poms).
- "Fixture-/Beispielcode (nicht ausgeliefert)": `*.java`/`*.kt` unter `src/main/` der End-to-End- und Beispielmodule.
- "Doku": alles unter `docs/` (arc42-Kapitel, ADRs, Benutzerhandbuch, Structurizr, Modul-pom, Docs-Tests), plus `README.md`, `GLOSSARY.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `MAINTAINING.md`, `SECURITY.md`, `NOTICE`, `LICENSE`.
- "Agenten-Werkzeug/Skripte": `.agents/`, `.claude/`, `scripts/`, `notes/`, `CLAUDE.md`, `skills-lock.json`.
- "Build/CI/Konfig": `pom.xml`-Dateien, `.github/`, `.mvn/`, Wrapper (`mvnw`, `mvnw.cmd`), JSON/XML/YAML/Gradle/Properties ausserhalb der obigen Gruppen.

| Art | 5.1.0 (Zeilen / Dateien) | `origin/main` (Zeilen / Dateien) |
|---|---|---|
| Produktionscode (ausgeliefert) | 751 / 6 (Kotlin) | **7360 / 55** (Java, plus 698 Zeilen Kotlin-Wrapper-Generator im Modul `-kotlin`) |
| Testcode | 3828 / 16 | **28303 / 221** |
| Testressourcen | 1 / 1 | 579 / 74 |
| Fixture-/Beispielcode | 208 / 18 | 1312 / 75 |
| Doku | 606 / 3 | **7228 / 80** |
| Agenten-Werkzeug/Skripte | 0 | **14821 / 206** |
| Build/CI/Konfig | 1708 / 17 | 4595 / 45 |
| Sonstiges | 18 / 3 | 362 / 13 |
| **Summe** | **7120 / 64** | **64560 / 769** |

Gesamtdifferenz: `git diff --shortstat 5.1.0 origin/main` = **776 Dateien geaendert, 62791 Einfuegungen, 5351 Loeschungen**. `git diff --shortstat 5.1.0 6.0.0` = 776 Dateien, 62817 Einfuegungen, 5351 Loeschungen.

Zeilen pro Modul (Auswahl, Produktion / Test; gleiches Skript, `python3 -I count.py origin/main x`):

| Modul | Produktion | Test |
|---|---|---|
| `matcher-generator-core` | 2480 | 5493 |
| `hamcrest-matcher-generator` (Hamcrest-Backend und Runtime) | 2474 | 11115 |
| `assertj-assertion-generator` | 1513 | 4320 |
| `hamcrest-matcher-generator-kotlin` | 698 | 3007 |
| `matcher-generator-annotations` | 161 | 63 |
| `-dependencies` / `-annotationprocessor` (Bruecken) | je 17 | je ca. 100 |
| End-to-End-Module (7 plus 5x-Suite) | Fixtures 1312 (mit Beispielmodulen) | Tests ca. 3500 |
| Beispielmodule (4) | 165 | 532 |

Grenzen dieser Zaehlung:

- **Zeilen sind kein Aufwandsmass.** Der Stand 5.1.0 war Kotlin, der neue Code ist Java. Java braucht fuer dieselbe Logik tendenziell mehr Zeilen. Der Anstieg von 751 auf 7360 Produktionszeilen ist deshalb kein Vergleich gleicher Funktionalitaet; 6.0.0 hat ausserdem einen AssertJ-Backend, ein Kotlin-DSL-Backend und eine Kernbibliothek, die es in 5.1.0 nicht gab.
- Die Kategorie "Doku" enthaelt unter `docs/` auch die Java-Tests des Docs-Moduls und Skripte der Doku-Pruefung, sowie CHANGELOG-Zeilen, die von release-please generiert sind (194 Zeilen).
- "Agenten-Werkzeug" (14821 Zeilen) ist Prozess-Infrastruktur fuer die KI-Sitzungen (Skills, Briefs, Python-Skripte wie `scripts/agents/issue.py`). Sie gehoert zur Arbeit, aber nicht zum Produkt, und wird in der Aufwandsschaetzung (Abschnitt 5) **nicht** mitgerechnet.
- Golden Files und generierte Dateien sind in "Testressourcen" nur mit 579 Zeilen vertreten; die Wrapper-Skripte `mvnw`/`mvnw.cmd` (408 Zeilen) sind generierter Boilerplate in "Build".
- `git diff --numstat` ueber Commits mit KI-Trailer summiert 74939 Einfuegungen und 20985 Loeschungen (`git log 5.1.0..origin/main -i -E --grep='^Co-Authored-By:.*Claude|^Claude-Session:' --numstat --format=`). Das ist Churn mit Mehrfachzaehlung von Zeilen, die in mehreren Commits erneut geaendert wurden, kein Nettowert. Er ist hoeher als die Netto-Differenz (62791), das ist erwartbar.

### 2.3 Tests

| Messwert | Wert | Quelle |
|---|---|---|
| Testannotationen in `origin/main` (statisch) | 705 `@Test`, 27 `@ParameterizedTest` (23 davon mit Namen `deprecated alias: {0}`), 1 `@FuzzTest` | `git grep -h -E '^\s*@(Test\|ParameterizedTest\|RepeatedTest\|TestFactory\|Property\|FuzzTest)\b' origin/main -- '*.java' '*.kt'` (gemessen) |
| Testannotationen im Stand 5.1.0 | 58 `@Test` | gleicher Befehl auf `5.1.0` (gemessen) |
| Testdateien mit Annotation je Modul | z. B. `hamcrest-matcher-generator` 16, `matcher-generator-core` 12, `plain-java-minimal` 10, `assertj` 9, `5x-compatibility` 7, `assertj-assertion-generator` 7 | `git grep -l` (gemessen) |
| Qualitaets-Gates (Konfiguration) | JaCoCo `COVEREDRATIO` Minimum 0,99 (Zeilen und Branches), PIT `mutationThreshold` 90 und `coverageThreshold` 99 | `git show origin/main:pom.xml` Zeilen 427 bis 433 und 898 bis 899 (gemessen) |

Die tatsaechliche Zahl ausgefuehrter Tests (Surefire/Failsafe, parametrisierte Faelle ausgeklappt) ist **nicht ermittelbar**, weil ich keinen Build gelaufen habe (kein Maven-Lauf in dieser Recherche). Die Annotationszahl ist ein Naeherungswert: parametrisierte Tests laufen mehrfach, Kotlin-Tests mit Backtick-Namen (`fun \`...\``, 81 Treffer) sind in `@Test` enthalten, Golden-File-Faelle und Compile-Failure-ITs (`src/it/` in `plain-kotlin`) laufen ueber andere Mechanismen. Wer eine harte Zahl will, muss `./mvnw verify` ausfuehren und die Surefire-/Failsafe-Berichte summieren.

### 2.4 Was dazugekommen ist (Features, aus Primaerquellen)

Aus dem `6.0.0`-Abschnitt von `CHANGELOG.md` (generiert aus Conventional Commits) und dem Spec #104: Kern-/Backend-Trennung (SPI), neue Koordinaten und `@GenerateMatchers`, Completeness-Check (Hamcrest und AssertJ), Mismatch-Meldungen mit Property-Pfaden, Referenzobjekt-Matcher `isXLike`, Runtime-Collection-Matcher, JSpecify-Nullness, `exclude`, Compiler-Diagnosen, AssertJ-Backend (Assert-Klassen, Soft Assertions, Representation), Kotlin-DSL-Backend, Kotlin-Property-Erkennung, Maven-Central-Pipeline ueber das Central Portal, release-please, Renovate, Beispielmodule (Maven Hamcrest/AssertJ/Kotlin, Gradle), arc42 plus Benutzerhandbuch, MAINTAINING.md. Quelle: `git show origin/main:CHANGELOG.md`, Issue #104 (User Stories 1 bis 97).

## 3. Beitraege

### 3.1 Commits nach Autor (gemessen)

`git log origin/main --format='%an' | sort | uniq -c | sort -rn`, gesamt 1033 Commits:

| Autorname | Commits |
|---|---|
| MarMer | 697 |
| Mariano Mertinat | 225 |
| mariano.mertinat | 64 |
| renovate[bot] | 24 |
| Claude | 17 |
| marmer-hamcrest-release[bot], github-agentic-autofix[bot], dependabot[bot], Kevin Shale, Julien Berthoud, Dmitrii Sapronov | je 1 |

Nach E-Mail-Identitaet (`git log --format='%an <%ae>'`): Die GitHub-noreply-Adresse des Owner-Kontos (`1055445+marmer@...`) steht bei 691 Commits (Name MarMer) und 224 (Name Mariano Mertinat), zusammen **915**. Das ist dieselbe Zahl, die die Contributors-API als `marmer` liefert (heute 915; die aeltere Recherche nannte 910). Weitere 71 Commits der Namen `mariano.mertinat`, `MarMer` (ohne Adresse) und eine zweite Adresse laufen in der API nicht unter `marmer` (Adressen hier nicht wiedergegeben). 17 Commits haben `Claude <noreply@anthropic.com>` als Autor.

**Der Fehler der alten Zahl:** `910 marmer / 17 claude` beantwortet "welches Konto steht im Autor-Feld". Seit der 6.0.0-Arbeit committen die Agenten-Sitzungen ueber PRs, die als Owner-Konto geoeffnet und als Squash-Merge in `main` aufgenommen werden; dann steht der Owner als Autor und `Co-Authored-By: Claude ...` sowie `Claude-Session:` nur im Trailer. Die 17 `claude`-Commits sind nur die Commits, bei denen die Sitzung selbst den Autor-Eintrag gesetzt hat (13 davon ab 2026-10-22, 2 im April, 2 im Juli).

### 3.2 Korrigierte Zaehlung mit Trailern (gemessen)

Alle Zahlen im Bereich `5.1.0..origin/main`, 275 Commits (alle aus 2026). Befehle (Gross-/Kleinschreibung ignoriert):

- Mit KI-Trailer (`Co-Authored-By: ...Claude...` **oder** `Claude-Session:`): `git log 5.1.0..origin/main -i -E --grep='^Co-Authored-By:.*Claude|^Claude-Session:' --format=%h | wc -l` = **228**.
- Nur `Co-Authored-By: Claude`: 225. Nur `Claude-Session:`: 220. Alle Commits im gesamten Repo mit einem der beiden Trailer: ebenfalls 228 (vor 5.1.0 gibt es keine).
- Ohne KI-Trailer: 47. Davon Bots: 24 Renovate, 1 Dependabot, 1 Release-Bot (der `Co-Authored-By`-Zaehler zaehlt Renovate in anderer Form); Nicht-Bot: 13 `MarMer` (April 2026, manuell, u. a. Records, Multiversion, Badges), 6 `Mariano Mertinat` (3 Merge-Commits, der CI-PR #354 und zwei weitere), 2 `Claude` (Aprilcommits ohne Trailer).
- Autor der 228 Trailer-Commits (`git log ... --format=%an`): **211 `Mariano Mertinat`**, 15 `Claude`, 1 `MarMer`, 1 `github-agentic-autofix[bot]`.
- Modellnamen in den `Co-Authored-By`-Trailern (225 Zeilen, `%(trailers:key=Co-Authored-By)`): 199 "Claude" ohne Modell, 13 "Claude Opus 5.5", 6 "Claude Haiku 5.5", 4 "Claude Sonnet 5.5", 2 "Claude Fable 5", 1 "Claude Sonnet 4.6". Die Modellangabe fehlt in den meisten Trailern, eine Modell-Verteilung ist daraus nicht ableitbar.

Anteile (abgeleitet): 228 von 275 Commits (82,9 %) im 6.0.0-Bereich; 227 von 248 Nicht-Bot-Commits (91,5 %). Die 21 Nicht-Bot-Commits ohne Trailer sind 13 `MarMer`, 6 `Mariano Mertinat` (davon 3 Merge-Commits und der CI-PR #354) und 2 `Claude`.

Im Zeitfenster der eigentlichen 6.0.0-Arbeit (`--since=2026-10-22`): 254 Commits (`git log --since=2026-10-22 --format=%h | wc -l`), Autoren dort: 215 `Mariano Mertinat`, 24 Renovate, 13 `Claude`, je 1 Release-Bot, Autofix-Bot, Dependabot.

### 3.3 PRs: von wem geoeffnet (gemessen)

REST `repos/.../pulls?state=all&per_page=100&page=1..7` (Seiten einzeln, weil `--paginate` hinter dem Proxy 403 liefert, siehe `docs/research/gh-paginate-proxy-403.md`), 278 PRs gesamt. Seit 2026-04-01 (`created_at`, 262 PRs): **218 PRs vom Owner-Konto `marmer` mit der Zeile "Generated with Claude Code" oder dem `claude.ai/code/session`-Link im PR-Text** (211 gemergt, 7 ohne Merge), 11 vom Owner-Konto **ohne** diese Zeile (10 gemergt, 1 nicht gemergt), 28 Renovate (24 gemergt), je 1 bis 3 Dependabot, Autofix- und Release-Bot. Alle Agenten-PRs sind formal vom Owner-Konto geoeffnet; eine eigene KI-Kennung als PR-Autor gibt es nicht. Mensch/Agent ist also nur ueber Text-Trailer und PR-Text unterscheidbar.

### 3.4 Methode und Grenzen

- Die Trailer entstehen, weil die Agenten-Konfiguration sie vorschreibt (die Zeilen `Co-Authored-By: Claude ...` und `Claude-Session: https://claude.ai/code/session_...`; vgl. `docs/agents/implementation.md`: "with the `Claude-Session:` line for its commits"). Ein Trailer belegt "KI war an diesem Commit beteiligt", nicht den Anteil. Ein Commit mit Trailer kann von einem Menschen nachbearbeitet sein; ein Commit ohne Trailer kann trotzdem KI-unterstuetzt sein (z. B. die 13 manuellen `MarMer`-Commits im April 2026, `Claude Init` am 2026-04-27 gehoert zur KI-Nutzung, hat aber keinen Trailer).
- Squash-Merges fassen eine PR in einen Commit zusammen; die Zahl der Commits ist damit eine Zahl **gemergter Einheiten**, nicht der Arbeitsschritte. Die Zahl der Commits ist deshalb kein Mass fuer Zeilen oder Zeit.
- Zeilenanteile nach Autor oder Trailer sind in dieser Recherche nicht ausgewertet (die Churn-Summe mit Trailer ist 74939 Einfuegungen gegen 3669 fuer die uebrigen menschlichen Commits ohne Trailer; Zaehlung: `git log 5.1.0..origin/main -i -E --invert-grep --grep=... --no-merges --numstat --format= --author='^(MarMer|Mariano|mariano|Claude)'`). Das ist Churn und enthaelt Mehrfachzaehlung; ich wuerde es nicht als Anteil zitieren.
- Der Owner-Anteil an Entscheidungen (Grilling, Freigaben, Review) ist nicht in Git sichtbar. Die Kommentare in den Tickets zeigen Dialoge, die ich nicht ausgewertet habe.

## 4. Motivation, woertlich aus den Primaerquellen

Quelle ist Issue #52 "Wayfinder: 6.0.0, the final feature release" (erstellt 2026-10-22T21:51:54Z, geschlossen 2026-11-14T23:57:55Z, Autor `marmer`), gelesen ueber `gh api repos/marmer/hamcrest-matcher-generator/issues/52`. Der Text ist in englischer Sprache.

### 4.1 Zitate (woertlich)

Aus **#52, Abschnitt "Destination"**:

> An agreed specification for **6.0.0**, the final feature release, plus implementation tickets labelled `ready-for-agent`. [...] After 6.0.0 the project runs on autopilot.

Aus **#52, Abschnitt "Notes", Punkt "Goals"**:

> **Goals**: (1) "last honours": feature-complete, then automatically maintained with minimal owner effort in development, release and updates; (2) **portfolio project**: code quality, a modern supply chain, and architecture docs (arc42 + ADRs) must be visible. AI-assisted development may stay visible as a side effect.

Aus **#52, Abschnitt "Out of scope"**:

> Features after 6.0.0.

Aus **#52, Abschnitt "Notes", Punkt zu Benachrichtigungen**:

> **Owner is notified of every automatic action with external effect** (owner request): a successful automatic release @-mentions the owner [...]; failures always notify.

Aus **#104 (Spec), Abschnitt "Problem Statement"**, letzte zwei Absaetze:

> The owner also wants to stop active feature work. After one last feature release the project must run on autopilot: bots update dependencies, merge green pull requests and publish releases, and the owner only steps in when something turns red. The project is also a portfolio piece, so its code quality, supply chain and architecture documentation (arc42 + ADRs) must hold up to scrutiny.

Aus **#104, Abschnitt "Owner and autopilot"**, User Story 94 (zeigt die Rolle der KI als Vertretung):

> As the owner, I want a `MAINTAINING.md` runbook covering a red Renovate PR, a failed publish, an expiring credential and a dependency that needs a newer Java, so that anyone with admin rights, or an AI agent session, can act for me.

Aus **#74 "Top quality goals (LASR Top-5 Challenger)"** (Kontextzeile):

> Context: the map's goals ("last honours" autopilot, portfolio project) [...]

Aus **#83** (CycloneDX/Provenance-Einfuehrung):

> Value is for consumers with supply-chain policies (Dependency-Track, Trivy, Grype) and the portfolio goal [...]

Aus **#503** (das Ticket selbst), "Context":

> Likely topics (owner's expectation, not decided): the motivation from map #52 ("last honours", portfolio project, then autopilot), AI-assisted development, efficiency gain. [...] Assumption: the map's Notes say AI use "may stay visible as a side effect"; whether it now becomes a topic is open.

Das einzige Repo-Dokument mit dem Wort "autopilot" ausserhalb der Tickets: `MAINTAINING.md` Zeile 4 ("The project runs on bots: Renovate merges green dependency pull requests, release-please keeps ...") und arc42 Kapitel 11 (`docs/src/docs/asciidoc/architecture/_chapters/11_technical_risks.adoc`, Zeile 6: "After 6.0.0 the project is meant to run on autopilot: bots update dependencies, merge green pull requests and publish releases, and the maintainer only steps in when something turns red."). `docs/agents/wayfinding.md` enthaelt keine Motivation (nur Prozessregeln).

Die Begriffssuche ueber die Issue-Suche (`GET search/issues?q=<begriff> is:issue repo:marmer/hamcrest-matcher-generator`) ergab: "honours" in #52, #74 und #503 als "last honours"; in #283 und #488 steht "honours" nur als Verb ("whether auto-merge honours a bypass actor", "endpoint honours `page=N`"), ohne Bezug zur Motivation; "portfolio" in #52, #74, #83, #104, #503; "side effect" in #52 und #503; "Freizeit" und "hobby": keine Treffer. Der Kommentar zu #52 (einziger, 2026-10-26T19:51:40Z) verweist nur auf Spec #104.

### 4.2 Was die Zitate belegen und was nicht

- **Belegt:** (1) "last honours" bedeutet laut #52: funktional vollstaendig machen, dann mit minimalem Owner-Aufwand automatisch weiterpflegen. (2) Das zweite Ziel ist "portfolio project": Codequalitaet, moderne Lieferkette, Architekturdoku muessen sichtbar sein. (3) KI-gestuetzte Entwicklung "may stay visible as a side effect". (4) Nach 6.0.0 "runs on autopilot", keine Features mehr.
- **Nicht belegt:** "Freizeit", Zeitnot als Ursache der 973-Tage-Pause, "nie Zeit", ein Effizienzziel ("mit KI schneller") oder KI als Hauptmotiv. Die Formulierung "Qualitaet, Feature-Reichtum, Absicherung" ist eine Zusammenfassung des Owners, nicht ein Zitat; in #52 sind Qualitaet (Quality gates, Portfolio) und Features (Feature-Pool, "completeness check ... is the lead feature") einzeln genannt, aber nicht als Motto.
- **Nuance:** Der Zusatz "portfolio project" ist ein zweites, eigenstaendiges Motiv neben "last honours". Wer im Post nur "letzte Ehre" nennt, laesst es weg; wer "Portfolio" nennt, sollte es als Sichtbarkeitsziel erklaeren, das #52 ausdruecklich als Ziel fuehrt.

### 4.3 Absicherung, Qualitaet: was im Repo konfiguriert ist (gemessen, aus Dateien)

- JaCoCo-Floor 0,99 (Zeilen und Branches) und PIT mutationThreshold 90 / coverageThreshold 99: `pom.xml` (Zeilen 427 bis 433, 898/899).
- Pflicht-Checks laut #52 Decisions ("Quality gates", #62): Build, Verify JDK 17/21/25/latest, eigener Sonar-Job (blockierend), Mutation testing (PIT, >= 90 % je Modul), Dependency review, CodeQL, PR-Titel, Domain docs; Spotless/google-java-format, Error Prone + NullAway in einem JDK-21+-Profil. Ob jeder dieser Gates heute faktisch gruen laeuft, habe ich nicht einzeln geprueft; der letzte Lauf `Release` und `CI-Build` auf `main` (2026-11-15T11:44:52Z) ist `success` (`gh api repos/.../actions/runs`).
- OpenSSF-Scorecard-Wert 7,4/10 am 2026-10-31 (aus #283), "Code-Review 0/10 (0 von 29 Changesets freigegeben)". Das ist relevant fuer die Aussage "Absicherung": Pull-Request-Reviews durch einen zweiten Menschen gibt es nicht (Ruleset verlangt 0 Freigaben).

## 5. Aufwandsschaetzung (SCHAETZUNG, nicht gemessen)

Frage: Wie lange braeuchte ein Mensch **allein, in der Freizeit**, fuer denselben Umfang? Es gibt dafuer keine Messung. Die Zahlen unten sind meine Annahmen und stuetzen sich auf keine zitierte Studie; die Tempo-Annahmen (Zeilen pro Stunde) sind frei gewaehlt und nicht aus der Literatur belegt. Ich behaupte keinen Beschleunigungsfaktor durch KI.

### 5.1 Was gerechnet wird

Aus Abschnitt 2.2, ohne Agenten-Werkzeug (14821 Zeilen) und ohne "Sonstiges":

- Produktionscode (ausgeliefert): 7360 Zeilen.
- Test: 28303 Zeilen Testcode plus 1312 Fixture-/Beispielcode plus 579 Testressourcen = 30194 Zeilen.
- Doku: 7228 Zeilen.
- Build/CI/Konfiguration: 4595 Zeilen.
- 24 Entscheidungskarten der Map #52 (Research, Grilling, Prototypen).
- 218 PRs und 204 Issues als Prozess-Overhead (Branch, PR, auf CI warten, mergen, Ticket pflegen).

Ausdruecklich nicht gerechnet: die Agenten-Infrastruktur (Skills, Briefs, `scripts/agents/`), weil ein Mensch ohne KI sie nicht braeuchte.

### 5.2 Methode A, zeilenbasiert (Schaetzung)

Gesamtstunden = Zeilen / (Zeilen pro Stunde), einschliesslich Entwurf, Debuggen, Review, nicht nur Tippen.

| Annahmensatz | Produktion | Test (incl. Fixtures) | Doku | Build/CI | Entscheidungen je Karte | PR je | Ticket je |
|---|---|---|---|---|---|---|---|
| **Untergrenze** (schneller, erfahrener Entwickler, wenig Neuland) | 15 Zeilen/h | 40 Zeilen/h | 30 Zeilen/h | 20 Zeilen/h | 3 h | 5 min | 5 min |
| **Mitte** | 8 Zeilen/h | 20 Zeilen/h | 15 Zeilen/h | 10 Zeilen/h | 5 h | 10 min | 10 min |
| **Obergrenze** (langsamer, viel Recherche, Neuland) | 4 Zeilen/h | 10 Zeilen/h | 8 Zeilen/h | 5 Zeilen/h | 8 h | 20 min | 15 min |

Ergebnis (Rechnung mit `python3`, ganze Stunden):

| Satz | Produktion | Test | Doku | Build/CI | Entscheidungen | PR+Ticket | **Summe** |
|---|---|---|---|---|---|---|---|
| Untergrenze | 491 | 755 | 241 | 230 | 72 | 35 | **1823 h** |
| Mitte | 920 | 1510 | 482 | 460 | 120 | 70 | **3561 h** |
| Obergrenze | 1840 | 3019 | 904 | 919 | 192 | 124 | **6998 h** |

### 5.3 Methode B, ticketbasiert (Schaetzung)

Gegenprobe ohne Zeilen: 155 `ready-for-agent`-Tickets, jedes klein geschnitten (so schreibt es `docs/agents/implementation.md`: "small and precise enough for a small model", eine Sorge je Ticket), plus 24 Entscheidungskarten. Stunden je Ticket frei angenommen:

| Satz | je Implementierungsticket | je Entscheidungskarte | Summe |
|---|---|---|---|
| Untergrenze | 2 h | 3 h | 155 x 2 + 24 x 3 = **382 h** |
| Mitte | 4 h | 5 h | 155 x 4 + 24 x 5 = **740 h** |
| Obergrenze | 8 h | 8 h | 155 x 8 + 24 x 8 = **1432 h** |

Die beiden Methoden weichen um etwa den Faktor 4 bis 5 voneinander ab (382 gegen 1823, 740 gegen 3561, 1432 gegen 6998). Das ist selbst eine Aussage: Die Schaetzung ist so unsicher, dass sie nur als **Groessenordnung** taugt. Methode A bewertet auch Testzeilen hoch, die in KI-Sitzungen mit Red-Green-Pflicht entstanden sind; Methode B unterstellt, dass die Tickets die Arbeit gut abbilden. Welche naeher liegt, ist ohne Messung nicht entscheidbar.

### 5.4 Von Stunden zu Kalenderzeit

Freizeit pro Woche ist ebenfalls eine freie Annahme (5, 10 oder 15 Stunden):

| Satz (Stunden) | bei 5 h/Woche | bei 10 h/Woche | bei 15 h/Woche |
|---|---|---|---|
| Methode B Untergrenze (382 h) | 76 Wochen (1,5 Jahre) | 38 Wochen (0,7 Jahre) | 25 Wochen (0,5 Jahre) |
| Methode B Mitte (740 h) | 148 Wochen (2,8 Jahre) | 74 Wochen (1,4 Jahre) | 49 Wochen (0,9 Jahre) |
| Methode A Untergrenze (1823 h) | 365 Wochen (7,0 Jahre) | 182 Wochen (3,5 Jahre) | 122 Wochen (2,3 Jahre) |
| Methode A Mitte (3561 h) | 712 Wochen (13,7 Jahre) | 356 Wochen (6,8 Jahre) | 237 Wochen (4,6 Jahre) |
| Methode A / B Obergrenze (6998 h / 1432 h) | 1400 Wochen (27 Jahre) / 286 Wochen (5,5 Jahre) | 700 (13,5 Jahre) / 143 (2,8 Jahre) | 467 (9 Jahre) / 95 (1,8 Jahre) |

(Wochen = Stunden / Wochenstunden, Jahre = Wochen / 52.)

### 5.5 Wie die Annahmen wirken

- **Tempo (Zeilen/Stunde)** ist der groesste Hebel: Halbiert man alle Tempi, verdoppelt sich Methode A. Das ist die Spalte, die ich am wenigsten belegen kann.
- **Testanteil:** 30194 Zeilen Test machen 42 Prozent der Methode-A-Summe in der Mitte; wuerde man Tests schneller als 20 Zeilen/h annehmen (etwa durch generische Hilfen), sinkt die Summe deutlich.
- **Freizeit pro Woche** wirkt linear auf die Kalenderzeit, aendert aber nichts am Stundenaufwand. 5 gegen 15 Stunden ist Faktor 3.
- **Was ein Mensch **nicht** haette:** Das Projekt waere vermutlich kleiner geschnitten worden (weniger Module, weniger Gates, keine 7 End-to-End-Module). Die Schaetzung rechnet "denselben Umfang", nicht "dasselbe Ergebnis mit weniger".
- **Was die Schaetzung nicht enthaelt:** Warte- und Einarbeitungszeiten nach langen Pausen (nach 973 Tagen wuerde ein Mensch Zeit zum Wiedereinarbeiten brauchen), Kontextwechsel, Review durch Dritte.

### 5.6 Gemessene Wallclock-Dauer (getrennt von der Schaetzung)

Gemessen (Abschnitt 1.5): **23 Tage 12 Stunden 55 Minuten** von Map #52 bis Tag `6.0.0`; **21 von 25 Kalendertagen** mit Commits; **242 gemergte PRs** und **204 Issues** in dieser Zeit. Daraus abgeleitet (Rechnung): rund 10 gemergte PRs pro Kalendertag und rund 8 Issues pro Kalendertag im Mittel. Das sind Durchsatz-Zahlen des Gesamtsystems (Owner plus Agenten plus Bots), kein Owner-Arbeitstempo.

Weil die Wallclock-Dauer (gemessen) und die Menschzeit (geschaetzt) verschiedene Dinge sind, rechne ich **keinen Faktor** zwischen ihnen aus. Wer einen Faktor nennen will, muesste den tatsaechlichen Owner-Aufwand messen (Abschnitt 7) und die Schaetzung mit der gleichen Zeilen-Basis annehmen.

## 6. Pruefung der Aussage

Aussage: "Bibliothek, der die letzte Ehre erwiesen wird: Qualitaet, Feature-Reichtum, Absicherung, fuer die in der Freizeit nie Zeit war, jetzt mit KI."

| Teilaussage | Status | Beleg |
|---|---|---|
| "letzte Ehre" | Gedeckt, mit eigener Wortbedeutung | #52 Notes: `"last honours": feature-complete, then automatically maintained ...`. Heisst: letztes Feature-Release, danach Autopilot, nicht "Beerdigung". "Features after 6.0.0" ist Out of scope (#52). |
| Qualitaet | Gedeckt (Konfiguration und Doku belegt, Laufzeit nicht einzeln geprueft) | JaCoCo 0,99, PIT 90, Sonar-Gate, Spotless/Error Prone/NullAway (`pom.xml`, #62); 7 ADRs, arc42, 22 Qualitaetsszenarien in Kapitel 10 (#91) |
| Feature-Reichtum | Gedeckt | AssertJ-Backend, Kotlin-DSL, Completeness-Check, Mismatch-Pfade, Referenzobjekt-Matcher, `exclude`, JSpecify (CHANGELOG 6.0.0, #104). In 5.1.0 gab es nur Hamcrest-Matcher (siehe Stand 5.1.0). |
| Absicherung | Teilweise gedeckt | Test- und Mutations-Gates ja; aber 705 + 27 + 1 Testannotationen statisch gezaehlt, nicht ausgefuehrte Tests; Code-Review durch Dritte 0/10 (Scorecard in #283) |
| "fuer die in der Freizeit nie Zeit war" | **Nicht belegt** | Kein Satz dazu in den Tickets oder im Repo. Konsistent damit sind: 973 Tage ohne Commit (2023-08-28 bis 2026-04-27) und, fuer die Alt-Commits 2017 bis 2023, 62 Prozent der Commits ausserhalb Mo bis Fr 08 bis 18 Uhr (Ortszeit laut Commit) oder am Wochenende (470 von 758, `git log` mit Auswertung der gespeicherten Zeit; davon 207 am Wochenende). Zeitzone und Arbeitszeit sind dabei Annahmen. Das ist ein schwacher Hinweis, kein Beleg fuer "nie Zeit". Kein Beleg fuer die Ursache der Pause. Die Aussage ist eine Selbstauskunft des Owners; im Post als solche formulieren. |
| "jetzt mit KI" | Gedeckt, aber als Nebeneffekt formuliert | 228 von 275 Commits seit 5.1.0 mit KI-Trailer (83 %), 218 von 262 PRs seit 2026-04-01 vom Owner-Konto mit Claude-Code-Zeile. #52: "AI-assisted development may stay visible as a side effect." Rolle der KI im Prozess (Ticketzuschnitt, Orchestrierung, Modellwahl, Review-Stufen): `docs/agents/implementation.md`. |

Zwei Formulierungs-Risiken fuer den Post, aus den Daten:

- **"Nie Zeit":** Die Daten zeigen Inaktivitaet, nicht Gruende. Eine Formulierung wie "lag jahrelang brach" ist gedeckt, "weil keine Zeit war" ist Selbstauskunft.
- **KI-Rolle:** Die Zahl 228 von 275 sagt "KI war am Commit beteiligt". Sie sagt nicht "KI hat den Code geschrieben, der Owner nicht". Der Owner hat laut Prozess Entscheidungen (Grilling-Tickets, `ready-for-human`, 16 Tickets) und Freigaben getroffen; der Anteil ist nicht messbar. Eine Prozent-Aussage zum KI-Anteil am Code ist **nicht belegbar**.

## 7. Luecken, nicht ermittelbar oder nicht geprueft

- **Owner-Arbeitszeit** in der 6.0.0-Phase: nicht ermittelbar. Weder Git noch GitHub liefern Zeit am Rechner, Zeit im Dialog oder Review-Zeit. Die PR-Laufzeit (Median 5,1 Minuten) ist Auto-Merge-Zeit, keine Arbeit.
- **KI-Rechenzeit, Tokens, Kosten:** nicht ermittelbar (kein Zugriff auf Nutzungsdaten der Sitzungen). Die Trailer nennen eine Sitzungs-URL, die ich nicht geoeffnet habe.
- **Tatsaechlich ausgefuehrte Tests:** nicht ermittelbar ohne `./mvnw verify` (kein Build in dieser Recherche). Zahlen oben sind statische Annotationen.
- **JaCoCo- und PIT-Istwerte:** nicht ermittelt; nur die Schwellen aus der `pom.xml`. `scripts/ci/pit-scores.py` und der Mutations-Build (laut CLAUDE.md etwa 10 Minuten, JDK 21+) wuerden sie liefern.
- **Zeilenanteil KI/Mensch:** nicht ausgewertet; nur Churn pro Commit-Gruppe, mit Mehrfachzaehlung (Abschnitt 3.4).
- **Modellverteilung** (Haiku/Sonnet/Opus je Ticket): `docs/agents/implementation.md` verlangt auf jedem Ticket einen Abschlusskommentar "Model: <haiku|sonnet|opus>, attempts: <n>"; ich habe diese Kommentare nicht ausgewertet. Die Trailer nennen das Modell nur in 26 von 225 Faellen.
- **Zeitzone der 2026-Commits:** Squash-Merge-Commits auf GitHub tragen oft UTC; eine Auswertung "abends/Wochenende" fuer 2026 waere deshalb unzuverlaessig und ist nicht angegeben (nur fuer 2017 bis 2023 mit Vorbehalt).
- **Kommentare der Tickets #53 bis #99:** nicht im Einzelnen gelesen; Motivationszitate stammen aus Notes/Destination von #52, Problem Statement von #104 und Kontextzeilen von #74, #83, #503. Eine Volltextsuche ueber alle Kommentare (`issues/comments`) habe ich nicht gemacht; die Issue-Suche deckt Titel und Body, nicht zwingend alle Kommentare.
- **Eigene Zeilenschaetzer:** die Tempo-Annahmen in Abschnitt 5 sind nicht an Literatur gespiegelt (kein Webzugriff genutzt). Wer eine Zahl im Post nennt, sollte sie als Eigenschaetzung kennzeichnen.
- **Aeltere Recherche korrigieren:** `docs/research/nutzung-und-verbreitung.md` nennt "0 Releases", "keine 6.x auf Central" und "910 marmer / 17 claude". Die ersten beiden Aussagen sind seit 2026-11-15 ueberholt (Release `6.0.0` auf GitHub und Central); die dritte ist durch Abschnitt 3 dieser Datei zu ergaenzen. Diese Datei habe ich nicht veraendert.
- Die Zaehlung der Maptypen (research, grilling, prototype, task) fuer genau die 24 Karten habe ich nicht einzeln nachgezaehlt (siehe Hinweis in 2.1).

## Quellen und Befehle (Auswahl, alle am 2026-11-15)

- `git rev-parse --is-shallow-repository` (false); `git fetch origin`; `git rev-list --count origin/main` (1033).
- `git log origin/main --format=%ad --date=format:%Y | sort | uniq -c` (Jahre); `... %Y-%m` (Monate); `... --date=short | sort -u` (aktive Tage, Luecken per Skript).
- `git tag`; `git for-each-ref refs/tags --format='%(refname:short) %(creatordate:short)'`.
- `git diff --shortstat 5.1.0 origin/main`; `git diff --shortstat 5.1.0 6.0.0`.
- Zeilenzaehlung nach Art und Modul: eigenes Python-Skript ueber `git ls-tree -r` und `git cat-file --batch` (Scratchpad, nicht im Repo), aufgerufen mit `python3 -I <skript> 5.1.0` und `python3 -I <skript> origin/main x`.
- Testannotationen: `git grep -h -E '^\s*@(Test|ParameterizedTest|RepeatedTest|TestFactory|Property|FuzzTest)\b' <rev> -- '*.java' '*.kt'`.
- Trailer: `git log 5.1.0..origin/main -i -E --grep='^Co-Authored-By:.*Claude|^Claude-Session:' --format=%h | wc -l` (228); Autoren dieser Menge mit `--format=%an`; Trailer-Werte mit `--format='%(trailers:key=Co-Authored-By,valueonly)'`.
- Autoren: `git log origin/main --format='%an' | sort | uniq -c`; `... '%an <%ae>'`.
- GitHub (REST): `gh api repos/marmer/hamcrest-matcher-generator/issues/52`, `.../issues/104`, `.../issues/503`, `.../issues/52/comments`, `.../releases`, `.../contributors`, `.../pulls?state=all&per_page=100&page=N` (N = 1 bis 7), `.../issues?state=all&per_page=100&page=N` (N = 1 bis 8, ohne Eintraege mit `pull_request`), `.../actions/runs?per_page=100`, `search/issues?q=<begriff>+is:issue+repo:marmer/hamcrest-matcher-generator`.
- Central: `https://repo1.maven.org/maven2/io/github/marmer/testutils/hamcrest-matcher-generator/maven-metadata.xml`.
- Repo-Dateien auf `origin/main`: `pom.xml` (Module, JaCoCo, PIT), `CHANGELOG.md`, `MAINTAINING.md`, `docs/agents/implementation.md`, `docs/src/docs/asciidoc/architecture/_chapters/11_technical_risks.adoc`, `docs/research/nutzung-und-verbreitung.md`, `docs/research/linkedin-post-sichtbarkeit.md`.

## Methodik und Grenzen

- Alle Zahlen kommen aus Git, `gh api` (REST) und einem `curl` auf Central; es wurde kein Build gestartet und nichts im Repo ausser dieser Datei geaendert. Nicht committet, nicht gepusht, keine Issues oder Kommentare geschrieben.
- `gh api --paginate` ist hinter dem Cloud-Proxy blockiert (siehe `docs/research/gh-paginate-proxy-403.md`); alle Listen wurden ueber einzelne `page=N`-Abrufe geholt (PRs 7 Seiten, Issues 8 Seiten). Die Summen sind ueber diese Seiten gebildet; die Zahlen 278 PRs und 229 Issues (alle Zeiten) stimmen mit den Ticketnummern bis #507 ueberein, wobei einzelne Nummern Issues, PRs oder fehlende Eintraege sein koennen.
- Zeitzonen: Commit-Zeiten sind Autor-Zeiten mit gespeicherter Zeitzone, GitHub-Zeiten sind UTC.
- Die Aufwandszahlen in Abschnitt 5 sind Schaetzungen mit frei gewaehlten Annahmen. Sie sind kein Beleg fuer einen Faktor und sollten im Post, wenn ueberhaupt, als "grobe Eigenschaetzung" erscheinen, mit der gemessenen Wallclock-Dauer (23 Tage 12 Stunden 55 Minuten) daneben, nicht dagegen gerechnet.
