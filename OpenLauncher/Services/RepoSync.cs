using System.Diagnostics;
using System.IO;

namespace OpenLauncher.Services;

/// <summary>
/// Gleicht vor jedem CLI-Start das Repo ~/proggs mit GitHub ab. Profile, Regeln und Skills kommen aus dem Repo;
/// ohne Abgleich startete eine Sitzung auf einem Rechner mit dessen veraltetem Stand.
/// Bewusst nur Fast-Forward (<c>git fetch</c> plus <c>git merge --ff-only</c>): nie ein echter Merge, nie ein Konflikt, und noch nicht
/// committete Aenderungen paralleler Sitzungen bleiben unangetastet. Geht das nicht (offline, lokale Commits
/// oder Aenderungen im Weg), wird trotzdem gestartet und der Grund gemeldet.
/// </summary>
public static class RepoSync
{
    public sealed record Result(bool Ok, string Message);

    private sealed record GitRun(int ExitCode, string Output, string Error, bool TimedOut);

    private static readonly TimeSpan PullTimeout = TimeSpan.FromSeconds(20);

    public static async Task<Result> PullAsync()
    {
        var log = Logger.Instance;
        var repo = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");
        if (!Directory.Exists(Path.Combine(repo, ".git")))
            return new Result(true, "kein Repo unter ~/proggs");

        try
        {
            var before = await RunGitAsync(repo, TimeSpan.FromSeconds(5), "rev-parse", "HEAD");
            var fetch = await RunGitAsync(repo, PullTimeout, "fetch", "--quiet");
            if (fetch.TimedOut)
            {
                log.Warn("RepoSync", "PullAsync", "git fetch nach 20 s abgebrochen");
                return new Result(false, "Zeitüberschreitung nach 20 s (offline?)");
            }
            if (fetch.ExitCode != 0)
            {
                var reason = FirstLine(fetch.Error);
                log.Warn("RepoSync", "PullAsync", $"git fetch fehlgeschlagen: {reason}");
                return new Result(false, reason);
            }

            // Der Fetch darf sterben, das Vorspulen nicht: git schreibt erst die Dateien und setzt HEAD zuletzt.
            // Ein Abbruch dazwischen hinterlaesst eine verwaiste index.lock und tausende Scheinaenderungen.
            // Nach der Frist startet die CLI, das Vorspulen laeuft im Hintergrund zu Ende.
            var pull = await RunGitAsync(repo, PullTimeout, killOnTimeout: false, "merge", "--ff-only", "--quiet", "@{u}");
            if (pull.TimedOut)
            {
                log.Warn("RepoSync", "PullAsync", "Vorspulen dauert länger als 20 s und läuft im Hintergrund weiter");
                return new Result(false, "Repo wird noch aktualisiert (großer Rückstand)");
            }
            if (pull.ExitCode != 0)
            {
                var reason = FirstLine(pull.Error);
                log.Warn("RepoSync", "PullAsync", $"git pull fehlgeschlagen: {reason}");
                return new Result(false, reason.Contains("fast-forward", StringComparison.OrdinalIgnoreCase)
                    ? "lokale Commits oder Änderungen verhindern den Abgleich"
                    : reason);
            }

            var after = await RunGitAsync(repo, TimeSpan.FromSeconds(5), "rev-parse", "HEAD");
            var message = before.Output == after.Output ? "Repo war aktuell" : "Repo aktualisiert";
            log.Info("RepoSync", "PullAsync", message, new { before = before.Output, after = after.Output });
            if (before.Output != after.Output) StarteVorwaermen();
            return new Result(true, message);
        }
        catch (Exception ex)
        {
            log.Error("RepoSync", "PullAsync", ex);
            return new Result(false, ex.Message);
        }
    }

