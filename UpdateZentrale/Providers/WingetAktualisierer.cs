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

        var zeile = TabellenZeile(lauf.Ausgabe, eintrag.WingetId ?? "");
        if (zeile is null)
        {
            // The id appearing without a parsable table means the output shape changed, not that
            // the program is missing -- saying "not installed" there would be a lie.
            var kenntPaket = lauf.Ausgabe.Contains(eintrag.WingetId ?? "\u0000", StringComparison.OrdinalIgnoreCase);
            return kenntPaket
                ? new PruefErgebnis(UpdateZustand.Unbekannt,
                    Meldung: "winget-Ausgabe war nicht lesbar – siehe Protokoll.", Protokoll: lauf.Ausgabe)
                : new PruefErgebnis(UpdateZustand.NichtInstalliert,
                    Meldung: "Nicht über winget installiert.", Protokoll: lauf.Ausgabe);
        }

        var (installiert, verfuegbar) = zeile.Value;
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

        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Zeitlimit überschritten.", Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode != 0)
        {
            var grund = lauf.Ausgabe.Contains("No applicable upgrade", StringComparison.OrdinalIgnoreCase)
                ? "Kein Upgrade verfügbar."
                : StoreAktualisierer.BrauchtRechte(lauf.Ausgabe)
                    ? "Dafür fehlen Administratorrechte – oben auf „Als Administrator neu starten“ klicken."
                    : $"winget endete mit Code {lauf.ExitCode}.";
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
