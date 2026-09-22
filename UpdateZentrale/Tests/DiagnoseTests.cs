using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Text;
using System.Text.Json;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using UpdateZentrale.ViewModels;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>Reads the structured events the app wrote into the private test log folder.</summary>
internal static class Ereignisse
{
    public static List<JsonElement> Alle(string? ordner = null)
    {
        var liste = new List<JsonElement>();
        foreach (var datei in Directory.EnumerateFiles(ordner ?? Protokollierung.Ordner, "diagnose-*.jsonl"))
        {
            using var strom = new FileStream(datei, FileMode.Open, FileAccess.Read, FileShare.ReadWrite | FileShare.Delete);
            using var leser = new StreamReader(strom, Encoding.UTF8);
            while (leser.ReadLine() is { } zeile)
                if (!string.IsNullOrWhiteSpace(zeile)) liste.Add(JsonDocument.Parse(zeile).RootElement.Clone());
        }
        return liste;
    }

    public static string? Text(this JsonElement e, string feld)
        => e.TryGetProperty(feld, out var w) && w.ValueKind == JsonValueKind.String ? w.GetString() : null;

    public static JsonElement? Daten(this JsonElement e, string feld)
        => e.TryGetProperty("daten", out var d) && d.TryGetProperty(feld, out var w) ? w : null;
}

public sealed class DiagnoseSchemaTests
{
    // 1
    [Fact]
    public void Zeile_hat_stabile_Schemafelder_in_fester_Reihenfolge()
    {
        var zeile = Diagnose.Zeile(Schwere.Warnung, "test", "test.ereignis", "Hallo", "phase1", null, 12, "ok",
            new Dictionary<string, object?> { ["zahl"] = 3, ["wahr"] = true, ["text"] = "x" });
        var e = JsonDocument.Parse(zeile).RootElement;
        var namen = e.EnumerateObject().Select(p => p.Name).ToList();

        Assert.Equal(new[] { "schema", "zeit", "schwere", "komponente", "typ", "sitzung", "phase", "meldung", "dauerMs", "ergebnis", "daten" }, namen);
        Assert.Equal(Diagnose.SchemaVersion, e.GetProperty("schema").GetInt32());
        Assert.EndsWith("Z", e.Text("zeit"));
        Assert.True(DateTimeOffset.TryParse(e.Text("zeit"), out _));
        Assert.Equal("warnung", e.Text("schwere"));
        Assert.Equal(Diagnose.SitzungId, e.Text("sitzung"));
        Assert.Equal(12, e.GetProperty("dauerMs").GetInt64());
        Assert.Equal(3, e.Daten("zahl")!.Value.GetInt32());
        Assert.DoesNotContain('\n', zeile);
    }

    // 6
    [Theory]
    [InlineData("password=hunter2", "hunter2")]
    [InlineData("PASSWORD: Geheim123", "Geheim123")]
    [InlineData("--api-key sk-abcdefghijklmnop1234", "sk-abcdefghijklmnop1234")]
    [InlineData("--Token=abc.def.ghi", "abc.def.ghi")]
    [InlineData("-Passwort 'Sommer2026!'", "Sommer2026!")]
    [InlineData("Authorization: Bearer eyJhbGciOi.payload123456.sig123456789", "payload123456")]
    [InlineData("authorization=Basic dXNlcjpwYXNz", "dXNlcjpwYXNz")]
    [InlineData("https://frank:supergeheim@example.com/repo.git", "supergeheim")]
    [InlineData("{\"apiKey\": \"AIzaSyA1234567890abcdefghijklmnopqrstu\"}", "AIzaSyA1234567890abcdefghijklmnopqrst")]
    [InlineData("GITHUB_TOKEN=ghp_abcdefghijklmnopqrstuvwxyz0123456789", "ghp_abcdefghijklmnopqrstuvwxyz0123456789")]
    [InlineData("export OPENAI_API_KEY=\"sk-proj-ABCDEFGHIJKLMNOPQRST\"", "sk-proj-ABCDEFGHIJKLMNOPQRST")]
    [InlineData("client_secret=xyz987&x=1", "xyz987")]
    [InlineData("Cookie: session=abcdef0123456789", "abcdef0123456789")]
    [InlineData("aws AKIAABCDEFGHIJKLMNOP", "AKIAABCDEFGHIJKLMNOP")]
    public void Geheimnisse_werden_in_allen_Schreibweisen_maskiert(string eingabe, string geheim)
    {
        var sauber = Bereinigung.Bereinigen(eingabe);
        Assert.DoesNotContain(geheim, sauber);
        Assert.Contains(Bereinigung.Maske, sauber);
    }

