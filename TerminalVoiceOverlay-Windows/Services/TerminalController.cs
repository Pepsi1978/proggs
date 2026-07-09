using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using TerminalVoiceOverlay.NativeMethods;

namespace TerminalVoiceOverlay.Services
{
    public static class TerminalController
    {
        /// <summary>
        /// Async-Wrapper fuer <see cref="PasteText"/>: schiebt die
        /// blockierenden Win32-Sleeps (BringToForeground 200 ms +
        /// autoEnter-Wartezeit 300 ms) auf einen Background-Thread, damit
        /// der UI-Thread waehrend einer Voice-Submit-Paste-Sequenz nicht
        /// fuer bis zu ~530 ms eingefroren ist. Die Clipboard-Zugriffe
        /// gehen weiterhin per Dispatcher.Invoke auf den UI-Thread —
        /// das sind aber nur Millisekunden. Aufrufer sollte awaiten,
        /// damit nachgelagerte Schritte (File.Delete des WAVs, State-
        /// Reset) garantiert nach dem Paste laufen.
        /// </summary>
        public static Task PasteTextAsync(string text, IntPtr terminalHwnd, bool autoEnter = false)
        {
            return Task.Run(() => PasteText(text, terminalHwnd, autoEnter));
        }

        /// <summary>
        /// Async-Wrapper fuer <see cref="ClearAllInput"/>: 5x Ctrl+U mit
        /// 50 ms Pausen sind ohne Wrapper 250 ms UI-Block. Auf Background-
        /// Thread bleibt die UI reaktiv waehrend der Eingabezeile frisch
        /// gemacht wird (z.B. Profil-Re-Correct).
        /// </summary>
        public static Task ClearAllInputAsync(IntPtr terminalHwnd)
        {
            return Task.Run(() => ClearAllInput(terminalHwnd));
        }

        /// <summary>
        /// Async-Wrapper fuer <see cref="ClearLine"/>: BringToForeground
        /// macht Thread.Sleep(200) — UI bleibt sonst pro W-Button-Klick
        /// 200 ms eingefroren.
        /// </summary>
        public static Task ClearLineAsync(IntPtr terminalHwnd)
        {
            return Task.Run(() => ClearLine(terminalHwnd));
        }

        /// <summary>
        /// Async-Wrapper fuer <see cref="CopySelection"/>: gleicher
        /// 200-ms-BringToForeground-Block wie alle Win32-Sequenzen, daher
        /// von der UI fernhalten.
        /// </summary>
        public static Task CopySelectionAsync(IntPtr terminalHwnd)
        {
            return Task.Run(() => CopySelection(terminalHwnd));
        }

        /// <summary>
        /// Async-Wrapper fuer <see cref="PasteClipboard"/>: gleiches
        /// Argument wie bei CopySelectionAsync.
        /// </summary>
        public static Task PasteClipboardAsync(IntPtr terminalHwnd)
        {
            return Task.Run(() => PasteClipboard(terminalHwnd));
        }

        /// <summary>
        /// Async-Wrapper fuer <see cref="PressReturn"/>: BringToForeground +
        /// keybd_event sind synchron und kosten ~200 ms UI-Block.
        /// </summary>
        public static Task PressReturnAsync(IntPtr terminalHwnd)
        {
            return Task.Run(() => PressReturn(terminalHwnd));
        }

        /// <summary>
        /// Generations-Zaehler fuer Clipboard-Restore. Jeder Paste-Zyklus
        /// erhoeht den Zaehler atomar; der spaeter geplante Restore-Task
        /// merkt sich seine Generation und prueft beim Ausfuehren ob die
        /// noch aktuell ist. Ist sie es nicht (= ein zweiter Paste hat
        /// das Clipboard inzwischen ueberschrieben), fasst der Restore
        /// das Clipboard NICHT mehr an — sonst wuerde er den frischen
        /// Text mit dem alten Inhalt ueberschreiben und die zweite
        /// Aufnahme ginge verloren. Direktive-3-Resilienz: thread-safe
        /// per Interlocked, kein Lock noetig, kein Risiko fuer Deadlock.
        /// </summary>
        private static long _clipboardGen;

