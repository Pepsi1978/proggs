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

            // Bring terminal to foreground
            if (terminalHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(terminalHwnd);
                Thread.Sleep(150); // macOS uses usleep 150ms
            }

            // Send Ctrl+V
            SendCtrlV();

            // Send Enter if auto-enter is enabled
            if (autoEnter)
            {
                Thread.Sleep(300); // macOS uses 300ms before optional Enter
                if (terminalHwnd != IntPtr.Zero)
                {
                    Win32.SetForegroundWindow(terminalHwnd);
                    Thread.Sleep(100);
                }
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
        /// Clears the current terminal input line.
        /// Escape → Home → Shift+End → Delete (fast and reliable, matches macOS Ctrl+C approach).
        /// </summary>
        public static void ClearLine(IntPtr terminalHwnd)
        {
            if (terminalHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(terminalHwnd);
                Thread.Sleep(150);
            }

            // Escape to cancel any selection/mode
            SendKey(Win32.VK_ESCAPE);
            Thread.Sleep(50);

            // Home — go to start of line
            SendKey(VK_HOME);
            Thread.Sleep(50);

            // Shift+End — select entire line
            Win32.keybd_event((byte)Win32.VK_SHIFT, 0, 0, UIntPtr.Zero);
            SendKey(VK_END);
            Win32.keybd_event((byte)Win32.VK_SHIFT, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Thread.Sleep(50);

            // Delete — delete the selection
            SendKey(VK_DELETE);
        }

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
            if (terminalHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(terminalHwnd);
                Thread.Sleep(100);
            }
            SendKey(VK_RETURN);
        }

        private static void SendCtrlV()
        {
            // Use keybd_event — works across UIPI boundaries unlike SendInput
            Win32.keybd_event((byte)Win32.VK_CONTROL, 0, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, 0, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_CONTROL, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }

        private static void SendKey(ushort vk)
        {
            Win32.keybd_event((byte)vk, 0, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)vk, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }
    }
}
