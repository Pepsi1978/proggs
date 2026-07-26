using System.IO;
using System.Text.Json;
using System.Diagnostics;
using System.Text;
using System.Text.Json.Serialization.Metadata;

namespace OpenLauncher.Services;

/// <summary>
/// Strukturiertes JSON-Lines-Logging (Observability-Standard).
/// Eintrag pro Zeile: ts, level, module, fn, msg, ctx.
/// Log-Pfad wird beim Start EINMAL ausgegeben.
/// </summary>
public sealed class Logger
{
    private static Logger? _instance;
    public static Logger Instance => _instance ??= new Logger();

    public string LogPath { get; }
    private readonly object _lock = new();
    private readonly JsonSerializerOptions _jsonOpts = new()
    {
        WriteIndented = false,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver()
    };

    private Logger()
    {
        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "OpenLauncher", "logs");
        Directory.CreateDirectory(dir);
        LogPath = Path.Combine(dir, $"launcher_{DateTime.Now:yyyyMMdd}.jsonl");
        // einmalige Bekanntgabe des Log-Pfads
        Console.Error.WriteLine($"Log: {LogPath}");
    }

    public void Info(string module, string fn, string msg, object? ctx = null) => Write("INFO", module, fn, msg, ctx);
    public void Warn(string module, string fn, string msg, object? ctx = null) => Write("WARN", module, fn, msg, ctx);
    public void Error(string module, string fn, string msg, object? ctx = null) => Write("ERROR", module, fn, msg, ctx);
    public void Error(string module, string fn, Exception ex, object? ctx = null) => Write("ERROR", module, fn, ex.Message, new { ctx, error = ErrorObject(ex) });
    public void Debug(string module, string fn, string msg, object? ctx = null) => Write("DEBUG", module, fn, msg, ctx);

    public string WriteErrorReport(string area, string details)
    {
        var dir = Path.Combine(Path.GetDirectoryName(LogPath)!, "errors");
        Directory.CreateDirectory(dir);
        var safeArea = string.Concat(area.Select(ch => char.IsLetterOrDigit(ch) ? ch : '_'));
        var path = Path.Combine(dir, $"{DateTime.Now:yyyyMMdd_HHmmss_fff}_{safeArea}.txt");
        File.WriteAllText(path, details, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        return path;
    }

    private static object ErrorObject(Exception ex) => new
    {
        type = ex.GetType().FullName,
        ex.Message,
        ex.StackTrace,
        inner = ex.InnerException == null ? null : ErrorObject(ex.InnerException)
    };

    private void Write(string level, string module, string fn, string msg, object? ctx)
    {
        try
        {
            var entry = new
            {
                ts = DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss.fff"),
                level,
                module,
                fn,
                msg,
                ctx
            };
            var line = JsonSerializer.Serialize(entry, _jsonOpts);
            lock (_lock)
            {
                File.AppendAllText(LogPath, line + Environment.NewLine);
            }
        }
        catch
        {
            // Logging darf die App nie killen.
        }
    }
}
