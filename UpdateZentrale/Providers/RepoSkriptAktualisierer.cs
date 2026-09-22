using System.IO;
using System.Text.RegularExpressions;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Own tools in ~/proggs. Updating never reimplements the existing PowerShell scripts, it only
/// calls them -- and never with -Force, because each script opens its own yes/no window that the
/// user has to answer.
/// </summary>
public sealed class RepoSkriptAktualisierer : IAktualisierer
{
    public string Art => "reposkript";

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var installiert = InstallierteVersion(eintrag);
        var quelle = QuellVersion(eintrag);

        // Signal 1: does the remote carry commits for this folder's build sources that are not here yet?
        var hinterstand = await HinterstandAsync(Pfade.RepoWurzel, eintrag.RepoOrdner, protokoll, abbruch);

        // Signal 2: is the source version newer than the built exe? Compared numerically --
        // a prefix test called "1.24.5" current for an exe built from "1.24.50".
        var quellstandNeuer = !string.IsNullOrWhiteSpace(quelle)
                              && !string.IsNullOrWhiteSpace(installiert)
                              && CliAktualisierer.Vergleiche(quelle, installiert) > 0;

        protokoll.Report("gebaut: " + installiert + " | Quelle: " + quelle);

        return Einordnen(installiert, quelle, hinterstand, quellstandNeuer, !string.IsNullOrWhiteSpace(eintrag.RepoOrdner));
    }

    /// <summary>
    /// The verdict of a check. A git query that failed is "unknown", never "current": the card
    /// then keeps the update button instead of hiding a pending build behind a green pill.
    /// </summary>
    internal static PruefErgebnis Einordnen(string installiert, string quelle, int? hinterstand,
                                            bool quellstandNeuer, bool hatRepoOrdner)
    {
        if (string.IsNullOrWhiteSpace(installiert))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, "", quelle, "Noch nicht gebaut.");

        if (hinterstand > 0 || quellstandNeuer)
        {
            var grund = hinterstand > 0
                ? hinterstand + " neue Commit(s) mit Quellcode auf origin/main"
                : "Quellstand " + quelle + " neuer als der gebaute Stand";
            return new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, installiert, quelle, grund + ".");
        }

        if (hatRepoOrdner && hinterstand is null)
            return new PruefErgebnis(UpdateZustand.Unbekannt, installiert, quelle,
                "Der Git-Stand ließ sich nicht abfragen – ob neue Commits vorliegen, ist unbekannt.");

        return new PruefErgebnis(UpdateZustand.Aktuell, installiert, quelle, "Gebauter Stand ist aktuell.");
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var skript = Path.Combine(Pfade.RepoWurzel, (eintrag.Skript ?? "").Replace('/', Path.DirectorySeparatorChar));
        if (!File.Exists(skript))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Skript fehlt: " + skript);

        // The check reports "update available" as soon as origin/main carries commits for this
        // folder, but the scripts only build what is on disk. Without pulling first they answer
        // "already current", the next check finds the same commits again, and the card asks for
        // the same update over and over. Same fast-forward-only pull the launcher itself does.
        var hinterstand = await HinterstandAsync(Pfade.RepoWurzel, eintrag.RepoOrdner, protokoll, abbruch);
        var lokalNeuer = LokaleQuelleNeuer(eintrag, protokoll);
        if (!DarfSkriptStarten(hinterstand, !string.IsNullOrWhiteSpace(eintrag.RepoOrdner), lokalNeuer))
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "Der Git-Stand ließ sich nicht abfragen, und lokal liegt kein neuerer Quellcode als im "
                         + "gebauten Programm. Das Update-Skript wurde nicht gestartet – es würde nur den "
                         + "unveränderten Stand bauen. Netz und Repo prüfen, dann erneut versuchen.");
        if (hinterstand is null && !string.IsNullOrWhiteSpace(eintrag.RepoOrdner))
            protokoll.Report("Git-Stand unbekannt, aber lokal liegt neuerer Quellcode – das Skript baut den vorhandenen Checkout.");
        if (hinterstand > 0)
        {
            protokoll.Report("git pull --ff-only (" + hinterstand + " neue Commit(s) für " + eintrag.RepoOrdner + ")");
            var zug = await Kommandozeile.AusfuehrenAsync("git", "pull --ff-only", TimeSpan.FromMinutes(3), Pfade.RepoWurzel, abbruch);
            protokoll.Report(zug.Ausgabe);
            if (zug.ExitCode != 0)
                return new PruefErgebnis(UpdateZustand.Fehler,
                    Meldung: "git pull --ff-only ist fehlgeschlagen (lokale Änderungen oder abweichender Verlauf) – "
                             + "das Repo muss erst von Hand abgeglichen werden. Das Update-Skript wurde nicht gestartet.",
                    Protokoll: zug.Ausgabe);
        }

        var args = ("-NoProfile -ExecutionPolicy Bypass -File \"" + skript + "\" " + eintrag.SkriptArgumente).TrimEnd();
        protokoll.Report("pwsh " + args);
        protokoll.Report("Das Skript zeigt jetzt ein Ja/Nein-Fenster. Ohne Klick auf 'Ja' passiert nichts.");

        // The scripts wait up to 240 s for the click, so the timeout must sit clearly above that.
        var zeitlimit = TimeSpan.FromSeconds(Math.Max(eintrag.DialogWartezeitSekunden, 300) + eintrag.ZeitlimitMinuten * 60);
        var exeVorher = await FingerabdruckAsync(eintrag, abbruch);
        var lauf = await Kommandozeile.AusfuehrenAsync(PwshPfad(), args, zeitlimit, Pfade.RepoWurzel, abbruch);
        protokoll.Report(lauf.Ausgabe);
        var exeNachher = await FingerabdruckAsync(eintrag, abbruch);

        return Auswerten(lauf, eintrag.StatusPraefix, !string.IsNullOrWhiteSpace(exeVorher) && exeVorher == exeNachher);
    }

    /// <summary>
    /// Without a git answer the script would build whatever is on disk. That is only worth a run
    /// when the disk demonstrably holds newer build sources than the built exe -- otherwise it
    /// reproduces exactly the "same update again" loop.
    /// </summary>
    internal static bool DarfSkriptStarten(int? hinterstand, bool hatRepoOrdner, bool lokaleQuelleNeuer)
        => !hatRepoOrdner || hinterstand is not null || lokaleQuelleNeuer;

    /// <summary>
    /// Independent of git: is the source version newer than the exe, or is any build source file
    /// newer than it? The second test is the one update-launcher.ps1 itself uses.
    /// </summary>
    private static bool LokaleQuelleNeuer(ProgrammEintrag eintrag, IProgress<string> protokoll)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        var installiert = InstallierteVersion(eintrag);
        var quelle = QuellVersion(eintrag);
        if (!string.IsNullOrWhiteSpace(quelle) && !string.IsNullOrWhiteSpace(installiert)
            && CliAktualisierer.Vergleiche(quelle, installiert) > 0)
            return true;

        if (string.IsNullOrWhiteSpace(exe) || !File.Exists(exe) || string.IsNullOrWhiteSpace(eintrag.RepoOrdner))
            return false;
        try
        {
            var ordner = Path.Combine(Pfade.RepoWurzel, eintrag.RepoOrdner.Replace('/', Path.DirectorySeparatorChar));
            return QuelleNeuerAlsBuild(ordner, File.GetLastWriteTimeUtc(exe));
        }
        catch (Exception ex)
        {
            protokoll.Report("Lokaler Quellstand nicht lesbar: " + ex.Message);
            return false;
        }
    }

    /// <summary>True if a build source below the folder (bin/obj excluded) is newer than the build.</summary>
    internal static bool QuelleNeuerAlsBuild(string ordner, DateTime buildZeitUtc)
    {
        if (!Directory.Exists(ordner)) return false;
        var trenner = Path.DirectorySeparatorChar;
        return BauQuellen
            .SelectMany(m => Directory.EnumerateFiles(ordner, m, SearchOption.AllDirectories))
            .Where(d => !d.Contains(trenner + "bin" + trenner, StringComparison.OrdinalIgnoreCase)
                        && !d.Contains(trenner + "obj" + trenner, StringComparison.OrdinalIgnoreCase))
            .Any(d => File.GetLastWriteTimeUtc(d) > buildZeitUtc);
    }

    /// <summary>
    /// Maps the script's status line onto a card state. "started" alone is not proof of an update:
    /// update-launcher.ps1 also says "started" when the build was already current and it only
    /// launched the existing exe. Reported as a finished update, the unchanged fingerprint then
    /// turned it into "not verified", the card offered the same update again -- the endless loop.
    /// </summary>
    internal static PruefErgebnis Auswerten(BefehlErgebnis lauf, string? statusPraefix, bool buildUnveraendert)
    {
        // How the process ended outranks what it printed: a "started" written before a timeout,
        // a cancel or a tree that could not be ended fully is never a finished update.
        if (Kommandozeile.UnsauberesEnde(lauf) is { } unsauber) return unsauber;

        var status = StatusLesen(lauf.Ausgabe, statusPraefix);
        if (status == "cancelled")
            return new PruefErgebnis(UpdateZustand.Abgebrochen,
                Meldung: "Du hast im Ja/Nein-Fenster auf 'Nein' geklickt – es wurde nichts geändert.", Protokoll: lauf.Ausgabe);
        if (status == "no-answer")
            return new PruefErgebnis(UpdateZustand.Abgebrochen,
                Meldung: "Kein Klick im Zeitfenster – es wurde nichts geändert.", Protokoll: lauf.Ausgabe);
        if (status == "already-current")
            return new PruefErgebnis(UpdateZustand.Aktuell, Meldung: "War bereits aktuell.", Protokoll: lauf.Ausgabe);
        if (status == "started")
            return buildUnveraendert
                ? new PruefErgebnis(UpdateZustand.Aktuell,
                    Meldung: "Der Build war bereits aktuell – das Skript hat ihn nur gestartet.", Protokoll: lauf.Ausgabe)
                : new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Neue Version gebaut und gestartet.", Protokoll: lauf.Ausgabe);
        if (lauf.ExitCode == 0 && string.IsNullOrWhiteSpace(statusPraefix))
            return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Skript erfolgreich durchgelaufen.", Protokoll: lauf.Ausgabe);
        if (lauf.ExitCode == 0)
            // The script promises a status line; without it the outcome is unknown, whatever the
            // exit code says. The fingerprint comparison afterwards decides, never this guess.
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: "Skript endete ohne Statuszeile – das Ergebnis wird am Build-Stand geprüft.", Protokoll: lauf.Ausgabe);

        return new PruefErgebnis(UpdateZustand.Fehler,
            Meldung: "Das Skript endete mit Code " + lauf.ExitCode + ".", Protokoll: lauf.Ausgabe);
    }

    /// <summary>
    /// The csproj version only moves when someone bumps it, so it alone would report a false
    /// failure after a rebuild. The write time of the built exe is what actually changes here.
    /// </summary>
    public Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        if (string.IsNullOrWhiteSpace(exe) || !File.Exists(exe)) return Task.FromResult("");

        try
        {
            var zeit = File.GetLastWriteTimeUtc(exe).ToString("yyyy-MM-dd HH:mm:ss");
            var version = InstallierteVersion(eintrag);
            return Task.FromResult((string.IsNullOrWhiteSpace(version) ? "" : version + " vom ") + zeit);
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "provider", Schwere.Debug);
            return Task.FromResult("");
        }
    }

    /// <summary>
    /// Only files that end up in the build count. The folders also carry runtime and profile data
    /// (OpenLauncher/models.json, Profiles/**) that other machines commit all day; counted as
    /// "new commits", they announced an update the script then rightly refused to build -- and
    /// the card asked for the same update again. Same file kinds update-launcher.ps1 checks
    /// against the exe, plus the MSBuild companions that change a build just as much.
    /// </summary>
    internal static readonly string[] BauQuellen =
        { "*.cs", "*.xaml", "*.csproj", "*.props", "*.targets", "*.resx", "*.manifest" };

    /// <returns>
    /// Commits on origin/main that touch build sources of the folder; 0 without a folder; null
    /// when git could not answer -- never a silent 0, which would read as "current".
    /// </returns>
    internal static async Task<int?> HinterstandAsync(string repoWurzel, string? ordner,
                                                      IProgress<string>? protokoll, CancellationToken abbruch)
    {
        if (string.IsNullOrWhiteSpace(ordner)) return 0;

        var holen = await Kommandozeile.AusfuehrenAsync("git", "fetch --quiet", TimeSpan.FromMinutes(3), repoWurzel, abbruch);
        if (holen.Abgelaufen || holen.ExitCode != 0)
        {
            // A stale origin/main can answer "0 behind" while the real remote is ahead -- that
            // would read as "current". Without a fresh fetch the state is unknown, full stop.
            protokoll?.Report("git fetch fehlgeschlagen (" + (holen.Abgelaufen ? "Zeitlimit" : "Code " + holen.ExitCode)
                              + ") – Remote-Stand unbekannt: " + holen.Ausgabe);
            return null;
        }

        var muster = string.Join(" ", BauQuellen.Select(m => "\":(glob)" + ordner.Trim('/', '\\') + "/**/" + m + "\""));
        var zaehl = await Kommandozeile.AusfuehrenAsync("git",
            "rev-list --count HEAD..origin/main -- " + muster,
            TimeSpan.FromMinutes(2), repoWurzel, abbruch);

        int? anzahl = !zaehl.Abgelaufen && zaehl.ExitCode == 0 && int.TryParse(zaehl.Ausgabe.Trim(), out var z) ? z : null;
        protokoll?.Report("git rev-list HEAD..origin/main -- " + ordner + " (Quellcode) -> "
                          + (anzahl?.ToString() ?? "FEHLER Code " + zaehl.ExitCode + ": " + zaehl.Ausgabe));
        return anzahl;
    }

    /// <summary>rebuild-overlay.ps1 requires PowerShell 7; fall back only if pwsh is missing.</summary>
    private static string PwshPfad()
    {
        var kandidaten = new[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PowerShell", "7", "pwsh.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Microsoft", "WindowsApps", "pwsh.exe")
        };
        return kandidaten.FirstOrDefault(File.Exists) ?? "powershell.exe";
    }

    internal static string StatusLesen(string ausgabe, string? praefix)
    {
        if (string.IsNullOrWhiteSpace(praefix)) return "";
        foreach (var zeile in ausgabe.Split('\n'))
        {
            var index = zeile.IndexOf(praefix, StringComparison.OrdinalIgnoreCase);
            if (index < 0) continue;
            var rest = zeile.Substring(index + praefix.Length).Trim();
            return rest.Split(' ', StringSplitOptions.RemoveEmptyEntries).FirstOrDefault() ?? "";
        }
        return "";
    }

    private static string InstallierteVersion(ProgrammEintrag eintrag)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfadWirksam);
        if (string.IsNullOrWhiteSpace(exe) || !File.Exists(exe)) return "";
        try
        {
            var info = System.Diagnostics.FileVersionInfo.GetVersionInfo(exe);
            return info.FileVersion ?? info.ProductVersion ?? "";
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "provider", Schwere.Debug);
            return "";
        }
    }

    /// <summary>Reads the csproj Version element -- the single source of truth for these tools.</summary>
    private static string QuellVersion(ProgrammEintrag eintrag)
    {
        if (string.IsNullOrWhiteSpace(eintrag.ProjektDatei)) return "";
        var datei = Path.Combine(Pfade.RepoWurzel, eintrag.ProjektDatei.Replace('/', Path.DirectorySeparatorChar));
        if (!File.Exists(datei)) return "";
        try
        {
            var treffer = Regex.Match(File.ReadAllText(datei), @"<Version>\s*([^<\s]+)\s*</Version>");
            return treffer.Success ? treffer.Groups[1].Value : "";
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "provider", Schwere.Debug);
            return "";
        }
    }
}
