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
        // Fehlerton: fallende Terz, deutlich tiefer als Start/Stop — auch fuer
        // Hotkey-/PTT-Nutzer, die den Button gar nicht sehen (03.09.2026).
        private readonly byte[] _errorPcm = CreatePcm((392, 110), (262, 260));
        // The output device is opened only around cues and released after this
        // idle tail. A permanently open WaveOut stream (ReadFully=true streams
        // silence forever) kept Windows reporting an active audio stream, blocked
        // sleep and kept Bluetooth headsets awake.
        private const int IdleCloseMs = 4000;
        private WaveOutEvent? _output;
        private BufferedWaveProvider? _buffer;
        private bool _playbackHealthy;
        private bool _disposed;
        private long _lastUseTicks;
        private readonly System.Threading.Timer _idleTimer;

        public RecordingCuePlayer()
        {
            // No device is opened here; see IdleCloseMs.
            _idleTimer = new System.Threading.Timer(_ => OnIdleTimer(), null,
                System.Threading.Timeout.Infinite, System.Threading.Timeout.Infinite);
        }

        /// <summary>
        /// Opens the output device ahead of a cue (e.g. when a recording is about
        /// to start) so the following PlayStart has no device-open latency. The
        /// device is released again after the idle tail.
        /// </summary>
        public void Prewarm()
        {
            try
            {
                lock (_lock)
                {
                    if (_disposed) return;
                    EnsureOutput();
                    ArmIdleClose();
                }
            }
            catch (Exception ex)
            {
                DiagLog.Write("AudioCue", "prewarm_failed", ("err", ex.Message), ("type", ex.GetType().Name));
            }
        }

        public void PlayStart() => Play(_startPcm, "start");

        public void PlayStop() => Play(_stopPcm, "stop");

        public void PlayError() => Play(_errorPcm, "error");

        private void Play(byte[] pcm, string cue)
        {
            try
            {
                lock (_lock)
                {
                    if (_disposed) return;

                    EnsureOutput();

                    _buffer!.ClearBuffer();
                    _buffer.AddSamples(pcm, 0, pcm.Length);
                    ArmIdleClose();
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

        // Caller holds _lock.
        private void EnsureOutput()
        {
            if (!_playbackHealthy || _output?.PlaybackState != PlaybackState.Playing || _buffer == null)
            {
                DisposeOutput();
                InitializeOutput();
            }
        }

        // Caller holds _lock. Restarts the idle tail after every use.
        private void ArmIdleClose()
        {
            _lastUseTicks = Environment.TickCount64;
            try { _idleTimer.Change(IdleCloseMs, System.Threading.Timeout.Infinite); }
            catch (ObjectDisposedException) { /* disposing */ }
        }

        private void OnIdleTimer()
        {
            try
            {
                lock (_lock)
                {
                    if (_disposed || _output == null) return;
                    long idleMs = Environment.TickCount64 - _lastUseTicks;
                    if (idleMs < IdleCloseMs)
                    {
                        // A newer cue arrived after the timer fired; wait for the rest of its tail.
                        try { _idleTimer.Change(IdleCloseMs - idleMs, System.Threading.Timeout.Infinite); }
                        catch (ObjectDisposedException) { }
                        return;
                    }
                    DisposeOutput();
                }
                DiagLog.Write("AudioCue", "output_released", ("idleMs", IdleCloseMs));
            }
            catch (Exception ex)
            {
                DiagLog.Write("AudioCue", "release_failed", ("err", ex.Message), ("type", ex.GetType().Name));
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
                try { _idleTimer.Dispose(); } catch { /* ignore */ }
                DisposeOutput();
            }
        }
    }
}
