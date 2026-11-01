using System.IO;
using System.Net.Http;
using System.Globalization;
using System.Text;

namespace JarvisWindows;

/// <summary>Der allgemeine Kanal zwischen Jarvis Windows HP und Jarvis am Handy: zwei gleichberechtigte Agenten, die sich jederzeit schreiben.
/// Läuft über den Relay (Geheimnis der MCP-Adresse), nicht über die Handy-Werkzeuge: So kommt Post auch an, wenn die Gegenseite gerade
/// weg ist, und eine lange Begutachtung hängt an keiner Zeitgrenze. Eingang erst auf die Platte, dann quittieren, dann abarbeiten;
/// die fertige Antwort bleibt im Eingang, bis sie beim Relay liegt: Ein Absturz oder Funkloch verliert nichts und rechnet nichts doppelt.
/// Fragen vom Handy beantwortet ein eigenes Gespräch: je Nachricht ein frisches Modellgespräch mit Auszug des Kanals, damit Live-Gedächtnis
/// und Kosten nicht mit dem Kanal altern und wachsen. Kein Lernen daraus und keine Werkzeuge, die für Frank sprechen: Dort redet nicht Frank.
/// Läuft auf dem Hauptfaden. Den Helfer-Katalog bekommt das Handy von hier nicht geschickt: Es führt die Helfer des HP nicht; kommt doch Helfer-Post, bekommt sie eine klare Absage (HelferKatalog.KanalNachricht).</summary>
public sealed class AgentenKanal
{
    public static AgentenKanal Shared { get; } = new();

    private static readonly AsyncLocal<bool> imLaufWert = new();
    /// <summary>Solange ein Lauf eine Nachricht vom Handy beantwortet: Rückfragen stehen in der Antwort, sonst warten beide aufeinander.</summary>
    public static bool ImLauf => imLaufWert.Value;
    /// <summary>withValue(true): Gilt für f und alles, was daraus startet, nicht für den Aufrufer.</summary>
    public static async Task<T> MitImLauf<T>(Func<Task<T>> f)
    {
        imLaufWert.Value = true;
        return await f();
    }

    public const string Titel = "Jarvis am Handy (Agentenkanal)";
    private const double WarteSekunden = 60.0;
    private const double Zeitlimit = 600.0;
    private const double Hoechstalter = 6.0 * 3600;
    private static readonly HashSet<string> Gesperrt = new()
    {
        "jarvis_merken", "jarvis_vergessen", "jarvis_langzeitkontext_ersetzen", "mail_senden",
        "programmieren", "programmier_nachtrag", "programmier_antwort", "programmierer_sprechen", "agent_starten", "windows_dialog",
        // Aufträge vom Handy bedienen den Laptop voll (Computer Use, PowerShell, Terminal, CLI), ändern aber nicht Jarvis' eigene Einstellungen und schreiben nicht in ChatGPT.
        "windows_einstellungen", "chatgpt_app",
        // Fremde Texte im Kanal lösen keine Helfer aus; ansehen (suchen, info) bleibt erlaubt.
        "windows_helfer_ausfuehren", "windows_helfer_verwalten", "windows_helfer_bauen",
        "helfer_ausfuehren", "helfer_verwalten", "helfer_bauen", "helfer_registrieren", "helfer_auftraege",
    };
    private readonly string eingangDatei = Path.Combine(Pfade.Unterstuetzung, "agentenkanal-eingang.json");
    private readonly string verlaufDatei = Path.Combine(Pfade.Unterstuetzung, "agentenkanal-verlauf.json");

    private List<JSONObjekt> eingang = new();
    private List<string> erledigt = new();
    private List<JSONObjekt> verlauf = new();
    private readonly Dictionary<string, TaskCompletionSource<string?>> wartende = new();
    private bool arbeitet;
    private CancellationTokenSource? laufAbbruch;
    private Guid? laufGespraech;
    private const int GrossAb = 64_000;
    /// <summary>Hat der Relay die letzte Nachricht abgelehnt (400 leer oder unvollständig, 413 zu groß)? Dann hilft kein Wiederholen.</summary>
    private int letzterCode;
    private Task schreibKette = Task.CompletedTask;

