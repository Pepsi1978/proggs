using System.Collections.ObjectModel;

namespace JarvisWindows;

/// <summary>Schlüssel nur fürs Vorlesen. Liegen verschlüsselt im Tresor (Tresor.cs), nie im Klartext, nie im Git.</summary>
public sealed class VorleseZugang
{
    public string Google { get; set; } = "";          // Google Cloud TTS (Chirp 3 HD)
    public string GeminiFrei { get; set; } = "";      // AI-Studio-Projekt ohne Abrechnung
    public string GeminiBezahlt { get; set; } = "";   // AI-Studio-Projekt mit Abrechnung
    public string Azure { get; set; } = "";           // Azure Speech-Schlüssel
    public string AzureRegion { get; set; } = VorleseKatalog.StandardAzureRegion;
    public string Qwen { get; set; } = "";            // Alibaba DashScope

    public static VorleseZugang Laden()
    {
        var z = Tresor.Lade<VorleseZugang>("vorlese-zugang");
        var neu = z == null;
        z ??= new VorleseZugang();
        // Erstbefüllung aus den allgemeinen Zugängen (aus ~/SK übernommen).
        var allg = Einstellungen.Shared.Zugang;
        if (z.Google.Length == 0) z.Google = allg.Google;
        if (neu && z.GeminiFrei.Length == 0) z.GeminiFrei = allg.Gemini;
        if (z.Azure.Length == 0) z.ErgaenzeAzureAusSK();
        z.Speichern();
        return z;
    }

    /// <summary>Schlüssel und Region der Azure-Ressource aus ~/SK/Azure/azure-speech.json. Vorhandene Werte bleiben.</summary>
    private void ErgaenzeAzureAusSK()
    {
        try
        {
            var o = JSON.Objekt(File.ReadAllBytes(Pfade.Sk("Azure/azure-speech.json")));
            var schluessel = (o?.Text("key1") ?? "").Trim();
            if (schluessel.Length == 0) return;
            Azure = schluessel;
            var region = (o?.Text("region") ?? "").Trim();
            if (region.Length > 0) AzureRegion = region;
        }
        catch { }
    }

    public void Speichern()
    {
        Tresor.Sichere(this, "vorlese-zugang");
        Geheimnisse.Setzen("vorlese", new[] { Google, GeminiFrei, GeminiBezahlt, Azure, Qwen });
    }

    /// <summary>Abzug für einen Vorleseauftrag außerhalb des Hauptfadens.</summary>
    public VorleseZugang Kopie() => (VorleseZugang)MemberwiseClone();
}

/// <summary>Eine geklonte Stimme aus dem Alibaba-Konto.</summary>
public sealed record KlonStimme(string Id, string Name, string Erstellt);

/// <summary>Alles, was ein Vorleseauftrag braucht: als Abzug für die Synthese außerhalb des Hauptfadens.</summary>
public sealed class VorleseStand
{
    public VorleseAnbieter Anbieter { get; init; }
    public Dictionary<VorleseAnbieter, string> StimmenJe { get; init; } = new();
    public Dictionary<VorleseAnbieter, double> TempoJe { get; init; } = new();
    public VorleseZugang Zugang { get; init; } = new();
    public string GeminiWahl { get; init; } = "";
    public List<RueckfallStufe> Kette { get; init; } = new();
    public bool ImmerDeutsch { get; init; }

    public string Stimme(VorleseAnbieter a) => StimmenJe.TryGetValue(a, out var s) ? s : "";
    public double Tempo(VorleseAnbieter a) => TempoJe.TryGetValue(a, out var t) ? t : 1;
}

/// <summary>Vorlese-Einstellungen wie in der Android-App (SecureSettings): Anbieter, Stimme und Tempo je Anbieter,
/// Gemini-Schlüsselwahl, Rückfallkette, Favoriten, „immer Deutsch“.</summary>
public sealed class VorleseEinstellungen : Beobachtbar
{
    public static VorleseEinstellungen Shared { get; } = new();
    public const string GeminiFreiDannBezahlt = "frei_dann_bezahlt";
    public const string GeminiNurFrei = "nur_frei";
    public const string GeminiNurBezahlt = "nur_bezahlt";
    public const double TempoMin = 0.5;
    public const double TempoMax = 2.0;

    private VorleseZugang _zugang;
    private CancellationTokenSource? speicherUhr;
    /// <summary>Nach Änderungen an Feldern von Zugang `ZugangGeaendert()` aufrufen (in Swift erledigte das der Setter der Struktur).</summary>
    public VorleseZugang Zugang { get => _zugang; set { _zugang = value; Melde(); ZugangGeaendert(); } }
    public void ZugangGeaendert()
    {
        speicherUhr?.Cancel();
        speicherUhr = new CancellationTokenSource();
        var t = speicherUhr.Token;
        Task.Delay(700, t).ContinueWith(_ => ZugangSichern(), t, TaskContinuationOptions.OnlyOnRanToCompletion, TaskScheduler.Default);
    }
    public void ZugangSichern() { speicherUhr?.Cancel(); speicherUhr = null; _zugang.Speichern(); }

