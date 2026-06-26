Pflicht-Schema am Ende jeder Aufgabe

Am Ende JEDER ausgefuehrten Aufgabe folgen drei mit duennen Strichen umrahmte Boxen, dann die Status-Meldung, dann maximal zwei Intelligenz-Vorschlaege.

## Die drei Boxen (feste Reihenfolge)
1. **ERLEDIGTE AUFGABEN** — IMMER. Nummerierte Liste aller bearbeiteten Aufgaben mit Status-Symbol.
2. **NÖTIGE AUFGABEN** — NUR wenn in Box 1 mindestens ein ⚠️ oder ❗ vorkommt. Was als Naechstes gemacht werden muss.
3. **VORGESCHLAGENE AUFGABEN** — IMMER, genau 5. Was die App/Umgebung am staerksten verbessern wuerde.

## Trennlinie
Genau 80 Heavy-Horizontal-Zeichen `━` (U+2501), alleine in der Zeile. KEINE Farbpunkte/Emojis, keine `═══` oder `───`, nicht kuerzer. Jede Box: obere Linie, Headline (Grossbuchstaben, schlicht), Linie. Untere Abschluss-Linie NUR bei der letzten Box (VORGESCHLAGENE).

## Box 1 — ERLEDIGTE AUFGABEN
Pro Aufgabe ein Farbpunkt VOR der Nummer, dann `N. Aufgabe: [kurz, max 3 Zeilen, leichtes Deutsch]`, darunter eingerueckt die Status-Zeile. Farbpunkt-Sequenz: 🔵 🟣 🟢 🟡 🟠 🟤 ⚫ (zyklisch; NIE 🔴). Status-Symbole: `✅ alles okay` / `⚠️ [was offen ist]` / `❗ [warum nicht]`. Bei ⚠️ und ❗ IMMER konkrete Erklaerung. Leerzeile zwischen Aufgaben.

## Box 2 — NÖTIGE AUFGABEN
Nur bei ⚠️/❗ in Box 1. Nummeriert `1.`, `2.` OHNE Farbpunkt, OHNE Status-Symbol, je eine konkrete Folge-Aufgabe (max 2 Zeilen).

## Box 3 — VORGESCHLAGENE AUFGABEN
Genau 5, nummeriert ohne Farbpunkt, je max 2 Zeilen, konkret + umsetzbar, staerkster Nutzen zuerst.

## Status-Meldung + Intelligenz-Vorschlaege
Nach den Boxen die Status-Meldung (z.B. "Committed, gepusht und plattformuebergreifend."). Danach maximal 2 Intelligenz-Vorschlaege, feste Reihenfolge: zuerst Aufgaben-bezogen, dann Harness-bezogen. Kein Pflicht-Fueller — lieber keinen als einen schwachen. Format je Vorschlag: `💡 **Intelligenz-Vorschlag N (Aufgabe/Harness)**: [Titel]` plus je 1 Satz "Was passiert / Warum Problem / Was ich vorschlage / Warum schlauer" und "Soll ich das umsetzen?".

## Wann anwenden
Bei jeder AUSGEFUEHRTEN Aufgabe (Feature, Bugfix, kleine Aenderung, UI, Config, neues Projekt). NICHT bei reiner Frage-Beantwortung, Code-Erklaerung oder Analyse ohne Umsetzung.

## NIEMALS
- Abschluss-Antwort ohne ERLEDIGTE-AUFGABEN-Box.
- Trennlinien mit Farbpunkten, anderen Zeichen oder kuerzer als 80 ━.
- Box 2 weglassen bei ⚠️/❗ — oder Box 2 ohne Trigger zeigen.
- Box 3 mit nicht genau 5 Punkten oder ganz weglassen.
- Status-Symbol in Box 1 weglassen oder 🔴 als Farbpunkt nutzen.
- Mehr als 2 Intelligenz-Vorschlaege oder falsche Reihenfolge (Harness vor Aufgabe).
