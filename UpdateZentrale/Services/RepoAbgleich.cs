using System.IO;
using System.Text;

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
            var ziehen = await VorspulenAsync(repoWurzel, nutzerStart: false);
            if (ziehen.Abgelaufen || ziehen.ExitCode != 0)
                return Melden(new AbgleichErgebnis(true, false,
                    "Repo ließ sich nicht vorspulen (" + VorspulGrund(ziehen) + "): "
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
    /// Fast-forwards the checkout to its upstream. An orphaned index.lock is always removed. What
    /// touches the index or moves local changes aside runs only for an update the user started:
    /// the check before it also runs unattended, while parallel sessions have files staged.
    /// </summary>
    /// <param name="nutzerStart">
    /// A refused fast-forward may first be finished as a killed run, then retried with autostash.
    /// In that order: an autostash on a half-finished run would stash thousands of pseudo-changes.
    /// </param>
    /// <param name="gitLaeuft">Test seam: is a git process alive on this machine?</param>
    internal static async Task<BefehlErgebnis> VorspulenAsync(string repoWurzel, bool nutzerStart,
        IProgress<string>? protokoll = null, CancellationToken abbruch = default, Func<bool>? gitLaeuft = null)
    {
        await SperreFreimachenAsync(repoWurzel, protokoll, abbruch, gitLaeuft ?? GitProzessLaeuft);

        var zug = await Kommandozeile.AusfuehrenAsync("git", "merge --ff-only --quiet @{u}",
            VorspulZeitlimit, repoWurzel, abbruch);
        if (zug.ExitCode == 0 || zug.Abgelaufen || zug.Abgebrochen || !nutzerStart
            || zug.Ausgabe.Contains("index.lock", StringComparison.OrdinalIgnoreCase))
            return zug;

        if (await HalbesVorspulenVollendenAsync(repoWurzel, protokoll, abbruch) is { } repariert) return repariert;

        var mitStash = await Kommandozeile.AusfuehrenAsync("git", "merge --ff-only --quiet --autostash @{u}",
            VorspulZeitlimit, repoWurzel, abbruch);
        // git reports success here: the tree is at the upstream, the local changes wait in the stash.
        if (mitStash.ExitCode == 0 && mitStash.Ausgabe.Contains(StashKonflikt, StringComparison.OrdinalIgnoreCase))
        {
            await LaufzeitdateienEntkonfliktenAsync(repoWurzel, protokoll, abbruch);
            return mitStash with { ExitCode = 1 };
        }
        return mitStash;
    }

    private const string StashKonflikt = "safe in the stash";

    /// <summary>
    /// Files the OpenLauncher rewrites at runtime (models.json) and that are versioned at the same time.
    /// A collision with the autostash leaves conflict markers in them; the launcher cannot parse that,
    /// falls back to its defaults and overwrites the user's model list (hidden models, order, providers).
    /// </summary>
    private static readonly string[] Laufzeitdateien = ["models.json"];

    /// <summary>
    /// After a colliding autostash the user's own state of a runtime file wins: take the stash side
    /// ("theirs" while applying a stash) and unstage it. The other conflicted files keep their markers.
    /// </summary>
    private static async Task LaufzeitdateienEntkonfliktenAsync(string repoWurzel, IProgress<string>? protokoll,
                                                                CancellationToken abbruch)
    {
        var limit = TimeSpan.FromMinutes(2);
        var offen = await Kommandozeile.AusfuehrenAsync("git", "diff --name-only --diff-filter=U", limit, repoWurzel, abbruch);
        if (offen.ExitCode != 0) return;

        foreach (var zeile in offen.Ausgabe.Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            var datei = Path.GetFileName(zeile);
            if (!Laufzeitdateien.Contains(datei, StringComparer.OrdinalIgnoreCase)) continue;

            var lokal = await Kommandozeile.AusfuehrenAsync("git", $"checkout --theirs -- \"{zeile}\"", limit, repoWurzel, abbruch);
            if (lokal.ExitCode != 0) continue;
            await Kommandozeile.AusfuehrenAsync("git", $"reset --quiet -- \"{zeile}\"", limit, repoWurzel, abbruch);
            Berichten(protokoll, "repo.laufzeitdatei", $"{zeile}: Konflikt mit dem Autostash, der lokale Stand bleibt erhalten.");
        }
    }

    /// <summary>The real reason for a refused fast-forward, not a guess at the usual one.</summary>
    internal static string VorspulGrund(BefehlErgebnis lauf)
    {
        if (lauf.Abgebrochen) return "abgebrochen";
        if (lauf.Abgelaufen) return "Zeitlimit überschritten";
        var ausgabe = lauf.Ausgabe ?? "";
        if (ausgabe.Contains(StashKonflikt, StringComparison.OrdinalIgnoreCase))
            return "lokale Änderungen kollidieren mit den neuen Commits, sie liegen jetzt im Git-Stash";
        return ausgabe.Contains("index.lock", StringComparison.OrdinalIgnoreCase)
            ? "Git-Sperre .git/index.lock: ein anderer Git-Prozess läuft, oder ein abgebrochener hat sie hinterlassen"
            : "lokale Änderungen oder abweichender Verlauf";
    }

    /// <summary>Git keeps its process alive as long as it holds the lock, also during a commit hook.</summary>
    internal static bool SperreVerwaist(DateTime sperreZeitUtc, DateTime jetztUtc, bool gitLaeuft)
        => !gitLaeuft && jetztUtc - sperreZeitUtc >= SperreMindestalter;

    private static bool GitProzessLaeuft()
        => Prozessdienst.LaeuftNachName("git") || Prozessdienst.LaeuftNachName("git-lfs");

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
                    Berichten(protokoll, "repo.sperre", "Verwaiste Git-Sperre .git/index.lock entfernt.");
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

    /// <summary>Below this nothing tells a killed run from ordinary local work, and autostash copes.</summary>
    private const int HalbMindestens = 10;

    /// <summary>
    /// Git writes the files first and moves HEAD last, so a run killed in between leaves files that
    /// match the upstream while differing from HEAD. Nobody produces those by hand in numbers.
    /// </summary>
    /// <param name="geschrieben">Differs from HEAD, matches the upstream: written by the killed run.</param>
    /// <param name="eigeneArbeit">Differs from both: real local work.</param>
    internal static bool IstHalbesVorspulen(int geschrieben, int eigeneArbeit)
        => geschrieben >= HalbMindestens && geschrieben > eigeneArbeit;

    /// <summary>
    /// Finishes a killed fast-forward at any point it died. Each file counts by what it holds:
    /// a file still equal to HEAD was simply not written yet and is taken from the upstream, and a
    /// file differing from both is local work and stays. HEAD moves last, so a failure on the way
    /// leaves the state in which the next run tries again.
    /// </summary>
    /// <returns>Null when this is no killed fast-forward; otherwise the outcome of the repair.</returns>
    private static async Task<BefehlErgebnis?> HalbesVorspulenVollendenAsync(string repoWurzel, IProgress<string>? protokoll,
                                                                            CancellationToken abbruch)
    {
        var limit = TimeSpan.FromMinutes(5);

        var vorfahr = await Kommandozeile.AusfuehrenAsync("git", "merge-base --is-ancestor HEAD @{u}", limit, repoWurzel, abbruch);
        if (vorfahr.ExitCode != 0) return null;

        // Something staged belongs to a parallel session, and a reset would take it out of the index.
        var vorgemerkt = await Kommandozeile.AusfuehrenAsync("git", "diff --cached --quiet", limit, repoWurzel, abbruch);
        if (vorgemerkt.ExitCode != 0) return null;

        const string nurPfade = "-c core.quotePath=false -c core.safecrlf=false diff --name-only";
        var zuHead = await Kommandozeile.AusfuehrenAsync("git", nurPfade, limit, repoWurzel, abbruch);
        if (zuHead.ExitCode != 0) return null;
        var geaendert = Pfade(zuHead.Ausgabe);
        if (geaendert.Count < HalbMindestens) return null;

        // A separate index compares the working tree with the upstream without touching the real one.
        var tempIndex = Path.Combine(Path.GetTempPath(), "updatezentrale-index-" + Guid.NewGuid().ToString("N"));
        var umgebung = new Dictionary<string, string> { ["GIT_INDEX_FILE"] = tempIndex };
        HashSet<string> zuUpstream;
        try
        {
            var lesen = await Kommandozeile.AusfuehrenAsync("git", "read-tree @{u}", limit, repoWurzel, abbruch, zusatzUmgebung: umgebung);
            if (lesen.ExitCode != 0) return null;
            var vergleich = await Kommandozeile.AusfuehrenAsync("git", nurPfade, limit, repoWurzel, abbruch, zusatzUmgebung: umgebung);
            if (vergleich.ExitCode != 0) return null;
            zuUpstream = Pfade(vergleich.Ausgabe);
        }
        finally
        {
            DateienLoeschen(tempIndex, tempIndex + ".lock");
        }

        var eigeneArbeit = geaendert.Count(zuUpstream.Contains);
        if (!IstHalbesVorspulen(geaendert.Count - eigeneArbeit, eigeneArbeit)) return null;

        var nachholen = zuUpstream
            .Where(p => !geaendert.Contains(p) || !File.Exists(Path.Combine(repoWurzel, p)))
            .ToList();
        if (nachholen.Count > 0)
        {
            var liste = Path.Combine(Path.GetTempPath(), "updatezentrale-pfade-" + Guid.NewGuid().ToString("N"));
            try
            {
                await File.WriteAllLinesAsync(liste, nachholen, new UTF8Encoding(false), abbruch);
                var holen = await Kommandozeile.AusfuehrenAsync("git", "checkout @{u} --pathspec-from-file=\"" + liste + "\"",
                    VorspulZeitlimit, repoWurzel, abbruch);
                if (holen.ExitCode != 0) return holen;
            }
            finally
            {
                DateienLoeschen(liste);
            }
        }

        // Removed upstream, untouched here: the killed run simply had not got to it yet.
        var entfernt = await Kommandozeile.AusfuehrenAsync("git", "-c core.quotePath=false diff --name-only --diff-filter=D HEAD @{u}",
            limit, repoWurzel, abbruch);
        if (entfernt.ExitCode == 0)
            DateienLoeschen(Pfade(entfernt.Ausgabe).Where(p => !geaendert.Contains(p))
                                                   .Select(p => Path.Combine(repoWurzel, p)).ToArray());

        var zurueck = await Kommandozeile.AusfuehrenAsync("git", "reset -q --mixed @{u}", VorspulZeitlimit, repoWurzel, abbruch);
        if (zurueck.ExitCode != 0) return zurueck;

        var meldung = "Abgebrochenes Vorspulen vollendet: HEAD und Index auf den Stand von origin gesetzt, "
                      + nachholen.Count + " Datei(en) nachgeholt, " + eigeneArbeit + " lokale Änderung(en) behalten.";
        Berichten(protokoll, "repo.halbes-vorspulen", meldung);
        return new BefehlErgebnis(0, meldung, false);
    }

    /// <summary>The output also carries stderr; only lines that cannot be git's own messages are paths.</summary>
    private static HashSet<string> Pfade(string ausgabe)
        => (ausgabe ?? "").Split('\n').Select(z => z.Trim().Trim('"'))
            .Where(z => z.Length > 0 && !z.StartsWith("warning: ", StringComparison.Ordinal)
                        && !z.StartsWith("hint: ", StringComparison.Ordinal))
            .ToHashSet(StringComparer.Ordinal);

    private static void DateienLoeschen(params string[] dateien)
    {
        foreach (var datei in dateien)
            try { File.Delete(datei); }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException) { Diagnose.Gefangen(ex, "repo", Schwere.Debug); }
    }

    private static void Berichten(IProgress<string>? protokoll, string typ, string meldung)
    {
        protokoll?.Report(meldung);
        Diagnose.Ereignis(Schwere.Warnung, "repo", typ, meldung);
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
