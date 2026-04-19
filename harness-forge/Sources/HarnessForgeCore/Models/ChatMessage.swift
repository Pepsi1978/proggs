// ChatMessage.swift
// Model fuer eine einzelne Nachricht innerhalb einer Konversation.

import Foundation

/// Eine einzelne Nachricht in einem LLM-Dialog.
public struct ChatMessage: Sendable, Codable, Equatable {
    /// Rolle, aus der die Nachricht stammt.
    public enum Role: String, Sendable, Codable {
        case system
        case user
        case assistant
        case tool
    }

    public let role: Role
    public let content: String
    public let toolCallID: String?
    public let toolCalls: [ToolCall]

    public init(
        role: Role,
        content: String,
        toolCallID: String? = nil,
        toolCalls: [ToolCall] = []
    ) {
        self.role = role
        self.content = content
        self.toolCallID = toolCallID
        self.toolCalls = toolCalls
    }

    // MARK: - Convenience-Konstruktoren

    public static func system(_ content: String) -> ChatMessage {
        ChatMessage(role: .system, content: content)
    }

    public static func user(_ content: String) -> ChatMessage {
        ChatMessage(role: .user, content: content)
    }

    public static func assistant(_ content: String, toolCalls: [ToolCall] = []) -> ChatMessage {
        ChatMessage(role: .assistant, content: content, toolCalls: toolCalls)
    }

    public static func tool(callID: String, content: String) -> ChatMessage {
        ChatMessage(role: .tool, content: content, toolCallID: callID)
    }
}

/// Aufruf eines Tools durch das Modell.
public struct ToolCall: Sendable, Codable, Equatable {
    public let id: String
    public let name: String
    /// Argumente als JSON-String, werden je nach Backend serialisiert.
    public let argumentsJSON: String

    public init(id: String, name: String, argumentsJSON: String) {
        self.id = id
        self.name = name
        self.argumentsJSON = argumentsJSON
    }
}
