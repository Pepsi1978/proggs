import SwiftUI
import WebKit

/// Eigene Anmeldung des Macs bei der Claude Console. Das Guthaben gibt es nur mit dem Sitzungs-Cookie, nicht mit einem API-Schlüssel
/// (inoffizielle Schnittstelle, wie am Handy in AnthropicGuthabenFaehigkeit.kt). Das Anmeldefenster arbeitet flüchtig, die Cookies liegen nur im Tresor.
@MainActor
enum ConsoleSitzung {
    enum Fehler: LocalizedError {
        case abgelaufen
        case meldung(String)
        var errorDescription: String? {
            switch self {
            case .abgelaufen: return "Die Anthropic-Konsole lehnt die Anmeldung gerade ab. Sie bleibt gespeichert und Jarvis versucht es weiter; hilft das nicht, bitte neu anmelden."
            case .meldung(let t): return t
            }
        }
    }

    struct Keks: Codable, Equatable {
        var name, wert, domain: String
    }

    struct Stand {
        let rest: Double
        let gewaehrt: Double?
        let kurs: Double?
        /// Derselbe Satz wie das Werkzeug des Handys, für die Punkte auf der Karte.
        let satz: String
    }

    private static let console = "https://platform.claude.com"
    static let loginURL = URL(string: "\(console)/login")!
    /// Fest für Anmeldung und Abfrage: Die Bot-Abwehr der Console bindet ihr Cookie an die Browserkennung.
    static let browserKennung = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15"
    private static let orgSchluessel = "consoleOrganisation"
    private static let tresorName = "console-sitzung"
    private static var kursStand: (wert: Double, datum: String, geholt: Date)?
    private static func imTresor() -> [Keks]? {
        Tresor.lade([Keks].self, name: tresorName, klartext: Pfade.unterstuetzung.appendingPathComponent("console-sitzung-gibt-es-nie-im-klartext.json"))
    }
    private static var kekse: [Keks] = {
        let k = imTresor() ?? []
        geheimnisseMelden(k)
        return k
    }()
    /// Zählt jede An- und Abmeldung; ein Abruf, der über eine davon läuft, ist ungültig.
    private(set) static var nummer = 0

    static var angemeldet: Bool { kekse.contains { $0.name == "sessionKey" } }

    private static func gehoertZurConsole(_ domain: String) -> Bool {
        let d = domain.hasPrefix(".") ? String(domain.dropFirst()) : domain
        return d == "claude.com" || d.hasSuffix(".claude.com")
    }

    private static func geheimnisseMelden(_ k: [Keks]) { Geheimnisse.setzen(tresorName, k.map(\.wert)) }

    private static func sichern() {
        Tresor.sichere(kekse, name: tresorName)
        geheimnisseMelden(kekse)
    }

    static func uebernehmen(_ cookies: [HTTPCookie]) {
        kekse = cookies.filter { gehoertZurConsole($0.domain) }.map { Keks(name: $0.name, wert: $0.value, domain: $0.domain) }
        nummer += 1
        sichern()
        log("guthaben", "Am Mac bei der Console angemeldet", wo: "Guthaben")
    }

    /// Bis 1.0.12 lag die Sitzung im Cookie-Speicher von WebKit, 1.0.13 hat sie beim Umzug nicht mitgenommen. Einmal in den Tresor holen und erst danach dort löschen.
    static func altbestandUebernehmen() async {
        let speicher = WKWebsiteDataStore.default().httpCookieStore
        let alt = await speicher.allCookies().filter { gehoertZurConsole($0.domain) }
        guard alt.contains(where: { $0.name == "sessionKey" }), Tresor.stoerung == nil else { return }
        if !angemeldet { uebernehmen(alt) }
        guard imTresor() == kekse else { return }
        for c in alt { await speicher.deleteCookie(c) }
    }

    static func abmelden() {
        kekse = []
        nummer += 1
        sichern()
        UserDefaults.standard.removeObject(forKey: orgSchluessel)
    }

