import Foundation

/// Persistente Liste der gepflegten Modelle. 1:1-Port von Services/ModelRegistry.cs.
///
/// Die Liste liegt im Repo (~/proggs/OpenLauncher/models.json) - NICHT im Anwendungsdatenordner.
/// Sie enthaelt keine Geheimnisse (nur Modell-IDs, Anzeigenamen, Sortierung) und ist damit auf
/// Windows und macOS dieselbe Datei: was auf einem Rechner gepflegt wird, ist nach Commit und Push
/// ueberall identisch vorhanden.
final class ModelRegistry {
    private static let gpt56SolSlug = "gpt-5.6-sol"
    private static let gpt56SolFastSlug = "gpt-5.6-sol-fast"
    private static let gpt56TerraSlug = "gpt-5.6-terra"
    private static let gpt56TerraFastSlug = "gpt-5.6-terra-fast"
    private static let gpt56LunaSlug = "gpt-5.6-luna"
    private static let gpt56LunaFastSlug = "gpt-5.6-luna-fast"

    private static let claudeOpus5Slug = "claude-opus-5[1m]"

    /// Anthropic-Modelle, die auch in bereits gespeicherte models.json nachgetragen werden
    /// (einmalig, per knownSyncedModelSlugs gemerkt - manuell entfernte Modelle bleiben weg).
    private static let anthropicModels: [(slug: String, displayName: String)] = [
        (claudeOpus5Slug, "Claude Opus 5 (1M)")
    ]

    /// Hebt die grossen Anthropic-Modelle auf ihre 1M-Kontext-Variante. Der Launcher reicht den
    /// Slug unveraendert an "claude --model" weiter; ohne "[1m]" startet Claude Code mit dem
    /// Standard-Kontextfenster. Laeuft genau einmal (Merker = neuer Slug in knownSyncedModelSlugs),
    /// damit eine spaetere manuelle Rueckstellung bestehen bleibt.
    private static let anthropicOneMillionMigrations: [(oldSlug: String, newSlug: String, displayName: String)] = [
        ("claude-opus-5", claudeOpus5Slug, "Claude Opus 5 (1M)"),
        ("claude-fable-5", "claude-fable-5[1m]", "Claude Fable 5 (1M)"),
        ("claude-sonnet-5", "claude-sonnet-5[1m]", "Claude Sonnet 5 (1M)"),
        ("claude-opus-4-8", "claude-opus-4-8[1m]", "Claude Opus 4.8 (1M)")
    ]

    private static var anthropicManagedSlugs: [String] {
        anthropicModels.map(\.slug) + anthropicOneMillionMigrations.map(\.newSlug)
    }

    private static let gpt56Models: [(slug: String, displayName: String)] = [
        (gpt56SolSlug, "GPT-5.6 Sol"),
        (gpt56SolFastSlug, "GPT-5.6 Sol Fast"),
        (gpt56TerraSlug, "GPT-5.6 Terra"),
        (gpt56TerraFastSlug, "GPT-5.6 Terra Fast"),
        (gpt56LunaSlug, "GPT-5.6 Luna"),
        (gpt56LunaFastSlug, "GPT-5.6 Luna Fast")
    ]

    private static var filePath: String {
        (Paths.repoRoot as NSString).appendingPathComponent("models.json")
    }

    private static var legacyFilePath: String {
        (Paths.appSupport as NSString).appendingPathComponent("models.json")
    }

    private static let nvidiaProviderId = ModelEntry.nvidiaProviderId
    private static let nvidiaProviderName = "NVIDIA"

    var groups: [ModelGroupEntry] = []

    static func load() -> ModelRegistry {
        let registry = ModelRegistry()
        migrateLegacyFile()

        if Paths.fileExists(filePath), let data = FileManager.default.contents(atPath: filePath) {
            if let parsed = try? JSONDecoder().decode([ModelGroupEntry].self, from: data),
               !parsed.isEmpty, parsed.contains(where: { !$0.id.isEmpty }) {
                registry.groups = parsed
                registry.repairAndNormalize()
                registry.save()
                Logger.shared.info("ModelRegistry", "load", "\(parsed.count) Modellgruppen geladen", ["filePath": filePath])
                return registry
            }

            // Alte Fassung: flache Modell-Liste ohne Gruppen.
            if let oldList = try? JSONDecoder().decode([ModelEntry].self, from: data),
               oldList.contains(where: { !$0.slug.isEmpty }) {
                registry.groups = createDefaults(openRouterModels: oldList)
                registry.save()
                Logger.shared.info("ModelRegistry", "load", "\(oldList.count) Modelle aus alter Liste migriert", ["filePath": filePath])
                return registry
            }

            Logger.shared.warn("ModelRegistry", "load", "models.json nicht lesbar, Defaults werden verwendet")
        }

        registry.groups = createDefaults()
        registry.save()
        return registry
    }

