using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Mikrofon-Aufnahme mit PROZESS-ISOLATION: die eigentliche NAudio/WinMM-
    /// Aufnahme laeuft in einem separaten Kindprozess (<see cref="CaptureWorker"/>,
    /// gestartet mit <c>--capture-worker</c>). Grund: der WinMM-<c>waveIn*</c>-
    /// Crash (0xc0000005 AccessViolation in <c>waveInReset</c>/
    /// <c>waveInPrepareHeader</c>, NAudio #1150/#657/#1084/#1203, alle OPEN —
    /// siehe <c>bugs/desktop/voice-pipeline.md</c> §3.2) ist eine
    /// Corrupted-State-Exception und in .NET NICHT per try/catch abfangbar;
    /// er wuerde den ganzen Overlay-Prozess killen. Im Kindprozess killt er
    /// nur den Worker — die UI und der bereits gesprochene Text ueberleben.
    ///
    /// Die oeffentliche API (<see cref="Start"/>, <see cref="StopAsync"/>,
    /// <see cref="LevelChanged"/>, <see cref="IsRecording"/>) ist unveraendert,
    /// sodass alle Aufrufer gleich bleiben. <see cref="Start"/> blockiert bis
    /// der Worker <c>READY</c> meldet — dadurch spielt der bestehende
    /// Aufruf-Ablauf (Start → PlayStart-Cue) den Start-Ton erst bei echtem
    /// Aufnahmebeginn (keine verlorenen ersten Woerter).
    ///
    /// Datenverlust-Schutz: Der Worker flusht die WAV laufend (Header-Laengen
    /// mitgefuehrt) und schliesst sie VOR dem crash-anfaelligen Dispose. Selbst
    /// bei Worker-Crash liest dieser Wrapper die bis dahin aufgenommene, valide
    /// WAV und gibt sie zur Transkription zurueck — kein Neu-Einsprechen.
    /// </summary>
    public sealed class AudioRecorder : IDisposable
    {
        private static readonly TimeSpan ReadyTimeout = TimeSpan.FromSeconds(4);
        private static readonly TimeSpan StopTimeout = TimeSpan.FromSeconds(5);
        // Kleinster sinnvoller WAV-Inhalt (44 Byte Header + etwas PCM). Darunter
        // gilt die Aufnahme als leer/unbrauchbar.
        private const long MinUsableWavBytes = 44 + 320;

        private readonly int _sampleRate;
        private readonly int _channels;
        private readonly object _stateLock = new();
        private Session? _session;
        private bool _disposed;

        /// <summary>
        /// Peak-Pegel des aktuellen Buffers, normiert 0..1. Wird ~alle 50 ms
        /// aus dem Worker-Datenstrom gefeuert. Subscriber laufen auf einem
        /// Pool-Thread — UI-Updates muessen auf den Dispatcher gehoben werden
        /// (unveraendertes Verhalten gegenueber der In-Process-Aufnahme).
        /// </summary>
        public event Action<float>? LevelChanged;

        public bool IsRecording
        {
            get { lock (_stateLock) { return _session != null; } }
        }

        public AudioRecorder(int sampleRate = 16000, int channels = 1)
        {
            _sampleRate = sampleRate;
            _channels = channels;
        }

        public bool Start()
        {
            var setupSw = Stopwatch.StartNew();
            string tempFile = Path.Combine(Path.GetTempPath(), $"tvo_recording_{Guid.NewGuid():N}.wav");
            Session session;

            lock (_stateLock)
            {
                ObjectDisposedException.ThrowIf(_disposed, this);
                if (_session != null) return false;
            }

            try
            {
                session = new Session(tempFile);
                var psi = new ProcessStartInfo
                {
                    FileName = Environment.ProcessPath!,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardInput = true,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                };
                psi.ArgumentList.Add("--capture-worker");
                psi.ArgumentList.Add("--rate");
                psi.ArgumentList.Add(_sampleRate.ToString(CultureInfo.InvariantCulture));
                psi.ArgumentList.Add("--channels");
                psi.ArgumentList.Add(_channels.ToString(CultureInfo.InvariantCulture));
                psi.ArgumentList.Add("--out");
                psi.ArgumentList.Add(tempFile);

                session.Proc = new Process { StartInfo = psi };
                session.Proc.OutputDataReceived += (_, e) => OnWorkerStdout(session, e.Data);
                session.Proc.ErrorDataReceived += (_, e) => OnWorkerStderr(e.Data);

                if (!session.Proc.Start())
                {
                    LogError("recording_start_process_failed", new InvalidOperationException("Process.Start returned false"), tempFile);
                    return false;
                }
                session.Proc.BeginOutputReadLine();
                session.Proc.BeginErrorReadLine();
            }
            catch (Exception ex)
            {
                LogError("recording_start_failed", ex, tempFile);
                return false;
            }

            // Auf READY warten — erst dann laeuft die Aufnahme wirklich.
            bool ready = session.Ready.Wait(ReadyTimeout);
            if (!ready || session.StartError != null)
            {
                LogError("recording_start_not_ready",
                    new TimeoutException(session.StartError ?? $"Worker meldete kein READY in {ReadyTimeout.TotalSeconds:0}s"),
                    tempFile);
                KillWorker(session);
                TryDeleteTempFile(tempFile);
                return false;
            }

            lock (_stateLock)
            {
                if (_session != null)
                {
                    // Race: parallel gestartet — diesen Worker verwerfen.
                    KillWorker(session);
                    TryDeleteTempFile(tempFile);
                    return false;
                }
                _session = session;
            }

            session.RecordingStopwatch.Restart();
            session.TouchLevel();
            session.StartWatchdog(() => WatchdogTick(session));

            DiagLog.Write("Audio", "recording_start",
                ("setupMs", setupSw.ElapsedMilliseconds),
                ("sampleRate", _sampleRate),
                ("channels", _channels),
                ("workerPid", SafePid(session)),
                ("path", tempFile));
            return true;
        }

        public string? Stop() => StopAsync().GetAwaiter().GetResult();

        public async Task<string?> StopAsync()
        {
            Session? session;
            lock (_stateLock)
            {
                session = _session;
                _session = null;
            }
            if (session == null) return null;
            if (Interlocked.Exchange(ref session.StopStarted, 1) != 0) return null;

            session.StopWatchdog();
            session.RecordingStopwatch.Stop();

            // STOP senden (Pipe kann zu sein, wenn der Worker schon crashte).
            try
            {
                session.Proc!.StandardInput.WriteLine("STOP");
                session.Proc.StandardInput.Flush();
            }
            catch (Exception ex)
            {
                DiagLog.Write("Audio", "stop_signal_failed", ("err", ex.Message), ("path", session.TempFile));
            }

            bool exited;
            try
            {
                using var cts = new CancellationTokenSource(StopTimeout);
                await session.Proc!.WaitForExitAsync(cts.Token).ConfigureAwait(false);
                exited = true;
            }
            catch (OperationCanceledException)
            {
                exited = false;
                DiagLog.Write("Audio", "stop_wait_timeout", ("path", session.TempFile));
                KillWorker(session);
            }

            int exitCode = SafeExitCode(session);
            long fileBytes = SafeFileSize(session.TempFile);
            bool usable = fileBytes >= MinUsableWavBytes;

            if (exited && exitCode != 0)
            {
                // Worker gecrasht (z.B. waveInReset AccessViolation). Dank
                // laufendem Flush ist die WAV dennoch bis kurz vor dem Crash
                // valide — der Text ist gerettet, nur die UI blieb verschont.
                DiagLog.Write("Audio", "capture_worker_crashed",
                    ("exitCode", exitCode),
                    ("fileBytes", fileBytes),
                    ("usableWav", usable),
                    ("path", session.TempFile));
            }

            DiagLog.Write("Audio", "recording_stop",
                ("ms", session.RecordingStopwatch.ElapsedMilliseconds),
                ("pcmBytes", session.PcmBytes),
                ("fileBytes", fileBytes),
                ("buffers", session.Buffers),
                ("peak", (session.PeakMille / 1000f).ToString("0.000", CultureInfo.InvariantCulture)),
                ("exitCode", exited ? exitCode : (int?)null),
                ("path", session.TempFile));

            DisposeProc(session);

            if (usable) return session.TempFile;
            TryDeleteTempFile(session.TempFile);
            return null;
        }

        // ── Worker-Ausgabe ──

        private void OnWorkerStdout(Session session, string? line)
        {
            if (string.IsNullOrEmpty(line)) return;
            try
            {
                if (line == "READY")
                {
                    session.Ready.Set();
                }
                else if (line.Length > 2 && line[0] == 'L' && line[1] == ' ')
                {
                    session.TouchLevel();
                    if (int.TryParse(line.AsSpan(2), NumberStyles.Integer, CultureInfo.InvariantCulture, out int mille))
                    {
                        if (mille > session.PeakMille) session.PeakMille = mille;
                        try { LevelChanged?.Invoke(mille / 1000f); }
                        catch (Exception ex) { DiagLog.Write("Audio", "level_listener_failed", ("err", ex.Message)); }
                    }
                }
                else if (line.StartsWith("DONE ", StringComparison.Ordinal))
                {
                    var parts = line.Split(' ', StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length >= 5)
                    {
                        session.PcmBytes = ParseLong(parts[2], session.PcmBytes);
                        session.Buffers = ParseLong(parts[3], session.Buffers);
                        session.PeakMille = (int)ParseLong(parts[4], session.PeakMille);
                    }
                }
                else if (line.StartsWith("ERR ", StringComparison.Ordinal))
                {
                    session.StartError = line.Substring(4);
                    session.Ready.Set();
                }
            }
            catch (Exception ex)
            {
                DiagLog.Write("Audio", "worker_stdout_parse_failed", ("err", ex.Message), ("line", line));
            }
        }

        private static void OnWorkerStderr(string? line)
        {
            if (string.IsNullOrEmpty(line)) return;
            DiagLog.Write("CaptureWorker", "worker_log", ("line", line));
        }

        // ── Level-Watchdog (Sonde): erkennt "Balken eingefroren" ──
        // Wenn trotz laufender Aufnahme > 2,5 s kein Pegel mehr kommt, ist der
        // Datenstrom versiegt (DataAvailable stumm, §3.1) oder der Worker haengt.
        // Wir loggen das EINMAL pro Session — sichtbar in diag.log, damit der
        // Vorfall in Zukunft nachvollziehbar ist (Observability-first).
        private static void WatchdogTick(Session session)
        {
            try
            {
                if (Volatile.Read(ref session.StopStarted) != 0) return;
                long sinceMs = Environment.TickCount64 - Volatile.Read(ref session.LastLevelTicks);
                if (sinceMs <= 2500) return;

                bool workerAlive = !SafeHasExited(session);
                if (session.StallLogged == 0 && Interlocked.Exchange(ref session.StallLogged, 1) == 0)
                {
                    DiagLog.Write("Audio", "capture_level_stalled",
                        ("silentMs", sinceMs),
                        ("workerAlive", workerAlive),
                        ("path", session.TempFile));
                }
            }
            catch { /* Watchdog darf nie werfen */ }
        }

        // ── Prozess-Hilfen ──

        private static void KillWorker(Session session)
        {
            try
            {
                if (session.Proc is { } p && !p.HasExited) p.Kill(entireProcessTree: true);
            }
            catch (Exception ex)
            {
                DiagLog.Write("Audio", "worker_kill_failed", ("err", ex.Message), ("path", session.TempFile));
            }
        }

        private static void DisposeProc(Session session)
        {
            session.StopWatchdog();
            try { session.Proc?.Dispose(); } catch { /* tolerant */ }
        }

        private static int SafeExitCode(Session session)
        {
            try { return session.Proc is { HasExited: true } p ? p.ExitCode : 0; }
            catch { return 0; }
        }

        private static bool SafeHasExited(Session session)
        {
            try { return session.Proc?.HasExited ?? true; }
            catch { return true; }
        }

        private static int SafePid(Session session)
        {
            try { return session.Proc?.Id ?? -1; }
            catch { return -1; }
        }

        private static long SafeFileSize(string path)
        {
            try { return File.Exists(path) ? new FileInfo(path).Length : 0; }
            catch { return 0; }
        }

        private static long ParseLong(string s, long fallback) =>
            long.TryParse(s, NumberStyles.Integer, CultureInfo.InvariantCulture, out var n) ? n : fallback;

        private static void TryDeleteTempFile(string tempFile)
        {
            try { if (File.Exists(tempFile)) File.Delete(tempFile); }
            catch (Exception ex) { DiagLog.Write("Audio", "recording_temp_delete_failed", ("err", ex.Message), ("path", tempFile)); }
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
            try { Stop(); } catch { /* tolerant */ }
        }

        private sealed class Session
        {
            public Session(string tempFile) => TempFile = tempFile;

            public string TempFile { get; }
            public Process? Proc;
            public readonly ManualResetEventSlim Ready = new(false);
            public volatile string? StartError;
            public readonly Stopwatch RecordingStopwatch = new();
            public long PcmBytes;
            public long Buffers;
            public int PeakMille;
            public int StopStarted;
            public int StallLogged;
            public long LastLevelTicks;
            private Timer? _watchdog;

            public void TouchLevel() => Volatile.Write(ref LastLevelTicks, Environment.TickCount64);

            public void StartWatchdog(Action tick)
                => _watchdog = new Timer(_ => tick(), null, 1000, 1000);

            public void StopWatchdog()
            {
                try { _watchdog?.Dispose(); } catch { /* tolerant */ }
                _watchdog = null;
            }
        }
    }
}
