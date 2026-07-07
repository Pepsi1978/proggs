import Foundation

/// Plain-Swift mirrors of the PromptBoard entities used on Windows so the
/// SQLite file and the Google Drive backup JSON stay binary/wire compatible.
/// Field names match the Windows C# POCOs (Prompt, Category, AppSettings)
/// exactly, including casing after serialization — see PromptBoardStore
/// for the JSON coding keys.

struct PBCategory {
    var id: UUID
    var name: String
    var sortOrder: Int
    var backgroundColorHex: String
    var type: Int // 0 = Standard, 1 = Projects, 2 = AiLibrary
    var createdAt: Date
    var updatedAt: Date
}

struct PBPrompt {
    var id: UUID
    var categoryId: UUID
    var shortLabel: String
    var originalText: String
    var improvedText: String?
    var activeVersion: Int // 0 = Original, 1 = Improved
    var isAlwaysOn: Bool
    /// When `isAlwaysOn` is true and the user dictates, this prompt is
    /// prepended to the transcribed text iff `isPrePrompt` is also true.
    /// Default for legacy prompts that pre-date the Pre/Post split.
    var isPrePrompt: Bool
    /// When `isAlwaysOn` is true, this prompt is APPENDED after the
    /// transcribed text iff `isPostPrompt` is true. Independent of
    /// `isPrePrompt` — both can be on (prompt appears front and back).
    var isPostPrompt: Bool
    var sortOrder: Int
    var promptKind: String // "Prompt" or "AiImprovementPrompt"
    var geminiModel: String?
    var isActiveForImprovement: Bool
    var improvedByAiPromptId: UUID?
    /// Optional Cmd+N hotkey (1-9) that pastes this prompt's effective text
    /// into the active terminal. Nil when no hotkey is assigned. Globally
    /// unique — assigning a hotkey already in use strips it from the previous
    /// owner ("last wins"). Mirrors Windows `Prompt.HotkeyNumber`.
    var hotkeyNumber: Int?
    /// Optional Cmd+Opt+A..Z hotkey (single letter) — Windows-Pendant ist
    /// Win+Alt+A..Z. Nil wenn keiner zugewiesen. Wie hotkeyNumber: globally
    /// unique ("last wins").
    var hotkeyLetter: String?
    var createdAt: Date
    var updatedAt: Date

    /// The text that gets inserted on click — Original unless Improved is
    /// selected and non-empty. Matches PromptExtensions.EffectiveText.
    var effectiveText: String {
        if activeVersion == 1, let it = improvedText, !it.isEmpty { return it }
        return originalText
    }
}

struct PBAppSettings {
    var id: UUID
    var groqApiKey: String?
    var geminiApiKey: String?
    var googleOAuthRefreshToken: String?
    var googleClientId: String?
    var googleClientSecret: String?
    var googleAccountEmail: String?
    var groqModel: String
    var alwaysOnTop: Bool
    var barHeight: Double
    var separatorTemplate: String
    var createdAt: Date
    var updatedAt: Date

    // Windows-1:1-Felder (in PBAppSettings nachgezogen). Migration in
    // PromptBoardStore.swift fuegt die Spalten idempotent hinzu.
    /// Auto-Hide aktiv? Default true (Windows-Default).
    var autoHide: Bool = true
    /// "vertical" oder "horizontal" — die zuletzt aktive Form.
    var orientation: String = "vertical"
    /// Disketten-Position ueber App-Neustart hinweg merken? Default false.
    var persistOverlayPosition: Bool = false
    /// Persistierte Vertikal-Position (nur wenn persistOverlayPosition=true).
    var overlayVerticalLeft: Double? = nil
    var overlayVerticalTop: Double? = nil
    /// Persistierte Horizontal-Position.
    var overlayHorizontalLeft: Double? = nil
    var overlayHorizontalTop: Double? = nil
}
