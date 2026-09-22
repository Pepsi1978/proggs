using System.IO;
using System.Text.Json;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>Loads programs.json from the repo folder at runtime -- new programs need no rebuild.</summary>
public static class Katalogdienst
{
    private static readonly JsonSerializerOptions Optionen = new()
    {
        PropertyNameCaseInsensitive = true,
        ReadCommentHandling = JsonCommentHandling.Skip,
        AllowTrailingCommas = true
    };

    public static (ProgrammKatalog Katalog, string? Fehler) Laden()
    {
        try
        {
            var datei = Pfade.KatalogDatei;
            if (!File.Exists(datei))
            {
                // Fall back to the copy next to the exe so a stand-alone run still works.
                datei = Path.Combine(AppContext.BaseDirectory, "programs.json");
            }
            if (!File.Exists(datei))
            {
                return (new ProgrammKatalog(), $"Katalog nicht gefunden: {Pfade.KatalogDatei}");
            }

            var katalog = JsonSerializer.Deserialize<ProgrammKatalog>(File.ReadAllText(datei), Optionen);
            if (katalog is null || katalog.Programme.Count == 0)
            {
                return (new ProgrammKatalog(), $"Katalog ist leer: {datei}");
            }

            // Einträge, die es nur auf manchen Rechnern gibt, verschwinden hier still: derselbe
            // Katalog gilt für alle Geräte, zeigt aber nur, was hier wirklich installiert ist.
            katalog.Programme.RemoveAll(e => e.AusblendenWennFehlt && !e.AufDiesemRechnerVorhanden);

            return (katalog, null);
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "katalog", "programs.json lesen");
            return (new ProgrammKatalog(), $"Katalog konnte nicht gelesen werden: {ex.Message}");
        }
    }
}
