---
name: vto-mikrofon-fehlt-stummer-klick
description: "TVO/CVO: Mikrofon-Klick verpufft stumm (waveInOpen BadDeviceId, USB-Mikrofon abgesteckt); Fix 03.09.2026 mit Preflight, sichtbarer Rückmeldung, WM_DEVICECHANGE-Selbstheilung"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: e2665a62-20eb-4a59-bbc5-3fe24d1c9de2
  modified: 2026-09-03T12:22:14.687Z
---

**Fehler:** Mikrofon-Button im TerminalVoiceOverlay (1.11.1) tat scheinbar nichts. `%LOCALAPPDATA%\TerminalVoiceOverlay\diag.log`: sechs Mal `CaptureWorker start_failed | NAudio.MmException: BadDeviceId calling waveInOpen` plus `Audio recording_start_not_ready`.

**Ursache:** Windows kannte kein Aufnahmegerät mehr (`Get-PnpDevice -Class AudioEndpoint` nur Lautsprecher, USB-Mikrofon „Razer Seiren V3 Mini“ auf `Present = False`) → `waveInOpen` auf Index 0 scheitert mit `BadDeviceId`. Im Code: kein Preflight auf die Geräteanzahl UND Fehlstart stumm geschluckt (`if (!started) return;`). Zwei laufende `TerminalVoiceOverlay.exe`-Prozesse sind gewollt (Watchdog + Overlay), kein Bug.

**Fix (TVO 1.11.2 / CVO 2.4.2, geteilte Dateien in beiden Projekten):**
- `AudioRecorder`: Preflight `InputDeviceCount` (= `waveInGetNumDevs`, öffnet nichts); Enum `RecordingStartFailure` + `LastStartFailureText` klassifiziert jede ERR-Zeile des Workers.
- `CaptureWorker`: eigener Geräte-Check, meldet `ERR NO_MIC` (zweite, unabhängige Schicht).
- `OverlayWindow.ReportRecordingStartFailure`: roter Button, Fehlerton (`RecordingCuePlayer.PlayError`), Tooltip mit Grund (ToolTip-Objekt-`Content` mutieren, nie neu zuweisen), ein Tray-Hinweis pro Episode.
- `WM_DEVICECHANGE`/`DBT_DEVNODES_CHANGED` entprellt: Gerät wieder da → Tooltip/Idle zurück; Gerät während Aufnahme weg → regulär stoppen, Gesprochenes verarbeiten. `RecordingLost`-Event, wenn der Worker mitten in der Aufnahme stirbt.
- Gleiche Klasse „stummer Riegel“: `_deploymentPending` (HTTP `/deployment/prepare`) blockierte das Mikrofon ohne Anzeige und Verfall → jetzt sichtbar + Verfall nach 3 Minuten.
- Haupt-Mic, BTW, Alt+F12, PTT laufen alle über `BtnMic_Click`/`BtnBtw_Click` → ein Helfer deckt alle ab. `WatchdogTick` loggte Worker-Tod nur, jetzt meldet er ihn.
- Unverändert: Aufnahme/Stop/Transkription/BTW/PTT/Hotkey/Deployment-Riegel, keine Regression. Offen als Vorschlag: Drive-Sync-Timeouts (100 s, Hintergrund).
- Poka-Yoke-Stufe 2 (Erzwingung): Start-Pfad kann bei `false` nicht mehr stumm enden, weil der Grund immer gesetzt ist und der einzige Rückgabepfad `ReportRecordingStartFailure` durchläuft.

**Why:** Liefert eine Start-/Init-Methode nur `bool` und der Aufrufer `return`t bei `false` bloß, ist das ein stummer Fehlerpfad. `MmResult.BadDeviceId` bei Index 0 heißt praktisch immer „kein Gerät“, nicht „falscher Index“.

**How to apply:** Bei „Button tut nichts“ im Overlay zuerst `diag.log` nach `start_failed` greppen, dann Geräteliste per `Get-PnpDevice` prüfen, erst danach am Klick-Handler suchen. Rebuild-Weg (`rebuild-overlay.ps1 Both`): [[deploy-guard-port-unterscheidung]], [[openlauncher-deploy-update-skript]].
