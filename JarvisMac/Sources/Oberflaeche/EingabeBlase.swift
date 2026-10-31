import AppKit
import SwiftUI

/// Die Sprechblase unten, auf dem Startbildschirm und im Gespräch gleich und wie am Handy: eingeklappt nur die untere Zeile mit Plus, Modell, Mikrofon und genau einem Knopf
/// (Abbrechen, solange Jarvis arbeitet, der Pfeil, sobald Text oder ein Anhang da ist, sonst Live). Der freie Platz oder das Mikrofon öffnet sie nach oben, ein weiterer Druck
/// verkleinert sie wieder; mit Text bleibt sie offen und zeigt den Papierkorb. Ohne [gespraech] (Startbildschirm) beginnt das Absenden ein neues Gespräch.
struct EingabeBlase: View {
    let gespraech: Gespraech?
    @Binding var anhang: Data?
    @State private var steuerung = AppSteuerung.shared
    @State private var schleife = SprachSchleife.shared
    @State private var live = LiveGespraech.shared
    @State private var diktat = Diktat.shared
    @State private var diktiert = false
    @State private var diktatFehler = ""
    @State private var nachDiktatSenden = false
    @FocusState private var fokus: Bool

    private var hatText: Bool { !steuerung.eingabeText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    private var kompakt: Bool { steuerung.blaseKompakt && steuerung.eingabeText.isEmpty }

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
            VStack(spacing: 0) {
                // Eingeklappt bleibt das Feld unsichtbar im Fokus: Wer einfach lostippt, öffnet die Blase damit.
                HStack(alignment: .top, spacing: 6) {
                    eingabeFeld
                    leerenKnopf
                }
                .padding(.leading, 8)
                .frame(height: kompakt ? 0 : nil)
                .clipped()
                .opacity(kompakt ? 0 : 1)
                .allowsHitTesting(!kompakt)
                HStack(spacing: 10) {
                    plusMenue
                    // Wie am Handy schaltet der freie Platz zwischen Plus und Modell um.
                    Color.clear.frame(maxWidth: .infinity).frame(height: 34)
                        .contentShape(Rectangle())
                        .onTapGesture { umschalten() }
                        .help(kompakt ? "Eingabe öffnen" : "Eingabe verkleinern")
                    ModellKapsel()
                    knoepfe
                }
            }
            .padding(.leading, 10).padding(.trailing, 8).padding(.vertical, 6)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(Farbe.glas(0.05)))
            .glassEffect(.regular, in: .rect(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(Farbe.glas(fokus && !kompakt ? 0.2 : 0.1)))
            .animation(.easeOut(duration: 0.18), value: kompakt)
        }
        .padding(.horizontal, 40).padding(.bottom, 22).padding(.top, 6)
        .frame(maxWidth: 940)
        .frame(maxWidth: .infinity)
        .onAppear {
            fokus = true
            // Beim Fensterstart nimmt sonst das erste Textfeld der Leiste den Fokus.
            Task { try? await Task.sleep(for: .milliseconds(150)); fokus = true }
        }
        .onChange(of: steuerung.blaseFokus) { _, _ in fokus = true }
    }

    private func umschalten() {
        if kompakt { steuerung.blaseKompakt = false; fokus = true }
        else if !hatText { steuerung.blaseKompakt = true; steuerung.eingabeText = "" }
        else { fokus = true }
    }

    /// Fester Platz wie am Handy, damit die Blase beim ersten Zeichen nicht springt: mit Text der Papierkorb, gleich danach „Rückgängig“.
    private var leerenKnopf: some View {
        ZStack {
            if hatText && !diktat.aktiv {
                Button {
                    steuerung.geleerterText = steuerung.eingabeText
                    steuerung.eingabeText = ""
                    fokus = true
                } label: { Image(systemName: "trash").font(.system(size: 13, weight: .medium)).foregroundStyle(Farbe.text3).frame(width: 30, height: 30).contentShape(Rectangle()) }
                .buttonStyle(.plain).help("Text löschen")
            } else if steuerung.eingabeText.isEmpty, let alt = steuerung.geleerterText {
                Button { steuerung.eingabeText = alt; fokus = true } label: {
                    Image(systemName: "arrow.uturn.backward").font(.system(size: 13, weight: .medium)).foregroundStyle(Farbe.haupt).frame(width: 30, height: 30).contentShape(Rectangle())
                }
                .buttonStyle(.plain).help("Gelöschten Text zurückholen")
            }
        }
        .frame(width: 30, height: 30)
        .padding(.top, 4)
    }

    private var plusMenue: some View {
        Menu {
            Button("Bildschirmfoto anhängen") { Task { anhang = await Self.bildschirmfoto() } }
            Button("Bild auswählen …") { bildWaehlen() }
            Divider()
            Button("An den Programmierer übergeben") { steuerung.programmiererUmschalten() }
            if steuerung.nachtragMoeglich { Button("Nachtrag") { steuerung.nachtragUmschalten() } }
        } label: {
            Image(systemName: "plus").font(.system(size: 15, weight: .semibold)).frame(width: 34, height: 34)
                .background(Circle().fill(Farbe.glas(0.07)))
        }
        .menuStyle(.button).buttonStyle(.plain).menuIndicator(.hidden).fixedSize()
    }

    private var eingabeFeld: some View {
        TextField("Frag Jarvis oder sag, was er am Mac tun soll …  (⏎ senden, ⇧⏎ neue Zeile)", text: $steuerung.eingabeText, axis: .vertical)
            .textFieldStyle(.plain).font(.jarvis(15)).lineLimit(1...10).focused($fokus)
            .padding(.vertical, 8)
            .onKeyPress(.return, phases: .down) { druck in
                if druck.modifiers.contains(.shift) { return .ignored }
                senden(); return .handled
            }
    }

    @ViewBuilder private var knoepfe: some View {
        DiktatKnopf(text: $steuerung.eingabeText, vorschau: false, meldeLaeuft: { laeuft in
            diktiert = laeuft
            if laeuft { steuerung.blaseKompakt = false }
            else {
                fokus = true
                // Kam nichts an, verkleinert das Stoppen die Blase wieder.
                if !hatText { steuerung.blaseKompakt = true; steuerung.eingabeText = "" }
            }
            if !laeuft && nachDiktatSenden { nachDiktatSenden = false; senden() }
        }, meldeFehler: { diktatFehler = $0 })
        if let gespraech, steuerung.abbrechbar(gespraech) {
            RundKnopf(symbol: "stop.fill", hilfe: "Abbrechen", aktiv: true, farbe: Farbe.haupt) { steuerung.abbrechen(gespraech) }
        } else if hatText || anhang != nil {
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

    private func senden() {
        // Läuft das Mikrofon noch, gehört das Gesagte zur Nachricht: erst fertig mitschreiben, dann senden.
        if diktiert {
            nachDiktatSenden = true
            diktat.stoppen()
            return
        }
        var t = steuerung.eingabeText.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasSuffix(";") { t = String(t.dropLast()).trimmingCharacters(in: .whitespaces) }
        guard !t.isEmpty || anhang != nil else { return }
        diktatFehler = ""
        if anhang == nil, t.hasPrefix(ProgrammierVorspann.nachtrag), let gespraech, gespraech.istProgrammierung {
            let rest = String(t.dropFirst(ProgrammierVorspann.nachtrag.count)).trimmingCharacters(in: .whitespacesAndNewlines)
            guard !rest.isEmpty else { diktatFehler = "Bitte den Nachtrag hinter dem Doppelpunkt ergänzen."; return }
            steuerung.nachtragSenden(gespraech, text: rest)
            return
        }
        let neuerAuftrag = ProgrammierVorspann.hatAuftrag(t)
        if neuerAuftrag, !ProgrammierVorspann.ohneAuftrag(t).contains(where: { $0.isLetter || $0.isNumber }) {
            diktatFehler = "Bitte die Aufgabe hinter dem Doppelpunkt ergänzen."
            return
        }
        let bild = anhang
        steuerung.eingabeText = ""; anhang = nil
        steuerung.blaseKompakt = true
        // Wie am Handy: Ein neuer Auftrag aus einem Programmiergespräch bekommt ein eigenes Gespräch, sonst vermischt er sich mit der laufenden Sitzung.
        steuerung.gespraech = neuerAuftrag && gespraech?.istProgrammierung == true ? nil : gespraech
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
