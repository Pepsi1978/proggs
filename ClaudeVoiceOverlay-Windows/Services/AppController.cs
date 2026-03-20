using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
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
        private const ushort VK_HOME = 0x24;
        private const ushort VK_END = 0x23;

        public static void PasteText(string text, IntPtr appHwnd, bool autoEnter = false)
        {
            string? previousClipboard = null;
            Application.Current.Dispatcher.Invoke(() =>
            {
                if (Clipboard.ContainsText())
                    previousClipboard = Clipboard.GetText();
                Clipboard.SetText(text);
            });

            BringToForeground(appHwnd);

            bool isCodex = IsCodexProcess(appHwnd);

            if (isCodex)
            {
                // Codex has a different Electron window structure:
                // Chrome_RenderWidgetHostHWND is NOT a child of the main window.
                // SetFocus on any child window STEALS keyboard focus.
                // Instead: just Escape (returns focus to input) → Ctrl+V
                Console.WriteLine("AppController: Codex mode — Escape → Ctrl+V");
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(200);
                SendCtrlV();
            }
            else
            {
                // Claude Desktop: render widget is a direct child — SetFocus + click works
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
                SendCtrlV();
            }

            if (autoEnter)
            {
                Thread.Sleep(300);
                SendKey(VK_RETURN);
            }

            if (previousClipboard != null)
            {
                var prev = previousClipboard;
                Task.Delay(500).ContinueWith(_ =>
                {
                    Application.Current.Dispatcher.Invoke(() => Clipboard.SetText(prev));
                });
            }
        }

        public static void ClearInput(IntPtr appHwnd)
        {
            BringToForeground(appHwnd);

            if (IsCodexProcess(appHwnd))
            {
                // Codex: double-Escape — first closes any popup/autocomplete,
                // second ensures focus lands in the input field
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(150);
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(200);
                SendKey(VK_END);
                Thread.Sleep(30);
                // Ctrl+Shift+Home selects all text from cursor to start within the input
                byte ctrlScan = (byte)Win32.MapVirtualKey(Win32.VK_CONTROL, Win32.MAPVK_VK_TO_VSC);
                byte shiftScan = (byte)Win32.MapVirtualKey(Win32.VK_SHIFT, Win32.MAPVK_VK_TO_VSC);
                byte homeScan = (byte)Win32.MapVirtualKey((uint)VK_HOME, Win32.MAPVK_VK_TO_VSC);
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
                // Claude Desktop: click into input field, then Ctrl+A is safe
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
                SendKeyCombo(Win32.VK_CONTROL, VK_A);
                Thread.Sleep(50);
                SendKey(VK_BACKSPACE);
            }
        }

        public static void SendReturn()
        {
            SendKey(VK_RETURN);
        }

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

        // ── Render Widget Focus (Claude Desktop only) ──

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

        // ── Input Field Click (Claude Desktop only) ──

        private static void ClickInputField(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;
            if (!Win32.GetWindowRect(appHwnd, out Win32.RECT rect)) return;

            int windowWidth = rect.Right - rect.Left;
            int windowHeight = rect.Bottom - rect.Top;
            if (windowWidth < 100 || windowHeight < 100) return;

            // Claude Desktop: input at ~86% height, 55% width (past sidebar)
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

        // ── Helpers ──

        private static bool IsOwnedByProcess(IntPtr foregroundHwnd, IntPtr appHwnd)
        {
            if (foregroundHwnd == appHwnd) return true;
            Win32.GetWindowThreadProcessId(foregroundHwnd, out uint fgPid);
            Win32.GetWindowThreadProcessId(appHwnd, out uint appPid);
            return fgPid == appPid && fgPid != 0;
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
