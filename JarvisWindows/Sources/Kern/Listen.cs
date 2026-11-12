using System.Globalization;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using Microsoft.Win32;

namespace JarvisWindows;

/// <summary>Eine Liste wie am Handy (listen/Listen.kt): Name, Art, Farbe und eigene Ansicht. Liest sich aus dem rohen JSON der Datei,
/// damit Felder einer neueren Handy-Fassung beim Zurückschreiben erhalten bleiben.</summary>
public sealed class Liste
{
    public JSONObjekt Roh { get; }
    public Liste(JSONObjekt roh) { Roh = roh; }
    public long Id => Roh.Ganz("id") ?? 0;
    public string Name => Roh.Text("name") ?? "";
    public string Art => Roh.Text("art") ?? "sonstiges";
    public int Farbe => (int)(Roh.Ganz("farbe") ?? 0);
    public string Sortierung => Roh.Text("sortierung") ?? "manuell";
    public string Gruppierung => Roh.Text("gruppierung") ?? "keine";
    public bool ErledigteZeigen => Roh.Wahr("erledigte_zeigen") ?? true;
    public double Pos => Roh.Zahl("pos") ?? 0;
    public long Geaendert => Roh.Ganz("geaendert") ?? 0;
    public bool Geloescht => Roh.Wahr("geloescht") ?? false;
    public List<string> Abschnitte => Roh.Texte("abschnitte");
}

/// <summary>Ein Eintrag; Eltern = 0 heißt oberste Ebene. Priorität 0 keine, 1 niedrig, 2 mittel, 3 hoch.</summary>
public sealed class ListenEintrag
{
    public JSONObjekt Roh { get; }
    public ListenEintrag(JSONObjekt roh) { Roh = roh; }
    public long Id => Roh.Ganz("id") ?? 0;
    public long ListeId => Roh.Ganz("liste") ?? 0;
    public string Titel => Roh.Text("titel") ?? "";
    public long Eltern => Roh.Ganz("eltern") ?? 0;
    public string Notiz => Roh.Text("notiz") ?? "";
    public int Prioritaet => (int)(Roh.Ganz("prioritaet") ?? 0);
    public string Faellig => Roh.Text("faellig") ?? "";
    public List<string> Tags => Roh.Texte("tags");
    public bool Erledigt => Roh.Wahr("erledigt") ?? false;
    public long ErledigtAm => Roh.Ganz("erledigt_am") ?? 0;
    public double Pos => Roh.Zahl("pos") ?? 0;
    public long Erstellt => Roh.Ganz("erstellt") ?? 0;
    public long Geaendert => Roh.Ganz("geaendert") ?? 0;
    public bool Geloescht => Roh.Wahr("geloescht") ?? false;
    public bool Angeheftet => Roh.Wahr("angeheftet") ?? false;
    public string Abschnitt => Roh.Text("abschnitt") ?? "";
}

/// <summary>Eine Zeile der Ansicht: Gruppenkopf oder Eintrag mit Einrücktiefe und Fortschritt seiner Unterpunkte.</summary>
public sealed record ListenZeile(ListenEintrag? Eintrag = null, int Tiefe = 0, int KinderErledigt = 0, int KinderGesamt = 0, string? Kopf = null, int KopfAnzahl = 0);

public static class ListenArten
{
    public static readonly (string Kennung, string Anzeige, string Zeichen)[] Alle =
    {
        ("einkauf", "Einkauf", "🛒"), ("aufgaben", "Aufgaben", "✅"), ("checkliste", "Checkliste", "📋"), ("packliste", "Packliste", "🎒"), ("ideen", "Ideen", "💡"), ("sonstiges", "Sonstiges", "📝"),
    };
    public static string Zeichen(string art) => Alle.FirstOrDefault(a => a.Kennung == art).Zeichen ?? "📝";
    public static readonly (string Kennung, string Anzeige)[] Sortierungen =
    {
        ("manuell", "Eigene Reihenfolge"), ("faellig", "Fälligkeit"), ("prioritaet", "Priorität"), ("titel", "Titel A–Z"), ("tag", "Tag"), ("erstellt", "Neueste zuerst"), ("aelteste", "Älteste zuerst"), ("geaendert", "Zuletzt geändert"),
    };
    public static readonly (string Kennung, string Anzeige)[] Gruppierungen =
    {
        ("keine", "Keine"), ("abschnitt", "Abschnitte"), ("prioritaet", "Priorität"), ("faellig", "Fälligkeit"), ("tag", "Tag"), ("erstellt", "Erstellt am"),
    };
    public static readonly string[] Prioritaeten = { "keine", "niedrig", "mittel", "hoch" };
    public static string PrioName(int p) => p is >= 0 and <= 3 ? Prioritaeten[p] : "keine";

    /// <summary>Schnelleingabe wie am Handy: „Milch !hoch #kühl“ → Titel „Milch“, Priorität hoch, Tag „kühl“.</summary>
    public static (string Titel, int Prio, List<string> Tags) Schnell(string text)
    {
        var prio = 0;
        var tags = new List<string>();
        var rest = new List<string>();
        foreach (var w in Regex.Split(text.Trim(), @"\s+"))
        {
            if (w.Length > 1 && w.StartsWith('!'))
            {
                var p = w[1..].ToLowerInvariant();
                int? wert = int.TryParse(p, out var z) && z is >= 0 and <= 3 ? z : p switch
                {
                    "keine" or "ohne" => 0, "niedrig" or "gering" or "low" => 1, "mittel" or "normal" or "medium" => 2, "hoch" or "wichtig" or "dringend" or "high" => 3, _ => null,
                };
                if (wert is { } x) { prio = x; continue; }
            }
            else if (w.Length > 1 && w.StartsWith('#')) { tags.Add(w[1..]); continue; }
            rest.Add(w);
        }
        var titel = string.Join(" ", rest).Trim();
        return (titel.Length == 0 ? text.Trim() : titel, prio, tags);
    }

