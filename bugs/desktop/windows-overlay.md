# Bekannte Bugs/Fallen: Windows-Overlay-Fenster (C#/WPF)

> PFLICHT-LESEN vor Arbeit an Franks Voice-Overlays **TVO** (TerminalVoiceOverlay-Windows) und
> **ClaudeVoiceOverlay-Windows** (immer-im-Vordergrund, globale Hotkeys, transparent).
> Stand: recherchiert am 2026-06-14 in **zwei Durchläufen** — (1) Best-Practices-Lauf, (2) dedizierte
> Bug-Recherche (je 7 Researcher; Fokus Lauf 2: github.com/dotnet/wpf-Issues, PowerToys, reale Vorfälle,
> konkrete Issue-Nummern + Fix-Versionen). ~110 Einträge in 7 Sektionen.
> Versions-Anker: **.NET 10** (`net10.0-windows`), WPF, Windows 10/11. Viele Fenster-/Z-Order-/DPI-Fallen
> leben unverändert in Win32/DWM; NEU sind .NET-9-Fluent-Theme-Crashes und .NET-9-Clipboard-/Deployment-Bugs.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/desktop/windows-overlay.md`](../../best-practices/desktop/windows-overlay.md).

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

---

## A) Always-on-top / Z-Order / Fokus

### A1. Win11 24H2: Topmost-Fenster fällt hinter Paint/Photos/Clipchamp ⭐ HAEUFIG (aktiver Bug)
- **Symptom:** Overlay (und sogar die Taskbar) verschwindet visuell hinter einem nicht-topmost Fenster, sobald Paint/Photos/Clipchamp geöffnet/geschlossen wird; `WS_EX_TOPMOST` ist per Spy++ noch gesetzt. Nur Win11 24H2, nicht Win10.
- **Ursache:** Regression in der Win11-24H2-DWM-Z-Order-Logik. Stand 2026-05 von MS unbestätigt/ungefixt.
- **Versionen:** Windows 11 24H2 (26100.x).
- **FIX:** `SetWindowPos(h, HWND_TOPMOST, 0,0,0,0, SWP_NOMOVE|SWP_NOSIZE|SWP_NOACTIVATE)` re-asserten — ereignisgetrieben auf `WM_WINDOWPOSCHANGED` oder per `DispatcherTimer` (1–3 s). `SWP_NOACTIVATE` zwingend, kein Toggle nötig. Siehe best-practices §2/§10.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/5562457/topmost-windows-temporarily-lose-z-order-priority (community/halb-offiziell)

### A2. `ShowInTaskbar="False"` bricht Topmost ⭐ HAEUFIG
- **Symptom:** Fenster mit `Topmost=true` + `ShowInTaskbar=false` liegt nicht mehr zuverlässig oben.
- **Ursache:** WPF erzeugt bei `ShowInTaskbar=false` ein verstecktes Owner-Window ohne `WS_EX_TOPMOST` → Z-Order-Vererbung Owner→Owned bricht.
- **Versionen:** alle WPF.
- **FIX:** Topmost direkt per `SetWindowPos(HWND_TOPMOST, …, SWP_NOACTIVATE)` am echten HWND erzwingen; oder `WS_EX_TOOLWINDOW` statt `ShowInTaskbar=false` (raus aus Taskbar + Alt-Tab, Topmost bleibt intakt).
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/1181672/overlay-windows-is-not-coming-on-top-when-showinta

### A3. `WS_EX_NOACTIVATE` killt Tab-Navigation im eigenen Fenster
- **Symptom:** Nach Setzen von `WS_EX_NOACTIVATE` funktioniert Tab-Navigation/Tastatur in den eigenen Controls nicht mehr.
- **Ursache:** Ein nicht-aktivierbares Fenster bekommt keinen Tastatur-Fokus. By design.
- **Versionen:** alle.
- **FIX:** Für ein reines Anzeige-Overlay (Voice-Status) gewollt → kein Fix nötig. Braucht das Overlay echte Tastaturbedienung, `WS_EX_NOACTIVATE` weglassen und Fokus-Klau nur über `ShowActivated=false` + selektives `WM_MOUSEACTIVATE` begrenzen.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/1190592/tab-navigation-not-working-after-making-wpf-app-as

### A4. Buttons im `WS_EX_NOACTIVATE`-Fenster reagieren nicht / kein Pressed-State
- **Symptom:** Klick auf einen Button im NOACTIVATE-Overlay löst nichts aus oder zeigt keinen Pressed-Zustand.
- **Ursache:** Ohne Aktivierung wird die Maus-Message ggf. verworfen.
- **Versionen:** alle.
- **FIX:** `HwndSource.AddHook`, `WM_MOUSEACTIVATE` (0x0021) abfangen und `MA_NOACTIVATE` (0x0003) zurückgeben → Maus wird verarbeitet, Fenster bleibt inaktiv. Bei reinem Status-Overlay ohne Buttons entfällt das.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-mouseactivate

### A5. `SetForegroundWindow` flasht nur den Taskleisten-Button ⭐ HAEUFIG
- **Symptom:** Aufruf bringt das Overlay nicht nach vorne, der Taskbar-Button blinkt nur (oder Rückgabe `false`).
- **Ursache:** Foreground-Lock — ein fremder Prozess darf das Vordergrundfenster nur unter engen Bedingungen setzen. By design, kein Bug.
- **Versionen:** alle.
- **FIX:** Für ein Overlay **gar nicht** `SetForegroundWindow` aufrufen; sichtbar/oben über `HWND_TOPMOST` + `SWP_NOACTIVATE`. Aufmerksamkeit über `FlashWindowEx(FLASHW_ALL|FLASHW_TIMERNOFG)`.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow

### A6. Zwei Topmost-Apps „flackern" (eine Z-Order-Gruppe)
- **Symptom:** Mein Overlay und ein anderes Always-on-top-Tool wechseln sich im Vordergrund ab.
- **Ursache:** Alle Topmost-Fenster teilen eine Z-Order-Gruppe; das zuletzt eingefügte/fokussierte gewinnt — es gibt keine „Priorität".
- **Versionen:** alle.
- **FIX:** NICHT mit 50-ms-Timer kontern (Endlosschleife). Ereignisgetrieben auf `WM_WINDOWPOSCHANGED` re-asserten, nur wenn real verdeckt; sonst Intervall ≥1 s + `SWP_NOACTIVATE`. Echtes „immer ganz oben" nur via `uiAccess`-Manifest (`ZBID_UIACCESS`).
- **Quelle:** https://blog.adeltax.com/window-z-order-in-windows-10/ (extern)

### A7. Topmost liegt unter Startmenü / Action Center / Task-Manager
- **Symptom:** Mein Topmost-Overlay wird von System-UI überdeckt.
- **Ursache:** Diese UIs liegen in höheren Z-Order-Bändern (`ZBID_IMMERSIVE_*`, `ZBID_SYSTEM_TOOLS`); Drittanbieter-Topmost (`ZBID_DESKTOP`) kann sie prinzipiell nicht überdecken.
- **Versionen:** Windows 8+.
- **FIX:** Als System-Verhalten akzeptieren. Nur via `uiAccess=true`-Manifest (höchstes Band, signiert + `Program Files`) legitim darüber. Private Band-APIs sind gesperrt.
- **Quelle:** https://blog.adeltax.com/window-z-order-in-windows-10/ (extern)

### A8. Exklusiv-Vollbild (Spiele) überdeckt jedes normale Topmost
- **Symptom:** Über einem Game im Exklusiv-Vollbild ist das Overlay unsichtbar; UWP-Vollbild minimiert sich, wenn man ein Topmost fokussiert.
- **Ursache:** Exklusiv-Vollbild übernimmt die Display-Kontrolle (umgeht DWM); normales `HWND_TOPMOST` liegt darunter.
- **Versionen:** alle.
- **FIX:** Per `SHQueryUserNotificationState` (`QUNS_RUNNING_D3D_FULL_SCREEN`) erkennen und Overlay ausblenden statt erzwingen; Nutzer auf Borderless-Windowed hinweisen; nie `SetForegroundWindow`. 100 % über Exklusiv-Vollbild nur via `uiAccess`.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/shellapi/nf-shellapi-shqueryusernotificationstate

### A9. AppBar lässt nach Crash Bildschirmplatz „verschwinden"
- **Symptom:** Nach Absturz bleibt ein Streifen Desktop reserviert; maximierte Fenster sparen den Bereich weiterhin aus.
- **Ursache:** `ABM_REMOVE` wurde nie gesendet — die System-AppBar-Liste hält die Reservierung.
- **Versionen:** alle.
- **FIX:** `ABM_REMOVE` zwingend in `OnClosing`/`ProcessExit`/globalem Crash-Handler; auf `ABN_POSCHANGED`/`ABN_FULLSCREENAPP` reagieren.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/shell/application-desktop-toolbars

### A10. `FlashWindowEx` aktiviert/stellt wieder her statt nur zu blinken (Win11)
- **Symptom:** Unerwartetes Aktivieren/Wiederherstellen statt reinem Blinken.
- **Ursache:** Falsche Flag-Wahl (`FLASHW_ALL` ohne `FLASHW_TIMERNOFG`) kann je nach Zustand aktivieren.
- **Versionen:** Windows 11.
- **FIX:** `FLASHW_TIMERNOFG` setzen — blinkt bis Vordergrund, ohne zu aktivieren/wiederherzustellen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-flashwinfo

### A11. `AllowSetForegroundWindow`-Recht verfällt sofort
- **Symptom:** Nach `AllowSetForegroundWindow` darf der Zielprozess doch nicht in den Vordergrund.
- **Ursache:** Das Recht erlischt beim nächsten Nutzer-Input (außer er geht an den Zielprozess) oder beim nächsten `AllowSetForegroundWindow` eines anderen Prozesses.
- **Versionen:** alle.
- **FIX:** Recht unmittelbar vor dem `SetForegroundWindow`-Aufruf erteilen, ohne dazwischenliegende Eingaben; auf `false` defensiv mit `FlashWindowEx` zurückfallen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-allowsetforegroundwindow

### A12. „Topmost vs. Topmost": letztes aktiviertes Fenster gewinnt (RDP/Citrix-Vollbild)
- **Symptom:** Das angepinnte Overlay verschwindet hinter einer Vollbild-RDP-/Citrix-Sitzung (`cdviewer.exe`/mstsc), obwohl Topmost — selbst als Admin.
- **Ursache:** Das Remote-Fenster hat selbst `WS_EX_TOPMOST`; innerhalb der Topmost-Gruppe ist das zuletzt aktivierte vorne. „Kein Fenster kann topmoster als ein anderes sein" (MS-bestätigt). Architektur-Grenze, kein Z-Order-Trick hilft.
- **Versionen:** Win10/11; PowerToys-AoT 0.55–0.91 (offen).
- **FIX:** Overlay mit `uiAccess=true` signieren + in `Program Files` installieren → `ZBID_UIACCESS`-Band, liegt über fremden Topmost. Workaround für mstsc: Vollbild kurz verlassen + neu maximieren.
- **Quelle:** https://github.com/microsoft/PowerToys/issues/16631 (extern)

### A13. WPF-Popup/ContextMenu/Tooltip wandert nicht mit dem Owner im Z-Order
- **Symptom:** Bei `Topmost=true` am Hauptfenster klebt ein WPF-`Popup`/ContextMenu/Tooltip über fremden Apps oder erscheint umgekehrt hinter dem eigenen Topmost-Fenster.
- **Ursache:** WPF-Popups sind eigene HWNDs außerhalb des Visual-Trees; die Runtime aktualisiert den Popup-Z-Index nicht, wenn der Owner wandert — Topmost wird einmalig geerbt und behalten.
- **Versionen:** alle WPF.
- **FIX:** Eigene `PopupNonTopmost : Popup` ableiten und im `OnOpened` den Topmost-Stil per `SetWindowPos` (`HWND_TOP`/`HWND_NOTOPMOST`) neu berechnen (gecachtes `_appliedTopMost` zurücksetzen).
- **Quelle:** https://gist.github.com/flq/903202 (extern)

### A14. Topmost-Overlay verschwindet bei „Desktop anzeigen" (Win+D) / Aero-Peek
- **Symptom:** Topmost-Fenster wird bei Win+D/Peek minimiert/verdeckt, obwohl `Topmost=true` (zusätzlich zum ShowInTaskbar-Fall A2).
- **Ursache:** „Show Desktop" zieht ein Desktop-Fenster nach vorne; WPF setzt `WS_EX_TOPMOST` nicht zuverlässig auf den versteckten Owner-HWND.
- **Versionen:** Win10/11.
- **FIX:** `WS_EX_TOPMOST` direkt per `SetWindowLong`/`SetWindowPos` auf den eigenen HWND zwingen (nicht auf WPFs Owner-Fenster verlassen); robust per `WH_GETMESSAGE`-Hook die Show-Desktop-Nachricht (`WM_USER+83` an Progman) abfangen.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/2127546/

### A15. Topmost-Overlay nur auf EINEM virtuellen Desktop sichtbar
- **Symptom:** Overlay (OSD/Logo) erscheint nur auf dem virtuellen Desktop, auf dem es erstellt wurde; nach Desktop-Wechsel weg.
- **Ursache:** `Topmost` wirkt nur innerhalb des aktuellen virtuellen Desktops; „auf allen Desktops" ist ein separates Konzept (`IVirtualDesktopManager`).
- **Versionen:** Win10/11.
- **FIX:** Fenster explizit per `IVirtualDesktopManager` (bzw. `mntone/VirtualDesktop`) auf „alle Desktops anheften"; Topmost zusätzlich beibehalten.
- **Quelle:** https://github.com/mntone/VirtualDesktop (extern)

### A16. Multi-Monitor: Topmost-Fenster off-screen / falscher Monitor nach Monitor-Sleep
- **Symptom:** Nach Monitor-Standby (besonders DisplayPort/HDMI-TV) liegen Fenster inkl. Topmost-Overlays auf dem falschen Monitor oder außerhalb des sichtbaren Bereichs.
- **Ursache:** DP-Monitore melden sich beim Sleep komplett ab (wie physisches Abstecken) → Windows re-detektiert und repositioniert; Topmost rettet die Position nicht.
- **Versionen:** Win10/11, hardwareabhängig; OS-seitig ab Build ≥ 26100 teils gefixt.
- **FIX:** `WM_DISPLAYCHANGE`/`DisplaySettingsChanged` abfangen, Bounds gegen `Screen.AllScreens` validieren + Topmost neu setzen; Ziel-Monitor per Geräte-ID (nicht Index) persistieren.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/ (Multi-Monitor sleep/wake) (extern)

### A17. UAC-Prompt (Secure Desktop) erscheint HINTER dem eigenen Topmost-Fenster
- **Symptom:** Während `Topmost=true` taucht der UAC-Prompt hinter dem Overlay auf bzw. das Fenster verliert nach Rückkehr vom Secure Desktop seinen Topmost-Zustand.
- **Ursache:** Der Secure Desktop ist eine isolierte Umgebung; der Übergang bricht/überdeckt den Topmost-Zustand.
- **Versionen:** Win10/11.
- **FIX (von MS so umgesetzt):** Vor dem Auslösen einer Elevation Topmost sichern, `Topmost=false` setzen, nach Rückkehr wiederherstellen.
- **Quelle:** https://github.com/microsoft/accessibility-insights-windows/pull/337/files (extern)

### A18. `SetWindowPos(HWND_TOPMOST)` in einer Schleife → CPU-Last, Flackern, Input-Lag
- **Symptom:** Topmost per Timer/Schleife „erzwungen" erzeugt CPU-Last und sichtbares Flackern.
- **Ursache:** Jeder `SetWindowPos`-Aufruf löst Z-Order-Neuberechnung + Paint/Redraw aus, auch ohne Änderung.
- **Versionen:** Win10/11 (Anti-Pattern).
- **FIX:** Immer `SWP_NOMOVE|SWP_NOSIZE|SWP_NOACTIVATE`; nur bei echter Zustandsänderung aufrufen — ereignisgetrieben auf `WM_WINDOWPOSCHANGED`/Fokuswechsel statt blind pollen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos

### A19. Win11 24H2: „rude window"-Logik schiebt Taskleiste vor Topmost / Borderless-Fullscreen inkonsistent
- **Symptom:** Auf 24H2 erscheint die Taskleiste plötzlich vor allen Fenstern (auch Topmost), bzw. Borderless-Fullscreen verdeckt Topmost-Overlays inkonsistent.
- **Ursache:** Regression in der „rude window"-Detection + Flip-Model-Umstellung bei windowed games (seit 22000.556, in 24H2).
- **Versionen:** Win11 24H2 (Build 26100).
- **FIX:** Reine Overlays: `uiAccess=true` (über der Shell). „Optimizations for windowed games" testweise aus; Diagnose-Tool `RudeWindowFixer`.
- **Quelle:** https://github.com/dechamps/RudeWindowFixer (extern)

### A20. `WindowStyle=None` maximiert verdeckt Taskleiste / falscher Monitor / Style-Toggle zerstört Maße
- **Symptom:** Borderless maximiert deckt die Taskleiste ab und ragt 1px über jeden Rand; auf Multi-Monitor wird auf den Primärmonitor maximiert; Style-Wechsel `None→SingleBorder→None` zur Laufzeit liefert falsche Größe.
- **Ursache:** Ohne Standard-Rahmen liefert WPF dem DWM keine korrekte Maximize-Box; `SystemParameters.WorkArea`-Binding versagt bei Taskleiste oben/seitlich.
- **Versionen:** alle WPF (#3766/#2242 offen).
- **FIX:** `WM_GETMINMAXINFO` per `HwndSource.AddHook` abfangen, `MonitorFromWindow(MONITOR_DEFAULTTONEAREST)`+`GetMonitorInfo`, `ptMaxPosition`/`ptMaxSize` auf `rcWork` (NICHT `rcMonitor`) des aktuellen Monitors setzen. WindowStyle nicht zur Laufzeit togglen.
- **Quelle:** https://github.com/dotnet/wpf/issues/3766 · https://github.com/dotnet/wpf/issues/2242 (extern)

---

## C) Click-through / Transparenz / Layered

### C1. `WS_EX_TRANSPARENT` klickt ohne `WS_EX_LAYERED` nicht zuverlässig durch ⭐ HAEUFIG
- **Symptom:** `WS_EX_TRANSPARENT` allein gesetzt, Klicks gehen trotzdem nicht (oder unzuverlässig) durch.
- **Ursache:** Hit-Test-Transparenz greift bei Top-Level-Fenstern zuverlässig erst, wenn das Fenster *layered* ist (RIT prüft dann die Bitmap-Transparenz).
- **Versionen:** alle.
- **FIX:** `WS_EX_LAYERED` mit-setzen — in WPF erledigt das `AllowsTransparency="True"`. Robuster Weg: `AllowsTransparency=true` + nachträgliches `WS_EX_TRANSPARENT`.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/winmsg/extended-window-styles

### C2. `Background="Transparent"` blockiert Klicks, `{x:Null}` lässt sie durch ⭐ HAEUFIG
- **Symptom:** Voll „transparenter" Bereich fängt trotzdem alle Klicks ab.
- **Ursache:** WPF behandelt jeden gefüllten Brush — auch `Transparent` — als „solides Glas" im Hit-Test; nur *kein* Brush ist hohl.
- **Versionen:** alle WPF.
- **FIX:** Durchlässige Flächen `Background="{x:Null}"`; interaktive Controls echten Brush. (Auf HWND-Ebene nur wirksam, wenn das Fenster layered + per-pixel-transparent ist.)
- **Quelle:** https://learn.microsoft.com/en-us/archive/blogs/dwayneneed/transparent-windows-in-wpf

### C3. Touch-Events verschwinden bei `WS_EX_TRANSPARENT`
- **Symptom:** Overlay empfängt nur Maus, keine Touch-Events mehr.
- **Ursache:** `WS_EX_TRANSPARENT` ist Hit-Test-Transparenz; der RIT überspringt das Fenster auch für Touch. By design (Win32).
- **Versionen:** alle.
- **FIX:** Touch separat über Raw-Touch/LL-Hook abgreifen; oder click-through nur bei Bedarf togglen; oder per-pixel-transparente Pixel (`{x:Null}`) statt `WS_EX_TRANSPARENT` (lassen Touch durch UND opake Bereiche treffbar).
- **Quelle:** https://github.com/dotnet/wpf/issues/3145 (extern)

### C4. Hover UND Durchklicken gleichzeitig unmöglich
- **Symptom:** Entweder Klicks abfangen + Hover sehen, oder durchklicken + blind.
- **Ursache:** Mit `WS_EX_TRANSPARENT` bekommt das Fenster gar keine Maus-Messages (auch kein `WM_MOUSEMOVE`).
- **Versionen:** alle.
- **FIX:** Fast-transparenter Brush (`#01000000`, opakes Pixel, optisch fast unsichtbar) ODER globaler `WH_MOUSE_LL`-Hook zum Hover-Lesen.
- **Quelle:** https://learn.microsoft.com/en-us/archive/blogs/dwayneneed/transparent-windows-in-wpf

