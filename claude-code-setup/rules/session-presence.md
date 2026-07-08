# Session-Presence: paralleler-Session-Hinweis (KEINE Sperre) (KRITISCH)

> Ziel (Franks Wortlaut): Eine Session darf die Arbeit einer anderen nicht aus Versehen ueberschreiben —
> aber NUR per HINWEIS, NIEMALS per Sperre. Frank arbeitet oft mit 4-5 gleichzeitig offenen Sessions
> am selben Repo `~/proggs` (geteilter Working-Tree).

## Warum keine Sperre (Vorfall 2026-06-25)

Die erste Fassung hatte einen harten Datei-Waechter (`deny` bei Edit/Write derselben Datei). Zwei
Sessions an verwandten Dateien sperrten sich GEGENSEITIG aus → Deadlock, beide ohne Ergebnis. Deshalb
gibt es nur noch den unverbindlichen Hinweis — kein `deny`, kein Block.

## Grundprinzip

| Hook | Event | Wirkung |
|------|-------|---------|
| `session-presence-warn` | UserPromptSubmit | Awareness: meldet bei jedem Prompt, wenn eine andere lebende Session im selben Projekt (cwd) arbeitet + deren zuletzt beruehrte Dateien. **Kein Block.** |

Der fruehere `session-presence-guard` (Datei-Sperre) ist deaktiviert (No-Op). Datenbasis:
`~/proggs/.claude/agent-memory/shared/active-tasks.jsonl` (von `task-ledger-*`-Hooks gepflegt).
Liveness: anderes `session_id`, gleiches `cwd`, `timestamp_last_update` < 8 Min, Status nicht beendet.

## Was ich bei dem Hinweis tue

Kein Warten-Zwang — normal weiterarbeiten. Solange ich nicht exakt DIESELBE Datei gleichzeitig editiere,
ist alles ok. Wuerde ich genau eine der genannten Dateien aendern: erst woanders weitermachen ODER die
Datei direkt VOR dem Edit neu lesen (nie blind ganze Datei ueberschreiben). Zusatzschutz: das Edit-Tool
meldet "file modified since read" → dann neu lesen. Notaus: `session-presence-disable.flag` im TEMP.

## Grenzen

Nur ein Hinweis, kein Schutz — ersetzt NICHT Git (nur eigene Dateien, fetch+rebase vor Push bleibt
Pflicht). Zugbasiert (Session erfaehrt es erst beim eigenen naechsten Prompt).

## Was NIEMALS passieren darf

- Eine Sperre (`deny`) wieder einbauen, die Edits zwischen Sessions blockiert (verursachte den Deadlock)
- Den Hinweis so verschaerfen dass er den Arbeitsfluss anhaelt · die git-seitige Sicherung dadurch ersetzen
