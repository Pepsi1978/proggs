import Foundation

/// Minimaler JSON-Baum mit STABILER Schluessel-Reihenfolge und JSONC-Unterstuetzung
/// (`//`- und `/* */`-Kommentare, nachgestellte Kommata).
///
/// **Referenztyp mit Absicht.** Der Windows-Launcher patcht die Konfig mit `System.Text.Json.Nodes`
/// (`JsonNode`), das ebenfalls ein Referenztyp ist:
/// `root.GetOrAddObject("provider").GetOrAddObject("openrouter")["models"] = ...` aendert dabei den
/// Baum an Ort und Stelle. Mit einem Wertetyp (struct/enum) wuerde jede Kette nur auf Kopien
/// arbeiten und der Patch still verpuffen - genau der Fehler, den der C#-Kommentar in PatchProvider
/// beschreibt. Deshalb ist JSONNode eine Klasse.
///
/// Warum nicht JSONSerialization? Der Launcher schreibt die vorhandene
/// ~/.config/opencode/opencode.jsonc zurueck. Ein Dictionary wuerde die Schluessel-Reihenfolge jedes
/// Mal neu wuerfeln - die Datei des Benutzers waere nach jedem Start umsortiert.
final class JSONNode {
    enum Kind {
        case object
        case array
        case string(String)
        case number(Double)
        case bool(Bool)
        case null
    }

    private(set) var kind: Kind
    /// Reihenfolge-erhaltende Eintraege eines Objekts.
    private(set) var entries: [(key: String, value: JSONNode)] = []
    private(set) var items: [JSONNode] = []

    init(_ kind: Kind) { self.kind = kind }

    static func object() -> JSONNode { JSONNode(.object) }
    static func array(_ items: [JSONNode] = []) -> JSONNode {
        let node = JSONNode(.array)
        node.items = items
        return node
    }
    static func string(_ value: String) -> JSONNode { JSONNode(.string(value)) }
    static func number(_ value: Double) -> JSONNode { JSONNode(.number(value)) }
    static func number(_ value: Int) -> JSONNode { JSONNode(.number(Double(value))) }
    static func bool(_ value: Bool) -> JSONNode { JSONNode(.bool(value)) }
    static let nullNode = JSONNode(.null)

    var isObject: Bool { if case .object = kind { return true }; return false }

    var stringValue: String? {
        if case .string(let value) = kind { return value }
        return nil
    }

    var intValue: Int? {
        if case .number(let value) = kind { return Int(value) }
        return nil
    }

    // ===================== Objekt-Zugriff =====================

    subscript(key: String) -> JSONNode? {
        get { entries.first { $0.key == key }?.value }
        set {
            guard isObject else { return }
            if let index = entries.firstIndex(where: { $0.key == key }) {
                if let newValue { entries[index] = (key, newValue) } else { entries.remove(at: index) }
            } else if let newValue {
                entries.append((key, newValue))
            }
        }
    }

    /// Liefert das Unterobjekt unter `key` und legt es an, falls es fehlt oder kein Objekt ist.
    /// Gegenstueck zu JsonExtensions.GetOrAddObject im Windows-Launcher.
    @discardableResult
    func getOrAddObject(_ key: String) -> JSONNode {
        if let existing = self[key], existing.isObject { return existing }
        let created = JSONNode.object()
        self[key] = created
        return created
    }

    /// Entfernt einen Schluessel; gibt zurueck, ob etwas entfernt wurde.
    @discardableResult
    func remove(_ key: String) -> Bool {
        guard let index = entries.firstIndex(where: { $0.key == key }) else { return false }
        entries.remove(at: index)
        return true
    }

    /// Stellt sicher, dass der Wert ein Objekt ist. Ist die Wurzel ausnahmsweise KEIN Objekt
    /// (korrupte opencode.json als Array/Skalar), entsteht ein frisches Objekt - der Aufrufer muss
    /// dieses Ergebnis zurueckschreiben, sonst ginge der Patch still verloren.
    static func ensureObject(_ node: JSONNode?) -> JSONNode {
        if let node, node.isObject { return node }
        return .object()
    }

    // ===================== Serialisierung =====================

    func serialized(indent: Int = 0) -> String {
        let pad = String(repeating: " ", count: indent)
        let innerPad = String(repeating: " ", count: indent + 2)
        switch kind {
        case .object:
            if entries.isEmpty { return "{}" }
            let body = entries
                .map { "\(innerPad)\(JSONNode.quote($0.key)): \($0.value.serialized(indent: indent + 2))" }
                .joined(separator: ",\n")
            return "{\n\(body)\n\(pad)}"
        case .array:
            if items.isEmpty { return "[]" }
            let body = items.map { "\(innerPad)\($0.serialized(indent: indent + 2))" }.joined(separator: ",\n")
            return "[\n\(body)\n\(pad)]"
        case .string(let text):
            return JSONNode.quote(text)
        case .number(let value):
            if value == value.rounded() && abs(value) < 1e15 { return String(Int64(value)) }
            return String(value)
        case .bool(let value):
            return value ? "true" : "false"
        case .null:
            return "null"
        }
    }

    /// JSON-String-Literal. Schraegstriche bleiben unmaskiert (wie
    /// JavaScriptEncoder.UnsafeRelaxedJsonEscaping im Windows-Launcher), damit Pfade und URLs in der
    /// Konfig lesbar bleiben.
    private static func quote(_ text: String) -> String {
        var out = "\""
        for scalar in text.unicodeScalars {
            switch scalar {
            case "\"": out += "\\\""
            case "\\": out += "\\\\"
            case "\n": out += "\\n"
            case "\r": out += "\\r"
            case "\t": out += "\\t"
            default:
                if scalar.value < 0x20 {
                    out += String(format: "\\u%04x", scalar.value)
                } else {
                    out.unicodeScalars.append(scalar)
                }
            }
        }
        return out + "\""
    }

