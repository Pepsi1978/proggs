# Bekannte Bugs & Fallen: Claude Code Hooks

> **PFLICHT-LESEN vor JEDER Arbeit an einem Claude-Code-Hook** (`.ps1`/`.sh` in
> `~/.claude/hooks/`, `settings.json`-`hooks`-Sektion, Plugin-`hooks.json`).
> Kuratiert aus offizieller Doku + Changelog, GitHub-Issues (anthropics/claude-code),
> Community (Medium/dev.to/Blogs) und eigenen Vorfaellen. Loesungen sind
> funktionserhaltend (nie "Feature weglassen").
>
> **Stand:** re-recherchiert am **2026-06-15** fuer Claude Code **v2.1.177** (7-Researcher-Schwarm,
> **Anker:** claude-code=2.1.177  <!-- maschinenlesbar fuer check-version-anchor.py -->
> Issue-Status HART per `gh` verifiziert; Vorgaenger-Stand war 2026-06-01 / v2.1.159). Versionsangaben
> pro Bug beachten — viele "per Design"-Fallen gelten dauerhaft, einige Bugs sind in
> neueren Versionen gefixt, einige Regressionen sind in der v2.1.x-Reihe noch offen.
> NEU 2026-06-15: §16 (CLI-Crash bei non-spec JSON, jq-Control-Char-Bypass, Tool-Hooks im
> Subagent, systemMessage-Drop, Windows-Pfade-mit-Leerzeichen) + ueberarbeitete Fix-Status (§15).

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): **Hochrisiko-Bereich (Stufe C)** — vor echter
> Arbeit hier ist der VOLLTEXT Pflicht (`Read` ohne `limit`); dieser Kurzcheck dient nur der
> Schnell-Orientierung. Bei JEDEM Fehler im Bereich gilt ebenfalls Volltext-Pflicht (Stufe B).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Hook soll blockieren | `exit 2` zum Blocken, NIE `exit 1` (blockiert nicht) | §1.1 |
| 2 | Hook endet normal | Standalone-Hook IMMER mit `exit 0` beenden | §1.1 |
| 3 | Kontext an Claude geben | Nur `hookSpecificOutput.{hookEventName,additionalContext}` | §2.1 |
| 4 | PowerShell-JSON-Output | `ConvertTo-Json -Depth 5` (sonst flach) | §2.2 |
| 5 | Hook tut nichts nach Edit | Session neu starten — Config ist gecacht | §4.1 |
| 6 | settings.json geaendert | Ein JSON-Fehler killt ALLE Hooks still — validieren | §3.1 |
| 7 | stdin (Event-JSON) lesen | Robust lesen, bei leer/falsch sauber `exit 0` | §5.2 |
| 8 | Windows: settings schreiben | UTF-8-BOM bricht Parse — BOM-frei speichern | §12.1 |
| 9 | Windows-Hook starten | Immer `pwsh`, nie `powershell.exe` | §12.2 |
| 10 | Stop-Hook bauen | ZUERST `stop_hook_active` pruefen (sonst Endlosschleife) | §6.1 |
| 11 | SubagentStop/Stop-Hook | Input-Guard noetig (feuert auch ohne Trigger) | §7.2 |
| 12 | SessionStart-Hook | Kein `type:"prompt"` (kein ToolUseContext) | §8.1 |
| 13 | matcher setzen | Exakt `Edit|Write`, MCP mit `.*`-Suffix | §9.1 |
| 14 | Bash-`.sh`-Hook | `+x`, LF (kein CRLF), kein zwingendes `jq` | §13.2 |
| 15 | "Hook error" trotz exit 0 | Falsches Label (Regression) — nicht als Fehler werten | §11.1 |
| 16 | Bash rundet Float → zeigt 0 | `printf "%.0f"` (bash 3.2) scheitert an `55.00000000000001` → Parameter-Expansion | §13.5 |
| 17 | Statusline-rate_limit nach Account-Wechsel falsch | `account_fp` aus `~/.claude.json` (accountUuid), NICHT `.credentials.json` (macOS=Keychain → fehlt) | §13.6 |
| 18 | Dot-sourced `.sh`-Bibliothek (hook-log/whiteboard-insert) | NIE top-level `exit` darin — killt in bash den `source`-Aufrufer (PS harmlos → Bug nur auf macOS) | §13.7 |
| 19 | Hook gibt `hookSpecificOutput` aus | STRIKT spec-konform bauen — non-spec JSON crasht die ganze Session (TypeError, keine Recovery) | §16.1 |
| 20 | Hook liest stdin (Security-Guard) | NIE `jq` — Control-Chars im stdin-JSON brechen jq → Guard wird STILL umgangen; `python json.loads` + fail-closed | §16.2 |
| 21 | Tool-Hook soll im Subagent feuern | PreToolUse/PostToolUse feuern NICHT fuer Tool-Calls IN Subagents — SubagentStart/Stop nutzen | §16.3 |
| 22 | Windows: `.sh`-Hook-Pfad mit Leerzeichen | Pfad in settings.json `"..."` quoten + Forward-Slashes + voller Interpreter-Pfad (sonst Arg-Splitting) | §16.6 |
| 23 | Statusline zeigt nach langer Pause falsche 5h/7d-Werte | Uralte `state/rate-limits-*.json` gewinnen das MAX. Beim LESEN Eintraege mit **abgelaufenem** 5h-Fenster verwerfen + Cleanup deterministisch per Marker statt Modulo-Lotterie | §13.8 |
| 24 | Hook laeuft rc=0, tut aber NICHTS | `timeout` fehlt auf macOS (GNU-coreutils); `\|\| true` schluckt rc=127 → ganzer Block tot | §13.9 |
| 25 | Log voll `ERROR … command failed at line N` | Letzter Befehl einer Funktion ist ein Test → Funktion gibt 1 zurueck; `return 0` ergaenzen | §13.10 |
| 26 | `echo` scheitert in async SessionEnd-Hook | stdout schon zu → EPIPE ist der Normalfall; `echo … \|\| true` | §13.11 |
| 27 | Geteilte Projekt-`settings.json`, zwei Plattformen | NIE absoluten Plattform-Pfad hart kodieren — `uname`-Weiche im `command` | §16.5 |
| 28 | Hook liest stdin per `python3 - <<'EOF'` | Heredoc belegt stdin → Daten kommen NIE an, Hook waehlt still den Default; `python3 -c` nutzen | §13.12 |
| 29 | Hook schreibt Diagnose ins Whiteboard | NIE konstanten Fehlertext — echte Fehlerart auswerten, sonst falsche Erinnerungen | §13.13 |

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
(`EF BB BF`); strikte JSON-Parser verweigern ein BOM am Dateianfang. **Zweite, tückische
Quelle (2026-06-15 isoliert):** `[System.IO.File]::WriteAllText($p,$json,[System.Text.Encoding]::UTF8)`
schreibt EBENFALLS einen BOM — denn `[System.Text.Encoding]::UTF8` ist `UTF8Encoding($true)`
(emit BOM = an). Die naheliegende, "saubere" .NET-Methode ist also die Falle. KORREKT ist nur
`(New-Object System.Text.UTF8Encoding $false)`. Da `WriteAllText` den Inhalt 1:1 schreibt
(z.B. nach `$raw -replace …`), bleibt die TAB-Einrückung erhalten — am TAB+BOM-Muster erkennt
man diese Quelle (ConvertTo-Json/biome wuerden Spaces erzeugen).
**Versionen:** Windows, per Design (Issue #9906; lokal 2026-05-22, 2026-06-01).
**Eigener Vorfall (2026-06-15):** `session-guard.ps1` reparierte defaultMode/effortLevel/model
in `settings.json` per `$raw -replace` + `WriteAllText(…, [System.Text.Encoding]::UTF8)` an 4
Stellen → bei JEDEM echten Neustart kam ein BOM in settings.json (TAB-Einrückung verraten von
Claude Codes eigenem Writer, BOM von session-guard). Empirisch bewiesen (`EF BB BF` vs `7B 0A 09`),
alle 4 Stellen + `redact-settings-reference.ps1` auf `UTF8Encoding $false` umgestellt. Der
`poka-yoke-json-validate`-Hook (BOM-Stripper) bleibt als Defense-in-Depth-Schicht 2. Hinweis:
`Out-File -Encoding UTF8` ist in **pwsh7 BOM-frei** (nur PS 5.1 schreibt damit BOM) — der
Verursacher war hier NICHT Out-File, sondern WriteAllText mit dem falschen Encoding-Objekt.
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

### 13.5 `printf "%.0f"` (bash-3.2-builtin) gibt 0 bei langen Float-Artefakten  ⭐
**Symptom:** Ein bash-Hook/eine Statusline zeigt `0` statt des echten gerundeten Wertes —
aber NUR bei manchen Werten, andere (glatte Ganzzahlen) funktionieren. Wirkt sporadisch
("ging am Anfang, dann nicht mehr").
**Ursache:** Die bash-3.2-builtin `printf "%.0f"` (macOS-System-bash) wirft bei einem
LANGEN Float wie `55.00000000000001` (typisches Floating-Point-Artefakt aus JSON/einer API)
`printf: invalid number` und gibt `0` (rc=1) zurueck. Glatte Werte (`36`, `55`) parst sie
problemlos. Locale-UNABHAENGIG — NICHT das de_DE-Komma-Problem (`,` vs `.`), sondern der
begrenzte Float-Parser der alten builtin printf. `/usr/bin/printf` und neuere bash koennen es.
**Versionen:** macOS bash 3.2 (System-Default `/bin/bash`), per Design.
**FIX (funktionserhaltend):** Float-Rundung NICHT mit `printf "%.0f"`. Reine
Parameter-Expansion (locale-unabhaengig + bash-3.2-fest): Ganzzahlteil `${v%%.*}` nehmen,
an erster Nachkommastelle (`${v#*.}`, `[5-9]` → +1) aufrunden. Alternativ
`LC_ALL=C awk 'BEGIN{printf "%.0f", v}'`. PowerShell `[int][Math]::Round(...)` ist nicht betroffen.
**Eigener Vorfall (2026-06-12):** `statusline.sh` zeigte `7d 0%` statt `55%`, weil die API
`seven_day.used_percentage` als `55.00000000000001` lieferte und dieser Float von der
MAX-Logik als hoechster Wert ausgewaehlt wurde; der 5h-Wert war eine glatte Ganzzahl → funktionierte.
Fix: `round_pct()` per Parameter-Expansion ersetzte alle 4 `printf "%.0f"`-Aufrufe.

### 13.6 Statusline-`account_fp` auf macOS IMMER "default" → 7d-rate_limit nach Account-Wechsel falsch  ⭐
**Symptom:** Die Statusline zeigt einen viel zu hohen 7-Tage-rate-limit (z.B. `7d 58%`), obwohl
das aktuell eingeloggte Konto frisch ist (echter Wert z.B. `2%`). Tritt typischerweise nach einem
**Account-Wechsel** auf. Der 5h-Wert ist korrekt — nur 7d ist falsch.
**Ursache (zwei verkettete Root Causes):**
1. **`account_fp` ist auf macOS IMMER "default".** Der Cross-Session-State (`~/.claude/state/rate-limits-*.json`)
   trennt Konten ueber einen Account-Fingerprint, der bislang aus `~/.claude/.credentials.json`
   gehasht wurde. **Auf macOS liegen die Credentials im Keychain — diese Datei existiert gar nicht**,
   also blieb `account_fp` immer `"default"` und die ganze Account-Trennung war wirkungslos.
2. **Das 7d-Fenster trennt nicht nach Konto.** Die MAX-Aggregation nimmt den hoechsten `seven_d`-Wert
   im selben `seven_d_resets`-Fenster. Der woechentliche Reset ist eine **kalendarische Konstante**
   (gleiche Uhrzeit/Wochentag) → altes UND neues Konto teilen denselben `seven_d_resets`-Timestamp.
   Der 16h alte 58%-Wert des Vortags-Kontos kaperte so den frischen 2%-Wert. (Beim 5h-Wert faellt es
   NICHT auf, weil sich die `five_h_resets`-Fenster zwischen Sessions unterscheiden → schon getrennt.)
**Versionen:** macOS (Keychain), per Design; verschaerft durch parallele Sessions + Account-Wechsel.
Latent auch auf Windows: der `.credentials.json`-Hash aendert sich bei JEDEM Token-Refresh
(alle paar Stunden) → der fp flappte und schloss eigene gueltige State-Files faelschlich aus.
**FIX (funktionserhaltend, 2 Schichten, lossless):**
1. **Root Cause:** `account_fp` BEVORZUGT aus `~/.claude.json` → `.oauthAccount.accountUuid` bilden
   (`jq -r '.oauthAccount.accountUuid // .userID // empty'`, dann hashen). Existiert auf BEIDEN
   Plattformen, ist global ueber alle parallelen Sessions konstant und wechselt nur bei echtem
   Account-Wechsel (stabil ueber Token-Refreshes). Fallback: `.credentials.json`-Datei-Hash (Windows), dann `"default"`.
2. **Defense fuer 7d:** Das 7d-MAX zusaetzlich auf Dateien begrenzen, deren `ts_seen` innerhalb von
   5h (18000s) der frischesten Session liegt — fuer den Fall, dass `account_fp` mal `"default"` bleibt.
   Parallele Live-Sessions refreshen sekuendlich (immer frisch, bleiben drin); tote Vortags-Sessions
   eines alten Kontos fallen raus. Lossless: kein echter aktueller Wert wird verworfen.
Der bestehende Fremdkonto-Cleanup (loescht State-Files mit fremdem fp) greift nach Fix 1 endlich
und raeumt die alten "default"-Reste proaktiv auf.
**Eigener Vorfall (2026-06-13):** Statusline zeigte `7d 58%` statt `2%` nach Account-Wechsel.
`account_fp` war auf macOS nie gesetzt (Keychain), 4 Vortags-State-Files (52-58%) teilten denselben
`seven_d_resets` wie die 2 frischen (2%) und gewannen das MAX. Fix in `statusline.{sh,ps1}` umgesetzt + verifiziert.

### 13.7 Top-Level `exit` in dot-sourced `.sh`-Bibliothek killt den Aufrufer (bash)  ⭐
**Symptom:** Mehrere Guard-/Hook-Skripte (config-guard.sh, bash-guard.sh, disk-guard.sh,
safety-gate.sh, memory-watchdog.sh …) tun NACH dem `source`-Aufruf einer geteilten
Bibliothek (`whiteboard-insert.sh`/`hook-log.sh`) NICHTS mehr — alles nach `source <lib>`
laeuft tot, ohne Fehlermeldung. Auf Windows (PS-Variante) faellt es NICHT auf.
**Ursache:** bash `source`/`.` fuehrt das Skript im AKTUELLEN Shell-Kontext aus. Ein
top-level `exit 0` in der Bibliothek beendet die AUFRUFENDE Shell, nicht nur die Lib.
In PowerShell ist ein top-level `exit` in `. lib.ps1` dagegen empirisch HARMLOS (der Aufrufer
laeuft weiter) — deshalb bleibt der Bug auf Windows unsichtbar und nur macOS/Linux sind betroffen.
Typisch eingeschleppt, wenn ein Tool/Selbstverbesserung die Regel "Standalone-Hooks brauchen
`exit 0` am Ende" blind auch auf BIBLIOTHEKEN anwendet (vgl. platform-and-paths: "exit 0 blind
zu 5 Hooks → 3 davon Bibliotheken → 15 Hooks still tot").
**Versionen:** bash per Design; PS harmlos. Eigener Vorfall 2026-06-15 (whiteboard-insert.ps1/.sh
hatten ein top-level `exit 0`; in bash killte das jeden sourcenden Hook, empirisch via
`bash -c 'source lib.sh; echo REACHED'` verifiziert — REACHED erschien nicht).
**FIX (funktionserhaltend):** Dot-sourced Bibliotheken (`hook-log.*`, `whiteboard-insert.*`)
enden nach ihren Funktionsdefinitionen OHNE top-level `exit`. Test:
`bash -c 'source lib.sh; echo REACHED'` — fehlt `REACHED`, killt die Lib den Aufrufer.
`hook-exit0-guard` muss Bibliotheken (`hook-log` etc.) vom exit-0-Zwang AUSNEHMEN
(`grep -v 'hook-log'` / `-notmatch 'hook-log'`), sonst erzwingt der Guard genau diesen Bug.
**Quelle:** eigener Vorfall 2026-06-15 (Hook-Drift-Aufloesung).

---

### 13.8 Statusline zeigt nach langer Rechner-Pause falsche rate-limit-Werte (uralte State-Files)  ⭐

**Symptom:** Nach wochenlanger Pause zeigt die Statusline beim Session-Start einen viel zu hohen
7-Tage-Wert (z.B. `7d 84%` statt `0%`) — und korrigiert sich **nach einer Weile von allein**.
Genau dieses "erst falsch, dann von selbst richtig" ist der Fingerabdruck dieses Bugs.

**Root Cause (zwei Fehler, die sich gegenseitig verstaerken):**

1. **Beim LESEN wurde nie geprueft, ob ein State-File noch lebt.** Der Cross-Session-State
   (`~/.claude/state/rate-limits-<sid>.json`) wird ueber alle Sessions per MAX aggregiert. Der
   Fenster-Filter nimmt das `seven_d_resets` der *frischesten* Datei als Referenz. Liegen beim
   Session-Start NUR Leichen da (eigene Session hat noch nichts geschrieben, weil `rate_limits`
   erst nach dem ersten API-Call im stdin steht), gilt die juengste **Leiche** als "die frischeste"
   und gewinnt das MAX. Sobald die eigene Session ihr File schreibt, kippt der Vergleich → der Wert
   wird "von allein" korrekt.
2. **Der 24h-Cleanup lief praktisch nie.** Bedingung war `if [ $((now_ts % 600)) -lt 2 ]` — ein
   2-Sekunden-Fenster alle 600 s, also **0,33 % Trefferchance pro Aufruf**. Das setzt voraus, dass
   die Statusline wirklich sekuendlich laeuft. Bei einem Rechner, der wochenlang stillsteht, wird
   das Fenster nie getroffen: Es lagen 58 Tage alte Files mit `seven_d: 84` herum.

**Fix (Defense in Depth, beide Schichten noetig):**

| Schicht | Wo | Was |
|---------|-----|-----|
| 1 — Anzeige (praeventiv) | jq-`$valid`-Filter (`.sh`) bzw. Lese-Schleife (`.ps1`) | Eintrag verwerfen wenn `five_h_resets` in der VERGANGENHEIT liegt **oder** `ts_seen` aelter als 5 h (18000 s). Lebende Sessions refreshen staendig und haben IMMER einen Reset in der Zukunft |
| 2 — Aufraeumen (reaktiv) | Cleanup-Block | Modulo-Lotterie ersetzt durch Marker-Datei `state/.last-cleanup`: aelter als 600 s (oder fehlend) → aufraeumen. Laeuft damit GARANTIERT beim ersten Aufruf nach einer Pause |

Schicht 1 allein macht die **Anzeige** korrekt (auch wenn Muell herumliegt), Schicht 2 verhindert die
Muellansammlung. Schicht 1 ist noetig, weil eine 20 h alte Leiche unter der 24-h-Cleanup-Grenze liegt
und trotzdem tot ist.

**Verlustfrei:** Verworfen wird ausschliesslich, was ein nachweislich abgelaufenes Zeitfenster
beschreibt — kein Wert eines laufenden Fensters kann verloren gehen. Die Performance-Sorge von
2026-05-09 bleibt gewahrt: das teure `find`/`Get-ChildItem` laeuft weiterhin hoechstens alle 10 Min,
pro Aufruf kostet nur ein `stat` auf EINE Datei.

**Erkennen (Diagnose in einem Befehl):**
```bash
now=$(date +%s); for f in ~/.claude/state/rate-limits-*.json; do
  jq -r --argjson n "$now" '"\(input_filename|split("/")|last)  ts_seen vor \((($n - .ts_seen)/3600)|floor)h  5h-Reset \(if .five_h_resets < $n then "ABGELAUFEN" else "laeuft" end)  7d=\(.seven_d)%"' "$f"
done
```

**Quelle:** eigener Vorfall 2026-08-26 (Frank-Bug-Report nach langer Rechner-Pause, Claude Code
2.1.246). Reproduziert, gefixt und verifiziert in `statusline.sh` + `statusline.ps1`.

---

### 13.9 `timeout` fehlt auf macOS — `|| true` macht daraus einen STILLEN Totalausfall  ⭐ KRITISCH

**Symptom:** Ein Hook laeuft scheinbar fehlerfrei (rc=0, kein stderr, kein Log-Eintrag), tut
aber faktisch NICHTS. Kein Fehler, keine Warnung, nichts im Transcript — der Hook ist tot und
niemand merkt es. Typischer Verlauf: Der Hook wurde auf Windows/Linux geschrieben und getestet,
auf macOS lief er nie.

**Ursache:** `timeout` ist Teil der **GNU coreutils** und auf macOS **NICHT vorhanden**
(`command -v timeout` liefert nichts; auch `gtimeout` nur nach `brew install coreutils`).
Der Aufruf scheitert mit `timeout: command not found` und rc=127. Steht dahinter — wie in
Hook-Code ueblich, um den Hook fail-open zu halten — ein `|| true`, wird aus "das Programm
existiert nicht" ein sauberes "alles in Ordnung". Bei einem Heredoc-Aufruf
(`timeout 2 python3 <<'PYEOF'`) faellt damit der **komplette** nachfolgende Codeblock aus,
nicht nur das Zeitlimit.

**Versionen:** macOS (jede), per Design. Auf Linux/Git-Bash-Windows unauffaellig — deshalb
faellt es beim Cross-Platform-Portieren regelmaessig durch.

**FIX (funktionserhaltend):** Timeout-Kommando erkennen statt voraussetzen. Ohne Zeitlimit
laufen ist immer besser als gar nicht laufen:
```bash
if command -v timeout  > /dev/null 2>&1; then _TO="timeout 2"
elif command -v gtimeout > /dev/null 2>&1; then _TO="gtimeout 2"
else _TO=""; fi
$_TO python3 <<'PYEOF' || true
```
`reindex-codebase.sh` macht das seit jeher vorbildlich — die Vorlage stand also im selben Ordner.

**Erkennen (Diagnose in einem Befehl):**
```bash
cd ~/.claude/hooks && grep -ln '(^|[^g])timeout ' *.sh | while read -r f; do
  grep -q 'command -v timeout' "$f" || echo "UNGESICHERT: $f"
done
```

**Eigener Vorfall (2026-08-26):** `antigen-matcher.sh` (`timeout 2 python3 <<'PYEOF' || true`)
— der gesamte Matcher lief auf macOS nie. `heartbeat.sh` (`timeout 30 brew outdated`) — `count`
blieb leer, wurde auf 0 gesetzt, der Check meldete IMMER "0 outdated"; real waren es 91 Pakete.
Beide gefixt und per `bash -x` verifiziert (vorher `+ timeout 2 python3`, nachher `+ python3`).

---

### 13.10 ERR-Trap meldet Falschfehler, wenn der letzte Befehl einer Funktion ein Test ist  ⭐

**Symptom:** Das Hook-Log fuellt sich mit `ERROR <hook>: command failed at line N — [ $? -eq 2 ]`
— bei JEDEM Aufruf, obwohl der Hook mit rc=0 endet und im Transcript nichts erscheint. Echte
Fehler gehen in der Masse unter (lokal 138 Falschmeldungen pro Session gegen ~12 echte).

**Ursache:** `hook-log.sh` registriert `trap '_hook_log_trap_handler $LINENO' ERR`. In bash gibt
eine Funktion den Exit-Code ihres LETZTEN Befehls zurueck. Endet sie auf einem Test wie
`[ $? -eq 2 ] && exit 2`, liefert sie im Normalfall (Bedingung nicht erfuellt) **1** — semantisch
"Fehler", gemeint war "alles gut". Der Trap hat also recht: der Rueckgabewert IST falsch. Nicht
die Sonde ist kaputt, sondern der Code, den sie misst.

**Versionen:** bash per Design (Funktions-Rueckgabewert = letzter Befehl).

**FIX (funktionserhaltend):** Den Trap NICHT abschwaechen (er ist die Observability-Schicht),
sondern den Rueckgabewert bewusst machen:
```bash
    if [ $? -eq 2 ]; then exit 2; fi
    return 0          # <- macht den Erfolgsfall explizit
}
```
Gilt fuer JEDE Funktion in einem Hook, der `hook-log.sh` sourct (lokal 36 Dateien). Faustregel:
endet eine Hook-Funktion auf `[ ... ]`, `grep`, `test` oder `[[ ... ]]`, gehoert ein `return 0`
darunter.

**Eigener Vorfall (2026-08-26):** `bash-guard.sh` / `check_forbidden`. Funktionalitaets-Diff nach
dem Fix: 9/9 Testfaelle unveraendert (4 harmlos frei, 5 gefaehrlich blockiert), Falschmeldungen 0.

---

### 13.11 `echo` in einem async SessionEnd-Hook bekommt EPIPE → Falschfehler im Log

**Symptom:** Ein SessionEnd-Hook loggt `ERROR <hook>: command failed at line N — echo "..."`.
Ein `echo` kann eigentlich nicht scheitern — hier schon.

**Ursache:** Bei `async: true` auf `SessionEnd` laeuft der Hook weiter, waehrend die Session
bereits abgebaut wird. Ist stdout dann geschlossen, liefert `echo` EPIPE und damit non-zero →
der ERR-Trap aus `hook-log.sh` (s. 13.10) meldet es als Fehler. Bei SessionEnd wird stdout
ohnehin nicht mehr angezeigt, der EPIPE ist also der ERWARTETE Normalfall.

**Versionen:** macOS/Linux, per Design.

**FIX (funktionserhaltend):** Die Ausgabe NICHT entfernen (sie greift, solange stdout lebt),
sondern den erwarteten EPIPE entschaerfen: `echo "..." || true`.

**Eigener Vorfall (2026-08-26):** `pending-admin-updates.sh`, 4 `echo`-Aufrufe, 12 Falschmeldungen
pro Session.

---

### 13.12 `python3 - <<'EOF'` in einer Pipe: das Heredoc belegt stdin, die Daten kommen NIE an  ⭐ STILL
**Symptom:** Ein Hook liest das Event-JSON per `printf '%s' "$INPUT" | python3 - <<'PY' … PY` und
klassifiziert JEDEN Fall als "unbekannt"/Standardwert. Kein Fehler, kein Log-Eintrag — der Hook
laeuft mit rc=0 durch und trifft nur immer die falsche Entscheidung.
**Ursache:** `python3 -` heisst "lies das SKRIPT von stdin". Das Heredoc liefert genau dieses
Skript — damit ist stdin belegt, und die per Pipe gelieferten Daten erreichen den Prozess nie.
`json.load(sys.stdin)` sieht nichts mehr (bzw. den Rest des Skripts), faellt in das `except` und
liefert den Default. Beide Konstrukte einzeln sind korrekt; nur ihre **Kombination** ist der Fehler.
**Versionen:** per Design (POSIX), jede bash/python3-Fassung.
**FIX (funktionserhaltend):** Skript per `python3 -c '…'` uebergeben, dann bleibt stdin fuer die
Daten frei — im `-c`-String nur doppelte Anfuehrungszeichen verwenden, damit das aeussere
`'…'`-Quoting haelt. Alternativ die Logik in eine eigene `.py`-Datei auslagern (Muster
`rule-size-guard.py`) und `… | python3 hook.py` aufrufen.
**PRAEVENTION:** Klassifizierende Hooks IMMER mit mehreren Eingaben gegentesten (echter Fall,
Gegenfall, kaputtes JSON) — ein einzelner Testfall trifft zufaellig den Default und sieht richtig aus.
**Quelle:** eigener Vorfall 2026-08-27 (`stopfailure-logger.sh`, beim Bau der Fehlerart-Erkennung
sofort im Test aufgefallen).

### 13.13 Hook schreibt einen fest verdrahteten Fehlertext → falsche Diagnose im Whiteboard  ⭐
**Symptom:** Im Whiteboard steht `StopFailure: API/Rate-Limit Error … Fix-Vorschlag: API-Key
pruefen`, im Detail-JSON derselben Zeile aber `"error":"authentication_failed"` und
`"Not logged in · Please run /login"`. Die Ursachensuche laeuft danach in die voellig falsche
Richtung (Rate-Limit statt fehlender Anmeldung).
**Ursache:** `stopfailure-logger.{sh,ps1}` setzte Titel, Status und Fix-Vorschlag als KONSTANTEN
Text zusammen und uebernahm nur das rohe JSON in die Details. Jeder StopFailure — egal welcher
Art — wurde damit als Rate-Limit etikettiert. Verschaerfend: der Status lautete pauschal
`TRANSIENT`, wodurch ein reparierbarer Anmeldefehler nie in der OFFEN-Liste landete und der
`invariant-check` ihn nicht zaehlte.
**Versionen:** eigener Hook, seit 2026-05-30 (Umstellung auf TRANSIENT) bis 2026-08-27.
**FIX (funktionserhaltend):** `error` aus dem Event-JSON auswerten und drei Faelle unterscheiden —
Anmeldung (`auth|login|credential|unauthorized|401`) → Titel "Nicht angemeldet", Status **OFFEN**,
Fix-Vorschlag verweist auf den Login-Abgleich (§3.9 in `claude-config.md`); Rate-Limit
(`rate|limit|quota|429`) → wie bisher TRANSIENT; sonst neutraler Titel mit der echten Fehlerart.
Fallback ohne python3 schreibt einen neutralen Eintrag statt einer falschen Behauptung.
**PRAEVENTION (Direktive #2):** Ein Hook, der eine Diagnose ins Whiteboard schreibt, darf sie NIE
konstant formulieren — sonst erzeugt er systematisch falsche Erinnerungen, die spaeter als Fakten
gelesen werden. Der Almanach-Eintrag ist dabei genauso wichtig wie der Fix selbst.
**Quelle:** eigener Vorfall 2026-08-27 (Suche nach einem vermeintlichen Login-Problem im
claude-mem-Observer, das in Wahrheit ein falsch etikettierter Eintrag war).

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

## 16. Re-Recherche 2026-06-15 (v2.1.177) — neue Bugs & Fallen

> Alle Issue-Status in diesem Abschnitt HART per `gh issue view` gegen anthropics/claude-code
> verifiziert (2026-06-15). `NOT_PLANNED` = won't fix → der Workaround bleibt DAUERHAFT aktiv
> (haertere Aussage als das fruehere "Status unklar"). `OPEN` = offen.

### 16.1 CLI-Hard-Crash bei non-spec `hookSpecificOutput`  ⭐ KRITISCH
**Symptom:** Ein Hook gibt ein `hookSpecificOutput` aus, das nicht spec-konform ist (falscher Typ,
fehlendes `hookEventName`, unerwartete Struktur) → die ganze Session crasht sofort mit TypeError,
keine Recovery.
**Ursache:** Fehlende graceful degradation beim Parsen der Hook-Ausgabe — ein Schema-Verstoss wird
nicht abgefangen, sondern reisst den Client mit.
**Versionen:** gemeldet ~v2.1.1xx; Issue #57483 **CLOSED NOT_PLANNED (2026-06-07)** → won't fix, bleibt aktiv.
**FIX (funktionserhaltend):** Hook-Output IMMER strikt nach Schema bauen:
`{"hookSpecificOutput":{"hookEventName":"<Event>", ...}}`. Eigene Hooks defensiv validieren BEVOR
sie JSON ausgeben (betrifft direkt `subagent-context.{ps1,sh}` u.ae.). PS: `ConvertTo-Json -Depth 5`,
keine `@{}`-Strings; Bash: nur valides JSON auf stdout, alles andere nach stderr.
**Quelle:** github.com/anthropics/claude-code/issues/57483.

### 16.2 Control-Chars im stdin-JSON brechen `jq` → Security-Guard wird still umgangen  ⭐ SECURITY
**Symptom:** Hook bekommt stdin-JSON, in dem String-Felder (`.prompt` bei UserPromptSubmit,
`.tool_input.command` bei Bash) literale Control-Chars U+0000–U+001F statt Escapes enthalten
(Trigger: Paste aus PDF, bestimmte Terminals, mehrzeilige Prompts). `jq` lehnt mit
`Invalid string: control characters from U+0000 through U+001F must be escaped` ab → der
Security-PreToolUse-Hook bricht STILL ab → die Aktion laeuft ungeprueft durch (silent bypass).
**Ursache:** Claude Code serialisiert die String-Felder nicht sauber durch `JSON.stringify`, bevor
sie ins stdin-Envelope geschrieben werden. Nicht-deterministisch.
**Versionen:** ALLE (Win/macOS/Linux); Issue #53463 **CLOSED NOT_PLANNED (2026-05-29)** → won't fix.
**FIX (funktionserhaltend):** stdin NIE mit `jq` parsen — `python3 -c "import sys,json; json.loads(sys.stdin.read())"`
(toleranter); bei Parse-Fehler **fail-closed** (deny/`exit 2`), NIE still durchwinken. Deckt sich mit
§13.2 (jq vermeiden) und verschaerft es: hier ist der jq-Bypass sicherheitskritisch.
**Quelle:** github.com/anthropics/claude-code/issues/53463.

### 16.3 PreToolUse/PostToolUse feuern NICHT fuer Tool-Calls IN Subagents
**Symptom:** Ein PreToolUse/PostToolUse-Hook (Guard, Logger) feuert fuer Tool-Calls des Hauptagenten,
aber NICHT fuer Tool-Calls, die ein Subagent (Agent-Tool) intern ausfuehrt.
**Ursache:** Tool-Hooks sind an die Haupt-Tool-Loop gebunden; Subagent-interne Tool-Calls laufen
ausserhalb.
**Versionen:** Issue #34692 **CLOSED NOT_PLANNED (2026-05-30)** → won't fix.
**FIX (funktionserhaltend):** Hook-abhaengige Policy NICHT innerhalb von Subagents erwarten. Fuer
Subagent-Kontext `SubagentStart`/`SubagentStop` nutzen; harte Policy zusaetzlich an der Stelle
verankern, wo der Subagent gespawnt wird.

### 16.4 `systemMessage` ohne `hookSpecificOutput`-Verschachtelung wird still gedroppt
**Symptom:** Ein Warn-Hook (PreToolUse/PostToolUse) setzt `systemMessage`, es erscheint aber nichts.
**Ursache:** Wie bei `additionalContext` (§2.1) muss auch hier die Struktur stimmen — flaches/falsch
platziertes Feld wird stumm verworfen.
**Versionen:** Issue #40380 **CLOSED NOT_PLANNED (2026-04-29)** → won't fix.
**FIX:** `systemMessage` ist ein TOP-LEVEL-Feld des Hook-JSON (neben `hookSpecificOutput`), nicht
darin verschachteln; `hookEventName` im `hookSpecificOutput` bleibt Pflicht fuer `additionalContext`.

### 16.5 `Cannot find module …cjs` — globale settings.json zeigt auf projekt-lokalen Pfad
**Symptom:** Bei jedem Pre/PostToolUse/Stop-Event feuert dutzendfach ein Modul-Fehler
(`Cannot find module session-state.cjs`).
**Ursache:** Die GLOBALE `settings.json` referenziert einen projekt-lokalen Hook-Pfad, der im
aktuellen Projekt nicht existiert.
**Versionen:** Issue #54743 **OPEN**.
**Eigener Vorfall (2026-08-26) — die haeufigere Variante: geteilte PROJEKT-settings.json.**
`proggs/.claude/settings.json` liegt im Repo und wird von macOS UND Windows gelesen. Darin stand
ein PreToolUse-Hook (`matcher: Bash|PowerShell`) in Exec-Form mit dem absoluten Windows-Pfad
`C:/Users/barwa/.../openlauncher-deploy-guard.ps1`. Auf macOS: rc=**64**, 976 Zeichen pwsh-Usage-
Banner auf **stdout** (kein JSON!), Fehler auf stderr — bei JEDEM Bash-Aufruf. Das erzeugt genau
das Bild "ganz viele PreToolUse-Hooks werfen Fehler". Wochenlang unbemerkt, weil NICHTS die
registrierten Hook-Pfade prueft.
**FIX (funktionserhaltend, beide Plattformen unveraendert lauffaehig):** Plattform-Weiche im
`command`-String statt Exec-Form (`args` kennt keine Shell-Expansion):
```json
"command": "if [ \"$(uname -s)\" = \"Darwin\" ] || [ \"$(uname -s)\" = \"Linux\" ]; then bash \"$CLAUDE_PROJECT_DIR/.claude/hooks/guard.sh\"; else pwsh -NoProfile -File \"C:/.../guard.ps1\"; fi"
```
Windows behaelt exakt denselben pwsh-Aufruf; macOS nimmt die gleichwertige `.sh`.
**PRAEVENTION (Poka-Yoke Stufe 2):** `startup-checks.sh` Check 7 liest beim Session-Start alle
settings-Dateien, extrahiert jeden Hook-Pfad (auch aus `args`), loest `$HOME`/`~`/
`$CLAUDE_PROJECT_DIR` auf und meldet tote Pfade — Plattform-Weichen erkennt er an `uname` und
ignoriert den fremden Zweig. Verifiziert: haette den Bug beim ersten Start gemeldet.

**FIX (funktionserhaltend):** Hook-Pfade in der globalen settings.json absolut + existenz-robust
halten; projekt-spezifische Hooks via `$CLAUDE_PROJECT_DIR` und in der PROJEKT-settings.json, nicht
global. Der Hook selbst sollte bei fehlender Datei graceful `exit 0`.

### 16.6 Windows: `.sh`-Hook-Pfade mit Leerzeichen ohne Quotes → Arg-Splitting
**Symptom:** PreToolUse/Bash-`.sh`-Hook scheitert, wenn der Pfad ein Leerzeichen enthaelt
(`C:/Users/First Last/...`); Bash interpretiert ihn als mehrere Argumente. Auch Backslash-Pfade
(`C:\Users\...`) werden von Git-Bash als Escapes gelesen; teils wird nur `bash` (ohne vollen Pfad) aufgerufen.
**Ursache:** `.sh`-Hook-Commands werden in settings.json ohne Quotes registriert (anders als `.js`-Hooks).
**Versionen:** Windows 11 + Git Bash; Issues #20551 (DUPLICATE), #22700 / #21878 (**NOT_PLANNED**).
Der explizite-bash-Aufruf-Bug (`cannot execute binary file`) ist separat in **v2.1.161 gefixt**.
**FIX (funktionserhaltend):** Hook-Command-Pfad selbst in `"..."` setzen, Forward-Slashes verwenden,
vollen Interpreter-Pfad angeben; auf Windows bevorzugt native `.ps1` via `pwsh -NoProfile -File "..."`.

### 16.7 Stop-Hook "Failed with non-blocking status code" / exit 2 als "Stop hook error"
**Symptom:** Stop-Hook meldet `Failed with non-blocking status code: No stderr output` (#59939, **OPEN**)
bzw. `exit 2` wird als "Stop hook error" angezeigt statt als Feedback (#34600, **NOT_PLANNED 2026-06-01**).
**Ursache:** Feedback-Pfad-Eigenheiten bei Stop. **Wichtig:** seit **v2.1.163** koennen Stop/SubagentStop
ueber `hookSpecificOutput.additionalContext` sauber Feedback geben (kein Error-Label) — das ist der
neue funktionserhaltende Weg.
**FIX:** Fuer Stop-Feedback `hookSpecificOutput.additionalContext` (v2.1.163+) statt `exit 2`; zum
echten Stop-Verhindern `decision:"block"`+`reason`. `exit 2` bei Stop NIE fuer reines Feedback.

### 16.8 Neue Events/Felder seit Almanach-Stand (Lueckenschluss, NICHT Bugs)
Diese kamen mit v2.1.152–v2.1.177 und gehoeren zum aktuellen Wissensstand (Details + Beispiele in
`best-practices/claude-tooling/hooks.md`):
- **~30 Events** in v2.1.177 (neu u.a. PostToolBatch, PermissionRequest/Denied, TaskCreated/Completed,
  FileChanged, CwdChanged, ConfigChange, PostCompact, Elicitation/Result, **MessageDisplay**, WorktreeCreate/Remove).
- **MessageDisplay** (v2.1.152): rein display-only (`displayContent`), KEINE Matcher, Timeout 10s, `exit 2`
  ignoriert — Claude sieht WEITER das Original (Falle: "ich habe es vor Claude versteckt" ist falsch).
- **SessionStart** `reloadSkills:true` (Skills mid-session), `sessionTitle` (nur source startup/resume).
- **Stop/SubagentStop**: `additionalContext` als Feedback (v2.1.163); Input-Felder `background_tasks`, `session_crons`.
- **Feldname `updatedInput`** (NICHT `modifiedInput`) fuer Tool-Argument-Rewrite in PreToolUse.
- **`args: string[]`** Exec-Form (v2.1.139) gegen Quoting/Shell-Injection; **`--safe-mode`** (v2.1.169) schaltet
  zum Debuggen auch Hooks ab; **if-Pfad-Matcher** `Edit(src/**)`/`Read(.env)` erst ab **v2.1.176** zuverlaessig.

---

## 15. Fix-Status — was auf v2.1.177 schon behoben ist

> Wichtig fuers Versions-Denken: Diese Eintraege waren frueher Bugs, sind aber in der
> aktuell installierten Version (**2.1.177**) bereits GEFIXT. Sie gelten nur noch fuer
> aeltere Versionen — auf 2.1.177 NICHT mehr als aktive Bugs behandeln. Status **HART per
> `gh issue view` verifiziert (2026-06-15)**: `COMPLETED` = echt gefixt, `NOT_PLANNED` =
> won't fix (Workaround bleibt), `DUPLICATE` = auf anderes Issue gebuendelt.

| Frueherer Bug | Gefixt ab / Status | Almanach-Bezug |
|---------------|--------------------|----------------|
| Hook-Output korrumpiert interaktiven Prompt (Hooks hatten Terminal-Zugriff) | **v2.1.141** | 11.3 |
| `transcript_path` ungueltig nach EnterWorktree / CWD-Wechsel | **v2.1.141** | — |
| Stop-Hook-Endlosschleife laeuft ewig (Block-Cap 8 eingefuehrt) | **v2.1.143** | 6.1, 11.2 |
| Hook-`if`-Conditions (`PowerShell(git push*)`) matchten nie | **v2.1.147** | 9.x |
| `if:"Bash(...)"` feuerte bei jedem `$()`/`$VAR` (False-Positive) | **v2.1.163** | 9.x, 16.8 |
| `if`-Pfad-Matcher `Edit(src/**)`/`Read(.env)` matchten nicht | **v2.1.176** | 16.8 |
| Deny-Rules auf `~/`-Pfade blockten `$HOME`-Bash nicht | **v2.1.163** | — |
| Windows: Hook mit explizitem bash-Aufruf "cannot execute binary file" | **v2.1.161** | 16.6 |
| `.claude.json`-Korruption bei parallelen Sessions (Windows) | **v2.1.61** | 12.7 |
| `/goal` haengt still bei `disableAllHooks`/`allowManagedHooksOnly` | **v2.1.140** | 14.2 |
| Symlinkte Settings loesen falsche `ConfigChange`-Hooks aus | **v2.1.140** | — |
| `additionalContext`-Support fuer PreToolUse (war "parsed, not supported") | **v2.1.… (#15664 COMPLETED 2026-01-16)** | 2.1 |
| PreToolUse exit code ignoriert / Operationen liefen weiter | **#21988 COMPLETED 2026-01-30** | 1.6 |
| PreToolUse exit 2 stoppt Claude statt Feedback | **#24327 COMPLETED 2026-02-22** (Modell-Verhalten bleibt → BP) | 1.x, 16.7 |
| UserPromptSubmit "error" trotz exit 0 + valid JSON | **#44943 COMPLETED 2026-04-08** | 11.1 |
| disableAllHooks umgeht managed org-Hooks (Security) | **#26637 COMPLETED 2026-02-19** (Managed-Hierarchie) | 14.2 |
| `claude plugin update` verliert `+x` auf `.sh`-Hooks | **#40280 COMPLETED 2026-04-18** (Cloud-Sync-Verlust bleibt) | 13.1 |
| Hooks stoppen nach ~2,5h still | **#16047 COMPLETED 2026-01-04** | — |
| SessionStart stdout silently dropped trotz valid JSON | **#13650 COMPLETED 2025-12-27** (Profil-Verschmutzung bleibt) | 5.1 |

### Noch NICHT gefixt auf v2.1.177 (Workaround bleibt aktiv — gh-Status in Klammern)
Per Design ODER `NOT_PLANNED` (won't fix) ODER `OPEN` — Loesungen oben weiter anwenden:
- exit 1 blockiert nicht / nur exit 2 (1.1) — per Design.
- Flaches `additionalContext`/`systemMessage` ohne `hookSpecificOutput` ignoriert (2.1, 16.4 #40380 NOT_PLANNED) — per Design.
- `type:"prompt"` nicht bei SessionStart/SessionEnd (8.1) — per Design.
- stdin dual-read noetig (Console.In vs `$input`) — Stop kein stdin Windows/pwsh (12.4, #46601 **NOT_PLANNED 2026-05-26**).
- UTF-8 BOM in settings.json (12.1, **#9906 NOT_PLANNED 2026-01-10**).
- Falsche "hook error"-Labels trotz exit 0 (11.1): **#34859 / #34713 NOT_PLANNED**, **#17088 noch OPEN** (PreToolUse), #45065 DUPLICATE.
- v2.1.123 Bash-Matcher droppt Kontext (2.4, **#55889 NOT_PLANNED 2026-06-01**) — won't fix; `Edit|Write` evtl. nicht betroffen, der v2.1.163 if-Bash-Fix ist separat.
- CLI-Crash bei non-spec hookSpecificOutput (16.1, **#57483 NOT_PLANNED 2026-06-07**).
- jq-Control-Char-Bypass im stdin (16.2, **#53463 NOT_PLANNED 2026-05-29**) — Security.
- Tool-Hooks feuern nicht im Subagent (16.3, **#34692 NOT_PLANNED 2026-05-30**).
- Plugin-command-Hooks Pre/PostToolUse verworfen (3.6): **#34573 / #14410 NOT_PLANNED**, #27398 DUPLICATE.
- UserPromptSubmit feuert bei Task-Completion (7.3, **#16952 NOT_PLANNED 2026-03-12**).
- `updatedInput` fuers Agent-Tool ignoriert (11.6, **#39814 NOT_PLANNED 2026-05-21**).
- PreToolUse + ToolSearch-Deferral Hang (11.5, **#33073 NOT_PLANNED 2026-04-08**).
- Windows-Hook-Pfade mit Leerzeichen/Backslash (16.6, **#22700 / #21878 NOT_PLANNED**).
- `session-state.cjs` Modul-Fehler (16.5, **#54743 OPEN**); Stop "non-blocking status code" (16.7, **#59939 OPEN**).

**Methodik-Hinweis:** Beim Re-Recherche-Lauf 2026-06-15 wurde JEDER genannte Issue-Status HART per
`gh issue view <nr> --repo anthropics/claude-code --json state,stateReason,closedAt` geprueft
(gh installiert + authentifiziert). `NOT_PLANNED` heisst: Anthropic plant KEINEN Fix → der Workaround
ist dauerhaft korrekt (nicht "vielleicht bald weg"). Das ist die belastbare Aussage; WebFetch-Snippets
waren teils irrefuehrend (mehrere Researcher meldeten Versionen, die gh widerlegt hat).

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

---

## 🔗 Bezug zu Best-Practices (Praevention, „wie macht man es richtig")

Dedizierte Gegenseite (seit 2026-06-15): [`best-practices/claude-tooling/claude-hooks.md`](../../best-practices/claude-tooling/claude-hooks.md)
— Almanach-gekoppelter Digest, damit der `bug-almanac-guard` fuer diesen Stufe-C-Bereich auch die
Best-Practices-Lektuere erzwingt (erst Almanach, dann Best Practices). Ausfuehrlicher Harness-Volltext
(alle 32 Events, JSON-Schema, Timeouts): [`best-practices/claude-tooling/hooks.md`](../../best-practices/claude-tooling/hooks.md).

| Bug-Abschnitt (diese Datei) | Best-Practice-Abschnitt in `best-practices-claude-hooks.md` |
|-----------------------------|--------------------------------------------------------------|
| §1 Exit-Codes, §13.7 dot-sourced libs | §1 Exit-Codes & FAIL-OPEN |
| §2 JSON-Output & Kontext-Injection, §16.1/§16.4 | §2 JSON-Output & Kontext-Injection |
| §5 stdin, §16.2 jq-Bypass, §12.4 stdin-Windows | §3 stdin & Security |
| §6 Stop-Loop, §7 Phantom-Events, §8 prompt-vs-command, §16.3 | §4 Event-Wahl & Guards |
| §9 matcher | §5 matcher |
| §12 Windows / PowerShell | §6 Windows / PowerShell |
| §13 macOS / Bash | §7 macOS / Bash |
| §3 Discovery, §4 Settings-Caching | §8 Cross-Platform & Wartung |
