// VerificationLayer.swift
// L4 — Verification: Silent-on-Success, Loud-on-Failure.
// Hooks pruefen einen Output. Nur fehlgeschlagene Hooks kommen zurueck —
// erfolgreiche sind unsichtbar. Das ist das dritte Leitprinzip aus AGENTS.md.

import Foundation

// MARK: - Hook-Protokoll

public protocol VerificationHook: Sendable {
    var name: String { get }
    /// Liefert `nil` bei Erfolg, `HookFailure` bei Misserfolg.
    func verify(_ output: String) async -> VerificationLayer.HookFailure?
}

// MARK: - Layer

public struct VerificationLayer: Sendable {

    public struct HookFailure: Sendable, Equatable {
        public let hookName: String
        public let reason: String

        public init(hookName: String, reason: String) {
            self.hookName = hookName
            self.reason = reason
        }
    }

    public let hooks: [any VerificationHook]

    public init(hooks: [any VerificationHook] = []) {
        self.hooks = hooks
    }

    /// Laesst alle Hooks parallel laufen und liefert NUR die fehlgeschlagenen zurueck.
    public func verify(_ output: String) async -> [HookFailure] {
        await withTaskGroup(of: HookFailure?.self) { group in
            for hook in hooks {
                group.addTask { await hook.verify(output) }
            }
            var failures: [HookFailure] = []
            for await failure in group where failure != nil {
                if let f = failure { failures.append(f) }
            }
            return failures
        }
    }
}

// MARK: - Eingebaute Hooks

/// Einfachster Hook: prueft ob der Output eine Mindestlaenge hat.
public struct MinimumLengthHook: VerificationHook {
    public let name = "min-length"
    public let minimumLength: Int

    public init(minimumLength: Int = 10) {
        self.minimumLength = minimumLength
    }

    public func verify(_ output: String) async -> VerificationLayer.HookFailure? {
        let trimmed = output.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.count < minimumLength {
            return VerificationLayer.HookFailure(
                hookName: name,
                reason: "Output nur \(trimmed.count) Zeichen, minimum \(minimumLength)"
            )
        }
        return nil
    }
}

/// Hook, der prueft ob der Output gueltiges JSON ist.
public struct ValidJSONHook: VerificationHook {
    public let name = "valid-json"

    public init() {}

    public func verify(_ output: String) async -> VerificationLayer.HookFailure? {
        guard let data = output.data(using: .utf8) else {
            return VerificationLayer.HookFailure(
                hookName: name,
                reason: "Output kann nicht als UTF-8 kodiert werden"
            )
        }
        do {
            _ = try JSONSerialization.jsonObject(with: data)
            return nil
        } catch {
            return VerificationLayer.HookFailure(
                hookName: name,
                reason: "Ungueltiges JSON: \(error.localizedDescription)"
            )
        }
    }
}

/// Hook, der prueft ob bestimmte Substrings NICHT im Output enthalten sind.
public struct ForbiddenSubstringsHook: VerificationHook {
    public let name = "forbidden-substrings"
    public let forbidden: [String]

    public init(forbidden: [String]) {
        self.forbidden = forbidden
    }

    public func verify(_ output: String) async -> VerificationLayer.HookFailure? {
        for bad in forbidden where output.contains(bad) {
            return VerificationLayer.HookFailure(
                hookName: name,
                reason: "Output enthaelt verbotene Zeichenkette: '\(bad)'"
            )
        }
        return nil
    }
}
