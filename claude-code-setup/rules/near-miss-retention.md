# Near-Miss Retention: Beinahe-Fehler behalten statt loeschen (KRITISCH)

> Quelle: MemRL (arXiv 2601.03192). Direktive #3 + #1.

## Regel

Eintraege in `experience-store.jsonl` / `bug-cases.jsonl` mit `"near_miss": true` werden beim Pruning
NICHT als erste geloescht, sondern BEVORZUGT behalten — auch gegenueber neueren Eintraegen. Ein
Near-Miss hat mehr Lernwert als ein gewoehnlich erfolgreicher Eintrag.

## Warum (Piloten-Analogie)

Ein Pilot, der fast abgestuerzt waere, schreibt den Vorfall genau auf — der naechste lernt daraus,
BEVOR er denselben Fehler macht. Eine Session mit `success_score: 3` + `error_count: 2` zeigt genau,
welche Kombination aus Aufgabentyp und Fehlermustern zu Problemen fuehrt — diese Info fehlt in den
"alles super"-Sessions. Beim naechsten aehnlichen Task kann das System proaktiv den kritischen Schritt
vermeiden (Compound Intelligence).

## Definition: Was ist ein Near-Miss?

Alle drei zutreffend: `success_score` >= 2 UND <= 3 · `error_count` > 0 · beide gleichzeitig
(beinahe gut gegangen, aber nicht ganz). KEIN Near-Miss: score 1 (echter Fehler → Bug-Case),
score 4-5 (Erfolg), error_count 0.

## Speicherorte

`~/proggs/.claude/agent-memory/shared/`: `experience-store.jsonl` (auto, Feld `near_miss`),
`trajectories.jsonl` (auto), `bug-cases.jsonl` (manuell). Der `experience-logger`-Hook setzt
`near_miss` automatisch bei score 2-3 + error_count>0.

## Wann LESEN (PFLICHT)

Vor einer neuen Aufgabe mit gleicher `task_category` wie ein Near-Miss-Eintrag → Near-Miss lesen.
Schnellsuche:
```bash
grep '"near_miss": true' ~/proggs/.claude/agent-memory/shared/experience-store.jsonl
```
Besonders vor: Hook-Edits (exit-Code), Release-Builds (R8), Cross-Platform (Pfade/Encoding), grossen Refactorings.

## Wann manuell in bug-cases.jsonl eintragen

Wenn eine Session BEINAHE in einem kritischen Fehler geendet waere (durch Zufall verhindert), etwas
"komisch war" ohne messbaren Fehler, oder die Situation beim naechsten Mal wirklich schiefgehen koennte.
Format: die ueblichen Felder + `"near_miss": true` + `"near_miss_reason": "…"`.

## Pruning-Prioritaet

1. Normale Eintraege (`near_miss: false`), aelteste zuerst. 2. Near-Miss nur, wenn normale erschoepft
und Limit (200 experience / 100 trajectories) noch ueberschritten. Near-Misses duerfen bis 100% des
Limits belegen — gewollt.

## Was NIEMALS passieren darf

- Near-Miss loeschen nur weil "alt" · Near-Miss ignorieren und normal starten (LESEN ist Pflicht)
- `near_miss: true` auf einen Score-5-Eintrag setzen · Pruning-Logik ohne Near-Miss-Schutz aendern
