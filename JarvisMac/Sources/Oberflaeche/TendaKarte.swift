import SwiftUI

/// Stand der Tenda-Karte: kommt fertig bewertet vom Handy-Jarvis (Werkzeug tenda_lesen, structuredContent), damit Mac, Windows,
/// Handy und Sprache dieselben Texte zeigen. Schalten (Modus, Connect, Neustart) läuft über dieselben Handy-Werkzeuge.
@MainActor @Observable
final class TendaDaten {
    static let shared = TendaDaten()
    private(set) var stand: JSONObjekt?
    private(set) var laeuft = false
    private(set) var meldung: String?
    /** Ein Schaltbefehl ist unterwegs: kein zweiter (Doppelklick hieße zwei Neustarts). */
    private(set) var schaltet = false
    @ObservationIgnored var automatischGelesen = false

    func lesen() async {
        guard !laeuft else { return }
        laeuft = true
        defer { laeuft = false }
        if let s = await McpKlient.shared.struktur("tenda_lesen", [:], sekunden: 150) { stand = s; meldung = nil }
        else { meldung = "Das Handy-Jarvis ist gerade nicht erreichbar oder hat keinen Router-Stand." }
    }

    func schalten(_ werkzeug: String, _ args: JSONObjekt) async {
        guard !schaltet else { return }
        schaltet = true
        defer { schaltet = false }
        meldung = "Befehl wird gesendet …"
        // Das Handy kann bei Mac-Ausfall direkt schalten (mehrere Router-Aufrufe): genug Zeit lassen.
        let e = await McpKlient.shared.rufe(werkzeug, args, sekunden: 120)
        meldung = McpKlient.ohneKontextHinweis(e.text)
    }
}

/// Karte „Tenda Internet“ in der Zentrale: zugeklappt Modus, Multiplayer-Einschätzung, Stand und rechts der Aktualisieren-Knopf,
/// aufgeklappt Verbindung, LTE- und 5G-Werte mit Bewertung und die Bedienung (Modus, Connect, Neustart mit Ja/Nein).
struct TendaKarte: View {
    @State private var d = TendaDaten.shared
    @State private var offen = false
    @State private var neustartFrage = false

    private var mp: JSONObjekt? { d.stand?["multiplayer"] as? JSONObjekt }

