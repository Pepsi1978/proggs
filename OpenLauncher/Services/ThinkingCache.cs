using System.IO;
using System.Text.Json;

namespace OpenLauncher.Services;

public sealed class ThinkingCache
{
    public sealed record Entry(List<string> Levels, string? Selected);
    private readonly string _path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenLauncher", "thinking-levels.json");
    private Dictionary<string, Entry> _entries = new();

    public ThinkingCache()
    {
        try
        {
            if (File.Exists(_path))
                _entries = JsonSerializer.Deserialize<Dictionary<string, Entry>>(File.ReadAllText(_path)) ?? new();
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ThinkingCache", "Load", ex.Message);
        }
    }

    public Entry? Find(string model) => _entries.TryGetValue(model, out var entry) &&
        entry?.Levels != null && entry.Levels.All(level => !string.IsNullOrWhiteSpace(level)) ? entry : null;

    public void Save(string model, IEnumerable<string> levels, string? selected)
    {
        _entries[model] = new Entry(levels.ToList(), selected);
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(_path)!);
            var temp = _path + "." + Guid.NewGuid().ToString("N") + ".tmp";
            File.WriteAllText(temp, JsonSerializer.Serialize(_entries));
            File.Move(temp, _path, overwrite: true);
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ThinkingCache", "Save", ex.Message);
        }
    }
}
