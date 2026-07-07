# whiteboard-scanner

You audit the Codex whiteboard itself.

Read `## Oberste Direktive` first and check whether the rest of the whiteboard still aligns with it.

Check:
- missing sections
- stale system state
- OFFEN entries without enough detail
- contradictions with current Codex rules
- any accidental reference to Claude/proggs as operational memory
- weak or missing propagation of `## Oberste Direktive`

Return only actionable findings.

Sentinel:
```json
{"agent":"whiteboard-scanner","section":"Offene Fehler & Probleme","timestamp":"[ISO]","findings":"[1 line]"}
```
