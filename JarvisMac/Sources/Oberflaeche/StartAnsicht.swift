import SwiftUI

/// Großer Startbildschirm: Kugel und Begrüßung, Schnell-Tags, unten die Blase mit Modell, Kosten und dem Knopf für den Live-Bildschirm.
/// Die Tagesauswertung steht nur in der Zentrale (die Daten werden hier schon vorgeladen); alles Weitere liegt unter Einstellungen.
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

    /// 150 Punkte (rund 3,5 cm) Luft zur Eingabezeile; in kleinen Fenstern weniger, damit Kugel und Begrüßung sichtbar bleiben.
    private func abstandZurBlase(fensterHoehe: CGFloat) -> CGFloat {
        let platzFuerDenRest: CGFloat = 510
        return min(150, max(12, fensterHoehe - platzFuerDenRest))
    }

    var body: some View {
        GeometryReader { fenster in
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
                        if daten.letzterFehlschlag != nil && !daten.laedt {
                            Text("Das Handy-Jarvis ist gerade nicht erreichbar.").font(.jarvis(13)).foregroundStyle(Farbe.warnung)
                        }
                    }
                }
                .padding(.horizontal, 64).padding(.bottom, 20)
                .frame(maxWidth: 884)
                .frame(maxWidth: .infinity)
            }
            .scrollIndicators(.never)
            // Fest über der Blase, damit die Tags ohne Scrollen zu sehen sind.
            DreiZeilen(abstand: 8) {
                ForEach(Self.schnellFragen, id: \.self) { frage in
                    SchnellTag(titel: frage) { steuerung.gespraech = nil; steuerung.sende(frage) }
                }
            }
            .padding(.horizontal, 64).padding(.top, 10).padding(.bottom, abstandZurBlase(fensterHoehe: fenster.size.height))
            .frame(maxWidth: 980)
            .frame(maxWidth: .infinity)
            .clipped()
            EingabeBlase(gespraech: nil, anhang: $anhang)
        }
        }
        .onAppear { daten.vorladen() }
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
        // Verborgene Tags dürfen nie den Tastaturfokus bekommen.
        .focusable(false)
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
