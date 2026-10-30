import SwiftUI
import WebKit

/// Eigene Anmeldung des Macs bei der Claude Console. Das Guthaben gibt es nur mit dem Sitzungs-Cookie, nicht mit einem API-Schlüssel
/// (inoffizielle Schnittstelle, wie am Handy in AnthropicGuthabenFaehigkeit.kt). Die Cookies verwaltet WebKit für Jarvis.
@MainActor
enum ConsoleSitzung {
    enum Fehler: LocalizedError {
        case abgelaufen
        case meldung(String)
        var errorDescription: String? {
            switch self {
            case .abgelaufen: return "Die Anmeldung bei der Anthropic-Konsole ist abgelaufen."
            case .meldung(let t): return t
            }
        }
    }

    private static let console = "https://platform.claude.com"
    static let loginURL = URL(string: "\(console)/login")!
    /// Fest für Anmeldung und Abfrage: Die Bot-Abwehr der Console bindet ihr Cookie an die Browserkennung.
    static let browserKennung = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15"
    private static let orgSchluessel = "consoleOrganisation"
    private static var kursStand: (wert: Double, datum: String, geholt: Date)?

    private static func cookies() async -> [HTTPCookie] {
        await WKWebsiteDataStore.default().httpCookieStore.allCookies().filter { $0.domain.hasSuffix("claude.com") }
    }

    static func angemeldet() async -> Bool { await cookies().contains { $0.name == "sessionKey" } }

    static func abmelden() async {
        let store = WKWebsiteDataStore.default().httpCookieStore
        for c in await cookies() { await store.deleteCookie(c) }
        UserDefaults.standard.removeObject(forKey: orgSchluessel)
    }

    /// Derselbe Satz wie das Werkzeug des Handys, damit die Anzeige nur eine Form kennen muss.
    static func guthabenSatz() async throws -> String {
        let org = try await organisation()
        guard let c = try await holen("\(console)/api/organizations/\(org)/prepaid/credits") as? [String: Any] else {
            throw Fehler.meldung("Die Anthropic-Konsole lieferte keine lesbare Antwort.")
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
        return satz
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
        var req = URLRequest(url: URL(string: adresse)!, timeoutInterval: 25)
        req.httpShouldHandleCookies = false
        req.setValue(HTTPCookie.requestHeaderFields(with: await cookies())["Cookie"], forHTTPHeaderField: "Cookie")
        req.setValue(browserKennung, forHTTPHeaderField: "User-Agent")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        req.setValue("\(console)/dashboard", forHTTPHeaderField: "Referer")
        let (daten, antwort) = try await URLSession.shared.data(for: req)
        let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
        let json = try? JSONSerialization.jsonObject(with: daten)
        // 401 oder ein 403 mit JSON kommt von der Console selbst: Sitzung weg. Ein 403 als HTML-Seite ist die Bot-Abwehr.
        if code == 401 || (code == 403 && json != nil) { throw Fehler.abgelaufen }
        guard (200..<300).contains(code), let json else {
            switch code {
            case 403: throw Fehler.meldung("Die Bot-Abwehr der Anthropic-Konsole lässt die Abfrage gerade nicht durch (403).")
            case 429: throw Fehler.meldung("Anthropic bremst gerade (429); bitte gleich noch einmal versuchen.")
            default: throw Fehler.meldung("Die Anthropic-Konsole antwortet mit Fehler \(code).")
            }
        }
        return json
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

/// Fenster mit der Anmeldeseite der Console; schließt sich, sobald die Sitzung steht. Auch beim Schließen von Hand wird das Guthaben neu geholt.
struct ConsoleAnmeldungAnsicht: View {
    @State private var guthaben = Guthaben.shared

    private func fertig() {
        guthaben.anmeldungOffen = false
        Task { await guthaben.laden(frisch: true) }
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Bei der Anthropic-Konsole anmelden").font(.jarvis(15, .semibold))
                    Text("Am besten per E-Mail-Code. Danach liest Jarvis dein Guthaben direkt hier am Mac.").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                }
                Spacer()
                Button("Schließen") { fertig() }
            }
            .padding(14)
            ConsoleBrowser(fertig: fertig)
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
        let web = context.coordinator.neuerBrowser(WKWebViewConfiguration(), in: rahmen)
        // Die Console wechselt nach der Anmeldung per Client-Navigation, ohne neue Seite.
        context.coordinator.beobachter = web.observe(\.url) { [weak koordinator = context.coordinator] w, _ in
            MainActor.assumeIsolated { koordinator?.pruefen(w.url) }
        }
        web.load(URLRequest(url: ConsoleSitzung.loginURL))
        return rahmen
    }

    func updateNSView(_ nsView: NSView, context: Context) {}

    @MainActor
    final class Koordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        let fertig: () -> Void
        var beobachter: NSKeyValueObservation?
        private var erledigt = false
        init(fertig: @escaping () -> Void) { self.fertig = fertig }

        func neuerBrowser(_ konfiguration: WKWebViewConfiguration, in rahmen: NSView) -> WKWebView {
            konfiguration.websiteDataStore = .default()
            let web = WKWebView(frame: rahmen.bounds, configuration: konfiguration)
            web.customUserAgent = ConsoleSitzung.browserKennung
            web.autoresizingMask = [.width, .height]
            web.navigationDelegate = self
            web.uiDelegate = self
            rahmen.addSubview(web)
            return web
        }

        func pruefen(_ url: URL?) {
            guard !erledigt, let u = url?.absoluteString, u.hasPrefix("https://platform.claude.com"), !u.contains("/login") else { return }
            Task {
                guard await ConsoleSitzung.angemeldet(), !erledigt else { return }
                erledigt = true
                fertig()
            }
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) { pruefen(webView.url) }

        // Die Google-Anmeldung öffnet ein Popup und meldet das Ergebnis an die Console zurück: Es liegt über der Seite, bis es sich schließt.
        func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
            guard let rahmen = webView.superview else { return nil }
            return neuerBrowser(configuration, in: rahmen)
        }

        func webViewDidClose(_ webView: WKWebView) {
            let haupt = webView.superview?.subviews.first as? WKWebView
            webView.removeFromSuperview()
            pruefen(haupt?.url)
        }
    }
}
