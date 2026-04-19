// Router.swift
// Actor, der alle registrierten Backends verwaltet und nach Task-Typ + Budget
// das passende auswaehlt. Durch Actor-Isolierung automatisch thread-safe.

import Foundation

/// Zentrale Anlaufstelle fuer alle KI-Aufrufe.
///
/// Der Router verwaltet registrierte `LLMClient`s und waehlt pro Anfrage das
/// passende Backend. Er ist als `actor` implementiert — alle Zugriffe sind
/// automatisch serialisiert, kein expliziter Lock noetig.
public actor Router {
    // MARK: - Interne Ablage

    private var backends: [String: LLMClient] = [:]

    public init() {}

    // MARK: - Registrierung

    public func register(_ backend: LLMClient) {
        backends[backend.identifier] = backend
    }

    public func unregister(identifier: String) {
        backends.removeValue(forKey: identifier)
    }

    public func listBackends() -> [String] {
        Array(backends.keys).sorted()
    }

    public func backend(identifier: String) throws -> LLMClient {
        guard let b = backends[identifier] else {
            throw LLMError.unknownBackend(identifier: identifier)
        }
        return b
    }

    // MARK: - Auswahl

    /// Aufgaben-Typ — leitet die Backend-Wahl.
    public enum TaskType: Sendable {
        case reasoning         // Komplexe Analyse, Strategie, Architektur
        case coding            // Code schreiben/modifizieren
        case summarization     // Zusammenfassen, Kondensieren
        case quickLookup       // Einfache Fragen, Definitionen
    }

    /// Kosten- und Prioritaeten-Budget fuer die Auswahl.
    public struct CostBudget: Sendable {
        public let maxTotalUSD: Double
        public let preferLocal: Bool

        public init(maxTotalUSD: Double = 10.0, preferLocal: Bool = false) {
            self.maxTotalUSD = maxTotalUSD
            self.preferLocal = preferLocal
        }

        public static let `default` = CostBudget()
    }

    /// Waehlt das passende Backend fuer den gegebenen Aufgabentyp.
    ///
    /// Heuristik:
    /// - `preferLocal` → LM Studio falls registriert
    /// - `reasoning`/`coding` → Anthropic > OpenAI > Gemini > LM Studio
    /// - `summarization`/`quickLookup` → guenstigstes Backend
    public func chooseBackend(
        for taskType: TaskType,
        budget: CostBudget = .default
    ) -> LLMClient? {
        let candidates = Array(backends.values)
        guard !candidates.isEmpty else { return nil }

        // Priorisiere lokale Modelle, wenn gewuenscht
        if budget.preferLocal, let local = candidates.first(where: { $0.identifier == "lmstudio" }) {
            return local
        }

        switch taskType {
        case .reasoning, .coding:
            // Reasoning/Coding: Qualitaet geht vor Kosten
            let preferredOrder = ["anthropic", "openai", "gemini", "lmstudio"]
            for id in preferredOrder {
                if let b = candidates.first(where: { $0.identifier == id }) {
                    return b
                }
            }
            return candidates.first

        case .summarization, .quickLookup:
            // Einfachere Aufgaben: niedrige Kosten bevorzugt
            return candidates.min { a, b in
                a.costPerMillionOutputTokens < b.costPerMillionOutputTokens
            }
        }
    }
}
