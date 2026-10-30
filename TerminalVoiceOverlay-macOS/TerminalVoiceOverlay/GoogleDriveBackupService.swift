import AppKit
import Foundation
import Network

/// Minimal OAuth 2.0 loopback flow + Drive v3 REST calls. No external
/// dependencies so `bash build.sh` keeps working.
///
/// Stores a single backup JSON in the hidden `appDataFolder` space so
/// the user never sees it in normal Drive and other apps with a
/// different client-id can't access it.
final class GoogleDriveBackupService {

    static let shared = GoogleDriveBackupService()

    /// Wird nach erfolgreichem Verbinden gepostet — der AppDelegate zieht dann
    /// Slots und Historie sofort aus der Cloud.
    static let didConnectNotification = Notification.Name("tvo.GoogleDriveBackupService.didConnect")

    // Eigene Session mit harten Grenzen (Windows-Pendant DriveHttp, 03.09.2026):
    // URLSession.shared wartet bis 60 s pro Paket und 7 Tage insgesamt — ein
    // haengender Drive-Aufruf blockierte so Backup und Slot-Sync still.
    private static let driveSession: URLSession = {
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 30
        cfg.timeoutIntervalForResource = 60
        return URLSession(configuration: cfg)
    }()

    struct ConnectInfo {
        let refreshToken: String
        let email: String?
    }

    private init() {}

    private var tcpListener: NWListener?
    private var listenerPort: UInt16 = 0
    private var authCompletion: ((Result<ConnectInfo, Error>) -> Void)?
    private var clientIdPending: String = ""
    private var clientSecretPending: String = ""

    // MARK: - Public API

    func isAuthenticated() -> Bool {
        guard let s = try? PromptBoardStore.shared.settings() else { return false }
        return !(s.googleClientId ?? "").isEmpty
            && !(s.googleClientSecret ?? "").isEmpty
            && !(s.googleOAuthRefreshToken ?? "").isEmpty
    }

    func connect(clientId: String, clientSecret: String,
                 completion: @escaping (Result<ConnectInfo, Error>) -> Void) {
        // Doppelklick / erneuter Versuch: alten Listener sauber schliessen,
        // sonst landet der Browser-Rueckruf auf einem verwaisten Port.
        stopListener()
        authCompletion = completion
        clientIdPending = clientId
        clientSecretPending = clientSecret
        DiagLog.info("Drive", "connect_start")
        startLoopbackListener()
    }

