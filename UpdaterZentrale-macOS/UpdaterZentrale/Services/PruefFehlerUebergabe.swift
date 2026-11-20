import Foundation
import Network

/// Failed CHECKS (a card's version query, single, via „Alle prüfen“ or in the start check) go to a
/// Claude Code session automatically -- the same start path as the update failures
/// (`ClaudeUebergabe.sitzungStarten`: OpenLauncher, Opus 5.5, profile minimal).
///
/// Collected instead of one start each: every failure enters a queue; 20 seconds after the last new
/// failure, and only once no batch operation runs, ONE session starts for all of them.
/// Own lock per program (`pruefFehlerUebergebenAm`), separate from the update failures.
/// Without any network connection nothing starts: no session can repair that.
@MainActor
final class PruefFehlerUebergabe {

    struct Fall {
        let eintrag: ProgrammEintrag
        let zeit: Date
        let meldung: String
        let installiert: String
        /// The card log when queued -- fallback in case the card has been replaced meanwhile.
        let auszug: String
    }

    /// At most one session per program and hour for check failures.
    static let sperrfrist: TimeInterval = 3600

    /// Quiet time after the last new failure before the session starts.
    private static let warteNanos: UInt64 = 20_000_000_000

    /// Log excerpt for all programs together (the launcher passes the task on as one argument).
    private static let protokollZeichen = 12000

    /// Upper bound for one program's message.
    private static let meldungZeichen = 600

    /// Upper bound for the whole task text; the rest is in the log files.
    private static let auftragZeichen = 25000

    static func kartenText(_ meldung: String) -> String {
        "Prüffehler automatisch an Claude Code übergeben – die Sitzung läuft im Terminal und macht die Prüfung künftig robuster."
            + (meldung.istLeer ? "" : " Meldung: " + meldung)
    }

    private let einstellungen: Einstellungen
    private let sammelLaeuft: () -> Bool
    private let karteMelden: (String, String) -> Void
    private let protokollLesen: (String) -> String?
    private let karteHatFehler: (String) -> Bool?

    private var warteschlange: [Fall] = []
    private var wecker: Task<Void, Never>?
    private var startLaeuft = false

    /// - Parameters:
    ///   - sammelLaeuft: is a batch operation running right now (start check, „Alle prüfen“, batch update)?
    ///   - karteMelden: sets the status of the card with this id.
    ///   - protokollLesen: the current card log of the id (nil if the card no longer exists).
    ///   - karteHatFehler: does the card of this id still show a failure (nil if the card no longer exists)?
    init(einstellungen: Einstellungen, sammelLaeuft: @escaping () -> Bool,
         karteMelden: @escaping (String, String) -> Void, protokollLesen: @escaping (String) -> String?,
         karteHatFehler: @escaping (String) -> Bool?) {
        self.einstellungen = einstellungen
        self.sammelLaeuft = sammelLaeuft
        self.karteMelden = karteMelden
        self.protokollLesen = protokollLesen
        self.karteHatFehler = karteHatFehler
    }

    /// Queues a failed check and resets the timer.
    func melden(_ eintrag: ProgrammEintrag, _ ergebnis: PruefErgebnis, protokoll: String) {
        Netzwaechter.shared.beobachten()
        let fall = Fall(eintrag: eintrag, zeit: Date(),
                        meldung: ergebnis.meldung.istLeer ? "(keine Meldung)" : ergebnis.meldung,
                        installiert: ergebnis.installierteVersion,
                        auszug: String(protokoll.suffix(Self.protokollZeichen)))
        // Only the newest failure per program counts.
        warteschlange.removeAll { $0.eintrag.id == eintrag.id }
        warteschlange.append(fall)
        Diagnose.ereignis(.info, "uebergabe", "prueffehler.vorgemerkt", fall.meldung, ["programm": eintrag.id])
        weckerStellen()
    }

