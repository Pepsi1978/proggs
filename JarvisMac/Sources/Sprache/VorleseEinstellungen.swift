import Foundation
import Observation

/// Schlüssel nur fürs Vorlesen. Liegen verschlüsselt im Tresor (Tresor.swift), nie im Klartext, nie im Git.
struct VorleseZugang: Codable, Equatable, Sendable {
    var google = ""          // Google Cloud TTS (Chirp 3 HD)
    var geminiFrei = ""      // AI-Studio-Projekt ohne Abrechnung
    var geminiBezahlt = ""   // AI-Studio-Projekt mit Abrechnung
    var azure = ""           // Azure Speech-Schlüssel
    var azureRegion = VorleseKatalog.standardAzureRegion
    var qwen = ""            // Alibaba DashScope

    static let datei = Pfade.unterstuetzung.appendingPathComponent("vorlese-zugang.json")

    @MainActor static func laden() -> VorleseZugang {
        var z = Tresor.lade(VorleseZugang.self, name: "vorlese-zugang", klartext: datei)
        let neu = z == nil
        if z == nil { z = VorleseZugang() }
        // Erstbefüllung aus den allgemeinen Zugängen (aus ~/SK übernommen).
        let allg = Einstellungen.shared.zugang
        if z!.google.isEmpty { z!.google = allg.google }
        if neu && z!.geminiFrei.isEmpty { z!.geminiFrei = allg.gemini }
        z!.speichern()
        return z!
    }

    func speichern() {
        Tresor.sichere(self, name: "vorlese-zugang")
        Geheimnisse.setzen("vorlese", [google, geminiFrei, geminiBezahlt, azure, qwen])
    }
}

/// Eine geklonte Stimme aus dem Alibaba-Konto.
struct KlonStimme: Codable, Hashable, Identifiable, Sendable {
    let id: String
    let name: String
    let erstellt: String
}

/// Alles, was ein Vorleseauftrag braucht – als Sendable-Abzug für die Synthese außerhalb des Hauptthreads.
struct VorleseStand: Sendable {
    var anbieter: VorleseAnbieter
    var stimmen: [VorleseAnbieter: String]
    var tempo: [VorleseAnbieter: Double]
    var zugang: VorleseZugang
    var geminiWahl: String
    var kette: [RueckfallStufe]
    var immerDeutsch: Bool

    func stimme(_ a: VorleseAnbieter) -> String { stimmen[a] ?? "" }
    func tempo(_ a: VorleseAnbieter) -> Double { tempo[a] ?? 1 }
}

/// Vorlese-Einstellungen wie in der Android-App (SecureSettings): Anbieter, Stimme und Tempo je Anbieter,
/// Gemini-Schlüsselwahl, Rückfallkette, Favoriten, „immer Deutsch“.
@MainActor @Observable final class VorleseEinstellungen {
    static let shared = VorleseEinstellungen()
    static let geminiFreiDannBezahlt = "frei_dann_bezahlt"
    static let geminiNurFrei = "nur_frei"
    static let geminiNurBezahlt = "nur_bezahlt"
    static let tempoMin = 0.5
    static let tempoMax = 2.0

    private let d = UserDefaults.standard

    var zugang: VorleseZugang { didSet { zugang.speichern() } }

