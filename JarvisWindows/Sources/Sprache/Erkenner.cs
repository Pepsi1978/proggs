using System.Media;
using System.Net;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Text;
using Microsoft.Win32;
using NAudio.CoreAudioApi;
using NAudio.CoreAudioApi.Interfaces;
using NAudio.Dsp;
using NAudio.Wave;

namespace JarvisWindows;

// MARK: - Netz

/// <summary>Geteilter HTTP-Weg der Sprachdienste: IPv4 zuerst (totes IPv6 ließ Abfragen sonst bis ans Zeitlimit hängen), eine langlebige Verbindungsgruppe.</summary>
public static class SprachHttp
{
    public static SocketsHttpHandler NeuerHandler() => new()
    {
        UseCookies = false,
        PooledConnectionLifetime = TimeSpan.FromMinutes(15),
        // Im Gespräch liegen Minuten zwischen zwei Sätzen: Die Verbindung zu Stimme und Groq soll das überstehen.
        PooledConnectionIdleTimeout = TimeSpan.FromMinutes(5),
        ConnectTimeout = TimeSpan.FromSeconds(15),
        ConnectCallback = async (kontext, ct) =>
        {
            var adressen = await Dns.GetHostAddressesAsync(kontext.DnsEndPoint.Host, ct).ConfigureAwait(false);
            Exception? letzter = null;
            foreach (var a in adressen.OrderBy(x => x.AddressFamily == AddressFamily.InterNetwork ? 0 : 1))
            {
                var s = new Socket(a.AddressFamily, SocketType.Stream, ProtocolType.Tcp) { NoDelay = true };
                try
                {
                    using var frist = CancellationTokenSource.CreateLinkedTokenSource(ct);
                    frist.CancelAfter(TimeSpan.FromSeconds(a.AddressFamily == AddressFamily.InterNetwork ? 8 : 4));
                    await s.ConnectAsync(a, kontext.DnsEndPoint.Port, frist.Token).ConfigureAwait(false);
                    return new NetworkStream(s, ownsSocket: true);
                }
                catch (Exception ex)
                {
                    s.Dispose();
                    letzter = ex;
                    ct.ThrowIfCancellationRequested();
                }
            }
            throw letzter ?? new IOException($"{kontext.DnsEndPoint.Host} nicht erreichbar");
        },
    };

    private static readonly SocketsHttpHandler Handler = NeuerHandler();

    public static readonly HttpClient Client = Neu();

    private static HttpClient Neu()
    {
        var c = new HttpClient(Handler, disposeHandler: false) { Timeout = Timeout.InfiniteTimeSpan };
        c.DefaultRequestHeaders.TryAddWithoutValidation("User-Agent", $"JarvisWindows/{AppInfo.Version}");
        return c;
    }

    /// <summary>Baut die Verbindung vorab auf, das Ergebnis zählt nicht. Höchstens drei Sekunden, nie ein Fehler.</summary>
    public static async Task Vorwaermen(string adresse)
    {
        try
        {
            using var frist = new CancellationTokenSource(TimeSpan.FromSeconds(3));
            using var req = new HttpRequestMessage(HttpMethod.Head, adresse);
            using var a = await Client.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, frist.Token).ConfigureAwait(false);
        }
        catch { }
    }

    /// <summary>Für ClientWebSocket.ConnectAsync.</summary>
    public static readonly HttpMessageInvoker Invoker = new(Handler, disposeHandler: false);
}

// MARK: - Rechte

public static class SprachRechte
{
    private const string Einwilligung = @"Software\Microsoft\Windows\CurrentVersion\CapabilityAccessManager\ConsentStore\microphone";

    /// <summary>Windows fragt Desktop-Apps nicht einzeln: Erlaubt ist, was die Datenschutzschalter nicht verbieten. Fehlt der Eintrag, gilt „erlaubt“.</summary>
    public static Task<bool> Mikrofon() => Task.Run(() =>
    {
        static bool Verboten(RegistryKey wurzel, string pfad)
        {
            try
            {
                using var k = wurzel.OpenSubKey(pfad);
                return string.Equals(k?.GetValue("Value") as string, "Deny", StringComparison.OrdinalIgnoreCase);
            }
            catch { return false; }
        }
        return !Verboten(Registry.LocalMachine, Einwilligung) && !Verboten(Registry.CurrentUser, Einwilligung)
            && !Verboten(Registry.CurrentUser, Einwilligung + @"\NonPackaged");
    });

    /// <summary>Windows-Diktat läuft nur mit eingeschalteter Online-Spracherkennung.</summary>
    public static Task<bool> Spracherkennung() => Task.Run(() =>
    {
        try
        {
            using var k = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Speech_OneCore\Settings\OnlineSpeechPrivacy");
            return k?.GetValue("HasAccepted") is int i && i == 1;
        }
        catch { return false; }
    });
}

// MARK: - Mikrofon

/// <summary>Ein Puffer vom Mikrofon: bereits mono, Rate wie vom Gerät geliefert.</summary>
public sealed record MikroPuffer(float[] Samples, int Rate)
{
    public int FrameLength => Samples.Length;
    public double Dauer => Rate > 0 ? (double)Samples.Length / Rate : 0;
}

