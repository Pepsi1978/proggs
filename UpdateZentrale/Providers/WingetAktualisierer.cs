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
            return new PruefErgebnis(UpdateZustand.NichtInstalliert,
                Meldung: "Nicht ueber winget installiert.", Protokoll: lauf.Ausgabe);
        }

        var (installiert, verfuegbar) = zeile.Value;
        var zustand = string.IsNullOrWhiteSpace(verfuegbar) || verfuegbar == installiert
            ? UpdateZustand.Aktuell
            : UpdateZustand.UpdateVerfuegbar;

        return new PruefErgebnis(zustand, installiert, verfuegbar,
            zustand == UpdateZustand.Aktuell ? "Auf dem neuesten Stand." : $"Neue Version {verfuegbar} verfuegbar.",
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
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Zeitlimit ueberschritten.", Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode != 0)
        {
            var grund = lauf.Ausgabe.Contains("No applicable upgrade", StringComparison.OrdinalIgnoreCase)
                ? "Kein Upgrade verfuegbar."
                : $"winget endete mit Code {lauf.ExitCode}.";
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: grund, Protokoll: lauf.Ausgabe);
        }

        return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update installiert.", Protokoll: lauf.Ausgabe);
    }

    /// <summary>Finds the package row and returns (installed, available) using header column offsets.</summary>
    private static (string Installiert, string Verfuegbar)? TabellenZeile(string ausgabe, string id)
    {
        var zeilen = ausgabe.Split('\n');
        var kopfIndex = Array.FindIndex(zeilen, z =>
            (z.Contains("Version", StringComparison.OrdinalIgnoreCase)) &&
            (z.TrimStart().StartsWith("Name", StringComparison.OrdinalIgnoreCase)));
        if (kopfIndex < 0) return null;

        var kopf = zeilen[kopfIndex];
        var spaltenVersion = kopf.IndexOf("Version", StringComparison.OrdinalIgnoreCase);
        // Column after Version is "Available"/"Verfuegbar" depending on the display language.
        var spaltenVerfuegbar = kopf.IndexOf("Available", StringComparison.OrdinalIgnoreCase);
        if (spaltenVerfuegbar < 0) spaltenVerfuegbar = kopf.IndexOf("Verf", spaltenVersion + 1, StringComparison.OrdinalIgnoreCase);
        var spaltenQuelle = kopf.IndexOf("Source", StringComparison.OrdinalIgnoreCase);
        if (spaltenQuelle < 0) spaltenQuelle = kopf.IndexOf("Quelle", StringComparison.OrdinalIgnoreCase);

        foreach (var zeile in zeilen.Skip(kopfIndex + 1))
        {
            if (!zeile.Contains(id, StringComparison.OrdinalIgnoreCase)) continue;

            var installiert = Ausschnitt(zeile, spaltenVersion, spaltenVerfuegbar);
            var verfuegbar = spaltenVerfuegbar < 0 ? "" : Ausschnitt(zeile, spaltenVerfuegbar, spaltenQuelle);
            return (installiert, verfuegbar);
        }
        return null;
    }

    private static string Ausschnitt(string zeile, int von, int bis)
    {
        if (von < 0 || von >= zeile.Length) return "";
        var ende = bis < 0 || bis > zeile.Length ? zeile.Length : bis;
        if (ende <= von) ende = zeile.Length;
        return zeile[von..ende].Trim();
    }
}
