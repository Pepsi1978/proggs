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

            // Zusammenbau 1:1 wie Windows' PromptChainBuilder: leere bzw. nur aus
            // Leerzeichen bestehende Texte werden UEBERSPRUNGEN und jede Prompt-ID
            // hoechstens EINMAL aufgenommen. Ohne beides erzeugte ein leerer
            // Always-On-Eintrag ein " ;  ; " im Prefix, und ein doppelt
            // eingetragener Prompt stand zweimal im Text.
            var parts: [String] = []
            var seen = Set<UUID>()
            for prompt in prompts.filter(filter).sorted(by: { $0.sortOrder < $1.sortOrder }) {
                let text = prompt.effectiveText.trimmingCharacters(in: .whitespacesAndNewlines)
                if text.isEmpty { continue }
                guard seen.insert(prompt.id).inserted else { continue }
                parts.append(prompt.effectiveText)
            }
            return parts.joined(separator: " ; ")
        } catch {
            NSLog("AlwaysOnPrefixService: \(error.localizedDescription)")
            return ""
        }
    }
}
