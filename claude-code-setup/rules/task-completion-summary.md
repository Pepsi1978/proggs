# Task-Completion-Summary: Pflicht-Schema am Ende jeder Aufgabe (KRITISCH)

> **Diese Regel gilt für ALLE Aufgaben in ALLEN Projekten — ausnahmslos.**
> Am Ende JEDER abgeschlossenen Aufgabe folgen drei mit dünnen Strichen umrahmte
> Boxen (ERLEDIGTE AUFGABEN, optional NÖTIGE AUFGABEN, VORGESCHLAGENE AUFGABEN),
> dann die Status-Meldung, dann maximal zwei Intelligenz-Vorschläge.

---

## Warum diese Regel existiert

Der Benutzer spricht Aufgaben oft per Voice mit Semikolon-Trenner ein und stapelt
mehrere Aufgaben in einer Session. Am Ende muss er auf einen Blick erkennen können:
**Welche Aufgaben sind wirklich abgehakt? Welche nur teilweise? Welche gar nicht?
Was muss als nächstes ran? Was würde die App noch besser machen?**
Eine fließende Prosa-Zusammenfassung verliert sich bei 5-20 parallelen Aufgaben —
drei klar abgegrenzte Boxen bleiben scannbar.

---

## Die drei Pflicht-Boxen am Ende jeder Antwort

In dieser festen Reihenfolge — getrennt durch jeweils eine Leerzeile:

| Box | Wann | Inhalt |
|-----|------|--------|
| 1. **ERLEDIGTE AUFGABEN** | IMMER | Nummerierte Liste aller in dieser Session bearbeiteten Aufgaben mit Status-Symbol |
| 2. **NÖTIGE AUFGABEN** | NUR wenn in Box 1 mindestens eine Aufgabe mit ⚠️ oder ❗ steht | Was als nächstes gemacht werden muss, weil es diesmal nicht abgeschlossen wurde |
| 3. **VORGESCHLAGENE AUFGABEN** | IMMER (genau 5 Stück) | Was die App/Software/Umgebung am stärksten verbessern würde |

---

## Format-Bausteine (gelten für ALLE drei Boxen)

### Die Trennlinie (schlichte dünne Heavy-Unicode-Linie)

Eine Trennlinie besteht aus genau **80 Heavy-Horizontal-Zeichen** `━` (U+2501).
KEINE Farbpunkte an den Enden — der Renderer kann die Linie nicht farbig zeichnen,
also lassen wir die Endmarker komplett weg und nehmen reine dünne Striche.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Wichtig:**
- Genau 80 `━` — nicht mehr, nicht weniger
- KEINE Emoji-Punkte vor oder nach der Linie (kein 🟠, kein 🟢, kein 🟡)
- KEINE Quadrat-Reihen 🟧🟨🟩 (sieht klobig aus)
- KEINE alternativen Zeichen ═══ oder ─── verwenden
- Linie steht immer alleine in ihrer Zeile, KEIN Text dahinter

### Box-Aufbau (zwei oder drei Trennlinien pro Box)

Jede Box hat eine obere Trennlinie und eine Trennlinie unter der Headline.
Eine **untere Abschluss-Trennlinie** gibt es NUR bei der LETZTEN Box (VORGESCHLAGENE
AUFGABEN) — bei ERLEDIGTE und NÖTIGE wird sie weggelassen, weil die obere Linie der
NÄCHSTEN Box visuell denselben Job erledigt. So sieht der Übergang sauber aus statt
mit doppelter Linie.

```
[Trennlinie 1: öffnet die Box]
Headline (z.B. ERLEDIGTE AUFGABEN)
[Trennlinie 2: schließt Headline, öffnet Inhalt]

[Inhalt der Box]

(KEINE Linie hier bei ERLEDIGTE/NÖTIGE — die nächste Box öffnet selbst.
 NUR bei der LETZTEN Box VORGESCHLAGENE: hier kommt die untere Abschluss-Linie.)
```

- Linie 1: oben (öffnet die Box) — IMMER
- Headline: schlicht in Großbuchstaben, KEINE Symbole davor/dahinter
- Linie 2: schließt die Headline ab und öffnet den Inhalt — IMMER
- Inhalt
- Linie 3 (Abschluss): NUR bei VORGESCHLAGENE AUFGABEN — die anderen Boxen enden ohne sie

