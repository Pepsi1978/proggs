using System.IO;
using System.Diagnostics;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>A running process as plain values -- no OS handle travels with it.</summary>
public sealed record ProzessInfo(int Pid, string Name, string Pfad, DateTime? Startzeit);

/// <param name="Gefunden">Target processes found before closing.</param>
/// <param name="NochLaufend">Target processes still running after close and tree kill.</param>
/// <param name="Problem">What went wrong while ending them, if anything.</param>
public sealed record BeendenErgebnis(int Gefunden, IReadOnlyList<ProzessInfo> NochLaufend, string? Problem)
{
    public bool Erfolgreich => NochLaufend.Count == 0;
}

/// <summary>
/// Knows which of a program's processes are alive. NSIS/Squirrel installers hang silently when the
/// target is running, so every update path asks here first.
/// </summary>
public static class Prozessdienst
{
    /// <summary>
    /// Snapshot of the entry's live processes. Every Process object created here is disposed
    /// before returning; callers get values only, so no handle ownership leaks out.
    /// </summary>
    public static IReadOnlyList<ProzessInfo> Laufende(ProgrammEintrag eintrag)
    {
        var treffer = new List<ProzessInfo>();
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
                using (kandidat)
                {
                    var pfad = PfadVon(kandidat);
                    if (PasstZumEintrag(pfad, eintrag, erwarteterOrdner))
                        treffer.Add(new ProzessInfo(kandidat.Id, name, pfad, StartzeitVon(kandidat)));
                }
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
    private static bool PasstZumEintrag(string pfad, ProgrammEintrag eintrag, string? erwarteterOrdner)
    {
        // Packaged apps live under WindowsApps in a folder that carries the version, so the path
        // moves with every update. The publisher hash from the package family name does not --
        // a substring test is intended here.
        if (eintrag.PaketKennung is { } kennung)
            return pfad.Contains(kennung, StringComparison.OrdinalIgnoreCase);

        // No folder known: the catalog names the exact process, and that name is the contract.
        if (erwarteterOrdner is null) return true;

        if (string.IsNullOrEmpty(pfad)) return false;   // Unreadable path: never assume a match.

        return LiegtIn(pfad, erwarteterOrdner);
    }

    /// <summary>
    /// True if the file lies in the folder or below it -- compared at a real directory boundary.
    /// A plain prefix test let "C:\Tools\App" claim "C:\Tools\Application\x.exe".
    /// </summary>
    internal static bool LiegtIn(string pfad, string ordner)
    {
        if (string.IsNullOrWhiteSpace(pfad) || string.IsNullOrWhiteSpace(ordner)) return false;
        try
        {
            var datei = Path.GetFullPath(pfad).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
            var basis = Path.GetFullPath(ordner).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
            return datei.Equals(basis, StringComparison.OrdinalIgnoreCase)
                   || datei.StartsWith(basis + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase);
        }
        catch
        {
            return false;   // Not a valid path: no match.
        }
    }

    private static string? OrdnerVon(ProgrammEintrag eintrag)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
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

    private static DateTime? StartzeitVon(Process prozess)
    {
        try { return prozess.StartTime; }
        catch { return null; }
    }

    public static bool Laeuft(ProgrammEintrag eintrag) => Laufende(eintrag).Count > 0;

    /// <summary>
    /// Closes gracefully first, kills only what refuses to go within the grace period, then
    /// checks what is really left. Each step re-reads the live list (with the path check), so a
    /// reused PID can never make it kill a foreign process.
    /// </summary>
    public static async Task<BeendenErgebnis> BeendenAsync(ProgrammEintrag eintrag, CancellationToken abbruch = default)
    {
        var anfangs = Laufende(eintrag);

        // Closing or killing needs a proven identity: a package id or an install folder, both
        // checked against the readable process path. A bare name may do for the "running"
        // display, never for ending a process -- a same-named foreign one would be hit.
        if (!HatSichereIdentitaet(eintrag))
        {
            return new BeendenErgebnis(anfangs.Count, anfangs, anfangs.Count == 0 ? null
                : "Keine sichere Zuordnung: Für " + eintrag.Name + " sind weder Installationsordner noch Paketkennung "
                  + "bekannt, deshalb wird kein Prozess nur anhand des Namens beendet.");
        }

        var probleme = new List<string>();

        foreach (var info in anfangs)
            MitProzess(info, p => p.CloseMainWindow(), probleme, "Schließen");

        try { await Task.Delay(2500, abbruch); } catch (OperationCanceledException) { }

        foreach (var info in Laufende(eintrag))
            MitProzess(info, p => { p.Kill(entireProcessTree: true); p.WaitForExit(5000); }, probleme, "Beenden");

        try { await Task.Delay(800, abbruch); } catch (OperationCanceledException) { }

        var rest = Laufende(eintrag);
        if (rest.Count > 0)
            probleme.Add("Läuft weiter: " + string.Join(", ", rest.Select(r => r.Name + " (PID " + r.Pid + ")")));

        return new BeendenErgebnis(anfangs.Count, rest, probleme.Count == 0 ? null : string.Join(" ", probleme));
    }

    /// <summary>True when processes can be tied to this entry by more than their name.</summary>
    internal static bool HatSichereIdentitaet(ProgrammEintrag eintrag)
        => eintrag.PaketKennung is not null || OrdnerVon(eintrag) is not null;

    /// <summary>
    /// Re-opens the process by PID and acts only if it is provably still the same one: both start
    /// times readable and equal. Unknown or different start time means a reused PID cannot be ruled
    /// out -- nothing is touched and that is reported. A process that is already gone is fine; any
    /// other failure is recorded, never swallowed.
    /// </summary>
    internal static void MitProzess(ProzessInfo info, Action<Process> aktion, List<string> probleme, string schritt)
    {
        try
        {
            using var p = Process.GetProcessById(info.Pid);
            var jetzt = StartzeitVon(p);
            if (info.Startzeit is not { } start || jetzt is null || jetzt.Value != start)
            {
                probleme.Add(schritt + " von " + info.Name + " (PID " + info.Pid + ") nicht ausgeführt: "
                             + (info.Startzeit is null || jetzt is null
                                 ? "Startzeit nicht lesbar"
                                 : "Startzeit weicht ab (PID wurde neu vergeben)")
                             + " – die Identität ist nicht gesichert.");
                return;
            }
            aktion(p);
        }
        catch (ArgumentException) { }            // gone
        catch (InvalidOperationException) { }    // exited meanwhile
        catch (Exception ex)
        {
            probleme.Add(schritt + " von " + info.Name + " (PID " + info.Pid + ") fehlgeschlagen: " + ex.Message);
        }
    }

    public static bool Starten(ProgrammEintrag eintrag, bool alsAdministrator)
    {
        // A packaged app cannot be launched by its exe path (the WindowsApps folder is locked
        // down); it is started through the apps folder instead. Windows also refuses to run
        // packaged apps elevated at all, so the admin flag is irrelevant here.
        if (eintrag.IstPaketApp) return PaketAppStarten(eintrag);

        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
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
            using var _ = Process.Start(start);   // only the start matters; release the handle at once
            return true;
        }
        catch
        {
            return false;   // User dismissed the UAC prompt, or the exe vanished.
        }
    }

    private static bool PaketAppStarten(ProgrammEintrag eintrag)
    {
        var ziel = "shell:AppsFolder\\" + eintrag.PackageFamilyName + "!" + (eintrag.AppxAnwendungsId ?? "App");
        try
        {
            using var _ = Process.Start(new ProcessStartInfo
            {
                FileName = "explorer.exe",
                Arguments = ziel,
                UseShellExecute = true
            });
            return true;
        }
        catch
        {
            return false;
        }
    }
}
