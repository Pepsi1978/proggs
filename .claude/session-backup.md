# Session Handoff — 2026-06-15 (CVO ClaudeVoiceOverlay-Windows)

## Ziel
Das **ClaudeVoiceOverlay-Windows (CVO)** vollwertig machen: 1:1-Portierung vom
TerminalVoiceOverlay (TVO), dann mehrere Fixes. Adressat aller Aenderungen: CVO (Windows,
C#/WPF, net10.0-windows). Mac-Pendant = ClaudeCodexVoiceOverlay-macOS (Referenz, NICHT angefasst).

## Aktueller Status (alles erledigt + deployed, CVO laeuft als publish/.exe)
- #46775/#46776 — CVO v2.0.0: volle Feature-Paritaet mit TVO gespiegelt (PromptBoard, Drive-Sync,
  History, Slots, Hotkeys, Settings). AppController/AppWatcher = Electron-Variante.
- #46784 — CVO v2.1.0: **Text-Einfuegen gefixt** (Root Cause: Claude-Desktop-Eingabefeld ist
  contenteditable/Chromium, kein Win32-Edit). Neu: UI Automation findet+fokussiert das Feld
  (FocusedElement -> Baum-Suche Edit/Document -> Render-Widget-Notnagel) + Clipboard + echtes
  Strg+V via SendInput mit KEYEVENTF_SCANCODE. Jede Operation auf STA-Worker-Thread.
- #46788 — CVO v2.1.1: **Drive-Backup teilt sich jetzt mit Mac-Claude** (vorher faelschlich TVO).
  Dateinamen -> promptboard-backup-claudecodex.json / prompt-history-claudecodex.json /
  prompt-slots-claudecodex.json (gleich wie ClaudeCodexVoiceOverlay-macOS).
- #46789 — CVO v2.1.2: **Mikrofone lila statt gelb** (Material Purple), zur Unterscheidung von TVO
  (bleibt gelb). Haupt-Mic + BTW-Mic + eingeklappter Mic. Separate BtnMicIdlePurple (Profil-Tiles
  bleiben gelb via BtnMicIdle).
- Parallele Session (NICHT ich): #46780-46782 net10-Umstellung, Directory.Build.props, Almanach+BP
  windows-electron-text-injection, Pakete aktualisiert.

## OFFEN / Naechste Schritte (priorisiert)
1. **Einfuege-Fix LIVE testen** (durch Frank): in Claude Desktop Chat / Code / Cowork einsprechen,
   pruefen ob Text ankommt. v2.1.0 ist deployed aber Wirksamkeit noch NICHT bestaetigt. Falls es
   hakt: Datei-Logging (JSON-Zeilen) in AppController einbauen (aktuell nur Console.WriteLine =
   in fensterloser EXE unsichtbar).
2. **Drive-Sync mit Mac-Claude testen**: in CVO Settings denselben Google-OAuth-Client + Konto wie
   im Mac-Claude-Overlay verbinden (appDataFolder ist client-scoped!), dann Restore -> Mac-Prompts.
3. Optional: CVO **lokal isolieren** (eigener Ordner %LOCALAPPDATA%\ClaudeVoiceOverlay\ statt
   geteiltem %LOCALAPPDATA%\PromptBoard\) — sonst vermischt sich die lokale DB mit Windows-TVO.
4. Optional: Mac-Claude-Overlay auch lila Mics (Konsistenz).

## Fehlgeschlagene Ansaetze / Fallen (WICHTIG)
- **Deploy/Neustart CVO**: nur EINEN Prozess killen reicht NICHT (Respawn via watcher.vbs +
  Selbst-Watchdog). IMMER alle killen: wscript(watcher.vbs) + beide ClaudeVoiceOverlay.exe, per
  NAME filtern (CommandLine-Filter killt die eigene pwsh!). Single-File laeuft aus TEMP -> publish/
  exe ist nach Start NICHT gelockt, alte Instanz laeuft aber weiter bis gekillt. Neustart per
  Startzeit verifizieren. Neustart = Start-Process wscript.exe watcher.vbs.
- **UIA-Referenz**: <Reference Include="UIAutomationClient/Types"> erzeugt MSB3243-Konflikt
  ("nach Zufallsprinzip"). Loesung: <FrameworkReference Include="Microsoft.WindowsDesktop.App.WPF"/>.
- **SendInput INPUT-struct**: Union MUSS MOUSEINPUT enthalten, sonst sizeof(INPUT)=32 statt 40 ->
  SendInput lehnt mit cbSize-Mismatch STILL ab (Rueckgabe 0). Jetzt korrekt in Win32.cs.
- **keybd_event** fuer Strg+V wird von Chromium ignoriert -> SendInput mit Scancode noetig.
- **Drive-Identitaet**: appDataFolder ist pro OAuth-Client; Dateiname entscheidet welche Apps
  syncen. Mac-Claude nutzt -claudecodex, Mac-TVO/Win-TVO plain. Win-CVO jetzt auf -claudecodex.

## Wichtige Referenzen
- Memory reference_voice_overlay_processes_cvo_tvo (CVO vs TVO Prozesse/Watcher/Ports/Kill-Ablauf).
- bugs/desktop/windows-electron-text-injection.md + best-practices/.../best-practices-windows-electron-text-injection.md
- Claude Desktop installiert: 1.12603.1 (3df4fd) = Electron 41 / Chromium 146 (nativer UIA an).
- CVO Build/Deploy: dotnet build ClaudeVoiceOverlay.csproj (explizit!) ; pwsh -File publish.ps1.
- AutoEnter-Port CVO 5724 / TVO 5723.

## Anker
- Branch: main
- Letzte Commits:
023586f18 #46789 - CVO v2.1.2: Mikrofon lila statt gelb
4305e74b8 #46788 - CVO v2.1.1: Drive-Backup teilt sich mit macOS-Claude
a4cbb8404 #46784 - CVO v2.1.0: Text-Einfuegen gefixt (UIA + SendInput-Scancode)
(parallel: #46780-46782 net10 + Almanach/BP; #46775/#46776 CVO-Portierung)
