import Foundation

/// Builds the always-on prefix string by reading every IsAlwaysOn prompt
/// from the shared SQLite database and joining them with " ; ".
/// No trailing separator — the caller appends the dictated text directly
/// and adds its own " ; " tail via the existing insert pipeline.
enum AlwaysOnPrefixService {

    static func build() -> String {
        do {
            let prompts = try PromptBoardStore.shared.allAlwaysOnPrompts()
            if prompts.isEmpty { return "" }
            return prompts
                .sorted { $0.sortOrder < $1.sortOrder }
                .map { $0.effectiveText }
                .joined(separator: " ; ")
        } catch {
            NSLog("AlwaysOnPrefixService: \(error.localizedDescription)")
            return ""
        }
    }
}