    /// Upload the given JSON as promptboard-backup.json. If an older copy
    /// already exists, it is overwritten in place. Any additional duplicate
    /// files with the same name (leftovers from earlier upload attempts)
    /// are deleted — same cleanup strategy the BestJournal project uses
    /// to keep its Drive backup folder from accumulating 95% stale files.
    func upload(json: String, completion: @escaping (Result<Void, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e):
                completion(.failure(e))
            case .success(let token):
                self.findAllBackupFileIds(token: token) { result in
                    switch result {
                    case .failure(let e):
                        completion(.failure(e))
                    case .success(let existingIds):
                        if let keepId = existingIds.first {
                            // One in-place update + async delete of the duplicates.
                            self.replaceBackup(id: keepId, token: token, json: json) { replaceResult in
                                // Fire-and-forget cleanup — the main upload is already done.
                                let duplicates = Array(existingIds.dropFirst())
                                if !duplicates.isEmpty {
                                    self.deleteFiles(ids: duplicates, token: token)
                                }
                                completion(replaceResult)
                            }
                        } else {
                            self.createBackup(token: token, json: json, completion: completion)
                        }
                    }
                }
            }
        }
    }

    func downloadLatest(completion: @escaping (Result<String?, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                // Always grab the newest one (modifiedTime desc) and skip the
                // stale duplicates. Prevents the "95% wrong data" restore
                // that BestJournal hit before its cleanup rule existed.
                self.findNewestBackupFileId(token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let fileId):
                        guard let id = fileId else { completion(.success(nil)); return }
                        self.downloadContent(id: id, token: token, completion: completion)
                    }
                }
            }
        }
    }

    func signOut() {
        guard var s = try? PromptBoardStore.shared.settings() else { return }
        s.googleOAuthRefreshToken = nil
        s.googleAccountEmail = nil
        try? PromptBoardStore.shared.updateSettings(s)
    }

    // MARK: - OAuth loopback

    private func startLoopbackListener() {
        do {
            let params = NWParameters.tcp
            tcpListener = try NWListener(using: params, on: .any)
        } catch {
            DiagLog.error("Drive", "listener_failed", error)
            authCompletion?(.failure(error))
            authCompletion = nil
            return
        }

        tcpListener?.stateUpdateHandler = { [weak self] state in
            if case .ready = state, let port = self?.tcpListener?.port?.rawValue {
                self?.listenerPort = port
                DiagLog.info("Drive", "listener_ready", [("port", port)])
                self?.openBrowserForAuth(port: port)
            } else if case .failed(let err) = state {
                DiagLog.error("Drive", "listener_state_failed", err)
            }
        }
        tcpListener?.newConnectionHandler = { [weak self] conn in
            conn.start(queue: .main)
            self?.readRequest(conn)
        }
        tcpListener?.start(queue: .main)
    }

    private func openBrowserForAuth(port: UInt16) {
        let redirect = "http://127.0.0.1:\(port)/callback"
        let scope = "https://www.googleapis.com/auth/drive.appdata openid email"
        var comps = URLComponents(string: "https://accounts.google.com/o/oauth2/v2/auth")!
        comps.queryItems = [
            URLQueryItem(name: "client_id", value: clientIdPending),
            URLQueryItem(name: "redirect_uri", value: redirect),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "scope", value: scope),
            URLQueryItem(name: "access_type", value: "offline"),
            URLQueryItem(name: "prompt", value: "consent"),
        ]
        if let url = comps.url {
            DiagLog.info("Drive", "browser_open", [("port", port)])
            NSWorkspace.shared.open(url)
        } else {
            authCompletion?(.failure(errorString("Could not build auth URL")))
            authCompletion = nil
        }
    }

    private func readRequest(_ conn: NWConnection) {
        conn.receive(minimumIncompleteLength: 1, maximumLength: 8192) { [weak self] data, _, _, _ in
            guard let self = self, let data = data,
                  let request = String(data: data, encoding: .utf8) else {
                conn.cancel(); return
            }
            // First line: "GET /callback?code=XYZ&... HTTP/1.1"
            let firstLine = request.split(separator: "\r\n", maxSplits: 1).first.map(String.init) ?? request
            let parts = firstLine.split(separator: " ")
            guard parts.count >= 2, let path = parts[safe: 1] else { conn.cancel(); return }
            // Nur /callback beantwortet den Login. favicon.ico, Vorab-Verbindungen
            // u. ae. duerfen den Listener nicht beenden (sonst kommt der echte
            // Rueckruf nie an).
            guard path.hasPrefix("/callback") else {
                DiagLog.info("Drive", "callback_ignored", [("path", String(path.prefix(40)))])
                self.respond(conn, status: "404 Not Found", html: "")
                return
            }
            let items = URLComponents(string: "http://127.0.0.1\(path)")?.queryItems ?? []
            let code = items.first(where: { $0.name == "code" })?.value
            let googleError = items.first(where: { $0.name == "error" })?.value
            self.stopListener()
            guard let code = code, !code.isEmpty else {
                let msg = googleError == "access_denied"
                    ? "Zugriff wurde im Browser abgelehnt."
                    : "Google hat keinen Anmelde-Code geliefert (\(googleError ?? "unbekannt"))."
                DiagLog.warn("Drive", "callback_without_code", [("error", googleError)])
                self.finishAuth(.failure(self.errorString(msg)), conn: conn)
                return
            }
            DiagLog.info("Drive", "callback_code_received")
            // Browser erst antworten, wenn der Token-Tausch durch ist — vorher
            // meldete die Seite "fertig", obwohl die App gar nicht verbunden war.
            self.exchangeCodeForTokens(code: code) { result in
                DispatchQueue.main.async { self.finishAuth(result, conn: conn) }
            }
        }
    }

    /// Speichert das Ergebnis selbst in der Datenbank (unabhaengig davon, ob der
    /// Einstellungsdialog noch offen ist), zeigt es im Browser an und meldet es
    /// danach an den Aufrufer. Laeuft auf dem Main-Thread.
    private func finishAuth(_ result: Result<ConnectInfo, Error>, conn: NWConnection) {
        switch result {
        case .success(let info):
            var saved = false
            if var s = try? PromptBoardStore.shared.settings() {
                s.googleClientId = clientIdPending
                s.googleClientSecret = clientSecretPending
                s.googleOAuthRefreshToken = info.refreshToken
                s.googleAccountEmail = info.email
                do {
                    try PromptBoardStore.shared.updateSettings(s)
                    saved = isAuthenticated()
                } catch {
                    DiagLog.error("Drive", "token_save_failed", error)
                }
            }
            DiagLog.info("Drive", "connect_done", [("saved", saved), ("email", info.email)])
            if saved {
                let who = info.email.map { " als \($0)" } ?? ""
                respond(conn, status: "200 OK",
                        html: "<h2>Google Drive ist verbunden\(who).</h2><p>Du kannst dieses Fenster schließen und zum Overlay zurückkehren.</p>")
                NotificationCenter.default.post(name: Self.didConnectNotification, object: nil)
                authCompletion?(.success(info))
            } else {
                respond(conn, status: "500 Internal Server Error",
                        html: "<h2>Anmeldung bei Google war erfolgreich, aber das Speichern im Overlay ist fehlgeschlagen.</h2><p>Bitte im Overlay erneut auf Verbinden klicken.</p>")
                authCompletion?(.failure(errorString("Die Verbindung konnte nicht gespeichert werden.")))
            }
        case .failure(let err):
            DiagLog.error("Drive", "connect_failed", err)
            respond(conn, status: "200 OK",
                    html: "<h2>Verbindung mit Google Drive fehlgeschlagen.</h2><p>\(Self.htmlEscape(err.localizedDescription))</p><p>Bitte im Overlay erneut auf Verbinden klicken.</p>")
            authCompletion?(.failure(err))
        }
        authCompletion = nil
        // Overlay nach vorn holen, damit Status bzw. Fehlermeldung sichtbar sind.
        NSApp.activate(ignoringOtherApps: true)
    }

    private func respond(_ conn: NWConnection, status: String, html: String) {
        let page = html.isEmpty ? "" :
            "<!doctype html><html lang=\"de\"><head><meta charset=\"utf-8\"><title>Google Drive</title></head>" +
            "<body style=\"font-family:-apple-system,sans-serif;padding:40px\">\(html)</body></html>"
        let body = Data(page.utf8)
        var head = "HTTP/1.1 \(status)\r\nContent-Type: text/html; charset=utf-8\r\n"
        head += "Content-Length: \(body.count)\r\nConnection: close\r\n\r\n"
        conn.send(content: Data(head.utf8) + body, completion: .contentProcessed({ _ in conn.cancel() }))
    }

    private static func htmlEscape(_ s: String) -> String {
        s.replacingOccurrences(of: "&", with: "&amp;")
         .replacingOccurrences(of: "<", with: "&lt;")
         .replacingOccurrences(of: ">", with: "&gt;")
    }

    /// application/x-www-form-urlencoded streng nach RFC 3986 — `.urlQueryAllowed`
    /// liess `+ & = /` unkodiert durch und konnte Code oder Secret verfaelschen.
    static func formEncode(_ params: [String: String]) -> Data? {
        let allowed = CharacterSet(charactersIn:
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
        return params.map { "\($0.key)=\($0.value.addingPercentEncoding(withAllowedCharacters: allowed) ?? "")" }
            .joined(separator: "&").data(using: .utf8)
    }

    private func stopListener() {
        tcpListener?.cancel()
        tcpListener = nil
    }

    private func exchangeCodeForTokens(code: String,
                                       completion: @escaping (Result<ConnectInfo, Error>) -> Void) {
        let redirect = "http://127.0.0.1:\(listenerPort)/callback"
        var req = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
        req.httpMethod = "POST"
        req.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        req.httpBody = Self.formEncode([
            "code": code,
            "client_id": clientIdPending,
            "client_secret": clientSecretPending,
            "redirect_uri": redirect,
            "grant_type": "authorization_code",
        ])

        Self.driveSession.dataTask(with: req) { [weak self] data, response, err in
            guard let self = self else { return }
            if let err = err { completion(.failure(err)); return }
            let status = (response as? HTTPURLResponse)?.statusCode ?? -1
            let dict = data.flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]
            DiagLog.info("Drive", "token_exchange", [("status", status),
                                                       ("error", dict["error"] as? String),
                                                       ("desc", dict["error_description"] as? String),
                                                       ("hasRefresh", dict["refresh_token"] != nil)])
            guard let refresh = dict["refresh_token"] as? String,
                  let access = dict["access_token"] as? String else {
                if let gErr = dict["error"] as? String {
                    let desc = (dict["error_description"] as? String) ?? ""
                    completion(.failure(self.errorString("Google lehnt den Token-Tausch ab: \(gErr) \(desc) (HTTP \(status)).")))
                } else {
                    completion(.failure(self.errorString("Missing refresh_token. Widerrufe die App bei myaccount.google.com und versuch es erneut.")))
                }
                return
            }
            self.fetchEmail(accessToken: access) { email in
                completion(.success(ConnectInfo(refreshToken: refresh, email: email)))
            }
        }.resume()
    }

    private func fetchEmail(accessToken: String, completion: @escaping (String?) -> Void) {
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/oauth2/v2/userinfo")!)
        req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        Self.driveSession.dataTask(with: req) { data, _, _ in
            guard let data = data,
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let email = dict["email"] as? String else {
                completion(nil); return
            }
            completion(email)
        }.resume()
    }

    // MARK: - Drive API calls

    private func freshAccessToken(completion: @escaping (Result<String, Error>) -> Void) {
        guard let s = try? PromptBoardStore.shared.settings(),
              let refresh = s.googleOAuthRefreshToken, !refresh.isEmpty,
              let id = s.googleClientId, let secret = s.googleClientSecret else {
            completion(.failure(errorString("Google Drive ist noch nicht eingerichtet.")))
            return
        }
        var req = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
        req.httpMethod = "POST"
        req.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        req.httpBody = Self.formEncode([
            "client_id": id,
            "client_secret": secret,
            "refresh_token": refresh,
            "grant_type": "refresh_token",
        ])
        Self.driveSession.dataTask(with: req) { data, response, err in
            if let err = err { completion(.failure(err)); return }
            let status = (response as? HTTPURLResponse)?.statusCode ?? -1
            let bodyText = data.flatMap { String(data: $0, encoding: .utf8) } ?? "<no body>"
            let dict = data.flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] } ?? [:]

            if let access = dict["access_token"] as? String {
                completion(.success(access))
                return
            }

            // Kein access_token im Response — echte Ursache aus Google's Antwort extrahieren.
            let googleError = (dict["error"] as? String) ?? "unknown"
            let googleErrorDesc = (dict["error_description"] as? String) ?? ""
            NSLog("[GoogleDriveBackup] Token-Refresh fehlgeschlagen: status=\(status) error=\(googleError) desc=\(googleErrorDesc) body=\(bodyText.prefix(500))")

            // invalid_grant: Refresh-Token ist revoked/abgelaufen — Settings
            // automatisch zuruecksetzen, damit der naechste Verbindungsversuch
            // sauber durchlaeuft und der User klare Anweisung bekommt.
            if googleError == "invalid_grant" {
                DiagLog.warn("Drive", "refresh_invalid_grant_token_cleared", [("desc", googleErrorDesc)])
                if var settings = try? PromptBoardStore.shared.settings() {
                    settings.googleOAuthRefreshToken = nil
                    settings.googleAccountEmail = nil
                    try? PromptBoardStore.shared.updateSettings(settings)
                }
                completion(.failure(self.errorString(
                    "Google-Konto-Verbindung ist abgelaufen oder wurde widerrufen. " +
                    "Bitte in den Einstellungen → Google Drive neu verbinden.")))
                return
            }

            let userMessage = googleErrorDesc.isEmpty
                ? "Google-Token-Refresh fehlgeschlagen (\(googleError), HTTP \(status))."
                : "Google-Token-Refresh fehlgeschlagen: \(googleErrorDesc) (\(googleError))."
            completion(.failure(self.errorString(userMessage)))
        }.resume()
    }

    /// Lists every non-trashed `promptboard-backup.json` in the appDataFolder,
    /// newest first (modifiedTime desc). Return value is [] when nothing
    /// exists yet. Used both for "upload + cleanup duplicates" and for
    /// "download the newest one".
    private func findAllBackupFileIds(token: String,
                                       completion: @escaping (Result<[String], Error>) -> Void) {
        var comps = URLComponents(string: "https://www.googleapis.com/drive/v3/files")!
        comps.queryItems = [
            URLQueryItem(name: "spaces", value: "appDataFolder"),
            URLQueryItem(name: "q", value: "name = 'promptboard-backup.json' and trashed = false"),
            URLQueryItem(name: "fields", value: "files(id, modifiedTime)"),
            URLQueryItem(name: "orderBy", value: "modifiedTime desc"),
            URLQueryItem(name: "pageSize", value: "100"),
        ]
        var req = URLRequest(url: comps.url!)
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        Self.driveSession.dataTask(with: req) { data, _, err in
            if let err = err { completion(.failure(err)); return }
            guard let data = data,
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let files = dict["files"] as? [[String: Any]] else {
                completion(.success([])); return
            }
            let ids = files.compactMap { $0["id"] as? String }
            completion(.success(ids))
        }.resume()
    }

    /// Convenience wrapper: returns just the id of the newest backup, or nil
    /// if there is none.
    private func findNewestBackupFileId(token: String,
                                        completion: @escaping (Result<String?, Error>) -> Void) {
        findAllBackupFileIds(token: token) { result in
            switch result {
            case .success(let ids): completion(.success(ids.first))
            case .failure(let e):   completion(.failure(e))
            }
        }
    }

    /// Fire-and-forget batch deletion — ignores individual errors because
    /// losing a duplicate isn't fatal (it just means we'll try again next
    /// time). Errors go into the debug stream for diagnostics.
    private func deleteFiles(ids: [String], token: String) {
        for id in ids {
            var req = URLRequest(url: URL(string: "https://www.googleapis.com/drive/v3/files/\(id)")!)
            req.httpMethod = "DELETE"
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            Self.driveSession.dataTask(with: req) { _, resp, err in
                if let err = err {
                    NSLog("[DriveBackup] duplicate-cleanup delete failed for \(id): \(err.localizedDescription)")
                } else if let http = resp as? HTTPURLResponse,
                          !(200..<300).contains(http.statusCode) && http.statusCode != 404 {
                    NSLog("[DriveBackup] duplicate-cleanup HTTP \(http.statusCode) on \(id)")
                }
            }.resume()
        }
    }

    private func createBackup(token: String, json: String,
                              completion: @escaping (Result<Void, Error>) -> Void) {
        let boundary = "promptboard-\(UUID().uuidString)"
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")!)
        req.httpMethod = "POST"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("multipart/related; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        let metadata: [String: Any] = [
            "name": "promptboard-backup.json",
            "parents": ["appDataFolder"],
        ]
        let metaData = try! JSONSerialization.data(withJSONObject: metadata)
        let metaString = String(data: metaData, encoding: .utf8)!

        var body = Data()
        body.append("--\(boundary)\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n\(metaString)\r\n".data(using: .utf8)!)
        body.append("--\(boundary)\r\nContent-Type: application/json\r\n\r\n\(json)\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        req.httpBody = body

        Self.driveSession.dataTask(with: req) { _, resp, err in
            if let err = err { completion(.failure(err)); return }
            if let http = resp as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                completion(.failure(self.errorString("Upload failed: HTTP \(http.statusCode)")))
                return
            }
            completion(.success(()))
        }.resume()
    }

    private func replaceBackup(id: String, token: String, json: String,
                               completion: @escaping (Result<Void, Error>) -> Void) {
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/upload/drive/v3/files/\(id)?uploadType=media")!)
        req.httpMethod = "PATCH"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = json.data(using: .utf8)
        Self.driveSession.dataTask(with: req) { _, resp, err in
            if let err = err { completion(.failure(err)); return }
            if let http = resp as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                completion(.failure(self.errorString("Upload (replace) failed: HTTP \(http.statusCode)")))
                return
            }
            completion(.success(()))
        }.resume()
    }

    private func downloadContent(id: String, token: String,
                                 completion: @escaping (Result<String?, Error>) -> Void) {
        var comps = URLComponents(string: "https://www.googleapis.com/drive/v3/files/\(id)")!
        comps.queryItems = [URLQueryItem(name: "alt", value: "media")]
        var req = URLRequest(url: comps.url!)
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        Self.driveSession.dataTask(with: req) { data, _, err in
            if let err = err { completion(.failure(err)); return }
            guard let data = data, let s = String(data: data, encoding: .utf8) else {
                completion(.success(nil)); return
            }
            completion(.success(s))
        }.resume()
    }

    private func errorString(_ message: String) -> NSError {
        NSError(domain: "GoogleDriveBackupService", code: 1,
            userInfo: [NSLocalizedDescriptionKey: message])
    }

    // MARK: - Prompt-Historie (Phase 5)
    //
    // Eigene Upload/Download-Methoden fuer prompt-history.json. Logisch
    // identisch zur regulaeren Backup-Methode oben, aber mit anderem
    // Dateinamen — wir teilen uns den OAuth-Refresh-Token, schreiben
    // aber in eine separate Datei im selben appDataFolder. So bleibt
    // die plattformuebergreifende Spiegelung der Historie unabhaengig
    // vom Promptboard-Backup.

    private static let historyFileName = "prompt-history.json"

    /// Laedt den Inhalt von prompt-history.json zu Drive hoch und raeumt
    /// Duplikate weg. Identisches Cleanup-Muster wie der regulaere
    /// Backup-Pfad oben — in Drive bleibt immer genau eine aktuelle Datei.
    func uploadHistory(json: String, completion: @escaping (Result<Void, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.historyFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        if let keepId = ids.first {
                            self.replaceFile(id: keepId, token: token, json: json) { replaceResult in
                                let dups = Array(ids.dropFirst())
                                if !dups.isEmpty { self.deleteFiles(ids: dups, token: token) }
                                completion(replaceResult)
                            }
                        } else {
                            self.createFile(name: Self.historyFileName,
                                            token: token, json: json,
                                            completion: completion)
                        }
                    }
                }
            }
        }
    }

    /// Holt den aktuellsten Stand der prompt-history.json aus Drive.
    /// Liefert nil wenn noch keine Cloud-Historie existiert.
    func downloadHistory(completion: @escaping (Result<String?, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.historyFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        guard let id = ids.first else { completion(.success(nil)); return }
                        self.downloadContent(id: id, token: token, completion: completion)
                    }
                }
            }
        }
    }

    // MARK: - Prompt-Zwischenspeicher-Slots
    //
    // Eigene Upload/Download-Methoden fuer prompt-slots.json — gleiche Mechanik
    // wie Historie/Backup, nur anderer Dateiname. Teilt sich den OAuth-Token
    // und das appDataFolder, bleibt aber eine eigene Datei, damit die 10
    // Zwischenspeicher-Slots unabhaengig gespiegelt werden.

    private static let slotsFileName = "prompt-slots.json"

    /// Laedt prompt-slots.json zu Drive hoch und raeumt Duplikate weg. Wird
    /// SOFORT nach jedem Speichern/Loeschen eines Slots aufgerufen.
    func uploadSlots(json: String, completion: @escaping (Result<Void, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.slotsFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        if let keepId = ids.first {
                            self.replaceFile(id: keepId, token: token, json: json) { replaceResult in
                                let dups = Array(ids.dropFirst())
                                if !dups.isEmpty { self.deleteFiles(ids: dups, token: token) }
                                completion(replaceResult)
                            }
                        } else {
                            self.createFile(name: Self.slotsFileName,
                                            token: token, json: json,
                                            completion: completion)
                        }
                    }
                }
            }
        }
    }

    /// Holt den aktuellsten Stand der prompt-slots.json aus Drive. Liefert nil
    /// wenn noch keine Cloud-Slots existieren.
    func downloadSlots(completion: @escaping (Result<String?, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.slotsFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        guard let id = ids.first else { completion(.success(nil)); return }
                        self.downloadContent(id: id, token: token, completion: completion)
                    }
                }
            }
        }
    }

    // Persoenliches Vokabular-Woerterbuch (personal-vocabulary.txt) — eigene
    // Datei im appDataFolder, gleiche Mechanik wie die Slots. Geraete-uebergreifend
    // geteilt zwischen beiden Overlays.
    private static let vocabularyFileName = "personal-vocabulary.txt"

    /// Laedt das persoenliche Vokabular-Woerterbuch zu Drive hoch und raeumt
    /// Duplikate weg. Called by the three-way sync (PersonalVocabularyDriveSync).
    func uploadVocabulary(text: String, completion: @escaping (Result<Void, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.vocabularyFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        if let keepId = ids.first {
                            self.replaceFile(id: keepId, token: token, json: text) { replaceResult in
                                let dups = Array(ids.dropFirst())
                                if !dups.isEmpty { self.deleteFiles(ids: dups, token: token) }
                                completion(replaceResult)
                            }
                        } else {
                            self.createFile(name: Self.vocabularyFileName,
                                            token: token, json: text,
                                            completion: completion)
                        }
                    }
                }
            }
        }
    }

    /// Downloads the newest vocabulary copy from Drive. `text` is nil only when no
    /// copy exists yet; `fileCount` > 1 means duplicates exist (uploadVocabulary
    /// removes them). Fails on any non-2xx response or non-UTF-8 content, so an
    /// error body is never merged as vocabulary (the three-way merge would treat
    /// every real term as "deleted in the cloud").
    func downloadVocabulary(completion: @escaping (Result<(text: String?, fileCount: Int), Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.vocabularyFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        guard let id = ids.first else {
                            completion(.success((text: nil, fileCount: 0)))
                            return
                        }
                        var comps = URLComponents(string: "https://www.googleapis.com/drive/v3/files/\(id)")!
                        comps.queryItems = [URLQueryItem(name: "alt", value: "media")]
                        var req = URLRequest(url: comps.url!)
                        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
                        Self.driveSession.dataTask(with: req) { data, resp, err in
                            if let err = err { completion(.failure(err)); return }
                            if let http = resp as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                                completion(.failure(self.errorString("Vocabulary download failed: HTTP \(http.statusCode)")))
                                return
                            }
                            guard let data = data, let text = String(data: data, encoding: .utf8) else {
                                completion(.failure(self.errorString("Vocabulary download returned no UTF-8 text")))
                                return
                            }
                            completion(.success((text: text, fileCount: ids.count)))
                        }.resume()
                    }
                }
            }
        }
    }

    // ── Gemini-Prompt-Backup-Bundle (Etappe 2d Mac) — eine Datei im appDataFolder ──
    private static let geminiPromptsFileName = "gemini-prompts-bundle.json"

    /// Laedt das Gemini-Prompt-Bundle (10 Prompts + Schalter + Praeambel) zu Drive hoch.
    func uploadGeminiPrompts(json: String, completion: @escaping (Result<Void, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.geminiPromptsFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        if let keepId = ids.first {
                            self.replaceFile(id: keepId, token: token, json: json) { replaceResult in
                                let dups = Array(ids.dropFirst())
                                if !dups.isEmpty { self.deleteFiles(ids: dups, token: token) }
                                completion(replaceResult)
                            }
                        } else {
                            self.createFile(name: Self.geminiPromptsFileName,
                                            token: token, json: json, completion: completion)
                        }
                    }
                }
            }
        }
    }

    /// Holt das aktuellste Gemini-Prompt-Bundle aus Drive (nil wenn keins existiert).
    func downloadGeminiPrompts(completion: @escaping (Result<String?, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.geminiPromptsFileName, token: token) { result in
                    switch result {
                    case .failure(let e): completion(.failure(e))
                    case .success(let ids):
                        guard let id = ids.first else { completion(.success(nil)); return }
                        self.downloadContent(id: id, token: token, completion: completion)
                    }
                }
            }
        }
    }

    // MARK: - Generische Helpers (parametrisierbarer Filename)

    /// Wie `findAllBackupFileIds`, aber mit konfigurierbarem Filename —
    /// damit derselbe Code-Pfad sowohl die regulaere Backup-Datei als
    /// auch die History-Datei und (kuenftig) einzelne Archive-Dateien
    /// auflisten kann.
    private func findAllFileIds(named fileName: String, token: String,
                                 completion: @escaping (Result<[String], Error>) -> Void) {
        var comps = URLComponents(string: "https://www.googleapis.com/drive/v3/files")!
        comps.queryItems = [
            URLQueryItem(name: "spaces", value: "appDataFolder"),
            URLQueryItem(name: "q", value: "name = '\(fileName)' and trashed = false"),
            URLQueryItem(name: "fields", value: "files(id, modifiedTime)"),
            URLQueryItem(name: "orderBy", value: "modifiedTime desc"),
            URLQueryItem(name: "pageSize", value: "100"),
        ]
        var req = URLRequest(url: comps.url!)
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        Self.driveSession.dataTask(with: req) { data, resp, err in
            if let err = err { completion(.failure(err)); return }
            // A failed listing (401/403/5xx) must not look like "no file yet":
            // callers would then create a duplicate or seed over the real cloud copy.
            if let http = resp as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                completion(.failure(self.errorString("Drive file listing failed: HTTP \(http.statusCode)")))
                return
            }
            guard let data = data,
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let files = dict["files"] as? [[String: Any]] else {
                completion(.success([])); return
            }
            let ids = files.compactMap { $0["id"] as? String }
            completion(.success(ids))
        }.resume()
    }

    private func createFile(name: String, token: String, json: String,
                            completion: @escaping (Result<Void, Error>) -> Void) {
        let boundary = "promptboard-\(UUID().uuidString)"
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")!)
        req.httpMethod = "POST"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("multipart/related; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        let metadata: [String: Any] = ["name": name, "parents": ["appDataFolder"]]
        let metaData = try! JSONSerialization.data(withJSONObject: metadata)
        let metaString = String(data: metaData, encoding: .utf8)!

        var body = Data()
        body.append("--\(boundary)\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n\(metaString)\r\n".data(using: .utf8)!)
        body.append("--\(boundary)\r\nContent-Type: application/json\r\n\r\n\(json)\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        req.httpBody = body

        Self.driveSession.dataTask(with: req) { _, resp, err in
            if let err = err { completion(.failure(err)); return }
            if let http = resp as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                completion(.failure(self.errorString("Upload failed: HTTP \(http.statusCode)")))
                return
            }
            completion(.success(()))
        }.resume()
    }

    private func replaceFile(id: String, token: String, json: String,
                             completion: @escaping (Result<Void, Error>) -> Void) {
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/upload/drive/v3/files/\(id)?uploadType=media")!)
        req.httpMethod = "PATCH"
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = json.data(using: .utf8)
        Self.driveSession.dataTask(with: req) { _, resp, err in
            if let err = err { completion(.failure(err)); return }
            if let http = resp as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
                completion(.failure(self.errorString("Upload (replace) failed: HTTP \(http.statusCode)")))
                return
            }
            completion(.success(()))
        }.resume()
    }
}

