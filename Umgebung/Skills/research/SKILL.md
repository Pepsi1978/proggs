---
name: research
description: "Zentraler Recherche-Orchestrator fuer JEDE Web-Recherche: nimmt einen strukturierten Research-Auftrag entgegen und fuehrt ihn mit sichtbaren beschrifteten parallelen Researchern (Continuous-Spawning), gepinnter Engine, Live-Zwischenfazit pro Researcher und ruhiger Auswertung aus. Nutze IMMER wenn der Benutzer 'recherchiere', 'such im Web', 'Web-Recherche', 'finde heraus', 'recherchier das' sagt ODER wenn ein anderer Skill (best-practices, bug-almanach-recherche, almanach-update, best-practices-update, direktiven-recherche, superintelligenz) bzw. Agent (researcher, forschungsagent, intelligence-researcher) Web-Recherche braucht und an diesen Skill delegiert. Erfuellt die Research-Strategie-Regel (Empfehlung + Frage 1 A/B/C/D)."
---

# research — Zentraler Recherche-Orchestrator

Dieser Skill ist die EINE ausfuehrbare Stelle fuer Web-Recherchen. Er kapselt das komplette
"WIE": Auftrag annehmen, Thema zerlegen, sichtbare parallele Researcher mit Continuous-Spawning
starten, pro Researcher sofort ein Zwischenfazit zeigen, ruhig auswerten und konkrete
Umsetz-Aufgaben ableiten. Alle anderen Research-Skills/Agenten **delegieren** hierher, damit
ihre Recherche-Arbeit konsistent und vollstaendig laeuft — ohne dass das "WIE" 8x dupliziert wird.

**Policy-Schicht (bleibt getrennt):** Die Regel `~/.claude/rules/research-strategy.md` entscheidet
das OB/WOMIT (Empfehlung + Frage 1 A/B/C/D + Eskalations-Frage 2 + Kostenkontrolle). Dieser Skill
ist die Orchestrierungs-Schicht (das WIE). Die Ausfuehrungs-Schicht sind die Skripte
`mm-research.py` / `or-research.py` bzw. der Opus-Schwarm.

---

## Block 0 — Fest eingebettete Pfade (kein Suchen!)

NIEMALS nach den Skripten/Keys suchen — sie liegen fest hier:

| Zweck | Pfad |
|-------|------|
| Firecrawl→MiniMax (Engine A) | `~/proggs/mm-research.py` |
| OpenRouter web_search (Engine B) | `~/proggs/or-research.py` |
| Approval-Flag (vom Hook erzwungen) | `$TEMP/research-approved.flag` (Windows) bzw. `$TMPDIR/research-approved.flag` |
| Firecrawl-Key | `~/SK/OpenCode/firecrawl-api-key.txt` |
| OpenRouter-Key | `~/SK/ClaudeCodeOpenRouter/openrouter.key` |
| Policy-Regel | `~/.claude/rules/research-strategy.md` |
| Rueckgabe-Schema-Vorlagen | `references/rueckgabe-schemata.md` (in diesem Skill) |

Aufruf-Konventionen (immer so, nie raten):
- **Engine A:** `python3 ~/proggs/mm-research.py "<unterthema>" [n]`
- **Engine B:** `python3 ~/proggs/or-research.py "<unterthema>" minimax/minimax-m3 parallel`
  — die Such-Engine `parallel` (= parallel.ai) wird IMMER explizit als 3. Argument gesetzt,
  damit garantiert NICHT `exa` verwendet wird. Eskalations-Modell: `z-ai/glm-5.2`.

---

## Der Research-Auftrag (die verlustfreie Bruecke)

Eine Recherche wird IMMER ueber ein **benanntes Feld-Schema** uebergeben — nie als Fliesstext.
Das verhindert "Stille-Post"-Verlust (A sagt B etwas, B sagt C etwas Verzerrtes). Wenn ein
aufrufender Skill delegiert, fuellt er diese Felder; fehlt eines, hier ERFRAGEN statt raten:

