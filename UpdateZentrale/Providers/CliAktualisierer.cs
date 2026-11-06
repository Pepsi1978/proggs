using System.IO;
using System.Net.Http;
using System.Text.RegularExpressions;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Command line tools that update themselves ("claude update", "codex update").
/// Availability comes from one of two sources, whichever the catalog
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
            protokoll.Report($"{Path.GetFileName(exe)} {eintrag.VersionsArgumente} -> {lauf.Ausgabe}");

            // Without a readable installed version any comparison is meaningless: "" against the
            // registry version used to announce an update that nobody could verify.
            var (version, problem) = FingerabdruckAus(lauf, null, eintrag);
            if (problem is not null)
            {
                protokoll.Report("Versionsabfrage fehlgeschlagen: " + problem);
                return new PruefErgebnis(UpdateZustand.Fehler, "", "",
                    "Die installierte Version ließ sich nicht ermitteln – " + problem, lauf.Ausgabe);
            }
            installiert = version;
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
            if (lauf.Abgelaufen || !PruefCodeGueltig(eintrag, lauf.ExitCode))
                return new PruefErgebnis(UpdateZustand.Fehler, installiert, "",
                    lauf.Abgelaufen
                        ? "Die Prüfung hat das Zeitlimit überschritten – der Stand ist unbekannt."
                        : "Die Prüfung endete mit Code " + lauf.ExitCode + " – der Stand ist unbekannt.",
                    lauf.Ausgabe);

            var geplant = GeplanteZeilen(lauf.Ausgabe, eintrag);

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
            var npm = await NpmVersionAsync(eintrag.NpmPaket, protokoll, abbruch);
            if (!npm.Erfolg)
                // The source exists -- it failed. "No source configured" would send the user
                // looking in the wrong place.
                return new PruefErgebnis(UpdateZustand.Fehler, installiert, "", npm.Problem ?? "Registry-Abfrage fehlgeschlagen.");

            var verfuegbar = npm.Version;
            var neuer = Vergleiche(verfuegbar, installiert) > 0;
            return new PruefErgebnis(neuer ? UpdateZustand.UpdateVerfuegbar : UpdateZustand.Aktuell,
                installiert, verfuegbar,
                neuer ? $"Neue Version {verfuegbar} verfügbar." : "Auf dem neuesten Stand.");
        }

        // Path 3: a plain-text URL that names the newest version (Kimi Code's CDN "latest").
        if (!string.IsNullOrWhiteSpace(eintrag.VersionsUrl))
        {
            var url = await UrlVersionAsync(Netz, eintrag.VersionsUrl, protokoll, abbruch, eintrag.VersionsUrlMuster);
            if (!url.Erfolg)
                return new PruefErgebnis(UpdateZustand.Fehler, installiert, "", url.Problem ?? "Versionsabfrage fehlgeschlagen.");

            var verfuegbar = url.Version;
            var neuer = Vergleiche(verfuegbar, installiert) > 0;
            return new PruefErgebnis(neuer ? UpdateZustand.UpdateVerfuegbar : UpdateZustand.Aktuell,
                installiert, verfuegbar,
                neuer ? $"Neue Version {verfuegbar} verfügbar." : "Auf dem neuesten Stand.");
        }

        return new PruefErgebnis(UpdateZustand.Unbekannt, installiert, "",
            "Keine Prüfquelle hinterlegt – das Update lässt sich trotzdem auslösen.");
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var exe = Pfade.Aufloesen(string.IsNullOrWhiteSpace(eintrag.UpdateExePfad)
            ? eintrag.ExePfadWirksam
            : eintrag.UpdateExePfad);
        if (!File.Exists(exe))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: $"Nicht gefunden: {exe}");

        // Captured before the run: afterwards ExePfadWirksam may already have fallen back to another path.
        var programmDatei = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        var npmGlobal = NpmGlobal.Betrifft(eintrag, programmDatei, NpmGlobal.GlobalOrdner);
        if (npmGlobal && await NpmGlobal.VorabLadenAsync(eintrag, protokoll, abbruch) is { } vorab) return vorab;

        try
        {
            return await UpdateLaufenAsync(eintrag, exe, programmDatei, npmGlobal, protokoll, abbruch);
        }
        catch (OperationCanceledException) when (npmGlobal && NpmGlobal.Zerstoert(eintrag, programmDatei, NpmGlobal.GlobalOrdner))
        {
            // Cancelled in a retry pause: the removed package must not stay removed.
            await NpmGlobal.WiederherstellenAsync(eintrag, programmDatei, protokoll);
            throw;
        }
    }

    private static async Task<PruefErgebnis> UpdateLaufenAsync(ProgrammEintrag eintrag, string exe, string programmDatei, bool npmGlobal,
                                                               IProgress<string> protokoll, CancellationToken abbruch)
    {
        // %USERPROFILE% & Co. as in the paths -- the helper scripts under werkzeuge\ are found that way.
        var args = Environment.ExpandEnvironmentVariables(eintrag.UpdateArgumente ?? "update");
        protokoll.Report($"{Path.GetFileName(exe)} {args}");

        // npm.cmd resolves its own modules relative to the working directory; started from the
        // project folder it looks for node_modules there (MODULE_NOT_FOUND). Its own folder is
        // always right, and harmless for every other tool.
        var ordner = Path.GetDirectoryName(exe);

        // alsAufrufer: die verwaltete exe kann das Admin-Flag dieser App tragen (siehe Kommandozeile).
        // npm tools: quiet time instead of a fixed limit, in case the run has to download after all.
        Task<BefehlErgebnis> Starten() => Kommandozeile.AusfuehrenAsync(
            exe, args, TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), ordner, abbruch: abbruch, alsAufrufer: true,
            lebenszeichen: npmGlobal ? NpmGlobal.DownloadLebenszeichen() : null);

        var lauf = await Starten();
        protokoll.Report(lauf.Ausgabe);

        // The tools fetch their own release list. When that one request fails ("Unable to fetch
        // latest version from npm registry") nothing was changed yet, and a second attempt a few
        // seconds later goes through -- which is exactly what a second click used to do by hand.
        // With a manifest fallback the tool's own downloader is not tried again: it stalled on the
        // route it picks itself (IPv6), every further attempt costs minutes for the same result.
        for (var versuch = 2; versuch <= 3 && IstNetzfehler(lauf) && !(eintrag.ErsatzDownload && IstDownloadAbbruch(lauf)); versuch++)
        {
            protokoll.Report("Netzfehler beim Abruf – Versuch " + versuch + " von 3 …");
            await Task.Delay(TimeSpan.FromSeconds(3), abbruch);
            lauf = await Starten();
            protokoll.Report(lauf.Ausgabe);
        }

        // The tool says another update is still running. Only for tools that update themselves
        // (not via pwsh/cmd): their orphaned leftovers are a stalled earlier run, see VerwaisteBeenden.
        if (IstUpdateSperre(lauf) && Prozesspfad.GleicheDatei(exe, Pfade.Aufloesen(eintrag.ExePfadWirksam))
            && await Task.Run(() => VerwaisteBeenden(exe, protokoll, abbruch), abbruch) > 0)
        {
            protokoll.Report("Neuer Versuch nach dem Beenden des hängengebliebenen Laufs …");
            lauf = await Starten();
            protokoll.Report(lauf.Ausgabe);
        }

        // npm removed the old package and the run ended before the new one arrived. Not while part
        // of the old process tree survived: two npm runs in the same node_modules make it worse.
        var zerstoert = npmGlobal && NpmGlobal.Zerstoert(eintrag, programmDatei, NpmGlobal.GlobalOrdner);
        if (zerstoert && lauf.BeendenProblem is null && await NpmGlobal.WiederherstellenAsync(eintrag, programmDatei, protokoll))
            return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update abgeschlossen, nachdem npm die Installation unterbrochen hatte.",
                                     Protokoll: lauf.Ausgabe);

        // The tool's own download failed (or the run hung): fetch the file named in the manifest
        // ourselves, IPv4 first. Not while another run of the tool still holds its update lock.
        if (eintrag.ErsatzDownload && !npmGlobal && !lauf.Abgebrochen && lauf.BeendenProblem is null && !IstUpdateSperre(lauf)
            && (lauf.Abgelaufen || IstDownloadAbbruch(lauf) || IstDownloadFrist(lauf))
            && Prozesspfad.GleicheDatei(exe, programmDatei)
            && await ErsatzDownloadAsync(eintrag, programmDatei, protokoll, abbruch) is { } ersatz)
            return ersatz with { Protokoll = lauf.Ausgabe + Environment.NewLine + ersatz.Protokoll };

        // Only while it is true: "stays usable" must not stand over a removed package.
        if (IstDownloadFrist(lauf) && !zerstoert)
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: LangsamesNetzMeldung + eintrag.Name + " bricht seinen eigenen Download nach etwa 10 Minuten ab. "
                         + "Die Verbindung ist gerade zu langsam für die neue Version – in einem schnelleren Netz erneut versuchen. "
                         + "Die installierte Version bleibt unverändert nutzbar.",
                Protokoll: lauf.Ausgabe);

        if (Kommandozeile.UnsauberesEnde(lauf) is { } unsauber) return unsauber;
        if (lauf.ExitCode != 0)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: $"Endete mit Code {lauf.ExitCode}.", Protokoll: lauf.Ausgabe);

        return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update abgeschlossen.", Protokoll: lauf.Ausgabe);
    }

    /// <summary>
    /// The version where the tool reports one. Entries without one use their
    /// dry-run plan as fingerprint instead: after a successful update it must be empty.
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

        var (fingerabdruck, problem) = FingerabdruckAus(versionsLauf, planLauf, eintrag);
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
    internal static (string Fingerabdruck, string? Problem) FingerabdruckAus(BefehlErgebnis? versionsLauf, BefehlErgebnis? planLauf,
                                                                           ProgrammEintrag? eintrag = null)
    {
        if (versionsLauf is not null)
        {
            if (versionsLauf.Abgelaufen) return ("", "Versionsabfrage hat das Zeitlimit überschritten.");
            if (versionsLauf.ExitCode != 0) return ("", "Versionsabfrage endete mit Code " + versionsLauf.ExitCode + ".");
            var version = VersionAus(versionsLauf.Ausgabe, eintrag?.VersionsMuster);
            return string.IsNullOrWhiteSpace(version)
                ? ("", "Versionsabfrage lieferte keine Versionsnummer.")
                : (version, null);
        }

        if (planLauf is not null)
        {
            if (planLauf.Abgelaufen) return ("", "Dry-Run hat das Zeitlimit überschritten.");
            if (!PruefCodeGueltig(eintrag, planLauf.ExitCode)) return ("", "Dry-Run endete mit Code " + planLauf.ExitCode + ".");
            var geplant = GeplanteZeilen(planLauf.Ausgabe, eintrag);
            return (geplant.Count == 0 ? "nichts offen" : string.Join(" | ", geplant), null);
        }

        return ("", null);
    }

    /// <summary>Exit codes that still carry a valid answer (rustup check: 100 = updates pending).</summary>
    internal static bool PruefCodeGueltig(ProgrammEintrag? eintrag, int exitCode)
        => exitCode == 0 || (eintrag?.PruefExitCodes.Contains(exitCode) ?? false);

    /// <summary>
    /// The planned updates in a check run: lines with an arrow by default, or -- when the catalog
    /// names them -- lines after a section marker that match a pattern.
    /// </summary>
    internal static List<string> GeplanteZeilen(string ausgabe, ProgrammEintrag? eintrag)
    {
        var zeilen = ausgabe.Replace("\r", "").Split('\n').AsEnumerable();

        if (!string.IsNullOrWhiteSpace(eintrag?.PruefAbschnitt))
        {
            var marke = eintrag.PruefAbschnitt;
            var liste = zeilen.ToList();
            var start = liste.FindIndex(z => z.Contains(marke, StringComparison.OrdinalIgnoreCase));
            // No section at all means nothing pending -- sdkmanager omits "Available Updates" then.
            zeilen = start < 0 ? Enumerable.Empty<string>() : liste.Skip(start + 1);
        }

        Func<string, bool> geplant = string.IsNullOrWhiteSpace(eintrag?.PruefZeilenMuster)
            ? z => z.Contains('→') || z.Contains("->")
            : new Regex(eintrag.PruefZeilenMuster).IsMatch;

        return zeilen.Where(geplant).Select(z => z.Trim()).Where(z => z.Length > 0).ToList();
    }

    /// <summary>
    /// Ends leftovers of an earlier update run of the same exe. A run that stalled (Antigravity's
    /// download hung without a connection) outlives the app that started it once that app is
    /// closed; it keeps its update lock open and every later "agy update" ends at once with
    /// "update already in progress". Only orphans running for at least two minutes count: an
    /// interactive session still has its terminal as parent, a just-started update may finish.
    /// Called only after the tool itself reported a running update, so a live session is never
    /// touched on a normal run.
    /// </summary>
    /// <returns>How many leftovers really ended.</returns>
    private static int VerwaisteBeenden(string exe, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var beendet = 0;
        var alle = Prozessbaum.Schnappschuss();
        var eigene = Environment.ProcessId;
        // By path, not by name: a self-updater's installer may run under another file name.
        foreach (var kandidat in System.Diagnostics.Process.GetProcesses())
        {
            using (kandidat)
            {
                if (abbruch.IsCancellationRequested || kandidat.Id <= 4 || kandidat.Id == eigene) continue;
                // Cheap path check with limited rights first: protected processes yield "" here and
                // are skipped without opening a full-access handle or logging a warning.
                if (!Prozesspfad.GleicheDatei(Prozesspfad.Lesen(kandidat.Id), exe)) continue;
                var baum = new List<System.Diagnostics.Process>();
                try
                {
                    _ = kandidat.Handle;                          // identity pinned, then the path again
                    if (!Prozesspfad.GleicheDatei(Prozesspfad.Lesen(kandidat.Id), exe)) continue;
                    var start = kandidat.StartTime;
                    var knoten = alle.FirstOrDefault(k => k.Pid == kandidat.Id);
                    if (knoten.Pid == 0 || !IstVerwaist(knoten.ElternPid, start, alle)) continue;
                    if (DateTime.Now - start < TimeSpan.FromMinutes(2))
                    {
                        protokoll.Report($"Verwaister {Path.GetFileName(exe)}-Prozess {kandidat.Id} läuft erst seit "
                                         + start.ToString("HH:mm") + " – bleibt bestehen, das Update bitte in ein paar Minuten erneut versuchen.");
                        continue;
                    }

                    protokoll.Report($"Verwaister {Path.GetFileName(exe)}-Prozess {kandidat.Id} (gestartet "
                                     + start.ToString("dd.MM. HH:mm") + ") hält das Update auf – wird beendet.");
                    baum = Prozessbaum.Erfassen(kandidat);       // descendants may hold the lock too
                    try
                    {
                        kandidat.Kill(entireProcessTree: true);
                    }
                    catch (Exception killAusnahme)
                    {
                        // Some descendant (elevated) refused; the check below tells whether the lock is gone.
                        Diagnose.Gefangen(killAusnahme, "provider", Schwere.Info);
                    }
                    kandidat.WaitForExit(5000);
                    foreach (var kind in baum) kind.WaitForExit(5000);
                    if (!kandidat.HasExited || baum.Any(k => !BeendetSicher(k)))
                        protokoll.Report($"Prozess {kandidat.Id} ließ sich nicht vollständig beenden.");
                    else
                        beendet++;
                }
                catch (Exception diagAusnahme)
                {
                    Diagnose.Gefangen(diagAusnahme, "provider", Schwere.Warnung);
                }
                finally
                {
                    foreach (var kind in baum) kind.Dispose();
                }
            }
        }
        return beendet;
    }

    /// <summary>HasExited, but a handle without query rights counts as still running.</summary>
    private static bool BeendetSicher(System.Diagnostics.Process prozess)
    {
        try { return prozess.HasExited; }
        catch { return false; }
    }

    /// <summary>Parent gone, or its PID now belongs to a younger process (reused).</summary>
    private static bool IstVerwaist(int elternPid, DateTime kindStart, List<Prozessbaum.Knoten> alle)
    {
        if (alle.All(k => k.Pid != elternPid)) return true;
        try
        {
            using var eltern = System.Diagnostics.Process.GetProcessById(elternPid);
            return eltern.StartTime > kindStart;
        }
        catch (ArgumentException)
        {
            return true;    // exited since the snapshot
        }
        catch
        {
            return false;   // not readable (other user, elevated): rather keep a live session
        }
    }

    /// <summary>A run refused because another update of the same tool is still going.</summary>
    internal static bool IstUpdateSperre(BefehlErgebnis lauf)
        => !lauf.Abgelaufen && !lauf.Abgebrochen && lauf.ExitCode != 0
           && lauf.Ausgabe.Contains("already in progress", StringComparison.OrdinalIgnoreCase);

    private static readonly string[] NetzHinweise =
    {
        "unable to fetch", "failed to check for updates", "network", "etimedout", "econnreset", "enotfound",
        "eai_again", "timed out", "timeout", "fetch failed", "socket hang up", "connection",
        // Go tools (agy) and the localized Winsock text -- "Download interrupted: ... wsarecv: Ein
        // Verbindungsversuch ist fehlgeschlagen" matched none of the English words above.
        "download interrupted", "wsarecv", "wsasend", "while reading body", "verbindungsversuch"
    };

    /// <summary>The tool's own download broke off mid-transfer (Go: agy). Only then the manifest fallback pays off.</summary>
    internal static bool IstDownloadAbbruch(BefehlErgebnis lauf)
        => lauf.ExitCode != 0
           && new[] { "download interrupted", "while reading body", "wsarecv", "deadline exceeded", "connection reset" }
               .Any(h => lauf.Ausgabe.Contains(h, StringComparison.OrdinalIgnoreCase));

    /// <summary>Download link and hash from an auto-updater manifest; null when one of them is missing.</summary>
    internal static (string Version, string Url, string Algorithmus, string Pruefsumme)? ManifestAuswerten(string json)
    {
        try
        {
            using var dokument = System.Text.Json.JsonDocument.Parse(json);
            var wurzel = dokument.RootElement;
            if (wurzel.ValueKind != System.Text.Json.JsonValueKind.Object) return null;
            string? Text(string name) => wurzel.TryGetProperty(name, out var feld) && feld.ValueKind == System.Text.Json.JsonValueKind.String
                ? feld.GetString() : null;

            var url = Text("url");
            if (!Uri.TryCreate(url, UriKind.Absolute, out var adresse) || adresse.Scheme != Uri.UriSchemeHttps) return null;
            if (Text("version") is not { Length: > 0 } version) return null;
            if (Text("sha512") is { Length: 128 } sha512) return (version, url, "SHA512", sha512.ToLowerInvariant());
            if (Text("sha256") is { Length: 64 } sha256) return (version, url, "SHA256", sha256.ToLowerInvariant());
            return null;   // never install a file that cannot be checked
        }
        catch (System.Text.Json.JsonException)
        {
            return null;
        }
    }

    /// <summary>
    /// Fetches the program file named in the manifest with curl -4, checks its hash and swaps it in:
    /// the old exe is renamed (Windows allows that while a session still runs it), the new one moved
    /// to its place, and "--version" must answer -- otherwise the old file comes back.
    /// </summary>
    /// <returns>null when the fallback could not even start (no manifest), so the normal failure path reports.</returns>
    private static async Task<PruefErgebnis?> ErsatzDownloadAsync(ProgrammEintrag eintrag, string exe, IProgress<string> protokoll,
                                                                  CancellationToken abbruch)
    {
        if (string.IsNullOrWhiteSpace(eintrag.VersionsUrl)) return null;
        protokoll.Report("Eigener Download gescheitert – die UpdateZentrale lädt die neue Version selbst (IPv4) …");

        (string Version, string Url, string Algorithmus, string Pruefsumme)? manifest;
        try
        {
            using var antwort = await Netzdienst.HolenAsync(Netz, eintrag.VersionsUrl, abbruch);
            manifest = antwort.IsSuccessStatusCode ? ManifestAuswerten(await antwort.Content.ReadAsStringAsync(abbruch)) : null;
        }
        catch (OperationCanceledException) when (abbruch.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception ex)
        {
            protokoll.Report("Manifest nicht lesbar: " + ex.Message);
            return null;
        }
        if (manifest is not { } m)
        {
            protokoll.Report("Das Manifest nennt keine Download-Adresse mit Prüfsumme.");
            return null;
        }

        // Only a ready program file is swapped in; an archive (macOS gets a .tar.gz) would need unpacking.
        if (!string.Equals(Path.GetExtension(new Uri(m.Url).AbsolutePath), Path.GetExtension(exe), StringComparison.OrdinalIgnoreCase))
        {
            protokoll.Report("Das Manifest nennt keine fertige Programmdatei: " + m.Url);
            return null;
        }

        var ordner = Path.GetDirectoryName(exe)!;

        // Only something really newer: a cached manifest must never put an older version in place.
        var vorher = await Kommandozeile.AusfuehrenAsync(exe, eintrag.VersionsArgumente ?? "--version", TimeSpan.FromMinutes(2),
                                                         ordner, abbruch: abbruch, alsAufrufer: true);
        var installiert = vorher.ExitCode == 0 && !vorher.Abgelaufen ? VersionAus(vorher.Ausgabe, eintrag.VersionsMuster) : "";
        if (string.IsNullOrWhiteSpace(installiert) || Vergleiche(m.Version, installiert) <= 0)
        {
            protokoll.Report("Manifest nennt " + m.Version + ", installiert ist "
                             + (string.IsNullOrWhiteSpace(installiert) ? "unbekannt" : installiert) + " – kein Ersatzdownload.");
            return null;
        }

        // Leftovers of earlier swaps that a session held open back then.
        foreach (var rest in Directory.EnumerateFiles(ordner, Path.GetFileName(exe) + ".alt*"))
            try { File.Delete(rest); } catch (Exception ex) when (ex is IOException or UnauthorizedAccessException) { /* still in use */ }

        var neu = exe + ".download";
        var alt = exe + ".alt";
        try
        {
            protokoll.Report("Lade " + m.Url);
            var laden = await WingetAktualisierer.CurlLadenAsync(m.Url, neu, eintrag, abbruch);
            if (laden.ExitCode != 0 || laden.Abgelaufen || laden.Abgebrochen || laden.BeendenProblem is not null || !File.Exists(neu))
                return new PruefErgebnis(UpdateZustand.Fehler,
                    Meldung: NetzGestoertMeldung + "Auch der Ersatzdownload per IPv4 scheiterte (curl Code " + laden.ExitCode + ").",
                    Protokoll: laden.Ausgabe);

            string ist;
            await using (var datei = File.OpenRead(neu))
                ist = Convert.ToHexString(m.Algorithmus == "SHA512"
                    ? await System.Security.Cryptography.SHA512.HashDataAsync(datei, abbruch)
                    : await System.Security.Cryptography.SHA256.HashDataAsync(datei, abbruch)).ToLowerInvariant();
            if (ist != m.Pruefsumme)
                return new PruefErgebnis(UpdateZustand.Fehler,
                    Meldung: "Ersatzdownload verworfen: die " + m.Algorithmus + "-Prüfsumme passt nicht zum Manifest.");
            protokoll.Report(m.Algorithmus + "-Prüfsumme stimmt.");

            // A leftover from the previous swap may still be locked by a running session.
            if (File.Exists(alt))
            {
                try { File.Delete(alt); }
                catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
                {
                    alt = exe + ".alt-" + DateTime.Now.ToString("yyyyMMddHHmmss");
                }
            }
            File.Move(exe, alt);
            try
            {
                File.Move(neu, exe);
            }
            catch
            {
                File.Move(alt, exe);
                throw;
            }

            var probe = await Kommandozeile.AusfuehrenAsync(exe, eintrag.VersionsArgumente ?? "--version", TimeSpan.FromMinutes(2),
                                                            ordner, abbruch: abbruch, alsAufrufer: true);
            if (probe.Abgelaufen || probe.ExitCode != 0)
            {
                File.Move(exe, neu, overwrite: true);
                File.Move(alt, exe);
                return new PruefErgebnis(UpdateZustand.Fehler,
                    Meldung: "Die geladene Version startete nicht (Code " + probe.ExitCode + ") – die alte bleibt installiert.",
                    Protokoll: probe.Ausgabe);
            }
            protokoll.Report("Neue Datei eingesetzt: " + probe.Ausgabe.Trim());
            try { File.Delete(alt); } catch (Exception ex) when (ex is IOException or UnauthorizedAccessException) { /* a running session still uses it */ }
            return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update per Ersatzdownload (IPv4) abgeschlossen.",
                                     Protokoll: probe.Ausgabe);
        }
        catch (OperationCanceledException) when (abbruch.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
        {
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Ersatzdownload ließ sich nicht einsetzen: " + ex.Message);
        }
        finally
        {
            try { if (File.Exists(neu)) File.Delete(neu); }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException) { /* next run overwrites it */ }
        }
    }

    /// <summary>A failed run that ended by itself and names the network as the reason.</summary>
    internal static bool IstNetzfehler(BefehlErgebnis lauf)
        => !lauf.Abgelaufen && !lauf.Abgebrochen && lauf.BeendenProblem is null && lauf.ExitCode != 0
           && !IstDownloadFrist(lauf)
           && NetzHinweise.Any(h => lauf.Ausgabe.Contains(h, StringComparison.OrdinalIgnoreCase));

    /// <summary>Start of the failure text for <see cref="IstDownloadFrist"/>; keeps it out of the Claude handover.</summary>
    public const string LangsamesNetzMeldung = "Download zu langsam: ";

    /// <summary>Start of the failure text when the network stayed down over all attempts; a session cannot repair that either.</summary>
    public const string NetzGestoertMeldung = "Netz gestört: ";

    /// <summary>
    /// The tool's own overall download deadline ran out (Claude Code: ~10 min for ~300 MB). Not a
    /// transient error: on the same slow line every retry burns another full deadline, three
    /// attempts kept the card busy for half an hour.
    /// </summary>
    internal static bool IstDownloadFrist(BefehlErgebnis lauf)
        => lauf.ExitCode != 0
           && (lauf.Ausgabe.Contains("exceeded the total deadline", StringComparison.OrdinalIgnoreCase)
               || lauf.Ausgabe.Contains("Download timed out", StringComparison.OrdinalIgnoreCase));

    /// <summary>IPv4 first and bounded per address -- see Netzdienst for why that matters here.</summary>
    private static HttpClient Netz => Netzdienst.Client;

    /// <summary>
    /// Fragt die Registry direkt per HTTPS statt über "npm view". Der npm-Shim löst seine eigenen
    /// Module relativ zum Arbeitsverzeichnis auf; startet die Zentrale ihn aus dem Projektordner,
    /// sucht er dort ein node_modules und bricht mit MODULE_NOT_FOUND ab. Die Registry-Abfrage
    /// braucht weder node noch ein bestimmtes Arbeitsverzeichnis.
    /// </summary>
    /// <summary>Success, version and reason kept apart: "" alone could not say why.</summary>
    internal readonly record struct NpmAbfrage(bool Erfolg, string Version, string? Problem);

    internal static Task<NpmAbfrage> NpmVersionAsync(string paket, IProgress<string> protokoll, CancellationToken abbruch)
        => NpmVersionAsync(Netz, paket, protokoll, abbruch);

    /// <param name="netz">Injectable so the HTTP path is testable with a fake handler.</param>
    internal static async Task<NpmAbfrage> NpmVersionAsync(HttpClient netz, string paket, IProgress<string> protokoll,
                                                           CancellationToken abbruch)
    {
        var adresse = $"https://registry.npmjs.org/{paket}/latest";
        NpmAbfrage ergebnis;
        try
        {
            using var antwort = await Netzdienst.HolenAsync(netz, adresse, abbruch);
            if (!antwort.IsSuccessStatusCode)
            {
                ergebnis = new NpmAbfrage(false, "",
                    "Registry-Abfrage fehlgeschlagen: HTTP " + (int)antwort.StatusCode + " " + antwort.ReasonPhrase + ".");
            }
            else
            {
                ergebnis = NpmAuswerten(await antwort.Content.ReadAsStringAsync(abbruch));
            }
        }
        catch (OperationCanceledException) when (abbruch.IsCancellationRequested)
        {
            throw;   // a requested cancel is not a registry failure
        }
        catch (Exception ex)
        {
            // Includes the HttpClient timeout, which also surfaces as a cancellation.
            ergebnis = new NpmAbfrage(false, "", "Registry-Abfrage fehlgeschlagen: " + ex.Message);
        }

        protokoll.Report($"registry.npmjs.org {paket} -> " + (ergebnis.Erfolg ? ergebnis.Version : ergebnis.Problem));
        return ergebnis;
    }

    /// <summary>Reads the first version number from a plain-text URL; redirects are followed.</summary>
    internal static async Task<NpmAbfrage> UrlVersionAsync(HttpClient netz, string adresse, IProgress<string> protokoll,
                                                           CancellationToken abbruch, string? muster = null)
    {
        NpmAbfrage ergebnis;
        try
        {
            using var antwort = await Netzdienst.HolenAsync(netz, adresse, abbruch);
            if (!antwort.IsSuccessStatusCode)
            {
                ergebnis = new NpmAbfrage(false, "",
                    "Versionsabfrage fehlgeschlagen: HTTP " + (int)antwort.StatusCode + " " + antwort.ReasonPhrase + ".");
            }
            else
            {
                var version = VersionAus(await antwort.Content.ReadAsStringAsync(abbruch), muster);
                ergebnis = string.IsNullOrWhiteSpace(version)
                    ? new NpmAbfrage(false, "", "Die Versionsadresse lieferte keine Versionsnummer.")
                    : new NpmAbfrage(true, version, null);
            }
        }
        catch (OperationCanceledException) when (abbruch.IsCancellationRequested)
        {
            throw;
        }
        catch (Exception ex)
        {
            ergebnis = new NpmAbfrage(false, "", "Versionsabfrage fehlgeschlagen: " + ex.Message);
        }

        protokoll.Report($"{adresse} -> " + (ergebnis.Erfolg ? ergebnis.Version : ergebnis.Problem));
        return ergebnis;
    }

    /// <summary>
    /// The version in a text: group 1 of the catalog pattern when one is set (two-part versions
    /// like Gradle's "9.7"), otherwise the first "x.y.z". An invalid pattern yields "" (unknown).
    /// </summary>
    internal static string VersionAus(string text, string? muster)
    {
        if (string.IsNullOrWhiteSpace(muster)) return VersionsMuster.Match(text).Value;
        try
        {
            var treffer = Regex.Match(text, muster, RegexOptions.None, TimeSpan.FromSeconds(1));
            return treffer.Success ? (treffer.Groups.Count > 1 ? treffer.Groups[1].Value : treffer.Value) : "";
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "provider", Schwere.Warnung);
            return "";
        }
    }

    /// <summary>Pure evaluation of the registry body: only a readable version number counts.</summary>
    internal static NpmAbfrage NpmAuswerten(string json)
    {
        try
        {
            using var dokument = System.Text.Json.JsonDocument.Parse(json);
            var roh = dokument.RootElement.ValueKind == System.Text.Json.JsonValueKind.Object
                      && dokument.RootElement.TryGetProperty("version", out var feld)
                      && feld.ValueKind == System.Text.Json.JsonValueKind.String
                ? feld.GetString() ?? ""
                : "";
            var version = VersionsMuster.Match(roh).Value;
            return string.IsNullOrWhiteSpace(version)
                ? new NpmAbfrage(false, "", "Registry lieferte keine Versionsnummer.")
                : new NpmAbfrage(true, version, null);
        }
        catch (System.Text.Json.JsonException ex)
        {
            return new NpmAbfrage(false, "", "Registry-Antwort war kein gültiges JSON: " + ex.Message);
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
