using System.Diagnostics;
using System.IO;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>
/// Real git, throwaway repos. Rebuilds what a killed fast-forward leaves behind: the working tree
/// partly on the upstream state, HEAD and index on the old one, and an orphaned index.lock.
/// </summary>
public sealed class RepoVorspulenTests : IDisposable
{
    private const int DateienAnzahl = 60;

    private readonly string _wurzel = Path.Combine(Path.GetTempPath(), "uz-vorspulen-" + Guid.NewGuid().ToString("N"));

    public void Dispose()
    {
        try
        {
            foreach (var datei in Directory.EnumerateFiles(_wurzel, "*", SearchOption.AllDirectories))
                File.SetAttributes(datei, FileAttributes.Normal);   // git objects are read-only
            Directory.Delete(_wurzel, true);
        }
        catch { }
    }

    private static string Git(string ordner, string args)
    {
        using var p = Process.Start(new ProcessStartInfo("git", args)
        {
            WorkingDirectory = ordner, UseShellExecute = false, CreateNoWindow = true,
            RedirectStandardOutput = true, RedirectStandardError = true
        })!;
        var fehlerLesen = p.StandardError.ReadToEndAsync();
        var ausgabe = p.StandardOutput.ReadToEnd();
        var fehler = fehlerLesen.GetAwaiter().GetResult();
        p.WaitForExit();
        if (p.ExitCode != 0) throw new InvalidOperationException("git " + args + ": " + fehler);
        return ausgabe.Trim();
    }

    /// <summary>Clone one commit behind origin; arbeit holds the upstream state on disk.</summary>
    private (string klon, string arbeit) Aufbauen()
    {
        var origin = Path.Combine(_wurzel, "origin.git");
        var arbeit = Path.Combine(_wurzel, "arbeit");
        var klon = Path.Combine(_wurzel, "klon");
        Directory.CreateDirectory(origin);
        Git(origin, "init --bare -b main");
        Git(_wurzel, "clone origin.git arbeit");
        Git(arbeit, "config user.email t@t");
        Git(arbeit, "config user.name t");
        Git(arbeit, "checkout -b main");
        for (var i = 0; i < DateienAnzahl; i++) File.WriteAllText(Path.Combine(arbeit, "d" + i + ".txt"), "alt");
        File.WriteAllText(Path.Combine(arbeit, "geht-weg.txt"), "alt");
        Git(arbeit, "add -A");
        Git(arbeit, "commit -m eins");
        Git(arbeit, "push -u origin main");
        Git(_wurzel, "clone origin.git klon");
        Git(klon, "config user.email t@t");
        Git(klon, "config user.name t");
        for (var i = 0; i < DateienAnzahl; i++) File.WriteAllText(Path.Combine(arbeit, "d" + i + ".txt"), "neu");
        File.WriteAllText(Path.Combine(arbeit, "kam-an.txt"), "neu");
        File.WriteAllText(Path.Combine(arbeit, "fehlt.txt"), "neu");
        File.Delete(Path.Combine(arbeit, "geht-weg.txt"));
        Git(arbeit, "add -A");
        Git(arbeit, "commit -m zwei");
        Git(arbeit, "push");
        Git(klon, "fetch");
        return (klon, arbeit);
    }

    /// <summary>The killed run got as far as file <paramref name="bis"/>.</summary>
    private static void HalbVorspulen(string klon, string arbeit, int bis)
    {
        for (var i = 0; i < bis; i++) File.Copy(Path.Combine(arbeit, "d" + i + ".txt"), Path.Combine(klon, "d" + i + ".txt"), true);
        File.Copy(Path.Combine(arbeit, "kam-an.txt"), Path.Combine(klon, "kam-an.txt"));
    }

    private static string SperreAnlegen(string klon, TimeSpan alter)
    {
        var sperre = Path.Combine(klon, ".git", "index.lock");
        File.WriteAllBytes(sperre, []);
        File.SetLastWriteTimeUtc(sperre, DateTime.UtcNow - alter);
        return sperre;
    }

