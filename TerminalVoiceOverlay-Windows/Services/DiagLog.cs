using System;
using System.IO;
using System.Text;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Minimaler, fehlertoleranter JSON-Lines-Datei-Logger fuer Laufzeit-Diagnose
    /// (Observability-First). Die fensterlose self-contained single-file EXE hat KEINE
    /// sichtbare Konsole — Console.WriteLine ist dort unsichtbar. Dieser Logger schreibt
    /// strukturierte Zeilen in eine feste Datei, die per
    ///   Get-Content "$env:LOCALAPPDATA\TerminalVoiceOverlay\diag.log" -Wait -Tail 30
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
        private static bool _announced;

        public static string FilePath => _path;

        private static string ResolvePath()
        {
            try
            {
                string dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "TerminalVoiceOverlay");
                Directory.CreateDirectory(dir);
                return Path.Combine(dir, "diag.log");
            }
            catch
            {
                return Path.Combine(Path.GetTempPath(), "TerminalVoiceOverlay-diag.log");
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

                lock (_lock)
                {
                    RotateIfBig();
                    if (!_announced)
                    {
                        _announced = true;
                        File.AppendAllText(_path,
                            "{\"ts\":\"" + DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss.fff") +
                            "\",\"ctx\":\"DiagLog\",\"msg\":\"=== session start ===\"}\n",
                            Encoding.UTF8);
                    }
                    File.AppendAllText(_path, sb.ToString() + "\n", Encoding.UTF8);
                }
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

        private static string Escape(string s)
        {
            if (string.IsNullOrEmpty(s)) return "";
            return s.Replace("\\", "\\\\").Replace("\"", "\\\"")
                    .Replace("\r", " ").Replace("\n", " ").Replace("\t", " ");
        }
    }
}
