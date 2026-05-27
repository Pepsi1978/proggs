import AppKit

// MARK: - Settings-Dialog
// Portierung von TerminalVoiceOverlay-Windows/Views/SettingsDialog.xaml
// (620×640px laut PORTING-INVENTORY.md). Skelett mit funktionierenden
// Feldern — pixelgenaue 1:1-Treue zur Windows-Optik kommt in einer
// Verfeinerungs-Etappe.

final class SettingsDialog: NSWindow {

    // MARK: - Felder
    private let groqKeyField     = NSTextField()
    private let geminiKeyField   = NSSecureTextField()
    private let separatorField   = NSTextField()
    private let autoHideCheck    = NSButton(checkboxWithTitle: "Auto-Hide (Collapsed-Mic)", target: nil, action: nil)
    private let horizontalCheck  = NSButton(checkboxWithTitle: "Horizontal-Orientierung", target: nil, action: nil)
    private let persistPosCheck  = NSButton(checkboxWithTitle: "Position ueber Neustart merken", target: nil, action: nil)
    private let googleClientId   = NSTextField()
    private let googleSecret     = NSSecureTextField()
    private let googleStatusLabel = NSTextField(labelWithString: "")

    // MARK: - Callbacks
    /// Wird gerufen wenn der Benutzer "Speichern" klickt. Argumente sind
    /// die finalen Werte aller Felder.
    var onSave: ((SettingsValues) -> Void)?
    var onGoogleConnect:    (() -> Void)?
    var onGoogleDisconnect: (() -> Void)?

    struct SettingsValues {
        let groqApiKey: String
        let geminiApiKey: String
        let separator: String
        let autoHide: Bool
        let horizontalOrientation: Bool
        let persistOverlayPosition: Bool
        let googleClientId: String
        let googleClientSecret: String
    }

    // MARK: - Setup

    init() {
        let frame = NSRect(x: 0, y: 0, width: 620, height: 640)
        super.init(contentRect: frame,
                   styleMask: [.titled, .closable],
                   backing: .buffered,
                   defer: false)
        self.title = "Einstellungen"
        self.level = .modalPanel
        self.isMovableByWindowBackground = true
        self.contentView?.wantsLayer = true
        self.contentView?.layer?.backgroundColor = NSColor(red: 0.118, green: 0.118, blue: 0.118, alpha: 1).cgColor

        buildLayout()
    }

    /// Werte aus UserDefaults vorbefuellen.
    func loadFromDefaults() {
        let d = UserDefaults.standard
        groqKeyField.stringValue    = d.string(forKey: "GROQ_API_KEY") ?? ""
        geminiKeyField.stringValue  = d.string(forKey: "GEMINI_API_KEY") ?? ""
        separatorField.stringValue  = d.string(forKey: "SEPARATOR_TEMPLATE") ?? "\n\n;\n\n"
        autoHideCheck.state         = d.bool(forKey: "AUTO_HIDE") ? .on : .off
        horizontalCheck.state       = d.bool(forKey: "HORIZONTAL_ORIENTATION") ? .on : .off
        persistPosCheck.state       = d.bool(forKey: "PERSIST_OVERLAY_POSITION") ? .on : .off
        googleClientId.stringValue  = d.string(forKey: "GOOGLE_CLIENT_ID") ?? ""
        googleSecret.stringValue    = d.string(forKey: "GOOGLE_CLIENT_SECRET") ?? ""
    }

    /// Status-Anzeige fuer Google-Drive-Verbindung (rot/gruen).
    func setGoogleStatus(_ text: String, connected: Bool) {
        googleStatusLabel.stringValue = text
        googleStatusLabel.textColor = connected
            ? NSColor(red: 0.27, green: 0.71, blue: 0.33, alpha: 1)
            : NSColor(red: 0.83, green: 0.18, blue: 0.18, alpha: 1)
    }

    // MARK: - Layout

