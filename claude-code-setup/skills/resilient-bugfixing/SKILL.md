---
name: resilient-bugfixing
description: >
  Nutze diesen Skill AUTOMATISCH bei JEDEM Bugfix und JEDEM Fehler — egal wie klein oder trivial.
  Trigger-Situationen: Bugfix, Fehler, Problem, Build-Fehler, Compile-Fehler, Hook-Fehler, 
  Test-Fehler, Laufzeitfehler, Crash, Exception, fehlgeschlagener Befehl, Exit-Code ungleich 0,
  Warnung die behoben werden soll, Performance-Problem, Regression.
  Deutsche Trigger: "funktioniert nicht", "Fehler", "Bug", "kaputt", "geht nicht", "fixen", 
  "reparieren", "klappt nicht", "Absturz", "haengt", "falsch", "stimmt nicht", "Problem", 
  "gebrochen", "defekt", "laeuft nicht", "schlaegt fehl", "Fehlermeldung".
  Englische Trigger: "error", "fix", "bug", "broken", "crash", "fail", "issue", "not working",
  "exception", "debug", "repair".
  AUCH bei Zwischenfehlern die spaeter gefixt werden — jeder Fehler zaehlt.
  Dieser Skill liefert die Checkliste fuer Direktive #3 (Resilient Bugfixing).
invocation: auto
---

# Dritte Direktive: Resilient Bugfixing — Vollstaendige Arbeitsanweisung

> **Jeder Bug wird zum permanenten System-Upgrade. Kein Fehler darf zweimal auftreten.**

## Bug-Case-Match dem Benutzer ANSAGEN (KRITISCH)

Wenn der `bug-case-auto-writer` Hook einen bekannten Fehler matcht und den Fix als
`additionalContext` injiziert, MUSS Claude das dem Benutzer **sofort auf Deutsch ansagen** —
mitten in der Arbeit, nicht erst am Ende. Der Benutzer will sehen dass das System sich erinnert.

**Pflicht-Format (woertlich so, mit dem Schild-Symbol am Anfang):**

```
🛡️ Der Bug-Case Auto-Writer hat einen bekannten Fehler erkannt ([X]% Match).
Letztes Mal war die Ursache: [Root Cause aus dem Match]
Ich wende den bekannten Fix zuerst an.
```

**Danach** den Fix aus dem Match anwenden — BEVOR eine eigene Hypothese versucht wird.

**Wann ansagen:**
- IMMER wenn `BEKANNTER FEHLER` im additionalContext steht
- SOFORT — nicht warten bis die Aufgabe fertig ist
- Auch bei kleinen Fehlern (50% Match reicht)

**Warum:** Ohne Ansage arbeitet der Hook unsichtbar. Der Benutzer sieht nur das Ergebnis,
nicht dass das System sich erinnert hat. Die Ansage macht den Compound Intelligence Effect
sichtbar: "Aha, das System hat sich erinnert und Zeit gespart."

## Pflicht-Ablauf bei JEDEM Bugfix

### Schritt 1: Root Cause finden (5-Warum-Methode)

Mindestens 3x "Warum?" fragen. Function-Level Lokalisierung (empirisch bewiesen optimal, arXiv 2604.00167):

```
Symptom: Was passiert?
Warum 1: Warum passiert das?
Warum 2: Warum passiert DAS?
Warum 3: Was ist die tiefste Ursache?
→ Root Cause = Funktion X macht Y falsch wegen Z
```

**Faustregel:** Root Cause nach 30 Sekunden noch unklar? → Hypothesen-Loop starten (2-3 Hypothesen formulieren, instrumentieren, Laufzeitdaten analysieren).

### Schritt 2: Verwandte Fehlerquellen suchen (PFLICHT)

| Dimension | Frage |
|-----------|-------|
| **Gleiche Klasse** | Kann der gleiche Fehlertyp an anderer Stelle auftreten? |
| **Gleiche Komponente** | Kann die betroffene Komponente auf andere Weise versagen? |
| **Gleiche Abhaengigkeit** | Welche anderen Teile haengen von der gleichen Sache ab? |

