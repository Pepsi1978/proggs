using System.Diagnostics;
using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>
/// Real process trees, harmless members only (powershell, cmd, ping). Every test records the
/// PIDs it created and kills them in finally, even when an assertion fails.
/// </summary>
public sealed class ProzessbaumTests
{
    internal static int? Wert(string ausgabe, string schluessel)
    {
        var zeile = ausgabe.Split('\n').FirstOrDefault(z => z.TrimStart().StartsWith(schluessel + "="));
        return int.TryParse(zeile?.Split('=')[1].Trim(), out var id) ? id : null;
    }

    internal static bool Lebt(int pid)
    {
        try
        {
            using var p = Process.GetProcessById(pid);
            return !p.HasExited;
        }
        catch (ArgumentException) { return false; }
        catch (InvalidOperationException) { return false; }
    }

    internal static void Aufraeumen(params int?[] pids)
    {
        foreach (var pid in pids)
        {
            if (pid is null) continue;
            try { using var p = Process.GetProcessById(pid.Value); p.Kill(entireProcessTree: true); p.WaitForExit(5000); }
            catch { /* already gone */ }
        }
    }

    /// <summary>
    /// Parent powershell -> child cmd (started without pipe inheritance) -> grandchild ping.
    /// A short time limit must take the whole tree down before AusfuehrenAsync returns.
    /// </summary>
    [Fact]
    public async Task Zeitlimit_beendet_den_ganzen_Baum()
    {
        var skript = "$c = Start-Process cmd.exe -ArgumentList '/c ping -n 120 127.0.0.1 >nul' -PassThru -WindowStyle Hidden; "
                     + "Start-Sleep -Milliseconds 800; "
                     + "$g = (Get-CimInstance Win32_Process -Filter ('ParentProcessId=' + $c.Id) | Select-Object -First 1).ProcessId; "
                     + "Write-Output ('KIND=' + $c.Id); Write-Output ('ENKEL=' + $g); Start-Sleep -Seconds 120";
        BefehlErgebnis? lauf = null;
        try
        {
            lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", "-NoProfile -Command \"" + skript + "\"",
                TimeSpan.FromSeconds(6));

            Assert.True(lauf.Abgelaufen);
            var kind = Wert(lauf.Ausgabe, "KIND");
            var enkel = Wert(lauf.Ausgabe, "ENKEL");
            Assert.NotNull(kind);                 // output read before the kill is preserved
            Assert.NotNull(enkel);
            Assert.False(Lebt(kind!.Value), "Kind lebt noch");
            Assert.False(Lebt(enkel!.Value), "Enkel lebt noch");
        }
        finally
        {
            if (lauf is not null) Aufraeumen(Wert(lauf.Ausgabe, "KIND"), Wert(lauf.Ausgabe, "ENKEL"));
        }
    }

    /// <summary>A cancel requested by the caller is not an exceeded time limit.</summary>
    [Fact]
    public async Task Aufruferabbruch_ist_kein_Zeitlimit()
    {
        using var abbruch = new CancellationTokenSource(TimeSpan.FromSeconds(3));
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe",
            "-NoProfile -Command \"Write-Output ('PID=' + $PID); Write-Output vorher; Start-Sleep -Seconds 60\"",
            TimeSpan.FromSeconds(60), abbruch: abbruch.Token);
        try
        {
            Assert.False(lauf.Abgelaufen, "Abbruch wurde als Zeitlimit gemeldet");
            Assert.True(lauf.Abgebrochen);
            Assert.Null(lauf.BeendenProblem);
            Assert.Contains("vorher", lauf.Ausgabe);               // output read before the cancel survives
            Assert.False(Lebt(Wert(lauf.Ausgabe, "PID")!.Value), "abgebrochener Prozess lebt noch");
        }
        finally
        {
            Aufraeumen(Wert(lauf.Ausgabe, "PID"));
        }
    }

    [Fact]
    public async Task Zeitlimit_ist_kein_Abbruch_und_meldet_sauberes_Beenden()
    {
        var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe",
            "-NoProfile -Command \"Write-Output ('PID=' + $PID); Start-Sleep -Seconds 60\"", TimeSpan.FromSeconds(3));
        try
        {
            Assert.True(lauf.Abgelaufen);
            Assert.False(lauf.Abgebrochen);
            Assert.Null(lauf.BeendenProblem);
            Assert.Contains("Zeitlimit überschritten", lauf.Ausgabe);
        }
        finally
        {
            Aufraeumen(Wert(lauf.Ausgabe, "PID"));
        }
    }

    /// <summary>
    /// The update-script case: the tool exits, a child it started keeps the inherited pipe.
    /// The run returns quickly, says so -- and must leave that child alive.
    /// </summary>
    [Fact]
    public async Task Gehaltene_Pipe_kehrt_schnell_zurueck_und_laesst_das_Kind_leben()
    {
        var skript = "$p = [Diagnostics.ProcessStartInfo]::new('ping.exe', '-n 60 127.0.0.1'); "
                     + "$p.UseShellExecute = $false; $k = [Diagnostics.Process]::Start($p); "
                     + "Write-Output ('KIND=' + $k.Id); exit 0";
        BefehlErgebnis? lauf = null;
        try
        {
            var uhr = Stopwatch.StartNew();
            lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe", "-NoProfile -Command \"" + skript + "\"",
                TimeSpan.FromSeconds(50));
            uhr.Stop();

            Assert.False(lauf.Abgelaufen);
            Assert.True(lauf.PipeGehalten);
            Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(25), "Lauf dauerte " + uhr.Elapsed);
            var kind = Wert(lauf.Ausgabe, "KIND");
            Assert.NotNull(kind);
            Assert.True(Lebt(kind!.Value), "das absichtlich langlebige Zielkind wurde beendet");
        }
        finally
        {
            if (lauf is not null) Aufraeumen(Wert(lauf.Ausgabe, "KIND"));
        }
    }
}