    [Fact]
    public void Harmlose_Texte_bleiben_erhalten()
    {
        const string text = "winget upgrade --id Anthropic.Claude --exact --silent | Version 2.1.278 | 3 Token übrig";
        Assert.Equal(text, Bereinigung.Bereinigen(text));
    }

    [Fact]
    public void Kuerzen_hat_sichtbare_Marke_und_urspruengliche_Laenge()
    {
        var lang = new string('a', 10_000) + "ENDE";
        var kurz = Bereinigung.Kuerzen(lang, 1000);
        Assert.True(kurz.Length < 1200);
        Assert.Contains("gekürzt", kurz);
        Assert.Contains("von 10004 Zeichen", kurz);
        Assert.EndsWith("ENDE", kurz);
        Assert.Equal("kurz", Bereinigung.Kuerzen("kurz", 1000));
    }

    [Fact]
    public void Ereigniszeile_ist_bereinigt_und_begrenzt()
    {
        var zeile = Diagnose.Zeile(Schwere.Info, "t", "t", "token=GEHEIM " + new string('x', 5000), null, null, null, null,
            new Dictionary<string, object?> { ["ausgabe"] = "password=hunter2 " + new string('y', 20_000) });
        Assert.DoesNotContain("GEHEIM", zeile);
        Assert.DoesNotContain("hunter2", zeile);
        Assert.True(zeile.Length < 12_000, "Zeile " + zeile.Length);
        Assert.Contains("gekürzt", zeile);
    }
}

public sealed class DiagnoseKorrelationTests
{
    private static PruefErgebnis Offen(string ziel) => new(UpdateZustand.UpdateVerfuegbar, "x", ziel);
    private static readonly PruefErgebnis Aktuell = new(UpdateZustand.Aktuell, "neu");

    // 2 + 3
    [Fact]
    public async Task Neues_Katalogprogramm_mit_neuem_Provider_erbt_die_Diagnose_und_Korrelation()
    {
        var id = "neu-" + Guid.NewGuid().ToString("N")[..10];
        var eintrag = new ProgrammEintrag { Id = id, Name = "Neues Programm " + id, Art = "ganzneueart" };
        var provider = new SkriptProvider(new[] { "1.0", "1.1" }, new[] { Offen("1.1"), Offen("1.1"), Aktuell });
        var k = new Laufkoordination();
        var karte = new ProgrammViewModel(eintrag, provider, new Einstellungen(), k) { KettenWarten = (_, _) => Task.CompletedTask };

        await karte.PruefenCommand.ExecuteAsync(null);
        await karte.AktualisierenCommand.ExecuteAsync(null);

        var alle = Ereignisse.Alle().Where(e => e.Text("programm") == id).ToList();
        var vorgaenge = alle.Where(e => e.Text("typ") == "vorgang.beginn").ToList();
        Assert.Equal(new[] { "pruefung", "update" }, vorgaenge.Select(e => e.Text("vorgangsart")));

        foreach (var beginn in vorgaenge)
        {
            var vid = beginn.Text("vorgang");
            var eigene = alle.Where(e => e.Text("vorgang") == vid).ToList();
            Assert.All(eigene, e => Assert.Equal(Diagnose.SitzungId, e.Text("sitzung")));
            Assert.All(eigene, e => Assert.Equal("ganzneueart", e.Text("art")));
            Assert.Contains(eigene, e => e.Text("typ")!.StartsWith("provider.pruefen"));
            var ende = Assert.Single(eigene, e => e.Text("typ") == "vorgang.ende");
            Assert.True(ende.TryGetProperty("dauerMs", out _));
            Assert.False(string.IsNullOrEmpty(ende.Text("ergebnis")));
        }

        var update = alle.Where(e => e.Text("vorgang") == vorgaenge[1].Text("vorgang")).Select(e => e.Text("typ")).ToList();
        Assert.Contains("provider.aktualisieren.beginn", update);
        Assert.Contains("kette.vorpruefung", update);
        Assert.Contains("kette.durchlauf", update);
        Assert.Contains("kette.urteil", update);
        Assert.Contains("karte.zustand", update);
    }

