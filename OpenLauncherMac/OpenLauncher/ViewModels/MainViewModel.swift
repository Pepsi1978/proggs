import AppKit

/// Rueckmeldungen an die Oberflaeche. In WPF erledigt das die Datenbindung
/// (INotifyPropertyChanged/ObservableCollection); AppKit hat das nicht, deshalb sagt das ViewModel
/// hier ausdruecklich Bescheid, welcher Bereich sich geaendert hat.
@MainActor
protocol MainViewModelDelegate: AnyObject {
    func modelGroupsChanged()
    func selectedModelChanged()
    func providersChanged()
    func thinkingOptionsChanged()
    func profileStateChanged()
    func statusChanged()
    func hiddenModelsChanged()
    func workDirChanged()

    // --- Dialoge (in WPF direkt im ViewModel, hier bewusst an das Fenster delegiert) ---
    func askModelDialog(groups: [ModelGroupEntry], defaultGroup: ModelGroupEntry, title: String,
                        confirmText: String, initialSlug: String,
                        initialDisplayName: String) -> (group: ModelGroupEntry, slug: String, displayName: String)?
    func confirmRemoveModel(displayName: String, slug: String) -> Bool
    func showHiddenModelsWindow()
    func showLastErrorWindow()
    func editProfileDocument(documents: InstructionProfileDocuments, isClaudeCode: Bool,
                             profileName: String) -> String?
    func editWorkModePrompt(workModeName: String, sourcePath: String, promptText: String) -> String?
    func browseWorkDirectory(current: String) -> String?
}

/// Gesamter Zustand des Launchers. 1:1-Port von ViewModels/MainViewModel.cs.
@MainActor
final class MainViewModel {
    weak var delegate: MainViewModelDelegate?

    private let registry: ModelRegistry
    private let router = OpenRouterService()
    private let lmStudio = LmStudioService()
    private let launcher = OpenLauncherService()
    private let updater = OpenCodeUpdateService()
    private let profiles = InstructionProfileService()
    private let modelDefaults = ModelDefaultsService()

    /// Laufende Ladevorgaenge. Ein neuer Modellwechsel loest den vorherigen ab; die abgeloeste
    /// Aufgabe darf ihre spaete Antwort nicht mehr in die Oberflaeche schreiben.
    private var loadProvidersTask: Task<Void, Never>?
    private var loadThinkingTask: Task<Void, Never>?

    /// Standard des gerade gewaehlten Modells, solange er noch greift. Er ueberstimmt die
    /// profilabhaengige Effort-Vorauswahl - aber nur bis der Nutzer das Profil selbst umstellt;
    /// danach gilt wieder die normale Regel (Strikt -> X High, sonst High).
    private var pendingModelDefault: ModelDefaultEntry?
    /// True, solange selectedModel den gespeicherten Standard setzt: die dabei ausgeloesten
    /// Profil-Wechsel sind programmatisch und duerfen pendingModelDefault nicht verwerfen.
    private var applyingModelDefault = false

    // ===================== Sammlungen =====================

    private(set) var modelGroups: [ModelGroupEntry] = []
    private(set) var providers: [ProviderEntry] = []
    private(set) var thinkingOptions: [ThinkingOptionEntry] = []
    private(set) var profileList: [InstructionProfileEntry] = []
    private(set) var workModes: [WorkModeEntry] = []
    private(set) var hiddenModels: [ModelEntry] = []

    // ===================== Zustand =====================

    var selectedModel: ModelEntry? {
        didSet {
            guard selectedModel !== oldValue else { return }
            onSelectedModelChanged(selectedModel)
            delegate?.selectedModelChanged()
        }
    }

    var selectedProvider: ProviderEntry? {
        didSet { if selectedProvider !== oldValue { delegate?.providersChanged() } }
    }

    var selectedThinkingOption: ThinkingOptionEntry? {
        didSet {
            guard selectedThinkingOption !== oldValue else { return }
            onSelectedThinkingOptionChanged(selectedThinkingOption)
            delegate?.thinkingOptionsChanged()
        }
    }

    private(set) var hasThinkingOptions = false
    private(set) var hasNoThinkingOptions = true
    private(set) var thinkingEmptyText = "Modell wählen."
    private(set) var thinkingTitle = "THINKING"
    private(set) var thinkingSubtitle = "Reasoning-Level"

    var workDir: String = "" {
        didSet { if workDir != oldValue { delegate?.workDirChanged() } }
    }

    private(set) var isLoading = false
    private(set) var statusText = "Bereit." {
        didSet { delegate?.statusChanged() }
    }
    private(set) var version = ""
    private(set) var lastErrorDetails = "Noch kein Fehler protokolliert."
    private(set) var lastErrorPath = ""
    private(set) var hasLastError = false

    var selectedProfile: InstructionProfileEntry? {
        didSet {
            guard selectedProfile !== oldValue else { return }
            onSelectedProfileChanged(selectedProfile)
            delegate?.profileStateChanged()
        }
    }

    var selectedWorkMode: WorkModeEntry? {
        didSet {
            guard selectedWorkMode !== oldValue else { return }
            refreshModelDefaultState()
            delegate?.profileStateChanged()
        }
    }

