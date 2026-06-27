# Windows-Electron-Text-Injection (C#/WPF → Claude Desktop) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
