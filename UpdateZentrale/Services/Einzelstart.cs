using System.Diagnostics;
using System.Runtime.InteropServices;

namespace UpdateZentrale.Services;

/// <summary>
/// A named system-wide lock that is really HELD, not merely existing.
///
/// The old check -- new Mutex(initiallyOwned: true, name, out createdNew) -- only asked whether
/// the kernel object existed. While the object still exists but is no longer owned -- between
/// the old instance's ReleaseMutex and Dispose, or while another handle keeps it open -- a new
/// start saw "exists" and quit. (When a process ends, Windows closes its handles; without any
/// other handle the object disappears, so a crash alone does not block forever.) Waiting on the
/// mutex asks the right question, and an abandoned one simply becomes ours.
///
/// Ownership rule: a Mutex belongs to the thread that acquired it. Beanspruchen and Freigeben
/// must run on the same thread (the app does both on the UI thread: OnStartup and Exit).
/// </summary>
public sealed class Einzelinstanz : IDisposable
{
    private readonly string _name;
    private Mutex? _sperre;

    public Einzelinstanz(string name) => _name = name;

    public bool Besitzt { get; private set; }

    /// <returns>
    /// true: this instance holds the lock. false: another instance holds it (also when an
    /// elevated instance's mutex cannot even be opened). null: the mutex itself failed -- the
    /// caller decides with a fallback instead of silently allowing a second instance.
    /// </returns>
    public bool? Beanspruchen(TimeSpan wartezeit)
    {
        if (Besitzt) return true;   // repeated call: idempotent

        try
        {
            _sperre ??= new Mutex(initiallyOwned: false, _name);
            try
            {
                Besitzt = _sperre.WaitOne(wartezeit);
            }
            catch (AbandonedMutexException)
            {
                Besitzt = true;   // previous owner died without releasing -- ours now
            }

            if (!Besitzt) SperreSchliessen();
            return Besitzt;
        }
        catch (UnauthorizedAccessException)
        {
            // Created by an elevated instance; a non-elevated one may not open it -- "running".
            SperreSchliessen();
            return false;
        }
        catch
        {
            SperreSchliessen();
            return null;
        }
    }

    /// <summary>Releases only what this instance holds; safe to call repeatedly.</summary>
    public void Freigeben()
    {
        if (Besitzt)
        {
            try { _sperre?.ReleaseMutex(); }
            catch (ApplicationException) { /* not owned by this thread -- the OS frees it at exit */ }
            Besitzt = false;
        }
        SperreSchliessen();
    }

    public void Dispose() => Freigeben();

    private void SperreSchliessen()
    {
        _sperre?.Dispose();
        _sperre = null;
    }
}

/// <summary>
/// The handoff from a running instance to its elevated successor. The old instance keeps the
/// lock through the UAC prompt (Process.Start with "runas" blocks until it is answered) and
/// only then shuts down; the successor, told the old PID, waits for exactly that process.
/// </summary>
public static class Uebernahme
{
    public const string Schalter = "--uebernahme";

    /// <summary>The internal start argument for the successor.</summary>
    public static string Argument(int pid) => Schalter + " " + pid;

    /// <returns>The predecessor PID -- only for exactly "--uebernahme &lt;positive int&gt;", never the own PID.</returns>
    public static int? PidAus(IReadOnlyList<string> argumente, int eigenePid)
    {
        if (argumente.Count != 2 || !string.Equals(argumente[0], Schalter, StringComparison.Ordinal)) return null;
        if (!int.TryParse(argumente[1], System.Globalization.NumberStyles.None,
                System.Globalization.CultureInfo.InvariantCulture, out var pid)) return null;
        return pid > 0 && pid != eigenePid ? pid : null;
    }

    /// <summary>
    /// Waits, bounded, for the predecessor to exit -- but only if that PID really is this same
    /// program. A dead PID, a foreign program or an unreadable path cost no waiting at all.
    /// </summary>
    /// <returns>true if nothing (any more) needs waiting for; false if it was still running at the limit.</returns>
    public static bool AufEndeWarten(int pid, string eigeneExe, TimeSpan grenze)
    {
        Process vorgaenger;
        try { vorgaenger = Process.GetProcessById(pid); }
        catch (ArgumentException) { return true; }   // already gone

        using (vorgaenger)
        {
            if (!Prozesspfad.GleicheDatei(Prozesspfad.Lesen(pid), eigeneExe)) return true;   // foreign: don't wait
            try { return vorgaenger.WaitForExit((int)grenze.TotalMilliseconds); }
            catch (InvalidOperationException) { return true; }
        }
    }
}

