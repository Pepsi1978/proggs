# Bekannte Bugs/Fallen: Text-Injection in Electron/Chromium-Felder unter Windows (C#/WPF) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Die EINE tragende Erkenntnis: Das Eingabefeld ist ein **HTML-`contenteditable`-DIV** (ProseMirror/
> Lexical) in einer einzigen grossen Chromium-Render-Flaeche — **kein Win32-Edit-Control**. Deshalb
> versagen `FindWindowEx`-Einzelsuche, `WM_SETTEXT`/`WM_PASTE`, `keybd_event` ohne Scancode und blinde
> Festkoordinaten-Klicks alle gleichzeitig. Der robuste, zu macOS symmetrische Weg: **Feld per UI
> Automation FINDEN + FOKUSSIEREN, Text per Zwischenablage + echtem Strg+V mit Scancodes EINFUEGEN.**
>
> Sektionen: **K** Kernursache/Strategie · **F** Fensterfindung/HWND · **A** Fokus/Vordergrund ·
> **T** Tastatur-Injektion · **M** Maus/DPI/Koordinaten · **C** Zwischenablage · **U** UI Automation ·
> **E** Electron-Eigenheiten & App-Unterschiede · **N** .NET-10-Deployment (self-contained single-file) ·
> **W** Windows 11 24H2.

