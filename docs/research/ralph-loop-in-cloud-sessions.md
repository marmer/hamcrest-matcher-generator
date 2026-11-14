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

## 8. Nachtrag: geklaerte offene Punkte

Stand: 2026-11-12 (Abend), `claude` im Container 2.1.322 (Kinder, Haiku); die Host-Session meldet `CLAUDE_CODE_VERSION=2.1.42`/Entrypoint `remote_mobile` (Env, b). Alle Tests mit Haiku, `--settings '{"disableAllHooks":true}'`, `--no-session-persistence`, Gesamtkosten der Tests unter 0,05 USD. Kennzeichnung wie oben: **(a)** Doku, **(b)** lokal verifiziert, **(c)** Annahme, **unbekannt**. Testartefakte liegen im Scratchpad `/tmp/claude-0/-home-user-hamcrest-matcher-generator/892e21a3-3ba3-5b24-b4aa-a167a08bed45/scratchpad/` (`t3a.json`, `t3b.json`, `t3c.json`, `t4.jsonl`, `t5.jsonl`, `t2g-A.json`, `t2g-B.json`, `heartbeat.sh`, `heartbeat.log`).

### 8.1 Haelt ein detachtes Hintergrund-Script die VM wach? (Punkt 1)

**Befunde zum Resume dieser Session (b):**

- `uptime -s` / `who -b`: Boot **2026-11-12 22:13:58 UTC**; PID 1 ist `/process_api --firecracker-init`, alle Prozesse (ausser denen von 22:13:58 an) sind neu. Der `environment-manager` laeuft mit `--session-mode resume` (`ps`, Startzeit 22:13:59), im `env-manager.log` stehen fuer fruehere Starts die Eintraege `session_mode: resume-cached`, "Fast resume: Using existing repository clones", "Using cloud session persistence with resume URL" (Zeitstempel 2026-10-30).
- **Das Dateisystem ueberlebte, Prozesse nicht:** `/tmp` enthaelt Dateien von vor dem Reboot (z.B. `/tmp/.ses` vom 22.10., `/tmp/claude-code-2001512437.diag.log` vom 12.11. 19:31, `/tmp/session-start.log` jetzt neu 22:15). Das alte Scratchpad des Vorgaengers `.../6f53e082-.../scratchpad/` (`ralph.sh`, `ralph/iter*.json`, `t1.json`, `t2.debug`, ...) ist vollstaendig vorhanden (letzte Aenderung 19:11). Es laeuft **kein** Prozess des Vorgaengers (`ps -eo pid,lstart,cmd`: ausser Init, `environment-manager`, `claude --preload`, MCP-PDF-Server nichts). Der Scratchpad-Pfad wechselte, weil die Session-ID der Kind-Session eine andere war als beim Vorgaenger (c); `/tmp/claude-0/-home-user-hamcrest-matcher-generator/` enthaelt 26 Session-Verzeichnisse.
- Letzte Aktivitaet der alten VM laut Dateien: 19:31 (`claude-code-2001512437.diag.log`); neuer Boot 22:13:58, das sind **ca. 2 h 43 min** Luecke. Frueherer Reboot-/Resume-Spuren: Diag-Logs vom 30.10. (08:49, 19:02) und 08.11. (00:24), also ist Reboot mit Resume der Normalfall nach Pausen (c: Schluss aus den Diag-Log-Zeitpunkten).
- Der Vorgaenger hatte **keinen** Hintergrundprozess gestartet (im alten Scratchpad nur Skripte/Ergebnisse, 2-Iterationen-Loop endete selbst). Es existiert daher **kein** Beleg, ob ein laufendes Script die Pause verhindert haette.

**Doku (a, code.claude.com/docs/en/claude-code-on-the-web, Abschnitt "Environment expired"):** "Cloud sessions stop after a period of inactivity and the session's VM is reclaimed. A session counts as inactive while it waits for you to approve an MCP connector tool call or to sign in to an MCP server, and it can expire during that wait." Beim Wiederoeffnen wird eine frische VM bereitgestellt; **wiederhergestellt**: Konversationsverlauf; **nicht wiederhergestellt**: laufende Shell-Commands, Subagents, session-scoped Scheduled Tasks und `/loop`. Die Doku nennt **keine** Zahl fuer die Inaktivitaetsdauer und sagt nicht, ob ein Hintergrundprozess als Aktivitaet zaehlt. `read_documentation` (Tool der Host-Session, Thema `session.resources`) lieferte keine Aussage zu Idle/Lebensdauer.

