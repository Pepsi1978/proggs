# PORTING-INVENTORY — TerminalVoiceOverlay-Windows → macOS

**Erstellt:** 2026-05-27 | Quelle: `/Users/frank/proggs/TerminalVoiceOverlay-Windows/`

Diese Datei wurde von einem Explore-Agent erstellt und dient als
Bauplan fuer die vollstaendige 1:1-Portierung der Windows-Version
nach macOS. Pro abgeschlossener Etappe in der Implementation den
zugehoerigen Abschnitt hier mit "✅" markieren.

---

## 1. Hauptfenster (OverlayWindow.xaml + .cs)

### Buttons (Gesamtanzahl: 24 + 1 Collapsed-Mic)

| Name | Stil | Größe | Funktion |
|---|---|---|---|
| UltrathinkButton (Stern) | CircleButton | 34×34 | PromptBoard öffnen |
| OrientationToggleButton (⇄) | CircleButton | 34×34 | Vertikal ↔ Horizontal umschalten |
| MicButton | MicButton | 52×52 | Hauptaufnahme starten/stoppen |
| BtwButton (BTW) | MicButton | 52×52 | Zwischenfrage-Aufnahme |
| Profile1Button–Profile10Button | ProfileButton | 24×32 | Gemini-Profil wählen (1–10), Linksklick=Re-Correct, Rechtsklick=nur Profil wechseln |
| WButton | CircleButton | 40×40 | Whisper-Modus (Raw-Transkription) |
| GButton | CircleButton | 40×40 | Gemini-Modus (KI-Korrektur) |
| XButton | CircleButton | 40×40 | Zeile löschen (Halten = alle Zeilen) |
| CopyButton | CircleButton | 40×40 | Text kopieren (Ctrl+C) |
| PasteButton | CircleButton | 40×40 | Text einfügen (Ctrl+V) |
| ScreenshotButton | CircleButton | 40×40 | Screenshot aufnehmen |
| InsertScreenshotButton | CircleButton | 40×40 | Letzten Screenshot einfügen |
| EnterButton (↵) | CircleButton | 40×40 | Auto-Enter an/aus |
| SaveButton (Diskette) | SaveButton | 28×28 | Position merken (session-lokal) |
| CollapsedMicButton | MicButton | 52×52 | Aufnahme im Collapsed-Zustand |

### Farb-Konstanten (C# `OverlayWindow.xaml.cs`)

| Konstante | Hex | Verwendung |
|---|---|---|
| BtnIdle | #2D2D2D | Standardhintergrund der meisten Buttons |
| BtnRecording | #C62828 | Mic-Button während Aufnahme |
| BtnRecordingBright | #FF5252 | Mic-Puls-Farbe |
| BtnProcessing | #EF6C00 | Mic-Button während Verarbeitung; Enter-Button wenn autoEnter=true |
| BtnSuccess | #2E7D32 | Mic/Enter nach Erfolg |
| ToggleOn | #2E7D32 | W/G-Button aktiv |
| ToggleOff | #2D2D2D | W/G-Button inaktiv |
| BtnBtwIdle | #FBC02D | BTW-Button ruhend (Yellow 700) |
| BtnBtwRecording | #F57F17 | BTW-Button aufnehmend (Yellow 900) |
| BtnBtwPulse | #FFEB3B | BTW-Puls (Yellow 500) |
| BtnX | #C62828 | X-Button |
| BtnMicIdle | #F9A825 | Mic ruhend (Yellow 800) |
| BtnCopy | #0288D1 | Copy-Button (Light Blue 700) |
| BtnPaste | #0277BD | Paste-Button (Light Blue 800) |
| BtnScreenshot | #00796B | Screenshot-Button (Teal 700) |
| BtnInsertScreenshot | #00897B | Insert-Screenshot-Button (Teal 600) |
| StarGold | #DAA520 | Stern aktiv |
| StarMuted | #8B7355 | Stern inaktiv |

### Sektionen mit Hintergrundfarben (S1–S7)

