using System.IO;
using System.Net.Http;
using System.Collections.ObjectModel;
using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using Microsoft.Win32;

namespace JarvisWindows;

/// <summary>Die gemeinsame Ablage aller Jarvis-Versionen auf dem Server (Relay, /j/&lt;Geheimnis&gt;/ablage).
/// Der Laptop hält die komplette Ablage als Kopie unter %APPDATA%\JarvisWindows\Ablage: Dokumente in docs.json, Inhalte nach Prüfsumme unter blobs.
/// Lesen geht also auch ohne Netz und ohne Handy; Schreiben braucht den Server und gilt erst, wenn er es angenommen hat.
/// Neues von anderen Versionen kommt per Long-Polling sofort an; eigene Änderungen wecken den Laptop nicht.
/// Läuft auf dem Hauptfaden; öffentliche Task-Methoden gehen von jedem Faden aus dorthin. Eintraege und Prompts werden nur als Ganzes ersetzt, nie verändert.
/// Einträge in docs werden nie verändert, nur ersetzt: Das Speichern im Hintergrund liest sie (am Mac waren es Kopien).</summary>
public sealed class AblageAbgleich : Beobachtbar
{
    public static AblageAbgleich Shared { get; } = new();
    public const string Quelle = AppInfo.Name;

    private ObservableCollection<AblageEintrag> _eintraege = new();
    public ObservableCollection<AblageEintrag> Eintraege { get => _eintraege; private set => Setze(ref _eintraege, value); }
    private ObservableCollection<PromptEintrag> _prompts = new();
    public ObservableCollection<PromptEintrag> Prompts { get => _prompts; private set => Setze(ref _prompts, value); }
    private string? _fehler;
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }
    private bool _verbunden;
    public bool Verbunden { get => _verbunden; private set => Setze(ref _verbunden, value); }
    private bool _bereit;
    /// <summary>Der erste Abgleich dieser Kopie ist durch: Vorher kennt sie vorhandene Titel und Nummern noch nicht.</summary>
    public bool Bereit { get => _bereit; private set => Setze(ref _bereit, value); }
    private int _ausstehend;
    /// <summary>Dateien, die noch vom Server kommen.</summary>
    public int Ausstehend { get => _ausstehend; private set => Setze(ref _ausstehend, value); }
    /// <summary>Dateien vom Laptop, die gerade hochgehen.</summary>
    public ObservableCollection<string> Hochladend { get; } = new();
    private int _inhalteStand;
    /// <summary>Zählt jeden neu eingetroffenen Inhalt: Kacheln ohne Bild versuchen es dann erneut.</summary>
    public int InhalteStand { get => _inhalteStand; private set => Setze(ref _inhalteStand, value); }
    private int _aufbauStand;
    /// <summary>Zählt jeden Neuaufbau der Liste.</summary>
    public int AufbauStand { get => _aufbauStand; private set => Setze(ref _aufbauStand, value); }

    private Dictionary<string, JSONObjekt> docs = new();
    private long stand;
    private bool laeuft;
    private bool ladeAufgabe;
    private bool nochmalLaden;
    private CancellationTokenSource? wiederholung;
    private CancellationTokenSource? abruf;
    private bool aufbauPlan;
    private bool speicherPlan;
    private readonly Dictionary<string, (int versuche, DateTime naechster)> nachversuche = new();
    private readonly Dictionary<string, string> textIndex = new();
    private DateTime letztesAufraeumen = DateTime.MinValue;
    /// <summary>Kennung des Serverspeichers: Wechselt sie, wurde er neu aufgesetzt, und die eigene Kopie muss zurück auf ihn.</summary>
    private string instanz = "";
    private readonly Dictionary<string, Task<string>> laufendeLadungen = new();

    private readonly string ordner;
    private string Blobs => Path.Combine(ordner, "blobs");
    private string DocsDatei => Path.Combine(ordner, "docs.json");

    private AblageAbgleich()
    {
        ordner = Path.Combine(Pfade.Unterstuetzung, "Ablage");
        Directory.CreateDirectory(Path.Combine(ordner, "blobs"));
    }

    private static string? Basis
    {
        get
        {
            var a = Einstellungen.Shared.Zugang.Relay.Trim();
            if (!a.EndsWith("/mcp", StringComparison.Ordinal)) return null;
            return a[..^4] + "/ablage";
        }
    }

    // MARK: Abgleich

    public void Hochfahren()
    {
        _ = Haupt.Auf(async () =>
        {
            if (laeuft) return;
            laeuft = true;
            // Nach dem Ruhezustand hängt der Long-Poll an einer toten Verbindung: abbrechen statt bis zum Zeitlimit warten.
            SystemEvents.PowerModeChanged += (_, e) =>
            {
                if (e.Mode != PowerModes.Resume) return;
                Haupt.Post(() =>
                {
                    abruf?.Cancel();
                    nachversuche.Clear();
                    AllesLaden();
                });
            };
            // Abseits des Hauptfadens: Bei großem Bestand verzögerte das JSON sonst den Start der App.
            await LadeDocs();
            NeuAufbauen();
            AllesLaden();
            await Schleife();
        });
    }

    private async Task Schleife()
    {
        var fehlerFolge = 0;
        double[] pausen = { 0.0, 2.0, 5.0, 15.0 };
        while (true)
        {
            var beginn = DateTime.Now;
            var runde = new CancellationTokenSource();
            abruf = runde;
            try
            {
                var anzahl = await Holen(25, runde.Token);
                fehlerFolge = 0;
                if (Fehler != null) Fehler = null;
                Verbunden = true;
                // Kam ohne Wartezeit und ohne Inhalt zurück: nicht im Dauerfeuer weiterfragen.
                if (anzahl == 0 && (DateTime.Now - beginn).TotalSeconds < 1) await Task.Delay(TimeSpan.FromSeconds(2));
            }
            catch (OperationCanceledException) when (runde.IsCancellationRequested)
            {
                continue;
            }
            catch (Exception ex)
            {
                // Ein einzelner Aussetzer kostet nichts und zeigt nichts; erst wenn es dauert, wird es langsamer und sichtbar.
                fehlerFolge += 1;
                Verbunden = false;
                if (fehlerFolge >= 2)
                {
                    Fehler = $"Server nicht erreichbar, du siehst den letzten Stand: {ex.Message}";
                    Log("ablage", "Abgleich unterbrochen", wo: "Ablage", fehler: ex.Message);
                }
                await Task.Delay(TimeSpan.FromSeconds(pausen[Math.Min(fehlerFolge, 3)]));
            }
        }
    }

    /// <summary>Einmal sofort abgleichen (Knopf „Neu laden“).</summary>
    public Task JetztAbgleichen() => Haupt.Auf(async () =>
    {
        try { await Holen(0, default); Fehler = null; }
        catch (Exception ex) { Fehler = $"Server nicht erreichbar, du siehst den letzten Stand: {ex.Message}"; }
    });

    private async Task<int> Holen(int warten, CancellationToken ct)
    {
        var basis = Basis ?? throw new IOException("Keine Relay-Adresse eingetragen.");
        using var req = new HttpRequestMessage(HttpMethod.Get, $"{basis}/aenderungen?seit={stand}&warten={warten}&geraet={Uri.EscapeDataString(Quelle)}");
        req.Headers.TryAddWithoutValidation("User-Agent", $"JarvisWindows/{AppInfo.Version}");
        var (code, daten) = await GehirnNetz.Anfrage(req, warten + 30, ct);
        if (code != 200) throw new IOException($"Der Server antwortet mit {code}.");
        // Das Lesen der Antwort abseits des Hauptfadens: Eine Seite kann viele Dokumente enthalten.
        var o = await Task.Run(() => JSON.Objekt(daten));
        if (o == null || o.Liste("docs") == null) throw new IOException("Unlesbare Antwort vom Server.");
        var liste = o.Objekte("docs");
        var neu = Zahl(o.GetValueOrDefault("stand"));
        // Der Server wurde neu aufgesetzt oder aus einem Backup geholt: Die eigene Kopie wird nicht verworfen, sie geht zurück auf den Server.
        var i = o.Text("instanz") ?? "";
        var neuerSpeicher = instanz.Length > 0 && i.Length > 0 && i != instanz;
        if (i.Length > 0) instanz = i;
        if (neuerSpeicher || o.Wahr("neuaufbau") == true)
        {
            Log("ablage", "Server wurde neu aufgesetzt, sende die eigene Kopie", wo: "Ablage");
            foreach (var k in docs.Keys.ToList()) docs[k] = new JSONObjekt(docs[k]) { ["stand"] = 0L };
            stand = 0;
            SpeichereDocs();
            _ = KopieZurueckSenden();
            return 0;
        }
        Bereit = true;
        // Ein kleinerer Stand ist eine überholte Antwort („Neu laden“ neben dem Long-Polling).
        if (neu < stand) return 0;
        var geaendert = false;
        foreach (var d in liste)
        {
            var art = d.Text("art");
            var id = d.Text("id");
            if (art == null || id == null) continue;
            var schluessel = $"{art}:{id}";
            // Eine Antwort von vor dem eigenen Speichern darf die gerade gespeicherte Fassung nicht zurückdrehen; das eigene Echo ändert nichts.
            if (docs.TryGetValue(schluessel, out var da) && Zahl(da.GetValueOrDefault("stand")) >= Zahl(d.GetValueOrDefault("stand"))) continue;
            docs[schluessel] = d;
            geaendert = true;
        }
        var weiter = neu != stand;
        stand = Math.Max(neu, stand);
        if (geaendert || weiter) SpeichereDocs();
        if (geaendert)
        {
            AufbauPlanen();
            Log("ablage", $"{liste.Count} Änderung(en) vom Server", wo: "Ablage");
            AllesLaden();
        }
        return liste.Count;
    }

    /// <summary>Kommen viele Änderungen nacheinander, wird die Liste einmal aufgebaut statt je Änderung.</summary>
    private async void AufbauPlanen()
    {
        if (aufbauPlan) return;
        aufbauPlan = true;
        await Task.Delay(150);
        aufbauPlan = false;
        NeuAufbauen();
    }

    private async Task LadeDocs()
    {
        var datei = DocsDatei;
        var o = await Task.Run(() =>
        {
            try { return JSON.Objekt(File.ReadAllBytes(datei)); } catch { return null; }
        });
        if (o == null) return;
        stand = Zahl(o.GetValueOrDefault("stand"));
        instanz = o.Text("instanz") ?? "";
        Bereit = stand > 0;
        var geladen = new Dictionary<string, JSONObjekt>();
        if (o.Obj("docs") is { } d)
            foreach (var (k, v) in d)
                if (v is JSONObjekt w) geladen[k] = w;
        docs = geladen;
    }

    /// <summary>Gesammelt und abseits des Hauptfadens. Die Datei ist nur eine Kopie: Fehlt die letzte Sekunde, holt der Abgleich sie wieder.</summary>
    private async void SpeichereDocs()
    {
        if (speicherPlan) return;
        speicherPlan = true;
        await Task.Delay(TimeSpan.FromSeconds(1));
        speicherPlan = false;
        var kopie = new Dictionary<string, JSONObjekt>(docs);
        var (s, inst, datei) = (stand, instanz, DocsDatei);
        await Task.Run(() =>
        {
            try { GehirnHilfe.SchreibeAtomar(datei, JSON.Daten(new JSONObjekt { ["stand"] = s, ["docs"] = kopie, ["instanz"] = inst })); } catch { }
        });
    }

    /// <summary>Lädt im Hintergrund alles, was hier noch fehlt. Eine Datei, die gerade nicht will (404, beschädigt, Platte voll), wartet für sich und hält die anderen nicht auf.</summary>
    public void AllesLaden() => Haupt.Post(() =>
    {
        if (ladeAufgabe) { nochmalLaden = true; return; }
        ladeAufgabe = true;
        _ = LadeLauf();
    });

    private async Task LadeLauf()
    {
        var bald = false;
        try
        {
            var benutzt = BenutzteInhalte();
            var blobs = Blobs;
            var fehlend = await Task.Run(() => benutzt.Where(s => !File.Exists(Path.Combine(blobs, s))).ToList());
            Ausstehend = fehlend.Count;
            foreach (var sha in fehlend)
            {
                var (versuche, naechster) = nachversuche.TryGetValue(sha, out var n) ? n : (0, DateTime.MinValue);
                if (naechster > DateTime.Now) { bald = true; continue; }
                try
                {
                    await InhaltLauf(sha);
                    nachversuche.Remove(sha);
                    Ausstehend -= 1;
                }
                catch (Exception)
                {
                    // Mit wachsendem Abstand je Datei: Eine volle Platte oder eine defekte Datei lädt sonst immer wieder Gigabytes.
                    nachversuche[sha] = (versuche + 1, DateTime.Now.AddSeconds(Math.Min(60 * Math.Pow(2, versuche), 3600)));
                    bald = true;
                }
            }
            // Selten aufräumen: Das Durchgehen aller Dateien lohnt nicht bei jeder Änderung. Was gerade erst entstand, gehört vermutlich zu einem laufenden Speichern und bleibt einen Tag liegen.
            if ((DateTime.Now - letztesAufraeumen).TotalSeconds > 3600)
            {
                letztesAufraeumen = DateTime.Now;
                var frisch = BenutzteInhalte();
                await Task.Run(() =>
                {
                    var grenze = DateTime.Now.AddDays(-1);
                    foreach (var u in new DirectoryInfo(blobs).EnumerateFiles())
                    {
                        if (frisch.Contains(u.Name)) continue;
                        try { if (u.LastWriteTime < grenze) u.Delete(); } catch { }
                    }
                });
            }
        }
        catch (Exception ex) { Log("ablage", "Laden der Inhalte unterbrochen", wo: "Ablage", fehler: ex.Message); bald = true; }
        ladeAufgabe = false;
        wiederholung?.Cancel();
        if (nochmalLaden) { nochmalLaden = false; AllesLaden(); }
        else if (bald)
        {
            var w = new CancellationTokenSource();
            wiederholung = w;
            try
            {
                await Task.Delay(TimeSpan.FromSeconds(60), w.Token);
                AllesLaden();
            }
            catch (OperationCanceledException) { }
        }
    }

    /// <summary>Alles aus der eigenen Kopie noch einmal zum Server, mit den alten Zeiten: Jüngeres dort gewinnt, und das Handy sendet seinen Teil ebenso.</summary>
    private async Task KopieZurueckSenden()
    {
        foreach (var d in docs.Values.ToList())
        {
            var art = d.Text("art");
            var id = d.Text("id");
            var w = d.Obj("daten");
            if (art == null || id == null || w == null) continue;
            var inhalte = new Dictionary<string, string>();
            var shas = w.Objekte("anhaenge").Select(a => a.Text("sha256")).Where(s => s != null).Select(s => s!).ToList();
            if (w.Text("text_sha") is { Length: > 0 } t) shas.Add(t);
            foreach (var s in shas) inhalte[s] = Pfad(s);
            try { await Senden(art, id, w, Zahl(d.GetValueOrDefault("geaendert")), d.Wahr("geloescht") == true, inhalte); } catch { }
        }
    }

    /// <summary>Eine ganz neue Kopie kennt vorhandene Titel und Nummern noch nicht: Sonst entstünden Doppel oder „gibt es nicht“.</summary>
    private async Task ErsterAbgleich()
    {
        if (!Bereit) await Holen(0, default);
    }

    private HashSet<string> BenutzteInhalte()
    {
        var r = new HashSet<string>();
        foreach (var d in docs.Values)
        {
            if (d.Text("art") != "eintrag" || d.Wahr("geloescht") == true) continue;
            var w = d.Obj("daten");
            if (w == null) continue;
            // Prüfsummen vom Server landen in Pfaden.
            if (w.Text("text_sha") is { Length: > 0 } t && KennungOk(t)) r.Add(t);
            foreach (var a in w.Objekte("anhaenge"))
                if (a.Text("sha256") is { Length: > 0 } s && KennungOk(s)) r.Add(s);
        }
        return r;
    }

    // MARK: Inhalte

    private string Pfad(string sha) => Path.Combine(Blobs, sha);

    /// <summary>Der Pfad zum Inhalt einer Prüfsumme, notfalls jetzt vom Server. Ein Inhalt wird nur einmal gleichzeitig geladen, ein abgebrochener Download macht beim nächsten Versuch weiter.</summary>
    public Task<string> Inhalt(string sha) => Haupt.Auf(() => InhaltLauf(sha));

    private async Task<string> InhaltLauf(string sha)
    {
        var ziel = Pfad(sha);
        if (File.Exists(ziel)) return ziel;
        if (laufendeLadungen.TryGetValue(sha, out var laufend)) return await laufend;
        var basis = Basis ?? throw new IOException("Keine Relay-Adresse eingetragen.");
        if (!KennungOk(sha)) throw new IOException("Ungültige Prüfsumme.");
        var t = Task.Run(() => Herunterladen($"{basis}/blob/{sha}", sha, ziel));
        laufendeLadungen[sha] = t;
        try
        {
            var r = await t;
            InhalteStand += 1;
            return r;
        }
        finally { laufendeLadungen.Remove(sha); }
    }

    private static async Task<string> Herunterladen(string url, string sha, string ziel)
    {
        // Die angefangene Datei bleibt liegen und wird beim nächsten Versuch fortgesetzt (resumeData am Mac).
        var teil = ziel + ".teil";
        var vorhanden = File.Exists(teil) ? new FileInfo(teil).Length : 0;
        using var req = new HttpRequestMessage(HttpMethod.Get, url);
        if (vorhanden > 0) req.Headers.Range = new System.Net.Http.Headers.RangeHeaderValue(vorhanden, null);
        using var gesamt = new CancellationTokenSource(TimeSpan.FromHours(6));
        using var antwort = await GehirnNetz.Strom(req, 120, gesamt.Token, GehirnNetz.DateiClient);
        var code = (int)antwort.StatusCode;
        if (code == 416)
        {
            File.Delete(teil);
            throw new IOException("Der Download passt nicht zur angefangenen Datei.");
        }
        if (code != 200 && code != 206) throw new FileNotFoundException("Die Datei gibt es auf dem Server nicht.");
        await using (var quelle = await antwort.Content.ReadAsStreamAsync(gesamt.Token))
        await using (var datei = new FileStream(teil, code == 206 ? FileMode.Append : FileMode.Create, FileAccess.Write, FileShare.None, 1 << 16, true))
        {
            var puffer = new byte[1 << 16];
            using var uhr = CancellationTokenSource.CreateLinkedTokenSource(gesamt.Token);
            while (true)
            {
                uhr.CancelAfter(TimeSpan.FromSeconds(120));
                var n = await quelle.ReadAsync(puffer, uhr.Token);
                if (n == 0) break;
                await datei.WriteAsync(puffer.AsMemory(0, n), gesamt.Token);
            }
        }
        if (await Pruefsumme(teil) != sha)
        {
            File.Delete(teil);
            throw new InvalidDataException("Die Prüfsumme der Datei stimmt nicht.");
        }
        File.Move(teil, ziel, true);
        return ziel;
    }

    public static async Task<string> Pruefsumme(string pfad)
    {
        return await Task.Run(async () =>
        {
            await using var f = new FileStream(pfad, FileMode.Open, FileAccess.Read, FileShare.Read, 1 << 20, true);
            return Convert.ToHexStringLower(await SHA256.HashDataAsync(f));
        });
    }

    public Task<string> Text(AblageEintrag e) => Haupt.Auf(async () =>
    {
        if (e.TextSha == null) return "";
        var u = await InhaltLauf(e.TextSha);
        return await Task.Run(() =>
        {
            try { return File.ReadAllText(u, Encoding.UTF8); } catch { return ""; }
        });
    });

    /// <summary>Volltextsuche: Titel, Dateinamen, Beschreibungen der Dateien und Texte. Noch nicht geladene Texte werden übersprungen, die Suche wartet nie aufs Netz.</summary>
    public Task<HashSet<string>> Suche(string wort, CancellationToken ct = default) => Haupt.Auf(async () =>
    {
        var w = wort.ToLowerInvariant();
        var r = new HashSet<string>();
        foreach (var e in Eintraege)
        {
            if (ct.IsCancellationRequested) break;
            if (e.Titel.ToLowerInvariant().Contains(w) || e.Dateien.Any(d => d.Name.ToLowerInvariant().Contains(w) || d.Beschreibung.ToLowerInvariant().Contains(w))) { r.Add(e.Id); continue; }
            if (e.TextSha is not { } sha) continue;
            if (!textIndex.ContainsKey(sha))
            {
                var u = Pfad(sha);
                var t = await Task.Run(() =>
                {
                    try { return File.Exists(u) ? File.ReadAllText(u, Encoding.UTF8) : null; } catch { return null; }
                });
                if (t != null) textIndex[sha] = t.ToLowerInvariant();
            }
            if (textIndex.TryGetValue(sha, out var inhalt) && inhalt.Contains(w)) r.Add(e.Id);
        }
        return r;
    });

    // MARK: Schreiben

    /// <summary>Schickt ein Dokument zum Server; fehlende Inhalte kommen zuerst hoch. Erst danach gilt es auch hier.</summary>
    private async Task Senden(string art, string id, JSONObjekt daten, long geaendert, bool geloescht = false, Dictionary<string, string>? inhalte = null)
    {
        var basis = Basis ?? throw AblageFehler.KeineAdresse;
        var url = $"{basis}/doc/{art}/{id}";
        var rumpf = new JSONObjekt { ["daten"] = daten, ["geaendert"] = geaendert, ["geloescht"] = geloescht, ["von"] = Quelle };
        async Task<(int, byte[])> Einmal()
        {
            using var req = new HttpRequestMessage(HttpMethod.Put, url) { Content = GehirnNetz.JsonInhalt(rumpf) };
            return await GehirnNetz.Anfrage(req, 60);
        }
        // Geht nur die Antwort verloren, erkennt der Server dieselbe Fassung wieder: Nichts entsteht doppelt.
        async Task<(int, byte[])> Schicken()
        {
            try { return await Einmal(); }
            catch (Exception ex) when (ex is HttpRequestException or IOException or TimeoutException)
            {
                await Task.Delay(TimeSpan.FromSeconds(2));
                return await Einmal();
            }
        }
        var (kopf, antwort) = await Schicken();
        if (kopf == 409)
        {
            foreach (var sha in JSON.Objekt(antwort)?.Texte("fehlend") ?? new List<string>())
            {
                if (inhalte == null || !inhalte.TryGetValue(sha, out var datei)) throw AblageFehler.InhaltFehlt;
                await using var strom = new FileStream(datei, FileMode.Open, FileAccess.Read, FileShare.Read, 1 << 16, true);
                using var r = new HttpRequestMessage(HttpMethod.Put, $"{basis}/blob/{sha}") { Content = new StreamContent(strom) };
                var (k, _) = await GehirnNetz.Anfrage(r, 900, default, GehirnNetz.DateiClient);
                if (k != 200) throw AblageFehler.Hochladen;
            }
            (kopf, antwort) = await Schicken();
        }
        if (kopf != 200) throw AblageFehler.Server;
        var ok = JSON.Objekt(antwort);
        if (ok?.Wahr("uebernommen") != true) throw AblageFehler.NeuereFassung;
        var neuerStand = Zahl(ok.GetValueOrDefault("stand"));
        docs[$"{art}:{id}"] = new JSONObjekt
        {
            ["art"] = art, ["id"] = id, ["daten"] = geloescht ? new JSONObjekt() : daten, ["geaendert"] = geaendert,
            ["geloescht"] = geloescht, ["von"] = Quelle, ["stand"] = neuerStand,
        };
        // Lückenlos direkt dahinter: Der Zeiger rückt mit, das eigene Echo braucht nicht zu kommen.
        if (neuerStand == stand + 1) stand += 1;
        _ = McpKlient.Shared.KontextVerwerfen();
        SpeichereDocs();
        NeuAufbauen();
    }

    private static long Jetzt() => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    /// <summary>Geht diese Uhr nach, gälte die eigene Änderung sonst als älter als der Stand, auf dem sie aufbaut.</summary>
    private long Zeit(string art, string id) => Math.Max(Jetzt(), Zahl(docs.GetValueOrDefault($"{art}:{id}")?.GetValueOrDefault("geaendert")) + 1);
    private static string NeueId() => Guid.NewGuid().ToString("N")[..20];

    /// <summary>Legt einen Inhalt in den eigenen Speicher, damit er nicht noch einmal vom Server geholt werden muss.</summary>
    private (string sha, string pfad) MerkeInhalt(byte[] daten)
    {
        var sha = Convert.ToHexStringLower(SHA256.HashData(daten));
        var u = Pfad(sha);
        if (!File.Exists(u)) GehirnHilfe.SchreibeAtomar(u, daten);
        return (sha, u);
    }

    private JSONObjekt? EintragDaten(string id)
    {
        if (!docs.TryGetValue($"eintrag:{id}", out var d) || d.Wahr("geloescht") == true) return null;
        return d.Obj("daten");
    }

    /// <summary>Neuer Text oder Ersatz bei genau gleichem Titel (wie am Handy); mit anhaengen wird ergänzt.</summary>
    public Task TextSpeichern(string titel, string inhalt, bool anhaengen = false) => Haupt.Auf(() => TextSpeichernLauf(titel, inhalt, anhaengen));

    private async Task TextSpeichernLauf(string titel, string inhalt, bool anhaengen)
    {
        await ErsterAbgleich();
        var name = TitelSauber(titel);
        var klein = name.ToLowerInvariant();
        var volltext = inhalt.Trim() + "\n";
        // Wie am Handy: Ersetzt wird nur bei genau gleichem Titel, ein eindeutiger Teil des Titels genügt nur zum Anhängen.
        var vorhanden = Eintraege.FirstOrDefault(e => TitelSauber(e.Titel).ToLowerInvariant() == klein);
        if (vorhanden == null && anhaengen)
        {
            var teil = Eintraege.Where(e => TitelSauber(e.Titel).ToLowerInvariant().Contains(klein)).ToList();
            if (teil.Count == 1) vorhanden = teil[0];
        }
        if (anhaengen && vorhanden?.TextSha != null)
        {
            // Beim Holen des Texts kann eine neuere Fassung ankommen: Angehängt wird an den Stand, der dann gilt.
            var v = vorhanden;
            var alt = await Text(v);
            while (Eintraege.FirstOrDefault(e => e.Id == v.Id) is { } jetzt && jetzt.TextSha != v.TextSha)
            {
                v = jetzt;
                alt = await Text(v);
            }
            volltext = alt + "\n\n" + volltext;
        }
        var bytes = Encoding.UTF8.GetBytes(volltext);
        var (sha, u) = MerkeInhalt(bytes);
        var id = vorhanden?.Id ?? NeueId();
        var zeit = Zeit("eintrag", id);
        var w = vorhanden != null && EintragDaten(vorhanden.Id) is { } da
            ? new JSONObjekt(da)
            : new JSONObjekt
            {
                ["id"] = id, ["titel"] = name, ["erstellt"] = zeit, ["herkunft"] = Quelle, ["quelle"] = Quelle, ["beschreibung"] = "", ["anhaenge"] = new List<JSONObjekt>(),
            };
        w["text_sha"] = sha;
        w["text_groesse"] = bytes.Length;
        w["geaendert"] = zeit;
        w["recherche"] = klein.Contains("recherche") || volltext.StartsWith("# Recherche", StringComparison.Ordinal);
        await Senden("eintrag", id, w, zeit, inhalte: new Dictionary<string, string> { [sha] = u });
        Log("ablage", $"Text gespeichert: {name}", wo: "Ablage");
    }

    /// <summary>Eine Datei vom Laptop in die Ablage: ein eigener Eintrag mit dem Dateinamen als Titel.</summary>
    public Task DateiHinzufuegen(string datei) => Haupt.Auf(async () =>
    {
        var dateiname = Path.GetFileName(datei);
        Hochladend.Add(dateiname);
        // Erst kopieren, dann die Prüfsumme der Kopie: Ändert sich die Datei gerade, liegt so nie falscher Inhalt unter einer Prüfsumme.
        // Abseits des Hauptfadens: Eine große Datei legte sonst die ganze App still. Das Datum zählt für das Aufräumen.
        var teil = Path.Combine(Blobs, Guid.NewGuid().ToString().ToUpperInvariant() + ".teil");
        try
        {
            await Task.Run(() =>
            {
                File.Copy(datei, teil);
                File.SetLastWriteTime(teil, DateTime.Now);
            });
            var sha = await Pruefsumme(teil);
            var u = Pfad(sha);
            if (!File.Exists(u)) File.Move(teil, u);
            var groesse = new FileInfo(u).Length;
            var endung = Path.GetExtension(datei).TrimStart('.').ToLowerInvariant();
            var typ = Dateiart.Bestimme(endung);
            var zeit = Jetzt();
            var titel = Path.GetFileNameWithoutExtension(datei);
            var id = NeueId();
            var anhang = new JSONObjekt
            {
                ["id"] = NeueId(), ["titel"] = titel, ["name"] = dateiname, ["endung"] = endung.Length == 0 ? "bin" : endung, ["mime"] = typ.mime, ["groesse"] = groesse,
                ["erstellt"] = zeit, ["herkunft"] = Quelle, ["sha256"] = sha, ["beschreibung"] = "", ["art"] = typ.art, ["kategorie"] = typ.kategorie,
            };
            var w = new JSONObjekt
            {
                ["id"] = id, ["titel"] = titel, ["text_sha"] = null, ["text_groesse"] = 0, ["erstellt"] = zeit, ["geaendert"] = zeit, ["herkunft"] = Quelle, ["quelle"] = Quelle,
                ["beschreibung"] = "", ["recherche"] = false, ["anhaenge"] = new List<JSONObjekt> { anhang },
            };
            await Senden("eintrag", id, w, zeit, inhalte: new Dictionary<string, string> { [sha] = u });
            Log("ablage", $"Datei hinzugefügt: {dateiname}", wo: "Ablage");
        }
        finally
        {
            Hochladend.Remove(dateiname);
            try { File.Delete(teil); } catch { }
        }
    });

    /// <summary>Löscht den Eintrag oder nur eine seiner Dateien, auf allen Jarvis-Versionen.</summary>
    public Task Loeschen(string eintragId, string? dateiId = null) => Haupt.Auf(() => LoeschenLauf(eintragId, dateiId));

    private async Task LoeschenLauf(string eintragId, string? dateiId)
    {
        var zeit = Zeit("eintrag", eintragId);
        if (dateiId != null && EintragDaten(eintragId) is { } alt)
        {
            var rest = alt.Objekte("anhaenge").Where(a => a.Text("id") != dateiId).ToList();
            var hatText = alt.Text("text_sha") is { Length: > 0 };
            if (rest.Count > 0 || hatText)
            {
                var w = new JSONObjekt(alt) { ["anhaenge"] = rest, ["geaendert"] = zeit };
                await Senden("eintrag", eintragId, w, zeit);
                return;
            }
        }
        await Senden("eintrag", eintragId, new JSONObjekt(), zeit, geloescht: true);
    }

    // MARK: Prompts

    /// <summary>Wie am Handy: Der gespeicherte Text endet immer mit dem Aufgabentrenner, damit die nächste Einsprache dort ansetzt.</summary>
    private static string PromptText(string t) => AufgabenText.Anhaengen("", t);

    public Task<PromptEintrag> PromptSpeichern(string text) => Haupt.Auf(() => PromptSpeichernLauf(text));

    private async Task<PromptEintrag> PromptSpeichernLauf(string text)
    {
        var sauber = PromptText(text);
        if (sauber.Length == 0) throw AblageFehler.Leer;
        var zeit = Jetzt();
        var w = new JSONObjekt { ["text"] = sauber, ["titel"] = "", ["erstellt"] = zeit, ["quelle"] = Quelle };
        await Senden("prompt", zeit.ToString(CultureInfo.InvariantCulture), w, zeit);
        return Prompts.FirstOrDefault(p => p.Erstellt == zeit) ?? new PromptEintrag(Prompts.Count, "", sauber, zeit, Quelle);
    }

    public Task PromptAendern(long erstellt, string text) => Haupt.Auf(() => PromptAendernLauf(erstellt, text));

    private async Task PromptAendernLauf(long erstellt, string text)
    {
        var id = erstellt.ToString(CultureInfo.InvariantCulture);
        if (docs.GetValueOrDefault($"prompt:{id}")?.Obj("daten") is not { } alt) throw AblageFehler.NichtMehrDa;
        var sauber = PromptText(text);
        if (sauber.Length == 0) throw AblageFehler.Leer;
        var w = new JSONObjekt(alt);
        if (w.Text("text") != sauber) w["titel"] = "";
        w["text"] = sauber;
        await Senden("prompt", id, w, Zeit("prompt", id));
    }

    public Task PromptLoeschen(long erstellt) => Haupt.Auf(() =>
    {
        var id = erstellt.ToString(CultureInfo.InvariantCulture);
        return Senden("prompt", id, new JSONObjekt(), Zeit("prompt", id), geloescht: true);
    });

    // MARK: Werkzeuge des Modells

    /// <summary>Was der Laptop selbst erledigt; alles andere geht ans Handy, ohne erst den Hauptfaden zu berühren.</summary>
    public static readonly IReadOnlySet<string> EigeneWerkzeuge = new HashSet<string> { "ablage_schreiben", "ablage_lesen", "ablage_loeschen", "prompts_lesen", "prompt_speichern" };

    /// <summary>Die Liste vom Handy zeigt auch Vorläufiges, kennt aber am Laptop gerade Gespeichertes womöglich noch nicht. Antwortet es nicht schnell, reicht die eigene Kopie.</summary>
    public WerkzeugErgebnis ListeMit(WerkzeugErgebnis vomHandy)
    {
        if (vomHandy.Fehler) return Liste();
        var fehlend = Eintraege.Where(e => !vomHandy.Text.Contains(e.Titel)).ToList();
        if (fehlend.Count == 0) return vomHandy;
        return new WerkzeugErgebnis(vomHandy.Text + "\n\nSchon in der Ablage, am Handy noch nicht angekommen:\n" + string.Join("\n", fehlend.Take(30).Select(e => $"- {e.Titel} ({e.Quelle})")));
    }

    /// <summary>Wie Werkzeug, aber nach der Frist bekommt das Modell eine Antwort; ein großer Download läuft dann im Hintergrund weiter.</summary>
    public async Task<WerkzeugErgebnis?> WerkzeugMitFrist(string name, JSONObjekt a, double sekunden)
    {
        var lauf = Werkzeug(name, a);
        if (await GehirnHilfe.Frist(sekunden, lauf)) return await lauf;
        return new WerkzeugErgebnis("Die Ablage hat nicht rechtzeitig geantwortet, eine Datei wird noch geholt. Bitte gleich noch einmal versuchen.", true);
    }

    /// <summary>Was der Laptop selbst am Server erledigt. Alles andere geht weiter ans Handy; dort erzeugte Einträge bekommen die Quelle des Laptops über _quelle.</summary>
    public Task<WerkzeugErgebnis?> Werkzeug(string name, JSONObjekt a) => Haupt.Auf(() => WerkzeugLauf(name, a));

    private static readonly CultureInfo Deutsch = CultureInfo.GetCultureInfo("de-DE");

    private async Task<WerkzeugErgebnis?> WerkzeugLauf(string name, JSONObjekt a)
    {
        try
        {
            if (!EigeneWerkzeuge.Contains(name)) return null;
            await ErsterAbgleich();
            switch (name)
            {
                case "ablage_schreiben":
                {
                    if (a.Wahr("als_pdf") == true) return null;
                    var titel = a.Text("titel") ?? "";
                    var inhalt = a.Text("inhalt") ?? "";
                    if (inhalt.Trim().Length == 0) return new WerkzeugErgebnis("Der Inhalt ist leer.", true);
                    await TextSpeichernLauf(titel, inhalt, a.Wahr("anhaengen") == true);
                    return new WerkzeugErgebnis($"Abgelegt als „{titel}“.");
                }
                case "ablage_lesen":
                {
                    // Ohne Titel die Liste vom Handy (mit Vorläufigem), Unbekanntes kann ein vorläufiges Ergebnis dort sein.
                    var titel = a.Text("titel");
                    if (titel == null || titel.Trim().Length == 0 || Finde(titel) is not { } e) return null;
                    var t = $"Eintrag „{e.Titel}“ von {e.Quelle} (Stand {e.Geaendert.ToString("d. MMM yyyy, HH:mm", Deutsch)}):\n\n";
                    if (e.TextSha != null) t += (await Text(e)).Kappe(60_000) + "\n";
                    if (e.Dateien.Any()) t += "\nDATEIEN (in der Ablage ansehen):\n" + string.Join("\n", e.Dateien.Select(d => $"- {d.Name} ({d.Art}, {AblageAnsicht.Groesse(d.Groesse)})"));
                    return new WerkzeugErgebnis(t);
                }
                case "ablage_loeschen":
                {
                    // Direkte Aufrufe der App kannten nur die Kennung; Unbekanntes kann ein vorläufiges Ergebnis am Handy sein.
                    var kennung = a.Text("eintrag_id");
                    var titel = (a.Text("titel") ?? "").Trim();
                    if (kennung == null && titel.Length == 0) return new WerkzeugErgebnis("Es fehlt der Titel des Eintrags, gelöscht wurde nichts.", true);
                    var e = kennung != null ? Eintraege.FirstOrDefault(x => x.Id == kennung) : Finde(titel);
                    if (e == null) return null;
                    var wunsch = (a.Text("datei") ?? "").Trim().ToLowerInvariant();
                    if (a.Text("datei_id") is { } dateiKennung)
                    {
                        var d = e.Dateien.FirstOrDefault(x => x.Id == dateiKennung);
                        if (d == null) return new WerkzeugErgebnis("Diese Datei gibt es nicht mehr.", true);
                        await LoeschenLauf(e.Id, d.Id);
                        return new WerkzeugErgebnis($"Gelöscht: „{d.Name}“ aus „{e.Titel}“.");
                    }
                    if (wunsch.Length > 0)
                    {
                        var teil = e.Dateien.Where(x => x.Name.ToLowerInvariant().Contains(wunsch)).ToList();
                        var d = e.Dateien.FirstOrDefault(x => x.Name.ToLowerInvariant() == wunsch) ?? (teil.Count == 1 ? teil[0] : null);
                        if (d == null) return new WerkzeugErgebnis($"Keine eindeutige Datei „{wunsch}“ in „{e.Titel}“.", true);
                        await LoeschenLauf(e.Id, d.Id);
                        return new WerkzeugErgebnis($"Gelöscht: „{d.Name}“ aus „{e.Titel}“.");
                    }
                    await LoeschenLauf(e.Id, null);
                    return new WerkzeugErgebnis($"Gelöscht: „{e.Titel}“.");
                }
                case "prompts_lesen":
                {
                    string Kopf(PromptEintrag p) => $"Prompt {p.Nummer}{(p.Titel.Length == 0 ? "" : $" ({p.Titel})")} [id {p.Erstellt}]:\n{p.Text}";
                    var n = Zahl(a.GetValueOrDefault("nummer"));
                    if (n > 0)
                    {
                        var p = Prompts.FirstOrDefault(x => x.Nummer == n);
                        if (p == null) return new WerkzeugErgebnis($"Einen Prompt mit der Nummer {n} gibt es nicht.", true);
                        return new WerkzeugErgebnis(Kopf(p));
                    }
                    if (Prompts.Count == 0) return new WerkzeugErgebnis("Noch keine Prompts gespeichert.");
                    return new WerkzeugErgebnis(string.Join("\n\n", Prompts.Select(Kopf)));
                }
                case "prompt_speichern":
                {
                    var aktion = a.Text("aktion") ?? "speichern";
                    var text = a.Text("text") ?? "";
                    var erstellt = Zahl(a.GetValueOrDefault("erstellt"));
                    var nummer = Zahl(a.GetValueOrDefault("nummer"));
                    var ziel = Prompts.FirstOrDefault(p => erstellt != 0 ? p.Erstellt == erstellt : p.Nummer == nummer);
                    switch (aktion)
                    {
                        case "loeschen":
                            if (ziel == null) return new WerkzeugErgebnis("Diesen Prompt gibt es nicht.", true);
                            if (erstellt != 0 && text.Length > 0 && ziel.Text.Trim() != text.Trim())
                                return new WerkzeugErgebnis("Der Prompt wurde inzwischen geändert und bleibt stehen.", true);
                            await PromptLoeschen(ziel.Erstellt);
                            return new WerkzeugErgebnis($"Prompt {ziel.Nummer} gelöscht. Die folgenden Prompts sind eine Nummer aufgerückt.");
                        case "aendern":
                            if (ziel == null) return new WerkzeugErgebnis("Diesen Prompt gibt es nicht.", true);
                            await PromptAendernLauf(ziel.Erstellt, text);
                            return new WerkzeugErgebnis($"Prompt {ziel.Nummer} geändert.");
                        default:
                            var neu = await PromptSpeichernLauf(text);
                            return new WerkzeugErgebnis($"Als Prompt {neu.Nummer} im Reiter Prompts der Ablage gespeichert.");
                    }
                }
                default:
                    return null;
            }
        }
        catch (Exception ex)
        {
            return new WerkzeugErgebnis(ex.Message, true);
        }
    }

    /// <summary>Genauer Titel oder ein eindeutiger Teil davon, wie am Handy.</summary>
    private AblageEintrag? Finde(string titel)
    {
        var g = TitelSauber(titel).ToLowerInvariant();
        var e = Eintraege.FirstOrDefault(x => TitelSauber(x.Titel).ToLowerInvariant() == g);
        if (e != null) return e;
        var teil = Eintraege.Where(x => TitelSauber(x.Titel).ToLowerInvariant().Contains(g)).ToList();
        return teil.Count == 1 ? teil[0] : null;
    }

    /// <summary>Die Liste aus der eigenen Kopie, wenn das Handy nicht antwortet.</summary>
    public WerkzeugErgebnis Liste()
    {
        var alle = Eintraege;
        if (alle.Count == 0) return new WerkzeugErgebnis("Die Ablage ist leer.");
        var zeilen = alle.Take(80).Select(e =>
            $"- {e.Titel} ({e.Quelle}, {e.Geaendert.ToString("d. MMM yyyy", Deutsch)})" + (e.Dateien.Any() ? ": " + string.Join(", ", e.Dateien.Select(d => d.Name)) : ""));
        return new WerkzeugErgebnis($"{alle.Count} Eintrag/Einträge in der Ablage (Handy nicht erreichbar, Vorläufiges fehlt hier):\n" + string.Join("\n", zeilen));
    }

    private static readonly Regex Pfadzeichen = new(@"[\\/:*?""<>|\p{Cc}]", RegexOptions.Compiled);
    private static readonly Regex Leerraum = new(@"\s+", RegexOptions.Compiled);

    /// <summary>Wie AblageSpeicher.dateiname am Handy: ohne Pfadzeichen und .md, höchstens 80 Zeichen, sonst entstünden Doppel.</summary>
    private static string TitelSauber(string titel)
    {
        var t = titel.Trim();
        if (t.EndsWith(".md", StringComparison.OrdinalIgnoreCase)) t = t[..^3];
        t = Pfadzeichen.Replace(t, " ");
        t = Leerraum.Replace(t, " ");
        t = t.Kappe(80).Trim();
        return t.Length == 0 ? "Notiz" : t;
    }

    // MARK: Ansicht

    /// <summary>Das Modell schickt Zahlen mitunter als Text.</summary>
    public static long Zahl(object? x) => x switch
    {
        long l => l,
        int i => i,
        double d => (long)d,
        string s when long.TryParse(s.Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var w) => w,
        _ => 0,
    };

    private void NeuAufbauen()
    {
        var neu = new List<AblageEintrag>();
        var ps = new List<(string titel, string text, long erstellt, string quelle)>();
        foreach (var d in docs.Values)
        {
            if (d.Wahr("geloescht") == true) continue;
            var w = d.Obj("daten");
            var id = d.Text("id");
            if (w == null || id == null) continue;
            switch (d.Text("art"))
            {
                case "eintrag":
                    // Kennungen vom Server landen in Pfaden.
                    var dateien = w.Objekte("anhaenge").Where(a => KennungOk(a.Text("id")) && KennungOk(a.Text("sha256"))).Select(a =>
                        new AblageDatei(a.Text("id") ?? "", a.Text("name") ?? "", a.Text("art") ?? "SONSTIGE", a.Text("kategorie") ?? "SONSTIGE",
                            a.Text("endung") ?? "", (int)Zahl(a.GetValueOrDefault("groesse")), a.Text("sha256") ?? "", a.Text("beschreibung") ?? "")).ToList();
                    var sha = w.Text("text_sha") is { Length: > 0 } s ? s : null;
                    neu.Add(new AblageEintrag(id, w.Text("titel") ?? "", DateTimeOffset.FromUnixTimeMilliseconds(Zahl(d.GetValueOrDefault("geaendert"))).LocalDateTime, sha != null,
                        w.Wahr("recherche") ?? false, dateien, sha, (int)Zahl(w.GetValueOrDefault("text_groesse")), QuellenName(w.GetValueOrDefault("quelle"))));
                    break;
                case "prompt":
                    ps.Add((w.Text("titel") ?? "", w.Text("text") ?? "", long.TryParse(id, NumberStyles.Integer, CultureInfo.InvariantCulture, out var e) ? e : 0, QuellenName(w.GetValueOrDefault("quelle"))));
                    break;
            }
        }
        // Die gezogene Reihenfolge des Handys; was sie noch nicht kennt, steht unten, das Älteste zuerst.
        var ordnung = (docs.GetValueOrDefault("ordnung:prompts")?.Obj("daten")?.Liste("reihenfolge") ?? new List<object?>()).Select(Zahl).ToList();
        var platz = new Dictionary<long, int>();
        for (var i = 0; i < ordnung.Count; i++) platz.TryAdd(ordnung[i], i);
        var sortiert = ps.OrderBy(p => platz.TryGetValue(p.erstellt, out var x) ? 0 : 1)
            .ThenBy(p => platz.TryGetValue(p.erstellt, out var x) ? x : 0)
            .ThenBy(p => p.erstellt)
            .Select((p, i) => new PromptEintrag(i + 1, p.titel, p.text, p.erstellt, p.quelle));
        Eintraege = new ObservableCollection<AblageEintrag>(neu.OrderByDescending(e => e.Geaendert));
        Prompts = new ObservableCollection<PromptEintrag>(sortiert);
        AufbauStand += 1;
    }

    private static bool KennungOk(string? s)
    {
        if (string.IsNullOrEmpty(s) || s.Length > 64) return false;
        return s.All(c => char.IsAsciiLetterOrDigit(c) || c == '_' || c == '-');
    }

    private static string QuellenName(object? x)
    {
        var s = (x as string ?? "").Trim();
        return s.Length == 0 ? "Jarvis Handy" : s;
    }
}

