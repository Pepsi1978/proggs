using System.Diagnostics;
using System.IO;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>
/// Real git, throwaway repos. Rebuilds what a killed fast-forward leaves behind: the working tree
/// on the upstream state, HEAD and index on the old one, and an orphaned index.lock.
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
        Git(arbeit, "add -A");
        Git(arbeit, "commit -m eins");
        Git(arbeit, "push -u origin main");
        Git(_wurzel, "clone origin.git klon");
        for (var i = 0; i < DateienAnzahl; i++) File.WriteAllText(Path.Combine(arbeit, "d" + i + ".txt"), "neu");
        File.WriteAllText(Path.Combine(arbeit, "kam-an.txt"), "neu");
        File.WriteAllText(Path.Combine(arbeit, "fehlt.txt"), "neu");
        Git(arbeit, "add -A");
        Git(arbeit, "commit -m zwei");
        Git(arbeit, "push");
        Git(klon, "fetch");
        return (klon, arbeit);
    }

    private static void HalbVorspulen(string klon, string arbeit)
    {
        for (var i = 0; i < DateienAnzahl; i++) File.Copy(Path.Combine(arbeit, "d" + i + ".txt"), Path.Combine(klon, "d" + i + ".txt"), true);
        File.Copy(Path.Combine(arbeit, "kam-an.txt"), Path.Combine(klon, "kam-an.txt"));
    }

    private static string SperreAnlegen(string klon, TimeSpan alter)
    {
        var sperre = Path.Combine(klon, ".git", "index.lock");
        File.WriteAllBytes(sperre, []);
        File.SetLastWriteTimeUtc(sperre, DateTime.UtcNow - alter);
        return sperre;
    }

    [Fact]
    public async Task Abgebrochenes_Vorspulen_wird_vollendet_und_lokale_Aenderungen_bleiben()
    {
        var (klon, arbeit) = Aufbauen();
        HalbVorspulen(klon, arbeit);
        File.WriteAllText(Path.Combine(klon, "d0.txt"), "lokal");
        File.WriteAllText(Path.Combine(klon, "eigen.txt"), "lokal");
        var sperre = SperreAnlegen(klon, TimeSpan.FromMinutes(30));

        var lauf = await RepoAbgleich.VorspulenAsync(klon, autostash: false, gitLaeuft: () => false);

        Assert.Equal(0, lauf.ExitCode);
        Assert.False(File.Exists(sperre));
        Assert.Equal(Git(klon, "rev-parse origin/main"), Git(klon, "rev-parse HEAD"));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "d0.txt")));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "eigen.txt")));
        Assert.Equal("neu", File.ReadAllText(Path.Combine(klon, "fehlt.txt")));
        Assert.Equal("M d0.txt", Git(klon, "status --porcelain -uno"));
    }

    [Fact]
    public async Task Echte_lokale_Aenderung_wird_nicht_als_halbes_Vorspulen_behandelt()
    {
        var (klon, _) = Aufbauen();
        var vorher = Git(klon, "rev-parse HEAD");
        File.WriteAllText(Path.Combine(klon, "d0.txt"), "lokal");

        var lauf = await RepoAbgleich.VorspulenAsync(klon, autostash: false, gitLaeuft: () => false);

        Assert.NotEqual(0, lauf.ExitCode);
        Assert.Equal(vorher, Git(klon, "rev-parse HEAD"));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "d0.txt")));
    }

    [Fact]
    public async Task Autostash_spult_trotz_lokaler_Aenderung_vor()
    {
        var (klon, _) = Aufbauen();
        File.WriteAllText(Path.Combine(klon, "eigen.txt"), "lokal");
        Git(klon, "add eigen.txt");

        var lauf = await RepoAbgleich.VorspulenAsync(klon, autostash: true, gitLaeuft: () => false);

        Assert.Equal(0, lauf.ExitCode);
        Assert.Equal(Git(klon, "rev-parse origin/main"), Git(klon, "rev-parse HEAD"));
        Assert.Equal("lokal", File.ReadAllText(Path.Combine(klon, "eigen.txt")));
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
    [InlineData(49, 0, false)]      // too little to tell it from ordinary local work
    [InlineData(100, 11, false)]
    public void Halbes_Vorspulen_verlangt_eine_klare_Mehrheit(int zuHead, int zuUpstream, bool erwartet)
        => Assert.Equal(erwartet, RepoAbgleich.IstHalbesVorspulen(zuHead, zuUpstream));

    [Fact]
    public void Der_Grund_nennt_die_Sperre_statt_lokaler_Aenderungen()
    {
        Assert.Contains("index.lock", RepoAbgleich.VorspulGrund("error: Unable to create '.git/index.lock': File exists."));
        Assert.Contains("lokale Änderungen", RepoAbgleich.VorspulGrund("error: Your local changes would be overwritten"));
    }
}
