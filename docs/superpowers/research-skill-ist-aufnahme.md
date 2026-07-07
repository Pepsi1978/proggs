# Research-Skill — Ist-Aufnahme der 11 Recherche-Einheiten

> Erstellt: 2026-06-21. Zweck: Grundlage für einen NEUEN zentralen `research`-Skill, an den
> alle hier erfassten Skills/Agenten delegieren sollen. Für jede Einheit ist das exakte
> Research-Profil dokumentiert, damit bei der Übergabe an den `research`-Skill NICHTS verloren geht.
>
> Quellenbasis: die jeweiligen `SKILL.md`/`<agent>.md`-Dateien, die referenzierte
> `references/researcher-prompts.md`, die Policy-Regel `~/.claude/rules/research-strategy.md`
> sowie die Pipeline-Skripte `mm-research.py` (Option A) und `or-research.py` (Option B).
> Wo eine Datei etwas nicht spezifiziert, steht „nicht spezifiziert".

---

## Gemeinsamer Rahmen (gilt laut research-strategy.md für ALLE 11)

- **Vor JEDER Web-Recherche Pflicht-Gate:** kurze Empfehlung (3-4 Zeilen, A/B/C/D) → `AskUserQuestion`
  Frage 1 (A/B/C/D). Bei Option A nach Abschluss Frage 2 (zusätzliche Eskalation?).
- **Option A = `mm-research.py`** (Firecrawl holt VOLLE Seiten → MiniMax M3 max Thinking wertet aus).
  Firecrawl Free: **max 2 gleichzeitig**, 1000 Seiten/Monat, 5 Suchen/min. Aufruf:
  `python3 ~/proggs/mm-research.py "<frage>" [n]`. Ausgabe = kompakte quellentreue Antwort auf stdout;
  Rohdaten + Thinking in `~/.mm-research/` (laufen NIE durch den Opus-Kontext).
- **Option B = `or-research.py`** (OpenRouter `web_search` server-tool, Snippets, ein 1M-Modell sucht selbst).
  Default-Modell `minimax/minimax-m3`, Eskalation `z-ai/glm-5.2`. Kein Monatslimit, höhere Parallelität.
  Aufruf: `python3 ~/proggs/or-research.py "<frage>" [modell] [engine]`. Ausgabe = Antwort + Web-Quellen
  auf stdout; Rohdaten in `~/.or-research/answer.json`.
- **Option C = Opus-Schwarm** (die bestehenden Researcher-Agenten). NUR auf explizite Wahl von Frank.
- **`research-approval`-Hook** blockt mm/or-research, bis A/B gewählt UND `touch "$TEMP/research-approved.flag"`.
- **Persistenz-Pflicht (`research-persistence.md`):** Stufe 3 = der Hauptagent arbeitet die Ergebnisse
  in `best-practices/` + `bugs/` ein (Kurzcheck UND Volltext).

---

## 1. bug-almanach-recherche — Skill

- **Name + Typ:** `bug-almanach-recherche` (Skill). Anspruchsvollste Research-Mechanik (feste Prompt-Vorlagen in `references/researcher-prompts.md`).
- **Zweck:** Recherchiert öffentlich bekannte Bugs/Fallen/Workarounds eines technischen Bereichs und
  erzeugt daraus einen kuratierten, **versionsbewussten** Bug-Almanach. **Output-Ziel:**
  `~/proggs/bugs/<kategorie>/<bereich>.md` (neue Datei). Zusätzlich: best-practices-Rückkopplung,
  `bugs/README.md`-Index, Hook-Mapping (`bug-almanac-guard`, `bug-almanac-hint.py`), Versions-Anker
  (`check-version-anchor.py`), Self-Test `bugs/health.py`, Commit+Push.
- **Research-Umfang heute:** Breite Bug-Suche (Schritt 2) + SEPARATE Fix-Status-Recherche (Schritt 3).
  Bei Option C: **7 Researcher gleichzeitig, Continuous-Spawning** (sobald einer fertig → sofort nächsten,
  konstant 7; >7 Themen → weiterspawnen bis alle Teilbereiche durch). Bei Option A: pro Bug-Aspekt ein
  `mm-research.py`-Call, **Firecrawl max 2 parallel** (2 starten → warten → 2 neue). Ganzes Thema komplett,
  kein Eintrags-Cap.
- **Engine/Modell heute:** Default A (`mm-research.py`); B = `or-research.py`; C = Opus-7er-Schwarm
  (`researcher`-Agenten, opus[1m], `opts.model` NICHT setzen).
