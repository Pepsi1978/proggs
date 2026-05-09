using System;
using System.Collections.Concurrent;
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

        // PID -> (isTerminal, expiry). EVENT_SYSTEM_FOREGROUND feuert bei
        // JEDEM Alt+Tab und jedem Mausklick auf ein anderes Top-Level-Window
        // — bei aktivem Arbeiten zwischen Browser, Editor und Terminal kommen
        // 10-50 Events pro Minute. Ohne Cache machte jeder Event einen
        // Process.GetProcessById-Aufruf (OpenProcess + QueryFullProcessImageName
        // + Dispose), je 0.5-1.5 ms inkl. Handle-Allokation. Mit 1-Sekunde-TTL
        // hat Frank trotzdem sofortige Reaktion auf neu gestartete Terminals
        // (er muss naemlich 1x in den neuen Prozess klicken — das ist die
        // Cache-Miss-Latenz, der "alte" Prozess kommt aus dem Cache). PID-
        // Reuse-Risiko: Windows recycelt PIDs nicht binnen 1 s, daher praktisch
        // unsichtbar.
        private static readonly ConcurrentDictionary<uint, (bool IsTerminal, long ExpiryTicks)> _pidCache = new();
        private const long PidCacheTtlTicks = TimeSpan.TicksPerSecond * 1; // 1 s

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

            // BeginInvoke statt Invoke: Dieser Callback laeuft auf dem
            // System-weiten WinEvent-Hook-Thread. Synchron auf den UI-Thread
            // zu warten staut bei beschaeftigtem UI alle System-Foreground-
            // Events auf — Alt+Tab und Fenster-Wechsel werden global langsam.
            // BeginInvoke postet asynchron und der Hook-Thread kehrt sofort
            // zurueck. Reihenfolge bleibt erhalten (Dispatcher-Queue ist FIFO).
            if (IsTerminalWindow(hwnd))
            {
                _lastTerminalHwnd = hwnd;
                Application.Current?.Dispatcher.BeginInvoke(() => TerminalActivated?.Invoke(hwnd));
            }
            else
            {
                Application.Current?.Dispatcher.BeginInvoke(() => TerminalDeactivated?.Invoke());
            }
        }

        private bool IsTerminalWindow(IntPtr hwnd)
        {
            Win32.GetWindowThreadProcessId(hwnd, out uint pid);
            if (pid == 0) return false;

            // Cache-Lookup. Treffer + nicht abgelaufen → direkt zurueck, kein
            // Win32-Roundtrip. Cache-Miss-Pfad weiter unten erfasst auch das
            // negative Ergebnis (NICHT-Terminal-Prozesse), denn Frank klickt
            // primaer auf seinen Browser hin und her — das ist der haeufigste
            // Pfad und profitiert am meisten vom Cache.
            long now = DateTime.UtcNow.Ticks;
            if (_pidCache.TryGetValue(pid, out var cached) && cached.ExpiryTicks > now)
                return cached.IsTerminal;

            bool isTerminal = false;
            try
            {
                using var proc = Process.GetProcessById((int)pid);
                var name = proc.ProcessName;
                foreach (var target in _terminalProcessNames)
                {
                    if (name.Equals(target, StringComparison.OrdinalIgnoreCase))
                    {
                        isTerminal = true;
                        break;
                    }
                }
            }
            catch
            {
                // Process may have exited — Default-Fall: NICHT-Terminal.
            }

            // Cache schreiben — auch negative Ergebnisse, denn die machen die
            // Mehrheit der Foreground-Events aus. Sehr seltenes Wachstum: Pro
            // einzigartiger PID 1 Eintrag bis Ablauf. Ein simpler Sweep beim
            // Schreiben begrenzt Wachstum auf maximal ein paar hundert Eintraege
            // selbst nach Stunden — kein dediziertes LRU noetig.
            _pidCache[pid] = (isTerminal, now + PidCacheTtlTicks);
            if (_pidCache.Count > 256)
            {
                foreach (var kv in _pidCache)
                {
                    if (kv.Value.ExpiryTicks <= now)
                        _pidCache.TryRemove(kv.Key, out _);
                }
            }
            return isTerminal;
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