    /// <summary>Fälligkeit kurz: heute, morgen, gestern oder „12.11.“.</summary>
    public static string DatumKurz(string iso)
    {
        if (!DateTime.TryParseExact(iso, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var d)) return iso;
        var heute = DateTime.Today;
        if (d == heute) return "heute";
        if (d == heute.AddDays(1)) return "morgen";
        if (d == heute.AddDays(-1)) return "gestern";
        return $"{d.Day}.{d.Month}.";
    }
}

/// <summary>Franks Listen am Laptop, Eins-zu-eins mit Handy und Tablet: je Liste und Eintrag eine JSON-Datei `listen/l_&lt;id&gt;.json` bzw.
/// `listen/e_&lt;id&gt;.json`, abgeglichen über den Geräteabgleich-Speicher des Servers (`/j/&lt;Geheimnis&gt;/abgleich`, Art „gedaechtnis“) wie
/// `GedaechtnisAbgleich.kt`. Der Laptop hält eine Kopie unter %APPDATA%\JarvisWindows\Listen; Lesen geht ohne Netz, Änderungen gehen sofort hoch
/// und bei Netzausfall später. Gelöscht wird nur als Grabstein in der Datei (geloescht = true), nie die Datei oder das Dokument.
/// Regeln wie am Handy: Haben beide Seiten geändert, gewinnt die jüngere Änderung; was der Server ablehnt (jüngere Fassung drüben), wird ersetzt.
/// Läuft auf dem Hauptfaden; Netz und JSON-Lesen großer Seiten abseits.</summary>
public sealed class ListenSpeicher : Beobachtbar
{
    public static ListenSpeicher Shared { get; } = new();
    public const string Ordner = "listen";
    private const int MaxTiefe = 4;

    private readonly Dictionary<long, Liste> listen = new();
    private readonly Dictionary<long, ListenEintrag> eintraege = new();
    /// <summary>Was der Server je Pfad zuletzt meldete: Prüfsumme und Zeit der Fassung.</summary>
    private readonly Dictionary<string, (string Sha, long Geaendert)> fern = new();
    /// <summary>Hier geändert und noch nicht vom Server angenommen.</summary>
    private readonly HashSet<string> offen = new();
    private long stand;
    private string instanz = "";
    private bool laeuft, hochladenLaeuft, speicherPlan;
    private CancellationTokenSource? abruf;
    private long letzteId;