fileprivate extension Array {
    subscript(safe idx: Int) -> Element? { indices.contains(idx) ? self[idx] : nil }
}

// MARK: - Gemini-Prompt-Backup-Sync (Etappe 2d Mac, Frank-Wunsch 2026-06-22)

/// Pendant zum Windows GeminiPromptDriveSync: buendelt die 10 Prompt-Dateien +
/// Woerterbuch-Schalter + Praeambel als EINE Drive-Datei (gemini-prompts-bundle.json),
/// LWW per savedAt-Timestamp (lokaler Marker .gemini-prompts-synced; sekundengenaues
/// ISO-8601-UTC, IDENTISCH zum Windows-Format). personal-vocabulary.txt ist NICHT
/// dabei (eigener Drei-Wege-Abgleich, PersonalVocabularyDriveSync). Nutzt die Drive-Mechanik von GoogleDriveBackupService.
enum GeminiPromptSync {
    private static let markerName = ".gemini-prompts-synced"

    private static var skDir: URL {
        FileManager.default.homeDirectoryForCurrentUser.appendingPathComponent("SK/VoiceOverlays")
    }

    private static var syncedFileNames: [String] {
        var names = [
            "gemini-correction-prompt-standard.txt",
            "gemini-correction-prompt-programmierung.txt",
            "gemini-correction-prompt-meta.txt",
        ]
        for i in 4...10 { names.append(String(format: "gemini-correction-prompt-%02d.txt", i)) }
        names.append("gemini-correction-prompt.txt")  // Legacy-Sammeldatei
        names.append("vocabulary-enabled.txt")        // Woerterbuch-Schalter
        names.append("vocabulary-preamble.txt")       // Woerterbuch-Einleitungstext
        names.append("parakeet-vocabulary-enabled.txt")  // Schalter des Parakeet-Woerterbuchs
        names.append("parakeet-vocabulary.txt")       // Begriffe des Parakeet-Woerterbuchs
        names.append(contentsOf: QuickPromptStore.fileNames)   // Schnell-Prompts 1-10 + Kurzbeschreibungen
        return names
    }

