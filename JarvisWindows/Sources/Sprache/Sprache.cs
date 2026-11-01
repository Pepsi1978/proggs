using System.Globalization;
using System.Speech.Synthesis;
using System.Windows.Threading;
using Microsoft.Win32;
using NAudio.CoreAudioApi;
using NAudio.Wave;
using NAudio.Wave.SampleProviders;

namespace JarvisWindows;

// SCHNITTSTELLE (fest): Oberfläche und Live nutzen genau diese Typen und Mitglieder.

// MARK: - Stimme (Vorlesen)

/// <summary>Sprachausgabe mit allen Anbietern der Android-App (Edge, Chirp 3 HD, Gemini 3.8 Flash/Lite, Azure, Alibaba, eigene
/// Stimme) satzweise und lückenlos, Rückfallkette wie am Handy, zuletzt die beste deutsche Systemstimme. Während des Sprechens ruht das Weckwort (kein Selbstauslösen).</summary>
public sealed class Stimme : Beobachtbar
{
    public static Stimme Shared { get; } = new();
    private bool _spricht;
    public bool Spricht { get => _spricht; private set => Setze(ref _spricht, value); }
    private float _pegel;
    /// <summary>0…1 für die Animation.</summary>
    public float Pegel { get => _pegel; private set => Setze(ref _pegel, value); }

    private int generation;
    private SpeechSynthesizer? system;
    private TaskCompletionSource? systemFertig;
    private DispatcherTimer? pegelUhr;

    // Wiedergabe: Das Gerät hört die Quelle dauerhaft ab, im Leerlauf ist sie stumm. Nach kurzer Ruhe wird es freigegeben,
    // sonst hielte ein offener Audiostrom den Rechner vom Energiesparen ab.
    private readonly object ausgabeSchloss = new();
    private WasapiOut? engine;
    private string? engineGeraet;
    private volatile bool engineLaeuft;
    private readonly SpielQuelle spieler = new();
    private CancellationTokenSource? freigabeUhr;
    private CancellationTokenSource? syntheseAbbruch;
    private const int LatenzMs = 100;

    /// <summary>Stimmen des gewählten Anbieters (Kennung, Anzeigename), Favoriten oben: für einfache Auswahllisten.</summary>
    public List<(string Id, string Name)> Stimmen
    {
        get
        {
            var v = VorleseEinstellungen.Shared;
            return v.StimmenListe(v.Anbieter).Select(st =>
                (st.Id, $"{(v.Favoriten.Contains(st.Id) ? "★ " : "")}{st.Name} ({(st.Geschlecht == Geschlecht.Weiblich ? "weiblich" : "männlich")}, {v.Anbieter.Kurz})")).ToList();
        }
    }

    private Stimme()
    {
        spieler.BeiPegel = wert => Haupt.Post(() =>
        {
            if (Spricht) Pegel = Pegel * 0.5f + wert * 0.5f;
        });
    }

    /// <summary>Hörprobe einer bestimmten Stimme (bestimmt auch den Anbieter).</summary>
    public Task Probe(string stimmeId)
    {
        var de = new CultureInfo("de-DE");
        var jetzt = DateTime.Now;
        return Sprich($"Hallo Frank, ich bin Jarvis. Heute ist {jetzt.ToString("dddd, d. MMMM", de)}, es ist {jetzt.ToString("HH:mm", de)} Uhr.", stimme: stimmeId);
    }

    /// <summary>Liest den Text vor und kehrt zurück, wenn fertig oder gestoppt. Mit der gewählten (oder einer Sonder-)Stimme;
    /// scheitert sie, Rückfallkette, dann Edge, zuletzt die Systemstimme.</summary>
    public Task Sprich(string text, string? stimme = null) => Haupt.Auf(() => SprichIntern(text, stimme));

    private async Task SprichIntern(string text, string? sonder)
    {
        StoppIntern();
        generation++;
        var meine = generation;
        var sauber = SprechText.FuerStimme(text);
        if (sauber.Length == 0) return;
        Weckwort.Shared.Pausieren();
        try
        {
            Spricht = true;
            freigabeUhr?.Cancel();
            try
            {
                var stand = VorleseEinstellungen.Shared.Stand(sonderStimme: sonder);
                if (stand.Anbieter == VorleseAnbieter.System)
                {
                    await SprichSystem(sauber, stand.Tempo(VorleseAnbieter.System), meine);
                    return;
                }
                var rest = await SprichKette(SprechText.Stuecke(sauber), stand, meine);
                if (generation != meine || rest.Count == 0) return;
                Log("fehler", "Vorlesen: Stimmen oder Wiedergabe gescheitert – Rückfall auf die Systemstimme");
                await SprichSystem(string.Join(" ", rest), stand.Tempo(VorleseAnbieter.System), meine);
            }
            finally
            {
                if (generation == meine)
                {
                    Spricht = false;
                    Pegel = 0;
                    PlaneFreigabe();
                }
            }
        }
        finally
        {
            Weckwort.Shared.Fortsetzen();
        }
    }

    public void Stopp() => Haupt.Post(StoppIntern);

