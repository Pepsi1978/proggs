using UpdateZentrale.Models;
using UpdateZentrale.Services;
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
    public void Sperrfrist_haelt_einen_erneuten_Fehlschlag_nicht_stundenlang_zurueck()
        => Assert.True(ClaudeUebergabe.Sperrfrist <= TimeSpan.FromHours(1));
}
