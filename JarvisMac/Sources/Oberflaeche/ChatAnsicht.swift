import AppKit
import SwiftUI
import UniformTypeIdentifiers

struct ChatAnsicht: View {
    let gespraech: Gespraech
    @State private var steuerung = AppSteuerung.shared
    @State private var anhang: Data?

    var body: some View {
        VStack(spacing: 0) {
            kopf
            ScrollViewReader { leser in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 18) {
                        if gespraech.nachrichten.isEmpty { leer }
                        ForEach(gespraech.nachrichten) { n in
                            NachrichtAnsicht(n: n, gespraech: gespraech).id(n.id)
                        }
                        Color.clear.frame(height: 8).id("ende")
                    }
                    .padding(.horizontal, 40).padding(.vertical, 24)
                    .frame(maxWidth: 940)
                    .frame(maxWidth: .infinity)
                }
                .scrollIndicators(.automatic)
                .defaultScrollAnchor(.bottom)
                .onChange(of: gespraech.nachrichten.last?.text) { _, _ in leser.scrollTo("ende", anchor: .bottom) }
                .onChange(of: gespraech.nachrichten.count) { _, _ in withAnimation { leser.scrollTo("ende", anchor: .bottom) } }
            }
            EingabeBlase(gespraech: gespraech, anhang: $anhang)
        }
        .onDrop(of: [.image, .fileURL], isTargeted: nil) { anbieter in
            guard let a = anbieter.first else { return false }
            _ = a.loadDataRepresentation(for: .image) { d, _ in
                if let d, let jpg = NSImage(data: d)?.jpgDaten() { Task { @MainActor in anhang = jpg } }
            }
            return true
        }
    }

    private var kopf: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(gespraech.titel).font(.jarvis(17, .semibold)).lineLimit(1)
                Text(gespraech.erstellt.formatted(date: .abbreviated, time: .shortened)).font(.jarvis(11)).foregroundStyle(Farbe.text3)
            }
            Spacer()
            if gespraech.arbeitet {
                HStack(spacing: 8) {
                    ProgressView().controlSize(.small)
                    Text("Jarvis arbeitet …").font(.jarvis(12, .medium)).foregroundStyle(Farbe.text2)
                }
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(Capsule().fill(Farbe.glas(0.06)))
            }
            RundKnopf(symbol: "waveform", hilfe: "Live-Gespräch (⌘L)", farbe: Farbe.gold) { steuerung.live() }
            RundKnopf(symbol: "trash", hilfe: "Gespräch löschen") {
                GespraechsSpeicher.shared.loeschen(gespraech); steuerung.gespraech = nil; steuerung.bereich = .start
            }
        }
        .padding(.horizontal, 28).padding(.top, 30).padding(.bottom, 12)
        .background(LinearGradient(colors: [Farbe.grund.opacity(0.7), .clear], startPoint: .top, endPoint: .bottom))
    }

    private var leer: some View {
        VStack(spacing: 14) {
            JarvisKugel(pegel: 0, zustand: .ruhe, groesse: 120)
            Text("Was kann ich für dich tun?").font(.jarvis(22, .semibold))
            Text("Ich habe dasselbe Gedächtnis wie auf deinem Handy und kann hier am Mac alles für dich erledigen.")
                .font(.jarvis(14)).foregroundStyle(Farbe.text2)
        }
        .frame(maxWidth: .infinity).padding(.top, 80)
    }
}

extension NSImage {
    /// Verkleinert auf höchstens 1600 px und speichert als JPEG – hält Anhänge im Verlauf klein.
    func jpgDaten() -> Data? {
        let gross = max(size.width, size.height)
        let f = gross > 1600 ? 1600 / gross : 1
        let ziel = NSSize(width: size.width * f, height: size.height * f)
        let neu = NSImage(size: ziel)
        neu.lockFocus(); draw(in: NSRect(origin: .zero, size: ziel)); neu.unlockFocus()
        guard let tiff = neu.tiffRepresentation, let rep = NSBitmapImageRep(data: tiff) else { return nil }
        return rep.representation(using: .jpeg, properties: [.compressionFactor: 0.7])
    }
    func pngDaten() -> Data? {
        guard let tiff = tiffRepresentation, let rep = NSBitmapImageRep(data: tiff) else { return nil }
        return rep.representation(using: .png, properties: [:])
    }
}

