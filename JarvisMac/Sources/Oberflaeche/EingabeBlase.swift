import AppKit
import SwiftUI

/// Die Sprechblase unten, auf dem Startbildschirm und im Gespräch gleich: Plus, Text, Modell mit Guthaben und Kosten, Diktat, Sprechen, Senden.
/// Ohne [gespraech] (Startbildschirm) beginnt das Absenden ein neues Gespräch.
struct EingabeBlase: View {
    let gespraech: Gespraech?
    @Binding var anhang: Data?
    @State private var steuerung = AppSteuerung.shared
    @State private var schleife = SprachSchleife.shared
    @State private var diktat = Diktat.shared
    @State private var live = LiveGespraech.shared
    @State private var text = ""
    @State private var diktiert = false
    @FocusState private var fokus: Bool

    var body: some View {
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
                    Button("Bildschirmfoto anhängen") { Task { anhang = await ChatAnsicht.bildschirmfoto() } }
                    Button("Bild auswählen …") { bildWaehlen() }
                    Divider()
                    Button("An den Programmierer übergeben") { text = "Übergib bitte folgende Aufgabe an den Programmierer: " + text; fokus = true }
                    Button("OpenLauncher-Session hier am Mac") { text = "Starte eine OpenLauncher-Session mit Opus 5.5 für folgende Aufgabe: " + text; fokus = true }
                } label: {
                    Image(systemName: "plus").font(.system(size: 15, weight: .semibold)).frame(width: 34, height: 34)
                        .background(Circle().fill(Farbe.glas(0.07)))
                }
                .menuStyle(.button).buttonStyle(.plain).menuIndicator(.hidden).fixedSize()

                TextField("Frag Jarvis oder sag, was er am Mac tun soll …  (⏎ senden, ⇧⏎ neue Zeile)", text: $text, axis: .vertical)
                    .textFieldStyle(.plain).font(.jarvis(15)).lineLimit(1...10).focused($fokus)
                    .padding(.vertical, 8)
                    .onKeyPress(.return, phases: .down) { druck in
                        if druck.modifiers.contains(.shift) { return .ignored }
                        senden(); return .handled
                    }

                ModellKapsel()

                RundKnopf(symbol: diktiert && diktat.aktiv ? "stop.fill" : "mic.fill", hilfe: "Diktieren", aktiv: diktiert && diktat.aktiv, farbe: Farbe.gruen) { diktieren() }
                if gespraech == nil {
                    Button { steuerung.liveOeffnen() } label: {
                        HStack(spacing: 5) {
                            Image(systemName: "waveform").font(.system(size: 12, weight: .semibold))
                            Text("Live").font(.jarvis(12, .semibold))
                        }
                        .foregroundStyle(live.aktiv ? Color.black : Farbe.text)
                        .padding(.horizontal, 12).frame(height: 34)
                        .background(Capsule().fill(live.aktiv ? AnyShapeStyle(Farbe.gold) : AnyShapeStyle(Farbe.glas(0.07))))
                        .overlay(Capsule().strokeBorder(Farbe.glas(0.10)))
                        .contentShape(Capsule())
                    }
                    .buttonStyle(.plain).fixedSize()
                    .help("Live-Bildschirm öffnen")
                }
                RundKnopf(symbol: "person.wave.2.fill", hilfe: "Sprachgespräch (⌘D)", aktiv: schleife.zustand != .aus, farbe: Farbe.haupt) {
                    if let gespraech { steuerung.gespraech = gespraech; steuerung.sprechen() } else { steuerung.kuerzelSprechen() }
                }
                RundKnopf(symbol: "arrow.up", hilfe: "Senden", aktiv: !text.isEmpty || anhang != nil, farbe: Farbe.haupt) { senden() }
            }
            .padding(.leading, 10).padding(.trailing, 8).padding(.vertical, 6)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Farbe.glas(0.05)))
            .glassEffect(.regular, in: .rect(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(Farbe.glas(fokus ? 0.2 : 0.1)))
        }
        .padding(.horizontal, 40).padding(.bottom, 22).padding(.top, 6)
        .frame(maxWidth: 940)
        .frame(maxWidth: .infinity)
        .onAppear {
            if gespraech != nil, !steuerung.entwurf.isEmpty { text = steuerung.entwurf; steuerung.entwurf = "" }
            fokus = true
        }
        .onChange(of: steuerung.entwurf) { _, neu in if gespraech != nil, !neu.isEmpty { text = neu; steuerung.entwurf = "" } }
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
            if let erkannt = await diktat.hoereBisStille(maxSekunden: 120) { text = AufgabenText.anhaengen(text, erkannt) }
            diktiert = false
            fokus = true
        }
    }

    private func bildWaehlen() {
        let p = NSOpenPanel()
        p.allowedContentTypes = [.image]
        if p.runModal() == .OK, let u = p.url, let b = NSImage(contentsOf: u) { anhang = b.jpgDaten() }
    }
}