    // ===================== Parser (JSONC) =====================

    static func parse(_ text: String) -> JSONNode? {
        var parser = JSONCParser(Array(text.unicodeScalars))
        return parser.parseValue()
    }
}

/// Handgeschriebener JSONC-Parser: erlaubt `//`- und `/* */`-Kommentare sowie nachgestellte Kommata
/// (beides kommt in opencode.jsonc vor) und behaelt die Reihenfolge der Objekt-Schluessel bei.
private struct JSONCParser {
    private let scalars: [Unicode.Scalar]
    private var index = 0

    init(_ scalars: [Unicode.Scalar]) {
        // BOM abschneiden: ein UTF-8-BOM bricht jeden Parse (Kurzcheck opencode-cli §10.3).
        self.scalars = scalars.first == "\u{FEFF}" ? Array(scalars.dropFirst()) : scalars
    }

    private var current: Unicode.Scalar? { index < scalars.count ? scalars[index] : nil }

    private mutating func skipTrivia() {
        while index < scalars.count {
            let c = scalars[index]
            if c == " " || c == "\n" || c == "\r" || c == "\t" {
                index += 1
            } else if c == "/" && index + 1 < scalars.count && scalars[index + 1] == "/" {
                while index < scalars.count && scalars[index] != "\n" { index += 1 }
            } else if c == "/" && index + 1 < scalars.count && scalars[index + 1] == "*" {
                index += 2
                while index + 1 < scalars.count && !(scalars[index] == "*" && scalars[index + 1] == "/") { index += 1 }
                index = Swift.min(index + 2, scalars.count)
            } else {
                return
            }
        }
    }

    mutating func parseValue() -> JSONNode? {
        skipTrivia()
        guard let c = current else { return nil }
        switch c {
        case "{": return parseObject()
        case "[": return parseArray()
        case "\"": return parseString().map { JSONNode.string($0) }
        case "t", "f": return parseBool()
        case "n": return parseNull()
        default: return parseNumber()
        }
    }

    private mutating func parseObject() -> JSONNode? {
        index += 1 // {
        let node = JSONNode.object()
        while true {
            skipTrivia()
            guard let c = current else { return nil }
            if c == "}" { index += 1; return node }
            if c == "," { index += 1; continue }
            guard let key = parseString() else { return nil }
            skipTrivia()
            guard current == ":" else { return nil }
            index += 1
            guard let value = parseValue() else { return nil }
            // Doppelte Schluessel: der letzte gewinnt (wie JsonNode).
            node[key] = value
        }
    }

    private mutating func parseArray() -> JSONNode? {
        index += 1 // [
        var items: [JSONNode] = []
        while true {
            skipTrivia()
            guard let c = current else { return nil }
            if c == "]" { index += 1; return JSONNode.array(items) }
            if c == "," { index += 1; continue }
            guard let value = parseValue() else { return nil }
            items.append(value)
        }
    }

    private mutating func parseString() -> String? {
        skipTrivia()
        guard current == "\"" else { return nil }
        index += 1
        var out = String.UnicodeScalarView()
        while index < scalars.count {
            let c = scalars[index]
            index += 1
            if c == "\"" { return String(out) }
            if c != "\\" { out.append(c); continue }

            guard index < scalars.count else { return nil }
            let escape = scalars[index]
            index += 1
            switch escape {
            case "n": out.append("\n")
            case "r": out.append("\r")
            case "t": out.append("\t")
            case "b": out.append("\u{08}")
            case "f": out.append("\u{0C}")
            case "u":
                guard index + 3 < scalars.count else { return nil }
                let hex = String(String.UnicodeScalarView(scalars[index..<(index + 4)]))
                index += 4
                guard var code = UInt32(hex, radix: 16) else { return nil }
                // Surrogatpaar zusammensetzen (Emoji u.ae.).
                if (0xD800...0xDBFF).contains(code),
                   index + 5 < scalars.count, scalars[index] == "\\", scalars[index + 1] == "u",
                   let low = UInt32(String(String.UnicodeScalarView(scalars[(index + 2)..<(index + 6)])), radix: 16),
                   (0xDC00...0xDFFF).contains(low) {
                    code = 0x10000 + ((code - 0xD800) << 10) + (low - 0xDC00)
                    index += 6
                }
                if let scalar = Unicode.Scalar(code) { out.append(scalar) }
            default:
                out.append(escape)
            }
        }
        return nil
    }

    private mutating func parseBool() -> JSONNode? {
        if matches("true") { return .bool(true) }
        if matches("false") { return .bool(false) }
        return nil
    }

    private mutating func parseNull() -> JSONNode? {
        matches("null") ? JSONNode(.null) : nil
    }

    private mutating func parseNumber() -> JSONNode? {
        var text = ""
        while index < scalars.count {
            let c = scalars[index]
            if (c >= "0" && c <= "9") || c == "-" || c == "+" || c == "." || c == "e" || c == "E" {
                text.unicodeScalars.append(c)
                index += 1
            } else {
                break
            }
        }
        guard let value = Double(text) else { return nil }
        return .number(value)
    }

    private mutating func matches(_ literal: String) -> Bool {
        let chars = Array(literal.unicodeScalars)
        guard index + chars.count <= scalars.count else { return false }
        for (offset, char) in chars.enumerated() where scalars[index + offset] != char { return false }
        index += chars.count
        return true
    }
}
