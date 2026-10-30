import SwiftUI

/// Großer Startbildschirm wie am Handy: Kugel und Begrüßung, die Reiter als Kacheln, Tagesauswertung, unten die Sprechblase mit Modell und Kosten.
/// Die Reiter stehen zusätzlich links in der Leiste; die Zentrale bleibt daneben als Übersicht mit allen Karten.
struct StartAnsicht: View {
    @State private var daten = ZentraleDaten.shared
    @State private var steuerung = AppSteuerung.shared
    @State private var anhang: Data?

    private static let reiter: [Bereich] = [.zentrale, .live, .programmierung, .ablage, .gedaechtnis, .aktivitaet, .einstellungen]

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 22) {
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
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 170), spacing: 10)], spacing: 10) {
                        ForEach(Self.reiter) { b in
                            MacKachel(symbol: b.symbol, titel: b.titel, farbe: Farbe.haupt) { steuerung.bereich = b }
                        }
                    }
                    GlasKarte {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                KartenTitel(symbol: "sun.horizon.fill", titel: "Tagesauswertung")
                                Spacer()
                                if daten.laedt { ProgressView().controlSize(.small) }
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
