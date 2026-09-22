using System.Diagnostics;
using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>
/// Single-instance lock with a private mutex name per test. A Mutex belongs to the thread that
/// acquired it, so every holder runs on its own dedicated Thread -- never Task.Run.
/// </summary>
public sealed class EinzelinstanzTests
{
    private static string Name() => @"Local\uz-test-" + Guid.NewGuid().ToString("N");

    /// <summary>Runs a holder on its own thread: acquire, signal, wait for the release order, release.</summary>
    private sealed class Halter : IDisposable
    {
        private readonly ManualResetEventSlim _gehalten = new();
        private readonly ManualResetEventSlim _loslassen = new();
        private readonly Thread _faden;
        public bool? Ergebnis;

        public Halter(string name, bool freigeben = true)
        {
            _faden = new Thread(() =>
            {
                var instanz = new Einzelinstanz(name);
                Ergebnis = instanz.Beanspruchen(TimeSpan.Zero);
                _gehalten.Set();
                _loslassen.Wait();
                if (freigeben) instanz.Freigeben();   // otherwise the thread ends holding it: abandoned
            }) { IsBackground = true };
            _faden.Start();
            _gehalten.Wait();
        }

        public void Loslassen() { _loslassen.Set(); _faden.Join(); }
        public void Dispose() { if (_faden.IsAlive) Loslassen(); }
    }

    private static bool? AufFaden(Func<bool?> aktion)
    {
        bool? ergebnis = null;
        var t = new Thread(() => ergebnis = aktion()) { IsBackground = true };
        t.Start();
        t.Join();
        return ergebnis;
    }

    /// <summary>
    /// Reproduces the defect: the old "does it exist?" check refuses while the holder is on its
    /// way out; waiting on the lock takes it over as soon as it is released.
    /// </summary>
    [Fact]
    public void Alte_Pruefung_scheitert_neue_wartet_und_uebernimmt()
    {
        var name = Name();
        using var halter = new Halter(name);
        Assert.True(halter.Ergebnis);

        var alteSemantik = AufFaden(() =>
        {
            using var m = new Mutex(initiallyOwned: true, name, out var neu);
            return neu;
        });
        Assert.False(alteSemantik);   // old: second instance quits even if the holder is about to leave

        var loslassen = new Thread(() => { Thread.Sleep(500); halter.Loslassen(); });
        loslassen.Start();
        var neueSemantik = AufFaden(() =>
        {
            var nachfolger = new Einzelinstanz(name);
            var ok = nachfolger.Beanspruchen(TimeSpan.FromSeconds(5));
            nachfolger.Freigeben();
            return ok;
        });
        loslassen.Join();
        Assert.True(neueSemantik);
    }

    [Fact]
    public void Freigegeben_aber_nicht_geschlossen_ist_frei()
    {
        var name = Name();
        Mutex? offen = null;
        var t = new Thread(() =>
        {
            offen = new Mutex(initiallyOwned: true, name, out _);
            offen.ReleaseMutex();              // released, handle still open -- object exists
        });
        t.Start(); t.Join();
        try
        {
            Assert.False(AufFaden(() => { using var m = new Mutex(true, name, out var neu); return neu; }));   // old: "taken"
            Assert.True(AufFaden(() =>
            {
                var i = new Einzelinstanz(name);
                var ok = i.Beanspruchen(TimeSpan.Zero);
                i.Freigeben();
                return ok;
            }));
        }
        finally
        {
            offen?.Dispose();
        }
    }

    [Fact]
    public void Verwaister_Mutex_wird_uebernommen()
    {
        var name = Name();
        var halter = new Halter(name, freigeben: false);
        halter.Loslassen();                     // thread ends holding the mutex -> abandoned

        Assert.True(AufFaden(() =>
        {
            var i = new Einzelinstanz(name);
            var ok = i.Beanspruchen(TimeSpan.FromSeconds(1));
            i.Freigeben();
            return ok;
        }));
    }

    [Fact]
    public void Waehrend_des_Dialogs_exklusiv_bei_Timeout_keine_zweite_Instanz()
    {
        var name = Name();
        using var alt = new Halter(name);        // the old instance, "in the UAC prompt"

        Assert.False(AufFaden(() =>
        {
            var i = new Einzelinstanz(name);
            var ok = i.Beanspruchen(TimeSpan.FromMilliseconds(400));
            Assert.False(i.Besitzt);
            return ok;
        }));
    }

