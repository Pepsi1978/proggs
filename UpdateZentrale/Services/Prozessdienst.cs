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
        var erwarteterOrdner = OrdnerVon(eintrag);

        foreach (var name in eintrag.AlleProzesse)
        {
            Process[] kandidaten;
            try
            {
                kandidaten = Process.GetProcessesByName(name);
            }
            catch
            {
                continue;
            }

            foreach (var kandidat in kandidaten)
            {
                if (PasstZumEintrag(kandidat, eintrag, erwarteterOrdner)) treffer.Add(kandidat);
            }
        }
        return treffer;
    }

    /// <summary>
    /// Process names collide across entries -- the Claude Code CLI and the Claude desktop app are
    /// both called "claude", and Windows matches names case-insensitively. Without a path check a
    /// desktop update would offer to kill running CLI sessions, so a known install folder always
    /// wins over the bare name.
    /// </summary>
    private static bool PasstZumEintrag(Process prozess, ProgrammEintrag eintrag, string? erwarteterOrdner)
    {
        if (erwarteterOrdner is null) return true;

        var pfad = PfadVon(prozess);
        if (string.IsNullOrEmpty(pfad)) return false;   // Unreadable path: never assume a match.

        return pfad.StartsWith(erwarteterOrdner, StringComparison.OrdinalIgnoreCase);
    }

    private static string? OrdnerVon(ProgrammEintrag eintrag)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfad);
        if (string.IsNullOrWhiteSpace(exe)) return null;

        var ordner = Path.GetDirectoryName(exe);
        return string.IsNullOrWhiteSpace(ordner) ? null : ordner;
    }

    private static string PfadVon(Process prozess)
    {
        try
        {
            return prozess.MainModule?.FileName ?? "";
        }
        catch
        {
            // Access denied for elevated or protected processes -- treated as "unknown path".
            return "";
        }
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
