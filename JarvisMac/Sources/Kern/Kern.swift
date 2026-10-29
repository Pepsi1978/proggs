import Foundation
import Observation

typealias JSONObjekt = [String: Any]

enum JSON {
    static func daten(_ wert: Any) -> Data {
        (try? JSONSerialization.data(withJSONObject: wert, options: [.sortedKeys])) ?? Data("{}".utf8)
    }
    static func text(_ wert: Any, schoen: Bool = false) -> String {
        let opt: JSONSerialization.WritingOptions = schoen ? [.sortedKeys, .prettyPrinted, .withoutEscapingSlashes] : [.sortedKeys, .withoutEscapingSlashes]
        guard let d = try? JSONSerialization.data(withJSONObject: wert, options: opt) else { return "{}" }
        return String(decoding: d, as: UTF8.self)
    }
    static func objekt(_ daten: Data) -> JSONObjekt? {
        (try? JSONSerialization.jsonObject(with: daten)) as? JSONObjekt
    }
    static func objekt(_ text: String) -> JSONObjekt? { objekt(Data(text.utf8)) }
    static func wert(_ daten: Data) -> Any? { try? JSONSerialization.jsonObject(with: daten, options: [.fragmentsAllowed]) }
}

enum Pfade {
    static let heim = FileManager.default.homeDirectoryForCurrentUser
    static let unterstuetzung: URL = {
        let u = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("JarvisMac", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true)
        return u
    }()
    static var gespraeche: URL {
        let u = unterstuetzung.appendingPathComponent("Gespraeche", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true)
        return u
    }
    static var temp: URL {
        let u = FileManager.default.temporaryDirectory.appendingPathComponent("JarvisMac", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true)
        return u
    }
    static func sk(_ teil: String) -> URL { heim.appendingPathComponent("SK").appendingPathComponent(teil) }
}

enum AppInfo {
    static var version: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "dev" }
    static var stand: String { Bundle.main.object(forInfoDictionaryKey: "BuildTimestamp") as? String ?? "" }
}

// MARK: - Zugänge (Schlüssel). Liegen nur in ~/Library/Application Support/JarvisMac/zugang.json (0600), nie im Git.

struct Zugang: Codable, Equatable {
    var anthropic = ""
    var openai = ""
    var gemini = ""
    var google = ""      // Google Cloud TTS (Chirp 3 HD)
    var groq = ""
    var relay = ""       // MCP-Relay des Handy-Jarvis (https://…/j/<token>)

    static let datei = Pfade.unterstuetzung.appendingPathComponent("zugang.json")

    static func laden() -> Zugang {
        var z = (try? Data(contentsOf: datei)).flatMap { try? JSONDecoder().decode(Zugang.self, from: $0) } ?? Zugang()
        let vorher = z
        z.ergaenzeAusSK()
        if z != vorher { z.speichern() }
        return z
    }

    /// Erstbefüllung aus ~/SK (Regel 1: Zugangsdaten liegen dort). Vorhandene Werte bleiben.
    mutating func ergaenzeAusSK() {
        if let d = try? Data(contentsOf: Pfade.sk("VoiceAgent/keys.json")) {
            var roh = d
            if roh.starts(with: [0xEF, 0xBB, 0xBF]) { roh = roh.dropFirst(3) }
            if let o = JSON.objekt(roh) {
                func s(_ k: String) -> String { (o[k] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines) }
                if anthropic.isEmpty { anthropic = s("claude") }
                if openai.isEmpty { openai = s("openai") }
                if gemini.isEmpty { gemini = s("gemini") }
                if google.isEmpty { google = s("google") }
                if groq.isEmpty { groq = s("groq") }
            }
        }
        if relay.isEmpty, let u = try? String(contentsOf: Pfade.sk("Jarvis/plugin-url.txt"), encoding: .utf8) {
            relay = u.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    func speichern() {
        guard let d = try? JSONEncoder().encode(self) else { return }
        try? d.write(to: Zugang.datei, options: .atomic)
        try? FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: Zugang.datei.path)
    }
}

// MARK: - Logbuch (Aktivität)

struct LogEintrag: Identifiable, Codable {
    var id = UUID()
    var zeit = Date()
    var art: String      // fehler, werkzeug, gehirn, sprache, live, system
    var text: String
}

@MainActor @Observable
final class Logbuch {
    static let shared = Logbuch()
    private(set) var eintraege: [LogEintrag] = []
    private let datei = Pfade.unterstuetzung.appendingPathComponent("logbuch.txt")

