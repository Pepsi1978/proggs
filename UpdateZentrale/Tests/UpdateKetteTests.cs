using System.IO;
using System.Text.Json;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using UpdateZentrale.ViewModels;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>
/// Scripted provider: each call takes the next queued answer; the last one repeats. Nothing is
/// installed, nothing touches the network.
/// </summary>
internal sealed class SkriptProvider : IAktualisierer
{
    private readonly Queue<string> _staende;
    private readonly Queue<PruefErgebnis> _pruefungen;
    private readonly Queue<PruefErgebnis> _updates;
    private string _letzterStand = "";
    private PruefErgebnis _letztePruefung = new(UpdateZustand.Unbekannt);
    private PruefErgebnis _letztesUpdate = new(UpdateZustand.Fertig);

    public int Updates, Pruefungen, Staende;
    public Func<Task>? VorUpdate;

    public SkriptProvider(IEnumerable<string> staende, IEnumerable<PruefErgebnis> pruefungen, IEnumerable<PruefErgebnis>? updates = null)
    {
        _staende = new Queue<string>(staende);
        _pruefungen = new Queue<PruefErgebnis>(pruefungen);
        _updates = new Queue<PruefErgebnis>(updates ?? new[] { new PruefErgebnis(UpdateZustand.Fertig) });
    }

    public string Art => "fake";

    public Task<string> FingerabdruckAsync(ProgrammEintrag e, CancellationToken a)
    {
        Staende++;
        if (_staende.Count > 0) _letzterStand = _staende.Dequeue();
        return Task.FromResult(_letzterStand);
    }

    public Task<PruefErgebnis> PruefenAsync(ProgrammEintrag e, IProgress<string> p, CancellationToken a)
    {
        Pruefungen++;
        if (_pruefungen.Count > 0) _letztePruefung = _pruefungen.Dequeue();
        return Task.FromResult(_letztePruefung);
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag e, IProgress<string> p, CancellationToken a)
    {
        Updates++;
        if (VorUpdate is not null) await VorUpdate();
        if (_updates.Count > 0) _letztesUpdate = _updates.Dequeue();
        return _letztesUpdate;
    }
}

public sealed class UpdateKetteTests
{
    private static PruefErgebnis Offen(string ziel) => new(UpdateZustand.UpdateVerfuegbar, "x", ziel);
    private static readonly PruefErgebnis Aktuell = new(UpdateZustand.Aktuell, "neu");

    private sealed class Mitschrift : IProgress<string>
    {
        public readonly List<string> Zeilen = new();
        public void Report(string value) => Zeilen.Add(value);
    }

    private static (UpdateKette kette, Mitschrift log, List<TimeSpan> pausen) Kette(SkriptProvider p, int max = 3, int nachpruefungen = 4)
    {
        var log = new Mitschrift();
        var pausen = new List<TimeSpan>();
        var kette = new UpdateKette(p, new ProgrammEintrag { Id = "t", Name = "T", Art = "winget" }, log,
            warten: (d, _) => { pausen.Add(d); return Task.CompletedTask; })
        { MaxDurchlaeufe = max, MaxNachpruefungen = nachpruefungen };
        return (kette, log, pausen);
    }

    // 1
    [Fact]
    public async Task Fertig_und_neuer_Stand_aber_dasselbe_Update_offen_ist_kein_Erfolg()
    {
        var p = new SkriptProvider(new[] { "B" }, new[] { Offen("2.0") });
        var (kette, _, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(1, p.Updates);                         // the same target is never re-run
        Assert.Equal(LaufErgebnis.NichtVerifiziert, u.Ergebnis);
        Assert.Equal(UpdateZustand.Fehler, u.FuerKarte.Zustand);
        Assert.Contains("Dasselbe Update (2.0)", u.Meldung);
        Assert.Contains("A → B", u.Meldung);                 // the fingerprint did move
    }

    [Fact]
    public async Task Leeres_Restangebot_wird_nicht_wiederholt()
    {
        var p = new SkriptProvider(new[] { "B" }, new[] { Offen("") });
        var (kette, _, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(1, p.Updates);
        Assert.Equal(LaufErgebnis.NichtVerifiziert, u.Ergebnis);
        Assert.Contains("nicht lesbar", u.Meldung);
    }

    [Fact]
    public async Task Anderes_Angebot_ohne_Fortschritt_wird_nicht_wiederholt()
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen("2.1") });
        var (kette, _, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(1, p.Updates);
        Assert.Equal(LaufErgebnis.NichtVerifiziert, u.Ergebnis);
    }

