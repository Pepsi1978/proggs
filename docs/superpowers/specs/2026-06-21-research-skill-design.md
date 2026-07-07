# Design-Spec: Zentraler `research`-Skill + verlustfreie Brücke

**Datum:** 2026-06-21
**Status:** Design freigegeben (Brainstorming), bereit für Implementierung via skill-creator
**Ist-Aufnahme:** `docs/superpowers/research-skill-ist-aufnahme.md` (Profile aller 11 Research-Einheiten)

---

## 1. Ziel

Das Research-System so umbauen, dass es FÜR FRANK rund läuft: sichtbare beschriftete
Parallel-Researcher mit Live-Fortschritt, gepinnte+angezeigte Engine, Kostendeckel, ruhige
verständliche Auswertung, Tempo durch Continuous-Spawning, ein zentraler getesteter Skill.
Der neue `research`-Skill kapselt das „WIE"; die anderen Research-Skills delegieren an ihn,
ohne dass ihre bisherige Arbeit schlechter wird.

Auslöser: Die erste echte Research in einer frischen Session lief „überhaupt nicht rund"
(8 Soll-Fixes, siehe §10). Diagnose: Die heutige „Strategie" ist eine **passive Regel** —
der Hauptagent muss jedes Mal selbst korrekt orchestrieren. Verhalten garantiert man
zuverlässiger mit einem **ausführbaren, getesteten Skill**.

---

## 2. Architektur — drei Schichten

```
POLICY (bleibt)        Regel research-strategy.md
                       → Empfehlung + Frage 1 (A/B/C/D) + Frage 2 (Eskalation)
                       → Kostenkontrolle, Approval-Hook-Gate, Continuous-Spawning-Regel
                              │ ruft
ORCHESTRIERUNG (NEU)   ~/.claude/skills/research/SKILL.md
                       → Research-Auftrag annehmen (festes Schema, §3)
                       → Themen-Zerlegung (2 Modi, §4)
                       → sichtbare parallele Researcher + Continuous-Spawning (§5)
                       → Zwischenfazit pro Researcher (§6)
                       → ruhige Auswertung nach jeder Stufe (§7) + Umsetz-Aufgaben (§8)
                              │ ruft (Bash, KEIN Opus-Token-Verbrauch für Quellenarbeit)
AUSFÜHRUNG (bleibt)    mm-research.py (Firecrawl→MiniMax)  ·  or-research.py (OpenRouter)
                       Opus-Researcher-Schwarm (Option C, nur bewusst)
```

Die Regel `research-strategy.md` §5 wird von „kein neuer Skill" auf „delegiert an den
`research`-Skill" umgeschrieben.

---

## 3. Die verlustfreie Brücke — der Research-Auftrag (KERN)

