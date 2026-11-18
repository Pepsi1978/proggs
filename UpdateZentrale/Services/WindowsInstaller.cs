namespace UpdateZentrale.Services;

/// <summary>
/// Windows Installer (msiexec) runs as a service, independent of the tool that started it: winget
/// may die (0xC000013A, 18.11.2026) while the MSI keeps installing. Callers wait for it before they
/// read the installed state. See bugs/bug-cases/2026-11-18-updatezentrale-winget-0xC000013A-*.
/// </summary>
public static class WindowsInstaller
{
    /// <summary>Upper bound of one wait: a stuck or foreign transaction must not block a card for long.</summary>
    public static readonly TimeSpan HoechsteWartezeit = TimeSpan.FromMinutes(5);

    /// <summary>Test seam: whether a Windows Installer transaction is running right now.</summary>
    internal static Func<bool> Laeuft { get; set; } = TransaktionLaeuft;

    /// <summary>Test seam: poll pause while waiting.</summary>
    internal static TimeSpan Pause { get; set; } = TimeSpan.FromSeconds(2);

    /// <summary>
    /// msiexec HOLDS the global mutex _MSIExecute during an install transaction; the idle service may
    /// keep it open without holding it. So: missing = idle, takeable at once = idle (released again),
    /// held = busy. Without the right to open it, nothing can be told apart -- busy, but every wait
    /// is bounded by <see cref="HoechsteWartezeit"/>.
    /// </summary>
    private static bool TransaktionLaeuft()
    {
        Mutex? mutex = null;
        try
        {
            if (!Mutex.TryOpenExisting(@"Global\_MSIExecute", out mutex)) return false;
            bool frei;
            try
            {
                frei = mutex.WaitOne(0);
            }
            catch (AbandonedMutexException)
            {
                frei = true;   // acquired: its owner is gone
            }
            if (frei) mutex.ReleaseMutex();
            return !frei;
        }
        catch (UnauthorizedAccessException)
        {
            return true;
        }
        catch (Exception ex) when (ex is System.IO.IOException or ArgumentException or ApplicationException or WaitHandleCannotBeOpenedException)
        {
            return false;
        }
        finally
        {
            mutex?.Dispose();
        }
    }

    /// <summary>Waits until no installer transaction runs, at most min(grenze, HoechsteWartezeit). true = idle.</summary>
    public static async Task<bool> AbwartenAsync(TimeSpan grenze, CancellationToken abbruch)
    {
        if (grenze > HoechsteWartezeit) grenze = HoechsteWartezeit;
        var uhr = System.Diagnostics.Stopwatch.StartNew();
        while (SicherLaeuft())
        {
            if (uhr.Elapsed >= grenze) return false;
            await Task.Delay(Pause, abbruch);
        }
        return true;
    }

    /// <summary>A failing probe must never block: unknown counts as idle.</summary>
    public static bool SicherLaeuft()
    {
        try { return Laeuft(); }
        catch (Exception ex)
        {
            Diagnose.Gefangen(ex, "msi", Schwere.Warnung);
            return false;
        }
    }
}
