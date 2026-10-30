import AppKit
import Foundation
import Observation
import SwiftUI

/// Transkription wie im Handy-Jarvis (Modul M1.4): Groq Whisper Large V3 Turbo oder Large V3 mit Wörterbuch und
/// vier Filtern gegen erfundene Texte, Parakeet primeline offline – dazu am Mac Apples Erkennung auf dem Gerät.
/// Auswahl in `Einstellungen.transkription`: "parakeet" | "groq" | "apple". Nie ein stiller Wechsel auf ein anderes
/// Verfahren; nur bei Groq nimmt Jarvis wie am Handy Turbo, solange Large V3 für die Organisation gesperrt ist.
@MainActor @Observable
final class TranskriptionsEinstellungen {
    static let shared = TranskriptionsEinstellungen()
    private let d = UserDefaults.standard

    nonisolated static let modellTurbo = "whisper-large-v3-turbo"
    nonisolated static let modellLargeV3 = "whisper-large-v3"
    nonisolated static let standardWoerterbuch = "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic, bug almanach, Anthrophic"

    var whisperModell: String { didSet { d.set(whisperModell, forKey: "tk.whisperModell") } }
    var filterStilleVorab: Bool { didSet { d.set(filterStilleVorab, forKey: "tk.filterStille") } }
    var filterSegmente: Bool { didSet { d.set(filterSegmente, forKey: "tk.filterSegmente") } }
    var filterZeitstempel: Bool { didSet { d.set(filterZeitstempel, forKey: "tk.filterZeitstempel") } }
    var filterFloskeln: Bool { didSet { d.set(filterFloskeln, forKey: "tk.filterFloskeln") } }
    var woerterbuchAn: Bool { didSet { d.set(woerterbuchAn, forKey: "tk.woerterbuchAn") } }
    var woerterbuchBegriffe: String { didSet { d.set(woerterbuchBegriffe, forKey: "tk.woerterbuch") } }

    /// Fortschritt des Parakeet-Downloads (0…1), nil = kein Download.
    var download: Double?
    var downloadFehler: String?

    init() {
        let d = UserDefaults.standard
        func b(_ k: String) -> Bool { d.object(forKey: k) as? Bool ?? true }
        whisperModell = d.string(forKey: "tk.whisperModell") ?? Self.modellTurbo
        filterStilleVorab = b("tk.filterStille")
        filterSegmente = b("tk.filterSegmente")
        filterZeitstempel = b("tk.filterZeitstempel")
        filterFloskeln = b("tk.filterFloskeln")
        woerterbuchAn = b("tk.woerterbuchAn")
        woerterbuchBegriffe = d.string(forKey: "tk.woerterbuch") ?? Self.standardWoerterbuch
        // Vorgabe wie gewünscht: Parakeet, wenn das geteilte Modell (von TVO/CVO) schon auf dem Mac liegt.
        if d.string(forKey: "transkription") == nil && ParakeetLocalClient.isInstalled {
            Einstellungen.shared.transkription = "parakeet"
        }
    }

    /// Wörterbuch als Whisper-Prompt („Begriffe: A, B.“), höchstens 350 Zeichen und 100 Begriffe – wie am Handy.
    var prompt: String? {
        guard woerterbuchAn else { return nil }
        var gesehen = Set<String>()
        let begriffe = woerterbuchBegriffe.components(separatedBy: CharacterSet(charactersIn: "\n\r,;"))
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty && !$0.hasPrefix("#") && gesehen.insert($0.lowercased()).inserted }
            .prefix(100)
        guard !begriffe.isEmpty else { return nil }
        var s = "Begriffe: "
        var n = 0
        for w in begriffe where s.count + w.count + 2 <= 350 {
            if n > 0 { s += ", " }
            s += w; n += 1
        }
        return n == 0 ? nil : s + "."
    }

    var anzeigeName: String {
        switch Einstellungen.shared.transkription {
        case "parakeet": return "Parakeet primeline (offline)"
        case "groq": return whisperModell == Self.modellLargeV3 ? "Groq Whisper Large V3" : "Groq Whisper Large V3 Turbo"
        default: return "Apple (offline)"
        }
    }

    func parakeetLaden() {
        guard download == nil else { return }
        download = 0; downloadFehler = nil
        ParakeetLocalClient.download(progress: { [weak self] n, gesamt in
            MainActor.assumeIsolated { self?.download = Double(n) / Double(max(gesamt, 1)) }
        }, completion: { [weak self] fehler in
            MainActor.assumeIsolated {
                self?.download = nil
                self?.downloadFehler = fehler?.localizedDescription
                if fehler == nil { ParakeetLocalClient.shared.preload() }
            }
        })
    }
}