| Feld | Bedeutung | Pflicht |
|------|-----------|---------|
| `thema` | Gesamtthema, 1 Satz | ja |
| `zweck` | wozu (bug / best_practice / direktive / superintelligenz / integrationsplan / adhoc) — bestimmt das Rueckgabe-Schema | ja |
| `zerlegungs_modus` | `feste_liste` \| `selbst_generierend` \| `iterativ_wellen` | ja |
| `unterthemen[]` | exakte Teilbereiche, **je 2-3 Saetze praezise** beschrieben (das Herz gegen Verlust). Bei `selbst_generierend`/`iterativ_wellen` ganz/teilweise leer + Generierungs-Auftrag | ja* |
| `version_anker` | LIVE-Softwareversion(en) + Verweis auf bestehenden Stand | bei bug/best_practice PFLICHT |
| `engine` | `A` (mm/Firecrawl) · `B` (or/OpenRouter parallel) · `C` (Opus-Schwarm) — aus Frage 1 | ja |
| `anzahl` · `wellen` · `cap` | Researcher-Zahl, Wellen, Eintrags-Cap (Default **kein Cap**) | ja |
| `rueckgabe_schema` | welches Output-Format (siehe `references/rueckgabe-schemata.md`) | ja |
| `persistenz_ziel` | Zielpfad(e), wohin der Aufrufer das Ergebnis einarbeitet | ja |
| `dup_quelle` | woher Duplikate gefiltert werden (bestehender Almanach, MEMORY, superintelligenz.md …) | nein |
| `nacharbeit_aufrufer` | was der Aufrufer DANACH selbst tun muss (z.B. `gh` OPEN/CLOSED-Pruefung — Researcher haben kein Bash) | nein |

Wird der Skill direkt vom Benutzer aufgerufen ("recherchiere X"), fehlt das meiste — dann
`zweck=adhoc`, `rueckgabe_schema=adhoc`, und das Thema sinnvoll selbst in Unterthemen zerlegen.

---

## Ablauf

### Schritt 1 — Auftrag pruefen + Approval

1. Auftrag gegen das Schema pruefen. Fehlt ein Pflichtfeld (besonders `version_anker` bei
   bug/best_practice), beim Aufrufer/Benutzer ERFRAGEN — nie raten (Versions-Luecke = falsche
   Fix-Stati, "Geister jagen").
2. Die Policy-Regel hat vor diesem Skill bereits Empfehlung + Frage 1 (A/B/C/D) gestellt; die
   gewaehlte Engine steht im `engine`-Feld. Falls der Skill direkt ohne vorherige Frage 1
   gestartet wurde: kurz die Empfehlung + Frage 1 nach `research-strategy.md` nachholen.
3. Approval-Gate: Der Hook verlangt `$TEMP/research-approved.flag`. Ist es gesetzt (vom Frage-1-
   Schritt), weiter. **KEINE Selbsttests** "ob das System geht" — die Pipeline ist verifiziert,
   es wird direkt gearbeitet.

### Schritt 2 — Themen-Zerlegung (zwei Modi)

- **`feste_liste`** (bug-almanach-recherche, best-practices, almanach-/best-practices-update):
  Die `unterthemen[]` sind bereits fest formuliert (5-7 Teilbereiche). 1:1 uebernehmen — nichts
  umschreiben, nichts weglassen. Vollstaendigkeit ist hier wichtiger als Kreativitaet.
- **`selbst_generierend`** (intelligence-researcher u.a.): Aus `thema` selbst praezise
  Unterthemen ableiten (je 2-3 Saetze), bevor gespawnt wird.
- **`iterativ_wellen`** (superintelligenz): Erste Welle Fragen generieren, nach jeder Welle eine
  **Luecken-Analyse** ("was fehlt noch, was war widerspruechlich?") und die naechste Welle
  daraus verbessern. Kreativitaet ist hier wichtiger als starre Vollstaendigkeit.

Jedes Unterthema bekommt eine **Beschriftung** (Kurztitel) fuer die Live-Anzeige.

### Schritt 3 — Sichtbare parallele Researcher + Continuous-Spawning ⭐

**Continuous-Spawning ist die oberste Regel: sobald EIN Researcher fertig ist, SOFORT den
naechsten aus der Warteschlange starten — NIEMALS auf eine ganze Welle warten.** Es laufen
konstant so viele gleichzeitig, wie die Engine erlaubt. Kein Wellen-Barrier, kein Leerlauf.

