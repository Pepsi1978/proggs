---
name: best-practices
description: "Recherchiert und pflegt Best Practices fuer Harness-Werkzeuge und Projekt-Software (Kotlin, Swift, Gradle), speichert sie im Arbeitsordner. Trigger: Best-Practices recherchieren, was ist neu in X."
---

# Best-Practices (Cowork-Fassung) — Harness- & Projekt-Wissen aktuell halten

Diese Cowork-Fassung recherchiert, **was sich an den eigenen Werkzeugen (Harness) und an der
benutzten Projekt-Software geaendert hat** und wie man sie heute am besten einsetzt, und speichert
das dauerhaft als wiederverwendbares Wissen (Compound Intelligence). Aendert nie ein Werkzeug —
recherchiert, speichert und schlaegt vor. Laeuft in der **Claude-Cowork-Desktop-App**.

> Abgrenzung: Tiefe Meta-Analyse / Werkzeuge tatsaechlich aendern = nicht dieser Skill (Teil 2). Geht
> es speziell um **Bugs einer Software** → `bug-almanach-recherche`. Allgemeine Themen-Recherche → `research`.

---

## 0. ZUERST LESEN — Ablage-Ort & Ordner anlegen (Cowork)

**Ergebnisse werden RELATIV im aktuell verbundenen Arbeitsordner gespeichert** (ueblicherweise der
gemountete `proggs`-Ordner) — NICHT in einen fest verdrahteten `~/proggs`-Pfad. Der `best-practices/`-Ordner
ist **flach 1:1 wie `bugs/`** aufgebaut: gleiche Kategorien und Dateinamen.

| Was | Relativer Pfad |
|-----|----------------|
| Harness (Claude-Code-Werkzeuge) | `best-practices/claude-tooling/<thema>.md` (`hooks.md`, `skills.md`, `agents.md`, `plugins.md`, `mcp.md`, `commands.md`, `settings.md`, `kontext.md`, `token-effizienz.md`, `arbeitsweise.md`, `researcher.md`, `neues.md`) |
| Projekt-Code | `best-practices/<kategorie>/<software>.md` (`android/kotlin.md`, `desktop/swift-appkit.md`, `android-build/gradle.md` …) — Kategorien: `android`, `android-build`, `desktop`, `web`, `apis`, `peripherie`, `assets`, `agents` |
| Bug-Almanach-Rueckkopplung | `bugs/<kategorie>/<bereich>.md` |
| Steuerdateien | `best-practices/README.md` (Index), `SYSTEM.md`, `_state.json`, `_changelog-archiv.md` |

**KEINE alte Taxonomie:** kein nummerierter Ordner (`01-hooks…12-neues`), kein „Neues rueckt eine Nummer
nach hinten", keine `projekt-code/`-Ebene, kein `best-practices-<x>.md`-Praefix. Ein neues Harness-Thema
ist einfach eine neue Datei `claude-tooling/<thema>.md`; thematisch Unsortiertes sammelt `claude-tooling/neues.md`.

**Ordner-anlegen ist Pflicht und erlaubt:** Fehlt ein Ziel-/Zwischenordner oder die Wissensbasis ganz →
ERST anlegen (Datei-Werkzeug bzw. `mkdir -p`, falls Shell verfuegbar), DANN schreiben. Jede Datei startet
mit einer kurzen Ueberschrift; Eintraege kommen erst beim Recherchieren dazu. NIEMALS abbrechen, weil ein
Ordner fehlt. Anderer Basis-Ordner vom Benutzer genannt → dorthin (gleiche flache Struktur).

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Mount-Schreibfalle:** Die Mount-Bruecke kann das **Dateiende abschneiden**. Nach JEDEM Schreiben das
  Dateiende pruefen (`tail -1`, `wc -l`) — besonders kritisch fuer das grosse `_changelog-archiv.md`
  (lieber das Script unten als von Hand schreiben).
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf laeuft max ~45 s; Hintergrundprozesse ueberleben den
  Wechsel zwischen Aufrufen NICHT. Researcher laufen als **Agenten** (unkritisch); jeder Git-/Schreib-/
  Script-Schritt muss in EINEM Aufruf durchlaufen.