    var body: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 8) {
                    Button { withAnimation(.easeOut(duration: 0.15)) { offen.toggle() } } label: {
                        VStack(alignment: .leading, spacing: 3) {
                            HStack(spacing: 8) {
                                KartenTitel(symbol: "antenna.radiowaves.left.and.right", titel: "Tenda Internet", farbe: Farbe.haupt)
                                if let m = d.stand?["modus_aktiv"] as? String {
                                    Text(m.replacingOccurrences(of: " (abgeleitet)", with: "")).font(.jarvis(12, .semibold)).foregroundStyle(Farbe.haupt)
                                        .padding(.horizontal, 8).padding(.vertical, 2).background(Capsule().fill(Farbe.haupt.opacity(0.14)))
                                }
                            }
                            Text(mp.map { "Multiplayer: " + (($0["text"] as? String) ?? "") } ?? (d.laeuft ? "Liest den Router …" : "Noch kein Stand"))
                                .font(.jarvis(14, .semibold)).foregroundStyle(stufenFarbe(mp?["stufe"] as? String))
                            Text(d.laeuft ? "Frische Abfrage läuft …" : standZeile).font(.jarvis(12)).foregroundStyle(d.laeuft ? Farbe.haupt : veraltet ? Farbe.warnung : Farbe.text3).lineLimit(1)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    if d.laeuft { ProgressView().controlSize(.small) }
                    RundKnopf(symbol: "arrow.clockwise", hilfe: "Router frisch abfragen", aktiv: d.laeuft, groesse: 30) { Task { await d.lesen() } }
                    Image(systemName: "chevron.down").foregroundStyle(Farbe.text3).rotationEffect(.degrees(offen ? 180 : 0)).padding(.top, 8)
                        .onTapGesture { withAnimation(.easeOut(duration: 0.15)) { offen.toggle() } }
                }
                if let m = d.meldung { Text(m).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
                if offen { aufgeklappt }
            }
        }
        .onAppear {
            if !d.automatischGelesen { d.automatischGelesen = true; Task { await d.lesen() } }
        }
        .alert("Router neu starten", isPresented: $neustartFrage) {
            Button("Ja", role: .destructive) { Task { await d.schalten("tenda_neustart", ["bestaetigt": true]) } }
            Button("Nein", role: .cancel) {}
        } message: { Text("Willst du den Router wirklich neu starten?") }
    }

    /** Alter beim Anzeigen berechnet (nicht beim Lesen), damit „aktuell“ nicht stehen bleibt; ab 90 Minuten veraltet wie am Handy. */
    private var zeit: Date? { (d.stand?["zeit"] as? NSNumber).map { Date(timeIntervalSince1970: $0.doubleValue) } }
    private var veraltet: Bool { (d.stand?["veraltet_grund"] as? String) != nil || (zeit.map { Date().timeIntervalSince($0) > 5400 } ?? false) }

    private var standZeile: String {
        guard let s = d.stand, let z = zeit else { return "" }
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE")
        f.dateFormat = Calendar.current.isDateInToday(z) ? "HH:mm 'Uhr'" : "dd.MM. HH:mm 'Uhr'"
        return "Stand " + f.string(from: z) + " · " + ((s["quelle"] as? String) ?? "") + (veraltet ? " · veraltet" : "")
    }

    @ViewBuilder private var aufgeklappt: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let b = mp?["begruendung"] as? String { Text("Multiplayer-Grundlage: " + b).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
            ForEach(Array(((d.stand?["abschnitte"] as? [JSONObjekt]) ?? []).enumerated()), id: \.offset) { _, a in
                Text((a["titel"] as? String) ?? "").font(.jarvis(13, .semibold)).foregroundStyle(Farbe.haupt).padding(.top, 4)
                ForEach(Array(((a["zeilen"] as? [JSONObjekt]) ?? []).enumerated()), id: \.offset) { _, z in
                    HStack(spacing: 8) {
                        Text((z["name"] as? String) ?? "").font(.jarvis(13)).foregroundStyle(Farbe.text2)
                        Spacer()
                        let wert = (z["wert"] as? String) ?? ""
                        Text(wert).font(.jarvis(13, wert.hasPrefix("nicht ") ? .regular : .semibold)).foregroundStyle(wert.hasPrefix("nicht ") ? Farbe.text3 : Farbe.text)
                        if let bew = z["bewertung"] as? String {
                            Text(bew).font(.jarvis(11, .semibold)).foregroundStyle(stufenFarbe(bew))
                                .padding(.horizontal, 6).padding(.vertical, 1).background(Capsule().fill(stufenFarbe(bew).opacity(0.14)))
                        }
                    }
                    .help((z["hinweis"] as? String) ?? "")
                }
            }
            Text("Modus eingestellt: " + ((d.stand?["modus_eingestellt"] as? String) ?? "nicht gelesen") + " · aktiv: " + ((d.stand?["modus_aktiv"] as? String) ?? "nicht geliefert"))
                .font(.jarvis(12)).foregroundStyle(Farbe.text2).padding(.top, 4)
            HStack(spacing: 8) {
                Spacer()
                Menu("Modus wählen") {
                    Button("5G SA bevorzugt") { Task { await d.schalten("tenda_modus_setzen", ["modus": "5g_sa"]) } }
                    Button("5G NSA") { Task { await d.schalten("tenda_modus_setzen", ["modus": "5g_nsa"]) } }
                    Button("nur 4G") { Task { await d.schalten("tenda_modus_setzen", ["modus": "4g"]) } }
                }
                .fixedSize()
                Button("Connect") { Task { await d.schalten("tenda_verbinden", [:]) } }
                Button("Router neu starten") { neustartFrage = true }
            }
            .disabled(d.schaltet)
            Text("Moduswechsel, Connect und Neustart unterbrechen das Internet kurz, auch für diesen Mac. Jarvis prüft danach den neuen Zustand und löst unklare Befehle nicht erneut aus.")
                .font(.jarvis(11)).foregroundStyle(Farbe.text3)
        }
    }

    private func stufenFarbe(_ s: String?) -> Color {
        switch s {
        case "exzellent", "gut": return Farbe.gruen
        case "mittel": return Farbe.warnung
        case "schlecht", "sehr schlecht": return Farbe.rot
        default: return Farbe.text
        }
    }
}
