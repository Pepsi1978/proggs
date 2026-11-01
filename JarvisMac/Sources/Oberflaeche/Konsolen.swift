import CryptoKit
import SwiftUI
import WebKit

struct WebKeks: Codable, Equatable {
    var name, wert, domain, pfad: String
    var sicher: Bool
    var nurHttp: Bool
    var ablauf: Date?
}

struct WebSitzungDaten: Codable {
    var kekse: [WebKeks] = []
    /// localStorage je Herkunft: Manche Seiten (Kimi) halten die Anmeldung dort statt im Cookie.
    var speicher: [String: [String: String]] = [:]
}

@MainActor
func webSkript(_ web: WKWebView, _ quelle: String) async -> String? {
    await withCheckedContinuation { (w: CheckedContinuation<String?, Never>) in
        // Die Fassung mit Rückruf: Die async-Fassung von WebKit stürzt bei einem Ergebnis null ab.
        web.evaluateJavaScript(quelle) { ergebnis, _ in w.resume(returning: ergebnis as? String) }
    }
}

/// Anmeldungen für Google, claude.ai und Kimi. Sie liegen nur im Tresor; die Browser arbeiten flüchtig und bekommen sie für den Abruf zurück.
@MainActor
enum WebSitzungen {
    private static var vorrat: [String: WebSitzungDaten?] = [:]
    private static var nummern: [String: Int] = [:]
    private static let speicherLesen = "(function(){ try { var o={}; for(var i=0;i<localStorage.length;i++){ var k=localStorage.key(i); o[k]=localStorage.getItem(k); } return JSON.stringify({h:location.origin,w:o}); } catch(e){ return null; } })()"

    private static func name(_ id: String) -> String { "konsole-" + id }
    private static var keinKlartext: URL { Pfade.unterstuetzung.appendingPathComponent("konsole-gibt-es-nie-im-klartext.json") }

    static func gehoertDazu(_ id: String, _ domain: String) -> Bool {
        let d = (domain.hasPrefix(".") ? String(domain.dropFirst()) : domain).lowercased()
        func teil(_ s: String) -> Bool { d == s || d.hasSuffix("." + s) }
        switch id {
        case "google": return teil("google.com")
        case "claude": return teil("claude.ai")
        case "kimi": return teil("kimi.ai") || teil("kimi.com") || teil("moonshot.cn") || teil("moonshot.ai")
        default: return false
        }
    }

    private static func melden(_ id: String, _ d: WebSitzungDaten?) {
        Geheimnisse.setzen(name(id), (d?.kekse.map(\.wert) ?? []) + (d?.speicher.values.flatMap { Array($0.values) } ?? []))
    }

    static func lade(_ id: String) -> WebSitzungDaten? {
        if let d = vorrat[id] { return d }
        let neu = Tresor.lade(WebSitzungDaten.self, name: name(id), klartext: keinKlartext)
        vorrat[id] = .some(neu)
        melden(id, neu)
        return neu
    }

    static func angemeldet(_ id: String) -> Bool {
        guard let d = lade(id) else { return false }
        switch id {
        case "google": return d.kekse.contains { $0.name == "SAPISID" }
        case "claude": return d.kekse.contains { $0.name == "sessionKey" }
        default: return !d.kekse.isEmpty || !d.speicher.isEmpty
        }
    }

    static func nummer(_ id: String) -> Int { nummern[id] ?? 0 }

    static func abmelden(_ id: String) {
        vorrat[id] = .some(nil)
        nummern[id, default: 0] += 1
        Tresor.loesche(name: name(id))
        melden(id, nil)
        log("guthaben", "Am Mac abgemeldet: \(id)", wo: "Konsolen")
    }

    /// Zieht Cookies und localStorage aus einem Browser in den Tresor. neu = eine Anmeldung, die eine laufende Abfrage ungültig macht.
    static func erfassen(_ id: String, _ web: WKWebView, neu: Bool, nurBeiNummer: Int? = nil) async {
        let cookies = await web.configuration.websiteDataStore.httpCookieStore.allCookies()
        let kekse = cookies.filter { gehoertDazu(id, $0.domain) }.map {
            WebKeks(name: $0.name, wert: $0.value, domain: $0.domain, pfad: $0.path, sicher: $0.isSecure, nurHttp: $0.isHTTPOnly, ablauf: $0.expiresDate)
        }
        let roh = await webSkript(web, speicherLesen)
        if let nurBeiNummer, nummer(id) != nurBeiNummer { return }
        var d = WebSitzungDaten(kekse: kekse, speicher: neu ? [:] : (lade(id)?.speicher ?? [:]))
        if let roh, let o = JSON.objekt(roh), let h = o["h"] as? String, let host = URL(string: h)?.host, gehoertDazu(id, host), let w = o["w"] as? JSONObjekt {
            d.speicher[h] = w.compactMapValues { $0 as? String }
        }
        if neu { nummern[id, default: 0] += 1 }
        vorrat[id] = .some(d)
        Tresor.sichere(d, name: name(id))
        melden(id, d)
    }

    /// Cookie-Kopf für eine Adresse, wie ein Browser ihn schickt.
    static func keksKopf(_ id: String, _ adresse: String) -> String {
        guard let u = URL(string: adresse), let host = u.host?.lowercased(), let d = lade(id) else { return "" }
        return d.kekse.filter { k in
            let w = (k.domain.hasPrefix(".") ? String(k.domain.dropFirst()) : k.domain).lowercased()
            return (host == w || (k.domain.hasPrefix(".") && host.hasSuffix("." + w))) && u.path.hasPrefix(k.pfad) && (k.ablauf ?? .distantFuture) > Date()
        }.map { "\($0.name)=\($0.wert)" }.joined(separator: "; ")
    }

    static func keks(_ id: String, _ name: String) -> String? { lade(id)?.kekse.first { $0.name == name }?.wert }
}

/// Liest eine Seite in einem unsichtbaren Browser mit Franks Anmeldung aus dem Tresor: Die Bot-Abwehr lässt nur einen echten Browser durch, und manche Seite baut ihren Inhalt erst per Skript auf.
@MainActor
enum VersteckterLeser {
    enum Fehler: LocalizedError {
        case verschoben
        var errorDescription: String? { "Abruf wegen eines Gesprächs verschoben." }
    }

