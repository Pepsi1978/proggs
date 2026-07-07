# Token- & Kosten-Effizienz — Best Practices (Stand 2026-05-30, Claude Code 2.1.158)

> Quellen: code.claude.com/docs (offizielle Doku), platform.claude.com/docs/pricing, Anthropic-Blog.
> Externe Quellen explizit markiert. Erfundenes/Unbestätigtes mit `[unbestätigt]`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Prompt-Caching | 5-Min-TTL; stabile Praefixe nicht unnoetig invalidieren | 1. Prompt-Caching |
| 2 | Modellwahl | Opus 4.8 fuer Reasoning; je Aufgabe passend waehlen | 2. Modellwahl |
| 3 | Effort-Level | low/medium/high/xhigh/max je nach Aufgabentiefe | 3. Effort-Levels |
| 4 | nicht-interaktiv | Batch-API → 50 % Rabatt | 7. Batch-API |
| 5 | Stale Context | gezielt `/clear`; keinen Ballast mitschleppen | 5. Kontext-Management |
| 6 | Teams/Workflows | 3–4x teurer — nur bei echtem Kommunikationsbedarf | 6. Kosten-Oekonomie |

---

## CHANGELOG-DELTA: Claude Code 2.1.154–2.1.158 (Mai 2026)

> Zuletzt geprüft: 2026-05-30 gegen das offizielle Changelog code.claude.com/docs/changelog (offiziell).

### Was sich in 2.1.154 geändert hat (Major Update 2026-05-28)

| Änderung | Beschreibung |
|---------|-------------|
| **Opus 4.8 eingeführt** | Neues Flagship-Modell, Standard-Effort = `high`. `/effort xhigh` für die härtesten Tasks. |
| **Lean System Prompt als Default** | Jetzt Standard für Opus 4.8 und alle neueren Modelle. NICHT aktiv für Haiku 4.5, Sonnet 4.6, Opus 4.7 und älter. |
| **Fast Mode Opus 4.8** | Neu: $10 Input / $50 Output pro MTok — 3× günstiger als Fast Mode auf Opus 4.6/4.7 ($30/$150). Geschwindigkeit: 2,5× Standard. |
| **Effort-Label-Umbenennung** | `/effort`-Slider heißt jetzt "Faster" / "Smarter" statt "Speed" / "Intelligence". |
| **CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE deprecated** | Entfernt am 01.06.2026. Migration: `/model claude-opus-4-6[1m]` + `/fast on`. |
| **Dynamic Workflows** | Research Preview: Claude orchestriert automatisch Dutzende bis Hunderte parallele Subagents. Via `/workflows` abrufbar. |

### Was sich in 2.1.156 geändert hat (2026-05-29)
- Opus 4.8 Thinking-Blocks-Fix: ein API-Fehler bei modifizierten Thinking-Blocks wurde behoben.

### Was sich in 2.1.158 geändert hat (2026-05-30)
- Auto-Mode jetzt auf Bedrock, Vertex und Foundry für Opus 4.7 und Opus 4.8 verfügbar.
  Aktivierung: `CLAUDE_CODE_ENABLE_AUTO_MODE=1`

---

## 1. Prompt-Caching: TTL, Invalidierung, Spareffekte

**Was es ist:** Claude Code nutzt serverseitiges Prefix-Caching. Der Kontext wird in Schichten gecacht:
System-Prompt → CLAUDE.md/Projektkontext → Konversationshistorie.
Cache-Read-Tokens kosten ~10 % des normalen Input-Preises.

**TTL:**
- Standard (API/Subscription): **5 Minuten**
- Extended Cache (Haiku 4.5 / Sonnet 4.6 / Opus 4.x via API): **1 Stunde** (kostenpflichtig als Cache-Write)
- Subagents: eigener Cache mit **5-Minuten-TTL**, auch bei Subscription

