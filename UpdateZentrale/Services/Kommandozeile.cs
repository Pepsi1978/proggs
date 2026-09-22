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
    public static Task<BefehlErgebnis> AusfuehrenAsync(
        string datei,
        string argumente,
        TimeSpan zeitlimit,
        string? arbeitsverzeichnis = null,
        CancellationToken abbruch = default,
        bool alsAufrufer = false)
        => AusfuehrenInternAsync(datei, argumente, zeitlimit, arbeitsverzeichnis, abbruch, alsAufrufer);

    /// <param name="baumBeenden">Test seam: how the tree is killed (default Kill(entireProcessTree)).</param>
    /// <param name="einzelnBeenden">Test seam: the follow-up kill of one captured survivor.</param>
    internal static async Task<BefehlErgebnis> AusfuehrenInternAsync(
        string datei,
        string argumente,
        TimeSpan zeitlimit,
        string? arbeitsverzeichnis = null,
        CancellationToken abbruch = default,
        bool alsAufrufer = false,
        Action<Process>? baumBeenden = null,
        Func<Process, bool>? einzelnBeenden = null)
    {
        // Every external command of the app passes here, so this is where it is recorded -- with
        // the ambient operation (see Diagnose), whichever provider or feature started it.
        var befehlId = Guid.NewGuid().ToString("N")[..12];
        var uhr = Stopwatch.StartNew();
        Diagnose.Ereignis(Schwere.Info, "kommando", "befehl.beginn", Path.GetFileName(datei) + " gestartet", "befehl",
            new Dictionary<string, object?>
            {
                ["befehl"] = befehlId,
                ["datei"] = Path.GetFileName(datei),
                ["argumente"] = Bereinigung.Sicher(argumente, 1000),
                ["verzeichnis"] = arbeitsverzeichnis,
                ["zeitlimitSek"] = (long)zeitlimit.TotalSeconds,
                ["alsAufrufer"] = alsAufrufer
            });
        try
        {
            var lauf = await AusfuehrenKernAsync(datei, argumente, zeitlimit, arbeitsverzeichnis, abbruch, alsAufrufer,
                baumBeenden, einzelnBeenden);
            BefehlEnde(befehlId, datei, lauf, uhr);
            return lauf;
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "kommando", "Befehl " + Path.GetFileName(datei));
            throw;
        }
    }

    /// <summary>End event: every flag the run has, plus a masked, bounded excerpt of the output.</summary>
    private static void BefehlEnde(string befehlId, string datei, BefehlErgebnis lauf, Stopwatch uhr)
    {
        var schwere = lauf.Abgelaufen || lauf.BeendenProblem is not null ? Schwere.Fehler
            : lauf.Abgebrochen || lauf.ExitCode != 0 ? Schwere.Warnung
            : Schwere.Info;
        var ergebnis = lauf.Abgebrochen ? "abgebrochen" : lauf.Abgelaufen ? "zeitlimit" : "exit " + lauf.ExitCode;
        Diagnose.Schreiben(schwere, "kommando", "befehl.ende", Path.GetFileName(datei) + " beendet (" + ergebnis + ")", "befehl",
            Diagnose.AktuellerVorgang, (long)uhr.Elapsed.TotalMilliseconds, ergebnis,
            new Dictionary<string, object?>
            {
                ["befehl"] = befehlId,
                ["datei"] = Path.GetFileName(datei),
                ["exitCode"] = lauf.ExitCode,
                ["abgelaufen"] = lauf.Abgelaufen,
                ["abgebrochen"] = lauf.Abgebrochen,
                ["pipeGehalten"] = lauf.PipeGehalten,
                ["beendenProblem"] = lauf.BeendenProblem,
                ["ausgabeLaenge"] = lauf.Ausgabe.Length,
                ["ausgabe"] = Bereinigung.Sicher(lauf.Ausgabe, 4000)
            });
    }

    private static async Task<BefehlErgebnis> AusfuehrenKernAsync(
        string datei,
        string argumente,
        TimeSpan zeitlimit,
        string? arbeitsverzeichnis,
        CancellationToken abbruch,
        bool alsAufrufer,
        Action<Process>? baumBeenden,
        Func<Process, bool>? einzelnBeenden)
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
            var problem = await BaumBeendenAsync(prozess,
                baumBeenden ?? (p => p.Kill(entireProcessTree: true)),
                einzelnBeenden ?? (p => { p.Kill(); return true; }));
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
    /// Kills the whole tree and verifies it. The descendants are captured first (handles held, so
    /// no PID reuse can fool the check); then the tree kill; then a bounded wait for the root AND
    /// exactly those captured children. A survivor is killed once more on its own -- never by
    /// name -- and whatever is still alive after that is reported, not hidden.
    /// </summary>
    /// <returns>null when everything is gone; otherwise what is known to be left over.</returns>
    private static async Task<string?> BaumBeendenAsync(Process prozess, Action<Process> baumBeenden,
                                                        Func<Process, bool> einzelnBeenden)
    {
        var kinder = Prozessbaum.Erfassen(prozess);
        try
        {
            var probleme = new List<string>();
            try
            {
                baumBeenden(prozess);
            }
            catch (InvalidOperationException)
            {
                // Exited between the cancel and the kill -- its children are checked below anyway.
            }
            catch (AggregateException ex)
            {
                // Not every member of the tree could be terminated (.NET reports it this way).
                probleme.Add("Nicht alle Kindprozesse ließen sich beenden: "
                             + string.Join("; ", ex.InnerExceptions.Select(i => i.Message)));
            }
            catch (Exception ex)
            {
                probleme.Add("Beenden fehlgeschlagen: " + ex.Message);
            }

            var frist = DateTime.UtcNow + Beendefrist;
            if (!await BeendetBisAsync(prozess, frist))
                probleme.Add("Prozess " + ProzessId(prozess) + " lief " + Beendefrist.TotalSeconds + " s nach dem Beenden noch.");

            var ueberlebende = new List<Process>();
            foreach (var kind in kinder)
                if (!await BeendetBisAsync(kind, frist)) ueberlebende.Add(kind);

            foreach (var kind in ueberlebende)
            {
                try { einzelnBeenden(kind); }
                catch (InvalidOperationException) { }   // gone meanwhile
                catch (Exception ex) { probleme.Add("Kindprozess " + Beschreiben(kind) + ": " + ex.Message); }
            }

            var nachfrist = DateTime.UtcNow + Nachfrist;
            var bleiben = new List<string>();
            foreach (var kind in ueberlebende)
                if (!await BeendetBisAsync(kind, nachfrist)) bleiben.Add(Beschreiben(kind));
            if (bleiben.Count > 0)
                probleme.Add("Kindprozess(e) liefen nach dem Beenden weiter: " + string.Join(", ", bleiben) + ".");

            return probleme.Count == 0 ? null : string.Join(" ", probleme);
        }
        finally
        {
            foreach (var kind in kinder) kind.Dispose();
        }
    }

    private static async Task<bool> BeendetBisAsync(Process prozess, DateTime frist)
    {
        var rest = frist - DateTime.UtcNow;
        if (rest < TimeSpan.Zero) rest = TimeSpan.Zero;
        try
        {
            using var zeit = new CancellationTokenSource(rest);
            await prozess.WaitForExitAsync(zeit.Token);
            return true;
        }
        catch (OperationCanceledException)
        {
            return prozess.HasExited;
        }
        catch (InvalidOperationException)
        {
            return true;   // no process associated any more: gone
        }
    }

    private static string Beschreiben(Process prozess)
    {
        try { return prozess.ProcessName + " (PID " + prozess.Id + ")"; }
        catch { return "PID " + ProzessId(prozess); }
    }

    private static readonly TimeSpan Beendefrist = TimeSpan.FromSeconds(5);
    private static readonly TimeSpan Nachfrist = TimeSpan.FromSeconds(2);

    private static string ProzessId(Process prozess)
    {
        try { return prozess.Id.ToString(); }
        catch { return "?"; }
    }

    private static int _aktiveLeser;

    /// <summary>Readers still attached to some pipe -- for tests: must return to its baseline.</summary>
    internal static int AktiveLeser => Volatile.Read(ref _aktiveLeser);

    /// <summary>
    /// A run that did not end cleanly, as a card state -- evaluated BEFORE any status line or
    /// exit code, so a line printed before a timeout ("started") can never turn it into success.
    /// </summary>
    /// <returns>null when the tool ended by itself.</returns>
    public static Models.PruefErgebnis? UnsauberesEnde(BefehlErgebnis lauf)
    {
        if (!lauf.Abgelaufen && !lauf.Abgebrochen && lauf.BeendenProblem is null) return null;

        var was = lauf.Abgebrochen ? "Abgebrochen" : "Zeitlimit überschritten";
        if (lauf.BeendenProblem is not null)
            return new Models.PruefErgebnis(Models.UpdateZustand.Fehler,
                Meldung: was + " – der Prozessbaum ließ sich nicht vollständig beenden: " + lauf.BeendenProblem,
                Protokoll: lauf.Ausgabe);

        return lauf.Abgebrochen
            ? new Models.PruefErgebnis(Models.UpdateZustand.Abgebrochen,
                Meldung: "Abgebrochen – der Prozessbaum wurde beendet.", Protokoll: lauf.Ausgabe)
            : new Models.PruefErgebnis(Models.UpdateZustand.Fehler,
                Meldung: "Zeitlimit überschritten – der Prozessbaum wurde beendet.", Protokoll: lauf.Ausgabe);
    }

    private static async Task MitlesenAsync(StreamReader leser, StringBuilder ziel, Lesezustand zustand)
    {
        var block = new char[4096];
        Interlocked.Increment(ref _aktiveLeser);
        try
        {
            int gelesen;
            while ((gelesen = await leser.ReadAsync(block, 0, block.Length)) > 0)
            {
                if (zustand.Aus) break;
                lock (ziel) ziel.Append(block, 0, gelesen);
            }
        }
        catch (Exception ex)
        {
            // A broken pipe only ends the reading, never the run -- but it is noted.
            Diagnose.Ausnahme(ex, "kommando", "Lesen der Prozessausgabe", Schwere.Debug);
        }
        finally
        {
            Interlocked.Decrement(ref _aktiveLeser);
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