    static func lies(_ id: String, url: String, js: String, sekunden: Double, erzwungen: Bool, fertig: @MainActor (String) -> Bool) async throws -> String {
        let konfiguration = WKWebViewConfiguration()
        konfiguration.websiteDataStore = .nonPersistent()
        let daten = WebSitzungen.lade(id)
        if let speicher = daten?.speicher, !speicher.isEmpty {
            let quelle = "(function(){ var m=\(JSON.text(speicher)); var s=m[location.origin]; if(!s) return; try { for(var k in s) if(localStorage.getItem(k)===null) localStorage.setItem(k,s[k]); } catch(e){} })();"
            konfiguration.userContentController.addUserScript(WKUserScript(source: quelle, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        for k in daten?.kekse ?? [] {
            var werte: [HTTPCookiePropertyKey: Any] = [.name: k.name, .value: k.wert, .domain: k.domain, .path: k.pfad]
            if k.sicher { werte[.secure] = "TRUE" }
            if let a = k.ablauf { werte[.expires] = a }
            if k.nurHttp { werte[HTTPCookiePropertyKey("HttpOnly")] = "TRUE" }
            if let c = HTTPCookie(properties: werte) { await konfiguration.websiteDataStore.httpCookieStore.setCookie(c) }
        }
        let nummer = WebSitzungen.nummer(id)
        let rahmen = NSRect(x: -32000, y: -32000, width: 1080, height: 1400)
        let web = WKWebView(frame: NSRect(origin: .zero, size: rahmen.size), configuration: konfiguration)
        web.customUserAgent = ConsoleSitzung.browserKennung
        // In einem eigenen, unsichtbaren Fenster: Ohne Fenster drosselt WebKit die Zeitgeber der Seite.
        let fenster = NSWindow(contentRect: rahmen, styleMask: [.borderless], backing: .buffered, defer: false)
        fenster.isReleasedWhenClosed = false
        fenster.alphaValue = 0
        fenster.ignoresMouseEvents = true
        fenster.sharingType = .none
        fenster.contentView = web
        fenster.orderBack(nil)
        defer { web.stopLoading(); fenster.close() }
        guard let adresse = URL(string: url) else { return "" }
        web.load(URLRequest(url: adresse))
        var letzter = ""
        let ende = Date().addingTimeInterval(sekunden)
        while Date() < ende {
            try await Task.sleep(for: .seconds(1))
            // Die Stimme hat Vorrang: Ein Abruf aus dem Hintergrund weicht sofort einem Gespräch.
            if !erzwungen && KontingentQuellen.gespraechLaeuft { throw Fehler.verschoben }
            guard let s = await webSkript(web, js), !s.isEmpty else { continue }
            letzter = s
            if fertig(s) { break }
        }
        // Erneuerte Cookies und Token zurück in den Tresor, sonst läuft die Anmeldung trotz Abfragen ab.
        await WebSitzungen.erfassen(id, web, neu: false, nurBeiNummer: nummer)
        return letzter
    }
}

/// Die Limits am Mac selbst lesen, mit eigener Anmeldung. Gleiche Abfragen wie am Handy in KontingenteFaehigkeit.kt.
@MainActor
enum KontingentQuellen {
    enum Fehler: LocalizedError {
        case nichtAngemeldet(String), meldung(String)
        var errorDescription: String? {
            switch self {
            case .nichtAngemeldet(let t), .meldung(let t): return t
            }
        }
    }

    private struct Roh {
        let name: String, genutzt: Double, fenster: Int64, reset: Int64, gelesen: Int64
    }

    static let alle: [(id: String, titel: String)] = [("claude", "Claude (Anthropic)"), ("codex", "ChatGPT / Codex (OpenAI)"), ("kimi", "Kimi"), ("gemini", "Gemini / Antigravity (Google)")]
    static let claudeLogin = "https://claude.ai/login"
    static let kimiSeite = "https://www.kimi.ai/settings/subscription"
    private static let speicher = "kontingente.mac"
    private static let stunden5: Int64 = 5 * 3600
    private static let woche: Int64 = 7 * 86400
    private static let berlin = TimeZone(identifier: "Europe/Berlin") ?? .current

    static var gespraechLaeuft: Bool { SprachSchleife.shared.zustand != .aus || LiveGespraech.shared.aktiv }

    private static var jetzt: Int64 { Int64(Date().timeIntervalSince1970) }

    private static func zeit(_ s: Int64, _ muster: String) -> String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.timeZone = berlin; f.dateFormat = muster
        return f.string(from: Date(timeIntervalSince1970: Double(s)))
    }

    private static func zeitText(_ s: Int64) -> String {
        var k = Calendar(identifier: .gregorian); k.timeZone = berlin
        let d = Date(timeIntervalSince1970: Double(s))
        if k.isDateInToday(d) { return "heute, " + zeit(s, "HH:mm") + " Uhr" }
        if k.isDateInTomorrow(d) { return "morgen, " + zeit(s, "HH:mm") + " Uhr" }
        return zeit(s, "EEEE, dd.MM., HH:mm") + " Uhr"
    }

    private static func dauerText(_ s: Int64) -> String {
        s >= 2 * 86400 ? "\(s / 86400) Tage" : (s >= 2 * 3600 ? "\(s / 3600) Stunden" : "\(max(s / 60, 1)) Minuten")
    }

    private static func anzeige(_ l: Roh) -> Kontingente.Limit {
        let j = jetzt
        let abgelaufen = l.reset > 0 && l.reset <= j
        let vergangen = l.fenster - (l.reset - (l.gelesen > 0 ? l.gelesen : j))
        let rechenbar = l.reset > j && l.fenster > 0 && l.genutzt > 0 && vergangen >= l.fenster / 10
        let hoch: Double? = rechenbar ? l.genutzt * Double(l.fenster) / Double(vergangen) : nil
        let schnell = l.genutzt < 100 && (hoch ?? 0) > 100
        let satz = l.reset <= 0 ? "Das Zeitfenster beginnt mit der nächsten Nutzung" : (abgelaufen ? "Zurückgesetzt, neuer Verbrauch erst beim nächsten Abruf" : "Zurückgesetzt: " + zeitText(l.reset))
        var tempo = ""
        if l.reset > 0 && !abgelaufen {
            if l.genutzt >= 100 { tempo = "Aufgebraucht bis zum Zurücksetzen." }
            else if schnell {
                let leer = l.reset - l.fenster + Int64(Double(vergangen) * 100 / l.genutzt)
                tempo = "Bei diesem Tempo wäre es \(zeitText(leer)) leer, \(dauerText(l.reset - leer)) vor dem Zurücksetzen."
            } else if let hoch { tempo = "Tempo passt: hochgerechnet etwa \(Int(hoch)) % bis zum Zurücksetzen." }
            else { tempo = "Für eine Hochrechnung ist es noch zu früh." }
        }
        return Kontingente.Limit(name: l.name, genutzt: abgelaufen ? 0 : l.genutzt, resetSatz: satz, resetAm: l.reset > 0 ? zeit(l.reset, "EEEE, dd.MM., HH:mm") + " Uhr" : "",
                                 resetSek: Double(l.reset), tempo: tempo, zuSchnell: schnell)
    }

    /// Der letzte Termin um 6 oder 18 Uhr deutscher Zeit: Bis zum nächsten bleibt der gespeicherte Stand, außer Frank fragt ausdrücklich neu.
    private static func letzterTermin() -> Int64 {
        var k = Calendar(identifier: .gregorian); k.timeZone = berlin
        let stunde = k.component(.hour, from: Date())
        let beginn = k.startOfDay(for: Date())
        let t = k.date(byAdding: .hour, value: stunde >= 18 ? 18 : (stunde >= 6 ? 6 : -6), to: beginn) ?? beginn
        return Int64(t.timeIntervalSince1970)
    }

    static func agyPfad() -> String? {
        let heim = Pfade.heim.path
        return ["\(heim)/.agy/bin/agy", "\(heim)/.local/bin/agy", "/opt/homebrew/bin/agy", "/usr/local/bin/agy"].first { FileManager.default.isExecutableFile(atPath: $0) }
    }

    static func hierMoeglich(_ id: String) async -> Bool {
        switch id {
        case "codex": return await CodexAnmeldung.shared.angemeldet
        case "gemini": return agyPfad() != nil
        default: return WebSitzungen.angemeldet(id)
        }
    }

    private static func gespeichert() -> JSONObjekt {
        UserDefaults.standard.string(forKey: speicher).flatMap { JSON.objekt($0) } ?? [:]
    }

    private static func ganz(_ o: JSONObjekt, _ k: String) -> Int64 { (o[k] as? NSNumber)?.int64Value ?? 0 }
    private static func zahl(_ o: JSONObjekt, _ k: String) -> Double { (o[k] as? NSNumber)?.doubleValue ?? 0 }

    /// Stand eines Dienstes am Mac. Scheitert die Abfrage, bleibt der letzte Stand mit Hinweis.
    static func lies(_ id: String, titel: String, frisch: Bool) async -> Kontingente.Anbieter {
        let alt = gespeichert()[id] as? JSONObjekt
        let altLimits = (alt?["limits"] as? [JSONObjekt] ?? []).map { Roh(name: $0["name"] as? String ?? "", genutzt: zahl($0, "genutzt"), fenster: ganz($0, "fenster"), reset: ganz($0, "reset"), gelesen: ganz($0, "gelesen")) }
        let altKonto = alt?["konto"] as? String ?? ""
        let altGeholt = alt.map { ganz($0, "geholt") } ?? 0
        func aus(_ konto: String, _ l: [Roh], _ hinweis: String, _ anmelden: Bool, _ geholt: Int64) -> Kontingente.Anbieter {
            Kontingente.Anbieter(id: id, titel: titel, konto: konto, hinweis: hinweis, anmelden: anmelden, stand: geholt <= 0 ? "" : "Stand " + zeit(geholt, "dd.MM., HH:mm") + " Uhr", limits: l.map { anzeige($0) })
        }
        if !frisch, !altLimits.isEmpty, altGeholt >= letzterTermin() || gespraechLaeuft { return aus(altKonto, altLimits, "", false, altGeholt) }
        do {
            let abruf: (String, [Roh])
            switch id {
            case "claude": abruf = try await claude(frisch)
            case "codex": abruf = try await codex()
            case "kimi": abruf = try await kimi(frisch)
            default: abruf = try await gemini()
            }
            let konto = abruf.0
            let neu = abruf.1
            // Fehlt nur ein Limit, bleibt dessen letzter Stand. Gemini liefert alles auf einmal: Dort ist ein fehlender Name umbenannt oder entfallen.
            let fehlen = id == "gemini" ? [] : altLimits.filter { a in !neu.contains { $0.name == a.name } }
            let alle = neu + fehlen
            let geholt = jetzt
            var o = gespeichert()
            o[id] = ["konto": konto, "geholt": geholt, "limits": alle.map { ["name": $0.name, "genutzt": $0.genutzt, "fenster": $0.fenster, "reset": $0.reset, "gelesen": $0.gelesen] as JSONObjekt }] as JSONObjekt
            UserDefaults.standard.set(JSON.text(o), forKey: speicher)
            let hinweis = !fehlen.isEmpty || neu.count < (id == "kimi" ? 2 : 1) ? "Ein Limit ließ sich gerade nicht lesen" + (fehlen.isEmpty ? "." : ", dort steht der letzte Stand.") : ""
            return aus(konto, alle, hinweis, false, geholt)
        } catch Fehler.nichtAngemeldet(let t) {
            log("guthaben", "Kontingent \(id): \(t)", wo: "Konsolen")
            return aus(altKonto, altLimits, altLimits.isEmpty ? t : "Letzter Stand. " + t, true, altGeholt)
        } catch {
            log("fehler", "Kontingent \(id) nicht abrufbar", wo: "Konsolen", fehler: error.localizedDescription)
            return aus(altKonto, altLimits, altLimits.isEmpty ? error.localizedDescription : "Letzter Stand. " + error.localizedDescription, false, altGeholt)
        }
    }

    static func vergessen(_ id: String) {
        var o = gespeichert()
        if o.removeValue(forKey: id) != nil { UserDefaults.standard.set(JSON.text(o), forKey: speicher) }
    }

    private static func claudeEndgueltig(_ s: String) -> Bool {
        guard let j = JSON.objekt(s) else { return false }
        let c = ganz(j, "s")
        return c == 200 || c == 401 || (c == 403 && (j["b"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines).hasPrefix("{"))
    }

    private static let claudeSkript = """
    (function(){ if(location.host!=='claude.ai') return null;
        if(window.__jk===undefined){ window.__jk=null; (async()=>{
            const fertig=(s,b)=>{ window.__jk=JSON.stringify({s:s,b:b}); if(s!==200&&s!==401) setTimeout(()=>{window.__jk=undefined},3000); };
            try {
                const a=await fetch('/api/organizations',{credentials:'include',headers:{Accept:'application/json'}});
                if(!a.ok){ fertig(a.status,(await a.text()).slice(0,200)); return; }
                const o=await a.json(); const org=o.find(x=>(x.capabilities||[]).includes('chat'))||o[0];
                const r=await fetch('/api/organizations/'+org.uuid+'/usage',{credentials:'include',headers:{Accept:'application/json'}});
                fertig(r.status,await r.text());
            } catch(e){ fertig(0,String(e)); } })(); }
        return window.__jk===undefined?null:window.__jk; })()
    """

    private static func zeitpunkt(_ text: String?) -> Int64 {
        guard let text, let d = ISO8601DateFormatter().date(from: text.replacingOccurrences(of: #"\.\d+"#, with: "", options: .regularExpression)) else { return 0 }
        return Int64(d.timeIntervalSince1970)
    }

    private static func claude(_ frisch: Bool) async throws -> (String, [Roh]) {
        guard WebSitzungen.angemeldet("claude") else { throw Fehler.nichtAngemeldet("Noch nicht bei claude.ai angemeldet.") }
        var antwort: JSONObjekt?
        // Zuerst die leichte Datei; lässt die Bot-Abwehr das nicht zu, die echte Seite, die deren Prüfung löst.
        for (adresse, frist) in [("https://claude.ai/robots.txt", 8.0), ("https://claude.ai/new", 30.0)] {
            let s = try await VersteckterLeser.lies("claude", url: adresse, js: claudeSkript, sekunden: frist, erzwungen: frisch, fertig: claudeEndgueltig)
            if let j = JSON.objekt(s) { antwort = j }
            if claudeEndgueltig(s) { break }
        }
        let code = antwort.map { ganz($0, "s") } ?? 0
        let text = antwort?["b"] as? String ?? ""
        if code == 401 || (code == 403 && text.trimmingCharacters(in: .whitespacesAndNewlines).hasPrefix("{")) { throw Fehler.nichtAngemeldet("claude.ai lehnt die Anmeldung ab (\(code)). Bitte neu anmelden.") }
        guard code == 200 else { throw Fehler.meldung(code == 0 ? "claude.ai antwortet gerade nicht." : "claude.ai antwortet gerade nicht (Fehler \(code)).") }
        guard let j = JSON.objekt(text) else { throw Fehler.meldung("claude.ai nennt keine Limits (Schnittstelle geändert?).") }
        var limits: [Roh] = []
        let felder: [(String, String, Int64)] = [("five_hour", "5-Stunden-Limit", stunden5), ("seven_day", "Wochenlimit", woche), ("seven_day_opus", "Wochenlimit Opus", woche), ("seven_day_sonnet", "Wochenlimit Sonnet", woche)]
        for (feld, name, fenster) in felder {
            guard let o = j[feld] as? JSONObjekt else { continue }
            limits.append(Roh(name: name, genutzt: zahl(o, "utilization"), fenster: fenster, reset: zeitpunkt(o["resets_at"] as? String), gelesen: jetzt))
        }
        guard !limits.isEmpty else { throw Fehler.meldung("claude.ai nennt keine Limits (Schnittstelle geändert?).") }
        return ("", limits)
    }

    private static func codex() async throws -> (String, [Roh]) {
        guard await CodexAnmeldung.shared.angemeldet else { throw Fehler.nichtAngemeldet("Noch nicht bei OpenAI angemeldet.") }
        var j: JSONObjekt?
        for versuch in 0..<2 where j == nil {
            let (token, konto) = try await CodexAnmeldung.shared.gueltigerToken()
            var req = URLRequest(url: URL(string: "https://chatgpt.com/backend-api/wham/usage")!, timeoutInterval: 25)
            req.setValue("application/json", forHTTPHeaderField: "Accept")
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            req.setValue("codex_cli_rs", forHTTPHeaderField: "originator")
            req.setValue("codex_cli_rs/\(OpenAIKlient.codexVersion) (JarvisMac)", forHTTPHeaderField: "User-Agent")
            req.setValue(konto, forHTTPHeaderField: "ChatGPT-Account-ID")
            let (daten, antwort) = try await URLSession.shared.data(for: req)
            let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
            if code == 401 || code == 403 {
                if versuch == 0 { await CodexAnmeldung.shared.ungueltigMelden(code); continue }
                throw Fehler.nichtAngemeldet("OpenAI lehnt die Anmeldung ab (\(code)). Bitte neu anmelden.")
            }
            guard code == 200 else { throw Fehler.meldung("OpenAI antwortet mit Fehler \(code).") }
            j = JSON.objekt(daten)
        }
        guard let j, let rl = j["rate_limit"] as? JSONObjekt else { throw Fehler.meldung("OpenAI nennt keine Limits (Schnittstelle geändert?).") }
        var limits: [Roh] = []
        for feld in ["primary_window", "secondary_window"] {
            guard let w = rl[feld] as? JSONObjekt else { continue }
            let fenster = (w["limit_window_seconds"] as? NSNumber)?.int64Value ?? woche
            let grund = fenster == woche ? "Wochenlimit" : (fenster == stunden5 ? "5-Stunden-Limit" : "Limit über \(fenster / 3600) Stunden")
            limits.append(Roh(name: limits.contains { $0.name == grund } ? grund + " (2)" : grund, genutzt: zahl(w, "used_percent"), fenster: fenster, reset: ganz(w, "reset_at"), gelesen: jetzt))
        }
        guard !limits.isEmpty else { throw Fehler.meldung("OpenAI nennt keine Limits (Schnittstelle geändert?).") }
        return ([j["email"] as? String, j["plan_type"] as? String].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "), limits)
    }

    private static let seitentext = "(function(){ return document.body ? (document.body.innerText || document.body.textContent || '') : null; })()"
    private static func muster(_ m: String, _ optionen: NSRegularExpression.Options = []) -> NSRegularExpression { try! NSRegularExpression(pattern: m, options: optionen) }
    private static let kimiGesamt = muster("Gesamtnutzung|Total usage|Overall usage", .caseInsensitive)
    private static let kimi5h = muster("Nutzung in 5 Stunden|(?<![\\d.,])5[- ]?(?:hours?|h)\\b", .caseInsensitive)
    private static let kimiAnmelden = muster("\\b(Anmelden|Einloggen|Log in|Sign in)\\b")
    private static let prozent = muster("(\\d+(?:[.,]\\d+)?)\\s*%")
    private static let kimiZeit = muster("(?:(\\d{4})-)?(\\d{1,2})-(\\d{1,2})\\s+(\\d{1,2}):(\\d{2})")

    private static func treffer(_ re: NSRegularExpression, _ t: NSString) -> [NSTextCheckingResult] { re.matches(in: t as String, range: NSRange(location: 0, length: t.length)) }

    /// „Gesamtnutzung 0.57% … Wird in 2026-11-28 19:08 zurückgesetzt“ und „Nutzung in 5 Stunden Code 6% Wird in 10-31 22:01 zurückgesetzt“.
    private static func kimiLesen(_ text: String) -> [Roh] {
        let t = text as NSString
        guard let g = treffer(kimiGesamt, t).last else { return [] }
        let stunden = treffer(kimi5h, t).first { $0.range.location > g.range.location }?.range.location ?? t.length
        let monat = kimiLimit(t.substring(with: NSRange(location: g.range.location, length: stunden - g.range.location)), "Monatliche Gesamtnutzung", 0)
        let fuenf = stunden < t.length ? kimiLimit(t.substring(from: stunden), "5-Stunden-Limit", stunden5) : nil
        return [fuenf, monat].compactMap { $0 }
    }

    static func kimiLesbar(_ text: String) -> Bool { !kimiLesen(text).isEmpty }

    private static func kimiLimit(_ teil: String, _ name: String, _ fenster: Int64) -> Roh? {
        let t = teil as NSString
        guard let p = treffer(prozent, t).first, let z = treffer(kimiZeit, t).first,
              let wert = Double(t.substring(with: p.range(at: 1)).replacingOccurrences(of: ",", with: ".")) else { return nil }
        func feld(_ i: Int) -> Int? { z.range(at: i).location == NSNotFound ? nil : Int(t.substring(with: z.range(at: i))) }
        let k = Calendar.current
        var teile = DateComponents(year: feld(1) ?? k.component(.year, from: Date()), month: feld(2), day: feld(3), hour: feld(4), minute: feld(5))
        guard var d = k.date(from: teile) else { return nil }
        if feld(1) == nil, d < k.startOfDay(for: Date()).addingTimeInterval(-86400) {
            teile.year = (teile.year ?? 0) + 1
            guard let spaeter = k.date(from: teile) else { return nil }
            d = spaeter
        }
        let ende = Int64(d.timeIntervalSince1970)
        // Der Monat ist unterschiedlich lang: Sein Fenster beginnt einen Kalendermonat vor dem Zurücksetzen.
        let vorher = k.date(byAdding: .month, value: -1, to: d) ?? d
        return Roh(name: name, genutzt: wert, fenster: fenster > 0 ? fenster : ende - Int64(vorher.timeIntervalSince1970), reset: ende, gelesen: jetzt)
    }

    private static func kimi(_ frisch: Bool) async throws -> (String, [Roh]) {
        guard WebSitzungen.angemeldet("kimi") else { throw Fehler.nichtAngemeldet("Noch nicht bei Kimi angemeldet.") }
        let beginn = Date()
        let text = try await VersteckterLeser.lies("kimi", url: kimiSeite, js: seitentext, sekunden: 30, erzwungen: frisch) { s in
            let l = kimiLesen(s)
            return l.count == 2 || (Date().timeIntervalSince(beginn) > 12 && l.isEmpty && !treffer(kimiAnmelden, s as NSString).isEmpty)
        }
        let limits = kimiLesen(text)
        if !limits.isEmpty { return ("", limits) }
        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { throw Fehler.meldung("Die Kimi-Seite hat nicht geladen.") }
        if !treffer(kimiAnmelden, text as NSString).isEmpty { throw Fehler.nichtAngemeldet("Kimi lehnt die Anmeldung ab. Bitte neu anmelden.") }
        throw Fehler.meldung("Die Kimi-Seite ließ sich nicht lesen (Seite geändert?).")
    }

    /// `agy -p /usage`: eine Zeile je Limit mit Tabulatoren: Gruppe, Art, Rest in Prozent, Zurücksetzen (UTC). Wie OpenLauncherServer/kern/agy_kontingent.py. Es zählt das Abo, nicht der Schlüssel.
    private static func gemini() async throws -> (String, [Roh]) {
        guard let pfad = agyPfad() else { throw Fehler.meldung("Die Antigravity-CLI ist auf diesem Mac nicht installiert.") }
        let r = await Shell.zsh("unset GEMINI_API_KEY; \(Shell.zitiert(pfad)) -p /usage </dev/null", sekunden: 25)
        let gruppen = ["Gemini Models": "Gemini-Modelle", "Claude and GPT models": "Claude und GPT"]
        var limits: [Roh] = []
        let sauber = r.ausgabe.replacingOccurrences(of: "\u{1B}\\[[0-9;?]*[A-Za-z]", with: "", options: .regularExpression)
        for zeile in sauber.components(separatedBy: "\n") {
            let t = zeile.components(separatedBy: "\t").map { $0.trimmingCharacters(in: .whitespaces) }
            guard t.count == 4, t[2].hasSuffix("%"), let rest = Double(t[2].dropLast()) else { continue }
            let reset = zeitpunkt(t[3])
            guard reset > 0 else { continue }
            let art = t[1].lowercased()
            let fuenf = art.range(of: "(five|5)[ -]?hour", options: .regularExpression) != nil
            // Eine unbekannte Art nicht als Woche ausgeben: Name und Fenster wären falsch.
            guard fuenf || art.contains("week") else { continue }
            limits.append(Roh(name: "\(gruppen[t[0]] ?? t[0]): \(fuenf ? "5-Stunden-Limit" : "Wochenlimit")", genutzt: ((100 - rest) * 10).rounded() / 10, fenster: fuenf ? stunden5 : woche, reset: reset, gelesen: jetzt))
        }
        if !limits.isEmpty { return ("Antigravity-CLI", limits) }
        let meldung = (r.ausgabe + r.fehler).trimmingCharacters(in: .whitespacesAndNewlines)
        let klein = meldung.lowercased()
        if klein.contains("login") || klein.contains("auth") || klein.contains("sign in") { throw Fehler.nichtAngemeldet("Die Antigravity-CLI ist nicht angemeldet.") }
        throw Fehler.meldung("agy nennt keine Limits (Code \(r.code)): \(meldung.prefix(200))")
    }
}

/// Das Google-Guthaben am Mac, wie am Handy in GoogleGuthabenFaehigkeit.kt: über die Schnittstelle von AI Studio mit Franks Google-Sitzung.
@MainActor
enum GoogleSitzung {
    enum Fehler: LocalizedError {
        case abgelaufen, meldung(String)
        var errorDescription: String? {
            switch self {
            case .abgelaufen: return "Die Google-Anmeldung am Mac ist abgelaufen. Bitte unter Einstellungen → Konsolen neu anmelden."
            case .meldung(let t): return t
            }
        }
    }

    static let loginURL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Faistudio.google.com%2Fbilling"
    private static let studio = "https://aistudio.google.com"
    private static let rpc = "https://alkalimakersuite-pa.clients6.google.com/$rpc/google.internal.alkali.applications.makersuite.v1.MakerSuiteService/"
    /// Öffentlicher Browser-Schlüssel der AI-Studio-Webseite (steht in deren Skript, kein Geheimnis von Frank).
    private static let studioSchluessel = "AIzaSyDdP816MREB3SkjZO04QXbjsigfcI0GWOs"
    private static let speicher = "google.mac.stand"

    static func vergessen() { UserDefaults.standard.removeObject(forKey: speicher) }

    private static func aufruf(_ methode: String, _ koerper: String) async throws -> [Any] {
        guard let sap = WebSitzungen.keks("google", "SAPISID"), let url = URL(string: rpc + methode) else { throw Fehler.abgelaufen }
        let ts = Int(Date().timeIntervalSince1970)
        let hash = Insecure.SHA1.hash(data: Data("\(ts) \(sap) \(studio)".utf8)).map { String(format: "%02x", $0) }.joined()
        var req = URLRequest(url: url, timeoutInterval: 25)
        req.httpMethod = "POST"
        req.httpShouldHandleCookies = false
        req.httpBody = Data(koerper.utf8)
        req.setValue("application/json+protobuf", forHTTPHeaderField: "Content-Type")
        req.setValue(WebSitzungen.keksKopf("google", rpc), forHTTPHeaderField: "Cookie")
        req.setValue("SAPISIDHASH \(ts)_\(hash)", forHTTPHeaderField: "Authorization")
        req.setValue(studioSchluessel, forHTTPHeaderField: "X-Goog-Api-Key")
        req.setValue("0", forHTTPHeaderField: "X-Goog-AuthUser")
        req.setValue(studio, forHTTPHeaderField: "Origin")
        req.setValue(studio + "/", forHTTPHeaderField: "Referer")
        req.setValue(ConsoleSitzung.browserKennung, forHTTPHeaderField: "User-Agent")
        let (daten, antwort) = try await URLSession.shared.data(for: req)
        let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
        if code == 401 || code == 403 { throw Fehler.abgelaufen }
        guard (200..<300).contains(code) else { throw Fehler.meldung("AI Studio antwortet mit Fehler \(code).") }
        guard let liste = JSON.wert(daten) as? [Any] else { throw Fehler.meldung("AI Studio lieferte keine lesbare Antwort (Schnittstelle geändert?).") }
        return liste
    }

    private static func l(_ a: [Any]?, _ i: Int) -> [Any]? { if let a, i < a.count { return a[i] as? [Any] }; return nil }
    private static func s(_ a: [Any]?, _ i: Int) -> String {
        guard let a, i < a.count else { return "" }
        return a[i] as? String ?? (a[i] as? NSNumber)?.stringValue ?? ""
    }
    private static func geld(_ a: [Any]?) -> Double {
        guard let a else { return 0 }
        return Double(Int64(s(a, 1)) ?? 0) + Double(Int64(s(a, 2)) ?? 0) / 1_000_000_000
    }
    private static func text(_ wert: Any) -> String { JSON.text(wert) }
    private static func zeit(_ d: Date, _ muster: String) -> String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.dateFormat = muster
        return f.string(from: d)
    }
    private static func datum(_ sek: Int64) -> String { zeit(Date(timeIntervalSince1970: Double(sek)), "dd.MM.yyyy") }
    private static func tageBis(_ d: Date) -> Int { Calendar.current.dateComponents([.day], from: Calendar.current.startOfDay(for: Date()), to: Calendar.current.startOfDay(for: d)).day ?? 0 }
    private static func tage(_ t: Int) -> String { t <= 0 ? "heute" : (t == 1 ? "morgen" : "in \(t) Tagen") }

    private static func vorauszahlung(_ kontoName: String) async throws -> Double {
        let ebp = s(try await aufruf("GetEmbeddedPortalParameters", text([kontoName, 1] as [Any])), 0)
        guard !ebp.isEmpty else { throw Fehler.meldung("AI Studio nennt keinen Zugang zur Zahlungsseite.") }
        var erlaubt = CharacterSet.alphanumerics
        erlaubt.insert(charactersIn: "-._~")
        let adresse = "https://payments.google.com/payments/u/0/embedded_landing_page?hl=de&ebp=" + (ebp.addingPercentEncoding(withAllowedCharacters: erlaubt) ?? ebp)
        guard let url = URL(string: adresse) else { throw Fehler.meldung("AI Studio nennt keinen Zugang zur Zahlungsseite.") }
        var req = URLRequest(url: url, timeoutInterval: 25)
        req.httpShouldHandleCookies = false
        req.setValue(WebSitzungen.keksKopf("google", adresse), forHTTPHeaderField: "Cookie")
        req.setValue(ConsoleSitzung.browserKennung, forHTTPHeaderField: "User-Agent")
        req.setValue(studio + "/", forHTTPHeaderField: "Referer")
        let (daten, antwort) = try await URLSession.shared.data(for: req)
        let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
        guard (200..<300).contains(code) else { throw Fehler.meldung("Die Zahlungsseite von Google antwortet mit Fehler \(code).") }
        let html = String(decoding: daten, as: UTF8.self) as NSString
        let re = try NSRegularExpression(pattern: "class=\"b3-balance-card-headline\"[^>]*>([^<]+)<")
        guard let m = re.firstMatch(in: html as String, range: NSRange(location: 0, length: html.length)) else { throw Fehler.meldung("Auf der Zahlungsseite steht keine Vorauszahlung (Seite geändert?).") }
        let roh = html.substring(with: m.range(at: 1)).replacingOccurrences(of: "&[^;]+;", with: "", options: .regularExpression)
        let z = String(roh.filter { $0.isNumber || $0 == "," || $0 == "." || $0 == "-" }).replacingOccurrences(of: ".", with: "").replacingOccurrences(of: ",", with: ".")
        guard let w = Double(z) else { throw Fehler.meldung("Die Vorauszahlung ist nicht lesbar.") }
        return w
    }

    private static func ausgaben() async throws -> (summe: Double, limit: Double?)? {
        var projekte: [String] = []
        func sammle(_ liste: [Any]?) {
            for p in liste?.compactMap({ $0 as? [Any] }) ?? [] where s(p, 0).hasPrefix("projects/") && !projekte.contains(s(p, 0)) { projekte.append(s(p, 0)) }
        }
        for (methode, feld) in [("ListCloudProjects", 0), ("ListImportedProjects", 4)] {
            do { sammle(l(try await aufruf(methode, "[]"), feld)) } catch Fehler.abgelaufen { throw Fehler.abgelaufen } catch {}
        }
        var summe = 0.0, limit = 0.0, gefunden = false
        for p in projekte {
            // Projekte ohne Abrechnung antworten mit 403: Das sagt hier nichts über die Anmeldung.
            guard let z = try? await aufruf("GetProjectUsageLimit", text([p] as [Any])), let ausgegeben = l(z, 2) else { continue }
            gefunden = true
            summe += geld(ausgegeben)
            limit += geld(l(z, 1))
        }
        return gefunden ? (summe, limit > 0.004 ? limit : nil) : nil
    }

    /// Derselbe Satz wie das Werkzeug des Handys. 15 Minuten gilt der letzte Stand, außer frisch.
    static func stand(frisch: Bool) async throws -> String {
        func zahl(_ o: JSONObjekt, _ k: String) -> Double? { (o[k] as? NSNumber)?.doubleValue }
        func ganz(_ o: JSONObjekt, _ k: String) -> Int64 { (o[k] as? NSNumber)?.int64Value ?? 0 }
        let alt = UserDefaults.standard.string(forKey: speicher).flatMap { JSON.objekt($0) } ?? [:]
        let jetztMs = Int64(Date().timeIntervalSince1970 * 1000)
        if !frisch, let gemerkt = alt["satz"] as? String, jetztMs - ganz(alt, "geholt") < 15 * 60_000 { return gemerkt }
        guard let konto = l(l(try await aufruf("ListBillingAccounts", "[]"), 0), 0) else { throw Fehler.meldung("Bei Google ist kein Rechnungskonto für AI Studio zu sehen.") }
        let kontoName = s(konto, 0)
        let w = s(konto, 2)
        let waehrung = w == "EUR" || w.isEmpty ? "€" : w
        let zeilen = l(try await aufruf("BatchGetCredits", text([[kontoName]] as [Any])), 0)?.compactMap { $0 as? [Any] } ?? []
        let jetzt = jetztMs / 1000
        let gutschriften = zeilen.map { z in (betrag: geld(l(z, 2)), verbraucht: geld(l(z, 3)), rest: geld(l(z, 4)), ende: Int64(s(l(z, 8), 0)) ?? 0) }.filter { $0.ende == 0 || $0.ende >= jetzt }
        var letzte: Int64 = 0
        var letzterBetrag: Double?
        do {
            for b in l(try await aufruf("GetGdpProfileAndBenefits", "[]"), 2)?.compactMap({ $0 as? [Any] }) ?? [] {
                if let beginn = Int64(s(l(b, 5), 0)), beginn > letzte { letzte = beginn; letzterBetrag = geld(l(b, 3)) }
            }
        } catch Fehler.abgelaufen { throw Fehler.abgelaufen } catch { log("guthaben", "Gewährungen nicht lesbar", wo: "Konsolen", fehler: error.localizedDescription) }
        let betragAlt = zahl(alt, "vorauszahlung")
        var vorher = zahl(alt, "vorher")
        var seit = ganz(alt, "seit")
        var gelesen = ganz(alt, "gelesen")
        var hinweis: String?
        var vz = betragAlt
        do {
            let neu = try await vorauszahlung(kontoName)
            if let betragAlt {
                if abs(neu - betragAlt) >= 0.004 { seit = jetztMs; vorher = betragAlt } else if seit <= 0 { seit = jetztMs }
            } else { seit = jetztMs; vorher = nil }
            vz = neu
            gelesen = jetztMs
        } catch Fehler.abgelaufen { throw Fehler.abgelaufen } catch { hinweis = "Vorauszahlung gerade nicht lesbar, das ist der letzte Betrag: " + error.localizedDescription }
        let monat = try await ausgaben()
        let ausgabenMonat = monat?.summe ?? zahl(alt, "ausgaben")
        let limitAusgaben = monat != nil ? monat?.limit : zahl(alt, "limit")

        let b = Guthaben.betrag
        var satz = "Google-Guthaben für die Gemini-Stimmen: übrig \(b(gutschriften.reduce(0) { $0 + $1.rest })) \(waehrung) von \(b(gutschriften.reduce(0) { $0 + $1.betrag })) \(waehrung), vom Guthaben abgebucht \(b(gutschriften.reduce(0) { $0 + $1.verbraucht })) \(waehrung)."
        if let ausgabenMonat {
            satz += " Ausgaben diesen Monat laut AI Studio: \(b(ausgabenMonat)) \(waehrung)"
            if let limitAusgaben { satz += " von \(b(limitAusgaben)) \(waehrung) Limit" }
            satz += "."
        }
        if let g = gutschriften.filter({ $0.rest > 0.004 }).sorted(by: { $0.ende < $1.ende }).first {
            satz += " Als Nächstes verfallen \(b(g.rest)) \(waehrung) am \(datum(g.ende)) (\(tage(tageBis(Date(timeIntervalSince1970: Double(g.ende))))))."
        }
        if letzte > 0, var d = Calendar.current.date(byAdding: .month, value: 1, to: Date(timeIntervalSince1970: Double(letzte))) {
            while d < Calendar.current.startOfDay(for: Date()), let n = Calendar.current.date(byAdding: .month, value: 1, to: d) { d = n }
            satz += " Nächstes Guthaben voraussichtlich am \(zeit(d, "dd.MM.yyyy")) (\(tage(tageBis(d))))"
            if let letzterBetrag { satz += ", etwa \(b(letzterBetrag)) \(waehrung)" }
            satz += "."
        }
        if let vz {
            satz += " Eingezahltes echtes Geld (Vorauszahlung): \(b(vz)) \(waehrung)"
            if gelesen < jetztMs { satz += ", letzter lesbarer Betrag, gerade nicht geprüft." }
            else if let vorher, vz < vorher - 0.004 { satz += ", gesunken von \(b(vorher)) \(waehrung) am \(datum(seit / 1000))." }
            else { satz += seit > 0 ? ", unverändert seit \(datum(seit / 1000))." : "." }
        }
        satz += " Die Vorauszahlung wird erst belastet, wenn das Guthaben aufgebraucht ist. Stand " + zeit(Date(), "dd.MM., HH:mm") + " Uhr."
        if let hinweis { satz += " " + hinweis }

        var neu: JSONObjekt = ["satz": satz, "geholt": jetztMs, "seit": seit, "gelesen": gelesen]
        if let vz { neu["vorauszahlung"] = vz }
        if let vorher { neu["vorher"] = vorher }
        if let ausgabenMonat { neu["ausgaben"] = ausgabenMonat }
        if let limitAusgaben { neu["limit"] = limitAusgaben }
        UserDefaults.standard.set(JSON.text(neu), forKey: speicher)
        return satz
    }
}

struct KonsolenDienst: Identifiable {
    let id: String
    let titel: String
    let hinweis: String
    let start: String
}

/// Anmeldefenster für Google, claude.ai und Kimi. Schließt sich, sobald die Sitzung steht; danach holt das `onDismiss` den Stand frisch.
struct KonsolenAnmeldungAnsicht: View {
    let dienst: KonsolenDienst
    @Environment(\.dismiss) private var schliessen
    @State private var griff = BrowserGriff()

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(dienst.titel).font(.jarvis(15, .semibold))
                    Text(dienst.hinweis).font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                }
                Spacer()
                Button("Fertig") {
                    Task {
                        if let web = griff.web { await WebSitzungen.erfassen(dienst.id, web, neu: true) }
                        schliessen()
                    }
                }
                Button("Abbrechen") { schliessen() }
            }
            .padding(14)
            ConsoleBrowser(fertig: { schliessen() }, start: URL(string: dienst.start), erkannt: { await erkannt($0) }, griff: griff)
        }
        .frame(width: 560, height: 740)
        .foregroundStyle(Farbe.text)
    }

    /// Fertig erkannt: gültige Sitzung gesichert, das Fenster schließt von selbst. Kimi nennt kein eindeutiges Cookie, dort zählen die lesbaren Limits.
    private func erkannt(_ web: WKWebView) async -> Bool {
        let id = dienst.id
        let da: Bool
        if id == "kimi" {
            let text = await webSkript(web, "(function(){ return document.body ? document.body.innerText : null; })()")
            da = text.map { KontingentQuellen.kimiLesbar($0) } ?? false
        } else {
            let host = web.url?.host ?? ""
            let seite = id == "google" ? host.hasSuffix("aistudio.google.com") : host == "claude.ai" && web.url?.path.contains("/login") == false
            let name = id == "google" ? "SAPISID" : "sessionKey"
            let cookies = await web.configuration.websiteDataStore.httpCookieStore.allCookies()
            da = seite && cookies.contains { $0.name == name && WebSitzungen.gehoertDazu(id, $0.domain) }
        }
        if da {
            await WebSitzungen.erfassen(id, web, neu: true)
            log("guthaben", "Am Mac angemeldet: \(id)", wo: "Konsolen")
        }
        return da
    }
}

/// Einstellungen → Konsolen: Anmeldung am Mac für alles, was unter Guthaben und Kontingente steht. Mit eigener Anmeldung liest Jarvis den Stand selbst, ohne sie kommt er vom Handy.
struct KonsolenAnsicht: View {
    @State private var g = Guthaben.shared
    @State private var k = Kontingente.shared
    @State private var offen: KonsolenDienst?
    @State private var codexDa = false
    @State private var codexCode = ""
    @State private var codexStatus = ""
    @State private var fuerZeichnen = 0   // Tresor und Abmelden melden sich nicht von selbst.
    @State private var abmeldenCodex = false

