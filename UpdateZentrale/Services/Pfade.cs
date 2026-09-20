using System.IO;

namespace UpdateZentrale.Services;

/// <summary>Single place that knows where things live, so no path literal is duplicated.</summary>
public static class Pfade
{
    public static string RepoWurzel { get; } = Ermitteln();

    public static string KatalogDatei => Path.Combine(RepoWurzel, "UpdateZentrale", "programs.json");

    public static string BenutzerOrdner { get; } = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "UpdateZentrale");

    public static string EinstellungsDatei => Path.Combine(BenutzerOrdner, "settings.json");

    public static string Winget { get; } = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Microsoft", "WindowsApps", "winget.exe");

    /// <summary>Expands %VARS% and returns an empty string for blank input.</summary>
    public static string Aufloesen(string? pfad)
    {
        if (string.IsNullOrWhiteSpace(pfad)) return "";
        var erweitert = Environment.ExpandEnvironmentVariables(pfad);
        try
        {
            // The catalog uses forward slashes (no JSON escaping); Windows APIs and the registry
            // compatibility key both want the canonical backslash form.
            return Path.GetFullPath(erweitert);
        }
        catch
        {
            return erweitert.Replace('/', Path.DirectorySeparatorChar);
        }
    }

    /// <summary>
    /// The app runs out of bin\Release\... inside the repo, so walk up until the repo marker is
    /// found. Falls back to ~/proggs, which is where the repo lives on this machine.
    /// </summary>
    private static string Ermitteln()
    {
        var dir = AppContext.BaseDirectory;
        for (var i = 0; i < 8 && !string.IsNullOrEmpty(dir); i++)
        {
            if (Directory.Exists(Path.Combine(dir, "UpdateZentrale"))
                && File.Exists(Path.Combine(dir, "UpdateZentrale", "programs.json")))
            {
                return dir;
            }
            dir = Path.GetDirectoryName(dir.TrimEnd(Path.DirectorySeparatorChar)) ?? "";
        }

        return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "proggs");
    }
}
