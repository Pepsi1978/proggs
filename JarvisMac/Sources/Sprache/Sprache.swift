import AppKit
import AVFoundation
import Foundation
import Observation

// SCHNITTSTELLE (fest): Oberfläche und Live nutzen genau diese Typen und Mitglieder.

// MARK: - Stimme (Vorlesen)

/// Sprachausgabe: Google Cloud TTS (Chirp 3 HD, deutsch) satzweise und lückenlos, Rückfall auf die beste
/// deutsche Systemstimme. Während des Sprechens ruht das Weckwort (kein Selbstauslösen).
@MainActor @Observable final class Stimme: NSObject, AVSpeechSynthesizerDelegate {
    static let shared = Stimme()
    private(set) var spricht = false
    private(set) var pegel: Float = 0          // 0…1 für die Animation

    private var generation = 0
    private var engine: AVAudioEngine?
    private var spieler: AVAudioPlayerNode?
    private let format = AVAudioFormat(standardFormatWithSampleRate: 24000, channels: 1)!
    private var geplant = 0
    private var gespielt = 0
    private let system = AVSpeechSynthesizer()
    private var systemFertig: CheckedContinuation<Void, Never>?
    private var pegelUhr: Timer?

    private static let chirpNamen = [
        "Achernar", "Aoede", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux", "Kore", "Laomedeia", "Leda",
        "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr",
        "Achird", "Algenib", "Algieba", "Alnilam", "Charon", "Enceladus", "Fenrir", "Iapetus", "Orus", "Puck",
        "Rasalgethi", "Sadachbia", "Sadaltager", "Schedar", "Umbriel", "Zubenelgenubi",
    ]
    private static let weiblich: Set<String> = ["Achernar", "Aoede", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux",
                                                "Kore", "Laomedeia", "Leda", "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr"]

    /// Verfügbare Stimmen (Kennung, Anzeigename) für die Einstellungen.
    var stimmen: [(id: String, name: String)] {
        Self.chirpNamen.map { ("de-DE-Chirp3-HD-\($0)", "\($0) (\(Self.weiblich.contains($0) ? "weiblich" : "männlich"), Chirp 3 HD)") }
            + [("system", "Systemstimme (offline)")]
    }

    override init() {
        super.init()
        system.delegate = self
    }

    /// Liest den Text vor und kehrt zurück, wenn fertig oder gestoppt.
    func sprich(_ text: String) async {
        stopp()
        generation += 1
        let meine = generation
        let sauber = SprechText.fuerStimme(text)
        guard !sauber.isEmpty else { return }
        Weckwort.shared.pausieren()
        defer { Weckwort.shared.fortsetzen() }
        spricht = true
        defer { if generation == meine { spricht = false; pegel = 0 } }

        let e = Einstellungen.shared
        let schluessel = e.zugang.google
        let stimme = e.stimme
        let stuecke = SprechText.stuecke(sauber)
        if !schluessel.isEmpty && stimme.contains("Chirp") {
            let rest = await sprichGoogle(stuecke, stimme: stimme, schluessel: schluessel, tempo: e.tempo, generation: meine)
            guard generation == meine else { return }
            if rest.isEmpty { return }
            await sprichSystem(rest.joined(separator: " "), tempo: e.tempo, generation: meine)
        } else {
            await sprichSystem(sauber, tempo: e.tempo, generation: meine)
        }
    }

    func stopp() {
        generation += 1
        spieler?.stop()
        if system.isSpeaking { system.stopSpeaking(at: .immediate) }
        let k = systemFertig; systemFertig = nil; k?.resume()
        pegelUhr?.invalidate(); pegelUhr = nil
        spricht = false
        pegel = 0
    }

    // MARK: Google Cloud TTS

    private func bereiteEngine() throws -> AVAudioPlayerNode {
        if let spieler, let engine, engine.isRunning { return spieler }
        let e = AVAudioEngine()
        let s = AVAudioPlayerNode()
        e.attach(s)
        e.connect(s, to: e.mainMixerNode, format: format)
        e.mainMixerNode.installTap(onBus: 0, bufferSize: 1024, format: nil) { [weak self] p, _ in
            let wert = min(1, Mikrofon.pegel(p) * 6)
            Task { @MainActor in
                guard let self, self.spricht else { return }
                self.pegel = self.pegel * 0.5 + wert * 0.5
            }
        }
        e.prepare()
        try e.start()
        engine = e; spieler = s
        return s
    }

