# Gedächtnis & Bericht — kompoundierendes Lernen ohne Wissensmüll

> Hier wird festgelegt, WO und WIE der Skill Gelerntes ablegt (Phase 5) und wie er berichtet (Phase 7).
> Kein interner Score. Das Gedächtnis ist score-frei organisiert: als Wissen, das *funktioniert*,
> nicht als Punktestand.

---

## Die drei Gedächtnis-Ebenen (gegen Context Rot, RI-3)

Mehr Kontext macht dümmer. Darum gilt: **nur Relevantes in den Hot-Context, alles andere abrufbar
auslagern** (Index zeigt auf Evidenz, statt Evidenz mitzuschleppen).

| Ebene | Datei(en) | Inhalt | Regel |
|-------|-----------|--------|-------|
| **Index (Hot)** | `.claude/agent-memory/shared/MEMORY.md` | kompakte Zeiger + 1–2-Satz-Lektionen, nach Thema | schlank halten; alte erledigte Einträge ins Archiv; nie rohe Logs |
| **Evidenz (Kalt)** | `bug-cases.jsonl`, `experience-store.jsonl`, `feedback_*.md` | volle Details pro Fall | NIE komplett laden; gezielt abrufen (grep/tail/gezielte Suche) |
| **Können (Semantisch)** | `~/.claude/rules/`, Skill-Library (siehe unten) | destillierte, allgemeine Regeln & wiederverwendbare Muster | wächst pro Lauf, bleibt schlank |

**Abruf statt Mitschleppen:** Beim Hinterfragen eines Subjekts zuerst den Index lesen; nur bei Treffer
den vollen Fall aus der Evidenz-Ebene dereferenzieren. So bläht sich MEMORY.md nie zur Mülldeponie auf.

## Episodisch → Semantisch (Phase 5, die Sleep-Agent-Rolle)

Aus "heute ist X passiert" (episodisch) wird "wenn Y, dann Z" (semantisch):
1. Nimm die rohen Erlebnisse des Laufs (was hinterfragt, was gefunden, was geprüft).
2. Verdichte sie zu **allgemeinen** Aussagen — eine Lektion, die über den Einzelfall hinaus gilt (Achse 2 + 6).
3. Lege sie am richtigen Ort ab (Tabelle oben). Eine Lektion, die nur einmal galt, ist kein semantisches Wissen — verwirf sie oder halte sie als Near-Miss.

## Die Skill-Library (Voyager-Prinzip)

Bewährte, **wiederverwendbare Lösungsmuster** — kombinierbar zu neuen Mustern. Ort: Sektion
"Bewährte Loesungsmuster" in MEMORY.md (Pheromon-Tabelle).
- Eintrag nur, wenn das Muster **mindestens einmal real funktioniert hat** (Achse 3) — keine ungeprüften Ideen.
- Kombinierbar: "Hook-Debug" + "Exit-Code-Prüfung" → "Robuster Hook-Fix". So wächst Können exponentiell, nicht linear.
- Schlank: max ~20 Einträge; ältestes schwaches Muster raus, wenn voll. Near-Miss-Muster bevorzugt behalten.

## Das Intelligenz-Journal (Trittsteine, score-frei)

Ein wachsendes Protokoll der **adoptierten UND verworfenen** Alternativen — damit kein Lauf dieselbe
Sackgasse zweimal erforscht und damit gute Zwischenschritte erhalten bleiben (DGM-Archiv-Idee, ohne Fitness-Zahl).
Ort: Sektion "Forschung & Intelligence" in MEMORY.md. Pro Eintrag:
- Datum · Subjekt (welche Handlungsweise) · aktueller Weg → erwogene Alternative · Entscheidung (adoptiert / verworfen / später) · **kurze Begründung an den Achsen** · ggf. Quelle.
- Verworfene Alternativen mit Grund sind genauso wertvoll wie adoptierte — sie sparen künftigen Läufen die Sackgasse.

## Gedächtnis schlank halten (Pruning)

- **Decay nach Nutzung, nicht nach Alter allein:** Einträge, die lange nie abgerufen/relevant waren, fliegen beim Pruning zuerst.
- **Near-Miss behalten:** Beinahe-Fehler sind selten, aber hochwertig — nie als erstes löschen.
- **Archivieren statt löschen (Verlustfreiheit, Achse 5):** erledigte Einträge > 30 Tage in einen `<!-- ARCHIV (Datum) -->`-Block am Sektionsanfang verschieben, nicht wegwerfen.
- **JSONL nur appenden, nie automatisch umschreiben.** Success-/Status-Felder nie fälschen.

---

## Bericht-Format (Phase 7)

Für Nicht-Programmierer verständlich, einfaches Deutsch, Analogien wo nötig. Aufbau:

```markdown
## Self-Improve Bericht — [Datum] ([Plattform])

**Was dieser Lauf intelligenter gemacht hat:**
[2–4 Sätze in Alltagssprache: welche Handlungsweise wurde hinterfragt, welcher intelligentere Weg
wurde gefunden und an der Wirklichkeit bestätigt.]

**Umgesetzt & an der Wirklichkeit geprüft:**
| Was (alter Weg → intelligenterer Weg) | Intelligenter weil (Achse) | Real geprüft? |

**Erforscht (auch ohne Anlass):**
| Thema | Gefundene Alternative | Quelle | Übernommen? (mit Grund) |

**Reflexion über die Definition selbst:**
[1–3 Sätze: neue Regel der Intelligenz entdeckt? Schwäche einer Achse? Oder: Definition hielt stand.]
```

## Entscheidungsliste (Pflicht am Ende — für Großes/Riskantes)

Alles, was NICHT selbst umgesetzt wurde (Direktiven, der Skill selbst, Löschungen, große Umbauten),
kommt hierhin — als klare Ja/Nein-Liste, damit Frank nur "Mach A2, B1" sagen muss.

```markdown
## Entscheidungsliste — Was soll umgesetzt werden?

### [Thema, z.B. Intelligenz-Mechanismen / Sicherheit / Plattform]
| # | Was wird intelligenter? | Warum wichtig (Problem, Alltagssprache, ≥2 Sätze) | Was genau passiert (≥2 Sätze) | Aufwand | Empfehlung | Ja/Nein |
|---|------------------------|---------------------------------------------------|-------------------------------|---------|------------|---------|
```

Regeln: jeder Fund landet in genau einer Zeile · Titel ohne Fachjargon (Begriffe wie "Hook" in
Klammern erklären) · Empfehlung immer JA oder NEIN mit kurzem Grund · keine reinen Info-Zeilen ohne
Handlungsoption · breite Spalten für 2–3 Sätze, nicht auf Stichworte kürzen.

## Abschluss

- Sichere Verbesserungen sind umgesetzt; Großes liegt als Entscheidungsliste vor.
- Cross-Platform-Sync + Commit/Push für alle eigenen Änderungen ([altlasten.md](altlasten.md) → A2, A6).
- Status-Meldung als letzter Satz ("Committed, gepusht und plattformübergreifend." nur wenn wirklich beides erledigt).
- Falls Shell-Updates anstehen: ganz zuletzt, nur nach Bestätigung ([altlasten.md](altlasten.md) → A3).
