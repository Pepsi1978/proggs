import AVFoundation
import Foundation
import Observation

// SCHNITTSTELLE (fest): LiveZeile, LiveGespraech (Zustand, pegelEin, pegelAus, mitschrift, fehler, aktiv, starten(in:), beenden()).

struct LiveZeile: Identifiable, Hashable {
    var id = UUID()
    var vonFrank: Bool
    var text: String
}

/// Live-Gespräch: gleichzeitig zuhören und sprechen (OpenAI GPT-Live/Realtime oder Gemini Live).
/// Sachfragen und Aktionen gehen an den JarvisAgent (gleiches Gehirn mit Handy- und Mac-Werkzeugen, gleiches Gedächtnis).
/// Der Live-Ton hat Vorrang: Ton und Socket laufen in eigenen Fäden hoher Priorität, die Oberfläche liest nur Pegel.
@MainActor @Observable final class LiveGespraech {
    static let shared = LiveGespraech()
    enum Zustand: String { case aus, verbindet, hoert, spricht, denkt }
    private(set) var zustand: Zustand = .aus { didSet { if zustand != oldValue { AppSteuerung.shared.pilleAbgleichen() } } }
    private(set) var pegelEin: Float = 0
    private(set) var pegelAus: Float = 0
    private(set) var mitschrift: [LiveZeile] = []
    private(set) var fehler: String?
    /// Anbieter und Modell der laufenden Sitzung (für die Anzeige).
    private(set) var beschreibung = ""
    var aktiv: Bool { zustand != .aus }

    @ObservationIgnored private var sitzung: LiveSitzung?
    @ObservationIgnored private var ton: LiveTon?
    @ObservationIgnored private var gespraech: Gespraech?
    @ObservationIgnored private var zeitgeber: Timer?
    @ObservationIgnored private var bruecke: Bruecke?
    @ObservationIgnored private var offeneAuftraege = 0
    @ObservationIgnored private var auftragsKette: Task<Void, Never>?
    @ObservationIgnored private var letzteAktivitaet = Date()
    /// Stille-Ende dieser Sitzung in Minuten, beim Start gelesen wie am Handy; 0 = nie.
    @ObservationIgnored private var stilleMinuten = 0
    /// Kommt die Verbindung so lange nicht zustande, endet das Gespräch auch bei „Nie“.
    private static let verbindenMaxSek: Double = 180
    /// Ab diesem Mikrofonpegel spricht jemand: Eine lange Rede zählt nicht als Stille, auch wenn die Mitschrift ausbleibt.
    private static let stimmPegel: Float = 0.1
    /// GPT-Live/Realtime: nicht delegierte Beiträge (Smalltalk) kommen ins Gespräch; delegierte schreibt der Agent selbst.
    @ObservationIgnored private var ausstehend: [Nachricht] = []
    @ObservationIgnored private var naechsteJarvisVomAgenten = 0

    /// Leitet die Rückmeldungen aus dem Socket-Faden auf den Hauptfaden.
    private final class Bruecke: LiveSitzungsDelegate, @unchecked Sendable {
        weak var ziel: LiveGespraech?
        init(_ z: LiveGespraech) { ziel = z }
        func liveBereit() { Task { @MainActor in self.ziel?.beiBereit() } }
        func liveNutzerText(_ text: String) { Task { @MainActor in self.ziel?.beiNutzer(text) } }
        func liveJarvisText(_ text: String) { Task { @MainActor in self.ziel?.beiJarvis(text) } }
        func liveWerkzeug(kennung: String, auftrag: String, hinweis: String) {
            Task { @MainActor in self.ziel?.beiWerkzeug(kennung: kennung, auftrag: auftrag, hinweis: hinweis) }
        }
        func liveFehler(_ text: String) { Task { @MainActor in self.ziel?.beiFehler(text) } }
        func liveEnde(_ grund: String?) { Task { @MainActor in self.ziel?.beenden() } }
    }

    // MARK: Start / Ende

    func starten(in g: Gespraech) {
        guard !aktiv else { return }
        fehler = nil
        mitschrift = []
        ausstehend = []
        naechsteJarvisVomAgenten = 0
        offeneAuftraege = 0
        gespraech = g
        zustand = .verbindet
        Stimme.shared.stopp()
        if SprachSchleife.shared.zustand != .aus { SprachSchleife.shared.beenden() }
        Weckwort.shared.pausieren()
        Task { await aufbauen(g) }
    }

