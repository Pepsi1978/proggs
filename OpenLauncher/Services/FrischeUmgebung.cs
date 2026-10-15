using System.IO;

namespace OpenLauncher.Services;

/// <summary>
/// Der OpenLauncher (und die Jarvis-PC-Brücke, die ihn aufruft) laufen oft stundenlang und behalten die
/// Umgebung von ihrem Start. Ein JDK-Update legt aber einen neuen Ordner an (jdk-21.0.x.y-hotspot),
/// stellt JAVA_HOME in Windows um und löscht den alten Ordner. Jede danach gestartete CLI-Sitzung erbte
/// trotzdem den toten Pfad, und gradlew brach mit "JAVA_HOME is set to an invalid directory" ab.
/// Vor jedem Start werden JAVA_HOME und PATH deshalb frisch aus Windows gelesen, wie bei einem neu
/// gestarteten Programm (gleiches Muster wie in der UpdateZentrale). Zeigt auch der gespeicherte Wert ins
/// Leere, nimmt der Launcher das neueste vorhandene JDK.
/// </summary>
internal static class FrischeUmgebung
{
    public static void Auffrischen()
    {
        try
        {
            var maschine = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Machine);
            var benutzer = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.User);
            if (!string.IsNullOrWhiteSpace(maschine))
            {
                var pfad = string.IsNullOrWhiteSpace(benutzer) ? maschine : maschine.TrimEnd(';') + ";" + benutzer;
                Environment.SetEnvironmentVariable("PATH", Environment.ExpandEnvironmentVariables(pfad));
            }

            var javaHome = Environment.GetEnvironmentVariable("JAVA_HOME", EnvironmentVariableTarget.User)
                           ?? Environment.GetEnvironmentVariable("JAVA_HOME", EnvironmentVariableTarget.Machine);
            if (!IstJdk(javaHome))
            {
                var ersatz = NeuestesJdk();
                if (ersatz != null)
                    Logger.Instance.Warn("FrischeUmgebung", "Auffrischen", "JAVA_HOME zeigt ins Leere, nehme neuestes JDK",
                        new { gespeichert = javaHome, ersatz });
                javaHome = ersatz;
            }
            Environment.SetEnvironmentVariable("JAVA_HOME", javaHome);
        }
        catch (Exception ex)
        {
            // Registry nicht lesbar: Die geerbte Umgebung ist das Beste, was bleibt.
            Logger.Instance.Warn("FrischeUmgebung", "Auffrischen", "Umgebung nicht aufgefrischt", new { ex.Message });
        }
    }

    private static bool IstJdk(string? ordner) =>
        !string.IsNullOrWhiteSpace(ordner) && File.Exists(Path.Combine(ordner.Trim().Trim('"'), "bin", "java.exe"));

    /// <summary>Neuestes JDK aus Adoptium, sonst das von Android Studio mitgebrachte.</summary>
    private static string? NeuestesJdk()
    {
        var programme = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
        var adoptium = Path.Combine(programme, "Eclipse Adoptium");
        if (Directory.Exists(adoptium))
        {
            var neuestes = Directory.GetDirectories(adoptium, "jdk-*")
                .Where(IstJdk)
                .OrderByDescending(d => Version.TryParse(new string(Path.GetFileName(d).Skip(4).TakeWhile(c => char.IsDigit(c) || c == '.').ToArray()).TrimEnd('.'), out var v) ? v : new Version(0, 0))
                .FirstOrDefault();
            if (neuestes != null) return neuestes;
        }
        var jbr = Path.Combine(programme, "Android", "Android Studio", "jbr");
        return IstJdk(jbr) ? jbr : null;
    }
}
