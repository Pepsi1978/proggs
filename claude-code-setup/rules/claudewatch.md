# ClaudeWatch: MCP-Integration & Session-Protokolle

## 1. Session-Start-Protokoll (PFLICHT)

Bei JEDEM Session-Start, BEVOR auf den Benutzer reagiert wird:
1. **`get_project_health`** (claudewatch MCP) — Baseline, sonst werden bekannte Fehler wiederholt.
2. **Friction lesen:** Rate >30 % → extra vorsichtig; Agent-Typ mit 0 % Success → NICHT spawnen;
   Top Friction = `buggy_code`/`retry:Bash` → jeden Befehl vor Ausfuehrung verifizieren.
3. **Bei "continue"/"resume":** `get_task_history(query)` BEVOR implementiert wird; bei "abandoned"/
   "in_progress" die Blocker/Loesung lesen, gescheiterte Ansaetze nicht wiederholen.

## 2. Waehrend der Session

- **Hook-Warnung ⚠:** SOFORT stoppen, `get_session_dashboard` (Error-Loops/Drift/Context/Kosten bewerten).
- **Context Pressure:** bei "pressure" `extract_current_session_memory`; bei "critical" Compaction erwaegen.
  Vor destruktiven Ops (reset --hard, rm -rf, grosse Refactors) IMMER `extract_current_session_memory`.
- **Errors/Blocker:** `get_blockers()` BEVOR extensiv debuggt wird. Gleicher Tool-Fehler 2-3× → STOP → `get_blockers()`.
- **Task History:** vor grossen Features `get_task_history(query)`; bei Frustration ("hatten wir schon")
  sofort `get_task_history` + `get_blockers`; bei "wie haben wir…?" `search_transcripts`.
- **Vor Commits:** `get_session_dashboard` → Commit-to-Attempt-Ratio (<0.3 = zu viel Raten).

## 3. Tools (Referenz)

`get_session_dashboard` (Live-Metriken) · `get_project_health` · `get_task_history` · `get_blockers` ·
`extract_current_session_memory` · `search_transcripts` · `get_drift_signal` · `get_live_friction` ·
`get_context_pressure` · `get_cost_velocity` · `get_suggestions`.
