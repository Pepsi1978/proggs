using System.IO;
using System.Net.Http;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Text;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace JarvisWindows;

/// <summary>Ein Eintrag des Katalogs. Der Steckbrief bleibt Text: So darf der Eintrag zwischen den Ausführungsfäden wandern.</summary>
public sealed class HelferEintrag
{
    public string Kennung { get; set; } = "";
    public string ManifestText { get; set; } = "{}";
    public string Zustand { get; set; } = "";
    public bool Aktiv { get; set; } = true;
    public double PruefungZeit { get; set; }
    public string PruefungErgebnis { get; set; } = "";
    public int Position { get; set; }
    /// <summary>Die zuletzt von Frank freigegebenen Rechte; nur die Oberfläche setzt das.</summary>
    public string Freigabe { get; set; } = "";
    /// <summary>Zustand vor dem Papierkorb.</summary>
    public string VorPapierkorb { get; set; } = "";

    [JsonIgnore] public string Id => Kennung;
    /// <summary>Gelesen wird oft (Suche, Liste, Rechte), geändert selten: einmal parsen, bis sich der Text ändert. Das Paar ist ein Objekt, damit andere Fäden nie halb Neues sehen.</summary>
    private Tuple<string, JSONObjekt>? gelesen;
    [JsonIgnore] public JSONObjekt Manifest
    {
        get
        {
            var text = ManifestText;
            var g = gelesen;
            if (g != null && ReferenceEquals(g.Item1, text)) return g.Item2;
            var neu = JSON.Objekt(text) ?? new JSONObjekt();
            gelesen = Tuple.Create(text, neu);
            return neu;
        }
    }
    public string Text(string k) => Manifest.Text(k) ?? "";
    public List<string> Liste(string k) => Manifest.Texte(k);
    [JsonIgnore] public JSONObjekt Rechte => Manifest.Obj("rechte") ?? new JSONObjekt();
    [JsonIgnore] public List<string> Konten => Rechte.Objekte("konten").Select(o => o.Text("id")).Where(i => i != null).Select(i => i!).ToList();
    [JsonIgnore] public string Name => Text("name").Length == 0 ? Kennung : Text("name");
    [JsonIgnore] public bool Nutzbar => Zustand == HelferKatalog.Bereit && Aktiv;
}

/// <summary>Die eigenen Helfer dieses Laptops: Katalog, Prüfung, Ausführung. Ablage: Helfer/katalog.json und je Helfer ein Ordner mit
/// aktueller Fassung (aktuell), letzter geprüfter Fassung (.vorher) und eigenem Speicher (speicher). Regeln: JarvisHelfer/README.md.
/// Windows-Programme laufen unter Franks Benutzer und sind nicht abgeschottet; Netz- und Dateigrenzen setzt hier das Review, nicht die Laufzeit.
/// Läuft auf dem Hauptfaden; Eintraege wird nur als Ganzes ersetzt, wenn Einträge dazukommen oder gehen.</summary>
public sealed class HelferKatalog : Beobachtbar
{
    public static HelferKatalog Shared { get; } = new();
    public const string Bereit = "bereit", InPruefung = "in_pruefung", Wartet = "wartet_auf_freigabe", Fehlerhaft = "fehlerhaft", Papierkorb = "papierkorb";
    /// <summary>Wert von `ort` im Steckbrief für Helfer dieses Laptops (Handy: handy, Mac: mac).</summary>
    public const string OrtHier = "windows";
    private const double FristSekunden = 600.0;
    /// <summary>So viele Byte Ergebnisdateien gehen höchstens mit durch den Kanal; Größeres bleibt am Laptop und wird nur genannt.
    /// Der Relay nimmt höchstens 1 MB je Nachricht (MAX_RUMPF), Base64 macht daraus ein Drittel mehr.</summary>
    private const int KanalDateien = 700_000;
    private const string OhneVollzugriff = "Der Vollzugriff ist am Laptop ausgeschaltet; Helfer laufen dort gerade nicht (Einstellungen → Laptop).";
    private static readonly Regex GesperrteWerkzeuge = new(@"^(programmier.*|helfer_.*|windows_helfer_.*|agent_starten|mail_senden|.*loeschen|.*einstellungen.*|windows_.*|openlauncher_sitzung|terminal_befehl|chatgpt_app|screenshot_ins_cli|updater_zentrale|cli_frage|handy_jarvis_.*)\z", RegexOptions.Compiled);

    private ObservableCollection<HelferEintrag> _eintraege = new();
    public ObservableCollection<HelferEintrag> Eintraege { get => _eintraege; private set => Setze(ref _eintraege, value); }
    private readonly HashSet<string> installiertGerade = new();
    private bool gestartet;

    private readonly string wurzel;
    private string KatalogDatei => Path.Combine(wurzel, "katalog.json");
    private string Eingang => Path.Combine(wurzel, "eingang");
    private string Ordner(string k) => Path.Combine(wurzel, k);

    private HelferKatalog()
    {
        wurzel = Path.Combine(Pfade.Unterstuetzung, "Helfer");
        Directory.CreateDirectory(wurzel);
        try
        {
            if (File.Exists(KatalogDatei) && System.Text.Json.JsonSerializer.Deserialize<List<HelferEintrag>>(File.ReadAllBytes(KatalogDatei)) is { } l)
                _eintraege = new ObservableCollection<HelferEintrag>(l);
        }
        catch (Exception ex) { Log("fehler", "Helfer-Katalog nicht gelesen", wo: "Eigene Tools", fehler: ex.Message); }
        // Mitten in der Prüfung beendet: Ohne bestandene Prüfung gilt der Helfer nicht als verfügbar.
        foreach (var e in _eintraege.Where(x => x.Zustand == InPruefung))
        {
            e.Zustand = Fehlerhaft;
            e.PruefungErgebnis = "Prüfung wurde unterbrochen.";
        }
    }

