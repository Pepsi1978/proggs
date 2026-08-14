# Windows-Overlay-Fenster (C#/WPF) — Best Practices (Stand 2026-06-14, .NET 10 / WPF, Windows 10/11)

> Für Franks Voice-Overlays **TVO** (TerminalVoiceOverlay-Windows) und **ClaudeVoiceOverlay-Windows**:
> immer-im-Vordergrund-Fenster mit globalen Hotkeys, transparent, ohne Fokus-Klau.
> Versions-Anker (live ermittelt): Overlay-Projekte zielen auf **`net10.0-windows`** (.NET 10, WPF),
> Windows 10/11. .NET 9/10 haben am Fenster-/Z-Order-/DPI-Modell **nichts** geändert — die Mechanik
> lebt unverändert in Win32 (`SetWindowPos`, `WS_EX_*`, DWM). Quellen: Microsoft Learn zuerst,
> Community als `extern` markiert.
> Zweite Seite (was schiefgeht): [`bugs/desktop/windows-overlay.md`](../../bugs/desktop/windows-overlay.md).

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
| 17 | Unsichtbare Fangflaeche ueber fremdem Fenster | NICHT per `SetParent` einhaengen (Alpha gilt nicht fuer Kind-HWNDs) — eigenstaendiges Top-Level darueberlegen und nachfuehren | §5 |
| 18 | „Unsichtbar", aber Klicks sollen ankommen | Alpha **1**/255, nicht 0 (bei 0 nimmt das Fenster keine Maustaste an); Farbschluessel taugt nicht — dort fallen Klicks durch | §5 |
| 19 | Sichtbare Linie ueber halbdurchsichtiger Flaeche | Nicht auf der Flaeche zeichnen (wird blass) — eigenes Farbschluessel-Fenster darueber | §5 |
| 20 | Pruefen, ob Durchsichtigkeit wirklich sitzt | `GetLayeredWindowAttributes` je HWND fragen; Bildschirmfoto beweist es NICHT | §5 |

---

## §1 Architektur & Grundsatz

Ein Voice-Overlay ist ein **randloses, transparentes, topmost** WPF-Fenster, das **Anzeige** ist
(Status, Pegel, Text) und sich **nie in den Vordergrund drängt**. Leitprinzip: **WPF-Properties für
90 %, P/Invoke nur für das, was WPF nicht kann** — `WS_EX_NOACTIVATE`, robuster Topmost-Refresh,
Click-through, DWM-Material, globale Hotkeys, AppBar, FlashWindowEx. Alle Interop-Styles werden in
**`OnSourceInitialized`** gesetzt (HWND existiert, Fenster noch nicht sichtbar) — nie im Konstruktor
(HWND = `IntPtr.Zero`) und nicht in `Loaded` (zu spät, Flackern). `WindowInteropHelper(this).EnsureHandle()`
erzwingt das HWND vorab, falls nötig.