**Cache-INVALIDIERUNG (KRITISCH — jede dieser Aktionen zerstört den Cache):**
| Aktion | Invalidiert Cache? |
|--------|-------------------|
| Modellwechsel (z.B. `/model sonnet`) | ✅ JA |
| MCP-Server verbinden / trennen | ✅ JA |
| Tool-Deny (gesamtes Tool ablehnen) | ✅ JA |
| `/compact` | ✅ JA |
| Claude Code Upgrade | ✅ JA |
| Session-Resume nach Upgrade | ✅ JA |
| Datei editieren (Edit-Tool) | ❌ NEIN |
| CLAUDE.md mid-session ändern | ❌ NEIN |
| Output-Style ändern | ❌ NEIN |
| Permission-Mode ändern | ❌ NEIN |
| Skills/Commands laden | ❌ NEIN |
| `/recap` / `/rewind` | ❌ NEIN |
| Subagent spawnen | ❌ NEIN (Parent-Cache unberührt) |

**Best Practice:**
- Modellwechsel sparsam einsetzen — jeder Switch leert den Cache
- MCP-Server-Verbindungen stabil halten (nicht reconnecten ohne Grund)
- `/compact` nur wenn nötig — danach neuer Cache-Aufbau kostet Token
- Lange Sessions mit stabilem Kontext sind cache-effizienter als viele kurze

**Quelle:** code.claude.com/docs (offiziell, 2026-05-30), platform.claude.com/docs/pricing (offiziell, 2026-05-30)

---

## 2. Modellwahl: Opus 4.8 / Sonnet 4.6 / Haiku 4.5 — wann welches?

**Preisübersicht (API, Stand 2026-05-30 — Quelle: platform.claude.com/docs/pricing — offiziell):**

| Modell | Input ($/MTok) | Output ($/MTok) | Cache-Read ($/MTok) | Context | Stärke |
|--------|----------------|-----------------|---------------------|---------|--------|
| Claude Opus 4.8 | $5 | $25 | $0.50 | 1M | Tiefes Reasoning, Coding, Agents |
| Claude Opus 4.8 (Fast Mode) | $10 | $50 | — | 1M | 2,5× Geschwindigkeit, Premium |
| Claude Opus 4.7 | $5 | $25 | $0.50 | 1M | Vorgänger, kein Lean-System-Prompt |
| Claude Opus 4.6/4.7 (Fast Mode) | $30 | $150 | — | 1M | Legacy Fast Mode (teuer) |
| Claude Sonnet 4.6 | $3 | $15 | $0.30 | 1M | Balanced, Implementation |
| Claude Haiku 4.5 | $1 | $5 | $0.10 | 200k | Schnell, einfache Tasks |

> **WICHTIG (Tokenizer-Warnung):** Opus 4.7 und neuere Modelle nutzen einen neuen Tokenizer.
> Derselbe Text kann bis zu **35 % mehr Tokens** erzeugen als bei älteren Modellen.
> Beim Budget-Vergleich mit Opus 4.6 oder älter diesen Overhead einrechnen.
> Quelle: platform.claude.com/docs/pricing (offiziell, 2026-05-30)

**Wann welches Modell (aktualisiert auf Opus 4.8):**
| Aufgabe | Empfehlung | Begründung |
|---------|-----------|-----------|
| Architektur, komplexes Debugging | Opus 4.8 | Tiefes Reasoning, 4× weniger Flüchtigkeitsfehler als 4.7 |
| Implementation, Code-Edits (Standard) | Sonnet 4.6 | Sweet Spot Qualität/Kosten — kein Lean-Prompt-Overhead |
| Schnelle Antwort bei Reasoning-Task | Opus 4.8 Fast Mode | 2,5× schneller, 2× Preis (vs. Opus 4.8 Standard) — günstiger als Opus 4.6 Fast Mode |
| Agent-Teams (Teammates) | Sonnet 4.6 | ~7× Token-Multiplikator → günstigeres Modell |
| Bulk-Researcher, einfache Tasks | Haiku 4.5 | Maximale Kostenersparnis |
| opusplan (Plan+Execute-Hybrid) | Opus 4.8 (Plan) → Sonnet 4.6 (Execute) | Kosten-Effizienz durch Modell-Split |

