using System.IO;
using System.Collections.ObjectModel;
using System.Text.Json;
using System.Text.Json.Serialization.Metadata;
using OpenLauncher.Models;

namespace OpenLauncher.Services;

/// <summary>
/// Persistente Liste der gepflegten Modelle (JSON in %AppData%/OpenLauncher).
/// Beim ersten Start werden Default-Gruppen angelegt (Slugs verifiziert gegen
/// GET /api/v1/models). Reihenfolge per Drag&Drop änderbar,
/// neue Modelle per AddModel, entfernen per RemoveModel.
/// </summary>
public sealed class ModelRegistry
{
    private const string Gpt56SolSlug = "gpt-5.6-sol";
    private const string Gpt56SolFastSlug = "gpt-5.6-sol-fast";
    private const string Gpt56TerraSlug = "gpt-5.6-terra";
    private const string Gpt56TerraFastSlug = "gpt-5.6-terra-fast";
    private const string Gpt56LunaSlug = "gpt-5.6-luna";
    private const string Gpt56LunaFastSlug = "gpt-5.6-luna-fast";

    private const string ClaudeOpus5Slug = "claude-opus-5[1m]";

    /// <summary>
    /// Anthropic-Modelle, die auch in bereits gespeicherte models.json nachgetragen werden
    /// (einmalig, per KnownSyncedModelSlugs gemerkt — manuell entfernte Modelle bleiben weg).
    /// </summary>
    private static readonly (string Slug, string DisplayName)[] AnthropicModels =
    [
        (ClaudeOpus5Slug, "Claude Opus 5 (1M)"),
    ];

    /// <summary>
    /// Hebt die grossen Anthropic-Modelle auf ihre 1M-Kontext-Variante. Der Launcher reicht den
    /// Slug unveraendert an "claude --model" weiter; ohne "[1m]" startet Claude Code mit dem
    /// Standard-Kontextfenster. Laeuft wie der Nachtrag genau einmal (Merker = neuer Slug in
    /// KnownSyncedModelSlugs), damit eine spaetere manuelle Rueckstellung bestehen bleibt.
    /// </summary>
    private static readonly (string OldSlug, string NewSlug, string DisplayName)[] AnthropicOneMillionMigrations =
    [
        ("claude-opus-5", ClaudeOpus5Slug, "Claude Opus 5 (1M)"),
        ("claude-fable-5", "claude-fable-5[1m]", "Claude Fable 5 (1M)"),
        ("claude-sonnet-5", "claude-sonnet-5[1m]", "Claude Sonnet 5 (1M)"),
        ("claude-opus-4-8", "claude-opus-4-8[1m]", "Claude Opus 4.8 (1M)"),
    ];

    private static IEnumerable<string> AnthropicManagedSlugs =>
        AnthropicModels.Select(definition => definition.Slug)
            .Concat(AnthropicOneMillionMigrations.Select(migration => migration.NewSlug));

