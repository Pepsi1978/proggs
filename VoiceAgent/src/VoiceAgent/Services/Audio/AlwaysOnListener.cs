using System;
using System.Collections.Generic;
using System.IO;
using NAudio.Wave;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Hoert dauerhaft ueber das Mikrofon (NAudio), erkennt anhand von Stille die Grenzen
    /// einer Aussage und meldet jede fertige Aussage als WAV-Datei ueber das OnUtterance-
    /// Event. Bei deaktiviertem Mikrofon (SetEnabled(false)) wird pausiert.
    ///
    /// Pre-Roll: die letzten ~400 ms vor dem Sprechbeginn werden mitgepuffert, damit der
    /// leise Wort-Anlaut nicht verschluckt wird (sonst gehen die ersten Worte verloren).
    ///
    /// Robust gegen Geraete-Wechsel/-Abziehen (Almanach §9.5): alle NAudio-Aufrufe in
    /// try/catch, Fehler werden geloggt statt geworfen.
    /// </summary>
    public sealed class AlwaysOnListener : IDisposable
    {
        /// <summary>Gemeldet, wenn eine vollstaendige Aussage als WAV-Datei vorliegt (Pfad).</summary>
        public event Action<string>? OnUtterance;

        /// <summary>
        /// Roher 16-kHz-mono-16-bit-PCM-Block bei JEDEM Mikrofon-Callback (buffer, gueltige Byte-Anzahl).
        /// Genutzt fuer kontinuierliches Wake-Word-Lauschen (WakeWordController).
        /// WICHTIG: Der Puffer wird von NAudio wiederverwendet — der Abonnent MUSS die Bytes
        /// sofort verarbeiten/kopieren (PcmConverter erzeugt ohnehin ein neues float[]),
        /// niemals die Referenz fuer spaeter behalten.
        /// </summary>
        public event Action<byte[], int>? OnFrame;

        /// <summary>
        /// Gemeldet im MOMENT des Sprechbeginns (erster lauter Block einer neuen Aussage) — fuer eine
        /// ZEITGENAUE Live-Statusanzeige ("Hoert zu" ab der ersten Millisekunde, nicht erst nach
        /// dem Stille-Ende). Feuert vom Audio-Thread (Abonnent muss auf den UI-Thread marshallen).
        /// </summary>
        public event Action? OnSpeechStart;

        private const int SampleRate = 16000;
        private const int PreRollMs = 400;                          // Vorlauf gegen verschluckte Anlaute
        private static readonly int PreRollBytes = SampleRate * 2 * PreRollMs / 1000;

        private readonly float _silenceThreshold;
        private readonly int _silenceMs;
        private readonly int _minUtteranceMs;
        private readonly int _minVoicedMs;          // Sprachgehalt-Vorfilter (Schicht 1, groq-transkription §2.1)
        private readonly double _minVoicedRatio;    // Mindest-Anteil lauter Zeit; 0 = Filter aus
        private readonly int _maxUtteranceMs;       // harter Deckel pro Aussage (0 = aus) — Endlos-Aufnahmen unmoeglich

        private WaveInEvent? _waveIn;
        private readonly List<byte> _buffer = new();
        private readonly List<byte> _preRoll = new();
        private readonly object _gate = new();
        private bool _inSpeech;
        private DateTime _lastVoiceUtc = DateTime.UtcNow;
        private DateTime _speechStartUtc = DateTime.UtcNow;
        private double _voicedMs;                   // aufsummierte LAUTE (Sprach-)Zeit der laufenden Aussage
        private volatile bool _enabled = true;
        private volatile bool _disposed;

        public AlwaysOnListener(double silenceThreshold, int silenceMs, int minUtteranceMs,
                                int minVoicedMs = 0, double minVoicedRatio = 0.0, int maxUtteranceMs = 0)
        {
            _silenceThreshold = (float)silenceThreshold;
            _silenceMs = silenceMs;
            _minUtteranceMs = minUtteranceMs;
            _minVoicedMs = minVoicedMs;
            _minVoicedRatio = minVoicedRatio;
            _maxUtteranceMs = maxUtteranceMs;
            // Sanity-Sonden: unsinnige Konfigwerte wuerden die Stille-Erkennung lautlos lahmlegen.
            Probe.That(_silenceThreshold > 0f, "AlwaysOnListener: silenceThreshold <= 0 — Sprache wird evtl. nie erkannt", new { silenceThreshold });
            Probe.That(silenceMs > 0, "AlwaysOnListener: silenceMs <= 0 — Aussage-Ende wird evtl. nie erkannt", new { silenceMs });
            Probe.That(minUtteranceMs >= 0, "AlwaysOnListener: minUtteranceMs negativ", new { minUtteranceMs });
            Probe.That(minVoicedMs >= 0 && minVoicedRatio >= 0.0 && minVoicedRatio <= 1.0,
                "AlwaysOnListener: Voiced-Filter-Schwellen unplausibel (minVoicedMs<0 oder Ratio nicht in [0,1])",
                new { minVoicedMs, minVoicedRatio });
        }

        /// <summary>
        /// True, solange gerade eine Aussage aufgenommen wird (Sprechbeginn erkannt, Ende noch
        /// nicht). Fuer den Wachfenster-Lebenszyklus: solange Frank spricht, darf das
        /// Wake-Fenster NICHT ablaufen (Voice-Pipeline-BP: Timer nur im echten Leerlauf).
        /// </summary>
        public bool IsCapturing { get { lock (_gate) return _inSpeech; } }

        public void SetEnabled(bool on)
        {
            _enabled = on;
            Log.Info($"AlwaysOnListener: Mikrofon {(on ? "an" : "aus")}");
            if (!on)
                lock (_gate) { _buffer.Clear(); _preRoll.Clear(); _inSpeech = false; _voicedMs = 0; }
        }

        /// <summary>
        /// Verwirft die gerade laufende Aufnahme (Buffer + Vorlauf), ohne die Aufnahme zu stoppen.
        /// Genutzt direkt nach dem Wecken: die Weckwort-Phrase ("Okay Computer") und der Erkennungston
        /// werden verworfen, damit ab da ein frischer Sprech-Start beginnt (wie ein Knopfdruck).
        /// </summary>
        public void ClearBuffer()
        {
            lock (_gate) { _buffer.Clear(); _preRoll.Clear(); _inSpeech = false; _voicedMs = 0; }
        }

        public void Start()
        {
            try
            {
                _waveIn = new WaveInEvent
                {
                    WaveFormat = new WaveFormat(SampleRate, 16, 1),
                    BufferMilliseconds = 100
                };
                _waveIn.DataAvailable += OnDataAvailable;
                _waveIn.RecordingStopped += (_, e) =>
                {
                    if (e.Exception != null)
                        Log.Error("AlwaysOnListener: RecordingStopped mit Fehler", e.Exception);
                };
                _waveIn.StartRecording();
                Log.Info($"AlwaysOnListener: Daueraufnahme gestartet (16 kHz mono, Stille-Ende nach {_silenceMs} ms, Pre-Roll {PreRollMs} ms)");
            }
            catch (Exception ex)
            {
                Log.Error("AlwaysOnListener: Start fehlgeschlagen (ist ein Mikrofon angeschlossen?)", ex);
            }
        }

        private void OnDataAvailable(object? sender, WaveInEventArgs e)
        {
            if (_disposed || !_enabled) return;
            try
            {
                // Rohen Frame fuer das Wake-Word-Lauschen weitergeben (vor der Stille-Logik).
                // Abonnent kopiert sofort (siehe OnFrame-Doku).
                OnFrame?.Invoke(e.Buffer, e.BytesRecorded);

                float rms = ComputeRms(e.Buffer, e.BytesRecorded);
                bool silent = rms < _silenceThreshold;
                var now = DateTime.UtcNow;
                bool speechJustStarted = false;   // Event erst NACH dem Lock feuern (kein Deadlock-Risiko)

                lock (_gate)
                {
                    // Vor dem Sprechbeginn laufend einen kurzen Vorlauf mitfuehren.
                    if (!_inSpeech)
                        AppendToPreRoll(e.Buffer, e.BytesRecorded);

                    if (!silent)
                    {
                        // Schicht 1 (groq-transkription §2.1): Dauer dieses LAUTEN Blocks fuer den
                        // Sprachgehalt-Vorfilter aufsummieren. 16-bit mono: count/2 Samples / Rate.
                        double blockMs = e.BytesRecorded / 2.0 / SampleRate * 1000.0;
                        if (!_inSpeech)
                        {
                            _inSpeech = true;
                            speechJustStarted = true;     // -> sofort "Hoert zu" anzeigen (zeitgenau)
                            _buffer.Clear();
                            _buffer.AddRange(_preRoll);   // Anlaut aus dem Vorlauf uebernehmen
                            _speechStartUtc = now;
                            _voicedMs = blockMs;          // neue Aussage: laute Zeit ab erstem lauten Block
                        }
                        else
                        {
                            AppendBytes(e.Buffer, e.BytesRecorded);
                            _voicedMs += blockMs;         // weitere laute Zeit aufsummieren
                        }
                        _lastVoiceUtc = now;
                        // Harter Deckel (Voice-Pipeline-Almanach): findet die Stille-Erkennung kein
                        // Ende (Tastatur/Atmen halten _lastVoiceUtc frisch), wird die Aussage hier
                        // finalisiert und VERARBEITET — der Rest laeuft als neue Aussage weiter.
                        // Ohne Deckel entstanden 41-s-/131-s-Endlos-Aufnahmen (Log 2026-06-10).
                        if (_maxUtteranceMs > 0 && (now - _speechStartUtc).TotalMilliseconds >= _maxUtteranceMs)
                        {
                            Log.Info($"AlwaysOnListener: Max-Aufnahmedauer erreicht ({_maxUtteranceMs} ms) — Aussage wird verarbeitet, Aufnahme laeuft weiter");
                            FinalizeUtterance(now);
                        }
                    }
                    else if (_inSpeech)
                    {
                        AppendBytes(e.Buffer, e.BytesRecorded); // nachlaufende Stille mitnehmen
                        if ((now - _lastVoiceUtc).TotalMilliseconds >= _silenceMs)
                            FinalizeUtterance(now);
                    }
                }

                // Sprechbeginn live melden (ausserhalb des Locks): Statuszeile sofort auf "Hoert zu".
                if (speechJustStarted) OnSpeechStart?.Invoke();
            }
            catch (Exception ex)
            {
                Log.Error("AlwaysOnListener: Fehler beim Verarbeiten der Audiodaten", ex);
            }
        }

        private void AppendBytes(byte[] buffer, int count)
        {
            for (int i = 0; i < count; i++) _buffer.Add(buffer[i]);
        }

        private void AppendToPreRoll(byte[] buffer, int count)
        {
            for (int i = 0; i < count; i++) _preRoll.Add(buffer[i]);
            if (_preRoll.Count > PreRollBytes)
                _preRoll.RemoveRange(0, _preRoll.Count - PreRollBytes);
        }

        // Aufruf erfolgt unter _gate-Lock.
        private void FinalizeUtterance(DateTime now)
        {
            byte[] pcm = _buffer.ToArray();
            double voicedMs = _voicedMs;          // vor dem Zuruecksetzen sichern (Schicht 1)
            _buffer.Clear();
            _preRoll.Clear();
            _inSpeech = false;
            _voicedMs = 0;

            double durationMs = (now - _speechStartUtc).TotalMilliseconds;
            if (pcm.Length == 0 || durationMs < _minUtteranceMs)
            {
                Log.Info($"AlwaysOnListener: Aussage zu kurz ({durationMs:F0} ms) — ignoriert");
                return;
            }

            // Schicht 1 (Almanach groq-transkription §2.1/2.2): Sprachgehalt-Vorfilter. Eine Aussage,
            // die zwar lang genug ist, aber kaum echte Stimme enthaelt (kurzer Peak + viel Stille),
            // loest bei Whisper die Stille-Halluzination aus ("Vielen Dank"). Solche sprach-armen
            // Aussagen GAR NICHT erst an Groq senden (spart zudem die 10-s-Mindestabrechnung).
            // Konservativ: echte (auch leise/kurze) Sprache hat genug Voiced-Zeit und bleibt erhalten.
            // Betrifft NUR den STT-Pfad — der kontinuierliche Wake-Word-Stream (OnFrame) ist davon
            // unberuehrt (Wake-Almanach Bug #33: kein Frame-Filtering vor dem Streaming-KWS).
            double voicedRatio = durationMs > 0 ? voicedMs / durationMs : 0.0;
            // Sanity-Sonde: die laute Zeit kann nie groesser als die Gesamtdauer sein (+1 ms Toleranz).
            Probe.That(voicedMs <= durationMs + 1.0,
                "AlwaysOnListener: voicedMs > durationMs — Zaehlfehler im Voiced-Vorfilter",
                new { voicedMs, durationMs });
            bool tooLittleVoice = (_minVoicedMs > 0 && voicedMs < _minVoicedMs)
                               || (_minVoicedRatio > 0.0 && voicedRatio < _minVoicedRatio);
            if (tooLittleVoice)
            {
                Log.Info($"AlwaysOnListener: sprach-arm verworfen (laut {voicedMs:F0}/{durationMs:F0} ms = {voicedRatio:P0}) — vor Groq gefiltert (Stille-Halluzinations-Schutz)");
                return;
            }

            string path;
            try
            {
                path = Path.Combine(Path.GetTempPath(), $"voiceagent-utt-{Guid.NewGuid():N}.wav");
                using (var writer = new WaveFileWriter(path, new WaveFormat(SampleRate, 16, 1)))
                {
                    writer.Write(pcm, 0, pcm.Length);
                }
                Log.Info($"AlwaysOnListener: Aussage erkannt ({durationMs:F0} ms) → {path}");
            }
            catch (Exception ex)
            {
                Log.Error("AlwaysOnListener: WAV-Schreiben fehlgeschlagen", ex);
                return;
            }

            OnUtterance?.Invoke(path);
        }

        private static float ComputeRms(byte[] buffer, int count)
        {
            int sampleCount = count / 2;
            if (sampleCount == 0) return 0f;
            double sum = 0;
            for (int i = 0; i + 1 < count; i += 2)
            {
                short s = (short)(buffer[i] | (buffer[i + 1] << 8));
                float f = s / 32768f;
                sum += (double)f * f;
            }
            return (float)Math.Sqrt(sum / sampleCount);
        }

        /// <summary>Reine RMS-Stille-Pruefung auf normalisierten Samples — isoliert testbar.</summary>
        public static bool IsSilence(float[] samples, float threshold)
        {
            if (samples == null || samples.Length == 0) return true;
            double sum = 0;
            foreach (var s in samples) sum += (double)s * s;
            double rms = Math.Sqrt(sum / samples.Length);
            return rms < threshold;
        }

        public void Dispose()
        {
            _disposed = true;
            try
            {
                _waveIn?.StopRecording();
                _waveIn?.Dispose();
            }
            catch (Exception ex)
            {
                Log.Error("AlwaysOnListener: Dispose-Fehler", ex);
            }
            _waveIn = null;
        }
    }
}