/// <summary>
/// Prozessdienst against real, harmless processes: copies of ping.exe in private temp folders,
/// so no test can ever match a process it did not start itself.
/// </summary>
public sealed class ProzessdienstTests : IDisposable
{
    private readonly string _wurzel = Path.Combine(Path.GetTempPath(), "uz-proz-" + Guid.NewGuid().ToString("N"));
    private readonly List<int> _pids = new();

    public void Dispose()
    {
        ProzessbaumTests.Aufraeumen(_pids.Select(p => (int?)p).ToArray());
        try { Directory.Delete(_wurzel, true); } catch { }
    }

    private string PingKopie(string ordner)
    {
        var ziel = Path.Combine(_wurzel, ordner);
        Directory.CreateDirectory(ziel);
        var exe = Path.Combine(ziel, "uzping.exe");
        File.Copy(Path.Combine(Environment.SystemDirectory, "PING.EXE"), exe);
        return exe;
    }

    private int Starten(string exe)
    {
        using var p = Process.Start(new ProcessStartInfo(exe, "-n 120 127.0.0.1") { UseShellExecute = false, CreateNoWindow = true })!;
        _pids.Add(p.Id);
        return p.Id;
    }

    private static ProgrammEintrag Eintrag(string exePfad)
        => new() { Id = "t", Name = "t", ExePfad = exePfad, Prozesse = new() { "uzping" } };

