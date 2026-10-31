import AppKit
import Carbon.HIToolbox
import SwiftUI

enum Bereich: String, CaseIterable, Identifiable {
    case start, zentrale, ablage, gespraech, live, einstellungen
    var id: String { rawValue }
    var titel: String {
        switch self {
        case .start: return "Start"
        case .zentrale: return "Zentrale"
        case .ablage: return "Ablage"
        case .gespraech: return "Gespräch"
        case .live: return "Live"
        case .einstellungen: return "Einstellungen"
        }
    }
    var symbol: String {
        switch self {
        case .start: return "house.fill"
        case .zentrale: return "sparkles"
        case .ablage: return "tray.full.fill"
        case .gespraech: return "bubble.left.and.text.bubble.right.fill"
        case .live: return "waveform"
        case .einstellungen: return "gearshape.fill"
        }
    }
}

/// Zentrale Steuerung: Navigation, Weckwort, globales Tastenkürzel, Sprachpille.
@MainActor @Observable
final class AppSteuerung {
    static let shared = AppSteuerung()
    var bereich: Bereich = .start { didSet { if bereich != oldValue { log("oberflaeche", "Bereich geöffnet: \(bereich.titel)", wo: "Seitenleiste") } } }
    var gespraech: Gespraech?
    /// Der Text der Sprechblase, wie am Handy einer für alle Ansichten: Er überlebt jeden Ansichtswechsel, und die rechte Leiste kann ihn bearbeiten.
    var eingabeText = "" { didSet { if !eingabeText.isEmpty { geleerterText = nil } } }
    /// Eingeklappt zeigt die Blase nur ihre untere Zeile; mit Text bleibt sie immer offen.
    var blaseKompakt = true
    /// Zählt hoch, wenn die Blase den Fokus bekommen soll.
    var blaseFokus = 0
    /// Was der Papierkorb der Blase zuletzt geleert hat, für „Rückgängig“.
    var geleerterText: String?
    /// Vorformulierter Text für die Blase (Prompt senden, in neuem Gespräch fortsetzen).
    var entwurf: String {
        get { eingabeText }
        set { eingabeText = newValue; if !newValue.isEmpty { blaseKompakt = false; blaseFokus += 1 } }
    }
    /// Der Prompt, der gerade im Vollbild bearbeitet wird.
    var promptBearbeiten: PromptEintrag?
    /// Der noch nicht gespeicherte neue Prompt: Er überlebt das Vollbild und den Wechsel des Bereichs.
    var promptEntwurf = ""
    /// Der eben gesendete Prompt, solange die Frage offen ist, ob er aus der Ablage weg soll.
    var promptLoeschenFrage: PromptEintrag?
    var promptLoeschenFehler: String?
    var handyVerbunden = false
    var handyVersion = ""
    private var hotKeyRefs: [EventHotKeyRef] = []
    private var hotKeyHandler = false
    /// Jarvis ist die vordere App. Danach richten sich die Animationen: Im Hintergrund rechnen sie nicht.
    private(set) var appVorn = true
    private let pille = SprachPille()
    /// Laufende Fragen aus der Eingabeblase, zum Abbrechen.
    private var fragen: [Int: (gespraech: UUID, aufgabe: Task<Void, Never>)] = [:]
    private var fragenNummer = 0

    func hochfahren() {
        DesignStand.shared.starten()
        fensterBeobachten()
        VorderApp.beobachten()
        TresorSperre.shared.beobachten()
        Weckwort.shared.beiErkennung = { [weak self] in self?.weckwortErkannt() }
        if Einstellungen.shared.weckwortAn { Weckwort.shared.starten() }
        if Einstellungen.shared.globalesKuerzel { kuerzelRegistrieren() }
        AgentenKanal.shared.starten()
        Task {
            while true {
                // Frisch im Hintergrund: Die Liste ist warm und der Stand stimmt, wenn Frank fragt.
                let liste = await McpKlient.shared.werkzeuge(frisch: true)
                handyVerbunden = !liste.isEmpty
                handyVersion = await McpKlient.shared.serverVersion
                let unsicher = await McpKlient.shared.unsicher
                try? await Task.sleep(for: .seconds(handyVerbunden && !unsicher ? 300 : 30))
            }
        }
        // Guthaben wie am Handy alle 15 Minuten, nicht während Live und nicht, solange weder Handy noch eigene Console-Anmeldung es liefern können.
        Task { if await ConsoleSitzung.altbestandUebernehmen() { await Guthaben.shared.laden(frisch: true) } }
        Task {
            while true {
                guard handyVerbunden || ConsoleSitzung.angemeldet, !LiveGespraech.shared.aktiv else { try? await Task.sleep(for: .seconds(30)); continue }
                await Guthaben.shared.laden()
                try? await Task.sleep(for: .seconds(900))
            }
        }
    }

