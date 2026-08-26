# Claude Code Hooks Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Vorab-Pflicht. Da `claude-hooks` **Stufe C** ist,
> ist der ALMANACH-Volltext zusätzlich Pflicht; dieser Kurzcheck ist nur die Schnell-Orientierung der Positiv-Seite.

| # | Situation | Best Practice (Kurzform) | Almanach |
|---|-----------|--------------------------|----------|
| 1 | Hook soll blockieren | `exit 2` (NIE `exit 1` — blockiert nicht); WorktreeCreate: jeder non-zero blockt | §1 |
| 2 | Write/Edit blockieren | NICHT auf `exit 2` verlassen (greift dort nicht) → `permissionDecision:"deny"` per JSON + `exit 0` | §1.6 |
| 3 | Standalone-Hook-Ende | IMMER `exit 0` am Ende; FAIL-OPEN (interner Fehler → `exit 0`, blockiert Session nie) | §1 |
| 4 | dot-sourced Bibliothek | NIE top-level `exit` (killt in bash den sourcenden Aufrufer) | §13.7 |
| 5 | Kontext an Claude geben | `{"hookSpecificOutput":{"hookEventName":"…","additionalContext":"…"}}` (verschachtelt!); PS `-Depth 5` | §2.1 |
| 6 | JSON-Ausgabe bauen | STRIKT spec-konform — non-spec `hookSpecificOutput` crasht die ganze Session (TypeError) | §16.1 |
| 7 | `systemMessage` setzen | Top-Level-Feld (neben `hookSpecificOutput`), nicht darin verschachteln | §16.4 |
| 8 | stdin (Event-JSON) parsen | NIE `jq` (Control-Char-Bypass = stiller Security-Umgang) → `python3 json.loads`, bei Fehler fail-closed | §16.2, §13.2 |
| 9 | stdin lesen (Windows) | Dual-read: erst `[Console]::In.ReadToEnd()`, dann `$input` (Stop-Hook bekommt sonst leeres stdin) | §12.4 |
| 10 | Side-Effect-Hook (SubagentStop/Stop) | Input-Guard: Pflichtfelder prüfen, sonst sofort `exit 0` (feuern auch ohne echten Trigger) | §7.2 |
| 11 | Passiver Context-Injection-Hook | KEIN Guard (würde legitime Injection verhindern) | §7.2 |
| 12 | Stop-Hook | ZUERST `stop_hook_active` prüfen → sonst `exit 0` (Endlosschleife) | §6.1 |
| 13 | SessionStart/SessionEnd | KEIN `type:"prompt"` (kein ToolUseContext) — `command` nutzen | §8.1 |
| 14 | matcher | exakt `Edit\|Write`; MCP mit `.*`-Suffix (`mcp__server__.*`); fehlender Matcher = alles | §9 |
| 15 | Windows | `pwsh` (nie `powershell.exe`); `pwsh -NoProfile -File "…"` (nicht `-Command`); Forward-Slash-Pfade | §12 |
| 16 | settings.json/JSON schreiben | BOM-frei (`UTF8Encoding $false`), nach Änderung validieren; Session neu starten (Config gecacht) | §12.1, §4.1 |
| 17 | Bash-`.sh`-Hook | `+x`, LF (kein CRLF), `set -e` mit `\|\| true` / `exit 0` im `trap` | §13.3, §13.4 |
| 18 | Cross-Platform | `.ps1` UND `.sh` mit identischer Logik (Drift = Bug) | §13.7 |
| 19 | Tool-Hook im Subagent erwartet | Feuert NICHT für Subagent-interne Tool-Calls → `SubagentStart`/`SubagentStop` | §16.3 |
| 20 | Unix-Tool im `.sh`-Hook nutzen | `timeout`/`gdate`/`gsed` gibt es auf macOS NICHT — `command -v` prüfen; mit `\|\| true` sonst stiller Totalausfall | §13.9 |
| 21 | Funktion in einem Hook schreiben | Endet sie auf einem Test (`[ … ]`, `grep`), gibt sie 1 zurück → `return 0` ans Ende | §13.10 |
| 22 | `echo` in async `SessionEnd` | stdout ist dort oft schon zu → `echo … \|\| true` (EPIPE ist der Normalfall) | §13.11 |
| 23 | Geteilte Projekt-`settings.json` | NIE absoluten Plattform-Pfad hart kodieren → `uname`-Weiche im `command` (nicht `args`) | §16.5 |
