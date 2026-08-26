# Claude Code Hooks — Best Practices (Präventions-Seite)

> **Zweite Seite der Medaille zum Bug-Almanach** [`bugs/claude-tooling/claude-hooks.md`](../../bugs/claude-tooling/claude-hooks.md)
> (**Stufe-C-Hochrisiko** — vor Hook-Arbeit ist der Almanach-VOLLTEXT Pflicht): dort steht *was schiefgeht*,
> hier *wie man einen Hook von vornherein richtig baut*. Diese Datei existiert vor allem, damit der
> `bug-almanac-guard` für den Hochrisiko-Bereich `claude-hooks` auch die Best-Practices-Lektüre erzwingt
> (erst Almanach, dann Best Practices).
>
> **Ausführlicher Harness-Volltext (verlustfrei ausgelagert):** [`best-practices/claude-tooling/hooks.md`](hooks.md)
> — dort stehen alle 32 Events, das vollständige JSON-Schema, Timeout-/Async-Details und Beispiele.
> Diese Datei ist der kompakte, Almanach-gekoppelte Digest; der 01-hooks-Volltext bleibt die Tiefe.
>
> **Stand:** 2026-06-15, Claude Code **2.1.177** (abgeleitet aus dem claude-hooks-Almanach + 01-hooks-Harness-BP).
> Pflicht-Werkzeug beim Hook-Bau: der **`hook-forge`-Skill** (Template + Checkliste + eingebautes `exit 0`).

---

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

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei / 01-hooks) | Bug-Gegenpart in `bugs/claude-tooling/claude-hooks.md` |
|---|---|
| §1 Exit-Codes & FAIL-OPEN | §1 (1.1–1.7), §13.7 (dot-sourced libs) |
| §2 JSON-Output & Kontext-Injection | §2 (2.1–2.6), §16.1, §16.4 |
| §3 stdin & Security | §5, §16.2, §12.4 |
| §4 Event-Wahl & Guards | §6 (Stop-Loop), §7 (Phantom-Events), §8 (prompt vs command), §16.3 |
| §5 matcher | §9 |
| §6 Windows / PowerShell | §12 |
| §7 macOS / Bash | §13 |
| §8 Cross-Platform & Wartung | §3 (Config-Cache/Discovery), §4, §13.7 |

---

## §1 Exit-Codes & FAIL-OPEN

- **Blocken nur mit `exit 2`** + Begründung auf stderr (`exit 1` blockiert NICHT — häufigster Hook-Bug). Bei
  `WorktreeCreate` blockt jeder non-zero Exit. Standalone-Hooks enden IMMER mit `exit 0`.
- **Write/Edit blocken** nicht über `exit 2` (greift dort nicht zuverlässig), sondern über JSON
  `{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny"}}` + `exit 0` (wirkt für alle Tools).
- **FAIL-OPEN als Grundhaltung:** jeder interne Fehler → `exit 0` ohne `deny` (try/catch in PS, `trap '…; exit 0' ERR`
  in bash). Ein Guard darf die Session nie versehentlich blockieren.
- **Dot-sourced Bibliotheken** (`hook-log`, `whiteboard-insert`) enden OHNE top-level `exit` — in bash killt ein
  `exit` den sourcenden Aufrufer (auf Windows/PS unsichtbar → der Drift-Klassiker).

## §2 JSON-Output & Kontext-Injection

- **Kontext immer verschachtelt:** `{"hookSpecificOutput":{"hookEventName":"<Event>","additionalContext":"…"}}` —
  flaches `{"additionalContext":…}` wird still verworfen. PowerShell: `ConvertTo-Json -Depth 5` (sonst flach).
