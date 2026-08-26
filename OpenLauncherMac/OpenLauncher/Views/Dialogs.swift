import AppKit

/// Alle modalen Fenster des Launchers. In WPF liegen sie teils direkt im ViewModel
/// (ShowModelDialog, ConfirmRemoveModel, ShowLastError) und teils als eigene Fenster
/// (ProfileEditorWindow, HiddenModelsWindow) vor - hier gebuendelt an einer Stelle.
@MainActor
enum Dialogs {
    /// Hintergrund fuer modale Fenster (`Background="#202028"` in XAML).
    private static var sheetBackground: NSColor {
        ThemeManager.current == .dark ? NSColor.wpf("#202028") : NSColor.wpf("#F2F0FE")
    }

    // MARK: - Modell hinzufuegen / bearbeiten

    /// `ShowModelDialog` aus MainViewModel.cs: Kategorie-Auswahl, Modell-ID, optionaler Anzeigename.
    static func modelDialog(parent: NSWindow?, groups: [ModelGroupEntry], defaultGroup: ModelGroupEntry,
                            title: String, confirmText: String, initialSlug: String,
                            initialDisplayName: String) -> (group: ModelGroupEntry, slug: String, displayName: String)? {
        guard !groups.isEmpty else { return nil }

        let window = makePanel(title: title, width: 560, height: 330)
        let content = NSView()
        content.translatesAutoresizingMaskIntoConstraints = false
        window.contentView = content

        let categoryLabel = UI.label("In welche Kategorie soll das Modell?", size: 13)
        let categoryPopUp = NSPopUpButton()
        categoryPopUp.translatesAutoresizingMaskIntoConstraints = false
        for group in groups { categoryPopUp.addItem(withTitle: group.title) }
        if let index = groups.firstIndex(where: { $0 === defaultGroup }) { categoryPopUp.selectItem(at: index) }

        let slugLabel = UI.label("Modell-ID", size: 13)
        let slugField = RoundedTextField()
        slugField.translatesAutoresizingMaskIntoConstraints = false
        slugField.stringValue = initialSlug

        let displayLabel = UI.label("Anzeigename (optional)", size: 13)
        let displayField = RoundedTextField()
        displayField.translatesAutoresizingMaskIntoConstraints = false
        displayField.stringValue = initialDisplayName

        let cancelButton = StyledButton(style: .ghost, title: "Abbrechen")
        let confirmButton = StyledButton(style: .accent, title: confirmText)
        confirmButton.horizontalPadding = 18
        cancelButton.horizontalPadding = 18

        let buttonRow = NSStackView(views: [cancelButton, confirmButton])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8
        buttonRow.translatesAutoresizingMaskIntoConstraints = false

        for view in [categoryLabel, categoryPopUp, slugLabel, slugField, displayLabel, displayField, buttonRow] as [NSView] {
            content.addSubview(view)
        }

        NSLayoutConstraint.activate([
            categoryLabel.topAnchor.constraint(equalTo: content.topAnchor, constant: 18),
            categoryLabel.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),

            categoryPopUp.topAnchor.constraint(equalTo: categoryLabel.bottomAnchor, constant: 8),
            categoryPopUp.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            categoryPopUp.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),

            slugLabel.topAnchor.constraint(equalTo: categoryPopUp.bottomAnchor, constant: 14),
            slugLabel.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),

