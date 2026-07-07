// ContextLayer.swift
// L2 — Kontext: Laedt AGENTS.md-aehnliche Markdown-Dateien und filtert
// nach Relevanz. Progressive Disclosure: nur die passenden Chunks wandern
// ins Modell, nicht die ganze Datei.

import Foundation

public struct ContextLayer: Sendable {

    // MARK: - Public Types

    public struct Chunk: Sendable, Equatable {
        public let heading: String
        public let content: String
        public let level: Int

        public init(heading: String, content: String, level: Int) {
            self.heading = heading
            self.content = content
            self.level = level
        }

        /// Alle interessanten Woerter, nach denen relevantChunks(for:) sucht.
        public var keywords: Set<String> {
            Set(ContextLayer.keywords(from: heading + " " + content))
        }
    }

    public enum LoadError: Error, LocalizedError, Sendable {
        case fileNotFound(URL)
        case notReadable(URL)

        public var errorDescription: String? {
            switch self {
            case .fileNotFound(let url): "Datei nicht gefunden: \(url.path)"
            case .notReadable(let url): "Datei nicht lesbar: \(url.path)"
            }
        }
    }

    // MARK: - State

    public let chunks: [Chunk]

    public init(chunks: [Chunk] = []) {
        self.chunks = chunks
    }

    // MARK: - Laden

    /// Laedt eine Markdown-Datei und zerlegt sie an Headings (# bis ######) in Chunks.
    public static func loadMarkdown(from url: URL) throws -> ContextLayer {
        guard FileManager.default.fileExists(atPath: url.path) else {
            throw LoadError.fileNotFound(url)
        }
        guard let text = try? String(contentsOf: url, encoding: .utf8) else {
            throw LoadError.notReadable(url)
        }
        return parseMarkdown(text)
    }

    /// Parst einen Markdown-Text in Chunks. Oeffentlich fuer Tests.
    public static func parseMarkdown(_ text: String) -> ContextLayer {
        var chunks: [Chunk] = []
        var currentHeading = ""
        var currentLevel = 0
        var currentContent: [String] = []

        func flush() {
            if !currentContent.isEmpty || !currentHeading.isEmpty {
                chunks.append(Chunk(
                    heading: currentHeading,
                    content: currentContent.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines),
                    level: currentLevel
                ))
            }
        }

        for line in text.split(separator: "\n", omittingEmptySubsequences: false) {
            let lineStr = String(line)
            if let (level, heading) = parseHeading(lineStr) {
                flush()
                currentHeading = heading
                currentLevel = level
                currentContent = []
            } else {
                currentContent.append(lineStr)
            }
        }
        flush()

        return ContextLayer(chunks: chunks.filter { !$0.heading.isEmpty || !$0.content.isEmpty })
    }

    private static func parseHeading(_ line: String) -> (level: Int, heading: String)? {
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        guard trimmed.hasPrefix("#") else { return nil }
        var level = 0
        for ch in trimmed {
            if ch == "#" { level += 1 } else { break }
        }
        guard level >= 1 && level <= 6 else { return nil }
        let heading = String(trimmed.dropFirst(level)).trimmingCharacters(in: .whitespaces)
        return (level, heading)
    }

    // MARK: - Relevanz

    /// Findet die `max` Chunks, deren Keywords am staerksten mit den Query-Keywords
    /// ueberlappen. Sehr einfach — in spaeteren Versionen Embeddings nachruesten.
    public func relevantChunks(for query: String, max: Int = 5) -> [Chunk] {
        let queryKeywords = Set(Self.keywords(from: query))
        guard !queryKeywords.isEmpty else { return [] }

        return chunks
            .map { ($0, $0.keywords.intersection(queryKeywords).count) }
            .filter { $0.1 > 0 }
            .sorted { $0.1 > $1.1 }
            .prefix(max)
            .map { $0.0 }
    }

    /// Stellt aus den relevantesten Chunks einen Kontext-Prompt zusammen.
    public func assembleContext(for query: String, max: Int = 5) -> String {
        let relevant = relevantChunks(for: query, max: max)
        guard !relevant.isEmpty else { return "" }
        return relevant.map { chunk in
            "## \(chunk.heading)\n\(chunk.content)"
        }.joined(separator: "\n\n")
    }

    // MARK: - Interne Helfer

    static func keywords(from text: String) -> [String] {
        text
            .lowercased()
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { $0.count > 3 }
    }
}
