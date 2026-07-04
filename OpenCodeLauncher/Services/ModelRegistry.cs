using System.IO;
using System.Text.Json;
using OpenCodeLauncher.Models;

namespace OpenCodeLauncher.Services;

/// <summary>
/// Persistente Liste der gepflegten Modelle (JSON in %AppData%/OpenCodeLauncher).
/// Beim ersten Start werden die 7 Default-Modelle angelegt (Slugs verifiziert am
/// 04.07.2026 gegen GET /api/v1/models). Reihenfolge per Drag&Drop änderbar,
/// neue Modelle per AddModel, entfernen per RemoveModel.
/// </summary>
public sealed class ModelRegistry
{
    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenCodeLauncher");
    private static readonly string FilePath = Path.Combine(Dir, "models.json");

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping
    };

    public List<ModelEntry> Models { get; private set; } = new();

    public static ModelRegistry Load()
    {
        var reg = new ModelRegistry();
        try
        {
            if (File.Exists(FilePath))
            {
                var json = File.ReadAllText(FilePath);
                var list = JsonSerializer.Deserialize<List<ModelEntry>>(json, JsonOpts);
                if (list != null && list.Count > 0)
                {
                    reg.Models = list;
                    Logger.Instance.Info("ModelRegistry", "Load", $"{list.Count} Modelle geladen", new { FilePath });
                    return reg;
                }
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ModelRegistry", "Load", $"Laden fehlgeschlagen, Defaults: {ex.Message}");
        }

        reg.Models = CreateDefaults();
        reg.Save();
        return reg;
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(Models, JsonOpts));
        }
        catch (Exception ex)
        {
            Logger.Instance.Error("ModelRegistry", "Save", ex.Message);
        }
    }

    public bool AddModel(string slug, string displayName)
    {
        slug = slug.Trim().ToLowerInvariant();
        if (Models.Any(m => string.Equals(m.Slug, slug, StringComparison.OrdinalIgnoreCase)))
        {
            Logger.Instance.Warn("ModelRegistry", "AddModel", $"Slug existiert bereits: {slug}");
            return false;
        }
        Models.Add(new ModelEntry { Slug = slug, DisplayName = string.IsNullOrWhiteSpace(displayName) ? slug : displayName });
        Save();
        Logger.Instance.Info("ModelRegistry", "AddModel", $"hinzugefügt: {slug}", new { displayName });
        return true;
    }

    public void RemoveAt(int index)
    {
        if (index < 0 || index >= Models.Count) return;
        var m = Models[index];
        Models.RemoveAt(index);
        Save();
        Logger.Instance.Info("ModelRegistry", "RemoveAt", $"entfernt: {m.Slug}");
    }

    public void Move(int from, int to)
    {
        if (from < 0 || from >= Models.Count || to < 0 || to >= Models.Count || from == to) return;
        var item = Models[from];
        Models.RemoveAt(from);
        Models.Insert(to, item);
        Save();
        Logger.Instance.Info("ModelRegistry", "Move", $"{from} -> {to}");
    }

    private static List<ModelEntry> CreateDefaults() => new()
    {
        new() { Slug = "z-ai/glm-5.2",             DisplayName = "GLM 5.2" },
        new() { Slug = "minimax/minimax-m3",       DisplayName = "MiniMax M3" },
        new() { Slug = "qwen/qwen3.7-max",         DisplayName = "Qwen 3.7 Max" },
        new() { Slug = "xiaomi/mimo-v2.5-pro",     DisplayName = "MiMo V2.5 Pro" },
        new() { Slug = "deepseek/deepseek-v4-pro", DisplayName = "DeepSeek V4 Pro" },
        new() { Slug = "deepseek/deepseek-v4-flash", DisplayName = "DeepSeek V4 Flash" },
        new() { Slug = "xiaomi/mimo-v2.5",         DisplayName = "MiMo V2.5" },
    };
}
