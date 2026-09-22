using System.IO;
using System.Text.Json;

namespace UpdateZentrale.Services;

public sealed class ProgrammEinstellung
{
    public bool AlsAdministrator { get; set; }
    public bool Ausgeblendet { get; set; }

    /// <summary>
    /// The Run entry that was replaced by a scheduled task. Kept so switching the elevated
    /// autostart off can put the original entry back exactly as it was.
    /// </summary>
    public string? GesicherterRunName { get; set; }

    public string? GesicherterRunWert { get; set; }

    /// <summary>
    /// Bis hierher wurden Fehlermeldungen weggeklickt. Das rote Band einer Karte bleibt so lange
    /// stehen, bis es quittiert wird -- und ein spaeterer Fehler taucht wieder auf, weil sein
    /// Zeitstempel jenseits dieser Marke liegt. Das Protokoll bleibt davon unberuehrt: quittiert
    /// wird nur die Anzeige, nichts wird geloescht.
    /// </summary>
    public DateTime? FehlerQuittiertBis { get; set; }
}

/// <summary>
/// User state lives outside the repo (%LOCALAPPDATA%\UpdateZentrale\settings.json) so the catalog
/// stays a clean, committable file even after toggling things in the UI.
/// </summary>
public sealed class Einstellungen
{
    private static readonly JsonSerializerOptions Optionen = new()
    {
        WriteIndented = true,
        PropertyNameCaseInsensitive = true
    };

    public Dictionary<string, ProgrammEinstellung> Programme { get; set; } = new();

    /// <summary>Light mode; dark stays the default because the app is usually opened briefly.</summary>
    public bool HellModus { get; set; }

    /// <summary>
    /// Remembers that the "always run elevated" flag was applied once, so switching it off stays
    /// switched off instead of being re-applied on the next start.
    /// </summary>
    public bool AdminStartGesetzt { get; set; }

    public static Einstellungen Laden()
    {
        try
        {
            if (File.Exists(Pfade.EinstellungsDatei))
            {
                var text = File.ReadAllText(Pfade.EinstellungsDatei);
                return JsonSerializer.Deserialize<Einstellungen>(text, Optionen) ?? new Einstellungen();
            }
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "einstellungen", Schwere.Warnung);
            // A corrupt settings file must never block the app; defaults are always usable.
        }
        return new Einstellungen();
    }

    public void Speichern()
    {
        try
        {
            Directory.CreateDirectory(Pfade.BenutzerOrdner);
            File.WriteAllText(Pfade.EinstellungsDatei, JsonSerializer.Serialize(this, Optionen));
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "einstellungen", Schwere.Warnung);
        }
    }

    public ProgrammEinstellung Fuer(string id)
    {
        if (!Programme.TryGetValue(id, out var wert))
        {
            wert = new ProgrammEinstellung();
            Programme[id] = wert;
        }
        return wert;
    }
}
