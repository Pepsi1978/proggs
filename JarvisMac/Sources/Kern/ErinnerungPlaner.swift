import AppKit
import Foundation
import Observation

/// Lässt fällige Erinnerungen erscheinen, wie ErinnerungPlaner am Handy, im Hauptfaden.
/// Wo sie erscheint, sagt der Server (Anwesenheit): Ist ein anderes Gerät dran, bleibt der Mac still und fragt nach einer Minute wieder nach.
/// Was angezeigt wird, ergibt sich jedes Mal neu aus Erinnerungen und Quittungen: Eine Bestätigung von anderswo, Löschen, Erledigen oder ein verschobener Termin beendet Banner, Ton und Stimme sofort.
@MainActor @Observable
final class ErinnerungPlaner {
    static let shared = ErinnerungPlaner()

    /// Gezeigte, noch offene Auslösungen; daraus entsteht das Banner in der App.
    private(set) var anzeige: [Faellig] = []
    /// Bestätigt, aber noch nicht vom Server angenommen; gilt hier schon.
    private(set) var lokaleQuittungen: [String: ErinnerungQuittung] = [:]

    @ObservationIgnored private var gezeigt = Set<String>()
    @ObservationIgnored private var gestartet = false
    @ObservationIgnored private var laeuft = false
    @ObservationIgnored private var nochmal = false
    @ObservationIgnored private var sendetQuittungen = false
    @ObservationIgnored private var gefragt = Set<String>()
    @ObservationIgnored private var naechsteNachfrage = Date.distantPast
    @ObservationIgnored private var weckerTon: NSSound?
    @ObservationIgnored private var weckerUhr: Task<Void, Never>?
    @ObservationIgnored private var warteschlange: [(quittung: String, text: String)] = []
    @ObservationIgnored private var sprechAufgabe: Task<Void, Never>?
    @ObservationIgnored private var sprichtGerade: String?
    @ObservationIgnored private let netz: URLSession = {
        let c = URLSessionConfiguration.ephemeral
        c.timeoutIntervalForRequest = 4
        c.timeoutIntervalForResource = 4
        return URLSession(configuration: c)
    }()

    private static let ordner: URL = {
        let u = Pfade.unterstuetzung.appendingPathComponent("Erinnerung", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true)
        return u
    }()
    private static var gezeigtDatei: URL { ordner.appendingPathComponent("gezeigt.json") }
    private static var quittungenDatei: URL { ordner.appendingPathComponent("quittungen.json") }

    private init() {
        if let d = try? Data(contentsOf: Self.gezeigtDatei), let l = JSON.wert(d) as? [String] { gezeigt = Set(l) }
        if let d = try? Data(contentsOf: Self.quittungenDatei), let o = JSON.objekt(d) {
            for (k, v) in o { if let w = v as? JSONObjekt { lokaleQuittungen[k] = ErinnerungQuittung.aus(id: k, w) } }
        }
    }

    private static func jetzt() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    /// Sekunden seit der letzten Eingabe an diesem Mac.
    private static func leerlauf() -> Double {
        guard let alle = CGEventType(rawValue: ~0) else { return .infinity }
        return CGEventSource.secondsSinceLastEventType(.combinedSessionState, eventType: alle)
    }

    /// Vor dem ersten Aufbau der Liste wäre alles „gelöscht“ und die gemerkten Auslösungen gingen verloren.
    private var bereit: Bool {
        let a = AblageAbgleich.shared
        return gestartet && a.bereit && a.aufbauStand > 0
    }

    func starten() {
        guard !gestartet else { return }
        gestartet = true
        Task {
            while true {
                await lauf()
                try? await Task.sleep(for: .seconds(20))
            }
        }
        // Anwesenheit: Solange der Mac benutzt wird, meldet er das einmal je Minute.
        Task {
            while true {
                if Self.leerlauf() < 120 { _ = await ziel(aktiv: true) }
                try? await Task.sleep(for: .seconds(60))
            }
        }
    }

    /// Nach jedem Neuaufbau der Ablage: Verstummen geht sofort, Neues prüft der Lauf.
    func ablageGeaendert() {
        guard bereit else { return }
        abgleichen()
        Task { await lauf() }
    }

    // MARK: Quittungen

