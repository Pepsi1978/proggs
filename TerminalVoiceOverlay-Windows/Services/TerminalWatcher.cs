using System;
using System.Diagnostics;
using System.Windows;
using TerminalVoiceOverlay.NativeMethods;

namespace TerminalVoiceOverlay.Services
{
    public sealed class TerminalWatcher : IDisposable
    {
        private readonly string[] _terminalProcessNames;

        private Win32.WinEventDelegate? _winEventDelegate;
        private IntPtr _hookHandle;
        private IntPtr _lastTerminalHwnd;

        public event Action<IntPtr>? TerminalActivated;
        public event Action? TerminalDeactivated;

        public IntPtr ActiveTerminalHwnd => _lastTerminalHwnd;

        public TerminalWatcher(string[]? processNames = null)
        {
            _terminalProcessNames = processNames ?? new[] { "WindowsTerminal", "pwsh", "powershell" };
        }

        public void Start()
        {
            // Must store delegate in a field to prevent GC
            _winEventDelegate = OnWinEvent;

            _hookHandle = Win32.SetWinEventHook(
                Win32.EVENT_SYSTEM_FOREGROUND,
                Win32.EVENT_SYSTEM_FOREGROUND,
                IntPtr.Zero,
                _winEventDelegate,
                0, 0,
                Win32.WINEVENT_OUTOFCONTEXT | Win32.WINEVENT_SKIPOWNPROCESS);

            if (_hookHandle == IntPtr.Zero)
                Console.WriteLine("TerminalWatcher: SetWinEventHook fehlgeschlagen");

            // Check initial state
            CheckForegroundWindow(Win32.GetForegroundWindow());
        }

        private void OnWinEvent(IntPtr hWinEventHook, uint eventType, IntPtr hwnd,
            int idObject, int idChild, uint dwEventThread, uint dwmsEventTime)
        {
            if (eventType == Win32.EVENT_SYSTEM_FOREGROUND)
            {
                CheckForegroundWindow(hwnd);
            }
        }

        private void CheckForegroundWindow(IntPtr hwnd)
        {
            if (hwnd == IntPtr.Zero) return;

            if (IsTerminalWindow(hwnd))
            {
                _lastTerminalHwnd = hwnd;
                Application.Current?.Dispatcher.Invoke(() => TerminalActivated?.Invoke(hwnd));
            }
            else
            {
                Application.Current?.Dispatcher.Invoke(() => TerminalDeactivated?.Invoke());
            }
        }

        private bool IsTerminalWindow(IntPtr hwnd)
        {
            Win32.GetWindowThreadProcessId(hwnd, out uint pid);
            if (pid == 0) return false;

            try
            {
                using var proc = Process.GetProcessById((int)pid);
                var name = proc.ProcessName;
                foreach (var target in _terminalProcessNames)
                {
                    if (name.Equals(target, StringComparison.OrdinalIgnoreCase))
                        return true;
                }
            }
            catch
            {
                // Process may have exited
            }

            return false;
        }

        public static Rect GetMonitorWorkArea(IntPtr hwnd)
        {
            var hMonitor = Win32.MonitorFromWindow(hwnd, Win32.MONITOR_DEFAULTTONEAREST);
            var mi = new Win32.MONITORINFO { cbSize = System.Runtime.InteropServices.Marshal.SizeOf<Win32.MONITORINFO>() };

            if (Win32.GetMonitorInfo(hMonitor, ref mi))
            {
                // Get DPI for scaling
                double dpiScale = 1.0;
                if (Win32.GetDpiForMonitor(hMonitor, 0, out uint dpiX, out _) == 0)
                {
                    dpiScale = dpiX / 96.0;
                }

                var work = mi.rcWork;
                return new Rect(
                    work.Left / dpiScale,
                    work.Top / dpiScale,
                    (work.Right - work.Left) / dpiScale,
                    (work.Bottom - work.Top) / dpiScale);
            }

            // Fallback
            return new Rect(0, 0, SystemParameters.PrimaryScreenWidth, SystemParameters.PrimaryScreenHeight);
        }

        public void Dispose()
        {
            if (_hookHandle != IntPtr.Zero)
            {
                Win32.UnhookWinEvent(_hookHandle);
                _hookHandle = IntPtr.Zero;
            }
        }
    }
}
