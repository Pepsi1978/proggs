using System.Text.RegularExpressions;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;
using Xunit;

namespace UpdateZentrale.Tests;

/// <summary>
/// The developer-tool cards: winget packages correlated only by name, version fields with an
/// operator, side-by-side installs, rustup's exit code 100 and sdkmanager's update table.
/// </summary>
public sealed class EntwicklungsKartenTests
{
    private static BefehlErgebnis Lauf(string ausgabe, int code = 0, bool abgelaufen = false) => new(code, ausgabe, abgelaufen);

    [Fact]
    public void Auswahl_nimmt_den_Namen_nur_wenn_er_gesetzt_ist()
    {
        Assert.Equal("--id Git.Git --exact", WingetAktualisierer.Auswahl(new ProgrammEintrag { WingetId = "Git.Git" }));
        Assert.Equal("--name \"PowerShell\"",
            WingetAktualisierer.Auswahl(new ProgrammEintrag { WingetId = "Microsoft.PowerShell", WingetName = "PowerShell" }));
    }

    [Fact]
    public void Update_per_Install_nutzt_immer_die_Kennung()
    {
        var ps = new ProgrammEintrag { WingetId = "Microsoft.PowerShell", WingetName = "PowerShell", WingetUpdatePerInstall = true };
        Assert.Equal("install --id Microsoft.PowerShell --exact", WingetAktualisierer.UpdateBefehl(ps));
        ps.WingetInstallerTyp = "wix";
        Assert.Equal("install --id Microsoft.PowerShell --exact --installer-type wix", WingetAktualisierer.UpdateBefehl(ps));
        Assert.Equal("upgrade --id Go --exact", WingetAktualisierer.UpdateBefehl(new ProgrammEintrag { WingetId = "Go" }));
    }

    [Fact]
    public void PowerShell_aus_der_Namenssuche_wird_mit_Update_erkannt()
    {
        const string ausgabe = "Name                   Id                   Version Available Source\n"
                             + "--------------------------------------------------------------------\n"
                             + "PowerShell 7.6.6.0-x64 Microsoft.PowerShell 7.6.6.0 7.6.7.0   winget\n";
        var ergebnis = WingetAktualisierer.ListeAuswerten(Lauf(ausgabe), "Microsoft.PowerShell");
        Assert.Equal(UpdateZustand.UpdateVerfuegbar, ergebnis.Zustand);
        Assert.Equal("7.6.6.0", ergebnis.InstallierteVersion);
        Assert.Equal("7.6.7.0", ergebnis.VerfuegbareVersion);
    }

    [Fact]
    public void Id_als_Teil_einer_laengeren_Id_zaehlt_nicht()
    {
        const string ausgabe = "PowerShell Preview Microsoft.PowerShell.Preview 7.7.0.3 7.7.0.4 winget\n";
        Assert.Null(WingetAktualisierer.TabellenZeile(ausgabe, "Microsoft.PowerShell"));
    }

    [Fact]
    public void Kein_Paket_aber_Programm_vorhanden_heisst_nicht_nicht_installiert()
    {
        var lauf = Lauf("No installed package found matching input criteria.", WingetAktualisierer.KeinPaketGefunden);
        Assert.Equal(UpdateZustand.Unbekannt, WingetAktualisierer.ListeAuswerten(lauf, "Microsoft.PowerShell", hierVorhanden: true).Zustand);
        Assert.Equal(UpdateZustand.NichtInstalliert, WingetAktualisierer.ListeAuswerten(lauf, "Microsoft.PowerShell").Zustand);
    }

    [Fact]
    public void Kleiner_Zeichen_ist_Teil_der_installierten_Version()
    {
        var zeile = WingetAktualisierer.TabellenZeile("Python Launcher Python.Launcher < 3.14.7 3.14.7 winget\n", "Python.Launcher");
        Assert.Equal(("< 3.14.7", "3.14.7"), zeile);
        Assert.Equal(UpdateZustand.UpdateVerfuegbar,
            WingetAktualisierer.ListeAuswerten(Lauf("Python Launcher Python.Launcher < 3.14.7 3.14.7 winget\n"), "Python.Launcher").Zustand);
    }

    [Fact]
    public void Groesser_Zeichen_ohne_neuere_Version_ist_aktuell()
    {
        var ergebnis = WingetAktualisierer.ListeAuswerten(
            Lauf("Visual Studio Build Tools 2022 Microsoft.VisualStudio.2022.BuildTools > 17.14.41 winget\n"),
            "Microsoft.VisualStudio.2022.BuildTools");
        Assert.Equal(UpdateZustand.Aktuell, ergebnis.Zustand);
        Assert.Equal("> 17.14.41", ergebnis.InstallierteVersion);
    }

    [Fact]
    public void Bei_mehreren_installierten_Fassungen_zaehlt_die_neueste()
    {
        const string ausgabe = "Microsoft .NET SDK 10.0.204 (x64) Microsoft.DotNet.SDK.10 10.0.204 10.0.402 winget\n"
                             + "Microsoft .NET SDK 10.0.402 (x64) Microsoft.DotNet.SDK.10 10.0.402          winget\n";
        var ergebnis = WingetAktualisierer.ListeAuswerten(Lauf(ausgabe), "Microsoft.DotNet.SDK.10");
        Assert.Equal(UpdateZustand.Aktuell, ergebnis.Zustand);
        Assert.Equal("10.0.402", ergebnis.InstallierteVersion);
    }

    [Theory]
    [InlineData(unchecked((int)0x8A150109), "", true)]
    [InlineData(3010, "", true)]
    [InlineData(1, "Restart your PC to finish installation.", true)]
    [InlineData(1, "Installer failed", false)]
    public void Neustart_noetig_wird_erkannt(int code, string ausgabe, bool erwartet)
        => Assert.Equal(erwartet, WingetAktualisierer.BrauchtNeustart(Lauf(ausgabe, code)));

