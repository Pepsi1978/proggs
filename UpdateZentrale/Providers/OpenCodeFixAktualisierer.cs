using System.IO;
using System.Text.Json;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// OpenCode as the OpenLauncher starts it: not the npm package, but the own build with the Windows
/// fix under ~/.local/share/opencode-mousefix (pointer file current.json). The update is the same
/// script the OpenLauncher runs (opencode-setup/build-install-windows-mousefix.ps1): newest
/// upstream tag, patches applied, tested, built, activated. When the patches no longer apply, the
/// script stops without touching the active version -- re-basing them is a job for the CLI.
/// </summary>
public sealed class OpenCodeFixAktualisierer : IAktualisierer
{
    public string Art => "opencodefix";

    private static string Wurzel => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".local", "share", "opencode-mousefix");

    private static string ZeigerDatei => Path.Combine(Wurzel, "current.json");
    private static string ZustandDatei => Path.Combine(Wurzel, "update-state.json");

    private static string Skript => Path.Combine(Pfade.RepoWurzel, "opencode-setup", "build-install-windows-mousefix.ps1");

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var aktiv = AktiveVersion();
        if (aktiv is null)
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, Meldung: "Kein Windows-Fix-Build aktiv: " + ZeigerDatei);

        protokoll.Report("Aktiv: " + aktiv.Value.Custom);
        var npm = await CliAktualisierer.NpmVersionAsync(eintrag.NpmPaket ?? "opencode-ai", protokoll, abbruch);
        if (!npm.Erfolg)
            return new PruefErgebnis(UpdateZustand.Fehler, aktiv.Value.Custom, "", npm.Problem ?? "Registry-Abfrage fehlgeschlagen.");

        if (CliAktualisierer.Vergleiche(npm.Version, aktiv.Value.Upstream) <= 0)
            return new PruefErgebnis(UpdateZustand.Aktuell, aktiv.Value.Custom, npm.Version, "Auf dem neuesten Stand (mit Windows-Fix).");

        var meldung = "Neue Version " + npm.Version + " – wird mit dem Windows-Fix gebaut.";
        if (LetzterZustand() is { Status: "failed" } z && z.Kandidat == npm.Version)
            meldung += " Letzter Bauversuch: " + z.Nachricht;
        return new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, aktiv.Value.Custom, npm.Version, meldung);
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        if (!File.Exists(Skript))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Bauskript nicht gefunden: " + Skript);

        // The checked version is passed on: -Version skips the script's retry pause (which would
        // answer "deferred" with exit code 0) but keeps its "already active" short cut, and the
        // script needs no npm of its own.
        var npm = await CliAktualisierer.NpmVersionAsync(eintrag.NpmPaket ?? "opencode-ai", protokoll, abbruch);
        if (!npm.Erfolg)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: npm.Problem ?? "Registry-Abfrage fehlgeschlagen.");

        var args = Argumente + " -Version " + npm.Version;
        protokoll.Report("pwsh " + args);
        var start = DateTime.UtcNow.AddSeconds(-5);
        var lauf = await Kommandozeile.AusfuehrenAsync(PwshPfad(), args,
            TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), Path.GetDirectoryName(Skript), abbruch: abbruch);
        protokoll.Report(lauf.Ausgabe);

        if (Kommandozeile.UnsauberesEnde(lauf) is { } unsauber) return unsauber;
        if (lauf.Ausgabe.Contains("OPENCODE_UPDATE_STATUS=busy", StringComparison.Ordinal))
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "Gerade baut schon ein anderer Lauf (meist der OpenLauncher) – kurz warten und erneut aktualisieren.",
                Protokoll: lauf.Ausgabe);

        // Only a state this run wrote counts; an older one would name somebody else's failure.
        var zustand = LetzterZustand() is { } z && z.Zeit >= start ? z : (BauZustand?)null;
        if (lauf.ExitCode == 0 && zustand?.Status is "installed" or "up-to-date")
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: zustand.Value.Nachricht + " Laufende Sitzungen behalten die alte Version.",
                Protokoll: lauf.Ausgabe);

        var grund = zustand is { Status: "failed" } ? zustand.Value.Nachricht : "Das Bauskript endete mit Code " + lauf.ExitCode + ".";
        if (IstPatchKonflikt(grund))
            grund += " Der Windows-Fix muss an die neue Version angepasst werden – das Update in OpenCode selbst beauftragen. Die bisherige Version bleibt aktiv.";
        return new PruefErgebnis(UpdateZustand.Fehler, Meldung: grund, Protokoll: lauf.Ausgabe);
    }

    public Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
        => Task.FromResult(AktiveVersion()?.Custom ?? "");

    /// <summary>The active build from current.json -- only when its exe really exists.</summary>
    internal static (string Upstream, string Custom)? AktiveVersion()
    {
        try
        {
            using var dok = JsonDocument.Parse(File.ReadAllText(ZeigerDatei));
            var aktiv = dok.RootElement.GetProperty("active");
            var exe = Path.GetFullPath(Path.Combine(Wurzel, aktiv.GetProperty("relativeExe").GetString() ?? ""));
            if (!exe.StartsWith(Wurzel, StringComparison.OrdinalIgnoreCase) || !File.Exists(exe)) return null;
            return (aktiv.GetProperty("upstreamVersion").GetString() ?? "", aktiv.GetProperty("customVersion").GetString() ?? "");
        }
        catch
        {
            return null;
        }
    }

    private static string Argumente => "-NoLogo -NoProfile -ExecutionPolicy Bypass -File \"" + Skript + "\"";

    /// <summary>What the card's log header shows -- the same string the run uses.</summary>
    public static string Befehlsbeschreibung => "pwsh " + Argumente + " -Version <neueste>";

    /// <summary>The script's messages for patches that no longer fit the new upstream source.</summary>
    internal static bool IstPatchKonflikt(string grund)
        => new[] { "nicht kompatibel", "konnte nicht angewendet", "nicht sicher patchbar", "nicht sicher anwendbar", "weder vorhanden noch sicher" }
            .Any(m => grund.Contains(m, StringComparison.OrdinalIgnoreCase));

    private readonly record struct BauZustand(string Status, string Nachricht, string Kandidat, DateTime Zeit);

    private static BauZustand? LetzterZustand()
    {
        try
        {
            using var dok = JsonDocument.Parse(File.ReadAllText(ZustandDatei));
            var w = dok.RootElement;
            string Feld(string n) => w.TryGetProperty(n, out var f) && f.ValueKind == JsonValueKind.String ? f.GetString() ?? "" : "";
            var zeit = DateTime.TryParse(Feld("lastAttemptUtc"), System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.AdjustToUniversal | System.Globalization.DateTimeStyles.AssumeUniversal, out var t)
                ? t : DateTime.MinValue;
            return new BauZustand(Feld("status"), Feld("message"), Feld("candidateVersion"), zeit);
        }
        catch
        {
            return null;
        }
    }

    private static string PwshPfad()
    {
        var programm = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PowerShell", "7", "pwsh.exe");
        return File.Exists(programm) ? programm : "pwsh.exe";
    }
}