**Schluss:** Belegt (a+b): Nach Inaktivitaet wird die VM gestoppt und spaeter per Resume neu gestartet; Dateien bleiben, Prozesse sind weg. **Ein detachtes Script verhindert das nicht (Heartbeat-Ergebnis unten, b).** Konsequenz fuer den Ralph-Loop bleibt wie in 2.4/5.3: Zustand in Dateien/Git, Script idempotent neu startbar, Hauptsession startet es nach Resume erneut.

**Heartbeat-Ergebnis (b, ausgewertet 2026-11-13 06:03 UTC):** Ein detachter Hintergrundprozess haelt die VM **nicht** wach. Das Log hat `START 2026-11-12T22:17:06Z` plus 12 Zeitstempel im 60-s-Takt, der letzte ist `22:28:06Z`; es gibt keine Luecke dazwischen und **keine** `END`-Zeile (der Prozess hat sich also nicht selbst nach 90 min beendet). Neuer Boot: `uptime -s` = 2026-11-13 06:01:25, `pgrep -f heartbeat.sh` findet nichts. Die letzte Aktivitaet der Hauptsession war um ca. 22:23-22:24 (letzte gelesene Benachrichtigung 22:23:13), der Prozess blieb also nur etwa 4-5 Minuten nach der letzten Session-Aktivitaet am Leben. Das passt zu der Doku-Angabe "nach einigen Minuten" aus Abschnitt 5.3. Annahme (c): Die VM wurde um 22:28 eingefroren bzw. gestoppt; aus dem Log allein ist nicht unterscheidbar, ob eingefroren oder beendet. Ebenfalls unbekannt: ob ein laufender Vordergrund-Tool-Aufruf der Hauptsession (z.B. `Monitor` oder ein langer `Bash`-Aufruf) die Session als aktiv zaehlt; das war nicht Teil des Tests. Konsequenz: Ein Ralph-Loop, den die Hauptsession nur startet und dann nicht mehr beobachtet, laeuft nicht zuverlaessig laenger als wenige Minuten. Die Hauptsession muss waehrend der Laufzeit aktiv bleiben (offen, womit: Monitor/laufender Tool-Aufruf), oder der Loop muss nach Resume idempotent neu gestartet werden.

**Heartbeat-Experiment (Aufbau):** Detachter Prozess (`setsid nohup`), schreibt alle 60 s einen UTC-Zeitstempel, beendet sich nach 90 min selbst und schreibt dann `END ...`.

- Script: `.../scratchpad/heartbeat.sh`; Log: `/tmp/claude-0/-home-user-hamcrest-matcher-generator/892e21a3-3ba3-5b24-b4aa-a167a08bed45/scratchpad/heartbeat.log`; PID **1539** (Start 22:17:06 UTC, Boot-Zeit der VM steht in der `START`-Zeile: `uptime_since=2026-11-12 22:13:58`).
- Test nur aussagekraeftig, wenn die Session nach dem Start **idle** ist (Agentenlauf haelt sie aktiv). Der Auftraggeber startet nach der Idle-Phase eine Auswertung mit **einem Befehl** (Lueckengrenze 90 s):

```bash
python3 -c "import sys,datetime as d;f='%Y-%m-%dT%H:%M:%SZ';L=[l.strip() for l in open(sys.argv[1]) if l[:2]=='20'];t=[d.datetime.strptime(x,f) for x in L];print(len(L),'Zeilen',L[0],'->',L[-1]);[print('LUECKE',L[i-1],'->',L[i],int((t[i]-t[i-1]).total_seconds()),'s') for i in range(1,len(t)) if (t[i]-t[i-1]).total_seconds()>90]" /tmp/claude-0/-home-user-hamcrest-matcher-generator/892e21a3-3ba3-5b24-b4aa-a167a08bed45/scratchpad/heartbeat.log; uptime -s; pgrep -f heartbeat.sh
```

Lesehilfe: (1) Keine Luecke bis `END` und `uptime -s` unveraendert (22:13:58) = die VM lief durch, ein Hintergrundprozess haelt sie also (zumindest bis 90 min) wach oder die Idle-Frist ist laenger. (2) Log bricht ohne `END` ab, `uptime -s` neuer als 22:13:58, `pgrep` leer = VM wurde trotz Heartbeat gestoppt/rebootet (Script haelt nicht wach). (3) Luecke in den Zeitstempeln, aber gleicher `uptime -s` und Prozess lebt weiter = VM wurde eingefroren/pausiert (Snapshot), Prozess nicht beendet. Auswertung kann nur der Auftraggeber leisten, Ergebnis hier: **ausstehend**.

