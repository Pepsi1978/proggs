# ClaudeWatch: MCP-Integration & Session-Protokolle

> Konsolidiert aus: claudewatch-session-start, claudewatch-session-protocol,
> claudewatch-tools

---

## 1. Session-Start-Protokoll (PFLICHT)

Bei JEDEM Session-Start (neu oder resumed), BEVOR auf den Benutzer reagiert wird:

1. **`get_project_health`** aufrufen (claudewatch MCP)
   - Ohne diesen Baseline-Check werden bekannte Fehler wiederholt
2. **Friction-Metriken lesen und verinnerlichen:**
   - Friction Rate >30% → High-Error-Umgebung, extra vorsichtig
   - Agent-Typ mit 0% Success Rate → NICHT spawnen
   - Top Friction = `buggy_code` oder `retry:Bash` → jeden Befehl vor Ausfuehrung verifizieren
3. **Bei "continue"/"resume":** `get_task_history(query: "<topic>")` aufrufen BEVOR implementiert wird.
   Bei Status "abandoned"/"in_progress": Blocker/Loesung lesen, fehlgeschlagene Ansaetze nicht wiederholen.

---

## 2. Waehrend der Session

### Hook-Warnung (⚠)

Wenn der PostToolUse-Hook mit `⚠` feuert: **SOFORT stoppen** und `get_session_dashboard`
aufrufen. Nicht weitermachen ohne die Situation zu bewerten (Error-Loops, Drift, Context-Pressure, Kosten).

### Context Pressure & Memory

- Bei "pressure" oder "critical": Compaction erwaegen.
  Bei "pressure": vorher `extract_current_session_memory` aufrufen
- Bei "burning" Cost Velocity: Quelle identifizieren vor Weitermachen
- Alle 30-45 Min in langen Sessions: "Habe ich etwas Wichtiges gelernt?"
  → Wenn ja: `extract_current_session_memory`
- Vor destruktiven Operationen (git reset --hard, rm -rf, grosse Refactors):
  IMMER `extract_current_session_memory` zuerst

### Error & Blocker Handling

- Bei Fehlern/Blockern: `get_blockers()` aufrufen BEVOR extensiv debuggt wird
- Gleicher Tool-Fehler 2-3x hintereinander → STOP → `get_blockers()` (bekanntes Problem)

### Task History Awareness

- Vor grossen Features: `get_task_history(query: "<feature>")` — war das schon mal versucht?
- Bei Benutzer-Frustration ("funktioniert nicht", "hatten wir schon"): Sofort
  `get_task_history` + `get_blockers` aufrufen
- Bei "wie haben wir...?": `search_transcripts(query: "X")` zuerst

### Progress Tracking

- Nach Major Work: `extract_current_session_memory` (Kontext frisch sichern)
- Vor Commits: `get_session_dashboard` → Commit-to-Attempt-Ratio pruefen
  (< 0.3 = zu viel Raten)
- Bei Stuck/Exploring: `get_drift_signal` → "drifting" → Scope reduzieren oder Benutzer fragen

---

## 3. Verfuegbare MCP-Tools (Referenz)

| Tool | Zweck |
|------|-------|
| `get_session_dashboard` | Alle Live-Metriken (Drift, Commit-Ratio, Kosten, Errors) |
| `get_project_health` | Session-Count, Friction-Rate, Agent-Success-Rate |
| `get_task_history` | Fruehere Task-Versuche nach Beschreibung abfragen |
| `get_blockers` | Bekannte Blocker mit Loesungen auflisten |
| `extract_current_session_memory` | Task-State sichern (vor riskanten Ops, nach Meilensteinen) |
| `search_transcripts` | Volltextsuche ueber alle Session-Transkripte |
| `get_drift_signal` | Erkennen ob Lesen ohne Implementieren stattfindet |
| `get_live_friction` | Echtzeit-Friction-Event-Stream |
| `get_context_pressure` | Kontextfenster-Auslastung |
| `get_cost_velocity` | Kosten-Burn-Rate (letzte 10 Min) |
| `get_suggestions` | Verbesserungsvorschlaege nach Impact sortiert |

Dokumentation: https://github.com/blackwell-systems/claudewatch#readme
