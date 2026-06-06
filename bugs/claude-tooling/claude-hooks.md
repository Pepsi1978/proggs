# Bekannte Bugs & Fallen: Claude Code Hooks

> **PFLICHT-LESEN vor JEDER Arbeit an einem Claude-Code-Hook** (`.ps1`/`.sh` in
> `~/.claude/hooks/`, `settings.json`-`hooks`-Sektion, Plugin-`hooks.json`).
> Kuratiert aus offizieller Doku + Changelog, GitHub-Issues (anthropics/claude-code),
> Community (Medium/dev.to/Blogs) und eigenen Vorfaellen. Loesungen sind
> funktionserhaltend (nie "Feature weglassen").
>
> **Stand:** recherchiert am **2026-06-01** fuer Claude Code **v2.1.159**. Versionsangaben
> pro Bug beachten — viele "per Design"-Fallen gelten dauerhaft, einige Bugs sind in
> neueren Versionen gefixt, einige Regressionen sind in der v2.1.x-Reihe noch offen.

---

## ⚡ Die wichtigsten Regeln (TL;DR — zuerst lesen)

1. **Jeder Hook endet mit `exit 0`** — ausser er soll *bewusst blockieren*, dann `exit 2`
   (NICHT `exit 1`!). `exit 1` blockiert NICHT und kann Turns vorzeitig beenden.
2. **Kontext fuer Claude nur via** `{"hookSpecificOutput":{"hookEventName":"...","additionalContext":"..."}}`.
   Flaches `additionalContext` oder plain stdout wird oft still ignoriert.
3. **Nach Hook-Aenderung Session neu starten** — die Hook-Config ist gecacht.
4. **Ein JSON-Syntaxfehler in settings.json killt ALLE Hooks still** — immer validieren.
5. **stdin (das Event-JSON) robust lesen** und bei leerem/falschem Input sauber `exit 0`.

---

## 0. Notfall: ALLE Hooks erroren auf einmal (Runtime/Interpreter kaputt) ⭐ KRITISCH