    /// <summary>Beantwortet der Kanal gerade eine Nachricht in diesem Gespräch? Dann zeigt die Blase den Stopp-Knopf.</summary>
    public bool LaeuftIn(Gespraech g) => laufGespraech == g.Id;

    /// <summary>Frank stoppt den Lauf: Er endet samt laufender Befehle, Jarvis am Handy erfährt es, die Nachricht wird nicht wiederholt.</summary>
    public void LaufAbbrechen(Gespraech g)
    {
        if (laufGespraech == g.Id) laufAbbruch?.Cancel();
    }

    private AgentenKanal()
    {
        try
        {
            if (File.Exists(eingangDatei) && JSON.Objekt(File.ReadAllBytes(eingangDatei)) is { } j)
            {
                eingang = j.Objekte("eingang").ToList();
                erledigt = j.Texte("erledigt").ToList();
            }
            if (File.Exists(verlaufDatei) && JSON.Objekt(File.ReadAllBytes(verlaufDatei)) is { } v) verlauf = v.Objekte("verlauf").ToList();
        }
        catch (Exception ex) { Log("fehler", "Agentenkanal: Stand nicht gelesen", wo: "Agentenkanal", fehler: ex.Message); }
    }

    public void Starten()
    {
        _ = Haupt.Auf(async () =>
        {
            // Sofort abfragen: Erst das zeigt dem Relay, dass die App läuft, und das Handy wartet darauf. Abgearbeitet wird, wenn die Gespräche geladen sind (Abarbeiten).
            var abfragen = Abfragen();
            while (!GespraechsSpeicher.Shared.Geladen) await Task.Delay(100);
            Abarbeiten();
            HelferKatalog.Shared.Starten();
            await abfragen;
        });
    }

    private string? Basis
    {
        get
        {
            var a = Einstellungen.Shared.Zugang.Relay.Trim();
            if (!a.EndsWith("/mcp", StringComparison.Ordinal)) return null;
            return a[..^4] + "/hp/kanal";
        }
    }

    private static async Task<(int, byte[])> Anfrage(string adresse, JSONObjekt? rumpf = null, double sekunden = 60)
    {
        using var req = new HttpRequestMessage(rumpf != null ? HttpMethod.Post : HttpMethod.Get, adresse);
        if (rumpf != null) req.Content = GehirnNetz.JsonInhalt(rumpf);
        req.Headers.TryAddWithoutValidation("User-Agent", $"JarvisWindows/{AppInfo.Version}");
        return await GehirnNetz.Anfrage(req, sekunden);
    }

    private async Task Abfragen()
    {
        var pause = 2.0;
        while (true)
        {
            var basis = Basis;
            if (basis == null) { await Task.Delay(TimeSpan.FromSeconds(30)); continue; }
            try
            {
                var (code, daten) = await Anfrage(basis, null, 40);
                var j = code == 200 ? JSON.Objekt(daten) : null;
                if (j == null) throw new IOException("Unerwartete Antwort vom Relay.");
                if (j.Wahr("handy") is { } handyDa) McpKlient.Shared.HandySignal(handyDa);
                var neu = j.Objekte("nachrichten");
                // Ohne Quittung liefert der Relay sofort dasselbe noch einmal: darum wie jeder Fehler mit Pause.
                if (neu.Count > 0 && !await Annehmen(neu, basis)) throw new IOException("Quittung nicht angekommen.");
                pause = 2;
            }
            catch (Exception)
            {
                await Task.Delay(TimeSpan.FromSeconds(pause));
                pause = Math.Min(pause * 2, 15);
            }
        }
    }

