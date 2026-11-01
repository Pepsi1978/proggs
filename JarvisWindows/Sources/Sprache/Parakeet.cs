using System.Diagnostics;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text.RegularExpressions;
using NAudio.Wave;
using SherpaOnnx;

namespace JarvisWindows;

public sealed class ParakeetError : Exception
{
    public ParakeetError(string text) : base(text) { }
    public static ParakeetError NotInstalled => new("Offline-Modell fehlt — in den Einstellungen \"Offline-Modell laden\" klicken");
    public static ParakeetError LoadFailed => new("Offline-Modell ließ sich nicht laden");
    public static ParakeetError AudioUnreadable => new("Aufnahme für die Offline-Transkription nicht lesbar");
    public static ParakeetError Download(string s) => new($"Download des Offline-Modells fehlgeschlagen: {s}");
}

/// <summary>Gemessene Werte wie in den Handy-Einstellungen (Laden/Aufwärmen, Arbeitsspeicher, letzte Mitschrift).</summary>
public sealed record ParakeetMessung(int LadeMs = 0, int RamMb = 0, int AudioMs = 0, int RechenMs = 0);

/// <summary>Offline-Transkription mit parakeet-primeline: deutscher Fine-Tune von nvidia/parakeet-tdt-0.6b-v3,
/// int8-ONNX über sherpa-onnx (NuGet org.k2fsa.sherpa.onnx), nur CPU mit 4 Threads. Hugging Face flozen1981/parakeet-primeline-onnx in der
/// festgelegten Revision. Die Dateien (rund 640 MB) liegen einmal pro Rechner unter %LOCALAPPDATA%\VoiceOverlays\ und werden mit TVO und CVO geteilt.
///
/// Nach dem einmaligen Download kein Netz, kein Schlüssel. Fehlt das Modell oder scheitert es, gibt es einen Fehler, nie einen Rückfall auf Groq oder Gemini.
/// Lizenz CC-BY-4.0: primeline (Fine-Tune), NVIDIA (Basismodell), k2-fsa/sherpa-onnx (Laufzeit).
/// Grundlage: TerminalVoiceOverlay-Windows/Services/ParakeetLocalClient.cs. Ergänzt: Mitschrift direkt aus Samples und Messwerte für die Einstellungen.</summary>
public sealed class ParakeetLocalClient
{
    public static readonly ParakeetLocalClient Shared = new();

    public const string Repo = "flozen1981/parakeet-primeline-onnx";
    public const string Revision = "d548e25b9bfe559aa274f361892dc4ed5d64743a";

    /// <summary>Dateien mit ihrer Größe in genau dieser Revision; die Größe dient als Vollständigkeitsprüfung.</summary>
    public static readonly (string Name, long Size)[] Files =
    {
        ("tokens.txt", 102_132),
        ("decoder.int8.onnx", 11_845_275),
        ("joiner.int8.onnx", 6_355_277),
        ("encoder.int8.onnx", 1_548_009),
        // Die Gewichte des Encoders; sie müssen neben encoder.int8.onnx liegen.
        ("encoder.int8.onnx.data", 650_776_320),
    };

    public static long TotalBytes => Files.Sum(f => f.Size);

