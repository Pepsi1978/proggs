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
    public string ConfigureProvider(string slug, ProviderEntry chosen, IReadOnlyList<ProviderEntry> allProviders)
    {
        var log = Logger.Instance;
        var modelString = $"openrouter/{slug}";

        try
        {
            var root = ReadConfig();
            root = PatchProvider(root, slug, chosen, allProviders);
            WriteConfig(root);
            log.Info("OpenCodeLauncherService", "ConfigureProvider",
                $"opencode-Konfig gepatched: {slug} via {chosen.ProviderName}",
                new { order = new[] { chosen.ProviderSlug } });
        }
        catch (Exception ex)
        {
            log.Error("OpenCodeLauncherService", "ConfigureProvider", ex.Message, new { slug, chosen.ProviderName });
            throw;
        }

        return modelString;
    }

    /// <summary>Startet opencode in einem neuen Windows-Terminal-Fenster.</summary>
    public void Launch(string modelString, string workDir)
    {
        var log = Logger.Instance;
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
                psi.FileName = wt;
                // -d = Arbeitsverzeichnis; pwsh -NoExit hält das Fenster nach opencode-Beginn offen.
                psi.Arguments = $"-d \"{workDir}\" pwsh -NoExit -Command \"opencode -m {modelString}\"";
            }
            else
            {
                psi.FileName = "pwsh";
                psi.Arguments = $"-NoExit -Command \"Set-Location -LiteralPath '{workDir}'; opencode -m {modelString}\"";
                psi.UseShellExecute = true;
            }

            var p = Process.Start(psi);
            log.Info("OpenCodeLauncherService", "Launch", $"opencode gestartet (PID {p?.Id})", new { modelString, workDir, wtUsed = wt != null });
        }
        catch (Exception ex)
        {
            log.Error("OpenCodeLauncherService", "Launch", ex.Message, new { modelString, workDir });
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

    private JsonNode ReadConfig()
    {
        var configPath = ResolveConfigPath();
        if (!File.Exists(configPath))
            return new JsonObject();
        var raw = File.ReadAllText(configPath);
        // BOM-frei sicherstellen (Kurzcheck §10.3): BOM bricht Parse.
        if (raw.Length > 0 && raw[0] == '\uFEFF') raw = raw[1..];
        if (string.IsNullOrWhiteSpace(raw)) return new JsonObject();

        // opencode.jsonc enthält // Kommentare -> JsonNode.Parse kennt kein CommentHandling,
        // darum über JsonDocument parsen (JsonDocumentOptions unterstützt CommentHandling)
        // und anschließend in eine mutable JsonNode konvertieren.
        using var doc = JsonDocument.Parse(raw, new JsonDocumentOptions
        {
            CommentHandling = JsonCommentHandling.Skip,
            AllowTrailingCommas = true
        });
        return JsonNode.Parse(doc.RootElement.GetRawText()) ?? new JsonObject();
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

    private static JsonNode PatchProvider(JsonNode root, string slug, ProviderEntry chosen, IReadOnlyList<ProviderEntry> all)
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

        var models = JsonExtensions.EnsureObject(root).GetOrAddObject("provider").GetOrAddObject("openrouter").GetOrAddObject("models");
        var modelNode = models.GetOrAddObject(slug);
        modelNode["name"] = $"{chosen.ProviderName} via OpenRouter";
        modelNode["options"] = optionsBlock;

        return root;
    }
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
