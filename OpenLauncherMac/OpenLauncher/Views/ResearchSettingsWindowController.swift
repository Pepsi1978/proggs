import AppKit

@MainActor
final class ResearchSettingsWindowController: NSWindowController, NSWindowDelegate {
    private let viewModel: MainViewModel
    private let service = CodexResearchService.shared
    private var settings = ResearchSettingsService.load()
    private var models: [CodexResearchModel] = []
    private var operation: Task<Void, Never>?
    private var reportTask: Task<Void, Never>?
    private let connection = NSTextField(wrappingLabelWithString: "Nicht verbunden")
    private let deviceCode = NSTextField(labelWithString: "")
    private let status = NSTextField(wrappingLabelWithString: "")
    private let target = NSTextField(wrappingLabelWithString: "")
    private let mode = NSPopUpButton()
    private let model = NSPopUpButton()
    private let effort = NSPopUpButton()
    private let period = NSPopUpButton()
    private let report = NSTextView()
    private var controls: [NSControl] = []
    private var cancelButton: NSButton!
    private let hours = [1, 6, 12, 24, 168]

    init(viewModel: MainViewModel) {
        self.viewModel = viewModel
        let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 720, height: 730),
                              styleMask: [.titled, .closable, .resizable], backing: .buffered, defer: false)
        window.title = "Einstellungen · Modell-Recherche"
        window.minSize = NSSize(width: 640, height: 640)
        window.isReleasedWhenClosed = false
        super.init(window: window)
        window.delegate = self
        window.center()
        buildInterface()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    private func button(_ title: String, _ action: Selector, managed: Bool = true) -> NSButton {
        let button = NSButton(title: title, target: self, action: action)
        if managed { controls.append(button) }
        return button
    }

    private func row(_ views: [NSView]) -> NSStackView {
        let stack = NSStackView(views: views)
        stack.orientation = .horizontal
        stack.spacing = 10
        return stack
    }

    private func buildInterface() {
        guard let content = window?.contentView else { return }
        let title = NSTextField(labelWithString: "Modell-Recherche")
        title.font = .systemFont(ofSize: 23, weight: .semibold)
        deviceCode.isSelectable = true
        deviceCode.font = .monospacedSystemFont(ofSize: 16, weight: .semibold)
        let login = button("Mit OpenAI anmelden", #selector(Self.login))
        let reload = button("Modelle laden", #selector(Self.reload))
        cancelButton = button("Abbrechen", #selector(cancel), managed: false)
        cancelButton.isEnabled = false
        let link = button("Anmeldeseite öffnen", #selector(openDevicePage), managed: false)
        mode.addItems(withTitles: ResearchMode.allCases.map { $0.title })
        mode.selectItem(at: ResearchMode.allCases.firstIndex(of: settings.mode) ?? 0)
        period.addItems(withTitles: hours.map { "Alle \($0) Stunden" })
        period.selectItem(at: hours.firstIndex(of: settings.periodHours) ?? 3)
        model.target = self
        model.action = #selector(modelChanged)
        controls += [mode, model, effort, period]
        for (control, label) in [(mode, "Recherche-Modus"), (model, "Recherche-Modell"), (effort, "Recherche-Effort"), (period, "Periodischer Abstand")] {
            control.setAccessibilityLabel(label)
        }
        report.isEditable = false
        report.isSelectable = true
        report.font = .monospacedSystemFont(ofSize: 12, weight: .regular)
        report.autoresizingMask = [.width]
        report.textContainer?.widthTracksTextView = true
        let scroll = NSScrollView()
        scroll.hasVerticalScroller = true
        scroll.borderType = .bezelBorder
        scroll.documentView = report
        scroll.heightAnchor.constraint(greaterThanOrEqualToConstant: 160).isActive = true
        let note = NSTextField(wrappingLabelWithString:
            "Eigene Anmeldung in der macOS-Keychain. Bestehende CLI-Anmeldungen bleiben unverändert. " +
            "Nur Websuche, keine Shell- oder Dateitools. KI-Aufrufe können Kontingent verbrauchen. " +
            "Katalogabfragen bleiben in jedem Modus aktiv. Bestehende Effort-Stufen werden nie automatisch gelöscht.")
        note.textColor = .secondaryLabelColor
        let stack = NSStackView(views: [title, connection, row([login, reload, cancelButton]), deviceCode, link, note,
            row([NSTextField(labelWithString: "Modus"), mode]),
            row([NSTextField(labelWithString: "Recherche-Modell"), model]),
            row([NSTextField(labelWithString: "Effort"), effort, NSTextField(labelWithString: "Abstand"), period]),
            target, button("Ausgewähltes Launcher-Modell recherchieren", #selector(research)),
            scroll, status, row([button("Speichern", #selector(save)), button("Schließen", #selector(closeSettings), managed: false)])])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false
        content.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 20),
            stack.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -20),
            stack.topAnchor.constraint(equalTo: content.topAnchor, constant: 20),
            stack.bottomAnchor.constraint(equalTo: content.bottomAnchor, constant: -20),
            scroll.widthAnchor.constraint(equalTo: stack.widthAnchor),
            note.widthAnchor.constraint(equalTo: stack.widthAnchor),
            connection.widthAnchor.constraint(equalTo: stack.widthAnchor),
            status.widthAnchor.constraint(equalTo: stack.widthAnchor),
            target.widthAnchor.constraint(equalTo: stack.widthAnchor)
        ])
    }

    override func showWindow(_ sender: Any?) {
        super.showWindow(sender)
        window?.appearance = NSAppearance(named: ThemeManager.current == .dark ? .darkAqua : .aqua)
        showReports()
        if reportTask == nil {
            reportTask = Task { [weak self] in
                while !Task.isCancelled {
                    do { try await Task.sleep(nanoseconds: 2_000_000_000) } catch { return }
                    self?.showReports()
                }
            }
        }
        reload()
    }

    private func run(_ action: @escaping @MainActor () async throws -> Void) {
        guard operation == nil else { return }
        controls.forEach { $0.isEnabled = false }
        cancelButton.isEnabled = true
        operation = Task {
            defer {
                operation = nil
                controls.forEach { $0.isEnabled = true }
                cancelButton.isEnabled = false
                deviceCode.stringValue = ""
                showReports()
            }
            do { try await action() }
            catch {
                status.stringValue = Task.isCancelled ? "Vorgang abgebrochen." :
                    (error as? ResearchFailure)?.localizedDescription ?? "Vorgang fehlgeschlagen. Anmeldung oder Verbindung prüfen."
                connection.stringValue = "Verbindung nicht bestätigt; gespeicherte Anmeldung bleibt erhalten."
            }
        }
    }

    private func reloadModels() async throws {
        let connected = try service.isConnected()
        connection.stringValue = connected ? "Anmeldung gespeichert; Kontozugriff wird geprüft …" : "Nicht verbunden · keine KI-Aufrufe"
        let available = try await service.models()
        try Task.checkCancellation()
        models = available
        model.removeAllItems()
        model.addItems(withTitles: models.map { $0.id })
        if let index = models.firstIndex(where: { $0.id == settings.model }) { model.selectItem(at: index) }
        modelChanged()
        connection.stringValue = connected ? "Verbunden · \(models.count) verfügbare Modelle" : "Nicht verbunden · keine KI-Aufrufe"
    }

    @objc private func modelChanged() {
        effort.removeAllItems()
        guard models.indices.contains(model.indexOfSelectedItem) else { return }
        effort.addItems(withTitles: models[model.indexOfSelectedItem].efforts)
        if effort.itemTitles.contains(settings.effort) { effort.selectItem(withTitle: settings.effort) }
    }

    private func saveSettings() throws {
        var next = settings
        next.mode = ResearchMode.allCases[max(0, mode.indexOfSelectedItem)]
        next.periodHours = hours[max(0, period.indexOfSelectedItem)]
        next.model = model.titleOfSelectedItem ?? settings.model
        next.effort = effort.titleOfSelectedItem ?? settings.effort
        try ResearchSettingsService.save(next)
        settings = next
        status.stringValue = "Einstellungen gespeichert."
    }

    @objc private func login() {
        run { [self] in
            connection.stringValue = "Geräteanmeldung wird gestartet …"
            try await service.login { [self] code in
                deviceCode.stringValue = "Gerätecode: \(code)"
                connection.stringValue = "Anmeldeseite öffnen und den Gerätecode bestätigen."
            }
            try await reloadModels()
        }
    }

    @objc private func reload() { run { [self] in try await reloadModels() } }
    @objc private func save() { run { [self] in try saveSettings() } }
    @objc private func cancel() { operation?.cancel() }
    @objc private func closeSettings() { close() }
    @objc private func openDevicePage() {
        NSWorkspace.shared.open(URL(string: "https://auth.openai.com/codex/device")!)
    }

    @objc private func research() {
        guard let target = viewModel.thinkingTarget else { status.stringValue = "Zuerst ein Launcher-Modell auswählen."; return }
        run { [self] in
            try saveSettings()
            status.stringValue = "Web-Recherche läuft …"
            do {
                let result = try await service.research(target, manual: true)
                try Task.checkCancellation()
                if let result {
                    viewModel.applyResearchResult(target, snapshot: result)
                    status.stringValue = "Belegte Recherche übernommen; alte Stufen bleiben erhalten."
                } else {
                    EffortStore.record(target, snapshot: nil, status: "Keine KI-Anfrage: Modus ausgeschaltet.")
                    status.stringValue = "Keine Übernahme. Siehe Bericht."
                }
            } catch {
                if !Task.isCancelled {
                    EffortStore.record(target, snapshot: nil, status: (error as? ResearchFailure)?.localizedDescription ?? "Recherche fehlgeschlagen; bisheriger Stand bleibt.")
                }
                throw error
            }
        }
    }

    private func showReports() {
        target.stringValue = "Launcher-Modell: " + (viewModel.thinkingTarget?.label ?? "keines")
        let text = EffortStore.reports.values.sorted { $0.attemptedAt > $1.attemptedAt }.map { entry in
            let attempted = DateFormatter.localizedString(from: entry.attemptedAt, dateStyle: .short, timeStyle: .short)
            let success = entry.snapshot.map { DateFormatter.localizedString(from: $0.checkedAt, dateStyle: .short, timeStyle: .short) } ?? "noch keiner"
            return "\(entry.target.label)\nVersuch: \(attempted)\n\(entry.status)\nLetzter Erfolg: \(success)\nQuelle: \(entry.snapshot?.source ?? "noch keine bestätigte Quelle")"
        }.joined(separator: "\n\n")
        let value = text.isEmpty ? "Noch keine Quellenberichte." : text
        if report.string != value { report.string = value }
    }

    func windowWillClose(_ notification: Notification) {
        operation?.cancel()
        reportTask?.cancel()
        reportTask = nil
    }
}
