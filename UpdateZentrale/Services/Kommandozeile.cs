using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

namespace UpdateZentrale.Services;

/// <param name="PipeGehalten">
/// The tool exited, but a program it started still holds the inherited output pipe (the repo
/// scripts launch the freshly built app that way). The run is complete; this only says why the
/// last lines may be missing.
/// </param>
public sealed record BefehlErgebnis(int ExitCode, string Ausgabe, bool Abgelaufen, bool PipeGehalten = false);

/// <summary>
/// Runs console tools and returns their whole output. winget and the PowerShell update scripts
/// paint progress with control characters -- those are stripped so the log stays readable.
/// </summary>
public static class Kommandozeile
{
    private static readonly Regex Steuerzeichen =
        new(@"\x1B\[[0-9;?]*[ -/]*[@-~]|[\x00-\x08\x0B\x0C\x0E-\x1F]", RegexOptions.Compiled);

    /// <param name="alsAufrufer">
    /// Nur für verwaltete Programmdateien setzen. Steht eine exe in Windows auf "Als
    /// Administrator ausführen" (AppCompat-Flag RUNASADMIN, genau der Schalter dieser App),
    /// scheitert ihr Start per CreateProcess aus einer nicht erhöhten Zentrale mit Win32-Fehler
    /// 740 — ohne Shell gibt es keinen UAC-Dialog, nur den Fehler. "RunAsInvoker" hebt die
    /// Anforderung für dieses eine Kind auf; läuft die Zentrale erhöht, erbt es deren Rechte.
    /// Bewusst NICHT der Standard: winget und die Update-Skripte starten Installer weiter, die
    /// ihre Elevation selbst anfordern dürfen müssen.
    /// </param>
    public static async Task<BefehlErgebnis> AusfuehrenAsync(
        string datei,
        string argumente,
        TimeSpan zeitlimit,
        string? arbeitsverzeichnis = null,
        CancellationToken abbruch = default,
        bool alsAufrufer = false)
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

        if (alsAufrufer) start.Environment["__COMPAT_LAYER"] = "RunAsInvoker";

        using var prozess = new Process { StartInfo = start, EnableRaisingEvents = true };
        var puffer = new StringBuilder();

        try
        {
            prozess.Start();
        }
        catch (System.ComponentModel.Win32Exception ex) when (ex.NativeErrorCode == 740)
        {
            return new BefehlErgebnis(-1,
                $"Start fehlgeschlagen: {Path.GetFileName(datei)} steht auf \"Als Administrator "
                + "ausführen\" und lässt sich deshalb nicht im Hintergrund starten. "
                + "Das Häkchen entfernen oder die Zentrale erhöht starten.", false);
        }
        catch (Exception ex)
        {
            return new BefehlErgebnis(-1, $"Start fehlgeschlagen: {ex.Message}", false);
        }

        // Read incrementally, never with ReadToEnd: a program the tool starts (update-launcher.ps1
        // and rebuild-overlay.ps1 start the freshly built app with UseShellExecute=$false) inherits
        // this pipe and keeps it open for as long as it runs. ReadToEnd then waits for an EOF that
        // only comes when the user closes that app -- the update run never ends.
        var fehlerPuffer = new StringBuilder();
        var ausgabe = MitlesenAsync(prozess.StandardOutput, puffer);
        var fehler = MitlesenAsync(prozess.StandardError, fehlerPuffer);

        using var zeitgeber = new CancellationTokenSource(zeitlimit);
        using var verbund = CancellationTokenSource.CreateLinkedTokenSource(zeitgeber.Token, abbruch);

        try
        {
            await prozess.WaitForExitAsync(verbund.Token);
        }
        catch (OperationCanceledException)
        {
            try { prozess.Kill(entireProcessTree: true); } catch { }
            await AuslaufenLassenAsync(ausgabe, fehler);
            return new BefehlErgebnis(-1, Saeubern(Zusammenfuegen(puffer, fehlerPuffer)), true);
        }

        // The tool itself has exited; give the pipes a moment to drain, then stop waiting.
        var gehalten = !await AuslaufenLassenAsync(ausgabe, fehler);
        if (gehalten)
        {
            lock (puffer)
            {
                puffer.AppendLine().Append("[UpdateZentrale] Ausgabe-Pipe wird noch von einem gestarteten "
                    + "Kindprozess gehalten – Lauf nach Prozessende abgeschlossen.");
            }
        }

        return new BefehlErgebnis(prozess.ExitCode, Saeubern(Zusammenfuegen(puffer, fehlerPuffer)), false, gehalten);
    }

    private static readonly TimeSpan Auslaufzeit = TimeSpan.FromSeconds(2);

    private static async Task MitlesenAsync(StreamReader leser, StringBuilder ziel)
    {
        var block = new char[4096];
        try
        {
            int gelesen;
            while ((gelesen = await leser.ReadAsync(block, 0, block.Length)) > 0)
            {
                lock (ziel) ziel.Append(block, 0, gelesen);
            }
        }
        catch
        {
            // A broken pipe only ends the reading, never the run.
        }
    }

    /// <returns>false if a pipe was still open after the grace period.</returns>
    private static async Task<bool> AuslaufenLassenAsync(Task ausgabe, Task fehler)
    {
        var beide = Task.WhenAll(ausgabe, fehler);
        return await Task.WhenAny(beide, Task.Delay(Auslaufzeit)) == beide;
    }

    private static string Zusammenfuegen(StringBuilder ausgabe, StringBuilder fehler)
    {
        string text, fehlertext;
        lock (ausgabe) text = ausgabe.ToString();
        lock (fehler) fehlertext = fehler.ToString();
        return string.IsNullOrWhiteSpace(fehlertext) ? text : text + Environment.NewLine + fehlertext;
    }

    private static string Saeubern(string text)
        // winget repaints its progress line with bare carriage returns even with
        // --disable-interactivity. Those must become line breaks, otherwise the table header ends
        // up glued behind spinner frames and the column parser never finds it.
        => Steuerzeichen.Replace(text ?? "", "")
            .Replace("\r\n", "\n")
            .Replace('\r', '\n')
            .Trim();
}