    [Fact]
    public void Vom_Hersteller_gesperrtes_Update_wird_erkannt()
    {
        Assert.True(WingetAktualisierer.NurUeberHersteller(Lauf(
            "The package cannot be upgraded using WinGet. Please use the method provided by the publisher for upgrading this package.", 20)));
        Assert.False(WingetAktualisierer.NurUeberHersteller(Lauf("Successfully installed")));
    }

    [Fact]
    public void Kindprozesse_bekommen_JAVA_HOME_und_PATH_frisch_aus_Windows()
    {
        var umgebung = new Dictionary<string, string?> { ["JAVA_HOME"] = @"C:\gibt\es\nicht", ["PATH"] = "alt" };
        Kommandozeile.FrischeUmgebung(umgebung);

        var erwartet = Environment.GetEnvironmentVariable("JAVA_HOME", EnvironmentVariableTarget.User)
                       ?? Environment.GetEnvironmentVariable("JAVA_HOME", EnvironmentVariableTarget.Machine);
        Assert.Equal(erwartet, umgebung.GetValueOrDefault("JAVA_HOME"));
        Assert.NotEqual("alt", umgebung["PATH"]);
        Assert.Contains(@"\Windows\System32", umgebung["PATH"], StringComparison.OrdinalIgnoreCase);
    }

    private const string RustupCheck =
        "stable-x86_64-pc-windows-msvc - update available: 1.96.0 (ac68faa20 2026-05-25) -> 1.99.0 (b940084d7 2026-09-28)\n"
        + "rustup - update available : 1.29.0 -> 1.29.1\n";

    [Fact]
    public void Rustup_Code_100_ist_eine_gueltige_Antwort()
    {
        var eintrag = new ProgrammEintrag { PruefExitCodes = new() { 0, 100 } };
        Assert.True(CliAktualisierer.PruefCodeGueltig(eintrag, 100));
        Assert.False(CliAktualisierer.PruefCodeGueltig(new ProgrammEintrag(), 100));
        Assert.Equal(2, CliAktualisierer.GeplanteZeilen(RustupCheck, eintrag).Count);
        Assert.NotEqual("", CliAktualisierer.FingerabdruckAus(null, Lauf(RustupCheck, 100), eintrag).Fingerabdruck);
        Assert.Equal("", CliAktualisierer.FingerabdruckAus(null, Lauf(RustupCheck, 100)).Fingerabdruck);
    }

    private const string SdkListe =
        "Installed packages:\n"
        + "  Path                 | Version | Description                  | Location\n"
        + "  emulator             | 36.4.10 | Android Emulator             | emulator\n"
        + "  platform-tools       | 37.0.0  | Android SDK Platform-Tools   | platform-tools\n"
        + "\nAvailable Packages:\n"
        + "  Path                 | Version | Description\n"
        + "  build-tools;37.0.0   | 37.0.0  | Android SDK Build-Tools 37\n"
        + "\nAvailable Updates:\n"
        + "  ID                       | Installed | Available\n"
        + "  -------                  | -------   | -------\n"
        + "  emulator                 | 36.4.10   | 37.2.12\n"
        + "  platform-tools           | 37.0.0    | 37.0.1\n"
        + "  platforms;android-CANARY | 9         | 15\n";

    private static ProgrammEintrag SdkEintrag() => Katalogeintrag("android-sdk");

    [Fact]
    public void Sdkmanager_zaehlt_nur_den_Abschnitt_der_Updates()
    {
        var geplant = CliAktualisierer.GeplanteZeilen(SdkListe, SdkEintrag());
        Assert.Equal(3, geplant.Count);
        Assert.StartsWith("emulator", geplant[0]);
    }

    [Fact]
    public void Sdkmanager_ohne_Updateabschnitt_hat_nichts_offen()
    {
        var ohne = SdkListe[..SdkListe.IndexOf("Available Updates", StringComparison.Ordinal)];
        Assert.Empty(CliAktualisierer.GeplanteZeilen(ohne, SdkEintrag()));
        Assert.Equal("nichts offen", CliAktualisierer.FingerabdruckAus(null, Lauf(ohne), SdkEintrag()).Fingerabdruck);
    }

    private static ProgrammEintrag Katalogeintrag(string id)
    {
        var (katalog, fehler) = Katalogdienst.Laden();
        Assert.Null(fehler);
        return katalog.Programme.Single(e => e.Id == id);
    }

    [Fact]
    public void Katalog_ist_gueltig_und_vollstaendig()
    {
        var (katalog, fehler) = Katalogdienst.Laden();
        Assert.Null(fehler);
        var alle = katalog.Programme;
        Assert.Equal(alle.Count, alle.Select(e => e.Id).Distinct().Count());

        foreach (var e in alle)
        {
            Assert.False(string.IsNullOrWhiteSpace(e.Name), e.Id);
            if (e.Art == "winget") Assert.False(string.IsNullOrWhiteSpace(e.WingetId), e.Id);
            if (e.Art == "cli") Assert.False(string.IsNullOrWhiteSpace(e.ExePfad), e.Id);
            if (e.PruefZeilenMuster is { } muster) _ = new Regex(muster);
        }

        var ps = alle.Single(e => e.Id == "powershell");
        Assert.Equal("PowerShell", ps.WingetName);
        Assert.True(ps.WingetUpdatePerInstall);
        Assert.Equal("wix", ps.WingetInstallerTyp);
        Assert.False(ps.IstPaketApp);
    }
}
