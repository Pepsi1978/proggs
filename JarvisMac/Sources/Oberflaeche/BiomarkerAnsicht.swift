import SwiftUI

/// Der Biomarker-Bereich am Mac, nach dem Biomarker-Bereich des Handys (bio/presentation): Abrufstand je Quelle, alle Messgrößen als Karten
/// mit aktuellem Wert, Einordnung gegen die Tage davor, Schnitten 7/30/90 und Verlauf als Balken; Zeitraum 7, 30 oder 90 Tage; Tageswerte eines Tages.
/// Die Karten lassen sich wie am Handy per Drag & Drop umsortieren.
struct BiomarkerAnsicht: View {
    @State private var daten = BioDaten.shared
    @State private var reihe = BioDaten.reihenfolge()
    @State private var ziehen = ZiehZustand()
    @State private var tagOffen = false
    @State private var auswertungOffen = false
    @MainActor private static var letzterTag = "heute"
    @State private var tag = BiomarkerAnsicht.letzterTag

    private var sichtbar: [String] {
        reihe.filter { id in
            guard let g = BioDaten.groessen.first(where: { $0.id == id }) else { return false }
            let (w, v) = daten.zu(g)
            return w != nil || !(v?.punkte.isEmpty ?? true)
        }
    }

    var body: some View {
        let _ = daten.stand
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                BereichsKopf(symbol: "bolt.heart.fill", titel: "Biomarker", untertitel: "Spiegel des Handys: Whoop, Oura, Waage und Trainings, ausgewertet wie in Jarvis am Handy.")
                knoepfe
                status
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 300), spacing: 12)], spacing: 12) {
                    ForEach(sichtbar, id: \.self) { id in
                        if let g = BioDaten.groessen.first(where: { $0.id == id }) {
                            let (w, v) = daten.zu(g)
                            BioKarte(groesse: g, wert: w, verlauf: v)
                                .ziehbar(id, reihe: $reihe, zustand: ziehen)
                        }
                    }
                }
                .ziehStapel(ziehen) { ids in
                    BioDaten.reihenfolgeSpeichern(ids)
                    log("biomarker", "Kartenreihenfolge geändert", wo: "Biomarker")
                }
                KlappKarte(symbol: "calendar", titel: "Tageswerte", farbe: Farbe.gold, untertitel: "Alle Werte eines Tages samt Trainings", offen: $tagOffen) {
                    VStack(alignment: .leading, spacing: 12) {
                        Picker("", selection: $tag) {
                            Text("Heute").tag("heute"); Text("Gestern").tag("gestern"); Text("Vorgestern").tag("vorgestern")
                        }
                        .pickerStyle(.segmented).labelsHidden().frame(width: 320)
                        MarkdownText(text: daten.tagText ?? "Lade …", groesse: 13).frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .onChange(of: tagOffen) { _, an in if an { Task { await daten.tagLaden(tag) } } }
                .onChange(of: tag) { _, w in BiomarkerAnsicht.letzterTag = w; Task { await daten.tagLaden(w) } }
                KlappKarte(symbol: "doc.text.fill", titel: "Auswertung im Wortlaut", farbe: Farbe.haupt, untertitel: "So, wie Jarvis sie am Handy rechnet", offen: $auswertungOffen) {
                    MarkdownText(text: daten.auswertungText ?? "", groesse: 13).frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(36)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .scrollIndicators(.never)
        .task { await daten.laden() }
    }

    private var knoepfe: some View {
        HStack(spacing: 10) {
            Picker("", selection: Binding(get: { daten.tage }, set: { t in Task { await daten.laden(tage: t) } })) {
                Text("7 Tage").tag(7); Text("30 Tage").tag(30); Text("90 Tage").tag(90)
            }
            .pickerStyle(.segmented).labelsHidden().frame(width: 260)
            Spacer()
            if daten.laedt { ProgressView().controlSize(.small) }
            RundKnopf(symbol: "arrow.clockwise", hilfe: "Vom Handy neu lesen", groesse: 32) { Task { await daten.laden(erzwingen: true) } }
            KapselKnopf(symbol: daten.holtFrisch ? "clock" : "arrow.down.circle.fill", titel: daten.holtFrisch ? "Holt frische Werte …" : "Frische Werte holen") {
                Task { await daten.frischHolen() }
            }
            .help("Bittet das Handy, jetzt bei Whoop, Oura und Waage abzurufen (bis zu einer Minute). Nur bei Bedarf: Jeder Abruf belastet die Whoop-Anmeldung.")
        }
    }

    private var status: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let z = daten.zuletzt {
                Text("Gelesen \(z.formatted(.relative(presentation: .named))) (\(z.formatted(date: .omitted, time: .shortened)) Uhr)")
                    .font(.jarvis(12)).foregroundStyle(Farbe.text3)
            }
            ForEach(Array(daten.kopf.enumerated()), id: \.offset) { _, k in
                Text(k).font(.jarvis(12)).foregroundStyle(k.hasPrefix("Hinweis") ? Farbe.warnung : Farbe.text2)
            }
            if let a = daten.abrufMeldung {
                MarkdownText(text: a, groesse: 12).padding(.horizontal, 12).padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.glas(0.04)))
            }
            if let f = daten.fehler { Text(f).font(.jarvis(12, .semibold)).foregroundStyle(Farbe.warnung) }
            if daten.werte.isEmpty && !daten.laedt && daten.fehler == nil { Text("Noch keine Werte vom Handy.").font(.jarvis(13)).foregroundStyle(Farbe.text2) }
        }
    }
}

