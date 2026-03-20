using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Controls text insertion into Electron apps (Claude Desktop, Codex, Cursor).
    /// Strategy: CDP first (if pre-connected) → keybd_event fallback.
    /// CDP scan happens at startup in background — NEVER blocks paste operations.
    /// </summary>
    public static class AppController
    {
        private const byte VK_A = 0x41;
        private const byte VK_BACKSPACE = 0x08;
        private const byte VK_RETURN = 0x0D;
        private const ushort VK_HOME = 0x24;
        private const ushort VK_END = 0x23;

        private static readonly CdpClient _cdp = new();

        /// <summary>
        /// Call once at startup — scans for CDP in background without blocking UI.
        /// </summary>
        public static void InitCdpInBackground()
        {
            Task.Run(async () =>
            {
                var connected = await _cdp.TryConnectAsync();
                if (connected)
                    Console.WriteLine($"AppController: CDP connected on port {_cdp.ActivePort} — using direct text injection");
                else
                    Console.WriteLine("AppController: No CDP available — using keybd_event fallback");
            });
        }

        // ── Public API ──

        public static void PasteText(string text, IntPtr appHwnd, bool autoEnter = false)
        {
            // CDP only if already connected (no blocking scan here!)
            if (_cdp.IsConnected)
            {
                try
                {
                    var ok = Task.Run(() => _cdp.InsertTextAsync(text)).GetAwaiter().GetResult();
                    if (ok)
                    {
                        Console.WriteLine("AppController: Pasted via CDP");
                        if (autoEnter)
                        {
                            Thread.Sleep(100);
                            Task.Run(() => _cdp.SendEnterAsync()).GetAwaiter().GetResult();
                        }
                        return;
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"AppController: CDP failed: {ex.Message}");
                }
            }

            // Fallback: clipboard + keybd_event
            PasteViaKeyboard(text, appHwnd, autoEnter);
        }

        public static void ClearInput(IntPtr appHwnd)
        {
            if (_cdp.IsConnected)
            {
                try
                {
                    var ok = Task.Run(() => _cdp.ClearInputAsync()).GetAwaiter().GetResult();
                    if (ok) return;
                }
                catch { }
            }

            ClearViaKeyboard(appHwnd);
        }

        public static void SendReturn()
        {
            SendKey(VK_RETURN);
        }

        public static void PressReturn(IntPtr appHwnd)
        {
            if (_cdp.IsConnected)
            {
                try
                {
                    var ok = Task.Run(() => _cdp.SendEnterAsync()).GetAwaiter().GetResult();
                    if (ok) return;
                }
                catch { }
            }

            PressReturnViaKeyboard(appHwnd);
        }

        // ── Keyboard Methods (proven approach from March 13) ──

        private static void PasteViaKeyboard(string text, IntPtr appHwnd, bool autoEnter)
        {
            string? previousClipboard = null;
            Application.Current.Dispatcher.Invoke(() =>
            {
                if (Clipboard.ContainsText())
                    previousClipboard = Clipboard.GetText();
                Clipboard.SetText(text);
            });

            BringToForeground(appHwnd);

            if (IsCodexProcess(appHwnd))
            {
                Console.WriteLine("AppController: Codex/Cursor mode — Escape → Ctrl+V");
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(200);
                SendCtrlV();
            }
            else
            {
                // Claude Desktop: focus render widget, click input field, paste
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

        private static void ClearViaKeyboard(IntPtr appHwnd)
        {
            BringToForeground(appHwnd);

            if (IsCodexProcess(appHwnd))
            {
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(150);
                SendKey(Win32.VK_ESCAPE);
                Thread.Sleep(200);
                SendKey(VK_END);
                Thread.Sleep(30);
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
                // Claude Desktop: click input, select all within input, delete
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
                SendKeyCombo(Win32.VK_CONTROL, VK_A);
                Thread.Sleep(50);
                SendKey(VK_BACKSPACE);
            }
        }

        private static void PressReturnViaKeyboard(IntPtr appHwnd)
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

        /// <summary>
        /// Clicks the input field in Claude Desktop.
        /// Uses bottom-center position that works across all tabs (Code, Chat, Cowork).
        /// The input field is always near the bottom of the content area.
        /// </summary>
        private static void ClickInputField(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;
            if (!Win32.GetWindowRect(appHwnd, out Win32.RECT rect)) return;

            int windowWidth = rect.Right - rect.Left;
            int windowHeight = rect.Bottom - rect.Top;
            if (windowWidth < 100 || windowHeight < 100) return;

            // Input field position across ALL tabs:
            // - Always near the bottom of the window
            // - Horizontally centered in the content area (right of sidebar)
            // - Sidebar is ~15-20% of window width on the left
            // Click at 50% width (center of content), 92% height (bottom area)
            int clickX = rect.Left + (int)(windowWidth * 0.50);
            int clickY = rect.Top + (int)(windowHeight * 0.92);

            clickX = Math.Clamp(clickX, rect.Left + 50, rect.Right - 50);
            clickY = Math.Clamp(clickY, rect.Top + 50, rect.Bottom - 30);

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