    public void Starten()
    {
        _ = Haupt.Auf(async () =>
        {
            if (gestartet) return;
            gestartet = true;
            Directory.CreateDirectory(Eingang);
            while (true)
            {
                try { await EingangPruefen(); }
                catch (Exception ex) { Log("fehler", "Helfer-Eingang nicht geprüft", wo: "Eigene Tools", fehler: ex.Message); }
                await Task.Delay(TimeSpan.FromSeconds(30));
            }
        });
    }

    private void Sichern()
    {
        try { GehirnHilfe.SchreibeAtomar(KatalogDatei, System.Text.Json.JsonSerializer.SerializeToUtf8Bytes(_eintraege.ToList())); }
        catch (Exception ex) { Log("fehler", "Helfer-Katalog nicht gesichert", wo: "Eigene Tools", fehler: ex.Message); }
        Melde(nameof(Eintraege));
        Melde(nameof(Sortiert));
        Melde(nameof(ImPapierkorb));
    }

    public List<HelferEintrag> Sortiert => Eintraege.Where(e => e.Zustand != Papierkorb).OrderBy(e => e.Position).ThenBy(e => e.Kennung, StringComparer.Ordinal).ToList();
    public List<HelferEintrag> ImPapierkorb => Eintraege.Where(e => e.Zustand == Papierkorb).ToList();
    public HelferEintrag? Finde(string k) => Eintraege.FirstOrDefault(e => e.Kennung == k);
    public List<string> Nutzer(string konto) => Eintraege.Where(e => e.Konten.Contains(konto)).Select(e => e.Name).ToList();
    public bool HatVorherigeFassung(string k) => File.Exists(Path.Combine(Ordner(k), ".vorher", "helfer.json"));

    private void Aendere(string k, Action<HelferEintrag> f)
    {
        var e = Finde(k);
        if (e == null) return;
        f(e);
        Sichern();
    }

    public List<JSONObjekt> KanalListe() => Sortiert.Select(KatalogZeile).ToList();

    private static JSONObjekt KatalogZeile(HelferEintrag e)
    {
        var m = e.Manifest;
        foreach (var weg in new[] { "tests", "datei", "rolle" }) m.Remove(weg);
        m["kennung"] = e.Kennung;
        m["ort"] = OrtHier;
        m["zustand"] = e.Zustand;
        m["aktiv"] = e.Aktiv;
        m["position"] = e.Position;
        m["pruefung"] = new JSONObjekt { ["zeit"] = e.PruefungZeit, ["ergebnis"] = e.PruefungErgebnis };
        return m;
    }

    // MARK: Suchen

    public List<HelferEintrag> Suche(string wort)
    {
        var woerter = Regex.Split(wort.ToLowerInvariant(), @"[^\p{L}\p{N}]+").Where(w => w.Length > 1).ToList();
        if (woerter.Count == 0) return Sortiert;
        int Punkte(HelferEintrag e)
        {
            var hoch = string.Join(" ", new[] { e.Kennung, e.Name }.Concat(e.Liste("schlagworte"))).ToLowerInvariant();
            var rest = string.Join(" ", new[] { e.Text("zweck") }.Concat(e.Liste("faehigkeiten"))).ToLowerInvariant();
            return woerter.Sum(w => (hoch.Contains(w) ? 3 : 0) + (rest.Contains(w) ? 1 : 0));
        }
        return Sortiert.Select(e => (e, p: Punkte(e))).Where(x => x.p > 0).OrderByDescending(x => x.p).Select(x => x.e).ToList();
    }

    // MARK: Verwalten

    /// <summary>Alles, was Modell und Handy dürfen. Freigeben und endgültig Löschen gibt es bewusst nur in der Oberfläche.</summary>
    public (bool ok, string text) Verwalten(string k, string aktion) => Haupt.Wert(() => VerwaltenLauf(k, aktion));

    private (bool ok, string text) VerwaltenLauf(string k, string aktion)
    {
        var e = Finde(k);
        if (e == null) return (false, $"Keinen Helfer „{k}“ gefunden.");
        switch (aktion)
        {
            case "aktivieren":
                Aendere(k, x => x.Aktiv = true);
                return (true, $"{e.Name} ist aktiv.");
            case "deaktivieren":
                Aendere(k, x => x.Aktiv = false);
                return (true, $"{e.Name} ist deaktiviert.");
            case "papierkorb":
                if (e.Zustand == Papierkorb) return (true, $"{e.Name} liegt schon im Papierkorb.");
                Aendere(k, x => { x.VorPapierkorb = x.Zustand; x.Zustand = Papierkorb; });
                return (true, $"{e.Name} liegt im Papierkorb. Konten und Schlüssel bleiben unberührt.");
            case "wiederherstellen":
                if (e.Zustand != Papierkorb) return (false, $"{e.Name} liegt nicht im Papierkorb.");
                Aendere(k, x => { x.Zustand = x.VorPapierkorb.Length == 0 ? Fehlerhaft : x.VorPapierkorb; x.VorPapierkorb = ""; });
                return (true, $"{e.Name} ist wiederhergestellt.");
            case "rueckrollen":
                return Rueckrollen(k);
            default:
                return (false, "Unbekannte aktion.");
        }
    }

    private static void OrdnerWeg(string pfad)
    {
        try { if (Directory.Exists(pfad)) Directory.Delete(pfad, true); } catch { }
    }

