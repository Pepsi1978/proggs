# Bekannte Bugs & Fallen: Cowork — Geplante & wiederkehrende Aufgaben (Scheduled Tasks / Routines)

> **PFLICHT-LESEN, BEVOR du in Cowork/Claude-Desktop eine geplante oder wiederkehrende Aufgabe
> anlegst** (Routines → New routine → Local/Remote, „/schedule", oder per Klartext „lege täglich um
> 9 Uhr … an"). Dieses Sub-Almanach vertieft Abschnitt 7 des allgemeinen Cowork-Almanachs
> ([`cowork.md`](cowork.md) §7) speziell für das Thema **Scheduling**.
>
> Kuratiert aus offizieller Anthropic-Doku/Support (`code.claude.com/docs`, `support.claude.com`),
> dem offiziellen Issue-Tracker `github.com/anthropics/claude-code` (Labels `area:cowork`,
> `area:routines`) und externer Praxis (Substack/Blogs, Security-Firmen). Lösungen sind
> funktionserhaltend (nie „Feature weglassen").
>
> **Stand:** recherchiert am **2026-06-15** mit 5 parallelen Researchern. Scheduling hat **keinen
> eigenen Versions-Changelog**; Anker ist der Stand der offiziellen Doku-/Support-Seiten
> (Quellenstand bis Juni 2026) plus die in den Issues genannten Versionen (Claude Code 2.1.71–2.1.173,
> Desktop-App 1.1.x–1.11.x). Wichtig: **Stand 2026-06-15 ist KEIN untersuchter Scheduled-Task-Bug per
> offiziellem Changelog (geprüft bis CHANGELOG v2.1.173, 2026-06-11) als behoben bestätigt.**
>
> **Quellen-Rangordnung:** offiziell (support.claude.com, code.claude.com, claude.com/docs,
> anthropic.com) = Grundwahrheit. GitHub-Issues im offiziellen Repo = nahe-offiziell (Anthropic-Tracker,
> aber Nutzer-Einreichungen). Community/Presse = `extern` (sekundär).
>
> **Gegenseite (Best Practices):** [`best-practices/claude-tooling/cowork-scheduled-tasks.md`](../../best-practices/claude-tooling/cowork-scheduled-tasks.md) (dedizierte Gegenseite, Bezugstabelle ganz unten; Kurzfassung auch in `best-practices-cowork.md` §5).

---

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

---

## 0. Die drei (faktisch vier) Scheduling-Systeme — erst wählen, dann anlegen

> Fast alle Scheduling-Probleme entstehen, weil das falsche System für den Zweck gewählt wurde. Die
> offizielle Doku unterscheidet **drei** Optionen; praktisch teilen sich **Cowork Scheduled Tasks**
> und die **Local Routines** im Code-Tab dieselbe lokale Engine (nur andere Oberfläche/Zielgruppe).

| Merkmal | **Cloud / Remote Routine** | **Local Task** (Cowork-Tab / Code-Tab „Local") | **`/loop`** (CLI, session-gebunden) |
|---|---|---|---|
| Läuft auf | Anthropic-Cloud | Eigener Rechner | Eigener Rechner |
| **Läuft bei geschlossener App?** | **Ja** (PC darf aus sein) | **Nein** — App offen + PC wach | **Nein** — Session offen |
| Dateizugriff lokal? | **Nein** (frischer Git-Clone, nur committeter Code) | **Ja** | **Ja** |
| MCP / Connectors | nur claude.ai-Connectors; `.mcp.json` aus Repo wird NICHT geladen | lokale `.mcp.json` + Connectors (Warm-up-Bug §8.1) | erbt von der Session |
| Permission-Prompts | keine (vollautonom) | pro Task; Ask-Mode kann stallen (§7.1) | erbt von der Session |
| **Mindestintervall** | **1 Stunde** (kürzer = abgelehnt) | **1 Minute** | 1 Minute |
| Catch-up bei verpassten Läufen | n/a (läuft immer) | **genau 1** Lauf, 7-Tage-Fenster, evtl. falsche Uhrzeit (§2.1) | **kein** Catch-up |
| 7-Tage-Ablauf | nein (durabel) | nein (durabel) | **ja** — recurring endet nach 7 Tagen automatisch |
| Trigger | Schedule + API (`/fire`) + GitHub-Events | nur Zeitplan | Zeitplan / dynamisch / Monitor |
| Push-Rechte | nur `claude/`-Branches (es sei denn freigegeben) | volles lokales Git | volles lokales Git |
| Verfügbarkeit | Pro/Max/Team/Ent. mit Claude Code on the web (Preview) | Cowork: alle Bezahlpläne (nicht Linux) | Claude Code CLI v2.1.72+ |

**Offizielle Wann-was-Regel:** Cloud = „soll zuverlässig ohne deine Maschine laufen". Local =
„braucht lokale Dateien/Tools". `/loop` = „schnelles Polling während einer offenen Session".
**Quelle:** https://code.claude.com/docs/en/scheduled-tasks (Vergleichstabelle, offiziell) ·
https://code.claude.com/docs/en/routines · https://code.claude.com/docs/en/desktop-scheduled-tasks

### So legst du eine geplante Aufgabe an (offiziell, Local Task)
1. Seitenleiste **Routines** → **New routine** → **Local** (für Cloud: **Remote**).
2. Felder: **Name** (wird zu lowercase-kebab-case, dient als Ordnername, muss eindeutig sein) ·
   **Description** (erscheint in der Task-Liste) · **Instructions** (der Prompt; enthält Picker für
   **Permission-Mode** und **Model**, darunter **Working Folder** und **Worktree-Toggle**) · **Schedule**.
3. **Ein Folder ist Pflicht vor dem Speichern** — ist er noch nicht getrustet, fragt Desktop danach.
4. **Schedule-Presets:** Manual (nur „Run now") · Hourly · Daily (Default 9:00 lokal) · Weekdays
   (ohne Sa/So) · Weekly (Tag + Zeit). Sonderintervalle (alle 15 Min, Monatserster, einmalig): Claude
   in Klartext bitten, z. B. *„schedule a task to run all tests every 6 hours"* oder *„remind me at
   3pm tomorrow …"* (einmalig → deaktiviert sich nach dem Feuern selbst).
**Speicherort lokal (zum Direkt-Editieren des Prompts):** `~/.claude/scheduled-tasks/<task-name>/SKILL.md`
bzw. unter Windows der vom Tool gemeldete Pfad `…\Claude\Scheduled\{taskId}\SKILL.md`. **Achtung:**
Schedule, Folder, Model und Enabled-State stehen **nicht** in der `SKILL.md` — die nur übers Edit-Formular
oder via Claude ändern. **Quelle:** https://code.claude.com/docs/en/desktop-scheduled-tasks (offiziell).

---

## 1. Wach-/Fokus-/Client-Bindung — das rote Band durch fast alle Bugs

> Kernmuster: **Lokale Tasks hängen am Client-App-/Fokus-/Wach-Status, nicht an einem Server-Trigger.**
> Daraus folgen Fokus-Bug, stille Skips, gebündeltes Nachfeuern und Boot-Loops. Robuste Alternative:
> Cloud-Routine.

### 1.1 Task läuft nur bei wachem Rechner + offener App — per Design
**Symptom:** Cron-Tasks feuern nicht, wenn die App geschlossen oder der Rechner im Sleep ist.
**Ursache:** Der Scheduler prüft jede Minute, **solange App offen + Rechner wach**. Kein Server-Trigger.
Zugeklappter Laptop schläft trotz „Keep computer awake".
**Versionen:** macOS/Windows, per Design. Verschärft durch 5-Min-Idle-Auto-Quit (#23092).
**FIX:** Settings → Desktop app → General → **„Keep computer awake"** aktivieren. Für Läufe bei
ausgeschaltetem Rechner: **Cloud-Routine**. Community-Variante: dedizierter Mac Mini als 24/7-„Cowork-Server".
**Quelle:** https://code.claude.com/docs/en/desktop-scheduled-tasks (offiziell) · GitHub #44128, #23092 (offizielles Repo).

### 1.2 Tasks feuern nur bei aktiv fokussiertem Cowork-Tab — Bug
**Symptom:** Ein für 15:00 geplanter Task feuert erst ~16:20, sobald man manuell in den Cowork-Tab klickt. App war offen, aber auf Chat-Ansicht.
**Ursache:** Der Scheduler scheint die aktiv fokussierte Cowork-Session zu brauchen (widerspricht der Doku „regardless of active view" → Bug, nicht Design).
**Versionen:** macOS + Win11, Stand März 2026, **OPEN** (belegt).
**FIX:** Cowork-Tab nach App-Start einmal aktiv anklicken; für verlässliche Läufe Cloud-Routine.
**Quelle:** https://github.com/anthropics/claude-code/issues/36131 (offizielles Repo).

### 1.3 Verpasste Tasks feuern beim nächsten Start gebündelt (gleichzeitig)
**Symptom:** Bei geschlossener App laufen Tasks nicht; beim nächsten Start feuern alle ausstehenden quasi gleichzeitig (belegt: zwei Tasks innerhalb 19 ms statt zu ihren Cron-Zeiten).
**Ursache:** Client-gebunden (§1.1); „runs on next launch" sammelt verpasste Läufe und feuert sie beim Start.
**Versionen:** macOS, Cowork Research Preview, **OPEN**. Vom Melder als „essentially unusable" / Regression markiert.
**FIX:** App offen + wach halten (OS-Mittel zum Wachhalten); zeitkritisch → Cloud-Routine. Beachte Catch-up-Logik §2.1.
**Quelle:** https://github.com/anthropics/claude-code/issues/44128 (offizielles Repo).

### 1.4 Mobile/Handy = nur Fernsteuerung des wachen Desktops („Dispatch")
**Symptom:** Erwartung, vom Handy gestartete geplante Arbeit laufe unabhängig — tut sie (lokal) nicht.
**Ursache:** „Dispatch" (Beta, Pro/Max) gibt **einen** durchgehenden Thread über Handy + Desktop mit
geteiltem Kontext. Die Ausführung passiert **immer lokal auf dem Desktop** — „Your desktop must be
active. If your computer is asleep or the Claude Desktop app is closed, Claude can't work on tasks."
Mobile ist reine Trigger-Brücke. Nur **Cloud-Routinen** laufen unabhängig vom Desktop.
**Sicherheitshinweis (offiziell):** Handy-Anweisungen lösen echte Aktionen am PC aus — Phishing/Injection kann kaskadieren.
**FIX:** Für vom Handy ausgelöste, desktop-unabhängige Arbeit eine Cloud-Routine nutzen.
**Quelle:** https://support.claude.com/en/articles/13947068-assign-tasks-from-anywhere-in-claude-cowork (offiziell).

### 1.5 Globaler Scheduler friert ein — alle Tasks stoppen, kein Catch-up beim Relaunch
**Symptom:** Alle Tasks stoppen ab einem Zeitpunkt; `lastRunAt` aller Tasks auf nahezu identische Timestamps
eingefroren; `nextRunAt` rechnet in der Anzeige weiter, feuert aber nie. Quit+Relaunch löst nichts aus.
**Ursache (vermutet):** Ein geteilter Scheduler-Checkpoint („last tick") hängt — möglicherweise nach einem
`update_scheduled_task`, das eine `SKILL.md` auf einem synchronisierten/junction-verlinkten Laufwerk
schrieb (EXDEV-Nähe). Stiller Lock-/Schreibfehler korrumpiert den globalen Scheduler-State für ALLE Tasks.
**Verwandt:** #55378 (Crons am Wochenende werden still gedroppt).
**Versionen:** Claude Code 2.1.170 / Desktop 1.11847.5, Win11, eröffnet 2026-06-10. **CLOSED as duplicate**
(Grundproblem unbehoben).
**FIX:** Toggle/Cron ändern hilft nicht; `SKILL.md`-Storage auf das Systemlaufwerk legen (nicht synced/junctioned).
**Quelle:** https://github.com/anthropics/claude-code/issues/66976 · https://github.com/anthropics/claude-code/issues/55378 (offizielles Repo).

---

## 2. Catch-up / verpasste Läufe

### 2.1 Nur EIN Catch-up-Lauf, evtl. zur falschen Uhrzeit, 7-Tage-Fenster — per Design
**Symptom:** Ein für 9 Uhr geplanter Task läuft erst um 23 Uhr; ein 6 Tage verschlafener Daily-Task läuft beim Aufwachen nur **einmal**, ältere Läufe sind verloren.
**Ursache:** Beim App-Start/Aufwachen prüft Desktop die letzten **7 Tage** und startet **genau einen**
Catch-up für die **zuletzt** verpasste Zeit — zur Aufwach-Zeit, ohne das zu „wissen". Älteres wird verworfen.
**Versionen:** per Design (Doku „Missed runs").
**FIX:** Zeit-Guardrails in den Prompt, wörtliches Anthropic-Beispiel: *„Only review today's commits. If
it's after 5pm, skip the review and just post a summary of what was missed."* Statt *„prepare me for my
next meeting"* lieber *„summarize today's meetings"* (bricht nicht, wenn der Lauf verspätet kommt).
Zeitkritisch/garantiert → Cloud-Routine.
**Quelle:** https://code.claude.com/docs/en/desktop-scheduled-tasks (offiziell).

---

## 3. ⭐ KRITISCH — Freeze / Boot-Loop

### 3.1 High-Frequency-Cron → unrecoverable Startup-Freeze
**Symptom:** Nach einem Task mit häufigem Cron (z. B. `0 21,22,23,0,1,2,3,4,5,6 * * *`, 10 Läufe/Nacht)
friert Claude Desktop bei **jedem** Start ein — keine UI, stiller Hang. Cache löschen / VM neu bauen /
CoworkVMService-Neustart / PC-Reboot helfen **nicht** (sie fassen die Task-Config nicht an).
**Ursache:** Der Catch-up-Mechanismus (§2.1) erkennt beim Start einen verpassten Lauf und hängt in der
Initialisierung **bevor** die UI rendert → Boot-Loop. High-Frequency-Cron garantiert bei jedem Start einen Catch-up-Trigger.
**Versionen:** Claude Code 2.1.71. Windows **#32213 OPEN**; macOS **#32167 CLOSED as not planned/stale** (gleicher Root Cause + gleicher Fix). #32125 (OSX-Hang) verwandt, Status unklar.
**FIX (funktionserhaltend):** Task-Config umbenennen, damit die App wieder bootet, dann moderate Frequenz —
- Windows: `Rename-Item "$env:APPDATA\Claude\local-agent-mode-sessions\<session-uuid>\<task-uuid>\scheduled-tasks.json" "scheduled-tasks.json.bak"`
- macOS: `~/Library/Application Support/Claude/claude-code-sessions/.../scheduled-tasks.json` entfernen/umbenennen.
**Quelle:** https://github.com/anthropics/claude-code/issues/32213 · https://github.com/anthropics/claude-code/issues/32167 (offizielles Repo).

---

## 4. Zombie-Läufe & Skip-Kaskaden

### 4.1 Task dispatcht, bleibt „Running" mit 0 Turns → permanente Skip-Cascade
**Symptom:** Task dispatcht termingerecht, aber der Agent-Turn startet nie. Entweder leere Session (0 Turns)
oder die Session bleibt unbegrenzt „Running" (25+ Min) → alle folgenden Slots werden **Skipped**. Ein Zombie = dauerhafter DoS; Löschen+Neuanlegen hilft nicht.
**Ursache:** Dispatcher und Agent-Runtime entkoppelt — Dispatch erfolgreich, Worker markiert „Running", führt aber weder aus noch gibt frei. Kein Watchdog.
**Versionen:** Claude Code 1.2278.0, Win11, **OPEN** (Regression).
**FIX:** App komplett neu starten; betroffene Tasks pausieren/löschen; zeitweise Cloud-Routine.
**Quelle:** https://github.com/anthropics/claude-code/issues/47899 (offizielles Repo).

### 4.2 Fälschlich „Skipped" nach normal beendetem Vorlauf
**Symptom:** Nach sauberem Exit (exit 0) werden mehrere folgende Stundenläufe fälschlich „Skipped", als liefe der Vorläufer noch. Concurrency-Guard blockiert stundenlang. (Skip-Gründe sieht man beim Hovern in der Run-History: PC schlief, Vorlauf lief noch, oder anderer Task lief — Tasks blockieren sich gegenseitig.)
**Ursache:** „Active session"-State wird nach normalem Exit nicht zurückgesetzt.
**Versionen:** Claude Code 2.1.96, Win11, **CLOSED as duplicate** (von #47899).
**FIX:** Task aus-/wieder-aktivieren + neue Konversation, um den falschen Active-State zu räumen.
**Quelle:** https://github.com/anthropics/claude-code/issues/47022 (offizielles Repo).

### 4.3 Cloud-Routine: Zombie-Lauf „running" über Tage / Cron feuert, Container führt nie aus
**Symptom:** (a) Ein Routine-Lauf hängt tagelang auf „running", kein Output, kein Cancel-Button. (b) Cron feuert, aber der Container führt den Prompt nie aus (`ended_reason: run_once_fired`).
**Ursache:** Kein Timeout/Auto-Beendigung für hängende Remote-Läufe; Dispatch/Ausführung entkoppelt.
**Versionen:** Web/Routines; #51353 **CLOSED**, #54260 (Status unklar).
**FIX:** Routine neu anlegen (Folgeläufe sind meist nicht blockiert); Transkripte prüfen statt Status (§9.3).
**Quelle:** https://github.com/anthropics/claude-code/issues/51353 · https://github.com/anthropics/claude-code/issues/54260 (offizielles Repo).

### 4.4 `wakeScheduler`-Feature-Flag „unavailable" → Tasks feuern nie automatisch
**Symptom:** Tasks erzeugen History-Einträge zur richtigen Zeit, aber der Agent wird nie gespawnt (Endlos-Spinner). Manuelles Anklicken des History-Eintrags startet den Task erfolgreich.
**Ursache:** Renderer-Flag `"wakeScheduler":{"status":"unavailable"}` — obwohl `ccdScheduledTasksEnabled`/`keepAwakeEnabled`/`coworkScheduledTasksEnabled` alle true und der Mac dauerhaft wach/eingesteckt ist.
**Versionen:** macOS (Apple Silicon), Desktop v1.569.0 / Claude Code 2.1.87, **OPEN**.
**FIX:** History-Eintrag manuell anklicken (kein echter Fix); für Verlässlichkeit Cloud-Routine.
**Quelle:** https://github.com/anthropics/claude-code/issues/44129 (offizielles Repo).

---

## 5. Cron-Syntax, Zeitzone & Frequenz

### 5.1 Nur 5-Feld-Cron numerisch — keine Sekunden / `L` `W` `?` / Namens-Aliase / `@`-Macros
**Symptom:** Ein Cron mit `MON`, `JAN`, `L`, `W`, `?`, Sekunden-Feld oder `@daily`/`@reboot` verhält sich nicht wie erwartet.
**Ursache/Fakt:** Akzeptiert wird Standard-**5-Feld-Cron** `Minute Stunde Tag-d-Monats Monat Wochentag` mit
`*`, Einzelwert, `*/n` (Steps), `a-b` (Range), `a,b,c` (Liste). **Nicht unterstützt:** Sekunden-Granularität;
Extended-Syntax `L`/`W`/`?`; Namens-Aliase (`MON`, `JAN`) — Wochentag numerisch `0`/`7`=So bis `6`=Sa.
`@`-Macros (`@daily`, `@reboot`) sind in der Doku nicht als unterstützt gelistet (*ehrlich markiert:* kein
expliziter Verbots-Satz gefunden, sie fehlen schlicht). Tag-d-Monats UND Wochentag beide gesetzt → **ODER**-Logik.
**Einmalige Aufgabe (`fireAt`):** ISO-8601 **mit Zeitzonen-Offset**, z. B. `2026-03-05T14:30:00-08:00`.
Entweder `cronExpression` (recurring) ODER `fireAt` (one-shot); one-shot deaktiviert sich nach dem Feuern selbst.
**FIX:** Cron numerisch + 5-Feld schreiben; einmalig per `fireAt` mit Offset.
**Quelle:** https://code.claude.com/docs/en/scheduled-tasks (offiziell) · `create_scheduled_task`-Tool-Schema (offiziell) · GitHub #57216 (offizielles Repo).

### 5.2 Frequenz-/Mengen-Limits: Cloud ≥ 1 h, Local ≥ 1 Min, max 50 Tasks/Session
**Symptom:** Eine sub-stündliche Cloud-Routine wird abgelehnt; der 51. Task wird abgelehnt.
**Ursache/Fakt:** Cloud-Routine-Mindestintervall **1 Stunde** (bewusste Rate-Limit-Entscheidung); Local/`/loop`
bis **1 Minute**. Eine Session hält max. **50 Tasks**. Stagger/Jitter: Läufe starten ein paar Minuten **nach**
der geplanten Zeit (deterministisch pro Task); One-Shots auf `:00`/`:30` bis 90 s früher.
**FIX:** Für häufigeres Polling Local Task/`/loop`; exaktes One-Shot-Timing → Minute ≠ `:00`/`:30` wählen (z. B. `3 9 * * *`).
**Quelle:** https://code.claude.com/docs/en/scheduled-tasks · https://code.claude.com/docs/en/routines (offiziell).

### 5.3 Cron = lokale Zeitzone (nicht UTC) + DST-Time-Picker-Offset
**Symptom:** Unsicherheit ob `0 9 * * *` UTC oder lokal ist; Time-Picker speichert leicht verschobene Zeit (Auswahl 10:31 → gespeichert 10:30 oder 10:41).
**Ursache:** Cron wird in der **lokalen Zeitzone** ausgewertet (Local UND Cloud: lokale Wandzeit, automatisch
umgerechnet). Der Time-Picker-Offset (-1/+10 Min) ist eine **DST/Locale-Falle**: nach einer US-DST-Umstellung
mit noch nicht umgestelltem Locale falscher UTC-Offset (auf 2 Maschinen reproduziert).
**Versionen:** Win11 (auto-sync), Claude Code 1.1.6041, März 2026 — Time-Picker-Bug Status unklar (mutmaßlich OPEN).
**FIX:** Lokale Zeiten direkt verwenden (keine UTC-Umrechnung). Nach DST-Umstellungen die gespeicherte
Task-Zeit kurz verifizieren; `fireAt` immer mit explizitem Offset.
**Quelle:** https://code.claude.com/docs/en/routines (offiziell, Zeitzone) · https://github.com/anthropics/claude-code/issues/33586 (offizielles Repo, Time-Picker).

### 5.4 Gelöschte Cron-Jobs feuern endlos weiter
**Symptom:** Durable/recurring Cron-Jobs feuern nach einigen Tagen im Rapid-Loop (alle paar Sekunden, 100+ Invocations/Min) statt zur Cron-Zeit; nach Löschen (bestätigt leer) feuern sie **weiter** bis zum Session-Neustart.
**Ursache (vermutet):** Scheduler trackt „last fired" nicht und re-evaluiert den Cron bei jedem Tick → Dauer-Match; gefeuerte Events liegen in einer In-Memory-Queue, die beim Delete nicht geflusht wird.
**Versionen:** Claude Code ≤ 2.1.92, macOS, **OPEN**.
**FIX:** Löschen + **Session/App neu starten**, dann moderat neu anlegen.
**Quelle:** https://github.com/anthropics/claude-code/issues/49198 (offizielles Repo).

---

## 6. Erstellung schlägt fehl / Tool fehlt / Pfad-Fallen

### 6.1 `/schedule` scheitert: `create_scheduled_task` nicht im Session-Kontext (Windows)
**Symptom:** In einem Cowork-Task meldet Claude, `create_scheduled_task` sei nicht in der Tool-Liste, und versucht erfolglose Fallbacks (`crontab`, `gh`). Kein Task entsteht.
**Ursache:** Das MCP-Tool wird in diesem Kontext nicht in den Session-Tool-Kontext injiziert.
**Versionen:** Claude 1.1.4328, Windows, **OPEN** (Regression).
**FIX:** Task per UI anlegen (Routines → New routine → Local) oder in einer normalen Desktop-Session per Klartext bitten.
**Quelle:** https://github.com/anthropics/claude-code/issues/29022 (offizielles Repo).

### 6.2 „Cannot create scheduled tasks from within a scheduled task session" — per Design
**Symptom:** Ein laufender Scheduled-Task kann keinen neuen Folge-Task anlegen.
**Ursache:** Bewusste Sperre gegen Rekursion/Task-Explosion.
**Versionen:** Claude Code 2.1.74, **OPEN** (per Design; Issue fordert Lockerung).
**FIX:** Der Task darf seinen **eigenen** Zeitplan/Prompt per `update_scheduled_task` ändern (offiziell unterstützt — Self-Reschedule, z. B. Review früher legen, wenn ein Release-Branch erkannt wird). Echte Folge-Tasks außerhalb der Task-Session anlegen.
**Quelle:** https://code.claude.com/docs/en/desktop-scheduled-tasks (offiziell) · https://github.com/anthropics/claude-code/issues/34931 (offizielles Repo).

### 6.3 „Failed to create scheduled task" bei UNC-/Fremdlaufwerk-Documents-Ordner
**Symptom:** Task-Erstellung schlägt zu 100 % mit „Failed to create scheduled task." fehl. Reproduzierbar, wenn der Documents-Ordner per Group-Policy-Folder-Redirection auf einen UNC-Pfad (`\\server\share`) zeigt; verwandt auch bei Documents auf anderem Laufwerk (EXDEV-Hardlink).
**Ursache:** Der Task-Storage ist auf `~/Documents/Claude/Scheduled/` festverdrahtet; UNC-Pfad bzw. Laufwerksgrenze brechen das Anlegen.
**Versionen:** Claude Code 2.1.123, Windows. UNC **#56001 OPEN**; EXDEV-Variante **#46534 CLOSED as duplicate**; konfigurierbarer Pfad ist Feature-Wunsch **#54859**.
**FIX:** Working-Folder/Documents auf einen **lokalen** Laufwerksbuchstaben legen; GPO-Redirection für den genutzten Ordner umgehen.
**Quelle:** https://github.com/anthropics/claude-code/issues/56001 · https://github.com/anthropics/claude-code/issues/46534 (offizielles Repo).

### 6.4 SKILL.md nicht aus der Session-VM lesbar / Tasks fehlen in der Sidebar
**Symptom:** (a) Bei Non-Standard-Documents-Pfad ist die `SKILL.md` aus der Session-VM nicht lesbar. (b) In der Desktop-Sidebar wird nur eine Teilmenge der Tasks gerendert, obwohl alle auf Disk liegen und laufen.
**Ursache:** Pfad-/Rendering-Fallen rund um den festverdrahteten Storage-Pfad.
**Versionen:** #34667 **CLOSED as not planned/stale**; #60667 (Sidebar) **OPEN**, macOS.
**FIX:** Standard-Documents-Pfad nutzen; Tasks notfalls direkt auf Disk unter `…\Claude\Scheduled\` prüfen/editieren.
**Quelle:** https://github.com/anthropics/claude-code/issues/34667 · https://github.com/anthropics/claude-code/issues/60667 (offizielles Repo).

---

## 7. Permissions

### 7.1 Permissions bei jedem Lauf neu / Run stallt im Ask-Mode; bypassPermissions wird nicht geerbt
**Symptom:** Im Ask-Mode stallt der Lauf bis zur manuellen Freigabe. Trotz „Always allow" bzw.
`~/.claude/settings.json`-Allow-Rules (inkl. `mcp__slack__*`, `mcp__gmail__*`) und sogar
`"defaultMode":"bypassPermissions"` erscheinen Prompts bei jedem Lauf erneut → unbeaufsichtigte Automatisierung scheitert.
**Ursache:** Der Task-Runner hat einen eigenen Permission-Mode und erbt weder die „Always allow"-Auswahl noch die settings.json-Allow-Rules / den Account-`defaultMode` zuverlässig.
**Versionen:** macOS/Windows. #47180 (Always allow) **OPEN**; #40470 (bypassPermissions) **OPEN** (belegt — Researcher-2-Annahme „closed" korrigiert).
**FIX:** Nach dem Anlegen **„Run now"** klicken und bei jedem Permission-Prompt **„always allow"** wählen →
künftige Läufe dieses Tasks genehmigen dieselben Tools. Genehmigungen pro Task auf der Detailseite review-/widerrufbar. Alternativ den Permission-Mode des Tasks von vornherein passend setzen.
**Quelle:** https://code.claude.com/docs/en/desktop-scheduled-tasks (offiziell) · https://github.com/anthropics/claude-code/issues/47180 · https://github.com/anthropics/claude-code/issues/40470 (offizielles Repo).

---

## 8. Connectors / MCP in der geplanten Session

### 8.1 MCP-Connectors erst nach „Warm-up"-Nachricht verfügbar (Subagent-Workaround)
**Symptom:** Beim automatischen Feuern sind MCP-Connectors (Slack, Gmail, GCal, Notion, Datadog, Jira …) **nicht** im Tool-Set; alle MCP-Calls scheitern. Eine beliebige User-Message initialisiert die Connectors sofort; Retries mit Delay helfen nicht (kein Timing-Problem).
**Ursache:** Scheduled-Task-Sessions initialisieren MCP-Connectors nicht, bevor eine menschliche Interaktion die Session „aufwärmt".
**Versionen:** Claude Code 2.1.78 / Claude 1.1.7053, macOS + Web, **OPEN**. Maintainer im Thread: „We have a fix on the way" — **kein** ausgelieferter Fix belegt, kein Fix-PR verlinkt.
**FIX (von Nutzern bestätigt):** Als **erste Aktion einen Subagent spawnen**, der den ersten MCP-Call absetzt — danach sieht auch der Hauptagent alle MCP-Server. (Passt zur Harness-Regel, Arbeit über sichtbare Subagents zu starten.)
**Quelle:** https://github.com/anthropics/claude-code/issues/35899 (offizielles Repo).

### 8.2 Cloud-Routine: kein lokaler Dateizugriff, nur claude.ai-Connectors, Push nur `claude/`-Branches
**Symptom:** Eine Cloud-Routine sieht lokale Dateien/uncommittete Branches nicht und kann nicht auf beliebige Branches pushen; ein lokal per `claude mcp add` hinzugefügter MCP-Server fehlt.
**Ursache:** Cloud läuft auf Anthropic-Infrastruktur mit frischem Repo-Klon (nur committeter Code vom Default-Branch); nutzt nur **claude.ai-Connectors** des Accounts; darf standardmäßig nur auf `claude/`-präfixierte Branches pushen.
**Versionen:** Research Preview.
**FIX:** Lokalen Dateizugriff/lokale MCP → **Local Task** statt Cloud. Für Cloud: Server als Connector unter claude.ai/customize/connectors hinzufügen ODER `.mcp.json` ins Repo einchecken; „Allow unrestricted branch pushes" pro Repo aktivieren.
**Quelle:** https://code.claude.com/docs/en/routines (offiziell).

### 8.3 Cloud-Routine: Netzwerk default „Trusted" → 403 `host_not_allowed`
**Symptom:** Ausgehende Requests zu eigenen/beliebigen Domains scheitern mit `403`, `x-deny-reason: host_not_allowed`.
**Ursache:** Das Default-Environment nutzt **Trusted**-Netzwerk: nur eine Allowlist (Paket-Registries, Cloud-APIs) ist erreichbar.
**FIX:** Im Environment **Network access** auf **Custom** (eigene Domains in „Allowed domains" eintragen, Default-Liste optional behalten) oder **Full** stellen. Connector-Traffic läuft ohnehin über Anthropic-Hosts.
**Quelle:** https://code.claude.com/docs/en/routines (offiziell).

---

## 9. Frische Session, State & Sicherheit

### 9.1 Jeder Lauf startet frisch ohne Erinnerung — per Design
**Symptom:** Tasks „erinnern" sich weder an das Anlege-Gespräch noch an vorherige Läufe; „mach das wie gestern" funktioniert nicht.
**Ursache:** Jeder fällige Task startet eine frische, unabhängige Session.
**FIX:** Prompt **vollständig selbst-enthaltend** formulieren (welche Connectors, Ausgabeformat, Präferenzen, Zeit-Guardrails). Persistenz nur über Dateien/Git/Connectors, nicht über Konversationsgedächtnis.
**Quelle:** https://code.claude.com/docs/en/desktop-scheduled-tasks (offiziell) · `create_scheduled_task`-Tool-Beschreibung.

### 9.2 Default: Lauf gegen uncommitteten Working-Directory-Stand → Worktree-Toggle
**Symptom:** Ein geplanter Lauf arbeitet gegen den aktuellen, evtl. unsauberen Stand inkl. uncommitteter Änderungen und kollidiert mit manueller Arbeit.
**Ursache:** Standardverhalten („runs against whatever state your working directory is in").
**FIX:** Beim Anlegen den **Worktree-Toggle** aktivieren → jeder Lauf bekommt einen isolierten Git-Worktree.
**Quelle:** https://code.claude.com/docs/en/desktop-scheduled-tasks (offiziell).

### 9.3 Grüner Status ≠ Aufgabe erfolgreich (Cloud-Routine)
**Symptom:** Eine Routine zeigt grünen Status, aber die Aufgabe ist fehlgeschlagen (blockierte Requests, fehlende Connector-Tools, Task-Fehler).
**Ursache:** Grün heißt nur „Session gestartet + ohne Infrastruktur-Fehler beendet", nicht „Prompt erfüllt".
**FIX:** Jeden Lauf öffnen und das **Transkript lesen**, um zu bestätigen, was Claude tatsächlich getan hat.
**Quelle:** https://code.claude.com/docs/en/routines (offiziell).

### 9.4 ⭐ KRITISCH — Prompt-Injection bei unbeaufsichtigten Läufen
**Symptom:** Ein geplanter Task mit Zugriff auf Mail/Kalender/Dateien führt schädliche Aktionen aus, wenn eine eingeschleuste Injektion beim (unbeaufsichtigten) Lauf aktiviert wird — niemand kann in Echtzeit stoppen.
**Ursache:** Indirekte Prompt-Injection (versteckt in Dokumenten, Webseiten, E-Mails, Kalender-Events) ist die #1-Bedrohung. Belegtes Beispiel: ein bösartiges Google-Calendar-Event konnte Code-Ausführung triggern (extern als CVSS 10/10 bewertet — *gegen Primärquelle gegenzuprüfen*). Anthropic nennt Agent-Sicherheit „active area of development".
**FIX:** Keine sensiblen Connectors/Ordner an unbeaufsichtigte Tasks hängen; **destruktive Guardrails** in den Prompt („do NOT delete anything / do NOT send anything"); **Failure-Cost-Tiering** — Tier 1 (verpasster Report) jedes Tool ok; Tier 2 (falsche Daten) Review-Schritt einbauen; Tier 3 (E-Mail/Zahlung/Löschen/Publish) **nie** unbeaufsichtigt mit Single-Layer-Tool automatisieren.
**Quelle:** https://support.claude.com/en/articles/13364135 (offiziell, Cowork sicher nutzen) · harmonic.security · ninetwothree.co · theregister.com (extern).

---

## 10. Quota / Limits

### 10.1 Geplante Tasks verbrauchen viel Usage; Cloud hat tägliches Run-Cap
**Symptom:** Mehrere schwere Tasks täglich treiben (v. a. auf Pro) schnell ans Limit; Cloud-Routinen werden „rejected until the window resets".
**Ursache/Fakt:** Jeder Lauf ist eine **volle Session** und zieht Subscription-Usage wie interaktive Sessions.
Cloud-Routinen haben zusätzlich ein **tägliches Run-Cap pro Account** (plan-abhängig); GitHub-Trigger haben
per-routine/per-account-Stundencaps. **Ausnahme:** einmalige (One-off-)Cloud-Läufe zählen **nicht** gegen das tägliche Run-Cap (verbrauchen aber normale Usage).
**FIX:** Leichte Tasks bündeln (1 Morning-Brief mit Mail+Kalender+News statt 3 Tasks); erste 2–3 Läufe prüfen und den Prompt straffen; bei aktivierten Usage-Credits Metered-Overage (Settings → Billing); verbleibende Läufe unter claude.ai/code/routines bzw. claude.ai/settings/usage einsehen.
**Quelle:** https://code.claude.com/docs/en/routines (offiziell) · promptguy.io · buildtolaunch.substack.com (extern).

---

## 11. Fix-Status (Stand 2026-06-15, ehrlich getrennt)

> Methodik: Issue-Header direkt gelesen; offizieller `CHANGELOG.md` bis **v2.1.173** (2026-06-11) durchsucht.
> **Kein** untersuchter Scheduled-Task-Bug ist per offiziellem Changelog als gefixt bestätigt. Der einzige
> angrenzende Changelog-Eintrag („Fixed sandbox network permission prompts … in auto and bypass-permissions
> mode") betrifft Sandbox-**Netzwerk**-Prompts generell — **nicht** belegt als Fix für #40470. Bezug daher unklar.

| Issue # | Thema | Belegter Status |
|---------|-------|-----------------|
| #32213 | High-Freq-Cron-Freeze (Windows) | **OPEN** |
| #32167 | scheduled-tasks.json-Freeze (macOS) | **CLOSED — not planned/stale** (nicht gefixt) |
| #32125 | OSX-Hang beim Start | OPEN (mutmaßlich, nicht hart verifiziert) |
| #36131 | Feuert nur bei fokussiertem Cowork-Tab | **OPEN** |
| #44128 | App zu = kein Lauf / Catch-up-Burst | **OPEN** |
| #44129 | `wakeScheduler` unavailable | **OPEN** |
| #47899 | Zombie „Running"/Skip-Cascade | **OPEN** |
| #47022 | Fälschlich „Skipped" nach Exit | **CLOSED — duplicate** |
| #49198 | Gelöschte Crons feuern endlos weiter | **OPEN** |
| #29022 | `create_scheduled_task` fehlt (/schedule) | **OPEN** |
| #34931 | „Cannot create from within" (Rekursionssperre) | **OPEN** (per Design) |
| #56001 | „Failed to create" bei UNC-Documents | **OPEN** |
| #46534 | EXDEV (Documents anderes Laufwerk) | **CLOSED — duplicate** |
| #34667 | SKILL.md nicht aus VM lesbar | **CLOSED — not planned/stale** |
| #60667 | Tasks fehlen in der Sidebar | **OPEN** |
| #47180 | „Always allow" nicht geerbt | **OPEN** |
| #40470 | bypassPermissions nicht geerbt | **OPEN** (Researcher-2-Annahme „closed" korrigiert) |
| #35899 | MCP-Connectors brauchen Warm-up | **OPEN** („fix on the way", nicht ausgeliefert) |
| #66976 | Globaler Scheduler eingefroren | **CLOSED — duplicate** (Grundproblem offen) |
| #51353 | Cloud-Routine-Zombie | **CLOSED** |
| #33586 | DST-Time-Picker-Offset | Status unklar (mutmaßlich OPEN) |
| #54260 / #55378 | Cloud cron fires-but-no-run / Weekend-silent-drop | Status unklar (mutmaßlich OPEN) |

**Noch NICHT gefixt (Workaround bleibt aktiv):** alle oben mit **OPEN** — insbesondere High-Freq-Freeze
(§3.1), Zombie/Skip (§4.1), Fokus-Bindung (§1.2), Permission-Vererbung (§7.1), MCP-Warm-up (§8.1).
**Ehrlichkeits-Hinweis:** „mutmaßlich OPEN" = nur aus Suchsnippet, Header nicht einzeln geöffnet — nicht als belegt werten.

---

## ✅ Pflicht-Checkliste vor dem Anlegen einer geplanten/wiederkehrenden Aufgabe

- [ ] **System bewusst gewählt** (§0): zuverlässig ohne wachen PC → Cloud-Routine; lokale Dateien/Tools → Local Task; nur in offener Session → `/loop`.
- [ ] **Prompt selbst-enthaltend** (§9.1): Connectors, Ausgabeformat, Präferenzen, Zeit-Guardrails drin.
- [ ] **Zeit-Guardrails** gegen Catch-up zur falschen Uhrzeit (§2.1): „nur heutige Daten; nach 17 Uhr überspringen".
- [ ] **Cron geprüft** (§5.1/§5.2): 5-Feld numerisch, keine Sekunden/`L`/`W`/Aliase/`@`-Macros; Cloud ≥ 1 h; einmalig = `fireAt` mit Offset.
- [ ] **Keine High-Frequency-Cron** (§3.1) → sonst Boot-Loop-Risiko.
- [ ] **Permissions vorab** (§7.1): nach Anlegen „Run now" + pro Tool „always allow".
- [ ] **MCP nötig?** (§8.1) → ersten Call von einem Subagent absetzen lassen (Warm-up).
- [ ] **Working-Folder lokal** (§6.3), nicht UNC/Fremdlaufwerk/synced/junction.
- [ ] **Wach/offen** (§1.1): „Keep computer awake" an, Cowork-Tab fokussiert — oder Cloud-Routine.
- [ ] **Worktree-Toggle** bei Code-Tasks (§9.2).
- [ ] **Sicherheit** (§9.4): keine sensiblen Connectors an unbeaufsichtigte Tasks; destruktive Guardrails; Failure-Cost-Tier beachten.
- [ ] **Nach dem 1. Lauf** Transkript prüfen (§9.3) — grüner Status ≠ Erfolg.

---

## 🔗 Bezug zur Best-Practices-Gegenseite

Bug-Almanach (diese Datei) ↔ Best-Practices [`best-practices/claude-tooling/cowork-scheduled-tasks.md`](../../best-practices/claude-tooling/cowork-scheduled-tasks.md) (dedizierte Gegenseite). Links die *Falle*, rechts die *Regel, die sie verhindert*.

| Bug-Abschnitt (dieser Almanach) | Adressiert durch Best-Practice (best-practices-cowork-scheduled-tasks.md) |
|---------------------------------|--------------------------------------------------------------|
| §0 System-Wahl · §1 Wach/Fokus · §1.4 Mobile | „Cloud-Routine für Zuverlässigkeit; Local nur für lokale Dateien" |
| §2.1 Catch-up | „Zeit-Guardrails in den Prompt" |
| §3.1 High-Freq-Freeze | „moderate Cron-Frequenz" |
| §5 Cron/Zeit | „5-Feld-Cron, lokale Zeit, fireAt mit Offset" |
| §7.1 Permissions | „Run now + always allow pro Task" |
| §8.1 MCP-Warm-up | „ersten MCP-Call per Subagent" |
| §9.1/§9.2 frische Session/Worktree | „selbst-enthaltender Prompt + Worktree-Toggl