    private(set) var profileContextText = "OpenCode · AGENTS.md"
    private(set) var canEditSelectedProfile = true
    private(set) var hasHiddenModels = false
    private(set) var modelDefaultButtonText = "Standard speichern"
    private(set) var modelDefaultSummary = ""
    private(set) var hasModelDefault = false
    /// Gegenstueck zu hasModelDefault: die Kontextzeile ("Claude Code · Minimal + …") und die
    /// Standard-Zeile teilen sich denselben Platz. Ohne dieses Gegenstueck stuenden beide
    /// untereinander und der Profil-Bereich saesse mit gespeichertem Standard eine Zeile tiefer.
    private(set) var hasNoModelDefault = true
    private(set) var canSaveModelDefault = false

    // ===================== Aufbau =====================

    init() {
        registry = ModelRegistry.load()
        modelGroups = registry.groups
        hiddenModels = modelGroups.flatMap(\.models).filter(\.isHidden)
        hasHiddenModels = !hiddenModels.isEmpty

        profileList = [
            InstructionProfileEntry(id: "minimal", displayName: "Minimal",
                                    descriptionText: "Frisches, leeres Profil zum Ausbauen", isEnabled: true),
            InstructionProfileEntry(id: "standard", displayName: "Standard",
                                    descriptionText: "Bewährte globale und Projektregeln", isEnabled: true),
            InstructionProfileEntry(id: "strict", displayName: "Strikt",
                                    descriptionText: "Mehr Kontrolle und Absicherung", isEnabled: true)
        ]
        workModes = [
            WorkModeEntry(id: "frei", displayName: "Freimodus", descriptionText: "Kein zusätzlicher Modus-Prompt"),
            WorkModeEntry(id: "schnell", displayName: "Schnellmodus", descriptionText: "Kleinster korrekter Eingriff"),
            WorkModeEntry(id: "normal", displayName: "Normalmodus", descriptionText: "Passend zu Risiko und Umfang"),
            WorkModeEntry(id: "gruendlich", displayName: "Gründlichkeitsmodus", descriptionText: "Randfälle und Härtung mitprüfen")
        ]

        selectedProfile = profileList.first { $0.id == "minimal" }
        // Freimodus ist die Vorauswahl fuer JEDES Profil und JEDES Modell: der Modellwechsel setzt
        // das Minimalprofil, der Profilwechsel setzt wieder diesen Modus -> ohne aktives Umschalten
        // laeuft jede Session ohne zusaetzlichen Modus-Prompt.
        selectedWorkMode = workModes.first { $0.id == "frei" }
        workDir = (Paths.home as NSString).appendingPathComponent("proggs")

        version = Self.buildVersionText()

        // WICHTIG: Swift ruft `didSet` NICHT auf, wenn eine Eigenschaft im Initializer gesetzt wird.
        // Die Startauswahl loest hier also weder Provider- noch Thinking-Ladevorgang aus - das holt
        // activateInitialSelection() nach, sobald das Fenster als Delegate haengt. (In WPF gibt es
        // das Problem nicht: dort feuert OnSelectedModelChanged auch aus dem Konstruktor heraus.)
        selectedModel = modelGroups.flatMap(\.models).first { !$0.isHidden }

        Task { await refreshOpenRouterFreeModels() }
        Task { await refreshLmStudioModels() }
        Task { await checkOpenCodeUpdate() }
    }

    /// Holt die im Initializer unterdrueckte Zustandsberechnung der Startauswahl nach.
    /// Muss vom Fenster GENAU EINMAL aufgerufen werden, nachdem `delegate` gesetzt ist.
    func activateInitialSelection() {
        onSelectedModelChanged(selectedModel)
        delegate?.selectedModelChanged()
    }

    /// Version UND Zeitstempel kommen aus dem App-Bundle: die Uhrzeit setzt build.sh beim Compile
    /// (Schluessel BuildTimestamp in der Info.plist), damit hier nie ein von Hand getippter - und
    /// damit moeglicherweise falscher - Zeitpunkt steht. Gegenstueck zum MSBuild-Target
    /// "SetBuildTimestamp" im Windows-Projekt.
    private static func buildVersionText() -> String {
        let info = Bundle.main.infoDictionary
        let number = (info?["CFBundleShortVersionString"] as? String) ?? "?"
        let timestamp = (info?["BuildTimestamp"] as? String) ?? ""
        return timestamp.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ? "Version \(number)"
            : "Version \(number) (\(timestamp) Uhr)"
    }

    // ===================== Modellwechsel =====================

    private func onSelectedModelChanged(_ value: ModelEntry?) {
        // Gespeicherter Standard des Modells zuerst holen: er bestimmt Profil, Modus und (spaeter,
        // sobald die Stufen geladen sind) den Effort. Ohne Standard bleibt es beim Minimalprofil.
        pendingModelDefault = value == nil ? nil : modelDefaults.find(value!.modelString)
        applyingModelDefault = true
        selectedProfile = profileList.first { $0.id == "minimal" }
        if let stored = pendingModelDefault {
            selectedProfile = profileList.first { $0.id == stored.profileId && $0.isEnabled } ?? selectedProfile
            selectedWorkMode = workModes.first { $0.id == stored.workModeId } ?? selectedWorkMode
        }
        applyingModelDefault = false

        selectedProvider = nil
        providers.removeAll()
        selectedThinkingOption = nil
        thinkingOptions.removeAll()

        let isClaude = Self.isClaudeCodeModel(value)
        thinkingTitle = isClaude ? "EFFORT" : "THINKING"
        thinkingSubtitle = isClaude ? "Claude-Code-Level" : "Reasoning-Level"
        profileContextText = isClaude ? "Claude Code · Minimal + Standard + Strikt" : "OpenCode · Profil-Snapshots"
        updateProfileAvailability()
        refreshModelDefaultState()
        delegate?.providersChanged()
        delegate?.profileStateChanged()

        guard let value else {
            loadProvidersTask?.cancel()
            loadThinkingTask?.cancel()
            updateThinkingState(emptyText: "Modell wählen.")
            delegate?.thinkingOptionsChanged()
            return
        }

        updateThinkingState(emptyText: "Lade Thinking …")
        delegate?.thinkingOptionsChanged()

        loadThinkingTask?.cancel()
        loadThinkingTask = Task { await loadThinkingOptions(model: value) }
        loadProvidersTask?.cancel()
        loadProvidersTask = Task { await loadProviders(model: value) }
    }