    var anbieter: VorleseAnbieter { didSet { d.set(anbieter.rawValue, forKey: "vl_anbieter") } }
    var edgeStimme: String { didSet { d.set(edgeStimme, forKey: "vl_edge_stimme"); log("einstellung", "vl_edge_stimme = \(edgeStimme)", wo: "Einstellungen/Stimme") } }
    var googleStimme: String { didSet { d.set(googleStimme, forKey: "vl_google_stimme"); log("einstellung", "vl_google_stimme = \(googleStimme)", wo: "Einstellungen/Stimme") } }
    var geminiFlashStimme: String { didSet { d.set(geminiFlashStimme, forKey: "vl_gemini_flash_stimme"); log("einstellung", "vl_gemini_flash_stimme = \(geminiFlashStimme)", wo: "Einstellungen/Stimme") } }
    var geminiLiteStimme: String { didSet { d.set(geminiLiteStimme, forKey: "vl_gemini_lite_stimme"); log("einstellung", "vl_gemini_lite_stimme = \(geminiLiteStimme)", wo: "Einstellungen/Stimme") } }
    var azureStimme: String { didSet { d.set(azureStimme, forKey: "vl_azure_stimme"); log("einstellung", "vl_azure_stimme = \(azureStimme)", wo: "Einstellungen/Stimme") } }
    var qwenStimme: String { didSet { d.set(qwenStimme, forKey: "vl_qwen_stimme"); log("einstellung", "vl_qwen_stimme = \(qwenStimme)", wo: "Einstellungen/Stimme") } }
    var klonStimme: String { didSet { d.set(klonStimme, forKey: "vl_klon_stimme"); log("einstellung", "vl_klon_stimme = \(klonStimme)", wo: "Einstellungen/Stimme") } }
    var geminiWahl: String { didSet { d.set(geminiWahl, forKey: "vl_gemini_wahl"); log("einstellung", "vl_gemini_wahl = \(geminiWahl)", wo: "Einstellungen/Stimme") } }
    /// Kennungen aus VorleseKatalog.rueckfallStufen in ihrer Reihenfolge.
    var kette: [String] { didSet { d.set(kette, forKey: "vl_kette"); log("einstellung", "vl_kette = \(kette)", wo: "Einstellungen/Stimme") } }
    var favoriten: Set<String> { didSet { d.set(Array(favoriten), forKey: "vl_favoriten") } }
    var immerDeutsch: Bool { didSet { d.set(immerDeutsch, forKey: "vl_immer_deutsch"); log("einstellung", "vl_immer_deutsch = \(immerDeutsch)", wo: "Einstellungen/Stimme") } }
    var klonStimmen: [KlonStimme] = []
    var klonFehler: String?
    private var tempoJe: [String: Double] { didSet { d.set(tempoJe, forKey: "vl_tempo"); log("einstellung", "vl_tempo = \(tempoJe)", wo: "Einstellungen/Stimme") } }

    init() {
        let d = UserDefaults.standard
        let alt = Einstellungen.shared
        zugang = VorleseZugang.laden()
        let vorgabeAnbieter: VorleseAnbieter = (alt.stimme == "system") ? .system
            : (!Einstellungen.shared.zugang.google.isEmpty ? .google : .edge)
        anbieter = d.string(forKey: "vl_anbieter").flatMap(VorleseAnbieter.init(rawValue:)) ?? vorgabeAnbieter
        edgeStimme = d.string(forKey: "vl_edge_stimme") ?? VorleseKatalog.standardEdge
        googleStimme = d.string(forKey: "vl_google_stimme") ?? (alt.stimme.contains("Chirp3") ? alt.stimme : VorleseKatalog.standardGoogle)
        geminiFlashStimme = d.string(forKey: "vl_gemini_flash_stimme") ?? VorleseKatalog.standardGeminiFlash
        geminiLiteStimme = d.string(forKey: "vl_gemini_lite_stimme") ?? VorleseKatalog.standardGeminiLite
        azureStimme = d.string(forKey: "vl_azure_stimme") ?? VorleseKatalog.standardAzure
        qwenStimme = d.string(forKey: "vl_qwen_stimme") ?? VorleseKatalog.standardQwen
        klonStimme = d.string(forKey: "vl_klon_stimme") ?? ""
        geminiWahl = d.string(forKey: "vl_gemini_wahl") ?? Self.geminiFreiDannBezahlt
        kette = (d.stringArray(forKey: "vl_kette") ?? []).filter { VorleseKatalog.rueckfallStufe($0) != nil }
        favoriten = Set(d.stringArray(forKey: "vl_favoriten") ?? [])
        immerDeutsch = d.object(forKey: "vl_immer_deutsch") as? Bool ?? true
        tempoJe = (d.dictionary(forKey: "vl_tempo") as? [String: Double]) ?? [:]
    }

    // MARK: Stimme je Anbieter

    func stimme(_ a: VorleseAnbieter) -> String {
        switch a {
        case .edge: return edgeStimme
        case .google: return googleStimme
        case .geminiFlash: return geminiFlashStimme
        case .geminiLite: return geminiLiteStimme
        case .azure: return azureStimme
        case .qwen: return qwenStimme
        case .qwenKlon: return klonStimme
        case .system: return "system"
        }
    }

    func setzeStimme(_ id: String, fuer a: VorleseAnbieter) {
        switch a {
        case .edge: edgeStimme = id
        case .google: googleStimme = id
        case .geminiFlash: geminiFlashStimme = id
        case .geminiLite: geminiLiteStimme = id
        case .azure: azureStimme = id
        case .qwen: qwenStimme = id
        case .qwenKlon: klonStimme = id
        case .system: break
        }
    }

    /// Stimmen eines Anbieters, Favoriten oben.
    func stimmenListe(_ a: VorleseAnbieter) -> [VorleseStimme] {
        let alle = a == .qwenKlon
            ? klonStimmen.map { VorleseStimme(id: $0.id, name: $0.name, geschlecht: .maennlich) }
            : VorleseKatalog.stimmen(a)
        return alle.filter { favoriten.contains($0.id) } + alle.filter { !favoriten.contains($0.id) }
    }

