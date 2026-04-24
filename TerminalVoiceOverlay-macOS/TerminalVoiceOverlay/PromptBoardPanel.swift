import AppKit

/// Side panel that mirrors the Windows VTO PromptBoardPanel.xaml:
/// - horizontal category tabs at the top
/// - vertical scrollable prompt list
/// - "+" for category, backup/settings icons in the header
/// - per-row edit and delete icons
/// - "+ Neuer Prompt" at the bottom
/// Click on a prompt -> dictation-less insert via callback.
final class PromptBoardPanel: NSPanel {

    var onInsertText: ((String) -> Void)?

    private let categoryStack = NSStackView()
    private let promptStack = NSStackView()
    private let addPromptButton = NSButton(title: "+ Neuer Prompt", target: nil, action: nil)
    private let titleLabel = NSTextField(labelWithString: "PromptBoard")

    private var categories: [PBCategory] = []
    private var activeCategoryId: UUID?

    init() {
        let contentRect = NSRect(x: 0, y: 0, width: 380, height: 528)
        super.init(
            contentRect: contentRect,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered, defer: false)
        isFloatingPanel = true
        level = .floating
        hidesOnDeactivate = false
        isMovableByWindowBackground = false
        backgroundColor = .clear
        isOpaque = false
        hasShadow = true
        titleVisibility = .hidden
        titlebarAppearsTransparent = true
        becomesKeyOnlyIfNeeded = true

        buildUI()
    }

    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }

    private func buildUI() {
        let root = NSView(frame: contentView!.bounds)
        root.autoresizingMask = [.width, .height]
        root.wantsLayer = true
        root.layer?.backgroundColor = NSColor(calibratedWhite: 0.12, alpha: 0.9).cgColor
        root.layer?.cornerRadius = 16
        root.layer?.borderColor = NSColor(calibratedWhite: 0.23, alpha: 1).cgColor
        root.layer?.borderWidth = 1
        contentView?.addSubview(root)

        titleLabel.textColor = NSColor(calibratedWhite: 0.8, alpha: 1)
        titleLabel.font = NSFont.boldSystemFont(ofSize: 13)

        let addCatBtn = makeIconButton(symbol: "+", tooltip: "Neue Kategorie", action: #selector(onAddCategory))
        let backupBtn = makeIconButton(symbol: "⇪", tooltip: "Backup / Wiederherstellen", action: #selector(onBackup))
        let settingsBtn = makeIconButton(symbol: "⚙", tooltip: "Einstellungen", action: #selector(onSettings))

        let header = NSStackView(views: [titleLabel, NSView(), addCatBtn, backupBtn, settingsBtn])
        header.orientation = .horizontal
        header.alignment = .centerY
        header.spacing = 6
        header.distribution = .fill
        (header.arrangedSubviews[1]).setContentHuggingPriority(.defaultLow, for: .horizontal)

        categoryStack.orientation = .horizontal
        categoryStack.alignment = .centerY
        categoryStack.spacing = 4

        let categoryScroll = NSScrollView()
        categoryScroll.hasHorizontalScroller = true
        categoryScroll.hasVerticalScroller = false
        categoryScroll.drawsBackground = false
        categoryScroll.borderType = .noBorder
        categoryScroll.documentView = categoryStack
        categoryScroll.translatesAutoresizingMaskIntoConstraints = false
        categoryStack.translatesAutoresizingMaskIntoConstraints = false

        promptStack.orientation = .vertical
        promptStack.alignment = .leading
        promptStack.spacing = 6

        let promptScroll = NSScrollView()
        promptScroll.hasVerticalScroller = true
        promptScroll.drawsBackground = false
        promptScroll.borderType = .noBorder
        promptScroll.documentView = promptStack
        promptScroll.translatesAutoresizingMaskIntoConstraints = false
        promptStack.translatesAutoresizingMaskIntoConstraints = false

        addPromptButton.target = self
        addPromptButton.action = #selector(onAddPrompt)
        addPromptButton.bezelStyle = .rounded

        let stack = NSStackView(views: [header, categoryScroll, promptScroll, addPromptButton])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        root.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: root.leadingAnchor, constant: 10),
            stack.trailingAnchor.constraint(equalTo: root.trailingAnchor, constant: -10),
            stack.topAnchor.constraint(equalTo: root.topAnchor, constant: 10),
            stack.bottomAnchor.constraint(equalTo: root.bottomAnchor, constant: -10),
            header.widthAnchor.constraint(equalTo: stack.widthAnchor),
            categoryScroll.widthAnchor.constraint(equalTo: stack.widthAnchor),
            categoryScroll.heightAnchor.constraint(equalToConstant: 32),
            promptScroll.widthAnchor.constraint(equalTo: stack.widthAnchor),
            promptScroll.heightAnchor.constraint(greaterThanOrEqualToConstant: 300),
            addPromptButton.widthAnchor.constraint(equalTo: stack.widthAnchor),
            promptStack.widthAnchor.constraint(equalTo: promptScroll.widthAnchor),
        ])
    }

    private func makeIconButton(symbol: String, tooltip: String, action: Selector) -> NSButton {
        let btn = NSButton(title: symbol, target: self, action: action)
        btn.bezelStyle = .circular
        btn.toolTip = tooltip
        btn.setButtonType(.momentaryPushIn)
        return btn
    }

    // MARK: - Public

    func dock(rightOf vto: NSWindow) {
        // Dock to the LEFT of the VTO pillar so the panel stays on-screen
        // when the user pins the pillar to the right edge.
        let origin = vto.frame.origin
        let panelSize = frame.size
        let newOrigin = NSPoint(x: origin.x - panelSize.width - 4, y: origin.y)
        setFrame(NSRect(origin: newOrigin, size: panelSize), display: true)
    }

    func refresh() {
        do {
            categories = try PromptBoardStore.shared.allCategories()
        } catch {
            NSLog("refresh failed: \(error.localizedDescription)")
            categories = []
        }
        renderCategoryTabs()
        if categories.isEmpty {
            activeCategoryId = nil
            renderEmptyState("Noch keine Kategorien. Klick +")
            return
        }
        if activeCategoryId == nil || !categories.contains(where: { $0.id == activeCategoryId }) {
            activeCategoryId = categories[0].id
        }
        renderPrompts()
    }

    // MARK: - Rendering

    private func renderCategoryTabs() {
        categoryStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        for cat in categories {
            let btn = NSButton(title: cat.name, target: self, action: #selector(onSelectCategory(_:)))
            btn.bezelStyle = .rounded
            btn.tag = categories.firstIndex(where: { $0.id == cat.id }) ?? 0
            if cat.id == activeCategoryId {
                btn.attributedTitle = NSAttributedString(string: cat.name, attributes: [
                    .foregroundColor: NSColor.white,
                    .font: NSFont.boldSystemFont(ofSize: 12)
                ])
            }
            // Right-click for context menu
            let menu = NSMenu()
            let rename = NSMenuItem(title: "Umbenennen", action: #selector(onRenameCategory(_:)), keyEquivalent: "")
            rename.target = self
            rename.representedObject = cat.id.uuidString
            let del = NSMenuItem(title: "Loeschen", action: #selector(onDeleteCategory(_:)), keyEquivalent: "")
            del.target = self
            del.representedObject = cat.id.uuidString
            menu.addItem(rename)
            menu.addItem(del)
            btn.menu = menu
            categoryStack.addArrangedSubview(btn)
        }
    }

    private func renderPrompts() {
        promptStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        guard let catId = activeCategoryId else { return }

        let prompts: [PBPrompt]
        do { prompts = try PromptBoardStore.shared.prompts(in: catId) }
        catch { NSLog("load prompts: \(error.localizedDescription)"); prompts = [] }

        if prompts.isEmpty {
            renderEmptyState("Keine Prompts in dieser Kategorie.")
            return
        }

        for p in prompts.sorted(by: { ($0.sortOrder, $0.shortLabel) < ($1.sortOrder, $1.shortLabel) }) {
            promptStack.addArrangedSubview(buildRow(for: p))
        }
    }

    private func buildRow(for prompt: PBPrompt) -> NSView {
        let row = NSView()
        row.wantsLayer = true
        row.layer?.backgroundColor = NSColor(calibratedWhite: 0.15, alpha: 1).cgColor
        row.layer?.cornerRadius = 8

        let dot = NSView(frame: NSRect(x: 0, y: 0, width: 10, height: 10))
        dot.wantsLayer = true
        dot.layer?.cornerRadius = 5
        dot.layer?.backgroundColor = (prompt.isAlwaysOn
            ? NSColor(red: 1, green: 0xD7/255.0, blue: 0, alpha: 1)
            : NSColor(calibratedWhite: 0.23, alpha: 1)).cgColor

        let insertBtn = NSButton(title: prompt.shortLabel, target: self, action: #selector(onInsertPrompt(_:)))
        insertBtn.bezelStyle = .recessed
        insertBtn.setButtonType(.momentaryLight)
        insertBtn.isBordered = false
        insertBtn.contentTintColor = .white
        insertBtn.alignment = .left
        insertBtn.toolTip = prompt.effectiveText.prefix(500).description
        insertBtn.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)

        let editBtn = NSButton(title: "✎", target: self, action: #selector(onEditPrompt(_:)))
        editBtn.bezelStyle = .recessed
        editBtn.isBordered = false
        editBtn.contentTintColor = NSColor(calibratedWhite: 0.75, alpha: 1)
        editBtn.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        editBtn.toolTip = "Bearbeiten"

        let deleteBtn = NSButton(title: "✕", target: self, action: #selector(onDeletePrompt(_:)))
        deleteBtn.bezelStyle = .recessed
        deleteBtn.isBordered = false
        deleteBtn.contentTintColor = NSColor(calibratedWhite: 0.75, alpha: 1)
        deleteBtn.identifier = NSUserInterfaceItemIdentifier(prompt.id.uuidString)
        deleteBtn.toolTip = "Loeschen"

        let rowStack = NSStackView(views: [dot, insertBtn, NSView(), editBtn, deleteBtn])
        rowStack.orientation = .horizontal
        rowStack.alignment = .centerY
        rowStack.spacing = 6
        rowStack.translatesAutoresizingMaskIntoConstraints = false
        rowStack.setContentHuggingPriority(.defaultLow, for: .horizontal)
        row.addSubview(rowStack)

        NSLayoutConstraint.activate([
            rowStack.leadingAnchor.constraint(equalTo: row.leadingAnchor, constant: 8),
            rowStack.trailingAnchor.constraint(equalTo: row.trailingAnchor, constant: -8),
            rowStack.topAnchor.constraint(equalTo: row.topAnchor, constant: 6),
            rowStack.bottomAnchor.constraint(equalTo: row.bottomAnchor, constant: -6),
            row.heightAnchor.constraint(greaterThanOrEqualToConstant: 32),
            row.widthAnchor.constraint(equalTo: promptStack.widthAnchor, constant: -4),
            dot.widthAnchor.constraint(equalToConstant: 10),
            dot.heightAnchor.constraint(equalToConstant: 10),
            editBtn.widthAnchor.constraint(equalToConstant: 22),
            deleteBtn.widthAnchor.constraint(equalToConstant: 22),
        ])
        return row
    }

    private func renderEmptyState(_ message: String) {
        promptStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        let label = NSTextField(labelWithString: message)
        label.textColor = NSColor(calibratedWhite: 0.6, alpha: 1)
        label.maximumNumberOfLines = 3
        label.lineBreakMode = .byWordWrapping
        promptStack.addArrangedSubview(label)
    }

    // MARK: - Actions

    @objc private func onSelectCategory(_ sender: NSButton) {
        guard sender.tag >= 0, sender.tag < categories.count else { return }
        activeCategoryId = categories[sender.tag].id
        renderCategoryTabs()
        renderPrompts()
    }

    @objc private func onAddCategory() {
        guard let name = PBTextInput.ask(title: "Neue Kategorie",
                                         label: "Name:", parent: self) else { return }
        let palette = ["#DCEDEC","#FCE4EC","#FFF3E0","#E8F5E9","#EDE7F6","#F3E5F5","#E3F2FD","#FFFDE7"]
        let taken = Set(categories.map { $0.backgroundColorHex })
        let color = palette.first { !taken.contains($0) } ?? "#DCEDEC"
        let nextOrder = (categories.map { $0.sortOrder }.max() ?? -1) + 1

        let now = Date()
        let cat = PBCategory(id: UUID(), name: name, sortOrder: nextOrder,
                             backgroundColorHex: color, type: 0,
                             createdAt: now, updatedAt: now)
        do {
            try PromptBoardStore.shared.addCategory(cat)
            activeCategoryId = cat.id
        } catch {
            NSAlert.warn("Kategorie konnte nicht angelegt werden: \(error.localizedDescription)")
        }
        refresh()
    }

    @objc private func onRenameCategory(_ sender: NSMenuItem) {
        guard let idStr = sender.representedObject as? String,
              let id = UUID(uuidString: idStr),
              var cat = categories.first(where: { $0.id == id }) else { return }
        guard let newName = PBTextInput.ask(title: "Kategorie umbenennen",
                                            label: "Neuer Name:",
                                            initialValue: cat.name, parent: self) else { return }
        cat.name = newName
        do { try PromptBoardStore.shared.updateCategory(cat) }
        catch { NSAlert.warn(error.localizedDescription) }
        refresh()
    }

    @objc private func onDeleteCategory(_ sender: NSMenuItem) {
        guard let idStr = sender.representedObject as? String,
              let id = UUID(uuidString: idStr),
              let cat = categories.first(where: { $0.id == id }) else { return }
        guard PBConfirm.ask(title: "Kategorie loeschen?",
                            message: "Kategorie '\(cat.name)' wird mit allen Prompts geloescht.",
                            parent: self) else { return }
        do {
            try PromptBoardStore.shared.deleteCategory(id)
            if activeCategoryId == id { activeCategoryId = nil }
        } catch { NSAlert.warn(error.localizedDescription) }
        refresh()
    }

    @objc private func onAddPrompt() {
        guard let catId = activeCategoryId else {
            NSAlert.warn("Lege zuerst eine Kategorie an.")
            return
        }
        guard let result = PBPromptEditDialog.ask(parent: self,
                                                  title: "Neuer Prompt",
                                                  label: "", text: "", alwaysOn: false) else { return }
        let now = Date()
        let prompt = PBPrompt(id: UUID(), categoryId: catId,
                              shortLabel: result.shortLabel,
                              originalText: result.originalText,
                              improvedText: nil, activeVersion: 0,
                              isAlwaysOn: result.isAlwaysOn,
                              sortOrder: 0, promptKind: "Prompt",
                              geminiModel: nil, isActiveForImprovement: false,
                              improvedByAiPromptId: nil,
                              createdAt: now, updatedAt: now)
        do { try PromptBoardStore.shared.addPrompt(prompt) }
        catch { NSAlert.warn(error.localizedDescription) }
        renderPrompts()
    }

    @objc private func onInsertPrompt(_ sender: NSButton) {
        guard let idStr = sender.identifier?.rawValue,
              let id = UUID(uuidString: idStr),
              let catId = activeCategoryId,
              let prompt = (try? PromptBoardStore.shared.prompts(in: catId))?
                .first(where: { $0.id == id }) else { return }
        onInsertText?(prompt.effectiveText)
    }

    @objc private func onEditPrompt(_ sender: NSButton) {
        guard let idStr = sender.identifier?.rawValue,
              let id = UUID(uuidString: idStr),
              let catId = activeCategoryId,
              var prompt = (try? PromptBoardStore.shared.prompts(in: catId))?
                .first(where: { $0.id == id }) else { return }
        guard let result = PBPromptEditDialog.ask(parent: self,
                                                  title: "Prompt bearbeiten",
                                                  label: prompt.shortLabel,
                                                  text: prompt.originalText,
                                                  alwaysOn: prompt.isAlwaysOn) else { return }
        prompt.shortLabel = result.shortLabel
        prompt.originalText = result.originalText
        prompt.isAlwaysOn = result.isAlwaysOn
        do { try PromptBoardStore.shared.updatePrompt(prompt) }
        catch { NSAlert.warn(error.localizedDescription) }
        renderPrompts()
    }

    @objc private func onDeletePrompt(_ sender: NSButton) {
        guard let idStr = sender.identifier?.rawValue,
              let id = UUID(uuidString: idStr),
              let catId = activeCategoryId,
              let prompt = (try? PromptBoardStore.shared.prompts(in: catId))?
                .first(where: { $0.id == id }) else { return }
        guard PBConfirm.ask(title: "Prompt loeschen?",
                            message: "Prompt '\(prompt.shortLabel)' wirklich loeschen?",
                            parent: self) else { return }
        do { try PromptBoardStore.shared.deletePrompt(id) }
        catch { NSAlert.warn(error.localizedDescription) }
        renderPrompts()
    }

    @objc private func onSettings() {
        guard let settings = try? PromptBoardStore.shared.settings() else { return }
        guard let result = PBSettingsDialog.ask(parent: self, settings: settings) else { return }
        var latest = (try? PromptBoardStore.shared.settings()) ?? settings
        latest.groqApiKey = result.groqApiKey
        latest.geminiApiKey = result.geminiApiKey
        latest.separatorTemplate = result.separatorTemplate
        latest.googleClientId = result.googleClientId
        latest.googleClientSecret = result.googleClientSecret
        try? PromptBoardStore.shared.updateSettings(latest)
    }

    @objc private func onBackup() {
        guard let action = PBTextInput.ask(
            title: "Backup / Wiederherstellen",
            label: "E = Export Datei, I = Import Datei, G = Google Drive sichern, R = Google Drive laden:",
            parent: self) else { return }
        switch action.trimmingCharacters(in: .whitespaces).uppercased() {
        case "E": exportToFile()
        case "I": importFromFile()
        case "G": uploadToDrive()
        case "R": restoreFromDrive()
        default: break
        }
    }

    // MARK: - Backup helpers

    private func buildBackupJson() throws -> String {
        let cats = try PromptBoardStore.shared.allCategories()
        var allPrompts: [PBPrompt] = []
        for c in cats { allPrompts.append(contentsOf: try PromptBoardStore.shared.prompts(in: c.id)) }
        let settings = try PromptBoardStore.shared.settings()

        let payload: [String: Any] = [
            "ExportedAt": ISO8601DateFormatter().string(from: Date()),
            "SeparatorTemplate": settings.separatorTemplate,
            "Categories": cats.map { c -> [String: Any] in
                [
                    "Id": c.id.uuidString,
                    "Name": c.name,
                    "SortOrder": c.sortOrder,
                    "BackgroundColorHex": c.backgroundColorHex,
                    "Type": c.type,
                ]
            },
            "Prompts": allPrompts.map { p -> [String: Any] in
                var dict: [String: Any] = [
                    "Id": p.id.uuidString,
                    "CategoryId": p.categoryId.uuidString,
                    "ShortLabel": p.shortLabel,
                    "OriginalText": p.originalText,
                    "ActiveVersion": p.activeVersion,
                    "IsAlwaysOn": p.isAlwaysOn,
                    "SortOrder": p.sortOrder,
                ]
                if let it = p.improvedText { dict["ImprovedText"] = it }
                return dict
            },
        ]
        let data = try JSONSerialization.data(withJSONObject: payload, options: [.prettyPrinted])
        return String(data: data, encoding: .utf8) ?? ""
    }

    private func applyBackupJson(_ json: String) throws {
        guard let data = json.data(using: .utf8),
              let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw NSError(domain: "PromptBoardPanel", code: 1,
                userInfo: [NSLocalizedDescriptionKey: "Ungueltiges Backup."])
        }
        let cats = root["Categories"] as? [[String: Any]] ?? []
        let prompts = root["Prompts"] as? [[String: Any]] ?? []

        for c in cats {
            guard let idStr = c["Id"] as? String, let id = UUID(uuidString: idStr) else { continue }
            let now = Date()
            let cat = PBCategory(
                id: id,
                name: (c["Name"] as? String) ?? "",
                sortOrder: (c["SortOrder"] as? Int) ?? 0,
                backgroundColorHex: (c["BackgroundColorHex"] as? String) ?? "#DCEDEC",
                type: (c["Type"] as? Int) ?? 0,
                createdAt: now, updatedAt: now)
            try PromptBoardStore.shared.upsertCategory(cat)
        }
        for p in prompts {
            guard let idStr = p["Id"] as? String, let id = UUID(uuidString: idStr),
                  let catStr = p["CategoryId"] as? String, let catId = UUID(uuidString: catStr) else { continue }
            let now = Date()
            let prompt = PBPrompt(
                id: id, categoryId: catId,
                shortLabel: (p["ShortLabel"] as? String) ?? "",
                originalText: (p["OriginalText"] as? String) ?? "",
                improvedText: p["ImprovedText"] as? String,
                activeVersion: (p["ActiveVersion"] as? Int) ?? 0,
                isAlwaysOn: (p["IsAlwaysOn"] as? Bool) ?? false,
                sortOrder: (p["SortOrder"] as? Int) ?? 0,
                promptKind: (p["PromptKind"] as? String) ?? "Prompt",
                geminiModel: p["GeminiModel"] as? String,
                isActiveForImprovement: (p["IsActiveForImprovement"] as? Bool) ?? false,
                improvedByAiPromptId: (p["ImprovedByAiPromptId"] as? String).flatMap(UUID.init(uuidString:)),
                createdAt: now, updatedAt: now)
            try PromptBoardStore.shared.upsertPrompt(prompt)
        }
    }

    private func exportToFile() {
        let save = NSSavePanel()
        save.nameFieldStringValue = "promptboard-backup-\(Int(Date().timeIntervalSince1970)).json"
        save.allowedContentTypes = [.json]
        guard save.runModal() == .OK, let url = save.url else { return }
        do {
            let json = try buildBackupJson()
            try json.write(to: url, atomically: true, encoding: .utf8)
            NSAlert.warn("Backup gespeichert: \(url.path)")
        } catch {
            NSAlert.warn("Export fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    private func importFromFile() {
        let open = NSOpenPanel()
        open.allowedContentTypes = [.json]
        open.allowsMultipleSelection = false
        guard open.runModal() == .OK, let url = open.url else { return }
        guard PBConfirm.ask(title: "Import bestaetigen",
                            message: "Vorhandene Eintraege mit gleicher ID werden ueberschrieben.",
                            confirmLabel: "Importieren", parent: self) else { return }
        do {
            let json = try String(contentsOf: url)
            try applyBackupJson(json)
            NSAlert.warn("Import abgeschlossen.")
            refresh()
        } catch {
            NSAlert.warn("Import fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    private func uploadToDrive() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            NSAlert.warn("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.")
            return
        }
        do {
            let json = try buildBackupJson()
            GoogleDriveBackupService.shared.upload(json: json) { result in
                DispatchQueue.main.async {
                    switch result {
                    case .success: NSAlert.warn("Backup bei Google Drive gespeichert.")
                    case .failure(let e): NSAlert.warn("Upload fehlgeschlagen: \(e.localizedDescription)")
                    }
                }
            }
        } catch {
            NSAlert.warn("Upload fehlgeschlagen: \(error.localizedDescription)")
        }
    }

    private func restoreFromDrive() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            NSAlert.warn("Noch kein Google-Konto verbunden. Bitte in den Einstellungen verbinden.")
            return
        }
        GoogleDriveBackupService.shared.downloadLatest { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }
                switch result {
                case .failure(let e):
                    NSAlert.warn("Download fehlgeschlagen: \(e.localizedDescription)")
                case .success(nil):
                    NSAlert.warn("Kein Backup bei Google Drive gefunden.")
                case .success(let json?):
                    guard PBConfirm.ask(title: "Google-Drive-Backup laden",
                                        message: "Lokale Eintraege mit gleicher ID werden ueberschrieben.",
                                        confirmLabel: "Einspielen", parent: self) else { return }
                    do {
                        try self.applyBackupJson(json)
                        NSAlert.warn("Google-Drive-Backup eingespielt.")
                        self.refresh()
                    } catch {
                        NSAlert.warn("Einspielen fehlgeschlagen: \(error.localizedDescription)")
                    }
                }
            }
        }
    }
}