    /// Uebernimmt eine Modell-Liste vom frueheren Ort einmalig ins Repo. Kopiert statt verschoben:
    /// die alte Datei bleibt als Sicherheitsnetz liegen. Existiert im Repo bereits eine Liste
    /// (etwa frisch aus Git geholt), gilt diese.
    private static func migrateLegacyFile() {
        guard !Paths.fileExists(filePath), Paths.fileExists(legacyFilePath) else { return }
        Paths.ensureDirectory(Paths.repoRoot)
        do {
            try FileManager.default.copyItem(atPath: legacyFilePath, toPath: filePath)
            Logger.shared.info("ModelRegistry", "migrateLegacyFile", "Modell-Liste ins Repo uebernommen",
                               ["legacy": legacyFilePath, "target": filePath])
        } catch {
            Logger.shared.warn("ModelRegistry", "migrateLegacyFile", "Uebernahme fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    func save() {
        Paths.ensureDirectory(Paths.repoRoot)
        // Ueber JSONNode statt JSONEncoder: nur so entsteht byte-gleiche Ausgabe zum Windows-
        // Launcher (Schluesselreihenfolge, "Key": Value, zwei Leerzeichen Einrueckung). models.json
        // liegt im Repo und wird von beiden Seiten geschrieben - unterschiedliche Formatierung
        // wuerde bei jedem Start einen Riesen-Diff und Abgleich-Konflikte erzeugen.
        let json = JSONNode.array(groups.map(\.jsonNode)).serialized()
        if !Paths.writeAtomic(json, to: ModelRegistry.filePath) {
            Logger.shared.error("ModelRegistry", "save", "Modell-Liste konnte nicht geschrieben werden")
        }
    }

    // ===================== Bearbeiten =====================

    @discardableResult
    func addModel(_ group: ModelGroupEntry, slug rawSlug: String, displayName: String) -> Bool {
        let slug = ModelRegistry.normalizeSlugForGroup(rawSlug, providerId: group.providerId)
        if group.models.contains(where: { $0.slug.caseInsensitiveCompare(slug) == .orderedSame }) {
            Logger.shared.warn("ModelRegistry", "addModel", "Slug existiert bereits: \(slug)")
            return false
        }
        let trimmed = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        group.models.append(ModelEntry(
            slug: slug,
            displayName: trimmed.isEmpty ? ModelRegistry.toDisplayName(slug) : trimmed,
            providerId: group.providerId,
            providerName: group.providerName,
            hasCustomDisplayName: !trimmed.isEmpty,
            isUserDefined: true
        ))
        save()
        Logger.shared.info("ModelRegistry", "addModel", "hinzugefügt: \(slug)", ["group": group.title])
        return true
    }

    /// Bearbeitet einen bestehenden Eintrag: Modell-ID (Slug inkl. Parameter wie "[1m]"),
    /// Anzeigename und optional die Kategorie. false, wenn der Ziel-Slug dort schon existiert.
    @discardableResult
    func updateModel(_ group: ModelGroupEntry, model: ModelEntry, targetGroup: ModelGroupEntry,
                     slug: String, displayName: String) -> Bool {
        guard let index = group.models.firstIndex(where: { $0 === model }) else { return false }

        let normalized = ModelRegistry.normalizeSlugForGroup(slug, providerId: targetGroup.providerId)
        if normalized.trimmingCharacters(in: .whitespaces).isEmpty { return false }
        if targetGroup.models.contains(where: { $0 !== model && $0.slug.caseInsensitiveCompare(normalized) == .orderedSame }) {
            Logger.shared.warn("ModelRegistry", "updateModel", "Slug existiert bereits: \(normalized)", ["group": targetGroup.title])
            return false
        }

        let oldSlug = model.slug
        let trimmedName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let hasCustomName = !trimmedName.isEmpty

        // Der alte Slug darf im OpenRouterFree-Sync nicht wieder auftauchen (gleiche Logik wie beim
        // Entfernen) - sonst legte der naechste Abgleich das umbenannte Modell erneut daneben an.
        if oldSlug.caseInsensitiveCompare(normalized) != .orderedSame,
           group.id.caseInsensitiveCompare("openrouter-free") == .orderedSame {
            ModelRegistry.addUnique(&group.hiddenModelSlugs, oldSlug)
            ModelRegistry.addUnique(&group.knownSyncedModelSlugs, oldSlug)
        }

        if group !== targetGroup {
            group.models.remove(at: index)
            if group.id.caseInsensitiveCompare("openrouter-free") == .orderedSame {
                ModelRegistry.addUnique(&group.hiddenModelSlugs, oldSlug)
                ModelRegistry.addUnique(&group.knownSyncedModelSlugs, oldSlug)
            }
            model.providerId = targetGroup.providerId
            model.providerName = targetGroup.providerName
            targetGroup.models.append(model)
        }

        model.slug = normalized
        model.displayName = hasCustomName ? trimmedName : ModelRegistry.toDisplayName(normalized)
        model.hasCustomDisplayName = hasCustomName
        model.isUserDefined = true

        save()
        Logger.shared.info("ModelRegistry", "updateModel", "bearbeitet: \(oldSlug) -> \(normalized)",
                           ["group": group.title, "target": targetGroup.title])
        return true
    }

    func removeAt(_ group: ModelGroupEntry, index: Int) {
        guard index >= 0 && index < group.models.count else { return }
        let model = group.models.remove(at: index)
        if group.id.caseInsensitiveCompare("openrouter-free") == .orderedSame {
            ModelRegistry.addUnique(&group.hiddenModelSlugs, model.slug)
            ModelRegistry.addUnique(&group.knownSyncedModelSlugs, model.slug)
        }
        save()
        Logger.shared.info("ModelRegistry", "removeAt", "entfernt: \(model.slug)", ["group": group.title])
    }

    func moveModel(_ group: ModelGroupEntry, from: Int, to rawTo: Int) {
        guard from >= 0 && from < group.models.count else { return }
        var to = rawTo
        if to < 0 { to = 0 }
        if to > group.models.count { to = group.models.count }
        if from == to { return }
        let item = group.models.remove(at: from)
        if to > from { to -= 1 }
        group.models.insert(item, at: Swift.min(Swift.max(to, 0), group.models.count))
        save()
        Logger.shared.info("ModelRegistry", "moveModel", "\(group.title):\(from) -> \(to)")
    }

    @discardableResult
    func moveModel(_ sourceGroup: ModelGroupEntry, from: Int, to targetGroup: ModelGroupEntry, index rawTo: Int) -> Bool {
        if sourceGroup === targetGroup {
            moveModel(sourceGroup, from: from, to: rawTo)
            return true
        }
        guard from >= 0 && from < sourceGroup.models.count else { return false }

        let item = sourceGroup.models[from]
        let targetSlug = ModelRegistry.normalizeSlugForGroup(item.slug, providerId: targetGroup.providerId)
        if targetGroup.models.contains(where: { $0.slug.caseInsensitiveCompare(targetSlug) == .orderedSame }) {
            Logger.shared.warn("ModelRegistry", "moveModel", "Ziel enthält Slug bereits: \(targetSlug)",
                               ["source": sourceGroup.title, "target": targetGroup.title])
            return false
        }

        sourceGroup.models.remove(at: from)
        item.slug = targetSlug
        item.providerId = targetGroup.providerId
        item.providerName = targetGroup.providerName
        var to = rawTo
        if to < 0 { to = targetGroup.models.count }
        if to > targetGroup.models.count { to = targetGroup.models.count }
        targetGroup.models.insert(item, at: to)
        save()
        Logger.shared.info("ModelRegistry", "moveModel", "\(sourceGroup.title):\(from) -> \(targetGroup.title):\(to)")
        return true
    }

    func moveGroup(from: Int, to: Int) {
        guard from >= 0, from < groups.count, to >= 0, to < groups.count, from != to else { return }
        let item = groups.remove(at: from)
        groups.insert(item, at: to)
        save()
        Logger.shared.info("ModelRegistry", "moveGroup", "\(from) -> \(to)")
    }

    // ===================== Sync =====================

    func syncOpenRouterFreeModels(_ remoteModels: [ModelEntry]) {
        syncGroupModels(groupId: "openrouter-free", remoteModels: remoteModels)
    }

    /// Gleicht die Gruppe "LM Studio" mit den Modellen ab, die der lokale LM-Studio-Server gerade
    /// anbietet. Entfernte Modelle wandern in die Ausgeblendet-Liste, eigene Eintraege und
    /// Umbenennungen bleiben erhalten - gleiche Regeln wie beim OpenRouterFree-Sync.
    func syncLmStudioModels(_ localModels: [ModelEntry]) {
        syncGroupModels(groupId: "lmstudio", remoteModels: localModels)
    }

    private func syncGroupModels(groupId: String, remoteModels: [ModelEntry]) {
        guard let group = groups.first(where: { $0.id.caseInsensitiveCompare(groupId) == .orderedSame }) else { return }

        var seen = Set<String>()
        let remote: [ModelEntry] = remoteModels
            .filter { !$0.slug.trimmingCharacters(in: .whitespaces).isEmpty }
            .filter { seen.insert($0.slug.lowercased()).inserted }
            .map { ModelRegistry.normalizeModel($0, providerId: group.providerId, providerName: group.providerName) }
        if remote.isEmpty { return }

        let currentSlugs = Set(group.models.map { $0.slug.lowercased() })
        if group.knownSyncedModelSlugs.isEmpty {
            for model in group.models { ModelRegistry.addUnique(&group.knownSyncedModelSlugs, model.slug) }
        }

        for known in group.knownSyncedModelSlugs where !currentSlugs.contains(known.lowercased()) {
            ModelRegistry.addUnique(&group.hiddenModelSlugs, known)
        }

        let hiddenSlugs = Set(group.hiddenModelSlugs.map { $0.lowercased() })
        var remoteBySlug: [String: ModelEntry] = [:]
        for model in remote { remoteBySlug[model.slug.lowercased()] = model }
        var merged: [ModelEntry] = []

        for existing in group.models {
            let key = existing.slug.lowercased()
            if hiddenSlugs.contains(key) { continue }
            guard let remoteModel = remoteBySlug[key] else {
                // Selbst hinzugefuegte/bearbeitete Eintraege ueberleben den Sync auch dann, wenn ihr
                // Slug in der Remote-Liste fehlt. Ausgeblendete Eintraege muessen ebenfalls erhalten
                // bleiben, damit sie in der Wiederherstellungsansicht nicht verloren gehen.
                if existing.isUserDefined || existing.isHidden { merged.append(existing) }
                continue
            }
            existing.providerId = group.providerId
            existing.providerName = group.providerName
            if !existing.hasCustomDisplayName { existing.displayName = remoteModel.displayName }
            merged.append(existing)
        }

        var mergedSlugs = Set(merged.map { $0.slug.lowercased() })
        for remoteModel in remote {
            let key = remoteModel.slug.lowercased()
            if hiddenSlugs.contains(key) || mergedSlugs.contains(key) { continue }
            merged.append(remoteModel)
            mergedSlugs.insert(key)
        }

        group.models = merged
        var uniqueRemote: [String] = []
        for model in remote { ModelRegistry.addUnique(&uniqueRemote, model.slug) }
        group.knownSyncedModelSlugs = uniqueRemote
        var uniqueHidden: [String] = []
        for slug in group.hiddenModelSlugs { ModelRegistry.addUnique(&uniqueHidden, slug) }
        group.hiddenModelSlugs = uniqueHidden

        save()
        Logger.shared.info("ModelRegistry", "syncGroupModels",
                           "\(group.title): \(group.models.count) sichtbare, \(group.hiddenModelSlugs.count) ausgeblendete Modelle")
    }

    // ===================== Reparatur / Normalisierung =====================

    private func repairAndNormalize() {
        for defaults in ModelRegistry.createDefaults() {
            guard let group = groups.first(where: { $0.id.caseInsensitiveCompare(defaults.id) == .orderedSame }) else {
                groups.append(defaults)
                continue
            }
            group.providerId = defaults.providerId
            group.providerName = defaults.providerName
            group.title = defaults.title

            if (group.models.isEmpty || group.models.allSatisfy { $0.slug.trimmingCharacters(in: .whitespaces).isEmpty }),
               group.id.caseInsensitiveCompare("openai") != .orderedSame {
                group.models = defaults.models
                continue
            }

            var seen = Set<String>()
            group.models = group.models
                .filter { !$0.slug.trimmingCharacters(in: .whitespaces).isEmpty }
                .filter { seen.insert($0.slug.lowercased()).inserted }

            for model in group.models {
                model.providerId = group.providerId
                model.providerName = group.providerName
                if model.displayName.trimmingCharacters(in: .whitespaces).isEmpty {
                    model.displayName = ModelRegistry.toDisplayName(model.slug)
                }
            }

            var uniqueHidden: [String] = []
            for slug in group.hiddenModelSlugs where !slug.trimmingCharacters(in: .whitespaces).isEmpty {
                ModelRegistry.addUnique(&uniqueHidden, slug)
            }
            group.hiddenModelSlugs = uniqueHidden

            var uniqueKnown: [String] = []
            for slug in group.knownSyncedModelSlugs where !slug.trimmingCharacters(in: .whitespaces).isEmpty {
                ModelRegistry.addUnique(&uniqueKnown, slug)
            }
            group.knownSyncedModelSlugs = uniqueKnown

            if group.id.caseInsensitiveCompare("openai") == .orderedSame {
                for definition in ModelRegistry.gpt56Models {
                    if !group.knownSyncedModelSlugs.contains(where: { $0.caseInsensitiveCompare(definition.slug) == .orderedSame }) {
                        if !group.models.contains(where: { $0.slug.caseInsensitiveCompare(definition.slug) == .orderedSame }) {
                            group.models.append(ModelRegistry.model(definition.slug, definition.displayName, "openai", "OpenAI"))
                        }
                        ModelRegistry.addUnique(&group.knownSyncedModelSlugs, definition.slug)
                    }
                    if let model = group.models.first(where: { $0.slug.caseInsensitiveCompare(definition.slug) == .orderedSame }),
                       !model.hasCustomDisplayName {
                        model.displayName = definition.displayName
                    }
                }
            }

            if group.id.caseInsensitiveCompare("entropic") == .orderedSame {
                // Zuerst die 1M-Umstellung, danach der Nachtrag: beide teilen sich den Merker
                // (neuer Slug in knownSyncedModelSlugs), sonst legte der Nachtrag Opus 5 nach der
                // Umstellung ein zweites Mal an.
                for migration in ModelRegistry.anthropicOneMillionMigrations {
                    if group.knownSyncedModelSlugs.contains(where: { $0.caseInsensitiveCompare(migration.newSlug) == .orderedSame }) { continue }

                    let outdated = group.models.first { $0.slug.caseInsensitiveCompare(migration.oldSlug) == .orderedSame }
                    let alreadyPresent = group.models.contains { $0.slug.caseInsensitiveCompare(migration.newSlug) == .orderedSame }
                    if let outdated, !alreadyPresent {
                        outdated.slug = migration.newSlug
                        if !outdated.hasCustomDisplayName { outdated.displayName = migration.displayName }
                        Logger.shared.info("ModelRegistry", "repairAndNormalize",
                                           "1M-Variante gesetzt: \(migration.oldSlug) -> \(migration.newSlug)")
                    }
                    ModelRegistry.addUnique(&group.knownSyncedModelSlugs, migration.newSlug)
                }

                for definition in ModelRegistry.anthropicModels {
                    if !group.knownSyncedModelSlugs.contains(where: { $0.caseInsensitiveCompare(definition.slug) == .orderedSame }) {
                        if !group.models.contains(where: { $0.slug.caseInsensitiveCompare(definition.slug) == .orderedSame }) {
                            group.models.insert(ModelRegistry.model(definition.slug, definition.displayName, "anthropic", "Anthropic"), at: 0)
                        }
                        ModelRegistry.addUnique(&group.knownSyncedModelSlugs, definition.slug)
                    }
                    if let model = group.models.first(where: { $0.slug.caseInsensitiveCompare(definition.slug) == .orderedSame }),
                       !model.hasCustomDisplayName {
                        model.displayName = definition.displayName
                    }
                }
            }
        }
    }

    // ===================== Vorgaben =====================

    private static func createDefaults(openRouterModels: [ModelEntry]? = nil) -> [ModelGroupEntry] {
        [
            createGroup("openrouter", "OpenRouter", "openrouter", "OpenRouter", normalizeOpenRouter(openRouterModels)),
            createGroup("entropic", "Anthropic", "anthropic", "Anthropic", [
                model(claudeOpus5Slug, "Claude Opus 5 (1M)", "anthropic", "Anthropic"),
                model("claude-fable-5[1m]", "Claude Fable 5 (1M)", "anthropic", "Anthropic"),
                model("claude-opus-4-8[1m]", "Claude Opus 4.8 (1M)", "anthropic", "Anthropic"),
                model("claude-sonnet-5[1m]", "Claude Sonnet 5 (1M)", "anthropic", "Anthropic"),
                model("claude-haiku-4-5", "Claude Haiku 4.5", "anthropic", "Anthropic"),
                model("claude-opus-4-7", "Claude Opus 4.7", "anthropic", "Anthropic"),
                model("claude-opus-4-6", "Claude Opus 4.6", "anthropic", "Anthropic"),
                model("claude-sonnet-4-6", "Claude Sonnet 4.6", "anthropic", "Anthropic"),
                model("claude-opus-4-5", "Claude Opus 4.5", "anthropic", "Anthropic"),
                model("claude-sonnet-4-5", "Claude Sonnet 4.5", "anthropic", "Anthropic")
            ]),
            createGroup("openrouter-free", "OpenRouterFree", "openrouter", "OpenRouter", [
                model("poolside/laguna-xs-2.1:free", "Poolside Laguna XS 2.1 Free", "openrouter", "OpenRouter"),
                model("cohere/north-mini-code:free", "Cohere North Mini Code Free", "openrouter", "OpenRouter"),
                model("nvidia/nemotron-3.5-content-safety:free", "NVIDIA Nemotron 3.5 Content Safety Free", "openrouter", "OpenRouter"),
                model("nvidia/nemotron-3-ultra-550b-a55b:free", "NVIDIA Nemotron 3 Ultra Free", "openrouter", "OpenRouter"),
                model("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", "NVIDIA Nemotron 3 Nano Omni Free", "openrouter", "OpenRouter"),
                model("poolside/laguna-xs.2:free", "Poolside Laguna XS.2 Free", "openrouter", "OpenRouter"),
                model("poolside/laguna-m.1:free", "Poolside Laguna M.1 Free", "openrouter", "OpenRouter"),
                model("google/gemma-4-26b-a4b-it:free", "Google Gemma 4 26B A4B Free", "openrouter", "OpenRouter"),
                model("google/gemma-4-31b-it:free", "Google Gemma 4 31B Free", "openrouter", "OpenRouter"),
                model("nvidia/nemotron-3-super-120b-a12b:free", "NVIDIA Nemotron 3 Super Free", "openrouter", "OpenRouter"),
                model("liquid/lfm-2.5-1.2b-thinking:free", "LiquidAI LFM2.5 1.2B Thinking Free", "openrouter", "OpenRouter"),
                model("liquid/lfm-2.5-1.2b-instruct:free", "LiquidAI LFM2.5 1.2B Instruct Free", "openrouter", "OpenRouter"),
                model("nvidia/nemotron-3-nano-30b-a3b:free", "NVIDIA Nemotron 3 Nano 30B A3B Free", "openrouter", "OpenRouter"),
                model("nvidia/nemotron-nano-12b-v2-vl:free", "NVIDIA Nemotron Nano 12B 2 VL Free", "openrouter", "OpenRouter"),
                model("qwen/qwen3-next-80b-a3b-instruct:free", "Qwen3 Next 80B A3B Instruct Free", "openrouter", "OpenRouter"),
                model("nvidia/nemotron-nano-9b-v2:free", "NVIDIA Nemotron Nano 9B V2 Free", "openrouter", "OpenRouter"),
                model("openai/gpt-oss-120b:free", "OpenAI GPT OSS 120B Free", "openrouter", "OpenRouter"),
                model("openai/gpt-oss-20b:free", "OpenAI GPT OSS 20B Free", "openrouter", "OpenRouter"),
                model("qwen/qwen3-coder:free", "Qwen3 Coder Free", "openrouter", "OpenRouter"),
                model("cognitivecomputations/dolphin-mistral-24b-venice-edition:free", "Venice Uncensored Free", "openrouter", "OpenRouter"),
                model("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B Instruct Free", "openrouter", "OpenRouter"),
                model("meta-llama/llama-3.2-3b-instruct:free", "Llama 3.2 3B Instruct Free", "openrouter", "OpenRouter"),
                model("nousresearch/hermes-3-llama-3.1-405b:free", "Hermes 3 405B Instruct Free", "openrouter", "OpenRouter")
            ]),
            createGroup("opencode-zen-free", "OpenCode Zen Free", "opencode", "OpenCode Zen", [
                model("gpt-5-nano", "GPT-5 Nano", "opencode", "OpenCode Zen"),
                model("deepseek-v4-flash-free", "DeepSeek V4 Flash Free", "opencode", "OpenCode Zen"),
                model("mimo-v2.5-free", "MiMo V2.5 Free", "opencode", "OpenCode Zen"),
                model("nemotron-3-ultra-free", "Nemotron 3 Ultra Free", "opencode", "OpenCode Zen"),
                model("north-mini-code-free", "North Mini Code Free", "opencode", "OpenCode Zen")
            ]),
            createGroup("openai", "OpenAI", "openai", "OpenAI", [
                model("gpt-5.3-codex-spark", "GPT-5.3 Codex Spark", "openai", "OpenAI"),
                model("gpt-5.4", "GPT-5.4", "openai", "OpenAI"),
                model("gpt-5.4-fast", "GPT-5.4 Fast", "openai", "OpenAI"),
                model("gpt-5.4-mini", "GPT-5.4 Mini", "openai", "OpenAI"),
                model("gpt-5.4-mini-fast", "GPT-5.4 Mini Fast", "openai", "OpenAI"),
                model("gpt-5.5", "GPT-5.5", "openai", "OpenAI"),
                model("gpt-5.5-fast", "GPT-5.5 Fast", "openai", "OpenAI"),
                model(gpt56SolSlug, "GPT-5.6 Sol", "openai", "OpenAI"),
                model(gpt56SolFastSlug, "GPT-5.6 Sol Fast", "openai", "OpenAI"),
                model(gpt56TerraSlug, "GPT-5.6 Terra", "openai", "OpenAI"),
                model(gpt56TerraFastSlug, "GPT-5.6 Terra Fast", "openai", "OpenAI"),
                model(gpt56LunaSlug, "GPT-5.6 Luna", "openai", "OpenAI"),
                model(gpt56LunaFastSlug, "GPT-5.6 Luna Fast", "openai", "OpenAI")
            ]),
            createGroup("opencode-go", "OpenCode-Go", "opencode-go", "OpenCode-Go", [
                model("deepseek-v4-flash", "DeepSeek V4 Flash", "opencode-go", "OpenCode-Go"),
                model("deepseek-v4-pro", "DeepSeek V4 Pro", "opencode-go", "OpenCode-Go"),
                model("glm-5.1", "GLM 5.1", "opencode-go", "OpenCode-Go"),
                model("glm-5.2", "GLM 5.2", "opencode-go", "OpenCode-Go"),
                model("kimi-k2.6", "Kimi K2.6", "opencode-go", "OpenCode-Go"),
                model("kimi-k2.7-code", "Kimi K2.7 Code", "opencode-go", "OpenCode-Go"),
                model("mimo-v2.5", "MiMo V2.5", "opencode-go", "OpenCode-Go"),
                model("mimo-v2.5-pro", "MiMo V2.5 Pro", "opencode-go", "OpenCode-Go"),
                model("minimax-m2.7", "MiniMax M2.7", "opencode-go", "OpenCode-Go"),
                model("minimax-m3", "MiniMax M3", "opencode-go", "OpenCode-Go"),
                model("qwen3.6-plus", "Qwen3.6 Plus", "opencode-go", "OpenCode-Go"),
                model("qwen3.7-max", "Qwen3.7 Max", "opencode-go", "OpenCode-Go"),
                model("qwen3.7-plus", "Qwen3.7 Plus", "opencode-go", "OpenCode-Go")
            ]),
            createGroup("nvidia", "NVIDIA", nvidiaProviderId, nvidiaProviderName, nvidiaFreeModels),
            // Lokale LM-Studio-Modelle. Die Liste kommt beim Start live vom lokalen Server
            // (syncLmStudioModels); die Vorgabe hier ist nur der Platzhalter, damit der Reiter
            // auch ohne laufenden Server existiert.
            createGroup("lmstudio", "LM Studio", LmStudioService.providerId, LmStudioService.providerName, [])
        ]
    }

    /// Kostenlose NIM-Endpunkte von build.nvidia.com, die OpenCode ueber den models.dev-Provider
    /// "nvidia" starten kann. Aufgenommen ist nur, was live gelistet, in models.dev kostenlos
    /// gefuehrt und tool_call-faehig ist. Sortierung: leistungsstaerkste zuerst.
    private static var nvidiaFreeModels: [ModelEntry] {
        [
            nvidiaModel("z-ai/glm-5.2", "GLM 5.2 (1M)"),
            nvidiaModel("thinkingmachines/inkling", "Inkling (1M)"),
            nvidiaModel("minimaxai/minimax-m3", "MiniMax M3 (1M)"),
            nvidiaModel("poolside/laguna-xs-2.1", "Poolside Laguna XS 2.1"),
            nvidiaModel("stepfun-ai/step-3.7-flash", "Step 3.7 Flash"),
            nvidiaModel("mistralai/mistral-medium-3.5-128b", "Mistral Medium 3.5"),
            nvidiaModel("google/gemma-4-31b-it", "Gemma 4 31B"),
            nvidiaModel("openai/gpt-oss-120b", "GPT-OSS 120B"),
            nvidiaModel("openai/gpt-oss-20b", "GPT-OSS 20B"),
            nvidiaModel("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "Nemotron 3 Nano Omni Reasoning"),
            nvidiaModel("nvidia/nemotron-3-nano-30b-a3b", "Nemotron 3 Nano 30B A3B"),
            nvidiaModel("nvidia/llama-3.1-nemotron-ultra-253b-v1", "Llama 3.1 Nemotron Ultra 253B"),
            nvidiaModel("nvidia/llama-3.3-nemotron-super-49b-v1.5", "Llama 3.3 Nemotron Super 49B v1.5"),
            nvidiaModel("nvidia/llama-3.3-nemotron-super-49b-v1", "Llama 3.3 Nemotron Super 49B v1"),
            nvidiaModel("nvidia/nvidia-nemotron-nano-9b-v2", "Nemotron Nano 9B v2"),
            nvidiaModel("nvidia/nemotron-nano-12b-v2-vl", "Nemotron Nano 12B v2 VL"),
            nvidiaModel("nvidia/llama-3.1-nemotron-nano-8b-v1", "Llama 3.1 Nemotron Nano 8B"),
            nvidiaModel("nvidia/llama-3.1-nemotron-nano-vl-8b-v1", "Llama 3.1 Nemotron Nano VL 8B"),
            nvidiaModel("nvidia/cosmos-reason2-8b", "Cosmos Reason2 8B"),
            nvidiaModel("nvidia/llama-3.1-nemotron-70b-instruct", "Llama 3.1 Nemotron 70B"),
            nvidiaModel("nvidia/nemotron-mini-4b-instruct", "Nemotron Mini 4B"),
            nvidiaModel("mistralai/mistral-nemotron", "Mistral Nemotron"),
            nvidiaModel("mistralai/mistral-7b-instruct-v0.3", "Mistral 7B Instruct v0.3"),
            nvidiaModel("meta/llama-3.3-70b-instruct", "Llama 3.3 70B Instruct"),
            nvidiaModel("meta/llama-3.1-70b-instruct", "Llama 3.1 70B Instruct"),
            nvidiaModel("meta/llama-3.2-90b-vision-instruct", "Llama 3.2 90B Vision"),
            nvidiaModel("meta/llama-3.2-11b-vision-instruct", "Llama 3.2 11B Vision"),
            nvidiaModel("meta/llama-3.1-8b-instruct", "Llama 3.1 8B Instruct"),
            nvidiaModel("meta/llama-3.2-1b-instruct", "Llama 3.2 1B Instruct"),
            nvidiaModel("google/gemma-3-12b-it", "Gemma 3 12B"),
            nvidiaModel("google/gemma-3-4b-it", "Gemma 3 4B")
        ]
    }

    private static func nvidiaModel(_ slug: String, _ displayName: String) -> ModelEntry {
        model(slug, displayName, nvidiaProviderId, nvidiaProviderName)
    }

    private static func normalizeOpenRouter(_ source: [ModelEntry]?) -> [ModelEntry] {
        let models = source ?? [
            model("z-ai/glm-5.2", "GLM 5.2", "openrouter", "OpenRouter"),
            model("minimax/minimax-m3", "MiniMax M3", "openrouter", "OpenRouter"),
            model("qwen/qwen3.7-max", "Qwen 3.7 Max", "openrouter", "OpenRouter"),
            model("xiaomi/mimo-v2.5-pro", "MiMo V2.5 Pro", "openrouter", "OpenRouter"),
            model("deepseek/deepseek-v4-pro", "DeepSeek V4 Pro", "openrouter", "OpenRouter"),
            model("deepseek/deepseek-v4-flash", "DeepSeek V4 Flash", "openrouter", "OpenRouter"),
            model("xiaomi/mimo-v2.5", "MiMo V2.5", "openrouter", "OpenRouter")
        ]
        for entry in models {
            entry.providerId = "openrouter"
            entry.providerName = "OpenRouter"
            if entry.slug.lowercased().hasPrefix("openrouter/") {
                entry.slug = String(entry.slug.dropFirst("openrouter/".count))
            }
            if entry.displayName.trimmingCharacters(in: .whitespaces).isEmpty {
                entry.displayName = toDisplayName(entry.slug)
            }
        }
        return models
    }

    private static func createGroup(_ id: String, _ title: String, _ providerId: String,
                                    _ providerName: String, _ models: [ModelEntry]) -> ModelGroupEntry {
        let known: [String]
        if id.caseInsensitiveCompare("openrouter-free") == .orderedSame {
            known = models.map(\.slug)
        } else if id.caseInsensitiveCompare("openai") == .orderedSame {
            known = models.filter { m in gpt56Models.contains { $0.slug.caseInsensitiveCompare(m.slug) == .orderedSame } }.map(\.slug)
        } else if id.caseInsensitiveCompare("entropic") == .orderedSame {
            known = models.filter { m in anthropicManagedSlugs.contains { $0.caseInsensitiveCompare(m.slug) == .orderedSame } }.map(\.slug)
        } else {
            known = []
        }
        return ModelGroupEntry(id: id, title: title, providerId: providerId, providerName: providerName,
                               models: models, hiddenModelSlugs: [], knownSyncedModelSlugs: known, isExpanded: true)
    }

    private static func normalizeModel(_ model: ModelEntry, providerId: String, providerName: String) -> ModelEntry {
        model.providerId = providerId
        model.providerName = providerName
        if model.slug.lowercased().hasPrefix("\(providerId.lowercased())/") {
            model.slug = String(model.slug.dropFirst(providerId.count + 1))
        }
        if model.displayName.trimmingCharacters(in: .whitespaces).isEmpty {
            model.displayName = toDisplayName(model.slug)
        }
        return model
    }

    private static func normalizeSlugForGroup(_ rawSlug: String, providerId: String) -> String {
        var slug = rawSlug.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        // Bei NVIDIA ist "nvidia/" Bestandteil der Modell-ID, nicht ein doppelter Provider-Praefix -
        // dort wuerde das Kuerzen die ID zerstoeren.
        if providerId.caseInsensitiveCompare(ModelEntry.nvidiaProviderId) == .orderedSame { return slug }
        if slug.hasPrefix("\(providerId.lowercased())/") {
            slug = String(slug.dropFirst(providerId.count + 1))
        }
        return slug
    }

    private static func addUnique(_ values: inout [String], _ value: String) {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return }
        if !values.contains(where: { $0.caseInsensitiveCompare(trimmed) == .orderedSame }) {
            values.append(trimmed)
        }
    }

    private static func model(_ slug: String, _ displayName: String, _ providerId: String, _ providerName: String) -> ModelEntry {
        ModelEntry(slug: slug, displayName: displayName, providerId: providerId, providerName: providerName)
    }

    static func toDisplayName(_ slug: String) -> String {
        let name = slug.split(separator: "/").last.map(String.init) ?? slug
        return name.split(separator: "-")
            .map { $0.prefix(1).uppercased() + $0.dropFirst() }
            .joined(separator: " ")
    }
}
