# `gh api --paginate` scheitert in Cloud-Sessions mit 403 (Numeric-ID-Pfade)

Stand: 2026-11-14, `gh` 2.89.0, Cloud-Session mit Agent-Proxy. Recherche ohne Aenderung am Repo ausser dieser Datei.

Kennzeichnung: **(b)** lokal empirisch verifiziert, **(a)** in Doku/Quelltext belegt, **Annahme**, **nicht verifiziert**.

## Frage

`.claude/skills/goRalph/scripts/summary.py` liest `repos/marmer/hamcrest-matcher-generator/pulls?state=closed&sort=updated&direction=desc&per_page=100` mit `gh api --paginate --slurp`. In Cloud-Sessions endet das mit `gh: Numeric-ID repository paths (repositories/{id}/...) are not supported through this proxy. Use repos/{owner}/{repo}/... endpoints instead. (HTTP 403)`. Warum, und was ist der robusteste Fix?

## Kurzantwort

1. Die Hypothese stimmt (b, a): GitHub liefert den `Link: rel="next"`-Header mit der Numeric-ID-Form `https://api.github.com/repositories/94122176/pulls?...`; `gh --paginate` ruft genau diese URL fuer Seite 2 auf; der Proxy lehnt `repositories/{id}/...` mit 403 ab. Seite 1 (`repos/{owner}/{repo}/...`) und ein manuelles `&page=2` ueber `repos/{owner}/{repo}/...` gehen durch.
2. Betroffen ist jede Liste mit mehr als einer Seite (hier: 262 geschlossene PRs, bei `per_page=100` 3 Seiten). Kurze Listen laufen, deshalb faellt es erst bei wachsendem Repo auf.
3. Empfehlung: eigene `page=N`-Schleife (Option a), zentral in `ghapi.gh_list` und `loop.sh gh_pages`. Funktioniert mit und ohne Proxy, braucht keine Rewrite-Logik, ist mit dem bestehenden Stub testbar.

## Befunde

### B1. Link-Header nutzt die Numeric-ID-Form (b, a)

- Doku (a, https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api): Beispiel-Header `link: <https://api.github.com/repositories/1300192/issues?page=2>; rel="prev", ... rel="next" ...`. Die Doku selbst zeigt also die `repositories/{id}`-Form.
- Experiment (b): `gh api -i "repos/marmer/hamcrest-matcher-generator/pulls?state=closed&per_page=1"` ->
  `HTTP/1.1 200 OK`, `Link: <https://api.github.com/repositories/94122176/pulls?state=closed&per_page=1&page=2>; rel="next", <https://api.github.com/repositories/94122176/pulls?state=closed&per_page=1&page=262>; rel="last"`.
- Mit `per_page=100` ebenfalls (b): `rel="next"` -> `.../repositories/94122176/pulls?state=closed&per_page=100&page=2`, `rel="last"` -> `page=3`.
- Gleiches gilt fuer `issues` (b): `Link: <https://api.github.com/repositories/94122176/issues?state=all&since=...&per_page=1&after=...&page=2>; rel="next"`. Also betrifft es jeden Listen-Endpoint des Repos, nicht nur `pulls`.

### B2. gh folgt dem Link-Header woertlich (a)

Quelltext https://raw.githubusercontent.com/cli/cli/v2.89.0/pkg/cmd/api/pagination.go: `findNextPage` liest per Regex `<([^>]+)>;\s*rel="([^"]+)"` die URL mit `rel="next"`; `pkg/cmd/api/api.go` (Zeile ca. 424) setzt `requestPath, hasNextPage = findNextPage(resp)` und fragt damit die naechste Seite an. gh schreibt die URL nicht um.

### B3. Proxy-Antwort (b)

- `gh api --paginate --slurp "repos/marmer/hamcrest-matcher-generator/pulls?state=closed&per_page=1"` -> rc 1, stderr `gh: Numeric-ID repository paths (repositories/{id}/...) are not supported through this proxy. ... (HTTP 403)`; die erste Seite war schon ausgegeben (Vorsicht: bei `--paginate` ohne `--slurp` kommt Teilausgabe plus Fehler).
- Direkter Test `gh api -i "repositories/1/pulls?per_page=1"` -> `HTTP/1.1 403 Forbidden`, Body: `{"message":"Numeric-ID repository paths (repositories/{id}/...) are not supported through this proxy. Use repos/{owner}/{repo}/... endpoints instead.","documentation_url":"https://docs.anthropic.com/en/docs/claude-code/github-actions"}`. Das ist eine Proxy-Regel, kein GitHub-Fehler.
- `/root/.ccr/README.md`: 403 vom Proxy bedeutet Policy-Ablehnung, nicht umgehen/wiederholen. `curl -sS "$HTTPS_PROXY/__agentproxy/status"`: `enabled: true`, `recentRelayFailures: []` (Policy-Denial erscheint dort nicht als Relay-Fehler). Der Proxy ist hier die Policy; ein Rewrite auf Proxy-Seite liegt nicht in unserer Hand (Annahme).