    var projekt: String?   // offenes Projekt in der Leiste

    /// imProjekt = aus der Projektleiste („Neues Gespräch im Projekt“); alles andere beginnt auf dem Startbildschirm.
    func neuesGespraech(imProjekt: Bool = false) {
        guard imProjekt else { startOeffnen(); return }
        // Ein offener Entwurf bleibt: Mehrmaliges Drücken legt nichts an.
        let g = entwurfOderNeu()
        g.projekt = projekt
        gespraech = g
        bereich = .gespraech
    }

    /// „Start“: neues Gespräch mit offener Blase. Angelegt wird es erst mit der ersten Nachricht, leere Gespräche gibt es also nicht.
    func startOeffnen() {
        Task { await McpKlient.shared.kontextVorladen() }
        if SprachSchleife.shared.zustand != .aus { SprachSchleife.shared.beenden() }
        gespraech = nil
        bereich = .start
        blaseKompakt = false
        blaseFokus += 1
    }

    /// „Neue Session“ der rechten Leiste, wie am Handy: leeres Gespräch, die Blase klappt ohne Text ein.
    func neueSession() {
        startOeffnen()
        blaseKompakt = true
    }

    // MARK: Programmierung und Nachtrag (Bedienlogik wie ProgrammierSchweber und AppViewModel am Handy)

    var programmiererAktiv: Bool { ProgrammierVorspann.hatAuftrag(eingabeText) }
    var nachtragAktiv: Bool { ProgrammierVorspann.hatNachtrag(eingabeText) }
    var nachtragMoeglich: Bool { gespraech?.istProgrammierung == true }

    /// Die Blase steht nur auf Start und im Gespräch: Von anderswo führen die Knöpfe dorthin.
    private func zurBlase() {
        if bereich != .start && bereich != .gespraech { bereich = gespraech != nil ? .gespraech : .start }
        blaseKompakt = false
        blaseFokus += 1
    }

    /// Setzt den Vorspann vor den Text; ein zweiter Klick nimmt genau ihn wieder heraus. Ein „Nachtrag:“ vorn weicht ihm.
    func programmiererUmschalten() {
        if programmiererAktiv { eingabeText = ProgrammierVorspann.ohneAuftrag(eingabeText); return }
        eingabeText = ProgrammierVorspann.mitAuftrag(ProgrammierVorspann.ohneNachtrag(eingabeText))
        zurBlase()
    }

    func nachtragUmschalten() {
        guard nachtragMoeglich else { return }
        if nachtragAktiv { eingabeText = ProgrammierVorspann.ohneNachtrag(eingabeText); return }
        eingabeText = ProgrammierVorspann.mitNachtrag(ProgrammierVorspann.ohneAuftrag(eingabeText))
        zurBlase()
    }

    /// „Nachtrag:“ vorn: Der Text geht ohne Modell in genau die Sitzung dieses Gesprächs. Geht er nicht raus, kommt er in die Blase zurück.
    func nachtragSenden(_ g: Gespraech, text: String) {
        let roh = eingabeText
        eingabeText = ""
        blaseKompakt = true
        g.nachrichten.append(Nachricht(rolle: .frank, text: "\(ProgrammierVorspann.nachtrag) \(text)"))
        g.speichern()
        Task {
            let antwort: String
            var fehler = false
            if let id = g.auftragId {
                let r = await McpKlient.shared.rufe("programmier_nachtrag", ["auftrag_id": id, "text": text, "franks_worte": "Nachtrag in die gleiche Session \(id)"])
                antwort = r.text; fehler = r.fehler
            } else {
                antwort = "Zu diesem Gespräch ist keine Programmier-Sitzung bekannt, der Nachtrag wurde nicht gesendet."
                fehler = true
            }
            log("gespraech", "Nachtrag per Knopf", wo: "Rechte Leiste", fehler: fehler ? antwort : nil)
            g.nachrichten.append(Nachricht(rolle: .jarvis, text: McpKlient.ohneKontextHinweis(antwort)))
            if (g.verlauf.last?["role"] as? String) == "assistant" {
                g.verlauf.append(["role": "user", "content": [["type": "text", "text": "\(ProgrammierVorspann.nachtrag) \(text)"]]])
                g.verlauf.append(["role": "assistant", "content": [["type": "text", "text": antwort]]])
            }
            g.speichern()
            if fehler && eingabeText.isEmpty { eingabeText = roh; blaseKompakt = false }
        }
    }