Übergabe ist **kein Fließtext** (verhindert „A→B→C-Stille-Post"), sondern ein **benanntes
Feld-Schema**. Jeder aufrufende Skill füllt die Felder; der `research`-Skill reicht sie 1:1
an die Researcher und gibt feste Felder zurück.

### Eingabe (Aufrufer → research-Skill)

| Feld | Bedeutung |
|------|-----------|
| `thema` | Gesamtthema, 1 Satz |
| `zweck` | wozu (Bug-Almanach / Best-Practice / Direktive / Ad-hoc / Superintelligenz) — bestimmt Rückgabe-Format |
| `zerlegungs_modus` | `feste_liste` \| `selbst_generierend` \| `iterativ_wellen` |
| `unterthemen[]` | exakte Teilbereiche, **je 2–3 Sätze präzise** beschrieben (das Herz gegen Stille-Post); bei `selbst_generierend`/`iterativ` leer/teilbefüllt + Generierungs-Auftrag |
| `version_anker` | LIVE-Software-Version(en) + Verweis auf bestehenden Stand (Pflicht für Bug/BP; sonst leer) |
| `engine` | `A` (mm/Firecrawl) · `B` (or/OpenRouter, Such-Engine=`parallel`) · `C` (Opus-Schwarm) — aus Frage 1 |
| `anzahl` · `wellen` · `cap` | vom Aufrufer gesetzt; Default **kein Cap** (Bug/BP) |
| `rueckgabe_schema` | welches Output-Format genau (siehe Schema-Katalog unten) |
| `persistenz_ziel` | Zielpfad(e), wohin das Ergebnis eingearbeitet wird |
| `dup_quelle` | woher Duplikate gefiltert werden (z.B. bestehender Almanach, MEMORY, superintelligenz.md) |
| `nacharbeit_aufrufer` | was der Aufrufer DANACH selbst tun muss (z.B. `gh` OPEN/CLOSED-Prüfung — Researcher haben kein Bash) |

### Rückgabe-Schema-Katalog (research-Skill → Aufrufer)

Der Skill hat pro `rueckgabe_schema` eine **feste Vorlage**, damit kein Skill verschlechtert wird:

| Schema | Felder |
|--------|--------|
| `bug` | 6 Bug-Felder (Symptom, Ursache, Versionen, funktionserhaltender Fix, Quelle, Fix-Status-Kandidat) + Issue-Nrn für `gh`-Prüfung |
| `best_practice` | Eintrag + Quelle-URL + Datum + `offiziell`/`extern`-Flag + Versions-Anker |
| `direktive` | nur NEUE/verbesserbare Vorschläge + Mapping auf Direktive #1/#2/#3 + Alltags-Analogie |
| `superintelligenz` | Findings (Direktive zugeordnet) + Top-5 + Meta + JA/NEIN-Umsetzbarkeit + Aufwand + Laien-Erklärung |
| `integrationsplan` | pro Paper/Quelle: Integrations-Plan (neue Regel/Skill/Agent-Upgrade) |
| `adhoc` | die 4 Auswertungsblöcke (§7) |

Jede Rückgabe zusätzlich: Quellen+Version pro Finding, **lossless** (bei vielen Funden in Datei
ausgelagert + Pfad + kompakte Summary), „offen/unsicher"-Block, „Nacharbeit-Hinweis".

---

## 4. Themen-Zerlegung — zwei Modi (beide Welten)

- **`feste_liste`** (Bug-Almanach, Best-Practices, almanach-/best-practices-update): Der Aufrufer
  übergibt 5–7 fest formulierte Teilbereiche (analog `bug-almanach-recherche/references/researcher-prompts.md`).
  Der Skill reicht sie 1:1 weiter — Vollständigkeit garantiert.
- **`selbst_generierend` / `iterativ_wellen`** (superintelligenz, intelligence-researcher): Der Skill
  generiert Fragen selbst und **verbessert sie zwischen den Wellen** (Lücken-Analyse) — Kreativität garantiert.

---

## 5. Sichtbare Researcher + Continuous-Spawning (Soll-Fix #1, #5)

### Live-Darstellung — jeder Researcher beschriftet mit Engine/Modus + Thema

```
🔬 Research: "<thema>"  ·  Engine: OpenRouter/parallel  ·  Modus: Eskalation  ·  Deckel: 10 Treffer/Researcher
   Researcher 1 [OpenRouter/parallel · Eskalation] — <voller Unterthemen-Satz> … läuft
   Researcher 2 [OpenRouter/parallel · Eskalation] — <voller Unterthemen-Satz> … ✓ fertig (8 Quellen)
   Researcher 3 [OpenRouter/parallel · Eskalation] — <voller Unterthemen-Satz> … läuft
   [aktiv: 5 · fertig: 1/12 · ~0,03 $]
```

### Continuous-Spawning — die OBERSTE Regel (Soll-Fix #3, #5, #8)

**Sobald EIN Researcher fertig ist, wird SOFORT der nächste gestartet — nie auf ganze Wellen
warten.** Es laufen konstant so viele gleichzeitig wie die Engine erlaubt:

| Engine | Max gleichzeitig | Quelle |
|--------|------------------|--------|
| A — Firecrawl (mm) | **2** (hartes Free-Limit) | Firecrawl 2 concurrent |
| B — OpenRouter (or), Such-Engine `parallel` | **5–7** | Paid = keine harten OR-Limits, nur Cloudflare-DDoS |
| C — Opus-Schwarm | **7** | RPM-empirisch (agent-and-researcher-rules) |

Ablauf (gilt für ALLE drei): N starten → einer fertig → sofort den nächsten aus der Warteschlange
spawnen → bis alle Unterthemen + alle Wellen durch sind. Kein Wellen-Barrier, kein Leerlauf.

### Engine-Pinning (Soll-Fix #2)

`or-research.py` wird IMMER mit explizitem `parallel` als 3. Argument aufgerufen (nicht nur per
Default), damit die Such-Engine garantiert `parallel.ai` ist, nicht `exa`. Die Statuszeile zeigt
die Engine an.

---

## 6. Zwischenfazit pro Researcher (Soll-Fix #6)

**Sobald ein Researcher zurückkommt — sofort, noch bevor alle fertig sind** — gibt der Skill ein
kurzes Kurzfazit (2–3 Sätze) aus: was Interessantes/Spannendes gefunden wurde, was evtl. umsetzbar
ist. So liest Frank Ergebnisse live mit:

```
   ✓ Researcher 2 — Kurzfazit: <2–3 Sätze: was rausgefunden, was daran interessant/umsetzbar ist>
```

---

## 7. Ruhige Auswertung nach JEDER Stufe (Soll-Fix #4)

Feste, ruhige Blöcke — **keine `━`-Linien, keine Farbpunkte** im Auswertungsblock, nur Überschriften:

```
## Kurzfassung
2–3 Sätze, was unterm Strich rauskam.

## Das Wichtigste
1. Befund — knapp + verständlich erklärt (Research = Neues → erklären).
2. …

## Für deinen Einsatz
Was das konkret fürs Projekt bedeutet (1–3 Punkte).

## Noch offen / unsicher
Was die Quellen NICHT hergaben oder widersprüchlich war.

Quellen: 12 · Engine: OpenRouter/parallel · Kosten: 0,07 $
```

Diese Auswertung kommt nach Stufe 1 → Frank entscheidet über Eskalation (Stufe 2) → danach
wieder dieselbe Auswertung (Zwei-Stufen, Soll-Fix #8).

---

## 8. Gesamtauswertung → konkrete Umsetz-Aufgaben (Soll-Fix #7)

Am Schluss der Gesamtauswertung leitet der Skill **konkrete, umsetzbare Aufgaben** ab: „Was
könnten wir jetzt wie umsetzen, was wäre sinnvoll?" — **im Kontext von Franks Projekten,
hauptsächlich dem aktuell bearbeiteten Projekt**. Das aktuelle Projekt wird zur Laufzeit ermittelt
(zuletzt bearbeitete App / aktueller Arbeitskontext).

```
## Was wir jetzt umsetzen könnten (Projekt: <aktuelles Projekt>)
1. <konkrete Aufgabe> — warum sinnvoll, grober Aufwand
2. …
```

---

## 9. Pro-Skill-Abstimmung (volle Beschreibungen)

Wie jeder Skill den `research`-Skill aufruft. Volle Zeilen statt Stichworte:

| Skill/Agent | Was genau recherchiert wird | Modus | Engine | Anzahl/Spawning | Rückgabe |
|-------------|-----------------------------|-------|--------|-----------------|----------|
| bug-almanach-recherche | Vollständige bekannte Bugs/Fallen/Workarounds eines Technik-Bereichs in der LIVE-Version, plus separat ob jeder Bug in neueren Versionen schon gefixt ist | feste_liste | A→C | 7, Continuous, kein Cap, mehrere Wellen | `bug` |
| almanach-update | Aktualisierung bestehender Almanache gegen die aktuelle Software-Version: neue Bugs, geänderte Fix-Stände, veraltete Einträge je Almanach-Datei | feste_liste | A→C | 7, Continuous, kein Cap | `bug` |
| best-practices | Aktuelle Best-Practices für Harness und Projekt-Sprachen gegen offizielle Changelogs/Docs, plus Rückkopplung gefundener Fallen in den Bug-Almanach | feste_liste | A→C | 7, Continuous, kein Cap | `best_practice` |
| best-practices-update | Aktualisierung bestehender Best-Practices-Dateien gegen neue Versionen, Datei für Datei mit Checkpoint | feste_liste | A→C | 7, Continuous, kein Cap | `best_practice` |
| direktiven-recherche | Neue Techniken/Papers zur besseren Umsetzung der 3 Direktiven, abgeglichen gegen Ist-Zustand, nur NEUE/verbesserbare Vorschläge | feste_liste | C | **5**, Continuous | `direktive` |
| superintelligenz | Kreative, iterative Suche nach grundlegenden System-Verbesserungen über mehrere Wellen mit Selbstverbesserung der Fragen zwischen den Wellen | iterativ_wellen | C | Wellen-Schwarm, Continuous + Zwischen-Analyse | `superintelligenz` |
| researcher (Agent) | Enger Einzel-Suchauftrag als Schwarm-Baustein; liefert Bullets + BP/BUG-KANDIDATEN-Blöcke | (Baustein) | A/B/C | aufrufer-gesteuert, kein fester Cap | aufrufer-gesteuert |
| forschungsagent (Agent) | Bewertet Forschung.md-Paper auf Intelligenz-Potenzial + Web-Ergänzung, erstellt Integrations-Pläne | feste_liste | C | aufrufer-gesteuert | `integrationsplan` |
| intelligence-researcher (Agent) | Sucht in 5 Dimensionen nach Reasoning-/Selbstverbesserungs-Durchbrüchen, mit Gedächtnis gegen Dubletten | feste_liste | C | 5 Dimensionen, Continuous | `superintelligenz` |

> **Falle:** `superintelligenz` **Skill** (≠ Agent) ist NUR Leitbild/Checkliste, KEIN Recherche-Workflow
> → bekommt KEINEN Übergabe-Block.

---

## 10. Die 8 Soll-Fixes als Akzeptanzkriterien

1. ✅ Sichtbare parallele Researcher, beschriftet mit Engine/Modus + vollem Unterthema, Live-Fortschritt (§5)
2. ✅ Engine gepinnt + angezeigt (`parallel`, nicht Exa) (§5)
3. ✅ Kostendeckel pro Researcher (`OR_MAX_TOTAL`), laufende Kostenanzeige, kein Doppelpreis (§5)
4. ✅ Ruhige Auswertungsblöcke, keine Linien-Wirrwarr (§7)
5. ✅ Tempo durch Continuous-Spawning statt Wellen-Warten (§5)
6. ✅ Keine Selbsttests des Systems — direkt arbeiten (System ist verifiziert)
7. ✅ Pfade fest im Skill eingebettet (mm/or-research.py, Flag, Keys) — kein Suchen
8. ✅ Zwei-Stufen mit Auswertung + Entscheidung nach jeder Stufe (§7), plus Zwischenfazit (§6) und Umsetz-Aufgaben (§8)

---

## 11. Systemweite Regel-Änderungen

- **research-strategy.md:** §5 auf Delegation umschreiben; Continuous-Spawning als oberste
  Researcher-Regel ergänzen (Firecrawl 2, OpenRouter 5–7, Opus 7 — einer fertig → sofort nächster).
- **agent-and-researcher-rules.md:** Continuous-Spawning-Abschnitt verstärken: gilt für ALLE
  Engines, nie auf ganze Wellen warten, „so schnell wie möglich neue Researcher starten" als
  oberste Regel.
- **50-Item-Cap** im `researcher`-Agent entfernen (lossless, 1M-Kontext). **Memory-Notiz:** beobachten,
  ob Researcher ohne Cap bei OpenRouter (neue Plattform, Limits unbekannt) scheitern → ggf. wieder einbauen.

---

## 12. Delegations-Mechanik (Übergabe-Block)

Jeder aufrufende Skill ersetzt seinen heutigen Recherche-Abschnitt durch EINEN standardisierten Block:

> „Für ALLE Web-Recherchen jetzt den `research`-Skill laden und ihm diesen Research-Auftrag
> übergeben: [thema, zweck, zerlegungs_modus, unterthemen[], version_anker, engine, anzahl/wellen/cap,
> rueckgabe_schema, persistenz_ziel, dup_quelle, nacharbeit_aufrufer]. Mit dem zurückgegebenen
> Ergebnis im rueckgabe_schema hier weiterarbeiten."

So lebt das „WIE" an genau einem Ort; die Spezial-Skills behalten nur ihr fachliches „WAS".

---

## 13. Bau & Direktiven-Konformität

- **Bau via `skill-creator`** (CLAUDE.md-Pflicht für Skills).
- **Pfade fest eingebettet:** `~/proggs/mm-research.py`, `~/proggs/or-research.py`,
  Approval-Flag `$TEMP/research-approved.flag`, Keys (`~/SK/OpenCode/firecrawl-api-key.txt`,
  `~/SK/ClaudeCodeOpenRouter/openrouter.key`).
- **Direktive #1 (Superintelligenz):** EINE getestete Orchestrierung = Harness-Verbesserung, kein Duplikat.
- **Direktive #2 (Selbstbeobachtung):** Zwischenfazit + Umsetz-Aufgaben machen Erkenntnisse sofort sichtbar.
- **Direktive #3 (Resilient Bugfixing):** verlustfreie Brücke + kein Cap = funktionserhaltend; Cap-Beobachtung als Poka-Yoke-Stufe-1.
- **research-persistence:** Ergebnisse werden über `persistenz_ziel` in best-practices/ + bugs/ eingearbeitet.
- **Spiegelung:** research-Skill nach claude-code-setup/ + Umgebung/Skills/ (harness-mirror-on-change).

---

## 14. Offene Beobachtungspunkte

- OpenRouter-Concurrency in der Praxis: hält 5–7 gleichzeitige `web_search`-Requests stabil? (Cloudflare-DDoS)
- Ohne 50-Cap: scheitern Researcher bei sehr großen Ergebnismengen über OpenRouter? (Memory-Notiz)
- Such-Engine `parallel.ai` vs. `exa` Qualität über Zeit (parallel ist gepinnt, aber beobachten).
