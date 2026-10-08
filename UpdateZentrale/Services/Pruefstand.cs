using System.IO;
using System.Text.Json;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

public sealed class PruefstandEintrag
{
    public UpdateZustand Zustand { get; set; }
    public string Installiert { get; set; } = "";
    public string Verfuegbar { get; set; } = "";
    public string Meldung { get; set; } = "";
    public DateTime Zeit { get; set; }
}

/// <summary>
/// The result of the last check per program, kept across starts
/// (%LOCALAPPDATA%\UpdateZentrale\pruefstand.json). The check that runs hidden at Windows logon
/// writes it; the window opened later shows it at once instead of checking everything again.
///
/// Only clear answers are kept. A failed or unknown check is never stored, so the next start
/// checks that program again instead of showing an old error as the current state.
/// </summary>
public static class Pruefstand
{
    private static readonly JsonSerializerOptions Optionen = new() { WriteIndented = true, PropertyNameCaseInsensitive = true };
    private static readonly object Sperre = new();
    private static Dictionary<string, PruefstandEintrag>? _stand;

    private static string Datei => Path.Combine(Pfade.BenutzerOrdner, "pruefstand.json");

    public static PruefstandEintrag? Lesen(string id)
    {
        lock (Sperre) return Stand().TryGetValue(id, out var eintrag) ? eintrag : null;
    }

    public static void Merken(string id, PruefErgebnis ergebnis)
    {
        if (ergebnis.Zustand is not (UpdateZustand.Aktuell or UpdateZustand.UpdateVerfuegbar or UpdateZustand.NichtInstalliert))
        {
            Vergessen(id);
            return;
        }

        lock (Sperre)
        {
            Stand()[id] = new PruefstandEintrag
            {
                Zustand = ergebnis.Zustand,
                Installiert = ergebnis.InstallierteVersion,
                Verfuegbar = ergebnis.VerfuegbareVersion,
                Meldung = ergebnis.Meldung,
                Zeit = DateTime.Now
            };
            Speichern();
        }
    }

    /// <summary>After an update the stored answer is void -- the next start checks that program again.</summary>
    public static void Vergessen(string id)
    {
        lock (Sperre)
        {
            if (Stand().Remove(id)) Speichern();
        }
    }

    private static Dictionary<string, PruefstandEintrag> Stand()
    {
        if (_stand is not null) return _stand;
        try
        {
            if (File.Exists(Datei))
                _stand = JsonSerializer.Deserialize<Dictionary<string, PruefstandEintrag>>(File.ReadAllText(Datei), Optionen);
        }
        catch (Exception diagAusnahme)
        {
            // A broken file only costs one fresh check.
            Diagnose.Gefangen(diagAusnahme, "pruefstand", Schwere.Warnung);
        }
        return _stand ??= new Dictionary<string, PruefstandEintrag>();
    }

    private static void Speichern()
    {
        try
        {
            Directory.CreateDirectory(Pfade.BenutzerOrdner);
            var vorlaeufig = Datei + ".tmp";
            File.WriteAllText(vorlaeufig, JsonSerializer.Serialize(_stand, Optionen));
            File.Move(vorlaeufig, Datei, overwrite: true);
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "pruefstand", Schwere.Warnung);
        }
    }
}
