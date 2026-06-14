# Bekannte Bugs/Fallen: Windows-Overlay-Fenster (C#/WPF)

> PFLICHT-LESEN vor Arbeit an Franks Voice-Overlays **TVO** (TerminalVoiceOverlay-Windows) und
> **ClaudeVoiceOverlay-Windows** (immer-im-Vordergrund, globale Hotkeys, transparent).
> Stand: recherchiert am 2026-06-14 (7 Researcher parallel, Microsoft Learn zuerst).
> Versions-Anker: **.NET 10** (`net10.0-windows`), WPF, Windows 10/11. .NET 9/10 haben am
> Fenster-/Z-Order-/DPI-Modell nichts geändert — die Fallen leben in Win32/DWM.
> Diese Fallen sind beim **Best-Practices-Lauf** mitgefunden worden; eine noch tiefere, dedizierte
> Bug-Recherche (Issue-Tracker-Fokus + Fix-Status) kann später per `bug-almanach-recherche` ergänzt werden.
> Zweite Seite (wie macht man es richtig):
> [`best-practices/projekt-code/desktop/best-practices-windows-overlay.md`](../../best-practices/projekt-code/desktop/best-practices-windows-overlay.md).

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Sektionen: **A** Always-on-top/Z-Order/Fokus · **C** Click-through/Transparenz/Layered ·
> **H** Hotkeys · **D** DPI/Multi-Monitor · **T** Tray/Autostart/Single-Instance.

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

---

## 🔗 Bezug zu den Best-Practices (Kopplung)

| Bug-Abschnitt | Best-Practice-Abschnitt (`best-practices-windows-overlay.md`) |
|---------------|---------------------------------------------------------------|
| A1–A11 (Always-on-top/Z-Order/Fokus) | §2 (Always-on-top), §10 (Z-Order & Aufmerksamkeit) |
| C1–C11 (Click-through/Transparenz/Layered) | §3 (Click-through), §5 (Transparenz & runde Ecken) |
| H1–H9 (Hotkeys) | §4 (Globale Hotkeys) |
| D1–D8 (DPI/Multi-Monitor) | §6 (Multi-Monitor & DPI) |
| T1–T10 (Tray/Autostart/Single-Instance) | §7 (Tray), §8 (Autostart), §9 (Single-Instance) |