    func quittung(_ id: String) -> ErinnerungQuittung? {
        AblageAbgleich.shared.quittungen[id] ?? lokaleQuittungen[id]
    }

    /// Offene, schon fällige Auslösungen, die noch niemand bestätigt hat.
    func faellig(_ jetzt: Int64) -> [Faellig] {
        AblageAbgleich.shared.erinnerungen.flatMap { $0.termine() }
            .filter { $0.zeit <= jetzt && quittung($0.quittung) == nil && $0.meldbar(jetzt) }
            .sorted { $0.zeit < $1.zeit }
    }

    /// Bestätigt genau eine Auslösung, die übrigen bleiben geplant. Gilt sofort, auch ohne Netz.
    func bestaetige(_ f: Faellig) {
        guard quittung(f.quittung) == nil else { return }
        lokaleQuittungen[f.quittung] = ErinnerungQuittung(id: f.quittung, zeit: Self.jetzt(), von: AblageAbgleich.quelle, erinnerung: f.erinnerung.id)
        quittungenSichern()
        log("erinnerung", "Bestätigt: \(f.anzeige)", wo: "Erinnerung")
        abgleichen()
        quittungenNachsenden()
    }

    private func quittungenNachsenden() {
        // Was der Server schon kennt, braucht die eigene Datei nicht mehr.
        let server = AblageAbgleich.shared.quittungen
        let bekannt = lokaleQuittungen.keys.filter { server[$0] != nil }
        if !bekannt.isEmpty {
            for k in bekannt { lokaleQuittungen[k] = nil }
            quittungenSichern()
        }
        guard !sendetQuittungen, !lokaleQuittungen.isEmpty else { return }
        sendetQuittungen = true
        let offen = Array(lokaleQuittungen.values)
        Task {
            defer { sendetQuittungen = false }
            for q in offen {
                do {
                    try await AblageAbgleich.shared.quittungSenden(q)
                    lokaleQuittungen[q.id] = nil
                    quittungenSichern()
                } catch {
                    // Bleibt gemerkt: Der nächste Lauf versucht es wieder. Ohne Netz hätten die übrigen dasselbe Schicksal.
                    log("erinnerung", "Bestätigung noch nicht beim Server", wo: "Erinnerung", fehler: error.localizedDescription)
                    break
                }
            }
        }
    }

    private func quittungenSichern() {
        var o: JSONObjekt = [:]
        for (k, q) in lokaleQuittungen { o[k] = q.alsJson() }
        try? JSON.daten(o).write(to: Self.quittungenDatei, options: .atomic)
    }

    private func gezeigtSichern() {
        try? JSON.daten(Array(gezeigt).sorted()).write(to: Self.gezeigtDatei, options: .atomic)
    }

    // MARK: Prüfen

    private func lauf() async {
        guard !laeuft else { nochmal = true; return }
        laeuft = true
        defer { laeuft = false }
        repeat {
            nochmal = false
            await einLauf()
        } while nochmal
    }

    private func einLauf() async {
        guard bereit else { return }
        abgleichen()
        quittungenNachsenden()
        let neu = faellig(Self.jetzt()).filter { !gezeigt.contains($0.quittung) }
        gefragt = gefragt.intersection(neu.map(\.quittung))
        guard !neu.isEmpty else { return }
        // Noch unbekannte Auslösungen fragen sofort, sonst einmal je Minute: So geht sie nach, wenn Frank wechselt.
        guard neu.contains(where: { !gefragt.contains($0.quittung) }) || Date() >= naechsteNachfrage else { return }
        gefragt.formUnion(neu.map(\.quittung))
        let leer = Self.leerlauf()
        let hier: Bool
        if let z = await ziel(aktiv: leer < 120) {
            hier = z == AblageAbgleich.quelle
        } else {
            // Server nicht erreichbar: nur zeigen, wenn Frank gerade hier sitzt.
            hier = Self.leerlauf() < 300
        }
        guard hier else {
            naechsteNachfrage = Date().addingTimeInterval(60)
            return
        }
        // Während der Nachfrage kann sich alles geändert haben.
        for f in faellig(Self.jetzt()) where !gezeigt.contains(f.quittung) { zeige(f) }
        gezeigtSichern()
        abgleichen()
    }

