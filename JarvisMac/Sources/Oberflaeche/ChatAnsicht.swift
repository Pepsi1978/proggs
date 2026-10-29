import AppKit
import SwiftUI
import UniformTypeIdentifiers

struct ChatAnsicht: View {
    let gespraech: Gespraech
    @State private var steuerung = AppSteuerung.shared
    @State private var einst = Einstellungen.shared
    @State private var schleife = SprachSchleife.shared
    @State private var diktat = Diktat.shared
    @State private var text = ""
    @State private var anhang: Data?
    @State private var diktiert = false
    @FocusState private var fokus: Bool

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
            eingabe
        }
        .onAppear {
            if !steuerung.entwurf.isEmpty { text = steuerung.entwurf; steuerung.entwurf = "" }
            fokus = true
        }
        .onDrop(of: [.image, .fileURL], isTargeted: nil) { anbieter in
            guard let a = anbieter.first else { return false }
            _ = a.loadDataRepresentation(for: .image) { d, _ in
                if let d, let png = NSImage(data: d)?.pngDaten() { Task { @MainActor in anhang = png } }
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
                .background(Capsule().fill(Color.white.opacity(0.06)))
            }
            RundKnopf(symbol: "waveform", hilfe: "Live-Gespräch (⌘L)", farbe: Farbe.violett) { steuerung.live() }
            RundKnopf(symbol: "trash", hilfe: "Gespräch löschen") {
                GespraechsSpeicher.shared.loeschen(gespraech); steuerung.gespraech = nil; steuerung.bereich = .zentrale
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

    private var eingabe: some View {
        VStack(spacing: 8) {
            if let anhang, let bild = NSImage(data: anhang) {
                HStack {
                    Image(nsImage: bild).resizable().scaledToFit().frame(height: 70).clipShape(RoundedRectangle(cornerRadius: 10))
                    Button { self.anhang = nil } label: { Image(systemName: "xmark.circle.fill") }.buttonStyle(.plain)
                    Spacer()
                }
            }
            if diktat.aktiv && diktiert {
                HStack(spacing: 8) {
                    PegelBalken(pegel: diktat.pegel)
                    Text(diktat.vorschau.isEmpty ? "Ich höre zu …" : diktat.vorschau).font(.jarvis(13)).foregroundStyle(Farbe.text2).lineLimit(2)
                    Spacer()
                }
            }
            HStack(alignment: .bottom, spacing: 10) {
                Menu {
                    Button("Bildschirmfoto anhängen") { Task { anhang = await Self.bildschirmfoto() } }
                    Button("Bild auswählen …") { bildWaehlen() }
                    Divider()
                    Button("An den Programmierer übergeben") { text = "Übergib bitte folgende Aufgabe an den Programmierer: " + text; fokus = true }
                    Button("OpenLauncher-Session hier am Mac") { text = "Starte eine OpenLauncher-Session mit Opus 5.5 für folgende Aufgabe: " + text; fokus = true }
                } label: {
                    Image(systemName: "plus").font(.system(size: 15, weight: .semibold)).frame(width: 34, height: 34)
                        .background(Circle().fill(Color.white.opacity(0.07)))
                }
                .menuStyle(.button).buttonStyle(.plain).menuIndicator(.hidden).fixedSize()

                TextField("Nachricht an Jarvis …  (⏎ senden, ⇧⏎ neue Zeile)", text: $text, axis: .vertical)
                    .textFieldStyle(.plain).font(.jarvis(15)).lineLimit(1...10).focused($fokus)
                    .padding(.vertical, 8)
                    .onKeyPress(.return, phases: .down) { druck in
                        if druck.modifiers.contains(.shift) { return .ignored }
                        senden(); return .handled
                    }

                Menu {
                    ForEach(ModellWahl.alle) { m in
                        Button { einst.modell = m.id } label: { Text(m.name + (m.id == einst.modell ? "  ✓" : "")) }
                    }
                    Divider()
                    ForEach(["low", "medium", "high", "xhigh", "max"], id: \.self) { e in
                        Button { einst.effort = e } label: { Text("Denkstufe \(e)" + (e == einst.effort ? "  ✓" : "")) }
                    }
                } label: {
                    Text(einst.modellWahl.name.replacingOccurrences(of: "Claude ", with: ""))
                        .font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text2)
                        .padding(.horizontal, 10).padding(.vertical, 8)
                        .background(Capsule().fill(Color.white.opacity(0.06)))
                }
                .menuStyle(.button).buttonStyle(.plain).menuIndicator(.hidden).fixedSize()

                RundKnopf(symbol: diktiert && diktat.aktiv ? "stop.fill" : "mic.fill", hilfe: "Diktieren", aktiv: diktiert && diktat.aktiv, farbe: Farbe.gruen) { diktieren() }
                RundKnopf(symbol: "person.wave.2.fill", hilfe: "Sprachgespräch (⌘D)", aktiv: schleife.zustand != .aus, farbe: Farbe.cyan) {
                    steuerung.gespraech = gespraech; steuerung.sprechen()
                }
                RundKnopf(symbol: "arrow.up", hilfe: "Senden", aktiv: !text.isEmpty || anhang != nil, farbe: Farbe.cyan) { senden() }
            }
            .padding(.leading, 10).padding(.trailing, 8).padding(.vertical, 6)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Color.white.opacity(0.05)))
            .glassEffect(.regular, in: .rect(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(Color.white.opacity(fokus ? 0.2 : 0.1)))
        }
        .padding(.horizontal, 40).padding(.bottom, 22).padding(.top, 6)
        .frame(maxWidth: 940)
        .frame(maxWidth: .infinity)
    }

    private func senden() {
        var t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasSuffix(";") { t = String(t.dropLast()).trimmingCharacters(in: .whitespaces) }
        guard !t.isEmpty || anhang != nil else { return }
        let bild = anhang
        text = ""; anhang = nil
        steuerung.gespraech = gespraech
        steuerung.sende(t, bild: bild)
    }

    private func diktieren() {
        if diktat.aktiv { diktat.stoppen(); return }
        diktiert = true
        Task {
            if let erkannt = await diktat.hoereBisStille(maxSekunden: 120), !erkannt.isEmpty {
                let basis = text.trimmingCharacters(in: .whitespaces)
                text = (basis.isEmpty ? "" : basis + " ") + erkannt + " ; "
            }
            diktiert = false
            fokus = true
        }
    }

    private func bildWaehlen() {
        let p = NSOpenPanel()
        p.allowedContentTypes = [.image]
        if p.runModal() == .OK, let u = p.url, let b = NSImage(contentsOf: u) { anhang = b.pngDaten() }
    }

    static func bildschirmfoto() async -> Data? {
        let datei = Pfade.temp.appendingPathComponent("anhang.png")
        try? FileManager.default.removeItem(at: datei)
        _ = await Shell.zsh("screencapture -x -m -t png '\(datei.path)' && sips -Z 1600 '\(datei.path)' >/dev/null", sekunden: 20)
        return try? Data(contentsOf: datei)
    }
}

extension NSImage {
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
                    Image(systemName: "bolt.fill").font(.system(size: 10)).foregroundStyle(Farbe.violett)
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
                        Image(systemName: s.amHandy ? "iphone" : "macbook").font(.system(size: 10)).foregroundStyle(s.amHandy ? Farbe.violett : Farbe.cyan).frame(width: 14)
                        Group {
                            if let ok = s.ok {
                                Image(systemName: ok ? "checkmark.circle.fill" : "exclamationmark.triangle.fill").foregroundStyle(ok ? Farbe.gruen : Farbe.orange)
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
        .background(RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.04)))
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
                .frame(width: 26, height: 24).background(RoundedRectangle(cornerRadius: 7).fill(Color.white.opacity(0.06)))
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