    private void StoppIntern()
    {
        generation++;
        syntheseAbbruch?.Cancel();
        spieler.Leeren();
        var s = system;
        if (s != null) Task.Run(() => { try { s.SpeakAsyncCancelAll(); } catch { } });
        var k = systemFertig;
        systemFertig = null;
        k?.TrySetResult();
        pegelUhr?.Stop();
        pegelUhr = null;
        Spricht = false;
        Pegel = 0;
    }

    // MARK: Wiedergabe

    /// <summary>Die Quelle: 24 kHz mono. Liefert im Leerlauf Stille und misst den Pegel des wirklich Gespielten.</summary>
    private sealed class SpielQuelle : ISampleProvider
    {
        public WaveFormat WaveFormat { get; } = WaveFormat.CreateIeeeFloatWaveFormat(VorleseSynthese.ZielRate, 1);
        private readonly object schloss = new();
        private readonly Queue<float[]> warteschlange = new();
        private float[]? aktuell;
        private int pos;
        private long letzterPegel;
        public Action<float>? BeiPegel;

        /// <summary>Noch nicht zu Ende gespielte Stücke.</summary>
        public int Offen { get { lock (schloss) return warteschlange.Count + (aktuell != null ? 1 : 0); } }
        public void Plane(float[] puffer) { lock (schloss) warteschlange.Enqueue(puffer); }
        public void Leeren() { lock (schloss) { warteschlange.Clear(); aktuell = null; pos = 0; } }

        public int Read(float[] buffer, int offset, int count)
        {
            var geschrieben = 0;
            lock (schloss)
            {
                while (geschrieben < count)
                {
                    if (aktuell == null)
                    {
                        if (warteschlange.Count == 0) break;
                        aktuell = warteschlange.Dequeue();
                        pos = 0;
                    }
                    var n = Math.Min(count - geschrieben, aktuell.Length - pos);
                    Array.Copy(aktuell, pos, buffer, offset + geschrieben, n);
                    pos += n;
                    geschrieben += n;
                    if (pos >= aktuell.Length) aktuell = null;
                }
            }
            if (geschrieben > 0)
            {
                float summe = 0;
                for (int i = 0; i < geschrieben; i++) summe += buffer[offset + i] * buffer[offset + i];
                var jetzt = Environment.TickCount64;
                if (jetzt - letzterPegel >= 33)
                {
                    letzterPegel = jetzt;
                    BeiPegel?.Invoke(Math.Min(1f, MathF.Sqrt(summe / geschrieben) * 6));
                }
            }
            if (geschrieben < count) Array.Clear(buffer, offset + geschrieben, count - geschrieben);
            return count;
        }
    }

    /// <summary>Verteilt mono auf alle Kanäle des Geräts.</summary>
    private sealed class AufKanaele : ISampleProvider
    {
        private readonly ISampleProvider quelle;
        private readonly int kanaele;
        private float[] mono = Array.Empty<float>();
        public WaveFormat WaveFormat { get; }
        public AufKanaele(ISampleProvider quelle, int kanaele)
        {
            this.quelle = quelle;
            this.kanaele = kanaele;
            WaveFormat = WaveFormat.CreateIeeeFloatWaveFormat(quelle.WaveFormat.SampleRate, kanaele);
        }
        public int Read(float[] buffer, int offset, int count)
        {
            var n = count / kanaele;
            if (mono.Length < n) mono = new float[n];
            var gelesen = quelle.Read(mono, 0, n);
            for (int i = 0; i < gelesen; i++)
                for (int k = 0; k < kanaele; k++) buffer[offset + i * kanaele + k] = mono[i];
            return gelesen * kanaele;
        }
    }

    /// <summary>Immer im Hintergrund: Geräte öffnen kann dauern, und WASAPI verlangt einen MTA-Faden.
    /// Wechselt das Standardgerät, wird beim nächsten Vorlesen neu geöffnet.</summary>
    private void BereiteEngine()
    {
        lock (ausgabeSchloss)
        {
            using var en = new MMDeviceEnumerator();
            var geraet = en.GetDefaultAudioEndpoint(DataFlow.Render, Role.Console);
            if (engine != null && engineLaeuft && engineGeraet == geraet.ID) return;
            SchliesseEngine();
            var misch = geraet.AudioClient.MixFormat;
            ISampleProvider q = spieler;
            if (misch.SampleRate != q.WaveFormat.SampleRate) q = new WdlResamplingSampleProvider(q, misch.SampleRate);
            if (misch.Channels != 1) q = new AufKanaele(q, misch.Channels);
            var w = new WasapiOut(geraet, AudioClientShareMode.Shared, true, LatenzMs);
            w.PlaybackStopped += (_, a) =>
            {
                if (!ReferenceEquals(engine, w)) return;   // Eine geschlossene Ausgabe meldet ihr Ende verspätet.
                engineLaeuft = false;
                if (a.Exception != null) Log("sprache", $"Vorlesen: Audioausgabe beendet ({a.Exception.Message})");
            };
            w.Init(new SampleToWaveProvider(q));
            w.Play();
            engine = w;
            engineGeraet = geraet.ID;
            engineLaeuft = true;
        }
    }

