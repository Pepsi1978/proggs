using System.Diagnostics;
using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

public sealed class KommandozeileTests
{
    /// <summary>
    /// The tool starts a long-lived child that inherits the output pipe and exits itself -- exactly
    /// what update-launcher.ps1 does with the freshly built launcher. The run must end right after
    /// the tool, with its output, instead of waiting for the child.
    /// </summary>
    [Fact]
    public async Task Lauf_endet_obwohl_ein_Kindprozess_die_Pipe_haelt()
    {
        var skript = "$p = [Diagnostics.ProcessStartInfo]::new('ping.exe', '-n 60 127.0.0.1'); "
                     + "$p.UseShellExecute = $false; $k = [Diagnostics.Process]::Start($p); "
                     + "Write-Output \"KIND=$($k.Id)\"; Write-Output 'LAUNCHER_UPDATE_STATUS=started'; exit 0";
        var uhr = Stopwatch.StartNew();

        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe",
            "-NoProfile -Command \"" + skript.Replace("\"", "\\\"") + "\"", TimeSpan.FromSeconds(50));
        uhr.Stop();

        try
        {
            Assert.False(lauf.Abgelaufen);
            Assert.Equal(0, lauf.ExitCode);
            Assert.True(lauf.PipeGehalten);
            Assert.Contains("LAUNCHER_UPDATE_STATUS=started", lauf.Ausgabe);
            Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(25), "Lauf dauerte " + uhr.Elapsed);
        }
        finally
        {
            var id = lauf.Ausgabe.Split('\n').FirstOrDefault(z => z.StartsWith("KIND="))?.Substring(5).Trim();
            if (int.TryParse(id, out var pid)) try { Process.GetProcessById(pid).Kill(); } catch { }
        }
    }

    [Fact]
    public async Task Zeitlimit_beendet_den_Lauf_und_meldet_es()
    {
        var uhr = Stopwatch.StartNew();
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe",
            "-NoProfile -Command \"Write-Output vorher; Start-Sleep -Seconds 60\"", TimeSpan.FromSeconds(3));

        Assert.True(lauf.Abgelaufen);
        Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(20), "Lauf dauerte " + uhr.Elapsed);
    }
}

public sealed class RepoSkriptTests
{
    private static BefehlErgebnis Lauf(string ausgabe, int code = 0, bool abgelaufen = false)
        => new(code, ausgabe, abgelaufen);

    [Fact]
    public void Started_mit_unveraendertem_Build_ist_kein_Update_und_kein_Fehler()
    {
        var e = RepoSkriptAktualisierer.Auswerten(
            Lauf("LAUNCHER_UPDATE_INFO=Build ist bereits aktuell\nLAUNCHER_UPDATE_STATUS=started VERSION=1.2.3 PID=5"),
            "LAUNCHER_UPDATE_STATUS=", buildUnveraendert: true);
        Assert.Equal(UpdateZustand.Aktuell, e.Zustand);
    }

    [Fact]
    public void Started_mit_neuem_Build_ist_fertig()
        => Assert.Equal(UpdateZustand.Fertig, RepoSkriptAktualisierer.Auswerten(
            Lauf("LAUNCHER_UPDATE_STATUS=started VERSION=1.2.4"), "LAUNCHER_UPDATE_STATUS=", false).Zustand);

    [Theory]
    [InlineData("LAUNCHER_UPDATE_STATUS=cancelled", UpdateZustand.Abgebrochen)]
    [InlineData("LAUNCHER_UPDATE_STATUS=no-answer (kein Klick)", UpdateZustand.Abgebrochen)]
    [InlineData("LAUNCHER_UPDATE_STATUS=already-current VERSION=1", UpdateZustand.Aktuell)]
    public void Statuszeilen_werden_wahrheitsgetreu_zugeordnet(string ausgabe, UpdateZustand erwartet)
        => Assert.Equal(erwartet, RepoSkriptAktualisierer.Auswerten(Lauf(ausgabe), "LAUNCHER_UPDATE_STATUS=", false).Zustand);

    [Fact]
    public void Abgelaufen_ohne_Status_ist_Fehler()
        => Assert.Equal(UpdateZustand.Fehler, RepoSkriptAktualisierer.Auswerten(
            Lauf("", -1, abgelaufen: true), "LAUNCHER_UPDATE_STATUS=", false).Zustand);

    [Fact]
    public void Exitcode_ungleich_null_ohne_Status_ist_Fehler()
        => Assert.Equal(UpdateZustand.Fehler, RepoSkriptAktualisierer.Auswerten(
            Lauf("Der Release-Build ist fehlgeschlagen", 1), "LAUNCHER_UPDATE_STATUS=", false).Zustand);

    [Fact]
    public void Fehlgeschlagene_Git_Abfrage_ist_nie_aktuell()
        => Assert.Equal(UpdateZustand.Unbekannt,
            RepoSkriptAktualisierer.Einordnen("1.2.3.0", "1.2.3", hinterstand: null, quellstandNeuer: false, hatRepoOrdner: true).Zustand);

