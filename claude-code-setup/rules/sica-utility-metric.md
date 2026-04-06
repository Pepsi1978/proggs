# SICA Utility-Metrik: Session-Qualitaet messen und daraus lernen (KRITISCH)

> Quelle: SICA-Paper (arXiv 2504.15228 — Self-Improving Coding Agent)
> Ein KI-Agent der sich durch systematische Selbstbewertung von 17% auf 53%
> Erfolgsrate verbessert hat — durch genau diese Metrik.
> Direktive: #1 Superintelligenz, #2 Selbstbeobachtung

---

## Was ist die Utility-Metrik?

Die Utility-Metrik ist eine zusammengesetzte Bewertungszahl fuer jede Session.
Sie beantwortet die Frage: **"Wie gut war diese Session wirklich?"**

Im Gegensatz zum reinen Session-Score (der nur fragt "wurde das Ziel erreicht?")
bewertet die Utility-Metrik DREI Dinge gleichzeitig:

| Komponente | Gewicht | Was gemessen wird |
|------------|---------|------------------|
| **Performance** | 50% | Wurde das Ziel erreicht? (session-score overall, Skala 1-5) |
| **Effizienz** | 25% | Wie wenig Fehler relativ zu den Tool-Aufrufen? |
| **Geschwindigkeit** | 25% | Wie wenig Tool-Aufrufe fuer das Ergebnis? |

**Warum diese Gewichtung:** Das Ziel zu erreichen ist am wichtigsten (50%).
Aber wie man dorthin kommt, zaehlt auch: Wer 50 Tool-Aufrufe braucht wo 10 reichen,
lernt weniger schnell als wer direkt und fehlerfrei arbeitet.

---

## Die Formel

```
utility = 0.5 * (overall / 5)
        + 0.25 * max(0, 1 - error_count / max(tool_count, 1))
        + 0.25 * max(0, 1 - tool_count / 50)
```

### Erklaerung fuer Nicht-Programmierer

- **`overall / 5`**: Der Session-Score (1-5) wird auf 0-1 normiert. Score 5 → 1.0, Score 3 → 0.6
- **`1 - error_count / tool_count`**: Fehlerquote. Keine Fehler → 1.0, jeder zweite Aufruf fehlerhaft → 0.5
- **`1 - tool_count / 50`**: Geschwindigkeit. 10 Tool-Aufrufe → 0.8, 50 Tool-Aufrufe → 0.0
- **`max(0, ...)`**: Verhindert negative Werte — minimum ist immer 0

### Beispielrechnungen

| Szenario | overall | errors | tools | utility |
|----------|---------|--------|-------|---------|
| Perfekte Session | 5 | 0 | 8 | 0.5 * 1.0 + 0.25 * 1.0 + 0.25 * 0.84 = **0.96** |
| Gute Session | 4 | 2 | 20 | 0.5 * 0.8 + 0.25 * 0.9 + 0.25 * 0.6 = **0.775** |
| Durchschnitt | 3 | 5 | 35 | 0.5 * 0.6 + 0.25 * 0.86 + 0.25 * 0.3 = **0.59** |
| Schlechte Session | 2 | 10 | 45 | 0.5 * 0.4 + 0.25 * 0.78 + 0.25 * 0.1 = **0.42** |
| Kritische Session | 1 | 15 | 50 | 0.5 * 0.2 + 0.25 * 0.7 + 0.25 * 0.0 = **0.28** |

---

## Bewertungsskala

| Utility-Wert | Bewertung | Bedeutung |
|-------------|-----------|-----------|
| **> 0.8** | Exzellent | System arbeitet auf hohem Niveau |
| **0.7 – 0.8** | Bewaehrt | Strategie hat funktioniert — in Pheromon-Tabelle eintragen |
| **0.5 – 0.7** | Akzeptabel | Ziel erreicht, aber Optimierungspotenzial |
| **0.3 – 0.5** | Verbesserungswuerdig | SICA-Selbstverbesserungs-Impuls PFLICHT |
| **< 0.3** | Kritisch | Sofort analysieren — was ist schiefgelaufen? |

---

## Der SICA-Kern: Selbstverbesserungs-Impuls (PFLICHT bei utility < 0.5)

Das ist der eigentliche Kern des SICA-Papers. Der Agent verbesserte sich von 17% auf
53% Erfolgsrate — nicht weil jemand ihn besser programmiert hat, sondern weil er nach
schlechten Sessions systematisch fragte: **"Was haette mir geholfen?"**

**Nach jeder Session mit utility < 0.5 MUSS Claude sich fragen:**

> "Welches Tool, welche Regel, oder welcher Hook haette diese Session effizienter gemacht?"

Die Antwort wird als **Intelligenz-Vorschlag** formuliert (Format aus `intelligence-suggestions-format.md`).

### Konkrete Fragen fuer den Selbstverbesserungs-Impuls

| Wenn... | Dann fragen: |
|---------|-------------|
| Viele Retries (>3 fuer gleiche Sache) | "Gibt es eine Regel die diesen Fehler verhindert haette?" |
| Viele Tool-Aufrufe (>30) | "Welches Muster haette mir eine Abkuerzung gezeigt?" |
| Niedriger overall-Score | "Was war der Kern des Problems — und wie persistiere ich das?" |
| Viele Fehler | "Gibt es einen Guard oder Poka-Yoke der das automatisch faengt?" |