Ergebnis IMMER dokumentieren — auch wenn nichts gefunden wird.

### Schritt 3: Zukunftssicheren Fix implementieren

Der Fix muss ALLE diese Eigenschaften haben:
- **Self-Healing**: Funktioniert nach Updates, Neustarts, Plattformwechsel
- **Defensiv**: Faengt die ganze Fehlerklasse ab, nicht nur den einen Fall
- **Funktionserhaltend**: NIEMALS Features entfernen um Bugs zu fixen

### Schritt 3b: Fix-Induced-Failure-Pruefung (VOR dem Commit)

| # | Pruefung |
|---|----------|
| 1 | Was haengt vom geaenderten Code ab? |
| 2 | Was wenn der Fix-Code selbst fehlschlaegt? |
| 3 | Aendert der Fix einen Systemzustand dauerhaft? |
| 4 | Kann der Fix mit anderem Code kollidieren (Race Conditions)? |
| 5 | Bricht der Fix etwas das vorher funktionierte? |
| 6 | Funktioniert der Fix auf macOS UND Windows? |
| 7 | Ueberlebt der Fix das naechste Update? |
| 8 | Was wenn eine Voraussetzung fehlt (Graceful Degradation)? |

### Schritt 3c: Funktionalitaets-Diff

```
VORHER:
- [Feature A] funktionierte ✅/❌
- [Feature B] funktionierte ✅/❌

NACHHER:
- [Feature A] funktioniert ✅/❌ (unveraendert/gefixt/REGRESSION)
- [Feature B] funktioniert ✅/❌
```

Wenn ein Feature von ✅ auf ❌ wechselt → Fix ist NICHT fertig.

### Schritt 4: Defense in Depth (mindestens 2 Schichten)

| Schicht | Typ | Beispiel |
|---------|-----|---------|
| 1 | Praeventiv | Eingabevalidierung, Type-Checks |
| 2 | Reaktiv | Try-Catch mit Fallback (nie leer!) |
| 3 | Selbstheilend | Auto-Patcher, Templates |
| 4 | Upstream | GitHub Issue beim Verursacher |

### Schritt 5: Poka-Yoke pruefen

1. Kann der Fehler durch ein Template/Default **eliminiert** werden? (Stufe 3)
2. Kann der Fehler durch einen Guard **erzwungen** werden? (Stufe 2)
3. Kann zumindest **gewarnt** werden? (Stufe 1)

### Schritt 6: Bug-Case in bug-cases.jsonl dokumentieren

```json
{"date":"YYYY-MM-DD","symptom":"...","root_cause":"...","fix":"...","files":["..."],"tags":["..."],"severity":"hoch/mittel/kritisch","prevention":"..."}
```

Falls `auto_captured: true` Eintraege existieren → diese JETZT mit Root Cause und Fix vervollstaendigen.

## Verbotene Fix-Strategien

| VERBOTEN | ERLAUBT |
|----------|---------|
| Feature auskommentieren | Feature mit Fehlerbehandlung absichern |
| Leeres `catch {}` | `catch` mit Logging + Fallback |
| Import entfernen weil er Fehler wirft | Import-Fehler abfangen + Fallback |
| Funktion durch leere Funktion ersetzen | Funktion reparieren |

## Checkliste (mental durchgehen vor Commit)

```
□ Root Cause identifiziert (nicht nur Symptom)?
□ Verwandte Fehlerquellen in 3 Dimensionen geprueft?
□ Fix ist self-healing, defensiv, ueberlebbar?
□ Fix-Induced-Failure-Pruefung (8 Punkte) bestanden?
□ Funktionalitaets-Diff: Alle Features weiterhin ✅?
□ KEINE Funktionalitaet entfernt oder geschluckt?
□ Mindestens 2 Absicherungsschichten?
□ Poka-Yoke geprueft?
□ Bug-Case dokumentiert (oder auto-captured ergaenzt)?
```
