using System.IO;
using System.Text.Json;

namespace OpenLauncher.Services;

public enum ResearchMode { Disabled, Fallback, Manual, Periodic }

public sealed record ResearchSettings
{
    public ResearchMode Mode { get; init; } = ResearchMode.Fallback;
    public string Model { get; init; } = "";
    public string Effort { get; init; } = "";
    public int PeriodHours { get; init; } = 24;
}

public static class ResearchSettingsService
{
    public static string DataDirectory { get; } = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "OpenLauncher", "Research");
    private static readonly object Gate = new();
    private static readonly string SettingsPath = Path.Combine(DataDirectory, "settings.json");

    public static ResearchSettings Load()
    {
        lock (Gate)
        {
            try
            {
                if (!File.Exists(SettingsPath)) return new();
                var settings = JsonSerializer.Deserialize<ResearchSettings>(File.ReadAllText(SettingsPath));
                if (settings == null || !Enum.IsDefined(settings.Mode) || settings.PeriodHours is < 1 or > 720)
                    return new() { Mode = ResearchMode.Disabled };
                return settings;
            }
            catch (Exception ex) when (ex is IOException or JsonException or UnauthorizedAccessException)
            {
                Logger.Instance.Warn(nameof(ResearchSettingsService), nameof(Load), "Recherche-Einstellungen nicht lesbar; KI deaktiviert.");
                return new() { Mode = ResearchMode.Disabled };
            }
        }
    }

    public static void Save(ResearchSettings settings)
    {
        if (!Enum.IsDefined(settings.Mode) || settings.PeriodHours is < 1 or > 720)
            throw new ArgumentException("Ungültige Recherche-Einstellungen.");
        lock (Gate)
        {
            Directory.CreateDirectory(DataDirectory);
            var temporary = SettingsPath + ".tmp";
            File.WriteAllText(temporary, JsonSerializer.Serialize(settings));
            File.Move(temporary, SettingsPath, true);
        }
    }
}
