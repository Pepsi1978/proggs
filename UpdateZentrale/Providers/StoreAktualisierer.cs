using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Store-signed MSIX packages, updated silently -- no Store window, no "open the app".
///
/// winget does not list these packages under `list --source msstore`, but `upgrade --id
/// &lt;ProductId&gt; --source msstore` does resolve them, which is the whole automation. Checking goes
/// through the plain `winget upgrade` table, because that lists pending updates without
/// installing anything.
/// </summary>
public sealed class StoreAktualisierer : IAktualisierer
{
    /// <summary>winget's exit code for "installed, but nothing newer available".</summary>
    private const int KeinUpgradeVerfuegbar = unchecked((int)0x8A150067);

    public string Art => "store";

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var appx = await AppxVersionAsync(eintrag, protokoll, abbruch);
        if (!appx.Erfolg)
            // A failed query is not "not installed": nothing is known about the package.
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "Die Paketabfrage ist fehlgeschlagen – der Stand ist unbekannt. " + appx.Problem);
        if (string.IsNullOrWhiteSpace(appx.Version))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: "Nicht installiert.");

        if (!File.Exists(Pfade.Winget))
            return new PruefErgebnis(UpdateZustand.Unbekannt, appx.Version, "", "winget wurde nicht gefunden.");

        // The upgrade table lists what is pending without touching anything.
        var args = "upgrade --include-unknown --disable-interactivity --accept-source-agreements";
        protokoll.Report("winget " + args);
        var lauf = await Kommandozeile.AusfuehrenAsync(Pfade.Winget, args, TimeSpan.FromMinutes(5), abbruch: abbruch);

        // winget labels store rows inconsistently -- sometimes the product id, sometimes the
        // package name or the MSIX full name. Matching any of them avoids silently reporting
        // "up to date" just because the label changed.
        var kennungen = new[] { eintrag.StoreProduktId, eintrag.AppxName, eintrag.PaketKennung }
            .Where(k => !string.IsNullOrWhiteSpace(k))
            .Select(k => k!)
            .ToArray();

        var ergebnis = UpgradeListeAuswerten(lauf, kennungen, appx.Version);
        protokoll.Report(ergebnis.Meldung);
        return ergebnis;
    }

    /// <summary>
    /// Only a clean listing may say "nothing pending". A run that timed out or failed lists
    /// nothing either -- that used to come out as "up to date".
    /// </summary>
    internal static PruefErgebnis UpgradeListeAuswerten(BefehlErgebnis lauf, string[] kennungen, string installiert)
    {
        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler, installiert, "",
                "Die Upgrade-Liste hat das Zeitlimit überschritten – ob ein Update offen ist, ist unbekannt.", lauf.Ausgabe);

        // "No installed package found" is winget's answer for an empty upgrade list.
        if (lauf.ExitCode == WingetAktualisierer.KeinPaketGefunden)
            return new PruefErgebnis(UpdateZustand.Aktuell, installiert, "", "Kein Eintrag in der Upgrade-Liste – nichts offen.", lauf.Ausgabe);

        if (lauf.ExitCode != 0)
            return new PruefErgebnis(UpdateZustand.Fehler, installiert, "",
                "Die Upgrade-Liste endete mit Code " + WingetAktualisierer.Code(lauf.ExitCode)
                + " – ob ein Update offen ist, ist unbekannt.", lauf.Ausgabe);

        var zeile = lauf.Ausgabe.Split('\n').FirstOrDefault(z =>
            kennungen.Any(k => z.Contains(k, StringComparison.OrdinalIgnoreCase)));

        if (zeile is null)
            return new PruefErgebnis(UpdateZustand.Aktuell, installiert, "", "Kein Eintrag in der Upgrade-Liste – nichts offen.", lauf.Ausgabe);

        var treffer = kennungen.First(k => zeile.Contains(k, StringComparison.OrdinalIgnoreCase));
        var verfuegbar = VerfuegbareVersion(zeile, treffer);
        return new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, installiert, verfuegbar,
            string.IsNullOrWhiteSpace(verfuegbar) ? "Update verfügbar: " + zeile.Trim() : "Neue Version " + verfuegbar + " verfügbar.",
            lauf.Ausgabe);
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        if (!File.Exists(Pfade.Winget))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "winget wurde nicht gefunden.");

        var args = "upgrade --id " + eintrag.StoreProduktId + " --source msstore --silent --disable-interactivity"
                 + " --accept-source-agreements --accept-package-agreements";
        protokoll.Report("winget " + args);

        var lauf = await Kommandozeile.AusfuehrenAsync(
            Pfade.Winget, args, TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), abbruch: abbruch);
        protokoll.Report(lauf.Ausgabe);

        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Zeitlimit überschritten.", Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode == KeinUpgradeVerfuegbar
            || WingetAktualisierer.IstNichtsZuTun(lauf))
        {
            var stand = (await AppxVersionAsync(eintrag, protokoll, abbruch)).Version;
            return new PruefErgebnis(UpdateZustand.Aktuell, stand, "", "War bereits aktuell.", lauf.Ausgabe);
        }

        if (lauf.ExitCode != 0)
        {
            var grund = BrauchtRechte(lauf.Ausgabe)
                ? "Dafür fehlen Administratorrechte – oben auf „Als Administrator neu starten“ klicken."
                : "winget endete mit Code " + lauf.ExitCode + ".";
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: grund, Protokoll: lauf.Ausgabe);
        }

        var neu = (await AppxVersionAsync(eintrag, protokoll, abbruch)).Version;
        return new PruefErgebnis(UpdateZustand.Fertig, neu, "", "Update installiert.", lauf.Ausgabe);
    }

    /// <summary>
    /// The registered package version -- read straight from Windows, no network. A failed query
    /// gives no fingerprint at all (logged), never an empty "not installed" that would compare
    /// as a change.
    /// </summary>
    public async Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
    {
        var appx = await AppxVersionAsync(eintrag, new Progress<string>(_ => { }), abbruch);
        if (!appx.Erfolg) Protokollierung.Schreiben(eintrag.Id, "Fingerabdruck unbekannt: " + appx.Problem);
        return appx.Version;
    }

    internal static bool BrauchtRechte(string ausgabe)
        => ausgabe.Contains("0x80073d28", StringComparison.OrdinalIgnoreCase)
           || ausgabe.Contains("Administratorrechte", StringComparison.OrdinalIgnoreCase)
           || ausgabe.Contains("elevation", StringComparison.OrdinalIgnoreCase);

    /// <summary>Success and "installed version" kept apart: empty after exit 0 means not installed.</summary>
    internal readonly record struct AppxAbfrage(bool Erfolg, string Version, string? Problem);

    internal static string AppxBefehl(string? appxName)
        => "-NoProfile -NonInteractive -Command \"(Get-AppxPackage -Name '" + appxName
           + "' | Sort-Object Version | Select-Object -Last 1).Version\"";

    private static readonly System.Text.RegularExpressions.Regex AppxVersionsMuster =
        new(@"^\d+(\.\d+){1,3}$", System.Text.RegularExpressions.RegexOptions.Compiled);

    /// <summary>
    /// stderr is merged into the output, so a successful exit code alone does not make the text a
    /// version: anything but a bare version number is a failed query.
    /// </summary>
    internal static AppxAbfrage AppxAuswerten(BefehlErgebnis lauf)
    {
        if (lauf.Abgelaufen) return new AppxAbfrage(false, "", "Get-AppxPackage hat das Zeitlimit überschritten.");
        if (lauf.ExitCode != 0) return new AppxAbfrage(false, "", "Get-AppxPackage endete mit Code " + lauf.ExitCode + ".");

        var text = lauf.Ausgabe.Trim();
        if (text.Length == 0) return new AppxAbfrage(true, "", null);
        return AppxVersionsMuster.IsMatch(text)
            ? new AppxAbfrage(true, text, null)
            : new AppxAbfrage(false, "", "Get-AppxPackage lieferte keine Versionsnummer: " + text);
    }

    private static async Task<AppxAbfrage> AppxVersionAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", AppxBefehl(eintrag.AppxName),
            TimeSpan.FromMinutes(2), abbruch: abbruch);
        var abfrage = AppxAuswerten(lauf);
        protokoll.Report("Installiertes Paket: " + (!abfrage.Erfolg
            ? "(Abfrage fehlgeschlagen: " + abfrage.Problem + ")"
            : string.IsNullOrWhiteSpace(abfrage.Version) ? "(nicht gefunden)" : abfrage.Version));
        return abfrage;
    }

    /// <summary>Reads the "Available" column: the field after the id that starts with a digit.</summary>
    private static string VerfuegbareVersion(string zeile, string id)
    {
        var stelle = zeile.IndexOf(id, StringComparison.OrdinalIgnoreCase);
        if (stelle < 0) return "";

        var felder = zeile[(stelle + id.Length)..]
            .Split(' ', StringSplitOptions.RemoveEmptyEntries)
            .Where(f => f.Length > 0 && char.IsDigit(f[0]))
            .ToList();

        // Version, then Available -- the latter is what the user cares about here.
        return felder.Count >= 2 ? felder[1] : felder.FirstOrDefault() ?? "";
    }
}
