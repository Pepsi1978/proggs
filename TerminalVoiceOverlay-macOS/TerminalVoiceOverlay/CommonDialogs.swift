import AppKit

// MARK: - Common Dialogs (Skelette)
// Portierungen der vier kleinen Sub-Dialoge aus
// TerminalVoiceOverlay-Windows/Views/. Pixelgenaue 1:1-Treue zur
// Windows-Optik wird in einer Verfeinerungs-Etappe nachgezogen.
//
//   ConfirmDialog            — generischer Loeschen-Confirm
//   TextInputDialog          — einzeilige Texteingabe (z.B. Kategoriename)
//   PromptEditDialog         — Prompt anlegen/bearbeiten
//   PromptHistoryEditDialog  — Historie-Eintrag nachbearbeiten

// MARK: - ConfirmDialog

final class ConfirmDialog: NSWindow {
    var onOk: (() -> Void)?
    var onCancel: (() -> Void)?

    init(title: String, message: String,
         okLabel: String = "Loeschen", cancelLabel: String = "Abbrechen") {
        let frame = NSRect(x: 0, y: 0, width: 420, height: 180)
        super.init(contentRect: frame,
                   styleMask: [.titled, .closable],
                   backing: .buffered,
                   defer: false)
        self.title = title
        self.level = .modalPanel
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.backgroundColor =
            NSColor(red: 0.118, green: 0.118, blue: 0.118, alpha: 1).cgColor
        buildLayout(message: message, okLabel: okLabel, cancelLabel: cancelLabel)
    }