- **Themen-Zerlegung:** FESTE Prompt-Vorlagen in `references/researcher-prompts.md`. Gemeinsamer Prompt-Kopf
  (`[BEREICH]`/`[VERSION]`/`[DATUM]` einsetzen) + **5 Standard-Teilbereiche**: (1) Offizielle Doku +
  Hersteller-Hilfen [Vorrang vor Foren], (2) GitHub-Issues/Bugtracker, (3) Community/Praxis, (4) Plattform-Fallen
  (Windows + macOS/Linux), (5) Mechanik/bereichsspezifisch. Bei 7 die Liste um Unterthemen erweitern.
  Fix-Status (Phase B): Changelog-Researcher liefern Issue-Nummern; Hauptagent prüft hart per `gh`.
  Pro Bug zurück: **Titel · Symptom · Ursache · Loesung (funktionserhaltend!) · betroffene Versionen · Quelle (URL)**.
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: Bereich + Slug; **LIVE ermittelte Version(en)** der jeweiligen Software (Schritt 1 — nicht raten,
    mehrere Versionen pro Android-Projekt normal); der gewählte Recherche-Weg (A/B/C); die 5-7 Teilbereiche;
    Pflicht-Limits (max 15 Fetches, 10 Min, KEIN Eintrags-Cap); Quellen-Rangordnung (offiziell > Foren);
    bestehender Almanach-Stand (falls Re-Recherche) für „nur Neues seit X"; Persistenz-Ziel `bugs/<kat>/<bereich>.md`.
  - ZURÜCK: pro Bug die 6 Felder; **separat** die Issue-Nummern/URLs für die `gh`-Fix-Status-Prüfung
    (Researcher liefern NUR die Nummern — die harte OPEN/CLOSED-Prüfung macht der Hauptagent via `gh`, weil
    `researcher` kein Bash hat); klare Trennung *belegt gefixt* vs. *Status unklar*; Crash-Meldungen.
- **Besonderheiten:** (a) **Versionsbewusst** — Version ist der Anker für Fix-Status; (b) **separate
  Fix-Status-Recherche** + harte `gh`-Prüfung durch Hauptagent (Researcher haben KEIN Bash); (c)
  Ehrlichkeits-Pflicht (kein „gefixt" ohne Beleg); (d) **best-practices-Abgleich beide Richtungen** (4a lesen
  via `grep best-practices/`, 4b Prävention zurückschreiben, 4c Bezugs-Tabellen synchron); (e) Researcher
  bei sehr vielen Funden File-as-Memory + Summary statt kappen.

---

## 2. best-practices — Skill

- **Name + Typ:** `best-practices` (Skill).
- **Zweck:** Pflegt Best-Practices in zwei Bereichen — (1) Harness (`claude-tooling/<thema>.md`, Quelle =
  Claude-Code-Changelog) und (2) Projekt-Code (`<kategorie>/<software>.md`, Quelle = eigener Software-Changelog).
  „Zweite Seite der Medaille" zum Bug-Almanach. **Output-Ziel:** `~/proggs/best-practices/<…>.md` (+ Bug-Rückkopplung
  in `bugs/`). Liefert am Ende eine Auswertung mit umsetzbaren Vorschlägen.
