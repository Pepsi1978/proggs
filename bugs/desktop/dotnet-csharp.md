# Bekannte Bugs & Fallen: C#/.NET 10 — WPF, WinUI 3, Windows-Desktop

> **PFLICHT-LESEN vor JEDER echten Arbeit an C#/.NET-Code** (`.cs`, `.csproj`, `.xaml`
> in WPF-/WinUI-3-/WinForms-/Konsolen-/Backend-Projekten). Trivialer Kleinkram (einzelner
> String, Doku, Versions-Bump) ausgenommen. Loesungen sind **funktionserhaltend** — nie
> "Feature weglassen".
>
> **Stand:** recherchiert am **2026-06-02**, **re-recherchiert am 2026-07-02** (Engine A: Firecrawl+MiniMax)
> — **LIVE jetzt .NET 10** (Sprung von .NET 8): `dotnet --version` = **10.0.301**, alle 4 Projekte auf
> **net10.0-windows**, **C# 14**.
> **Anker:** dotnet=10.0  <!-- maschinenlesbar fuer check-version-anchor.py -->
> WPF auf .NET, WinUI 3 / Windows App SDK, Windows 10/11. Kontext: self-contained
> single-file Desktop-Overlays (TerminalVoiceOverlay, ClaudeVoiceOverlay, PromptBoard).
> **.NET 10 ist LTS** (Support bis 14.11.2028). GitHub-Issue-Status per `gh` verifiziert (OPEN/CLOSED je Bug).
> Versionsangaben pro Bug beachten — viele Punkte sind "per Design" und gelten dauerhaft.
> **Beim Sprung net8.0→net10.0 MUSS man die Breaking Changes von .NET 9 UND .NET 10 pruefen** (9 wird
> uebersprungen). Die neuen Punkte des Sprungs stehen gebuendelt in **§17**; die alten §13-Punkte (.NET 7→8)
> gelten weiter, wenn ein Projekt diesen Sprung noch nicht gemacht hat.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `.Result`/`.Wait()` auf UI-Thread | Durchgaengig `await`; in Lib `ConfigureAwait(false)` | §7.1 |
| 2 | Single-File: `Assembly.Location` leer | `Environment.ProcessPath` statt `Assembly.Location` nutzen | §1.1 |
| 3 | Content-Dateien fehlen nach Publish | Assets im Publish-Skript explizit neben EXE spiegeln | §1.9 |
| 4 | WPF veroeffentlichen | Kein `PublishTrimmed`/AOT bei WPF verwenden | §1.5 |
| 5 | Transparentes Click-through-Overlay | Overlay in WPF bauen, nicht WinUI 3 | §12.7 |
| 6 | Clipboard-Zugriff | IMMER Retry-Schleife um jeden Clipboard-Aufruf | §4.1 |
| 7 | Upgrade net8.0 → **net10.0** (C# 14) | `field`-Keyword-Konflikt + null-cond.-Assignment-Falle; single-file native-lib-Suche geaendert; Clipboard-Obsoletion | §17 |
| 7 | Overlay liegt nicht obenauf | `SetWindowPos(HWND_TOPMOST, SWP_NOACTIVATE)` periodisch setzen | §2.1 |
| 8 | Scharfe Anzeige ueber Monitore | `app.manifest PerMonitorV2`; Position per physische Pixel | §3.1 |
| 9 | Viele HTTP-Calls (Whisper/Gemini) | Ein statischer Client mit `PooledConnectionLifetime` | §8.1 |
| 10 | `SetForegroundWindow` wirkt nicht | `AttachThreadInput`-Trick oder Trigger aus eigenem Prozess | §5.5 |
| 11 | Event-Handler feuert beim XAML-Laden | `_ready`-Flag; Handler returnt solange false | §2.10 |
| 12 | `UseWindowsForms` + ImplicitUsings (CS0104) | Globale `Using Remove` fuer Drawing/Forms im csproj | §6.7 |
| 13 | `dotnet publish` self-contained | `--self-contained true` explizit (RID impliziert es nicht) | §13.1 |
| 14 | Memory-Leak bei Events/Timern | Im `Dispose`/`Unloaded` immer `-=` und `Stop()` | §9.1 |
| 15 | `Process.Start(url)` wirft | `new ProcessStartInfo(url){ UseShellExecute = true }` | §13.8 |
| 16 | One-Shot-Hotkey haengt nach mehrfacher Nutzung | KeyUp-Debounce-Flag durch zeitbasierten Cooldown ersetzen | §5.8 |

---

## 🔗 Bezug zu den Best Practices ([`best-practices/desktop/dotnet-csharp.md`](../../best-practices/desktop/dotnet-csharp.md))

Dieser Almanach sagt *was schiefgeht*; die Best-Practices-Datei sagt *wie man es von vornherein
richtig macht*. Wechselseitig gepflegt:

| Bug-Abschnitt (hier) | Praevention in `best-practices/desktop/dotnet-csharp.md` |
|----------------------|------------------------------------------------------|
| §1 Single-File/Trimming, §13.1 self-contained | BP 3 Deployment/Publish |
| §2 WPF-Overlay, §12.7 WinUI kein Overlay | BP 4 UI-Framework-Wahl & Overlays |
| §3 DPI/Multi-Monitor, §14 Manifest | BP 5 Win32-Integration (Manifest) |
| §4 Clipboard | BP 5 + TL;DR 5 |
| §7 async/Threading | BP 1 Async/Threading |
| §8 HttpClient | BP 2 HTTP/Netzwerk |
| §9 Leaks/Disposables | BP 6 Ressourcen/Disposables |
| §10 System.Text.Json | BP 7 Daten/Serialisierung |
| §11 C#-Sprache/Mechanik | BP 7 Sprache + BP 1 (Collections) |
| §12 WinUI 3 | BP 4 + BP 1 (Threading) |

---

## 1. Deployment: Single-File / Self-Contained / Trimming

### 1.1 `Assembly.Location` ist leer im Single-File  ⭐ HAEUFIG
**Symptom:** Code, der ueber `Assembly.Location`/`CodeBase` Dateien neben der EXE findet, bekommt `""` (z.B. `.env`/Config neben der exe wird nicht gefunden).
**Ursache:** Im Single-File-Bundle haben gebundelte Assemblies keinen Dateipfad (Analyzer IL3000).
**Versionen:** .NET 5+ inkl. 8 — per Design (dotnet/runtime#13531 CLOSED COMPLETED = dokumentiert, Verhalten bleibt).
**FIX:** Pfad neben der echten EXE via `Environment.ProcessPath` (.NET 6+) bzw. `Path.GetDirectoryName(Environment.ProcessPath)`; `AppContext.BaseDirectory` funktioniert nur ohne Self-Extract.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/warnings/il3000 · dotnet/runtime#13531

### 1.2 Native Runtime-DLLs nicht im Single-File enthalten
**Symptom:** "single file" hat trotzdem mehrere native `.dll` daneben; oder `DllNotFoundException`.
**Ursache:** Standardmaessig werden nur **managed** DLLs gebuendelt.
**Versionen:** .NET 5+ — per Design (dotnet/runtime#42772 CLOSED COMPLETED).
**FIX:** `<IncludeNativeLibrariesForSelfExtract>true</IncludeNativeLibrariesForSelfExtract>` (extrahiert nach `%TEMP%/.net`). ABER siehe 1.3 (WPF-Konflikt).
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/overview · dotnet/runtime#42772

### 1.3 WPF + Single-File mit eingebetteten Native-Libs crasht
**Symptom:** WPF-App startet nicht / crasht beim Laden eingebetteter Native-DLLs aus dem TEMP-Extraktionsordner.
**Ursache:** WPFs DLL-Consistency-Check verbietet das Laden von Native-DLLs aus dem TEMP-Self-Extract-Ordner.
**Versionen:** .NET 5+ WPF (dotnet/runtime#38636 CLOSED COMPLETED, Verhalten bleibt relevant).
**FIX:** `IncludeNativeLibrariesForSelfExtract` bei WPF MEIDEN bzw. Native-Libs als separate Dateien neben die EXE legen. (Frank-Setup nutzt `IncludeNativeLibrariesForSelfExtract=true` mit `EnableCompressionInSingleFile` — bei nativen Abhaengigkeiten testen, sonst lose ausliefern.)
**Quelle:** dotnet/runtime#38636

### 1.4 Single-File-Extraktion nach %TEMP% (Permissions/Tampering)
**Symptom:** Native Libs werden nach `%TEMP%/.net` extrahiert — auf Mehrbenutzer-Systemen evtl. beschreibbar.
**Ursache:** Default-Extraktionsverzeichnis.
**Versionen:** alle Single-File — per Design.
**FIX:** `DOTNET_BUNDLE_EXTRACT_BASE_DIR` auf geschuetztes Verzeichnis setzen; oder Extraktion vermeiden (nur managed = in-memory).
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/overview

### 1.5 WPF + `PublishTrimmed`/ReadyToRun = kaputtes Rendering/Crash  ⭐ WICHTIG
**Symptom:** Getrimmte/R2R WPF-App rendert kaputt (z.B. Buttons), crasht, oder erzeugt kein gueltiges Executable.
**Ursache:** WPF nutzt massiv Reflection; Trimming entfernt benoetigten Code; im SDK fuer WPF deaktiviert.
**Versionen:** .NET 8 (dotnet/wpf#11436 **OPEN**, dotnet/runtime#60936).
**FIX:** `PublishTrimmed` fuer WPF NICHT verwenden (funktionserhaltend: weglassen). Self-contained ohne Trim. R2R fuer WPF nur ohne Trimming.
**Quelle:** dotnet/wpf#11436 · dotnet/runtime#60936

### 1.6 `PublishTrimmed` deaktiviert reflection-basiertes System.Text.Json
**Symptom:** Getrimmte App wirft `InvalidOperationException: Reflection-based serialization has been disabled` schon bei `JsonSerializer.Serialize(new {...})`.
**Ursache:** `PublishTrimmed=true` setzt automatisch `JsonSerializerIsReflectionEnabledByDefault=false`.
**Versionen:** ab .NET 8 — per Design.
**FIX:** STJ Source-Generator nutzen (empfohlen); oder `<JsonSerializerIsReflectionEnabledByDefault>true</JsonSerializerIsReflectionEnabledByDefault>`.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/serialization/8.0/publishtrimmed

### 1.7 P/Invoke `SetWindowsHookEx` schlaegt in Single-File/x86 fehl
**Symptom:** Globaler Hook (Hotkeys/Tastatur) funktioniert nur im normalen Build, nicht als Single-File self-contained x86.
**Ursache:** Hook braucht gueltiges Modul-Handle; Single-File-Host liefert kein passendes `hMod`.
**Versionen:** .NET 6/7, x86 (dotnet/runtime#71522 CLOSED COMPLETED — in neuerer Version verbessert, bei x86 weiter beachten).
**FIX:** `GetModuleHandle(null)` statt EXE-Pfad; Low-Level-Hook (`WH_KEYBOARD_LL`); nicht als win-x86-Single-File ausliefern (win-x64 nutzen).
**Quelle:** dotnet/runtime#71522

### 1.8 `StripSymbols`/`DebugSymbols` — keine PDBs mehr
**Symptom:** Native-AOT/self-contained Build hat keine Debug-Symbole; oder keine `.pdb` trotz frueher.
**Ursache:** `StripSymbols` default true (.NET 8); `DebugSymbols=false` unterdrueckt PDB komplett.
**Versionen:** ab .NET 8 — per Design.
**FIX:** `<StripSymbols>false</StripSymbols>` bzw. `DebugSymbols` nicht auf false; PDB-Verhalten ueber `DebugType` steuern.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/deployment/8.0/stripsymbols-default

### 1.9 Single-File-Publish kopiert `<Content>`-Dateien NICHT zuverlaessig neben die EXE  ⭐ WICHTIG
**Symptom:** `dotnet build` legt Content-Dateien (z.B. ML-Modelle, Sounds) korrekt nach `bin/...` ab und alles funktioniert; nach `dotnet publish -p:PublishSingleFile=true` fehlen genau diese Dateien neben der EXE — der Ziel-Unterordner wird zwar ANGELEGT, bleibt aber LEER. Zur Laufzeit `FileNotFoundException` beim Laden (Pfad via `AppContext.BaseDirectory`).
**Konkreter Vorfall 2026-06-08 (VoiceAgent):** `assets/wakeword-model/encoder.onnx` fehlte im publish → `SherpaWakeWordEngine` warf FileNotFound → das Weckwort-Feature deaktivierte sich still (catch + Fallback), die App zeigte „ruht — sag Weckwort", verarbeitete aber JEDE Aussage. `bin/Release` hatte alle 7 Modelldateien, `publish/assets/wakeword-model/` war leer.
**Ursache:** `<Content Include="..\**\*" CopyToOutputDirectory="PreserveNewest" />` steuert primaer den BUILD-Output. Beim Single-File-Publish werden Wildcard-Content-Ordner nicht zuverlaessig in den Publish-Output uebernommen (bekannte single-file-Eigenheit; native Libs sind embedded, lose Content-Dateien aber nicht).
**Versionen:** .NET 6–10, `PublishSingleFile=true` (+ `IncludeNativeLibrariesForSelfExtract`/`EnableCompressionInSingleFile`).
**FIX (funktionserhaltend):** Im Publish-Skript NACH `dotnet publish` die Content-Assets EXPLIZIT neben die EXE spiegeln (`Copy-Item src\...\assets\* $out\assets -Recurse -Force`) und das Vorhandensein der Schluesseldatei verifizieren (`Test-Path encoder.onnx`, sonst Warnung). Ergaenzend `CopyToPublishDirectory="PreserveNewest"` am Content-Item setzen. Native-API-Modelle gehoeren als lose Dateien neben die EXE, nicht embedded (vgl. wake-word-Almanach §6).
**Quelle:** eigener Vorfall 2026-06-08 (VoiceAgent, per Observability-Sonde lokalisiert: „Wake-Word-Modelldatei fehlt: encoder.onnx") · https://learn.microsoft.com/en-us/dotnet/core/deploying/single-file/overview

---

## 2. WPF Overlay-spezifisch (Topmost, Transparenz, Airspace, DWM)

### 2.1 Topmost geht verloren bei `ShowInTaskbar=false`  ⭐ OVERLAY
**Symptom:** Overlay (`Topmost=true`) liegt hinter anderen Fenstern, sobald `ShowInTaskbar=false`.
**Ursache:** WPF erzeugt fuer ShowInTaskbar=false ein verstecktes Owner-Fenster OHNE `WS_EX_TOPMOST`.
**Versionen:** alle WPF inkl. .NET 8 (architektonisch, verwandt dotnet/wpf#152).
**FIX:** Per `SetWindowPos(hwnd, HWND_TOPMOST, …, SWP_NOMOVE|SWP_NOSIZE|SWP_NOACTIVATE)` nach dem Laden Topmost manuell (ggf. periodisch) setzen; oder ShowInTaskbar=true lassen.
**Quelle:** https://learn.microsoft.com/en-us/answers/questions/1181672/

### 2.2 Topmost verliert sich gegen Fullscreen-/andere Topmost-Fenster  ⭐ OVERLAY
**Symptom:** Overlay verschwindet hinter Vollbild-Apps/Spielen oder anderen Topmost-Fenstern.
**Ursache:** In der Topmost-Z-Ordnung gewinnt das zuletzt aktivierte Fenster; exklusiver Fullscreen umgeht die Z-Order ganz.
**Versionen:** Win32, alle.
**FIX:** `SetWindowPos` mit `SWP_NOACTIVATE` periodisch (Timer) erneut auf HWND_TOPMOST, ohne Fokus zu klauen. Exklusiver Fullscreen laesst sich nicht ueberlagern → Borderless-Erkennung; ggf. AppBar + `ABN_FULLSCREENAPP`.
**Quelle:** https://github.com/wailsapp/wails/issues/4272

### 2.3 `AllowsTransparency=true` bremst Rendering (Layered Window)  ⭐ OVERLAY
**Symptom:** Overlay mit `AllowsTransparency=True` + `WindowStyle=None` ruckelt, hohe CPU/GPU.
**Ursache:** Per-Pixel-Transparenz erzwingt ein Layered Window (`WS_EX_LAYERED`); DWM muss jeden Frame neu komponieren (nicht der schnelle opake Pfad).
**Versionen:** alle WPF inkl. .NET 8 — architektonisch.
**FIX:** Transparente Flaeche klein halten; statische Bereiche per `CacheMode=BitmapCache`; wo moeglich opakes Fenster + `DwmExtendFrameIntoClientArea`. Funktion (Durchsicht) bleibt, nur Scope verkleinern.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.windows.window.allowstransparency

### 2.4 Click-through (`WS_EX_TRANSPARENT`) macht ganzes Fenster maus-tot
**Symptom:** Nach `WS_EX_TRANSPARENT|WS_EX_LAYERED` faellt die Maus durch, aber das Overlay empfaengt selbst keine Klicks (Buttons tot).
**Ursache:** `WS_EX_TRANSPARENT` macht das GANZE Fenster click-through, nicht nur transparente Regionen.
**Versionen:** Win32, alle.
**FIX:** Click-through je nach Maus-Region per `GetWindowLong/SetWindowLong` toggeln; interaktive Teile in zweites Fenster ohne TRANSPARENT.
**Quelle:** https://windows-hexerror.linestarve.com/q/so55202379

### 2.5 Airspace: HwndHost/WebView2 verdeckt WPF-Inhalt  ⭐
**Symptom:** WPF-Elemente koennen nicht ueber einem WebView2/HwndHost-Control liegen; transparente Bereiche werden schwarz.
**Ursache:** HwndHost mischt Win32-HWND-Rendering mit WPF-Composition (Airspace, >10 Jahre bekannt).
**Versionen:** alle WPF (dotnet/wpf#152 **OPEN**).
**FIX:** `Microsoft.Web.WebView2.Wpf.WebView2CompositionControl` (Visual-Hosting) statt Standard-WebView2 — loest Z-Order + Transparenz.
**Quelle:** dotnet/wpf#152 · MicrosoftEdge/WebView2Feedback#286

### 2.6 Acrylic/Blur (`SetWindowCompositionAttribute`) ruckelt bei Move/Resize
**Symptom:** Mit Acrylic-Blur haengt das Fenster beim Ziehen/Resizen; Blur verschwindet bis maximiert.
**Ursache:** `ACCENT_ENABLE_ACRYLICBLURBEHIND` ist undokumentiert + teuer beim Live-Compositing.
**Versionen:** Win10 v2004+ Regression (dotnet/wpf#3608 **OPEN**).
**FIX:** Blur bei `WM_ENTERSIZEMOVE` aus, bei `WM_EXITSIZEMOVE` an; oder Mica (`DWMWA_SYSTEMBACKDROP_TYPE`, Win11). `DwmEnableBlurBehindWindow` wirkt auf aktuellem Win10/11 nicht mehr wie der Name suggeriert.
**Quelle:** dotnet/wpf#3608

### 2.7 Dark-Mode-Titelleiste erscheint nicht / zu spaet
**Symptom:** `DWMWA_USE_IMMERSIVE_DARK_MODE` faerbt die Titelleiste nicht, oder erst nach Re-Aktivierung.
**Ursache:** Aufruf im Konstruktor (HWND existiert noch nicht); DWM rendert die Leiste nicht sofort neu.
**Versionen:** Win10 1809+.
**FIX:** `DwmSetWindowAttribute` erst in `SourceInitialized`/nach `EnsureHandle()`; nach Theme-Wechsel kurzes `SetWindowPos`-Redraw; auf `SystemEvents.UserPreferenceChanged` hoeren. Attribut-Konstante 20 (19 vor Win10 1903).
**Quelle:** https://learn.microsoft.com/en-us/answers/questions/893697/

### 2.8 Custom WindowChrome maximiert verdeckt die Taskbar
**Symptom:** Fenster mit `WindowStyle=None` + WindowChrome ueberdeckt beim Maximieren die Taskleiste.
**Ursache:** `WM_GETMINMAXINFO` nicht angepasst → Fenster waechst ueber den Arbeitsbereich.
**Versionen:** alle WPF.
**FIX:** `WM_GETMINMAXINFO` per HwndSource-Hook abfangen und auf `MONITORINFO.rcWork` begrenzen.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.windows.shell.windowchrome

### 2.9 Layered Window: `UpdateLayeredWindow` vs. `SetLayeredWindowAttributes`
**Symptom:** Nach einmaligem `SetLayeredWindowAttributes` schlagen weitere `UpdateLayeredWindow` stumm fehl.
**Ursache:** Beide APIs konkurrieren; der Layered-Style muss dazwischen geloescht + neu gesetzt werden.
**Versionen:** alle Windows — per Design.
**FIX:** Nur EINE Methode pro Fenster. Bei Per-Pixel-Alpha durchgehend `UpdateLayeredWindow`. Keine HWND-Children in transparenten WPF-Fenstern (Airspace).
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-updatelayeredwindow

### 2.10 Event-Handler (ValueChanged/Checked/SelectionChanged) feuert waehrend XAML-Load -> NullRef  ⭐ HAEUFIG
**Symptom:** Fenster mit Slider/CheckBox/ComboBox crasht beim Oeffnen — die App verschwindet komplett (NullReferenceException in einem ValueChanged/Checked-Handler).
**Ursache:** Beim XAML-Parsen setzt z.B. `<Slider Minimum="0.002">` einen Wert -> WPF coerced den Value -> `ValueChanged` feuert BEVOR `InitializeComponent` fertig ist. Der Handler greift auf Controls zu, die im XAML WEITER UNTEN stehen und noch nicht erzeugt sind (null). Ein Guard, der nur EIN Control auf null prueft, reicht NICHT — die XAML-Reihenfolge entscheidet, welches Control schon existiert (das vor dem Slider) und welches noch fehlt (das danach).
**Versionen:** alle WPF inkl. .NET 8/10 — per Design (Property-Coercion beim Laden, `RangeBase.OnMinimumChanged`).
**FIX:** Ein `_ready`/`_loaded`-Flag, das erst NACH dem vollstaendigen Befuellen (Populate/Loaded) auf true gesetzt wird; der Handler returnt, solange es false ist. Defense in Depth: `Application.DispatcherUnhandledException` mit `e.Handled = true` (NACH dem Logging) verhindert, dass ein UI-Handler-Fehler die ganze App verschwinden laesst (Graceful Degradation).
**Quelle:** dotnet/wpf — RangeBase Property-Coercion; Frank-Vorfall 2026-06-07 (VoiceAgent: Einstellungen-Fenster-Crash, per Observability-Log lokalisiert).

---

## 3. DPI / Multi-Monitor

### 3.1 WPF nicht Per-Monitor-DPI-aware → unscharf  ⭐ HAEUFIG
**Symptom:** Fenster wird beim Verschieben auf Monitor mit anderer Skalierung unscharf, Koordinaten stimmen nicht.
**Ursache:** Ohne Manifest rendert das OS in System-DPI und streckt bitmap-maessig. WPF-Core ist standardmaessig NICHT High-DPI-aware.
**Versionen:** alle .NET (dotnet/wpf#859, #9803 **OPEN** speziell .NET 8).
**FIX:** `app.manifest` mit `<dpiAwareness>PerMonitorV2</dpiAwareness>` + `<ApplicationManifest>` im csproj; `WM_DPICHANGED` behandeln. Bei .NET 8 zusaetzlich bekannt, dass das Manifest allein manchmal nicht greift (#9803) → Positionierung absichern (3.3).
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/hidpi/declaring-managed-apps-dpi-aware · dotnet/wpf#9803

### 3.2 Mixed-Mode WPF + WinForms: WinForms-Teil bleibt unscharf
**Symptom:** Eingebettete WinForms-Controls unscharf, obwohl der Rest scharf ist.
**Ursache:** WinForms braucht zusaetzlich `Application.SetHighDpiMode(HighDpiMode.PerMonitorV2)` im Code; Manifest allein reicht nicht.
**Versionen:** .NET Core 3.0+/8.
**FIX:** Manifest PerMonitorV2 + `Application.SetHighDpiMode(PerMonitorV2)` VOR dem ersten Fenster.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/hidpi/declaring-managed-apps-dpi-aware

### 3.3 `Window.Left/Top` falsch bei PerMonitorV2  ⭐ OVERLAY
**Symptom:** `Window.Left/Top` liefern falsche/sprunghafte Werte beim Positionieren ueber Monitore mit unterschiedlicher DPI.
**Ursache:** WPF rechnet Left/Top in DIP des aktuellen Monitors; beim DPI-Wechsel entsteht ein Koordinaten-Mismatch.
**Versionen:** WPF PerMonitorV2 (dotnet/wpf#4127 **OPEN**).
**FIX:** Positionierung ueber physische Pixel via `SetWindowPos`/`MoveWindow` (HWND); Monitor-Bounds aus `Screen.AllScreens` oder `MonitorFromWindow`/`GetMonitorInfo`.
**Quelle:** dotnet/wpf#4127

### 3.4 Multi-Monitor-Positionierung mit `SystemParameters` falsch
**Symptom:** Overlay landet auf falschem Monitor / ausserhalb des sichtbaren Bereichs; negative Koordinaten verwirren.
**Ursache:** `SystemParameters.WorkArea`/`PrimaryScreen*` liefern nur den Primaer-Monitor; virtueller Desktop hat negative Koordinaten.
**Versionen:** alle WPF.
**FIX:** `Screen.AllScreens`/`EnumDisplayMonitors` fuer echte Bounds; `SystemParameters.VirtualScreen*` beruecksichtigen.
**Quelle:** https://ireznykov.com/2017/01/13/wpf-windows-on-two-screens/

### 3.5 MessageBox/ToolWindow auf Zweitmonitor unscharf / kein DPI-Wechsel
**Symptom:** System-MessageBox blurry auf Monitor mit anderem DPI; per-monitor ToolWindow skaliert beim Wechsel nicht.
**Ursache:** MessageBox nutzt System-DPI; ToolWindow erhaelt unter Win11 kein `WM_DPICHANGED`.
**Versionen:** WPF (#6775, #10422 **OPEN**), Win11.
**FIX:** Eigene WPF-Dialog-Fenster statt System-MessageBox; Fenster als normales Top-Level statt ToolWindow; DPI-Change per `HwndSource`-Hook abfangen.
**Quelle:** dotnet/wpf#10422

---

## 4. Clipboard

### 4.1 `Clipboard.SetText` wirft `CLIPBRD_E_CANT_OPEN` (0x800401D0)  ⭐ HAEUFIG
**Symptom:** `SetText`/`SetDataObject` haengt hunderte ms und wirft "OpenClipboard Failed" — oft durch RDP-Sync, Editoren, Debugger.
**Ursache:** Nur EIN Thread systemweit darf das Clipboard offen haben.
**Versionen:** alle WPF inkl. .NET 8 (dotnet/wpf#9901 **OPEN**) — Win32-Beschraenkung.
**FIX:** Eigene Retry-Schleife (3-10×, ~100 ms via `await Task.Delay`) um `Clipboard.SetDataObject(data, copy:true)`. NIE ein einzelner Aufruf ohne Retry. `FrameworkCompatibilityPreferences.ShouldThrowOnCopyOrCutFailure` bewusst setzen.
**⚠️ WinForms-vs-WPF-Falle (eigener Vorfall 2026-06-07, TVO PromptInputWindow):** Die 4-Argument-Overload mit eingebautem Retry `Clipboard.SetDataObject(data, copy, retryTimes, retryDelay)` gibt es NUR in `System.Windows.Forms.Clipboard` — das WPF-`System.Windows.Clipboard` kennt nur `(data)` und `(data, copy)`. Ein 4-arg-Aufruf in einer WPF-Datei wirft Compile-Error **CS1501** ("Keine Ueberladung … nimmt 4 Argumente an"). In WPF also IMMER die Schleife von Hand. Selbst wenn das Projekt `UseWindowsForms=true` hat, loest `Clipboard` ohne Namespace-Qualifizierung auf `System.Windows.Clipboard` auf, solange nur `using System.Windows;` (nicht `System.Windows.Forms`) im File steht.
**Quelle:** dotnet/wpf#9901 + eigener Vorfall 2026-06-07

### 4.2 Clipboard-Restore-Timing (Paste-and-Restore)  ⭐ relevant fuer Voice-Overlays
**Symptom:** Nach "altes Clipboard sichern → eigenen Text einfuegen → wiederherstellen" ist der restaurierte Inhalt leer/falsch.
**Ursache:** Restore passiert, bevor die Ziel-App den Paste verarbeitet hat.
**Versionen:** alle WPF — Timing.
**FIX:** Vor Restore ausreichend Delay (`await Task.Delay(150-300ms)`); komplettes `IDataObject` (alle Formate) sichern; Restore in Retry-Loop. (Frank-Setup nutzt 500 ms Delay — passt.)
**Quelle:** https://www.syncfusion.com/forums/195106/

### 4.3 Delayed-Rendering: Clipboard nicht in `WM_RENDERFORMAT` oeffnen
**Symptom:** Verzoegert gerenderte Clipboard-Daten schlagen fehl/haengen.
**Ursache:** Beim `WM_RENDERFORMAT` haelt der Anforderer das Clipboard bereits offen.
**Versionen:** alle — per Design.
**FIX:** Im Handler direkt `SetClipboardData(format, hData)` OHNE `OpenClipboard`. Vor Owner-Zerstoerung `WM_RENDERALLFORMATS`.
**Quelle:** https://devblogs.microsoft.com/oldnewthing/20121224-00/?p=5763

### 4.4 CF_HTML braucht praezisen Offset-Header
**Symptom:** HTML im Clipboard wird von Word/Browsern nicht erkannt/falsch eingefuegt.
**Ursache:** CF_HTML verlangt StartHTML/EndHTML/StartFragment/EndFragment-Offsets in UTF-8-Bytes.
**Versionen:** alle — per Design.
**FIX:** `DataObject.SetData(DataFormats.Html, …)` nutzen (baut den Header); bei manueller Erzeugung Offsets in Bytes (nicht Zeichen).
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/dataxchg/clipboard-operations

---

## 5. Tastatur-Simulation, Hotkeys, Hooks, Foreground

### 5.1 `keybd_event` kommt nicht/falsches Fenster an  ⭐ relevant fuer Voice-Overlays
**Symptom:** Simulierte Tasten kommen nicht oder in der falschen App an.
**Ursache:** Ziel-Fenster hat keinen Fokus; oder Legacy `keybd_event` wird von Spielen/Security ignoriert.
**Versionen:** Win32; `keybd_event` offiziell deprecated.
**FIX:** Vor dem Senden `SetForegroundWindow(targetHwnd)` + kurz warten; `SendInput` statt `keybd_event`.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput

### 5.2 `SendInput`: Scan-Code & haengende Modifier
**Symptom:** Spiele/RDP ignorieren virtuelle Tasten; Modifier (Strg/Shift) bleibt "gedrueckt".
**Ursache:** Virtual-Key statt Hardware-Scan-Code; Zustand wird nicht zurueckgesetzt.
**Versionen:** Win32.
**FIX:** `KEYEVENTF_SCANCODE` + `wScan` via `MapVirtualKey`; vor Senden stuck Modifier per KEYUP loesen; KEYDOWN+KEYUP immer paaren.
**Quelle:** https://www.codeproject.com/Articles/5264831/How-to-Send-Inputs-using-Csharp

### 5.3 `SendInput` tippt falsches Zeichen bei AZERTY/QWERTZ/IME  ⭐
**Symptom:** Simulierte Tasten erzeugen falsche Zeichen bei nicht-QWERTY-Layout oder aktiver IME.
**Ursache:** VK/Scan-Code werden vom aktiven Layout uebersetzt (A↔Q); IME faengt Eingabe ab.
**Versionen:** Win32.
**FIX:** Fuer Text `KEYEVENTF_UNICODE` mit `wScan=Zeichen` (layout-unabhaengig) ODER Clipboard + Strg+V. Fuer echte Tasten `MapVirtualKeyEx` mit Layout-HKL des Ziel-Threads.
**Quelle:** https://rpnfan.github.io/keyboard-heaven/deep-dive/windows-keyboard-chain/

### 5.4 `RegisterHotKey` "already registered" (Fehler 1409)
**Symptom:** `RegisterHotKey` liefert false (`ERROR_HOTKEY_ALREADY_REGISTERED`).
**Ursache:** Andere App (oder eigene bei Reload) hat die Kombi belegt; kein sauberes `UnregisterHotKey`.
**Versionen:** Win32.
**FIX:** Rueckgabe pruefen, Konflikt dem Nutzer melden + Alternativ-Hotkey; beim Beenden IMMER `UnregisterHotKey`. Nicht still scheitern.
**Quelle:** https://sudonull.com/post/150161-Register-global-hotkeys-using-WPF

### 5.5 `SetForegroundWindow` tut nichts (Foreground-Lock)  ⭐ HAEUFIG
**Symptom:** Fenster blinkt nur in der Taskbar statt nach vorn zu kommen.
**Ursache:** Windows verbietet Fokus-Diebstahl, wenn der eigene Prozess nicht den aktuellen Vordergrund haelt.
**Versionen:** Win32, alle.
**FIX:** `AttachThreadInput` an den Vordergrund-Thread + `SetForegroundWindow` + Detach; oder kooperierender Prozess ruft `AllowSetForegroundWindow`. Bei Hotkey-Trigger aus eigenem Prozess meist unnoetig. `SPI_SETFOREGROUNDLOCKTIMEOUT=0` nur Notnagel.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow

> **Zwei konkrete Fallen beim Cross-Window-Paste aus einem Hotkey (verifiziert 2026-06-21, TVO #47062/#47063):**
> Szenario: globaler Hotkey (Strg+Alt+P) wird in Fenster A (Browser) gedrueckt, der eigene Prozess soll
> Zielfenster B (Terminal) nach vorn holen und dort einfuegen. Symptom: B blinkt nur, Paste verpufft.
> 1. **`AttachThreadInput` an den AKTUELLEN VORDERGRUND-Thread heften, NICHT an den Ziel-Thread.** Nur das
>    gerade aktive Fenster (A) besitzt das Foreground-Recht, das man sich leiht. `GetWindowThreadProcessId(GetForegroundWindow())`,
>    nicht `...(targetHwnd)`. An den Ziel-Thread geheftet ist der Trick wirkungslos.
> 2. **Trigger-Modifier VOR dem Fenster-Wechsel freigeben.** Sind Strg/Alt beim `SetForegroundWindow` noch
>    gedrueckt (Finger haelt sie / Stream-Deck-„Hotkey"-Aktion haelt sie), blockiert Windows den Wechsel.
>    Erst synthetische KEYUP fuer Alt/Win/Shift senden, DANN Foreground, DANN Strg+V. Verraeterisches Indiz:
>    eine **Makro-Taste** (Logitech G Hub, mit 50 ms Press/Release-Sequenz) funktioniert, das direkte
>    Tastendruecken / die Stream-Deck-„Hotkey"-Aktion aber nicht — weil das Makro die Modifier sauber vorab loslaesst.

### 5.6 Low-Level-Hook (`WH_KEYBOARD_LL`/`WH_MOUSE_LL`) laggt / wird still entfernt
**Symptom:** Globaler Hook macht das System traege; manchmal hoert er ohne Fehler auf zu feuern.
**Ursache:** Callback laeuft im eigenen UI-Thread; bei Blockade > System-Timeout (1000 ms) entfernt Win den Hook lautlos.
**Versionen:** Win7+; Timeout-Cap seit Win10 1709.
**FIX:** Hook auf eigenem Thread mit Message-Loop; im Callback NICHT blockieren (Arbeit delegieren, sofort `CallNextHookEx`); wo moeglich Raw Input (`WM_INPUT`); periodisch Hook-Lebendigkeit pruefen + re-registrieren.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/winmsg/lowlevelmouseproc

### 5.7 WndProc-/Message-Window fuer Hotkeys/Tray bei Tray-only-Apps
**Symptom:** Globale Hotkeys/Tray-Messages kommen nicht an, wenn die App kein sichtbares Hauptfenster hat.
**Ursache:** Ohne sichtbares Fenster kein HWND mit Message-Loop fuer `RegisterHotKey`/`Shell_NotifyIcon`.
**Versionen:** WPF alle.
**FIX:** Dediziertes verstecktes Message-Window (`HwndSource` 0×0) + `AddHook(WndProc)` — nicht das verborgene WPF-Owner-Fenster missbrauchen.
**Quelle:** https://tyrrrz.me/blog/wndproc-in-wpf

### 5.8 Hotkey mit KeyUp-Debounce-Flag haengt nach langer Operation dauerhaft  ⭐ HAEUFIG bei One-Shot-Hotkeys
**Symptom:** Ein globaler Hotkey, der eine LANGE Aktion ausloest (z.B. Strg+Alt+P = Screenshot ~160 ms + sofortiges Paste ~260 ms), funktioniert anfangs, feuert aber nach einigen Benutzungen GAR NICHT mehr — bis der Prozess neu startet. Ein zweiter Hotkey mit kuerzerer Aktion (z.B. nur Paste) ueberlebt laenger.
**Ursache:** Klassisches Anti-Pattern — ein bool-Debounce-Flag (`_keyDown`) wird beim KeyDown gesetzt und NUR beim KeyUp zurueckgesetzt. Bei der langen Aktion blockiert der UI-Thread und/oder synthetische Tastatureingaben (Strg+V via `keybd_event`/`SendInput`) stoeren die Hook-Verarbeitung, sodass der physische KeyUp verschluckt wird (verwandt mit §5.6 LowLevelHooksTimeout). Das Flag bleibt dauerhaft `true` → die `!_keyDown`-Bedingung ist nie wieder erfuellt → der Hotkey ist „tot". Makro-Tasten (Logitech G Hub, Stream Deck) verschaerfen das, weil sie KeyDown/KeyUp synthetisch und teils unvollstaendig senden.
**Versionen:** per Design, alle (`WH_KEYBOARD_LL`-basierte Hotkeys).
**FIX (funktionserhaltend, Poka-Yoke Stufe 3):** Debounce NICHT vom (unzuverlaessigen) KeyUp abhaengig machen, sondern **zeitbasierter Cooldown** — Trigger nur wenn `DateTime.UtcNow >= _cooldownUntil`, danach `_cooldownUntil = UtcNow.AddMilliseconds(N)` mit N > Operationsdauer. Der Cooldown laeuft IMMER von selbst ab, also kann nichts haengen bleiben. Genau dieses Muster ist fuer Toggle-Hotkeys oft schon vorhanden (`_altF12CooldownUntil`) — auf alle One-Shot-Hotkeys uebertragen. Behoben in TVO 1.4.15 / CVO 2.1.14 (#47060).
**Quelle:** eigener Fix 2026-06-21; verwandt §5.6 (Hook-Timeout), §5.1/§5.5 (Foreground beim Paste — separates Symptom).

---

## 6. Tray / Shell / Fenster-Aktivierung / Single-Instance

### 6.1 Tray-Icon verschwindet nach Explorer-Neustart  ⭐
**Symptom:** NotifyIcon weg nachdem explorer.exe neugestartet ist; App laeuft weiter.
**Ursache:** Taskbar-Neuerstellung loescht alle Tray-Icons.
**Versionen:** alle Windows — per Design.
**FIX:** `RegisterWindowMessage("TaskbarCreated")`, in WndProc darauf lauschen und Icon erneut `Show()`/`Visible=true`.
**Quelle:** https://learn.microsoft.com/en-us/answers/questions/1316178/

### 6.2 Taskbar-Progress/Jump-List weg nach Explorer-Neustart
**Symptom:** Fortschrittsbalken auf dem Taskleisten-Button verschwindet.
**Ursache:** `ITaskbarList3`-Zustand wird bei Taskbar-Neuerstellung verworfen.
**Versionen:** Win7+.
**FIX:** Auf `WM_TASKBARCREATED` lauschen und `SetProgressState/Value` neu setzen; in WPF `TaskbarItemInfo` neu binden.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nf-shobjidl_core-itaskbarlist3-setprogressvalue

### 6.3 Tray-Kontextmenue schliesst nicht beim Klick daneben
**Symptom:** WPF-`ContextMenu` am Tray-Icon bleibt offen, wenn der Nutzer ausserhalb klickt.
**Ursache:** Ohne Vordergrund-Fokus bekommt das Menue kein Deactivate-Event.
**Versionen:** WPF + WinForms NotifyIcon.
**FIX:** Vor dem Oeffnen `SetForegroundWindow(helperHwnd)` (Shell-Trick); oder fertige Lib (H.NotifyIcon).
**Quelle:** https://social.msdn.microsoft.com/Forums/exchange/en-US/7b169558-48b2-4bc9-9993-6327aca7cbe8/

### 6.4 Single-Instance-Mutex vom GC freigegeben / Fenster nicht nach vorn
**Symptom:** Zweite Instanz startet trotzdem; oder zweiter Start aktiviert das vorhandene Fenster nicht.
**Ursache:** Lokale Mutex-Variable wird wegoptimiert/GC-gesammelt; fehlende IPC.
**Versionen:** .NET alle.
**FIX:** Mutex `static` halten (oder `GC.KeepAlive`); global eindeutiger Name (`Global\<GUID>`); via Named Pipe/`WM_COPYDATA` erste Instanz benachrichtigen → `ShowWindow(SW_RESTORE)`+`SetForegroundWindow`.
**Quelle:** https://www.codestudy.net/blog/correct-net-way-to-implement-a-single-instance-application/

### 6.5 Toast (unpackaged) braucht Startmenue-Shortcut mit AUMID
**Symptom:** `ToastNotificationManager` zeigt bei unpackaged Win32/.NET-App keinen Toast.
**Ursache:** Ohne Startmenue-Shortcut mit AppUserModelID (+ CLSID) verweigert Windows den Toast.
**Versionen:** Windows 10/11.
**FIX:** Beim ersten Start Shortcut mit AUMID + CLSID anlegen; `CreateToastNotifier(aumid)` mit derselben AUMID. Einfacher: `CommunityToolkit`-Helper.
**Quelle:** https://learn.microsoft.com/en-us/windows/apps/develop/notifications/app-notifications/send-local-toast-other-apps

### 6.6 WPF: Taskleisten-Icon der LAUFENDEN App generisch, obwohl Vorschau + angepinntes Symbol korrekt  ⭐ HAEUFIG
**Symptom:** Bei geoeffneter WPF-App zeigt der Taskleisten-Button das generische Windows-Default-Symbol. Die Fenster-Vorschau (Thumbnail-Titel) UND das angepinnte/geschlossene Symbol zeigen dagegen das richtige App-Icon. `Window.Icon` UND `<ApplicationIcon>` sind korrekt gesetzt, die `.ico` enthaelt alle Groessen (16/32/48/64/128/256).
**Ursache:** Windows nutzt zwei getrennte Icon-Slots: `ICON_SMALL` (16px — Titelleiste + Thumbnail) und `ICON_BIG` (32px — Taskleisten-Button). WPFs `Window.Icon` belegt den ICON_SMALL-Slot zuverlaessig, den ICON_BIG-Slot aber NICHT immer — dann faellt die Taskleiste auf das Default-Symbol zurueck. Erklaert exakt, warum Vorschau (SMALL) stimmt, Taskleiste (BIG) aber generisch ist.
**Versionen:** WPF auf .NET (beobachtet .NET 10, single-file self-contained) — 2026-06-08, Frank-Vorfall (VoiceAgent).
**FIX (funktionserhaltend):** Im `SourceInitialized` (HWND existiert) beide Icons explizit via `WM_SETICON` setzen. Robust + ohne Zusatzpaket: `ExtractIconEx(Environment.ProcessPath, 0, out hBig, out hSmall, 1)` laedt genau das eingebettete Exe-Icon (das angepinnt bereits stimmt). `Environment.ProcessPath` ist single-file-sicher (§1.1). `Window.Icon` im XAML als Fallback/Design-Time belassen. Selbst gesetzte HICONs am Ende per `DestroyIcon` freigeben (§9.4). NICHT mit AppUserModelID/Grouping verwechseln: ein AUMID-Mismatch erzeugt einen SEPARATEN Button mit dem (korrekten) Window-Icon — nicht das generische Symbol.
**Quelle:** Win32 WM_SETICON/ExtractIconEx (learn.microsoft.com) + Frank-Vorfall 2026-06-08 (VoiceAgent #46616).

### 6.7 `UseWindowsForms=true` + `UseWPF=true` + ImplicitUsings → `Color`/`MessageBox`/`Application` mehrdeutig (CS0104)  ⭐ WICHTIG
**Symptom:** Sobald man fuer ein Tray-Icon (`System.Windows.Forms.NotifyIcon`) `<UseWindowsForms>true</>` zu einem WPF-Projekt mit `<ImplicitUsings>enable</>` hinzufuegt, brechen ploetzlich bestehende WPF-Dateien mit CS0104: „`Color` ist eine mehrdeutige Referenz zwischen `System.Windows.Media.Color` und `System.Drawing.Color`" (gleiches fuer `MessageBox`, `Application`, `Point`, `Size`).
**Ursache:** Das Windows-Desktop-SDK fuegt bei `UseWindowsForms=true` + ImplicitUsings die GLOBALEN usings `System.Drawing` UND `System.Windows.Forms` hinzu. Die kollidieren mit den WPF-Namespaces, die die bestehenden Dateien per `using System.Windows;`/`System.Windows.Media;` ziehen. `UseWPF` fuegt selbst KEINE impliziten usings hinzu — daher fiel es vorher nicht auf.
**Versionen:** .NET 6–10, WPF+WinForms im selben Projekt mit ImplicitUsings — per Design.
**FIX (funktionserhaltend):** Die zwei globalen usings im csproj entfernen und nur lokal (file-scoped) im Tray-Code ziehen:
```xml
<ItemGroup>
  <Using Remove="System.Drawing" />
  <Using Remove="System.Windows.Forms" />
</ItemGroup>
```
Alternativ einzelne Stellen voll qualifizieren (`System.Windows.MessageBox.Show`) — aber `<Using Remove>` ist sauberer (eine Stelle, alle WPF-Dateien bleiben unveraendert). NotifyIcon bleibt voll nutzbar (lokales `using System.Windows.Forms;` in der Tray-Datei). WinForms-NotifyIcon ist dem eigenen `Shell_NotifyIcon`-Nachbau vorzuziehen: es re-registriert nach Explorer-Neustart selbst (§6.1) und liefert ein funktionierendes Tray-Kontextmenue (§6.3). Begleit-Warnung `WFO0003` (High-DPI-Manifest) ist bei reiner WPF-App mit nur Tray-Icon irrelevant (Manifest fuer WPF `PerMonitorV2` behalten, §3.1).
**Quelle:** eigener Vorfall 2026-06-08 (VoiceAgent #46622: Minimieren-ins-Tray) · https://learn.microsoft.com/en-us/dotnet/core/project-sdk/overview#implicit-using-directives

---

## 7. async/await, Threading, Dispatcher

### 7.1 Deadlock durch `.Result`/`.Wait()` auf UI-Thread  ⭐ KRITISCH
**Symptom:** App haengt komplett ein, wenn ein Event-Handler `someAsync().Result`/`.Wait()` aufruft.
**Ursache:** UI-`SynchronizationContext` blockiert und wartet auf die Continuation, die genau diesen Thread braucht.
**Versionen:** alle mit SyncContext (WPF/WinForms) — per Design.
**FIX:** "async all the way" — durchgehend `await`; in Library-Code `ConfigureAwait(false)`. Falls synchron unvermeidbar: `Task.Run(() => asyncMethod()).GetAwaiter().GetResult()`.
**Quelle:** https://blog.stephencleary.com/2012/07/dont-block-on-async-code.html

### 7.2 `async void` schluckt Exceptions  ⭐
**Symptom:** Fehler in async-Methode crasht die App oder verschwindet, kein `catch` greift.
**Ursache:** `async void` gibt kein beobachtbares Task zurueck; Exception landet auf dem SyncContext.
**Versionen:** alle — per Design.
**FIX:** `async Task` statt `async void`. Ausnahme Event-Handler: ganzer Body in `try/catch`. Async-Lambda nie an `Action` (wird `async void`) — `Func<Task>` nutzen.
**Quelle:** https://blog.elmah.io/demystifying-async-void-in-c-why-its-dangerous-and-when-its-okay/

### 7.3 Fehlendes `ConfigureAwait(false)` in Bibliotheks-Code
**Symptom:** Unnoetige Rueckkehr auf UI-Kontext; mit `.Result` Deadlock-Verstaerker.
**Ursache:** Ohne `ConfigureAwait(false)` springt jede Continuation auf den Originalkontext.
**Versionen:** per Design (Desktop hat SyncContext, ASP.NET Core nicht).
**FIX:** In Service-/Library-Code (HTTP zu Whisper/Gemini, Audio-Pipeline) durchgehend `.ConfigureAwait(false)`; in direktem UI-Event-Code weglassen.
**Quelle:** https://devblogs.microsoft.com/dotnet/configureawait-faq/

### 7.4 Fire-and-forget ohne Fehlerbehandlung
**Symptom:** Hintergrund-Task wirft Exception → kein Log, evtl. App-Crash.
**Ursache:** Nicht-awaiteter Task ohne Exception-Observer.
**Versionen:** per Design.
**FIX:** `SafeFireAndForget` (NuGet AsyncAwaitBestPractices) mit `onException`; oder `_ = task.ContinueWith(t => Log(t.Exception), OnlyOnFaulted)`.
**Quelle:** https://okyrylchuk.dev/blog/mastering-async-and-await-in-csharp-best-practices/

### 7.5 `CancellationToken` nicht durchgereicht / `Task.Run`-Token cancelt nur Scheduling
**Symptom:** Abbrechen hat keinen Effekt; Operation laeuft weiter.
**Ursache:** Token nicht an innere async-Methoden weitergegeben; `Task.Run(delegate, token)` bricht nur die Planung ab, nicht den laufenden Delegate.
**Versionen:** per Design.
**FIX:** Token an JEDE token-akzeptierende Methode propagieren; INNERHALB des Delegate `token.ThrowIfCancellationRequested()`; bei Abbruch `OperationCanceledException` werfen (nicht still `return`).
**Quelle:** https://devblogs.microsoft.com/premier-developer/recommended-patterns-for-cancellationtoken/

### 7.6 `Dispatcher.Invoke()` blockiert / Deadlock aus Parallel-Kontext
**Symptom:** `Parallel.For`/Background-Thread haengt bei synchronem `Dispatcher.Invoke`.
**Ursache:** Synchrones Invoke wartet auf belegten UI-Thread.
**Versionen:** .NET 7+ (dotnet/wpf#8066 **OPEN**).
**FIX:** `Dispatcher.BeginInvoke`/`InvokeAsync` statt `Invoke`; UI-Updates sammeln und gebuendelt posten; `Application.Current.Dispatcher` nutzen.
**Quelle:** dotnet/wpf#8066

### 7.7 `ValueTask` mehrfach awaiten/cachen
**Symptom:** Undefiniertes Verhalten/Crash bei mehrfachem Await derselben `ValueTask`.
**Ursache:** `ValueTask` ist nicht wiederverwendbar (IValueTaskSource kann recycelt sein).
**Versionen:** ab .NET Core 2.1 — per Design.
**FIX:** Genau einmal awaiten ODER sofort `.AsTask()`; Ergebnis in lokale Variable.
**Quelle:** https://blog.marcgravell.com/2019/08/prefer-valuetask-to-task-always-and.html

### 7.8 `SemaphoreSlim.Release()` nicht in `finally`; `lock` um `await`
**Symptom:** Deadlock oder Semaphor-Count leer, wenn zwischen `WaitAsync` und `Release` eine Exception fliegt.
**Ursache:** Release wird bei Exception uebersprungen; `lock`-Statement erlaubt kein `await`; SemaphoreSlim nicht reentrant.
**Versionen:** per Design.
**FIX:** `await sem.WaitAsync(token); try { … } finally { sem.Release(); }`. Fuer async-Mutex `SemaphoreSlim(1,1)`; nie `lock` um `await`.
**Quelle:** https://github.com/dotnet/runtime/issues/71706

### 7.9 Channels: `Writer.Complete()` vergessen / unbounded → OOM
**Symptom:** `ReadAllAsync()` beendet nie; oder Speicher waechst unbegrenzt unter Last.
**Ursache:** Ohne `Complete()` weiss der Consumer nicht, dass Schluss ist; unbounded Channel hat keinen Backpressure.
**Versionen:** ab .NET Core 3.0.
**FIX:** Producer ruft im `finally` `Writer.Complete()`; `Channel.CreateBounded(capacity)` fuer Backpressure (z.B. Audio-Frames).
**Quelle:** https://devblogs.microsoft.com/dotnet/an-introduction-to-system-threading-channels/

### 7.10 `lock(this)`/`lock(typeof(X))`/`lock("literal")` Anti-Pattern
**Symptom:** Sporadische Deadlocks, scheinbar von fremdem Code.
**Ursache:** `this`, `Type`-Objekte und interned Strings sind global sichtbar — fremder Code kann darauf locken.
**Versionen:** per Design.
**FIX:** Dediziertes `private readonly object _lock = new();` pro Ressource. (.NET 9/C# 13: `System.Threading.Lock`.)
**Quelle:** https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/statements/lock

### 7.11 `ThreadLocal`/`[ThreadStatic]` ueber `await`-Grenzen
**Symptom:** Erwarteter thread-lokaler Wert ist nach `await` weg/falsch.
**Ursache:** Continuation kann auf anderem ThreadPool-Thread laufen; ThreadLocal folgt nicht dem Async-Fluss.
**Versionen:** per Design.
**FIX:** `AsyncLocal<T>` statt `ThreadLocal<T>` fuer async-Kontextdaten.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.threading.asynclocal-1

### 7.12 `await foreach` ohne `.WithCancellation()`/`.ConfigureAwait()`
**Symptom:** Async-Stream (z.B. gestreamte Gemini-Tokens) nicht abbrechbar / springt auf UI-Kontext.
**Ursache:** Token kann bei fremden `IAsyncEnumerable` nicht als Parameter durchgereicht werden.
**Versionen:** ab C# 8.
**FIX:** `await foreach (var x in source.WithCancellation(token).ConfigureAwait(false))`; eigene Iteratoren mit `[EnumeratorCancellation]`.
**Quelle:** https://learn.microsoft.com/en-us/archive/msdn-magazine/2019/november/csharp-iterating-with-async-enumerables-in-csharp-8

### 7.13 `DoEvents`/`Dispatcher.PushFrame` — Reentrancy
**Symptom:** Doppelte Handler, schwer reproduzierbare Crashes bei genesteten Message-Pumps.
**Ursache:** DoEvents/PushFrame oeffnen eine genestete Loop, die Eingabe-/Timer-Messages reentrant ausliefert (auch ShowDialog/Invoke).
**Versionen:** alle WPF/WinForms — per Design.
**FIX:** `DoEvents` meiden; `async`/`await` + `await Dispatcher.Yield()`; falls unvermeidbar Reentrancy-Guard-Flag.
**Quelle:** https://getandplay.github.io/2019/05/21/Learn-more-about-how-WPF-Dispatcher-works-PushFrame/

---

## 8. HttpClient / Networking

### 8.1 `new HttpClient()` pro Request → Socket-Exhaustion  ⭐ relevant (Whisper/Gemini-Calls)
**Symptom:** Nach vielen API-Calls `SocketException`, Ports erschoepft.
**Ursache:** Jeder disposte HttpClient laesst Sockets im TIME_WAIT.
**Versionen:** alle — per Design.
**FIX:** EIN statischer langlebiger `HttpClient` oder `IHttpClientFactory`. ABER siehe 8.2.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines

### 8.2 Singleton-HttpClient → stale DNS
**Symptom:** Nach DNS-Wechsel (Endpoint-Failover) erreicht der Client weiter die alte IP.
**Ursache:** HttpClient loest DNS nur beim Verbindungsaufbau auf; ewig gehaltener Client refreshed nie.
**Versionen:** per Design (`SocketsHttpHandler` ab .NET Core 2.1).
**FIX:** `SocketsHttpHandler.PooledConnectionLifetime = TimeSpan.FromMinutes(2-5)` → periodische Verbindungserneuerung.
**Quelle:** https://www.meziantou.net/avoid-dns-issues-with-httpclient-in-dotnet.htm

---

## 9. Ressourcen, Leaks, IDisposable, Audio

### 9.1 Event-Handler-Leak: `+=` ohne `-=`  ⭐
**Symptom:** Subscriber (z.B. ViewModels an langlebigem Audio-Service) werden nie GC-collected; Speicher waechst.
**Ursache:** Publisher haelt starke Referenz auf den Subscriber-Delegate.
**Versionen:** per Design.
**FIX:** Im `Dispose`/`Unloaded` immer `-=`; bei langlebigem Publisher `WeakEventManager`.
**Quelle:** https://michaelscodingspot.com/5-techniques-to-avoid-memory-leaks-by-events-in-c-net-you-should-know/

### 9.2 `DispatcherTimer`-Leak
**Symptom:** Objekte werden nie freigegeben, obwohl Control/Window geschlossen.
**Ursache:** `DispatcherTimer` haelt starke Referenz auf den Tick-Handler; der globale Dispatcher haelt den Timer.
**Versionen:** alle WPF inkl. .NET 8.
**FIX:** Timer `Stop()` + Handler `-=` in `Unloaded`/`Closed`/`Dispose`.
**Quelle:** https://github.com/ScottPlot/ScottPlot/issues/1115

### 9.3 WPF Binding-Leak ohne DependencyProperty/INPC
**Symptom:** Objekte nie GC-collected, gehalten ueber `PropertyDescriptor.ValueChanged`.
**Ursache:** WPF abonniert ValueChanged, wenn Ziel kein DP ist und INotifyPropertyChanged fehlt.
**Versionen:** alle WPF (dotnet/wpf#5739 CLOSED COMPLETED fuer einen Spezialfall; Muster bleibt).
**FIX:** an DependencyProperty binden oder INPC implementieren; `BindingMode=OneTime` wo statisch; Transform-Bindings beim Entfernen `ClearValue`.
**Quelle:** dotnet/wpf#5739

### 9.4 GDI-Handle-Leak — Limit 10.000
**Symptom:** Nach langer Laufzeit `Win32Exception: Error creating window handle` / schwarze Grafik.
**Ursache:** `Bitmap`/`Icon`/`Graphics`/`Brush`/`Font` nicht disposed; per-Prozess-GDI-Limit 10.000.
**Versionen:** alle — per Design.
**FIX:** Alle GDI-Objekte in `using`/`Dispose()`; `GetIconInfo`-Bitmaps freigeben; mit "GDI Objects"-Counter ueberwachen.
**Quelle:** https://devblogs.microsoft.com/oldnewthing/20170519-00/?p=96195

### 9.5 NAudio/WASAPI: Crash beim Geraete-Wechsel/Abziehen  ⭐ relevant (Mikrofon)
**Symptom:** App crasht (`COMException`) wenn das Audio-Geraet (USB-Headset) abgezogen oder Default gewechselt wird.
**Ursache:** `WasapiCapture`/`WasapiOut` halten ein totes `MMDevice`-Handle; `Dispose()` kann beim getrennten Default-Device blockieren.
**Versionen:** NAudio (#772, #747).
**FIX:** `MMDeviceEnumerator.RegisterEndpointNotificationCallback`; bei `OnDefaultDeviceChanged`/`OnDeviceStateChanged` Capture/Out sauber neu initialisieren (try-catch, ersten "Active"-Event ueberspringen).
**Quelle:** https://github.com/naudio/NAudio/issues/772

### 9.6 Eigener Finalizer statt `SafeHandle`; `GC.SuppressFinalize` vergessen
**Symptom:** Unmanaged Ressourcen spaet/nie frei; Objekte ueberleben unnoetig eine GC-Generation.
**Ursache:** Eigener Finalizer ist teuer/fehleranfaellig; ohne `GC.SuppressFinalize(this)` in `Dispose()` laeuft der Finalizer trotz Dispose.
**Versionen:** per Design.
**FIX:** Unmanaged Handles in `SafeHandle` kapseln; Standard-Dispose-Pattern mit `GC.SuppressFinalize(this)`. CA1816 aktivieren.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/fundamentals/code-analysis/quality-rules/ca1816

### 9.7 `FileSystemWatcher` stoppt nach Buffer-Overflow
**Symptom:** Nach `InternalBufferOverflowException` liefert der Watcher keine Events mehr.
**Ursache:** Zu viele Aenderungen in kurzer Zeit ueberlaufen den internen Puffer.
**Versionen:** alle (dotnet/runtime#81226 CLOSED COMPLETED).
**FIX:** `InternalBufferSize` erhoehen, `NotifyFilter` eng setzen, `Error`-Event abonnieren und Watcher dort neu starten.
**Quelle:** dotnet/runtime#81226

---

## 10. System.Text.Json / Serialisierung

### 10.1 camelCase nicht konfiguriert → Properties bleiben null  ⭐ (Whisper/Gemini liefern camelCase)
**Symptom:** Deserialisierung von API-JSON ergibt Default/null.
**Ursache:** STJ matcht standardmaessig case-sensitive.
**Versionen:** per Design.
**FIX:** `JsonSerializerOptions { PropertyNamingPolicy = CamelCase, PropertyNameCaseInsensitive = true }`. Options als statische Instanz wiederverwenden (nicht pro Call neu).
**Quelle:** https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/character-casing

### 10.2 STJ Source-Gen faellt nicht auf Reflection zurueck → NotSupportedException
**Symptom:** `JsonSerializer.Serialize(obj, Context.Default.Options)` wirft fuer nicht-registrierte Typen / eigene IEnumerable.
**Ursache:** ab .NET 7 kein impliziter Reflection-Fallback bei source-gen Options.
**Versionen:** .NET 7+ (dotnet/runtime#71714 CLOSED COMPLETED, dokumentiert).
**FIX:** `TypeInfoResolver = JsonTypeInfoResolver.Combine(MyContext.Default, new DefaultJsonTypeInfoResolver())`; oder alle Typen registrieren; konkrete `List<T>`/Array.
**Quelle:** dotnet/runtime#71714

### 10.3 Polymorphie wirft ohne `[JsonDerivedType]`
**Symptom:** Serialisieren von Basisklassen-Referenzen verliert abgeleitete Properties/wirft.
**Ursache:** STJ serialisiert nur deklarierte Typen.
**Versionen:** eingebauter Support ab .NET 8.
**FIX:** `[JsonPolymorphic]` + `[JsonDerivedType(typeof(X), "x")]`; Discriminator exakt (case-sensitive).
**Quelle:** https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/polymorphism

### 10.4 `BinaryFormatter` wirft zur Laufzeit
**Symptom:** `BinaryFormatter.Serialize/Deserialize` werfen `NotSupportedException` zur Laufzeit.
**Ursache:** Obsoletion wegen Sicherheitsmaengeln. (WPF/WinForms ausgenommen — dort nur Compile-Obsolete.)
**Versionen:** ab .NET 8 — per Design.
**FIX:** auf `System.Text.Json`/`DataContractSerializer`/MessagePack migrieren. Compat-Switch `EnableUnsafeBinaryFormatterSerialization` nur als Notnagel.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/serialization/8.0/binaryformatter-disabled

---

## 11. C#-Sprache & Laufzeit-Mechanik

### 11.1 Null-forgiving `!` hat keine Runtime-Garantie
**Symptom:** `NullReferenceException` trotz nicht-nullbarem Typ.
**Ursache:** `!` beeinflusst nur die Compiler-Flow-Analyse; NRT ist Compile-Time-only.
**Versionen:** ab C# 8; in .NET 8 neue Projekte standardmaessig aktiviert.
**FIX:** `!` sparsam; echte Null-Checks; bei Reflection/Deserialisierung/Interop mit null rechnen.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/operators/null-forgiving

### 11.2 NRT-Felder still null bei Arrays / nicht initialisiert
**Symptom:** Non-nullable Referenz-Feld ist null, keine Warnung.
**Ursache:** Array-Elemente/Struct-Felder starten als `default` (null); Flow-Analyse deckt nur lokale Variablen/Parameter ab.
**Versionen:** per Design.
**FIX:** Felder im Konstruktor initialisieren, `required` (C# 11), Arrays nach Befuellung pruefen.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/csharp/nullable-references

### 11.3 Struct-Mutation in Collection / `foreach` trifft Kopie
**Symptom:** Aenderung an `list[i].Property` oder `foreach`-Variable "verschwindet".
**Ursache:** Value-Type-Semantik: Indexer/foreach geben eine KOPIE; bei `readonly`-Feldern defensive Kopien.
**Versionen:** per Design; `readonly struct` ab C# 7.2.
**FIX:** Struct ersetzen (`list[i] = new …`), `CollectionsMarshal.AsSpan(list)[i].X = …` fuer In-Place, oder Klasse; `readonly struct` deklarieren.
**Quelle:** https://devblogs.microsoft.com/premier-developer/the-in-modifier-and-the-readonly-structs-in-c/

### 11.4 record-Value-Equality bricht bei Collection-Property
**Symptom:** Zwei records mit gleichem Listen-Inhalt sind `!Equals`.
**Ursache:** Auto-`Equals` nutzt `EqualityComparer<T>.Default` pro Property; Collections haben Reference-Equality.
**Versionen:** ab C# 9.
**FIX:** `Equals`/`GetHashCode` fuer das Collection-Member mit `SequenceEqual` ueberschreiben; oder value-equality-faehigen Typ.
**Quelle:** https://github.com/dotnet/csharplang/discussions/4845

### 11.5 `double`/`float` fuer Geldbetraege
**Symptom:** Rundungsfehler (0.1 + 0.2 ≠ 0.3).
**Ursache:** Base-2-Brueche, `0.1` binaer nicht exakt.
**Versionen:** per Design.
**FIX:** Fuer Geld immer `decimal`; nicht mit `double` mischen; `Math.Round` mit fester Stelle.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/builtin-types/floating-point-numeric-types

### 11.6 Inkonsistente String-Vergleichs-Defaults (Turkish-i)
**Symptom:** `Contains`/`StartsWith`/`ToUpper` liefern je nach Locale falsche Ergebnisse; `"i".ToUpper()` in tr-TR = `"İ"`.
**Ursache:** `Equals`/`==` sind ordinal, aber `Contains`/`StartsWith`/`IndexOf` (ohne Overload) und `ToUpper`/`ToLower` kultur-sensitiv.
**Versionen:** per Design.
**FIX:** Explizit `StringComparison.Ordinal`/`OrdinalIgnoreCase`; `ToUpperInvariant`; `string.Equals(a,b,OrdinalIgnoreCase)` statt `ToLower`-Vergleich.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/standard/base-types/best-practices-strings

### 11.7 `Enum.Parse`/Cast akzeptiert undefinierte Werte
**Symptom:** Ungueltiger Enum-Wert (aus JSON/API) wird akzeptiert; `switch` faellt durch.
**Ursache:** Enums sind Integer-Wrapper; jeder int ist zuweisbar.
**Versionen:** per Design.
**FIX:** Nach `Parse`/`TryParse` zusaetzlich `Enum.IsDefined` pruefen; bei `[Flags]` Bitmaske pruefen.
**Quelle:** https://gaevoy.com/2021/12/01/parsing-enum-pitfalls.html

### 11.8 `Span<T>`/`stackalloc` in async/Feldern/returned
**Symptom:** Compile-Fehler, oder Stack-Korruption/`StackOverflowException`.
**Ursache:** `Span<T>` ist ref struct (nicht ueber `await`/`yield`, nicht in Feldern, nicht geboxt); `stackalloc`-Speicher wird beim Methoden-Exit ungueltig; grosse `stackalloc` sprengen den Stack.
**Versionen:** ab C# 7.2.
**FIX:** Fuer async-Grenzen `Memory<T>`; grosse Buffer via `ArrayPool<T>.Shared`; `stackalloc` nur klein/kurzlebig; nie Span aus `stackalloc` returnen.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/operators/stackalloc

### 11.9 Nicht-exhaustive `switch`-Expression → Laufzeit-Exception
**Symptom:** `SwitchExpressionException` zur Laufzeit (z.B. nach Hinzufuegen eines Enum-Werts).
**Ursache:** Compiler warnt nur (CS8509); fehlender Arm wirft zur Laufzeit.
**Versionen:** ab C# 8.
**FIX:** CS8509 als Error behandeln; oder `_ => throw new ArgumentOutOfRangeException(...)` als letzten Arm.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/csharp/language-reference/operators/switch-expression

### 11.10 Veraenderliches Feld als Dictionary-/HashSet-Key
**Symptom:** Eintrag nach Aenderung eines Key-Felds nicht mehr auffindbar.
**Ursache:** Aenderung eines in `GetHashCode`/`Equals` einfliessenden Felds veraendert den Bucket.
**Versionen:** per Design.
**FIX:** Nur unveraenderliche Werte als Key (`record`/`readonly`); Key-Objekte nach Einfuegen nie mutieren.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.object.gethashcode

### 11.11 Mehrfache Enumeration eines `IEnumerable`
**Symptom:** Teure Query (DB/HTTP) laeuft mehrmals; Side-Effects doppelt.
**Ursache:** Deferred Execution — jede Iteration fuehrt die Query erneut aus.
**Versionen:** per Design.
**FIX:** Frueh `.ToList()`/`.ToArray()` materialisieren; Analyzer CA1851 aktivieren.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/fundamentals/code-analysis/quality-rules/ca1851

### 11.12 `DateTime.Parse` nutzt CurrentCulture; `DateTime` statt `DateTimeOffset`
**Symptom:** Datums-Parsing crasht/liefert falschen Tag je nach Locale; Zeitzonen-/DST-Fehler bei API-Timestamps.
**Ursache:** `Parse`/`ToString` ohne Provider nutzen Thread-Kultur; `DateTime` traegt keinen UTC-Offset.
**Versionen:** per Design (.NET 8: `TwoDigitYearMax`=2049, TypeConverter beachtet Culture).
**FIX:** Bei Maschinen-/API-Daten `CultureInfo.InvariantCulture` + `ParseExact`/`TryParseExact`; fuer absolute Zeitpunkte `DateTimeOffset`.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/standard/base-types/parsing-datetime

### 11.13 `ConcurrentDictionary.GetOrAdd` ruft valueFactory mehrfach
**Symptom:** Teure Factory (z.B. Token-Abruf) laeuft bei Parallelzugriff mehrmals.
**Ursache:** `GetOrAdd` sperrt nicht waehrend der valueFactory (nur das Einfuegen ist atomar).
**Versionen:** per Design (dotnet/runtime#33221 CLOSED, dokumentiert).
**FIX:** Wert in `Lazy<T>` wrappen: `dict.GetOrAdd(key, k => new Lazy<T>(() => Expensive())).Value`.
**Quelle:** dotnet/runtime#33221

### 11.14 `Dictionary<,>` aus mehreren Threads → Korruption
**Symptom:** Sporadische `InvalidOperationException`, falsche Werte, Endlosschleifen.
**Ursache:** `Dictionary` ist nicht thread-safe.
**Versionen:** per Design.
**FIX:** `ConcurrentDictionary` (mit 11.13 beachten); oder Lock um alle Zugriffe.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/api/system.collections.concurrent.concurrentdictionary-2

---

## 12. WinUI 3 / Windows App SDK

### 12.1 WinUI 3 unpackaged Single-File startet nicht  ⭐ (PromptBoard relevant)
**Symptom:** Als Single-File publizierte unpackaged WinUI-3-App startet nicht (`Microsoft.WindowsAppRuntime.Bootstrap.dll` nicht ladbar; `resources.pri` separat).
**Ursache:** WindowsAppSDK ist nicht single-file-faehig.
**Versionen:** WASDK 1.x, .NET 8/9 (microsoft-ui-xaml#10173 **OPEN**, msbuild#7587).
**FIX:** Single-File fuer WinUI 3 NICHT verwenden; normal self-contained (mehrere Dateien), explizites RID (`win-x64`, kein AnyCPU); `resources.pri`/Bootstrap bleiben separat.
**Quelle:** microsoft/microsoft-ui-xaml#10173

### 12.2 Bootstrapper unpackaged: "Access is denied" / Runtime nicht gefunden
**Symptom:** `Bootstrap.Initialize`/`MddBootstrapInitialize` schlaegt fehl (0x80070005 bzw. "Package dependency criteria could not be resolved").
**Ursache:** Regression beim Runtime-Upgrade (1.1.3→1.1.4) / falsche minVersion / Runtime nicht installiert.
**Versionen:** WASDK 1.1.4+/1.5+ (#2918, #5530 CLOSED COMPLETED — in neuerer WASDK gefixt).
**FIX:** WASDK-Version pinnen, neueste 1.x/2.0; korrekte `majorMinorVersion`/`versionTag`; Runtime-Installer mitliefern.
**Quelle:** microsoft/WindowsAppSDK#2918 · #5530

### 12.3 `Window.Activate()` holt Background-Window nicht nach vorn
**Symptom:** `Activate()` aktiviert ein im Hintergrund liegendes WinUI-3-Window nicht.
**Ursache:** WinUI-3 `Activate` ruft kein echtes `SetForegroundWindow`; OS-Foreground-Restriktion.
**Versionen:** WinUI 3 (microsoft-ui-xaml#7595 **OPEN/REOPENED**).
**FIX:** `WindowNative.GetWindowHandle` + `ShowWindow(SW_RESTORE)` + `SetForegroundWindow`.
**Quelle:** microsoft/microsoft-ui-xaml#7595

### 12.4 `DispatcherQueue` null / `COMException` wrong thread
**Symptom:** `DispatcherQueue.GetForCurrentThread()` null im Worker-Thread; UI-Update aus falschem Thread wirft `COMException` (RPC_E_WRONG_THREAD).
**Ursache:** Nur der UI-Thread hat eine DispatcherQueue; WinUI-3-UI-Objekte sind streng thread-affin (kein Auto-Marshalling).
**Versionen:** WinUI 3 (microsoft-ui-xaml#4219 CLOSED COMPLETED; #8410).
**FIX:** Gecachte UI-`DispatcherQueue.TryEnqueue(...)`; vorher `HasThreadAccess` pruefen.
**Quelle:** microsoft/microsoft-ui-xaml#4219

### 12.5 Nur EIN `ContentDialog` pro Thread; Window-Close mit offenem Dialog/Popup crasht
**Symptom:** Zweiter ContentDialog wirft `COMException`; Window-Close mit offenem Dialog/ComboBox-Dropdown/WebView2 crasht.
**Ursache:** WinUI 3 erlaubt nur einen offenen ContentDialog/Thread; Popups/XamlRoot werden beim Close ungueltig.
**Versionen:** WASDK 1.x (#4661, #8913 CLOSED COMPLETED, #8605).
**FIX:** Globalen "Dialog offen"-Guard; vor `Close()`/neuem Dialog offene Dialoge `Hide()`, Popups/Flyouts schliessen, WebView2 `Close()`.
**Quelle:** microsoft/microsoft-ui-xaml#4661 · #8913

### 12.6 WinUI-3-Desktop crasht in Release-Config
**Symptom:** Leeres packaged WinUI-3-Projekt crasht nur in Release (Debug laeuft).
**Ursache:** Trimming/Optimierung entfernt benoetigte XAML-Metadaten.
**Versionen:** WASDK 1.4 (microsoft-ui-xaml#9675 CLOSED COMPLETED).
**FIX:** `PublishTrimmed=false` fuer WinUI 3; Release ohne aggressive Trim-Settings.
**Quelle:** microsoft/microsoft-ui-xaml#9675

### 12.7 WinUI 3: kein `AllowsTransparency`, kein System-Tray, kein echtes Click-through
**Symptom:** Transparentes durchklickbares Overlay / NotifyIcon / Topmost wie in WPF nicht baubar.
**Ursache:** WinUI 3 rendert per Composition/Direct3D; kennt keine Per-Pixel-Hit-Test-Durchlaessigkeit, kapselt kein Win32-Fenster-API.
**Versionen:** WinUI 3 alle — per Design.
**FIX:** Fuer Overlay-Apps bewusst **WPF statt WinUI 3** waehlen. Falls WinUI: HWND via `WindowNative.GetWindowHandle` + `WS_EX_TRANSPARENT|WS_EX_LAYERED` (ganzes Fenster); Tray per `Shell_NotifyIcon`/Lib (H.NotifyIcon); Topmost per `AppWindow`/`SetWindowPos`; Backdrop nur Mica/Acrylic.
**Quelle:** https://learn.microsoft.com/en-us/answers/questions/1418063/ · microsoft/microsoft-ui-xaml#10746

### 12.8 Mica-Backdrop crasht (beim Monitor-Wechsel / generell)
**Symptom:** WinUI-3-App crasht beim Setzen von Mica oder beim Ziehen zwischen Monitoren.
**Ursache:** Backdrop-Controller-Init/Disposal-Race bzw. DPI-Uebergangs-Bug.
**Versionen:** WASDK 1.x (#7079 CLOSED COMPLETED, #8495).
**FIX:** `MicaController.IsSupported()` pruefen; Backdrop erst nach Window-Activation setzen; Controller sauber disposen; neueste WASDK.
**Quelle:** microsoft/microsoft-ui-xaml#7079

### 12.9 WinUI 3 globaler Hotkey: kein `System.Windows.Interop`
**Symptom:** `RegisterHotKey` nicht direkt nutzbar (kein WPF-`HwndSource`).
**Ursache:** WinUI 3 hat keinen WPF-Interop-Namespace.
**Versionen:** WASDK alle.
**FIX:** `WinRT.Interop.WindowNative.GetWindowHandle` + `SetWindowSubclass`-SubclassProc fuer `WM_HOTKEY`.
**Quelle:** microsoft/WindowsAppSDK#941

---

## 13. .NET 8 Breaking Changes & SDK/Build (Publish-Falle)

### 13.1 `-r RID` / `--arch` impliziert NICHT mehr self-contained  ⭐ HAEUFIG
**Symptom:** `dotnet publish -r win-x64` (oder `--arch x64`) liefert framework-dependent statt self-contained.
**Ursache:** Default geaendert.
**Versionen:** ab .NET 8 — per Design.
**FIX:** Explizit `--self-contained true` bzw. `<SelfContained>true</SelfContained>`. (Frank-publish.ps1 setzt `--self-contained true` — korrekt.)
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/sdk/8.0/runtimespecific-app-default

### 13.2 `dotnet publish`/`pack` nutzt jetzt Release-Konfiguration
**Symptom:** Publish/Pack baut Release statt Debug — andere Optimierung/Symbole.
**Ursache:** Default-Config geaendert.
**Versionen:** ab .NET 8 — per Design.
**FIX:** Bewusst `-c Debug` falls Debug-Publish gewollt.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/sdk/8.0/dotnet-publish-config

### 13.3 Versions-/distro-spezifische RIDs nicht mehr erkannt
**Symptom:** `NETSDK1083: RuntimeIdentifier 'win10-x64' is not recognized`.
**Ursache:** .NET 8 nutzt portablen RID-Graph; nur portable RIDs (`win-x64`).
**Versionen:** ab .NET 8 — per Design.
**FIX:** Portable RID (`win-x64`) statt `win10-x64`. Notfalls `<UseRidGraph>true</UseRidGraph>`.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/sdk/8.0/rid-graph

### 13.4 NuGet-Audit erzeugt neue Warnungen (NU1901-1904)
**Symptom:** Restore/Build zeigt Sicherheitswarnungen; CI mit `TreatWarningsAsErrors` bricht.
**Ursache:** NuGet-Audit standardmaessig aktiv.
**Versionen:** ab .NET 8.
**FIX:** Verwundbare Pakete aktualisieren (Ziel). Notfalls `<NuGetAudit>false</NuGetAudit>`.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/sdk/8.0/dotnet-restore-audit

### 13.5 Implizites `using System.Net.Http` / `System.Drawing`-Refs entfallen
**Symptom:** `HttpClient`/`System.Drawing`-Typen nach Migration nicht gefunden (Compile-Fehler).
**Ursache:** Globales implizites Using bzw. transitive Referenzen entfernt.
**Versionen:** ab .NET 8 — per Design.
**FIX:** `using System.Net.Http;` explizit; `System.Drawing.Common`/`System.Configuration.ConfigurationManager` NuGet direkt referenzieren.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/sdk/8.0/implicit-global-using-netfx

### 13.6 `Enumerable.Sum` wirft neue OverflowException
**Symptom:** `.Sum()` wirft je nach Quelle (Array vs IEnumerable) `OverflowException`.
**Ursache:** LINQ-Vektorisierung aendert die Additions-Reihenfolge → Zwischensummen ueberlaufen.
**Versionen:** ab .NET 8 — per Design.
**FIX:** Eigene `checked`-Sum-Schleife; bei Bedarf groesseren Akkumulator-Typ.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/core-libraries/8.0/enumerable-sum

### 13.7 `ConfigurationBinder` strenger / ueberspringt ungueltige Array-Elemente
**Symptom:** Config-Binding wirft bei Wert-Mismatch ODER ueberspringt ungueltige Array-Elemente still.
**Ursache:** Striktere bzw. geaenderte Fehlerbehandlung.
**Versionen:** ab .NET 8 — per Design.
**FIX:** Config validieren (`ValidateDataAnnotations`); Werte/Typen korrigieren.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/extensions/8.0/configurationbinder-skips-failed-array-elements

### 13.8 `Process.Start(url)` wirft auf .NET / `WindowStyle` jetzt beachtet
**Symptom:** `Process.Start("https://…")` wirft `Win32Exception`; gestartete Prozesse erscheinen minimiert/versteckt.
**Ursache:** `UseShellExecute` ist auf .NET standardmaessig `false`; `WindowStyle` wird jetzt auch ohne ShellExecute angewendet.
**Versionen:** .NET Core 3.0+ / ab .NET 8 — per Design (dotnet/runtime#28005 CLOSED COMPLETED).
**FIX:** Fuer URL `new ProcessStartInfo(url) { UseShellExecute = true }`; `WindowStyle` explizit `Normal` wenn sichtbar gewuenscht.
**Quelle:** dotnet/runtime#28005

### 13.9 Console-Encoding UTF-8 / `Environment.GetFolderPath` anders auf Unix
**Symptom:** Tool-/CLI-Ausgaben mit veraenderter Code-Page; auf macOS liefert `SpecialFolder.Personal` jetzt `$HOME/Documents`.
**Ursache:** .NET-8-SDK nutzt UTF-8-Konsolen-Encoding; XDG/NSDocumentDirectory-Korrektur.
**Versionen:** ab .NET 8 — per Design (Cross-Platform-relevant fuer macOS-Port).
**FIX:** `Console.OutputEncoding` bei Bedarf explizit; fuer `$HOME` `SpecialFolder.UserProfile` statt `Personal`.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/core-libraries/8.0/getfolderpath-unix

---

## 14. Windows-Plattform / Manifest / P/Invoke

### 14.1 P/Invoke: falscher CharSet / fehlendes SetLastError
**Symptom:** Strings kommen als Muell zurueck; `GetLastWin32Error` liefert Unsinn.
**Ursache:** Default-CharSet ist Ansi; ohne `SetLastError=true` faengt die Runtime den Fehlercode nicht.
**Versionen:** alle; `LibraryImport` ab .NET 7/8.
**FIX:** `[DllImport(…, CharSet=CharSet.Unicode, SetLastError=true)]` (Win32-W-APIs); `Marshal.GetLastPInvokeError()`; kein `[Out] string`. Neu: `[LibraryImport]` (Source-Gen).
**Quelle:** https://learn.microsoft.com/en-us/dotnet/standard/native-interop/best-practices

### 14.2 `[LibraryImport]`-Migration wirft zur Laufzeit
**Symptom:** Umstieg `[DllImport]`→`[LibraryImport]` crasht (Marshalling).
**Ursache:** Source-generiertes Marshalling behandelt `bool`/`char`-Sets anders als Legacy.
**Versionen:** .NET 7+ (dotnet/runtime#75052 CLOSED COMPLETED).
**FIX:** Explizite `[MarshalAs]`/`StringMarshalling`; `bool` → `[MarshalAs(UnmanagedType.Bool)]`.
**Quelle:** dotnet/runtime#75052

### 14.3 `requestedExecutionLevel` — UAC-Prompt / Start-Fehler (uiAccess 24H2)
**Symptom:** App fordert unnoetig Admin; oder startet unter 24H2 mit `uiAccess=true` nicht.
**Ursache:** Falscher Level im Manifest.
**Versionen:** Windows 10/11 (uiAccess-Bug speziell 24H2).
**FIX:** Standard `<requestedExecutionLevel level="asInvoker" uiAccess="false"/>`. `uiAccess=true` nur mit Signatur + Installation in vertrauenswuerdigem Pfad.
**Quelle:** https://learn.microsoft.com/en-us/answers/questions/3928564/

### 14.4 `OSVersion` "luegt" ohne supportedOS-Eintrag
**Symptom:** `Environment.OSVersion` meldet alte Windows-Version.
**Ursache:** Ab Win 8.1 meldet GetVersionEx die hoechste deklarierte Version.
**Versionen:** Windows 8.1+.
**FIX:** Im `<compatibility>`-Block `<supportedOS Id="{8e0f7a12-…}"/>` (Win 10/11). Fuer Versionschecks `RtlGetVersion`.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/sysinfo/targeting-your-application-at-windows-8-1

### 14.5 SmartScreen blockiert unsignierte self-contained EXE
**Symptom:** "Windows protected your PC — SmartScreen prevented an unrecognized app".
**Ursache:** EXE unsigniert / ohne Reputation.
**Versionen:** Windows 10/11 — per Design.
**FIX:** Code-Signing (EV-Cert umgeht SmartScreen sofort; OV baut Reputation auf). Kein Code-Eingriff.
**Quelle:** https://learn.microsoft.com/en-us/answers/questions/1858327/

### 14.6 MAX_PATH 260 — `PathTooLongException` trotz Win10+
**Symptom:** `PathTooLongException` bei Pfaden > 260 Zeichen.
**Ursache:** Long-Path braucht BEIDES: HKLM `LongPathsEnabled=1` UND `longPathAware` im Manifest.
**Versionen:** Win10 1607+; .NET 6+.
**FIX:** `<longPathAware>true</longPathAware>` im Manifest + Registry; alternativ `\\?\`-Praefix.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation

### 14.7 Autostart bricht bei Pfad mit Leerzeichen (HKCU Run)
**Symptom:** App startet beim Login nicht, wenn der EXE-Pfad Leerzeichen enthaelt.
**Ursache:** Ohne Anfuehrungszeichen behandelt Windows alles nach dem ersten Leerzeichen als Argumente.
**Versionen:** alle — per Design.
**FIX:** Pfad in Anfuehrungszeichen: `"\"" + exePath + "\""` nach `HKCU\…\Run`.
**Quelle:** https://github.com/rocksdanister/lively/issues/219

### 14.8 `DoDragDrop` blockiert UI-Thread; elevated App empfaengt kein Drop (UIPI)
**Symptom:** Waehrend Drag feuern keine async-Events; Drag aus Explorer auf Admin-App tut nichts.
**Ursache:** `DoDragDrop` ist modaler OLE-Aufruf (STA/UI-Thread); UIPI blockiert Messages von niedriger auf hohe Integritaetsstufe.
**Versionen:** alle WPF/WinForms; Vista+.
**FIX:** Drag nur vom UI-Thread; lange Arbeit nach Drop async. Fuer elevated: `ChangeWindowMessageFilterEx` fuer `WM_DROPFILES`/`WM_COPYDATA`/`WM_COPYGLOBALDATA` mit `MSGFLT_ALLOW` — besser App nicht elevated laufen lassen.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/desktop/wpf/advanced/drag-and-drop-overview

### 14.9 Suspend/Resume + Lock/Fast-User-Switch nicht erkannt
**Symptom:** App reagiert nicht auf Standby/Aufwachen oder Sperre/Benutzerwechsel.
**Ursache:** Keine WndProc fuer `WM_POWERBROADCAST` / keine WTS-Registrierung.
**Versionen:** alle.
**FIX:** In .NET einfach `SystemEvents.PowerModeChanged` / `SessionSwitchEventArgs`; nativ `WM_POWERBROADCAST` + `WTSRegisterSessionNotification`/`WM_WTSSESSION_CHANGE`.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/power/wm-powerbroadcast-messages

### 14.10 `timeBeginPeriod` global wirkungslos / Win11-Throttling
**Symptom:** Hochaufloesender Timer driftet/bleibt grob, besonders wenn minimiert/verdeckt.
**Ursache:** Ab Win10 2004 wirkt `timeBeginPeriod` nur pro Prozess; Win11 garantiert fuer occluded Fenster keine erhoehte Aufloesung.
**Versionen:** Win10 2004+, Win11 (WindowsAppSDK#3263 CLOSED COMPLETED).
**FIX:** `timeBeginPeriod` pro Prozess setzen (+ `timeEndPeriod`); fuer praezises Scheduling `CreateWaitableTimerEx(…HIGH_RESOLUTION)`.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/api/timeapi/nf-timeapi-timebeginperiod

### 14.11 File-Association: HKCU UserChoice ist hash-geschuetzt
**Symptom:** Programmatisch gesetzte Default-App fuer eine Endung wird ignoriert/zurueckgesetzt.
**Ursache:** Win10/11 schuetzen `UserChoice` mit benutzer-/maschinenspezifischem Hash.
**Versionen:** Windows 10/11.
**FIX:** Eigene ProgID + Capabilities unter `HKCU\Software\Classes` + `OpenWithProgids`, dann `SHChangeNotify`. Finale Wahl muss der User im OS-Dialog treffen — UserChoice NICHT faelschen.
**Quelle:** https://learn.microsoft.com/en-us/windows/win32/shell/fa-sample-scenarios

---

## 15. WinForms-spezifisch (Frank nutzt WinForms-Teile fuer DPI/Tray)

### 15.1 Top-Level-Forms skalieren Min/Max-Size nach DPI
**Symptom:** In PerMonitorV2 aendern sich `MinimumSize`/`MaximumSize` mit dem Monitor-DPI; Layout-Logik bricht.
**Versionen:** ab .NET 8 (WinForms) — per Design.
**FIX:** Opt-out via `runtimeconfig.json`: `System.Windows.Forms.ScaleTopLevelFormMinMaxSizeForDpi = false`.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/windows-forms/8.0/forms-scale-size-to-dpi

### 15.2 `ImageList.ColorDepth` default = Depth32Bit; `PictureBox` prueft Zertifikate
**Symptom:** Icons sehen anders aus (Alpha/Farbtiefe); `PictureBox.Load(url)` schlaegt bei ungueltigem Zertifikat fehl.
**Versionen:** ab .NET 8 (WinForms) — per Design.
**FIX:** `ColorDepth` explizit setzen; Bild manuell via `HttpClient` laden + als Stream uebergeben.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/compatibility/windows-forms/8.0/imagelist-colordepth

---

## 15b. Nachtrag aus dem Best-Practices-Lauf (2026-06-02)

Diese Bugs/Fallen kamen beim positiven Best-Practices-Lauf zutage und ergaenzen die Themen-Abschnitte oben.

### 15b.1 `JsonSerializerOptions` nach erstem Gebrauch eingefroren → InvalidOperationException (zu §10)
**Symptom:** Aenderung an einer geteilten `JsonSerializerOptions`-Instanz nach der ersten (De)Serialisierung wirft `InvalidOperationException`.
**Ursache:** Die Optionen werden beim ersten Gebrauch implizit immutable.
**Versionen:** .NET 8 — per Design.
**FIX:** Optionen EINMAL konfigurieren, dann `MakeReadOnly()`; als statische Instanz wiederverwenden (Analyzer CA1869). Siehe BP §3.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/standard/serialization/system-text-json/configure-options

### 15b.2 IHttpClientFactory + Cookies = CookieContainer-Leak (zu §8)
**Symptom:** Cookies lecken zwischen unabhaengigen App-Teilen; bei Handler-Recycling gehen Cookies verloren.
**Ursache:** Gepoolte Handler teilen sich den `CookieContainer`.
**Versionen:** .NET 8 — per Design.
**FIX:** Bei Cookie-Bedarf NICHT `IHttpClientFactory` nutzen, sondern eigenen Handler/Client mit dediziertem `CookieContainer`.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/fundamentals/networking/http/httpclient-guidelines

### 15b.3 Polly wirft `TimeoutRejectedException`, nicht `TimeoutException` (zu §8)
**Symptom:** `catch (TimeoutException)` faengt den Resilience-Timeout NICHT.
**Ursache:** `Microsoft.Extensions.Http.Resilience`/Polly wirft `TimeoutRejectedException`.
**Versionen:** .NET 8.
**FIX:** Im `ShouldHandle`/catch `TimeoutRejectedException` behandeln.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/resilience/http-resilience

### 15b.4 Scoped-Service aus dem Root-Provider (Captive-Dependency) (zu §11/DI)
**Symptom:** Fehler/Captive-Dependency beim Aufloesen eines Scoped-Service direkt aus dem Root-Provider (Desktop-App ohne Request-Scope).
**Ursache:** Kein aktiver Scope.
**Versionen:** alle .NET Core/.NET.
**FIX:** Expliziten Scope via `IServiceScopeFactory.CreateScope()`; Singletons duerfen keine Scoped-Services kapseln.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/core/extensions/dependency-injection/guidelines

### 15b.5 `BitmapSource` auf ThreadPool-Thread laden leakt einen Dispatcher (zu §9)
**Symptom:** Speicher waechst; ein Dispatcher bleibt haengen, selbst wenn das Bild gefreezt wird.
**Ursache:** WPF erzeugt fuer das Laden auf einem Nicht-UI-Thread einen Dispatcher, der nicht aufgeraeumt wird.
**Versionen:** WPF (dotnet/wpf#3412).
**FIX:** `BitmapSource`/`BitmapImage` auf dem UI-Thread laden, oder Bytes laden + auf UI-Thread dekodieren; danach `Freeze()`.
**Quelle:** https://github.com/dotnet/wpf/issues/3412

### 15b.6 WPF-Virtualisierung wird still abgeschaltet (zu §2/Performance)
**Symptom:** Grosse Liste ruckelt / hoher Speicher trotz `VirtualizingStackPanel`.
**Ursache:** Container direkt zum ItemsControl adden, gemischte Container-Typen (Separator+MenuItem), `CanContentScroll="false"`, oder Container-gebundener State (`Expander.IsExpanded` am Container statt Item) bei Recycling.
**Versionen:** alle WPF.
**FIX:** Nur ItemsSource-Binding (keine direkten Container), einheitliche Item-Typen, `CanContentScroll="true"`, State ans Item binden. Siehe BP §5.
**Quelle:** https://learn.microsoft.com/en-us/dotnet/desktop/wpf/advanced/optimizing-performance-controls

### 15b.7 CommunityToolkit `[RelayCommand]` CanExecute aktualisiert sich nicht automatisch (zu §12/MVVM)
**Symptom:** Button bleibt im falschen Enabled-State, obwohl sich die Bedingung geaendert hat.
**Ursache:** Der Command merkt Aenderungen an `CanExecute` nicht selbst.
**Versionen:** CommunityToolkit.Mvvm 8.x.
**FIX:** Auf der gekoppelten Property `[NotifyCanExecuteChangedFor(nameof(XxxCommand))]` setzen (oder `IRelayCommand.NotifyCanExecuteChanged()`).
**Quelle:** https://learn.microsoft.com/en-us/dotnet/communitytoolkit/mvvm/generators/relaycommand

### 15b.8 WinUI 3: kein DataGrid, kein AdornerLayer, VS-XAML-Designer nicht unterstuetzt (zu §12)
**Symptom:** Fehlende Standard-Controls; der VS-Design-Tab funktioniert nicht.
**Ursache:** WinUI-3-Funktionsumfang.
**Versionen:** WASDK alle.
**FIX:** DataGrid via Community `WinUI.TableView` (Wartung selbst pruefen); Adorner per Canvas/Grid-Overlay nachbauen; statt Designer XAML Hot Reload nutzen.
**Quelle:** https://learn.microsoft.com/en-us/windows/apps/windows-app-sdk/migrate-to-windows-app-sdk/wpf-patterns-winui3

### 15b.9 Moq 4.20.0 SponsorLink (Tooling-Falle)
**Symptom:** Moq 4.20.0 (Aug 2023) las still Git-Emails aus und lud sie hoch.
**Ursache:** Eingebettete `SponsorLink`-DLL (in 4.20.2 entfernt).
**Versionen:** Moq 4.20.0.
**FIX:** Moq-Version pinnen oder auf **NSubstitute** migrieren (MIT, unkritisch). Siehe BP §8.
**Quelle:** https://medium.com/azlamps/moq-scandal-or-why-caring-about-licenses-is-a-good-idea-9c5086024435

---

## 16. Fix-Status — was am 2026-06-02 per `gh` verifiziert wurde

> Ehrlichkeits-Hinweis: "CLOSED COMPLETED" heisst bei .NET oft "Verhalten dokumentiert /
> per Design bestaetigt", NICHT zwingend "Workaround unnoetig". Bei per-Design-Punkten
> (Single-File, Process.Start, ConcurrentDictionary) bleibt der FIX dauerhaft gueltig.

| Bug | Repo#Issue | Status (2026-06-02) | Bedeutung |
|-----|-----------|---------------------|-----------|
| Clipboard CLIPBRD_E_CANT_OPEN (4.1) | dotnet/wpf#9901 | **OPEN** | Retry-Workaround bleibt aktiv |
| Acrylic-Blur Resize (2.6) | dotnet/wpf#3608 | **OPEN** | Workaround bleibt |
| PerMonitor-DPI .NET 8 (3.1) | dotnet/wpf#9803 | **OPEN** | Workaround bleibt |
| Window.Left/Top PerMonitorV2 (3.3) | dotnet/wpf#4127 | **OPEN** | SetWindowPos-Workaround bleibt |
| Trimming bricht WPF (1.5) | dotnet/wpf#11436 | **OPEN** | PublishTrimmed weiter meiden |
| Airspace (2.5) | dotnet/wpf#152 | **OPEN** | CompositionControl-Workaround bleibt |
| Dispatcher.Invoke blockiert (7.6) | dotnet/wpf#8066 | **OPEN** | BeginInvoke bleibt |
| ToolWindow DPI (3.5) | dotnet/wpf#10422 | **OPEN** | Top-Level statt ToolWindow |
| Single-File native libs (1.2/1.3) | dotnet/runtime#42772, #38636 | CLOSED (per Design) | Workaround bleibt gueltig |
| Assembly.Location leer (1.1) | dotnet/runtime#13531 | CLOSED (per Design) | ProcessPath bleibt Pflicht |
| Hook in Single-File (1.7) | dotnet/runtime#71522 | CLOSED COMPLETED | x86 weiter meiden |
| Process.Start(url) (13.8) | dotnet/runtime#28005 | CLOSED (per Design) | UseShellExecute=true bleibt |
| LibraryImport-Marshalling (14.2) | dotnet/runtime#75052 | CLOSED COMPLETED | MarshalAs-Angaben bleiben |
| FileSystemWatcher Overflow (9.7) | dotnet/runtime#81226 | CLOSED COMPLETED | Error-Handler bleibt sinnvoll |
| ConcurrentDictionary GetOrAdd (11.13) | dotnet/runtime#33221 | CLOSED (per Design) | Lazy-Workaround bleibt |
| WinUI Single-File unpackaged (12.1) | microsoft-ui-xaml#10173 | **OPEN** | Kein Single-File fuer WinUI |
| WinUI Window.Activate (12.3) | microsoft-ui-xaml#7595 | **OPEN/REOPENED** | SetForegroundWindow bleibt |
| WinUI publish unpackaged (12.x) | WindowsAppSDK#4901 | **OPEN** | RID explizit setzen |
| WinUI Mica crash (12.8) | microsoft-ui-xaml#7079 | CLOSED COMPLETED | neuere WASDK nutzen |
| WinUI DispatcherQueue null (12.4) | microsoft-ui-xaml#4219 | CLOSED COMPLETED | TryEnqueue bleibt Muster |
| WinUI Release-Crash (12.6) | microsoft-ui-xaml#9675 | CLOSED COMPLETED | Trim aus |
| WinUI ContentDialog close (12.5) | microsoft-ui-xaml#8913 | CLOSED COMPLETED | Dialog-Guard bleibt sinnvoll |
| Bootstrapper unpackaged (12.2) | WindowsAppSDK#2918, #5530 | CLOSED COMPLETED | neueste WASDK |
| Timer-Resolution (14.10) | WindowsAppSDK#3263 | CLOSED COMPLETED | HIGH_RESOLUTION-Timer |
| WPF Binding-Leak INCC (9.3) | dotnet/wpf#5739 | CLOSED COMPLETED | Muster bleibt beachtenswert |

**Methodik-Hinweis:** Status per `gh issue view --json state,stateReason`. C#-12-Compiler-
Breaking-Changes (Collection-Expressions-Overloads, Primary-Constructor-Capture, `ref readonly`)
liegen nur als kuratierte Markdown-Liste im `dotnet/roslyn`-Repo und wurden in dieser Recherche
NICHT aus einer Einzel-URL verifiziert — bei Bedarf `gh repo clone dotnet/roslyn` →
`docs/compilers/CSharp/Compiler Breaking Changes - DotNet 8.md`.

---

## 17. .NET 9 + .NET 10: Neuerungen & Breaking Changes beim Sprung von net8.0 (WPF/Desktop)

> Recherchiert 2026-07-02 (Engine A). Frank ist von .NET 8 auf **.NET 10** (net10.0-windows, C# 14). Quellen:
> learn.microsoft.com „What's new in WPF/.NET for .NET 9/10", „.NET 10 breaking changes", „What's new in C# 14".
> **Pflicht:** Breaking Changes von .NET 9 UND .NET 10 pruefen (9 wird uebersprungen).

### 17.1 WPF-Neuerungen (.NET 9 → .NET 10)
- **.NET 9:** Fluent theme (Windows-11-Look), **`ThemeMode`** (Light/Dark/System zur Laufzeit), Support fuer
  Windows-**Akzentfarbe**, silbenbasierte Ligaturen. Fluent-UI ist **„still in progress"** — nicht auf Vollstaendigkeit verlassen.
- **.NET 10:** Fluent-Styles fuer weitere Controls (`DatePicker`, `GridSplitter`, `GridView`, `GroupBox`,
  `Hyperlink`, `Label`, `NavigationWindow`, `RichTextBox`, `TextBox`); Expander-Animation- + HighContrast-Crash-Fixes;
  RTL-Layout-Fixes (`MenuItem`/`Expander`/`TreeViewItem`); Performance (XAML-Parsing, Font-Rendering, DynamicResources).
- **Neue APIs (additiv, nicht breaking):** `MessageBoxButton`/`MessageBoxResult` bekommen die vollen Win32-Werte
  (`AbortRetryIgnore`, `RetryCancel`, `CancelTryContinue`, `Abort`, `Retry`, `Ignore`, `TryAgain`, `Continue`);
  **Grid-Kurzschreibweise** als String: `<Grid ColumnDefinitions="1*, 2*, Auto, *, 300" RowDefinitions="1*, Auto, 25"/>`.

### 17.2 Clipboard-Umbau (.NET 9/10) — ⭐ relevant fuer die Voice-Overlays (§4)
- **Symptom nach Upgrade:** Compile-**Obsolete-Warnungen** auf `Clipboard.SetData`/`GetData`/`SetDataObject` mit
  beliebigen Objekten; im Extremfall Laufzeitfehler, weil `BinaryFormatter` (in .NET 9 komplett obsolet/entfernt) intern nicht mehr traegt.
- **Ursache:** WPF und WinForms nutzen ab .NET 10 **dieselbe Clipboard-Codebasis**; `BinaryFormatter`-basierte
  Clipboard-Pfade sind obsoleted.
- **FIX:** Fuer eigene Typen die **neuen JSON-Serialisierungs-Methoden** nutzen (`SetDataAsJson`/`TryGetData<T>`);
  fuer reinen Text bleibt `Clipboard.SetText`/`GetText` unveraendert gueltig (Frank nutzt fast nur Text →
  **die Retry-Schleife aus §4.1 bleibt der Kern, kein Handlungsdruck**). Nur bei Objekt-Clipboard-Daten migrieren.

### 17.3 .NET 10 Breaking Changes, die Desktop-/Overlay-Apps treffen
| Aenderung | Wirkung | FIX |
|-----------|---------|-----|
| **Single-file: native library search geaendert** | P/Invoke findet native `.dll` neben der EXE evtl. nicht mehr | Native Libs explizit ausliefern/laden; `DllImportSearchPath` gezielt setzen |
| **`DllImportSearchPath.AssemblyDirectory` sucht NUR im Assembly-Verzeichnis** | frueher breitere Suche → jetzt `DllNotFoundException` moeglich | Pfad-Strategie pruefen (relevant fuer `SetWindowsHookEx`/native Overlay-Libs, §1.7/§14) |
| **Keine Default-SIGTERM-/Terminierungs-Handler** | Prozess-Shutdown-Verhalten anders (v.a. Konsolen-/Backend-Teil) | eigenen Signal-Handler registrieren, wenn Graceful-Shutdown gebraucht |
| **HTTP/3 aus bei `PublishTrimmed`** | HTTP/3-Requests fallen still auf HTTP/2 zurueck | bewusst; falls HTTP/3 gebraucht: nicht trimmen bzw. Feature erzwingen |
| **C# 14 Overload-Resolution mit `Span`-Parametern** | andere Ueberladung wird gewaehlt → subtile Verhaltensaenderung | bei Span-lastigem Code Ueberladungen pruefen/testen |

### 17.4 C# 14 — neue Features + die zwei echten Fallen
- **`field`-Keyword (field-backed properties)** — ⭐ **BREAKING:** `field` ist jetzt ein **kontextuelles Keyword**.
  Wer eine Variable/ein Member namens `field` hat (z. B. „Datenbank-Field"), bekommt Konflikte/Warnungen.
  **FIX:** `@field` oder `this.field` zum Disambiguieren, oder umbenennen. Beispiel-Nutzen:
  `public string Msg { get; set => field = value ?? throw new ArgumentNullException(); }`.
- **Null-conditional Assignment** `customer?.Order = GetCurrentOrder();` — ⭐ **Falle:** die rechte Seite wird
  **nur ausgewertet, wenn links nicht null** ist → **Seiteneffekte von `GetCurrentOrder()` bleiben aus**, wenn `customer==null`.
  Nicht mit `++`/`--` kombinierbar.
- **Extension members** (`extension`-Bloecke: Properties/statische/Operatoren) — alte `this`-Syntax bleibt gueltig, Umstieg freiwillig.
- Weiter: `nameof(List<>)` (unbound generics), `Span`/`ReadOnlySpan` implizite Konvertierungen, Modifier an
  untypisierten Lambda-Parametern, `partial`-Konstruktoren/-Events. Vollstaendige C#-14-Breaking-Changes:
  learn.microsoft.com „compiler breaking changes - dotnet 10".

### 17.5 File-based Apps (.NET 10) — neu, kein Bug
`dotnet run app.cs` fuehrt eine **einzelne `.cs`-Datei ohne `.csproj`/`.sln`** aus; NuGet/SDK per `#:package`/`#:sdk`-Direktiven,
Shebang `#!/usr/bin/env dotnet` fuer Skripte; `dotnet project convert app.cs` macht ein volles Projekt daraus.
Nuetzlich fuer kleine Tools/Skripte (ersetzt teils Python-Helfer auf Windows).

### 17.6 WinForms-Teil (Frank nutzt WinForms fuer DPI/Tray, §15) — .NET 10
- **Dark Mode final** (`Application.SetColorMode(SystemColorMode)` nicht mehr experimentell, Warnung **WFO5001** entfaellt),
  **Async Forms final** (WFO5002 entfaellt). Neue Obsolete-Analyzer: **WFDEV004** (`Form.OnClosing`/`OnClosed`),
  **WFDEV005** (`GetData` → **`TryGetData<T>`**), **WFDEV006** (einzelne Legacy-Controls). Neue API `Form.ScreenCaptureMode`
  (`Allow`/`HideContent`/`HideWindow`).

## Pflicht-Checkliste vor C#/.NET-Arbeit

- [ ] Diese Datei gelesen, Stand-Datum gegen `dotnet --version` + TargetFramework abgeglichen?
- [ ] Async: nirgends `.Result`/`.Wait()` auf dem UI-Thread? (§7.1) `async void` nur in Event-Handlern? (§7.2)
- [ ] Single-File: `Environment.ProcessPath` statt `Assembly.Location`? Kein `PublishTrimmed` bei WPF? (§1)
- [ ] Overlay: Topmost per `SetWindowPos` abgesichert? `AllowsTransparency`-Scope klein? (§2)
- [ ] DPI: `app.manifest` PerMonitorV2? Positionierung ueber physische Pixel? (§3)
- [ ] Clipboard: Retry-Schleife um jeden Zugriff? (§4.1)
- [ ] HttpClient: ein statischer Client mit `PooledConnectionLifetime`? (§8)
- [ ] Disposables (Audio, Streams, Timer, Events) in `using`/`Dispose` + `-=`? (§9)
- [ ] JSON: `PropertyNameCaseInsensitive`/CamelCase fuer API-DTOs? Options als statische Instanz? (§10.1)
- [ ] WinUI 3: kein Single-File, kein echtes Transparenz-Overlay erwartet? (§12.1, §12.7)
- [ ] Publish: `--self-contained true` explizit (RID impliziert es nicht mehr)? (§13.1)
- [ ] Nach erlebtem neuen Bug: hier als Eintrag ergaenzen + Stand-Header aktualisieren.