public sealed class AblageFehler : Exception
{
    private AblageFehler(string text) : base(text) { }
    public static AblageFehler KeineAdresse => new("Keine Relay-Adresse eingetragen (Einstellungen → Konten).");
    public static AblageFehler InhaltFehlt => new("Der Inhalt fehlt hier, nichts wurde gespeichert.");
    public static AblageFehler Hochladen => new("Die Datei ließ sich nicht hochladen, nichts wurde gespeichert.");
    public static AblageFehler Server => new("Der Server hat das nicht angenommen, nichts wurde gespeichert.");
    public static AblageFehler NeuereFassung => new("Auf dem Server liegt schon eine neuere Fassung. Sie erscheint gleich hier.");
    public static AblageFehler Leer => new("Der Text ist leer.");
    public static AblageFehler NichtMehrDa => new("Das gibt es inzwischen nicht mehr.");
}

/// <summary>Dieselbe Einteilung wie Dateityp.art auf dem Handy; die Android-App berechnet sie aus der Endung selbst neu.</summary>
public static class Dateiart
{
    private static readonly HashSet<string> Code = new() { "kt", "kts", "java", "py", "js", "ts", "css", "sh", "ps1", "c", "h", "cpp", "cs", "swift", "go", "rs", "sql", "gradle", "bat", "rb", "php" };
    private static readonly HashSet<string> Daten = new() { "json", "xml", "yaml", "yml", "toml", "ini", "conf", "properties" };