### 8.2 Echter `git push` aus `claude -p`-Kind (Punkt 2)

- **Remote/Credentials (b):** `origin` = `https://github.com/marmer/hamcrest-matcher-generator` (ohne Zugangsdaten in der URL); `git config` enthaelt nur `url.https://github.com/.insteadof` (SSH-Formen auf HTTPS umgeschrieben, Name der Werte `git@github.com:`/`ssh://...@github.com/`), `http.proxyauthmethod basic`, `credential.interactive false`; kein Credential-Helper in der Config. Authentisiert wird ueber den Agent-Proxy (`HTTPS_PROXY`), der laut Doku (a) die Credentials serverseitig anhaengt. Tokens wurden nicht ausgegeben.
- **Push-Test:** Ein Kind (Haiku, `--settings '{"disableAllHooks":true}'`, `--max-turns` klein, `--allowedTools` nur `git add/commit/push` auf diese Datei) committet diese Datei und pusht ausschliesslich `HEAD:refs/heads/ccr-bcc81c73-3tvsvy` (kein Force, kein anderer Branch). Dass der Push gelungen ist, belegt der Commit `docs: Nachtrag zur Ralph-Loop-Recherche` mit dem Trailer des Kindes auf dem Remote-Branch (`git ls-remote origin refs/heads/ccr-bcc81c73-3tvsvy`; SHA im Endbericht des Auftrags).
- **Guardrail-Hooks, relevant fuer die Sicherheit (b):**
  - `.claude/settings.json` enthaelt **nur** den `SessionStart`-Hook; es gibt **keine** `.claude/hooks/block-dangerous-git.sh` im Repo (nur `session-start.sh`). Das Skript `.agents/skills/git-guardrails-claude-code/scripts/block-dangerous-git.sh` (blockt u.a. `git push`, `reset --hard`, `push --force`, exit 2) existiert, ist aber **nicht** in die Settings eingehaengt. Ein Kind im Loop wird also heute von **keinem** Repo-Guardrail am Push gehindert.
  - `--settings '{"disableAllHooks":true, "hooks":{PreToolUse:...block-dangerous-git.sh}}'` (Test A, `git push --dry-run` auf den Branch): Hook **greift nicht**, Befehl lief ("Everything up-to-date"). Dasselbe Hook-JSON ohne `disableAllHooks` und mit `--setting-sources user` (Test B): Hook **blockt** den Befehl ("BLOCKED: ... matches dangerous pattern 'git push'", `permission_denials: 1`). Folgerung: `disableAllHooks:true` schaltet auch einen per `--settings` mitgegebenen Guardrail ab; ein Loop, der Hooks fuer Geschwindigkeit abschaltet, hat **keinen** Hook-Schutz mehr. Schutz muss dann ueber `--allowedTools` (eng, z.B. nur `Bash(git push origin HEAD:refs/heads/ticket-*)`), Prompt und serverseitige Branch-Protection laufen. Alternative (c, nicht getestet): nur den SessionStart-Hook vermeiden, statt alle Hooks abzuschalten, z.B. eigene `--settings`/`--setting-sources` ohne Projekt-Hook plus Guardrail-Hook (Test B zeigt, dass `--setting-sources user` + `--settings`-Hook funktioniert; kostet aber CLAUDE.md und Repo-Skills, siehe T4).
  - Das Host-`~/.claude/launcher-settings.json` hat Hooks `SessionStart` und `Stop` (`stop-hook-git-check.sh`); sie gelten fuer die Host-Session. Ob sie in Kinder vererbt werden, wurde nicht gezielt geprueft (c: nein, Settings kommen per `--settings` des Launchers, nicht per Env).
- **PR-Anlage: nicht real getestet** (Auftrag). Nur lesend (b): `gh api 'repos/marmer/hamcrest-matcher-generator/pulls?head=marmer:ccr-bcc81c73-3tvsvy&state=all'` liefert #401 (open, "docs: research Ralph loop via script in cloud sessions") und #273 (closed). Ob `gh api -X POST .../pulls` aus einem Kind erlaubt ist: **unbekannt**.

### 8.3 `--max-turns`- und `--max-budget-usd`-Ueberschreitung, 429 (Punkt 3)

Alle drei Faelle (b, `--output-format json`, Haiku):