### B4. Manuelle Seitenabfrage ueber `repos/{owner}/{repo}` geht durch (b)

`gh api "repos/marmer/hamcrest-matcher-generator/pulls?state=closed&sort=updated&direction=desc&per_page=100&page=N" --jq length` -> N=1: 100, N=2: 100, N=3: 62, N=27 (weit dahinter): 0. Ausserdem `gh api -i ...&per_page=1&page=2` -> `HTTP/1.1 200 OK`. Eine Seite kuerzer als `per_page` (oder leer) markiert das Ende zuverlaessig. Rate-Limit-Header: `X-Ratelimit-Limit: 15000`, `Remaining: 14998` (core).

### B5. Search-API-Links sind proxy-tauglich (b)

`gh api -i "search/issues?q=repo:marmer/hamcrest-matcher-generator+is:pr+is:merged+merged:>=2026-11-01&per_page=1"` -> `Link: <https://api.github.com/search/issues?q=...&page=2>; rel="next"` (kein `repositories/`), `X-Ratelimit-Resource: search`, `X-Ratelimit-Limit: 30` (pro Minute), `total_count` 95. Search deckt max. 1000 Treffer ab (Annahme, aus GitHub-Doku zur Search-API in Erinnerung, nicht in dieser Session geprueft; nicht verifiziert).

### B6. Betroffene Aufrufstellen im Repo (b, per `grep -rn paginate`)

| Stelle | Aufruf |
|---|---|
| `.claude/skills/goRalph/scripts/ghapi.py` Z. 36 | `gh_list(path)` = `gh("--paginate", "--slurp", path)`, flacht die Seiten zu einer Liste. Einziger Pagination-Punkt der Python-Scripts |
| `.claude/skills/goRalph/scripts/summary.py` Z. 40-42, 46 | `gh_list` fuer `pulls?state=closed&sort=updated&direction=desc&per_page=100` (Fehlerquelle), `issues/{spec}/sub_issues`, `issues?labels=retro&state=all&since=...`, pro Ticket `issues/{n}/comments?since=...` |
| `.claude/skills/goRalph/scripts/interview.py` Z. 49, 54, 56 | `gh_list` fuer `sub_issues` und zwei `issues?labels=...`-Listen |
| `.claude/skills/goRalph/scripts/loop.sh` Z. 216 | `gh_pages() { gh_call --paginate --slurp "$1"; }`, genutzt in Z. 238 (sub_issues), 240 (`pulls?state=open`), 248 (check-runs), 261 (PR-Kommentare), 266 (Reviews) |
| Doku/Beispiele | `docs/agents/issue-tracker.md` Z. 44, `.agents/skills/setup-matt-pocock-skills/issue-tracker-github.md` Z. 24, `docs/agents/best-practices-passing.md` Z. 45, `docs/agents/ralph-loop.md` Z. 70 beschreiben `--paginate` (Beschreibung bzw. Beispiel, nicht ausgefuehrt) |

Tests: `scripts/agents/test_go_ralph_summary.py` (Stub-`gh`, Z. 29 `*--paginate*--slurp*) cat ...` liefert alle Seiten, sonst nur `.[0]`), analog `test_go_ralph_interview.py` und `test_go_ralph_loop.py`. Die Stubs matchen auf `--paginate --slurp` und ignorieren `page=`.

Check-runs liefert ein Objekt (`{"check_runs": [...]}`) statt eines Arrays; die `--slurp`-Behandlung dort muss beim Umbau mit bedacht werden (Annahme, `loop.sh` Z. 248 ff. nicht im Detail gelesen).

## Optionen im Vergleich

