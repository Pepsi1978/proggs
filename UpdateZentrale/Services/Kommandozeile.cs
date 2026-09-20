using System.Diagnostics;
using System.Text;
using System.Text.RegularExpressions;

namespace UpdateZentrale.Services;

public sealed record BefehlErgebnis(int ExitCode, string Ausgabe, bool Abgelaufen);

/// <summary>
/// Runs console tools and returns their whole output. winget and the PowerShell update scripts
/// paint progress with control characters -- those are stripped so the log stays readable.
/// </summary>
public static class Kommandozeile
{
    private static readonly Regex Steuerzeichen =
        new(@"\x1B\[[0-9;?]*[ -/]*[@-~]|[\x00-\x08\x0B\x0C\x0E-\x1F]", RegexOptions.Compiled);

    public static async Task<BefehlErgebnis> AusfuehrenAsync(
        string datei,
        string argumente,
        TimeSpan zeitlimit,
        string? arbeitsverzeichnis = null,
        CancellationToken abbruch = default)
    {
        var start = new ProcessStartInfo
        {
            FileName = datei,
            Arguments = argumente,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true,
            StandardOutputEncoding = Encoding.UTF8,
            StandardErrorEncoding = Encoding.UTF8,
            WorkingDirectory = arbeitsverzeichnis ?? ""
        };

        using var prozess = new Process { StartInfo = start, EnableRaisingEvents = true };
        var puffer = new StringBuilder();

        try
        {
            prozess.Start();
        }
        catch (Exception ex)
        {
            return new BefehlErgebnis(-1, $"Start fehlgeschlagen: {ex.Message}", false);
        }

        var ausgabe = prozess.StandardOutput.ReadToEndAsync(abbruch);
        var fehler = prozess.StandardError.ReadToEndAsync(abbruch);

        using var zeitgeber = new CancellationTokenSource(zeitlimit);
        using var verbund = CancellationTokenSource.CreateLinkedTokenSource(zeitgeber.Token, abbruch);

        try
        {
            await prozess.WaitForExitAsync(verbund.Token);
        }
        catch (OperationCanceledException)
        {
            try { prozess.Kill(entireProcessTree: true); } catch { }
            return new BefehlErgebnis(-1, Saeubern(puffer.Append(await SicherAsync(ausgabe)).ToString()), true);
        }

        puffer.Append(await SicherAsync(ausgabe));
        var fehlertext = await SicherAsync(fehler);
        if (!string.IsNullOrWhiteSpace(fehlertext)) puffer.AppendLine().Append(fehlertext);

        return new BefehlErgebnis(prozess.ExitCode, Saeubern(puffer.ToString()), false);
    }

    private static async Task<string> SicherAsync(Task<string> aufgabe)
    {
        try { return await aufgabe; } catch { return ""; }
    }

    private static string Saeubern(string text)
        => Steuerzeichen.Replace(text ?? "", "").Replace("\r\n", "\n").Trim();
}