    private static void OrdnerKopie(string von, string nach)
    {
        Directory.CreateDirectory(nach);
        foreach (var d in Directory.GetFiles(von)) File.Copy(d, Path.Combine(nach, Path.GetFileName(d)), true);
        foreach (var u in Directory.GetDirectories(von)) OrdnerKopie(u, Path.Combine(nach, Path.GetFileName(u)));
    }

    private (bool ok, string text) Rueckrollen(string k)
    {
        var vorher = Path.Combine(Ordner(k), ".vorher");
        var aktuell = Path.Combine(Ordner(k), "aktuell");
        string text;
        try { text = File.ReadAllText(Path.Combine(vorher, "helfer.json"), new UTF8Encoding(false, true)); }
        catch { return (false, "Es gibt keine frühere geprüfte Fassung."); }
        OrdnerWeg(aktuell);
        try { OrdnerKopie(vorher, aktuell); }
        catch (Exception ex) { return (false, $"Zurücksetzen gescheitert: {ex.Message}"); }
        Aendere(k, e =>
        {
            e.ManifestText = text;
            e.Zustand = BrauchtFreigabe(e.Manifest) && e.Freigabe != Signatur(e.Manifest) ? Wartet : Bereit;
            e.PruefungZeit = GehirnHilfe.JetztSekunden();
            e.PruefungErgebnis = $"Auf die letzte geprüfte Fassung {e.Text("version")} zurückgesetzt.";
        });
        return (true, "Die letzte geprüfte Fassung gilt wieder.");
    }

    public void Freigeben(string k) => Haupt.Post(() =>
    {
        Aendere(k, e =>
        {
            e.Freigabe = Signatur(e.Manifest);
            if (e.Zustand == Wartet) e.Zustand = Bereit;
        });
        Log("werkzeug", $"Helfer {k}: Rechte von Frank freigegeben", wo: "Eigene Tools");
    });

    public void Verschieben(string k, int um) => Haupt.Post(() =>
    {
        var l = Sortiert.Select(e => e.Kennung).ToList();
        var i = l.IndexOf(k);
        if (i < 0 || i + um < 0 || i + um >= l.Count) return;
        (l[i], l[i + um]) = (l[i + um], l[i]);
        for (var n = 0; n < l.Count; n++) if (Finde(l[n]) is { } e) e.Position = n;
        Sichern();
    });

    public void EndgueltigLoeschen(string k) => Haupt.Post(() =>
    {
        if (Finde(k)?.Zustand != Papierkorb) return;
        OrdnerWeg(Ordner(k));
        Eintraege = new ObservableCollection<HelferEintrag>(Eintraege.Where(e => e.Kennung != k));
        Sichern();
        Log("werkzeug", $"Helfer {k} endgültig gelöscht", wo: "Eigene Tools");
    });

    // MARK: Installieren

    private static bool BrauchtFreigabe(JSONObjekt m)
    {
        var r = m.Obj("rechte") ?? new JSONObjekt();
        // Windows-Programme rufen keine Jarvis-Werkzeuge auf; erklärt einer trotzdem welche, entscheidet Frank.
        // Die Laufzeit setzt erklärte Hosts hier nicht durch: Wer ins Netz will, braucht deshalb immer Franks Ja.
        return (r.Liste("netz")?.Count ?? 0) > 0 || (r.Liste("konten")?.Count ?? 0) > 0 || (r.Liste("werkzeuge")?.Count ?? 0) > 0 || r.Wahr("extern") == true;
    }

    private static string Signatur(JSONObjekt m) => JSON.Text(m.Obj("rechte") ?? new JSONObjekt());

    private static string? PruefeSteckbrief(JSONObjekt m, Dictionary<string, string> dateien, JSONObjekt review)
    {
        var k = m.Text("kennung") ?? "";
        if (k.Length > 40 || !Regex.IsMatch(k, @"^[a-z0-9][a-z0-9-]*\z")) return "Die Kennung muss klein aus a-z, 0-9 und - bestehen, höchstens 40 Zeichen.";
        if (m.Text("ort") != OrtHier) return "Dieser Helfer ist nicht für diesen Laptop bestimmt (ort).";
        var art = m.Text("art") ?? "";
        if (art == "skript")
        {
            var d = m.Text("datei") ?? "";
            if ((d != "main.py" && d != "main.ps1") || !dateien.ContainsKey(d)) return "datei muss main.py oder main.ps1 sein und mitgeliefert werden.";
        }
        else if (art == "agent")
        {
            if ((m.Text("rolle") ?? "").Length == 0) return "Ein Agent braucht eine rolle.";
        }
        else return "art muss skript oder agent sein.";
        foreach (var name in dateien.Keys)
            if (name.StartsWith('/') || name.StartsWith('.') || name.Contains('\\') || name.Contains(':') || name.Split('/').Contains(".."))
                return $"Unzulässiger Dateiname {name}.";
        var tests = m.Objekte("tests");
        if (!tests.Any(t => t.Obj("erwartet")?.Wahr("ok") == true) || !tests.Any(t => t.Obj("erwartet")?.Wahr("ok") == false))
            return "Mindestens ein Normal- und ein Fehlerfall gehören in tests.";
        foreach (var w in m.Obj("rechte")?.Texte("werkzeuge") ?? new List<string>())
            if (GesperrteWerkzeuge.IsMatch(w)) return $"Das Werkzeug {w} ist für Helfer gesperrt.";
        if (review.Ganz("kritisch_offen") != 0 || (review.Text("zusammenfassung") ?? "").Length == 0)
            return "Ohne abgeschlossenes Review (kritisch_offen = 0, zusammenfassung) wird nichts registriert.";
        return null;
    }