    /// <summary>
    /// Committet genau eine Datei und pusht sie (vorher Rebase auf origin). Andere, noch nicht committete
    /// Aenderungen paralleler Sitzungen bleiben unangetastet (Commit nur mit Pfad, --autostash beim Rebase).
    /// </summary>
    public static async Task<Result> CommitAndPushFileAsync(string filePath, string message)
    {
        var log = Logger.Instance;
        var repo = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");
        try
        {
            var add = await RunGitAsync(repo, TimeSpan.FromSeconds(10), "add", "--", filePath);
            if (add.ExitCode != 0) return new Result(false, FirstLine(add.Error));
            var diff = await RunGitAsync(repo, TimeSpan.FromSeconds(10), "diff", "--cached", "--quiet", "--", filePath);
            if (diff.ExitCode == 0) return new Result(true, "keine Änderung");
            var commit = await RunGitAsync(repo, TimeSpan.FromSeconds(20), "commit", "--quiet", "-m", message, "--", filePath);
            if (commit.ExitCode != 0) return new Result(false, FirstLine(commit.Error));
            var pull = await RunGitAsync(repo, PullTimeout, killOnTimeout: false, "pull", "--rebase", "--autostash", "--quiet");
            if (pull.ExitCode != 0 || pull.TimedOut) return new Result(false, "committet, aber Abgleich fehlgeschlagen: " + FirstLine(pull.Error));
            var push = await RunGitAsync(repo, TimeSpan.FromSeconds(30), "push", "--quiet", "origin", "HEAD:main");
            if (push.ExitCode != 0 || push.TimedOut) return new Result(false, "committet, aber Push fehlgeschlagen: " + FirstLine(push.Error));
            log.Info("RepoSync", "CommitAndPushFileAsync", "Datei committet und gepusht", new { filePath });
            return new Result(true, "committet und gepusht");
        }
        catch (Exception ex)
        {
            log.Error("RepoSync", "CommitAndPushFileAsync", ex, new { filePath });
            return new Result(false, ex.Message);
        }
    }

    /// <summary>
    /// Neuer Stand vom Repo: die Aufgabe "Vorwaermen" (Werkzeuge/vorwaermen) sofort anstoßen, statt bis zu 10 Minuten
    /// auf ihren Takt zu warten. Sie baut die geänderten Apps vor; der Sitzungsbau wartet per Hook darauf.
    /// Über die Aufgabenplanung, damit kein Langläufer Handles des Launchers erbt.
    /// </summary>
    private static void StarteVorwaermen()
    {
        if (!OperatingSystem.IsWindows()) return;
        try
        {
            var psi = new ProcessStartInfo("schtasks") { UseShellExecute = false, CreateNoWindow = true };
            foreach (var a in new[] { "/run", "/tn", "Vorwaermen" }) psi.ArgumentList.Add(a);
            Process.Start(psi)?.Dispose();
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("RepoSync", "StarteVorwaermen", ex.Message);
        }
    }

    private static Task<GitRun> RunGitAsync(string repo, TimeSpan timeout, params string[] args)
        => RunGitAsync(repo, timeout, killOnTimeout: true, args);

    /// <param name="killOnTimeout">
    /// false fuer alles, was den Arbeitsbaum schreibt: Nach der Frist kehrt der Aufruf zurueck, git laeuft weiter.
    /// </param>
    private static async Task<GitRun> RunGitAsync(string repo, TimeSpan timeout, bool killOnTimeout, params string[] args)
    {
        var psi = new ProcessStartInfo("git")
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true
        };
        psi.ArgumentList.Add("-C");
        psi.ArgumentList.Add(repo);
        foreach (var arg in args) psi.ArgumentList.Add(arg);
        // Nie auf eine Anmeldung warten: der Launcher hat kein Terminal, git haenge sonst bis zum Timeout.
        psi.Environment["GIT_TERMINAL_PROMPT"] = "0";

        var process = Process.Start(psi) ?? throw new InvalidOperationException("git konnte nicht gestartet werden");
        var outTask = process.StandardOutput.ReadToEndAsync();
        var errTask = process.StandardError.ReadToEndAsync();
        using var cts = new CancellationTokenSource(timeout);
        try
        {
            await process.WaitForExitAsync(cts.Token);
        }
        catch (OperationCanceledException)
        {
            if (killOnTimeout)
            {
                try { process.Kill(entireProcessTree: true); } catch { }
                process.Dispose();
            }
            else
            {
                // Die Pipes bleiben offen und werden weiter geleert, sonst stirbt git beim naechsten Schreiben.
                _ = Task.WhenAll(outTask, errTask, process.WaitForExitAsync())
                    .ContinueWith(_ => process.Dispose(), TaskScheduler.Default);
            }
            return new GitRun(-1, "", "", true);
        }
        using (process)
            return new GitRun(process.ExitCode, (await outTask).Trim(), (await errTask).Trim(), false);
    }

    private static string FirstLine(string text) =>
        string.IsNullOrWhiteSpace(text) ? "unbekannter Fehler" : text.Split('\n')[0].Trim();
}
