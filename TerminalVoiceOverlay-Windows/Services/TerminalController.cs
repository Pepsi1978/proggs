using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows;
using TerminalVoiceOverlay.NativeMethods;

namespace TerminalVoiceOverlay.Services
{
    public static class TerminalController
    {
        /// <summary>
        /// Pastes text into the active terminal via Clipboard + Ctrl+V.
        /// </summary>
        public static void PasteText(string text)
        {
            // Set clipboard on UI thread
            Application.Current.Dispatcher.Invoke(() => Clipboard.SetText(text));

            // Small delay to ensure clipboard is set
            Thread.Sleep(50);

            // Send Ctrl+V
            SendCtrlV();
            Debug.WriteLine("TerminalController: Text eingefügt");
        }

        /// <summary>
        /// Clears the current terminal input line by sending Escape.
        /// Works with PSReadLine in all target terminals.
        /// </summary>
        public static void ClearLine()
        {
            SendKey(Win32.VK_ESCAPE);
            Debug.WriteLine("TerminalController: Zeile gelöscht");
        }

        private static void SendCtrlV()
        {
            var inputs = new Win32.INPUT[4];

            // Ctrl down
            inputs[0] = MakeKeyInput(Win32.VK_CONTROL, false);
            // V down
            inputs[1] = MakeKeyInput(Win32.VK_V, false);
            // V up
            inputs[2] = MakeKeyInput(Win32.VK_V, true);
            // Ctrl up
            inputs[3] = MakeKeyInput(Win32.VK_CONTROL, true);

            Win32.SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<Win32.INPUT>());
        }

        private static void SendKey(ushort vk)
        {
            var inputs = new Win32.INPUT[2];
            inputs[0] = MakeKeyInput(vk, false);
            inputs[1] = MakeKeyInput(vk, true);
            Win32.SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<Win32.INPUT>());
        }

        private static Win32.INPUT MakeKeyInput(ushort vk, bool keyUp)
        {
            return new Win32.INPUT
            {
                type = Win32.INPUT_KEYBOARD,
                u = new Win32.INPUTUNION
                {
                    ki = new Win32.KEYBDINPUT
                    {
                        wVk = vk,
                        wScan = 0,
                        dwFlags = keyUp ? Win32.KEYEVENTF_KEYUP : 0,
                        time = 0,
                        dwExtraInfo = IntPtr.Zero
                    }
                }
            };
        }
    }
}
