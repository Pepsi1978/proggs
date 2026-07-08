# Second Brain (Cortex): Speichern, Mitlernen & Laden (KRITISCH)

> `second-brain`-MCP. **Volltext: `claude-code-setup/docs/rules/second-brain.md`.**

## 1. Funktionierende Bugfixes (Direktive #3)
Bugfix FUNKTIONIERT -> Eintrag unter `bugfixes/<unterkategorie>`, Titel `Bugfix <App> <Bereich>
<YYYY-MM-DD HH:MM>` (echte Uhr Europe/Berlin). Nur bestaetigte Fixes (objektiv -> direkt; nur-Benutzer/
unsicher -> EINMAL fragen, erst bei Ja). Ergaenzt lokale Doku `~/proggs/bugs/`. Inhalt: Symptom, Root
Cause, Fix, Verifikation, Funktionalitaets-Diff.

## 2. Session-Mitlernen + Entscheidungs-Rueckfluss
Automatisch: SessionEnd-Hook `session-brain-summary` -> `Programmierung/Sessions`. Manuell (PFLICHT):
echte Grundsatz-Entscheidung -> am ENDE der Aufgabe "ins Gehirn merken?" -> bei Ja `remember` unter
`Programmierung/Entscheidungen`. NIEMALS automatisch (Frank bestaetigt jede).

## 3. IMMER EINZELN laden
`get_by_category` auf grosse Kategorien -> truncated (~32k Token; Modell behauptet faelschlich
"gelesen"). Daher: ganze Kategorie -> `get_category_item('<Kat>', N)` per Nummer bis `total`; einzelner
Eintrag -> `get_by_title`. `get_by_category` NUR fuer kleine Kategorien.

## Was NIEMALS
- Unbestaetigten Bugfix speichern - Format/Titel-Schema abwandeln - Grundsatz-Entscheidung ohne Franks Ja
  speichern oder Vorschlag weglassen - `get_by_category` auf grosse Kategorie - "N Regeln eingelesen"
  bestaetigen ohne Einzelabrufe.