        /// <summary>
        /// Versucht eine Clipboard-Operation auf dem UI-Thread bis zu
        /// <paramref name="maxAttempts"/> mal mit kurzem Backoff. WPFs
        /// Clipboard-API wirft <c>System.Runtime.InteropServices.COMException</c>
        /// (HRESULT 0x800401D0 — CLIPBRD_E_CANT_OPEN) wenn ein anderer
        /// Process gerade das Clipboard offen hat (z.B. Excel beim
        /// Kopieren, Browser beim DragDrop, Antivirus-Scanner). Frueher
        /// fiel die App in dem Fall mit unhandled Exception aus dem
        /// async-void-Handler — der Watchdog restartete still. Jetzt
        /// schlucken wir die Exception nach max. 5 Versuchen und schreiben
        /// einen Fehler ins Console-Log; der Voice-Submit-Flow merkt zwar
        /// dass nichts gepastet wurde (durch das null-Result), aber die
        /// App stuerzt nicht ab.
        /// </summary>
        private static bool TryRunOnUiThread(Action action, int maxAttempts = 5)
        {
            var app = Application.Current;
            if (app is null) return false; // Application is shutting down

            for (int attempt = 1; attempt <= maxAttempts; attempt++)
            {
                try
                {
                    app.Dispatcher.Invoke(action);
                    return true;
                }
                catch (System.Runtime.InteropServices.COMException ex)
                    when (attempt < maxAttempts)
                {
                    // Backoff: 30, 60, 120, 240 ms — typische Clipboard-
                    // Locks (Excel-Copy, Browser-DragDrop) sind binnen
                    // 100 ms wieder offen.
                    Thread.Sleep(30 * (1 << (attempt - 1)));
                    Console.WriteLine($"Clipboard busy (attempt {attempt}/{maxAttempts}): {ex.Message}");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Clipboard operation failed: {ex.GetType().Name}: {ex.Message}");
                    return false;
                }
            }
            return false;
        }