    private static readonly (string Slug, string DisplayName)[] Gpt56Models =
    [
        (Gpt56SolSlug, "GPT-5.6 Sol"),
        (Gpt56SolFastSlug, "GPT-5.6 Sol Fast"),
        (Gpt56TerraSlug, "GPT-5.6 Terra"),
        (Gpt56TerraFastSlug, "GPT-5.6 Terra Fast"),
        (Gpt56LunaSlug, "GPT-5.6 Luna"),
        (Gpt56LunaFastSlug, "GPT-5.6 Luna Fast"),
    ];

    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenLauncher");
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
            // Atomar schreiben (Temp + Move) — konsistent mit dem übrigen Persistenz-Code. Ein Absturz
            // mitten in File.WriteAllText würde models.json sonst abschneiden; Load() fiele beim nächsten
            // Start still auf Defaults zurück und verlöre Modell-Liste, -Reihenfolge und Hidden-State.
            var tmp = FilePath + ".tmp";
            File.WriteAllText(tmp, JsonSerializer.Serialize(Groups, JsonOpts));
            File.Move(tmp, FilePath, overwrite: true);
        }
        catch (Exception ex)
        {
            Logger.Instance.Error("ModelRegistry", "Save", ex.Message);
        }
    }

    public bool AddModel(ModelGroupEntry group, string slug, string displayName)
    {
        slug = NormalizeSlugForGroup(slug, group.ProviderId);
        if (group.Models.Any(m => string.Equals(m.Slug, slug, StringComparison.OrdinalIgnoreCase)))
        {
            Logger.Instance.Warn("ModelRegistry", "AddModel", $"Slug existiert bereits: {slug}");
            return false;
        }
        group.Models.Add(new ModelEntry
        {
            Slug = slug,
            DisplayName = string.IsNullOrWhiteSpace(displayName) ? ToDisplayName(slug) : displayName,
            HasCustomDisplayName = !string.IsNullOrWhiteSpace(displayName),
            IsUserDefined = true,
            ProviderId = group.ProviderId,
            ProviderName = group.ProviderName
        });
        Save();
        Logger.Instance.Info("ModelRegistry", "AddModel", $"hinzugefügt: {slug}", new { group = group.Title, displayName });
        return true;
    }

    /// <summary>
    /// Bearbeitet einen bestehenden Eintrag: Modell-ID (Slug inkl. Parameter wie "[1m]"),
    /// Anzeigename und optional die Kategorie. false, wenn der Ziel-Slug dort schon existiert.
    /// </summary>
    public bool UpdateModel(ModelGroupEntry group, ModelEntry model, ModelGroupEntry targetGroup, string slug, string displayName)
    {
        var index = group.Models.IndexOf(model);
        if (index < 0) return false;

        var normalized = NormalizeSlugForGroup(slug, targetGroup.ProviderId);
        if (string.IsNullOrWhiteSpace(normalized)) return false;
        if (targetGroup.Models.Any(m => !ReferenceEquals(m, model) && string.Equals(m.Slug, normalized, StringComparison.OrdinalIgnoreCase)))
        {
            Logger.Instance.Warn("ModelRegistry", "UpdateModel", $"Slug existiert bereits: {normalized}", new { group = targetGroup.Title });
            return false;
        }

        var oldSlug = model.Slug;
        var hasCustomName = !string.IsNullOrWhiteSpace(displayName);

        // Der alte Slug darf im OpenRouterFree-Sync nicht wieder auftauchen (gleiche Logik wie beim
        // Entfernen) — sonst legte der naechste Abgleich das umbenannte Modell erneut daneben an.
        if (!string.Equals(oldSlug, normalized, StringComparison.OrdinalIgnoreCase) &&
            string.Equals(group.Id, "openrouter-free", StringComparison.OrdinalIgnoreCase))
        {
            AddUnique(group.HiddenModelSlugs, oldSlug);
            AddUnique(group.KnownSyncedModelSlugs, oldSlug);
        }

        if (!ReferenceEquals(group, targetGroup))
        {
            group.Models.RemoveAt(index);
            if (string.Equals(group.Id, "openrouter-free", StringComparison.OrdinalIgnoreCase))
            {
                AddUnique(group.HiddenModelSlugs, oldSlug);
                AddUnique(group.KnownSyncedModelSlugs, oldSlug);
            }
            model.ProviderId = targetGroup.ProviderId;
            model.ProviderName = targetGroup.ProviderName;
            targetGroup.Models.Add(model);
        }

        model.Slug = normalized;
        model.DisplayName = hasCustomName ? displayName.Trim() : ToDisplayName(normalized);
        model.HasCustomDisplayName = hasCustomName;
        model.IsUserDefined = true;

        Save();
        group.RefreshHeaderText();
        targetGroup.RefreshHeaderText();
        Logger.Instance.Info("ModelRegistry", "UpdateModel", $"bearbeitet: {oldSlug} -> {normalized}",
            new { group = group.Title, target = targetGroup.Title, displayName = model.DisplayName });
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

    public bool MoveModel(ModelGroupEntry sourceGroup, int from, ModelGroupEntry targetGroup, int to)
    {
        if (ReferenceEquals(sourceGroup, targetGroup))
        {
            MoveModel(sourceGroup, from, to);
            return true;
        }
        if (from < 0 || from >= sourceGroup.Models.Count) return false;

        var item = sourceGroup.Models[from];
        var targetSlug = NormalizeSlugForGroup(item.Slug, targetGroup.ProviderId);
        if (targetGroup.Models.Any(m => string.Equals(m.Slug, targetSlug, StringComparison.OrdinalIgnoreCase)))
        {
            Logger.Instance.Warn("ModelRegistry", "MoveModel", $"Ziel enthält Slug bereits: {targetSlug}", new { source = sourceGroup.Title, target = targetGroup.Title });
            return false;
        }

        sourceGroup.Models.RemoveAt(from);
        item.Slug = targetSlug;
        item.ProviderId = targetGroup.ProviderId;
        item.ProviderName = targetGroup.ProviderName;
        if (to < 0) to = targetGroup.Models.Count;
        if (to > targetGroup.Models.Count) to = targetGroup.Models.Count;
        targetGroup.Models.Insert(to, item);
        Save();
        sourceGroup.RefreshHeaderText();
        targetGroup.RefreshHeaderText();
        Logger.Instance.Info("ModelRegistry", "MoveModel", $"{sourceGroup.Title}:{from} -> {targetGroup.Title}:{to}");
        return true;
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
            if (!remoteBySlug.TryGetValue(existing.Slug, out var remoteModel))
            {
                // Selbst hinzugefuegte/bearbeitete Eintraege ueberleben den Sync auch dann, wenn
                // ihr Slug in der Remote-Liste fehlt. Ausgeblendete Eintraege muessen ebenfalls
                // erhalten bleiben, damit sie in der Wiederherstellungsansicht nicht verloren gehen.
                if (existing.IsUserDefined || existing.IsHidden) merged.Add(existing);
                continue;
            }
            existing.ProviderId = group.ProviderId;
            existing.ProviderName = group.ProviderName;
            if (!existing.HasCustomDisplayName) existing.DisplayName = remoteModel.DisplayName;
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
            group.Title = defaults.Title;
            if ((group.Models.Count == 0 || group.Models.All(m => string.IsNullOrWhiteSpace(m.Slug))) &&
                !string.Equals(group.Id, "openai", StringComparison.OrdinalIgnoreCase))
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

            if (string.Equals(group.Id, "openai", StringComparison.OrdinalIgnoreCase))
            {
                foreach (var definition in Gpt56Models)
                {
                    if (!group.KnownSyncedModelSlugs.Contains(definition.Slug, StringComparer.OrdinalIgnoreCase))
                    {
                        if (!group.Models.Any(model => string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase)))
                            group.Models.Add(Model(definition.Slug, definition.DisplayName, "openai", "OpenAI"));
                        AddUnique(group.KnownSyncedModelSlugs, definition.Slug);
                    }

                    var model = group.Models.FirstOrDefault(model => string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase));
                    if (model != null && !model.HasCustomDisplayName) model.DisplayName = definition.DisplayName;
                }
            }

            if (string.Equals(group.Id, "entropic", StringComparison.OrdinalIgnoreCase))
            {
                // Zuerst die 1M-Umstellung, danach der Nachtrag: beide teilen sich den Merker
                // (neuer Slug in KnownSyncedModelSlugs), sonst legte der Nachtrag Opus 5 nach
                // der Umstellung ein zweites Mal an.
                foreach (var migration in AnthropicOneMillionMigrations)
                {
                    if (group.KnownSyncedModelSlugs.Contains(migration.NewSlug, StringComparer.OrdinalIgnoreCase)) continue;

                    var outdated = group.Models.FirstOrDefault(model => string.Equals(model.Slug, migration.OldSlug, StringComparison.OrdinalIgnoreCase));
                    var alreadyPresent = group.Models.Any(model => string.Equals(model.Slug, migration.NewSlug, StringComparison.OrdinalIgnoreCase));
                    if (outdated != null && !alreadyPresent)
                    {
                        outdated.Slug = migration.NewSlug;
                        if (!outdated.HasCustomDisplayName) outdated.DisplayName = migration.DisplayName;
                        Logger.Instance.Info("ModelRegistry", "RepairAndNormalize", $"1M-Variante gesetzt: {migration.OldSlug} -> {migration.NewSlug}");
                    }
                    AddUnique(group.KnownSyncedModelSlugs, migration.NewSlug);
                }

                foreach (var definition in AnthropicModels)
                {
                    if (!group.KnownSyncedModelSlugs.Contains(definition.Slug, StringComparer.OrdinalIgnoreCase))
                    {
                        if (!group.Models.Any(model => string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase)))
                            group.Models.Insert(0, Model(definition.Slug, definition.DisplayName, "anthropic", "Anthropic"));
                        AddUnique(group.KnownSyncedModelSlugs, definition.Slug);
                    }

                    var model = group.Models.FirstOrDefault(model => string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase));
                    if (model != null && !model.HasCustomDisplayName) model.DisplayName = definition.DisplayName;
                }
            }
        }
    }

    private static ObservableCollection<ModelGroupEntry> CreateDefaults(IEnumerable<ModelEntry>? openRouterModels = null) => new()
    {
        CreateGroup("openrouter", "OpenRouter", "openrouter", "OpenRouter", NormalizeOpenRouter(openRouterModels).ToArray()),
        CreateGroup("entropic", "Anthropic", "anthropic", "Anthropic", new[]
        {
            Model(ClaudeOpus5Slug, "Claude Opus 5 (1M)", "anthropic", "Anthropic"),
            Model("claude-fable-5[1m]", "Claude Fable 5 (1M)", "anthropic", "Anthropic"),
            Model("claude-opus-4-8[1m]", "Claude Opus 4.8 (1M)", "anthropic", "Anthropic"),
            Model("claude-sonnet-5[1m]", "Claude Sonnet 5 (1M)", "anthropic", "Anthropic"),
            Model("claude-haiku-4-5", "Claude Haiku 4.5", "anthropic", "Anthropic"),
            Model("claude-opus-4-7", "Claude Opus 4.7", "anthropic", "Anthropic"),
            Model("claude-opus-4-6", "Claude Opus 4.6", "anthropic", "Anthropic"),
            Model("claude-sonnet-4-6", "Claude Sonnet 4.6", "anthropic", "Anthropic"),
            Model("claude-opus-4-5", "Claude Opus 4.5", "anthropic", "Anthropic"),
            Model("claude-sonnet-4-5", "Claude Sonnet 4.5", "anthropic", "Anthropic"),
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
            Model(Gpt56SolSlug, "GPT-5.6 Sol", "openai", "OpenAI"),
            Model(Gpt56SolFastSlug, "GPT-5.6 Sol Fast", "openai", "OpenAI"),
            Model(Gpt56TerraSlug, "GPT-5.6 Terra", "openai", "OpenAI"),
            Model(Gpt56TerraFastSlug, "GPT-5.6 Terra Fast", "openai", "OpenAI"),
            Model(Gpt56LunaSlug, "GPT-5.6 Luna", "openai", "OpenAI"),
            Model(Gpt56LunaFastSlug, "GPT-5.6 Luna Fast", "openai", "OpenAI"),
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
        CreateGroup("nvidia", "NVIDIA", NvidiaProviderId, NvidiaProviderName, NvidiaFreeModels),
    };

    /// <summary>
    /// Kostenlose NIM-Endpunkte von build.nvidia.com, die OpenCode ueber den models.dev-Provider
    /// "nvidia" starten kann. Aufgenommen ist nur, was alle drei Bedingungen erfuellt:
    /// live unter GET https://integrate.api.nvidia.com/v1/models gelistet, in models.dev mit
    /// cost.input/output = 0 gefuehrt (also ohne Guthabenverbrauch) und tool_call-faehig — ohne
    /// Werkzeugaufrufe kann OpenCode ein Modell nicht sinnvoll als Programmiermodell fahren.
    /// Bewusst NICHT enthalten: die kostenpflichtigen Endpunkte (Nemotron 3 Ultra/Super,
    /// DeepSeek V4 Flash/Pro), Embedding-, Rerank-, Safety- und Bildmodelle sowie alles, was
    /// models.dev als status "deprecated"/"alpha" fuehrt (z.B. Kimi K2.6) — solche Modelle
    /// blendet OpenCode aus seinem Picker aus und koennte sie gar nicht erst starten.
    /// Die Slugs tragen den Hersteller-Praefix der NVIDIA-ID; zusammen mit dem Provider ergibt
    /// das die Start-ID "nvidia/&lt;hersteller&gt;/&lt;modell&gt;" (siehe ModelEntry.ModelString).
    /// Sortierung: leistungsstaerkste zuerst.
    /// </summary>
    private static ModelEntry[] NvidiaFreeModels =>
    [
        NvidiaModel("z-ai/glm-5.2", "GLM 5.2 (1M)"),
        NvidiaModel("thinkingmachines/inkling", "Inkling (1M)"),
        NvidiaModel("minimaxai/minimax-m3", "MiniMax M3 (1M)"),
        NvidiaModel("poolside/laguna-xs-2.1", "Poolside Laguna XS 2.1"),
        NvidiaModel("stepfun-ai/step-3.7-flash", "Step 3.7 Flash"),
        NvidiaModel("mistralai/mistral-medium-3.5-128b", "Mistral Medium 3.5"),
        NvidiaModel("google/gemma-4-31b-it", "Gemma 4 31B"),
        NvidiaModel("openai/gpt-oss-120b", "GPT-OSS 120B"),
        NvidiaModel("openai/gpt-oss-20b", "GPT-OSS 20B"),
        NvidiaModel("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "Nemotron 3 Nano Omni Reasoning"),
        NvidiaModel("nvidia/nemotron-3-nano-30b-a3b", "Nemotron 3 Nano 30B A3B"),
        NvidiaModel("nvidia/llama-3.1-nemotron-ultra-253b-v1", "Llama 3.1 Nemotron Ultra 253B"),
        NvidiaModel("nvidia/llama-3.3-nemotron-super-49b-v1.5", "Llama 3.3 Nemotron Super 49B v1.5"),
        NvidiaModel("nvidia/llama-3.3-nemotron-super-49b-v1", "Llama 3.3 Nemotron Super 49B v1"),
        NvidiaModel("nvidia/nvidia-nemotron-nano-9b-v2", "Nemotron Nano 9B v2"),
        NvidiaModel("nvidia/nemotron-nano-12b-v2-vl", "Nemotron Nano 12B v2 VL"),
        NvidiaModel("nvidia/llama-3.1-nemotron-nano-8b-v1", "Llama 3.1 Nemotron Nano 8B"),
        NvidiaModel("nvidia/llama-3.1-nemotron-nano-vl-8b-v1", "Llama 3.1 Nemotron Nano VL 8B"),
        NvidiaModel("nvidia/cosmos-reason2-8b", "Cosmos Reason2 8B"),
        NvidiaModel("nvidia/llama-3.1-nemotron-70b-instruct", "Llama 3.1 Nemotron 70B"),
        NvidiaModel("nvidia/nemotron-mini-4b-instruct", "Nemotron Mini 4B"),
        NvidiaModel("mistralai/mistral-nemotron", "Mistral Nemotron"),
        NvidiaModel("mistralai/mistral-7b-instruct-v0.3", "Mistral 7B Instruct v0.3"),
        NvidiaModel("meta/llama-3.3-70b-instruct", "Llama 3.3 70B Instruct"),
        NvidiaModel("meta/llama-3.1-70b-instruct", "Llama 3.1 70B Instruct"),
        NvidiaModel("meta/llama-3.2-90b-vision-instruct", "Llama 3.2 90B Vision"),
        NvidiaModel("meta/llama-3.2-11b-vision-instruct", "Llama 3.2 11B Vision"),
        NvidiaModel("meta/llama-3.1-8b-instruct", "Llama 3.1 8B Instruct"),
        NvidiaModel("meta/llama-3.2-1b-instruct", "Llama 3.2 1B Instruct"),
        NvidiaModel("google/gemma-3-12b-it", "Gemma 3 12B"),
        NvidiaModel("google/gemma-3-4b-it", "Gemma 3 4B"),
    ];

    private const string NvidiaProviderId = ModelEntry.NvidiaProviderId;
    private const string NvidiaProviderName = "NVIDIA";

    private static ModelEntry NvidiaModel(string slug, string displayName) =>
        Model(slug, displayName, NvidiaProviderId, NvidiaProviderName);

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
            : string.Equals(id, "openai", StringComparison.OrdinalIgnoreCase)
                ? models.Where(model => Gpt56Models.Any(definition =>
                    string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase)))
                    .Select(model => model.Slug)
                    .ToList()
            : string.Equals(id, "entropic", StringComparison.OrdinalIgnoreCase)
                ? models.Where(model => AnthropicManagedSlugs.Any(slug =>
                    string.Equals(model.Slug, slug, StringComparison.OrdinalIgnoreCase)))
                    .Select(model => model.Slug)
                    .ToList()
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

    private static string NormalizeSlugForGroup(string slug, string providerId)
    {
        slug = slug.Trim().ToLowerInvariant();
        // Bei NVIDIA ist "nvidia/" Bestandteil der Modell-ID, nicht ein doppelter Provider-Praefix
        // (siehe ModelEntry.NvidiaProviderId) — dort wuerde das Kuerzen die ID zerstoeren.
        if (string.Equals(providerId, ModelEntry.NvidiaProviderId, StringComparison.OrdinalIgnoreCase))
            return slug;
        if (slug.StartsWith($"{providerId}/", StringComparison.OrdinalIgnoreCase))
            slug = slug[$"{providerId}/".Length..];
        return slug;
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
