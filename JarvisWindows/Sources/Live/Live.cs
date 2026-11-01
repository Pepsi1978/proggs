using System.Collections.ObjectModel;
using System.Text;
using System.Windows.Threading;

namespace JarvisWindows;

// SCHNITTSTELLE (fest): LiveZeile, LiveGespraech (Zustand, PegelEin, PegelAus, Mitschrift, Fehler, Aktiv, Starten(g), Beenden()).

public sealed record LiveZeile(bool VonFrank, string Text)
{
    public Guid Id { get; } = Guid.NewGuid();
}

/// <summary>LiveGespraech.Zustand der Mac-Fassung. C# erlaubt keinen geschachtelten Typ mit dem Namen der Eigenschaft Zustand.</summary>
public enum LiveZustand { Aus, Verbindet, Hoert, Spricht, Denkt }

/// <summary>Live-Gespräch: gleichzeitig zuhören und sprechen (OpenAI GPT-Live/Realtime oder Gemini Live).
/// Sachfragen und Aktionen gehen an den JarvisAgent (gleiches Gehirn mit Handy- und PC-Werkzeugen, gleiches Gedächtnis).
/// Der Live-Ton hat Vorrang: Ton und Socket laufen in eigenen Fäden hoher Priorität, die Oberfläche liest nur Pegel.</summary>
public sealed class LiveGespraech : Beobachtbar
{
    public static LiveGespraech Shared { get; } = new();

