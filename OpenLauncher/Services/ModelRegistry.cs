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
        ("claude-haiku-5-5", "Claude Haiku 5.5"),
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
        ("claude-fable-5", "claude-fable-5-1[1m]", "Claude Fable 5.1 (1M)"),
        ("claude-sonnet-5", "claude-sonnet-5[1m]", "Claude Sonnet 5 (1M)"),
        ("claude-opus-4-8", "claude-opus-4-8[1m]", "Claude Opus 4.8 (1M)"),
        // Sonnet 5.5 loest Sonnet 5 ab: der bisherige Eintrag wird einmalig umgestellt.
        ("claude-sonnet-5[1m]", "claude-sonnet-5-5[1m]", "Claude Sonnet 5.5 (1M)"),
        // Fable 5.2 loest Fable 5.1 ab: der bisherige Eintrag wird einmalig umgestellt.
        ("claude-fable-5-1[1m]", "claude-fable-5-2[1m]", "Claude Fable 5.2 (1M)"),
    ];

    private static IEnumerable<string> AnthropicManagedSlugs =>
        AnthropicModels.Select(definition => definition.Slug)
            .Concat(AnthropicOneMillionMigrations.Select(migration => migration.NewSlug));

    private static readonly (string Slug, string DisplayName)[] Gpt56Models =
    [
        ("gpt-6.2-astra", "GPT-6.2 Astra"),
        ("gpt-6.2-sol", "GPT-6.2 Sol"),
        ("gpt-6.1-terra", "GPT-6.1 Terra"),
        (Gpt56TerraSlug, "GPT-5.6 Terra"),
        ("gpt-6-luna", "GPT-6 Luna"),
    ];

    /// <summary>
    /// GPT-6-Modelle, die einmalig nachgetragen werden. Sie stehen (noch) nicht nativ im
    /// OpenCode-Katalog und laufen deshalb wie GPT-6.2 Astra als Direktmodell (IsUserDefined),
    /// damit ConfigureProvider den provider.openai.models-Eintrag schreibt.
    /// </summary>
    private static readonly (string Slug, string DisplayName)[] Gpt6Models =
    [
        ("gpt-6.2-sol", "GPT-6.2 Sol"),
        ("gpt-6.1-terra", "GPT-6.1 Terra"),
        ("gpt-6-luna", "GPT-6 Luna"),
    ];

    /// <summary>
    /// GPT-6.2 Sol loest GPT-6 Sol ab: der bisherige Eintrag wird einmalig umgestellt
    /// (Merker: neuer Slug in KnownSyncedModelSlugs), damit er nicht doppelt erscheint.
    /// </summary>
    private static readonly (string OldSlug, string NewSlug, string DisplayName)[] Gpt6Migrations =
    [
        ("gpt-6-sol", "gpt-6.2-sol", "GPT-6.2 Sol"),
        ("gpt-6.1-sol", "gpt-6.2-sol", "GPT-6.2 Sol"),
        ("gpt-6-astra", "gpt-6.2-astra", "GPT-6.2 Astra"),
    ];

    /// <summary>
    /// Die Modell-Liste liegt im Repo (~/proggs/OpenLauncher/models.json), nicht im
    /// Anwendungsdatenordner. Sie enthaelt keine Geheimnisse — nur Modell-IDs, Anzeigenamen und
    /// die Sortierung — und gehoert damit wie die Profiltexte unter Profiles/ (siehe
    /// InstructionProfileService) zum versionierten Bestand: was hier gepflegt wird, ist nach
    /// Commit und Push auf jedem Rechner identisch vorhanden. Eine Liste am frueheren Ort wird
    /// beim ersten Start einmalig uebernommen (MigrateLegacyFile).
    /// </summary>
    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "proggs", "OpenLauncher");
    private static readonly string FilePath = Path.Combine(Dir, "models.json");

    private static readonly string LegacyFilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "OpenLauncher", "models.json");

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
        MigrateLegacyFile();
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
                            reg.ApplyStoredLayout();
                            reg.Save();
                            Logger.Instance.Info("ModelRegistry", "Load", $"{parsedGroups.Count} Modellgruppen geladen", new { FilePath });
                            return reg;
                        }
                    }

                    var oldList = JsonSerializer.Deserialize<List<ModelEntry>>(json, JsonOpts);
                    if (oldList != null && oldList.Any(m => !string.IsNullOrWhiteSpace(m.Slug)))
                    {
                        reg.Groups = CreateDefaults(oldList);
                        reg.ApplyStoredLayout();
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
                    reg.ApplyStoredLayout();
                    reg.Save();
                    Logger.Instance.Info("ModelRegistry", "Load", $"{groups.Count} Modellgruppen geladen", new { FilePath });
                    return reg;
                }
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ModelRegistry", "Load", $"Laden fehlgeschlagen, Defaults: {ex.Message}");
            BackupBrokenFile();
        }

        reg.Groups = CreateDefaults();
        reg.ApplyStoredLayout();
        reg.Save();
        return reg;
    }

    /// <summary>
    /// Uebernimmt eine Modell-Liste vom frueheren Ort (%AppData%/OpenLauncher/models.json)
    /// einmalig ins Repo, damit eine ueber die Oberflaeche gepflegte Liste beim Umstieg nicht
    /// verloren geht. Kopiert statt verschoben: die alte Datei bleibt als Sicherheitsnetz liegen.
    /// Existiert im Repo bereits eine Liste (etwa frisch aus Git geholt), gilt diese — sonst
    /// ueberschriebe der alte Stand eines Rechners die gepushte Fassung.
    /// </summary>
    private static void MigrateLegacyFile()
    {
        if (File.Exists(FilePath) || !File.Exists(LegacyFilePath)) return;
        try
        {
            Directory.CreateDirectory(Dir);
            File.Copy(LegacyFilePath, FilePath);
            Logger.Instance.Info("ModelRegistry", "MigrateLegacyFile", "Modell-Liste ins Repo uebernommen", new { LegacyFilePath, FilePath });
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ModelRegistry", "MigrateLegacyFile", $"Uebernahme fehlgeschlagen: {ex.Message}", new { LegacyFilePath, FilePath });
        }
    }

    /// <summary>
    /// Eine nicht lesbare models.json (z. B. Git-Konfliktmarker nach einem Update) wird gleich durch
    /// die Defaults ueberschrieben — Modell-Liste, Reihenfolge und Hidden-State waeren verloren.
    /// Vorher eine Kopie ablegen, damit sich der Stand wiederherstellen laesst.
    /// </summary>
    private static void BackupBrokenFile()
    {
        try
        {
            if (!File.Exists(FilePath)) return;
            var backup = FilePath + ".defekt-" + DateTime.Now.ToString("yyyyMMdd-HHmmss");
            File.Copy(FilePath, backup, overwrite: true);
            Logger.Instance.Warn("ModelRegistry", "BackupBrokenFile", "Defekte Modell-Liste gesichert", new { backup });
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ModelRegistry", "BackupBrokenFile", $"Sicherung fehlgeschlagen: {ex.Message}");
        }
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(Dir);
            // Atomar schreiben (Temp + Move) — konsistent mit dem übrigen Persistenz-Code. Ein Absturz
            // mitten in File.WriteAllText würde models.json sonst abschneiden; Load() fiele beim nächsten
            // Start still auf Defaults zurück und verlöre Modell-Liste, -Reihenfolge und Hidden-State.
            var json = JsonSerializer.Serialize(Groups, JsonOpts).Replace("\r\n", "\n");
            // Unveraenderten Inhalt nicht neu schreiben: sonst erzeugt jeder Start eine Scheinaenderung,
            // die beim naechsten Update mit dem Upstream kollidiert. LF wie im Repo (*.json eol=lf).
            if (File.Exists(FilePath) && File.ReadAllText(FilePath).Replace("\r\n", "\n") == json) return;
            var tmp = FilePath + ".tmp";
            File.WriteAllText(tmp, json);
            File.Move(tmp, FilePath, overwrite: true);
        }
        catch (Exception ex)
        {
            Logger.Instance.Error("ModelRegistry", "Save", ex.Message);
        }
    }

    // ===================== Anordnung (Reihenfolge + Sichtbarkeit) =====================

    /// <summary>
    /// Die gemeinsame Modell-Anordnung aller Rechner: Gruppenreihenfolge, je Gruppe die Modelle in ihrer
    /// Reihenfolge (mit Name, eigenem Eintrag, Ausgeblendet) und vom Nutzer entfernte Modelle. Liegt
    /// versioniert neben models.json, wird aber NUR bei Bedienaktionen (Verschieben, Aus-/Einblenden,
    /// Hinzufuegen, Bearbeiten, Entfernen) geschrieben und sofort committet und gepusht. models.json bleibt
    /// der rechnerabhaengige Katalog (LM Studio, OpenRouter-Abgleich, Aufklappzustand); die Anordnung wird
    /// beim Laden und nach jedem Repo-Abgleich darueber gelegt. Kein Update und kein Katalog-Abgleich
    /// aendert so Reihenfolge oder Sichtbarkeit, und jeder Rechner (auch Jarvis) zeigt denselben Stand.
    /// Modelle, die die Anordnung noch nicht kennt, haengen hinten an.
    /// </summary>
    public static string LayoutPath { get; } = Path.Combine(Dir, "model-layout.json");

    private sealed class ModelLayout
    {
        public List<string> Gruppen { get; set; } = new();
        public Dictionary<string, List<LayoutModel>> Modelle { get; set; } = new();
        public Dictionary<string, List<string>> Entfernt { get; set; } = new();
    }

    private sealed class LayoutModel
    {
        public string Slug { get; set; } = "";
        public string Name { get; set; } = "";
        public bool EigenerName { get; set; }
        public bool Eigen { get; set; }
        public bool Ausgeblendet { get; set; }
    }

    private string? _layoutText;
    private readonly Dictionary<string, HashSet<string>> _removedSinceSave = new(StringComparer.OrdinalIgnoreCase);
    private static readonly SemaphoreSlim LayoutPushLock = new(1, 1);
    private static int _layoutPushQueued;

    private static ModelLayout? ReadLayout(out string? text)
    {
        text = null;
        try
        {
            if (!File.Exists(LayoutPath)) return null;
            text = File.ReadAllText(LayoutPath).Replace("\r\n", "\n");
            return JsonSerializer.Deserialize<ModelLayout>(text, JsonOpts);
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ModelRegistry", "ReadLayout", $"Anordnung nicht lesbar: {ex.Message}", new { LayoutPath });
            return null;
        }
    }

    private void ApplyStoredLayout()
    {
        var layout = ReadLayout(out var text);
        if (layout == null) return;
        _layoutText = text;
        ApplyLayout(layout);
    }

    /// <summary>
    /// Nach einem Repo-Abgleich: hat ein anderer Rechner eine neue Anordnung gepusht, sie uebernehmen.
    /// </summary>
    public bool ReloadLayoutIfChanged()
    {
        var layout = ReadLayout(out var text);
        if (layout == null || text == _layoutText) return false;
        _layoutText = text;
        var changed = ApplyLayout(layout);
        if (changed) Save();
        Logger.Instance.Info("ModelRegistry", "ReloadLayoutIfChanged", "Neue Anordnung aus dem Repo übernommen", new { changed });
        return changed;
    }

    private static List<T>? FindLayoutList<T>(Dictionary<string, List<T>> map, string groupId) =>
        map.FirstOrDefault(pair => string.Equals(pair.Key, groupId, StringComparison.OrdinalIgnoreCase)).Value;

    private bool ApplyLayout(ModelLayout layout)
    {
        var changed = false;
        var target = 0;
        foreach (var id in layout.Gruppen)
        {
            var index = Groups.ToList().FindIndex(g => string.Equals(g.Id, id, StringComparison.OrdinalIgnoreCase));
            if (index < target) continue;
            if (index != target) { Groups.Move(index, target); changed = true; }
            target++;
        }

        foreach (var group in Groups)
        {
            var entries = FindLayoutList(layout.Modelle, group.Id) ?? new List<LayoutModel>();
            var removed = (FindLayoutList(layout.Entfernt, group.Id) ?? new List<string>()).ToHashSet(StringComparer.OrdinalIgnoreCase);
            var listed = entries.Select(e => e.Slug).ToHashSet(StringComparer.OrdinalIgnoreCase);
            foreach (var model in group.Models.Where(m => removed.Contains(m.Slug) && !listed.Contains(m.Slug)).ToList())
            {
                group.Models.Remove(model);
                changed = true;
            }

            var position = 0;
            foreach (var entry in entries)
            {
                if (string.IsNullOrWhiteSpace(entry.Slug)) continue;
                var index = group.Models.ToList().FindIndex(m => string.Equals(m.Slug, entry.Slug, StringComparison.OrdinalIgnoreCase));
                if (index < 0)
                {
                    // Auf einem anderen Rechner hinzugefuegt: eigene Eintraege uebernehmen. Katalogmodelle,
                    // die es hier nicht gibt (z. B. fremde LM-Studio-Modelle), nicht vortaeuschen.
                    if (!entry.Eigen) continue;
                    var added = Model(entry.Slug, string.IsNullOrWhiteSpace(entry.Name) ? ToDisplayName(entry.Slug) : entry.Name,
                        group.ProviderId, group.ProviderName);
                    added.IsUserDefined = true;
                    added.HasCustomDisplayName = entry.EigenerName;
                    group.Models.Insert(Math.Min(position, group.Models.Count), added);
                    index = position;
                    changed = true;
                }
                if (index < position) continue;
                if (index != position) { group.Models.Move(index, position); changed = true; }
                var model = group.Models[position];
                if (model.IsHidden != entry.Ausgeblendet) { model.IsHidden = entry.Ausgeblendet; changed = true; }
                if (entry.Eigen && !model.IsUserDefined) { model.IsUserDefined = true; changed = true; }
                if (entry.EigenerName && !string.IsNullOrWhiteSpace(entry.Name) &&
                    (!model.HasCustomDisplayName || model.DisplayName != entry.Name))
                {
                    model.DisplayName = entry.Name;
                    model.HasCustomDisplayName = true;
                    changed = true;
                }
                position++;
            }
            group.RefreshHeaderText();
        }
        return changed;
    }

    /// <summary>Speichert nach einer Bedienaktion Katalog und Anordnung und pusht die Anordnung.</summary>
    public void SaveUserChange()
    {
        Save();
        SaveLayout();
    }

    /// <summary>Merkt ein vom Nutzer entferntes Modell, damit es auf allen Rechnern verschwindet.</summary>
    private void MarkRemoved(ModelGroupEntry group, string slug)
    {
        if (!_removedSinceSave.TryGetValue(group.Id, out var set))
            _removedSinceSave[group.Id] = set = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        set.Add(slug);
    }

    private void SaveLayout()
    {
        try
        {
            var old = ReadLayout(out _);
            var layout = new ModelLayout();
            foreach (var group in Groups)
            {
                layout.Gruppen.Add(group.Id);
                var local = group.Models.Select(m => m.Slug).ToHashSet(StringComparer.OrdinalIgnoreCase);
                var removed = (FindLayoutList(old?.Entfernt ?? new(), group.Id) ?? new List<string>())
                    .Concat(_removedSinceSave.TryGetValue(group.Id, out var now) ? now : Enumerable.Empty<string>())
                    .Where(slug => !local.Contains(slug))
                    .Distinct(StringComparer.OrdinalIgnoreCase)
                    .ToList();
                var entries = group.Models.Select(m => new LayoutModel
                {
                    Slug = m.Slug,
                    Name = m.DisplayName,
                    EigenerName = m.HasCustomDisplayName,
                    Eigen = m.IsUserDefined,
                    Ausgeblendet = m.IsHidden
                }).ToList();
                // Eintraege, die es nur auf anderen Rechnern gibt (z. B. deren LM-Studio-Modelle), behalten.
                foreach (var entry in FindLayoutList(old?.Modelle ?? new(), group.Id) ?? new List<LayoutModel>())
                    if (!local.Contains(entry.Slug) && !removed.Contains(entry.Slug, StringComparer.OrdinalIgnoreCase))
                        entries.Add(entry);
                layout.Modelle[group.Id] = entries;
                layout.Entfernt[group.Id] = removed;
            }
            foreach (var id in old?.Gruppen ?? new List<string>())
            {
                if (layout.Gruppen.Contains(id, StringComparer.OrdinalIgnoreCase)) continue;
                layout.Gruppen.Add(id);
                if (FindLayoutList(old!.Modelle, id) is { } m) layout.Modelle[id] = m;
                if (FindLayoutList(old!.Entfernt, id) is { } r) layout.Entfernt[id] = r;
            }
            _removedSinceSave.Clear();

            var json = JsonSerializer.Serialize(layout, JsonOpts).Replace("\r\n", "\n");
            if (json == _layoutText) return;
            var tmp = LayoutPath + ".tmp";
            File.WriteAllText(tmp, json);
            File.Move(tmp, LayoutPath, overwrite: true);
            _layoutText = json;
            _ = PushLayoutAsync();
        }
        catch (Exception ex)
        {
            Logger.Instance.Error("ModelRegistry", "SaveLayout", ex.Message);
        }
    }

    /// <summary>
    /// Pusht die Anordnung im Hintergrund, entprellt (mehrere schnelle Aenderungen = ein Commit) und
    /// nacheinander, damit sich zwei Laeufe nie im selben Repo ueberholen.
    /// </summary>
    private static async Task PushLayoutAsync()
    {
        if (Interlocked.Exchange(ref _layoutPushQueued, 1) == 1) return;
        await Task.Delay(TimeSpan.FromSeconds(3));
        await LayoutPushLock.WaitAsync();
        try
        {
            Interlocked.Exchange(ref _layoutPushQueued, 0);
            var result = await RepoSync.CommitAndPushFileAsync(LayoutPath, "OpenLauncher: Modell-Anordnung der Seitenleiste geändert");
            if (result.Ok) Logger.Instance.Info("ModelRegistry", "PushLayoutAsync", result.Message);
            else Logger.Instance.Warn("ModelRegistry", "PushLayoutAsync", result.Message);
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("ModelRegistry", "PushLayoutAsync", ex.Message);
        }
        finally
        {
            LayoutPushLock.Release();
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
        SaveUserChange();
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
        if (!string.Equals(oldSlug, normalized, StringComparison.OrdinalIgnoreCase) && IsLiveCatalogGroup(group.Id))
        {
            AddUnique(group.HiddenModelSlugs, oldSlug);
            AddUnique(group.KnownSyncedModelSlugs, oldSlug);
        }

        if (!ReferenceEquals(group, targetGroup))
        {
            group.Models.RemoveAt(index);
            if (IsLiveCatalogGroup(group.Id))
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
        if (!ReferenceEquals(group, targetGroup) || !string.Equals(oldSlug, normalized, StringComparison.OrdinalIgnoreCase))
            MarkRemoved(group, oldSlug);

        SaveUserChange();
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
        MarkRemoved(group, m.Slug);
        if (IsLiveCatalogGroup(group.Id))
        {
            AddUnique(group.HiddenModelSlugs, m.Slug);
            AddUnique(group.KnownSyncedModelSlugs, m.Slug);
        }
        SaveUserChange();
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
        SaveUserChange();
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
        MarkRemoved(sourceGroup, item.Slug);
        item.Slug = targetSlug;
        item.ProviderId = targetGroup.ProviderId;
        item.ProviderName = targetGroup.ProviderName;
        if (to < 0) to = targetGroup.Models.Count;
        if (to > targetGroup.Models.Count) to = targetGroup.Models.Count;
        targetGroup.Models.Insert(to, item);
        SaveUserChange();
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
        SaveUserChange();
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

    public void SyncOpenRouterFreeModels(IEnumerable<ModelEntry> remoteModels) =>
        SyncGroupModels("openrouter-free", remoteModels);

    public void SyncOpenRouterModels(IEnumerable<ModelEntry> remoteModels) =>
        SyncGroupModels("openrouter", remoteModels);

    public void SyncOpenCodeZenFreeModels(IEnumerable<ModelEntry> remoteModels) =>
        SyncGroupModels("opencode-zen-free", remoteModels);

    /// <summary>
    /// Gleicht die Gruppe "LM Studio" mit den Modellen ab, die der lokale LM-Studio-Server
    /// gerade anbietet. Entfernte Modelle wandern in die Ausgeblendet-Liste, eigene Eintraege
    /// und Umbenennungen bleiben erhalten — gleiche Regeln wie beim OpenRouterFree-Sync.
    /// </summary>
    public void SyncLmStudioModels(IEnumerable<ModelEntry> localModels) =>
        SyncGroupModels("lmstudio", localModels);

    private void SyncGroupModels(string groupId, IEnumerable<ModelEntry> remoteModels)
    {
        var group = Groups.FirstOrDefault(g => string.Equals(g.Id, groupId, StringComparison.OrdinalIgnoreCase));
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
        Logger.Instance.Info("ModelRegistry", "SyncGroupModels", $"{group.Title}: {group.Models.Count} sichtbare, {group.HiddenModelSlugs.Count} ausgeblendete Modelle");
    }

    private static bool IsLiveCatalogGroup(string groupId) =>
        groupId.Equals("openrouter", StringComparison.OrdinalIgnoreCase) ||
        groupId.Equals("openrouter-free", StringComparison.OrdinalIgnoreCase) ||
        groupId.Equals("opencode-zen-free", StringComparison.OrdinalIgnoreCase);

    private void RepairAndNormalize()
    {
        foreach (var defaults in CreateDefaults())
        {
            var group = Groups.FirstOrDefault(g => string.Equals(g.Id, defaults.Id, StringComparison.OrdinalIgnoreCase));
            if (group == null)
            {
                // Moonshot AI (Kimi) gehoert direkt unter OpenAI, Google (Gemini) direkt unter Moonshot AI
                // -- nicht ans Ende hinter LM Studio.
                var anchorId = defaults.Id.ToLowerInvariant() switch
                {
                    MoonshotGroupId => "openai",
                    GoogleGroupId => MoonshotGroupId,
                    _ => null
                };
                var anchorIndex = anchorId == null
                    ? -1
                    : Groups.ToList().FindIndex(g => string.Equals(g.Id, anchorId, StringComparison.OrdinalIgnoreCase));
                if (anchorIndex >= 0) Groups.Insert(anchorIndex + 1, defaults);
                else Groups.Add(defaults);
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

                foreach (var migration in Gpt6Migrations)
                {
                    if (group.KnownSyncedModelSlugs.Contains(migration.NewSlug, StringComparer.OrdinalIgnoreCase)) continue;

                    var outdated = group.Models.FirstOrDefault(model => string.Equals(model.Slug, migration.OldSlug, StringComparison.OrdinalIgnoreCase));
                    var alreadyPresent = group.Models.Any(model => string.Equals(model.Slug, migration.NewSlug, StringComparison.OrdinalIgnoreCase));
                    if (outdated != null && !alreadyPresent)
                    {
                        outdated.Slug = migration.NewSlug;
                        if (!outdated.HasCustomDisplayName) outdated.DisplayName = migration.DisplayName;
                        Logger.Instance.Info("ModelRegistry", "RepairAndNormalize", $"GPT-6-Nachfolger gesetzt: {migration.OldSlug} -> {migration.NewSlug}");
                    }
                    AddUnique(group.KnownSyncedModelSlugs, migration.NewSlug);
                }

                foreach (var definition in Gpt6Models)
                {
                    if (group.KnownSyncedModelSlugs.Contains(definition.Slug, StringComparer.OrdinalIgnoreCase)) continue;
                    if (!group.Models.Any(model => string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase)))
                    {
                        var entry = Model(definition.Slug, definition.DisplayName, "openai", "OpenAI");
                        entry.IsUserDefined = true;
                        group.Models.Add(entry);
                    }
                    AddUnique(group.KnownSyncedModelSlugs, definition.Slug);
                }

                // Altmodelle entfernen (auch ausgeblendete: sie dürfen nicht in Listen zurückkehren).
                // Reihenfolge und Sichtbarkeit der übrigen bleiben unangetastet: Früher wurde hier bei
                // jedem Start IsHidden=false gesetzt, dadurch kam z. B. GPT-5.6 Terra nach jedem Update
                // wieder. Fehlt ein aktuelles Modell, nur weil es per Merker schon bekannt war, bleibt es
                // weg (vom Nutzer entfernt).
                foreach (var model in group.Models.Where(m => !Gpt56Models.Any(d => d.Slug.Equals(m.Slug, StringComparison.OrdinalIgnoreCase))).ToList())
                    group.Models.Remove(model);
                foreach (var model in group.Models)
                {
                    // Direktmodelle: ConfigureProvider braucht IsUserDefined für den provider.openai.models-Eintrag.
                    model.IsUserDefined = true;
                }
            }

            if (string.Equals(group.Id, GoogleGroupId, StringComparison.OrdinalIgnoreCase))
            {
                // Die erste Google-Reihe (1.24.84) zielte auf die Gemini CLI; deren Modelle bedient
                // die Antigravity CLI mit Privat-Abo nicht -> einmalig entfernen, neue Vorgaben
                // einmalig nachtragen (danach bleiben Nutzer-Aenderungen unangetastet).
                group.Models.Where(model => RetiredGeminiCliSlugs.Contains(model.Slug, StringComparer.OrdinalIgnoreCase))
                    .ToList().ForEach(model => group.Models.Remove(model));
                foreach (var definition in defaults.Models)
                {
                    if (group.KnownSyncedModelSlugs.Contains(definition.Slug, StringComparer.OrdinalIgnoreCase)) continue;
                    if (!group.Models.Any(model => string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase)))
                        group.Models.Insert(Math.Min(defaults.Models.IndexOf(definition), group.Models.Count), definition);
                    AddUnique(group.KnownSyncedModelSlugs, definition.Slug);
                }
            }

            if (string.Equals(group.Id, MoonshotGroupId, StringComparison.OrdinalIgnoreCase))
            {
                // Neue Kimi-Vorgaben (z. B. K3.1 Pro) einmalig nachtragen; danach bleiben
                // Nutzer-Aenderungen unangetastet. Gruppen von vor 1.24.102 haben keinen Merker:
                // ihre damaligen Vorgaben gelten als bekannt, damit geloeschte nicht zurueckkehren.
                if (group.KnownSyncedModelSlugs.Count == 0)
                    foreach (var slug in LegacyMoonshotSlugs) AddUnique(group.KnownSyncedModelSlugs, slug);
                for (var index = 0; index < defaults.Models.Count; index++)
                {
                    var definition = defaults.Models[index];
                    if (group.KnownSyncedModelSlugs.Contains(definition.Slug, StringComparer.OrdinalIgnoreCase)) continue;
                    if (!group.Models.Any(model => string.Equals(model.Slug, definition.Slug, StringComparison.OrdinalIgnoreCase)))
                        group.Models.Insert(Math.Min(index, group.Models.Count), definition);
                    AddUnique(group.KnownSyncedModelSlugs, definition.Slug);
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
                        if (!outdated.HasCustomDisplayName || outdated.DisplayName == "Claude Sonnet 5")
                            outdated.DisplayName = migration.DisplayName;
                        else if (outdated.DisplayName.StartsWith("Claude Fable 5.1", StringComparison.OrdinalIgnoreCase))
                            outdated.DisplayName = "Claude Fable 5.2" + outdated.DisplayName["Claude Fable 5.1".Length..];
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
            Model("claude-fable-5-2[1m]", "Claude Fable 5.2 (1M)", "anthropic", "Anthropic"),
            Model("claude-opus-4-8[1m]", "Claude Opus 4.8 (1M)", "anthropic", "Anthropic"),
            Model("claude-sonnet-5-5[1m]", "Claude Sonnet 5.5 (1M)", "anthropic", "Anthropic"),
            Model("claude-haiku-5-5", "Claude Haiku 5.5", "anthropic", "Anthropic"),
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
            Model("gpt-6.2-astra", "GPT-6.2 Astra", "openai", "OpenAI"),
            Model("gpt-6.2-sol", "GPT-6.2 Sol", "openai", "OpenAI"),
            Model("gpt-6.1-terra", "GPT-6.1 Terra", "openai", "OpenAI"),
            Model(Gpt56TerraSlug, "GPT-5.6 Terra", "openai", "OpenAI"),
            Model("gpt-6-luna", "GPT-6 Luna", "openai", "OpenAI"),
        }),
        // Kimi-Code-Abo (kimi.ai), in OpenCode per /connect -> "Kimi For Coding" mit API-Schluessel
        // angemeldet. Die Modelle stehen nativ im OpenCode-Katalog unter kimi-code-plan-global.
        CreateGroup(MoonshotGroupId, "Moonshot AI", MoonshotProviderId, "Moonshot AI", new[]
        {
            Model("k3.1-pro", "Kimi K3.1 Pro", MoonshotProviderId, "Moonshot AI"),
            Model("k3.1-pro-highspeed", "Kimi K3.1 Pro HighSpeed", MoonshotProviderId, "Moonshot AI"),
            Model("k3", "Kimi K3 (1M)", MoonshotProviderId, "Moonshot AI"),
            Model("k3-256k", "Kimi K3 256K", MoonshotProviderId, "Moonshot AI"),
            Model("kimi-for-coding", "Kimi for Coding (K2.8 Preview)", MoonshotProviderId, "Moonshot AI"),
            Model("kimi-for-coding-highspeed", "Kimi for Coding HighSpeed", MoonshotProviderId, "Moonshot AI"),
        }),
        // Google-AI-Abo (Google-Konto per OAuth) in der Antigravity CLI (agy). Seit 18.06.2026 nimmt
        // nur noch agy die Privat-Abos an. Modelle laut antigravity.google/docs/models (14.10.2026):
        // Gemini 4 Argon nur mit AI Ultra, Gemini 3.8 Flash ab Free/Pro. 3.1 Pro, 3.6/3.7 Flash sind
        // dort nur Enterprise. Gemini 4 Neon (agy 1.5.13), Hydrogen (agy 1.5.8) und Carbon (agy 1.5.15) seit 23.11.2026. Seit agy 1.3.4 genuegt der Basisname, die Stufe kommt per --effort.
        CreateGroup(GoogleGroupId, "Google", GoogleProviderId, "Google", new[]
        {
            Model("gemini-4-neon", "Gemini 4 Neon", GoogleProviderId, "Google"),
            Model("gemini-4-hydrogen", "Gemini 4 Hydrogen", GoogleProviderId, "Google"),
            Model("gemini-4-carbon", "Gemini 4 Carbon", GoogleProviderId, "Google"),
            Model("gemini-4-argon", "Gemini 4 Argon (Ultra)", GoogleProviderId, "Google"),
            Model("gemini-3.8-flash", "Gemini 3.8 Flash", GoogleProviderId, "Google"),
        }),
        CreateGroup("nvidia", "NVIDIA", NvidiaProviderId, NvidiaProviderName, NvidiaFreeModels),
        // Lokale LM-Studio-Modelle. Die Liste kommt beim Start live vom lokalen Server
        // (SyncLmStudioModels); die Vorgabe hier ist nur der Platzhalter, damit der Reiter
        // auch ohne laufenden Server existiert.
        CreateGroup("lmstudio", "LM Studio", LmStudioService.ProviderId, LmStudioService.ProviderName,
            Array.Empty<ModelEntry>()),
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
    public const string MoonshotGroupId = "moonshot";
    public const string MoonshotProviderId = "kimi-code-plan-global";
    /// <summary>Kimi-Vorgaben bis 1.24.101, als die Moonshot-Gruppe noch keinen Nachtrags-Merker fuehrte.</summary>
    private static readonly string[] LegacyMoonshotSlugs = ["k3", "k3-256k", "kimi-for-coding", "kimi-for-coding-highspeed"];
    public const string GoogleGroupId = "google";
    public const string GoogleProviderId = "google";

    /// <summary>Modelle der ersten Google-Reihe (Gemini CLI), die agy mit Privat-Abo nicht anbietet.</summary>
    private static readonly string[] RetiredGeminiCliSlugs =
    [
        "auto", "gemini-3.1-pro-preview", "gemini-3-pro-preview", "gemini-3.5-flash",
        "gemini-3-flash-preview", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite"
    ];
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
        KnownSyncedModelSlugs = string.Equals(id, "openrouter-free", StringComparison.OrdinalIgnoreCase) ||
                                string.Equals(id, MoonshotGroupId, StringComparison.OrdinalIgnoreCase)
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
        if (string.Equals(providerId, "anthropic", StringComparison.OrdinalIgnoreCase))
            slug = NormalizeAnthropicSlug(slug);
        return slug;
    }

    /// <summary>
    /// Anthropic schreibt die Nebenversion mit Bindestrich ("claude-fable-5-1"), nicht mit Punkt.
    /// Ein von Hand eingetragenes "claude-fable-5.1" kennt Claude Code nicht: es meldet "There's an
    /// issue with the selected model" und startet mit dem Standardmodell (Fable 5) weiter — der
    /// Fehler faellt also erst im laufenden Terminal auf. Deshalb wird die Punkt-Schreibweise hier
    /// still auf die gueltige Form gezogen. Das angehaengte "[1m]" bleibt unberuehrt.
    /// </summary>
    private static string NormalizeAnthropicSlug(string slug) =>
        System.Text.RegularExpressions.Regex.Replace(slug, @"^(claude-[a-z]+-\d+)\.(\d+)", "$1-$2");

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
