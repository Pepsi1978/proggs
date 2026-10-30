import AppKit
import SwiftUI

/// Hell, Automatik oder Dunkel. Die Automatik wechselt bei Sonnenaufgang und Sonnenuntergang in Berlin, gerechnet auf dem Mac, ohne Netz.
/// Bewusst ohne Actor-Bindung: Die Farben lesen `dunkel` auch in Vorgabewerten von Ansichten.
@Observable
final class DesignStand {
    static let shared = DesignStand()

    enum Modus: String, CaseIterable {
        case hell, automatik, dunkel
        var titel: String {
            switch self {
            case .hell: return "Hellmodus"
            case .automatik: return "Automatik"
            case .dunkel: return "Dunkelmodus"
            }
        }
        var symbol: String {
            switch self {
            case .hell: return "sun.max.fill"
            case .automatik: return "circle.lefthalf.filled"
            case .dunkel: return "moon.fill"
            }
        }
        /// Hell → Automatik → Dunkel → Hell.
        var naechster: Modus {
            switch self {
            case .hell: return .automatik
            case .automatik: return .dunkel
            case .dunkel: return .hell
            }
        }
    }

    var modus: Modus {
        didSet {
            UserDefaults.standard.set(modus.rawValue, forKey: "darstellung")
            log("einstellung", "Darstellung = \(modus.rawValue)", wo: "Seitenleiste")
            aktualisieren()
        }
    }
    private(set) var dunkel: Bool

    init() {
        let m = Modus(rawValue: UserDefaults.standard.string(forKey: "darstellung") ?? "") ?? .automatik
        modus = m
        dunkel = Self.istDunkel(m, jetzt: Date())
    }

    func weiter() { modus = modus.naechster }

    /// Nur für den Tooltip, aber die Seitenleiste rechnet oft neu: Die Zeiten werden einmal am Tag bestimmt.
    @ObservationIgnored private var zeitenText = ""
    @ObservationIgnored private var zeitenTag = -1
    private static let uhrzeit: DateFormatter = { let f = DateFormatter(); f.timeZone = berlin; f.dateFormat = "HH:mm"; return f }()

    var hilfe: String {
        var kalender = Calendar(identifier: .gregorian)
        kalender.timeZone = Self.berlin
        let tag = kalender.ordinality(of: .day, in: .year, for: Date()) ?? 0
        if tag != zeitenTag {
            let s = Self.sonne(an: Date())
            zeitenText = "Berlin heute: Sonnenaufgang \(Self.uhrzeit.string(from: s.auf)) Uhr, Sonnenuntergang \(Self.uhrzeit.string(from: s.unter)) Uhr."
            zeitenTag = tag
        }
        return "\(modus.titel). Antippen wechselt zu \(modus.naechster.titel). \(zeitenText)"
    }

    /// Einmal beim Start: stellt die App ein und prüft in der Automatik jede halbe Minute, ob die Sonne auf- oder untergegangen ist.
    @MainActor func starten() {
        NSApp?.appearance = NSAppearance(named: dunkel ? .darkAqua : .aqua)
        aktualisieren()
        Task { @MainActor [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(30))
                self?.aktualisieren()
            }
        }
    }

    private func aktualisieren() {
        let neu = Self.istDunkel(modus, jetzt: Date())
        guard neu != dunkel else { return }
        dunkel = neu
        Task { @MainActor in NSApp?.appearance = NSAppearance(named: neu ? .darkAqua : .aqua) }
    }

    private static func istDunkel(_ m: Modus, jetzt: Date) -> Bool {
        switch m {
        case .hell: return false
        case .dunkel: return true
        case .automatik:
            let s = sonne(an: jetzt)
            return jetzt < s.auf || jetzt >= s.unter
        }
    }

    static let berlin = TimeZone(identifier: "Europe/Berlin") ?? .current

    /// Sonnenaufgang und -untergang in Berlin nach den NOAA-Näherungsformeln (Sonnenrand mit Lichtbrechung: 90,833°).
    static func sonne(an datum: Date) -> (auf: Date, unter: Date) {
        var kalender = Calendar(identifier: .gregorian)
        kalender.timeZone = berlin
        let tag = kalender.ordinality(of: .day, in: .year, for: datum) ?? 1
        let k = kalender.dateComponents([.year, .month, .day], from: datum)
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC") ?? .gmt
        let mitternacht = utc.date(from: k) ?? datum

        let g = 2 * Double.pi / 365 * Double(tag - 1)
        let gleichung = 229.18 * (0.000075 + 0.001868 * cos(g) - 0.032077 * sin(g) - 0.014615 * cos(2 * g) - 0.040849 * sin(2 * g))
        let dekl = 0.006918 - 0.399912 * cos(g) + 0.070257 * sin(g) - 0.006758 * cos(2 * g) + 0.000907 * sin(2 * g) - 0.002697 * cos(3 * g) + 0.00148 * sin(3 * g)
        let breite = 52.52 * Double.pi / 180, laenge = 13.405
        let cosH = cos(90.833 * Double.pi / 180) / (cos(breite) * cos(dekl)) - tan(breite) * tan(dekl)
        let h = acos(min(1, max(-1, cosH))) * 180 / Double.pi
        let auf = 720 - 4 * (laenge + h) - gleichung
        let unter = 720 - 4 * (laenge - h) - gleichung
        return (mitternacht.addingTimeInterval(auf * 60), mitternacht.addingTimeInterval(unter * 60))
    }
}

/// Grüner Punkt, solange das Handy-Jarvis erreichbar ist.
struct LivePunkt: View {
    var groesse: CGFloat = 8
    @State private var steuerung = AppSteuerung.shared
    var body: some View {
        Circle().fill(steuerung.handyVerbunden ? Farbe.gruen : Farbe.warnung).frame(width: groesse, height: groesse)
            .shadow(color: steuerung.handyVerbunden ? Farbe.gruen : Farbe.warnung, radius: 4)
            .help(steuerung.handyVerbunden ? "Mit dem Handy verbunden" : "Handy nicht erreichbar")
    }
}
