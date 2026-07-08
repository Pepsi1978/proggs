# Verlustfrei-Prinzip: Kontext reduzieren OHNE Funktionalitaet zu verlieren (KRITISCH)

> Systemweit fuer jede Kontext-Optimierung. Gehoert zu Direktive #3 (Funktionalitaets-Erhaltungspflicht).

## Grundsatz

Kontext darf reduziert werden (gegen Abstuerze, fuer Qualitaet) — aber NIEMALS auf Kosten von
Funktionalitaet. Zwei Arten, nur eine erlaubt:

| **Verlustbehaftet (lossy) — VERBOTEN** | **Verlustfrei (lossless) — ERLAUBT** |
|----------------------------------------|--------------------------------------|
| Information wegwerfen: Truncation, blindes `head_limit` das Treffer kappt, gebrauchte Regel weglassen, Feature entfernen | Information auslagern, bleibt erreichbar: File-as-Memory (per Pfad nachladbar), progressive disclosure, path-scoped rules, just-in-time, count→gezielt-content |
| bricht Reasoning, Capability sinkt | Capability bleibt 100 % — jeder Fakt per `read(pfad)` wiederholbar |

**Merksatz:** Kein Wissen/keine Faehigkeit entfernen — nur Ballast aus dem Hot-Context an Orte
verlagern, von denen er jederzeit zurueckgeholt werden kann.

## Weniger Ballast ist sogar BESSER (Context Rot)

Mehr Tokens = schlechtere Genauigkeit, lange vor dem Limit (Chroma 2026: 18 Modelle degradieren;
GPT-4 98,1%→64,1% nur durch Struktur). Lossless-Reduktion macht Agenten crash-sicher UND besser.

## Regressionstest-Pflicht bei JEDER Reduktion

Baseline (vorher: was leistet der Agent?) festhalten → nur verlustfrei aendern → identischen Lauf
wiederholen, vergleichen. Near-100 % der Leistung muss erhalten bleiben; jede Einbusse = Regression =
zuruecknehmen/nachbessern. Erst wenn Capability == Baseline UND Absturzproblem geloest: bestanden.

## Was NIEMALS passieren darf

- Kontext reduzieren durch Wegwerfen/Kappen statt Auslagern · eine Optimierung ohne bestandenen
  Regressionstest behalten · "spart Tokens" als Rechtfertigung fuer Capability-Verlust · eine
  verhaltenskritische Regel weglassen/scopen sodass sie fehlt wenn gebraucht