    /// <summary>Prüft die neue Fassung auf diesem Laptop. Bestanden: Sie gilt, die bisherige wird zur letzten geprüften. Durchgefallen: Die bisherige bleibt.</summary>
    public Task<JSONObjekt> Installieren(JSONObjekt manifest, Dictionary<string, string> dateien, JSONObjekt review) => Haupt.Auf(() => InstallierenLauf(manifest, dateien, review));

    private async Task<JSONObjekt> InstallierenLauf(JSONObjekt m, Dictionary<string, string> dateien, JSONObjekt review)
    {
        JSONObjekt Nein(string t) => new() { ["ok"] = false, ["fehler"] = t };
        if (!Einstellungen.Shared.Vollzugriff) return Nein(OhneVollzugriff);
        if (PruefeSteckbrief(m, dateien, review) is { } f) return Nein(f);
        var k = m.Text("kennung") ?? "";
        if (!installiertGerade.Add(k)) return Nein("Dieser Helfer wird gerade schon geprüft.");
        try
        {
            var neu = Path.Combine(Ordner(k), "neu");
            var aktuell = Path.Combine(Ordner(k), "aktuell");
            var vorher = Path.Combine(Ordner(k), ".vorher");
            OrdnerWeg(neu);
            try
            {
                Directory.CreateDirectory(neu);
                foreach (var (name, inhalt) in dateien)
                {
                    var ziel = Path.Combine(neu, name.Replace('/', Path.DirectorySeparatorChar));
                    Directory.CreateDirectory(Path.GetDirectoryName(ziel)!);
                    GehirnHilfe.SchreibeAtomar(ziel, new UTF8Encoding(false).GetBytes(inhalt));
                }
                File.WriteAllBytes(Path.Combine(neu, "helfer.json"), new UTF8Encoding(false).GetBytes(JSON.Text(m, schoen: true)));
            }
            catch (Exception ex) { return Nein($"Paket nicht gespeichert: {ex.Message}"); }

            var manifestText = JSON.Text(m);
            var alt = Finde(k);
            // Im Papierkorb bleibt er dort: Eine neue Fassung wird nur vermerkt, zurückholen kann ihn allein Frank.
            var imKorb = alt?.Zustand == Papierkorb;
            var bewaehrt = alt != null && (imKorb ? alt.VorPapierkorb : alt.Zustand) is Bereit or Wartet;
            if (!bewaehrt && !imKorb)
            {
                if (alt == null)
                {
                    var position = Eintraege.Select(e => e.Position).DefaultIfEmpty(-1).Max() + 1;
                    Eintraege = new ObservableCollection<HelferEintrag>(Eintraege.Append(new HelferEintrag { Kennung = k, ManifestText = manifestText, Zustand = InPruefung, Position = position }));
                    Sichern();
                }
                else Aendere(k, e => { e.ManifestText = manifestText; e.Zustand = InPruefung; });
            }
            var fehlgeschlagen = await Testen(m, neu, k);
            var jetzt = GehirnHilfe.JetztSekunden();
            var version = m.Text("version") ?? "";
            if (fehlgeschlagen != null)
            {
                OrdnerWeg(neu);
                var ergebnisF = $"Prüfung von {version} durchgefallen: {fehlgeschlagen}";
                Aendere(k, e =>
                {
                    if (!bewaehrt && !imKorb) e.Zustand = Fehlerhaft;
                    e.PruefungZeit = jetzt;
                    e.PruefungErgebnis = bewaehrt ? ergebnisF + " Die bisherige Fassung gilt weiter." : ergebnisF;
                });
                Log("fehler", $"Helfer {k}: {ergebnisF}", wo: "Eigene Tools");
                return new JSONObjekt { ["ok"] = false, ["fehler"] = ergebnisF, ["zustand"] = Finde(k)?.Zustand ?? Fehlerhaft };
            }
            // Die alte Fassung wird erst aufgegeben, wenn die neue sicher liegt: Ein Virenscanner oder offener Ordner darf sie nie vernichten.
            JSONObjekt NichtUebernommen(string grund)
            {
                OrdnerWeg(neu);
                var text = $"Fassung {version} nicht übernommen: {grund}";
                Aendere(k, e =>
                {
                    if (!bewaehrt && !imKorb) e.Zustand = Fehlerhaft;
                    e.PruefungZeit = jetzt;
                    e.PruefungErgebnis = bewaehrt ? text + " Die bisherige Fassung gilt weiter." : text;
                });
                Log("fehler", $"Helfer {k}: {text}", wo: "Eigene Tools");
                return Nein(text);
            }
            var gesichert = false;
            if (bewaehrt && Directory.Exists(aktuell))
            {
                OrdnerWeg(vorher);
                try { Directory.Move(aktuell, vorher); gesichert = true; }
                catch (Exception ex) { return NichtUebernommen(ex.Message); }
            }
            else
            {
                OrdnerWeg(aktuell);
                if (Directory.Exists(aktuell)) return NichtUebernommen("Der alte Ordner ließ sich nicht entfernen.");
            }
            try { Directory.Move(neu, aktuell); }
            catch (Exception ex)
            {
                if (gesichert) try { Directory.Move(vorher, aktuell); } catch { }
                return NichtUebernommen(ex.Message);
            }
            var anzahl = m.Liste("tests")?.Count ?? 0;
            var ergebnis = $"{anzahl} Tests bestanden auf diesem Laptop; Review: {review.Ganz("befunde") ?? 0} Befunde, keine kritischen offen.";
            Aendere(k, e =>
            {
                e.ManifestText = manifestText;
                var neuerZustand = BrauchtFreigabe(m) && e.Freigabe != Signatur(m) ? Wartet : Bereit;
                if (imKorb) e.VorPapierkorb = neuerZustand; else e.Zustand = neuerZustand;
                e.PruefungZeit = jetzt;
                e.PruefungErgebnis = ergebnis;
            });
            var zustand = Finde(k)?.Zustand ?? Bereit;
            Log("werkzeug", $"Helfer {k} {version} registriert: {zustand}", wo: "Eigene Tools", ergebnis: ergebnis);
            return new JSONObjekt { ["ok"] = true, ["zustand"] = zustand, ["pruefung"] = new JSONObjekt { ["ergebnis"] = ergebnis, ["zeit"] = jetzt } };
        }
        finally { installiertGerade.Remove(k); }
    }

