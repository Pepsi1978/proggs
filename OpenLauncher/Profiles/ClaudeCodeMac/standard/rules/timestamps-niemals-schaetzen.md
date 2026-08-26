# Timestamps werden NIEMALS geschaetzt — immer die echte Uhr abfragen (KRITISCH)

> Ausloeser: Cortex-Dashboard-Versions-Zeitstempel wurden handgeschaetzt und liefen ~30 Min der echten
> Uhr voraus. Franks Wortlaut: "Niemals bei einem Timestamp die Zeit schaetzen. Generell niemals."

## Die eine Regel

Bevor irgendwo ein Datum/eine Uhrzeit eingetragen wird, wird die echte Uhr abgefragt — IMMER, ausnahmslos.
Kein Timestamp wird aus dem Kopf geschaetzt oder fortgeschrieben.

| Kontext | Pflicht-Abfrage |
|---------|-----------------|
| Git Bash / Linux / macOS | `date '+%d.%m.%Y, %H.%M'` |
| PowerShell | `Get-Date -Format 'dd.MM.yyyy, HH:mm'` |
| Python | `datetime.now(ZoneInfo("Europe/Berlin"))` |
| Server/VPS | Container-/Systemzeit (NTP), nie lokal raten |

## Wo das gilt (nicht abschliessend)

Versions-Zeitstempel · features.json-`eingebaut`/`stand` · Bugfix-Titel fuers Second Brain · Almanach-/
Best-Practices-"Stand:"-Zeilen · Memory-Eintraege · Session-Handoffs · Commit-Texte · Logs/Reports/UI.

## Was NIEMALS passieren darf

- Eine Uhrzeit "ungefaehr" eintragen weil die Abfrage einen Tool-Call kostet · einen frueheren Timestamp
  gedanklich fortschreiben ("+20 Min") · bei mehreren Bumps die Zeit nur einmal abfragen (vor JEDEM neu) ·
  die Zeitzone raten (massgeblich Europe/Berlin)
