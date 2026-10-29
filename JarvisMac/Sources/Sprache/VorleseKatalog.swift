import Foundation

/// Vorlese-Anbieter wie in der Android-App (tts/TtsCatalog.kt) – gleiche Kennungen, Stimmen und Rückfallstufen.
enum VorleseAnbieter: String, CaseIterable, Identifiable, Sendable {
    case qwenKlon = "qwen_clone"
    case qwen = "qwen"
    case google = "google_cloud"
    case geminiFlash = "gemini_flash"
    case geminiLite = "gemini_lite"
    case edge = "edge_tts"
    case azure = "azure_neural"
    case system = "system"          // nur am Mac: beste deutsche Systemstimme, offline

    var id: String { rawValue }

    var titel: String {
        switch self {
        case .qwenKlon: return "Meine Stimme"
        case .qwen: return "Alibaba"
        case .google: return "Google Chirp 3 HD"
        case .geminiFlash: return "Gemini 3.8 Flash TTS"
        case .geminiLite: return "Gemini 3.8 Flash-Lite TTS"
        case .edge: return "Microsoft Edge"
        case .azure: return "Azure Neural"
        case .system: return "Systemstimme (offline)"
        }
    }

    var kurz: String {
        switch self {
        case .qwenKlon: return "Meine"
        case .qwen: return "Alibaba"
        case .google: return "Google"
        case .geminiFlash: return "Gemini 3.8"
        case .geminiLite: return "Gemini 3.8 Lite"
        case .edge: return "Edge"
        case .azure: return "Azure"
        case .system: return "System"
        }
    }

    var istGemini: Bool { self == .geminiFlash || self == .geminiLite }

    /// Wertet das Sprechtempo aus (Gemini über das Strecken des fertigen Tons); Alibaba spricht im eigenen Tempo.
    var kenntTempo: Bool { self == .google || self == .edge || self == .azure || self == .system || istGemini }

    var beschreibung: String {
        switch self {
        case .edge: return "Kostenlos, ohne Schlüssel – der garantierte Rückfall."
        case .google: return "Sehr natürliche deutsche Stimmen, eigener Google-Cloud-Schlüssel."
        case .geminiFlash: return "Gemini 3.8 Flash TTS, 30 Stimmen, kostenloser und/oder bezahlter AI-Studio-Schlüssel."
        case .geminiLite: return "Gemini 3.8 Flash-Lite TTS, schneller und günstiger, gleiche 30 Stimmen."
        case .azure: return "Azure AI Speech mit eigenem Schlüssel und Region, 17 Neural-Stimmen plus HD (HD nur Stufe S0)."
        case .qwen: return "Alibaba Qwen TTS Standardstimmen (DashScope-Schlüssel)."
        case .qwenKlon: return "Deine geklonte Stimme aus dem Alibaba-Konto (DashScope-Schlüssel)."
        case .system: return "Apples beste deutsche Stimme, ohne Netz."
        }
    }
}

enum Geschlecht: Sendable { case weiblich, maennlich }

struct VorleseStimme: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let geschlecht: Geschlecht
}

/// Eine Stufe der Vorlese-Rückfallkette; `schluessel` nur bei Gemini: „frei“ oder „bezahlt“.
struct RueckfallStufe: Identifiable, Hashable, Sendable {
    let id: String
    let anbieter: VorleseAnbieter
    let schluessel: String?
    let titel: String
    static let frei = "frei"
    static let bezahlt = "bezahlt"
}

enum VorleseKatalog {
    static let standardEdge = "de-DE-SeraphinaMultilingualNeural"
    static let standardGoogle = "de-DE-Chirp3-HD-Kore"
    static let standardQwen = "Cherry"
    static let standardGeminiFlash = "gemini_flash:Leda"
    static let standardGeminiLite = "gemini_lite:Leda"
    static let standardAzure = "azure:de-DE-SeraphinaMultilingualNeural"
    static let standardAzureRegion = "westeurope"

    static let geminiFlashModell = "gemini-3.8-flash-tts"
    static let geminiLiteModell = "gemini-3.8-flash-lite-tts"
    static let qwenModell = "qwen3-tts-flash"
    static let qwenKlonModell = "qwen3-tts-vc-2026-01-22"

    static let edgeStimmen: [VorleseStimme] = [
        .init(id: "de-DE-SeraphinaMultilingualNeural", name: "Seraphina", geschlecht: .weiblich),
        .init(id: "de-DE-KatjaNeural", name: "Katja", geschlecht: .weiblich),
        .init(id: "de-DE-AmalaNeural", name: "Amala", geschlecht: .weiblich),
        .init(id: "de-DE-FlorianMultilingualNeural", name: "Florian", geschlecht: .maennlich),
        .init(id: "de-DE-KillianNeural", name: "Killian", geschlecht: .maennlich),
        .init(id: "de-DE-ConradNeural", name: "Conrad", geschlecht: .maennlich),
    ]