    /// <summary>null = alle Tests bestanden. Tests bekommen nie einen Schlüssel.</summary>
    private async Task<string?> Testen(JSONObjekt m, string neu, string kennung)
    {
        // Eigener Speicher- und Ausgabeordner: Eine durchgefallene Fassung darf die Daten der geltenden nicht anfassen.
        var probe = Path.Combine(Path.GetTempPath(), $"helfer-probe-{Guid.NewGuid()}");
        try
        {
            foreach (var t in m.Objekte("tests"))
            {
                var name = t.Text("name") ?? "Test";
                var erwartet = t.Obj("erwartet") ?? new JSONObjekt();
                var eingabe = t.Obj("eingabe") ?? new JSONObjekt();
                var r = await LaufStarten(m, neu, kennung, JSON.Text(eingabe), new Dictionary<string, string>(), 120, probe);
                var sollOk = erwartet.Wahr("ok") ?? true;
                if (r.Ok != sollOk) return $"„{name}“: {(r.Ok ? "lief durch, obwohl ein Fehler erwartet war" : $"Fehler: {r.Fehler.Kappe(300)}")}";
                var gesamt = r.Text + " " + r.DatenText + " " + r.Fehler;
                foreach (var teil in erwartet.Texte("enthaelt"))
                    if (!gesamt.Contains(teil)) return $"„{name}“: Ergebnis enthält „{teil}“ nicht.";
            }
            return null;
        }
        finally { OrdnerWeg(probe); }
    }

    public Task EingangPruefen() => Haupt.Auf(EingangPruefenLauf);

    private async Task EingangPruefenLauf()
    {
        // Pakete warten, bis der Vollzugriff wieder an ist; sie werden weder geprüft noch verworfen.
        if (!Einstellungen.Shared.Vollzugriff || !Directory.Exists(Eingang)) return;
        foreach (var paket in Directory.GetDirectories(Eingang))
        {
            var kennung = Path.GetFileName(paket);
            if (kennung.StartsWith('.')) continue;
            JSONObjekt? reg;
            try { reg = JSON.Objekt(File.ReadAllBytes(Path.Combine(paket, "registrierung.json"))); } catch { continue; }
            if (reg == null) continue;
            var ergebnis = new JSONObjekt { ["ok"] = false, ["fehler"] = "Unbekannter Bauschlüssel." };
            var schluessel = reg.Text("bau_schluessel") ?? "";
            if (HelferWerkzeuge.BauSchluesselPasst(schluessel, kennung, Finde(kennung) != null))
            {
                JSONObjekt? m = null;
                try { m = JSON.Objekt(File.ReadAllBytes(Path.Combine(paket, "helfer.json"))); } catch { }
                if (m != null && m.Text("kennung") == kennung)
                {
                    var dateien = new Dictionary<string, string>();
                    string? keinText = null;
                    foreach (var u in new DirectoryInfo(paket).EnumerateFiles("*", SearchOption.AllDirectories))
                    {
                        var rel = Path.GetRelativePath(paket, u.FullName).Replace('\\', '/');
                        // Wie skipsHiddenFiles am Mac: Punktdateien und versteckte Dateien gehören nicht ins Paket.
                        if (rel.Split('/').Any(t => t.StartsWith('.')) || u.Attributes.HasFlag(FileAttributes.Hidden)) continue;
                        if (rel is "helfer.json" or "registrierung.json") continue;
                        try { dateien[rel] = File.ReadAllText(u.FullName, new UTF8Encoding(false, true)); }
                        catch (Exception) { keinText = rel; }
                    }
                    if (keinText != null)
                    {
                        // Nie still auslassen: Der Helfer wäre sonst unvollständig installiert.
                        ergebnis = new JSONObjekt { ["ok"] = false, ["fehler"] = $"Die Datei {keinText} ist kein UTF-8-Text." };
                    }
                    else
                    {
                        ergebnis = await InstallierenLauf(m, dateien, reg.Obj("review") ?? new JSONObjekt());
                        if (ergebnis.Wahr("ok") == true) HelferWerkzeuge.BauSchluesselVerbrauchen(schluessel);
                    }
                }
                else ergebnis = new JSONObjekt { ["ok"] = false, ["fehler"] = "helfer.json fehlt, ist ungültig oder nennt nicht die Kennung des Ordners." };
            }
            OrdnerWeg(paket);
            var ok = ergebnis.Wahr("ok") == true;
            var text = ok ? $"Helfer {kennung}: {ergebnis.Text("zustand") ?? ""}" : $"Helfer {kennung} abgelehnt: {ergebnis.Text("fehler") ?? ""}";
            Log(ok ? "werkzeug" : "fehler", text, wo: "Eigene Tools");
            _ = Shell.Mitteilung("Eigene Tools", text.Kappe(180));
        }
    }