/// <summary>Eine Aufnahme über ein eigenes WasapiCapture im gemeinsamen Modus. Format immer vom Gerät, Neustart nach Gerätewechsel
/// über die Endpunkt-Meldungen von Windows.</summary>
public sealed class Mikrofon
{
    private WasapiCapture? engine;
    private MMDeviceEnumerator? aufzaehler;
    private Melder? melder;
    private readonly object schloss = new();
    /// <summary>Auf- und Abbau der Aufnahme der Reihe nach. Beides kann dauern: Aufrufer auf dem Hauptfaden nehmen `StartenImHintergrund`.</summary>
    private readonly object ablauf = new();
    /// <summary>Läuft auf dem Audio-Faden.</summary>
    public Action<MikroPuffer>? BeiPuffer;
    /// <summary>Wird nach einem Gerätewechsel aufgerufen (Hauptfaden), nachdem die Aufnahme neu gestartet wurde oder scheiterte.</summary>
    public Action? BeiGeraetewechsel;

    public bool Laeuft { get { lock (schloss) return engine != null; } }

    /// <summary>Nur unter `ablauf`: Ein Gerätewechsel darf ein inzwischen gestopptes Mikrofon nicht wieder starten.</summary>
    private bool gewuenscht;

    public void Starten() { lock (ablauf) { gewuenscht = true; StartenIntern(); } }

    public Task StartenImHintergrund() => Task.Run(Starten);

    public Task StoppenImHintergrund() => Task.Run(Stoppen);

    private sealed class Melder : IMMNotificationClient
    {
        private readonly Action wechsel;
        public Melder(Action wechsel) { this.wechsel = wechsel; }
        public void OnDefaultDeviceChanged(DataFlow flow, Role role, string defaultDeviceId) { if (flow == DataFlow.Capture && role == Role.Console) wechsel(); }
        public void OnDeviceStateChanged(string deviceId, DeviceState newState) { }
        public void OnDeviceAdded(string pwstrDeviceId) { }
        public void OnDeviceRemoved(string deviceId) { }
        public void OnPropertyValueChanged(string pwstrDeviceId, PropertyKey key) { }
    }

    private void Gewechselt()
    {
        // Eigener Faden: Die Meldung kommt aus Windows und darf dort nicht warten.
        Task.Run(() =>
        {
            Log("sprache", "Audiogerät gewechselt – Aufnahme startet neu");
            lock (ablauf)
            {
                if (!gewuenscht) return;
                try { StartenIntern(); }
                catch (Exception ex) { Log("fehler", $"Mikrofon: Neustart gescheitert ({ex.Message})"); }
            }
            Haupt.Post(() => BeiGeraetewechsel?.Invoke());
        });
    }

    private void StartenIntern()
    {
        StoppenIntern();
        var en = new MMDeviceEnumerator();
        if (!en.HasDefaultAudioEndpoint(DataFlow.Capture, Role.Console))
        {
            en.Dispose();
            throw new InvalidOperationException("Kein Mikrofon verfügbar");
        }
        var geraet = en.GetDefaultAudioEndpoint(DataFlow.Capture, Role.Console);
        var e = new WasapiCapture(geraet, true, 50);
        var format = e.WaveFormat;
        if (format.SampleRate <= 0 || format.Channels <= 0)
        {
            e.Dispose();
            en.Dispose();
            throw new InvalidOperationException("Kein Mikrofon verfügbar");
        }
        e.DataAvailable += (_, a) =>
        {
            if (a.BytesRecorded <= 0) return;
            var ziel = BeiPuffer;
            if (ziel == null) return;
            var p = ZuMono(a.Buffer, a.BytesRecorded, format);
            if (p.Length > 0) ziel(new MikroPuffer(p, format.SampleRate));
        };
        e.RecordingStopped += (_, a) =>
        {
            if (a.Exception == null) return;
            bool aktuell;
            lock (schloss) aktuell = ReferenceEquals(engine, e);
            if (aktuell) Gewechselt();
        };
        e.StartRecording();
        lock (schloss) engine = e;
        aufzaehler = en;
        melder = new Melder(Gewechselt);
        en.RegisterEndpointNotificationCallback(melder);
    }

    public void Stoppen() { lock (ablauf) { gewuenscht = false; StoppenIntern(); } }

    private void StoppenIntern()
    {
        if (aufzaehler != null)
        {
            try { if (melder != null) aufzaehler.UnregisterEndpointNotificationCallback(melder); } catch { }
            aufzaehler.Dispose();
            aufzaehler = null;
            melder = null;
        }
        WasapiCapture? e;
        lock (schloss) { e = engine; engine = null; }
        if (e == null) return;
        try { e.StopRecording(); } catch { }
        try { e.Dispose(); } catch { }
    }

