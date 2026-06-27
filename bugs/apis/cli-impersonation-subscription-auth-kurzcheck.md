# Bekannte Bugs/Risiken: „Drittsoftware nutzt KI-Abo statt API-Key" (CLI-Impersonation) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Abo statt API-Key in Drittsoftware geplant | Erst Legalitaet pruefen — oft ToS-Verstoss | TL;DR, Checkliste |
| 2 | ⭐ Claude Pro/Max OAuth in Drittsoftware | Seit 09.01.2026 geblockt + Ban — NICHT bauen | §C1 |
| 3 | "only authorized for use with Claude Code" (403) | Harter Client-Block, kein Retry — auf API-Key | §C1, §E2 |
| 4 | Codex „Sign in with ChatGPT" (offiziell) | Erlaubt; Port 1455, `~/.codex/auth.json` | §A1 |
| 5 | Headless/SSH-Login schlaegt fehl | `codex login --device-auth` (Admin-Freigabe) | §A2 |
| 6 | Ploetzlich 401/403 nach Anbieter-Update | Client-ID/Originator/Endpoint hat sich geaendert | §E1 |
| 7 | `auth.json`/Tokens ablegen | Wie Passwort: nie committen, nie loggen | §A1, Checkliste |
| 8 | Legaler, stabiler Ausweg | API-Key ODER offizielle CLI (auch per SSH) | §E3 |