| Fall | Aufruf | Exit-Code | JSON |
|---|---|---|---|
| Max-Turns | Aufgabe mit zwei Bash-Aufrufen, `--max-turns 1` (`t3a.json`) | **1** | `type: result`, `subtype: "error_max_turns"`, `is_error: true`, `terminal_reason: "max_turns"`, `num_turns: 2`, `stop_reason: "tool_use"`, `errors: ["Reached maximum number of turns (1)"]`, **kein `result`-Feld** |
| Budget | `--max-budget-usd 0.001` (`t3c.json`) | **1** | `subtype: "error_max_budget_usd"`, `is_error: true`, `terminal_reason: "budget_exhausted"`, `errors: ["Reached maximum budget ($0.001)"]`, **kein `result`-Feld**; `total_cost_usd` 0,0030 (Limit wird leicht ueberschritten, weil die Pruefung nach dem Request greift) |
| Budget nicht erreicht | `--max-budget-usd 0.01`, Kosten 0,0048 (`t3b.json`) | 0 | `subtype: "success"`, `is_error: false`, `terminal_reason: "completed"`, `result` gesetzt |

Zuverlaessige Erkennung im Loop-Script: **Nicht** `.result` lesen (fehlt bei Abbruch), sondern `jq -r '.subtype'` und `.is_error`; Exit-Code 1 ist bei beiden Limits gesetzt (im Gegensatz zu T6a, wo ein Permission-Fehler rc 0 lieferte). Empfohlene Auswertung: `rc!=0 || .is_error==true` = Fehler; `subtype` unterscheidet `error_max_turns` (Iteration nicht fertig: naechste Iteration oder abbrechen), `error_max_budget_usd` (Budget zu klein oder Loop zu teuer: nicht blind wiederholen) von `success`; `success` allein heisst weiterhin nicht "Aufgabe erledigt" (Artefakte pruefen, 2.3 T6a). `--max-budget-usd` stoppt laut Doku (a) auch noch laufende Hintergrundarbeit des Kindes (headless, "Background tasks at exit").

**429 (a, nicht verifiziert):** Doku code.claude.com/docs/en/errors: "Claude Code retries transient failures up to 10 times with exponential backoff before showing you an error"; Temporaere 429-Throttles werden wiederholt, auch bei claude.ai-Abo (ab v2.1.199); `CLAUDE_CODE_MAX_RETRIES` (Default 10, Cap 15); `CLAUDE_CODE_RETRY_WATCHDOG=1` fuer unbeaufsichtigte Laeufe wiederholt 429/529 unbegrenzt, ausser bei gemeldetem Spend-Limit/aufgebrauchten Credits (dann sofort Fehler); Wartezeit-Caps `CLAUDE_CODE_RETRY_WATCHDOG_MAX_WAIT_MS` (ab v2.1.295) und `..._MAX_USAGE_LIMIT_WAIT_MS` (ab v2.1.301). Headless-Doku (a): Retry-Events `system/api_retry` im stream-json mit `error: rate_limit` u.a. **Unbekannt**: Exit-Code und `subtype` bei endgueltigem 429/Usage-Limit in `-p` (der Abschnitt "Usage limits" der Fehlerseite wurde nicht gelesen, Seite hat ca. 627 000 Zeichen, nur die ersten 100 000 wurden gelesen). Nicht provozierbar; im Loop-Script defensiv behandeln (`rc != 0` und Text in `.result`/`stderr` auf "limit" pruefen, grosse Pause, `limit-loop` des Repos).

### 8.4 `MAX_SUBAGENT_SPAWN_DEPTH` und verwandte Env-Variablen (Punkt 4)

- `env | grep -i -E 'subagent|spawn|depth'` (b, Host-Session): nur `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH=1`; zusaetzlich `CLAUDE_CODE_CHILD_SESSION=1`, `CLAUDE_CODE_SESSION_ATTENDED=1`, `CLAUDE_CODE_REMOTE=true`, `CLAUDE_CODE_REMOTE_ENVIRONMENT_TYPE=cloud_default`, `CLAUDE_CODE_WORKER_EPOCH=5` (Epoche 5 = Zahl der Resumes/Neustarts, c). Secret-artige Werte (z.B. `CLAUDE_CODE_MESSAGING_TOKEN`) nicht ausgegeben.
- **Kind darf Agent-Tool nutzen (b):** Ein `claude -p`-Kind rief den `Agent`-Tool (`subagent_type: general-purpose`) auf, der Subagent antwortete "PONG"; `subagent_stats` im Ergebnis: `spawned: 1`, `max_depth: 1`, `spawned_by_subagents: 0`, `refused: {depth_limit: 0, concurrency_limit: 0, budget: 0}`. Das Kind sieht dieselben Env-Werte (`CHILD_SESSION=1`, `MAX_SUBAGENT_SPAWN_DEPTH=1`), die also an `claude -p`-Kinder **vererbt** werden. Der Subagent lief standardmaessig im Hintergrund ("Async agent launched"), `-p` wartete auf ihn (Doku a: bleibt offen bis Subagent fertig).
- **Wirkung auf Ralph-Kinder:** Ein Ralph-Kind (Tiefe 1) darf Subagents starten; die Tiefenbegrenzung 1 wuerde laut Zaehler `refused.depth_limit` Subagents **dieser Subagents** verweigern (c: Zaehlername und `max_depth: 1` deuten darauf, Enkel-Spawn nicht getestet). Ein Ralph-Loop sollte also nicht auf verschachtelte Orchestrierung (Subagent startet Subagent) bauen, und Kinder duerfen fuer Parallelarbeit eine Ebene Subagents nutzen. Ob `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH` im Script-Kind hoeher gesetzt werden duerfte: unbekannt/ungetestet (wuerde die Vorgabe der Umgebung umgehen, daher nicht versucht).