    private func onSelectedThinkingOptionChanged(_ value: ThinkingOptionEntry?) {
        refreshModelDefaultState()
        guard let model = selectedModel, let value else { return }
        let label = Self.isClaudeCodeModel(model) ? "Effort" : "Thinking"
        statusText = "\(label) für \(model.displayName): \(value.displayName)"
    }

    // ===================== Thinking-/Effort-Stufen =====================

    private func loadThinkingOptions(model: ModelEntry, forceRefresh: Bool = false) async {
        var levels = OpenCodeVariantCatalog.launcherLevels(for: model)
        if model.providerId.caseInsensitiveCompare("openrouter") == .orderedSame {
            let apiLevels = await router.thinkingLevels(slug: model.slug, forceRefresh: forceRefresh)
            if !apiLevels.isEmpty { levels = apiLevels }
        }
        if Task.isCancelled { return }

        thinkingOptions = levels.map(Self.toThinkingOption)
        selectProfileThinkingOption()
        let isClaude = Self.isClaudeCodeModel(model)
        let empty = isClaude ? "Kein Effort für dieses Modell erkannt." : "Kein Thinking für dieses Modell erkannt."
        let prompt = isClaude ? "Effort-Wert wählen." : "Thinking-Wert wählen."
        updateThinkingState(emptyText: levels.isEmpty ? empty : prompt)
        delegate?.thinkingOptionsChanged()
    }

    private func updateThinkingState(emptyText: String) {
        hasThinkingOptions = !thinkingOptions.isEmpty
        hasNoThinkingOptions = !hasThinkingOptions
        thinkingEmptyText = hasThinkingOptions ? "" : emptyText
    }

    static func toThinkingOption(_ value: String) -> ThinkingOptionEntry {
        let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let displayName: String
        switch normalized {
        case "xhigh": displayName = "X High"
        case "max": displayName = "Max"
        case "thinking": displayName = "Thinking"
        case "none": displayName = "None"
        case "minimal": displayName = "Minimal"
        case "low": displayName = "Low"
        case "medium": displayName = "Medium"
        case "high": displayName = "High"
        default: displayName = normalized
        }
        let description: String
        switch normalized {
        case "none": description = "aus"
        case "minimal": description = "sehr knapp"
        case "low": description = "leicht"
        case "medium": description = "empfohlen"
        case "high": description = "gründlich"
        case "xhigh": description = "maximal hoch"
        case "max": description = "Maximum"
        case "thinking": description = "aktiviert"
        default: description = "Thinking"
        }
        return ThinkingOptionEntry(value: normalized, displayName: displayName, descriptionText: description)
    }

    // ===================== Modell-Listen aktualisieren =====================

    private func refreshOpenRouterFreeModels() async {
        do {
            let freeModels = try await router.freeModels()
            if freeModels.isEmpty { return }
            registry.syncOpenRouterFreeModels(freeModels)
            modelGroups = registry.groups
            refreshHiddenModels()
            delegate?.modelGroupsChanged()
        } catch {
            Logger.shared.warn("MainViewModel", "refreshOpenRouterFreeModels",
                               "OpenRouterFree bleibt bei Fallback-Liste: \(error.localizedDescription)")
        }
    }

    /// Liest beim Start die lokal in LM Studio geladenen/verfuegbaren Modelle und fuellt damit den
    /// Reiter "LM Studio". Ist LM Studio nicht installiert oder der Server aus, bleibt die zuletzt
    /// bekannte Liste stehen - der Launcher startet trotzdem normal.
    private func refreshLmStudioModels() async {
        guard LmStudioService.isInstalled else { return }
        let localModels = await lmStudio.localModelsWithServer()
        if localModels.isEmpty { return }
        registry.syncLmStudioModels(localModels)
        modelGroups = registry.groups
        refreshHiddenModels()
        delegate?.modelGroupsChanged()
    }

    private func refreshHiddenModels() {
        hiddenModels = modelGroups.flatMap(\.models).filter(\.isHidden)
        hasHiddenModels = !hiddenModels.isEmpty
        delegate?.hiddenModelsChanged()
    }

    // ===================== Profile / Modi =====================

    private func onSelectedProfileChanged(_ value: InstructionProfileEntry?) {
        // Stellt der Nutzer das Profil selbst um, ist der gespeicherte Standard verlassen: ab hier
        // gilt wieder die profilabhaengige Effort-Vorauswahl.
        if !applyingModelDefault { pendingModelDefault = nil }
        updateProfileAvailability()
        refreshModelDefaultState()
        guard let value else { return }

        selectedWorkMode = workModes.first { $0.id == "frei" }
        selectProfileThinkingOption()
        statusText = "Profil \(value.displayName) ausgewählt."
    }