---

## Box 1 — ERLEDIGTE AUFGABEN (immer)

### Genaues Format

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ERLEDIGTE AUFGABEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔵 1. Aufgabe: [So kurz wie möglich, max 3 Zeilen, leichtes Deutsch]
   ✅ alles okay

🟣 2. Aufgabe: [So kurz wie möglich]
   ⚠️ [was noch fehlt oder offen ist]

🟢 3. Aufgabe: [So kurz wie möglich]
   ❗ [warum es nicht funktioniert hat]

(KEINE untere Abschluss-Linie — die nächste Box (NÖTIGE oder VORGESCHLAGENE)
 öffnet direkt darunter mit ihrer eigenen oberen Linie.)
```

### Aufbau-Regeln

| Regel | Detail |
|-------|--------|
| **Nummerierung** | `1. Aufgabe:`, `2. Aufgabe:`, `3. Aufgabe:` — mit Punkt nach der Zahl |
| **Farbpunkt vor der Nummer** | Jede Aufgabe bekommt einen eigenen Emoji-Punkt VOR der Nummer |
| **Beschreibung** | So kurz wie möglich. Wenn ein halber Satz reicht, ein halber Satz. Maximal 3 Zeilen. Eindeutig klar erkennbar, leichtes Deutsch |
| **Status-Zeile** | IMMER auf eigener Zeile unter der Aufgabe, eingerückt mit 3 Leerzeichen |
| **Leerzeile zwischen Aufgaben** | PFLICHT |

### Farbpunkt-Sequenz für die Aufgaben (zyklisch wenn mehr als 7)

1. 🔵 (blau)
2. 🟣 (lila)
3. 🟢 (grün)
4. 🟡 (gelb)
5. 🟠 (orange)
6. 🟤 (braun)
7. ⚫ (schwarz)

**Niemals verwenden:** 🔴 (rot — Konflikt mit ❗-Fehler-Status)

### Die drei Status-Symbole

| Symbol | Bedeutung | Format |
|--------|-----------|--------|
| ✅ | Zu 100 % erledigt, alles funktioniert | `✅ alles okay` |
| ⚠️ | Teilweise erledigt, etwas fehlt oder muss noch nachgezogen werden | `⚠️ [konkrete Beschreibung was offen ist]` |
| ❗ | Nicht umgesetzt — gescheitert, blockiert oder verschoben | `❗ [konkrete Beschreibung warum nicht]` |

Bei ⚠️ und ❗ MUSS eine konkrete Erklärung folgen — nie nur das Symbol allein.

---

## Box 2 — NÖTIGE AUFGABEN (nur bei ⚠️ oder ❗)

### Wann erscheinen

Diese Box erscheint **NUR wenn in Box 1 mindestens eine Aufgabe mit ⚠️ oder ❗
markiert ist.** Wenn alle Aufgaben ✅ sind, entfällt diese Box komplett.

### Was sie enthält

Für jede ⚠️- oder ❗-Aufgabe aus Box 1: eine konkrete Folge-Aufgabe, die formuliert
was als nächstes gemacht werden muss. Die Anzahl ergibt sich aus den Lücken.

### Genaues Format

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
NÖTIGE AUFGABEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. [Konkrete Folge-Aufgabe, max 2 Zeilen leichtes Deutsch.
   Direkt umsetzbar formuliert.]

2. [Konkrete Folge-Aufgabe, max 2 Zeilen leichtes Deutsch.]

(KEINE untere Abschluss-Linie — die VORGESCHLAGENE-Box öffnet direkt darunter
 mit ihrer eigenen oberen Linie.)
```

### Aufbau-Regeln

| Regel | Detail |
|-------|--------|
| **Nummerierung** | `1.`, `2.`, `3.` — KEIN "Aufgabe:" Präfix, weil sie noch nicht erledigt sind |
| **KEIN Farbpunkt vor der Nummer** | Schlichte Liste, unterscheidet sich optisch von Box 1 |
| **KEIN Status-Symbol** | Diese Aufgaben sind noch nicht angegangen worden |
| **Max 2 Zeilen** | Knapp und direkt umsetzbar |
| **Leerzeile zwischen Aufgaben** | PFLICHT |

