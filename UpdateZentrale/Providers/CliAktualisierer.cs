using System.IO;
using System.Text.RegularExpressions;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Command line tools that update themselves ("claude update", "codex update",
/// "lms runtime update"). Availability comes from one of two sources, whichever the catalog
/// entry provides: a dry-run subcommand, or the npm registry for tools published there.
/// </summary>
public sealed class CliAktualisierer : IAktualisierer
{
    public string Art => "cli";

    private static readonly Regex VersionsMuster = new(@"\d+\.\d+\.\d+[\w.+-]*", RegexOptions.Compiled);

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfad);
        if (!File.Exists(exe))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: $"Nicht gefunden: {exe}");

        var installiert = "";
        if (!string.IsNullOrWhiteSpace(eintrag.VersionsArgumente))
        {
            var lauf = await Kommandozeile.AusfuehrenAsync(exe, eintrag.VersionsArgumente, TimeSpan.FromMinutes(2), abbruch: abbruch);
            installiert = VersionsMuster.Match(lauf.Ausgabe).Value;
            protokoll.Report($"{Path.GetFileName(exe)} {eintrag.VersionsArgumente} -> {lauf.Ausgabe}");
        }

        // Path 1: the tool can tell us itself what it would update.
        if (!string.IsNullOrWhiteSpace(eintrag.PruefArgumente))
        {
            protokoll.Report($"{Path.GetFileName(exe)} {eintrag.PruefArgumente}");
            var lauf = await Kommandozeile.AusfuehrenAsync(
                exe, eintrag.PruefArgumente, TimeSpan.FromMinutes(6), abbruch: abbruch);
            protokoll.Report(lauf.Ausgabe);

            var geplant = lauf.Ausgabe
                .Split('\n')
                .Where(z => z.Contains('→') || z.Contains("->"))
                .Select(z => z.Trim())
                .ToList();

            if (geplant.Count > 0)
            {
                return new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, installiert,
                    $"{geplant.Count} Paket(e)",
                    geplant.Count == 1 ? geplant[0] : $"{geplant.Count} Aktualisierungen geplant.",
                    lauf.Ausgabe);
            }
            return new PruefErgebnis(UpdateZustand.Aktuell, installiert, "", "Alles auf dem neuesten Stand.", lauf.Ausgabe);
        }

        // Path 2: compare against the npm registry, which carries the same version line.
        if (!string.IsNullOrWhiteSpace(eintrag.NpmPaket))
        {
            var verfuegbar = await NpmVersionAsync(eintrag.NpmPaket, protokoll, abbruch);
            if (!string.IsNullOrWhiteSpace(verfuegbar))
            {
                var neuer = Vergleiche(verfuegbar, installiert) > 0;
                return new PruefErgebnis(neuer ? UpdateZustand.UpdateVerfuegbar : UpdateZustand.Aktuell,
                    installiert, verfuegbar,
                    neuer ? $"Neue Version {verfuegbar} verfuegbar." : "Auf dem neuesten Stand.");
            }
        }

        return new PruefErgebnis(UpdateZustand.Unbekannt, installiert, "",
            "Keine Pruefquelle hinterlegt - Update laesst sich trotzdem ausloesen.");
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfad);
        if (!File.Exists(exe))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: $"Nicht gefunden: {exe}");

        var args = eintrag.UpdateArgumente ?? "update";
        protokoll.Report($"{Path.GetFileName(exe)} {args}");

        var lauf = await Kommandozeile.AusfuehrenAsync(
            exe, args, TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), abbruch: abbruch);
        protokoll.Report(lauf.Ausgabe);

        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Zeitlimit ueberschritten.", Protokoll: lauf.Ausgabe);
        if (lauf.ExitCode != 0)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: $"Endete mit Code {lauf.ExitCode}.", Protokoll: lauf.Ausgabe);

        return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update abgeschlossen.", Protokoll: lauf.Ausgabe);
    }

    private static async Task<string> NpmVersionAsync(string paket, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var npm = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "npm", "npm.cmd");
        if (!File.Exists(npm)) npm = "npm.cmd";

        var lauf = await Kommandozeile.AusfuehrenAsync(npm, $"view {paket} version", TimeSpan.FromMinutes(2), abbruch: abbruch);
        protokoll.Report($"npm view {paket} version -> {lauf.Ausgabe}");
        return VersionsMuster.Match(lauf.Ausgabe).Value;
    }

    /// <summary>Numeric component compare; returns &gt;0 when a is newer than b.</summary>
    private static int Vergleiche(string a, string b)
    {
        if (string.IsNullOrWhiteSpace(a)) return -1;
        if (string.IsNullOrWhiteSpace(b)) return 1;

        var links = Teile(a);
        var rechts = Teile(b);
        for (var i = 0; i < Math.Max(links.Length, rechts.Length); i++)
        {
            var l = i < links.Length ? links[i] : 0;
            var r = i < rechts.Length ? rechts[i] : 0;
            if (l != r) return l.CompareTo(r);
        }
        return 0;

        static int[] Teile(string v) => v
            .Split('.', '+', '-')
            .Select(t => int.TryParse(new string(t.TakeWhile(char.IsDigit).ToArray()), out var z) ? z : 0)
            .ToArray();
    }
}
