# Bekannte Bugs/Fallen: Windows-Overlay-Fenster (C#/WPF) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Sektionen: **A** Always-on-top/Z-Order/Fokus · **C** Click-through/Transparenz/Layered ·
> **H** Hotkeys · **D** DPI/Multi-Monitor · **T** Tray/Autostart/Single-Instance ·
> **W** WindowChrome & DWM-Material · **P** Deployment/Lifecycle/.NET-9-10-Regressionen.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Overlay fällt auf Win11 24H2 hinter Paint/Photos | Periodischer/ereignisgetriebener `SetWindowPos(HWND_TOPMOST, SWP_NOACTIVATE)` | A1 |
| 2 | `ShowInTaskbar=False` → Topmost kaputt | Topmost auf echtem HWND erzwingen / `WS_EX_TOOLWINDOW` | A2 |
| 3 | `SetForegroundWindow` blinkt nur Taskleiste | Nicht aufrufen; `FlashWindowEx(FLASHW_TIMERNOFG)` | A5 |
| 4 | Zwei Topmost-Apps flackern | Ereignisgetrieben re-asserten, kein 50-ms-Timer | A6 |
| 5 | Klicks gehen nicht durch | `WS_EX_TRANSPARENT` **+** `WS_EX_LAYERED` | C1 |
| 6 | „Transparenter" Bereich blockiert Klicks | `Background="{x:Null}"` statt `"Transparent"` | C2 |
| 7 | Style wird auf x64 nicht übernommen | `SetWindowLongPtr` per `IntPtr.Size` | C6 |
| 8 | Ruckeln/WebView2 unsichtbar (Airspace) | `AllowsTransparency=false` + DWM-Weg | C7 |
| 9 | Runde Ecken/Mica greifen nicht | `Background=Transparent` + `WindowChrome` + `EnsureHandle`, Win11 | C9 |
| 10 | Hotkey tut nichts / `RegisterHotKey`=false | Rückgabe + `GetLastWin32Error` (1409) prüfen | H1 |
| 11 | Hotkey feuert nie | In `OnSourceInitialized` registrieren, nicht im Ctor | H2 |
| 12 | Hotkey-Dauerfeuer beim Halten | `MOD_NOREPEAT` | H3 |
| 13 | LL-Hook crasht zufällig | Delegate als **statisches Feld** halten | H5 |
| 14 | Push-to-Talk geht nicht | `WH_KEYBOARD_LL` (RegisterHotKey kann kein Release) | H9 |
| 15 | Overlay unscharf auf 2. Monitor | `app.manifest` `PerMonitorV2,PerMonitor` | D1 |
| 16 | Fenster auf falschem Monitor | `WindowStartupLocation=Manual` vor Show | D2 |
| 17 | Position um Skalierung daneben | Pixel→DIP teilen (`/DpiScaleX`) | D3 |
| 18 | Tray-Icon weg nach Explorer-Neustart | `H.NotifyIcon`/Lib behandelt `TaskbarCreated` | T1 |
| 19 | Autostart startet nicht (Pfad-Leerzeichen) | Pfad **quoten** + `Environment.ProcessPath` | T4 |
| 20 | Mutex blockt Start nach Crash | `createdNew`-Muster statt `WaitOne` | T7 |
| 21 | App crasht beim Hell/Dunkel-Wechsel (.NET 9 Fluent) | `ThemeMode` fest Light/Dark, Theme-Update über Dispatcher | P1 |
| 22 | Clipboard/Drag-Drop wirft `PlatformNotSupportedException` | Eigene Typen als JSON/`byte[]`, kein BinaryFormatter | P5 |
| 23 | Tray-only-App beendet sich sofort | `ShutdownMode=OnExplicitShutdown` | P9 |
| 24 | Maximiert verdeckt Taskleiste / falscher Monitor | `WM_GETMINMAXINFO`-Hook auf `rcWork` des aktuellen Monitors | A20 |
| 25 | Mica/runde Ecken verschwinden bei Resize/Hide-Show | DWM-Attribute bei `WM_SIZE`/`WM_DPICHANGED`/`IsVisibleChanged` neu setzen | W7, W8 |
| 26 | WindowChrome flackert/weiße Streifen beim Resize | `NonClientFrameEdges="Right"` + `UseLayoutRounding` | W4, W6 |
| 27 | Push-to-Talk-DLL fehlt im Release (SharpHook) | Expliziter RID, native Lib mit-entpacken | H10 |
| 28 | Trimming/NativeAOT → WPF startet nicht | Trimming/AOT für WPF nicht nutzen; SingleFile ohne Trim | P7 |
| 29 | Topmost verschwindet bei Win+D / virt. Desktop / Monitor-Sleep | eigenes HWND-Topmost / `IVirtualDesktopManager` / `WM_DISPLAYCHANGE` | A14, A15, A16 |
| 30 | Eingehängtes Overlay (`SetParent`) bleibt deckend, Alpha wirkungslos | `WS_EX_LAYERED` gilt nicht für Kind-HWNDs darin → nicht einhängen, eigenständiges Top-Level darüberlegen und nachführen | C17 |
| 31 | Bildschirmfoto zeigt kein Rot, auf dem Schirm ist alles rot | `BitBlt`/`ImageGrab` erfassen eingehängte Layered-Fenster nicht → `GetLayeredWindowAttributes` je HWND fragen statt fotografieren | C18 |