struct PegelBalken: View {
    var pegel: Float
    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30)) { tl in
            let t = tl.date.timeIntervalSinceReferenceDate
            HStack(spacing: 3) {
                ForEach(0..<5, id: \.self) { i in
                    let h = 4 + CGFloat(pegel) * 18 * CGFloat(0.6 + 0.4 * sin(t * 9 + Double(i)))
                    Capsule().fill(Farbe.gruen).frame(width: 3, height: max(4, h))
                }
            }
            .frame(height: 22)
        }
    }
}

struct NachrichtAnsicht: View {
    let n: Nachricht
    let gespraech: Gespraech
    @State private var schwebt = false
    @State private var schritteOffen = false
    @State private var bewertung: Bool?

    private var frageVorher: String {
        guard let i = gespraech.nachrichten.firstIndex(where: { $0.id == n.id }) else { return "" }
        return gespraech.nachrichten[..<i].last(where: { $0.rolle == .frank })?.text ?? ""
    }

    var body: some View {
        if n.rolle == .frank { frank } else { jarvis }
    }

    private var frank: some View {
        HStack {
            Spacer(minLength: 120)
            VStack(alignment: .trailing, spacing: 6) {
                if let b = n.bild, let bild = NSImage(data: b) {
                    Image(nsImage: bild).resizable().scaledToFit().frame(maxHeight: 220).clipShape(RoundedRectangle(cornerRadius: 14))
                }
                HStack(alignment: .top, spacing: 6) {
                    if n.gesprochen { Image(systemName: "waveform").font(.system(size: 11)).foregroundStyle(Color.black.opacity(0.5)) }
                    Text(n.text).font(.jarvis(15)).foregroundStyle(Color.black.opacity(0.88)).textSelection(.enabled)
                }
                .padding(.horizontal, 16).padding(.vertical, 11)
                .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(Farbe.akzent))
            }
        }
    }

    private var jarvis: some View {
        HStack(alignment: .top, spacing: 14) {
            JarvisKugel(pegel: 0, zustand: n.laeuft ? .denkt : .ruhe, groesse: 30)
                .frame(width: 30, height: 30)
            VStack(alignment: .leading, spacing: 10) {
                if !n.schritte.isEmpty { schritteAnsicht }
                if n.text.isEmpty && n.laeuft {
                    DenkPunkte()
                } else {
                    MarkdownText(text: n.text, groesse: 15)
                }
                if !n.laeuft && !n.text.isEmpty {
                    HStack(spacing: 6) {
                        KleinKnopf(symbol: "doc.on.doc", hilfe: "Kopieren") {
                            NSPasteboard.general.clearContents(); NSPasteboard.general.setString(n.text, forType: .string)
                        }
                        KleinKnopf(symbol: "speaker.wave.2", hilfe: "Vorlesen") { Task { await Stimme.shared.sprich(n.text) } }
                        KleinKnopf(symbol: bewertung == true ? "hand.thumbsup.fill" : "hand.thumbsup", hilfe: "Gefällt mir – Jarvis lernt daraus") {
                            guard bewertung != true else { return }   // je Antwort nur einmal lernen
                            bewertung = true; StilLernen.bewerte(antwort: n.text, frage: frageVorher, gut: true)
                        }
                        KleinKnopf(symbol: bewertung == false ? "hand.thumbsdown.fill" : "hand.thumbsdown", hilfe: "Gefällt mir nicht – Jarvis lernt daraus") {
                            guard bewertung != false else { return }
                            bewertung = false; StilLernen.bewerte(antwort: n.text, frage: frageVorher, gut: false)
                        }
                        TeilenKnopf(text: n.text).frame(width: 26, height: 24)
                        KleinKnopf(symbol: "arrow.turn.up.right", hilfe: "In neuem Gespräch fortsetzen") {
                            AppSteuerung.shared.neuesGespraech()
                            AppSteuerung.shared.entwurf = "Zur Fortsetzung, deine letzte Antwort war:\n\n\(n.text.prefix(3000))\n\nWeiter: "
                        }
                        KleinKnopf(symbol: "arrow.clockwise", hilfe: "Erneut fragen") {
                            if let frage = gespraech.nachrichten.last(where: { $0.rolle == .frank })?.text {
                                AppSteuerung.shared.gespraech = gespraech; AppSteuerung.shared.sende(frage)
                            }
                        }
                        Text(n.zeit.formatted(date: .omitted, time: .shortened)).font(.jarvis(10)).foregroundStyle(Farbe.text3).padding(.leading, 4)
                    }
                    .opacity(schwebt ? 1 : 0.0)
                }
            }
            Spacer(minLength: 40)
        }
        .onHover { schwebt = $0 }
    }

    private var schritteAnsicht: some View {
        VStack(alignment: .leading, spacing: 6) {
            Button { withAnimation(.spring(duration: 0.3)) { schritteOffen.toggle() } } label: {
                HStack(spacing: 6) {
                    Image(systemName: "bolt.fill").font(.system(size: 10)).foregroundStyle(Farbe.gold)
                    Text("\(n.schritte.count) \(n.schritte.count == 1 ? "Schritt" : "Schritte")").font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text2)
                    if let letzter = n.schritte.last, !schritteOffen {
                        Text("· \(Self.anzeigename(letzter.werkzeug))").font(.jarvis(12)).foregroundStyle(Farbe.text3).lineLimit(1)
                    }
                    Image(systemName: schritteOffen ? "chevron.up" : "chevron.down").font(.system(size: 9, weight: .bold)).foregroundStyle(Farbe.text3)
                }
            }
            .buttonStyle(.plain)
            if schritteOffen {
                ForEach(n.schritte) { s in
                    HStack(spacing: 8) {
                        Image(systemName: s.amHandy ? "iphone" : "macbook").font(.system(size: 10)).foregroundStyle(s.amHandy ? Farbe.gold : Farbe.haupt).frame(width: 14)
                        Group {
                            if let ok = s.ok {
                                Image(systemName: ok ? "checkmark.circle.fill" : "exclamationmark.triangle.fill").foregroundStyle(ok ? Farbe.gruen : Farbe.warnung)
                            } else { ProgressView().controlSize(.mini) }
                        }
                        .font(.system(size: 11)).frame(width: 14)
                        Text(Self.anzeigename(s.werkzeug)).font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text2)
                        Text(s.kurz).font(.system(size: 11, design: .monospaced)).foregroundStyle(Farbe.text3).lineLimit(1)
                    }
                }
            }
        }
        .padding(.horizontal, 12).padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 12).fill(Farbe.glas(0.04)))
    }

    static func anzeigename(_ w: String) -> String {
        w.replacingOccurrences(of: "_", with: " ").replacingOccurrences(of: "ae", with: "ä").replacingOccurrences(of: "oe", with: "ö")
            .replacingOccurrences(of: "ue", with: "ü").capitalized(with: Locale(identifier: "de_DE"))
    }
}

struct KleinKnopf: View {
    let symbol: String
    let hilfe: String
    let aktion: () -> Void
    var body: some View {
        Button(action: aktion) {
            Image(systemName: symbol).font(.system(size: 11, weight: .medium)).foregroundStyle(Farbe.text2)
                .frame(width: 26, height: 24).background(RoundedRectangle(cornerRadius: 7).fill(Farbe.glas(0.06)))
        }
        .buttonStyle(.plain).help(hilfe)
    }
}

struct DenkPunkte: View {
    var body: some View {
        TimelineView(.animation) { tl in
            let t = tl.date.timeIntervalSinceReferenceDate
            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { i in
                    Circle().fill(Farbe.akzent).frame(width: 7, height: 7)
                        .opacity(0.3 + 0.7 * max(0, sin(t * 4 - Double(i) * 0.7)))
                        .scaleEffect(0.8 + 0.3 * max(0, sin(t * 4 - Double(i) * 0.7)))
                }
            }
            .padding(.vertical, 6)
        }
    }
}
