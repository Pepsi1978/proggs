using System.Net;
using System.Net.Http;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using UpdateZentrale.ViewModels;
using Xunit;

namespace UpdateZentrale.Tests;

public sealed class NpmAbfrageTests
{
    private sealed class FakeHandler : HttpMessageHandler
    {
        private readonly Func<CancellationToken, Task<HttpResponseMessage>> _antwort;
        public FakeHandler(Func<CancellationToken, Task<HttpResponseMessage>> antwort) => _antwort = antwort;
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage anfrage, CancellationToken abbruch)
            => _antwort(abbruch);
    }

    private static HttpClient Netz(HttpStatusCode code, string text)
        => new(new FakeHandler(_ => Task.FromResult(new HttpResponseMessage(code) { Content = new StringContent(text) })));

    private static readonly IProgress<string> Still = new Progress<string>(_ => { });

    [Fact]
    public void Auswertung_Erfolg_leer_ungueltig()
    {
        Assert.Equal("2.1.279", CliAktualisierer.NpmAuswerten("{\"version\":\"2.1.279\"}").Version);
        Assert.False(CliAktualisierer.NpmAuswerten("{\"version\":\"\"}").Erfolg);
        Assert.False(CliAktualisierer.NpmAuswerten("{\"version\":\"latest\"}").Erfolg);
        Assert.False(CliAktualisierer.NpmAuswerten("{\"name\":\"x\"}").Erfolg);
        Assert.False(CliAktualisierer.NpmAuswerten("[1,2]").Erfolg);
        var kaputt = CliAktualisierer.NpmAuswerten("<html>Bad Gateway</html>");
        Assert.False(kaputt.Erfolg);
        Assert.Contains("kein gültiges JSON", kaputt.Problem);
    }

    [Fact]
    public async Task Http_Erfolg_liefert_Version()
    {
        var e = await CliAktualisierer.NpmVersionAsync(Netz(HttpStatusCode.OK, "{\"version\":\"0.156.0\"}"), "p", Still, default);
        Assert.True(e.Erfolg);
        Assert.Equal("0.156.0", e.Version);
    }

    [Fact]
    public async Task Http_Fehlerstatus_ist_Problem_nicht_fehlende_Quelle()
    {
        var e = await CliAktualisierer.NpmVersionAsync(Netz(HttpStatusCode.ServiceUnavailable, "down"), "p", Still, default);
        Assert.False(e.Erfolg);
        Assert.StartsWith("Registry-Abfrage fehlgeschlagen", e.Problem);
        Assert.DoesNotContain("Prüfquelle", e.Problem);
    }

    [Fact]
    public async Task Netzfehler_und_Http_Zeitlimit_sind_Problem()
    {
        var netz = new HttpClient(new FakeHandler(_ => throw new HttpRequestException("Host nicht erreichbar")));
        Assert.False((await CliAktualisierer.NpmVersionAsync(netz, "p", Still, default)).Erfolg);

        // HttpClient's own timeout arrives as a cancellation although nobody asked for one.
        var zeitlimit = new HttpClient(new FakeHandler(_ => throw new TaskCanceledException("Timeout")));
        Assert.False((await CliAktualisierer.NpmVersionAsync(zeitlimit, "p", Still, default)).Erfolg);
    }

    [Fact]
    public async Task Angeforderter_Abbruch_wird_weitergeworfen()
    {
        using var quelle = new CancellationTokenSource();
        var netz = new HttpClient(new FakeHandler(async t => { await Task.Delay(Timeout.Infinite, t); return new HttpResponseMessage(); }));
        var lauf = CliAktualisierer.NpmVersionAsync(netz, "p", Still, quelle.Token);
        quelle.Cancel();
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => lauf);
    }
}

public sealed class SammelbesitzValidierungTests
{
    private sealed class ZaehlAktualisierer : IAktualisierer
    {
        public int Aufrufe;
        public string Art => "fake";
        public Task<PruefErgebnis> PruefenAsync(ProgrammEintrag e, IProgress<string> p, CancellationToken a)
        {
            Interlocked.Increment(ref Aufrufe);
            return Task.FromResult(new PruefErgebnis(UpdateZustand.Aktuell, "1.0"));
        }
        public Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag e, IProgress<string> p, CancellationToken a)
            => throw new NotSupportedException();
        public Task<string> FingerabdruckAsync(ProgrammEintrag e, CancellationToken a) => Task.FromResult("");
    }

    private static (ProgrammViewModel karte, ZaehlAktualisierer fake) Karte(Laufkoordination k)
    {
        var fake = new ZaehlAktualisierer();
        return (new ProgrammViewModel(new ProgrammEintrag { Id = "x", Name = "x", Art = "fake" }, fake, new Einstellungen(), k), fake);
    }

    [Fact]
    public async Task Fremder_Koordinator_wird_abgelehnt()
    {
        var eigene = new Laufkoordination();
        var fremde = new Laufkoordination();
        var (karte, fake) = Karte(eigene);
        using var fremderBesitz = fremde.SammelBeginnen()!;
        using var einzel = eigene.EinzelBeginnen()!;   // the own coordination is busy

        Assert.False(eigene.IstAktiverSammelbesitz(fremderBesitz));
        await karte.PruefenImSammelAsync(fremderBesitz);
        Assert.Equal(0, fake.Aufrufe);
        Assert.Equal(ProgrammViewModel.BelegtText, karte.StatusText);
    }

    [Fact]
    public async Task Freigegebener_Sammelbesitz_wird_abgelehnt()
    {
        var k = new Laufkoordination();
        var (karte, fake) = Karte(k);
        var besitz = k.SammelBeginnen()!;
        besitz.Dispose();

        Assert.False(k.IstAktiverSammelbesitz(besitz));
        await karte.PruefenImSammelAsync(besitz);
        Assert.Equal(0, fake.Aufrufe);
    }

    [Fact]
    public async Task Alter_Sammelbesitz_kann_neuen_weder_nutzen_noch_freigeben()
    {
        var k = new Laufkoordination();
        var (karte, fake) = Karte(k);
        var alt = k.SammelBeginnen()!;
        alt.Dispose();
        using var neu = k.SammelBeginnen()!;

        Assert.False(k.IstAktiverSammelbesitz(alt));
        Assert.True(k.IstAktiverSammelbesitz(neu));

        await karte.PruefenImSammelAsync(alt);
        Assert.Equal(0, fake.Aufrufe);

        alt.Dispose();                        // second release of the old token
        Assert.True(k.IstAktiverSammelbesitz(neu));
        Assert.True(k.SammelLaeuft);

        await karte.PruefenImSammelAsync(neu);
        Assert.Equal(1, fake.Aufrufe);
    }

    [Fact]
    public void Einzelbesitz_ist_kein_Sammelbesitz()
    {
        var k = new Laufkoordination();
        using var einzel = k.EinzelBeginnen()!;
        Assert.False(k.IstAktiverSammelbesitz(einzel));
    }
}
