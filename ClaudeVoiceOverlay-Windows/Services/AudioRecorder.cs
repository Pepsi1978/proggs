using System;
using System.IO;
using NAudio.Wave;

namespace ClaudeVoiceOverlay.Services
{
    public sealed class AudioRecorder : IDisposable
    {
        private readonly int _sampleRate;
        private readonly int _channels;
        private WaveInEvent? _waveIn;
        private WaveFileWriter? _writer;
        private string? _tempFile;
        private readonly object _lock = new();

        public bool IsRecording { get; private set; }

        /// <summary>
        /// Wird waehrend einer laufenden Aufnahme alle ~100ms gefeuert
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

            var waveFormat = new WaveFormat(_sampleRate, 16, _channels);
            _writer = new WaveFileWriter(_tempFile, waveFormat);

            _waveIn = new WaveInEvent
            {
                WaveFormat = waveFormat,
                BufferMilliseconds = 100
            };

            _waveIn.DataAvailable += (_, e) =>
            {
                lock (_lock)
                {
                    _writer?.Write(e.Buffer, 0, e.BytesRecorded);
                }

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
                try { LevelChanged?.Invoke(normalized); }
                catch { /* Listener-Fehler duerfen die Aufnahme nicht abbrechen. */ }
            };

            _waveIn.StartRecording();
            IsRecording = true;
            Console.WriteLine($"AudioRecorder: Aufnahme gestartet ({_sampleRate}Hz, {_channels}ch)");
        }

        public string? Stop()
        {
            if (!IsRecording) return null;
            IsRecording = false;

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
                return _tempFile;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AudioRecorder: Fehler beim Stoppen: {ex.Message}");
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
