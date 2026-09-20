using System.IO;
using System.Diagnostics;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Knows which of a program's processes are alive. NSIS/Squirrel installers hang silently when the
/// target is running, so every update path asks here first.
/// </summary>
public static class Prozessdienst
{
    public static IReadOnlyList<Process> Laufende(ProgrammEintrag eintrag)
    {
        var treffer = new List<Process>();
        foreach (var name in eintrag.AlleProzesse)
        {
            try
            {
                treffer.AddRange(Process.GetProcessesByName(name));
            }
            catch
            {
                // A process can die between enumeration and access -- never fatal for a check.
            }
        }
        return treffer;
    }

    public static bool Laeuft(ProgrammEintrag eintrag) => Laufende(eintrag).Count > 0;

    /// <summary>Closes gracefully first, kills only what refuses to go within the grace period.</summary>
    public static async Task<int> BeendenAsync(ProgrammEintrag eintrag, CancellationToken abbruch = default)
    {
        var prozesse = Laufende(eintrag);
        foreach (var p in prozesse)
        {
            try { p.CloseMainWindow(); } catch { }
        }

        try { await Task.Delay(2500, abbruch); } catch (OperationCanceledException) { }

        foreach (var p in Laufende(eintrag))
        {
            try { p.Kill(entireProcessTree: true); } catch { }
        }

        try { await Task.Delay(800, abbruch); } catch (OperationCanceledException) { }
        return prozesse.Count;
    }

    public static bool Starten(ProgrammEintrag eintrag, bool alsAdministrator)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfad);
        if (string.IsNullOrWhiteSpace(exe) || !File.Exists(exe)) return false;

        var start = new ProcessStartInfo
        {
            FileName = exe,
            Arguments = eintrag.StartArgumente ?? "",
            WorkingDirectory = Path.GetDirectoryName(exe) ?? "",
            UseShellExecute = true
        };
        if (alsAdministrator) start.Verb = "runas";

        try
        {
            Process.Start(start);
            return true;
        }
        catch
        {
            return false;   // User dismissed the UAC prompt, or the exe vanished.
        }
    }
}