### 8.5 Ausfuehrbares Bit bei Skill-Scripts (Punkt 5)

- **Doku (a):** code.claude.com/docs/en/skills vollstaendig gelesen (0 bis 119 051 Zeichen; Anfang, `${CLAUDE_SKILL_DIR}`, Beispiel "codebase-visualizer" mit `python3 ${CLAUDE_SKILL_DIR}/scripts/visualize.py .` und `allowed-tools: Bash(python3 *)`; Beispiel `render-chart` mit `allowed-tools: Bash(${CLAUDE_SKILL_DIR}/scripts/render.sh *)`). Zu `chmod`, Ausfuehrbit oder Shebang steht dort **nichts**; die Doku ruft Python-Scripts ueber den Interpreter auf. Das Ausfuehrbit ist also dokumentarisch **nicht geregelt** (Lueckenbefund).
- **Lokaler Test (b):** Temporaerer Skill `.claude/skills/tmp-exec-test/` (danach geloescht, nie committet; `git status` sauber) mit `scripts/with-x.sh` (`-rwxr-xr-x`) und `scripts/without-x.sh` (`-rw-r--r--`), beide `#!/usr/bin/env bash`. `claude -p "/tmp-exec-test"` (Haiku) rief per `${CLAUDE_SKILL_DIR}` (zum absoluten Pfad aufgeloest) auf: (1) `.../with-x.sh` direkt: `with-x-ok`; (2) `.../without-x.sh` direkt: **`Exit code 126`, `Permission denied`**; (3) `bash .../without-x.sh`: `without-x-ok`.
- **Empfehlung (b/c):** Im `SKILL.md` immer `bash ${CLAUDE_SKILL_DIR}/scripts/ralph.sh ...` (bzw. `python3 ...`) schreiben und in `allowed-tools` exakt dieselbe Form freigeben (`Bash(bash ${CLAUDE_SKILL_DIR}/scripts/ralph.sh *)`); dann ist das Ausfuehrbit egal, es ueberlebt auch Git-Modus-Verluste (Checkout ohne Modus, Archiv, Windows). `chmod +x` trotzdem setzen und per Git (`git update-index --chmod=+x`) festhalten, falls jemand direkt aufruft. Schlug etwas fehl, meldet die Shell `126`, das Modell kann dann selbst auf `bash` ausweichen (im Test tat Haiku das nicht von allein, es folgte der Anweisung, alle drei Aufrufe zu machen).

### 8.6 SessionStart-Hook: JaCoCo-Fehler beim Maven-Pre-Fetch (Punkt 6)