    /// Gibt die Stücke zurück, die nicht über Google gesprochen werden konnten (für den Rückfall).
    private func sprichGoogle(_ stuecke: [String], stimme: String, schluessel: String, tempo: Double, generation meine: Int) async -> [String] {
        let s: AVAudioPlayerNode
        do { s = try bereiteEngine() } catch {
            log("fehler", "Vorlesen: Audioausgabe nicht startbar (\(error.localizedDescription))")
            return stuecke
        }
        geplant = 0; gespielt = 0
        var aufgaben: [Int: Task<AVAudioPCMBuffer?, Never>] = [:]
        func sichere(_ i: Int) {
            guard i < stuecke.count, aufgaben[i] == nil else { return }
            let text = stuecke[i]
            let fmt = format
            aufgaben[i] = Task.detached {
                await Self.synthese(text, stimme: stimme, schluessel: schluessel, tempo: tempo, format: fmt)
            }
        }
        var rest: [String] = []
        for i in 0..<stuecke.count {
            sichere(i); sichere(i + 1); sichere(i + 2)
            let puffer = await aufgaben[i]?.value
            guard generation == meine else { aufgaben.values.forEach { $0.cancel() }; return [] }
            guard let puffer else {
                log("fehler", "Vorlesen: Google-TTS fehlgeschlagen – Rückfall auf Systemstimme")
                rest = Array(stuecke[i...])
                aufgaben.values.forEach { $0.cancel() }
                break
            }
            geplant += 1
            s.scheduleBuffer(puffer, completionCallbackType: .dataPlayedBack) { [weak self] _ in
                // Nur Rückrufe der eigenen Wiedergabe zählen – stopp() löst die alten nachträglich aus.
                Task { @MainActor in if self?.generation == meine { self?.gespielt += 1 } }
            }
            if !s.isPlaying { s.play() }
        }
        // Warten, bis alles geplante gespielt ist.
        while generation == meine && gespielt < geplant {
            try? await Task.sleep(for: .milliseconds(40))
        }
        return generation == meine ? rest : []
    }

    nonisolated private static func synthese(_ text: String, stimme: String, schluessel: String, tempo: Double, format: AVAudioFormat) async -> AVAudioPCMBuffer? {
        var komp = URLComponents(string: "https://texttospeech.googleapis.com/v1/text:synthesize")!
        komp.queryItems = [URLQueryItem(name: "key", value: schluessel)]
        var req = URLRequest(url: komp.url!, timeoutInterval: 20)
        req.httpMethod = "POST"
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        req.httpBody = JSON.daten([
            "input": ["text": text],
            "voice": ["languageCode": "de-DE", "name": stimme],
            "audioConfig": ["audioEncoding": "LINEAR16", "sampleRateHertz": 24000, "speakingRate": min(max(tempo, 0.7), 1.3)],
        ] as JSONObjekt)
        for versuch in 0..<2 {
            if Task.isCancelled { return nil }
            do {
                let (daten, antwort) = try await URLSession.shared.data(for: req)
                let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
                guard code == 200, let o = JSON.objekt(daten), let b64 = o["audioContent"] as? String,
                      let wav = Data(base64Encoded: b64), let (samples, _) = Wav.lies(wav) else {
                    log("fehler", "Google-TTS \(code): \(String(decoding: daten.prefix(200), as: UTF8.self))")
                    if code >= 500 && versuch == 0 { continue }
                    return nil
                }
                guard let p = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(samples.count)) else { return nil }
                p.frameLength = AVAudioFrameCount(samples.count)
                let ziel = p.floatChannelData![0]
                samples.withUnsafeBufferPointer { q in ziel.update(from: q.baseAddress!, count: samples.count) }
                return p
            } catch {
                if versuch == 0 && !Task.isCancelled { continue }
                log("fehler", "Google-TTS: \(error.localizedDescription)")
                return nil
            }
        }
        return nil
    }

    // MARK: Systemstimme

    private static func besteDeutscheStimme() -> AVSpeechSynthesisVoice? {
        let deutsch = AVSpeechSynthesisVoice.speechVoices().filter { $0.language.hasPrefix("de") }
        return deutsch.max { a, b in
            if a.quality.rawValue != b.quality.rawValue { return a.quality.rawValue < b.quality.rawValue }
            return (a.language == "de-DE" ? 1 : 0) < (b.language == "de-DE" ? 1 : 0)
        } ?? AVSpeechSynthesisVoice(language: "de-DE")
    }

    private func sprichSystem(_ text: String, tempo: Double, generation meine: Int) async {
        guard generation == meine else { return }
        let u = AVSpeechUtterance(string: text)
        u.voice = Self.besteDeutscheStimme()
        u.rate = Float(min(max(Double(AVSpeechUtteranceDefaultSpeechRate) * tempo, 0.3), 0.65))
        // Der Systemstimme fehlt ein Pegel – sanft pulsierende Animation stattdessen.
        let start = Date()
        pegelUhr?.invalidate()
        pegelUhr = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.spricht else { return }
                let t = Date().timeIntervalSince(start)
                self.pegel = Float(0.35 + 0.25 * sin(t * 9) + 0.15 * sin(t * 23))
            }
        }
        await withCheckedContinuation { (k: CheckedContinuation<Void, Never>) in
            systemFertig = k
            system.speak(u)
        }
        pegelUhr?.invalidate(); pegelUhr = nil
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in let k = self.systemFertig; self.systemFertig = nil; k?.resume() }
    }

    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        Task { @MainActor in let k = self.systemFertig; self.systemFertig = nil; k?.resume() }
    }
}