| #  | Signal / Situation | Sofort-Regel | Volltext |
|----|--------------------|--------------|----------|
| 1  | Text landet in KEINEM der 3 Felder, macOS klappt | Feld ist contenteditable, kein Edit-Control → UIA finden+fokussieren, dann Clipboard-Paste | K1 |
| 2  | `FindWindowEx(top,…, "Chrome_RenderWidgetHostHWND")` = NULL | Sucht nur DIREKTE Kinder; Ziel liegt tief → `EnumChildWindows` (rekursiv) mit Klassen-Filter | F1 |
| 3  | Eigene Rekursion um EnumChildWindows → HWND doppelt | `EnumChildWindows` einmal aufrufen, flach filtern (rekursiert selbst) | F2 |
| 4  | Kaskade mit `Chrome_WidgetWin_0/_1` bricht je Rechner | Zwischen-Klassennamen sind Implementierungsdetail → nie fest verdrahten | F3 |
| 5  | Mehrere passende HWNDs, Text geht ins falsche | Aktives, sichtbares, nicht-0-grosses Render-HWND waehlen (oder UIA) | F4 |
| 6  | `EnumChildWindows` findet gar kein Render-HWND | Evtl. Off-Screen-Rendering → nur UIA/Accessibility-Weg moeglich | F5 |
| 7  | `SetForegroundWindow` blinkt nur Taskleiste | Foreground-Lock; eigenen Input (ALT via SendInput) erzeugen, Rueckgabe pruefen | A1 |
| 8  | `AttachThreadInput`-Trick friert die App ein | Deadlock; meiden, ALT-SendInput-Weg nutzen; sonst kurz + im `finally` detach | A2 |
| 9  | `AllowSetForegroundWindow` wirkt nur kurz | Recht verfaellt beim naechsten Input → frisch je Sequenz, vor SetForeground | A3 |
| 10 | Minimiertes Ziel kommt nicht nach vorne | Erst `IsIconic`→`SW_RESTORE`, dann aktivieren; `WaitForInputIdle` statt Sleep | A5 |
| 11 | `SetFocus` aufs Fremdfeld gibt NULL | Wirkt nur in eigener Input-Queue → Top-Window aktivieren, nicht Kind fokussieren | A6 |
| 12 | Strg+V via `keybd_event` kommt nicht an | Veraltet, kein Scancode → `SendInput` mit `MapVirtualKey`+`KEYEVENTF_SCANCODE` | T1 |
| 13 | `SendInput` mit nur `wVk`, `wScan=0` ignoriert | Chromium braucht echten Scancode → `KEYEVENTF_SCANCODE` setzen | T2 |
| 14 | Paste mal ja, mal nein (Modifier verschluckt) | Reihenfolge Ctrl↓ → 10–30 ms → V↓ → V↑ → Ctrl↑; rechte Modifier `EXTENDEDKEY` | T3 |
| 15 | Klappt als Nicht-Admin, nicht als Admin (o. umgekehrt) | UIPI: Integritaetslevel angleichen, Sender NICHT elevated starten | T4 |
| 16 | `WM_PASTE`/`WM_SETTEXT`/`ControlSend` tun nichts | contenteditable ist kein Edit-Control → nur fokus-gebundenes Strg+V | T5 |
| 17 | `SendKeys.SendWait("^v")` unzuverlaessig | Wartet bei Fremdprozess nicht → P/Invoke `SendInput` mit Scancode | T6 |
| 18 | Blinder Klick auf 55%/86% trifft daneben | Feld wandert (ResizeObserver) → Position per UIA holen, nicht fix | M1 |
| 19 | Klick auf 125/150%-Monitor systematisch verschoben | `GetWindowRect` logisch vs `SetCursorPos` physisch → Per-Monitor-V2 + umrechnen | M2 |
| 20 | Versatz nur auf Zweitmonitor | Fehlende PerMonitorV2-Awareness im app.manifest | M3 |
| 21 | Klickpunkt einige px daneben (Schatten) | `GetWindowRect` enthaelt DWM-Rahmen → `DWMWA_EXTENDED_FRAME_BOUNDS` | M4 |
| 22 | Klick landet auf Primaer- statt Zweitmonitor | `SendInput` absolut braucht `MOUSEEVENTF_VIRTUALDESK` + Normalisierung | M5 |
| 23 | `Clipboard.SetText` wirft `CLIPBRD_E_CANT_OPEN` | Fremdprozess haelt Clipboard → Retry-Schleife; WinForms-Clipboard stabiler | C1 |
| 24 | Feld bleibt leer / bekommt alten Inhalt | Restore gewinnt das Rennen → 300–500 ms warten o. Restore weglassen | C2 |
| 25 | Clipboard nach App-Exit leer | OLE delayed rendering → `SetDataObject(text, copy:true)` / `Flush()` | C3 |
| 26 | `ThreadStateException` beim Clipboard | OLE braucht STA → nur auf UI-/STA-Thread, `SetApartmentState` vor Start | C5 |
| 27 | UIA findet das Feld nicht (leerer Baum) | Chromium-A11y ist AUS/lazy → per UIA-Client/`WM_GETOBJECT` aktivieren, warten | U1 |
| 28 | UIA leer, sobald Overlay Fokus hat / Ziel minimiert | Chromium baut Baum ab → Ziel sichtbar halten, Element neu aufloesen | U2 |
| 29 | `FocusedElement` null/falsch/langsam | Nicht darauf verlassen → vom Fenster-HWND gezielt nach Edit/Document suchen | U3 |
| 30 | UIA-Call friert WPF-UI ein | Cross-Process-COM auf UI-Thread → dedizierter Worker-Thread + Timeout | U4 |
| 31 | Managed `System.Windows.Automation` unzuverlaessig | UIA 2.0 veraltet → native `IUIAutomation` / **FlaUI (UIA3)** | U5 |
| 32 | `FindAll`/`Descendants` haengt im Chromium-Baum | `FindFirst` + enger Scope + `CacheRequest` | U6 |
| 33 | Erster `FindFirst` direkt nach Aktivierung = null | Baum kommt asynchron → Retry/Backoff oder `StructureChanged`-Event | U7 |
| 34 | `ValuePattern.SetValue` wirft/fehlt am contenteditable | UIA kann contenteditable-Text NICHT setzen → nur finden+fokussieren, dann Paste | U8 |
| 35 | HWND-/Titel-Logik bricht beim App-Update | Electron ist bewegliches Ziel → versionsunabhaengig (Accessibility, Prozessname) | E4 |
| 36 | Logik fuer Claude trifft Codex/Cursor/VS Code nicht | Gleiche Electron-Basis, andere Struktur → pro App erkennen, Mapping-Tabelle | E5 |
| 37 | Globaler Hotkey feuert nicht | Claude nutzt `Ctrl+Alt+Space`; globalShortcut first-come → Rueckgabe pruefen, ausweichen | E9 |
| 38 | Welche Chromium-Version? UIA verlässlich? | Claude 1.12603.1 = Electron 41/Chromium 146 → native UIA an; Version zur Laufzeit auslesen | E4 |
| 39 | Eigene native Helper-DLL: `DllNotFoundException` im Single-File | .NET 10 sucht nicht mehr im .exe-Ordner → `IncludeNativeLibrariesForSelfExtract` / `AssemblyDirectory` | N1 |
| 40 | Pfad neben der .exe leer (IL3000) | `Assembly.Location`="" im Single-File → `AppContext.BaseDirectory` / `Environment.ProcessPath` | N2 |
| 41 | Trimming/AOT → WPF startet nicht | WPF nicht trimmbar; COM-Marshalling bricht → Single-File OHNE Trimming/AOT | N3 |
| 42 | FlaUI nach Publish: `Interop.UIAutomationClient` fehlt | `EmbedInteropTypes=false` setzen, im Bundle Auffindbarkeit prüfen | N4 |
| 43 | DPI/Koordinaten im Single-File daneben | eigenes app.manifest (PerMonitorV2 + Win10/11-supportedOS) einbetten | N5 |
| 44 | Overlay fällt auf 24H2 hinter Paint/Photos | 24H2-Z-Order-Regression → Topmost ereignisgetrieben re-asserten / Overlay vor Paste ausblenden | W1 |
| 45 | Klappt nur als Nicht-Admin (Zukunft) | 24H2 „Administrator Protection" (aktuell deaktiviert, latent) → Sender nie elevated | W2 |
| 46 | Code-Tab fuellt nicht, Chat/Cowork schon | Feld meldet sich als `Group` (nicht Edit/Document); FocusedElement-Filter tolerant (Edit/Document/Group) + `RootWebArea` ausschliessen | U9 |
| 47 | Cowork fuellt nicht DIREKT nach beendeter Aufgabe (Klick ins Feld → naechstes Mal geht's) | Fokus auf `Fortschritt`-Button; `FindFirst(Edit\|Document)` bleibt an `RootWebArea` haengen → `RootWebArea` per `NotCondition` aus der Suche ausschliessen, dann findet `FindFirst` das echte Feld | U10 |
| 48 | Code-Tab fuellt nicht wenn Fokus woanders (Fallback `found=null`) | Code-Tab-Feld ist eine `Group` (nicht Edit/Document) → Fallback ueber Edit/Document/`Group` + `FindAll`, gezielt das `tiptap`/`ProseMirror`-Feld per ClassName picken, Container (`dframe-main`) ueberspringen | U11 |
