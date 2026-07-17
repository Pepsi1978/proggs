using System;
using System.Buffers;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Threading;
using NAudio.Wave;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Isolierter Mikrofon-Aufnahme-Prozess (Prozess-Isolation gegen den
    /// nicht abfangbaren WinMM-Crash). Laeuft als eigene Instanz derselben
    /// EXE, gestartet mit <c>--capture-worker</c>. Die gesamte gefaehrliche
    /// NAudio/WinMM-<c>waveIn*</c>-Schicht (siehe
    /// <c>bugs/desktop/voice-pipeline.md</c> §3.2 — <c>waveInReset</c>/
    /// <c>waveInPrepareHeader</c> AccessViolation, NAudio #1150/#657/#1084/
    /// #1203, alle OPEN) lebt NUR in diesem Kindprozess. Ein Crash hier
    /// (0xc0000005, Corrupted-State-Exception, in .NET nicht per try/catch
    /// abfangbar) killt ausschliesslich DIESEN Prozess — das Overlay-UI und
    /// der bereits gesprochene/getippte Text bleiben unberuehrt.
    ///
    /// Zwei Robustheits-Garantien:
    /// 1. Die WAV wird bei JEDEM Buffer geflusht (Header-Laengen mitgefuehrt),
    ///    also ist die Datei zu jedem Zeitpunkt valide. Selbst ein Crash
    ///    MITTEN in der Aufnahme verliert nur den allerletzten ~50-ms-Buffer;
    ///    der Parent transkribiert das bereits Aufgenommene.
    /// 2. Beim Stopp wird der Writer ZUERST geschlossen (WAV vollstaendig auf
    ///    Platte), DANN erst <c>WaveIn.Dispose()</c> aufgerufen (die Stelle,
    ///    die crashen kann). Selbst wenn dieser Dispose crasht, ist die
    ///    Aufnahme schon sicher.
    ///
    /// Protokoll (zeilenbasiert, InvariantCulture):
    ///   stdout  Parent liest:   READY | L &lt;0..1000&gt; | DONE &lt;fileBytes&gt; &lt;pcmBytes&gt; &lt;buffers&gt; &lt;peakMille&gt; | ERR &lt;msg&gt;
    ///   stdin   Parent schreibt: STOP   (EOF = Parent tot = ebenfalls stoppen)
    ///   stderr  freie Diagnose-Zeilen — der Parent leitet sie in DiagLog
    ///           (nur EIN Prozess schreibt so in diag.log, keine Datei-Races).
    /// </summary>
    public static class CaptureWorker
    {
        // Notbremse: falls nie ein STOP kommt (Parent-Bug), nie endlos
        // aufnehmen und die Platte fluten. Grosszuegig — niemand diktiert am
        // Stueck so lange; rein defensiv (Direktive 3).
        private static readonly TimeSpan MaxRecording = TimeSpan.FromMinutes(20);
        private static readonly TimeSpan FlushInterval = TimeSpan.FromMilliseconds(200);
        private static readonly TimeSpan WriterDrainTimeout = TimeSpan.FromSeconds(4);
        private const long LevelEmitIntervalMs = 100;

        private static readonly object _writerLock = new();
        private static readonly ManualResetEventSlim _finished = new(false);
        private static readonly BlockingCollection<CapturedBuffer> _writeQueue = new();
        private static WaveFileWriter? _writer;
        private static WaveInEvent? _waveIn;
        private static Thread? _writerThread;
        private static string _outPath = "";
        private static long _pcmBytes;
        private static long _buffers;
        private static int _peakMille;
        private static int _stopping;          // 0/1 via Interlocked
        private static int _finalizing;        // 0/1 via Interlocked
        private static int _pendingLevelPeak;
        private static long _lastLevelEmitTicks;
        private static readonly Stopwatch _sinceFlush = new();

        /// <summary>
        /// Einstieg aus <c>App.OnStartup</c>. Blockiert bis die Aufnahme
        /// beendet ist und ruft danach <see cref="Environment.Exit(int)"/> —
        /// kehrt also nie zurueck (die WPF-Pipeline wird nie betreten).
        /// </summary>
        public static void Run(string[] args)
        {
            // NAudio postet RecordingStopped ueber den SynchronizationContext,
            // der beim StartRecording aktiv ist. Dieser Worker-Thread stammt
            // aus App.OnStartup und traegt den WPF-Dispatcher-Context — den wir
            // aber mit _finished.Wait blockieren. Ohne Entfernen wuerde
            // RecordingStopped nie feuern und der Stop erst per 3-s-Timeout
            // finalisieren (spuerbare Latenz). Null-Context → NAudio ruft den
            // Callback direkt vom Capture-Thread auf.
            SynchronizationContext.SetSynchronizationContext(null);

            int rate = ArgInt(args, "--rate", 16000);
            int channels = ArgInt(args, "--channels", 1);
            _outPath = ArgStr(args, "--out", "") ?? "";

            if (string.IsNullOrWhiteSpace(_outPath))
            {
                Emit("ERR missing --out");
                Environment.Exit(2);
                return;
            }

            try
            {
                var format = new WaveFormat(rate, 16, channels);
                lock (_writerLock)
                {
                    _writer = new WaveFileWriter(_outPath, format);
                }
                _writerThread = new Thread(WriterLoop)
                {
                    IsBackground = true,
                    Name = "capture-wav-writer"
                };
                _writerThread.Start();
                _waveIn = new WaveInEvent
                {
                    WaveFormat = format,
                    BufferMilliseconds = 50,
                    NumberOfBuffers = 3
                };
                _waveIn.DataAvailable += OnDataAvailable;
                _waveIn.RecordingStopped += OnRecordingStopped;
                _waveIn.StartRecording();
                _sinceFlush.Restart();
            }
            catch (Exception ex)
            {
                Log("start_failed", ex.ToString());
                Emit("ERR " + OneLine(ex.Message));
                SafeCloseWriterOnly();
                Environment.Exit(2);
                return;
            }

            Emit("READY");
            Log("worker_ready", $"rate={rate} channels={channels} pid={Environment.ProcessId}");

            // stdin-Leser: STOP oder EOF (Parent tot) beenden die Aufnahme.
            var stdinThread = new Thread(StdinLoop) { IsBackground = true, Name = "capture-stdin" };
            stdinThread.Start();

            // Warten bis stop finalisiert ODER Notbremse greift.
            if (!_finished.Wait(MaxRecording))
            {
                Log("max_recording_reached", $"{MaxRecording.TotalMinutes:0} min cap");
                RequestStop();
                _finished.Wait(TimeSpan.FromSeconds(5));
            }

            // Fallback-Exit (der reguelaere Exit passiert in FinalizeAndExit).
            Environment.Exit(0);
        }

        private static void StdinLoop()
        {
            try
            {
                string? line;
                while ((line = Console.In.ReadLine()) != null)
                {
                    if (line.Trim().Equals("STOP", StringComparison.OrdinalIgnoreCase))
                    {
                        RequestStop();
                        return;
                    }
                }
                // EOF: Parent-Pipe geschlossen → Parent ist weg → stoppen.
                Log("stdin_eof", "parent gone, stopping");
                RequestStop();
            }
            catch (Exception ex)
            {
                Log("stdin_loop_failed", ex.Message);
                RequestStop();
            }
        }

        private static void RequestStop()
        {
            if (Interlocked.Exchange(ref _stopping, 1) != 0) return;
            try
            {
                // Native Stop kann blockieren/crashen → in eigenem Thread mit
                // Timeout. RecordingStopped finalisiert; kommt es nicht, greift
                // der Timeout und wir finalisieren selbst.
                var stopThread = new Thread(() =>
                {
                    try { _waveIn?.StopRecording(); }
                    catch (Exception ex) { Log("stop_recording_failed", ex.Message); FinalizeAndExit(); }
                })
                { IsBackground = true, Name = "capture-stop" };
                stopThread.Start();

                if (!_finished.Wait(TimeSpan.FromSeconds(3)))
                {
                    Log("stop_timeout", "RecordingStopped did not fire in 3s — finalizing from writer");
                    FinalizeAndExit();
                }
            }
            catch (Exception ex)
            {
                Log("request_stop_failed", ex.Message);
                FinalizeAndExit();
            }
        }

        private static void OnDataAvailable(object? sender, WaveInEventArgs e)
        {
            if (e.BytesRecorded <= 0) return;

            byte[] copy = ArrayPool<byte>.Shared.Rent(e.BytesRecorded);
            Buffer.BlockCopy(e.Buffer, 0, copy, 0, e.BytesRecorded);
            try
            {
                _writeQueue.Add(new CapturedBuffer(copy, e.BytesRecorded));
            }
            catch (InvalidOperationException)
            {
                ArrayPool<byte>.Shared.Return(copy);
                return;
            }

            _pcmBytes += e.BytesRecorded;
            _buffers++;

            int peak = 0;
            int sampleCount = e.BytesRecorded / 2;
            for (int i = 0; i < sampleCount; i++)
            {
                short sample = (short)(e.Buffer[i * 2] | (e.Buffer[i * 2 + 1] << 8));
                int abs = sample < 0 ? -sample : sample;
                if (abs > peak) peak = abs;
            }
            int mille = (int)(peak / 32768f * 1000f);
            if (mille > _peakMille) _peakMille = mille;
            if (mille > _pendingLevelPeak) _pendingLevelPeak = mille;

            long now = Environment.TickCount64;
            if (now - _lastLevelEmitTicks >= LevelEmitIntervalMs)
            {
                _lastLevelEmitTicks = now;
                int level = Interlocked.Exchange(ref _pendingLevelPeak, 0);
                Emit("L " + level.ToString(CultureInfo.InvariantCulture));
            }
        }

        private static void WriterLoop()
        {
            try
            {
                foreach (var buffer in _writeQueue.GetConsumingEnumerable())
                {
                    try
                    {
                        lock (_writerLock)
                        {
                            var writer = _writer;
                            if (writer == null) continue;
                            writer.Write(buffer.Data, 0, buffer.Count);
                            if (_sinceFlush.Elapsed >= FlushInterval)
                            {
                                writer.Flush();
                                _sinceFlush.Restart();
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Log("write_failed", ex.Message);
                    }
                    finally
                    {
                        ArrayPool<byte>.Shared.Return(buffer.Data);
                    }
                }
            }
            catch (Exception ex)
            {
                Log("writer_loop_failed", ex.Message);
            }
        }

        private static void OnRecordingStopped(object? sender, StoppedEventArgs e)
        {
            if (e.Exception != null) Log("recording_stopped_error", e.Exception.Message);
            FinalizeAndExit();
        }

        /// <summary>
        /// Queue leeren, Writer schliessen (WAV vollstaendig + valide), DONE
        /// melden und ohne crash-anfaelliges WaveIn-Dispose beenden. Idempotent.
        /// </summary>
        private static void FinalizeAndExit()
        {
            if (Interlocked.Exchange(ref _finalizing, 1) != 0) return;
            try { _writeQueue.CompleteAdding(); } catch { /* idempotent */ }
            if (_writerThread is { } writerThread && !writerThread.Join(WriterDrainTimeout))
            {
                Log("writer_drain_timeout", $"queue={_writeQueue.Count}");
                _finished.Set();
                Environment.Exit(3);
                return;
            }

            long fileBytes = CloseWriterAndMeasure();

            Emit($"DONE {fileBytes} {_pcmBytes} {_buffers} {_peakMille}");
            Log("worker_done", $"fileBytes={fileBytes} pcmBytes={_pcmBytes} buffers={_buffers} peakMille={_peakMille}");
            _finished.Set();

            // Der Prozess endet jetzt ohnehin. Windows gibt die nativen WaveIn-
            // Handles beim Prozessende frei; ein explizites Dispose wuerde nur
            // erneut den bekannten waveInReset-Crash oder einen Endlos-Hang
            // riskieren, nachdem die WAV bereits vollstaendig gesichert ist.
            Environment.Exit(0);
        }

        private static long CloseWriterAndMeasure()
        {
            lock (_writerLock)
            {
                try
                {
                    _writer?.Flush();
                    _writer?.Dispose();
                }
                catch (Exception ex)
                {
                    Log("writer_close_failed", ex.Message);
                }
                finally
                {
                    _writer = null;
                }
            }

            try
            {
                if (File.Exists(_outPath)) return new FileInfo(_outPath).Length;
            }
            catch (Exception ex)
            {
                Log("file_size_failed", ex.Message);
            }
            return 0;
        }

        private static void SafeCloseWriterOnly()
        {
            lock (_writerLock)
            {
                try { _writer?.Dispose(); } catch { /* tolerant */ }
                _writer = null;
            }
            try { if (File.Exists(_outPath)) File.Delete(_outPath); } catch { /* tolerant */ }
        }

        // ── stdout/stderr Protokoll ──

        private static readonly object _emitLock = new();
        private static void Emit(string line)
        {
            lock (_emitLock)
            {
                try { Console.Out.WriteLine(line); Console.Out.Flush(); }
                catch { /* Parent-Pipe evtl. zu — egal, Worker beendet ohnehin */ }
            }
        }

        private static void Log(string tag, string detail)
        {
            try { Console.Error.WriteLine(tag + " | " + OneLine(detail)); Console.Error.Flush(); }
            catch { /* tolerant */ }
        }

        private static string OneLine(string s) =>
            (s ?? "").Replace('\r', ' ').Replace('\n', ' ');

        private readonly record struct CapturedBuffer(byte[] Data, int Count);

        private static int ArgInt(string[] args, string key, int fallback)
        {
            var v = ArgStr(args, key, null);
            return int.TryParse(v, NumberStyles.Integer, CultureInfo.InvariantCulture, out var n) ? n : fallback;
        }

        private static string? ArgStr(string[] args, string key, string? fallback)
        {
            for (int i = 0; i < args.Length - 1; i++)
                if (args[i] == key) return args[i + 1];
            return fallback;
        }
    }
}