    private static float[] ZuMono(byte[] d, int n, WaveFormat f)
    {
        int kanaele = Math.Max(1, f.Channels), bytes = Math.Max(1, f.BitsPerSample / 8), rahmen = n / (bytes * kanaele);
        var aus = new float[rahmen];
        var sub = (f as WaveFormatExtensible)?.SubFormat;
        // Extensible nur mit bekanntem Unterformat auswerten: 32-Bit-PCM als Float gelesen wäre reines Rauschen.
        var istFloat = f.Encoding == WaveFormatEncoding.IeeeFloat || sub == new Guid("00000003-0000-0010-8000-00aa00389b71")
            || (f.Encoding == WaveFormatEncoding.Extensible && sub == null && f.BitsPerSample == 32);
        int i = 0;
        for (int r = 0; r < rahmen; r++)
        {
            float s = 0;
            for (int k = 0; k < kanaele; k++, i += bytes)
            {
                s += bytes switch
                {
                    4 when istFloat => BitConverter.ToSingle(d, i),
                    4 => BitConverter.ToInt32(d, i) / 2147483648f,
                    3 => ((d[i] << 8 | d[i + 1] << 16 | d[i + 2] << 24) >> 8) / 8388608f,
                    2 => BitConverter.ToInt16(d, i) / 32768f,
                    _ => 0f,
                };
            }
            aus[r] = s / kanaele;
        }
        return aus;
    }

    public static float Pegel(MikroPuffer p) => Pegel(p.Samples);

    public static float Pegel(float[] s, int anzahl = -1)
    {
        var n = anzahl < 0 ? s.Length : anzahl;
        if (n <= 0) return 0;
        float summe = 0;
        for (int i = 0; i < n; i++) summe += s[i] * s[i];
        return MathF.Sqrt(summe / n);
    }
}

/// <summary>Grundrauschen der letzten drei Sekunden, nach Zeit statt nach Pufferzahl: Bei 10-ms-Puffern wären 70 Einträge nur 0,7 Sekunden,
/// das Perzentil stiege bei einem langen Satz auf Sprachpegel und die Aufnahme endete mitten im Satz. Die leisesten Stellen zählen: Sprache hat immer Lücken.</summary>
public sealed class RauschBoden
{
    private const double Fenster = 3, ErsteMessung = 0.3, Takt = 0.25;
    private readonly Queue<(float Rms, double Dauer)> verlauf = new();
    private double imFenster, gesamt, seitMessung;
    public float Wert { get; private set; } = 0.004f;
    public float Schwelle => Math.Max(Wert * 3.2f, 0.012f);

    public void Messe(float rms, double dauer)
    {
        verlauf.Enqueue((rms, dauer));
        imFenster += dauer;
        gesamt += dauer;
        while (imFenster > Fenster && verlauf.Count > 1) imFenster -= verlauf.Dequeue().Dauer;
        if (gesamt <= ErsteMessung)
        {
            Wert = Math.Max(0.002f, verlauf.Average(x => x.Rms));
            return;
        }
        seitMessung += dauer;
        if (seitMessung < Takt) return;
        seitMessung = 0;
        var sortiert = verlauf.Select(x => x.Rms).ToArray();
        Array.Sort(sortiert);
        Wert = Math.Max(0.002f, sortiert[sortiert.Length / 5]);
    }
}

/// <summary>Wandelt Mikrofonpuffer in eine Zielrate (z. B. 16 kHz mono) um, über Puffergrenzen hinweg ohne Knackser.</summary>
public sealed class Umwandler
{
    public int Ziel { get; }
    private WdlResampler? konverter;
    private int quelle;
    private float[] ausgabe = Array.Empty<float>();

    public Umwandler(int ziel) { Ziel = ziel; }

    public float[]? Wandle(MikroPuffer p)
    {
        if (p.Samples.Length == 0) return null;
        if (p.Rate == Ziel) return p.Samples;
        if (konverter == null || quelle != p.Rate)
        {
            quelle = p.Rate;
            konverter = new WdlResampler();
            konverter.SetMode(true, 2, false);
            konverter.SetFilterParms();
            konverter.SetFeedMode(true);
            konverter.SetRates(p.Rate, Ziel);
        }
        var kapazitaet = (int)((long)p.Samples.Length * Ziel / p.Rate) + 64;
        if (ausgabe.Length < kapazitaet) ausgabe = new float[kapazitaet];
        var ein = konverter.ResamplePrepare(p.Samples.Length, 1, out var inPuffer, out var inVersatz);
        Array.Copy(p.Samples, 0, inPuffer, inVersatz, Math.Min(ein, p.Samples.Length));
        var n = konverter.ResampleOut(ausgabe, 0, ein, kapazitaet, 1);
        if (n <= 0) return null;
        var aus = new float[n];
        Array.Copy(ausgabe, aus, n);
        return aus;
    }
}

// MARK: - Erkenner

/// <summary>Streaming-Erkennung: `BeiText(gesamt, final)` liefert den bisherigen Gesamttext (fertige Teile + vorläufiger Rest).</summary>
public interface ISprachErkenner
{
    Action<string, bool>? BeiText { get; set; }
    string Name { get; }
    /// <summary>Hält eine Sitzung beliebig lange (bei Apple der SpeechAnalyzer; hier alle Erkenner).</summary>
    bool NutztAnalyzer { get; }
    string Text { get; }
    Task Starten();
    /// <summary>Audio-Faden.</summary>
    void Fuettere(MikroPuffer p);
    /// <summary>Beendet die Erkennung. `abschliessen` = auf das Endergebnis warten (Diktat), sonst sofort abbrechen.</summary>
    Task<string> Beenden(bool abschliessen);
}