- **Befund (b):** `/tmp/session-start.log` (Lauf 22:14:03): vier Versuche, jeder endet mit `[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.16:check (jacoco-check) on project hamcrest-matcher-generator: Coverage checks have not been met.`; Abschluss: "Maven pre-fetch failed ... Builds fall back to online resolution." Der Hook (`.claude/hooks/session-start.sh`, `prefetch_maven_dependencies`, Zeilen 35 ff.) fuehrt `./mvnw -B -q install -Dtest=BeanPropertyMatcherTest -Dit.test=MatcherGenerationProcessorIT -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false` aus und wiederholt bis zu 4-mal mit 2/4/8 s Pause (gedacht fuer HTTP 429).
- **Ursachenvermutung (c, nicht durch Coverage-Report belegt):** Die Root-`pom.xml` (Zeilen ca. 375 bis 394, Execution `jacoco-check`) verlangt `COVEREDRATIO` mindestens **0,99** (zwei Limits). Der Hook fuehrt nur **eine** Unit-Testklasse und **eine** IT-Klasse aus; damit ist die Gesamtabdeckung des Moduls `hamcrest-matcher-generator` unter 99 %, `check` laeuft vor `install` und bricht ab. Der Pre-Fetch laedt die Dependencies trotzdem herunter (Zweck erfuellt, c: der Fehler tritt erst nach dem Test-Lauf auf), aber das `install` der Module nach dem Fehler fehlt und die Schleife wiederholt vier Mal unnoetig (kostet Zeit; ca. 83 s je Start, T2).
- **Relevanz fuer den Loop:** Gering. Der Loop setzt `disableAllHooks`, der Hook laeuft dann nicht (T5). Relevant nur fuer die Haupt-/Kind-Sessions ohne diese Option und fuer jeden `claude -p`-Aufruf ohne sie (83 s + Race auf `~/.m2` bei Parallelkindern). Keine Reparatur vorgenommen (Auftrag: nur Befund). Naheliegende Abhilfe (c, ungetestet): `-Djacoco.skip=true` oder `-Dskip...` fuer den Pre-Fetch bzw. den `check`-Execution ueberspringen.

### 8.7 Repo-Grep nach vorhandener Ralph-/Loop-Mechanik (Punkt 7)

`rg -i -n 'ralph|--max-turns|claude -p' --glob '!docs/research/**' --glob '!**/target/**'` (zusaetzlich `rg -i -l ... --hidden -g '!.git/**'`, damit `.claude/`, `.agents/` und `.github/` eingeschlossen sind) im ganzen Arbeitsverzeichnis: **keine Treffer** ausserhalb von `docs/research/` (b; Sanity-Check: `rg -c -i 'claude -p' docs/research` findet 15 Treffer in dieser Datei). Es gibt im Repo also weder Ralph-Skript noch `claude -p`-/`--max-turns`-Aufrufe; die bestehenden Verwandten (`limit-loop`, `claude-handoff`, `loop-me`) enthalten diese Zeichenketten nicht (c: `claude-handoff` nutzt `claude --bg`, was das Muster nicht trifft). Damit ist die Aussage aus Abschnitt 4 ("Grep nicht vollstaendig") nun belegt (b).

### 8.8 Offen geblieben

- Haelt ein Hintergrundprozess die VM wach: **nein**, er stoppte ca. 4-5 min nach der letzten Session-Aktivitaet (8.1). Offen: zaehlt ein laufender Monitor-/Bash-Aufruf der Hauptsession als Aktivitaet?
- Exit-Code/`subtype` von `-p` bei endgueltigem 429/Usage-Limit (8.3): nur Doku-Teilwissen, nicht verifiziert.
- Enkel-Spawn (Subagent startet Subagent) und Hoechstwert-Aenderung von `CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH` (8.4): nicht getestet.
- PR-Anlage per REST aus einem Kind (8.2): nicht getestet.
- Ursache des JaCoCo-Fehlers (8.6): Annahme.

## 9. Trial on a test spec (#421)

