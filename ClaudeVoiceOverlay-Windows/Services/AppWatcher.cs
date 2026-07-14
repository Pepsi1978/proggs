using System;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Windows;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Beobachtet das aktive Vordergrund-Fenster und meldet, wenn eine der
    /// Ziel-Apps (Claude Desktop oder kombinierte ChatGPT-/Codex-App) nach vorne kommt bzw. den Fokus
    /// verliert. Schwester-Klasse zu TerminalVoiceOverlay/TerminalWatcher —
    /// identische Mechanik (SetWinEventHook + PID-Cache), nur andere
    /// Ziel-Prozesse. Die oeffentliche API (AppActivated/AppDeactivated,
    /// ActiveAppHwnd, GetMonitorWorkArea) ist deckungsgleich mit der des
    /// TerminalWatcher, damit das gemeinsame OverlayWindow 1:1 funktioniert.
    /// </summary>
    public sealed class AppWatcher : IDisposable
    {
        private readonly string[] _targetProcessNames;

        private Win32.WinEventDelegate? _winEventDelegate;
        private IntPtr _hookHandle;
        private IntPtr _lastAppHwnd;

        // PID -> (isTarget, expiry). EVENT_SYSTEM_FOREGROUND feuert bei JEDEM
        // Alt+Tab und jedem Mausklick auf ein anderes Top-Level-Window — beim
        // Arbeiten zwischen Browser, Editor und Claude kommen 10-50 Events pro
        // Minute. Ohne Cache macht jeder Event einen Process.GetProcessById-
        // Aufruf (OpenProcess + QueryFullProcessImageName + Dispose), je
        // 0.5-1.5 ms inkl. Handle-Allokation. Mit 1-Sekunde-TTL bleibt die
        // Reaktion auf neu gestartete Ziel-Apps sofort (1x reinklicken = die
        // Cache-Miss-Latenz). PID-Reuse-Risiko: Windows recycelt PIDs nicht
        // binnen 1 s, daher praktisch unsichtbar.
        private static readonly ConcurrentDictionary<uint, (bool IsTarget, long ExpiryTicks)> _pidCache = new();
        private const long PidCacheTtlTicks = TimeSpan.TicksPerSecond * 1; // 1 s

        public event Action<IntPtr>? AppActivated;
        public event Action? AppDeactivated;

        public IntPtr ActiveAppHwnd => _lastAppHwnd;

        /// <summary>
        /// Poll-Variante zu den EVENT_SYSTEM_FOREGROUND-Events: prueft das
        /// AKTUELLE Vordergrundfenster und liefert dessen HWND, wenn es zu
        /// einer Ziel-App gehoert — sonst IntPtr.Zero. Grundlage fuer den
        /// selbstheilenden Sichtbarkeits-Poll im Overlay: ein verpasstes
        /// Foreground-Event oder ein kurz aufpoppendes Fremdfenster (z.B. ein
        /// Notifier-Toast, das AppDeactivated ausloest) darf das Overlay nicht
        /// dauerhaft verschwinden lassen, solange die Ziel-App real vorne ist.
        /// </summary>
        public IntPtr GetForegroundTargetAppHwnd()
        {
            var hwnd = Win32.GetForegroundWindow();
            if (hwnd == IntPtr.Zero || !IsTargetAppWindow(hwnd)) return IntPtr.Zero;
            _lastAppHwnd = hwnd;
            return hwnd;
        }

        public AppWatcher(string[]? processNames = null)
        {
            _targetProcessNames = processNames ?? new[] { "Claude", "Codex", "ChatGPT" };
        }

        public void Start()
        {
            DiagLog.Write("AppWatcher", "starting", ("targets", string.Join(",", _targetProcessNames)));

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
            {
                Console.WriteLine("AppWatcher: SetWinEventHook fehlgeschlagen");
                DiagLog.Write("AppWatcher", "hook_failed");
            }

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
            if (IsTargetAppWindow(hwnd))
            {
                _lastAppHwnd = hwnd;
                DiagLog.Write("AppWatcher", "target_foreground", ("hwnd", $"0x{hwnd.ToInt64():X}"));
                Application.Current?.Dispatcher.BeginInvoke(() => AppActivated?.Invoke(hwnd));
            }
            else
            {
                Application.Current?.Dispatcher.BeginInvoke(() => AppDeactivated?.Invoke());
            }
        }

        private bool IsTargetAppWindow(IntPtr hwnd)
        {
            Win32.GetWindowThreadProcessId(hwnd, out uint pid);
            if (pid == 0) return false;

            // Cache-Lookup. Treffer + nicht abgelaufen -> direkt zurueck, kein
            // Win32-Roundtrip. Der Cache-Miss-Pfad erfasst auch das negative
            // Ergebnis (NICHT-Ziel-Prozesse) — das ist der haeufigste Pfad
            // (Browser, Editor) und profitiert am meisten vom Cache.
            long now = DateTime.UtcNow.Ticks;
            if (_pidCache.TryGetValue(pid, out var cached) && cached.ExpiryTicks > now)
                return cached.IsTarget;

            bool isTarget = false;
            try
            {
                using var proc = Process.GetProcessById((int)pid);
                var name = proc.ProcessName;
                foreach (var target in _targetProcessNames)
                {
                    if (name.Equals(target, StringComparison.OrdinalIgnoreCase))
                    {
                        isTarget = true;
                        DiagLog.Write("AppWatcher", "process_matched", ("process", name), ("pid", pid), ("target", target));
                        break;
                    }
                }
            }
            catch
            {
                // Process may have exited — Default-Fall: NICHT-Ziel.
            }

            // Cache schreiben — auch negative Ergebnisse. Ein simpler Sweep
            // beim Schreiben begrenzt das Wachstum auf ein paar hundert
            // Eintraege selbst nach Stunden — kein dediziertes LRU noetig.
            _pidCache[pid] = (isTarget, now + PidCacheTtlTicks);
            if (_pidCache.Count > 256)
            {
                foreach (var kv in _pidCache)
                {
                    if (kv.Value.ExpiryTicks <= now)
                        _pidCache.TryRemove(kv.Key, out _);
                }
            }
            return isTarget;
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