    // 2 (Sammellauf)
    [Fact]
    public async Task Einzelvorgaenge_im_Sammellauf_tragen_die_Eltern_ID()
    {
        var id = "sam-" + Guid.NewGuid().ToString("N")[..10];
        var k = new Laufkoordination();
        var karten = Enumerable.Range(0, 2).Select(i => new ProgrammViewModel(
            new ProgrammEintrag { Id = id + "-" + i, Name = "S" + i, Art = "fake" },
            new SkriptProvider(new[] { "1" }, new[] { Aktuell }), new Einstellungen(), k)).ToList();

        string sammelId;
        using (var sammel = Diagnose.VorgangBeginnen("sammelpruefung", null, "Test-Sammellauf"))
        using (var besitz = k.SammelBeginnen()!)
        {
            sammelId = sammel.Id;
            foreach (var karte in karten) await karte.PruefenImSammelAsync(besitz);
            sammel.Beenden("abgeschlossen");
        }

        var kinder = Ereignisse.Alle().Where(e => e.Text("programm")?.StartsWith(id) == true && e.Text("typ") == "vorgang.beginn").ToList();
        Assert.Equal(2, kinder.Count);
        Assert.All(kinder, e => Assert.Equal(sammelId, e.Text("eltern")));
        Assert.Single(Ereignisse.Alle(), e => e.Text("vorgang") == sammelId && e.Text("typ") == "vorgang.ende");
    }

    // 2 (abgewiesen/doppelt)
    [Fact]
    public async Task Abgewiesener_Start_wird_protokolliert()
    {
        var id = "abw-" + Guid.NewGuid().ToString("N")[..10];
        var k = new Laufkoordination();
        var karte = new ProgrammViewModel(new ProgrammEintrag { Id = id, Name = "A", Art = "fake" },
            new SkriptProvider(new[] { "1" }, new[] { Aktuell }), new Einstellungen(), k);
        using (k.EinzelBeginnen())
            await karte.PruefenCommand.ExecuteAsync(null);

        Assert.Contains(Ereignisse.Alle(), e => e.Text("typ") == "start.abgewiesen" && e.Daten("programm")?.GetString() == id);
    }

    // 5
    [Fact]
    public async Task Ausnahme_im_Provider_wird_vollstaendig_mit_Kontext_erfasst()
    {
        var id = "aus-" + Guid.NewGuid().ToString("N")[..10];
        var karte = new ProgrammViewModel(new ProgrammEintrag { Id = id, Name = "Kaputt", Art = "fake" },
            new WerfenderProvider(), new Einstellungen(), new Laufkoordination());

        await karte.PruefenCommand.ExecuteAsync(null);

        Assert.Equal(UpdateZustand.Fehler, karte.Zustand);
        var alle = Ereignisse.Alle().Where(e => e.Text("programm") == id).ToList();
        var ausnahme = Assert.Single(alle, e => e.Text("typ") == "ausnahme");
        Assert.Equal(typeof(InvalidOperationException).FullName, ausnahme.Daten("ausnahmeTyp")!.Value.GetString());
        Assert.Contains("WerfenderProvider", ausnahme.Daten("stack")!.Value.GetString());
        Assert.Contains("Innere Ursache", ausnahme.Daten("inner")!.Value.GetString());
        Assert.DoesNotContain("hunter2", ausnahme.Daten("ausnahmeMeldung")!.Value.GetString());
        Assert.Contains(alle, e => e.Text("typ") == "vorgang.ende" && e.Text("ergebnis") == "Ausnahme");
    }

