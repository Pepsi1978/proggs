using System.IO;
using System.Net.Http;
using System.Text;

namespace JarvisWindows;

/// <summary>Anbindung an das Handy-Jarvis über dessen MCP-Relay (dieselbe Adresse wie das ChatGPT-Plugin und Claude Code).
/// Darüber laufen alle Werkzeuge der Handy-App: Live-Gedächtnis, Merken, Aufgaben, Wecker, Kalender, Ablage,
/// Biodaten, Agenten, Programmieraufträge … – so ist der Laptop immer auf demselben Stand wie das Handy.
/// Actor am Mac: Eigenschaften sind hier Methoden mit Task, der Zustand steht unter einem Schloss. Sicher von jedem Faden aus.
/// (WerkzeugDefinition und WerkzeugErgebnis stehen in Kern/Tresor.cs.)</summary>
public sealed class McpKlient
{
    public static McpKlient Shared { get; } = new();

    /// <summary>Der Hinweis, den das Handy an viele Ergebnisse hängt; in der Oberfläche hat er nichts zu suchen.</summary>
    public static string OhneKontextHinweis(string t)
        => t.Replace("\n(Falls in diesem Gespräch noch nicht geschehen: zuerst jarvis_kontext aufrufen.)", "").Trim();

    /// <summary>Technische Werkzeuge für die Ablage-Oberfläche; das Modell braucht sie nicht. Dazu die Kanäle des Handys zu den PC-Jarvis-Versionen.</summary>
    public static readonly IReadOnlySet<string> NurFuerOberflaeche = new HashSet<string>
    {
        "ablage_struktur", "ablage_datei", "jarvis_mac_starten", "jarvis_mac_sprechen", "jarvis_mac_lesen",
        "jarvis_windows_hp_starten", "jarvis_windows_hp_sprechen", "jarvis_windows_hp_lesen",
    };
    public static readonly IReadOnlySet<string> PrivateWerkzeuge = new HashSet<string>
    {
        "kontingente_lesen", "jarvis_kontext", "jarvis_gemerktes_lesen", "jarvis_merken", "jarvis_langzeitkontext_lesen", "jarvis_langzeitkontext_ersetzen",
        "gedaechtnis_suchen", "mail_lesen", "mail_senden", "tagebuch_lesen", "tagesauswertung_lesen", "tagesdaten_lesen",
        "biomarker_auswertung", "biomarker_tag", "biomarker_verlauf", "ablage_lesen", "ablage_struktur", "ablage_schreiben",
        "prompts_lesen", "ideen_lesen", "protokoll_lesen", "logbuch_lesen", "kalender_lesen", "aufgaben_lesen",
    };

    private readonly object schloss = new();
    private List<WerkzeugDefinition> werkzeugCache = new();
    private string anweisungen = "";
    private string serverVersion = "";
    private int naechsteId = 1;
    private string verbundeneAdresse = "";
    private DateTime letzterFehlschlag = DateTime.MinValue;
    private int fehlschlaegeInFolge;
    private Task<List<WerkzeugDefinition>>? werkzeugAbruf;
    private CancellationTokenSource? werkzeugAbbruch;
    private string kontextVorrat = "";
    private DateTime kontextZeit = DateTime.MinValue;
    private Task? kontextAbruf;

    private McpKlient() { }

    public Task<string> Anweisungen() { lock (schloss) return Task.FromResult(anweisungen); }
    public Task<string> ServerVersion() { lock (schloss) return Task.FromResult(serverVersion); }

    private static string Adresse => Einstellungen.Shared.Zugang.Relay;

    public sealed class Fehler : Exception
    {
        private Fehler(string text) : base(text) { }
        public static Fehler KeineAdresse => new("Keine Relay-Adresse für das Handy-Jarvis eingetragen (Einstellungen → Konten).");
        public static Fehler Http(int c, string t) => new($"Handy-Jarvis antwortet mit HTTP {c}: {t.Kappe(200)}");
        public static Fehler Antwort(string t) => new(t);
    }