    private void SchliesseEngine()
    {
        var w = engine;
        engine = null;
        engineGeraet = null;
        engineLaeuft = false;
        if (w == null) return;
        try { w.Stop(); } catch { }
        try { w.Dispose(); } catch { }
    }

    private void PlaneFreigabe()
    {
        freigabeUhr?.Cancel();
        var uhr = new CancellationTokenSource();
        freigabeUhr = uhr;
        _ = Task.Run(async () =>
        {
            try { await Task.Delay(TimeSpan.FromSeconds(30), uhr.Token).ConfigureAwait(false); }
            catch (OperationCanceledException) { return; }
            lock (ausgabeSchloss) SchliesseEngine();
        });
    }

    /// <summary>Erzeugt die Stücke (zwei im Voraus) und spielt sie lückenlos. Gibt die Stücke zurück, die keine Stimme erzeugen konnte.</summary>
    private async Task<List<string>> SprichKette(List<string> stuecke, VorleseStand stand, int meine)
    {
        try { await Task.Run(BereiteEngine); }
        catch (Exception ex)
        {
            Log("fehler", $"Vorlesen: Audioausgabe nicht startbar ({ex.Message})");
            return stuecke;
        }
        if (generation != meine) return new List<string>();
        syntheseAbbruch?.Cancel();
        var abbruch = new CancellationTokenSource();
        syntheseAbbruch = abbruch;
        var ct = abbruch.Token;
        var aufgaben = new Dictionary<int, Task<float[]?>>();
        void Sichere(int i)
        {
            if (i >= stuecke.Count || aufgaben.ContainsKey(i)) return;
            var text = stuecke[i];
            aufgaben[i] = Task.Run(async () =>
            {
                try
                {
                    var (ton, anbieter) = await VorleseSynthese.Synthetisiere(text, stand, ct).ConfigureAwait(false);
                    // Gemini kennt kein Sprechtempo: der fertige Ton wird gestreckt (Tonhöhe bleibt).
                    var strecken = anbieter.IstGemini ? stand.Tempo(anbieter) : 1;
                    return VorleseSynthese.Puffer(ton, strecken);
                }
                catch (OperationCanceledException) when (ct.IsCancellationRequested) { return null; }
                catch (Exception ex)
                {
                    Log("fehler", $"Vorlesen: {ex.Message}");
                    return null;
                }
            });
        }
        var rest = new List<string>();
        for (int i = 0; i < stuecke.Count; i++)
        {
            Sichere(i); Sichere(i + 1); Sichere(i + 2);
            var puffer = await aufgaben[i];
            if (generation != meine) { abbruch.Cancel(); return new List<string>(); }
            if (puffer == null)
            {
                rest = stuecke.Skip(i).ToList();
                abbruch.Cancel();
                break;
            }
            spieler.Plane(puffer);
        }
        // Warten, bis alles geplante gespielt ist.
        while (generation == meine && spieler.Offen > 0)
        {
            // Steht die Ausgabe (Gerätewechsel), wird nichts mehr abgeholt: nicht ewig warten.
            if (!engineLaeuft)
            {
                Log("fehler", "Vorlesen: Audioausgabe während der Wiedergabe gestoppt");
                spieler.Leeren();
                break;
            }
            await Task.Delay(40);
        }
        // Das zuletzt Abgeholte liegt noch im Gerätepuffer: nicht vorzeitig zum Mikrofon wechseln.
        if (generation == meine) await Task.Delay(LatenzMs + 40);
        return generation == meine ? rest : new List<string>();
    }

    // MARK: Systemstimme

    private static void WaehleBesteDeutscheStimme(SpeechSynthesizer s)
    {
        var deutsch = s.GetInstalledVoices().Where(v => v.Enabled && v.VoiceInfo.Culture.Name.StartsWith("de", StringComparison.OrdinalIgnoreCase))
            .OrderByDescending(v => v.VoiceInfo.Culture.Name == "de-DE" ? 1 : 0).FirstOrDefault();
        if (deutsch != null) s.SelectVoice(deutsch.VoiceInfo.Name);
    }

    private async Task SprichSystem(string text, double tempo, int meine)
    {
        if (generation != meine) return;
        // Der Systemstimme fehlt ein Pegel: sanft pulsierende Animation stattdessen.
        var start = DateTime.Now;
        pegelUhr?.Stop();
        var uhr = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(50) };
        uhr.Tick += (_, _) =>
        {
            if (!Spricht) return;
            var t = (DateTime.Now - start).TotalSeconds;
            Pegel = (float)(0.35 + 0.25 * Math.Sin(t * 9) + 0.15 * Math.Sin(t * 23));
        };
        pegelUhr = uhr;
        uhr.Start();
        var k = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        systemFertig = k;
        // SAPI: +10 ist dreifach, -10 ein Drittel des Normaltempos.
        var rate = Math.Clamp((int)Math.Round(10 * Math.Log(Math.Max(tempo, 0.1), 3)), -10, 10);
        _ = Task.Run(() =>
        {
            try
            {
                if (system == null)
                {
                    var neu = new SpeechSynthesizer();
                    neu.SetOutputToDefaultAudioDevice();
                    WaehleBesteDeutscheStimme(neu);
                    system = neu;
                }
                var s = system;
                void Fertig(object? _, SpeakCompletedEventArgs a)
                {
                    s.SpeakCompleted -= Fertig;
                    k.TrySetResult();
                }
                s.SpeakCompleted += Fertig;
                s.Rate = rate;
                if (k.Task.IsCompleted) { s.SpeakCompleted -= Fertig; return; }
                s.SpeakAsync(text);
            }
            catch (Exception ex)
            {
                Log("fehler", $"Vorlesen: Systemstimme nicht nutzbar ({ex.Message})");
                k.TrySetResult();
            }
        });
        await k.Task;
        if (ReferenceEquals(systemFertig, k)) systemFertig = null;
        uhr.Stop();
        if (ReferenceEquals(pegelUhr, uhr)) pegelUhr = null;
    }
}