/// Einheitlicher Einstieg fürs Diktat: 16-kHz-Mono-Samples → Text mit dem gewählten Verfahren.
enum Transkription {
    struct Fehler: LocalizedError {
        let text: String
        var errorDescription: String? { text }
    }

    @MainActor static func erkenne(_ samples: [Float]) async throws -> String {
        let e = TranskriptionsEinstellungen.shared
        let analyse = SprachAnalyse(samples)
        switch Einstellungen.shared.transkription {
        case "parakeet":
            guard ParakeetLocalClient.isInstalled else {
                throw Fehler(text: "Offline-Modell Parakeet fehlt – Einstellungen → Sprache → Transkription → „Modell laden“. Kein Rückfall auf Groq.")
            }
            // „Stille vorab“ gilt wie am Handy auch offline.
            if e.filterStilleVorab && analyse.stimmMs < SprachAnalyse.minSprachMs { return "" }
            let zuschnitt = e.filterStilleVorab ? analyse.zugeschnitten(samples) : samples
            return try await ParakeetLocalClient.shared.transkribiere(zuschnitt)
        default:
            return try await GroqWhisper.transkribiere(samples, analyse: analyse, schluessel: Einstellungen.shared.zugang.groq,
                                                      modell: e.whisperModell, prompt: e.prompt,
                                                      filter: .init(stille: e.filterStilleVorab, segmente: e.filterSegmente,
                                                                    zeitstempel: e.filterZeitstempel, floskeln: e.filterFloskeln))
        }
    }
}

/// Stimm-Zeitachse einer Aufnahme (20-ms-Rahmen) – Grundlage für „Stille vorab“, Zuschnitt und Zeitstempel-Filter.
struct SprachAnalyse: Sendable {
    static let rahmenMs = 20
    // Unter der Diktat-Schwelle (mindestens 0,012), sonst verwirft „Stille vorab“ leises Sprechen, das das Diktat als begonnen zählt.
    static let rmsSchwelle: Float = 0.009
    static let minSprachMs = 150
    let stimmhaft: [Bool]
    var stimmMs: Int { stimmhaft.filter { $0 }.count * Self.rahmenMs }

    init(_ samples: [Float], rate: Int = 16000) {
        let n = rate * Self.rahmenMs / 1000
        var r: [Bool] = []
        r.reserveCapacity(samples.count / n + 1)
        var i = 0
        while i + n <= samples.count {
            var s: Float = 0
            for j in i..<(i + n) { s += samples[j] * samples[j] }
            r.append((s / Float(n)).squareRoot() > Self.rmsSchwelle)
            i += n
        }
        stimmhaft = r
    }

    private init(stimmhaft: [Bool]) { self.stimmhaft = stimmhaft }

    /// Stille vorn und hinten abschneiden (mit 300 ms Rand), damit nichts Leeres hochgeht.
    func zugeschnitten(_ samples: [Float], rate: Int = 16000) -> [Float] {
        guard let a = stimmhaft.firstIndex(of: true), let b = stimmhaft.lastIndex(of: true) else { return samples }
        let n = rate * Self.rahmenMs / 1000
        let rand = 15
        let von = max(0, (a - rand) * n), bis = min(samples.count, (b + 1 + rand) * n)
        return von < bis ? Array(samples[von..<bis]) : samples
    }

    /// Zeitachse passend zur zugeschnittenen Aufnahme.
    func nachZuschnitt() -> SprachAnalyse {
        guard let a = stimmhaft.firstIndex(of: true), let b = stimmhaft.lastIndex(of: true) else { return self }
        let von = max(0, a - 15), bis = min(stimmhaft.count, b + 1 + 15)
        return SprachAnalyse(stimmhaft: Array(stimmhaft[von..<bis]))
    }

    func segmentHatSprache(_ start: Double, _ ende: Double, minAnteil: Double = 0.10) -> Bool {
        guard !stimmhaft.isEmpty, ende > start else { return true }
        let erst = min(max(Int(start * 1000 / Double(Self.rahmenMs)), 0), stimmhaft.count - 1)
        let letzt = min(max(Int(ende * 1000 / Double(Self.rahmenMs)), erst), stimmhaft.count - 1)
        let n = stimmhaft[erst...letzt].filter { $0 }.count
        return Double(n) / Double(letzt - erst + 1) >= minAnteil
    }
}

// MARK: - Einstellungsansicht