---

## Wann die Utility-Metrik berechnet wird

### SCHREIBEN (nach jeder nicht-trivialen Session)

Nach Abschluss einer Session mit >5 Tool-Calls wird die Utility BERECHNET und
als zusaetzliches Feld `utility_score` in den Experience Store geschrieben:

```json
{
  "date": "2026-04-06",
  "task_type": "bugfix",
  "task_description": "Hook-Fehler nach Update behoben",
  "strategy": "CBR + Poka-Yoke",
  "tool_count": 18,
  "error_count": 2,
  "success_score": 4,
  "utility_score": 0.79,
  "tags": ["hook", "bugfix", "poka-yoke"]
}
```

### LESEN (vor komplexen Aufgaben)

Bevor eine komplexe Aufgabe gestartet wird (>5 erwartete Tool-Calls):
Letzten 5 Experience-Eintraege lesen und Utility-Schnitt berechnen.

```bash
tail -5 ~/proggs/.claude/agent-memory/shared/experience-store.jsonl
```

Wenn Utility-Schnitt < 0.5: Erhoehte Vorsicht — aktiv nach Pheromon-Tabelle suchen.

---

## Zusammenspiel mit bestehenden Systemen

| System | Rolle |
|--------|-------|
| **session-scores.jsonl** | Liefert `overall`, `tool_count` (als `tool_calls`), `errors` als Rohdaten |
| **experience-store.jsonl** | Speichert den berechneten `utility_score` als neues Feld |
| **Pheromon-Tabelle** (MEMORY.md) | Eintraege mit utility > 0.7 werden als "bewaehrt" markiert und bevorzugt |
| **Intelligenz-Vorschlaege** | Bei utility < 0.5 PFLICHT — SICA-Selbstverbesserungs-Impuls |
| **self-improve Skill** | Liest Utility-Trends und eskaliert bei anhaltendem Abfall |

---

## Trend-Analyse: Wann wird es zum Alarm?

Die Utility-Metrik entfaltet ihre volle Wirkung erst ueber mehrere Sessions.

| Signal | Bedeutung | Reaktion |
|--------|-----------|---------|
| Letzte 5 Sessions durchschnittlich > 0.7 | System verbessert sich | Bestaetigung — Compound Effect wirkt |
| Letzte 5 Sessions durchschnittlich 0.5-0.7 | Plateau | Aktiv nach Verbesserungen suchen |
| Letzte 5 Sessions durchschnittlich < 0.5 | Abfall | `/self-improve` Skill starten |
| Einzelne Session utility < 0.3 | Kritischer Ausreisser | Sofort analysieren, Blocker dokumentieren |
| 3 aufeinanderfolgende Sessions utility < 0.4 | Systemisches Problem | `auto-thorough-escalation.md` Schwellen pruefen |

---

## Praktische Berechnung (Python-Einzeiler)

```python
# In Python einzeilig berechenbar:
def utility(overall, errors, tools):
    return round(
        0.5 * (overall / 5)
        + 0.25 * max(0, 1 - errors / max(tools, 1))
        + 0.25 * max(0, 1 - tools / 50),
        3
    )

# Beispiel:
# utility(4, 2, 20) → 0.775
```

Oder als Bash-Einzeiler fuer schnelle Berechnung nach einer Session:
```bash
python3 -c "o=4; e=2; t=20; print(round(0.5*(o/5)+0.25*max(0,1-e/max(t,1))+0.25*max(0,1-t/50),3))"
```

---

## Was NIEMALS passieren darf

- ❌ Utility-Score berechnen und dann NICHTS damit machen
- ❌ Bei utility < 0.5 keinen Selbstverbesserungs-Impuls formulieren
- ❌ Den utility_score faelschen (hoeher setzen als berechnet) — das untergraebt den Compound Effect
- ❌ Utility nur einmal berechnen und dann nicht mehr auf Trends achten
- ❌ Sessions mit <5 Tool-Calls berechnen (zu wenig Daten, Ergebnis ist nicht aussagekraeftig)
- ❌ Utility als einzige Metrik verwenden — sie ergaenzt den session-score, ersetzt ihn nicht
- ❌ Den SICA-Kern vergessen: Schlechte Sessions sind LERNMOMENTE, keine Niederlage

---

## Zusammenfassung: Der SICA-Kreislauf

```
Session laeuft
      ↓
Utility berechnen (nach Session)
      ↓
utility > 0.7?  → In Pheromon-Tabelle als "bewaehrt" eintragen
      ↓
utility < 0.5?  → SICA-Selbstverbesserungs-Impuls:
                  "Was haette mir geholfen?" → Intelligenz-Vorschlag
      ↓
Trend ueber 5 Sessions?
      → Steigend = Compound Effect wirkt
      → Fallend = self-improve Skill starten
```

**Das Ziel:** In 50 Sessions soll der 5-Session-Utility-Schnitt von typisch 0.5-0.6
auf stabil > 0.75 steigen — genau wie SICA von 17% auf 53% gestiegen ist.
