using System.Diagnostics;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using System.Text.Json.Serialization.Metadata;
using OpenCodeLauncher.Models;

namespace OpenCodeLauncher.Services;

/// <summary>
/// Startet OpenCode mit einem gewählten Modell über einen gewählten Provider.
/// Vorgehen (Nutzer-Entscheidung: opencode.json schreiben + opencode starten):
///   1. Globale opencode.json (~/.config/opencode/opencode.json) BOM-frei einlesen
///      (Kurzcheck §10.3: UTF-8-BOM bricht Parse) und als JsonDocument verarbeiten.
///   2. provider.openrouter.models.<slug>.options.provider.order = [gewählter Provider]
///      + allow_fallbacks:false setzen. Dadurch läuft OpenCode exakt über den gewählten Provider.
///   3. Datei atomar + BOM-frei zurückschreiben (.bak Backup).
///   4. opencode -m openrouter/<slug> "<workdir>" in neuem Windows-Terminal starten.
/// OpenCode zeigt im Banner das Modell (openrouter/<slug>) und verbindet sich dann.
/// </summary>
public sealed class OpenCodeLauncherService
{
    private const string Gpt56SolSlug = "gpt-5.6-sol";
    private const string Gpt56SolFastSlug = "gpt-5.6-sol-fast";
    private const string Gpt56TerraSlug = "gpt-5.6-terra";
    private const string Gpt56TerraFastSlug = "gpt-5.6-terra-fast";
    private const string Gpt56LunaSlug = "gpt-5.6-luna";
    private const string Gpt56LunaFastSlug = "gpt-5.6-luna-fast";

    private static readonly TerminalTabColor[] TerminalTabColors =
    [
        new("black", "#0C0C0C"),
        new("red", "#C50F1F"),
        new("green", "#13A10E"),
        new("yellow", "#C19C00"),
        new("blue", "#0037DA"),
        new("purple", "#881798"),
        new("cyan", "#3A96DD"),
        new("white", "#CCCCCC"),
        new("bright-black", "#767676"),
        new("bright-red", "#E74856"),
        new("bright-green", "#16C60C"),
        new("bright-yellow", "#F9F1A5"),
        new("bright-blue", "#3B78FF"),
        new("bright-purple", "#B4009E"),
        new("bright-cyan", "#61D6D6"),
        new("bright-white", "#F2F2F2"),
    ];

    private static readonly TerminalTabColor[] ClaudeTerminalTabColors =
    [
        new("red", "#FF3B3B"),
        new("orange", "#FF8C42"),
        new("yellow", "#FFD93D"),
        new("green", "#4CAF50"),
        new("cyan", "#00BCD4"),
        new("blue", "#2196F3"),
        new("purple", "#9C27B0"),
        new("pink", "#FF1493"),
    ];

