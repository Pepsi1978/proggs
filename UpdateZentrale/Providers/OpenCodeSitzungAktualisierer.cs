using System.Diagnostics;
using System.IO;
using System.Text.Json;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// OpenCode with the Windows fix, updated by a programming session instead of by the build script
/// alone. The script stops whenever the fix no longer applies to the new upstream source, and
/// merging the fix into the new version is work only a CLI can do. So an open update starts an
/// OpenLauncher session (Opus 5.5, profile minimal, fast mode, effort high) through the launcher's
/// windowless entry point (--jarvis-auftrag), with the task as its first message.
///
/// The card then shows a pending update: the session runs on its own, and the next check confirms
/// it once current.json points at the new version (UpdateKette.StagedUrteil). Check and
/// fingerprint are those of <see cref="OpenCodeFixAktualisierer"/>.
/// </summary>
public sealed class OpenCodeSitzungAktualisierer : IAktualisierer
{
    private readonly OpenCodeFixAktualisierer _pruefer = new();

    public string Art => "opencodesitzung";

    internal const string Profil = "minimal";
    internal const string Modus = "schnell";
    internal const string Modell = "claude-opus-5-5[1m]";
    internal const string Effort = "high";
    internal const string SitzungsName = "UpdateZentrale: OpenCode-Update";

    /// <summary>A session started for the same version this recently is not started a second time.</summary>
    internal static readonly TimeSpan Sperrzeit = TimeSpan.FromHours(3);

    private static string MarkerDatei => Path.Combine(Pfade.BenutzerOrdner, "opencode-sitzung.json");

    internal static string LauncherExe => Path.Combine(Pfade.RepoWurzel, "OpenLauncher", "bin", "Release",
        "net8.0-windows10.0.19041.0", "win-x64", "OpenLauncher.exe");

    public Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
        => _pruefer.PruefenAsync(eintrag, protokoll, abbruch);