    // MARK: Ausführen

    public sealed class Lauf
    {
        public bool Ok { get; set; }
        public string Text { get; set; } = "";
        public string DatenText { get; set; } = "{}";
        public string Fehler { get; set; } = "";
        public List<(string name, string pfad)> Dateien { get; set; } = new();
        public int DauerMs { get; set; }
    }

    /// <summary>Ruft einen freigegebenen Helfer auf; Frank kann ihn deaktivieren, das Modell kann Rechte nicht erweitern.</summary>
    public Task<Lauf> Ausfuehren(string k, JSONObjekt eingabe) => Haupt.Auf(() => AusfuehrenLauf(k, eingabe));

    private async Task<Lauf> AusfuehrenLauf(string k, JSONObjekt eingabe)
    {
        if (!Einstellungen.Shared.Vollzugriff) return new Lauf { Fehler = OhneVollzugriff };
        var e = Finde(k);
        if (e == null) return new Lauf { Fehler = $"Keinen Helfer „{k}“ gefunden." };
        if (!e.Aktiv) return new Lauf { Fehler = $"{e.Name} ist deaktiviert." };
        if (e.Zustand != Bereit)
            return new Lauf { Fehler = e.Zustand == Wartet ? $"{e.Name} wartet auf Franks Freigabe in Eigene Tools." : $"{e.Name} ist nicht einsatzbereit ({e.Zustand})." };
        var konten = new Dictionary<string, string>();
        foreach (var id in e.Konten)
        {
            var wert = KontoWert(id);
            if (wert == null) return new Lauf { Fehler = $"Das Konto „{id}“ fehlt oder ist gesperrt. Es steht unter Konten und APIs." };
            konten["KONTO_" + id.ToUpperInvariant().Replace('-', '_')] = wert;
        }
        var beginn = DateTime.Now;
        var r = await LaufStarten(e.Manifest, Path.Combine(Ordner(k), "aktuell"), k, JSON.Text(eingabe), konten, FristSekunden);
        r.DauerMs = (int)(DateTime.Now - beginn).TotalMilliseconds;
        Log("werkzeug", $"Helfer {k}: {(r.Ok ? "ok" : "Fehler")}", wo: "Eigene Tools", werkzeug: k, fehler: r.Ok ? null : r.Fehler, dauerMs: r.DauerMs);
        return r;
    }

    private static string? KontoWert(string id)
    {
        var z = Einstellungen.Shared.Zugang;
        var stimmen = VorleseEinstellungen.Shared.Zugang;
        if (z.Weitere.TryGetValue(id, out var w)) return w.Gesperrt || w.Wert.Length == 0 ? null : w.Wert;
        if (z.GesperrtFuerHelfer.Contains(id)) return null;
        string wert;
        switch (id)
        {
            case "anthropic": wert = z.Anthropic; break;
            case "openai": wert = z.Openai; break;
            case "gemini": wert = z.Gemini; break;
            // Genau die Werte, die „Konten und APIs“ bearbeitet: Die Stimmen-Schlüssel liegen im eigenen Tresor-Eintrag.
            case "google": wert = stimmen.Google; break;
            case "gemini_tts_frei": wert = stimmen.GeminiFrei; break;
            case "gemini_tts_bezahlt": wert = stimmen.GeminiBezahlt; break;
            case "azure": wert = stimmen.Azure; break;
            case "qwen": wert = stimmen.Qwen; break;
            case "groq": wert = z.Groq; break;
            default: return null;
        }
        return wert.Length == 0 ? null : wert;
    }

    private async Task<Lauf> LaufStarten(JSONObjekt m, string ordner, string kennung, string eingabe, Dictionary<string, string> konten, double frist, string? basis = null)
    {
        if (m.Text("art") == "agent")
        {
            var erlaubt = (m.Obj("rechte")?.Texte("werkzeuge") ?? new List<string>()).Where(w => !GesperrteWerkzeuge.IsMatch(w)).ToHashSet();
            return await AgentLauf(m.Text("rolle") ?? "", kennung, eingabe, frist, erlaubt, (m.Obj("rechte")?.Liste("netz")?.Count ?? 0) > 0);
        }
        var datei = Path.Combine(ordner, m.Text("datei") ?? "main.ps1");
        var b = basis ?? Ordner(kennung);
        return await Task.Run(() => ProgrammLauf(datei, b, eingabe, konten, frist));
    }

    /// <summary>Nur die erklärten Werkzeuge: Was der Steckbrief nicht nennt, gibt es für diesen Lauf nicht, also auch keine verschachtelten Helfer.</summary>
    private async Task<Lauf> AgentLauf(string rolle, string name, string eingabe, double frist, HashSet<string> erlaubt, bool netz)
    {
        var g = GespraechsSpeicher.Shared.Neu();
        g.Titel = $"Helfer {name}";
        var auftrag = $"[EIGENES TOOL {name} – Lauf ohne Frank]\n{rolle}\n\nEingabe als JSON:\n{eingabe}\n\nAntworte nur mit dem Ergebnis. Fehlt etwas Nötiges, beginne mit FEHLER:";
        var abbruch = new CancellationTokenSource();
        var t = AgentenKanal.MitImLauf(() => JarvisAgent.Shared.Frage(auftrag, g, nurWerkzeuge: erlaubt, ct: abbruch.Token, websuche: netz));
        if (!await GehirnHilfe.Frist(frist, t))
        {
            abbruch.Cancel();
            GespraechsSpeicher.Shared.Loeschen(g);
            return new Lauf { Fehler = "Keine Antwort innerhalb der Frist." };
        }
        var antwort = Geheimnisse.Schwaerzen(await t).Trim();
        GespraechsSpeicher.Shared.Loeschen(g);
        if (antwort.StartsWith("FEHLER:", StringComparison.Ordinal) || antwort.Length == 0 || (antwort.Split("\n\n").LastOrDefault() ?? "").StartsWith("⚠️", StringComparison.Ordinal))
            return new Lauf { Fehler = antwort.Length == 0 ? "Keine Antwort." : antwort.Kappe(1000) };
        return new Lauf { Ok = true, Text = antwort };
    }

