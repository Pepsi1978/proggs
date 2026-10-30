import AppKit
import Carbon.HIToolbox
import SwiftUI

enum Bereich: String, CaseIterable, Identifiable {
    case start, zentrale, gespraech, live, programmierung, ablage, gedaechtnis, aktivitaet, einstellungen
    var id: String { rawValue }
    var titel: String {
        switch self {
        case .start: return "Start"
        case .zentrale: return "Zentrale"
        case .gespraech: return "Gespräch"
        case .live: return "Live"
        case .programmierung: return "Programmierung"
        case .ablage: return "Ablage"
        case .gedaechtnis: return "Gedächtnis"
        case .aktivitaet: return "Aktivität"
        case .einstellungen: return "Einstellungen"
        }
    }
    var symbol: String {
        switch self {
        case .start: return "house.fill"
        case .zentrale: return "sparkles"
        case .gespraech: return "bubble.left.and.text.bubble.right.fill"
        case .live: return "waveform"
        case .programmierung: return "chevron.left.forwardslash.chevron.right"
        case .ablage: return "tray.full.fill"
        case .gedaechtnis: return "brain.head.profile.fill"
        case .aktivitaet: return "list.bullet.rectangle.portrait.fill"
        case .einstellungen: return "gearshape.fill"
        }
    }
}

/// Zentrale Steuerung: Navigation, Weckwort, globales Tastenkürzel, Sprach-Einblendung.
@MainActor @Observable
final class AppSteuerung {
    static let shared = AppSteuerung()
    var bereich: Bereich = .start { didSet { if bereich != oldValue { log("oberflaeche", "Bereich geöffnet: \(bereich.titel)", wo: "Seitenleiste") } } }
    var gespraech: Gespraech?
    var entwurf = ""
    /// Der Prompt, der gerade im Vollbild bearbeitet wird; promptStand zählt hoch, wenn die Liste neu geladen werden muss.
    var promptBearbeiten: PromptEintrag?
    var promptStand = 0
    var handyVerbunden = false
    var handyVersion = ""
    private var hotKeyRefs: [EventHotKeyRef] = []
    private var hotKeyHandler = false
    private var hud: SprachHUD?

    func hochfahren() {
        DesignStand.shared.starten()
        VorderApp.beobachten()
        TresorSperre.shared.beobachten()
        Weckwort.shared.beiErkennung = { [weak self] in self?.weckwortErkannt() }
        if Einstellungen.shared.weckwortAn { Weckwort.shared.starten() }
        if Einstellungen.shared.globalesKuerzel { kuerzelRegistrieren() }
        Task {
            while true {
                let liste = await McpKlient.shared.werkzeuge()
                handyVerbunden = !liste.isEmpty
                handyVersion = await McpKlient.shared.serverVersion
                try? await Task.sleep(for: .seconds(handyVerbunden ? 300 : 30))
            }
        }
        // Guthaben wie am Handy alle 15 Minuten, nicht während Live; jede Minute nur, solange das Handy noch gar nicht geantwortet hat.
        Task {
            while true {
                if !LiveGespraech.shared.aktiv { await Guthaben.shared.laden() }
                try? await Task.sleep(for: .seconds(handyVerbunden ? 900 : 60))
            }
        }
    }

    var projekt: String?   // offenes Projekt in der Leiste

    /// imProjekt = aus der Projektleiste („Neues Gespräch im Projekt“); alles andere landet unter „Gespräche“.
    func neuesGespraech(imProjekt: Bool = false) {
        let g = GespraechsSpeicher.shared.neu()
        g.projekt = imProjekt ? projekt : nil
        gespraech = g
        bereich = .gespraech
    }

    func oeffne(_ g: Gespraech) {
        gespraech = g
        bereich = .gespraech
    }

