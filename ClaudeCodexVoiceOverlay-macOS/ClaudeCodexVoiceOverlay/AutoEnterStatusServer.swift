import Foundation
import Network

// MARK: - AutoEnterStatusServer
// Portierung von TerminalVoiceOverlay-Windows/Services/AutoEnterStatusServer.cs
//
// Minimal-HTTP-Server auf 127.0.0.1:5723 fuer Stream-Deck-XL-Polling.
// Routen:
//   GET  /autoenter/status   -> {"on":true|false}
//   POST /autoenter/toggle   -> {"on":<neuer Stand>}
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

            listener.stateUpdateHandler = { state in
                switch state {
                case .ready:
                    tvoDebug("[AutoEnterHTTP] listening on 127.0.0.1:\(Self.port)")
                case .failed(let err):
                    tvoDebug("[AutoEnterHTTP] failed: \(err)")
                case .cancelled:
                    tvoDebug("[AutoEnterHTTP] cancelled")
                default: break
                }
            }
            listener.start(queue: queue)
        } catch {
            tvoDebug("[AutoEnterHTTP] could not start: \(error)")
        }
    }

    func stop() {
        listener?.cancel()
        listener = nil
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