    private async Task<bool> Annehmen(List<JSONObjekt> neu, string basis)
    {
        // Eine späte Antwort landet im Kanalgespräch: Ohne geladene Gespräche entstünde sonst ein zweites.
        while (!GespraechsSpeicher.Shared.Geladen && neu.Any(n => n.Text("art") == "antwort")) await Task.Delay(100);
        var ids = new List<string>();
        foreach (var n in neu)
        {
            var id = n.Text("id");
            if (id == null) continue;
            ids.Add(id);
            if (erledigt.Contains(id) || eingang.Any(x => x.Text("id") == id)) continue;
            var art = n.Text("art") ?? "info";
            var text = n.Text("text") ?? "";
            if (!IstHelfer(art)) Merke(id, "Handy", art, text);
            // Helfer-Post ist Technik: Sie steht nicht im Verlauf, den das Modell und Frank lesen.
            if (art == "antwort") { AntwortKam(n.Text("antwort_auf") ?? "", text); erledigt.Add(id); }
            else eingang.Add(n);
        }
        erledigt = erledigt.TakeLast(300).ToList();
        Sichere();
        await SchreibenFertig();   // Erst auf der Platte, dann quittieren.
        Abarbeiten();
        try
        {
            var (code, _) = await Anfrage(basis + "/quittung", new JSONObjekt { ["ids"] = ids }, 20);
            return code == 200;
        }
        catch (Exception) { return false; }
    }

    private void AntwortKam(string frage, string text)
    {
        if (wartende.Remove(frage, out var w)) { w.TrySetResult(text); return; }
        // Niemand wartet mehr: Sie steht im Kanalgespräch und im Verlauf (handy_jarvis_lesen).
        ZeigeImGespraech($"Späte Antwort von Jarvis am Handy: {text}");
        _ = Shell.Mitteilung("Jarvis am Handy", text.Kappe(180));
    }

    public static bool IstHelfer(string art) => art.StartsWith("helfer_", StringComparison.Ordinal) || art == "cli_recherche";

    private JSONObjekt? EingangMit(string id) => eingang.FirstOrDefault(x => x.Text("id") == id);

    private void Abarbeiten()
    {
        if (arbeitet || !GespraechsSpeicher.Shared.Geladen) return;
        var n = eingang.FirstOrDefault();
        if (n == null) return;
        arbeitet = true;
        _ = Lauf(n);
    }

    private async Task Lauf(JSONObjekt n)
    {
        var ok = false;
        try { ok = await Bearbeite(n); }
        catch (Exception ex) { Log("fehler", "Agentenkanal: Nachricht gescheitert", wo: "Agentenkanal", fehler: ex.Message); }
        // Antwort nicht zugestellt (Laptop ohne Netz): Sie bleibt im Eingang, später erneut senden.
        if (!ok) await Task.Delay(TimeSpan.FromSeconds(30));
        arbeitet = false;
        Abarbeiten();
    }

    private async Task<bool> Bearbeite(JSONObjekt n)
    {
        var id = n.Text("id") ?? "";
        var art = n.Text("art") ?? "info";
        var text = n.Text("text") ?? "";
        if (art == "info")
        {
            ZeigeImGespraech($"Mitteilung von Jarvis am Handy: {text}");
        }
        else
        {
            var antwort = n.Text("fertige_antwort");
            if (antwort == null)
            {
                var alter = GehirnHilfe.JetztSekunden() - (n.Zahl("zeit") ?? 0);
                if (IstHelfer(art))
                    antwort = await HelferKatalog.Shared.KanalNachricht(art, text);
                // Ein Lauf kann schon etwas bewirkt haben, wenn er durch Absturz oder Beenden abriss: nie blind wiederholen.
                else if (n.Wahr("begonnen") == true)
                    antwort = $"{AppInfo.Name} wurde während der Bearbeitung beendet. Ob der Auftrag ganz ausgeführt wurde, ist offen; er wird nicht wiederholt. Frag nach dem Stand oder gib ihn neu.";
                else if (alter > Hoechstalter)
                    antwort = "Deine Nachricht lag länger als sechs Stunden, ich habe sie nicht mehr bearbeitet. Frag neu, falls sie noch gilt.";
                else
                {
                    if (EingangMit(id) is { } laufend) { laufend["begonnen"] = true; Sichere(); await SchreibenFertig(); }
                    antwort = await Beantworte(art, text, id);
                }
                if (EingangMit(id) is { } e)
                {
                    e["fertige_antwort"] = antwort;
                    Sichere();
                }
            }
            if (!await Zustellen(id, antwort ?? "")) return false;
        }
        eingang.RemoveAll(x => x.Text("id") == id);
        erledigt.Add(id);
        Sichere();
        return true;
    }