            slugField.topAnchor.constraint(equalTo: slugLabel.bottomAnchor, constant: 8),
            slugField.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            slugField.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),

            displayLabel.topAnchor.constraint(equalTo: slugField.bottomAnchor, constant: 12),
            displayLabel.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),

            displayField.topAnchor.constraint(equalTo: displayLabel.bottomAnchor, constant: 8),
            displayField.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            displayField.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),

            buttonRow.topAnchor.constraint(equalTo: displayField.bottomAnchor, constant: 18),
            buttonRow.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18)
        ])

        var accepted = false
        let handler = ButtonHandler()
        handler.onCancel = { window.sheetParent?.endSheet(window, returnCode: .cancel) ?? NSApp.stopModal(withCode: .cancel) }
        handler.onConfirm = {
            guard !slugField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
            accepted = true
            window.sheetParent?.endSheet(window, returnCode: .OK) ?? NSApp.stopModal(withCode: .OK)
        }
        cancelButton.target = handler
        cancelButton.action = #selector(ButtonHandler.cancelClicked)
        confirmButton.target = handler
        confirmButton.action = #selector(ButtonHandler.confirmClicked)

        window.initialFirstResponder = slugField
        runModal(window, parent: parent)

        guard accepted else { return nil }
        let group = groups[Swift.max(0, categoryPopUp.indexOfSelectedItem)]
        return (group,
                slugField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines),
                displayField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    // MARK: - Modell entfernen bestaetigen

    /// `ConfirmRemoveModel` aus MainViewModel.cs.
    static func confirmRemoveModel(parent: NSWindow?, displayName: String, slug: String) -> Bool {
        let alert = NSAlert()
        alert.messageText = "Modell entfernen?"
        alert.informativeText = "\(displayName)\n\(slug)\n\nDas Modell wird nur aus der Launcher-Liste entfernt. OpenCode selbst bleibt unverändert."
        alert.alertStyle = .warning
        alert.addButton(withTitle: "Entfernen")
        alert.addButton(withTitle: "Abbrechen")
        if let parent {
            var result = false
            let semaphore = DispatchSemaphore(value: 0)
            alert.beginSheetModal(for: parent) { response in
                result = response == .alertFirstButtonReturn
                semaphore.signal()
            }
            // Sheets laufen asynchron; hier wird bewusst auf die Antwort gewartet, weil der Aufrufer
            // (wie in WPF) ein synchrones Ja/Nein erwartet.
            while semaphore.wait(timeout: .now()) == .timedOut {
                RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.01))
            }
            return result
        }
        return alert.runModal() == .alertFirstButtonReturn
    }

    // MARK: - Fehlerdetails

    /// `ShowLastError` aus MainViewModel.cs: schreibgeschuetztes, scrollbares Textfeld.
    static func showErrorDetails(parent: NSWindow?, details: String) {
        let window = makePanel(title: "OpenLauncher - Fehlerdetails", width: 980, height: 720)
        let content = NSView()
        window.contentView = content

        let textView = NSTextView()
        textView.isEditable = false
        textView.isSelectable = true
        textView.string = details
        textView.font = .monospacedSystemFont(ofSize: 12, weight: .regular)
        textView.backgroundColor = ThemeManager.current == .dark ? NSColor.wpf("#18181E") : .white
        textView.textColor = ThemeManager.current == .dark ? .white : .black
        textView.isVerticallyResizable = true
        textView.isHorizontallyResizable = true
        textView.textContainer?.widthTracksTextView = false
        textView.textContainer?.containerSize = NSSize(width: CGFloat.greatestFiniteMagnitude,
                                                        height: CGFloat.greatestFiniteMagnitude)
        textView.textContainerInset = NSSize(width: 10, height: 10)

        let scrollView = NSScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.hasVerticalScroller = true
        scrollView.hasHorizontalScroller = true
        scrollView.borderType = .lineBorder
        scrollView.documentView = textView

        let closeButton = StyledButton(style: .ghost, title: "Schließen")
        closeButton.horizontalPadding = 20
        let handler = ButtonHandler()
        handler.onCancel = { window.sheetParent?.endSheet(window, returnCode: .cancel) ?? NSApp.stopModal(withCode: .cancel) }
        closeButton.target = handler
        closeButton.action = #selector(ButtonHandler.cancelClicked)

        content.addSubview(scrollView)
        content.addSubview(closeButton)
        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: content.topAnchor, constant: 14),
            scrollView.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 14),
            scrollView.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -14),
            scrollView.bottomAnchor.constraint(equalTo: closeButton.topAnchor, constant: -12),
            closeButton.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -14),
            closeButton.bottomAnchor.constraint(equalTo: content.bottomAnchor, constant: -14)
        ])

        runModal(window, parent: parent)
    }

    // MARK: - Profil-/Modus-Editor

    /// Gegenstueck zu ProfileEditorWindow.xaml. Beide Betriebsarten (Profil und Arbeitsmodus)
    /// teilen sich dasselbe Fenster - genau wie unter Windows.
    static func editText(parent: NSWindow?, windowTitle: String, headline: String, cliText: String,
                         introText: String, filePath: String, text: String,
                         saveButtonTitle: String) -> String? {
        let window = makePanel(title: windowTitle, width: 1040, height: 760, resizable: true)
        let content = NSView()
        window.contentView = content

        let badge = LogoBadgeView()
        let titleLabel = UI.label(headline, size: 15, weight: .semibold)
        let cliLabel = UI.label(cliText, size: 11, role: .dim)
        let introLabel = UI.label(introText, size: 12, role: .muted)
        introLabel.lineBreakMode = .byWordWrapping
        introLabel.maximumNumberOfLines = 6
        introLabel.preferredMaxLayoutWidth = 980

        let fileChip = UI.label((filePath as NSString).lastPathComponent, size: 12, weight: .bold, role: .muted)
        let pathLabel = UI.label(filePath, size: 11, role: .dim, monospaced: true)
        pathLabel.lineBreakMode = .byTruncatingMiddle

        let textView = NSTextView()
        textView.isEditable = true
        textView.isRichText = false
        textView.string = text
        textView.font = .monospacedSystemFont(ofSize: 12.5, weight: .regular)
        textView.backgroundColor = ThemeManager.current == .dark ? NSColor.wpf("#18181E") : .white
        textView.textColor = ThemeManager.current == .dark ? NSColor.wpf("#F1EFF9") : NSColor.wpf("#211D33")
        textView.insertionPointColor = textView.textColor ?? .white
        textView.isAutomaticQuoteSubstitutionEnabled = false
        textView.isAutomaticDashSubstitutionEnabled = false
        textView.isAutomaticTextReplacementEnabled = false
        textView.isVerticallyResizable = true
        textView.textContainerInset = NSSize(width: 10, height: 10)
        textView.textContainer?.widthTracksTextView = true

        let scrollView = NSScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.hasVerticalScroller = true
        scrollView.borderType = .lineBorder
        scrollView.documentView = textView

        let cancelButton = StyledButton(style: .ghost, title: "Abbrechen")
        let saveButton = StyledButton(style: .accent, title: saveButtonTitle)
        cancelButton.horizontalPadding = 18
        saveButton.horizontalPadding = 18

        var accepted = false
        let handler = ButtonHandler()
        handler.onCancel = { window.sheetParent?.endSheet(window, returnCode: .cancel) ?? NSApp.stopModal(withCode: .cancel) }
        handler.onConfirm = {
            accepted = true
            window.sheetParent?.endSheet(window, returnCode: .OK) ?? NSApp.stopModal(withCode: .OK)
        }
        cancelButton.target = handler
        cancelButton.action = #selector(ButtonHandler.cancelClicked)
        saveButton.target = handler
        saveButton.action = #selector(ButtonHandler.confirmClicked)

        let buttonRow = NSStackView(views: [cancelButton, saveButton])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8
        buttonRow.translatesAutoresizingMaskIntoConstraints = false

        for view in [badge, titleLabel, cliLabel, introLabel, fileChip, pathLabel, scrollView, buttonRow] as [NSView] {
            view.translatesAutoresizingMaskIntoConstraints = false
            content.addSubview(view)
        }

        NSLayoutConstraint.activate([
            badge.topAnchor.constraint(equalTo: content.topAnchor, constant: 14),
            badge.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            badge.widthAnchor.constraint(equalToConstant: 32),
            badge.heightAnchor.constraint(equalToConstant: 32),

            titleLabel.topAnchor.constraint(equalTo: content.topAnchor, constant: 12),
            titleLabel.leadingAnchor.constraint(equalTo: badge.trailingAnchor, constant: 12),
            cliLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 2),
            cliLabel.leadingAnchor.constraint(equalTo: badge.trailingAnchor, constant: 12),

            introLabel.topAnchor.constraint(equalTo: badge.bottomAnchor, constant: 14),
            introLabel.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            introLabel.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),

            fileChip.topAnchor.constraint(equalTo: introLabel.bottomAnchor, constant: 14),
            fileChip.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            pathLabel.topAnchor.constraint(equalTo: fileChip.bottomAnchor, constant: 6),
            pathLabel.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            pathLabel.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),

            scrollView.topAnchor.constraint(equalTo: pathLabel.bottomAnchor, constant: 10),
            scrollView.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            scrollView.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),
            scrollView.bottomAnchor.constraint(equalTo: buttonRow.topAnchor, constant: -14),

            buttonRow.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),
            buttonRow.bottomAnchor.constraint(equalTo: content.bottomAnchor, constant: -16)
        ])

        window.initialFirstResponder = textView
        runModal(window, parent: parent)
        return accepted ? textView.string : nil
    }

    // MARK: - Ausgeblendete Modelle

    /// Gegenstueck zu HiddenModelsWindow.xaml.
    static func showHiddenModels(parent: NSWindow?, viewModel: MainViewModel) {
        let window = makePanel(title: "Ausgeblendete Modelle", width: 680, height: 560, resizable: true)
        let content = NSView()
        window.contentView = content

        let headline = UI.label("Ausgeblendete Modelle", size: 21, weight: .semibold)
        let subline = UI.label("Diese Modelle sind weiterhin gespeichert. Blende sie hier einzeln wieder ein.",
                               size: 13, role: .muted)
        let countLabel = UI.label("\(viewModel.hiddenModels.count) ausgeblendet", size: 12, role: .dim)

        let stack = NSStackView()
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false

        let documentView = FlippedView()
        documentView.translatesAutoresizingMaskIntoConstraints = false
        documentView.addSubview(stack)

        let scrollView = NSScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.hasVerticalScroller = true
        scrollView.drawsBackground = false
        scrollView.documentView = documentView

        let closeButton = StyledButton(style: .ghost, title: "Schließen")
        closeButton.horizontalPadding = 14
        let handler = ButtonHandler()
        handler.onCancel = { window.sheetParent?.endSheet(window, returnCode: .cancel) ?? NSApp.stopModal(withCode: .cancel) }
        closeButton.target = handler
        closeButton.action = #selector(ButtonHandler.cancelClicked)

        func rebuild() {
            for view in stack.arrangedSubviews {
                stack.removeArrangedSubview(view)
                view.removeFromSuperview()
            }
            for model in viewModel.hiddenModels {
                let card = SurfaceView(cornerRadius: 12)
                card.translatesAutoresizingMaskIntoConstraints = false
                let name = UI.label(model.displayName, size: 13, weight: .semibold)
                let slug = UI.label(model.modelString, size: 11, role: .dim, monospaced: true)
                let restore = StyledButton(style: .ghost, title: "Einblenden")
                restore.horizontalPadding = 14
                let restoreHandler = ButtonHandler()
                restoreHandler.onConfirm = {
                    viewModel.restoreModel(model)
                    countLabel.stringValue = "\(viewModel.hiddenModels.count) ausgeblendet"
                    rebuild()
                }
                restore.target = restoreHandler
                restore.action = #selector(ButtonHandler.confirmClicked)
                // Handler am Button festhalten, damit er nicht vorzeitig freigegeben wird.
                objc_setAssociatedObject(restore, Unmanaged.passUnretained(restore).toOpaque(),
                                         restoreHandler, .OBJC_ASSOCIATION_RETAIN)

                for view in [name, slug, restore] as [NSView] {
                    view.translatesAutoresizingMaskIntoConstraints = false
                    card.addSubview(view)
                }
                NSLayoutConstraint.activate([
                    name.topAnchor.constraint(equalTo: card.topAnchor, constant: 11),
                    name.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 14),
                    name.trailingAnchor.constraint(lessThanOrEqualTo: restore.leadingAnchor, constant: -14),
                    slug.topAnchor.constraint(equalTo: name.bottomAnchor, constant: 3),
                    slug.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 14),
                    slug.trailingAnchor.constraint(lessThanOrEqualTo: restore.leadingAnchor, constant: -14),
                    slug.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -11),
                    restore.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -14),
                    restore.centerYAnchor.constraint(equalTo: card.centerYAnchor)
                ])
                stack.addArrangedSubview(card)
                card.widthAnchor.constraint(equalTo: stack.widthAnchor).isActive = true
            }
        }
        rebuild()

        for view in [headline, subline, scrollView, countLabel, closeButton] as [NSView] {
            view.translatesAutoresizingMaskIntoConstraints = false
            content.addSubview(view)
        }

        NSLayoutConstraint.activate([
            headline.topAnchor.constraint(equalTo: content.topAnchor, constant: 18),
            headline.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 20),
            subline.topAnchor.constraint(equalTo: headline.bottomAnchor, constant: 5),
            subline.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 20),
            subline.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -20),

            scrollView.topAnchor.constraint(equalTo: subline.bottomAnchor, constant: 14),
            scrollView.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 18),
            scrollView.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),
            scrollView.bottomAnchor.constraint(equalTo: closeButton.topAnchor, constant: -14),

            documentView.leadingAnchor.constraint(equalTo: scrollView.contentView.leadingAnchor),
            documentView.trailingAnchor.constraint(equalTo: scrollView.contentView.trailingAnchor),
            documentView.topAnchor.constraint(equalTo: scrollView.contentView.topAnchor),
            stack.topAnchor.constraint(equalTo: documentView.topAnchor),
            stack.leadingAnchor.constraint(equalTo: documentView.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: documentView.trailingAnchor),
            stack.bottomAnchor.constraint(equalTo: documentView.bottomAnchor),

            countLabel.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 20),
            countLabel.centerYAnchor.constraint(equalTo: closeButton.centerYAnchor),
            closeButton.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -18),
            closeButton.bottomAnchor.constraint(equalTo: content.bottomAnchor, constant: -18)
        ])

        runModal(window, parent: parent)
    }

    // MARK: - Helfer

    private static func makePanel(title: String, width: CGFloat, height: CGFloat,
                                  resizable: Bool = false) -> NSWindow {
        var mask: NSWindow.StyleMask = [.titled, .closable]
        if resizable { mask.insert(.resizable) }
        // StyleMask bewusst SCHON HIER final setzen und nie zur Laufzeit umschalten
        // (Bug-Almanach swift-appkit §A2/§A5).
        let window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: width, height: height),
                              styleMask: mask, backing: .buffered, defer: false)
        window.title = title
        window.isReleasedWhenClosed = false
        window.backgroundColor = sheetBackground
        window.appearance = NSAppearance(named: ThemeManager.current == .dark ? .darkAqua : .aqua)
        window.contentView?.translatesAutoresizingMaskIntoConstraints = false
        return window
    }

    private static func runModal(_ window: NSWindow, parent: NSWindow?) {
        if let parent {
            parent.beginSheet(window) { _ in NSApp.stopModal() }
            NSApp.runModal(for: window)
            parent.endSheet(window)
        } else {
            window.center()
            NSApp.runModal(for: window)
        }
        window.orderOut(nil)
    }
}