        /// <summary>
        /// Pastes text into the terminal via Clipboard + Ctrl+V.
        /// Ensures the terminal window is focused first.
        /// </summary>
        public static void PasteText(string text, IntPtr terminalHwnd, bool autoEnter = false)
        {
            var totalSw = Stopwatch.StartNew();
            DiagLog.Write("Paste", "start", ("chars", text.Length), ("autoEnter", autoEnter), ("hwnd", $"0x{terminalHwnd.ToInt64():X}"));
            // Generation-ID fuer diesen Paste-Zyklus. Wenn ein folgender
            // Paste den Zaehler erhoeht, weiss der Restore-Task dass er
            // nichts mehr machen darf.
            long myGen = System.Threading.Interlocked.Increment(ref _clipboardGen);

            // Save previous clipboard content. Mit Retry-Wrapper: ein
            // gerade laufender Excel-Copy o.ae. wuerde sonst die App
            // ueber unhandled COMException zum Watchdog-Restart zwingen.
            string? previousClipboard = null;
            var clipboardSw = Stopwatch.StartNew();
            bool clipboardSet = TryRunOnUiThread(() =>
            {
                if (Clipboard.ContainsText())
                    previousClipboard = Clipboard.GetText();
                Clipboard.SetText(text);
            });
            DiagLog.Perf("Paste", "clipboard_set", clipboardSw, ("ok", clipboardSet), ("chars", text.Length));
            if (!clipboardSet)
            {
                Console.WriteLine("PasteText: Clipboard.SetText fehlgeschlagen — Paste uebersprungen.");
                DiagLog.Warn("Paste", "clipboard_set_failed", ("chars", text.Length));
                return;
            }

            // ── Modifier-Release VOR dem Fenster-Wechsel (Fix 2026-06-21) ──
            // KRITISCH: Erst die Trigger-Modifier (Alt/Win/Shift) freigeben,
            // DANN das Fenster nach vorn holen. Noch gedrueckte Modifier
            // (Strg+Alt+P per Stream Deck oder Finger gehalten) blockieren sonst
            // den Foreground-Wechsel aus FREMDEN Fenstern (Browser etc.) -> das
            // Terminal blinkt nur in der Taskleiste, der Paste verpufft. Die
            // G4-Makrotaste lief bisher NUR, weil ihr Makro die Modifier per
            // 50ms-Timing schon vorher sauber loslaesst — dieser Fix macht das
            // unabhaengig vom Ausloeser. (Bug-Almanach dotnet-csharp 5.5.)
            ReleaseNonCtrlModifiers();

            // Bring terminal to foreground (robust: AttachThreadInput + AllowSetForegroundWindow)
            var foregroundSw = Stopwatch.StartNew();
            bool foregroundOk = BringToForeground(terminalHwnd);
            DiagLog.Perf("Paste", "foreground", foregroundSw, ("ok", foregroundOk), ("hwnd", $"0x{terminalHwnd.ToInt64():X}"));
            if (!foregroundOk)
            {
                DiagLog.Warn("Paste", "foreground_failed_skip_ctrl_v", ("hwnd", $"0x{terminalHwnd.ToInt64():X}"));
                return;
            }

            // ── Sicherheitsnetz: Modifier nochmal freigeben vor Ctrl+V ──
            // Wenn der Paste durch einen Hotkey ausgeloest wurde (z.B.
            // Win+Alt+B oder Shift+Alt+M), sind die Trigger-Modifier-Tasten
            // beim SendCtrlV-Aufruf moeglicherweise IMMER NOCH virtuell
            // gedrueckt (Frank haelt sie physisch). Windows wuerde dann
            // "Win+Alt+Ctrl+V" sehen statt einfachem "Ctrl+V" — und das ist
            // kein Paste mehr. Wir schicken synthetische KEYUP-Events fuer
            // alle nicht-Ctrl-Modifier, damit der virtuelle Tastatur-State
            // sauber ist, BEVOR Ctrl+V geht. Bei Strg+1..9-Hotkeys ist Ctrl
            // der gewollte Modifier und bleibt unten — daher kein Release
            // fuer Ctrl hier. Hardware-KEYUP-Events kommen spaeter (wenn
            // Frank physisch loslaesst) und sind dann ein NoOp.
            ReleaseNonCtrlModifiers();

            // Send Ctrl+V
            var sendSw = Stopwatch.StartNew();
            SendCtrlV();
            DiagLog.Perf("Paste", "ctrl_v", sendSw);

            // Send Enter if auto-enter is enabled
            if (autoEnter)
            {
                Thread.Sleep(300); // macOS uses 300ms before optional Enter
                BringToForeground(terminalHwnd);
                SendKey(VK_RETURN);
                DiagLog.Write("Paste", "auto_enter_sent");
            }

            // Restore previous clipboard after paste completes — aber NUR
            // wenn unsere Generation noch aktuell ist. Sonst hat ein
            // anderer Paste das Clipboard schon mit frischen Inhalten
            // ueberschrieben und wir wuerden den ueberholen.
            if (previousClipboard != null)
            {
                var prev = previousClipboard;
                Task.Delay(500).ContinueWith(_ =>
                {
                    if (System.Threading.Interlocked.Read(ref _clipboardGen) != myGen)
                    {
                        Console.WriteLine("Clipboard restore skipped — newer paste in flight.");
                        DiagLog.Write("Paste", "clipboard_restore_skipped_newer_generation");
                        return;
                    }
                    var restoreSw = Stopwatch.StartNew();
                    bool restored = TryRunOnUiThread(() => Clipboard.SetText(prev));
                    DiagLog.Perf("Paste", "clipboard_restore", restoreSw, ("ok", restored));
                });
            }
            DiagLog.Perf("Paste", "total", totalSw, ("chars", text.Length), ("autoEnter", autoEnter));
        }

