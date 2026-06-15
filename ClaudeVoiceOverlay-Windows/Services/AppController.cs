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

        // ── Oeffentliche API: Async-Wrapper (haelt Win32-Sleeps vom UI-Thread) ──

        public static Task PasteTextAsync(string text, IntPtr appHwnd, bool autoEnter = false)
            => Task.Run(() => PasteText(text, appHwnd, autoEnter));

        public static Task ClearLineAsync(IntPtr appHwnd)     => Task.Run(() => ClearLine(appHwnd));
        public static Task ClearAllInputAsync(IntPtr appHwnd) => Task.Run(() => ClearAllInput(appHwnd));
        public static Task CopySelectionAsync(IntPtr appHwnd) => Task.Run(() => CopySelection(appHwnd));
        public static Task PasteClipboardAsync(IntPtr appHwnd)=> Task.Run(() => PasteClipboard(appHwnd));
        public static Task PressReturnAsync(IntPtr appHwnd)   => Task.Run(() => PressReturn(appHwnd));

        // ── Oeffentliche API: synchrone Einstiegspunkte (laufen auf STA-Worker) ──

        public static void PasteText(string text, IntPtr appHwnd, bool autoEnter = false)
            => RunOnStaThread(() => PasteTextCore(text, appHwnd, autoEnter));

        // Bei Electron-Feldern leeren ClearLine und ClearAllInput identisch das
        // gesamte contenteditable (Strg+A + Backspace) — es gibt keine Terminal-
        // "Zeile". API getrennt fuer OverlayWindow-Kompatibilitaet.
        public static void ClearLine(IntPtr appHwnd)     => RunOnStaThread(() => ClearInputCore(appHwnd));
        public static void ClearAllInput(IntPtr appHwnd) => RunOnStaThread(() => ClearInputCore(appHwnd));

        public static void CopySelection(IntPtr appHwnd)
            => RunOnStaThread(() => { ActivateTarget(appHwnd); Thread.Sleep(40); SendComboScancode(Win32.VK_CONTROL, VK_C); });

        public static void PasteClipboard(IntPtr appHwnd)
            => RunOnStaThread(() => { FocusTarget(appHwnd); ReleaseHeldModifiers(); SendCtrlVScancode(); });

        public static void PressReturn(IntPtr appHwnd)
            => RunOnStaThread(() => { FocusTarget(appHwnd); Thread.Sleep(40); SendKeyScancode(VK_RETURN); });

        public static void SendReturn() => RunOnStaThread(() => SendKeyScancode(VK_RETURN));

        // ── Kern-Abläufe ────────────────────────────────────────────────────

        private static long _clipboardGen;

        private static void PasteTextCore(string text, IntPtr appHwnd, bool autoEnter)
        {
            long myGen = Interlocked.Increment(ref _clipboardGen);
            DiagLog.Write("Paste", "PasteText START",
                ("hwnd", "0x" + appHwnd.ToInt64().ToString("X")),
                ("len", text.Length), ("autoEnter", autoEnter));

            // 1. Zwischenablage setzen (STA-Thread → direkter Clipboard-Zugriff) + verifizieren
            string? previous = null;
            if (!SetClipboardText(text, out previous))
            {
                DiagLog.Write("Paste", "ABBRUCH: Clipboard.SetText fehlgeschlagen");
                return;
            }

            // 2. Ziel aktivieren + Eingabefeld per UIA finden/fokussieren (positions-unabhaengig)
            FocusTarget(appHwnd);

            // 2b. LIVE-LOGIK-SONDE (Intent-Verifikation, observability-live-logic-probes):
            //     Sitzt der Tastaturfokus JETZT wirklich auf einem echten Eingabefeld (erwartet)
            //     oder noch auf Button/Seiten-Root (tatsaechlich)? CHECKPOINT 'erwartet vs. ist'
            //     -> ok:false bedeutet, Strg+V wird gleich verpuffen. Live mitlesbar:
            //     Get-Content "$env:LOCALAPPDATA\ClaudeVoiceOverlay\diag.log" -Wait -Tail 30
            VerifyFocusCheckpoint("Fokus nach FocusTarget (vor Strg+V)");

            // 3. Gehaltene Hotkey-Modifier neutralisieren, sonst kommt "Win+Alt+Ctrl+V" an
            ReleaseHeldModifiers();

            // 4. Echtes Strg+V mit Hardware-Scancodes
            bool pasted = SendCtrlVScancode();
            DiagLog.Write("Paste", "Strg+V gesendet", ("sent", pasted), ("autoEnter", autoEnter));

            // 5. Optionales Enter (separat, nach kurzer Verzoegerung)
            if (autoEnter)
            {
                Thread.Sleep(300);
                SendKeyScancode(VK_RETURN);
            }

            // 6. Vorherigen Clipboard-Inhalt verzoegert wiederherstellen (Generationsschutz)
            if (previous != null)
            {
                var prev = previous;
                Task.Delay(600).ContinueWith(_ =>
                {
                    if (Interlocked.Read(ref _clipboardGen) != myGen) return; // neuerer Paste aktiv
                    RunOnStaThread(() => { try { Clipboard.SetText(prev); } catch { /* tolerant */ } });
                });
            }
        }

        private static void ClearInputCore(IntPtr appHwnd)
        {
            FocusTarget(appHwnd);
            ReleaseHeldModifiers();
            SendComboScancode(Win32.VK_CONTROL, VK_A); // alles markieren
            Thread.Sleep(40);
            SendKeyScancode(VK_BACKSPACE);             // loeschen
        }

        // ── Ziel aktivieren + Eingabefeld fokussieren ───────────────────────

        /// <summary>Aktiviert die Ziel-App und fokussiert ihr aktives Eingabefeld per UIA.</summary>
        private static void FocusTarget(IntPtr appHwnd)
        {
            ActivateTarget(appHwnd);
            Thread.Sleep(50);              // SetForegroundWindow ist asynchron
            FocusInputFieldUia(appHwnd);   // positions-unabhaengig (UIA), best effort
        }

        /// <summary>
        /// Bringt die Ziel-App legal in den Vordergrund (AllowSetForegroundWindow +
        /// AttachThreadInput-Leihe). Stellt minimierte Fenster wieder her.
        /// </summary>
        private static void ActivateTarget(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;

            if (Win32.IsIconic(appHwnd))
                Win32.ShowWindow(appHwnd, Win32.SW_RESTORE);

            var currentFg = Win32.GetForegroundWindow();
            if (IsOwnedByProcess(currentFg, appHwnd))
                return; // schon vorne

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

                // Bug U9-Variante (Cowork nach beendeter Aufgabe, real getroffen 2026-06-15,
                // per diag.log belegt): Der Fokus liegt direkt nach einer Cowork-Antwort auf dem
                // 'Fortschritt N von N'-Button -> Pfad1 verworfen (korrekt). Frueher traf
                // FindFirst(Edit|Document) als ERSTES die Seiten-Wurzel 'RootWebArea' (Document,
                // bildschirmfuellend), die der Root-Guard verwarf -> FindFirst gab danach AUF, das
                // echte ProseMirror-Edit dahinter wurde nie gefunden -> Strg+V verpuffte (Text kam
                // erst nach manuellem Maus-Klick ins Feld). Fix: RootWebArea direkt aus der Suche
                // ausschliessen, dann liefert FindFirst sofort das echte Feld. FindFirst + enger
                // Scope bleibt erhalten (U6: kein teures FindAll/Descendants ueber den ganzen Baum).
                var cond = new AndCondition(
                    new OrCondition(
                        new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Edit),
                        new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Document)),
                    new PropertyCondition(AutomationElement.IsKeyboardFocusableProperty, true),
                    new NotCondition(
                        new PropertyCondition(AutomationElement.AutomationIdProperty, "RootWebArea")));

                // 2. Im Baum des Top-Level-Fensters suchen (Fokus liegt auf Container)
                var root = AutomationElement.FromHandle(appHwnd);
                if (root != null)
                {
                    var field = root.FindFirst(TreeScope.Descendants, cond);
                    DiagLog.Write("UIA", "Pfad2 FindFirst(Descendants)", ("found", Describe(field)));
                    if (field != null && IsPageRoot(field))
                        DiagLog.Write("UIA", "Pfad2 verworfen: Treffer ist Seiten-Wurzel (RootWebArea), kein Feld");
                    else if (field != null && TrySetFocus(field))
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
                    var field2 = rwRoot?.FindFirst(TreeScope.Subtree, cond);
                    DiagLog.Write("UIA", "Pfad3 FindFirst(Subtree)", ("found", Describe(field2)));
                    if (field2 != null && IsPageRoot(field2))
                        DiagLog.Write("UIA", "Pfad3 verworfen: Treffer ist Seiten-Wurzel (RootWebArea), kein Feld");
                    else if (field2 != null && TrySetFocus(field2))
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
        private static void VerifyFocusCheckpoint(string step)
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
            }
            catch (Exception ex)
            {
                DiagLog.Write("CHECKPOINT", step + " — Sonde warf", ("err", ex.GetType().Name));
            }
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

        private static bool SetClipboardText(string text, out string? previous)
        {
            previous = null;
            for (int attempt = 1; attempt <= 6; attempt++)
            {
                try
                {
                    if (Clipboard.ContainsText())
                        previous = Clipboard.GetText();
                    // copy:true → OLE-Flush, Inhalt bleibt nach Tool-Ende erhalten
                    Clipboard.SetDataObject(text, true);
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
        private static void RunOnStaThread(Action action)
        {
            Exception? captured = null;
            var t = new Thread(() =>
            {
                try { action(); }
                catch (Exception ex) { captured = ex; }
            });
            t.IsBackground = true;
            t.SetApartmentState(ApartmentState.STA);
            t.Start();
            t.Join();
            if (captured != null)
                Console.WriteLine($"[AppController] STA-Thread-Fehler: {captured.GetType().Name}: {captured.Message}");
        }
    }
}