    private func selectProfileThinkingOption() {
        guard let profile = selectedProfile, !thinkingOptions.isEmpty else { return }

        // Ein von Hand gespeicherter Modell-Standard schlaegt die profilabhaengige Vorauswahl.
        // Er greift erst hier, weil die Effort-Stufen erst nach dem Modellwechsel geladen sind.
        if let storedValue = pendingModelDefault?.thinkingValue,
           !storedValue.trimmingCharacters(in: .whitespaces).isEmpty,
           let storedOption = thinkingOptions.first(where: { $0.value == storedValue }) {
            selectedThinkingOption = storedOption
            return
        }

        let preferredValue = profile.id == "strict" ? "xhigh" : "high"
        selectedThinkingOption = thinkingOptions.first { $0.value == preferredValue } ?? thinkingOptions.last
    }

    /// Haelt Schalterbeschriftung und Standard-Anzeige zum aktuell gewaehlten Modell aktuell.
    /// Entspricht die Auswahl genau dem gespeicherten Standard, wird der Schalter zum Entfernen -
    /// derselbe Knopf schaltet den Standard also an und wieder aus.
    private func refreshModelDefaultState() {
        let stored = selectedModel.flatMap { modelDefaults.find($0.modelString) }
        hasModelDefault = stored != nil
        hasNoModelDefault = stored == nil
        canSaveModelDefault = selectedModel != nil && selectedProfile != nil && selectedWorkMode != nil
        modelDefaultSummary = stored.map {
            "★ Standard: \(describeProfile($0.profileId)) · \(describeWorkMode($0.workModeId)) · \(Self.describeThinking($0.thinkingValue))"
        } ?? ""
        modelDefaultButtonText = matchesStoredDefault(stored) ? "★ Standard entfernen" : "☆ Standard speichern"
    }

    private func matchesStoredDefault(_ stored: ModelDefaultEntry?) -> Bool {
        guard let stored else { return false }
        return stored.profileId == selectedProfile?.id
            && stored.workModeId == selectedWorkMode?.id
            && stored.thinkingValue.caseInsensitiveCompare(selectedThinkingOption?.value ?? "") == .orderedSame
    }

    private func describeProfile(_ profileId: String) -> String {
        profileList.first { $0.id == profileId }?.displayName ?? profileId
    }

    private func describeWorkMode(_ workModeId: String) -> String {
        workModes.first { $0.id == workModeId }?.displayName ?? workModeId
    }

    private static func describeThinking(_ thinkingValue: String) -> String {
        thinkingValue.trimmingCharacters(in: .whitespaces).isEmpty
            ? "ohne Stufe"
            : toThinkingOption(thinkingValue).displayName
    }

    private func updateProfileAvailability() {
        canEditSelectedProfile = selectedProfile != nil
            && (!Self.isClaudeCodeModel(selectedModel) || Self.isClaudeCodeProfileSupported(selectedProfile!.id))
    }

    // Claude Code unterstuetzt alle drei Profile, jedes mit eigenem Repo-Config-Ordner
    // (CLAUDE_CONFIG_DIR): Minimal (regelfrei, Skills nur per Symlink), Standard und Strikt
    // (versionierte skills/rules/agents/commands im Repo, frei bearbeitbar, auf jedem Rechner gleich).
    private static func isClaudeCodeProfileSupported(_ profileId: String) -> Bool {
        ["standard", "minimal", "strict"].contains(profileId)
    }

    static func isClaudeCodeModel(_ model: ModelEntry?) -> Bool {
        model?.providerId.caseInsensitiveCompare("anthropic") == .orderedSame
    }

    // ===================== Modell-Standard =====================

    /// Speichert die aktuelle Auswahl (Profil, Modus, Effort) als Standard des gewaehlten Modells -
    /// oder entfernt ihn wieder, wenn genau dieser Standard schon gilt. Jedes Modell hat seinen
    /// eigenen Standard; er wird bei jedem Wechsel auf dieses Modell und damit auch beim App-Start
    /// vorausgewaehlt.
    func toggleModelDefault() {
        guard let model = selectedModel, let profile = selectedProfile, let workMode = selectedWorkMode else {
            statusText = "Bitte Modell, Profil und Modus wählen."
            return
        }

        let key = model.modelString
        let stored = modelDefaults.find(key)
        if matchesStoredDefault(stored) {
            modelDefaults.remove(key)
            pendingModelDefault = nil
            statusText = "Standard für \(model.displayName) entfernt."
            Logger.shared.info("MainViewModel", "toggleModelDefault", "Modell-Standard entfernt", ["key": key])
        } else {
            let entry = ModelDefaultEntry(profileId: profile.id,
                                          workModeId: workMode.id,
                                          thinkingValue: selectedThinkingOption?.value ?? "")
            modelDefaults.save(key, entry: entry)
            pendingModelDefault = entry
            statusText = "Standard für \(model.displayName) gespeichert: \(profile.displayName) · \(workMode.displayName) · \(Self.describeThinking(entry.thinkingValue))"
            Logger.shared.info("MainViewModel", "toggleModelDefault", "Modell-Standard gespeichert",
                               ["key": key, "profileId": entry.profileId,
                                "workModeId": entry.workModeId, "thinkingValue": entry.thinkingValue])
        }

        refreshModelDefaultState()
        delegate?.profileStateChanged()
    }

