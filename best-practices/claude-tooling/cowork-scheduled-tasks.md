# Cowork — Geplante & wiederkehrende Aufgaben — Best Practices (Stand 2026-06-15)

> Die „richtige Seite der Medaille" zu [`bugs/claude-tooling/cowork-scheduled-tasks.md`](../../bugs/claude-tooling/cowork-scheduled-tasks.md):
> *wie man geplante/wiederkehrende Aufgaben (Scheduled Tasks / Routines / `/loop`) in Cowork von
> vornherein RICHTIG anlegt und betreibt*. Während der Almanach sagt *was schiefgeht*, sagt diese
> Datei *wie man es gut macht*.
>
> **Versions-Anker:** Cowork hat keinen klassischen Changelog — Anker ist der Stand der offiziellen
> Anthropic-Doku/Support-Seiten (Quellenstand bis Juni 2026). Live recherchiert am **2026-06-15** mit
> 3 Fokus-Researchern (Task-Design, Sicherheit, Praxis-Workflow), aufbauend auf der 5-Researcher-Recherche
> des Almanachs.
> **Quellen-Rangordnung:** offiziell (code.claude.com/docs, support.claude.com, anthropic.com,
> mitgelieferte `schedule`-Skill, MCP-Tool-Schemas) = Grundwahrheit; Community als `extern` gelabelt
> (überstimmt nie Offizielles). Jeder Eintrag trägt Quelle + `offiziell`/`extern`-Flag.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Welches System? | Failure-Cost-Test + 5-Fragen-Routing: Cloud (ohne wachen PC) · Local (lokale Dateien) · `/loop` (nur in Session) | §A1, §A2 |
| 2 | Prompt schreiben | Selbst-enthaltend: Ziel, Schritte, Pfade/URLs/Repos, Connectors **namentlich**, Ausgabeformat, Präferenzen | §B1, §B2 |
| 3 | Catch-up-Verspätung | Zeit-Guardrails: „nur heutige Daten; nach 17 Uhr nur Summary"; feste Fenster statt relativer Bezüge | §B3 |
| 4 | Was soll Claude IGNORIEREN? | Explizit benennen („skip Newsletter/Auto-Mails") — niemand korrigiert live | §B4 |
| 5 | Kadenz | Presets bevorzugen, Cron nur für Sonderfälle; Min: Local 1 Min / Cloud 1 h | §C1, §C2 |
| 6 | Exaktes Timing | Wegen Jitter ungerade Minute wählen (`3 9 * * *` statt `0 9`) | §C3 |
| 7 | Model & Worktree | Model pro Task passend; Worktree-Toggle bei Git-Repos mit uncommitteter Arbeit | §D1, §D3 |
| 8 | Permissions vorab | Nach Anlegen **„Run now"** + pro Tool **„always allow"** (sonst Stall im Ask-Mode) | §D2 |
| 9 | ⭐ Sicherheit: Start | Read-only zuerst (Briefings/Reports); keine sensiblen Daten/Connectors, keine irreversiblen Aktionen | §E1, §E2 |
| 10 | ⭐ Sicherheit: Scope | Dedizierter Ordner + minimaler Mount-Modus (read-only > no-delete > read-write); „do NOT delete/send" im Prompt | §E3, §E4 |
| 11 | Failure-Cost-Tier | Tier 1 unbeaufsichtigt ok · Tier 2 nur mit Review · Tier 3 (Geld/Mail/Löschen) nie unbeaufsichtigt | §E5 |
| 12 | Zuverlässigkeit | „Keep computer awake"; Garantie nur per Cloud-Routine (oder 24/7-Rechner) | §F1, §F2 |
| 13 | Verifikation | „Run now" testen + Transkript lesen — grüner Status ≠ Erfolg; Run-History für Skip-Gründe | §G1, §G2 |
| 14 | Verwalten/Verfeinern | SKILL.md auf Disk editieren; Self-Reschedule per `update_scheduled_task`; Verwaltung per Klartext | §G3, §G4 |
| 15 | Dauerkontext | `claude.md` im Arbeitsordner (Rolle, Projekte, VIPs, Prioritäten) → relevantere, kürzere Prompts | §G5 |
| 16 | Quota | Jeder Lauf = volle Session → leichte Tasks bündeln, Frequenz maßvoll, erste Läufe prüfen | §H1 |

> **Goldene Grundregel:** Erst die *Failure-Cost* bestimmen, dann das System wählen — und den Prompt so
> schreiben, dass er **kalt, unbeaufsichtigt und evtl. zur falschen Uhrzeit** trotzdem das Richtige tut.

---

## A. System-Wahl & Grundsatz

### §A1. Failure-Cost-Test als oberste Entscheidungsregel
**Best Practice:** Vor jeder Automatisierung fragen: „Was passiert, WENN dieser Lauf fehlschlägt?" — nicht „wird er fehlschlagen". Tier 1 (nur ein fehlendes Briefing) → jedes Claude-Tool reicht; Tier 2 (falsche Daten geschrieben) → Task **mit Review-Schritt**, nie „set and forget"; Tier 3 (Mail raus / Daten gelöscht / Zahlung) → nie mit einem Single-Layer-Tool unbeaufsichtigt.
**Begründung:** Entkoppelt die Tool-Wahl vom Bauchgefühl; deckt sich mit Anthropics offizieller Regel „avoid sensitive data and consequential actions".
**Quelle:** buildtolaunch.substack.com/p/claude-cowork-scheduled-tasks-vs-routines-vs-loop · `extern` (gestützt von support.claude.com/articles/13364135 · `offiziell`).

### §A2. 5-Fragen-Routing (feste Reihenfolge) → genau ein System pro Job
**Best Practice:** (1) Muss es laufen, während die Maschine AUS ist? → **Cloud-Routine**, fertig. (2) Braucht es lokale Dateien/MCPs/uncommitteten Kontext? → **Local Task** (bzw. `/loop` nur während aktiver Session). (3) Braucht es Gedächtnis über Läufe? → das ist die harte Decke nativer Tools (über Dateien/Git lösen). (4) Berührt es externe Systeme (CRM/Payment/Prod-DB)? → externer Automatisierer (n8n/Zapier) + Approval. (5) Was bricht bei Fehlschlag? → Failure-Cost (§A1).
**Offizielle Kurzregel (wörtlich):** Cloud „for work that should run reliably without your machine" · Local „when you need access to local files and tools" · `/loop` „for quick polling during a session".
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks (Tip) · `offiziell` · buildtolaunch (5-Fragen) · `extern`.

### §A3. „Repeatable, scoped, reviewable" → starte mit einer Routine, nicht mit einem Agentensystem
**Best Practice:** Ist eine Aufgabe wiederholbar, eng abgegrenzt und prüfbar, ist eine Routine der richtige Default. Erfahrungswert: ~80 % früherer schwergewichtiger Workflows (Quelle lesen, Datei prüfen, Brief schreiben, Inbox aufräumen) passen in Routines; nur ~20 % (persistentes Gedächtnis, voller FS-Zugriff) brauchen mehr.
**Begründung:** Routines sind „die fehlende Mitte" — schnell aufgesetzt, stark genug für echte wiederkehrende Arbeit; spart Over-Engineering.
**Quelle:** buildtolaunch.substack.com · `extern`.

### §A4. Cowork denkt, klassische Automatisierer schaufeln Daten
**Best Practice:** Reasoning nötig (zusammenfassen, analysieren, entscheiden, Report) → Cowork-Task. Hochvolumiges, deterministisches „wenn X dann Y" → Zapier/Make/n8n. Sie ergänzen sich („Zapier moves data, Cowork thinks about it").
**Quelle:** petrvojacek.cz/en/blog/claude-cowork-daily-briefing · `extern`.