    [Theory]
    [InlineData(null, false, false)]   // git unbekannt, lokal nichts Neues -> kein blinder Start
    [InlineData(null, true, true)]     // git unbekannt, lokal nachweislich neuer -> Start erlaubt
    [InlineData(0, false, true)]       // git beantwortet -> normaler Ablauf
    [InlineData(3, false, true)]
    public void Skriptstart_bei_unbekanntem_Git_nur_mit_lokal_neuer_Quelle(int? hinterstand, bool lokalNeuer, bool erwartet)
        => Assert.Equal(erwartet, RepoSkriptAktualisierer.DarfSkriptStarten(hinterstand, hatRepoOrdner: true, lokalNeuer));

    [Fact]
    public void Ohne_Repo_Ordner_ist_Git_egal()
        => Assert.True(RepoSkriptAktualisierer.DarfSkriptStarten(0, hatRepoOrdner: false, lokaleQuelleNeuer: false));

    [Theory]
    [InlineData("1.24.50", "1.24.5.0", true)]
    [InlineData("1.24.5", "1.24.50.0", false)]
    [InlineData("1.24.48", "1.24.48.0", false)]
    [InlineData("1.2", "1.24.48.0", false)]
    [InlineData("1.3.0", "1.2.9.0", true)]
    public void Versionsvergleich_ist_numerisch(string quelle, string gebaut, bool neuer)
        => Assert.Equal(neuer, CliAktualisierer.Vergleiche(quelle, gebaut) > 0);
}

/// <summary>
/// Real git, throwaway repos: a bare "origin" and a clone. Proves that only commits touching
/// build sources count as pending, and that a broken query is reported as unknown.
/// </summary>
public sealed class HinterstandTests : IDisposable
{
    private readonly string _wurzel = Path.Combine(Path.GetTempPath(), "uz-test-" + Guid.NewGuid().ToString("N"));

    public void Dispose()
    {
        try
        {
            foreach (var datei in Directory.EnumerateFiles(_wurzel, "*", SearchOption.AllDirectories))
                File.SetAttributes(datei, FileAttributes.Normal);   // git objects are read-only
            Directory.Delete(_wurzel, true);
        }
        catch { }
    }

    private static void Git(string ordner, string args)
    {
        var p = Process.Start(new ProcessStartInfo("git", args)
        {
            WorkingDirectory = ordner, UseShellExecute = false, CreateNoWindow = true,
            RedirectStandardOutput = true, RedirectStandardError = true
        })!;
        p.StandardOutput.ReadToEnd();
        var fehler = p.StandardError.ReadToEnd();
        p.WaitForExit();
        if (p.ExitCode != 0) throw new InvalidOperationException("git " + args + ": " + fehler);
    }

    private (string klon, string arbeit) Aufbauen()
    {
        var origin = Path.Combine(_wurzel, "origin.git");
        var arbeit = Path.Combine(_wurzel, "arbeit");
        var klon = Path.Combine(_wurzel, "klon");
        Directory.CreateDirectory(origin);
        Git(origin, "init --bare -b main");
        Git(_wurzel, "clone origin.git arbeit");
        Git(arbeit, "config user.email t@t");
        Git(arbeit, "config user.name t");
        Directory.CreateDirectory(Path.Combine(arbeit, "Werkzeug", "Unter"));
        File.WriteAllText(Path.Combine(arbeit, "Werkzeug", "App.cs"), "1");
        Git(arbeit, "add -A");
        Git(arbeit, "commit -m start");
        Git(arbeit, "push origin main");
        Git(_wurzel, "clone origin.git klon");
        return (klon, arbeit);
    }

    private static void Commit(string arbeit, string relativ, string inhalt)
    {
        var pfad = Path.Combine(arbeit, relativ);
        Directory.CreateDirectory(Path.GetDirectoryName(pfad)!);
        File.WriteAllText(pfad, inhalt);
        Git(arbeit, "add -A");
        Git(arbeit, "commit -m x");
        Git(arbeit, "push origin main");
    }

    [Fact]
    public async Task Nur_Quellcode_Commits_zaehlen()
    {
        var (klon, arbeit) = Aufbauen();

        Commit(arbeit, "Werkzeug/models.json", "{}");
        Commit(arbeit, "Werkzeug/Profiles/x/settings.json", "{}");
        Commit(arbeit, "Anderes/Code.cs", "x");
        Assert.Equal(0, await RepoSkriptAktualisierer.HinterstandAsync(klon, "Werkzeug", null, CancellationToken.None));

        Commit(arbeit, "Werkzeug/App.cs", "2");
        Commit(arbeit, "Werkzeug/Unter/Seite.xaml", "<x/>");
        Assert.Equal(2, await RepoSkriptAktualisierer.HinterstandAsync(klon, "Werkzeug", null, CancellationToken.None));
    }

