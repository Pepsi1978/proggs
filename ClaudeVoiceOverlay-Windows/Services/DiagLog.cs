using System;
using System.Diagnostics;
using System.Collections.Concurrent;
using System.IO;
using System.Text;
using System.Threading.Tasks;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Minimaler, fehlertoleranter JSON-Lines-Datei-Logger fuer Laufzeit-Diagnose
    /// (Observability-First). Die fensterlose self-contained single-file EXE hat KEINE
    /// sichtbare Konsole — Console.WriteLine ist dort unsichtbar. Dieser Logger schreibt
    /// strukturierte Zeilen in eine feste Datei, die per
    ///   Get-Content "$env:LOCALAPPDATA\ClaudeVoiceOverlay\diag.log" -Wait -Tail 30
    /// live mitgelesen werden kann.
    ///
    /// Format: {"ts":"2026-...","ctx":"UIA","msg":"...", &lt;weitere k/v&gt;}
    /// Grundsatz: Logging darf NIE den Ablauf stoeren — jede Methode ist werf-frei.
    /// Single-File-sicher: nutzt LocalApplicationData, NICHT Assembly.Location (IL3000).
    /// </summary>
    public static class DiagLog
    {
        private static readonly object _lock = new();
        private static readonly string _path = ResolvePath();
        private static readonly BlockingCollection<string> _queue = new();
        private static readonly Task _writerTask = Task.Factory.StartNew(
            WriterLoop,
            TaskCreationOptions.LongRunning);
        private static bool _announced;

        static DiagLog()
        {
            AppDomain.CurrentDomain.ProcessExit += (_, _) => FlushOnExit();
            AppDomain.CurrentDomain.DomainUnload += (_, _) => FlushOnExit();
        }

        public static string FilePath => _path;

        public static void Info(string ctx, string msg, params (string Key, object? Value)[] extra)
            => Write(ctx, msg, extra);

        public static void Warn(string ctx, string msg, params (string Key, object? Value)[] extra)
            => Write(ctx, msg, Combine(("level", "WARN"), extra));

        public static void Error(string ctx, string msg, Exception ex, params (string Key, object? Value)[] extra)
            => Write(ctx, msg, Combine(("level", "ERROR"), Combine(("err", ex.Message), Combine(("type", ex.GetType().Name), extra))));

        public static void Perf(string ctx, string step, Stopwatch sw, params (string Key, object? Value)[] extra)
            => Write(ctx, "perf", Combine(("step", step), Combine(("ms", sw.ElapsedMilliseconds), extra)));

        private static string ResolvePath()
        {
            try
            {
                string dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "ClaudeVoiceOverlay");
                Directory.CreateDirectory(dir);
                return Path.Combine(dir, "diag.log");
            }
            catch
            {
                return Path.Combine(Path.GetTempPath(), "ClaudeVoiceOverlay-diag.log");
            }
        }

        /// <summary>Schreibt eine JSON-Zeile. Niemals werfend.</summary>
        public static void Write(string ctx, string msg, params (string Key, object? Value)[] extra)
        {
            try
            {
                var sb = new StringBuilder(256);
                sb.Append('{');
                AppendKv(sb, "ts", DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss.fff"));
                sb.Append(','); AppendKv(sb, "ctx", ctx);
                sb.Append(','); AppendKv(sb, "msg", msg);
                if (extra != null)
                    foreach (var (k, v) in extra)
                    {
                        sb.Append(',');
                        AppendKv(sb, k, v?.ToString() ?? "null");
                    }
                sb.Append('}');

                EnqueueLine(sb.ToString());
            }
            catch { /* Logging darf nie crashen */ }
        }

        private static void EnqueueLine(string line)
        {
            lock (_lock)
            {
                if (!_announced)
                {
                    _announced = true;
                    _queue.Add("{\"ts\":\"" + DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss.fff") +
                               "\",\"ctx\":\"DiagLog\",\"msg\":\"=== session start ===\"}");
                }
                _queue.Add(line);
            }
        }

        private static void WriterLoop()
        {
            try
            {
                foreach (var line in _queue.GetConsumingEnumerable())
                {
                    try
                    {
                        RotateIfBig();
                        File.AppendAllText(_path, line + "\n", Encoding.UTF8);
                    }
                    catch { /* Logging darf nie crashen */ }
                }
            }
            catch { /* Logging darf nie crashen */ }
        }

        private static void FlushOnExit()
        {
            try
            {
                _queue.CompleteAdding();
                _writerTask.Wait(500);
            }
            catch { /* Logging darf nie crashen */ }
        }

        private static void RotateIfBig()
        {
            try
            {
                var fi = new FileInfo(_path);
                if (fi.Exists && fi.Length > 1_000_000) // 1 MB -> rotieren
                {
                    string bak = _path + ".1";
                    if (File.Exists(bak)) File.Delete(bak);
                    File.Move(_path, bak);
                }
            }
            catch { /* tolerant */ }
        }

        private static void AppendKv(StringBuilder sb, string key, string val)
        {
            sb.Append('"').Append(Escape(key)).Append("\":\"").Append(Escape(val)).Append('"');
        }

        private static (string Key, object? Value)[] Combine((string Key, object? Value) first, params (string Key, object? Value)[] rest)
        {
            var result = new (string Key, object? Value)[(rest?.Length ?? 0) + 1];
            result[0] = first;
            if (rest != null && rest.Length > 0)
                Array.Copy(rest, 0, result, 1, rest.Length);
            return result;
        }

        private static string Escape(string s)
        {
            if (string.IsNullOrEmpty(s)) return "";
            return s.Replace("\\", "\\\\").Replace("\"", "\\\"")
                    .Replace("\r", " ").Replace("\n", " ").Replace("\t", " ");
        }
    }
}
