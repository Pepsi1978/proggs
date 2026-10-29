import Foundation

/// Gemini Live mit „Jarvis direkt“ – 1:1 aus der Handy-App (Jarvis/live/JarvisWeiche.kt) übernommen.
/// Die App, nicht das Sprachmodell, sorgt dafür, dass jeder abgeschlossene Beitrag von Frank an Jarvis geht und Gemini
/// nur Jarvis' Antworten spricht (Gemini 3.8 Live hat Proactive Audio fest an und kennt keinen erzwungenen Werkzeugaufruf).
/// Reine Logik; alle Aufrufe unter einer Sperre, die zurückgegebenen Aktionen führt das Live-Gespräch danach aus.
final class JarvisWeiche: @unchecked Sendable {
    enum Aktion {
        case nutzerFertig
        case anJarvis(kennung: String, auftrag: String, hinweis: String)
        case werkzeugantwort(ids: [String], text: String)
        case einspielen(String)
        case anzeigen(String)
        case signal(ton: [Data], text: String)
        case fehler(String)
        case protokoll(String)
    }

    private enum Tor { case begruessung, zu, wartet, offen }

    static let marke = "[JARVIS-ANTWORT] "
    static let mitschriftFehlt = "(Keine Mitschrift von Franks Worten; sinngemäß vom Sprachmodell:) "
    static let nachlaufMs: Int64 = 350
    static let mitschriftWartenMs: Int64 = 1_500
    static let wartenMaxMs: Int64 = 1_500
    static let stummMaxMs: Int64 = 10_000
    static let jarvisMaxMs: Int64 = 150_000
    static let echoMax = 2
    static let ohneMitschriftMax = 2
    static let signalWorte: Set<String> = ["mhm", "mm", "hm", "hmm", "okay", "ok", "oke", "moment", "einen", "kleinen", "augenblick", "sekunde",
        "ich", "schau", "schaue", "seh", "sehe", "mal", "kurz", "nach", "lass", "mich", "nachsehen", "nachschauen", "verstehe", "aha", "ah", "oh", "so"]
    static let signalMaxWorte = 6
    static let signalMaxBytes = 144_000
    static let signalNachlaufMs: Int64 = 400
    static let signalMaxMs: Int64 = 6_000

    private let ruheMs: Int64
    private let lock = NSRecursiveLock()

    private var tor = Tor.begruessung
    var tonFrei: Bool { sync { tor == .begruessung || tor == .offen } }

    private struct Uebergabe { let kennung: String; let seitMs: Int64 }
    private struct OhneMitschrift { let auftrag: String; let seitMs: Int64; let begruessung: Bool }
    private final class Gepuffert {
        let seitMs: Int64
        var ton: [Data] = []
        var bytes = 0
        var text = ""
        var endeMs: Int64 = 0
        init(_ s: Int64) { seitMs = s }
    }

    private var beitrag = ""
    private var letzteMitschriftMs: Int64 = 0
    private var letzteSpracheMs: Int64 = 0
    private var abschlussFaellig = false
    private var ersatzAuftrag = ""
    private var ohneMitschrift: OhneMitschrift?
    private var erzeugt = false
    private var offeneAufrufe: [String] = []
    private var laufend: Uebergabe?
    private var wartend: String?
    private var ungehoert: String?
    private var unterbrochenFlag = false
    private var letzteAntwort = ""
    private var echos = 0
    private var nutzerSeitAbgabe = false
    private var abgabeMs: Int64 = 0
    private var wartetSeitMs: Int64 = 0
    private var tonSeitOffen = false
    private var gepuffert: Gepuffert?
    private var signalGenutzt = false
    private var letztesSignal = ""
    private var ohneMitschriftFolge = 0
    private var nr = 0

    init(ruheMs: Int64) { self.ruheMs = ruheMs }

    private func sync<T>(_ f: () -> T) -> T { lock.lock(); defer { lock.unlock() }; return f() }

    var arbeitet: Bool { sync { laufend != nil || wartend != nil } }

