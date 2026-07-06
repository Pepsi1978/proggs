using System.IO;
using System.Collections.ObjectModel;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using OpenCodeLauncher.Models;

namespace OpenCodeLauncher.Services;

/// <summary>
/// Persistente Liste der gepflegten Modelle (JSON in %AppData%/OpenCodeLauncher).
/// Beim ersten Start werden Default-Gruppen angelegt (Slugs verifiziert gegen
/// GET /api/v1/models). Reihenfolge per Drag&Drop änderbar,
/// neue Modelle per AddModel, entfernen per RemoveModel.
/// </summary>
public sealed class ModelRegistry
{
    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenCodeLauncher");
    private static readonly string FilePath = Path.Combine(Dir, "models.json");

    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
        TypeInfoResolver = new DefaultJsonTypeInfoResolver()
    };

    public ObservableCollection<ModelGroupEntry> Groups { get; private set; } = new();

    public static ModelRegistry Load()
    {
        var reg = new ModelRegistry();
        try
        {
            if (File.Exists(FilePath))
            {
                var json = File.ReadAllText(FilePath);
                if (json.TrimStart().StartsWith("["))
                {
                    using var doc = JsonDocument.Parse(json);
                    var isGroupFile = doc.RootElement.ValueKind == JsonValueKind.Array &&
                                      doc.RootElement.GetArrayLength() > 0 &&
                                      doc.RootElement[0].TryGetProperty("Models", out _);
                    if (isGroupFile)
                    {
                        var parsedGroups = JsonSerializer.Deserialize<ObservableCollection<ModelGroupEntry>>(json, JsonOpts);
                        if (parsedGroups != null && parsedGroups.Count > 0)
                        {
                            reg.Groups = parsedGroups;
                            reg.RepairAndNormalize();
                            reg.Save();
                            Logger.Instance.Info("ModelRegistry", "Load", $"{parsedGroups.Count} Modellgruppen geladen", new { FilePath });
                            return reg;
                        }
                    }

                    var oldList = JsonSerializer.Deserialize<List<ModelEntry>>(json, JsonOpts);
                    if (oldList != null && oldList.Any(m => !string.IsNullOrWhiteSpace(m.Slug)))
                    {
                        reg.Groups = CreateDefaults(oldList);
                        reg.Save();
                        Logger.Instance.Info("ModelRegistry", "Load", $"{oldList.Count} Modelle aus alter Liste migriert", new { FilePath });
                        return reg;
                    }
                }

                var groups = JsonSerializer.Deserialize<ObservableCollection<ModelGroupEntry>>(json, JsonOpts);
                if (groups != null && groups.Count > 0)
                {
                    reg.Groups = groups;
                    reg.RepairAndNormalize();
                    reg.Save();
                    Logger.Instance.Info("ModelRegistry", "Load", $"{groups.Count} Modellgruppen geladen", new { FilePath });
                    return reg;
                }
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ModelRegistry", "Load", $"Laden fehlgeschlagen, Defaults: {ex.Message}");
        }

        reg.Groups = CreateDefaults();
        reg.Save();
        return reg;
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            File.WriteAllText(FilePath, JsonSerializer.Serialize(Groups, JsonOpts));
        }
        catch (Exception ex)
        {
            Logger.Instance.Error("ModelRegistry", "Save", ex.Message);
        }
    }

    public bool AddModel(ModelGroupEntry group, string slug, string displayName)
    {
        slug = slug.Trim().ToLowerInvariant();
        if (group.Models.Any(m => string.Equals(m.Slug, slug, StringComparison.OrdinalIgnoreCase)))
        {
            Logger.Instance.Warn("ModelRegistry", "AddModel", $"Slug existiert bereits: {slug}");
            return false;
        }
        group.Models.Add(new ModelEntry
        {
            Slug = slug,
            DisplayName = string.IsNullOrWhiteSpace(displayName) ? ToDisplayName(slug) : displayName,
            ProviderId = group.ProviderId,
            ProviderName = group.ProviderName
        });
        Save();
        Logger.Instance.Info("ModelRegistry", "AddModel", $"hinzugefügt: {slug}", new { group = group.Title, displayName });
        return true;
    }

    public void RemoveAt(ModelGroupEntry group, int index)
    {
        if (index < 0 || index >= group.Models.Count) return;
        var m = group.Models[index];
        group.Models.RemoveAt(index);
        if (string.Equals(group.Id, "openrouter-free", StringComparison.OrdinalIgnoreCase))
        {
            AddUnique(group.HiddenModelSlugs, m.Slug);
            AddUnique(group.KnownSyncedModelSlugs, m.Slug);
        }
        Save();
        Logger.Instance.Info("ModelRegistry", "RemoveAt", $"entfernt: {m.Slug}", new { group = group.Title });
    }

    public void MoveModel(ModelGroupEntry group, int from, int to)
    {
        if (from < 0 || from >= group.Models.Count) return;
        if (to < 0) to = 0;
        if (to > group.Models.Count) to = group.Models.Count;
        if (from == to) return;
        var item = group.Models[from];
        group.Models.RemoveAt(from);
        if (to > from) to--;
        group.Models.Insert(Math.Clamp(to, 0, group.Models.Count), item);
        Save();
        Logger.Instance.Info("ModelRegistry", "MoveModel", $"{group.Title}:{from} -> {to}");
    }

    public void MoveGroup(int from, int to)
    {
        if (from < 0 || from >= Groups.Count || to < 0 || to >= Groups.Count || from == to) return;
        var item = Groups[from];
        Groups.RemoveAt(from);
        Groups.Insert(to, item);
        Save();
        Logger.Instance.Info("ModelRegistry", "MoveGroup", $"{from} -> {to}");
    }

    public void ReplaceGroupModels(string groupId, IEnumerable<ModelEntry> models)
    {
        var group = Groups.FirstOrDefault(g => string.Equals(g.Id, groupId, StringComparison.OrdinalIgnoreCase));
        if (group == null) return;

        group.Models.Clear();
        foreach (var model in models)
        {
            model.ProviderId = group.ProviderId;
            model.ProviderName = group.ProviderName;
            if (model.Slug.StartsWith("openrouter/", StringComparison.OrdinalIgnoreCase))
                model.Slug = model.Slug["openrouter/".Length..];
            if (string.IsNullOrWhiteSpace(model.DisplayName)) model.DisplayName = ToDisplayName(model.Slug);
            group.Models.Add(model);
        }

        Save();
        group.RefreshHeaderText();
        Logger.Instance.Info("ModelRegistry", "ReplaceGroupModels", $"{group.Title}: {group.Models.Count} Modelle aktualisiert");
    }

    public void SyncOpenRouterFreeModels(IEnumerable<ModelEntry> remoteModels)
    {
        var group = Groups.FirstOrDefault(g => string.Equals(g.Id, "openrouter-free", StringComparison.OrdinalIgnoreCase));
        if (group == null) return;

        var remote = remoteModels
            .Where(m => !string.IsNullOrWhiteSpace(m.Slug))
            .GroupBy(m => m.Slug, StringComparer.OrdinalIgnoreCase)
            .Select(g => NormalizeModel(g.First(), group.ProviderId, group.ProviderName))
            .ToList();
        if (remote.Count == 0) return;

        var currentSlugs = group.Models.Select(m => m.Slug).ToHashSet(StringComparer.OrdinalIgnoreCase);
        if (group.KnownSyncedModelSlugs.Count == 0)
        {
            foreach (var slug in currentSlugs) AddUnique(group.KnownSyncedModelSlugs, slug);
        }

        var knownSlugs = group.KnownSyncedModelSlugs.ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var removedSlug in knownSlugs.Where(slug => !currentSlugs.Contains(slug)))
            AddUnique(group.HiddenModelSlugs, removedSlug);

        var hiddenSlugs = group.HiddenModelSlugs.ToHashSet(StringComparer.OrdinalIgnoreCase);
        var remoteBySlug = remote.ToDictionary(m => m.Slug, StringComparer.OrdinalIgnoreCase);
        var merged = new List<ModelEntry>();

        foreach (var existing in group.Models)
        {
            if (hiddenSlugs.Contains(existing.Slug)) continue;
            if (!remoteBySlug.TryGetValue(existing.Slug, out var remoteModel)) continue;
            existing.ProviderId = group.ProviderId;
            existing.ProviderName = group.ProviderName;
            existing.DisplayName = remoteModel.DisplayName;
            merged.Add(existing);
        }

        var mergedSlugs = merged.Select(m => m.Slug).ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var remoteModel in remote)
        {
            if (hiddenSlugs.Contains(remoteModel.Slug) || mergedSlugs.Contains(remoteModel.Slug)) continue;
            merged.Add(remoteModel);
            mergedSlugs.Add(remoteModel.Slug);
        }

        group.Models.Clear();
        foreach (var model in merged) group.Models.Add(model);
        group.KnownSyncedModelSlugs = remote.Select(m => m.Slug).Distinct(StringComparer.OrdinalIgnoreCase).ToList();
        group.HiddenModelSlugs = group.HiddenModelSlugs.Distinct(StringComparer.OrdinalIgnoreCase).ToList();

        Save();
        group.RefreshHeaderText();
        Logger.Instance.Info("ModelRegistry", "SyncOpenRouterFreeModels", $"{group.Models.Count} sichtbare, {group.HiddenModelSlugs.Count} ausgeblendete OpenRouterFree-Modelle");
    }

    private void RepairAndNormalize()
    {
        foreach (var defaults in CreateDefaults())
        {
            var group = Groups.FirstOrDefault(g => string.Equals(g.Id, defaults.Id, StringComparison.OrdinalIgnoreCase));
            if (group == null)
            {
                Groups.Add(defaults);
                continue;
            }
            group.ProviderId = defaults.ProviderId;
            group.ProviderName = defaults.ProviderName;
            if (group.Models.Count == 0 || group.Models.All(m => string.IsNullOrWhiteSpace(m.Slug)))
            {
                group.Models.Clear();
                foreach (var model in defaults.Models) group.Models.Add(model);
                continue;
            }

            var cleaned = group.Models
                .Where(m => !string.IsNullOrWhiteSpace(m.Slug))
                .GroupBy(m => m.Slug, StringComparer.OrdinalIgnoreCase)
                .Select(g => g.First())
                .ToList();
            group.Models.Clear();
            foreach (var model in cleaned) group.Models.Add(model);

            foreach (var model in group.Models)
            {
                model.ProviderId = group.ProviderId;
                model.ProviderName = group.ProviderName;
                if (string.IsNullOrWhiteSpace(model.DisplayName)) model.DisplayName = ToDisplayName(model.Slug);
            }
            group.HiddenModelSlugs = group.HiddenModelSlugs
                .Where(slug => !string.IsNullOrWhiteSpace(slug))
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .ToList();
            group.KnownSyncedModelSlugs = group.KnownSyncedModelSlugs
                .Where(slug => !string.IsNullOrWhiteSpace(slug))
                .Distinct(StringComparer.OrdinalIgnoreCase)
                .ToList();
        }
    }

    private static ObservableCollection<ModelGroupEntry> CreateDefaults(IEnumerable<ModelEntry>? openRouterModels = null) => new()
    {
        CreateGroup("openrouter", "OpenRouter", "openrouter", "OpenRouter", NormalizeOpenRouter(openRouterModels).ToArray()),
        CreateGroup("entropic", "Entropic", "anthropic", "Entropic", new[]
        {
            Model("claude-fable-5", "Claude Fable 5", "anthropic", "Entropic"),
            Model("claude-opus-4-8", "Claude Opus 4.8", "anthropic", "Entropic"),
            Model("claude-sonnet-5", "Claude Sonnet 5", "anthropic", "Entropic"),
            Model("claude-haiku-4-5", "Claude Haiku 4.5", "anthropic", "Entropic"),
            Model("claude-opus-4-7", "Claude Opus 4.7", "anthropic", "Entropic"),
            Model("claude-opus-4-6", "Claude Opus 4.6", "anthropic", "Entropic"),
            Model("claude-sonnet-4-6", "Claude Sonnet 4.6", "anthropic", "Entropic"),
            Model("claude-opus-4-5", "Claude Opus 4.5", "anthropic", "Entropic"),
            Model("claude-sonnet-4-5", "Claude Sonnet 4.5", "anthropic", "Entropic"),
        }),
        CreateGroup("openrouter-free", "OpenRouterFree", "openrouter", "OpenRouter", new[]
        {
            Model("poolside/laguna-xs-2.1:free", "Poolside Laguna XS 2.1 Free", "openrouter", "OpenRouter"),
            Model("cohere/north-mini-code:free", "Cohere North Mini Code Free", "openrouter", "OpenRouter"),
            Model("nvidia/nemotron-3.5-content-safety:free", "NVIDIA Nemotron 3.5 Content Safety Free", "openrouter", "OpenRouter"),
            Model("nvidia/nemotron-3-ultra-550b-a55b:free", "NVIDIA Nemotron 3 Ultra Free", "openrouter", "OpenRouter"),
            Model("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", "NVIDIA Nemotron 3 Nano Omni Free", "openrouter", "OpenRouter"),
            Model("poolside/laguna-xs.2:free", "Poolside Laguna XS.2 Free", "openrouter", "OpenRouter"),
            Model("poolside/laguna-m.1:free", "Poolside Laguna M.1 Free", "openrouter", "OpenRouter"),
            Model("google/gemma-4-26b-a4b-it:free", "Google Gemma 4 26B A4B Free", "openrouter", "OpenRouter"),
            Model("google/gemma-4-31b-it:free", "Google Gemma 4 31B Free", "openrouter", "OpenRouter"),
            Model("nvidia/nemotron-3-super-120b-a12b:free", "NVIDIA Nemotron 3 Super Free", "openrouter", "OpenRouter"),
            Model("liquid/lfm-2.5-1.2b-thinking:free", "LiquidAI LFM2.5 1.2B Thinking Free", "openrouter", "OpenRouter"),
            Model("liquid/lfm-2.5-1.2b-instruct:free", "LiquidAI LFM2.5 1.2B Instruct Free", "openrouter", "OpenRouter"),
            Model("nvidia/nemotron-3-nano-30b-a3b:free", "NVIDIA Nemotron 3 Nano 30B A3B Free", "openrouter", "OpenRouter"),
            Model("nvidia/nemotron-nano-12b-v2-vl:free", "NVIDIA Nemotron Nano 12B 2 VL Free", "openrouter", "OpenRouter"),
            Model("qwen/qwen3-next-80b-a3b-instruct:free", "Qwen3 Next 80B A3B Instruct Free", "openrouter", "OpenRouter"),
            Model("nvidia/nemotron-nano-9b-v2:free", "NVIDIA Nemotron Nano 9B V2 Free", "openrouter", "OpenRouter"),
            Model("openai/gpt-oss-120b:free", "OpenAI GPT OSS 120B Free", "openrouter", "OpenRouter"),
            Model("openai/gpt-oss-20b:free", "OpenAI GPT OSS 20B Free", "openrouter", "OpenRouter"),
            Model("qwen/qwen3-coder:free", "Qwen3 Coder Free", "openrouter", "OpenRouter"),
            Model("cognitivecomputations/dolphin-mistral-24b-venice-edition:free", "Venice Uncensored Free", "openrouter", "OpenRouter"),
            Model("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B Instruct Free", "openrouter", "OpenRouter"),
            Model("meta-llama/llama-3.2-3b-instruct:free", "Llama 3.2 3B Instruct Free", "openrouter", "OpenRouter"),
            Model("nousresearch/hermes-3-llama-3.1-405b:free", "Hermes 3 405B Instruct Free", "openrouter", "OpenRouter"),
        }),
        CreateGroup("opencode-zen-free", "OpenCode Zen Free", "opencode", "OpenCode Zen", new[]
        {
            Model("gpt-5-nano", "GPT-5 Nano", "opencode", "OpenCode Zen"),
            Model("deepseek-v4-flash-free", "DeepSeek V4 Flash Free", "opencode", "OpenCode Zen"),
            Model("mimo-v2.5-free", "MiMo V2.5 Free", "opencode", "OpenCode Zen"),
            Model("nemotron-3-ultra-free", "Nemotron 3 Ultra Free", "opencode", "OpenCode Zen"),
            Model("north-mini-code-free", "North Mini Code Free", "opencode", "OpenCode Zen"),
        }),
        CreateGroup("openai", "OpenAI", "openai", "OpenAI", new[]
        {
            Model("gpt-5.3-codex-spark", "GPT-5.3 Codex Spark", "openai", "OpenAI"),
            Model("gpt-5.4", "GPT-5.4", "openai", "OpenAI"),
            Model("gpt-5.4-fast", "GPT-5.4 Fast", "openai", "OpenAI"),
            Model("gpt-5.4-mini", "GPT-5.4 Mini", "openai", "OpenAI"),
            Model("gpt-5.4-mini-fast", "GPT-5.4 Mini Fast", "openai", "OpenAI"),
            Model("gpt-5.5", "GPT-5.5", "openai", "OpenAI"),
            Model("gpt-5.5-fast", "GPT-5.5 Fast", "openai", "OpenAI"),
        }),
        CreateGroup("opencode-go", "OpenCode-Go", "opencode-go", "OpenCode-Go", new[]
        {
            Model("deepseek-v4-flash", "DeepSeek V4 Flash", "opencode-go", "OpenCode-Go"),
            Model("deepseek-v4-pro", "DeepSeek V4 Pro", "opencode-go", "OpenCode-Go"),
            Model("glm-5.1", "GLM 5.1", "opencode-go", "OpenCode-Go"),
            Model("glm-5.2", "GLM 5.2", "opencode-go", "OpenCode-Go"),
            Model("kimi-k2.6", "Kimi K2.6", "opencode-go", "OpenCode-Go"),
            Model("kimi-k2.7-code", "Kimi K2.7 Code", "opencode-go", "OpenCode-Go"),
            Model("mimo-v2.5", "MiMo V2.5", "opencode-go", "OpenCode-Go"),
            Model("mimo-v2.5-pro", "MiMo V2.5 Pro", "opencode-go", "OpenCode-Go"),
            Model("minimax-m2.7", "MiniMax M2.7", "opencode-go", "OpenCode-Go"),
            Model("minimax-m3", "MiniMax M3", "opencode-go", "OpenCode-Go"),
            Model("qwen3.6-plus", "Qwen3.6 Plus", "opencode-go", "OpenCode-Go"),
            Model("qwen3.7-max", "Qwen3.7 Max", "opencode-go", "OpenCode-Go"),
            Model("qwen3.7-plus", "Qwen3.7 Plus", "opencode-go", "OpenCode-Go"),
        }),
    };

    private static IEnumerable<ModelEntry> NormalizeOpenRouter(IEnumerable<ModelEntry>? source)
    {
        var models = source?.ToList() ?? new List<ModelEntry>
        {
            Model("z-ai/glm-5.2", "GLM 5.2", "openrouter", "OpenRouter"),
            Model("minimax/minimax-m3", "MiniMax M3", "openrouter", "OpenRouter"),
            Model("qwen/qwen3.7-max", "Qwen 3.7 Max", "openrouter", "OpenRouter"),
            Model("xiaomi/mimo-v2.5-pro", "MiMo V2.5 Pro", "openrouter", "OpenRouter"),
            Model("deepseek/deepseek-v4-pro", "DeepSeek V4 Pro", "openrouter", "OpenRouter"),
            Model("deepseek/deepseek-v4-flash", "DeepSeek V4 Flash", "openrouter", "OpenRouter"),
            Model("xiaomi/mimo-v2.5", "MiMo V2.5", "openrouter", "OpenRouter"),
        };
        foreach (var model in models)
        {
            model.ProviderId = "openrouter";
            model.ProviderName = "OpenRouter";
            if (model.Slug.StartsWith("openrouter/", StringComparison.OrdinalIgnoreCase))
                model.Slug = model.Slug["openrouter/".Length..];
            if (string.IsNullOrWhiteSpace(model.DisplayName)) model.DisplayName = ToDisplayName(model.Slug);
            yield return model;
        }
    }

    private static ModelGroupEntry CreateGroup(string id, string title, string providerId, string providerName, IReadOnlyList<ModelEntry> models) => new()
    {
        Id = id,
        Title = title,
        ProviderId = providerId,
        ProviderName = providerName,
        IsExpanded = true,
        Models = new ObservableCollection<ModelEntry>(models),
        KnownSyncedModelSlugs = string.Equals(id, "openrouter-free", StringComparison.OrdinalIgnoreCase)
            ? models.Select(m => m.Slug).ToList()
            : new List<string>()
    };

    private static ModelEntry NormalizeModel(ModelEntry model, string providerId, string providerName)
    {
        model.ProviderId = providerId;
        model.ProviderName = providerName;
        if (model.Slug.StartsWith($"{providerId}/", StringComparison.OrdinalIgnoreCase))
            model.Slug = model.Slug[$"{providerId}/".Length..];
        if (string.IsNullOrWhiteSpace(model.DisplayName)) model.DisplayName = ToDisplayName(model.Slug);
        return model;
    }

    private static void AddUnique(List<string> values, string value)
    {
        if (string.IsNullOrWhiteSpace(value)) return;
        if (!values.Contains(value, StringComparer.OrdinalIgnoreCase)) values.Add(value);
    }

    private static ModelEntry Model(string slug, string displayName, string providerId, string providerName) => new()
    {
        Slug = slug,
        DisplayName = displayName,
        ProviderId = providerId,
        ProviderName = providerName
    };

    private static string ToDisplayName(string slug)
    {
        var name = slug.Split('/').Last();
        return string.Join(" ", name.Split('-', StringSplitOptions.RemoveEmptyEntries).Select(part =>
            part.Length == 0 ? part : char.ToUpperInvariant(part[0]) + part[1..]));
    }
}