    private int _aenderung;
    /// <summary>Zählt jede Änderung des Bestands (Oberfläche baut neu).</summary>
    public int Aenderung { get => _aenderung; private set => Setze(ref _aenderung, value); }
    private string? _fehler;
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }
    private bool _bereit;
    /// <summary>Der erste Abgleich mit dem Server ist durch.</summary>
    public bool Bereit { get => _bereit; private set => Setze(ref _bereit, value); }
    private int _ausstehend;
    /// <summary>Änderungen vom Laptop, die noch nicht beim Server sind.</summary>
    public int Ausstehend { get => _ausstehend; private set => Setze(ref _ausstehend, value); }
    private DateTime? _zuletzt;
    public DateTime? Zuletzt { get => _zuletzt; private set => Setze(ref _zuletzt, value); }

    private readonly string wurzel = Path.Combine(Pfade.Unterstuetzung, "Listen");
    private string Dateien => Path.Combine(wurzel, Ordner);
    private string ZustandDatei => Path.Combine(wurzel, "zustand.json");

    private ListenSpeicher() { Directory.CreateDirectory(Dateien); }

    private static string? Basis
    {
        get
        {
            var a = Einstellungen.Shared.Zugang.Relay.Trim();
            if (!a.EndsWith("/mcp", StringComparison.Ordinal)) return null;
            return a[..^4] + "/abgleich";
        }
    }

    // MARK: Lesen

    public IReadOnlyList<Liste> Alle => listen.Values.Where(l => !l.Geloescht).OrderBy(l => l.Pos).ThenBy(l => l.Id).ToList();
    public Liste? ListeZu(long id) => listen.TryGetValue(id, out var l) && !l.Geloescht ? l : null;
    public List<ListenEintrag> EintraegeVon(long liste) => eintraege.Values.Where(e => e.ListeId == liste && !e.Geloescht).ToList();
    public int Offen(long liste) => eintraege.Values.Count(e => e.ListeId == liste && !e.Geloescht && !e.Erledigt);
    public int OffenGesamt => Alle.Sum(l => Offen(l.Id));

    // MARK: Hochfahren und Abgleich

    public void Hochfahren()
    {
        _ = Haupt.Auf(async () =>
        {
            if (laeuft) return;
            laeuft = true;
            await LadeLokal();
            SystemEvents.PowerModeChanged += (_, e) => { if (e.Mode == PowerModes.Resume) Haupt.Post(() => abruf?.Cancel()); };
            await Schleife();
        });
    }

    private async Task LadeLokal()
    {
        var (z, dateien) = await Task.Run(() =>
        {
            JSONObjekt? zust = null;
            try { zust = JSON.Objekt(File.ReadAllBytes(ZustandDatei)); } catch { }
            var liste = new List<JSONObjekt>();
            foreach (var f in Directory.EnumerateFiles(Dateien, "*.json"))
                try { if (JSON.Objekt(File.ReadAllBytes(f)) is { } o) liste.Add(o); } catch { }
            return (zust, liste);
        });
        if (z != null)
        {
            stand = z.Ganz("stand") ?? 0;
            instanz = z.Text("instanz") ?? "";
            if (z.Obj("fern") is { } f)
                foreach (var (pfad, wert) in f)
                    if (wert is JSONObjekt w) fern[pfad] = (w.Text("sha") ?? "", w.Ganz("geaendert") ?? 0);
            foreach (var p in z.Texte("offen")) offen.Add(p);
            Bereit = stand > 0;
        }
        foreach (var o in dateien) Uebernimm(o);
        Ausstehend = offen.Count;
        Aenderung++;
    }

    /// <summary>Ein Objekt in den Bestand: Liste, wenn es kein Feld „liste“ hat, sonst Eintrag.</summary>
    private void Uebernimm(JSONObjekt o)
    {
        if (o.Ganz("id") is not { } id) return;
        if (o.ContainsKey("liste")) eintraege[id] = new ListenEintrag(o); else listen[id] = new Liste(o);
    }

    private async Task Schleife()
    {
        var fehlerFolge = 0;
        double[] pausen = { 0, 2, 5, 15, 30 };
        while (true)
        {
            var runde = new CancellationTokenSource();
            abruf = runde;
            var beginn = DateTime.Now;
            try
            {
                if (offen.Count > 0) await Hochladen();
                var n = await Holen(25, runde.Token);
                fehlerFolge = 0;
                Fehler = null;
                Zuletzt = DateTime.Now;
                if (n == 0 && (DateTime.Now - beginn).TotalSeconds < 1) await Task.Delay(2000);
            }
            catch (OperationCanceledException) when (runde.IsCancellationRequested) { }
            catch (Exception ex)
            {
                fehlerFolge++;
                if (fehlerFolge >= 2)
                {
                    Fehler = Basis == null ? "Keine Relay-Adresse eingetragen; die Listen zeigen den letzten Stand." : $"Server nicht erreichbar, du siehst den letzten Stand: {ex.Message}";
                    if (fehlerFolge == 2) Log("listen", "Listen-Abgleich unterbrochen", wo: "Listen", fehler: ex.Message);
                }
                await Task.Delay(TimeSpan.FromSeconds(pausen[Math.Min(fehlerFolge, pausen.Length - 1)]));
            }
        }
    }

    /// <summary>Sofort abgleichen (Knopf „Neu laden“).</summary>
    public void JetztAbgleichen() => Haupt.Post(() => abruf?.Cancel());

    private static HttpRequestMessage Anfrage(HttpMethod m, string url)
    {
        var r = new HttpRequestMessage(m, url);
        r.Headers.TryAddWithoutValidation("User-Agent", $"JarvisWindows/{AppInfo.Version}");
        return r;
    }

    private async Task<int> Holen(int warten, CancellationToken ct)
    {
        var basis = Basis ?? throw new IOException("Keine Relay-Adresse eingetragen.");
        var gesamt = 0;
        for (var runden = 0; runden < 200; runden++)
        {
            using var req = Anfrage(HttpMethod.Get, $"{basis}/aenderungen?seit={stand}&warten={(runden == 0 ? warten : 0)}&geraet={Uri.EscapeDataString(AppInfo.Name)}");
            var (code, daten) = await GehirnNetz.Anfrage(req, warten + 30, ct);
            if (code != 200) throw new IOException($"Der Server antwortet mit {code}.");
            var o = await Task.Run(() => JSON.Objekt(daten)) ?? throw new IOException("Unlesbare Antwort vom Server.");
            var i = o.Text("instanz") ?? "";
            // Neu aufgesetzter Speicher: von vorn lesen; was hier geändert ist, geht danach wieder hoch.
            if ((o.Wahr("neuaufbau") == true || (instanz.Length > 0 && i.Length > 0 && i != instanz)) && stand != 0)
            {
                Log("listen", "Abgleich-Speicher neu aufgesetzt, lese von vorn", wo: "Listen");
                instanz = i;
                stand = 0;
                fern.Clear();
                foreach (var p in AlleLokalenPfade()) offen.Add(p);
                continue;
            }
            if (i.Length > 0) instanz = i;
            var neu = new List<(string Pfad, string Sha, long Geaendert)>();
            foreach (var d in o.Objekte("docs"))
            {
                if (d.Text("art") != "gedaechtnis" || d.Wahr("geloescht") == true) continue;
                if (d.Obj("daten") is not { } w || w.Text("pfad") is not { } pfad || !PfadOk(pfad)) continue;
                var sha = w.Text("text_sha") ?? "";
                var geaendert = d.Ganz("geaendert") ?? 0;
                if (sha.Length == 0) continue;
                fern[pfad] = (sha, geaendert);
                neu.Add((pfad, sha, geaendert));
            }
            foreach (var (pfad, sha, geaendert) in neu)
            {
                var lokal = LokalSha(pfad);
                if (lokal == sha) { offen.Remove(pfad); continue; }
                // Hier geändert und noch nicht oben: Die jüngere Änderung gewinnt.
                if (offen.Contains(pfad) && LokalGeaendert(pfad) > geaendert) continue;
                if (await HoleDatei(basis, pfad, sha)) { offen.Remove(pfad); gesamt++; }
            }
            var weiter = o.Ganz("stand") ?? stand;
            var mehr = o.Wahr("mehr") == true && weiter != stand;
            stand = weiter;
            if (!mehr) break;
        }
        if (!Bereit) Bereit = true;
        Ausstehend = offen.Count;
        ZustandSpeichern();
        if (gesamt > 0) { Aenderung++; Log("listen", $"{gesamt} Listen-Änderung(en) vom Handy", wo: "Listen"); }
        return gesamt;
    }

    private static bool PfadOk(string pfad) => Regex.IsMatch(pfad, @"^listen/[le]_\d{1,20}\.json$");

    private IEnumerable<string> AlleLokalenPfade() =>
        listen.Keys.Select(id => $"{Ordner}/l_{id}.json").Concat(eintraege.Keys.Select(id => $"{Ordner}/e_{id}.json")).ToList();

    private string LokalDatei(string pfad) => Path.Combine(wurzel, pfad.Replace('/', Path.DirectorySeparatorChar));

    private string LokalSha(string pfad)
    {
        try { return Convert.ToHexStringLower(SHA256.HashData(File.ReadAllBytes(LokalDatei(pfad)))); } catch { return ""; }
    }

    private long LokalGeaendert(string pfad)
    {
        var id = long.Parse(Regex.Match(pfad, @"\d+").Value);
        return pfad.Contains("/l_") ? (listen.TryGetValue(id, out var l) ? l.Geaendert : 0) : (eintraege.TryGetValue(id, out var e) ? e.Geaendert : 0);
    }

    private async Task<bool> HoleDatei(string basis, string pfad, string sha)
    {
        using var req = Anfrage(HttpMethod.Get, $"{basis}/blob/{sha}");
        var (code, daten) = await GehirnNetz.Anfrage(req, 30);
        if (code == 404) return false;   // abgelöst und schon aufgeräumt: die nächste Fassung kommt mit dem Abgleich
        if (code != 200) throw new IOException($"Datei holen: Server {code}");
        if (Convert.ToHexStringLower(SHA256.HashData(daten)) != sha) { Log("fehler", $"{pfad} kam beschädigt an", wo: "Listen"); return false; }
        if (JSON.Objekt(daten) is not { } o) return false;
        await Task.Run(() => Atomar(LokalDatei(pfad), daten));
        Uebernimm(o);
        return true;
    }

    private static void Atomar(string ziel, byte[] daten)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(ziel)!);
        var teil = ziel + ".teil";
        File.WriteAllBytes(teil, daten);
        File.Move(teil, ziel, true);
    }

    /// <summary>Kennung des Dokuments am Server wie `GedaechtnisAbgleich.kennung`: nur Buchstaben, Ziffern, _ und -.</summary>
    public static string Kennung(string pfad)
    {
        var glatt = Regex.Replace(pfad, "[^A-Za-z0-9_-]", "_");
        var eindeutig = pfad.All(c => (char.IsLetterOrDigit(c) && c < 128) || "_-./".Contains(c));
        return eindeutig && glatt.Length <= 64 ? glatt : glatt[..Math.Min(40, glatt.Length)] + "_" + Convert.ToHexStringLower(SHA256.HashData(Encoding.UTF8.GetBytes(pfad)))[..23];
    }

    /// <summary>Lädt alle hier geänderten Dateien hoch. Abgelehnt heißt: drüben liegt eine jüngere Fassung, sie gilt und kommt gleich mit.</summary>
    private async Task Hochladen()
    {
        if (hochladenLaeuft) return;
        var basis = Basis;
        if (basis == null) return;
        hochladenLaeuft = true;
        try
        {
            foreach (var pfad in offen.ToList())
            {
                byte[] daten;
                try { daten = await File.ReadAllBytesAsync(LokalDatei(pfad)); } catch { offen.Remove(pfad); continue; }
                var sha = Convert.ToHexStringLower(SHA256.HashData(daten));
                using (var req = Anfrage(HttpMethod.Put, $"{basis}/blob/{sha}"))
                {
                    req.Content = new ByteArrayContent(daten);
                    req.Content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream");
                    var (c, _) = await GehirnNetz.Anfrage(req, 30);
                    if (c is < 200 or >= 300) throw new IOException($"Listen-Datei hochladen: Server {c}");
                }
                var vorher = fern.TryGetValue(pfad, out var f) ? f.Geaendert : 0;
                var geaendert = Math.Max(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), vorher + 1);
                var rumpf = new JSONObjekt
                {
                    ["daten"] = new JSONObjekt { ["pfad"] = pfad, ["text_sha"] = sha, ["groesse"] = (long)daten.Length },
                    ["geaendert"] = geaendert, ["geloescht"] = false, ["von"] = AppInfo.Name,
                };
                using var dreq = Anfrage(HttpMethod.Put, $"{basis}/doc/gedaechtnis/{Kennung(pfad)}");
                dreq.Content = GehirnNetz.JsonInhalt(rumpf);
                var (code, antwortDaten) = await GehirnNetz.Anfrage(dreq, 30);
                if (code == 409) continue;   // Datei am Server noch nicht vollständig: nächste Runde
                if (code is < 200 or >= 300) throw new IOException($"Listen-Fassung melden: Server {code}");
                var antwort = JSON.Objekt(antwortDaten) ?? new();
                if (antwort.Wahr("uebernommen") == true)
                {
                    fern[pfad] = (sha, geaendert);
                    offen.Remove(pfad);
                    continue;
                }
                // Drüben ist eine jüngere Fassung: Sie gilt (wie am Handy), die hiesige Änderung weicht.
                offen.Remove(pfad);
                if (antwort.Obj("doc") is { } d && d.Obj("daten") is { } w && w.Text("text_sha") is { Length: > 0 } fs)
                {
                    fern[pfad] = (fs, d.Ganz("geaendert") ?? 0);
                    await HoleDatei(basis, pfad, fs);
                    Meldung = "Eine Änderung kam zeitgleich von einem anderen Gerät; dessen jüngere Fassung gilt.";
                    Aenderung++;
                }
            }
        }
        finally
        {
            hochladenLaeuft = false;
            Ausstehend = offen.Count;
            ZustandSpeichern();
        }
    }

    private string? _meldung;
    /// <summary>Hinweis für die Oberfläche (etwa nach einer Kollision), verschwindet mit der nächsten Änderung von Frank.</summary>
    public string? Meldung { get => _meldung; set => Setze(ref _meldung, value); }

    private async void ZustandSpeichern()
    {
        if (speicherPlan) return;
        speicherPlan = true;
        await Task.Delay(800);
        speicherPlan = false;
        var z = new JSONObjekt
        {
            ["stand"] = stand, ["instanz"] = instanz,
            ["fern"] = fern.ToDictionary(kv => kv.Key, kv => (object?)new JSONObjekt { ["sha"] = kv.Value.Sha, ["geaendert"] = kv.Value.Geaendert }),
            ["offen"] = offen.Cast<object?>().ToList(),
        };
        var datei = ZustandDatei;
        await Task.Run(() => { try { Atomar(datei, JSON.Daten(z)); } catch { } });
    }

    // MARK: Schreiben (wie Listen.schreibe am Handy: alles in einem Zug, nur echte Änderungen)

    public sealed class Schreiber
    {
        private readonly ListenSpeicher s;
        internal readonly Dictionary<long, JSONObjekt> ListenNeu = new(), EintraegeNeu = new();
        internal Schreiber(ListenSpeicher s) { this.s = s; }
        public Liste? Liste(long id) => ListenNeu.TryGetValue(id, out var n) ? new Liste(n) : s.ListeZu(id);
        public ListenEintrag? Eintrag(long id) => EintraegeNeu.TryGetValue(id, out var n) ? new ListenEintrag(n) : s.eintraege.TryGetValue(id, out var e) && !e.Geloescht ? e : null;
        public List<ListenEintrag> AlleEintraege(long liste) =>
            s.eintraege.Values.Where(e => e.ListeId == liste && !EintraegeNeu.ContainsKey(e.Id)).Concat(EintraegeNeu.Values.Select(n => new ListenEintrag(n)).Where(e => e.ListeId == liste)).Where(e => !e.Geloescht).ToList();

        /// <summary>Eine Kopie mit geänderten Feldern vormerken; unverändert wird nichts geschrieben (sonst schlüge eine neue Zeit jüngere Änderungen drüben).</summary>
        public void SetzeListe(Liste l, Action<JSONObjekt> aendern) => Setze(l.Roh, l.Id, ListenNeu, s.listen.TryGetValue(l.Id, out var a) ? a.Roh : null, aendern);
        public void SetzeEintrag(ListenEintrag e, Action<JSONObjekt> aendern) => Setze(e.Roh, e.Id, EintraegeNeu, s.eintraege.TryGetValue(e.Id, out var a) ? a.Roh : null, aendern);
        public void NeuListe(JSONObjekt o) => ListenNeu[o.Ganz("id")!.Value] = o;
        public void NeuEintrag(JSONObjekt o) => EintraegeNeu[o.Ganz("id")!.Value] = o;

        private static void Setze(JSONObjekt basis, long id, Dictionary<long, JSONObjekt> ziel, JSONObjekt? gespeichert, Action<JSONObjekt> aendern)
        {
            var kopie = new JSONObjekt(ziel.TryGetValue(id, out var schon) ? schon : basis);
            aendern(kopie);
            if (gespeichert != null && GleichOhneZeit(gespeichert, kopie)) { ziel.Remove(id); return; }
            ziel[id] = kopie;
        }

        private static bool GleichOhneZeit(JSONObjekt a, JSONObjekt b)
        {
            var x = new JSONObjekt(a) { ["geaendert"] = 0L };
            var y = new JSONObjekt(b) { ["geaendert"] = 0L };
            return JSON.Text(x) == JSON.Text(y);
        }

        public List<ListenEintrag> Nachkommen(long id, long liste)
        {
            var alle = AlleEintraege(liste).GroupBy(e => e.Eltern).ToDictionary(g => g.Key, g => g.ToList());
            var aus = new List<ListenEintrag>();
            var gesehen = new HashSet<long> { id };
            void Sammle(long v) { if (alle.TryGetValue(v, out var k)) foreach (var e in k) if (gesehen.Add(e.Id)) { aus.Add(e); Sammle(e.Id); } }
            Sammle(id);
            return aus;
        }

        public List<ListenEintrag> Vorfahren(ListenEintrag e)
        {
            var aus = new List<ListenEintrag>();
            var gesehen = new HashSet<long> { e.Id };
            var v = e.Eltern;
            while (v != 0 && gesehen.Add(v)) { var p = Eintrag(v); if (p == null || p.ListeId != e.ListeId) break; aus.Add(p); v = p.Eltern; }
            return aus;
        }

        public int Tiefe(ListenEintrag e) => Vorfahren(e).Count;
        public int Hoehe(long id, long liste)
        {
            var alle = AlleEintraege(liste).GroupBy(e => e.Eltern).ToDictionary(g => g.Key, g => g.ToList());
            var gesehen = new HashSet<long>();
            int H(long v) => !gesehen.Add(v) ? 0 : alle.TryGetValue(v, out var k) && k.Count > 0 ? k.Max(e => 1 + H(e.Id)) : 0;
            return H(id);
        }
    }

    /// <summary>Wie `Listen.schreibe`: Datei, Bestand und Abgleich in einem Zug. Nur auf dem Hauptfaden.</summary>
    public T Schreibe<T>(Func<Schreiber, T> block)
    {
        var s = new Schreiber(this);
        var ergebnis = block(s);
        if (s.ListenNeu.Count == 0 && s.EintraegeNeu.Count == 0) return ergebnis;
        var jetzt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        foreach (var (id, o) in s.ListenNeu) Ablegen($"{Ordner}/l_{id}.json", o, jetzt);
        foreach (var (id, o) in s.EintraegeNeu) Ablegen($"{Ordner}/e_{id}.json", o, jetzt);
        Meldung = null;
        Ausstehend = offen.Count;
        Aenderung++;
        ZustandSpeichern();
        // Gleich hoch, nicht erst nach dem nächsten Long-Poll.
        Ui.Starte(async () => { try { await Hochladen(); } catch (Exception ex) { Log("listen", "Listen-Upload wartet", wo: "Listen", fehler: ex.Message); } });
        return ergebnis;
    }

    private void Ablegen(string pfad, JSONObjekt o, long jetzt)
    {
        o["geaendert"] = jetzt;
        var daten = JSON.Daten(o);
        Atomar(LokalDatei(pfad), daten);
        Uebernimm(o);
        offen.Add(pfad);
    }

    public long NeueId()
    {
        var vorschlag = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() * 1000 + Random.Shared.Next(1000);
        letzteId = Math.Max(letzteId + 1, vorschlag);
        return letzteId;
    }

    // MARK: Listen

    public Liste ListeAnlegen(string name, string art = "sonstiges") => Schreibe(s =>
    {
        var jetzt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var alle = Alle;
        var o = new JSONObjekt
        {
            ["id"] = NeueId(), ["name"] = name.Trim(), ["art"] = ListenArten.Alle.Any(a => a.Kennung == art) ? art : "sonstiges", ["farbe"] = (long)(alle.Count % 8),
            ["sortierung"] = "manuell", ["gruppierung"] = "keine", ["erledigte_zeigen"] = true, ["pos"] = (alle.Count == 0 ? 0 : alle.Max(l => l.Pos)) + 1.0,
            ["erstellt"] = jetzt, ["geaendert"] = jetzt, ["geloescht"] = false, ["abschnitte"] = new List<object?>(),
        };
        s.NeuListe(o);
        return new Liste(o);
    });

    public void ListeAendern(long id, Action<JSONObjekt> aendern) => Schreibe(s => { if (s.Liste(id) is { } l) s.SetzeListe(l, aendern); return 0; });

    /// <summary>Liste samt allen Einträgen als gelöscht markieren.</summary>
    public void ListeLoeschen(long id) => Schreibe(s =>
    {
        if (s.Liste(id) is not { } l) return 0;
        s.SetzeListe(l, o => o["geloescht"] = true);
        foreach (var e in s.AlleEintraege(id)) s.SetzeEintrag(e, o => o["geloescht"] = true);
        return 0;
    });

    public void ListenOrdnen(IReadOnlyList<long> ids) => Schreibe(s =>
    {
        for (var i = 0; i < ids.Count; i++) if (s.Liste(ids[i]) is { } l && l.Pos != i + 1.0) { var p = i + 1.0; s.SetzeListe(l, o => o["pos"] = p); }
        return 0;
    });

    // MARK: Einträge

    public ListenEintrag? Hinzufuegen(long liste, string titel, long eltern = 0, int prioritaet = 0, IEnumerable<string>? tags = null, string abschnitt = "", string faellig = "", string notiz = "") => Schreibe(s =>
    {
        abschnitt = abschnitt.Trim();
        if (abschnitt.Length > 0 && s.Liste(liste) is { } l0 && !l0.Abschnitte.Contains(abschnitt))
            s.SetzeListe(l0, o => o["abschnitte"] = l0.Abschnitte.Append(abschnitt).Cast<object?>().ToList());
        if (s.Liste(liste) == null) return null;
        var oben = eltern != 0 ? s.Eintrag(eltern) : null;
        if (oben != null && oben.ListeId != liste) oben = null;
        var vater = oben == null ? 0 : s.Tiefe(oben) + 1 > MaxTiefe ? oben.Eltern : oben.Id;
        var geschwister = s.AlleEintraege(liste).Where(e => e.Eltern == vater).ToList();
        var jetzt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var o = new JSONObjekt
        {
            ["id"] = NeueId(), ["liste"] = liste, ["titel"] = titel.Trim(), ["eltern"] = vater, ["notiz"] = notiz.Trim(), ["prioritaet"] = (long)Math.Clamp(prioritaet, 0, 3),
            ["faellig"] = faellig, ["tags"] = (tags ?? Array.Empty<string>()).Select(t => t.Trim().TrimStart('#')).Where(t => t.Length > 0).Cast<object?>().ToList(),
            ["erledigt"] = false, ["erledigt_am"] = 0L, ["pos"] = (geschwister.Count == 0 ? 0 : geschwister.Max(e => e.Pos)) + 1.0, ["erstellt"] = jetzt, ["geaendert"] = jetzt,
            ["geloescht"] = false, ["angeheftet"] = false, ["abschnitt"] = vater == 0 ? abschnitt : "",
        };
        s.NeuEintrag(o);
        return new ListenEintrag(o);
    });

    public void Aendern(long id, Action<JSONObjekt> aendern) => Schreibe(s => { if (s.Eintrag(id) is { } e) s.SetzeEintrag(e, aendern); return 0; });

    /// <summary>Abhaken wie am Handy: nimmt alle Unterpunkte mit; sind alle Geschwister erledigt, auch den Oberpunkt. Wieder öffnen öffnet erledigte Oberpunkte.</summary>
    public void Abhaken(long id, bool erledigt) => Schreibe(s =>
    {
        if (s.Eintrag(id) is not { } e) return 0;
        var jetzt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        void Setze(ListenEintrag x, bool an) => s.SetzeEintrag(x, o => { o["erledigt"] = an; o["erledigt_am"] = an ? jetzt : 0L; });
        Setze(e, erledigt);
        if (erledigt)
        {
            foreach (var k in s.Nachkommen(id, e.ListeId).Where(k => !k.Erledigt)) Setze(k, true);
            foreach (var p in s.Vorfahren(e))
            {
                if (p.Erledigt || s.AlleEintraege(e.ListeId).Any(x => x.Eltern == p.Id && !x.Erledigt)) break;
                Setze(p, true);
            }
        }
        else foreach (var p in s.Vorfahren(e)) if (p.Erledigt) Setze(p, false);
        return 0;
    });

    public void Loeschen(long id) => Schreibe(s =>
    {
        if (s.Eintrag(id) is not { } e) return 0;
        s.SetzeEintrag(e, o => o["geloescht"] = true);
        foreach (var k in s.Nachkommen(id, e.ListeId)) s.SetzeEintrag(k, o => o["geloescht"] = true);
        return 0;
    });

    public int ErledigteLoeschen(long liste) => Schreibe(s =>
    {
        var weg = s.AlleEintraege(liste).Where(e => e.Erledigt).ToList();
        foreach (var e in weg)
        {
            s.SetzeEintrag(e, o => o["geloescht"] = true);
            foreach (var k in s.Nachkommen(e.Id, liste)) s.SetzeEintrag(k, o => o["geloescht"] = true);
        }
        return weg.Count;
    });

    /// <summary>In eine andere Liste verschieben, mit allen Unterpunkten; dort auf der obersten Ebene.</summary>
    public void Verschieben(long id, long ziel) => Schreibe(s =>
    {
        if (s.Eintrag(id) is not { } e || s.Liste(ziel) == null) return 0;
        var kinder = s.Nachkommen(id, e.ListeId);
        var oben = s.AlleEintraege(ziel).Where(x => x.Eltern == 0).ToList();
        var pos = (oben.Count == 0 ? 0 : oben.Max(x => x.Pos)) + 1.0;
        s.SetzeEintrag(e, o => { o["liste"] = ziel; o["eltern"] = 0L; o["pos"] = pos; });
        foreach (var k in kinder) s.SetzeEintrag(k, o => o["liste"] = ziel);
        return 0;
    });

    /// <summary>Unterpunkt eines anderen Eintrags derselben Liste machen (0 = oberste Ebene). False, wenn das einen Kreis oder mehr als fünf Ebenen ergäbe.</summary>
    public bool Einhaengen(long id, long eltern) => Schreibe(s =>
    {
        if (s.Eintrag(id) is not { } e) return false;
        if (eltern == id || (eltern != 0 && s.Nachkommen(id, e.ListeId).Any(k => k.Id == eltern))) return false;
        if (eltern != 0)
        {
            if (s.Eintrag(eltern) is not { } p || p.ListeId != e.ListeId) return false;
            if (s.Tiefe(p) + 1 + s.Hoehe(id, e.ListeId) > MaxTiefe) return false;
        }
        var geschwister = s.AlleEintraege(e.ListeId).Where(x => x.Eltern == eltern).ToList();
        var pos = (geschwister.Count == 0 ? 0 : geschwister.Max(x => x.Pos)) + 1.0;
        s.SetzeEintrag(e, o => { o["eltern"] = eltern; o["pos"] = pos; });
        return true;
    });

    /// <summary>Eigene Reihenfolge: ids sind Geschwister in der neuen Reihenfolge.</summary>
    public void Ordnen(IReadOnlyList<long> ids) => Schreibe(s =>
    {
        for (var i = 0; i < ids.Count; i++) if (s.Eintrag(ids[i]) is { } e && e.Pos != i + 1.0) { var p = i + 1.0; s.SetzeEintrag(e, o => o["pos"] = p); }
        return 0;
    });

    // MARK: Ansicht (wie Listen.ansicht am Handy)

    private static readonly StringComparer Kollator = StringComparer.Create(new CultureInfo("de-DE"), CompareOptions.IgnoreCase);

    private static int Manuell(ListenEintrag a, ListenEintrag b) { var c = a.Pos.CompareTo(b.Pos); return c != 0 ? c : a.Erstellt.CompareTo(b.Erstellt); }
    private static int Datum(ListenEintrag a, ListenEintrag b) => a.Faellig == b.Faellig ? 0 : a.Faellig.Length == 0 ? 1 : b.Faellig.Length == 0 ? -1 : string.CompareOrdinal(a.Faellig, b.Faellig);
    private static int Prio(ListenEintrag a, ListenEintrag b) => b.Prioritaet.CompareTo(a.Prioritaet);
    private static string? ErsterTag(ListenEintrag e) => e.Tags.Count == 0 ? null : e.Tags.OrderBy(t => t, Kollator).First();

    private static Comparison<ListenEintrag> Vergleicher(string sortierung)
    {
        Comparison<ListenEintrag> v = sortierung switch
        {
            "faellig" => (a, b) => Folge(Datum(a, b), Prio(a, b), Manuell(a, b)),
            "prioritaet" => (a, b) => Folge(Prio(a, b), Datum(a, b), Manuell(a, b)),
            "titel" => (a, b) => Folge(Kollator.Compare(a.Titel, b.Titel), Manuell(a, b)),
            "tag" => (a, b) =>
            {
                string? x = ErsterTag(a), y = ErsterTag(b);
                var t = x == y ? 0 : x == null ? 1 : y == null ? -1 : Kollator.Compare(x, y);
                return Folge(t, Prio(a, b), Manuell(a, b));
            },
            "erstellt" => (a, b) => b.Erstellt.CompareTo(a.Erstellt),
            "aelteste" => (a, b) => a.Erstellt.CompareTo(b.Erstellt),
            "geaendert" => (a, b) => b.Geaendert.CompareTo(a.Geaendert),
            _ => Manuell,
        };
        return (a, b) => Folge(b.Angeheftet.CompareTo(a.Angeheftet), v(a, b));
    }

    private static int Folge(params int[] werte) { foreach (var w in werte) if (w != 0) return w; return 0; }

    private static (int Rang, string Name) FaelligGruppe(string f)
    {
        if (!DateTime.TryParseExact(f, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var d)) return (9, "Ohne Datum");
        var heute = DateTime.Today;
        if (d < heute) return (0, "Überfällig");
        if (d == heute) return (1, "Heute");
        if (d == heute.AddDays(1)) return (2, "Morgen");
        if (d <= heute.AddDays(7)) return (3, "Nächste 7 Tage");
        return (4, "Später");
    }

    /// <summary>Die Einträge einer Liste so, wie Frank sie am Handy sieht: offene oben (sortiert und gruppiert wie eingestellt),
    /// Unterpunkte eingerückt unter ihrem Oberpunkt, erledigte Oberpunkte unten unter „Erledigt“.</summary>
    public List<ListenZeile> Ansicht(Liste liste, bool? erledigteZeigen = null)
    {
        var alle = EintraegeVon(liste.Id);
        var nachId = alle.ToDictionary(e => e.Id);
        bool ErreichtOben(ListenEintrag e)
        {
            var gesehen = new HashSet<long> { e.Id };
            var v = e.Eltern;
            while (v != 0) { if (!gesehen.Add(v) || !nachId.TryGetValue(v, out var p)) return false; v = p.Eltern; }
            return true;
        }
        var kinder = alle.GroupBy(e => e.Eltern != 0 && nachId.ContainsKey(e.Eltern) && ErreichtOben(e) ? e.Eltern : 0L).ToDictionary(g => g.Key, g => g.ToList());
        var vgl = Vergleicher(liste.Sortierung);
        var zeigen = erledigteZeigen ?? liste.ErledigteZeigen;
        var aus = new List<ListenZeile>();
        var gezeigt = new HashSet<long>();
        List<ListenEintrag> Sortiert(IEnumerable<ListenEintrag> l) { var x = l.ToList(); x.Sort(vgl); return x; }
        void Zeile(ListenEintrag e, int tiefe)
        {
            if (!gezeigt.Add(e.Id)) return;
            var k = kinder.TryGetValue(e.Id, out var kk) ? kk : new();
            aus.Add(new ListenZeile(e, tiefe, k.Count(x => x.Erledigt), k.Count));
            foreach (var x in Sortiert(k.Where(x => !x.Erledigt))) Zeile(x, tiefe + 1);
            if (zeigen || e.Erledigt) foreach (var x in Sortiert(k.Where(x => x.Erledigt))) Zeile(x, tiefe + 1);
        }
        void Gruppe(string kopf, List<ListenEintrag> l) { aus.Add(new ListenZeile(Kopf: kopf, KopfAnzahl: l.Count)); foreach (var e in l) Zeile(e, 0); }
        var oben = kinder.TryGetValue(0, out var o0) ? o0 : new();
        var offenOben = Sortiert(oben.Where(e => !e.Erledigt));
        switch (liste.Gruppierung)
        {
            case "abschnitt":
                var reihe = liste.Abschnitte.Concat(offenOben.Select(e => e.Abschnitt).Where(a => a.Length > 0 && !liste.Abschnitte.Contains(a)).Distinct().OrderBy(a => a, Kollator)).ToList();
                foreach (var g in offenOben.GroupBy(e => e.Abschnitt).OrderBy(g => g.Key.Length == 0 ? -1 : reihe.IndexOf(g.Key))) Gruppe(g.Key.Length == 0 ? "Ohne Abschnitt" : g.Key, g.ToList());
                break;
            case "erstellt":
                foreach (var g in offenOben.GroupBy(e => DateTimeOffset.FromUnixTimeMilliseconds(e.Erstellt).LocalDateTime.Date).OrderByDescending(g => g.Key))
                    Gruppe("Erstellt " + g.Key.ToString("d. MMMM yyyy", new CultureInfo("de-DE")), g.ToList());
                break;
            case "prioritaet":
                foreach (var g in offenOben.GroupBy(e => e.Prioritaet).OrderByDescending(g => g.Key)) Gruppe(g.Key == 0 ? "Ohne Priorität" : "Priorität " + ListenArten.PrioName(g.Key), g.ToList());
                break;
            case "faellig":
                foreach (var g in offenOben.GroupBy(e => FaelligGruppe(e.Faellig)).OrderBy(g => g.Key.Rang)) Gruppe(g.Key.Name, g.ToList());
                break;
            case "tag":
                foreach (var g in offenOben.GroupBy(e => ErsterTag(e) ?? "").OrderBy(g => g.Key.Length == 0 ? 1 : 0).ThenBy(g => g.Key, Kollator)) Gruppe(g.Key.Length == 0 ? "Ohne Tag" : "#" + g.Key, g.ToList());
                break;
            default:
                foreach (var e in offenOben) Zeile(e, 0);
                break;
        }
        var fertig = oben.Where(e => e.Erledigt).OrderByDescending(e => e.ErledigtAm).ToList();
        if (zeigen && fertig.Count > 0) Gruppe("Erledigt", fertig);
        return aus;
    }
}