        /// <summary>
        /// Clears the current terminal input line via Ctrl+U — the universal
        /// "kill line" readline shortcut. Works identically across:
        ///   • Claude Code CLI (Ink/React TUI — official shortcut for clearing input)
        ///   • PowerShell (PSReadLine default binding)
        ///   • bash, zsh, sh (readline default)
        ///   • Node.js / Python interactive REPLs
        ///   • Codex CLI
        /// Critical property: Ctrl+U clears ONLY the input buffer. It never sends
        /// SIGINT. If a task is running and the input is empty, Ctrl+U is a
        /// harmless no-op — the running task stays untouched.
        /// Earlier attempts (Ctrl+C, Home+Shift+End+Delete) both failed:
        ///   • Ctrl+C = SIGINT → interrupts running task
        ///   • Shift+End does not select text in a terminal (shift-selection is
        ///     a GUI-only concept; terminals have no cursor-based selection model)
        /// </summary>
        public static void ClearLine(IntPtr terminalHwnd)
        {
            BringToForeground(terminalHwnd);
            SendKeyCombo(Win32.VK_CONTROL, VK_U);
        }

        /// <summary>
        /// Loescht den GESAMTEN Eingabe-Buffer — auch bei mehrzeiliger Eingabe
        /// (z.B. Claude Code CLI mit Shift+Enter-Zeilen). Drueckt Ctrl+U mehrmals
        /// hintereinander mit kleinen Pausen, bis garantiert nichts mehr in der
        /// Eingabezeile steht. Ctrl+U ist ein harmloser No-Op wenn der Buffer
        /// leer ist, also kein Risiko bei zu vielen Wiederholungen.
        ///
        /// Wird beim Profil-Wechsel im Voice-Overlay verwendet, damit der zuletzt
        /// eingefuegte Prompt restlos verschwindet, bevor die neue Korrektur
        /// reingepastet wird.
        /// </summary>
        public static void ClearAllInput(IntPtr terminalHwnd)
        {
            BringToForeground(terminalHwnd);
            for (int i = 0; i < 5; i++)
            {
                SendKeyCombo(Win32.VK_CONTROL, VK_U);
                Thread.Sleep(50);
            }
        }

        /// <summary>
        /// Copies the currently selected text in the terminal via Ctrl+C.
        /// Windows Terminal detects selection and copies instead of sending SIGINT.
        /// </summary>
        public static void CopySelection(IntPtr terminalHwnd)
        {
            BringToForeground(terminalHwnd);
            SendKeyCombo(Win32.VK_CONTROL, VK_C);
        }

        /// <summary>
        /// Pastes the current clipboard content into the terminal via Ctrl+V.
        /// Does NOT modify the clipboard — pastes whatever is already there.
        /// </summary>
        public static void PasteClipboard(IntPtr terminalHwnd)
        {
            BringToForeground(terminalHwnd);
            SendCtrlV();
        }

        private const ushort VK_C = 0x43;
        private const ushort VK_U = 0x55;

        private const byte VK_HOME = 0x24;
        private const byte VK_END = 0x23;
        private const byte VK_DELETE = 0x2E;
        private const byte VK_BACKSPACE = 0x08;
        private const byte VK_RETURN = 0x0D;

        /// <summary>
        /// Sends the Enter/Return key. Used for the Enter button's immediate-fire behavior
        /// when toggling autoEnter on.
        /// </summary>
        public static void SendReturn()
        {
            SendKey(VK_RETURN);
        }

        /// <summary>
        /// Focuses the terminal window then sends the Enter/Return key.
        /// Called by EnterButton when toggling auto-enter ON to fire a Return immediately.
        /// </summary>
        public static void PressReturn(IntPtr terminalHwnd)
        {
            BringToForeground(terminalHwnd);
            SendKey(VK_RETURN);
        }