/// <summary>Windows-Spracherkennung (Windows.Media.SpeechRecognition, Deutsch) mit Live-Vorschau, Gegenstück zu Apples Erkennung.
/// Sie greift das Mikrofon selbst; `Fuettere` ist deshalb leer, das Mikrofon läuft daneben nur für Pegel und Ende-Erkennung.</summary>
public sealed class WindowsErkenner : ISprachErkenner
{
    public Action<string, bool>? BeiText { get; set; }
    public string Name => "Windows-Spracherkennung";
    public bool NutztAnalyzer => true;
    /// <summary>Nur für das Weckwort: fertige Teile nicht aufsammeln (Dauerbetrieb).</summary>
    public bool Dauerbetrieb { get; }

    private readonly object schloss = new();
    private string fertig = "";
    private string vorlaeufig = "";
    private global::Windows.Media.SpeechRecognition.SpeechRecognizer? erkenner;
    private TaskCompletionSource? beendet;
    private bool ende;

    public WindowsErkenner(bool dauerbetrieb) { Dauerbetrieb = dauerbetrieb; }

    public string Text
    {
        get { lock (schloss) return (fertig + " " + vorlaeufig).Trim(' '); }
    }

    public async Task Starten()
    {
        if (!await SprachRechte.Spracherkennung())
            throw new InvalidOperationException("Spracherkennung nicht erlaubt (Windows-Einstellungen → Datenschutz → Spracherkennung → Online-Spracherkennung)");
        await Task.Run(async () =>
        {
            var de = global::Windows.Media.SpeechRecognition.SpeechRecognizer.SupportedTopicLanguages.FirstOrDefault(l => l.LanguageTag.StartsWith("de", StringComparison.OrdinalIgnoreCase));
            if (de == null) throw new InvalidOperationException("Deutsche Spracherkennung nicht verfügbar (Windows-Einstellungen → Zeit und Sprache → Sprache: Deutsch mit Spracherkennung)");
            var e = new global::Windows.Media.SpeechRecognition.SpeechRecognizer(de);
            e.Constraints.Add(new global::Windows.Media.SpeechRecognition.SpeechRecognitionTopicConstraint(global::Windows.Media.SpeechRecognition.SpeechRecognitionScenario.Dictation, "diktat"));
            var kompiliert = await e.CompileConstraintsAsync();
            if (kompiliert.Status != global::Windows.Media.SpeechRecognition.SpeechRecognitionResultStatus.Success)
            {
                e.Dispose();
                throw new InvalidOperationException($"Spracherkennung nicht startbar ({kompiliert.Status})");
            }
            // Pausen beenden die Sitzung nicht: Das Ende bestimmt das Diktat bzw. das Weckwort.
            e.ContinuousRecognitionSession.AutoStopSilenceTimeout = TimeSpan.FromHours(2);
            e.HypothesisGenerated += (_, a) =>
            {
                string gesamt;
                lock (schloss) { vorlaeufig = a.Hypothesis.Text; gesamt = (fertig + " " + vorlaeufig).Trim(' '); }
                BeiText?.Invoke(gesamt, false);
            };
            e.ContinuousRecognitionSession.ResultGenerated += (_, a) =>
            {
                var s = (a.Result.Text ?? "").Trim();
                string gesamt;
                lock (schloss)
                {
                    if (Dauerbetrieb) fertig = s; else if (s.Length > 0) fertig += (fertig.Length == 0 ? "" : " ") + s;
                    vorlaeufig = "";
                    gesamt = (fertig + " " + vorlaeufig).Trim(' ');
                }
                BeiText?.Invoke(gesamt, true);
            };
            e.ContinuousRecognitionSession.Completed += async (s, a) =>
            {
                TaskCompletionSource? k;
                bool weiter;
                lock (schloss) { weiter = !ende; k = beendet; beendet = null; }
                k?.TrySetResult();
                if (!weiter) return;
                // Von selbst beendet (Netz weg, Dienst hat aufgelegt): frisch weiterhören, sonst bliebe die Erkennung still aus.
                Log("sprache", $"Erkennung beendet: {a.Status}");
                await Task.Delay(500);
                lock (schloss) if (ende) return;
                try { await s.StartAsync(); }
                catch (Exception ex) { Log("fehler", $"Erkennung: Neustart gescheitert ({ex.Message})"); }
            };
            erkenner = e;
            try { await e.ContinuousRecognitionSession.StartAsync(); }
            catch (COMException ex) when ((uint)ex.HResult == 0x80045509)
            {
                throw new InvalidOperationException("Spracherkennung nicht erlaubt (Windows-Einstellungen → Datenschutz → Spracherkennung → Online-Spracherkennung)");
            }
        });
    }

    public void Fuettere(MikroPuffer p) { }

