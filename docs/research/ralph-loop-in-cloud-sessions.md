# Ralph-Loop in Claude-Code-Cloud-Sessions und Skill-Dateien

Stand: 2026-11-12, `claude` 2.1.322 (Cloud-Session, Container, Ubuntu). Recherche ohne Aenderung am Repo ausser dieser Datei.

Kennzeichnung: **(a)** in Doku belegt, **(b)** lokal empirisch verifiziert, **(c)** Annahme/ungeprueft, **unbekannt** = nicht herauszufinden.

## 1. Kurzfazit

1. **Ja, ein Shell-Script-Ralph-Loop ist in dieser Cloud-Session machbar (b).** `claude -p` laeuft im Container, nutzt die OAuth-Anmeldung der Session (`claude auth status`: `authMethod: oauth_token`), hat Netz (ueber den Proxy), `git ls-remote`, `gh api` und `git push --dry-run` funktionieren. Eine 2-Iterationen-Schleife mit frischer Instanz pro Iteration und Abbruch ueber Datei lief in 20 s (Haiku). Die Hauptsession muss nur das Script starten (`Bash run_in_background`) und per Statusdatei/`Monitor` beobachten; sie kommuniziert nicht mit den Agenten.
2. **Pflicht-Bedingung in DIESEM Repo (b):** Der `SessionStart`-Hook `.claude/hooks/session-start.sh` laeuft in **jedem** `claude -p`-Kind (CLAUDE_CODE_REMOTE=true wird vererbt) und fuehrt ein volles `./mvnw install` aus: **83 s** Startzeit pro Iteration, plus Race auf `~/.m2` und `/tmp/session-start.log` bei parallelen Kindern. Im Loop **immer** `--settings '{"disableAllHooks":true}'` setzen (Start dann ~4,5 s). CLAUDE.md und Skills des Repos werden dabei trotzdem geladen (b).
3. **Nicht benutzen:** `--bare` (b: "Authentication error", liest kein OAuth, braucht `ANTHROPIC_API_KEY`, den es hier nicht gibt) und `--setting-sources user` (b: laedt dann weder CLAUDE.md noch Repo-Skills). `claude --bg`/`claude agents` scheitern mit "Workspace not trusted" (b), solange `hasTrustDialogAccepted` fehlt.
4. **Die MCP-Tools `mcp__claude-code-remote__*` sind Modell-Tools der Host-Session.** Aus einem Script heraus erreichbar? **Unbekannt**; das `-p`-Kind sah sie nicht (b), und ich fand keine dokumentierte Script-Schnittstelle (c: Script-Weg waere die Anthropic-API/`claude --cloud <id>`, ungeprueft). Dokumentiert (a) ist `claude -p "msg" --cloud <session-id>` (Nachricht in bestehende Cloud-Session einreihen), `claude --cloud "Aufgabe"` (neue Cloud-Session) erfordert aber `claude auth login` mit claude.ai-Konto, in diesem Container ungetestet.
5. **Skills:** Neben `SKILL.md` duerfen beliebige Dateien liegen (reference.md, examples.md, `scripts/`, Templates, Assets); sie werden on demand gelesen, Scripts ausgefuehrt, nicht geladen (a). Pfade ueber `${CLAUDE_SKILL_DIR}`.

## 2. Befunde Frage 1: Script-gesteuerte Agenten

### 2.1 Umgebung (b)

