using System;
using System.IO;
using System.Text;

namespace VoiceAgent.Diagnostics
{
    /// <summary>
    /// Zentrales Diagnose-Log fuer die gesamte App. Thread-safe, append-only,
    /// timestamped, UTF-8. Schluckt eigene Fehler — Logging darf die App NIEMALS
    /// zum Absturz bringen.
    ///
    /// Datei: %LOCALAPPDATA%\VoiceAgent\logs\voiceagent.log
    /// Zweck: nach einem Lauf nachvollziehen, ob/welche Funktion versagt hat und wo
    /// (Sonden-Prinzip). Jede Komponente loggt Start, wichtige Schritte und Fehler.
    /// </summary>
    public static class Log
    {
        private static readonly object _gate = new();
        private static readonly string _logPath;

        static Log()
        {
            string path;
            try
            {
                string dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "VoiceAgent", "logs");
                Directory.CreateDirectory(dir);
                path = Path.Combine(dir, "voiceagent.log");
            }
            catch
            {
                // Fallback: TEMP, falls LOCALAPPDATA nicht beschreibbar ist.
                path = Path.Combine(Path.GetTempPath(), "voiceagent.log");
            }
            _logPath = path;
            Info("================ VoiceAgent gestartet ================");
        }

        /// <summary>Voller Pfad der aktuellen Log-Datei (fuer die UI: "Log oeffnen").</summary>
        public static string LogFilePath => _logPath;

        public static void Info(string message) => Write("INFO ", message, null);
        public static void Warn(string message) => Write("WARN ", message, null);
        public static void Error(string message, Exception? ex = null) => Write("ERROR", message, ex);

        private static void Write(string level, string message, Exception? ex)
        {
            try
            {
                var sb = new StringBuilder(160);
                sb.Append(DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff"))
                  .Append("  [").Append(level).Append("]  ")
                  .Append(message);

                if (ex != null)
                {
                    sb.Append("  | ").Append(ex.GetType().Name).Append(": ").Append(ex.Message);
                    if (!string.IsNullOrEmpty(ex.StackTrace))
                        sb.Append(Environment.NewLine).Append(ex.StackTrace);
                }
                sb.Append(Environment.NewLine);

                lock (_gate)
                {
                    File.AppendAllText(_logPath, sb.ToString(), Encoding.UTF8);
                }
            }
            catch
            {
                // Diagnostics must never break the main flow.
            }
        }
    }
}
