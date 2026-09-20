using System.Diagnostics;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// MSIX apps. winget lists them without a source, so "winget upgrade" reports nothing even when a
/// newer build exists -- the installed version therefore comes from Get-AppxPackage.
///
/// Two flavours: packages the Microsoft Store maintains (update handed to the Store), and
/// packages that ship their own updater inside the app (Codex Desktop). The catalog flag
/// selbstAktualisierend decides which one applies.
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
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: "Nicht installiert.", Protokoll: lauf.Ausgabe);

        // Neither the Store nor the in-app updater exposes a queryable "latest version", so this
        // stays honest instead of guessing: show what is installed and offer the right action.
        var meldung = eintrag.SelbstAktualisierend
            ? "Aktualisiert sich selbst – die Schaltfläche öffnet die App."
            : "Der Store verwaltet die Updates – die Schaltfläche öffnet die Store-Seite.";

        return new PruefErgebnis(UpdateZustand.Unbekannt, version, "", meldung, lauf.Ausgabe);
    }

    public Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        return Task.FromResult(eintrag.SelbstAktualisierend
            ? AppOeffnen(eintrag, protokoll)
            : StoreOeffnen(eintrag, protokoll));
    }

    /// <summary>Launches the packaged app through the apps folder, using its package family name.</summary>
    private static PruefErgebnis AppOeffnen(ProgrammEintrag eintrag, IProgress<string> protokoll)
    {
        var ziel = "shell:AppsFolder\\" + eintrag.PackageFamilyName + "!" + (eintrag.AppxAnwendungsId ?? "App");
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = "explorer.exe",
                Arguments = ziel,
                UseShellExecute = true
            });
            protokoll.Report("Geöffnet: " + ziel);
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: eintrag.Name + " wurde geöffnet – das Update läuft in der App selbst.");
        }
        catch (Exception ex)
        {
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Die App ließ sich nicht öffnen: " + ex.Message);
        }
    }

    private static PruefErgebnis StoreOeffnen(ProgrammEintrag eintrag, IProgress<string> protokoll)
    {
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = "ms-windows-store://pdp/?PFN=" + eintrag.PackageFamilyName,
                UseShellExecute = true
            });
            protokoll.Report("Store geöffnet: " + eintrag.PackageFamilyName);
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: "Microsoft Store geöffnet – dort auf 'Aktualisieren' klicken.");
        }
        catch (Exception ex)
        {
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Der Store ließ sich nicht öffnen: " + ex.Message);
        }
    }
}