    /// <summary>
    /// origin/main exists locally and equals HEAD, so rev-list would answer 0 -- but the fetch
    /// fails (remote gone). The stale 0 must not surface as "current".
    /// </summary>
    [Fact]
    public async Task Gescheiterter_Fetch_mit_altem_origin_ist_unbekannt_nicht_aktuell()
    {
        var (klon, _) = Aufbauen();
        Git(klon, "remote set-url origin \"" + Path.Combine(_wurzel, "gibt-es-nicht.git") + "\"");

        var hinterstand = await RepoSkriptAktualisierer.HinterstandAsync(klon, "Werkzeug", null, CancellationToken.None);

        Assert.Null(hinterstand);
        Assert.NotEqual(UpdateZustand.Aktuell,
            RepoSkriptAktualisierer.Einordnen("1.2.3.0", "1.2.3", hinterstand, quellstandNeuer: false, hatRepoOrdner: true).Zustand);
    }

    [Fact]
    public void Lokale_Quelle_neuer_als_Build_wird_erkannt_bin_und_obj_zaehlen_nicht()
    {
        var ordner = Path.Combine(_wurzel, "quelle");
        Directory.CreateDirectory(Path.Combine(ordner, "bin"));
        var code = Path.Combine(ordner, "App.cs");
        var gebaut = Path.Combine(ordner, "bin", "Gebaut.cs");
        File.WriteAllText(code, "x");
        File.WriteAllText(gebaut, "x");
        var build = DateTime.UtcNow;

        File.SetLastWriteTimeUtc(code, build.AddMinutes(-5));
        File.SetLastWriteTimeUtc(gebaut, build.AddMinutes(5));
        Assert.False(RepoSkriptAktualisierer.QuelleNeuerAlsBuild(ordner, build));

        File.SetLastWriteTimeUtc(code, build.AddMinutes(5));
        Assert.True(RepoSkriptAktualisierer.QuelleNeuerAlsBuild(ordner, build));
    }

    [Fact]
    public async Task Kaputte_Abfrage_liefert_null_statt_null_Commits()
    {
        var keinRepo = Path.Combine(_wurzel, "leer");
        Directory.CreateDirectory(keinRepo);
        Assert.Null(await RepoSkriptAktualisierer.HinterstandAsync(keinRepo, "Werkzeug", null, CancellationToken.None));
    }
}

public sealed class FingerabdruckTests
{
    [Theory]
    [InlineData("1.2.3", "", LaufErgebnis.NichtVerifiziert)]   // nachher nicht lesbar
    [InlineData("", "1.2.4", LaufErgebnis.NichtVerifiziert)]   // vorher nicht lesbar
    [InlineData("", "", LaufErgebnis.NichtVerifiziert)]
    [InlineData("1.2.3", "1.2.3", LaufErgebnis.NichtVerifiziert)]
    [InlineData("1.2.3", "1.2.4", LaufErgebnis.Erfolgreich)]    // echter Wechsel
    public void Erfolg_nur_mit_zwei_vorhandenen_und_verschiedenen_Staenden(string vorher, string nachher, LaufErgebnis erwartet)
        => Assert.Equal(erwartet, UpdateZentrale.ViewModels.ProgrammViewModel.FingerabdruckUrteil(vorher, nachher).Ergebnis);

    private static BefehlErgebnis Lauf(string ausgabe, int code = 0, bool abgelaufen = false) => new(code, ausgabe, abgelaufen);

    [Fact]
    public void Abgebrochener_DryRun_ist_nicht_nichts_offen()
    {
        Assert.Equal("", CliAktualisierer.FingerabdruckAus(null, Lauf("", -1, abgelaufen: true)).Fingerabdruck);
        Assert.Equal("", CliAktualisierer.FingerabdruckAus(null, Lauf("Fehler", 2)).Fingerabdruck);
        Assert.NotNull(CliAktualisierer.FingerabdruckAus(null, Lauf("Fehler", 2)).Problem);
    }

    [Fact]
    public void Sauberer_DryRun_liefert_Plan_oder_nichts_offen()
    {
        Assert.Equal("nichts offen", CliAktualisierer.FingerabdruckAus(null, Lauf("All up-to-date.")).Fingerabdruck);
        Assert.Equal("llama.cpp 2.41.0 → 2.42.0",
            CliAktualisierer.FingerabdruckAus(null, Lauf("Update Plan:\n  llama.cpp 2.41.0 → 2.42.0\n")).Fingerabdruck);
    }

    [Fact]
    public void Gescheiterte_Versionsabfrage_faellt_nicht_auf_DryRun_zurueck()
    {
        Assert.Equal("", CliAktualisierer.FingerabdruckAus(Lauf("2.1.278 (Claude Code)", 1), Lauf("")).Fingerabdruck);
        Assert.Equal("", CliAktualisierer.FingerabdruckAus(Lauf("", -1, abgelaufen: true), null).Fingerabdruck);
        Assert.Equal("2.1.278", CliAktualisierer.FingerabdruckAus(Lauf("2.1.278 (Claude Code)"), null).Fingerabdruck);
    }
}