    // UTType.preferredMIMEType der Mac-Fassung: gängige Typen fest, damit das Ergebnis nicht von der Registry des Rechners abhängt, der Rest aus der Registry.
    private static readonly Dictionary<string, string> Mimes = new()
    {
        ["txt"] = "text/plain", ["log"] = "text/plain", ["csv"] = "text/csv", ["tsv"] = "text/tab-separated-values", ["json"] = "application/json", ["xml"] = "application/xml",
        ["yaml"] = "application/yaml", ["yml"] = "application/yaml", ["toml"] = "application/toml", ["js"] = "text/javascript", ["ts"] = "video/mp2t", ["css"] = "text/css", ["py"] = "text/x-python-script",
        ["java"] = "text/x-java-source", ["c"] = "text/x-c", ["h"] = "text/x-c", ["cpp"] = "text/x-c++src", ["cs"] = "text/plain", ["swift"] = "text/x-swift", ["sh"] = "application/x-sh",
        ["php"] = "text/php", ["rb"] = "text/ruby-script", ["sql"] = "application/sql", ["bat"] = "application/x-bat",
        ["pdf"] = "application/pdf", ["doc"] = "application/msword", ["docx"] = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        ["odt"] = "application/vnd.oasis.opendocument.text", ["rtf"] = "text/rtf", ["epub"] = "application/epub+zip",
        ["xls"] = "application/vnd.ms-excel", ["xlsx"] = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ["ods"] = "application/vnd.oasis.opendocument.spreadsheet",
        ["ppt"] = "application/vnd.ms-powerpoint", ["pptx"] = "application/vnd.openxmlformats-officedocument.presentationml.presentation", ["odp"] = "application/vnd.oasis.opendocument.presentation",
        ["zip"] = "application/zip", ["7z"] = "application/x-7z-compressed", ["rar"] = "application/x-rar-compressed", ["gz"] = "application/gzip", ["tar"] = "application/x-tar",
        ["png"] = "image/png", ["jpg"] = "image/jpeg", ["jpeg"] = "image/jpeg", ["webp"] = "image/webp", ["bmp"] = "image/bmp", ["tif"] = "image/tiff", ["tiff"] = "image/tiff", ["heic"] = "image/heic", ["heif"] = "image/heif", ["ico"] = "image/x-icon",
        ["mp3"] = "audio/mpeg", ["m4a"] = "audio/mp4", ["wav"] = "audio/wav", ["flac"] = "audio/flac", ["ogg"] = "audio/ogg", ["opus"] = "audio/opus", ["aac"] = "audio/aac",
        ["mp4"] = "video/mp4", ["m4v"] = "video/mp4", ["mov"] = "video/quicktime", ["mkv"] = "video/x-matroska", ["webm"] = "video/webm", ["avi"] = "video/x-msvideo",
    };

