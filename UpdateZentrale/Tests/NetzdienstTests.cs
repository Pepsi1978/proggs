using System.Diagnostics;
using System.IO;
using System.Net;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

public sealed class NetzdienstTests
{
    private sealed class Verbindung : IDisposable
    {
        public IPAddress Adresse { get; }
        public bool Entsorgt { get; private set; }
        public Verbindung(IPAddress adresse) => Adresse = adresse;
        public void Dispose() => Entsorgt = true;
    }

    private static readonly IPAddress Tot1 = IPAddress.Parse("156.154.112.39");
    private static readonly IPAddress Tot2 = IPAddress.Parse("156.154.113.39");
    private static readonly IPAddress Gut = IPAddress.Parse("104.21.89.78");

    /// <summary>Dead addresses never answer; only the per-address deadline ends them.</summary>
    private static async Task<Verbindung> Verbinden(IPAddress adresse, CancellationToken abbruch)
    {
        if (!adresse.Equals(Gut)) await Task.Delay(Timeout.Infinite, abbruch);
        return new Verbindung(adresse);
    }

    [Fact]
    public async Task Tote_Sperradressen_vorn_gute_hinten_gewinnt_schnell()
    {
        var uhr = Stopwatch.StartNew();
        var sieger = await Netzdienst.WettlaufAsync([Tot1, Tot2, Gut], Verbinden,
            TimeSpan.FromSeconds(4), TimeSpan.FromMilliseconds(50), CancellationToken.None);
        Assert.Equal(Gut, sieger.Adresse);
        Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(2), "Wettlauf wartete die Frist der toten Adressen ab.");
    }

    [Fact]
    public async Task Fehlschlag_startet_sofort_die_naechste_Adresse()
    {
        var uhr = Stopwatch.StartNew();
        var sieger = await Netzdienst.WettlaufAsync([Tot1, Gut],
            (a, t) => a.Equals(Gut) ? Task.FromResult(new Verbindung(a)) : Task.FromException<Verbindung>(new IOException("refused")),
            TimeSpan.FromSeconds(4), TimeSpan.FromSeconds(10), CancellationToken.None);
        Assert.Equal(Gut, sieger.Adresse);
        Assert.True(uhr.Elapsed < TimeSpan.FromSeconds(2));
    }

    [Fact]
    public async Task Alle_tot_endet_nach_der_Frist_mit_Fehler()
    {
        await Assert.ThrowsAsync<AggregateException>(() => Netzdienst.WettlaufAsync([Tot1, Tot2], Verbinden,
            TimeSpan.FromMilliseconds(200), TimeSpan.FromMilliseconds(50), CancellationToken.None));
        await Assert.ThrowsAsync<AggregateException>(() => Netzdienst.WettlaufAsync<Verbindung>([], Verbinden,
            TimeSpan.FromMilliseconds(200), TimeSpan.FromMilliseconds(50), CancellationToken.None));
    }

    [Fact]
    public async Task Spaeter_Erfolg_eines_Verlierers_wird_entsorgt()
    {
        var spaet = new TaskCompletionSource<Verbindung>();
        var verlierer = new Verbindung(Tot1);
        var sieger = await Netzdienst.WettlaufAsync([Tot1, Gut],
            (a, t) => a.Equals(Gut) ? Task.FromResult(new Verbindung(a)) : spaet.Task,
            TimeSpan.FromSeconds(4), TimeSpan.FromMilliseconds(20), CancellationToken.None);
        Assert.Equal(Gut, sieger.Adresse);
        spaet.SetResult(verlierer);
        for (var i = 0; i < 50 && !verlierer.Entsorgt; i++) await Task.Delay(20);
        Assert.True(verlierer.Entsorgt);
    }

    [Fact]
    public async Task Abbruch_von_aussen_wird_durchgereicht()
    {
        using var abbruch = new CancellationTokenSource(TimeSpan.FromMilliseconds(100));
        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => Netzdienst.WettlaufAsync([Tot1, Tot2], Verbinden,
            TimeSpan.FromSeconds(10), TimeSpan.FromMilliseconds(20), abbruch.Token));
    }

    [Fact]
    public void Ordnen_IPv4_zuerst_ohne_Doppelte()
    {
        var v6 = IPAddress.Parse("2606:4700:3032::6815:594e");
        Assert.Equal([Tot1, Gut, v6], Netzdienst.Ordnen([v6, Tot1, Gut, Tot1, v6]));
    }

    [Fact]
    public void DoH_Antwort_nur_A_Eintraege()
    {
        const string json = "{\"Status\":0,\"Answer\":[{\"name\":\"dl.deno.land\",\"type\":5,\"data\":\"dl.deno-cdn.net.\"},"
                            + "{\"name\":\"dl.deno-cdn.net\",\"type\":1,\"data\":\"172.67.138.213\"},"
                            + "{\"name\":\"dl.deno-cdn.net\",\"type\":1,\"data\":\"104.21.89.78\"}]}";
        Assert.Equal([IPAddress.Parse("172.67.138.213"), Gut], Netzdienst.DohAuswerten(json));
        Assert.Empty(Netzdienst.DohAuswerten("{\"Status\":3}"));
        Assert.Empty(Netzdienst.DohAuswerten("kein json"));
    }

    /// <summary>Live check against the real host; only with UZ_LIVE_NETZ=1 (needs the network), else "skipped".</summary>
    [LiveFact]
    public async Task Live_Deno_Versionsabfrage()
    {
        var zeilen = new List<string>();
        var uhr = Stopwatch.StartNew();
        var ergebnis = await CliAktualisierer.UrlVersionAsync(Netzdienst.Client, "https://dl.deno.land/release-latest.txt",
            new SofortProtokoll(zeilen), CancellationToken.None);
        File.WriteAllText(Path.Combine(Path.GetTempPath(), "uz-live-netz.txt"),
            $"{ergebnis.Erfolg} {ergebnis.Version} {ergebnis.Problem} {uhr.Elapsed.TotalSeconds:F2}s\n" + string.Join("\n", zeilen));
        Assert.True(ergebnis.Erfolg, ergebnis.Problem);
        Assert.NotEmpty(await Netzdienst.DohAsync("dl.deno.land", CancellationToken.None));
    }

    private sealed class LiveFactAttribute : FactAttribute
    {
        public LiveFactAttribute()
        {
            if (Environment.GetEnvironmentVariable("UZ_LIVE_NETZ") != "1")
                Skip = "Live-Netztest: nur mit UZ_LIVE_NETZ=1.";
        }
    }

    private sealed class SofortProtokoll(List<string> zeilen) : IProgress<string>
    {
        public void Report(string value) => zeilen.Add(value);
    }
}
