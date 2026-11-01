using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using Xunit;

namespace UpdateZentrale.Tests;

public sealed class NpmGlobalTests
{
    private const string Global = @"C:\Users\x\AppData\Roaming\npm";

    [Fact]
    public void Shim_im_globalen_npm_Ordner_ist_betroffen()
        => Assert.True(NpmGlobal.Betrifft(new ProgrammEintrag { NpmPaket = "@openai/codex" }, Global + @"\codex.cmd", Global + @"\"));

    [Fact]
    public void Native_Installation_mit_npmPaket_als_Versionsquelle_bleibt_aussen_vor()
        => Assert.False(NpmGlobal.Betrifft(new ProgrammEintrag { NpmPaket = "bun" }, @"C:\Users\x\.bun\bin\bun.exe", Global));

    [Fact]
    public void Ohne_npmPaket_ist_nichts_betroffen()
        => Assert.False(NpmGlobal.Betrifft(new ProgrammEintrag(), Global + @"\kimi.cmd", Global));

    [Fact]
    public void Shim_ohne_Paketordner_gilt_als_zerstoert_Umzug_auf_nativen_Pfad_nicht()
    {
        var wurzel = Directory.CreateTempSubdirectory("uz-npmtest-").FullName;
        try
        {
            var shim = Path.Combine(wurzel, "codex.cmd");
            var nativ = Path.Combine(wurzel, "codex.exe");
            File.WriteAllText(shim, "x");
            var eintrag = new ProgrammEintrag { NpmPaket = "@openai/codex", ExePfad = nativ, ExePfadAlternativen = { shim } };

            Assert.True(NpmGlobal.Zerstoert(eintrag, shim, wurzel));

            Directory.CreateDirectory(Path.Combine(wurzel, "node_modules", "@openai", "codex"));
            Assert.False(NpmGlobal.Zerstoert(eintrag, shim, wurzel));

            File.Delete(shim);
            Assert.True(NpmGlobal.Zerstoert(eintrag, shim, wurzel));

            File.WriteAllText(nativ, "x");
            Assert.False(NpmGlobal.Zerstoert(eintrag, shim, wurzel));
        }
        finally
        {
            Directory.Delete(wurzel, recursive: true);
        }
    }

    [Fact]
    public void Wiederherstellung_uebernimmt_den_npm_Befehl_aus_dem_Katalog()
    {
        var firebase = new ProgrammEintrag { NpmPaket = "firebase-tools", UpdateArgumente = "install -g --allow-scripts=re2 firebase-tools@latest" };
        Assert.Equal(firebase.UpdateArgumente, NpmGlobal.WiederherstellArgumente(firebase));

        var codex = new ProgrammEintrag { NpmPaket = "@openai/codex", UpdateArgumente = "update" };
        Assert.StartsWith("install -g @openai/codex@latest", NpmGlobal.WiederherstellArgumente(codex));
    }

    [Fact]
    public void Lebenszeichen_steigt_auch_wenn_der_Ordner_schrumpfen_wuerde()
    {
        var zeichen = NpmGlobal.DownloadLebenszeichen();
        var erstes = zeichen();
        Assert.True(zeichen() >= erstes);
    }

    [Fact]
    public void Vorab_Download_ist_nie_global_und_ohne_Skripte()
    {
        var argumente = NpmGlobal.VorabArgumente("@openai/codex", @"C:\Temp\uz");
        Assert.DoesNotContain(" -g", argumente);
        Assert.Contains("--prefix \"C:\\Temp\\uz\"", argumente);
        Assert.Contains("--ignore-scripts", argumente);
        Assert.Contains("@openai/codex@latest", argumente);
    }
}