    static let qwenStimmen: [VorleseStimme] = [
        .init(id: "Cherry", name: "Cherry", geschlecht: .weiblich),
        .init(id: "Chelsie", name: "Chelsie", geschlecht: .weiblich),
        .init(id: "Serena", name: "Serena", geschlecht: .weiblich),
        .init(id: "Jada", name: "Jada", geschlecht: .weiblich),
        .init(id: "Sunny", name: "Sunny", geschlecht: .weiblich),
        .init(id: "Ethan", name: "Ethan", geschlecht: .maennlich),
        .init(id: "Dylan", name: "Dylan", geschlecht: .maennlich),
    ]

    private static let chirpWeiblich = ["Achernar", "Aoede", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux", "Kore",
                                        "Laomedeia", "Leda", "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr"]
    private static let chirpMaennlich = ["Achird", "Algenib", "Algieba", "Alnilam", "Charon", "Enceladus", "Fenrir", "Iapetus",
                                         "Orus", "Puck", "Rasalgethi", "Sadachbia", "Sadaltager", "Schedar", "Umbriel", "Zubenelgenubi"]

    static let googleStimmen: [VorleseStimme] =
        chirpWeiblich.map { .init(id: "de-DE-Chirp3-HD-\($0)", name: $0, geschlecht: .weiblich) }
        + chirpMaennlich.map { .init(id: "de-DE-Chirp3-HD-\($0)", name: $0, geschlecht: .maennlich) }

    /// Gemini 3.8 TTS hat dieselben 30 Stimmnamen wie Chirp 3 HD; die Kennung trägt den Anbieter vorn („gemini_flash:Leda“).
    static let geminiFlashStimmen = googleStimmen.map { VorleseStimme(id: "gemini_flash:\($0.name)", name: $0.name, geschlecht: $0.geschlecht) }
    static let geminiLiteStimmen = googleStimmen.map { VorleseStimme(id: "gemini_lite:\($0.name)", name: $0.name, geschlecht: $0.geschlecht) }

    static func geminiName(_ id: String) -> String { id.components(separatedBy: ":").last ?? id }
    /// Dieselbe Stimme bei Chirp 3 HD (Rückfall, wenn der kostenlose Gemini-Schlüssel scheitert).
    static func chirpZu(_ id: String) -> String { "de-DE-Chirp3-HD-\(geminiName(id))" }

    static let azureStimmen: [VorleseStimme] = [
        .init(id: "azure:de-DE-Seraphina:DragonHDLatestNeural", name: "Seraphina HD", geschlecht: .weiblich),
        .init(id: "azure:de-DE-Florian:DragonHDLatestNeural", name: "Florian HD", geschlecht: .maennlich),
        .init(id: "azure:de-DE-SeraphinaMultilingualNeural", name: "Seraphina", geschlecht: .weiblich),
        .init(id: "azure:de-DE-KatjaNeural", name: "Katja", geschlecht: .weiblich),
        .init(id: "azure:de-DE-AmalaNeural", name: "Amala", geschlecht: .weiblich),
        .init(id: "azure:de-DE-ElkeNeural", name: "Elke", geschlecht: .weiblich),
        .init(id: "azure:de-DE-GiselaNeural", name: "Gisela (Kind)", geschlecht: .weiblich),
        .init(id: "azure:de-DE-KlarissaNeural", name: "Klarissa", geschlecht: .weiblich),
        .init(id: "azure:de-DE-LouisaNeural", name: "Louisa", geschlecht: .weiblich),
        .init(id: "azure:de-DE-MajaNeural", name: "Maja", geschlecht: .weiblich),
        .init(id: "azure:de-DE-TanjaNeural", name: "Tanja", geschlecht: .weiblich),
        .init(id: "azure:de-DE-FlorianMultilingualNeural", name: "Florian", geschlecht: .maennlich),
        .init(id: "azure:de-DE-ConradNeural", name: "Conrad", geschlecht: .maennlich),
        .init(id: "azure:de-DE-KillianNeural", name: "Killian", geschlecht: .maennlich),
        .init(id: "azure:de-DE-BerndNeural", name: "Bernd", geschlecht: .maennlich),
        .init(id: "azure:de-DE-ChristophNeural", name: "Christoph", geschlecht: .maennlich),
        .init(id: "azure:de-DE-KasperNeural", name: "Kasper", geschlecht: .maennlich),
        .init(id: "azure:de-DE-KlausNeural", name: "Klaus", geschlecht: .maennlich),
        .init(id: "azure:de-DE-RalfNeural", name: "Ralf", geschlecht: .maennlich),
    ]

