# Verlustfrei-Prinzip: Kontext reduzieren OHNE Funktionalitaet zu verlieren (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-05-31. Gilt systemweit fuer jede
> Kontext-Optimierung — beim Hauptagenten wie bei jedem Subagent/Worker.
> Gehoert eng zu Direktive #3 (`resilient-bugfixing.md`, Funktionalitaets-Erhaltungspflicht).

---

## Grundsatz

Kontext darf reduziert werden, um Abstuerze zu verhindern und die Qualitaet zu steigern —
aber **NIEMALS auf Kosten von Funktionalitaet**. Es gibt zwei Arten der Reduktion. Nur eine
ist erlaubt.

| | **Verlustbehaftet (lossy) — VERBOTEN** | **Verlustfrei (lossless) — ERLAUBT** |
|---|----------------------------------------|--------------------------------------|
| Prinzip | Information **wegwerfen** | Information **auslagern**, bleibt erreichbar |
| Beispiele | Truncation, blindes `head_limit` das Treffer kappt, gebrauchte Regel weglassen, Feature entfernen | File-as-Memory (per Pfad nachladbar), progressive disclosure (Tool voll da, Schema on-demand), path-scoped rules (Regel laedt bei passenden Dateien), just-in-time retrieval, count→gezielt-content |
| Folge | bricht Multi-Step-Reasoning, Capability sinkt | Capability bleibt **100%** — jeder Fakt per `read(pfad)` wiederholbar |

**Merksatz:** Wir entfernen kein Wissen und keine Faehigkeit — wir verlagern Ballast aus dem
Hot-Context an Orte, von denen er jederzeit zurueckgeholt werden kann.

## Warum weniger Ballast sogar BESSER ist (Context Rot)

Mehr Tokens = schlechtere Genauigkeit, lange bevor das Limit erreicht ist (Chroma 2026:
18 Modelle, alle degradieren; Stanford "lost in the middle": 30%+ Drop in der Mitte;
GPT-4: 98,1%→64,1% nur durch Struktur). Ein Agent mit Regel-Ballast + Datei-Dumps arbeitet
nachweislich **ungenauer**. Lossless-Reduktion macht Agenten also nicht nur crash-sicher,
sondern qualitativ besser. Das entkraeftet die Sorge "weniger Kontext = schlechter" — das
Gegenteil stimmt, solange verlustfrei reduziert wird.

## Pflicht-Garantie bei JEDER Reduktion (Funktionalitaets-Erhalt beweisen)

Quelle: Anthropic "Demystifying evals for AI agents". Vor und nach jeder Kontext-Optimierung:

1. **Baseline (vorher):** Festhalten was der Agent/die Pipeline aktuell leistet (welche
   Findings, welche Tiefe, welches Verhalten).
2. **Aenderung:** Nur verlustfrei (siehe Tabelle).
3. **Regressionstest (nachher):** Identischen Lauf wiederholen, gegen Baseline vergleichen.
   **Near-100% der vorherigen Leistung muss erhalten bleiben.** Jeder Punkt, den der Agent
   vorher konnte und jetzt nicht mehr → Regression → Aenderung **zuruecknehmen oder nachbessern**.
4. Erst wenn Capability == Baseline UND das Absturzproblem geloest: Aenderung gilt als bestanden.

## Was NIEMALS passieren darf
- Kontext reduzieren durch Wegwerfen/Kappen statt Auslagern (lossy)
- Eine Optimierung behalten, ohne den Regressionstest gegen die Baseline bestanden zu haben
- "Spart Tokens" als Rechtfertigung fuer Capability-Verlust akzeptieren
- Eine verhaltenskritische Regel weglassen/scopen, sodass sie fehlt wenn sie gebraucht wird

Siehe [[resilient-bugfixing]] (Funktionalitaets-Erhaltungspflicht) und [[subagent-crash-proofing]]
(wie man Agenten verlustfrei absturzsicher baut).
