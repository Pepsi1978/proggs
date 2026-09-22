using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using UpdateZentrale.ViewModels;
using Xunit;

namespace UpdateZentrale.Tests;

public sealed class LaufkoordinationTests
{
    [Fact]
    public void Einzel_gegen_Einzel_nur_einer()
    {
        var k = new Laufkoordination();
        using var erster = k.EinzelBeginnen();
        Assert.NotNull(erster);
        Assert.Null(k.EinzelBeginnen());
    }

    [Fact]
    public void Einzel_blockiert_Sammel()
    {
        var k = new Laufkoordination();
        using var einzel = k.EinzelBeginnen();
        Assert.Null(k.SammelBeginnen());
    }

    [Fact]
    public void Sammel_blockiert_Einzel_und_Sammel()
    {
        var k = new Laufkoordination();
        using var sammel = k.SammelBeginnen();
        Assert.NotNull(sammel);
        Assert.True(k.SammelLaeuft);
        Assert.Null(k.EinzelBeginnen());
        Assert.Null(k.SammelBeginnen());
    }

    [Fact]
    public void Freigabe_nach_Ausnahme_und_Abbruch()
    {
        var k = new Laufkoordination();

        Assert.Throws<InvalidOperationException>((Action)(() =>
        {
            using var b = k.EinzelBeginnen();
            throw new InvalidOperationException("Installer kaputt");
        }));
        Assert.False(k.Belegt);

        Assert.Throws<OperationCanceledException>((Action)(() =>
        {
            using var b = k.SammelBeginnen();
            throw new OperationCanceledException();
        }));
        Assert.False(k.Belegt);
        Assert.NotNull(k.EinzelBeginnen());
    }

    [Fact]
    public void Doppelte_Freigabe_gibt_keinen_fremden_Besitz_frei()
    {
        var k = new Laufkoordination();
        var alt = k.EinzelBeginnen()!;
        alt.Dispose();
        using var neu = k.SammelBeginnen();
        alt.Dispose();   // stray second release
        Assert.True(k.Belegt);
        Assert.Null(k.EinzelBeginnen());
    }

    /// <summary>
    /// Many threads released at the same instant, mixing single and batch requests: in every
    /// round exactly one may win. Plain bool checks lose this regularly.
    /// </summary>
    [Fact]
    public void Gleichzeitige_Starts_lassen_genau_einen_zu()
    {
        const int Runden = 300, Faeden = 16;
        for (var r = 0; r < Runden; r++)
        {
            var k = new Laufkoordination();
            var gewinner = 0;
            using var start = new Barrier(Faeden);
            var besitze = new Laufbesitz?[Faeden];
            var threads = Enumerable.Range(0, Faeden).Select(i => new Thread(() =>
            {
                start.SignalAndWait();
                besitze[i] = i % 2 == 0 ? k.EinzelBeginnen() : k.SammelBeginnen();
                if (besitze[i] is not null) Interlocked.Increment(ref gewinner);
            })).ToList();
            threads.ForEach(t => t.Start());
            threads.ForEach(t => t.Join());

            Assert.Equal(1, gewinner);
            foreach (var b in besitze) b?.Dispose();
            Assert.False(k.Belegt);
        }
    }

    [Fact]
    public void Aenderungsereignis_bei_Erwerb_und_Freigabe()
    {
        var k = new Laufkoordination();
        var zaehler = 0;
        k.Geaendert += (_, _) => zaehler++;
        using (k.EinzelBeginnen()) { }
        Assert.Equal(2, zaehler);
    }
}

/// <summary>
/// The cards against the coordinator, with a controllable fake: only checks are driven here,
/// because checks write nothing to the real update history.
/// </summary>
public sealed class KartenKoordinationTests
{
    private sealed class FakeAktualisierer : IAktualisierer
    {
        public readonly TaskCompletionSource Freigabe = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public int Aufrufe;
        public int Gleichzeitig;
        public int HoechstGleichzeitig;

        public string Art => "fake";

        public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
        {
            Interlocked.Increment(ref Aufrufe);
            var jetzt = Interlocked.Increment(ref Gleichzeitig);
            HoechstGleichzeitig = Math.Max(HoechstGleichzeitig, jetzt);
            try { await Freigabe.Task; }
            finally { Interlocked.Decrement(ref Gleichzeitig); }
            return new PruefErgebnis(UpdateZustand.Aktuell, "1.0", "", "ok");
        }

        public Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
            => throw new NotSupportedException();