    private async Task<JSONObjekt> Rpc(string methode, JSONObjekt? parameter = null, double sekunden = 60, CancellationToken ct = default)
    {
        var a = Adresse;
        if (a.Length == 0 || !Uri.TryCreate(a, UriKind.Absolute, out var url)) throw Fehler.KeineAdresse;
        using var req = new HttpRequestMessage(HttpMethod.Post, url);
        req.Headers.TryAddWithoutValidation("Accept", "application/json, text/event-stream");
        req.Headers.TryAddWithoutValidation("User-Agent", $"JarvisWindows/{AppInfo.Version}");
        var rumpf = new JSONObjekt { ["jsonrpc"] = "2.0", ["id"] = Interlocked.Increment(ref naechsteId) - 1, ["method"] = methode };
        if (parameter != null) rumpf["params"] = parameter;
        req.Content = GehirnNetz.JsonInhalt(rumpf);
        var (code, daten) = await GehirnNetz.Anfrage(req, sekunden, ct);
        var text = Encoding.UTF8.GetString(daten);
        if (code != 200) throw Fehler.Http(code, text);
        // Antwort kann JSON oder ein einzelnes SSE-Ereignis sein.
        var json = JSON.Objekt(daten);
        if (json == null)
        {
            var z = text.Split('\n', StringSplitOptions.RemoveEmptyEntries).LastOrDefault(l => l.StartsWith("data:", StringComparison.Ordinal));
            if (z != null) json = JSON.Objekt(z[5..].Trim());
        }
        if (json == null) throw Fehler.Antwort("Unlesbare Antwort vom Handy-Jarvis");
        if (json.Obj("error") is { } f) throw Fehler.Antwort(f.Text("message") ?? "MCP-Fehler");
        return json.Obj("result") ?? new JSONObjekt();
    }

    public async Task Verbinden(CancellationToken ct = default)
    {
        var r = await Rpc("initialize", new JSONObjekt
        {
            ["protocolVersion"] = "2025-06-18", ["capabilities"] = new JSONObjekt(),
            ["clientInfo"] = new JSONObjekt { ["name"] = "JarvisWindows", ["version"] = AppInfo.Version },
        }, 20, ct);
        lock (schloss)
        {
            anweisungen = r.Text("instructions") ?? "";
            serverVersion = r.Obj("serverInfo")?.Text("version") ?? "";
        }
    }

    /// <summary>Werkzeugliste des Handys, 10 Minuten zwischengespeichert, nach Namen sortiert (stabil für den Prompt-Cache).</summary>
    public async Task<List<WerkzeugDefinition>> Werkzeuge(bool frisch = false, CancellationToken ct = default)
    {
        var a = Adresse;
        Task<List<WerkzeugDefinition>> abruf;
        CancellationTokenSource? abbruch;
        lock (schloss)
        {
            if (a != verbundeneAdresse) // Relay gewechselt
            {
                werkzeugCache = new();
                anweisungen = "";
                verbundeneAdresse = a;
                letzterFehlschlag = DateTime.MinValue;
                fehlschlaegeInFolge = 0;
                kontextVorrat = "";
                kontextZeit = DateTime.MinValue;
            }
            // Die App erneuert die Liste selbst im Hintergrund: Keine Frage wartet darauf.
            if (!frisch && werkzeugCache.Count > 0) return werkzeugCache;
            // Eben nicht erreichbar: nicht bei jeder Frage erneut bis zur Zeitgrenze warten. Die App prüft alle 30 Sekunden selbst.
            if (!frisch && (DateTime.Now - letzterFehlschlag).TotalSeconds < 45) return new();
            if (werkzeugAbruf == null)
            {
                werkzeugAbbruch = new CancellationTokenSource();
                var token = werkzeugAbbruch.Token;
                werkzeugAbruf = Task.Run(async () =>
                {
                    try { return await WerkzeugeAbrufen(token); }
                    finally { lock (schloss) { werkzeugAbruf = null; werkzeugAbbruch = null; } }
                });
            }
            abruf = werkzeugAbruf;
            abbruch = werkzeugAbbruch;
        }
        // Stopp soll nicht auf den Abruf warten müssen.
        using (ct.Register(() => { try { abbruch?.Cancel(); } catch (ObjectDisposedException) { } }))
            return await abruf;
    }