    public async Task<string> Beenden(bool abschliessen)
    {
        var e = erkenner;
        erkenner = null;
        lock (schloss) ende = true;
        if (e == null) return Text;
        await Task.Run(async () =>
        {
            try
            {
                if (abschliessen)
                {
                    var k = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
                    lock (schloss) beendet = k;
                    await e.ContinuousRecognitionSession.StopAsync();
                    await Task.WhenAny(k.Task, Task.Delay(3000));
                }
                else
                {
                    await e.ContinuousRecognitionSession.CancelAsync();
                }
            }
            catch (Exception ex) { Log("sprache", $"Erkennung beendet: {ex.Message}"); }
            try { e.Dispose(); } catch { }
        });
        return Text;
    }
}

/// <summary>Weckwort-Erkennung ganz auf diesem Rechner: Energie-VAD wie im Diktat, gesprochene Abschnitte gehen als kurzer Ausschnitt an Parakeet
/// (Wortliste mit dem Weckwort), das Ergebnis prüft `Weckwort.Passt`. Gerechnet wird nur, wenn jemand spricht.</summary>
public sealed class ParakeetWeckErkenner : ISprachErkenner
{
    private const int Rate = 16000;
    private const int VorlaufSamples = Rate * 4 / 10;       // 0,4 s vor dem ersten lauten Puffer
    private const int FensterSamples = Rate * 3;            // Ausschnitt höchstens 3 s …
    private const int UeberlappSamples = Rate;              // … mit 1 s Überlapp, damit ein Weckwort an der Schnittstelle nicht zerfällt
    private const double StilleBisEnde = 0.45;
    private const int FruehSamples = Rate * 6 / 5;          // nach 1,2 s schon einmal prüfen: „Hey Jarvis, …“ soll nicht aufs Ende des Fensters warten
    private const double MinStimme = 0.2;                   // Klicks und Husten nicht rechnen

    public Action<string, bool>? BeiText { get; set; }
    public string Name => "Parakeet";
    public bool NutztAnalyzer => true;

    private readonly Umwandler umwandler = new(Rate);
    private readonly object schloss = new();
    private readonly string hotwords;
    private readonly List<float> vorlauf = new();
    private readonly List<float> abschnitt = new();
    private readonly RauschBoden boden = new();
    private bool spricht;
    private bool fruehGeprueft;
    private double stille, stimme;
    private float[]? offen;
    private bool rechnet;
    private bool ende;
    private bool haelt;
    private string letzter = "";

    public ParakeetWeckErkenner(string phrase)
    {
        hotwords = ParakeetLocalClient.Hotwords(phrase + ",Hey Jarvis,Jarvis");
    }

    public string Text { get { lock (schloss) return letzter; } }

    public async Task Starten()
    {
        await ParakeetLocalClient.Shared.Laden();
        ParakeetLocalClient.Shared.Halten(true);
        lock (schloss) haelt = true;
    }

    public void Fuettere(MikroPuffer p)
    {
        var w = umwandler.Wandle(p);
        if (w == null) return;
        var rms = Mikrofon.Pegel(w);
        var dauer = (double)w.Length / Rate;
        lock (schloss)
        {
            if (ende) return;
            boden.Messe(rms, dauer);
            var laut = rms > boden.Schwelle;
            if (!spricht)
            {
                if (laut)
                {
                    spricht = true;
                    fruehGeprueft = false;
                    stille = 0;
                    stimme = dauer;
                    abschnitt.Clear();
                    abschnitt.AddRange(vorlauf);
                    abschnitt.AddRange(w);
                }
                vorlauf.AddRange(w);
                if (vorlauf.Count > VorlaufSamples) vorlauf.RemoveRange(0, vorlauf.Count - VorlaufSamples);
                return;
            }
            abschnitt.AddRange(w);
            if (laut) { stimme += dauer; stille = 0; } else stille += dauer;
            if (stille >= StilleBisEnde)
            {
                if (stimme >= MinStimme) Abgeben(abschnitt.ToArray());
                abschnitt.Clear();
                vorlauf.Clear();
                spricht = false;
            }
            else if (abschnitt.Count >= FensterSamples)
            {
                Abgeben(abschnitt.ToArray());
                abschnitt.RemoveRange(0, abschnitt.Count - UeberlappSamples);
                stimme = 0;
            }
            else if (!fruehGeprueft && abschnitt.Count >= FruehSamples && stimme >= MinStimme)
            {
                fruehGeprueft = true;
                Abgeben(abschnitt.ToArray());
            }
        }
    }

    /// <summary>Unter dem Schloss. Gerechnet wird immer nur ein Ausschnitt; kommt der nächste früher, zählt der neueste.</summary>
    private void Abgeben(float[] ausschnitt)
    {
        offen = ausschnitt;
        if (rechnet) return;
        rechnet = true;
        _ = Task.Run(Arbeiten);
    }

    private async Task Arbeiten()
    {
        while (true)
        {
            float[]? a;
            lock (schloss)
            {
                a = ende ? null : offen;
                offen = null;
                if (a == null) { rechnet = false; return; }
            }
            try
            {
                var text = await ParakeetLocalClient.Shared.Kurzmitschrift(a, hotwords).ConfigureAwait(false);
                if (text.Length == 0) continue;
                bool beendet;
                lock (schloss) { letzter = text; beendet = ende; }
                if (!beendet) BeiText?.Invoke(text, true);
            }
            catch (Exception ex)
            {
                Log("fehler", $"Weckwort: {ex.Message}");
            }
        }
    }

