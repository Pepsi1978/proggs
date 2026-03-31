# Intent Drift Prevention

## Rule: Track and Verify Original Intent

The original session goal is automatically saved by the intent-anker hook.
A reminder marker is written every 5 turns.

> **Finding the goal file (CRITICAL — platform-dependent):**
> - **macOS**: The file is at `$TMPDIR/claude-session-goal.txt` — on macOS `$TMPDIR` is NOT `/tmp/` but `/var/folders/.../T/`. Always use `${TMPDIR:-/tmp}/claude-session-goal.txt` or read the env var first.
> - **Windows**: `$env:TEMP/claude-session-goal.txt`
> - **Linux**: `/tmp/claude-session-goal.txt`
> - **Quick check**: Run `echo ${TMPDIR:-/tmp}` to find the correct directory.
> - The reminder file is at the same location: `${TMPDIR:-/tmp}/claude-intent-reminder.txt`

When working on a task that spans more than 10 tool calls:

1. **At the start**: Read the goal file (use `cat "${TMPDIR:-/tmp}/claude-session-goal.txt"`) to recall the user's original request
2. **Every ~5 turns** (when reminder file updates): Re-read the goal file and verify: "Am I still working toward this exact goal?"
3. **Before any major direction change**: Explicitly state what you're about to do differently and why
4. **If you notice drift**: Stop immediately and ask the user

## Why This Matters

In long sessions (100+ turns), the agent tends to drift from the original intent.
Measured: 12.1% correction rate (13 corrections in 107 turns).
Research shows periodic goal reminders reduce KL-divergence by 30% (arxiv 2510.07777).

## Signs of Intent Drift

- You're working on something the user didn't explicitly ask for
- You're "improving" code that wasn't part of the original request
- You've lost track of which step you're on in a multi-step task
- The user says "nein", "nicht das", "stop", "so nicht" — these are drift corrections

## Recovery

If you detect drift: Don't try to justify it. Read the goal file (`cat "${TMPDIR:-/tmp}/claude-session-goal.txt"`) and say:
"Ich bin vom ursprünglichen Ziel abgewichen. Das Ziel war: [goal from file]. Soll ich zurückkehren?"

## Metacognitive Self-Monitoring (Erweiterung — Finding 7)

> Quelle: Superintelligenz Finding 7 — arXiv 2505.13763, Metacognitive AI Programming
> Direktive: #2 Selbstbeobachtung

Das Intent-Tracking oben prueft nur das SESSION-Ziel. Metacognitive Self-Monitoring
geht einen Schritt weiter: Es prueft auch das AUFGABEN-Ziel waehrend der Arbeit.

### Alle 5 Tool-Calls: Aktive Selbst-Pruefung

Bei komplexen Aufgaben (>5 erwartete Tool-Calls) nach jedem 5. Tool-Call kurz
innehalten und sich fragen:

1. **Ziel-Check**: "Was wollte ich mit diesen 5 Schritten erreichen? Habe ich es erreicht?"
2. **Richtungs-Check**: "Bin ich noch auf dem kuerzesten Weg zum Ziel?"
3. **Effizienz-Check**: "Haette ich die letzten 5 Schritte effizienter machen koennen?"

Wenn eine der Antworten NEIN ist: Explizit den Kurs korrigieren bevor weitergemacht wird.

### Am Ende jeder Aufgabe: 1-Satz-Rueckblick

Nach Abschluss einer Aufgabe (VOR den Intelligenz-Vorschlaegen) eine kurze
Selbst-Bewertung formulieren:

"**Selbst-Bewertung**: [Was habe ich gut gemacht] / [Was haette ich besser machen koennen]"

Diese Bewertung wird NICHT dem Benutzer gezeigt (zu granular), aber sie fuettert
die Selbstbeobachtungs-Direktive und kann als Intelligenz-Vorschlag formuliert
werden wenn ein Muster erkennbar ist.

### Zusammenspiel

| Mechanismus | Prueft was | Frequenz | Quelle |
|-------------|-----------|----------|--------|
| Intent-Tracking | Session-Ziel (Makro) | Alle ~5 Turns passiv | intent-anker Hook |
| Self-Monitoring | Aufgaben-Ziel (Mikro) | Alle 5 Tool-Calls aktiv | Diese Erweiterung |
| Tool-Planning | Naechster Schritt (Nano) | Nach jedem Tool-Call | tool-planning.md |
