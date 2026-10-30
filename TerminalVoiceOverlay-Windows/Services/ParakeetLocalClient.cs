using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using NAudio.Wave;
using SherpaOnnx;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Offline-Transkription mit parakeet-primeline: deutscher Fine-Tune von nvidia/parakeet-tdt-0.6b-v3,
    /// int8-ONNX ueber sherpa-onnx (NuGet org.k2fsa.sherpa.onnx), nur CPU mit 4 Threads. Genau das Modell aus
    /// github.com/winidi/dictate: Hugging Face flozen1981/parakeet-primeline-onnx in der dort festgelegten
    /// Revision. Die Dateien (rund 640 MB) liegen einmal pro Rechner unter %LOCALAPPDATA%\VoiceOverlays\ und
    /// werden von TVO und CVO geteilt.
    ///
    /// Nach dem einmaligen Download kein Netz, kein Schluessel. Fehlt das Modell oder scheitert es, gibt es
    /// einen Fehler, nie einen Rueckfall auf Groq oder Gemini.
    /// Lizenz CC-BY-4.0: primeline (Fine-Tune), NVIDIA (Basismodell), k2-fsa/sherpa-onnx (Laufzeit).
    ///
    /// Gleiche Datei in TerminalVoiceOverlay-Windows und ClaudeVoiceOverlay-Windows (nur der Namespace weicht ab).
    /// </summary>
    public sealed class ParakeetLocalClient
    {
        public static readonly ParakeetLocalClient Shared = new();

        private const string Repo = "flozen1981/parakeet-primeline-onnx";
        private const string Revision = "d548e25b9bfe559aa274f361892dc4ed5d64743a";

        /// <summary>Dateien mit ihrer Groesse in genau dieser Revision; die Groesse dient als Vollstaendigkeitspruefung.</summary>
        public static readonly (string Name, long Size)[] Files =
        {
            ("tokens.txt", 102_132),
            ("decoder.int8.onnx", 11_845_275),
            ("joiner.int8.onnx", 6_355_277),
            ("encoder.int8.onnx", 1_548_009),
            // Die Gewichte des Encoders; sie muessen neben encoder.int8.onnx liegen.
            ("encoder.int8.onnx.data", 650_776_320),
        };

        public static long TotalBytes => Files.Sum(f => f.Size);

        public static string ModelDir => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "VoiceOverlays", "parakeet-primeline-onnx");

        public static bool IsInstalled => Files.All(f =>
        {
            var p = Path.Combine(ModelDir, f.Name);
            return File.Exists(p) && new FileInfo(p).Length == f.Size;
        });

        private const int SampleRate = 16_000;
        private const int Threads = 4;
        /// <summary>Der Encoder scheitert ueber 400 s und braucht davor quadratisch mehr Speicher: laengere Diktate in Stuecken.</summary>
        private const int MaxChunkS = 90;
        private const int RetryChunkS = 20;
        /// <summary>Leise Aufnahmen kommen sonst leer zurueck: auf diese Spitze anheben, hoechstens um MaxGain.</summary>
        private const float TargetPeak = 0.5f;
        private const float MaxGain = 30f;
        /// <summary>Am echten Modell gemessen: 1,5 liess "Jarvis" als "Travis" stehen, ab 4 verbiegt es Nachbarwoerter.</summary>
        private const float HotwordsScore = 3f;
        /// <summary>Nach so langer Ruhe wird der Arbeitsspeicher wieder frei (TVO und CVO laufen gleichzeitig).</summary>
        private static readonly TimeSpan IdleUnload = TimeSpan.FromMinutes(15);

        private readonly SemaphoreSlim _gate = new(1, 1);
        private OfflineRecognizer? _recognizer;
        private Timer? _idleTimer;
        /// <summary>Nur mit bpe.vocab neben der .exe laeuft die Beam-Suche, die allein Wortlisten beachtet.</summary>
        private bool _hotwordsAvailable;

        // Die NuGet-Bindung 1.13.8 kennt nur CreateStream() ohne Wortliste; die C-API exportiert die Variante mit
        // Wortliste aber. Der Griff des Erkenners ist in der Bindung privat, darum per Reflection (Version fest im csproj).
        [DllImport("sherpa-onnx-c-api")]
        private static extern IntPtr SherpaOnnxCreateOfflineStreamWithHotwords(
            IntPtr recognizer, [MarshalAs(UnmanagedType.LPUTF8Str)] string hotwords);

        private static readonly FieldInfo? RecognizerHandleField =
            typeof(OfflineRecognizer).GetField("_handle", BindingFlags.NonPublic | BindingFlags.Instance);

        /// <summary>Laedt das Modell im Hintergrund vor (beim Aufnahmestart), damit das Diktat nicht wartet.</summary>
        public void Preload()
        {
            if (!IsInstalled) return;
            _ = Task.Run(async () =>
            {
                await _gate.WaitAsync().ConfigureAwait(false);
                try { LoadIfNeeded(); }
                catch (Exception ex) { DiagLog.Warn("STT", "parakeet_preload_failed", ("error", ex.Message)); }
                finally { _gate.Release(); }
            });
        }

        /// <summary>Mitschrift einer fertigen Aufnahme, ganz auf diesem Rechner.</summary>
        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            await _gate.WaitAsync().ConfigureAwait(false);
            try
            {
                return await Task.Run(() =>
                {
                    var r = LoadIfNeeded();
                    var samples = ReadSamples(wavFilePath);
                    // Je Aufnahme frisch gelesen: Aenderungen in den Einstellungen wirken sofort, ohne neu zu laden.
                    var hotwords = _hotwordsAvailable ? ParakeetVocabularySetting.Hotwords() : string.Empty;
                    var sw = Stopwatch.StartNew();
                    var parts = new List<string>();
                    foreach (var chunk in Split(samples, MaxChunkS))
                    {
                        var text = Decode(r, chunk, hotwords);
                        // Ein langes Stueck kommt gelegentlich leer zurueck; kurze Teile derselben Aufnahme klappen.
                        if (text.Length == 0 && chunk.Length > RetryChunkS * SampleRate)
                            text = string.Join(" ", Split(chunk, RetryChunkS).Select(c => Decode(r, c, hotwords)).Where(t => t.Length > 0));
                        if (text.Length > 0) parts.Add(text);
                    }
                    DiagLog.Write("STT", "parakeet_decode",
                        ("audio_ms", samples.Length * 1000L / SampleRate), ("ms", sw.ElapsedMilliseconds),
                        ("hotwords", hotwords.Length == 0 ? 0 : hotwords.Split('/').Length));
                    ScheduleUnload();
                    return string.Join(" ", parts);
                }).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                DiagLog.Warn("STT", "parakeet_failed", ("error", ex.Message));
                throw;
            }
            finally { _gate.Release(); }
        }

        private OfflineRecognizer LoadIfNeeded()
        {
            if (_recognizer is not null) return _recognizer;
            if (!IsInstalled)
                throw new InvalidOperationException("Offline-Modell fehlt — in den Einstellungen \"Offline-Modell laden\" klicken.");
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
            // Vokabular des Modells: zerlegt die Begriffe der Wortliste in Modell-Token. Liegt neben der .exe, nicht im
            // Modell-Download. Die Liste selbst kommt je Aufnahme ueber den Stream, darum kein HotwordsFile.
            var bpe = Path.Combine(AppContext.BaseDirectory, "bpe.vocab");
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
                DiagLog.Warn("STT", "parakeet_bpe_vocab_missing", ("path", bpe));
                cfg.DecodingMethod = "greedy_search";
            }
            var r = new OfflineRecognizer(cfg);
            // Aufwaermen: Die erste echte Mitschrift waere sonst etwa doppelt so langsam.
            for (int i = 0; i < 2; i++) Decode(r, new float[SampleRate]);
            _recognizer = r;
            DiagLog.Write("STT", "parakeet_loaded", ("ms", sw.ElapsedMilliseconds), ("threads", Threads));
            return r;
        }

        private void ScheduleUnload()
        {
            _idleTimer?.Dispose();
            _idleTimer = new Timer(_ =>
            {
                if (!_gate.Wait(0)) return; // gerade in Arbeit: der naechste Lauf stellt den Zeitgeber neu
                try
                {
                    _recognizer?.Dispose();
                    _recognizer = null;
                    DiagLog.Write("STT", "parakeet_unloaded");
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
                DiagLog.Warn("STT", "parakeet_hotwords_failed", ("error", "kein Stream"));
            }
            catch (Exception ex)
            {
                DiagLog.Warn("STT", "parakeet_hotwords_failed", ("error", ex.Message));
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
            return fmt.SampleRate == SampleRate ? mono : Resample(mono, fmt.SampleRate);
        }

        /// <summary>Lineare Umrechnung auf 16 kHz (die Overlays nehmen ohnehin mit 16 kHz auf).</summary>
        private static float[] Resample(float[] src, int fromRate)
        {
            var len = (int)((long)src.Length * SampleRate / fromRate);
            var dst = new float[len];
            double step = (double)fromRate / SampleRate;
            for (int i = 0; i < len; i++)
            {
                double pos = i * step;
                int j = (int)pos;
                double f = pos - j;
                float a = src[Math.Min(j, src.Length - 1)], b = src[Math.Min(j + 1, src.Length - 1)];
                dst[i] = (float)(a + (b - a) * f);
            }
            return dst;
        }

        /// <summary>Stuecke von hoechstens maxS Sekunden, geschnitten an der leisesten Stelle kurz vor dem Ende.</summary>
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

        /// <summary>
        /// Laedt die Modelldateien der festgelegten Revision (einmalig, rund 640 MB). Fertige Dateien werden
        /// uebersprungen; jede Datei landet erst als Ganzes im Zielordner. progress(geladen, gesamt).
        /// </summary>
        public static async Task DownloadAsync(IProgress<(long Done, long Total)> progress, CancellationToken ct = default)
        {
            Directory.CreateDirectory(ModelDir);
            using var http = new HttpClient { Timeout = Timeout.InfiniteTimeSpan };
            long done = 0;
            foreach (var (name, size) in Files)
            {
                var target = Path.Combine(ModelDir, name);
                if (File.Exists(target) && new FileInfo(target).Length == size) { done += size; progress.Report((done, TotalBytes)); continue; }
                var tmp = Path.Combine(ModelDir, $"dl-{Guid.NewGuid():N}.part");
                try
                {
                    var url = $"https://huggingface.co/{Repo}/resolve/{Revision}/{name}";
                    using (var resp = await http.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, ct).ConfigureAwait(false))
                    {
                        resp.EnsureSuccessStatusCode();
                        await using var src = await resp.Content.ReadAsStreamAsync(ct).ConfigureAwait(false);
                        await using var dst = File.Create(tmp);
                        var buf = new byte[1 << 18];
                        long got = 0, last = 0;
                        int n;
                        while ((n = await src.ReadAsync(buf, ct).ConfigureAwait(false)) > 0)
                        {
                            await dst.WriteAsync(buf.AsMemory(0, n), ct).ConfigureAwait(false);
                            got += n;
                            if (got - last > 4 << 20) { last = got; progress.Report((done + got, TotalBytes)); }
                        }
                    }
                    if (new FileInfo(tmp).Length != size) throw new IOException($"{name} ist unvollstaendig.");
                    File.Move(tmp, target, overwrite: true);
                }
                finally
                {
                    if (File.Exists(tmp)) File.Delete(tmp);
                }
                done += size;
                progress.Report((done, TotalBytes));
            }
            DiagLog.Write("STT", "parakeet_model_downloaded", ("bytes", TotalBytes));
        }
    }
}