// MARK: - Diktat

/// Spracheingabe mit Ende-Erkennung (Energie-VAD mit Rauschboden) und Transkription über Apple (on-device) oder Groq.
@MainActor @Observable final class Diktat {
    static let shared = Diktat()
    private(set) var aktiv = false
    private(set) var pegel: Float = 0
    private(set) var vorschau = ""             // Zwischenergebnis während des Sprechens
    private(set) var fehler: String?

    private var stoppWunsch = false
    private var abbruchWunsch = false

    /// Vom Audio-Thread beschrieben, vom Hauptthread gelesen.
    private final class Messung: @unchecked Sendable {
        let lock = NSLock()
        var pegel: Float = 0
        var rauschboden: Float = 0.004
        var messungen = 0
        var sprachZeit: Double = 0
        var begonnen = false
        var letzteSprache = Date.distantPast
        var samples: [Float] = []
        func mit<T>(_ f: (Messung) -> T) -> T { lock.lock(); defer { lock.unlock() }; return f(self) }
    }

    /// Hört zu, bis Frank fertig gesprochen hat (Stille) oder `stoppen()` kommt; gibt den erkannten Text zurück (nil = nichts).
    func hoereBisStille(maxSekunden: Double = 30) async -> String? {
        guard !aktiv else { return nil }
        fehler = nil
        guard await SprachRechte.mikrofon() else {
            fehler = "Mikrofon nicht erlaubt (Systemeinstellungen → Datenschutz → Mikrofon → Jarvis)."
            log("fehler", "Diktat: \(fehler!)")
            return nil
        }
        Weckwort.shared.pausieren()
        defer { Weckwort.shared.fortsetzen() }
        aktiv = true; stoppWunsch = false; abbruchWunsch = false; vorschau = ""; pegel = 0
        defer { aktiv = false; pegel = 0 }

        let apple = Einstellungen.shared.transkription != "groq"
        let messung = Messung()
        let ziel16k = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: 16000, channels: 1, interleaved: false)!
        let umwandler = Umwandler(ziel: ziel16k)
        var erkenner: ApfelErkenner?
        if apple {
            let e = ApfelErkenner(dauerbetrieb: false)
            e.beiText = { [weak self] text, _ in Task { @MainActor in self?.vorschau = text } }
            do { try await e.starten(); erkenner = e } catch {
                fehler = "Spracherkennung: \(error.localizedDescription)"
                log("fehler", "Diktat: \(fehler!)")
                return nil
            }
        }
        let mikro = Mikrofon()
        let mitSamples = !apple
        mikro.beiPuffer = { p in
            erkenner?.fuettere(p)
            let rms = Mikrofon.pegel(p)
            let dauer = Double(p.frameLength) / p.format.sampleRate
            let w = mitSamples ? umwandler.wandle(p) : nil
            messung.mit { m in
                m.pegel = rms
                m.messungen += 1
                if m.messungen <= 6 {                      // erste ~0,25 s: Rauschboden messen
                    m.rauschboden = max(0.002, (m.rauschboden * Float(m.messungen - 1) + rms) / Float(m.messungen))
                } else if rms < m.rauschboden * 1.5 {
                    m.rauschboden = m.rauschboden * 0.98 + rms * 0.02
                }
                let schwelle = max(m.rauschboden * 3.2, 0.012)
                if rms > schwelle {
                    m.sprachZeit += dauer
                    m.letzteSprache = Date()
                    if m.sprachZeit > 0.18 { m.begonnen = true }
                }
                if let w, let k = w.floatChannelData, m.samples.count < 16000 * 120 {
                    m.samples.append(contentsOf: UnsafeBufferPointer(start: k[0], count: Int(w.frameLength)))
                }
            }
        }
        do { try mikro.starten() } catch {
            fehler = error.localizedDescription
            log("fehler", "Diktat: Mikrofon startet nicht (\(fehler!))")
            if let erkenner { await erkenner.beenden(abschliessen: false) }
            return nil
        }
        Toene.start()
        let start = Date()
        var abgeschlossen = false
        while true {
            try? await Task.sleep(for: .milliseconds(50))
            let (p, begonnen, letzte) = messung.mit { ($0.pegel, $0.begonnen, $0.letzteSprache) }
            pegel = min(1, p * 8)
            if abbruchWunsch || Task.isCancelled { break }
            if stoppWunsch { abgeschlossen = true; break }
            let jetzt = Date()
            if jetzt.timeIntervalSince(start) > maxSekunden { abgeschlossen = true; break }
            if begonnen {
                // Apple liefert schon Text: kleine Pause genügt nicht, wenn noch kein Wort da ist.
                let stille = jetzt.timeIntervalSince(letzte)
                if stille > 1.2 && (!apple || !vorschau.isEmpty || stille > 2.5) { abgeschlossen = true; break }
            } else if jetzt.timeIntervalSince(start) > 8 {
                break
            }
        }
        mikro.stoppen()
        guard abgeschlossen, messung.mit({ $0.begonnen }) || stoppWunsch else {
            if let erkenner { await erkenner.beenden(abschliessen: false) }
            vorschau = ""
            return nil
        }
        Toene.ende()
        var text: String
        if let erkenner {
            text = await erkenner.beenden(abschliessen: true)
        } else {
            let (samples, sprach) = messung.mit { ($0.samples, $0.sprachZeit) }
            do {
                text = try await GroqWhisper.transkribiere(samples, sprachSekunden: sprach, schluessel: Einstellungen.shared.zugang.groq)
            } catch {
                fehler = error.localizedDescription
                log("fehler", "Diktat (Groq): \(fehler!)")
                return nil
            }
        }
        text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        vorschau = text
        log("sprache", "Diktat erkannt (\(apple ? "Apple" : "Groq")): \(text.prefix(120))")
        return text.isEmpty ? nil : text
    }

    /// Beendet das Zuhören und erkennt das bisher Gesagte.
    func stoppen() { if aktiv { stoppWunsch = true } }
    /// Verwirft die laufende Aufnahme.
    func abbrechen() { if aktiv { abbruchWunsch = true } }
}