/// Eine Messgröße: Name, Quelle, Wert, Einordnung und der Verlauf als Balken mit Schnittlinie, der jüngste Tag kräftig.
struct BioKarte: View {
    let groesse: BioGroesse
    let wert: BioWert?
    let verlauf: BioVerlauf?

    private var einordnungFarbe: Color {
        guard let e = wert?.einordnung else { return Farbe.text2 }
        return e.contains("besser") ? Farbe.gruen : e.contains("schlechter") ? Farbe.rot : Farbe.text2
    }

    private var wertText: String {
        if let w = wert?.wert { return w }
        if let l = verlauf?.punkte.last { return "\(l.wert.formatted(.number.precision(.fractionLength(0...1)))) \(verlauf?.einheit ?? "")" }
        return "–"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(groesse.titel).font(.jarvis(14, .semibold)).lineLimit(1)
                Spacer()
                Text(wert?.quelle ?? verlauf?.quelle ?? groesse.gruppe).font(.jarvis(11)).foregroundStyle(Farbe.text3).lineLimit(1)
            }
            HStack(spacing: 10) {
                Text(wertText).font(.jarvis(24, .bold))
                if let d = wert?.datum { Text("vom " + ListenArten.datumKurz(d)).font(.jarvis(11)).foregroundStyle(Farbe.warnung) }
                Spacer()
            }
            if let w = wert, !w.einordnung.isEmpty { Text(w.einordnung).font(.jarvis(12, .semibold)).foregroundStyle(einordnungFarbe) }
            if let sd = wert?.schnittDavor {
                Text("Schnitt davor \(sd)" + (wert?.schnitte.map { " · 7/30/90 Tage: \($0)" } ?? "")).font(.jarvis(11)).foregroundStyle(Farbe.text3)
            }
            if let v = verlauf, v.punkte.count > 1 { BioBalken(verlauf: v, richtung: wert?.richtung ?? "neutral") }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(Farbe.glas(0.055)))
        .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(Farbe.glas(0.10)))
        .contentShape(Rectangle())
        .help("Ziehen zum Umsortieren")
    }
}

private struct BioBalken: View {
    let verlauf: BioVerlauf
    let richtung: String
    private let hoehe: CGFloat = 64