        /// <summary>
        /// Robustly brings a window to the foreground, overcoming the Windows foreground-lock
        /// restriction by attaching to the target window's input queue. Ported from the sister
        /// project ClaudeVoiceOverlay-Windows/Services/AppController.cs — proven in production.
        /// </summary>
        private static bool BringToForeground(IntPtr terminalHwnd)
        {
            if (terminalHwnd == IntPtr.Zero)
            {
                DiagLog.Warn("Paste", "foreground_no_hwnd");
                return false;
            }

            if (!Win32.IsWindow(terminalHwnd))
            {
                DiagLog.Warn("Paste", "foreground_stale_hwnd", ("hwnd", $"0x{terminalHwnd.ToInt64():X}"));
                return false;
            }

            var activationHwnd = ResolveActivationWindow(terminalHwnd);
            if (activationHwnd == IntPtr.Zero || !Win32.IsWindow(activationHwnd))
            {
                DiagLog.Warn("Paste", "foreground_invalid_root", ("hwnd", $"0x{terminalHwnd.ToInt64():X}"), ("root", $"0x{activationHwnd.ToInt64():X}"));
                return false;
            }

            var currentFg = Win32.GetForegroundWindow();
            if (IsSameOrDescendant(currentFg, activationHwnd))
            {
                Thread.Sleep(30);
                return true;
            }

            // AttachThreadInput-Trick (Fix 2026-06-21): An den AKTUELLEN VORDERGRUND-
            // Thread (currentFg — z.B. der Browser, in dem der Hotkey gedrueckt wurde)
            // heften, NICHT an den Ziel-Thread. Nur das Vordergrund-Fenster besitzt das
            // Foreground-Recht, das wir uns kurz leihen muessen, damit
            // SetForegroundWindow(terminalHwnd) das Terminal aus einem FREMDEN Fenster
            // heraus tatsaechlich nach vorn holt. Vorher wurde faelschlich an den
            // Terminal-Thread geheftet -> aus Browser/anderen Apps scheiterte der
            // Wechsel (Foreground-Lock) -> Terminal blinkte nur, Paste verpuffte
            // (Bug-Almanach dotnet-csharp 5.5). Wichtig: Windows Terminal kann
            // ein inneres TermControl-HWND als Ziel liefern. Aktiviert werden
            // muss aber das Root-Top-Level-HWND; SetForegroundWindow auf einem
            // Child-HWND kann fehlschlagen und danach trifft Ctrl+V kein Feld.
            uint ourThread = Win32.GetCurrentThreadId();
            uint fgThread  = Win32.GetWindowThreadProcessId(currentFg, out _);
            uint targetThread = Win32.GetWindowThreadProcessId(activationHwnd, out _);

            bool attachedFg = false;
            bool attachedTarget = false;
            try
            {
                if (ourThread != fgThread && fgThread != 0)
                    attachedFg = Win32.AttachThreadInput(ourThread, fgThread, true);
                if (ourThread != targetThread && targetThread != 0 && targetThread != fgThread)
                    attachedTarget = Win32.AttachThreadInput(ourThread, targetThread, true);

                if (Win32.IsIconic(activationHwnd))
                    Win32.ShowWindow(activationHwnd, Win32.SW_RESTORE);

                Win32.AllowSetForegroundWindow(unchecked((uint)-1));
                bool setOk = Win32.SetForegroundWindow(activationHwnd);
                Win32.BringWindowToTop(activationHwnd);
                Thread.Sleep(120);

                var after = Win32.GetForegroundWindow();
                bool ok = IsSameOrDescendant(after, activationHwnd);
                DiagLog.Write("Paste", "foreground_verify",
                    ("ok", ok),
                    ("setOk", setOk),
                    ("hwnd", $"0x{terminalHwnd.ToInt64():X}"),
                    ("root", $"0x{activationHwnd.ToInt64():X}"),
                    ("before", $"0x{currentFg.ToInt64():X}"),
                    ("after", $"0x{after.ToInt64():X}"));
                return ok;
            }
            finally
            {
                if (attachedTarget)
                    Win32.AttachThreadInput(ourThread, targetThread, false);
                if (attachedFg)
                    Win32.AttachThreadInput(ourThread, fgThread, false);
            }
        }