    // ===================== Update-Pruefung =====================

    private func checkOpenCodeUpdate() async {
        let result = await updater.check()
        if result.status == "installed" {
            statusText = result.message
        } else if result.status == "failed" {
            statusText = "OpenCode-Update zurückgestellt; letzter Fix bleibt aktiv."
        }
    }

    // ===================== Provider laden =====================

    private func loadProviders(model: ModelEntry) async {
        isLoading = true
        statusText = "Lade Provider für \(model.displayName) …"
        defer {
            // Nur der aktuelle (nicht abgeloeste) Ladevorgang darf die Ladeanzeige zuruecksetzen -
            // sonst loescht ein per Modellwechsel abgebrochener Vorgang sie, waehrend der
            // nachfolgende Ladevorgang noch laeuft (Ladeanzeige wuerde vorzeitig verschwinden).
            if !Task.isCancelled { isLoading = false }
        }

        do {
            if model.providerId.caseInsensitiveCompare("openrouter") != .orderedSame {
                let direct = ProviderEntry()
                direct.providerName = model.providerName
                direct.providerSlug = model.providerId
                direct.tag = model.providerId
                direct.status = 0
                await enrichDirectProvider(model: model, direct: direct)
                // Nach dem await pruefen, ob dieser Ladevorgang schon abgeloest wurde (schneller
                // Modellwechsel): sonst schreibt eine spaete Antwort die Provider des falschen
                // Modells und Start wuerde mit falschem Provider konfigurieren.
                if Task.isCancelled { return }
                providers = [direct]
                selectedProvider = direct
                statusText = "\(model.displayName) über \(model.providerName) bereit."
                delegate?.providersChanged()
                return
            }

            let (displayName, loaded) = try await router.providers(slug: model.slug, isCancelled: { Task.isCancelled })
            if Task.isCancelled { return }
            if let displayName, !displayName.trimmingCharacters(in: .whitespaces).isEmpty,
               displayName != model.displayName {
                // Anzeigename aus API uebernehmen, falls die Liste ihn noch als Slug zeigt.
                // Author-Praefix ("Z-AI: ") case-insensitiv entfernen: der Slug-Author ist klein-,
                // der API-Name grossgeschrieben - ein case-sensitives Replace liesse ihn stehen.
                let authorPrefix = "\(model.slug.split(separator: "/").first.map(String.init) ?? ""): "
                model.displayName = displayName.lowercased().hasPrefix(authorPrefix.lowercased())
                    ? String(displayName.dropFirst(authorPrefix.count))
                    : displayName
            }
            providers = loaded
            selectedProvider = providers.first
            statusText = loaded.isEmpty
                ? "Keine Provider für \(model.displayName) gefunden (Slug korrekt?)."
                : "\(loaded.count) Provider für \(model.displayName) geladen."
            delegate?.providersChanged()
            delegate?.selectedModelChanged()
        } catch {
            if Task.isCancelled { return }
            statusText = "Fehler: \(error.localizedDescription)"
            let details = Self.buildErrorDetails(action: "Provider laden", error: error, model: model,
                                                 provider: nil, workDir: nil, version: version)
            lastErrorPath = Logger.shared.writeErrorReport(area: "provider_load", details: details)
            lastErrorDetails = details + "\n\nGespeichert unter: \(lastErrorPath)"
            hasLastError = true
            Logger.shared.error("MainViewModel", "loadProviders", error.localizedDescription,
                                ["slug": model.slug, "lastErrorPath": lastErrorPath])
            delegate?.providersChanged()
        }
    }

    private func enrichDirectProvider(model: ModelEntry, direct: ProviderEntry) async {
        guard let metadata = OpenCodeModelMetadataCatalog.find(providerId: model.providerId, slug: model.slug) else { return }

        direct.contextLength = metadata.contextLength
        do {
            let (_, providers) = try await router.providers(slug: metadata.openRouterSlug, isCancelled: { Task.isCancelled })
            if providers.isEmpty { return }

            let fastest = providers.filter { $0.throughputLast30m != nil }
                .max { ($0.throughputLast30m ?? 0) < ($1.throughputLast30m ?? 0) }
            if let fastest {
                direct.throughputLast30m = fastest.throughputLast30m
                direct.contextLength = Swift.max(direct.contextLength, fastest.contextLength)
            } else {
                direct.contextLength = Swift.max(direct.contextLength, providers.map(\.contextLength).max() ?? 0)
            }
        } catch {
            // Das Enrichment ist nur Best-Effort: der Direkt-Provider wird trotzdem ohne
            // Zusatz-Metadaten angezeigt.
            Logger.shared.warn("MainViewModel", "enrichDirectProvider",
                               "OpenCode-Metadaten-Fallback für \(model.slug): \(error.localizedDescription)",
                               ["providerId": model.providerId, "openRouterSlug": metadata.openRouterSlug])
        }
    }

    // ===================== Befehle =====================

