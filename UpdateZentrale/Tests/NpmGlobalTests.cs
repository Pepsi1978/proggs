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
    public void Vorab_Download_ist_nie_global_und_ohne_Skripte()
    {
        var argumente = NpmGlobal.VorabArgumente("@openai/codex", @"C:\Temp\uz");
        Assert.DoesNotContain(" -g", argumente);
        Assert.Contains("--prefix \"C:\\Temp\\uz\"", argumente);
        Assert.Contains("--ignore-scripts", argumente);
        Assert.Contains("@openai/codex@latest", argumente);
    }
}
