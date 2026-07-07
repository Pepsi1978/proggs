import Foundation

/// Builds always-on prefix and suffix strings from the IsAlwaysOn prompts
/// in the shared SQLite database. Joined with " ; " — no trailing
/// separator on either side; the caller stitches them around the
/// dictated text and adds its own " ; " tail downstream.
///
/// Pre vs. Post split (since #1820): each always-on prompt has its own
/// `isPrePrompt` / `isPostPrompt` flags. A prompt with both flags set
/// shows up on both sides; a prompt with neither set falls back to
/// `pre` so legacy data and the historic "Always-On = Pre" expectation
/// keep working.
enum AlwaysOnPrefixService {

    /// Backwards-compatible single string. Returns whatever should sit
    /// IN FRONT of the dictated text — equivalent to old `build()`.
    static func build() -> String { buildPre() }

    static func buildPre() -> String {
        return buildJoined { p in
            // Default-to-pre for legacy data: a row with both flags off
            // (impossible via UI, but possible for old/imported rows)
            // is treated as Pre so it's not silently lost.
            p.isPrePrompt || (!p.isPrePrompt && !p.isPostPrompt)
        }
    }

    static func buildPost() -> String {
        return buildJoined { p in p.isPostPrompt }
    }

    private static func buildJoined(_ filter: (PBPrompt) -> Bool) -> String {
        do {
            let prompts = try PromptBoardStore.shared.allAlwaysOnPrompts()
            if prompts.isEmpty { return "" }
            return prompts
                .filter(filter)
                .sorted { $0.sortOrder < $1.sortOrder }
                .map { $0.effectiveText }
                .joined(separator: " ; ")
        } catch {
            NSLog("AlwaysOnPrefixService: \(error.localizedDescription)")
            return ""
        }
    }
}
