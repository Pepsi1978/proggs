# ClaudeWatch: MCP-Integration & Session-Protokolle

## 1. Session-Start (PFLICHT)
1. **`get_project_health`** - Baseline, sonst bekannte Fehler wiederholt.
2. **Friction:** Rate >30 % -> vorsichtig; Agent-Typ 0 % Success -> NICHT spawnen; Top Friction
   `buggy_code`/`retry:Bash` -> jeden Befehl vor Ausfuehrung verifizieren.
3. **"continue"/"resume":** `get_task_history(query)` VOR Implementierung; bei "abandoned"/"in_progress"
   Blocker/Loesung lesen, gescheiterte Ansaetze nicht wiederholen.

## 2. Waehrend der Session
- **Hook-Warnung:** SOFORT stoppen, `get_session_dashboard` (Error-Loops/Drift/Context/Kosten).
- **Context Pressure:** "pressure" -> `extract_current_session_memory`; "critical" -> Compaction erwaegen.
  Vor destruktiven Ops (reset --hard, rm -rf, grosse Refactors) IMMER `extract_current_session_memory`.
- **Errors/Blocker:** `get_blockers()` VOR extensivem Debuggen. Gleicher Tool-Fehler 2-3x -> STOP -> `get_blockers()`.
- **Task History:** vor grossen Features `get_task_history(query)`; bei Frustration ("hatten wir schon")
  sofort + `get_blockers`; bei "wie haben wir?" `search_transcripts`.
- **Vor Commits:** `get_session_dashboard` -> Commit-to-Attempt-Ratio (<0.3 = zu viel Raten).

## 3. Weitere Tools
`get_drift_signal` - `get_live_friction` - `get_context_pressure` - `get_cost_velocity` - `get_suggestions`.
