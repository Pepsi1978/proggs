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

            // 1. Zwischenablage setzen (STA-Thread → direkter Clipboard-Zugriff) + verifizieren
            string? previous = null;
            if (!SetClipboardText(text, out previous))
            {
                Console.WriteLine("[AppController] PasteText: Clipboard.SetText fehlgeschlagen — abgebrochen.");
                return;
            }

            // 2. Ziel aktivieren + Eingabefeld per UIA finden/fokussieren (positions-unabhaengig)
            FocusTarget(appHwnd);

            // 3. Gehaltene Hotkey-Modifier neutralisieren, sonst kommt "Win+Alt+Ctrl+V" an
            ReleaseHeldModifiers();

            // 4. Echtes Strg+V mit Hardware-Scancodes
            bool pasted = SendCtrlVScancode();
            Console.WriteLine($"[AppController] paste sent={pasted} autoEnter={autoEnter} hwnd=0x{appHwnd.ToInt64():X}");

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
                // 1. Das bereits fokussierte Element (folgt dem Fokus automatisch)
                AutomationElement? focused = null;
                try { focused = AutomationElement.FocusedElement; } catch { /* s.u. */ }
                if (focused != null && IsTextField(focused) && TrySetFocus(focused))
                {
                    Console.WriteLine("[AppController] UIA: FocusedElement ist Textfeld → fokussiert");
                    return true;
                }

                var cond = new AndCondition(
                    new OrCondition(
                        new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Edit),
                        new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Document)),
                    new PropertyCondition(AutomationElement.IsKeyboardFocusableProperty, true));

                // 2. Im Baum des Top-Level-Fensters suchen (Fokus liegt auf Container)
                var root = AutomationElement.FromHandle(appHwnd);
                if (root != null)
                {
                    var field = root.FindFirst(TreeScope.Descendants, cond);
                    if (field != null && TrySetFocus(field))
                    {
                        Console.WriteLine("[AppController] UIA: Textfeld im Fensterbaum gefunden → fokussiert");
                        return true;
                    }
                }

                // 3. HWND-Notnagel: Render-Widget suchen, dann erneut UIA darauf
                IntPtr renderWidget = FindRenderWidget(appHwnd);
                if (renderWidget != IntPtr.Zero)
                {
                    var rwRoot = AutomationElement.FromHandle(renderWidget);
                    var field2 = rwRoot?.FindFirst(TreeScope.Subtree, cond);
                    if (field2 != null && TrySetFocus(field2))
                    {
                        Console.WriteLine("[AppController] UIA: Textfeld unter Render-Widget gefunden → fokussiert");
                        return true;
                    }
                }

                Console.WriteLine("[AppController] UIA: kein Textfeld gefunden — Paste geht an aktuelles Fokus-Element");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[AppController] UIA-Fokus fehlgeschlagen: {ex.GetType().Name}: {ex.Message}");
            }
            return false;
        }

        private static bool IsTextField(AutomationElement el)
        {
            try
            {
                var ct = el.Current.ControlType;
                return (ct == ControlType.Edit || ct == ControlType.Document)
                    && el.Current.IsKeyboardFocusable;
            }
            catch { return false; }
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
