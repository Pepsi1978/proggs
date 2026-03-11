using System;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    public static class AppController
    {
        private const byte VK_RETURN = 0x0D;

        /// <summary>
        /// Pastes text into the target app (Claude/Codex) via Clipboard + SendKeys Ctrl+V.
        /// Uses SendKeys.SendWait for Electron-App compatibility, with keybd_event fallback.
        /// Optionally sends Enter afterwards (auto-enter).
        /// </summary>
        public static void PasteText(string text, IntPtr appHwnd, bool autoEnter = false)
        {
            // Save previous clipboard content before overwriting
            string previousClipboard = null;
            Application.Current.Dispatcher.Invoke(() =>
            {
                if (Clipboard.ContainsText())
                    previousClipboard = Clipboard.GetText();
            });

            // Set clipboard on UI thread
            Application.Current.Dispatcher.Invoke(() => Clipboard.SetText(text));

            // Bring target app to foreground
            if (appHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(appHwnd);
                Thread.Sleep(100);
            }

            // Send Ctrl+V via SendKeys (works reliably with Electron apps)
            try
            {
                System.Windows.Forms.SendKeys.SendWait("^v");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AppController: SendKeys failed ({ex.Message}), using keybd_event fallback");
                SendCtrlVFallback();
            }

            // Restore previous clipboard asynchronously after 500ms
            if (previousClipboard != null)
            {
                var prev = previousClipboard;
                Task.Delay(500).ContinueWith(_ =>
                {
                    Application.Current.Dispatcher.Invoke(() => Clipboard.SetText(prev));
                });
            }

            // Send Enter if auto-enter is enabled
            if (autoEnter)
            {
                Thread.Sleep(300);
                // Re-focus app before Enter
                if (appHwnd != IntPtr.Zero)
                {
                    Win32.SetForegroundWindow(appHwnd);
                    Thread.Sleep(100);
                }
                SendKey(VK_RETURN);
            }
        }

        /// <summary>
        /// Clears the current input in the target app (Claude/Codex).
        /// Uses Ctrl+A (select all) + Backspace — works for Electron text areas.
        /// </summary>
        public static void ClearInput(IntPtr appHwnd)
        {
            if (appHwnd != IntPtr.Zero)
            {
                Win32.SetForegroundWindow(appHwnd);
                Thread.Sleep(150);
            }

            try
            {
                // Select all text in the input field
                System.Windows.Forms.SendKeys.SendWait("^a");
                Thread.Sleep(50);

                // Delete selected text
                System.Windows.Forms.SendKeys.SendWait("{BACKSPACE}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AppController: SendKeys failed ({ex.Message}), using keybd_event fallback");
                ClearInputFallback();
            }
        }

        private static void SendCtrlVFallback()
        {
            // Use keybd_event — works across UIPI boundaries
            Win32.keybd_event((byte)Win32.VK_CONTROL, 0, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, 0, 0, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_V, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_CONTROL, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }

        private static void ClearInputFallback()
        {
            // Ctrl+A via keybd_event
            const byte VK_A = 0x41;
            const byte VK_BACKSPACE = 0x08;

            Win32.keybd_event((byte)Win32.VK_CONTROL, 0, 0, UIntPtr.Zero);
            Win32.keybd_event(VK_A, 0, 0, UIntPtr.Zero);
            Win32.keybd_event(VK_A, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Win32.keybd_event((byte)Win32.VK_CONTROL, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
            Thread.Sleep(50);

            Win32.keybd_event(VK_BACKSPACE, 0, 0, UIntPtr.Zero);
            Win32.keybd_event(VK_BACKSPACE, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }

        private static void SendKey(byte vk)
        {
            Win32.keybd_event(vk, 0, 0, UIntPtr.Zero);
            Win32.keybd_event(vk, 0, Win32.KEYEVENTF_KEYUP, UIntPtr.Zero);
        }
    }
}