| Sektion | Inhalt | Hintergrund | CornerRadius |
|---|---|---|---|
| S1 | Stern + ⇄ | #B31F1B15 | 34,34,0,0 (oben rund) |
| S2 | Mic + BTW + Profile 1–3 | #B31F1C15 | — |
| S3 | W + G + Profile 4–5 | #B319151F | — |
| S4 | X + Profile 6 | #B31F1515 | — |
| S5 | Copy + Paste + Profile 7–8 | #B3151B1D | — |
| S6 | Screenshot + Insert + Profile 9–10 | #B3151B15 | — |
| S7 | Enter + Diskette | #B31A1A1A | 0,0,34,34 (unten rund) |
| Trennlinien | je 1px schwarz #FF000000 | zwischen Sektionen | — |

### Layouts

- **FullView** (vertikal): 96×612px, CornerRadius=36, StackPanel vertikal
- **HorizontalView**: Gleiche Buttons werden zur Laufzeit in eine horizontale StackPanel (`HBar`) umgehängt — **kein Duplikat der Button-Instanzen**
- **CollapsedView** (Mic-Pille): 84×84px, CornerRadius=42, nur CollapsedMicButton sichtbar
- Fenster-Offset beim Ein-/Ausklappen: FullHeight=612, CollapsedHeight=96, CollapseTopOffset=50 (damit Mic-Position konstant bleibt)

### Animationen

| Animation | Trigger | Details |
|---|---|---|
| **HoverScale** | MouseEnter/Leave jedes Buttons | ScaleTransform 1.0→1.15 / 1.15→1.0, 150ms, QuadraticEase EaseOut |
| **MicPuls** (DispatcherTimer) | Während Aufnahme, 500ms-Takt | #C62828 ↔ #FF5252 |
| **BtwPuls** (DispatcherTimer) | Während BTW-Aufnahme, 500ms-Takt | #F57F17 ↔ #FFEB3B |
| **Reset** (DispatcherTimer) | 3s nach Erfolg/Fehler | → RecordingState.Idle |
| **BeamFade / Crossfade** | Collapse/Expand (AutoHide) | `_collapseBeamGen` Guards, FullView ↔ CollapsedView, teilweise via DoubleAnimation auf Opacity |
| **Waveform** | Während Aufnahme, ~100ms (NAudio-Buffer) | 14 Rechteck-Striche, 2px breit, 1px Spacing, Höhe 3–40px, speist sich aus `AudioRecorder.LevelChanged` |
| **SeparatorGlow** (im PromptInputWindow) | Klick auf `;`-Button | DropShadowEffect #FFFFE680 blendet in ~400ms aus |

### Drag-Verhalten

- **Rechtsklick + Ziehen** auf das Panel: Overlay verschieben (Win32 `GetCursorPos` → `SetWindowPos`)
- Threshold: 4px (`DragThresholdPx`), danach wird ein Profil-Tile-Klick abgebrochen und stattdessen Drag ausgelöst
- **Linksklick** auf CollapsedMicButton: Aufnahme starten
- Disketten-Button merkt Position pro Orientierung (`_savedHorizontalPos`, `_savedVerticalPos`) — nur Session-persistent, außer `PersistOverlayPosition=true` in AppSettings

---

## 2. Sub-Dialoge