    public Task<string> Beenden(bool abschliessen)
    {
        bool hielt;
        lock (schloss) { ende = true; offen = null; hielt = haelt; haelt = false; }
        if (hielt) ParakeetLocalClient.Shared.Halten(false);
        return Task.FromResult(Text);
    }
}

// MARK: - Frist

public static partial class Globale
{
    /// <summary>Wartet höchstens `sekunden` auf eine Arbeit. true = fertig geworden. Die Arbeit läuft bei Fristende weiter, nur niemand wartet mehr.</summary>
    public static Task<bool> MitFrist(double sekunden, Func<Task> arbeit)
    {
        var signal = new EinmalSignal();
        var k = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        signal.Setze(k);
        _ = Lauf();
        _ = Uhr();
        return k.Task;

        async Task Lauf()
        {
            try { await arbeit(); } catch { }
            signal.Melde(true);
        }
        async Task Uhr()
        {
            await Task.Delay(TimeSpan.FromSeconds(sekunden)).ConfigureAwait(false);
            signal.Melde(false);
        }
    }
}

public sealed class EinmalSignal
{
    private readonly object schloss = new();
    private TaskCompletionSource<bool>? k;
    private bool fertig;
    public void Setze(TaskCompletionSource<bool> k) { lock (schloss) this.k = k; }
    public void Melde(bool wert)
    {
        TaskCompletionSource<bool>? x;
        lock (schloss)
        {
            if (fertig) return;
            fertig = true;
            x = k;
            k = null;
        }
        x?.TrySetResult(wert);
    }
}

// MARK: - Groq Whisper

/// <summary>Groq Whisper wie im Handy-Modul M1.4: Large V3 Turbo oder Large V3, Wörterbuch-Prompt mit Echo-Schutz und vier Filter
/// (Stille vorab, Segment-Werte, Zeitstempel, Floskeln). Ist Large V3 für die Organisation gesperrt, nimmt Jarvis
/// dieselbe Aufnahme sofort mit Turbo und überspringt Large V3 30 Minuten, wie am Handy.</summary>
public static class GroqWhisper
{
    public sealed record Filter(bool Stille = true, bool Segmente = true, bool Zeitstempel = true, bool Floskeln = true);

    private static readonly HashSet<string> FloskelListe = new()
    {
        "vielen dank", "vielen dank fürs zuschauen", "vielen dank fuers zuschauen", "vielen dank für eure aufmerksamkeit",
        "vielen dank für ihre aufmerksamkeit", "vielen dank für die aufmerksamkeit", "bis zum nächsten mal", "bis zum nächsten video",
        "untertitel", "untertitel des zdf", "untertitelung des zdf für funk", "untertitel im auftrag des zdf für funk",
        "untertitel von stephanie geiges", "untertitel der amara org community", "der text ist nicht auf deutsch",
        "thank you", "thank you for watching", "thanks for watching", "please subscribe", "subtitles by the amara org community",
        "danke fürs zuschauen", "untertitel im auftrag des zdf", "sous titrage société radio canada", "merci d avoir regardé",
        "subtítulos realizados por la comunidad de amara org", "gracias por ver el video",
    };
    /// <summary>Kurze Wörter, die Whisper bei fast stiller Aufnahme erfindet: gesperrt bis 1,5 s Stimme.</summary>
    private static readonly HashSet<string> KurzeFloskeln = new() { "danke", "danke schön", "dankeschön", "tschüss", "amen", "musik", "swr", "copyright wdr", "untertitel" };

    private static readonly object SperreLock = new();
    private static DateTime gesperrtBis = DateTime.MinValue;
    public static bool LargeV3Gesperrt { get { lock (SperreLock) return DateTime.Now < gesperrtBis; } }
    public static void SperreAufheben() { lock (SperreLock) gesperrtBis = DateTime.MinValue; }

    public static string Normal(string s)
    {
        var c = new StringBuilder();
        foreach (var z in s.ToLowerInvariant())
        {
            if (char.IsLetter(z)) c.Append(z);
            else if (char.IsWhiteSpace(z)) c.Append(' ');
        }
        return string.Join(" ", c.ToString().Split(' ', StringSplitOptions.RemoveEmptyEntries));
    }

