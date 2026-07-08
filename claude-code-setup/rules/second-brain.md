# Second Brain (Cortex): Speichern, Mitlernen & Laden (KRITISCH)

> Zentrales Gedaechtnis via `second-brain`-MCP. **Volltext (Bugfix-Format, Session-Mitlernen-Mechanik):
> `claude-code-setup/docs/rules/second-brain.md`.**

## 1. Funktionierende Bugfixes ablegen (ergaenzt Direktive #3)
Sobald ein Bugfix als FUNKTIONIEREND gilt → als ein Eintrag unter `bugfixes/<unterkategorie>`, Titel
`Bugfix <App> <Bereich> <YYYY-MM-DD HH:MM>` (echte Uhr Europe/Berlin). Nur bestaetigte Fixes (objektiv
verifizierbar → direkt; nur-Benutzer-beurteilbar/unsicher → EINMAL fragen, erst bei Ja). Ergaenzt die
lokale Bug-Doku `~/proggs/bugs/`. Inhalt: Symptom, Root Cause (Datei/Funktion), Fix, Verifikation, Funktionalitaets-Diff.

## 2. Session-Mitlernen + Entscheidungs-Rueckfluss
Automatisch (nur kennen): SessionEnd-Hook `session-brain-summary` → `Programmierung/Sessions`. Manuell
(PFLICHT): Faellt eine echte Grundsatz-Entscheidung, am ENDE der Aufgabe vorschlagen "ins Gehirn merken?"
→ bei Ja `remember` unter `Programmierung/Entscheidungen`. NIEMALS automatisch (Frank bestaetigt jede).

## 3. IMMER EINZELN laden — nie ganze grosse Kategorien
`get_by_category` auf grosse Kategorien (Rules, Almanache) → truncated (~32k Token, Modell behauptet dann
faelschlich "gelesen"). Daher: ganze Kategorie → `get_category_item('<Kat>', N)` per Nummer bis `total`;
einzelner Eintrag → `get_by_title`. `get_by_category` NUR fuer kleine Kategorien/Ueberblick.

## Was NIEMALS
- Unbestaetigten Bugfix speichern · Format/Titel-Schema abwandeln · eine Grundsatz-Entscheidung ohne
  Franks Ja speichern oder den Vorschlag weglassen · `get_by_category` auf eine grosse Kategorie · "N
  Regeln eingelesen" bestaetigen ohne die Einzelabrufe.