### C5. Layered-Modus (`WS_EX_LAYERED`) zur Laufzeit nicht umschaltbar in WPF
- **Symptom:** Versuch, `AllowsTransparency`/das Layered-Bit nach dem Anzeigen zu ändern, schlägt fehl.
- **Ursache:** WPF legt den Layered-Modus nur bei der Konstruktion fest.
- **Versionen:** alle WPF.
- **FIX:** Layered fix lassen, nur das **`WS_EX_TRANSPARENT`-Bit** togglen (erlaubt). Für echten Moduswechsel neues Fenster + Element-Tree übertragen.
- **Quelle:** https://learn.microsoft.com/en-us/archive/blogs/dwayneneed/transparent-windows-in-wpf

### C6. `SetWindowLong` 32-bit vs. 64-bit (Style nicht übernommen / Werte abgeschnitten)
- **Symptom:** Style wird auf x64 nicht übernommen, Handle-Werte abgeschnitten.
- **Ursache:** Klassisches `SetWindowLong` arbeitet mit 32-bit `LONG`; auf x64 braucht es `SetWindowLongPtr`.
- **Versionen:** alle (x64).
- **FIX:** `GetWindowLongPtr`/`SetWindowLongPtr` per `IntPtr.Size`-Dispatch; bei Frame-Styles `SetWindowPos(..., SWP_FRAMECHANGED)` (für reines Transparent-Toggle meist nicht nötig).
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowlongw

### C7. `AllowsTransparency=true`: hohe CPU/GPU, Ruckeln, Airspace (WebView2/Media unsichtbar) ⭐ HAEUFIG
- **Symptom:** Overlay langsam (v.a. bei Animationen/Resize); eingebetteter Browser/Video bleibt schwarz/leer.
- **Ursache:** `AllowsTransparency=true` erzwingt den `UpdateLayeredWindow`-Pfad (Per-Pixel-Alpha, Software-Composition) + Airspace (Layered-Window zeigt nur die WPF-Bitmap; Child-HWNDs werden nicht einkomponiert).
- **Versionen:** alle WPF.
- **FIX:** `AllowsTransparency=false` + Win11-DWM-Weg: `WindowStyle=None`, `Background=Transparent`, `WindowChrome GlassFrameThickness="-1"`, runde Ecken via `DWMWA_WINDOW_CORNER_PREFERENCE`, Material via `DWMWA_SYSTEMBACKDROP_TYPE`. Child-HWNDs sind dann normal sichtbar.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/desktop/wpf/advanced/graphics-rendering-tiers · https://dwayneneed.github.io/wpf/2013/02/26/mitigating-airspace-issues-in-wpf-applications.html (extern)

### C8. Schwarzer/weißer Blitz beim ersten Anzeigen
- **Symptom:** Beim Start blitzt das Overlay kurz schwarz/weiß auf.
- **Ursache:** Fenster wird angezeigt, bevor der erste Frame komponiert ist; bei Backdrops zusätzlich, wenn Dark-Mode nach dem Material gesetzt wird.
- **Versionen:** alle WPF.
- **FIX:** Fenster `Hidden`/`Opacity=0` starten, in **`ContentRendered`** einblenden; DWM-Attribute in `SourceInitialized`/nach `EnsureHandle`; `DWMWA_USE_IMMERSIVE_DARK_MODE` **vor** dem Backdrop.
- **Quelle:** https://tvc-16.science/mica-wpf.html (extern)

### C9. Abgerundete Ecken / Mica greifen nicht; Fenster bleibt eckig/weiß
- **Symptom:** `DWMWCP_ROUND`/Backdrop zeigen keine Wirkung.
- **Ursache:** (a) Per-Pixel-Alpha-Layering (`AllowsTransparency=true`) kann grundsätzlich nicht gerundet werden; (b) WPF-`Background` nicht `Transparent`; (c) kein `WindowChrome`/`WindowStyle=None`; (d) läuft auf Windows 10 (Attribute No-Ops); (e) Fenster maximiert/gesnapped.
- **Versionen:** Win11 22000+ für die Attribute.
- **FIX:** `AllowsTransparency=false` + `Background=Transparent` + `WindowChrome GlassFrameThickness="-1"`; Aufruf nach `EnsureHandle()`; Win11 voraussetzen, Win10 sauber degradieren.
- **Quelle:** https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-rounded-corners

### C10. ClearType-Text unscharf auf transluzentem Hintergrund
- **Symptom:** Schrift auf transparenter/Acrylic-Fläche wirkt unsauber/„fett".
- **Ursache:** ClearType-Subpixel-Rendering funktioniert nicht sauber über transparenten Pixeln.
- **Versionen:** alle WPF.
- **FIX:** `TextOptions.TextFormattingMode="Display"` und/oder `TextRenderingMode="Grayscale"`; Text auf eine leicht deckende Sub-Fläche (halbtransparente `Border`) statt direkt auf 0-Alpha legen.
- **Quelle:** https://github.com/dotnet/wpf/issues/8545 (extern)

