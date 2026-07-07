using System;
using System.IO;
using System.Runtime.InteropServices;
using SherpaOnnx;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Wake-Word-Erkennung auf Basis von sherpa-onnx (KeywordSpotter, open-vocabulary KWS,
    /// 100 % offline, Apache-2.0). Kapselt das native Handle hinter <see cref="IWakeWordEngine"/>.
    ///
    /// Wissensbasis: Bug-Almanach + Best-Practices `*-wake-word.md`.
    /// - EIN persistenter OnlineStream fuer die Lebensdauer (Streaming-Modell mit Kontext) — #2/§1.
    /// - Decode-Loop `while (IsReady) Decode`, danach `Reset` nach Treffer — #21/#9.
    /// - 16 kHz mono float[-1,1]; PCM-Konvertierung via <see cref="PcmConverter"/> — §2.
    /// - Modell-Pfade ueber AppContext.BaseDirectory (nie Arbeitsverzeichnis) — #31/§6.
    /// - DLL-Suchpfad voranstellen gegen aelteres onnxruntime.dll in System32 — #1/§1.
    /// - Alle nativen Aufrufe in try/catch (kein App-Crash bei Geraete-/Modellfehlern) — §9.5.
    ///
    /// Faellt bei fehlendem Modell/Native-Fehler NICHT hart aus: der Konstruktor wirft eine
    /// klare Exception, die der Aufrufer faengt und Wake-Word deaktiviert (App laeuft weiter).
    /// </summary>
    public sealed class SherpaWakeWordEngine : IWakeWordEngine
    {
        public const int SampleRate = 16000;

        private readonly KeywordSpotter _spotter;
        private readonly OnlineStream _stream;
        private readonly object _gate = new();   // AcceptWaveform/Decode fuer denselben Stream serialisieren (§1)
        private bool _disposed;

        private static bool _dllDirSet;

        /// <summary>
        /// Erstellt die Engine. <paramref name="modelDir"/> enthaelt encoder.onnx, decoder.onnx,
        /// joiner.onnx und tokens.txt. <paramref name="keywordsFile"/> ist die (vor-tokenisierte)
        /// keywords.txt fuer das aktuell gewaehlte Weckwort — null = die gebundelte im modelDir.
        /// Wirft bei fehlenden Dateien oder Native-Init-Fehlern — der Aufrufer behandelt das (Wake-Word aus).
        /// </summary>
        public SherpaWakeWordEngine(string modelDir, int numThreads = 1, string? keywordsFile = null)
        {
            EnsureDllSearchPath();

            string encoder  = Path.Combine(modelDir, "encoder.onnx");
            string decoder  = Path.Combine(modelDir, "decoder.onnx");
            string joiner   = Path.Combine(modelDir, "joiner.onnx");
            string tokens   = Path.Combine(modelDir, "tokens.txt");
            // keywords.txt folgt dem gewaehlten Weckwort (separater, beschreibbarer Pfad) — sonst
            // die gebundelte Datei im modelDir (Default "Okay Computer").
            string keywords = string.IsNullOrWhiteSpace(keywordsFile)
                ? Path.Combine(modelDir, "keywords.txt")
                : keywordsFile!;

            // Sonden: fehlende Modelldateien sind die haeufigste stille Ursache (#11/§6).
            foreach (var (path, label) in new[]
                     { (encoder, "encoder.onnx"), (decoder, "decoder.onnx"), (joiner, "joiner.onnx"),
                       (tokens, "tokens.txt"), (keywords, "keywords.txt") })
            {
                Probe.That(File.Exists(path), $"Wake-Word-Modelldatei fehlt: {label}", new { path });
                if (!File.Exists(path))
                    throw new FileNotFoundException($"Wake-Word-Modelldatei fehlt: {path}", path);
            }

            var config = new KeywordSpotterConfig();
            config.FeatConfig.SampleRate = SampleRate;
            config.FeatConfig.FeatureDim = 80;
            config.ModelConfig.Transducer.Encoder = encoder;
            config.ModelConfig.Transducer.Decoder = decoder;
            config.ModelConfig.Transducer.Joiner = joiner;
            config.ModelConfig.Tokens = tokens;
            config.ModelConfig.Provider = "cpu";
            config.ModelConfig.NumThreads = numThreads;   // niedrig halten (Always-on-Effizienz §4)
            config.ModelConfig.Debug = 0;
            config.KeywordsFile = keywords;
            // KeywordsScore/-Threshold bleiben auf den Modell-Defaults (1.0 / 0.25) — pro Keyword
            // in keywords.txt via :boost / #threshold feinjustierbar (#10/§1).

            try
            {
                _spotter = new KeywordSpotter(config);
                _stream = _spotter.CreateStream();
                Log.Info("Wake-Word-Engine bereit (sherpa-onnx KeywordSpotter)", new { modelDir, numThreads });
            }
            catch (Exception ex)
            {
                Log.Error("Wake-Word-Engine: Native-Initialisierung fehlgeschlagen", ex, new { modelDir });
                _spotter?.Dispose();
                throw;
            }
        }

        public string? Accept(float[] samples)
        {
            if (_disposed || samples == null || samples.Length == 0) return null;
            try
            {
                lock (_gate)
                {
                    _stream.AcceptWaveform(SampleRate, samples);
                    while (_spotter.IsReady(_stream))        // PFLICHT: while, nicht ein einzelner Decode (#21)
                        _spotter.Decode(_stream);

                    string keyword = _spotter.GetResult(_stream).Keyword;
                    if (!string.IsNullOrEmpty(keyword))
                    {
                        _spotter.Reset(_stream);             // PFLICHT direkt nach Treffer (#9), sonst Dauerfeuer
                        return keyword;
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Error("Wake-Word-Engine: Fehler beim Verarbeiten eines Audio-Blocks", ex);
            }
            return null;
        }

        public void Reset()
        {
            if (_disposed) return;
            try { lock (_gate) { _spotter.Reset(_stream); } }
            catch (Exception ex) { Log.Error("Wake-Word-Engine: Reset fehlgeschlagen", ex); }
        }

        public void Dispose()
        {
            if (_disposed) return;
            _disposed = true;
            try { _stream?.Dispose(); _spotter?.Dispose(); }   // native Handles freigeben (§1)
            catch (Exception ex) { Log.Error("Wake-Word-Engine: Dispose-Fehler", ex); }
        }

        /// <summary>
        /// Stellt den eigenen Programmordner dem nativen Loader voran, damit die mitgelieferte
        /// onnxruntime.dll gewinnt und nicht eine aeltere aus C:\Windows\System32 (#1/§1).
        /// </summary>
        private static void EnsureDllSearchPath()
        {
            if (_dllDirSet) return;
            _dllDirSet = true;
            try
            {
                string dir = AppContext.BaseDirectory;
                if (!string.IsNullOrEmpty(dir)) SetDllDirectory(dir);
            }
            catch (Exception ex)
            {
                Log.Warn("Wake-Word-Engine: SetDllDirectory fehlgeschlagen (unkritisch)", new { error = ex.Message });
            }
        }

        [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool SetDllDirectory(string lpPathName);
    }
}