    [Fact]
    public void Unbehandelte_Ausnahme_wird_einmal_voll_und_danach_als_Verweis_erfasst()
    {
        var marke = "unb-" + Guid.NewGuid().ToString("N")[..10];
        Exception fehler;
        try { throw new ApplicationException(marke); } catch (Exception ex) { fehler = ex; }

        Diagnose.UnbehandelteAusnahme("ui", fehler, beendetApp: false);
        Diagnose.UnbehandelteAusnahme("appdomain", fehler, beendetApp: true);
        Diagnose.UnbehandelteAusnahme("task", null, beendetApp: false);

        var alle = Ereignisse.Alle();
        Assert.Single(alle, e => e.Text("typ") == "ausnahme" && e.Text("meldung")!.Contains(marke));
        Assert.Contains(alle, e => e.Text("typ") == "ausnahme.unbehandelt.wiederholt" && e.Daten("quelle")?.GetString() == "appdomain");
        Assert.Contains(alle, e => e.Text("typ") == "ausnahme.unbehandelt");
    }

    // 11
    [Fact]
    public void LetzteBerichte_liest_alte_Zeilen_und_ueberspringt_kaputte()
    {
        var id = "alt-" + Guid.NewGuid().ToString("N")[..10];
        var alt = "{\"Zeit\":\"2026-09-21T14:26:51.9385802+02:00\",\"ProgrammId\":\"" + id + "\",\"Name\":\"Alt\",\"Art\":\"cli\",\"Ergebnis\":0,"
                  + "\"Meldung\":\"Verifiziert: 1 → 2\",\"VersionVorher\":\"1\",\"VersionNachher\":\"2\",\"AusstehendeVersion\":null,"
                  + "\"Befehl\":\"x update\",\"ExitCode\":0,\"Erhoeht\":true,\"ProtokollDatei\":\"x\",\"ErgebnisText\":\"Erfolgreich\","
                  + "\"Kurzfassung\":\"k\",\"IstFehler\":false}";
        File.AppendAllText(Protokollierung.VerlaufsDatei, alt + "\n{kaputt, keine JSON-Zeile\n"
            + "{\"ProgrammId\":\"" + id + "-neu\",\"Ergebnis\":5,\"Meldung\":\"m\"}\n");

        var berichte = Protokollierung.LetzteBerichte();

        Assert.Equal(LaufErgebnis.Erfolgreich, berichte[id].Ergebnis);
        Assert.Equal("2", berichte[id].VersionNachher);
        Assert.Equal(LaufErgebnis.BereitsAktuell, berichte[id + "-neu"].Ergebnis);
        Assert.Contains(Ereignisse.Alle(), e => e.Text("typ") == "verlauf.kaputte_zeilen");
    }

    private sealed class WerfenderProvider : IAktualisierer
    {
        public string Art => "fake";
        public Task<PruefErgebnis> PruefenAsync(ProgrammEintrag e, IProgress<string> p, CancellationToken a)
            => throw new InvalidOperationException("Provider kaputt, password=hunter2", new IOException("Innere Ursache"));
        public Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag e, IProgress<string> p, CancellationToken a) => throw new NotSupportedException();
        public Task<string> FingerabdruckAsync(ProgrammEintrag e, CancellationToken a) => Task.FromResult("");
    }
}

public sealed class DiagnoseSchreiberTests : IDisposable
{
    private readonly string _ordner = Path.Combine(Path.GetTempPath(), "uz-diag-" + Guid.NewGuid().ToString("N"));

    public DiagnoseSchreiberTests() => Directory.CreateDirectory(_ordner);

    public void Dispose()
    {
        TestOrdner.Loeschen(_ordner);
    }