    private static string MimeZu(string e)
    {
        if (e.Length == 0) return "application/octet-stream";
        if (Mimes.TryGetValue(e, out var m)) return m;
        try
        {
            using var k = Registry.ClassesRoot.OpenSubKey("." + e);
            if (k?.GetValue("Content Type") is string t && t.Length > 0) return t;
        }
        catch { }
        return "application/octet-stream";
    }

    public static (string art, string kategorie, string mime) Bestimme(string e)
    {
        var mime = MimeZu(e);
        switch (e)
        {
            case "md" or "markdown": return ("MARKDOWN", "TEXTE", "text/markdown");
            case "csv" or "tsv": return ("TABELLE_TEXT", "TABELLEN", mime);
            case "svg": return ("SVG", "BILDER", "image/svg+xml");
            case "gif": return ("ANIMATION", "BILDER", "image/gif");
            case "html" or "htm": return ("HTML", "TEXTE", "text/html");
        }
        if (Code.Contains(e)) return ("CODE", "TEXTE", mime);
        if (Daten.Contains(e)) return ("DATEN", "TEXTE", mime);
        switch (e)
        {
            case "pdf": return ("PDF", "DOKUMENTE", "application/pdf");
            case "doc" or "docx" or "odt" or "rtf" or "epub": return ("DOKUMENT", "DOKUMENTE", mime);
            case "xls" or "xlsx" or "ods": return ("TABELLE", "TABELLEN", mime);
            case "ppt" or "pptx" or "odp" or "key": return ("PRAESENTATION", "PRAESENTATIONEN", mime);
            case "zip" or "7z" or "rar" or "gz" or "tar": return ("ARCHIV", "SONSTIGE", mime);
        }
        if (mime.StartsWith("image/", StringComparison.Ordinal)) return ("BILD", "BILDER", mime);
        if (mime.StartsWith("audio/", StringComparison.Ordinal)) return ("AUDIO", "AUDIO", mime);
        if (mime.StartsWith("video/", StringComparison.Ordinal)) return ("VIDEO", "VIDEO", mime);
        if (e == "txt" || e == "log" || mime.StartsWith("text/", StringComparison.Ordinal)) return ("TEXT", "TEXTE", mime);
        return ("SONSTIGE", "SONSTIGE", mime);
    }
}