    static func azureName(_ id: String) -> String { id.hasPrefix("azure:") ? String(id.dropFirst(6)) : id }
    static func istAzureHd(_ id: String) -> Bool { id.contains("DragonHD") }

    /// Regionsname („westeurope“); nimmt auch einen eingefügten Endpunkt aus dem Azure-Portal. nil = unbrauchbar.
    static func azureRegion(_ eingabe: String) -> String? {
        var roh = eingabe.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if roh.isEmpty { roh = standardAzureRegion }
        if let r = roh.range(of: #"^(?:https?://)?([a-z0-9]+)\.(?:api\.cognitive|tts\.speech)\."#, options: .regularExpression) {
            let teil = String(roh[r]).replacingOccurrences(of: "https://", with: "").replacingOccurrences(of: "http://", with: "")
            roh = String(teil.prefix { $0 != "." })
        }
        return roh.range(of: #"^[a-z0-9]{2,40}$"#, options: .regularExpression) != nil ? roh : nil
    }

    static let rueckfallStufen: [RueckfallStufe] = [
        .init(id: "gemini_flash:frei", anbieter: .geminiFlash, schluessel: RueckfallStufe.frei, titel: "Gemini 3.8 Flash TTS (kostenlos)"),
        .init(id: "gemini_flash:bezahlt", anbieter: .geminiFlash, schluessel: RueckfallStufe.bezahlt, titel: "Gemini 3.8 Flash TTS (bezahlt)"),
        .init(id: "gemini_lite:frei", anbieter: .geminiLite, schluessel: RueckfallStufe.frei, titel: "Gemini 3.8 Flash-Lite TTS (kostenlos)"),
        .init(id: "gemini_lite:bezahlt", anbieter: .geminiLite, schluessel: RueckfallStufe.bezahlt, titel: "Gemini 3.8 Flash-Lite TTS (bezahlt)"),
        .init(id: VorleseAnbieter.google.id, anbieter: .google, schluessel: nil, titel: VorleseAnbieter.google.titel),
        .init(id: VorleseAnbieter.azure.id, anbieter: .azure, schluessel: nil, titel: VorleseAnbieter.azure.titel),
        .init(id: VorleseAnbieter.edge.id, anbieter: .edge, schluessel: nil, titel: VorleseAnbieter.edge.titel),
        .init(id: VorleseAnbieter.qwen.id, anbieter: .qwen, schluessel: nil, titel: VorleseAnbieter.qwen.titel),
        .init(id: VorleseAnbieter.qwenKlon.id, anbieter: .qwenKlon, schluessel: nil, titel: VorleseAnbieter.qwenKlon.titel),
    ]

    static func rueckfallStufe(_ id: String) -> RueckfallStufe? { rueckfallStufen.first { $0.id == id } }

    /// Feste Stimmen eines Anbieters (die geklonten kommen aus dem Alibaba-Konto, siehe VorleseEinstellungen.klonStimmen).
    static func stimmen(_ a: VorleseAnbieter) -> [VorleseStimme] {
        switch a {
        case .edge: return edgeStimmen
        case .google: return googleStimmen
        case .geminiFlash: return geminiFlashStimmen
        case .geminiLite: return geminiLiteStimmen
        case .azure: return azureStimmen
        case .qwen: return qwenStimmen
        case .qwenKlon: return []
        case .system: return [.init(id: "system", name: "Beste deutsche Systemstimme", geschlecht: .weiblich)]
        }
    }

    /// Anbieter zu einer Stimm-Kennung aus dem festen Katalog.
    static func anbieterZu(_ id: String) -> VorleseAnbieter? {
        if id == "system" { return .system }
        for a in VorleseAnbieter.allCases where stimmen(a).contains(where: { $0.id == id }) { return a }
        if id.hasPrefix("qwen-tts-vc-") { return .qwenKlon }
        return nil
    }

    /// Anzeigename einer geklonten Stimme („qwen-tts-vc-FrankHD-voice-2026…“ → „Frank HD“).
    static func klonName(_ id: String) -> String {
        guard let r = id.range(of: #"^qwen-tts-vc-(.+?)-voice-"#, options: .regularExpression) else { return id }
        var name = String(id[r]).replacingOccurrences(of: "qwen-tts-vc-", with: "")
        if name.hasSuffix("-voice-") { name = String(name.dropLast(7)) }
        // Alibaba erlaubt keine Leerzeichen: vor einem Großbuchstaben nach einem Kleinbuchstaben trennen.
        var aus = ""
        var vorher: Character?
        for z in name {
            if let v = vorher, v.isLowercase, z.isUppercase { aus.append(" ") }
            aus.append(z); vorher = z
        }
        return aus
    }
}
