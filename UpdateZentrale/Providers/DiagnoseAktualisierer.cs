using System.Diagnostics;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Decorator around every provider: begin, end, duration, resulting state and any exception of
/// each check, update and fingerprint call. Applied centrally when a card is created, so a new
/// provider class or a new catalog program gets this without a single line of its own.
/// </summary>
public sealed class DiagnoseAktualisierer : IAktualisierer
{
    private readonly IAktualisierer _innen;

    private DiagnoseAktualisierer(IAktualisierer innen) => _innen = innen;

    /// <summary>Idempotent: an already wrapped provider is returned as it is.</summary>
    public static IAktualisierer Umhuellen(IAktualisierer anbieter)
        => anbieter as DiagnoseAktualisierer ?? new DiagnoseAktualisierer(anbieter);

    public IAktualisierer Innen => _innen;

    public string Art => _innen.Art;

    public Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
        => Messen("pruefen", eintrag, () => _innen.PruefenAsync(eintrag, protokoll, abbruch));

    public Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
        => Messen("aktualisieren", eintrag, () => _innen.AktualisierenAsync(eintrag, protokoll, abbruch));

    public async Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
    {
        var uhr = Stopwatch.StartNew();
        try
        {
            var wert = await _innen.FingerabdruckAsync(eintrag, abbruch);
            Diagnose.Ereignis(string.IsNullOrWhiteSpace(wert) ? Schwere.Warnung : Schwere.Debug, "provider", "provider.fingerabdruck",
                string.IsNullOrWhiteSpace(wert) ? "Fingerabdruck nicht lesbar." : "Fingerabdruck gelesen.",
                daten: Daten(eintrag, uhr, ("wert", Bereinigung.Sicher(wert, 300))));
            return wert;
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "provider", "Fingerabdruck " + eintrag.Id);
            throw;
        }
    }

    private async Task<PruefErgebnis> Messen(string aufruf, ProgrammEintrag eintrag, Func<Task<PruefErgebnis>> arbeit)
    {
        var uhr = Stopwatch.StartNew();
        Diagnose.Ereignis(Schwere.Debug, "provider", "provider." + aufruf + ".beginn", aufruf + " über " + Art, aufruf,
            Daten(eintrag, null));
        try
        {
            var ergebnis = await arbeit();
            var schwere = ergebnis.Zustand switch
            {
                UpdateZustand.Fehler => Schwere.Fehler,
                UpdateZustand.Unbekannt or UpdateZustand.Abgebrochen => Schwere.Warnung,
                _ => Schwere.Info
            };
            Diagnose.Ereignis(schwere, "provider", "provider." + aufruf + ".ende", ergebnis.Meldung, aufruf,
                Daten(eintrag, uhr, ("zustand", ergebnis.Zustand.ToString()), ("installiert", ergebnis.InstallierteVersion),
                    ("verfuegbar", ergebnis.VerfuegbareVersion), ("erstNachNeustart", ergebnis.ErstNachNeustart)));
            return ergebnis;
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "provider", aufruf + " " + eintrag.Id);
            throw;
        }
    }

    private Dictionary<string, object?> Daten(ProgrammEintrag eintrag, Stopwatch? uhr, params (string Schluessel, object? Wert)[] mehr)
    {
        var daten = new Dictionary<string, object?> { ["providerArt"] = Art, ["programm"] = eintrag.Id };
        if (uhr is not null) daten["dauerMs"] = (long)uhr.Elapsed.TotalMilliseconds;
        foreach (var (schluessel, wert) in mehr) daten[schluessel] = wert;
        return daten;
    }
}