    public static string ModelDir => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "VoiceOverlays", "parakeet-primeline-onnx");

    public static bool IsInstalled => Files.All(f =>
    {
        var p = Path.Combine(ModelDir, f.Name);
        return File.Exists(p) && new FileInfo(p).Length == f.Size;
    });

    private const int SampleRate = 16_000;
    private const int Threads = 4;
    /// <summary>Der Encoder scheitert über 400 s und braucht davor quadratisch mehr Speicher: längere Diktate in Stücken.</summary>
    private const int MaxChunkS = 90;
    private const int RetryChunkS = 20;
    /// <summary>Leise Aufnahmen kommen sonst leer zurück: auf diese Spitze anheben, höchstens um MaxGain.</summary>
    private const float TargetPeak = 0.5f;
    private const float MaxGain = 30f;
    /// <summary>Am echten Modell gemessen: 1,5 ließ „Jarvis“ als „Travis“ stehen, ab 4 verbiegt es Nachbarwörter.</summary>
    private const float HotwordsScore = 3f;
    /// <summary>Wie am Handy: mehr als 100 Begriffe gehen nicht in die Beam-Suche.</summary>
    private const int MaxHotwords = 100;
    /// <summary>Nach so langer Ruhe wird der Arbeitsspeicher wieder frei (TVO und CVO laufen gleichzeitig). Das Diktat lädt beim Aufnahmestart vor.</summary>
    private static readonly TimeSpan IdleUnload = TimeSpan.FromMinutes(15);

    private readonly SemaphoreSlim _gate = new(1, 1);
    private OfflineRecognizer? _recognizer;
    private Timer? _idleTimer;
    /// <summary>Nur mit bpe.vocab läuft die Beam-Suche, die allein Wortlisten beachtet.</summary>
    private bool _hotwordsAvailable;
    /// <summary>Solange das Weckwort lauscht, bleibt das Modell geladen: Sonst käme die Antwort auf „Hey Jarvis“ erst nach dem Neuladen.</summary>
    private int _halten;

    // Die NuGet-Bindung 1.13.8 kennt nur CreateStream() ohne Wortliste; die C-API exportiert die Variante mit
    // Wortliste aber. Der Griff des Erkenners ist in der Bindung privat, darum per Reflection (Version fest im csproj).
    [DllImport("sherpa-onnx-c-api")]
    private static extern IntPtr SherpaOnnxCreateOfflineStreamWithHotwords(IntPtr recognizer, [MarshalAs(UnmanagedType.LPUTF8Str)] string hotwords);

    private static readonly FieldInfo? RecognizerHandleField = typeof(OfflineRecognizer).GetField("_handle", BindingFlags.NonPublic | BindingFlags.Instance);

    /// <summary>Begriffe im Format von sherpa-onnx: durch „/“ getrennt; „/“, „:“ und „#“ haben dort eine eigene Bedeutung und werden
    /// zu Leerzeichen. Doppelte (ohne Groß/Klein) fallen weg, höchstens 100 Begriffe.</summary>
    public static string Hotwords(string aus)
    {
        var gesehen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var begriffe = new List<string>();
        foreach (var teil in aus.Split('\n', '\r', ',', ';'))
        {
            var begriff = Regex.Replace(teil, "[/:#]", " ").Trim(' ', '\t');
            if (begriff.Length == 0 || !gesehen.Add(begriff)) continue;
            begriffe.Add(begriff);
            if (begriffe.Count >= MaxHotwords) break;
        }
        return string.Join("/", begriffe);
    }

    /// <summary>Lädt das Modell im Hintergrund vor (beim Aufnahmestart), damit das Diktat nicht wartet.</summary>
    public void Preload()
    {
        if (!IsInstalled) return;
        _ = Task.Run(async () =>
        {
            await _gate.WaitAsync().ConfigureAwait(false);
            try { LoadIfNeeded(); ScheduleUnload(); }
            catch (Exception ex) { Log("fehler", $"Parakeet: {ex.Message}"); }
            finally { _gate.Release(); }
        });
    }

    /// <summary>Lädt das Modell und wartet darauf; wirft, wenn es fehlt oder scheitert (Weckwort).</summary>
    public async Task Laden()
    {
        await _gate.WaitAsync().ConfigureAwait(false);
        try { await Task.Run(() => { LoadIfNeeded(); ScheduleUnload(); }).ConfigureAwait(false); }
        finally { _gate.Release(); }
    }

    /// <summary>Modell im Speicher halten (true) oder wieder freigeben lassen (false). Aufrufe zählen.</summary>
    public void Halten(bool an)
    {
        var n = Interlocked.Add(ref _halten, an ? 1 : -1);
        if (n <= 0) { Interlocked.Exchange(ref _halten, 0); ScheduleUnload(); }
    }

    /// <summary>Mitschrift einer fertigen Aufnahme, ganz auf diesem Rechner.</summary>
    public async Task<string> Transcribe(string fileURL, string hotwords = "")
    {
        await _gate.WaitAsync().ConfigureAwait(false);
        try
        {
            return await Task.Run(() =>
            {
                var r = LoadIfNeeded();
                return Rechne(r, ReadSamples(fileURL), hotwords);
            }).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            Log("fehler", $"Parakeet: {ex.Message}");
            throw;
        }
        finally { _gate.Release(); }
    }

    /// <summary>Mitschrift aus 16-kHz-Mono-Samples (vom Diktat), ganz auf diesem Rechner. `hotwords`: Wortliste je Aufnahme aus `Hotwords(aus:)`; leer = ohne Wortliste.</summary>
    public async Task<string> Transkribiere(float[] samples, string hotwords = "")
    {
        await _gate.WaitAsync().ConfigureAwait(false);
        try
        {
            return await Task.Run(() => Rechne(LoadIfNeeded(), samples, hotwords)).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            Log("fehler", $"Parakeet: {ex.Message}");
            throw;
        }
        finally { _gate.Release(); }
    }

    /// <summary>Kurzer Ausschnitt fürs Weckwort: ohne Messung und ohne Logbuch, das wäre sonst bei jedem Geräusch im Raum ein Eintrag.</summary>
    public async Task<string> Kurzmitschrift(float[] samples, string hotwords = "")
    {
        await _gate.WaitAsync().ConfigureAwait(false);
        try
        {
            return await Task.Run(() =>
            {
                var r = LoadIfNeeded();
                var text = Decode(r, samples, _hotwordsAvailable ? hotwords : "");
                ScheduleUnload();
                return text;
            }).ConfigureAwait(false);
        }
        finally { _gate.Release(); }
    }

    private string Rechne(OfflineRecognizer r, float[] samples, string hotwords)
    {
        var sw = Stopwatch.StartNew();
        var liste = _hotwordsAvailable ? hotwords : "";
        var parts = new List<string>();
        foreach (var chunk in Split(samples, MaxChunkS))
        {
            var text = Decode(r, chunk, liste);
            // Ein langes Stück kommt gelegentlich leer zurück; kurze Teile derselben Aufnahme klappen.
            if (text.Length == 0 && chunk.Length > RetryChunkS * SampleRate)
                text = string.Join(" ", Split(chunk, RetryChunkS).Select(c => Decode(r, c, liste)).Where(t => t.Length > 0));
            if (text.Length > 0) parts.Add(text);
        }
        var ms = (int)sw.ElapsedMilliseconds;
        var audioMs = (int)(samples.Length * 1000L / SampleRate);
        SetzeMessung(audioMs: audioMs, rechenMs: ms);
        Log("sprache", $"Parakeet: {audioMs} ms Aufnahme in {ms} ms gerechnet");
        ScheduleUnload();
        return string.Join(" ", parts);
    }

    private readonly object _messLock = new();
    private ParakeetMessung? _messung;
    public ParakeetMessung? Messung { get { lock (_messLock) return _messung; } }

    private void SetzeMessung(int? ladeMs = null, int? audioMs = null, int? rechenMs = null)
    {
        lock (_messLock)
        {
            var m = _messung ?? new ParakeetMessung();
            if (ladeMs != null) m = m with { LadeMs = ladeMs.Value, RamMb = ArbeitsspeicherMb() };
            if (audioMs != null) m = m with { AudioMs = audioMs.Value };
            if (rechenMs != null) m = m with { RechenMs = rechenMs.Value };
            _messung = m;
        }
    }

    private static int ArbeitsspeicherMb()
    {
        using var p = Process.GetCurrentProcess();
        return (int)(p.WorkingSet64 / 1_048_576);
    }

    /// <summary>Belegter Platz der Modelldateien.</summary>
    public static long BelegteBytes => Files.Sum(f =>
    {
        var p = Path.Combine(ModelDir, f.Name);
        return File.Exists(p) ? new FileInfo(p).Length : 0L;
    });

    private OfflineRecognizer LoadIfNeeded()
    {
        if (_recognizer != null) return _recognizer;
        if (!IsInstalled) throw ParakeetError.NotInstalled;
        var sw = Stopwatch.StartNew();
        var cfg = new OfflineRecognizerConfig();
        cfg.FeatConfig.SampleRate = SampleRate;
        cfg.FeatConfig.FeatureDim = 128;
        cfg.ModelConfig.Transducer.Encoder = Path.Combine(ModelDir, "encoder.int8.onnx");
        cfg.ModelConfig.Transducer.Decoder = Path.Combine(ModelDir, "decoder.int8.onnx");
        cfg.ModelConfig.Transducer.Joiner = Path.Combine(ModelDir, "joiner.int8.onnx");
        cfg.ModelConfig.Tokens = Path.Combine(ModelDir, "tokens.txt");
        cfg.ModelConfig.NumThreads = Threads;
        cfg.ModelConfig.Provider = "cpu";
        cfg.ModelConfig.ModelType = "nemo_transducer";
        // Vokabular des Modells: zerlegt die Begriffe der Wortliste in Modell-Token. Liegt im Programmordner, nicht im
        // Modell-Download. Die Liste selbst kommt je Aufnahme über den Stream, darum kein HotwordsFile.
        var bpe = Pfade.Ressource("bpe.vocab");
        _hotwordsAvailable = File.Exists(bpe);
        if (_hotwordsAvailable)
        {
            cfg.ModelConfig.BpeVocab = bpe;
            cfg.ModelConfig.ModelingUnit = "bpe";
            cfg.DecodingMethod = "modified_beam_search";
            cfg.HotwordsScore = HotwordsScore;
        }
        else
        {
            Log("fehler", "Parakeet: bpe.vocab fehlt im Programmordner, Wörterbuch wirkt nicht");
            cfg.DecodingMethod = "greedy_search";
        }
        OfflineRecognizer r;
        try { r = new OfflineRecognizer(cfg); }
        catch (Exception ex) { Log("fehler", $"Parakeet: {ex.Message}"); throw ParakeetError.LoadFailed; }
        // Aufwärmen: Die erste echte Mitschrift wäre sonst etwa doppelt so langsam.
        for (int i = 0; i < 2; i++) Decode(r, new float[SampleRate]);
        _recognizer = r;
        var ladeMs = (int)sw.ElapsedMilliseconds;
        SetzeMessung(ladeMs: ladeMs);
        Log("sprache", $"Parakeet geladen in {ladeMs} ms ({Threads} Threads)");
        return r;
    }

    private void ScheduleUnload()
    {
        _idleTimer?.Dispose();
        _idleTimer = new Timer(_ =>
        {
            if (Volatile.Read(ref _halten) > 0) return;      // Das Ende des Haltens stellt den Zeitgeber neu.
            if (!_gate.Wait(0)) return;                       // gerade in Arbeit: der nächste Lauf stellt den Zeitgeber neu
            try
            {
                if (_recognizer == null) return;
                _recognizer.Dispose();
                _recognizer = null;
                Log("sprache", "Parakeet entladen (Ruhe)");
            }
            finally { _gate.Release(); }
        }, null, IdleUnload, Timeout.InfiniteTimeSpan);
    }

    /// <summary>Leere Liste = normaler Stream. Scheitert die Wortliste, wird trotzdem transkribiert.</summary>
    private static OfflineStream CreateStream(OfflineRecognizer r, string hotwords)
    {
        if (hotwords.Length == 0) return r.CreateStream();
        try
        {
            var handle = ((HandleRef)RecognizerHandleField!.GetValue(r)!).Handle;
            var stream = SherpaOnnxCreateOfflineStreamWithHotwords(handle, hotwords);
            GC.KeepAlive(r);
            if (stream != IntPtr.Zero) return new OfflineStream(stream);
            Log("fehler", "Parakeet: Wortliste ohne Wirkung (kein Stream)");
        }
        catch (Exception ex)
        {
            Log("fehler", $"Parakeet: Wortliste ohne Wirkung ({ex.Message})");
        }
        return r.CreateStream();
    }

    private static string Decode(OfflineRecognizer r, float[] chunk, string hotwords = "")
    {
        float peak = 0;
        foreach (var v in chunk) peak = Math.Max(peak, Math.Abs(v));
        var input = chunk;
        if (peak > 0 && peak < TargetPeak)
        {
            var gain = Math.Min(TargetPeak / peak, MaxGain);
            input = chunk.Select(v => v * gain).ToArray();
        }
        using var stream = CreateStream(r, hotwords);
        stream.AcceptWaveform(SampleRate, input);
        r.Decode(stream);
        return (stream.Result.Text ?? "").Trim();
    }

    /// <summary>WAV lesen, auf mono 16 kHz Float bringen.</summary>
    private static float[] ReadSamples(string path)
    {
        try
        {
            using var reader = new AudioFileReader(path);
            var fmt = reader.WaveFormat;
            var all = new List<float>((int)Math.Min(int.MaxValue, reader.Length / 4));
            var buf = new float[fmt.SampleRate * fmt.Channels];
            int n;
            while ((n = reader.Read(buf, 0, buf.Length)) > 0)
            {
                if (fmt.Channels == 1) { for (int i = 0; i < n; i++) all.Add(buf[i]); }
                else
                {
                    for (int i = 0; i + fmt.Channels <= n; i += fmt.Channels)
                    {
                        float s = 0;
                        for (int c = 0; c < fmt.Channels; c++) s += buf[i + c];
                        all.Add(s / fmt.Channels);
                    }
                }
            }
            var mono = all.ToArray();
            if (fmt.SampleRate == SampleRate) return mono;
            return new Umwandler(SampleRate).Wandle(new MikroPuffer(mono, fmt.SampleRate)) ?? throw ParakeetError.AudioUnreadable;
        }
        catch (ParakeetError) { throw; }
        catch { throw ParakeetError.AudioUnreadable; }
    }

    /// <summary>Stücke von höchstens maxS Sekunden, geschnitten an der leisesten Stelle kurz vor dem Ende.</summary>
    private static List<float[]> Split(float[] samples, int maxS)
    {
        int maxN = maxS * SampleRate, searchN = Math.Min(15, maxS / 2) * SampleRate, winN = SampleRate * 2 / 5;
        var parts = new List<float[]>();
        int begin = 0;
        while (samples.Length - begin > maxN)
        {
            int best = begin + maxN - searchN;
            double bestEnergy = double.MaxValue;
            for (int p = best; p + winN <= begin + maxN; p += winN)
            {
                double e = 0;
                for (int i = p; i < p + winN; i++) e += samples[i] * samples[i];
                if (e < bestEnergy) { bestEnergy = e; best = p; }
            }
            int cut = best + winN / 2;
            parts.Add(samples[begin..cut]);
            begin = cut;
        }
        parts.Add(samples[begin..]);
        return parts;
    }

    /// <summary>Lädt die Modelldateien der festgelegten Revision (einmalig, rund 640 MB). Fertige Dateien werden übersprungen;
    /// jede Datei landet erst als Ganzes im Zielordner. progress(geladen, gesamt) und completion kommen auf dem Hauptfaden.</summary>
    public static void Download(Action<long, long> progress, Action<Exception?> completion)
    {
        _ = Task.Run(async () =>
        {
            try
            {
                Directory.CreateDirectory(ModelDir);
                using var http = new HttpClient(SprachHttp.NeuerHandler()) { Timeout = Timeout.InfiniteTimeSpan };
                long done = 0;
                foreach (var (name, size) in Files)
                {
                    var target = Path.Combine(ModelDir, name);
                    if (File.Exists(target) && new FileInfo(target).Length == size)
                    {
                        done += size;
                        var d0 = done;
                        Haupt.Post(() => progress(d0, TotalBytes));
                        continue;
                    }
                    var tmp = Path.Combine(ModelDir, $"dl-{Guid.NewGuid():N}.part");
                    try
                    {
                        var url = $"https://huggingface.co/{Repo}/resolve/{Revision}/{name}";
                        using (var resp = await http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead).ConfigureAwait(false))
                        {
                            if (!resp.IsSuccessStatusCode) throw ParakeetError.Download($"HTTP {(int)resp.StatusCode}");
                            await using var src = await resp.Content.ReadAsStreamAsync().ConfigureAwait(false);
                            await using var dst = File.Create(tmp);
                            var buf = new byte[1 << 18];
                            long got = 0, last = 0;
                            int n;
                            while ((n = await src.ReadAsync(buf).ConfigureAwait(false)) > 0)
                            {
                                await dst.WriteAsync(buf.AsMemory(0, n)).ConfigureAwait(false);
                                got += n;
                                if (got - last > 4 << 20)
                                {
                                    last = got;
                                    var d1 = done + got;
                                    Haupt.Post(() => progress(d1, TotalBytes));
                                }
                            }
                        }
                        if (new FileInfo(tmp).Length != size) throw ParakeetError.Download($"{name} unvollständig");
                        File.Move(tmp, target, overwrite: true);
                    }
                    finally
                    {
                        if (File.Exists(tmp)) File.Delete(tmp);
                    }
                    done += size;
                    var d2 = done;
                    Haupt.Post(() => progress(d2, TotalBytes));
                }
                Log("sprache", $"Parakeet-Modell geladen ({TotalBytes / 1_048_576} MB)");
                Haupt.Post(() => completion(null));
            }
            catch (Exception ex)
            {
                Haupt.Post(() => completion(ex is ParakeetError ? ex : ParakeetError.Download(ex.Message)));
            }
        });
    }
}
