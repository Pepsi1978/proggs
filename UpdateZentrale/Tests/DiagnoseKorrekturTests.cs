using System.IO;
using System.IO.Compression;
using System.Text;
using System.Text.Json;
using UpdateZentrale.Models;
using UpdateZentrale.Services;
using UpdateZentrale.ViewModels;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>
/// R7 correction: K1 masking of every persisted field, K2 exact ZIP budget, K3 daily text
/// rotation, K4 honest fallback and warnings. Uses static seams, so it runs alone.
/// </summary>
[Collection("Prozessende")]
public sealed class DiagnoseKorrekturTests : IDisposable
{
    private readonly string _ordner = Path.Combine(Path.GetTempPath(), "uz-diag-" + Guid.NewGuid().ToString("N"));
    private readonly string _logVorher = Protokollierung.Ordner;

    public DiagnoseKorrekturTests() => Directory.CreateDirectory(_ordner);

    public void Dispose()
    {
        Protokollierung.Ordner = _logVorher;
        Protokollierung.MaxTextBytes = 10 * 1024 * 1024;
        Protokollierung.MaxTextTeile = 10;
        Protokollierung.KopfReserveBytes = 512 * 1024;
        TestOrdner.Loeschen(_ordner);
    }

    private static readonly string[] Geheimnisse =
    {
        "hunter2", "ghp_abcdefghijklmnopqrstuvwxyz0123456789", "sk-abcdefghijklmnop1234", "GeheimName77", "GeheimArt88"
    };

    private static string AlleTexte(string ordner)
    {
        var sb = new StringBuilder();
        foreach (var d in Directory.GetFiles(ordner, "*", SearchOption.AllDirectories))
            if (!d.EndsWith(".zip")) sb.Append(File.ReadAllText(d, Encoding.UTF8));
        return sb.ToString();
    }

    // K1
    [Fact]
    public async Task Geheimnisse_in_Kontext_Katalog_Fingerabdruck_und_Bericht_erreichen_keine_Datei()
    {
        var logs = Path.Combine(_ordner, "logs");
        Protokollierung.Ordner = logs;

        var eintrag = new ProgrammEintrag
        {
            Id = "id-token=GeheimName77", Name = "Name password=hunter2", Art = "api_key=GeheimArt88",
            Gruppe = "Gruppe GITHUB_TOKEN=ghp_abcdefghijklmnopqrstuvwxyz0123456789",
            Skript = "skript --token sk-abcdefghijklmnop1234", WingetId = "winget secret=hunter2", StoreProduktId = "store token=hunter2"
        };
        var provider = new SkriptProvider(new[] { "stand password=hunter2", "neu password=hunter2 2" },
            new[] { new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, "1", "2", "Meldung token=GeheimName77"),
                new PruefErgebnis(UpdateZustand.Aktuell, "2") });
        var karte = new ProgrammViewModel(eintrag, provider, new Einstellungen(), new Laufkoordination())
            { KettenWarten = (_, _) => Task.CompletedTask };

        await karte.AktualisierenCommand.ExecuteAsync(null);
        Diagnose.Ereignis(Schwere.Info, "komponente token=hunter2", "typ secret=GeheimName77", "m", "phase pwd=hunter2",
            new Dictionary<string, object?> { ["schlüssel password=hunter2"] = "x" });

        var zip = DiagnoseExport.Erstellen(logs, Path.Combine(_ordner, "export"), DateTime.Now, new[] { eintrag },
            new Dictionary<string, object?> { ["notiz"] = "Authorization: Bearer abcdefghijklmnop12345", ["version"] = "1" });

        var text = AlleTexte(logs);
        using (var archiv = ZipFile.OpenRead(zip))
            foreach (var e in archiv.Entries)
                using (var leser = new StreamReader(e.Open(), Encoding.UTF8)) text += leser.ReadToEnd();

        Assert.Contains("Update-Lauf:", text);   // the files were really written
        foreach (var geheim in Geheimnisse.Append("abcdefghijklmnop12345"))
            Assert.DoesNotContain(geheim, text);