    /// A later check of this program ended without a failure: the queued failure is gone.
    func erledigt(_ id: String) {
        let vorher = warteschlange.count
        warteschlange.removeAll { $0.eintrag.id == id }
        guard warteschlange.count != vorher else { return }
        Diagnose.ereignis(.info, "uebergabe", "prueffehler.erledigt", "Spätere Prüfung ohne Fehler", ["programm": id])
        if warteschlange.isEmpty {
            wecker?.cancel()
            wecker = nil
        }
    }

    private func weckerStellen() {
        wecker?.cancel()
        wecker = Task { [weak self] in
            // Cancelled means: a new failure arrived and set a new timer.
            do { try await Task.sleep(nanoseconds: PruefFehlerUebergabe.warteNanos) } catch { return }
            while true {
                guard let self else { return }
                if !self.sammelLaeuft() && !self.startLaeuft {
                    self.abarbeiten()
                    return
                }
                do { try await Task.sleep(nanoseconds: 1_000_000_000) } catch { return }
            }
        }
    }

    /// Takes the queue out and starts in a SEPARATE task: a new failure only cancels the timer, never a
    /// launcher start in progress.
    private func abarbeiten() {
        wecker = nil
        let faelle = warteschlange
        warteschlange = []
        guard !faelle.isEmpty else { return }
        startLaeuft = true
        Task {
            await self.starten(faelle)
            self.startLaeuft = false
        }
    }

    private func starten(_ faelle: [Fall]) async {
        let ids = faelle.map(\.eintrag.id).joined(separator: ",")
        guard Netzwaechter.shared.verbunden else {
            Diagnose.ereignis(.warnung, "uebergabe", "prueffehler.ohneNetz",
                              "Keine Netzverbindung – Prüffehler gehen nicht an Claude Code.", ["programme": ids])
            return
        }

        // Same queue as the update failures: launcher starts run one after another.
        while ClaudeUebergabe.automatischLaeuft {
            do { try await Task.sleep(nanoseconds: 500_000_000) } catch { return }
        }
        ClaudeUebergabe.automatischLaeuft = true
        defer { ClaudeUebergabe.automatischLaeuft = false }

        // Only failures the card still shows: a card that is checking again or no longer shows a failure
        // drops out (a new failure queues itself again). A replaced card (catalog reload) keeps its case.
        let aktuell = faelle.filter { karteHatFehler($0.eintrag.id) ?? true }
        if aktuell.count < faelle.count {
            Diagnose.ereignis(.info, "uebergabe", "prueffehler.verworfen", "Karte zeigt keinen Fehler mehr",
                              ["programme": faelle.filter { !(karteHatFehler($0.eintrag.id) ?? true) }.map(\.eintrag.id).joined(separator: ",")])
        }

        let jetzt = Date()
        let offen = aktuell.filter { fall in
            guard let zuletzt = einstellungen.fuer(fall.eintrag.id).pruefFehlerUebergebenAm else { return true }
            return jetzt.timeIntervalSince(zuletzt) >= Self.sperrfrist
        }
        guard !offen.isEmpty else {
            if !aktuell.isEmpty {
                Diagnose.ereignis(.info, "uebergabe", "prueffehler.gesperrt", "Sperrfrist für Prüffehler läuft",
                                  ["programme": aktuell.map(\.eintrag.id).joined(separator: ",")])
            }
            return
        }

        // Marked BEFORE the start: if the app ends during the launcher start, the lock still holds.
        let bisher = offen.map { einstellungen.fuer($0.eintrag.id).pruefFehlerUebergebenAm }
        for fall in offen { einstellungen.fuer(fall.eintrag.id).pruefFehlerUebergebenAm = jetzt }
        einstellungen.speichern()

        let kennung = offen.count == 1 ? offen[0].eintrag.id : "sammel-\(offen.count)"
        let text = auftragText(offen)
        let (problem, vielleicht) = await ClaudeUebergabe.sitzungStarten(
            dateiName: "prueffehler-\(kennung)", sitzungsName: "Updater-Zentrale: Prüffehler \(kennung)", auftrag: text)
        Diagnose.ereignis(problem == nil ? .info : .warnung, "uebergabe", "prueffehler.start", problem ?? "Sitzung gestartet",
                          ["programme": offen.map(\.eintrag.id).joined(separator: ",")])

        // Unclear (the launcher did not answer): the session usually runs anyway, so the mark stays.
        if problem != nil && !vielleicht {
            for (fall, alt) in zip(offen, bisher) { einstellungen.fuer(fall.eintrag.id).pruefFehlerUebergebenAm = alt }
            einstellungen.speichern()
        }
        for fall in offen {
            if problem == nil || vielleicht {
                karteMelden(fall.eintrag.id, Self.kartenText(Self.kappen(fall.meldung, 200)))
            } else if let problem {
                karteMelden(fall.eintrag.id, "Prüffehler konnte nicht an Claude Code übergeben werden: " + problem)
            }
        }
    }

