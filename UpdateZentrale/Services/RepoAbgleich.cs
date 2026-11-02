using System.IO;

namespace UpdateZentrale.Services;

/// <param name="GeholtOk">git fetch answered: origin/main is fresh.</param>
/// <param name="GezogenOk">The checkout is at origin's state (fast-forwarded or already there).</param>
public sealed record AbgleichErgebnis(bool GeholtOk, bool GezogenOk, string Meldung, DateTime Zeit);

/// <summary>
/// Brings ~/proggs to the state of origin before the own tools are checked: fetch, then
/// fast-forward only -- never a merge, never a rebase, never a stash. The own tools are built
/// from this checkout, so a check against an old checkout can only ever say "there are commits",
/// not which version is waiting.
///
/// One run serves every caller: four cards checking at once share a single fetch instead of
/// each starting their own, and a result stays valid for two minutes.
/// </summary>
public static class RepoAbgleich
{
    private static readonly TimeSpan Gueltig = TimeSpan.FromMinutes(2);
    private static readonly object Sperre = new();
    private static readonly Dictionary<string, Task<AbgleichErgebnis>> Laeufe = new(StringComparer.OrdinalIgnoreCase);

    public static Task<AbgleichErgebnis> AbgleichenAsync(string repoWurzel, bool erzwingen = false)
    {
        lock (Sperre)
        {
            if (Laeufe.TryGetValue(repoWurzel, out var lauf))
            {
                var nochGut = !lauf.IsCompleted
                              || (!erzwingen && lauf.Status == TaskStatus.RanToCompletion && lauf.Result.GeholtOk
                                  && DateTime.Now - lauf.Result.Zeit < Gueltig);
                if (nochGut) return lauf;
            }

            lauf = AusfuehrenAsync(repoWurzel);
            Laeufe[repoWurzel] = lauf;
            return lauf;
        }
    }

