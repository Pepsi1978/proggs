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
                Thread.Sleep(100);
            }

            // Send Ctrl+V
            SendCtrlV();

            // Send Enter if auto-enter is enabled
            if (autoEnter)
            {
                Thread.Sleep(300); // Reduced from 500ms
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
        /// Goes to End, then spams Backspace to delete everything.
        /// </summary>
        public static void ClearLine(IntPtr terminalHwnd)
        {
            if (terminalHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(terminalHwnd);
                Thread.Sleep(150);
            }

            // Escape to dismiss autocomplete/menus
            SendKey(Win32.VK_ESCAPE);
            Thread.Sleep(100);

            // End — go to end of line
            SendKey(VK_END);
            Thread.Sleep(50);

            // Send many Backspaces to delete entire line (max ~500 chars)
            for (int i = 0; i < 500; i++)
            {
                Win32.keybd_event(VK_BACKSPACE, 0, 0, UIntPtr.Zero);
                Win32.keybd_event(VK_BACKSPACE, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            }

        }

        private const byte VK_END = 0x23;
        private const byte VK_BACKSPACE = 0x08;
        private const byte VK_RETURN = 0x0D;

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
