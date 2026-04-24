import AppKit

/// Minimal AppKit dialogs used by the PromptBoard panel. Kept in one
/// file because each dialog is only a handful of lines in AppKit — no
/// need for separate XIBs.

// MARK: - Modal presenter (safe against invisible / trapped modal sessions)

/// Bundles the "open a modal dialog safely while the app is background"
/// pattern so every call site does it the same way.
///
/// The Voice Overlay is a background app whose only windows are
/// `.floating` panels that deliberately can't become key. When a modal
/// dialog is triggered (NSAlert, NSSavePanel, NSWindow+runModal, etc.)
/// without preparation, three things go wrong at once:
///  1. The dialog opens while our app is inactive → it can slip behind
///     other apps' windows and be invisible to the user.
///  2. Our own `.floating` pillar/PromptBoard panels sit *above* the
///     dialog and steal clicks.
///  3. User clicks then fall through to the modal session and produce
///     system beeps, making the app look frozen.
///
/// `PBModalPresenter.runHidingFloatingPanels` solves all three at once:
///  - Force-activates our app
///  - Hides every `.floating` window while the modal is up
///  - Restores them (level + visibility) after the modal closes — even
///    if the modal throws or the user kills the dialog from the dock.
enum PBModalPresenter {
    /// Execute `body` while all floating panels are hidden and our app
    /// is active. The body itself does the `runModal` call; this helper
    /// only sets up the environment around it.
    @discardableResult
    static func runHidingFloatingPanels<T>(_ body: () -> T) -> T {
        let floatingVisible = NSApp.windows.filter {
            $0.isVisible && $0.level == .floating
        }
        floatingVisible.forEach { $0.orderOut(nil) }
        defer {
            // Always put them back — .floating level, ordered front,
            // even if the body panicked or we got here via an early exit.
            floatingVisible.forEach {
                $0.level = .floating
                $0.orderFrontRegardless()
            }
        }
        NSApp.activate(ignoringOtherApps: true)
        return body()
    }

    /// Convenience wrapper for NSAlert which does its own window management;
    /// we only need to hide floating panels + activate the app around it.
    static func runModal(on alert: NSAlert) -> NSApplication.ModalResponse {
        return runHidingFloatingPanels {
            alert.window.level = .modalPanel
            alert.window.makeKeyAndOrderFront(nil)
            alert.window.orderFrontRegardless()
            return alert.runModal()
        }
    }

    /// Convenience wrapper for NSSavePanel / NSOpenPanel.
    static func runModal(on savePanel: NSSavePanel) -> NSApplication.ModalResponse {
        return runHidingFloatingPanels {
            savePanel.level = .modalPanel
            savePanel.makeKeyAndOrderFront(nil)
            savePanel.orderFrontRegardless()
            return savePanel.runModal()
        }
    }
}

// MARK: - Shared dark theme

/// The PromptBoard panel is always dark (calibratedWhite 0.11, 0.97 alpha).
/// To keep the auxiliary dialogs (new/edit prompt, settings, confirms,
/// text input) visually consistent, every dialog window is forced into
/// dark-aqua appearance and content views/fields are tinted explicitly.
enum PBDarkTheme {
    static let panelBackground = NSColor(calibratedWhite: 0.13, alpha: 1)
    static let fieldBackground = NSColor(calibratedWhite: 0.19, alpha: 1)
    static let fieldBorder     = NSColor(calibratedWhite: 0.38, alpha: 1)
    static let textPrimary     = NSColor.white
    static let textSecondary   = NSColor(calibratedWhite: 0.85, alpha: 1)

    static func apply(to window: NSWindow) {
        window.appearance = NSAppearance(named: .darkAqua)
        window.backgroundColor = panelBackground
    }

    /// Applies dark appearance to an NSAlert so system alerts also blend
    /// with the PromptBoard style.
    static func apply(to alert: NSAlert) {
        alert.window.appearance = NSAppearance(named: .darkAqua)
    }

    static func styleLabel(_ label: NSTextField) {
        label.textColor = textSecondary
        label.drawsBackground = false
    }

    static func styleField(_ field: NSTextField) {
        field.textColor = textPrimary
        field.backgroundColor = fieldBackground
        field.drawsBackground = true
        field.wantsLayer = true
        field.layer?.cornerRadius = 4
        field.layer?.borderColor = fieldBorder.cgColor
        field.layer?.borderWidth = 1
        field.focusRingType = .none
    }