    private async Task<List<WerkzeugDefinition>> WerkzeugeAbrufen(CancellationToken ct)
    {
        try
        {
            bool ohneAnweisungen;
            lock (schloss) ohneAnweisungen = anweisungen.Length == 0;
            if (ohneAnweisungen) await Verbinden(ct);
            var r = await Rpc("tools/list", null, 30, ct);
            var liste = new List<WerkzeugDefinition>();
            foreach (var t in r.Objekte("tools"))
            {
                var n = t.Text("name");
                if (n == null) continue;
                var schema = t.Obj("inputSchema") ?? new JSONObjekt { ["type"] = "object", ["properties"] = new JSONObjekt() };
                if (!schema.ContainsKey("type") || schema["type"] == null) schema["type"] = "object";
                liste.Add(new WerkzeugDefinition(n, t.Text("description") ?? "", schema));
            }
            liste.Sort((x, y) => string.CompareOrdinal(x.Name, y.Name));
            string version;
            lock (schloss)
            {
                if (liste.Count > 0)
                {
                    werkzeugCache = liste;
                    letzterFehlschlag = DateTime.MinValue;
                    fehlschlaegeInFolge = 0;
                }
                version = serverVersion;
            }
            Log("system", $"Handy-Jarvis verbunden: {liste.Count} Werkzeuge ({version})");
        }
        catch (Exception ex)
        {
            lock (schloss)
            {
                if (ct.IsCancellationRequested) return werkzeugCache;
                // Ein einzelner Aussetzer lässt die Liste stehen. Erst beim zweiten Fehlschlag in Folge gilt das Handy als weg:
                // keinen alten Stand als „verbunden“ melden.
                fehlschlaegeInFolge += 1;
                if (fehlschlaegeInFolge >= 2) werkzeugCache = new();
                letzterFehlschlag = DateTime.Now;
            }
            Log("fehler", $"Handy-Jarvis nicht erreichbar: {GehirnNetz.Beschreibung(ex)}");
        }
        lock (schloss) return werkzeugCache;
    }

    public Task<bool> Verbunden() { lock (schloss) return Task.FromResult(werkzeugCache.Count > 0); }
    /// <summary>Der letzte Abruf ging schief: Die App prüft dann bald erneut statt erst in fünf Minuten.</summary>
    public Task<bool> Unsicher() { lock (schloss) return Task.FromResult(fehlschlaegeInFolge > 0); }

    /// <summary>Holt das Live-Gedächtnis schon, während Frank spricht (Weckwort, neues Gespräch), damit die erste Antwort nicht darauf wartet.</summary>
    public Task KontextVorladen()
    {
        lock (schloss)
        {
            if (kontextAbruf != null || (DateTime.Now - kontextZeit).TotalSeconds <= 60) return Task.CompletedTask;
            kontextAbruf = Task.Run(async () =>
            {
                try
                {
                    var r = await Rufe("jarvis_kontext", new JSONObjekt(), 30);
                    if (!r.Fehler) lock (schloss) { kontextVorrat = r.Text; kontextZeit = DateTime.Now; }
                }
                finally { lock (schloss) kontextAbruf = null; }
            });
        }
        return Task.CompletedTask;
    }

    /// <summary>Das Live-Gedächtnis für ein neues Gespräch: der höchstens 5 Minuten alte Vorrat, sonst höchstens 8 Sekunden Warten.
    /// Kommt nichts, bleibt es leer: Das Modell erfährt das und die nächste Frage holt es nach. Ein alter Stand gilt nie.</summary>
    public async Task<string> KontextFuerGespraech()
    {
        string Frisch() { lock (schloss) return (DateTime.Now - kontextZeit).TotalSeconds < 300 ? kontextVorrat : ""; }
        if (Frisch().Length > 0) return Frisch();
        lock (schloss)
            if (werkzeugCache.Count == 0 && (DateTime.Now - letzterFehlschlag).TotalSeconds < 45) return "";
        await KontextVorladen();
        Task? t;
        lock (schloss) t = kontextAbruf;
        if (t != null) await GehirnHilfe.Frist(8, t);
        return Frisch();
    }

    /// <summary>Was nur liest, ändert das Gedächtnis nicht. Alles andere kann es ändern, etwa jarvis_merken: Dann gilt der Vorrat nicht mehr.</summary>
    private static bool NurLesend(string name)
        => name.EndsWith("_lesen", StringComparison.Ordinal) || name is "jarvis_kontext" or "gedaechtnis_suchen" or "web_suche" or "ablage_datei" or "ablage_struktur" or "vorgaenge";

    /// <summary>Diese Werkzeuge erzeugen am Handy einen Ablage-Eintrag: Er soll trotzdem als vom Laptop gelten.</summary>
    private static readonly HashSet<string> LegtInAblage = new() { "ablage_schreiben", "ablage_datei_speichern", "bild_erzeugen", "in_ablage_uebernehmen" };

