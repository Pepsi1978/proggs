using System;
using System.Diagnostics;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>Ergebnis einer Befehlsausfuehrung (kurz gehalten fuers Vorlesen).</summary>
    public sealed record ShellResult(bool Ok, int ExitCode, string Output);

    /// <summary>
    /// Fuehrt einen PowerShell-Befehl aus — die konkrete "Computer Use"-Aktion. Wird NUR aufgerufen,
    /// nachdem der <see cref="VoiceAgent.Core.CommandGuard"/> den Befehl freigegeben hat (oder Frank
    /// im sicheren Modus per Sprache bestaetigt hat). Selbst trifft der Executor KEINE
    /// Sicherheitsentscheidung — die liegt komplett im Guard.
    ///
    /// Sicher gegen Quoting-Bypaesse durch `-EncodedCommand` (Base64/UTF-16LE) statt String-Verkettung.
    /// Timeout + Output-Kappung, damit ein haengender/geschwaetziger Befehl die App nicht blockiert.
    /// </summary>
    public sealed class ShellExecutor
    {
        private const int MaxOutputChars = 1500;   // genug fuer Kontext/Vorlesen, ohne den Agenten zu fluten

        /// <summary>Fuehrt den Befehl aus und liefert ein kurzes Ergebnis. Wirft nie — Fehler stehen im Result.</summary>
        public async Task<ShellResult> RunAsync(string command, int timeoutMs = 20000, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(command))
                return new ShellResult(false, -1, "Leerer Befehl.");

            try
            {
                // EncodedCommand vermeidet jegliche Argument-Quoting-Fallen (Almanach: keine String-Verkettung an die Shell).
                var encoded = Convert.ToBase64String(Encoding.Unicode.GetBytes(command));
                var psi = new ProcessStartInfo
                {
                    FileName = "powershell.exe",
                    Arguments = $"-NoProfile -NonInteractive -ExecutionPolicy Bypass -EncodedCommand {encoded}",
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    StandardOutputEncoding = Encoding.UTF8,
                    StandardErrorEncoding = Encoding.UTF8
                };

                using var proc = new Process { StartInfo = psi };
                var stdout = new StringBuilder();
                var stderr = new StringBuilder();
                proc.OutputDataReceived += (_, e) => { if (e.Data != null) stdout.AppendLine(e.Data); };
                proc.ErrorDataReceived += (_, e) => { if (e.Data != null) stderr.AppendLine(e.Data); };

                Log.Info("ShellExecutor: fuehre Befehl aus", new { command });
                if (!proc.Start())
                    return new ShellResult(false, -1, "Konnte PowerShell nicht starten.");

                proc.BeginOutputReadLine();
                proc.BeginErrorReadLine();

                using var timeoutCts = CancellationTokenSource.CreateLinkedTokenSource(ct);
                timeoutCts.CancelAfter(timeoutMs);

                try
                {
                    await proc.WaitForExitAsync(timeoutCts.Token).ConfigureAwait(false);
                }
                catch (OperationCanceledException)
                {
                    try { if (!proc.HasExited) proc.Kill(entireProcessTree: true); } catch { /* best effort */ }
                    Log.Warn("ShellExecutor: Befehl abgebrochen (Timeout)", new { command, timeoutMs });
                    return new ShellResult(false, -1, $"Der Befehl hat zu lange gedauert (ueber {timeoutMs / 1000} Sekunden) und wurde abgebrochen.");
                }

                var combined = (stdout.ToString().Trim() + "\n" + stderr.ToString().Trim()).Trim();
                if (combined.Length > MaxOutputChars)
                    combined = combined.Substring(0, MaxOutputChars) + " … (gekuerzt)";

                bool ok = proc.ExitCode == 0;
                Log.Info("ShellExecutor: Befehl beendet", new { command, exit = proc.ExitCode, ok });
                return new ShellResult(ok, proc.ExitCode, combined.Length == 0 ? "(keine Ausgabe)" : combined);
            }
            catch (Exception ex)
            {
                Log.Error("ShellExecutor: Ausfuehrung fehlgeschlagen", ex, new { command });
                return new ShellResult(false, -1, "Der Befehl konnte nicht ausgefuehrt werden: " + ex.Message);
            }
        }
    }
}