    /// <summary>Dieselbe Kennung bei jedem Versuch: Der Relay legt die Antwort nicht doppelt ab. Lehnt er sie ab, statt nicht erreichbar zu sein, ginge das ewig so weiter und alles dahinter stünde: Dann kommt ein Hinweis, und die Warteschlange läuft weiter.</summary>
    private async Task<bool> Zustellen(string id, string antwort)
    {
        if (string.IsNullOrWhiteSpace(antwort)) antwort = "(keine Antwort)";
        if (await Sende("antwort", antwort, id, $"antwort-{id}")) return true;
        if (letzterCode is not (400 or 413)) return false;
        Log("fehler", $"Agentenkanal: Relay lehnt die Antwort ab (HTTP {letzterCode})", wo: "Agentenkanal");
        var kurz = $"Die vollständige Antwort ist zu groß für den Kanal, sie steht am Laptop im Gespräch „{Titel}“. Der Anfang:\n{antwort.Kappe(4000)}";
        if (EingangMit(id) is { } e) { e["fertige_antwort"] = kurz; Sichere(); }
        if (await Sende("antwort", kurz, id, $"antwort-{id}")) return true;
        return letzterCode is 400 or 413;
    }

    private async Task<string> Beantworte(string art, string text, string id)
    {
        Log("gespraech", $"Agentenkanal: {art} vom Handy", wo: "Agentenkanal");
        var g = KanalGespraech();
        var auftrag = Auftrag(art, text, VerlaufText(ohne: id));
        var antwort = "";
        var gestoppt = "Frank hat diesen Lauf am Laptop gestoppt. Was bis dahin geschehen ist, bleibt bestehen.";
        var abbruch = new CancellationTokenSource();
        laufAbbruch = abbruch;
        laufGespraech = g.Id;
        AppSteuerung.Shared.AbbruchStandGeaendert();
        try
        {
            for (var versuch = 0; versuch < 3; versuch++)
            {
                try
                {
                    if (versuch > 0) await Task.Delay(TimeSpan.FromSeconds(20), abbruch.Token);
                    // Erst wenn das Gespräch frei ist, den Verlauf leeren: Ein laufender Lauf hinge seine Ergebnisse sonst an die neue Liste, und die API lehnte sie ab.
                    var frei = DateTime.Now.AddSeconds(Zeitlimit);
                    while (g.Arbeitet)
                    {
                        if (DateTime.Now > frei) return $"{AppInfo.Name} ist nach zehn Minuten nicht fertig geworden und hat abgebrochen. Frag enger gefasst noch einmal.";
                        await Task.Delay(200, abbruch.Token);
                    }
                }
                catch (OperationCanceledException) { return gestoppt; }
                // Ab hier bis zum Beginn der Frage kein await: Sonst könnte wieder jemand das Gespräch belegen.
                g.Verlauf = new List<JSONObjekt>();
                g.Kontext = "";
                while (g.Nachrichten.Count > 200) g.Nachrichten.RemoveAt(0);
                var lauf = MitImLauf(() => JarvisAgent.Shared.Frage(auftrag, g, ohneWerkzeuge: Gesperrt, ct: abbruch.Token));
                if (!await GehirnHilfe.Frist(Zeitlimit, lauf))
                {
                    abbruch.Cancel();
                    return $"{AppInfo.Name} ist nach zehn Minuten nicht fertig geworden und hat abgebrochen. Frag enger gefasst noch einmal.";
                }
                try { antwort = await lauf; }
                catch (Exception ex)
                {
                    Log("fehler", "Agentenkanal: Lauf gescheitert", wo: "Agentenkanal", fehler: ex.Message);
                    antwort = $"⚠️ {ex.Message}";
                }
                if (abbruch.IsCancellationRequested) return gestoppt;
                // Modellfehler: Frage meldet sie als Text, und ein Aussetzer soll keine Antwort sein.
                if (!(antwort.Split("\n\n").LastOrDefault() ?? "").StartsWith("⚠️", StringComparison.Ordinal))
                    return string.IsNullOrWhiteSpace(antwort) ? "(keine Antwort)" : antwort.Replace("sag „weiter“, dann mache ich weiter.", "frag nach, wenn ich weitermachen soll.");
            }
            return $"{AppInfo.Name} konnte nicht antworten: {antwort}";
        }
        finally
        {
            laufAbbruch = null;
            laufGespraech = null;
            AppSteuerung.Shared.AbbruchStandGeaendert();
        }
    }