    func refresh() {
        guard let model = selectedModel else { return }
        updateThinkingState(emptyText: "Lade Thinking …")
        delegate?.thinkingOptionsChanged()
        loadProvidersTask?.cancel()
        loadProvidersTask = Task { await loadProviders(model: model) }
        loadThinkingTask?.cancel()
        loadThinkingTask = Task { await loadThinkingOptions(model: model, forceRefresh: true) }
    }

    func addModel() {
        let defaultGroup = findGroup(for: selectedModel)
            ?? modelGroups.first { $0.id == "openrouter" }
            ?? modelGroups.first
        guard let defaultGroup else { return }
        guard let result = delegate?.askModelDialog(groups: modelGroups, defaultGroup: defaultGroup,
                                                    title: "Neues Modell hinzufügen", confirmText: "Hinzufügen",
                                                    initialSlug: "", initialDisplayName: "") else { return }

        if registry.addModel(result.group, slug: result.slug, displayName: result.displayName) {
            let entry = result.group.models.last
            selectedModel = entry
            result.group.isExpanded = true
            statusText = "Modell '\(entry?.displayName ?? result.slug)' zu '\(result.group.title)' hinzugefügt."
        } else {
            statusText = "Modell '\(result.slug)' existiert in '\(result.group.title)' bereits."
        }
        modelGroups = registry.groups
        delegate?.modelGroupsChanged()
    }

    func editModel() {
        guard let model = selectedModel else {
            statusText = "Kein Modell ausgewählt."
            return
        }
        guard let group = findGroup(for: model) else { return }
        guard let result = delegate?.askModelDialog(groups: modelGroups, defaultGroup: group,
                                                    title: "Modell bearbeiten", confirmText: "Speichern",
                                                    initialSlug: model.slug,
                                                    initialDisplayName: model.displayName) else { return }

        guard registry.updateModel(group, model: model, targetGroup: result.group,
                                   slug: result.slug, displayName: result.displayName) else {
            statusText = "Modell '\(result.slug)' existiert in '\(result.group.title)' bereits."
            return
        }

        result.group.isExpanded = true
        modelGroups = registry.groups
        // Auswahl neu setzen: Provider/Thinking haengen am Slug und muessen nach der Bearbeitung
        // neu geladen werden.
        selectedModel = nil
        selectedModel = model
        statusText = group === result.group
            ? "Modell '\(model.displayName)' bearbeitet."
            : "Modell '\(model.displayName)' bearbeitet und nach '\(result.group.title)' verschoben."
        delegate?.modelGroupsChanged()
    }

    func removeModel() {
        guard let model = selectedModel, let group = findGroup(for: model) else { return }
        guard let index = group.models.firstIndex(where: { $0 === model }) else { return }
        guard delegate?.confirmRemoveModel(displayName: model.displayName, slug: model.slug) == true else { return }

        registry.removeAt(group, index: index)
        selectedModel = group.models.dropFirst(Swift.min(index, group.models.count)).first { !$0.isHidden }
            ?? group.models.prefix(Swift.min(index, group.models.count)).last { !$0.isHidden }
            ?? modelGroups.flatMap(\.models).first { !$0.isHidden }
        modelGroups = registry.groups
        refreshHiddenModels()
        delegate?.modelGroupsChanged()
    }

    func hideModel(_ model: ModelEntry) {
        if model.isHidden { return }
        guard let group = findGroup(for: model),
              let index = group.models.firstIndex(where: { $0 === model }) else { return }

        model.isHidden = true
        hiddenModels.append(model)
        hasHiddenModels = true
        registry.save()

        if selectedModel === model {
            selectedModel = group.models.dropFirst(index + 1).first { !$0.isHidden }
                ?? group.models.prefix(index).last { !$0.isHidden }
                ?? modelGroups.flatMap(\.models).first { !$0.isHidden }
        }

        statusText = "Modell '\(model.displayName)' ausgeblendet."
        delegate?.modelGroupsChanged()
        delegate?.hiddenModelsChanged()
    }

    func restoreModel(_ model: ModelEntry) {
        if !model.isHidden { return }
        guard findGroup(for: model) != nil else { return }

        model.isHidden = false
        hiddenModels.removeAll { $0 === model }
        hasHiddenModels = !hiddenModels.isEmpty
        registry.save()
        if selectedModel == nil { selectedModel = model }
        statusText = "Modell '\(model.displayName)' wieder eingeblendet."
        delegate?.modelGroupsChanged()
        delegate?.hiddenModelsChanged()
    }

    func showHiddenModels() {
        delegate?.showHiddenModelsWindow()
    }

    func browseWorkDir() {
        if let chosen = delegate?.browseWorkDirectory(current: workDir) { workDir = chosen }
    }

    func showLastError() {
        delegate?.showLastErrorWindow()
    }

    /// Oeffnet den Ordner, in dem Logs und Fehlerberichte liegen.
    func openLogFolder() {
        let folder = lastErrorPath.trimmingCharacters(in: .whitespaces).isEmpty
            ? (Logger.shared.logPath as NSString).deletingLastPathComponent
            : (lastErrorPath as NSString).deletingLastPathComponent
        guard Paths.directoryExists(folder) else { return }
        NSWorkspace.shared.open(URL(fileURLWithPath: folder))
    }

    // ===================== Start =====================

