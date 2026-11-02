using UpdateZentrale.Models;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

public class FremdeSperreTests
{
    private static readonly string[] Blocker = { "BackgroundDownload" };

    private sealed class Mitschrift : IProgress<string>
    {
        public List<string> Zeilen { get; } = new();
        public void Report(string value) => Zeilen.Add(value);
    }

    private static ProgrammEintrag Eintrag(int minuten = 90)
        => new() { Id = "t", Name = "VS", Art = "winget", WarteMinuten = minuten, SperrFehlerMuster = @"\b5002\b" };

    private static UpdateKette Kette(SkriptProvider p, ProgrammEintrag e, Func<IReadOnlyList<string>> laufende)
        => new(p, e, new Mitschrift(), warten: (_, _) => Task.CompletedTask) { SperrLaufende = laufende };

    private static PruefErgebnis Fehler(string ausgabe) => new(UpdateZustand.Fehler, Meldung: "kaputt", Protokoll: ausgabe);
    private static readonly PruefErgebnis Offen = new(UpdateZustand.UpdateVerfuegbar, "x", "2.0");
    private static readonly PruefErgebnis Aktuell = new(UpdateZustand.Aktuell, "2.0");

    [Fact]
    public async Task Wartet_bis_der_Prozess_weg_ist()
    {
        var protokoll = new Mitschrift();
        var abfragen = 0;

        var ergebnis = await FremdeSperre.AbwartenAsync(Eintrag(), protokoll, _ => { }, TimeSpan.FromHours(1),
            () => ++abfragen < 3 ? Blocker : Array.Empty<string>(), (_, _) => Task.CompletedTask, default);

        Assert.Null(ergebnis);
        Assert.Equal(3, abfragen);
        Assert.Contains(protokoll.Zeilen, z => z.StartsWith("Warte auf BackgroundDownload"));
    }

    [Fact]
    public async Task Ohne_Prozess_keine_Meldung()
    {
        var protokoll = new Mitschrift();
        Assert.Null(await FremdeSperre.AbwartenAsync(Eintrag(), protokoll, _ => { }, TimeSpan.FromHours(1),
            () => Array.Empty<string>(), (_, _) => Task.CompletedTask, default));
        Assert.Empty(protokoll.Zeilen);
    }

    [Fact]
    public async Task Sperrfehler_wird_nach_dem_Warten_genau_einmal_wiederholt()
    {
        var p = new SkriptProvider(new[] { "A", "B" }, new[] { Aktuell },
            new[] { Fehler("Exitcode: 5002"), new PruefErgebnis(UpdateZustand.Fertig) });
        var u = await Kette(p, Eintrag(), () => Array.Empty<string>()).AusfuehrenAsync("A", Offen, default);

        Assert.Equal(2, p.Updates);
        Assert.Equal(LaufErgebnis.Erfolgreich, u.Ergebnis);
    }

    [Fact]
    public async Task Zweiter_Sperrfehler_endet_die_Kette()
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen }, new[] { Fehler("5002"), Fehler("5002"), Fehler("5002") });
        var u = await Kette(p, Eintrag(), () => Array.Empty<string>()).AusfuehrenAsync("A", Offen, default);

        Assert.Equal(2, p.Updates);
        Assert.Equal(LaufErgebnis.Fehlgeschlagen, u.Ergebnis);
    }

    [Fact]
    public async Task Anderer_Fehler_wird_auch_bei_laufendem_Prozess_nicht_wiederholt()
    {
        var abfragen = 0;
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen }, new[] { Fehler("Platte voll") });
        // Free for the first look, then running while the installer fails.
        var u = await Kette(p, Eintrag(), () => ++abfragen == 1 ? Array.Empty<string>() : Blocker).AusfuehrenAsync("A", Offen, default);

        Assert.Equal(1, p.Updates);
        Assert.Equal("kaputt", u.Meldung);
    }

    [Fact]
    public async Task Limit_startet_kein_Update_und_keine_Reparatursitzung()
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen });
        var u = await Kette(p, Eintrag(0), () => Blocker).AusfuehrenAsync("A", Offen, default);

        Assert.Equal(0, p.Updates);
        Assert.Equal(LaufErgebnis.Fehlgeschlagen, u.Ergebnis);
        Assert.Contains("BackgroundDownload", u.Meldung);
        Assert.False(ClaudeUebergabe.AutomatischRelevant(new UpdateBericht { Ergebnis = u.Ergebnis, Meldung = u.Meldung }));
    }

    [Fact]
    public async Task Limit_vor_der_Wiederholung_behaelt_den_echten_Fehler()
    {
        var abfragen = 0;
        var e = Eintrag(0);
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen }, new[] { Fehler("5002") });
        var u = await Kette(p, e, () => ++abfragen == 1 ? Array.Empty<string>() : Blocker).AusfuehrenAsync("A", Offen, default);

        Assert.Equal(1, p.Updates);
        Assert.Contains("kaputt", u.Meldung);
        Assert.Equal("5002", u.FuerKarte.Protokoll);
    }

    [Fact]
    public async Task Abbruch_beendet_das_Warten()
    {
        using var abbruch = new CancellationTokenSource();
        abbruch.Cancel();

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() => FremdeSperre.AbwartenAsync(Eintrag(), new Mitschrift(), _ => { },
            TimeSpan.FromHours(1), () => Blocker, Task.Delay, abbruch.Token));
    }
}
