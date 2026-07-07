// ExecutionLayer.swift
// L3 — Execution: Tool-Registry mit atomaren Werkzeugen.
// Actor, um konkurrierende Zugriffe aus mehreren Tasks zu serialisieren.

import Foundation
import HarnessForgeCore

public actor ExecutionLayer {

    // MARK: - Public Types

    public struct Tool: Sendable {
        public let name: String
        public let description: String
        public let parametersJSONSchema: String
        public let execute: @Sendable (String) async throws -> String

        public init(
            name: String,
            description: String,
            parametersJSONSchema: String = "{}",
            execute: @escaping @Sendable (String) async throws -> String
        ) {
            self.name = name
            self.description = description
            self.parametersJSONSchema = parametersJSONSchema
            self.execute = execute
        }

        /// Exportiert diese Tool-Definition fuer das Modell (wird an LLMRequest weitergegeben).
        public var toolDefinition: ToolDefinition {
            ToolDefinition(
                name: name,
                description: description,
                parametersJSONSchema: parametersJSONSchema
            )
        }
    }

    public enum ExecutionError: Error, LocalizedError, Sendable {
        case unknownTool(String)
        case executionFailed(tool: String, message: String)

        public var errorDescription: String? {
            switch self {
            case .unknownTool(let name): "Unbekanntes Tool: '\(name)'"
            case .executionFailed(let tool, let msg): "Tool '\(tool)' fehlgeschlagen: \(msg)"
            }
        }
    }

    // MARK: - State

    private var tools: [String: Tool] = [:]
    private var invocationCount: [String: Int] = [:]

    public init() {}

    // MARK: - Registrierung

    public func register(_ tool: Tool) {
        tools[tool.name] = tool
    }

    public func unregister(name: String) {
        tools.removeValue(forKey: name)
    }

    public func listTools() -> [ToolDefinition] {
        tools.values
            .sorted { $0.name < $1.name }
            .map(\.toolDefinition)
    }

    public func hasTool(name: String) -> Bool {
        tools[name] != nil
    }

    // MARK: - Aufruf

    /// Ruft das Tool auf und traegt die Aufrufzahl nach.
    public func invoke(name: String, argumentsJSON: String) async throws -> String {
        guard let tool = tools[name] else {
            throw ExecutionError.unknownTool(name)
        }
        invocationCount[name, default: 0] += 1
        do {
            return try await tool.execute(argumentsJSON)
        } catch {
            throw ExecutionError.executionFailed(tool: name, message: error.localizedDescription)
        }
    }

    public func invocations(for name: String) -> Int {
        invocationCount[name, default: 0]
    }

    public func totalInvocations() -> Int {
        invocationCount.values.reduce(0, +)
    }
}
