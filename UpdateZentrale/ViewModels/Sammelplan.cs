namespace UpdateZentrale.ViewModels;

/// <summary>
/// The order of "install all", decided in one place:
/// <list type="number">
/// <item>Own tools (repo scripts): the UpdateZentrale itself first -- it restarts the app and the
/// new instance continues the batch -- then OpenLauncher, TVO, CVO. They ask yes/no, and right
/// after the click on "install all" the user is still at the screen.</item>
/// <item>CLI-critical updates (PowerShell, Node, Git, Windows Terminal …): they can end running CLI
/// sessions, so they come early -- once they are through, work in the CLI can go on undisturbed.</item>
/// <item>Everything else.</item>
/// </list>
/// Within a phase the shortest expected update goes first, so the long installers (Visual Studio,
/// Android Studio, Docker) do not hold the quick ones back. Major version jumps marked
/// "hauptversionNurPerKlick" are left out entirely.
/// </summary>
public static class Sammelplan
{
    public const int PhaseEigene = 1;
    public const int PhaseCli = 2;
    public const int PhaseRest = 3;

    public const string SelbstId = "updatezentrale";

    public sealed record Schritt(ProgrammViewModel Programm, int Phase, bool Parallel);

    public static List<Schritt> Planen(IEnumerable<ProgrammViewModel> offen)
        => offen
            .Where(p => !p.IstHauptversionsSprung)
            .Select(p => new Schritt(p, PhaseVon(p), DarfParallel(p)))
            .OrderBy(s => s.Programm.Eintrag.Zuletzt ? 1 : 0)
            .ThenBy(s => s.Phase)
            .ThenBy(s => s.Programm.Eintrag.Id.Equals(SelbstId, StringComparison.OrdinalIgnoreCase) ? 0 : 1)
            .ThenBy(s => s.Programm.ErwarteteDauerSekunden)
            .ThenBy(s => s.Programm.KatalogIndex)
            .ToList();

    public static int PhaseVon(ProgrammViewModel p)
        => p.Eintrag.Art.Equals("reposkript", StringComparison.OrdinalIgnoreCase) ? PhaseEigene
            : p.KannCliBeenden ? PhaseCli
            : PhaseRest;

    /// <summary>
    /// Independent self-updaters may run beside the serial lane: they touch only their own
    /// folder. Serial stays everything that shares a lock or a tree -- winget/MSI/Store (one
    /// installer at a time), npm (one global node_modules), the PowerShell helper scripts (they
    /// rewrite the user PATH) and anything that asks yes/no or can end a CLI session.
    /// </summary>
    public static bool DarfParallel(ProgrammViewModel p)
    {
        var e = p.Eintrag;
        if (p.KannCliBeenden) return false;
        if (e.Art.Equals("opencodesitzung", StringComparison.OrdinalIgnoreCase)) return true;
        if (!e.Art.Equals("cli", StringComparison.OrdinalIgnoreCase)) return false;
        var befehl = (e.UpdateExePfad ?? "") + " " + (e.UpdateArgumente ?? "");
        return !befehl.Contains("npm", StringComparison.OrdinalIgnoreCase)
               && !befehl.Contains("pwsh", StringComparison.OrdinalIgnoreCase)
               && !befehl.Contains("powershell", StringComparison.OrdinalIgnoreCase);
    }

    public static string PhasenName(int phase) => phase switch
    {
        PhaseEigene => "Eigene Werkzeuge",
        PhaseCli => "CLI-kritisch",
        _ => "Übrige"
    };
}
