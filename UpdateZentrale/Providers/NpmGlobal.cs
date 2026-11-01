using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Schuetzt global per npm installierte Kommandozeilen-Werkzeuge vor einem abgebrochenen Update.
/// npm raeumt das alte Paket samt Shim weg, BEVOR es das neue laedt. Endet der Lauf in dieser
/// Luecke hart (Zeitlimit, Abbruch), bleibt kein Rollback: das Werkzeug fehlt. Deshalb liegt der
/// langsame Teil vorn, wo ein Abbruch nichts zerstoert, und eine zerstoerte Installation wird
/// sofort wiederhergestellt. Siehe bugs/bug-cases/2026-11-01-updatezentrale-npm-kill-zerstoert-cli.md.
/// </summary>
internal static class NpmGlobal
{
    /// <summary>Liegt die Programmdatei direkt im globalen npm-Ordner? Native Installationen bleiben aussen vor.</summary>
    internal static bool Betrifft(ProgrammEintrag eintrag, string programmDatei, string globalOrdner)
    {
        if (string.IsNullOrWhiteSpace(eintrag.NpmPaket) || string.IsNullOrWhiteSpace(programmDatei)) return false;
        var ordner = Path.GetDirectoryName(programmDatei);
        return ordner is not null
               && string.Equals(Path.TrimEndingDirectorySeparator(ordner), Path.TrimEndingDirectorySeparator(globalOrdner),
                                StringComparison.OrdinalIgnoreCase);
    }

    internal static string GlobalOrdner
        => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "npm");

    /// <summary>Der npm-Start selbst: der global aktualisierte, sonst der von Node.js mitgebrachte.</summary>
    private static string? NpmDatei()
    {
        var kandidaten = new[]
        {
            Path.Combine(GlobalOrdner, "npm.cmd"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "nodejs", "npm.cmd")
        };
        return kandidaten.FirstOrDefault(File.Exists);
    }

    /// <summary>
    /// Waechst, solange npm laedt: cacache schreibt eine Tarball laufend nach _cacache\tmp. Macht das
    /// Zeitlimit zur Ruhezeit, ein langsamer, aber laufender Download wird nicht mehr abgeschossen.
    /// </summary>
    private static long DownloadGroesse()
    {
        var ordner = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "npm-cache", "_cacache", "tmp");
        return Directory.Exists(ordner)
            ? new DirectoryInfo(ordner).EnumerateFiles("*", SearchOption.AllDirectories).Sum(datei => datei.Length)
            : 0;
    }

    /// <summary>Argumente des Vorab-Downloads: nur in den npm-Cache, die globale Installation bleibt unberuehrt.</summary>
    internal static string VorabArgumente(string paket, string zielOrdner)
        => "install --prefix \"" + zielOrdner + "\" " + paket + "@latest"
           + " --ignore-scripts --no-audit --no-fund --no-package-lock --no-bin-links --loglevel=error";

    /// <summary>
    /// Laedt die neue Version in den npm-Cache, solange die alte noch unangetastet ist.
    /// Null = weiter mit dem Update; sonst das Ergebnis, das den Lauf beendet.
    /// </summary>
    internal static async Task<PruefErgebnis?> VorabLadenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var npm = NpmDatei();
        if (npm is null) return null;

        var ziel = Path.Combine(Path.GetTempPath(), "uz-npmvorab-" + Guid.NewGuid().ToString("N")[..8]);
        Directory.CreateDirectory(ziel);
        try
        {
            protokoll.Report("Lädt " + eintrag.NpmPaket + " vorab in den npm-Cache, die installierte Version bleibt dabei unberührt …");
            var lauf = await Kommandozeile.AusfuehrenAsync(npm, VorabArgumente(eintrag.NpmPaket!, ziel),
                TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), Path.GetDirectoryName(npm), abbruch: abbruch, alsAufrufer: true,
                lebenszeichen: DownloadGroesse);

            if (lauf.Abgebrochen || lauf.BeendenProblem is not null) return Kommandozeile.UnsauberesEnde(lauf);
            if (lauf.Abgelaufen || CliAktualisierer.IstNetzfehler(lauf))
            {
                protokoll.Report(lauf.Ausgabe);
                return new PruefErgebnis(UpdateZustand.Fehler,
                    Meldung: CliAktualisierer.LangsamesNetzMeldung + eintrag.Name + " ließ sich nicht vorab laden. "
                             + "Die Verbindung ist gerade zu langsam oder gestört – später erneut versuchen. "
                             + "Die installierte Version bleibt unverändert nutzbar.",
                    Protokoll: lauf.Ausgabe);
            }
            if (lauf.ExitCode != 0)
                protokoll.Report("Vorab-Download endete mit Code " + lauf.ExitCode + ", das Update läuft ohne ihn weiter: " + lauf.Ausgabe);
            return null;
        }
        finally
        {
            try { await Task.Run(() => Directory.Delete(ziel, recursive: true)); }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
            {
                Diagnose.Gefangen(ex, "provider", Schwere.Info);
            }
        }
    }

    /// <summary>
    /// Stellt ein vom abgebrochenen Update zerstoertes Werkzeug wieder her. Laeuft auch nach einem
    /// Klick auf Abbrechen zu Ende: ein fehlendes Werkzeug ist schlimmer als die kurze Wartezeit.
    /// </summary>
    /// <returns>True, wenn die Programmdatei danach wieder da ist.</returns>
    internal static async Task<bool> WiederherstellenAsync(ProgrammEintrag eintrag, string programmDatei, IProgress<string> protokoll)
    {
        var npm = NpmDatei();
        if (npm is null)
        {
            protokoll.Report("Wiederherstellung nicht möglich: npm.cmd wurde nicht gefunden.");
            return false;
        }

        protokoll.Report(Path.GetFileName(programmDatei) + " fehlt nach dem abgebrochenen npm-Lauf – stelle " + eintrag.NpmPaket + " wieder her …");
        var lauf = await Kommandozeile.AusfuehrenAsync(npm, "install -g " + eintrag.NpmPaket + "@latest --no-audit --no-fund",
            TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), Path.GetDirectoryName(npm), abbruch: CancellationToken.None, alsAufrufer: true,
            lebenszeichen: DownloadGroesse);
        protokoll.Report(lauf.Ausgabe);
        return !lauf.Abgelaufen && lauf.ExitCode == 0 && File.Exists(programmDatei);
    }
}
