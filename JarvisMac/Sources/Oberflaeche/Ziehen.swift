import AppKit
import SwiftUI
import UniformTypeIdentifiers

/// Drag & Drop zum Umsortieren, wie das Modul M1.2 am Handy: Karte an beliebiger Stelle greifen und ziehen; die anderen rücken live zur Seite,
/// Loslassen speichert die Reihenfolge. Geht für senkrechte Stapel und Gitter (der Platz unter dem Zeiger zählt). Ein Klick ohne Bewegung bleibt ein Klick.
/// Getauscht wird erst, wenn der Zeiger die Mitte der Nachbarkarte in Zugrichtung überschreitet: Sonst läge er nach dem Tausch einer kleinen Karte
/// mit einer großen noch in der großen und tauschte sie beim nächsten Bewegen zurück (Flackern). In einer Zeile zählt die Breite.
@MainActor @Observable
final class ZiehZustand {
    var gezogen: String?
    @ObservationIgnored var vorher: [String] = []
    @ObservationIgnored var rahmen: [String: CGRect] = [:]
    @ObservationIgnored var gespeichert: ([String]) -> Void = { _ in }
    @ObservationIgnored var reihe: Binding<[String]>?
    @ObservationIgnored private var waechter: Task<Void, Never>?
    let raum = UUID().uuidString

    func beginne(_ id: String, _ reihe: Binding<[String]>) {
        if gezogen != nil { beende() }
        gezogen = id
        self.reihe = reihe
        vorher = reihe.wrappedValue
        // macOS meldet das Ende eines Ziehens außerhalb der Karten nicht: Sobald keine Maustaste mehr gedrückt ist, ist es vorbei.
        waechter?.cancel()
        waechter = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(250))
                if NSEvent.pressedMouseButtons == 0 { self?.beende(); return }
            }
        }
    }

    func beende() {
        waechter?.cancel(); waechter = nil
        guard gezogen != nil else { return }
        gezogen = nil
        if let neu = reihe?.wrappedValue, neu != vorher { gespeichert(neu) }
        reihe = nil
    }
}

private struct ZiehDelegate: DropDelegate {
    let id: String
    let reihe: Binding<[String]>
    let zustand: ZiehZustand

    func validateDrop(info: DropInfo) -> Bool { zustand.gezogen != nil }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        MainActor.assumeIsolated { tauschen(info.location) }
        return DropProposal(operation: .move)
    }

    func dropEntered(info: DropInfo) { MainActor.assumeIsolated { tauschen(info.location) } }

    func performDrop(info: DropInfo) -> Bool {
        MainActor.assumeIsolated { zustand.beende() }
        return true
    }

    @MainActor private func tauschen(_ punkt: CGPoint) {
        guard let g = zustand.gezogen, g != id, var r = Optional(reihe.wrappedValue),
              let von = r.firstIndex(of: g), let nach = r.firstIndex(of: id) else { return }
        if let zr = zustand.rahmen[id], let gr = zustand.rahmen[g] {
            let gleicheZeile = abs(zr.minY - gr.minY) < min(zr.height, gr.height) / 2
            let mitte = gleicheZeile ? zr.width / 2 : zr.height / 2
            let lage = gleicheZeile ? punkt.x : punkt.y
            if nach > von ? lage < mitte : lage > mitte { return }
        }
        r.move(fromOffsets: IndexSet(integer: von), toOffset: nach > von ? nach + 1 : nach)
        withAnimation(.spring(duration: 0.25)) { reihe.wrappedValue = r }
    }
}

extension View {
    /// Macht eine Karte ziehbar; [reihe] ist die Reihenfolge aller Karten des Stapels, [zustand] gehört dem Stapel (je Stapel einer).
    /// [art] trennt ineinanderliegende Stapel (Kacheln in einer Karte): Ein innerer Stapel mit eigener Art fängt das Ziehen der äußeren Karten nicht ab.
    func ziehbar(_ id: String, reihe: Binding<[String]>, zustand: ZiehZustand, art: UTType = .text) -> some View {
        self
            .onGeometryChange(for: CGRect.self) { $0.frame(in: .named(zustand.raum)) } action: { zustand.rahmen[id] = $0 }
            .opacity(zustand.gezogen == id ? 0.55 : 1)
            .onDrag {
                zustand.beginne(id, reihe)
                if art == .url, let u = URL(string: "jarvis-ziehen:" + (id.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? "")) {
                    return NSItemProvider(object: u as NSURL)
                }
                return NSItemProvider(object: id as NSString)
            }
            .onDrop(of: [art], delegate: ZiehDelegate(id: id, reihe: reihe, zustand: zustand))
    }

    /// Der Stapel, in dem die Karten liegen: Koordinatenraum für die Mitte und Abschluss eines Loslassens in einer Lücke.
    func ziehStapel(_ zustand: ZiehZustand, art: UTType = .text, gespeichert: @escaping ([String]) -> Void) -> some View {
        zustand.gespeichert = gespeichert
        return self
            .coordinateSpace(.named(zustand.raum))
            .onDrop(of: [art], isTargeted: nil) { _ in
                let lief = zustand.gezogen != nil
                zustand.beende()
                return lief
            }
    }
}
