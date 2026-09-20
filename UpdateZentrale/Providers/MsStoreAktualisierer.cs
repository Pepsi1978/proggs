using System.Diagnostics;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// MSIX/Store apps. winget lists them without a source, so "winget upgrade" reports nothing even
/// when the Store has a newer build. The installed version therefore comes from Get-AppxPackage,
/// and the update is handed to the Store via its package family name.
/// </summary>
public sealed class MsStoreAktualisierer : IAktualisierer
{
    public string Art => "msstore";

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var befehl = "-NoProfile -NonInteractive -Command \"(Get-AppxPackage -Name '" + eintrag.AppxName
                   + "' | Sort-Object Version | Select-Object -Last 1).Version\"";
        protokoll.Report("powershell " + befehl);

        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", befehl, TimeSpan.FromMinutes(2), abbruch: abbruch);
        var version = lauf.Ausgabe.Trim();
        protokoll.Report(version);

        if (string.IsNullOrWhiteSpace(version))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: "Store-App nicht installiert.", Protokoll: lauf.Ausgabe);

        // The Store exposes no queryable "latest version" per package, so this stays honest instead
        // of guessing: show what is installed and offer the Store page.
        return new PruefErgebnis(UpdateZustand.Unbekannt, version, "",
            "Der Store verwaltet die Updates - die Schaltflaeche oeffnet die Store-Seite.", lauf.Ausgabe);
    }

    public Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        try
        {
            // Opens the Store straight at this package; the Store does the actual download.
            Process.Start(new ProcessStartInfo
            {
                FileName = "ms-windows-store://pdp/?PFN=" + eintrag.PackageFamilyName,
                UseShellExecute = true
            });
            protokoll.Report("Store geoeffnet: " + eintrag.PackageFamilyName);
            return Task.FromResult(new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: "Microsoft Store geoeffnet - dort auf 'Aktualisieren' klicken."));
        }
        catch (Exception ex)
        {
            return Task.FromResult(new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "Store liess sich nicht oeffnen: " + ex.Message));
        }
    }
}
