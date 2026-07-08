# Task-Completion-Summary: Pflicht-Schema am Ende jeder Aufgabe (KRITISCH)

> **Gilt für ALLE ausgeführten Aufgaben in ALLEN Projekten — ausnahmslos.**
> Am Ende JEDER abgeschlossenen Aufgabe: drei mit dünnen Strichen umrahmte Boxen,
> dann die Status-Meldung, dann maximal zwei Intelligenz-Vorschläge. Zweck: Der Benutzer
> sieht bei 5-20 gestapelten (Voice-)Aufgaben auf einen Blick, was fertig/teilweise/offen ist.

---

## Die drei Pflicht-Boxen (feste Reihenfolge, je eine Leerzeile dazwischen)

| Box | Wann | Inhalt |
|-----|------|--------|
| 1. **ERLEDIGTE AUFGABEN** | IMMER | Alle bearbeiteten Aufgaben, nummeriert, mit Status-Symbol |
| 2. **NÖTIGE AUFGABEN** | NUR wenn Box 1 mind. ein ⚠️ oder ❗ enthält | Was als nächstes ran muss |
| 3. **VORGESCHLAGENE AUFGABEN** | IMMER (genau 5) | Was App/Software/Umgebung am stärksten verbessert |

## Trennlinie & Box-Aufbau

- Trennlinie = **genau 80 × `━` (U+2501)**, allein in der Zeile. KEINE Farbpunkte/Emoji an den Enden, KEINE `═══`/`───`.
- Jede Box: obere Linie → Headline (GROSSBUCHSTABEN, keine Symbole) → Linie unter der Headline → Inhalt.
- **Untere Abschluss-Linie NUR bei der letzten Box (VORGESCHLAGENE).** Box 1 und 2 enden ohne — die nächste Box öffnet mit ihrer eigenen oberen Linie.

## Box 1 — ERLEDIGTE AUFGABEN

- Pro Aufgabe **Farbpunkt vor der Nummer**, Sequenz zyklisch: 🔵 🟣 🟢 🟡 🟠 🟤 ⚫. **NIE 🔴** (Konflikt mit ❗).
- Format `N. Aufgabe: [so kurz wie möglich, max 3 Zeilen, leichtes Deutsch]`, darunter eingerückt (3 Leerzeichen) die Status-Zeile mit GENAU einem Symbol:
  - `✅ alles okay` (zu 100 % erledigt)
  - `⚠️ [konkret was fehlt/offen ist]` (teilweise)
  - `❗ [konkret warum nicht]` (gescheitert/blockiert/verschoben)
- Bei ⚠️ und ❗ IMMER konkrete Erklärung — nie nur das Symbol. Leerzeile zwischen Aufgaben.

## Box 2 — NÖTIGE AUFGABEN (nur bei ⚠️/❗)

Schlichte Nummern `1.`, `2.` — **kein** Farbpunkt, **kein** Status-Symbol, max 2 Zeilen, direkt umsetzbar formuliert. Für jede Lücke aus Box 1 eine konkrete Folge-Aufgabe. Entfällt komplett, wenn alle Aufgaben ✅ sind.

## Box 3 — VORGESCHLAGENE AUFGABEN (immer genau 5)

Schlichte Nummern `1.`–`5.` (kein Farbpunkt), max 2 Zeilen, leichtes Deutsch, konkret + umsetzbar, **stärkster Nutzen zuerst**. Bezug = aktueller Arbeitskontext (App-Features/UX/Stabilität; bei Harness/Regeln → weitere Harness-Verbesserungen). Kommt IMMER.

## Vollständiges Beispiel

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ERLEDIGTE AUFGABEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔵 1. Aufgabe: Paywall in drei neuen Sprachen ergänzt.
   ✅ alles okay

🟣 2. Aufgabe: Premium-Sheet zeigt die neuen Strings.
   ⚠️ tschechische Übersetzung fehlt noch

🟢 3. Aufgabe: Onboarding sollte eine Animation bekommen.
   ❗ Compose API hat sich geändert, eigene Session nötig

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
NÖTIGE AUFGABEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. Tschechische Übersetzung für Premium-Sheet nachziehen.

2. Onboarding-Animation auf neue Compose 1.7 API migrieren.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
VORGESCHLAGENE AUFGABEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. Paywall-A/B-Test einbauen und Conversion vergleichen.

2. Onboarding-Tracking ergänzen, wo Nutzer abbrechen.

3. Premium-Sheet auch im Querformat sauber rendern.

4. Statistik-Bildschirm um Wochen- und Monatsübersicht erweitern.

5. Notifications mit smarterer Standardzeit pro Wochentag.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Committed, gepusht und plattformübergreifend.

💡 Intelligenz-Vorschlag 1 (Aufgabe): …
💡 Intelligenz-Vorschlag 2 (Harness): …
```

## Status-Meldung & Intelligenz-Vorschläge (nach den Boxen)

Nach der Abschluss-Linie folgt die Status-Meldung (aus CLAUDE.md: „Committed, gepusht und
plattformübergreifend." / „Committed und gepusht." / „Ich habe weder committed noch gepusht.").

Danach **maximal 2** Intelligenz-Vorschläge, feste Reihenfolge: **zuerst Aufgabe-bezogen, dann
Harness-bezogen**. Kein Pflicht-Füller — lieber keiner als ein schwacher. Format je Vorschlag:

```
💡 **Intelligenz-Vorschlag 1 (Aufgabe)**: [Kurzer Titel]
Was passiert ist: [1 Satz]
Warum das ein Problem ist: [1 Satz]
Was ich vorschlage: [1 Satz, konkret]
Warum das System davon schlauer wird: [1 Satz]
Soll ich das umsetzen?
```

## Wann angewendet

- **JA** bei jeder ausgeführten/committeten Aufgabe: Feature, Bugfix, 1-Zeilen-Änderung, UI, Config, neues Projekt, Multi-Task (jede Aufgabe eigene Nummer in Box 1).
- **NEIN** bei reinen Fragen/Erklärungen/Code-Lesen/Recherche ohne Umsetzung.
- Faustregel: Wo im Status „Committed…" oder „weder committed noch gepusht" steht, MÜSSEN davor die Boxen stehen (Box 2 optional).

## Was NIEMALS passieren darf

- ❌ Abschluss ohne ERLEDIGTE-AUFGABEN-Box; oder Box 3 weglassen / mit ≠ 5 Punkten.
- ❌ Box 2 weglassen wenn ⚠️/❗ vorkam — oder Box 2 einfügen wenn alles ✅ ist.
- ❌ Trennlinie ≠ 80 × `━`, mit Farbpunkten/Emoji an den Enden oder als `═══`/`───`.
- ❌ In Box 1: Aufgabe ohne Farbpunkt/Nummer, ohne Status-Symbol, oder ⚠️/❗ ohne Erklärung; 🔴 als Farbpunkt.
- ❌ Farbpunkte vor den Nummern in Box 2/3 (die sind schlicht).
- ❌ Mehr als 2 Intelligenz-Vorschläge oder falsche Reihenfolge (Harness vor Aufgabe).
- ❌ Boxen mitten in der Antwort statt am Ende.

## Zusammenspiel

Status-Meldung/Cross-Platform: siehe CLAUDE.md. Multi-Task (`semicolon-task-separator`): jede
Aufgabe eigene Nummer in Box 1. Selbstbeobachtung (#2): Rückblick fließt in den Aufgabe-Vorschlag.
Diese Regel ersetzt alle älteren Task-Completion-Summary-Fassungen.