    func schreib(_ art: String, _ text: String) {
        let e = LogEintrag(art: art, text: text)
        eintraege.append(e)
        if eintraege.count > 800 { eintraege.removeFirst(eintraege.count - 800) }
        let f = DateFormatter(); f.dateFormat = "dd.MM. HH:mm:ss"
        let zeile = "\(f.string(from: e.zeit)) [\(art)] \(text.replacingOccurrences(of: "\n", with: " ⏎ "))\n"
        if let h = try? FileHandle(forWritingTo: datei) {
            h.seekToEndOfFile(); h.write(Data(zeile.utf8)); try? h.close()
        } else {
            try? Data(zeile.utf8).write(to: datei)
        }
    }
}

func log(_ art: String, _ text: String) {
    Task { @MainActor in Logbuch.shared.schreib(art, text) }
}

// MARK: - Einstellungen

enum Anbieter: String, CaseIterable, Identifiable, Codable {
    case anthropic, openai
    var id: String { rawValue }
    var name: String { self == .anthropic ? "Anthropic (Claude)" : "OpenAI (ChatGPT-Konto oder API-Schlüssel)" }
}

struct ModellWahl: Identifiable, Hashable {
    let id: String
    let name: String
    let anbieter: Anbieter
    static let alle: [ModellWahl] = [
        .init(id: "claude-opus-5-5", name: "Claude Opus 5.5", anbieter: .anthropic),
        .init(id: "claude-sonnet-5-5", name: "Claude Sonnet 5.5", anbieter: .anthropic),
        .init(id: "claude-haiku-5-5", name: "Claude Haiku 5.5", anbieter: .anthropic),
        .init(id: "claude-fable-5-1", name: "Claude Fable 5.1", anbieter: .anthropic),
        .init(id: "gpt-6-astra", name: "GPT-6 Astra", anbieter: .openai),
        .init(id: "gpt-6.1-sol", name: "GPT-6.1 Sol", anbieter: .openai),
        .init(id: "gpt-6-luna", name: "GPT-6 Luna", anbieter: .openai),
        .init(id: "gpt-5.6-terra", name: "GPT-5.6 Terra", anbieter: .openai),
    ]
    static func zu(_ id: String) -> ModellWahl { alle.first { $0.id == id } ?? alle[0] }
}

@MainActor @Observable
final class Einstellungen {
    static let shared = Einstellungen()
    private let d = UserDefaults.standard

    var zugang = Zugang.laden() { didSet { zugang.speichern() } }

    var modell: String { didSet { d.set(modell, forKey: "modell") } }
    var effort: String { didSet { d.set(effort, forKey: "effort") } }
    var schrittLimit: Int { didSet { d.set(schrittLimit, forKey: "schrittLimit") } }
    var vorlesen: Bool { didSet { d.set(vorlesen, forKey: "vorlesen") } }
    var stimme: String { didSet { d.set(stimme, forKey: "stimme") } }
    var tempo: Double { didSet { d.set(tempo, forKey: "tempo") } }
    var weckwortAn: Bool { didSet { d.set(weckwortAn, forKey: "weckwortAn") } }
    var weckwort: String { didSet { d.set(weckwort, forKey: "weckwort") } }
    var transkription: String { didSet { d.set(transkription, forKey: "transkription") } }   // apple | groq
    var liveAnbieter: String { didSet { d.set(liveAnbieter, forKey: "liveAnbieter") } }     // openai | gemini
    var liveStimmeOpenAI: String { didSet { d.set(liveStimmeOpenAI, forKey: "liveStimmeOpenAI") } }
    var liveStimmeGemini: String { didSet { d.set(liveStimmeGemini, forKey: "liveStimmeGemini") } }
    var liveModellOpenAI: String { didSet { d.set(liveModellOpenAI, forKey: "liveModellOpenAI") } }
    var liveModellGemini: String { didSet { d.set(liveModellGemini, forKey: "liveModellGemini") } }
    var vollzugriff: Bool { didSet { d.set(vollzugriff, forKey: "vollzugriff") } }
    var lernenAn: Bool { didSet { d.set(lernenAn, forKey: "lernenAn") } }
    var globalesKuerzel: Bool { didSet { d.set(globalesKuerzel, forKey: "globalesKuerzel") } }

    init() {
        let d = UserDefaults.standard
        func s(_ k: String, _ v: String) -> String { d.string(forKey: k) ?? v }
        func b(_ k: String, _ v: Bool) -> Bool { d.object(forKey: k) as? Bool ?? v }
        modell = s("modell", "claude-opus-5-5")
        effort = s("effort", "medium")
        schrittLimit = d.object(forKey: "schrittLimit") as? Int ?? 40
        vorlesen = b("vorlesen", true)
        stimme = s("stimme", "de-DE-Chirp3-HD-Charon")
        tempo = d.object(forKey: "tempo") as? Double ?? 1.05
        weckwortAn = b("weckwortAn", true)
        weckwort = s("weckwort", "Hey Jarvis")
        transkription = s("transkription", "apple")
        liveAnbieter = s("liveAnbieter", "openai")
        liveStimmeOpenAI = s("liveStimmeOpenAI", "flitz")
        liveStimmeGemini = s("liveStimmeGemini", "Charon")
        liveModellOpenAI = s("liveModellOpenAI", "gpt-live-1")
        liveModellGemini = s("liveModellGemini", "gemini-3.8-live")
        vollzugriff = b("vollzugriff", true)
        lernenAn = b("lernenAn", true)
        globalesKuerzel = b("globalesKuerzel", true)
        if d.string(forKey: "liveAnbieter") == nil && zugang.openai.isEmpty && !zugang.gemini.isEmpty { liveAnbieter = "gemini" }
    }

    /// Hat Jarvis überhaupt einen Kopf? (Claude-Schlüssel, ChatGPT-Anmeldung oder OpenAI-Schlüssel)
    func gehirnZugang() async -> Bool {
        if !zugang.anthropic.isEmpty || !zugang.openai.isEmpty { return true }
        return await CodexAnmeldung.shared.angemeldet
    }

    var modellWahl: ModellWahl { ModellWahl.zu(modell) }
}
