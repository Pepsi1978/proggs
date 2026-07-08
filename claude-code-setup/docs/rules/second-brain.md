# Second Brain (Cortex): Speichern, Mitlernen & Laden (KRITISCH)

> Zentrales Fehler-/Wissens-Gedaechtnis ueber alle Werkzeuge (Claude Code + OpenCode/Codex) via
> `second-brain`-MCP. Drei Aspekte: Bugfixes ablegen (§1), Session-Mitlernen + Entscheidungen (§2),
> immer einzeln laden (§3).

## 1. Funktionierende Bugfixes ablegen (ergaenzt Direktive #3)

Sobald ein Bugfix als FUNKTIONIEREND gilt → als ein Eintrag ins Second Brain, festes Format, unter
`bugfixes/<unterkategorie>`, Titel inkl. Datum UND Uhrzeit. Nur bestaetigte Fixes. Ergaenzt (ersetzt
nicht) die lokale Bug-Doku `~/proggs/bugs/` (Gehirn = reaktiv abrufbare Fall-Akte).
**Wann FUNKTIONIEREND:** objektiv verifizierbar (Build gruen, Tests bestanden, Deploy healthy, Symptom
reproduzierbar WEG) → selbst verifizieren + direkt speichern. Nur-Benutzer-beurteilbar (Optik/UI) oder
unsicher → EINMAL fragen "Hat der Fix funktioniert?", erst bei Ja speichern. Default: nicht speichern bis
bestaetigt; spaeteres "hat nicht funktioniert" → per `forget` entfernen, nach echtem Fix neu schreiben.
**Format:** Titel `Bugfix <App> <Bereich> <YYYY-MM-DD HH:MM>` (lokale Uhr Europe/Berlin im Moment des
Speicherns). Kategorie `bugfixes/<unterkategorie>` — ZUERST pruefen ob eine passende existiert
(`list_memories`/`get_by_category`), nur sonst neue sprechende anlegen. Inhalt (Klartext): `Bugfix
<Datum HH:MM>: <App> <Bereich>. Symptom … Root Cause <konkret: Datei/Funktion/CSS-Klasse> … Fix <was +
Stelle> … Verwandte Pruefung … Verifikation … Funktionalitaets-Diff … [Poka-Yoke]`. Danach Frank in
EINEM Satz: "Im Gehirn dokumentiert: <Titel> [<Kategorie>]." MCP nicht verbunden → Eintrag nicht
verlieren, kurz melden, auf die lokale Bug-Doku ausweichen.

## 2. Session-Mitlernen (Gruppe D, automatisch) + Entscheidungs-Rueckfluss (manuell)

**Automatisch (nur kennen):** SessionEnd-Hook `session-brain-summary` sammelt Franks Prompts + Commits +
geaenderte Dateien (Secrets redaktiert) → `POST /session-log` → verdichtet zu "gemacht/entschieden/
gelernt" unter `Programmierung/Sessions` (Titel `Session <CLI> <Projekt> — YYYY-MM-DD HH:MM`). Dazu:
Kern-Block "Woran Frank gerade baut", Projektstand-Recall, Cross-CLI (OpenCode gleiches Schema).
**Manuell (PFLICHT):** Faellt eine echte Grundsatz-Entscheidung, am ENDE der Aufgabe (nie mittendrin)
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

Unbestaetigten Bugfix speichern · Bugfix-Format/Titel-Schema abwandeln · blind neue Unterkategorie
anlegen · eine Grundsatz-Entscheidung ohne Franks Ja speichern oder den Vorschlag weglassen · den
SessionEnd-Hook ohne Auftrag deaktivieren · `get_by_category` auf eine grosse Kategorie · "N Regeln
eingelesen" bestaetigen ohne die Einzelabrufe.