    private func aufbauen(_ g: Gespraech) async {
        let erlaubt: Bool
        switch AVCaptureDevice.authorizationStatus(for: .audio) {
        case .authorized: erlaubt = true
        case .notDetermined: erlaubt = await AVCaptureDevice.requestAccess(for: .audio)
        default: erlaubt = false
        }
        guard zustand == .verbindet else { return }
        guard erlaubt else {
            abbrechen("Jarvis darf das Mikrofon nicht nutzen. Bitte in Systemeinstellungen → Datenschutz → Mikrofon erlauben.")
            return
        }
        let e = Einstellungen.shared
        let gemini = e.liveAnbieter == "gemini"
        let neuerTon = LiveTon(eingabeRate: gemini ? 16_000 : 24_000)
        let neu: LiveSitzung
        if gemini {
            guard !e.zugang.gemini.isEmpty else { abbrechen("Kein Gemini-Schlüssel eingetragen (Einstellungen → Konten)."); return }
            let modell = LiveOptionen.geminiModell(e.liveModellGemini)
            let stimme = LiveOptionen.geminiStimme(e.liveStimmeGemini)
            neu = LiveGemini(ton: neuerTon, schluessel: e.zugang.gemini, modell: modell, stimme: stimme)
            beschreibung = (LiveOptionen.geminiModelle.first { $0.id == modell }?.name ?? modell) + " · " + stimme
        } else {
            guard !e.zugang.openai.isEmpty else { abbrechen("Kein OpenAI-Schlüssel eingetragen (Einstellungen → Konten)."); return }
            let modell = LiveOptionen.openAIModell(e.liveModellOpenAI)
            let stimme = LiveOptionen.openAIStimme(e.liveStimmeOpenAI, modell: modell)
            neu = LiveOpenAI(ton: neuerTon, schluessel: e.zugang.openai, modell: modell, stimme: stimme)
            beschreibung = (LiveOptionen.openAIModelle.first { $0.id == modell }?.name ?? modell) + " · " + stimme
        }
        let b = Bruecke(self)
        bruecke = b
        neu.delegate = b
        neuerTon.beiMikro = { [weak neu] pcm in neu?.mikro(pcm) }
        neuerTon.beiAusfall = { [weak self] text in
            Task { @MainActor in
                guard let self else { return }
                self.fehler = text
                log("live", text)
                self.beenden()
            }
        }
        do { try await neuerTon.starten() } catch {
            guard zustand == .verbindet, gespraech === g else { return }
            abbrechen("Mikrofon oder Lautsprecher lässt sich nicht starten: \(error.localizedDescription)")
            return
        }
        // Frank kann während des Tonstarts beendet haben.
        guard zustand == .verbindet, gespraech === g else {
            neuerTon.stoppen()
            return
        }
        ton = neuerTon
        sitzung = neu
        log("live", "Live-Gespräch beginnt: \(beschreibung)")
        neu.starten()
        letzteAktivitaet = Date()
        stilleMinuten = e.liveStilleMinuten
        let uhr = Timer(timeInterval: 0.1, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.takt() }
        }
        // Auch während Frank scrollt oder ein Fenster zieht: Beitragsenden und die Jarvis-Weiche hängen an diesem Takt.
        RunLoop.main.add(uhr, forMode: .common)
        zeitgeber = uhr
    }

    private func abbrechen(_ text: String) {
        fehler = text
        log("live", text)
        zustand = .aus
        Weckwort.shared.fortsetzen()
    }

    func beenden() {
        guard aktiv else { return }
        zeitgeber?.invalidate(); zeitgeber = nil
        sitzung?.beenden()
        ton?.stoppen()
        sitzung = nil; ton = nil; bruecke = nil
        ausstehendSchreiben()
        gespraech?.speichern()
        zustand = .aus
        pegelEin = 0; pegelAus = 0
        log("live", "Live-Gespräch beendet")
        Weckwort.shared.fortsetzen()
    }

    /// Beendet nach kurzer Pause, aber nur dieselbe Sitzung: Frank kann inzwischen beendet und neu gestartet haben.
    private func beendenNach(_ sekunden: Double) {
        let s = sitzung
        Task {
            try? await Task.sleep(for: .seconds(sekunden))
            if self.sitzung === s { self.beenden() }
        }
    }

    /// Frank tippt zum Unterbrechen (Jarvis verstummt sofort).
    func unterbrechen() { sitzung?.angetippt() }

    // MARK: Takt (100 ms)

    private func takt() {
        guard let ton, let sitzung else { return }
        sitzung.takt()
        pegelEin = ton.pegelEin
        pegelAus = ton.pegelAus
        let spricht = ton.sprichtHoerbar
        let denkt = offeneAuftraege > 0 || ((sitzung as? LiveGemini)?.weicheArbeitet ?? false)
        if spricht || denkt || (sitzung.bereit && pegelEin >= Self.stimmPegel) { letzteAktivitaet = Date() }
        let neu: Zustand = !sitzung.bereit ? .verbindet : spricht ? .spricht : denkt ? .denkt : .hoert
        if neu != zustand { zustand = neu }
        let ruhe = Date().timeIntervalSince(letzteAktivitaet)
        if !sitzung.bereit && ruhe > Self.verbindenMaxSek {
            fehler = "Live-Gespräch beendet: Die Verbindung kam nicht zustande."
            log("live", "Verbindung kam nicht zustande: Live-Gespräch endet")
            beenden()
        } else if stilleMinuten > 0 && ruhe > Double(stilleMinuten) * 60 {
            fehler = "Live-Gespräch nach \(stilleMinuten) Minuten Stille beendet."
            log("live", "\(stilleMinuten) Minuten Stille: Live-Gespräch endet")
            beenden()
        }
    }

    // MARK: Rückmeldungen der Sitzung

    private func beiBereit() {
        letzteAktivitaet = Date()
        if zustand == .verbindet { zustand = .hoert }
    }

    private func beiNutzer(_ text: String) {
        letzteAktivitaet = Date()
        mitschrift.append(LiveZeile(vonFrank: true, text: text))
        // Vorheriger Zug abgeschlossen (Antwort da): ins Gespräch.
        if ausstehend.contains(where: { $0.rolle == .jarvis }) { ausstehendSchreiben() }
        if !(sitzung?.istGemini ?? false) { ausstehend.append(Nachricht(rolle: .frank, text: text, gesprochen: true)) }
        if LiveOptionen.istBeenden(text) {
            log("live", "Frank beendet das Gespräch per Sprache")
            beendenNach(1.8)
        }
    }

    private func beiJarvis(_ text: String) {
        letzteAktivitaet = Date()
        mitschrift.append(LiveZeile(vonFrank: false, text: text))
        guard !(sitzung?.istGemini ?? false) else { return }
        if naechsteJarvisVomAgenten > 0 {
            // Gesprochene Fassung von Jarvis' Antwort: steht über den Agenten schon im Gespräch.
            naechsteJarvisVomAgenten -= 1
            ausstehend.removeAll()
            return
        }
        ausstehend.append(Nachricht(rolle: .jarvis, text: text, gesprochen: true))
    }

    private func ausstehendSchreiben() {
        guard let g = gespraech, !ausstehend.isEmpty else { ausstehend = []; return }
        g.nachrichten.append(contentsOf: ausstehend)
        ausstehend = []
        g.speichern()
    }

    private func beiFehler(_ text: String) {
        fehler = text
        log("live", "Fehler: \(text)")
    }

    private func beiWerkzeug(kennung: String, auftrag: String, hinweis: String) {
        guard let g = gespraech, let s = sitzung else { return }
        letzteAktivitaet = Date()
        // Der Agent schreibt Frage und Antwort selbst ins Gespräch: Franks ausstehende Worte und Füllsätze entfallen.
        ausstehend.removeAll()
        let worte = auftrag.trimmingCharacters(in: .whitespacesAndNewlines)
        if LiveOptionen.istBeenden(worte) {
            s.ergebnis(kennung: kennung, text: "Bis später, Frank.")
            beendenNach(2.5)
            return
        }
        guard !worte.isEmpty else {
            s.ergebnis(kennung: kennung, text: "Frank hat noch nichts gesagt. Sag nichts weiter.")
            return
        }
        offeneAuftraege += 1
        let frage = hinweis.isEmpty ? worte : worte + "\n" + hinweis
        let vorher = auftragsKette
        let hoheStufe = ["high", "xhigh", "max"].contains(Einstellungen.shared.effort)
        auftragsKette = Task { @MainActor in
            await vorher?.value   // immer nur eine Anfrage gleichzeitig an dasselbe Gespräch
            var fertig = false
            var zwischenstand = false
            let wache = Task { @MainActor in
                try? await Task.sleep(for: .seconds(hoheStufe ? 120 : 60))
                guard !fertig, !Task.isCancelled, let s = self.sitzung else { return }
                // Braucht Jarvis länger: Zwischenstand, die Arbeit läuft weiter und wird nachgereicht.
                zwischenstand = true
                s.ergebnis(kennung: kennung, text: LiveOptionen.dauertNoch)
                log("live", "Jarvis braucht länger: Zwischenstand an das Live-Modell")
            }
            let antwort = await JarvisAgent.shared.frage(frage, in: g, gesprochen: true)
            fertig = true
            wache.cancel()
            self.offeneAuftraege = max(0, self.offeneAuftraege - 1)
            self.letzteAktivitaet = Date()
            let text = antwort.isEmpty ? "Jarvis hat keine Antwort geliefert." : antwort
            let beendenGewuenscht = g.nachrichten.last(where: { $0.rolle == .jarvis })?.schritte.contains { $0.werkzeug == "sprachsession_beenden" } ?? false
            if let s = self.sitzung, self.aktiv {
                self.naechsteJarvisVomAgenten += 1
                let ok = zwischenstand ? s.nachreichen(kennung: kennung, text: text) : s.ergebnis(kennung: kennung, text: text)
                if !ok { await Stimme.shared.sprich(text) }
            } else if zwischenstand {
                // Gespräch inzwischen beendet: Ergebnis vorlesen (steht auch im Chat).
                await Stimme.shared.sprich("Frank, die Antwort ist da. " + text)
            }
            if beendenGewuenscht {
                try? await Task.sleep(for: .seconds(4))
                if self.sitzung === s { self.beenden() }
            }
        }
    }
}