    private func entwurfOderNeu() -> Gespraech {
        Task { await McpKlient.shared.kontextVorladen() }
        if let g = gespraech, g.nachrichten.isEmpty, !g.arbeitet { return g }
        return GespraechsSpeicher.shared.neu()
    }

    /// Ja auf die Frage nach dem Senden: löscht nur den Eintrag der Ablage, der Text in der Eingabe bleibt.
    func gesendetenPromptLoeschen(_ p: PromptEintrag) {
        Task {
            // Über die feste Kennung und nur bei unverändertem Text: Eine inzwischen geänderte Fassung bleibt stehen.
            let r = await AblageAbgleich.shared.werkzeug("prompt_speichern", ["aktion": "loeschen", "nummer": p.nummer, "erstellt": p.erstellt, "text": p.text])
            if let r, r.fehler { promptLoeschenFehler = r.text; return }
            log("ablage", "Gesendeten Prompt gelöscht", wo: "Ablage/Prompts")
        }
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
        fragenNummer += 1
        let nr = fragenNummer
        fragen[nr] = (g.id, Task {
            let antwort = await JarvisAgent.shared.frage(t.isEmpty ? "Schau dir das Bild an." : t, in: g, gesprochen: gesprochen, bild: bild)
            fragen[nr] = nil
            if gesprochen && Einstellungen.shared.vorlesen && !Task.isCancelled { await Stimme.shared.sprich(antwort) }
        })
    }

    /// Stopp-Knopf wie am Handy: solange Jarvis in diesem Gespräch an einer Frage aus der Blase arbeitet oder das Sprachgespräch antwortet oder spricht.
    func abbrechbar(_ g: Gespraech) -> Bool {
        let s = SprachSchleife.shared
        if s.gespraech === g && (s.zustand == .denkt || s.zustand == .spricht) { return true }
        return g.arbeitet && fragen.values.contains { $0.gespraech == g.id }
    }

    func abbrechen(_ g: Gespraech) {
        gespraechBeendet(g)
        log("gespraech", "Antwort abgebrochen", wo: "Eingabeblase")
    }

    /// Laufende Fragen und das Sprachgespräch dieses Gesprächs enden, etwa beim Löschen.
    func gespraechBeendet(_ g: Gespraech) {
        for f in fragen.values where f.gespraech == g.id { f.aufgabe.cancel() }
        if SprachSchleife.shared.gespraech === g && SprachSchleife.shared.zustand != .aus { SprachSchleife.shared.beenden() }
    }

    func sprechen() { kuerzelSprechen() }

    func live() {
        if LiveGespraech.shared.aktiv { LiveGespraech.shared.beenden(); return }
        let g = gespraech ?? GespraechsSpeicher.shared.neu()
        gespraech = g
        bereich = .live
        LiveGespraech.shared.starten(in: g)
    }

    /// Knopf „Live“ in der Blase: öffnet nur den Live-Bildschirm, gestartet wird dort. Vom Start aus ein neues Gespräch, aus einem Gespräch dieses.
    func liveOeffnen(in g: Gespraech?) {
        if !LiveGespraech.shared.aktiv { gespraech = g }
        bereich = .live
    }

    func fensterZeigen() {
        NSApp.activate()
        let fenster = NSApp.windows.filter(\.canBecomeMain)
        for w in fenster {
            if w.isMiniaturized { w.deminiaturize(nil) }
            w.makeKeyAndOrderFront(nil)
        }
        // Geschlossen gibt es kein Fenster mehr zum Zeigen: Das erneute Öffnen der App legt es wieder an.
        Task {
            try? await Task.sleep(for: .milliseconds(200))
            if !NSApp.windows.contains(where: { $0.canBecomeMain && $0.isVisible }) { NSWorkspace.shared.open(Bundle.main.bundleURL) }
        }
    }