// MARK: - Diktat

/// <summary>Spracheingabe mit Ende-Erkennung (Energie-VAD mit Rauschboden) und Transkription über Parakeet (offline), Groq oder die Windows-Erkennung.</summary>
public sealed class Diktat : Beobachtbar
{
    public static Diktat Shared { get; } = new();
    private bool _aktiv;
    public bool Aktiv { get => _aktiv; private set => Setze(ref _aktiv, value); }
    private float _pegel;
    public float Pegel { get => _pegel; private set => Setze(ref _pegel, value); }
    private string _vorschau = "";
    /// <summary>Zwischenergebnis während des Sprechens.</summary>
    public string Vorschau { get => _vorschau; private set => Setze(ref _vorschau, value); }
    private bool _schreibtMit;
    /// <summary>Mikrofon zu, die Aufnahme wird gerade in Text umgewandelt.</summary>
    public bool SchreibtMit { get => _schreibtMit; private set => Setze(ref _schreibtMit, value); }
    private float[]? _offeneAufnahme;
    /// <summary>Gescheiterte Mitschrift, kann erneut umgewandelt werden.</summary>
    public float[]? OffeneAufnahme { get => _offeneAufnahme; private set => Setze(ref _offeneAufnahme, value); }
    private string? _fehler;
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }

    private volatile bool stoppWunsch;
    private volatile bool abbruchWunsch;

    /// <summary>Vom Audio-Faden beschrieben, vom Hauptfaden gelesen.</summary>
    private sealed class Messung
    {
        public float Pegel;
        public float Rauschboden = 0.004f;
        public int Messungen;
        public readonly List<float> Verlauf = new();              // Pegel der letzten ~3 Sekunden
        public double Dauer;
        public double SprachZeit;
        public bool Begonnen;
        public DateTime LetzteSprache = DateTime.MinValue;
        public readonly List<float> Samples = new(16000 * 30);    // nicht mitten in der Aufnahme umkopieren
    }

    /// <summary>Hört zu und gibt den erkannten Text zurück (null = nichts). Wie am Handy: Das Diktat-Mikrofon läuft, bis Frank `Stoppen()` auslöst.
    /// Mit `stilleSek` (Gesprächsmodus) endet die Aufnahme nach so viel Stille hinter dem letzten Wort, mit `wartenSek`, wenn bis dahin keine Sprache kam.</summary>
    public Task<string?> HoereBisStille(double maxSekunden = 600, double? stilleSek = null, double? wartenSek = null, CancellationToken ct = default) =>
        Haupt.Auf(() => HoereIntern(maxSekunden, stilleSek, wartenSek, ct));

    private async Task<string?> HoereIntern(double maxSekunden, double? stilleSek, double? wartenSek, CancellationToken ct)
    {
        if (Aktiv) return null;
        Fehler = null;
        // Vorab belegen: Ein zweiter Aufruf während der Rechteprüfung soll nicht auch noch starten.
        Aktiv = true;
        try
        {
            if (!await SprachRechte.Mikrofon())
            {
                Fehler = "Mikrofon nicht erlaubt (Windows-Einstellungen → Datenschutz → Mikrofon → Desktop-Apps).";
                Log("fehler", $"Diktat: {Fehler}");
                return null;
            }
            Weckwort.Shared.Pausieren();
            try
            {
                stoppWunsch = false; abbruchWunsch = false; Vorschau = ""; Pegel = 0;
                return await Hoere(maxSekunden, stilleSek, wartenSek, ct);
            }
            finally
            {
                Weckwort.Shared.Fortsetzen();
            }
        }
        finally
        {
            Aktiv = false; Pegel = 0; SchreibtMit = false;
        }
    }

    private async Task<string?> Hoere(double maxSekunden, double? stilleSek, double? wartenSek, CancellationToken ct)
    {
        _ = TranskriptionsEinstellungen.Shared;            // setzt beim ersten Mal die Vorgabe (Parakeet, falls vorhanden)
        var verfahren = Einstellungen.Shared.Transkription;
        var mitWindows = verfahren == "windows";
        if (verfahren == "parakeet") ParakeetLocalClient.Shared.Preload();   // lädt, während Frank spricht
        var messung = new Messung();
        var umwandler = new Umwandler(16000);
        ISprachErkenner? erkenner = null;
        if (mitWindows)
        {
            var e = new WindowsErkenner(dauerbetrieb: false);
            e.BeiText = (text, _) => Haupt.Post(() => Vorschau = text);
            try { await e.Starten(); erkenner = e; }
            catch (Exception ex)
            {
                Fehler = $"Spracherkennung: {ex.Message}";
                Log("fehler", $"Diktat: {Fehler}");
                return null;
            }
        }
        var mikro = new Mikrofon();
        var mitSamples = !mitWindows;
        var hoechstSamples = (long)(16000 * maxSekunden);
        OffeneAufnahme = null;
        mikro.BeiPuffer = p =>
        {
            erkenner?.Fuettere(p);
            var rms = Mikrofon.Pegel(p);
            var dauer = p.Dauer;
            var w = mitSamples ? umwandler.Wandle(p) : null;
            lock (messung)
            {
                var m = messung;
                m.Pegel = rms;
                m.Dauer += dauer;
                // Die ersten 250 ms läuft der Piepton (wie am Handy): zählt weder als Sprache noch fürs Grundrauschen.
                if (m.Dauer >= 0.25)
                {
                    m.Messungen++;
                    m.Verlauf.Add(rms);
                    if (m.Verlauf.Count > 70) m.Verlauf.RemoveAt(0);
                    if (m.Messungen <= 6)                  // danach ~0,25 s: Rauschboden messen
                    {
                        m.Rauschboden = Math.Max(0.002f, (m.Rauschboden * (m.Messungen - 1) + rms) / m.Messungen);
                    }
                    else if (m.Messungen % 5 == 0)
                    {
                        // Die leisesten Stellen der letzten Sekunden: Sprache hat immer Lücken, und ein einzelner Aussetzer zählt nicht.
                        // So folgt der Boden auch einem Lüfter, der später anspringt, sonst hielte er die Aufnahme bis zur Höchstdauer offen.
                        var sortiert = m.Verlauf.OrderBy(x => x).ToList();
                        m.Rauschboden = Math.Max(0.002f, sortiert[sortiert.Count / 5]);
                    }
                    var schwelle = Math.Max(m.Rauschboden * 3.2f, 0.012f);
                    if (rms > schwelle)
                    {
                        m.SprachZeit += dauer;
                        m.LetzteSprache = DateTime.Now;
                        if (m.SprachZeit > 0.18) m.Begonnen = true;
                    }
                }
                if (w != null && m.Samples.Count < hoechstSamples) m.Samples.AddRange(w);
            }
        };
        try { await mikro.StartenImHintergrund(); }
        catch (Exception ex)
        {
            Fehler = ex.Message;
            Log("fehler", $"Diktat: Mikrofon startet nicht ({Fehler})");
            if (erkenner != null) await erkenner.Beenden(abschliessen: false);
            return null;
        }
        Toene.Start();
        var start = DateTime.Now;
        var abgeschlossen = false;
        while (true)
        {
            await Task.Delay(50);
            float p;
            bool begonnen;
            DateTime letzte;
            lock (messung) { p = messung.Pegel; begonnen = messung.Begonnen; letzte = messung.LetzteSprache; }
            Pegel = Math.Min(1f, p * 8);
            if (abbruchWunsch || ct.IsCancellationRequested) break;
            if (stoppWunsch) { abgeschlossen = true; break; }
            var jetzt = DateTime.Now;
            if ((jetzt - start).TotalSeconds > maxSekunden) { abgeschlossen = true; break; }
            if (begonnen)
            {
                if (stilleSek is double s && (jetzt - letzte).TotalSeconds >= s) { abgeschlossen = true; break; }
            }
            else if (wartenSek is double w && (jetzt - start).TotalSeconds >= w)
            {
                break;
            }
        }
        await mikro.StoppenImHintergrund();
        bool wurdeBegonnen;
        lock (messung) wurdeBegonnen = messung.Begonnen;
        if (!abgeschlossen || !(wurdeBegonnen || stoppWunsch))
        {
            if (erkenner != null) await erkenner.Beenden(abschliessen: false);
            Vorschau = "";
            return null;
        }
        SchreibtMit = true;
        string text;
        if (erkenner != null)
        {
            text = await erkenner.Beenden(abschliessen: true);
        }
        else
        {
            float[] samples;
            lock (messung) samples = messung.Samples.ToArray();
            try
            {
                text = await ErkenneMitNeuversuch(samples);
            }
            catch (Exception ex)
            {
                // Die Aufnahme bleibt erhalten: Frank soll zehn Minuten Diktat nicht neu sprechen müssen.
                OffeneAufnahme = samples;
                Fehler = ex.Message;
                Log("fehler", $"Diktat ({verfahren}): {Fehler}");
                return null;
            }
        }
        text = text.Trim();
        Vorschau = text;
        Log("sprache", $"Diktat erkannt ({TranskriptionsEinstellungen.Shared.AnzeigeName}): {text.Schnitt(120)}");
        return text.Length == 0 ? null : text;
    }

    private async Task<string> ErkenneMitNeuversuch(float[] samples)
    {
        try { return await Transkription.Erkenne(samples); }
        catch (Exception ex)
        {
            Log("sprache", $"Mitschrift gescheitert, zweiter Versuch: {ex.Message}");
        }
        await Task.Delay(1000);
        return await Transkription.Erkenne(samples);
    }

    /// <summary>Wandelt die zuletzt gescheiterte Aufnahme erneut um.</summary>
    public Task<string?> ErneutUmwandeln() => Haupt.Auf<string?>(async () =>
    {
        var samples = OffeneAufnahme;
        if (Aktiv || samples == null) return null;
        Aktiv = true; SchreibtMit = true; Fehler = null;
        try
        {
            var text = (await Transkription.Erkenne(samples)).Trim();
            OffeneAufnahme = null;
            return text.Length == 0 ? null : text;
        }
        catch (Exception ex)
        {
            Fehler = ex.Message;
            Log("fehler", $"Diktat, erneutes Umwandeln: {Fehler}");
            return null;
        }
        finally
        {
            Aktiv = false; SchreibtMit = false;
        }
    });

    /// <summary>Beendet das Zuhören und erkennt das bisher Gesagte.</summary>
    public void Stoppen() { if (Aktiv) stoppWunsch = true; }
    /// <summary>Verwirft die laufende Aufnahme.</summary>
    public void Abbrechen() { if (Aktiv) abbruchWunsch = true; }
}

