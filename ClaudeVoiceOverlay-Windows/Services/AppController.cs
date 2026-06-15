using System;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Steuert die Ziel-App (Claude Desktop / Codex) per Win32-Tastatur- und
    /// Maus-Simulation. Schwester-Klasse zu TerminalVoiceOverlay/TerminalController:
    /// die oeffentliche API (PasteText[Async], ClearLine[Async], ClearAllInput[Async],
    /// CopySelection[Async], PasteClipboard[Async], PressReturn[Async], SendReturn)
    /// ist deckungsgleich, damit das gemeinsame OverlayWindow 1:1 funktioniert.
    ///
    /// Unterschiede zum TerminalController (App-spezifisch):
    ///   • Eingabefeld-Fokus: Electron braucht FindWindowEx(Chrome_RenderWidgetHostHWND)
    ///     + Klick ins Eingabefeld; Codex hat eine andere Fensterstruktur (nur Escape).
    ///   • Loeschen: Ctrl+A + Backspace (Electron-Textfeld) statt Ctrl+U (Terminal-Readline).
    ///
    /// Uebernommen vom TerminalController (Robustheit, plattformneutral):
    ///   • Async-Wrapper (Win32-Sleeps weg vom UI-Thread)
    ///   • Clipboard-Generationsschutz (kein Restore-Race bei schnellen Pastes)
    ///   • TryRunOnUiThread mit Retry (Clipboard-COMException-Wand)
    ///   • ReleaseNonCtrlModifiers vor Ctrl+V (Hotkey-getriggerte Pastes)
    /// </summary>
    public static class AppController
    {
        private const byte VK_A         = 0x41;
        private const byte VK_C         = 0x43;
        private const byte VK_BACKSPACE = 0x08;
        private const byte VK_RETURN    = 0x0D;
        private const ushort VK_HOME    = 0x24;
        private const ushort VK_END     = 0x23;

        // ── Async-Wrapper (halten die blockierenden Win32-Sleeps vom UI-Thread) ──

        public static Task PasteTextAsync(string text, IntPtr appHwnd, bool autoEnter = false)
            => Task.Run(() => PasteText(text, appHwnd, autoEnter));

        public static Task ClearLineAsync(IntPtr appHwnd)
            => Task.Run(() => ClearLine(appHwnd));

        public static Task ClearAllInputAsync(IntPtr appHwnd)
            => Task.Run(() => ClearAllInput(appHwnd));

        public static Task CopySelectionAsync(IntPtr appHwnd)
            => Task.Run(() => CopySelection(appHwnd));

        public static Task PasteClipboardAsync(IntPtr appHwnd)
            => Task.Run(() => PasteClipboard(appHwnd));

        public static Task PressReturnAsync(IntPtr appHwnd)
            => Task.Run(() => PressReturn(appHwnd));

        // ── Clipboard-Generationsschutz ──
        // Jeder Paste-Zyklus erhoeht den Zaehler atomar; der geplante Restore-Task
        // merkt sich seine Generation und fasst das Clipboard NICHT an, wenn
        // inzwischen ein zweiter Paste lief — sonst wuerde er den frischen Text
        // mit dem alten Inhalt ueberschreiben und die zweite Aufnahme ginge
        // verloren. Thread-safe per Interlocked, kein Lock, kein Deadlock.
        private static long _clipboardGen;

        /// <summary>
        /// Versucht eine Clipboard-Operation auf dem UI-Thread mit kurzem Backoff.
        /// WPFs Clipboard-API wirft COMException (CLIPBRD_E_CANT_OPEN) wenn ein
        /// anderer Prozess das Clipboard offen hat (Excel-Copy, Browser-DragDrop,
        /// Antivirus). Ohne diese Wand fiel die App ueber eine unhandled Exception
        /// aus dem async-void-Handler und der Watchdog restartete still.
        /// </summary>
        private static bool TryRunOnUiThread(Action action, int maxAttempts = 5)
        {
            var app = Application.Current;
            if (app is null) return false; // shutting down

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
                    Thread.Sleep(30 * (1 << (attempt - 1))); // 30,60,120,240 ms
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

        // ── Paste ──

        /// <summary>
        /// Fuegt Text in die Ziel-App ein (Clipboard + Ctrl+V). Fokussiert das
        /// Eingabefeld vorher und stellt das alte Clipboard danach wieder her.
        /// </summary>
        public static void PasteText(string text, IntPtr appHwnd, bool autoEnter = false)
        {
            long myGen = Interlocked.Increment(ref _clipboardGen);

            string? previousClipboard = null;
            bool clipboardSet = TryRunOnUiThread(() =>
            {
                if (Clipboard.ContainsText())
                    previousClipboard = Clipboard.GetText();
                Clipboard.SetText(text);
            });
            if (!clipboardSet)
            {
                Console.WriteLine("PasteText: Clipboard.SetText fehlgeschlagen — Paste uebersprungen.");
                return;
            }

            BringToForeground(appHwnd);

            // Modifier-Release: Bei Hotkey-getriggertem Paste haelt Frank evtl. die
            // Trigger-Modifier (Win/Alt/Shift) noch physisch — Windows saehe sonst
            // "Win+Alt+Ctrl+V" statt "Ctrl+V". Ctrl bleibt unten (Strg+1..9-Hotkeys).
            ReleaseNonCtrlModifiers();

            FocusInputField(appHwnd);
            SendCtrlV();

            if (autoEnter)
            {
                Thread.Sleep(300);
                SendKey(VK_RETURN);
            }

            // Clipboard nur restoren, wenn unsere Generation noch aktuell ist.
            if (previousClipboard != null)
            {
                var prev = previousClipboard;
                Task.Delay(500).ContinueWith(_ =>
                {
                    if (Interlocked.Read(ref _clipboardGen) != myGen)
                    {
                        Console.WriteLine("Clipboard restore skipped — newer paste in flight.");
                        return;
                    }
                    TryRunOnUiThread(() => Clipboard.SetText(prev));
                });
            }
        }

        /// <summary>
        /// Fuegt den AKTUELLEN Clipboard-Inhalt ein (ohne ihn zu veraendern).
        /// </summary>
        public static void PasteClipboard(IntPtr appHwnd)
        {
            BringToForeground(appHwnd);
            ReleaseNonCtrlModifiers();
            FocusInputField(appHwnd);
            SendCtrlV();
        }

        // ── Loeschen (Electron-Textfeld) ──
        // ClearLine und ClearAllInput sind bei Electron-Apps funktional identisch:
        // beide leeren das gesamte Eingabefeld. Anders als ein Terminal (Readline,
        // Ctrl+U) ist das Eingabefeld ein markierbares Textfeld — Ctrl+A erfasst
        // auch mehrzeilige Eingaben in einem Rutsch.

        public static void ClearLine(IntPtr appHwnd)    => ClearInputCore(appHwnd);
        public static void ClearAllInput(IntPtr appHwnd) => ClearInputCore(appHwnd);

        private static void ClearInputCore(IntPtr appHwnd)
        {
            BringToForeground(appHwnd);

            if (IsCodexProcess(appHwnd))
            {
                // Codex: double-Escape (1. schliesst Popup/Autocomplete, 2. fokussiert
                // das Eingabefeld), dann Ctrl+Shift+Home-Selektion + Backspace.
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(150);
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(200);
                SendKey(VK_END);
                Thread.Sleep(30);
                byte ctrlScan  = (byte)Win32.MapVirtualKey(Win32.VK_CONTROL, Win32.MAPVK_VK_TO_VSC);
                byte shiftScan = (byte)Win32.MapVirtualKey(Win32.VK_SHIFT, Win32.MAPVK_VK_TO_VSC);
                byte homeScan  = (byte)Win32.MapVirtualKey(VK_HOME, Win32.MAPVK_VK_TO_VSC);
                Win32.keybd_event((byte)Win32.VK_CONTROL, ctrlScan, 0, UIntPtr.Zero);
                Win32.keybd_event((byte)Win32.VK_SHIFT, shiftScan, 0, UIntPtr.Zero);
                Win32.keybd_event((byte)VK_HOME, homeScan, Win32.KEYEVENTF_EXTENDEDKEY, UIntPtr.Zero);
                Win32.keybd_event((byte)VK_HOME, homeScan, Win32.KEYEVENTF_EXTENDEDKEY | Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
                Win32.keybd_event((byte)Win32.VK_SHIFT, shiftScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
                Win32.keybd_event((byte)Win32.VK_CONTROL, ctrlScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
                Thread.Sleep(50);
                SendKey(VK_BACKSPACE);
            }
            else
            {
                // Claude Desktop: ins Eingabefeld klicken, dann Ctrl+A + Backspace.
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
                SendKeyCombo(Win32.VK_CONTROL, VK_A);
                Thread.Sleep(50);
                SendKey(VK_BACKSPACE);
            }
        }

        // ── Return / Copy ──

        public static void SendReturn() => SendKey(VK_RETURN);

        public static void PressReturn(IntPtr appHwnd)
        {
            BringToForeground(appHwnd);

            if (IsCodexProcess(appHwnd))
            {
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(100);
            }
            else
            {
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
            }

            SendKey(VK_RETURN);
        }

        /// <summary>
        /// Kopiert die aktuelle Auswahl in der Ziel-App via Ctrl+C.
        /// </summary>
        public static void CopySelection(IntPtr appHwnd)
        {
            BringToForeground(appHwnd);
            SendKeyCombo(Win32.VK_CONTROL, VK_C);
        }

        // ── Eingabefeld-Fokus ──

        /// <summary>
        /// Bringt das Eingabefeld der Ziel-App in den Fokus. Codex hat eine
        /// abweichende Electron-Fensterstruktur (Chrome_RenderWidgetHostHWND ist
        /// KEIN direktes Kind) — dort wuerde SetFocus auf ein Kind den Tastatur-
        /// Fokus STEHLEN; deshalb nur Escape (gibt Fokus ans Eingabefeld zurueck).
        /// Bei Claude Desktop: Render-Widget fokussieren + ins Feld klicken.
        /// </summary>
        private static void FocusInputField(IntPtr appHwnd)
        {
            if (IsCodexProcess(appHwnd))
            {
                Console.WriteLine("AppController: Codex mode — Escape vor Eingabe");
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(200);
            }
            else
            {
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
            }
        }

        private static void FocusDirectRenderWidget(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;

            IntPtr renderWidget = Win32.FindWindowEx(appHwnd, IntPtr.Zero, "Chrome_RenderWidgetHostHWND", null);
            if (renderWidget == IntPtr.Zero)
            {
                Console.WriteLine("AppController: No Chrome_RenderWidgetHostHWND child — skipping SetFocus");
                return;
            }

            uint ourThread = Win32.GetCurrentThreadId();
            uint targetThread = Win32.GetWindowThreadProcessId(appHwnd, out _);

            bool attached = false;
            if (ourThread != targetThread)
                attached = Win32.AttachThreadInput(ourThread, targetThread, true);

            Win32.SetFocus(renderWidget);
            Console.WriteLine("AppController: Focused Chrome_RenderWidgetHostHWND");

            if (attached)
                Win32.AttachThreadInput(ourThread, targetThread, false);

            Thread.Sleep(50);
        }

        private static void ClickInputField(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;
            if (!Win32.GetWindowRect(appHwnd, out Win32.RECT rect)) return;

            int windowWidth = rect.Right - rect.Left;
            int windowHeight = rect.Bottom - rect.Top;
            if (windowWidth < 100 || windowHeight < 100) return;

            // Claude Desktop: Eingabefeld bei ~86% Hoehe, 55% Breite (rechts der Sidebar).
            int clickX = rect.Left + (int)(windowWidth * 0.55);
            int clickY = rect.Top + (int)(windowHeight * 0.86);

            clickX = Math.Clamp(clickX, rect.Left + 50, rect.Right - 50);
            clickY = Math.Clamp(clickY, rect.Top + 50, rect.Bottom - 50);

            Win32.GetCursorPos(out Win32.POINT savedPos);

            Win32.SetCursorPos(clickX, clickY);
            Thread.Sleep(15);
            Win32.mouse_event(Win32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, UIntPtr.Zero);
            Thread.Sleep(15);
            Win32.mouse_event(Win32.MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
            Thread.Sleep(150);

            Win32.SetCursorPos(savedPos.X, savedPos.Y);

            Console.WriteLine($"AppController: Clicked at ({clickX},{clickY}), window=({rect.Left},{rect.Top},{rect.Right},{rect.Bottom})");
        }

        // ── Process Detection ──

        private static bool IsCodexProcess(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return false;
            Win32.GetWindowThreadProcessId(appHwnd, out uint pid);
            try
            {
                using var proc = Process.GetProcessById((int)pid);
                var name = proc.ProcessName;
                return name.Equals("Codex", StringComparison.OrdinalIgnoreCase)
                    || name.Equals("Cursor", StringComparison.OrdinalIgnoreCase);
            }
            catch { return false; }
        }

        // ── Window Activation ──

        private static void BringToForeground(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;

            var currentFg = Win32.GetForegroundWindow();
            if (IsOwnedByProcess(currentFg, appHwnd))
            {
                Thread.Sleep(30);
                return;
            }

            uint ourThread = Win32.GetCurrentThreadId();
            uint targetThread = Win32.GetWindowThreadProcessId(appHwnd, out _);

            bool attached = false;
            if (ourThread != targetThread)
                attached = Win32.AttachThreadInput(ourThread, targetThread, true);

            Win32.AllowSetForegroundWindow(unchecked((uint)-1));
            Win32.SetForegroundWindow(appHwnd);
            Win32.BringWindowToTop(appHwnd);
            Thread.Sleep(200);

            if (attached)
                Win32.AttachThreadInput(ourThread, targetThread, false);
        }

        private static bool IsOwnedByProcess(IntPtr foregroundHwnd, IntPtr appHwnd)
        {
            if (foregroundHwnd == appHwnd) return true;
            Win32.GetWindowThreadProcessId(foregroundHwnd, out uint fgPid);
            Win32.GetWindowThreadProcessId(appHwnd, out uint appPid);
            return fgPid == appPid && fgPid != 0;
        }

        // ── Low-level key/mouse helpers ──

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
            byte ctrlScan = (byte)Win32.MapVirtualKey(Win32.VK_CONTROL, Win32.MAPVK_VK_TO_VSC);
            byte vScan    = (byte)Win32.MapVirtualKey(Win32.VK_V, Win32.MAPVK_VK_TO_VSC);

            Win32.keybd_event((byte)Win32.VK_CONTROL, ctrlScan, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, vScan, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, vScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_CONTROL, ctrlScan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }

        /// <summary>
        /// Synthetische KEYUP-Events fuer Win, Alt und Shift, damit der virtuelle
        /// Tastatur-State sauber ist bevor SendCtrlV laeuft. Ctrl bleibt unten,
        /// weil Strg+1..9-Hotkeys den Ctrl-Modifier intentional gedrueckt halten.
        /// </summary>
        private static void ReleaseNonCtrlModifiers()
        {
            void Up(ushort vk)
            {
                byte scan = (byte)Win32.MapVirtualKey(vk, Win32.MAPVK_VK_TO_VSC);
                Win32.keybd_event((byte)vk, scan, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            }

            Up(0x5B); // VK_LWIN
            Up(0x5C); // VK_RWIN
            Up(0xA4); // VK_LMENU (linke Alt)
            Up(0xA5); // VK_RMENU (rechte Alt / AltGr)
            Up(0xA0); // VK_LSHIFT
            Up(0xA1); // VK_RSHIFT
            Thread.Sleep(20);
        }

        private static void SendKey(ushort vk)
        {
            byte scan = (byte)Win32.MapVirtualKey(vk, Win32.MAPVK_VK_TO_VSC);
            // Home, End, Delete, Insert, Page Up/Down, Arrow keys (0x21–0x2E) sind Extended Keys
            uint flags = (vk >= 0x21 && vk <= 0x2E) ? Win32.KEYEVENTF_EXTENDEDKEY : 0;

            Win32.keybd_event((byte)vk, scan, flags, UIntPtr.Zero);
            Win32.keybd_event((byte)vk, scan, flags | Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }
    }
}
