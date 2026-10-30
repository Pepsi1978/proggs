import AppKit
import SwiftUI

/// Guthaben von Anthropic und Google, gelesen über die Werkzeuge des Handys oder die eigene Console-Anmeldung, und was die letzte Aufgabe gekostet hat.
@MainActor @Observable
final class Guthaben {
    static let shared = Guthaben()

    var anthropicText: String?
    /// Warum die letzte Abfrage keine Zahl lieferte. Dann ist anthropicRest, falls vorhanden, ein alter Stand.
    var anthropicHinweis: String?
    var anthropicRest: Double?
    var anthropicGewaehrt: Double?
    var kurs: Double?
    var googleText: String?
    var googleHinweis: String?
    var googleRest: Double?
    var googleGewaehrt: Double?
    var googleWaehrung = "€"
    var laedt = false
    /// Der Mac hat eine eigene Console-Anmeldung und liest das Guthaben dann selbst statt über das Handy.
    var macAngemeldet = ConsoleSitzung.angemeldet
    var anmeldungOffen = false
    /// Was die letzte Aufgabe mit einem Claude-Modell gekostet hat, in Dollar: aus den Tokens der API, sofort und ohne Abfrage.
    var kosten: Double?

    private var geholt: Date?

    func laden(frisch: Bool = false) async {
        // Ein ausdrücklicher Wunsch wartet die laufende Abfrage ab, statt verloren zu gehen.
        if laedt && !frisch { return }
        while laedt { try? await Task.sleep(for: .milliseconds(300)) }
        laedt = true; defer { laedt = false }
        async let a: Void = anthropicLaden(frisch: frisch)
        async let g: Void = googleLaden(frisch: frisch)
        _ = await (a, g)
    }

    private func anthropicLaden(frisch: Bool) async {
        macAngemeldet = ConsoleSitzung.angemeldet
        var macFehler: String?
        if macAngemeldet {
            if !frisch, anthropicRest != nil, anthropicHinweis == nil, let geholt, Date().timeIntervalSince(geholt) < 900 { return }
            // Wer sich während des Abrufs an- oder abmeldet, macht dessen Ergebnis ungültig.
            let nr = ConsoleSitzung.nummer
            do {
                let s = try await ConsoleSitzung.stand()
                guard nr == ConsoleSitzung.nummer else { return }
                anthropicHinweis = nil
                anthropicText = s.satz; anthropicRest = s.rest; anthropicGewaehrt = s.gewaehrt
                if let k = s.kurs { kurs = k }
                geholt = Date()
                return
            } catch ConsoleSitzung.Fehler.abgelaufen {
                guard nr == ConsoleSitzung.nummer else { return }
                log("guthaben", "Console-Anmeldung abgelaufen, Mac abgemeldet", wo: "Guthaben")
                abmelden()   // Fällt danach auf das Handy zurück und bietet wieder den Link an.
            } catch {
                guard nr == ConsoleSitzung.nummer else { return }
                log("fehler", "Anthropic-Guthaben nicht abrufbar", wo: "Guthaben", fehler: error.localizedDescription)
                macFehler = error.localizedDescription   // Das Handy kann vielleicht noch antworten.
            }
        }
        let r = await McpKlient.shared.rufe("anthropic_guthaben", frisch ? ["frisch": true] : [:], sekunden: 60)
        let t = McpKlient.ohneKontextHinweis(r.text)
        guard !r.fehler else {
            // Der Hinweis des Handys verweist auf dessen Einstellungen; am Mac steht der Link direkt darunter. Die Bot-Abwehr-Meldung sagt das Gegenteil und bleibt.
            anthropicHinweis = macFehler ?? (t.contains("anmelden") && !t.contains("hilft") ? "Für das Guthaben einmal bei der Anthropic-Konsole anmelden." : t)
            return
        }
        uebernehmen(t)
    }

    func abmelden() {
        ConsoleSitzung.abmelden()
        macAngemeldet = false
        anthropicRest = nil; anthropicGewaehrt = nil; anthropicText = nil; anthropicHinweis = nil; geholt = nil
    }