    [Fact]
    public void Nach_Freigabe_uebernimmt_genau_einer()
    {
        var name = Name();
        var alt = new Halter(name);
        var gewinner = 0;
        using var bereit = new CountdownEvent(2);
        using var ende = new ManualResetEventSlim();
        var wartende = Enumerable.Range(0, 2).Select(_ => new Thread(() =>
        {
            var i = new Einzelinstanz(name);
            bereit.Signal();
            if (i.Beanspruchen(TimeSpan.FromSeconds(3)) == true)
            {
                Interlocked.Increment(ref gewinner);
                ende.Wait(TimeSpan.FromSeconds(6));   // hold until the other one timed out
                i.Freigeben();
            }
        }) { IsBackground = true }).ToList();
        wartende.ForEach(t => t.Start());
        bereit.Wait();
        Thread.Sleep(200);
        alt.Loslassen();

        Thread.Sleep(4000);                     // the loser's 3 s wait is over by now
        Assert.Equal(1, gewinner);
        ende.Set();
        wartende.ForEach(t => t.Join());
    }

    [Fact]
    public void Wiederholtes_Beanspruchen_und_Freigeben_ist_harmlos()
    {
        var name = Name();
        Assert.True(AufFaden(() =>
        {
            var i = new Einzelinstanz(name);
            var a = i.Beanspruchen(TimeSpan.Zero);
            var b = i.Beanspruchen(TimeSpan.Zero);   // idempotent
            i.Freigeben();
            i.Freigeben();                           // second release: no throw
            return a == true && b == true && !i.Besitzt;
        }));
    }
}

public sealed class UebernahmeTests : IDisposable
{
    private readonly string _wurzel = Path.Combine(Path.GetTempPath(), "uz-inst-" + Guid.NewGuid().ToString("N"));
    private readonly List<int> _pids = new();

    public void Dispose()
    {
        ProzessbaumTests.Aufraeumen(_pids.Select(p => (int?)p).ToArray());
        try { Directory.Delete(_wurzel, true); } catch { }
    }

    private string Kopie(string ordner)
    {
        var ziel = Path.Combine(_wurzel, ordner);
        Directory.CreateDirectory(ziel);
        var exe = Path.Combine(ziel, "uzinst.exe");
        File.Copy(Path.Combine(Environment.SystemDirectory, "PING.EXE"), exe);
        return exe;
    }

    private int Starten(string exe)
    {
        using var p = Process.Start(new ProcessStartInfo(exe, "-n 120 127.0.0.1") { UseShellExecute = false, CreateNoWindow = true })!;
        _pids.Add(p.Id);
        return p.Id;
    }

    [Theory]
    [InlineData(new[] { "--uebernahme", "4242" }, 4242)]
    [InlineData(new[] { "--uebernahme", "0" }, null)]
    [InlineData(new[] { "--uebernahme", "-5" }, null)]
    [InlineData(new[] { "--uebernahme", "abc" }, null)]
    [InlineData(new[] { "--uebernahme", "1 2" }, null)]
    [InlineData(new[] { "--uebernahme" }, null)]
    [InlineData(new[] { "--UEBERNAHME", "4242" }, null)]
    [InlineData(new[] { "--uebernahme", "4242", "x" }, null)]
    [InlineData(new string[0], null)]
    public void Argument_wird_streng_geprueft(string[] argumente, int? erwartet)
        => Assert.Equal(erwartet, Uebernahme.PidAus(argumente, eigenePid: 1));

    [Fact]
    public void Eigene_PID_ist_keine_Uebernahme()
        => Assert.Null(Uebernahme.PidAus(new[] { "--uebernahme", "77" }, eigenePid: 77));

    [Fact]
    public void Argument_und_Parser_passen_zusammen()
        => Assert.Equal(1234, Uebernahme.PidAus(Uebernahme.Argument(1234).Split(' '), eigenePid: 1));

