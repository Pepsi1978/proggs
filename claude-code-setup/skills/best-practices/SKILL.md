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
  TypeScript, Rust) — dafuer ist dieser Skill nicht da. Er betrifft ausschliesslich die
  Claude-Code-Werkzeuge selbst, also WIE man den Harness am besten benutzt.
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
└── 11-neues/best-practices.md
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
   Was in keine Kategorie 1–10 passt → Kategorie 11 (Neues).
5. **Speichern:** Kategorie-`best-practices.md` aktualisieren (jeder Eintrag mit Quelle + Datum +
   `offiziell`/`extern`-Flag), `_changelog-archiv.md` verbatim neu holen (siehe Abschnitt unten),
   `README.md` + `_state.json` aktualisieren. Neue Werkzeug-Klassen aus Kategorie 11 bekommen einen eigenen
   Unterordner (12-…, 13-…) — die Taxonomie waechst selbst.
6. **Auswertung ausgeben** (siehe Format unten).

## Changelog-Archiv — vollstaendig & verbatim (KRITISCH)

Das `_changelog-archiv.md` ist die **Rohdaten-Grundlage** fuer alle spaeteren Mustererkennungen
(wie entwickelt sich Claude Code?). Eine Zusammenfassung ist hier wertlos — ein Researcher kann
nur mit dem Volltext arbeiten. Deshalb gilt strikt:

- **Verbatim, ungekuerzt:** Die Datei enthaelt den KOMPLETTEN offiziellen Changelog JEDER Version,
  alle Bullet-Points wortwoertlich. KEINE Zusammenfassungen, KEINE Wochen-Stichpunkte, KEINE Kuerzung.
- **Kanonische Quelle:** `https://raw.githubusercontent.com/anthropics/claude-code/main/CHANGELOG.md`
  — enthaelt alle Versionen (neueste oben), zurueck bis in die 0.x-Reihe. Mindest-Abdeckung: ab 2.0.
- **Download-Methode:** IMMER direkter Datei-Download (`Invoke-WebRequest` / `curl`), der den Inhalt
  1:1 speichert. NIEMALS WebFetch oder ein Researcher — die fassen zusammen und zerstoeren die
  Vollstaendigkeit. Genau das war der urspruengliche Fehler (2026-05-25).
- **Aufbau:** Kurzer Kopf (Quelle, Download-Datum, Abdeckung), darunter der komplette Changelog
  chronologisch (neueste Version oben), jede Version `## X.Y.Z — YYYY-MM-DD` mit allen Original-Bullets.
- **Datum je Version (PFLICHT):** Der CHANGELOG.md enthaelt KEINE Datumsangaben. Das Release-Datum
  jeder Version kommt aus den **npm-Publish-Zeitstempeln**: `npm view @anthropic-ai/claude-code time --json`
  (Fallback: Registry-Fetch `https://registry.npmjs.org/@anthropic-ai%2Fclaude-code`, Feld `time`). Jeder
  Versions-Header wird zu `## X.Y.Z — YYYY-MM-DD` angereichert (Datum aus dem Roh-ISO-String, ohne
  Zeitzonen-Shift). Versionen ohne npm-Eintrag (sehr alte 0.2.x): `— Datum unbekannt`. So ist die
  Entwicklung auch ZEITLICH erfassbar, nicht nur nach Versionsnummer.
- **Bei jedem Lauf:** Datei komplett neu holen (der kanonische CHANGELOG.md enthaelt ohnehin die
  Gesamthistorie, neue Versionen sind dann oben automatisch dabei).
- **Pflicht-Verifikation:** Nach dem Download Anzahl `## `-Versions-Header UND Bullet-Zeilen gegen
  die Quelldatei pruefen — muessen identisch sein, sonst ging beim Schreiben etwas verloren.

## Taxonomie (11 Kategorien, selbst-erweiternd)

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
| 11 | Neues / Horizont-Scan | Auffangzone fuer alles, was in 1–10 nicht passt. Waechst zu eigenen Kategorien heran. |

Kategorie 11 ist wichtig: Nicht nur das suchen, was schon bekannt ist — gerade die ganz
neuen Faehigkeiten bringen den groessten Sprung. Alles Unbekannte landet hier und wird,
wenn es sich als wichtig erweist, zu einer eigenen Kategorie.

## Researcher-Regeln (KRITISCH — Absturz-Schutz)

- **Modell:** Claude Sonnet 4.6. **Effort:** X-High.
- **Max 130.000 Token pro Researcher.** Kein Researcher darf darueber hinaus laufen.
- **Scope klein schneiden — beim Volllauf 1 Kategorie pro Researcher:** Empirisch (Erstlauf
  2026-05-25) verbraucht ein Researcher mit 2–3 Kategorien ~150–160k Token — UEBER dem 130k-Ziel.
  Deshalb beim Volllauf genau EINE Kategorie pro Researcher. Nur bei kleinen Delta-Laeufen duerfen
  verwandte Kategorien zusammengefasst werden, wenn der Zuwachs gering ist.
- **Das 130k-Limit ist ein Ziel, kein Selbst-Stopp:** Ein Subagent misst seinen Token-Verbrauch
  nicht live und kann sich nicht selbst stoppen. Durchgesetzt wird das Limit ueber (a) kleinen
  Scope (1 Kategorie) und (b) die Continuation unten — nicht ueber Selbstmessung.
- **Checkpoint / Continuation:** Der Researcher schreibt seinen Fortschritt **inkrementell** in
  die Kategorie-Datei und endet mit einem klaren Checkpoint-Marker (was ist fertig, wo
  weitermachen). Ist er nicht fertig, wird ein **Continuation-Researcher am Checkpoint**
  gestartet. So geht nie Fortschritt verloren und nichts stuerzt ab.
- **Parallelitaet:** Max ~5 Researcher gleichzeitig (Sweet Spot, sichtbar im Hauptchat).
- Zusaetzlich gilt die allgemeine Researcher-Regel: max 50 Ergebnisse / 15 Web-Fetches / 10 Min je Lauf.

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

Am Ende eines Laufs eine gut strukturierte, uebersichtliche Auswertung ausgeben:

```
## Best-Practices-Lauf — [Datum]
Geprueft: Version [alt] → [neu]

### Was hat sich geaendert
- [Kategorie]: [kurze Beschreibung] (Quelle, Datum)
- …

### Direkte Verbesserungsvorschlaege
1. [Konkret, sofort umsetzbar]
2. …

### Eigene Werkzeuge — moeglicherweise veraltet (nur Hinweis)
- [Werkzeug X] koennte durch [Neuerung] ueberholt sein → tiefere Pruefung waere Teil 2
```

Die Verbesserungsvorschlaege zusaetzlich im bestehenden Intelligenz-Vorschlags-Format ausgeben.
Das "veraltete Werkzeuge"-Flagging ist nur ein Hinweis — Analyse und Umsetzung gehoeren in Teil 2.

## Wiederverwendung

- Nutzt den vorhandenen `researcher`-Agenten fuer die parallele Web-Recherche.
- Grenzt sich ab von `claude-delta-scanner` (eigene Repo-Aenderungen fuer Codex),
  `direktiven-recherche` (die drei Direktiven) und `/self-improve` (Umgebungs-Audit).
