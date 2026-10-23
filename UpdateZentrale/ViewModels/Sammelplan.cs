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

    /// <summary>Entries marked "zuletzt" run after every phase -- their own step at the very end.</summary>
    public const int PhaseZuletzt = 4;

    public static List<Schritt> Planen(IEnumerable<ProgrammViewModel> offen)
        => offen
            .Where(p => !p.IstHauptversionsSprung)
            .Select(p => new Schritt(p, p.Eintrag.Zuletzt ? PhaseZuletzt : PhaseVon(p), DarfParallel(p) && !p.Eintrag.Zuletzt))
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
    /// Only entries the catalog marks "parallel" run beside the serial lane (see
    /// <see cref="Models.ProgrammEintrag.Parallel"/>). Serial stays everything that shares a lock, a
    /// tree or a runtime -- winget/MSI/Store, npm, the helper scripts -- and anything that asks
    /// yes/no or can end a CLI session, whatever the catalog says.
    /// </summary>
    public static bool DarfParallel(ProgrammViewModel p)
        => p.Eintrag.Parallel && !p.KannCliBeenden
           && (p.Eintrag.Art.Equals("cli", StringComparison.OrdinalIgnoreCase)
               || p.Eintrag.Art.Equals("opencodesitzung", StringComparison.OrdinalIgnoreCase));

    public static string PhasenName(int phase) => phase switch
    {
        PhaseEigene => "Eigene Werkzeuge",
        PhaseCli => "CLI-kritisch",
        PhaseZuletzt => "Zum Schluss",
        _ => "Übrige"
    };
}
