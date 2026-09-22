using System.IO;
using System.Net.Http;
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
        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        if (!File.Exists(exe))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: $"Nicht gefunden: {exe}");

        var installiert = "";
        if (!string.IsNullOrWhiteSpace(eintrag.VersionsArgumente))
        {
            var lauf = await Kommandozeile.AusfuehrenAsync(exe, eintrag.VersionsArgumente, TimeSpan.FromMinutes(2), abbruch: abbruch, alsAufrufer: true);
            installiert = VersionsMuster.Match(lauf.Ausgabe).Value;
            protokoll.Report($"{Path.GetFileName(exe)} {eintrag.VersionsArgumente} -> {lauf.Ausgabe}");
        }

        // Path 1: the tool can tell us itself what it would update.
        if (!string.IsNullOrWhiteSpace(eintrag.PruefArgumente))
        {
            protokoll.Report($"{Path.GetFileName(exe)} {eintrag.PruefArgumente}");
            var lauf = await Kommandozeile.AusfuehrenAsync(
                exe, eintrag.PruefArgumente, TimeSpan.FromMinutes(6), abbruch: abbruch, alsAufrufer: true);
            protokoll.Report(lauf.Ausgabe);

            // A dry run that failed or timed out lists nothing -- which would read as "nothing
            // pending". Only a clean run is allowed to say "up to date".
            if (lauf.Abgelaufen || lauf.ExitCode != 0)
                return new PruefErgebnis(UpdateZustand.Fehler, installiert, "",
                    lauf.Abgelaufen
                        ? "Die Prüfung hat das Zeitlimit überschritten – der Stand ist unbekannt."
                        : "Die Prüfung endete mit Code " + lauf.ExitCode + " – der Stand ist unbekannt.",
                    lauf.Ausgabe);

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
                    neuer ? $"Neue Version {verfuegbar} verfügbar." : "Auf dem neuesten Stand.");
            }
        }

        return new PruefErgebnis(UpdateZustand.Unbekannt, installiert, "",
            "Keine Prüfquelle hinterlegt – das Update lässt sich trotzdem auslösen.");
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        if (!File.Exists(exe))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: $"Nicht gefunden: {exe}");

        var args = eintrag.UpdateArgumente ?? "update";
        protokoll.Report($"{Path.GetFileName(exe)} {args}");

        // alsAufrufer: die verwaltete exe kann das Admin-Flag dieser App tragen (siehe Kommandozeile).
        var lauf = await Kommandozeile.AusfuehrenAsync(
            exe, args, TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), abbruch: abbruch, alsAufrufer: true);
        protokoll.Report(lauf.Ausgabe);

        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Zeitlimit überschritten.", Protokoll: lauf.Ausgabe);
        if (lauf.ExitCode != 0)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: $"Endete mit Code {lauf.ExitCode}.", Protokoll: lauf.Ausgabe);

        return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update abgeschlossen.", Protokoll: lauf.Ausgabe);
    }

    /// <summary>
    /// The version where the tool reports one. The LM Studio runtimes have none, so their
    /// fingerprint is the dry-run plan instead: after a successful update it must be empty.
    /// </summary>
    public async Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        if (!File.Exists(exe)) return "";

        BefehlErgebnis? versionsLauf = null, planLauf = null;
        if (!string.IsNullOrWhiteSpace(eintrag.VersionsArgumente))
            versionsLauf = await Kommandozeile.AusfuehrenAsync(exe, eintrag.VersionsArgumente,
                TimeSpan.FromMinutes(2), abbruch: abbruch, alsAufrufer: true);

        // The dry run only when there is no version reading at all -- see FingerabdruckAus.
        if (versionsLauf is null && !string.IsNullOrWhiteSpace(eintrag.PruefArgumente))
            planLauf = await Kommandozeile.AusfuehrenAsync(exe, eintrag.PruefArgumente,
                TimeSpan.FromMinutes(8), abbruch: abbruch, alsAufrufer: true);

        var (fingerabdruck, problem) = FingerabdruckAus(versionsLauf, planLauf);
        if (problem is not null) Protokollierung.Schreiben(eintrag.Id, "Fingerabdruck unbekannt: " + problem);
        return fingerabdruck;
    }

    /// <summary>
    /// Pure evaluation of the fingerprint queries. A failed or timed-out query yields "" (unknown),
    /// never a plausible value: an aborted dry run lists nothing and used to read as "nichts offen"
    /// -- a fake "after" state that proved an update which never happened. Likewise a failed
    /// version query must not fall back to the dry run: the two readings would be of different
    /// kinds and always differ.
    /// </summary>
    internal static (string Fingerabdruck, string? Problem) FingerabdruckAus(BefehlErgebnis? versionsLauf, BefehlErgebnis? planLauf)
    {
        if (versionsLauf is not null)
        {
            if (versionsLauf.Abgelaufen) return ("", "Versionsabfrage hat das Zeitlimit überschritten.");
            if (versionsLauf.ExitCode != 0) return ("", "Versionsabfrage endete mit Code " + versionsLauf.ExitCode + ".");
            var version = VersionsMuster.Match(versionsLauf.Ausgabe).Value;
            return string.IsNullOrWhiteSpace(version)
                ? ("", "Versionsabfrage lieferte keine Versionsnummer.")
                : (version, null);
        }

        if (planLauf is not null)
        {
            if (planLauf.Abgelaufen) return ("", "Dry-Run hat das Zeitlimit überschritten.");
            if (planLauf.ExitCode != 0) return ("", "Dry-Run endete mit Code " + planLauf.ExitCode + ".");
            var geplant = planLauf.Ausgabe
                .Split('\n')
                .Where(z => z.Contains('→') || z.Contains("->"))
                .Select(z => z.Trim())
                .ToList();
            return (geplant.Count == 0 ? "nichts offen" : string.Join(" | ", geplant), null);
        }

        return ("", null);
    }

    private static readonly HttpClient Netz = new() { Timeout = TimeSpan.FromSeconds(30) };

    /// <summary>
    /// Fragt die Registry direkt per HTTPS statt über "npm view". Der npm-Shim löst seine eigenen
    /// Module relativ zum Arbeitsverzeichnis auf; startet die Zentrale ihn aus dem Projektordner,
    /// sucht er dort ein node_modules und bricht mit MODULE_NOT_FOUND ab. Die Registry-Abfrage
    /// braucht weder node noch ein bestimmtes Arbeitsverzeichnis.
    /// </summary>
    private static async Task<string> NpmVersionAsync(string paket, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var adresse = $"https://registry.npmjs.org/{paket}/latest";
        try
        {
            using var antwort = await Netz.GetAsync(adresse, abbruch);
            antwort.EnsureSuccessStatusCode();
            using var strom = await antwort.Content.ReadAsStreamAsync(abbruch);
            using var json = await System.Text.Json.JsonDocument.ParseAsync(strom, cancellationToken: abbruch);
            var version = json.RootElement.TryGetProperty("version", out var feld) ? feld.GetString() ?? "" : "";
            protokoll.Report($"registry.npmjs.org {paket} -> {version}");
            return VersionsMuster.Match(version).Value;
        }
        catch (Exception ex)
        {
            protokoll.Report($"registry.npmjs.org {paket} -> Abfrage fehlgeschlagen: {ex.Message}");
            return "";
        }
    }

    /// <summary>
    /// Numeric component compare; returns &gt;0 when a is newer than b. Part by part: "1.24.5" is
    /// older than "1.24.50" -- a prefix test says otherwise.
    /// </summary>
    internal static int Vergleiche(string a, string b)
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
