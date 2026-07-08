# Session-Presence: Hinweis auf parallele Sessions (KEINE Sperre) (KRITISCH)

> Nicht die Arbeit einer anderen Session ueberschreiben — nur HINWEIS, nie Sperre. Frank hat oft 4-5 Sessions am Repo `~/proggs`.

## Warum keine Sperre (Vorfall 2026-06-25)

Die erste Fassung sperrte per `deny` bei Edit/Write derselben Datei → zwei Sessions an verwandten
Dateien sperrten sich GEGENSEITIG aus (Deadlock). Deshalb nur der unverbindliche Hinweis.

## Mechanik

`session-presence-warn` (UserPromptSubmit) meldet bei jedem Prompt, wenn eine andere lebende Session im
selben `cwd` arbeitet + deren zuletzt beruehrte Dateien — **kein Block**. Alter `session-presence-guard`
deaktiviert (No-Op). Datenbasis: `active-tasks.jsonl`. Liveness: anderes `session_id`, gleiches `cwd`,
`timestamp_last_update` < 8 Min, Status nicht beendet.

## Was ich tue

Kein Warten-Zwang — normal weiter. Nicht exakt DIESELBE Datei gleichzeitig editiert = ok. Wuerde ich
genau eine genannte Datei aendern: erst woanders weiter ODER die Datei VOR dem Edit neu lesen. Zusatz:
Edit-Tool meldet "file modified since read" → neu lesen. Notaus: `session-presence-disable.flag` im TEMP.

## Grenzen & Verbote

Nur Hinweis — ersetzt NICHT Git (eigene Dateien, fetch+rebase vor Push Pflicht). NIEMALS: eine
`deny`-Sperre wieder einbauen (verursachte den Deadlock) · den Hinweis so verschaerfen dass er den
Arbeitsfluss anhaelt · die git-seitige Sicherung dadurch ersetzen.