### SettingsDialog (620×640px)
Einstellungen für die laufende Instanz.
**Felder:** GroqKeyBox (API-Key), GeminiKeyBox (API-Key optional), SeparatorBox (Trenn-Template zwischen AlwaysOn-Prompts), AutoHideCheck (Auto-Collapse), HorizontalCheck (Orientierung), PersistPositionCheck (Disketten-Position über Neustart), GoogleClientIdBox, GoogleClientSecretBox.
**Buttons:** BtnGoogleConnect (grün #16A34A), BtnGoogleDisconnect, BtnCancel, BtnOk ("Speichern", gold #B8860B).
**Statusanzeige:** GoogleStatus (rot/grün-Text).

### ConfirmDialog (420px, Höhe auto)
Generischer Bestätigungs-Dialog (Löschen).
**Felder:** TitleText, MessageText.
**Buttons:** BtnCancel ("Abbrechen"), BtnOk ("Loeschen", rot #E53935).

### PromptEditDialog (560×780px)
Prompt anlegen oder bearbeiten, mit optionaler Mic-Aufnahme + Gemini-Verbesserung.
**Felder:** ShortLabelBox (Kurzbezeichnung), OriginalTextBox (mehrzeilig), AlwaysOnCheckbox (immer mitschicken), PrePromptCheckbox (vor Diktat), PostPromptCheckbox (nach Diktat), HotkeyPanel (WrapPanel mit Strg+1..9-Toggle-Buttons).
**Buttons:** BtnCancel, BtnMic (🎤, rund 40px), BtnGemini (G, rund 40px), BtnOk ("Speichern").
**Nach Gemini-Verbesserung:** DualSavePanel eingeblendet mit BtnSaveImproved + BtnSaveOriginal (BtnOk wird ausgeblendet).
**StatusText:** Aufnahme-/Gemini-Fortschritt.

### PromptHistoryEditDialog (720×520px, CanResize)
Einzelnen Historie-Eintrag nachbearbeiten.
**Felder:** MetaLabel (Metadaten), EditBox (mehrzeiliger Text, golden CaretBrush #FFD700).
**Buttons:** BtnCancel, BtnSave (gold).

### TextInputDialog (440px, Höhe auto)
Generisches Einzeilen-Eingabefenster (z.B. Kategoriename).
**Felder:** TitleText, LabelText ("Name:"), InputBox.
**Buttons:** BtnCancel, BtnOk.

### PromptInputWindow (1140×490px)
Großes Freitext-Eingabefenster, andockt links neben das PromptBoard-Panel.
**Hintergrund:** halbtransparent 78% (#C71E1E1E).
**Felder:** InputBox (mehrzeilig, FontSize=26, golden Caret), PreviewLabel (Pre/Post-Vorschau).
**Toolbar-Buttons:** SoloDockStarButton (Stern ⭐ gold, Promptboard ausblenden + direkt ans Overlay andocken), SeparatorButton (`;`, Aufgabentrenner), GeminiButton (G), ClearInputButton (X rot).
**Hint:** "Enter sendet · Shift+Enter neue Zeile · Rechtsklick zum Verschieben".

### PromptHistoryWindow (760×490px)
Scrollbare Liste aller gespeicherten Prompt-Einträge.
**Hintergrund:** halbtransparent 78% (#C71E1E1E).
**Elemente:** HistoryList (StackPanel mit Einträgen), CountLabel, StatusLabel.
**Interaktion:** Linksklick auf Eintrag → in InputBox einfügen; Rechtsklick → Fenster verschieben.

### PromptBoardPanel (532×490px)
Hauptfenster für Prompt-Kategorien und -Listen. Andockt links neben das Voice-Overlay.
**Styles:** CategoryTab, CategoryTabActive (#B8860B), IconButton (26×26, Segoe Fluent Icons), RowIconButton (22×22), PromptRow, PromptButton.
**Toolbar:** Buttons für Kategorie hinzufügen, Backup, Einstellungen, Stern (AlwaysOn-Toggle).
**Zeilen:** Jede Prompt-Zeile hat RowIconButtons für Bearbeiten, Löschen, Hotkey zuweisen (Kontextmenü mit A..Z-Untermenü).

---

## 3. Services

### AlwaysOnPrefixService
Baut den AlwaysOn-Prefix-String aus der Datenbank zusammen (Pre- und Post-Prompts, durch `SeparatorTemplate` getrennt). Verwendet `IPromptChainBuilder` und `IPromptRepository`. Externe Lib: `Microsoft.Extensions.DependencyInjection`, `Microsoft.Extensions.Logging`.

### AudioRecorder
Nimmt Mikrofon-Audio über NAudio (`WaveInEvent`) auf, schreibt WAV in eine Temp-Datei, feuert `LevelChanged`-Events (~100ms) mit Peak-Pegel 0..1. Externe Lib: **NAudio**.

### AutoEnterStatusServer
Minimalistischer HTTP-Server auf `http://127.0.0.1:5723/`. Routen: `GET /autoenter/status` → JSON `{"on":true|false}`, `POST /autoenter/toggle`. Für Stream-Deck-XL-Integration via Polling. Keine externe Lib.

### Config
Liest `.env`-Datei aus mehreren Suchpfaden (Priorität: `~/SK/VoiceOverlays/.env`, neben der `.exe`, `~/.env`, `%APPDATA%`). Felder: `GROQ_API_KEY`, `WHISPER_MODEL`, `WHISPER_LANG`, `WHISPER_URL`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_THINKING_LEVEL`, `AUDIO_SAMPLE_RATE`, `AUDIO_CHANNELS`, `TERMINAL_PROCESS_NAMES`.

### GeminiClient
Sendet Text an die Gemini REST API (`generativelanguage.googleapis.com`), inklusive Retry-Logik (3 Retries bei 429/500/503, exponentielles Backoff 2/4/8/16/32s). 3 Prompt-Templates: Diktat-Cleanup (Profil 1–10), Prompt-Engineer-Verbesserung, Titel-Generierung. Shared `HttpClient`. Externe API: **Google Gemini**.

### GoogleDriveBackupService
Speichert/lädt `promptboard-backup.json` im Google Drive `appDataFolder`. OAuth2 via `PromptBoardSecretStore`. Externe Libs: **Google.Apis.Drive.v3**, **Google.Apis.Auth**.

### GroqWhisperClient
Sendet WAV-Bytes an die Groq Whisper API (`api.groq.com/openai/v1/audio/transcriptions`), max. 3 Retries. Shared `HttpClient`. Externe API: **Groq Whisper**.

### HotkeyRegistry
Thread-sicheres In-Memory-Register für zwei Hotkey-Typen: `ConcurrentDictionary<int, Entry>` für Strg+1..9, `ConcurrentDictionary<char, Entry>` für Win+Alt+A..Z. Wird von `PromptBoardPanel` nach jedem Render neu befüllt. Keine externe Lib.

### PromptBoardHost
DI-Container-Bootstrap (Microsoft.Extensions.DependencyInjection + EF Core SQLite). DB-Pfad: `%LOCALAPPDATA%\PromptBoard\promptboard.db`. Registriert alle Repositories und Services. Statischer Prozess-Singleton.

### PromptBoardSecretStore
Liest/schreibt Google-OAuth-Credentials aus `~/SK/PromptBoard/.env` (Felder: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_OAUTH_REFRESH_TOKEN`, `GOOGLE_ACCOUNT_EMAIL`). Atomares Schreiben via Temp-Datei+Rename.

### PromptHistoryDriveSync
Synchronisiert `prompt-history.json` + `archive-NNN.md` über Google Drive `appDataFolder` (separater Dateiname von PromptBoard-Backup). Verwendet denselben Secret-Store und OAuth-Token wie GoogleDriveBackupService. Externe Libs: **Google.Apis.Drive.v3**, **Google.Apis.Auth**.

### PromptHistoryService
Liest/schreibt `%LOCALAPPDATA%\PromptBoard\history\prompt-history.json`. Maximal 100 aktive Einträge (älteste rotieren in MD-Archiv `Terminal Archiv\`, max. 500 Einträge pro Datei). JSON-Format: Array von `{id, text, title, timestamp}` (camelCase). Atomares Schreiben, SemaphoreSlim-Lock gegen Datei-Races.

### TerminalController
Statische Klasse. Sendet Text via Clipboard+Ctrl+V in das Terminal-Fenster (Win32 `keybd_event`/`SendInput`). Operationen: `PasteTextAsync`, `ClearLineAsync` (Home+Shift+End+Delete), `ClearAllInputAsync` (5× Ctrl+U), `CopySelectionAsync` (Ctrl+C), `PasteClipboardAsync`, `SendReturnAsync`. Alle mit `Task.Run`-Wrapper (200ms BringToForeground-Delay auf Hintergrundthread). Externe Lib: **P/Invoke (Win32)**.

### TerminalWatcher
Installiert `SetWinEventHook(EVENT_SYSTEM_FOREGROUND)` und prüft, ob das neue Vordergrundfenster einem Terminal-Prozess gehört (`WindowsTerminal`, `pwsh`, `powershell`, `cmd`, `mintty` — konfigurierbar). PID-Cache mit 1s-TTL. Events: `TerminalActivated(HWND)`, `TerminalDeactivated`. Externe Lib: **Win32 P/Invoke**.

### VoiceServiceProvider
Prozess-weiter Service-Locator. Hält die gemeinsamen Instanzen `AudioRecorder`, `GroqWhisperClient`, `GeminiClient?` und `PromptHistoryService` (Lazy-Init, Double-Checked-Locking).

---

## 4. Hotkeys + Tastatur-Aktionen

Alle Hotkeys laufen über einen einzigen Low-Level-Keyboard-Hook (`WH_KEYBOARD_LL`), der in `InstallPushToTalkHook()` installiert wird.

| Tastenkombination | Aktion |
|---|---|
| **Strg+Alt+Leertaste** (Hold ≥500ms) | Push-to-Talk: Aufnahme läuft solange gehalten |
| **Strg+Alt+Leertaste** (Tap <500ms) | Toggle-Modus: 1×Tap=Start, 1×Tap=Stop+Transkribieren |
| **Shift+Alt+Leertaste** | Alternativkombo für PTT (kein Zoom-Konflikt) |
| **Shift+Alt+M** | Alternativkombo für PTT (bulletproof) |
| **Strg+F9 / Shift+F9** | Weitere PTT-Alternativen |
| **Alt+F12** | Toggle Aufnahme (primärer G-HUB G5-Hotkey, 350ms Cooldown) |
| **Alt+F11** | Explorer-Fenster am Release-Bundle-Pfad öffnen |
| **Strg+Alt+P** | Screenshot aufnehmen + sofort einfügen (OneShot) |
| **Strg+Alt+I** | Letzten Screenshot einfügen (Insert) |
| **Strg+1..Strg+9** | Prompt aus HotkeyRegistry per Nummer einfügen |
| **Win+Alt+A..Win+Alt+Z** | Prompt aus HotkeyRegistry per Buchstabe einfügen |
| Kein direkter Hotkey für Auto-Enter-Toggle (nur via `POST /autoenter/toggle` am HTTP-Server oder Enter-Button-Klick) | — |

**Profile-Tiles (Maustasten):**
- Linksklick → Re-Correct (letzten Roh-Whisper-Text durch neues Profil schicken, Eingabe ersetzen)
- Rechtsklick → nur Profil wechseln, kein Re-Correct

---

## 5. Persistenz / Daten-Format

### Prompts + Kategorien — SQLite-Datenbank
- **Pfad:** `%LOCALAPPDATA%\PromptBoard\promptboard.db`
- **Tabellen:** `Prompts`, `Categories`, `AppSettings`, `AiImprovementPrompts` (TPH-Discriminator)
- **Prompt-Felder:** `Id (Guid)`, `CategoryId`, `ShortLabel`, `OriginalText`, `ImprovedText?`, `ActiveVersion (Original|Improved)`, `IsAlwaysOn`, `IsPrePrompt`, `IsPostPrompt`, `SortOrder`, `HotkeyNumber? (1–9)`, `HotkeyLetter? (A–Z)`, `ImprovedByAiPromptId?`, `CreatedAtUtc`, `UpdatedAtUtc`
- **Kategorie-Felder:** `Id`, `Name`, `BackgroundColorHex`, `SortOrder`, `Type (Standard/System)`, `CreatedAtUtc`, `UpdatedAtUtc`

### AppSettings — SQLite (Singleton-Zeile)
Felder: `GroqApiKey?`, `GeminiApiKey?`, `GoogleOAuthRefreshToken?`, `GoogleClientId?`, `GoogleClientSecret?`, `GoogleAccountEmail?`, `GroqModel` (default `whisper-large-v3-turbo`), `AlwaysOnTop` (bool, true), `BarHeight` (double, 140), `AutoHide` (bool, true), `Orientation` (string, `"vertical"`|`"horizontal"`), `SeparatorTemplate` (string, `"\n\n;\n\n"`), `PersistOverlayPosition` (bool, false), `OverlayVerticalLeft?`, `OverlayVerticalTop?`, `OverlayHorizontalLeft?`, `OverlayHorizontalTop?` (alle double?).

### Drive-Backup JSON (`promptboard-backup.json`)
```json
{
  "schemaVersion": 1,
  "createdAtUtc": "2026-...",
  "appVersion": "...",
  "categories": [
    { "id": "guid", "name": "...", "backgroundColorHex": "#hex",
      "sortOrder": 0, "type": 0, "createdAtUtc": "...", "updatedAtUtc": "..." }
  ],
  "prompts": [
    { "id": "guid", "categoryId": "guid", "shortLabel": "...",
      "originalText": "...", "improvedText": null, "activeVersion": 0,
      "isAlwaysOn": false, "sortOrder": 0, "improvedByAiPromptId": null,
      "isAiImprovementPrompt": false, "geminiModel": null,
      "isActiveForImprovement": false, "createdAtUtc": "...", "updatedAtUtc": "...",
      "hotkeyNumber": null, "hotkeyLetter": null }
  ]
}
```
**Nicht enthalten:** API-Keys, OAuth-Tokens (die bleiben in `~/SK/PromptBoard/.env`).

### Prompt-Historie JSON (`%LOCALAPPDATA%\PromptBoard\history\prompt-history.json`)
```json
[
  { "id": "uuid", "text": "...", "title": "4-Wort-Titel", "timestamp": "2026-..." }
]
```
Max. 100 Einträge aktiv; ältere landen als Markdown in `%LOCALAPPDATA%\PromptBoard\history\Terminal Archiv\archive-001.md` (max. 500 pro Datei, neuester oben). Synchron mit macOS (gleiche Feldnamen, camelCase).

### OAuth-Secrets (`~/SK/PromptBoard/.env`)
Format: KEY=VALUE-Paare. Felder: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_OAUTH_REFRESH_TOKEN`, `GOOGLE_ACCOUNT_EMAIL`.

### Config (`.env` für Groq/Gemini)
Suchpfad-Priorität: `~/SK/VoiceOverlays/.env` → neben `.exe` → CWD → `~/.env` → `%APPDATA%\TerminalVoiceOverlay\.env`. Felder: `GROQ_API_KEY` (Pflicht), `WHISPER_MODEL`, `WHISPER_LANG`, `WHISPER_URL`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_THINKING_LEVEL`, `AUDIO_SAMPLE_RATE`, `AUDIO_CHANNELS`, `TERMINAL_PROCESS_NAMES`.

### Crash-Log
`%LOCALAPPDATA%\PromptBoard\crash.log` (UTF-8, Append).

### Screenshot-Debug-Log
`%LOCALAPPDATA%\PromptBoard\screenshot-debug\screenshot.log`.

---

## Etappen-Status (wird pro Implementations-Schritt aktualisiert)

| # | Etappe | Status |
|---|--------|--------|
| 1a | Beam-Fade-Helpers + Orientation-State | ✅ (#1111) |
| 1b | Horizontales Layout (HBar mit MakeHGroup) | ✅ (#1113) |
| 2  | applyOrientation + beamToOrientation | ✅ (#1114) — Test-Hotkey Cmd+Shift+O |
| 3  | Glide-Animation (Smootherstep) | ✅ (#1115) |
| 4  | Collapsed-Mic-Pille mit BeamFade | ✅ (#1116) — Test-Hotkey Cmd+Shift+C |
| 5a | OrientationToggleButton + SaveButton (UI) | ✅ (#1117) |
| 5b | Drag-Verhalten Rechtsklick + Threshold | offen |
| 5b | Drag-Verhalten Rechtsklick + 4px-Threshold | ✅ (#1120) |
| 6  | Settings-Dialog (620×640) | ✅ (#1120) — Skelett mit allen Feldern, Cmd+Shift+, |
| 7  | Confirm/PromptEdit/PromptHistoryEdit/TextInput-Dialoge | ✅ (#1120) — Skelette in CommonDialogs.swift |
| 8  | PromptInputWindow + PromptHistoryWindow | ✅ — bereits in PromptInputPanel.swift + PromptHistoryPanel.swift |
| 9  | AutoEnterStatusServer (HTTP, 127.0.0.1:5723) | ✅ (#1118) |
| 10 | Hotkey-Registry (Cmd+1..9 ✅, Cmd+Opt+A..Z Stub) | ✅ (#1118) — Letter-Lookup braucht DB-Schema-Migration |
| 11 | AlwaysOnPrefixService + Pre/Post-Prompt-Logik | ✅ — bereits vor Portierung vorhanden |
| 12 | Schwester-Projekt ClaudeCodexVoiceOverlay-macOS angleichen | ✅ (#1120) — Helfer (OverlayOrientation + OverlayGlideAnimation) kopiert, Layout-Adaptation des Panels offen |