    static func stand() async throws -> Stand {
        let org = try await organisation()
        guard let c = try await holen("\(console)/api/organizations/\(org)/prepaid/credits") as? [String: Any],
              c["balance"] is [String: Any] || c["amount"] is Double else {
            throw Fehler.meldung("Die Anthropic-Konsole lieferte keine lesbare Antwort (Schnittstelle geändert?).")
        }
        let rest = betrag(c["balance"] as? [String: Any], cent: c["amount"] as? Double ?? 0)
        // Nur gültige Tranchen mit Rest zählen, abgelaufene Vormonate verfälschen sonst „gewährt“ und das Verfallsdatum.
        var gewaehrt = 0.0
        var verfaellt: Date?
        for feld in ["tranches", "promo_tranches"] {
            for t in c[feld] as? [[String: Any]] ?? [] {
                let ende = (t["expires_at"] as? String).flatMap(zeitpunkt)
                if let ende, ende <= Date() { continue }
                guard betrag(t["remaining"] as? [String: Any], cent: t["remaining_amount_minor_units"] as? Double ?? 0) > 0 else { continue }
                gewaehrt += betrag(t["granted"] as? [String: Any], cent: t["granted_amount_minor_units"] as? Double ?? 0)
                if let ende, verfaellt == nil || ende < verfaellt! { verfaellt = ende }
            }
        }
        let b = Guthaben.betrag
        let k = await kurs()
        var satz = "Übrig: \(b(rest)) $"
        if let k { satz += " (≈ \(b(rest * k.wert)) €)" }
        if gewaehrt > 0 { satz += " von \(b(gewaehrt)) $" }
        var details: [String] = []
        if gewaehrt > 0 { details.append("Verbraucht \(b(max(0, gewaehrt - rest))) $") }
        if let verfaellt { details.append("verfällt am " + zeit(verfaellt, "dd.MM.yyyy")) }
        details.append("Stand " + zeit(Date(), "dd.MM., HH:mm") + " Uhr")
        satz += ". " + details.joined(separator: " · ") + "."
        if let k {
            let wert = k.wert.formatted(.number.precision(.fractionLength(4)).locale(Locale(identifier: "de_DE")))
            satz += " Kurs 1 $ = \(wert) € (EZB \(k.datum))."
        }
        return Stand(rest: rest, gewaehrt: gewaehrt > 0 ? gewaehrt : nil, kurs: k?.wert, satz: satz)
    }

    /// Die Organisation mit API-Zugang (nicht die Abo-Organisation von claude.ai).
    private static func organisation() async throws -> String {
        if let org = UserDefaults.standard.string(forKey: orgSchluessel) { return org }
        let liste = try await holen("\(console)/api/organizations") as? [[String: Any]] ?? []
        guard let org = liste.first(where: { ($0["capabilities"] as? [String])?.contains("api") == true })?["uuid"] as? String else {
            throw Fehler.meldung("In der Anthropic-Konsole gibt es keine Organisation mit API-Zugang.")
        }
        UserDefaults.standard.set(org, forKey: orgSchluessel)
        return org
    }

    private static func holen(_ adresse: String) async throws -> Any {
        let nr = nummer
        var req = URLRequest(url: URL(string: adresse)!, timeoutInterval: 25)
        req.httpShouldHandleCookies = false
        req.setValue(kekse.map { "\($0.name)=\($0.wert)" }.joined(separator: "; "), forHTTPHeaderField: "Cookie")
        req.setValue(browserKennung, forHTTPHeaderField: "User-Agent")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue("\(console)/dashboard", forHTTPHeaderField: "Referer")
        let (daten, antwort) = try await URLSession.shared.data(for: req)
        let http = antwort as? HTTPURLResponse
        if let http, nr == nummer { erneuerteKekse(http) }
        let code = http?.statusCode ?? 0
        let json = try? JSONSerialization.jsonObject(with: daten)
        // 401 oder ein 403 mit JSON kommt von der Console selbst: Sitzung weg (am Handy so beobachtet). Ein 403 als HTML-Seite ist die Bot-Abwehr.
        if code == 401 || (code == 403 && json != nil) || http?.url?.path.contains("/login") == true { throw Fehler.abgelaufen }
        if let json, (200..<300).contains(code) { return json }
        switch code {
        case 200..<300: throw Fehler.meldung("Die Anthropic-Konsole lieferte keine lesbare Antwort (Schnittstelle geändert?).")
        case 403: throw Fehler.meldung("Die Bot-Abwehr der Anthropic-Konsole lässt die Abfrage gerade nicht durch (403).")
        case 429: throw Fehler.meldung("Anthropic bremst gerade (429); bitte gleich noch einmal versuchen.")
        default: throw Fehler.meldung("Die Anthropic-Konsole antwortet mit Fehler \(code).")
        }
    }

