using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using NAudio.Wave;

namespace TerminalVoiceOverlay.Services
{
    public sealed class AudioRecorder : IDisposable
    {
        private readonly int _sampleRate;
        private readonly int _channels;
        private readonly object _stateLock = new();
        private RecordingSession? _session;
        private bool _disposed;

        public bool IsRecording
        {
            get
            {
                lock (_stateLock)
                {
                    return _session != null;
                }
            }
        }

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

        public bool Start()
        {
            var setupSw = Stopwatch.StartNew();
            string tempFile = Path.Combine(Path.GetTempPath(), $"tvo_recording_{Guid.NewGuid():N}.wav");
            WaveFileWriter? writer = null;
            WaveInEvent? waveIn = null;
            RecordingSession? session = null;

            try
            {
                lock (_stateLock)
                {
                    ObjectDisposedException.ThrowIf(_disposed, this);
                    if (_session != null) return false;

                    var waveFormat = new WaveFormat(_sampleRate, 16, _channels);
                    writer = new WaveFileWriter(tempFile, waveFormat);
                    waveIn = new WaveInEvent
                    {
                        WaveFormat = waveFormat,
                        BufferMilliseconds = 50,
                        NumberOfBuffers = 3
                    };

                    session = new RecordingSession(waveIn, writer, tempFile);
                    session.DataAvailableHandler = (_, e) => OnDataAvailable(session, e);
                    session.RecordingStoppedHandler = (_, e) => OnRecordingStopped(session, e);
                    waveIn.DataAvailable += session.DataAvailableHandler;
                    waveIn.RecordingStopped += session.RecordingStoppedHandler;
                    _session = session;
                }

                waveIn.StartRecording();
                session.RecordingStopwatch.Restart();
                session.StartCompleted.TrySetResult();

                Console.WriteLine($"AudioRecorder: Aufnahme gestartet ({_sampleRate}Hz, {_channels}ch)");
                DiagLog.Write("Audio", "recording_start", ("setupMs", setupSw.ElapsedMilliseconds), ("sampleRate", _sampleRate), ("channels", _channels), ("path", tempFile));
                return true;
            }
            catch (Exception ex)
            {
                LogError("recording_start_failed", ex, tempFile);
                if (session != null)
                {
                    session.StartFailed = true;
                    session.StartCompleted.TrySetResult();
                    CompleteSession(session, ex);
                    session.Stopped.Task.GetAwaiter().GetResult();
                }
                else
                {
                    CleanupStartupResources(waveIn, writer, tempFile);
                }

                throw;
            }
            finally
            {
                session?.StartCompleted.TrySetResult();
            }
        }

        public string? Stop()
        {
            RecordingSession? session;
            lock (_stateLock)
            {
                session = _session;
            }

            if (session == null) return null;
            session.StartCompleted.Task.GetAwaiter().GetResult();
            if (session.StartFailed)
            {
                session.Stopped.Task.GetAwaiter().GetResult();
                return null;
            }

            if (Interlocked.CompareExchange(ref session.StopStarted, 1, 0) == 0 &&
                Volatile.Read(ref session.CompletionStarted) == 0)
            {
                try
                {
                    session.WaveIn.StopRecording();
                }
                catch (Exception ex)
                {
                    LogError("recording_stop_failed", ex, session.TempFile);
                    CompleteSession(session, ex);
                }
            }

            // RecordingStopped is raised only after NAudio has delivered its final buffers.
            session.Stopped.Task.GetAwaiter().GetResult();
            return session.CompletionError == null ? session.TempFile : null;
        }

        private void OnDataAvailable(RecordingSession session, WaveInEventArgs e)
        {
            lock (session.WriterLock)
            {
                session.Writer?.Write(e.Buffer, 0, e.BytesRecorded);
            }

            session.BytesRecorded += e.BytesRecorded;
            session.BuffersRecorded++;

            int peak = 0;
            int sampleCount = e.BytesRecorded / 2;
            for (int i = 0; i < sampleCount; i++)
            {
                short sample = (short)(e.Buffer[i * 2] | (e.Buffer[i * 2 + 1] << 8));
                int abs = sample < 0 ? -sample : sample;
                if (abs > peak) peak = abs;
            }

            float normalized = peak / 32768f;
            if (normalized > session.PeakMax) session.PeakMax = normalized;
            try
            {
                LevelChanged?.Invoke(normalized);
            }
            catch (Exception ex)
            {
                LogError("level_listener_failed", ex, session.TempFile);
            }
        }

        private void OnRecordingStopped(RecordingSession session, StoppedEventArgs e)
        {
            session.StartCompleted.Task.GetAwaiter().GetResult();
            CompleteSession(session, e.Exception);
        }