    private VorleseAnbieter _anbieter;
    public VorleseAnbieter Anbieter { get => _anbieter; set { if (Setze(ref _anbieter, value)) Vorgaben.Setze("vl_anbieter", value.RawValue); } }

    private bool Merke(ref string feld, string wert, string schluessel, string name)
    {
        if (!Setze(ref feld, wert, name)) return false;
        Vorgaben.Setze(schluessel, wert);
        Logbuch.Shared.Einstellung(schluessel, wert, "Einstellungen/Stimme");
        return true;
    }

    private string _edgeStimme, _googleStimme, _geminiFlashStimme, _geminiLiteStimme, _azureStimme, _qwenStimme, _klonStimme, _geminiWahl;
    public string EdgeStimme { get => _edgeStimme; set => Merke(ref _edgeStimme, value, "vl_edge_stimme", nameof(EdgeStimme)); }
    public string GoogleStimme { get => _googleStimme; set => Merke(ref _googleStimme, value, "vl_google_stimme", nameof(GoogleStimme)); }
    public string GeminiFlashStimme { get => _geminiFlashStimme; set => Merke(ref _geminiFlashStimme, value, "vl_gemini_flash_stimme", nameof(GeminiFlashStimme)); }
    public string GeminiLiteStimme { get => _geminiLiteStimme; set => Merke(ref _geminiLiteStimme, value, "vl_gemini_lite_stimme", nameof(GeminiLiteStimme)); }
    public string AzureStimme { get => _azureStimme; set => Merke(ref _azureStimme, value, "vl_azure_stimme", nameof(AzureStimme)); }
    public string QwenStimme { get => _qwenStimme; set => Merke(ref _qwenStimme, value, "vl_qwen_stimme", nameof(QwenStimme)); }
    /// <summary>Kennung der gewählten geklonten Stimme (`klonStimme` in Swift).</summary>
    public string KlonStimmeId { get => _klonStimme; set => Merke(ref _klonStimme, value, "vl_klon_stimme", nameof(KlonStimmeId)); }
    public string GeminiWahl { get => _geminiWahl; set => Merke(ref _geminiWahl, value, "vl_gemini_wahl", nameof(GeminiWahl)); }

    /// <summary>Kennungen aus VorleseKatalog.RueckfallStufen in ihrer Reihenfolge. Änderungen speichern sich selbst.</summary>
    public ObservableCollection<string> Kette { get; }
    /// <summary>Nur über FavoritUmschalten ändern.</summary>
    public HashSet<string> Favoriten { get; }
    private bool _immerDeutsch;
    public bool ImmerDeutsch
    {
        get => _immerDeutsch;
        set
        {
            if (!Setze(ref _immerDeutsch, value)) return;
            Vorgaben.Setze("vl_immer_deutsch", value);
            Logbuch.Shared.Einstellung("vl_immer_deutsch", value ? "true" : "false", "Einstellungen/Stimme");
        }
    }
    public ObservableCollection<KlonStimme> KlonStimmen { get; } = new();
    private string? _klonFehler;
    public string? KlonFehler { get => _klonFehler; set => Setze(ref _klonFehler, value); }
    private readonly Dictionary<string, double> tempoJe = new();

    private static List<string> Liste(string k) => Vorgaben.Objekt(k) switch
    {
        List<object?> l => l.OfType<string>().ToList(),
        IEnumerable<string> l => l.ToList(),
        _ => new List<string>(),
    };