    /// Set-Cookie wie im Browser übernehmen, sonst veralten Sitzung und Bot-Abwehr-Cookie.
    private static func erneuerteKekse(_ http: HTTPURLResponse) {
        guard let felder = http.allHeaderFields as? [String: String], let url = http.url else { return }
        let neu = HTTPCookie.cookies(withResponseHeaderFields: felder, for: url).filter { gehoertZurConsole($0.domain) }
        guard !neu.isEmpty else { return }
        for c in neu {
            let weg = c.value.isEmpty || (c.expiresDate ?? .distantFuture) <= Date()
            // Eine Antwort beendet die Sitzung nie von allein, nur Franks Abmelden. Ein neuer Wert ersetzt den alten auf jeder Domain, sonst gingen zwei mit.
            if c.name == "sessionKey" {
                if weg { continue }
                kekse.removeAll { $0.name == c.name }
            } else {
                kekse.removeAll { $0.name == c.name && $0.domain == c.domain }
            }
            if !weg { kekse.append(Keks(name: c.name, wert: c.value, domain: c.domain)) }
        }
        sichern()
    }

    /// Euro je Dollar (EZB, ändert sich einmal am Tag); scheitert der Abruf, gilt der letzte Kurs.
    private static func kurs() async -> (wert: Double, datum: String, geholt: Date)? {
        if let k = kursStand, Date().timeIntervalSince(k.geholt) < 6 * 3600 { return k }
        for adresse in ["https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR", "https://api.frankfurter.app/latest?from=USD&to=EUR"] {
            guard let (daten, _) = try? await URLSession.shared.data(from: URL(string: adresse)!),
                  let j = try? JSONSerialization.jsonObject(with: daten) as? [String: Any],
                  let eur = (j["rates"] as? [String: Any])?["EUR"] as? Double, eur > 0 else { continue }
            kursStand = (eur, j["date"] as? String ?? "", Date())
            return kursStand
        }
        return kursStand
    }

    /// Betrag aus `{credits: {amount_minor, exponent}}`, sonst aus dem Cent-Feld.
    private static func betrag(_ o: [String: Any]?, cent: Double) -> Double {
        if let c = o?["credits"] as? [String: Any], let minor = c["amount_minor"] as? Double {
            return minor / pow(10, c["exponent"] as? Double ?? 2)
        }
        return cent / 100
    }

    /// ISO-Zeitpunkt, auch mit Mikrosekunden.
    private static func zeitpunkt(_ text: String) -> Date? {
        ISO8601DateFormatter().date(from: text.replacingOccurrences(of: #"\.\d+"#, with: "", options: .regularExpression))
    }

    private static func zeit(_ d: Date, _ muster: String) -> String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.dateFormat = muster
        return f.string(from: d)
    }
}

/// Fenster mit der Anmeldeseite der Console; schließt sich, sobald die Sitzung steht. Das Guthaben holt danach das `onDismiss` des Sheets.
struct ConsoleAnmeldungAnsicht: View {
    @State private var guthaben = Guthaben.shared

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Bei der Anthropic-Konsole anmelden").font(.jarvis(15, .semibold))
                    Text("Am besten per E-Mail-Code. Danach liest Jarvis dein Guthaben direkt hier am Mac.").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                }
                Spacer()
                Button("Schließen") { guthaben.anmeldungOffen = false }
            }
            .padding(14)
            ConsoleBrowser { guthaben.anmeldungOffen = false }
        }
        .frame(width: 560, height: 740)
        .foregroundStyle(Farbe.text)
    }
}

