# Second Brain (Cortex): Speichern & Laden (KRITISCH)

> `second-brain`-MCP. **Volltext: `claude-code-setup/docs/rules/second-brain.md`.**

## 1. Bugfixes bleiben lokal (Direktive #3)
Bugfixes ausschließlich in `.claude/agent-memory/shared/bug-cases.jsonl`, `~/proggs/bugs/` und bei
Bedarf `~/proggs/best-practices/` dokumentieren. Für Bugfixes NIEMALS `second-brain` `remember`
verwenden und weder `bugfixes` noch eine Unterkategorie davon im Gehirn anlegen oder befüllen.

## 2. Entscheidungs-Rueckfluss
Echte Grundsatz-Entscheidung -> am ENDE der Aufgabe "ins Gehirn merken?" -> bei Ja `remember` unter
`Programmierung/Entscheidungen`. NIEMALS automatisch (Frank bestaetigt jede).

## 3. IMMER EINZELN laden
`get_by_category` auf grosse Kategorien -> truncated (~32k Token; Modell behauptet faelschlich
"gelesen"). Daher: ganze Kategorie -> `get_category_item('<Kat>', N)` per Nummer bis `total`; einzelner
Eintrag -> `get_by_title`. `get_by_category` NUR fuer kleine Kategorien.

## Was NIEMALS
- Bugfixes ins Second Brain speichern oder dort `bugfixes`-Kategorien anlegen - Grundsatz-Entscheidung
  ohne Franks Ja speichern oder Vorschlag weglassen - `get_by_category` auf grosse Kategorie - "N Regeln
  eingelesen" bestaetigen ohne Einzelabrufe.
