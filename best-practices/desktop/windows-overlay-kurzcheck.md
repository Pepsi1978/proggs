# Windows-Overlay-Fenster (C#/WPF) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Overlay soll oben bleiben, ohne Fokus zu klauen | `Topmost=true` + `ShowActivated=false` + `WS_EX_NOACTIVATE\|WS_EX_TOOLWINDOW` in `OnSourceInitialized` | §2 |
| 2 | Overlay nach vorne zwingen | NIE `SetForegroundWindow`; `SetWindowPos(HWND_TOPMOST, …, SWP_NOACTIVATE)` | §2 |
| 3 | Win11-24H2-Z-Order-Bug (fällt hinter Paint/Photos) | ereignisgetriebener Re-Assert auf `WM_WINDOWPOSCHANGED` (kein 50-ms-Timer) | §2, §10 |
| 4 | Durchklickbares Overlay (click-through) | `WS_EX_TRANSPARENT` **+** `WS_EX_LAYERED`; nur Transparent-Bit togglen | §3 |
| 5 | Teil-Bereiche interaktiv | WPF: `Background="{x:Null}"` (nicht `"Transparent"`) + `IsHitTestVisible` | §3 |
| 6 | Globaler Toggle-Hotkey | `RegisterHotKey` + `MOD_NOREPEAT` in `OnSourceInitialized`, Rückgabe prüfen | §4 |
| 7 | Push-to-Talk (Halten/Loslassen) | `WH_KEYBOARD_LL`-Hook (RegisterHotKey kann kein Release) / SharpHook | §4 |
| 8 | Transparenz + runde Ecken ohne Flackern | `AllowsTransparency=false` + `WindowChrome` + DWM (`DWMWCP_ROUND`/Backdrop) | §5 |
| 9 | Start-Blitz (schwarz/weiß) | `Opacity=0` starten, in `ContentRendered` einblenden; DWM in `SourceInitialized` | §5 |
| 10 | Scharf + richtiger Monitor | `app.manifest` `PerMonitorV2,PerMonitor`; Pixel→DIP teilen; `WindowStartupLocation=Manual` | §6 |
| 11 | System-Tray | `H.NotifyIcon.Wpf` (nicht das inaktive Hardcodet); Multi-Size-.ico | §7 |
| 12 | Autostart | `HKCU\…\Run` mit **gequotetem** `Environment.ProcessPath`; elevated → Task Scheduler | §8 |
| 13 | Single-Instance | Named `Mutex` (`createdNew`) + Named Pipe/`WM_COPYDATA` für Argumente | §9 |
| 14 | Andere Instanz nach vorne holen | `AllowSetForegroundWindow` / Selbst-Aktivierung in alter Instanz | §9, §10 |
| 15 | Aufmerksamkeit ohne Fokus-Klau | `FlashWindowEx` mit `FLASHW_TIMERNOFG` | §10 |
| 16 | Vollbild-App/Spiel erkennen | `SHQueryUserNotificationState` → bei Vollbild ausblenden | §10 |
