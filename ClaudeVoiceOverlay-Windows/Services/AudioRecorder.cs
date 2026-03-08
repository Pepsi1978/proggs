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

        public AudioRecorder(int sampleRate = 16000, int channels = 1)
        {
            _sampleRate = sampleRate;
            _channels = channels;
        }

        public void Start()
        {
            if (IsRecording) return;

            _tempFile = Path.Combine(Path.GetTempPath(), $"cvo_recording_{Guid.NewGuid():N}.wav");

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
