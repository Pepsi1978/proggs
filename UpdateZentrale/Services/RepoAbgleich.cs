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
            var ziehen = await Kommandozeile.AusfuehrenAsync("git", "merge --ff-only --quiet @{u}",
                TimeSpan.FromMinutes(2), repoWurzel);
            if (ziehen.Abgelaufen || ziehen.ExitCode != 0)
                return Melden(new AbgleichErgebnis(true, false,
                    "Repo ließ sich nicht vorspulen (lokale Änderungen oder abweichender Verlauf): "
                    + Kurz(ziehen.Ausgabe), DateTime.Now));

            return Melden(new AbgleichErgebnis(true, true, "Repo ist auf dem Stand von origin.", DateTime.Now));
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "repo", "Repo-Abgleich", Schwere.Warnung);
            return new AbgleichErgebnis(false, false, "Repo-Abgleich fehlgeschlagen: " + ex.Message, DateTime.Now);
        }
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
