---
name: research
description: "Zentraler Recherche-Orchestrator fuer JEDE Web-Recherche: nimmt einen strukturierten Research-Auftrag entgegen und fuehrt ihn mit sichtbaren beschrifteten parallelen Researchern (Continuous-Spawning), gepinnter Engine, Live-Zwischenfazit pro Researcher und ruhiger Auswertung aus. Nutze IMMER wenn der Benutzer 'recherchiere', 'such im Web', 'Web-Recherche', 'finde heraus', 'recherchier das' sagt ODER wenn ein anderer Skill (best-practices, bug-almanach-recherche, almanach-update, best-practices-update, direktiven-recherche, superintelligenz) bzw. Agent (researcher, forschungsagent, intelligence-researcher) Web-Recherche braucht und an diesen Skill delegiert. Erfuellt die Research-Strategie-Regel (Empfehlung + Frage 1 A/B/C/D). Registriert als verbindlichen LETZTEN Schritt jeden neu angelegten oder erweiterten Almanach + Best-Practices-Bereich in allen drei Almanach-Hooks (index/hint/guard)."
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
| OpenRouter `:online` (Engine B) | `~/proggs/or-research.py` |
| Continuous-Spawning-Runner (A+B, erzwingt max N parallel) | `~/proggs/research-swarm.py` |
| Approval-Flag (vom Hook erzwungen) | `$TEMP/research-approved.flag` (Windows) bzw. `$TMPDIR/research-approved.flag` |
| Firecrawl-Key | `~/SK/OpenCode/firecrawl-api-key.txt` |
| OpenRouter-Key | `~/SK/ClaudeCodeOpenRouter/openrouter.key` |
| Policy-Regel | `~/.claude/rules/research-strategy.md` |
| Rueckgabe-Schema-Vorlagen | `references/rueckgabe-schemata.md` (in diesem Skill) |

Aufruf-Konventionen (immer so, nie raten):
- **Engine A:** `python3 ~/proggs/mm-research.py "<unterthema>" [n]`
- **Engine B:** `python3 ~/proggs/or-research.py "<unterthema>" minimax/minimax-m3:online`
  — das Modell-Suffix `:online` laesst OpenRouter selbst eine Websuche dazuschalten (web-Plugin,
  Such-Engine intern = parallel.ai). **KEINE explizite Engine als 3. Argument** (kein `parallel`/
  `exa`/`firecrawl`). `:online` ist bei hoher Parallelitaet stabiler als das alte `web_search`-
  Server-Tool (A/B-getestet 2026-06-21). `reasoning:high` ist im Werkzeug eingebaut. Bei mehreren
  Parallel-Laeufen pro Lauf ein eigenes `OR_OUTDIR` setzen (sonst ueberschreiben sich die Ausgaben).
  Eskalations-Modell (mehr Denkkraft): `z-ai/glm-5.2:online`.

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
| `engine` | `A` (mm/Firecrawl) · `B` (or/OpenRouter `:online`) · `C` (Opus-Schwarm) — aus Frage 1 | ja |
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
| B — OpenRouter (or), `:online` | **7** (`:online` verteilt selbst auf mehrere Modell-Provider → last-stabil; A/B-Test 2026-06-21: 10 echt-parallel sauber. Der intermittente JSON-Tool-Call-Leak §42 wird vom `or-research.py`-Retry gefangen) | `or-research.py … minimax/minimax-m3:online` |
| C — Opus-Schwarm | **7** | Agent-Tool, `subagent_type:general-purpose` + Prompt |

Praktische Umsetzung — **Continuous-Spawning ist PFLICHT (Zeit nicht verschwenden):** sobald EIN
Researcher fertig ist, startet SOFORT der naechste, sodass konstant das Engine-Limit gleichzeitig laeuft.
NIE in Wellen warten. Bei mehr Unterthemen als dem Limit → nur Limit-viele starten, Rest nachziehen.