- **Research-Umfang heute:** Standardlauf deckt ALLE Kategorien ab (oder fokussierbar auf eine, z.B. „nur Kotlin").
  Pro Kategorie: WAS hat sich geändert (Changelog) + WIE wendet man es an + Alternativen (extern). Bei Option C:
  **7 Researcher gleichzeitig, Continuous-Spawning** (5 sicher, 7 ok, 12 → Absturz). Kein Findings-Cap.
- **Engine/Modell heute:** Default A (`mm-research.py` pro Kategorie, Firecrawl max 2 parallel); B; C =
  Opus-7er-Schwarm (opus[1m], X-High). **Ausnahme:** Der Changelog-Verbatim-Download läuft NIE über einen
  Researcher, sondern über das deterministische `scripts/update-changelog.ps1/.sh` (sonst wird zusammengefasst → Vollständigkeit zerstört).
- **Themen-Zerlegung:** 12-Kategorien-Taxonomie (Hooks, Skills, Agents, Plugins, MCP, Commands, Settings,
  Kontext, Token-Effizienz, Arbeitsweise, Researcher, Neues/Horizont-Scan). Pro Kategorie ein begrenzter
  Suchauftrag. Keine festen Prompt-Vorlagendatei wie #1 — die Kategorien-Taxonomie IST die Zerlegung.
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: gewählte Kategorie(n) ODER Volllauf; **Versions-Anker** (Harness = installierte Claude-Code-Version;
    Projekt-Code = LIVE ermittelte Software-Version); **bestehender Stand** (`_state.json.last_version` + letztes
    Datum) → nur Delta recherchieren; Quellen-Rangordnung (offiziell > extern); Recherche-Weg A/B/C.
  - ZURÜCK: pro Eintrag **Quelle + Datum + `offiziell`/`extern`-Flag**; Versionsnummer der Neuerung; ob es ein
    vorhandenes Hook/Skill/Agent/Setting berührt; gefundene **Bugs** (für Rückkopplung in den Almanach); Material
    für die 3-teilige Auswertung (Teil A je Kategorie, Teil B Kurz-Header, Teil C umsetzbare Vorschläge mit
    **Umgebungs-Gegencheck**).
- **Besonderheiten:** (a) Changelog-Verbatim per Script, NIE Researcher; (b) Harness vs. Projekt-Code haben
  UNTERSCHIEDLICHE Changelog-Quellen + Versions-Anker; (c) Bug-Rückkopplung Pflicht bei Projekt-Code; (d)
  Header-Format erzwingen `# <Thema/Software> — Best Practices (Stand JJJJ-MM-TT, Version V)`; (e) Teil C nur
  research-gestützte Vorschläge nach Ist-Abgleich.

---

## 3. best-practices-update — Skill

- **Name + Typ:** `best-practices-update` (Skill). Batch-/Wellen-Version von #2.
- **Zweck:** Hebt BESTEHENDE Best-Practices-Dateien als **Welle** auf den neuesten Stand (Re-Recherche).
  **Output-Ziel:** dieselben `best-practices/<…>.md`-Dateien (Stand-Header + Anker heben) + Bug-Rückkopplung.
  Primär für regelmäßige Wellen in Claude Cowork.
- **Research-Umfang heute:** Geht viele Dateien nacheinander durch (Auswahl: Stand-Verfall / Harness vs.
  Projekt-Code / Datei-Liste / Alle). Pro Datei der vollständige `best-practices`-Ablauf. **Eine Datei = ein
  Rettungspunkt** (committen+pushen vor der nächsten). Researcher-Strom konstant 7 (Option C).
- **Engine/Modell heute:** Default A/B; C = Opus-7er-Schwarm. Identische A/B/C-Mechanik wie #2.
- **Themen-Zerlegung:** Pro Datei dieselbe Logik wie #2 (Kategorie/Software). Den BESTEHENDEN Stand mitgeben →
  nur NEUES seit Stand X recherchieren.
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: Arbeitsliste (welche Dateien, priorisiert nach Stand-Verfall); pro Datei: bestehender Stand/Datum,
    LIVE-Version, Harness-vs-Projekt-Code-Quelle, Recherche-Weg.
  - ZURÜCK: pro Datei wie #2 (Quelle+Datum+Flag, Versionen, Bugs für Rückkopplung), plus Hinweis ob fertig
    oder Continuation nötig (Checkpoint).
- **Besonderheiten:** (a) Wellen-Disziplin + TaskCreate-Fortschritt; (b) Fix-Status/Versions-Claims HART per
  `gh` (Hauptagent); (c) Cowork-Spezifik (`cowork-git.sh push-files`, ~45s/Shell-Aufruf, Dateiende prüfen);
  (d) Self-Test `bugs/health.py` (coupling-Check) vor Commit.

---

## 4. almanach-update — Skill

- **Name + Typ:** `almanach-update` (Skill). Batch-/Wellen-Version von #1.
- **Zweck:** Hebt BESTEHENDE Bug-Almanache als **Welle** auf die aktuelle Software-Version (Re-Recherche).
  **Legt NIE einen neuen Almanach an** (das macht #1). **Output-Ziel:** dieselben `bugs/<kat>/<bereich>.md`
  (Stand + Anker heben) + Best-Practices-Rückkopplung. Primär für Cowork.
- **Research-Umfang heute:** Viele Almanache nacheinander (Auswahl: Stand-Verfall / Kategorie / Liste / Alle).
  Pro Almanach der erprobte **7-Schritte-Ablauf** aus #1. Researcher-Strom konstant 7 (Continuous-Spawning,
  NIE Workflow, max 7 — ab ~12 RPM-Absturz).
- **Engine/Modell heute:** Default A/B; C = Opus-7er-Schwarm. Identische A/B/C-Mechanik wie #1.
- **Themen-Zerlegung:** Pro Almanach die 7 Teilbereiche aus #1: offizielle Doku/Changelog · Issue-Tracker ·
  Community/Praxis · Plattform-Fallen (Win+Mac) · Mechanik · Fix-Status-Changelog · neue Features. Den
  BESTEHENDEN Almanach-Stand mitgeben → nur Neues seit Stand X (bestehende Almanache oft schon umfangreich).
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: Arbeitsliste (Almanache priorisiert); pro Almanach: bestehender Stand/Bug-Liste, **LIVE-Version(en)**,
    die 7 Teilbereiche, Recherche-Weg.
  - ZURÜCK: pro Bug die 6 Felder + Dedup-Hinweis; Issue-Nummern für `gh`-Fix-Status (`NOT_PLANNED`=won't-fix
    → Workaround DAUERHAFT, `COMPLETED`=gefixt, `DUPLICATE`=gebündelt); Best-Practices-Kandidaten für 4b.
- **Besonderheiten:** wie #1 plus: (a) Wellen-Disziplin (ein Almanach = ein Rettungspunkt); (b) `gh`-Gegenprobe
  Pflicht (Web-Snippets bei Versions-/Status-Angaben unzuverlässig); (c) Cowork-Spezifik; (d) Self-Test
  `bugs/health.py` (alle 5 Checks grün).

---

## 5. direktiven-recherche — Skill

- **Name + Typ:** `direktiven-recherche` (Skill — startet den gleichnamigen Agenten, siehe #10).
- **Zweck:** Startet tiefe Internet-Recherche zur besseren Umsetzung der 3 Hauptdirektiven. **Output-Ziel:**
  Bericht `~/proggs/DIREKTIVEN-RECHERCHE-[DATUM].md` mit NUR NEUEN/VERBESSERBAREN Vorschlägen.
- **Research-Umfang heute:** Spawnt den `direktiven-recherche`-Agenten (der intern **5 (laut Skill) bzw. 3
  (laut Agent-Datei, reduziert für Stabilität)** parallele Researcher fährt). Dauer 5-10 Min.
- **Engine/Modell heute:** Skill ruft Agent via `subagent_type: "general-purpose"`, `model: "opus"`, mit dem
  vollständigen Agent-Prompt. Der 5/3-Researcher-Schwarm = **Option C**; A/B laufen über den Orchestrator.
- **Themen-Zerlegung:** Nicht im Skill selbst — die Zerlegung in 3 Researcher-Themen liegt im Agenten (#10).
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: die vollständigen 3 Direktiven-Texte (Agent liest sie selbst); Ist-Zustand (was schon implementiert);
    Recherche-Weg A/B/C.
  - ZURÜCK: NUR neue/verbesserbare Vorschläge (nichts schon Implementiertes); ausführliche deutsche Erklärungen
    (Nicht-Programmierer); konkrete Umsetzungsvorschläge; aktuelle Quellen (2025-2026).
- **Besonderheiten:** Diskrepanz Skill (5 Researcher) vs. Agent-Datei (3, „reduziert für Stabilität") — der
  research-Skill muss die strengere Zahl respektieren bzw. konfigurierbar machen.

---

## 6. superintelligenz — Skill

- **Name + Typ:** `superintelligenz` (Skill, `invocation: auto`).
- **Zweck:** Liefert die **Checkliste/das Leitbild für Direktive #1** (8 Dimensionen, Compound Intelligence,
  Harness-Begriff, autonome Selbstverbesserung). Es ist primär ein **Wissens-/Verhaltens-Skill**, KEIN
  eigenständiger Web-Recherche-Workflow. (Die eigentliche iterative Web-Recherche macht der `superintelligenz`-AGENT, #11.)
- **Research-Umfang heute:** Keine eigene Web-Recherche-Mechanik im SKILL.md — nur Fragen-Leitfaden für jede
  Session. „Research-Umfang heute" = nicht spezifiziert (kein Schwarm, keine Wellen im Skill).
- **Engine/Modell heute:** nicht spezifiziert (kein Recherche-Aufruf im Skill).
- **Themen-Zerlegung:** nicht spezifiziert (Skill ist Checkliste, kein Recherche-Auftrag).
- **Übergabe-Bedarf:** Für den research-Skill **kein direkter Recherche-Übergabe-Bedarf** — dieser Skill
  delegiert nicht selbst. Relevant wird er nur über den gleichnamigen Agenten (#11).
- **Besonderheiten:** Wichtig fürs Brücken-Design: NICHT jeder „Recherche"-genannte Skill ist ein
  Recherche-Workflow. Dieser hier ist reines Leitbild — der research-Skill braucht ihn nicht zu ersetzen.

---

## 7. researcher — Agent

- **Name + Typ:** `researcher` (Agent). Frontmatter: `model: opus`, `effort: high`, `maxTurns: 18`,
  Tools: WebSearch, WebFetch, Read, Write, Glob, Grep (**KEIN Bash**).
- **Zweck:** Schneller, leichtgewichtiger Web-Lookup. „Scout, kein Architekt." Wird 3-5 (bzw. im
  Almanach-Kontext bis 7) parallel gespawnt. **Output:** Bullet-Points mit Quellen, an den aufrufenden
  Orchestrator zurück; zusätzlich Sentinel-Writeback in MEMORY.md.
- **Research-Umfang heute:** 1 enger Suchauftrag pro Agent. Bei Bulk: max 50 Items/Durchgang. Dies ist die
  Bausteineinheit, aus der die Schwärme von #1/#2/#3/#4/#5/#10/#11 bestehen.
- **Engine/Modell heute:** **Option C** (Opus-Web). Der research-Strategy-Header sagt explizit: Standard ist
  `mm-research.py`/`or-research.py`; dieser Agent nur bei expliziter Opus-Wahl oder schnellem Einzel-Lookup.
- **Themen-Zerlegung:** Bekommt EINEN Suchbegriff/Suchauftrag vom Orchestrator. Keine eigene Zerlegung.
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: präziser, eng gescopter Suchauftrag; harte Limits (WebSearch max 8, WebFetch max 5, 150 Zeilen/Seite,
    Antwort max 100 Zeilen, maxTurns 18); Hinweis MEMORY.md vorher lesen (Dup-Vermeidung).
  - ZURÜCK: Bullet-Points + Quellen; **PFLICHT-Blöcke** `BEST-PRACTICES-KANDIDATEN:` und `BUG-KANDIDATEN:`
    (je mit Quelle-URL + Software-Version, oder `KEINE`); Sentinel-JSON nach `$TEMP/agent-writeback-researcher.json`;
    `[ERROR:]`-Präfix bei Fehlschlag.
- **Besonderheiten:** (a) Robustness/Circuit-Breaker (3 Tool-Fehler → sofort zurück; Turn 15 → Zusammenfassung);
  (b) **interner 50-Item-Cap** (Bulk-Chunking) — steht im Widerspruch zum „kein Eintrags-Cap" der Opus-Schwarm-
  Regel und zum Lossless-Prinzip → der research-Skill muss klären, welcher Cap gilt; (c) Sentinel-Writeback-Mechanik.

---

## 8. forschungsagent — Agent

- **Name + Typ:** `forschungsagent` (Agent). `model: opus`, `effort: high`, `maxTurns: 18`,
  Tools: Read, Write, Glob, Grep, WebSearch, WebFetch, Edit (**KEIN Bash**).
- **Zweck:** Bewertet Paper/Trends aus `~/proggs/Forschung.md` und wandelt sie in **CLI-spezifische
  Integrations-Pläne** (neue Rule/Agent-Upgrade/neuer Agent/neuer Skill/Hook). **Output-Ziel:** Rückschreiben in
  `~/proggs/Forschung.md` + Sentinel-JSON.
- **Research-Umfang heute:** FOKUSSIERT auf die lokale `Forschung.md` (zuerst lesen!), Web nur ergänzend.
  Kein Schwarm — ein einzelner Agent. Bewertet pro Paper nach 4-Kriterien-Framework (Umsetzbarkeit 30 /
  Intelligenz-Hebel 30 / Claude-Code-Fit 20 / Haltbarkeit 20).
- **Engine/Modell heute:** Web-Anteil = **Option C** (Opus-Web). Standard A/B über Orchestrator.
- **Themen-Zerlegung:** Keine Schwarm-Zerlegung; pro Forschung.md-Eintrag eine Bewertung. Web-Suchen sind
  gezielt (max 5 WebSearch / 5 WebFetch).
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: Inhalt von `Forschung.md` + MEMORY.md „Forschung & Intelligence" (was umgesetzt/verworfen) + Liste
    bestehender Agenten (`~/.claude/agents/*.md`); Recherche-Weg A/B/C.
  - ZURÜCK: pro Paper das feste Output-Format (Quelle, Intelligenz-Potenzial %, einfache Erklärung, konkreter
    Integrations-Plan mit Schritten, betroffene Komponenten, JA/NEIN-Empfehlung); Sentinel-JSON.
- **Besonderheiten:** (a) **lokale Quelle Forschung.md** ist primär, Web sekundär — der research-Skill muss
  diesen „lokal-zuerst"-Modus abdecken; (b) klare Abgrenzung zu `intelligence-researcher` (der sucht breit,
  forschungsagent bewertet fokussiert); (c) schreibt Integrations-Pläne, keine reine Faktenliste.

---

## 9. intelligence-researcher — Agent

- **Name + Typ:** `intelligence-researcher` (Agent). `model: opus`, `effort: high`, `maxTurns: 20`,
  Tools: Read, Write, Glob, Grep, WebSearch, WebFetch (**KEIN Bash**).
- **Zweck:** Dedizierter Intelligenz-Forscher für **self-improve Stufe 5**. Sucht Reasoning-Durchbrüche,
  kognitive Werkzeuge, kompetitive Analyse, biologische Muster, Selbstverbesserungs-Mechanismen.
  **Output-Ziel:** Findings via Sentinel-JSON → MEMORY.md „Forschung & Intelligence".
- **Research-Umfang heute:** Ein Agent über **5 Forschungsdimensionen** (Reasoning-Papers / kognitive Tools /
  kompetitive Analyse / biologische Muster / Selbstverbesserung). **Max 8 Findings pro Lauf.** Hat Gedächtnis
  früherer Findings (MEMORY.md-Status UMGESETZT/BLOCKIERT/OFFEN/VERWORFEN) zur Dup-Vermeidung.
- **Engine/Modell heute:** **Option C** (Opus-Web). Standard A/B über Orchestrator.
- **Themen-Zerlegung:** Die 5 Forschungsdimensionen (je mit konkreten Suchfeldern/Quellen: arXiv, ICML, NeurIPS,
  ICLR, GitHub, Cursor/Windsurf/Devin etc.). Pro Dimension ein klares Ziel („mind. 1 adaptierbare Technik").
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: MEMORY.md „Forschung & Intelligence" (Status-Liste für Dup-Filter); die 5 Dimensionen; Limits
    (WebSearch max 10, WebFetch max 8, 150 Zeilen/Seite, max 8 Findings, maxTurns 20).
  - ZURÜCK: pro Finding festes Format (Quelle, einfache Erklärung, Vorteil, Aufwand 5min/30min/1h/1d,
    JA/NEIN-Empfehlung, Umsetzungsschritte); Sentinel-JSON; `[ERROR:]`-Routing bei Fehler.
- **Besonderheiten:** (a) **interner 8-Findings-Cap** + Memory-basierte Dup-Vermeidung — der research-Skill
  muss den Memory-Status-Filter (UMGESETZT/BLOCKIERT/OFFEN/VERWORFEN) übernehmen; (b) 5 fixe Forschungs-
  dimensionen mit konkreten Quellen; (c) gehört in den self-improve-Workflow (Stufe 5).

---

## 10. direktiven-recherche — Agent

- **Name + Typ:** `direktiven-recherche` (Agent). `model: opus`, `effort: high`, `maxTurns: 40`,
  Tools: Read, Write, Edit, Glob, Grep, **Bash**, WebSearch, WebFetch, **Agent** (kann selbst Sub-Researcher spawnen).
- **Zweck:** Systematischer Tiefenforscher zur Umsetzung der 3 Direktiven. **Output-Ziel:**
  `~/proggs/DIREKTIVEN-RECHERCHE-[DATUM].md` + ausführliche deutsche Auswertung an den Benutzer (min. 500 Wörter).
- **Research-Umfang heute:** **5 Phasen.** Phase 1 = Direktiven + Ist-Zustand lesen. Phase 2 = **3 parallele
  Researcher** (bewusst REDUZIERT von 5 → 3 für Stabilität; Vorfall 2026-03-28: 5×50+ Funde → Absturz). Jeder
  Researcher: 5-8 Funde, STOPP bei 8. Phase 3 = gegen Ist-Zustand abgleichen (verwerfen/verbessern/neu).
  Phase 4 = Bericht. Phase 5 = Auswertung. Erwartung: 15-24 Funde total.
- **Engine/Modell heute:** 3-Researcher-Schwarm = **Option C** (Opus). Standard A/B über Orchestrator.
- **Themen-Zerlegung:** 3 FESTE Researcher-Themen: (1) Superintelligenz & Compound Intelligence [D1+D2],
  (2) Resilient Bugfixing & Self-Healing [D3 + Claude-Code-Architektur], (3) Aktuelle Papers & kompetitive
  Analyse [alle 3]. JEDEM Researcher wird eine **Zusammenfassung (max 500 Wörter) der relevanten Direktive(n)**
  mitgegeben — NICHT der Volltext (Kontext-Schutz). Limits pro Researcher: max 8 WebSearch/WebFetch, 150 Zeilen/Seite.
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: vollständige 3 Direktiven-Texte (Agent liest selbst) UND eine **500-Wörter-Zusammenfassung je Direktive**
    für die Sub-Researcher; Ist-Zustand-Dateiliste; frühere Recherche-Berichte; Recherche-Weg.
  - ZURÜCK: pro Fund (Titel, URL, Was gefunden, Umsetzungsvorschlag für CLI); Ist-Abgleich-Klassifikation
    (verwerfen/verbessern/neu); strukturierter Bericht; ausführliche Auswertung mit Alltags-Analogien.
- **Besonderheiten:** (a) **3 statt 5 Researcher** (Stabilität); (b) **Direktiven-Zusammenfassung statt Volltext**
  an Sub-Researcher (KRITISCHE Kontext-Disziplin); (c) Ist-Abgleich (nichts schon Implementiertes vorschlagen);
  (d) min. 500-Wörter-Auswertung mit Analogien Pflicht; (e) Turn-Budget 40 mit Phasen-Tracking.

---

## 11. superintelligenz — Agent

- **Name + Typ:** `superintelligenz` (Agent). `model: opus`, `effort: high`, `maxTurns: 30`,
  Tools: Read, Write, Edit, Glob, Grep, WebSearch, WebFetch, **Agent** (kann Sub-Agenten spawnen).
- **Zweck:** Iterativer, kreativer Wellen-Forscher für FUNDAMENTALE Systemverbesserungen entlang der 3
  Direktiven. **Output-Ziel:** Duplikat-freie Implementierungsliste `~/proggs/superintelligenz.md` + Top-5 mit
  ausführlichen Erklärungen + Meta-Learnings + Sentinel-JSON.
- **Research-Umfang heute:** **Mehrere Wellen** (Phase 0 Vorbereitung/Dup-Filter, Welle 1 breit/horizontal,
  Zwischen-Analyse [Herzstück: Lücken/Widersprüche/Kreuzverbindungen/Meta-Reflexion → neue Fragen], Welle 2
  Tiefenbohrung, Welle 3 interdisziplinäre Synthese). **Verbessert seine eigenen Fragestellungen ZWISCHEN den
  Wellen** (IRCoT-Pattern). Terminierungs-Checks (Welle überspringen bei <20% neuen Funden). Max 10 Findings/Lauf.
- **Engine/Modell heute:** Wellen-Schwarm = **Option C** (Opus). Standard A/B über Orchestrator. Limits:
  WebSearch max 15, WebFetch max 10, 150 Zeilen/Seite, max 10 Findings, maxTurns 30, Antwort max 300 Zeilen.
- **Themen-Zerlegung:** Pro Welle selbst-generierte Fragen, verteilt über die 3 Direktiven (Welle 1: 4-5 Fragen,
  davon mind. 1 pro Direktive UNGEWÖHNLICH/interdisziplinär). Welle 2: 3-5 SPEZIFISCHERE Fragen (adressieren
  Lücken/Widersprüche aus Welle 1). Welle 3: interdisziplinäre Querverbindungen (Biologie/Spieltheorie/
  Psychologie/Ingenieurwesen/Komplexitätstheorie → Agenten).
- **Übergabe-Bedarf (KRITISCH):**
  - HIN: bestehende `superintelligenz.md` „Bereits Recherchiert"-Liste (Dup-Filter) + MEMORY.md; Lücken-Analyse
    (welche Direktive hat die wenigsten Findings); die 3 Direktiven als Kompass; Recherche-Weg.
  - ZURÜCK: pro Finding das ausführliche Format (Direktive, Quelle, Welle, Nicht-Programmierer-Erklärung,
    konkreter Vorteil, Aufwand, Umsetzungstyp+Dateien+Schritte, Kreativitäts-Bonus, Abhängigkeiten, Risiko,
    Empfehlung, Status OFFEN); Top-5 mit Alltags-Analogien; **Meta-Learnings** (was bei der Recherche funktioniert
    hat); Sentinel-JSON mit findings_per_directive.
- **Besonderheiten:** (a) **iterative Wellen mit Selbstverbesserung der Fragen** — der research-Skill darf diese
  Wellen-Mechanik + Zwischen-Analyse NICHT verlieren (das ist der USP dieses Agenten); (b) **interdisziplinäre
  Kreativität** Pflicht; (c) Duplikat-Filter aus `superintelligenz.md`; (d) Meta-Learning-Rückschreiben; (e)
  jedes Finding MUSS einer Direktive zugeordnet sein, sonst verwerfen.

---

## Quervergleichs-Tabelle

| # | Einheit | Typ | Researcher-Anzahl | Wellen | Engine-Default heute | Persistenz-Ziel | Übergabe-Kernfelder (HIN → ZURÜCK) |
|---|---------|-----|-------------------|--------|----------------------|-----------------|-------------------------------------|
| 1 | bug-almanach-recherche | Skill | 7 (Option C), A: 2 parallel | 2 Phasen (Bug-Suche + Fix-Status), Continuous-Spawning | A (`mm-research.py`) | `bugs/<kat>/<bereich>.md` + best-practices-Rückkopplung | LIVE-Version(en), 5-7 Teilbereiche, Quellen-Rang → 6 Bug-Felder + Issue-Nrn für `gh` |
| 2 | best-practices | Skill | 7 (Option C), A: 2 parallel | 1 (alle Kategorien) | A | `best-practices/<…>.md` + Bug-Rückkopplung | Kategorie(n), Versions-Anker, last_version-Delta → Eintrag+Quelle+Datum+Flag, Bugs |
| 3 | best-practices-update | Skill | 7 (Option C) | Welle (viele Dateien) | A | dieselben best-practices-Dateien (Stand heben) | Arbeitsliste, bestehender Stand je Datei → wie #2 + Checkpoint |
| 4 | almanach-update | Skill | 7 (Option C) | Welle (7-Schritte je Almanach) | A | dieselben `bugs/`-Almanache (Stand heben) | Arbeitsliste, LIVE-Version, bestehender Stand → 6 Bug-Felder + `gh`-Issue-Nrn |
| 5 | direktiven-recherche | Skill | 5 (Skill) / 3 (Agent) | Agent-Phasen | C-Spawn via general-purpose | `DIREKTIVEN-RECHERCHE-[DATUM].md` | Direktiven-Texte, Ist-Zustand → nur NEUE Vorschläge, dt. Erklärung |
| 6 | superintelligenz | Skill | — (keine eigene Recherche) | — | nicht spezifiziert | — (Leitbild/Checkliste) | kein direkter Recherche-Übergabe-Bedarf |
| 7 | researcher | Agent | 1 (Baustein für Schwärme) | — | C (Opus-Web) | MEMORY.md (Sentinel) | enger Suchauftrag, Limits → Bullets + Quellen + BP/BUG-KANDIDATEN-Blöcke |
| 8 | forschungsagent | Agent | 1 (kein Schwarm) | — | C (Web ergänzend) | `Forschung.md` (Rückschreiben) | Forschung.md + MEMORY + Agent-Liste → Integrations-Pläne pro Paper |
| 9 | intelligence-researcher | Agent | 1 | — (5 Dimensionen) | C (Opus-Web) | MEMORY.md (Sentinel) | MEMORY-Status-Liste, 5 Dimensionen → max 8 Findings (festes Format) |
| 10 | direktiven-recherche | Agent | 3 (reduziert von 5) | 5 Phasen | C | `DIREKTIVEN-RECHERCHE-[DATUM].md` | 3 Direktiven + 500-Wort-Zusammenfassung je Direktive → 15-24 Funde, Ist-Abgleich |
| 11 | superintelligenz | Agent | Wellen-Schwarm (Sub-Agenten) | 1-3 Wellen + Zwischen-Analyse | C | `superintelligenz.md` (+ Meta-Learnings) | Dup-Liste, Lücken-Analyse, Direktiven → Findings (Direktive zugeordnet) + Top-5 + Meta |

---

## Erkenntnisse fürs Brücken-Design (Verlust-Gefahren)

1. **Versions-Anker ist die größte Verlust-Gefahr (Bug-/BP-Familie #1-4).** Diese vier liefern nur dann
   korrekten Output, wenn die LIVE ermittelte Software-Version(en) UND der bestehende Stand mitgegeben werden.
   Verliert die Brücke diese, „jagt man Geister" (falsche Fix-Status). Der research-Skill MUSS Version(en) +
   bestehenden Stand als Pflicht-Eingabefelder führen — und ZURÜCK pro Bug die 6 Felder + Issue-Nummern für die
   `gh`-Fix-Status-Prüfung (die NUR der aufrufende Hauptagent machen kann, weil `researcher` kein Bash hat).

2. **Feste Prompt-Vorlagen vs. freie Zerlegung — zwei verschiedene Welten.** #1/#4 haben eine FESTE
   Prompt-Datei (`references/researcher-prompts.md`, 5-7 Teilbereiche); #10 hat 3 feste Direktiven-Themen +
   500-Wort-Zusammenfassungen; #11 generiert Fragen selbst und VERBESSERT sie zwischen Wellen; #9 hat 5 fixe
   Dimensionen. Der research-Skill darf nicht alles auf einen Standard-Prompt einebnen — er braucht einen
   **Modus für vorgefertigte Teilbereiche** (HIN als Liste) UND einen **Modus für selbst-generierte/iterative
   Fragen** (#11). Sonst geht entweder die Vollständigkeit (#1) oder die Kreativität (#11) verloren.

3. **Output-Format ist pro Einheit hochspezifisch — am leichtesten zu verlieren.** Bug = 6 Felder; BP =
   Quelle+Datum+`offiziell`/`extern`-Flag; #8 = Integrations-Plan; #9/#11 = JA/NEIN-Empfehlung + Aufwand +
   Nicht-Programmierer-Erklärung; #10/#11 = Alltags-Analogien + Direktiven-Mapping. Die Brücke muss das
   gewünschte **Rückgabe-Schema als Parameter** entgegennehmen und durchreichen — ein generisches
   „Bullet-Points mit Quellen" (wie #7) würde #1, #8, #11 verschlechtern.

4. **Cap-Konflikt: Lossless vs. harte Caps.** Die Skills #1-4 verlangen „KEIN Eintrags-Cap" (alle Funde,
   lossless). Die Agenten haben aber harte interne Caps: `researcher` 50 Items / 100 Zeilen, `intelligence-
   researcher` 8 Findings, `superintelligenz` 10, `direktiven-recherche` 8/Researcher. Der research-Skill MUSS
   den Cap **vom aufrufenden Kontext steuerbar** machen (Almanach/BP = lossless via File-as-Memory; self-improve-
   Forscher = bewusst gedeckelt) — sonst kappt er entweder echte Bug-Funde oder flutet self-improve.

5. **Persistenz- und Memory-Mechanik unterscheidet sich stark.** Ziele: `bugs/` (#1,4), `best-practices/`
   (#2,3), `DIREKTIVEN-RECHERCHE-[DATUM].md` (#5,10), `superintelligenz.md` + Meta-Learnings (#11),
   `Forschung.md` (#8), MEMORY.md via Sentinel-JSON (#7,9). Plus Dup-Filter aus unterschiedlichen Quellen
   (superintelligenz.md, MEMORY-Status, bestehender Almanach-Stand). Die Brücke muss **Persistenz-Ziel-Pfad +
   Dup-Quelle** als Übergabefelder führen — sonst landet Wissen am falschen Ort oder Duplikate werden nicht
   gefiltert. Sonderfall #6: ist nur Leitbild, KEIN Recherche-Workflow — der research-Skill braucht ihn nicht
   abzudecken (Falle: nicht jeden „Recherche"-Namen als Workflow behandeln).
