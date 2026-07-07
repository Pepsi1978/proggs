// ConstraintLayer.swift
// L1 — Harte Constraints: Linter-Regeln, Schema-Validierung.
// "Silent on success, loud on failure": nur Verstoesse werden zurueckgegeben.

import Foundation

public struct ConstraintLayer: Sendable {

    // MARK: - Public Types

    public enum Severity: Sendable, Equatable {
        case info
        case warning
        case error
    }

    public struct Violation: Sendable, Equatable {
        public let ruleID: String
        public let message: String
        public let severity: Severity

        public init(ruleID: String, message: String, severity: Severity) {
            self.ruleID = ruleID
            self.message = message
            self.severity = severity
        }
    }

    public struct Rule: Sendable {
        public let id: String
        public let description: String
        public let severity: Severity
        public let check: @Sendable (String) -> Bool
        public let failureMessage: String

        /// `check` liefert `true` wenn die Regel eingehalten ist, `false` bei Verstoss.
        public init(
            id: String,
            description: String,
            severity: Severity = .error,
            failureMessage: String,
            check: @escaping @Sendable (String) -> Bool
        ) {
            self.id = id
            self.description = description
            self.severity = severity
            self.failureMessage = failureMessage
            self.check = check
        }
    }

    // MARK: - State

    public let rules: [Rule]

    public init(rules: [Rule] = []) {
        self.rules = rules
    }

    // MARK: - Validation

    /// Fuehrt alle Regeln aus und liefert nur die fehlgeschlagenen zurueck.
    public func validate(_ input: String) -> [Violation] {
        rules.compactMap { rule in
            rule.check(input)
                ? nil
                : Violation(ruleID: rule.id, message: rule.failureMessage, severity: rule.severity)
        }
    }

    /// Validiert, dass ein JSON-String (a) parsebar ist und (b) alle benoetigten
    /// Top-Level-Keys enthaelt. Sehr einfache Schema-Pruefung.
    public func validateJSONKeys(_ jsonString: String, requiredKeys: [String]) -> [Violation] {
        guard let data = jsonString.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return [Violation(
                ruleID: "json.parse",
                message: "Eingabe ist kein gueltiges JSON-Objekt",
                severity: .error
            )]
        }

        return requiredKeys.compactMap { key in
            obj[key] == nil ? Violation(
                ruleID: "json.missing-key",
                message: "Erforderlicher Key fehlt: '\(key)'",
                severity: .error
            ) : nil
        }
    }

    // MARK: - Eingebaute Standard-Regeln

    /// Regel: Kein leerer String.
    public static let nonEmpty = Rule(
        id: "input.non-empty",
        description: "Eingabe darf nicht leer sein",
        failureMessage: "Eingabe ist leer",
        check: { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    )

    /// Regel: Eingabe darf maximal N Zeichen haben.
    public static func maxLength(_ max: Int) -> Rule {
        Rule(
            id: "input.max-length.\(max)",
            description: "Eingabe darf max. \(max) Zeichen haben",
            severity: .warning,
            failureMessage: "Eingabe laenger als \(max) Zeichen"
        ) { $0.count <= max }
    }

    /// Regel: Eingabe darf KEINE der angegebenen Substrings enthalten.
    public static func forbidsSubstrings(_ forbidden: [String], ruleID: String = "input.forbidden") -> Rule {
        Rule(
            id: ruleID,
            description: "Eingabe darf nicht enthalten: \(forbidden.joined(separator: ", "))",
            failureMessage: "Eingabe enthaelt verbotene Zeichenkette"
        ) { input in
            !forbidden.contains { input.contains($0) }
        }
    }
}