**Engine A + B (Bash-Skripte) → IMMER ueber `~/proggs/research-swarm.py` (erzwungene Durchsetzung):**
Eine Skill-Regel allein ist nur advisory; das Skript haelt per `ThreadPoolExecutor(max_workers=N)` KONSTANT
N parallel und zieht bei jedem fertigen Researcher SOFORT den naechsten aus der Queue — das Continuous-
Spawning ist im CODE erzwungen (deterministisch), nicht von Hand zu orchestrieren und nicht "in Wellen"
verschlechterbar. Engine-Limits hart gedeckelt: **A=2, B=7** (Ueberanforderung wird gedeckelt + gewarnt).
```bash
# Unterthemen je 1 Zeile in eine Datei, dann der Swarm (im Hintergrund starten):
python3 ~/proggs/research-swarm.py B ~/.research-swarm/themen.txt   # B = :online, konstant 7 parallel
python3 ~/proggs/research-swarm.py A ~/.research-swarm/themen.txt   # A = Firecrawl, konstant 2 parallel
```
Roh-Antworten je Researcher in `~/.research-swarm/answer-<i>.txt` (+ eigenes `run-<i>/`, kein Ueberschreiben),
NICHT im Hauptkontext. Wenn `done.flag` da ist, je Researcher ein Zwischenfazit (Schritt 4) zeigen.

**Engine C (Opus) — NICHT skriptbar (Agent-Tool-Aufrufe macht der Hauptagent), darum Pattern PFLICHT:**
Continuous-Spawning HIER von Hand, aber genauso strikt: **erst 7 Agent-Tool-Aufrufe gleichzeitig**
(`subagent_type:general-purpose` + Prompt); **sobald EINE Completion-Notification kommt, im selben Zug den
naechsten wartenden Researcher spawnen** → wieder 7 laufend. NIE auf alle 7 warten (Wellen-Barrier =
Zeitverlust). Jeder Researcher schreibt sein Ergebnis in eine eigene Datei + gibt nur eine Kurz-Summary
zurueck (kontextschonend, crash-sicher nach `subagent-crash-proofing`).
> laufen 7, einer kommt zurueck → nur noch 6 → SOFORT den 8. spawnen (wieder 7) → … bis alle durch.

**Live-Darstellung — jeder Researcher beschriftet mit Engine/Modus + Thema:**

```
🔬 Research: "<thema>"  ·  Engine: OpenRouter/:online  ·  Modus: Eskalation  ·  Deckel: 10 Treffer/Researcher
   Researcher 1 [OpenRouter/:online · Eskalation] — <voller Unterthemen-Satz> … laeuft
   Researcher 2 [OpenRouter/:online · Eskalation] — <voller Unterthemen-Satz> … ✓ fertig (8 Quellen)
   Researcher 3 [OpenRouter/:online · Eskalation] — <voller Unterthemen-Satz> … laeuft
   [aktiv: 7 · fertig: 2/12 · ~0,07 $]
```

Die Engine wird in der Kopfzeile UND an jedem Researcher angezeigt (Soll: sichtbar womit
recherchiert wird). Bei Engine B steht dort immer `:online` — keine explizite Such-Engine.

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

Quellen: 12 · Engine: OpenRouter/:online · Kosten: 0,07 $
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
B: MiniMax M3 :online (or, OpenRouter Go)          → pay-per-use, bis 7 parallel (last-stabil + Retry)
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

### Schritt 9 — Hook-Registrierung (PFLICHT, der allerletzte Schritt) ⭐

**Immer wenn Schritt 8 einen Almanach-Bereich (`bugs/<kategorie>/<bereich>.md`) und/oder seine
Best-Practices-Gegenseite NEU angelegt oder erweitert hat, wird als ALLERLETZTER Schritt der neue
Bereich in allen DREI Almanach-Hooks registriert.** Das ist nicht optional — ohne diesen Schritt
ist die Recherche-Pipeline unvollstaendig: der Almanach laege als totes Wissen im Repo, ohne dass
ein Hook ihn einblendet, im Prompt erkennt oder passende Edits absichert (kein Compound-Effekt).

Kurz die drei Hooks (volle Anleitung + Tests + Spiegelung: **`references/hook-registrierung.md`**):

1. **`bug-almanac-index`** (SessionStart) — listet **rekursiv** alle Almanache → **automatisch**,
   nur verifizieren, dass die Datei in `bugs/<kategorie>/` liegt. Keine Code-Aenderung.
