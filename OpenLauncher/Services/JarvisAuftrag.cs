using System.IO;

namespace OpenLauncher.Services;

/// <summary>
/// Startbefehl ohne Fenster für Programmieraufträge von Jarvis (Franks Sprachassistent):
/// <c>OpenLauncher.exe --jarvis-auftrag &lt;auftrag.txt&gt; [--name "Jarvis: App"] [--profil minimal] [--modus schnell]
/// [--modell claude-opus-5-5[1m]] [--effort high] [--ordner C:\Users\…\proggs]</c>.
/// Startet genau wie der Startknopf eine Claude-Code-Sitzung (Profil-Ordner, aktive CLAUDE.md mit Modus, Modell,
/// Effort, Terminal-Farbe), dazu Remote Control mit Namen und den Auftrag als erste Nachricht. Aufrufer ist die
/// PC-Brücke (Jarvis/werkzeuge/pc-bruecke). Läuft neben einem offenen OpenLauncher, ohne dessen Fenster zu berühren.
/// Rückgabe: 0 = gestartet, 1 = Fehler (Einzelheiten im OpenLauncher-Log).
/// </summary>
public static class JarvisAuftrag
{
    public const string Schalter = "--jarvis-auftrag";

    /// <summary>null = kein Jarvis-Auftrag in den Argumenten, sonst der Exit-Code.</summary>
    public static int? Verarbeite(string[] args)
    {
        var stelle = Array.IndexOf(args, Schalter);
        if (stelle < 0) return null;
        string Wert(string name, string vorgabe)
        {
            var i = Array.IndexOf(args, name);
            return i >= 0 && i + 1 < args.Length && !string.IsNullOrWhiteSpace(args[i + 1]) ? args[i + 1] : vorgabe;
        }

        var log = Logger.Instance;
        try
        {
            var datei = stelle + 1 < args.Length ? args[stelle + 1] : "";
            if (!File.Exists(datei)) throw new FileNotFoundException("Auftragsdatei fehlt", datei);
            var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            var profil = Wert("--profil", "minimal");
            var modus = Wert("--modus", "schnell");
            var modell = Wert("--modell", "claude-opus-5-5[1m]");
            var effort = Wert("--effort", "high");
            var name = Wert("--name", "Jarvis");
            var ordner = Wert("--ordner", Path.Combine(home, "proggs"));

            var configDir = new InstructionProfileService().EnsureClaudeConfigDir(profil, modus);
            new OpenLauncherService().LaunchClaudeCode(modell, ordner, effort, configDir, useTmux: false, remoteControlName: name, initialPromptFile: datei);
            log.Info("JarvisAuftrag", "Verarbeite", "Jarvis-Sitzung gestartet", new { name, profil, modus, modell, effort, ordner });
            return 0;
        }
        catch (Exception ex)
        {
            log.Error("JarvisAuftrag", "Verarbeite", ex);
            return 1;
        }
    }
}
