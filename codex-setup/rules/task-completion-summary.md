# Task-Completion-Summary: Pflicht-Schema am Ende jeder Aufgabe (KRITISCH)

> **Diese Regel gilt für ALLE Aufgaben in ALLEN Projekten.**
> Am Ende JEDER abgeschlossenen Aufgabe MUSS zuerst ein kurzes 3-Punkte-Schema
> in leichtem Deutsch stehen, bevor weitere Blöcke kommen.

---

## Warum diese Regel existiert

Der Benutzer möchte nach jeder Aufgabe auf einen Blick sehen können: Was war gewünscht?
Was wurde gemacht? Wie sieht es jetzt aus? Ohne diese strukturierte Zusammenfassung
muss er sich die Information aus langen technischen Blöcken zusammensuchen.

---

## Das Pflicht-Schema

### Genaues Format

```markdown
**Aufgabe:**
[Was der Benutzer gewollt hat — in 3-4 Sätzen maximal. Leichtes Deutsch.
Keine Fachbegriffe, wenn vermeidbar.]

**Was wurde gemacht:**
[Was konkret umgesetzt wurde — in 3-4 Sätzen maximal. Leichtes Deutsch.
Ohne technische Details, wenn möglich, aber ehrlich und konkret.]

**Wie funktioniert es jetzt:**
[Kurze Beschreibung der neuen Funktionalität. Wie sieht das Ergebnis aus?
Wie läuft es aus Benutzersicht ab? Leichtes Deutsch.]
```

### Sprachregeln

| Regel | Warum |
|-------|-------|
| Leichtes Deutsch | Der Benutzer soll den Status sofort verstehen. |
| Keine unnötigen Fachbegriffe | Die Zusammenfassung ist aus Benutzersicht, nicht aus Implementierungssicht. |
| 3-4 Sätze pro Punkt | Knapp, aber vollständig. |
| Exakte Titel `**Aufgabe:**`, `**Was wurde gemacht:**`, `**Wie funktioniert es jetzt:**` | Sofort visuell erkennbar. |
| Keine Emojis in diesem Block | Sauber und klar. |
| Keine Tabellen in diesem Block | Reiner Fließtext. |

### Platzierung in der Abschlussantwort

Die Reihenfolge am Ende jeder Aufgabe ist:

```text
**Aufgabe:** ...
**Was wurde gemacht:** ...
**Wie funktioniert es jetzt:** ...

Commit-/Push-Status, falls relevant.
Verifikation, falls relevant.
Intelligenz-Vorschlag, falls sinnvoll.
```

---

## Beispiel

```markdown
**Aufgabe:**
Der Schreibimpuls des Tages soll nach dem Wegklicken wirklich bis Mitternacht
verschwunden bleiben. Das soll in beiden Tagebuch-Apps gleich funktionieren.

**Was wurde gemacht:**
Beide Apps speichern jetzt das Wegklick-Datum so, dass es zuverlässig erhalten bleibt.
Die lokale Zeitzone wird dabei korrekt beachtet.

**Wie funktioniert es jetzt:**
Wenn du den Schreibimpuls wegklickst, bleibt er den Rest des Tages weg. Am nächsten
Tag erscheint automatisch ein neuer.
```

---

## Wann das Schema angewendet wird

| Situation | Schema nötig? |
|-----------|---------------|
| Feature implementiert | **JA** |
| Bug gefixt | **JA** |
| Kleine Änderung | **JA**, aber sehr knapp |
| UI angepasst | **JA** |
| Config oder Settings geändert | **JA** |
| Neues Projekt erstellt | **JA** |
| Nur Frage beantwortet | **NEIN** |
| Nur Code gelesen oder erklärt | **NEIN** |
| Recherche oder Analyse ohne Umsetzung | **NEIN** |

Faustregel: Immer wenn ein Arbeitsstatus wie "geändert", "synchronisiert",
"committed" oder "nicht committed" gemeldet wird, gehört dieses Schema davor.

---

## Was NIEMALS passieren darf

- Abschlussantwort ohne das 3-Punkte-Schema nach einer erledigten Aufgabe
- Schema in technischer Sprache
- Punkt mit mehr als 4 Sätzen
- Schema nach den Intelligenz-Vorschlägen
- Schema mit Emojis oder Tabellen
- Ein Punkt weggelassen
- Die Platzhalter-Titel ändern

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `semicolon-task-separator.md` | Bei vielen kleinen Teilaufgaben reicht eine Gesamtzusammenfassung am Ende. |
| `intelligence-system.md` | Intelligenz-Vorschläge kommen nach diesem Schema. |
| `self-observation.md` | Der Rückblick fließt knapp in "Was wurde gemacht" ein. |
| `resilient-bugfixing.md` | Bei Bugfixes wird die Ursache kurz und verständlich erwähnt. |