    [Fact]
    public void Nachfolger_wartet_auf_den_echten_Vorgaenger()
    {
        var exe = Kopie("A");
        var pid = Starten(exe);
        var killer = new Thread(() => { Thread.Sleep(1500); ProzessbaumTests.Aufraeumen(pid); });
        killer.Start();

        var uhr = Stopwatch.StartNew();
        Assert.True(Uebernahme.AufEndeWarten(pid, exe, TimeSpan.FromSeconds(10)));
        killer.Join();
        Assert.InRange(uhr.Elapsed.TotalMilliseconds, 1000, 8000);
    }

    [Fact]
    public void Fremde_PID_kostet_keine_Wartezeit()
    {
        var pid = Starten(Kopie("Fremd"));
        var uhr = Stopwatch.StartNew();
        Assert.True(Uebernahme.AufEndeWarten(pid, Path.Combine(_wurzel, "Anders", "uzinst.exe"), TimeSpan.FromSeconds(10)));
        Assert.True(uhr.Elapsed < TimeSpan.FromMilliseconds(800), "wartete " + uhr.Elapsed);
        Assert.True(ProzessbaumTests.Lebt(pid));
    }

    [Fact]
    public void Tote_PID_sofort_und_Grenze_wird_eingehalten()
    {
        Assert.True(Uebernahme.AufEndeWarten(int.MaxValue - 11, @"C:\x\y.exe", TimeSpan.FromSeconds(10)));

        var exe = Kopie("Bleibt");
        var pid = Starten(exe);
        var uhr = Stopwatch.StartNew();
        Assert.False(Uebernahme.AufEndeWarten(pid, exe, TimeSpan.FromMilliseconds(700)));   // still running at the limit
        Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(3));
    }

    [Fact]
    public void Finder_trifft_nur_dieselbe_Exe()
    {
        var a = Kopie("A");
        var b = Kopie("B");
        var pidA = Starten(a);
        Starten(b);

        Assert.Equal(pidA, Einzelstart.AndereInstanz("uzinst", a, eigenePid: 0));
        Assert.Null(Einzelstart.AndereInstanz("uzinst", Path.Combine(_wurzel, "C", "uzinst.exe"), eigenePid: 0));
        Assert.Null(Einzelstart.AndereInstanz("uzinst", a, eigenePid: pidA));   // itself is no "other"
    }

    [Theory]
    [InlineData(@"C:\A\x.exe", @"C:\A\x.exe", true)]
    [InlineData(@"c:\a\X.EXE", @"C:\A\x.exe", true)]
    [InlineData(@"C:\A\..\A\x.exe", @"C:\A\x.exe", true)]
    [InlineData(@"C:\A\x.exe", @"C:\AB\x.exe", false)]
    [InlineData("", @"C:\A\x.exe", false)]      // unknown path never matches
    [InlineData(null, null, false)]
    public void Exe_Vergleich_ist_exakt(string? a, string? b, bool erwartet)
        => Assert.Equal(erwartet, Prozesspfad.GleicheDatei(a, b));

    [Fact]
    public void Pfad_eines_eigenen_Prozesses_ist_lesbar_toter_ist_leer()
    {
        var exe = Kopie("Pfad");
        var pid = Starten(exe);
        Assert.True(Prozesspfad.GleicheDatei(Prozesspfad.Lesen(pid), exe));
        Assert.Equal("", Prozesspfad.Lesen(int.MaxValue - 3));
    }

    /// <summary>Repeated path/process queries must not pile up handles (no windows are touched).</summary>
    [Fact]
    public void Wiederholte_Abfragen_haeufen_keine_Handles_an()
    {
        var a = Kopie("Mess");
        Starten(a);
        for (var i = 0; i < 20; i++) Einzelstart.AndereInstanz("uzinst", a, 0);

        static int Handles()
        {
            GC.Collect(); GC.WaitForPendingFinalizers(); GC.Collect();
            using var ich = Process.GetCurrentProcess();
            return ich.HandleCount;
        }

        var vorher = Handles();
        for (var i = 0; i < 300; i++) Einzelstart.AndereInstanz("uzinst", a, 0);
        var nachher = Handles();
        Assert.True(nachher - vorher < 40, $"Handles {vorher} -> {nachher}");
    }
}

