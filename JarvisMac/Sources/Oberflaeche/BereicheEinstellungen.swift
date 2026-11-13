import SwiftUI

/// Einstellungen → Zentrale & Bereiche: Karten der Zentrale (zeigen, Reihenfolge), Listen-Abgleich, Biomarker und Solarbatterie.
/// Jede Einstellung steht auch im Katalog von mac_einstellungen, damit Frank sie per Sprache abfragen und umstellen kann.
struct BereicheEinstellungen: View {
    static let zentraleKarten: [(id: String, titel: String)] = [
        ("mac", "Am Mac"), ("auswertung", "Tagesauswertung"), ("biomarker", "Biomarker"), ("solar", "Solarbatterie"), ("aufgaben", "Aufgaben heute"), ("termine", "Termine"), ("wetter", "Wetter"),
    ]

    @State private var einst = Einstellungen.shared

    private var ausgeblendet: [String] {
        einst.zentraleAusgeblendet.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    private func zeile<V: View>(_ titel: String, _ beschreibung: String = "", @ViewBuilder _ inhalt: () -> V) -> some View {
        HStack(alignment: .center, spacing: 16) {
            VStack(alignment: .leading, spacing: 3) {
                Text(titel).font(.jarvis(14, .semibold))
                if !beschreibung.isEmpty { Text(beschreibung).font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true) }
            }
            Spacer(minLength: 20)
            inhalt()
        }
        .padding(.vertical, 6)
    }

    private func sichtbar(_ id: String) -> Binding<Bool> {
        Binding(get: { !ausgeblendet.contains(id) }, set: { an in
            var l = ausgeblendet
            if an { l.removeAll { $0 == id } } else if !l.contains(id) { l.append(id) }
            einst.zentraleAusgeblendet = l.joined(separator: ",")
        })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "sparkles", titel: "Zentrale")
                    Text("Karten lassen sich in der Zentrale per Drag & Drop verschieben. Hier blendest du sie ein oder aus.").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    ForEach(Self.zentraleKarten, id: \.id) { k in
                        zeile(k.titel) { Toggle("", isOn: sichtbar(k.id)).toggleStyle(.switch).labelsHidden() }
                    }
                    zeile("Reihenfolge zurücksetzen", "Stellt die Werks-Reihenfolge der Zentrale wieder her.") {
                        KapselKnopf(symbol: "arrow.uturn.backward", titel: "Zurücksetzen") {
                            einst.zentraleReihenfolge = ""
                            log("einstellung", "Zentrale-Reihenfolge zurückgesetzt", wo: "Einstellungen")
                        }
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "checklist", titel: "Listen", farbe: Farbe.gruen)
                    zeile("Mit Handy und Tablet abgleichen", "Ablage → Listen sind dieselben Listen wie auf Handy und Tablet. Aus: Der Mac zeigt und ändert nur seine Kopie, nichts geht hoch, bis du es wieder einschaltest.") {
                        Toggle("", isOn: $einst.listenAbgleichAn).toggleStyle(.switch).labelsHidden()
                            .onChange(of: einst.listenAbgleichAn) { _, _ in ListenSpeicher.shared.jetztAbgleichen() }
                    }
                    zeile("Jetzt abgleichen", "Holt sofort den Stand vom Server und schickt Offenes los.") {
                        KapselKnopf(symbol: "arrow.clockwise", titel: "Abgleichen") { ListenSpeicher.shared.jetztAbgleichen() }
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "bolt.heart.fill", titel: "Biomarker", farbe: Farbe.rot)
                    Text("Der Mac ist ein Spiegel des Handys: Abgerufen wird nur am Handy (Whoop verträgt einen Abrufer). Die fachlichen Biomarker-Einstellungen (Quellen, Anmeldungen, Abrufzeiten) stehen am Handy.")
                        .font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                    zeile("Zeitraum", "Vergleich und Verlauf im Biomarker-Bereich.") {
                        StufenWahl(stufen: ["7 Tage", "30 Tage", "90 Tage"], wahl: Binding(get: { "\(einst.biomarkerTage) Tage" }, set: { w in
                            guard let t = Int(w.split(separator: " ").first ?? "") else { return }
                            einst.biomarkerTage = t
                            Task { await BioDaten.shared.laden(tage: t) }
                        }))
                        .frame(width: 260)
                    }
                    zeile("Beim Öffnen der Zentrale lesen", "Liest Auswertung und Verläufe vom Handy, höchstens alle fünf Minuten.") {
                        Toggle("", isOn: $einst.biomarkerBeimStart).toggleStyle(.switch).labelsHidden()
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "sun.max.fill", titel: "Solarbatterie", farbe: Farbe.gold)
                    zeile("Beim Start lesen", "Einmal je App-Start von selbst, sobald die Zentrale aufgeht (wie am Handy).") {
                        Toggle("", isOn: $einst.solarBeimStart).toggleStyle(.switch).labelsHidden()
                    }
                    zeile("Regelmäßig lesen", "Zusätzlich im Takt, solange Jarvis läuft; nie während eines Sprachgesprächs.") {
                        Picker("", selection: $einst.solarTaktMinuten) {
                            Text("nur Start und Knopf").tag(0); Text("alle 5 Minuten").tag(5); Text("alle 15 Minuten").tag(15)
                            Text("alle 30 Minuten").tag(30); Text("jede Stunde").tag(60)
                        }
                        .labelsHidden().frame(maxWidth: 220)
                    }
                }
            }
        }
    }
}