    private static var markerURL: URL { skDir.appendingPathComponent(markerName) }

    private static func readMarker() -> String {
        (try? String(contentsOf: markerURL, encoding: .utf8))?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }
    private static func writeMarker(_ s: String) {
        try? FileManager.default.createDirectory(at: skDir, withIntermediateDirectories: true)
        try? s.write(to: markerURL, atomically: true, encoding: .utf8)
    }

    private static func nowStamp() -> String {
        ISO8601DateFormatter().string(from: Date())  // "2026-06-22T19:30:00Z" — wie Windows
    }

    private static func buildLocalBundleJSON(savedAt: String) -> String? {
        var files: [String: String] = [:]
        for name in syncedFileNames {
            let url = skDir.appendingPathComponent(name)
            if let content = try? String(contentsOf: url, encoding: .utf8) {
                files[name] = content
            }
        }
        if files.isEmpty { return nil }
        let obj: [String: Any] = ["savedAt": savedAt, "files": files]
        guard let data = try? JSONSerialization.data(withJSONObject: obj),
              let json = String(data: data, encoding: .utf8) else { return nil }
        return json
    }

    // ── Dirty-Marker + Serialisierung (Pendant zu Windows SyncGate/.gemini-prompts-dirty,
    // Fix 21.09.2026: Schnell-Prompts kamen vom Mac nicht ins Backup) ──
    // Jede lokale Aenderung setzt zuerst den Dirty-Marker; er wird erst nach einem
    // ERFOLGREICHEN Upload geloescht. Ein gescheiterter Upload (Netz weg, Token
    // abgelaufen) wird so beim naechsten Start bzw. naechsten Verbinden wiederholt —
    // und ein ungesicherter lokaler Stand wird nie von einem Cloud-Bundle ueberschrieben.
    // Alle Laeufe gehen ueber eine serielle Queue; Uploads vor dem ersten Abgleich
    // warten, damit ein Start-Upload (z. B. KI-Ueberschrift) kein neueres Bundle
    // vom anderen Geraet mit veraltetem Mac-Stand ueberschreibt.
    private static let dirtyMarkerName = ".gemini-prompts-dirty"
    private static var dirtyURL: URL { skDir.appendingPathComponent(dirtyMarkerName) }
    private static let syncQueue = DispatchQueue(label: "GeminiPromptSync.queue")
    private static var initialSyncDone = false   // nur auf syncQueue lesen/schreiben

