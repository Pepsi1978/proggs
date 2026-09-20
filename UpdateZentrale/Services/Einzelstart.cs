using System.Diagnostics;
using System.Runtime.InteropServices;

namespace UpdateZentrale.Services;

/// <summary>
/// Keeps the app to a single instance. Two windows would write the same settings.json and the
/// same log files, and two update runs could collide on the same installer -- so a second start
/// brings the existing window forward instead of opening another one.
/// </summary>
public static class Einzelstart
{
    private const string Name = @"Global\UpdateZentrale.Einzelstart";

    private static Mutex? _sperre;

    /// <summary>True when this process may continue; false when another instance already runs.</summary>
    public static bool Beanspruchen()
    {
        try
        {
            _sperre = new Mutex(initiallyOwned: true, Name, out var neu);
            if (neu) return true;
        }
        catch (UnauthorizedAccessException)
        {
            // The running instance is elevated and this one is not -- also "already running".
        }
        catch
        {
            return true;   // Never block the app over a failing mutex.
        }

        _sperre = null;
        return false;
    }

    public static void Freigeben()
    {
        try
        {
            _sperre?.ReleaseMutex();
            _sperre?.Dispose();
        }
        catch
        {
        }
        _sperre = null;
    }

    /// <summary>
    /// Brings the window of the instance that is already running to the front. Restoring it first
    /// matters: a minimised window reports success from SetForegroundWindow without ever becoming
    /// visible.
    /// </summary>
    public static void VorhandenesFensterZeigen()
    {
        try
        {
            var eigene = Environment.ProcessId;
            foreach (var prozess in Process.GetProcessesByName("UpdateZentrale"))
            {
                if (prozess.Id == eigene) continue;

                var fenster = prozess.MainWindowHandle;
                if (fenster == IntPtr.Zero) continue;

                if (IsIconic(fenster)) ShowWindow(fenster, SwRestore);
                SetForegroundWindow(fenster);
                return;
            }
        }
        catch
        {
        }
    }

    private const int SwRestore = 9;

    [DllImport("user32.dll")]
    private static extern bool SetForegroundWindow(IntPtr fenster);

    [DllImport("user32.dll")]
    private static extern bool ShowWindow(IntPtr fenster, int befehl);

    [DllImport("user32.dll")]
    private static extern bool IsIconic(IntPtr fenster);
}