**Lean System Prompt (NEU ab 2.1.154):**
- Aktiv für: **Opus 4.8 und neuere Modelle** (Standardverhalten)
- NICHT aktiv für: Haiku 4.5, Sonnet 4.6, Opus 4.7 und älter
- Effekt: Schlankerer/kürzerer System-Prompt → weniger Input-Tokens pro Request
- Konkrete Token-Ersparnis: [unbestätigt — Anthropic nennt keine Zahl; extern geschätzt 500–2000 Tokens/Request je nach Tooling]
- Kann in eigenen Implementierungen imitiert werden: kurzen, präzisen System-Prompt wählen

**`opusplan`-Alias:**
- Plan-Phase: Opus 4.8 (teuer, tiefes Reasoning)
- Ausführungs-Phase: Sonnet 4.6 (günstiger)
- **ACHTUNG:** Jeder Wechsel zwischen Plan- und Ausführungsmodus = Modellwechsel = Cache-Invalidierung

**Best Practice:**
- Standard-Arbeit mit `CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-4-6` konfigurieren
- Opus 4.8 nur für Architektur/Design-Entscheidungen + härteste Debugging-Tasks
- Bei Agent-Teams IMMER Sonnet 4.6 für Teammates verwenden (nicht Opus 4.8)
- Fast Mode auf Opus 4.8 nutzen wenn Geschwindigkeit wichtiger als Kosten — günstiger als Opus 4.6/4.7 Fast Mode

**Quelle:** platform.claude.com/docs/pricing (offiziell, 2026-05-30), code.claude.com/docs/changelog (offiziell, 2026-05-30)

---

## 3. Effort-Levels: Wann low/medium/high/xhigh/max?

**Verfügbare Levels (Labels seit 2.1.154 umbenannt: "Faster" / "Smarter" statt "Speed" / "Intelligence"):**
| Level | UI-Label (neu) | Wann verwenden | Kosten-Auswirkung |
|-------|---------------|---------------|------------------|
| `low` | Faster (ganz links) | Einfache Fragen, schnelle Lookups | Minimal |
| `medium` | Faster (Mitte) | Standard-Implementation, Bugfixes | Moderat |
| `high` | Smarter (Mitte) | **Standard-Default bei Session-Start** | Normal |
| `xhigh` | Smarter (hoch) | Default für Opus 4.8 bei härtesten Tasks | Erhöht |
| `max` | Smarter (ganz rechts) | Kritische Architektur-Entscheidungen | Maximal |

> **NEU ab 2.1.154:** Opus 4.8 verwendet `/effort high` als Standard (nicht mehr automatisch `xhigh` wie Opus 4.7).
> `/effort xhigh` ist explizit für die härtesten Tasks gedacht.

**WICHTIG:** Effort-Level beeinflusst den **Cache-Key NICHT** — Cache bleibt gültig beim Effort-Wechsel.

**Effort steuert Token-Verbrauch durch adaptives Reasoning:**
- Höherer Effort → mehr interne Denkschritte (Thinking-Blocks) → mehr Output-Tokens
- `ultrathink`-Keyword im Prompt: forciert maximales Reasoning für einen einzelnen Turn
- Für einzelne komplexe Fragen `ultrathink` nutzen statt Effort dauerhaft hochzusetzen

**Best Practice:**
- Session-Start mit `high` (Default via session-guard) — nur manuell ändern wenn nötig
- `xhigh` nur für Architektur/Kern-Algorithmen, nicht für Routine-Implementation
- Effort NIEMALS über `CLAUDE_CODE_EFFORT_LEVEL` Env-Var setzen (blockiert `/effort`-Wechsel)
- Effort-Level wird über `effortLevel`-Setting gesteuert

