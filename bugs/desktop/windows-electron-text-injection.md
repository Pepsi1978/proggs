# Bekannte Bugs/Fallen: Text-Injection in Electron/Chromium-Felder unter Windows (C#/WPF)

> PFLICHT-LESEN vor Arbeit an der Windows-Text-Einfuege-Funktion von **ClaudeVoiceOverlay-Windows**
> (C#/.NET 8, WPF-Overlay), die Text in die Eingabefelder von **Claude Desktop** (Electron/Chromium)
> schreibt: **Chat** ("Nachricht schreiben", ganz unten), **Code** (Befehlszeile unten) und
> **Cowork** (Eingabefeld mittig/oberes Dreiviertel, rutscht nach unten, sobald Aufgaben erscheinen).
>
> **Leitsymptom:** Unter Windows landet in KEINES der drei Felder Text. Unter macOS funktioniert exakt
> dieselbe Funktion (Accessibility / AXUIElement) einwandfrei. Dieser Almanach erklaert, warum — und
> liefert den funktionserhaltenden Windows-Weg, der das macOS-AXUIElement-Vorgehen spiegelt.
>
> Stand: recherchiert am **2026-06-15** mit **7 parallelen Researchern** (Fokus: Microsoft Learn +
> Chromium-Accessibility-Doku zuerst, dann dotnet/wpf-, dotnet/winforms-, electron/electron-,
> PowerToys-Issues, AutoHotkey-/FlaUI-Praxis). ~50 Eintraege in 8 Sektionen.
>
> **Versions-Anker:**
> - Sender: **.NET 8** (`net8.0-windows`), **WPF**, Windows 10/11.
> - Ziel: **Claude Desktop 1.12603.1** (Build `3df4fd`, 2026-06-11), Electron-App mit gebuendeltem
>   Chromium. 2026er-Electron-Linie ist ~39–42 (Chromium ~134+); **ab Chromium 138 ist der native
>   UI-Automation-Provider standardmaessig AN** (per WebSearch 2026-06-15 bestaetigt) — Franks aktuelle
>   Build unterstuetzt den UIA-Weg also voll, die alte Chrome-117-UIA-Regression ist erledigt.
>
> **Wichtig:** Dies ist der reine **Bug-Almanach** (bekannte Fehler/Fallen/Workarounds). Die
> Gegenseite (Best-Practices — "wie macht man es von vornherein richtig") wird in einem **getrennten
> Lauf** angelegt; dieser Almanach enthaelt bewusst keine allgemeinen Best-Practice-Essays.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Die EINE tragende Erkenntnis: Das Eingabefeld ist ein **HTML-`contenteditable`-DIV** (ProseMirror/
> Lexical) in einer einzigen grossen Chromium-Render-Flaeche — **kein Win32-Edit-Control**. Deshalb
> versagen `FindWindowEx`-Einzelsuche, `WM_SETTEXT`/`WM_PASTE`, `keybd_event` ohne Scancode und blinde
> Festkoordinaten-Klicks alle gleichzeitig. Der robuste, zu macOS symmetrische Weg: **Feld per UI
> Automation FINDEN + FOKUSSIEREN, Text per Zwischenablage + echtem Strg+V mit Scancodes EINFUEGEN.**
>
> Sektionen: **K** Kernursache/Strategie · **F** Fensterfindung/HWND · **A** Fokus/Vordergrund ·
> **T** Tastatur-Injektion · **M** Maus/DPI/Koordinaten · **C** Zwischenablage · **U** UI Automation ·
> **E** Electron-Eigenheiten & App-Unterschiede.

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

---

## K) Kernursache & Strategie (zuerst lesen)

### K1. Warum unter Windows in KEIN Feld Text landet — und der zu macOS symmetrische Fix ⭐ HAEUFIG
- **Symptom:** Dieselbe Funktion fuellt unter macOS alle Felder, unter Windows keines — egal ob Chat, Code oder Cowork.
- **Ursache (mehrschichtig, alle wirken zusammen):**
  1. Das Eingabefeld ist ein **HTML-`contenteditable`-DIV** (ProseMirror/Lexical/Slate) in EINER grossen Chromium-Render-Flaeche — **kein** natives Win32-Edit-Control. → Alle `WM_SETTEXT`/`WM_PASTE`/`EM_*`-Tricks laufen ins Leere (siehe T5, E1).
  2. Das echte Eingabe-HWND (`Chrome_RenderWidgetHostHWND`) liegt **tief verschachtelt**, nicht als direktes Kind → einstufiges `FindWindowEx` scheitert (F1).
  3. `keybd_event`-Strg+V liefert **keinen Hardware-Scancode**, den der Chromium-Renderer erwartet (T1/T2).
  4. Der **blinde Festkoordinaten-Klick** (55 %/86 %) trifft das **wandernde** Feld und auf High-DPI die falsche Pixelposition (M1–M4).
  5. macOS hat keinen Foreground-Lock und kein UIPI → dort fallen mehrere dieser Huerden komplett weg, was das Plattform-Delta erklaert (A1, T4).
- **Versionen:** Per Design (Chromium-Architektur + Win32-Eingabemodell).
- **FIX (funktionserhaltend, der tragende Weg):** Den macOS-AXUIElement-Ansatz spiegeln, statt blind zu klicken:
  1. **Accessibility scharfschalten** (Chromium-Baum ist lazy/aus — U1): native UIA-Client gegen das Fenster halten bzw. `WM_GETOBJECT` an `Chrome_RenderWidgetHostHWND` senden; bei Franks Build (Chromium ≥138) ist native UIA ohnehin standardmaessig aktiv.
  2. **Feld FINDEN + FOKUSSIEREN** via UIA (`ControlType.Edit`/`Document`, `SetFocus()`) — monitor-, DPI- und HWND-bewusst, immun gegen das Wandern (U8, M1).
  3. **Text per Zwischenablage + echtem Strg+V mit Scancodes** einschleusen (`SendInput` + `KEYEVENTF_SCANCODE`, Clipboard via `SetDataObject(copy:true)`), Modifier vorher freigeben, Restore verzoegert (T1/T2, C2/C3).
  - `ValuePattern.SetValue` bleibt nur der **optionale schnelle Pfad** fuer den seltenen Fall, dass ein Feld es freigibt (U8).
- **Quelle:** Chromium Accessibility Overview <https://chromium.googlesource.com/chromium/src/+/main/docs/accessibility/overview.md>; Microsoft Learn ValuePattern.SetValue <https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.valuepattern.setvalue?view=windowsdesktop-8.0>

---

## F) Fensterfindung / HWND-Hierarchie

### F1. Einstufiges `FindWindowEx` findet `Chrome_RenderWidgetHostHWND` nie ⭐ HAEUFIG (wahrscheinliche Hauptursache)
- **Symptom:** `FindWindowEx(top, NULL, "Chrome_RenderWidgetHostHWND", NULL)` gibt immer `NULL` zurueck (`GetLastError` = 0, "nur nicht gefunden"). Kein Ziel-HWND → kein Text.
- **Ursache:** `FindWindowEx` durchsucht laut Doku **nur direkte Kindfenster** ("searches only direct child windows. It does not search other descendants."). In modernem Electron/Chromium liegt das Render-Widget aber mehrere Ebenen tief: Top-HWND → `Chrome_WidgetWin_1` → `Chrome_WidgetWin_0` / "Intermediate D3D Window" → `Chrome_RenderWidgetHostHWND`.
- **Versionen:** Per Design (alle Windows seit 2000); Verschachtelung gilt fuer alle Aura-/Chromium-Builds.
- **FIX:** Rekursiv absteigen. `EnumChildWindows(top, callback, …)` EINMAL aufrufen — das enumeriert laut Doku bereits ALLE Nachkommen rekursiv — im Callback per `GetClassName` auf `Chrome_RenderWidgetHostHWND` filtern. Wartungsaermer als eine feste FindWindowEx-Kaskade, weil die Zwischen-Klassennamen wechseln (F3).
- **Quelle:** Microsoft Learn FindWindowEx (Remarks) <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-findwindowexa>; Raymond Chen "EnumChildWindows enumerates recursively" <https://devblogs.microsoft.com/oldnewthing/20070116-04/?p=28393>

### F2. Doppelte Rekursion um `EnumChildWindows` (Enkel mehrfach gezaehlt)
- **Symptom:** Eigene manuelle Rekursion um `EnumChildWindows` → dasselbe `Chrome_RenderWidgetHostHWND` taucht mehrfach auf, Text wird doppelt gesendet.
- **Ursache:** `EnumChildWindows` rekursiert bereits selbst ("if a child window has created child windows of its own, EnumChildWindows enumerates those windows as well"). Wer zusaetzlich selbst rekursiv laeuft, zaehlt Enkel doppelt.
- **Versionen:** Per Design / versionsunabhaengig.
- **FIX:** `EnumChildWindows` genau EINMAL auf dem Top-HWND, im Callback flach filtern — NICHT im Callback erneut `EnumChildWindows` pro Kind aufrufen.
- **Quelle:** The Old New Thing <https://devblogs.microsoft.com/oldnewthing/20070116-04/?p=28393>

### F3. Klassennamen der Zwischenfenster wechseln je Chromium-/Electron-Version und GPU-Modus
- **Symptom:** Eine fest verdrahtete Kaskade mit `Chrome_WidgetWin_0` funktioniert auf einem Rechner, liefert auf einem anderen `NULL`.
- **Ursache:** `_0` vs `_1` haengt von Reihenfolge/Anzahl registrierter Fensterklassen im Prozess ab (Electron registriert andere/mehr Klassen als reines Chrome). Zusaetzlich existiert das "Intermediate D3D Window" (GPU-Prozess, DirectComposition) je nach GPU-Modus oder fehlt. Chromium-Entwickler ausdruecklich: "Child windows are implementation details. You should not be relying on them." `Chrome_RenderWidgetHostHWND` selbst ist ein bewusst beibehaltener Kompatibilitaets-Hack (fuer aeltere Synaptics-Treiber).
- **Versionen:** Versionsunabhaengig instabil (explizit als Implementierungsdetail deklariert).
- **FIX:** Zwischen-Klassennamen NIE fest verdrahten. Nur die Blatt-Klasse `Chrome_RenderWidgetHostHWND` per Vollbaum-`EnumChildWindows` ansteuern. Strategisch: HWND-Pfad ganz vermeiden, Accessibility-API nutzen (Chromium-Team empfiehlt selbst "Use MSAA or UI Automation if possible").
- **Quelle:** chromium-dev Thread (Scott Violet / Carlos Pizano) <https://groups.google.com/a/chromium.org/g/chromium-dev/c/hmBh5YHjOFY>; GPU-Compositing <https://www.chromium.org/developers/design-documents/gpu-accelerated-compositing-in-chrome/>

### F4. Mehrere HWNDs pro Fenster — nur eines nimmt Eingaben an
- **Symptom:** Mehrere passende Kindfenster; Text ans erste/falsche → nichts passiert. Oder GPU-/Browser-Prozess-HWND statt aktivem Renderer getroffen.
- **Ursache:** Ein sichtbares Electron-Fenster verteilt sich auf mehrere Prozesse (Browser/GPU/Renderer) mit eigenen HWNDs. Historisch ein Kindfenster pro Tab; nur das aktive hat gesetzte Dimensionen, inaktive sind 0-gross. Nur das korrekte, aktive `Chrome_RenderWidgetHostHWND` verarbeitet die Eingabe.
- **Versionen:** Versionsunabhaengig (Multiprozess-Architektur).
- **FIX:** Nach `Chrome_RenderWidgetHostHWND` filtern UND auf `IsWindowVisible` + nicht-leeres `GetClientRect` pruefen. Robuster: Ziel ueber UIA (fokussiertes Element) bestimmen statt ueber HWND-Reihenfolge.
- **Quelle:** chromium-dev <https://groups.google.com/a/chromium.org/g/chromium-dev/c/hmBh5YHjOFY>; electron/electron #5122 (zcbenz: child windows enumerieren) <https://github.com/electron/electron/issues/5122>

### F5. Off-Screen-Rendering (OSR): kein echtes `Chrome_RenderWidgetHostHWND`
- **Symptom:** `EnumChildWindows` findet KEIN Render-HWND, obwohl das Fenster sichtbar ist. Jede HWND-Methode scheitert grundsaetzlich.
- **Ursache:** Bei `webPreferences.offscreen: true` rendert Chromium in eine Bitmap/GPU-Textur — kein reales Kind-Fenster. (Hinweis: Claude Desktop laeuft normalerweise NICHT im OSR-Modus; relevant nur, falls die Suche systematisch leer bleibt trotz korrekter Traversierung.)
- **Versionen:** Per Design fuer OSR-Modus.
- **FIX:** Nicht ueber Win32-HWND, sondern ueber UIA/IAccessible2 einfuegen — funktioniert unabhaengig davon, ob ein Render-HWND existiert (= direktes Windows-Pendant zur macOS-AXUIElement-Loesung).
- **Quelle:** Electron OSR-Doku <https://github.com/electron/electron/blob/main/docs/tutorial/offscreen-rendering.md>; electron #8392 (geschlossen) <https://github.com/electron/electron/issues/8392>

### F6. `GetWindowRect`/`GetClientRect` auf verschachteltem Render-HWND: 0-Groesse oder Koordinatenmix
- **Symptom:** Render-HWND gefunden, aber `GetClientRect` liefert leeres Rechteck, oder Klick landet im Leeren.
- **Ursache:** (1) Inaktive/verdeckte Render-HWNDs haben bewusst keine Dimensionen → leeres Rect. (2) `GetClientRect` = Client-relativ (0,0), `GetWindowRect` = Screen-Koordinaten; Verwechslung → falsche Position. Bei tiefer Verschachtelung zusaetzlich `ClientToScreen`/`MapWindowPoints` noetig.
- **Versionen:** Per Design.
- **FIX:** `IsWindowVisible` + nicht-leeres `GetClientRect` pruefen; fuer absolute Positionen `GetWindowRect` (Screen) oder Client-Punkte explizit umrechnen. Am robustesten: gar nicht koordinatenbasiert, sondern Fokus + Einfuegen via UIA (siehe auch M4 zu Extended Frame Bounds).
- **Quelle:** Microsoft Learn GetWindowRect <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getwindowrect>

---

## A) Fokus / Vordergrund / Aktivierung

### A1. `SetForegroundWindow` scheitert leise — nur Taskleisten-Blinken ⭐ HAEUFIG
- **Symptom:** `SetForegroundWindow(claudeHwnd)` gibt `false` (selten `true`), Claude kommt nicht nach vorne, Taskleisten-Button blinkt. Anschliessender Text landet nirgends.
- **Ursache:** Foreground-Lock seit Windows 2000/XP: Aktivierung nur erlaubt, wenn u. a. der aufrufende Prozess SELBST Vordergrund ist ODER das letzte Eingabe-Ereignis erhalten hat ODER der Lock-Timeout abgelaufen ist. Ein Hintergrund-Overlay erfuellt keine Bedingung → System blinkt nur. Microsoft: "It is possible for a process to be denied the right to set the foreground window even if it meets these conditions." (macOS kennt das nicht.)
- **Versionen:** Per Design, Win2000–Win11 (Win10/11 verschaerft).
- **FIX:** Vor der Aktivierung die "letzte Eingabe"-Qualifikation selbst herstellen: (1) bei `IsIconic` → `ShowWindow(SW_RESTORE)`; (2) eigenen Input erzeugen — ein ALT-Tastendruck per `SendInput` (down+up) setzt laut `LockSetForegroundWindow`-Doku den Lock vom System selbst zurueck; (3) `SetForegroundWindow`, Rueckgabe pruefen, per `GetForegroundWindow()` verifizieren. Kein globaler Timeout-Hack noetig (A4).
- **Quelle:** Microsoft Learn SetForegroundWindow <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow>; PowerToys PR #1282 (SendInput-Hack) <https://github.com/microsoft/PowerToys/pull/1282>

### A2. `AttachThreadInput`-Workaround haengt am zweiten `SetForegroundWindow` (Deadlock)
- **Symptom:** Input-Queues anhaengen + erneut `SetForegroundWindow` → die Funktion "haengt hier", WPF-UI friert ein.
- **Ursache:** `AttachThreadInput` koppelt zwei Input-Queues; beide Threads teilen Fokus/Aktiv/Capture/Z-Order und arbeiten Eingaben seriell ab. Blockiert der fremde (Chromium-)Thread, wartet der eigene unbegrenzt. Raymond Chen fuehrt "attached input queues" als eines der fuenf gefaehrlichsten Win32-Dinge.
- **Versionen:** Per Design (von Microsoft abgeraten).
- **FIX:** Fuer die Aktivierung `AttachThreadInput` ganz meiden → ALT-SendInput-Weg (A1). Wenn unverzichtbar: so kurz wie moeglich attached bleiben, NIE ueber blockierende Operationen, sauber im `finally` mit `AttachThreadInput(…, FALSE)` detachen, nicht den UI-Thread mit Clipboard-/Netz-Arbeit anhaengen.
- **Quelle:** Raymond Chen "I warned you: the dangers of attaching input queues" <https://devblogs.microsoft.com/oldnewthing/20080801-00/?p=21393>; Microsoft Learn AttachThreadInput <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-attachthreadinput>

### A3. `AllowSetForegroundWindow` / `ASFW_ANY`: Recht verfaellt beim naechsten Input
- **Symptom:** Nach `AllowSetForegroundWindow` klappt Aktivierung kurz, dann nicht mehr — sobald der Benutzer tippt/klickt oder ein anderer Prozess die Funktion aufruft.
- **Ursache:** Nur ein gerade berechtigter Prozess kann das Recht per PID weiterreichen; es verfaellt "the next time that either the user generates input … or the next time a process calls AllowSetForegroundWindow". `ASFW_ANY` ist genauso fluechtig. Claude/Chromium ruft es nicht fuer uns auf.
- **Versionen:** Per Design, Win2000+.
- **FIX:** Falls genutzt: aus einem gerade-Vordergrund-Prozess (z. B. dem Overlay im Moment des Klicks) das Ziel-PID freigeben, UNMITTELBAR davor, ohne dazwischenliegende Benutzereingabe. Nicht auf langlebige Freigaben verlassen. Bevorzugt: ALT-SendInput-Trick (A1), der prozessintern berechtigt.
- **Quelle:** Microsoft Learn AllowSetForegroundWindow <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-allowsetforegroundwindow>; LockSetForegroundWindow <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-locksetforegroundwindow>

### A4. `SPI_SETFOREGROUNDLOCKTIMEOUT=0`-Trick ist global und fragil
- **Symptom:** Timeout vor `SetForegroundWindow` auf 0 setzen und danach zuruecksetzen — mal klappt's, mal bleibt der Fokusschutz systemweit aus (bei Crash/Race zwischen den beiden Aufrufen).
- **Ursache:** `SPI_SETFOREGROUNDLOCKTIMEOUT` ist eine GLOBALE Einstellung, kein prozesslokaler Schalter; der `SPIF_SENDCHANGE`-Broadcast wird nicht garantiert sofort verarbeitet (Timing-Race), und Verhalten schwankt je Windows-Version.
- **Versionen:** Wirkung versionsabhaengig (Win98/2000 bis Win11).
- **FIX:** Moeglichst meiden. Falls genutzt: alten Wert per `SPI_GETFOREGROUNDLOCKTIMEOUT` lesen und im `try/finally` exakt restaurieren. Besser durch lokalen ALT-SendInput-Trick (A1) ersetzen.
- **Quelle:** Microsoft Learn SystemParametersInfo <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-systemparametersinfoa>; GLFW-Diskussion <https://discourse.glfw.org/t/why-is-spi-setforegroundlocktimeout-set-to-zero-on-glfwinit-win32/1874>

### A5. `BringWindowToTop`/`SetForegroundWindow` ohne `SW_RESTORE` aktiviert minimiertes Ziel nicht
- **Symptom:** Claude ist minimiert/im Hintergrund; `BringWindowToTop`/`SetForegroundWindow` allein bringen es nicht sichtbar in den Vordergrund, oder ohne Tastaturfokus → Text ins Leere.
- **Ursache:** `BringWindowToTop` aendert nur Z-Order, nicht die Aktivierung; ein minimiertes Fenster muss erst per `ShowWindow(SW_RESTORE)` wiederhergestellt werden. Falsche Reihenfolge → Flackern/Fokusverlust.
- **Versionen:** Per Design.
- **FIX (bewaehrte Reihenfolge):** `if (IsIconic) ShowWindow(SW_RESTORE)` → ALT-SendInput (A1) → `SetForegroundWindow` → ggf. `BringWindowToTop`. Danach `WaitForInputIdle(prozess, timeout)` statt festem `Sleep`, sonst geht die erste Eingabe verloren (A6).
- **Quelle:** Microsoft Learn SetForegroundWindow / ShowWindow <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-showwindow>

### A6. `SetFocus` ueber Prozessgrenzen = NULL; erste Eingabe geht durch Race verloren
- **Symptom:** `SetFocus(childHwnd)` auf das fremde Feld gibt NULL, kein Fokus. Oder: Aktivierung klappt, aber direkt danach gesendeter Text wird verschluckt.
- **Ursache:** `SetFocus` wirkt nur in der Input-Queue des aufrufenden Threads ("should not be used to set the keyboard focus to a window associated with another thread's message queue"); ueber Prozessgrenze → NULL. Zusaetzlich Timing-Race: nach `SetForegroundWindow` braucht das Ziel Zeit bis eingabebereit.
- **Versionen:** Per Design.
- **FIX:** Kein prozessuebergreifendes `SetFocus`. Nur das Top-Level-Fenster sauber aktivieren (A1/A5), dann den natuerlichen Feld-Fokus nutzen. Vor dem Tippen `WaitForInputIdle` statt Sleep. Gezielte Feld-Fokussierung lieber per UIA `SetFocus()` (U8) als per Win32-SetFocus.
- **Quelle:** Microsoft Learn SetFocus <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setfocus>; PowerToys #1310 <https://github.com/microsoft/PowerToys/issues/1310>

### A7. WPF-Overlay (Topmost / `WS_EX_NOACTIVATE` / `ShowActivated=false`) stiehlt/verliert Fokus
- **Symptom:** Das Overlay aktiviert sich beim Erscheinen/Klick selbst → Claude wird gar nicht Vordergrund; oder mit `ShowInTaskbar=false` rutscht es trotz `Topmost` unter andere Fenster.
- **Ursache:** Standard-WPF-Fenster aktivieren sich beim Anzeigen; dann ist das Overlay Vordergrund und der folgende `SetForegroundWindow(claude)` konkurriert. `ShowActivated=false` wirkt NUR, wenn VOR dem Anzeigen gesetzt. `ShowInTaskbar=false` erzeugt ein verstecktes Owner-Window ohne `WS_EX_TOPMOST`.
- **Versionen:** WPF / .NET (auch .NET 8).
- **FIX:** Overlay als echtes Nicht-aktivierendes Fenster: `ShowActivated=false` VOR `Show()`, zusaetzlich `WS_EX_NOACTIVATE` im `SourceInitialized` setzen; `WM_MOUSEACTIVATE` → `MA_NOACTIVATE`. Aktivierungs-Sequenz fuer Claude erst ausloesen, NACHDEM das Overlay als nicht-aktivierend feststeht (so bleibt der "letzte Input" beim Ziel). Siehe Schwester-Almanach `windows-overlay.md` (A-Sektion).
- **Quelle:** Rick Strahl "Window Activation Headaches in WPF" <https://weblog.west-wind.com/posts/2020/Oct/12/Window-Activation-Headaches-in-WPF>; Microsoft Learn Window.ShowActivated <https://learn.microsoft.com/en-us/dotnet/api/system.windows.window.showactivated>

---

## T) Tastatur-Injektion (Strg+V)

### T1. `keybd_event` ist veraltet und liefert keinen Scancode → Chromium verwirft das Paste ⭐ HAEUFIG
- **Symptom:** Ctrl↓ V↓ V↑ Ctrl↑ per `keybd_event`: in Notepad klappt's, in Claude Desktop nichts — kein Fehler.
- **Ursache:** `keybd_event` ist von Microsoft "superseded" und setzt typisch `bScan=0`. Der Blink-Renderer baut `KeyboardEvent` (inkl. `code`) aus dem Scancode; fehlt ein plausibler Scancode, wird die Sequenz nicht als echte Tastenkombi interpretiert. Native Edit-Felder sind toleranter.
- **Versionen:** Per Design (seit Win2000 abgeloest).
- **FIX:** `SendInput` mit `INPUT[]`-Array; pro Taste `wScan = MapVirtualKey(vk, MAPVK_VK_TO_VSC)` + Flag `KEYEVENTF_SCANCODE`. Reihenfolge im Array: Ctrl↓, V↓, V↑, Ctrl↑ als eine Charge.
- **Quelle:** Microsoft Learn keybd_event ("superseded. Use SendInput instead.") <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-keybd_event>

### T2. `SendInput` ohne `KEYEVENTF_SCANCODE`/`MapVirtualKey`: Virtual-Key allein reicht nicht
- **Symptom:** Auch nach Umstieg auf `SendInput` kommt Strg+V nicht an, wenn nur `wVk` gesetzt ist und `wScan=0`.
- **Ursache:** Der Virtual-Key wechselt je Layout, der Scancode bleibt konstant; Chromium/Blink (wie RDP) braucht den echten Scancode, um `code` zu rekonstruieren.
- **Versionen:** Per Design.
- **FIX:** `wScan = MapVirtualKey(...)` + `KEYEVENTF_SCANCODE`. Dann gilt: "If KEYEVENTF_SCANCODE is specified, wScan identifies the key and wVk is ignored." Scancode-Eingabe ist layout-unabhaengig und am robustesten.
- **Quelle:** Microsoft Learn KEYBDINPUT <https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput>

### T3. `KEYEVENTF_EXTENDEDKEY` und zu schnelles KeyDown/KeyUp-Timing
- **Symptom:** Statt Paste erscheint "v" im Feld oder nichts; sporadisch.
- **Ursache:** (a) Rechte Modifier/Pfeiltasten/Numpad brauchen `KEYEVENTF_EXTENDEDKEY` (0xE0-Praefix) — fuer V/linke Ctrl nicht, aber sobald rechte Modifier genutzt werden. (b) Asynchroner Renderer-Eventloop: feuert V↓ zu dicht hinter Ctrl↓, sieht Blink Ctrl noch nicht als gedrueckt.
- **Versionen:** Per Design / bei Chromium verschaerft.
- **FIX:** Reihenfolge Ctrl↓ → 10–30 ms Pause → V↓ → V↑ → Ctrl↑. Linke Ctrl (`VK_LCONTROL`) ohne, rechte Modifier MIT `EXTENDEDKEY`. (AHK-Pendant: `SendEvent` mit `SetKeyDelay` statt `SendInput` im Nullzeit-Block, siehe T7.)
- **Quelle:** Microsoft Learn KEYBDINPUT (Extended-Key) <https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput>; AutoHotkey-Forum (Timing) <https://www.autohotkey.com/boards/viewtopic.php?t=92380>

### T4. UIPI/UAC-Elevation: elevierter Sender → nicht-elevierte App = leiser Fehlschlag ⭐ HAEUFIG
- **Symptom:** Als Admin gestartetes WPF-Tool → nichts kommt an; ohne Elevation klappt es (oder umgekehrt). `GetLastError`/Rueckgabe melden Erfolg.
- **Ursache:** User Interface Privilege Isolation (UIPI): Eingaben/Window-Messages nur an Prozesse mit gleichem oder niedrigerem Integritaetslevel. Sender High-IL, Claude Medium-IL (oder umgekehrt) → `SendInput` wird still geblockt. Explizit: "Neither GetLastError nor the return value will indicate the failure was caused by UIPI." macOS kennt kein UIPI → erklaert das Plattform-Delta mit.
- **Versionen:** Per Design seit Vista.
- **FIX:** Integritaetslevel ANGLEICHEN — Sender NICHT als Admin starten (beide Medium). Muss der Sender elevated sein: Claude ebenfalls elevated, ODER den Sende-Teil in einen Medium-IL-Hilfsprozess auslagern. Saubere, aber aufwaendige Loesung: `uiAccess="true"`-Manifest (braucht Code-Signatur + Program-Files-Pfad). Kein offener UAC-Dialog/gesperrter Desktop zur Sendezeit.
- **Quelle:** Microsoft Learn UIPI-Issues (Power Automate) <https://learn.microsoft.com/en-us/troubleshoot/power-platform/power-automate/desktop-flows/ui-automation/uipi-issues>; Windows Integrity Mechanism <https://learn.microsoft.com/en-us/previous-versions/dotnet/articles/bb625963(v=msdn.10)>

### T5. `WM_PASTE`/`WM_SETTEXT` direkt ans Control funktioniert bei Chromium NICHT
- **Symptom:** `WM_PASTE`/`WM_SETTEXT` per `SendMessage` (oder AHK `ControlSend`) ans Eingabe-HWND erreicht nichts.
- **Ursache:** `WM_PASTE` adressiert ein natives Edit-Control/Combobox. Chromium rendert das Feld als HTML-`contenteditable` in EINER Render-Flaeche — kein adressierbares Edit-Control; die Nachricht laeuft ins Leere (gleicher Grund, warum `ControlSend` bei Electron oft versagt).
- **Versionen:** Per Design (Chromium-Architektur).
- **FIX:** `WM_PASTE`/`ControlSend` aufgeben. Fokus-gebundener Weg: Fenster aktivieren (A1), kurz warten, Scancode-`SendInput` Strg+V (T1/T2). Chromium reagiert nur auf vom System verteilte, fokus-gebundene Tastatureingabe.
- **Quelle:** Microsoft Learn WM_PASTE <https://learn.microsoft.com/en-us/windows/win32/dataxchg/wm-paste>

### T6. C#-`SendKeys.Send/SendWait("^v")` gegen Chromium unzuverlaessig
- **Symptom:** `SendKeys.SendWait("^v")` paste-t mal, mal nicht — speziell bei Chromium und grossen Texten.
- **Ursache:** Microsoft: "The SendKeys class is susceptible to timing issues"; die neuere Implementierung "will not wait for messages to be processed when they are sent to another process" — gegen einen Fremdprozess entfaellt also die Synchronisierung. `^v` pastet nur Text, kein Bild.
- **Versionen:** .NET (alle, inkl. .NET 8).
- **FIX:** `SendKeys` fuer Chromium meiden → P/Invoke `SendInput` mit Scancode (T1/T2), oder Clipboard setzen + nur Paste-Trigger feuern. Wenn `SendKeys` bleiben muss: Legacy-Implementierung (`SendKeys=JournalHook` in app.config) + feste Puffer.
- **Quelle:** Microsoft Learn SendKeys <https://learn.microsoft.com/en-us/dotnet/api/system.windows.forms.sendkeys?view=windowsdesktop-8.0>

### T7. AutoHotkey-Praxis: `SendInput ^v` schluckt, `SendEvent`/`SendPlay` greift
- **Symptom:** Breit berichtet: `SendInput ^v` kommt in Electron/Chromium nicht an, `Send`/`SendEvent ^v` schon.
- **Ursache:** `SendInput` feuert alle Events atomar/ultraschnell — asynchrone Chromium-Renderer bekommen den Modifier-Zustand dazwischen nicht mit. `SendEvent`/`SendPlay` mit `SetKeyDelay` respektieren den Tastenrhythmus. (Spiegelung desselben Win32-Problems, das C#/WPF trifft.)
- **Versionen:** AHK v1/v2; Electron/Chromium generell.
- **FIX (uebertragen auf C#):** In der eigenen `SendInput`-Sequenz mit dem `time`-Feld bzw. kleinen `Sleep`-Pausen zwischen den Events arbeiten, statt alles im Nullzeit-Block zu feuern (vgl. T3). `ControlSend` vermeiden (T5).
- **Quelle:** AutoHotkey-Forum <https://www.autohotkey.com/boards/viewtopic.php?t=92380>; AHK v2 "How to Send Keystrokes" <https://www.autohotkey.com/docs/v2/howto/SendKeys.htm>

### T8. `LLKHF_INJECTED`-Flag: Schutz-/Anti-Cheat-Software kann injizierte Eingaben filtern
- **Symptom:** `SendInput` technisch korrekt (Scancode, Timing, IL stimmen), trotzdem ignoriert die Eingabe — als wuerde nur "echte" Tastatur akzeptiert.
- **Ursache:** Windows markiert jedes `SendInput`/`keybd_event`-Event in Low-Level-Hooks mit `LLKHF_INJECTED` (ggf. `LLKHF_LOWER_IL_INJECTED`). Endpoint-/Anti-Cheat-Software (Vanguard, EAC, EDR) kann per `WH_KEYBOARD_LL`-Hook injizierte Eingaben aussortieren, bevor sie Chromium erreichen.
- **Versionen:** Per Design; verschaerft bei aktivem kernelnahem Schutz.
- **FIX:** Das Flag laesst sich aus User-Mode nicht legitim entfernen. Praktisch: pruefen, ob input-filternde Schutzsoftware laeuft; IL-Mismatch zuerst loesen (T4, manche Filter greifen nur dort); wo erlaubt Ausnahme eintragen; sonst auf nutzergetriggertes Paste / offizielle API ausweichen.
- **Quelle:** Microsoft Learn KBDLLHOOKSTRUCT <https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-kbdllhookstruct>

---

## M) Maus / feste Koordinaten / DPI / Multi-Monitor

### M1. Blinder Klick auf feste Prozent-Koordinaten verfehlt das wandernde Feld ⭐ HAEUFIG
- **Symptom:** Klick auf 55 %/86 % landet im Nachrichtenverlauf statt im Feld; Strg+V fuegt nichts ein. "Zufaellig" mal ja, mal nein.
- **Ursache:** Das Feld hat keine feste Position. Bei Cowork startet der Composer mittig und rutscht nach unten, sobald Aufgaben erscheinen; bei Chat waechst er mehrzeilig. Ein `ResizeObserver` synchronisiert das Layout dynamisch. Zusaetzlich folgt die View dem Element mit Latenz (electron #18342).
- **Versionen:** Per Design (dynamisches DOM-Layout).
- **FIX:** Nicht blind klicken. Feld per UIA lokalisieren (`ControlType.Edit`/`Document` → `BoundingRectangle` → Mittelpunkt) oder ganz ohne Klick fokussieren (`SetFocus()`), dann Strg+V. UIA liefert die aktuelle Position auch bei verschobenem Layout. Falls doch Klick: Position unmittelbar davor frisch abfragen, kurze Settle-Time gegen ResizeObserver-Lag.
- **Quelle:** electron/electron #18342 (offen) <https://github.com/electron/electron/issues/18342>; MDN ResizeObserver <https://developer.mozilla.org/en-US/docs/Web/API/ResizeObserver>

### M2. `SetCursorPos`/`mouse_event` physisch, `GetWindowRect` virtualisiert → Versatz auf High-DPI ⭐ HAEUFIG
- **Symptom:** Auf skaliertem Monitor (125/150/175 %) klickt der Punkt konsistent zu weit oben-links; Versatz waechst mit der Skalierung. Auf 100 % kein Fehler ("funktioniert auf meinem Rechner").
- **Ursache:** Ist der aufrufende Prozess DPI-unaware / nur System-DPI-aware, virtualisiert Windows DPI-sensitive Rueckgaben: `GetWindowRect` ist "virtualized for DPI" → logische (96-DPI) Koordinaten. `SetCursorPos`/`mouse_event` erwarten PHYSISCHE Pixel. Man fuettert logische Werte in eine physische API → der Faktor fehlt.
- **Versionen:** Per Design seit Win8.1/10 High-DPI-Modell.
- **FIX:** (a) Prozess Per-Monitor-V2-aware machen (M3) → `GetWindowRect` liefert physische Pixel passend zu `SetCursorPos`; ODER (b) logische Rect explizit mit dem Skalierungsfaktor des Zielmonitors multiplizieren; ODER (c) `SetPhysicalCursorPos`/`GetPhysicalCursorPos` nutzen. DPI-Awareness explizit setzen, nicht darauf vertrauen.
- **Quelle:** Microsoft Learn GetWindowRect ("virtualized for DPI") <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getwindowrect>; High-DPI Improvements <https://learn.microsoft.com/en-us/windows/win32/hidpi/high-dpi-improvements-for-desktop-applications>

### M3. Fehlende Per-Monitor-V2-Awareness im app.manifest → physische/logische Pixel gemischt
- **Symptom:** Versatz nur auf Sekundaermonitoren mit anderer Skalierung, oder aendert sich beim Fensterverschieben zwischen Monitoren.
- **Ursache:** Ohne Deklaration laeuft ein .NET-8-WPF-Prozess nur System-DPI-aware; auf abweichend skalierten Monitoren bleiben Rueckgaben virtualisiert, waehrend der Mauszeiger physisch arbeitet.
- **Versionen:** Per Design; Per-Monitor-V2 ab Win10 1703.
- **FIX:** app.manifest `<dpiAwareness>PerMonitorV2</dpiAwareness>` bzw. beim Start `SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2)`. Fuer Umrechnungen `LogicalToPhysicalPointForPerMonitorDPI` / `PhysicalToLogicalPointForPerMonitorDPI`. Per-Monitor-DPI ueber `GetDpiForWindow` abfragen, `GetSystemMetricsForDpi` statt `GetSystemMetrics`.
- **Quelle:** Microsoft Learn High-DPI Desktop Development <https://learn.microsoft.com/en-us/windows/win32/hidpi/high-dpi-desktop-application-development-on-windows>; LogicalToPhysicalPointForPerMonitorDPI <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-logicaltophysicalpointforpermonitordpi>

### M4. `GetWindowRect` enthaelt unsichtbaren DWM-Rahmen/Schatten → Prozent-Basis verfaelscht
- **Symptom:** Selbst nach DPI-Korrektur sitzt der Punkt leicht daneben (einige px), weil die Bezugsflaeche zu gross ist.
- **Ursache:** Seit Vista/10 schliesst `GetWindowRect` unsichtbare Resize-Raender/DWM-Schatten ein → die Rect ist groesser als das sichtbar gerenderte Fenster; Prozentrechnung darauf verschiebt jeden Punkt.
- **Versionen:** Per Design seit Vista.
- **FIX:** Sichtbare Bounds via `DwmGetWindowAttribute(hWnd, DWMWA_EXTENDED_FRAME_BOUNDS, …)`; Prozentrechnung darauf. Zwei Fallstricke: Extended Frame Bounds sind NICHT DPI-adjustiert (physische Pixel — konsistent mit M2/M3 halten, nicht zusaetzlich skalieren); Aufruf erst NACH erstem Anzeigen des Fensters.
- **Quelle:** Microsoft Learn GetWindowRect (DWMWA_EXTENDED_FRAME_BOUNDS) <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getwindowrect>; Cyotek-Beispiel <https://www.cyotek.com/blog/getting-a-window-rectangle-without-the-drop-shadow>

### M5. Multi-Monitor: negative/virtuelle Koordinaten; absolutes `SendInput` nur auf Primaermonitor
- **Symptom:** Klick landet auf dem Primaermonitor, obwohl Claude auf einem zweiten (links/oben) Monitor ist.
- **Ursache:** (a) `SetCursorPos` erwartet absolute Koordinaten des virtuellen Desktops, die auf Sekundaermonitoren negativ sein koennen. (b) `MOUSEEVENTF_ABSOLUTE` mappt 0..65535 standardmaessig nur auf den Primaermonitor; ohne `MOUSEEVENTF_VIRTUALDESK` werden Zweitmonitor-Punkte falsch projiziert.
- **Versionen:** Per Design.
- **FIX:** Fuer absolute Bewegung `MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_VIRTUALDESK` und gegen die gesamten virtuellen Masse normalisieren (`SM_XVIRTUALSCREEN`/`SM_YVIRTUALSCREEN` Ursprung, `SM_CXVIRTUALSCREEN`/`SM_CYVIRTUALSCREEN`). Bei `SetCursorPos` echte (ggf. negative) Koordinaten ohne Clamping.
- **Quelle:** Microsoft Learn mouse_event (VIRTUALDESK) <https://learn.microsoft.com/en-us/windows/desktop/api/winuser/nf-winuser-mouse_event>

### M6. `mouse_event` ist veraltet → `SendInput`-Migration mit eigenen Fallen
- **Symptom:** Vereinzelt verschluckte/vertauschte Klick-Events; Down/Up an leicht unterschiedlichen Positionen.
- **Ursache:** `mouse_event` ist "superseded". Beim Umstieg: (1) Down/Up getrennt gesendet statt im einen `INPUT[]`-Array → Verschachtelung; (2) `dx/dy`/`mouseData` in manchen Bindings als `uint` → keine negativen Werte (win32metadata #933); (3) ohne `VIRTUALDESK` Primaermonitor-Mapping (M5); (4) `SendInput` ist nicht automatisch "DPI-richtig".
- **Versionen:** `mouse_event` deprecated; Bindings-Bug offen.
- **FIX:** `SendInput`, Down+Up im selben Array (atomar), `MOUSEEVENTF_MOVE|ABSOLUTE|VIRTUALDESK` + Normalisierung (M5), negative Monitorkoordinaten als `int` marshallen, DPI-Awareness explizit setzen (M3).
- **Quelle:** Microsoft Learn mouse_event <https://learn.microsoft.com/en-us/windows/desktop/api/winuser/nf-winuser-mouse_event>; win32metadata #933 (offen) <https://github.com/microsoft/win32metadata/issues/933>

### M7. Simulierter Klick setzt Fokus auf das falsche Kind-HWND
- **Symptom:** Klick "trifft" sichtbar, aber Strg+V landet nirgends, oder Paste geht in die falsche Render-View.
- **Ursache:** Mehrere `Chrome_RenderWidgetHostHWND` (eines je Tab/RenderWidget), teils transparent. Ein blinder Festkoordinaten-Klick setzt Fokus auf das geometrisch oben liegende Kind — nicht zwingend das mit dem Eingabefeld. `SetForegroundWindow` gehoert NICHT auf Kind-Fenster.
- **Versionen:** Per Design (Chromium-Fensterarchitektur).
- **FIX:** Nicht blind aufs Top-Level klicken. Edit-/Document-Element via UIA aufloesen und gezielt `SetFocus()` — steuert das korrekte Render-Widget an, unabhaengig von der HWND-Geometrie. Falls Klick noetig: erst Top-Level aktivieren (nie Kind-HWND), dann auf den per UIA ermittelten Feld-Mittelpunkt klicken.
- **Quelle:** chromium-dev <https://groups.google.com/a/chromium.org/g/chromium-dev/c/hmBh5YHjOFY>; Microsoft Learn SetForegroundWindow <https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow>

---

## C) Zwischenablage (Clipboard)

### C1. `CLIPBRD_E_CANT_OPEN` (0x800401D0) bei `Clipboard.SetText`/`SetDataObject` ⭐ HAEUFIG
- **Symptom:** `COMException (0x800401D0): OpenClipboard Failed`, oft im `Flush()`-Schritt; reproduzierbar nach vielen schnellen `SetText`-Aufrufen.
- **Ursache:** Die Zwischenablage ist global single-owner; ein anderer Prozess haelt sie im Flush-Moment offen. Bekannte Taeter: Windows-Zwischenablageverlauf (`cbdhsvc`), AV mit Clipboard-Schutz (ESET `eOPPFrame.exe`), RDP-Clip-Monitore, Klipboard-Manager, Skype — und Chromium/Electron (Claude Desktop) selbst als Clipboard-Listener, der direkt nach dem Set liest. WPF nutzt `OleServicesContext`+delayed rendering und ist anfaelliger als WinForms. `GetOpenClipboardWindow()` liefert oft 0.
- **Versionen:** .NET 8, Win10/11; per Design ein Race (dotnet/wpf #9901 OPEN).
- **FIX:** Retry-Schleife (5–10 Versuche, `Thread.Sleep(10)`), `ExternalException`/`COMException` fangen — Lese- UND Schreibseite. Robustere Alternative: `System.Windows.Forms.Clipboard` statt `System.Windows.Clipboard` (praktisch kaum betroffen). Nicht auf `GetOpenClipboardWindow()` zur Diagnose verlassen.
- **Quelle:** dotnet/wpf #9901 (OPEN, Root-Cause-Analyse) <https://github.com/dotnet/wpf/issues/9901>; Microsoft Q&A <https://learn.microsoft.com/en-us/answers/questions/1695747/>

### C2. Race: Save → Set → Paste → Restore zu schnell → leeres Feld ⭐ HAEUFIG
- **Symptom:** Zielfeld bleibt leer oder bekommt den alten (restaurierten) Inhalt statt des neuen Texts.
- **Ursache:** Chromium/Electron liest den Clipboard-Inhalt beim Paste ASYNCHRON im Renderer, nicht synchron beim Strg+V. Schreibt das Overlay sofort danach die alte Zwischenablage zurueck, gewinnt das Restore das Rennen. PowerToys musste das Clipboard-Polling fuer Electron von 150 ms auf 500 ms erhoehen. Separater Fehler im selben Pfad: gehaltene Hotkey-Modifier nicht freigegeben → App bekam `Win+Shift+Ctrl+V` statt `Ctrl+V`.
- **Versionen:** Per Design (Renderer-Async); auf Windows/Electron staerker als auf macOS.
- **FIX:** (1) Vor dem Strg+V ALLE Modifier-Tasten loslassen, dann sauberes Strg+V. (2) Vor dem Restore 300–500 ms warten (Chromium braucht bis 500 ms) — besser an einen Clipboard-Update-Check koppeln (Sequenznummer/`WM_CLIPBOARDUPDATE`) statt fest zu schlafen. (3) Pragmatisch: Restore weglassen oder weit nach hinten verlagern, wenn der eingefuegte Text wichtiger ist.
- **Quelle:** microsoft/PowerToys PR #46486 (MERGED, "auto-copy failing on Electron/Chromium apps") <https://github.com/microsoft/PowerToys/pull/46486>; Microsoft Learn Clipboard Operations <https://learn.microsoft.com/en-us/windows/win32/dataxchg/clipboard-operations>

### C3. `Clipboard.SetText` ohne `copy:true`/`Flush()` → Inhalt verschwindet nach Prozess-Ende
- **Symptom:** Overlay setzt Text und schliesst sofort → Zwischenablage anschliessend leer, Paste findet nichts.
- **Ursache:** WPF legt per OLE mit delayed rendering ab: standardmaessig nur ein Zeiger aufs `IDataObject`, echte Daten erst bei Anfrage. Beendet sich der Prozess vorher, gibt es keinen Renderer mehr. `copy:false` loescht beim Exit; nur `copy:true`/`Flush()` materialisiert sofort.
- **Versionen:** Per Design (OLE delayed rendering), alle .NET.
- **FIX:** `Clipboard.SetDataObject(text, copy:true)` statt `SetText`, oder `Clipboard.Flush()` danach. Genau dieser Flush ist die Race-Stelle aus C1 → in dieselbe Retry-Schleife packen.
- **Quelle:** Microsoft Learn Clipboard.SetDataObject <https://learn.microsoft.com/en-us/dotnet/api/system.windows.clipboard.setdataobject?view=windowsdesktop-9.0>

### C4. OLE delayed rendering: `SetData` legt scheinbar nichts ab
- **Symptom:** Nach `SetData`/`SetDataObject` meldet ein direkter Lesetest leere/fehlende Formate.
- **Ursache:** `OleSetClipboard` registriert das `IDataObject` mit delayed rendering und erhoeht nur den Refcount; echte Bytes erst bei `OleFlushClipboard`/Format-Anfrage. Bricht das Message-Pumpen ab oder liest jemand im falschen Moment, wirkt es "leer".
- **Versionen:** Per Design (Win32/OLE).
- **FIX:** `Clipboard.Flush()`/`OleFlushClipboard` erzwingen, wenn der Inhalt sofort/unabhaengig vom Pump verfuegbar sein soll. Fuer Text einfaches `string`-DataObject mit `copy:true` (kein Custom-Format/BinaryFormatter — C6). STA-Thread mit laufender Dispatcher-Loop am Leben halten, bis das Paste durch ist.
- **Quelle:** Microsoft Learn Clipboard Operations / OleSetClipboard <https://learn.microsoft.com/en-us/windows/win32/dataxchg/clipboard-operations>

### C5. STA-Thread-Pflicht: Clipboard ausserhalb STA → `ThreadStateException`/leise Fehler
- **Symptom:** "Current thread must be set to single thread apartment (STA) mode before OLE calls" — typisch aus `Task.Run`/ThreadPool/Timer-Callback.
- **Ursache:** Clipboard geht ueber OLE, das zwingend STA verlangt. ThreadPool-Threads sind MTA. Apartment-State muss VOR Thread-Start gesetzt werden; nachtraegliches `SetApartmentState` wirft selbst.
- **Versionen:** Per Design (alle .NET).
- **FIX:** Clipboard immer auf dem UI-/STA-Thread (`Dispatcher.Invoke`). Dedizierter Hintergrund-Thread: vor `Start()` `SetApartmentState(ApartmentState.STA)`. Nie aus ThreadPool direkt zugreifen.
- **Quelle:** Microsoft Learn STAThreadAttribute <https://learn.microsoft.com/en-us/dotnet/api/system.stathreadattribute?view=net-9.0>

### C6. .NET-9-Clipboard-Regression: `SetDataObject` mit Custom-Typen/BinaryFormatter
- **Symptom:** `SetDataObject(customDataObject)` legt nichts ab; `GetDataPresent` = false; bei BinaryFormatter-Pfaden `PlatformNotSupportedException`. In .NET 8 ok.
- **Ursache:** .NET 9 hat BinaryFormatter entfernt (betrifft auch .NET 8, wenn dort deaktiviert) und auf `ComWrappers` umgestellt → implizites Zurueck-Casten auf abgeleitetes `DataObject` bricht; zusaetzlich ein Konstruktor-Bug (winforms #12789), gefixt im Feb-2025-Servicing. **Wichtig:** Reiner Text via `SetText`/`SetDataObject(string, copy:true)` ist NICHT betroffen.
- **Versionen:** .NET 9 (ab `9.0.100-rc.2`), Teil-Fix Feb-2025-Servicing; **Franks .NET 8 + Klartext → nicht betroffen.**
- **FIX:** Bei reinem Text bleiben (formatspezifische `SetText`, kein abgeleitetes `DataObject`). Falls Custom noetig: .NET-10-`SetDataAsJson<T>`/`TryGetData<T>` (JSON statt BinaryFormatter).
- **Quelle:** dotnet/winforms #12789 (CLOSED/fixed, Feb-Servicing) <https://github.com/dotnet/winforms/issues/12789>; dotnet/wpf #10049 <https://github.com/dotnet/wpf/issues/10049>; Migrations-Doku <https://learn.microsoft.com/en-us/dotnet/standard/serialization/binaryformatter-migration-guide/winforms-wpf-ole-guidance>

### C7. Zu viele schnelle Clipboard-Zugriffe → `OpenClipboard`-Fehler
- **Symptom:** Einzelaufrufe ok, aber viele `SetText`/`GetText` in dichter Folge loesen reproduzierbar `CLIPBRD_E_CANT_OPEN` aus — auch ohne Fremd-AV, mit aktivem Zwischenablageverlauf leichter.
- **Ursache:** Jedes Set triggert sofortiges Re-Rendering durch `cbdhsvc`, der dabei kurz sperrt; bei dichter Folge ueberlappen die Sperren mit dem eigenen Flush (dasselbe Race wie C1, frequenzgetrieben).
- **Versionen:** Per Design (Timing), Win10/11 mit Zwischenablageverlauf.
- **FIX:** Zugriffe serialisieren/entzerren (5–10 ms Pausen), Retry-Schleife (C1) als Netz, genau einmal setzen statt redundant.
- **Quelle:** dotnet/wpf #9901 (OPEN) <https://github.com/dotnet/wpf/issues/9901>

---

## U) UI Automation (Finden + Befuellen)

### U1. Chromium-Accessibility ist standardmaessig AUS / lazy → UIA-Baum leer ⭐ HAEUFIG
- **Symptom:** UIA sieht nur das Top-Window ohne Kinder; `FindFirst(Descendants, …)` auf das Feld = `null`. Inspect.exe zeigt leeren Inhalt, bis man hineinklickt.
- **Ursache:** Chromium aktiviert Accessibility nur "on demand" und lazy: Chrome feuert `EVENT_SYSTEM_ALERT` (Custom-ID 1); erst wenn ein AT mit `WM_GETOBJECT` antwortet (bzw. UIA/IAccessible2 angefragt wird), baut der Renderer den Baum auf. Vorher leer.
- **Versionen:** Verhalten versionsunabhaengig. **Bei Franks Build (Chromium ≥138) ist der native UIA-Provider standardmaessig AN** → der Baum wird zuverlaessiger/schneller materialisiert; die alte Chrome-117-Regression (`--force-renderer-accessibility` brach UIA, Workaround `=complete`) ist erledigt.
- **FIX:** Native UIA-Client (`CUIAutomation`/`IUIAutomation` / FlaUI) gegen das Fenster halten — dann sendet Windows `WM_GETOBJECT` automatisch. Optional explizit `SendMessage(hwndRenderWidget, WM_GETOBJECT, 0, OBJID_CLIENT)` und kurz auf den Baum WARTEN (U7). Notfalls Claude mit `--force-renderer-accessibility` starten. AHK/FlaUI-Praxis (`activateChromiumAccessibility`): `ElementFromPoint` aktiviert den Baum, `ElementFromHandle` nicht.
- **Quelle:** Chromium Accessibility Overview <https://chromium.googlesource.com/chromium/src/+/main/docs/accessibility/overview.md>; Native UIA in Chromium 138 <https://developer.chrome.com/blog/windows-uia-support-update>; Descolada UIAutomation Wiki <https://github.com/Descolada/UIAutomation/wiki/08.-Common-pitfalls;-tips-and-tricks>

### U2. Chromium baut den UIA-Baum ab, wenn das Fenster nicht sichtbar/aktiv ist
- **Symptom:** UIA klappt, wenn Claude vorne ist, liefert aber `null`/leer, sobald das Overlay den Fokus hat oder Claude minimiert/verdeckt ist. Intermittierend.
- **Ursache:** "Some programs hide their UIA tree when the window is not visible to save memory. Chromium applications in particular do this." Genau die Overlay-vor-Claude-Konstellation.
- **Versionen:** Per Design.
- **FIX:** Vor jedem UIA-Zugriff sicherstellen, dass Claude sichtbar/nicht minimiert ist (`IsIconic`-Check, kurz aktivieren), UIA-Zugriff, dann Overlay wieder nach vorn. Element-Handles nach Sichtbarkeitswechsel neu aufloesen, nicht "kalt" cachen.
- **Quelle:** Descolada UIAutomation Wiki <https://github.com/Descolada/UIAutomation/wiki/08.-Common-pitfalls;-tips-and-tricks>

### U3. `AutomationElement.FocusedElement` null / falsch / langsam
- **Symptom:** `FocusedElement` ist `null`, liefert das Document-Root statt des contenteditable, oder dauert Sekunden — und zeigt das Overlay, wenn DAS gerade Fokus hat.
- **Ursache:** (1) Setzt aktiven Baum voraus (U1). (2) Chromium meldet Fokus auf Container-Ebene; das contenteditable ist Kind darunter. (3) Cross-Process-COM, synchron auf dem UI-Thread → langsam. (4) Wer Fokus hat, ist evtl. das Overlay.
- **Versionen:** Per Design (Chromium) / Langsamkeit alle UIA.
- **FIX:** Nicht auf `FocusedElement` verlassen. Vom Claude-Fenster-HWND ausgehen (`ElementFromHandle`) und gezielt nach `ControlType.Edit`/`Document` (Name/AutomationId) suchen; notfalls per TreeWalker zum Edit-Descendant absteigen. Teuren Aufruf cachen/auf Worker-Thread (U4) mit Timeout.
- **Quelle:** Microsoft Learn AutomationElement.FocusedElement <https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.automationelement.focusedelement>; Obtaining UI Automation Elements <https://learn.microsoft.com/en-us/dotnet/framework/ui-automation/obtaining-ui-automation-elements>

### U4. STA/Threading: UIA-Calls auf dem WPF-UI-Thread blockieren/deadlocken gegen Electron
- **Symptom:** `FindFirst`/`FocusedElement`/Pattern-Calls frieren die WPF-UI ein; gelegentlich `ContextSwitchDeadlock`-MDA; Timeouts, wenn der Electron-Renderer beschaeftigt ist.
- **Ursache:** Microsoft: "If your client application might try to find elements in its own user interface, you must make all UI Automation calls on a separate thread." UIA-COM-Calls sind synchron/cross-process; ein nicht-pumpender STA-Thread blockiert. Electron blockiert seinen Renderer bei langen Operationen.
- **Versionen:** Alle UIA-Versionen.
- **FIX:** ALLE UIA-Calls auf einem dedizierten Worker-Thread (nie WPF-Dispatcher), Ergebnis per `Dispatcher.Invoke` zurueck. Managed API: Worker als STA; native `IUIAutomation`: MTA empfohlen. Harte Timeouts (`Task` + `CancellationToken`/`Task.WhenAny`), `CacheRequest` gegen Round-Trips (U6).
- **Quelle:** Microsoft Learn Obtaining UI Automation Elements <https://learn.microsoft.com/en-us/dotnet/framework/ui-automation/obtaining-ui-automation-elements>; "The Horror of Blocking Electron's Main Process" <https://medium.com/actualbudget/the-horror-of-blocking-electrons-main-process-351bf11a763c>

### U5. Managed `System.Windows.Automation` (UIA 2.0) vs. natives `IUIAutomation` (UIA 3.0)
- **Symptom:** Mit der managed API: mehr `null`-Treffer, langsamer, sporadische Fehler (`IsValuePatternSupported`-Exceptions), fehlende neuere Patterns.
- **Ursache:** Die managed Wrapper basieren auf UIA 2.0 (nicht mehr aktiv gepflegt); UIA 3.0 (nativ/COM) hat "improved reliability and performance" — gegen dynamische Chromium-Baeume deutlich spuerbar.
- **Versionen:** managed = Legacy; native = empfohlen.
- **FIX:** Native `IUIAutomation`/`CUIAutomation` per COM-Interop — am pragmatischsten **FlaUI (`FlaUI.UIA3`)** (kapselt UIA 3, inkl. `FlaUI.Core.Input.Keyboard` fuer den Paste-Fallback). Alternativ `TestStack/UIAComWrapper` (alte API-Form, intern UIA 3) als Migrationspfad. .NET 8: COM-Interop voll unterstuetzt.
- **Quelle:** Microsoft Learn UI Automation Overview <https://learn.microsoft.com/en-us/windows/win32/winauto/uiauto-uiautomationoverview>; TestStack/UIAComWrapper <https://github.com/TestStack/UIAComWrapper>; FlaUI <https://github.com/FlaUI/FlaUI>

### U6. Performance: `FindAll`/`TreeScope.Descendants` ueber den ganzen Chromium-Baum haengt
- **Symptom:** `FindFirst`/`FindAll` mit `Descendants` braucht Sekunden oder blockiert; jede Property ist ein eigener Cross-Process-Round-Trip.
- **Ursache:** UIA ist langsamer als MSAA; Chromium-Baeume sind riesig (ganze Webseite). `FindAll` bricht nicht frueh ab; `Descendants` durchwandert den kompletten Teilbaum.
- **Versionen:** Alle UIA.
- **FIX:** `FindFirst` statt `FindAll`; Scope klein (erst `TreeScope.Children` zum Container, dann gezielt); `CacheRequest` mit nur noetigen Properties/Patterns (`FindFirstBuildCache`); stabile Bedingung (`AutomationId`/`Name`+`ControlType`); gefundenes Edit-Element cachen (Invalidierung bei Sichtbarkeitswechsel, U2).
- **Quelle:** Descolada UIAutomation Wiki (Speed) <https://github.com/Descolada/UIAutomation/wiki/08.-Common-pitfalls;-tips-and-tricks>; Microsoft Learn FindFirst <https://learn.microsoft.com/en-us/windows/desktop/api/uiautomationclient/nf-uiautomationclient-iuiautomationelement-findfirst>

### U7. Latenz/Race: AX-Baum kommt asynchron → erster `FindFirst` nach Aktivierung = null
- **Symptom:** Erster Durchlauf nach Start/Fokuswechsel findet nichts; ein zweiter ms spaeter klappt. Elemente teils faelschlich "offscreen".
- **Ursache:** Multi-Prozess: Der AX-Baum wird im Renderer gebaut, serialisiert und asynchron an den Browser-Prozess geschickt; der Cache "may lag what's in the renderer process by a fraction of a second".
- **Versionen:** Per Design (Multi-Prozess); verwandter offener Chromium-Report 491839.
- **FIX:** Retry-mit-Wait (5 Versuche, 50→100→200 ms Backoff), bis das Edit-Element da ist. Sauberer: auf `StructureChanged`/`AutomationFocusChangedEvent` warten. Accessibility frueh (beim Overlay-Start) einmalig aktivieren, damit der Baum "warm" ist.
- **Quelle:** Chromium Accessibility Overview ("cache may lag") <https://chromium.googlesource.com/chromium/src/+/main/docs/accessibility/overview.md>

### U8. `ValuePattern.SetValue` scheitert am contenteditable; `TextPattern` ist read-only ⭐ HAEUFIG (Kern des Befuellens)
- **Symptom:** `TryGetCurrentPattern(ValuePattern.Pattern)` = false, oder `IsReadOnly == true` und `SetValue` wirft `InvalidOperationException`. `TextPattern` kann Text nur lesen.
- **Ursache:** Mehrzeilige Edit-/Document-Controls implementieren laut MS `IValueProvider` (=ValuePattern) NICHT, sondern `ITextProvider` (=TextPattern) — und "TextPattern does not support setting the text … text input must be simulated". Chromium exponiert fuers contenteditable Rolle Document/Group mit (lesendem) TextPattern, oft kein schreibbares ValuePattern; `NativeWindowHandle` ist null.
- **Versionen:** Per Design (MS-Spezifikation + Chromium-Pattern-Exposition), alle Versionen.
- **FIX (der tragende Weg):** UIA nur zum FINDEN + FOKUSSIEREN nutzen: Edit-Element finden (U1/U6/U7), `SetFocus()`/Invoke → Caret im richtigen Feld. Dann Text per **Zwischenablage + echtem Strg+V** (Scancode-`SendInput`/`FlaUI.Keyboard`, T1/T2) einschleusen — robust fuer langen/Unicode-Text, alten Clipboard-Inhalt sichern/wiederherstellen (C2/C3). `ValuePattern.SetValue` zuerst versuchen (schneller Pfad), bei `false`/Exception auf Paste zurueckfallen. (= Symmetrie zu macOS: AX lokalisiert, der Text landet via gesetztem Wert/Tastatur.)
- **Quelle:** Microsoft Learn ValuePattern.SetValue ("text input must be simulated") <https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.valuepattern.setvalue?view=windowsdesktop-8.0>; UI Automation TextPattern Overview <https://learn.microsoft.com/en-us/dotnet/framework/ui-automation/ui-automation-textpattern-overview>

---

## E) Electron/Chromium-Eigenheiten & App-Unterschiede

### E1. Eingabefelder sind `contenteditable`-DIVs (ProseMirror/Lexical), kein Win32-Edit-Control ⭐ HAEUFIG
- **Symptom:** `WM_SETTEXT`/`WM_GETTEXT`/`WM_PASTE`/`EM_*` und AHK `ControlSetText`/`ControlGetText` tun nichts/liefern Muell; Feld bleibt leer.
- **Ursache:** Moderne Electron-Apps (Claude, Codex, Cursor, VS Code) rendern Eingaben als HTML-`contenteditable` mit JS-Editor-Engine — kein Win32-Edit-Control, keine echte `<textarea>`. Die `EM_*`/`WM_SETTEXT`-Maschinerie existiert nur fuer native Controls; das DIV hoert nur auf DOM-Events, die der Renderer aus echten Inputs synthetisiert.
- **Versionen:** Per Design (alle Electron/Chromium).
- **FIX:** Nie Textnachrichten ans HWND. Text in Zwischenablage + echtes Paste (E7/T1), ODER UIA finden+fokussieren (U8). AHK-Konsens: "faster and more reliable to place the text on the clipboard and paste it, rather than using Send commands".
- **Quelle:** ProseMirror-Diskussion <https://discuss.prosemirror.net/t/the-content-of-non-editable-node-is-still-editable/2154>; AutoHotkey-Forum <https://www.autohotkey.com/boards/viewtopic.php?t=93737>

### E2. Mehrere HWNDs pro Fenster; nur das Render-Widget-HWND nimmt Input an
- **Symptom:** `FindWindow` liefert `Chrome_WidgetWin_1`, aber dorthin gesendete Inputs landen nirgends.
- **Ursache:** Aussen die Fenster-Shell `Chrome_WidgetWin_1`, darunter als Kind `Chrome_RenderWidgetHostHWND` (nimmt Eingaben an), plus separate Prozesse/Fenster (GPU, ggf. OSR). Electron-Maintainer zcbenz: Render-Widget nur durch Enumeration der Child-Windows finden.
- **Versionen:** Per Design (alle Electron).
- **FIX:** Nicht aufs Top-Level zielen. Child-Windows enumerieren und `Chrome_RenderWidgetHostHWND` finden (F1/F4) — besser Feld per Accessibility lokalisieren (U8) und fokussiert Paste senden. Klassennamen-Suche ist "discouraged in favor of using accessibility APIs".
- **Quelle:** electron/electron #5122 (zcbenz) <https://github.com/electron/electron/issues/5122>; chromium-dev <https://groups.google.com/a/chromium.org/g/chromium-dev/c/hmBh5YHjOFY>

### E3. ResizeObserver / dynamisches Layout verschiebt das Feld; feste Klick-Koordinaten brechen
- **Symptom:** Hartkodierte Koordinaten treffen anfangs, aber sobald Inhalt erscheint (Cowork: Feld rutscht nach unten; Chat: Composer waechst), klickt die Automatisierung ins Leere.
- **Ursache:** Reines HTML/CSS-Flexbox + JS-`ResizeObserver`; die Composer-Position ergibt sich dynamisch aus Fensterhoehe, Zoom/DPI, Panels, erscheinenden Task-Panels — keine stabile Bildschirmkoordinate. (aaddrick dokumentiert einen "Chromium layout cache bug" fuer Claude Desktop.)
- **Versionen:** Per Design; verschaerft mit jeder UI-Iteration (Claude Desktop wird haeufig aktualisiert).
- **FIX:** Keine absoluten Pixel verdrahten. Feld zur Laufzeit per UIA aufloesen (aktuelles `BoundingRectangle`) oder ganz ohne Koordinaten per `SetFocus()` + Paste. Wenn Klick noetig: Position unmittelbar davor neu abfragen. (Siehe M1.)
- **Quelle:** Electron BrowserWindow-Doku <https://www.electronjs.org/docs/latest/api/browser-window>; aaddrick claude-desktop-debian <https://github.com/aaddrick/claude-desktop-debian>

### E4. Claude Desktop ist Electron; Fensterstruktur variiert mit der Electron-/Chromium-Version
- **Symptom:** Eine HWND-/Selektor-Logik bricht nach einem App-Update.
- **Ursache:** Claude Desktop ist Electron (bestaetigt von Anthropic); Electron buendelt eine feste Chromium-Version pro Release, und Claude wird sehr haeufig aktualisiert. Minified-Variablennamen und interne Struktur aendern sich zwischen Releases (aaddrick muss Patches bei fast jedem Release nachziehen). 2026er-Electron-Linie ~39–42 (Chromium ~134+); eine offiziell publizierte Electron-/Chromium-Nummer fuer Claude Desktop gibt es nicht — sie muss zur Laufzeit ausgelesen werden. Franks Build: **1.12603.1 (3df4fd, 2026-06-11)**.
- **Versionen:** Versionsabhaengig, bewegliches Ziel.
- **FIX:** Keine konkrete Electron-/Chromium-Version annehmen. Versionsunabhaengig bauen: Accessibility-Baum statt Klassennamen, Feld ueber Rolle/Name statt fester Verschachtelung. Chromium-Version notfalls aus dem Renderer-User-Agent / gebuendelten `.pak`-Dateien ermitteln, nicht raten.
- **Quelle:** "2026 Audit of Famous Electron Apps" <https://codenote.net/en/posts/famous-electron-apps-2026-research/>; HN "Why is Claude an Electron App?" <https://news.ycombinator.com/item?id=47104973>

### E5. Strukturunterschiede Claude Desktop vs. Codex vs. Cursor vs. VS Code (gleiche Electron-Basis)
- **Symptom:** Eine fuer Claude entwickelte HWND-/Fenstersuche findet bei den anderen nichts/das falsche Fenster.
- **Ursache:** Alle Electron → gleiche Grundklasse `Chrome_WidgetWin_1` + intern `Chrome_RenderWidgetHostHWND`. ABER: Fenstertitel unterscheiden sich (VS Code/Cursor pro Datei/Workspace), Editor-Engine unterscheidet sich (Cursor/VS Code = Monaco fuer Code, Claude/Codex = chat-orientierter contenteditable-Composer), Renderer-Verschachtelung/Custom-Title-Bar weichen ab. Zusatzfalle: Edge nutzt fuer Flyouts `Chrome_WidgetWin_2`. Cursor ist ein VS-Code-Fork.
- **Versionen:** Per Design, app-/versionsabhaengig.
- **FIX:** Pro Zielanwendung getrennt erkennen, nie universelle Fixverdrahtung. Zielprozess ueber Prozessnamen/-pfad identifizieren (z. B. `claude.exe`), Feld ueber Accessibility-Baum statt Titel/Klassennamen-Index aufloesen. Konfigurierbare Mapping-Tabelle (App → Erkennungsstrategie).
- **Quelle:** Microsoft Q&A (dynamischer WindowClassName) <https://learn.microsoft.com/en-us/answers/questions/2005597/conditions-for-dynamically-changing-windowclassnam>; 2026-Audit <https://codenote.net/en/posts/famous-electron-apps-2026-research/>

### E6. `KEYEVENTF_UNICODE`/`VK_PACKET` (Zeichen-fuer-Zeichen tippen) liefert in Chromium falsche/fehlende Zeichen
- **Symptom:** Direktes Tippen jedes Zeichens per `SendInput` mit `KEYEVENTF_UNICODE` produziert in manchen Chromium-Feldern falsche/fehlende Zeichen; langsam.
- **Ursache:** Bei `KEYEVENTF_UNICODE` kommt `WM_KEYDOWN/UP` mit `wVk=VK_PACKET`; erst `TranslateMessage` macht `WM_CHAR`. Chromium-Renderer verarbeiten diese synthetischen Unicode-Events teils inkonsistent (vgl. microsoft/terminal #12977).
- **Versionen:** Chromium-/App-abhaengig, offen.
- **FIX:** Nicht Zeichen-fuer-Zeichen tippen → Copy-Paste (E7): Text in Zwischenablage (`CF_UNICODETEXT`), fokussieren, echtes Strg+V mit Scancodes. Behandelt Unicode/Emojis korrekt und ist zuverlaessiger.
- **Quelle:** Microsoft Learn KEYBDINPUT (KEYEVENTF_UNICODE/VK_PACKET) <https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput>; microsoft/terminal #12977 <https://github.com/microsoft/terminal/issues/12977>

### E7. Belegt funktionierender Weg A: Zwischenablage + echtes Paste mit Scancodes
- **Symptom/Falle:** siehe E6 (direktes Tippen unzuverlaessig).
- **Ursache:** Copy-Paste umgeht die Unicode-Event-Inkonsistenz und die contenteditable-Beschraenkungen.
- **Versionen:** Funktioniert versionsuebergreifend.
- **FIX (empfohlene Sequenz):** (1) alten Clipboard-Inhalt sichern; (2) eigenen Text als `CF_UNICODETEXT` schreiben (`SetDataObject(copy:true)`, C3); (3) Zielfeld fokussieren (UIA `SetFocus()` oder Klick auf per UIA ermitteltes Rect); (4) echtes Strg+V via `SendInput` mit `KEYEVENTF_SCANCODE` (T1/T2); (5) Clipboard verzoegert wiederherstellen (C2). Vor Schritt 4 Fokus sicherstellen (A6) und kurz warten.
- **Quelle:** AutoHotkey Send-Doku (Clipboard-Paste-Empfehlung) <https://www.autohotkey.com/docs/v2/howto/SendKeys.htm>; Microsoft Learn KEYBDINPUT <https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput>

### E8. Belegt funktionierender Weg B: UI Automation — aber A11y aus, ValuePattern/TextPattern setzen keinen Text
- **Symptom:** UIA findet das Feld nicht (Baum leer) oder findet es, aber `ValuePattern.SetValue`/`TextPattern` schreiben nichts.
- **Ursache:** (a) Accessibility in Electron/Chromium standardmaessig deaktiviert, lazy aktiviert (U1) — `app.setAccessibilitySupportEnabled(true)` bzw. `--force-renderer-accessibility`. (b) `TextPattern` kann grundsaetzlich keinen Text einfuegen; `ValuePattern` bei contenteditable oft nur lesend (U8).
- **Versionen:** Per Design (Chromium/UIA-weit). Franks Build (Chromium ≥138): native UIA an → Baum zuverlaessiger.
- **FIX:** UIA NUR zum Lokalisieren+Fokussieren (Rolle `edit`/`document` + Name, `BoundingRectangle`, `SetFocus()`); Baum vorab erzwingen; Text per Clipboard-Paste (E7). `webContents.paste()`/`insertText` ist NICHT nutzbar, da man die fremde App nicht kontrolliert.
- **Quelle:** Electron Accessibility-Doku <https://www.electronjs.org/docs/latest/tutorial/accessibility/>; Chromium UI-Automation-Doku <https://chromium.googlesource.com/chromium/src/+/HEAD/docs/accessibility/browser/uiautomation.md>; Microsoft Learn TextPattern Overview <https://learn.microsoft.com/en-us/dotnet/framework/ui-automation/ui-automation-textpattern-overview>

### E9. Electron `globalShortcut`-Konflikte: Claude greift sich Hotkeys; "global" ist unzuverlaessig
- **Symptom:** Der globale Hotkey des Overlays feuert nicht (Claude hat dieselbe Kombi registriert), oder umgekehrt — ohne Fehlermeldung.
- **Ursache:** Electrons `globalShortcut` registriert OS-weite Hotkeys; Claude Desktop nutzt einen globalen Hotkey (laut aaddrick `Ctrl+Alt+Space`). Global = first-come-first-served; der zweite Registrant bekommt die Taste nicht (oder still fehlgeschlagen). Electrons `globalShortcut` ist dokumentiert unzuverlaessig ("not actually 'global' … not reliable").
- **Versionen:** Bekannt ueber viele Electron-Versionen (electron #27240, #8491).
- **FIX:** `RegisterHotKey`-Rueckgabe im Overlay pruefen, bei Kollision auf freie Kombi ausweichen, Claudes `Ctrl+Alt+Space` meiden, Hotkey konfigurierbar machen. Nach Ausloesen nicht annehmen, dass das Ziel Fokus hat — aktiv setzen + verifizieren (A1/A6).
- **Quelle:** electron/electron #27240 (offen) <https://github.com/electron/electron/issues/27240>; Electron globalShortcut-Doku <https://www.electronjs.org/docs/latest/api/global-shortcut>

---

## 🔧 Fix-Status (Schritt 3 — was ist behoben, was bleibt aktiv?)

> **Methodik & Ehrlichkeit:** `gh` war in dieser Umgebung nicht verfuegbar und die GitHub-API lieferte
> ueber das Web-Fetch-Werkzeug leere Antworten. Die Issue-Stati (OPEN/CLOSED/MERGED) stammen daher aus
> der direkten Quellen-Lektuere der Researcher (Stand Juni 2026), NICHT aus eigener gh-Verifikation —
> entsprechend als "researcher-belegt" markiert. Per-Design-Verhalten ist ueber offizielle
> Microsoft-Learn-/Chromium-Quellen abgesichert. Die **Chromium-138-native-UIA-Aussage wurde
> unabhaengig per WebSearch (2026-06-15) bestaetigt**.

### Belegt behoben / positiv fuer Franks aktuelle Build

| Frueherer Bug | Status | Bezug |
|---------------|--------|-------|
| Chrome 117 brach UIA mit `--force-renderer-accessibility` (Workaround `=complete`) | **Behoben** — native UIA-Provider **seit Chromium 138 standardmaessig AN** (WebSearch-verifiziert). Franks Build (2026-06, Chromium ≥138 wahrscheinlich) → UIA-Pfad voll unterstuetzt, Proxy-Layer entfaellt | U1, E8 |
| .NET-9-`DataObject`-Konstruktor-Bug (winforms #12789) | **Behoben** im Feb-2025-Servicing (researcher-belegt). **Betrifft nur Custom-DataObject** — Franks .NET 8 + Klartext ist ohnehin NICHT betroffen | C6 |
| PowerToys-Auto-Copy scheitert an Electron/Chromium (PR #46486) | **MERGED** (researcher-belegt) — bestaetigt die 500-ms-Async-Race-Erkenntnis; in Franks Code als Workaround C2 umzusetzen | C2 |

### NICHT gefixt / per Design (Workaround bleibt aktiv)

Diese sind grundlegendes Win32-/Chromium-Verhalten und werden sich nicht "von selbst" aendern — der jeweilige FIX bleibt dauerhaft noetig:

- **F1/F2/F3** `FindWindowEx` nur direkte Kinder; Klassennamen sind Implementierungsdetail — per Design.
- **F4/F5/E2** Mehrere HWNDs pro Fenster, OSR ohne Render-HWND — per Design (Multiprozess).
- **A1–A6** Foreground-Lock, `AttachThreadInput`-Deadlock, `AllowSetForegroundWindow`-Fluechtigkeit, `SetFocus` ueber Prozessgrenze — per Design (Microsoft raet ab).
- **T1/T2/T5/T8** keybd_event/Scancode-Pflicht, `WM_PASTE` an contenteditable wirkungslos, `LLKHF_INJECTED`-Filterbarkeit — per Design.
- **T4** UIPI/Elevation-Blockade — per Design seit Vista.
- **M1–M7** Festkoordinaten vs. wanderndes Feld, DPI-Virtualisierung, Multi-Monitor, falsches Kind-HWND — per Design.
- **C1/C7** `CLIPBRD_E_CANT_OPEN`-Race — **dotnet/wpf #9901 OPEN** (researcher-belegt), nicht gefixt.
- **C3/C4/C5** OLE delayed rendering, STA-Pflicht — per Design.
- **U1/U2/U7** Chromium-A11y lazy/aus, Baum-Abbau bei Unsichtbarkeit, asynchrone Baum-Latenz — per Design.
- **U8** `ValuePattern.SetValue`/`TextPattern` koennen contenteditable-Text nicht setzen — per Design (MS-Spezifikation).
- **E1/E3/E4/E5** contenteditable statt Edit-Control, ResizeObserver-Layout, Electron als bewegliches Ziel, App-Strukturunterschiede — per Design.
- **E6** `KEYEVENTF_UNICODE`/`VK_PACKET`-Inkonsistenz (terminal #12977) — offen.
- **E9** Electron `globalShortcut` unzuverlaessig (electron #27240) — offen.

---

## ✅ Pflicht-Checkliste (vor/bei Arbeit an der Windows-Text-Injection)

- [ ] **Strategie:** Nicht blind klicken/tippen — Feld per **UIA finden + fokussieren**, Text per **Clipboard + Scancode-Strg+V** (K1, U8, E7).
- [ ] **HWND:** `EnumChildWindows` (rekursiv, EINMAL) statt einstufigem `FindWindowEx`; nur Blatt-Klasse `Chrome_RenderWidgetHostHWND`, Zwischen-Klassennamen NIE fest verdrahten; aktives/sichtbares HWND waehlen (F1–F4).
- [ ] **Fokus:** `IsIconic`→`SW_RESTORE` → ALT-SendInput (Foreground-Recht) → `SetForegroundWindow` (Rueckgabe pruefen) → `WaitForInputIdle`; kein prozessuebergreifendes `SetFocus`, kein `AttachThreadInput`-Dauerattach (A1–A6).
- [ ] **Overlay:** als Nicht-aktivierendes Fenster (`ShowActivated=false` VOR Show + `WS_EX_NOACTIVATE`), damit es dem Ziel den Fokus nicht stiehlt (A7).
- [ ] **Tastatur:** `SendInput` mit `MapVirtualKey`+`KEYEVENTF_SCANCODE`; Ctrl↓ → 10–30 ms → V↓ → V↑ → Ctrl↑; KEIN `keybd_event`/`SendKeys`/`WM_PASTE` (T1–T6).
- [ ] **Elevation:** Integritaetslevel angleichen — Sender NICHT als Admin starten (UIPI, T4).
- [ ] **Maus (falls noetig):** PerMonitorV2-Awareness setzen; sichtbare Bounds via `DWMWA_EXTENDED_FRAME_BOUNDS`; Position relativ zu UIA-`BoundingRectangle`, nicht fix; `SendInput` mit `ABSOLUTE|VIRTUALDESK` + Normalisierung (M1–M7).
- [ ] **Clipboard:** `SetDataObject(text, copy:true)` in Retry-Schleife (`CLIPBRD_E_CANT_OPEN`); auf STA-Thread; Restore erst 300–500 ms nach dem Paste oder weglassen; Modifier vor dem Paste freigeben (C1–C5, C7).
- [ ] **UIA:** native API/**FlaUI (UIA3)**, alle Calls auf Worker-Thread mit Timeout; `FindFirst`+enger Scope+`CacheRequest`; Baum aktivieren + Retry/Backoff (U1, U3–U7).
- [ ] **A11y aktiv?** Bei Franks Build (Chromium ≥138) native UIA standardmaessig an — sonst per `WM_GETOBJECT`/`--force-renderer-accessibility` scharfschalten (U1, E8).
- [ ] **App-spezifisch:** Zielprozess ueber Prozessnamen erkennen, Mapping-Tabelle Claude/Codex/Cursor; nie auf feste Electron-Version verlassen (E4, E5).
- [ ] **Hotkey:** `RegisterHotKey`-Rueckgabe pruefen, `Ctrl+Alt+Space` (Claude) meiden (E9).
- [ ] Neuen selbst erlebten Bug → hier als Eintrag + Kurzcheck-Zeile ergaenzen, Stand-Header aktualisieren.
