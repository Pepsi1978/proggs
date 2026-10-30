import SwiftUI

/// Guthaben von Anthropic und Google, gelesen über die Werkzeuge des Handys (dort liegt die Anmeldung), und was die letzte Aufgabe gekostet hat.
@MainActor @Observable
final class Guthaben {
    static let shared = Guthaben()

    var anthropicText: String?
    var anthropicRest: Double?
    var anthropicGewaehrt: Double?
    var kurs: Double?
    var googleText: String?
    var googleRest: Double?
    var googleGewaehrt: Double?
    var googleWaehrung = "€"
    var laedt = false
    /// Was die letzte Aufgabe mit einem Claude-Modell gekostet hat, in Dollar (Guthaben vorher minus nachher, wie am Handy).
    var kosten: Double?

    private var geholt: Date?
    private var laufende = 0
    private var messung: Task<Double?, Never>?

    func laden(frisch: Bool = false) async {
        guard !laedt else { return }
        laedt = true; defer { laedt = false }
        async let a: Void = anthropicLaden(frisch: frisch)
        async let g: Void = googleLaden(frisch: frisch)
        _ = await (a, g)
    }

    private func anthropicLaden(frisch: Bool) async {
        let r = await McpKlient.shared.rufe("anthropic_guthaben", frisch ? ["frisch": true] : [:], sekunden: 60)
        let t = Self.ohneHinweis(r.text)
        anthropicText = t
        guard !r.fehler, let rest = Self.zahl(t, #"Übrig: ([0-9.,]+) \$"#) else { return }
        anthropicRest = rest
        anthropicGewaehrt = Self.zahl(t, #"Übrig: [^\n]*? von ([0-9.,]+) \$"#)
        kurs = Self.zahl(t, #"Kurs 1 \$ = ([0-9.,]+) €"#)
        geholt = Date()
    }

    private func googleLaden(frisch: Bool) async {
        let r = await McpKlient.shared.rufe("google_guthaben", frisch ? ["frisch": true] : [:], sekunden: 60)
        let t = Self.ohneHinweis(r.text)
        googleText = t
        guard !r.fehler, let rest = Self.zahl(t, #"übrig ([0-9.,]+) "#) else { return }
        googleRest = rest
        googleGewaehrt = Self.zahl(t, #"übrig [0-9.,]+ \S+ von ([0-9.,]+) "#)
        if let m = t.range(of: #"übrig [0-9.,]+ (\S+) von"#, options: .regularExpression) {
            googleWaehrung = String(t[m].split(separator: " ")[2])
        }
    }

    // MARK: Kosten je Aufgabe

    func kostenStart() {
        laufende += 1
        guard laufende == 1, !LiveGespraech.shared.aktiv else { return }
        kosten = nil
        let alt = anthropicRest, frischGenug = geholt.map { Date().timeIntervalSince($0) < 120 } ?? false
        messung = Task { @MainActor in
            if frischGenug, let alt { return alt }
            await self.anthropicLaden(frisch: true)
            return self.anthropicRest
        }
    }

    /// Die Console bucht verzögert: sofort und nach einer Minute noch einmal frisch holen.
    func kostenEnde() {
        laufende = max(0, laufende - 1)
        guard laufende == 0, let m = messung else { return }
        messung = nil
        Task { @MainActor in
            guard let start = await m.value else { return }
            for warten in [0.0, 60.0] {
                if warten > 0 { try? await Task.sleep(for: .seconds(warten)) }
                await self.anthropicLaden(frisch: true)
                if let rest = self.anthropicRest, start - rest > 0 { self.kosten = start - rest }
            }
        }
    }

    // MARK: Text

    static func betrag(_ x: Double) -> String { x.formatted(.number.precision(.fractionLength(2)).locale(Locale(identifier: "de_DE"))) }

    private static func zahl(_ text: String, _ muster: String) -> Double? {
        guard let re = try? NSRegularExpression(pattern: muster),
              let m = re.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)), m.numberOfRanges > 1,
              let r = Range(m.range(at: 1), in: text) else { return nil }
        return Double(text[r].replacingOccurrences(of: ".", with: "").replacingOccurrences(of: ",", with: "."))
    }

    private static func ohneHinweis(_ t: String) -> String {
        t.replacingOccurrences(of: "\n(Falls in diesem Gespräch noch nicht geschehen: zuerst jarvis_kontext aufrufen.)", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// Der Satz des Handys als einzelne Punkte; die große Zahl steht schon darüber.
    static func punkte(_ text: String) -> [String] {
        text.replacingOccurrences(of: " · ", with: ". ").components(separatedBy: ". ")
            .map { $0.trimmingCharacters(in: CharacterSet(charactersIn: ". ")) }
            .filter { !$0.isEmpty }
    }
}

/// Modellwahl mit Guthaben und Kosten, in jeder Eingabeblase gleich: „12,34 $ −0,08 $ · Opus 5.5“.
struct ModellKapsel: View {
    @State private var einst = Einstellungen.shared
    @State private var guthaben = Guthaben.shared

    private var istClaude: Bool { einst.modellWahl.anbieter == .anthropic }
    /// Unter 5 € rot, sonst grün; ohne Kurs bleibt es grün (wie am Handy).
    private var knapp: Bool {
        guard let r = guthaben.anthropicRest, let k = guthaben.kurs else { return false }
        return r * k < 5
    }

    var body: some View {
        Menu {
            ForEach(ModellWahl.alle) { m in
                Button { einst.modell = m.id } label: { Text(m.name + (m.id == einst.modell ? "  ✓" : "")) }
            }
            Divider()
            ForEach(["low", "medium", "high", "xhigh", "max"], id: \.self) { e in
                Button { einst.effort = e } label: { Text("Denkstufe \(e)" + (e == einst.effort ? "  ✓" : "")) }
            }
            Divider()
            if let r = guthaben.anthropicRest { Text("Anthropic: \(Guthaben.betrag(r)) $ übrig") }
            if let k = guthaben.kosten, k > 0 { Text("Letzte Aufgabe: \(Guthaben.betrag(k)) $") }
            if let g = guthaben.googleRest { Text("Google: \(Guthaben.betrag(g)) \(guthaben.googleWaehrung) übrig") }
            Button("Guthaben ansehen …") { EinstellungenAnsicht.startReiter = "guthaben"; AppSteuerung.shared.bereich = .einstellungen }
        } label: {
            HStack(spacing: 5) {
                if istClaude, let r = guthaben.anthropicRest {
                    Text("\(Guthaben.betrag(r)) $").font(.jarvis(11, .bold)).foregroundStyle(knapp ? Farbe.rot : Farbe.gruen)
                    if let k = guthaben.kosten, k > 0 {
                        Text("−\(Guthaben.betrag(k)) $").font(.jarvis(11, .bold)).foregroundStyle(Farbe.rot)
                    }
                    Text("·").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                }
                Text(einst.modellWahl.name.replacingOccurrences(of: "Claude ", with: "")).font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text2)
            }
            .lineLimit(1)
            .padding(.horizontal, 10).padding(.vertical, 8)
            .background(Capsule().fill(Farbe.glas(0.06)))
        }
        .menuStyle(.button).buttonStyle(.plain).menuIndicator(.hidden).fixedSize()
        .help("Modell, Denkstufe und Guthaben")
    }
}

/// Einstellungen → Guthaben: je Anbieter eine Karte wie am Handy.
struct GuthabenAnsicht: View {
    @State private var g = Guthaben.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Geschenktes Guthaben wird immer vor deinem eigenen Geld verbraucht. Hier siehst du, wie viel noch übrig ist und wann es verfällt.")
                .font(.jarvis(13)).foregroundStyle(Farbe.text2)
            karte(titel: "Claude-Modelle (Anthropic)", symbol: "brain", erklaerung: "Bezahlt Jarvis' Kopf, wenn du ein Claude-Modell gewählt hast.",
                  rest: g.anthropicRest, gewaehrt: g.anthropicGewaehrt, einheit: "$", umrechnung: g.kurs, text: g.anthropicText)
            karte(titel: "Gemini-Stimmen (Google AI Studio)", symbol: "waveform", erklaerung: "Bezahlt das Vorlesen mit Gemini-TTS und das Live-Gespräch mit Gemini auf dem bezahlten Schlüssel.",
                  rest: g.googleRest, gewaehrt: g.googleGewaehrt, einheit: g.googleWaehrung, umrechnung: nil, text: g.googleText)
            HStack(spacing: 10) {
                KapselKnopf(symbol: "arrow.clockwise", titel: "Jetzt neu abfragen", hervorgehoben: true) { Task { await g.laden(frisch: true) } }
                if g.laedt { ProgressView().controlSize(.small) }
            }
            if let k = g.kosten, k > 0 {
                Text("Die letzte Aufgabe mit einem Claude-Modell hat \(Guthaben.betrag(k)) $ gekostet.").font(.jarvis(13)).foregroundStyle(Farbe.text2)
            }
            Text("Die Anmeldung bei der Claude Console und bei Google liegt auf dem Handy: Einstellungen → Guthaben.").font(.jarvis(12)).foregroundStyle(Farbe.text3)
        }
        .task { await g.laden() }
    }

    private func karte(titel: String, symbol: String, erklaerung: String, rest: Double?, gewaehrt: Double?, einheit: String, umrechnung: Double?, text: String?) -> some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                KartenTitel(symbol: symbol, titel: titel)
                Text(erklaerung).font(.jarvis(12)).foregroundStyle(Farbe.text2)
                if let rest {
                    HStack(alignment: .firstTextBaseline, spacing: 10) {
                        Text("\(Guthaben.betrag(rest)) \(einheit)").font(.jarvis(30, .bold)).foregroundStyle(Farbe.haupt)
                        if let umrechnung { Text("≈ \(Guthaben.betrag(rest * umrechnung)) €").font(.jarvis(14)).foregroundStyle(Farbe.text2) }
                    }
                    if let gewaehrt, gewaehrt > 0 { balken(min(1, max(0, (gewaehrt - rest) / gewaehrt))) }
                    if let text {
                        ForEach(Guthaben.punkte(text), id: \.self) { p in
                            Text("• " + p).font(.jarvis(13)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                        }
                    }
                } else {
                    Text(text ?? "Lade …").font(.jarvis(13)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }

    private func balken(_ anteil: Double) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Farbe.glas(0.08))
                Capsule().fill(Farbe.akzent).frame(width: geo.size.width * anteil)
            }
        }
        .frame(height: 8)
    }
}
