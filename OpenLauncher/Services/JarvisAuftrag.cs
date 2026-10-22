using System.IO;

namespace OpenLauncher.Services;

/// <summary>
/// Startbefehl ohne Fenster für Programmieraufträge von Jarvis (Franks Sprachassistent):
/// <c>OpenLauncher.exe --jarvis-auftrag &lt;auftrag.txt&gt; [--name "Jarvis: App"] [--profil minimal] [--modus schnell]
/// [--modell claude-opus-5-5[1m]] [--effort high] [--ordner C:\Users\…\proggs] [--auftrag-id NUMMER] [--cli claude|codex]</c>.
/// Mit --cli codex (etwa --modell gpt-6.1-sol) startet stattdessen das Codex CLI wie der Startknopf, ohne Remote Control.
/// Mit --auftrag-id bekommt die Sitzung die Jarvis-Hooks (Fortschritt und Rückfragen in den Jarvis-Chat).
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
            var auftragId = Wert("--auftrag-id", "");
            if (auftragId.Length > 0 && !System.Text.RegularExpressions.Regex.IsMatch(auftragId, "^[A-Za-z0-9]{1,20}$"))
                throw new ArgumentException("Ungültige Auftragsnummer", nameof(args));

            var cli = Wert("--cli", "claude");
            var profile = new InstructionProfileService();
            if (cli == "codex")
            {
                // Wie der Startknopf mit Codex CLI: Profil und Modus in die AGENTS.md des Arbeitsordners, eigenes Codex-Zuhause.
                // Codex kennt weder Remote Control noch die Jarvis-Hooks: Die Sitzung meldet selbst über jarvis_hook.py melde.
                profile.ActivateCodexProjectAgents(profil, modus, ordner);
                var codexHome = profile.PrepareCodexHome(profil);
                new OpenLauncherService().LaunchCodexCli(new Models.ModelEntry { Slug = modell }, ordner, effort, codexHome, useTmux: false,
                    initialPromptFile: datei, jarvisAuftragId: auftragId.Length > 0 ? auftragId : null, title: name);
            }
            else
            {
                var configDir = profile.EnsureClaudeConfigDir(profil, modus);
                new OpenLauncherService().LaunchClaudeCode(modell, ordner, effort, configDir, useTmux: false, remoteControlName: name, initialPromptFile: datei,
                    jarvisAuftragId: auftragId.Length > 0 ? auftragId : null);
            }
            log.Info("JarvisAuftrag", "Verarbeite", "Jarvis-Sitzung gestartet", new { cli, name, profil, modus, modell, effort, ordner, auftragId });
            return 0;
        }
        catch (Exception ex)
        {
            log.Error("JarvisAuftrag", "Verarbeite", ex);
            return 1;
        }
    }
}