- **`systemMessage` ist Top-Level** (neben `hookSpecificOutput`), nicht darin.
- **Strikt spec-konform bauen** — ein non-spec `hookSpecificOutput` crasht die ganze Session (TypeError, keine
  Recovery, #57483 won't-fix). Nur valides JSON auf stdout, alles andere nach stderr.
- Plain stdout landet nur bei `SessionStart`/`Setup`/`SubagentStart` im Kontext — sonst das `additionalContext`-Schema nutzen.

## §3 stdin & Security

- **stdin (Event-JSON) NIE mit `jq` parsen** — literale Control-Chars (U+0000–U+001F) brechen `jq`, ein
  Security-Guard wird dann STILL umgangen (#53463 won't-fix). Stattdessen `python3 -c "import sys,json; json.loads(sys.stdin.read())"`
  und bei Parse-Fehler **fail-closed** (deny/`exit 2`), nie still durchwinken.
- **Windows dual-read:** erst `[Console]::In.ReadToEnd()`, dann `$input | Out-String` (Stop-Hook bekommt auf
  Windows/pwsh sonst leeres stdin).
- Tool-Input kommt als stdin-JSON, NICHT als Env-Variable (`tool_input`/`cwd`/`session_id` aus dem JSON parsen).

## §4 Event-Wahl & Guards

- **Stop-Hook:** ZUERST `stop_hook_active` prüfen → wenn true sofort `exit 0` (sonst Endlosschleife). Stop-Hooks idempotent.
- **Input-Guard für Side-Effect-Hooks** (SubagentStop/Stop/PostToolUse): am Anfang Pflichtfelder prüfen (`agent_id`
  bei SubagentStop, `tool_name` bei PostToolUse), sonst `exit 0` ohne Side-Effect — sie feuern auch ohne echten Trigger.
  **AUSNAHME:** rein passive Context-Injection-Hooks brauchen KEINEN Guard (er würde legitime Injection verhindern).
- **`type:"prompt"` nicht bei SessionStart/SessionEnd** (kein ToolUseContext) → `command`. `prompt`+`command`-Key-Mix
  bricht ALLE Hooks still.
- **Tool-Hooks feuern nicht im Subagent** (#34692) → für Subagent-Kontext `SubagentStart`/`SubagentStop`.

## §5 matcher

- Bewusst exakt setzen: `Edit|Write` (case-sensitiv, `MultiEdit` nicht `multiEdit`). MCP-Tools brauchen `.*`-Suffix
  (`mcp__server__.*`). Fehlender/`*`-Matcher = match-all. Bei Nicht-Tool-Events (UserPromptSubmit/Stop) wird der
  Matcher ignoriert → im Hook selbst filtern.

## §6 Windows / PowerShell

- IMMER `pwsh` (PowerShell 7+), nie `powershell.exe` (cp1252, lädt Module nicht). Start als
  `pwsh -NoProfile -ExecutionPolicy Bypass -File "…"` mit **Forward-Slash**-Pfaden (nie inline `-Command`).
- **JSON BOM-frei schreiben:** `[System.IO.File]::WriteAllText($p,$json,(New-Object System.Text.UTF8Encoding $false))`
  — `[System.Text.Encoding]::UTF8` schreibt einen BOM (Falle!). Ein BOM in settings.json killt die ganze Config still.
- Umlaute: `[Console]::OutputEncoding=[Text.Encoding]::UTF8`.

## §7 macOS / Bash

- `.sh` mit `+x` und **LF** (CRLF → „bad interpreter: …^M"). `set -e` vorsichtig: harmlose non-zero Exits mit `|| true`
  abfangen, im `trap` `… ; exit 0` (graceful). Kein zwingendes `jq` (fehlt oft) — Python-`json` nutzen.
- Dot-sourced Libs ohne top-level `exit` (siehe §1).
- **Kein Unix-Tool ungeprüft voraussetzen.** `timeout` ist GNU-coreutils und auf macOS **nicht vorhanden**.
  In Verbindung mit `|| true` wird daraus ein stiller Totalausfall: `timeout 2 python3 <<'PYEOF' || true`
  scheitert mit rc=127, `|| true` schluckt es, der ganze Heredoc-Block läuft nie — bei rc=0 und leerem stderr.
  Richtig ist erkennen statt voraussetzen (ohne Zeitlimit laufen schlägt gar nicht laufen):
  ```bash
  if command -v timeout > /dev/null 2>&1; then _TO="timeout 2"
  elif command -v gtimeout > /dev/null 2>&1; then _TO="gtimeout 2"
  else _TO=""; fi
  $_TO python3 <<'PYEOF' || true
  ```
  Gleiches Prinzip für `gdate`, `gsed`, `realpath`, `sha256sum`. Almanach §13.9.
- **Jede Hook-Funktion endet mit einem bewussten Rückgabewert.** In bash gibt eine Funktion den Exit-Code
  ihres letzten Befehls zurück — endet sie auf einem Test (`[ $? -eq 2 ] && exit 2`), liefert sie im
  Normalfall `1`. Der ERR-Trap aus `hook-log.sh` meldet das (zu Recht) als Fehler und flutet das Log.
  Faustregel: endet eine Funktion auf `[ … ]`, `grep`, `test` oder `[[ … ]]`, gehört ein `return 0` darunter.
  Den Trap dafür **nicht** abschwächen — er ist die Observability-Schicht. Almanach §13.10.
- **Ausgaben in async `SessionEnd`-Hooks absichern:** dort ist stdout beim Lauf oft schon geschlossen,
  `echo` liefert EPIPE und damit non-zero. `echo "…" || true` erhält die Ausgabe, solange stdout lebt,
  und macht den erwarteten EPIPE zu keinem Fehler. Almanach §13.11.

## §8 Cross-Platform & Wartung

- Jeder eigene Hook existiert als **`.ps1` UND `.sh`** mit identischer Logik (Drift ist ein Bug — gemeinsame Logik
  ggf. in eine `.py` auslagern, die beide Wrapper aufrufen, wie `bug-almanac-hint`).
- **Config ist gecacht:** Hook-Änderungen greifen erst nach Session-Neustart — beim Debuggen ZUERST ausschließen.
- settings.json nach jeder Änderung mit `python3 -m json.tool` validieren (ein Syntaxfehler killt ALLE Hooks still).
- **Geteilte Projekt-`settings.json` niemals mit einem plattformspezifischen absoluten Pfad bestücken.**
  Die Datei liegt im Repo und wird von macOS *und* Windows gelesen; ein hart kodierter `C:/Users/…`-Pfad
  lässt den Hook auf der anderen Plattform bei **jedem** Tool-Aufruf scheitern (rc≠0 + Nicht-JSON auf stdout).
  Lösung ist eine `uname`-Weiche im `command`-String — die `args`-Exec-Form kennt keine Shell-Expansion und
  scheidet daher aus:
  ```json
  "command": "if [ \"$(uname -s)\" = \"Darwin\" ]; then bash \"$CLAUDE_PROJECT_DIR/.claude/hooks/g.sh\"; else pwsh -NoProfile -File \"C:/…/g.ps1\"; fi"
  ```
  So bleibt jede Plattform bei genau ihrem bisherigen Aufruf. Almanach §16.5.
- **Registrierte Hook-Pfade beim Session-Start verifizieren** (Poka-Yoke Stufe 2): `startup-checks.sh` Check 7
  liest alle settings-Dateien, löst `$HOME`/`~`/`$CLAUDE_PROJECT_DIR` auf und meldet tote Pfade. Ohne diesen
  Check bleibt ein toter Hook-Pfad beliebig lange unentdeckt — es gibt sonst keine Instanz, die ihn prüft.
- Neue/geänderte Hooks über den **`hook-forge`-Skill** bauen (Template hat `exit 0`, try/catch, Input-Guard eingebaut
  → Poka-Yoke Stufe 3).

---

## Pflicht-Checkliste vor Hook-Arbeit

- [ ] Almanach-VOLLTEXT [`claude-hooks.md`](../../bugs/claude-tooling/claude-hooks.md) gelesen (Stufe C) + dieser Kurzcheck?
- [ ] `hook-forge`-Skill genutzt (Template mit `exit 0`/try-catch/Guard)?
- [ ] Blocken via `exit 2` bzw. `permissionDecision:"deny"` (Write/Edit) — nie `exit 1`? FAIL-OPEN sonst?
- [ ] Kontext via verschachteltes `hookSpecificOutput` (PS `-Depth 5`), strikt spec-konform?
- [ ] stdin per `python3 json.loads` (kein `jq`), Windows dual-read, fail-closed bei Parse-Fehler?
- [ ] Side-Effect-Hook mit Input-Guard? Passiver Injection-Hook OHNE Guard?
- [ ] `.ps1` UND `.sh` identisch, `pwsh`/LF/`+x`, BOM-frei, Session neu gestartet?