    private LiveZustand _zustand = LiveZustand.Aus;
    public LiveZustand Zustand
    {
        get => _zustand;
        private set
        {
            if (Setze(ref _zustand, value))
            {
                Melde(nameof(Aktiv));
                AppSteuerung.Shared.PilleAbgleichen();
            }
        }
    }
    private float _pegelEin;
    public float PegelEin { get => _pegelEin; private set => Setze(ref _pegelEin, value); }
    private float _pegelAus;
    public float PegelAus { get => _pegelAus; private set => Setze(ref _pegelAus, value); }
    public ObservableCollection<LiveZeile> Mitschrift { get; } = new();
    private string? _fehler;
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }
    private string _beschreibung = "";
    /// <summary>Anbieter und Modell der laufenden Sitzung (für die Anzeige).</summary>
    public string Beschreibung { get => _beschreibung; private set => Setze(ref _beschreibung, value); }
    public bool Aktiv => Zustand != LiveZustand.Aus;

    private LiveSitzung? sitzung;
    private LiveTon? ton;
    private Gespraech? gespraech;
    private DispatcherTimer? zeitgeber;
    private CancellationTokenSource? taktAbbruch;
    private Bruecke? bruecke;
    private int offeneAuftraege;
    private Task? auftragsKette;
    private DateTime letzteAktivitaet = DateTime.UtcNow;
    /// <summary>Stille-Ende dieser Sitzung in Minuten, beim Start gelesen wie am Handy; 0 = nie.</summary>
    private int stilleMinuten;
    /// <summary>Kommt die Verbindung so lange nicht zustande, endet das Gespräch auch bei „Nie“.</summary>
    private const double VerbindenMaxSek = 180;
    /// <summary>Ab diesem Mikrofonpegel spricht jemand: Eine lange Rede zählt nicht als Stille, auch wenn die Mitschrift ausbleibt.</summary>
    private const float StimmPegel = 0.1f;
    /// <summary>GPT-Live/Realtime: nicht delegierte Beiträge (Smalltalk) kommen ins Gespräch; delegierte schreibt der Agent selbst.</summary>
    private readonly List<Nachricht> ausstehend = new();
    private int naechsteJarvisVomAgenten;

    private LiveGespraech() { }

    /// <summary>Leitet die Rückmeldungen aus dem Socket-Faden auf den Hauptfaden. Ziel wird beim Beenden geleert (in Swift eine schwache Referenz):
    /// Späte Meldungen der alten Sitzung dürfen eine neue nicht beenden.</summary>
    private sealed class Bruecke : LiveSitzungsDelegate
    {
        public volatile LiveGespraech? Ziel;
        public Bruecke(LiveGespraech z) { Ziel = z; }
        public void LiveBereit() => Haupt.Post(() => Ziel?.BeiBereit());
        public void LiveNutzerText(string text) => Haupt.Post(() => Ziel?.BeiNutzer(text));
        public void LiveJarvisText(string text) => Haupt.Post(() => Ziel?.BeiJarvis(text));
        public void LiveWerkzeug(string kennung, string auftrag, string hinweis) => Haupt.Post(() => Ziel?.BeiWerkzeug(kennung, auftrag, hinweis));
        public void LiveFehler(string text) => Haupt.Post(() => Ziel?.BeiFehler(text));
        public void LiveEnde(string? grund) => Haupt.Post(() => Ziel?.Beenden());
    }

    // MARK: Start / Ende

    public void Starten(Gespraech g) => Haupt.Post(() =>
    {
        if (Aktiv) return;
        Fehler = null;
        Mitschrift.Clear();
        ausstehend.Clear();
        naechsteJarvisVomAgenten = 0;
        offeneAuftraege = 0;
        auftragsKette = null;   // Eine neue Sitzung wartet nicht auf die Frage einer alten.
        gespraech = g;
        Zustand = LiveZustand.Verbindet;
        Stimme.Shared.Stopp();
        if (SprachSchleife.Shared.Zustand != SprachZustand.Aus) SprachSchleife.Shared.Beenden();
        Weckwort.Shared.Pausieren();
        _ = Aufbauen(g);
    });

    private async Task Aufbauen(Gespraech g)
    {
        if (Zustand != LiveZustand.Verbindet) return;
        if (!LiveTon.MikrofonErlaubt())
        {
            Abbrechen("Jarvis darf das Mikrofon nicht nutzen. Bitte in Einstellungen → Datenschutz und Sicherheit → Mikrofon erlauben.");
            return;
        }
        var e = Einstellungen.Shared;
        var gemini = e.LiveAnbieter == "gemini";
        var neuerTon = new LiveTon(eingabeRate: gemini ? 16_000 : 24_000, blockMs: gemini ? 40 : 100);
        LiveSitzung neu;
        if (gemini)
        {
            if (e.Zugang.Gemini.Length == 0) { neuerTon.Stoppen(); Abbrechen("Kein Gemini-Schlüssel eingetragen (Einstellungen → Konten)."); return; }
            var modell = LiveOptionen.GeminiModell(e.LiveModellGemini);
            var stimme = LiveOptionen.GeminiStimme(e.LiveStimmeGemini);
            neu = new LiveGemini(neuerTon, e.Zugang.Gemini, modell, stimme);
            Beschreibung = (LiveOptionen.GeminiModelle.FirstOrDefault(m => m.Id == modell)?.Name ?? modell) + " · " + stimme;
        }
        else
        {
            if (e.Zugang.Openai.Length == 0) { neuerTon.Stoppen(); Abbrechen("Kein OpenAI-Schlüssel eingetragen (Einstellungen → Konten)."); return; }
            var modell = LiveOptionen.OpenAIModell(e.LiveModellOpenAI);
            var stimme = LiveOptionen.OpenAIStimme(e.LiveStimmeOpenAI, modell);
            neu = new LiveOpenAI(neuerTon, e.Zugang.Openai, modell, stimme);
            Beschreibung = (LiveOptionen.OpenAIModelle.FirstOrDefault(m => m.Id == modell)?.Name ?? modell) + " · " + stimme;
        }
        var b = new Bruecke(this);
        bruecke = b;
        neu.Delegate = b;
        neuerTon.BeiMikro = pcm => neu.Mikro(pcm);
        neuerTon.BeiAusfall = text => Haupt.Post(() =>
        {
            Fehler = text;
            Log("live", text);
            Beenden();
        });
        ton = neuerTon;
        sitzung = neu;
        Log("live", $"Live-Gespräch beginnt: {Beschreibung}");
        letzteAktivitaet = DateTime.UtcNow;
        stilleMinuten = e.LiveStilleMinuten;
        // Nur Anzeige und Stille-Ende hängen am Hauptfaden.
        zeitgeber = new DispatcherTimer(TimeSpan.FromMilliseconds(100), DispatcherPriority.Normal, (_, _) => Takt(), Dispatcher.CurrentDispatcher);
        zeitgeber.Start();
        SitzungsTaktStarten(neu);
        // Socket und Ton zugleich: Beide sind unabhängig, und so zählt nur der längere Weg. Ton, der vorher ankommt, wartet im Puffer.
        neu.Starten();
        try { await neuerTon.Starten(); }
        catch (UnauthorizedAccessException)
        {
            AufbauGescheitert("Jarvis darf das Mikrofon nicht nutzen. Bitte in Einstellungen → Datenschutz und Sicherheit → Mikrofon erlauben.", neuerTon, neu);
        }
        catch (Exception ex)
        {
            AufbauGescheitert($"Mikrofon oder Lautsprecher lässt sich nicht starten: {ex.Message}", neuerTon, neu);
        }
    }

    /// <summary>Beitragsenden und die Jarvis-Weiche laufen im eigenen Faden: Rendert oder blockiert die Oberfläche (Fensterzug, Ablage), rutscht die Übergabe an Jarvis nicht nach hinten.</summary>
    private void SitzungsTaktStarten(LiveSitzung s)
    {
        var a = new CancellationTokenSource();
        taktAbbruch = a;
        new Thread(() =>
        {
            while (!a.IsCancellationRequested)
            {
                try { s.Takt(); }
                catch (Exception ex) { Log("fehler", "Live: Takt der Sitzung fehlgeschlagen", fehler: ex.Message); }
                a.Token.WaitHandle.WaitOne(50);
            }
        }) { IsBackground = true, Priority = ThreadPriority.AboveNormal, Name = "jarvis.live.takt" }.Start();
    }

    /// <summary>Der Tonstart ist gescheitert, der Socket läuft womöglich schon. Hat Frank inzwischen beendet, ist alles schon abgebaut.</summary>
    private void AufbauGescheitert(string text, LiveTon t, LiveSitzung s)
    {
        if (!ReferenceEquals(ton, t)) return;
        zeitgeber?.Stop(); zeitgeber = null;
        taktAbbruch?.Cancel(); taktAbbruch = null;
        s.Beenden();
        t.Stoppen();
        sitzung = null; ton = null;
        Abbrechen(text);
    }

    private void Abbrechen(string text)
    {
        Fehler = text;
        Log("live", text);
        if (bruecke != null) { bruecke.Ziel = null; bruecke = null; }
        Zustand = LiveZustand.Aus;
        Weckwort.Shared.Fortsetzen();
    }

    public void Beenden()
    {
        Haupt.Post(() =>
        {
            if (!Aktiv) return;
            zeitgeber?.Stop(); zeitgeber = null;
            taktAbbruch?.Cancel(); taktAbbruch = null;
            sitzung?.Beenden();
            ton?.Stoppen();
            sitzung = null; ton = null;
            if (bruecke != null) { bruecke.Ziel = null; bruecke = null; }
            AusstehendSchreiben();
            gespraech?.Speichern();
            Zustand = LiveZustand.Aus;
            PegelEin = 0; PegelAus = 0;
            Log("live", "Live-Gespräch beendet");
            Weckwort.Shared.Fortsetzen();
        });
    }

    /// <summary>Beendet nach kurzer Pause, aber nur dieselbe Sitzung: Frank kann inzwischen beendet und neu gestartet haben.</summary>
    private async void BeendenNach(double sekunden)
    {
        var s = sitzung;
        await Task.Delay(TimeSpan.FromSeconds(sekunden));
        if (ReferenceEquals(sitzung, s)) Beenden();
    }

    /// <summary>Frank tippt zum Unterbrechen (Jarvis verstummt sofort).</summary>
    public void Unterbrechen() => sitzung?.Angetippt();

    // MARK: Anzeige und Stille-Ende (100 ms, Hauptfaden)

    private void Takt()
    {
        var t = ton; var s = sitzung;
        if (t == null || s == null) return;
        PegelEin = t.PegelEin;
        PegelAus = t.PegelAus;
        var spricht = t.SprichtHoerbar;
        var denkt = offeneAuftraege > 0 || ((s as LiveGemini)?.WeicheArbeitet ?? false);
        if (spricht || denkt || (s.Bereit && PegelEin >= StimmPegel)) letzteAktivitaet = DateTime.UtcNow;
        var neu = !s.Bereit || !t.Laeuft ? LiveZustand.Verbindet : spricht ? LiveZustand.Spricht : denkt ? LiveZustand.Denkt : LiveZustand.Hoert;
        if (neu != Zustand) Zustand = neu;
        var ruhe = (DateTime.UtcNow - letzteAktivitaet).TotalSeconds;
        if (!s.Bereit && ruhe > VerbindenMaxSek)
        {
            Fehler = "Live-Gespräch beendet: Die Verbindung kam nicht zustande.";
            Log("live", "Verbindung kam nicht zustande: Live-Gespräch endet");
            Beenden();
        }
        else if (stilleMinuten > 0 && ruhe > stilleMinuten * 60.0)
        {
            Fehler = $"Live-Gespräch nach {stilleMinuten} Minuten Stille beendet.";
            Log("live", $"{stilleMinuten} Minuten Stille: Live-Gespräch endet");
            Beenden();
        }
    }

    // MARK: Rückmeldungen der Sitzung

    private void BeiBereit()
    {
        letzteAktivitaet = DateTime.UtcNow;
        if (Zustand == LiveZustand.Verbindet && ton?.Laeuft == true) Zustand = LiveZustand.Hoert;
    }

    private void BeiNutzer(string text)
    {
        letzteAktivitaet = DateTime.UtcNow;
        Mitschrift.Add(new LiveZeile(true, text));
        // Vorheriger Zug abgeschlossen (Antwort da): ins Gespräch.
        if (ausstehend.Any(n => n.Rolle == Rolle.Jarvis)) AusstehendSchreiben();
        if (!(sitzung?.IstGemini ?? false)) ausstehend.Add(new Nachricht(Rolle.Frank, text, gesprochen: true));
        if (LiveOptionen.IstBeenden(text))
        {
            Log("live", "Frank beendet das Gespräch per Sprache");
            BeendenNach(1.8);
        }
    }

    private void BeiJarvis(string text)
    {
        letzteAktivitaet = DateTime.UtcNow;
        Mitschrift.Add(new LiveZeile(false, text));
        if (sitzung?.IstGemini ?? false) return;
        if (naechsteJarvisVomAgenten > 0)
        {
            // Gesprochene Fassung von Jarvis' Antwort: steht über den Agenten schon im Gespräch.
            naechsteJarvisVomAgenten -= 1;
            ausstehend.Clear();
            return;
        }
        ausstehend.Add(new Nachricht(Rolle.Jarvis, text, gesprochen: true));
    }

    private void AusstehendSchreiben()
    {
        var g = gespraech;
        if (g == null || ausstehend.Count == 0) { ausstehend.Clear(); return; }
        foreach (var n in ausstehend) g.Nachrichten.Add(n);
        ausstehend.Clear();
        g.Speichern();
    }

    private void BeiFehler(string text)
    {
        Fehler = text;
        Log("live", $"Fehler: {text}");
    }

    private void BeiWerkzeug(string kennung, string auftrag, string hinweis)
    {
        var g = gespraech; var s = sitzung;
        if (g == null || s == null) return;
        letzteAktivitaet = DateTime.UtcNow;
        // Der Agent schreibt Frage und Antwort selbst ins Gespräch: Franks ausstehende Worte und Füllsätze entfallen.
        ausstehend.Clear();
        var worte = auftrag.Trim();
        if (LiveOptionen.IstBeenden(worte))
        {
            s.Ergebnis(kennung, "Bis später, Frank.");
            BeendenNach(2.5);
            return;
        }
        if (worte.Length == 0)
        {
            s.Ergebnis(kennung, "Frank hat noch nichts gesagt. Sag nichts weiter.");
            return;
        }
        offeneAuftraege += 1;
        naechsteJarvisVomAgenten = 0;
        var frage = hinweis.Length == 0 ? worte : worte + "\n" + hinweis;
        var vorher = auftragsKette;
        var hoheStufe = Einstellungen.Shared.Effort is "high" or "xhigh" or "max";
        auftragsKette = AuftragAusfuehren(vorher, frage, g, s, kennung, hoheStufe);
    }

    private async Task AuftragAusfuehren(Task? vorher, string frage, Gespraech g, LiveSitzung s, string kennung, bool hoheStufe)
    {
        // Immer nur eine Anfrage gleichzeitig an dasselbe Gespräch. Ein Fehler der vorigen betrifft diese nicht.
        if (vorher != null)
        {
            try { await vorher; }
            catch (Exception ex) { Log("fehler", "Live: voriger Auftrag fehlgeschlagen", fehler: ex.Message); }
        }
        var fertig = false;
        var zwischenstand = false;
        var gestreamt = false;
        var strom = new StringBuilder();
        var gesendet = 0;
        // Jede Anfrage endet irgendwann: Sonst stünden alle folgenden, und das Stille-Ende griffe nie.
        using var frist = new CancellationTokenSource(TimeSpan.FromMinutes(10));
        Action<string>? stroemen = null;
        if (s.KannStroemen) stroemen = stueck =>
        {
            lock (strom)
            {
                strom.Append(stueck);
                if (!ReferenceEquals(sitzung, s)) return;
                while (SatzEnde(strom, gesendet) is var ende && ende > 0)
                {
                    var satz = strom.ToString(gesendet, ende - gesendet).Trim();
                    gesendet = ende;
                    if (satz.Length > 0 && s.Stueck(kennung, satz))
                    {
                        // Der erste Satz spricht, während das Gehirn noch schreibt. Spart die ganze Schreibzeit bis zum ersten Ton.
                        gestreamt = true;
                        naechsteJarvisVomAgenten += 1;
                    }
                }
            }
        };
        using var wacheAbbruch = new CancellationTokenSource();
        _ = Wache();
        async Task Wache()
        {
            try { await Task.Delay(TimeSpan.FromSeconds(hoheStufe ? 120 : 60), wacheAbbruch.Token); }
            catch (OperationCanceledException) { return; }
            // Nur die eigene Sitzung: Die Kennung gilt nirgends sonst. Spricht Jarvis schon, braucht es keinen Zwischenstand.
            if (fertig || gestreamt || !ReferenceEquals(sitzung, s)) return;
            // Braucht Jarvis länger: Zwischenstand, die Arbeit läuft weiter und wird nachgereicht.
            zwischenstand = true;
            s.Ergebnis(kennung, LiveOptionen.DauertNoch);
            Log("live", "Jarvis braucht länger: Zwischenstand an das Live-Modell");
        }
        string antwort;
        try { antwort = await JarvisAgent.Shared.Frage(frage, g, gesprochen: true, onText: stroemen, ct: frist.Token); }
        catch (Exception ex)
        {
            Log("fehler", "Live: Jarvis-Anfrage fehlgeschlagen", fehler: ex.Message);
            antwort = "";
        }
        fertig = true;
        wacheAbbruch.Cancel();
        // Spät fertig in einer neuen Sitzung: Deren Zähler und Stille-Ende gehen diesen Auftrag nichts an.
        var eigeneSitzung = ReferenceEquals(sitzung, s) && Aktiv;
        if (eigeneSitzung)
        {
            offeneAuftraege = Math.Max(0, offeneAuftraege - 1);
            letzteAktivitaet = DateTime.UtcNow;
        }
        var text = antwort.Length == 0 ? "Jarvis hat keine Antwort geliefert." : antwort;
        var beendenGewuenscht = g.Nachrichten.LastOrDefault(n => n.Rolle == Rolle.Jarvis)?.Schritte.Any(x => x.Werkzeug == "sprachsession_beenden") ?? false;
        try
        {
            if (eigeneSitzung)
            {
                string? rest = null;
                lock (strom) if (gestreamt) rest = RestNachStrom(strom.ToString(gesendet, strom.Length - gesendet), antwort);
                if (rest != null)
                {
                    if (rest.Length > 0)
                    {
                        naechsteJarvisVomAgenten += 1;
                        if (!s.Stueck(kennung, rest)) await Stimme.Shared.Sprich(rest);
                    }
                }
                else
                {
                    naechsteJarvisVomAgenten += 1;
                    var ok = zwischenstand ? s.Nachreichen(kennung, text) : s.Ergebnis(kennung, text);
                    if (!ok) await Stimme.Shared.Sprich(text);
                }
            }
            else if (zwischenstand)
            {
                // Gespräch inzwischen beendet: Ergebnis vorlesen (steht auch im Chat).
                await Stimme.Shared.Sprich("Frank, die Antwort ist da. " + text);
            }
        }
        catch (Exception ex) { Log("fehler", "Live: Antwort nicht übergeben", fehler: ex.Message); }
        if (beendenGewuenscht)
        {
            await Task.Delay(TimeSpan.FromSeconds(4));
            if (ReferenceEquals(sitzung, s)) Beenden();
        }
    }

    /// <summary>Was nach dem Strom noch zu sagen ist: der angefangene letzte Satz, und ein Fehlerhinweis, der nur im fertigen Text steht.</summary>
    private static string RestNachStrom(string offen, string antwort)
    {
        var rest = offen.Trim();
        var letzter = antwort.Split("\n\n").LastOrDefault()?.Trim() ?? "";
        return letzter.StartsWith('⚠') || letzter.StartsWith("(abgebrochen") || letzter.StartsWith("(Schrittlimit")
            ? (rest + " " + letzter).Trim()
            : rest;
    }

    /// <summary>Ende des nächsten vollständigen Satzes ab start, sonst 0. Nicht hinter Zahlen und kurzen Kürzeln („3. Mai“, „z. B.“), und nicht zu kurz: GPT-Live formuliert jeden Kommentar für sich.</summary>
    private static int SatzEnde(StringBuilder t, int start)
    {
        const int MinZeichen = 15;
        for (var i = start; i < t.Length - 1; i++)
        {
            var c = t[i];
            if (c == '\n' && i - start >= MinZeichen) return i + 1;
            if (c is not ('.' or '!' or '?' or '…') || !char.IsWhiteSpace(t[i + 1]) || i - start < MinZeichen) continue;
            if (c == '.')
            {
                var w = i;
                while (w > start && !char.IsWhiteSpace(t[w - 1])) w--;
                var wort = i - w;
                if (wort <= 3 || char.IsDigit(t[i - 1])) continue;
            }
            return i + 1;
        }
        return 0;
    }
}