Probelauf des gebauten Loops (Spec #410) auf einer Test-Spec (#443) mit drei Dokumentations-Tickets (#444 bis #446, Haiku). Drei Laeufe, aus der Hauptsession per `loop.sh` gestartet; alle Aussagen sind Beobachtungen (b) aus diesen Laeufen, ausser den ausdruecklich als Annahme oder Schluss markierten, jeweils ein Lauf je Frage, also keine Statistik. Nicht Teil dieses Trials: der Lauf auf einer echten kleinen Spec (#422).

### 9.1 Ergebnis je Abnahmefrage

| Frage | Beobachtung | Urteil |
|---|---|---|
| `permissions.deny` in `-p` ohne Hooks | Haiku-Probe (3 Turns) mit `disableAllHooks`: `git branch -D` und `git push --force --dry-run` abgelehnt, obwohl `Bash(git:*)` erlaubt ist; `permission_denials` im Ergebnis gefuellt. | haelt (nur die zwei Regeln geprueft, nicht alle vier) |
| `stop: now` | Lauf 1, ca. 2,5 min in Iteration 1: das Script beendete `claude` innerhalb von weniger als 60 s nach der Aenderung der Control-Datei; Zeilen `STOP iteration 1 killed (stop now)` und `END ... (0 ok, 1 failed)`, kein Retro, `ended`-Marker geschrieben. | wie spezifiziert; Branches und Worktrees `ticket-444`/`ticket-446` (lokal committet, nicht gepusht) bleiben liegen |
| `stop: after-step` | Lauf 2, Stop 21 s nach Start von Iteration 2: die Iteration lief von allein zu Ende (13 Turns, ca. 1,5 min), danach Retro, dann `END (2 ok, 0 failed)`. Das Script achtet den Stop. Der Orchestrator las `control.json` nur einmal am Start, nie zwischen Arbeitsschritten (siehe 9.3). | Script-Seite ok; "nach dem Arbeitsschritt" haengt am Orchestrator und ist nicht erfuellt (D1) |
| `stop: after-iteration` | Lauf 3, gesetzt waehrend der wiederaufgenommenen Iteration 1: Iteration endete ok, dann Retro, dann `END after 1 iterations: stop requested (after-iteration) (1 ok, 0 failed)`. | wie spezifiziert |
| Resume nach `kill -9` | Lauf 3: `kill -9` auf Script und `claude` mitten in Iteration 1, danach `loop.sh 443 --resume-only`. Dasselbe Run-Verzeichnis (`443-20261113-230118`) wurde fortgesetzt, Status `running`, Iteration 1 (die getoetete) lief erneut, die verwaiste Lock-Datei wurde akzeptiert. | funktioniert |
| Control-Datei, waehrend Subagents laufen | Der Orchestrator einer Iteration liest `control.json` hoechstens einmal am Start (Iteration 2 von Lauf 2: ein Read, vor dem Stop). Subagents sehen die Datei vermutlich nie (Schluss aus dem Verhalten, nicht gemessen). Ein Stop `after-step` wirkt daher erst, wenn die Iteration fertig ist; nur `now` greift mitten drin (das Script killt, der Orchestrator ist nicht beteiligt). | Luecke (D1) |
| `max_parallel` | Auf 1 gesetzt; der Orchestrator startete in beiden Iterationen trotzdem je 3 Implementer parallel. Er suchte in `loop.sh` nach "parallel" und fand nichts im Prompt-Pfad. Wie `$RALPH_RUN_DIR` zu nutzen ist, war ihm unklar. | wird ignoriert (D1) |
| VM wach bei laufendem Monitor (ca. 2 h) | Idle-Heartbeat (Messprotokoll der Hauptsession, nicht im Repo): Hauptsession untaetig, ein Monitor je 30 min neu gestartet (Start 23:05:38 UTC, nach dem Ende von Lauf 3 um 23:05:10). Heartbeats kamen um 23:35, 00:05 und 00:35 UTC (VM nach 30, 60 und 90 min wach); um ca. 00:51 UTC (nach 1 h 46 min) wurde der Container neu gestartet, die Messung brach dort ab (Heartbeat 4 ging verloren). Dateien (Run-Verzeichnis, Branches, Worktrees) ueberlebten, Prozesse nicht. | mindestens 90 min wach; Neustart nach 1 h 46 min trotz laufendem Monitor, die Ursache ist nicht bekannt (Annahme: Container-Lebensdauer oder Wartung, nicht Leerlauf; nicht belegt) |

Zur Einordnung der letzten Zeile: der Lauf ueber ca. 2 h ist kein vollstaendiger 2-h-Loop mit Implementierungsarbeit. Er misst nur, ob die VM wach bleibt, solange das Script laeuft und die Hauptsession einen Monitor betreibt; der Monitor-Test ist ein Heartbeat einer sonst untaetigen Session. Ob echte Arbeit (Maven, Parallelkinder) die VM laenger oder kuerzer wach haelt, ist damit nicht gezeigt (Annahme). Auch offen aus 8.8: zaehlt ein laufender Monitor der Hauptsession als Aktivitaet.

### 9.2 Kosten

Lauf 2: Iteration 1 0,43 USD, Iteration 2 0,37 USD, Retro 0,32 USD; Lauf 3: Iteration 1 0,30 USD, Retro 0,37 USD; Lauf 1 nicht erfasst. Zusammen etwa 1,8 USD und mehr; die Haiku-Implementer stehen nicht im `result`-Betrag der Iteration, die Summe ist also eine Untergrenze (c). Der Trial hat zudem Reste auf `main` und im Repo hinterlassen: `docs/research/ralph-trial/issue-py-subcommands.md` (ueber #447 gemergt), offene PRs #448 und #449 (Testdateien) und #453 (Learnings des Retros).

### 9.3 Defekte und Tickets

| Id | Defekt | Ticket |
|---|---|---|
| D1 | `max_parallel` wird vom Orchestrator ignoriert (F3, F11) und `control.json` wird zwischen Arbeitsschritten nicht neu gelesen, so dass `after-step` nicht wirkt (F8) | #456 |
| D2 | `gh pr merge --auto --squash` wird abgelehnt (Allow-Liste kennt nur `Bash(gh api:*)`), das MCP-Tool `enable_pr_auto_merge` fehlt in `-p`, per REST gibt es keinen Weg (GraphQL in Cloud-Sessions gesperrt): PRs bleiben offen ohne Auto-Merge, `is_error` bleibt false, nur eine `DENIAL`-Zeile (F6) | #450 |
| D3 | Die Frontier schliesst Tickets mit offenem PR nicht aus; es gab keine Uebergabenotiz, Iteration 2 implementierte #444 bis #446 ein zweites Mal und endete mit einer interaktiven Rueckfrage im Ergebnistext statt Ticket-Kommentar plus `needs-info` (F9) | #451 |
| D4 | `issue-status.py --help` wird als Spec-Nummer gelesen (F10) | #452 |
| D5 | Kind kann `$RALPH_RUN_DIR` nicht lesen (`;`-Ketten und `$VAR`-Expansion abgelehnt), daher keine Uebergabenotiz und kein Ticket-Kommentar (F14, F12: 6 und 7 `DENIAL`-Zeilen in Retro und Iteration) | #455 |
| D6 | Handmerge von #447 ueber die Merge-API, waehrend `Domain docs in sync` rot oder offen war; die Regel "nie von Hand mergen" hat im Loop keine Sperre (F14) | teils #455; die fehlende Merge-Sperre #457 |
| D7 | Flaky `test_sleeping_script_reports_the_waiting_state` (rote Stufe "Test the agent scripts" bei #447, F16) | #454 |

Ohne Fehler: F13 (Resume), F1, F2, F15. F4 (liegengebliebene Branches nach `stop now`) und F7 (die "Killed"-Meldung der Shell erscheint nur, wenn stderr mitgeschrieben wird; der Skill leitet nur stdout um) sind Beobachtungen ohne Ticket. Ergebnis der Pipeline im Trial: von drei Tickets wurde eines gemergt (F16).

### 9.4 Auto-Merge aus einem `-p`-Kind (#455)

Probe am 2026-11-14 aus einer Cloud-Session (Subagent der Hauptsession, kein `-p`-Kind; dieselbe `gh`-Umgebung und derselbe Proxy, ob das `-p`-Kind sie erbt, ist nicht gemessen):

- `gh api graphql -f query='{viewer{login}}'` wird mit HTTP 403 abgelehnt. Die Meldung nennt selbst die Ausweichroute: "use the CCR routes on api.github.com: ... PUT or DELETE /repos/{owner}/{repo}/pulls/{n}/ccr/auto_merge" (daneben `review_threads`, `resolve`, `ready_for_review`, `convert_to_draft`). Die `enablePullRequestAutoMerge`-Mutation per GraphQL ist also nicht moeglich.
- Die REST-Route ist erreichbar: `gh api -X PUT repos/marmer/hamcrest-matcher-generator/pulls/99999999/ccr/auto_merge -f merge_method=squash` auf eine nicht vorhandene PR antwortet 404 "Could not resolve to a PullRequest" (keine 403, keine Proxy-Ablehnung), `GET .../ccr/review_threads` ebenso. Der Erfolgsfall (eine echte PR) wurde bewusst nicht geprueft, um auf keiner echten PR Auto-Merge zu schalten; ebenso ist offen, ob der Body-Parameter `merge_method` die richtige Form ist (Annahme).
- Folge: `scripts/agents/ralph-iteration.py auto-merge <pr>` nutzt die Route. Das erste echte Ergebnis steht aus und gehoert in den naechsten Lauf (#422); bei Ablehnung gibt der Wrapper eine `ERROR`-Zeile aus und merged nie selbst.

### 9.5 Allow-Muster fuer Skripte (#466)

Probe am 2026-11-14 (haiku, `-p`, `--max-turns 4`, `disableAllHooks`, echte `allowed`- und `--settings`-Strings von `loop.sh`), Befehl `python3 scripts/agents/ralph-iteration.py --help`: `Bash(python3 scripts/:*)` ergibt 1 permission_denial, `Bash(python3 scripts/*)` ergibt 0. Die Form `:*` braucht eine Wortgrenze, ein Pfad setzt das Wort fort. Die anderen Eintraege (`./mvnw`, `gh api`, `git`, `head`, `grep`) passen auf echte Befehle.