    private static async Task<AbgleichErgebnis> AusfuehrenAsync(string repoWurzel)
    {
        try
        {
            var holen = await Kommandozeile.AusfuehrenAsync("git", "fetch --quiet", TimeSpan.FromMinutes(2), repoWurzel);
            if (holen.Abgelaufen || holen.ExitCode != 0)
                return Melden(new AbgleichErgebnis(false, false,
                    "git fetch fehlgeschlagen (" + (holen.Abgelaufen ? "Zeitlimit" : "Code " + holen.ExitCode) + "): "
                    + Kurz(holen.Ausgabe), DateTime.Now));

            // Fast-forward only: with local changes in the way or a diverged history git refuses
            // and changes nothing. The cards then still count the commits that are waiting.
            var ziehen = await VorspulenAsync(repoWurzel, autostash: false);
            if (ziehen.Abgelaufen || ziehen.ExitCode != 0)
                return Melden(new AbgleichErgebnis(true, false,
                    "Repo ließ sich nicht vorspulen (" + VorspulGrund(ziehen.Ausgabe) + "): "
                    + Kurz(ziehen.Ausgabe), DateTime.Now));

            return Melden(new AbgleichErgebnis(true, true, "Repo ist auf dem Stand von origin.", DateTime.Now));
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "repo", "Repo-Abgleich", Schwere.Warnung);
            return new AbgleichErgebnis(false, false, "Repo-Abgleich fehlgeschlagen: " + ex.Message, DateTime.Now);
        }
    }

    /// <summary>
    /// A large catch-up checks out thousands of files and fetches LFS blobs. A short limit would
    /// kill exactly that run and leave the half-finished state the steps below have to repair.
    /// </summary>
    internal static readonly TimeSpan VorspulZeitlimit = TimeSpan.FromMinutes(15);

    /// <summary>A lock younger than this may belong to a git that is only just starting.</summary>
    internal static readonly TimeSpan SperreMindestalter = TimeSpan.FromMinutes(2);

    /// <summary>
    /// Fast-forwards the checkout to its upstream and repairs what an earlier, killed run left
    /// behind: a stale index.lock and a checkout that reached the new state while HEAD did not.
    /// </summary>
    /// <param name="autostash">
    /// Moves uncommitted changes aside for the fast-forward. Only for an update the user started;
    /// the check before it never stashes.
    /// </param>
    /// <param name="gitLaeuft">Test seam: is a git process alive on this machine?</param>
    internal static async Task<BefehlErgebnis> VorspulenAsync(string repoWurzel, bool autostash,
        IProgress<string>? protokoll = null, CancellationToken abbruch = default, Func<bool>? gitLaeuft = null)
    {
        await SperreFreimachenAsync(repoWurzel, protokoll, abbruch, gitLaeuft ?? GitProzessLaeuft);

        var zug = await Kommandozeile.AusfuehrenAsync("git",
            "merge --ff-only --quiet " + (autostash ? "--autostash " : "") + "@{u}",
            VorspulZeitlimit, repoWurzel, abbruch);
        if (zug.ExitCode == 0 || zug.Abgelaufen || zug.Abgebrochen) return zug;

        return await HalbesVorspulenVollendenAsync(repoWurzel, protokoll, abbruch)
            ? new BefehlErgebnis(0, "Abgebrochenes Vorspulen vollendet.", false)
            : zug;
    }

    /// <summary>The real reason for a refused fast-forward, not a guess at the usual one.</summary>
    internal static string VorspulGrund(string ausgabe)
        => (ausgabe ?? "").Contains("index.lock", StringComparison.OrdinalIgnoreCase)
            ? "Git-Sperre .git/index.lock: ein anderer Git-Prozess läuft, oder ein abgebrochener hat sie hinterlassen"
            : "lokale Änderungen oder abweichender Verlauf";

    /// <summary>Git keeps its process alive as long as it holds the lock, also during a commit hook.</summary>
    internal static bool SperreVerwaist(DateTime sperreZeitUtc, DateTime jetztUtc, bool gitLaeuft)
        => !gitLaeuft && jetztUtc - sperreZeitUtc >= SperreMindestalter;

    private static bool GitProzessLaeuft()
    {
        foreach (var name in new[] { "git", "git-lfs" })
        {
            var prozesse = System.Diagnostics.Process.GetProcessesByName(name);
            var gefunden = prozesse.Length > 0;
            foreach (var p in prozesse) p.Dispose();
            if (gefunden) return true;
        }
        return false;
    }

    /// <summary>
    /// A running git gets a minute to finish. An orphaned lock is removed, because every later git
    /// call in this repo would otherwise fail with it. If neither applies, the caller's git call
    /// reports the lock.
    /// </summary>
    private static async Task SperreFreimachenAsync(string repoWurzel, IProgress<string>? protokoll,
        CancellationToken abbruch, Func<bool> gitLaeuft)
    {
        var sperre = Path.Combine(repoWurzel, ".git", "index.lock");
        var bis = DateTime.UtcNow.AddSeconds(60);
        while (File.Exists(sperre))
        {
            try
            {
                if (SperreVerwaist(File.GetLastWriteTimeUtc(sperre), DateTime.UtcNow, gitLaeuft()))
                {
                    File.Delete(sperre);
                    Berichten(protokoll, Schwere.Warnung, "repo.sperre", "Verwaiste Git-Sperre .git/index.lock entfernt.");
                    return;
                }
                if (DateTime.UtcNow >= bis) return;
                await Task.Delay(TimeSpan.FromSeconds(3), abbruch);
            }
            catch (OperationCanceledException)
            {
                return;
            }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException)
            {
                Diagnose.Gefangen(ex, "repo", Schwere.Warnung);
                return;
            }
        }
    }

    /// <summary>
    /// The working tree holds the upstream state, HEAD and the index still the old one. Git writes
    /// the files first and moves HEAD last, so a run killed in between leaves exactly that, and git
    /// then refuses every further fast-forward because of the "local changes" it left itself.
    /// </summary>
    internal static bool IstHalbesVorspulen(int abweichungZuHead, int abweichungZuUpstream)
        => abweichungZuHead >= 50 && abweichungZuUpstream * 10 <= abweichungZuHead;

    /// <summary>
    /// Finishes such a run with reset --mixed: it moves HEAD and the index and leaves every file in
    /// the working tree alone, so uncommitted changes survive. Files git had not written yet are
    /// the only ones taken from the upstream.
    /// </summary>
    private static async Task<bool> HalbesVorspulenVollendenAsync(string repoWurzel, IProgress<string>? protokoll,
                                                                  CancellationToken abbruch)
    {
        var limit = TimeSpan.FromMinutes(5);
        var vorfahr = await Kommandozeile.AusfuehrenAsync("git", "merge-base --is-ancestor HEAD @{u}", limit, repoWurzel, abbruch);
        if (vorfahr.ExitCode != 0) return false;

        var zuHead = await Kommandozeile.AusfuehrenAsync("git", "status --porcelain --no-renames -uno", limit, repoWurzel, abbruch);
        if (zuHead.ExitCode != 0) return false;

        // A separate index compares the working tree with the upstream without touching the real one.
        var tempIndex = Path.Combine(Path.GetTempPath(), "updatezentrale-index-" + Guid.NewGuid().ToString("N"));
        var umgebung = new Dictionary<string, string> { ["GIT_INDEX_FILE"] = tempIndex };
        BefehlErgebnis zuUpstream;
        try
        {
            var lesen = await Kommandozeile.AusfuehrenAsync("git", "read-tree @{u}", limit, repoWurzel, abbruch, zusatzUmgebung: umgebung);
            if (lesen.ExitCode != 0) return false;
            zuUpstream = await Kommandozeile.AusfuehrenAsync("git", "-c core.quotePath=false status --porcelain --no-renames -uno",
                limit, repoWurzel, abbruch, zusatzUmgebung: umgebung);
            if (zuUpstream.ExitCode != 0) return false;
        }
        finally
        {
            foreach (var datei in new[] { tempIndex, tempIndex + ".lock" })
                try { File.Delete(datei); }
                catch (Exception ex) when (ex is IOException or UnauthorizedAccessException) { Diagnose.Gefangen(ex, "repo", Schwere.Debug); }
        }

        // Second column: working tree against the upstream. "AD": new upstream file not on disk yet.
        var abweichend = Zeilen(zuUpstream.Ausgabe).Where(z => z.Length > 3 && z[1] != ' ').ToList();
        if (!IstHalbesVorspulen(Zeilen(zuHead.Ausgabe).Count, abweichend.Count)) return false;

        var zurueck = await Kommandozeile.AusfuehrenAsync("git", "reset -q --mixed @{u}", VorspulZeitlimit, repoWurzel, abbruch);
        if (zurueck.ExitCode != 0) return false;
        Berichten(protokoll, Schwere.Warnung, "repo.halbes-vorspulen",
            "Abgebrochenes Vorspulen vollendet: HEAD und Index auf den Stand von origin gesetzt, Arbeitsbaum unverändert.");

        var fehlend = abweichend.Where(z => z.StartsWith("AD ", StringComparison.Ordinal))
                                .Select(z => "\"" + z[3..].Trim().Trim('"') + "\"").ToList();
        // One call per file: LFS blobs may take long, and a failure only concerns that one file.
        foreach (var pfad in fehlend)
        {
            var holen = await Kommandozeile.AusfuehrenAsync("git", "checkout @{u} -- " + pfad, VorspulZeitlimit, repoWurzel, abbruch);
            if (holen.ExitCode != 0)
                Berichten(protokoll, Schwere.Warnung, "repo.halbes-vorspulen", "Datei fehlt weiter: " + pfad + " – " + Kurz(holen.Ausgabe));
        }
        return true;
    }

    private static List<string> Zeilen(string text)
        => (text ?? "").Split('\n').Select(z => z.TrimEnd('\r')).Where(z => z.Length > 0).ToList();

    private static void Berichten(IProgress<string>? protokoll, Schwere schwere, string typ, string meldung)
    {
        protokoll?.Report(meldung);
        Diagnose.Ereignis(schwere, "repo", typ, meldung);
    }

    private static AbgleichErgebnis Melden(AbgleichErgebnis ergebnis)
    {
        Diagnose.Ereignis(ergebnis.GezogenOk ? Schwere.Info : Schwere.Warnung, "repo", "repo.abgleich", ergebnis.Meldung);
        return ergebnis;
    }

    private static string Kurz(string text)
    {
        var zeile = (text ?? "").Replace('\n', ' ').Trim();
        return zeile.Length > 300 ? zeile[..300] + " …" : zeile;
    }
}