    private static string Auftrag(string art, string text, string verlauf)
    {
        var vorgabe = art switch
        {
            "stand" => "Ermittle den aktuellen Stand der Programmieraufgaben auf diesem Laptop, nicht aus der Erinnerung: laufende Sitzungen (`Get-Process claude`, Terminal-Fenster und ihr Inhalt per windows_powershell, %LOCALAPPDATA%\\JarvisPcBruecke), `git -C ~/proggs status -sb` und `git log`, laufende Bauvorgänge. Nenne je Aufgabe, was fertig ist, was gerade läuft und was hängt.",
            "begutachten" => "Begutachte das gesamte Geschehen auf diesem Laptop zu dieser Aufgabe: Bildschirm (windows_bildschirm), Programme, Terminal-Inhalte, Sitzungen, Logs, Git-Stand, Bau und Tests. Sag deine Einschätzung: Was läuft gut, was ist falsch oder riskant, was sollte als Nächstes geschehen.",
            _ => "Ist es ein Auftrag, führe ihn wirklich am Laptop aus (Computer Use, PowerShell, Terminal, CLI), statt ihn nur zu beantworten, und melde das Ergebnis. Ist es eine Frage, beantworte sie und prüfe dafür am Laptop selbst nach, statt zu raten.",
        };
        return $"[AGENTENKANAL – Nachricht von deinem Schwester-Agenten Jarvis am Handy, nicht von Frank. Art: {art}]\n{text}\n\n{vorgabe}\n"
            + "Deine Antwort geht zurück an Jarvis am Handy: kurz, sachlich, für sich verständlich. Rückfragen und Vorschläge, wer was wann macht, schreibst du einfach in die Antwort."
            + (verlauf.Length == 0 ? "" : $"\n\nZuletzt auf dem Kanal:\n{verlauf}");
    }

    private Gespraech KanalGespraech()
    {
        var da = GespraechsSpeicher.Shared.Alle.FirstOrDefault(x => x.Titel == Titel);
        if (da != null) return da;
        var g = GespraechsSpeicher.Shared.Neu();
        g.Titel = Titel;
        GespraechsSpeicher.Shared.Aufnehmen(g);
        return g;
    }

    private void ZeigeImGespraech(string text)
    {
        var g = KanalGespraech();
        g.Nachrichten.Add(new Nachricht(Rolle.Jarvis, text));
        g.Speichern();
    }

    /// <summary>Legt beim Relay ab und wiederholt bei Netzfehlern; null = nicht angekommen.</summary>
    private async Task<JSONObjekt?> Post(string art, string text, string? antwortAuf, string id)
    {
        var basis = Basis;
        if (basis == null) return null;
        var rumpf = new JSONObjekt { ["id"] = id, ["art"] = art, ["text"] = text };
        if (antwortAuf != null) rumpf["antwort_auf"] = antwortAuf;
        if (!IstHelfer(art) && !id.Contains("helfer-")) Merke(id, "Laptop", art, text);
        // Große Ergebnisse brauchen über langsames Netz ihre Zeit.
        var groesse = Encoding.UTF8.GetByteCount(text);
        var frist = groesse > GrossAb ? Math.Max(120, groesse / 50_000.0) : 15;
        letzterCode = 0;
        for (var versuch = 0; versuch < 4; versuch++)
        {
            if (versuch > 0) await Task.Delay(TimeSpan.FromSeconds(1 << versuch));
            try
            {
                var (code, daten) = await Anfrage(basis, rumpf, frist);
                if (code == 200) return JSON.Objekt(daten) ?? new JSONObjekt();
                if (code is 400 or 413) { letzterCode = code; break; }
            }
            catch (Exception) { }
        }
        Log("fehler", "Agentenkanal: Nachricht ans Handy nicht angekommen", wo: "Agentenkanal");
        return null;
    }

