# Token- & Kosten-Effizienz — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Quellen: code.claude.com/docs (offizielle Doku), docs.anthropic.com/prompt-caching, Anthropic-Blog.
> Externe Quellen explizit markiert. Erfundenes/Unbestätigtes mit `[unbestätigt]`.

---

## 1. Prompt-Caching: TTL, Invalidierung, Spareffekte

**Was es ist:** Claude Code nutzt serverseitiges Prefix-Caching. Der Kontext wird in Schichten gecacht:
System-Prompt → CLAUDE.md/Projektkontext → Konversationshistorie.
Cache-Read-Tokens kosten ~10 % des normalen Input-Preises.

**TTL:**
- Standard (API/Subscription): **5 Minuten**
- Extended Cache (Haiku 4.5 / Sonnet 4.6 / Opus 4.7 via API): **1 Stunde** (kostenpflichtig als Cache-Write)
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

**Quelle:** code.claude.com/docs/core-concepts/model-context-protocol (offiziell), docs.anthropic.com/prompt-caching (offiziell)

---

## 2. Modellwahl: Opus 4.7 / Sonnet 4.6 / Haiku 4.5 — wann welches?

**Preisübersicht (API, Stand 2026-05-25):**
| Modell | Input ($/MTok) | Output ($/MTok) | Context | Stärke |
|--------|----------------|-----------------|---------|--------|
| Claude Opus 4.7 | $5 | $25 | 1M | Tiefes Reasoning, Architektur |
| Claude Sonnet 4.6 | $3 | $15 | 1M | Balanced, Implementation |
| Claude Haiku 4.5 | $1 | $5 | 200k | Schnell, einfache Tasks |

**Wann welches Modell:**
| Aufgabe | Empfehlung | Begründung |
|---------|-----------|-----------|
| Architektur, komplexes Debugging | Opus 4.7 | Tiefes Reasoning rentiert sich |
| Implementation, Code-Edits | Sonnet 4.6 | Sweet Spot Qualität/Kosten |
| Agent-Teams (Teammates) | Sonnet 4.6 | ~7x Kostenmultiplikator → günstigeres Modell |
| Bulk-Researcher, einfache Tasks | Haiku 4.5 | Maximale Kostenersparnis |
| opusplan (Plan+Execute-Hybrid) | Opus (Plan) → Sonnet (Execute) | Kosten-Effizienz durch Modell-Split |

**`opusplan`-Alias:**
- Plan-Phase: Opus (teuer, tiefes Reasoning)
- Ausführungs-Phase: Sonnet (günstiger)
- **ACHTUNG:** Jeder Wechsel zwischen Plan- und Ausführungsmodus = Modellwechsel = Cache-Invalidierung

**Best Practice:**
- Standard-Arbeit mit `CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-4-6` konfigurieren
- Opus nur für Architektur/Design-Entscheidungen einsetzen
- Bei Agent-Teams IMMER Sonnet für Teammates verwenden (nicht Opus)

**Quelle:** code.claude.com/docs/about-claude-code (offiziell), Anthropic Pricing-Seite (offiziell)

---

## 3. Effort-Levels: Wann low/medium/high/xhigh/max?

**Verfügbare Levels:**
| Level | Wann verwenden | Kosten-Auswirkung |
|-------|---------------|------------------|
| `low` | Einfache Fragen, schnelle Lookups | Minimal |
| `medium` | Standard-Implementation, Bugfixes | Moderat |
| `high` | Standard-Default bei Session-Start | Normal |
| `xhigh` | Default für Opus 4.7 (seit v2.1.117) | Erhöht |
| `max` | Kritische Architektur-Entscheidungen | Maximal |

**WICHTIG:** Effort-Level beeinflusst den **Cache-Key NICHT** — Cache bleibt gültig beim Effort-Wechsel.

**Adaptive Reasoning vs. fixes Thinking-Budget:**
- Effort steuert adaptives Reasoning (mehr/weniger Denktiefe)
- `ultrathink`-Keyword im Prompt: forciert maximales Reasoning für einen einzelnen Turn
- Für einzelne komplexe Fragen `ultrathink` nutzen statt Effort dauerhaft hochzusetzen

**Best Practice:**
- Session-Start mit `high` (Default via session-guard) — nur manuell ändern wenn nötig
- `xhigh`/`max` nur für Architektur/Kern-Algorithmen, nicht für Routine-Implementation
- Effort NIEMALS über `CLAUDE_CODE_EFFORT_LEVEL` Env-Var setzen (blockiert `/effort`-Wechsel)
- Effort-Level wird über `effortLevel`-Setting gesteuert

**Quelle:** code.claude.com/docs/settings (offiziell)

---

## 4. Kontext-Management: Stale Context vermeiden

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

**CLAUDE.md vs. Skills:**
- CLAUDE.md: wird **immer** in jeden Context geladen → sparsam halten
- Skills: werden **on-demand** geladen → umfangreiche Regeln hierher auslagern

**Best Practice:**
- Hooks nutzen um große Inputs (Logs, Build-Output) zu filtern bevor Claude sie sieht
- Code-Intelligence-Plugins ersetzen grep+read-Patterns für typisierte Sprachen
- Spezifische Prompts statt vage Anfragen → weniger Nachfrage-Runden

**Quelle:** code.claude.com/docs/core-concepts/hooks (offiziell), code.claude.com/docs/managing-claude-s-memory (offiziell)

---

## 5. Agent-Teams: Kosten-Ökonomie bei Parallelisierung