    private VorleseEinstellungen()
    {
        var alt = Einstellungen.Shared;
        _zugang = VorleseZugang.Laden();
        var vorgabeAnbieter = alt.Stimme == "system" ? VorleseAnbieter.System
            : (alt.Zugang.Google.Length > 0 ? VorleseAnbieter.Google : VorleseAnbieter.Edge);
        _anbieter = VorleseAnbieter.Aus(Vorgaben.Text("vl_anbieter")) ?? vorgabeAnbieter;
        _edgeStimme = Vorgaben.Text("vl_edge_stimme") ?? VorleseKatalog.StandardEdge;
        _googleStimme = Vorgaben.Text("vl_google_stimme") ?? (alt.Stimme.Contains("Chirp3") ? alt.Stimme : VorleseKatalog.StandardGoogle);
        _geminiFlashStimme = Vorgaben.Text("vl_gemini_flash_stimme") ?? VorleseKatalog.StandardGeminiFlash;
        _geminiLiteStimme = Vorgaben.Text("vl_gemini_lite_stimme") ?? VorleseKatalog.StandardGeminiLite;
        _azureStimme = Vorgaben.Text("vl_azure_stimme") ?? VorleseKatalog.StandardAzure;
        _qwenStimme = Vorgaben.Text("vl_qwen_stimme") ?? VorleseKatalog.StandardQwen;
        _klonStimme = Vorgaben.Text("vl_klon_stimme") ?? "";
        _geminiWahl = Vorgaben.Text("vl_gemini_wahl") ?? GeminiFreiDannBezahlt;
        Kette = new ObservableCollection<string>(Liste("vl_kette").Where(id => VorleseKatalog.RueckfallStufeZu(id) != null));
        Kette.CollectionChanged += (_, _) =>
        {
            var kopie = Kette.ToList();
            Vorgaben.Setze("vl_kette", kopie);
            Logbuch.Shared.Einstellung("vl_kette", string.Join(", ", kopie), "Einstellungen/Stimme");
            Melde(nameof(Kette));
        };
        Favoriten = new HashSet<string>(Liste("vl_favoriten"));
        _immerDeutsch = Vorgaben.Wahr("vl_immer_deutsch", true);
        if (Vorgaben.Objekt("vl_tempo") is JSONObjekt o)
            foreach (var (k, v) in o)
                if (v is long l) tempoJe[k] = l;
                else if (v is double d) tempoJe[k] = d;
    }

    // MARK: Stimme je Anbieter

    public string Stimme(VorleseAnbieter a) => a switch
    {
        VorleseAnbieter.Edge => EdgeStimme,
        VorleseAnbieter.Google => GoogleStimme,
        VorleseAnbieter.GeminiFlash => GeminiFlashStimme,
        VorleseAnbieter.GeminiLite => GeminiLiteStimme,
        VorleseAnbieter.Azure => AzureStimme,
        VorleseAnbieter.Qwen => QwenStimme,
        VorleseAnbieter.QwenKlon => KlonStimmeId,
        _ => "system",
    };

    public void SetzeStimme(string id, VorleseAnbieter fuer)
    {
        switch (fuer)
        {
            case VorleseAnbieter.Edge: EdgeStimme = id; break;
            case VorleseAnbieter.Google: GoogleStimme = id; break;
            case VorleseAnbieter.GeminiFlash: GeminiFlashStimme = id; break;
            case VorleseAnbieter.GeminiLite: GeminiLiteStimme = id; break;
            case VorleseAnbieter.Azure: AzureStimme = id; break;
            case VorleseAnbieter.Qwen: QwenStimme = id; break;
            case VorleseAnbieter.QwenKlon: KlonStimmeId = id; break;
        }
    }

    /// <summary>Stimmen eines Anbieters, Favoriten oben.</summary>
    public List<VorleseStimme> StimmenListe(VorleseAnbieter a)
    {
        var alle = a == VorleseAnbieter.QwenKlon
            ? KlonStimmen.Select(k => new VorleseStimme(k.Id, k.Name, Geschlecht.Maennlich)).ToList()
            : VorleseKatalog.Stimmen(a).ToList();
        return alle.Where(s => Favoriten.Contains(s.Id)).Concat(alle.Where(s => !Favoriten.Contains(s.Id))).ToList();
    }

    public void FavoritUmschalten(string id)
    {
        if (!Favoriten.Remove(id)) Favoriten.Add(id);
        Vorgaben.Setze("vl_favoriten", Favoriten.ToList());
        Melde(nameof(Favoriten));
    }

    // MARK: Tempo je Anbieter (Gemini startet bei 1,0 wie am Handy)

    public double Tempo(VorleseAnbieter a)
    {
        var vorgabe = a.IstGemini ? 1.0 : Einstellungen.Shared.Tempo;
        return Math.Min(Math.Max(tempoJe.TryGetValue(a.RawValue, out var t) ? t : vorgabe, TempoMin), TempoMax);
    }

    public void SetzeTempo(double wert, VorleseAnbieter fuer)
    {
        tempoJe[fuer.RawValue] = Math.Min(Math.Max(wert, TempoMin), TempoMax);
        var kopie = new JSONObjekt();
        foreach (var (k, v) in tempoJe) kopie[k] = v;
        Vorgaben.Setze("vl_tempo", kopie);
        Logbuch.Shared.Einstellung("vl_tempo", JSON.Text(kopie), "Einstellungen/Stimme");
        Melde(nameof(Tempo));
    }

    // MARK: Rückfallkette

