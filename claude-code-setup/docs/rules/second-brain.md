# Second Brain (Cortex): Speichern & Laden (KRITISCH)

> Zentrales Wissens-Gedaechtnis ueber alle Werkzeuge (Claude Code + OpenCode/Codex) via
> `second-brain`-MCP. Drei Aspekte: Bugfixes lokal halten (§1), Entscheidungen (§2),
> immer einzeln laden (§3).

## 1. Bugfixes ausschließlich lokal dokumentieren (ergaenzt Direktive #3)

Bugfixes werden NICHT im Second Brain abgelegt. Root Cause, Fix, Verifikation, verwandte Prüfung und
Poka-Yoke bleiben dauerhaft in `.claude/agent-memory/shared/bug-cases.jsonl`, `~/proggs/bugs/` und bei
Bedarf `~/proggs/best-practices/` erhalten. Diese lokalen Quellen sind versioniert beziehungsweise für
die automatische Bug-Erkennung strukturiert und vermeiden redundante Einzelakten im Gehirn.

Für Bugfixes gilt daher ausnahmslos: `second-brain` `remember` nicht aufrufen, keine Kategorie
`bugfixes` und keine Unterkategorie davon anlegen oder befüllen. Die Qualitäts- und Lernpflichten der
Direktiven #2 und #3 bleiben vollständig bestehen; nur das Second Brain ist kein Bugfix-Ablageort mehr.

## 2. Entscheidungs-Rueckfluss (manuell)

Faellt eine echte Grundsatz-Entscheidung, am ENDE der Aufgabe (nie mittendrin)
vorschlagen: "Soll ich diese Entscheidung ins Gehirn merken? [Entscheidung + Begruendung, je 1 Satz]".
Bei Ja: `remember`, Titel `Entscheidung <Bereich>: <Kurz> <YYYY-MM-DD>`, Kategorie
`Programmierung/Entscheidungen`, Inhalt = Entscheidung + verworfene Alternative + Begruendung. NIEMALS
automatisch (anders als Bugfixes) — Frank bestaetigt jede. Titel-/Kategorie-Schema nie abwandeln (bricht
Chronologie/Recall).

## 3. IMMER EINZELN laden — nie ganze grosse Kategorien

`get_by_category('Programmierung/Rules')` = ~14 Regeln / ~32k Token → sprengt das Tool-Response-Limit
(truncated → Modell behauptet faelschlich "gelesen"). Daher Second-Brain-Daten IMMER EINZELN laden:
- Ganze Kategorie → per Nummer iterieren `get_category_item('<Kategorie>', 1)` (liefert 1 Eintrag +
  nennt `total`), dann 2, 3 … bis `total` (kein Titel-Raten — Poka-Yoke Stufe 3).
- Einzelner bekannter Eintrag → `get_by_title` (tolerant gegen angehaengtes `[Kategorie]`/`— N Zeichen`/`92.`).
- `get_by_category` NUR fuer KLEINE Kategorien/Ueberblick — NIE fuer grosse (Rules, Almanache/*).
Wo eine CLI ihre Regeln aus dem Gehirn bezieht (v.a. OpenCode), ist das Laden Session-Start-Pflicht (per
Nummer durchiteriern, dann "N Regeln eingelesen" — nur so viele wie wirklich abgerufen). Claude Code
laedt lokal aus `~/.claude/rules/` — aber sobald es SELBST Second-Brain-Daten abruft, gilt Einzeln.

## Was NIEMALS

Bugfixes ins Second Brain speichern · dort `bugfixes` oder eine Unterkategorie anlegen · eine
Grundsatz-Entscheidung ohne Franks Ja speichern oder den Vorschlag weglassen ·
`get_by_category` auf eine grosse Kategorie · "N Regeln
eingelesen" bestaetigen ohne die Einzelabrufe.
