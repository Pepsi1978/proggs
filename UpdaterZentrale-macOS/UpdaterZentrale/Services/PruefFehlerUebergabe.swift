import Foundation
import Network

/// Gescheiterte PRUEFUNGEN (Versionsabfrage einer Karte, einzeln, per „Alle prüfen“ oder in der
/// Anfangspruefung) gehen automatisch an eine Claude-Code-Sitzung -- derselbe Startweg wie die
/// Update-Fehler (`ClaudeUebergabe.sitzungStarten`: OpenLauncher, Opus 5.5, Profil minimal).
///
/// Gesammelt statt einzeln: jeder Fehler kommt in eine Warteschlange; 20 Sekunden nach dem letzten
/// neuen Fehler und erst, wenn kein Sammelvorgang mehr laeuft, startet EINE Sitzung fuer alle.
/// Eigene Sperrfrist je Programm (`pruefFehlerUebergebenAm`), getrennt von den Update-Fehlern.
/// Ohne jede Netzverbindung startet nichts: das repariert keine Sitzung.
@MainActor
final class PruefFehlerUebergabe {

    struct Fall {
        let eintrag: ProgrammEintrag
        let zeit: Date
        let meldung: String
        let installiert: String
        /// Das Kartenprotokoll beim Vormerken -- Rueckfall, falls die Karte inzwischen ersetzt wurde.
        let auszug: String
    }

    /// Hoechstens eine Sitzung je Programm und Stunde fuer Prueffehler.
    static let sperrfrist: TimeInterval = 3600

    /// Ruhe nach dem letzten neuen Fehler, bevor die Sitzung startet.
    private static let warteNanos: UInt64 = 20_000_000_000

    /// Protokollauszug fuer alle Programme zusammen (der Launcher reicht den Auftrag als ein Argument weiter).
    private static let protokollZeichen = 12000

    static func kartenText(_ meldung: String) -> String {
        "Prüffehler automatisch an Claude Code übergeben – die Sitzung läuft im Terminal und macht die Prüfung künftig robuster."
            + (meldung.istLeer ? "" : " Meldung: " + meldung)
    }

    private let einstellungen: Einstellungen
    private let sammelLaeuft: () -> Bool
    private let karteMelden: (String, String) -> Void
    private let protokollLesen: (String) -> String?

    private var warteschlange: [Fall] = []
    private var wecker: Task<Void, Never>?
    private var startLaeuft = false

    /// - Parameters:
    ///   - sammelLaeuft: laeuft gerade ein Sammelvorgang (Anfangspruefung, „Alle prüfen“, Sammellauf)?
    ///   - karteMelden: setzt den Status der Karte mit dieser Id.
    ///   - protokollLesen: das aktuelle Kartenprotokoll der Id (nil, wenn es die Karte nicht mehr gibt).
    init(einstellungen: Einstellungen, sammelLaeuft: @escaping () -> Bool,
         karteMelden: @escaping (String, String) -> Void, protokollLesen: @escaping (String) -> String?) {
        self.einstellungen = einstellungen
        self.sammelLaeuft = sammelLaeuft
        self.karteMelden = karteMelden
        self.protokollLesen = protokollLesen
    }

    /// Merkt eine gescheiterte Pruefung vor und stellt den Wecker neu.
    func melden(_ eintrag: ProgrammEintrag, _ ergebnis: PruefErgebnis, protokoll: String) {
        Netzwaechter.shared.beobachten()
        let fall = Fall(eintrag: eintrag, zeit: Date(),
                        meldung: ergebnis.meldung.istLeer ? "(keine Meldung)" : ergebnis.meldung,
                        installiert: ergebnis.installierteVersion,
                        auszug: String(protokoll.suffix(Self.protokollZeichen)))
        // Je Programm zaehlt nur der neueste Fehler.
        warteschlange.removeAll { $0.eintrag.id == eintrag.id }
        warteschlange.append(fall)
        Diagnose.ereignis(.info, "uebergabe", "prueffehler.vorgemerkt", fall.meldung, ["programm": eintrag.id])
        weckerStellen()
    }