    var body: some View {
        let punkte = verlauf.punkte
        let werte = punkte.map(\.wert)
        let minW = min(werte.min() ?? 0, verlauf.schnitt ?? .greatestFiniteMagnitude)
        let maxW = max(werte.max() ?? 0, verlauf.schnitt ?? -.greatestFiniteMagnitude)
        let unten = minW - (maxW - minW) * 0.25
        let spanne = max(1e-9, maxW - unten)
        // Farben vor dem Zeichnen lesen: Die Darstellung soll beim Umschalten mitgehen.
        let haupt = Farbe.haupt, gruen = Farbe.gruen, rot = Farbe.rot, linie = Farbe.text2
        let f = DateFormatter()
        let _ = f.dateFormat = "dd.MM."
        VStack(spacing: 4) {
            ZStack(alignment: .topLeading) {
                HStack(alignment: .bottom, spacing: 2) {
                    ForEach(Array(punkte.enumerated()), id: \.offset) { i, p in
                        let besser = verlauf.schnitt.map { s in richtung != "neutral" && (richtung == "besser" ? p.wert >= s : p.wert <= s) } ?? false
                        let farbe = i == punkte.count - 1 ? haupt : (richtung == "neutral" || verlauf.schnitt == nil) ? haupt.opacity(0.45) : besser ? gruen.opacity(0.6) : rot.opacity(0.55)
                        RoundedRectangle(cornerRadius: 2).fill(farbe)
                            .frame(maxWidth: .infinity)
                            .frame(height: max(2, CGFloat((p.wert - unten) / spanne) * hoehe))
                            .help("\(f.string(from: p.tag)): \(p.wert.formatted(.number.precision(.fractionLength(0...2)))) \(verlauf.einheit)")
                    }
                }
                .frame(height: hoehe, alignment: .bottom)
                if let s = verlauf.schnitt {
                    let y = hoehe - CGFloat((s - unten) / spanne) * hoehe
                    Rectangle().fill(linie.opacity(0.7)).frame(height: 1).offset(y: min(max(y, 0), hoehe - 1)).allowsHitTesting(false)
                }
            }
            .frame(height: hoehe)
            .clipped()
            HStack(spacing: 6) {
                Text(f.string(from: punkte[0].tag))
                Spacer()
                if let s = verlauf.schnitt { Text("Schnitt \(s.formatted(.number.precision(.fractionLength(0...2)))) \(verlauf.einheit)") }
                Spacer()
                Text(f.string(from: punkte[punkte.count - 1].tag))
            }
            .font(.jarvis(10)).foregroundStyle(Farbe.text3)
        }
    }
}

/// Startkarte „Biomarker“ in der Zentrale, wie BiomarkerStartKarte am Handy: die Kerngrößen mit Einordnung, Klick öffnet den Bereich.
struct BiomarkerKarte: View {
    @State private var d = BioDaten.shared

    var body: some View {
        let _ = d.stand
        let kern: [(BioGroesse, BioWert)] = BioDaten.kern.compactMap { id in
            guard let g = BioDaten.groessen.first(where: { $0.id == id }), let w = d.zu(g).wert else { return nil }
            return (g, w)
        }
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    KartenTitel(symbol: "bolt.heart.fill", titel: "Biomarker", farbe: Farbe.rot)
                    Spacer()
                    if d.laedt { ProgressView().controlSize(.small) }
                    RundKnopf(symbol: "arrow.clockwise", hilfe: "Vom Handy neu lesen", groesse: 30) { Task { await d.laden(erzwingen: true) } }
                    RundKnopf(symbol: "arrow.up.right.square", hilfe: "Biomarker-Bereich öffnen", groesse: 30) { AppSteuerung.shared.bereich = .biomarker }
                }
                if let abruf = d.kopf.first(where: { $0.hasPrefix("Letzter Abruf") }) {
                    Text(abruf).font(.jarvis(12)).foregroundStyle(Farbe.text3).lineLimit(2)
                }
                if kern.isEmpty {
                    Text(d.laedt ? "Lade die Biomarker vom Handy …" : d.fehler ?? "Noch keine Werte vom Handy.").font(.jarvis(13)).foregroundStyle(Farbe.text2)
                } else {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 8)], spacing: 8) {
                        ForEach(kern, id: \.0.id) { g, w in
                            let farbe = w.einordnung.contains("besser") ? Farbe.gruen : w.einordnung.contains("schlechter") ? Farbe.rot : Farbe.text3
                            VStack(alignment: .leading, spacing: 2) {
                                Text(g.titel).font(.jarvis(12)).foregroundStyle(Farbe.text2).lineLimit(1)
                                Text(w.wert).font(.jarvis(17, .bold)).lineLimit(1)
                                Text(w.einordnung).font(.jarvis(11)).foregroundStyle(farbe).lineLimit(1)
                            }
                            .padding(.horizontal, 12).padding(.vertical, 10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(RoundedRectangle(cornerRadius: 12).fill(Farbe.glas(0.04)))
                        }
                    }
                    if let f = d.fehler { Text(f).font(.jarvis(12)).foregroundStyle(Farbe.warnung) }
                }
            }
        }
    }
}

