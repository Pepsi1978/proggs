# Codex Global Rules

Diese Regeln gelten fuer Codex plattformuebergreifend auf macOS und Windows.

## Zweck und Geltungsbereich

- Diese Datei ist fuer Codex gedacht, nicht fuer Claude Code.
- `codex-setup/` ist der Synchronisierungsordner fuer Codex auf macOS und Codex auf Windows.
- Die Regeln in `codex-setup/` sollen in beide Richtungen gelten: macOS zu Windows und Windows zu macOS.
- `claude-code-setup/`, `CLAUDE.md` und andere Claude-Code-Dateien sind fuer Codex keine Arbeitsanweisung.
- Wenn der Benutzer `Cloud Code` sagt, ist damit `Claude Code` gemeint.

## Erlaubtes Arbeitsverzeichnis

- Codex arbeitet nur im Codex-Workspace.
- macOS: `/Users/frank/Codex`
- Windows: `C:\Users\barwa\Codex`
- Andere lokale Kopien desselben Repos sind fuer Codex tabu, ausser der Benutzer aendert das ausdruecklich.

## Verbotenes Arbeitsverzeichnis

- Codex darf niemals in `proggs` arbeiten.
- macOS: `/Users/frank/proggs`
- Windows: `C:\Users\barwa\proggs`
- `proggs` ist ausschliesslich fuer Claude Code reserviert.
- Codex darf Dateien dort nicht lesen, schreiben, als `workdir` verwenden oder fuer Git-Operationen benutzen.

## Trennung zu Claude Code

- Codex ignoriert `claude-code-setup/` grundsaetzlich.
- Codex nutzt daraus keine Regeln, Hooks, Settings, MCP-Konfigurationen oder Workflow-Vorgaben.
- Eine Ausnahme gilt nur dann, wenn der Benutzer ausdruecklich eine Aufgabe an genau diesem Ordner verlangt. Dann ist er Projektinhalt, nicht Regelquelle.

## Git-Regeln fuer Codex

- Lokale Commits duerfen in `Codex` erstellt werden, wenn sie zur Aufgabe gehoeren.
- Commits sollen nur aufgabenrelevante Aenderungen enthalten.
- Fremde oder bereits vorhandene, nicht zur Aufgabe gehoerende Aenderungen duerfen nicht ungefragt mit committed werden.
- Ausnahme: Fuer Aenderungen innerhalb von `codex-setup/` darf Codex eigenstaendig committen und pushen.
- Diese Ausnahme gilt nur fuer `codex-setup/`, weil dieser Ordner die plattformuebergreifende Synchronisation von Codex-Regeln zwischen macOS und Windows traegt.
- Fuer alle anderen Ordner im Repository gilt: `git push` nur nach ausdruecklicher Benutzeranweisung.
- Vor einem moeglichen Push soll Codex Aenderungen bevorzugt lokal validieren.

## Startverhalten fuer Codex

- Zu Beginn jeder Codex-Session soll zuerst diese Regelbasis gelesen werden.
- Wenn andere lokale Dateien im Repository widerspruechliche Aussagen enthalten, sind sie fuer Codex nicht massgeblich, sofern sie Claude Code betreffen.
- Das Ziel ist, dass dieselben Codex-Regeln auf macOS und in PowerShell 7 unter Windows gleich gelten.