    private func buildLayout() {
        guard let cv = self.contentView else { return }
        let pad: CGFloat = 20
        var y: CGFloat = 600

        func addLabel(_ text: String, atY: CGFloat) {
            let lbl = NSTextField(labelWithString: text)
            lbl.frame = NSRect(x: pad, y: atY, width: 580, height: 18)
            lbl.textColor = NSColor.white
            lbl.font = .systemFont(ofSize: 12, weight: .semibold)
            cv.addSubview(lbl)
        }

        func addTextField(_ tf: NSTextField, atY: CGFloat, height: CGFloat = 24) {
            tf.frame = NSRect(x: pad, y: atY, width: 580, height: height)
            tf.bezelStyle = .roundedBezel
            tf.font = .systemFont(ofSize: 13)
            cv.addSubview(tf)
        }

        addLabel("Groq API Key (Pflicht)", atY: y); y -= 24
        addTextField(groqKeyField, atY: y); y -= 36

        addLabel("Gemini API Key (optional)", atY: y); y -= 24
        addTextField(geminiKeyField, atY: y); y -= 36

        addLabel("Trenn-Template (zwischen AlwaysOn-Prompts)", atY: y); y -= 24
        addTextField(separatorField, atY: y); y -= 40

        // Checkboxen
        autoHideCheck.frame    = NSRect(x: pad, y: y, width: 580, height: 22); y -= 28
        horizontalCheck.frame  = NSRect(x: pad, y: y, width: 580, height: 22); y -= 28
        persistPosCheck.frame  = NSRect(x: pad, y: y, width: 580, height: 22); y -= 36
        for cb in [autoHideCheck, horizontalCheck, persistPosCheck] {
            (cb.cell as? NSButtonCell)?.backgroundColor = .clear
            cb.contentTintColor = NSColor.white
            cv.addSubview(cb)
        }

        // Google-Drive-Sektion
        addLabel("Google Drive Backup (Client-ID + Secret)", atY: y); y -= 24
        addTextField(googleClientId, atY: y); y -= 32
        addTextField(googleSecret, atY: y); y -= 36

        // Connect / Disconnect Buttons
        let btnConnect = NSButton(title: "Verbinden",
                                  target: self, action: #selector(connectClicked))
        btnConnect.bezelStyle = .rounded
        btnConnect.frame = NSRect(x: pad, y: y, width: 120, height: 28); y -= 0
        cv.addSubview(btnConnect)

        let btnDisconnect = NSButton(title: "Trennen",
                                     target: self, action: #selector(disconnectClicked))
        btnDisconnect.bezelStyle = .rounded
        btnDisconnect.frame = NSRect(x: pad + 130, y: y, width: 120, height: 28); y -= 32
        cv.addSubview(btnDisconnect)

        googleStatusLabel.frame = NSRect(x: pad, y: y, width: 580, height: 18); y -= 32
        googleStatusLabel.font = .systemFont(ofSize: 12)
        cv.addSubview(googleStatusLabel)

        // OK / Cancel
        let btnCancel = NSButton(title: "Abbrechen",
                                 target: self, action: #selector(cancelClicked))
        btnCancel.bezelStyle = .rounded
        btnCancel.frame = NSRect(x: 620 - pad - 240, y: 20, width: 110, height: 32)
        cv.addSubview(btnCancel)

        let btnSave = NSButton(title: "Speichern",
                               target: self, action: #selector(saveClicked))
        btnSave.bezelStyle = .rounded
        btnSave.keyEquivalent = "\r"  // Enter = Speichern
        btnSave.frame = NSRect(x: 620 - pad - 120, y: 20, width: 110, height: 32)
        cv.addSubview(btnSave)
    }

    // MARK: - Actions

    @objc private func saveClicked() {
        let values = SettingsValues(
            groqApiKey: groqKeyField.stringValue,
            geminiApiKey: geminiKeyField.stringValue,
            separator: separatorField.stringValue,
            autoHide: autoHideCheck.state == .on,
            horizontalOrientation: horizontalCheck.state == .on,
            persistOverlayPosition: persistPosCheck.state == .on,
            googleClientId: googleClientId.stringValue,
            googleClientSecret: googleSecret.stringValue
        )
        // Direkt in UserDefaults persistieren — der Aufrufer kann via onSave
        // zusaetzlich auf die Felder reagieren.
        let d = UserDefaults.standard
        d.set(values.groqApiKey,             forKey: "GROQ_API_KEY")
        d.set(values.geminiApiKey,           forKey: "GEMINI_API_KEY")
        d.set(values.separator,              forKey: "SEPARATOR_TEMPLATE")
        d.set(values.autoHide,               forKey: "AUTO_HIDE")
        d.set(values.horizontalOrientation,  forKey: "HORIZONTAL_ORIENTATION")
        d.set(values.persistOverlayPosition, forKey: "PERSIST_OVERLAY_POSITION")
        d.set(values.googleClientId,         forKey: "GOOGLE_CLIENT_ID")
        d.set(values.googleClientSecret,     forKey: "GOOGLE_CLIENT_SECRET")
        onSave?(values)
        self.close()
    }

    @objc private func cancelClicked() {
        self.close()
    }

    @objc private func connectClicked() {
        onGoogleConnect?()
    }

    @objc private func disconnectClicked() {
        onGoogleDisconnect?()
    }
}