- **Git NIEMALS nackt:** IMMER ueber `bash ~/proggs/cowork-git.sh` (Datenverlust-Waechter). NIE direktes `git`.

---

## Zwei Bereiche, ein flacher Ordner

1. **Harness** (`claude-tooling/`): Hooks, Skills, Agents, Plugins, MCP-Server, Slash-Commands, Settings,
   Kontext-Management, Token-Effizienz, Arbeitsweise, Researcher, Neues. Quelle = offizieller
   **Claude-Code-Changelog** (via `scripts/update-changelog`). Versions-Anker = installierte Claude-Code-Version.
2. **Projekt-Code** (`android/`, `android-build/`, `desktop/`, `web/`, `apis/`, `peripherie/`, `assets/`,
   `agents/`): `<kategorie>/<software>.md`. Quelle = **eigener Changelog/Release-Notes** der Software (KEIN
   Claude-Script). Versions-Anker = LIVE ermittelte installierte Version (`kotlinc -version`, `swift --version`,
   `./gradlew --version`, `dotnet --version` …). Zweite Seite der Medaille zum Bug-Almanach: dort *was
   schiefgeht*, hier *wie man es von vornherein richtig macht*.

## Fokus auf einen Bereich (statt Volllauf)

Standard deckt alle Kategorien ab; einschraenkbar auf EINEN Bereich ("nur fuer Kotlin", "nur fuer Hooks",
"nur fuer Chrome-Erweiterungen") — dann nur die genannte Kategorie/Software recherchieren und schreiben.
Das nutzt u. a. die `bug-almanach-recherche`, wenn sie gezielt eine Software aufrollen laesst.

## Ablauf eines Laufs

1. **Stand lesen:** `best-practices/_state.json` + `README.md` → welche Version/welches Datum zuletzt?
2. **Changelog verbatim holen + Delta bestimmen:** Den KOMPLETTEN offiziellen Claude-Code-Changelog
   wortwoertlich holen (Script, siehe unten) und mit `last_version` vergleichen, um neue Versionen zu
   erkennen. (Volllauf gewuenscht → Delta-Schritt ueberspringen, alle Kategorien neu aufrollen.)
3. **Nichts Relevantes neu?** → "Nichts Neues seit Version X (Stand: Datum)" melden, fertig.
4. **Delta vorhanden?** → parallele Researcher starten (Regeln unten). Jeder bearbeitet einen klar
   begrenzten Bereich und recherchiert pro Kategorie: **WAS** hat sich geaendert (offizielles Changelog),
   **WIE** wendet man es am besten an (offizielle Docs/Engineering-Blog), **Alternativen** von aussen
   (extern gelabelt, sekundaer). Was in keine Kategorie passt → `claude-tooling/neues.md`.
5. **Speichern:** Themen-/Software-Datei aktualisieren (jeder Eintrag mit Quelle + Datum +
   `offiziell`/`extern`-Flag), `_changelog-archiv.md` inkrementell, `README.md` + `_state.json`.
   Bei **Projekt-Code**-Laeufen zusaetzlich die **Bug-Almanach-Rueckkopplung** (Abschnitt unten).
   **Self-Test (falls Shell + Python):** danach `python bugs/health.py` — alle fuenf coupling-Checks
   muessen gruen sein. Kein Python in Cowork? → Bezugs-Tabellen + Header manuell pruefen. Wird ein
   SOFTWARE-gebundener Almanach neu angelegt/zurueckgekoppelt, traegt er das strukturierte
   `> **Anker:** <label>=<version>`-Feld (SYSTEM.md §7) + ggf. einen `bugs/check-version-anchor.py`-Eintrag.
6. **Auswertung ausgeben** (Format unten).

## Changelog-Archiv — vollstaendig & verbatim (KRITISCH)

`best-practices/_changelog-archiv.md` ist die **Rohdaten-Grundlage** fuer spaetere Mustererkennung. Eine
Zusammenfassung ist hier wertlos — ein Researcher braucht den Volltext. Strikt:

