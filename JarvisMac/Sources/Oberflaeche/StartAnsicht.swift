import SwiftUI

/// Großer Startbildschirm wie am Handy: Kugel und Begrüßung, Tagesauswertung, unten die Blase mit Modell, Kosten und dem Knopf für den Live-Bildschirm.
/// Die Zentrale bleibt daneben als Übersicht mit allen Karten; alles Weitere liegt unter Einstellungen.
struct StartAnsicht: View {
    @State private var daten = ZentraleDaten.shared
    @State private var steuerung = AppSteuerung.shared
    @State private var anhang: Data?

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 22) {
                    ZugangsHinweis()
                    Button { steuerung.kuerzelSprechen() } label: { KugelLive(groesse: 150) }
                        .buttonStyle(.plain)
                        .help("Antippen zum Sprechen (⌥⌘J überall)")
                        .padding(.top, 30)
                    VStack(spacing: 6) {
                        HStack(spacing: 10) {
                            LivePunkt()
                            Text(ZentraleAnsicht.gruss).font(.jarvis(34, .bold))
                        }
                        Text(ZentraleAnsicht.datumZeile).font(.jarvis(14, .medium)).foregroundStyle(Farbe.text2)
                    }
                    GlasKarte {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                KartenTitel(symbol: "sun.horizon.fill", titel: "Tagesauswertung")
                                Spacer()
                                if daten.laedt { ProgressView().controlSize(.small) }
                                RundKnopf(symbol: "arrow.clockwise", hilfe: "Neu laden", groesse: 30) { Task { await daten.laden(erzwingen: true) } }
                            }
                            if let a = daten.auswertung {
                                MarkdownText(text: ZentraleAnsicht.anfang(a), groesse: 14).frame(maxWidth: .infinity, alignment: .leading)
                                Button("Alles in der Zentrale ansehen") { steuerung.bereich = .zentrale }
                                    .buttonStyle(.plain).font(.jarvis(13, .semibold)).foregroundStyle(Farbe.haupt)
                            } else {
                                Text(daten.laedt ? "Lade deine Tagesauswertung …" : "Das Handy-Jarvis ist gerade nicht erreichbar.")
                                    .font(.jarvis(14)).foregroundStyle(Farbe.text2)
                            }
                        }
                    }
                    VStack(spacing: 8) {
                        ForEach(ZentraleAnsicht.vorschlaege, id: \.self) { v in
                            KapselKnopf(symbol: "arrow.turn.down.right", titel: v, voll: true) { steuerung.gespraech = nil; steuerung.sende(v) }
                        }
                    }
                }
                .padding(.horizontal, 40).padding(.bottom, 20)
                .frame(maxWidth: 860)
                .frame(maxWidth: .infinity)
            }
            .scrollIndicators(.never)
            EingabeBlase(gespraech: nil, anhang: $anhang)
        }
        .task { await daten.laden() }
    }
}
