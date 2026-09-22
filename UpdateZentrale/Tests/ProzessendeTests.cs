using System.Diagnostics;
using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

[CollectionDefinition("Prozessende", DisableParallelization = true)]
public sealed class ProzessendeSammlung { }

/// <summary>
/// Real process trees with a private child: a copy of ping.exe named uzfix.exe in a temp folder
/// of its own. Only processes whose image lies in THAT folder are ever cleaned up -- never by name.
/// Runs alone (no parallel tests) so the reader counter is meaningful.
/// </summary>
[Collection("Prozessende")]
public sealed class ProzessendeTests : IDisposable
{
    private readonly string _ordner = Path.Combine(Path.GetTempPath(), "uz-fix-" + Guid.NewGuid().ToString("N"));
    private readonly string _kind;

    public ProzessendeTests()
    {
        Directory.CreateDirectory(_ordner);
        _kind = Path.Combine(_ordner, "uzfix.exe");
        File.Copy(Path.Combine(Environment.SystemDirectory, "PING.EXE"), _kind);
    }

    public void Dispose()
    {
        foreach (var pid in EigeneKinder()) ProzessbaumTests.Aufraeumen(pid);
        try { Directory.Delete(_ordner, true); } catch { }
    }

    /// <summary>Only processes started from this test's private copy.</summary>
    private List<int> EigeneKinder()
    {
        var alle = Process.GetProcessesByName("uzfix");
        try
        {
            return alle.Where(p => Prozesspfad.GleicheDatei(Prozesspfad.Lesen(p.Id), _kind)).Select(p => p.Id).ToList();
        }
        finally
        {
            foreach (var p in alle) p.Dispose();
        }
    }

    /// <summary>Root powershell starts the private child WITHOUT pipe inheritance and blocks.</summary>
    private string BlockierenderBaum()
        => "-NoProfile -Command \"$k = Start-Process '" + _kind + "' -ArgumentList '-n 120 127.0.0.1' -WindowStyle Hidden -PassThru; "
           + "Write-Output ('ROOT=' + $PID); Write-Output ('KIND=' + $k.Id); Start-Sleep -Seconds 120\"";

    /// <summary>Root powershell starts the private child INHERITING stdout/stderr and exits cleanly.</summary>
    private string VererbenderBaum()
        => "-NoProfile -Command \"$p = [Diagnostics.ProcessStartInfo]::new('" + _kind + "', '-n 60 127.0.0.1'); "
           + "$p.UseShellExecute = $false; $k = [Diagnostics.Process]::Start($p); "
           + "Write-Output ('ROOT=' + $PID); Write-Output ('KIND=' + $k.Id); exit 0\"";