| Option | Proxy (Cloud) | normales gh | API-Calls | Rate Limit | Einfachheit | Testbarkeit |
|---|---|---|---|---|---|---|
| (a) Eigene Schleife `page=N` ueber `repos/{owner}/{repo}/...` bis kurze/leere Seite | ja (B4) | ja | Seiten + 0 (bei Ende mit voller letzter Seite 1 Leerabruf) | core 15000/h, unkritisch | gering: ca. 8 Zeilen in `ghapi.gh_list`, ca. 8 in `loop.sh` | gut: Stub muss `page=` auswerten (kleine Anpassung) |
| (b) Link-URL umschreiben (`repositories/<id>` -> `repos/<owner>/<repo>`) | nur per eigener Schleife, `gh --paginate` kann nicht umschreiben (B2) | ja | wie (a) | wie (a) | schlechter als (a): gleiche Schleife plus Header-Parsing (`gh api -i`), Regex auf Format, das GitHub aendern darf | schlechter (Header im Stub) |
| (c1) Begrenzte Abfrage: `pulls?state=closed&sort=updated&direction=desc&per_page=100` nur Seite 1 | ja | ja | 1 | minimal | trivial | trivial, aber **falsch** sobald in der Run-Zeit mehr als 100 PRs aktualisiert wurden; `pulls` hat kein `since` (Annahme: Doku nicht geprueft). Frueher Abbruch nach Fenster waere wieder Schleife = (a) |
| (c2) Search-API `search/issues?q=repo:... is:pr is:merged merged:>=<since>` | ja (B5) | ja | wenige (nur Treffer im Fenster) | **separates Limit 30/min** (B5), max 1000 Treffer | mittel: anderes Antwortformat (`items`, `pull_request`-Unterobjekt, `merged_at` evtl. nur im `pull_request`-Feld), eigener Sonderfall nur fuer summary.py, loest loop.sh/interview.py nicht | mittel, eigene Fixtures |
| (d) MCP-GitHub-Tool (`mcp__github__list_pull_requests`) | nicht verifiziert; gilt nur fuer die Host-Session, nicht fuer Scripts/`claude -p`-Kinder (siehe `docs/research/ralph-loop-in-cloud-sessions.md` 2.1: Kind sieht kein `mcp__github__*`) | nein (nur Cloud) | je Seite 1 | unbekannt | Script kann kein MCP aufrufen | nicht testbar im Script-Stub |

Ausgeschlossen: ein Proxy-Fix (nicht in unserer Hand, Annahme) und `gh api --paginate --hostname`/Env-Tricks (nicht verifiziert, nicht vorhanden in gh-Flagliste, Annahme).

## Empfehlung

Option (a), an den zwei zentralen Stellen:

1. `ghapi.gh_list(path)`: haengt `&page=N` (bzw. `?page=N`, falls kein `?` im Pfad) an, ruft `gh(path_with_page)` ohne `--paginate`/`--slurp`, sammelt Items, bricht ab bei `len(page) < per_page` oder leerer Seite. `per_page` aus dem Pfad lesen (alle Aufrufer setzen `per_page=100`); ohne `per_page` waere der Default 30 -- dann abbrechen nur bei leerer Seite. Harte Obergrenze der Seiten (z. B. 100) als Schutz gegen Endlosschleife, bei Ueberschreitung `RuntimeError` (nie still abschneiden).
2. `loop.sh gh_pages`: gleiche Schleife in Bash, Ausgabe weiter als JSON-Array von Seiten, damit die `jq`-Auswertung ab Z. 238 unveraendert bleibt (`jq -s`/`--slurp`-aequivalentes Zusammensetzen); Sonderfall `check-runs` (Objekt statt Array) beachten.
3. Tests: Stub in `test_go_ralph_summary.py`, `test_go_ralph_interview.py`, `test_go_ralph_loop.py` wertet `page=N` aus (Fixture ist schon eine Seitenliste, `.[N-1]`, leeres Array jenseits). Neuer Test: 2 Seiten (100 + 3 Items) werden komplett gelesen; Fehler auf Seite 2 beendet den Lauf wie bisher (kein falsches Gruen, siehe `docs/agents/ralph-loop.md` Z. 70).
4. Die Doku-Beispiele mit `--paginate` bleiben fuer normale Sessions gueltig; in Cloud-Sessions auf `page=N` verweisen.

Begruendung: einzige Option, die in beiden Umgebungen identisch laeuft, ohne Wissen ueber Repo-IDs oder Link-Format, mit der geringsten Aenderung und ohne neues Rate-Limit-Regime.

## Annahmen und offene Punkte

- Annahme: Der Proxy-Block ist beabsichtigte Policy und bleibt; wir koennen ihn nicht aendern.
- Annahme: Die Schleife mit `page=N` funktioniert fuer alle sechs verwendeten Endpoints (`pulls`, `issues`, `sub_issues`, `comments`, `check-runs`, `reviews`). Getestet (b) nur `pulls` und `issues` (nur Link-Header). `sub_issues`, `comments`, `check-runs`, `reviews` nicht verifiziert.
- Annahme: `issues?...` kann cursor-basiert (`after=`) paginieren (B1 zeigt `after=...&page=2`); `page=N` wurde dort nicht bis zum Ende getestet, nicht verifiziert.
- Nicht verifiziert: Search-API-Limit 1000 Treffer; `pulls` ohne `since`-Parameter; ob `mcp__github__*` aus Scripts erreichbar ist (spricht dagegen: Notiz in `ralph-loop-in-cloud-sessions.md`).
- Offen: Der Check in `loop.sh` auf `check-runs` (Objekt-Antwort) und die genaue `--slurp`-Nachbearbeitung wurden nicht im Detail gelesen.
- Offen: `gh api -i` einer Seite mit rel="next" auf Nicht-Cloud-gh wurde nicht gegengeprueft (Annahme: dort gleiches Verhalten, Link-Header ist serverseitig).
