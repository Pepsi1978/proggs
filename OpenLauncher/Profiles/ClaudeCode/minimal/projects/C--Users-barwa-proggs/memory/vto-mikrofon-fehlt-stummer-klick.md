---
name: vto-mikrofon-fehlt-stummer-klick
description: "TVO/CVO 03.09.2026 — USB-Mikrofon abgesteckt, waveInOpen BadDeviceId, Mikrofon-Klick verpuffte stumm; Fix mit Preflight, sichtbarer Rückmeldung, WM_DEVICECHANGE-Selbstheilung"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: e2665a62-20eb-4a59-bbc5-3fe24d1c9de2
  modified: 2026-09-03T12:22:14.687Z
---

**Fehler:** Klick auf den Mikrofon-Button im TerminalVoiceOverlay (1.11.1) tat scheinbar nichts. Im
`%LOCALAPPDATA%\TerminalVoiceOverlay\diag.log` stand sechs Mal `CaptureWorker start_failed |
NAudio.MmException: BadDeviceId calling waveInOpen` plus `Audio recording_start_not_ready`.

**Root Cause:** Windows kannte kein einziges Aufnahmegerät mehr (`Get-PnpDevice -Class AudioEndpoint`
zeigte nur den Lautsprecher, das USB-Mikrofon „Razer Seiren V3 Mini“ stand auf `Present = False`).
`waveInOpen` auf Index 0 scheitert dann mit `BadDeviceId`. Die tiefste Ursache im Code: kein
Preflight auf die Geräteanzahl UND der Fehlstart wurde stumm geschluckt (`if (!started) return;`).
Die zwei laufenden `TerminalVoiceOverlay.exe`-Prozesse sind gewollt (Watchdog + Overlay), kein Bug.

**Fix (TVO 1.11.2 / CVO 2.4.2, geteilte Dateien in beiden Projekten):**
- `AudioRecorder`: `InputDeviceCount` (= `waveInGetNumDevs`, öffnet nichts) als Preflight; Enum
  `RecordingStartFailure` + `LastStartFailureText` klassifiziert jede ERR-Zeile des Workers.
- `CaptureWorker`: eigener Geräte-Check, meldet `ERR NO_MIC` (zweite, unabhängige Schicht).
- `OverlayWindow.ReportRecordingStartFailure`: roter Button, Fehlerton (`RecordingCuePlayer.PlayError`),
  Tooltip mit Grund (ToolTip-Objekt-`Content` mutieren, nie neu zuweisen), ein Tray-Hinweis pro Episode.
- `WM_DEVICECHANGE`/`DBT_DEVNODES_CHANGED` entprellt → Gerät wieder da: Tooltip/Idle zurück;
  Gerät während Aufnahme weg: regulär stoppen, Gesprochenes verarbeiten. `RecordingLost`-Event, wenn
  der Worker mitten in der Aufnahme stirbt.
- Gleiche Klasse „stummer Riegel“: `_deploymentPending` (HTTP `/deployment/prepare`) blockierte das
  Mikrofon ohne Anzeige und ohne Verfall → jetzt sichtbar + Verfall nach 3 Minuten.

**Verwandte Prüfung:** Haupt-Mic, BTW, Alt+F12, PTT laufen alle über `BtnMic_Click`/`BtnBtw_Click`
→ ein Helfer deckt alle ab. `WatchdogTick` loggte Worker-Tod nur, jetzt meldet er ihn. Drive-Sync-
Timeouts (100 s) sind Hintergrund und bleiben als Vorschlag offen.

**Poka-Yoke-Stufe:** 2 (Erzwingung): Der Start-Pfad kann bei `false` nicht mehr stumm enden, weil der
Grund immer gesetzt ist und der einzige Rückgabepfad `ReportRecordingStartFailure` durchläuft.

**Muster-Erkennung:** Wenn eine Start-/Init-Methode nur `bool` liefert und der Aufrufer bei `false`
bloß `return`t, ist das ein stummer Fehlerpfad. `MmResult.BadDeviceId` bei Index 0 heißt praktisch
immer „kein Gerät“, nicht „falscher Index“. Erst `diag.log` lesen, dann `Get-PnpDevice` prüfen.

**Funktions-Diff:** Aufnahme/Stop/Transkription/BTW/PTT/Hotkey/Deployment-Riegel unverändert ✅;
neu: sichtbarer Fehlstart, Geräte-Selbstheilung, Riegel-Verfall. Keine Regression.

**How to apply:** Bei „Button tut nichts“ im Overlay zuerst `diag.log` nach `start_failed` greppen
und die Geräteliste prüfen, bevor am Klick-Handler gesucht wird. Siehe [[deploy-guard-port-unterscheidung]]
und [[openlauncher-deploy-update-skript]] für den Rebuild-Weg (`rebuild-overlay.ps1 Both`).
