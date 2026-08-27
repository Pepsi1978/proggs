import AppKit

/// Minimal AppKit dialogs used by the PromptBoard panel. Kept in one
/// file because each dialog is only a handful of lines in AppKit — no
/// need for separate XIBs.

// MARK: - Main-thread dispatch that survives modal run loops

/// Runs `work` on the main thread in run-loop mode `.common`, which
/// includes both `.default` and `.modalPanel`. Plain `DispatchQueue.main.async`
/// posts to `.default` only — when one of our `NSApp.runModal(for:)` modal
/// sessions is active (PromptEditDialog, SettingsDialog, etc.) the modal
/// loop runs in `.modalPanel`, so async blocks queue up but never fire
/// until the modal returns. That made the Gemini "Verbessere mit Gemini..."
/// status hang forever even though the network response had already
/// arrived 4s in. See debug log entry from 2026-04-25 16:55–16:57: the
/// callback only fired once the user cancelled the dialog (74s later),
/// at which point `[weak self]` resolved to nil. Using `.common` makes
/// the callback fire as soon as the network response arrives, regardless
/// of whether a modal is up.
private func onMainCommon(_ work: @escaping () -> Void) {
    RunLoop.main.perform(inModes: [.common], block: work)
}

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

    /// Dunkles Styling fuer NSPopUpButton (Hotkey-Picker im Edit-Dialog).
    /// Behaelt die System-Pfeil-Cell aber faerbt Hintergrund und Text passend.
    static func stylePopup(_ popup: NSPopUpButton) {
        popup.contentTintColor = textPrimary
        popup.wantsLayer = true
        popup.layer?.backgroundColor = fieldBackground.cgColor
        popup.layer?.cornerRadius = 4
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
    let isPrePrompt: Bool
    let isPostPrompt: Bool
    /// Cmd+1..9 hotkey assignment, or nil for no hotkey.
    /// Empfaenger (PromptBoardPanel) muss `stripHotkeyFromOthers` aufrufen
    /// damit globale Eindeutigkeit erhalten bleibt ("last wins").
    let hotkeyNumber: Int?
    /// Cmd+Opt+A..Z hotkey assignment, or nil. Globally unique ("last wins").
    /// Pendant zu Windows Win+Alt+A..Z.
    let hotkeyLetter: String?
}

final class PBPromptEditDialog: NSWindowController, NSWindowDelegate {
    private let labelField = NSTextField()
    private let textView = NSTextView()
    private let alwaysOnCheckbox = NSButton(checkboxWithTitle: "Immer mitschicken (Always-On Prefix)", target: nil, action: nil)
    /// Sub-option of always-on: prepend this prompt before the dictated
    /// text. Independent of postPromptCheckbox — both can be on so the
    /// prompt appears front and back.
    private let prePromptCheckbox = NSButton(checkboxWithTitle: "Pre-Prompt (vor dem Diktat)", target: nil, action: nil)
    /// Sub-option of always-on: append this prompt after the dictated text.
    private let postPromptCheckbox = NSButton(checkboxWithTitle: "Post-Prompt (nach dem Diktat)", target: nil, action: nil)
    /// Cmd+1..9 hotkey picker. Index 0 = "Kein Hotkey", Indizes 1..9 = Cmd+1..9.
    private let hotkeyPopup = NSPopUpButton(frame: .zero, pullsDown: false)
    /// Cmd+Opt+A..Z Letter-Hotkey-Popup (Windows-Pendant: Win+Alt+A..Z)
    private let letterPopup = NSPopUpButton(frame: .zero, pullsDown: false)
    private let statusLabel = NSTextField(labelWithString: "")
    private var result: PBPromptEditResult?

    // ── Footer buttons (built in init, swapped at runtime) ──
    private var micButton: NSButton!
    /// True, solange eine Aufnahme laeuft, die DIESER Dialog gestartet hat.
    /// Der AudioRecorder ist prozessweit geteilt — ohne dieses Flag wuerde das
    /// Schliessen des Editors eine laufende OVERLAY-Aufnahme abwuergen und der
    /// gesprochene Text waere weg (Windows-Pendant: `_recordingStartedHere`).
    private var recordingStartedHere = false
    private var geminiButton: NSButton!
    private var saveButton: NSButton!           // single "Speichern" before improvement
    private var saveOriginalButton: NSButton!   // "Original speichern"  — visible after G
    private var saveImprovedButton: NSButton!   // "Verbessert speichern" — visible after G
    private var rightButtonContainer: NSStackView!  // holds either saveButton or the dual save stack

    /// Original prompt text BEFORE Gemini improvement, captured the moment the
    /// user clicks "G". Lets us offer "Original speichern" alongside
    /// "Verbessert speichern" without losing what was originally typed/dictated.
    private var originalBeforeImprove: String?
    private var isImproving = false

    /// Mic pulse: red while recording, amber while transcribing.
    private var pulseTimer: Timer?
    private var pulseBright = false

    /// True while the short-label field still holds an auto-generated value
    /// (after mic transcription or after Gemini improvement). The first
    /// moment the user manually types into the label field this flips to
    /// false — and from then on the auto-titler never overwrites the
    /// user's wording.
    private var labelIsAutoGenerated = false
    /// Re-entrancy guard while WE write into labelField — without it our
    /// own programmatic assignment would trip the manual-edit detector.
    private var suppressLabelChange = false
    private var labelEditObserver: NSObjectProtocol?

    init(title: String, initialLabel: String, initialText: String,
         initialAlwaysOn: Bool, initialPrePrompt: Bool, initialPostPrompt: Bool,
         initialHotkey: Int? = nil, initialHotkeyLetter: String? = nil) {
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 540, height: 580),
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

        prePromptCheckbox.state = initialPrePrompt ? .on : .off
        prePromptCheckbox.contentTintColor = PBDarkTheme.textPrimary
        prePromptCheckbox.attributedTitle = NSAttributedString(
            string: "Pre-Prompt (vor dem Diktat)",
            attributes: [.foregroundColor: PBDarkTheme.textPrimary])

        postPromptCheckbox.state = initialPostPrompt ? .on : .off
        postPromptCheckbox.contentTintColor = PBDarkTheme.textPrimary
        postPromptCheckbox.attributedTitle = NSAttributedString(
            string: "Post-Prompt (nach dem Diktat)",
            attributes: [.foregroundColor: PBDarkTheme.textPrimary])

        // ── Hotkey-Picker (Cmd+1..9) ──
        // Index 0 = "Kein Hotkey", 1..9 = "Cmd+N". Bei Auswahl wird im Save-
        // Pfad `stripHotkeyFromOthers` aufgerufen, damit ein Hotkey IMMER
        // global eindeutig ist ("last wins" — andere Prompts verlieren ihn
        // automatisch). Mirrors Windows PBPromptEditDialog.
        hotkeyPopup.removeAllItems()
        hotkeyPopup.addItem(withTitle: "Kein Hotkey")
        for n in 1...9 { hotkeyPopup.addItem(withTitle: "Cmd+\(n)") }
        if let hk = initialHotkey, hk >= 1, hk <= 9 {
            hotkeyPopup.selectItem(at: hk)
        } else {
            hotkeyPopup.selectItem(at: 0)
        }
        PBDarkTheme.stylePopup(hotkeyPopup)

