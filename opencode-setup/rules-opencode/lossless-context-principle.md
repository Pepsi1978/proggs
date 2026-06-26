Verlustfrei-Prinzip: Kontext reduzieren OHNE Funktionalitaet zu verlieren

Kontext darf reduziert werden (gegen Abstuerze, fuer Qualitaet) — aber NIEMALS auf Kosten von Funktionalitaet. Nur eine Art der Reduktion ist erlaubt:

- **Verlustbehaftet (lossy) — VERBOTEN:** Information wegwerfen. Z.B. Truncation, blindes `head_limit` das echte Treffer kappt, eine gebrauchte Regel weglassen, ein Feature entfernen. Folge: Capability sinkt.
- **Verlustfrei (lossless) — ERLAUBT:** Information auslagern, bleibt erreichbar. Z.B. File-as-Memory (per Pfad nachladbar), progressive disclosure (Tool da, Schema on-demand), path-scoped Regeln (laden bei passenden Dateien), just-in-time retrieval, count -> gezielt content. Capability bleibt 100% — jeder Fakt per `read(pfad)` wiederholbar.

Merksatz: kein Wissen und keine Faehigkeit entfernen — nur Ballast aus dem Hot-Context auslagern, jederzeit zurueckholbar.

## Weniger Ballast ist sogar BESSER (Context Rot)
Mehr Tokens = schlechtere Genauigkeit, lange bevor das Limit erreicht ist. Lossless-Reduktion macht Agenten also nicht nur crash-sicher, sondern qualitativ besser.

## Pflicht-Garantie bei JEDER Reduktion
1. Baseline (vorher): festhalten, was der Agent/die Pipeline aktuell leistet.
2. Aenderung: nur verlustfrei.
3. Regressionstest (nachher): identischen Lauf wiederholen, gegen Baseline vergleichen. Near-100% der Leistung muss erhalten bleiben — jede Einbusse = Regression -> zuruecknehmen oder nachbessern.

## NIEMALS
- Kontext reduzieren durch Wegwerfen/Kappen statt Auslagern.
- Eine Optimierung behalten ohne bestandenen Regressionstest.
- "Spart Tokens" als Rechtfertigung fuer Capability-Verlust akzeptieren.
- Eine verhaltenskritische Regel weglassen/scopen, sodass sie fehlt wenn sie gebraucht wird.
