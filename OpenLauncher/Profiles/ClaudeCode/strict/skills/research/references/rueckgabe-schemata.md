# Rueckgabe-Schema-Katalog

Pro `rueckgabe_schema` aus dem Research-Auftrag gibt der `research`-Skill das Ergebnis in
GENAU diesem Format an den aufrufenden Skill zurueck. So bleibt jede aufrufende Einheit
genauso gut wie vorher — kein "generische Bullets verschlechtern den Spezial-Skill".

Jede Rueckgabe enthaelt ZUSAETZLICH zu den Schema-Feldern immer:
- Quelle(n)-URL + Software-Version pro Finding
- einen `offen/unsicher`-Block (was die Quellen nicht hergaben / widerspruechlich war)
- den `nacharbeit_aufrufer`-Hinweis (was der Aufrufer noch selbst tun muss)
- bei vielen Funden: lossless in Datei ausgelagert + Pfad + kompakte Summary (nie kappen)

---

## `bug` — fuer bug-almanach-recherche, almanach-update

Pro gefundenem Bug die **6 Almanach-Felder**:

```
- Symptom: <was sichtbar schiefgeht>
- Ursache: <Root Cause>
- Versionen: <betroffene Version(en) + ab wann gefixt, falls bekannt>
- Fix: <funktionserhaltender Workaround/Fix — NIE "Feature weglassen">
- Quelle: <URL + offiziell/extern>
- Fix-Status-Kandidat: <OPEN/CLOSED-Vermutung + Issue-Nr. fuer gh-Pruefung>
```

`nacharbeit_aufrufer` (Pflicht): Die harte `gh issue view <nr>`-OPEN/CLOSED-Pruefung macht der
aufrufende Hauptagent (Researcher haben kein Bash). Pro Bug separat pruefen, ob in neuerer
Version bereits gefixt. Danach in `bugs/<kategorie>/<bereich>.md` einarbeiten (Kurzcheck + Volltext)
+ `bugs/README.md`-Index falls neuer Bereich.

---

## `best_practice` — fuer best-practices, best-practices-update

Pro Best-Practice-Eintrag:

```
- Eintrag: <die Empfehlung, konkret>
- Begruendung: <warum, kurz>
- Quelle: <URL>
- Datum: <Stand der Quelle>
- Label: <offiziell | extern>
- Versions-Anker: <fuer welche Softwareversion>
```

`nacharbeit_aufrufer`: In `best-practices/<kategorie>/<bereich>.md` einarbeiten (Kurzcheck-Tabelle
+ Volltext-Abschnitt). Gefundene Bugs/Fallen ZUSAETZLICH in den Bug-Almanach zurueckkoppeln.

---

## `direktive` — fuer direktiven-recherche (Skill + Agent)

Nur **NEUE oder verbesserbare** Vorschlaege (gegen den Ist-Zustand abgeglichen):

```
- Vorschlag: <konkrete Technik/Verbesserung>
- Direktive-Mapping: <#1 Superintelligenz | #2 Selbstbeobachtung | #3 Resilient Bugfixing>
- Ist-Zustand: <was es heute dazu schon gibt — warum der Vorschlag NEU/besser ist>
- Alltags-Analogie: <1 Satz, laienverstaendlich>
- Quelle: <URL + Paper/Doku>
```

`nacharbeit_aufrufer`: Bericht in `DIREKTIVEN-RECHERCHE-[DATUM].md`. KEINE Aenderung an den
geschuetzten Direktiven-Regeln ohne Franks Freigabe (ACE-geschuetzte Zone).

---

## `superintelligenz` — fuer superintelligenz (Agent), intelligence-researcher

```
- Finding: <Verbesserung/Durchbruch>
- Direktive-Mapping: <#1/#2/#3>
- Umsetzbar: <JA/NEIN> · Aufwand: <grob>
- Laien-Erklaerung: <1-2 Saetze>
- Quelle: <URL>
```

Am Ende: **Top-5** der staerksten Findings + ein **Meta**-Abschnitt (was die Recherche ueber das
System selbst gezeigt hat). Dup-Filter gegen `dup_quelle` (z.B. `superintelligenz.md`, MEMORY).

`nacharbeit_aufrufer`: Dup-freie Implementierungsliste in `superintelligenz.md` pflegen.

---

## `integrationsplan` — fuer forschungsagent

Pro Paper/Quelle:

```
- Paper/Quelle: <Titel + URL>
- Intelligenz-Potenzial: <hoch/mittel/niedrig + warum>
- Integrations-Plan: <konkret: neue Regel? neuer Skill? Agent-Upgrade? Wie genau?>
- Aufwand/Risiko: <grob>
```

`nacharbeit_aufrufer`: In `Forschung.md` einarbeiten.

---

## `adhoc` — fuer Direkt-Aufruf "recherchiere X" + researcher-Agent-Baustein

Die vier ruhigen Auswertungsbloecke (keine Linien):

```
## Kurzfassung
2-3 Saetze.

## Das Wichtigste
1. … 2. … 3. …

## Fuer deinen Einsatz
1-3 Punkte, projektbezogen.

## Noch offen / unsicher
…

Quellen: N · Engine: <…> · Kosten: <…>
```

Beim `researcher`-Agent als Schwarm-Baustein zusaetzlich (wie gehabt) die Bloecke
`BEST-PRACTICES-KANDIDATEN:` und `BUG-KANDIDATEN:` (oder `KEINE`), damit der Orchestrator
sie konsolidieren kann. **Kein 50-Item-Cap** mehr — bei sehr vielen Funden lossless in Datei
auslagern + Summary (siehe research-strategy.md / subagent-crash-proofing).
