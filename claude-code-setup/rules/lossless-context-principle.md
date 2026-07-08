# Verlustfrei-Prinzip: Kontext reduzieren OHNE Funktionalitaet zu verlieren (KRITISCH)

> Systemweit fuer jede Kontext-Optimierung. Teil von Direktive #3 (Funktionalitaets-Erhaltung).

## Grundsatz
Kontext darf reduziert werden (gegen Abstuerze, fuer Qualitaet) — aber NIEMALS auf Kosten von Funktionalitaet.

| **Verlustbehaftet — VERBOTEN** | **Verlustfrei — ERLAUBT** |
|--------------------------------|---------------------------|
| Info wegwerfen: Truncation, blindes `head_limit`, gebrauchte Regel weglassen, Feature entfernen | Info auslagern, bleibt erreichbar: File-as-Memory, progressive disclosure, path-scoped, count→gezielt-content |
| Capability sinkt | Capability bleibt 100 % — jeder Fakt per `read(pfad)` wiederholbar |

**Merksatz:** Kein Wissen/keine Faehigkeit entfernen — nur Ballast aus dem Hot-Context an Orte verlagern,
von denen er zurueckholbar ist. Weniger Ballast ist sogar BESSER (Context Rot: mehr Tokens = schlechtere Genauigkeit).

## Regressionstest-Pflicht
Bei JEDER Reduktion: Baseline festhalten → nur verlustfrei aendern → Lauf wiederholen, vergleichen.
Near-100 % der Leistung muss bleiben; jede Einbusse = Regression = zuruecknehmen.

## Was NIEMALS
- Kontext durch Wegwerfen/Kappen statt Auslagern reduzieren · eine Optimierung ohne bestandenen
  Regressionstest behalten · "spart Tokens" als Rechtfertigung fuer Capability-Verlust.
