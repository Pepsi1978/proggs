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
            return new Result(true, message);
        }
        catch (Exception ex)
        {
            log.Error("RepoSync", "PullAsync", ex);
            return new Result(false, ex.Message);
        }
    }

    /// <summary>
    /// Liest eine Datei so, wie sie auf GitHub (origin/main) liegt: nur <c>git fetch</c> plus <c>git show</c>,
    /// der Arbeitsbaum bleibt unberuehrt. Null, wenn offline oder die Datei dort fehlt.
    /// </summary>
    public static async Task<string?> ReadUpstreamFileAsync(string repoRelativePath)
    {
        var repo = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");
        try
        {
            if (!Directory.Exists(Path.Combine(repo, ".git"))) return null;
            var fetch = await RunGitAsync(repo, PullTimeout, "fetch", "--quiet");
            if (fetch.TimedOut || fetch.ExitCode != 0) return null;
            var show = await RunGitAsync(repo, TimeSpan.FromSeconds(10), "show", "@{u}:" + repoRelativePath.Replace('\\', '/'));
            return show.ExitCode == 0 && !show.TimedOut ? show.Output : null;
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("RepoSync", "ReadUpstreamFileAsync", ex.Message, new { repoRelativePath });
            return null;
        }
    }

    /// <summary>
    /// Pusht genau eine Datei als eigenen Commit direkt auf den GitHub-Stand (origin/main), ohne Rebase, ohne den
    /// lokalen Branch und ohne fremde Commits paralleler Sitzungen anzufassen. Danach wird die Datei vorgemerkt,
    /// damit das naechste Vorspulen (Start-Abgleich, Update-Zentrale) nicht an ihr scheitert. Wird origin/main
    /// zwischendurch weitergeschoben, neu aufsetzen (bis zu drei Versuche).
    /// </summary>
    public static async Task<Result> PushFileOnUpstreamAsync(string repoRelativePath, string message)
    {
        var repo = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");
        var rel = repoRelativePath.Replace('\\', '/');
        var index = Path.Combine(Path.GetTempPath(), "openlauncher-index-" + Guid.NewGuid().ToString("N"));
        try
        {
            for (var attempt = 1; attempt <= 3; attempt++)
            {
                var fetch = await RunGitAsync(repo, PullTimeout, "fetch", "--quiet");
                if (fetch.TimedOut || fetch.ExitCode != 0) return new Result(false, "Abgleich fehlgeschlagen: " + FirstLine(fetch.Error));
                var upstream = (await RunGitAsync(repo, TimeSpan.FromSeconds(5), "rev-parse", "@{u}")).Output.Trim();
                var blob = (await RunGitAsync(repo, TimeSpan.FromSeconds(10), "hash-object", "-w", "--path=" + rel, "--", rel)).Output.Trim();
                if (upstream.Length == 0 || blob.Length == 0) return new Result(false, "Git-Stand nicht lesbar");
                var env = new Dictionary<string, string> { ["GIT_INDEX_FILE"] = index };
                if (File.Exists(index)) File.Delete(index);
                await RunGitAsync(repo, TimeSpan.FromSeconds(20), true, env, "read-tree", upstream);
                await RunGitAsync(repo, TimeSpan.FromSeconds(10), true, env, "update-index", "--add", "--cacheinfo", $"100644,{blob},{rel}");
                var tree = (await RunGitAsync(repo, TimeSpan.FromSeconds(20), true, env, "write-tree")).Output.Trim();
                var baseTree = (await RunGitAsync(repo, TimeSpan.FromSeconds(5), "rev-parse", upstream + "^{tree}")).Output.Trim();
                if (tree.Length == 0) return new Result(false, "Baum nicht erstellbar");
                if (tree == baseTree)
                {
                    await RunGitAsync(repo, TimeSpan.FromSeconds(10), "add", "--", rel);
                    return new Result(true, "keine Änderung");
                }
                var commit = (await RunGitAsync(repo, TimeSpan.FromSeconds(10), "commit-tree", tree, "-p", upstream, "-m", message)).Output.Trim();
                if (commit.Length == 0) return new Result(false, "Commit nicht erstellbar");
                var push = await RunGitAsync(repo, TimeSpan.FromSeconds(30), "push", "--quiet", "origin", commit + ":refs/heads/main");
                if (push.ExitCode == 0 && !push.TimedOut)
                {
                    await RunGitAsync(repo, PullTimeout, "fetch", "--quiet");
                    await RunGitAsync(repo, TimeSpan.FromSeconds(10), "add", "--", rel);
                    Logger.Instance.Info("RepoSync", "PushFileOnUpstreamAsync", "Datei gepusht", new { rel, commit, attempt });
                    return new Result(true, "gepusht");
                }
                if (push.TimedOut) return new Result(false, "Push: Zeitüberschreitung");
            }
            return new Result(false, "Push dreimal abgewiesen (GitHub-Stand wechselte)");
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("RepoSync", "PushFileOnUpstreamAsync", ex.Message, new { rel });
            return new Result(false, ex.Message);
        }
        finally
        {
            try { if (File.Exists(index)) File.Delete(index); } catch { }
        }
    }

    /// <summary>Merkt eine Datei im Index vor (gleicher Inhalt wie GitHub: das Vorspulen scheitert dann nicht an ihr).</summary>
    public static async Task StageFileAsync(string repoRelativePath)
    {
        var repo = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");
        try { await RunGitAsync(repo, TimeSpan.FromSeconds(10), "add", "--", repoRelativePath.Replace('\\', '/')); }
        catch (Exception ex) { Logger.Instance.Warn("RepoSync", "StageFileAsync", ex.Message); }
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
            // Lief schon vorher ein Rebase (parallele Sitzung), gehoert er nicht uns: nie abbrechen.
            var rebaseVorher = Directory.Exists(Path.Combine(repo, ".git", "rebase-merge")) ||
                               Directory.Exists(Path.Combine(repo, ".git", "rebase-apply"));
            var pull = await RunGitAsync(repo, PullTimeout, killOnTimeout: false, "pull", "--rebase", "--autostash", "--quiet");
            if (pull.ExitCode != 0 || pull.TimedOut)
            {
                // Den eigenen Rebase nie halb stehen lassen: sonst blieben Konfliktmarker im Arbeitsbaum.
                if (!pull.TimedOut && !rebaseVorher) await RunGitAsync(repo, TimeSpan.FromSeconds(20), "rebase", "--abort");
                return new Result(false, "committet, aber Abgleich fehlgeschlagen: " + FirstLine(pull.Error));
            }
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

    private static Task<GitRun> RunGitAsync(string repo, TimeSpan timeout, params string[] args)
        => RunGitAsync(repo, timeout, killOnTimeout: true, args);

    /// <param name="killOnTimeout">
    /// false fuer alles, was den Arbeitsbaum schreibt: Nach der Frist kehrt der Aufruf zurueck, git laeuft weiter.
    /// </param>
    private static Task<GitRun> RunGitAsync(string repo, TimeSpan timeout, bool killOnTimeout, params string[] args)
        => RunGitAsync(repo, timeout, killOnTimeout, (Dictionary<string, string>?)null, args);

    private static async Task<GitRun> RunGitAsync(string repo, TimeSpan timeout, bool killOnTimeout, Dictionary<string, string>? environment, params string[] args)
    {
        var psi = new ProcessStartInfo("git")
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true,
            // git liefert UTF-8; ohne feste Kodierung zerlegte Windows Umlaute und Gedankenstriche in Modellnamen.
            StandardOutputEncoding = System.Text.Encoding.UTF8,
            StandardErrorEncoding = System.Text.Encoding.UTF8
        };
        if (environment != null) foreach (var pair in environment) psi.Environment[pair.Key] = pair.Value;
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