### 0.1 `dyld: Library not loaded: …libllhttp.9.x.dylib` → node kaputt nach `brew install` (macOS)
**Symptom:** In ALLEN Sessions bei JEDEM Tool-Aufruf "PreToolUse: Hook error" bzw.
`Failed with non-blocking status code: dyld[…]: Library not loaded: /opt/homebrew/opt/llhttp/lib/libllhttp.9.3.dylib`.
**Ursache:** Ein `brew install <tool>` hat als Nebenwirkung eine GETEILTE Bibliothek
(`llhttp`) hochgezogen (9.3 → 9.4.1). `node` war gegen die alte `libllhttp.9.3.dylib`
gelinkt → node startet nicht mehr → jeder node-basierte Hook (und node-MCP/Tools) erroren.
Konkreter Ausloeser 2026-06-06: `git-delta` und `node` teilen sich `llhttp`
(`brew uses --installed llhttp` zeigt beide).
**Versionen:** macOS/Homebrew, jederzeit moeglich beim Bundeln mehrerer brew-Installs.
**FIX:** `brew reinstall node` (linkt node gegen das aktuelle llhttp; hier wurde node
25.9.0 → 26.0.0 gegen `libllhttp.9.4.dylib` gezogen). Danach MUSS `node --version` laufen.
NIEMALS `libllhttp.9.3.dylib` auf 9.4 symlinken — ABI-Inkompatibilitaet → subtile node-Abstuerze.
**PRAEVENTION (Direktive #3):** (1) Nach JEDEM `brew install` auf macOS sofort `node --version`
pruefen. (2) Vor dem Install pruefen, was eine neue Lib bricht: `brew uses --installed <lib>`.
(3) Tools, die sich Libs mit node teilen (git-delta → llhttp), bewusst getrennt installieren.
(4) Nach node-Major-Bump (25→26) koennen native Addons (better-sqlite3, node-pty in
MCP/claude-mem/claudewatch) brechen — bei Folgefehlern das Paket `npm rebuild` bzw. neu installieren.
**Quelle:** eigener Vorfall 2026-06-06 (self-improve-Lauf installierte 10 CLI-Tools; git-delta zog llhttp hoch).

---

## 1. Exit-Codes (haeufigste Fehlerquelle)

### 1.1 `exit 1` blockiert NICHT — nur `exit 2` blockiert  ⭐ HAEUFIG
**Symptom:** Ein Guard/Validator-Hook zeigt seine Fehlermeldung, aber die gefaehrliche
Aktion laeuft trotzdem durch.
**Ursache:** Claude Code wertet `exit 1` (und jeden Code ausser 2) als *nicht-blockierenden*
Fehler — stderr geht an den User, die Aktion faehrt fort. Nur `exit 2` blockiert. Weicht
von der Unix-Konvention ab.
**Versionen:** per Design, unveraendert.
**FIX:** Zum Blockieren `exit 2` + Begruendung auf stderr. Vorsicht: uncaught Exceptions
in Python/Bash defaulten zu `exit 1` — Logik in try/except wrappen und im Fehlerfall
bewusst `exit 2`. Standalone-Hooks am Ende immer `exit 0`.
**Quelle:** thinkingthroughcode (Medium), code.claude.com/docs/en/hooks.

### 1.2 `exit 2` blockiert nur bei BESTIMMTEN Events
**Symptom:** `exit 2` im PostToolUse-Hook verhindert nichts.
**Ursache:** `exit 2` blockt nur bei PreToolUse, PermissionRequest, UserPromptSubmit,
Stop, SubagentStop, PreCompact, TaskCreated/Completed, ConfigChange, Elicitation,
WorktreeCreate. Bei PostToolUse, SessionStart/End, Notification, SubagentStart,
PostCompact, MessageDisplay etc. zeigt `exit 2` nur stderr — blockiert nicht (Tool lief schon).
**Versionen:** per Design.
**FIX:** Blockier-Logik nur in blockierfaehige Events legen (v.a. PreToolUse).

### 1.3 Bei `exit 2` wird der JSON-Output IGNORIERT
**Symptom:** JSON-Schema (`permissionDecision` etc.) bleibt wirkungslos, wenn der Hook
mit `exit 2` endet.
**Ursache:** Claude Code parst stdout-JSON NUR bei `exit 0`. Bei `exit 2` zaehlt nur stderr (plain text).
**Versionen:** per Design.
**FIX:** Strukturierte Entscheidungen (deny/allow/modifiedInput) IMMER mit `exit 0` + JSON.
Plain-Text-Block per `exit 2` + stderr. Nicht mischen.

### 1.4 `exit 0` ohne Output ist KEIN "allow"
**Symptom:** PreToolUse-Hook gibt `exit 0`, die Aktion wird trotzdem von der normalen
Permission-Pruefung blockiert.
**Ursache:** `exit 0` ohne JSON = "normale Permission-Regeln gelten", nicht "erlaubt".
**Versionen:** per Design.
**FIX:** Zum echten Erlauben `{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"allow"}}`.
Fuer "ich entscheide nicht" `permissionDecision:"defer"` (normaler Flow laeuft weiter).

### 1.5 PostToolUse `exit 1` blockiert Claude faelschlich
**Symptom:** PostToolUse-Hook mit `exit 1` laesst Claude haengen, bis der User antwortet —
obwohl die Doku PostToolUse als "non-blocking" beschreibt.
**Ursache:** Bug im Exit-Code-Handling von PostToolUse.
**Versionen:** gemeldet (Issue #4809), Status offen — vorsichtshalber dauerhaft beachten.
**FIX:** PostToolUse-Hooks IMMER `exit 0`, Fehler nur loggen.

### 1.6 PreToolUse `exit 2` blockiert Write/Edit NICHT (sicherheitsrelevant)
**Symptom:** Ein `exit 2`-Hook stoppt `Bash` korrekt, aber bei `Write`/`Edit` wird die
Datei trotzdem erstellt/geaendert.
**Ursache:** Bug — der Write/Edit-Pfad wertet `exit 2` nicht aus.
**Versionen:** gemeldet (Issues #13744, #21988), Status offen.
**FIX:** Statt Exit-Code die JSON-Ausgabe `permissionDecision:"deny"` nutzen (greift fuer
alle Tools). Schutz von Dateien nie allein auf den exit-2-Block stuetzen.

### 1.7 WorktreeCreate — JEDER non-zero Exit blockiert
**Symptom:** WorktreeCreate-Hook bricht schon bei `exit 1` das Worktree-Anlegen ab.
**Ursache:** Sonderfall — anders als alle anderen Events blockiert hier jeder non-zero Exit.
**Versionen:** per Design.
**FIX:** In WorktreeCreate-Hooks IMMER sauber `exit 0` am Ende.

---

## 2. JSON-Output-Schema & Kontext-Injection

### 2.1 Flaches `additionalContext` wird still ignoriert  ⭐ HAEUFIG
**Symptom:** Hook gibt `{"additionalContext":"..."}` aus, nichts erscheint in Claudes
Kontext, kein Fehler.
**Ursache:** `additionalContext` MUSS in `hookSpecificOutput` verschachtelt sein, UND
`hookEventName` ist im selben Objekt Pflicht. Fehlt es, wird der Output stumm verworfen.
**Versionen:** per Design (verifiziert lokal 2026-05-24, #1049).
**FIX:** `{"hookSpecificOutput":{"hookEventName":"PreToolUse","additionalContext":"..."}}`.

### 2.2 PowerShell `ConvertTo-Json` flacht das verschachtelte Objekt ab
**Symptom:** PS-Hook erzeugt `hookSpecificOutput` als String `"System.Collections.Hashtable"`
statt verschachteltem JSON.
**Ursache:** `ConvertTo-Json` serialisiert standardmaessig nur 2 Ebenen tief.
**Versionen:** per Design (PowerShell-Eigenheit).
**FIX:** `ConvertTo-Json -Depth 5` (oder hoeher).

### 2.3 stdout landet nur bei BESTIMMTEN Events in Claudes Kontext
**Symptom:** Plain-stdout eines PreToolUse/PostToolUse-Hooks taucht nie in Claudes Kontext auf.
**Ursache:** Plain stdout fliesst nur bei `SessionStart`, `Setup`, `SubagentStart` in den
Kontext. Bei PreToolUse/PostToolUse geht plain stdout nur ins Debug-Log.
**Versionen:** per Design.
**FIX:** Fuer Kontext bei PreToolUse/PostToolUse das `hookSpecificOutput.additionalContext`-Schema
verwenden (nicht plain stdout).

### 2.4 v2.1.123: Bash-Matcher droppt ALLE Kontext-Kanaele  🆕 REGRESSION
**Symptom:** Ein Pre-/PostToolUse-Hook mit `matcher:"Bash"` feuert und gibt korrektes JSON
aus, aber Claude referenziert den Text NIE. `additionalContext`, `systemMessage` UND plain
stdout kommen alle nicht an.
**Ursache:** Regression — die Harness empfaengt das JSON, surfaced es aber nicht im Tool-Result
(speziell beim Bash-Matcher).
**Versionen:** ab v2.1.123 gemeldet (Issue #55889). Auf 2.1.159 verifizieren, ob noch praesent.
**FIX (funktionserhaltend):** Kontext stattdessen ueber `UserPromptSubmit`, `SessionStart`
oder `SubagentStart` injizieren; ODER den Marker zusaetzlich in eine Datei schreiben, die
der Agent per Pfad nachlaedt (deckt sich mit dem Lossless-Prinzip).

### 2.5 `additionalContext` bei UserPromptSubmit akkumuliert in der History
**Symptom:** Jede User-Nachricht haengt einen weiteren system-reminder-Block an → Kontext
waechst unkontrolliert.
**Ursache:** UserPromptSubmit-`additionalContext` ist nicht ephemer.
**Versionen:** gemeldet (Issue #40216).
**FIX:** Kurz/kompakt halten; grosse Inhalte in eine Datei schreiben und nur den Pfad injizieren.

### 2.6 SessionStart-`additionalContext` aus PLUGIN-Hooks wird nicht surfaced
**Symptom:** Plugin-`SessionStart`-Hook setzt `additionalContext`, es landet aber nie im Kontext.
**Ursache:** Bug speziell bei Plugin-SessionStart-Hooks (Doc-Schema fehlte SessionStart, #22031).
**Versionen:** gemeldet (Issue #16538), mind. bis 2.1.x.
**FIX:** Bei SessionStart-Plugin-Hooks Kontext via plain stdout ausgeben (SessionStart
surfaced plain stdout, s. 2.3), oder ueber UserPromptSubmit injizieren.

---

## 3. Hooks feuern nicht / Config wird still ignoriert

### 3.1 EIN JSON-Syntaxfehler killt die GESAMTE Config still  ⭐ HAEUFIG / Nr.1-Ursache
**Symptom:** Kein einziger Hook feuert, keinerlei Warnung.
**Ursache:** Ein einzelner Syntaxfehler (z.B. trailing comma) → Claude Code ignoriert die
ganze settings.json still. Auch falsch escapte Bash-Permission-Regex (Quotes in `|`-Alternation)
korrumpiert settings.local.json ("Files with errors are skipped entirely").
**Versionen:** per Design (Issue #33650).
**FIX:** `python3 -m json.tool ~/.claude/settings.json` validieren VOR dem Start. Backup vorher.
JSON nie mit sed/echo bearbeiten — Edit-Tool oder Python-json-Modul.

### 3.2 matcher `*` matcht nicht / Tool-Namen sind case-sensitive
**Symptom:** Hook feuert nie.
**Ursache:** `"*"` als Matcher hat einen bekannten Bug; Tool-Namen sind case-sensitiv
(`MultiEdit`, nicht `multiEdit`).
**Versionen:** gemeldet, Status offen.
**FIX:** Exakte Namen `Edit|Write` (bzw. `.*` statt `*`). Gueltige Tool-Namen u.a.: Bash,
Read, Write, Edit, Glob, Grep, WebSearch, WebFetch, TodoWrite, Skill, Task.

### 3.3 Relative Pfade / fehlendes `+x` / fehlender Shebang
**Symptom:** Hook bleibt still, ohne Fehler.
**Ursache:** Relativer Pfad statt absolut; `.sh` ohne Execute-Bit; fehlende Shebang-Zeile.
**Versionen:** per Design.
**FIX:** Absolute Pfade (`$CLAUDE_PROJECT_DIR`/`${CLAUDE_PLUGIN_ROOT}`/`$USERPROFILE`),
`chmod +x script.sh`, `#!/usr/bin/env bash` als erste Zeile.

### 3.4 PostToolUse Edit/Write feuert nie
**Symptom:** PostToolUse fuer Edit/Write wird nie aufgerufen (per `--debug` bestaetigt),
SessionStart in derselben Config feuert.
**Versionen:** v2.1.47 gemeldet (Issues #26962, #15441, #6305) — selektives Versagen nur bei Tool-Events.
**FIX (funktionserhaltend):** Datei-Aenderungen alternativ ueber einen `Bash`-Matcher oder
einen Stop-Hook nachbearbeiten, bis der Fix greift.

### 3.5 SessionStart-Hooks feuern nicht bei NEUEN Conversations
**Symptom:** SessionStart-Hook laeuft nur bei `/clear`, `/compact`, `resume` — nicht beim
echten Frischstart einer neuen Unterhaltung.
**Versionen:** gemeldet (Issues #10373, #12634).
**FIX:** Kritische Init-Logik nicht allein an SessionStart binden; ggf. zusaetzlich an
UserPromptSubmit (erste Nachricht) absichern.

### 3.6 Plugin-command-Hooks fuer Pre/PostToolUse werden still verworfen
**Symptom:** In `plugins/.../hooks/hooks.json` registrierte command-Hooks fuer Pre/PostToolUse
werden beim Laden stumm gedroppt; prompt-Hooks auf demselben Event funktionieren.
**Versionen:** 2026 gemeldet (Issues #34573, #27398, #14410), Status offen.
**FIX:** Solche command-Hooks in der user-`settings.json` definieren statt im Plugin.

### 3.7 `/hooks` zeigt "No hooks configured" trotz gueltiger Config
**Symptom:** Hooks stehen in settings.json, werden aber nicht geladen.
**Versionen:** gemeldet (Issue #11544).
**FIX:** Config validieren, Session neu starten; Pfade/Format gegen ein funktionierendes
Beispiel pruefen.

---

## 4. Settings-Caching

### 4.1 Hook-Aenderungen greifen erst nach Session-Neustart  ⭐ HAEUFIG
**Symptom:** settings.json editiert, der neue/geaenderte Hook tut nichts.
**Ursache:** Die Hook-Config wird beim Start gecacht; der File-Watcher verpasst Aenderungen gelegentlich.
**Versionen:** per Design (Issue #22679).
**FIX:** Session neu starten (`/clear` oder Neustart). So entsteht der Eindruck "Hook kaputt",
obwohl er nur noch nicht geladen ist — beim Debuggen ZUERST ausschliessen.

---

## 5. stdin / Environment

### 5.1 Shell-Profil-Output zerstoert das stdin-/stdout-JSON
**Symptom:** Der Hook bekommt kaputtes/un-parsebares stdin, oder sein JSON-stdout wird mit
Profil-Bannern vermischt → Parse-Fehler.
**Ursache:** Hooks laufen in non-interaktiven Shells, die `.bashrc`/`.zshrc`/PowerShell-Profil
sourcen; unbedingte `echo`/Banner werden dem Hook vorangestellt.
**Versionen:** per Design.
**FIX:** Profil-Ausgaben mit `[[ $- == *i* ]]`-Guard kapseln (nur interaktiv). Hooks mit
`pwsh -NoProfile` bzw. `bash --noprofile --norc` starten. Nur reines JSON auf stdout.

### 5.2 `$CLAUDE_TOOL_INPUT` / Env-Variablen sind leer
**Symptom:** Erwartete Hook-Env-Variablen sind leer.
**Ursache:** Tool-Input kommt NICHT als Env-Variable, sondern als stdin-JSON.
**Versionen:** per Design (Issue #9567).
**FIX:** `tool_input`, `cwd`, `session_id`, `transcript_path` aus dem stdin-JSON parsen.

---

## 6. Stop-Hook-Endlosschleifen

### 6.1 Stop-Hook ohne `stop_hook_active`-Check = Endlosschleife  ⭐ KRITISCH
**Symptom:** Die Session laeuft endlos weiter (bis ~50 min Session-Limit verbrannt sind).
**Ursache:** Der Stop-Hook feuert nach JEDER Antwort. Blockt er, macht Claude weiter → Stop
feuert erneut → Endlosschleife. Auch ausgeloest durch `{"continue":true}`/`{"ok":false}`.
**Versionen:** per Design; konkreter Loop in Issues #55754, #10205. Seit v2.1.147 endet der
Turn mit Warnung nach 8 Blocks (`CLAUDE_CODE_STOP_HOOK_BLOCK_CAP` ueberschreibbar) — der
Loop bleibt aber teuer.
**FIX:** Im Stop-Hook ZUERST `stop_hook_active` aus dem Input pruefen — wenn `true`, sofort
`exit 0` (Claude darf stoppen). Erst danach eigene Logik. Stop-Hooks idempotent halten.

### 6.2 Stop-Hook haengt unendlich bei langsamem API-Call im Hook
**Symptom:** Claude Code friert komplett ein.
**Ursache:** Der Hook ruft selbst ein langsames LLM/API auf; bei Netz/Rate-Limit kein
Timeout → blockiert ewig.
**Versionen:** gemeldet (superpowers #390).
**FIX:** Eigenes Timeout im Hook (`timeout`-Wrapper), Fallback bei Fehler, `exit 0`.

---

## 7. Phantom-Events & SubagentStop

### 7.1 SubagentStop liefert KEIN `agent_id` — Subagent nicht identifizierbar
**Symptom:** Bei mehreren parallelen Subagents teilen sich alle dieselbe `session_id`; der
Hook weiss nicht, welcher fertig ist.
**Ursache:** Die Payload enthaelt nur die Session-ID, kein subagent-spezifisches Feld (offener Feature-Request).
**Versionen:** offen (Issues #7881, #14859).
**FIX (funktionserhaltend):** Jeden Subagent eine eigene Marker-/Sentinel-Datei schreiben
lassen und im Hook diese Datei matchen, statt auf `agent_id` zu setzen.

### 7.2 Stop/SubagentStop feuern auch ohne echten Trigger — Input-Guard noetig
**Symptom:** Der Hook laeuft bei jedem Stop-Event mit und schreibt Spam (lokal: 50+ Fires/Session).
**Ursache:** Ohne Pruefung reagiert der Hook auf jedes Stop-Event.
**Versionen:** per Design (lokaler Vorfall 2026-04-15/18).
**FIX:** Am Hook-Anfang pruefen, ob die erwarteten Felder existieren; sonst sofort `exit 0`
ohne Side-Effect. AUSNAHME: rein passive Kontext-Injection-Hooks (kein Write) brauchen
keinen Guard — er wuerde legitime Injection verhindern.

### 7.3 UserPromptSubmit feuert faelschlich, wenn ein Subagent/Task fertig wird
**Symptom:** UserPromptSubmit-Hook laeuft, obwohl kein echter User-Prompt vorliegt.
**Versionen:** gemeldet (Issue #16952).
**FIX:** Im UserPromptSubmit-Hook pruefen, ob wirklich ein User-Prompt vorliegt.

---

## 8. prompt-Typ vs command-Typ

### 8.1 `type:"prompt"` funktioniert NICHT bei SessionStart/SessionEnd
**Symptom:** Ein prompt-Hook bei SessionStart wird stumm nicht ausgefuehrt
("ToolUseContext is required for prompt hooks").
**Ursache:** Bei SessionStart/SessionEnd gibt es keinen ToolUseContext.
**Versionen:** per Design (Issues #54532, #39184).
**FIX:** Bei SessionStart/SessionEnd `type:"command"` verwenden.

### 8.2 `type:"prompt"` + `command`-Key bricht ALLE Hooks still ab
**Symptom:** Saemtliche Hooks feuern nicht mehr, keine Fehlermeldung.
**Ursache:** Typ/Key-Verwechslung (prompt-Hook mit `command`-Feld) macht die Hook-Config ungueltig.
**Versionen:** per Design (lokal als `feedback_hook_type_confusion`).
**FIX:** `type` und Key konsistent halten: `prompt`+`prompt`, `command`+`command`.

---

## 9. matcher-Patterns

### 9.1 Fehlender Matcher = laeuft fuer ALLES
**Symptom:** Hook feuert unerwartet bei jedem Tool.
**Ursache:** `"*"`, `""` oder weggelassener `matcher` = Match-all. Nur Buchstaben/Ziffern/`_`/`|`
= exakt oder `|`-Liste; alles andere wird als JS-Regex interpretiert.
**Versionen:** per Design.
**FIX:** Bewusst exakten Matcher setzen (`Edit|Write`).

### 9.2 MCP-Matcher ohne `.*`-Suffix matcht NICHTS
**Symptom:** `"matcher":"mcp__memory"` feuert nie.
**Ursache:** MCP-Tools heissen `mcp__<server>__<tool>` — exaktes `mcp__memory` matcht keinen vollen Namen.
**Versionen:** per Design.
**FIX:** `mcp__memory__.*` (alle Tools des Servers) oder `mcp__.*__write.*`.

### 9.3 `matcher` wird bei Nicht-Tool-Events still ignoriert
**Symptom:** Ein Matcher an UserPromptSubmit/Stop/TaskCreated hat keine Wirkung.
**Ursache:** Diese Events feuern immer; der `matcher` wird ignoriert (kein Fehler).
**Versionen:** per Design. AUSNAHME SessionStart: nutzt `matcher` fuer `source`
(`startup`/`resume`/`clear`/`compact`) — aber Vorsicht (s. 11.4).
**FIX:** Nicht auf Matcher-Filterung verlassen; im Hook selbst per Input-Feldern filtern.

---

## 10. Timeout, async & Performance

### 10.1 UserPromptSubmit-Timeout ist nur 30s (statt 600s)
**Symptom:** UserPromptSubmit-Hook timeoutet, besonders in CI/`-p`.
**Ursache:** Default 30s, weil er jeden Prompt blockt.
**Versionen:** per Design.
**FIX:** Explizites `timeout`-Feld setzen, falls laenger noetig.

### 10.2 `async:true` ist NICHT echt non-blocking
**Symptom:** Der Agent-Loop blockiert trotz `async`.
**Ursache:** `async` respektiert weiterhin das Timeout (default 600s). Ein haengender HTTP-Call
friert den Agent minutenlang ein.
**Versionen:** per Design.
**FIX:** `timeout` reduzieren; `async` nur fuer unkritische Hintergrund-Notifications.
Blocker NIE `async`. `asyncRewake:true` weckt Claude bei `exit 2` mit stderr.

### 10.3 Viele Hooks = mehrere Sekunden Latenz pro Prompt
**Symptom:** ~18-21s pro Prompt im Projekt vs. ~5s ausserhalb.
**Ursache:** 11+ Hooks ueber viele Events, jeder spawnt einen Prozess → kumulative Startlatenz.
**Versionen:** per Design (ruflo #1530).
**FIX (funktionserhaltend):** `async:true` fuer unkritische Hooks; mehrere Checks in EINEN
Hook bundeln statt viele Prozesse; Baseline mit `disableAllHooks:true` messen.

### 10.4 Output-Cap ~10.000 Zeichen
**Symptom:** Langer Hook-Output erscheint abgeschnitten.
**Ursache:** Output-Limit pro Hook.
**Versionen:** per Design.
**FIX:** Automatisch — Claude schreibt den Volltext in eine Session-Datei + Preview mit Pfad.
Bewusst kurz halten, grosse Daten in Datei auslagern.

---

## 11. Aktuelle Versions-Bugs & Regressionen (v2.1.x — auf 2.1.159 pruefen)

### 11.1 Falsche "Hook Error"-Labels trotz `exit 0`  🆕 AKTUELL / HAEUFIG
**Symptom:** Das Transcript zeigt bei JEDEM Tool-Call "hook error", obwohl der Hook mit
`exit 0` und gueltigem JSON endet. Das falsche Label kann Claude dazu bringen, Turns vorzeitig zu beenden.
**Ursache:** Regression im Hook-Error-Reporting — jede Hook-Ausgabe wird als "error" gelabelt,
unabhaengig von Exit-Code/stdout/stderr.
**Versionen:** v2.0.28 bis v2.1.x, mehrfach gemeldet (Issues #34859, #34713, #17088, #44943) — Status offen.
**FIX (funktionserhaltend):** `suppressOutput:true` setzen / stderr leer halten; Hook trotzdem
`exit 0`. Das Label NICHT als echten Fehler behandeln. Auf gefixte Version updaten, wenn verfuegbar.

### 11.2 Stop-Hook-Endlosschleife — Loop-Cap erst ab v2.1.147
- Siehe 6.1. Vor v2.1.147 kein Cap → ganze Session konnte verbrennen. Gefixt (Cap=8) ab **v2.1.147**.

### 11.3 Hook-Output korrumpierte interaktive Prompts — gefixt ab v2.1.151
**Symptom (alt):** Hook-Output ins Terminal zerstoerte eine sichtbare interaktive Eingabe.
**FIX/Status:** Hooks haben seit **v2.1.151** keinen Terminal-Zugriff mehr; fuer Bells/
Notifications `terminalSequence` im JSON nutzen (OSC 0/1/2/9/99/777 + BEL).

### 11.4 SessionStart `source="startup"` statt `"clear"` nach /clear
**Symptom:** Eine Hook-Gruppe mit `"matcher":"clear"` matcht nach `/clear` nie (v.a. VS-Code-Ext).
**Ursache:** `/clear` ruft createSession() ohne Args → wirkt wie Frischstart (`startup`).
**Versionen:** gemeldet (Issues #49937, #26794).
**FIX:** Nicht allein auf `source=clear` verlassen; `startup` mitbehandeln.

### 11.5 PreToolUse + ToolSearch-Deferral = Agent-Hang
**Symptom:** Der Agent haengt nach deferred Tool-Loading, wenn PreToolUse-Hooks aktiv sind.
**Ursache:** Zusammenspiel von ToolSearch (`ENABLE_TOOL_SEARCH`) und PreToolUse.
**Versionen:** gemeldet (Issue #33073).
**FIX:** PreToolUse-Scope verengen oder ToolSearch fuer betroffene Tools deaktivieren.

### 11.6 `modifiedInput`/`updatedInput` fuer das Agent-Tool wird verworfen
**Symptom:** PreToolUse-Hook setzt `updatedInput` fuers Task/Agent-Tool — es wird ignoriert
(`additionalContext` aus demselben Hook kommt aber an).
**Versionen:** gemeldet (Issue #39814), mind. bis 2.1.x.
**FIX:** Beim Agent-Tool nicht auf Input-Rewrite verlassen; `additionalContext` nutzen oder
per `deny`+Begruendung umlenken.

---

## 12. Windows / PowerShell

### 12.1 UTF-8 BOM in settings.json bricht den JSON-Parse  ⭐ HAEUFIG (uns 2x getroffen)
**Symptom:** Hooks (und MCP) werden still ignoriert; Claude faellt auf Defaults zurueck, ohne
Fehlermeldung. Beim eigenen Lesen per Python: `Unexpected UTF-8 BOM (decode using utf-8-sig)`.
**Ursache:** PowerShell `Out-File`/`>`/`Set-Content` (PS 5.1) schreibt UTF-8 MIT BOM
(`EF BB BF`); strikte JSON-Parser verweigern ein BOM am Dateianfang.
**Versionen:** Windows, per Design (Issue #9906; lokal 2026-05-22 und 2026-06-01).
**FIX:** JSON nie mit `Out-File`/`>` schreiben. `Out-File -Encoding UTF8NoBOM` (PS6+) oder
`[System.IO.File]::WriteAllText($p,$json,(New-Object System.Text.UTF8Encoding $False))`.
Beim eigenen Python-Lesen `encoding='utf-8-sig'` (BOM-tolerant). Bestehende Datei BOM-strippen.

### 12.2 `powershell.exe` (5.1) statt `pwsh` schlaegt fehl / korrumpiert UTF-8
**Symptom:** "module could not be loaded"; Em-Dash/Umlaute werden zerstoert.
**Ursache:** Windows PowerShell 5.1 laedt Module im Claude-Exec-Kontext nicht und arbeitet
in cp1252 (kein 3-Byte-UTF-8).
**Versionen:** Windows.
**FIX:** IMMER `pwsh` (PowerShell 7+), nie `powershell.exe`. Im Code `--` statt `—`. Fuer
Umlaute `[Console]::OutputEncoding=[Text.Encoding]::UTF8`.

### 12.3 Inline `pwsh -Command` scheitert am Git-Bash-Quoting
**Symptom:** Hook-Command bricht; `$_` wird von Bash vor pwsh expandiert; `C:\Users` → `C:Users`.
**Ursache:** Git Bash interpretiert `\g \L \s \c` als Escapes, bevor pwsh sie sieht.
**Versionen:** Windows + Git Bash, per Design.
**FIX:** Nie inline `-Command`. Immer `pwsh -NoProfile -File script.ps1` mit Forward-Slash-Pfaden
(`C:/...`, akzeptiert PowerShell).

### 12.4 Stop-Hook bekommt kein stdin auf Windows/pwsh
**Symptom:** Stop-Hook erhaelt leeres stdin; `[Console]::In.ReadToEnd()` liefert "",
`IsInputRedirected`=False. UserPromptSubmit mit gleicher pwsh-Config funktioniert.
**Ursache:** stdin wird bei bestimmten Hook-Typen (Stop) auf Windows/pwsh nicht durchgereicht.
**Versionen:** Windows, v2.1.101 gemeldet (Issue #46601).
**FIX:** Leeres stdin sauber abfangen (`exit 0` statt Crash). BEIDE Lesewege versuchen:
erst `[Console]::In.ReadToEnd()`, dann `$input | Out-String` (deckt unterschiedliche Invokationen ab).

### 12.5 pwsh nicht im PATH haengt den Start
**Symptom:** Claude Code (aus Git Bash) haengt beim Start; Worker-Hooks brechen.
**Ursache:** Der Hook ruft `pwsh` als nackten Befehl, der nicht im PATH ist.
**Versionen:** Windows (claude-mem #1062).
**FIX:** Vollen Pfad zu pwsh verwenden oder PATH verifizieren; MCP/Hook-Configs mit absoluten Pfaden.

### 12.6 SessionStart-Hook spawnt `powershell.exe` → Tastatur im TUI tot
**Symptom:** Nach dem SessionStart-Hook reagiert die Claude-Code-TUI nicht mehr auf Tastatur.
**Ursache:** Node spawnt powershell.exe als Child, das den Console-Input-Mode aendert und
beim Beenden Non-Raw-Mode hinterlaesst; Claude restauriert Raw-Mode nicht.
**Versionen:** Windows (Issue #26586).
**FIX:** Kurze, nicht-interaktive `pwsh`-Skripte; kein Spawn von `powershell.exe` aus SessionStart.

### 12.7 Parallele Sessions korrumpieren `.claude.json`
**Symptom:** "JSON Parse error: Unexpected EOF"; Config kaputt.
**Ursache:** Mehrere parallele Claude-Instanzen schreiben gleichzeitig (kein atomares Write/Lock).
**Versionen:** Windows (Issue #28806). ✅ **Hotfix gefixt ab v2.1.61** — auf 2.1.159 nicht mehr relevant.
**FIX:** Eigene Config-Schreiber atomar machen (temp-Datei + `os.replace`/rename); nicht
mehrere Sessions gleichzeitig dieselbe Config schreiben lassen.

---

## 13. macOS / Linux / Bash

### 13.1 Plugin-Hooks ohne Execute-Bit (Cloud-Sync/Git)
**Symptom:** "Permission denied: bash .../hook.sh"; der Hook laeuft gar nicht.
**Ursache:** pCloud/Dropbox/iCloud/Git bewahren das Unix-`+x`-Bit nicht; Plugins werden ohne `+x` installiert.
**Versionen:** macOS/Linux (Issue #20818).
**FIX:** `chmod +x` auf die Hook-Skripte; ein SessionStart-Self-Heal, der alle Plugin-`*.sh`
per `find ... ! -perm -u+x -exec chmod +x {} +` ausfuehrbar macht (plugin-health-check-Pattern).

### 13.2 `jq` nicht installiert
**Symptom:** "jq: command not found"; der Hook (oft Stop) bricht.
**Ursache:** Plugins haben eine undokumentierte jq-Abhaengigkeit; jq fehlt v.a. auf Windows/Git-Bash
und frischen macOS-Systemen.
**Versionen:** beide (Issue #14817).
**FIX:** `brew install jq`/`apt-get install jq` ODER JSON in Python parsen (`json`-Modul)
statt jq. Eigene Hooks nie zwingend auf jq bauen.
**Eigener Vorfall (2026-06-01):** `bug-almanac-guard.sh` extrahierte den `file_path` mit
`jq`; fehlt jq, ist `file_path` leer → der Guard erkennt nie einen Bereich und feuert stumm
NIE (Sicherheitsnetz tot, ohne Fehlermeldung). Fix: `file_path` per
`python3 -c "json.load(sys.stdin)…"` statt jq (python3 war fuer den JSON-Output ohnehin
noetig → eine Abhaengigkeit weniger auf dem Hot Path). Beim Hook-Debugging gefunden.

### 13.3 CRLF → "bad interpreter: /usr/bin/env bash^M"
**Symptom:** Der `.sh`-Hook bricht zuverlaessig.
**Ursache:** Git `core.autocrlf=true` macht aus LF CRLF; der Kernel sucht `bash\r`.
**Versionen:** macOS/Linux (Datei kam von Windows), per Design.
**FIX:** `.gitattributes`: `*.sh text eol=lf`. Bestehende: `sed -i 's/\r$//' *.sh` bzw. `dos2unix`.
Git global `core.autocrlf=input`.

### 13.4 `set -e` killt den Hook beim ersten harmlosen Nicht-0-Exit
**Symptom:** Der Bash-Hook bricht scheinbar grundlos mittendrin ab (z.B. `grep` ohne Treffer = exit 1).
**Ursache:** `set -e` beendet das Skript bei jedem Nicht-0-Exit; viele Tools liefern legitim Nicht-0.
**Versionen:** macOS/Linux Bash.
**FIX:** Kritische Stellen mit `|| true` absichern; bewusst am Ende `exit 0`. (Trap fuer ERR
zum Loggen ist ok, darf aber den Hook nicht mit non-zero beenden.)
**Eigener Vorfall (2026-06-01):** `bug-almanac-{guard,index}.sh` hatten `trap 'log' ERR`
OHNE `exit 0`. Bei einem scheiternden Befehl (python3) loggte der trap, dann beendete
`set -e` das Skript mit non-zero → "hook error". Fix: **`exit 0` IN den trap** packen:
`trap 'hook_log_warn "…"; exit 0' ERR`. So endet der Hook bei JEDEM unerwarteten Fehler
graceful — genau wie die PS1-Variante via `try/catch` + finalem `exit 0`. Beim Debugging gefunden.

---

## 14. Security-Fallen

### 14.1 MCP-Tool als Hook fuer Policy-Enforcement = umgehbar
**Symptom:** Bei MCP-Disconnect produziert der Hook nur einen non-blockierenden Fehler → die
Aktion laeuft durch.
**Ursache:** MCP-Tool-Hooks haengen an einer Verbindung, die fehlen kann (auf SessionStart/Setup
oft "not connected").
**FIX:** Harte Policy IMMER `type:"command"`; MCP-Tool-Hooks nur fuer Observability.

### 14.2 `disableAllHooks` deaktiviert KEINE managed/org-Hooks
**Symptom:** `disableAllHooks:true` gesetzt, org-deployte Hooks laufen weiter.
**Ursache:** Das Flag wirkt nur auf user/project-Hooks.
**Versionen:** per Design (Issue #26637).
**FIX:** Bewusst sein, dass org-Hooks separat verwaltet werden.

---

## 15. Fix-Status — was auf v2.1.159 schon behoben ist

> Wichtig fuers Versions-Denken: Diese Eintraege waren frueher Bugs, sind aber in der
> aktuell installierten Version (2.1.159) bereits GEFIXT. Sie gelten nur noch fuer
> aeltere Versionen — auf 2.1.159 NICHT mehr als aktive Bugs behandeln (Changelog-belegt,
> Stand 2026-06-01).

| Frueherer Bug | Gefixt ab | Almanach-Bezug |
|---------------|-----------|----------------|
| Hook-Output korrumpiert interaktiven Prompt (Hooks hatten Terminal-Zugriff) | **v2.1.141** | 11.3 |
| `transcript_path` ungueltig nach EnterWorktree / CWD-Wechsel | **v2.1.141** | — |
| Stop-Hook-Endlosschleife laeuft ewig (Block-Cap 8 eingefuehrt) | **v2.1.143** | 6.1, 11.2 |
| Hook-`if`-Conditions (`PowerShell(git push*)` / `Bash(...)`) matchten nie | **v2.1.147** | 9.x (matcher-conditions) |
| Plugin-`Stop`/`UserPromptSubmit` brechen bei Cache-Cleanup ab | **v2.1.147** | 3.6-nah |
| `.claude.json`-Korruption bei parallelen Sessions (Windows) | **v2.1.61** | 12.7 |
| `/goal` haengt still bei `disableAllHooks` / `allowManagedHooksOnly` | **v2.1.140** | 14.2-nah |
| Symlinkte Settings loesen falsche `ConfigChange`-Hooks aus | **v2.1.140** | — |

### Noch NICHT gefixt auf v2.1.159 (Workaround bleibt aktiv)
Per Design oder noch offen — Loesungen oben weiter anwenden:
- exit 1 blockiert nicht / nur exit 2 (1.1) — per Design.
- Flaches `additionalContext` ohne `hookSpecificOutput` ignoriert (2.1) — per Design.
- `type:"prompt"` nicht bei SessionStart/SessionEnd (8.1) — per Design.
- stdin dual-read noetig (Console.In vs `$input`) (12.4) — noch offen.
- UTF-8 BOM in settings.json (12.1, #9906) — noch offen.
- Falsche "hook error"-Labels trotz exit 0 (11.1, #34859/#34713) — kein Fix belegt, wahrsch. offen.
- v2.1.123 Bash-Matcher droppt Kontext (2.4, #55889) — wahrsch. offen; betrifft v.a. `Bash`-Matcher, `Edit|Write` evtl. nicht.
- PreToolUse exit 2 blockt Write/Edit nicht (1.6, #13744 als Duplikat geschlossen, kein eigener Fix belegt).

**Methodik-Hinweis:** Der GitHub-Issue-Status liess sich nur eingeschraenkt verifizieren
(github.com per WebFetch blockiert — nur Such-Snippets). Die "gefixt"-Angaben sind
Changelog-belegt; die "noch offen"-Angaben sind teils "kein Fix gefunden" und vorsichtig
zu behandeln. Bei naechster Re-Recherche `gh issue view <nr>` nutzen fuer harten Status.

---

## Pflicht-Checkliste vor Hook-Arbeit
- [ ] Diese Datei komplett gelesen, Stand-Datum gegen `claude --version` abgeglichen?
- [ ] Hook endet mit `exit 0` (oder bewusst `exit 2` zum Blocken — NIE `exit 1`)?
- [ ] Kontext via `hookSpecificOutput.{hookEventName,additionalContext}` (PS: `-Depth 5`)?
- [ ] stdin robust gelesen (beide Wege) + Guard fuer leeres/falsches Input?
- [ ] Bei Side-Effect-Hooks (SubagentStop): Input-Guard? Bei passiver Injection: KEIN Guard?
- [ ] settings.json nach Aenderung mit `python3 -m json.tool` validiert + Session neu gestartet?
- [ ] Cross-Platform: `.ps1` UND `.sh`, `pwsh` (nicht `powershell.exe`), Forward-Slash-Pfade,
      `.sh` mit `+x` und LF?
- [ ] Kein `type:"prompt"` bei SessionStart/SessionEnd?
- [ ] Matcher bewusst gesetzt (`Edit|Write`, MCP mit `.*`)?