    // 1
    [Fact]
    public async Task Zeitlimit_beendet_Root_und_genau_dieses_Kind()
    {
        var uhr = Stopwatch.StartNew();
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", BlockierenderBaum(), TimeSpan.FromSeconds(4));
        uhr.Stop();

        Assert.True(lauf.Abgelaufen);
        Assert.False(lauf.Abgebrochen);
        Assert.Null(lauf.BeendenProblem);
        Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(20), "Rückkehr nach " + uhr.Elapsed);
        Assert.False(ProzessbaumTests.Lebt(ProzessbaumTests.Wert(lauf.Ausgabe, "ROOT")!.Value));
        Assert.False(ProzessbaumTests.Lebt(ProzessbaumTests.Wert(lauf.Ausgabe, "KIND")!.Value));
        Assert.Empty(EigeneKinder());
    }

    // 2
    [Fact]
    public async Task Aufruferabbruch_beendet_Root_und_Kind_und_ist_kein_Zeitlimit()
    {
        using var abbruch = new CancellationTokenSource(TimeSpan.FromSeconds(4));
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", BlockierenderBaum(), TimeSpan.FromMinutes(2), abbruch: abbruch.Token);

        Assert.True(lauf.Abgebrochen);
        Assert.False(lauf.Abgelaufen);
        Assert.Null(lauf.BeendenProblem);
        Assert.False(ProzessbaumTests.Lebt(ProzessbaumTests.Wert(lauf.Ausgabe, "ROOT")!.Value));
        Assert.False(ProzessbaumTests.Lebt(ProzessbaumTests.Wert(lauf.Ausgabe, "KIND")!.Value));
        Assert.Empty(EigeneKinder());
    }

    [Fact]
    public async Task Gleichzeitiges_Zeitlimit_und_Abbruch_ergibt_genau_ein_Flag()
    {
        for (var i = 0; i < 3; i++)
        {
            using var abbruch = new CancellationTokenSource(TimeSpan.FromSeconds(2));
            var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe",
                "-NoProfile -Command \"Start-Sleep -Seconds 60\"", TimeSpan.FromSeconds(2), abbruch: abbruch.Token);
            Assert.True(lauf.Abgelaufen ^ lauf.Abgebrochen, "Abgelaufen=" + lauf.Abgelaufen + " Abgebrochen=" + lauf.Abgebrochen);
        }
    }

    // 3
    [Fact]
    public async Task Fertiger_Root_mit_erbendem_Kind_kehrt_begrenzt_zurueck_und_laesst_es_leben()
    {
        var uhr = Stopwatch.StartNew();
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", VererbenderBaum(), TimeSpan.FromMinutes(1));
        uhr.Stop();

        var kind = ProzessbaumTests.Wert(lauf.Ausgabe, "KIND");
        Assert.NotNull(kind);
        Assert.Equal(0, lauf.ExitCode);
        Assert.True(lauf.PipeGehalten);
        Assert.False(lauf.Abgelaufen);
        Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(20), "Rückkehr nach " + uhr.Elapsed);
        Assert.True(ProzessbaumTests.Lebt(kind!.Value), "das gültige Kind wurde beendet");

        ProzessbaumTests.Aufraeumen(kind);   // the test ends it, by its PID only
        Assert.False(ProzessbaumTests.Lebt(kind.Value));
    }

    // 4
    [Fact]
    public async Task Kurzer_Prozess_liefert_stdout_stderr_und_Exitcode()
    {
        var lauf = await Kommandozeile.AusfuehrenAsync("cmd.exe", "/c \"echo AUSGABE& echo FEHLER 1>&2& exit /b 3\"", TimeSpan.FromSeconds(30));

        Assert.Equal(3, lauf.ExitCode);
        Assert.Contains("AUSGABE", lauf.Ausgabe);
        Assert.Contains("FEHLER", lauf.Ausgabe);
        Assert.False(lauf.Abgelaufen);
        Assert.False(lauf.Abgebrochen);
        Assert.False(lauf.PipeGehalten);
        Assert.Null(lauf.BeendenProblem);
    }

    // 7
    [Fact]
    public async Task Simulierter_Beendenfehler_wird_gemeldet_und_fail_closed_bewertet()
    {
        // Only the root is killed, and the follow-up kill of the child "fails": the child survives.
        var lauf = await Kommandozeile.AusfuehrenInternAsync("powershell.exe", BlockierenderBaum(), TimeSpan.FromSeconds(4),
            baumBeenden: p => p.Kill(), einzelnBeenden: _ => false);
        var kind = ProzessbaumTests.Wert(lauf.Ausgabe, "KIND");
        try
        {
            Assert.True(lauf.Abgelaufen);
            Assert.NotNull(lauf.BeendenProblem);
            Assert.Contains("liefen nach dem Beenden weiter", lauf.BeendenProblem);
            Assert.Contains("PID " + kind, lauf.BeendenProblem);

            var unsauber = Kommandozeile.UnsauberesEnde(lauf);
            Assert.Equal(UpdateZustand.Fehler, unsauber!.Zustand);
            Assert.Contains("nicht vollständig beenden", unsauber.Meldung);

            // Through the whole update chain the card ends as an error, never as success.
            var provider = new SkriptProvider(new[] { "A" }, new[] { new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, "1", "2") },
                new[] { unsauber });
            var urteil = await new UpdateKette(provider, new ProgrammEintrag { Id = "u", Name = "U" }, new Progress<string>(_ => { }))
                .AusfuehrenAsync("A", new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, "1", "2"), default);
            Assert.Equal(LaufErgebnis.Fehlgeschlagen, urteil.Ergebnis);
            Assert.Equal(UpdateZustand.Fehler, urteil.FuerKarte.Zustand);
        }
        finally
        {
            ProzessbaumTests.Aufraeumen(kind);
        }
    }

    // 8
    [Fact]
    public async Task Wiederholte_Laeufe_hinterlassen_keine_Prozesse_und_keine_Leser()
    {
        var vorher = Kommandozeile.AktiveLeser;
        var kinder = new List<int?>();
        for (var i = 0; i < 3; i++)
        {
            var gehalten = await Kommandozeile.AusfuehrenAsync("powershell.exe", VererbenderBaum(), TimeSpan.FromMinutes(1));
            kinder.Add(ProzessbaumTests.Wert(gehalten.Ausgabe, "KIND"));
            await Kommandozeile.AusfuehrenAsync("powershell.exe", BlockierenderBaum(), TimeSpan.FromSeconds(3));
        }

        ProzessbaumTests.Aufraeumen(kinder.ToArray());

        var frist = DateTime.UtcNow.AddSeconds(15);
        while (Kommandozeile.AktiveLeser > vorher && DateTime.UtcNow < frist) await Task.Delay(200);

        Assert.True(Kommandozeile.AktiveLeser <= vorher, "Leser vorher " + vorher + ", nachher " + Kommandozeile.AktiveLeser);
        Assert.Empty(EigeneKinder());
    }
}