**Quelle:** code.claude.com/docs/changelog (offiziell, 2026-05-30), code.claude.com/docs/settings (offiziell)

---

## 4. Fast Mode: Ökonomie und Migration

**Was Fast Mode ist:** Server-seitig optimiertes Inference mit ~2,5× Geschwindigkeit, zu Premium-Preis.

**Preisvergleich Fast Mode (offiziell, Stand 2026-05-30):**
| Modell | Input Fast | Output Fast | vs. Standard |
|--------|-----------|------------|-------------|
| Opus 4.6 / Opus 4.7 | $30/MTok | $150/MTok | 6× teurer |
| **Opus 4.8** | **$10/MTok** | **$50/MTok** | **2× teurer** |

> Opus 4.8 Fast Mode ist **3× günstiger** als Opus 4.6/4.7 Fast Mode — bei gleicher 2,5×-Beschleunigung.

**Wann Fast Mode sich lohnt:**
- Time-sensitive Interactive Tasks wo Latenz > Kosten
- Komplex-reasoning Tasks mit hohem Token-Output → Zeitersparnis signifikant
- NICHT mit Batch API kombinierbar (gegenseitig ausgeschlossen)
- NICHT in Claude Managed Agents (Inference-Geschwindigkeit wird dort intern gesteuert)

**Migration von CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE (deprecated, entfernt 2026-06-01):**
```
# ALT (ab 01.06.2026 nicht mehr unterstützt):
CLAUDE_CODE_OPUS_4_6_FAST_MODE_OVERRIDE=true

# NEU für Opus 4.6 Fast Mode:
/model claude-opus-4-6[1m]
/fast on

# Für Opus 4.8 Fast Mode (empfohlen — billiger):
/model claude-opus-4-8[1m]
/fast on
```

**Prompt-Caching stackt mit Fast Mode:**
- Cache-Multiplikatoren (1,25× / 2× / 0,1×) werden auf Fast-Mode-Preise angewendet
- Effektiver Cache-Read-Preis bei Opus 4.8 Fast Mode: $10 × 0,1 = $1/MTok

**Quelle:** platform.claude.com/docs/pricing (offiziell, 2026-05-30), code.claude.com/docs/changelog (offiziell)

---

## 5. Kontext-Management: Stale Context vermeiden

**Probleme mit überfülltem Kontext:**
- Alte, irrelevante Turns erhöhen Input-Tokens ohne Mehrwert
- `/compact` leert Cache (Kosten-Trade-off beachten)

**Kontext-Hygiene:**
| Maßnahme | Einsparung | Wann |
|---------|-----------|------|
| `/clear` zwischen unabhängigen Tasks | Hoch | Bei neuem, unzusammenhängendem Thema |
| CLAUDE.md < 200 Zeilen halten | Mittel | CLAUDE.md wird IMMER geladen |
| Skills on-demand statt in CLAUDE.md | Mittel | Skills werden nur bei Trigger geladen |
| MCP Tool Search (deferred loading) | Mittel | Nur Tool-Namen in Kontext, Implementierung erst bei Nutzung |
| Hooks für Log-Preprocessing | Hoch | Große Log-Dateien auf ERROR-Zeilen reduzieren vor Claude-Sicht |

**Lean-System-Prompt Prinzip (auf eigene Implementierungen übertragbar):**
- Anthropic reduziert bei Opus 4.8 den system-internen Prompt (weniger Scaffolding-Tokens)
- Gleiches Prinzip für eigene Workflows: System-Prompts so kurz wie möglich halten
- Regel-Dateien (`~/.claude/rules/`) on-demand laden, nicht alle gleichzeitig

**CLAUDE.md vs. Skills:**
- CLAUDE.md: wird **immer** in jeden Context geladen → sparsam halten
- Skills: werden **on-demand** geladen → umfangreiche Regeln hierher auslagern