    /// Ist Jarvis wirklich vorn? Dann ist die Blase unten die Anzeige, sonst die Pille.
    var fensterVorn: Bool {
        NSApp.isActive && NSApp.windows.contains { $0.canBecomeMain && $0.isVisible && !$0.isMiniaturized }
    }

    /// Bildschirm gesperrt oder im Ruhezustand: NSApp.isActive kann dann weiter true sein.
    static var bildschirmAus: Bool {
        let sitzung = CGSessionCopyCurrentDictionary() as? [String: Any]
        return sitzung?["CGSSessionScreenIsLocked"] as? Bool == true || CGDisplayIsAsleep(CGMainDisplayID()) != 0
    }

    /// „Hey Jarvis“: wie am Handy. Ist Jarvis vorn, geht das offene Gespräch weiter, sonst beginnt ein neues im Hintergrund mit der Pille.
    func weckwortErkannt() {
        guard !LiveGespraech.shared.aktiv, SprachSchleife.shared.zustand == .aus else { return }
        log("sprache", "Weckwort erkannt")
        sprachgespraechStarten()
    }

    private func sprachgespraechStarten() {
        Task { await McpKlient.shared.kontextVorladen() }
        guard fensterVorn else {
            // Im Hintergrund bleibt die Ansicht, wie sie ist: Ein Fehlalarm darf nichts Offenes verdrängen. Klick auf die Pille oder die Rückkehr zu Jarvis zeigt das Gespräch.
            SprachSchleife.shared.starten(in: GespraechsSpeicher.shared.neu())
            return
        }
        let g = bereich == .gespraech ? (gespraech ?? GespraechsSpeicher.shared.neu()) : entwurfOderNeu()
        gespraech = g
        bereich = .gespraech
        SprachSchleife.shared.starten(in: g, inApp: true)
    }

    // MARK: Sprachpille

    /// Sichtbar, solange Live läuft, und im Gesprächsmodus, wenn Jarvis nicht vorn ist.
    func pilleAbgleichen() {
        pille.setze(sichtbar: LiveGespraech.shared.aktiv || (SprachSchleife.shared.zustand != .aus && !fensterVorn))
    }

    /// Damit die Pille nicht auf dem Bildschirmfoto landet, das sie gerade angestoßen hat.
    func pilleFuerAufnahme(ausblenden: Bool) async {
        await pille.fuerAufnahme(ausblenden: ausblenden)
    }

    /// Klick auf die Pille: öffnet Jarvis mit dem laufenden Gespräch.
    func pilleOeffnen() {
        if LiveGespraech.shared.aktiv { bereich = .live }
        else if let g = SprachSchleife.shared.gespraech { gespraech = g; bereich = .gespraech }
        fensterZeigen()
    }

    private func fensterBeobachten() {
        appVorn = NSApp.isActive
        let nc = NotificationCenter.default
        for name in [NSApplication.didBecomeActiveNotification, NSApplication.didResignActiveNotification, NSWindow.didMiniaturizeNotification,
                     NSWindow.didDeminiaturizeNotification, NSWindow.willCloseNotification] {
            nc.addObserver(forName: name, object: nil, queue: .main) { _ in
                // Einen Schritt später: Beim Schließen zählt das Fenster in diesem Moment noch als sichtbar.
                DispatchQueue.main.async {
                    MainActor.assumeIsolated {
                        let s = AppSteuerung.shared
                        if s.appVorn != NSApp.isActive { s.appVorn = NSApp.isActive }
                        s.pilleAbgleichen()
                        // Zurück in Jarvis, während das Gespräch im Hintergrund schon Inhalt hat: Es erscheint.
                        if NSApp.isActive, let g = SprachSchleife.shared.gespraech, SprachSchleife.shared.zustand != .aus, !g.nachrichten.isEmpty, s.gespraech !== g {
                            s.gespraech = g
                            s.bereich = .gespraech
                        }
                    }
                }
            }
        }
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
        if SprachSchleife.shared.zustand != .aus { SprachSchleife.shared.beenden(); return }
        guard !LiveGespraech.shared.aktiv else { return }
        sprachgespraechStarten()
    }
}