        // Letter-Hotkey-Popup: "Kein", "A", "B", ..., "Z"
        letterPopup.removeAllItems()
        letterPopup.addItem(withTitle: "Kein")
        for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ" {
            letterPopup.addItem(withTitle: "Cmd+Opt+\(c)")
        }
        if let l = initialHotkeyLetter, let idx = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            .firstIndex(of: Character(l.uppercased())) {
            let pos = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".distance(
                from: "ABCDEFGHIJKLMNOPQRSTUVWXYZ".startIndex, to: idx)
            letterPopup.selectItem(at: pos + 1)
        } else {
            letterPopup.selectItem(at: 0)
        }
        PBDarkTheme.stylePopup(letterPopup)

        // ── Footer buttons ──
        let cancel = PBDarkTheme.makeSecondaryButton(title: "Abbrechen", target: self, action: #selector(self.cancel))

        micButton = makeRoundActionButton(symbol: "🎤",
            tooltip: "Aufnehmen — klicken zum Starten/Stoppen. Ergebnis wird in den Prompt-Text eingefuegt.",
            action: #selector(toggleRecording))
        geminiButton = makeRoundActionButton(symbol: "G",
            tooltip: "Mit Gemini verbessern — der aktuelle Text wird durch eine bereinigte Variante ersetzt. Original bleibt erhalten und kann separat gespeichert werden.",
            action: #selector(runGemini))

        saveButton = PBDarkTheme.makePrimaryButton(title: "Speichern",
            target: self, action: #selector(saveCurrent))
        saveButton.keyEquivalent = "\r"

        saveOriginalButton = PBDarkTheme.makeSecondaryButton(title: "Original speichern",
            target: self, action: #selector(saveOriginal))
        saveImprovedButton = PBDarkTheme.makePrimaryButton(title: "Verbessert speichern",
            target: self, action: #selector(saveImproved))
        saveImprovedButton.keyEquivalent = "\r"

        // Disable mic / G when their backing services aren't available.
        micButton.isEnabled = VoiceServiceProvider.recorderAvailable
        geminiButton.isEnabled = VoiceServiceProvider.geminiAvailable

        rightButtonContainer = NSStackView(views: [saveButton])
        rightButtonContainer.orientation = .vertical
        rightButtonContainer.alignment = .trailing
        rightButtonContainer.spacing = 6

        let centerStack = NSStackView(views: [micButton, geminiButton])
        centerStack.orientation = .horizontal
        centerStack.spacing = 8
        centerStack.alignment = .centerY

        // Footer layout: cancel (left) | mic+G (center) | save (right)
        // Spacers give the center group its centered position regardless
        // of how wide the side groups grow.
        let leftSpacer = NSView()
        let rightSpacer = NSView()
        let buttonRow = NSStackView(views: [cancel, leftSpacer, centerStack, rightSpacer, rightButtonContainer])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 0
        buttonRow.alignment = .centerY
        buttonRow.distribution = .fill
        leftSpacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        rightSpacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        leftSpacer.widthAnchor.constraint(equalTo: rightSpacer.widthAnchor).isActive = true

        // Status line for recording / Gemini progress.
        statusLabel.font = NSFont.systemFont(ofSize: 11)
        statusLabel.textColor = NSColor(calibratedWhite: 0.6, alpha: 1)
        statusLabel.lineBreakMode = .byTruncatingTail
        statusLabel.maximumNumberOfLines = 1

        let labelTop = NSTextField(labelWithString: "Kurzbezeichnung:")
        PBDarkTheme.styleLabel(labelTop)
        let textTop = NSTextField(labelWithString: "Prompt-Text:")
        PBDarkTheme.styleLabel(textTop)

        // Indented sub-stack for the Pre/Post checkboxes — visually
        // signals they're sub-options of the always-on toggle above.
        let subOptionStack = NSStackView(views: [prePromptCheckbox, postPromptCheckbox])
        subOptionStack.orientation = .vertical
        subOptionStack.alignment = .leading
        subOptionStack.spacing = 2
        subOptionStack.edgeInsets = NSEdgeInsets(top: 0, left: 22, bottom: 0, right: 0)

        // Hotkey-Reihe: Label "Hotkey:" + Popup-Picker.
        let hotkeyLabel = NSTextField(labelWithString: "Hotkey:")
        PBDarkTheme.styleLabel(hotkeyLabel)
        let hotkeyRow = NSStackView(views: [hotkeyLabel, hotkeyPopup, letterPopup])
        hotkeyRow.orientation = .horizontal
        hotkeyRow.spacing = 8
        hotkeyRow.alignment = .centerY

        let stack = NSStackView(views: [labelTop, labelField, textTop, scroll, alwaysOnCheckbox, subOptionStack, hotkeyRow, statusLabel, buttonRow])
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
            buttonRow.widthAnchor.constraint(equalTo: stack.widthAnchor),
            statusLabel.widthAnchor.constraint(equalTo: stack.widthAnchor),
        ])
        // Becoming the window's delegate is critical: without it, closing
        // the window via the red title-bar button does NOT call stopModal,
        // leaving the modal session alive forever (app starts beeping on
        // every subsequent click because there's no visible modal window
        // left to absorb them).
        window.delegate = self

        // Manual edits to the short-label field disarm the auto-titler.
        // After the very first user keystroke the label is treated as
        // user-owned and never overwritten by mic / Gemini-driven
        // re-titling. NSControl posts NSControl.textDidChangeNotification
        // when the user types in an NSTextField; programmatic assignments
        // do NOT fire it, but we still guard with suppressLabelChange so
        // future tooling (binding, etc.) can't accidentally disarm it.
        labelEditObserver = NotificationCenter.default.addObserver(
            forName: NSControl.textDidChangeNotification,
            object: labelField, queue: .main) { [weak self] _ in
                guard let self = self, !self.suppressLabelChange else { return }
                self.labelIsAutoGenerated = false
        }
    }

    required init?(coder: NSCoder) { fatalError() }

    deinit {
        pulseTimer?.invalidate()
        if let obs = labelEditObserver {
            NotificationCenter.default.removeObserver(obs)
        }
    }

    // Called when the window closes via any path — red title-bar X, save,
    // cancel, Cmd+W. Guarantees that the modal session actually ends.
    func windowWillClose(_ notification: Notification) {
        // Defensive: NUR eine Aufnahme stoppen, die DIESER Dialog selbst
        // gestartet hat — sonst wuerde das Schliessen des Editors eine gerade
        // laufende Overlay-Aufnahme abwuergen (geteilter AudioRecorder) und der
        // gesprochene Text waere verloren. Die eigene Aufnahme wird beendet und
        // ihre Datei weggeraeumt, damit das Mikrofon nicht haengen bleibt.
        if recordingStartedHere, VoiceServiceProvider.recorder?.isRecording == true {
            if let url = VoiceServiceProvider.recorder?.stop() {
                try? FileManager.default.removeItem(at: url)
            }
            recordingStartedHere = false
        }
        pulseTimer?.invalidate()
        NSApp.stopModal(withCode: result != nil ? .OK : .cancel)
    }

    // MARK: - Save paths

    @objc private func saveCurrent() { commitSave(text: textView.string) }
    @objc private func saveImproved() { commitSave(text: textView.string) }
    @objc private func saveOriginal() {
        // Persist what the user originally typed/dictated, even though the
        // textbox now shows the Gemini-improved version.
        commitSave(text: originalBeforeImprove ?? textView.string)
    }

    private func commitSave(text: String) {
        var label = labelField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
        let safeText = text.trimmingCharacters(in: .whitespacesAndNewlines)

        // Auto-derive a label from the first few words when the user only
        // dictated text and never typed a short label themselves. Mirrors
        // the Windows-side AutoLabelFromText behaviour.
        if label.isEmpty && !safeText.isEmpty {
            label = Self.autoLabelFromText(safeText)
            labelField.stringValue = label
        }
        guard !label.isEmpty else {
            showStatus("Bitte eine Kurzbezeichnung eintragen oder Prompt-Text einsprechen.")
            window?.makeFirstResponder(labelField)
            return
        }

        // Refresh the timestamp suffix on every save — both for new
        // prompts and edits of existing ones. The label is meant as a
        // "last modified" indicator, so re-saving a prompt should bump
        // it. Older behavior kept the original creation timestamp,
        // which made it impossible to tell at a glance which prompts
        // were recently revised.
        label = Self.ensureTimestampSuffix(label)

        // Hotkey-Auswahl: Index 0 = "Kein", 1..9 = Cmd+N
        let pickedHotkey: Int? = {
            let idx = hotkeyPopup.indexOfSelectedItem
            return (idx >= 1 && idx <= 9) ? idx : nil
        }()

        // Pre/Post-Auto-Wrap (Windows-Pendant #1858): wenn die Pre-/Post-
        // Checkbox aktiv ist und der Text noch nicht im Wrap-Format steckt,
        // automatisch in 'Pre-Prompt: "..."' bzw 'Post-Prompt: "..."' einbetten.
        // So muss der Benutzer das nicht selbst tippen — beim erneuten Edit
        // wird's idempotent erkannt und nicht doppelt gewrappt.
        let isPre = prePromptCheckbox.state == .on
        let isPost = postPromptCheckbox.state == .on
        let wrappedText = Self.applyPrePostWrap(text: text, isPre: isPre, isPost: isPost)

        // Letter-Hotkey: Index 0 = "Kein", 1..26 = A..Z
        let pickedLetter: String? = {
            let idx = letterPopup.indexOfSelectedItem
            guard idx >= 1, idx <= 26 else { return nil }
            let letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            let pos = letters.index(letters.startIndex, offsetBy: idx - 1)
            return String(letters[pos])
        }()

        result = PBPromptEditResult(
            shortLabel: label,
            originalText: wrappedText,
            isAlwaysOn: alwaysOnCheckbox.state == .on,
            isPrePrompt: isPre,
            isPostPrompt: isPost,
            hotkeyNumber: pickedHotkey,
            hotkeyLetter: pickedLetter)
        window?.close()    // windowWillClose handles stopModal
    }

    /// Pre/Post-Auto-Wrap: idempotent. Wenn isPre=true und der Text noch nicht
    /// mit 'Pre-Prompt: "' beginnt, wird er gewrappt. Analog fuer Post. Wenn
    /// die Checkbox AUS ist und der Text gewrappt ist, wird der Wrap entfernt
    /// (= Round-Trip-fest). Beide Flags gleichzeitig: Pre umschliesst aussen,
    /// Post innen — das spielt aber praktisch keine Rolle weil normalerweise
    /// nur eines aktiv ist.
    private static func applyPrePostWrap(text: String, isPre: Bool, isPost: Bool) -> String {
        var t = text.trimmingCharacters(in: .whitespacesAndNewlines)

        // Existierende Wraps abbauen (idempotent)
        if let unwrapped = unwrap(t, prefix: "Pre-Prompt: \"", suffix: "\"") { t = unwrapped }
        if let unwrapped = unwrap(t, prefix: "Post-Prompt: \"", suffix: "\"") { t = unwrapped }

        // Neu wrappen wenn Flag gesetzt
        if isPost { t = "Post-Prompt: \"\(t)\"" }
        if isPre  { t = "Pre-Prompt: \"\(t)\""  }
        return t
    }

    private static func unwrap(_ s: String, prefix: String, suffix: String) -> String? {
        guard s.hasPrefix(prefix), s.hasSuffix(suffix), s.count >= prefix.count + suffix.count else {
            return nil
        }
        let start = s.index(s.startIndex, offsetBy: prefix.count)
        let end = s.index(s.endIndex, offsetBy: -suffix.count)
        return String(s[start..<end])
    }

    /// Sets a fresh "(dd.MM.yyyy, HH:mm)" suffix on the label, replacing
    /// any previously appended timestamp in our own format. Non-timestamp
    /// parenthesised notes the user may have typed (e.g. "(WIP)") are
    /// left in place — only our exact format is stripped so we don't
    /// trample on user content. Idempotent: calling multiple times only
    /// ever leaves one timestamp, always the most recent.
    private static func ensureTimestampSuffix(_ label: String) -> String {
        var stripped = label
        // Match exactly our own format: " (dd.MM.yyyy, HH:mm)" possibly
        // followed by trailing whitespace. Only strip if the suffix
        // matches this shape — anything else is user content.
        let pattern = #"\s*\(\d{2}\.\d{2}\.\d{4},\s*\d{2}:\d{2}\)\s*$"#
        if let range = stripped.range(of: pattern, options: .regularExpression) {
            stripped.removeSubrange(range)
        }
        stripped = stripped.trimmingCharacters(in: .whitespaces)
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.dateFormat = "dd.MM.yyyy, HH:mm"
        return stripped + " (" + fmt.string(from: Date()) + ")"
    }

    @objc private func cancel() {
        result = nil
        window?.close()    // windowWillClose handles stopModal
    }

    /// Builds a short label from the first 7 non-empty words of the prompt
    /// text, hard-capped at 40 characters. Falls back to "Prompt" if the
    /// text is unusable.
    private static func autoLabelFromText(_ text: String) -> String {
        let words = text.split(whereSeparator: { $0.isWhitespace }).map(String.init)
        guard !words.isEmpty else { return "Prompt" }
        var sb = ""
        for w in words.prefix(7) {
            if !sb.isEmpty { sb += " " }
            sb += w
            if sb.count >= 40 { break }
        }
        let trimmed = sb.trimmingCharacters(in: CharacterSet(charactersIn: ".,;:!? "))
        return trimmed.isEmpty ? "Prompt" : trimmed
    }

    // MARK: - Mic recording

    @objc private func toggleRecording() {
        guard let recorder = VoiceServiceProvider.recorder,
              let groq = VoiceServiceProvider.groq else {
            showStatus("Mic-Aufnahme nicht verfuegbar (kein Groq-Key in .env).")
            return
        }

        // Der AudioRecorder ist prozessweit geteilt. Laeuft bereits eine
        // Aufnahme, die NICHT dieser Dialog gestartet hat (typisch: die
        // Overlay-Aufnahme), duerfen wir sie weder stoppen noch ihr Audio in
        // dieses Feld umleiten — sonst bricht die Overlay-Aufnahme ab.
        if recorder.isRecording && !recordingStartedHere {
            showStatus("Es laeuft bereits eine Aufnahme (Voice-Overlay). Bitte zuerst dort stoppen.")
            return
        }

        if recorder.isRecording {
            // ── Stop & transcribe ──
            recordingStartedHere = false
            stopPulse()
            setMicButtonTinted(true)
            showStatus("Transkribiere...")
            guard let url = recorder.stop() else {
                showStatus("Aufnahme leer.")
                setMicButtonTinted(false)
                return
            }
            groq.transcribe(fileURL: url) { [weak self] result in
                onMainCommon {
                    guard let self = self else { return }
                    switch result {
                    case .success(let text):
                        self.appendToPromptText(text)
                        self.showStatus("\(text.count) Zeichen eingefuegt.")
                        // Auto-title only if the user hasn't already typed a
                        // custom label themselves. autoTitle is no-op when
                        // Gemini isn't configured.
                        self.autoTitle()
                    case .failure(let err):
                        self.showStatus("Transkription fehlgeschlagen: \(err.localizedDescription)")
                    }
                    self.setMicButtonTinted(false)
                    try? FileManager.default.removeItem(at: url)
                }
            }
            return
        }

        // ── Start ──
        do {
            try recorder.start()
            recordingStartedHere = true
            startPulse()
            showStatus("Aufnahme laeuft. Klick erneut auf Mic zum Stoppen.")
        } catch {
            showStatus("Aufnahme konnte nicht gestartet werden: \(error.localizedDescription)")
            setMicButtonTinted(false)
        }
    }

    private func appendToPromptText(_ newText: String) {
        let trimmed = newText.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        var current = textView.string
        if !current.isEmpty,
           !current.hasSuffix(" "), !current.hasSuffix("\n") {
            current += " "
        }
        textView.string = current + trimmed
        // Scroll caret to the end so the user sees the new text land.
        let len = textView.string.count
        textView.setSelectedRange(NSRange(location: len, length: 0))
        textView.scrollRangeToVisible(NSRange(location: len, length: 0))
    }

    private func startPulse() {
        pulseTimer?.invalidate()
        pulseBright = false
        setMicButtonRecording(bright: false)
        pulseTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            self.pulseBright.toggle()
            self.setMicButtonRecording(bright: self.pulseBright)
        }
    }

    private func stopPulse() {
        pulseTimer?.invalidate()
        pulseTimer = nil
        pulseBright = false
    }

    private func setMicButtonRecording(bright: Bool) {
        let red    = NSColor(calibratedRed: 0.90, green: 0.22, blue: 0.21, alpha: 1)
        let bright1 = NSColor(calibratedRed: 1.00, green: 0.40, blue: 0.40, alpha: 1)
        micButton.layer?.backgroundColor = (bright ? bright1 : red).cgColor
    }

    /// Amber = transcription in progress. Idle = neutral grey.
    private func setMicButtonTinted(_ transcribing: Bool) {
        if transcribing {
            micButton.layer?.backgroundColor = NSColor(calibratedRed: 0.72, green: 0.53, blue: 0.04, alpha: 1).cgColor
        } else {
            micButton.layer?.backgroundColor = NSColor(calibratedWhite: 0.24, alpha: 1).cgColor
        }
    }

    // MARK: - Gemini correction

    @objc private func runGemini() {
        guard !isImproving else {
            tvoDebug("[Dlg] runGemini ignored — already improving")
            return
        }
        guard let gemini = VoiceServiceProvider.gemini else {
            tvoDebug("[Dlg] runGemini: no gemini client configured")
            showStatus("Gemini nicht verfuegbar (kein Gemini-Key in .env).")
            return
        }
        let current = textView.string.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !current.isEmpty else {
            tvoDebug("[Dlg] runGemini: empty text, skip")
            showStatus("Kein Text zum Verbessern.")
            return
        }

        isImproving = true
        geminiButton.isEnabled = false
        micButton.isEnabled = false
        showStatus("Verbessere mit Gemini...")
        let startedAt = Date()
        tvoDebug("[Dlg] runGemini: send \(current.count) chars, \(current.components(separatedBy: "\n").count) lines")
        // PromptBoard-Editor nutzt das Prompt-Engineer-Template (rohes
        // Diktat → kopierfertiger Claude-Code-CLI-Prompt). Das Diktat-
        // Cleanup-Template bleibt dem Overlay-Pfad vorbehalten.
        gemini.buildClaudeCodePrompt(current) { [weak self] result in
            onMainCommon {
                guard let self = self else {
                    tvoDebug("[Dlg] runGemini: callback fired but self is nil")
                    return
                }
                let elapsed = Date().timeIntervalSince(startedAt)
                self.isImproving = false
                self.geminiButton.isEnabled = VoiceServiceProvider.geminiAvailable
                self.micButton.isEnabled = VoiceServiceProvider.recorderAvailable

                switch result {
                case .success(let improved):
                    let trimmed = improved.trimmingCharacters(in: .whitespacesAndNewlines)
                    tvoDebug("[Dlg] runGemini: SUCCESS in \(String(format: "%.1f", elapsed))s — \(trimmed.count) chars returned")
                    guard !trimmed.isEmpty else {
                        self.showStatus("Gemini lieferte leere Antwort.")
                        return
                    }
                    // Capture the user's original ONLY on the first improvement
                    // so multiple back-to-back G clicks don't lose the very
                    // first text.
                    if self.originalBeforeImprove == nil {
                        self.originalBeforeImprove = current
                    }
                    self.textView.string = trimmed
                    self.showImprovedFooter()
                    self.showStatus("Verbessert (\(trimmed.count) Zeichen). Original im Hintergrund erhalten.")
                    // Re-title against the improved text — overwrites a
                    // previous auto-title but never a manually edited label.
                    self.autoTitle()
                case .failure(let err):
                    tvoDebug("[Dlg] runGemini: FAIL in \(String(format: "%.1f", elapsed))s — \(err.localizedDescription)")
                    self.showStatus("Gemini-Verbesserung fehlgeschlagen: \(err.localizedDescription)")
                }
            }
        }
    }

    /// Replaces the single "Speichern" button with a vertical stack
    /// containing "Verbessert speichern" (primary) and "Original speichern"
    /// (secondary). Window grows by 22 pt to keep the footer airy.
    private func showImprovedFooter() {
        rightButtonContainer.arrangedSubviews.forEach {
            rightButtonContainer.removeArrangedSubview($0)
            $0.removeFromSuperview()
        }
        rightButtonContainer.addArrangedSubview(saveImprovedButton)
        rightButtonContainer.addArrangedSubview(saveOriginalButton)
        // Make both buttons the same width so they read as a coherent pair.
        saveOriginalButton.widthAnchor.constraint(
            equalTo: saveImprovedButton.widthAnchor).isActive = true
        if let w = window {
            var f = w.frame
            f.size.height += 22
            f.origin.y -= 22 // anchor to top edge so buttons appear closer to mouse
            w.setFrame(f, display: true, animate: false)
        }
    }

    // MARK: - Auto-title via Gemini

    /// Asks Gemini for a max-3-word headline of the current prompt text and
    /// drops it into the short-label field. Skipped silently when:
    /// - Gemini isn't configured (no API key),
    /// - the prompt text is empty,
    /// - the user has already typed a custom label themselves
    ///   (see `labelIsAutoGenerated`).
    /// Called after mic transcription AND after Gemini improvement so the
    /// label always reflects the freshest version of the text.
    private func autoTitle() {
        guard let gemini = VoiceServiceProvider.gemini else { return }

        let currentLabel = labelField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
        // Skip when the field already holds something the user typed.
        // First-time auto-fill (currentLabel empty) or replacing a previous
        // auto-fill (labelIsAutoGenerated true) are both fine.
        if !currentLabel.isEmpty && !labelIsAutoGenerated { return }

        let text = textView.string.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.isEmpty { return }

        let prompt = """
        Schreibe eine sehr kurze Ueberschrift (genau MAXIMAL 3 Woerter, \
        ohne Anfuehrungszeichen, ohne Punkt am Ende, im Stil eines Datei-Tags) \
        fuer den folgenden Prompt-Text. Gib NUR die Ueberschrift aus, sonst nichts.

        Prompt-Text:
        \(text)
        """

        gemini.correctText(prompt) { [weak self] result in
            onMainCommon {
                guard let self = self else { return }
                switch result {
                case .success(let raw):
                    let headline = Self.sanitizeHeadline(raw)
                    if headline.isEmpty { return }
                    self.suppressLabelChange = true
                    self.labelField.stringValue = headline
                    self.suppressLabelChange = false
                    self.labelIsAutoGenerated = true
                case .failure(let err):
                    NSLog("[PromptEditDialog] AutoTitle failed: %@", err.localizedDescription)
                }
            }
        }
    }

    /// Defensive trim of the model output. Gemini sometimes wraps replies
    /// in quotes, adds trailing periods, or returns more than 3 words
    /// despite the explicit instruction. Hard-cap to 3 words so the bar
    /// stays compact regardless of the model's mood.
    private static func sanitizeHeadline(_ raw: String) -> String {
        let line = raw.split(separator: "\n").first.map(String.init) ?? raw
        let stripped = line.trimmingCharacters(in: CharacterSet(charactersIn: "\"'.,*#`–— \t"))
        let words = stripped.split(whereSeparator: { $0.isWhitespace }).map(String.init)
        if words.isEmpty { return "" }
        let n = min(3, words.count)
        return words.prefix(n).joined(separator: " ")
    }

    // MARK: - Helpers

    private func showStatus(_ text: String) {
        statusLabel.stringValue = text
    }

    /// 40x40 circular button with a single glyph, matching the Windows
    /// PromptEditDialog ActionRound style.
    private func makeRoundActionButton(symbol: String, tooltip: String, action: Selector) -> NSButton {
        let btn = NSButton(title: "", target: self, action: action)
        btn.isBordered = false
        btn.toolTip = tooltip
        btn.setButtonType(.momentaryChange)
        btn.wantsLayer = true
        btn.layer?.backgroundColor = NSColor(calibratedWhite: 0.24, alpha: 1).cgColor
        btn.layer?.borderColor = NSColor(calibratedWhite: 0.46, alpha: 1).cgColor
        btn.layer?.borderWidth = 1
        btn.layer?.cornerRadius = 20
        btn.attributedTitle = NSAttributedString(
            string: symbol,
            attributes: [
                .foregroundColor: NSColor.white,
                .font: NSFont.systemFont(ofSize: 16, weight: .semibold)
            ])
        btn.widthAnchor.constraint(equalToConstant: 40).isActive = true
        btn.heightAnchor.constraint(equalToConstant: 40).isActive = true
        return btn
    }

    static func ask(parent: NSWindow?, title: String,
                    label: String, text: String, alwaysOn: Bool,
                    prePrompt: Bool, postPrompt: Bool,
                    hotkey: Int? = nil, hotkeyLetter: String? = nil) -> PBPromptEditResult? {
        let ctrl = PBPromptEditDialog(title: title, initialLabel: label,
                                      initialText: text, initialAlwaysOn: alwaysOn,
                                      initialPrePrompt: prePrompt,
                                      initialPostPrompt: postPrompt,
                                      initialHotkey: hotkey,
                                      initialHotkeyLetter: hotkeyLetter)
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
    // Windows-1:1: AutoHide, Orientation, PersistOverlayPosition.
    let autoHide: Bool
    let orientation: String           // "vertical" | "horizontal"
    let persistOverlayPosition: Bool
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
    // Windows-1:1: AutoHide, Horizontal-Orientation, Persist-Position-Checkboxen
    private let autoHideCheck = NSButton(checkboxWithTitle: "Auto-Hide (Mic-Pille bei Inaktivitaet)", target: nil, action: nil)
    private let horizontalCheck = NSButton(checkboxWithTitle: "Horizontal-Orientierung beim Start", target: nil, action: nil)
    private let persistPositionCheck = NSButton(checkboxWithTitle: "Disketten-Position ueber App-Neustart hinweg merken", target: nil, action: nil)
    // Woerterbuch-Schalter (Frank-Wunsch 2026-06-22): steuert die geteilte SK-Datei
    // vocabulary-enabled.txt; GeminiClient prueft sie vor dem Laden des Vokabulars.
    private let vocabularyEnabledCheck = NSButton(checkboxWithTitle: "Wörterbuch verwenden (an Gemini mitschicken)", target: nil, action: nil)
    // Persoenliches Vokabular-Woerterbuch (mehrzeilig, scrollbar). Schreibt/liest
    // die SK-Datei personal-vocabulary.txt; GeminiClient haengt sie als Praeambel
    // vor den Korrektur-Prompt, AppDelegate synchronisiert sie ueber Drive.
    private let vocabularyTextView = NSTextView()
    private let vocabularyScroll = NSScrollView()
    // Editierbarer Einleitungstext (Praeambel) fuer den Woerterbuch-Block
    // (Frank-Wunsch 2026-06-22): liegt als vocabulary-preamble.txt im SK-Ordner.
    // Mac las sie bisher nur (GeminiClient.effectiveVocabularyPreamble); jetzt auch
    // hier editierbar wie auf Windows, Aenderung geht sofort ins Drive-Backup.
    private let preambleTextView = NSTextView()
    private let preambleScroll = NSScrollView()

    private var settings: PBAppSettings
    private var result: PBSettingsResult?

    init(settings: PBAppSettings) {
        self.settings = settings
        // Wunschhoehe 860 — auf kleinen Bildschirmen (MacBook Air, externe
        // Zweitmonitore) passt das nicht, und der untere Teil des Dialogs war
        // schlicht nicht erreichbar. Deshalb: Hoehe auf den sichtbaren Bereich
        // deckeln und den Inhalt darunter scrollbar machen (siehe unten).
        let preferredHeight: CGFloat = 860
        let available = (NSScreen.main?.visibleFrame.height ?? preferredHeight) - 40
        let windowHeight = Swift.min(preferredHeight, Swift.max(360, available))
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 640, height: windowHeight),
            styleMask: [.titled, .closable, .resizable],
            backing: .buffered, defer: false)
        window.title = "Einstellungen"
        // Untergrenze, damit das Fenster nicht unter die Bedienbarkeit
        // geschrumpft werden kann; nach oben bleibt es frei skalierbar.
        window.minSize = NSSize(width: 560, height: 360)
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

        // The active voice clients may read their keys from the central SK
        // .env while the PromptBoard DB is empty. Display those effective keys.
        let fileConfig = try? Config.load()
        groqField.stringValue = settings.groqApiKey.flatMap { $0.isEmpty ? nil : $0 }
            ?? fileConfig?.groqApiKey ?? ""
        geminiField.stringValue = settings.geminiApiKey.flatMap { $0.isEmpty ? nil : $0 }
            ?? fileConfig?.geminiApiKey ?? ""
        separatorField.stringValue = settings.separatorTemplate
        clientIdField.stringValue = settings.googleClientId ?? ""
        clientSecretField.stringValue = settings.googleClientSecret ?? ""
        // Checkboxen mit gespeicherten Settings vorbefuellen.
        autoHideCheck.state = settings.autoHide ? .on : .off
        horizontalCheck.state = (settings.orientation == "horizontal") ? .on : .off
        persistPositionCheck.state = settings.persistOverlayPosition ? .on : .off
        autoHideCheck.contentTintColor = NSColor.white
        horizontalCheck.contentTintColor = NSColor.white
        persistPositionCheck.contentTintColor = NSColor.white
        vocabularyEnabledCheck.state = PBSettingsDialog.loadVocabularyEnabled() ? .on : .off
        vocabularyEnabledCheck.contentTintColor = NSColor.white

        // Persoenliches Vokabular aus der SK-Datei vorbefuellen + scrollbares
        // mehrzeiliges Textfeld (dunkel) konfigurieren.
        vocabularyTextView.string = PBSettingsDialog.loadPersonalVocabulary()
        vocabularyScroll.hasVerticalScroller = true
        vocabularyScroll.borderType = .bezelBorder
        vocabularyScroll.translatesAutoresizingMaskIntoConstraints = false
        vocabularyScroll.drawsBackground = true
        vocabularyScroll.backgroundColor = NSColor(red: 0.18, green: 0.18, blue: 0.18, alpha: 1)
        vocabularyTextView.minSize = NSSize(width: 0, height: 0)
        vocabularyTextView.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        vocabularyTextView.isVerticallyResizable = true
        vocabularyTextView.isHorizontallyResizable = false
        vocabularyTextView.autoresizingMask = [.width]
        vocabularyTextView.textContainer?.containerSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        vocabularyTextView.textContainer?.widthTracksTextView = true
        vocabularyTextView.isRichText = false
        vocabularyTextView.font = .systemFont(ofSize: 13)
        vocabularyTextView.drawsBackground = true
        vocabularyTextView.backgroundColor = NSColor(red: 0.18, green: 0.18, blue: 0.18, alpha: 1)
        vocabularyTextView.textColor = .white
        vocabularyTextView.insertionPointColor = .white
        vocabularyScroll.documentView = vocabularyTextView

        // Praeambel-Editierfeld analog zum Vokabular-Feld (dunkel, mehrzeilig, scrollbar),
        // vorbefuellt mit dem aktuell wirksamen Einleitungstext (Datei oder Default).
        preambleTextView.string = GeminiClient.effectiveVocabularyPreamble()
        preambleScroll.hasVerticalScroller = true
        preambleScroll.borderType = .bezelBorder
        preambleScroll.translatesAutoresizingMaskIntoConstraints = false
        preambleScroll.drawsBackground = true
        preambleScroll.backgroundColor = NSColor(red: 0.18, green: 0.18, blue: 0.18, alpha: 1)
        preambleTextView.minSize = NSSize(width: 0, height: 0)
        preambleTextView.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        preambleTextView.isVerticallyResizable = true
        preambleTextView.isHorizontallyResizable = false
        preambleTextView.autoresizingMask = [.width]
        preambleTextView.textContainer?.containerSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        preambleTextView.textContainer?.widthTracksTextView = true
        preambleTextView.isRichText = false
        preambleTextView.font = .systemFont(ofSize: 13)
        preambleTextView.drawsBackground = true
        preambleTextView.backgroundColor = NSColor(red: 0.18, green: 0.18, blue: 0.18, alpha: 1)
        preambleTextView.textColor = .white
        preambleTextView.insertionPointColor = .white
        preambleScroll.documentView = preambleTextView

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

        let editPromptsButton = PBDarkTheme.makeSecondaryButton(
            title: "Gemini-Prompts bearbeiten…", target: self, action: #selector(editGeminiPrompts))

        func label(_ text: String) -> NSTextField {
            let l = NSTextField(labelWithString: text)
            PBDarkTheme.styleLabel(l)
            return l
        }

        PBDarkTheme.styleLabel(statusLabel)
        updateStatus()

        // Karten-Look (Frank-Wunsch 2026-06-22, analog Windows): zusammengehoerige Felder in
        // betitelte NSBox-Karten gruppieren. fullWidth-Views (Felder/Scrollviews) spannen ueber
        // die ganze Kartenbreite; Labels/Checkboxen bleiben linksbuendig. NSBox rendert im
        // Dark-Appearance des Fensters automatisch dunkel.
        func card(_ title: String, _ rows: [NSView], fullWidth: [NSView] = []) -> NSBox {
            let inner = NSStackView(views: rows)
            inner.orientation = .vertical
            inner.alignment = .leading
            inner.spacing = 6
            inner.translatesAutoresizingMaskIntoConstraints = false
            let box = NSBox()
            box.title = title
            box.titlePosition = .atTop
            box.translatesAutoresizingMaskIntoConstraints = false
            box.contentView?.addSubview(inner)
            if let cv = box.contentView {
                NSLayoutConstraint.activate([
                    inner.leadingAnchor.constraint(equalTo: cv.leadingAnchor, constant: 10),
                    inner.trailingAnchor.constraint(equalTo: cv.trailingAnchor, constant: -10),
                    inner.topAnchor.constraint(equalTo: cv.topAnchor, constant: 8),
                    inner.bottomAnchor.constraint(equalTo: cv.bottomAnchor, constant: -8),
                ])
            }
            for v in fullWidth {
                v.widthAnchor.constraint(equalTo: inner.widthAnchor).isActive = true
            }
            return box
        }

        let apiCard = card("API-Schlüssel", [
            label("Groq API Key (Whisper-Transkription)"), groqField,
            label("Gemini API Key (optional)"), geminiField,
        ], fullWidth: [groqField, geminiField])

        let vocabCard = card("Persönliches Wörterbuch", [
            label("Häufige Begriffe für die Gemini-Korrektur"),
            vocabularyEnabledCheck, vocabularyScroll,
            label("Einleitungstext (Präambel) – wird der Wörterbuch-Liste vorangestellt"),
            preambleScroll,
        ], fullWidth: [vocabularyScroll, preambleScroll])

        let promptsCard = card("Gemini-Korrektur-Prompts (Profil 1–10)", [editPromptsButton])

        let behaviorCard = card("Darstellung & Verhalten", [
            label("Separator-Template"), separatorField,
            autoHideCheck, horizontalCheck, persistPositionCheck,
        ], fullWidth: [separatorField])

        let driveCard = card("Google Drive Backup", [
            label("Client ID"), clientIdField,
            label("Client Secret"), clientSecretField,
            statusLabel, connectRow,
        ], fullWidth: [clientIdField, clientSecretField])

        // Die Karten wandern in eine Scrollansicht; die Knopfreihe bleibt
        // FEST am unteren Rand. Wuerde sie mitscrollen, waeren "Speichern" und
        // "Abbrechen" auf einem kleinen Bildschirm erst nach dem Scrollen
        // erreichbar — genau die Falle, die den Dialog vorher unbedienbar machte.
        let stack = NSStackView(views: [apiCard, vocabCard, promptsCard, behaviorCard, driveCard])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 12
        stack.translatesAutoresizingMaskIntoConstraints = false

        let contentScroll = NSScrollView()
        contentScroll.translatesAutoresizingMaskIntoConstraints = false
        contentScroll.hasVerticalScroller = true
        contentScroll.hasHorizontalScroller = false
        contentScroll.autohidesScrollers = false
        contentScroll.borderType = .noBorder
        contentScroll.drawsBackground = false
        // Heller Scrollbalken: auf dem dunklen Untergrund waere die helle
        // Standardvariante kaum zu sehen.
        contentScroll.scrollerKnobStyle = .light

        let documentView = NSView()
        documentView.translatesAutoresizingMaskIntoConstraints = false
        documentView.addSubview(stack)
        contentScroll.documentView = documentView

        buttonRow.translatesAutoresizingMaskIntoConstraints = false
        window.contentView?.addSubview(contentScroll)
        window.contentView?.addSubview(buttonRow)

        guard let contentView = window.contentView else { return }

        NSLayoutConstraint.activate([
            // Scrollbereich fuellt alles oberhalb der Knopfreihe.
            contentScroll.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            contentScroll.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            contentScroll.topAnchor.constraint(equalTo: contentView.topAnchor),
            contentScroll.bottomAnchor.constraint(equalTo: buttonRow.topAnchor, constant: -12),

            // Knopfreihe fest am unteren Rand.
            buttonRow.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            buttonRow.leadingAnchor.constraint(greaterThanOrEqualTo: contentView.leadingAnchor, constant: 16),
            buttonRow.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -16),

            // Der Inhalt ist genau so breit wie der sichtbare Bereich — sonst
            // entstuende zusaetzlich ein waagerechter Scrollbalken.
            documentView.widthAnchor.constraint(equalTo: contentScroll.contentView.widthAnchor),

            stack.leadingAnchor.constraint(equalTo: documentView.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: documentView.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: documentView.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: documentView.bottomAnchor, constant: -16),

            apiCard.widthAnchor.constraint(equalTo: stack.widthAnchor),
            vocabCard.widthAnchor.constraint(equalTo: stack.widthAnchor),
            promptsCard.widthAnchor.constraint(equalTo: stack.widthAnchor),
            behaviorCard.widthAnchor.constraint(equalTo: stack.widthAnchor),
            driveCard.widthAnchor.constraint(equalTo: stack.widthAnchor),
            vocabularyScroll.heightAnchor.constraint(equalToConstant: 90),
            preambleScroll.heightAnchor.constraint(equalToConstant: 90),
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

    // Persoenliches Vokabular lebt als Datei im SK-Ordner (gleicher Ort wie die
    // Korrektur-Prompts). Dieses Feld ist nur die Editier-Oberflaeche; GeminiClient
    // liest die Datei direkt, AppDelegate synchronisiert sie ueber Drive.
    private static var personalVocabularyURL: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("SK/VoiceOverlays/personal-vocabulary.txt")
    }

    static func loadPersonalVocabulary() -> String {
        (try? String(contentsOf: personalVocabularyURL, encoding: .utf8)) ?? ""
    }

    static func savePersonalVocabulary(_ text: String) {
        let url = personalVocabularyURL
        do {
            try FileManager.default.createDirectory(
                at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
            try (trimmed + "\n").write(to: url, atomically: true, encoding: .utf8)
        } catch {
            NSLog("Persoenliches Woerterbuch konnte nicht gespeichert werden: \(error.localizedDescription)")
        }
    }

    // Woerterbuch-Schalter (Frank-Wunsch 2026-06-22): geteilte SK-Datei
    // vocabulary-enabled.txt. "true" = Woerterbuch wird mitgeschickt; sonst aus.
    private static var vocabularyEnabledURL: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("SK/VoiceOverlays/vocabulary-enabled.txt")
    }

    static func loadVocabularyEnabled() -> Bool {
        guard let s = try? String(contentsOf: vocabularyEnabledURL, encoding: .utf8) else { return false }
        return s.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "true"
    }

    static func saveVocabularyEnabled(_ enabled: Bool) {
        let url = vocabularyEnabledURL
        do {
            try FileManager.default.createDirectory(
                at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
            try (enabled ? "true\n" : "false\n").write(to: url, atomically: true, encoding: .utf8)
        } catch {
            NSLog("Woerterbuch-Schalter konnte nicht gespeichert werden: \(error.localizedDescription)")
        }
    }

    @objc private func save() {
        result = PBSettingsResult(
            groqApiKey: nullIfBlank(groqField.stringValue),
            geminiApiKey: nullIfBlank(geminiField.stringValue),
            separatorTemplate: separatorField.stringValue.isEmpty ? "\n\n;\n\n" : separatorField.stringValue,
            googleClientId: nullIfBlank(clientIdField.stringValue),
            googleClientSecret: nullIfBlank(clientSecretField.stringValue),
            autoHide: autoHideCheck.state == .on,
            orientation: horizontalCheck.state == .on ? "horizontal" : "vertical",
            persistOverlayPosition: persistPositionCheck.state == .on)
        // Persoenliches Vokabular zusaetzlich in die SK-Datei schreiben (wird von
        // GeminiClient direkt gelesen, nicht ueber die DB-Settings).
        PBSettingsDialog.savePersonalVocabulary(vocabularyTextView.string)
        PBSettingsDialog.saveVocabularyEnabled(vocabularyEnabledCheck.state == .on)
        GeminiClient.saveVocabularyPreamble(preambleTextView.string)   // editierbare Praeambel (wie Windows)
        GeminiPromptSync.tryUpload()   // Schalter-/Praeambel-Aenderung sofort ins Backup
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
            onMainCommon {
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

    @objc private func editGeminiPrompts() {
        GeminiPromptListDialog.show()
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

// MARK: - Gemini-Prompt-Editor (Etappe 2c, Frank-Wunsch 2026-06-22)

/// Editor fuer EINE Gemini-Korrektur-Prompt-Vorlage (Profil 1-10). Pendant zum
/// Windows GeminiPromptEditDialog. Laedt die wirksame Vorlage, speichert sie
/// zurueck in die SK-Datei. Die zwei Knoepfe fuegen {{TEXT}} und {{WOERTERBUCH}}
/// an der Cursor-Position ein. Wird nested im (modalen) Settings-Dialog gezeigt,
/// daher KEIN runHidingFloatingPanels — die floating panels sind schon versteckt.
final class GeminiPromptEditDialog: NSWindowController, NSWindowDelegate {
    private let profile: Int
    private let textView = NSTextView()
    private let scroll = NSScrollView()

    init(profile: Int) {
        self.profile = profile
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 640, height: 560),
            styleMask: [.titled, .closable],
            backing: .buffered, defer: false)
        window.title = GeminiClient.profileLabel(profile)
        window.isReleasedWhenClosed = false
        window.level = .modalPanel
        PBDarkTheme.apply(to: window)
        super.init(window: window)

        let header = NSTextField(labelWithString: GeminiClient.profileLabel(profile))
        header.font = .systemFont(ofSize: 14, weight: .bold)
        header.textColor = .white
        header.drawsBackground = false

        let hint = NSTextField(wrappingLabelWithString:
            "Das ist die Vorlage, die an Gemini geschickt wird. Mit den Knöpfen unten fügst du an der Cursor-Stelle die Platzhalter ein: einen für deinen gesprochenen Text und einen für das Wörterbuch. Fehlt der Text-Platzhalter, wird dein Text hinten angehängt; fehlt der Wörterbuch-Platzhalter, kommt das Wörterbuch (wenn eingeschaltet) an den Anfang.")
        hint.font = .systemFont(ofSize: 11)
        hint.textColor = PBDarkTheme.textSecondary
        hint.drawsBackground = false

        scroll.hasVerticalScroller = true
        scroll.borderType = .bezelBorder
        scroll.translatesAutoresizingMaskIntoConstraints = false
        scroll.drawsBackground = true
        scroll.backgroundColor = PBDarkTheme.fieldBackground
        textView.minSize = NSSize(width: 0, height: 0)
        textView.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        textView.isVerticallyResizable = true
        textView.isHorizontallyResizable = false
        textView.autoresizingMask = [.width]
        textView.textContainer?.containerSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        textView.textContainer?.widthTracksTextView = true
        textView.isRichText = false
        textView.font = NSFont.monospacedSystemFont(ofSize: 12, weight: .regular)
        textView.drawsBackground = true
        textView.backgroundColor = PBDarkTheme.fieldBackground
        textView.textColor = .white
        textView.insertionPointColor = .white
        textView.string = GeminiClient.effectivePrompt(profile: profile)
        scroll.documentView = textView

        let insertText = PBDarkTheme.makeSecondaryButton(title: "Platzhalter für gesprochenen Text", target: self, action: #selector(insertTextMarker))
        let insertVocab = PBDarkTheme.makeSecondaryButton(title: "Wörterbuch-Platzhalter", target: self, action: #selector(insertVocabMarker))
        let insertRow = NSStackView(views: [insertText, insertVocab])
        insertRow.orientation = .horizontal
        insertRow.spacing = 8

        let save = PBDarkTheme.makePrimaryButton(title: "Speichern", target: self, action: #selector(saveDlg))
        save.keyEquivalent = "\r"
        let cancel = PBDarkTheme.makeSecondaryButton(title: "Abbrechen", target: self, action: #selector(cancelDlg))
        let buttonRow = NSStackView(views: [cancel, save])
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8

        let stack = NSStackView(views: [header, hint, scroll, insertRow, buttonRow])
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 10
        stack.translatesAutoresizingMaskIntoConstraints = false
        window.contentView?.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: window.contentView!.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: window.contentView!.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: window.contentView!.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: window.contentView!.bottomAnchor, constant: -16),
            hint.widthAnchor.constraint(equalTo: stack.widthAnchor),
            scroll.widthAnchor.constraint(equalTo: stack.widthAnchor),
            scroll.heightAnchor.constraint(greaterThanOrEqualToConstant: 360),
        ])
        window.delegate = self
    }

    required init?(coder: NSCoder) { fatalError() }

    private func insert(_ marker: String) {
        let range = textView.selectedRange()
        textView.textStorage?.replaceCharacters(in: range, with: marker)
        let newLoc = range.location + (marker as NSString).length
        textView.setSelectedRange(NSRange(location: newLoc, length: 0))
        window?.makeFirstResponder(textView)
    }
    @objc private func insertTextMarker() { insert("{{TEXT}}") }
    @objc private func insertVocabMarker() { insert("{{WOERTERBUCH}}") }

    @objc private func saveDlg() {
        GeminiClient.saveProfilePrompt(profile: profile, text: textView.string)
        GeminiPromptSync.tryUpload()   // sofort ins Google-Drive-Backup
        window?.close()
    }
    @objc private func cancelDlg() {
        window?.close()
    }

    func windowWillClose(_ notification: Notification) {
        NSApp.stopModal()
    }

    static func show(profile: Int) {
        let dlg = GeminiPromptEditDialog(profile: profile)
        guard let w = dlg.window else { return }
        w.center()
        w.makeKeyAndOrderFront(nil)
        w.orderFrontRegardless()
        NSApp.runModal(for: w)
    }
}

/// Liste der 10 Gemini-Profile. Pendant zum Windows GeminiPromptListDialog.
/// Pro Profil ein Knopf, der den Editor fuer dieses Profil oeffnet.
final class GeminiPromptListDialog: NSWindowController, NSWindowDelegate {
    init() {
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 460, height: 520),
            styleMask: [.titled, .closable],
            backing: .buffered, defer: false)
        window.title = "Gemini-Prompts"
        window.isReleasedWhenClosed = false
        window.level = .modalPanel
        PBDarkTheme.apply(to: window)
        super.init(window: window)

        let header = NSTextField(labelWithString: "Gemini-Prompts bearbeiten")
        header.font = .systemFont(ofSize: 14, weight: .bold)
        header.textColor = .white
        header.drawsBackground = false

        let hint = NSTextField(wrappingLabelWithString:
            "Wähle ein Profil zum Bearbeiten. Jedes Profil hat seine eigene Vorlage, die an Gemini geschickt wird. Die Prompts gelten für alle Overlays auf diesem Rechner.")
        hint.font = .systemFont(ofSize: 11)
        hint.textColor = PBDarkTheme.textSecondary
        hint.drawsBackground = false

        var profileButtons: [NSButton] = []
        var rows: [NSView] = [header, hint]
        for profile in 1...10 {
            let btn = PBDarkTheme.makeSecondaryButton(title: GeminiClient.profileLabel(profile), target: self, action: #selector(openProfile(_:)))
            btn.tag = profile
            btn.alignment = .left
            profileButtons.append(btn)
            rows.append(btn)
        }
        let close = PBDarkTheme.makeSecondaryButton(title: "Schließen", target: self, action: #selector(closeDlg))
        rows.append(close)

        let stack = NSStackView(views: rows)
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 6
        stack.translatesAutoresizingMaskIntoConstraints = false
        window.contentView?.addSubview(stack)

        var constraints: [NSLayoutConstraint] = [
            stack.leadingAnchor.constraint(equalTo: window.contentView!.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: window.contentView!.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: window.contentView!.topAnchor, constant: 16),
            stack.bottomAnchor.constraint(equalTo: window.contentView!.bottomAnchor, constant: -16),
            hint.widthAnchor.constraint(equalTo: stack.widthAnchor),
        ]
        for btn in profileButtons {
            constraints.append(btn.widthAnchor.constraint(equalTo: stack.widthAnchor))
        }
        NSLayoutConstraint.activate(constraints)
        window.delegate = self
    }

    required init?(coder: NSCoder) { fatalError() }

    @objc private func openProfile(_ sender: NSButton) {
        GeminiPromptEditDialog.show(profile: sender.tag)
    }
    @objc private func closeDlg() {
        window?.close()
    }

    func windowWillClose(_ notification: Notification) {
        NSApp.stopModal()
    }

    static func show() {
        let dlg = GeminiPromptListDialog()
        guard let w = dlg.window else { return }
        w.center()
        w.makeKeyAndOrderFront(nil)
        w.orderFrontRegardless()
        NSApp.runModal(for: w)
    }
}
