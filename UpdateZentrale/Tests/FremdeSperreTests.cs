using UpdateZentrale.Models;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

public class FremdeSperreTests
{
    private sealed class Mitschrift : IProgress<string>
    {
        public List<string> Zeilen { get; } = new();
        public void Report(string value) => Zeilen.Add(value);
    }

    [Fact]
    public async Task Ohne_laufenden_Prozess_geht_es_sofort_weiter_ohne_Meldung()
    {
        var protokoll = new Mitschrift();
        var eintrag = new ProgrammEintrag { Name = "VS" };

        var ergebnis = await FremdeSperre.AbwartenAsync(eintrag, protokoll, _ => { }, CancellationToken.None);

        Assert.Null(ergebnis);
        Assert.Empty(protokoll.Zeilen);
    }

    [Fact]
    public async Task Wartet_bis_der_Prozess_weg_ist()
    {
        var protokoll = new Mitschrift();
        var abfragen = 0;
        var eintrag = new ProgrammEintrag { Name = "VS" };

        var ergebnis = await FremdeSperre.AbwartenAsync(eintrag, protokoll, _ => { }, CancellationToken.None,
            () => ++abfragen < 3 ? new[] { "BackgroundDownload" } : Array.Empty<string>(), TimeSpan.FromMilliseconds(1));

        Assert.Null(ergebnis);
        Assert.Equal(3, abfragen);
        Assert.Contains(protokoll.Zeilen, z => z.StartsWith("Warte auf BackgroundDownload"));
    }

    [Fact]
    public async Task Nach_dem_Limit_nennt_die_Meldung_den_Prozess()
    {
        var eintrag = new ProgrammEintrag { Name = "VS", WarteMinuten = 0 };

        var ergebnis = await FremdeSperre.AbwartenAsync(eintrag, new Mitschrift(), _ => { }, CancellationToken.None,
            () => new[] { "BackgroundDownload" }, TimeSpan.FromMilliseconds(1));

        Assert.Contains("BackgroundDownload", ergebnis);
    }

    [Fact]
    public async Task Abbruch_beendet_das_Warten()
    {
        using var abbruch = new CancellationTokenSource();
        abbruch.Cancel();
        var eintrag = new ProgrammEintrag { Name = "VS" };

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => FremdeSperre.AbwartenAsync(eintrag, new Mitschrift(), _ => { }, abbruch.Token,
            () => new[] { "BackgroundDownload" }, TimeSpan.FromSeconds(1)));
    }
}