- `claude --version` -> `2.1.322 (Claude Code)`; Binary `/opt/node22/bin/claude`.
- Auth: `claude auth status` -> `loggedIn: true`, `authMethod: oauth_token`, `apiProvider: firstParty`. `ANTHROPIC_API_KEY`/`ANTHROPIC_AUTH_TOKEN` sind **nicht** gesetzt; `ANTHROPIC_BASE_URL` zeigt auf api.anthropic.com. Das Kind erbt die Credentials der Session ohne Zutun.
- Env der Session (nur Namen/unkritische Werte geprueft): `CLAUDE_CODE_REMOTE=true`, `CLAUDE_CODE_CHILD_SESSION=1`, `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH=1`, `CLAUDE_CODE_SESSION_ATTENDED=1`, `GH_TOKEN`/`GITHUB_TOKEN` gesetzt (Werte nicht ausgegeben). Hinweis: diese Session selbst ist ein Kind (`CHILD_SESSION=1`); ob die Haupt-Session identisch konfiguriert ist: unbekannt.
- Proxy: `/root/.ccr/README.md` (Zeilen 1 ff.): HTTPS laeuft ueber lokalen Proxy 127.0.0.1:46435; 403/407 = Host nicht erlaubt, nicht umgehen.
- Git: `git remote -v` -> `https://github.com/marmer/hamcrest-matcher-generator`; `git ls-remote --heads origin` ok; `gh api repos/marmer/hamcrest-matcher-generator --jq .full_name` ok (beides aus einem `claude -p`-Kind heraus, b). `git push --dry-run origin HEAD:refs/heads/research-probe-dryrun` -> `* [new branch] HEAD -> research-probe-dryrun`. Ein **echter Push** wurde nicht gemacht (Auftrag); dass der Dry-Run den Receive-Pack-Handshake bis zur Auth ausfuehrt, ist (c).
- Doku (a, code.claude.com/docs/en/claude-code-on-the-web): GitHub-Credentials bleiben ausserhalb der VM, ein GitHub-Proxy haengt sie serverseitig an; Doku Cloud-Environments (Zeile 258 der gespeicherten Seite): "the proxy rejects branch deletions and pushes of anything other than a branch, such as a tag. It doesn't limit which branches a push can update" (Schutz ueber GitHub-Branch-Protection). Repo-Regel (`docs/agents/implementation.md`): Owner genehmigt Push von `ticket-<n>`-, Retro- und Session-End-Branches.
- MCP im Kind (b): `system/init` des Kindes listet nur Plugin-Server (`plugin:pdf-viewer:pdf` connected; Snowflake/Databricks/Definite/Gmail/Calendar failed; viele needs-auth). **Kein** `mcp__github__*`, **kein** `mcp__claude-code-remote__*` (Haiku bestaetigte "nein"). Das Kind hat dafuer `gh`/`git` per Bash.
- Tools des Kindes (b): u.a. `Task`, `Bash`, `Edit`, `Monitor`, `CronCreate`, `ListAgents`, `SendMessage` (deferred), `ScheduleWakeup`. Agenttypen: u.a. `implementer` (aus `.claude/agents/implementer.md`), `general-purpose`, `Explore`, `Plan`. 119 Skills sichtbar, darunter `implement`, `implement-spec`, `limit-loop`, `claude-handoff`, `retro` (b, init-Event; fuer `disable-model-invocation`-Skills heisst "sichtbar" nur im Init, das Modell nannte `limit-loop` in seiner Skill-Liste nicht, da die Beschreibung aus dem Modellkontext entfernt ist).
- Hinweis `-p` und Trust (b + a): `-p` ueberspringt den Trust-Dialog; stderr: "Ignoring 9 permissions.allow entries from .claude/settings.local.json: this workspace has not been trusted". Die Repo-`allow`-Regeln aus `settings.local.json` greifen im Kind also **nicht**; `.claude/settings.json` (Hook) dagegen schon. Doku bestaetigt: "Without `--bare`, a `-p` session runs the hooks in a project's `.claude/settings.json` ... even in a folder you've never trusted" (headless).

### 2.2 Relevante CLI-Flags (`claude --help`, b; Semantik aus Doku a)

