# Task-Completion-Summary: Pflicht-Schema am Ende jeder Aufgabe (KRITISCH)

> **Diese Regel gilt für ALLE Aufgaben in ALLEN Projekten — ausnahmslos.**
> Am Ende JEDER abgeschlossenen Aufgabe MUSS die "ERLEDIGTE AUFGABEN"-Box stehen,
> bevor Status-Meldung und Intelligenz-Vorschläge kommen.

---

## Warum diese Regel existiert

Der Benutzer spricht Aufgaben oft per Voice mit Semikolon-Trenner ein und stapelt
mehrere Aufgaben in einer Session. Am Ende muss er auf einen Blick erkennen können:
**Welche Aufgaben sind wirklich abgehakt? Welche nur teilweise? Welche gar nicht?**
Eine fließende Prosa-Zusammenfassung verliert sich bei 5-10 parallelen Aufgaben —
eine nummerierte Status-Box bleibt scannbar.

---

## Das Pflicht-Format (IMMER am Ende der Abschluss-Antwort, vor Status-Meldung)

### Genaues Format

```
═══════════════════════════════════════════════════
                ERLEDIGTE AUFGABEN
═══════════════════════════════════════════════════

🔵 1. Aufgabe: [Kurzbeschreibung in 2-3 Sätzen,
   leichtes Deutsch, was umgesetzt wurde]
   ✅ alles okay

🟣 2. Aufgabe: [Kurzbeschreibung in 2-3 Sätzen,
   leichtes Deutsch]
   ⚠️ [was noch fehlt oder offen ist]

🟢 3. Aufgabe: [Kurzbeschreibung in 2-3 Sätzen,
   leichtes Deutsch]
   ❗ [warum es nicht funktioniert hat]

═══════════════════════════════════════════════════
```

### Aufbau-Regeln

| Regel | Detail |
|-------|--------|
| **Headline** | Genau `ERLEDIGTE AUFGABEN` — zentriert dargestellt, nicht ändern |
| **Trennlinien** | `═══════════════════════════════════════════════════` (Unicode-Doppelstrich, ca. 51 Zeichen) — oben UND unten |
| **Nummerierung** | `1. Aufgabe:`, `2. Aufgabe:`, `3. Aufgabe:` — mit Punkt nach der Zahl |
| **Farbpunkt** | Jede Aufgabe bekommt einen eigenen Emoji-Punkt VOR der Nummer (nie orange — orange ist Eingrenzungs-Symbol) |
| **Beschreibung** | 2-3 Sätze in leichtem Deutsch, so wie man es einem Freund erklären würde. Keine Fachbegriffe wenn vermeidbar |
| **Status-Zeile** | IMMER auf eigener Zeile unter der Aufgabe, eingerückt mit 3 Leerzeichen |
| **Leerzeile zwischen Aufgaben** | PFLICHT — jede Aufgabe ist visuell vom Rest getrennt |

### Farbpunkt-Sequenz für die Aufgaben

In dieser Reihenfolge verwenden (zyklisch wenn mehr als 7 Aufgaben):

1. 🔵 (blau)
2. 🟣 (lila)
3. 🟢 (grün)
4. 🟡 (gelb)
5. 🟤 (braun)
6. ⚫ (schwarz)
7. ⚪ (weiß)

**Niemals verwenden:** 🟠 (orange — reserviert für die Eingrenzungs-Linien)
**Niemals verwenden:** 🔴 (rot — Konflikt mit ❗-Fehler-Status)

### Die drei Status-Symbole

| Symbol | Bedeutung | Format |
|--------|-----------|--------|
| ✅ | Zu 100 % erledigt, alles funktioniert | `✅ alles okay` |
| ⚠️ | Teilweise erledigt, etwas fehlt oder muss noch nachgezogen werden | `⚠️ [konkrete Beschreibung was offen ist]` |
| ❗ | Nicht umgesetzt — gescheitert, blockiert oder verschoben | `❗ [konkrete Beschreibung warum nicht]` |

**Wichtig:** Bei ⚠️ und ❗ MUSS eine konkrete Erklärung folgen — nie nur das Symbol allein.

### Platzierung in der Antwort

Die Reihenfolge am Ende jeder Aufgabe ist:

```
1. (während der Arbeit) Tool-Aufrufe, Code-Änderungen, kurze Updates
2. (optional) Insight-Block
...
AM ENDE — in dieser Reihenfolge:

═══════════════════════════════════════════════════
                ERLEDIGTE AUFGABEN
═══════════════════════════════════════════════════

🔵 1. Aufgabe: ...
   ✅ alles okay

🟣 2. Aufgabe: ...
   ✅ alles okay

═══════════════════════════════════════════════════

Committed und gepusht. / Committed, gepusht und plattformübergreifend.

💡 Intelligenz-Vorschlag 1 (Aufgaben-bezogen): ...
💡 Intelligenz-Vorschlag 2 (Harness-bezogen): ...
```

---

## Intelligenz-Vorschläge: Maximal 2, feste Reihenfolge

Nach der Status-Meldung kommen **genau zwei** Intelligenz-Vorschläge in dieser
festen Reihenfolge:

### Vorschlag 1 — Aufgaben-bezogen (ZUERST)

Bezieht sich auf die soeben erledigten Aufgaben. Was ist beim Bearbeiten aufgefallen?
Was könnte beim nächsten Mal schneller, sauberer oder zuverlässiger laufen?
Mögliche Stoßrichtungen:

| Richtung | Beispiel |
|----------|----------|
| Workflow-Learning | "Bei diesem Aufgabentyp lohnt sich Python-Batch statt Edit-für-Edit" |
| App-Verbesserung | "Beim Editieren ist mir aufgefallen, dass der Settings-Bildschirm zwei Buttons hat die das Gleiche tun" |
| Bug-Vermeidung | "Diese Konstellation könnte beim nächsten Mal stolpern — Test schreiben?" |
| Persistierung | "Diese Erkenntnis sollte als Memory gespeichert werden" |

### Vorschlag 2 — Harness-bezogen (DANACH)

Bezieht sich auf die Programmierumgebung selbst. Was am Harness (Hooks, Skills,
Agents, Rules, Settings) könnte verbessert werden, damit zukünftig effizienter
gearbeitet werden kann?

| Richtung | Beispiel |
|----------|----------|
| Neuer Hook | "Ein Pre-Commit-Hook könnte diesen Fehlertyp automatisch fangen" |
| Neue Regel | "Diese Beobachtung sollte als permanente Regel persistiert werden" |
| Skill-Verbesserung | "Skill X könnte einen zusätzlichen Trigger gebrauchen" |
| Agent-Optimierung | "Agent Y könnte schneller laufen wenn er Z auslagert" |

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

## Beispiel mit drei Aufgaben (gut)

```
═══════════════════════════════════════════════════
                ERLEDIGTE AUFGABEN
═══════════════════════════════════════════════════

🔵 1. Aufgabe: Die Paywall wurde um drei neue
   Sprachen erweitert. Türkisch, Polnisch und
   Schwedisch sind jetzt vollständig übersetzt
   und werden in der App angezeigt.
   ✅ alles okay

🟣 2. Aufgabe: Das Premium-Bottom-Sheet wurde
   überarbeitet und zeigt jetzt die neuen Strings.
   Auf Deutsch und Englisch sieht es korrekt aus.
   ⚠️ tschechische Übersetzung steht noch aus, kommt in der nächsten Session

🟢 3. Aufgabe: Der neue Onboarding-Screen sollte
   einen sanften Animations-Effekt beim Erscheinen
   bekommen.
   ❗ Compose Animation API hat sich in Version 1.7 geändert, braucht eigene Session für saubere Migration

═══════════════════════════════════════════════════
```

---

## Wann das Schema ANGEWENDET wird

| Situation | Box nötig? |
|-----------|-----------|
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
noch gepusht" im Status steht, MUSS davor die ERLEDIGTE-AUFGABEN-Box stehen.

---

## Was NIEMALS passieren darf

- ❌ Abschluss-Antwort ohne ERLEDIGTE-AUFGABEN-Box
- ❌ Die alten 3 Punkte (`Aufgabe:` / `Was wurde gemacht:` / `Wie funktioniert es jetzt:`) verwenden — komplett ersetzt
- ❌ Aufgaben ohne Nummerierung oder ohne Farbpunkt auflisten
- ❌ Status-Symbol weglassen (jede Aufgabe BRAUCHT ✅, ⚠️ oder ❗)
- ❌ ⚠️ oder ❗ ohne Erklärung was offen ist / warum es nicht ging
- ❌ Mehrere Aufgaben in einem Punkt zusammenfassen — JEDE Aufgabe bekommt ihre eigene Nummer
- ❌ Orange (🟠) als Aufgaben-Farbpunkt verwenden (reserviert für Eingrenzungs-Linien)
- ❌ Rot (🔴) als Aufgaben-Farbpunkt verwenden (Konflikt mit ❗)
- ❌ Mehr als 2 Intelligenz-Vorschläge
- ❌ Intelligenz-Vorschläge in falscher Reihenfolge (Harness vor Aufgabe)
- ❌ Box mitten in der Antwort statt am Ende
- ❌ Trennlinien weglassen oder durch andere Zeichen ersetzen

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `cross-platform-pflicht` (CLAUDE.md) | Status-Meldung kommt NACH der Box, VOR den Vorschlägen |
| `intelligence-suggestions-format` | Diese Regel überschreibt: max 2 Vorschläge, feste Reihenfolge (Aufgabe → Harness) |
| `selbstbeobachtung` (Direktive #2) | Rückblick fließt in den Aufgaben-bezogenen Vorschlag mit ein |
| `resilient-bugfixing` (Direktive #3) | Bei Bugfixes: Status der einzelnen Bugfix-Aufgabe in der Box |
| `semicolon-task-separator` | Bei mehreren Aufgaben aus einem Voice-Prompt: jede bekommt ihre eigene Nummer in der Box |
| `task-completion-summary` (alte Version) | KOMPLETT ERSETZT durch dieses neue Schema — die alten 3 Punkte gibt es nicht mehr |