// MARK: - Weckwort

/// <summary>Weckwort „Hey Jarvis“: lauscht dauerhaft auf diesem Rechner (Parakeet offline, ohne Modell die Windows-Erkennung).
/// Pausen werden gezählt: jedes `Pausieren()` braucht ein `Fortsetzen()`.</summary>
public sealed class Weckwort : Beobachtbar
{
    public static Weckwort Shared { get; } = new();
    private bool _lauscht;
    public bool Lauscht { get => _lauscht; private set => Setze(ref _lauscht, value); }
    private string? _fehler;
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }
    public Action? BeiErkennung;

    private bool gewuenscht;
    private int pausen;
    private Mikrofon? mikro;
    private ISprachErkenner? erkenner;
    private bool startet;
    private bool nachStartErneut;
    private CancellationTokenSource? neustartAufgabe;
    private DateTime letzterTreffer = DateTime.MinValue;

    private Weckwort()
    {
        SystemEvents.PowerModeChanged += (_, a) =>
        {
            if (a.Mode == PowerModes.Resume) Haupt.Post(() => NeuStarten("nach Ruhezustand"));
            else if (a.Mode == PowerModes.Suspend) Haupt.Post(() => _ = StoppeIntern());
        };
    }

    public void Starten() => Haupt.Post(() =>
    {
        gewuenscht = true;
        _ = StarteIntern();
    });

    public void Stoppen() => Haupt.Post(() =>
    {
        gewuenscht = false;
        _ = StoppeIntern();
    });

    /// <summary>Kurz anhalten, solange Diktat, Vorlesen oder Live das Mikrofon brauchen.</summary>
    public void Pausieren() => Haupt.Post(() =>
    {
        pausen++;
        if (Lauscht || startet) _ = StoppeIntern();
    });

    public void Fortsetzen() => Haupt.Post(async () =>
    {
        pausen = Math.Max(0, pausen - 1);
        if (pausen != 0 || !gewuenscht) return;
        await Task.Delay(350);
        await StarteIntern();
    });

    private void NeuStarten(string grund)
    {
        if (!gewuenscht || pausen != 0) return;
        Log("sprache", $"Weckwort startet neu ({grund})");
        _ = Neu();
        async Task Neu()
        {
            await StoppeIntern();
            await Task.Delay(500);
            await StarteIntern();
        }
    }

    /// <summary>Parakeet, wenn das Modell da ist; sonst die Windows-Erkennung. Scheitern beide, meldet der Aufrufer den letzten Fehler.</summary>
    private async Task<ISprachErkenner> StarteErkenner()
    {
        if (ParakeetLocalClient.IsInstalled)
        {
            var p = new ParakeetWeckErkenner(Einstellungen.Shared.Weckwort);
            p.BeiText = (text, _) => Haupt.Post(() => Pruefe(text));
            try
            {
                await p.Starten();
                return p;
            }
            catch (Exception ex)
            {
                await p.Beenden(abschliessen: false);
                Log("fehler", $"Weckwort: Parakeet nicht nutzbar ({ex.Message}), nehme die Windows-Erkennung");
            }
        }
        var w = new WindowsErkenner(dauerbetrieb: true);
        w.BeiText = (text, _) => Haupt.Post(() => Pruefe(text));
        await w.Starten();
        return w;
    }

    private async Task StarteIntern()
    {
        if (!gewuenscht || pausen != 0 || Lauscht || !Einstellungen.Shared.WeckwortAn) return;
        // Läuft noch ein Start, der gerade an einer Pause scheitern kann: danach neu prüfen, sonst bliebe das Weckwort aus.
        if (startet) { nachStartErneut = true; return; }
        startet = true;
        try
        {
            if (!await SprachRechte.Mikrofon())
            {
                Fehler = "Mikrofon nicht erlaubt – Weckwort aus.";
                Log("fehler", $"Weckwort: {Fehler}");
                return;
            }
            ISprachErkenner e;
            try { e = await StarteErkenner(); }
            catch (Exception ex)
            {
                Fehler = $"Weckwort: {ex.Message}";
                Log("fehler", Fehler);
                return;
            }
            // Während des Starts pausiert oder abgeschaltet?
            if (!gewuenscht || pausen != 0) { await e.Beenden(abschliessen: false); return; }
            var m = new Mikrofon();
            m.BeiPuffer = p => e.Fuettere(p);
            m.BeiGeraetewechsel = () => NeuStarten("Gerätewechsel");
            try { await m.StartenImHintergrund(); }
            catch (Exception ex)
            {
                Fehler = $"Weckwort: Mikrofon startet nicht ({ex.Message})";
                Log("fehler", Fehler);
                await e.Beenden(abschliessen: false);
                return;
            }
            // Während des Mikrofonstarts pausiert oder abgeschaltet?
            if (!gewuenscht || pausen != 0)
            {
                await m.StoppenImHintergrund();
                await e.Beenden(abschliessen: false);
                return;
            }
            mikro = m; erkenner = e;
            Lauscht = true; Fehler = null;
            Log("sprache", $"Weckwort lauscht auf „{Einstellungen.Shared.Weckwort}“ ({e.Name})");
            // Regelmäßig frisch starten: Das hält den Zustand klein.
            var intervall = e.NutztAnalyzer ? 600.0 : 50.0;
            neustartAufgabe?.Cancel();
            var uhr = new CancellationTokenSource();
            neustartAufgabe = uhr;
            _ = Intervall();
            async Task Intervall()
            {
                try { await Task.Delay(TimeSpan.FromSeconds(intervall), uhr.Token); }
                catch (OperationCanceledException) { return; }
                NeuStarten("Intervall");
            }
        }
        finally
        {
            startet = false;
            if (nachStartErneut)
            {
                nachStartErneut = false;
                _ = StarteIntern();
            }
        }
    }

    private async Task StoppeIntern()
    {
        var e = await MikroAus();
        if (e != null) await e.Beenden(abschliessen: false);
    }

    /// <summary>Erst das Mikrofon, danach der Erkenner: Solange die Aufnahme läuft, füttert sie ihn noch.</summary>
    private async Task<ISprachErkenner?> MikroAus()
    {
        neustartAufgabe?.Cancel();
        neustartAufgabe = null;
        var m = mikro;
        var e = erkenner;
        mikro = null; erkenner = null;
        Lauscht = false;
        if (m != null) await m.StoppenImHintergrund();
        return e;
    }

    private static readonly HashSet<string> Varianten = new()
    {
        "jarvis", "jarvice", "jervis", "javis", "jarwis", "jarwiss", "tschawis", "dschawis",
        "charvis", "scharvis", "jarviz", "jarvis's", "dravis", "travis",
    };

    /// <summary>Erkennt das Weckwort tolerant gegen Erkennungsvarianten.</summary>
    public static bool Passt(string roh, string phrase)
    {
        var t = GroqWhisper.Normal(roh);
        if (t.Length == 0) return false;
        var p = GroqWhisper.Normal(phrase);
        if (p.Length > 0 && t.Contains(p)) return true;
        var woerter = t.Split(' ', StringSplitOptions.RemoveEmptyEntries);
        var einleitung = new HashSet<string> { "hey", "hej", "hei", "he", "hallo", "okay", "ok", "hi", "ey" };
        var phraseNutztJarvis = p.Contains("jarvis") || p.Length == 0;
        if (!phraseNutztJarvis) return false;
        for (int i = 0; i < woerter.Length; i++)
        {
            var w = woerter[i];
            var istJarvis = Varianten.Contains(w) && ((w != "travis" && w != "dravis") || (i > 0 && einleitung.Contains(woerter[i - 1])));
            if (!istJarvis) continue;
            if (i > 0 && einleitung.Contains(woerter[i - 1])) return true;
            if (i == 0) return true;                       // „Jarvis, …“ am Satzanfang
        }
        return false;
    }

    private void Pruefe(string text)
    {
        if (!Lauscht || !Passt(text, Einstellungen.Shared.Weckwort)) return;
        if ((DateTime.Now - letzterTreffer).TotalSeconds <= 2) return;
        letzterTreffer = DateTime.Now;
        var ende = text.Length > 60 ? text[^60..] : text;
        Log("sprache", $"Weckwort erkannt: „{ende}“");
        // Der Piepton kommt mit der ersten Gesprächsrunde. Sobald das Mikrofon frei ist, geht es los; der Erkenner endet nebenher.
        _ = Weiter();
        async Task Weiter()
        {
            var alterErkenner = await MikroAus();
            if (BeiErkennung != null) BeiErkennung();
            else SprachSchleife.Shared.Starten(GespraechsSpeicher.Shared.Neu());
            if (alterErkenner != null) await alterErkenner.Beenden(abschliessen: false);
            // Hat niemand pausiert (z. B. leerer Handler), nach kurzer Zeit wieder lauschen.
            await Task.Delay(2000);
            if (pausen == 0) await StarteIntern();
        }
    }
}