/// Kleiner Ziel-Empfaenger fuer die selbstgezeichneten Buttons (die kein NSButton sind und deshalb
/// kein eigenes Action-Ziel mitbringen).
@MainActor
final class ButtonHandler: NSObject {
    var onCancel: (() -> Void)?
    var onConfirm: (() -> Void)?

    @objc func cancelClicked() { onCancel?() }
    @objc func confirmClicked() { onConfirm?() }
}

/// Das Akzent-Quadrat mit dem ✦ aus der Titelleiste.
final class LogoBadgeView: NSView {
    private let label = UI.label("✦", size: 17, role: .accent)

    init() {
        super.init(frame: .zero)
        wantsLayer = true
        addSubview(label)
        NSLayoutConstraint.activate([
            label.centerXAnchor.constraint(equalTo: centerXAnchor),
            label.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
        applyTheme()
        NotificationCenter.default.addObserver(self, selector: #selector(applyTheme),
                                               name: ThemeManager.themeChangedNotification, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    @objc private func applyTheme() {
        let palette = ThemeManager.palette
        layer?.cornerRadius = 10
        layer?.borderWidth = 1
        layer?.backgroundColor = palette.accentSoftBg.flattened(over: ThemeManager.flattenBase).cgColor
        layer?.borderColor = palette.accentLine.cgColor
        label.applyTheme()
    }
}