/// Startkarte „Solarbatterie“ in der Zentrale, wie VictronStartKarte am Handy: BMV-712 und Laderegler mit Aktualisieren-Knopf.
struct SolarKarte: View {
    @State private var s = SolarDaten.shared

    var body: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    VStack(alignment: .leading, spacing: 2) {
                        KartenTitel(symbol: "sun.max.fill", titel: "Solarbatterie", farbe: Farbe.gold)
                        Text(s.laeuft ? "Liest die Anlage …" : s.standText).font(.jarvis(12)).foregroundStyle(s.laeuft ? Farbe.haupt : Farbe.text3).lineLimit(1)
                    }
                    Spacer()
                    if s.laeuft { ProgressView().controlSize(.small) }
                    RundKnopf(symbol: "arrow.clockwise", hilfe: s.laeuft ? "Solarbatterie wird gelesen" : "Solarbatterie aktualisieren", aktiv: s.laeuft, groesse: 30) {
                        Task { await s.lesen() }
                    }
                }
                if let f = s.fehler, !s.laeuft { Text(f).font(.jarvis(12)).foregroundStyle(Farbe.warnung) }
                if let b = s.geraet("batteriemonitor") {
                    if let soc = (b["ladezustand_prozent"] as? NSNumber)?.doubleValue {
                        ladebalken(soc, (b["strom_a"] as? NSNumber)?.doubleValue ?? 0)
                    }
                    abschnitt("Batterie (BMV-712)", SolarDaten.batterieZeilen(b))
                }
                if let l = s.geraet("laderegler") { abschnitt("Laderegler (MPPT)", SolarDaten.reglerZeilen(l)) }
            }
        }
    }

    private func ladebalken(_ soc: Double, _ strom: Double) -> some View {
        let farbe = soc >= 60 ? Farbe.gruen : soc >= 30 ? Farbe.gold : Farbe.rot
        let grund = Farbe.glas(0.08)
        return VStack(alignment: .leading, spacing: 4) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(grund)
                    Capsule().fill(farbe).frame(width: max(14, geo.size.width * min(max(soc, 0), 100) / 100))
                }
            }
            .frame(height: 14)
            Text("\(soc.formatted(.number.precision(.fractionLength(0...1)))) %" + (strom > 0.05 ? " · lädt" : strom < -0.05 ? " · entlädt" : ""))
                .font(.jarvis(12, .semibold)).foregroundStyle(farbe)
        }
    }

    private func abschnitt(_ titel: String, _ zeilen: [(String, String)]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(titel).font(.jarvis(13, .semibold)).foregroundStyle(Farbe.haupt)
            ForEach(Array(zeilen.enumerated()), id: \.offset) { _, z in
                HStack(spacing: 8) {
                    Text(z.0).font(.jarvis(13)).foregroundStyle(Farbe.text2)
                    Spacer()
                    Text(z.1).font(.jarvis(13, .semibold))
                }
            }
        }
    }
}