| Engine | Max gleichzeitig | Aufruf |
|--------|------------------|--------|
| A — Firecrawl (mm) | **2** (hartes Free-Limit) | `mm-research.py` |
| B — OpenRouter (or), Such-Engine `parallel` | **2** (empirisch: mehr → kaputte Laeufe durch Last/Provider-Routing; or-research.py hat Retry als Sicherheitsnetz) | `or-research.py … parallel` |
| C — Opus-Schwarm | **7** | Agent-Tool, `subagent_type:general-purpose` + Prompt |

Praktische Umsetzung (Engine A/B = Bash-Skripte, KEIN Opus-Token-Verbrauch fuer die Quellenarbeit):
- N gleichzeitig per `run_in_background` starten (N = Engine-Limit). Jeder Lauf schreibt seine
  Rohdaten in seine Datei (mm: `~/.mm-research/`, or: `~/.or-research/answer.json`) — die landen
  NICHT im Hauptkontext.
- Sobald ein Lauf fertig ist (Notification): sein Zwischenfazit zeigen (Schritt 4) UND sofort den
  naechsten wartenden Researcher starten — bis alle Unterthemen aller Wellen durch sind.
- Engine C (Opus): genauso — 7 parallele Agent-Tool-Aufrufe; sobald einer zurueck ist, sofort den
  naechsten spawnen (Continuous-Spawning, nicht auf alle 7 warten). Crash-sicher nach
  `subagent-crash-proofing` (enger Scope, Datei-Auslagerung).

**Live-Darstellung — jeder Researcher beschriftet mit Engine/Modus + Thema:**

```
🔬 Research: "<thema>"  ·  Engine: OpenRouter/parallel  ·  Modus: Eskalation  ·  Deckel: 10 Treffer/Researcher
   Researcher 1 [OpenRouter/parallel · Eskalation] — <voller Unterthemen-Satz> … laeuft
   Researcher 2 [OpenRouter/parallel · Eskalation] — <voller Unterthemen-Satz> … ✓ fertig (8 Quellen)
   Researcher 3 [OpenRouter/parallel · Eskalation] — <voller Unterthemen-Satz> … laeuft
   [aktiv: 5 · fertig: 2/12 · ~0,04 $]
```

Die Engine wird in der Kopfzeile UND an jedem Researcher angezeigt (Soll: sichtbar womit
recherchiert wird). Bei Engine B steht dort immer `parallel` — nie `exa`.

### Schritt 4 — Zwischenfazit pro Researcher (sofort)

**Sobald ein Researcher zurueckkommt — noch bevor alle fertig sind — sofort ein Kurzfazit
(2-3 Saetze)** ausgeben: was er herausgefunden hat und was daran interessant/umsetzbar ist.
So liest der Benutzer Ergebnisse live mit, statt am Ende auf einen Block zu warten:

```
   ✓ Researcher 2 — Kurzfazit: <2-3 Saetze: Kernfund + warum interessant/umsetzbar>
```

Das Kurzfazit ist eine kompakte Lesefassung der Researcher-Antwort (nicht die Rohdaten).

### Schritt 5 — Auswertung nach jeder Stufe (ruhig, im Rueckgabe-Schema)

Wenn alle Researcher einer Stufe fertig sind: ruhige Auswertung in **festen Bloecken**, **keine
`━`-Linien, keine Farbpunkte** (Research = Neues → verstaendlich erklaeren):

```
## Kurzfassung
2-3 Saetze, was unterm Strich rauskam.

## Das Wichtigste
1. Befund — knapp + verstaendlich erklaert.
2. …

## Fuer deinen Einsatz
Was das konkret fuers Projekt bedeutet (1-3 Punkte).

## Noch offen / unsicher
Was die Quellen NICHT hergaben oder widerspruechlich war.

Quellen: 12 · Engine: OpenRouter/parallel · Kosten: 0,07 $
```

Zusaetzlich liefert der Skill das Ergebnis im **`rueckgabe_schema`** des Auftrags (siehe
`references/rueckgabe-schemata.md`) zurueck an den Aufrufer — exakt in dessen Format, damit kein
Skill verschlechtert wird. Bei vielen Funden **lossless**: in Datei auslagern + Pfad + kompakte
Summary (Funde nie kappen — der `cap` steuert nur, wie viel inline zurueckkommt). Immer mit dabei:
Quellen+Version pro Finding, der "offen/unsicher"-Block und der `nacharbeit_aufrufer`-Hinweis.

