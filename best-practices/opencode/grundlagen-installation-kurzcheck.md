# Grundlagen & Installation Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Was OpenCode ist | Open-Source-KI-Coding-Agent (SST/Anomaly, `opencode.ai` — NICHT die Klon-Domain `open-code.ai`); TUI + Desktop (Beta) + IDE; provider-agnostisch, Client/Server-Architektur | §1 |
| 2 | Installation universell | `curl -fsSL https://opencode.ai/install \| bash` ODER npm — Paket heißt `opencode-ai` (NICHT `opencode`); alte Versionen < 0.1.x vorher entfernen | §2 |
| 3 | Windows-Installation | WSL empfohlen; nativ am besten `scoop install opencode` (PATH automatisch) oder `choco`; npm-Wrapper fehleranfällig; `curl\|bash` läuft nur in Git Bash/WSL (nicht PowerShell/CMD) | §2, §7 |
| 4 | Provider verbinden | `/connect` (TUI) oder `opencode auth login`; Credentials in `~/.local/share/opencode/auth.json` | §3 |
| 5 | Headless / Skripte | `opencode run "..."`; an laufenden Server hängen via `opencode serve` + `--attach http://localhost:4096` (umgeht MCP-Kaltstart); `--dangerously-skip-permissions` ist gefährlich | §3 |
| 6 | Leader-Key | Default `ctrl+x`: erst Leader drücken+loslassen, DANN Buchstabe (z.B. `ctrl+x` dann `n` = neue Session); `leader_timeout` 2000 ms | §4 |
| 7 | Wichtigste Slash-Befehle | `/init`, `/new` (`/clear`), `/sessions`, `/compact`, `/connect`, `/undo` / `/redo`, `/export` | §5 |
| 8 | Undo/Redo braucht Git | `/undo` + `/redo` verwalten Datei- UND Nachrichtenänderungen über Git → Projekt MUSS ein Git-Repo sein | §5 |
| 9 | Empfohlener Workflow | mit `Tab` in Plan-Modus Feature beschreiben + iterieren, dann `Tab` zu Build „Go ahead."; Subagents `@explore`/`@scout` für Spezialarbeit | §6 |
| 10 | Pfade & Logs ermitteln | `opencode debug paths` (undokumentiert, sehr nützlich) zeigt exakte Pfade je OS; Logs in `~/.local/share/opencode/log/`, mehr per `--log-level DEBUG` / `--print-logs` | §3, §8 |
| 11 | OpenCode ohne Tastatur kopieren/einfügen | Windows-Terminal-`settings.json`: `"copyOnSelect": true`, `"experimental.rightClickContextMenu": false`, optional `"copyFormatting": "none"`; Linksauswahl kopiert, Rechtsklick fügt ein | §7 |