- **Verbatim, ungekuerzt:** KOMPLETTER offizieller Changelog JEDER Version, alle Bullet-Points wortwoertlich.
  KEINE Zusammenfassung, KEINE Kuerzung.
- **Kanonische Quelle:** der Claude-Code-`CHANGELOG.md` (raw, anthropics/claude-code) — alle Versionen,
  neueste oben. Mindest-Abdeckung: ab 2.0.
- **Download-Methode:** Direkter Datei-Download via Script — NIEMALS WebFetch oder ein Researcher (die
  fassen zusammen und zerstoeren die Vollstaendigkeit).
- **Datum je Version:** aus den **npm-Publish-Zeitstempeln** (`@anthropic-ai/claude-code`, Feld `time`).
  Format: `## X.Y.Z — YYYY-MM-DD`, fehlt der npm-Eintrag: `— Datum unbekannt`.

### Ausfuehrung: das Script (nicht von Hand nachbauen!)

Die komplette Mechanik (Download + npm-Datum + Erstlauf/inkrementell + Verifikation) steckt im Script unter
`scripts/`. NICHT als Prosa rekonstruieren — aufrufen. **Pflicht: den Zielordner relativ setzen** (der
Default ist bereits `./best-practices`, aber zur Sicherheit immer mitgeben), sonst schreibt das Script am
gemounteten Ordner vorbei:

| Plattform | Befehl (relativer Zielordner!) |
|-----------|--------------------------------|
| macOS/Linux/Cowork-VM | `bash scripts/update-changelog.sh --data-dir ./best-practices` |
| Windows-CLI | `pwsh scripts/update-changelog.ps1 -DataDir ./best-practices` |

- **Ohne Flag = inkrementell:** nur Versionen neuer als `_state.json.last_version` werden oben angehaengt;
  alte Eintraege + Hand-Notizen bleiben unangetastet.
- **Mit `--first-run` (sh) / `-FirstRun` (ps1):** kompletter Neu-Aufbau (Erstlauf oder Reparatur).
- Das Script pflegt `_state.json` selbst und gibt am Ende eine Verifikation aus (Header-Zahl, Duplikat-Check).
- Nach dem Lauf das **Dateiende** von `_changelog-archiv.md` pruefen (`tail -1`, `wc -l`) — Mount-Truncation-Schutz.

## Taxonomie (12 Kategorien, selbst-erweiternd — als flache Dateien)

| # | Kategorie | Inhalt | Datei (flach) |
|---|-----------|--------|---------------|
| 1 | Hooks | Events, Schema, Exit-Codes, Neuerungen | `claude-tooling/hooks.md` |
| 2 | Skills | SKILL.md-Format, Trigger, References, skill-creator | `claude-tooling/skills.md` |
| 3 | Agents | Agent-Tool, Definitionen, Modellwahl, Parallelisierung | `claude-tooling/agents.md` |
| 4 | Plugins | plugin.json, Struktur, Marketplace, Komponenten | `claude-tooling/plugins.md` |
| 5 | MCP-Server | .mcp.json, Konfiguration, lohnende Server, Resources/Tools | `claude-tooling/mcp.md` |
| 6 | Slash-Commands | Command-Format, Argumente | `claude-tooling/commands.md` |
| 7 | Settings & Konfig | settings.json, Permissions, Env-Vars, Statusline, Output-Styles | `claude-tooling/settings.md` |
| 8 | Kontext-Management | Compaction, Microcompact, Autocompact-Schwellen, Memory | `claude-tooling/kontext.md` |
| 9 | Token- & Kosten-Effizienz | Caching, Modellwahl, Effort-Levels, Parallelisierungs-Oekonomie | `claude-tooling/token-effizienz.md` |
| 10 | Arbeitsweise / Verhalten | Wie man Fragen angeht, Multi-Task, Planung, TDD | `claude-tooling/arbeitsweise.md` |
| 11 | Researcher & Internet-Recherche | Robuster Einsatz von Researcher-Subagenten: Parallelitaet, Token-/Fetch-Limits, Absturz-Vermeidung, Checkpointing, gute Web-Prompts (eigener Fokus, weil Researcher oft abstuerzen) | `claude-tooling/researcher.md` |
| 12 | Neues / Horizont-Scan | Auffangzone fuer alles, was in keine Kategorie passt | `claude-tooling/neues.md` |