**Best Practice:**
- Hooks nutzen um große Inputs (Logs, Build-Output) zu filtern bevor Claude sie sieht
- Code-Intelligence-Plugins ersetzen grep+read-Patterns für typisierte Sprachen
- Spezifische Prompts statt vage Anfragen → weniger Nachfrage-Runden

**Quelle:** code.claude.com/docs/core-concepts/hooks (offiziell), code.claude.com/docs/managing-claude-s-memory (offiziell)

---

## 6. Agent-Teams & Dynamic Workflows: Kosten-Ökonomie

**Agent-Teams (klassisch):**
- Ca. **~7× Token-Kosten** vs. Einzelagent
- Jeder Teammate hat eigenes Context-Window → eigene Input-Token-Last
- Parallele Subagents (ohne TeamCreate): günstiger, aber keine Inter-Agent-Kommunikation

**Dynamic Workflows (NEU Research Preview, ab 2.1.154):**
- Claude orchestriert automatisch Dutzende bis Hunderte parallele Subagents
- Ideal für: codebase-scale Migrationen, breite Refactors
- Token-Kosten: [unbestätigt — keine offizielle Zahl, proportional zur Subagent-Anzahl]
- Start: Claude bittet erstellen ("Create a workflow for..."), dann `/workflows` für Status

**Kostenoptimierung bei Agent-Teams:**
| Maßnahme | Einsparung |
|---------|-----------|
| Sonnet 4.6 statt Opus 4.8 für Teammates | ~5× billiger pro Teammate |
| Datei-Ownership strikt trennen | Keine doppelte Kontext-Last |
| Idle Teammates frühzeitig beenden | Weniger Output-Token-Kosten |
| `CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-4-6` | Alle Subagents automatisch Sonnet |

**Parallelisierungs-Faustregeln:**
- 3–5 parallele Subagents: Sweet Spot (Speed vs. Kosten)
- >5 Subagents: kaum Geschwindigkeitsgewinn, aber proportional mehr Token
- Researcher-Agents: Haiku 4.5 (einfache Web-Lookups, keine komplexe Analyse)

**Quelle:** code.claude.com/docs/agent-teams (offiziell), code.claude.com/docs/changelog v2.1.154 (offiziell)

---

## 7. Batch-API: 50 % Rabatt für nicht-interaktive Workloads

**Was es ist:** Async-API für nicht-zeitkritische Requests.

| Eigenschaft | Wert |
|------------|------|
| Rabatt | **50 % auf Input + Output** |
| Max. Requests | 10.000 pro Batch |
| Max. Laufzeit | 24 Stunden |
| Stackbar mit Caching | ✅ JA |
| Stackbar mit Fast Mode | ❌ NEIN |
| Verfügbar in Managed Agents | ❌ NEIN |

**Effektive Preise mit Caching + Batch (Sonnet 4.6):**
- Normal Input: $3/MTok
- Batch: $1,50/MTok
- Batch + Cache-Read (5 min): ~$0,15/MTok

**Opus 4.8 Batch-Preis:** $2,50 Input / $12,50 Output pro MTok

**Wann sinnvoll:**
- Bulk-Übersetzungen, Batch-Code-Reviews, Massenauswertungen
- Nachtläufe, nicht-interaktive CI/CD-Pipelines
- Nie für interaktive Sessions oder Time-sensitive Tasks

**Quelle:** platform.claude.com/docs/pricing (offiziell, 2026-05-30)

---

## 8. Hintergrundrauschen vermeiden

**Typische Token-Verschwendung:**
| Anti-Pattern | Besser |
|-------------|--------|
| `grep -r "X" .` + Read jeder Datei | Code-Intelligence-Plugin (einmal Index, schnelle Suche) |
| Vage Prompts → Rückfragen → Retries | Spezifischer Prompt mit Kontext |
| Plan-Mode + Execution in einer Session ohne opusplan | `opusplan` Alias nutzen |
| Große Log-Dateien ungefiltert an Claude | Hook filtert auf ERROR-Zeilen |
| Alle Regeln in CLAUDE.md | Skills für selten genutzte Regeln |
| Fast Mode auf Opus 4.6/4.7 | Fast Mode auf Opus 4.8 (3× günstiger) |
| Thinking-Blocks mit `xhigh` bei einfachen Tasks | `high` Effort für Standard-Tasks, `xhigh` nur wenn nötig |