        private void CompleteSession(RecordingSession session, Exception? error)
        {
            if (Interlocked.Exchange(ref session.CompletionStarted, 1) != 0) return;

            Exception? completionError = error;
            try
            {
                session.RecordingStopwatch.Stop();

                try
                {
                    session.WaveIn.DataAvailable -= session.DataAvailableHandler;
                    session.WaveIn.RecordingStopped -= session.RecordingStoppedHandler;
                }
                catch (Exception ex)
                {
                    completionError ??= ex;
                    LogError("recording_handler_cleanup_failed", ex, session.TempFile);
                }

                try
                {
                    session.WaveIn.Dispose();
                }
                catch (Exception ex)
                {
                    completionError ??= ex;
                    LogError("recording_input_cleanup_failed", ex, session.TempFile);
                }

                lock (session.WriterLock)
                {
                    try
                    {
                        session.Writer?.Dispose();
                    }
                    catch (Exception ex)
                    {
                        completionError ??= ex;
                        LogError("recording_writer_cleanup_failed", ex, session.TempFile);
                    }
                    finally
                    {
                        session.Writer = null;
                    }
                }

                if (session.StartFailed)
                {
                    TryDeleteTempFile(session.TempFile);
                }

                long fileBytes = 0;
                try
                {
                    if (File.Exists(session.TempFile)) fileBytes = new FileInfo(session.TempFile).Length;
                }
                catch (Exception ex)
                {
                    LogError("recording_file_size_failed", ex, session.TempFile);
                }

                session.CompletionError = completionError;
                string outcome = session.StartFailed
                    ? "recording_start_cleanup"
                    : Volatile.Read(ref session.StopStarted) != 0
                        ? "recording_stop"
                        : "recording_ended_unexpectedly";
                DiagLog.Write("Audio", outcome,
                    ("ms", session.RecordingStopwatch.ElapsedMilliseconds),
                    ("pcmBytes", session.BytesRecorded),
                    ("fileBytes", fileBytes),
                    ("buffers", session.BuffersRecorded),
                    ("peak", session.PeakMax.ToString("0.000")),
                    ("error", completionError?.ToString()),
                    ("path", session.TempFile));

                if (completionError != null || Volatile.Read(ref session.StopStarted) == 0)
                    TryDeleteTempFile(session.TempFile);
            }
            finally
            {
                lock (_stateLock)
                {
                    if (ReferenceEquals(_session, session)) _session = null;
                }

                session.Stopped.TrySetResult();
            }
        }

        private static void CleanupStartupResources(WaveInEvent? waveIn, WaveFileWriter? writer, string tempFile)
        {
            try
            {
                waveIn?.Dispose();
            }
            catch (Exception ex)
            {
                LogError("recording_start_input_cleanup_failed", ex, tempFile);
            }

            try
            {
                writer?.Dispose();
            }
            catch (Exception ex)
            {
                LogError("recording_start_writer_cleanup_failed", ex, tempFile);
            }

            TryDeleteTempFile(tempFile);
        }

        private static void TryDeleteTempFile(string tempFile)
        {
            try
            {
                if (File.Exists(tempFile)) File.Delete(tempFile);
            }
            catch (Exception ex)
            {
                LogError("recording_temp_delete_failed", ex, tempFile);
            }
        }

        private static void LogError(string message, Exception ex, string? path)
        {
            Console.WriteLine($"AudioRecorder: {message}: {ex.Message}");
            DiagLog.Write("Audio", message, ("error", ex.ToString()), ("path", path));
        }

        public void Dispose()
        {
            lock (_stateLock)
            {
                if (_disposed) return;
                _disposed = true;
            }

            Stop();
        }

        private sealed class RecordingSession
        {
            public RecordingSession(WaveInEvent waveIn, WaveFileWriter writer, string tempFile)
            {
                WaveIn = waveIn;
                Writer = writer;
                TempFile = tempFile;
            }

            public WaveInEvent WaveIn { get; }
            public WaveFileWriter? Writer { get; set; }
            public string TempFile { get; }
            public object WriterLock { get; } = new();
            public TaskCompletionSource StartCompleted { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
            public TaskCompletionSource Stopped { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);
            public Stopwatch RecordingStopwatch { get; } = new();
            public EventHandler<WaveInEventArgs>? DataAvailableHandler { get; set; }
            public EventHandler<StoppedEventArgs>? RecordingStoppedHandler { get; set; }
            public Exception? CompletionError { get; set; }
            public bool StartFailed { get; set; }
            public int StopStarted;
            public int CompletionStarted;
            public long BytesRecorded;
            public long BuffersRecorded;
            public float PeakMax;
        }
    }
}
