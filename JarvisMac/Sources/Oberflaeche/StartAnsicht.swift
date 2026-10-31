import SwiftUI

/// Großer Startbildschirm wie am Handy: Kugel und Begrüßung, Tagesauswertung, Schnell-Tags, unten die Blase mit Modell, Kosten und dem Knopf für den Live-Bildschirm.
/// Die Zentrale bleibt daneben als Übersicht mit allen Karten; alles Weitere liegt unter Einstellungen.
struct StartAnsicht: View {
    @State private var daten = ZentraleDaten.shared
    @State private var steuerung = AppSteuerung.shared
    @State private var anhang: Data?

    static let schnellFragen = [
        "Wie war mein Tag?", "Was steht heute an?", "Wie habe ich geschlafen?", "Prüfe meine Updates", "Wie ist das Wetter?", "Was steht morgen an?",
        "Welche Aufgaben sind offen?", "Wie ist meine Erholung?", "Wann ist mein nächster Dienst?", "Welche Termine habe ich?", "Wie ist meine HRV?",
        "Was ist neu in meinen Mails?", "Wie geht es meinem Mac?", "Welche Programmieraufträge laufen?", "Wie sieht meine Woche aus?", "Welche Wecker sind gestellt?",
        "Welche Ideen sind offen?", "Wie lange brauche ich zur Arbeit?", "Wie viel Guthaben habe ich?", "Was habe ich gestern gemacht?", "Wie war mein letztes Training?",
        "Was nehme ich heute ein?", "Schau dir meinen Bildschirm an", "Was hast du dir gemerkt?",
    ]

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
                    DreiZeilen(abstand: 8) {
                        ForEach(Self.schnellFragen, id: \.self) { frage in
                            SchnellTag(titel: frage) { steuerung.gespraech = nil; steuerung.sende(frage) }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
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

private struct SchnellTag: View {
    let titel: String
    let aktion: () -> Void
    @State private var schwebt = false
    var body: some View {
        Button(action: aktion) {
            Text(titel).font(.jarvis(13, .medium)).foregroundStyle(Farbe.text).lineLimit(1).fixedSize()
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(Capsule().fill(Farbe.glas(schwebt ? 0.13 : 0.07)))
                .overlay(Capsule().strokeBorder(Farbe.glas(schwebt ? 0.18 : 0.10)))
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .onHover { schwebt = $0 }
    }
}

/// Bricht bei voller Zeile um und zeigt in jeder Fensterbreite genau drei Zeilen; was nicht mehr hineinpasst, bleibt verborgen.
private struct DreiZeilen: Layout {
    var abstand: CGFloat
    private let zeilen = 3

    /// Je Kind die Stelle innerhalb der Fläche, nil = passt nicht mehr.
    private func anordnung(_ kinder: Subviews, breite: CGFloat) -> (stellen: [CGPoint?], hoehe: CGFloat) {
        var stellen: [CGPoint?] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var zeile = 0
        var zeilenHoehe: CGFloat = 0
        for k in kinder {
            let g = k.sizeThatFits(.unspecified)
            if x > 0 && x + g.width > breite {
                zeile += 1
                if zeile >= zeilen { stellen.append(nil); continue }
                x = 0
                y += zeilenHoehe + abstand
                zeilenHoehe = 0
            }
            if zeile >= zeilen { stellen.append(nil); continue }
            stellen.append(CGPoint(x: x, y: y))
            x += g.width + abstand
            zeilenHoehe = max(zeilenHoehe, g.height)
        }
        return (stellen, y + zeilenHoehe)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let breite = proposal.width ?? 700
        return CGSize(width: breite, height: anordnung(subviews, breite: breite).hoehe)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let stellen = anordnung(subviews, breite: bounds.width).stellen
        for (k, stelle) in zip(subviews, stellen) {
            // Verborgene Kinder liegen weit außerhalb des Fensters.
            let p = stelle.map { CGPoint(x: bounds.minX + $0.x, y: bounds.minY + $0.y) } ?? CGPoint(x: -100_000, y: -100_000)
            k.place(at: p, anchor: .topLeading, proposal: .unspecified)
        }
    }
}