    private static readonly string[] ErlaubteUmgebung =
    {
        "SystemRoot", "windir", "SystemDrive", "ComSpec", "PATHEXT", "PATH", "TEMP", "TMP", "USERPROFILE", "HOMEDRIVE", "HOMEPATH", "USERNAME", "APPDATA", "LOCALAPPDATA",
        "ProgramData", "ProgramFiles", "ProgramFiles(x86)", "ProgramW6432", "CommonProgramFiles", "NUMBER_OF_PROCESSORS", "PROCESSOR_ARCHITECTURE", "OS",
    };

    private static string? ImPfad(string datei)
    {
        foreach (var o in (Environment.GetEnvironmentVariable("PATH") ?? "").Split(';', StringSplitOptions.RemoveEmptyEntries))
        {
            try
            {
                var p = Path.Combine(o.Trim(), datei);
                // Der Starter aus dem Store öffnet sonst nur die Store-Seite.
                if (File.Exists(p) && !o.Contains("WindowsApps", StringComparison.OrdinalIgnoreCase)) return p;
            }
            catch (ArgumentException) { }
        }
        return null;
    }

    private static (string exe, string[] vorweg) FindePython()
    {
        if (ImPfad("python.exe") is { } p) return (p, Array.Empty<string>());
        var lokal = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "Python");
        if (Directory.Exists(lokal))
        {
            var neu = Directory.GetDirectories(lokal, "Python3*").OrderByDescending(x => x, StringComparer.Ordinal)
                .Select(x => Path.Combine(x, "python.exe")).FirstOrDefault(File.Exists);
            if (neu != null) return (neu, Array.Empty<string>());
        }
        if (ImPfad("py.exe") is { } py) return (py, new[] { "-3" });
        return ("python.exe", Array.Empty<string>());
    }

    private static string FindePowerShell()
    {
        if (ImPfad("pwsh.exe") is { } p) return p;
        var fest = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PowerShell", "7", "pwsh.exe");
        return File.Exists(fest) ? fest : "powershell.exe";
    }

    /// <summary>Eigener Prozess statt Shell: nur die festgelegte Umgebung, Eingabe auf stdin, ohne Fenster.</summary>
    private static async Task<Lauf> ProgrammLauf(string datei, string basis, string eingabe, Dictionary<string, string> konten, double frist)
    {
        var kennung = Guid.NewGuid().ToString().ToUpperInvariant();
        var arbeit = Path.Combine(Path.GetTempPath(), $"helfer-{kennung}");
        var ausgabe = Path.Combine(basis, "ausgabe", kennung);
        var speicher = Path.Combine(basis, "speicher");
        foreach (var u in new[] { arbeit, ausgabe, speicher }) Directory.CreateDirectory(u);
        try
        {
            Aufraeumen(Path.Combine(basis, "ausgabe"));
            var psi = new ProcessStartInfo
            {
                UseShellExecute = false, CreateNoWindow = true, WorkingDirectory = arbeit,
                RedirectStandardInput = true, RedirectStandardOutput = true, RedirectStandardError = true,
            };
            if (Path.GetExtension(datei).Equals(".py", StringComparison.OrdinalIgnoreCase))
            {
                var (exe, vorweg) = FindePython();
                psi.FileName = exe;
                foreach (var v in vorweg) psi.ArgumentList.Add(v);
                psi.ArgumentList.Add(datei);
            }
            else
            {
                psi.FileName = FindePowerShell();
                foreach (var v in new[] { "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command" }) psi.ArgumentList.Add(v);
                // Ein- und Ausgabe in UTF-8, der Exit-Code des Skripts bleibt erhalten.
                psi.ArgumentList.Add("$u=[Text.UTF8Encoding]::new($false);[Console]::InputEncoding=$u;[Console]::OutputEncoding=$u;& '" + datei.Replace("'", "''") + "';exit $LASTEXITCODE");
            }
            var vorhanden = psi.Environment.ToDictionary(x => x.Key, x => x.Value, StringComparer.OrdinalIgnoreCase);
            psi.Environment.Clear();
            foreach (var k in ErlaubteUmgebung)
                if (vorhanden.TryGetValue(k, out var w) && w != null) psi.Environment[k] = w;
            psi.Environment["HOME"] = Pfade.Heim;
            psi.Environment["HELFER_AUSGABE"] = ausgabe;
            psi.Environment["HELFER_SPEICHER"] = speicher;
            psi.Environment["PYTHONUTF8"] = "1";
            psi.Environment["PYTHONIOENCODING"] = "utf-8";
            foreach (var (k, v) in konten) psi.Environment[k] = v;

            using var p = new Process { StartInfo = psi };
            try { p.Start(); }
            catch (Exception ex) { return new Lauf { Fehler = $"Der Helfer startet nicht: {ex.Message}" }; }

            var aus = new Sammler();
            var fehl = new Sammler();
            var leseAus = aus.Lies(p.StandardOutput.BaseStream);
            var leseFehl = fehl.Lies(p.StandardError.BaseStream);
            _ = Task.Run(async () =>
            {
                try
                {
                    await p.StandardInput.BaseStream.WriteAsync(new UTF8Encoding(false).GetBytes(eingabe));
                    await p.StandardInput.BaseStream.FlushAsync();
                    p.StandardInput.Close();
                }
                catch (Exception) { }
            });
            var abgebrochen = false;
            using var fertig = new CancellationTokenSource();
            var wachhund = Task.Run(async () =>
            {
                try { await Task.Delay(TimeSpan.FromSeconds(frist), fertig.Token); } catch (OperationCanceledException) { return; }
                try
                {
                    if (p.HasExited) return;
                    abgebrochen = true;
                    // Der ganze Baum: Sonst liefen vom Helfer gestartete Kindprozesse nach der Frist weiter.
                    p.Kill(entireProcessTree: true);
                }
                catch (Exception) { }
            });
            await p.WaitForExitAsync();
            fertig.Cancel();
            // Ein Enkelprozess im Hintergrund kann die Pipes offenhalten: Nach dem Ende des Helfers nur kurz auf den Rest warten.
            await GehirnHilfe.Frist(3, Task.WhenAll(leseAus, leseFehl));
            var stdout = Geheimnisse.Schwaerzen(aus.Text());
            var stderr = Geheimnisse.Schwaerzen(fehl.Text());
            var code = p.ExitCode;
            // NTSTATUS-Fehler (0xC0000000 und höher) entsprechen dem Signal am Mac: Absturz.
            var absturz = abgebrochen || (uint)code >= 0xC0000000;
            if (absturz || code != 0)
            {
                var grund = stderr.Trim();
                if (absturz) return new Lauf { Fehler = "Der Helfer wurde abgebrochen (Frist oder Absturz)." };
                if (grund.Length == 0) return new Lauf { Fehler = $"Exit-Code {code}." };
                if (grund.Length > 1500)
                {
                    grund = grund[^1500..];
                    if (char.IsLowSurrogate(grund[0])) grund = grund[1..];
                }
                return new Lauf { Fehler = grund };
            }
            var j = JSON.Objekt(stdout.Trim());
            if (j == null) return new Lauf { Fehler = "Der Helfer lieferte kein JSON." };
            var dateien = new List<(string name, string pfad)>();
            var ausgabeVoll = Path.GetFullPath(ausgabe) + Path.DirectorySeparatorChar;
            foreach (var d in j.Objekte("dateien"))
            {
                string name;
                string voll;
                try
                {
                    name = Path.GetFileName(d.Text("name") ?? "");
                    var roh = d.Text("pfad") ?? name;
                    voll = Path.GetFullPath(Path.IsPathRooted(roh) ? roh : Path.Combine(ausgabe, roh));
                }
                catch (Exception ex) when (ex is ArgumentException or NotSupportedException or PathTooLongException)
                {
                    return new Lauf { Fehler = "Die gemeldete Datei hat einen ungültigen Pfad." };
                }
                // Nur aus dem eigenen Ausgabeordner: Sonst gäbe ein Helfer beliebige Dateien des Laptops weiter.
                if (name.Length == 0 || !voll.StartsWith(ausgabeVoll, StringComparison.OrdinalIgnoreCase) || !File.Exists(voll))
                    return new Lauf { Fehler = $"Die gemeldete Datei „{name}“ liegt nicht im Ausgabeordner oder fehlt." };
                dateien.Add((name, voll));
            }
            return new Lauf { Ok = true, Text = j.Text("text") ?? "", DatenText = JSON.Text(j.Obj("daten") ?? new JSONObjekt()), Dateien = dateien };
        }
        catch (Exception ex)
        {
            return new Lauf { Fehler = $"Der Helfer ließ sich nicht vorbereiten: {ex.Message}" };
        }
        finally
        {
            OrdnerWeg(arbeit);
        }
    }

    /// <summary>Sammelt die Ausgabe eines Prozesses; `Text` geht auch, während noch gelesen wird.</summary>
    private sealed class Sammler
    {
        private readonly MemoryStream m = new();
        public async Task Lies(Stream s)
        {
            var puffer = new byte[1 << 14];
            try
            {
                while (true)
                {
                    var n = await s.ReadAsync(puffer);
                    if (n == 0) return;
                    lock (m) m.Write(puffer, 0, n);
                }
            }
            catch (Exception) { }
        }
        public string Text()
        {
            lock (m) return new UTF8Encoding(false).GetString(m.GetBuffer(), 0, (int)m.Length);
        }
    }

    /// <summary>Ergebnisdateien bleiben sieben Tage.</summary>
    private static void Aufraeumen(string ordner)
    {
        var grenze = DateTime.Now.AddDays(-7);
        try
        {
            foreach (var u in new DirectoryInfo(ordner).EnumerateDirectories())
                if (u.LastWriteTime < grenze) OrdnerWeg(u.FullName);
        }
        catch (Exception) { }
    }

    // MARK: Kanal zum Handy

    /// <summary>Das Handy schickt dem HP keine Helfer-Post und führt seine Helfer nicht. Sie wird beantwortet, aber nie ausgeführt: Sonst liefe fremder Code ohne Bauschlüssel und ohne Freigabe.</summary>
    public Task<string> KanalNachricht(string art, string text)
        => Task.FromResult(JSON.Text(new JSONObjekt { ["ok"] = false, ["fehler"] = $"{AppInfo.Name} führt über den Kanal keine Helfer-Aufträge aus ({art}). Schick den Auftrag als Nachricht mit art=frage." }));
}