    [Theory]
    [InlineData(DateienAnzahl)]
    [InlineData(25)]    // killed early: most files still hold the old content
    public async Task Abgebrochenes_Vorspulen_wird_vollendet_und_lokale_Aenderungen_bleiben(int bis)
    {
        var (klon, arbeit) = Aufbauen();
        HalbVorspulen(klon, arbeit, bis);
        File.WriteAllText(Path.Combine(klon, "d0.txt"), "lokal");
        File.WriteAllText(Path.Combine(klon, "eigen.txt"), "lokal");
        var sperre = SperreAnlegen(klon, TimeSpan.FromMinutes(30));

        var lauf = await RepoAbgleich.VorspulenAsync(klon, nutzerStart: true, gitLaeuft: () => false);

        Assert.Equal(0, lauf.ExitCode);
        Assert.False(File.Exists(sperre));
        Assert.Equal(Git(klon, "rev-parse origin/main"), Git(klon, "rev-parse HEAD"));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "d0.txt")));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "eigen.txt")));
        Assert.Equal("neu", File.ReadAllText(Path.Combine(klon, "fehlt.txt")));
        Assert.Equal("neu", File.ReadAllText(Path.Combine(klon, "d" + (DateienAnzahl - 1) + ".txt")));
        Assert.False(File.Exists(Path.Combine(klon, "geht-weg.txt")));
        Assert.Equal("M d0.txt", Git(klon, "status --porcelain -uno"));
        Assert.Empty(Git(klon, "stash list"));
    }

    [Fact]
    public async Task Die_Pruefung_setzt_nichts_zurueck_und_nimmt_nur_die_Sperre_weg()
    {
        var (klon, arbeit) = Aufbauen();
        var vorher = Git(klon, "rev-parse HEAD");
        HalbVorspulen(klon, arbeit, DateienAnzahl);
        var sperre = SperreAnlegen(klon, TimeSpan.FromMinutes(30));

        var lauf = await RepoAbgleich.VorspulenAsync(klon, nutzerStart: false, gitLaeuft: () => false);

        Assert.NotEqual(0, lauf.ExitCode);
        Assert.False(File.Exists(sperre));
        Assert.Equal(vorher, Git(klon, "rev-parse HEAD"));
    }

    [Fact]
    public async Task Vorgemerkte_Dateien_verhindern_die_Reparatur()
    {
        var (klon, arbeit) = Aufbauen();
        HalbVorspulen(klon, arbeit, DateienAnzahl);
        File.WriteAllText(Path.Combine(klon, "vorgemerkt.txt"), "lokal");
        Git(klon, "add vorgemerkt.txt");
        // Blocks the autostash fallback too, so the test sees only what the repair decided.
        File.WriteAllText(Path.Combine(klon, "fehlt.txt"), "lokal");

        var lauf = await RepoAbgleich.VorspulenAsync(klon, nutzerStart: true, gitLaeuft: () => false);

        Assert.Contains("vorgemerkt.txt", Git(klon, "diff --cached --name-only"));
        Assert.DoesNotContain("Abgebrochenes Vorspulen", lauf.Ausgabe);
    }

    [Fact]
    public async Task Echte_lokale_Aenderung_wird_nicht_als_halbes_Vorspulen_behandelt()
    {
        var (klon, _) = Aufbauen();
        var vorher = Git(klon, "rev-parse HEAD");
        File.WriteAllText(Path.Combine(klon, "d0.txt"), "lokal");

        var lauf = await RepoAbgleich.VorspulenAsync(klon, nutzerStart: false, gitLaeuft: () => false);

        Assert.NotEqual(0, lauf.ExitCode);
        Assert.Equal(vorher, Git(klon, "rev-parse HEAD"));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "d0.txt")));
    }

    [Fact]
    public async Task Nutzerstart_spult_mit_Autostash_trotz_lokaler_Aenderung_vor()
    {
        var (klon, _) = Aufbauen();
        File.WriteAllText(Path.Combine(klon, "eigen.txt"), "lokal");
        Git(klon, "add eigen.txt");

        var lauf = await RepoAbgleich.VorspulenAsync(klon, nutzerStart: true, gitLaeuft: () => false);

        Assert.Equal(0, lauf.ExitCode);
        Assert.Equal(Git(klon, "rev-parse origin/main"), Git(klon, "rev-parse HEAD"));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "eigen.txt")));
    }

    [Fact]
    public async Task Kollidierender_Autostash_gilt_als_Fehler_und_nennt_den_Stash()
    {
        var (klon, _) = Aufbauen();
        File.WriteAllText(Path.Combine(klon, "d0.txt"), "lokal");

        var lauf = await RepoAbgleich.VorspulenAsync(klon, nutzerStart: true, gitLaeuft: () => false);

        Assert.NotEqual(0, lauf.ExitCode);
        Assert.Contains("Stash", RepoAbgleich.VorspulGrund(lauf));
        Assert.NotEmpty(Git(klon, "stash list"));
    }

    [Theory]
    [InlineData(false, 30, true)]
    [InlineData(true, 30, false)]   // a running git may own the lock, whatever its age
    [InlineData(false, 1, false)]   // a young lock may belong to a git that is just starting
    public void Sperre_gilt_nur_ohne_Git_und_mit_Mindestalter_als_verwaist(bool gitLaeuft, int alterMinuten, bool erwartet)
    {
        var jetzt = DateTime.UtcNow;
        Assert.Equal(erwartet, RepoAbgleich.SperreVerwaist(jetzt.AddMinutes(-alterMinuten), jetzt, gitLaeuft));
    }

    [Theory]
    [InlineData(3307, 6, true)]     // the real case of 02.11.2026
    [InlineData(9, 0, false)]       // too little to tell it from ordinary local work
    [InlineData(100, 100, false)]   // as much own work as written files: leave it to a human
    public void Halbes_Vorspulen_verlangt_eine_klare_Mehrheit(int geschrieben, int eigeneArbeit, bool erwartet)
        => Assert.Equal(erwartet, RepoAbgleich.IstHalbesVorspulen(geschrieben, eigeneArbeit));

    [Theory]
    [InlineData("error: Unable to create '.git/index.lock': File exists.", false, false, "index.lock")]
    [InlineData("error: Your local changes would be overwritten", false, false, "lokale Änderungen")]
    [InlineData("error: Your local changes would be overwritten", true, false, "Zeitlimit")]
    [InlineData("error: Your local changes would be overwritten", false, true, "abgebrochen")]
    public void Der_Grund_nennt_die_wahre_Ursache(string ausgabe, bool abgelaufen, bool abgebrochen, string erwartet)
        => Assert.Contains(erwartet, RepoAbgleich.VorspulGrund(new BefehlErgebnis(1, ausgabe, abgelaufen, Abgebrochen: abgebrochen)));
}