    /// <summary>Kann eine Stufe überhaupt laufen (Schlüssel bzw. geklonte Stimme hinterlegt)?</summary>
    public bool Bereit(RueckfallStufe s) => s.Anbieter switch
    {
        VorleseAnbieter.GeminiFlash or VorleseAnbieter.GeminiLite => (s.Schluessel == RueckfallStufe.Frei ? _zugang.GeminiFrei : _zugang.GeminiBezahlt).Length > 0,
        VorleseAnbieter.Google => _zugang.Google.Length > 0,
        VorleseAnbieter.Azure => _zugang.Azure.Length > 0,
        VorleseAnbieter.Qwen => _zugang.Qwen.Length > 0,
        VorleseAnbieter.QwenKlon => _zugang.Qwen.Length > 0 && KlonStimmeId.Length > 0,
        _ => true,
    };

    public void StufeUmschalten(string id)
    {
        if (!Kette.Remove(id)) Kette.Add(id);
    }

    public void StufeVerschieben(string id, int um)
    {
        var i = Kette.IndexOf(id);
        if (i < 0) return;
        var j = Math.Min(Math.Max(i + um, 0), Kette.Count - 1);
        if (i == j) return;
        Kette.Move(i, j);
    }

    /// <summary>Abzug für einen Vorleseauftrag; `sonderStimme` (z. B. Hörprobe) bestimmt dann auch den Anbieter.</summary>
    public VorleseStand Stand(string? sonderStimme = null)
    {
        var haupt = Anbieter;
        var stimmen = new Dictionary<VorleseAnbieter, string>();
        var tempi = new Dictionary<VorleseAnbieter, double>();
        foreach (var a in VorleseAnbieter.AllCases) { stimmen[a] = Stimme(a); tempi[a] = Tempo(a); }
        if (sonderStimme != null && VorleseKatalog.AnbieterZu(sonderStimme) is VorleseAnbieter sa) { haupt = sa; stimmen[sa] = sonderStimme; }
        var stufen = Kette.Select(VorleseKatalog.RueckfallStufeZu).Where(s => s != null).Select(s => s!)
            .Where(s => !(s.Anbieter == haupt && s.Schluessel == null))
            .Where(Bereit).ToList();
        return new VorleseStand
        {
            Anbieter = haupt, StimmenJe = stimmen, TempoJe = tempi, Zugang = _zugang.Kopie(), GeminiWahl = GeminiWahl,
            Kette = stufen, ImmerDeutsch = ImmerDeutsch,
        };
    }

    // MARK: Geklonte Stimmen aus dem Alibaba-Konto

    public Task KlonStimmenLaden() => Haupt.Auf(async () =>
    {
        KlonFehler = null;
        var schluessel = new string(_zugang.Qwen.Where(c => !char.IsWhiteSpace(c)).ToArray());
        if (schluessel.Length == 0) { KlonStimmen.Clear(); Melde(nameof(KlonStimmen)); return; }
        try
        {
            using var req = new HttpRequestMessage(HttpMethod.Post, "https://dashscope-intl.aliyuncs.com/api/v1/services/audio/tts/customization");
            req.Headers.TryAddWithoutValidation("Authorization", $"Bearer {schluessel}");
            req.Content = new ByteArrayContent(JSON.Daten(new JSONObjekt
            {
                ["model"] = "qwen-voice-enrollment",
                ["input"] = new JSONObjekt { ["action"] = "list" },
            }));
            req.Content.Headers.TryAddWithoutValidation("Content-Type", "application/json; charset=utf-8");
            using var frist = new CancellationTokenSource(TimeSpan.FromSeconds(30));
            using var antwort = await SprachHttp.Client.SendAsync(req, frist.Token);
            var code = (int)antwort.StatusCode;
            var daten = await antwort.Content.ReadAsByteArrayAsync(frist.Token);
            if (code != 200)
            {
                KlonFehler = code == 401 || code == 403 ? $"Der DashScope-Schlüssel wurde abgelehnt (Fehler {code})."
                    : code == 429 ? "Das Kontingent bei Alibaba ist gerade erschöpft (Fehler 429)." : $"Alibaba antwortete mit Fehler {code}.";
                return;
            }
            var liste = JSON.Objekt(daten)?.Obj("output")?.Objekte("voice_list") ?? new List<JSONObjekt>();
            KlonStimmen.Clear();
            foreach (var e in liste)
            {
                var id = e.Text("voice");
                if (string.IsNullOrEmpty(id)) continue;
                KlonStimmen.Add(new KlonStimme(id, VorleseKatalog.KlonName(id), e.Text("gmt_create") ?? ""));
            }
            Melde(nameof(KlonStimmen));
            if (KlonStimmeId.Length == 0 && KlonStimmen.Count > 0) KlonStimmeId = KlonStimmen[0].Id;
        }
        catch (Exception ex)
        {
            KlonFehler = $"Alibaba nicht erreichbar: {ex.Message}";
        }
    });
}