    func nutzerText(_ text: String, _ jetzt: Int64) -> [Aktion] { sync {
        if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return pruefen(jetzt) }
        beitrag += text
        letzteMitschriftMs = jetzt; letzteSpracheMs = jetzt
        nutzerSeitAbgabe = true
        ohneMitschriftFolge = 0
        if tor == .begruessung { tor = .zu }
        let aus = signalVerwerfenWennNeu(jetzt)
        if (tor == .offen || tor == .wartet) && !tonSeitOffen {
            tor = .zu
            ungehoert = ungehoert.map { "\($0)\n\(letzteAntwort)" } ?? letzteAntwort
        }
        return aus + pruefen(jetzt)
    } }

    func nutzerSpricht(_ jetzt: Int64) -> [Aktion] { sync { letzteSpracheMs = jetzt; return signalVerwerfenWennNeu(jetzt) } }

    private func signalVerwerfenWennNeu(_ jetzt: Int64) -> [Aktion] {
        guard let g = gepuffert else { return [] }
        if jetzt - g.seitMs <= Self.nachlaufMs { return [] }
        return signalVerwerfen("Frank spricht weiter", verbraucht: false)
    }

    private func signalVerwerfen(_ grund: String, verbraucht: Bool = true) -> [Aktion] {
        guard let g = gepuffert else { return [] }
        gepuffert = nil
        if verbraucht { signalGenutzt = true }
        return [.protokoll("Weiche: eigene Äußerung von Gemini stumm (\(grund)): „\(g.text.trimmingCharacters(in: .whitespaces).prefix(80))“")]
    }

    func eigenerTon(_ pcm: Data, _ jetzt: Int64) { sync {
        guard let g = puffer(jetzt), g.endeMs == 0 else { return }
        g.bytes += pcm.count
        if g.bytes > Self.signalMaxBytes { g.ton.removeAll() } else { g.ton.append(pcm) }
    } }

    func eigeneMitschrift(_ text: String, _ jetzt: Int64) { sync { puffer(jetzt)?.text += text } }

    private func puffer(_ jetzt: Int64) -> Gepuffert? {
        if let g = gepuffert { return g }
        if tor != .zu || signalGenutzt || (beitrag.isEmpty && !(laufend != nil || wartend != nil)) { return nil }
        let g = Gepuffert(jetzt); gepuffert = g; return g
    }

    func modellAusgabe(_ jetzt: Int64, ton: Bool = true) -> [Aktion] { sync {
        erzeugt = true
        var aus: [Aktion] = []
        if ton && tor == .offen && !tonSeitOffen {
            tonSeitOffen = true
            aus.append(.protokoll("Weiche: Gemini spricht Jarvis' Antwort, \(jetzt - abgabeMs) ms nach der Abgabe"))
        }
        if tor == .zu && !beitrag.isEmpty { abschlussFaellig = true }
        return aus + pruefen(jetzt)
    } }

    func werkzeugAufruf(_ id: String, _ auftrag: String, _ jetzt: Int64) -> [Aktion] { sync {
        erzeugt = false
        var aus: [Aktion] = []
        if let g = gepuffert, g.endeMs == 0 { g.endeMs = jetzt }
        if !beitrag.isEmpty {
            offeneAufrufe.append(id); abschlussFaellig = true; ersatzAuftrag = auftrag
        } else if laufend != nil || wartend != nil {
            offeneAufrufe.append(id)
        } else if (tor == .offen || tor == .wartet) && !nutzerSeitAbgabe && !letzteAntwort.isEmpty {
            echos += 1
            if echos <= Self.echoMax {
                aus.append(.protokoll("Weiche: Gemini fragt Jarvis erneut ohne neuen Beitrag (\(echos)), Antwort aus dem Speicher"))
                aus.append(.werkzeugantwort(ids: [id], text: letzteAntwort))
                tor = .offen
            } else {
                aus.append(.werkzeugantwort(ids: [id], text: "Das ist bereits Jarvis' Antwort. Sag jetzt nichts mehr."))
                aus.append(.anzeigen(letzteAntwort))
                aus.append(.fehler("Gemini hat Jarvis' Antwort nicht vorgelesen; sie steht im Chat."))
                tor = .zu
            }
        } else {
            offeneAufrufe.append(id)
            ohneMitschrift = OhneMitschrift(auftrag: auftrag, seitMs: jetzt, begruessung: tor == .begruessung)
        }
        return aus + pruefen(jetzt)
    } }

    func werkzeugAbgebrochen(_ ids: [String]) { sync { offeneAufrufe.removeAll { ids.contains($0) } } }

    func verbindungNeu() { sync { offeneAufrufe.removeAll(); ohneMitschrift = nil } }

    func zugEnde(_ jetzt: Int64) -> [Aktion] { sync {
        erzeugt = false
        var aus: [Aktion] = []
        switch tor {
        case .begruessung: tor = .zu
        case .wartet: tor = .offen; abgabeMs = jetzt
        case .offen: if tonSeitOffen { tor = .zu; aus.append(.protokoll("Weiche: Jarvis' Antwort fertig gesprochen")) }
        case .zu:
            if !beitrag.isEmpty { abschlussFaellig = true }
            if let g = gepuffert, g.endeMs == 0 { g.endeMs = jetzt }
        }
        return aus + pruefen(jetzt)
    } }

    func unterbrochen(_ jetzt: Int64) -> [Aktion] { sync {
        erzeugt = false
        let aus = signalVerwerfen("unterbrochen", verbraucht: false)
        switch tor {
        case .begruessung: tor = .zu
        case .wartet: tor = .offen; abgabeMs = jetzt
        case .offen: if tonSeitOffen { tor = .zu; unterbrochenFlag = true }
        case .zu: break
        }
        return aus + pruefen(jetzt)
    } }

    func angetippt() { sync { if tor == .offen && tonSeitOffen { unterbrochenFlag = true } } }

    func jarvisAntwort(_ kennung: String, _ text: String, _ jetzt: Int64) -> [Aktion] { sync {
        guard let l = laufend, l.kennung == kennung else { return [.protokoll("Weiche: verspätete Antwort zu \(kennung) verworfen")] }
        laufend = nil
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let antwort = t.isEmpty ? "Jarvis hat keine Antwort geliefert." : t
        var aus: [Aktion] = [.protokoll("Weiche: Antwort von Jarvis nach \(jetzt - l.seitMs) ms, \(antwort.count) Zeichen")]
        if wartend != nil || !beitrag.isEmpty {
            ungehoert = ungehoert.map { "\($0)\n\(antwort)" } ?? antwort
            aus.append(.protokoll("Weiche: Frank sprach weiter, Antwort geht mit dem nächsten Beitrag an Jarvis"))
        } else {
            aus += abgeben(antwort, jetzt)
        }
        return aus + pruefen(jetzt)
    } }

    /// Mac-Ergänzung: ein spätes Ergebnis (nach dem Zwischenstand) jetzt sprechen lassen.
    func nachreichen(_ text: String, _ jetzt: Int64) -> [Aktion] { sync { abgeben(text, jetzt) + pruefen(jetzt) } }

    func takt(_ jetzt: Int64) -> [Aktion] { sync { pruefen(jetzt) } }

    private func pruefen(_ jetzt: Int64) -> [Aktion] {
        var aus: [Aktion] = []
        if tor == .wartet && jetzt - wartetSeitMs >= Self.wartenMaxMs { tor = .offen; abgabeMs = jetzt }
        if tor == .offen && !tonSeitOffen && abgabeMs > 0 && !letzteAntwort.isEmpty && jetzt - abgabeMs >= Self.stummMaxMs {
            aus.append(.anzeigen(letzteAntwort))
            aus.append(.fehler("Gemini hat Jarvis' Antwort nicht vorgelesen; sie steht im Chat."))
            tor = .zu
        }
        if let l = laufend, jetzt - l.seitMs >= Self.jarvisMaxMs {
            aus += jarvisAntwort(l.kennung, "Jarvis hat nicht rechtzeitig geantwortet. Ich kann dazu gerade nichts sagen; frag bitte gleich noch einmal.", jetzt)
        }
        if let o = ohneMitschrift {
            if !beitrag.isEmpty { ohneMitschrift = nil; abschlussFaellig = true }
            else if jetzt - o.seitMs >= Self.mitschriftWartenMs {
                ohneMitschrift = nil
                if o.begruessung || o.auftrag.trimmingCharacters(in: .whitespaces).isEmpty || ohneMitschriftFolge >= Self.ohneMitschriftMax {
                    if ohneMitschriftFolge >= Self.ohneMitschriftMax && !o.begruessung {
                        aus.append(.fehler("Gemini liefert keine Mitschrift deiner Worte; bitte das Live-Gespräch neu starten."))
                    }
                    aus.append(.werkzeugantwort(ids: offeneAufrufe, text: "Frank hat noch nichts gesagt. Sag nichts weiter."))
                    offeneAufrufe.removeAll()
                } else {
                    ohneMitschriftFolge += 1
                    beitrag += o.auftrag
                    ersatzAuftrag = Self.mitschriftFehlt
                    abschlussFaellig = true
                    letzteMitschriftMs = 0
                }
            }
        }
        let ruhig = jetzt - max(letzteMitschriftMs, letzteSpracheMs)
        if !beitrag.isEmpty && (abschlussFaellig || ruhig >= ruheMs) && jetzt - letzteMitschriftMs >= Self.nachlaufMs {
            let text = beitrag.trimmingCharacters(in: .whitespacesAndNewlines)
            let ersatz = ersatzAuftrag
            beitrag = ""; abschlussFaellig = false; ersatzAuftrag = ""
            aus.append(.nutzerFertig)
            aus.append(.protokoll("Weiche: Beitrag abgeschlossen (\(text.count) Zeichen)"))
            let worte = ersatz == Self.mitschriftFehlt ? Self.mitschriftFehlt + text : text
            wartend = wartend.map { "\($0)\n\(worte)" } ?? worte
        }
        if laufend == nil, let w = wartend {
            wartend = nil
            aus.append(uebergeben(w, jetzt))
        }
        aus += signalPruefen(jetzt)
        return aus
    }

    private func signalPruefen(_ jetzt: Int64) -> [Aktion] {
        guard let g = gepuffert else { return [] }
        if g.endeMs == 0 { return jetzt - g.seitMs >= Self.signalMaxMs ? signalVerwerfen("kein Ende") : [] }
        if jetzt - g.endeMs < Self.signalNachlaufMs { return [] }
        let text = g.text.trimmingCharacters(in: .whitespacesAndNewlines)
        let grund: String?
        if g.bytes > Self.signalMaxBytes { grund = "zu lang" }
        else if !Self.neutralesSignal(text) { grund = "kein neutrales Signal" }
        else if Self.signalSchluessel(text) == letztesSignal { grund = "wiederholt" }
        else if g.ton.isEmpty { grund = "kein Ton" }
        else if tor != .zu || (!(laufend != nil || wartend != nil) && beitrag.isEmpty) { grund = "nichts zu überbrücken" }
        else { grund = nil }
        if let grund { return signalVerwerfen(grund) }
        gepuffert = nil
        signalGenutzt = true
        letztesSignal = Self.signalSchluessel(text)
        return [.signal(ton: g.ton, text: text), .protokoll("Weiche: neutrales Signal von Gemini hörbar: „\(text)“")]
    }

    private func uebergeben(_ worte: String, _ jetzt: Int64) -> Aktion {
        nr += 1
        let kennung = "weiche-\(nr)"
        var hinweis = ""
        if unterbrochenFlag { hinweis += "(Frank hat deine vorige gesprochene Antwort unterbrochen.)" }
        if let u = ungehoert {
            hinweis += (hinweis.isEmpty ? "" : "\n") + "(Deine vorige Antwort hat Frank nicht gehört, weil er weitersprach: „\(u)“. Beziehe sie ein, soweit sie noch passt, vor allem Bestätigungen erledigter Aktionen.)"
        }
        unterbrochenFlag = false
        ungehoert = nil
        laufend = Uebergabe(kennung: kennung, seitMs: jetzt)
        return .anJarvis(kennung: kennung, auftrag: worte, hinweis: hinweis)
    }

    private func abgeben(_ antwort: String, _ jetzt: Int64) -> [Aktion] {
        let aus = signalVerwerfen("Antwort von Jarvis ist da", verbraucht: false)
        signalGenutzt = false
        letzteAntwort = antwort
        echos = 0
        nutzerSeitAbgabe = false
        tonSeitOffen = false
        abgabeMs = jetzt
        if !offeneAufrufe.isEmpty {
            let ids = offeneAufrufe
            offeneAufrufe.removeAll()
            tor = .offen
            return aus + [.werkzeugantwort(ids: ids, text: antwort)]
        }
        if erzeugt { tor = .wartet; wartetSeitMs = jetzt } else { tor = .offen }
        return aus + [.einspielen(Self.marke + antwort)]
    }

    static func neutralesSignal(_ text: String) -> Bool {
        let t = text.lowercased()
        let satzzeichen = Set(".,!?…-–—'’\"„“‚‘")
        if t.contains(where: { !$0.isLetter && !$0.isWhitespace && !satzzeichen.contains($0) }) { return false }
        var worte: [String] = []
        let regex = try! NSRegularExpression(pattern: "alles\\s+klar|\\p{L}+")
        for m in regex.matches(in: t, range: NSRange(t.startIndex..., in: t)) {
            if let r = Range(m.range, in: t) { worte.append(String(t[r])) }
        }
        return !worte.isEmpty && worte.count <= signalMaxWorte && worte.allSatisfy {
            signalWorte.contains($0) || $0.split(whereSeparator: { $0.isWhitespace }).map(String.init) == ["alles", "klar"]
        }
    }

    private static func signalSchluessel(_ text: String) -> String { String(text.lowercased().filter { $0.isLetter }) }
}
