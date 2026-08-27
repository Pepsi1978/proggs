import Foundation
import Network

// MARK: - AutoEnterStatusServer
// Portierung von TerminalVoiceOverlay-Windows/Services/AutoEnterStatusServer.cs
//
// Minimal-HTTP-Server auf 127.0.0.1:5724 fuer Stream-Deck-XL-Polling.
// Routen (vollstaendig wie Windows):
//   GET  /autoenter/status   -> {"on":true|false}
//   POST /autoenter/toggle   -> {"on":<neuer Stand>}
//   GET  /recording/status   -> {"busy":true|false}
//   POST /deployment/prepare -> {"ready":true|false}
//   POST /deployment/release -> {"ready":true}
//   GET  /autoenter/events   -> text/event-stream, Push bei jeder Zustandsaenderung
//   POST /log                -> haengt den Rumpf an TVO-hotkey.log an (204)
//
// Bewusst KEIN Framework wie Vapor — wir wollen 0 externe Dependencies
// und KEINE zusaetzlichen Permissions ueber das Network-Entitlement hinaus.

final class AutoEnterStatusServer {

    static let port: UInt16 = 5724

    /// Wird vom AppDelegate gesetzt — liefert den aktuellen Auto-Enter-Status.
    var statusProvider: (() -> Bool)?

    /// Wird vom AppDelegate gesetzt — togglet den Status und liefert
    /// den NEUEN Wert zurueck.
    var toggleHandler: (() -> Bool)?
    var busyProvider: (() -> Bool)?
    var deploymentPrepareHandler: (() -> Bool)?
    var deploymentReleaseHandler: (() -> Void)?

    private var listener: NWListener?
    private let queue = DispatchQueue(label: "tvo.autoenter.server")

    /// Offene SSE-Verbindungen (GET /autoenter/events). Windows haelt dafuer
    /// `_sseClients` mit `_sseLock`; hier uebernimmt die serielle `queue` die
    /// Sequenzialisierung — gleichzeitige Writes auf dieselbe Verbindung wuerden
    /// den Frame-Aufbau ("data: ...\n\n") zerreissen.
    private var sseClients: [NWConnection] = []

    /// Wie oft der Bind schon fehlgeschlagen ist. Begrenzt die Wiederholungen,
    /// damit ein dauerhaft fremdbelegter Port nicht endlos Versuche erzeugt.
    private var startAttempts = 0
    private static let maxStartAttempts = 10

    func start() {
        do {
            let parameters = NWParameters.tcp
            parameters.allowLocalEndpointReuse = true
            // Nur localhost — kein Lauschen auf externen Interfaces.
            parameters.requiredLocalEndpoint = NWEndpoint.hostPort(
                host: NWEndpoint.Host("127.0.0.1"),
                port: NWEndpoint.Port(rawValue: Self.port)!
            )
            let listener = try NWListener(using: parameters)
            self.listener = listener

            listener.newConnectionHandler = { [weak self] conn in
                self?.handle(connection: conn)
            }

            listener.stateUpdateHandler = { [weak self] state in
                switch state {
                case .ready:
                    tvoDebug("[AutoEnterHTTP] listening on 127.0.0.1:\(Self.port)")
                    // Erfolgreich gebunden -> Zaehler zuruecksetzen, damit ein
                    // spaeterer Ausfall wieder die vollen Versuche bekommt.
                    self?.startAttempts = 0
                case .failed(let err):
                    // Selbstheilung statt stillem Aufgeben: Ist der Port beim
                    // Start noch von einer Vorgaenger-Instanz (oder der
                    // Schwester-App) belegt, war der Status-Server bisher fuer
                    // die GANZE Sitzung tot — und damit auch der Aufnahme-Schutz
                    // beim naechsten Build ("Status nicht sicher lesbar,
                    // fail-closed"). Jetzt wird in Abstaenden erneut versucht.
                    tvoDebug("[AutoEnterHTTP] failed: \(err)")
                    self?.scheduleRestart(after: err)
                case .cancelled:
                    tvoDebug("[AutoEnterHTTP] cancelled")
                default: break
                }
            }
            listener.start(queue: queue)
        } catch {
            tvoDebug("[AutoEnterHTTP] could not start: \(error)")
            scheduleRestart(after: error)
        }
    }