| Flag | Zweck im Loop |
|---|---|
| `-p/--print` | nicht-interaktiv; Exit 0 bei Erfolg, nonzero bei Fehlern (a) |
| `--output-format json` / `stream-json` (+`--verbose`) | `json`: ein Objekt mit `result`, `session_id`, `num_turns`, `total_cost_usd`, `permission_denials`, `terminal_reason` (b). `stream-json`: Live-Events, `system/init` mit Tools/Skills/MCP-Status (b) |
| `--json-schema` | strukturierte Antwort in `structured_output` (a; nicht getestet) |
| `--max-turns N` | begrenzt Agentenschleife pro Aufruf (b: wirkte; Verhalten bei Ueberschreitung nicht getestet) |
| `--max-budget-usd X` | Kostenobergrenze pro Aufruf (nur mit `-p`) (a, Hilfetext; nicht ausgereizt) |
| `--permission-mode acceptEdits\|dontAsk\|auto\|bypassPermissions\|plan\|manual` | Baseline. In `-p` ohne Modus: Manual, was nicht freigegeben ist wird **verweigert** (a, v2.1.302+) |
| `--allowedTools` | Regeln, z.B. `"Bash(git status *)"`. Verbundene Befehle (`a; echo`) werden einzeln geprueft und gedeniet (b: Haiku-Kind musste `git ls-remote` und `gh api` getrennt aufrufen) |
| `--permission-prompts none` | nichts fragen, alles Unentschiedene sofort ablehnen, Modell wird angewiesen nicht zu wiederholen (a, v2.1.259+) |
| `--dangerously-skip-permissions` / `--permission-mode bypassPermissions` | Hilfetext: "Recommended only for sandboxes with no internet access" (b). Hier nicht getestet |
| `--session-id <uuid>` / `--resume <id>` / `--continue` / `--fork-session` | feste Session-ID und Fortsetzen (b: `--session-id` + `--resume` lieferte den vorher genannten Wert "BANANE"). Ralph-Loop will das Gegenteil: frischer Kontext, also `--no-session-persistence` oder neue IDs |
| `--no-session-persistence` | keine Transkripte auf Platte (b, genutzt) |
| `--append-system-prompt[-file]`, `--system-prompt` | Zusatz-/Ersatz-Systemprompt (a). Bei Ersatz entfaellt der Git-Status-Snapshot |
| `--model haiku\|sonnet\|opus`, `--effort`, `--fallback-model` | Kosten-/Qualitaetssteuerung (b: Haiku genutzt) |
| `--settings <file-or-json>` | zusaetzliche Settings; `{"disableAllHooks":true}` unterdrueckt den SessionStart-Hook (b) |
| `--add-dir`, `--tools`, `--disallowedTools`, `--agents`, `--agent`, `--mcp-config`, `--strict-mcp-config`, `--plugin-dir` | Kontext/Tools eingrenzen (a, Hilfetext) |
| `-w/--worktree` | eigener Git-Worktree je Lauf (Hilfetext, nicht getestet) |
| `--bg`, `claude agents/attach/logs/stop/rm` | Hintergrundsessions; in `-p` abgelehnt (a); ohne `-p` hier "Workspace not trusted" (b) |
| `--cloud [text\|id]` | neue Cloud-Session bzw. mit `-p <msg> --cloud <session-id>` Nachricht in bestehende Session (a; nicht getestet) |

Doku-Details (a, headless): SIGTERM -> Exit 143, Turn unfertig; SIGINT beendet den Turn sauber. `-p` wartet nach dem Turn auf Hintergrundarbeit (Background-Commands, Subagents, Monitor, 10-Min-Cap `CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS`); ein Kind sollte also keine Hintergrundprozesse hinterlassen. `--bare` soll laut Doku kuenftig Default fuer `-p` werden (Risiko fuer das Loop-Script: Flag/Verhalten kann sich aendern; Version pinnen/pruefen).

### 2.3 Empirische Tests (b), alle mit Scratchpad-Dateien

Scratchpad: `/tmp/claude-0/-home-user-hamcrest-matcher-generator/6f53e082-6f32-4498-81b8-7c4e48628a2f/scratchpad/` (`ralph.sh`, `ralph/iter*.json`, `t*.json*`).

| Test | Befehl (gekuerzt) | Ergebnis |
|---|---|---|
| T1 Default | `claude -p "Antworte nur mit hi" --output-format json --max-turns 1 --no-session-persistence` | rc 0, `result: "hi"`, Wanduhr **87 s**, `duration_ms` 2853, Modell `claude-opus-5-5` (Default!), Kosten **0,1857 USD** (Cache-Aufbau 22k Tokens) |
| T2 Haiku, Hook aktiv | wie T1 mit `--model haiku`, stream-json | Wanduhr **97 s**; Debug-Log: `Hooks: SessionStart:startup [command hook] finished with status 0 (83343ms)`, Ausgabe enthaelt Maven-Fehler `jacoco-maven-plugin ... check`; eigentliche Modellarbeit 8,9 s, Kosten 0,0068 USD. CLAUDE.md geladen (Kind zitierte Abschnitt), 119 Skills |
| T3 `--bare` | `claude -p ... --bare` | 0,9 s, `result: "Authentication error"`, `is_error: true` -> unbrauchbar ohne API-Key |
| T4 `--setting-sources user` | | 4,5 s, aber Kind kennt weder CLAUDE.md noch Repo-Skills |
| T5 `--settings '{"disableAllHooks":true}'` | | **4,8 s**; Kind sieht CLAUDE.md (nannte Abschnitt "Architecture: Generation Flow"), Agent/SendMessage-Tools, `git push --dry-run` ok |
| T6 Mini-Ralph-Loop | `ralph.sh` (siehe 5.2), max 4 Iter., Abbruch wenn Datei `DONE` existiert | Iter. 1: 8 s, 3 Turns, 0,0043 USD; Iter. 2: 13 s, 5 Turns, 0,0060 USD, schreibt `DONE`; Iter. 3 startet nicht. **Gesamt 20,7 s.** `state.txt` = 2 Zeilen. |
| T6a Fehlversuch | `--allowedTools "Write(<abs path>/*)"` | `permission_denials` fuellte sich, Datei nicht geschrieben, Loop lief 4x leer (rc 0 trotz Fehlschlag!). Erst `--add-dir <dir>` plus `Write(//<dir>/**)`-Regeln (Absolutpfad-Syntax) funktionierte; welcher der beiden Teile den Ausschlag gab: nicht isoliert. Lehre: **rc 0 heisst nicht Erfolg**, Abbruch/Erfolg nur ueber Artefakte und `permission_denials` pruefen |
| T7 Parallel | 3x `claude -p` gleichzeitig (Haiku, Hooks aus) | alle ok, 4,7 s gesamt; Rate-Limit-Grenze nicht erreicht (zu klein) |
| T8 Resume | `--session-id <uuid>` dann `--resume <uuid>` | Kontext erhalten, 5,2 s |
| T9 Sichtbarkeit | `mcp__claude-code-remote__list_sessions` nach den Kind-Laeufen | keine neuen Sessions in der Liste (die Kind-Laeufe erscheinen nicht als eigene Cloud-Sessions; ob sie in das Transkript der Host-Session fliessen: unbekannt) |