    private static readonly string ConfigDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        ".config", "opencode");

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver()
    };

    /// <summary>
    /// Schreibt die Provider-Order (exakt der gewählte Provider, ohne Fallback)
    /// in die globale opencode.jsonc/opencode.json. Gibt den Modell-String zurück, der an opencode -m geht.
    /// </summary>
    public string ConfigureProvider(ModelEntry model, ProviderEntry chosen, IReadOnlyList<ProviderEntry> allProviders, string? thinkingLevel)
    {
        var log = Logger.Instance;
        var modelString = model.ModelString;
        thinkingLevel = NormalizeThinkingLevel(thinkingLevel);

        if (!string.Equals(model.ProviderId, "openrouter", StringComparison.OrdinalIgnoreCase))
        {
            var usesPriorityServiceTier = UsesPriorityServiceTier(model.ProviderId, model.Slug);
            if (!string.IsNullOrWhiteSpace(thinkingLevel) || IsGpt56Model(model.ProviderId, model.Slug))
            {
                var root = ReadConfig();
                root = PatchDirectModel(root, model.ProviderId, model.Slug, model.DisplayName, thinkingLevel);
                WriteConfig(root);
                if (!string.IsNullOrWhiteSpace(thinkingLevel)) PatchModelVariantState(modelString, thinkingLevel);
                log.Info("OpenCodeLauncherService", "ConfigureProvider", "Direktmodell-Variante gesetzt", new
                {
                    model = modelString,
                    thinkingLevel,
                    serviceTier = usesPriorityServiceTier ? "priority" : null
                });
            }
            log.Info("OpenCodeLauncherService", "ConfigureProvider", $"Direktmodell ohne OpenRouter-Routing: {modelString}");
            return modelString;
        }

        try
        {
            var root = ReadConfig();
            root = PatchProvider(root, model.Slug, model.DisplayName, chosen, allProviders, thinkingLevel);
            WriteConfig(root);
            PatchModelVariantState(modelString, thinkingLevel);
            log.Info("OpenCodeLauncherService", "ConfigureProvider",
                $"opencode-Konfig gepatched: {model.Slug} via {chosen.ProviderName}",
                new { order = new[] { chosen.ProviderSlug }, thinkingLevel });
        }
        catch (Exception ex)
        {
            log.Error("OpenCodeLauncherService", "ConfigureProvider", ex, new { model.Slug, model.ProviderId, chosen.ProviderName });
            throw;
        }

        return modelString;
    }

    /// <summary>Startet opencode in einem neuen Windows-Terminal-Fenster.</summary>
    public void Launch(string modelString, string workDir, string? thinkingLevel, string profileConfigPath, bool minimalIsolation = false)
    {
        var log = Logger.Instance;
        thinkingLevel = NormalizeThinkingLevel(thinkingLevel);
        try
        {
            Directory.CreateDirectory(workDir);

            // Bevorzugt Windows Terminal (AppX, evtl. nicht im PATH) — Fallback: eigene Konsole.
            var wt = ResolveWt();
            var tabColor = PickTerminalTabColor();
            // WICHTIG: opencode wird über ein Temp-Script (-File) gestartet, NICHT über einen
            // inline "-Command"-String. Ein solcher String enthielte das Semikolon aus
            // "$env:OPENCODE_CONFIG='...'; & opencode ...". Windows Terminal deutet ';' in seiner
            // Argumentliste als eigenen Kommando-Trenner und versucht den Teil dahinter als
            // separates Programm zu starten (Fehler 0x80070002 "Die angegebene Datei wurde nicht
            // gefunden") — es öffnen sich ein leeres pwsh-Fenster (Teil vor ';') und ein
            // Fehler-Fenster (Teil danach), bis der Konsolen-Fallback ohne wt greift. Ein
            // -File-Script hat kein ';' in der wt-Argumentliste und ist immun (gleiche Technik
            // wie der bereits funktionierende Claude-Code-Weg).
            var innerScript = BuildOpenCodeStartScript(modelString, workDir, thinkingLevel, profileConfigPath, minimalIsolation);
            var shell = ResolvePowerShellExecutable();
            var robustLauncherScript = shell.IsPwsh ? ResolveRobustLauncherScript() : null;

            if (!string.IsNullOrEmpty(wt) && !string.IsNullOrEmpty(robustLauncherScript))
            {
                var robustProcess = LaunchOpenCodeViaRobustPowerShell(wt, robustLauncherScript, shell.Path, innerScript, workDir, modelString, thinkingLevel, tabColor, log);
                log.Info("OpenCodeLauncherService", "Launch", $"robuster Windows-Terminal-Launcher gestartet (PID {robustProcess?.Id})", new { modelString, workDir, thinkingLevel, tabColor = tabColor.Name });
                return;
            }

            var psi = new ProcessStartInfo
            {
                UseShellExecute = false,
                CreateNoWindow = false,
                WorkingDirectory = workDir,
            };

            if (!string.IsNullOrEmpty(wt))
            {
                log.Warn("OpenCodeLauncherService", "Launch", shell.IsPwsh
                    ? "start-wt-common.ps1 nicht gefunden; nutze direkten Windows-Terminal-Start ohne Retry-Wrapper"
                    : "pwsh.exe nicht gefunden; nutze Windows PowerShell ohne Retry-Wrapper");
                psi.FileName = wt;
                psi.ArgumentList.Add("new-tab");
                psi.ArgumentList.Add("--tabColor");
                psi.ArgumentList.Add(tabColor.Hex);
                psi.ArgumentList.Add("--title");
                psi.ArgumentList.Add(string.IsNullOrWhiteSpace(thinkingLevel) ? $"OpenCode-{tabColor.Name}" : $"OpenCode-{tabColor.Name}-{thinkingLevel}");
                psi.ArgumentList.Add("--startingDirectory");
                psi.ArgumentList.Add(workDir);
                psi.ArgumentList.Add(shell.Path);
                psi.ArgumentList.Add("-NoExit");
                psi.ArgumentList.Add("-ExecutionPolicy");
                psi.ArgumentList.Add("Bypass");
                psi.ArgumentList.Add("-File");
                psi.ArgumentList.Add(innerScript);
                log.Info("OpenCodeLauncherService", "Launch", $"Terminal-Tabfarbe gewählt: {tabColor.Name} ({tabColor.Hex})");
            }
            else
            {
                psi.FileName = shell.Path;
                psi.ArgumentList.Add("-NoExit");
                psi.ArgumentList.Add("-ExecutionPolicy");
                psi.ArgumentList.Add("Bypass");
                psi.ArgumentList.Add("-File");
                psi.ArgumentList.Add(innerScript);
            }

            var p = Process.Start(psi);
            log.Info("OpenCodeLauncherService", "Launch", $"opencode gestartet (PID {p?.Id})", new { modelString, workDir, thinkingLevel, wtUsed = wt != null });
        }
        catch (Exception ex)
        {
            log.Error("OpenCodeLauncherService", "Launch", ex, new { modelString, workDir, thinkingLevel });
            throw;
        }
    }

    /// <summary>Startet Claude Code wie die Desktop-Verknüpfung, aber mit gewähltem Modell und Effort.</summary>
    /// <param name="claudeConfigDir">
    /// Wenn gesetzt, wird CLAUDE_CONFIG_DIR auf diesen Ordner gezeigt (Minimal-Profil: frisches,
    /// leeres Profil im Repo). Bei null startet Claude mit dem normalen ~/.claude (Standard-Profil).
    /// </param>
    public void LaunchClaudeCode(string modelId, string workDir, string? effortLevel, string? claudeConfigDir = null)
    {
        var log = Logger.Instance;
        effortLevel = NormalizeThinkingLevel(effortLevel);
        try
        {
            Directory.CreateDirectory(workDir);
            var wt = ResolveWt();
            var tabColor = PickClaudeTerminalTabColor();
            var innerScript = BuildClaudeCodeStartScript(modelId, workDir, effortLevel, tabColor.Name, claudeConfigDir);
            var shell = ResolvePowerShellExecutable();
            var robustLauncherScript = shell.IsPwsh ? ResolveRobustLauncherScript() : null;

            if (!string.IsNullOrEmpty(wt) && !string.IsNullOrEmpty(robustLauncherScript))
            {
                var process = LaunchClaudeCodeViaRobustPowerShell(wt, robustLauncherScript, shell.Path, innerScript, workDir, modelId, effortLevel, tabColor, log);
                log.Info("OpenCodeLauncherService", "LaunchClaudeCode", $"robuster Claude-Code-Launcher gestartet (PID {process?.Id})", new { modelId, workDir, effortLevel, tabColor = tabColor.Name });
                return;
            }

            var psi = new ProcessStartInfo
            {
                UseShellExecute = false,
                CreateNoWindow = false,
                WorkingDirectory = workDir,
            };
            if (!string.IsNullOrEmpty(wt))
            {
                psi.FileName = wt;
                psi.ArgumentList.Add("new-tab");
                psi.ArgumentList.Add("--tabColor");
                psi.ArgumentList.Add(tabColor.Hex);
                psi.ArgumentList.Add("--title");
                psi.ArgumentList.Add(BuildClaudeCodeTitle(tabColor.Name, effortLevel));
                psi.ArgumentList.Add("--startingDirectory");
                psi.ArgumentList.Add(workDir);
                psi.ArgumentList.Add(shell.Path);
                psi.ArgumentList.Add("-NoExit");
                psi.ArgumentList.Add("-ExecutionPolicy");
                psi.ArgumentList.Add("Bypass");
                psi.ArgumentList.Add("-File");
                psi.ArgumentList.Add(innerScript);
            }
            else
            {
                psi.FileName = shell.Path;
                psi.ArgumentList.Add("-NoExit");
                psi.ArgumentList.Add("-ExecutionPolicy");
                psi.ArgumentList.Add("Bypass");
                psi.ArgumentList.Add("-File");
                psi.ArgumentList.Add(innerScript);
            }

            var p = Process.Start(psi);
            log.Info("OpenCodeLauncherService", "LaunchClaudeCode", $"Claude Code gestartet (PID {p?.Id})", new { modelId, workDir, effortLevel, wtUsed = wt != null });
        }
        catch (Exception ex)
        {
            log.Error("OpenCodeLauncherService", "LaunchClaudeCode", ex, new { modelId, workDir, effortLevel });
            throw;
        }
    }

    private static string? ResolveWt()
    {
        try
        {
            // AppX-Alias liegt typisch unter %LOCALAPPDATA%\Microsoft\WindowsApps\wt.exe
            var local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            var candidate = Path.Combine(local, "Microsoft", "WindowsApps", "wt.exe");
            if (File.Exists(candidate)) return candidate;

            using var p = Process.Start(new ProcessStartInfo("where.exe", "wt") { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true });
            if (p != null && p.WaitForExit(2000))
            {
                var line = p.StandardOutput.ReadLine();
                if (!string.IsNullOrWhiteSpace(line) && File.Exists(line.Trim())) return line.Trim();
            }
        }
        catch { /* Fallback unten */ }
        return null;
    }

    private static PowerShellExecutable ResolvePowerShellExecutable()
    {
        var programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
        var pwsh = Path.Combine(programFiles, "PowerShell", "7", "pwsh.exe");
        if (File.Exists(pwsh)) return new PowerShellExecutable(pwsh, true);

        try
        {
            using var p = Process.Start(new ProcessStartInfo("where.exe", "pwsh.exe") { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true });
            if (p != null && p.WaitForExit(2000))
            {
                var line = p.StandardOutput.ReadLine();
                if (!string.IsNullOrWhiteSpace(line) && File.Exists(line.Trim())) return new PowerShellExecutable(line.Trim(), true);
            }
        }
        catch { /* Fallback unten */ }

        var systemRoot = Environment.GetFolderPath(Environment.SpecialFolder.Windows);
        var windowsPowerShell = Path.Combine(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        return File.Exists(windowsPowerShell)
            ? new PowerShellExecutable(windowsPowerShell, false)
            : new PowerShellExecutable("powershell.exe", false);
    }

    private static TerminalTabColor PickTerminalTabColor()
    {
        try
        {
            var colorByName = TerminalTabColors.ToDictionary(c => c.Name, StringComparer.OrdinalIgnoreCase);
            var state = ReadTabColorState();
            var remaining = state.Remaining
                .Where(name => colorByName.ContainsKey(name) && !string.Equals(name, state.LastColor, StringComparison.OrdinalIgnoreCase))
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .ToList();

            if (remaining.Count == 0)
            {
                remaining = TerminalTabColors
                    .Select(c => c.Name)
                    .Where(name => !string.Equals(name, state.LastColor, StringComparison.OrdinalIgnoreCase))
                    .ToList();
            }

            var index = RandomNumberGenerator.GetInt32(remaining.Count);
            var pickedName = remaining[index];
            remaining.RemoveAt(index);
            WriteTabColorState(new TabColorState
            {
                LastColor = pickedName,
                Remaining = remaining
            });
            return colorByName[pickedName];
        }
        catch
        {
            return TerminalTabColors[RandomNumberGenerator.GetInt32(TerminalTabColors.Length)];
        }
    }

    private static TerminalTabColor PickClaudeTerminalTabColor() =>
        ClaudeTerminalTabColors[RandomNumberGenerator.GetInt32(ClaudeTerminalTabColors.Length)];

    private static TabColorState ReadTabColorState()
    {
        var path = ResolveTabColorStatePath();
        if (!File.Exists(path)) return new TabColorState();

        var raw = File.ReadAllText(path, Encoding.UTF8);
        return JsonSerializer.Deserialize<TabColorState>(raw, JsonOpts) ?? new TabColorState();
    }

    private static void WriteTabColorState(TabColorState state)
    {
        var path = ResolveTabColorStatePath();
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        var tmp = path + ".tmp";
        File.WriteAllText(tmp, JsonSerializer.Serialize(state, JsonOpts), new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        File.Move(tmp, path, overwrite: true);
    }

    private static string ResolveTabColorStatePath() => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenCodeLauncher",
        "tab-color-state.json");

    private static string EscapePowerShellSingleQuotedValue(string value) => value.Replace("'", "''", StringComparison.Ordinal);

    private static string? ResolveRobustLauncherScript()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var installed = Path.Combine(home, "start-wt-common.ps1");
        if (File.Exists(installed)) return installed;

        var repoMirror = Path.Combine(home, "proggs", "claude-code-setup", "launcher", "start-wt-common.ps1");
        return File.Exists(repoMirror) ? repoMirror : null;
    }

    private static Process? LaunchOpenCodeViaRobustPowerShell(
        string wtPath,
        string robustLauncherScript,
        string powerShellPath,
        string innerScript,
        string workDir,
        string modelString,
        string? thinkingLevel,
        TerminalTabColor tabColor,
        Logger log)
    {
        var title = string.IsNullOrWhiteSpace(thinkingLevel) ? $"OpenCode-{tabColor.Name}" : $"OpenCode-{tabColor.Name}-{thinkingLevel}";
        var tabArgs = new[]
        {
            "new-tab",
            "--tabColor", tabColor.Hex,
            "--title", title,
            "--startingDirectory", workDir,
            powerShellPath,
            "-NoExit",
            "-ExecutionPolicy", "Bypass",
            "-File", innerScript
        };
        var fallbackArgs = new[]
        {
            "-NoExit",
            "-ExecutionPolicy", "Bypass",
            "-File", innerScript
        };

        var tempScript = Path.Combine(Path.GetTempPath(), $"opencode-launcher-wt-{Guid.NewGuid():N}.ps1");
        var script = $$"""
$ErrorActionPreference = 'Continue'
. {{PowerShellLiteral(robustLauncherScript)}}
$tabArgs = @({{PowerShellArrayLiteral(tabArgs)}})
$fallbackArgs = @({{PowerShellArrayLiteral(fallbackArgs)}})
try {
    $ok = Start-WtCliRobust -LogFile {{PowerShellLiteral(log.LogPath)}} -WtPath {{PowerShellLiteral(wtPath)}} -TabArgs $tabArgs -InnerMatch 'opencode-launcher-opencode-run-' -FallbackPwshArgs $fallbackArgs -FallbackWorkDir {{PowerShellLiteral(workDir)}}
    if (-not $ok) { exit 2 }
} finally {
    Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue
}
""";
        File.WriteAllText(tempScript, script, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));

        var psi = new ProcessStartInfo
        {
            FileName = powerShellPath,
            UseShellExecute = false,
            CreateNoWindow = true,
            WorkingDirectory = workDir,
        };
        psi.ArgumentList.Add("-NoProfile");
        psi.ArgumentList.Add("-ExecutionPolicy");
        psi.ArgumentList.Add("Bypass");
        psi.ArgumentList.Add("-File");
        psi.ArgumentList.Add(tempScript);
        log.Info("OpenCodeLauncherService", "LaunchOpenCodeViaRobustPowerShell", "OpenCode-Start vorbereitet", new { modelString, thinkingLevel, tabColor = tabColor.Name });
        return Process.Start(psi);
    }

    private static Process? LaunchClaudeCodeViaRobustPowerShell(
        string wtPath,
        string robustLauncherScript,
        string powerShellPath,
        string innerScript,
        string workDir,
        string modelId,
        string? effortLevel,
        TerminalTabColor tabColor,
        Logger log)
    {
        var title = BuildClaudeCodeTitle(tabColor.Name, effortLevel);
        var tabArgs = new[]
        {
            "new-tab",
            "--tabColor", tabColor.Hex,
            "--title", title,
            "--startingDirectory", workDir,
            powerShellPath,
            "-NoExit",
            "-ExecutionPolicy", "Bypass",
            "-File", innerScript
        };
        var fallbackArgs = new[]
        {
            "-NoExit",
            "-ExecutionPolicy", "Bypass",
            "-File", innerScript
        };

        var tempScript = Path.Combine(Path.GetTempPath(), $"opencode-launcher-claude-wt-{Guid.NewGuid():N}.ps1");
        var script = $$"""
$ErrorActionPreference = 'Continue'
. {{PowerShellLiteral(robustLauncherScript)}}
$tabArgs = @({{PowerShellArrayLiteral(tabArgs)}})
$fallbackArgs = @({{PowerShellArrayLiteral(fallbackArgs)}})
try {
    $ok = Start-WtCliRobust -LogFile {{PowerShellLiteral(log.LogPath)}} -WtPath {{PowerShellLiteral(wtPath)}} -TabArgs $tabArgs -InnerMatch 'opencode-launcher-claude-code-' -FallbackPwshArgs $fallbackArgs -FallbackWorkDir {{PowerShellLiteral(workDir)}}
    if (-not $ok) { exit 2 }
} finally {
    Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue
}
""";
        File.WriteAllText(tempScript, script, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));

        var psi = new ProcessStartInfo
        {
            FileName = powerShellPath,
            UseShellExecute = false,
            CreateNoWindow = true,
            WorkingDirectory = workDir,
        };
        psi.ArgumentList.Add("-NoProfile");
        psi.ArgumentList.Add("-ExecutionPolicy");
        psi.ArgumentList.Add("Bypass");
        psi.ArgumentList.Add("-File");
        psi.ArgumentList.Add(tempScript);
        log.Info("OpenCodeLauncherService", "LaunchClaudeCodeViaRobustPowerShell", "Claude-Code-Start vorbereitet", new { modelId, effortLevel, tabColor = tabColor.Name });
        return Process.Start(psi);
    }

    private static string BuildClaudeCodeStartScript(string modelId, string workDir, string? effortLevel, string colorName, string? claudeConfigDir)
    {
        effortLevel = NormalizeThinkingLevel(effortLevel);
        var tempScript = Path.Combine(Path.GetTempPath(), $"opencode-launcher-claude-code-{Guid.NewGuid():N}.ps1");
        var tempSettings = BuildClaudeCodeSessionSettings(modelId, effortLevel);
        var script = $$"""
$ErrorActionPreference = 'Continue'
[Console]::Write("`e[?1004l")
Set-Location -LiteralPath {{PowerShellLiteral(workDir)}}

$profilePath = Join-Path $HOME 'Documents\PowerShell\Microsoft.PowerShell_profile.ps1'
if (Test-Path $profilePath) {
    . $profilePath
}

$focusKiller = $null
if (Get-Command Start-ThreadJob -ErrorAction SilentlyContinue) {
    $focusKiller = Start-ThreadJob -ScriptBlock {
        $esc = [char]0x1B
        while ($true) {
            Start-Sleep -Seconds 1
            try { [Console]::Write("$esc[?1004l") } catch { break }
        }
    }
}

try {
    # Minimal-Profil: eigener, frischer Config-Ordner (keine Rules/Hooks/Memory/Almanach).
    # Leer -> Standard-Profil (~/.claude) bleibt unveraendert.
    $claudeConfigDir = {{PowerShellLiteral(claudeConfigDir ?? string.Empty)}}
    if ($claudeConfigDir) {
        New-Item -ItemType Directory -Force -Path $claudeConfigDir | Out-Null
        $env:CLAUDE_CONFIG_DIR = $claudeConfigDir
    }
    $sessionSettings = {{PowerShellLiteral(tempSettings)}}
    $claudeArgs = @('--dangerously-skip-permissions', '--settings', $sessionSettings, '--model', {{PowerShellLiteral(modelId)}})
    $effort = {{PowerShellLiteral(effortLevel ?? string.Empty)}}
    if ($effort) {
        # Only pass --effort as the INITIAL value; do NOT set $env:CLAUDE_CODE_EFFORT_LEVEL.
        # The env var takes precedence over the runtime /effort command, which would pin the
        # effort permanently and make /effort changes silently snap back (rule: never via env).
        $claudeArgs += @('--effort', $effort)
    }
    $colorName = {{PowerShellLiteral(colorName)}}
    if ($colorName) {
        $claudeArgs += "/color $colorName"
    } else {
        $claudeArgs += '/color'
    }
    & claude @claudeArgs
} finally {
    if ($focusKiller) {
        Stop-Job $focusKiller -ErrorAction SilentlyContinue
        Remove-Job $focusKiller -Force -ErrorAction SilentlyContinue
    }
    [Console]::Write("`e[?1004l")
    Remove-Item -LiteralPath {{PowerShellLiteral(tempSettings)}} -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue
}
""";
        File.WriteAllText(tempScript, script, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        return tempScript;
    }

    private static string BuildClaudeCodeSessionSettings(string modelId, string? effortLevel)
    {
        var tempSettings = Path.Combine(Path.GetTempPath(), $"opencode-launcher-claude-settings-{Guid.NewGuid():N}.json");
        var root = new JsonObject
        {
            ["model"] = modelId
        };
        if (IsPersistableClaudeEffort(effortLevel)) root["effortLevel"] = effortLevel;
        File.WriteAllText(tempSettings, root.ToJsonString(JsonOpts), new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        return tempSettings;
    }

    private static bool IsPersistableClaudeEffort(string? effortLevel) =>
        effortLevel is "low" or "medium" or "high" or "xhigh";

    private static string BuildClaudeCodeTitle(string colorName, string? effortLevel) =>
        string.IsNullOrWhiteSpace(effortLevel) ? $"Claude-{colorName}" : $"Claude-{colorName}-{effortLevel}";

    private static string PowerShellArrayLiteral(IEnumerable<string> values) => string.Join(", ", values.Select(PowerShellLiteral));

    private static string PowerShellLiteral(string value) => $"'{EscapePowerShellSingleQuotedValue(value)}'";

    /// <summary>
    /// PowerShell-Block fuer das Minimalprofil: blendet die von OpenCode zwangsweise gelesenen
    /// AGENTS.md (Projekt im Arbeitsverzeichnis + globale ~/.config/opencode/AGENTS.md) fuer die
    /// Dauer der Session aus. Die globale wird durch eine leere Datei ersetzt, damit ein evtl.
    /// vorhandener `instructions`-Verweis in der globalen opencode.jsonc keinen fehlenden Pfad
    /// referenziert. Restore-AgentsFiles (im finally) stellt beide wieder her; bricht die Session
    /// hart ab, heilt der naechste Start das haengengebliebene Backup selbst.
    /// </summary>
    private static string BuildMinimalHideBlock(string workDir)
    {
        var projectAgents = PowerShellLiteral(Path.Combine(workDir, "AGENTS.md"));
        const string template = """
# --- Minimalprofil: ausschliesslich die Minimal-Datei als Kontext ---
$script:MiniHidden = @()
function Hide-AgentsFile([string]$Path, [bool]$ReplaceEmpty) {
    $bak = "$Path.opencode-minimal-hidden"
    if ((Test-Path -LiteralPath $bak) -and -not (Test-Path -LiteralPath $Path)) {
        try { Move-Item -LiteralPath $bak -Destination $Path -Force } catch {}
    }
    if (Test-Path -LiteralPath $Path) {
        if (Test-Path -LiteralPath $bak) { return }
        try {
            Move-Item -LiteralPath $Path -Destination $bak -Force
            if ($ReplaceEmpty) { [System.IO.File]::WriteAllText($Path, '') }
            $script:MiniHidden += ,@($Path, $bak, $ReplaceEmpty)
        } catch {
            Write-Host ('[Minimal] AGENTS.md konnte nicht ausgeblendet werden: ' + $_.Exception.Message) -ForegroundColor DarkYellow
        }
    }
}
function Restore-AgentsFiles {
    foreach ($e in $script:MiniHidden) {
        try {
            $p = $e[0]; $bak = $e[1]; $empty = $e[2]
            if ($empty -and (Test-Path -LiteralPath $p) -and ((Get-Item -LiteralPath $p).Length -eq 0)) {
                Remove-Item -LiteralPath $p -Force
            }
            if ((Test-Path -LiteralPath $bak) -and -not (Test-Path -LiteralPath $p)) {
                Move-Item -LiteralPath $bak -Destination $p -Force
            }
        } catch {}
    }
}
Hide-AgentsFile __PROJECT_AGENTS__ $false
Hide-AgentsFile (Join-Path $env:USERPROFILE '.config\opencode\AGENTS.md') $true
""";
        return template.Replace("__PROJECT_AGENTS__", projectAgents, StringComparison.Ordinal);
    }

    /// <summary>
    /// Schreibt ein Temp-Script, das die Profil-Config setzt und opencode startet, und gibt
    /// dessen Pfad zurück. Bewusst ein -File-Script statt eines inline "-Command"-Strings:
    /// Der Command enthielte das Semikolon aus "$env:OPENCODE_CONFIG='...'; & opencode ...",
    /// und Windows Terminal deutet ';' in seiner Argumentliste als eigenen Kommando-Trenner
    /// (der Teil dahinter wird als separates Programm gestartet → Fehler 0x80070002
    /// "Die angegebene Datei wurde nicht gefunden"). Ein -File-Script hat kein ';' in der
    /// wt-Argumentliste und ist damit immun — dieselbe Technik wie der Claude-Code-Weg.
    /// $env:OPENCODE_CONFIG und der opencode-Aufruf stehen auf getrennten Zeilen (kein ';').
    /// </summary>
    private static string BuildOpenCodeStartScript(string modelString, string workDir, string? thinkingLevel, string profileConfigPath, bool minimalIsolation)
    {
        thinkingLevel = NormalizeThinkingLevel(thinkingLevel);
        var executable = ResolveOpenCodeExecutable();
        var invoke = Path.IsPathFullyQualified(executable)
            ? $"& '{EscapePowerShellSingleQuotedValue(executable)}'"
            : executable;
        // ConfigureProvider already stores the selected start level in OpenCode's native
        // variant state. A second process-local --variant override competes with Ctrl+T,
        // which must remain the single owner of in-session variant changes.
        var openCodeInvocation = $"{invoke} -m '{EscapePowerShellSingleQuotedValue(modelString)}'";

        // Minimalprofil: OpenCode liest Projekt- und globale AGENTS.md ZWANGSWEISE ein (kein
        // Abschalt-Flag existiert). Damit wirklich nur die Minimal-Datei (via OPENCODE_CONFIG
        // instructions) als Kontext greift, blenden wir diese beiden Dateien fuer die Dauer der
        // Session aus und stellen sie im finally wieder her (mit Self-Heal beim naechsten Start).
        var hideBlock = minimalIsolation ? BuildMinimalHideBlock(workDir) : string.Empty;
        var restoreCall = minimalIsolation ? "Restore-AgentsFiles" : string.Empty;

        var tempScript = Path.Combine(Path.GetTempPath(), $"opencode-launcher-opencode-run-{Guid.NewGuid():N}.ps1");
        // stderr MUST be redirected to a per-process file: unhandled Bun/Effect errors in the
        // OpenCode TUI main thread print raw stack traces to stderr, which is the same TTY the
        // TUI renders on — foreign bytes corrupt the diff-rendered screen until a full repaint
        // (bugs/opencode/opencode-cli.md #14a). stdout/stdin stay on the console so the TUI,
        // mouse handling and prompts keep working. Empty logs are deleted afterwards; non-empty
        // ones are announced (watchdog) so errors stay observable instead of hidden.
        var script = $$"""
$ErrorActionPreference = 'Continue'
Set-Location -LiteralPath {{PowerShellLiteral(workDir)}}
$env:OPENCODE_CONFIG = {{PowerShellLiteral(profileConfigPath)}}
{{hideBlock}}
$stderrDir = Join-Path $env:USERPROFILE '.local\share\opencode\log\stderr'
try {
    New-Item -ItemType Directory -Force -Path $stderrDir | Out-Null
    $old = @(Get-ChildItem -LiteralPath $stderrDir -Filter 'opencode-stderr-*.log' -File -ErrorAction SilentlyContinue)
    foreach ($f in @($old | Where-Object Length -eq 0)) {
        Remove-Item -LiteralPath $f.FullName -Force -ErrorAction SilentlyContinue
    }
    $oldNonEmpty = @($old | Where-Object Length -gt 0)
    if ($oldNonEmpty.Count -gt 0) {
        $newest = $oldNonEmpty | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        Write-Host ("[stderr-Waechter] {0} Fehlerprotokoll(e) frueherer OpenCode-Laeufe - neuestes: {1}" -f $oldNonEmpty.Count, $newest.FullName) -ForegroundColor Yellow
        Get-Content -LiteralPath $newest.FullName -TotalCount 2 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host ('  ' + $_) -ForegroundColor DarkYellow }
    }
} catch {
    Write-Host ('[stderr-Waechter] Vorpruefung uebersprungen: ' + $_.Exception.Message) -ForegroundColor DarkYellow
}
if (-not (Test-Path -LiteralPath $stderrDir)) { $stderrDir = $env:TEMP }
$stderrLog = Join-Path $stderrDir ('opencode-stderr-{0:yyyyMMdd-HHmmss}-{1}.log' -f (Get-Date), $PID)
try {
    {{openCodeInvocation}} 2>>$stderrLog
} finally {
    {{restoreCall}}
    try {
        if (Test-Path -LiteralPath $stderrLog) {
            if ((Get-Item -LiteralPath $stderrLog).Length -eq 0) {
                Remove-Item -LiteralPath $stderrLog -Force -ErrorAction SilentlyContinue
            } else {
                Write-Host ('[stderr-Waechter] Dieser Lauf hatte Fehlerausgaben: ' + $stderrLog) -ForegroundColor Yellow
            }
        }
    } catch {}
    Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue
}
""";
        File.WriteAllText(tempScript, script, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        return tempScript;
    }

    private static string ResolveOpenCodeExecutable()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var root = Path.Combine(home, ".local", "share", "opencode-mousefix");
        foreach (var pointerName in new[] { "current.json", "current.json.bak" })
        {
            var pointerPath = Path.Combine(root, pointerName);
            try
            {
                using var json = JsonDocument.Parse(File.ReadAllText(pointerPath));
                foreach (var entryName in new[] { "active", "previous" })
                {
                    if (!json.RootElement.TryGetProperty(entryName, out var entry)) continue;
                    if (!entry.TryGetProperty("relativeExe", out var relativeNode)) continue;
                    var relative = relativeNode.GetString();
                    if (string.IsNullOrWhiteSpace(relative)) continue;
                    var fullRoot = Path.GetFullPath(root).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
                    var fullPath = Path.GetFullPath(Path.Combine(root, relative));
                    if (!fullPath.StartsWith(fullRoot, StringComparison.OrdinalIgnoreCase)) continue;
                    if (File.Exists(fullPath)) return fullPath;
                }
            }
            catch (Exception ex)
            {
                Logger.Instance.Warn("OpenCodeLauncherService", "ResolveOpenCodeExecutable", $"{pointerName} ist ungültig: {ex.Message}");
            }
        }

        var legacy = Path.Combine(root, "opencode.exe");
        return File.Exists(legacy) ? legacy : "opencode";
    }

    private static string? NormalizeThinkingLevel(string? thinkingLevel) => string.IsNullOrWhiteSpace(thinkingLevel)
        ? null
        : thinkingLevel.Trim().ToLowerInvariant();

    private static string ResolveModelStatePath()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        return Path.Combine(home, ".local", "state", "opencode", "model.json");
    }

    private static void PatchModelVariantState(string modelString, string? thinkingLevel)
    {
        thinkingLevel = NormalizeThinkingLevel(thinkingLevel);
        if (string.IsNullOrWhiteSpace(thinkingLevel)) return;

        var statePath = ResolveModelStatePath();
        var stateDir = Path.GetDirectoryName(statePath)!;
        Directory.CreateDirectory(stateDir);

        JsonNode root;
        try
        {
            if (File.Exists(statePath))
            {
                var raw = File.ReadAllText(statePath);
                root = string.IsNullOrWhiteSpace(raw) ? new JsonObject() : JsonNode.Parse(raw) ?? new JsonObject();
            }
            else
            {
                root = new JsonObject();
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenCodeLauncherService", "PatchModelVariantState", $"model.json konnte nicht gelesen werden, State wird neu aufgebaut: {ex.Message}", new { statePath });
            root = new JsonObject();
        }

        var obj = JsonExtensions.EnsureObject(root);
        var variants = obj.GetOrAddObject("variant");
        variants[modelString] = thinkingLevel;

        var tmp = statePath + ".tmp";
        using (var fs = new FileStream(tmp, FileMode.Create, FileAccess.Write, FileShare.None))
        using (var sw = new StreamWriter(fs, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false)))
        {
            sw.Write(obj.ToJsonString(JsonOpts));
        }
        File.Move(tmp, statePath, overwrite: true);
    }

    private JsonNode ReadConfig()
    {
        var configPath = ResolveConfigPath();
        if (!File.Exists(configPath))
            return new JsonObject();
        var raw = File.ReadAllText(configPath);
        // BOM-frei sicherstellen (Kurzcheck §10.3): BOM bricht Parse.
        if (raw.Length > 0 && raw[0] == '\uFEFF') raw = raw[1..];
        if (string.IsNullOrWhiteSpace(raw)) return new JsonObject();

        // JSONC direkt als JsonNode parsen. Nicht über GetRawText gehen: das liefert die
        // Original-Kommentare zurück und würde beim zweiten Parse wieder an "//" scheitern.
        return JsonNode.Parse(raw, null, new JsonDocumentOptions
        {
            CommentHandling = JsonCommentHandling.Skip,
            AllowTrailingCommas = true
        }) ?? new JsonObject();
    }

    private void WriteConfig(JsonNode root)
    {
        var configPath = ResolveConfigPath();
        var dir = Path.GetDirectoryName(configPath)!;
        Directory.CreateDirectory(dir);
        // Backup (.bak überschreibt vorherigen Lauf — bewusst, nur ein Rollback-Punkt nötig).
        if (File.Exists(configPath)) File.Copy(configPath, configPath + ".bak", overwrite: true);

        // Temp-Datei + atomares Replace (verhindert korrupte Config bei Absturz mitten im Schreiben).
        var tmp = configPath + ".tmp";
        using (var fs = new FileStream(tmp, FileMode.Create, FileAccess.Write, FileShare.None))
        using (var sw = new StreamWriter(fs, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false)))
        {
            sw.Write(root.ToJsonString(JsonOpts));
        }
        File.Move(tmp, configPath, overwrite: true);
    }

    private static string ResolveConfigPath()
    {
        var jsonc = Path.Combine(ConfigDir, "opencode.jsonc");
        if (File.Exists(jsonc)) return jsonc;

        var json = Path.Combine(ConfigDir, "opencode.json");
        return json;
    }

    private static JsonNode PatchProvider(JsonNode root, string slug, string modelDisplayName, ProviderEntry chosen, IReadOnlyList<ProviderEntry> all, string? thinkingLevel)
    {
        // order enthält bewusst nur den gewählten Provider: Frank will exaktes Routing ohne Fallback.
        var order = new JsonArray { chosen.ProviderSlug };

        var providerBlock = new JsonObject
        {
            ["order"] = order,
            ["allow_fallbacks"] = false,
            ["require_parameters"] = true,
        };
        var optionsBlock = new JsonObject { ["provider"] = providerBlock };
        ClearThinkingOptions(optionsBlock);
        ApplyThinkingOptions(optionsBlock, "openrouter", slug, thinkingLevel);

        // EnsureObject-Ergebnis zurückgeben, NICHT root: ist root ausnahmsweise kein Objekt
        // (korrupte opencode.json als Array/Skalar), liefert EnsureObject ein neues, losgelöstes
        // Objekt — ein "return root" würde die Datei unverändert zurückschreiben und den Patch
        // still verwerfen. Für gültige Objekt-Configs ist rootObject identisch mit root.
        var rootObject = JsonExtensions.EnsureObject(root);
        var models = rootObject.GetOrAddObject("provider").GetOrAddObject("openrouter").GetOrAddObject("models");
        var modelNode = models.GetOrAddObject(slug);
        modelNode["name"] = $"{modelDisplayName} via {chosen.ProviderName}";
        modelNode["options"] = optionsBlock;

        return rootObject;
    }

    private static JsonNode PatchDirectModel(JsonNode root, string providerId, string slug, string modelDisplayName, string? thinkingLevel)
    {
        // Siehe PatchProvider: EnsureObject-Ergebnis verwenden und zurückgeben, damit der Patch
        // auch bei einer nicht-objektförmigen Wurzel greift statt still verloren zu gehen.
        var rootObject = JsonExtensions.EnsureObject(root);
        var models = rootObject.GetOrAddObject("provider").GetOrAddObject(providerId).GetOrAddObject("models");
        var modelNode = models.GetOrAddObject(slug);
        modelNode["name"] = modelDisplayName;

        var optionsBlock = modelNode.GetOrAddObject("options");
        // OpenCode variants own the active reasoning level. Keeping reasoningEffort in model
        // options pins every request and prevents in-session variant changes from taking effect.
        ClearThinkingOptions(optionsBlock);
        if (IsGpt56Model(providerId, slug)) NormalizeExistingGpt56Models(models);
        return rootObject;
    }

    private static void NormalizeExistingGpt56Models(JsonObject models)
    {
        NormalizeModelPair(models, Gpt56SolSlug, Gpt56SolFastSlug, "GPT-5.6 Sol");
        NormalizeModelPair(models, Gpt56TerraSlug, Gpt56TerraFastSlug, "GPT-5.6 Terra");
        NormalizeModelPair(models, Gpt56LunaSlug, Gpt56LunaFastSlug, "GPT-5.6 Luna");
    }

    private static void NormalizeModelPair(JsonObject models, string normalSlug, string fastSlug, string displayName)
    {
        if (models[normalSlug] is JsonObject normal)
        {
            normal["name"] = displayName;
            normal.Remove("id");
            var normalOptions = normal.GetOrAddObject("options");
            normalOptions.Remove("serviceTier");
            ClearThinkingOptions(normalOptions);
        }

        if (models[fastSlug] is JsonObject fast)
        {
            fast["id"] = normalSlug;
            fast["name"] = $"{displayName} Fast";
            var fastOptions = fast.GetOrAddObject("options");
            ClearThinkingOptions(fastOptions);
            fastOptions["serviceTier"] = "priority";
        }
    }

    private static bool UsesPriorityServiceTier(string providerId, string slug)
    {
        if (!string.Equals(providerId, "openai", StringComparison.OrdinalIgnoreCase)) return false;

        var modelId = slug.Trim();
        if (modelId.StartsWith("openai/", StringComparison.OrdinalIgnoreCase)) modelId = modelId["openai/".Length..];
        return string.Equals(modelId, Gpt56SolFastSlug, StringComparison.OrdinalIgnoreCase) ||
               string.Equals(modelId, Gpt56TerraFastSlug, StringComparison.OrdinalIgnoreCase) ||
               string.Equals(modelId, Gpt56LunaFastSlug, StringComparison.OrdinalIgnoreCase);
    }

    private static bool IsGpt56Model(string providerId, string slug)
    {
        if (!string.Equals(providerId, "openai", StringComparison.OrdinalIgnoreCase)) return false;

        var modelId = slug.Trim();
        if (modelId.StartsWith("openai/", StringComparison.OrdinalIgnoreCase)) modelId = modelId["openai/".Length..];
        return string.Equals(modelId, Gpt56SolSlug, StringComparison.OrdinalIgnoreCase) ||
               string.Equals(modelId, Gpt56SolFastSlug, StringComparison.OrdinalIgnoreCase) ||
               string.Equals(modelId, Gpt56TerraSlug, StringComparison.OrdinalIgnoreCase) ||
               string.Equals(modelId, Gpt56TerraFastSlug, StringComparison.OrdinalIgnoreCase) ||
               string.Equals(modelId, Gpt56LunaSlug, StringComparison.OrdinalIgnoreCase) ||
               string.Equals(modelId, Gpt56LunaFastSlug, StringComparison.OrdinalIgnoreCase);
    }

    private static void ApplyThinkingOptions(JsonObject optionsBlock, string providerId, string slug, string? thinkingLevel)
    {
        thinkingLevel = NormalizeThinkingLevel(thinkingLevel);
        if (string.IsNullOrWhiteSpace(thinkingLevel)) return;

        var id = slug.Trim().ToLowerInvariant();
        if (string.Equals(providerId, "openai", StringComparison.OrdinalIgnoreCase)
            || id.StartsWith("openai/", StringComparison.Ordinal)
            || id.Contains("gpt", StringComparison.Ordinal))
        {
            optionsBlock["reasoningEffort"] = thinkingLevel;
            return;
        }

        optionsBlock["reasoning"] = new JsonObject { ["effort"] = thinkingLevel };
    }

    private static void ClearThinkingOptions(JsonObject optionsBlock)
    {
        optionsBlock.Remove("reasoningEffort");
        optionsBlock.Remove("reasoningSummary");
        optionsBlock.Remove("reasoning");
        optionsBlock.Remove("thinking");
        optionsBlock.Remove("effort");
    }
}

internal sealed record TerminalTabColor(string Name, string Hex);

internal sealed record PowerShellExecutable(string Path, bool IsPwsh);

internal sealed class TabColorState
{
    public string? LastColor { get; set; }
    public List<string> Remaining { get; set; } = [];
}

internal static class JsonExtensions
{
    public static JsonObject GetOrAddObject(this JsonObject obj, string name)
    {
        if (obj.TryGetPropertyValue(name, out var n) && n is JsonObject o) return o;
        var created = new JsonObject();
        obj[name] = created;
        return created;
    }

    public static JsonObject EnsureObject(this JsonNode? node)
    {
        if (node is JsonObject o) return o;
        return new JsonObject();
    }

    public static JsonObject GetOrAddObject(this JsonNode? node, string name) => node.EnsureObject().GetOrAddObject(name);
}
