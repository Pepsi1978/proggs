# Best Practices: Token- & Kosten-Effizienz (Kategorie 09)

> Stand: 2026-05-25 | Claude Code v2.1.150 | Quelle: offizielle Anthropic-Dokumentation

---

## 1. Modellwahl: Wann welches Modell?

### Aliases (Stand KW 20 2026)

| Alias | Modell | Wann verwenden |
|-------|--------|----------------|
| `sonnet` | Claude Sonnet 4.6 | Standard für alles: Implementation, Recherche, Reviews |
| `opus` | Claude Opus 4.7 | Architektur, komplexe Debugging-Sessions, tiefes Reasoning |
| `haiku` | Claude Haiku 3.5 | Einfache Textoperationen, schnelle Checks |
| `sonnet[1m]` | Sonnet 4.6 + 1M Context | Sehr große Codebases (max 200K Standard reicht meist) |
| `opus[1m]` | Opus 4.7 + 1M Context | Nur wenn Opus + sehr großer Kontext BEIDE nötig |
| `opusplan` | Opus für Plan-Phase, Sonnet für Ausführung | Planung komplex, Implementierung einfach |
| `best` | Bestes verfügbares Modell | Immer aktuell, kein Alias-Pflege nötig |

### Faustregeln

- **Sonnet** ist der Sweet Spot: 80% der Aufgaben, 30% der Kosten von Opus
- **Opus** nur wenn Sonnet nach 2 Versuchen scheitert oder Aufgabe nachweislich Reasoning braucht
- **`opusplan`-Falle**: Jeder Mode-Toggle (Plan ↔ Auto) = Modellwechsel = **Cache-Miss**. Nicht im Wechsel-Rhythmus nutzen.
- Subagents bekommen Sonnet per Default (`CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-4-6`)

---

## 2. Effort-Levels: Rechnerleistung gezielt steuern

```
/effort low    → Einfache Korrekturen, Formatierungen (~40% der Kosten von high)
/effort medium → Standard für mittlere Aufgaben (~70%)
/effort high   → Default bei Session-Start (Session-Guard setzt das automatisch)
/effort xhigh  → Opus 4.7 spezifisch: Extended Thinking mit mehr Budget
/effort max    → Gilt nur für aktuelle Session, kein Persistenz
```

**Wichtig:** Effort wird NUR über `/effort`-Command gesteuert, NICHT über `CLAUDE_CODE_EFFORT_LEVEL`-Env-Variable (blockiert `/effort`-Änderungen).

**KW 20 neu:** Hooks sehen das aktuelle Effort-Level im Event-Payload — Hooks können unterschiedlich reagieren je nach Effort.

---

## 3. Prompt-Caching: Wie es funktioniert

### Automatische Funktionsweise

Claude Code verwendet **automatisches Prompt-Caching** — prefix-basiert, server-seitig. Kein manuelles Opt-in nötig. Der Kontext wird gecacht sobald er groß genug ist (>1024 Tokens).

### Cache-TTL

| Kontext | TTL |
|---------|-----|
| API-Key-Nutzung | 5 Minuten |
| Subscription (Max/Team/Claude.ai) | **1 Stunde** (automatisch, kein Opt-in) |
| Via `ENABLE_PROMPT_CACHING_1H=1` | 1 Stunde auch für API-Key-Nutzer (Beta) |

### Was den Cache ERHÄLT

- Gleiche Konversation ohne Modellwechsel
- Anhängen von Text ans Ende (Präfix bleibt stabil)
- Kurze Tool-Ergebnis-Sequenzen die Kontext nicht stark verändern

### Was den Cache INVALIDIERT (= teuer!)

- `/clear` oder `/new` → neuer leerer Kontext
- Modellwechsel (auch über `/model`, `opusplan`-Toggle)
- MCP-Server Connect/Disconnect
- `/compact` → neuer komprimierter Kontext
- `/rewind` → rollt Kontext zurück
- Claude Code Upgrade (neue Version)
- Abrufen desselben Prompts nach Ablauf der TTL

### Steuerung über Env-Variablen

```bash
DISABLE_PROMPT_CACHING=1          # Vollständig deaktivieren (Debugging)
ENABLE_PROMPT_CACHING_1H=1        # 1h-TTL für API-Key-Nutzer aktivieren
```

---

## 4. Kontext-Management: Die größte Stellschraube

### CLAUDE.md: Kleiner halten = billiger

- **Ziel: unter 200 Zeilen** für CLAUDE.md
- Regeln die selten gelten → in Skill auslagern (wird on-demand geladen)
- Skills laden **on-demand** — sie landen NICHT bei jedem Turn im Kontext
- MCP-Tool-Definitionen: **deferred by default** — nur Namen im Kontext, vollständige Schemas erst bei Nutzung

### /clear zwischen unabhängigen Aufgaben

Wenn zwei Aufgaben nichts miteinander zu tun haben:
```
Aufgabe A erledigt → /clear → Aufgabe B starten
```
→ Spart Kontext-Overhead, reduziert Caching-Nachteile durch Kontextwachstum

### Subagents für verbose Operationen

Große Dateien lesen, lange Tool-Outputs analysieren, Recherche → in Subagent auslagern. Der Hauptkontext bleibt sauber.

---

## 5. Agent-Teams: Kosten-Falle kennen

Agent Teams (`/team`) verbrauchen **~7x mehr Token** als normale Sessions:
- Jeder Teammate hat eigenen Kontext
- Kommunikation zwischen Teammates kostet Token
- **Subagents ohne Kommunikationsbedarf**: Normale parallele Subagents bevorzugen

**Wann Agent Teams sinnvoll sind:**
- Aufgaben wo Frontend und Backend sich koordinieren müssen
- Iterative Arbeit wo Teammitglieder aufeinander reagieren sollen

**Wann NICHT:**
- Parallele unabhängige Recherchen → einfach 3-5 `researcher`-Agents parallel
- Parallele Implementierungen → `coder`-Agents mit Datei-Ownership

---

## 6. Hintergrund-Kosten kennen

- **Hooks**: Laufen bei jedem Tool-Call → kurz halten, kein unnötiges Logging
- **Background Sessions** (Claude.ai): ~$0.04/Session für initiales Prefill
- **MCP-Server**: Jeder angeschlossene Server vergrößert den Kontext leicht
- **Subagent-Caching**: Subagents bekommen eigenen Cache mit 5-Min-TTL (auch bei Subscription)

---

## 7. Zusammenfassung: Die 7 wichtigsten Hebel

1. **Sonnet statt Opus** für 80% der Aufgaben
2. **`/effort low/medium`** für einfache Aufgaben — nicht immer `high`
3. **CLAUDE.md unter 200 Zeilen** halten, Rest in Skills
4. **`/clear` zwischen unabhängigen Aufgaben** statt Session aufblähen
5. **Subagents für verbose Ops** (Kontext-Schonung im Haupt-Agent)
6. **Parallele Subagents** statt Agent-Teams wo keine Kommunikation nötig
7. **Modellwechsel vermeiden** während einer Aufgabe (Cache-Invalidierung)
