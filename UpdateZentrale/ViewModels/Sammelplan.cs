namespace UpdateZentrale.ViewModels;

/// <summary>
/// The order of a batch run, decided in one place. The UI has two independent buttons -- the
/// CLI-critical updates and everything else -- and each runs its part of this order:
/// <list type="number">
/// <item>Own tools (repo scripts): the UpdateZentrale itself first -- it restarts the app and the
/// new instance continues the run -- then OpenLauncher, TVO, CVO. They ask yes/no, and right
/// after the click the user is still at the screen.</item>
/// <item>CLI-critical updates (PowerShell, Node, Git, Windows Terminal, JDK …): they can end
/// running CLI sessions; they belong to the CLI button.</item>
/// <item>Everything else.</item>
/// </list>
/// Within a phase an entry whose prerequisites ("nach") are in the same run goes after them;
/// otherwise the shortest expected update goes first, so the long installers (Visual Studio,
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
    {
        var liste = offen.Where(p => !p.IstHauptversionsSprung).ToList();
        var ids = new HashSet<string>(liste.Select(p => p.Eintrag.Id), StringComparer.OrdinalIgnoreCase);
        // One level is enough for the catalog's chains (JDK → SDK, Node → npm tools).
        bool Wartet(ProgrammViewModel p) => p.Eintrag.Nach.Any(ids.Contains);

        return liste
            // A step that waits for another one in the run never runs beside it in the parallel lane.
            .Select(p => new Schritt(p, p.Eintrag.Zuletzt ? PhaseZuletzt : PhaseVon(p), DarfParallel(p) && !p.Eintrag.Zuletzt && !Wartet(p)))
            .OrderBy(s => s.Phase)
            .ThenBy(s => s.Programm.Eintrag.Id.Equals(SelbstId, StringComparison.OrdinalIgnoreCase) ? 0 : 1)
            .ThenBy(s => Wartet(s.Programm) ? 1 : 0)
            .ThenBy(s => s.Programm.ErwarteteDauerSekunden)
            .ThenBy(s => s.Programm.KatalogIndex)
            .ToList();
    }

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