    // 7
    [Fact]
    public void Parallele_Schreiber_erzeugen_nur_ganze_JSON_Zeilen()
    {
        var schreiber = new DiagnoseSchreiber(() => _ordner, @"Local\uz-test-" + Guid.NewGuid().ToString("N"));
        const int Faeden = 8, JeFaden = 400;
        var threads = Enumerable.Range(0, Faeden).Select(f => new Thread(() =>
        {
            for (var i = 0; i < JeFaden; i++)
                schreiber.Schreiben(Diagnose.Zeile(Schwere.Info, "t", "t", "Faden " + f + " Zeile " + i + " " + new string('ä', 300),
                    null, null, null, null, null));
        })).ToList();
        threads.ForEach(t => t.Start());
        threads.ForEach(t => t.Join());

        var zeilen = File.ReadAllLines(schreiber.Datei(DateTime.Now, 1), Encoding.UTF8);
        Assert.Equal(Faeden * JeFaden, zeilen.Length);
        foreach (var zeile in zeilen) JsonDocument.Parse(zeile).Dispose();   // throws on any torn line
    }

    // 8
    [Fact]
    public void Groessenrotation_mit_Tagesgrenze()
    {
        var schreiber = new DiagnoseSchreiber(() => _ordner, @"Local\uz-test-" + Guid.NewGuid().ToString("N"))
            { MaxDateiBytes = 2000, MaxTeileProTag = 3 };
        for (var i = 0; i < 60; i++) schreiber.Schreiben(Diagnose.Zeile(Schwere.Info, "t", "t", new string('x', 200), null, null, null, null, null));

        Assert.True(File.Exists(schreiber.Datei(DateTime.Now, 1)));
        Assert.True(File.Exists(schreiber.Datei(DateTime.Now, 2)));
        Assert.True(File.Exists(schreiber.Datei(DateTime.Now, 3)));
        Assert.False(File.Exists(schreiber.Datei(DateTime.Now, 4)));
        Assert.True(schreiber.Verworfen > 0);
        Assert.All(Directory.GetFiles(_ordner, "diagnose-*.jsonl"), d => Assert.True(new FileInfo(d).Length <= 2000 + 400));
    }

    [Fact]
    public void Aufbewahrung_loescht_nur_alte_inaktive_Dateien()
    {
        var jetzt = new DateTime(2026, 9, 22, 12, 0, 0);
        string Anlegen(string name, DateTime schreibzeit)
        {
            var d = Path.Combine(_ordner, name);
            File.WriteAllText(d, "x");
            File.SetLastWriteTime(d, schreibzeit);
            return d;
        }

        var alt = Anlegen("diagnose-2026-07-01.jsonl", jetzt.AddDays(-80));
        var altTeil = Anlegen("diagnose-2026-07-01.2.jsonl", jetzt.AddDays(-80));
        var altLog = Anlegen("updates-2026-07-01.log", jetzt.AddDays(-80));
        var heute = Anlegen("diagnose-2026-09-22.jsonl", jetzt);
        var altNameAberFrisch = Anlegen("updates-2026-07-02.log", jetzt.AddDays(-1));
        var verlauf = Anlegen("verlauf.jsonl", jetzt.AddDays(-400));
        var fremd = Anlegen("notiz.txt", jetzt.AddDays(-400));

        var geloescht = Diagnose.Aufraeumen(_ordner, jetzt);

        Assert.Equal(3, geloescht);
        Assert.False(File.Exists(alt));
        Assert.False(File.Exists(altTeil));
        Assert.False(File.Exists(altLog));
        Assert.True(File.Exists(heute));
        Assert.True(File.Exists(altNameAberFrisch));
        Assert.True(File.Exists(verlauf));
        Assert.True(File.Exists(fremd));
    }