    /// <summary>Transkribiert 16-kHz-Mono-Samples.</summary>
    public static async Task<string> Transkribiere(float[] samples, SprachAnalyse analyse, string schluessel, string modell, string? prompt, Filter filter)
    {
        if (schluessel.Length == 0) throw new InvalidOperationException("Kein Groq-Schlüssel eingetragen (Einstellungen → Konten).");
        // Schicht 1: Stille vorab: gar nicht erst hochladen, und Stille vorn/hinten abschneiden.
        if (filter.Stille && analyse.StimmMs < SprachAnalyse.MinSprachMs) return "";
        var hochladen = filter.Stille ? analyse.Zugeschnitten(samples) : samples;
        var a = filter.Stille ? analyse.NachZuschnitt() : analyse;
        var wav = Wav.Kodiere(hochladen, 16000);
        if (wav.Length > 25 * 1024 * 1024) throw new InvalidOperationException("Die Aufnahme ist für Groq zu groß.");
        var o = await Einmal(wav, modell, prompt, schluessel);
        // Whisper gibt bei Gemurmel gern den Prompt selbst aus: dann einmal ohne Wörterbuch.
        if (prompt != null && IstPromptEcho(o.Text("text") ?? "", prompt))
        {
            Log("sprache", "Groq: Prompt-Echo erkannt, erneut ohne Wörterbuch");
            o = await Einmal(wav, modell, null, schluessel);
        }
        return Filtere(o, a, filter);
    }

    private static async Task<JSONObjekt> Einmal(byte[] wav, string gewaehlt, string? prompt, string schluessel)
    {
        var modell = gewaehlt == TranskriptionsEinstellungen.ModellLargeV3 && LargeV3Gesperrt ? TranskriptionsEinstellungen.ModellTurbo : gewaehlt;
        var (code, daten) = await Sende(wav, modell, prompt, schluessel);
        var text = Encoding.UTF8.GetString(daten, 0, Math.Min(daten.Length, 600));
        if (code != 200 && modell == TranskriptionsEinstellungen.ModellLargeV3 && (code == 403 || text.Contains("model_permission_blocked")))
        {
            lock (SperreLock) gesperrtBis = DateTime.Now.AddMinutes(30);
            Log("sprache", "Groq: Large V3 für die Organisation gesperrt – 30 Minuten Turbo");
            modell = TranskriptionsEinstellungen.ModellTurbo;
            (code, daten) = await Sende(wav, modell, prompt, schluessel);
        }
        var o = code == 200 ? JSON.Objekt(daten) : null;
        if (o == null) throw new InvalidOperationException($"Groq {code}: {Encoding.UTF8.GetString(daten, 0, Math.Min(daten.Length, 300))}");
        return o;
    }

    private static async Task<(int, byte[])> Sende(byte[] wav, string modell, string? prompt, string schluessel)
    {
        var grenze = Guid.NewGuid().ToString().ToUpperInvariant();
        var felder = new List<(string, string)> { ("model", modell), ("language", "de"), ("response_format", "verbose_json"), ("temperature", "0") };
        if (prompt != null) felder.Add(("prompt", prompt));
        using var rumpf = new MemoryStream(wav.Length + 2048);
        void Schreibe(string s) { var b = Encoding.UTF8.GetBytes(s); rumpf.Write(b, 0, b.Length); }
        foreach (var (k, v) in felder) Schreibe($"--{grenze}\r\nContent-Disposition: form-data; name=\"{k}\"\r\n\r\n{v}\r\n");
        Schreibe($"--{grenze}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"aufnahme.wav\"\r\nContent-Type: audio/wav\r\n\r\n");
        rumpf.Write(wav, 0, wav.Length);
        Schreibe($"\r\n--{grenze}--\r\n");
        using var req = new HttpRequestMessage(HttpMethod.Post, "https://api.groq.com/openai/v1/audio/transcriptions");
        req.Headers.TryAddWithoutValidation("Authorization", $"Bearer {schluessel}");
        req.Content = new ByteArrayContent(rumpf.ToArray());
        req.Content.Headers.TryAddWithoutValidation("Content-Type", $"multipart/form-data; boundary={grenze}");
        using var frist = new CancellationTokenSource(TimeSpan.FromSeconds(60));
        try
        {
            using var antwort = await SprachHttp.Client.SendAsync(req, frist.Token);
            return ((int)antwort.StatusCode, await antwort.Content.ReadAsByteArrayAsync(frist.Token));
        }
        catch (OperationCanceledException)
        {
            throw new TimeoutException("Groq hat nicht rechtzeitig geantwortet.");
        }
    }

    /// <summary>Schichten 2–4 wie WhisperHallucinationFilter.kt.</summary>
    private static string Filtere(JSONObjekt o, SprachAnalyse analyse, Filter f)
    {
        var gesamt = (o.Text("text") ?? "").Trim();
        var segmente = o.Objekte("segments");
        if (!f.Segmente || segmente.Count == 0) return FloskelSperre(gesamt, analyse, f);
        static double D(JSONObjekt s, string k) => s.Zahl(k) ?? 0;
        var sicher = segmente.Where(s =>
        {
            double stille = D(s, "no_speech_prob"), logp = D(s, "avg_logprob"), komp = D(s, "compression_ratio");
            var dauer = D(s, "end") - D(s, "start");
            if (stille > 0.6 && logp < -1.0) return false;
            if (komp > 2.4) return false;
            return !(dauer > 0 && dauer < 0.4 && stille > 0.6);
        }).ToList();
        if (sicher.Count == 0) return "";
        var behalten = sicher;
        if (f.Zeitstempel)
        {
            var passend = sicher.Where(s => s.Zahl("start") == null || s.Zahl("end") == null
                || analyse.SegmentHatSprache(D(s, "start"), D(s, "end"))).ToList();
            if (passend.Count > 0) behalten = passend;   // alle verworfen = vermutlich Zeitversatz, Schicht 2 behalten
        }
        var text = string.Join(" ", behalten.Select(s => s.Text("text")?.Trim(' ', '\t')).Where(t => t != null));
        return FloskelSperre(text.Trim(), analyse, f);
    }