    /// Accent-filled action button (e.g. "Speichern", "Verbinden").
    static func makePrimaryButton(title: String, target: AnyObject?, action: Selector) -> NSButton {
        let btn = NSButton(title: "", target: target, action: action)
        btn.isBordered = false
        btn.wantsLayer = true
        btn.layer?.backgroundColor = NSColor(red: 0.29, green: 0.56, blue: 0.99, alpha: 1).cgColor
        btn.layer?.cornerRadius = 6
        btn.attributedTitle = NSAttributedString(
            string: title,
            attributes: [
                .foregroundColor: NSColor.white,
                .font: NSFont.systemFont(ofSize: 13, weight: .semibold)
            ])
        btn.widthAnchor.constraint(greaterThanOrEqualToConstant: 96).isActive = true
        btn.heightAnchor.constraint(equalToConstant: 28).isActive = true
        return btn
    }

    /// Neutral outline button (e.g. "Abbrechen", "Trennen").
    static func makeSecondaryButton(title: String, target: AnyObject?, action: Selector) -> NSButton {
        let btn = NSButton(title: "", target: target, action: action)
        btn.isBordered = false
        btn.wantsLayer = true
        btn.layer?.backgroundColor = NSColor(calibratedWhite: 0.24, alpha: 1).cgColor
        btn.layer?.borderColor = NSColor(calibratedWhite: 0.45, alpha: 1).cgColor
        btn.layer?.borderWidth = 1
        btn.layer?.cornerRadius = 6
        btn.attributedTitle = NSAttributedString(
            string: title,
            attributes: [
                .foregroundColor: NSColor.white,
                .font: NSFont.systemFont(ofSize: 13, weight: .medium)
            ])
        btn.widthAnchor.constraint(greaterThanOrEqualToConstant: 96).isActive = true
        btn.heightAnchor.constraint(equalToConstant: 28).isActive = true
        return btn
    }
}

// MARK: - Text input

enum PBTextInput {
    /// Shows a modal dialog asking for a single line of text. Returns the
    /// trimmed string or nil when the user cancels.
    static func ask(title: String, label: String, initialValue: String = "",
                    parent: NSWindow?) -> String? {
        let alert = NSAlert()
        alert.messageText = title
        alert.informativeText = label
        alert.addButton(withTitle: "OK")
        alert.addButton(withTitle: "Abbrechen")
        PBDarkTheme.apply(to: alert)

        let field = NSTextField(frame: NSRect(x: 0, y: 0, width: 320, height: 22))
        field.stringValue = initialValue
        PBDarkTheme.styleField(field)
        alert.accessoryView = field
        alert.window.initialFirstResponder = field

        // Free-floating modal (no sheet binding) — sheet-modal on NSPanel was buggy
        // and caused the parent panel to collapse.
        _ = parent  // kept for API compatibility
        let response = PBModalPresenter.runModal(on: alert)

        guard response == .alertFirstButtonReturn else { return nil }
        let v = field.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
        return v.isEmpty ? nil : v
    }
}

// MARK: - Confirm

enum PBConfirm {
    static func ask(title: String, message: String,
                    confirmLabel: String = "Loeschen",
                    parent: NSWindow?) -> Bool {
        let alert = NSAlert()
        alert.messageText = title
        alert.informativeText = message
        alert.alertStyle = .warning
        alert.addButton(withTitle: confirmLabel)
        alert.addButton(withTitle: "Abbrechen")
        PBDarkTheme.apply(to: alert)
        _ = parent  // kept for API compatibility; runSheetModal on NSPanel was buggy
        let response = PBModalPresenter.runModal(on: alert)
        return response == .alertFirstButtonReturn
    }
}

// MARK: - Prompt editor

struct PBPromptEditResult {
    let shortLabel: String
    let originalText: String
    let isAlwaysOn: Bool
}

final class PBPromptEditDialog: NSWindowController, NSWindowDelegate {
    private let labelField = NSTextField()
    private let textView = NSTextView()
    private let alwaysOnCheckbox = NSButton(checkboxWithTitle: "Immer mitschicken (Always-On Prefix)", target: nil, action: nil)
    private var result: PBPromptEditResult?