    func start() {
        guard let model = selectedModel, let provider = selectedProvider else {
            statusText = "Bitte Modell und Provider wählen."
            return
        }
        guard let profile = selectedProfile else {
            statusText = "Bitte ein Profil wählen."
            return
        }
        guard let workMode = selectedWorkMode else {
            statusText = "Bitte einen Modus wählen."
            return
        }
        let isClaudeCode = Self.isClaudeCodeModel(model)
        if isClaudeCode && !Self.isClaudeCodeProfileSupported(profile.id) {
            statusText = "Profil \(profile.displayName) ist für Claude Code noch nicht eingerichtet."
            return
        }
        guard Paths.directoryExists(workDir) else {
            statusText = "Arbeitsverzeichnis existiert nicht."
            return
        }

        do {
            let thinkingLevel = selectedThinkingOption?.commandValue
            let profileDocuments = try profiles.loadProfile(isClaudeCode: isClaudeCode,
                                                            profileId: profile.id, workDir: workDir)
            Logger.shared.info("MainViewModel", "start", "Vollständige Startauswahl geprüft", [
                "model": model.modelString,
                "provider": provider.providerName,
                "providerSlug": provider.providerSlug,
                "thinkingLevel": thinkingLevel ?? "",
                "profile": profile.id,
                "workMode": workMode.id,
                "profileGlobal": profileDocuments.globalPath,
                "workDir": workDir
            ])

            if isClaudeCode {
                // Jedes Profil hat seinen eigenen Repo-Config-Ordner (CLAUDE_CONFIG_DIR):
                // Standard/Strikt mit versionierten skills/rules/agents/commands, Minimal regelfrei
                // (Skills per Symlink). Der Modus-Prompt haengt hinter dem Profil in der aktiven
                // CLAUDE.md -> er gilt fuer die ganze Session, genau wie bei OpenCode.
                let claudeConfigDir = try profiles.ensureClaudeConfigDir(profileId: profile.id, workModeId: workMode.id)
                try launcher.launchClaudeCode(modelId: model.slug, workDir: workDir,
                                              effortLevel: thinkingLevel, claudeConfigDir: claudeConfigDir)
                statusText = (thinkingLevel ?? "").isEmpty
                    ? "Claude Code gestartet: \(model.displayName) · Profil \(profile.displayName) · Modus \(workMode.displayName)"
                    : "Claude Code gestartet: \(model.displayName) · Effort \(selectedThinkingOption?.displayName ?? "") · Profil \(profile.displayName) · Modus \(workMode.displayName)"
                return
            }

            // Projekt-AGENTS.md passend zum Profil setzen (Minimal -> nur minimal.md), BEVOR die
            // Session vorbereitet und OpenCode gestartet wird.
            try profiles.activateProjectAgents(profileId: profile.id, workDir: workDir)
            let profileSession = try profiles.prepareOpenCodeSession(
                profileId: profile.id,
                workDir: workDir,
                isLmStudio: model.providerId.caseInsensitiveCompare(LmStudioService.providerId) == .orderedSame
            )
            let modelString = try launcher.configureProvider(model: model, chosen: provider,
                                                             allProviders: providers, thinkingLevel: thinkingLevel)
            try launcher.launch(modelString: modelString, workDir: workDir, thinkingLevel: thinkingLevel,
                                profileConfigPath: profileSession.configPath, workMode: workMode.id)
            Logger.shared.info("MainViewModel", "start", "OpenCode-Profilsnapshot erstellt", [
                "profileId": profileSession.profileId,
                "sourceGlobalPath": profileSession.sourceGlobalPath,
                "globalSnapshotPath": profileSession.globalSnapshotPath,
                "configPath": profileSession.configPath
            ])
            statusText = (thinkingLevel ?? "").isEmpty
                ? "OpenCode gestartet: \(model.displayName) via \(provider.providerName) · Profil \(profile.displayName) · Modus \(workMode.displayName)"
                : "OpenCode gestartet: \(model.displayName) via \(provider.providerName) · Thinking \(selectedThinkingOption?.displayName ?? "") · Profil \(profile.displayName) · Modus \(workMode.displayName)"
        } catch {
            let details = Self.buildErrorDetails(action: "OpenCode starten", error: error, model: model,
                                                 provider: provider, workDir: workDir, version: version)
            lastErrorPath = Logger.shared.writeErrorReport(area: "start", details: details)
            lastErrorDetails = details + "\n\nGespeichert unter: \(lastErrorPath)"
            hasLastError = true
            statusText = "Start fehlgeschlagen. Details gespeichert: \((lastErrorPath as NSString).lastPathComponent)"
            Logger.shared.error("MainViewModel", "start", error.localizedDescription, [
                "model": model.modelString,
                "provider": provider.providerName,
                "workDir": workDir,
                "lastErrorPath": lastErrorPath
            ])
        }
    }

    // ===================== Profil-/Modus-Editor =====================

