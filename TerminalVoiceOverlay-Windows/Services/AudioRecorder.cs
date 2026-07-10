using System;
using System.Diagnostics;
using System.IO;
using NAudio.Wave;

namespace TerminalVoiceOverlay.Services
{
    public sealed class AudioRecorder : IDisposable
    {
        private readonly int _sampleRate;
        private readonly int _channels;
        private WaveInEvent? _waveIn;
        private WaveFileWriter? _writer;
        private string? _tempFile;
        private readonly object _lock = new();
        private readonly Stopwatch _recordingSw = new();
        private long _bytesRecorded;
        private long _buffersRecorded;
        private float _peakMax;

        public bool IsRecording { get; private set; }

        /// <summary>
        /// Wird waehrend einer laufenden Aufnahme alle ~50ms gefeuert
        /// (Takt entspricht WaveInEvent.BufferMilliseconds). Der Wert ist
        /// der Peak-Pegel des aktuellen Buffers, normiert auf 0..1 (Stille
        /// bis Vollausschlag). Subscribers laufen auf dem NAudio-Thread —
        /// UI-Updates muessen auf den Dispatcher gehoben werden.
        /// </summary>
        public event Action<float>? LevelChanged;

        public AudioRecorder(int sampleRate = 16000, int channels = 1)
        {
            _sampleRate = sampleRate;
            _channels = channels;
        }

        public void Start()
        {
            if (IsRecording) return;

            _tempFile = Path.Combine(Path.GetTempPath(), $"tvo_recording_{Guid.NewGuid():N}.wav");
            _bytesRecorded = 0;
            _buffersRecorded = 0;
            _peakMax = 0;
            _recordingSw.Restart();

            var waveFormat = new WaveFormat(_sampleRate, 16, _channels);
            _writer = new WaveFileWriter(_tempFile, waveFormat);

            _waveIn = new WaveInEvent
            {
                WaveFormat = waveFormat,
                BufferMilliseconds = 50
            };

            _waveIn.DataAvailable += (_, e) =>
            {
                lock (_lock)
                {
                    _writer?.Write(e.Buffer, 0, e.BytesRecorded);
                }
                _bytesRecorded += e.BytesRecorded;
                _buffersRecorded++;

                // Peak-Pegel aus dem 16-bit signed PCM-Buffer berechnen.
                // Wir scannen alle Samples und behalten den groessten
                // Absolutwert. Geteilt durch 32768 ergibt einen Wert in
                // 0..1 — direkt brauchbar als Strich-Hoehe in der UI.
                // Subscriber laeuft auf dem NAudio-Thread, deswegen darf
                // der Listener KEINE UI-Calls ohne Dispatcher.Invoke
                // machen — das Overlay erledigt das selbst.
                int peak = 0;
                int sampleCount = e.BytesRecorded / 2;
                for (int i = 0; i < sampleCount; i++)
                {
                    short sample = (short)(e.Buffer[i * 2] | (e.Buffer[i * 2 + 1] << 8));
                    int abs = sample < 0 ? -sample : sample;
                    if (abs > peak) peak = abs;
                }
                float normalized = peak / 32768f;
                if (normalized > _peakMax) _peakMax = normalized;
                try { LevelChanged?.Invoke(normalized); }
                catch { /* Listener-Fehler duerfen die Aufnahme nicht abbrechen. */ }
            };

            _waveIn.StartRecording();
            IsRecording = true;
            Console.WriteLine($"AudioRecorder: Aufnahme gestartet ({_sampleRate}Hz, {_channels}ch)");
            DiagLog.Write("Audio", "recording_start", ("sampleRate", _sampleRate), ("channels", _channels), ("path", _tempFile));
        }

        public string? Stop()
        {
            if (!IsRecording) return null;
            IsRecording = false;
            _recordingSw.Stop();

            try
            {
                _waveIn?.StopRecording();

                // Dispose writer under lock so no DataAvailable callback can write after this
                lock (_lock)
                {
                    _writer?.Dispose();
                    _writer = null;
                }

                _waveIn?.Dispose();
                _waveIn = null;

                Console.WriteLine("AudioRecorder: Aufnahme gestoppt");
                long fileBytes = 0;
                try { if (!string.IsNullOrEmpty(_tempFile) && File.Exists(_tempFile)) fileBytes = new FileInfo(_tempFile).Length; }
                catch { }
                DiagLog.Write("Audio", "recording_stop",
                    ("ms", _recordingSw.ElapsedMilliseconds),
                    ("pcmBytes", _bytesRecorded),
                    ("fileBytes", fileBytes),
                    ("buffers", _buffersRecorded),
                    ("peak", _peakMax.ToString("0.000")),
                    ("path", _tempFile));
                return _tempFile;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AudioRecorder: Fehler beim Stoppen: {ex.Message}");
                DiagLog.Error("Audio", "recording_stop_failed", ex, ("ms", _recordingSw.ElapsedMilliseconds));
                return null;
            }
        }

        public void Dispose()
        {
            if (IsRecording) Stop();
            _writer?.Dispose();
            _waveIn?.Dispose();
        }
    }
}
