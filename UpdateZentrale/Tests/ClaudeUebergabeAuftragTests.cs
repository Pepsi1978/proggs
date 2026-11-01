using UpdateZentrale.Models;
using UpdateZentrale.Services;
using UpdateZentrale.ViewModels;
using Xunit;

namespace UpdateZentrale.Tests;

public sealed class ClaudeUebergabeAuftragTests
{
    private static string Auftrag()
    {
        var eintrag = new ProgrammEintrag { Id = "tvo", Name = "TVO" };
        var bericht = new UpdateBericht { Name = "TVO", Ergebnis = LaufErgebnis.Fehlgeschlagen, Meldung = "kaputt" };
        return ClaudeUebergabe.AuftragText(new[] { (eintrag, bericht) });
    }

    [Fact]
    public void Auftrag_nennt_alle_Schritte_in_der_vorgegebenen_Reihenfolge()
    {
        var text = Auftrag();
        var stellen = new[] { "1. Finde die Ursache", "2. Repariere dauerhaft", "3. Halte Ursache", "4. Führe danach das Update",
                              "5. Führe danach ohne Rückfrage das Standard-Code-Review", "6. Baue als allerletzten Schritt die UpdateZentrale neu" }
            .Select(s => text.IndexOf(s, StringComparison.Ordinal)).ToList();

        Assert.DoesNotContain(-1, stellen);
        Assert.Equal(stellen.OrderBy(s => s), stellen);
    }

    [Fact]
    public void Auftrag_verlangt_das_Review_und_nennt_das_Update_Skript()
    {
        var text = Auftrag();

        Assert.Contains("REVIEW: ja", text);
        Assert.Contains("update-zentrale.ps1", text);
    }

    [Fact]
    public void Auftrag_enthaelt_die_Leitplanken_fuer_jede_Sitzung()
    {
        var text = Auftrag();

        Assert.Contains("ohne Administratorrechte", text);
        Assert.Contains("nicht ohne Rückfrage", text);
        Assert.Contains("no-answer", text);
        Assert.Contains("Versionsnummer direkt vor dem Bump", text);
    }

    [Fact]
    public void Programm_das_die_Sitzung_beenden_kann_kommt_erst_nach_Review_und_Neubau()
    {
        var eintrag = new ProgrammEintrag { Id = "git", Name = "Git", CliRisiko = "Ersetzt git.exe." };
        var text = ClaudeUebergabe.AuftragText(new[] { (eintrag, new UpdateBericht { Name = "Git", Meldung = "x" }) });

        Assert.Contains("Ersetzt git.exe.", text);
        Assert.Contains("erst nach den Schritten 5 und 6", text);
    }

    [Fact]
    public void Scheitert_die_UpdateZentrale_selbst_erledigt_Schritt_6_ihr_Update()
    {
        var eintrag = new ProgrammEintrag { Id = Sammelplan.SelbstId, Name = "UpdateZentrale" };
        var text = ClaudeUebergabe.AuftragText(new[] { (eintrag, new UpdateBericht { Name = "UpdateZentrale", Meldung = "x" }) });

        Assert.Contains("Schritt 4 entfällt für sie", text);
    }
}
