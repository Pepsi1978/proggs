using System;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    public static class AppController
    {
        private const byte VK_A = 0x41;
        private const byte VK_BACKSPACE = 0x08;
        private const byte VK_RETURN = 0x0D;

        /// <summary>
        /// Pastes text into the target app (Claude/Codex) via Clipboard + keybd_event Ctrl+V.
        /// Uses keybd_event with proper scan codes for reliability with Electron apps.
        /// </summary>
        public static void PasteText(string text, IntPtr appHwnd, bool autoEnter = false)
        {
            // Save previous clipboard content
            string? previousClipboard = null;
            Application.Current.Dispatcher.Invoke(() =>
            {
                if (Clipboard.ContainsText())
                    previousClipboard = Clipboard.GetText();
                Clipboard.SetText(text);
            });

            // Bring target app to foreground
            if (appHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(appHwnd);
                Thread.Sleep(150);
            }

            // Send Ctrl+V via keybd_event with scan codes
            SendCtrlV();

            // Send Enter if auto-enter is enabled
            if (autoEnter)
            {
                Thread.Sleep(300);
                if (appHwnd != IntPtr.Zero)
                {
                    Win32.SetForegroundWindow(appHwnd);
                    Thread.Sleep(100);
                }
                SendKey(VK_RETURN);
            }

            // Restore previous clipboard after 500ms
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
        /// Clears the current input in the target app (Claude/Codex).
        /// Ctrl+A (select all) + Backspace — works for Electron text areas.
        /// Uses keybd_event with scan codes for reliability.
        /// </summary>
        public static void ClearInput(IntPtr appHwnd)
        {
            if (appHwnd != IntPtr.Zero)
            {
                Win32.AllowSetForegroundWindow(unchecked((uint)-1));
                Win32.SetForegroundWindow(appHwnd);
                Thread.Sleep(150);
            }

            // Ctrl+A to select all
            SendKeyCombo(Win32.VK_CONTROL, VK_A);
            Thread.Sleep(50);

            // Backspace to delete selection
            SendKey(VK_BACKSPACE);
        }

        /// <summary>
        /// Sends the Enter/Return key without focusing any window.
        /// </summary>
        public static void SendReturn()
        {
            SendKey(VK_RETURN);
        }

        /// <summary>
        /// Focuses the target app then sends Enter/Return.
        /// Called by EnterButton when toggling auto-enter ON.
        /// </summary>
        public static void PressReturn(IntPtr appHwnd)
        {
            if (appHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(appHwnd);
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
            uint flags = (vk >= 0x21 && vk <= 0x2E) ? Win32.KEYEVENTF_EXTENDEDKEY : 0;

            Win32.keybd_event((byte)vk, scan, flags, UIntPtr.Zero);
            Win32.keybd_event((byte)vk, scan, flags | Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }
    }
}