    // 2
    [Fact]
    public async Task Folgeupdate_erreicht_im_zweiten_Durchlauf_den_Endstand()
    {
        var p = new SkriptProvider(new[] { "1.1", "1.2" }, new[] { Offen("1.2"), Aktuell });
        var (kette, log, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("1.0", Offen("1.1"), default);

        Assert.Equal(LaufErgebnis.Erfolgreich, u.Ergebnis);
        Assert.Equal(2, p.Updates);
        Assert.Equal(2, u.Uebergaenge!.Count);
        Assert.Contains(log.Zeilen, z => z.StartsWith("Folgeupdate 2 von 3"));
        Assert.Contains("1.0 → 1.2", u.Meldung);
        Assert.Equal(UpdateZustand.Aktuell, u.FuerKarte.Zustand);
    }

    // 3
    [Fact]
    public async Task Unveraendert_genau_ein_Aufruf_dann_Stopp()
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen("2.0") });
        var (kette, _, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(1, p.Updates);
        Assert.Equal(LaufErgebnis.NichtVerifiziert, u.Ergebnis);
        Assert.Contains("kein erneuter Versuch", u.Meldung);
    }

    // 4
    [Fact]
    public async Task Fortschritt_in_jedem_Durchlauf_endet_hart_nach_N()
    {
        var p = new SkriptProvider(new[] { "2", "3", "4", "5" }, new[] { Offen("b"), Offen("c"), Offen("d"), Offen("e") });
        var (kette, _, _) = Kette(p, max: 3);
        var u = await kette.AusfuehrenAsync("1", Offen("a"), default);

        Assert.Equal(3, p.Updates);
        Assert.Equal(LaufErgebnis.NichtVerifiziert, u.Ergebnis);
        Assert.Contains("Nach 3 Durchläufen", u.Meldung);
        Assert.Equal(3, u.Uebergaenge!.Count);
    }

    // 5
    [Fact]
    public async Task Nachlaufendes_Kind_wird_abgewartet_ein_Aufruf_Erfolg_erst_spaeter()
    {
        var p = new SkriptProvider(new[] { "A", "A", "B" }, new[] { Aktuell });
        var (kette, _, pausen) = Kette(p, nachpruefungen: 4);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(1, p.Updates);
        Assert.Equal(LaufErgebnis.Erfolgreich, u.Ergebnis);
        Assert.Equal(2, pausen.Count);          // confirmed only on the third reading
        Assert.Equal(1, p.Pruefungen);          // the provider is asked once the state moved
    }

    // 6
    [Fact]
    public async Task Nachpruefung_ist_begrenzt_und_endet_ohne_Erfolg()
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen("2.0") });
        var (kette, _, pausen) = Kette(p, nachpruefungen: 4);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.NotEqual(LaufErgebnis.Erfolgreich, u.Ergebnis);
        Assert.Equal(3, pausen.Count);
        Assert.Equal(4, p.Staende);
        Assert.Equal(1, p.Pruefungen);
    }

    // 7
    [Theory]
    [InlineData(UpdateZustand.Fehler)]
    [InlineData(UpdateZustand.Unbekannt)]
    public async Task Nachpruefung_Fehler_oder_Unbekannt_ist_kein_Erfolg(UpdateZustand zustand)
    {
        var p = new SkriptProvider(new[] { "B" }, new[] { new PruefErgebnis(zustand, Meldung: "kaputt") });
        var (kette, _, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(LaufErgebnis.NichtVerifiziert, u.Ergebnis);
        Assert.Equal(1, p.Updates);
    }

    [Fact]
    public async Task Unlesbarer_Stand_nach_dem_Update_ist_kein_Erfolg()
    {
        var p = new SkriptProvider(new[] { "" }, new[] { Aktuell });
        var (kette, _, _) = Kette(p);
        Assert.Equal(LaufErgebnis.NichtVerifiziert, (await kette.AusfuehrenAsync("A", Offen("2.0"), default)).Ergebnis);
    }

    // 8
    [Theory]
    [InlineData(UpdateZustand.Abgebrochen, LaufErgebnis.Abgebrochen)]
    [InlineData(UpdateZustand.Fehler, LaufErgebnis.Fehlgeschlagen)]
    public async Task Abbruch_oder_Fehler_wird_nicht_wiederholt(UpdateZustand update, LaufErgebnis erwartet)
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen("2.0") }, new[] { new PruefErgebnis(update, Meldung: "x") });
        var (kette, _, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(erwartet, u.Ergebnis);
        Assert.Equal(1, p.Updates);
        Assert.Equal(0, p.Pruefungen);
    }

    // 9
    [Fact]
    public async Task Bereits_aktuell_mit_bestaetigter_Nachpruefung_zeigt_aktuell()
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Aktuell },
            new[] { new PruefErgebnis(UpdateZustand.Aktuell, Meldung: "War bereits aktuell.") });
        var (kette, _, pausen) = Kette(p);
        var u = await kette.AusfuehrenAsync("A", Offen("2.0"), default);

        Assert.Equal(LaufErgebnis.BereitsAktuell, u.Ergebnis);
        Assert.Equal(UpdateZustand.Aktuell, u.FuerKarte.Zustand);
        Assert.Empty(pausen);
        Assert.False(new UpdateBericht { Ergebnis = u.Ergebnis }.IstFehler);
    }

    // 13
    [Fact]
    public void Vorpruefung_aktuell_ergibt_keinen_Updateaufruf()
    {
        var u = UpdateKette.BereitsAktuell("A", Aktuell);
        Assert.NotNull(u);
        Assert.Equal(0, u!.UpdateAufrufe);
        Assert.Equal(UpdateZustand.Aktuell, u.FuerKarte.Zustand);
        Assert.Null(UpdateKette.BereitsAktuell("A", Offen("2.0")));
    }

    // 10a
    [Fact]
    public async Task Staged_wird_nicht_wiederholt_und_merkt_sich_das_Ziel()
    {
        var p = new SkriptProvider(new[] { "A" }, new[] { Offen("2.0") },
            new[] { new PruefErgebnis(UpdateZustand.Fertig, Meldung: "beim Neustart", ErstNachNeustart: true) });
        var (kette, _, _) = Kette(p);
        var u = await kette.AusfuehrenAsync("1.0", Offen("2.0"), default);

        Assert.Equal(LaufErgebnis.Ausstehend, u.Ergebnis);
        Assert.Equal("2.0", u.AusstehendeVersion);
        Assert.Equal(1, p.Updates);
        Assert.Equal(0, p.Pruefungen);
    }

    // 10b
    [Fact]
    public void Staged_Bestaetigung_nur_mit_Ziel_und_aktueller_Pruefung()
    {
        var offen = new UpdateBericht { Zeit = DateTime.Now.AddDays(-1), VersionNachher = "1.0", AusstehendeVersion = "2.0" };

        Assert.Null(UpdateKette.StagedUrteil(offen, "1.5", Aktuell, DateTime.Now));            // any change is not enough
        Assert.Null(UpdateKette.StagedUrteil(offen, "2.0", Offen("2.1"), DateTime.Now));       // still/again offered
        Assert.Null(UpdateKette.StagedUrteil(offen, "", Aktuell, DateTime.Now));              // unreadable
        Assert.Equal(LaufErgebnis.Erfolgreich, UpdateKette.StagedUrteil(offen, "2.0", Aktuell, DateTime.Now)!.Value.Ergebnis);
        Assert.Equal(LaufErgebnis.Erfolgreich, UpdateKette.StagedUrteil(offen, "2.0.1", Aktuell, DateTime.Now)!.Value.Ergebnis);

        var alt = new UpdateBericht { Zeit = DateTime.Now.AddDays(-8), VersionNachher = "1.0", AusstehendeVersion = "2.0" };
        Assert.Equal(LaufErgebnis.NichtVerifiziert, UpdateKette.StagedUrteil(alt, "1.0", Offen("2.0"), DateTime.Now)!.Value.Ergebnis);

        var ohneZiel = new UpdateBericht { Zeit = DateTime.Now, VersionNachher = "1.0" };
        Assert.Null(UpdateKette.StagedUrteil(ohneZiel, "1.1", Offen("1.2"), DateTime.Now));
        Assert.Equal(LaufErgebnis.Erfolgreich, UpdateKette.StagedUrteil(ohneZiel, "1.1", Aktuell, DateTime.Now)!.Value.Ergebnis);
    }

    [Fact]
    public void Alte_Verlaufseintraege_bleiben_lesbar_neuer_Wert_wird_angehaengt()
    {
        var alt = JsonSerializer.Deserialize<UpdateBericht>("{\"ProgrammId\":\"x\",\"Ergebnis\":4,\"Meldung\":\"m\"}")!;
        Assert.Equal(LaufErgebnis.NichtVerifiziert, alt.Ergebnis);
        Assert.Equal(5, (int)LaufErgebnis.BereitsAktuell);
        Assert.Equal("Bereits aktuell", new UpdateBericht { Ergebnis = LaufErgebnis.BereitsAktuell }.ErgebnisText);
    }
}

