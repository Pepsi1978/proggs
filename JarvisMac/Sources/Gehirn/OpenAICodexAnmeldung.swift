import Foundation

/// Anmeldung mit Franks ChatGPT-Konto per Codex-Gerätecode – eigener Login nur für diesen Mac.
/// Tokens liegen in ~/Library/Application Support/JarvisMac/codex.json (0600). NIE die Tokens des Handys oder aus
/// ~/SK/VoiceAgent mitbenutzen: OpenAI rotiert den Refresh-Token, zwei Geräte mit demselben Token beenden sich
/// gegenseitig (refresh_token_reused).
///
/// Ablauf für die Oberfläche:
///   let (code, url) = try await CodexAnmeldung.shared.start()   // Code anzeigen + in Zwischenablage, url öffnen
///   let email = try await CodexAnmeldung.shared.warteAufFreigabe() // wartet, bis Frank im Browser bestätigt hat
actor CodexAnmeldung {
    static let shared = CodexAnmeldung()

    enum Fehler: LocalizedError {
        case nichtAngemeldet, abgelaufen, antwort(String)
        var errorDescription: String? {
            switch self {
            case .nichtAngemeldet: return "Nicht mit ChatGPT angemeldet (Einstellungen → Konten → Mit ChatGPT anmelden)."
            case .abgelaufen: return "Der Anmeldecode ist abgelaufen. Bitte die Anmeldung neu starten."
            case .antwort(let t): return t
            }
        }
    }

    private struct Speicher: Codable {
        var accessToken: String
        var refreshToken: String?
        var ablauf: Date
        var konto: String?
        var email: String?
    }

    private static let clientId = "app_EMoamEEZ73f0CkXaXp7hrann"
    private static let tokenURL = URL(string: "https://auth.openai.com/oauth/token")!
    private static let codeURL = URL(string: "https://auth.openai.com/api/accounts/deviceauth/usercode")!
    private static let pruefURL = URL(string: "https://auth.openai.com/api/accounts/deviceauth/token")!
    static let freigabeURL = URL(string: "https://auth.openai.com/codex/device")!
    private static let redirect = "https://auth.openai.com/deviceauth/callback"

    private let datei = Pfade.unterstuetzung.appendingPathComponent("codex.json")
    private var speicher: Speicher?
    private var offen: (benutzerCode: String, kennung: String, intervall: Int, bis: Date)?
    private var erneuerung: Task<Speicher, Error>?

    init() {
        speicher = (try? Data(contentsOf: datei)).flatMap { try? JSONDecoder().decode(Speicher.self, from: $0) }
    }

    var angemeldet: Bool { speicher != nil }
    var email: String? { speicher?.email }

    func abmelden() {
        speicher = nil
        try? FileManager.default.removeItem(at: datei)
    }

    /// Nach 401/403 vom Backend: Token sofort als abgelaufen behandeln, damit der nächste Aufruf erneuert.
    func ungueltigMelden(_ code: Int) {
        guard var s = speicher else { return }
        s.ablauf = .distantPast
        speicher = s
    }

    // MARK: Gerätecode

    func start() async throws -> (benutzerCode: String, url: URL) {
        let (code, text) = try await Self.post(Self.codeURL, json: ["client_id": Self.clientId])
        guard code == 200, let o = JSON.objekt(text),
              let benutzer = (o["user_code"] as? String)?.trimmingCharacters(in: .whitespaces), !benutzer.isEmpty,
              let kennung = (o["device_auth_id"] as? String)?.trimmingCharacters(in: .whitespaces), !kennung.isEmpty
        else { throw Fehler.antwort("OpenAI hat keinen Gerätecode geliefert (\(code)): \(text.prefix(200))") }
        let iv = (o["interval"] as? Int) ?? Int(o["interval"] as? String ?? "") ?? 5
        offen = (benutzer, kennung, min(max(iv, 3), 30), Date().addingTimeInterval(15 * 60))
        return (benutzer, Self.freigabeURL)
    }

    /// Fragt alle paar Sekunden nach, bis Frank den Code im Browser bestätigt hat. Gibt die E-Mail des Kontos zurück.
    func warteAufFreigabe() async throws -> String? {
        guard var o = offen else { throw Fehler.antwort("Keine Anmeldung gestartet.") }
        while Date() < o.bis {
            try await Task.sleep(for: .seconds(o.intervall))
            let antwort: (Int, String)
            do {
                antwort = try await Self.post(Self.pruefURL, json: ["device_auth_id": o.kennung, "user_code": o.benutzerCode])
            } catch {
                o.intervall = min(o.intervall + 2, 30); continue
            }
            let (code, text) = antwort
            if code == 403 || code == 404 || code == 429 || code >= 500 {
                if code == 429 || text.contains("slow_down") { o.intervall = min(o.intervall + 5, 30) }
                continue
            }
            guard code == 200, let j = JSON.objekt(text),
                  let ac = (j["authorization_code"] as? String), !ac.isEmpty,
                  let verifier = (j["code_verifier"] as? String), !verifier.isEmpty
            else { throw Fehler.antwort("Geräteanmeldung abgelehnt (\(code)): \(text.prefix(200))") }
            offen = nil
            let (c2, t2) = try await Self.postForm(Self.tokenURL, [
                "grant_type": "authorization_code", "client_id": Self.clientId, "code": ac,
                "redirect_uri": Self.redirect, "code_verifier": verifier,
            ])
            let s = try Self.speicherAus(c2, t2, alt: nil)
            sichern(s)
            log("system", "ChatGPT-Anmeldung am Mac erfolgreich\(s.email.map { " (\($0))" } ?? "")")
            return s.email
        }
        offen = nil
        throw Fehler.abgelaufen
    }

    // MARK: Token

    /// Gültiger Zugriffstoken und ChatGPT-Konto. Erneuerung serialisiert: parallele Aufrufer warten auf dieselbe.
    func gueltigerToken() async throws -> (String, String) {
        guard let s = speicher else { throw Fehler.nichtAngemeldet }
        if Date() < s.ablauf.addingTimeInterval(-120), let k = s.konto { return (s.accessToken, k) }
        if let laufend = erneuerung {
            let n = try await laufend.value
            return (n.accessToken, n.konto ?? "")
        }
        guard let refresh = s.refreshToken else { abmelden(); throw Fehler.nichtAngemeldet }
        let t = Task { () throws -> Speicher in
            let (c, text) = try await Self.postForm(Self.tokenURL, [
                "grant_type": "refresh_token", "refresh_token": refresh, "client_id": Self.clientId,
            ])
            return try Self.speicherAus(c, text, alt: s)
        }
        erneuerung = t
        defer { erneuerung = nil }
        do {
            let n = try await t.value
            sichern(n)
            guard let k = n.konto else { throw Fehler.antwort("Im Codex-Token fehlt die ChatGPT-Account-ID.") }
            return (n.accessToken, k)
        } catch {
            let txt = error.localizedDescription.lowercased()
            if txt.contains("invalid_grant") || txt.contains("refresh_token_reused") || txt.contains("(401)") {
                abmelden()
                log("fehler", "ChatGPT-Anmeldung am Mac ungültig, bitte neu anmelden")
            }
            throw error
        }
    }

    private func sichern(_ s: Speicher) {
        speicher = s
        guard let d = try? JSONEncoder().encode(s) else { return }
        try? d.write(to: datei, options: .atomic)
        try? FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: datei.path)
    }

    private static func speicherAus(_ code: Int, _ text: String, alt: Speicher?) throws -> Speicher {
        guard code == 200, let j = JSON.objekt(text), let access = j["access_token"] as? String, !access.isEmpty else {
            throw Fehler.antwort("OpenAI-Anmeldung fehlgeschlagen (\(code)): \(text.prefix(200))")
        }
        let id = j["id_token"] as? String
        let laufzeit = (j["expires_in"] as? Double) ?? Double(j["expires_in"] as? Int ?? 3600)
        return Speicher(
            accessToken: access,
            refreshToken: (j["refresh_token"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? alt?.refreshToken,
            ablauf: Date().addingTimeInterval(laufzeit),
            konto: authWert(access, "chatgpt_account_id") ?? authWert(access, "account_id")
                ?? id.flatMap { authWert($0, "chatgpt_account_id") } ?? alt?.konto,
            email: id.flatMap { jwt($0)?["email"] as? String } ?? (jwt(access)?["email"] as? String)
                ?? authWert(access, "email") ?? alt?.email)
    }

    private static func jwt(_ token: String) -> JSONObjekt? {
        let teile = token.split(separator: ".")
        guard teile.count > 1 else { return nil }
        var b = String(teile[1]).replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
        while b.count % 4 != 0 { b += "=" }
        return Data(base64Encoded: b).flatMap { JSON.objekt($0) }
    }

    private static func authWert(_ token: String, _ name: String) -> String? {
        let v = (jwt(token)?["https://api.openai.com/auth"] as? JSONObjekt)?[name] as? String
        return (v?.isEmpty ?? true) ? nil : v
    }

    // MARK: HTTP

    private static func post(_ url: URL, json: JSONObjekt) async throws -> (Int, String) {
        var r = URLRequest(url: url, timeoutInterval: 25)
        r.httpMethod = "POST"
        r.setValue("application/json", forHTTPHeaderField: "Content-Type")
        r.setValue("application/json", forHTTPHeaderField: "Accept")
        r.httpBody = JSON.daten(json)
        let (d, a) = try await URLSession.shared.data(for: r)
        return ((a as? HTTPURLResponse)?.statusCode ?? 0, String(decoding: d, as: UTF8.self))
    }

    private static func postForm(_ url: URL, _ felder: [String: String]) async throws -> (Int, String) {
        var r = URLRequest(url: url, timeoutInterval: 25)
        r.httpMethod = "POST"
        r.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        r.setValue("application/json", forHTTPHeaderField: "Accept")
        var erlaubt = CharacterSet.alphanumerics
        erlaubt.insert(charactersIn: "-._~")
        r.httpBody = Data(felder.map { k, v in
            "\(k)=\(v.addingPercentEncoding(withAllowedCharacters: erlaubt) ?? v)"
        }.joined(separator: "&").utf8)
        let (d, a) = try await URLSession.shared.data(for: r)
        return ((a as? HTTPURLResponse)?.statusCode ?? 0, String(decoding: d, as: UTF8.self))
    }
}