/// <summary>Pure decisions: no processes involved.</summary>
public sealed class ProzessendeAuswertungTests
{
    private static BefehlErgebnis Lauf(string ausgabe, bool abgelaufen = false, bool abgebrochen = false, string? problem = null)
        => new(-1, ausgabe, abgelaufen, Abgebrochen: abgebrochen, BeendenProblem: problem);

    // 5
    [Fact]
    public void Started_vor_Zeitlimit_ist_nie_Erfolg()
    {
        var e = RepoSkriptAktualisierer.Auswerten(Lauf("LAUNCHER_UPDATE_STATUS=started VERSION=1 PID=2", abgelaufen: true),
            "LAUNCHER_UPDATE_STATUS=", buildUnveraendert: false);
        Assert.Equal(UpdateZustand.Fehler, e.Zustand);

        var abgebrochen = RepoSkriptAktualisierer.Auswerten(Lauf("LAUNCHER_UPDATE_STATUS=started", abgebrochen: true),
            "LAUNCHER_UPDATE_STATUS=", false);
        Assert.Equal(UpdateZustand.Abgebrochen, abgebrochen.Zustand);
        Assert.NotEqual(UpdateZustand.Fertig, abgebrochen.Zustand);
    }

    // 6
    [Theory]
    [InlineData("LAUNCHER_UPDATE_STATUS=cancelled")]
    [InlineData("LAUNCHER_UPDATE_STATUS=no-answer (kein Klick)")]
    public void Status_mit_Beendenproblem_verliert_die_Warnung_nicht(string ausgabe)
    {
        var e = RepoSkriptAktualisierer.Auswerten(Lauf(ausgabe, abgebrochen: true, problem: "Kindprozess(e) liefen nach dem Beenden weiter: x (PID 1)."),
            "LAUNCHER_UPDATE_STATUS=", false);
        Assert.Equal(UpdateZustand.Fehler, e.Zustand);
        Assert.Contains("nicht vollständig beenden", e.Meldung);
        Assert.Contains("PID 1", e.Meldung);
    }

    [Fact]
    public void Nichts_zu_tun_gilt_nicht_bei_abgebrochenem_oder_unsauberem_Lauf()
    {
        Assert.False(WingetAktualisierer.IstNichtsZuTun(Lauf("No applicable upgrade found.", abgebrochen: true)));
        Assert.False(WingetAktualisierer.IstNichtsZuTun(Lauf("No applicable upgrade found.", problem: "x")));
        Assert.True(WingetAktualisierer.IstNichtsZuTun(new BefehlErgebnis(unchecked((int)0x8A15002B), "", false)));
    }

    [Fact]
    public void Sauberes_Ende_ergibt_keinen_Eingriff()
        => Assert.Null(Kommandozeile.UnsauberesEnde(new BefehlErgebnis(0, "ok", false)));

    [Fact]
    public void Nachfahren_folgen_nur_echten_Kanten_und_sind_zyklussicher()
    {
        var alle = new[]
        {
            new Prozessbaum.Knoten(10, 1), new Prozessbaum.Knoten(11, 10), new Prozessbaum.Knoten(12, 11),
            new Prozessbaum.Knoten(20, 1), new Prozessbaum.Knoten(30, 30),   // unrelated, self-parent
            new Prozessbaum.Knoten(40, 41), new Prozessbaum.Knoten(41, 40)    // cycle, unrelated
        };
        Assert.Equal(new[] { 11, 12 }, Prozessbaum.Nachfahren(alle, 10));
        Assert.Equal(new[] { 41 }, Prozessbaum.Nachfahren(alle, 40));
        Assert.Empty(Prozessbaum.Nachfahren(alle, 12));
    }
}
