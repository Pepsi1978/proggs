# Windows-Electron-Text-Injection (C#/WPF → Claude Desktop) — Best Practices (Stand 2026-06-15, .NET 10 / WPF, Windows 11 24H2, Claude Desktop 1.12603.1 = Electron 41 / Chromium 146)

> **Worum es geht:** Ein externes **C#/WPF-Overlay** (.NET 10, `net10.0-windows`, self-contained single-file)
> soll aus sich heraus Text in die Eingabefelder der **Claude Desktop App** (Chat, Code, Cowork) einfügen —
> auch wenn das Feld die Position wechselt (Chat unten, Cowork Mitte/Dreiviertel und rutscht nach unten,
> Code unten). Ziel ist das robuste **Windows-Äquivalent** zur funktionierenden macOS-Accessibility-Lösung
> (`AXUIElement`).
>
> **Die EINE tragende Erkenntnis (zuerst lesen):** Das Eingabefeld ist ein **HTML-`contenteditable`-DIV**
> (ProseMirror/Lexical) in EINER großen Chromium-Render-Fläche — **kein** Win32-Edit-Control. Deshalb ist
> der richtige Weg von vornherein, den macOS-Ansatz zu spiegeln: **Feld per UI Automation FINDEN +
> FOKUSSIEREN (positions-unabhängig), Text per Zwischenablage + echtem Strg+V mit Hardware-Scancodes
> EINFÜGEN.** Alles andere (feste Koordinaten-Klicks, `WM_SETTEXT`/`WM_PASTE`, `keybd_event`,
> `ValuePattern.SetValue` aufs contenteditable) ist von Anfang an der falsche Weg.
>
> **Versions-Anker (live verifiziert, 2026-06-15):** Claude Desktop 1.12603.1 (Build 3df4fd, 2026-06-11)
> ≈ **Electron 41 → Chromium 146.0.7680.65** (V8 14.6, Node 24.14.0). Konsequenz: **nativer UI-Automation-
> Provider standardmäßig aktiv** (seit Chromium 138) → UIA ist der zuverlässige Primärweg. Codex/Cursor/
> VS Code teilen dieselbe Electron-Basis, aber NICHT dieselbe Fensterstruktur (§0, §3). Zukunftssicher
> bauen: Version zur Laufzeit lesen, nie hart verdrahten (§0).
>
> **Quellenpolitik:** Microsoft Learn / Chromium-Accessibility / Electron / Chrome-for-Developers zuerst
> (`offiziell`); Community klar als `extern` markiert — überstimmt nie das Offizielle.
>
> **Zweite Seite der Medaille (was schiefgeht):**
> [`bugs/desktop/windows-electron-text-injection.md`](../../bugs/desktop/windows-electron-text-injection.md).
> Jeder §-Abschnitt unten verweist auf die passenden Bug-Sektionen (K/F/A/T/M/C/U/E/N/W); die
> Bezugstabelle steht am Dateiende.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| #  | Situation | Best Practice (Kurzform) | Volltext |
|----|-----------|--------------------------|----------|
| 1  | Grundentscheidung „wie fülle ich das Feld?" | macOS-Weg spiegeln: **UIA finden+fokussieren → Clipboard + echtes Strg+V (Scancode)**. Nie blind klicken/Text-Messages senden. | §0, §2, §3 |
| 2  | Welche Chromium-Version? UIA verlässlich? | 1.12603.1 = Electron 41/Chromium 146 → nativer UIA-Provider AN. Version trotzdem zur Laufzeit auslesen, nie fixieren. | §0 |
| 3  | Overlay soll NICHT den Fokus dauerhaft klauen | `ShowActivated=false` + `Topmost=true` + `WS_EX_NOACTIVATE\|WS_EX_TOOLWINDOW` in `OnSourceInitialized`; alle `SetWindowPos` mit `SWP_NOACTIVATE`. | §1.1 |
| 4  | Ziel-App kurz nach vorne holen (legal) | Auslöser = User-Input INS Overlay; `AllowSetForegroundWindow(zielPid)` unmittelbar davor; Rückgabe von `SetForegroundWindow` prüfen, bei 0 → `FlashWindowEx`. | §1.2 |
| 5  | „Fokus leihen" statt klauen | `AttachThreadInput(mein, ziel, true)` → aktivieren/`SetFocus` → **im `finally` `…false`**. Nie an sich selbst attachen; Detach nie vergessen. | §1.3 |
| 6  | Minimierte/schlafende Ziel-App | erst `IsIconic`→`ShowWindow(SW_RESTORE)`, bei Cold-Start `WaitForInputIdle`, dann aktivieren. | §1.4 |
| 7  | Fokus sauber zurückgeben | vorheriges Foreground-HWND merken; nach dem Einfügen dieselbe Aktivierungssequenz darauf anwenden; Overlay bleibt dank NOACTIVATE ohnehin fokus-neutral. | §1.5 |
| 8  | Text wirklich einfügen | Clipboard (`SetDataObject(text, copy:true)`, `CF_UNICODETEXT`) + **ein** `SendInput`-Array `Ctrl↓ V↓ V↑ Ctrl↑` mit `KEYEVENTF_SCANCODE`. | §2.1, §2.2 |
| 9  | Scancode statt Virtual-Key | `wScan = MapVirtualKey(VK_*, MAPVK_VK_TO_VSC)`, `wVk=0`, Flag `KEYEVENTF_SCANCODE` — VK allein wird je Layout verworfen. | §2.1 |
| 10 | `keybd_event` benutzen? | Nein — veraltet, kein Scancode, vermischbar. Immer `SendInput` (atomar pro Aufruf). | §2.1 |
| 11 | Mehrzeilig / Sonderzeichen / Emoji | Clipboard-Paste trägt UTF-16 verlustfrei (Surrogate inkl.). NICHT Zeichen-für-Zeichen via `KEYEVENTF_UNICODE` tippen. | §2.3 |
| 12 | `ValuePattern.SetValue` als Schreibweg? | Nur optionaler Schnellpfad für einzeilige Felder; bei contenteditable/mehrzeilig versagt es („text input must be simulated"). | §2.4 |
| 13 | Optionales Auto-Enter | **separater** `SendInput` (Enter↓/↑, Scancode) NACH kurzer Verzögerung — sonst trifft Enter ein leeres Feld. | §2.5 |
| 14 | Schnelle Folge-Einfügungen | serielle Queue mit Sequenznummer; vor Strg+V gehaltene Modifier per KEYUP neutralisieren (PowerToys-Fix); kein paralleler Clipboard-Zugriff. | §2.6 |
| 15 | Sender als Admin starten? | Nein — UIPI blockiert `SendInput` in nicht-elevated Ziel **stillschweigend** (kein Fehlercode). Gleiches Integritätslevel wie Claude. | §2.6, §1.2 |
| 16 | Aktives Feld positions-unabhängig finden | **Primär** `AutomationElement.FocusedElement` auf eigenem Worker-Thread; prüfen `ControlType.Edit/Document/Group` (Code-Tab = Group!) + `IsKeyboardFocusable`; `RootWebArea` ausschliessen. | §3.1 |
| 17 | Fokus liegt auf Container statt Feld | `TreeWalker.ControlViewWalker` / `FindFirst(Descendants, AndCondition(ControlType, IsKeyboardFocusable))`. | §3.2 |
| 18 | UIA 2.0 oder 3.0? | Native UIA 3.0 — in .NET praktisch über **FlaUI.UIA3**. UIA 2.0 (`System.Windows.Automation`) nur Legacy. | §3.3 |
| 19 | UIA-Baum leer? | Chromium baut A11y lazy auf; der erste UIA-Zugriff aktiviert ihn on-demand (WM_GETOBJECT). Notfall: Ziel mit `--force-renderer-accessibility` starten. | §3.4 |
| 20 | Pixel-Rechteck nötig (Klick) | `Current.BoundingRectangle` zur Laufzeit frisch abfragen (nie cachen); auf `Rect.Empty` prüfen. | §3.5 |
| 21 | UIA liefert gar nichts | Caret-Fallback `GetGUIThreadInfo` (`hwndCaret`/`rcCaret` → `ClientToScreen`); bei Chromium oft leer → nur Notnagel. | §3.6 |
| 22 | HWND-Notnagel | rekursiv `EnumChildWindows` → `Chrome_RenderWidgetHostHWND` (NICHT feste Zwischen-Klassennamen), dann erneut UIA darauf. | §3.7 |
| 23 | Bricht beim App-Update | Electron ist bewegliches Ziel → versionsunabhängig: Accessibility + Prozessname, nie HWND/Titel/Klasse fest verdrahten. | §0, §3.7 |
| 24 | Logik trifft Codex/Cursor nicht | gleiche Electron-Basis, andere Struktur → pro App erkennen, Mapping-Tabelle, Feld über A11y statt Titel. | §0 |
| 25 | Womit debuggen? | Reihenfolge: A11y erzwingen → **Accessibility Insights**/Inspect.exe → Spy++ (HWND-Kette) → `ax_dump_tree --api=uia` → JSON-Logging → Logik-Sonden. | §4 |
| 26 | Kam Fokus/Element/Paste an? | erwartet-vs-tatsächlich: `FocusedElement` abgleichen, Feldwert per ValuePattern (einzeilig)/TextPattern (mehrzeilig) lesen, `GetClipboardSequenceNumber`. | §4.5 |
| 27 | Single-File: Injection-P/Invokes weg? | `user32`-Importe (`SendInput`, `EnumChildWindows` …) sind NICHT betroffen. FlaUI/COM: `EmbedInteropTypes=false`, kein Trimming/AOT, eigenes `app.manifest` (PerMonitorV2). | §1.6, §3.3 |

---

## §0 Versions-Anker & Zukunftssicherheit (zuerst lesen)

**Warum das zuerst kommt:** Electron/Chromium ist ein **bewegliches Ziel**. Jede Empfehlung unten hängt
daran, welche Chromium-Version (und damit welches Accessibility-Verhalten) die installierte Build hat.

- **Belegte Zuordnung für 1.12603.1:** **Electron 41 → Chromium 146.0.7680.65** (V8 14.6, Node v24.14.0).
  Electron 41 hob Chromium von 144.0.7559.60 auf **146.0.7680.65**; empfohlene Patch-Linie ist 41.0.2.
  Quelle: [Electron 41.0 Release-Blog](https://www.electronjs.org/blog/electron-41-0) (2026-03-10) · `offiziell`.
- **Ehrlich:** Anthropic publiziert die gebündelte Electron-Version NICHT pro Build; die App-Versionsnummer
  (1.12603.1) steht in keiner Electron-Quelle. Die Zuordnung ist **belegt-wahrscheinlich**, aber nur per
  Laufzeit-Auslesen 100 % sicher. (Frühere Diagnose im Bug-Almanach E4: aaddrick-Repackaging meldete
  Electron 41.2.0 / 41.5.0 für nahe Builds.)
- **Folge — UIA ist verlässlich:** Seit **Chromium 138** ist der **native UI-Automation-Provider** unter
  Windows standardmäßig aktiv (löst die alte MSAA→UIA-Emulation ab). Bei Chromium 146 also garantiert an →
  UIA ist der zuverlässige Primärweg zum Finden/Fokussieren (§3). MSAA/IAccessible2 bleiben unverändert
  direkt verbunden. Quelle: [Chrome for Developers — Windows UIA support update](https://developer.chrome.com/blog/windows-uia-support-update) (2025-08-14) · `offiziell`.
- **Latentes Zukunftsrisiko:** Die Enterprise-Policy `UiAutomationProviderEnabled` (Rückfall auf die alte
  Emulation) wird **nur bis Chrome 146** unterstützt — 146 ist die letzte Version, in der sie greift. Die
  neueste Electron-Major per Juni 2026 ist **Electron 42 → Chromium 148.0.7778.96** (V8 14.8, Node v24.15.0);
  ein Update von Claude Desktop dorthin springt zwei Chromium-Majors weiter. Quelle: [Electron 42.0 Release-Blog](https://www.electronjs.org/blog/electron-42-0) (2026-05-07) · `offiziell`. → Auf den **nativen** UIA-Pfad
  bauen (zukunftssicher), nicht auf Emulation.
- **Version zur LAUFZEIT auslesen (so bleibt es zukunftssicher):**
  1. **DevTools/Remote-Debugging (genau):** `claude.exe --remote-debugging-port=9222` (Pfad oft
     `%LOCALAPPDATA%\AnthropicClaude\app-1.12603.1\claude.exe`), dann `http://localhost:9222` → Renderer-
     Target → Konsole `JSON.stringify(process.versions)` (liefert `electron`/`chrome`/`v8`/`node`). Quelle:
     [Electron — Debugging the Main Process](https://www.electronjs.org/docs/latest/tutorial/debugging-main-process/) · `offiziell` (Flag); Anwendung auf Fremd-App · `extern`.
  2. **`navigator.userAgent` (robust, ohne Node-Integration):** enthält `Chrome/146.0.x` und `Electron/41.x`. `extern`.
  3. **PowerShell (nur App-Version):** `(Get-Item "$env:LOCALAPPDATA\AnthropicClaude\app-1.12603.1\claude.exe").VersionInfo | Format-List` — gibt NUR Anthropics App-Version, NICHT Chromium. Nur als Cross-Check. `extern`.
- **Codex/Cursor/VS Code mitdenken:** gleiche Electron-Basis (Klasse `Chrome_WidgetWin_1` + intern
  `Chrome_RenderWidgetHostHWND`), aber andere Fenstertitel und Editor-Engine (Monaco vs. chat-orientierter
  contenteditable-Composer). → Pro Zielanwendung getrennt erkennen (Prozessname/-pfad, z. B. `claude.exe`),
  Feld über den Accessibility-Baum statt über Titel/Klassenindex auflösen, konfigurierbare Mapping-Tabelle
  (App → Erkennungsstrategie). Bug-Bezug: E4, E5.

> **Merksatz §0:** *Version nie hart verdrahten. Accessibility-Baum statt Klassennamen. Prozessname statt
> Fenstertitel. Dann überlebt das Tool den nächsten Claude-Desktop-Update.*

---

## §1 Säule 1 — Das Overlay-Fenster richtig bauen (Fokus LEIHEN, nicht klauen)

> Abgrenzung zur Schwester-BP [`best-practices-windows-overlay.md`](best-practices-windows-overlay.md):
> Dort geht es um ein Overlay, das sich **nie** in den Vordergrund drängt. Hier kommt der Sonderfall dazu,
> dass das Overlay die **Ziel-App** (Claude Desktop) für den Moment des Einfügens **kurz** aktivieren muss —
> ohne den eigenen Fokus dauerhaft an sich zu reißen. Kernprinzip: **Das Overlay aktiviert sich selbst nie;
> Foreground-Wechsel zur Ziel-App sind kurz, `AttachThreadInput`-umrahmt, mit garantiertem Detach, und nur
> durch ein User-Input-Event ins Overlay ausgelöst.**

### §1.1 Overlay als nicht-aktivierendes Werkzeugfenster
- **`ShowActivated=false` VOR dem ersten `Show()`** setzen — das Fenster erscheint, ohne aktiviert zu werden
  (Setzen nach dem ersten Show wirkt nicht). Plus `Topmost=true`. Quelle: [Window.ShowActivated (.NET 10)](https://learn.microsoft.com/en-us/dotnet/api/system.windows.window.showactivated) · `offiziell`.
- **`WS_EX_NOACTIVATE (0x08000000)` + `WS_EX_TOOLWINDOW (0x00000080)`** per Interop setzen (WPF kann das nicht
  in XAML): im `SourceInitialized`-Handler HWND via `new WindowInteropHelper(this).Handle` holen,
  `GetWindowLong(hwnd, GWL_EXSTYLE)` lesen, ge-OR-t per `SetWindowLong` zurückschreiben. `WS_EX_NOACTIVATE`
  bewirkt, dass das Fenster keinen Tastaturfokus an sich reißt; `WS_EX_TOOLWINDOW` hält es aus Alt-Tab/
  Taskleiste. Quelle: [Extended Window Styles](https://learn.microsoft.com/en-us/windows/win32/winmsg/extended-window-styles) · `offiziell`; Interop-Vorgehen via `WindowInteropHelper`/`SourceInitialized`: [WPF-Windows-Doku](https://learn.microsoft.com/en-us/dotnet/desktop/wpf/windows/) · `offiziell`.
- **Topmost setzen ohne zu aktivieren:** `SetWindowPos(hwnd, HWND_TOPMOST, 0,0,0,0, SWP_NOMOVE|SWP_NOSIZE|SWP_NOACTIVATE)`.
  `SWP_NOACTIVATE` ist hier entscheidend. Quelle: [SetWindowPos](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos) · `offiziell`.
- **Topmost ereignisgesteuert re-asserten**, nicht pollen: bei `Activated`/`Deactivated` bzw. per WinEvent-Hook
  auf Foreground-Wechsel der Ziel-App erneut `SetWindowPos(HWND_TOPMOST, …, SWP_NOACTIVATE)`. Relevant gegen
  die Win11-24H2-Z-Order-Regression (Overlay fällt hinter Paint/Photos). Bug-Bezug: A7, W1.

### §1.2 Ziel-App legal nach vorne holen
- **`SetForegroundWindow` gelingt nur unter dokumentierten Bedingungen** — der sauberste legale Auslöser ist,
  dass **das Overlay gerade das letzte Input-Event erhalten hat** (User hat ins Overlay geklickt/getippt).
  Rückgabewert prüfen; bei `0` **nicht hämmern**, sondern `FlashWindowEx` als Fallback. Quelle:
  [SetForegroundWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow) (2025-10-06) · `offiziell`.
- **`AllowSetForegroundWindow(zielPid)`** ist der dokumentierte Weg, dem Zielprozess das Vordergrundrecht zu
  „verleihen" — gezielt die Ziel-PID übergeben (sauberer als `ASFW_ANY = (DWORD)-1`). **Das Recht ist
  flüchtig** (verfällt beim nächsten User-Input / nächsten `AllowSetForegroundWindow`), daher **unmittelbar
  vor** der Aktivierung aufrufen, nie cachen. Quelle: [AllowSetForegroundWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-allowsetforegroundwindow) (2025-10-06) · `offiziell`.
- **Integritätslevel angleichen:** Sender NICHT elevated starten — sonst blockiert UIPI sowohl Aktivierung als
  auch spätere `SendInput`-Injektion **stillschweigend**. Bug-Bezug: T4, A1–A5.

### §1.3 „Fokus leihen" via AttachThreadInput — korrekt und sicher
- `AttachThreadInput` teilt Input-State (Fokus + Tastaturzustand) zweier Threads → erlaubt `SetFocus`/
  `SetForegroundWindow` auf ein fremdes Fenster. **Muster:** (a) `GetWindowThreadProcessId(zielHwnd)` und
  `GetCurrentThreadId()`; (b) bei Ungleichheit `AttachThreadInput(mein, fremd, true)`; (c) aktivieren/
  fokussieren; (d) **im `finally` `AttachThreadInput(mein, fremd, false)`** — Detach NIE auslassen.
- **Vorbedingungen/Stolpersteine (offiziell):** Ein Thread kann sich **nicht an sich selbst** anhängen
  (`mein != fremd` prüfen); beide Threads brauchen eine Message-Queue; ein Journal-Record-Hook lässt es
  fehlschlagen; nicht über Desktop-Grenzen. Quelle: [AttachThreadInput](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-attachthreadinput) (2025-07-01) · `offiziell`. Action+`finally`-Muster (C#): [shlomio (2012)](https://shlomio.wordpress.com/2012/09/04/solved-setforegroundwindow-win32-api-not-always-works/) · `extern`. Bug-Bezug: A2 (Deadlock-Falle), A6.

### §1.4 Minimierte / schlafende Ziel-App in der Aktivierungssequenz
- **Erst restaurieren:** `if (IsIconic(zielHwnd)) ShowWindow(zielHwnd, SW_RESTORE);` VOR `SetForegroundWindow`
  — auf ein minimiertes Fenster tut Foreground nichts. Quelle: [ShowWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-showwindow) · `offiziell`; [IsIconic](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-isiconic) · `offiziell`.
- **Cold-Start-Pfad:** Wird die Ziel-App gerade gestartet/geweckt, vor dem Senden `WaitForInputIdle(prozessHandle, timeout)`
  — kehrt zurück, sobald die App ihre Message-Queue verarbeitet und empfangsbereit ist (statt blindem `Sleep`).
  Quelle: [WaitForInputIdle](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-waitforinputidle) · `offiziell`. Bug-Bezug: A5.

### §1.5 Fokus sauber zurückgeben
- VOR dem Einfügen das aktuelle Foreground-HWND merken (`GetForegroundWindow()`); NACH dem Einfügen, falls
  vorher eine andere App fokussiert war, dieselbe Aktivierungssequenz (§1.2–§1.4) auf das gemerkte HWND
  anwenden — **direkt** danach (verliehenes Recht verfällt beim nächsten Input). Da das Overlay
  `WS_EX_NOACTIVATE` trägt, „behält" es den Fokus ohnehin nie — der größte Teil der Sauberkeit ergibt sich
  schon aus dem Nicht-Aktivieren des Overlays.

### §1.6 Single-File-Deployment — was das Overlay betrifft
- Die Win32-P/Invokes der Injection (`SendInput`, `EnumChildWindows`, `SetForegroundWindow`, `AttachThreadInput`,
  `GetGUIThreadInfo`) liegen in `user32.dll` → vom Single-File **nicht** betroffen, funktionieren unverändert.
- **Kein Trimming/NativeAOT** (`PublishTrimmed=false`, kein `PublishAot`): WPF ist nicht trimmbar, COM-Marshalling
  (UIA/FlaUI) bricht sonst. `PublishSingleFile=true` + `SelfContained=true` ist der unterstützte Weg.
- **Eigenes `app.manifest`** einbetten: `supportedOS` Win10/11 + `<dpiAwareness>PerMonitorV2</dpiAwareness>` —
  Pflicht für pixelgenaue Maus-Injection (reine Tastatur-/Clipboard-Injection ist DPI-unabhängig).
- FlaUI-Interop: `Interop.UIAutomationClient` auf `<EmbedInteropTypes>false</EmbedInteropTypes>` setzen.
  Bug-Bezug: N1–N6. Detail-BP: [`best-practices-dotnet-csharp.md`](best-practices-dotnet-csharp.md).

---

## §2 Säule 2 — Das Einfüge-System (Text-Injection)

> **Methoden-Rangfolge (Begründung in §2.4):** **1.** Clipboard + echtes Strg+V via `SendInput`
> (Scancodes) — einziger Weg, der bei contenteditable zuverlässig den nativen Paste-Pfad triggert und
> Unicode/mehrzeilig verlustfrei trägt. **2.** `ValuePattern.SetValue` nur als optionaler Schnellpfad für
> einzeilige Felder. **3.** `SendKeys` nur Notnagel. **NICHT verwenden:** `TextPattern` (read-only),
> `WM_PASTE`/`WM_SETTEXT` (kein Edit-Control), `KEYEVENTF_UNICODE`-Direkttippen als Primärweg.

### §2.1 Echtes Strg+V mit Hardware-Scancodes (`SendInput`)
- **`SendInput(cInputs, pInputs[], sizeof(INPUT))`** synthetisiert Tastendrücke; `cbSize` muss exakt
  `sizeof(INPUT)` sein. **Garantie:** Events werden seriell und nicht mit Fremd-Input vermischt eingespeist —
  **aber nur, wenn alle in EINEM Aufruf als Array übergeben** werden (nicht vier Einzelaufrufe). Rückgabewert =
  Anzahl eingefügter Events; `0` = blockiert (anderer Thread/UIPI). Quelle: [SendInput](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-sendinput) (2025-07-01) · `offiziell`.
- **Scancode statt Virtual-Key:** `wScan = MapVirtualKey(VK_CONTROL, MAPVK_VK_TO_VSC)` bzw.
  `MapVirtualKey('V', MAPVK_VK_TO_VSC)`, dazu `wVk = 0` und Flag `KEYEVENTF_SCANCODE (0x0008)`. Begründung
  (Originaldoku): der Virtual-Key kann je Layout/gedrückte Tasten variieren, der Scancode ist stabil — ein
  „echterer" Hardware-Tastendruck, den der Chromium-Renderer als Strg+V-Akzelerator akzeptiert. `KEYEVENTF_KEYUP (0x0002)`
  fürs Loslassen. Quellen: [KEYBDINPUT](https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput) · `offiziell`; [MapVirtualKey](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-mapvirtualkeya) · `offiziell`.
- **Kanonische Reihenfolge (ein 4-Element-Array):** `Ctrl↓ → V↓ → V↑ → Ctrl↑`. Quelle (offizielles Muster
  Modifier↓/Taste↓/Taste↑/Modifier↑): [SendInput-Doku-Beispiel](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-sendinput) · `offiziell`; identisches Strg+V-Beispiel + „alle 4 in einem Array"-Hinweis: [batchloaf (2012)](https://batchloaf.wordpress.com/2012/10/18/simulating-a-ctrl-v-keystroke-in-win32-c-or-c-using-sendinput/) · `extern`.
- **`keybd_event` ist veraltet** (Deprecation-Hinweis in der Doku): kein Scancode, mit echtem Input vermischbar →
  immer `SendInput`. Quelle: [keybd_event](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-keybd_event) · `offiziell`. Bug-Bezug: T1, T2, T3, E6.

### §2.2 Zwischenablage in WPF/.NET 10 korrekt setzen
- **`Clipboard.SetDataObject(text, copy:true)`** (oder `Clipboard.SetText(text)`): `copy:true` legt die Daten
  **persistent** ab (OLE-Flush), sodass sie nach Tool-Ende erhalten bleiben. `SetText` schreibt als
  `UnicodeText` (UTF-16) → Chromium liest `CF_UNICODETEXT` bevorzugt; exakt das richtige Format für
  contenteditable. Quellen: [Clipboard.SetDataObject](https://learn.microsoft.com/en-us/dotnet/api/system.windows.clipboard.setdataobject) · `offiziell`; [Clipboard.SetText](https://learn.microsoft.com/en-us/dotnet/api/system.windows.clipboard.settext) · `offiziell`.
- **STA-Pflicht:** `Clipboard` nur aus einem STA-Thread. Der WPF-UI-Thread ist STA; aus Worker-Threads über
  `Application.Current.Dispatcher.Invoke(...)` marshallen oder eigenen Thread mit
  `SetApartmentState(ApartmentState.STA)` erzeugen. Bug-Bezug: C5.
- **Retry gegen `CLIPBRD_E_CANT_OPEN (0x800401D0)`:** Nur ein Prozess darf die Zwischenablage gleichzeitig
  offen halten; hält ein Clipboard-Manager/Antivirus sie, wirft `SetText` eine `ExternalException`. WinForms
  `Clipboard.SetDataObject(data, copy, retryTimes: 10, retryDelay: 100)` hat dafür eine eingebaute Retry-
  Überladung; in WPF manuell 5–10× je 100–200 ms umwickeln. Quelle: [Clipboard.SetDataObject (WinForms, Retry-Overload)](https://learn.microsoft.com/en-us/dotnet/api/system.windows.forms.clipboard.setdataobject) · `offiziell`; [dotnet/wpf #9901](https://github.com/dotnet/wpf/issues/9901) · `extern`. Bug-Bezug: C1, C7.
- **Verzögertes Restore:** Der Paste im Chromium-Renderer läuft **asynchron**. Den Originalinhalt erst nach
  Verzögerung (≈300–500 ms) bzw. nach bestätigtem Paste wiederherstellen — sonst liest Chromium beim Paste den
  alten Inhalt. Pragmatisch bei einem reinen Einfüge-Tool: nur Plaintext sichern oder Restore ganz weglassen.
  Bug-Bezug: C2, C3. .NET-10-Hinweis: reiner Text ist vom BinaryFormatter-Aus NICHT betroffen (Bug N6).

### §2.3 Mehrzeilig, Sonderzeichen, Unicode/Emoji
- Über den Clipboard-Weg **trivial gelöst**: `CF_UNICODETEXT` (UTF-16) transportiert mehrzeiligen Text (`\r\n`),
  Sonderzeichen und Emoji/Surrogate-Pairs (z. B. U+1F37A → `0xD83C 0xDF7A`) 1:1; Chromium fügt alles in einem
  Rutsch ein. **Keine** Zeichen-für-Zeichen-Simulation. Würde man direkt via `KEYEVENTF_UNICODE` tippen, müsste
  jedes Emoji als zwei `INPUT`-Events (High/Low-Surrogate) gesendet werden und Chromium verarbeitet diese
  synthetischen Unicode-Events inkonsistent. Quellen: [KEYBDINPUT (`KEYEVENTF_UNICODE`/`VK_PACKET`)](https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-keybdinput) · `offiziell`; [Surrogates & Supplementary Characters](https://learn.microsoft.com/en-us/windows/win32/intl/surrogates-and-supplementary-characters) · `offiziell`. Bug-Bezug: E6, E7.

### §2.4 Methodenvergleich mit klarer Empfehlung
| Methode | Bei contenteditable (Electron/Chromium)? | Begründung / Quelle |
|---------|------------------------------------------|---------------------|
| **(a) Clipboard + echtes Strg+V (Scancode)** | **JA — einzig zuverlässig** | Triggert den nativen Paste-Handler + DOM-`paste`-Event; trägt Unicode/mehrzeilig. PowerToys nutzt exakt das für Electron. |
| (b) `ValuePattern.SetValue` | nur einzeilige Edit-Controls | MS-Doku: gilt für Single-Line; Multi-Line/Document → kein ValuePattern. |
| (c) `TextPattern` | NEIN (zum Schreiben) | MS-Doku wörtlich: *„TextPattern does not support setting the text … text input must be simulated."* — read-only. |
| (d) `WM_PASTE`/`WM_SETTEXT` ans HWND | NEIN | adressiert Win32-Edit-Controls; Web-Content hat kein solches Ziel-HWND. |
| (e) `SendKeys` | unzuverlässig | VKs ohne Scancode, kein Timing/Fokus-Management — fragil. |

`ValuePattern.SetValue` bleibt der **optionale Schnellpfad** für den seltenen Fall, dass ein einzeiliges Feld
es freigibt (wirft sonst `InvalidOperationException`/`ElementNotEnabledException`). Quelle:
[ValuePattern.SetValue](https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.valuepattern.setvalue) · `offiziell`; [TextPattern Overview](https://learn.microsoft.com/en-us/dotnet/framework/ui-automation/ui-automation-textpattern-overview) · `offiziell`. Bug-Bezug: U8, T5, E1, E8.

### §2.5 Optionales automatisches Enter (Absenden)
- Als **separater** `SendInput`-Aufruf NACH dem Paste: 2-Element-Array `Enter↓ + Enter↑`, ebenfalls Scancode
  (`MapVirtualKey(VK_RETURN, MAPVK_VK_TO_VSC)`, `KEYEVENTF_SCANCODE`). **Mit kleiner Verzögerung nach dem
  Paste** senden, damit der eingefügte Text verarbeitet ist — sonst Race: Enter trifft ein leeres Feld. In
  Claude Desktop löst Enter das Absenden aus → nur aktivieren, wenn Auto-Submit gewollt ist.

### §2.6 Idempotenz / Race-Vermeidung bei schnellen Folge-Einfügungen
- **Serielle Queue mit Sequenznummer:** nie zwei Einfügungen parallel — gemeinsame Ressource ist die EINE
  Systemzwischenablage + der EINE Fokus. Jede Operation: Clipboard setzen → (verifizieren) → Paste → (optional
  Restore) → erst dann nächste.
- **Gehaltene Modifier neutralisieren:** Wurde der Vorgang per Hotkey ausgelöst, hält Windows evtl. noch
  `Win/Shift/Ctrl/Alt`; das injizierte `Ctrl+V` käme dann als `Win+Shift+Ctrl+V` an → Paste passiert nicht.
  Vor dem Block per `GetAsyncKeyState` prüfen und gehaltene Modifier per KEYUP loslassen, danach ggf.
  wiederherstellen. Quelle (genau dieses Electron-contenteditable-Problem + Fix): [microsoft/PowerToys PR #46486](https://github.com/microsoft/PowerToys/pull/46486) · `offiziell`.
- **Fokus zuerst + kurz warten:** `SetForegroundWindow` ist asynchron — vor dem Strg+V ~50 ms warten, sonst
  gehen Keystrokes verloren, obwohl `GetForegroundWindow` schon den neuen Wert meldet. `extern` (Win32-Timing).
- **Verifizieren statt annehmen:** vor dem Paste prüfen, dass der eigene Text wirklich in der Zwischenablage
  liegt (`GetClipboardSequenceNumber` änderte sich); nach `SendInput` Rückgabewert == 4 prüfen (sonst Logging,
  möglicher UIPI-Block). **Sender nie elevated** (UIPI blockiert sonst still). Bug-Bezug: C2, T4, A6.

---

## §3 Säule 3 — Erkennungssystem: das aktive Feld positions-unabhängig finden (Herzstück)

> **Kernprinzip:** Die zuverlässige, positions-unabhängige Lösung ist **UI Automation**, nicht Pixel.
> `FocusedElement` folgt dem Fokus automatisch — egal ob das Feld unten (Chat/Code) oder in der Mitte steht
> und nach unten rutscht (Cowork). Glücksfall bei Chromium 146: nativer UIA-Provider an (§0). **Jede Iteration
> Fokus/Position LIVE neu auswerten, nichts cachen** — genau das macht die Kette immun gegen das wandernde Feld.

### §3.1 Primär: `AutomationElement.FocusedElement`
- Das aktuell fokussierte Element koordinaten-frei über `AutomationElement.FocusedElement` (UIA 2.0) bzw.
  `IUIAutomation::GetFocusedElement` (UIA 3.0). Danach verifizieren: `ControlType.Edit` (einzeilig) **oder**
  `ControlType.Document` (mehrzeilig/Rich-Text — contenteditable wird oft als Document mit TextPattern
  exponiert), plus `IsKeyboardFocusable == true`, optional `IsTextPatternAvailable`. **Wichtig:** UIA-Aufrufe
  immer auf einem **eigenen Worker-Thread** (STA) ausführen, nie auf dem eigenen WPF-UI-Thread (sonst
  Deadlock-Gefahr). Quellen: [AutomationElement.FocusedElement](https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.automationelement.focusedelement) · `offiziell`; [IUIAutomation::GetFocusedElement](https://learn.microsoft.com/en-us/windows/win32/api/uiautomationclient/nf-uiautomationclient-iuiautomation-iuiautomation-getfocusedelement) · `offiziell`. Bug-Bezug: U3, U4.
- **Praxis-Korrektur (real getroffen 2026-06-15, CVO #46796 v2.1.3):** Den ControlType-Filter NICHT auf
  `Edit`/`Document` beschraenken. Chromium meldet dasselbe contenteditable je nach ARIA-Rolle auch als
  **`Group`** — im Claude-**Code**-Tab ist das Feld 'Prompt' eine `Group` (Chat/Cowork = `Edit`). Akzeptiere
  das **fokussierte** Element daher ControlType-tolerant (`Edit`/`Document`/`Group`) bei `IsKeyboardFocusable`;
  es hat bereits den Fokus, `SetFocus` ist nur best-effort. Schliesse zwingend die Seiten-Wurzel aus
  (**Root-Guard**: `AutomationId=="RootWebArea"` bzw. quasi bildschirmfuellendes Rect) — sonst fokussiert der
  Fallback `FindFirst(Edit|Document)` die ganze Render-Flaeche (`RootWebArea`, z.B. 3840x2064) und Strg+V
  verpufft. Bug-Bezug: U9.

### §3.2 Fokus liegt auf einem Container statt aufs Feld → gezielt suchen
- Mit `TreeWalker.ControlViewWalker` navigieren (überspringt rein strukturelle Knoten — dieselbe View, die
  Screenreader sehen; enthält das aktive Textfeld); `RawViewWalker` nur, wenn man wirklich jeden Knoten braucht.
  Gezielte Suche: `FindFirst(TreeScope.Descendants, new AndCondition(new PropertyCondition(ControlTypeProperty, Edit/Document), new PropertyCondition(IsKeyboardFocusableProperty, true)))`.
  Quellen: [TreeWalker.ControlViewWalker](https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.treewalker.controlviewwalker) · `offiziell`; [Navigate with TreeWalker](https://learn.microsoft.com/en-us/dotnet/framework/ui-automation/navigate-among-ui-automation-elements-with-treewalker) · `offiziell`; [IsKeyboardFocusableProperty](https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.automationelement.iskeyboardfocusableproperty) · `offiziell`. Bug-Bezug: U6 (`FindFirst` + enger Scope statt `FindAll`/Descendants über den ganzen Baum).

### §3.3 UIA 3.0 statt 2.0 — in .NET über FlaUI
- **Native UIA 3.0** (COM `IUIAutomation`) ist für moderne Apps (Chromium/Electron zählt dazu) empfohlen; **UIA
  2.0** (managed `System.Windows.Automation`) ist Legacy/feature-arm. In .NET am praktischsten über die
  Bibliothek **FlaUI.UIA3** (kapselt das COM-Interop, gleiche API-Oberfläche; Backend UIA2/UIA3 austauschbar).
  Quellen: [FlaUI](https://github.com/FlaUI/FlaUI) · `extern`; [FlaUI-Architektur](https://deepwiki.com/FlaUI/FlaUI/2-architecture) · `extern`. Single-File-Falle: `Interop.UIAutomationClient` mit `EmbedInteropTypes=false` (§1.6). Bug-Bezug: U5, N4.

### §3.4 Chromium-Accessibility scharfschalten / sicherstellen, dass der Baum da ist
- Bei Chromium 138+ ist der native UIA-Provider standardmäßig aktiv (§0). Chromium baut den A11y-Baum aber
  **lazy** auf — sobald ein UIA-Client den Baum abfragt, schickt Windows `WM_GETOBJECT` an Chromiums Fenster;
  Chromium nimmt dann „assistive technology läuft" an und aktiviert die Accessibility **on-demand**. **Das
  externe Tool IST diese assistive technology** — der erste UIA-Zugriff (`FocusedElement`/`ElementFromHandle`)
  weckt den Baum; man muss an Claude Desktop nichts ändern. **Notfall** (falls der Baum leer/unvollständig
  bleibt): Ziel-App mit `--force-renderer-accessibility` starten. Quellen: [Chromium UI Automation-Doku](https://chromium.googlesource.com/chromium/src.git/+/refs/heads/main/docs/accessibility/browser/uiautomation.md) · `offiziell`; [Electron Accessibility](https://www.electronjs.org/docs/latest/tutorial/accessibility/) · `offiziell`. Bug-Bezug: U1, U2, U7.

### §3.5 Pixel-Rechteck nur bei Bedarf — `BoundingRectangle` (+ `SetFocus`) live
- Braucht man doch eine Koordinate (Klick), `AutomationElement.Current.BoundingRectangle` zur Laufzeit frisch
  abfragen (physische Bildschirmkoordinaten) — **nie cachen**. Vorher auf `Rect.Empty` prüfen (Element nicht
  sichtbar/gescrollt) und zum Klicken die Mitte mit Sicherheitsabstand nehmen. `SetFocus()` setzt voraus, dass
  `IsKeyboardFocusable == true`, und bringt das Element nicht zwingend nach vorne (ggf. Fenster vorher
  aktivieren, §1). Quellen: [BoundingRectangleProperty](https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.automationelement.boundingrectangleproperty) · `offiziell`; [AutomationElement.SetFocus](https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.automationelement.setfocus) · `offiziell`. Bug-Bezug: M1, M4, F6.

### §3.6 Fallback: Caret-Position via `GetGUIThreadInfo`
- Nur wenn UIA kein verwertbares Element/Rect liefert: `GetGUIThreadInfo(threadId, ref GUITHREADINFO)`
  (`cbSize` vorher setzen, `threadId=0` für Vordergrund-Thread). `hwndCaret` + `rcCaret` (Client-Koordinaten →
  `ClientToScreen` umrechnen). Bei Chromium zeichnet der Renderer den Caret oft selbst → `hwndCaret` ggf. leer
  → **nur Notnagel**, nicht pixelgenau. Quellen: [GetGUIThreadInfo](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getguithreadinfo) · `offiziell`; [GUITHREADINFO](https://learn.microsoft.com/en-us/windows/win32/api/winuser/ns-winuser-guithreadinfo) · `offiziell`.

### §3.7 Letzter Notnagel: HWND-Suche `EnumChildWindows` → `Chrome_RenderWidgetHostHWND`
- Vom Top-Level-Fenster aus **rekursiv** mit `EnumChildWindows` absteigen und das Kind mit Klassenname
  `Chrome_RenderWidgetHostHWND` suchen — **`EnumChildWindows` einmal aufrufen** (es rekursiert selbst), im
  Callback **flach** per `GetClassName` filtern; **Zwischen-Klassennamen (`Chrome_WidgetWin_0/_1`, „Intermediate
  D3D Window") NIE fest verdrahten** (variieren je Version/GPU-Modus). Mit dem gefundenen HWND wieder UIA
  aufsetzen: `AutomationElement.FromHandle(hwnd)`. `Chrome_RenderWidgetHostHWND` ist laut Chromium-Devs ein
  „Hack"/Kompatibilitätsrelikt → nur Fallback. Quellen: [EnumChildWindows](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-enumchildwindows) · `offiziell`; [Raymond Chen — EnumChildWindows rekursiert](https://devblogs.microsoft.com/oldnewthing/20070116-04/?p=28393) · `extern`; [chromium-dev (HWND-Kette)](https://groups.google.com/a/chromium.org/g/chromium-dev/c/hmBh5YHjOFY) · `extern`. Bug-Bezug: F1, F2, F3, F4, F5.

### §3.8 Die robuste Fallback-Kette (geordnet)
1. **`FocusedElement` direkt** (Worker-Thread): `ControlType.Edit/Document` + `IsKeyboardFocusable` prüfen → positions-unabhängig, folgt dem Fokus. *(§3.1)*
2. **UIA-Suche im Baum** (`ControlViewWalker`/`FindFirst(Descendants, AndCondition)`), falls Fokus auf Container liegt. *(§3.2)*
3. **`BoundingRectangle` live** des gefundenen Elements, nur wenn eine Koordinate gebraucht wird (nie cachen, `Rect.Empty` prüfen). *(§3.5)*
4. **Caret via `GetGUIThreadInfo`**, nur wenn UIA gar nichts liefert (bei Chromium oft leer). *(§3.6)*
5. **HWND-Notnagel** `EnumChildWindows → Chrome_RenderWidgetHostHWND`, dann erneut UIA darauf. *(§3.7)*

**Querschnitt-Regel:** in jeder Iteration Fokus/Position frisch abfragen — kein Caching. Das ist der Grund,
warum die Kette das wandernde Feld in Chat/Cowork/Code zuverlässig trifft.

---

## §4 Säule 4 — Debugging (Einfüge-/Erkennungsfehler finden)

> **Faustregel:** erst *sehen* (existiert das Element und wie?), dann *zielen* (richtiges HWND/Element?),
> dann *messen* (kam Fokus + Paste an — per unabhängigem Signal?).

### §4.0 A11y zuerst erzwingen
- Ohne aktivierten A11y-Baum sind alle Inspektoren blind. Zum Debuggen Claude Desktop mit
  `--force-renderer-accessibility` starten (oder Narrator kurz an / NVDA laufen lassen). Im Produktivbetrieb
  reicht die On-demand-Aktivierung (§3.4). Quelle: [Chromium accessibility inspect README](https://chromium.googlesource.com/chromium/src/+/refs/heads/main/tools/accessibility/inspect/README.md) · `offiziell`.

### §4.1 Accessibility Insights for Windows (Erst-Inspektor)
- Live-Inspect des UIA-Baums jeder fremden App: zeigt **ControlType**, Accessible Name, alle **Properties**
  (`IsKeyboardFocusable`, `HasKeyboardFocus`, `IsVisible`, `BoundingRectangle`) und unterstützte **Patterns**
  (ValuePattern/TextPattern). Damit prüft man, ob das Claude-Eingabefeld als `Edit` oder `Document` exponiert
  wird und welche Patterns es trägt. Workflow: „Inspect" → Element-Modus → übers Feld hovern/fokussieren →
  „Pause UIA Tree" → Patterns prüfen. Offiziell empfohlener Ersatz für Inspect.exe. Quellen: [Übersicht](https://accessibilityinsights.io/docs/windows/overview/) · `offiziell`; [Inspect-Anleitung](https://accessibilityinsights.io/docs/windows/getstarted/inspect/) · `offiziell`; [Event-Monitoring (Fokus-/StructureChanged-Events)](https://accessibilityinsights.io/docs/windows/getstarted/eventmonitoring/) · `offiziell`.

### §4.2 Inspect.exe (Windows SDK)
- UIA- **und** MSAA-Baum; „Watch Focus" verfolgt den Fokus → zeigt FocusedElement, ControlType, Properties,
  Patterns; umschaltbar UIA/MSAA und Raw/Control/Content-View (ein Feld kann in einer View sichtbar, in der
  anderen gefiltert sein). Pfad: `C:\Program Files (x86)\Windows Kits\10\bin\<10.0.x.x>\x64\inspect.exe`
  (kommt mit dem Windows SDK, kein Admin nötig). „Show Highlight Rectangle" bestätigt visuell, dass das
  gefundene Element wirklich das sichtbare Feld umrandet. Quelle: [Inspect (Microsoft Learn)](https://learn.microsoft.com/en-us/windows/win32/winauto/inspect-objects) (2025-07-14) · `offiziell`.

### §4.3 Spy++ (Visual Studio)
- **HWND-/Fensterklassen-Hierarchie** + Live-Message-Mitschnitt. Verifiziert die Chromium-Kette
  (`Chrome_WidgetWin_1` → … → `Chrome_RenderWidgetHostHWND`) und dass das Tool das richtige, aktuelle HWND
  adressiert (nach Tab-/View-Wechsel tauscht Chromium das Handle aus → gecachtes HWND zeigt ins Leere).
  Messages-View (Logging Options) für `WM_KEYDOWN`/`WM_CHAR`/`WM_SETFOCUS` am Ziel-HWND — `WM_SETTEXT`/
  `WM_PASTE` greifen bei contenteditable erwartungsgemäß NICHT. Quellen: [Spy++ Überblick](https://learn.microsoft.com/en-us/visualstudio/debugger/using-spy-increment) · `offiziell`; [Messages-View](https://learn.microsoft.com/en-us/visualstudio/debugger/how-to-use-messages-view) · `offiziell`.

### §4.4 Programmatische A11y-Tree-Dumps
- **Chromium `ax_dump_tree` / `ax_dump_events`** (offizielle CLI): vollständiger Baum bzw. Live-Events (Fokus/
  Text-Änderung). App-Wahl per `--pattern=*Claude*`; **immer `--api=uia`** (zeigt exakt, was das UIA-Tool sieht;
  Default wäre `ia2`). Quelle: [Chromium accessibility inspect README](https://chromium.googlesource.com/chromium/src/+/refs/heads/main/tools/accessibility/inspect/README.md) · `offiziell`.
- **FlaUInspect** (.NET, gleicher Stack wie das Tool): UIA-Baum + Detail-Properties, Hover-Modus mit Strg.
  Quelle: [FlaUInspect](https://github.com/FlaUI/FlaUInspect) · `extern`.
- **`chrome://accessibility`** (über Remote-Debugging) zum Querverifizieren der Blink-Rolle (textbox/document),
  bevor man auf die UIA-Projektion schaut. Quelle: [Chrome — full accessibility tree](https://developer.chrome.com/blog/full-accessibility-tree) · `offiziell`.

### §4.5 Strukturiertes Logging + Logik-Sonden (erwartet vs. tatsächlich)
- **JSON-Lines pro Einfügeversuch** loggen (passt zur Observability-First-Direktive): `FocusedElement`
  (Name, ControlType, `IsKeyboardFocusable`, `HasKeyboardFocus`, verfügbare Patterns), gefundenes **HWND +
  Klasse** (und ob es seit dem letzten Versuch wechselte), **BoundingRectangle** (on-screen, nicht 0×0),
  **`SetForegroundWindow`-Ergebnis**, **`SendInput`-Rückgabewert** (== 4?) + `Marshal.GetLastWin32Error()`,
  **`GetClipboardSequenceNumber`** vorher/nachher.
- **Sonde „kam der Paste an?"** — nach dem Paste den **Feldwert auslesen und gegen den erwarteten Text
  vergleichen**: einzeilige Felder via `ValuePattern.Current.Value`; **mehrzeilige Felder via `TextPattern`**
  (sie haben KEIN ValuePattern — wer stur ValuePattern erwartet, scheitert hier, obwohl der Paste evtl.
  funktionierte). Quellen: [ValuePattern.Value](https://learn.microsoft.com/en-us/dotnet/api/system.windows.automation.valuepattern.valuepatterninformation.value) · `offiziell`; [Value-Pattern (ein-/mehrzeilig)](https://learn.microsoft.com/en-us/dotnet/framework/ui-automation/implementing-the-ui-automation-value-control-pattern) · `offiziell`; [GetClipboardSequenceNumber](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getclipboardsequencenumber) · `offiziell`.
- **Sonde „kam der Fokus an?"** — nach dem Aktivieren `FocusedElement` lesen und gegen das erwartete Zielelement
  (RuntimeId/BoundingRectangle) abgleichen; `HasKeyboardFocus` als Bedingung.
- **Live mitlesen:** parallel `ax_dump_events --api=uia --pattern=*Claude*` → unabhängiges Signal, ob Focus-
  und Text-/Value-Changed-Events feuern.

### §4.6 Empfohlene Debugging-Reihenfolge
1. A11y erzwingen (`--force-renderer-accessibility`) — sonst sind alle Tools blind.
2. **Accessibility Insights** (oder Inspect.exe, „Watch Focus", UIA-Modus): exponiert sich das Feld als
   Edit/Document? Welche Patterns? BoundingRectangle plausibel?
3. **Spy++** Windows-View: HWND-/Klassenkette verifizieren, richtiges aktuelles HWND.
4. **`ax_dump_tree --api=uia`** / FlaUInspect als Referenz-Soll-Zustand; **`ax_dump_events --api=uia`** live.
5. **JSON-Lines-Logging** einbauen (siehe §4.5).
6. **Logik-Sonden**: Feldwert per ValuePattern (einzeilig)/TextPattern (mehrzeilig) gegen Erwartung; bei Bedarf
   Spy++ Messages-View für `WM_KEYDOWN`/`WM_CHAR`, falls Tastatur-Input nicht ankommt.

---

## §5 Die empfohlene Gesamt-Sequenz (alles zusammengesetzt)

**Einmalig (Overlay-Setup):** `ShowActivated=false` + `Topmost=true`; im `OnSourceInitialized` HWND holen und
`WS_EX_NOACTIVATE|WS_EX_TOOLWINDOW` setzen; initial `SetWindowPos(HWND_TOPMOST, …, SWP_NOACTIVATE)`; Topmost
ereignisgesteuert re-asserten. Single-File ohne Trimming/AOT, eigenes `app.manifest` (PerMonitorV2),
`EmbedInteropTypes=false` für UIA-Interop. Sender **nicht** elevated. UIA auf eigenem STA-Worker-Thread.

**Pro Einfüge-Operation (seriell, mit Sequenznummer):**
1. Ziel-App über **Prozessname** (`claude.exe`) + Mapping-Tabelle bestimmen (nicht über Fenstertitel/Klasse).
2. Vorheriges Foreground-HWND merken (`GetForegroundWindow`).
3. Feld finden — **Fallback-Kette §3.8**: `FocusedElement` → UIA-Suche → (BoundingRectangle nur falls Koordinate nötig).
4. Clipboard setzen: `SetDataObject(text, copy:true)` mit Retry-Schleife; per `GetClipboardSequenceNumber` verifizieren.
5. Ziel aktivieren: ggf. `IsIconic→SW_RESTORE`; `AttachThreadInput(mein, ziel, true)`; `AllowSetForegroundWindow(zielPid)`; `SetForegroundWindow` (Rückgabe prüfen, sonst `FlashWindowEx`); UIA-`SetFocus()` aufs Feld; ~50 ms warten.
6. Gehaltene Modifier per KEYUP neutralisieren (PowerToys-Fix).
7. **Ein** `SendInput`-Array `Ctrl↓ V↓ V↑ Ctrl↑` mit `KEYEVENTF_SCANCODE`; Rückgabe == 4 prüfen.
8. Optional: nach kurzer Verzögerung separater `SendInput` Enter↓/↑ (nur wenn Auto-Submit gewollt).
9. Fokus zurückgeben (gemerktes HWND, §1.5); `AttachThreadInput(…, false)` im `finally`.
10. Clipboard verzögert restoren (≈300–500 ms); **Logik-Sonde**: Feldwert per ValuePattern/TextPattern gegen Erwartung; dann nächste Queue-Operation freigeben.

---

## Bezugstabelle: Best-Practice-Abschnitt ↔ Bug-Almanach-Sektion

> Gegenstück: [`bugs/desktop/windows-electron-text-injection.md`](../../bugs/desktop/windows-electron-text-injection.md).
> Diese Tabelle in BEIDEN Dateien synchron halten.

| BP-Abschnitt (hier) | Thema | Bug-Almanach-Sektion(en) |
|---------------------|-------|--------------------------|
| §0 Versions-Anker / Zukunftssicherheit | Electron/Chromium-Version, App-Unterschiede | E4, E5 |
| §1.1 Overlay nicht-aktivierend | Topmost/NOACTIVATE/Fokus | A7, W1 |
| §1.2 Ziel-App legal nach vorne | SetForegroundWindow/AllowSetForegroundWindow/UIPI | A1, A3, A4, T4 |
| §1.3 Fokus leihen (AttachThreadInput) | Attach/Detach, Deadlock | A2, A6 |
| §1.4 Minimiert/Cold-Start | SW_RESTORE/WaitForInputIdle | A5 |
| §1.6 Single-File-Deployment | Helper-DLLs, Trimming, Manifest, FlaUI-Interop | N1, N2, N3, N4, N5 |
| §2.1 SendInput-Scancode | keybd_event, Scancode, Timing | T1, T2, T3, E6 |
| §2.2 Clipboard korrekt | CLIPBRD_E_CANT_OPEN, Restore-Race, STA, .NET 10 | C1, C2, C3, C5, C7, N6 |
| §2.3 Unicode/mehrzeilig | KEYEVENTF_UNICODE/VK_PACKET | E6, E7 |
| §2.4 Methodenvergleich | ValuePattern/TextPattern/WM_PASTE | U8, T5, E1, E8 |
| §2.6 Idempotenz/Race | Modifier, Fokus-Timing, UIPI | C2, T4, A6 |
| §3.1 FocusedElement | FocusedElement null/Threading | U3, U4 |
| §3.2 TreeWalker/FindFirst | FindAll/Descendants hängt | U6 |
| §3.3 UIA3/FlaUI | UIA 2.0 vs 3.0, Single-File-Interop | U5, N4 |
| §3.4 Chromium-A11y scharfschalten | A11y aus/lazy, Baum-Abbau, Latenz | U1, U2, U7 |
| §3.5 BoundingRectangle live | wanderndes Feld, DPI, 0-Rect | M1, M4, F6 |
| §3.7 HWND-Notnagel | FindWindowEx/EnumChildWindows/Klassennamen/OSR | F1, F2, F3, F4, F5 |
| §4 Debugging | Diagnose-Werkzeuge, Verifikation | (querschnittlich; v. a. U1, F-Sektion) |
| §5 Gesamt-Sequenz | belegte funktionierende Wege | K1, E7, E8 + Pflicht-Checkliste |

---

## Quellen- & Ehrlichkeits-Notiz
- **Versions-Anker** (Electron 41 = Chromium 146.0.7680.65; Electron 42 = Chromium 148; nativer UIA seit
  Chromium 138; Policy `UiAutomationProviderEnabled` nur bis Chrome 146): durchgängig `offiziell` belegt
  (electronjs.org-Release-Blogs, Chrome-for-Developers). **Nicht offiziell belegbar** ist die Zuordnung der
  Anthropic-App-Version 1.12603.1 zu einer konkreten Electron-Major — daher die Empfehlung, die Version zur
  Laufzeit auszulesen (§0).
- **Alle API-Bedingungen, Flags und Sequenzen** (SendInput/Scancode, Clipboard, UIA, AttachThreadInput,
  SetForegroundWindow, GetGUIThreadInfo, EnumChildWindows) sind über Microsoft Learn / Chromium- / Electron-
  Docs `offiziell` belegt. Als `extern` markiert sind: das C#-AttachThreadInput-Muster, das batchloaf-Strg+V-
  Beispiel, FlaUI/FlaUInspect und die chromium-dev-HWND-Ketten-Diskussion — sie überstimmen das Offizielle nie.
- **Quer-Befund (ehrlich):** Es gibt keinen Beleg, dass Windows 11 24H2 das SetForegroundWindow-/DPI-Verhalten
  gegenüber 23H2 verschärft (Bug W4); 24H2-spezifisch ist v. a. die Z-Order-Regression (W1) und die latente,
  derzeit deaktivierte „Administrator Protection" (W2).