    private static func markDirty() {
        try? FileManager.default.createDirectory(at: skDir, withIntermediateDirectories: true)
        try? UUID().uuidString.write(to: dirtyURL, atomically: true, encoding: .utf8)
    }
    private static func readDirty() -> String {
        (try? String(contentsOf: dirtyURL, encoding: .utf8))?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }
    private static func clearDirty(ifStill generation: String) {
        guard !generation.isEmpty, readDirty() == generation else { return }
        try? FileManager.default.removeItem(at: dirtyURL)
    }

    /// true, wenn das Cloud-Bundle neuer ist als der lokale Marker. Vergleicht
    /// Zeitpunkte statt Zeichenketten — Windows schreibt "…+00:00" mit 7
    /// Nachkommastellen, der Mac "…Z" ohne.
    private static func isNewerThanMarker(_ cloudSavedAt: String, _ marker: String) -> Bool {
        guard let cloud = PromptSlotStore.parseDate(cloudSavedAt) else { return false }
        guard !marker.isEmpty, let local = PromptSlotStore.parseDate(marker) else { return true }
        return cloud > local
    }

    /// Laedt synchron hoch (nur auf syncQueue aufrufen). true bei Erfolg.
    private static func uploadBlocking() -> Bool {
        let generation = readDirty()
        let savedAt = nowStamp()
        guard let json = buildLocalBundleJSON(savedAt: savedAt) else {
            clearDirty(ifStill: generation)   // lokal nichts vorhanden — nichts zu sichern
            return true
        }
        let done = DispatchSemaphore(value: 0)
        var ok = false
        GoogleDriveBackupService.shared.uploadGeminiPrompts(json: json) { result in
            switch result {
            case .success:
                writeMarker(savedAt)
                clearDirty(ifStill: generation)
                ok = true
            case .failure(let e):
                NSLog("[GeminiPromptSync] Backup-Upload fehlgeschlagen: \(e.localizedDescription)")
            }
            done.signal()
        }
        _ = done.wait(timeout: .now() + 90)
        return ok
    }

