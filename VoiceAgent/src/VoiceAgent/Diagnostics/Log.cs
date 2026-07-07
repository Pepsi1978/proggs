using System;
using System.IO;
using System.Runtime.CompilerServices;
using System.Text;
using System.Text.Encodings.Web;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace VoiceAgent.Diagnostics
{
    /// <summary>Schweregrad eines Log-Eintrags. Reihenfolge = Filter-Schwelle.</summary>
    public enum LogLevel
    {
        Debug = 0,
        Info = 1,
        Warn = 2,
        Error = 3,
        Fatal = 4
    }

    /// <summary>
    /// Zentrales strukturiertes Diagnose-Log (Observability-First-Standard).
    ///
    /// Jede Zeile ist ein eigenstaendiges JSON-Objekt (JSON Lines) mit den Feldern
    /// ts, level, module, fn, msg, ctx, trace — maschinen- und KI-lesbar, durchsuchbar
    /// per grep/jq, ideal um spaeter gezielt Fehler herauszuziehen.
    ///
    /// Eigenschaften:
    /// - Thread-safe (dedizierter Lock, nie lock(this)).
    /// - Schluckt eigene Fehler — Logging darf die App NIEMALS zum Absturz bringen.
    /// - module/fn werden automatisch via [CallerFilePath]/[CallerMemberName] befuellt
    ///   (Aufrufer muessen nichts uebergeben).
    /// - Level-Schwelle umschaltbar (Default INFO, per Env VOICEAGENT_LOG_LEVEL=DEBUG
    ///   oder zur Laufzeit ueber MinLevel).
    /// - Groessen-Rotation (voiceagent.log -> .1 -> .2), kein unbegrenztes Wachstum.
    /// - Spiegelung auf stdout/stderr -> ermoeglicht Live-Tailing (Terminal/Debugger).
    ///
    /// Datei: %LOCALAPPDATA%\VoiceAgent\logs\voiceagent.log
    /// Live mitlesen (PowerShell): Get-Content "$env:LOCALAPPDATA\VoiceAgent\logs\voiceagent.log" -Wait -Tail 20
    ///
    /// SICHERHEIT: Niemals Secrets/Tokens/PII roh in msg oder ctx geben — sensible Werte
    /// vor dem Loggen maskieren (das ist Aufgabe des Aufrufers).
    /// </summary>
    public static class Log
    {
        private const long MaxBytes = 5 * 1024 * 1024; // 5 MB pro Datei, dann Rotation
        private const int MaxBackups = 2;              // voiceagent.1.log, voiceagent.2.log

        private static readonly object _gate = new();
        private static readonly string _logPath;
        private static long _currentBytes;

        // Eine statische, eingefrorene Options-Instanz (CA1869: nie pro Aufruf neu).
        // UnsafeRelaxedJsonEscaping haelt Umlaute lesbar (ae/oe/ue statt \uXXXX).
        private static readonly JsonSerializerOptions _json = new()
        {
            WriteIndented = false,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
            Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
        };

        /// <summary>Aktuelle Filter-Schwelle. Eintraege unterhalb werden verworfen.</summary>
        public static LogLevel MinLevel { get; set; } = LogLevel.Info;

        /// <summary>
        /// Korrelations-ID des gerade laufenden Sprach-Turns (0 = kein Turn aktiv).
        /// Wird von TurnTrace gesetzt; jeder Log-Eintrag waehrend eines Turns traegt sie
        /// als "turn"-Feld, sodass man im Live-Tail einen kompletten Turn (gehoert ->
        /// verstanden -> Aktion -> gesprochen) als zusammenhaengende Kette filtern kann.
        /// </summary>
        public static long CurrentTurn { get; set; }

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

            try { _currentBytes = File.Exists(_logPath) ? new FileInfo(_logPath).Length : 0; }
            catch { _currentBytes = 0; }

            // Level-Schwelle aus Umgebungsvariable (z.B. VOICEAGENT_LOG_LEVEL=DEBUG).
            try
            {
                var env = Environment.GetEnvironmentVariable("VOICEAGENT_LOG_LEVEL");
                if (!string.IsNullOrWhiteSpace(env) &&
                    Enum.TryParse<LogLevel>(env.Trim(), ignoreCase: true, out var lvl))
                {
                    MinLevel = lvl;
                }
            }
            catch { /* Default INFO behalten */ }

            // Log-Pfad EINMAL beim Start ausgeben (Datei + Konsole), damit man immer
            // weiss, worauf man die Auswertung ansetzen kann.
            Info("VoiceAgent gestartet", new { logPath = _logPath, minLevel = MinLevel.ToString() });
            try { Console.WriteLine($"Log: {_logPath}"); } catch { /* keine Konsole angehaengt */ }
        }

        /// <summary>Voller Pfad der aktuellen Log-Datei (fuer die UI: "Log oeffnen").</summary>
        public static string LogFilePath => _logPath;

        public static void Debug(string message, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
            => Write(LogLevel.Debug, message, null, ctx, file, fn);

        public static void Info(string message, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
            => Write(LogLevel.Info, message, null, ctx, file, fn);

        public static void Warn(string message, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
            => Write(LogLevel.Warn, message, null, ctx, file, fn);

        public static void Error(string message, Exception? ex = null, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
            => Write(LogLevel.Error, message, ex, ctx, file, fn);

        public static void Fatal(string message, Exception? ex = null, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
            => Write(LogLevel.Fatal, message, ex, ctx, file, fn);

        private static void Write(LogLevel level, string message, Exception? ex, object? ctx,
            string file, string fn)
        {
            if (level < MinLevel) return; // unter der Schwelle: verwerfen

            try
            {
                long turn = CurrentTurn;
                var entry = new LogEntry
                {
                    Ts = DateTimeOffset.Now.ToString("yyyy-MM-ddTHH:mm:ss.fffzzz"),
                    Level = LevelName(level),
                    Turn = turn == 0 ? null : turn,
                    Module = ModuleOf(file),
                    Fn = fn,
                    Msg = message,
                    Ctx = ctx,
                    Trace = ex == null ? null : $"{ex.GetType().FullName}: {ex.Message}\n{ex.StackTrace}"
                };

                string line = JsonSerializer.Serialize(entry, _json);
                byte[] bytes = Encoding.UTF8.GetBytes(line + "\n");

                lock (_gate)
                {
                    RotateIfNeeded(bytes.Length);
                    using var fs = new FileStream(_logPath, FileMode.Append, FileAccess.Write, FileShare.Read);
                    fs.Write(bytes, 0, bytes.Length);
                    _currentBytes += bytes.Length;
                }

                // Spiegelung auf stdout/stderr (ermoeglicht Live-Tailing im Terminal).
                try
                {
                    var sink = level >= LogLevel.Warn ? Console.Error : Console.Out;
                    sink.WriteLine(line);
                }
                catch { /* keine Konsole angehaengt (WinExe) — egal */ }
            }
            catch
            {
                // Diagnostics must never break the main flow.
            }
        }

        // Vorab-gecachte Level-Namen: spart pro Log-Eintrag eine Enum.ToString()- UND eine
        // ToUpperInvariant()-Allokation. Reihenfolge = LogLevel-Werte (0..4), identische Strings
        // wie level.ToString().ToUpperInvariant() -> rein verhaltensneutral.
        private static readonly string[] _levelNames = { "DEBUG", "INFO", "WARN", "ERROR", "FATAL" };

        /// <summary>Gibt den Level-Namen aus dem Cache; faellt fuer unerwartete Werte sicher zurueck.</summary>
        private static string LevelName(LogLevel level)
        {
            int i = (int)level;
            return (uint)i < (uint)_levelNames.Length ? _levelNames[i] : level.ToString().ToUpperInvariant();
        }

        /// <summary>Dateiname ohne Pfad und Endung als Modul-Name (z.B. "AlwaysOnListener").</summary>
        private static string ModuleOf(string file)
        {
            if (string.IsNullOrEmpty(file)) return "?";
            try { return Path.GetFileNameWithoutExtension(file); }
            catch { return "?"; }
        }

        /// <summary>Rotiert die Log-Dateien, wenn die naechste Zeile das Groessen-Limit sprengt.</summary>
        private static void RotateIfNeeded(int incomingBytes)
        {
            if (_currentBytes + incomingBytes <= MaxBytes) return;

            try
            {
                // aeltestes Backup loeschen, restliche eins hochschieben, aktuelle -> .1
                string Backup(int n) => _logPath + "." + n;

                string oldest = Backup(MaxBackups);
                if (File.Exists(oldest)) File.Delete(oldest);

                for (int n = MaxBackups - 1; n >= 1; n--)
                {
                    string src = Backup(n);
                    if (File.Exists(src)) File.Move(src, Backup(n + 1), overwrite: true);
                }

                if (File.Exists(_logPath)) File.Move(_logPath, Backup(1), overwrite: true);
                _currentBytes = 0;
            }
            catch
            {
                // Rotation fehlgeschlagen -> weiterschreiben statt Logging zu verlieren.
                _currentBytes = 0;
            }
        }

        /// <summary>Eine Zeile im JSON-Lines-Log. Felder bewusst kurz (kompakte Zeilen).</summary>
        private sealed class LogEntry
        {
            [JsonPropertyName("ts")] public string Ts { get; set; } = "";
            [JsonPropertyName("level")] public string Level { get; set; } = "";
            [JsonPropertyName("turn")] public long? Turn { get; set; }
            [JsonPropertyName("module")] public string Module { get; set; } = "";
            [JsonPropertyName("fn")] public string Fn { get; set; } = "";
            [JsonPropertyName("msg")] public string Msg { get; set; } = "";
            [JsonPropertyName("ctx")] public object? Ctx { get; set; }
            [JsonPropertyName("trace")] public string? Trace { get; set; }
        }
    }
}