struct TranskriptionsEinstellungenAnsicht: View {
    @State private var einst = Einstellungen.shared
    @State private var tk = TranskriptionsEinstellungen.shared
    @State private var begriffe = TranskriptionsEinstellungen.shared.woerterbuchBegriffe
    @State private var auffrischen = 0

    private var parakeetDa: Bool { _ = auffrischen; return ParakeetLocalClient.isInstalled }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            KartenTitel(symbol: "text.bubble.fill", titel: "Transkription (Spracheingabe)", farbe: Farbe.gruen)
            Text("Gilt für Diktat, Sprachgespräch und „Hey Jarvis“. Jarvis wechselt nie still auf ein anderes Verfahren; fehlt etwas, sagt er es.")
                .font(.jarvis(12)).foregroundStyle(Farbe.text2)

            VStack(spacing: 8) {
                wahl("parakeet", "Parakeet primeline – offline", "Deutscher Fine-Tune von Parakeet TDT 0.6B v3, int8 über sherpa-onnx, nur CPU (4 Threads). Kein Internet, kein Schlüssel.")
                wahl("groq", "Groq Whisper – online", "Whisper Large V3 Turbo oder Large V3 bei Groq, mit Wörterbuch und Filtern. Braucht Netz und Groq-Schlüssel.")
                wahl("apple", "Apple – offline", "Spracherkennung von macOS direkt auf dem Mac, mit Live-Vorschau beim Sprechen.")
            }

