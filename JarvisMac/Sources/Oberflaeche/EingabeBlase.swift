import AppKit
import SwiftUI

/// Die Sprechblase unten, auf dem Startbildschirm und im Gespräch gleich: Plus, Text, Modell mit Guthaben und Kosten, Mikrofon und rechts wie am Handy
/// genau ein Knopf: Abbrechen, solange Jarvis arbeitet, der Pfeil, sobald Text oder ein Anhang da ist, sonst Live.
/// Ohne [gespraech] (Startbildschirm) beginnt das Absenden ein neues Gespräch.
struct EingabeBlase: View {
    let gespraech: Gespraech?
    @Binding var anhang: Data?
    /// Text über den Knöpfen statt daneben, für schmale Bereiche wie die Zentrale.
    var zweizeilig = false
    @State private var steuerung = AppSteuerung.shared
    @State private var schleife = SprachSchleife.shared
    @State private var live = LiveGespraech.shared
    @State private var diktat = Diktat.shared
    @State private var text = ""
    @State private var diktiert = false
    @State private var diktatFehler = ""
    @State private var nachDiktatSenden = false
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
            if diktat.aktiv && (diktiert || schleife.zustand == .hoert) {
                HStack(spacing: 8) {
                    DiktatStatus(groesse: 13, zeilen: 2)
                    Spacer()
                }
            }
            if !diktatFehler.isEmpty {
                Text(diktatFehler).font(.jarvis(12)).foregroundStyle(Farbe.warnung).frame(maxWidth: .infinity, alignment: .leading)
            }
            Group {
                if zweizeilig {
                    VStack(spacing: 6) {
                        eingabeFeld.padding(.horizontal, 8)
                        HStack(spacing: 10) {
                            plusMenue
                            ModellKapsel()
                            Spacer(minLength: 0)
                            knoepfe
                        }
                    }
                } else {
                    HStack(alignment: .bottom, spacing: 10) {
                        plusMenue
                        eingabeFeld
                        ModellKapsel()
                        knoepfe
                    }
                }
            }
            .padding(.leading, 10).padding(.trailing, 8).padding(.vertical, 6)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Farbe.glas(0.05)))
            .glassEffect(.regular, in: .rect(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(Farbe.glas(fokus ? 0.2 : 0.1)))
        }
        .padding(.horizontal, zweizeilig ? 0 : 40).padding(.bottom, zweizeilig ? 0 : 22).padding(.top, zweizeilig ? 0 : 6)
        .frame(maxWidth: zweizeilig ? .infinity : 940)
        .frame(maxWidth: .infinity)
        .onAppear {
            if gespraech != nil, !steuerung.entwurf.isEmpty { text = steuerung.entwurf; steuerung.entwurf = "" }
            restUebernehmen()
            fokus = true
        }
        .onChange(of: steuerung.entwurf) { _, neu in if gespraech != nil, !neu.isEmpty { text = neu; steuerung.entwurf = "" } }
        .onChange(of: steuerung.blasenRest) { _, _ in restUebernehmen() }
        // Getippter Text geht nicht verloren, wenn die Ansicht wechselt, etwa durch das Weckwort: Die nächste Blase übernimmt ihn.
        .onDisappear { if !text.isEmpty { steuerung.blasenRest = AufgabenText.verbinde(steuerung.blasenRest, text) } }
    }

    private var plusMenue: some View {
        Menu {
            Button("Bildschirmfoto anhängen") { Task { anhang = await Self.bildschirmfoto() } }
            Button("Bild auswählen …") { bildWaehlen() }
            Divider()
            Button("An den Programmierer übergeben") { text = "Übergib bitte folgende Aufgabe an den Programmierer: " + text; fokus = true }
            Button("OpenLauncher-Session hier am Mac") { text = "Starte eine OpenLauncher-Session mit Opus 5.5 für folgende Aufgabe: " + text; fokus = true }
        } label: {
            Image(systemName: "plus").font(.system(size: 15, weight: .semibold)).frame(width: 34, height: 34)
                .background(Circle().fill(Farbe.glas(0.07)))
        }
        .menuStyle(.button).buttonStyle(.plain).menuIndicator(.hidden).fixedSize()
    }

    private var eingabeFeld: some View {
        TextField(zweizeilig ? "Frag Jarvis oder sag, was er am Mac tun soll …" : "Frag Jarvis oder sag, was er am Mac tun soll …  (⏎ senden, ⇧⏎ neue Zeile)", text: $text, axis: .vertical)
            .textFieldStyle(.plain).font(.jarvis(15)).lineLimit(1...10).focused($fokus)
            .padding(.vertical, 8)
            .onKeyPress(.return, phases: .down) { druck in
                if druck.modifiers.contains(.shift) { return .ignored }
                senden(); return .handled
            }
    }

    @ViewBuilder private var knoepfe: some View {
        DiktatKnopf(text: $text, vorschau: false, meldeLaeuft: { laeuft in
            diktiert = laeuft
            if !laeuft { fokus = true }
            if !laeuft && nachDiktatSenden { nachDiktatSenden = false; senden() }
        }, meldeFehler: { diktatFehler = $0 })
        if let gespraech, steuerung.abbrechbar(gespraech) {
            RundKnopf(symbol: "stop.fill", hilfe: "Abbrechen", aktiv: true, farbe: Farbe.haupt) { steuerung.abbrechen(gespraech) }
        } else if !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || anhang != nil {
            RundKnopf(symbol: "arrow.up", hilfe: "Senden", aktiv: true, farbe: Farbe.haupt) { senden() }
        } else {
            Button { steuerung.liveOeffnen(in: gespraech) } label: {
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
            .help("Live-Bildschirm öffnen (⌘L startet direkt)")
        }
    }

    private func restUebernehmen() {
        guard !steuerung.blasenRest.isEmpty else { return }
        text = AufgabenText.verbinde(text, steuerung.blasenRest)
        steuerung.blasenRest = ""
    }

    private func senden() {
        // Läuft das Mikrofon noch, gehört das Gesagte zur Nachricht: erst fertig mitschreiben, dann senden.
        if diktiert {
            nachDiktatSenden = true
            diktat.stoppen()
            return
        }
        var t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasSuffix(";") { t = String(t.dropLast()).trimmingCharacters(in: .whitespaces) }
        guard !t.isEmpty || anhang != nil else { return }
        let bild = anhang
        text = ""; anhang = nil
        steuerung.gespraech = gespraech
        steuerung.sende(t, bild: bild)
    }

    private func bildWaehlen() {
        let p = NSOpenPanel()
        p.allowedContentTypes = [.image]
        if p.runModal() == .OK, let u = p.url, let b = NSImage(contentsOf: u) { anhang = b.jpgDaten() }
    }

    private static func bildschirmfoto() async -> Data? {
        let datei = Pfade.temp.appendingPathComponent("anhang.jpg")
        try? FileManager.default.removeItem(at: datei)
        _ = await Shell.zsh("screencapture -x -m -t jpg '\(datei.path)' && sips -Z 1440 -s formatOptions 60 '\(datei.path)' >/dev/null", sekunden: 20)
        return try? Data(contentsOf: datei)
    }
}
