# Task-Completion-Summary: Pflicht-Schema am Ende jeder Aufgabe (KRITISCH)

> Am Ende JEDER ausgefuehrten Aufgabe: drei mit `━`-Linien umrahmte Boxen, dann Status-Meldung, dann max
> 2 Intelligenz-Vorschlaege. **Vollstaendiges Beispiel + Detailregeln:
> `claude-code-setup/docs/rules/task-completion-summary.md`.**

## Die drei Pflicht-Boxen (feste Reihenfolge)

1. **ERLEDIGTE AUFGABEN** (IMMER) — jede bearbeitete Aufgabe nummeriert, Farbpunkt vor der Nummer
   (Sequenz 🔵🟣🟢🟡🟠🟤⚫, **NIE 🔴**) + GENAU ein Status-Symbol: `✅ alles okay` / `⚠️ [was fehlt]` /
   `❗ [warum gescheitert]`. Bei ⚠️/❗ IMMER konkrete Erklaerung.
2. **NÖTIGE AUFGABEN** (NUR wenn Box 1 ein ⚠️ oder ❗ enthaelt) — schlichte Nummern, Folge-Aufgaben.
3. **VORGESCHLAGENE AUFGABEN** (IMMER, genau 5) — schlichte Nummern, staerkster Nutzen zuerst.

## Format

Trennlinie = genau **80 × `━`** (U+2501), allein in der Zeile, keine Farbpunkte/Emoji an den Enden, kein
`═══`/`───`. Box: obere Linie → Headline (GROSSBUCHSTABEN) → Linie → Inhalt. **Untere Abschluss-Linie NUR
bei der letzten Box.** Danach die Status-Meldung, dann max 2 Intelligenz-Vorschlaege (zuerst Aufgabe, dann Harness).

## Wann

JA bei jeder ausgefuehrten/committeten Aufgabe (auch 1-Zeilen-Fix; Multi-Task: jede eigene Nummer in Box
1). NEIN bei reinen Fragen/Erklaerungen/Recherche ohne Umsetzung.

## Was NIEMALS
- Abschluss ohne ERLEDIGTE-Box; Box 3 weglassen / ≠ 5 Punkte · Box 2 weglassen bei ⚠️/❗ (oder einfuegen
  wenn alles ✅) · Trennlinie ≠ 80×`━` oder mit Emoji-Enden · 🔴 als Farbpunkt · ⚠️/❗ ohne Erklaerung ·
  >2 Intelligenz-Vorschlaege oder falsche Reihenfolge · Boxen mitten in der Antwort statt am Ende.