    private func buildLayout(message: String, okLabel: String, cancelLabel: String) {
        guard let cv = self.contentView else { return }
        let lbl = NSTextField(wrappingLabelWithString: message)
        lbl.frame = NSRect(x: 20, y: 70, width: 380, height: 80)
        lbl.textColor = .white
        lbl.font = .systemFont(ofSize: 13)
        cv.addSubview(lbl)

        let cancel = NSButton(title: cancelLabel, target: self, action: #selector(cancelClicked))
        cancel.bezelStyle = .rounded
        cancel.frame = NSRect(x: 420 - 250, y: 20, width: 110, height: 32)
        cv.addSubview(cancel)

        let ok = NSButton(title: okLabel, target: self, action: #selector(okClicked))
        ok.bezelStyle = .rounded
        ok.keyEquivalent = "\r"
        ok.frame = NSRect(x: 420 - 130, y: 20, width: 110, height: 32)
        // Roter Akzent fuer "Loeschen"
        if okLabel.lowercased().contains("loesch") {
            ok.contentTintColor = NSColor(red: 0.90, green: 0.22, blue: 0.22, alpha: 1)
        }
        cv.addSubview(ok)
    }

    @objc private func okClicked()     { onOk?();     close() }
    @objc private func cancelClicked() { onCancel?(); close() }
}

// MARK: - TextInputDialog

final class TextInputDialog: NSWindow {
    private let inputField = NSTextField()
    var onOk: ((String) -> Void)?
    var onCancel: (() -> Void)?

    init(title: String, label: String = "Name:", initialValue: String = "") {
        let frame = NSRect(x: 0, y: 0, width: 440, height: 180)
        super.init(contentRect: frame,
                   styleMask: [.titled, .closable],
                   backing: .buffered,
                   defer: false)
        self.title = title
        self.level = .modalPanel
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.backgroundColor =
            NSColor(red: 0.118, green: 0.118, blue: 0.118, alpha: 1).cgColor
        inputField.stringValue = initialValue
        buildLayout(label: label)
    }

    private func buildLayout(label: String) {
        guard let cv = self.contentView else { return }
        let lbl = NSTextField(labelWithString: label)
        lbl.textColor = .white
        lbl.frame = NSRect(x: 20, y: 110, width: 400, height: 20)
        cv.addSubview(lbl)

        inputField.frame = NSRect(x: 20, y: 70, width: 400, height: 28)
        inputField.bezelStyle = .roundedBezel
        inputField.font = .systemFont(ofSize: 14)
        cv.addSubview(inputField)

        let cancel = NSButton(title: "Abbrechen", target: self, action: #selector(cancelClicked))
        cancel.bezelStyle = .rounded
        cancel.frame = NSRect(x: 440 - 250, y: 20, width: 110, height: 32)
        cv.addSubview(cancel)

        let ok = NSButton(title: "OK", target: self, action: #selector(okClicked))
        ok.bezelStyle = .rounded
        ok.keyEquivalent = "\r"
        ok.frame = NSRect(x: 440 - 130, y: 20, width: 110, height: 32)
        cv.addSubview(ok)
    }

    @objc private func okClicked() {
        onOk?(inputField.stringValue)
        close()
    }
    @objc private func cancelClicked() {
        onCancel?()
        close()
    }
}

// MARK: - PromptEditDialog (Skelett)

final class PromptEditDialog: NSWindow {
    private let shortLabelField = NSTextField()
    private let originalTextView = NSTextView()
    private let alwaysOnCheck   = NSButton(checkboxWithTitle: "Immer mitschicken (AlwaysOn)", target: nil, action: nil)
    private let prePromptCheck  = NSButton(checkboxWithTitle: "Pre-Prompt (vor Diktat)",       target: nil, action: nil)
    private let postPromptCheck = NSButton(checkboxWithTitle: "Post-Prompt (nach Diktat)",     target: nil, action: nil)

    struct EditResult {
        let shortLabel: String
        let originalText: String
        let isAlwaysOn: Bool
        let isPrePrompt: Bool
        let isPostPrompt: Bool
    }
    var onSave: ((EditResult) -> Void)?
    var onCancel: (() -> Void)?

    init(initialShortLabel: String = "",
         initialOriginalText: String = "",
         initialAlwaysOn: Bool = false,
         initialPrePrompt: Bool = false,
         initialPostPrompt: Bool = false) {
        let frame = NSRect(x: 0, y: 0, width: 560, height: 780)
        super.init(contentRect: frame,
                   styleMask: [.titled, .closable, .resizable],
                   backing: .buffered,
                   defer: false)
        self.title = "Prompt bearbeiten"
        self.level = .modalPanel
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.backgroundColor =
            NSColor(red: 0.118, green: 0.118, blue: 0.118, alpha: 1).cgColor

        shortLabelField.stringValue = initialShortLabel
        originalTextView.string     = initialOriginalText
        alwaysOnCheck.state    = initialAlwaysOn   ? .on : .off
        prePromptCheck.state   = initialPrePrompt  ? .on : .off
        postPromptCheck.state  = initialPostPrompt ? .on : .off

        buildLayout()
    }

    private func buildLayout() {
        guard let cv = self.contentView else { return }
        let pad: CGFloat = 20
        var y: CGFloat = 720

        let lblShort = NSTextField(labelWithString: "Kurzbezeichnung")
        lblShort.textColor = .white
        lblShort.frame = NSRect(x: pad, y: y, width: 520, height: 18); y -= 24
        cv.addSubview(lblShort)
        shortLabelField.bezelStyle = .roundedBezel
        shortLabelField.font = .systemFont(ofSize: 13)
        shortLabelField.frame = NSRect(x: pad, y: y, width: 520, height: 28); y -= 38
        cv.addSubview(shortLabelField)

        let lblOriginal = NSTextField(labelWithString: "Originaltext")
        lblOriginal.textColor = .white
        lblOriginal.frame = NSRect(x: pad, y: y, width: 520, height: 18); y -= 24
        cv.addSubview(lblOriginal)

        let scroll = NSScrollView(frame: NSRect(x: pad, y: y - 380, width: 520, height: 380))
        scroll.hasVerticalScroller = true
        scroll.borderType = .bezelBorder
        originalTextView.font = .systemFont(ofSize: 14)
        originalTextView.isEditable = true
        originalTextView.frame = scroll.bounds
        originalTextView.autoresizingMask = [.width, .height]
        scroll.documentView = originalTextView
        cv.addSubview(scroll)
        y -= 392

        for cb in [alwaysOnCheck, prePromptCheck, postPromptCheck] {
            cb.contentTintColor = NSColor.white
            cb.frame = NSRect(x: pad, y: y, width: 520, height: 22)
            cv.addSubview(cb)
            y -= 28
        }

        let cancel = NSButton(title: "Abbrechen", target: self, action: #selector(cancelClicked))
        cancel.bezelStyle = .rounded
        cancel.frame = NSRect(x: 560 - pad - 240, y: 20, width: 110, height: 32)
        cv.addSubview(cancel)

        let save = NSButton(title: "Speichern", target: self, action: #selector(saveClicked))
        save.bezelStyle = .rounded
        save.keyEquivalent = "\r"
        save.frame = NSRect(x: 560 - pad - 120, y: 20, width: 110, height: 32)
        cv.addSubview(save)
    }

    @objc private func saveClicked() {
        let r = EditResult(
            shortLabel: shortLabelField.stringValue,
            originalText: originalTextView.string,
            isAlwaysOn:   alwaysOnCheck.state == .on,
            isPrePrompt:  prePromptCheck.state == .on,
            isPostPrompt: postPromptCheck.state == .on
        )
        onSave?(r)
        close()
    }
    @objc private func cancelClicked() {
        onCancel?()
        close()
    }
}

// MARK: - PromptHistoryEditDialog (Skelett)

final class PromptHistoryEditDialog: NSWindow {
    private let metaLabel = NSTextField(labelWithString: "")
    private let editView = NSTextView()
    var onSave: ((String) -> Void)?
    var onCancel: (() -> Void)?

    init(metadata: String, initialText: String) {
        let frame = NSRect(x: 0, y: 0, width: 720, height: 520)
        super.init(contentRect: frame,
                   styleMask: [.titled, .closable, .resizable],
                   backing: .buffered,
                   defer: false)
        self.title = "Historie bearbeiten"
        self.level = .modalPanel
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.backgroundColor =
            NSColor(red: 0.118, green: 0.118, blue: 0.118, alpha: 1).cgColor

        metaLabel.stringValue = metadata
        editView.string = initialText
        buildLayout()
    }

    private func buildLayout() {
        guard let cv = self.contentView else { return }
        let pad: CGFloat = 20

        metaLabel.frame = NSRect(x: pad, y: 480, width: 680, height: 18)
        metaLabel.textColor = NSColor(white: 0.6, alpha: 1)
        metaLabel.font = .systemFont(ofSize: 11)
        cv.addSubview(metaLabel)

        let scroll = NSScrollView(frame: NSRect(x: pad, y: 80, width: 680, height: 390))
        scroll.hasVerticalScroller = true
        scroll.borderType = .bezelBorder
        editView.font = .systemFont(ofSize: 14)
        editView.isEditable = true
        // Goldener Cursor (analog Windows CaretBrush #FFD700)
        editView.insertionPointColor = NSColor(red: 1.0, green: 0.84, blue: 0.0, alpha: 1)
        editView.frame = scroll.bounds
        editView.autoresizingMask = [.width, .height]
        scroll.documentView = editView
        cv.addSubview(scroll)

        let cancel = NSButton(title: "Abbrechen", target: self, action: #selector(cancelClicked))
        cancel.bezelStyle = .rounded
        cancel.frame = NSRect(x: 720 - pad - 240, y: 20, width: 110, height: 32)
        cv.addSubview(cancel)

        let save = NSButton(title: "Speichern", target: self, action: #selector(saveClicked))
        save.bezelStyle = .rounded
        save.keyEquivalent = "\r"
        save.contentTintColor = NSColor(red: 0.72, green: 0.52, blue: 0.04, alpha: 1)
        save.frame = NSRect(x: 720 - pad - 120, y: 20, width: 110, height: 32)
        cv.addSubview(save)
    }

    @objc private func saveClicked() {
        onSave?(editView.string)
        close()
    }
    @objc private func cancelClicked() {
        onCancel?()
        close()
    }
}