// MARK: - Weckwort

/// Weckwort „Hey Jarvis“ – lauscht dauerhaft auf dem Mac (Apple On-Device-Erkennung).
/// Pausen werden gezählt: jedes `pausieren()` braucht ein `fortsetzen()`.
@MainActor @Observable final class Weckwort {
    static let shared = Weckwort()
    private(set) var lauscht = false
    private(set) var fehler: String?
    var beiErkennung: (() -> Void)?

    private var gewuenscht = false
    private var pausen = 0
    private var mikro: Mikrofon?
    private var erkenner: ApfelErkenner?
    private var startet = false
    private var neustartAufgabe: Task<Void, Never>?
    private var letzterTreffer = Date.distantPast
    private var beobachter: [NSObjectProtocol] = []

    init() {
        let nc = NSWorkspace.shared.notificationCenter
        beobachter.append(nc.addObserver(forName: NSWorkspace.didWakeNotification, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor in self?.neuStarten(grund: "nach Ruhezustand") }
        })
        beobachter.append(nc.addObserver(forName: NSWorkspace.willSleepNotification, object: nil, queue: .main) { [weak self] _ in
            Task { @MainActor in await self?.stoppeIntern() }
        })
    }

    func starten() {
        gewuenscht = true
        Task { await starteIntern() }
    }

    func stoppen() {
        gewuenscht = false
        Task { await stoppeIntern() }
    }

    /// Kurz anhalten, solange Diktat, Vorlesen oder Live das Mikrofon brauchen.
    func pausieren() {
        pausen += 1
        if lauscht || startet { Task { await stoppeIntern() } }
    }

    func fortsetzen() {
        pausen = max(0, pausen - 1)
        guard pausen == 0, gewuenscht else { return }
        Task {
            try? await Task.sleep(for: .milliseconds(350))
            await starteIntern()
        }
    }

    private func neuStarten(grund: String) {
        guard gewuenscht, pausen == 0 else { return }
        log("sprache", "Weckwort startet neu (\(grund))")
        Task {
            await stoppeIntern()
            try? await Task.sleep(for: .milliseconds(500))
            await starteIntern()
        }
    }

    private func starteIntern() async {
        guard gewuenscht, pausen == 0, !lauscht, !startet, Einstellungen.shared.weckwortAn else { return }
        startet = true
        defer { startet = false }
        guard await SprachRechte.mikrofon() else {
            fehler = "Mikrofon nicht erlaubt – Weckwort aus."
            log("fehler", "Weckwort: \(fehler!)")
            return
        }
        let e = ApfelErkenner(dauerbetrieb: true)
        e.beiText = { [weak self] text, _ in
            Task { @MainActor in self?.pruefe(text) }
        }
        do { try await e.starten() } catch {
            fehler = "Weckwort: \(error.localizedDescription)"
            log("fehler", fehler!)
            return
        }
        // Während des Starts pausiert oder abgeschaltet?
        guard gewuenscht, pausen == 0 else { await e.beenden(abschliessen: false); return }
        let m = Mikrofon()
        m.beiPuffer = { p in e.fuettere(p) }
        m.beiGeraetewechsel = { [weak self] in Task { @MainActor in self?.neuStarten(grund: "Gerätewechsel") } }
        do { try m.starten() } catch {
            fehler = "Weckwort: Mikrofon startet nicht (\(error.localizedDescription))"
            log("fehler", fehler!)
            await e.beenden(abschliessen: false)
            return
        }
        mikro = m; erkenner = e
        lauscht = true; fehler = nil
        log("sprache", "Weckwort lauscht auf „\(Einstellungen.shared.weckwort)“ (\(e.nutztAnalyzer ? "SpeechAnalyzer" : "SFSpeechRecognizer"))")
        // Regelmäßig frisch starten: SFSpeechRecognizer endet nach ~1 Minute, der Analyzer hält den Zustand klein.
        let intervall: Double = e.nutztAnalyzer ? 600 : 50
        neustartAufgabe?.cancel()
        neustartAufgabe = Task { [weak self] in
            try? await Task.sleep(for: .seconds(intervall))
            guard !Task.isCancelled else { return }
            self?.neuStarten(grund: "Intervall")
        }
    }

    private func stoppeIntern() async {
        neustartAufgabe?.cancel(); neustartAufgabe = nil
        let m = mikro, e = erkenner
        mikro = nil; erkenner = nil
        lauscht = false
        m?.stoppen()
        if let e { await e.beenden(abschliessen: false) }
    }

    private static let varianten = ["jarvis", "jarvice", "jervis", "javis", "jarwis", "jarwiss", "tschawis", "dschawis",
                                    "charvis", "scharvis", "jarviz", "jarvis's", "dravis", "travis"]

    /// Erkennt das Weckwort tolerant gegen Erkennungsvarianten.
    static func passt(_ roh: String, phrase: String) -> Bool {
        let t = GroqWhisper.normal(roh)
        guard !t.isEmpty else { return false }
        let p = GroqWhisper.normal(phrase)
        if !p.isEmpty && t.contains(p) { return true }
        let woerter = t.split(separator: " ").map(String.init)
        let einleitung: Set<String> = ["hey", "hej", "hei", "he", "hallo", "okay", "ok", "hi", "ey"]
        let phraseNutztJarvis = p.contains("jarvis") || p.isEmpty
        guard phraseNutztJarvis else { return false }
        for (i, w) in woerter.enumerated() {
            let istJarvis = varianten.contains(w) && (w != "travis" && w != "dravis" || (i > 0 && einleitung.contains(woerter[i - 1])))
            guard istJarvis else { continue }
            if i > 0 && einleitung.contains(woerter[i - 1]) { return true }
            if i == 0 { return true }                       // „Jarvis, …“ am Satzanfang
        }
        return false
    }

    private func pruefe(_ text: String) {
        guard lauscht, Self.passt(text, phrase: Einstellungen.shared.weckwort) else { return }
        guard Date().timeIntervalSince(letzterTreffer) > 2 else { return }
        letzterTreffer = Date()
        log("sprache", "Weckwort erkannt: „\(text.suffix(60))“")
        Task {
            await stoppeIntern()
            Toene.weckwort()
            if let beiErkennung {
                beiErkennung()
            } else {
                SprachSchleife.shared.starten(in: GespraechsSpeicher.shared.neu())
            }
            // Hat niemand pausiert (z. B. leerer Handler), nach kurzer Zeit wieder lauschen.
            try? await Task.sleep(for: .seconds(2))
            if pausen == 0 { await starteIntern() }
        }
    }
}

