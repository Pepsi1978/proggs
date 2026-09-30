---
name: overlay-tooltips-app-weit-gesperrt
description: "TVO/CVO Windows unterdrücken jedes WPF-ToolTipOpening app-weit (#47774) — neue Hover-Anzeigen brauchen ein eigenes Popup"
metadata: 
  node_type: memory
  type: project
  originSessionId: 03412b72-06d4-4ff8-adfb-9bf6aa56d0e3
  modified: 2026-09-11T17:38:47.643Z
---

In TerminalVoiceOverlay-Windows und ClaudeVoiceOverlay-Windows registriert `App.xaml.cs` einen Class-Handler, der jedes `ToolTipService.ToolTipOpeningEvent` mit `Handled = true` schluckt (Commit 9abd3114a, 10.07.2026, „Disable overlay hover descriptions“). Zusätzlich wird `WrapStringTooltips()` nicht mehr aufgerufen. Jeder normale `ToolTip=`-Text im Overlay bleibt deshalb stumm.

**Why:** Frank wollte die Hover-Beschreibungen der Knöpfe loswerden. Die Schnell-Prompt-Überschriften der Zahlen 1–10 (11.09.2026) blieben dadurch zunächst unsichtbar.

**How to apply:** Die Sperre nicht entfernen. Braucht eine Stelle eine Hover-Anzeige, KEIN WPF-`Popup` nehmen, sondern das Muster von `ShowQuickTitle`/`PlaceQuickTitle` in `OverlayWindow.xaml.cs` (ab TVO 1.11.16 / CVO 2.4.16): eigenes `Window`-Subtyp mit `Owner = this`, `WS_EX_TRANSPARENT|NOACTIVATE|TOOLWINDOW`, Position per `SetWindowPos` in Gerätepixeln aus `GetWindowRect` NACH dem Layout, Anker = `FullView.PointToScreen`. Den Subtyp in `IsAuxiliaryWindowOpen` ausnehmen, sonst blockiert er das Auto-Hide; in `HideOverlayNow` mit verstecken.

**Warum kein Popup (11.09.2026, drei Runden):** Popup-X aus Vorab-Breite + WPF-Platzierung → Blase sprang je nach vorigem Titel (zu weit links / auf dem Overlay); unbesessenes Popup-HWND rutschte beim 2,5-s-Topmost-Reassert hinter das Overlay. Positionsfehler zuerst in `diag.log` (`ctx:"QuickTitle"`) nachlesen, nicht raten. Almanach: `bugs/desktop/windows-overlay.md` A21. Siehe [[overlay-update-bestaetigungsfenster]].