    func favoritUmschalten(_ id: String) {
        if favoriten.contains(id) { favoriten.remove(id) } else { favoriten.insert(id) }
    }

    // MARK: Tempo je Anbieter (Gemini startet bei 1,0 wie am Handy)

    func tempo(_ a: VorleseAnbieter) -> Double {
        let vorgabe = a.istGemini ? 1.0 : Einstellungen.shared.tempo
        return min(max(tempoJe[a.rawValue] ?? vorgabe, Self.tempoMin), Self.tempoMax)
    }

    func setzeTempo(_ wert: Double, fuer a: VorleseAnbieter) {
        tempoJe[a.rawValue] = min(max(wert, Self.tempoMin), Self.tempoMax)
    }

    // MARK: Rückfallkette

    /// Kann eine Stufe überhaupt laufen (Schlüssel bzw. geklonte Stimme hinterlegt)?
    func bereit(_ s: RueckfallStufe) -> Bool {
        switch s.anbieter {
        case .geminiFlash, .geminiLite:
            return !(s.schluessel == RueckfallStufe.frei ? zugang.geminiFrei : zugang.geminiBezahlt).isEmpty
        case .google: return !zugang.google.isEmpty
        case .azure: return !zugang.azure.isEmpty
        case .qwen: return !zugang.qwen.isEmpty
        case .qwenKlon: return !zugang.qwen.isEmpty && !klonStimme.isEmpty
        case .edge, .system: return true
        }
    }

    func stufeUmschalten(_ id: String) {
        if kette.contains(id) { kette.removeAll { $0 == id } } else { kette.append(id) }
    }

    func stufeVerschieben(_ id: String, um schritt: Int) {
        guard let i = kette.firstIndex(of: id) else { return }
        let j = min(max(i + schritt, 0), kette.count - 1)
        guard i != j else { return }
        kette.swapAt(i, j)
    }

    /// Abzug für einen Vorleseauftrag; `sonderStimme` (z. B. Hörprobe) bestimmt dann auch den Anbieter.
    func stand(sonderStimme: String? = nil) -> VorleseStand {
        var haupt = anbieter
        var stimmen: [VorleseAnbieter: String] = [:]
        var tempi: [VorleseAnbieter: Double] = [:]
        for a in VorleseAnbieter.allCases { stimmen[a] = stimme(a); tempi[a] = tempo(a) }
        if let s = sonderStimme, let a = VorleseKatalog.anbieterZu(s) { haupt = a; stimmen[a] = s }
        let stufen = kette.compactMap(VorleseKatalog.rueckfallStufe)
            .filter { !($0.anbieter == haupt && $0.schluessel == nil) }
            .filter(bereit)
        return VorleseStand(anbieter: haupt, stimmen: stimmen, tempo: tempi, zugang: zugang, geminiWahl: geminiWahl,
                            kette: stufen, immerDeutsch: immerDeutsch)
    }

    // MARK: Geklonte Stimmen aus dem Alibaba-Konto

    func klonStimmenLaden() async {
        klonFehler = nil
        let schluessel = zugang.qwen.filter { !$0.isWhitespace }
        guard !schluessel.isEmpty else { klonStimmen = []; return }
        var req = URLRequest(url: URL(string: "https://dashscope-intl.aliyuncs.com/api/v1/services/audio/tts/customization")!, timeoutInterval: 30)
        req.httpMethod = "POST"
        req.setValue("Bearer \(schluessel)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        req.httpBody = JSON.daten(["model": "qwen-voice-enrollment", "input": ["action": "list"]] as JSONObjekt)
        do {
            let (daten, antwort) = try await URLSession.shared.data(for: req)
            let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
            guard code == 200 else {
                klonFehler = code == 401 || code == 403 ? "Der DashScope-Schlüssel wurde abgelehnt (Fehler \(code))."
                    : (code == 429 ? "Das Kontingent bei Alibaba ist gerade erschöpft (Fehler 429)." : "Alibaba antwortete mit Fehler \(code).")
                return
            }
            let liste = ((JSON.objekt(daten)?["output"] as? JSONObjekt)?["voice_list"] as? [JSONObjekt]) ?? []
            klonStimmen = liste.compactMap { e in
                guard let id = e["voice"] as? String, !id.isEmpty else { return nil }
                return KlonStimme(id: id, name: VorleseKatalog.klonName(id), erstellt: e["gmt_create"] as? String ?? "")
            }
            if klonStimme.isEmpty, let erste = klonStimmen.first { klonStimme = erste.id }
        } catch {
            klonFehler = "Alibaba nicht erreichbar: \(error.localizedDescription)"
        }
    }
}