Kategorie 12 (Neues) ist wichtig: gerade die ganz neuen Faehigkeiten bringen den groessten Sprung. Die
`#`-Spalte sind nur Listennummern, KEINE Ordnernamen/Reihenfolge — die Speicherung ist flach. Ein neues
Harness-Thema ist einfach eine neue Datei (z. B. `claude-tooling/output-styles.md`).

**Projekt-Code-Kategorien:** dieselben wie `bugs/<kategorie>/`, je `best-practices/<kategorie>/<software>.md`
(z. B. `android/kotlin.md`, `desktop/swift-appkit.md`; kein `best-practices-`-Praefix, keine `projekt-code/`-
Ebene). **Mechanik-Unterschied:** Harness → Claude-Code-Changelog (Script, Anker = Claude-Code-Version).
Projekt-Code → eigener Software-Changelog (KEIN Claude-Script, Anker = live ermittelte Software-Version).
Quellen-Rangordnung gleich. Fokussierter Lauf ("nur fuer Kotlin") = genau eine Software/Kategorie, kein Volllauf.

## Kopplung zum Bug-Almanach (beide Richtungen — PFLICHT bei Projekt-Code)

Best-Practices und Bug-Almanach (`bugs/<kategorie>/<bereich>.md`) sind zwei Seiten derselben Medaille; die
Kopplung laeuft symmetrisch in beide Richtungen. Foerdert die Recherche einen konkreten BUG / eine Falle
zutage (nicht nur eine positive Empfehlung), gehoert er in den Almanach ZURUECK (v. a. Projekt-Code).

**A — Bug-Fund zurueckschreiben** (Symptom + Ursache + funktionserhaltende Loesung + betroffene Versionen):
- **`bugs/<kategorie>/<bereich>.md` existiert** → als Eintrag ergaenzen (Format aus `bugs/SYSTEM.md`:
  `## N. Titel / Symptom / Ursache / Versionen / FIX / Quelle`), gegen Bestehendes DEDUPLIZIEREN,
  Stand-Header aktualisieren, im FIX-Feld auf den Best-Practice-Abschnitt verweisen.