---

## Box 3 — VORGESCHLAGENE AUFGABEN (immer 5)

### Wann erscheinen

**IMMER** wenn eine Aufgaben-Session stattgefunden hat. Auch bei reinen
Harness-/Regel-/Setup-Sessions — dann beziehen sich die Vorschläge auf weitere
Verbesserungen am aktuellen Arbeitskontext.

### Was sie enthält

Genau **5 Vorschläge** für Aufgaben, die das Projekt am stärksten verbessern würden.
Bezugspunkt richtet sich nach dem aktuellen Arbeitskontext:

| Kontext der Session | Vorschläge beziehen sich auf |
|--------------------|------------------------------|
| Android-App (BestJournal, Entropie Reductor, …) | Features, UX, Performance, Stabilität der App |
| Desktop-App (TVO, PromptBoard, …) | Features, UX, Cross-Platform-Verhalten |
| Harness / Regeln / Hooks / Skills | Weitere Harness-Verbesserungen, fehlende Guards, neue Skills |
| Doku / README | Klarheit, Vollständigkeit, fehlende Teile |

### Genaues Format

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
VORGESCHLAGENE AUFGABEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. [Vorschlag in max 2 Zeilen leichtes Deutsch.
   Konkret, umsetzbar, mit klarem Nutzen.]

2. [Vorschlag in max 2 Zeilen leichtes Deutsch.]

3. [Vorschlag in max 2 Zeilen leichtes Deutsch.]

4. [Vorschlag in max 2 Zeilen leichtes Deutsch.]

5. [Vorschlag in max 2 Zeilen leichtes Deutsch.]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Aufbau-Regeln

| Regel | Detail |
|-------|--------|
| **Genau 5 Vorschläge** | Nicht 3, nicht 7 — fünf, immer fünf |
| **Nummerierung** | `1.`, `2.`, `3.`, `4.`, `5.` — KEIN "Aufgabe:" Präfix |
| **KEIN Farbpunkt vor der Nummer** | Schlichte Liste wie NÖTIGE AUFGABEN |
| **Max 2 Zeilen pro Vorschlag** | Wenn es länger braucht: kürzen oder anders priorisieren |
| **Leichtes Deutsch** | So wie man es einem Freund erklären würde |
| **Konkret + umsetzbar** | Nicht "vielleicht mal verbessern" — sondern eine Aufgabe die Frank direkt geben könnte |
| **Leerzeile zwischen Vorschlägen** | PFLICHT |
| **Reihenfolge** | Stärkster Nutzen zuerst (Nummer 1 ist der wichtigste Vorschlag) |

---

## Intelligenz-Vorschläge: Maximal 2, feste Reihenfolge

Nach der Status-Meldung kommen **genau zwei** Intelligenz-Vorschläge:

### Vorschlag 1 — Aufgaben-bezogen (ZUERST)

Bezieht sich auf den Bearbeitungs-Workflow der soeben erledigten Aufgaben.
Was ist beim Bearbeiten aufgefallen? Was könnte beim nächsten Mal schneller,
sauberer oder zuverlässiger laufen?

### Vorschlag 2 — Harness-bezogen (DANACH)

Bezieht sich auf die Programmierumgebung selbst — Hooks, Skills, Agents, Rules,
Settings. Was am Harness könnte verbessert werden, damit zukünftig effizienter
gearbeitet werden kann?

### Format pro Vorschlag

```
💡 **Intelligenz-Vorschlag 1 (Aufgabe)**: [Kurzer Titel]
Was passiert ist: [1 Satz]
Warum das ein Problem ist: [1 Satz]
Was ich vorschlage: [1 Satz, konkret]
Warum das System davon schlauer wird: [1 Satz]
Soll ich das umsetzen?
```

### Regeln

| Regel | Detail |
|-------|--------|
| **Maximal 2 Vorschläge** | Nie mehr, lieber gar keinen als einen schwachen |
| **Reihenfolge fest** | IMMER zuerst Aufgaben-bezogen, dann Harness-bezogen |
| **Kein Pflicht-Füller** | Wenn nichts wirklich aufgefallen ist: weglassen, nicht erzwingen |
| **Echter Mehrwert** | Jeder Vorschlag muss umsetzbar und sinnvoll sein |