    private static string FloskelSperre(string text, SprachAnalyse analyse, Filter f)
    {
        if (!f.Floskeln || text.Length == 0 || text.Length > 64) return text;
        var n = new StringBuilder();
        foreach (var z in text.ToLowerInvariant()) n.Append(char.IsLetter(z) ? z : ' ');
        var teile = n.ToString().Split(' ', StringSplitOptions.RemoveEmptyEntries);
        var norm = string.Join(" ", teile);
        if (norm.Length == 0 || teile.Length > 8) return text;
        if (KurzeFloskeln.Contains(norm)) return analyse.StimmMs < 1500 ? "" : text;
        if (!FloskelListe.Contains(norm)) return text;
        return analyse.StimmMs < 600 ? "" : text;
    }

    public static bool IstPromptEcho(string text, string prompt)
    {
        static string Norm(string s) => new(s.ToLowerInvariant().Where(char.IsLetterOrDigit).ToArray());
        string t = Norm(text), p = Norm(prompt), marker = Norm("Begriffe");
        if (t.Length == 0 || p.Length == 0) return false;
        if (t.StartsWith(marker, StringComparison.Ordinal) && p.Contains(t[marker.Length..])) return true;
        return t.Length >= 12 && p.Contains(t);
    }
}

public static class Wav
{
    public static byte[] Kodiere(float[] samples, int rate)
    {
        var bytes = samples.Length * 2;
        var d = new byte[44 + bytes];
        void S(int i, string t) => Encoding.ASCII.GetBytes(t).CopyTo(d, i);
        void U32(int i, int v) => BitConverter.TryWriteBytes(d.AsSpan(i), (uint)v);
        void U16(int i, int v) => BitConverter.TryWriteBytes(d.AsSpan(i), (ushort)v);
        S(0, "RIFF"); U32(4, 36 + bytes); S(8, "WAVE");
        S(12, "fmt "); U32(16, 16); U16(20, 1); U16(22, 1); U32(24, rate); U32(28, rate * 2); U16(32, 2); U16(34, 16);
        S(36, "data"); U32(40, bytes);
        for (int i = 0; i < samples.Length; i++)
            BitConverter.TryWriteBytes(d.AsSpan(44 + i * 2), (short)(Math.Clamp(samples[i], -1f, 1f) * 32767));
        return d;
    }

    /// <summary>Liest PCM16-Mono aus einer WAV-Datei (sucht den data-Chunk). Gibt Samples und Rate zurück.</summary>
    public static (float[] Samples, double Rate)? Lies(byte[] b)
    {
        if (b.Length <= 44 || Encoding.ASCII.GetString(b, 0, 4) != "RIFF") return null;
        int Le32(int i) => b[i] | b[i + 1] << 8 | b[i + 2] << 16 | b[i + 3] << 24;
        var rate = 24000.0;
        var i = 12;
        while (i + 8 <= b.Length)
        {
            var id = Encoding.ASCII.GetString(b, i, 4);
            var len = Le32(i + 4);
            if (len < 0) return null;
            if (id == "fmt " && i + 16 <= b.Length) rate = Le32(i + 12);
            if (id == "data")
            {
                var start = i + 8;
                var ende = (int)Math.Min(b.Length, (long)start + len);
                var s = new float[Math.Max(0, (ende - start) / 2)];
                for (int j = 0; j < s.Length; j++) s[j] = BitConverter.ToInt16(b, start + j * 2) / 32768f;
                return (s, rate);
            }
            var weiter = (long)i + 8 + len + (len % 2);
            if (weiter > int.MaxValue) return null;
            i = (int)weiter;
        }
        return null;
    }
}

// MARK: - Töne

public static class Toene
{
    /// <summary>Einmal erzeugt und geladen: Der Piepton soll sofort kommen. Gegenstück zu „Tink“: heller, kurzer Klang.</summary>
    private static readonly SoundPlayer? Piep = Erzeuge();

    private static SoundPlayer? Erzeuge()
    {
        try
        {
            const int rate = 44100;
            var n = rate * 110 / 1000;
            var s = new float[n];
            for (int i = 0; i < n; i++)
            {
                var t = (double)i / rate;
                var anfang = Math.Min(1.0, i / (rate * 0.002));
                s[i] = (float)(anfang * Math.Exp(-t * 42) * (0.34 * Math.Sin(2 * Math.PI * 2093 * t) + 0.16 * Math.Sin(2 * Math.PI * 4186 * t)));
            }
            var p = new SoundPlayer(new MemoryStream(Wav.Kodiere(s, rate)));
            p.Load();
            return p;
        }
        catch { return null; }
    }

    public static void Start()
    {
        try { Piep?.Stop(); Piep?.Play(); } catch { }
    }
}
