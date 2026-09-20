using System.Text.Json.Serialization;

namespace UpdateZentrale.Models;

/// <summary>
/// One catalog entry from programs.json. Adding a program means adding an object here -- never
/// touching code -- which is why every provider-specific field lives on this single type instead
/// of in a subclass hierarchy.
/// </summary>
public sealed class ProgrammEintrag
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Gruppe { get; set; } = "Weitere";
    public string Beschreibung { get; set; } = "";

    /// <summary>winget | msstore | cli | reposkript</summary>
    public string Art { get; set; } = "winget";

    public string? WingetId { get; set; }
    public string? AppxName { get; set; }
    public string? PackageFamilyName { get; set; }

    /// <summary>Application id inside the MSIX package; almost always "App".</summary>
    public string? AppxAnwendungsId { get; set; }

    /// <summary>Store product id (e.g. 9PLM9XGG6VKS), used for the silent msstore upgrade.</summary>
    public string? StoreProduktId { get; set; }

    /// <summary>True when this entry is a packaged (MSIX) app rather than a plain exe.</summary>
    [JsonIgnore]
    public bool IstPaketApp => !string.IsNullOrWhiteSpace(PackageFamilyName);

    /// <summary>
    /// Publisher hash from the package family name. Every install folder of that package under
    /// WindowsApps carries it, which makes it a reliable way to tell a packaged app's processes
    /// apart from a same-named CLI.
    /// </summary>
    [JsonIgnore]
    public string? PaketKennung
    {
        get
        {
            if (string.IsNullOrWhiteSpace(PackageFamilyName)) return null;
            var teil = PackageFamilyName.Split('_').LastOrDefault();
            return string.IsNullOrWhiteSpace(teil) ? null : teil;
        }
    }

    public string ExePfad { get; set; } = "";
    public string? VersionsArgumente { get; set; }
    public string? UpdateArgumente { get; set; }
    public string? PruefArgumente { get; set; }
    public string? NpmPaket { get; set; }
    public string? StartArgumente { get; set; }

    /// <summary>Check output lists updates as "alt -> neu"; counting arrows tells us how many.</summary>
    public bool PfeilZaehlen { get; set; }

    public int ZeitlimitMinuten { get; set; } = 20;

    public string? RepoOrdner { get; set; }
    public string? Skript { get; set; }
    public string? SkriptArgumente { get; set; }
    public string? StatusPraefix { get; set; }
    public string? ProjektDatei { get; set; }
    public int DialogWartezeitSekunden { get; set; } = 300;

    public List<string> Prozesse { get; set; } = new();
    public List<string> HelferProzesse { get; set; } = new();
    public bool BeendenVorUpdate { get; set; }
    public bool NeuStartenNachUpdate { get; set; }

    public string? Hinweis { get; set; }
    public string Akzent { get; set; } = "#7C5CFF";

    /// <summary>Default for the admin toggle; the user override in settings.json wins.</summary>
    public bool AdminStandard { get; set; }

    [JsonIgnore]
    public IEnumerable<string> AlleProzesse => Prozesse.Concat(HelferProzesse);
}

public sealed class ProgrammKatalog
{
    public int Version { get; set; } = 1;
    public List<ProgrammEintrag> Programme { get; set; } = new();
}
