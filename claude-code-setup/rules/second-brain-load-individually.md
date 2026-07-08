# Second-Brain immer EINZELN laden — nie ganze Kategorien zusammenschmeissen (KRITISCH)

> Ausloeser: `get_by_category('Programmierung/Rules')` gibt ~14 Regeln auf einmal = 128.074 Zeichen /
> ~32k Token → sprengt jedes Tool-Response-Limit (Claude: `result exceeds maximum allowed tokens`;
> OpenCode: `truncated`). Das Modell behauptet dann faelschlich "Regeln eingelesen", obwohl es sie NIE
> gelesen hat (Halluzination). EINE einzelne Regel ist klein genug.

## Die eine Regel

Daten aus dem Second Brain (`second-brain`-MCP) werden IMMER EINZELN geladen — ein Eintrag pro Abruf —
NIEMALS als ganze grosse Kategorie (`get_by_category`) in den Kontext.

- **Ganze Kategorie durchgehen** → per Nummer iterieren mit `get_category_item`:
  `get_category_item('<Kategorie>', 1)` liefert EINEN Eintrag 1:1 + nennt `total`; dann 2, 3, … bis `total`.
  Kein Titel raten, keine Liste parsen — eine Zahl kann kein Modell falsch tippen (Poka-Yoke Stufe 3).
- **Einzelnen bekannten Eintrag** → `get_by_title` (tolerant gegen faelschlich angehaengtes
  ` [Kategorie]` / ` — N Zeichen` / fuehrende `92.`).
- `get_by_category` (alle auf einmal) NUR fuer KLEINE Kategorien / Ueberblick — NIEMALS fuer grosse
  wie `Programmierung/Rules` oder `Programmierung/Almanache/*` (50-100 KB je Eintrag → truncated).

## Pflicht-Startaufgabe (v.a. OpenCode)

Wo eine CLI ihre Arbeitsregeln aus dem Gehirn bezieht, ist das Laden der Regeln eine MUSS-Aufgabe am
Session-Start (erste Handlung). `Programmierung/Rules` per Nummer durchiteriern, dann "N Regeln
eingelesen" bestaetigen (N = `total`; nur so viele wie wirklich abgerufen). Claude Code laedt seine
Regeln lokal aus `~/.claude/rules/` (kein Gehirn-Start-Abruf noetig) — aber sobald Claude SELBST
Second-Brain-Daten abruft, gilt das Einzeln-Prinzip ebenso.

## Warum einzeln besser ist

Kein Truncation (Inhalt kommt vollstaendig an) · Ehrlichkeit erzwungen (nur bestaetigen was wirklich
geladen wurde) · verlustfrei + gezielt (grosse Almanache nur bei Bedarf einzeln, kein Context-Rot durch Bulk).

## Was NIEMALS passieren darf

- `get_by_category` auf eine grosse Kategorie zum Laden in den Kontext (wird truncated → NICHT gelesen)
- "N Regeln eingelesen" bestaetigen ohne dass die Einzelabrufe durchgelaufen sind
- Den Start-Abruf als optional behandeln · mehrere grosse Eintraege in einem Abruf zusammenfassen