    public Task<bool> Sende(string art, string text, string? antwortAuf = null, string? id = null)
        => Haupt.Auf(async () => await Post(art, text, antwortAuf, id ?? Guid.NewGuid().ToString().ToUpperInvariant()) != null);

    private void Loese(string id, string? antwort)
    {
        if (wartende.Remove(id, out var w)) w.TrySetResult(antwort);
    }

    /// <summary>Frage ans Handy: wartet höchstens eine Minute. Danach kommt die Antwort trotzdem an, sie steht dann im Kanalgespräch.</summary>
    public Task<WerkzeugErgebnis> FrageHandy(string text, CancellationToken ct = default)
        => Haupt.Auf(() => FrageHandyLauf(text, ct));

    private async Task<WerkzeugErgebnis> FrageHandyLauf(string text, CancellationToken ct)
    {
        var id = Guid.NewGuid().ToString().ToUpperInvariant();
        bool? abgelegt = null;
        var handyDa = true;
        // Erst anmelden, dann senden: Eine schnelle Antwort kann kommen, bevor der Relay zurück ist.
        var w = new TaskCompletionSource<string?>(TaskCreationOptions.RunContinuationsAsynchronously);
        wartende[id] = w;
        using var abbruch = ct.Register(() => Haupt.Post(() => Loese(id, null)));
        _ = Uhr();
        _ = Senden();
        async Task Uhr()
        {
            await Task.Delay(TimeSpan.FromSeconds(WarteSekunden));
            Loese(id, null);
        }
        async Task Senden()
        {
            var stand = await Post("frage", text, null, id);
            abgelegt = stand != null;
            handyDa = stand?.Wahr("zugestellt") ?? false;
            if (!handyDa) Loese(id, null);
        }
        var antwort = await w.Task;
        if (antwort != null) return new WerkzeugErgebnis(antwort);
        if (ct.IsCancellationRequested) return new WerkzeugErgebnis("(abgebrochen)", true);
        // Das Senden läuft noch: Zugestellt ist damit nichts, das darf das Modell Frank nicht sagen.
        if (abgelegt == null) return new WerkzeugErgebnis("Die Nachricht ist nach einer Minute noch nicht beim Relay angekommen; ob sie ankommt, ist offen. Eine Antwort stünde später in handy_jarvis_lesen.", true);
        if (abgelegt == false)
            return new WerkzeugErgebnis("Die Nachricht ist nicht beim Relay angekommen (keine Verbindung oder keine Relay-Adresse).", true);
        if (!handyDa) return new WerkzeugErgebnis("Das Handy ist gerade nicht verbunden. Die Frage bleibt eingereiht; die Antwort steht später in handy_jarvis_lesen.");
        return new WerkzeugErgebnis("Noch keine Antwort nach einer Minute. Die Frage ist zugestellt; die Antwort steht später in handy_jarvis_lesen.");
    }

    private void Merke(string id, string von, string art, string text)
    {
        if (verlauf.Any(x => x.Text("id") == id)) return;
        verlauf.Add(new JSONObjekt { ["id"] = id, ["von"] = von, ["art"] = art, ["text"] = text.Kappe(4000), ["zeit"] = GehirnHilfe.JetztSekunden() });
        verlauf = verlauf.TakeLast(100).ToList();
        Schreibe(verlaufDatei, new JSONObjekt { ["verlauf"] = verlauf.ToList() });
    }

