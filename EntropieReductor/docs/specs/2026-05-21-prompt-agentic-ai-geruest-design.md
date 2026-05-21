# Agentic-AI-Prompts: Gerüst für alle 3 Stufen

**Datum:** 2026-05-21
**Status:** ABGESCHLOSSEN — alle 15 Etappen + Aufteilungen 8a/8b + 9/9b/9c committed (#890 – #907)
**Vorgänger-Spec:** `../../PROMPT-PROJEKT-TODO.md`
**Sitzungsführung:** Brainstorming-Skill (superpowers) übersprungen auf Frank's Wunsch nach Sektion 2/6 — direktes Bauen mit Etappen-Commits.

---

## 1. Klärungs-Antworten der Planungs-Session (2026-05-21)

| # | Frage | Antwort | Implikation fürs Gerüst |
|---|-------|---------|-------------------------|
| 1 | Scope | Alle 3 Stufen komplett | 4-6 Tage über mehrere Sessions, 15 Etappen |
| 2 | Permissions | Kategorie-übergreifend (flexibel) | Read-Tools frei für alle Prompts, Write-Tools per `prompt_tool_permissions` einzeln freischaltbar. Kategorie ist nur fürs UI-Sortieren. |
| 3 | Trigger | Background via WorkManager | WorkManager-Dependency, eigener Notification-Channel, `prompt_triggers`-Tabelle mit `nextScheduledAt` |
| 4 | Modelle | Pro Prompt manuell wählen | Kein `ModelSelector`/Auto-Heuristik. Einfaches Dropdown im Prompt-Editor. Speicherung als `model: String` (z.B. `"gemini-2.5-pro"`) |
| 5 | Tokens | Zähler + optionales Limit pro Prompt | `TokenMeter` mit `token_usage_daily`-Tabelle, Balkendiagramm in Einstellungen, `tokenLimitPerDay: Int?` pro Prompt (null = kein Limit) |
| 6 | Vorlagen | 5-7 Beispiele mitgeliefert | "Vorlagen einfügen"-Button im Prompts-Screen |

---

## 2. Architektur (Ansatz B: Tool-Registry + Modular)

```
┌────────────────────────────────────────────────────────────┐
│                         UI (Compose)                        │
│  PromptsScreen ─ ExecutionDialog ─ HistoryScreen           │
│  TokenStatsScreen + Bar-Chart ─ ConfirmWriteDialog          │
│  TriggerConfigDialog ─ AuditLogScreen                       │
└──────────────────────────┬─────────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────────┐
│            ViewModel-Schicht (AgenticPromptViewModel)       │
│  beobachtet WorkflowRunner.events, steuert Confirm-Dialog   │
└──────────────────────────┬─────────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────────┐
│              domain/agentic/   (Gerüst-Kern)                │
│                                                             │
│   ToolRegistry ──► AgenticTool (interface)                  │
│        │                  ▲                                 │
│        │                  │ implementiert von 18 Tools      │
│   WorkflowRunner ◄────────┘                                 │
│        │                                                    │
│        ├── ReActLoop (max 20 Steps, Gemini ⇄ Tool-Call)    │
│        ├── PermissionGate    (Read frei, Write per Prompt)  │
│        ├── ConfirmationGate  (UI-Confirm für Writes)        │
│        ├── TokenMeter        (Tages-Aggregation + Limit)    │
│        └── ExecutionLogger   (schreibt prompt_executions)   │
└──────────────────────────┬─────────────────────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
      GeminiApi+   Existing Repos   New Repos
      FunctionCalls (Entry/Memory/   (PromptExecution
      (Erweiterung) Aufgaben/...)    PromptToolPerm
                                      TokenUsageDaily
                                      PromptTrigger)
                           │
                           ▼
                  Room AppDatabase
                  (Migration 21→22)
                           │
                           ▼
                WorkManager (Stufe 3)
                TriggerScheduler
                └── periodic + event-listener
```

---

## 3. Datenmodell-Änderungen (Migration 21 → 22)

### Erweiterung von `saved_prompts`

| Spalte | Typ | Default | Bedeutung |
|--------|-----|---------|-----------|
| `model` | TEXT NOT NULL | `'gemini-2.5-flash'` | Vom Nutzer gewähltes Gemini-Modell |
| `tokenLimitPerDay` | INTEGER NULL | `null` | null = kein Limit. Bei Überschreitung Workflow-Block |
| `trustModeDefault` | INTEGER NOT NULL | `0` | 1 = alle Write-Tools dieses Prompts ohne Confirm |

### 5 neue Tabellen

1. **`prompt_executions`** — Eine Zeile pro Ausführung mit Snapshot des Prompts (überlebt Prompt-Löschung).
2. **`prompt_execution_steps`** — Feinkörnige Schritte im ReAct-Loop (LLM_CALL / TOOL_CALL / USER_CONFIRM / BLOCKED) mit CASCADE auf `prompt_executions.id`.
3. **`prompt_tool_permissions`** — Write-Tool-Freischaltung pro Prompt. Read-Tools brauchen keinen Eintrag.
4. **`token_usage_daily`** — Tagesaggregierte Tokens pro Prompt für Balkendiagramm-Performance. Inkrementell vom ExecutionLogger gefüttert. Recompute-Button als Drift-Schutz.
5. **`prompt_triggers`** — Auto-Ausführungs-Konfiguration (MANUAL / CRON / EVENT / CHAIN).

### Audit-Log-Persistenz

`prompt_executions.promptId` hat **kein** Foreign-Key auf `saved_prompts` — Audit überlebt Prompt-Löschung. Snapshot-Felder (`snapshotName`, `snapshotContent`, etc.) machen den Eintrag eigenständig lesbar.

---

## 4. Etappen-Plan (15 Etappen, je 1 Commit + Push)

| # | Etappe | Status |
|---|--------|--------|
| 1 | DB-Migration 21→22 + 5 Entities + 3 SavedPromptEntity-Felder + DAOs + Repos | ✅ #890 |
| 2 | GeminiApi erweitern um Function-Calling | ✅ #891 |
| 3 | `AgenticTool` Interface + `ToolRegistry` + 1 Referenz-Read-Tool | ✅ #892 |
| 4 | 3 Gates: `PermissionGate`, `TokenMeter`, `ConfirmationGate` | ✅ #893 |
| 5 | `WorkflowRunner` (ReAct-Loop) + `ExecutionLogger` | ✅ #894 |
| 6 | Restliche 8 Read-Tools | ✅ #895 |
| 7 | 10 Write-Tools | ✅ #896 |
| 8a | UI: Ausführen-Knopf, Execution-Dialog | ✅ #897 |
| 8b | UI: History-Screen | ✅ #903 |
| 9 | EditDialog: Modell-Dropdown + Trust-Toggle | ✅ #899 |
| 9b | UI: Permission-Editor-Dialog | ✅ #900 |
| 9c | UI-ConfirmationGate (Sicherheit) | ✅ #901 |
| 10 | TokenStats-Screen mit Balkendiagramm + Limit-Einstellung | ✅ #898 |
| 11 | `TriggerScheduler` + WorkManager + Cron-Parser | ✅ #904 |
| 12 | Cross-Prompt-Read Tool + Chain-Trigger | ✅ #905 |
| 13 | 7 Beispiel-Vorlagen | ✅ #902 |
| 14 | `BackupPayload` v9 (Agentic-AI restorebar) | ✅ #906 |
| 15 | AuditLog-Polish: Filter-Chips + Pruning | ✅ #907 |

---

## 5. Wichtige Befunde aus dem Codebase-Check

- **`MemoryEntryEntity` + `InsightEntity` leben in `ScientistDatabase`**, nicht in `AppDatabase`. Read-Tools `read_memory` und `read_insights` (Etappe 6) müssen aus `ScientistDatabase` lesen.
- 18 bestehende Repositories sind direkt von Tools nutzbar — keine Veränderung nötig, nur Aufrufe.
- `GeminiApi.kt` (57 Zeilen) hat aktuell KEIN Function-Calling-Schema → Erweiterung um `tools`, `toolConfig`, `FunctionCall`, `FunctionResponse` in Etappe 2.
- DB nutzt `.fallbackToDestructiveMigration(dropAllTables = true)` als Sicherheitsnetz → unsere Migration MUSS sauber sein, sonst Datenverlust.
- `PromptCategory` hat 6 Werte (AUFGABEN, ENTROPIE, THESEN, ANALYSE, FORSCHER, CODEX) — wird in Stufe 1+ nur fürs UI-Sortieren verwendet, nicht für Permissions.

---

## 6. Sicherheits-Konzept (übernommen aus PROMPT-PROJEKT-TODO.md)

| Risiko | Schutz |
|--------|--------|
| KI löscht versehentlich Daten | Lösch-Tools brauchen doppelte Bestätigung |
| Endlos-Loop verbraucht Quota | Max 20 Tool-Calls pro Ausführung, max 100k Tokens, max 5 Min Laufzeit |
| Token-Quota überschritten | Pro-Prompt-Tageslimit (optional) → Status `BLOCKED_BY_TOKEN_LIMIT` |
| KI ändert Profil unkontrolliert | `update_profil` nicht in Tool-Default-Liste |
| Workflow-Run hängt | Timeout 60s pro Tool-Call, gesamt max 5 Min |
| Audit unzuverlässig | `prompt_executions` ohne FK auf saved_prompts — Löschungen können Audit nicht zerstören |

---

## 7. Resume-Anker für Folge-Sessions

**Trigger-Phrase:** "Mach weiter mit dem Prompt-Projekt" oder "Etappe N starten"

Folge-Session-Start:
1. Dieses Dokument lesen
2. `EntropieReductor/PROMPT-PROJEKT-TODO.md` als ursprüngliche Spec lesen
3. `git log --oneline | grep -i "prompt"` für bisherige Commits
4. Etappen-Tabelle in Sektion 4 checken: welche ist die nächste pending Etappe?
5. Pre-Flight-Plan für diese Etappe geben, dann bauen.

---
