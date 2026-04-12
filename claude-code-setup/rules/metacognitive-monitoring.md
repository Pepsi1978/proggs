# Metacognitive Monitoring — Hyperagent-Pattern (KRITISCH)

> **Diese Regel implementiert das Hyperagent-Pattern (Meta AI, arXiv 2603.19461) und
> ist ein Kernbestandteil der Zweiten Direktive (Selbstbeobachtung).**
> **Sie laeuft AUTOMATISCH — kein manueller Trigger noetig.**

## Was ist Metacognitive Monitoring?

Metacognition = "Denken ueber das eigene Denken". Der Task-Agent (Claude Code) beobachtet
sich WAEHREND der Arbeit und sammelt Daten fuer die Analyse am Ende.

## Die 4 Echtzeit-Tracker (waehrend der Arbeit mitlaufen lassen)

### Tracker 1: Retry-Zaehler
- Jedes Mal wenn ein Tool-Aufruf fehlschlaegt und wiederholt wird: mental +1
- Jedes Mal wenn ein Build fehlschlaegt und neu gestartet wird: mental +1
- **Alarmschwelle: >3 Retries fuer die GLEICHE Sache** → Sofort innehalten und Root Cause suchen
- Nicht blind nochmal versuchen — das ist das Gegenteil von Intelligenz

### Tracker 2: Drift-Detektor
- Alle ~10 Tool-Calls: Mental pruefen "Arbeite ich noch am urspruenglichen Ziel?"
- Bei Intent-Reminder-Datei-Update (alle 5 Turns via intent-anker Hook): Ziel-Datei lesen
- **Alarmschwelle: Arbeit an etwas das der Benutzer NICHT explizit angefragt hat**
- Bei Drift: Dem Benutzer transparent sagen und zuruecklenken

### Tracker 3: Korrektur-Zaehler
- Jede Benutzer-Korrektur ("nein", "nicht so", "stop", "anders") wird gezaehlt
- **Alarmschwelle: >1 Korrektur zum GLEICHEN Thema** → SOFORT als Regel persistieren
- Eine Korrektur = Lernmoment. Zwei Korrekturen zum gleichen Thema = Systemversagen.

### Tracker 4: Wissens-Vertrauen
- Bei jedem Zugriff auf Memory/Regeln/Whiteboard: "Ist das noch aktuell?"
- **Alarmschwelle: Information ist aelter als 7 Tage UND betrifft Dateipfade/Funktionsnamen**
- Im Zweifel: Kurz pruefen (Datei existiert? Funktion existiert?) bevor darauf gebaut wird

## Wann der Hyperagent AUTOMATISCH laeuft

Der Stop-Hook (`hyperagent-stop.ps1`/`.sh`) injiziert am Ende jeder Claude-Antwort einen
metacognitiven Analyse-Prompt. Dieser Prompt erinnert Claude an die Analyse-Pflicht.

**Vollstaendige Analyse (Hyperagent als Sub-Agent spawnen)** bei:
- Session mit >20 Tool-Calls
- Session mit >2 Benutzer-Korrekturen
- Session mit >3 Build-Fehlern
- Auf explizite Anfrage des Benutzers

**Leichte Analyse (inline, kein Sub-Agent)** bei:
- Session mit 5-20 Tool-Calls
- Keine besonderen Vorkommnisse
- Format: 2-3 Saetze Selbstbeobachtung + ggf. 1 Intelligenz-Vorschlag

**Keine Analyse** bei:
- Triviale Aufgaben (<5 Tool-Calls)
- Reine Frage-Antwort-Sessions ohne Code-Aenderungen

## Session-Score-System

Jede nicht-triviale Session bekommt einen Score der in `~/.claude/session-scores.jsonl`
geschrieben wird. Das ermoeglicht Trend-Analyse ueber die Zeit.

### Score-Dimensionen

| Dimension | 1 (schlecht) | 3 (ok) | 5 (perfekt) |
|-----------|-------------|--------|-------------|
| Intent-Treue | Ziel komplett verfehlt | Ziel mit Umwegen erreicht | Ziel direkt und vollstaendig erreicht |
| Effizienz | Viele Retries, endlose Schleifen | Einige Umwege, insgesamt ok | Minimale Schritte, kein Retry |
| Memory-Aktualitaet | Falsches Wissen verwendet | Alles korrekt, nichts geprueft | Aktiv verifiziert, veraltetes aktualisiert |
| Lernertrag | Nichts gelernt, nichts persistiert | Fehler gefixt aber nicht dokumentiert | Neue Regeln/Skills aus Session extrahiert |

### Gesamt-Score Berechnung
`overall = (intent + efficiency + memory + learning) / 4`

### Trend-Analyse
- **Steigender Trend** (letzte 5 Sessions Durchschnitt > vorherige 5) = System wird intelligenter
- **Fallender Trend** = ALARM — etwas ist schiefgegangen, `/self-improve` muss ran
- **Stagnation** = Plateau — kreativere Verbesserungen noetig

## Compound Gains (das Ziel)

Jede metacognitive Analyse soll mindestens EINEN dieser Outputs produzieren:
1. **Neue Regel** → Verhindert zukuenftigen Fehler
2. **Memory-Update** → Aktualisiert veraltetes Wissen
3. **Skill-Kandidat** → Neues wiederverwendbares Pattern
4. **Prozess-Verbesserung** → Effizienterer Workflow

Diese Outputs akkumulieren sich ueber Sessions:
- Session 1: 1 neue Regel
- Session 10: 10 Regeln → weniger Fehler → hoehere Effizienz-Scores
- Session 50: System macht fast keine vermeidbaren Fehler mehr
- Session 100: Kreativitaet und Innovation statt Fehlerbehebung

Das ist der **Compound Intelligence Effect** — und metacognitive Monitoring ist sein Motor.

## Defense in Depth (Direktive #3)

| Schicht | Mechanismus | Was es absichert |
|---------|-------------|-----------------|
| 1 | Diese Regel (immer geladen) | Claude weiss WIE metacognitive Analyse funktioniert |
| 2 | Stop-Hook (prompt injection) | Erinnerung am Ende JEDER Antwort |
| 3 | Intent-Anker-Hook (UserPromptSubmit) | Drift-Erkennung alle 5 Turns |
| 4 | Hyperagent (Sub-Agent) | Tiefe Analyse bei komplexen Sessions |
| 5 | Session-Scores (JSONL) | Quantitative Trend-Messung ueber die Zeit |
| 6 | Whiteboard-Integration (Sentinel) | Erkenntnisse fliessen ins zentrale Wissen |

Wenn eine Schicht ausfaellt, fangen die anderen auf. Beispiel:
- Stop-Hook kaputt → Regel erinnert Claude trotzdem
- Regel vergessen → Stop-Hook injiziert Erinnerung
- Beides kaputt → Intent-Anker erkennt zumindest Drift
- JSONL nicht schreibbar → Sentinel-Datei als Fallback

## Was NIEMALS passieren darf

- ❌ Session beenden ohne metacognitive Reflexion (bei >5 Tool-Calls)
- ❌ Benutzer-Korrektur erhalten und NICHT als Lernmoment behandeln
- ❌ >3 Retries fuer die gleiche Sache ohne innezuhalten
- ❌ Veraltetes Wissen verwenden ohne es zu pruefen
- ❌ Session-Score nicht schreiben (bei nicht-trivialer Session)
- ❌ Metacognitive Analyse die nur Lob enthaelt — IMMER mindestens 1 Verbesserung finden
