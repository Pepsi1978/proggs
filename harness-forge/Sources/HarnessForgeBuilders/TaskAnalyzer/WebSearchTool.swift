// WebSearchTool.swift
// Abstrakte Schnittstelle zu Web-Such-Providern (Tavily, Brave, SerpAPI, …).
// Default-Implementation liefert eine leere Liste — der TaskAnalyzer laeuft also
// auch komplett offline. Wer echte Web-Recherche moechte, reicht eine eigene
// Implementierung in den Analyzer-Konstruktor.

import Foundation

public struct WebSearchResult: Sendable, Codable, Equatable {
    public let title: String
    public let url: String
    public let snippet: String

    public init(title: String, url: String, snippet: String) {
        self.title = title
        self.url = url
        self.snippet = snippet
    }
}

public protocol WebSearchTool: Sendable {
    func search(query: String, maxResults: Int) async throws -> [WebSearchResult]
}

/// Default: kein Web-Zugriff. Liefert immer eine leere Ergebnisliste.
public struct NoWebSearchTool: WebSearchTool {
    public init() {}

    public func search(query: String, maxResults: Int) async throws -> [WebSearchResult] {
        []
    }
}