    init(title: String, initialLabel: String, initialText: String, initialAlwaysOn: Bool) {
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 540, height: 460),
            styleMask: [.titled, .closable],
            backing: .buffered, defer: false)
        window.title = title
        window.isReleasedWhenClosed = false
        // Keep the editor above our .floating pillar/panel instead of demoting
        // those panels temporarily — demotion caused stuck-low-level panels
        // when runModal didn't return cleanly.
        window.level = .modalPanel
        PBDarkTheme.apply(to: window)
        super.init(window: window)

        labelField.placeholderString = "Kurzbezeichnung"
        labelField.stringValue = initialLabel
        PBDarkTheme.styleField(labelField)

        let scroll = NSScrollView()
        scroll.hasVerticalScroller = true
        scroll.borderType = .noBorder
        scroll.drawsBackground = true
        scroll.backgroundColor = PBDarkTheme.fieldBackground
        scroll.wantsLayer = true
        scroll.layer?.cornerRadius = 6
        scroll.layer?.borderWidth = 1
        scroll.layer?.borderColor = PBDarkTheme.fieldBorder.cgColor
        textView.isEditable = true
        textView.isRichText = false
        textView.string = initialText
        textView.font = NSFont.systemFont(ofSize: 13)
        textView.textColor = PBDarkTheme.textPrimary
        textView.backgroundColor = PBDarkTheme.fieldBackground
        textView.drawsBackground = true
        textView.insertionPointColor = PBDarkTheme.textPrimary
        textView.autoresizingMask = [.width]
        scroll.documentView = textView

        alwaysOnCheckbox.state = initialAlwaysOn ? .on : .off
        alwaysOnCheckbox.contentTintColor = PBDarkTheme.textPrimary
        alwaysOnCheckbox.attributedTitle = NSAttributedString(
            string: "Immer mitschicken (Always-On Prefix)",
            attributes: [.foregroundColor: PBDarkTheme.textPrimary])

        let ok = PBDarkTheme.makePrimaryButton(title: "Speichern", target: self, action: #selector(save))
        ok.keyEquivalent = "\r"
        let cancel = PBDarkTheme.makeSecondaryButton(title: "Abbrechen", target: self, action: #selector(self.cancel))

        let buttonRow = NSStackView(views: [cancel, ok])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8
        buttonRow.alignment = .centerY

        let labelTop = NSTextField(labelWithString: "Kurzbezeichnung:")
        PBDarkTheme.styleLabel(labelTop)
        let textTop = NSTextField(labelWithString: "Prompt-Text:")
        PBDarkTheme.styleLabel(textTop)

        let stack = NSStackView(views: [labelTop, labelField, textTop, scroll, alwaysOnCheckbox, buttonRow])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 6
        stack.translatesAutoresizingMaskIntoConstraints = false
        window.contentView?.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: window.contentView!.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: window.contentView!.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: window.contentView!.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: window.contentView!.bottomAnchor, constant: -16),
            labelField.widthAnchor.constraint(equalTo: stack.widthAnchor),
            scroll.heightAnchor.constraint(equalToConstant: 260),
            scroll.widthAnchor.constraint(equalTo: stack.widthAnchor),
        ])
        // Becoming the window's delegate is critical: without it, closing
        // the window via the red title-bar button does NOT call stopModal,
        // leaving the modal session alive forever (app starts beeping on
        // every subsequent click because there's no visible modal window
        // left to absorb them).
        window.delegate = self
    }

    required init?(coder: NSCoder) { fatalError() }

    // Called when the window closes via any path — red title-bar X, save,
    // cancel, Cmd+W. Guarantees that the modal session actually ends.
    func windowWillClose(_ notification: Notification) {
        NSApp.stopModal(withCode: result != nil ? .OK : .cancel)
    }

    @objc private func save() {
        let label = labelField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !label.isEmpty else { return }
        result = PBPromptEditResult(
            shortLabel: label,
            originalText: textView.string,
            isAlwaysOn: alwaysOnCheckbox.state == .on)
        window?.close()    // windowWillClose handles stopModal
    }

    @objc private func cancel() {
        result = nil
        window?.close()    // windowWillClose handles stopModal
    }

    static func ask(parent: NSWindow?, title: String,
                    label: String, text: String, alwaysOn: Bool) -> PBPromptEditResult? {
        let ctrl = PBPromptEditDialog(title: title, initialLabel: label,
                                      initialText: text, initialAlwaysOn: alwaysOn)
        guard let w = ctrl.window else { return nil }
        w.center()
        return PBModalPresenter.runHidingFloatingPanels {
            w.makeKeyAndOrderFront(nil)
            w.orderFrontRegardless()
            tvoDebug("[Dlg] PromptEdit open frame=\(w.frame) visible=\(w.isVisible)")
            NSApp.runModal(for: w)
            tvoDebug("[Dlg] PromptEdit closed result=\(ctrl.result != nil ? "OK" : "cancel")")
            return ctrl.result
        }
    }
}