    private func auftragText(_ faelle: [Fall]) -> String {
        let zeit = DateFormatter()
        zeit.dateFormat = "dd.MM.yyyy, HH:mm:ss"
        let ordner = Pfade.projektOrdner
        var t = "Auftrag von der UpdaterZentrale (macOS): Die Prüfung von \(faelle.count) Programm(en) ist fehlgeschlagen. "
              + "Finde die Ursache, beuge dauerhaft vor und bestätige danach eine erfolgreiche Prüfung.\n"
        t += "Geprüft wird über die Updater-Zentrale (\(ordner), Katalog programs.json, Prüfwege in \(ordner)/UpdaterZentrale/Providers).\n"
        t += "REVIEW: ja\n\n"
        t += "Arbeite diese Schritte automatisch und genau in dieser Reihenfolge ab:\n"
        t += "1. Finde die Ursache aus Meldung und Protokoll unten (bei mehreren Programmen jede einzeln, bei Bedarf in den Protokolldateien). "
           + "Vorübergehende Netz- oder Serverfehler wie Timeouts zählen auch: Mach dann die Prüfung robuster, z. B. mit Wiederholung, Ausweichquelle oder längerer Frist.\n"
        t += "2. Beuge dauerhaft in der App vor, möglichst für die ganze Fehlerart, nicht nur für diesen einen Fall (programs.json oder Prüfweg im Code). "
           + "Kann dieselbe Ursache auch unter Windows auftreten, zieh ~/proggs/UpdateZentrale mit.\n"
        t += "3. Halte Ursache und Vorbeugung im passenden Bug-Almanach unter ~/proggs/bugs fest.\n"
        t += "4. Führe die Prüfung jedes betroffenen Programms erneut aus (auf dem reparierten Weg, notfalls hier von Hand nachgestellt) und bestätige, dass sie jetzt durchläuft.\n"
        t += "5. Führe ohne Rückfrage das Standard-Code-Review über die Änderungen dieser Sitzung aus (Skill /code-review) und behebe die Befunde. Ohne Codeänderung entfällt das Review.\n"
        t += "6. Bumpe die Version (Regeln in der CLAUDE.md von \(ordner), bei Windows-Änderungen auch dort), baue, committe und pushe. "
           + "Baue als allerletzten Schritt die Updater-Zentrale neu, sobald sich in \(ordner) etwas geändert hat: bash \(ordner)/update-zentrale.sh, mit dem höchsten erlaubten Zeitlimit. "
           + "Läuft die App, zeigt mir das Skript ein Ja/Nein-Fenster und schließt sie danach. Prüfe danach in ~/Library/Logs/UpdaterZentrale-selbstupdate.log, ob die neue Fassung gestartet ist.\n\n"
        t += "Regeln für diese Sitzung:\n"
        t += "- Beende andere laufende Programme nicht ohne Rückfrage. Eigene Werkzeuge (OpenLauncher, TVO, CVO, Updater-Zentrale) nur über ihre Update-Skripte nach den Regeln der CLAUDE.md.\n"
        t += "- Ein Nein oder keine Antwort in einem Rückfragefenster (cancelled, no-answer) ist kein Klemmen: nicht erneut starten, sondern melden.\n"
        t += "- Andere Sitzungen können parallel an diesem Repo arbeiten: Lies die Versionsnummer direkt vor dem Bump frisch und committe nur deine eigenen Pfade.\n"
        t += "- Schritt 6 schließt die App und bräche so einen Sammellauf ab: Trägt die letzte Zeile von " + Protokollierung.sammelDatei
           + " das Ereignis „plan“ oder „fortsetzung“, läuft er noch. Warte dann vor Schritt 6 auf sein Ende, höchstens zwei Stunden.\n"

        let budget = Self.protokollZeichen / max(faelle.count, 1)
        for fall in faelle {
            let eintrag = fall.eintrag
            t += "\n=== \(eintrag.name) ===\n"
            t += "Programm: \(eintrag.name) (Id \(eintrag.id), Art \(eintrag.art))\n"
            t += "Zeitpunkt: \(zeit.string(from: fall.zeit))\n"
            if !fall.installiert.istLeer { t += "Installierte Version: \(fall.installiert)\n" }
            t += "Meldung: \(Self.kappen(fall.meldung, Self.meldungZeichen))\n"
            t += "Tagesprotokoll (Zeilen mit [\(eintrag.id)]): \(Protokollierung.tagesDatei(fall.zeit))\n"
            t += "Weitere Protokolle (Diagnose-JSONL, verlauf.jsonl): \(Protokollierung.ordner)\n"

            // Read fresh: the last lines of the check reach the card asynchronously.
            let protokoll = protokollLesen(eintrag.id) ?? fall.auszug
            let auszug = Self.ende(protokoll, budget)
            if !auszug.istLeer {
                t += "\nProtokollauszug der Karte" + (auszug.count < protokoll.trimmed.count ? " (gekürzt, nur das Ende)" : "") + ":\n"
                t += "----\n" + auszug + "\n----\n"
            }
        }

        // Hard cap for the whole text; whatever is cut off is in the log files.
        if t.count > Self.auftragZeichen {
            let hinweis = "\n… [Auftrag gekürzt – den Rest in den Protokolldateien unter \(Protokollierung.ordner) lesen]\n"
            t = String(t.prefix(max(Self.auftragZeichen - hinweis.count, 0))) + hinweis
        }
        return t
    }