### C11. `PrintWindow`/IntelliPoint-Race lässt Layered-WPF-Fenster crashen
- **Symptom:** Sporadische `E_INVALIDARG`-Abstürze in `UpdateLayeredWindow`, oft mit Microsoft-IntelliPoint-Maustreiber.
- **Ursache:** `PrintWindow` schaltet das Fenster kurz in System-Redirected-Modus; WPF rendert auf separatem Thread → Race.
- **Versionen:** in WPF seit 3.5 SP1 / .NET (Core)/.NET 10 enthalten (Retry).
- **FIX:** Kein Handlungsbedarf bei Standard-WPF; bei eigenem `UpdateLayeredWindow`-Pfad selbst Retry einbauen.
- **Quelle:** https://learn.microsoft.com/en-us/archive/blogs/dwayneneed/transparent-windows-in-wpf

### C12. `IsHitTestVisible="False"` am Window macht es NICHT click-through
- **Symptom:** `IsHitTestVisible="False"` aufs `Window` gesetzt — Klicks gehen trotzdem nicht durch zur App dahinter.
- **Ursache:** `IsHitTestVisible` ist eine `UIElement`-Property, die nur den WPF-internen Visual-Hittest beeinflusst, nicht den Win32-HWND-Hittest. Das Fenster bleibt auf HWND-Ebene klickbar.
- **Versionen:** alle WPF (#7474/#3088 offen).
- **FIX:** Für echtes Durchklicken `WS_EX_TRANSPARENT | WS_EX_LAYERED` per `SetWindowLong(GWL_EXSTYLE)` in `SourceInitialized` setzen — nicht auf `IsHitTestVisible` verlassen (siehe C1).
- **Quelle:** https://github.com/dotnet/wpf/issues/7474 (extern)

### C13. `AllowsTransparency=true` + `DropShadowEffect`: viele Overlays → UI friert
- **Symptom:** Ab ~10 gleichzeitig offenen transparenten Fenstern mit `DropShadowEffect` ruckelt/staut die UI massiv; entfernt man nur `AllowsTransparency`+`DropShadowEffect`, ist sie wieder flüssig.
- **Ursache:** Jedes Layered-Window mit per-pixel-Alpha + Blur-Effekt erzwingt temporäre Zwischen-Render-Flächen pro Fenster — skaliert nicht über viele Transparenzfenster.
- **Versionen:** .NET FW 4.7.2 + .NET 6+ (#7857 offen).
- **FIX:** Schatten nicht per `DropShadowEffect` aufs Live-Visual; stattdessen DWM-Schatten oder vorgerendertes Schatten-PNG/`CacheMode="BitmapCache"`. Zahl gleichzeitiger Transparenz-Overlays minimieren (ein Fenster, mehrere Visuals).
- **Quelle:** https://github.com/dotnet/wpf/issues/7857 (extern)

### C14. Transparentes Fenster wird beim „Fenster teilen" (Teams/Meet) komplett schwarz
- **Symptom:** Beim „Fenster teilen" sehen andere Teilnehmer den Transparenzbereich (`AllowsTransparency=true`, Alpha=0) als schwarze Fläche; „Bildschirm teilen" ist OK.
- **Ursache:** Die Capture-Software greift den Inhalt per `PrintWindow`/DWM-Thumbnail ab und interpretiert Alpha=0 als Schwarz (kein Alpha im Capture-Stream).
- **Versionen:** .NET FW 4.8 + .NET 6, Win11 22H2 (#7724 offen).
- **FIX:** „near-transparent" Background `#01000000` (Alpha=1) statt voll transparent → beim Sharing fast unsichtbar statt schwarz; oder Overlay als Child-Region desselben HWND.
- **Quelle:** https://github.com/dotnet/wpf/issues/7724 (extern)

### C15. Fenster wird nach Remote-Desktop-Reconnect schwarz bei `Visibility.Collapsed`
- **Symptom:** Per `Visibility.Collapsed` verstecktes und über eine frisch verbundene RDP-Sitzung wieder gezeigtes Fenster ist komplett schwarz; `Hide()`/`Visibility.Hidden` nicht betroffen.
- **Ursache:** Bei `Collapsed` bleibt das Fenster im Visual-Tree, wird aber nicht gerendert; RDP wechselt beim Reconnect die Render-Pipeline und WPF re-paintet nicht.
- **Versionen:** Win10/Server, .NET Core WPF (#8486, „use Hide()").
- **FIX:** Overlay nie über `Visibility.Collapsed` verstecken — `window.Hide()`/`Visibility.Hidden`/`Opacity=0` verwenden (RDP-sicher, funktionsidentisch).
- **Quelle:** https://github.com/dotnet/wpf/issues/8486 (extern)

### C16. `AllowsTransparency=true` killt das Resizen an Kanten/Ecken
- **Symptom:** Mit `AllowsTransparency=true` (oft für runde Ecken/Schatten) funktioniert Maus-Resize an Kanten/Ecken gar nicht mehr.
- **Ursache:** Das Layered-Window deaktiviert den vom DWM/WindowChrome bereitgestellten Resize-Border-Hit-Test; WPF leitet keine `WM_NCHITTEST`-Treffer für die Resize-Zonen weiter.
- **Versionen:** alle WPF.
- **FIX:** `AllowsTransparency` vermeiden; runde Ecken/Schatten über DWM (W4) + `WindowChrome ResizeBorderThickness`. Wenn Transparenz nötig: eigener `WM_NCHITTEST`-Hook gibt `HTLEFT`/`HTTOP`/`HTBOTTOMRIGHT` etc. zurück.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/644595/

### C17. Eingehängtes Kindfenster (`SetParent`): Deckkraft gilt NICHT für seinen Inhalt ⭐ HAEUFIG
- **Symptom:** Ein Overlay wird per `SetParent` in ein fremdes Fenster eingehängt und mit `SetLayeredWindowAttributes(..., LWA_ALPHA)` fast durchsichtig gestellt — es bleibt trotzdem **voll deckend**. Jede Änderung des Alpha-Werts bleibt wirkungslos. Als eigenständiges Fenster funktioniert derselbe Aufruf.
- **Ursache:** `WS_EX_LAYERED` wirkt nur auf die Zeichnung des Fensters **selbst**. Kindfenster mit eigenem HWND (jedes Widget in Tk/WinForms, jeder Control-Handle) sind nicht Teil dieser Ebene und werden ungefiltert darüber gemalt. Bei einem Top-Level-Fenster fällt das nicht auf, weil dort die gesamte Fensterfläche über eine Ebene komponiert wird.
- **Versionen:** alle (layered Kindfenster überhaupt erst ab Windows 8).
- **Erkennen:** `GetLayeredWindowAttributes` je HWND abfragen, inklusive `EnumChildWindows`:
  ```
  TkTopLevel : WS_EX_LAYERED True,  Alpha 1/255      <- hier sitzt die Durchsichtigkeit
  TkChild    : WS_EX_LAYERED False, keine Deckkraft  <- und das malt deckend darüber
  ```
- **FIX:** Nicht einhängen. Das Overlay als eigenständiges Top-Level-Fenster (`WS_EX_TOOLWINDOW`, topmost) über das Zielfenster legen und seine Lage nachführen — dann greift die Deckkraft auf den gesamten Inhalt. Wer einhängen *muss*, darf im Kindfenster nichts zeichnen, was eigene HWNDs erzeugt.
- **Gemessen:** 14.08.2026, Werkzeug `Werkzeuge/zeigefinger` (Python/Tk über scrcpy), Windows 11 26200.

### C18. Falsche Fährte: Bildschirmfotos zeigen eingehängte Layered-Fenster nicht ⭐ HAEUFIG
- **Symptom:** Auf dem Schirm ist die Fläche knallig eingefärbt, das Bildschirmfoto zeigt sie **nicht**. Eine Pixel-Messung „vorher/nachher" meldet Farbunterschied 0 — und man sucht den Fehler an der falschen Stelle.
- **Ursache:** Aufnahmen über den Bildschirm-DC (`BitBlt`, PIL `ImageGrab`, viele Screenshot-Skripte) erfassen so ein Fenster nicht verlässlich; die Zusammensetzung passiert erst im DWM.
- **Versionen:** alle mit DWM.
- **FIX:** Für Transparenz-Fragen **nie** ein Bildschirmfoto als Beweis nehmen. Windows direkt befragen: `GetWindowLong(GWL_EXSTYLE)` auf `WS_EX_LAYERED` prüfen und `GetLayeredWindowAttributes` nach Alpha/Farbschlüssel fragen — je HWND, Kindfenster eingeschlossen. Zum Abfotografieren sonst `PrintWindow` mit `PW_RENDERFULLCONTENT` statt `BitBlt`.
- **Gemessen:** 14.08.2026, selber Fall wie C17 — drei Messrunden liefen ins Leere, bevor die direkte Abfrage die Ursache zeigte.

---

## H) Hotkeys

### H1. `RegisterHotKey` gibt `false` / Hotkey tut nichts ⭐ HAEUFIG
- **Symptom:** `RegisterHotKey` liefert `false`, Hotkey feuert nie.
- **Ursache:** Kombi bereits belegt (`1409 ERROR_HOTKEY_ALREADY_REGISTERED`) — andere App oder zweite Instanz.
- **Versionen:** alle.
- **FIX:** Rückgabewert + `Marshal.GetLastWin32Error()` prüfen; bei Konflikt Alternativkombi anbieten und neu registrieren. Nie still ignorieren.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-registerhotkey

### H2. Hotkey feuert nie, Handle ist `Zero` (im Konstruktor registriert) ⭐ HAEUFIG
- **Symptom:** Im Konstruktor registriert → nie eine `WM_HOTKEY`.
- **Ursache:** HWND existiert vor `SourceInitialized` nicht (`WindowInteropHelper.Handle == IntPtr.Zero`).
- **Versionen:** alle WPF.
- **FIX:** Registrierung in `OnSourceInitialized` (bzw. nach `SourceInitialized`-Event).
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-registerhotkey

### H3. Hotkey-Dauerfeuer beim Gedrückthalten
- **Symptom:** Beim Halten mehrere `WM_HOTKEY`/s → Overlay toggelt mehrfach.
- **Ursache:** `MOD_NOREPEAT` fehlt.
- **Versionen:** Windows 7+.
- **FIX:** `MOD_NOREPEAT` (0x4000) in `fsModifiers` mit OR verknüpfen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-registerhotkey

### H4. Hotkey bleibt nach App-Ende systemweit belegt
- **Symptom:** Nach Schließen/Crash funktioniert die Kombi in keiner App mehr (bis Reboot).
- **Ursache:** `UnregisterHotKey` wurde nicht aufgerufen.
- **Versionen:** alle.
- **FIX:** In `OnClosed`/`Dispose` immer `UnregisterHotKey(handle, id)` + `RemoveHook`; bei Crash-Resilienz globalen Cleanup.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-registerhotkey

### H5. Low-Level-Hook crasht die App zufällig (`ExecutionEngineException`) ⭐ HAEUFIG
- **Symptom:** Push-to-Talk-Hook läuft anfangs, App stürzt später ohne klare Ursache ab.
- **Ursache:** Das Hook-Delegate wurde nur lokal referenziert und vom GC verschoben/eingesammelt.
- **Versionen:** alle .NET.
- **FIX:** Delegate als **statisches Feld** (dauerhaft gehaltene Instanz) speichern; Hook-Proc als statische Methode.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowshookexw

### H6. Low-Level-Hook wird unbemerkt deaktiviert (`LowLevelHooksTimeout`)
- **Symptom:** Push-to-Talk funktioniert nach einiger Zeit/unter Last nicht mehr, ohne Fehler.
- **Ursache:** Callback überschreitet den `LowLevelHooksTimeout` (max. 1000 ms) → Win7+ entfernt den Hook still.
- **Versionen:** Windows 7+.
- **FIX:** Callback minimal halten (nur vkCode lesen, Flag setzen), schwere Arbeit auf Worker/Dispatcher; `CallNextHookEx` sofort; Hook ggf. periodisch neu setzen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/winmsg/lowlevelkeyboardproc

### H7. Hotkey greift nicht über Admin-Fenster / UAC-Dialog (UIPI)
- **Symptom:** Overlay-Hotkey funktioniert überall, nur nicht solange ein elevated Fenster/UAC-Dialog im Vordergrund ist.
- **Ursache:** UIPI blockiert `WM_HOTKEY` (und LL-Hook-Events) von niedriger- an höher-privilegierte Prozesse.
- **Versionen:** alle (mit UAC).
- **FIX:** App und Zielprozesse auf gleichem Integritätslevel; oder bewusst akzeptieren. Echte Cross-Privilege-Reichweite nur via `uiAccess=true`-Manifest (signiert + `Program Files`) — meist Overkill.
- **Quelle:** https://github.com/Chaoses-Ib/Windows/blob/main/Kernel/Security/UIPI.md (extern)

### H8. `F12` / reservierte Kombis nicht registrierbar
- **Symptom:** F12-Hotkey funktioniert nicht; manche `MOD_WIN`-Kombis schlagen fehl.
- **Ursache:** F12 ist permanent dem Debugger reserviert; viele Win-Kombis gehören dem OS.
- **Versionen:** alle.
- **FIX:** Andere Tasten/Modifier (Ctrl/Alt/Shift-Kombis) wählen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-registerhotkey

### H9. Push-to-Talk mit `RegisterHotKey` unmöglich (kein Release-Event)
- **Symptom:** „Halten = an, Loslassen = aus" lässt sich mit `RegisterHotKey` nicht bauen.
- **Ursache:** `RegisterHotKey` postet nur `WM_HOTKEY` beim Drücken — kein Up-Event.
- **Versionen:** alle.
- **FIX:** `WH_KEYBOARD_LL`-Hook auf `WM_KEYDOWN`/`WM_SYSKEYDOWN` (an) vs. `WM_KEYUP`/`WM_SYSKEYUP` (aus); oder Lib **SharpHook** (echte KeyReleased-Events). Hook-Regeln aus H5/H6 beachten.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/winmsg/lowlevelkeyboardproc · https://github.com/TolikPylypchuk/SharpHook (extern)

### H10. SharpHook: `DllNotFoundException` 'uiohook' bei AnyCPU/Single-File/NativeAOT ⭐ HAEUFIG
- **Symptom:** Beim Start `Unable to load DLL 'uiohook' … (0x8007007E)` — im Debug auf dem Dev-Rechner ok, beim Kunden Crash. Besonders AnyCPU, Single-File, NativeAOT.
- **Ursache:** SharpHook lädt die native `uiohook.dll` aus den RID-Ordnern; bei AnyCPU ist die Bitness unklar, bei Single-File/NativeAOT werden native Assets nicht neben die EXE extrahiert.
- **Versionen:** SharpHook 1.x–7.x.
- **FIX:** Kein AnyCPU — expliziter `<RuntimeIdentifier>win-x64</RuntimeIdentifier>`; bei Single-File `<IncludeNativeLibrariesForSelfExtract>true</IncludeNativeLibrariesForSelfExtract>`; VC++-Runtime sicherstellen; bei NativeAOT native Assets als Content mit `CopyToOutputDirectory`.
- **Quelle:** https://github.com/TolikPylypchuk/SharpHook/issues/32 (extern)

### H11. SharpHook: zweite Hook-Instanz korrumpiert globalen libuiohook-State
- **Symptom:** Eine zweite `IGlobalHook`-Instanz (oder zweimal `RunAsync`) → verschluckte Events, einfrierender Hook, Crash; `TaskPoolGlobalHook` ignoriert `SuppressEvent` still.
- **Ursache:** `libuiohook` erlaubt nur EINE Callback-Registrierung pro Prozess; bei `TaskPoolGlobalHook` läuft der Handler auf Pool-Threads, der native Callback ist längst zurück → Suppression wirkungslos.
- **Versionen:** alle (by design).
- **FIX:** Genau EINE Hook-Instanz als Singleton; alte vor neuer `Dispose()`-en; für Event-Suppression (Push-to-Talk/Key-Blocking) `SimpleGlobalHook` statt `TaskPoolGlobalHook`.
- **Quelle:** https://sharphook.tolik.io/ (Docs) (extern)

### H12. Multi-Window/-Instance: gleiche Hotkey-ID scheitert / WM_HOTKEY feuert an alle Handler
- **Symptom:** In einer Multi-Window-App schlägt `RegisterHotKey` fürs zweite Fenster mit gleicher ID fehl; in WPF mit geteiltem `ComponentDispatcher` feuert WM_HOTKEY scheinbar mehrfach.
- **Ursache:** Hotkey-IDs sind thread-lokal eindeutig; mehrere Fenster auf demselben UI-Thread teilen den ID-Raum, der Dispatcher routet WM_HOTKEY an alle Handler im Thread.
- **Versionen:** Win App SDK 1.6.x, analog WPF (#10073).
- **FIX:** Pro Fenster/Instanz eindeutige ID (Zähler/`GlobalAddAtom`); besser ein zentrales message-only Fenster, das den Hotkey einmal registriert und intern ans aktive Fenster routet.
- **Quelle:** https://github.com/microsoft/microsoft-ui-xaml/issues/10073 (extern)

### H13. LL-Keyboard-Hook von Kernel-Anti-Cheat (EAC/BattlEye/Vanguard) blockiert/gewertet
- **Symptom:** Hotkey/Push-to-Talk funktioniert auf dem Desktop, aber sobald ein Spiel mit Kernel-Anti-Cheat im Vordergrund ist, kommen keine Events; im Extremfall „tampering"-Report/Kick.
- **Ursache:** Kernel-Anti-Cheats inspizieren Hook-Ketten/Dispatch-Tables. Reines Lauschen wird meist toleriert, Input-*Injektion* (`SendInput`/Makros) als Cheat gewertet.
- **Versionen:** aktuell 2025/26 (Valorant/Vanguard, EAC, BattlEye).
- **FIX:** Nur passiv lauschen, niemals Input ins Spiel injizieren; für Spiele-Kompatibilität auf **Raw Input** (`WM_INPUT`/`RegisterRawInputDevices`) statt LL-Hook umstellen; App nicht elevated.
- **Quelle:** https://s4dbrd.github.io/posts/how-kernel-anti-cheats-work/ (extern)

### H14. Hotkey nur auf bestimmtem Tastatur-Layout (AltGr, non-US, Dvorak) — VK-Mismatch
- **Symptom:** Hotkey funktioniert auf US-QWERTY, nicht auf DE/Dvorak; `Ctrl+Alt+X` feuert ungewollt beim normalen Tippen.
- **Ursache:** VK-Codes sind layout-abhängig; AltGr ist intern `LCtrl+RAlt` → ein als `Ctrl+Alt+X` registrierter Hotkey kollidiert mit AltGr-Zeichen (`@`, `€`).
- **Versionen:** generisch Windows.
- **FIX:** Hotkeys über Scancodes (`MapVirtualKeyEx`) an die physische Taste binden oder per „Taste drücken zum Belegen" live erfassen; `Ctrl+Alt` meiden, `Ctrl+Shift`/`Win` nutzen; im LL-Hook das gelieferte `vkCode`/`scanCode` auswerten.
- **Quelle:** https://rpnfan.github.io/keyboard-heaven/deep-dive/windows-keyboard-chain/ (extern)

### H15. `Win+G`/`Win+Shift+S` nicht per `RegisterHotKey` übernehmbar (OS-reserviert)
- **Symptom:** `RegisterHotKey` mit `MOD_WIN` für `Win+G`/`Win+Shift+S` schlägt fehl / die App bekommt nie WM_HOTKEY; Registry-Tricks deaktivieren Win+G nicht zuverlässig.
- **Ursache:** WIN-Kombinationen sind systemweit für das OS reserviert und werden mit höherer Priorität abgefangen, als eine App sie überschreiben kann.
- **Versionen:** Win10/11 (by design).
- **FIX:** Keine `Win+…`-Kombis für eigene Hotkeys; auf `Ctrl/Alt/Shift` ausweichen. Im Belegen-Dialog reservierte WIN-Kombis aktiv verbieten.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/3936433/ (extern)

### H16. NHotkey (WPF): Laufzeit-Änderung/Entfernung von KeyBindings nicht erkannt
- **Symptom:** Taste/Modifier eines `KeyBinding` zur Laufzeit geändert oder `KeyBinding` entfernt → alter Hotkey bleibt global registriert / Änderung greift nicht.
- **Ursache:** Der `HotkeyManager` reagiert nur auf das Attached-Property `RegisterGlobalHotkey`, nicht auf Collection-Entfernung oder Key/Modifier-Änderung.
- **Versionen:** NHotkey 2.x–4.x (dokumentiert).
- **FIX:** Ändern: `SetRegisterGlobalHotkey(binding, false)` → Key/Modifier setzen → wieder `true`. Entfernen: ZUERST `false`, DANN entfernen. Rein programmatisch `HotkeyManager.Current.AddOrReplace(...)`/`Remove(...)`.
- **Quelle:** https://github.com/thomaslevesque/NHotkey (extern)

---

## D) DPI / Multi-Monitor

### D1. Overlay unscharf / falsch dimensioniert auf Nicht-Primärmonitor ⭐ HAEUFIG
- **Symptom:** Text/Grafik verwaschen, Fenster falsch groß, sobald es auf einem Monitor mit anderer Skalierung als der Primärmonitor liegt.
- **Ursache:** WPF-Default ist System-Aware → Windows bitmap-stretcht bei abweichendem DPI.
- **Versionen:** alle WPF (auch .NET 10).
- **FIX:** `app.manifest` mit `<dpiAwareness>PerMonitorV2,PerMonitor</dpiAwareness>` + `<dpiAware>true</dpiAware>`, im csproj als `ApplicationManifest`.
- **Quelle:** https://github.com/dotnet/wpf/issues/859 (extern, WPF-Team) · https://learn.microsoft.com/en-us/windows/win32/hidpi/setting-the-default-dpi-awareness-for-a-process

### D2. Fenster erscheint auf falschem Monitor trotz gesetztem `Left`/`Top`
- **Symptom:** `Left/Top` gesetzt, Fenster landet trotzdem zentriert auf dem Primärmonitor.
- **Ursache:** `WindowStartupLocation` steht auf `CenterScreen`/`CenterOwner` → überschreibt manuelle Koordinaten.
- **Versionen:** alle WPF.
- **FIX:** `WindowStartupLocation = Manual` **vor** dem Anzeigen; bei Bedarf erst nach `Loaded` positionieren (wenn `ActualWidth/Height` gebraucht).
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/hidpi/high-dpi-desktop-application-development-on-windows

### D3. Position um Skalierungsfaktor daneben (z.B. 150 %)
- **Symptom:** Fenster systematisch verschoben, je weiter vom Primärmonitor desto stärker.
- **Ursache:** `Screen.Bounds`/Win32-`RECT` (physische Pixel) direkt als WPF-`Left`/`Top` (DIPs) verwendet.
- **Versionen:** alle WPF.
- **FIX:** Pixel → DIPs via `pixel / dpi.DpiScaleX` oder `HwndSource.CompositionTarget.TransformFromDevice`. Nie Pixel direkt als DIP zuweisen.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.windows.dpiscale

### D4. Rekursive DPI-Wechsel-Schleife beim Ziehen über die Monitorgrenze
- **Symptom:** Beim Drag zwischen Monitoren flackert/springt das Fenster, mehrfache `DpiChanged`-Events.
- **Ursache:** Im DPI-Wechsel die vorgeschlagene Zielgröße ignorieren / Fenster eigenmächtig sizen → löst erneuten Change aus.
- **Versionen:** alle WPF.
- **FIX:** `base.OnDpiChanged(...)` aufrufen und WPF skalieren lassen; eigene Positionslogik nur additiv; Spezialfälle `WM_GETDPISCALEDSIZE`.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/hidpi/high-dpi-desktop-application-development-on-windows

### D5. `GetDpiForMonitor` inkonsistent im PMv2-Thread
- **Symptom:** DPI-Abfrage stimmt nicht mit dem Fenster überein.
- **Ursache:** `GetDpiForMonitor` ist nicht DPI-aware und an die Process-Awareness gekoppelt; in einem Per-Monitor-Thread ungeeignet.
- **Versionen:** alle.
- **FIX:** `GetDpiForWindow(hwnd)` bzw. WPF `VisualTreeHelper.GetDpi(this)` — liefern den DPI des Top-Level-Fensters.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/shellscalingapi/nf-shellscalingapi-getdpiformonitor

### D6. Forced reset der Awareness bei gemischten HWND-Bäumen (z.B. WebView2-Host)
- **Symptom:** Die ganze App wird plötzlich wieder System-Aware/unscharf, nachdem ein Child-/Fremd-HWND erzeugt wurde.
- **Ursache:** Win10 1703+ erlaubt keine unterschiedlichen Awareness-Modi im selben HWND-Baum.
- **Versionen:** Windows 10 1703+.
- **FIX:** Alle HWNDs auf denselben Modus halten; für bewusst getrennte Sekundärfenster `SetThreadDpiAwarenessContext` **vor** der Window-Erstellung setzen und danach zurücksetzen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/hidpi/high-dpi-improvements-for-desktop-applications

### D7. `Screen.Bounds` liefert virtualisierte (96-DPI-)Werte
- **Symptom:** Monitorgrößen kommen „zu klein"/auf 96 DPI normiert zurück, Positionsrechnung daneben.
- **Ursache:** Aufruf aus System-Aware-/Unaware-Kontext → Windows virtualisiert die Rückgaben.
- **Versionen:** alle.
- **FIX:** Prozess PMv2 sicherstellen (D1); beim temporären `SetThreadDpiAwarenessContext` den alten Kontext zuverlässig wiederherstellen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/hidpi/high-dpi-desktop-application-development-on-windows

### D8. `<ApplicationHighDpiMode>` (csproj) wirkt bei WPF nicht
- **Symptom:** csproj-DPI-Schalter gesetzt, WPF bleibt System-Aware/unscharf.
- **Ursache:** `<ApplicationHighDpiMode>` ist **WinForms-only** (steuert `HighDpiMode`-Enum); WPF konfiguriert Awareness ausschließlich über `app.manifest`.
- **Versionen:** alle WPF.
- **FIX:** Nur `app.manifest` verwenden (D1); kein csproj-Schalter erwarten.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/hidpi/setting-the-default-dpi-awareness-for-a-process

### D9. `Popup` ignoriert `PlacementMode` → falsche Anfangs-DPI
- **Symptom:** Ein `Popup` mit `PlacementMode.MousePoint` wird an der Maus gezeichnet, behält aber die DPI des Monitors, auf dem die obere-linke Ecke des `PlacementTarget` liegt → bei 175%/100%-Mix falsch skaliert.
- **Ursache:** WPF wählt den Monitor zur DPI-Bestimmung anhand von Top/Left des `PlacementTarget` statt des realen `PlacementOrigin` (Fundstelle `Popup.cs` ~Z. 3573).
- **Versionen:** .NET 6, Win10 22H2 (#8091 offen).
- **FIX:** Popup auf dem Monitor erzeugen, der den realen PlacementOrigin enthält; Workaround: `PlacementTarget` aufs Eltern-Control mit korrekter DPI setzen.
- **Quelle:** https://github.com/dotnet/wpf/issues/8091 (extern)

### D10. ContextMenu: erste MenuItem-Ebene beim programmatischen Öffnen unskaliert
- **Symptom:** PerMonitorV2, 100%→150%: beim Öffnen per Code (`cm.IsOpen=true`) bleibt die erste MenuItem-Liste unskaliert; Untermenüs korrekt; Rechtsklick funktioniert.
- **Ursache:** Beim programmatischen Öffnen fehlt der `PlacementTarget`/DPI-Kontext fürs Wurzel-Popup.
- **Versionen:** .NET FW 4.8 – .NET 8, Win10 (Win11 teils gefixt) (#3874).
- **FIX:** `cm.PlacementTarget = elternControl;` vor `IsOpen = true`.
- **Quelle:** https://github.com/dotnet/wpf/issues/3874 (extern)

### D11. `WindowStyle="ToolWindow"` reagiert unter Win11 nicht auf DPI-Änderung
- **Symptom:** PerMonitorV2-App: ein ToolWindow skaliert als einziges Fenster nicht mit, wenn die Monitor-DPI zur Laufzeit geändert wird. Win10 korrekt.
- **Ursache:** Vermutlich `WS_EX_TOOLWINDOW`-bedingt — das Fenster verarbeitet `WM_DPICHANGED` auf Win11 nicht wie ein normales Fenster.
- **Versionen:** .NET 6/8, Win11 (#10422 offen).
- **FIX:** Workaround: Fenster einmal manuell resizen (erzwingt Re-Scaling); wenn ToolWindow-Optik nicht zwingend, `WindowStyle` wechseln.
- **Quelle:** https://github.com/dotnet/wpf/issues/10422 (extern)

### D12. Falsche Touch-/Stylus-Koordinaten auf Zweitmonitor (WM_Pointer)
- **Symptom:** Mit aktiviertem Pointer-Stack liefern `GetStylusPoint`/`GetTouchPoint` auf einem Sekundär-Touchscreen versetzte Koordinaten; InkCanvas zeichnet falsch.
- **Ursache:** `HwndPointerInputProvider.GetOriginOffsetsLogical` nutzt `RootVisual.PointToScreen((0,0))` direkt — korrekt nur bei einem Bildschirm; bei Multi-Monitor fehlt der DisplayRect-Offset.
- **Versionen:** alle .NET mit WM_Pointer (#8517, PR #11051 offen).
- **FIX:** Bis zum Fix Pointer-Support (`Switch.System.Windows.Input.Stylus.EnablePointerSupport`) auf Multi-Monitor-Touch nicht aktivieren; sonst Maus/Touch-Koordinaten selbst gegen den Zielmonitor-Offset korrigieren.
- **Quelle:** https://github.com/dotnet/wpf/issues/8517 (extern)

### D13. `MessageBox` unscharf trotz PerMonitorV2
- **Symptom:** `MessageBox.Show` auf einem 150%-Sekundärmonitor erscheint korrekt groß, aber unscharf (mit Primärmonitor-Skalierung gerendert und hochgestreckt).
- **Ursache:** `MessageBox` ist die native Win32-Funktion und wird mit der DPI-Awareness des Primärmonitor-Threads erzeugt.
- **Versionen:** .NET FW 4.8 / .NET 7, Win10/11 (#6775).
- **FIX:** Vor dem Aufruf Thread-DPI auf GDI-Scaling umschalten: `var p=SetThreadDpiAwarenessContext(-5 /*UNAWARE_GDISCALED*/); MessageBox.Show(...); SetThreadDpiAwarenessContext(p);` (als IDisposable-Wrapper). Besser: `TaskDialog`.
- **Quelle:** https://github.com/dotnet/wpf/issues/6775 (extern)

### D14. Fensterrahmen/Titelleiste rendern fehlerhaft bei gemischter Skalierung
- **Symptom:** Multi-Monitor mit 150%+100%: beim Verschieben rendern Rahmen/Titelleiste fehlerhaft (verzerrte Borders, graue Linie unter dem Schließen-Button).
- **Ursache:** PerMonitorV2-Chrome-Rendering bei nicht-ganzzahligem Skalierungswechsel berechnet die Non-Client-Bereiche nicht sauber neu.
- **Versionen:** .NET FW 4.7.2/4.8+, Win10/11 (#6314 offen).
- **FIX:** Kein sauberer Framework-Fix; pragmatisch Custom-Chrome (WindowChrome) DPI-sauber selbst zeichnen (`PerMonitor` statt V2 behebt es, macht das Fenster aber zu groß — schlechter Trade-off).
- **Quelle:** https://github.com/dotnet/wpf/issues/6314 (extern)

### D15. `WindowStartupLocation=CenterScreen` versagt auf Zweitmonitor (extremer Mix)
- **Symptom:** Primär 300%, Sekundär 100%: ein neues `CenterScreen`-Fenster, geöffnet während das MainWindow am Sekundärmonitor liegt, erscheint außerhalb des Bildschirms.
- **Ursache:** WPF rechnet die Zentrierung mit dem Primärmonitor-Faktor (300%) und wendet ihn am 100%-Monitor an → massiver Offset (Zirkularität Position↔DPI).
- **Versionen:** .NET 6 / .NET FW 4.8, Win11 (#6103, verwandt #4127/#3343/#3105).
- **FIX:** Zentrierung am tatsächlichen Zielmonitor selbst rechnen (Working-Area des Zielmonitors → Left/Top in DIPs); PerMonitorV2 NICHT abschalten.
- **Quelle:** https://github.com/dotnet/wpf/issues/6103 (extern)

### D16. .NET-7-Regression: Fensterinhalt unscharf bei hoher Skalierung (gefixt)
- **Symptom:** Nach Umstellung auf .NET 7 (Preview 4) rendert der ganze Fensterinhalt unscharf, deutlich bei 250%; .NET FW 4.8 nicht betroffen.
- **Ursache:** Bug in der High-DPI-Initialisierung (.NET 7 Preview 4, PR #5765) — Prozess zu spät DPI-aware.
- **Versionen:** Regression .NET 7.0 Preview 4; **gefixt** ab folgendem Preview (PR #6245); RTM nicht betroffen. (Verwandt: ClearType-DWrite #2025 gefixt via Servicing-Port.)
- **FIX:** Auf aktuelle .NET-Version updaten; Notlösung: `SetProcessDPIAware()` vor dem ersten Fenster.
- **Quelle:** https://github.com/dotnet/wpf/issues/6586 (extern)

---

## T) Tray / Autostart / Single-Instance

### T1. Tray-Icon verschwindet nach Explorer-Neustart ⭐ HAEUFIG
- **Symptom:** App läuft weiter, Icon ist nach explorer.exe-Crash/Neustart weg.
- **Ursache:** Die Shell registriert alle Tray-Icons neu; die App muss auf die Broadcast-Message `"TaskbarCreated"` reagieren und ihr Icon per `NIM_ADD` neu hinzufügen.
- **Versionen:** alle.
- **FIX:** `H.NotifyIcon`/Hardcodet/`WinForms.NotifyIcon` tun das **automatisch** → diese Libs nutzen. Bei eigenem P/Invoke: `RegisterWindowMessage("TaskbarCreated")` in der WndProc behandeln.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/shell/notification-area

### T2. Unscharfes/zu kleines Tray-Icon bei High-DPI
- **Symptom:** Icon pixelig/falsch skaliert auf 4K/HiDPI.
- **Ursache:** Nur eine 16×16-Auflösung in der `.ico`; System skaliert hoch.
- **Versionen:** alle.
- **FIX:** `.ico` mit mehreren Größen (16/20/24/32/48/256); App als PerMonitorV2-aware (D1).
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/shellapi/ns-shellapi-notifyicondataa

### T3. GUID-Tray-Icon weg nach Update/Pfadwechsel
- **Symptom:** Nach App-Update oder Verschieben der `.exe` erscheint das Icon nicht mehr.
- **Ursache:** Bei `NIF_GUID` verknüpft Windows die GUID mit dem Dateipfad; Pfadänderung invalidiert die Registrierung.
- **Versionen:** alle.
- **FIX:** Binary Authenticode-signieren (GUID übersteht Move) ODER GUID ändern ODER `hWnd`+`uID` statt GUID nutzen.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/shellapi/ns-shellapi-notifyicondataa

### T4. Autostart startet nicht / falsche .exe bei Pfad mit Leerzeichen ⭐ HAEUFIG
- **Symptom:** Run-Key gesetzt, App startet beim Login nicht oder ein falsches Programm.
- **Ursache:** Pfad ohne Quotes (`C:\Program Files\My App\app.exe …`) → Windows interpretiert `C:\Program` als Executable.
- **Versionen:** alle.
- **FIX:** Executable-Pfad **immer in Anführungszeichen**; Pfad via `Environment.ProcessPath` (nicht `Assembly.Location` — bei single-file leer).
- **Quelle:** https://learn.microsoft.com/en-us/uwp/api/windows.applicationmodel.startuptask (Kontext) — Run-Key-Verhalten

### T5. Packaged App: Registry-Autostart wird ignoriert/virtualisiert
- **Symptom:** MSIX-App schreibt `HKCU\…\Run`, Eintrag wird ignoriert.
- **Ursache:** Packaged Apps laufen mit Registry-Virtualisierung.
- **Versionen:** MSIX/Packaged.
- **FIX:** In Packaged Apps **ausschließlich** `windows.startupTask`-Manifest-Extension + `StartupTask.RequestEnableAsync()`.
- **Quelle:** https://learn.microsoft.com/en-us/uwp/api/windows.applicationmodel.startuptask

### T6. App re-aktiviert vom User deaktivierten Autostart
- **Symptom:** User deaktiviert Autostart im Task-Manager, App schaltet ihn wieder ein → malware-artig, ggf. Store-Ablehnung.
- **Ursache:** App ignoriert den `DisabledByUser`-Zustand.
- **Versionen:** alle.
- **FIX:** Bei `StartupTask` ist `RequestEnableAsync()` nach `DisabledByUser` per Design wirkungslos → respektieren. Bei Registry: vor erneutem Setzen prüfen, ob der User den Eintrag entfernt hat, und das nicht überschreiben.
- **Quelle:** https://learn.microsoft.com/en-us/uwp/api/windows.applicationmodel.startuptask

### T7. Mutex überlebt App-Crash → `AbandonedMutexException` ⭐ HAEUFIG
- **Symptom:** Nach hartem Crash startet die App nicht mehr / wirft `AbandonedMutexException`.
- **Ursache:** `WaitOne`-basierte Single-Instance stolpert über den verwaisten Mutex.
- **Versionen:** alle .NET.
- **FIX:** `out bool createdNew`-Muster nutzen (OS gibt den Mutex bei Prozess-Ende frei). Falls `WaitOne`: `AbandonedMutexException` fangen und als „darf starten" behandeln.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.threading.mutex

### T8. Single-Instance greift nicht über Sessions / oder zu global (`Global\` vs. `Local\`)
- **Symptom:** Zwei User können die App nicht parallel starten — oder sollen es nicht, tun es aber doch.
- **Ursache:** Mutex-Namensraum-Präfix falsch.
- **Versionen:** alle.
- **FIX:** Bewusst wählen: pro-User-Tray-App → `Local\…`; systemweite Singleton → `Global\…` (dann ACL für Nicht-Admins setzen).
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.threading.mutex

### T9. „Nach vorne holen" der bestehenden Instanz tut nichts (Taskleiste blinkt nur)
- **Symptom:** Zweiter Start aktiviert das bestehende Fenster nicht, der Button blinkt nur.
- **Ursache:** `SetForegroundWindow` ist gesperrt, wenn der (sich beendende) neue Prozess nicht Vordergrund ist und der User mit etwas anderem arbeitet.
- **Versionen:** alle.
- **FIX:** Die **neue** Instanz ruft vor dem Beenden `AllowSetForegroundWindow(pidAlt)` auf; oder die **alte** Instanz aktiviert sich selbst (per Named Pipe/`WM_COPYDATA` getriggert). `Topmost=true;Topmost=false`-Trick gegen Z-Order-Hänger.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow

### T10. `Hardcodet.NotifyIcon.Wpf` inaktiv / kein NativeAOT
- **Symptom:** Verbreitete Tray-Lib ohne NativeAOT/Trimming-Support, seit 2024 ohne Releases.
- **Ursache:** Das Hardcodet-Projekt nennt sich selbst „inactive base project".
- **Versionen:** Hardcodet v2.0.1 (2024).
- **FIX:** Auf **`H.NotifyIcon.Wpf`** wechseln (gleiche `tb:TaskbarIcon`-API, gepflegt, NativeAOT/Trimming).
- **Quelle:** https://github.com/HavenDV/H.NotifyIcon (extern)

### T11. ContextMenu erscheint HINTER der Taskleiste + schließt nicht (Shell hat Fokus) ⭐ HAEUFIG
- **Symptom:** Hatte man vorher Startmenü/Suche/Lautstärke (Shell) fokussiert, erscheint das Tray-ContextMenu beim Rechtsklick hinter der Taskleiste und schließt nicht beim Klick daneben.
- **Ursache:** Ist `Shell_TrayWnd` Vordergrund, rendert Windows das Menü unter der Taskleiste; das eigene Message-Window wird nicht in den Vordergrund geholt → keine Deaktivierungs-Erkennung.
- **Versionen:** H.NotifyIcon alle, Win10/11 (#5).
- **FIX:** Vor dem Anzeigen den Win32-Foreground-Trick: `SetForegroundWindow(messageWindowHandle)` + nach dem Menü `PostMessage(hwnd, WM_NULL, 0, 0)` (klassischer MSDN-Trick). H.NotifyIcon/Hardcodet tun das intern — aktuelle Version nutzen.
- **Quelle:** https://github.com/HavenDV/H.NotifyIcon/issues/5 (extern)

### T12. Doppelklick löst ZUSÄTZLICH den Single-Click-Befehl aus (`NoLeftClickDelay`)
- **Symptom:** Mit `LeftClickCommand` + `DoubleClickCommand` + `NoLeftClickDelay="True"` feuert ein Doppelklick beide Befehle.
- **Ursache:** `NoLeftClickDelay` entfernt das Wartefenster, in dem der Single-Click zugunsten des Double-Click verworfen würde — beide Win32-Messages werden durchgereicht.
- **Versionen:** H.NotifyIcon, Win11 (#200 offen).
- **FIX:** Single-Click verzögert über einen Timer (`GetDoubleClickTime()`, ~500 ms); trifft ein zweiter Klick ein, Single-Timer abbrechen. Notlösung: im DoubleClick-Command ein Flag setzen, das den SingleClick kurz unterdrückt.
- **Quelle:** https://github.com/HavenDV/H.NotifyIcon/issues/200 (extern)

### T13. ToolTip unter Win11 (neuer XAML-Tray) nicht angezeigt + Paket-Verwechslung
- **Symptom:** Icon erscheint, aber kein ToolTip beim Hover (nur seit dem neuen Win11-XAML-Tray; Win10 ok).
- **Ursache:** (a) Reine `TrayIcon`-Klasse (`H.NotifyIcon`-Core) hat keinen ToolTip-Handler — nur der WPF-`TaskbarIcon` (`H.NotifyIcon.Wpf`). (b) Der neue Win11-XAML-Tray ändert das ToolTip-Handling (`NIF_TIP` greift nicht mehr wie zuvor).
- **Versionen:** H.NotifyIcon 2.0.50, Win11 (#25/hardcodet #65).
- **FIX:** Für WPF immer `H.NotifyIcon.Wpf` (TaskbarIcon) referenzieren, nicht das Core-Paket; ToolTip-Text bei jedem `NIM_MODIFY` mit `NIF_TIP` erneut übergeben.
- **Quelle:** https://github.com/HavenDV/H.NotifyIcon/issues/25 (extern)

### T14. Mittelklick feuert als Linksklick (Win11 22H2-Regression)
- **Symptom:** Mittelklick aufs Tray-Icon triggert den Linksklick statt eines Middle-Click-Events. Betrifft H.NotifyIcon, Hardcodet UND WinForms-NotifyIcon → Windows-Regression.
- **Ursache:** Windows liefert ab Build 22621.1344 `WM_MBUTTON*` für den Tray-Bereich nicht mehr wie zuvor.
- **Versionen:** Win11 22H2 (#82, MS-Root-Cause offen).
- **FIX:** Zusätzlich auf die Non-Client-Varianten `WM_NCMBUTTONUP`/`WM_NCMBUTTONDOWN` hören und aufs Middle-Click-Event mappen (Maintainer-Test-Build `4a5c913`).
- **Quelle:** https://github.com/HavenDV/H.NotifyIcon/issues/82 (extern)

### T15. `Shell_NotifyIcon` scheitert beim Start mit ERROR_TIMEOUT (Race mit Shell-Init)
- **Symptom:** Beim Autostart/Login (System unter Last) gibt `Shell_NotifyIcon(NIM_ADD)` `false` zurück (`ERROR_TIMEOUT`), Icon erscheint nicht.
- **Ursache:** `Shell_NotifyIcon` nutzt intern `SendMessageTimeout` zum Taskleisten-Fenster; beim Login pumpt der Explorer-Thread noch keine Messages → Timeout.
- **Versionen:** Win32-API-inhärent, jede Lib.
- **FIX:** Bei `false` Retry-Schleife mit Backoff (5–10×, ~250–500 ms); zusätzlich auf `TaskbarCreated` hören (T1). Nicht auf `GetLastError` für die Erfolgsentscheidung verlassen.
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/1316178/ (extern)

### T16. Doppelte Tray-Icons nach Resume aus Standby/Hibernate
- **Symptom:** Nach Aufwachen aus Sleep/Hibernate erscheinen zwei Icons derselben App; das Geister-Icon verschwindet oft erst beim Hover.
- **Ursache:** Beim Resume broadcastet die Shell ggf. erneut `TaskbarCreated`; Apps, die mit `NIM_ADD` reagieren ohne vorher `NIM_DELETE`, fügen ein zweites Icon hinzu.
- **Versionen:** plattformübergreifendes Muster (EarTrumpet #404).
- **FIX:** Vor jedem Re-Register bei `TaskbarCreated`/Resume defensiv `NIM_DELETE` für die bestehende GUID/ID, dann `NIM_ADD`; stabile `guidItem`; bei existierender GUID `NIM_MODIFY`.
- **Quelle:** https://github.com/File-New-Project/EarTrumpet/issues/404 (extern)

### T17. Win11: Balloon/Notification-Icon falsch nach Laufzeit-Icon-Wechsel
- **Symptom:** `NotifyIcon.Icon` zur Laufzeit geändert, dann `ShowBalloonTip` → das Notification-Center zeigt das alte/falsche Icon.
- **Ursache:** Win11 cacht das Icon beim ersten Registrieren für die Toast-Darstellung; ein späteres `NIM_MODIFY` wird für die Notification-Pipeline nicht übernommen.
- **Versionen:** Win11, .NET 6/7/8 (winforms #12373 offen).
- **FIX:** Icon-Handle im selben `NOTIFYICONDATA`-Aufruf mit `NIF_ICON` aktualisieren und unmittelbar vor `ShowBalloonTip` erneut `NIM_MODIFY`; oder Win11-Toast-API (`ToastNotification`) statt klassischer Balloons.
- **Quelle:** https://github.com/dotnet/winforms/issues/12373 (extern)

### T18. Named-Pipe-Single-Instance in `OnStartup` → harte Windows-Crashes
- **Symptom:** Wird der Pipe-Listener im `OnStartup` gestartet, kommt es häufig zu harten Prozessabbrüchen (nicht nur Exceptions).
- **Ursache:** Race zwischen Dispatcher-Init und Pipe-Server-Start; der Pipe-Callback feuert, bevor der UI-Thread bereit ist.
- **Versionen:** alle WPF.
- **FIX:** Pipe-Server erst nach `MainWindow.Loaded`/erstem Dispatcher-Idle starten; eingehende Args über `Dispatcher.BeginInvoke` marshallen, nie direkt im Pipe-Thread UI anfassen.
- **Quelle:** https://weblog.west-wind.com/posts/2016/May/13/Creating-Single-Instance-WPF-Applications-that-open-multiple-Files (extern)

### T19. MSIX/ClickOnce: Mutex-/Pipe-Namen pro Paket-Identität isoliert
- **Symptom:** Single-Instance funktioniert als Win32-Exe, aber als MSIX-Paket verhindert der Mutex die Zweitinstanz nicht; bei mehreren installierten Versionen kollidieren Instanzen falsch.
- **Ursache:** MSIX läuft in einer Identitäts-Sandbox; `Local\`-Objekte sind paket-/sessionisoliert → derselbe Name trifft nicht denselben Mutex. ClickOnce nutzt pro Version andere Assembly-Namen.
- **Versionen:** MSIX/ClickOnce, .NET 6–10.
- **FIX:** Bei MSIX `Global\`-Mutex mit aus der **Package Family Name** abgeleitetem stabilem Namen; robuster: Windows-App-SDK-Single-Instance-Redirection (`AppInstance`).
- **Quelle:** https://learn.microsoft.com/en-us/answers/questions/730364/ (extern)

### T20. Autostart: `StartupApproved\Run` deaktiviert den Run-Key still
- **Symptom:** Der Eintrag unter `…\CurrentVersion\Run` ist vorhanden, die App startet aber nicht mit Windows (im Task-Manager als deaktiviert).
- **Ursache:** Der Parallelschlüssel `…\Explorer\StartupApproved\Run` enthält ein „deaktiviert"-Byte-Flag (vom User/Cleanup/Defender gesetzt), das den Run-Eintrag überstimmt.
- **Versionen:** Win10/11.
- **FIX:** Vor dem Aktivieren prüfen, ob `StartupApproved\Run` den eigenen Namen als deaktiviert führt → Nutzer-Wunsch **respektieren**, nicht zwangsweise reaktivieren. Beim sauberen Entfernen beide Schlüssel bereinigen.
- **Quelle:** http://windowsir.blogspot.com/2022/07/startupapprovedrun-pt-ii.html (extern)

### T21. Autostart per Task Scheduler „whether logged on or not" → Fenster läuft unsichtbar
- **Symptom:** Per Aufgabenplanung gestartete App zeigt kein Fenster (läuft als unsichtbarer Hintergrundprozess) oder kommt ohne Fokus hoch.
- **Ursache:** „Run whether the user is logged on or not" läuft in einer nicht-interaktiven Session (Session 0) ohne sichtbaren Desktop; vom Scheduler gestartete Prozesse haben zudem kein Foreground-Recht.
- **Versionen:** Windows-Aufgabenplanung, alle WPF.
- **FIX:** Trigger auf **„Run only when user is logged on"** stellen (interaktive Session); für Elevation zusätzlich „Run with highest privileges". App holt sich selbst nach vorne (`ShowWindow(SW_SHOWNORMAL)`+`SetForegroundWindow`); `--autostart`-Argument mitgeben.
- **Quelle:** https://learn.microsoft.com/en-us/troubleshoot/windows-client/system-management-components/task-scheduler-task-only-runs-in-background (extern)

### T22. Autostart-`--autostart`-Fenster blitzt trotz „minimiert" kurz weiß auf
- **Symptom:** Trotz „versteckt/minimiert starten" blitzt das Fenster beim Start kurz weiß auf.
- **Ursache:** WPF zeigt das Fenster (Default-weißer Hintergrund) einen Frame lang, bevor `WindowState=Minimized`/`Hide()` greift.
- **Versionen:** alle WPF.
- **FIX:** `WindowState=Minimized`/`Visibility=Hidden` **vor** dem ersten `Show()` (im Konstruktor) setzen, im `--autostart`-Fall das Hauptfenster gar nicht zeigen; Window-`Background` auf Zielfarbe (nicht Default-Weiß); finalen `WindowState` erst in `ContentRendered`.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.windows.application.startup (extern)

---

## W) WindowChrome & DWM-Material (Mica/Acrylic/runde Ecken)

### W1. WindowChrome zieht den Nicht-Client-Frame zu klein → transparente Löcher beim Maximieren ⭐ HAEUFIG
- **Symptom:** Mit `<WindowChrome/>` sind die Frame-Maße zu klein; beim Maximieren hängt der Frame über den Monitorrand → transparente Löcher in der UI.
- **Ursache:** `WindowChrome` wurde seit Windows 10 nie ans neue Nicht-Client-Modell angepasst; die internen Maße sind falsch.
- **Versionen:** .NET Core 3.1 – .NET 9, .NET FW 4.8 (#3887 Sammel-Issue, offen).
- **FIX:** `NonClientFrameEdges="Bottom,Left,Right"`; den Titelleisten-Versatz NICHT mit Margins kaschieren, sondern `WM_GETMINMAXINFO` selbst behandeln (Maximize-Bounds auf `rcWork` des aktuellen Monitors, DPI-korrekt — siehe A20).
- **Quelle:** https://github.com/dotnet/wpf/issues/3887 (extern)

### W2. Caption-Button-Hover bleibt kleben (`WM_NCMOUSELEAVE` nicht behandelt)
- **Symptom:** Schnelles Überfahren der Min/Max/Close-Buttons im erweiterten Frame lässt den Hover-Highlight kleben.
- **Ursache:** WPF verarbeitet `WM_NCMOUSELEAVE` im WindowChrome-Hook nicht zuverlässig.
- **Versionen:** Win10/11, alle .NET (Teil von #3887).
- **FIX:** Eigener `HwndSourceHook`, `WM_NCMOUSELEAVE` (0x02A2) abfangen und Hover-Zustand der Caption-Buttons aktiv zurücksetzen; zusätzlich `WM_NCMOUSEMOVE` tracken.
- **Quelle:** https://github.com/dotnet/wpf/issues/3887 (extern)

### W3. `NonClientFrameEdges="Left"` + Aero-Caption-Buttons → Inhalt rutscht bei Akzentfarb-Wechsel nach links
- **Symptom:** Mit linkem Nicht-Client-Edge + `UseAeroCaptionButtons` verschiebt sich der Inhalt über 1–2 s um ~50px nach links, sobald die Windows-Akzentfarbe geändert wird.
- **Ursache:** Fehlerhafte Neuberechnung des Client-Offsets nach `WM_SETTINGCHANGE`/Theme-Event bei aktivem linkem Edge.
- **Versionen:** offen (#3887).
- **FIX:** `Left` in `NonClientFrameEdges` vermeiden (`Bottom,Left,Right` als Block oder ohne Aero-Caption-Buttons); bei `WM_SETTINGCHANGE` das WindowChrome neu zuweisen.
- **Quelle:** https://github.com/dotnet/wpf/issues/3887 (extern)

### W4. `GlassFrameThickness`: weiße Streifen (≠0) vs. runde Ecken bleiben im Snap (=0)
- **Symptom:** `GlassFrameThickness="1"` → weiße Streifen in Resize-Richtung; `="0"` → runde Ecken bleiben auch im gesnappten/halbierten Zustand (Desktop blitzt durch) + ruckartiges Resize.
- **Ursache:** Bei `=0` rendert WPF die runden Ecken selbst und respektiert `CornerRadius` im Snapped-State nicht; bei `>0` Composition-Lag.
- **Versionen:** Win11 21H2/22H2+, alle .NET (#7769; betrifft auch Chrome/Steam/Discord/Rider).
- **FIX:** `GlassFrameThickness="0"` + `CornerRadius="0"` UND die Rundung an den DWM delegieren: `DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE=33, DWMWCP_ROUND=2)` — DWM rundet nur im Normalzustand, snapped korrekt eckig.
- **Quelle:** https://github.com/dotnet/wpf/issues/7769 (extern)

### W5. 1px schwarze Linie oben / flackernder schwarzer Balken beim Resize
- **Symptom:** 1px schwarze Linie am oberen Rand; bei der DWM-Corner-Variante ein flackernder schwarzer Balken während des Resizes; teils sichtbare Fensterkante ~10px neben der Fläche.
- **Ursache:** Falsche Nicht-Client-Größe (W1) + WPF-eigenes Ecken-/Frame-Rendering; DWM zeichnet seinen Frame, bevor WPF den Client füllt.
- **Versionen:** Win11 22H2 (21H2 oft nicht) (#7769/#3887).
- **FIX:** `UseLayoutRounding="True"` + `SnapsToDevicePixels="True"` am Root; obersten 1px-Rand mit eigenem `Border` (DWM-Frame-Farbe) überdecken; Fenster-`Background` nicht-transparent halten.
- **Quelle:** https://github.com/dotnet/wpf/issues/7769 (extern)

### W6. WindowChrome flackert beim Resize (besonders linke/obere Kante)
- **Symptom:** Beim Ziehen der linken/oberen Kante flackert/zittert der Inhalt; rechte/untere Kante meist ruhig.
- **Ursache:** Liegt am DirectX-Compositor und der Reihenfolge `WM_NCCALCSIZE`/`WM_WINDOWPOSCHANGING` vs. Neuzeichnen, weniger an WindowChrome selbst.
- **Versionen:** seit ~WPF 4, offen (#1176).
- **FIX:** `NonClientFrameEdges="Right"` setzen (behebt das Flackern im Beispiel vollständig); ergänzend `UseLayoutRounding`.
- **Quelle:** https://github.com/dotnet/wpf/issues/1176 (extern)

### W7. Mica/Acrylic verschwindet bei Resize/Maximize/Restore und Monitorwechsel ⭐ HAEUFIG
- **Symptom:** Der DWM-Backdrop verschwindet (Bereich wird schwarz/leer), sobald das Fenster resized, maximiert/wiederhergestellt oder auf einen anderen Monitor gezogen wird.
- **Ursache:** Der Backdrop hängt an der DWM-Frame-Erweiterung + gesetztem `DWMWA_SYSTEMBACKDROP_TYPE`; bei Größen-/Monitorwechsel (DPI/Composition-Reset) wird der erweiterte Frame zurückgesetzt.
- **Versionen:** WPF + Drittlibs (MicaWPF/WPF-UI), Win11 (#8545).
- **FIX:** Backdrop-Setup zentralisieren und bei `WM_DPICHANGED`, `WM_SIZE`/`SizeChanged`, `WM_WINDOWPOSCHANGED` erneut `DwmExtendFrameIntoClientArea(margins{-1})` + `DwmSetWindowAttribute(DWMWA_SYSTEMBACKDROP_TYPE, …)` setzen; `Background=Transparent` halten.
- **Quelle:** https://github.com/dotnet/wpf/issues/8545 (extern)

### W8. DWM-Attribute (Backdrop/Ecken) nach `Hide()`/`Show()`-Zyklus weg
- **Symptom:** Nach `Hide()` + `Show()` (typisch Tray-/Autostart-minimiert) sind Eckenrundung/Backdrop verschwunden.
- **Ursache:** Beim Hide/Show-Zyklus (HWND-Recreate) gehen die per `DwmSetWindowAttribute` gesetzten Attribute verloren — sie kleben am HWND, nicht am `Window`-Objekt.
- **Versionen:** alle WPF mit DWM-Interop, Win11.
- **FIX:** HWND früh und stabil erzeugen (`EnsureHandle()` im Konstruktor), DWM-Attribute bei jedem `IsVisibleChanged`/`SourceInitialized` neu setzen; Tray-Sichtbarkeit über `WindowState`/`ShowInTaskbar` steuern statt HWND-Recreate.
- **Quelle:** https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-rounded-corners

### W9. `DWMWA_MICA_EFFECT` (1029) veraltet vs. `DWMWA_SYSTEMBACKDROP_TYPE` (38)
- **Symptom:** Mica über `DWMWA_MICA_EFFECT=1029` gesetzt greift nur auf alten Insider-Builds, auf aktuellem Win11 nicht.
- **Ursache:** `1029` war ein undokumentiertes Insider-Attribut, ab Build 22523 durch das öffentliche `DWMWA_SYSTEMBACKDROP_TYPE=38` ersetzt.
- **Versionen:** Win11 < 22523 vs. ≥ 22523.
- **FIX:** Build prüfen: ab 22523 `DWMWA_SYSTEMBACKDROP_TYPE` mit `DWMSBT_MAINWINDOW=2` (Mica)/`DWMSBT_TRANSIENTWINDOW=3` (Acrylic); davor Fallback aufs alte Attribut.
- **Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwm_systembackdrop_type

### W10. Backdrop greift nicht ohne erweiterten Frame / mit opakem Background; Mica folgt nur System-Theme
- **Symptom:** `DWMWA_SYSTEMBACKDROP_TYPE` gesetzt, aber das Fenster bleibt undurchsichtig; oder app-internes Hell/Dunkel wird ignoriert, Mica zeigt die System-Palette.
- **Ursache:** (a) DWM-Frame nicht in den Client erweitert (`DwmExtendFrameIntoClientArea` mit `-1`-Margins fehlt) oder opaker `Window.Background` übermalt den Backdrop. (b) Der DWM-Backdrop richtet sich nach dem System-Theme; die app-interne Theme-Wahl wird nicht in die Backdrop-Config synchronisiert.
- **Versionen:** Win11, alle WPF-DWM-Ansätze.
- **FIX:** Reihenfolge: Frame erweitern (`-1`-Margins) → Backdrop-Type setzen → `Window.Background=Transparent`. Bei app-internem Theme-Wechsel Backdrop-Tint/Theme selbst nachziehen + `SystemThemeWatcher`/`WM_SETTINGCHANGE`-Handler.
- **Quelle:** https://learn.microsoft.com/en-us/windows/apps/develop/ui/system-backdrops

---

## P) Deployment, Lifecycle & .NET-9/10-Regressionen

### P1. .NET-9-Fluent-Theme CRASHT bei System-Theme-/Akzentfarben-Wechsel ⭐ HAEUFIG (gefährlichster neuer Bug)
- **Symptom:** App mit eingebautem Fluent-Theme (`ThemeMode.System`) crasht, sobald der Nutzer Hell/Dunkel oder die Akzentfarbe ändert: `InvalidOperationException: The calling thread cannot access this object because a different thread owns it.` in `ThemeManager.OnSystemThemeChanged()` — später sogar still. Auf Win10 löst schon ein Spotlight-Hintergrundwechsel aus. Fatal für langlaufende Tray-/Overlay-Apps.
- **Ursache:** `ThemeManager.OnSystemThemeChanged` wird aus `SystemThemeFilterMessage` NICHT über den Dispatcher aufgerufen und greift cross-thread auf UI-Objekte zu; ignoriert auch `Application.Current==null` und eigene Dispatcher-Threads.
- **Versionen:** .NET 9 (Fluent experimentell, `WPF0001`), Win10/11 24H2; im Lauf von .NET-9-Servicing/.NET 10 dispatcher-marshalled gefixt.
- **FIX:** `ThemeMode` fest auf `Light`/`Dark` statt `System` (kein Crash). Wer System-Theme folgen will: `UserPreferenceChanged`/`WM_SETTINGCHANGE` selbst abfangen und Theme-Update strikt über `Dispatcher.Invoke` marshallen; Multi-Thread-Fenster meiden. Auf aktuelles .NET-9-Servicing/.NET 10 updaten.
- **Quelle:** https://github.com/dotnet/wpf/issues/9906 (extern)

### P2. .NET 10: Fluent + `ThemeMode="System"` → TextBox-Crash (`BorderThickness` UnsetValue)
- **Symptom:** Unter .NET 10 mit Fluent + `ThemeMode=System` laden TextBox-basierte Controls nicht; Crash `InvalidOperationException: '{DependencyProperty.UnsetValue}' is not a valid value for property 'BorderThickness'`. Trifft jedes Eingabe-Overlay.
- **Ursache:** Regression im Fluent-Style-Resolving für `BorderThickness` unter `ThemeMode=System`.
- **Versionen:** .NET 10 Preview 4; **gefixt** ab Preview 5/RTM.
- **FIX:** `ThemeMode` explizit `Light`/`Dark` setzen; auf .NET-10-RTM updaten.
- **Quelle:** https://github.com/dotnet/core/blob/main/release-notes/10.0/preview/preview4/wpf.md (extern)

### P3. Fluent + Windows-High-Contrast → `XamlParseException` (doppelter Ressourcenschlüssel)
- **Symptom:** Bei aktivem Fluent + High-Contrast crasht die App: `XamlParseException: Item has already been added. Key in dictionary: 'SystemColorWindowColorBrush'`.
- **Ursache:** Doppelte Registrierung desselben System-Brush-Schlüssels im zusammengeführten Fluent-ResourceDictionary unter High-Contrast.
- **Versionen:** .NET 9 Fluent (#10043).
- **FIX:** Fluent-Aktivierung in try/catch; bei aktivem High-Contrast aufs klassische Theme zurückfallen; Servicing-Update.
- **Quelle:** https://github.com/dotnet/wpf/issues/10043 (extern)

### P4. `ThemeMode`-Zugriff im Code → Compile-Fehler WPF0001 (experimentell)
- **Symptom:** Setzen von `Application.ThemeMode`/`Window.ThemeMode` im Code erzeugt Compiler-Fehler **WPF0001**.
- **Ursache:** Die API ist in .NET 9 als experimentell markiert (`[Experimental("WPF0001")]`).
- **Versionen:** .NET 9 (experimentell), .NET 10 in Stabilisierung.
- **FIX:** Lokal `#pragma warning disable WPF0001` um die Stelle (nicht projektweit); alternativ Theme über `ResourceDictionary`-Austausch in `App.xaml`.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/desktop/wpf/whats-new/net90

### P5. .NET 9: BinaryFormatter entfernt → Clipboard/Drag-Drop mit Custom-Typen wirft `PlatformNotSupportedException` ⭐ HAEUFIG
- **Symptom:** Ab .NET 9 wirft `Clipboard.SetDataObject`/`DataObject` mit eigenen, nicht-intrinsischen Typen `PlatformNotSupportedException` — ein Overlay, das ein eigenes Selection-Objekt in die Zwischenablage legt oder Drag-Drop unterstützt, bricht hart ab.
- **Ursache:** `BinaryFormatter` wurde in .NET 9 vollständig entfernt; WPF nutzt intern nur noch ein Subset für bekannte Typen (Text/Bild/FileDrop).
- **Versionen:** .NET 9.0+ (alle), .NET 10. **Nicht** .NET 8.
- **FIX:** Custom-Daten selbst serialisieren — als `string` (JSON) oder `byte[]` ablegen, beim Auslesen deserialisieren (MS-Migration-Guide empfiehlt JSON).
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/standard/serialization/binaryformatter-migration-guide/wpf-applications

### P6. .NET 9: abgeleitete `DataObject`-Klasse → Clipboard bleibt still leer
- **Symptom:** `Clipboard.SetDataObject(new MyDataObject())` mit einer von `DataObject` abgeleiteten Klasse legt nichts ab; `GetDataPresent(Format)` liefert `false`. Kein Crash, stiller Datenverlust.
- **Ursache:** Regression durch die BinaryFormatter-Umstellung im Pfad mit überschriebenem `GetFormats()`.
- **Versionen:** .NET 9.0.100-rc.2 + RTM-Linie (#10049). Funktioniert in .NET 8.
- **FIX:** Nicht von `DataObject` ableiten — ein `DataObject` instanziieren und Formate per `SetData(format, byte[]/string)` explizit setzen.
- **Quelle:** https://github.com/dotnet/wpf/issues/10049 (extern)

### P7. Trimming/NativeAOT mit WPF nicht unterstützt → XAML-Reflection bricht
- **Symptom:** `PublishTrimmed=true`/NativeAOT bei einer WPF-Overlay-App → startet nicht (XAML-Reflection findet Typen nicht; Tray-Icon-/Style-Ressourcen verschwinden).
- **Ursache:** WPF nutzt massiv Laufzeit-Reflection; Trimming-Support ist im SDK absichtlich deaktiviert, NativeAOT für WPF nicht unterstützt.
- **Versionen:** alle .NET (6–10).
- **FIX:** Trimming/NativeAOT für WPF nicht verwenden; für kleine Deployments `PublishSingleFile` (ohne Trim) + ggf. ReadyToRun.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/core/deploying/trimming/incompatibilities

### P8. Self-contained Single-File / ARM64 / Manifest: native WPF-DLL- und DPI-Fallen
- **Symptom:** (a) `PublishSingleFile` + `IncludeNativeLibrariesInSingleFile` → `PresentationNative_cor3.dll` lädt nicht aus TEMP, App crasht beim Start. (b) win-arm64 self-contained → `D3DCompiler_47.dll`-Stub bricht 3rd-Party-Libs (z.B. CefSharp). (c) `app.manifest` (PerMonitorV2) wird beim Cross-Targeting/Single-File nicht eingebettet → App ist nur „System Aware" (unscharf).
- **Ursache:** WPFs DLL-Konsistenzprüfung lehnt native Libs aus TEMP ab; ARM64-Runtime liefert einen Export-losen D3DCompiler-Stub; fehlendes `<ApplicationManifest>` bzw. SDK-Cross-Targeting-Bug bettet den Manifest nicht ein.
- **Versionen:** .NET 5–10, Publish-abhängig.
- **FIX:** `IncludeNativeLibrariesInSingleFile` NICHT setzen (native WPF-DLLs als Begleitdateien); ARM64-D3DCompiler-Stub nach Publish entfernen; `<ApplicationManifest>app.manifest</ApplicationManifest>` explizit setzen und DPI-Awareness als Fallback per `SetProcessDpiAwarenessContext(PER_MONITOR_AWARE_V2)` früh in `Main` absichern.
- **Quelle:** https://github.com/dotnet/runtime/issues/38636 · https://github.com/dotnet/wpf/issues/5462 · https://github.com/dotnet/wpf/issues/7609 (extern)

### P9. Tray-only-Lifecycle: sofortiger Exit (`ShutdownMode`) + Timer-/Binding-Leaks bei Dauerläufern ⭐ HAEUFIG
- **Symptom:** (a) Reine Tray-/Overlay-App ohne sichtbares Fenster beendet sich sofort. (b) Über Stunden/Tage wachsender RAM/CPU: `DispatcherTimer`/EventHandler halten geschlossene Fenster am Leben (#9331/#3384), `ItemsControl`-Bindings leaken (`WeakEventManager`/`ConditionalWeakTable`, #5739/#6806).
- **Ursache:** `ShutdownMode`-Default `OnLastWindowClose` beendet die App ohne offenes Fenster; Dispatcher hält starke Referenzen auf Timer→Handler→Fenster; Collection-/Transform-Bindings leaken.
- **Versionen:** alle WPF; besonders kritisch für net10.0-windows-Dauerläufer.
- **FIX:** `ShutdownMode="OnExplicitShutdown"` + nur per `Application.Shutdown()` (Tray-„Beenden") beenden. Overlay-Fenster wiederverwenden (`Hide()`/`Show()` statt `new`+`Close()`); beim Verstecken `timer.Stop()` + `Tick -=`, ItemsSource abkoppeln, Bindings lösen; einen langlebigen App-Timer statt pro Fenster.
- **Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.windows.application.shutdownmode · https://github.com/dotnet/wpf/issues/9331 (extern)

---

## ✅ Fix-Status (was ist in neueren Versionen schon behoben?)

> Belege aus github.com/dotnet/wpf-Issues/PRs + offiziellen .NET-Release-Notes. Ehrlichkeit:
> streng getrennt nach *belegt gefixt* vs. *Workaround bleibt aktiv*.

**Belegt gefixt (Versions-Anker):**

| Früherer Bug | Gefixt ab | Beleg |
|--------------|-----------|-------|
| D16 .NET-7-DPI-Unschärfe-Regression | .NET 7 (Preview nach P4, PR #6245) | dotnet/wpf #6586 |
| D (ClearType-DWrite-Modus, #2025) | Servicing-Port aus .NET FW 4.7–4.8 | dotnet/wpf #2025 |
| P2 .NET-10 Fluent TextBox-Crash | .NET 10 **Preview 5** | core/release-notes 10.0 preview4 |
| P1 .NET-9-Fluent Theme-Wechsel-Crash | im Lauf von .NET-9-Servicing / .NET 10 (dispatcher-marshalled) | dotnet/wpf #9906 |
| C11 PrintWindow/IntelliPoint-Race | WPF 3.5 SP1 / in .NET (Core) enthalten | MS-Blog Dwayne Need |
| T5/T15 Tray-Icon nach Explorer-Neustart/DPI-Cache | hardcodet 1.0.9 (Commit de6a2b2); Lib behandelt TaskbarCreated | hardcodet #15 |

**Noch NICHT gefixt — Workaround bleibt aktiv:**

- **Win32/DWM-Architektur (kein WPF-Fix):** A1 (Win11-24H2-Z-Order), A12 (Topmost-vs-Topmost), A17 (Secure Desktop), A19 (rude-window), A5/A18 (SetForegroundWindow/Loop), W1–W6 (WindowChrome-Maße — #3887/#7769/#1176 offen), W7–W10 (DWM-Backdrop-Lebenszyklus).
- **By design / Plattform-Vertrag:** A3 (NOACTIVATE↔Tab), C1/C12 (Layered nötig fürs Durchklicken), C3 (Touch), H7/H13 (UIPI/Anti-Cheat), H15 (Win-Kombis), D8/P7 (csproj-DPI WinForms-only / WPF-Trimming).
- **Offen im Issue-Tracker (dotnet/wpf):** A20/#3766/#2242 (Maximize), C13/#7857, C14/#7724, C15/#8486, D9/#8091, D10/#3874, D11/#10422, D12/#8517 (PR #11051), D13/#6775, D14/#6314, D15/#6103, P6/#10049, T-Tray-Issues (#5/#200/#82/#12373).
- **Experimentell (kann sich ändern):** P4 (`ThemeMode`/WPF0001).

> Methodik-Hinweis: Fix-Versionen aus den dotnet/wpf-Release-Notes/PRs verifiziert. Wo nur ein
> Issue-Snippet vorlag, bleibt der Status bewusst „offen" statt „gefixt".

---

## 📋 Pflicht-Checkliste (vor Overlay-Arbeit abhaken)

- [ ] Always-on-top: `Topmost=true` + `ShowActivated=false` + `WS_EX_NOACTIVATE|WS_EX_TOOLWINDOW`; **nie** `SetForegroundWindow` (A5)
- [ ] Topmost ereignisgetrieben re-asserten (`WM_WINDOWPOSCHANGED`), kein Timer-Spam (A1/A6/A18)
- [ ] Verstecken über `Hide()`/`Visibility.Hidden`/`Opacity=0`, **nie** `Visibility.Collapsed` (C15)
- [ ] Click-through: `WS_EX_TRANSPARENT` **+** `WS_EX_LAYERED`; `{x:Null}` statt `Transparent`; `SetWindowLongPtr` (C1/C2/C6/C12)
- [ ] Transparenz/runde Ecken über **DWM-Weg** (`AllowsTransparency=false` + `WindowChrome` + DWM); DWM-Attribute bei Resize/Hide-Show/DPI neu setzen (C16/W4/W7/W8)
- [ ] WindowChrome: `NonClientFrameEdges="Right"` (Flacker), `WM_GETMINMAXINFO`-Hook auf `rcWork` (Maximize) (W1/W6/A20)
- [ ] Hotkey: `RegisterHotKey` + `MOD_NOREPEAT` in `OnSourceInitialized`, Rückgabe prüfen, `UnregisterHotKey` beim Schließen (H1–H4)
- [ ] Push-to-Talk: `WH_KEYBOARD_LL`, Delegate **statisch**, Callback schlank; SharpHook mit explizitem RID + native Lib (H5/H6/H10/H11)
- [ ] `app.manifest` `PerMonitorV2,PerMonitor`; Pixel→DIP teilen; `WindowStartupLocation=Manual`; Manifest bei Single-File einbetten (D1/D3/P8)
- [ ] Tray: `H.NotifyIcon.Wpf` (nicht Core/Hardcodet); `Shell_NotifyIcon`-Retry; `NIM_DELETE` vor `NIM_ADD` bei Resume; Foreground-Trick fürs Menü (T11/T13/T15/T16)
- [ ] Autostart: gequoteter `Environment.ProcessPath`/`GetModuleFileNameW`; `StartupApproved\Run` respektieren; Task-Scheduler „logged on"; White-Flash via `ContentRendered` (T20/T21/T22)
- [ ] Single-Instance: Named `Mutex` (`createdNew`) + Pipe **nach** `Loaded`; MSIX → `Global\`+PackageFamilyName; `AllowSetForegroundWindow` fürs Nach-vorne-Holen (T7/T18/T19)
- [ ] .NET 9/10: `ThemeMode` fest `Light`/`Dark` (nicht `System`); Clipboard-Custom-Typen als JSON/`byte[]`; kein Trimming/AOT (P1/P2/P5/P7)
- [ ] Tray-only: `ShutdownMode=OnExplicitShutdown`; Timer/Bindings beim Verstecken lösen (Leak-Hygiene) (P9)

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-windows-overlay.md`) |
|---------------|---------------------------------------------------------------|
| A1–A20 (Always-on-top/Z-Order/Fokus) | §2 (Always-on-top), §10 (Z-Order & Aufmerksamkeit) |
| C1–C16 (Click-through/Transparenz/Layered) | §3 (Click-through), §5 (Transparenz & runde Ecken) |
| H1–H16 (Hotkeys) | §4 (Globale Hotkeys) |
| D1–D16 (DPI/Multi-Monitor) | §6 (Multi-Monitor & DPI) |
| T1–T22 (Tray/Autostart/Single-Instance) | §7 (Tray), §8 (Autostart), §9 (Single-Instance) |
| W1–W10 (WindowChrome & DWM-Material) | §5 (Transparenz & runde Ecken), §1 (Architektur) |
| P1–P9 (Deployment/Lifecycle/.NET-9-10) | §1 (Architektur), §7–§9 (Tray/Autostart/Single-Instance) |