    /// Nach jedem Speichern (Editor/Schalter/Praeambel/KI-Ueberschrift) aufrufen.
    /// Fire-and-forget.
    static func tryUpload() {
        markDirty()
        syncQueue.async {
            // Vor dem ersten Abgleich nicht hochladen — der Abgleich laedt den
            // markierten Stand anschliessend selbst hoch.
            guard initialSyncDone else { return }
            guard GoogleDriveBackupService.shared.isAuthenticated() else {
                NSLog("[GeminiPromptSync] Google Drive nicht verbunden — Upload wartet auf Verbindung")
                return
            }
            _ = uploadBlocking()
        }
    }

    /// Beim App-Start und nach jedem (Neu-)Verbinden mit Google Drive.
    /// Ungesicherte lokale Aenderungen (Dirty-Marker) werden zuerst hochgeladen;
    /// sonst gilt LWW: nur ein neueres Cloud-Bundle ueberschreibt lokal.
    static func trySyncFromCloud() {
        syncQueue.async {
            defer { initialSyncDone = true }
            guard GoogleDriveBackupService.shared.isAuthenticated() else {
                NSLog("[GeminiPromptSync] Google Drive nicht verbunden — kein Abgleich der Schnell-Prompts")
                return
            }
            if !readDirty().isEmpty {
                _ = uploadBlocking()
                return
            }
            let done = DispatchSemaphore(value: 0)
            var downloaded: Result<String?, Error> = .success(nil)
            GoogleDriveBackupService.shared.downloadGeminiPrompts { result in
                downloaded = result
                done.signal()
            }
            _ = done.wait(timeout: .now() + 90)
            guard case .success(let maybeJson) = downloaded else {
                if case .failure(let e) = downloaded {
                    NSLog("[GeminiPromptSync] Cloud-Abgleich fehlgeschlagen: \(e.localizedDescription)")
                }
                return
            }
            guard let json = maybeJson,
                  let data = json.data(using: .utf8),
                  let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let savedAt = obj["savedAt"] as? String,
                  let files = obj["files"] as? [String: String] else {
                _ = uploadBlocking()  // kein gueltiges Cloud-Bundle -> lokalen Stand saeen
                return
            }
            // Waehrend des Downloads lokal gespeichert? Dann gewinnt der lokale Stand.
            if !readDirty().isEmpty { _ = uploadBlocking(); return }
            if isNewerThanMarker(savedAt, readMarker()) {
                try? FileManager.default.createDirectory(at: skDir, withIntermediateDirectories: true)
                let allowed = Set(syncedFileNames)
                for (name, content) in files {
                    let safe = (name as NSString).lastPathComponent  // kein Pfad-Trick
                    guard !safe.isEmpty, allowed.contains(safe) else { continue }
                    try? content.write(to: skDir.appendingPathComponent(safe), atomically: true, encoding: .utf8)
                }
                writeMarker(savedAt)
                DispatchQueue.main.async {
                    NotificationCenter.default.post(name: QuickPromptStore.changedNotification, object: nil)
                }
            }
            // Lueckenpruefung (Pendant Windows MissingInCloud): fehlt eine lokal
            // vorhandene Datei im Cloud-Bundle (z. B. Schnell-Prompts, die ein
            // anderes Geraet nie hatte), den lokalen Stand hochladen.
            let missing = syncedFileNames.contains { name in
                files[name] == nil
                    && FileManager.default.fileExists(atPath: skDir.appendingPathComponent(name).path)
            }
            if missing { _ = uploadBlocking() }
        }
    }
}

