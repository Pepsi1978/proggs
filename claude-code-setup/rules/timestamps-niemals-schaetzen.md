# Timestamps werden NIEMALS geschätzt — immer die echte Uhr abfragen (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-07-05. Gilt AUTOMATISCH in JEDER Session,
> auf ALLEN Plattformen, für ALLE Arten von Zeitangaben.
> Auslöser (Vorfall 2026-07-05): Die Versions-Zeitstempel des Cortex-Dashboards (0.38.x/0.39.0)
> wurden von Claude HANDGESCHÄTZT und liefen der echten Uhr ~30 Minuten voraus — Frank sah im
> Footer „02.20 Uhr", während es real 01.51 Uhr war. Franks Wortlaut: „Niemals bei einem
> Timestamp die Zeit schätzen. Generell niemals. Ich möchte immer die korrekte Uhrzeit dort
> eingetragen haben. Das bei allen Timestamps."

---

## Die eine Regel

**Bevor irgendwo ein Datum oder eine Uhrzeit eingetragen wird, wird die echte Uhr abgefragt —
IMMER, ausnahmslos. Kein Timestamp wird jemals aus dem Kopf geschätzt oder fortgeschrieben.**

| Plattform / Kontext | Pflicht-Abfrage |
|---------------------|-----------------|
| Git Bash / Linux / macOS | `date '+%d.%m.%Y, %H.%M'` (bzw. passendes Format) |
| PowerShell | `Get-Date -Format 'dd.MM.yyyy, HH:mm'` |
| Python | `datetime.now(ZoneInfo("Europe/Berlin"))` |
| Server/VPS | Container-/Systemzeit abfragen (NTP-synchron), nie lokal raten |

## Wo das gilt (Beispiele — die Liste ist nicht abschließend)

- Versions-Zeitstempel (`VERSION = "x.y.z (TT.MM.JJJJ, HH.MM Uhr)"` — Regel version-bump-visible-always)
- features.json-`eingebaut`-Felder und `stand`-Kopfzeilen
- Bugfix-Titel fürs Second Brain (`Bugfix <App> <Bereich> <YYYY-MM-DD HH:MM>`)
- Almanach-/Best-Practices-„Stand:"-Zeilen, Memory-Einträge, Session-Handoffs, Commit-Texte
- Jede Zeitangabe in Logs, Reports, Dokus, UI-Texten

## Warum

Ein geschätzter Timestamp ist eine stille Falschinformation: Frank nutzt die Stempel als
Update-Kontrolle („ist mein Deploy angekommen?") und zur zeitlichen Einordnung. Ein falscher
Stempel untergräbt genau dieses Vertrauen — und die echte Abfrage kostet einen einzigen,
billigen Befehl.

## Was NIEMALS passieren darf

- ❌ Eine Uhrzeit „ungefähr" eintragen, weil die Abfrage einen Tool-Call kostet
- ❌ Einen früheren Timestamp gedanklich fortschreiben („+20 Minuten seit vorhin")
- ❌ Bei mehreren Bumps in einer Session die Zeit nur einmal abfragen und weiterverwenden —
  vor JEDEM neuen Eintragen frisch abfragen
- ❌ Die Zeitzone raten — maßgeblich ist Europe/Berlin (Franks Standort)

## Autorität

Diese Datei (`~/.claude/rules/timestamps-niemals-schaetzen.md`) wird automatisch in jeder
Session geladen. Repo-Spiegelung: `~/proggs/claude-code-setup/rules/timestamps-niemals-schaetzen.md`.
KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwächen.