        foreach (var datei in Directory.GetFiles(logs, "*.jsonl"))
            foreach (var zeile in File.ReadAllLines(datei).Where(z => z.Trim().Length > 0))
                JsonDocument.Parse(zeile).Dispose();
    }

    // K2
    [Fact]
    public void Zipgrenze_gilt_fuer_die_Summe_unkomprimierter_Eintraege()
    {
        var logs = Path.Combine(_ordner, "zlogs");
        Directory.CreateDirectory(logs);
        var zufall = new Random(7);
        for (var i = 0; i < 6; i++)
        {
            var bytes = new byte[300_000];
            zufall.NextBytes(bytes);
            File.WriteAllText(Path.Combine(logs, "diagnose-2026-09-2" + i + ".jsonl"), Convert.ToBase64String(bytes));
        }

        const long Grenze = 700_000;
        var pfad = DiagnoseExport.Erstellen(logs, Path.Combine(_ordner, "zexp"), DateTime.Now, Array.Empty<ProgrammEintrag>(),
            Diagnose.Laufzeitinfo(), maxDatei: 250_000, maxGesamt: Grenze);

        using var zip = ZipFile.OpenRead(pfad);
        Assert.True(zip.Entries.Sum(e => e.Length) <= Grenze, "Summe " + zip.Entries.Sum(e => e.Length));
        Assert.Contains(zip.Entries, e => e.FullName == "README.md");
        Assert.Contains(zip.Entries, e => e.FullName == "metadaten.json");
        Assert.Contains(zip.Entries, e => e.FullName == "katalog.json");
    }

    [Fact]
    public void Rest_unter_200_Bytes_wird_nie_ueberzogen_und_leer_gibt_keinen_Eintrag()
    {
        using var puffer = new MemoryStream();
        using var zip = new ZipArchive(puffer, ZipArchiveMode.Create, leaveOpen: true);
        long gesamt = 0;
        Assert.True(DiagnoseExport.Eintragen(zip, "a", new string('ä', 400), 150, ref gesamt));
        Assert.True(gesamt <= 150, "gesamt " + gesamt);
        Assert.False(DiagnoseExport.Eintragen(zip, "b", "noch mehr", 150, ref gesamt) && gesamt > 150);

        long voll = 150;
        Assert.False(DiagnoseExport.Eintragen(zip, "c", "x", 150, ref voll));
        Assert.Equal("aä", DiagnoseExport.Utf8Praefix("aää", 4));   // never half a character
    }

    [Fact]
    public void Fehler_beim_Export_hinterlaesst_keine_eigene_Tempdatei_und_schont_fremde()
    {
        var ziel = Path.Combine(_ordner, "fexp");
        Directory.CreateDirectory(ziel);
        var fremd = Path.Combine(ziel, "fremd.tmp");
        File.WriteAllText(fremd, "gehört jemand anderem");

        Assert.ThrowsAny<Exception>(() => DiagnoseExport.Erstellen(_ordner, ziel, DateTime.Now, WirftBeimLesen(),
            Diagnose.Laufzeitinfo()));

        Assert.True(File.Exists(fremd));
        Assert.Equal(new[] { "fremd.tmp" }, Directory.GetFiles(ziel).Select(Path.GetFileName));
    }

    private static IEnumerable<ProgrammEintrag> WirftBeimLesen()
    {
        yield return new ProgrammEintrag { Id = "x" };
        throw new IOException("absichtlicher Fehler im Katalog");
    }

    // K3
    [Fact]
    public void Tageslog_rotiert_Bericht_zeigt_auf_den_Abschluss_Budget_meldet_sich()
    {
        var logs = Path.Combine(_ordner, "tlogs");
        Protokollierung.Ordner = logs;
        Protokollierung.MaxTextBytes = 3000;
        Protokollierung.MaxTextTeile = 3;
        Protokollierung.KopfReserveBytes = 600;
        Diagnose.WarnungZuruecksetzen();

        for (var i = 0; i < 8; i++) Protokollierung.Schreiben("p", "Zeile " + i + " " + new string('x', 400));
        Assert.True(File.Exists(Protokollierung.TagesTeil(DateTime.Now, 2)));

        var bericht = new UpdateBericht { ProgrammId = "p", Name = "P", Meldung = "fertig" };
        Protokollierung.LaufBeginnen(new ProgrammEintrag { Id = "p", Name = "P", Art = "fake" }, "befehl", "1");
        Protokollierung.LaufBeenden(bericht);
        Assert.True(File.Exists(bericht.ProtokollDatei));
        Assert.Contains("Ergebnis:", File.ReadAllText(bericht.ProtokollDatei));

        var vorher = Protokollierung.Verworfen;
        for (var i = 0; i < 40; i++) Protokollierung.Schreiben("p", new string('y', 500));
        Assert.False(File.Exists(Protokollierung.TagesTeil(DateTime.Now, 4)));
        Assert.True(Protokollierung.Verworfen > vorher);
        Assert.Contains("Tageslimit", Diagnose.LetzteWarnung);
        Assert.All(Directory.GetFiles(logs, "updates-*.log"), d => Assert.True(new FileInfo(d).Length <= 3000));

        // Retention and export also cover the parts.
        var altTeil = Path.Combine(logs, "updates-2026-07-01.2.log");
        File.WriteAllText(altTeil, "alt");
        File.SetLastWriteTime(altTeil, DateTime.Now.AddDays(-80));
        Diagnose.Aufraeumen(logs, DateTime.Now);
        Assert.False(File.Exists(altTeil));

        var zip = DiagnoseExport.Erstellen(logs, Path.Combine(_ordner, "texp"), DateTime.Now, Array.Empty<ProgrammEintrag>(), Diagnose.Laufzeitinfo());
        using var archiv = ZipFile.OpenRead(zip);
        Assert.Contains(archiv.Entries, e => e.FullName == "logs/" + Path.GetFileName(Protokollierung.TagesTeil(DateTime.Now, 2)));
        Diagnose.WarnungZuruecksetzen();
    }

    // K4
    [Fact]
    public void Nicht_erhaltener_Mutex_schreibt_nie_ungesichert_in_die_Primaerdatei()
    {
        var name = @"Local\uz-test-" + Guid.NewGuid().ToString("N");
        using var gehalten = new ManualResetEventSlim();
        using var loslassen = new ManualResetEventSlim();
        var halter = new Thread(() =>
        {
            using var m = new Mutex(true, name);
            gehalten.Set();
            loslassen.Wait();
            m.ReleaseMutex();
        }) { IsBackground = true };
        halter.Start();
        gehalten.Wait();
        Diagnose.WarnungZuruecksetzen();
        try
        {
            var fallback = Path.Combine(_ordner, "mfb", "f.jsonl");
            var schreiber = new DiagnoseSchreiber(() => _ordner, name) { MutexWartezeit = TimeSpan.FromMilliseconds(200), FallbackDatei = fallback };
            schreiber.Schreiben(Diagnose.Zeile(Schwere.Info, "t", "t", "gesperrt", null, null, null, null, null));

            Assert.False(File.Exists(schreiber.Datei(DateTime.Now, 1)));
            Assert.Contains("gesperrt", File.ReadAllText(fallback));
            Assert.Contains("Mutex", Diagnose.LetzteWarnung);
        }
        finally
        {
            loslassen.Set();
            halter.Join();
            Diagnose.WarnungZuruecksetzen();
        }
    }

    [Fact]
    public void Ausgeschoepftes_Tagesbudget_loest_die_Warnung_aus()
    {
        Diagnose.WarnungZuruecksetzen();
        var schreiber = new DiagnoseSchreiber(() => _ordner, @"Local\uz-test-" + Guid.NewGuid().ToString("N"))
            { MaxDateiBytes = 500, MaxTeileProTag = 1 };
        for (var i = 0; i < 10; i++) schreiber.Schreiben(Diagnose.Zeile(Schwere.Info, "t", "t", new string('b', 200), null, null, null, null, null));

        Assert.True(schreiber.Verworfen > 0);
        Assert.Contains("Tageslimit", Diagnose.LetzteWarnung);
        Diagnose.WarnungZuruecksetzen();
    }

    [Fact]
    public void Fruehere_Warnung_wird_beim_Abonnieren_sofort_uebernommen()
    {
        Diagnose.WarnungZuruecksetzen();
        Diagnose.WarnungMelden("vor dem Fenster entstanden");
        string? angezeigt = null;
        void Ziel(string w) => angezeigt = w;
        try
        {
            Diagnose.WarnungAbonnieren(Ziel);
            Assert.Equal("vor dem Fenster entstanden", angezeigt);
        }
        finally
        {
            Diagnose.Warnung -= Ziel;
            Diagnose.WarnungZuruecksetzen();
        }
    }
}
