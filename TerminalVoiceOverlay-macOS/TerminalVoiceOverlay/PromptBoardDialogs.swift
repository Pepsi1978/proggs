import AppKit

/// Minimal AppKit dialogs used by the PromptBoard panel. Kept in one
/// file because each dialog is only a handful of lines in AppKit — no
/// need for separate XIBs.

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

        let field = NSTextField(frame: NSRect(x: 0, y: 0, width: 320, height: 22))
        field.stringValue = initialValue
        alert.accessoryView = field
        alert.window.initialFirstResponder = field

        let response = parent != nil
            ? alert.runSheetModal(for: parent!)
            : alert.runModal()

        guard response == .alertFirstButtonReturn else { return nil }
        let v = field.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
        return v.isEmpty ? nil : v
    }
}

extension NSAlert {
    fileprivate func runSheetModal(for window: NSWindow) -> NSApplication.ModalResponse {
        var response: NSApplication.ModalResponse = .abort
        beginSheetModal(for: window) { r in
            response = r
            NSApp.stopModal(withCode: r)
        }
        return NSApp.runModal(for: window)
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
        let response = parent != nil
            ? alert.runSheetModal(for: parent!)
            : alert.runModal()
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
        super.init(window: window)

        labelField.placeholderString = "Kurzbezeichnung"
        labelField.stringValue = initialLabel

        let scroll = NSScrollView()
        scroll.hasVerticalScroller = true
        scroll.borderType = .bezelBorder
        textView.isEditable = true
        textView.isRichText = false
        textView.string = initialText
        textView.font = NSFont.systemFont(ofSize: 13)
        textView.autoresizingMask = [.width]
        scroll.documentView = textView

        alwaysOnCheckbox.state = initialAlwaysOn ? .on : .off

        let ok = NSButton(title: "Speichern", target: self, action: #selector(save))
        ok.bezelStyle = .rounded
        ok.keyEquivalent = "\r"
        let cancel = NSButton(title: "Abbrechen", target: self, action: #selector(cancel))
        cancel.bezelStyle = .rounded

        let buttonRow = NSStackView(views: [cancel, ok])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8
        buttonRow.alignment = .centerY

        let labelTop = NSTextField(labelWithString: "Kurzbezeichnung:")
        let textTop = NSTextField(labelWithString: "Prompt-Text:")

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
    private let connectButton = NSButton(title: "Verbinden", target: nil, action: nil)
    private let disconnectButton = NSButton(title: "Trennen", target: nil, action: nil)

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
        super.init(window: window)

        groqField.stringValue = settings.groqApiKey ?? ""
        geminiField.stringValue = settings.geminiApiKey ?? ""
        separatorField.stringValue = settings.separatorTemplate
        clientIdField.stringValue = settings.googleClientId ?? ""
        clientSecretField.stringValue = settings.googleClientSecret ?? ""
        updateStatus()

        connectButton.target = self
        connectButton.action = #selector(connectGoogle)
        disconnectButton.target = self
        disconnectButton.action = #selector(disconnectGoogle)

        let ok = NSButton(title: "Speichern", target: self, action: #selector(save))
        ok.bezelStyle = .rounded
        ok.keyEquivalent = "\r"
        let cancel = NSButton(title: "Abbrechen", target: self, action: #selector(cancelDlg))
        cancel.bezelStyle = .rounded

        let connectRow = NSStackView(views: [connectButton, disconnectButton])
        connectRow.orientation = .horizontal
        connectRow.spacing = 8

        let buttonRow = NSStackView(views: [cancel, ok])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8

        let stack = NSStackView(views: [
            NSTextField(labelWithString: "Groq API Key (Whisper-Transkription)"),
            groqField,
            NSTextField(labelWithString: "Gemini API Key (optional)"),
            geminiField,
            NSTextField(labelWithString: "Separator-Template"),
            separatorField,
            NSTextField(labelWithString: "Google Drive Backup"),
            NSTextField(labelWithString: "Client ID"),
            clientIdField,
            NSTextField(labelWithString: "Client Secret"),
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
        connectButton.title = "Oeffne Browser..."

        GoogleDriveBackupService.shared.connect(clientId: id, clientSecret: secret) { [weak self] result in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.connectButton.isEnabled = true
                self.connectButton.title = "Verbinden"
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
}

extension NSAlert {
    static func warn(_ message: String) {
        let a = NSAlert()
        a.messageText = "PromptBoard"
        a.informativeText = message
        a.runModal()
    }
}