- **existiert NICHT** → NICHT im Vorbeigehen einen halben Almanach anlegen (ihm fehlt die Fix-Status-Pruefung
  per `gh`, die nur `bug-almanach-recherche` macht). Stattdessen melden ("Best-Practices-Recherche hat Bugs
  fuer <bereich> gefunden, aber keinen Almanach") und `bug-almanach-recherche` vorschlagen (erst Franks OK).
  Gefundene Bugs kompakt mitliefern, damit nichts verloren geht.

**B — Bezugs-Tabellen synchron halten:** Existieren BEIDE Dateien, in JEDER eine wechselseitige
Abschnitts-Bezugs-Tabelle „Best-Practice-Abschnitt ↔ Bug-Abschnitt" aktuell halten. Fehlt eine → anlegen;
kamen Abschnitte dazu → ergaenzen. Diese Rueckrichtung ist PFLICHT (beide Speicher fuettern sich gegenseitig).

## Researcher-Regeln (KRITISCH — Absturz-Schutz)

- **Modell:** hoechstes Opus (1M). **Effort:** X-High. `opts.model` NICHT setzen. **Agent-Typ:** `researcher`
  (laeuft als Agent, nicht als Shell-Hintergrund → vom ~45s-Cowork-Limit unberuehrt).
- **Direkt 7 Researcher GLEICHZEITIG starten, dann Continuous-Spawning:** Bei genug Themen IMMER mit
  **7 auf einmal** beginnen (NICHT erst 4, dann 3). Mehr als 7 Themen: sobald einer fertig ist, SOFORT den
  naechsten hinterher starten, sodass konstant 7 laufen — keine zweite Welle. Empirisch: 5 sicher, 7 laeuft,
  ~12 → Abstuerze. Obergrenze ~7 (RPM-Limit, NICHT Kontextfenster).
- **Warum ~7 (RPM, nicht Kontext):** Web-Researcher sind anfrage-dicht (100+ RPM bei 5 Stueck). Zu viele
  sprengen das Anfrage-Raten-Limit/den Server-Burst-Schutz — UNABHAENGIG vom 1M-Kontext.
- **KEIN Findings-/Ergebnis-Cap:** ALLE gefundenen Best-Practices/Bugs dokumentieren (kein "max 50"); mit
  1M-Kontext kein Absturzrisiko, Kappen waere lossy. Sehr viele Funde verlustfrei in die Kategorie-Datei
  schreiben + kompakte Summary, nie kappen.
- **Retry mit Backoff bei 429 (PFLICHT):** Rate-Limit-Absturz → sofort melden + exponential backoff neu
  starten (`retry-after` beachten), nie still aufgeben.
- **Scope (gegen RPM/Haengen):** ~15 Websuchen/Fetches, ~10 Min pro Researcher. Begrenzt die Anfrage-Rate,
  nicht die Findings-Zahl.
- **Checkpoint / Continuation:** Researcher schreibt Fortschritt **inkrementell** in die Kategorie-Datei
  und endet mit klarem Checkpoint-Marker (was fertig, wo weitermachen). Nicht fertig → Continuation-Researcher
  am Checkpoint starten.
- **Einheitliches Header-Format erzwingen + aufraeumen:** Jede Kategorie-Datei MUSS beginnen mit
  `# [Kategorie] — Best Practices (Stand JJJJ-MM-TT, Claude Code X.Y.Z)` — im Prompt verlangen UND nach dem
  Lauf pruefen. Sentinel-/Writeback-Artefakte (`_writeback.json`) gehoeren NICHT in `best-practices/` — vor
  dem Commit entfernen.

## Quellen-Rangordnung

| Prioritaet | Quelle | Rolle |
|-----------|--------|-------|
| 1 | Offizielle Quellen (Claude-Code-Changelog, offizielle Docs, Engineering-Blog; bzw. Hersteller-Releases der jeweiligen Software) | **Grundwahrheit** fuer die Fakten |
| 2 | Externe/Community (Blogs, Profis, GitHub-Diskussionen) | Nur abwaegbare **Alternative**, klar als `extern` gelabelt — ueberstimmt NIE das Offizielle |

Jeder Eintrag traegt **Quelle + Datum**. Wird nichts Neueres gefunden, gilt der letzte Eintrag weiter.

## Verlaesslichkeits-Leitplanken

- Eine falsche Best-Practice ist schlimmer als gar keine — offiziell zuerst, alles mit Quelle + Datum.
- Niemals eine externe Behauptung als offiziell darstellen.
- Dieser Skill schreibt in `best-practices/` UND (Rueckkopplung) in `bugs/<bereich>.md`, und schlaegt vor.
  Er aendert NIEMALS ein Hook, Skill, Agent, MCP oder Setting (nur Wissensdateien).

## Auswertungs-Format

Am Ende JEDES Laufs eine **ausfuehrliche** Auswertung — nicht nur Stichpunkte. Drei Teile in dieser Reihenfolge:

### Teil A — Ausfuehrliche Auswertung je Kategorie
Pro betroffener Kategorie: **Neuerungen** (was hat sich geaendert, Version + Quelle), **Best Practices**
(wie nutzt man es heute am besten), **Betrifft eigene Werkzeuge** (beruehrt es ein vorhandenes
Hook/Skill/Agent/Setting?).

### Teil B — Kurz-Header
```
## Best-Practices-Lauf — [Datum]
Geprueft: Version [alt] → [neu] | Kategorien: [Anzahl] | Quellen: [Anzahl]
```

### Teil C — UMSETZBARE VERBESSERUNGSVORSCHLAEGE (das Herzstueck — PFLICHT am Ende)
Nummerierte Liste konkreter, sofort abnickbarer Vorschlaege — NUR solche mit echtem, belegtem Vorteil.
Pro Vorschlag genau dieses Format:
```
N. [Konkrete Aktion in einem Satz]
   Vorteil (belegt): [was es bringt] — Quelle: [offizieller Fund aus diesem Lauf]
   Betrifft: [Datei/Werkzeug] | Aufwand: [klein/mittel/gross] | Risiko: [niedrig/mittel/hoch]
```
Regeln fuer Teil C:
- **Umgebungs-Gegencheck VOR dem Listen (PFLICHT):** Jeden Vorschlag gegen den IST-Zustand pruefen, nicht
  nur aus dem Changelog-Feature ableiten. Drei Fragen: (1) Existieren die betroffenen Skills/Hooks/Dateien
  ueberhaupt? (2) Nutzt der betroffene Skill/Hook das Feature wirklich? (3) Bringt es in GENAU dieser
  Umgebung echten Nutzen? Nur Vorschlaege, die alle drei bestehen, werden gelistet.
- NUR research-gestuetzte Vorschlaege mit nachweisbarem Vorteil. Lieber 3 starke als 10 schwache.
- Jeder Vorschlag mit einem einfachen "ja" umsetzbar (konkret, nicht "man koennte mal").
- Vorschlaege, die ein eigenes Werkzeug aendern, klar markieren (Teil-2-Gebiet, aber hier abnickbar).
- Widerlegt ein Fund einen FRUEHEREN Vorschlag → ehrlich sagen und den alten zurueckziehen.
- Danach auf die Auswahl des Benutzers warten und NUR die bestaetigten Vorschlaege umsetzen.

---

## Sichern (Cowork-Git)

Git-Repo verbunden → committen + pushen ueber das Cowork-Skript (nur eigene relative Pfade):
```bash
bash ~/proggs/cowork-git.sh setup                 # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push-files "#NNN - best-practices: <bereich> aktualisiert" \
  best-practices/<...> bugs/<...>
```
Kein Git-Repo verbunden → nur speichern und dem Benutzer den Ablage-Pfad nennen.

## Was NIEMALS passieren darf
- Aus Cowork mit nacktem `git commit`/`git push` arbeiten (immer `cowork-git.sh`).
- Den Script-Zielordner fest auf `~/proggs/best-practices` lassen — IMMER relativ (`./best-practices`), sonst
  schreibt das Script am Mount vorbei.
- Eine alte Pfad-Taxonomie erzeugen (nummerierte Ordner, `projekt-code/`, `best-practices-<x>.md`-Praefix) —
  die Struktur ist flach: `claude-tooling/<thema>.md` und `<kategorie>/<software>.md`.
- Den Changelog per WebFetch/Researcher holen (zerstoert die Vollstaendigkeit) — nur das Script + Dateiende pruefen.
- Echte Funde an einem kuenstlichen Cap abschneiden (alle dokumentieren; sehr viele verlustfrei auslagern).
- Eine externe Behauptung als offiziell darstellen; Unsicheres als sicher ausgeben; Vorschlaege ohne belegten Vorteil listen.
- Mehr als ~7 Researcher gleichzeitig (RPM-Absturz) oder einen Researcher-Crash verschweigen.
- Quelle/Datum/`offiziell`/`extern`-Flag bei einem Eintrag weglassen.
- Bei Projekt-Code die Bug-Almanach-Rueckkopplung oder die Bezugs-Tabellen vergessen.
- Ein Werkzeug (Hook/Skill/Agent/MCP/Setting) tatsaechlich aendern — dieser Skill nur recherchiert/speichert/schlaegt vor.
- Nach einem Schreiben das Dateiende nicht pruefen (Mount-Truncation).

## Referenzen
- `scripts/update-changelog.{sh,ps1}` — holt den Claude-Code-Changelog verbatim (DataDir-Default relativ).
- `best-practices/`, `bugs/` im Arbeitsordner — Ziele der Persistenz (flach 1:1).
- `bugs/SYSTEM.md`, `bugs/health.py` — Almanach-System + Self-Test im Arbeitsordner.
- Verwandte Skills: `bug-almanach-recherche` (Bugs), `research` (allgemeine Themen-Recherche).
- `bugs/claude-tooling/cowork-git-push.md` — die Cowork-Git-/Mount-Regeln.
