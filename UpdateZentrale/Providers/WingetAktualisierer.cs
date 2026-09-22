using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// winget packages. The CLI has no JSON output for "list", so the table is parsed by the column
/// positions of its header row -- splitting on whitespace breaks on names like "LM Studio 0.4.24+1".
/// </summary>
public sealed class WingetAktualisierer : IAktualisierer
{
    public string Art => "winget";

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        if (!File.Exists(Pfade.Winget))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "winget wurde nicht gefunden.");

        var args = $"list --id {eintrag.WingetId} --exact --disable-interactivity --accept-source-agreements";
        protokoll.Report($"winget {args}");
        var lauf = await Kommandozeile.AusfuehrenAsync(Pfade.Winget, args, TimeSpan.FromMinutes(3), abbruch: abbruch);
        protokoll.Report(lauf.Ausgabe);

        return ListeAuswerten(lauf, eintrag.WingetId ?? "");
    }

    /// <summary>winget's exit code for "no installed package matches" (measured with v1.29).</summary>
    internal const int KeinPaketGefunden = unchecked((int)0x8A150014);

    /// <summary>winget's exit code for "installed, no applicable upgrade".</summary>
    internal const int KeinAnwendbaresUpgrade = unchecked((int)0x8A15002B);

    /// <summary>
    /// Process failures first: a timed-out or failed run has no package row either, and used to
    /// read as "not installed". Only winget's own "no package found" answer means that.
    /// </summary>
    internal static PruefErgebnis ListeAuswerten(BefehlErgebnis lauf, string id)
    {
        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "winget hat das Zeitlimit überschritten – der Stand ist unbekannt.", Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode == KeinPaketGefunden
            || (lauf.ExitCode == 0 && MeldetKeinPaket(lauf.Ausgabe)))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert,
                Meldung: "Nicht über winget installiert.", Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode != 0)
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "winget endete mit Code " + Code(lauf.ExitCode) + " – der Stand ist unbekannt.", Protokoll: lauf.Ausgabe);

        var zeile = TabellenZeile(lauf.Ausgabe, id);
        if (zeile is null)
            // A clean run without the package row means the output shape changed, not that the
            // program is missing -- saying "not installed" there would be a lie.
            return new PruefErgebnis(UpdateZustand.Unbekannt,
                Meldung: "winget-Ausgabe war nicht lesbar – siehe Protokoll.", Protokoll: lauf.Ausgabe);

        var (installiert, verfuegbar) = zeile.Value;
        if (string.IsNullOrWhiteSpace(installiert))
            return new PruefErgebnis(UpdateZustand.Unbekannt,
                Meldung: "winget nannte keine installierte Version – siehe Protokoll.", Protokoll: lauf.Ausgabe);

        var zustand = string.IsNullOrWhiteSpace(verfuegbar) || verfuegbar == installiert
            ? UpdateZustand.Aktuell
            : UpdateZustand.UpdateVerfuegbar;

        return new PruefErgebnis(zustand, installiert, verfuegbar,
            zustand == UpdateZustand.Aktuell ? "Auf dem neuesten Stand." : $"Neue Version {verfuegbar} verfügbar.",
            lauf.Ausgabe);
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        if (!File.Exists(Pfade.Winget))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "winget wurde nicht gefunden.");

        var args = $"upgrade --id {eintrag.WingetId} --exact --silent --disable-interactivity "
                 + "--accept-source-agreements --accept-package-agreements";
        protokoll.Report($"winget {args}");

        var lauf = await Kommandozeile.AusfuehrenAsync(
            Pfade.Winget, args, TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), abbruch: abbruch);
        protokoll.Report(lauf.Ausgabe);

        if (Kommandozeile.UnsauberesEnde(lauf) is { } unsauber) return unsauber;

        // Nothing to do is not a failure: the card must say "current", not show a red band.
        if (IstNichtsZuTun(lauf))
            return new PruefErgebnis(UpdateZustand.Aktuell, Meldung: "War bereits aktuell – kein Upgrade nötig.", Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode != 0)
        {
            var grund = StoreAktualisierer.BrauchtRechte(lauf.Ausgabe)
                ? "Dafür fehlen Administratorrechte – oben auf „Als Administrator neu starten“ klicken."
                : "winget endete mit Code " + Code(lauf.ExitCode) + ".";
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: grund, Protokoll: lauf.Ausgabe);
        }

        // Some installers (Claude Desktop) only stage the update and swap it in on next launch.
        if (lauf.Ausgabe.Contains("Restart the application", StringComparison.OrdinalIgnoreCase)
            || lauf.Ausgabe.Contains("Starten Sie die Anwendung neu", StringComparison.OrdinalIgnoreCase))
        {
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: "Installiert – wird beim nächsten Start von " + eintrag.Name + " übernommen.",
                Protokoll: lauf.Ausgabe, ErstNachNeustart: true);
        }

        return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update installiert.", Protokoll: lauf.Ausgabe);
    }

    /// <summary>
    /// The installed version. For packaged apps Get-AppxPackage is both faster and more truthful
    /// than the winget table, because it reads the registration rather than a correlation.
    /// </summary>
    public async Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
    {
        if (!string.IsNullOrWhiteSpace(eintrag.AppxName))
        {
            var appx = StoreAktualisierer.AppxAuswerten(
                await Kommandozeile.AusfuehrenAsync("powershell.exe", StoreAktualisierer.AppxBefehl(eintrag.AppxName),
                    TimeSpan.FromMinutes(2), abbruch: abbruch));
            if (!appx.Erfolg) Protokollierung.Schreiben(eintrag.Id, "Fingerabdruck unbekannt: " + appx.Problem);
            return appx.Version;
        }

        if (!File.Exists(Pfade.Winget)) return "";

        var args = $"list --id {eintrag.WingetId} --exact --disable-interactivity --accept-source-agreements";
        var lauf = await Kommandozeile.AusfuehrenAsync(Pfade.Winget, args, TimeSpan.FromMinutes(3), abbruch: abbruch);
        var (wert, problem) = ListenFingerabdruck(lauf, eintrag.WingetId ?? "");
        if (problem is not null) Protokollierung.Schreiben(eintrag.Id, "Fingerabdruck unbekannt: " + problem);
        return wert;
    }

    /// <summary>The installed version from a list run -- only from a clean run, never a guess.</summary>
    internal static (string Fingerabdruck, string? Problem) ListenFingerabdruck(BefehlErgebnis lauf, string id)
    {
        if (lauf.Abgelaufen) return ("", "winget list hat das Zeitlimit überschritten.");
        if (lauf.ExitCode == KeinPaketGefunden) return ("", "winget findet das Paket nicht.");
        if (lauf.ExitCode != 0) return ("", "winget list endete mit Code " + Code(lauf.ExitCode) + ".");
        var installiert = TabellenZeile(lauf.Ausgabe, id)?.Installiert ?? "";
        return string.IsNullOrWhiteSpace(installiert) ? ("", "winget list lieferte keine lesbare Version.") : (installiert, null);
    }

    /// <summary>winget's "installed, nothing newer" answer to an upgrade, in code or in words.</summary>
    internal static bool IstNichtsZuTun(BefehlErgebnis lauf)
        => !lauf.Abgelaufen && !lauf.Abgebrochen && lauf.BeendenProblem is null
           && (lauf.ExitCode == KeinAnwendbaresUpgrade
               || lauf.Ausgabe.Contains("No applicable upgrade", StringComparison.OrdinalIgnoreCase)
               || lauf.Ausgabe.Contains("No available upgrade", StringComparison.OrdinalIgnoreCase)
               || lauf.Ausgabe.Contains("Kein anwendbares Upgrade", StringComparison.OrdinalIgnoreCase));

    private static bool MeldetKeinPaket(string ausgabe)
        => ausgabe.Contains("No installed package found", StringComparison.OrdinalIgnoreCase)
           || ausgabe.Contains("Es wurde kein installiertes Paket", StringComparison.OrdinalIgnoreCase);

    /// <summary>winget codes are HRESULTs; hex is what its documentation and issues use.</summary>
    internal static string Code(int exitCode) => exitCode < 0 ? "0x" + exitCode.ToString("X8") : exitCode.ToString();

    /// <summary>
    /// Finds the package row and returns (installed, available).
    ///
    /// Header column offsets are not usable: winget pads columns by display width, and a name
    /// that is itself version-like ("LM Studio 0.4.24+1") pushes the row out of alignment.
    /// Anchoring on the exact package id and reading the fields behind it survives that, and
    /// works regardless of the display language.
    /// </summary>
    private static (string Installiert, string Verfuegbar)? TabellenZeile(string ausgabe, string id)
    {
        if (string.IsNullOrWhiteSpace(id)) return null;

        foreach (var zeile in ausgabe.Split('\n'))
        {
            var stelle = zeile.IndexOf(id, StringComparison.OrdinalIgnoreCase);
            if (stelle < 0) continue;
            if (zeile.TrimStart().StartsWith("Name", StringComparison.OrdinalIgnoreCase)) continue;

            var rest = zeile[(stelle + id.Length)..]
                .Split(' ', StringSplitOptions.RemoveEmptyEntries)
                .ToList();

            // Trailing source column ("winget", "msstore", "MSIX\..."): not a version.
            while (rest.Count > 0 && !SiehtNachVersionAus(rest[^1])) rest.RemoveAt(rest.Count - 1);

            return rest.Count switch
            {
                0 => ("", ""),
                1 => (rest[0], ""),
                _ => (rest[0], rest[1])
            };
        }
        return null;
    }

    /// <summary>A version field always starts with a digit; "winget" and "Unknown" do not.</summary>
    private static bool SiehtNachVersionAus(string feld)
        => feld.Length > 0 && char.IsDigit(feld[0]);
}