/// <summary>
/// The card as a whole, with the log redirected to the test folder (see TestUmgebung).
/// Both tests in one class: xunit runs them one after the other.
/// </summary>
public sealed class KartenUpdateKetteTests
{
    private static ProgrammViewModel Karte(string id, IAktualisierer p, Laufkoordination k)
        => new(new ProgrammEintrag { Id = id, Name = "Karte " + id, Art = "fake" }, p, new Einstellungen(), k);

    // 11
    [Fact]
    public async Task Koordination_bleibt_ueber_Nachpruefung_und_Folgedurchlauf_belegt()
    {
        var k = new Laufkoordination();
        var p = new SkriptProvider(new[] { "1.0", "1.1", "1.1", "1.2" }, new[] { Offen("1.1"), Offen("1.1"), Offen("1.2"), Aktuell });
        var id = "k11-" + Guid.NewGuid().ToString("N");
        var karte = Karte(id, p, k);
        var andere = Karte("andere-" + id, new SkriptProvider(new[] { "x" }, new[] { Aktuell }), k);

        var belegtBeiNachpruefung = new List<bool>();
        var zweiterVersuchAbgewiesen = false;
        karte.KettenWarten = async (_, _) =>
        {
            belegtBeiNachpruefung.Add(k.Belegt);
            await andere.PruefenCommand.ExecuteAsync(null);
            zweiterVersuchAbgewiesen = andere.StatusText == ProgrammViewModel.BelegtText;
        };
        p.VorUpdate = () => { belegtBeiNachpruefung.Add(k.Belegt); return Task.CompletedTask; };

        // Pass 1 moves the state at once but the provider still shows the old offer (one lag poll),
        // pass 2 reaches the end state.
        await karte.AktualisierenCommand.ExecuteAsync(null);

        Assert.Equal(2, p.Updates);
        Assert.Equal(3, belegtBeiNachpruefung.Count);   // two update calls + one settle pause
        Assert.All(belegtBeiNachpruefung, Assert.True);
        Assert.True(zweiterVersuchAbgewiesen);
        Assert.False(k.Belegt);
        Assert.Equal(UpdateZustand.Aktuell, karte.Zustand);
        Assert.Equal(LaufErgebnis.Erfolgreich, karte.LetzterBericht!.Ergebnis);
    }