        private static IntPtr ResolveActivationWindow(IntPtr hwnd)
        {
            var root = Win32.GetAncestor(hwnd, Win32.GA_ROOT);
            return root == IntPtr.Zero ? hwnd : root;
        }

        private static bool IsSameOrDescendant(IntPtr hwnd, IntPtr ancestor)
        {
            if (hwnd == IntPtr.Zero || ancestor == IntPtr.Zero) return false;
            if (hwnd == ancestor) return true;
            var cur = hwnd;
            for (int i = 0; i < 12 && cur != IntPtr.Zero; i++)
            {
                cur = Win32.GetParent(cur);
                if (cur == ancestor) return true;
            }
            return false;
        }

        private static void SendKeyCombo(ushort modifier, ushort key)
        {
            byte modScan = (byte)Win32.MapVirtualKey(modifier, Win32.MAPVK_VK_TO_VSC);
            byte keyScan = (byte)Win32.MapVirtualKey(key, Win32.MAPVK_VK_TO_VSC);

            Win32.keybd_event((byte)modifier, modScan, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)key, keyScan, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)key, keyScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Win32.keybd_event((byte)modifier, modScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }

        private static void SendCtrlV()
        {
            // Use keybd_event with proper scan codes
            byte ctrlScan = (byte)Win32.MapVirtualKey(Win32.VK_CONTROL, Win32.MAPVK_VK_TO_VSC);
            byte vScan    = (byte)Win32.MapVirtualKey(Win32.VK_V, Win32.MAPVK_VK_TO_VSC);

            Win32.keybd_event((byte)Win32.VK_CONTROL, ctrlScan, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, vScan, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, vScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_CONTROL, ctrlScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }

        /// <summary>
        /// Sendet synthetische KEYUP-Events fuer Win, Alt und Shift damit
        /// der virtuelle Tastatur-State sauber ist bevor <see cref="SendCtrlV"/>
        /// laeuft. Wird vor jedem Paste aufgerufen, denn Hotkey-getriggerte
        /// Pastes laufen waehrend Frank die Trigger-Modifier physisch noch
        /// haelt — Windows wuerde sonst "Win+Alt+Ctrl+V" sehen statt nur
        /// "Ctrl+V". Ctrl wird hier NICHT released, weil die Strg+1..9-
        /// Hotkeys den Ctrl-Modifier intentional gedrueckt halten und
        /// SendCtrlV gleich darauf Ctrl down nochmal sendet (idempotent
        /// im keybd_event-Modell).
        /// </summary>
        private static void ReleaseNonCtrlModifiers()
        {
            void Up(ushort vk)
            {
                byte scan = (byte)Win32.MapVirtualKey(vk, Win32.MAPVK_VK_TO_VSC);
                Win32.keybd_event((byte)vk, scan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            }

            // Beide Win-Tasten, beide Alt-Tasten, beide Shift-Tasten.
            Up(0x5B); // VK_LWIN
            Up(0x5C); // VK_RWIN
            Up(0xA4); // VK_LMENU (linke Alt)
            Up(0xA5); // VK_RMENU (rechte Alt / AltGr)
            Up(0xA0); // VK_LSHIFT
            Up(0xA1); // VK_RSHIFT
            // Kleine Pause damit Windows die Up-Events verarbeiten kann.
            System.Threading.Thread.Sleep(20);
        }

        private static void SendKey(ushort vk)
        {
            byte scan = (byte)Win32.MapVirtualKey(vk, Win32.MAPVK_VK_TO_VSC);
            // Home, End, Delete, Insert, Page Up/Down, Arrow keys (0x21–0x2E) are extended keys
            uint flags = (vk >= 0x21 && vk <= 0x2E) ? Win32.KEYEVENTF_EXTENDEDKEY : 0;

            Win32.keybd_event((byte)vk, scan, flags, UIntPtr.Zero);
            Win32.keybd_event((byte)vk, scan, flags | Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }
    }
}