    /// <summary>Die Oberfläche hat die Ablage geändert: Der gemerkte Kontext zeigt sie nicht mehr richtig.</summary>
    public Task KontextVerwerfen()
    {
        lock (schloss) { kontextVorrat = ""; kontextZeit = DateTime.MinValue; }
        return Task.CompletedTask;
    }

    public async Task<WerkzeugErgebnis> Rufe(string name, JSONObjekt argumente, double sekunden = 150, CancellationToken ct = default)
    {
        var start = DateTime.Now;
        // Ablage und Prompts liegen auf dem Server: Der Laptop schreibt selbst dorthin, auch wenn das Handy aus ist.
        if (AblageAbgleich.EigeneWerkzeuge.Contains(name))
        {
            var lokal = await AblageAbgleich.Shared.WerkzeugMitFrist(name, argumente, sekunden);
            if (lokal != null)
            {
                if (!NurLesend(name)) await KontextVerwerfen();
                Log("werkzeug", $"Ablage-Werkzeug {name}: {(lokal.Fehler ? "Fehler" : "ok")}", wo: "Server", werkzeug: name, fehler: lokal.Fehler ? lokal.Text : null,
                    dauerMs: (int)(DateTime.Now - start).TotalMilliseconds);
                return lokal;
            }
        }
        var args = new JSONObjekt(argumente);
        if (LegtInAblage.Contains(name)) args["_quelle"] = AblageAbgleich.Quelle;
        var nurListe = name == "ablage_lesen";
        var e = await RufeOhneLog(name, args, nurListe ? 3 : sekunden, ct);
        // Die Liste kommt vom Handy, weil sie auch Vorläufiges zeigt. Ist es aus, reicht die eigene Kopie der Ablage.
        if (nurListe && (args.Text("titel") ?? "").Trim().Length == 0) e = AblageAbgleich.Shared.ListeMit(e);
        if (!NurLesend(name)) await KontextVerwerfen();
        var ms = (int)(DateTime.Now - start).TotalMilliseconds;
        // Das Logbuch soll die App verbessern, nicht das Leben mitschreiben: Private Inhalte nur als Länge, Dateiabrufe nur bei Fehlern.
        if (name != "ablage_datei" || e.Fehler)
        {
            var privat = PrivateWerkzeuge.Contains(name);
            Log("werkzeug", $"Handy-Werkzeug {name}: {(e.Fehler ? "Fehler" : "ok")}", wo: "Handy über MCP", werkzeug: name,
                eingabe: privat ? null : JSON.Text(args), ergebnis: e.Fehler ? null : (privat ? $"({e.Text.Length} Zeichen)" : e.Text),
                fehler: e.Fehler ? e.Text : null, dauerMs: ms);
        }
        return e;
    }

    private async Task<WerkzeugErgebnis> RufeOhneLog(string name, JSONObjekt argumente, double sekunden, CancellationToken ct)
    {
        try
        {
            var r = await Rpc("tools/call", new JSONObjekt { ["name"] = name, ["arguments"] = argumente }, sekunden, ct);
            var inhalt = r.Objekte("content");
            var text = string.Join("\n", inhalt.Select(b => b.Text("text")).Where(t => t != null));
            byte[]? bild = null;
            var b = inhalt.FirstOrDefault(x => x.Text("type") == "image");
            if (b?.Text("data") is { } s)
            {
                try { bild = Convert.FromBase64String(s); } catch (FormatException) { }
            }
            if (text.Length == 0 && r.TryGetValue("structuredContent", out var sc) && sc != null) text = JSON.Text(sc);
            return new WerkzeugErgebnis(text.Length == 0 ? "(leer)" : text, r.Wahr("isError") ?? false, bild);
        }
        catch (Exception ex)
        {
            return new WerkzeugErgebnis($"Handy-Jarvis nicht erreichbar: {GehirnNetz.Beschreibung(ex)}", true);
        }
    }

    /// <summary>Bequemer Textaufruf für die Oberfläche (Tagesauswertung, Aufgaben, Kontext …).</summary>
    public async Task<string?> Text(string name, JSONObjekt? argumente = null, double sekunden = 60, CancellationToken ct = default)
    {
        var e = await Rufe(name, argumente ?? new JSONObjekt(), sekunden, ct);
        return e.Fehler ? null : e.Text;
    }
}
