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

extension Bundle {
    /// Eine mitgelieferte Markdown-Datei aus den Ressourcen der App.
    func mitgeliefert(_ name: String) -> String {
        url(forResource: name, withExtension: "md").flatMap { try? String(contentsOf: $0, encoding: .utf8) } ?? "\(name).md fehlt im App-Bundle."
    }
}

enum AppInfo {
    static var version: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "dev" }
    static var stand: String { Bundle.main.object(forInfoDictionaryKey: "BuildTimestamp") as? String ?? "" }
}

// MARK: - Zugänge (Schlüssel). Liegen verschlüsselt im Tresor (Tresor.swift), nie im Klartext, nie im Git.

struct Zugang: Codable, Equatable {
    var anthropic = ""
    var openai = ""
    var gemini = ""
    var google = ""      // Google Cloud TTS (Chirp 3 HD)
    var groq = ""
    var relay = ""       // MCP-Relay des Handy-Jarvis (https://…/j/<token>)

    static let datei = Pfade.unterstuetzung.appendingPathComponent("zugang.json")

    static func laden() -> Zugang {
        var z = Tresor.lade(Zugang.self, name: "zugang", klartext: datei) ?? Zugang()
        let vorher = z
        z.ergaenzeAusSK()
        if z != vorher { z.speichern() }
        z.geheimnisseMelden()
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
        Tresor.sichere(self, name: "zugang")
        geheimnisseMelden()
    }

    private func geheimnisseMelden() {
        let token = relay.split(separator: "/").last.map(String.init) ?? ""
        Geheimnisse.setzen("zugang", [anthropic, openai, gemini, google, groq, relay, token])
    }
}

// MARK: - Logbuch (Aktivität)

struct LogEintrag: Identifiable, Codable {
    var id = UUID()
    var zeit = Date()
    var art: String      // fehler, werkzeug, gehirn, sprache, live, system, oberflaeche, gespraech, einstellung, tresor, ablage, gedaechtnis
    var text: String     // Was ist passiert
    var wo: String? = nil          // In welchem Bereich
    var werkzeug: String? = nil    // Eingesetztes Werkzeug
    var eingabe: String? = nil     // Argumente des Werkzeugs
    var ergebnis: String? = nil    // Was herauskam
    var fehler: String? = nil      // Fehlertext, sonst leer
    var dauerMs: Int? = nil

    var anzeige: String {
        var t = text
        if let wo { t += " · \(wo)" }
        if let dauerMs { t += " · \(dauerMs) ms" }
        if let fehler { t += "\nFehler: \(fehler)" } else if let ergebnis { t += "\n\(ergebnis.prefix(300))" }
        return t
    }
}

