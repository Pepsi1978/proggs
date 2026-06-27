# Claude Code Hooks Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