// MARK: - Sprachschleife

/// <summary>Zustand der Sprachschleife (`SprachSchleife.Zustand` in Swift; in C# kann der Typ nicht so heißen wie die Eigenschaft).</summary>
public enum SprachZustand { Aus, Hoert, Denkt, Spricht }

/// <summary>Der Gesprächsmodus wie am Handy (SprechEnde.kt): Piepton, zuhören bis zur eingestellten Stillezeit nach dem letzten Wort, Jarvis, vorlesen, wieder von vorn.
/// Kommt nach dem Piepton nichts (7 Sekunden, ab der zweiten Runde 9), ist Schluss, ebenso bei einem Ausbefehl.</summary>
public sealed class SprachSchleife : Beobachtbar
{
    public static SprachSchleife Shared { get; } = new();
    private SprachZustand _zustand = SprachZustand.Aus;
    public SprachZustand Zustand
    {
        get => _zustand;
        private set { if (Setze(ref _zustand, value)) AppSteuerung.Shared.PilleAbgleichen(); }
    }
    private Gespraech? _gespraech;
    public Gespraech? Gespraech { get => _gespraech; private set => Setze(ref _gespraech, value); }
    private CancellationTokenSource? aufgabe;
    private int lauf;

    /// <summary>[inApp]: Das Gespräch begann bei Jarvis vorn; sonst gilt die Stillezeit für außerhalb.</summary>
    public void Starten(Gespraech g, bool inApp = false) => Haupt.Post(() =>
    {
        aufgabe?.Cancel();
        Diktat.Shared.Abbrechen();
        Stimme.Shared.Stopp();
        lauf++;
        var meiner = lauf;
        Gespraech = g;
        var neu = new CancellationTokenSource();
        aufgabe = neu;
        _ = Schleife(g, meiner, inApp, neu.Token);
    });

