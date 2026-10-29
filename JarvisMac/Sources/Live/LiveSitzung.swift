import Foundation

/// Rückmeldungen einer Live-Sitzung an das Live-Gespräch. Kommen aus dem Socket-Faden.
protocol LiveSitzungsDelegate: AnyObject {
    func liveBereit()
    func liveNutzerText(_ text: String)
    func liveJarvisText(_ text: String)
    func liveWerkzeug(kennung: String, auftrag: String, hinweis: String)
    func liveFehler(_ text: String)
    func liveEnde(_ grund: String?)
}

/// Gemeinsame Schnittstelle der Anbieter (GPT-Live, Realtime, Gemini Live).
class LiveSitzung: @unchecked Sendable {
    let ton: LiveTon
    weak var delegate: LiveSitzungsDelegate?
    var bereit = false
    var istGemini: Bool { false }

    init(ton: LiveTon) { self.ton = ton }

    func starten() {}
    func beenden() {}
    func mikro(_ pcm: Data) {}
    /// Jarvis' Antwort auf einen Auftrag zurück ans Sprachmodell. false = nicht angekommen.
    @discardableResult func ergebnis(kennung: String, text: String) -> Bool { false }
    /// Spätes Ergebnis nach einem Zwischenstand jetzt sprechen lassen.
    @discardableResult func nachreichen(kennung: String, text: String) -> Bool { false }
    /// Regelmäßig (alle 100 ms) aus dem Live-Gespräch: Zeitgeber der Sitzung.
    func takt() {}
    /// Frank tippt zum Unterbrechen.
    func angetippt() { ton.leeren() }
}

/// WebSocket mit Dauerverbindung (Ping alle 20 s), eigener Faden mit hoher Priorität (Live vor Hintergrundarbeit).
final class LiveSocket: NSObject, URLSessionWebSocketDelegate, @unchecked Sendable {
    private var session: URLSession!
    private var task: URLSessionWebSocketTask!
    private let beiOffen: () -> Void
    private let beiText: (String) -> Void
    private let beiZu: (Int, String) -> Void
    private var zu = false
    private let lock = NSLock()
    private var ping: DispatchSourceTimer?
    private static let warteschlange: OperationQueue = {
        let q = OperationQueue(); q.maxConcurrentOperationCount = 1; q.qualityOfService = .userInteractive; q.name = "jarvis.live.socket"; return q
    }()

    init(anfrage: URLRequest, beiOffen: @escaping () -> Void, beiText: @escaping (String) -> Void, beiZu: @escaping (Int, String) -> Void) {
        self.beiOffen = beiOffen; self.beiText = beiText; self.beiZu = beiZu
        super.init()
        let konf = URLSessionConfiguration.default
        konf.timeoutIntervalForRequest = 30
        konf.timeoutIntervalForResource = 24 * 3600
        session = URLSession(configuration: konf, delegate: self, delegateQueue: Self.warteschlange)
        task = session.webSocketTask(with: anfrage)
        task.maximumMessageSize = 32 * 1024 * 1024
        task.resume()
        empfangen()
    }

    private func empfangen() {
        task.receive { [weak self] r in
            guard let self else { return }
            switch r {
            case .success(let m):
                switch m {
                case .string(let s): self.beiText(s)
                case .data(let d): self.beiText(String(decoding: d, as: UTF8.self))
                @unknown default: break
                }
                self.empfangen()
            case .failure(let e):
                let code = (self.task.response as? HTTPURLResponse)?.statusCode ?? -1
                let grund = self.task.closeReason.map { String(decoding: $0, as: UTF8.self) } ?? e.localizedDescription
                self.schliessen(code: self.task.closeCode.rawValue != 0 ? self.task.closeCode.rawValue : code, grund: grund)
            }
        }
    }

    private func schliessen(code: Int, grund: String) {
        lock.lock(); let war = zu; zu = true; lock.unlock()
        guard !war else { return }
        ping?.cancel(); ping = nil
        beiZu(code, grund)
        session.invalidateAndCancel()
    }

    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        let t = DispatchSource.makeTimerSource(queue: .global(qos: .userInitiated))
        t.schedule(deadline: .now() + 20, repeating: 20)
        t.setEventHandler { [weak self] in self?.task.sendPing { _ in } }
        t.resume()
        ping = t
        beiOffen()
    }

    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        schliessen(code: closeCode.rawValue, grund: reason.map { String(decoding: $0, as: UTF8.self) } ?? "")
    }

    var offen: Bool { lock.lock(); defer { lock.unlock() }; return !zu }

    @discardableResult func sende(_ objekt: JSONObjekt) -> Bool {
        guard offen, let d = try? JSONSerialization.data(withJSONObject: objekt, options: [.withoutEscapingSlashes]) else { return false }
        task.send(.string(String(decoding: d, as: UTF8.self))) { _ in }
        return true
    }

    /// Von uns beendet: kein Fehler mehr an den Aufrufer.
    func trennen() {
        lock.lock(); let war = zu; zu = true; lock.unlock()
        guard !war else { return }
        ping?.cancel(); ping = nil
        task.cancel(with: .normalClosure, reason: nil)
        session.invalidateAndCancel()
    }
}