public sealed class RechteTokenTests
{
    [Theory]
    [InlineData("~ RUNASADMIN", true)]
    [InlineData("~ HIGHDPIAWARE RUNASADMIN", true)]
    [InlineData("~ runasadmin", true)]
    [InlineData("~ RUNASADMINX", false)]
    [InlineData("~ XRUNASADMIN", false)]
    [InlineData("~ HIGHDPIAWARE", false)]
    [InlineData(null, false)]
    public void Nur_das_exakte_Token_zaehlt(string? wert, bool erwartet)
        => Assert.Equal(erwartet, Rechte.HatAdminToken(wert));

    [Theory]
    [InlineData(null, "~ RUNASADMIN")]
    [InlineData("~ RUNASADMINX", "~ RUNASADMINX RUNASADMIN")]
    [InlineData("~ HIGHDPIAWARE", "~ HIGHDPIAWARE RUNASADMIN")]
    [InlineData("~ RUNASADMIN", "~ RUNASADMIN")]
    public void Hinzufuegen_erhaelt_andere_Tokens(string? wert, string erwartet)
        => Assert.Equal(erwartet, Rechte.MitAdminToken(wert));

    [Theory]
    [InlineData("~ HIGHDPIAWARE RUNASADMIN", "~ HIGHDPIAWARE")]
    [InlineData("~ RUNASADMIN", null)]
    [InlineData("~ RUNASADMINX RUNASADMIN", "~ RUNASADMINX")]
    [InlineData("~ RUNASADMINX", "~ RUNASADMINX")]
    public void Entfernen_nimmt_nur_das_echte_Token(string wert, string? erwartet)
        => Assert.Equal(erwartet, Rechte.OhneAdminToken(wert));
}

public sealed class TerminalAbbruchTests
{
    [Fact]
    public async Task Abbruch_ist_Abbruch_Ausgabe_bleibt_Prozess_endet()
    {
        using var abbruch = new CancellationTokenSource(TimeSpan.FromSeconds(4));
        var text = await Terminal.AusfuehrenAsync("Write-Output vorher; Write-Output ('PID=' + $PID); Start-Sleep -Seconds 60", abbruch.Token);
        var pid = ProzessbaumTests.Wert(text, "PID");
        try
        {
            Assert.Contains("vorher", text);
            Assert.Contains("[Abgebrochen]", text);
            Assert.DoesNotContain("Beendet mit Code", text);
            Assert.DoesNotContain("Zeitlimit", text);
            Assert.NotNull(pid);
            Assert.False(ProzessbaumTests.Lebt(pid!.Value));
        }
        finally
        {
            ProzessbaumTests.Aufraeumen(pid);
        }
    }

    [Fact]
    public void Beendenproblem_bleibt_sichtbar()
    {
        var text = Terminal.Auswerten(new BefehlErgebnis(-1, "x", false, Abgebrochen: true, BeendenProblem: "Prozess 12 lief noch."));
        Assert.Contains("[Abgebrochen]", text);
        Assert.Contains("Prozess 12 lief noch.", text);
        Assert.DoesNotContain("Beendet mit Code", text);
    }
}

/// <summary>
/// The fail-closed fallback. A technically failing mutex is produced with an invalid name (a
/// namespace that does not exist); no real Global\ mutex is touched. All lock operations of one
/// owner run on the same dedicated thread.
/// </summary>
public sealed class FallbackTests
{
    private static string Lokal() => @"Local\uz-fb-" + Guid.NewGuid().ToString("N");
    private static string Kaputt() => @"GibtEsNicht\uz-fb-" + Guid.NewGuid().ToString("N");

    private static T AufFaden<T>(Func<T> aktion)
    {
        T ergebnis = default!;
        Exception? fehler = null;
        var t = new Thread(() => { try { ergebnis = aktion(); } catch (Exception ex) { fehler = ex; } }) { IsBackground = true };
        t.Start(); t.Join();
        if (fehler is not null) throw new Xunit.Sdk.XunitException("Faden: " + fehler);
        return ergebnis;
    }

    private static bool Frei(string name) => AufFaden(() =>
    {
        var probe = new Einzelinstanz(name);
        var ok = probe.Beanspruchen(TimeSpan.Zero) == true;
        probe.Freigeben();
        return ok;
    });

    [Fact]
    public void Ungueltiger_Name_ist_technischer_Fehlschlag()
        => Assert.Null(AufFaden(() => new Einzelinstanz(Kaputt()).Beanspruchen(TimeSpan.Zero)));