            if einst.transkription == "parakeet" { parakeet }
            if einst.transkription == "groq" { groq }
            if einst.transkription != "apple" {
                Text("Filter gegen erfundene Texte bei Stille").font(.jarvis(13, .semibold)).padding(.top, 4)
                HStack(spacing: 8) {
                    chip("Stille vorab", $tk.filterStilleVorab)
                    if einst.transkription == "groq" {
                        chip("Segment-Werte", $tk.filterSegmente)
                        chip("Zeitstempel", $tk.filterZeitstempel)
                        chip("Floskeln", $tk.filterFloskeln)
                    }
                }
                if einst.transkription == "parakeet" {
                    Text("Wörterbuch und Whisper-Filter gelten nur für Groq; „Stille vorab“ wirkt auch hier.")
                        .font(.jarvis(11)).foregroundStyle(Farbe.text3)
                }
            }
        }
    }

    private func wahl(_ id: String, _ titel: String, _ text: String) -> some View {
        let aktiv = einst.transkription == id
        return Button { einst.transkription = id } label: {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: aktiv ? "largecircle.fill.circle" : "circle")
                    .font(.system(size: 16)).foregroundStyle(aktiv ? Farbe.gruen : Farbe.text3)
                VStack(alignment: .leading, spacing: 3) {
                    Text(titel).font(.jarvis(14, .semibold)).foregroundStyle(Farbe.text)
                    Text(text).font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
            .padding(12)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Farbe.glas(aktiv ? 0.08 : 0.03)))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(aktiv ? Farbe.gruen.opacity(0.5) : Farbe.glas(0.06)))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func chip(_ titel: String, _ wert: Binding<Bool>) -> some View {
        Button { wert.wrappedValue.toggle() } label: {
            HStack(spacing: 6) {
                Image(systemName: wert.wrappedValue ? "checkmark" : "xmark").font(.system(size: 10, weight: .bold))
                Text(titel).font(.jarvis(12, .semibold))
            }
            .foregroundStyle(wert.wrappedValue ? Color.black : Farbe.text2)
            .padding(.horizontal, 12).padding(.vertical, 7)
            .background(Capsule().fill(wert.wrappedValue ? AnyShapeStyle(Farbe.gruen) : AnyShapeStyle(Farbe.glas(0.07))))
        }
        .buttonStyle(.plain)
    }

    private var parakeet: some View {
        VStack(alignment: .leading, spacing: 8) {
            let mb = { (b: Int64) in "\(b / 1_048_576) MB" }
            HStack {
                Text("Modell").font(.jarvis(13, .semibold))
                Spacer()
                if let p = tk.download {
                    Text("Lädt … \(Int(p * 100)) %").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                } else if parakeetDa {
                    Text("Bereit, \(mb(ParakeetLocalClient.belegteBytes)) belegt").font(.jarvis(12, .medium)).foregroundStyle(Farbe.gruen)
                } else {
                    Text("Nicht geladen (\(mb(ParakeetLocalClient.totalBytes)))").font(.jarvis(12)).foregroundStyle(Farbe.text3)
                }
            }
            if let p = tk.download { ProgressView(value: p).tint(Farbe.gruen) }
            if let m = ParakeetLocalClient.shared.messung {
                Text("Gemessen auf diesem Mac: Laden und Aufwärmen \(String(format: "%.1f", Double(m.ladeMs) / 1000)) s, Arbeitsspeicher etwa \(m.ramMb) MB"
                     + (m.audioMs > 0 ? "; letzte Mitschrift \(String(format: "%.1f", Double(m.audioMs) / 1000)) s Aufnahme in \(String(format: "%.2f", Double(m.rechenMs) / 1000)) s gerechnet." : "."))
                    .font(.jarvis(11)).foregroundStyle(Farbe.text2)
            }
            Text("Das Modell liegt geteilt unter ~/Library/Application Support/VoiceOverlays/ – dasselbe wie in TerminalVoiceOverlay und ClaudeVoiceOverlay.")
                .font(.jarvis(11)).foregroundStyle(Farbe.text3)
            if tk.download == nil && !parakeetDa {
                KapselKnopf(symbol: "arrow.down.circle.fill", titel: "Modell laden (\(mb(ParakeetLocalClient.totalBytes)), einmalig)", hervorgehoben: true) { tk.parakeetLaden() }
            } else if parakeetDa {
                KapselKnopf(symbol: "bolt.fill", titel: "Vorladen und messen") {
                    ParakeetLocalClient.shared.preload()
                    Task { try? await Task.sleep(for: .seconds(8)); auffrischen += 1 }
                }
            }
            if let f = tk.downloadFehler { Text(f).font(.jarvis(12)).foregroundStyle(Farbe.rot) }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 14).fill(Farbe.tiefe(0.2)))
        .onChange(of: tk.download) { _, _ in auffrischen += 1 }
    }

    private var groq: some View {
        VStack(alignment: .leading, spacing: 10) {
            if einst.zugang.groq.isEmpty {
                Text("Kein Groq-Schlüssel eingetragen (Einstellungen → Konten).").font(.jarvis(12, .medium)).foregroundStyle(Farbe.warnung)
            }
            Text("Whisper-Modell").font(.jarvis(13, .semibold))
            HStack(spacing: 8) {
                chipWahl("Large V3 Turbo (schnell)", tk.whisperModell != TranskriptionsEinstellungen.modellLargeV3) {
                    tk.whisperModell = TranskriptionsEinstellungen.modellTurbo; GroqWhisper.sperreAufheben()
                }
                chipWahl("Large V3 (genauer)", tk.whisperModell == TranskriptionsEinstellungen.modellLargeV3) {
                    tk.whisperModell = TranskriptionsEinstellungen.modellLargeV3; GroqWhisper.sperreAufheben()
                }
            }
            Text("Large V3 muss in der Groq-Konsole für deine Organisation freigeschaltet sein; sonst nimmt Jarvis wie am Handy Turbo"
                 + (GroqWhisper.largeV3Gesperrt ? " (gerade gesperrt, derzeit Turbo)." : "."))
                .font(.jarvis(11)).foregroundStyle(Farbe.text3)
            Text("Persönliches Wörterbuch").font(.jarvis(13, .semibold)).padding(.top, 4)
            chip("Wörterbuch mitsenden", $tk.woerterbuchAn)
            if tk.woerterbuchAn {
                Text("Begriffe (durch Komma oder Zeilenumbruch getrennt)").font(.jarvis(11)).foregroundStyle(Farbe.text2)
                TextEditor(text: $begriffe).font(.jarvis(12)).frame(minHeight: 70).scrollContentBackground(.hidden)
                    .padding(6).background(RoundedRectangle(cornerRadius: 10).fill(Farbe.tiefe(0.25)))
                if begriffe != tk.woerterbuchBegriffe {
                    KapselKnopf(symbol: "checkmark", titel: "Begriffe speichern", hervorgehoben: true) { tk.woerterbuchBegriffe = begriffe }
                }
            }
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 14).fill(Farbe.tiefe(0.2)))
    }

    private func chipWahl(_ titel: String, _ aktiv: Bool, _ aktion: @escaping () -> Void) -> some View {
        Button(action: aktion) {
            Text(titel).font(.jarvis(12, .semibold))
                .foregroundStyle(aktiv ? Color.black : Farbe.text2)
                .padding(.horizontal, 12).padding(.vertical, 7)
                .background(Capsule().fill(aktiv ? AnyShapeStyle(Farbe.haupt) : AnyShapeStyle(Farbe.glas(0.07))))
        }
        .buttonStyle(.plain)
    }
}