    // 10
    [Fact]
    public void Nicht_beschreibbarer_Ordner_nutzt_begrenzten_Fallback()
    {
        var blocker = Path.Combine(_ordner, "ist-eine-datei");
        File.WriteAllText(blocker, "kein Ordner");
        var fallback = Path.Combine(_ordner, "fallback", "diag.jsonl");
        var schreiber = new DiagnoseSchreiber(() => blocker, @"Local\uz-test-" + Guid.NewGuid().ToString("N"))
            { FallbackDatei = fallback, MaxFallbackBytes = 3000 };

        for (var i = 0; i < 50; i++)
            schreiber.Schreiben(Diagnose.Zeile(Schwere.Fehler, "t", "t", "Zeile " + i + new string('z', 100), null, null, null, null, null));

        Assert.True(File.Exists(fallback));
        Assert.True(new FileInfo(fallback).Length <= 3000);
        Assert.True(schreiber.Verworfen > 0);
        Assert.NotNull(Diagnose.LetzteWarnung);
        foreach (var zeile in File.ReadAllLines(fallback)) JsonDocument.Parse(zeile).Dispose();
    }

    // 9
    [Fact]
    public void Diagnosepaket_enthaelt_das_Noetige_und_keine_Geheimnisse()
    {
        var logs = Path.Combine(_ordner, "logs");
        var ziel = Path.Combine(_ordner, "export");
        Directory.CreateDirectory(logs);
        File.WriteAllText(Path.Combine(logs, "diagnose-2026-09-22.jsonl"),
            "{\"meldung\":\"password=hunter2\"}\n{\"meldung\":\"Authorization: Bearer abcdefghijklmnop12345\"}\n", Encoding.UTF8);
        File.WriteAllText(Path.Combine(logs, "updates-2026-09-22.log"), "GITHUB_TOKEN=ghp_abcdefghijklmnopqrstuvwxyz0123456789\n", Encoding.UTF8);
        File.WriteAllText(Path.Combine(logs, "verlauf.jsonl"), "{\"ProgrammId\":\"x\",\"Meldung\":\"--api-key sk-abcdefghijklmnop1234\"}\n");
        // One oversized log: must be cut to the per-file limit with a visible marker.
        using (var gross = new StreamWriter(Path.Combine(logs, "diagnose-2026-09-21.jsonl")))
            for (var i = 0; i < 60_000; i++) gross.WriteLine("{\"i\":" + i + ",\"meldung\":\"" + new string('g', 80) + "\"}");
        File.WriteAllText(Path.Combine(logs, "settings.json"), "{\"geheim\":\"nie im Paket\"}");

        var katalog = new[] { new ProgrammEintrag { Id = "a", Name = "A", Art = "reposkript", SkriptArgumente = "--token abc123geheim" } };
        var pfad = DiagnoseExport.Erstellen(logs, ziel, new DateTime(2026, 9, 22, 13, 45, 7), katalog, Diagnose.Laufzeitinfo());

        Assert.Equal("UpdateZentrale-Diagnose-20260922-134507.zip", Path.GetFileName(pfad));
        Assert.True(new FileInfo(pfad).Length < DiagnoseExport.MaxGesamtBytes);
        using var zip = ZipFile.OpenRead(pfad);
        var namen = zip.Entries.Select(e => e.FullName).ToList();
        Assert.Contains("README.md", namen);
        Assert.Contains("metadaten.json", namen);
        Assert.Contains("katalog.json", namen);
        Assert.Contains("logs/diagnose-2026-09-22.jsonl", namen);
        Assert.Contains("logs/updates-2026-09-22.log", namen);
        Assert.Contains("logs/verlauf.jsonl", namen);
        Assert.DoesNotContain("logs/settings.json", namen);

        var alles = new StringBuilder();
        foreach (var eintrag in zip.Entries)
        {
            using var leser = new StreamReader(eintrag.Open(), Encoding.UTF8);
            var inhalt = leser.ReadToEnd();
            alles.Append(inhalt);
            if (eintrag.FullName == "logs/diagnose-2026-09-21.jsonl")
            {
                Assert.True(inhalt.Length <= DiagnoseExport.MaxDateiBytes + 200);
                Assert.Contains("gekürzt", inhalt);
            }
        }
        var text = alles.ToString();
        foreach (var geheim in new[] { "hunter2", "abcdefghijklmnop12345", "ghp_abcdefghijklmnopqrstuvwxyz0123456789", "sk-abcdefghijklmnop1234", "abc123geheim", "nie im Paket" })
            Assert.DoesNotContain(geheim, text);

        using var meta = JsonDocument.Parse(new StreamReader(zip.GetEntry("metadaten.json")!.Open()).ReadToEnd());
        foreach (var feld in new[] { "version", "pid", "architektur", "betriebssystem", "dotnet", "erhoeht", "sitzung", "erstellt", "schema" })
            Assert.True(meta.RootElement.TryGetProperty(feld, out _), "fehlt: " + feld);
    }
}

