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
            if (!string.IsNullOrWhiteSpace(thinkingLevel))
            {
                var root = ReadConfig();
                PatchDirectModel(root, model.ProviderId, model.Slug, model.DisplayName, thinkingLevel);
                WriteConfig(root);
                PatchModelVariantState(modelString, thinkingLevel);
                log.Info("OpenCodeLauncherService", "ConfigureProvider", $"Direktmodell-Thinking gesetzt: {model.ProviderId}/{model.Slug} -> {thinkingLevel}");
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
    public void Launch(string modelString, string workDir, string? thinkingLevel, string profileConfigPath)
    {
        var log = Logger.Instance;
        thinkingLevel = NormalizeThinkingLevel(thinkingLevel);
        try
        {
            Directory.CreateDirectory(workDir);

            // Bevorzugt Windows Terminal (AppX, evtl. nicht im PATH) — Fallback: eigene Konsole.
            var wt = ResolveWt();
            var psi = new ProcessStartInfo
            {
                UseShellExecute = false,
                CreateNoWindow = false,
                WorkingDirectory = workDir,
            };

            if (!string.IsNullOrEmpty(wt))
            {
                var tabColor = PickTerminalTabColor();
                var shell = ResolvePowerShellExecutable();
                var robustLauncherScript = shell.IsPwsh ? ResolveRobustLauncherScript() : null;
                if (!string.IsNullOrEmpty(robustLauncherScript))
                {
                    var robustProcess = LaunchViaRobustPowerShell(wt, robustLauncherScript, shell.Path, modelString, workDir, thinkingLevel, profileConfigPath, tabColor, log);
                    log.Info("OpenCodeLauncherService", "Launch", $"robuster Windows-Terminal-Launcher gestartet (PID {robustProcess?.Id})", new { modelString, workDir, thinkingLevel, tabColor = tabColor.Name });
                    return;
                }

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
                psi.ArgumentList.Add("-Command");
                psi.ArgumentList.Add(BuildOpenCodeCommand(modelString, thinkingLevel, profileConfigPath));
                log.Info("OpenCodeLauncherService", "Launch", $"Terminal-Tabfarbe gewählt: {tabColor.Name} ({tabColor.Hex})");
            }
            else
            {
                var shell = ResolvePowerShellExecutable();
                psi.FileName = shell.Path;
                psi.ArgumentList.Add("-NoExit");
                psi.ArgumentList.Add("-ExecutionPolicy");
                psi.ArgumentList.Add("Bypass");
                psi.ArgumentList.Add("-Command");
                psi.ArgumentList.Add($"Set-Location -LiteralPath '{EscapePowerShellSingleQuotedValue(workDir)}'; {BuildOpenCodeCommand(modelString, thinkingLevel, profileConfigPath)}");
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
    public void LaunchClaudeCode(string modelId, string workDir, string? effortLevel)
    {
        var log = Logger.Instance;
        effortLevel = NormalizeThinkingLevel(effortLevel);
        try
        {
            Directory.CreateDirectory(workDir);
            var wt = ResolveWt();
            var tabColor = PickClaudeTerminalTabColor();
            var innerScript = BuildClaudeCodeStartScript(modelId, workDir, effortLevel, tabColor.Name);
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

            var p = Process.Start(new ProcessStartInfo("where.exe", "wt") { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true });
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
            var p = Process.Start(new ProcessStartInfo("where.exe", "pwsh.exe") { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true });
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

    private static Process? LaunchViaRobustPowerShell(
        string wtPath,
        string robustLauncherScript,
        string powerShellPath,
        string modelString,
        string workDir,
        string? thinkingLevel,
        string profileConfigPath,
        TerminalTabColor tabColor,
        Logger log)
    {
        var title = string.IsNullOrWhiteSpace(thinkingLevel) ? $"OpenCode-{tabColor.Name}" : $"OpenCode-{tabColor.Name}-{thinkingLevel}";
        var command = BuildOpenCodeCommand(modelString, thinkingLevel, profileConfigPath);
        var tabArgs = new[]
        {
            "new-tab",
            "--tabColor", tabColor.Hex,
            "--title", title,
            "--startingDirectory", workDir,
            powerShellPath,
            "-NoExit",
            "-ExecutionPolicy", "Bypass",
            "-Command", command
        };
        var fallbackArgs = new[]
        {
            "-NoExit",
            "-ExecutionPolicy", "Bypass",
            "-Command", command
        };

        var tempScript = Path.Combine(Path.GetTempPath(), $"opencode-launcher-wt-{Guid.NewGuid():N}.ps1");
        var script = $$"""
$ErrorActionPreference = 'Continue'
. {{PowerShellLiteral(robustLauncherScript)}}
$tabArgs = @({{PowerShellArrayLiteral(tabArgs)}})
$fallbackArgs = @({{PowerShellArrayLiteral(fallbackArgs)}})
try {
    $ok = Start-WtCliRobust -LogFile {{PowerShellLiteral(log.LogPath)}} -WtPath {{PowerShellLiteral(wtPath)}} -TabArgs $tabArgs -InnerMatch {{PowerShellLiteral("opencode(?:\\.exe)?['\"]?\\s+-m")}} -FallbackPwshArgs $fallbackArgs -FallbackWorkDir {{PowerShellLiteral(workDir)}}
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

    private static string BuildClaudeCodeStartScript(string modelId, string workDir, string? effortLevel, string colorName)
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

    private static string BuildOpenCodeCommand(string modelString, string? thinkingLevel, string profileConfigPath)
    {
        var executable = ResolveOpenCodeExecutable();
        var command = Path.IsPathFullyQualified(executable)
            ? $"& '{EscapePowerShellSingleQuotedValue(executable)}'"
            : executable;
        var variant = Path.IsPathFullyQualified(executable) && !string.IsNullOrWhiteSpace(thinkingLevel)
            ? $" --variant '{EscapePowerShellSingleQuotedValue(thinkingLevel)}'"
            : string.Empty;
        var config = EscapePowerShellSingleQuotedValue(profileConfigPath);
        return $"$env:OPENCODE_CONFIG = '{config}'; {command} -m '{EscapePowerShellSingleQuotedValue(modelString)}'{variant}";
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

        var models = JsonExtensions.EnsureObject(root).GetOrAddObject("provider").GetOrAddObject("openrouter").GetOrAddObject("models");
        var modelNode = models.GetOrAddObject(slug);
        modelNode["name"] = $"{modelDisplayName} via {chosen.ProviderName}";
        modelNode["options"] = optionsBlock;

        return root;
    }

    private static void PatchDirectModel(JsonNode root, string providerId, string slug, string modelDisplayName, string? thinkingLevel)
    {
        var models = JsonExtensions.EnsureObject(root).GetOrAddObject("provider").GetOrAddObject(providerId).GetOrAddObject("models");
        var modelNode = models.GetOrAddObject(slug);
        modelNode["name"] = modelDisplayName;

        var optionsBlock = modelNode.GetOrAddObject("options");
        ClearThinkingOptions(optionsBlock);
        ApplyThinkingOptions(optionsBlock, providerId, slug, thinkingLevel);
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