// MARK: - Settings

struct PBSettingsResult {
    let groqApiKey: String?
    let geminiApiKey: String?
    let separatorTemplate: String
    let googleClientId: String?
    let googleClientSecret: String?
}

final class PBSettingsDialog: NSWindowController, NSWindowDelegate {

    private let groqField = NSTextField()
    private let geminiField = NSTextField()
    private let separatorField = NSTextField()
    private let clientIdField = NSTextField()
    private let clientSecretField = NSTextField()
    private let statusLabel = NSTextField(labelWithString: "nicht verbunden")
    private var connectButton: NSButton!
    private var disconnectButton: NSButton!

    private var settings: PBAppSettings
    private var result: PBSettingsResult?

    init(settings: PBAppSettings) {
        self.settings = settings
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 620, height: 620),
            styleMask: [.titled, .closable],
            backing: .buffered, defer: false)
        window.title = "Einstellungen"
        window.isReleasedWhenClosed = false
        // Keep Settings above the .floating overlay/panel. No demotion of
        // other panels required — that approach was fragile (stuck level
        // if runModal didn't exit cleanly = beeping app).
        window.level = .modalPanel
        PBDarkTheme.apply(to: window)
        super.init(window: window)

        for field in [groqField, geminiField, separatorField, clientIdField, clientSecretField] {
            PBDarkTheme.styleField(field)
        }

        groqField.stringValue = settings.groqApiKey ?? ""
        geminiField.stringValue = settings.geminiApiKey ?? ""
        separatorField.stringValue = settings.separatorTemplate
        clientIdField.stringValue = settings.googleClientId ?? ""
        clientSecretField.stringValue = settings.googleClientSecret ?? ""

        // Themed buttons kept as instance variables so connectGoogle() can
        // still flip their enabled state / title mid-OAuth flow.
        connectButton = PBDarkTheme.makeSecondaryButton(title: "Verbinden", target: self, action: #selector(connectGoogle))
        disconnectButton = PBDarkTheme.makeSecondaryButton(title: "Trennen", target: self, action: #selector(disconnectGoogle))
        let ok = PBDarkTheme.makePrimaryButton(title: "Speichern", target: self, action: #selector(save))
        ok.keyEquivalent = "\r"
        let cancel = PBDarkTheme.makeSecondaryButton(title: "Abbrechen", target: self, action: #selector(cancelDlg))

        let connectRow = NSStackView(views: [connectButton, disconnectButton])
        connectRow.orientation = .horizontal
        connectRow.spacing = 8

        let buttonRow = NSStackView(views: [cancel, ok])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8

        func label(_ text: String) -> NSTextField {
            let l = NSTextField(labelWithString: text)
            PBDarkTheme.styleLabel(l)
            return l
        }

        PBDarkTheme.styleLabel(statusLabel)
        updateStatus()

        let stack = NSStackView(views: [
            label("Groq API Key (Whisper-Transkription)"),
            groqField,
            label("Gemini API Key (optional)"),
            geminiField,
            label("Separator-Template"),
            separatorField,
            label("Google Drive Backup"),
            label("Client ID"),
            clientIdField,
            label("Client Secret"),
            clientSecretField,
            statusLabel,
            connectRow,
            buttonRow,
        ])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 6
        stack.translatesAutoresizingMaskIntoConstraints = false
        window.contentView?.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: window.contentView!.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: window.contentView!.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: window.contentView!.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: window.contentView!.bottomAnchor, constant: -16),
            groqField.widthAnchor.constraint(equalTo: stack.widthAnchor),
            geminiField.widthAnchor.constraint(equalTo: stack.widthAnchor),
            separatorField.widthAnchor.constraint(equalTo: stack.widthAnchor),
            clientIdField.widthAnchor.constraint(equalTo: stack.widthAnchor),
            clientSecretField.widthAnchor.constraint(equalTo: stack.widthAnchor),
        ])

        window.delegate = self
    }

    required init?(coder: NSCoder) { fatalError() }

    private func updateStatus() {
        if let token = settings.googleOAuthRefreshToken, !token.isEmpty {
            let email = settings.googleAccountEmail ?? ""
            statusLabel.stringValue = email.isEmpty ? "verbunden" : "verbunden (\(email))"
            statusLabel.textColor = NSColor.systemGreen
        } else {
            statusLabel.stringValue = "nicht verbunden"
            statusLabel.textColor = NSColor.systemRed
        }
    }

    @objc private func save() {
        result = PBSettingsResult(
            groqApiKey: nullIfBlank(groqField.stringValue),
            geminiApiKey: nullIfBlank(geminiField.stringValue),
            separatorTemplate: separatorField.stringValue.isEmpty ? " ; " : separatorField.stringValue,
            googleClientId: nullIfBlank(clientIdField.stringValue),
            googleClientSecret: nullIfBlank(clientSecretField.stringValue))
        window?.close()    // windowWillClose handles stopModal (no double-stop)
    }

    @objc private func cancelDlg() {
        result = nil
        window?.close()    // windowWillClose handles stopModal
    }

    @objc private func connectGoogle() {
        let id = nullIfBlank(clientIdField.stringValue)
        let secret = nullIfBlank(clientSecretField.stringValue)
        guard let id = id, let secret = secret else {
            NSAlert.warn("Bitte erst Client ID und Client Secret eintragen.")
            return
        }

        // Persist credentials before starting the flow.
        settings.googleClientId = id
        settings.googleClientSecret = secret
        try? PromptBoardStore.shared.updateSettings(settings)

        connectButton.isEnabled = false
        setButtonTitle(connectButton, "Oeffne Browser...")

        GoogleDriveBackupService.shared.connect(clientId: id, clientSecret: secret) { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.connectButton.isEnabled = true
                self.setButtonTitle(self.connectButton, "Verbinden")
                switch result {
                case .success(let info):
                    self.settings.googleOAuthRefreshToken = info.refreshToken
                    self.settings.googleAccountEmail = info.email
                    try? PromptBoardStore.shared.updateSettings(self.settings)
                    self.updateStatus()
                case .failure(let err):
                    NSAlert.warn("Google-Login fehlgeschlagen: \(err.localizedDescription)")
                }
            }
        }
    }

    @objc private func disconnectGoogle() {
        settings.googleOAuthRefreshToken = nil
        settings.googleAccountEmail = nil
        try? PromptBoardStore.shared.updateSettings(settings)
        updateStatus()
    }

    func windowWillClose(_ notification: Notification) {
        NSApp.stopModal(withCode: result != nil ? .OK : .cancel)
    }

    static func ask(parent: NSWindow?, settings: PBAppSettings) -> PBSettingsResult? {
        let dlg = PBSettingsDialog(settings: settings)
        guard let w = dlg.window else { return nil }
        w.center()
        return PBModalPresenter.runHidingFloatingPanels {
            w.makeKeyAndOrderFront(nil)
            w.orderFrontRegardless()
            tvoDebug("[Dlg] Settings open frame=\(w.frame) visible=\(w.isVisible)")
            NSApp.runModal(for: w)
            tvoDebug("[Dlg] Settings closed result=\(dlg.result != nil ? "OK" : "cancel")")
            return dlg.result
        }
    }

    private func nullIfBlank(_ s: String) -> String? {
        let t = s.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }

    /// Update the visible title of a themed button (which uses attributedTitle).
    private func setButtonTitle(_ btn: NSButton, _ title: String) {
        btn.attributedTitle = NSAttributedString(
            string: title,
            attributes: [
                .foregroundColor: NSColor.white,
                .font: NSFont.systemFont(ofSize: 13, weight: .medium)
            ])
    }
}

extension NSAlert {
    static func warn(_ message: String) {
        let a = NSAlert()
        a.messageText = "PromptBoard"
        a.informativeText = message
        PBDarkTheme.apply(to: a)
        _ = PBModalPresenter.runModal(on: a)
    }
}