// MARK: - Personal vocabulary three-way sync (port of Windows PromptVocabularyDriveSync)

/// Syncs the personal vocabulary (`personal-vocabulary.txt` in the SK folder)
/// through Google Drive between all devices and both overlays. Own file in the
/// `appDataFolder`, same OAuth mechanics as the slots (GoogleDriveBackupService).
///
/// THREE-WAY MERGE (Frank's decision 2026-10-22, replaces the former pure union):
/// every machine remembers the state it last agreed on with the cloud (`baseURL`).
/// New terms from any device are kept; a term deleted on ONE side since that last
/// sync disappears everywhere, and a changed spelling (github -> GitHub) wins over
/// the old one. Without a remembered state (first run) it is a plain union, so
/// nothing is lost that was not explicitly deleted.
enum PersonalVocabularyDriveSync {
    /// Local path of the vocabulary file -- same SK folder as the Gemini correction
    /// prompts, so TVO and CVO on ONE machine share the same file.
    static var localURL: URL {
        FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("SK/VoiceOverlays/personal-vocabulary.txt")
    }

    /// The state this machine last agreed on with the cloud (base of the three-way
    /// merge). Machine-local on purpose: shared by TVO and CVO, never synced.
    static var baseURL: URL {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSHomeDirectory()).appendingPathComponent("Library/Application Support")
        return support.appendingPathComponent("VoiceOverlays/personal-vocabulary.synced.txt")
    }

    /// Serializes all runs (launch + every settings save) so they never overlap.
    private static let syncQueue = DispatchQueue(label: "PersonalVocabularyDriveSync.queue")

    /// Three-way sync with Drive: merges base, local and cloud, rewrites the local file
    /// only if its terms changed (keeps the user's layout otherwise), uploads only if the
    /// cloud's terms differ, then remembers the result as the new base. Runs at startup
    /// and after every save in the settings dialog. Fire-and-forget; `completion` runs
    /// on the main thread with true when the run succeeded AND uploaded to the cloud.
    static func sync(completion: ((_ uploaded: Bool) -> Void)? = nil) {
        syncQueue.async {
            let uploaded = runBlocking()
            if let completion = completion {
                DispatchQueue.main.async { completion(uploaded) }
            }
        }
    }

    /// One sync run. Only call on syncQueue (blocks on the Drive calls).
    /// Returns true only if the run completed and uploaded a new cloud copy.
    private static func runBlocking() -> Bool {
        guard GoogleDriveBackupService.shared.isAuthenticated() else {
            tvoDebug("[VocabSync] skipped: drive not connected")
            return false
        }
        let localText = (try? String(contentsOf: localURL, encoding: .utf8)) ?? ""
        let baseText = try? String(contentsOf: baseURL, encoding: .utf8)

        let downloadDone = DispatchSemaphore(value: 0)
        var downloaded: Result<(text: String?, fileCount: Int), Error>?
        GoogleDriveBackupService.shared.downloadVocabulary { result in
            downloaded = result
            downloadDone.signal()
        }
        _ = downloadDone.wait(timeout: .now() + 90)
        let cloudText: String?
        let fileCount: Int
        switch downloaded {
        case .none:
            tvoDebug("[VocabSync] download timed out")
            return false
        case .some(.failure(let e)):
            tvoDebug("[VocabSync] download failed: \(e.localizedDescription)")
            return false
        case .some(.success(let value)):
            cloudText = value.text
            fileCount = value.fileCount
        }

        let mergedTerms = mergeTerms(base: baseText, local: localText, cloud: cloudText)
        let merged = render(local: localText, terms: mergedTerms)

        if terms(localText) != mergedTerms {
            // Saved in the settings dialog while we were downloading? Do not overwrite
            // that edit -- the run queued by the save merges it next.
            let current = (try? String(contentsOf: localURL, encoding: .utf8)) ?? ""
            if current != localText {
                tvoDebug("[VocabSync] local file changed during sync, deferring to next run")
                return false
            }
            do {
                try FileManager.default.createDirectory(
                    at: localURL.deletingLastPathComponent(), withIntermediateDirectories: true)
                try (merged + "\n").write(to: localURL, atomically: true, encoding: .utf8)
                tvoDebug("[VocabSync] local vocabulary updated")
            } catch {
                tvoDebug("[VocabSync] local write failed: \(error.localizedDescription)")
                return false
            }
        }

        let cloudDiffers = cloudText == nil
            ? !mergedTerms.isEmpty
            : Set(terms(cloudText)) != Set(mergedTerms)
        var didUpload = false
        if cloudDiffers || fileCount > 1 {
            let uploadDone = DispatchSemaphore(value: 0)
            var uploaded = false
            GoogleDriveBackupService.shared.uploadVocabulary(text: merged) { result in
                switch result {
                case .success:
                    uploaded = true
                case .failure(let e):
                    tvoDebug("[VocabSync] upload failed: \(e.localizedDescription)")
                }
                uploadDone.signal()
            }
            _ = uploadDone.wait(timeout: .now() + 90)
            guard uploaded else { return false }
            didUpload = true
            tvoDebug("[VocabSync] vocabulary uploaded to cloud")
        }

        do {
            try FileManager.default.createDirectory(
                at: baseURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try (merged + "\n").write(to: baseURL, atomically: true, encoding: .utf8)
        } catch {
            tvoDebug("[VocabSync] base write failed: \(error.localizedDescription)")
            return false
        }
        return didUpload
    }

    /// The terms of a vocabulary text: split on line breaks, comma and semicolon,
    /// trimmed, comment lines (#) skipped, deduplicated case-insensitively.
    static func terms(_ text: String?) -> [String] {
        var seen = Set<String>()
        var result: [String] = []
        guard let text = text, !text.isEmpty else { return result }
        for raw in text.replacingOccurrences(of: "\r", with: "\n").components(separatedBy: "\n") {
            let line = raw.trimmingCharacters(in: .whitespaces)
            if line.isEmpty || line.hasPrefix("#") { continue }
            for part in line.components(separatedBy: CharacterSet(charactersIn: ",;")) {
                let term = part.trimmingCharacters(in: .whitespaces)
                if !term.isEmpty && seen.insert(term.lowercased()).inserted { result.append(term) }
            }
        }
        return result
    }

    /// Three-way merge of the term lists, local order first. A term in the base that is
    /// missing on one side was deleted there since the last sync and is dropped. A term
    /// not in the base is new and kept. The spelling that changed since the base wins.
    /// No cloud copy yet: local is the truth (seeds the cloud). No base: plain union.
    static func mergeTerms(base: String?, local: String?, cloud: String?) -> [String] {
        let localTerms = terms(local)
        guard let cloud = cloud else { return localTerms }
        let cloudTerms = terms(cloud)
        let baseMap = toMap(terms(base))
        let localMap = toMap(localTerms)
        let cloudMap = toMap(cloudTerms)

        var result: [String] = []
        var done = Set<String>()
        for term in localTerms + cloudTerms {
            let key = term.lowercased()
            if !done.insert(key).inserted { continue }
            let b = baseMap[key]
            let l = localMap[key]
            let c = cloudMap[key]
            if b != nil && (l == nil || c == nil) { continue }
            let spelling: String
            if let l = l, b == nil || l != b {
                spelling = l
            } else if let c = c, b == nil || c != b {
                spelling = c
            } else {
                spelling = l ?? c ?? term
            }
            result.append(spelling)
        }
        return result
    }

    /// Case-insensitive lookup (lowercased key -> spelling), first occurrence wins.
    private static func toMap(_ terms: [String]) -> [String: String] {
        var map: [String: String] = [:]
        for t in terms {
            let key = t.lowercased()
            if map[key] == nil { map[key] = t }
        }
        return map
    }

    /// Comment lines of the local file first, then all terms as one comma line.
    private static func render(local: String?, terms: [String]) -> String {
        var lines: [String] = []
        if let local = local, !local.isEmpty {
            for raw in local.replacingOccurrences(of: "\r", with: "\n").components(separatedBy: "\n") {
                let line = raw.trimmingCharacters(in: .whitespaces)
                if line.hasPrefix("#") { lines.append(line) }
            }
        }
        if !terms.isEmpty { lines.append(terms.joined(separator: ", ")) }
        return lines.joined(separator: "\n")
    }
}