private struct ConsoleBrowser: NSViewRepresentable {
    let fertig: () -> Void

    func makeCoordinator() -> Koordinator { Koordinator(fertig: fertig) }

    func makeNSView(context: Context) -> NSView {
        let rahmen = NSView()
        context.coordinator.starten(in: rahmen)
        return rahmen
    }

    func updateNSView(_ nsView: NSView, context: Context) {}

    static func dismantleNSView(_ nsView: NSView, coordinator: Koordinator) { coordinator.wache?.cancel() }

    @MainActor
    final class Koordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        let fertig: () -> Void
        var wache: Task<Void, Never>?
        private let speicher = WKWebsiteDataStore.nonPersistent()
        private weak var rahmen: NSView?
        private weak var haupt: WKWebView?
        private var popups: [WKWebView] = []
        private var popupKnopf: NSButton?
        private var erledigt = false
        init(fertig: @escaping () -> Void) { self.fertig = fertig }

        func starten(in rahmen: NSView) {
            self.rahmen = rahmen
            let konfiguration = WKWebViewConfiguration()
            konfiguration.websiteDataStore = speicher
            let web = browser(konfiguration, in: rahmen)
            haupt = web
            web.load(URLRequest(url: ConsoleSitzung.loginURL))
            // Die Console wechselt nach der Anmeldung per Client-Navigation, und das Cookie kann später kommen als die neue Adresse: regelmäßig nachsehen.
            wache = Task { [weak self] in
                while !Task.isCancelled {
                    try? await Task.sleep(for: .seconds(1))
                    await self?.pruefen()
                }
            }
        }

        private func browser(_ konfiguration: WKWebViewConfiguration, in rahmen: NSView) -> WKWebView {
            let web = WKWebView(frame: rahmen.bounds, configuration: konfiguration)
            web.customUserAgent = ConsoleSitzung.browserKennung
            web.autoresizingMask = [.width, .height]
            web.navigationDelegate = self
            web.uiDelegate = self
            rahmen.addSubview(web)
            return web
        }

        private func pruefen() async {
            guard !erledigt, let u = haupt?.url?.absoluteString, u.hasPrefix("https://platform.claude.com"), !u.contains("/login") else { return }
            let cookies = await speicher.httpCookieStore.allCookies()
            guard !erledigt, cookies.contains(where: { $0.name == "sessionKey" }) else { return }
            erledigt = true
            wache?.cancel()
            ConsoleSitzung.uebernehmen(cookies)
            fertig()
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) { Task { await pruefen() } }

        // Die Google-Anmeldung öffnet ein Popup und meldet das Ergebnis an die Console zurück: Es liegt über der Seite, bis es sich schließt oder man es schließt.
        func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
            guard let rahmen else { return nil }
            let popup = browser(configuration, in: rahmen)
            popups.append(popup)
            popupKnopf?.removeFromSuperview()
            let knopf = NSButton(title: "Popup schließen", target: self, action: #selector(popupSchliessen))
            knopf.translatesAutoresizingMaskIntoConstraints = false
            rahmen.addSubview(knopf)
            NSLayoutConstraint.activate([knopf.topAnchor.constraint(equalTo: rahmen.topAnchor, constant: 8), knopf.trailingAnchor.constraint(equalTo: rahmen.trailingAnchor, constant: -8)])
            popupKnopf = knopf
            return popup
        }

        func webViewDidClose(_ webView: WKWebView) { popupEntfernen(webView) }

        @objc private func popupSchliessen() { if let p = popups.last { popupEntfernen(p) } }

        private func popupEntfernen(_ popup: WKWebView) {
            popups.removeAll { $0 === popup }
            popup.removeFromSuperview()
            if popups.isEmpty { popupKnopf?.removeFromSuperview(); popupKnopf = nil }
        }
    }
}
