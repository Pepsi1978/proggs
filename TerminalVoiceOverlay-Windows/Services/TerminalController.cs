using System;
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
        /// Pastes text into the terminal via Clipboard + Ctrl+V.
        /// Ensures the terminal window is focused first.
        /// </summary>
        public static void PasteText(string text, IntPtr terminalHwnd, bool autoEnter = false)
        {
            // Save previous clipboard content
            string? previousClipboard = null;
            Application.Current.Dispatcher.Invoke(() =>
            {
                if (Clipboard.ContainsText())
                    previousClipboard = Clipboard.GetText();
                Clipboard.SetText(text);
            });

            // Bring terminal to foreground (robust: AttachThreadInput + AllowSetForegroundWindow)
            BringToForeground(terminalHwnd);

            // Send Ctrl+V
            SendCtrlV();

            // Send Enter if auto-enter is enabled
            if (autoEnter)
            {
                Thread.Sleep(300); // macOS uses 300ms before optional Enter
                BringToForeground(terminalHwnd);
                SendKey(VK_RETURN);
            }

            // Restore previous clipboard after paste completes
            if (previousClipboard != null)
            {
                var prev = previousClipboard;
                Task.Delay(500).ContinueWith(_ =>
                {
                    Application.Current.Dispatcher.Invoke(() => Clipboard.SetText(prev));
                });
            }
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
        private static void BringToForeground(IntPtr terminalHwnd)
        {
            if (terminalHwnd == IntPtr.Zero) return;

            var currentFg = Win32.GetForegroundWindow();
            if (currentFg == terminalHwnd)
            {
                Thread.Sleep(30);
                return;
            }

            uint ourThread = Win32.GetCurrentThreadId();
            uint targetThread = Win32.GetWindowThreadProcessId(terminalHwnd, out _);

            bool attached = false;
            if (ourThread != targetThread)
                attached = Win32.AttachThreadInput(ourThread, targetThread, true);

            Win32.AllowSetForegroundWindow(unchecked((uint)-1));
            Win32.SetForegroundWindow(terminalHwnd);
            Win32.BringWindowToTop(terminalHwnd);
            Thread.Sleep(200);

            if (attached)
                Win32.AttachThreadInput(ourThread, targetThread, false);
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