### 2.4 Grenzen

- **Rate-Limits/Kosten (a):** "cloud sessions share rate limits with all other Claude and Claude Code usage within your account. Running multiple tasks in parallel consumes more rate limits proportionately." Gilt auch fuer `claude -p`-Kinder (gleiche OAuth-Identitaet, c). Das Repo kennt 429/"hit your limit" bereits (`limit-loop`). `total_cost_usd` ist eine Client-Schaetzung (a); bei Subscription-Auth zaehlt es Limit, nicht Rechnung (c). **Default-Modell ist Opus** (T1: 0,19 USD fuer "hi"), im Loop immer `--model` setzen.
- **Container-Lebensdauer/Inaktivitaet (a, cloud-environments "Time limits"):** nach "a few minutes without activity" pausiert die VM, spaeter kann sie reclaimed werden; dann gehen Hintergrund-Shell-Kommandos, Subagents und session-scoped Cron verloren, Konversation bleibt. Ob ein laufendes Hintergrund-Script/`claude -p` die VM wach haelt (also als "Aktivitaet" zaehlt): **unbekannt**; sicherer: Hauptsession pollt aktiv (Monitor/Bash-Wait) und das Script ist wiederaufsetzbar (Zustand in Dateien/Git, `--resume`-faehig).
- **Zeitlimits (a):** Bash-Tool wartet default 2 min, max 10 min im Vordergrund, danach Hintergrund (ausser `sleep`-Befehle); SessionStart-Hook-Timeout 600 s; Setup-Script nur bis ~5 min gecacht. `run_in_background`: Default-Timeout 30 min, max 2 h (Tool-Beschreibung der Hauptsession); `Monitor`: max 30 min je Watch, dann re-arm.
- **Parallelitaet:** technisch ok (T7). Repo-spezifisch: `~/.m2`-Race und HTTP 429 von Maven Central bei parallelen Builds (`docs/agents/build-pitfalls.md` Z. 21; Hook-Kommentar), deshalb `./mvnw -o` in Kindern und Hook aus. `docs/agents/implementation.md`: max. 2 parallele Implementer, nur bei disjunkten Dateien; je Ticket eigener Worktree/Branch.
- **Spawn-Tiefe:** `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH=1` (b, Env). Wirkung auf per Script gestartete `claude -p` (neuer Prozess, aber `CLAUDE_CODE_CHILD_SESSION=1` vererbt): unbekannt, im Test lief ein Kind problemlos; Unterkinder des Kindes nicht getestet.
- **MCP aus Script (c/unbekannt):** `create_session`, `send_message`, `list_events`, `get_session` sind Tools der Host-Session; sie sind nicht im `claude mcp list` der CLI und nicht im Kind (b). Ein Script kann sie nicht aufrufen, sofern kein oeffentlicher Endpunkt existiert (nicht gefunden). Fuer "Kind-Sessions" bleibt: Modell ruft `create_session`/`send_message` auf (Modellseitig), nicht Script-gesteuert; dafuer muss die Hauptsession pro Aufruf Tokens aufwenden, genau das, was vermieden werden soll.
- **Git/PR:** Push per `git` (Proxy haengt Credential an, a) und `gh api` (REST) funktionieren; `gh pr`/`gh issue` scheitern laut CLAUDE.md (GraphQL blockiert); PR-Anlage und Auto-Merge laufen im Repo ueber `mcp__github__*` der Hauptsession (`docs/agents/implementation.md`). **Das Kind hat diese MCP-Tools nicht** -> PR-Erstellung per `gh api repos/.../pulls` (REST) waere noetig: (c), nicht getestet.
- **Sicherheit:** Der Loop-Prompt verarbeitet Repo-Inhalte; `-p` ueberspringt Trust-Dialog (a). Kein `--dangerously-skip-permissions` ohne zwingenden Grund; Container ist VM-isoliert (a), hat aber Internet und `GH_TOKEN`.