        public Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch) => Task.FromResult("");
    }

    private static ProgrammViewModel Karte(string id, IAktualisierer fake, Laufkoordination k)
        => new(new ProgrammEintrag { Id = id, Name = id, Art = "fake" }, fake, new Einstellungen(), k);

    [Fact]
    public async Task Zwei_Karten_starten_nie_gleichzeitig()
    {
        var k = new Laufkoordination();
        var fake = new FakeAktualisierer();
        var a = Karte("a", fake, k);
        var b = Karte("b", fake, k);

        var laufA = a.PruefenCommand.ExecuteAsync(null);
        Assert.True(k.Belegt);
        Assert.False(b.KannPruefen);
        Assert.False(b.AktionMoeglich);

        await b.PruefenCommand.ExecuteAsync(null);          // must be refused at once
        Assert.Equal(1, fake.Aufrufe);
        Assert.Equal(ProgrammViewModel.BelegtText, b.StatusText);

        fake.Freigabe.SetResult();
        await laufA;
        Assert.False(k.Belegt);
        Assert.True(b.KannPruefen);
        Assert.Equal(1, fake.HoechstGleichzeitig);
    }

    [Fact]
    public async Task Doppelklick_derselben_Karte_nur_ein_Lauf()
    {
        var k = new Laufkoordination();
        var fake = new FakeAktualisierer();
        var a = Karte("a", fake, k);

        var erster = a.PruefenCommand.ExecuteAsync(null);
        var zweiter = a.PruefenCommand.ExecuteAsync(null);
        fake.Freigabe.SetResult();
        await Task.WhenAll(erster, zweiter);

        Assert.Equal(1, fake.Aufrufe);
        Assert.False(k.Belegt);
    }

    [Fact]
    public async Task Sammelbesitz_sperrt_Klicks_laesst_aber_die_eigenen_Kartenlaeufe_zu()
    {
        var k = new Laufkoordination();
        var fake = new FakeAktualisierer();
        var a = Karte("a", fake, k);
        fake.Freigabe.SetResult();

        using (var sammel = k.SammelBeginnen()!)
        {
            await a.PruefenCommand.ExecuteAsync(null);      // click during the batch: refused
            Assert.Equal(0, fake.Aufrufe);

            await a.PruefenImSammelAsync(sammel);            // the batch's own call: runs
            Assert.Equal(1, fake.Aufrufe);
            Assert.True(k.SammelLaeuft);                     // and does not release the batch
        }

        Assert.False(k.Belegt);
        await a.PruefenCommand.ExecuteAsync(null);
        Assert.Equal(2, fake.Aufrufe);
    }

    [Fact]
    public async Task Einzellauf_blockiert_Sammelbeginn()
    {
        var k = new Laufkoordination();
        var fake = new FakeAktualisierer();
        var a = Karte("a", fake, k);

        var lauf = a.PruefenCommand.ExecuteAsync(null);
        Assert.Null(k.SammelBeginnen());
        fake.Freigabe.SetResult();
        await lauf;
        using var sammel = k.SammelBeginnen();
        Assert.NotNull(sammel);
    }
}

public sealed class ProviderZustandTests
{
    private static BefehlErgebnis Lauf(string ausgabe, int code = 0, bool abgelaufen = false) => new(code, ausgabe, abgelaufen);

    private const string ClaudeZeile = "Name   Id               Version    Source\n------------------------------------------\nClaude Anthropic.Claude 2.2553.1.0 winget";
    private const string StudioZeile = "Name           Id                   Version Available  Source\n---\nAndroid Studio Google.AndroidStudio 2026.1  2026.1.4.7 winget";

    // ---- winget list ----
    [Fact] public void Winget_Zeitlimit_ist_Fehler_nicht_NichtInstalliert()
        => Assert.Equal(UpdateZustand.Fehler, WingetAktualisierer.ListeAuswerten(Lauf("", -1, true), "Anthropic.Claude").Zustand);

    [Fact] public void Winget_fremder_Exitcode_ist_Fehler()
        => Assert.Equal(UpdateZustand.Fehler, WingetAktualisierer.ListeAuswerten(Lauf("Failed when searching source", unchecked((int)0x8A15000F)), "Anthropic.Claude").Zustand);

    [Fact] public void Winget_kein_Paket_gefunden_ist_NichtInstalliert()
        => Assert.Equal(UpdateZustand.NichtInstalliert, WingetAktualisierer.ListeAuswerten(
            Lauf("No installed package found matching input criteria.", WingetAktualisierer.KeinPaketGefunden), "X.Y").Zustand);

    [Fact] public void Winget_sauberer_Lauf_ohne_Zeile_ist_Unbekannt()
        => Assert.Equal(UpdateZustand.Unbekannt, WingetAktualisierer.ListeAuswerten(Lauf("irgendwas anderes"), "Anthropic.Claude").Zustand);

    [Fact] public void Winget_sauberer_Lauf_aktuell_und_update()
    {
        var aktuell = WingetAktualisierer.ListeAuswerten(Lauf(ClaudeZeile), "Anthropic.Claude");
        Assert.Equal(UpdateZustand.Aktuell, aktuell.Zustand);
        Assert.Equal("2.2553.1.0", aktuell.InstallierteVersion);

        var update = WingetAktualisierer.ListeAuswerten(Lauf(StudioZeile), "Google.AndroidStudio");
        Assert.Equal(UpdateZustand.UpdateVerfuegbar, update.Zustand);
        Assert.Equal("2026.1.4.7", update.VerfuegbareVersion);
    }

