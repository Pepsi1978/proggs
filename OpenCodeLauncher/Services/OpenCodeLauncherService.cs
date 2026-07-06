using System.Diagnostics;
using System.IO;
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
                PatchDirectThinking(root, model.ProviderId, model.Slug, model.DisplayName, thinkingLevel);
                WriteConfig(root);
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
    public void Launch(string modelString, string workDir, string? thinkingLevel)
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
                var robustLauncherScript = ResolveRobustLauncherScript();
                if (!string.IsNullOrEmpty(robustLauncherScript))
                {
                    var robustProcess = LaunchViaRobustPowerShell(wt, robustLauncherScript, modelString, workDir, thinkingLevel, tabColor, log);
                    log.Info("OpenCodeLauncherService", "Launch", $"robuster Windows-Terminal-Launcher gestartet (PID {robustProcess?.Id})", new { modelString, workDir, thinkingLevel, tabColor = tabColor.Name });
                    return;
                }

                log.Warn("OpenCodeLauncherService", "Launch", "start-wt-common.ps1 nicht gefunden; nutze direkten Windows-Terminal-Start ohne Retry-Wrapper");
                psi.FileName = wt;
                psi.ArgumentList.Add("new-tab");
                psi.ArgumentList.Add("--tabColor");
                psi.ArgumentList.Add(tabColor.Hex);
                psi.ArgumentList.Add("--title");
                psi.ArgumentList.Add(string.IsNullOrWhiteSpace(thinkingLevel) ? $"OpenCode-{tabColor.Name}" : $"OpenCode-{tabColor.Name}-{thinkingLevel}");
                psi.ArgumentList.Add("--startingDirectory");
                psi.ArgumentList.Add(workDir);
                psi.ArgumentList.Add("pwsh");
                psi.ArgumentList.Add("-NoExit");
                psi.ArgumentList.Add("-Command");
                psi.ArgumentList.Add(BuildOpenCodeCommand(modelString, thinkingLevel));
                log.Info("OpenCodeLauncherService", "Launch", $"Terminal-Tabfarbe gewählt: {tabColor.Name} ({tabColor.Hex})");
            }
            else
            {
                psi.FileName = "pwsh";
                psi.ArgumentList.Add("-NoExit");
                psi.ArgumentList.Add("-Command");
                psi.ArgumentList.Add($"Set-Location -LiteralPath '{EscapePowerShellSingleQuotedValue(workDir)}'; {BuildOpenCodeCommand(modelString, thinkingLevel)}");
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

    private static TerminalTabColor PickTerminalTabColor() => TerminalTabColors[Random.Shared.Next(TerminalTabColors.Length)];

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
        string modelString,
        string workDir,
        string? thinkingLevel,
        TerminalTabColor tabColor,
        Logger log)
    {
        var title = string.IsNullOrWhiteSpace(thinkingLevel) ? $"OpenCode-{tabColor.Name}" : $"OpenCode-{tabColor.Name}-{thinkingLevel}";
        var command = BuildOpenCodeCommand(modelString, thinkingLevel);
        var tabArgs = new[]
        {
            "new-tab",
            "--tabColor", tabColor.Hex,
            "--title", title,
            "--startingDirectory", workDir,
            "pwsh.exe",
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
    $ok = Start-WtCliRobust -LogFile {{PowerShellLiteral(log.LogPath)}} -WtPath {{PowerShellLiteral(wtPath)}} -TabArgs $tabArgs -InnerMatch 'opencode\s+-m' -FallbackPwshArgs $fallbackArgs -FallbackWorkDir {{PowerShellLiteral(workDir)}}
    if (-not $ok) { exit 2 }
} finally {
    Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue
}
""";
        File.WriteAllText(tempScript, script, new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));

        var pwshExe = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PowerShell", "7", "pwsh.exe");
        if (!File.Exists(pwshExe)) pwshExe = "pwsh.exe";
        var psi = new ProcessStartInfo
        {
            FileName = pwshExe,
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

    private static string PowerShellArrayLiteral(IEnumerable<string> values) => string.Join(", ", values.Select(PowerShellLiteral));

    private static string PowerShellLiteral(string value) => $"'{EscapePowerShellSingleQuotedValue(value)}'";

    private static string BuildOpenCodeCommand(string modelString, string? thinkingLevel)
    {
        // Der interaktive TUI-Start akzeptiert kein --variant; Thinking wird vorab in opencode.json(c) geschrieben.
        return $"opencode -m '{EscapePowerShellSingleQuotedValue(modelString)}'";
    }

    private static string? NormalizeThinkingLevel(string? thinkingLevel) => string.IsNullOrWhiteSpace(thinkingLevel)
        ? null
        : thinkingLevel.Trim().ToLowerInvariant();

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
        ApplyThinkingOptions(optionsBlock, thinkingLevel, includeOpenRouterReasoningObject: true);

        var models = JsonExtensions.EnsureObject(root).GetOrAddObject("provider").GetOrAddObject("openrouter").GetOrAddObject("models");
        var modelNode = models.GetOrAddObject(slug);
        modelNode["name"] = $"{modelDisplayName} via {chosen.ProviderName}";
        modelNode["options"] = optionsBlock;

        return root;
    }

    private static void PatchDirectThinking(JsonNode root, string providerId, string slug, string modelDisplayName, string thinkingLevel)
    {
        var models = JsonExtensions.EnsureObject(root).GetOrAddObject("provider").GetOrAddObject(providerId).GetOrAddObject("models");
        var modelNode = models.GetOrAddObject(slug);
        modelNode["name"] = modelDisplayName;

        var optionsBlock = modelNode.GetOrAddObject("options");
        ApplyThinkingOptions(optionsBlock, thinkingLevel, includeOpenRouterReasoningObject: false);
    }

    private static void ApplyThinkingOptions(JsonObject optionsBlock, string? thinkingLevel, bool includeOpenRouterReasoningObject)
    {
        if (string.IsNullOrWhiteSpace(thinkingLevel))
        {
            optionsBlock.Remove("reasoningEffort");
            optionsBlock.Remove("reasoning");
            return;
        }

        optionsBlock["reasoningEffort"] = thinkingLevel;
        if (includeOpenRouterReasoningObject)
        {
            optionsBlock["reasoning"] = new JsonObject
            {
                ["effort"] = thinkingLevel
            };
        }
    }
}

internal sealed record TerminalTabColor(string Name, string Hex);

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