    public Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
        => _pruefer.FingerabdruckAsync(eintrag, abbruch);

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var npm = await CliAktualisierer.NpmVersionAsync(eintrag.NpmPaket ?? "opencode-ai", protokoll, abbruch);
        if (!npm.Erfolg)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: npm.Problem ?? "Registry-Abfrage fehlgeschlagen.");

        var aktiv = OpenCodeFixAktualisierer.AktiveVersion();
        if (aktiv is { } a && CliAktualisierer.Vergleiche(npm.Version, a.Upstream) <= 0)
            return new PruefErgebnis(UpdateZustand.Aktuell, a.Custom, npm.Version, "Bereits aktuell (mit Windows-Fix).");

        if (LetzterStart() is { } frueher && frueher.Version == npm.Version && DateTime.Now - frueher.Zeit < Sperrzeit)
        {
            protokoll.Report("Für " + npm.Version + " läuft schon eine Sitzung seit " + frueher.Zeit.ToString("HH:mm") + " Uhr – keine zweite.");
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: "Die OpenLauncher-Sitzung für " + npm.Version + " läuft seit " + frueher.Zeit.ToString("HH:mm")
                         + " Uhr. Die Karte bestätigt das Update, sobald die neue Version aktiv ist.",
                ErstNachNeustart: true);
        }

        if (!File.Exists(LauncherExe))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "OpenLauncher nicht gefunden: " + LauncherExe);

        var ordner = Directory.CreateDirectory(Path.Combine(Pfade.BenutzerOrdner, "auftraege")).FullName;
        var datei = Path.Combine(ordner, "opencode-" + npm.Version + "-" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + ".md");
        await File.WriteAllTextAsync(datei, Auftrag(npm.Version, aktiv?.Custom), abbruch);

        var args = Argumente(datei);
        protokoll.Report("OpenLauncher.exe " + args);

        // Shell start, no redirected pipes: the launcher opens a terminal that would inherit them,
        // and the wait would then last until that session ends (see memory "erbt Agenten-Pipe").
        using var prozess = Process.Start(new ProcessStartInfo(LauncherExe, args)
        {
            UseShellExecute = true,
            WorkingDirectory = Path.GetDirectoryName(LauncherExe)!
        });
        if (prozess is null)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Der OpenLauncher ließ sich nicht starten.");

        using var zeit = CancellationTokenSource.CreateLinkedTokenSource(abbruch);
        zeit.CancelAfter(TimeSpan.FromMinutes(2));
        try
        {
            await prozess.WaitForExitAsync(zeit.Token);
        }
        catch (OperationCanceledException) when (!abbruch.IsCancellationRequested)
        {
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "Der OpenLauncher hat sich nach zwei Minuten nicht zurückgemeldet – ob die Sitzung läuft, ist unklar.");
        }

        if (prozess.ExitCode != 0)
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "Der OpenLauncher konnte die Sitzung nicht starten (Code " + prozess.ExitCode + ") – Einzelheiten im OpenLauncher-Log.");

        StartMerken(npm.Version);
        protokoll.Report("Sitzung gestartet, Auftrag: " + datei);
        return new PruefErgebnis(UpdateZustand.Fertig,
            Meldung: "OpenLauncher-Sitzung gestartet (Opus 5.5, minimal, Schnellmodus, high): sie baut " + npm.Version
                     + " mit dem Windows-Fix. Die Karte bestätigt das Update, sobald die neue Version aktiv ist.",
            ErstNachNeustart: true);
    }

    internal static string Argumente(string auftragsDatei)
        => "--jarvis-auftrag \"" + auftragsDatei + "\" --name \"" + SitzungsName + "\" --profil " + Profil
           + " --modus " + Modus + " --modell \"" + Modell + "\" --effort " + Effort
           + " --ordner \"" + Pfade.RepoWurzel + "\"";

    /// <summary>What the card's log header shows.</summary>
    public static string Befehlsbeschreibung => "OpenLauncher.exe --jarvis-auftrag <auftrag> --profil " + Profil
                                                + " --modus " + Modus + " --modell " + Modell + " --effort " + Effort;

    /// <summary>The first message of the session: goal, the one build path, and when it is done.</summary>
    internal static string Auftrag(string version, string? aktiv) =>
        "Auftrag von der UpdateZentrale: OpenCode CLI aktualisieren\n\n"
        + "Ziel: Die neueste OpenCode-Version " + version + " läuft mit unserem Windows-Fix "
        + "(aktiv ist bisher " + (aktiv ?? "keine Fix-Version") + ").\n\n"
        + "1. Baue sie mit dem vorhandenen Skript: `pwsh -NoProfile -File ~/proggs/opencode-setup/build-install-windows-mousefix.ps1 -Version " + version + "`.\n"
        + "2. Bricht das Skript ab, weil die Patches unter `opencode-setup/patches` nicht mehr zur neuen Version passen, "
        + "verschmilz den Windows-Fix mit der neuen Version: Patches an den neuen Quellstand anpassen, die Patch-Revision im Skript erhöhen, erneut bauen. "
        + "Keine Fix-Funktion weglassen (alle `capabilities` in current.json müssen wieder `present` sein).\n"
        + "3. Meldet das Skript `OPENCODE_UPDATE_STATUS=busy`, läuft schon ein Bau (meist der OpenLauncher): kurz warten und erneut starten.\n"
        + "4. Fertig ist es erst, wenn `~/.local/share/opencode-mousefix/current.json` unter `active.upstreamVersion` " + version + " zeigt.\n\n"
        + "Laufende OpenCode-Sitzungen nicht beenden, die bisherige Version bleibt bis zur Aktivierung nutzbar. "
        + "Danach den Pflicht-Abschluss nach den Profilregeln (Version, Commit, Push), falls am Skript oder an den Patches etwas geändert wurde.\n";

    private readonly record struct Start(string Version, DateTime Zeit);

    private static Start? LetzterStart()
    {
        try
        {
            if (!File.Exists(MarkerDatei)) return null;
            using var dok = JsonDocument.Parse(File.ReadAllText(MarkerDatei));
            var w = dok.RootElement;
            return new Start(w.GetProperty("version").GetString() ?? "", w.GetProperty("zeit").GetDateTime());
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "provider", Schwere.Debug);
            return null;
        }
    }

    private static void StartMerken(string version)
    {
        try
        {
            Directory.CreateDirectory(Pfade.BenutzerOrdner);
            File.WriteAllText(MarkerDatei, JsonSerializer.Serialize(new { version, zeit = DateTime.Now }));
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "provider", Schwere.Warnung);
        }
    }
}
