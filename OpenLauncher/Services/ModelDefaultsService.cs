using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using OpenLauncher.Models;

namespace OpenLauncher.Services;

/// <summary>
/// Speichert je Modell eine von Hand festgelegte Startauswahl (Profil, Modus, Effort/Thinking).
/// Der Ablageort ist bewusst derselbe wie bei der Modell-Liste (siehe ModelRegistry): das Repo
/// ~/proggs/OpenLauncher/. Der Inhalt ist modellbezogene Konfiguration ohne Geheimnisse und
/// gehoert damit zum versionierten Bestand — nach Commit und Push steht derselbe Standard auf
/// jedem Rechner bereit.
/// Schluessel ist die provider-qualifizierte Modell-ID (ModelEntry.ModelString), damit gleich
/// benannte Slugs verschiedener Provider nicht kollidieren.
/// </summary>
public sealed class ModelDefaultsService
{
    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "proggs", "OpenLauncher");
    private static readonly string FilePath = Path.Combine(Dir, "model-defaults.json");

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver()
    };

    private readonly Dictionary<string, ModelDefaultEntry> _defaults = new(StringComparer.OrdinalIgnoreCase);

    public ModelDefaultsService()
    {
        try
        {
            if (!File.Exists(FilePath)) return;
            var parsed = JsonSerializer.Deserialize<Dictionary<string, ModelDefaultEntry>>(File.ReadAllText(FilePath), JsonOpts);
            if (parsed == null) return;
            foreach (var (key, value) in parsed)
            {
                if (string.IsNullOrWhiteSpace(key) || value == null) continue;
                _defaults[key] = value;
            }
            Logger.Instance.Info("ModelDefaultsService", "Load", $"{_defaults.Count} Modell-Standard(s) geladen", new { FilePath });
        }
        catch (Exception ex)
        {
            // Eine unlesbare Datei darf den Start nicht verhindern: dann gilt eben kein Standard.
            Logger.Instance.Warn("ModelDefaultsService", "Load", $"Modell-Standards ignoriert: {ex.Message}", new { FilePath });
            _defaults.Clear();
        }
    }

    /// <summary>Gespeicherter Standard des Modells oder null, wenn keiner hinterlegt ist.</summary>
    public ModelDefaultEntry? Find(string modelKey) =>
        string.IsNullOrWhiteSpace(modelKey) ? null : _defaults.GetValueOrDefault(modelKey);

    public void Save(string modelKey, ModelDefaultEntry entry)
    {
        if (string.IsNullOrWhiteSpace(modelKey)) return;
        _defaults[modelKey] = entry;
        Persist();
    }

    public bool Remove(string modelKey)
    {
        if (!_defaults.Remove(modelKey)) return false;
        Persist();
        return true;
    }

    private void Persist()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            // Atomar schreiben (Temp + Move) wie bei models.json: ein Absturz mitten im Schreiben
            // darf die Datei nicht abgeschnitten zuruecklassen.
            var tmp = FilePath + ".tmp";
            File.WriteAllText(tmp, JsonSerializer.Serialize(_defaults, JsonOpts));
            File.Move(tmp, FilePath, overwrite: true);
        }
        catch (Exception ex)
        {
            Logger.Instance.Error("ModelDefaultsService", "Persist", ex.Message);
        }
    }
}
