using System.Diagnostics;
using System.IO;

namespace OpenLauncher.Services;

/// <summary>
/// Gleicht vor jedem CLI-Start das Repo ~/proggs mit GitHub ab. Profile, Regeln und Skills kommen aus dem Repo;
/// ohne Abgleich startete eine Sitzung auf einem Rechner mit dessen veraltetem Stand.
/// Bewusst nur Fast-Forward (<c>git pull --ff-only</c>): nie ein Merge, nie ein Konflikt, und noch nicht
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
            var pull = await RunGitAsync(repo, PullTimeout, "pull", "--ff-only", "--no-rebase", "--quiet");
            if (pull.TimedOut)
            {
                log.Warn("RepoSync", "PullAsync", "git pull nach 20 s abgebrochen");
                return new Result(false, "Zeitüberschreitung nach 20 s (offline?)");
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
            return new Result(true, message);
        }
        catch (Exception ex)
        {
            log.Error("RepoSync", "PullAsync", ex);
            return new Result(false, ex.Message);
        }
    }

    private static async Task<GitRun> RunGitAsync(string repo, TimeSpan timeout, params string[] args)
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

        using var process = Process.Start(psi) ?? throw new InvalidOperationException("git konnte nicht gestartet werden");
        var outTask = process.StandardOutput.ReadToEndAsync();
        var errTask = process.StandardError.ReadToEndAsync();
        using var cts = new CancellationTokenSource(timeout);
        try
        {
            await process.WaitForExitAsync(cts.Token);
        }
        catch (OperationCanceledException)
        {
            try { process.Kill(entireProcessTree: true); } catch { }
            return new GitRun(-1, "", "", true);
        }
        return new GitRun(process.ExitCode, (await outTask).Trim(), (await errTask).Trim(), false);
    }

    private static string FirstLine(string text) =>
        string.IsNullOrWhiteSpace(text) ? "unbekannter Fehler" : text.Split('\n')[0].Trim();
}