// MARK: - Sprachschleife

/// Klassische Sprachschleife: zuhören → Jarvis → vorlesen → kurzes Folgefenster.
@MainActor @Observable final class SprachSchleife {
    static let shared = SprachSchleife()
    enum Zustand: String { case aus, hoert, denkt, spricht }
    private(set) var zustand: Zustand = .aus
    private(set) var gespraech: Gespraech?
    private var aufgabe: Task<Void, Never>?
    private var lauf = 0

    private static let endeWoerter = ["danke", "danke schön", "dankeschön", "vielen dank", "stopp", "stop", "das wars",
                                      "das war es", "das war's", "hör auf", "hoer auf", "tschüss", "ende", "beenden",
                                      "sprachsession beenden", "gespräch beenden", "nichts mehr", "passt danke"]

    static func istEnde(_ text: String) -> Bool {
        let n = GroqWhisper.normal(text.replacingOccurrences(of: "'", with: ""))
        guard n.split(separator: " ").count <= 4 else { return false }
        let ohneJarvis = n.replacingOccurrences(of: "jarvis", with: "").trimmingCharacters(in: .whitespaces)
        return endeWoerter.contains { w in
            let x = GroqWhisper.normal(w)
            return ohneJarvis == x || ohneJarvis.hasPrefix(x + " ") || ohneJarvis.hasSuffix(" " + x)
        }
    }

