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
/// <param name="Abgelaufen">The internal time limit ran out and the tree was killed.</param>
/// <param name="Abgebrochen">
/// The caller cancelled. Kept apart from Abgelaufen: a user's cancel is no exceeded time limit.
/// </param>
/// <param name="BeendenProblem">
/// Set when killing the tree after a time limit or cancel did not fully work -- the run then
/// must not look as if everything had been cleaned up.
/// </param>
public sealed record BefehlErgebnis(int ExitCode, string Ausgabe, bool Abgelaufen, bool PipeGehalten = false,
                                    bool Abgebrochen = false, string? BeendenProblem = null);

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
        var lesen = new Lesezustand();
        var ausgabe = MitlesenAsync(prozess.StandardOutput, puffer, lesen);
        var fehler = MitlesenAsync(prozess.StandardError, fehlerPuffer, lesen);

        using var zeitgeber = new CancellationTokenSource(zeitlimit);
        using var verbund = CancellationTokenSource.CreateLinkedTokenSource(zeitgeber.Token, abbruch);

        try
        {
            await prozess.WaitForExitAsync(verbund.Token);
        }
        catch (OperationCanceledException)
        {
            // The caller wins over the timer when both fired: a cancel is never reported as a
            // time limit.
            var vomAufrufer = abbruch.IsCancellationRequested;
            var problem = await BaumBeendenAsync(prozess);
            if (!await AuslaufenLassenAsync(ausgabe, fehler)) lesen.Aus = true;

            var text = Zusammenfuegen(puffer, fehlerPuffer)
                       + Environment.NewLine + "[UpdateZentrale] "
                       + (vomAufrufer ? "Abgebrochen" : "Zeitlimit überschritten")
                       + (problem is null ? " – Prozessbaum beendet." : " – " + problem);
            return new BefehlErgebnis(-1, Saeubern(text), Abgelaufen: !vomAufrufer, Abgebrochen: vomAufrufer,
                                      BeendenProblem: problem);
        }

        // The tool itself has exited; give the pipes a moment to drain, then stop waiting.
        var gehalten = !await AuslaufenLassenAsync(ausgabe, fehler);
        if (gehalten)
        {
            // The orphaned readers end when the holding child exits (measured: nothing piles up
            // across runs). Until then they must not keep growing a buffer nobody reads.
            lesen.Aus = true;
            lock (puffer)
            {
                puffer.AppendLine().Append("[UpdateZentrale] Ausgabe-Pipe wird noch von einem gestarteten "
                    + "Kindprozess gehalten – Lauf nach Prozessende abgeschlossen.");
            }
        }

        return new BefehlErgebnis(prozess.ExitCode, Saeubern(Zusammenfuegen(puffer, fehlerPuffer)), false, gehalten);
    }

    private static readonly TimeSpan Auslaufzeit = TimeSpan.FromSeconds(2);

    private sealed class Lesezustand
    {
        public volatile bool Aus;
    }

    /// <summary>
    /// Kills the whole tree and waits, bounded, for the root to be gone.
    /// </summary>
    /// <returns>null when done; otherwise what is known to be left over.</returns>
    private static async Task<string?> BaumBeendenAsync(Process prozess)
    {
        string? problem = null;
        try
        {
            prozess.Kill(entireProcessTree: true);
        }
        catch (InvalidOperationException)
        {
            // Exited between the cancel and the kill -- nothing left to do.
        }
        catch (AggregateException ex)
        {
            // Not every member of the tree could be terminated (.NET reports it this way).
            problem = "Nicht alle Kindprozesse ließen sich beenden: "
                      + string.Join("; ", ex.InnerExceptions.Select(i => i.Message));
        }
        catch (Exception ex)
        {
            problem = "Beenden fehlgeschlagen: " + ex.Message;
        }

        try
        {
            using var frist = new CancellationTokenSource(Beendefrist);
            await prozess.WaitForExitAsync(frist.Token);
        }
        catch (OperationCanceledException)
        {
            problem = (problem is null ? "" : problem + " ") + "Prozess " + ProzessId(prozess)
                      + " lief " + Beendefrist.TotalSeconds + " s nach dem Beenden noch.";
        }
        catch (InvalidOperationException)
        {
            // No process associated any more: it is gone.
        }
        return problem;
    }

    private static readonly TimeSpan Beendefrist = TimeSpan.FromSeconds(5);

    private static string ProzessId(Process prozess)
    {
        try { return prozess.Id.ToString(); }
        catch { return "?"; }
    }

    private static async Task MitlesenAsync(StreamReader leser, StringBuilder ziel, Lesezustand zustand)
    {
        var block = new char[4096];
        try
        {
            int gelesen;
            while ((gelesen = await leser.ReadAsync(block, 0, block.Length)) > 0)
            {
                if (zustand.Aus) break;
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