/// <summary>
/// Needs the process-wide writer or real processes, so it runs alone (see ProzessendeSammlung).
/// </summary>
[Collection("Prozessende")]
public sealed class DiagnoseProzessTests : IDisposable
{
    private readonly string _ordner = Path.Combine(Path.GetTempPath(), "uz-diagp-" + Guid.NewGuid().ToString("N"));
    private readonly string _kind;

    public DiagnoseProzessTests()
    {
        Directory.CreateDirectory(_ordner);
        _kind = Path.Combine(_ordner, "uzfix.exe");
        File.Copy(Path.Combine(Environment.SystemDirectory, "PING.EXE"), _kind);
    }

    public void Dispose()
    {
        var alle = Process.GetProcessesByName("uzfix");
        foreach (var p in alle)
        {
            if (Prozesspfad.GleicheDatei(Prozesspfad.Lesen(p.Id), _kind)) ProzessbaumTests.Aufraeumen(p.Id);
            p.Dispose();
        }
        TestOrdner.Loeschen(_ordner);
    }

    private static JsonElement BefehlEnde(string vorgangId)
        => Assert.Single(Ereignisse.Alle(), e => e.Text("vorgang") == vorgangId && e.Text("typ") == "befehl.ende");

    // 4
    [Fact]
    public async Task Befehle_werden_mit_allen_Flags_und_bereinigter_Ausgabe_erfasst()
    {
        async Task<(BefehlErgebnis lauf, JsonElement ende, JsonElement beginn)> Lauf(Func<Task<BefehlErgebnis>> aktion)
        {
            using var vorgang = Diagnose.VorgangBeginnen("test-befehl");
            var lauf = await aktion();
            var beginn = Assert.Single(Ereignisse.Alle(), e => e.Text("vorgang") == vorgang.Id && e.Text("typ") == "befehl.beginn");
            return (lauf, BefehlEnde(vorgang.Id), beginn);
        }

        var (_, ok, okBeginn) = await Lauf(() => Kommandozeile.AusfuehrenAsync("cmd.exe", "/c echo password=hunter2 ALLES_GUT", TimeSpan.FromSeconds(30)));
        Assert.Equal("cmd.exe", okBeginn.Daten("datei")!.Value.GetString());
        Assert.Equal(30, okBeginn.Daten("zeitlimitSek")!.Value.GetInt64());
        Assert.DoesNotContain("hunter2", okBeginn.Daten("argumente")!.Value.GetString());
        Assert.Equal(0, ok.Daten("exitCode")!.Value.GetInt32());
        Assert.Equal("info", ok.Text("schwere"));
        Assert.Contains("ALLES_GUT", ok.Daten("ausgabe")!.Value.GetString());
        Assert.DoesNotContain("hunter2", ok.Daten("ausgabe")!.Value.GetString());
        Assert.True(ok.TryGetProperty("dauerMs", out _));

        var (_, fehler, _) = await Lauf(() => Kommandozeile.AusfuehrenAsync("cmd.exe", "/c exit /b 7", TimeSpan.FromSeconds(30)));
        Assert.Equal(7, fehler.Daten("exitCode")!.Value.GetInt32());
        Assert.Equal("warnung", fehler.Text("schwere"));

        var (_, zeit, _) = await Lauf(() => Kommandozeile.AusfuehrenAsync("powershell.exe", "-NoProfile -Command \"Start-Sleep 60\"", TimeSpan.FromSeconds(2)));
        Assert.True(zeit.Daten("abgelaufen")!.Value.GetBoolean());
        Assert.Equal("fehler", zeit.Text("schwere"));

        using (var abbruch = new CancellationTokenSource(TimeSpan.FromSeconds(2)))
        {
            var (_, weg, _) = await Lauf(() => Kommandozeile.AusfuehrenAsync("powershell.exe", "-NoProfile -Command \"Start-Sleep 60\"",
                TimeSpan.FromMinutes(1), abbruch: abbruch.Token));
            Assert.True(weg.Daten("abgebrochen")!.Value.GetBoolean());
            Assert.Equal("abgebrochen", weg.Text("ergebnis"));
        }

        var (pipeLauf, pipe, _) = await Lauf(() => Kommandozeile.AusfuehrenAsync("powershell.exe",
            "-NoProfile -Command \"$p=[Diagnostics.ProcessStartInfo]::new('" + _kind + "','-n 60 127.0.0.1'); $p.UseShellExecute=$false; "
            + "$k=[Diagnostics.Process]::Start($p); Write-Output ('KIND=' + $k.Id)\"", TimeSpan.FromMinutes(1)));
        Assert.True(pipe.Daten("pipeGehalten")!.Value.GetBoolean());
        ProzessbaumTests.Aufraeumen(ProzessbaumTests.Wert(pipeLauf.Ausgabe, "KIND"));

        var (problemLauf, problem, _) = await Lauf(() => Kommandozeile.AusfuehrenInternAsync("powershell.exe",
            "-NoProfile -Command \"$k = Start-Process '" + _kind + "' -ArgumentList '-n 120 127.0.0.1' -WindowStyle Hidden -PassThru; "
            + "Write-Output ('KIND=' + $k.Id); Start-Sleep -Seconds 120\"", TimeSpan.FromSeconds(3),
            baumBeenden: p => p.Kill(), einzelnBeenden: _ => false));
        Assert.Contains("liefen nach dem Beenden weiter", problem.Daten("beendenProblem")!.Value.GetString());
        Assert.Equal("fehler", problem.Text("schwere"));
        ProzessbaumTests.Aufraeumen(ProzessbaumTests.Wert(problemLauf.Ausgabe, "KIND"));
    }

