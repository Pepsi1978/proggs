# claudewatch — Available MCP Tools

- `get_session_dashboard` — all live metrics in one call (drift, commit ratio, cost, errors)
- `get_project_health` — session count, friction rate, CLAUDE.md status, agent success rate
- `get_task_history` — query previous task attempts by description
- `get_blockers` — list known blockers with solutions
- `extract_current_session_memory` — checkpoint task state (before risky ops, after milestones)
- `search_transcripts` — full-text search across all session transcripts
- `get_drift_signal` — detect when you're stuck reading without implementing
- `get_live_friction` — real-time friction event stream
- `get_context_pressure` — context window utilization
- `get_cost_velocity` — cost burn rate (last 10 min)
- `get_suggestions` — improvement suggestions ranked by impact

Full documentation: https://github.com/blackwell-systems/claudewatch#readme