    [Fact]
    public void Global_kaputt_lokal_erworben_andere_Exe_laeuft_ergibt_false_und_gibt_lokal_frei()
    {
        var lokal = Lokal();
        var gehalten = AufFaden(() => Einzelstart.Entscheiden(new Einzelinstanz(Kaputt()), new Einzelinstanz(lokal),
            TimeSpan.Zero, andereInstanzLaeuft: () => true));

        Assert.Null(gehalten);
        Assert.True(Frei(lokal), "lokaler Mutex blieb belegt");
    }

    [Fact]
    public void Global_kaputt_lokal_erworben_keine_andere_ergibt_true_und_haelt_bis_Freigeben()
    {
        var lokal = Lokal();
        using var gehalten = new ManualResetEventSlim();
        using var loslassen = new ManualResetEventSlim();
        Einzelinstanz? besitz = null;
        var halter = new Thread(() =>
        {
            besitz = Einzelstart.Entscheiden(new Einzelinstanz(Kaputt()), new Einzelinstanz(lokal),
                TimeSpan.Zero, andereInstanzLaeuft: () => false);
            gehalten.Set();
            loslassen.Wait();
            besitz?.Freigeben();
        }) { IsBackground = true };
        halter.Start();
        gehalten.Wait();

        Assert.NotNull(besitz);
        Assert.True(besitz!.Besitzt);
        Assert.False(Frei(lokal), "lokale Sperre wird nicht gehalten");

        loslassen.Set();
        halter.Join();
        Assert.True(Frei(lokal));
    }

    [Fact]
    public void Global_und_lokal_kaputt_ergibt_niemals_true()
    {
        var aufgerufen = false;
        var gehalten = AufFaden(() => Einzelstart.Entscheiden(new Einzelinstanz(Kaputt()), new Einzelinstanz(Kaputt()),
            TimeSpan.Zero, andereInstanzLaeuft: () => { aufgerufen = true; return false; }));
        Assert.Null(gehalten);
        Assert.False(aufgerufen);   // the process finder is no substitute for a held lock
    }

    [Fact]
    public void Lokal_belegt_ergibt_false()
    {
        var lokal = Lokal();
        using var gehalten = new ManualResetEventSlim();
        using var loslassen = new ManualResetEventSlim();
        var anderer = new Thread(() =>
        {
            var i = new Einzelinstanz(lokal);
            i.Beanspruchen(TimeSpan.Zero);
            gehalten.Set();
            loslassen.Wait();
            i.Freigeben();
        }) { IsBackground = true };
        anderer.Start();
        gehalten.Wait();
        try
        {
            Assert.Null(AufFaden(() => Einzelstart.Entscheiden(new Einzelinstanz(Kaputt()), new Einzelinstanz(lokal),
                TimeSpan.FromMilliseconds(200), andereInstanzLaeuft: () => false)));
        }
        finally
        {
            loslassen.Set();
            anderer.Join();
        }
    }

    [Fact]
    public void Global_erworben_gibt_den_lokalen_Wrapper_frei_global_belegt_ergibt_false()
    {
        var global = Lokal();   // any valid name stands in for the global one
        var lokal = Lokal();
        var gewonnen = AufFaden(() =>
        {
            var b = Einzelstart.Entscheiden(new Einzelinstanz(global), new Einzelinstanz(lokal), TimeSpan.Zero, () => false);
            var ok = b is not null && b.Besitzt;
            b?.Freigeben();
            return ok;
        });
        Assert.True(gewonnen);
        Assert.True(Frei(lokal));

        using var gehalten = new ManualResetEventSlim();
        using var loslassen = new ManualResetEventSlim();
        var halter = new Thread(() =>
        {
            var i = new Einzelinstanz(global);
            i.Beanspruchen(TimeSpan.Zero);
            gehalten.Set();
            loslassen.Wait();
            i.Freigeben();
        }) { IsBackground = true };
        halter.Start();
        gehalten.Wait();
        try
        {
            Assert.Null(AufFaden(() => Einzelstart.Entscheiden(new Einzelinstanz(global), new Einzelinstanz(lokal),
                TimeSpan.Zero, () => false)));
            Assert.True(Frei(lokal));   // taken global: no local fallback
        }
        finally
        {
            loslassen.Set();
            halter.Join();
        }
    }
}