    /// Text an Jarvis schicken (aus Zentrale, Menüleiste oder Chat).
    func sende(_ text: String, gesprochen: Bool = false, bild: Data? = nil) {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty || bild != nil else { return }
        log("gespraech", "Frage an Jarvis: \(t.prefix(500))", wo: gesprochen ? "Sprache" : "Texteingabe")
        let g = gespraech ?? GespraechsSpeicher.shared.neu()
        gespraech = g
        bereich = .gespraech
        Task {
            let antwort = await JarvisAgent.shared.frage(t.isEmpty ? "Schau dir das Bild an." : t, in: g, gesprochen: gesprochen, bild: bild)
            if gesprochen && Einstellungen.shared.vorlesen { await Stimme.shared.sprich(antwort) }
        }
    }

    func sprechen() {
        let g = gespraech ?? GespraechsSpeicher.shared.neu()
        gespraech = g
        if SprachSchleife.shared.zustand != .aus { SprachSchleife.shared.beenden(); return }
        SprachSchleife.shared.starten(in: g)
    }

    func live() {
        if LiveGespraech.shared.aktiv { LiveGespraech.shared.beenden(); return }
        let g = gespraech ?? GespraechsSpeicher.shared.neu()
        gespraech = g
        bereich = .live
        LiveGespraech.shared.starten(in: g)
    }

    func fensterZeigen() {
        NSApp.activate()
        for w in NSApp.windows where w.canBecomeMain { w.makeKeyAndOrderFront(nil) }
    }

    /// „Hey Jarvis“: neues Sprachgespräch, schwebende Einblendung über allen Programmen.
    func weckwortErkannt() {
        guard !LiveGespraech.shared.aktiv, SprachSchleife.shared.zustand == .aus else { return }
        log("sprache", "Weckwort erkannt")
        let g = GespraechsSpeicher.shared.neu()
        g.titel = "Sprachgespräch"
        gespraech = g
        if hud == nil { hud = SprachHUD() }
        hud?.zeigen()
        SprachSchleife.shared.starten(in: g)
    }

    // MARK: Globales Tastenkürzel ⌥⌘J = sprechen, ⌥⌘K = Fenster

    func kuerzelRegistrieren() {
        guard hotKeyRefs.isEmpty else { return }
        if !hotKeyHandler {
            hotKeyHandler = true
            var typ = EventTypeSpec(eventClass: OSType(kEventClassKeyboard), eventKind: UInt32(kEventHotKeyPressed))
            InstallEventHandler(GetApplicationEventTarget(), { _, ereignis, _ in
                var id = EventHotKeyID()
                GetEventParameter(ereignis, EventParamName(kEventParamDirectObject), EventParamType(typeEventHotKeyID), nil,
                                  MemoryLayout<EventHotKeyID>.size, nil, &id)
                MainActor.assumeIsolated {
                    if id.id == 1 { AppSteuerung.shared.kuerzelSprechen() } else { AppSteuerung.shared.fensterZeigen() }
                }
                return noErr
            }, 1, &typ, nil, nil)
        }
        for (taste, nr) in [(kVK_ANSI_J, UInt32(1)), (kVK_ANSI_K, UInt32(2))] {
            var ref: EventHotKeyRef?
            RegisterEventHotKey(UInt32(taste), UInt32(cmdKey | optionKey), EventHotKeyID(signature: OSType(0x4A525653), id: nr),
                                GetApplicationEventTarget(), 0, &ref)
            if let ref { hotKeyRefs.append(ref) }
        }
    }

    func kuerzelAbmelden() {
        hotKeyRefs.forEach { UnregisterEventHotKey($0) }
        hotKeyRefs = []
    }

    func kuerzelSprechen() {
        if SprachSchleife.shared.zustand != .aus { SprachSchleife.shared.beenden(); hud?.verbergen(); return }
        if hud == nil { hud = SprachHUD() }
        let g = GespraechsSpeicher.shared.neu()
        g.titel = "Sprachgespräch"
        gespraech = g
        hud?.zeigen()
        SprachSchleife.shared.starten(in: g)
    }
}