    // 10 (App/Update bleiben unberührt)
    [Fact]
    public async Task Kaputter_Diagnoseschreiber_bricht_weder_Pruefung_noch_Update()
    {
        var vorher = Diagnose.Schreiber;
        var blocker = Path.Combine(_ordner, "blockiert");
        File.WriteAllText(blocker, "x");
        Diagnose.Schreiber = new DiagnoseSchreiber(() => blocker, @"Local\uz-test-" + Guid.NewGuid().ToString("N"))
            { FallbackDatei = Path.Combine(_ordner, "fb", "f.jsonl") };
        try
        {
            var p = new SkriptProvider(new[] { "1.0", "1.1" }, new[] { new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, "1.0", "1.1"),
                new PruefErgebnis(UpdateZustand.Aktuell, "1.1") });
            var karte = new ProgrammViewModel(new ProgrammEintrag { Id = "fb-" + Guid.NewGuid().ToString("N")[..8], Name = "FB", Art = "fake" },
                p, new Einstellungen(), new Laufkoordination()) { KettenWarten = (_, _) => Task.CompletedTask };

            await karte.AktualisierenCommand.ExecuteAsync(null);
            Diagnose.Ausnahme(new Exception("egal"), "t", "t");

            Assert.Equal(UpdateZustand.Aktuell, karte.Zustand);
            Assert.Equal(LaufErgebnis.Erfolgreich, karte.LetzterBericht!.Ergebnis);
        }
        finally
        {
            Diagnose.Schreiber = vorher;
        }
    }
}