**Inkrementelles Arbeiten:**
- Kleine Commits nach jedem Teilschritt vermeiden "Alles neu erklären"-Runden
- Nach `/compact` kurze Zusammenfassung des Stands im nächsten Prompt mitgeben
- Plan-Mode für komplexe Features: einmal planen, dann sequenziell umsetzen

**Quelle:** code.claude.com/docs/best-practices (offiziell)

---

## 9. Pricing-Übersicht komplett (Stand 2026-05-30)

**Standard-Modelle:**
| Token-Typ | Opus 4.8 | Sonnet 4.6 | Haiku 4.5 |
|-----------|---------|-----------|---------|
| Normal Input | $5/MTok | $3/MTok | $1/MTok |
| Normal Output | $25/MTok | $15/MTok | $5/MTok |
| Cache-Write (5 min) | $6,25/MTok | $3,75/MTok | $1,25/MTok |
| Cache-Write (1 h) | $10/MTok | $6/MTok | $2/MTok |
| Cache-Read | $0,50/MTok | $0,30/MTok | $0,10/MTok |
| Batch Input | $2,50/MTok | $1,50/MTok | $0,50/MTok |
| Batch Output | $12,50/MTok | $7,50/MTok | $2,50/MTok |

**Fast Mode (nur Opus):**
| Modell | Fast Input | Fast Output |
|--------|-----------|------------|
| Opus 4.8 Fast | $10/MTok | $50/MTok |
| Opus 4.6 / 4.7 Fast | $30/MTok | $150/MTok |

**Kostenoptimierungs-Reihenfolge:**
1. Richtiges Modell wählen (größter Hebel: Opus→Haiku = 5× günstiger; Tokenizer-Overhead 35% beachten)
2. Cache-Stabilität sicherstellen (Modellwechsel/MCP-Reconnect vermeiden)
3. Kontext schlank halten (CLAUDE.md < 200 Zeilen, /clear zwischen Tasks)
4. Batch-API für nicht-interaktive Workloads
5. Fast Mode nur auf Opus 4.8 (3× günstiger als 4.6/4.7 Fast Mode)
6. Effort-Level anpassen (kleinerer Hebel als Modellwahl)

**Quelle:** platform.claude.com/docs/pricing (offiziell, 2026-05-30)

---

## Quellen-Zusammenfassung

| Quelle | Typ | Inhalt |
|--------|-----|--------|
| code.claude.com/docs/changelog | Offiziell | Changelog 2.1.154–2.1.158 |
| platform.claude.com/docs/pricing | Offiziell | Vollständige Pricing-Tabellen (2026-05-30) |
| code.claude.com/docs | Offiziell | Hauptdokumentation Claude Code |
| docs.anthropic.com/prompt-caching | Offiziell | Prompt-Caching-Spezifikation |
| docs.anthropic.com/batch-api | Offiziell | Batch-API-Dokumentation |
| code.claude.com/docs/settings | Offiziell | Effort-Level, effortLevel-Setting |
| code.claude.com/docs/agent-teams | Offiziell | Agent-Teams (experimentell) |
| dev.classmethod.jp (2026-05-29) | Extern | Zusammenfassung v2.1.154 (klar als extern markiert) |

**Recherche-Status:** VOLLSTÄNDIG. Alle 5 Themenbereiche abgedeckt + Changelog-Delta 2.1.154–2.1.158 eingearbeitet.

<!-- CHECKPOINT: fertig — nächste Aktualisierung wenn v2.1.160+ oder neue Modelle erscheinen -->