// MARK: - Schwebende Sprach-Einblendung (über allen Programmen, ohne Fokus zu stehlen)

final class HUDPanel: NSPanel {
    override var canBecomeKey: Bool { false }
    override var canBecomeMain: Bool { false }
}

@MainActor
final class SprachHUD {
    private let panel: HUDPanel
    private var beobachter: Task<Void, Never>?

    init() {
        panel = HUDPanel(contentRect: NSRect(x: 0, y: 0, width: 420, height: 168),
                         styleMask: [.nonactivatingPanel, .borderless], backing: .buffered, defer: false)
        panel.isFloatingPanel = true
        panel.level = .statusBar
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .stationary]
        panel.backgroundColor = .clear
        panel.isOpaque = false
        panel.hasShadow = true
        panel.hidesOnDeactivate = false
        panel.contentView = NSHostingView(rootView: SprachHUDAnsicht())
    }

    func zeigen() {
        guard let schirm = NSScreen.main?.visibleFrame else { return }
        panel.setFrameOrigin(NSPoint(x: schirm.midX - 210, y: schirm.maxY - 190))
        panel.alphaValue = 0
        panel.orderFrontRegardless()
        NSAnimationContext.runAnimationGroup { $0.duration = 0.25; panel.animator().alphaValue = 1 }
        beobachter?.cancel()
        beobachter = Task { [weak self] in
            // Verbergen, sobald die Sprachschleife endet.
            try? await Task.sleep(for: .seconds(1.5))
            while !Task.isCancelled {
                if SprachSchleife.shared.zustand == .aus { self?.verbergen(); return }
                try? await Task.sleep(for: .milliseconds(400))
            }
        }
    }

    func verbergen() {
        beobachter?.cancel()
        NSAnimationContext.runAnimationGroup({ $0.duration = 0.3; panel.animator().alphaValue = 0 }) { [panel] in
            Task { @MainActor in panel.orderOut(nil) }
        }
    }
}

struct SprachHUDAnsicht: View {
    @State private var schleife = SprachSchleife.shared
    @State private var diktat = Diktat.shared
    @State private var stimme = Stimme.shared

    private var kugelZustand: JarvisKugel.KugelZustand {
        switch schleife.zustand {
        case .aus: return .ruhe
        case .hoert: return .hoert
        case .denkt: return .denkt
        case .spricht: return .spricht
        }
    }

    private var zeile: String {
        switch schleife.zustand {
        case .hoert: return diktat.vorschau.isEmpty ? "Ich höre zu …" : diktat.vorschau
        case .denkt: return "Einen Moment …"
        case .spricht:
            return schleife.gespraech?.nachrichten.last(where: { $0.rolle == .jarvis })?.text ?? ""
        case .aus: return ""
        }
    }

    var body: some View {
        HStack(spacing: 16) {
            JarvisKugel(pegel: schleife.zustand == .spricht ? stimme.pegel : diktat.pegel, zustand: kugelZustand, groesse: 96)
            VStack(alignment: .leading, spacing: 6) {
                Text("JARVIS").font(.jarvis(11, .heavy)).tracking(3).foregroundStyle(Farbe.haupt)
                Text(zeile).font(.jarvis(15, .medium)).foregroundStyle(Farbe.text).lineLimit(4)
                    .animation(.easeOut(duration: 0.2), value: zeile)
            }
            Spacer(minLength: 0)
        }
        .padding(20)
        .frame(width: 420, height: 168)
        .background(RoundedRectangle(cornerRadius: 28, style: .continuous).fill(Farbe.grund.opacity(0.82)))
        .glassEffect(.regular, in: .rect(cornerRadius: 28))
        .overlay(RoundedRectangle(cornerRadius: 28, style: .continuous).strokeBorder(Farbe.glas(0.14)))
        .onTapGesture { SprachSchleife.shared.beenden() }
    }
}
