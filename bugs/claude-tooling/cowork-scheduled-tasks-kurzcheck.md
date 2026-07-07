# Cowork — Geplante & wiederkehrende Aufgaben (Scheduled Tasks / Routines) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** ([`bugs/SYSTEM.md`](../SYSTEM.md) §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei JEDEM Fehler im Bereich (Stufe B) und vor Hochrisiko-Arbeit
> (Stufe C) — besonders vor High-Frequency-Cron (§3.1) und vor unbeaufsichtigten Tasks mit
> schreibenden Connectors (§9.4).

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | „Welches System nehmen?" (zuverlässig vs. lokale Dateien) | Zuverlässig ohne wachen PC → **Cloud-Routine**; braucht lokale Dateien/Tools → **Local Task**; nur in offener Session → `/loop` | §0 |
| 2 | Geplante Aufgabe feuert nicht / zur falschen Zeit | Lokal nur bei **wachem PC + offener App**; Cowork-Tab fokussiert halten; sonst Cloud-Routine | §1.1 / §1.2 |
| 3 | App friert bei JEDEM Start ein nach häufigem Cron | `scheduled-tasks.json` umbenennen → bootet wieder; danach moderate Frequenz | §3.1 ⭐KRITISCH |
| 4 | Verpasster Lauf kommt nur 1× und zur falschen Uhrzeit | Per Design (1 Catch-up, 7-Tage-Fenster) → **Zeit-Guardrails in den Prompt** | §2.1 |
| 5 | Verpasste Tasks feuern beim Start alle gleichzeitig | Client-gebunden, kein Server-Trigger → „Keep computer awake" / Cloud-Routine | §1.3 |
| 6 | Task „Running" mit 0 Turns, alle Folgeläufe „Skipped" | Zombie-DoS → App komplett neu starten, Task pausieren, Cloud-Routine | §4.1 |
| 7 | `/schedule` scheitert: `create_scheduled_task` fehlt | Tool nicht injiziert (Win-Regression) → Task per Routines-UI anlegen | §6.1 |
| 8 | „Failed to create scheduled task" | Documents per GPO auf UNC umgeleitet / anderes Laufwerk → Working-Folder lokal | §6.3 |
| 9 | „Cannot create … from within a scheduled task session" | Per Design → Task darf nur sich SELBST per `update_scheduled_task` umplanen | §6.2 |
| 10 | Permission-Prompt bei jedem Lauf / Run stallt im Ask-Mode | Nach Anlegen **„Run now"** + pro Tool **„always allow"**; bypassPermissions wird NICHT geerbt | §7.1 |
| 11 | Geplanter Lauf: MCP-Connectors leer / alle MCP-Calls scheitern | Session nicht „aufgewärmt" → ersten MCP-Call von einem **Subagent** absetzen | §8.1 |
| 12 | Cloud-Routine sieht lokale Dateien / `.mcp.json` nicht | Per Design (frischer Clone, nur claude.ai-Connectors) → Local Task ODER Connector+`.mcp.json` ins Repo | §8.2 |
| 13 | Cloud-Routine: HTTP 403 `host_not_allowed` | Netzwerk default „Trusted" → Environment auf Custom/Full + Domains allowlisten | §8.3 |
| 14 | Cron mit `MON`/`JAN`/`L`/`W`/`@daily`/Sekunden | Nicht unterstützt — nur 5-Feld-Cron numerisch; einmalig = `fireAt` ISO-8601 **mit Offset** | §5.1 |
| 15 | Cron „alle 10 Min" in Cloud abgelehnt | Cloud-Mindestintervall **1 h**, Local 1 Min; max 50 Tasks/Session | §5.2 |
| 16 | Grüner Status, aber Aufgabe trotzdem fehlgeschlagen | Grün = „Session lief", nicht „Prompt erfüllt" → **Transkript lesen** | §9.3 |
| 17 | Task „erinnert" sich nicht / trifft uncommitteten Stand | Per Design frische Session → selbst-enthaltender Prompt + Worktree-Toggle | §9.1 / §9.2 |
| 18 | Unbeaufsichtigter Task mit Mail/Kalender/Dateizugriff | Prompt-Injection real (CVSS-10-Kalender-Beispiel) → keine sensiblen Connectors, „do NOT delete/send"-Guardrails | §9.4 ⭐KRITISCH |
| 19 | Geplante Tasks verbrauchen Quota überraschend schnell | Jeder Lauf = volle Session → leichte Tasks bündeln; Cloud hat tägliches Run-Cap | §10.1 |
| 20 | Mobile/Handy: Task läuft nicht | „Dispatch" ist nur Fernsteuerung des **wachen Desktops**; unabhängig nur Cloud-Routine | §1.4 |