---

## Vollständiges Beispiel (Multi-Task-Session mit gemischten Ergebnissen)

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

---

## Wann das Schema ANGEWENDET wird

| Situation | Schema nötig? |
|-----------|--------------|
| Feature implementiert | **JA** |
| Bug gefixt | **JA** |
| Kleine Änderung (1 Zeile) | **JA** — eine Aufgabe in der Liste |
| UI angepasst | **JA** |
| Config/Settings geändert | **JA** |
| Neues Projekt erstellt | **JA** |
| Mehrere Aufgaben per Semikolon-Trenner | **JA** — alle als nummerierte Liste |
| Nur Frage beantwortet (kein Code geändert) | **NEIN** — nur bei ausgeführten Aufgaben |
| Nur Code gelesen/erklärt | **NEIN** |
| Recherche/Analyse (ohne Umsetzung) | **NEIN** — nur bei "Aufgabe erledigt"-Momenten |

**Faustregel:** Immer wenn "Committed und gepusht" oder "Ich habe weder committed
noch gepusht" im Status steht, MÜSSEN davor die drei Boxen stehen (Box 2 optional).

---

## Was NIEMALS passieren darf

- ❌ Abschluss-Antwort ohne ERLEDIGTE-AUFGABEN-Box
- ❌ Die alten 3 Punkte (`Aufgabe:` / `Was wurde gemacht:` / `Wie funktioniert es jetzt:`) verwenden — komplett ersetzt
- ❌ Farbpunkte an den Enden der Trennlinien (kein 🟠, 🟡 oder 🟢 vor oder nach den ━)
- ❌ Quadrat-Reihen 🟧🟨🟩 als Trennlinie — sieht klobig aus
- ❌ Trennlinien mit ═══ oder ─── oder anderen Zeichen statt 80 × `━`
- ❌ Trennlinien kürzer als 80 ━
- ❌ Beschreibungen aufblasen die in einem halben Satz erklärt werden könnten — kürzer ist besser
- ❌ Beschreibungen länger als 3 Zeilen
- ❌ Box 2 (NÖTIGE AUFGABEN) weglassen wenn ⚠️ oder ❗ in Box 1 vorkam
- ❌ Box 2 (NÖTIGE AUFGABEN) ohne Trigger einfügen (wenn alle ✅)
- ❌ Box 3 (VORGESCHLAGENE AUFGABEN) mit weniger oder mehr als genau 5 Punkten
- ❌ Box 3 (VORGESCHLAGENE AUFGABEN) weglassen — sie kommt IMMER
- ❌ Aufgaben ohne Nummerierung oder ohne Farbpunkt in Box 1
- ❌ Farbpunkte vor den Nummern in Box 2 oder Box 3 (die sind schlicht)
- ❌ Status-Symbol weglassen in Box 1 (jede Aufgabe BRAUCHT ✅, ⚠️ oder ❗)
- ❌ ⚠️ oder ❗ ohne konkrete Erklärung
- ❌ Mehrere Aufgaben in einem Punkt zusammenfassen
- ❌ Rot (🔴) als Aufgaben-Farbpunkt verwenden (Konflikt mit ❗)
- ❌ Mehr als 2 Intelligenz-Vorschläge oder falsche Reihenfolge (Harness vor Aufgabe)
- ❌ Boxen mitten in der Antwort statt am Ende

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `cross-platform-pflicht` (CLAUDE.md) | Status-Meldung kommt NACH allen drei Boxen, VOR den Vorschlägen |
| `intelligence-suggestions-format` | Diese Regel überschreibt: max 2 Vorschläge, feste Reihenfolge |
| `selbstbeobachtung` (Direktive #2) | Rückblick fließt in den Aufgaben-bezogenen Intelligenz-Vorschlag mit ein |
| `resilient-bugfixing` (Direktive #3) | Bei Bugfixes: jeder Bugfix = eigene Aufgabe in Box 1 |
| `semicolon-task-separator` | Mehrere Aufgaben aus einem Voice-Prompt: jede bekommt eigene Nummer in Box 1 |
| `task-completion-summary` (alte Versionen) | KOMPLETT ERSETZT durch dieses neue Schema |