    func editProfile() {
        guard let profile = selectedProfile, profile.isEnabled, let model = selectedModel else { return }

        let isClaudeCode = Self.isClaudeCodeModel(model)
        if isClaudeCode && !Self.isClaudeCodeProfileSupported(profile.id) { return }
        do {
            let documents = try profiles.loadProfile(isClaudeCode: isClaudeCode,
                                                     profileId: profile.id, workDir: workDir)
            // Jedes Profil ist genau EINE Datei -> Editor zeigt Dateiname + Pfad und ein Textfeld.
            guard let text = delegate?.editProfileDocument(documents: documents, isClaudeCode: isClaudeCode,
                                                           profileName: profile.displayName) else { return }
            try profiles.saveProfile(isClaudeCode: isClaudeCode, profileId: profile.id,
                                     workDir: workDir, globalText: text, projectText: "")
            statusText = "Profil \(profile.displayName) für \(isClaudeCode ? "Claude Code" : "OpenCode") gespeichert."
            Logger.shared.info("MainViewModel", "editProfile", "Profil gespeichert", [
                "cli": isClaudeCode ? "claude" : "opencode",
                "profile": profile.id,
                "globalPath": documents.globalPath
            ])
        } catch {
            statusText = "Profil konnte nicht gespeichert werden: \(error.localizedDescription)"
            Logger.shared.error("MainViewModel", "editProfile", error.localizedDescription,
                                ["workDir": workDir, "model": model.modelString])
        }
    }

    /// Bearbeitet den Prompt des gewaehlten Arbeitsmodus. Der gespeicherte Text ist die einzige
    /// Quelle: OpenCode liest dieselbe Datei bei jedem Modellaufruf (auch nach dem Umschalten in der
    /// TUI), Claude Code bekommt sie beim Start hinter das Profil geschrieben.
    func editWorkMode() {
        guard let workMode = selectedWorkMode else { return }
        do {
            let path = try InstructionProfileService.resolveWorkModeSourcePath(workMode.id)
            let current = try profiles.loadWorkMode(workMode.id)
            guard let text = delegate?.editWorkModePrompt(workModeName: workMode.displayName,
                                                          sourcePath: path, promptText: current) else { return }
            try profiles.saveWorkMode(workMode.id, text: text)
            statusText = "Modus \(workMode.displayName) gespeichert."
            Logger.shared.info("MainViewModel", "editWorkMode", "Modus-Prompt gespeichert",
                               ["mode": workMode.id, "path": path])
        } catch {
            statusText = "Modus konnte nicht gespeichert werden: \(error.localizedDescription)"
            Logger.shared.error("MainViewModel", "editWorkMode", error.localizedDescription, ["mode": workMode.id])
        }
    }

    // ===================== Drag & Drop =====================

    func moveModel(_ group: ModelGroupEntry, from: Int, to: Int) {
        guard from >= 0, from < group.models.count else { return }
        let item = group.models[from]
        registry.moveModel(group, from: from, to: to)
        selectedModel = item
        delegate?.modelGroupsChanged()
    }

    func moveModel(_ sourceGroup: ModelGroupEntry, from: Int, to targetGroup: ModelGroupEntry, index: Int) {
        guard from >= 0, from < sourceGroup.models.count else { return }
        let item = sourceGroup.models[from]
        guard registry.moveModel(sourceGroup, from: from, to: targetGroup, index: index) else {
            statusText = "Modell '\(item.displayName)' existiert in '\(targetGroup.title)' bereits."
            return
        }
        targetGroup.isExpanded = true
        selectedModel = item
        statusText = sourceGroup === targetGroup
            ? "Modell in '\(targetGroup.title)' verschoben."
            : "Modell nach '\(targetGroup.title)' verschoben."
        delegate?.modelGroupsChanged()
    }

    func moveGroup(from: Int, to: Int) {
        guard from >= 0, from < modelGroups.count, to >= 0, to < modelGroups.count, from != to else { return }
        registry.moveGroup(from: from, to: to)
        modelGroups = registry.groups
        delegate?.modelGroupsChanged()
    }

    func toggleGroup(_ group: ModelGroupEntry) {
        group.isExpanded.toggle()
        registry.save()
        delegate?.modelGroupsChanged()
    }

    func findGroup(for model: ModelEntry?) -> ModelGroupEntry? {
        guard let model else { return nil }
        return modelGroups.first { group in group.models.contains { $0 === model } }
    }

    // ===================== Fehlerbericht =====================

    private static func buildErrorDetails(action: String, error: Error, model: ModelEntry?,
                                          provider: ProviderEntry?, workDir: String?, version: String) -> String {
        let stamp = DateFormatter()
        stamp.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        var lines: [String] = []
        lines.append("Aktion: \(action)")
        lines.append("Zeit: \(stamp.string(from: Date()))")
        lines.append("App-Version: \(version)")
        lines.append("Arbeitsverzeichnis: \(workDir ?? "(nicht gesetzt)")")
        lines.append("Modell: \(model?.displayName ?? "(nicht gesetzt)") [\(model?.slug ?? "-")]")
        lines.append("Provider: \(provider?.providerName ?? "(nicht gesetzt)") [\(provider?.providerSlug ?? "-") / \(provider?.tag ?? "-")]")
        lines.append("Logdatei: \(Logger.shared.logPath)")
        lines.append("")
        lines.append("Fehler: \(type(of: error))")
        lines.append("Message: \(error.localizedDescription)")
        let nsError = error as NSError
        lines.append("Domain: \(nsError.domain)  Code: \(nsError.code)")
        if !nsError.userInfo.isEmpty {
            lines.append("UserInfo:")
            for (key, value) in nsError.userInfo {
                lines.append("  \(key): \(value)")
            }
        }
        if let underlying = nsError.userInfo[NSUnderlyingErrorKey] as? NSError {
            lines.append("")
            lines.append("Innerer Fehler: \(underlying.domain) / \(underlying.code)")
            lines.append("Message: \(underlying.localizedDescription)")
        }
        return lines.joined(separator: "\n")
    }
}
