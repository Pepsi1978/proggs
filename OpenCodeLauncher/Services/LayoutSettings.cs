using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;

namespace OpenCodeLauncher.Services;

public sealed class LayoutSettings
{
    private const double DefaultModelPaneWidth = 300;
    private const double MinModelPaneWidth = 240;
    private const double MaxModelPaneWidth = 760;

    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenCodeLauncher");
    private static readonly string FilePath = Path.Combine(Dir, "layout.json");

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver()
    };

    public double ModelPaneWidth { get; set; } = DefaultModelPaneWidth;

    /// <summary>Gewaehltes Design: "Dark" oder "Light". Wird beim App-Start angewendet.</summary>
    public string Theme { get; set; } = "Dark";

    public static LayoutSettings Load()
    {
        try
        {
            if (!File.Exists(FilePath)) return new LayoutSettings();
            var settings = JsonSerializer.Deserialize<LayoutSettings>(File.ReadAllText(FilePath), JsonOpts);
            if (settings == null) return new LayoutSettings();
            settings.ModelPaneWidth = Clamp(settings.ModelPaneWidth);
            return settings;
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("LayoutSettings", "Load", $"Layout-Settings ignoriert, Defaults: {ex.Message}");
            return new LayoutSettings();
        }
    }

    public void Save()
    {
        try
        {
            ModelPaneWidth = Clamp(ModelPaneWidth);
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(this, JsonOpts));
        }
        catch (Exception ex)
        {
            Logger.Instance.Error("LayoutSettings", "Save", ex.Message);
        }
    }

    private static double Clamp(double value)
    {
        if (double.IsNaN(value) || double.IsInfinity(value)) return DefaultModelPaneWidth;
        return Math.Clamp(value, MinModelPaneWidth, MaxModelPaneWidth);
    }
}
