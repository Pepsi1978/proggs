using System.IO;
using System.Text.Json;

namespace UpdateZentrale.Services;

public sealed class ProgrammEinstellung
{
    public bool AlsAdministrator { get; set; }
    public bool Ausgeblendet { get; set; }
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
        catch
        {
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
        catch
        {
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