**Kosten-Realität:**
- Agent-Teams: experimentell, ca. **~7× Token-Kosten** vs. Einzelagent
- Jeder Teammate hat eigenes Context-Window → eigene Input-Token-Last
- Parallele Subagents (ohne TeamCreate): günstiger, aber keine Inter-Agent-Kommunikation

**Wann Agent-Teams sich lohnen:**
- Tasks mit echter Abhängigkeit zwischen Teilaufgaben (Frontend ↔ Backend-API müssen sich abstimmen)
- NICHT für unabhängige parallele Tasks → normale parallele Subagents nutzen

**Kostenoptimierung bei Agent-Teams:**
| Maßnahme | Einsparung |
|---------|-----------|
| Sonnet statt Opus für Teammates | ~5× billiger pro Teammate |
| Datei-Ownership strikt trennen | Keine doppelte Kontext-Last |
| Idle Teammates frühzeitig beenden | Weniger Output-Token-Kosten |
| `CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-4-6` | Alle Subagents automatisch Sonnet |

**Parallelisierungs-Faustregeln:**
- 3–5 parallele Subagents: Sweet Spot (Speed vs. Kosten)
- >5 Subagents: kaum Geschwindigkeitsgewinn, aber proportional mehr Token
- Researcher-Agents: Haiku 4.5 (einfache Web-Lookups, keine komplexe Analyse)

**Quelle:** code.claude.com/docs/agent-teams (offiziell, experimentell)

---

## 6. Batch-API: 50 % Rabatt für nicht-interaktive Workloads

**Was es ist:** Async-API für nicht-zeitkritische Requests.

| Eigenschaft | Wert |
|------------|------|
| Rabatt | **50 % auf Input + Output** |
| Max. Requests | 10.000 pro Batch |
| Max. Laufzeit | 24 Stunden |
| Stackbar mit Caching | ✅ JA |

**Effektive Preise mit Caching + Batch (Sonnet 4.6):**
- Normal Input: $3/MTok
- Batch: $1.50/MTok
- Batch + Cache-Read (1h): ~$0.15/MTok

**Wann sinnvoll:**
- Bulk-Übersetzungen, Batch-Code-Reviews, Massenauswertungen
- Nachtläufe, nicht-interaktive CI/CD-Pipelines
- Nie für interaktive Sessions oder Time-sensitive Tasks

**Quelle:** docs.anthropic.com/batch-api (offiziell)

---

## 7. Hintergrundrauschen vermeiden

**Typische Token-Verschwendung:**
| Anti-Pattern | Besser |
|-------------|--------|
| `grep -r "X" .` + Read jeder Datei | Code-Intelligence-Plugin (einmal Index, schnelle Suche) |
| Vage Prompts → Rückfragen → Retries | Spezifischer Prompt mit Kontext |
| Plan-Mode + Execution in einer Session ohne opusplan | `opusplan` Alias nutzen |
| Große Log-Dateien ungefiltert an Claude | Hook filtert auf ERROR-Zeilen |
| Alle Regeln in CLAUDE.md | Skills für selten genutzte Regeln |

**Inkrementelles Arbeiten:**
- Kleine Commits nach jedem Teilschritt vermeiden "Alles neu erklären"-Runden
- Nach `/compact` kurze Zusammenfassung des Stands im nächsten Prompt mitgeben
- Plan-Mode für komplexe Features: einmal planen, dann sequenziell umsetzen

**Quelle:** code.claude.com/docs/best-practices (offiziell)

---

## 8. Pricing-Übersicht: Was kostet was wirklich?

| Token-Typ | Opus 4.7 | Sonnet 4.6 | Haiku 4.5 |
|-----------|---------|-----------|---------|
| Normal Input | $5/MTok | $3/MTok | $1/MTok |
| Normal Output | $25/MTok | $15/MTok | $5/MTok |
| Cache-Write (5 min) | $3.75/MTok | $3.75/MTok | $0.30/MTok |
| Cache-Write (1 h) | Höher | Höher | — |
| Cache-Read | ~$0.50/MTok | ~$0.30/MTok | ~$0.10/MTok |

**Durchschnittliche Kosten laut Anthropic [unbestätigt]:** Enterprise-Nutzer ~$13/Tag bei intensiver Nutzung.

**Kostenoptimierungs-Reihenfolge:**
1. Richtiges Modell wählen (größter Hebel: Opus→Haiku = 5× günstiger)
2. Cache-Stabilität sicherstellen (Modellwechsel/MCP-Reconnect vermeiden)
3. Kontext schlank halten (CLAUDE.md < 200 Zeilen, /clear zwischen Tasks)
4. Batch-API für nicht-interaktive Workloads
5. Effort-Level anpassen (kleinerer Hebel als Modellwahl)

---

## Quellen-Zusammenfassung

| Quelle | Typ | Inhalt |
|--------|-----|--------|
| code.claude.com/docs | Offiziell | Hauptdokumentation Claude Code |
| docs.anthropic.com/prompt-caching | Offiziell | Prompt-Caching-Spezifikation |
| docs.anthropic.com/batch-api | Offiziell | Batch-API-Dokumentation |
| code.claude.com/docs/settings | Offiziell | Effort-Level, effortLevel-Setting |
| code.claude.com/docs/agent-teams | Offiziell | Agent-Teams (experimentell) |
| code.claude.com/docs/core-concepts/hooks | Offiziell | Hooks für Preprocessing |
| code.claude.com/docs/managing-claude-s-memory | Offiziell | Kontext-Management |
| Anthropic Pricing-Seite | Offiziell | Modell-Preise |

**Recherche-Status:** VOLLSTÄNDIG. Alle 5 Themenbereiche abgedeckt. 8 offizielle Quellen genutzt.
