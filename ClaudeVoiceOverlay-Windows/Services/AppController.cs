using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Automation;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Steuert die Ziel-App (Claude Desktop / Codex, Electron/Chromium) per
    /// UI Automation + echtem Strg+V. Schwester-Klasse zu TerminalVoiceOverlay/
    /// TerminalController: die oeffentliche API (PasteText[Async], ClearLine[Async],
    /// ClearAllInput[Async], CopySelection[Async], PasteClipboard[Async],
    /// PressReturn[Async], SendReturn) ist deckungsgleich, damit das gemeinsame
    /// OverlayWindow 1:1 funktioniert.
    ///
    /// KERN-STRATEGIE (Stand 2026-06-15, nach Bug-Almanach + Best-Practices
    /// `windows-electron-text-injection`): Das Eingabefeld von Claude Desktop ist
    /// ein HTML-contenteditable-DIV (ProseMirror/Lexical) in EINER grossen
    /// Chromium-Render-Flaeche — KEIN Win32-Edit-Control. Deshalb spiegeln wir den
    /// funktionierenden macOS-Accessibility-Weg:
    ///   1. Feld per UI Automation FINDEN + FOKUSSIEREN (positions-unabhaengig:
    ///      FocusedElement -> Baum-Suche Edit/Document -> HWND-Notnagel Render-Widget).
    ///   2. Text per Zwischenablage + echtem Strg+V mit Hardware-SCANCODES via
    ///      SendInput EINFUEGEN (Chromium verwirft reine Virtual-Keys / keybd_event).
    /// Bewusst NICHT mehr: fester Koordinaten-Klick, keybd_event ohne Scancode,
    /// FindWindowEx-Einzelsuche, WM_PASTE/WM_SETTEXT, ValuePattern.SetValue aufs
    /// contenteditable — alle vier versagen bei Chromium gleichzeitig.
    ///
    /// THREADING: Jede Operation laeuft auf einem dedizierten STA-Thread
    /// (RunOnStaThread). Damit ist (a) der Clipboard-Zugriff STA-konform,
    /// (b) UIA NICHT auf dem WPF-UI-Thread (Deadlock-Schutz, Best-Practices §3.1).
    /// </summary>
    public static class AppController
    {
        private const ushort VK_A         = 0x41;
        private const ushort VK_C         = 0x43;
        private const ushort VK_BACKSPACE = 0x08;
        private const ushort VK_RETURN    = 0x0D;

        private static readonly object InputSequenceGate = new();

        // ── Oeffentliche API: Async-Wrapper (haelt Win32-Sleeps vom UI-Thread) ──

        public static Task<bool> PasteTextAsync(string text, IntPtr appHwnd, bool autoEnter = false)
            => Task.Run(() => PasteText(text, appHwnd, autoEnter));

        public static Task<bool> ClearLineAsync(IntPtr appHwnd)     => Task.Run(() => ClearLine(appHwnd));
        public static Task<bool> ClearAllInputAsync(IntPtr appHwnd) => Task.Run(() => ClearAllInput(appHwnd));
        public static Task<bool> CopySelectionAsync(IntPtr appHwnd) => Task.Run(() => CopySelection(appHwnd));
        public static Task<bool> PasteClipboardAsync(IntPtr appHwnd, bool autoEnter = false)
            => Task.Run(() => PasteClipboard(appHwnd, autoEnter));
        public static Task<bool> PressReturnAsync(IntPtr appHwnd)   => Task.Run(() => PressReturn(appHwnd));

        // ── Oeffentliche API: synchrone Einstiegspunkte (laufen auf STA-Worker) ──

        public static bool PasteText(string text, IntPtr appHwnd, bool autoEnter = false)
        {
            lock (InputSequenceGate)
                return RunOnStaThread(() => PasteTextCore(text, appHwnd, autoEnter));
        }

        // Bei Electron-Feldern leeren ClearLine und ClearAllInput identisch das
        // gesamte contenteditable (Strg+A + Backspace) — es gibt keine Terminal-
        // "Zeile". API getrennt fuer OverlayWindow-Kompatibilitaet.
        public static bool ClearLine(IntPtr appHwnd)
        {
            lock (InputSequenceGate)
                return RunOnStaThread(() => ClearInputCore(appHwnd));
        }

        public static bool ClearAllInput(IntPtr appHwnd)
        {
            lock (InputSequenceGate)
                return RunOnStaThread(() => ClearInputCore(appHwnd));
        }

        public static bool CopySelection(IntPtr appHwnd)
        {
            lock (InputSequenceGate)
                return RunOnStaThread(() =>
                {
                    if (!ActivateTarget(appHwnd)) return false;
                    Thread.Sleep(40);
                    return SendComboScancode(Win32.VK_CONTROL, VK_C);
                });
        }

        public static bool PasteClipboard(IntPtr appHwnd, bool autoEnter = false)
        {
            lock (InputSequenceGate)
                return RunOnStaThread(() => PasteClipboardCore(appHwnd, autoEnter));
        }

        public static bool PressReturn(IntPtr appHwnd)
        {
            lock (InputSequenceGate)
                return RunOnStaThread(() =>
                {
                    if (!FocusTarget(appHwnd)) return false;
                    Thread.Sleep(40);
                    return SendKeyScancode(VK_RETURN);
                });
        }

        public static bool SendReturn()
        {
            lock (InputSequenceGate)
                return RunOnStaThread(() => SendKeyScancode(VK_RETURN));
        }

        // ── Kern-Abläufe ────────────────────────────────────────────────────

        private static bool PasteTextCore(string text, IntPtr appHwnd, bool autoEnter)
        {
            DiagLog.Write("Paste", "PasteText START",
                ("hwnd", "0x" + appHwnd.ToInt64().ToString("X")),
                ("len", text.Length), ("autoEnter", autoEnter));

            // 1. Zwischenablage setzen (STA-Thread → direkter Clipboard-Zugriff) + verifizieren
            if (!SetClipboardText(text, out IDataObject? previous, out uint ownedClipboardSequence))
            {
                DiagLog.Write("Paste", "ABBRUCH: Clipboard.SetText fehlgeschlagen");
                return false;
            }

            bool pasteSent = false;
            try
            {
                // 2. Ziel aktivieren + Eingabefeld per UIA finden/fokussieren (positions-unabhaengig)
                if (!FocusTarget(appHwnd)) return false;

            // 2b. LIVE-LOGIK-SONDE (Intent-Verifikation, observability-live-logic-probes):
            //     Sitzt der Tastaturfokus JETZT wirklich auf einem echten Eingabefeld (erwartet)
            //     oder noch auf Button/Seiten-Root (tatsaechlich)? CHECKPOINT 'erwartet vs. ist'
            //     -> ok:false bedeutet, Strg+V wird gleich verpuffen. Live mitlesbar:
            //     Get-Content "$env:LOCALAPPDATA\ClaudeVoiceOverlay\diag.log" -Wait -Tail 30
                if (!VerifyFocusCheckpoint("Fokus nach FocusTarget (vor Strg+V)")) return false;

            // 3. Gehaltene Hotkey-Modifier neutralisieren, sonst kommt "Win+Alt+Ctrl+V" an
                ReleaseHeldModifiers();

            // 4. Echtes Strg+V mit Hardware-Scancodes
                bool pasted = SendCtrlVScancode();
                DiagLog.Write("Paste", "Strg+V gesendet", ("sent", pasted), ("autoEnter", autoEnter));
                if (!pasted) return false;
                pasteSent = true;

            // 5. Optionales Enter (separat, nach kurzer Verzoegerung)
                if (autoEnter)
                {
                    Thread.Sleep(300);
                    if (!FocusTarget(appHwnd) || !SendKeyScancode(VK_RETURN)) return false;
                }

                return true;
            }
            finally
            {
                // Chromium muss den Clipboard-Inhalt vor der Wiederherstellung konsumieren.
                if (pasteSent) Thread.Sleep(600);
                RestoreClipboard(previous, ownedClipboardSequence);
            }
        }

        private static bool PasteClipboardCore(IntPtr appHwnd, bool autoEnter)
        {
            if (!FocusTarget(appHwnd)) return false;
            ReleaseHeldModifiers();
            if (!SendCtrlVScancode()) return false;

            if (!autoEnter) return true;

            Thread.Sleep(300);
            return FocusTarget(appHwnd) && SendKeyScancode(VK_RETURN);
        }

        private static bool ClearInputCore(IntPtr appHwnd)
        {
            if (!FocusTarget(appHwnd)) return false;
            ReleaseHeldModifiers();
            if (!SendComboScancode(Win32.VK_CONTROL, VK_A)) return false; // alles markieren
            Thread.Sleep(40);
            return SendKeyScancode(VK_BACKSPACE);      // loeschen
        }

        // ── Ziel aktivieren + Eingabefeld fokussieren ───────────────────────

        /// <summary>Aktiviert die Ziel-App und fokussiert ihr aktives Eingabefeld per UIA.</summary>
        private static bool FocusTarget(IntPtr appHwnd)
        {
            if (!ActivateTarget(appHwnd)) return false;
            Thread.Sleep(50);              // SetForegroundWindow ist asynchron
            return FocusInputFieldUia(appHwnd);
        }

        /// <summary>
        /// Bringt die Ziel-App legal in den Vordergrund (AllowSetForegroundWindow +
        /// AttachThreadInput-Leihe). Stellt minimierte Fenster wieder her.
        /// </summary>
        private static bool ActivateTarget(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero || !Win32.IsWindow(appHwnd)) return false;

            if (Win32.IsIconic(appHwnd))
                Win32.ShowWindow(appHwnd, Win32.SW_RESTORE);

            var currentFg = Win32.GetForegroundWindow();
            if (IsOwnedByProcess(currentFg, appHwnd))
                return true; // schon vorne

            uint ourThread = Win32.GetCurrentThreadId();
            uint targetThread = Win32.GetWindowThreadProcessId(appHwnd, out uint targetPid);

            bool attached = false;
            try
            {
                if (ourThread != targetThread)
                    attached = Win32.AttachThreadInput(ourThread, targetThread, true);

                Win32.AllowSetForegroundWindow(targetPid != 0 ? targetPid : Win32.ASFW_ANY);
                bool ok = Win32.SetForegroundWindow(appHwnd);
                Win32.BringWindowToTop(appHwnd);
                if (!ok)
                    Console.WriteLine("[AppController] SetForegroundWindow=false (Foreground-Lock?)");
                Thread.Sleep(120);
                return IsOwnedByProcess(Win32.GetForegroundWindow(), appHwnd);
            }
            finally
            {
                if (attached)
                    Win32.AttachThreadInput(ourThread, targetThread, false);
            }
        }

        /// <summary>
        /// Findet das aktive Eingabefeld der Ziel-App per UI Automation und
        /// fokussiert es — positions-unabhaengig (Chat unten, Cowork Mitte, Code
        /// unten). Fallback-Kette §3.8: FocusedElement → Baum-Suche → Render-Widget.
        /// </summary>
        private static bool FocusInputFieldUia(IntPtr appHwnd)
        {
            try
            {
                // 1. Das bereits fokussierte Element (folgt dem Fokus automatisch).
                //    Chromium meldet das contenteditable je nach ARIA-Rolle als Edit
                //    (Chat/Cowork) ODER Group (Code-Tab 'Prompt') — daher ControlType-
                //    tolerant. Hat das Element bereits den Tastaturfokus, landet Strg+V
                //    ohnehin dort: SetFocus ist nur best-effort, das Ergebnis ist egal.
                AutomationElement? focused = null;
                try { focused = AutomationElement.FocusedElement; }
                catch (Exception ex) { DiagLog.Write("UIA", "FocusedElement warf", ("err", ex.GetType().Name)); }
                bool focusedUsable = focused != null && IsUsableFocusTarget(focused);
                DiagLog.Write("UIA", "Pfad1 FocusedElement", ("el", Describe(focused)), ("usable", focusedUsable));
                if (focusedUsable)
                {
                    TrySetFocus(focused!); // idempotent; Element ist bereits fokussiert
                    DiagLog.Write("UIA", "Pfad1 OK: fokussiertes Eingabeziel akzeptiert", ("el", Describe(focused)));
                    return true;
                }

                // 2. Das echte Eingabefeld im Fensterbaum suchen (Fokus liegt auf einem Container).
                //    Bug-Historie (real getroffen 2026-06-15, per diag.log belegt) — zwei Faelle:
                //    (a) Cowork direkt nach beendeter Aufgabe: Fokus auf dem 'Fortschritt N von N'-
                //        Button -> Pfad1 verworfen. Frueher traf FindFirst(Edit|Document) als ERSTES
                //        die Seiten-Wurzel 'RootWebArea' (Document, bildschirmfuellend) und gab danach
                //        auf -> echtes Feld nie gefunden.
                //    (b) Code-Tab: Fokus auf dem bildschirmfuellenden Container 'dframe-main' -> Pfad1
                //        verworfen. Das echte Feld ist dort eine GROUP (name='Prompt'), KEIN Edit/
                //        Document -> die reine Edit/Document-Suche fand GAR NICHTS (null).
                //    In beiden Faellen verpuffte Strg+V; Text kam erst nach manuellem Maus-Klick.
                //    Fix: FindProseMirrorField() findet das echte 'tiptap ProseMirror'-contenteditable
                //    ueber ALLE Tabs (Chat/Cowork=Edit, Code=Group) per ClassName und ueberspringt
                //    bildschirmfuellende Container (RootWebArea / 'dframe-main').
                var root = AutomationElement.FromHandle(appHwnd);
                if (root != null)
                {
                    var field = FindProseMirrorField(root, TreeScope.Descendants);
                    DiagLog.Write("UIA", "Pfad2 FindProseMirrorField(Descendants)", ("found", Describe(field)));
                    if (field != null && TrySetFocus(field))
                    {
                        DiagLog.Write("UIA", "Pfad2 OK: Textfeld im Fensterbaum -> fokussiert", ("el", Describe(field)));
                        return true;
                    }
                }
                else
                {
                    DiagLog.Write("UIA", "Pfad2 uebersprungen: FromHandle=null", ("hwnd", "0x" + appHwnd.ToInt64().ToString("X")));
                }

                // 3. HWND-Notnagel: Render-Widget suchen, dann erneut UIA darauf
                IntPtr renderWidget = FindRenderWidget(appHwnd);
                DiagLog.Write("UIA", "Pfad3 RenderWidget", ("hwnd", "0x" + renderWidget.ToInt64().ToString("X")));
                if (renderWidget != IntPtr.Zero)
                {
                    var rwRoot = AutomationElement.FromHandle(renderWidget);
                    var field2 = rwRoot != null ? FindProseMirrorField(rwRoot, TreeScope.Subtree) : null;
                    DiagLog.Write("UIA", "Pfad3 FindProseMirrorField(Subtree)", ("found", Describe(field2)));
                    if (field2 != null && TrySetFocus(field2))
                    {
                        DiagLog.Write("UIA", "Pfad3 OK: Textfeld unter Render-Widget -> fokussiert", ("el", Describe(field2)));
                        return true;
                    }
                }

                DiagLog.Write("UIA", "KEIN Textfeld gefunden -> Paste geht an aktuelles Fokus-Element");
            }
            catch (Exception ex)
            {
                DiagLog.Write("UIA", "FocusInputFieldUia-Ausnahme", ("err", ex.GetType().Name), ("msg", ex.Message));
            }
            return false;
        }

        /// <summary>
        /// Ist das (bereits fokussierte) Element ein brauchbares Einfuege-Ziel?
        /// Chromium meldet dasselbe contenteditable je nach ARIA-Rolle als Edit,
        /// Document ODER Group (Claude Chat/Cowork = Edit, Code-Tab 'Prompt' = Group)
        /// — daher ControlType-tolerant. Ausgeschlossen wird nur die Seiten-Wurzel
        /// (RootWebArea, bildschirmfuellend), die KEIN Eingabefeld ist (Root-Guard).
        /// </summary>
        private static bool IsUsableFocusTarget(AutomationElement el)
        {
            try
            {
                var c = el.Current;
                if (!c.IsKeyboardFocusable) return false;
                if (IsPageRoot(el)) return false;
                var ct = c.ControlType;
                return ct == ControlType.Edit || ct == ControlType.Document || ct == ControlType.Group;
            }
            catch { return false; }
        }

        /// <summary>
        /// Erkennt die Web-/contenteditable-Wurzel der ganzen Seite (KEIN echtes Feld):
        /// Chromium gibt ihr stabil die AutomationId "RootWebArea"; als zusaetzliche
        /// Absicherung gilt auch ein quasi bildschirmfuellendes Rechteck als Wurzel.
        /// Verhindert, dass Pfad 2/3 faelschlich die ganze Render-Flaeche fokussieren
        /// (Fokus weg vom echten Feld -> Strg+V verpufft).
        /// </summary>
        private static bool IsPageRoot(AutomationElement el)
        {
            try
            {
                var c = el.Current;
                if (string.Equals(c.AutomationId, "RootWebArea", StringComparison.Ordinal)) return true;
                var r = c.BoundingRectangle;
                return !r.IsEmpty && r.Width >= 2400 && r.Height >= 1400; // ganze Render-Flaeche
            }
            catch { return false; }
        }

        /// <summary>
        /// Kompakte, werf-freie Beschreibung eines UIA-Elements fuers Diagnose-Log:
        /// ControlType, Name, AutomationId, ClassName, IsKeyboardFocusable, IsOffscreen
        /// und BoundingRectangle (die y-Position verraet, ob es das untere Eingabefeld
        /// oder z.B. der obere Code-Editor ist).
        /// </summary>
        private static string Describe(AutomationElement? el)
        {
            if (el == null) return "null";
            try
            {
                var c = el.Current;
                var r = c.BoundingRectangle;
                string type = c.ControlType?.ProgrammaticName?.Replace("ControlType.", "") ?? "?";
                return $"type={type} name='{Trunc(c.Name, 40)}' autoId='{Trunc(c.AutomationId, 24)}' "
                     + $"class='{Trunc(c.ClassName, 24)}' kbFocus={c.IsKeyboardFocusable} "
                     + $"offscreen={c.IsOffscreen} rect=[{(int)r.X},{(int)r.Y},{(int)r.Width}x{(int)r.Height}]";
            }
            catch (Exception ex) { return "describe-failed:" + ex.GetType().Name; }
        }

        private static string Trunc(string? s, int max)
        {
            if (string.IsNullOrEmpty(s)) return "";
            return s.Length <= max ? s : s.Substring(0, max) + "...";
        }

        /// <summary>
        /// Live-Logik-Sonde (Intent-Verifikation): bestaetigt NACH dem Fokussieren, ob der
        /// Tastaturfokus tatsaechlich auf einem brauchbaren Eingabefeld liegt (Edit/Document/
        /// Group, kein RootWebArea). Schreibt einen CHECKPOINT-Eintrag 'erwartet vs. tatsaechlich'
        /// in den Diagnose-Kanal — getrennt vom Fehler-Log, damit der Logik-Strang live verfolgbar
        /// bleibt. ok:false heisst: das gleich folgende Strg+V landet im Leeren. Best-effort, werf-frei.
        /// </summary>
        private static bool VerifyFocusCheckpoint(string step)
        {
            try
            {
                AutomationElement? focused = AutomationElement.FocusedElement;
                bool ok = focused != null && IsUsableFocusTarget(focused);
                DiagLog.Write("CHECKPOINT", step,
                    ("intent", "Tastaturfokus auf echtem Eingabefeld (Edit/Document/Group, kein RootWebArea)"),
                    ("expected", "usable input field"),
                    ("actual", Describe(focused)),
                    ("ok", ok));
                return ok;
            }
            catch (Exception ex)
            {
                DiagLog.Write("CHECKPOINT", step + " — Sonde warf", ("err", ex.GetType().Name));
                return false;
            }
        }

        /// <summary>
        /// Findet das echte Eingabefeld (tiptap/ProseMirror-contenteditable) im UIA-Teilbaum —
        /// ROBUST ueber alle Claude-Tabs: Chromium exponiert dasselbe Feld je nach ARIA-Rolle als
        /// ControlType Edit (Chat/Cowork, name='Schreiben Sie Ihre Anfrage an Claude') ODER Group
        /// (Code-Tab, name='Prompt'). Deshalb:
        ///   1. FindAll ueber focusable Edit/Document/Group (RootWebArea per Condition ausgeschlossen).
        ///   2. Bildschirmfuellende Container (Seiten-Wurzel / 'dframe-main') per IsPageRoot ueberspringen.
        ///   3. Bevorzugt das Element mit ClassName 'tiptap'/'ProseMirror' (stabiler Editor-Anker,
        ///      sprach- und positions-unabhaengig); sonst das erste brauchbare Nicht-Root-Feld.
        /// FindAll (statt FindFirst) ist noetig, weil das echte Feld unter mehreren focusable Containern
        /// stehen kann und FindFirst sonst den falschen zuerst liefert. Die Condition ist eng (focusable,
        /// kein Root) und der Pfad laeuft nur als Fallback (wenn Pfad1 versagt) — der U6-Performance-
        /// Hinweis wird damit bewusst und begrenzt in Kauf genommen (Korrektheit > wenige ms).
        /// </summary>
        private static AutomationElement? FindProseMirrorField(AutomationElement root, TreeScope scope)
        {
            try
            {
                var cond = new AndCondition(
                    new OrCondition(
                        new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Edit),
                        new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Document),
                        new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Group)),
                    new PropertyCondition(AutomationElement.IsKeyboardFocusableProperty, true),
                    new NotCondition(
                        new PropertyCondition(AutomationElement.AutomationIdProperty, "RootWebArea")));

                AutomationElementCollection matches = root.FindAll(scope, cond);
                AutomationElement? fallback = null;
                foreach (AutomationElement el in matches)
                {
                    if (IsPageRoot(el)) continue; // RootWebArea (Rect-Regel) / 'dframe-main' etc. ueberspringen
                    string cls = SafeClassName(el);
                    if (cls.IndexOf("ProseMirror", StringComparison.OrdinalIgnoreCase) >= 0 ||
                        cls.IndexOf("tiptap", StringComparison.OrdinalIgnoreCase) >= 0)
                        return el;        // das echte contenteditable-Feld (Editor-Anker)
                    fallback ??= el;      // erstes brauchbares Nicht-Root-Feld als Rueckfall merken
                }
                return fallback;
            }
            catch (Exception ex)
            {
                DiagLog.Write("UIA", "FindProseMirrorField-Ausnahme", ("err", ex.GetType().Name));
                return null;
            }
        }

        /// <summary>Werf-freier ClassName-Zugriff (UIA-Property-Reads koennen werfen).</summary>
        private static string SafeClassName(AutomationElement el)
        {
            try { return el.Current.ClassName ?? string.Empty; }
            catch { return string.Empty; }
        }

        private static bool TrySetFocus(AutomationElement el)
        {
            try { el.SetFocus(); return true; }
            catch (Exception ex)
            {
                Console.WriteLine($"[AppController] SetFocus warf: {ex.GetType().Name}");
                return false;
            }
        }

        /// <summary>
        /// Rekursive HWND-Suche nach "Chrome_RenderWidgetHostHWND". EnumChildWindows
        /// rekursiert selbst — wir filtern im Callback flach per GetClassName und
        /// verdrahten KEINE Zwischen-Klassennamen (Chrome_WidgetWin_*) fest.
        /// </summary>
        private static IntPtr FindRenderWidget(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return IntPtr.Zero;
            IntPtr found = IntPtr.Zero;
            try
            {
                Win32.EnumChildWindows(appHwnd, (hwnd, _) =>
                {
                    var sb = new StringBuilder(256);
                    Win32.GetClassName(hwnd, sb, sb.Capacity);
                    if (sb.ToString() == "Chrome_RenderWidgetHostHWND")
                    {
                        found = hwnd;
                        return false; // Enumeration stoppen
                    }
                    return true;
                }, IntPtr.Zero);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[AppController] FindRenderWidget warf: {ex.Message}");
            }
            return found;
        }

        // ── Tastatur-Injektion via SendInput (Hardware-Scancodes) ───────────

        private static Win32.INPUT KeyInput(ushort vk, bool keyUp)
        {
            ushort scan = (ushort)Win32.MapVirtualKey(vk, Win32.MAPVK_VK_TO_VSC);
            uint flags = Win32.KEYEVENTF_SCANCODE | (keyUp ? Win32.KEYEVENTF_KEYUP : 0);
            return new Win32.INPUT
            {
                type = Win32.INPUT_KEYBOARD,
                u = new Win32.INPUTUNION
                {
                    ki = new Win32.KEYBDINPUT
                    {
                        wVk = 0,            // 0 + Scancode-Flag = echter Hardware-Tastendruck
                        wScan = scan,
                        dwFlags = flags,
                        time = 0,
                        dwExtraInfo = IntPtr.Zero
                    }
                }
            };
        }

        private static bool SendInputs(params Win32.INPUT[] inputs)
        {
            uint sent = Win32.SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<Win32.INPUT>());
            if (sent != inputs.Length)
                Console.WriteLine($"[AppController] SendInput: nur {sent}/{inputs.Length} (UIPI-Block? err={Marshal.GetLastWin32Error()})");
            return sent == inputs.Length;
        }

        /// <summary>Echtes Strg+V als EIN 4-Element-Array (Ctrl↓ V↓ V↑ Ctrl↑).</summary>
        private static bool SendCtrlVScancode()
            => SendInputs(
                KeyInput(Win32.VK_CONTROL, false),
                KeyInput(Win32.VK_V, false),
                KeyInput(Win32.VK_V, true),
                KeyInput(Win32.VK_CONTROL, true));

        /// <summary>Modifier+Taste als 4-Element-Array (z.B. Strg+A, Strg+C).</summary>
        private static bool SendComboScancode(ushort modifier, ushort key)
            => SendInputs(
                KeyInput(modifier, false),
                KeyInput(key, false),
                KeyInput(key, true),
                KeyInput(modifier, true));

        /// <summary>Einzelne Taste (Down/Up), z.B. Enter, Backspace.</summary>
        private static bool SendKeyScancode(ushort vk)
            => SendInputs(KeyInput(vk, false), KeyInput(vk, true));

        /// <summary>
        /// Laesst gehaltene Nicht-Ctrl-Modifier (Win/Alt/Shift, beide Seiten) per
        /// KEYUP los, bevor Strg+V geht — sonst saehe Windows "Win+Alt+Ctrl+V".
        /// Nur tatsaechlich gedrueckte Tasten (GetAsyncKeyState) loslassen.
        /// </summary>
        private static void ReleaseHeldModifiers()
        {
            ushort[] mods = { 0x5B, 0x5C, 0xA4, 0xA5, 0xA0, 0xA1 }; // LWin RWin LAlt RAlt LShift RShift
            bool any = false;
            foreach (var vk in mods)
            {
                if ((Win32.GetAsyncKeyState(vk) & 0x8000) != 0)
                {
                    SendInputs(KeyInput(vk, true)); // nur KEYUP
                    any = true;
                }
            }
            if (any) Thread.Sleep(20);
        }

        // ── Zwischenablage (auf STA-Thread, mit Retry gegen CLIPBRD_E_CANT_OPEN) ──

        private static bool SetClipboardText(string text, out IDataObject? previous, out uint ownedSequence)
        {
            previous = null;
            ownedSequence = 0;
            for (int attempt = 1; attempt <= 6; attempt++)
            {
                try
                {
                    previous = Clipboard.GetDataObject();
                    // copy:true → OLE-Flush, Inhalt bleibt nach Tool-Ende erhalten
                    Clipboard.SetDataObject(text, true);
                    ownedSequence = Win32.GetClipboardSequenceNumber();
                    return true;
                }
                catch (Exception ex) when (attempt < 6)
                {
                    Thread.Sleep(40 * attempt); // 40,80,120,160,200 ms
                    Console.WriteLine($"[AppController] Clipboard busy ({attempt}/6): {ex.GetType().Name}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[AppController] Clipboard endgueltig fehlgeschlagen: {ex.Message}");
                    return false;
                }
            }
            return false;
        }

        private static void RestoreClipboard(IDataObject? previous, uint ownedSequence)
        {
            for (int attempt = 1; attempt <= 6; attempt++)
            {
                try
                {
                    if (Win32.GetClipboardSequenceNumber() != ownedSequence)
                    {
                        DiagLog.Write("Paste", "Clipboard-Restore uebersprungen: extern geaendert");
                        return;
                    }

                    if (previous == null)
                        Clipboard.Clear();
                    else
                        Clipboard.SetDataObject(previous, true);
                    return;
                }
                catch (Exception ex) when (attempt < 6)
                {
                    Thread.Sleep(40 * attempt);
                    Console.WriteLine($"[AppController] Clipboard-Restore busy ({attempt}/6): {ex.GetType().Name}");
                }
                catch (Exception ex)
                {
                    DiagLog.Write("Paste", "Clipboard-Restore fehlgeschlagen", ("err", ex.Message));
                    return;
                }
            }
        }

        // ── Helfer ──────────────────────────────────────────────────────────

        private static bool IsOwnedByProcess(IntPtr foregroundHwnd, IntPtr appHwnd)
        {
            if (foregroundHwnd == appHwnd) return true;
            Win32.GetWindowThreadProcessId(foregroundHwnd, out uint fgPid);
            Win32.GetWindowThreadProcessId(appHwnd, out uint appPid);
            return fgPid == appPid && fgPid != 0;
        }

        /// <summary>
        /// Fuehrt eine Aktion auf einem dedizierten STA-Thread aus und wartet auf
        /// ihr Ende. STA ist Pflicht fuer Clipboard und entkoppelt UIA vom WPF-UI-
        /// Thread (Deadlock-Schutz). Aufrufer (Task.Run oder UI) blockiert nur per
        /// Join — kein Message-Pump-Deadlock.
        /// </summary>
        private static T RunOnStaThread<T>(Func<T> action)
        {
            Exception? captured = null;
            T? result = default;
            var t = new Thread(() =>
            {
                try { result = action(); }
                catch (Exception ex) { captured = ex; }
            });
            t.IsBackground = true;
            t.SetApartmentState(ApartmentState.STA);
            t.Start();
            t.Join();
            if (captured != null) throw captured;
            return result!;
        }
    }
}
