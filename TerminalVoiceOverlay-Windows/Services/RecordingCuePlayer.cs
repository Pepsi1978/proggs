using System;
using System.Media;
using NAudio.Wave;

namespace TerminalVoiceOverlay.Services
{
    public sealed class RecordingCuePlayer : IDisposable
    {
        private const int SampleRate = 44100;
        private const int DesiredLatencyMs = 100;
        private const int OutputBufferCount = 3;
        private readonly object _lock = new();
        private readonly byte[] _startPcm = CreatePcm((880, 150));
        private readonly byte[] _stopPcm = CreatePcm((660, 120), (440, 120));
        private WaveOutEvent? _output;
        private BufferedWaveProvider? _buffer;
        private bool _playbackHealthy;
        private bool _disposed;

        public RecordingCuePlayer()
        {
            lock (_lock)
            {
                try
                {
                    InitializeOutput();
                }
                catch (Exception ex)
                {
                    DiagLog.Write("AudioCue", "output_init_failed", ("err", ex.Message), ("type", ex.GetType().Name));
                }
            }
        }

        public void PlayStart() => Play(_startPcm, "start");

        public void PlayStop() => Play(_stopPcm, "stop");

        private void Play(byte[] pcm, string cue)
        {
            try
            {
                lock (_lock)
                {
                    if (_disposed) return;

                    if (!_playbackHealthy || _output?.PlaybackState != PlaybackState.Playing || _buffer == null)
                    {
                        DisposeOutput();
                        InitializeOutput();
                    }

                    _buffer!.ClearBuffer();
                    _buffer.AddSamples(pcm, 0, pcm.Length);
                }

                DiagLog.Write("AudioCue", "pcm_queued", ("cue", cue), ("bytes", pcm.Length), ("latencyMs", DesiredLatencyMs));
            }
            catch (Exception ex)
            {
                DiagLog.Write("AudioCue", "queue_failed", ("cue", cue), ("err", ex.Message), ("type", ex.GetType().Name));
                try { SystemSounds.Beep.Play(); }
                catch (Exception fallbackEx)
                {
                    DiagLog.Write("AudioCue", "fallback_failed", ("cue", cue), ("err", fallbackEx.Message), ("type", fallbackEx.GetType().Name));
                }
            }
        }

        private void InitializeOutput()
        {
            var buffer = new BufferedWaveProvider(new WaveFormat(SampleRate, 16, 1))
            {
                BufferDuration = TimeSpan.FromSeconds(2),
                DiscardOnBufferOverflow = true,
                ReadFully = true
            };
            var output = new WaveOutEvent
            {
                DesiredLatency = DesiredLatencyMs,
                NumberOfBuffers = OutputBufferCount
            };

            try
            {
                output.PlaybackStopped += OnPlaybackStopped;
                output.Init(buffer);
                output.Play();
                _buffer = buffer;
                _output = output;
                _playbackHealthy = true;
                DiagLog.Write("AudioCue", "output_ready", ("latencyMs", DesiredLatencyMs), ("buffers", OutputBufferCount));
            }
            catch
            {
                output.PlaybackStopped -= OnPlaybackStopped;
                output.Dispose();
                throw;
            }
        }

        private void OnPlaybackStopped(object? sender, StoppedEventArgs e)
        {
            lock (_lock)
                _playbackHealthy = false;

            DiagLog.Write("AudioCue", "output_stopped",
                ("error", e.Exception?.Message ?? "none"),
                ("disposed", _disposed));
        }

        private void DisposeOutput()
        {
            if (_output == null) return;
            _output.PlaybackStopped -= OnPlaybackStopped;
            _output.Dispose();
            _output = null;
            _buffer = null;
            _playbackHealthy = false;
        }

        private static byte[] CreatePcm(params (int Frequency, int DurationMs)[] tones)
        {
            int sampleCount = 0;
            foreach (var tone in tones)
                sampleCount += SampleRate * tone.DurationMs / 1000;

            var pcm = new byte[sampleCount * sizeof(short)];
            int byteOffset = 0;
            foreach (var (frequency, durationMs) in tones)
            {
                int toneSamples = SampleRate * durationMs / 1000;
                int fadeSamples = Math.Min(SampleRate / 200, toneSamples / 2);
                for (int i = 0; i < toneSamples; i++)
                {
                    double fade = Math.Min(1.0, Math.Min(i + 1, toneSamples - i) / (double)fadeSamples);
                    short sample = (short)(Math.Sin(2 * Math.PI * frequency * i / SampleRate) * short.MaxValue * 0.22 * fade);
                    pcm[byteOffset++] = (byte)sample;
                    pcm[byteOffset++] = (byte)(sample >> 8);
                }
            }

            return pcm;
        }

        public void Dispose()
        {
            lock (_lock)
            {
                _disposed = true;
                DisposeOutput();
            }
        }
    }
}