    [Fact] public void Winget_Fingerabdruck_nur_aus_sauberem_Lauf()
    {
        Assert.Equal("", WingetAktualisierer.ListenFingerabdruck(Lauf(ClaudeZeile, -1, true), "Anthropic.Claude").Fingerabdruck);
        Assert.Equal("", WingetAktualisierer.ListenFingerabdruck(Lauf(ClaudeZeile, 5), "Anthropic.Claude").Fingerabdruck);
        Assert.NotNull(WingetAktualisierer.ListenFingerabdruck(Lauf(ClaudeZeile, 5), "Anthropic.Claude").Problem);
        Assert.Equal("2.2553.1.0", WingetAktualisierer.ListenFingerabdruck(Lauf(ClaudeZeile), "Anthropic.Claude").Fingerabdruck);
    }

    [Theory]
    [InlineData("", unchecked((int)0x8A15002B), false, true)]
    [InlineData("No applicable upgrade found.", 1, false, true)]
    [InlineData("Kein anwendbares Upgrade gefunden.", 1, false, true)]
    [InlineData("Installer failed", 1, false, false)]
    [InlineData("No applicable upgrade found.", -1, true, false)]
    public void Winget_nichts_zu_tun_wird_erkannt(string ausgabe, int code, bool abgelaufen, bool erwartet)
        => Assert.Equal(erwartet, WingetAktualisierer.IstNichtsZuTun(Lauf(ausgabe, code, abgelaufen)));

    // ---- Appx ----
    [Fact] public void Appx_Fehler_ist_nicht_nicht_installiert()
    {
        Assert.False(StoreAktualisierer.AppxAuswerten(Lauf("", -1, true)).Erfolg);
        Assert.False(StoreAktualisierer.AppxAuswerten(Lauf("", 1)).Erfolg);
        Assert.False(StoreAktualisierer.AppxAuswerten(Lauf("Get-AppxPackage : Zugriff verweigert")).Erfolg);
    }

    [Fact] public void Appx_leer_bei_Exit0_ist_nicht_installiert_Version_ist_Version()
    {
        var leer = StoreAktualisierer.AppxAuswerten(Lauf(""));
        Assert.True(leer.Erfolg);
        Assert.Equal("", leer.Version);
        Assert.Equal("26.915.4065.0", StoreAktualisierer.AppxAuswerten(Lauf("26.915.4065.0\n")).Version);
    }

    // ---- Store upgrade list ----
    private static readonly string[] Kennungen = { "9PLM9XGG6VKS", "OpenAI.Codex" };

    [Fact] public void Store_Liste_Zeitlimit_oder_Fehlercode_nie_aktuell()
    {
        Assert.Equal(UpdateZustand.Fehler, StoreAktualisierer.UpgradeListeAuswerten(Lauf("", -1, true), Kennungen, "1.0").Zustand);
        Assert.Equal(UpdateZustand.Fehler, StoreAktualisierer.UpgradeListeAuswerten(Lauf("", unchecked((int)0x8A15000F)), Kennungen, "1.0").Zustand);
    }

    [Fact] public void Store_Liste_sauber_ohne_Zeile_ist_aktuell_mit_Zeile_update()
    {
        Assert.Equal(UpdateZustand.Aktuell, StoreAktualisierer.UpgradeListeAuswerten(Lauf(StudioZeile), Kennungen, "1.0").Zustand);
        Assert.Equal(UpdateZustand.Aktuell, StoreAktualisierer.UpgradeListeAuswerten(
            Lauf("No installed package found matching input criteria.", WingetAktualisierer.KeinPaketGefunden), Kennungen, "1.0").Zustand);
        var update = StoreAktualisierer.UpgradeListeAuswerten(Lauf("Codex 9PLM9XGG6VKS 26.915 26.920.1 msstore"), Kennungen, "26.915");
        Assert.Equal(UpdateZustand.UpdateVerfuegbar, update.Zustand);
        Assert.Equal("26.920.1", update.VerfuegbareVersion);
    }

    // ---- CLI version gate ----
    [Fact] public void Cli_Versionsabfrage_ohne_Nummer_ist_Problem()
    {
        Assert.NotNull(CliAktualisierer.FingerabdruckAus(Lauf("command not found"), null).Problem);
        Assert.NotNull(CliAktualisierer.FingerabdruckAus(Lauf("2.1.278", 1), null).Problem);
        Assert.Null(CliAktualisierer.FingerabdruckAus(Lauf("2.1.278 (Claude Code)"), null).Problem);
    }
}
