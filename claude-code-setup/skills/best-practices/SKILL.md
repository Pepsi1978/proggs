---
name: best-practices
description: >
  Recherchiert und pflegt die Best Practices fuer die eigenen Claude-Code-Werkzeuge
  (den "Harness"): Hooks, Skills, Agents, Plugins, MCP-Server, Slash-Commands, Settings,
  Kontext-Management, Token-Effizienz und Arbeitsweise. Gleicht den aktuellen Stand gegen
  die offiziellen Claude-Code-Changelogs und Anthropic-Docs ab, speichert das Wissen
  chronologisch im best-practices/-Ordner und liefert eine strukturierte Auswertung mit
  direkten Verbesserungsvorschlaegen. Nutze diesen Skill IMMER wenn der Benutzer sagt:
  "Best-Practices", "starte den Best-Practices-Skill", "Best Practices recherchieren",
  "recherchiere Best Practices fuer meine Werkzeuge", "was ist neu in Claude Code",
  "sind meine Hooks/Skills/Agents/MCP noch aktuell", "aktualisiere mein Harness-Wissen",
  "wie nutzt man Hooks/Skills/Agents/Plugins/MCP heute am besten", "Harness-Recherche".
  NICHT triggern fuer Best Practices von PROJEKT-Code (Kotlin, Compose, Android, C#,
  TypeScript, Rust) — dafuer ist dieser Skill nicht da.
invocation: user
---

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

## Abgrenzung — wann dieser Skill NICHT greift

