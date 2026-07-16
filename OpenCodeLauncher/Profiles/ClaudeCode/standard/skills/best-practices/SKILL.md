---
name: best-practices
description: >
  Recherchiert und pflegt Best Practices in zwei Bereichen: (1) die eigenen
  Claude-Code-Werkzeuge (den "Harness"): Hooks, Skills, Agents, Plugins, MCP-Server,
  Slash-Commands, Settings, Kontext-Management, Token-Effizienz, Arbeitsweise; und (2)
  die Software/Sprachen, die in den Projekten benutzt werden (Kotlin, Swift, Gradle,
  .NET/WPF, TypeScript, Rust …) — das ist die "zweite Seite der Medaille" zum
  Bug-Almanach. Gleicht den aktuellen Stand gegen die jeweils passenden offiziellen
  Changelogs/Docs ab (Claude-Code-Changelog fuer den Harness, der eigene Software-
  Changelog fuer Projekt-Code), speichert das Wissen im best-practices/-Ordner (und
  koppelt dabei gefundene Bugs in den Bug-Almanach bugs/ zurueck) und liefert eine
  strukturierte Auswertung mit direkten Verbesserungsvorschlaegen. Nutze
  diesen Skill IMMER wenn der Benutzer sagt: "Best-Practices", "starte den
  Best-Practices-Skill", "Best Practices recherchieren", "recherchiere Best Practices
  fuer [meine Werkzeuge / Kotlin / Swift / Gradle / …]", "was ist neu in Claude Code /
  in Kotlin / in Swift", "sind meine Hooks/Skills/MCP noch aktuell", "aktualisiere mein
  Harness-Wissen", "Best Practices fuer die aktuelle [Software]-Version", "Harness-
  Recherche". Standardlauf deckt ALLE Kategorien ab; man kann ihn aber auf EINEN Bereich
  fokussieren ("Best-Practices nur fuer Kotlin", "nur fuer Hooks", "nur fuer Chrome-
  Erweiterungen"). Aendert nie ein Werkzeug — recherchiert, speichert und schlaegt vor.
invocation: user
---

<!-- delegation-research-skill -->
> **Web-Recherche laeuft ueber den zentralen `research`-Skill (Delegation, seit 2026-06-21).**
> Nach Frage 1 (Policy `research-strategy.md`: Empfehlung + A/B/C/D) die Recherche NICHT selbst
> orchestrieren — den `research`-Skill laden und ihm diesen Research-Auftrag uebergeben (verlustfreie
> Bruecke; ALLE Felder ausfuellen, nichts erzaehlen):
> - **zweck:** best_practice · **rueckgabe_schema:** `best_practice` · **zerlegungs_modus:** `feste_liste`
> - **unterthemen[]:** pro Kategorie: WAS hat sich geaendert (offizielles Changelog) + Best-Practice-Aspekte (je 2-3 Saetze praezise — werden 1:1 an die Researcher gereicht)
> - **version_anker:** PFLICHT — LIVE-Version + last_version-Delta (Changelog-Anker)
> - **engine:** A→C · **anzahl/wellen/cap:** 7, Continuous-Spawning, KEIN Eintrags-Cap
> - **persistenz_ziel:** `best-practices/<kategorie>/<bereich>.md (Kurzcheck+Volltext) + gefundene Bugs zurueck in bugs/` · **dup_quelle:** bestehender Best-Practices-Stand
> - **nacharbeit_aufrufer:** gefundene Fallen/Bugs in den Bug-Almanach zurueckkoppeln
> Der research-Skill uebernimmt sichtbare beschriftete Researcher + Continuous-Spawning + Zwischenfazit
> pro Researcher + ruhige Auswertung und gibt das Ergebnis im `best_practice`-Schema zurueck; damit
> hier weiterarbeiten. (Die A/B/C-Engine-Details unten bleiben als Referenz, werden aber vom research-Skill ausgefuehrt.)


# Best-Practices — Harness-Wissen aktuell halten

## Zweck

Mein Wissen ueber meine eigenen Werkzeuge ist auf den Trainingsstand eingefroren, aber
Claude Code entwickelt sich staendig weiter. Dieser Skill schliesst die Luecke: Er
recherchiert, was sich an den Werkzeugen geaendert hat **und** wie man sie heute am besten
einsetzt, und speichert das dauerhaft. So wird reaktive Recherche zu proaktivem Wissen,
das beim naechsten Mal schon bereitliegt (Compound Intelligence Effect, Direktive #1).

Dieser Skill ist **Teil 1** (Wissen sammeln + strukturieren + auswerten). Das spaetere
Plugin "Harness-Intelligenz" (Teil 2) denkt ueber das gesammelte Wissen nach und verbessert
Werkzeuge — das ist NICHT Aufgabe dieses Skills.

## Zwei Bereiche, ein Ordner

Der Ordner ist **flach 1:1 wie `bugs/`** aufgebaut (seit 2026-06-16): `best-practices/<kategorie>/<bereich>.md`,
gleiche Kategorien und Dateinamen wie `bugs/<kategorie>/<bereich>.md`. Der Skill pflegt zwei Arten von
Best-Practices im selben Schema:

1. **Harness** (Kategorie `claude-tooling/`): die Claude-Code-Werkzeuge. Themen-Dateien
   `claude-tooling/hooks.md`, `claude-tooling/skills.md`, `claude-tooling/agents.md`, `…/plugins.md`,
   `…/mcp.md`, `…/commands.md`, `…/settings.md`, `…/kontext.md`, `…/token-effizienz.md`,
   `…/arbeitsweise.md`, `…/researcher.md`, `…/neues.md`. Quelle ist der offizielle **Claude-Code**-Changelog
   (via `update-changelog.ps1`-Script). (Daneben liegen in `claude-tooling/` die Bug-gepaarten Digests wie
   `claude-hooks.md`, `mcp-server.md`, `claude-config.md` — Gegenstücke zu `bugs/claude-tooling/`.)
2. **Projekt-Code** (Kategorien `android/`, `android-build/`, `desktop/`, `web/`, `apis/`, `peripherie/`,
   `assets/`, `agents/`): `best-practices/<kategorie>/<software>.md` (z. B. `android/kotlin.md`,
   `desktop/swift-appkit.md`). Quelle ist der **eigene Changelog** der jeweiligen Software (KEIN
   Claude-Script) — die installierte Software-Version wird live ermittelt und ist der
   Versions-Anker. Das ist die zweite Seite der Medaille zum Bug-Almanach (`~/proggs/bugs/`):
   dort steht *was schiefgeht*, hier *wie man es von vornherein richtig macht*.

## Abgrenzung — wann dieser Skill NICHT greift

- **Tiefe Meta-Analyse** ueber den ganzen Ordner, Mustererkennung, Zukunftsprojektion: Teil 2.
- **Werkzeuge tatsaechlich aendern/reparieren**: Teil 2. Dieser Skill schlaegt nur vor und
  fasst nie ein Werkzeug an.

## Fokus auf einen Bereich (statt Volllauf)

Standardmaessig deckt ein Lauf alle Kategorien ab. Man kann den Skill aber auf EINEN
Bereich einschraenken — z.B. "Best-Practices nur fuer Kotlin", "nur fuer Hooks", "nur fuer
Chrome-Erweiterungen". Dann nur die genannte Kategorie/Software recherchieren und schreiben.
Das nutzt u.a. der `bug-almanach-recherche`-Skill, wenn er gezielt eine Software aufrollen
laesst.

## Erster Lauf: Wissensbasis anlegen (falls noch nicht vorhanden)

Pruefe, ob `~/proggs/best-practices/` existiert. Falls nicht, lege die Struktur an
(flach 1:1 wie `bugs/`, gleiche Kategorien):

```
best-practices/
├── README.md              ← Kategorie-Index (siehe bestehende README.md)
├── SYSTEM.md              ← Systembeschreibung + Aenderungs-Historie (§8)
├── _state.json            ← {"last_version": null, "last_checked": null}
├── _changelog-archiv.md   ← VERBATIM Claude-Code-Changelog (Recherche-Quelle, vom Script geholt)
├── claude-tooling/        ← Harness: hooks.md, skills.md, agents.md, plugins.md, mcp.md,
│                            commands.md, settings.md, kontext.md, token-effizienz.md,
│                            arbeitsweise.md, researcher.md, neues.md  (+ Bug-gepaarte Digests)
├── android/ · android-build/ · desktop/ · web/ · apis/ · peripherie/ · assets/ · agents/
│                            ← Projekt-Code: <software>.md je Kategorie (1:1 wie bugs/)
```

Der Ordner liegt bewusst **im Repo**, damit er nach macOS mit-synct. Jede Themen-/Software-Datei
startet mit einer kurzen Ueberschrift; Eintraege kommen erst beim Recherchieren dazu.

## Ablauf eines Laufs

1. **Stand lesen:** `_state.json` + `README.md` lesen → welche Version/welches Datum war zuletzt?
2. **Changelog verbatim holen + Delta bestimmen:** Den KOMPLETTEN offiziellen Changelog
   wortwoertlich herunterladen (siehe Abschnitt "Changelog-Archiv — vollstaendig & verbatim")
   und mit `last_version` vergleichen, um die neuen Versionen zu erkennen.
   (Bei explizitem Wunsch nach Volllauf den Delta-Schritt ueberspringen und alle Kategorien neu aufrollen.)
3. **Nichts Relevantes neu?** → "Nichts Neues seit Version X (Stand: Datum)" melden, fertig.
4. **Delta vorhanden?** → ZUERST den Recherche-Weg per `AskUserQuestion` waehlen lassen
   (Regel `research-strategy.md`, Frage 1 A/B/C/D) — NIE automatisch losrecherchieren:
   - **A (Standard): Firecrawl + MiniMax M3 (max Thinking)** → `python3 ~/proggs/mm-research.py "<kategorie-frage>" [n]`
     pro Kategorie. **Firecrawl Free = max 2 GLEICHZEITIG** (2 starten → auf Ergebnis warten → naechste 2;
     NICHT 7). Nach Abschluss Frage 2 (zusaetzliche Eskalation?). MiniMax liefert die quellentreue
     Auswertung; der Hauptagent arbeitet sie in die Kategorie-Dateien ein (Stufe 3 / `research-persistence`).
   - **B (Eskalation): MiniMax + parallel (max Thinking)** → `python3 ~/proggs/or-research.py "<frage>"`
     (kein 2-Limit, hoehere Parallelitaet, pay-per-use, kein Monatslimit).
   - **C (Sonnet-5-Schwarm):** NUR auf explizite Wahl → die **Researcher-Regeln** unten (7 parallel, Continuous-Spawning).
   - Der `research-approval`-Hook blockt mm/or-research, bis Frank A/B gewaehlt UND die Freigabe gesetzt ist
     (`touch "$TEMP/research-approved.flag"`).
   Jeder Researcher/Lauf bearbeitet einen klar begrenzten Bereich und recherchiert pro Kategorie:
   - **WAS** hat sich geaendert (offizielles Changelog)
   - **WIE** wendet man es am besten an (Anthropic-Docs / Engineering-Blog)
   - **Alternativen** von aussen (externe Quellen, klar als `extern` gelabelt, sekundaer)
   Was in keine definierte Kategorie passt → Kategorie 12 (Neues, immer die letzte Kategorie).
5. **Speichern:** Kategorie-`best-practices.md` aktualisieren (jeder Eintrag mit Quelle + Datum +
   `offiziell`/`extern`-Flag), `_changelog-archiv.md` inkrementell aktualisieren (siehe Abschnitt unten),
   `README.md` + `_state.json` aktualisieren. Ein neues Harness-Thema ist einfach eine neue Datei
   `claude-tooling/<thema>.md` (keine Nummerierung/Reihenfolge mehr); thematisch noch Unsortiertes
   sammelt `claude-tooling/neues.md`. Bei **Projekt-Code**-
   Laeufen zusaetzlich die **Bug-Almanach-Rueckkopplung** ausfuehren (Abschnitt „Kopplung zum
   Bug-Almanach"): gefundene Bugs nach `bugs/<bereich>.md` zurueckschreiben + Bezugs-Tabellen synchron halten.
   **Self-Test (seit 2026-06-15):** danach `python ~/proggs/bugs/health.py` laufen lassen — die coupling-Pruefung
   faengt fehlende/asymmetrische Bezugs-Tabellen sofort (alle fuenf Checks muessen gruen sein). Wird dabei ein
   SOFTWARE-gebundener Almanach neu angelegt/zurueckgekoppelt, traegt er das strukturierte
   `> **Anker:** <label>=<version>`-Feld (SYSTEM.md §7) + ggf. einen `check-version-anchor.py`-Eintrag
   (Details: Skill `bug-almanach-recherche`, Schritt 6, Punkte 3-5).
6. **Auswertung ausgeben** (siehe Format unten).

## Changelog-Archiv — vollstaendig & verbatim (KRITISCH)

Das `_changelog-archiv.md` ist die **Rohdaten-Grundlage** fuer alle spaeteren Mustererkennungen
(wie entwickelt sich Claude Code?). Eine Zusammenfassung ist hier wertlos — ein Researcher kann
nur mit dem Volltext arbeiten. Deshalb gilt strikt:

- **Verbatim, ungekuerzt:** Die Datei enthaelt den KOMPLETTEN offiziellen Changelog JEDER Version,
  alle Bullet-Points wortwoertlich. KEINE Zusammenfassungen, KEINE Wochen-Stichpunkte, KEINE Kuerzung.
- **Kanonische Quelle:** `https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md`
  — enthaelt alle Versionen (neueste oben), zurueck bis in die 0.x-Reihe. Mindest-Abdeckung: ab 2.0.
- **Download-Methode:** Direkter Datei-Download — NIEMALS WebFetch oder ein Researcher (die fassen
  zusammen und zerstoeren die Vollstaendigkeit; genau das war der urspruengliche Fehler 2026-05-25).
- **Datum je Version:** Der CHANGELOG.md hat KEINE Datumsangaben. Das Release-Datum kommt aus den
  **npm-Publish-Zeitstempeln** (`@anthropic-ai/claude-code`, Feld `time`). Format: `## X.Y.Z — YYYY-MM-DD`,
  fehlt ein npm-Eintrag (sehr alte 0.2.x): `— Datum unbekannt`. So ist die Entwicklung auch ZEITLICH
  erfassbar, nicht nur nach Versionsnummer.

### Ausfuehrung: das Script (nicht von Hand nachbauen!)

Die komplette Mechanik (Download + npm-Datum + Erstlauf/inkrementell + Verifikation) steckt in einem
deterministischen Script. NICHT als Prosa rekonstruieren — aufrufen:

| Plattform | Befehl |
|-----------|--------|
| Windows | `pwsh ${CLAUDE_SKILL_DIR}/scripts/update-changelog.ps1` |
| macOS/Linux | `bash ${CLAUDE_SKILL_DIR}/scripts/update-changelog.sh` |

- **Ohne Flag = inkrementell:** nur Versionen neuer als `_state.json.last_version` werden oben
  angehaengt; alte Eintraege + eigene Hand-Notizen bleiben unangetastet.
- **Mit `-FirstRun` (ps1) / `--first-run` (sh):** kompletter Neu-Aufbau (Erstlauf oder Reparatur).
- Das Script pflegt `_state.json` selbst und gibt am Ende eine Verifikation aus (Versions-Header-Zahl,
  Duplikat-Check). Getestet 2026-05-25: FirstRun = 296 Versionen, inkrementell haengt exakt die neuen an.

## Taxonomie (12 Kategorien, selbst-erweiternd)

| # | Kategorie | Inhalt |
|---|-----------|--------|
| 1 | Hooks | Events, Schema, Exit-Codes, Neuerungen |
| 2 | Skills | SKILL.md-Format, Trigger, References, skill-creator |
| 3 | Agents | Agent-Tool, Definitionen, Modellwahl, Parallelisierung |
| 4 | Plugins | plugin.json, Struktur, Marketplace, Komponenten |
| 5 | MCP-Server | .mcp.json, Konfiguration, lohnende Server, Resources/Tools |
| 6 | Slash-Commands | Command-Format, Argumente |
| 7 | Settings & Konfig | settings.json, Permissions, Env-Vars, Statusline, Output-Styles |
| 8 | Kontext-Management | Compaction, Microcompact, Autocompact-Schwellen, Memory |
| 9 | Token- & Kosten-Effizienz | Caching, Modellwahl, Effort-Levels, Parallelisierungs-Oekonomie |
| 10 | Arbeitsweise / Verhalten | Wie man Fragen angeht, Multi-Task, Planung, TDD |
| 11 | Researcher & Internet-Recherche | Robuster Einsatz von Researcher-Subagenten: Parallelitaet, Token-/Fetch-Limits, Absturz-Vermeidung, Checkpointing, gute Prompts fuer Web-Recherche. (Wir nutzen Internet-Researcher sehr oft — und sie stuerzen oft ab, daher eigener Fokus neben Kategorie 3 Agents.) |
| 12 | Neues / Horizont-Scan | Auffangzone fuer alles, was in keine definierte Kategorie passt. **Bleibt IMMER die letzte Kategorie** (hoechste Nummer). |

Kategorie 12 (Neues) ist wichtig: Nicht nur das suchen, was schon bekannt ist — gerade die ganz
neuen Faehigkeiten bringen den groessten Sprung. Alles Unbekannte landet hier und wird,
wenn es sich als wichtig erweist, zu einer eigenen Kategorie.

**Speicherung (seit 2026-06-16):** Die Harness-Themen liegen flach als `claude-tooling/<thema>.md`
(`hooks.md`, `skills.md`, …, `neues.md`) — keine nummerierten Ordner, keine Reihenfolge-Pflege mehr.
Die Tabelle oben ist nur die inhaltliche Orientierung; die `#`-Spalte sind Listennummern, keine
Ordnernamen. Ein neues Harness-Thema ist einfach eine neue Datei (z. B. `claude-tooling/output-styles.md`).

### Projekt-Code-Kategorien (`best-practices/<kategorie>/<software>.md`)

Neben `claude-tooling/` (Harness) gibt es die Projekt-Code-Kategorien `android/`, `android-build/`,
`desktop/`, `web/`, `apis/`, `peripherie/`, `assets/`, `agents/` — **dieselben Kategorien wie der
Bug-Almanach `bugs/<kategorie>/`**, mit je einer selbst-identifizierenden Datei direkt im
Kategorie-Ordner (`best-practices/<kategorie>/<software>.md`, z. B. `android/kotlin.md`,
`desktop/swift-appkit.md`; kein `best-practices-`-Praefix, keine `projekt-code/`-Ebene mehr),
die bei Bedarf entsteht (Kategorie-Index: `best-practices/README.md`).

**Wichtiger Mechanik-Unterschied — beim Recherchieren beachten:**
- **Harness-Kategorien (01–12):** Changelog-Quelle ist der **Claude-Code**-Changelog,
  geholt mit dem `update-changelog.ps1`/`.sh`-Script (siehe naechster Abschnitt). Versions-
  Anker = installierte Claude-Code-Version.
- **Projekt-Code (`best-practices/<kategorie>/<software>.md`):** Changelog-Quelle ist der **eigene**
  Changelog/die Release-Notes der jeweiligen Software (Kotlin-Releases bei JetBrains,
  Swift-Releases bei Apple, Gradle-Releases …). Das Claude-Changelog-Script wird hier NICHT
  benutzt. Versions-Anker = die LIVE ermittelte installierte Version dieser Software
  (`kotlinc -version`, `swift --version`, `./gradlew --version`, `dotnet --version` …).
  Quellen-Rangordnung gleich: offizielle Hersteller-Quelle = Grundwahrheit, Community = `extern`.

Bei einem fokussierten Lauf ("nur fuer Kotlin") wird genau eine Software/Kategorie
recherchiert und geschrieben — kein Volllauf.

## Kopplung zum Bug-Almanach (beide Richtungen — PFLICHT bei Projekt-Code)

Best-Practices und der Bug-Almanach (`~/proggs/bugs/<bereich>.md`) sind zwei Seiten derselben Medaille,
und die Kopplung laeuft in BEIDE Richtungen — symmetrisch zum `bug-almanach-recherche`-Skill (dessen
Schritt 4 die Praevention in best-practices schreibt). Wenn DIESER Skill laeuft und die Recherche einen
konkreten BUG / eine Falle / einen Stolperstein zutage foerdert (nicht nur eine positive Empfehlung),
gehoert dieser Bug in den Almanach ZURUECK. Gilt v.a. fuer Projekt-Code-Laeufe (Kotlin, Swift, Gradle …);
ein Harness-Bug-Fund passt analog in den jeweiligen Almanach bzw. `bug-cases.jsonl`.

### A — Bug-Fund in den Almanach zurueckschreiben
Pro gefundenem echten Bug (Symptom + Ursache + funktionserhaltende Loesung + betroffene Versionen):
- **`bugs/<kategorie>/<bereich>.md` existiert** → als Eintrag ergaenzen (Format aus `~/proggs/bugs/SYSTEM.md`:
  `## N. Titel [⭐ HAEUFIG] / Symptom / Ursache / Versionen / FIX / Quelle`), gegen bestehende Eintraege
  DEDUPLIZIEREN (gleicher Bug → nicht doppelt, hoechstens praezisieren), Stand-Header aktualisieren.
  Im FIX-Feld auf den passenden Best-Practice-Abschnitt verweisen.
- **`bugs/<bereich>.md` existiert NICHT** → NICHT im Vorbeigehen einen halben Almanach anlegen (ihm
  fehlt die Fix-Status-Pruefung per `gh`, die nur `bug-almanach-recherche` macht). Stattdessen dem
  Benutzer melden ("Best-Practices-Recherche hat Bugs gefunden, aber es gibt keinen Almanach fuer
  <bereich>") und den Skill `bug-almanach-recherche` vorschlagen (erst Franks OK). Die gefundenen Bugs
  kompakt mitliefern, damit nichts verloren geht.

### B — Bezugs-Tabellen synchron halten
Existieren BEIDE Dateien (`best-practices/<kategorie>/<software>.md` UND
`bugs/<kategorie>/<bereich>.md`), in JEDER eine wechselseitige Abschnitts-Bezugs-Tabelle „Best-Practice-Abschnitt ↔
Bug-Abschnitt" aktuell halten. Fehlt eine, anlegen; kamen Abschnitte dazu, ergaenzen. So bleibt jede
Best-Practice mit ihrer konkreten Bug-Loesung verlinkt (und umgekehrt).

Diese Rueckrichtung ist PFLICHT — frueher schrieb nur `bug-almanach-recherche` Richtung best-practices.
Jetzt fuettern sich beide Speicher gegenseitig (Compound Intelligence, Direktive #1).

## Researcher-Regeln (KRITISCH — Absturz-Schutz)

> **Gilt fuer Option C (Sonnet-5-Schwarm)** aus Schritt 4. Standard ist A/B (Token-sparende
> `mm-research.py`/`or-research.py`-Skripte, Regel `research-strategy.md`); die folgenden Regeln
> greifen, wenn Frank den Sonnet-5-Schwarm ausdruecklich waehlt — oder fuer den Changelog-Verbatim-Download
> (der NIE ueber einen Researcher laeuft, siehe Changelog-Archiv-Abschnitt).

- **Modell:** Sonnet 5 (`model:"sonnet"`, seit 2026-07-01 statt Opus 4.8 — natives 1M-Kontext, siehe
  `research-strategy.md` §4a). **Effort:** High (Standard-Session-Effort — NICHT X-High, Frank-Korrektur 2026-07-01).
- **Direkt 7 Researcher GLEICHZEITIG starten, dann CONTINUOUS-SPAWNING (Frank 2026-06-02 + 2026-06-03):**
  Bei genug Themen IMMER mit **7 auf einmal** beginnen — NICHT erst 4 und danach nochmal 3 (Zeitverschwendung).
  7 gleichzeitig funktionieren einwandfrei. Gibt es MEHR als 7 Themen: sobald EINER fertig wird (nur noch 6
  laufen), SOFORT den naechsten fuers naechste Thema hinterher starten, damit wieder 7 laufen — bis ALLE
  Themen abgedeckt sind. **NIEMALS** warten, bis die ersten 7 fertig sind, und dann eine zweite Welle (z.B. 3)
  nachschieben (waere in der Spitze 10 → RPM-Risiko, langsamer). So bleibt die Parallelitaet konstant bei 7
  und der RPM-Strom gleichmaessig (kein Burst). Empirisch: **5 sicher, 7 laeuft einwandfrei, 12 → 11 abgestuerzt**.
  Also Obergrenze ~7 gleichzeitig.
- **Warum die Obergrenze NICHT vom Kontextfenster kommt (wichtig):** Web-Researcher sind
  ANFRAGE-DICHT (2–3 Tool-Runden/Turn → 100+ RPM bei 5 Stueck). Zu viele gleichzeitig sprengen das
  Anfrage-Raten-Limit (RPM) bzw. den Server-Burst-Schutz ("server is temporarily limiting requests ·
  not your usage limit"). Das ist UNABHAENGIG vom 1M-Kontextfenster — das 1M-Kontext (Opus 4.8[1m]
  bzw. Sonnet 5, das nativ 1M hat) loest den *Kontext*-Crash (→ kein Findings-Cap mehr, siehe unten),
  aber NICHT den *RPM*-Crash. Darum bleibt ~7 die Obergrenze + 429-Backoff. (Anfrage-SPARSE Agenten
  wie Uebersetzer vertragen 15–20, weil sie ueberwiegend lokal arbeiten.)
- **KEIN Findings-/Ergebnis-Cap (Frank-Korrektur 2026-06-02):** ALLE gefundenen Best-Practices/Bugs
  dokumentieren — kein kuenstliches "max 50". Mit 1M-Kontext ist die Menge kein Absturzrisiko mehr;
  ein hartes Cap waere *lossy* (siehe `lossless-context-principle.md`). Bei sehr vielen Funden
  verlustfrei in die Kategorie-Datei schreiben (File-as-Memory) + kompakte Summary, nie kappen.
- **Retry mit Backoff bei 429 (PFLICHT, bleibt):** Stuerzt ein Researcher mit Rate-Limit ab, sofort dem
  Benutzer melden und mit exponential backoff neu starten (`retry-after`-Header beachten) — nie still aufgeben.
- **Scope (gegen RPM/Haengen, NICHT gegen Vollstaendigkeit):** ~15 Websuchen/Fetches, ~10 Min pro
  Researcher. Begrenzt die ANFRAGE-Rate, nicht die Findings-Zahl. (Beobachtet: ~140–200k Token je
  Kategorie ist normal und unkritisch — der limitierende Faktor ist die Anfrage-Rate, nicht die Token.)
- **Checkpoint / Continuation:** Der Researcher schreibt seinen Fortschritt **inkrementell** in
  die Kategorie-Datei und endet mit einem klaren Checkpoint-Marker (was ist fertig, wo weitermachen).
  Ist er nicht fertig, wird ein **Continuation-Researcher am Checkpoint** gestartet. So geht nie
  Fortschritt verloren und nichts stuerzt ab.
- **Einheitliches Header-Format erzwingen + aufraeumen:** Jede Kategorie-Datei MUSS mit
  `# [Kategorie] — Best Practices (Stand JJJJ-MM-TT, Claude Code X.Y.Z)` beginnen — im Prompt verlangen
  UND nach dem Lauf pruefen (beim Live-Test wich 03-agents vom Format ab). Sentinel-/Writeback-Artefakte
  (`_writeback.json`) gehoeren NICHT in `best-practices/` — vor dem Commit entfernen (per .gitignore abgesichert).

## Quellen-Rangordnung

| Prioritaet | Quelle | Rolle |
|-----------|--------|-------|
| 1 | Offizielle Anthropic-Quellen (Claude-Code-Changelog, code.claude.com/docs, Anthropic-Engineering-Blog) | **Grundwahrheit** fuer die Werkzeug-Fakten |
| 2 | Externe / Community (Blogs, Profis, GitHub-Diskussionen) | Nur als abwaegbare **Alternative**, klar als `extern` gelabelt — ueberstimmt NIE das Offizielle |

Jeder Eintrag traegt **Quelle + Datum**, damit Aktualitaet abwaegbar bleibt. Regel: Wird nichts
Neueres gefunden, gilt der letzte Eintrag weiter als aktuell.

## Verlaesslichkeits-Leitplanken

- Eine falsche Best-Practice ist schlimmer als gar keine — deshalb offiziell zuerst, alles mit Quelle + Datum.
- Niemals eine externe Behauptung als offiziell darstellen.
- Dieser Skill schreibt in den `best-practices/`-Ordner UND (Rueckkopplung) in den Bug-Almanach
  `bugs/<bereich>.md`, und schlaegt vor. Er aendert niemals ein Hook, Skill, Agent, MCP oder Setting
  (nur Wissensdateien — Code/Werkzeuge bleiben unangetastet).

## Auswertungs-Format (Schritt 6)

Am Ende JEDES Laufs eine **ausfuehrliche** Auswertung ausgeben — nicht nur Stichpunkte.
Drei Teile in dieser Reihenfolge:

### Teil A — Ausfuehrliche Auswertung je Kategorie
Fuer JEDE betroffene Kategorie ein eigener Abschnitt mit:
- **Neuerungen:** was hat sich geaendert (Versionsnummer + Quelle, soweit bekannt)
- **Best Practices:** wie nutzt man es heute am besten (das Wichtigste, nicht nur Aufzaehlung)
- **Betrifft eigene Werkzeuge:** beruehrt das ein vorhandenes Hook/Skill/Agent/Setting? (Hinweis)

### Teil B — Kurz-Header
```
## Best-Practices-Lauf — [Datum]
Geprueft: Version [alt] → [neu] | Kategorien: [Anzahl] | Quellen: [Anzahl]
```

### Teil C — UMSETZBARE VERBESSERUNGSVORSCHLAEGE (das Herzstueck — PFLICHT am Ende)
Eine nummerierte Liste konkreter, **sofort abnickbarer** Vorschlaege. AUSSCHLIESSLICH solche,
bei denen die Recherche einen **echten, belegten Vorteil** gezeigt hat — kein Fuellmaterial.
Pro Vorschlag GENAU dieses Format, damit der Benutzer einfach "ja, ja, ja" sagen kann:

```
N. [Konkrete Aktion in einem Satz]
   Vorteil (belegt): [was es bringt] — Quelle: [offizieller Fund aus diesem Lauf]
   Betrifft: [Datei/Werkzeug] | Aufwand: [klein/mittel/gross] | Risiko: [niedrig/mittel/hoch]
```

Regeln fuer Teil C:
- **Umgebungs-Gegencheck VOR dem Listen (PFLICHT):** Jeden Vorschlag erst gegen den IST-Zustand der
  Umgebung pruefen, nicht nur aus dem Changelog-Feature ableiten. Drei Fragen: (1) Existieren die
  betroffenen Skills/Hooks/Dateien ueberhaupt? (2) Nutzt der betroffene Skill/Hook das Feature
  wirklich (z.B. ruft er das Tool, das gesperrt werden soll, ueberhaupt auf; schreibt der Hook die
  Skills die er neu laden soll wirklich)? (3) Bringt es in GENAU dieser Umgebung echten Nutzen — oder
  waere es nur ein theoretischer Zusatz? Nur Vorschlaege die alle drei bestehen, werden gelistet.
  Grund: rein feature-getriebene Vorschlaege ohne Ist-Abgleich erweisen sich bei der Umsetzung oft
  als unpassend (Beispiel: disallowed-tools fuer einen Skill der das Tool nie aufruft; reloadSkills
  fuer einen SessionStart-Hook der gar keine Skills schreibt).
- NUR research-gestuetzte Vorschlaege mit nachweisbarem Vorteil. Lieber 3 starke als 10 schwache.
- Jeder Vorschlag muss mit einem einfachen "ja" umsetzbar sein (konkret, nicht "man koennte mal").
- Vorschlaege die ein eigenes Werkzeug aendern klar markieren (Teil-2-Gebiet, aber hier abnickbar gelistet).
- Wenn ein Fund einen FRUEHEREN Vorschlag widerlegt (z.B. Feature ist buggy), ehrlich sagen und
  den alten Vorschlag zurueckziehen.
- Danach auf die Auswahl des Benutzers warten und NUR die bestaetigten Vorschlaege umsetzen.

## Wiederverwendung

- Nutzt den vorhandenen `researcher`-Agenten fuer die parallele Web-Recherche.
- Grenzt sich ab von `claude-delta-scanner` (eigene Repo-Aenderungen fuer Codex),
  `direktiven-recherche` (die drei Direktiven) und `/self-improve` (Umgebungs-Audit).