Quelle: [WindowInteropHelper (.NET 10)](https://learn.microsoft.com/en-us/dotnet/api/system.windows.interop.windowinterophelper) · offiziell.

---

## §2 Always-on-top ohne Fokus-Klau

**Standard-Setup (XAML):**

```xml
<Window Topmost="True"
        ShowActivated="False"   <!-- nicht aktivieren beim Show; MUSS vor Show stehen -->
        ShowInTaskbar="False"
        WindowStyle="None"
        AllowsTransparency="False"  <!-- siehe §5: DWM-Weg bevorzugen -->
        Background="Transparent"
        Focusable="False"/>
```

**P/Invoke in `OnSourceInitialized`** — der Kern gegen Fokus-Klau:

```csharp
const int GWL_EXSTYLE = -20;
const int WS_EX_NOACTIVATE = 0x08000000;  // Fenster wird NIE aktiviert → klaut keinen Fokus
const int WS_EX_TOOLWINDOW = 0x00000080;  // raus aus Alt-Tab + Taskleiste
const int WS_EX_TOPMOST    = 0x00000008;
static readonly IntPtr HWND_TOPMOST = new(-1);
const uint SWP_NOMOVE = 0x0002, SWP_NOSIZE = 0x0001, SWP_NOACTIVATE = 0x0010;

protected override void OnSourceInitialized(EventArgs e) {
    base.OnSourceInitialized(e);
    var h = new WindowInteropHelper(this).Handle;
    int ex = GetWindowLong(h, GWL_EXSTYLE);
    SetWindowLong(h, GWL_EXSTYLE, ex | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW | WS_EX_TOPMOST);
}
// Refresh-Werkzeug (Win11-Bug + andere Topmost-Apps); SWP_NOACTIVATE ist PFLICHT:
void EnsureTopmost() {
    var h = new WindowInteropHelper(this).Handle;
    SetWindowPos(h, HWND_TOPMOST, 0,0,0,0, SWP_NOMOVE|SWP_NOSIZE|SWP_NOACTIVATE);
}
```

**Warum `SetWindowPos(HWND_TOPMOST, SWP_NOACTIVATE)` statt nur `Topmost=true`?** WPFs `Topmost` lässt
sich nur per `Topmost=false; Topmost=true`-Toggle (flackeranfällig) re-applien; `SetWindowPos` ist sauber,
idempotent und garantiert über `SWP_NOACTIVATE`, dass **nicht** aktiviert wird. Das offizielle MSDN-Beispiel
in der `SetForegroundWindow`-Doku zeigt exakt dieses Muster.

**`SetForegroundWindow` für ein Overlay NIE aufrufen** — das löst den Foreground-Lock aus; ein fremder
Prozess darf das Vordergrundfenster nur unter engen Bedingungen setzen, sonst **blinkt nur der
Taskleisten-Button**. Sichtbar/oben bleiben erreicht man ausschließlich über `HWND_TOPMOST` + `SWP_NOACTIVATE`.

Quellen: [SetWindowPos](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos) · [SetForegroundWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow) · [Extended Window Styles](https://learn.microsoft.com/en-us/windows/win32/winmsg/extended-window-styles) · alle offiziell.

---

## §3 Click-through / durchklickbares Overlay

**Mechanik:** `WS_EX_TRANSPARENT` (0x20) liefert Hit-Test-Transparenz (Klicks gehen ans Fenster darunter),
greift bei Top-Level-Fenstern aber **erst zuverlässig mit `WS_EX_LAYERED`** (0x80000). In WPF setzt
`AllowsTransparency="True"` das Layered-Bit automatisch. Das **Transparent-Bit** darf man zur Laufzeit
frei togglen, das **Layered-Bit nicht** (WPF legt den Layered-Modus nur bei der Konstruktion fest).

```csharp
// 64-bit-sicher: SetWindowLongPtr per IntPtr.Size dispatchen (siehe §-Falle C6)
public static void SetClickThrough(Window w, bool transparent) {
    IntPtr h = new WindowInteropHelper(w).Handle;        // nach OnSourceInitialized
    long ex = GetExStyle(h);
    ex = transparent ? ex | 0x20 : ex & ~0x20;           // nur WS_EX_TRANSPARENT togglen
    SetExStyle(h, ex);
}
```

**Teil-Klickbarkeit (nur bestimmte Bereiche interaktiv):**
- WPF-Ebene: durchlässige Flächen mit `Background="{x:Null}"` (NICHT `"Transparent"` — WPF behandelt jeden
  gefüllten Brush als „solides Glas"), interaktive Controls bekommen einen echten Brush; `IsHitTestVisible="False"`
  schaltet Einzelelemente durchlässig.
- HWND-Ebene (echtes Durchklicken zu **fremden** Apps): `WS_EX_TRANSPARENT` gilt fürs ganze Fenster — für
  regionsabhängige Interaktivität das Bit positionsabhängig per Maus-Hook/Timer togglen.

**Harte Grenze:** Hover **und** Durchklicken gleichzeitig ist unmöglich — mit `WS_EX_TRANSPARENT` bekommt
das Fenster gar keine Maus-Messages. Workaround: fast-transparenter Brush (`#01000000`) oder globaler
`WH_MOUSE_LL`-Hook. Touch wird von `WS_EX_TRANSPARENT` ebenfalls geschluckt.

Quellen: [Extended Window Styles](https://learn.microsoft.com/en-us/windows/win32/winmsg/extended-window-styles) · [SetWindowLongW](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowlongw) · [Transparent Windows in WPF (Dwayne Need, MS-Blog)](https://learn.microsoft.com/en-us/archive/blogs/dwayneneed/transparent-windows-in-wpf) · offiziell.

---

## §4 Globale Hotkeys

**Entscheidung:** Toggle-Hotkey mit Modifier → **`RegisterHotKey`** (leichtgewichtig, OS-verwaltet).
Push-to-Talk / Einzeltaste ohne Modifier → **`WH_KEYBOARD_LL`-Hook** (RegisterHotKey kennt kein Release-Event).

**RegisterHotKey über `HwndSource` (kanonisch):**

```csharp
const uint MOD_CONTROL=0x2, MOD_SHIFT=0x4, MOD_NOREPEAT=0x4000; // NOREPEAT = kein Dauerfeuer
const int WM_HOTKEY=0x0312, HOTKEY_ID=9000;

protected override void OnSourceInitialized(EventArgs e) {
    base.OnSourceInitialized(e);
    var h = new WindowInteropHelper(this).Handle;
    HwndSource.FromHwnd(h).AddHook(Hook);
    bool ok = RegisterHotKey(h, HOTKEY_ID, MOD_CONTROL|MOD_SHIFT|MOD_NOREPEAT, 0x56 /*V*/);
    if (!ok) { /* GetLastWin32Error()==1409 → Kombi belegt: Alternative anbieten, NICHT still schlucken */ }
}
IntPtr Hook(IntPtr hwnd, int msg, IntPtr wp, IntPtr lp, ref bool handled) {
    if (msg==WM_HOTKEY && wp.ToInt32()==HOTKEY_ID) { ToggleOverlay(); handled=true; }
    return IntPtr.Zero;
}
protected override void OnClosed(EventArgs e) {
    UnregisterHotKey(new WindowInteropHelper(this).Handle, HOTKEY_ID); // PFLICHT, sonst Kombi systemweit belegt
    base.OnClosed(e);
}
```

**Low-Level-Hook (Push-to-Talk) — die kritischen Regeln:** Delegate als **statisches Feld** halten
(sonst GC → `ExecutionEngineException`); Callback **schlank** halten und unter dem `LowLevelHooksTimeout`
(max. 1000 ms) bleiben, sonst entfernt Windows den Hook still; `CallNextHookEx` immer aufrufen (außer die
Taste soll bewusst global blockiert werden); installierender Thread braucht eine Message-Loop (in WPF der
UI-Dispatcher). Auf `WM_KEYDOWN`/`WM_SYSKEYDOWN` → Mikro an, `WM_KEYUP`/`WM_SYSKEYUP` → aus.

**UAC/UIPI:** Ein nicht-elevated Overlay empfängt `WM_HOTKEY` (und LL-Hook-Events) **nicht**, solange ein
**elevated** Fenster im Vordergrund ist. Pragmatik: App nicht-elevated halten und das Aussetzen über
UAC-Dialogen/Admin-Fenstern akzeptieren; echte Cross-Privilege-Reichweite nur via `uiAccess`-Manifest
(signiert + `Program Files`).

`extern`-Libs: **NHotkey.Wpf** (v4, Jan 2026) für deklarative Toggle-Hotkeys; **SharpHook** (v7.1, Mai 2026)
für Push-to-Talk (echte KeyReleased-Events). Beide parallel kollidieren nicht.

Quellen: [RegisterHotKey](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-registerhotkey) · [WM_HOTKEY](https://learn.microsoft.com/en-us/windows/win32/inputdev/wm-hotkey) · [LowLevelKeyboardProc](https://learn.microsoft.com/en-us/windows/win32/winmsg/lowlevelkeyboardproc) · [SetWindowsHookExW](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowshookexw) · offiziell. `extern`: [Magnus Montin](https://blog.magnusmontin.net/2015/03/31/implementing-global-hot-keys-in-wpf/), [NHotkey](https://github.com/thomaslevesque/NHotkey), [SharpHook](https://github.com/TolikPylypchuk/SharpHook).

---

## §5 Transparenz & abgerundete Ecken ohne Flackern

**Kern-Entscheidung: `AllowsTransparency=true` vermeiden, wenn es geht.** Es erzwingt den
`UpdateLayeredWindow`-Pfad (Per-Pixel-Alpha, Software-Composition), bringt **Airspace** (kein WebView2/
MediaElement/D3DImage sichtbar im Fenster) und kostet Performance.

**Weg A — DWM-Backdrop + runde Ecken (Win11, empfohlen):** `AllowsTransparency=false` + `WindowStyle=None`
+ `Background="Transparent"` + `WindowChrome` (`GlassFrameThickness="-1"` = „sheet of glass"); in
`OnSourceInitialized` per `DwmSetWindowAttribute`:

```csharp
const int DWMWA_USE_IMMERSIVE_DARK_MODE=20, DWMWA_WINDOW_CORNER_PREFERENCE=33,
          DWMWA_BORDER_COLOR=34, DWMWA_SYSTEMBACKDROP_TYPE=38;
const int DWMWCP_ROUND=2, DWMSBT_NONE=1, DWMSBT_TRANSIENTWINDOW=3; // Acrylic = transient

int dark=1;     DwmSetWindowAttribute(h, DWMWA_USE_IMMERSIVE_DARK_MODE, ref dark, 4);   // VOR Backdrop
int round=DWMWCP_ROUND;          DwmSetWindowAttribute(h, DWMWA_WINDOW_CORNER_PREFERENCE, ref round, 4);
int bd=DWMSBT_TRANSIENTWINDOW;   DwmSetWindowAttribute(h, DWMWA_SYSTEMBACKDROP_TYPE, ref bd, 4); // oder DWMSBT_NONE
int none=unchecked((int)0xFFFFFFFE); DwmSetWindowAttribute(h, DWMWA_BORDER_COLOR, ref none, 4); // randlos
```

DWM zeichnet runde Ecken/Material hardwarebeschleunigt; auf Windows 10 sind die Calls still wirkungslos
(sauberes Degradieren). **Hinweis:** WPFs Fluent-Theme (`ThemeMode`, .NET 9+) setzt automatisch **Mica** —
für ein bewusst durchscheinendes Overlay meist nicht gewollt → `ThemeMode="None"` lassen.

**Weg B — echtes Per-Pixel-Alpha:** Nur wenn beliebig geformte, weich-transluzente Pixel über fremdem
Desktop nötig sind und **keine** Child-HWNDs (WebView2 etc.) im Fenster liegen → `AllowsTransparency=true`.

**Flacker-Best-Practices (immer):** Fenster mit `Opacity=0`/`Hidden` starten, in **`ContentRendered`**
einblenden (vermeidet Start-Blitz); DWM-Attribute in `SourceInitialized`/nach `EnsureHandle`, nicht in
`Loaded`; Dark-Mode **vor** dem Backdrop setzen; `RenderCapability.Tier` prüfen (Tier 0 = Software → Animationen
sparsam); `BitmapCache` für statische Teile.

Quellen: [DWMWINDOWATTRIBUTE](https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute) · [Apply rounded corners (Win11)](https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/ui/apply-rounded-corners) · [Graphics Rendering Tiers](https://learn.microsoft.com/en-us/dotnet/desktop/wpf/advanced/graphics-rendering-tiers) · [What's new WPF .NET 9](https://learn.microsoft.com/en-us/dotnet/desktop/wpf/whats-new/net90) · offiziell. `extern`: [Airspace (Dwayne Need)](https://dwayneneed.github.io/wpf/2013/02/26/mitigating-airspace-issues-in-wpf-applications.html), [Mica in WPF (tvc-16)](https://tvc-16.science/mica-wpf.html), [WPF-UI lepoco](https://wpfui.lepo.co).

**Weg C — unsichtbare Fangflaeche ueber einem fremden Fenster (Tk, WinForms, jedes Toolkit):**
Soll ein Overlay die Maus abfangen, ohne das Bild darunter zu veraendern (Auswahl-Werkzeuge,
Messwerkzeuge, Lupen), gilt:

1. **Nicht per `SetParent` in das Zielfenster einhaengen.** `WS_EX_LAYERED` wirkt nur auf die
   Zeichnung des Fensters selbst; jedes Kind-HWND darin (in Tk die Zeichenflaeche, in WinForms
   jedes Control) wird ungefiltert darueber gemalt und bleibt **voll deckend**. Der eingestellte
   Alpha-Wert ist dann folgenlos — ein Fehlerbild, das man endlos am falschen Ende sucht
   (Bug-Almanach C17).
2. **Stattdessen ein eigenstaendiges Top-Level-Fenster darueberlegen** (`WS_EX_TOOLWINDOW`,
   topmost, randlos) und seine Lage dem Zielfenster nachfuehren — z. B. alle 250 ms aus
   `GetClientRect` + `ClientToScreen`. Dann greift die Deckkraft auf den gesamten Inhalt.
3. **Deckkraft 1/255, nicht 0.** Bei 0 nimmt das Fenster keine Maustaste mehr an; 1/255 ist mit
   blossem Auge nicht von unsichtbar zu unterscheiden und faengt weiter.
4. **Farbschluessel (`LWA_COLORKEY`) taugt hier NICHT** als „unsichtbar": Klicks fallen durch die
   schluesselfarbenen Bereiche hindurch. Er ist das Mittel der Wahl fuer das Gegenteil — sichtbare
   Linien ohne Flaeche, etwa einen Auswahlrahmen: Fenster in der Schluesselfarbe fuellen, nur die
   Linie in echter Farbe zeichnen, `WS_EX_TRANSPARENT` dazu, damit es die Maus nicht abfaengt.
5. **Farbe niemals auf der halbdurchsichtigen Flaeche zeichnen.** Eine Linie auf einer Flaeche mit
   11 % Deckkraft kommt als blasses Grau an. Sichtbare Elemente gehoeren in ein eigenes
   Farbschluessel-Fenster darueber (Punkt 4).
6. **Pruefen, nicht fotografieren.** Ob die Durchsichtigkeit sitzt, beantwortet
   `GetLayeredWindowAttributes` je HWND — Bildschirmfotos ueber `BitBlt`/`ImageGrab` erfassen solche
   Fenster nicht verlaesslich und liefern falsche Entwarnung (Bug-Almanach C18).

Gegenprobe in drei Zeilen (Python/ctypes, sinngemaess in jeder Sprache):

```python
stil = user32.GetWindowLongW(hwnd, GWL_EXSTYLE)          # WS_EX_LAYERED gesetzt?
ok = user32.GetLayeredWindowAttributes(hwnd, byref(key), byref(alpha), byref(flags))
# ok == 0  -> keine Deckkraft gesetzt -> Fenster ist voll deckend
```

Belegt am 14.08.2026 im Werkzeug `Werkzeuge/zeigefinger` (Python/Tk ueber scrcpy), Windows 11 26200.

---

## §6 Multi-Monitor & DPI-Skalierung

**WPF ist standardmäßig nur System-Aware** (vom WPF-Team bestätigt) — auf einem Monitor mit anderer
Skalierung als der Primärmonitor wird das Overlay **bitmap-gestretcht** (unscharf) und falsch dimensioniert.
Es gibt **keinen** csproj-Schalter (`<ApplicationHighDpiMode>` ist WinForms-only); Awareness wird **nur**
über `app.manifest` gesetzt:

```xml
<application xmlns="urn:schemas-microsoft-com:asm.v3"><windowsSettings>
  <dpiAwareness xmlns="http://schemas.microsoft.com/SMI/2016/WindowsSettings">PerMonitorV2,PerMonitor</dpiAwareness>
  <dpiAware xmlns="http://schemas.microsoft.com/SMI/2005/WindowsSettings">true</dpiAware>
</windowsSettings></application>
```

**Positionierung:** WPF-`Left`/`Top` sind **DIPs** (96-DPI), `Screen.Bounds`/Win32-`RECT` sind **physische
Pixel** (im PMv2-Prozess echt). Umrechnen: `DIP = Pixel / DpiScaleX`. `WindowStartupLocation = Manual`
**vor** dem Anzeigen (sonst überschreibt `CenterScreen` die Koordinaten). DPI-Faktor robust über
`VisualTreeHelper.GetDpi(this)` bzw. `HwndSource.CompositionTarget.TransformFromDevice` (nicht
`GetDpiForMonitor` aus dem PMv2-Thread). Auf Monitor-Drag in `OnDpiChanged`/`Window.DpiChanged` reagieren
(`base.OnDpiChanged` aufrufen, WPF skaliert selbst).

Quellen: [High-DPI Desktop Dev](https://learn.microsoft.com/en-us/windows/win32/hidpi/high-dpi-desktop-application-development-on-windows) · [Default DPI awareness for a process](https://learn.microsoft.com/en-us/windows/win32/hidpi/setting-the-default-dpi-awareness-for-a-process) · [VisualTreeHelper.GetDpi](https://learn.microsoft.com/en-us/dotnet/api/system.windows.media.visualtreehelper.getdpi) · [Window.DpiChanged](https://learn.microsoft.com/en-us/dotnet/api/system.windows.window.dpichanged) · offiziell. `extern`: [dotnet/wpf#859](https://github.com/dotnet/wpf/issues/859).

---

## §7 System-Tray

WPF hat **kein** natives NotifyIcon. Empfehlung: **`H.NotifyIcon.Wpf`** (aktiv gepflegt, NativeAOT/Trimming,
XAML/MVVM, WPF-ContextMenu statt WinForms-Look) — **nicht** das verbreitete `Hardcodet.NotifyIcon.Wpf`
(inaktiv seit 2024, kein NativeAOT). Beide haben dieselbe `tb:TaskbarIcon`-API und behandeln die
`TaskbarCreated`-Message (Explorer-Neustart) intern. Alternative ohne Dependency:
`System.Windows.Forms.NotifyIcon` (WinForms-Stilbruch) oder rohes `Shell_NotifyIcon`-P/Invoke.

Praxis: `.ico` mit **mehreren Auflösungen** (16/20/24/32/48/256) für scharfe Icons bei High-DPI;
Fenster beim „Schließen" nur verstecken (`e.Cancel=true; Hide()`), App läuft im Tray weiter.

Quellen: [Notification Area (Win32)](https://learn.microsoft.com/en-us/windows/win32/shell/notification-area) · [NOTIFYICONDATA (DPI/GUID)](https://learn.microsoft.com/en-us/windows/win32/api/shellapi/ns-shellapi-notifyicondataa) · offiziell. `extern`: [H.NotifyIcon](https://github.com/HavenDV/H.NotifyIcon), [hardcodet (inaktiv)](https://github.com/hardcodet/wpf-notifyicon).

---

## §8 Autostart

**Unpackaged (.exe, häufigster Fall):** `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`, per-User,
kein Admin/UAC-Prompt. Pfad **immer in Anführungszeichen** (sonst bricht ein Pfad mit Leerzeichen) und
**`Environment.ProcessPath`** statt `Assembly.Location` (das ist bei single-file-publish leer):

```csharp
key.SetValue("MeineApp", $"\"{Environment.ProcessPath}\" --autostart");
```

Beim Autostart das Argument auswerten und **minimiert in den Tray** starten (kein Fenster zeigen).
**Elevated ohne UAC-Prompt → Task Scheduler** (`/SC ONLOGON /RL HIGHEST`). **MSIX/Packaged → `windows.startupTask`-
Manifest + `StartupTask.RequestEnableAsync()`** (nie Registry — wird virtualisiert). Den vom User im
Task-Manager **deaktivierten** Autostart **nie** automatisch reaktivieren (bei `StartupTask` ist
`RequestEnableAsync` nach `DisabledByUser` per Design wirkungslos).

Quellen: [StartupTask](https://learn.microsoft.com/en-us/uwp/api/windows.applicationmodel.startuptask) · offiziell. `extern`: [Microsoft.Win32.TaskScheduler](https://github.com/dahall/TaskScheduler).

---

## §9 Single-Instance

**Named `Mutex` (`createdNew`-Muster)** für die Sperre + **Named Pipe** (oder `WM_COPYDATA`) für Argument-
Übergabe an die laufende Instanz:

```csharp
_mutex = new Mutex(true, @"Local\MeineApp_{GUID}", out bool isNew);
if (!isNew) { SendArgsToRunningInstance(e.Args); Shutdown(); return; }
```

Namensraum bewusst wählen: `Local\…` = pro Session/User (Tray-App), `Global\…` = maschinenweit. Das
`createdNew`-Muster ist crash-fest (OS gibt den Mutex bei Prozess-Ende frei) — bei `WaitOne`-Varianten
`AbandonedMutexException` als „darf starten" behandeln. **Bestehende Instanz nach vorne holen:**
`SetForegroundWindow` ist gesperrt, wenn der User mit etwas anderem arbeitet → die **neue** Instanz ruft
`AllowSetForegroundWindow(pidAlt)` auf, oder die **alte** Instanz aktiviert sich selbst (per Pipe/`WM_COPYDATA`
getriggert — ein Prozess darf sich selbst nach vorne holen). Fertig-von-der-Stange:
`Microsoft.VisualBasic`-`WindowsFormsApplicationBase.IsSingleInstance` (inkl. `OnStartupNextInstance`-Args).

Quellen: [Mutex](https://learn.microsoft.com/en-us/dotnet/api/system.threading.mutex) · [SetForegroundWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow) · [WindowsFormsApplicationBase.IsSingleInstance](https://learn.microsoft.com/en-us/dotnet/api/microsoft.visualbasic.applicationservices.windowsformsapplicationbase.issingleinstance) · offiziell.

---

## §10 Z-Order-Koexistenz & Aufmerksamkeit

**Grundregel:** Im normalen User-Space gibt es **kein** „über allen anderen Topmost-Fenstern" — alle
Topmost-Fenster teilen sich eine Z-Order-Gruppe (Band `ZBID_DESKTOP`); das zuletzt nach vorne gebrachte
gewinnt. System-UI (Startmenü, Action Center, Task-Manager, Bildschirmtastatur) liegt in höheren Bändern
und ist von Drittanbieter-Topmost **prinzipiell** nicht überdeckbar.

- **Kein Timer-Spam:** periodisches `SetWindowPos(HWND_TOPMOST)` ist ein Notnagel. Sauber: ereignisgetrieben
  auf `WM_WINDOWPOSCHANGED` re-asserten, nur wenn real verdeckt; falls Polling, dann ≥1 s + `SWP_NOACTIVATE`.
- **Kante reservieren statt überlagern → AppBar** (`SHAppBarMessage`): `ABM_NEW` → `ABM_QUERYPOS` →
  `ABM_SETPOS` → `MoveWindow`; auf `ABN_POSCHANGED`/`ABN_FULLSCREENAPP` reagieren; **`ABM_REMOVE` zwingend**
  beim Beenden/Crash (sonst bleibt Desktop-Platz reserviert). Das ist der einzige Weg, andere Fenster echt
  *fernzuhalten*.
- **Aufmerksamkeit ohne Fokus-Klau → `FlashWindowEx`** mit `FLASHW_ALL | FLASHW_TIMERNOFG` (blinkt bis der
  User das Fenster holt, ohne zu aktivieren/wiederherzustellen).
- **Vollbild erkennen → `SHQueryUserNotificationState`**: bei `QUNS_RUNNING_D3D_FULL_SCREEN`/`QUNS_BUSY`/
  `QUNS_PRESENTATION_MODE` Overlay ausblenden statt erzwingen.
- **Wirklich ganz oben (über andere Topmost-Tools)** nur legitim via **`uiAccess=true`-Manifest** (Band
  `ZBID_UIACCESS`) — signiert + Installation in `Program Files`. Private Band-APIs (`CreateWindowInBand`)
  sind gesperrt und kein Produktionsweg.

Quellen: [SetWindowPos](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos) · [FlashWindowEx](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-flashwindowex) · [Application Desktop Toolbars (AppBar)](https://learn.microsoft.com/en-us/windows/win32/shell/application-desktop-toolbars) · [SHQueryUserNotificationState](https://learn.microsoft.com/en-us/windows/win32/api/shellapi/nf-shellapi-shqueryusernotificationstate) · offiziell. `extern`: [Window Z-Order Deep-Dive (ADeltaX)](https://blog.adeltax.com/window-z-order-in-windows-10/).

---

## 🔗 Bezug zum Bug-Almanach (Kopplung)

| Best-Practice-Abschnitt | Bug-Almanach-Abschnitt (`bugs/desktop/windows-overlay.md`) |
|-------------------------|------------------------------------------------------------|
| §2 (Always-on-top), §10 (Z-Order) | A1–A20 (Win11-Z-Order/ShowInTaskbar/NOACTIVATE/SetForegroundWindow/Topmost-vs-Topmost/Popup/Win+D/virt.Desktop/Monitor-Sleep/Secure-Desktop/Maximize) |
| §3 (Click-through), §5 (Transparenz) | C1–C16 (TRANSPARENT/LAYERED/Touch/Hover/Airspace/Blitz/runde Ecken/IsHitTestVisible/DropShadow/Window-Sharing/RDP/Resize) |
| §4 (Globale Hotkeys) | H1–H16 (RegisterHotKey/NOREPEAT/LL-Hook-GC/Timeout/UIPI/Push-to-Talk/SharpHook-Deploy/Multi-Window/Anti-Cheat/AltGr/NHotkey) |
| §6 (Multi-Monitor & DPI) | D1–D16 (System-Aware/Manifest/DIP-Pixel/DpiChanged/Popup-DPI/ContextMenu/ToolWindow/MessageBox/CenterScreen/.NET7-Regression) |
| §7–§9 (Tray/Autostart/Single-Instance) | T1–T22 (TaskbarCreated/Icon-DPI/GUID/Run-Quotes/Mutex/ContextMenu-Z/Doppelklick/Win11-ToolTip/Middle-Click/Pipe/MSIX/StartupApproved/Task-Scheduler) |
| §5 (Transparenz & runde Ecken), §1 (Architektur) | W1–W10 (WindowChrome-Maße/NonClientFrameEdges/GlassFrame/Resize-Flacker/Mica-Acrylic-Lebenszyklus/Backdrop-Frame) |
| §1 (Architektur), §7–§9 | P1–P9 (Fluent-Theme-Crash/.NET10-TextBox/High-Contrast/BinaryFormatter-Clipboard/Trimming-AOT/Single-File/ShutdownMode/Leaks) |

> **Checkpoint:** Vollständig recherchiert (7 Researcher parallel, Microsoft Learn zuerst, Stand 2026-06-14,
> .NET 10 / WPF, Windows 10/11). Kern für TVO/ClaudeVoiceOverlay: `WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW`-Topmost
> ohne `SetForegroundWindow`, DWM-Weg (kein `AllowsTransparency`) für runde transparente Optik, `RegisterHotKey`
> + `MOD_NOREPEAT` (Push-to-Talk via LL-Hook), `app.manifest` PerMonitorV2, und Z-Order ereignisgetrieben statt
> Timer-Spam re-asserten.