    /// Baut den Listener nach einem Fehlschlag neu auf. Typischer Fall: der
    /// Port ist beim Start noch von einer Vorgaenger-Instanz belegt, die
    /// gerade beendet wird — nach ein paar Sekunden klappt es. Abstand waechst
    /// (2 s, 4 s, 6 s …), Deckel bei zehn Versuchen; danach steht die Ursache
    /// im Log statt in einer Endlosschleife.
    private func scheduleRestart(after error: Error) {
        startAttempts += 1
        guard startAttempts <= Self.maxStartAttempts else {
            tvoDebug("[AutoEnterHTTP] Port \(Self.port) bleibt belegt — nach \(Self.maxStartAttempts) Versuchen aufgegeben (\(error)). Aufnahme-Status ist fuer diese Sitzung nicht abfragbar.")
            return
        }
        let delay = Double(startAttempts) * 2.0
        tvoDebug("[AutoEnterHTTP] Neuversuch \(self.startAttempts)/\(Self.maxStartAttempts) in \(delay)s (Port \(Self.port) belegt?)")
        listener?.cancel()
        listener = nil
        queue.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.start()
        }
    }

    func stop() {
        listener?.cancel()
        listener = nil
        queue.async { [weak self] in
            guard let self = self else { return }
            for connection in self.sseClients { connection.cancel() }
            self.sseClients.removeAll()
        }
    }

    // MARK: - Connection handling

    private func handle(connection: NWConnection) {
        connection.start(queue: queue)
        receive(on: connection)
    }

    private func receive(on connection: NWConnection) {
        connection.receive(minimumIncompleteLength: 1,
                           maximumLength: 4096) { [weak self] data, _, isComplete, error in
            guard let self = self else { return }
            if let data = data, !data.isEmpty,
               let raw = String(data: data, encoding: .utf8) {
                // SSE: Verbindung bleibt OFFEN und wird in die Broadcast-Liste
                // aufgenommen (Windows: HandleSseSubscribe). Alle anderen Routen
                // antworten und schliessen wie bisher.
                if self.isSseRequest(raw) {
                    self.beginSseStream(on: connection)
                    return
                }
                let response = self.makeResponse(forRequest: raw)
                connection.send(content: response,
                                completion: .contentProcessed { _ in
                    connection.cancel()
                })
            } else if isComplete || error != nil {
                connection.cancel()
            }
        }
    }

    /// Sehr leichter HTTP-Parser — wir brauchen nur die erste Zeile
    /// (Method + Path), und ignorieren alle Header. Body ignorieren wir
    /// auch — der POST-Endpunkt hat keinen Inhalt.
    private func makeResponse(forRequest raw: String) -> Data {
        let firstLine = raw.split(separator: "\r\n", maxSplits: 1,
                                  omittingEmptySubsequences: true)
            .first.map(String.init) ?? ""
        let parts = firstLine.split(separator: " ")
        let method = parts.count > 0 ? String(parts[0]) : ""
        let path   = parts.count > 1 ? String(parts[1]) : ""

        let json: String
        switch (method, path) {
        case ("GET", "/autoenter/status"):
            let on = self.statusProvider?() ?? false
            json = "{\"on\":\(on ? "true" : "false")}"
        case ("POST", "/autoenter/toggle"):
            let on = self.toggleHandler?() ?? false
            json = "{\"on\":\(on ? "true" : "false")}"
        case ("GET", "/recording/status"):
            let busy = self.busyProvider?() ?? false
            json = "{\"busy\":\(busy ? "true" : "false")}"
        case ("POST", "/deployment/prepare"):
            let ready = self.deploymentPrepareHandler?() ?? false
            json = "{\"ready\":\(ready ? "true" : "false")}"
        case ("POST", "/deployment/release"):
            self.deploymentReleaseHandler?()
            json = "{\"ready\":true}"
        case ("POST", "/log"):
            // Diagnose-Endpunkt fuer das Stream-Deck-Plugin (kein Dateizugriff
            // im Webview). Der Rumpf wird 1:1 uebernommen — kein JSON-Parsing.
            appendPluginLog(body: httpBody(of: raw))
            return httpResponse(status: "204 No Content",
                                contentType: "text/plain",
                                body: Data())
        default:
            return httpResponse(status: "404 Not Found",
                                contentType: "text/plain",
                                body: Data("not found".utf8))
        }

        let body = Data(json.utf8)
        return httpResponse(status: "200 OK",
                            contentType: "application/json",
                            body: body)
    }

    // MARK: - Server-Sent Events (GET /autoenter/events)

    /// Erkennt eine SSE-Anfrage an der ersten Zeile.
    private func isSseRequest(_ raw: String) -> Bool {
        guard let firstLine = raw.split(separator: "\r\n", maxSplits: 1,
                                        omittingEmptySubsequences: true).first else { return false }
        let parts = firstLine.split(separator: " ")
        guard parts.count > 1 else { return false }
        return String(parts[0]) == "GET" && String(parts[1]) == "/autoenter/events"
    }

    /// Oeffnet den Ereignis-Strom: Header senden, aktuellen Stand SOFORT
    /// nachschieben (damit ein frisch verbundener Client nicht bis zur naechsten
    /// Aenderung im Dunkeln sitzt — 1:1 Windows) und die Verbindung offen halten.
    private func beginSseStream(on connection: NWConnection) {
        let header = """
        HTTP/1.1 200 OK\r
        Content-Type: text/event-stream; charset=utf-8\r
        Cache-Control: no-cache, no-store\r
        Connection: keep-alive\r
        X-Accel-Buffering: no\r
        \r

        """
        let initial = statusProvider?() ?? false
        var payload = Data(header.utf8)
        payload.append(Self.sseFrame(on: initial))

        connection.send(content: payload, completion: .contentProcessed { [weak self] error in
            guard let self = self else { return }
            if error != nil {
                connection.cancel()
                return
            }
            self.queue.async {
                self.sseClients.append(connection)
                tvoDebug("[AutoEnterHTTP] SSE-Client verbunden (initial=\(initial), gesamt=\(self.sseClients.count))")
            }
        })

        // Abbruch der Gegenseite erkennen, damit tote Verbindungen die Liste
        // nicht zuwachsen lassen.
        connection.stateUpdateHandler = { [weak self] state in
            switch state {
            case .failed, .cancelled:
                self?.queue.async { self?.removeSseClient(connection) }
            default: break
            }
        }
    }

    /// Schiebt einen neuen Zustand an alle offenen SSE-Clients.
    /// Windows-Pendant: `NotifyStateChanged` + `DoBroadcast`.
    func notifyStateChanged(_ newState: Bool) {
        queue.async { [weak self] in
            guard let self = self, !self.sseClients.isEmpty else { return }
            let frame = Self.sseFrame(on: newState)
            for connection in self.sseClients {
                connection.send(content: frame, completion: .contentProcessed { [weak self] error in
                    if error != nil {
                        self?.queue.async { self?.removeSseClient(connection) }
                    }
                })
            }
        }
    }

    private func removeSseClient(_ connection: NWConnection) {
        sseClients.removeAll { $0 === connection }
        connection.cancel()
    }

    private static func sseFrame(on: Bool) -> Data {
        Data("data: {\"on\":\(on ? "true" : "false")}\n\n".utf8)
    }

    // MARK: - POST /log

    /// Rumpf einer HTTP-Anfrage (alles nach der Leerzeile).
    private func httpBody(of raw: String) -> String {
        guard let range = raw.range(of: "\r\n\r\n") else { return "" }
        return String(raw[range.upperBound...])
    }

    private func appendPluginLog(body: String) {
        let path = (NSTemporaryDirectory() as NSString).appendingPathComponent("TVO-hotkey.log")
        let stamp = DateFormatter()
        stamp.dateFormat = "HH:mm:ss.SSS"
        let line = "\(stamp.string(from: Date())) PLUGIN \(body)\n"
        guard let data = line.data(using: .utf8) else { return }
        if let handle = FileHandle(forWritingAtPath: path) {
            defer { try? handle.close() }
            try? handle.seekToEnd()
            try? handle.write(contentsOf: data)
        } else {
            FileManager.default.createFile(atPath: path, contents: data)
        }
    }

    private func httpResponse(status: String,
                              contentType: String,
                              body: Data) -> Data {
        let header = """
        HTTP/1.1 \(status)\r
        Content-Type: \(contentType); charset=utf-8\r
        Content-Length: \(body.count)\r
        Connection: close\r
        Cache-Control: no-store\r
        \r

        """
        var data = Data(header.utf8)
        data.append(body)
        return data
    }
}
