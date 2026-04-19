// ClientFactory.swift
// Sucht einen geeigneten LLM-Backend nach folgender Reihenfolge:
//   1) ANTHROPIC_API_KEY → AnthropicBackend
//   2) OPENAI_API_KEY    → OpenAIBackend
//   3) GEMINI_API_KEY    → GeminiBackend
//   4) Fallback          → LMStudioBackend (lokal auf :1234)

import Foundation
import HarnessForgeCore

enum ClientFactory {

    enum FactoryError: Error, LocalizedError {
        case noBackendAvailable

        var errorDescription: String? {
            switch self {
            case .noBackendAvailable:
                return """
                Kein KI-Backend verfuegbar. Setze eine dieser Umgebungsvariablen:
                  ANTHROPIC_API_KEY=sk-ant-...
                  OPENAI_API_KEY=sk-...
                  GEMINI_API_KEY=...
                Oder starte LM Studio lokal auf Port 1234.
                """
            }
        }
    }

    /// Waehlt ein Backend und gibt eine kurze Info-Zeile zurueck, welches.
    static func make() -> (client: any LLMClient, label: String) {
        let env = ProcessInfo.processInfo.environment
        if let key = env["ANTHROPIC_API_KEY"], !key.isEmpty {
            return (AnthropicBackend(apiKey: key), "Anthropic Claude")
        }
        if let key = env["OPENAI_API_KEY"], !key.isEmpty {
            return (OpenAIBackend(apiKey: key), "OpenAI GPT")
        }
        if let key = env["GEMINI_API_KEY"], !key.isEmpty {
            return (GeminiBackend(apiKey: key), "Google Gemini")
        }
        return (LMStudioBackend(), "LM Studio (lokal)")
    }

    /// Hilfsfunktion: prueft, ob ueberhaupt ein ENV-Key gesetzt ist.
    static func hasAnyKey() -> Bool {
        let env = ProcessInfo.processInfo.environment
        return ["ANTHROPIC_API_KEY", "OPENAI_API_KEY", "GEMINI_API_KEY"]
            .contains { env[$0]?.isEmpty == false }
    }
}