---

## B. Prompt-/Instruktions-Design

### §B1. Prompt muss selbst-enthaltend sein (kalt ausführbar)
**Best Practice:** So schreiben, dass eine völlig frische Session ihn „kalt" ausführen kann — nie auf „die obige Konversation"/„das gerade Besprochene" verweisen. Reinnehmen: (1) klares Ziel, (2) konkrete Schritte, (3) alle Pfade/URLs/Repos/Tool-Namen, (4) Ausgabeformat/Erfolgskriterium, (5) Constraints/Präferenzen. In zweiter Person Imperativ.
**Begründung:** Jeder Lauf startet „fresh, with no memory of this conversation".
**Quelle:** mitgelieferte `schedule`-Skill + Tool-Beschreibung `create_scheduled_task` · `offiziell`.

### §B2. Connectors namentlich im Prompt benennen
**Best Practice:** Ausdrücklich nennen, welche Connectors zu nutzen sind (z. B. „nutze den Gmail- und den Google-Calendar-Connector"), inkl. Ausgabeformat und Präferenzen.
**Begründung:** Die Tool-Beschreibung verlangt wörtlich „include which connectors to use, the output format, and any preferences". Zusätzlich nützt es gegen die MCP-Warm-up-Falle (Almanach §8.1).
**Quelle:** Tool-Beschreibung `create_scheduled_task` · `offiziell`.

### §B3. Zeit-Guardrails einbauen (gegen Catch-up zur falschen Uhrzeit)
**Best Practice:** Zeit-/Zustandsschranken in die Instruktion schreiben, weil ein „9-Uhr-Task" real auch um 23 Uhr laufen kann. Feste Fenster statt relativer Bezüge: „Fasse die Commits von HEUTE zusammen. Wenn es nach 17 Uhr ist, überspringe das Review und poste nur, was verpasst wurde." Statt „bereite mein nächstes Meeting vor" → „fasse die heutigen Meetings zusammen".
**Begründung:** Catch-up-Läufe feuern verspätet; ohne Guardrail wird der Output inhaltlich falsch.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks („Missed runs") · `offiziell`.

### §B4. Explizit sagen, was zu IGNORIEREN/SKIPPEN ist + Ausgabeformat festlegen
**Best Practice:** Unbeaufsichtigt fasst Claude sonst jede Marketing-Mail zusammen → „skip newsletters/auto-notifications" hineinschreiben. Ausgabeformat hart definieren („ein Satz pro Item", „scannbar, keine Absätze"). Speicherort konkret („outputs/morning-brief.md" statt „speichere das").
**Begründung:** Niemand korrigiert den Lauf live — Präzision im Prompt ersetzt die fehlende Aufsicht.
**Quelle:** promptguy.io/claude-cowork-scheduled-tasks (Good/Bad-Prompt-Muster) · `extern`.

### §B5. Offizielle Use-Case-Vorlagen nutzen (klares Outcome)
**Best Practice:** An den offiziellen Beispieltypen orientieren — Daily Code Review, Weekly Dependency Audit, Morning Briefing (aus Kalender+Inbox), Backlog-Pflege, Alert-Triage, Deploy-Verifikation, Docs-Drift. Anthropic empfiehlt Tasks „unattended, repeatable, and tied to a clear outcome".
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks + /routines („Example use cases") · `offiziell`.

---

## C. Cadence / Zeitplan

### §C1. Presets bevorzugen, Cron nur für Sonderfälle
**Best Practice:** Schedule-Presets nutzen — Manual (nur „Run now"), Hourly, Daily (Default 9:00 lokal), Weekdays (ohne Sa/So), Weekly (Tag+Zeit). Sonderfälle (alle 15 Min, Monatserster, einmaliger Zukunftslauf) per Klartext: „run all the tests every 6 hours". Cron wird in **lokaler Zeit** ausgewertet (nicht UTC).
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks („Schedule options") · `offiziell`.

### §C2. Mindestintervall + maßvolle Frequenz
**Best Practice:** Local/`/loop` Minimum **1 Minute**, Cloud Minimum **1 Stunde** (häufigere Cron-Ausdrücke werden abgelehnt). Frequenz am echten Bedarf koppeln, nicht „so oft wie möglich": Monitoring oft stündlich, Briefings Weekday-Daily, Reports/Competitor-Watch wöchentlich. Eine wöchentliche Tiefen-Analyse schlägt sieben halbgare tägliche.
**Begründung:** Weniger Läufe = weniger Quota + weniger Rauschen.
**Quelle:** Vergleichstabelle der Doku · `offiziell` · dev.classmethod.jp · `extern`.

### §C3. Jitter bewusst einplanen — nicht gegen `:00`/`:30` planen
**Best Practice:** Bei exaktem Timing eine ungerade Minute wählen (`3 9 * * *` statt `0 9 * * *`), dann greift der One-Shot-Jitter nicht. Generell keine sekundengenaue Planung erwarten: Desktop-Tasks bekommen einen deterministischen Offset von einigen Minuten (gleicher Task = gleicher Offset), stündliche bis ~10 Min, `/loop`-Recurring bis 30 Min.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks + /scheduled-tasks („Jitter") · `offiziell`.

### §C4. `/loop` nicht für Dauerbetrieb (7-Tage-Verfall)
**Best Practice:** `/loop` nur für kurzlebiges In-Session-Polling (z. B. `/loop 5m check CI on PR #247`). Für Dauerhaftes Desktop-Task oder Cloud-Routine — `/loop` ist session-gebunden und verfällt nach 7 Tagen (ein letzter Lauf, dann Selbstlöschung).
**Quelle:** code.claude.com/docs/en/scheduled-tasks („Seven-day expiry") · `offiziell`.

---

## D. Model, Permission-Mode & Worktree

### §D1. Model pro Task gezielt setzen
**Best Practice:** Den Model-Picker im Instructions-Feld nutzen — anspruchsvolle Analyse → stärkstes Modell; einfaches Polling/Briefing → schlankeres (spart Quota). Bei Cloud-Routinen gilt das gewählte Modell für jeden Lauf.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks + /routines · `offiziell`.

### §D2. Permission-Mode bewusst wählen + „Run now" zum Vorautorisieren
**Best Practice:** Jeder Task hat einen eigenen Permission-Mode. Im Ask-Mode **stallt** der Lauf, wenn ein Tool keine Freigabe hat → direkt nach dem Anlegen **„Run now"** klicken und pro Tool **„always allow"** wählen. Allow-Rules aus `~/.claude/settings.json` gelten zusätzlich. Cloud-Routinen haben keinen Permission-Mode (Scoping nur über Repos/Environment/Connectors).
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks („Permissions") · `offiziell`.

### §D3. Worktree-Toggle bei Git-Repos mit uncommitteter Arbeit
**Best Practice:** Toggle aktivieren, wenn der Task in einem Git-Repo läuft und nicht gegen deinen aktuellen (auch uncommitteten) Stand arbeiten soll → jeder Lauf bekommt einen isolierten Worktree (`<project-root>/.claude/worktrees/`).
**Begründung:** Verhindert Kollisionen mit manueller Arbeit, macht Läufe reproduzierbar.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks (Worktree-Note) · `offiziell`.

---

## E. Sicherheit (unbeaufsichtigte Läufe)

> Mentales Modell von Anthropic: Risiko = **was Claude lesen darf** × **was Claude tun darf**. Prompt-Injection
> gelingt nur, wenn beides gleichzeitig wahr ist — eine Seite brechen senkt das Risiko drastisch.

### §E1. Read-only zuerst, Vertrauen über Läufe aufbauen
**Best Practice:** Geplante Tasks anfangs nur für unkritische Read-only-Arbeit (Zusammenfassungen, Briefings, Recherche). Komplexeres erst automatisieren, wenn es über mehrere Läufe stabil war.
**Quelle:** support.claude.com/en/articles/13364135 · `offiziell`.

### §E2. Keine sensiblen Daten/Connectors, keine irreversiblen Aktionen
**Best Practice:** Keine Tasks, die auf sensible Dateien zugreifen, in deinem Namen senden, Käufe tätigen oder schwer rückgängig zu machende Aktionen ausführen. Sensible Connectors (Banking, Health, Finanz) gar nicht erst mit einem unbeaufsichtigten Task verbinden. Minimaler Connector-Scope pro Task.
**Quelle:** support.claude.com/en/articles/13364135 · `offiziell`.

### §E3. Dedizierter Ordner + minimaler Mount-Modus
**Best Practice:** Eigenen, kleinen Arbeitsordner statt breitem Zugriff; wichtige Dateien backuppen. Den am wenigsten privilegierten Mount-Modus wählen, der die Aufgabe noch erfüllt: **read-only** (Briefings/Reports) > **read-write-no-delete** > **read-write**. Enterprise: per MDM-Allowlist erzwingbar.
**Begründung:** Der gemountete Ordner ist das Einzige, was ein kompromittierter Claude erreichen kann — also klein halten. Symlink-Auflösung erfolgt vor Pfadvalidierung (kein Ausbruch).
**Quelle:** support.claude.com/en/articles/13364135 + anthropic.com/engineering/how-we-contain-claude · `offiziell`.

### §E4. Destruktive Guardrails in den Prompt (als Ergänzung, nicht als alleinige Schicht)
**Best Practice:** Explizite Verbote schreiben („do NOT delete files", „do NOT send messages", „only summarize, do not act"), Scope eng fassen. **Wichtig:** Das ist nur eine probabilistische Modell-Schicht — IMMER mit der deterministischen Umgebungsschicht (Mount-Modus §E3, Connector-Scope §E2) kombinieren.
**Quelle:** support.claude.com/en/articles/14128542 + anthropic.com/engineering/how-we-contain-claude · `offiziell`.

### §E5. Failure-Cost-Tiering anwenden
**Best Practice:** Tier 1 (unbeaufsichtigt ok): Read-only, Briefings, Recherche, Datei-Organisation im dedizierten Ordner — leichte Aufsicht, Stichproben. Tier 2 (nur mit Review/Approval): begrenzte Write-Aktionen — Entwürfe (nicht senden), no-delete-Schreiben, interne Tickets. Tier 3 (nie unbeaufsichtigt): Finanztransaktionen, Senden im eigenen Namen, Löschen, PII/regulierte Daten, Policy-Änderungen → Multi-Step-Verifikation + Mensch-im-Loop.
**Quelle:** support.claude.com/en/articles/13364135 (Tier-Vorgabe) · `offiziell` · atlan.com/know/ai-agent-risks-guardrails (3-Tier-Struktur) · `extern`.

### §E6. Kein „Act without asking" für Unbeaufsichtigtes; Umgebungsgrenze schlägt Klick-Genehmigung
**Best Practice:** „Act without asking"/Auto-Mode nur bei aktiver Überwachung + vertrauenswürdigen Quellen + Stop-Möglichkeit — bei geplanten Tasks ist keine dieser Bedingungen erfüllt. Lieber harte Umgebungsgrenzen (Mount-Modus, Egress, Connector-Scope) als sich auf manuelles Genehmigen verlassen (Nutzer genehmigen ~93 % der Prompts → fehlbar).
**Quelle:** support.claude.com/en/articles/13364135 + anthropic.com/engineering/how-we-contain-claude · `offiziell`.

### §E7. Memory-Poisoning + Compliance/Audit-Gap kennen
**Best Practice:** Bewusst sein, dass geplante Tasks State über Sessions behalten (Memory, CLAUDE.md, Workspace) — eine einmal eingeschleuste Injektion wird bei jedem Start neu geladen → Memory/State sauber halten, keine untrusted-Inhalte dauerhaft schreiben lassen. Cowork-Aktivität wird NICHT in der Compliance API erfasst; für Audit Team/Enterprise per OpenTelemetry ins SIEM streamen (OTel-Events enthalten Prompt-Volltext/Tool-Parameter/E-Mail → ggf. redacten).
**Quelle:** anthropic.com/engineering/how-we-contain-claude + support.claude.com/en/articles/14477985 · `offiziell`.

---

## F. Zuverlässigkeit

### §F1. „Keep computer awake" + Task knapp nach Rechnerstart legen
**Best Practice:** Settings → Desktop app → General → **Keep computer awake** aktivieren (Deckel zuklappen schläft trotzdem). Task-Zeit ein paar Minuten nach dem üblichen Rechnerstart legen, damit der morgendliche Catch-up sauber greift.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks · `offiziell` · petrvojacek.cz · `extern`.

### §F2. Garantie nur per Cloud-Routine (oder 24/7-Rechner)
**Best Practice:** Muss ein Lauf zwingend zur Zeit X passieren, unabhängig vom Maschinenzustand → **Cloud-Routine** (läuft auch bei ausgeschaltetem Rechner, Min. 1 h). Alternative für lokale Tasks: dauerhaft eingeschalteter Rechner („Cowork-Server", App offen). Lokale Tasks geben bei Verpassen nur EINEN Catch-up — kein Ersatz für harte Garantie.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks · `offiziell` · promptguy.io / buildtolaunch · `extern`.

### §F3. macOS-Benachrichtigungen vorab erlauben
**Best Practice:** Auf macOS Claude-Notifications auf „Allow" stellen, damit Lauf-/Catch-up-Benachrichtigungen sichtbar sind.
**Quelle:** dev.classmethod.jp · `extern`.

---

## G. Verifikation & Verwaltung

### §G1. „Run now" als Erst-Test
**Best Practice:** Nach dem Anlegen sofort „Run now" — validiert den Prompt und autorisiert Permissions vor, ohne auf den nächsten Slot zu warten.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks · `offiziell`.

### §G2. Run-History + Transkript lesen — Status ≠ Erfolg
**Best Practice:** Auf der Detailseite die Run-History öffnen und das Transkript jedes Laufs lesen. Hover über Skipped-Einträge zeigt den Grund (Rechner schlief / Vorlauf aktiv / anderer Task lief). Bei Cloud-Routinen nicht dem grünen Status vertrauen — „green … means the session started and exited without an infrastructure error. It does not mean the task succeeded." Erste 2–3 Läufe aktiv kontrollieren und den Prompt nachschärfen.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks + /routines · `offiziell` · promptguy.io · `extern`.

### §G3. SKILL.md auf Disk editieren für schnelle Tweaks
**Best Practice:** Task-Prompts liegen als `~/.claude/scheduled-tasks/<task-name>/SKILL.md` (YAML-Frontmatter `name`/`description`, Body = Prompt). Direkt editieren → greift beim nächsten Lauf. **Achtung:** Schedule, Folder, Model, Enabled stehen NICHT in der Datei — die nur über Edit-Form/Klartext ändern.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks · `offiziell`.

### §G4. Self-Reschedule + Verwaltung per Klartext
**Best Practice:** Ein laufender Task kann via `update_scheduled_task` seinen eigenen Schedule/Prompt ändern (z. B. Review früher legen, wenn ein Release-Branch erkannt wird) — für adaptive Workflows nutzen (partielle Updates möglich; eigene ID = `name`-Attribut im `<scheduled-task name>`-Tag). Verwaltung per Zuruf: „pause my dependency-audit task", „show my scheduled tasks", „remind me at 3pm to check the deploy".
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks + `update_scheduled_task`-Schema + `schedule`-Skill · `offiziell`.

### §G5. `claude.md` im Arbeitsordner als Dauerkontext
**Best Practice:** Eine `claude.md` mit Rolle, aktuellen Projekten, VIP-Kontakten, Prioritäten anlegen — Claude liest sie bei jedem Lauf → Outputs werden relevanter und der Prompt kürzer (gut für Qualität UND Quota).
**Quelle:** petrvojacek.cz · promptguy.io/set-up-claude-cowork · `extern`.

### §G6. Least-Privilege regelmäßig auditieren
**Best Practice:** Im „Always allowed"-Panel der Task-Detailseite gespeicherte Tool-Freigaben prüfen und unnötige widerrufen. Ungenutzte Tasks pausieren/löschen statt im Hintergrund weiterlaufen lassen.
**Quelle:** code.claude.com/docs/en/desktop-scheduled-tasks + support.claude.com/en/articles/13364135 · `offiziell`.

---

## H. Bewährte Task-Typen & Kosten

### §H1. Kosten-/Usage-Effizienz: bündeln, maßvoll, früh prüfen
**Best Practice:** Jeder Lauf ist eine volle Cowork-Session und verbraucht mehr Quota als ein Chat → verwandte leichte Arbeit in EINEN Task bündeln (ein Morning-Brief mit Mail+Kalender+News statt drei Tasks). Frequenz maßvoll (§C2). Erste Läufe prüfen und Prompt straffen (Self-Refinement: Claude schreibt den Prompt nach dem ersten Lauf oft selbst um). Plan an die Last anpassen.
**Quelle:** promptguy.io · petrvojacek.cz · `extern` (Quota-Hinweis auch claudefa.st-FAQ).

### §H2. Bewährte, „sticky" Task-Typen
**Best Practice:** Erprobte Muster mit gutem Aufwand/Nutzen-Verhältnis:
- **Daily Morning Briefing** (Weekday-Daily): aus 3–5 Quellen → ein strukturierter Output (Kalender, antwortbedürftige Mails ohne Newsletter, 1–2 Metriken, vorgeschlagene Priorität).
- **Error-Log-Monitoring → PRs** (stündlich, Local + MCP): Logs lesen, Rauschen filtern, für lohnende Issues PRs öffnen → du reviewst PRs statt Logs.
- **Inbox-Triage mit Draft-Only-Guardrail** (Weekday-Daily): kategorisieren + Ein-Satz-Summary; kurze Antworten als `.txt`-Entwürfe ablegen, „do NOT send" (Tier 2).
- **Wöchentliche Reports / Competitor-Watch**: Daten kompilieren UND Trends/Anomalien kommentieren; Output in datierte Dateien.
- **Datei-/Beleg-Verarbeitung** (Local): Downloads sortieren (alt → Archiv, „do NOT delete"), Belegbilder → echte XLSX → nach `processed/` verschieben.
**Quelle:** promptguy.io · petrvojacek.cz · claudefa.st · aiblewmymind · `extern` (Verhalten teils offiziell bestätigt).

---

## ✅ Checkliste: ein guter geplanter Task

1. **System gewählt** per Failure-Cost + 5-Fragen (§A1, §A2)?
2. **Prompt selbst-enthalten** (Ziel, Schritte, Pfade, Connectors namentlich, Format, Präferenzen) (§B1, §B2)?
3. **Zeit-Guardrails** gegen Catch-up + **Skip-Regeln** + **Ausgabeformat** (§B3, §B4)?
4. **Kadenz** passend, Min-Intervall beachtet, **Jitter** bei exaktem Timing (§C1–§C3)?
5. **Model**, **Permission-Mode**, **Worktree** bewusst gesetzt (§D1–§D3)?
6. **Sicherheit:** read-only zuerst, keine sensiblen Daten/Connectors, dedizierter Ordner + minimaler Mount-Modus, destruktive Guardrails, kein „Act without asking", Tier beachtet (§E1–§E6)?
7. **Zuverlässigkeit:** „Keep awake" / Cloud-Routine für Garantie (§F1, §F2)?
8. **„Run now"** getestet + Transkript geprüft (Status ≠ Erfolg) (§G1, §G2)?
9. **Least-Privilege** + ungenutzte Tasks pausiert (§G6)?
10. **Quota:** leichte Tasks gebündelt, Frequenz maßvoll (§H1)?

---

## 🔗 Bezug zur Bug-Almanach-Gegenseite

Best-Practices (diese Datei) ↔ Bug-Almanach [`bugs/claude-tooling/cowork-scheduled-tasks.md`](../../bugs/claude-tooling/cowork-scheduled-tasks.md). Links die *Regel, die es richtig macht*, rechts die *Falle, die sie verhindert*.

| Best-Practice-Abschnitt (diese Datei) | Verhindert Bug/Falle (Almanach-Abschnitt) |
|---------------------------------------|-------------------------------------------|
| §A System-Wahl (Failure-Cost, Routing) | §0 System-Wahl · §1 Wach/Fokus/Client-Bindung |
| §B3 Zeit-Guardrails · §B1 selbst-enthalten | §2.1 Catch-up zur falschen Uhrzeit · §9.1 frische Session |
| §C2 Mindestintervall · §C3 Jitter | §3.1 High-Freq-Freeze · §5 Cron/Zeit/Frequenz |
| §D2 Permissions vorab · §D3 Worktree | §7 Permission-Vererbung · §9.2 uncommitteter Stand |
| §B2 Connectors namentlich | §8.1 MCP-Warm-up |
| §E Sicherheit (read-only, Scope, Tiering) | §9.4 Prompt-Injection bei unbeaufsichtigten Tasks |
| §F Zuverlässigkeit (Keep awake, Cloud) | §1.1 wach+offen · §1.3 gebündeltes Nachfeuern |
| §G2 Transkript lesen | §9.3 grüner Status ≠ Erfolg |
| §H1 bündeln/maßvoll | §10 Quota/Limits |
