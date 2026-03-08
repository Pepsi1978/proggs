using System;
using System.Threading;
using System.Windows;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    public static class AppController
    {
        /// <summary>
        /// Pastes text into the target app (Claude/Codex) via Clipboard + SendKeys Ctrl+V.
        /// Uses SendKeys.SendWait for Electron-App compatibility, with keybd_event fallback.
        /// </summary>
        public static void PasteText(string text, IntPtr appHwnd)
        {
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
                Console.WriteLine($"AppController: Text eingefuegt via SendKeys (hwnd={appHwnd})");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AppController: SendKeys fehlgeschlagen ({ex.Message}), verwende keybd_event Fallback");
                SendCtrlVFallback();
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
                Console.WriteLine("AppController: Eingabe geloescht via SendKeys");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AppController: SendKeys fehlgeschlagen ({ex.Message}), verwende keybd_event Fallback");
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
            Console.WriteLine("AppController: Ctrl+V gesendet (keybd_event Fallback)");
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
            Console.WriteLine("AppController: Eingabe geloescht (keybd_event Fallback)");
        }
    }
}