    public void Beenden() => Haupt.Post(() =>
    {
        lauf++;
        aufgabe?.Cancel();
        aufgabe = null;
        Diktat.Shared.Abbrechen();
        Stimme.Shared.Stopp();
        Zustand = SprachZustand.Aus;
    });

    private async Task Schleife(Gespraech g, int meiner, bool inApp, CancellationToken ct)
    {
        Weckwort.Shared.Pausieren();
        try
        {
            Log("sprache", "Sprachschleife gestartet");
            var runde = 0;
            while (lauf == meiner && !ct.IsCancellationRequested)
            {
                if (runde > 0) await Task.Delay(300);
                if (lauf != meiner || ct.IsCancellationRequested) break;
                Zustand = SprachZustand.Hoert;
                // Ein abgebrochenes Diktat ist gleich frei, eine laufende Mitschrift braucht länger und wird abgewartet.
                var warte = 0;
                while (Diktat.Shared.Aktiv && warte < 1200 && lauf == meiner) { await Task.Delay(50); warte++; }
                if (lauf != meiner || ct.IsCancellationRequested) break;
                if (Diktat.Shared.Aktiv) { Log("fehler", "Gesprächsmodus: Das Mikrofon wurde nicht frei"); break; }
                // Je Runde neu: Eine per Sprache geänderte Zeit gilt ab der nächsten Frage, ebenso das Verlassen des Fensters oder ein gesperrter Bildschirm.
                var stilleJetzt = inApp && AppSteuerung.Shared.FensterVorn && !AppSteuerung.BildschirmAus ? Einstellungen.Shared.StilleInAppSek : Einstellungen.Shared.StilleAussenSek;
                var text = await Diktat.Shared.HoereBisStille(maxSekunden: 120, stilleSek: stilleJetzt, wartenSek: runde == 0 ? 7 : 9, ct: ct);
                if (text == null) break;
                if (lauf != meiner || ct.IsCancellationRequested) break;
                if (SprachBefehle.IstSessionEnde(text))
                {
                    // Geht nie ans Modell: kein Eintrag im Gespräch, nur die kurze Bestätigung.
                    Log("sprache", $"Sprachschleife beendet („{text}“)");
                    Zustand = SprachZustand.Spricht;
                    await Stimme.Shared.Sprich("Okay, Sprachsession beendet.");
                    break;
                }
                Zustand = SprachZustand.Denkt;
                var antwort = await JarvisAgent.Shared.Frage(text, g, gesprochen: true, ct: ct);
                if (lauf != meiner || ct.IsCancellationRequested) break;
                if (Einstellungen.Shared.Vorlesen && antwort.Length > 0)
                {
                    Zustand = SprachZustand.Spricht;
                    await Stimme.Shared.Sprich(antwort);
                    if (lauf != meiner || ct.IsCancellationRequested) break;
                }
                var letzte = g.Nachrichten.LastOrDefault(n => n.Rolle.ToString() == "Jarvis");
                if (letzte != null && letzte.Schritte.Any(s => s.Werkzeug == "sprachsession_beenden")) break;
                runde++;
            }
        }
        catch (OperationCanceledException) { }
        catch (Exception ex)
        {
            Log("fehler", $"Sprachschleife: {ex.Message}");
        }
        finally
        {
            Weckwort.Shared.Fortsetzen();
            if (lauf == meiner) Zustand = SprachZustand.Aus;
        }
    }
}
