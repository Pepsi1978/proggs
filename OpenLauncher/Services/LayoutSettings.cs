using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Text.Json.Serialization.Metadata;

namespace OpenLauncher.Services;

public sealed class LayoutSettings
{
    private const double DefaultModelPaneWidth = 300;
    private const double MinModelPaneWidth = 240;
    private const double MaxModelPaneWidth = 760;
    private const double DefaultEffortPaneWidth = 210;
    private const double MinEffortPaneWidth = 150;
    private const double MaxEffortPaneWidth = 400;
    private const double MinProviderRowShare = 0.2;
    private const double MaxProviderRowShare = 0.8;
    private const double DefaultWindowWidth = 1360;
    private const double DefaultWindowHeight = 860;
    private const double MinWindowWidth = 960;
    private const double MinWindowHeight = 600;

    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenLauncher");
    private static readonly string FilePath = Path.Combine(Dir, "layout.json");

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        // NaN als "noch nie gespeichert"-Sentinel muss serialisierbar sein.
        NumberHandling = JsonNumberHandling.AllowNamedFloatingPointLiterals,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver()
    };

    public double ModelPaneWidth { get; set; } = DefaultModelPaneWidth;

    /// <summary>Breite der Effort-Spalte in Pixeln, per senkrechtem Griff verschiebbar.</summary>
    public double EffortPaneWidth { get; set; } = DefaultEffortPaneWidth;

    /// <summary>
    /// Anteil der Provider-Karte an der Hoehe von Provider + Profil (0..1), per waagerechtem Griff
    /// verschiebbar. NaN = noch nie verschoben, dann bleibt es beim gleichmaessigen Sternverhaeltnis.
    /// Als Anteil statt in Pixeln gespeichert, damit die Aufteilung beim Groessenaendern des Fensters
    /// erhalten bleibt.
    /// </summary>
    public double ProviderRowShare { get; set; } = double.NaN;

    // NaN = noch nie gespeichert. Ein numerischer Sentinel wie -1 kollidiert mit echten negativen
    // Fensterkoordinaten (Monitor links/oberhalb des Primärmonitors) und würde deren Wiederherstellung
    // verhindern.
    public double WindowLeft { get; set; } = double.NaN;
    public double WindowTop { get; set; } = double.NaN;
    public double WindowWidth { get; set; } = DefaultWindowWidth;
    public double WindowHeight { get; set; } = DefaultWindowHeight;
    public string WindowState { get; set; } = "Normal";

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
            settings.EffortPaneWidth = ClampEffort(settings.EffortPaneWidth);
            settings.ProviderRowShare = ClampShare(settings.ProviderRowShare);
            settings.NormalizeWindowBounds();
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
            EffortPaneWidth = ClampEffort(EffortPaneWidth);
            ProviderRowShare = ClampShare(ProviderRowShare);
            NormalizeWindowBounds();
            Directory.CreateDirectory(Dir);
            // Atomar schreiben (Temp + Move) — konsistent mit dem übrigen Persistenz-Code und robust
            // gegen einen Absturz mitten im (häufigen, debounce-getriggerten) Schreiben.
            var tmp = FilePath + ".tmp";
            File.WriteAllText(tmp, JsonSerializer.Serialize(this, JsonOpts));
            File.Move(tmp, FilePath, overwrite: true);
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

    private static double ClampEffort(double value)
    {
        if (double.IsNaN(value) || double.IsInfinity(value)) return DefaultEffortPaneWidth;
        return Math.Clamp(value, MinEffortPaneWidth, MaxEffortPaneWidth);
    }

    // NaN bleibt NaN: das ist der "noch nie verschoben"-Sentinel und darf nicht auf einen
    // Zahlenwert gezogen werden, sonst faellt das Standardlayout beim ersten Start weg.
    private static double ClampShare(double value)
    {
        if (double.IsNaN(value)) return double.NaN;
        if (double.IsInfinity(value)) return double.NaN;
        return Math.Clamp(value, MinProviderRowShare, MaxProviderRowShare);
    }

    private void NormalizeWindowBounds()
    {
        if (double.IsNaN(WindowWidth) || double.IsInfinity(WindowWidth)) WindowWidth = DefaultWindowWidth;
        if (double.IsNaN(WindowHeight) || double.IsInfinity(WindowHeight)) WindowHeight = DefaultWindowHeight;
        WindowWidth = Math.Max(WindowWidth, MinWindowWidth);
        WindowHeight = Math.Max(WindowHeight, MinWindowHeight);
        if (WindowState != "Maximized") WindowState = "Normal";
    }
}
