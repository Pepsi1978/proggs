using System.IO;

namespace OpenLauncher.Services;

/// <summary>
/// Startbefehl ohne Fenster für Programmieraufträge von Jarvis (Franks Sprachassistent):
/// <c>OpenLauncher.exe --jarvis-auftrag &lt;auftrag.txt&gt; [--name "Jarvis: App"] [--profil minimal] [--modus schnell]
/// [--modell claude-opus-5-5[1m]] [--effort high] [--ordner C:\Users\…\proggs] [--auftrag-id NUMMER]
/// [--cli claude|codex|opencode|kimi|antigravity] [--anbieter openai]</c>. Ab 1.24.111 jedes Modell aus dem Angebot:
/// --anbieter wählt bei OpenCode die Modellgruppe, kimi und antigravity starten wie die CLI-Kacheln.
/// Mit --cli codex (etwa --modell gpt-6.1-sol) startet stattdessen das Codex CLI wie der Startknopf, ohne Remote Control;
/// mit --cli opencode OpenCode mit openai/&lt;modell&gt; (ab 1.24.99).
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

            var cli = Wert("--cli", "claude").ToLowerInvariant();
            var anbieter = Wert("--anbieter", "");
            var id = auftragId.Length > 0 ? auftragId : null;
            var profile = new InstructionProfileService();
            if (cli == "codex")
            {
                // Wie der Startknopf mit Codex CLI: Profil und Modus in die AGENTS.md des Arbeitsordners, eigenes Codex-Zuhause.
                // Codex kennt weder Remote Control noch die Jarvis-Hooks: Die Sitzung meldet selbst über jarvis_hook.py melde.
                profile.ActivateCodexProjectAgents(profil, modus, ordner);
                var codexHome = profile.PrepareCodexHome(profil);
                new OpenLauncherService().LaunchCodexCli(new Models.ModelEntry { Slug = modell }, ordner, effort, codexHome, useTmux: false,
                    initialPromptFile: datei, jarvisAuftragId: id, title: name);
            }
            else if (cli == "opencode")
            {
                // Wie der Startknopf mit OpenCode (OpenAI-Anmeldung per ChatGPT-Abo): Profil-AGENTS.md, Sitzungs-Konfig, Effort
                // als Start-Variante, Modus über das work-mode-Plugin. Fortschritt meldet die Sitzung über den Jarvis-MCP,
                // Rückfragen und Nachträge laufen über das Plugin jarvis-auftrag.js (aktiv nur mit JARVIS_AUFTRAG_ID).
                profile.ActivateProjectAgents(profil, ordner);
                var sitzung = profile.PrepareOpenCodeSession(profil, ordner, isLmStudio: anbieter.Equals(LmStudioService.ProviderId, StringComparison.OrdinalIgnoreCase));
                if (id != null) InstalliereJarvisPlugin(home);
                var launcher = new OpenLauncherService();
                var eintrag = anbieter.Length == 0 || anbieter.Equals("openai", StringComparison.OrdinalIgnoreCase)
                    ? new Models.ModelEntry { ProviderId = "openai", ProviderName = "OpenAI", Slug = modell, DisplayName = modell, IsUserDefined = true }
                    : ModelRegistry.Load().Groups.SelectMany(g => g.Models).FirstOrDefault(m => m.Slug == modell && m.ProviderId.Equals(anbieter, StringComparison.OrdinalIgnoreCase))
                        ?? throw new ArgumentException($"Modell {modell} ({anbieter}) steht nicht im Angebot", nameof(args));
                // OpenRouter braucht in der Oberfläche eine Provider-Wahl; ohne sie routet OpenRouter selbst (kein Patch).
                var modelString = anbieter.Equals("openrouter", StringComparison.OrdinalIgnoreCase) ? eintrag.ModelString
                    : launcher.ConfigureProvider(eintrag, new Models.ProviderEntry(), Array.Empty<Models.ProviderEntry>(), effort);
                // Der Effort gilt bei OpenCode nur über den gemerkten Modellzustand: Ohne diesen Schritt fiel er bei OpenRouter still weg.
                if (anbieter.Equals("openrouter", StringComparison.OrdinalIgnoreCase)) launcher.SetzeOpenCodeVariante(modelString, effort);
                launcher.Launch(modelString, ordner, effort, sitzung.ConfigPath, modus, useTmux: false,
                    initialPromptFile: datei, jarvisAuftragId: id, title: name);
            }
            else if (cli == "kimi")
            {
                // Kennt weder Remote Control noch die Jarvis-Hooks: Die Sitzung meldet selbst über jarvis_hook.py melde.
                profile.ActivateCodexProjectAgents(profil, modus, ordner);
                new OpenLauncherService().LaunchKimiCli(new Models.ModelEntry { Slug = modell }, ordner, effort, useTmux: false,
                    initialPromptFile: datei, jarvisAuftragId: id, customTitle: name);
            }
            else if (cli == "antigravity")
            {
                profile.ActivateCodexProjectAgents(profil, modus, ordner);
                new OpenLauncherService().LaunchAntigravityCli(new Models.ModelEntry { Slug = modell }, ordner, effort, useTmux: false,
                    initialPromptFile: datei, jarvisAuftragId: id, customTitle: name);
            }
            else if (cli != "claude")
            {
                throw new ArgumentException($"Unbekanntes CLI: {cli}", nameof(args));
            }
            else
            {
                var configDir = profile.EnsureClaudeConfigDir(profil, modus);
                new OpenLauncherService().LaunchClaudeCode(modell, ordner, effort, configDir, useTmux: false, remoteControlName: name, initialPromptFile: datei,
                    jarvisAuftragId: id);
            }
            log.Info("JarvisAuftrag", "Verarbeite", "Jarvis-Sitzung gestartet", new { cli, anbieter, name, profil, modus, modell, effort, ordner, auftragId });
            return 0;
        }
        catch (Exception ex)
        {
            log.Error("JarvisAuftrag", "Verarbeite", ex);
            return 1;
        }
    }

    /// <summary>Das Plugin jarvis-auftrag.js (Rückfragen, Abschluss, Nachträge ans Handy) liegt im Repo; OpenCode lädt es aus ~/.config/opencode/plugins.
    /// Ohne JARVIS_AUFTRAG_ID tut es nichts, darf also immer dort liegen. Gegenstück zu installiereJarvisPlugin am Mac.</summary>
    private static void InstalliereJarvisPlugin(string home)
    {
        var quelle = Path.Combine(home, "proggs", "opencode-setup", "plugins", "jarvis-auftrag.js");
        var ziel = Path.Combine(home, ".config", "opencode", "plugins", "jarvis-auftrag.js");
        if (!File.Exists(quelle)) return;
        var neu = File.ReadAllBytes(quelle);
        if (File.Exists(ziel) && File.ReadAllBytes(ziel).AsSpan().SequenceEqual(neu)) return;
        Directory.CreateDirectory(Path.GetDirectoryName(ziel)!);
        File.WriteAllBytes(ziel, neu);
    }
}