    private func weckerStellen() {
        wecker?.cancel()
        wecker = Task { [weak self] in
            // Abgebrochen heisst: ein neuer Fehler kam dazu und hat einen neuen Wecker gestellt.
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

    /// Nimmt die Warteschlange heraus und startet in einer EIGENEN Aufgabe: ein neuer Fehler bricht nur den
    /// Wecker ab, nie einen laufenden Launcher-Start.
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

        // Dieselbe Reihe wie die Update-Fehler: die Launcher-Starts laufen nacheinander.
        while ClaudeUebergabe.automatischLaeuft {
            do { try await Task.sleep(nanoseconds: 500_000_000) } catch { return }
        }
        ClaudeUebergabe.automatischLaeuft = true
        defer { ClaudeUebergabe.automatischLaeuft = false }

        let jetzt = Date()
        let offen = faelle.filter { fall in
            guard let zuletzt = einstellungen.fuer(fall.eintrag.id).pruefFehlerUebergebenAm else { return true }
            return jetzt.timeIntervalSince(zuletzt) >= Self.sperrfrist
        }
        guard !offen.isEmpty else {
            Diagnose.ereignis(.info, "uebergabe", "prueffehler.gesperrt", "Sperrfrist für Prüffehler läuft", ["programme": ids])
            return
        }

        // VOR dem Start vorgemerkt: endet die App waehrend des Launcher-Starts, gilt die Sperrfrist trotzdem.
        let bisher = offen.map { einstellungen.fuer($0.eintrag.id).pruefFehlerUebergebenAm }
        for fall in offen { einstellungen.fuer(fall.eintrag.id).pruefFehlerUebergebenAm = jetzt }
        einstellungen.speichern()

        let kennung = offen.count == 1 ? offen[0].eintrag.id : "sammel-\(offen.count)"
        let text = auftragText(offen)
        let (problem, vielleicht) = await ClaudeUebergabe.sitzungStarten(
            dateiName: "prueffehler-\(kennung)", sitzungsName: "Updater-Zentrale: Prüffehler \(kennung)", auftrag: text)
        Diagnose.ereignis(problem == nil ? .info : .warnung, "uebergabe", "prueffehler.start", problem ?? "Sitzung gestartet",
                          ["programme": offen.map(\.eintrag.id).joined(separator: ",")])

        // Unklar (der Launcher antwortete nicht): die Sitzung laeuft meist doch, die Marke bleibt.
        if problem != nil && !vielleicht {
            for (fall, alt) in zip(offen, bisher) { einstellungen.fuer(fall.eintrag.id).pruefFehlerUebergebenAm = alt }
            einstellungen.speichern()
        }
        for fall in offen {
            if problem == nil || vielleicht {
                karteMelden(fall.eintrag.id, Self.kartenText(fall.meldung))
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
            t += "Meldung: \(fall.meldung)\n"
            t += "Tagesprotokoll (Zeilen mit [\(eintrag.id)]): \(Protokollierung.tagesDatei(fall.zeit))\n"
            t += "Weitere Protokolle (Diagnose-JSONL, verlauf.jsonl): \(Protokollierung.ordner)\n"

            // Frisch gelesen: die letzten Zeilen der Pruefung kommen asynchron auf der Karte an.
            let protokoll = protokollLesen(eintrag.id) ?? fall.auszug
            let auszug = Self.ende(protokoll, budget)
            if !auszug.istLeer {
                t += "\nProtokollauszug der Karte" + (auszug.count < protokoll.trimmed.count ? " (gekürzt, nur das Ende)" : "") + ":\n"
                t += "----\n" + auszug + "\n----\n"
            }
        }
        return t
    }

    /// Das Ende eines Protokolls in hoechstens `budget` Zeichen, an einer Zeilengrenze abgeschnitten.
    private static func ende(_ text: String, _ budget: Int) -> String {
        let sauber = text.trimmed
        guard sauber.count > budget else { return sauber }
        let rest = String(sauber.suffix(max(budget - 2, 200)))
        if let umbruch = rest.firstIndex(of: "\n"), rest.distance(from: rest.startIndex, to: umbruch) < rest.count / 2 {
            return "… " + String(rest[rest.index(after: umbruch)...])
        }
        return "… " + rest
    }
}

/// Netzstatus fuer die Prueffehler-Uebergabe. Beim ersten Fehler gestartet und danach dauerhaft gelesen:
/// `currentPath` direkt nach `start()` ist noch nicht verlaesslich. Nur ein sicheres „kein Netz“
/// (`.unsatisfied`) haelt die Sitzung auf; ein noch unbekannter Stand gilt als verbunden.
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