    private func uebernehmen(_ t: String) {
        guard let rest = Self.zahl(t, #"Übrig: ([0-9.,]+) \$"#) else { anthropicHinweis = t; return }
        anthropicHinweis = nil
        anthropicText = t
        anthropicRest = rest
        anthropicGewaehrt = Self.zahl(t, #"Übrig: [^\n]*? von ([0-9.,]+) \$"#)
        kurs = Self.zahl(t, #"Kurs 1 \$ = ([0-9.,]+) €"#)
        geholt = Date()
    }

    private func googleLaden(frisch: Bool) async {
        let r = await McpKlient.shared.rufe("google_guthaben", frisch ? ["frisch": true] : [:], sekunden: 60)
        let t = McpKlient.ohneKontextHinweis(r.text)
        guard !r.fehler, let rest = Self.zahl(t, #"übrig ([0-9.,]+) "#) else { googleHinweis = t; return }
        googleHinweis = nil
        googleText = t
        googleRest = rest
        googleGewaehrt = Self.zahl(t, #"übrig [0-9.,]+ \S+ von ([0-9.,]+) "#)
        if let m = t.range(of: #"übrig [0-9.,]+ (\S+) von"#, options: .regularExpression) {
            googleWaehrung = String(t[m].split(separator: " ")[2])
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

    /// Der Satz des Handys als einzelne Punkte; die große Zahl steht schon darüber.
    static func punkte(_ text: String) -> [String] {
        text.replacingOccurrences(of: "z. B.", with: "z.\u{00A0}B.").replacingOccurrences(of: " · ", with: ". ").components(separatedBy: ". ")
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

    /// Kleine Beträge brauchen mehr Stellen: Eine Haiku-Antwort kostet oft weniger als einen Cent.
    static func kostenText(_ k: Double) -> String {
        k >= 0.01 ? Guthaben.betrag(k) : k.formatted(.number.precision(.fractionLength(4)).locale(Locale(identifier: "de_DE")))
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
            if let k = guthaben.kosten, k > 0 { Text("Letzte Aufgabe: \(Self.kostenText(k)) $ (Listenpreis)") }
            if let g = guthaben.googleRest { Text("Google: \(Guthaben.betrag(g)) \(guthaben.googleWaehrung) übrig") }
            if guthaben.anthropicRest == nil, !guthaben.macAngemeldet { Button("Bei der Anthropic-Konsole anmelden …") { guthaben.anmeldungOffen = true } }
            Button("Guthaben ansehen …") { EinstellungenAnsicht.startReiter = "guthaben"; AppSteuerung.shared.bereich = .einstellungen }
        } label: {
            HStack(spacing: 5) {
                if istClaude {
                    if let r = guthaben.anthropicRest {
                        // Ein alter Stand darf nicht wie ein aktueller grün aussehen.
                        Text("\(Guthaben.betrag(r)) $").font(.jarvis(11, .bold))
                            .foregroundStyle(guthaben.anthropicHinweis != nil ? Farbe.warnung : (knapp ? Farbe.rot : Farbe.gruen))
                    }
                    if let k = guthaben.kosten, k > 0 {
                        Text("−\(Self.kostenText(k)) $").font(.jarvis(11, .bold)).foregroundStyle(Farbe.rot)
                    }
                    if guthaben.anthropicRest != nil || (guthaben.kosten ?? 0) > 0 {
                        Text("·").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                    }
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
                  rest: g.anthropicRest, gewaehrt: g.anthropicGewaehrt, einheit: "$", umrechnung: g.kurs, text: g.anthropicText, hinweis: g.anthropicHinweis) {
                if g.macAngemeldet {
                    Button("Am Mac bei der Anthropic-Konsole abmelden") { log("guthaben", "Am Mac bei der Console abgemeldet", wo: "Guthaben"); g.abmelden(); Task { await g.laden(frisch: true) } }
                        .buttonStyle(.plain).font(.jarvis(13, .semibold)).foregroundStyle(Farbe.text2)
                } else {
                    Button { g.anmeldungOffen = true } label: {
                        Label("Bei der Anthropic-Konsole anmelden", systemImage: "arrow.up.right.square").font(.jarvis(13, .semibold)).underline()
                    }
                    .buttonStyle(.plain).foregroundStyle(Farbe.haupt).pointerStyle(.link)
                }
            }
            karte(titel: "Gemini-Stimmen (Google AI Studio)", symbol: "waveform", erklaerung: "Bezahlt das Vorlesen mit Gemini-TTS und das Live-Gespräch mit Gemini auf dem bezahlten Schlüssel.",
                  rest: g.googleRest, gewaehrt: g.googleGewaehrt, einheit: g.googleWaehrung, umrechnung: nil, text: g.googleText, hinweis: g.googleHinweis) { EmptyView() }
            HStack(spacing: 10) {
                KapselKnopf(symbol: "arrow.clockwise", titel: "Jetzt neu abfragen", hervorgehoben: true) { Task { await g.laden(frisch: true) } }
                if g.laedt { ProgressView().controlSize(.small) }
            }
            if let k = g.kosten, k > 0 {
                Text("Die letzte Aufgabe mit einem Claude-Modell hat zum Listenpreis \(ModellKapsel.kostenText(k)) $ gekostet, ohne Websuche.").font(.jarvis(13)).foregroundStyle(Farbe.text2)
            }
            Text("Die Anmeldung bei Google liegt auf dem Handy: Einstellungen → Guthaben.").font(.jarvis(12)).foregroundStyle(Farbe.text3)
        }
        .task { await g.laden() }
    }

    private func karte<Fuss: View>(titel: String, symbol: String, erklaerung: String, rest: Double?, gewaehrt: Double?, einheit: String, umrechnung: Double?, text: String?, hinweis: String?, @ViewBuilder fuss: () -> Fuss) -> some View {
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
                    if let hinweis { Text("Alter Stand. " + hinweis).font(.jarvis(13)).foregroundStyle(Farbe.warnung).fixedSize(horizontal: false, vertical: true) }
                    if let text {
                        ForEach(Array(Guthaben.punkte(text).enumerated()), id: \.offset) { _, p in
                            Text("• " + p).font(.jarvis(13)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                        }
                    }
                } else {
                    Text(hinweis ?? "Lade …").font(.jarvis(13)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                }
                fuss()
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