    func starten(in g: Gespraech) {
        aufgabe?.cancel()
        Diktat.shared.abbrechen()
        Stimme.shared.stopp()
        lauf += 1
        let meiner = lauf
        gespraech = g
        aufgabe = Task { [weak self] in await self?.schleife(g, lauf: meiner) }
    }

    func beenden() {
        lauf += 1
        aufgabe?.cancel(); aufgabe = nil
        Diktat.shared.abbrechen()
        Stimme.shared.stopp()
        zustand = .aus
    }

    private func schleife(_ g: Gespraech, lauf meiner: Int) async {
        Weckwort.shared.pausieren()
        defer {
            Weckwort.shared.fortsetzen()
            if lauf == meiner { zustand = .aus }
        }
        log("sprache", "Sprachschleife gestartet")
        while lauf == meiner && !Task.isCancelled {
            zustand = .hoert
            // Ein gerade abgebrochenes Diktat braucht einen Moment, bis es frei ist.
            var warte = 0
            while Diktat.shared.aktiv && warte < 40 { try? await Task.sleep(for: .milliseconds(50)); warte += 1 }
            guard let text = await Diktat.shared.hoereBisStille(maxSekunden: 30) else { break }
            guard lauf == meiner, !Task.isCancelled else { break }
            if Self.istEnde(text) {
                log("sprache", "Sprachschleife beendet („\(text)“)")
                break
            }
            zustand = .denkt
            let antwort = await JarvisAgent.shared.frage(text, in: g, gesprochen: true)
            guard lauf == meiner, !Task.isCancelled else { break }
            if Einstellungen.shared.vorlesen && !antwort.isEmpty {
                zustand = .spricht
                await Stimme.shared.sprich(antwort)
                guard lauf == meiner, !Task.isCancelled else { break }
            }
        }
    }
}