## 3. Befunde Frage 2: Skills

Quelle (a): https://code.claude.com/docs/en/skills (gelesen bis Zeichen ~100000 plus Rest; der Mittelteil mit Hooks/fork-Details und `chmod`-Hinweisen wurde nicht vollstaendig gelesen).

**Dateien neben SKILL.md (a):** Ein Skill ist ein Verzeichnis; nur `SKILL.md` ist Pflicht. Beispiel der Doku: `reference.md` ("loaded when needed"), `examples.md`, `scripts/helper.py` ("executed, not loaded"). Supporting files laden on demand (progressive disclosure): SKILL.md verlinkt sie, Claude liest bei Bedarf. "Keep SKILL.md under 500 lines". Der Body bleibt nach Aufruf im Kontext; nach Compaction wird nur der Anfang behalten (a, Troubleshooting) -> Wichtigstes nach oben.

**Referenzieren (a):** `${CLAUDE_SKILL_DIR}` = Verzeichnis der SKILL.md, funktioniert unabhaengig vom cwd und auch in `allowed-tools`: `allowed-tools: Bash(${CLAUDE_SKILL_DIR}/scripts/render.sh *)`. Weitere Substitutionen: `$ARGUMENTS`, `$ARGUMENTS[N]`/`$N`, `$name` (mit `arguments:`), `${CLAUDE_SESSION_ID}`, `${CLAUDE_EFFORT}`, `${CLAUDE_PROJECT_DIR}`, in Plugins `${CLAUDE_PLUGIN_ROOT}`/`${CLAUDE_PLUGIN_DATA}`. Ohne Platzhalter wird `ARGUMENTS: <wert>` angehaengt. Inline-Shell: `` !`cmd` `` bzw. Block ```` ```! ````, laeuft **vor** dem Modell, Output ersetzt den Platzhalter; Exit >= 2 bricht den Aufruf ab (`|| true` anhaengen); per `disableSkillShellExecution` abschaltbar; geprueft gegen Permission-Regeln.

**Frontmatter (a):** `name`, `description`, `when_to_use` (zusammen max 1536 Zeichen), `argument-hint`, `arguments`, `disable-model-invocation`, `user-invocable`, `allowed-tools` (nur vorab freigeben, schraenkt nicht ein, gilt fuer den aufrufenden Turn), `disallowed-tools`, `model`, `effort`, `context: fork`, `agent` (Subagent-Typ bei fork, z.B. Explore/Plan/general-purpose), `background` (mit fork, Default true), `hooks` (ab Aufruf bis Sessionende), `paths` (Glob-Aktivierung), `shell` (bash/powershell), `metadata`/`license`/`compatibility` (informativ). Unbekannte Felder werden still ignoriert; Booleans akzeptieren true/false/yes/no/on/off/1/0. Ausserhalb Claude Code (claude.ai-Upload, Skills API) nur `name`, `description`, `license`, `compatibility`, `metadata`, `allowed-tools`; Extra-Keys = harter Fehler. `<`/`>` in description werden als `&lt;`/`&gt;` angezeigt.

**Orte/Prioritaet (a):** Enterprise (managed) > persoenlich `~/.claude/skills/` > Projekt `.claude/skills/<name>/SKILL.md`; verschachtelt `<subdir>/.claude/skills/`; `--add-dir`; Plugin `<plugin>/skills/` als `/plugin:skill`; claude.ai-synchronisierte Skills. In Cloud-Sessions: Repo-`.claude/skills|agents|commands` werden mit dem Clone geladen; `~/.claude/...` nicht (a, cloud-environments Tabelle). Nach Aendern: Listing-Budget 1% des Kontexts (`skillListingBudgetFraction`).

**Ausfuehrbares Bit:** In der gelesenen Doku nicht belegt (Seitenmitte nicht gelesen) -> **unbekannt**. Lokal (b): `.agents/skills/git-guardrails-claude-code/scripts/block-dangerous-git.sh` hat `-rwxr-xr-x`. Sicherer Weg: im Skill `bash ${CLAUDE_SKILL_DIR}/scripts/x.sh` statt direktem Aufruf (c). Hinweis Repo: Skills liegen unter `.agents/skills/<name>/` und `.claude/skills/<name>` ist ein Symlink darauf (b, `ls -la`).

**`-p` und Skills (a):** `claude -p "/skill-name args"` expandiert user-invoked Skills; forked Skills erscheinen im stream-json mit `parent_tool_use_id = forked-command-...`. Ein Ralph-Loop kann daher pro Iteration `claude -p "/implement-ticket 123"` aufrufen; der Skill muss dann ohne Rueckfragen auskommen (`--permission-prompts none`).

## 4. Vorhandene Repo-Skills (b)

Skills liegen in `.agents/skills/*`, `.claude/skills/*` sind Symlinks (nur `brief-me` und `limit-loop` direkt in `.claude/skills/`).

| Skill | Dateien neben SKILL.md | Relevanz |
|---|---|---|
| `limit-loop` (`.claude/skills/limit-loop/SKILL.md`, 25 Z.) | keine | **Kein Ralph-Loop.** Plant per `loop`-Skill einen wiederkehrenden Tick (`CronCreate`), der durch Usage-Limit (429) abgebrochene Agenten per `SendMessage` fortsetzt. `disable-model-invocation: true`. Modell-/Tool-gesteuert, nicht script-gesteuert; 7-Tage-Ablauf, Stop ueber `CronDelete` |
| `loop-me` | `agents/openai.yaml` | Spec-Grilling fuer Workflows ("loop" = Lebensmuster), nichts mit Ralph |
| `implement`, `implement-spec` | `agents/openai.yaml` | Orchestrator im Modell: Subagents (`Task`) im Hintergrund, Frontier aus Ticket-Graph, ein PR je Ticket (`docs/agents/implementation.md`). Kommunikation laeuft ueber das Modell, genau das, was ein Ralph-Script ersetzen wuerde |
| `claude-handoff` | `agents/openai.yaml` | Startet `claude --bg --name ... -- "$(cat file)"`; in dieser Cloud-Session scheitert `--bg` an "Workspace not trusted" (b). Ob es in anderen Cloud-Sessions geht: unbekannt |
| `writing-for-agents` | `SKILL-MECHANICS.md`, `agents/openai.yaml` | Erklaert Invocation-Wahl (model- vs. user-invoked), Router-Skills; Regeln fuer neue Skills |
| `setup-matt-pocock-skills` | `domain.md`, `issue-tracker-{github,gitlab,local}.md`, `triage-labels.md`, `agents/openai.yaml` | Beispiel fuer Referenzdateien im Skillordner |
| `git-guardrails-claude-code` | `scripts/block-dangerous-git.sh` (+x), `agents/openai.yaml` | Beispiel fuer Script im Skill + Hook |

`agents/openai.yaml` (z.B. `interface.display_name`, `policy.allow_implicit_invocation: false`) ist fuer Claude Code irrelevant (c; gehoert zu anderem Tooling).

Weitere Repo-Dateien: `.claude/settings.json` hat nur den `SessionStart`-Hook; `.claude/settings.local.json` Allow-Regeln (greifen in `-p` nicht, b); `.claude/agents/implementer.md` (Tools Bash, Read, Edit, Write, Glob, Grep, Skill; Brief `docs/agents/implementer-brief.md`); `scripts/agents/` (`verify.sh`, `issue.py`, `issue-status.py`, `workflow_jobs.py`, ...). Eine Ralph-artige Script-Mechanik existiert im Repo **nicht** (Grep nach `claude -p`/`ralph` in `.claude`, `.agents`, `docs/agents` nicht gefunden: nur `limit-loop`/`claude-handoff`; Grep nicht vollstaendig ueber alle Dateien gelaufen, daher c).

## 5. Empfohlene Architektur fuer einen Ralph-Loop-Skill

### 5.1 Dateilayout

```
.agents/skills/ralph-loop/            (+ Symlink .claude/skills/ralph-loop)
  SKILL.md              user-invoked (disable-model-invocation: true), kurz: Start, Monitor, Abbruch
  scripts/ralph.sh      die Schleife (chmod +x; Aufruf via `bash ${CLAUDE_SKILL_DIR}/scripts/ralph.sh`)
  prompt.md             der feste Iterations-Prompt (Vorlage mit {{TICKET}}, {{DONE_FILE}})
  references/loop-contract.md   Statusdatei-Format, Abbruchregeln, Exit-Codes
allowed-tools: Bash(bash ${CLAUDE_SKILL_DIR}/scripts/ralph.sh *), Monitor
```

Zustand ausserhalb des Repos (Scratchpad oder `/tmp/ralph/<run>/`): `status.json`, `iter-N.json`, `iter-N.err`, `DONE`/`FAILED`, `progress.md` (Gedaechtnis zwischen Iterationen, vom Agenten gepflegt); echtes Gedaechtnis ist Git (Branch `ticket-<n>`, Commits).

### 5.2 Script-Skizze (getestet im Kern, siehe T6; Erweiterungen ungetestet)

```bash
#!/usr/bin/env bash
set -u
RUN=${RUN_DIR:?}; MAX=${MAX_ITER:-10}; MODEL=${MODEL:-haiku}; BUDGET=${ITER_BUDGET_USD:-1.00}
TOTAL_CAP=${TOTAL_CAP_USD:-10}; PROMPT_FILE=${PROMPT_FILE:?}
cd "${WORKDIR:?}"; total=0; fails=0
for i in $(seq 1 "$MAX"); do
  [ -e "$RUN/STOP" ] && { echo "stopped"; break; }       # manueller Not-Aus
  [ -e "$RUN/DONE" ] && { echo "done"; break; }
  claude -p "$(cat "$PROMPT_FILE")" \
    --settings '{"disableAllHooks":true}' --model "$MODEL" \
    --output-format json --max-turns 40 --max-budget-usd "$BUDGET" \
    --permission-mode acceptEdits --permission-prompts none \
    --allowedTools "Bash(git *)" "Bash(./mvnw -o *)" "Read" "Edit" "Write" \
    --no-session-persistence < /dev/null > "$RUN/iter-$i.json" 2> "$RUN/iter-$i.err"
  rc=$?
  cost=$(jq -r '.total_cost_usd // 0' "$RUN/iter-$i.json" 2>/dev/null || echo 0)
  total=$(echo "$total + $cost" | bc -l)
  jq -c --argjson i "$i" '{iter:$i,rc:'"$rc"',turns:.num_turns,cost:.total_cost_usd,denials:(.permission_denials|length),err:.is_error}' \
     "$RUN/iter-$i.json" >> "$RUN/status.jsonl" 2>/dev/null
  # Fehlerbehandlung: Rate-Limit/Auth -> Backoff oder Abbruch; sonst nach 3 Fehlern in Folge abbrechen
  if [ $rc -ne 0 ] || jq -e '.is_error==true' "$RUN/iter-$i.json" >/dev/null 2>&1; then
    fails=$((fails+1)); [ $fails -ge 3 ] && { touch "$RUN/FAILED"; break; }; sleep $((60*fails)); else fails=0; fi
  [ "$(echo "$total > $TOTAL_CAP" | bc -l)" = 1 ] && { touch "$RUN/FAILED"; echo "cost cap"; break; }
done
```

Wichtig: `< /dev/null` (sonst 3-s-Warnung "no stdin data"), Hooks aus, Modell setzen, Erfolg an **Artefakten** messen (Datei `DONE` nur, wenn der Agent die Akzeptanzkriterien per Test belegt hat; besser: das Script prueft selbst, z.B. `./mvnw -o -q verify` oder `git log origin/ticket-N..`).

### 5.3 Ueberwachung durch die Hauptsession

- Start: `Bash` mit `run_in_background: true`, `nohup`-frei (ein Prozess, dessen Ende die Session benachrichtigt; Timeout bis 2 h).
- Beobachten ohne Agentenkommunikation: `Monitor` mit `tail -f $RUN/status.jsonl | grep --line-buffered -E 'iter|FAILED|DONE'` (max. 30 min je Watch, dann re-arm; Filter muss auch Fehler/Abbruch erfassen, "Stille ist kein Erfolg").
- Kostet die Hauptsession pro Ereignis Tokens; sparsame Ereignisse (eine Zeile je Iteration).
- Fallback bei Container-Pause/Neustart: Script idempotent (liest `progress.md`, Git-Stand), `RUN_DIR` in dauerhaftem Pfad, Neustart durch die Hauptsession.

### 5.4 Abbruchkriterien und Limits

1. `DONE`-Datei (Erfolg, vom Script verifiziert), 2. `MAX_ITER`, 3. Gesamtkostencap `TOTAL_CAP_USD` plus `--max-budget-usd` je Iteration, 4. `STOP`-Datei als Not-Aus, 5. drei Fehl-Iterationen in Folge, 6. 429/"hit your limit": nicht eng pollen; schlafen bis zum Reset oder abbrechen und `limit-loop` greifen lassen, 7. Wall-Clock-Cap.
Sicherheit: Hooks aus nur fuer den Loop; `--allowedTools` eng (kein bares `Bash`); kein `--dangerously-skip-permissions`; Pushes nur auf `ticket-<n>`, nie `main`; Kind bekommt keine Secrets im Prompt; Prompt aus Repo-Datei, nicht aus Issue-Text (Prompt-Injection, c).

### 5.5 Alternativen

- **Modell-gesteuert** (`Task`-Subagents / `mcp__claude-code-remote__create_session`): Kommunikation und Tokens laufen durch die Hauptsession; nur das ist heute der etablierte Repo-Weg (`/implement-spec`).
- **`claude -p --cloud <id>`** (a): Nachricht in bestehende Cloud-Session; braucht claude.ai-Login im Container und kann nicht auf Antwort warten; ungetestet.
- **Cloud Scheduled Tasks / Routines** (a, Doku `routines`): Cron-/Event-gesteuerte Cloud-Sessions; nicht untersucht.

## 6. Offene Fragen / Annahmen

- Haelt ein laufendes Hintergrund-Script die VM wach (Idle-Pause "after a few minutes without activity")? **unbekannt**; Test: Script mit `sleep 600` laufen lassen und danach pruefen.
- Gehen `claude -p`-Kinder in das Usage-/Rate-Limit-Konto der Host-Session ein und treffen 429 dieselben Limits? (a: ja, geteilte Limits; im Test nicht provoziert.)
- Ob echter `git push` aus dem Kind geht (nur Dry-Run); Proxy-/Branch-Regeln des Environments; ob `gh api .../pulls` (REST) PRs anlegen darf.
- Wirkung von `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH=1` und `CLAUDE_CODE_CHILD_SESSION=1` auf Script-Kinder, die selbst `Task` nutzen.
- Ob Script-Kinder ins Transkript der Host-Session einfliessen (T9 sagt: nicht als eigene Sessions sichtbar).
- `chmod +x` bei Skill-Scripts: Doku-Passage nicht gelesen.
- `--max-turns`-Ueberschreitungsverhalten (Exit-Code, `subtype`), `--max-budget-usd`-Abbruch nicht provoziert.
- Der SessionStart-Hook meldete im Kind einen JaCoCo-`check`-Fehler beim Pre-Fetch (nicht untersucht; unabhaengig von der Fragestellung, aber: der Hook-Befehl `-Dtest=BeanPropertyMatcherTest` mit Coverage-Check schlug hier fehl).
- Modell-IDs: Default `claude-opus-5-5`, Haiku `claude-haiku-5-5` laut init/usage (b); `--model haiku` loest korrekt auf.

## 7. Quellen

Doku (WebFetch am 2026-11-12):
- https://code.claude.com/docs/en/headless (Flags, `--bare`, Berechtigungen, Resume, SIGTERM, Hintergrundarbeit bei `-p`)
- https://code.claude.com/docs/en/skills (Dateien, Frontmatter, Substitutionen, Orte)
- https://code.claude.com/docs/en/claude-code-on-the-web (Cloud: Rate-Limits, Idle/Expiry, `--cloud`, GitHub-Auth, Subagents)
- https://code.claude.com/docs/en/cloud-environments (Time limits, GitHub-Proxy, Hooks im Cloud-Kontext)

Lokal:
- `claude --help`, `claude --version`, `claude auth status`, `claude mcp list`, `claude agents`, `claude --bg`
- `/root/.ccr/README.md`
- `/home/user/hamcrest-matcher-generator/CLAUDE.md`, `.claude/settings.json`, `.claude/settings.local.json`, `.claude/hooks/session-start.sh`, `.claude/agents/implementer.md`
- `.claude/skills/limit-loop/SKILL.md`, `.agents/skills/{loop-me,implement-spec,claude-handoff,writing-for-agents,setup-matt-pocock-skills,git-guardrails-claude-code}/`
- `docs/agents/implementation.md`, `docs/agents/build-pitfalls.md`
- Testartefakte im Scratchpad: `ralph.sh`, `ralph/iter1.json`, `ralph/iter2.json`, `t1.json`, `t2.jsonl`, `t2.debug` (Hook-Dauer 83343 ms)