    private func stufe(_ hier: Bool, _ abgelaufen: Bool, _ handy: Bool) -> (String, Color) {
        if hier && abgelaufen { return ("Abgelaufen, bitte neu anmelden", Farbe.rot) }
        if hier { return ("Verbunden", Farbe.gruen) }
        if handy { return ("Über das Handy", Farbe.warnung) }
        return ("Nicht verbunden", Farbe.text3)
    }

    private func vomHandy(_ id: String) -> Bool { k.vomHandy.contains(id) && k.anbieter.contains { $0.id == id && !$0.limits.isEmpty } }
    private func abgelehnt(_ id: String) -> Bool { k.anbieter.contains { $0.id == id && $0.anmelden && !k.vomHandy.contains(id) } }

    private func karte<Knoepfe: View>(_ symbol: String, _ titel: String, _ text: String, _ stand: (String, Color), @ViewBuilder knoepfe: () -> Knoepfe) -> some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    KartenTitel(symbol: symbol, titel: titel)
                    Spacer()
                    Circle().fill(stand.1).frame(width: 8, height: 8)
                    Text(stand.0).font(.jarvis(12, .semibold)).foregroundStyle(stand.1)
                }
                Text(text).font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 10) { knoepfe() }
            }
        }
    }

    private func web(_ dienst: KonsolenDienst, _ titel: String, _ text: String) -> some View {
        let hier = WebSitzungen.angemeldet(dienst.id)
        return karte("gauge.with.dots.needle.67percent", titel, text, stufe(hier, abgelehnt(dienst.id), vomHandy(dienst.id))) {
            KapselKnopf(symbol: "link", titel: hier ? "Neu anmelden" : "Anmelden", hervorgehoben: !hier) { offen = dienst }
            if hier {
                KapselKnopf(symbol: "xmark", titel: "Abmelden") {
                    WebSitzungen.abmelden(dienst.id)
                    KontingentQuellen.vergessen(dienst.id)
                    fuerZeichnen += 1
                    Task { await k.laden(frisch: true) }
                }
            }
        }
    }

    var body: some View {
        let _ = fuerZeichnen
        let anthropicHier = ConsoleSitzung.angemeldet
        let googleHier = WebSitzungen.angemeldet("google")
        let agyDa = KontingentQuellen.agyPfad() != nil
        VStack(alignment: .leading, spacing: 18) {
            Text("Melde dich hier am Mac an, dann liest Jarvis Guthaben und Kontingente direkt und schnell. Ohne eigene Anmeldung kommt der Stand vom Handy. Die Anmeldungen liegen verschlüsselt im Tresor.")
                .font(.jarvis(13)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)

            Text("Guthaben").font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text3)
            karte("brain", "Anthropic-Konsole", "Guthaben und Kosten der Claude-Modelle.", stufe(anthropicHier, g.macAbgelaufen, g.anthropicRest != nil)) {
                KapselKnopf(symbol: "link", titel: anthropicHier ? "Neu anmelden" : "Anmelden", hervorgehoben: !anthropicHier) { g.anmeldungOffen = true }
                if anthropicHier {
                    KapselKnopf(symbol: "xmark", titel: "Abmelden") {
                        log("guthaben", "Am Mac bei der Console abgemeldet", wo: "Konsolen")
                        g.abmelden()
                        Task { await g.laden(frisch: true) }
                    }
                }
            }
            karte("waveform", "Google AI Studio", "Guthaben der Gemini-Stimmen und des Live-Gesprächs.", stufe(googleHier, g.googleAbgelaufen, g.googleRest != nil)) {
                KapselKnopf(symbol: "link", titel: googleHier ? "Neu anmelden" : "Anmelden", hervorgehoben: !googleHier) {
                    offen = KonsolenDienst(id: "google", titel: "Bei Google anmelden", hinweis: "Google lässt die Anmeldung in eingebetteten Browsern nicht immer zu. Dann bleibt der Stand vom Handy.", start: GoogleSitzung.loginURL)
                }
                if googleHier {
                    KapselKnopf(symbol: "xmark", titel: "Abmelden") {
                        WebSitzungen.abmelden("google")
                        GoogleSitzung.vergessen()
                        fuerZeichnen += 1
                        Task { await g.laden(frisch: true) }
                    }
                }
            }

            Text("Kontingente").font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text3)
            web(KonsolenDienst(id: "claude", titel: "Bei Claude anmelden", hinweis: "Am besten per E-Mail-Code. Das Fenster schließt sich danach von selbst.", start: KontingentQuellen.claudeLogin), "Claude", "Limits des Claude-Abos von claude.ai.")
            karte("gauge.with.dots.needle.67percent", "ChatGPT / Codex", "Limits des ChatGPT-Abos. Dieselbe Anmeldung wie für die GPT-Modelle.", stufe(codexDa, abgelehnt("codex"), vomHandy("codex"))) {
                KapselKnopf(symbol: "link", titel: codexDa ? "Neu anmelden" : "Anmelden", hervorgehoben: !codexDa) { codexAnmelden() }
                if codexDa { KapselKnopf(symbol: "xmark", titel: "Abmelden") { abmeldenCodex = true } }
            }
            if !codexCode.isEmpty { Text(codexCode).font(.jarvis(24, .bold)).foregroundStyle(Farbe.haupt).textSelection(.enabled) }
            if !codexStatus.isEmpty { Text(codexStatus).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
            web(KonsolenDienst(id: "kimi", titel: "Bei Kimi anmelden", hinweis: "Anmelden, dann Fertig. Sobald die Limits sichtbar sind, schließt sich das Fenster von selbst.", start: KontingentQuellen.kimiSeite), "Kimi", "Limits des Kimi-Abos von kimi.ai.")
            karte("gauge.with.dots.needle.67percent", "Gemini / Antigravity",
                  agyDa ? "Limits der Antigravity-CLI. Sie liest diesen Mac direkt, die Anmeldung liegt in der CLI selbst." : "Limits der Antigravity-CLI. Sie ist hier nicht installiert, darum kommt der Stand vom Handy.",
                  stufe(agyDa, abgelehnt("gemini"), vomHandy("gemini"))) {
                if agyDa, let pfad = KontingentQuellen.agyPfad() {
                    KapselKnopf(symbol: "link", titel: "Im Terminal anmelden") {
                        let esc = Shell.zitiert(pfad).replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\"")
                        Task { _ = await Shell.osascript("tell application \"Terminal\"\nactivate\ndo script \"\(esc)\"\nend tell") }
                    }
                }
            }
        }
        .task {
            codexDa = await CodexAnmeldung.shared.angemeldet
            async let a: Void = g.laden()
            async let b: Void = k.laden()
            _ = await (a, b)
        }
        .sheet(item: $offen, onDismiss: {
            fuerZeichnen += 1
            Task {
                async let a: Void = g.laden(frisch: true)
                async let b: Void = k.laden(frisch: true)
                _ = await (a, b)
            }
        }) { KonsolenAnmeldungAnsicht(dienst: $0) }
        .confirmationDialog("Bei ChatGPT abmelden?", isPresented: $abmeldenCodex) {
            Button("Abmelden", role: .destructive) {
                Task {
                    await CodexAnmeldung.shared.abmelden()
                    KontingentQuellen.vergessen("codex")
                    codexDa = false
                    codexStatus = "Abgemeldet."
                    await k.laden(frisch: true)
                }
            }
        } message: { Text("Das gilt auch für die GPT-Modelle von Jarvis auf diesem Mac.") }
    }

    private func codexAnmelden() {
        Task {
            do {
                let (code, url) = try await CodexAnmeldung.shared.start()
                codexCode = code
                NSPasteboard.general.clearContents(); NSPasteboard.general.setString(code, forType: .string)
                codexStatus = "Code kopiert – im Browser einfügen und freigeben."
                NSWorkspace.shared.open(url)
                let email = try await CodexAnmeldung.shared.warteAufFreigabe()
                codexCode = ""
                codexDa = true
                codexStatus = "Angemeldet als \(email ?? "ChatGPT-Konto")."
                await k.laden(frisch: true)
            } catch {
                codexCode = ""
                codexStatus = "Anmeldung fehlgeschlagen: \(error.localizedDescription)"
            }
        }
    }
}
