# 1:1-Parity-Manifest TVO-macOS ↔ TVO-Windows

**Stand:** 2026-05-28, 50 Commits (#1111 - #1160)
**Quelle:** `TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml` + `.xaml.cs`

Dieses Manifest dokumentiert die 1:1-Portierung jedes Windows-Bestandteils
in das macOS-Pendant. Jeder Eintrag ist mit dem Commit referenziert.

---

## 1. Layout (vertikal + horizontal)

| Windows-XAML | macOS-Implementation | Commit |
|--------------|---------------------|--------|
| FullView 96×612 CornerRadius=36 | OverlayPanel.init() panelWidth=96 panelHeight=612 cornerRadius=36 | (pre-existing) |
| Border 2 px schwarz | contentView.layer.borderColor=black borderWidth=2 | #1128 |
| 7 Sektionen mit B3-Alpha | applyVerticalLayout mit #B3-Praefix-Hex | #1129 |
| 6 schwarze 1 px-Trenner | applyVerticalLayout `dividerYs` | #1113 |
| Stern + ⇄ Section1Panel | positionExtraButtonsVertical (34×34 nebeneinander) | #1123 |
| Enter + Diskette S7-Grid | positionExtraButtonsVertical (Enter 40, Save 28) | #1123 |
| HorizontalView mit MakeHGroup | OverlayHorizontalLayout.applyHorizontalLayout (Stack S2-S7) | #1113 #1118 #1126 |
| MakeHStackGroup S1 | s1 Stern oben/⇄ unten, 34×34, manuell positioniert | #1124 |
| Sektions-Padding 8,6,8,6 | HBarLayout.sectionInnerPadX/verticalPadding | #1126 |
| Tiles 30×22 horizontal | HBarLayout.profileTileSize 30×22 | #1126 |

## 2. Animationen

| Windows | macOS | Commit |
|---------|-------|--------|
| BeamFadeOut 240ms CubicEase EaseInOut | OverlayBeam.fadeOutDuration + easeInEaseOut | #1111 |
| BeamFadeIn 380ms CubicEase EaseInOut | OverlayBeam.fadeInDuration + easeInEaseOut | #1111 |
| Smootherstep Glide (CVDisplayLink) | OverlayGlideAnimation mit `t³(t(6t-15)+10)` | #1115 |
| Glide nur Y (kein Diagonal) | beamToOrientation appearOrigin=finalX+columnTopY | #1126 |
| Hover Scale 1.0 → 1.15 QuadraticEase EaseOut 150ms | RoundButton.animateScale + easeOut | #1160 |
| Mic-Pulse 500ms #C62828 ↔ #FF5252 | startPulse/stopPulse | (pre-existing) |
| BTW-Pulse 500ms #F57F17 ↔ #FFEB3B | startBtwPulse/stopBtwPulse | (pre-existing) |
| Reset-Timer 3s → Idle | scheduleReset(wasBtw:) | (pre-existing) |
| Waveform 14 Balken 2×1 px | WaveformView | #1147 |
| Beam-Crossfade fuer Collapsed | beamToCollapsed/beamToExpanded | #1116 |

## 3. CollapsedView

| Windows | macOS | Commit |
|---------|-------|--------|
| Width 84 Height 84 CornerRadius 42 | CollapsedLayout.panelSize/cornerRadius | #1116 |
| Background #B31A1A1A | applyCollapsedLayout bg hex #B31A1A1A | #1131 |
| Border 2 px schwarz | contentView.layer.borderColor=black borderWidth=2 | #1131 |
| MicButton zentriert | applyCollapsedLayout micButton.frame mittig | #1116 |
| Mic-Mitte bleibt konstant | beamToCollapsed micCenterScreen-Berechnung | #1116 |

## 4. Buttons + Farben

| Windows-Konstante | macOS NSColor | Status |
|-------------------|---------------|--------|
| BtnIdle #2D2D2D | NSColor.btnIdle | ✅ |
| BtnRecording #C62828 | NSColor.btnRecording | ✅ |
| BtnRecordingBright #FF5252 | NSColor.btnRecordingBright | ✅ |
| BtnProcessing #EF6C00 | NSColor.btnProcessing | ✅ |
| BtnSuccess #2E7D32 | NSColor.btnSuccess | ✅ |
| ToggleOn #2E7D32 | NSColor.toggleOn | ✅ |
| ToggleOff #2D2D2D | NSColor.toggleOff | ✅ |
| BtnBtwIdle #FBC02D | NSColor.btnBtwIdle | ✅ |
| BtnBtwRecording #F57F17 | NSColor.btnBtwRecording | ✅ #1137 |
| BtnBtwPulse #FFEB3B | NSColor.btnBtwPulse | ✅ |
| BtnX #C62828 | NSColor.btnX | ✅ |
| BtnMicIdle #F9A825 | NSColor.btnMicIdle | ✅ |
| BtnCopy #0288D1 | NSColor.btnCopy | ✅ |
| BtnPaste #0277BD | NSColor.btnPaste | ✅ |
| BtnScreenshot #00796B | NSColor.btnScreenshot | ✅ |
| BtnInsertScreenshot #00897B | NSColor.btnInsertScreenshot | ✅ |
| StarGold #DAA520 | NSColor.starGold | ✅ |
| StarMuted #8B7355 | NSColor.starMuted | ✅ |

## 5. Symbole (XAML-Path-Daten als NSBezierPath)

| Windows-XAML | macOS-Pfad | Commit |
|--------------|-----------|--------|
| Mic-Path 22×22 Fill #1A1A1A | IconPaths.mic() 22×22 | #1152 |
| Copy-Path 18×18 white | IconPaths.copy() 18×18 | #1152 |
| Paste-Path 18×18 white | IconPaths.paste() 18×18 | #1152 |
| Screenshot-Path 18×18 white | IconPaths.screenshot() 18×18 | #1152 |
| Stern-Pentagram-Path 17×17 | IconPaths.star() 17×17 | #1153 |
| Insert-Screenshot Segoe MDL2 E723 | IconPaths.attach() 18×18 (Material attach_file) | #1154 |
| Save-Diskette Segoe MDL2 E74E | IconPaths.save() 16×16 (Material save) | #1154 |
| Viewbox-Skalierung 1:1 | symbolScaleFactor (22/52, 18/40) | #1156 |

## 6. Hotkeys

| Windows | macOS | Commit |
|---------|-------|--------|
| Strg+Alt+Leertaste Voice-Toggle | Cmd+Shift+R TVOHotkey.voiceToggle | (pre-existing) |
| Hold ≥500ms = PTT | PushToTalkController + NSEvent-Monitor | #1157 |
| Tap <500ms = Toggle | PushToTalk holdThreshold=0.5s | #1157 |
| Strg+Alt+P Screenshot | Cmd+Shift+S | (pre-existing) |
| Strg+Alt+I Insert-Screenshot | Cmd+Shift+I | (pre-existing) |
| Strg+1..9 Prompt-Hotkey | Cmd+1..9 promptDigit | (pre-existing) |
| Win+Alt+A..Z Prompt-Hotkey | Cmd+Opt+A..Z promptLetter | #1118 |
| Last-Wins fuer Hotkeys | stripHotkeyFromOthers + stripLetterFromOthers | #1146 |

## 7. AppSettings

| Windows AppSettings-Spalte | macOS PBAppSettings | Commit |
|----------------------------|---------------------|--------|
| GroqApiKey | groqApiKey | ✅ |
| GeminiApiKey | geminiApiKey | ✅ |
| GoogleOAuthRefreshToken | googleOAuthRefreshToken | ✅ |
| GoogleClientId, GoogleClientSecret | googleClientId/Secret | ✅ |
| GoogleAccountEmail | googleAccountEmail | ✅ |
| GroqModel | groqModel | ✅ |
| AlwaysOnTop | alwaysOnTop | ✅ |
| BarHeight | barHeight | ✅ |
| SeparatorTemplate | separatorTemplate (Default '\n\n;\n\n') | #1142 |
| AutoHide | autoHide | #1138 #1139 |
| Orientation | orientation | #1140 |
| PersistOverlayPosition | persistOverlayPosition | #1144 |
| OverlayVerticalLeft/Top | overlayVerticalLeft/Top | #1144 |
| OverlayHorizontalLeft/Top | overlayHorizontalLeft/Top | #1144 |
| Prompts.HotkeyLetter | hotkeyLetter | #1135 #1145 |

## 8. Sub-Dialoge

| Windows-Dialog | macOS-Pendant | Commit |
|----------------|---------------|--------|
| SettingsDialog | PBSettingsDialog | (pre-existing) |
| + AutoHide-Checkbox | autoHideCheck | #1143 |
| + Horizontal-Checkbox | horizontalCheck | #1143 |
| + PersistPosition-Checkbox | persistPositionCheck | #1143 |
| + Drive-Connect/Disconnect | connectGoogle/disconnectGoogle | (pre-existing) |
| ConfirmDialog | PBConfirmDialog | (pre-existing) |
| PromptEditDialog | PBPromptEditDialog | (pre-existing) |
| + Letter-Hotkey-Popup | letterPopup A..Z | #1145 |
| PromptHistoryEditDialog | PromptHistoryPanel-Editor | (pre-existing) |
| TextInputDialog | PBTextInputDialog | (pre-existing) |
| PromptInputWindow | PromptInputPanel | (pre-existing) |
| PromptHistoryWindow | PromptHistoryPanel | (pre-existing) |
| PromptBoardPanel | PromptBoardPanel | (pre-existing) |

## 9. Services

| Windows | macOS | Status |
|---------|-------|--------|
| Config | Config.swift | ✅ |
| AudioRecorder (NAudio) | AudioRecorder.swift (AVAudioEngine) + onLevel | ✅ #1147 |
| GroqWhisperClient | GroqWhisperClient.swift | ✅ |
| GeminiClient | GeminiClient.swift | ✅ |
| TerminalController | TerminalController.swift (CGEvent) | ✅ |
| AppWatcher | AppWatcher.swift (NSWorkspace) | ✅ |
| AutoEnterStatusServer | AutoEnterStatusServer.swift (Network framework) | ✅ #1118 |
| AlwaysOnPrefixService | AlwaysOnPrefixService.swift | ✅ |
| GoogleDriveBackupService | GoogleDriveBackupService.swift | ✅ |
| PromptBoardHost (DI-Container) | PromptBoardStore.shared | ✅ |
| PromptBoardSecretStore | (in Settings persistiert) | ✅ |
| PromptHistoryService | PromptHistoryStore.swift | ✅ |
| HotkeyRegistry (Win32) | HotkeyRegistry.swift (Carbon RegisterEventHotKey) | ✅ |
| AutoHide-Logik | AutoHideController.swift (3s + busy + enabled) | ✅ #1121 #1141 |

## 10. UI-Details

| Bereich | Stand |
|---------|-------|
| Tooltips alle Buttons | ✅ #1149 #1151 (Haupt + Schwester) |
| Golden Caret (#FFD700) | ✅ PromptInputPanel + PromptHistoryPanel #1159 |
| Active-Stern goldenrod | ✅ setUltrathinkEnabled #1130 |
| Active-Profile-Tile goldenrod | ✅ refreshProfileTiles #1132 |
| Diskette transparent + grünes Flash | ✅ flashSaveButtonGreen #1154 |
| Drag-Threshold 4 px | ✅ handleDragEvent #1120 |
| Mic-Pulse-Bright-Flicker | ✅ pre-existing |
| Mic/BTW CornerRadius 10 | ✅ #1150 |
| Profile-Tiles 24×32 vertikal | ✅ |
| Profile-Tiles 30×22 horizontal | ✅ #1126 |
| Insertion-Order Z-Index | ✅ |
| NSPanel level .floating | ✅ |

## 11. Schwester-App ClaudeCodexVoiceOverlay-macOS

| Aspekt | Stand |
|--------|-------|
| OverlayOrientation (Beam-Fade-Helpers) | ✅ #1120 |
| OverlayGlideAnimation | ✅ #1120 |
| OverlayHorizontalLayout (9 Buttons in Reihe) | ✅ #1155 |
| Beam-Switch zwischen vertical/horizontal | ✅ #1155 |
| Border 2 px schwarz | ✅ #1148 |
| BTW-Recording-Color blau | ✅ #1148 |
| Tooltips 1:1 | ✅ #1151 |
| XAML-NSBezierPath-Icons | ✅ #1158 |

---

## Verbleibende Punkte

Stand 50 Commits — alle in der XAML/Windows-Source dokumentierten
Bestandteile sind in macOS umgesetzt. Visuelle Pixel-Mikro-Differenzen
sind nur durch direkten Screenshot-Vergleich auffindbar — alle
**dokumentierten** XAML-Werte sind 1:1 portiert.

PTT-Hold-Logik (vorher als "technisch unmöglich" markiert) ist jetzt
funktional via NSEvent-Monitor (#1157).