    private void Sichere()
        // Kopie der Einträge: Sie ändern sich auf dem Hauptfaden weiter, während im Hintergrund geschrieben wird.
        => Schreibe(eingangDatei, new JSONObjekt { ["eingang"] = eingang.Select(x => new JSONObjekt(x)).ToList(), ["erledigt"] = erledigt.ToList() });

    /// <summary>Der Reihe nach im Hintergrund: Der Hauptfaden wartet nie auf die Platte, und ein älterer Stand überholt keinen neueren.</summary>
    private void Schreibe(string pfad, JSONObjekt daten)
    {
        schreibKette = schreibKette.ContinueWith(_ =>
        {
            try { GehirnHilfe.SchreibeAtomar(pfad, JSON.Daten(daten)); }
            catch (Exception ex) { Log("fehler", "Agentenkanal: Stand nicht gesichert", wo: "Agentenkanal", fehler: ex.Message); }
        }, CancellationToken.None, TaskContinuationOptions.None, TaskScheduler.Default);
    }

    /// <summary>Alles bisher Gesicherte liegt auf der Platte (vor der Quittung, beim Beenden).</summary>
    public Task SchreibenFertig() => schreibKette;

    public string VerlaufText(int anzahl = 8, string ohne = "", int breite = 1500)
        => Haupt.Wert(() => string.Join("\n", verlauf.Where(n => n.Text("id") != ohne).TakeLast(anzahl).Select(n =>
        {
            var zeit = DateTimeOffset.FromUnixTimeMilliseconds((long)((n.Zahl("zeit") ?? 0) * 1000)).LocalDateTime.ToString("dd.MM. HH:mm", CultureInfo.InvariantCulture);
            return $"[{zeit}] {n.Text("von") ?? ""} ({n.Text("art") ?? ""}): {(n.Text("text") ?? "").Kappe(breite)}";
        })));

    public static readonly IReadOnlyList<LokalesWerkzeug> Werkzeuge = new[]
    {
        new LokalesWerkzeug(new WerkzeugDefinition(
            "handy_jarvis_sprechen",
            "Jarvis am Laptop: spricht über den Agentenkanal mit deinem Schwester-Agenten Jarvis am Handy, jederzeit und von dir aus. art=frage: Er arbeitet die Frage auf dem Handy ab (Apps, Sensoren, was dort läuft); du wartest bis zu einer Minute auf die Antwort. art=info: Mitteilung ohne Antwort, zum Beispiel wer was wann übernimmt. Während du selbst gerade eine Kanalnachricht beantwortest, geht nur art=info: Rückfragen schreibst du dann in deine Antwort.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["art"] = new JSONObjekt { ["type"] = "string", ["enum"] = new List<string> { "frage", "info" }, ["description"] = "frage oder info." },
                    ["text"] = new JSONObjekt { ["type"] = "string", ["description"] = "Die Nachricht, vollständig und für sich verständlich." },
                },
                ["required"] = new List<string> { "art", "text" },
            }), async a =>
        {
            var text = (a.Text("text") ?? "").Trim();
            if (text.Length == 0) return new WerkzeugErgebnis("Der Text ist leer.", true);
            if (a.Text("art") == "info")
            {
                var ok = await Shared.Sende("info", text);
                return new WerkzeugErgebnis(ok ? "Mitteilung an Jarvis am Handy abgelegt." : "Die Mitteilung ist nicht beim Relay angekommen.", !ok);
            }
            if (ImLauf) return new WerkzeugErgebnis("Du beantwortest gerade eine Kanalnachricht: Schreib die Rückfrage in deine Antwort.", true);
            return await Shared.FrageHandy(text, Shell.Abbruch.Value);
        }),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "handy_jarvis_lesen",
            "Jarvis am Laptop: die letzten Nachrichten des Agentenkanals mit Jarvis am Handy, in beide Richtungen, auch späte Antworten.",
            new JSONObjekt { ["type"] = "object", ["properties"] = new JSONObjekt() }), a =>
        {
            var t = Shared.VerlaufText(anzahl: 20, breite: 4000);
            return Task.FromResult(new WerkzeugErgebnis(t.Length == 0 ? "Auf dem Kanal steht noch nichts." : t));
        }),
    };
}