    /// Cuts a text to at most `grenze` characters, marked with „…“.
    private static func kappen(_ text: String, _ grenze: Int) -> String {
        text.count > grenze ? String(text.prefix(max(grenze - 2, 0))) + " …" : text
    }

    /// The end of a log in at most `budget` characters, cut at a line boundary.
    private static func ende(_ text: String, _ budget: Int) -> String {
        let sauber = text.trimmed
        guard sauber.count > budget else { return sauber }
        guard budget > 2 else { return "" }
        let rest = String(sauber.suffix(budget - 2))
        if let umbruch = rest.firstIndex(of: "\n"), rest.distance(from: rest.startIndex, to: umbruch) < rest.count / 2 {
            return "… " + String(rest[rest.index(after: umbruch)...])
        }
        return "… " + rest
    }
}

/// Network state for the check-failure handover. Started at the first failure and read from then on:
/// `currentPath` right after `start()` is not reliable yet. Only a definite "no network"
/// (`.unsatisfied`) holds the session back; a state not yet known counts as connected.
final class Netzwaechter: @unchecked Sendable {
    static let shared = Netzwaechter()

    private let monitor = NWPathMonitor()
    private let schloss = NSLock()
    private var status: NWPath.Status?
    private var gestartet = false

    private init() {}

    func beobachten() {
        schloss.lock()
        defer { schloss.unlock() }
        guard !gestartet else { return }
        gestartet = true
        monitor.pathUpdateHandler = { [weak self] pfad in
            guard let self else { return }
            self.schloss.lock()
            self.status = pfad.status
            self.schloss.unlock()
        }
        monitor.start(queue: DispatchQueue(label: "UpdaterZentrale.netzwaechter"))
    }

    var verbunden: Bool {
        schloss.lock()
        defer { schloss.unlock() }
        return status != .unsatisfied
    }
}
