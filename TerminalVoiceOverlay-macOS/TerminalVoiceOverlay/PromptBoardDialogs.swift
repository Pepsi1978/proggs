import AppKit

/// Minimal AppKit dialogs used by the PromptBoard panel. Kept in one
/// file because each dialog is only a handful of lines in AppKit — no
/// need for separate XIBs.

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
        let response = alert.runModal()

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
        let response = alert.runModal()
        return response == .alertFirstButtonReturn
    }
}

// MARK: - Prompt editor

struct PBPromptEditResult {
    let shortLabel: String
    let originalText: String
    let isAlwaysOn: Bool
}

final class PBPromptEditDialog: NSWindowController {
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
    }

    required init?(coder: NSCoder) { fatalError() }

    @objc private func save() {
        let label = labelField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !label.isEmpty else { return }
        result = PBPromptEditResult(
            shortLabel: label,
            originalText: textView.string,
            isAlwaysOn: alwaysOnCheckbox.state == .on)
        NSApp.stopModal(withCode: .OK)
        window?.close()
    }

    @objc private func cancel() {
        result = nil
        NSApp.stopModal(withCode: .cancel)
        window?.close()
    }

    static func ask(parent: NSWindow?, title: String,
                    label: String, text: String, alwaysOn: Bool) -> PBPromptEditResult? {
        let ctrl = PBPromptEditDialog(title: title, initialLabel: label,
                                      initialText: text, initialAlwaysOn: alwaysOn)
        ctrl.window?.center()
        NSApp.runModal(for: ctrl.window!)
        return ctrl.result
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
        NSApp.stopModal(withCode: .OK)
        window?.close()
    }

    @objc private func cancelDlg() {
        result = nil
        NSApp.stopModal(withCode: .cancel)
        window?.close()
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
        dlg.window?.center()
        NSApp.runModal(for: dlg.window!)
        return dlg.result
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
        a.runModal()
    }
}
