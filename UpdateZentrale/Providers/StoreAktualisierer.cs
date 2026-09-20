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
        var installiert = await AppxVersionAsync(eintrag, protokoll, abbruch);
        if (string.IsNullOrWhiteSpace(installiert))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: "Nicht installiert.");

        if (!File.Exists(Pfade.Winget))
            return new PruefErgebnis(UpdateZustand.Unbekannt, installiert, "", "winget wurde nicht gefunden.");

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

        var zeile = lauf.Ausgabe.Split('\n').FirstOrDefault(z =>
            kennungen.Any(k => z.Contains(k, StringComparison.OrdinalIgnoreCase)));

        if (zeile is null)
        {
            protokoll.Report("Kein Eintrag in der Upgrade-Liste – nichts offen.");
            return new PruefErgebnis(UpdateZustand.Aktuell, installiert, "", "Auf dem neuesten Stand.");
        }

        protokoll.Report(zeile.Trim());
        var treffer = kennungen.First(k => zeile.Contains(k, StringComparison.OrdinalIgnoreCase));
        var verfuegbar = VerfuegbareVersion(zeile, treffer);
        return new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, installiert, verfuegbar,
            string.IsNullOrWhiteSpace(verfuegbar) ? "Update verfügbar." : "Neue Version " + verfuegbar + " verfügbar.");
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
            || lauf.Ausgabe.Contains("No available upgrade", StringComparison.OrdinalIgnoreCase))
        {
            var stand = await AppxVersionAsync(eintrag, protokoll, abbruch);
            return new PruefErgebnis(UpdateZustand.Aktuell, stand, "", "War bereits aktuell.", lauf.Ausgabe);
        }

        if (lauf.ExitCode != 0)
        {
            var grund = BrauchtRechte(lauf.Ausgabe)
                ? "Dafür fehlen Administratorrechte – oben auf „Als Administrator neu starten“ klicken."
                : "winget endete mit Code " + lauf.ExitCode + ".";
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: grund, Protokoll: lauf.Ausgabe);
        }

        var neu = await AppxVersionAsync(eintrag, protokoll, abbruch);
        return new PruefErgebnis(UpdateZustand.Fertig, neu, "", "Update installiert.", lauf.Ausgabe);
    }

    /// <summary>The registered package version -- read straight from Windows, no network.</summary>
    public async Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
        => await AppxVersionAsync(eintrag, new Progress<string>(_ => { }), abbruch);

    internal static bool BrauchtRechte(string ausgabe)
        => ausgabe.Contains("0x80073d28", StringComparison.OrdinalIgnoreCase)
           || ausgabe.Contains("Administratorrechte", StringComparison.OrdinalIgnoreCase)
           || ausgabe.Contains("elevation", StringComparison.OrdinalIgnoreCase);

    private static async Task<string> AppxVersionAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var befehl = "-NoProfile -NonInteractive -Command \"(Get-AppxPackage -Name '" + eintrag.AppxName
                   + "' | Sort-Object Version | Select-Object -Last 1).Version\"";
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", befehl, TimeSpan.FromMinutes(2), abbruch: abbruch);
        var version = lauf.Ausgabe.Trim();
        protokoll.Report("Installiertes Paket: " + (string.IsNullOrWhiteSpace(version) ? "(nicht gefunden)" : version));
        return version;
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