    private func ziel(aktiv: Bool) async -> String? {
        guard let url = AblageAbgleich.shared.anwesenheitAdresse(aktiv: aktiv) else { return nil }
        guard let (daten, kopf) = try? await netz.data(from: url), (kopf as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        return JSON.objekt(daten)?["ziel"] as? String
    }

    private func zeige(_ f: Faellig) {
        let e = f.erinnerung
        gezeigt.insert(f.quittung)
        log("erinnerung", "Fällig: \(e.zeile) (\(e.form))", wo: "Erinnerung")
        Task { _ = await Shell.mitteilung(e.zeile, e.kurz) }
        vorlesen(f.quittung, e.kurz)
        if e.wecker { AppSteuerung.shared.fensterZeigen() }
    }

    /// Bringt Banner, Ton und Stimme auf den Stand von Erinnerungen und Quittungen.
    private func abgleichen() {
        guard bereit else { return }
        let jetzt = Self.jetzt()
        var gueltig: [String: Faellig] = [:]
        for e in AblageAbgleich.shared.erinnerungen where e.erledigt == 0 {
            for f in e.termine() where f.zeit <= jetzt && quittung(f.quittung) == nil { gueltig[f.quittung] = f }
        }
        let vorher = gezeigt.count
        gezeigt = gezeigt.filter { gueltig[$0] != nil }
        if gezeigt.count != vorher { gezeigtSichern() }

        let neu = gezeigt.compactMap { gueltig[$0] }.sorted { $0.zeit == $1.zeit ? $0.quittung < $1.quittung : $0.zeit < $1.zeit }
        if neu != anzeige { anzeige = neu }

        // Stimme: Was nicht mehr angezeigt wird, schweigt.
        warteschlange.removeAll { !gezeigt.contains($0.quittung) }
        if let s = sprichtGerade, !gezeigt.contains(s) { Stimme.shared.stopp() }

        if neu.contains(where: { $0.erinnerung.wecker }) { klingeln() } else { tonAus() }
    }

    // MARK: Wecker

    /// Läuft, bis Frank ausdrücklich „Aus“ klickt oder eine Quittung von anderswo kommt. Ein Neustart beendet das Klingeln, nicht die Pflicht zum Ausschalten.
    private func klingeln() {
        guard weckerTon == nil else { return }
        let ton = NSSound(named: "Glass")
        ton?.loops = true
        ton?.play()
        weckerTon = ton
        AppSteuerung.shared.fensterZeigen()
        weckerUhr?.cancel()
        weckerUhr = Task {
            while !Task.isCancelled {
                for f in anzeige where f.erinnerung.wecker { vorlesen(f.quittung, f.erinnerung.kurz) }
                try? await Task.sleep(for: .seconds(120))
            }
        }
    }

    private func tonAus() {
        weckerUhr?.cancel(); weckerUhr = nil
        weckerTon?.stop(); weckerTon = nil
    }

    // MARK: Stimme

    private func vorlesen(_ quittung: String, _ text: String) {
        guard sprichtGerade != quittung, !warteschlange.contains(where: { $0.quittung == quittung }) else { return }
        warteschlange.append((quittung, text))
        guard sprechAufgabe == nil else { return }
        sprechAufgabe = Task {
            await sprechSchleife()
            sprechAufgabe = nil
        }
    }

    /// Ein Sprachgespräch, Live, Diktat oder eine laufende Antwort wird nie unterbrochen: Stimme.sprich stoppt sonst, was gerade läuft.
    private var stimmeBelegt: Bool {
        LiveGespraech.shared.aktiv || SprachSchleife.shared.zustand != .aus || Diktat.shared.aktiv || Stimme.shared.spricht
    }

    private func sprechSchleife() async {
        while let naechste = warteschlange.first {
            while stimmeBelegt, warteschlange.first?.quittung == naechste.quittung { try? await Task.sleep(for: .seconds(1)) }
            guard warteschlange.first?.quittung == naechste.quittung else { continue }
            warteschlange.removeFirst()
            sprichtGerade = naechste.quittung
            weckerTon?.pause()
            await Stimme.shared.sprich(naechste.text)
            weckerTon?.resume()
            sprichtGerade = nil
        }
    }
}
