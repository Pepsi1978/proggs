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
        /// Clears the current terminal input line by sending Ctrl+C.
        /// Matches macOS approach (TerminalController.swift clearLine).
        /// Ctrl+C discards the entire pending input and gives a fresh prompt
        /// in PowerShell (PSReadLine CancelLine), cmd.exe, and bash/zsh.
        /// </summary>
        public static void ClearLine(IntPtr terminalHwnd)
        {
            if (terminalHwnd != IntPtr.Zero)
            {
                Win32.AllowSetForegroundWindow(unchecked((uint)-1));
                Win32.SetForegroundWindow(terminalHwnd);
                Thread.Sleep(150);
            }

            // Ctrl+C — cancels current input, fresh prompt (same as macOS)
            SendKeyCombo(Win32.VK_CONTROL, VK_C);
        }

        private const ushort VK_C = 0x43;

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
