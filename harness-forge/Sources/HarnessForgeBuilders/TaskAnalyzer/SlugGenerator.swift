// SlugGenerator.swift
// Wandelt eine freiformatige Aufgabenbeschreibung in einen Kurznamen um, der
// als Dateisystem-Ordner und Git-Branch tauglich ist.
// Regeln: lowercase, ASCII, kebab-case, max 40 Zeichen, keine Sonderzeichen.

import Foundation

public enum SlugGenerator {

    // MARK: - Oeffentliche API

    /// Erzeugt aus einer freiformatigen Aufgabe einen sauberen Slug.
    public static func generate(from taskDescription: String, maxLength: Int = 40) -> String {
        let normalized = foldUmlautsAndDiacritics(taskDescription)
        let kebabbed = normalized
            .lowercased()
            .replacingOccurrences(of: "[^a-z0-9]+", with: "-", options: .regularExpression)

        let trimmed = kebabbed.trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        let truncated = String(trimmed.prefix(maxLength))
            .trimmingCharacters(in: CharacterSet(charactersIn: "-"))

        return truncated.isEmpty ? "unnamed-harness" : truncated
    }

    /// Stellt Eindeutigkeit her: wenn `base` bereits existiert, werden `-2`, `-3`, …
    /// angehaengt, bis ein freier Name gefunden ist.
    public static func ensureUnique(base: String, existing: Set<String>) -> String {
        if !existing.contains(base) { return base }
        var index = 2
        while existing.contains("\(base)-\(index)") {
            index += 1
        }
        return "\(base)-\(index)"
    }

    // MARK: - Interne Helfer

    /// Faltet Umlaute und diakritische Zeichen zu ASCII-Aequivalenten.
    /// Ae → ae, ü → u, ß → ss, é → e, usw.
    private static func foldUmlautsAndDiacritics(_ input: String) -> String {
        var s = input
        let replacements: [(String, String)] = [
            ("ä", "ae"), ("ö", "oe"), ("ü", "ue"),
            ("Ä", "ae"), ("Ö", "oe"), ("Ü", "ue"),
            ("ß", "ss"),
            ("æ", "ae"), ("œ", "oe"),
            ("å", "a"), ("Å", "a"),
        ]
        for (from, to) in replacements {
            s = s.replacingOccurrences(of: from, with: to)
        }
        // Restliche Diakritika via NFKD-Zerlegung entfernen
        if let folded = s.applyingTransform(.stripDiacritics, reverse: false) {
            s = folded
        }
        return s
    }
}