    [Theory]
    [InlineData(@"C:\Tools\Application\a.exe", @"C:\Tools\App", false)]
    [InlineData(@"C:\Tools\App2\a.exe", @"C:\Tools\App", false)]
    [InlineData(@"C:\Tools\App\a.exe", @"C:\Tools\App", true)]
    [InlineData(@"C:\Tools\App\sub\b.exe", @"C:\Tools\App", true)]
    [InlineData(@"C:\Tools\App\a.exe", @"C:\Tools\App\", true)]
    [InlineData(@"c:\tools\app\A.EXE", @"C:\Tools\App", true)]
    [InlineData(@"C:\Tools\App", @"C:\Tools\App", true)]
    [InlineData("", @"C:\Tools\App", false)]                 // unreadable path: never a match
    public void Pfadgrenze_ist_eine_echte_Verzeichnisgrenze(string pfad, string ordner, bool erwartet)
        => Assert.Equal(erwartet, Prozessdienst.LiegtIn(pfad, ordner));

    [Fact]
    public void Geschwisterordner_wird_nicht_als_eigener_Prozess_erkannt()
    {
        var fremd = PingKopie("Application");
        var pid = Starten(fremd);
        var erwartet = Path.Combine(_wurzel, "App", "uzping.exe");   // "App" is a prefix of "Application"

        Assert.DoesNotContain(Prozessdienst.Laufende(Eintrag(erwartet)), i => i.Pid == pid);
        Assert.Contains(Prozessdienst.Laufende(Eintrag(fremd)), i => i.Pid == pid);
    }

    [Fact]
    public async Task Beenden_prueft_danach_was_wirklich_noch_laeuft()
    {
        var exe = PingKopie("Ziel");
        var pid = Starten(exe);

        var ergebnis = await Prozessdienst.BeendenAsync(Eintrag(exe));

        Assert.Equal(1, ergebnis.Gefunden);
        Assert.True(ergebnis.Erfolgreich, ergebnis.Problem);
        Assert.Empty(ergebnis.NochLaufend);
        Assert.False(ProzessbaumTests.Lebt(pid));
    }

    [Fact]
    public async Task Beenden_laesst_fremde_Prozesse_gleichen_Namens_stehen()
    {
        var ziel = PingKopie("Ziel");
        var fremd = PingKopie("Fremd");
        var fremdPid = Starten(fremd);
        Starten(ziel);

        var ergebnis = await Prozessdienst.BeendenAsync(Eintrag(ziel));

        Assert.True(ergebnis.Erfolgreich, ergebnis.Problem);
        Assert.True(ProzessbaumTests.Lebt(fremdPid), "fremder Prozess gleichen Namens wurde beendet");
    }

    /// <summary>Snapshot of one of our own test processes, read the same way Laufende does.</summary>
    private static ProzessInfo Snapshot(string exe, int pid)
        => Prozessdienst.Laufende(Eintrag(exe)).Single(i => i.Pid == pid);

    [Fact]
    public void Verschwundener_Prozess_ist_kein_Fehler()
    {
        var probleme = new List<string>();
        Prozessdienst.MitProzess(new ProzessInfo(int.MaxValue - 7, "weg", "", DateTime.Now), p => p.Kill(), probleme, "Beenden");
        Assert.Empty(probleme);
    }

    [Fact]
    public void Wiederverwendete_PID_wird_nicht_angefasst_und_gemeldet()
    {
        var pid = Starten(PingKopie("Reuse"));
        var probleme = new List<string>();
        var ausgefuehrt = false;

        // Same PID, but a different start time: a reused PID -- must not be touched.
        Prozessdienst.MitProzess(new ProzessInfo(pid, "uzping", "", new DateTime(2001, 1, 1)),
            _ => ausgefuehrt = true, probleme, "Beenden");

        Assert.False(ausgefuehrt);
        Assert.True(ProzessbaumTests.Lebt(pid));
        Assert.Single(probleme);
        Assert.Contains("weicht ab", probleme[0]);
    }

    [Fact]
    public void Unbekannte_Startzeit_fuehrt_die_Aktion_nicht_aus()
    {
        var pid = Starten(PingKopie("OhneStart"));
        var probleme = new List<string>();
        var ausgefuehrt = false;

        Prozessdienst.MitProzess(new ProzessInfo(pid, "uzping", "", null), _ => ausgefuehrt = true, probleme, "Beenden");

        Assert.False(ausgefuehrt);
        Assert.True(ProzessbaumTests.Lebt(pid));
        Assert.Single(probleme);
        Assert.Contains("Startzeit nicht lesbar", probleme[0]);
    }

    [Fact]
    public void Fehlschlag_beim_Beenden_wird_sichtbar_nicht_verschluckt()
    {
        var exe = PingKopie("Fehlschlag");
        var pid = Starten(exe);
        var info = Snapshot(exe, pid);                          // identified: path and start time known
        Assert.NotNull(info.Startzeit);

        var probleme = new List<string>();
        Prozessdienst.MitProzess(info, _ => throw new System.ComponentModel.Win32Exception(5, "Zugriff verweigert"),
            probleme, "Beenden");

        Assert.Single(probleme);
        Assert.Contains("Zugriff verweigert", probleme[0]);
    }

    /// <summary>
    /// A catalog entry with only a process name: shown as running, but never closed or killed.
    /// </summary>
    [Fact]
    public async Task Eintrag_nur_mit_Namen_beendet_nichts_und_scheitert_verstaendlich()
    {
        var pid = Starten(PingKopie("NurName"));
        var nurName = new ProgrammEintrag { Id = "n", Name = "NurName", Prozesse = new() { "uzping" } };

        Assert.False(Prozessdienst.HatSichereIdentitaet(nurName));
        Assert.Contains(Prozessdienst.Laufende(nurName), i => i.Pid == pid);   // display still works

        var ergebnis = await Prozessdienst.BeendenAsync(nurName);

        Assert.False(ergebnis.Erfolgreich);
        Assert.Contains(ergebnis.NochLaufend, i => i.Pid == pid);
        Assert.Contains("Keine sichere Zuordnung", ergebnis.Problem);
        Assert.True(ProzessbaumTests.Lebt(pid), "Prozess wurde trotz fehlender Identität beendet");
    }
}
