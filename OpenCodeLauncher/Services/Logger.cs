using System.IO;
using System.Text.Json;
using System.Diagnostics;

namespace OpenCodeLauncher.Services;

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
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    private Logger()
    {
        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "OpenCodeLauncher", "logs");
        Directory.CreateDirectory(dir);
        LogPath = Path.Combine(dir, $"launcher_{DateTime.Now:yyyyMMdd}.jsonl");
        // einmalige Bekanntgabe des Log-Pfads
        Console.Error.WriteLine($"Log: {LogPath}");
    }

    public void Info(string module, string fn, string msg, object? ctx = null) => Write("INFO", module, fn, msg, ctx);
    public void Warn(string module, string fn, string msg, object? ctx = null) => Write("WARN", module, fn, msg, ctx);
    public void Error(string module, string fn, string msg, object? ctx = null) => Write("ERROR", module, fn, msg, ctx);
    public void Debug(string module, string fn, string msg, object? ctx = null) => Write("DEBUG", module, fn, msg, ctx);

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