- **Projekt-Code-Best-Practices** (Kotlin, Compose, Android, C#, TypeScript, Rust): nicht
  dieser Skill. Hier geht es ausschliesslich um die Claude-Code-Werkzeuge.
- **Tiefe Meta-Analyse** ueber den ganzen Ordner, Mustererkennung, Zukunftsprojektion: Teil 2.
- **Werkzeuge tatsaechlich aendern/reparieren**: Teil 2. Dieser Skill schlaegt nur vor und
  fasst nie ein Werkzeug an.

## Erster Lauf: Wissensbasis anlegen (falls noch nicht vorhanden)

Pruefe, ob `~/proggs/best-practices/` existiert. Falls nicht, lege die Struktur an:

```
best-practices/
├── README.md              ← Inhaltsverzeichnis, verlinkt jede Kategorie
├── _state.json            ← {"last_version": null, "last_checked": null}
├── _changelog-archiv.md   ← VOLLSTAENDIGER offizieller Changelog, verbatim (siehe Abschnitt unten)
├── 01-hooks/best-practices.md
├── 02-skills/best-practices.md
├── 03-agents/best-practices.md
├── 04-plugins/best-practices.md
├── 05-mcp/best-practices.md
├── 06-commands/best-practices.md
├── 07-settings/best-practices.md
├── 08-kontext/best-practices.md
├── 09-token-effizienz/best-practices.md
├── 10-arbeitsweise/best-practices.md
├── 11-researcher/best-practices.md
└── 12-neues/best-practices.md   ← "Neues" bleibt immer die letzte Kategorie
```

Der Ordner liegt bewusst **im Repo**, damit er nach macOS mit-synct. Jede Kategorie-Datei
startet mit einer kurzen Ueberschrift; Eintraege kommen erst beim Recherchieren dazu.

## Ablauf eines Laufs

1. **Stand lesen:** `_state.json` + `README.md` lesen → welche Version/welches Datum war zuletzt?
2. **Changelog verbatim holen + Delta bestimmen:** Den KOMPLETTEN offiziellen Changelog
   wortwoertlich herunterladen (siehe Abschnitt "Changelog-Archiv — vollstaendig & verbatim")
   und mit `last_version` vergleichen, um die neuen Versionen zu erkennen.
   (Bei explizitem Wunsch nach Volllauf den Delta-Schritt ueberspringen und alle Kategorien neu aufrollen.)
3. **Nichts Relevantes neu?** → "Nichts Neues seit Version X (Stand: Datum)" melden, fertig.
4. **Delta vorhanden?** → parallele Researcher starten (siehe Researcher-Regeln). Jeder Researcher
   bearbeitet einen klar begrenzten Bereich und recherchiert pro Kategorie:
   - **WAS** hat sich geaendert (offizielles Changelog)
   - **WIE** wendet man es am besten an (Anthropic-Docs / Engineering-Blog)
   - **Alternativen** von aussen (externe Quellen, klar als `extern` gelabelt, sekundaer)
   Was in keine definierte Kategorie passt → Kategorie 12 (Neues, immer die letzte Kategorie).
5. **Speichern:** Kategorie-`best-practices.md` aktualisieren (jeder Eintrag mit Quelle + Datum +
   `offiziell`/`extern`-Flag), `_changelog-archiv.md` inkrementell aktualisieren (siehe Abschnitt unten),
   `README.md` + `_state.json` aktualisieren. Neue Werkzeug-Klassen aus Kategorie 12 (Neues) bekommen
   einen eigenen Unterordner — eingefuegt VOR `Neues`, das dabei eine Nummer nach hinten rueckt
   (die Taxonomie waechst selbst, `Neues` bleibt immer die letzte Kategorie).
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

**Regel zur Reihenfolge:** "Neues" ist IMMER die letzte Kategorie (hoechste Nummer). Kommt eine
neue definierte Kategorie dazu, wird sie VOR "Neues" eingefuegt und "Neues" rueckt eine Nummer
nach hinten (Ordner entsprechend umbenennen). Beispiel: aus `12-neues` wird `13-neues`, die neue
Kategorie wird 12.

## Researcher-Regeln (KRITISCH — Absturz-Schutz)

- **Modell:** Claude Sonnet 4.6. **Effort:** X-High.
- **1 Researcher pro Kategorie, aber in BATCHES von 3–5 (KRITISCH, empirisch 2026-05-25):**
  Web-Researcher sind ANFRAGE-DICHT (2–3 Tool-Runden pro Turn × viele Turns → 100+ RPM bei 5 Stueck).
  Zu viele gleichzeitig sprengen das Anfrage-Raten-Limit (RPM) bzw. den Server-Burst-Schutz
  ("server is temporarily limiting requests · not your usage limit"). Live-Test: **12 gleichzeitig →
  11 abgestuerzt; 5 gleichzeitig → alle ok**; offiziell stabil sind ~3–5. Also Researcher in Wellen
  von 3–5 starten, NICHT alle auf einmal. (Anders als anfrage-SPARSE Agenten wie Uebersetzer, die
  ueberwiegend lokal arbeiten — die vertragen 15–20 gleichzeitig, weil sie kaum Anfragen/Minute erzeugen.)
- **Retry mit Backoff bei 429 (PFLICHT):** Stuerzt ein Researcher mit Rate-Limit ab, sofort dem Benutzer
  melden und mit exponential backoff neu starten (`retry-after`-Header beachten) — nie still aufgeben.
- **Scope eng halten:** max ~8 Websuchen / ~5 Fetches, ~8–10 Min pro Researcher. Begrenzt Anfrage-Rate
  UND Kontext. (Beobachtet: ~140–165k Token je Kategorie ist normal und unkritisch — der limitierende
  Faktor ist die ANFRAGE-RATE, nicht die Token.)
- **Checkpoint / Continuation:** Der Researcher schreibt seinen Fortschritt **inkrementell** in
  die Kategorie-Datei und endet mit einem klaren Checkpoint-Marker (was ist fertig, wo weitermachen).
  Ist er nicht fertig, wird ein **Continuation-Researcher am Checkpoint** gestartet. So geht nie
  Fortschritt verloren und nichts stuerzt ab.
- Zusaetzlich gilt die allgemeine Researcher-Regel: max 50 Ergebnisse / 15 Web-Fetches / 10 Min je Lauf.
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
- Dieser Skill liest/schreibt nur den `best-practices/`-Ordner und schlaegt vor. Er aendert
  niemals ein Hook, Skill, Agent, MCP oder Setting.

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