/// Ausführliches Logbuch für die spätere Auswertung durch eine KI: je Tag eine Datei mit einer JSON-Zeile je Ereignis (was, wo, wann,
/// Werkzeug, Ergebnis, Fehler, Dauer). Bleibt höchstens 30 Tage liegen, Schlüssel werden vor dem Schreiben geschwärzt.
@MainActor @Observable
final class Logbuch {
    static let shared = Logbuch()
    nonisolated static let aufbewahrungTage = 30
    nonisolated static let ordner: URL = {
        let u = Pfade.unterstuetzung.appendingPathComponent("Logbuch", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
        return u
    }()
    private(set) var eintraege: [LogEintrag] = []
    private var aufgeraeumtAm = ""
    private let tagFormat: DateFormatter = { let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"; return f }()
    private let zeitFormat: ISO8601DateFormatter = { let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]; f.timeZone = .current; return f }()

    init() {
        // Der alte Klartext-Mitschnitt ohne Altersgrenze entfällt.
        try? FileManager.default.removeItem(at: Pfade.unterstuetzung.appendingPathComponent("logbuch.txt"))
        aufraeumen()
    }

    func schreib(_ art: String, _ text: String, wo: String? = nil, werkzeug: String? = nil, eingabe: String? = nil,
                 ergebnis: String? = nil, fehler: String? = nil, dauerMs: Int? = nil) {
        func sauber(_ s: String?, _ grenze: Int) -> String? { s.map { String(Geheimnisse.schwaerzen($0).prefix(grenze)) } }
        let e = LogEintrag(art: art, text: sauber(text, 4000) ?? "", wo: wo, werkzeug: werkzeug, eingabe: sauber(eingabe, 2000),
                           ergebnis: sauber(ergebnis, 4000), fehler: sauber(fehler, 2000), dauerMs: dauerMs)
        eintraege.append(e)
        if eintraege.count > 800 { eintraege.removeFirst(eintraege.count - 800) }
        aufraeumen()
        var o: JSONObjekt = ["zeit": zeitFormat.string(from: e.zeit), "art": e.art, "was": e.text, "version": AppInfo.version]
        o["wo"] = e.wo ?? Self.ort(art); o["werkzeug"] = e.werkzeug; o["eingabe"] = e.eingabe
        o["ergebnis"] = e.ergebnis; o["fehler"] = e.fehler ?? (art == "fehler" ? e.text : nil); o["dauer_ms"] = e.dauerMs
        let datei = Self.ordner.appendingPathComponent(tagFormat.string(from: e.zeit) + ".jsonl")
        // Auf die Platte im Hintergrund: Der Hauptfaden wartet bei jedem Ereignis sonst auf Öffnen, Schreiben und Schließen.
        Self.schreiber.async {
            var zeile = JSON.daten(o); zeile.append(0x0A)
            if let h = try? FileHandle(forWritingTo: datei) {
                h.seekToEndOfFile(); h.write(zeile); try? h.close()
            } else {
                FileManager.default.createFile(atPath: datei.path, contents: zeile, attributes: [.posixPermissions: 0o600])
            }
        }
    }

    nonisolated static let schreiber = DispatchQueue(label: "jarvis.logbuch", qos: .utility)

    /// Einstellungen ändern sich beim Ziehen und Tippen im Takt: Eingetragen wird nur der Endwert nach einer Sekunde Ruhe.
    @ObservationIgnored private var offeneEinstellungen: [String: Task<Void, Never>] = [:]
    func einstellung(_ name: String, _ wert: String, wo: String) {
        offeneEinstellungen[name]?.cancel()
        offeneEinstellungen[name] = Task { [weak self] in
            try? await Task.sleep(for: .seconds(1))
            if !Task.isCancelled { self?.schreib("einstellung", "\(name) = \(wert)", wo: wo) }
        }
    }

    private static func ort(_ art: String) -> String {
        switch art {
        case "gehirn": return "Gehirn"
        case "sprache": return "Sprache"
        case "live": return "Live-Gespräch"
        case "werkzeug": return "Werkzeuge"
        default: return "App"
        }
    }

    /// Einmal am Tag: Dateien, deren Datum älter als 30 Tage ist, löschen.
    private func aufraeumen() {
        let heute = tagFormat.string(from: Date())
        guard heute != aufgeraeumtAm else { return }
        aufgeraeumtAm = heute
        let grenze = Date().addingTimeInterval(-Double(Self.aufbewahrungTage) * 86400)
        let dateien = (try? FileManager.default.contentsOfDirectory(at: Self.ordner, includingPropertiesForKeys: nil)) ?? []
        for d in dateien where d.pathExtension == "jsonl" {
            if let tag = tagFormat.date(from: d.deletingPathExtension().lastPathComponent), tag < grenze { try? FileManager.default.removeItem(at: d) }
        }
    }
}

func log(_ art: String, _ text: String, wo: String? = nil, werkzeug: String? = nil, eingabe: String? = nil,
         ergebnis: String? = nil, fehler: String? = nil, dauerMs: Int? = nil) {
    Task { @MainActor in Logbuch.shared.schreib(art, text, wo: wo, werkzeug: werkzeug, eingabe: eingabe, ergebnis: ergebnis, fehler: fehler, dauerMs: dauerMs) }
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

    var zugang = Zugang.laden() { didSet { speicherUhr?.cancel(); speicherUhr = Task { [weak self] in
        // Erst nach kurzer Tipp-Pause verschlüsseln und schreiben, nicht bei jedem Tastendruck.
        try? await Task.sleep(for: .seconds(0.7))
        if !Task.isCancelled { self?.zugangSichern() }
    } } }
    @ObservationIgnored private var speicherUhr: Task<Void, Never>?
    func zugangSichern() { speicherUhr?.cancel(); speicherUhr = nil; zugang.speichern() }

    var modell: String { didSet { d.set(modell, forKey: "modell"); Logbuch.shared.einstellung("modell", "\(modell)", wo: "Einstellungen") } }
    var effort: String { didSet { d.set(effort, forKey: "effort"); Logbuch.shared.einstellung("effort", "\(effort)", wo: "Einstellungen") } }
    var schrittLimit: Int { didSet { d.set(schrittLimit, forKey: "schrittLimit"); Logbuch.shared.einstellung("schrittLimit", "\(schrittLimit)", wo: "Einstellungen") } }
    var vorlesen: Bool { didSet { d.set(vorlesen, forKey: "vorlesen"); Logbuch.shared.einstellung("vorlesen", "\(vorlesen)", wo: "Einstellungen") } }
    var stimme: String { didSet { d.set(stimme, forKey: "stimme"); Logbuch.shared.einstellung("stimme", "\(stimme)", wo: "Einstellungen") } }
    var tempo: Double { didSet { d.set(tempo, forKey: "tempo"); Logbuch.shared.einstellung("tempo", "\(tempo)", wo: "Einstellungen") } }
    var weckwortAn: Bool { didSet { d.set(weckwortAn, forKey: "weckwortAn"); Logbuch.shared.einstellung("weckwortAn", "\(weckwortAn)", wo: "Einstellungen") } }
    var weckwort: String { didSet { d.set(weckwort, forKey: "weckwort"); Logbuch.shared.einstellung("weckwort", "\(weckwort)", wo: "Einstellungen") } }
    var transkription: String { didSet { d.set(transkription, forKey: "transkription"); Logbuch.shared.einstellung("transkription", "\(transkription)", wo: "Einstellungen") } }   // apple | groq
    var liveAnbieter: String { didSet { d.set(liveAnbieter, forKey: "liveAnbieter"); Logbuch.shared.einstellung("liveAnbieter", "\(liveAnbieter)", wo: "Einstellungen") } }     // openai | gemini
    var liveStimmeOpenAI: String { didSet { d.set(liveStimmeOpenAI, forKey: "liveStimmeOpenAI"); Logbuch.shared.einstellung("liveStimmeOpenAI", "\(liveStimmeOpenAI)", wo: "Einstellungen") } }
    var liveStimmeGemini: String { didSet { d.set(liveStimmeGemini, forKey: "liveStimmeGemini"); Logbuch.shared.einstellung("liveStimmeGemini", "\(liveStimmeGemini)", wo: "Einstellungen") } }
    var liveModellOpenAI: String { didSet { d.set(liveModellOpenAI, forKey: "liveModellOpenAI"); Logbuch.shared.einstellung("liveModellOpenAI", "\(liveModellOpenAI)", wo: "Einstellungen") } }
    var liveModellGemini: String { didSet { d.set(liveModellGemini, forKey: "liveModellGemini"); Logbuch.shared.einstellung("liveModellGemini", "\(liveModellGemini)", wo: "Einstellungen") } }
    /// Live-Gespräch endet nach so vielen Minuten Stille, 0 = nie.
    var liveStilleMinuten: Int { didSet { d.set(liveStilleMinuten, forKey: "liveStilleMinuten"); Logbuch.shared.einstellung("liveStilleMinuten", "\(liveStilleMinuten)", wo: "Einstellungen") } }
    var vollzugriff: Bool { didSet { d.set(vollzugriff, forKey: "vollzugriff"); Logbuch.shared.einstellung("vollzugriff", "\(vollzugriff)", wo: "Einstellungen") } }
    var lernenAn: Bool { didSet { d.set(lernenAn, forKey: "lernenAn"); Logbuch.shared.einstellung("lernenAn", "\(lernenAn)", wo: "Einstellungen") } }
    var globalesKuerzel: Bool { didSet { d.set(globalesKuerzel, forKey: "globalesKuerzel"); Logbuch.shared.einstellung("globalesKuerzel", "\(globalesKuerzel)", wo: "Einstellungen") } }

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
        liveStilleMinuten = d.object(forKey: "liveStilleMinuten") as? Int ?? 5
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