    // 12
    [Fact]
    public async Task Genau_ein_Abschluss_pro_Aktion_und_nie_vorzeitig_Erfolgreich()
    {
        var k = new Laufkoordination();
        var id = "k12-" + Guid.NewGuid().ToString("N");
        var p = new SkriptProvider(new[] { "1.0", "1.1", "1.2" }, new[] { Offen("1.1"), Offen("1.2"), Aktuell });
        var karte = Karte(id, p, k);
        karte.KettenWarten = (_, _) => Task.CompletedTask;

        string? verlaufWaehrendDesLaufs = null;
        p.VorUpdate = () =>
        {
            if (p.Updates == 2)   // before the second pass: nothing final may be written yet
                verlaufWaehrendDesLaufs = File.Exists(Protokollierung.VerlaufsDatei)
                    ? string.Join("\n", File.ReadAllLines(Protokollierung.VerlaufsDatei).Where(z => z.Contains(id)))
                    : "";
            return Task.CompletedTask;
        };

        await karte.AktualisierenCommand.ExecuteAsync(null);

        Assert.Equal("", verlaufWaehrendDesLaufs);

        var eintraege = File.ReadAllLines(Protokollierung.VerlaufsDatei).Where(z => z.Contains(id)).ToList();
        Assert.Single(eintraege);
        Assert.Equal(LaufErgebnis.Erfolgreich, JsonSerializer.Deserialize<UpdateBericht>(eintraege[0])!.Ergebnis);

        var tag = File.ReadAllLines(Protokollierung.TagesDatei());
        var kopf = Array.FindIndex(tag, z => z.Contains("Update-Lauf: Karte " + id));
        Assert.True(kopf >= 0);
        var fuesse = tag.Skip(kopf).TakeWhile((z, i) => i == 0 || !z.StartsWith("Update-Lauf:"))
            .Count(z => z.StartsWith("Ergebnis:"));
        Assert.Equal(1, fuesse);
        Assert.Single(tag, z => z.Contains("Update-Lauf: Karte " + id));
    }

    private static PruefErgebnis Offen(string ziel) => new(UpdateZustand.UpdateVerfuegbar, "x", ziel);
    private static readonly PruefErgebnis Aktuell = new(UpdateZustand.Aktuell, "neu");
}