/// <summary>
/// Keeps the app to a single instance. Two windows would write the same settings.json and the
/// same log files, and two update runs could collide on the same installer -- so a second start
/// brings the existing window forward instead of opening another one.
/// </summary>
public static class Einzelstart
{
    private const string GlobalerName = @"Global\UpdateZentrale.Einzelstart";
    private const string LokalerName = @"Local\UpdateZentrale.Einzelstart";

    private static Einzelinstanz? _instanz;

    private static readonly TimeSpan NormaleWartezeit = TimeSpan.FromSeconds(1);
    private static readonly TimeSpan UebernahmeGrenze = TimeSpan.FromSeconds(20);
    private static readonly TimeSpan NachUebernahme = TimeSpan.FromSeconds(3);

    /// <summary>True when this process may continue; false when another instance already runs.</summary>
    public static bool Beanspruchen(IReadOnlyList<string>? argumente = null)
    {
        if (_instanz is { Besitzt: true }) return true;
        _instanz?.Freigeben();   // a stale wrapper from an earlier failed attempt

        var eigeneExe = Rechte.EigeneExe;
        var wartezeit = NormaleWartezeit;   // absorbs "closed and reopened at once"
        if (Uebernahme.PidAus(argumente ?? Array.Empty<string>(), Environment.ProcessId) is { } vorgaenger)
        {
            Uebernahme.AufEndeWarten(vorgaenger, eigeneExe, UebernahmeGrenze);
            wartezeit = NachUebernahme;
        }

        _instanz = Entscheiden(new Einzelinstanz(GlobalerName), new Einzelinstanz(LokalerName), wartezeit,
            () => AndereInstanz("UpdateZentrale", eigeneExe, Environment.ProcessId) is not null);
        return _instanz is not null;
    }

    /// <summary>
    /// Fail-closed decision: the app only continues while it HOLDS a lock.
    /// <list type="bullet">
    /// <item>Global acquired → continue; global taken → stop.</item>
    /// <item>Global technically failed → the local lock, but only if it is acquired AND no other
    /// running copy of this exe exists (that one may hold the global lock this process cannot
    /// see). Otherwise the local lock is released again → stop.</item>
    /// <item>Both technically failed → stop. Never a run without a held lock.</item>
    /// </list>
    /// Every wrapper that does not win is released, so no handle stays behind.
    /// </summary>
    /// <returns>The held lock, or null when this instance must not start.</returns>
    internal static Einzelinstanz? Entscheiden(Einzelinstanz global, Einzelinstanz lokal, TimeSpan wartezeit,
                                              Func<bool> andereInstanzLaeuft)
    {
        var g = global.Beanspruchen(wartezeit);
        if (g == true)
        {
            lokal.Freigeben();
            return global;
        }
        global.Freigeben();
        if (g == false)
        {
            lokal.Freigeben();
            return null;
        }

        if (lokal.Beanspruchen(wartezeit) != true)
        {
            lokal.Freigeben();
            return null;
        }
        if (andereInstanzLaeuft())
        {
            lokal.Freigeben();
            return null;
        }
        return lokal;
    }

    public static void Freigeben()
    {
        _instanz?.Freigeben();
        _instanz = null;
    }

    /// <summary>
    /// Another running process of exactly this exe -- the name alone is not enough, a foreign
    /// program may carry it too. Every Process object is disposed, also on an early hit.
    /// </summary>
    internal static int? AndereInstanz(string prozessname, string eigeneExe, int eigenePid)
    {
        Process[] alle;
        try { alle = Process.GetProcessesByName(prozessname); }
        catch { return null; }

        try
        {
            foreach (var prozess in alle)
            {
                if (prozess.Id == eigenePid) continue;
                if (Prozesspfad.GleicheDatei(Prozesspfad.Lesen(prozess.Id), eigeneExe)) return prozess.Id;
            }
            return null;
        }
        finally
        {
            foreach (var prozess in alle) prozess.Dispose();
        }
    }

    /// <summary>
    /// Brings the window of the instance that is already running to the front. Restoring it first
    /// matters: a minimised window reports success from SetForegroundWindow without ever becoming
    /// visible. Only a window of this very exe is touched; an unreadable path means no activation.
    /// </summary>
    public static void VorhandenesFensterZeigen()
    {
        try
        {
            if (AndereInstanz("UpdateZentrale", Rechte.EigeneExe, Environment.ProcessId) is not { } pid) return;

            using var prozess = Process.GetProcessById(pid);
            var fenster = prozess.MainWindowHandle;
            if (fenster == IntPtr.Zero) return;

            if (IsIconic(fenster)) ShowWindow(fenster, SwRestore);
            SetForegroundWindow(fenster);
        }
        catch
        {
            // Gone meanwhile or window not reachable (UIPI towards an elevated window): nothing to show.
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
