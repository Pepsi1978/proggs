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
        authCompletion = completion
        clientIdPending = clientId
        clientSecretPending = clientSecret
        startLoopbackListener()
    }

    /// Upload the given JSON as promptboard-backup-claudecodex.json. If an older copy
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
            authCompletion?(.failure(error))
            return
        }

        tcpListener?.stateUpdateHandler = { [weak self] state in
            if case .ready = state, let port = self?.tcpListener?.port?.rawValue {
                self?.listenerPort = port
                self?.openBrowserForAuth(port: port)
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
            NSWorkspace.shared.open(url)
        } else {
            authCompletion?(.failure(errorString("Could not build auth URL")))
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
            let fullURL = "http://127.0.0.1\(path)"
            let code = URLComponents(string: fullURL)?
                .queryItems?.first(where: { $0.name == "code" })?.value
            let body = "Received verification code. You may now close this window."
            let response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: \(body.utf8.count)\r\n\r\n\(body)"
            conn.send(content: response.data(using: .utf8), completion: .contentProcessed({ _ in
                conn.cancel()
                self.stopListener()
                if let code = code {
                    self.exchangeCodeForTokens(code: code)
                } else {
                    self.authCompletion?(.failure(self.errorString("No code in callback")))
                    self.authCompletion = nil
                }
            }))
        }
    }

    private func stopListener() {
        tcpListener?.cancel()
        tcpListener = nil
    }

    private func exchangeCodeForTokens(code: String) {
        let redirect = "http://127.0.0.1:\(listenerPort)/callback"
        var req = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
        req.httpMethod = "POST"
        req.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let body = [
            "code": code,
            "client_id": clientIdPending,
            "client_secret": clientSecretPending,
            "redirect_uri": redirect,
            "grant_type": "authorization_code",
        ]
        req.httpBody = body.map { "\($0.key)=\($0.value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")" }
            .joined(separator: "&").data(using: .utf8)

        URLSession.shared.dataTask(with: req) { [weak self] data, _, err in
            guard let self = self else { return }
            if let err = err { self.authCompletion?(.failure(err)); self.authCompletion = nil; return }
            guard let data = data,
                  let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let refresh = dict["refresh_token"] as? String,
                  let access = dict["access_token"] as? String else {
                self.authCompletion?(.failure(self.errorString("Missing refresh_token. Widerrufe die App bei myaccount.google.com und versuch es erneut.")))
                self.authCompletion = nil
                return
            }
            self.fetchEmail(accessToken: access) { email in
                self.authCompletion?(.success(ConnectInfo(refreshToken: refresh, email: email)))
                self.authCompletion = nil
            }
        }.resume()
    }

    private func fetchEmail(accessToken: String, completion: @escaping (String?) -> Void) {
        var req = URLRequest(url: URL(string: "https://www.googleapis.com/oauth2/v2/userinfo")!)
        req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        URLSession.shared.dataTask(with: req) { data, _, _ in
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
        let body = [
            "client_id": id,
            "client_secret": secret,
            "refresh_token": refresh,
            "grant_type": "refresh_token",
        ]
        req.httpBody = body.map { "\($0.key)=\($0.value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")" }
            .joined(separator: "&").data(using: .utf8)
        URLSession.shared.dataTask(with: req) { data, response, err in
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

    /// Lists every non-trashed `promptboard-backup-claudecodex.json` in the appDataFolder,
    /// newest first (modifiedTime desc). Return value is [] when nothing
    /// exists yet. Used both for "upload + cleanup duplicates" and for
    /// "download the newest one".
    private func findAllBackupFileIds(token: String,
                                       completion: @escaping (Result<[String], Error>) -> Void) {
        var comps = URLComponents(string: "https://www.googleapis.com/drive/v3/files")!
        comps.queryItems = [
            URLQueryItem(name: "spaces", value: "appDataFolder"),
            URLQueryItem(name: "q", value: "name = 'promptboard-backup-claudecodex.json' and trashed = false"),
            URLQueryItem(name: "fields", value: "files(id, modifiedTime)"),
            URLQueryItem(name: "orderBy", value: "modifiedTime desc"),
            URLQueryItem(name: "pageSize", value: "100"),
        ]
        var req = URLRequest(url: comps.url!)
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        URLSession.shared.dataTask(with: req) { data, _, err in
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
            URLSession.shared.dataTask(with: req) { _, resp, err in
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
            "name": "promptboard-backup-claudecodex.json",
            "parents": ["appDataFolder"],
        ]
        let metaData = try! JSONSerialization.data(withJSONObject: metadata)
        let metaString = String(data: metaData, encoding: .utf8)!

        var body = Data()
        body.append("--\(boundary)\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n\(metaString)\r\n".data(using: .utf8)!)
        body.append("--\(boundary)\r\nContent-Type: application/json\r\n\r\n\(json)\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        req.httpBody = body

        URLSession.shared.dataTask(with: req) { _, resp, err in
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
        URLSession.shared.dataTask(with: req) { _, resp, err in
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
        URLSession.shared.dataTask(with: req) { data, _, err in
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
    // Eigene Upload/Download-Methoden fuer prompt-history-claudecodex.json. Logisch
    // identisch zur regulaeren Backup-Methode oben, aber mit anderem
    // Dateinamen — wir teilen uns den OAuth-Refresh-Token, schreiben
    // aber in eine separate Datei im selben appDataFolder. So bleibt
    // die plattformuebergreifende Spiegelung der Historie unabhaengig
    // vom Promptboard-Backup.

    private static let historyFileName = "prompt-history-claudecodex.json"

    /// Laedt den Inhalt von prompt-history-claudecodex.json zu Drive hoch und raeumt
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

    /// Holt den aktuellsten Stand der prompt-history-claudecodex.json aus Drive.
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
    // Eigene Upload/Download-Methoden fuer prompt-slots-claudecodex.json — gleiche Mechanik
    // wie Historie/Backup, nur anderer Dateiname. Teilt sich den OAuth-Token
    // und das appDataFolder, bleibt aber eine eigene Datei, damit die 10
    // Zwischenspeicher-Slots unabhaengig gespiegelt werden.

    private static let slotsFileName = "prompt-slots-claudecodex.json"

    /// Laedt prompt-slots-claudecodex.json zu Drive hoch und raeumt Duplikate weg. Wird
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

    /// Holt den aktuellsten Stand der prompt-slots-claudecodex.json aus Drive. Liefert nil
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
    // Datei im appDataFolder, gleiche Mechanik wie die Slots. BEWUSST GETEILT
    // (gleicher Dateiname in beiden Overlays), damit ein Wort, das auf einem
    // Geraet/Overlay gespeichert wird, ueberall erscheint.
    private static let vocabularyFileName = "personal-vocabulary.txt"

    /// Laedt das persoenliche Vokabular-Woerterbuch zu Drive hoch und raeumt
    /// Duplikate weg. Wird nach jedem Speichern im Settings-Dialog aufgerufen.
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

    /// Holt das aktuellste Vokabular-Woerterbuch aus Drive. Liefert nil wenn
    /// noch keins existiert.
    func downloadVocabulary(completion: @escaping (Result<String?, Error>) -> Void) {
        freshAccessToken { tokenResult in
            switch tokenResult {
            case .failure(let e): completion(.failure(e))
            case .success(let token):
                self.findAllFileIds(named: Self.vocabularyFileName, token: token) { result in
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
        URLSession.shared.dataTask(with: req) { data, _, err in
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

        URLSession.shared.dataTask(with: req) { _, resp, err in
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
        URLSession.shared.dataTask(with: req) { _, resp, err in
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
/// dabei (eigener Vereinigungs-Sync). Nutzt die Drive-Mechanik von GoogleDriveBackupService.
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

    /// Nach jedem Speichern (Editor/Schalter/Praeambel) aufrufen. Fire-and-forget.
    static func tryUpload() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else { return }
        let savedAt = nowStamp()
        guard let json = buildLocalBundleJSON(savedAt: savedAt) else { return }
        GoogleDriveBackupService.shared.uploadGeminiPrompts(json: json) { result in
            if case .success = result { writeMarker(savedAt) }
        }
    }

    /// Einmal beim App-Start. LWW: nur ein neueres Cloud-Bundle ueberschreibt lokal.
    static func trySyncFromCloud() {
        guard GoogleDriveBackupService.shared.isAuthenticated() else { return }
        GoogleDriveBackupService.shared.downloadGeminiPrompts { result in
            guard case .success(let maybeJson) = result else { return }
            guard let json = maybeJson,
                  let data = json.data(using: .utf8),
                  let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let savedAt = obj["savedAt"] as? String,
                  let files = obj["files"] as? [String: String] else {
                tryUpload()  // kein gueltiges Cloud-Bundle -> lokalen Stand saeen
                return
            }
            if savedAt <= readMarker() { return }  // lokal aktuell/neuer
            try? FileManager.default.createDirectory(at: skDir, withIntermediateDirectories: true)
            for (name, content) in files {
                let safe = (name as NSString).lastPathComponent  // kein Pfad-Trick
                if safe.isEmpty { continue }
                try? content.write(to: skDir.appendingPathComponent(safe), atomically: true, encoding: .utf8)
            }
            writeMarker(savedAt)
        }
    }
}
