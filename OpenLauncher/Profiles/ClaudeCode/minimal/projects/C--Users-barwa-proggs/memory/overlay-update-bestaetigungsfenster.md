---
name: overlay-update-bestaetigungsfenster
description: "rebuild-overlay.ps1 (TVO/CVO) zeigt ab 1.5.0 ein Ja/Nein-Fenster; mit 600000 ms Timeout starten, bei cancelled/no-answer nicht wiederholen, nie -Force"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: 564533f3-8b57-47b4-b297-4a6b91ebe680
  modified: 2026-09-11T12:19:44.452Z
---

`~/proggs/rebuild-overlay.ps1` (Update von TerminalVoiceOverlay/ClaudeVoiceOverlay) fragt seit 11.09.2026 per Ja/Nein-Fenster, bevor es irgendetwas beendet oder baut — analog zu [[openlauncher-deploy-update-skript]].

**Why:** Frank will verhindern, dass ein Update eine laufende Spracheingabe abschießt.

**How to apply:** Befehl immer mit dem höchsten Timeout (600000 ms) starten, nicht abbrechen, solange das Fenster wartet. `OVERLAY_UPDATE_STATUS=cancelled` oder `=no-answer` → nicht erneut starten, sondern Frank melden. `-Force` nur auf ausdrückliche Ansage.