### Schritt 6 — Zwei-Stufen-Eskalation

Nach Stufe 1 (Engine A, Firecrawl) kommt die obige Auswertung. Meldet die Auswertung
"unsicher / widerspruechlich / Quellen reichen nicht" ODER der Benutzer will gruendlicher:
Eskalation gemaess Policy-Regel anbieten (Frage 2). Stufen:

```
A: MiniMax M3 auf Firecrawl-Quellen (mm)         → Standard, Free-Credits
B: 1M-Modell + OpenRouter web_search parallel (or) → pay-per-use, max 2 parallel (Last-stabil, Retry)
C: Opus-Schwarm                                    → teuer, nur bewusst gewaehlt
```

Nach JEDER Stufe wieder dieselbe ruhige Auswertung (Schritt 5) → der Benutzer entscheidet ueber
die naechste Stufe. Eskalation nie automatisch durchlaufen ohne Auswertung + Entscheidung.

### Schritt 7 — Gesamtauswertung → konkrete Umsetz-Aufgaben

Zum Schluss der Gesamtauswertung **konkrete, umsetzbare Aufgaben** ableiten — "was koennten wir
jetzt wie umsetzen, was waere sinnvoll?" — **im Kontext von Franks Projekten, hauptsaechlich dem
aktuell bearbeiteten Projekt** (zur Laufzeit ermitteln: zuletzt bearbeitete App / aktueller
Arbeitskontext). Nur bei `zweck=adhoc`/Direkt-Aufruf; bei Delegation kommt das Ergebnis im
Rueckgabe-Schema zurueck und der Aufrufer macht die fachliche Nacharbeit.

```
## Was wir jetzt umsetzen koennten (Projekt: <aktuelles Projekt>)
1. <konkrete Aufgabe> — warum sinnvoll, grober Aufwand
2. …
```

### Schritt 8 — Persistenz (research-persistence-Regel)

Taugen die Ergebnisse als Best Practices / enthalten sie Bugs/Fallen, werden sie ueber
`persistenz_ziel` in `best-practices/` (+ Bugs in `bugs/`) eingearbeitet — Kurzcheck UND Volltext
(`~/.claude/rules/research-persistence.md`). Bei Delegation macht das der aufrufende Skill mit dem
zurueckgegebenen Ergebnis.

---

## Engine-Wahl-Spickzettel (Detail in der Policy-Regel)

- **A (Firecrawl+MiniMax):** volle Seiten, tiefe Einzelrecherche; Free-Credits; **nur 2 parallel**.
- **B (OpenRouter parallel):** Snippets, **max 2 parallel** (Continuous-Spawning mit 2; mehr → kaputte
  Laeufe durch Last, or-research.py hat Retry), pay-per-use, kein Monatslimit; Such-Engine immer
  `parallel` (nie `exa`). Modell `minimax/minimax-m3` laeuft bei 2 parallel stabil.
- **C (Opus-Schwarm):** nur wenn Frank es ausdruecklich waehlt (teuer); 7 parallel, Continuous-Spawning.

---

## Was NIEMALS passieren darf

- ❌ Recherche-Auftrag als Fliesstext annehmen statt ueber das benannte Feld-Schema (Verlustgefahr)
- ❌ Bei bug/best_practice ohne `version_anker` recherchieren (falsche Fix-Stati)
- ❌ Auf ganze Wellen warten statt Continuous-Spawning (Zeitverlust — die oberste Regel)
- ❌ Mehr als 2 Firecrawl-Researcher gleichzeitig (Free-Limit → 429)
- ❌ Such-Engine `exa` statt `parallel` verwenden (parallel immer explizit als 3. Argument)
- ❌ Selbsttests "ob das System geht" — die Pipeline ist verifiziert
- ❌ Nach den Skripten/Keys suchen — die Pfade stehen in Block 0
- ❌ Funde an einem Cap abschneiden (lossless: in Datei auslagern); `cap` steuert nur Inline-Menge
- ❌ Auswertung mit Linien-Wirrwarr statt ruhiger Bloecke
- ❌ Das Rueckgabe-Schema des Aufrufers ignorieren und "generische Bullets" liefern
