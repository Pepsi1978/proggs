// Lesson.swift
// SwiftData-Model fuer eine gelernte Regel ("Never make that mistake again").

import Foundation
import SwiftData

/// Eine gelernte Regel aus frueheren Fehlern oder erfolgreichen Mustern.
/// Wird von der Reflection-Loop erzeugt und bei kuenftigen Entscheidungen
/// als Kontext an das Modell mitgegeben.
@Model
public final class Lesson {
    @Attribute(.unique) public var id: UUID
    public var discoveredAt: Date
    public var title: String
    /// Kontext, in dem die Regel entstanden ist.
    public var context: String
    /// Symptom — was aufgefallen ist.
    public var symptom: String
    /// Ursache — warum.
    public var rootCause: String
    /// Abhilfe — was aendern, damit es nicht wieder passiert.
    public var mitigation: String
    public var tags: [String]

    public init(
        id: UUID = UUID(),
        title: String,
        context: String,
        symptom: String,
        rootCause: String,
        mitigation: String,
        tags: [String] = []
    ) {
        self.id = id
        self.discoveredAt = Date()
        self.title = title
        self.context = context
        self.symptom = symptom
        self.rootCause = rootCause
        self.mitigation = mitigation
        self.tags = tags
    }
}