2. **`bug-almanac-hint.py`** (UserPromptSubmit) — kuratiertes `AREAS`-Dict: **Eintrag ergaenzen**
   mit Synonym-Stichwoertern, je in **Leerzeichen- UND Bindestrich-Variante** (Substring-Matching;
   deutsche Eingaben nutzen oft Bindestriche). `py_compile` + Positiv-Tests (beide Schreibweisen) +
   ein leerer Negativ-Test. Kollidierende Stichwoerter fremder Bereiche vermeiden.
3. **`bug-almanac-guard`** (PreToolUse) — nur erweitern, wenn der Bereich ein **klares Datei-Muster**
   hat; Konzept-/Querschnitts-Bereiche (wie `agents/…`) bewusst NICHT erzwingen, nur dokumentieren.

Danach JEDE geaenderte Hook-Datei in **beide** Spiegel-Orte (`claude-code-setup/hooks` UND
`Umgebung/Hooks`) 1:1 spiegeln (harness-mirror-Pflicht; bei `.ps1`/`.sh` beide Varianten; kein
`__pycache__`), nur eigene Dateien namentlich stagen, committen, fetch+rebase, pushen.

**Wer registriert:** wer in Schritt 8 persistiert hat. Bei `adhoc`/Direkt-Aufruf macht es dieser
Skill selbst; bei Delegation der aufrufende Skill mit dem zurueckgegebenen Ergebnis. So oder so ist
die Hook-Registrierung der verbindliche Abschluss der gesamten Recherche→Persistenz-Pipeline.

> Hinweis: Hooks editieren ist Harness-Arbeit — der `bug-almanac-guard` verlangt vorher den
> Hooks-Almanach (Stufe C, Volltext) + Best-Practices. Details in `references/hook-registrierung.md`.

---

## Engine-Wahl-Spickzettel (Detail in der Policy-Regel)

- **A (Firecrawl+MiniMax):** volle Seiten, tiefe Einzelrecherche; Free-Credits; **nur 2 parallel**.
- **B (OpenRouter `:online`):** Snippets, **bis 7 parallel** (Continuous-Spawning; `:online` verteilt
  selbst auf mehrere Modell-Provider → last-stabil, A/B-Test 2026-06-21: 10 echt-parallel sauber;
  `or-research.py`-Retry faengt den intermittenten Leak §42), pay-per-use, kein Monatslimit. Modell
  `minimax/minimax-m3:online` — KEINE explizite Such-Engine angeben. Eskalation: `z-ai/glm-5.2:online`.
- **C (Opus-Schwarm):** nur wenn Frank es ausdruecklich waehlt (teuer); 7 parallel, Continuous-Spawning.

---

## Was NIEMALS passieren darf

- ❌ Recherche-Auftrag als Fliesstext annehmen statt ueber das benannte Feld-Schema (Verlustgefahr)
- ❌ Bei bug/best_practice ohne `version_anker` recherchieren (falsche Fix-Stati)
- ❌ Auf ganze Wellen warten statt Continuous-Spawning (Zeitverlust — die oberste Regel)
- ❌ Mehr als 2 Firecrawl-Researcher gleichzeitig (Free-Limit → 429)
- ❌ Bei Engine B eine explizite Such-Engine (`parallel`/`exa`/`firecrawl`) als 3. Argument angeben — `:online` regelt die Suche selbst (Modell-Suffix, kein `tools`-Block)
- ❌ Mehrere Engine-B-Parallel-Laeufe ohne eigenes `OR_OUTDIR` je Lauf (sie ueberschreiben sich)
- ❌ Selbsttests "ob das System geht" — die Pipeline ist verifiziert
- ❌ Nach den Skripten/Keys suchen — die Pfade stehen in Block 0
- ❌ Funde an einem Cap abschneiden (lossless: in Datei auslagern); `cap` steuert nur Inline-Menge
- ❌ Auswertung mit Linien-Wirrwarr statt ruhiger Bloecke
- ❌ Das Rueckgabe-Schema des Aufrufers ignorieren und "generische Bullets" liefern
- ❌ Einen neu angelegten/erweiterten Almanach-Bereich persistieren, ohne ihn in den drei Almanach-Hooks zu registrieren (Schritt 9) — totes Wissen im Repo, kein Compound-Effekt
- ❌ Beim `hint`-Eintrag nur Leerzeichen-Schreibweisen aufnehmen (Bindestrich-Varianten fehlen → deutsche Eingaben triggern nicht) oder die Hook-Aenderung nicht in beide Spiegel-Orte spiegeln
