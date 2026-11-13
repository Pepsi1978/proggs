import Foundation

/// Ergebnis eines Abgleichs. `geholtOk`: git fetch hat geantwortet, origin/main ist frisch.
/// `gezogenOk`: der Checkout steht auf dem Stand von origin (vorgespult oder schon dort).
struct AbgleichErgebnis: Sendable {
    let geholtOk: Bool
    let gezogenOk: Bool
    let meldung: String
    let zeit: Date
}

/// Bringt ~/proggs auf den Stand von origin, bevor die eigenen Werkzeuge geprueft werden: fetch,
/// dann nur Vorspulen -- nie ein Merge, nie ein Rebase, nie ein Stash. Die eigenen Werkzeuge werden
/// aus diesem Checkout gebaut; gegen einen alten Checkout liesse sich nur "es gibt Commits" sagen,
/// nicht welche Version wartet. 1:1-Port von Services/RepoAbgleich.cs.
///
/// Ein Lauf bedient alle Aufrufer: vier Karten, die gleichzeitig pruefen, teilen sich einen einzigen
/// fetch, und ein Ergebnis bleibt zwei Minuten gueltig.
actor RepoAbgleich {
    static let shared = RepoAbgleich()

    private static let gueltig: TimeInterval = 120
    private var lauf: Task<AbgleichErgebnis, Never>?
    private var letztes: AbgleichErgebnis?

    func abgleichen(erzwingen: Bool = false) async -> AbgleichErgebnis {
        if let lauf { return await lauf.value }
        if !erzwingen, let letztes, letztes.geholtOk, Date().timeIntervalSince(letztes.zeit) < Self.gueltig {
            return letztes
        }
        let neu = Task { await Self.ausfuehren(Pfade.repoWurzel) }
        lauf = neu
        let ergebnis = await neu.value
        letztes = ergebnis
        lauf = nil
        return ergebnis
    }

    private static func ausfuehren(_ wurzel: String) async -> AbgleichErgebnis {
        let holen = await Kommandozeile.ausfuehren("/usr/bin/git", ["fetch", "--quiet"],
                                                   zeitlimit: 120, arbeitsverzeichnis: wurzel)
        if holen.abgelaufen || holen.exitCode != 0 {
            return melden(AbgleichErgebnis(geholtOk: false, gezogenOk: false,
                                           meldung: "git fetch fehlgeschlagen (\(holen.abgelaufen ? "Zeitlimit" : "Code \(holen.exitCode)")): "
                                                  + kurz(holen.ausgabe), zeit: Date()))
        }
        // Nur Vorspulen: mit lokalen Aenderungen im Weg oder abweichendem Verlauf verweigert git und
        // aendert nichts. Die Karten zaehlen dann weiter die wartenden Commits.
        let ziehen = await vorspulen(wurzel, nutzerStart: false)
        if ziehen.abgelaufen || ziehen.exitCode != 0 {
            return melden(AbgleichErgebnis(geholtOk: true, gezogenOk: false,
                                           meldung: "Repo ließ sich nicht vorspulen (\(vorspulGrund(ziehen))): "
                                                  + kurz(ziehen.ausgabe), zeit: Date()))
        }
        return melden(AbgleichErgebnis(geholtOk: true, gezogenOk: true,
                                       meldung: "Repo ist auf dem Stand von origin.", zeit: Date()))
    }

    /// Ein grosser Rueckstand schreibt tausende Dateien und laedt LFS-Dateien. Ein kurzes Limit
    /// wuergte genau diesen Lauf ab und hinterliesse den halben Zustand, den die Schritte unten reparieren.
    static let vorspulZeitlimit: TimeInterval = 15 * 60

    /// Eine juengere Sperre kann einem gerade startenden git gehoeren.
    static let sperreMindestalter: TimeInterval = 120

    /// Spult auf den Upstream vor. Eine verwaiste index.lock wird immer entfernt. Was den Index anfasst
    /// oder lokale Aenderungen beiseitelegt, laeuft nur bei einem vom Nutzer gestarteten Update: Die
    /// Pruefung davor laeuft auch unbeaufsichtigt, waehrend parallele Sitzungen Dateien vorgemerkt haben.
    /// `nutzerStart`: Erst ein abgebrochenes Vorspulen vollenden, dann erst der Autostash; umgekehrt
    /// staesche er tausende Scheinaenderungen. 1:1-Port von RepoAbgleich.VorspulenAsync.
    static func vorspulen(_ wurzel: String, nutzerStart: Bool,
                          protokoll: Fortschritt? = nil) async -> BefehlErgebnis {
        await sperreFreimachen(wurzel, protokoll)

        let git = "/usr/bin/git"
        let zug = await Kommandozeile.ausfuehren(git, ["merge", "--ff-only", "--quiet", "@{u}"],
                                                 zeitlimit: vorspulZeitlimit, arbeitsverzeichnis: wurzel)
        if zug.exitCode == 0 || zug.abgelaufen || !nutzerStart
            || zug.ausgabe.localizedCaseInsensitiveContains("index.lock") {
            return zug
        }

        if let repariert = await halbesVorspulenVollenden(wurzel, protokoll) { return repariert }

        let mitStash = await Kommandozeile.ausfuehren(git, ["merge", "--ff-only", "--quiet", "--autostash", "@{u}"],
                                                      zeitlimit: vorspulZeitlimit, arbeitsverzeichnis: wurzel)
        // git meldet hier Erfolg: Der Baum steht auf dem Upstream, die lokalen Aenderungen warten im Stash.
        if mitStash.exitCode == 0, mitStash.ausgabe.localizedCaseInsensitiveContains(stashKonflikt) {
            await laufzeitdateienEntkonflikten(wurzel, protokoll)
            return BefehlErgebnis(exitCode: 1, ausgabe: mitStash.ausgabe, abgelaufen: false)
        }
        return mitStash
    }

    private static let stashKonflikt = "safe in the stash"

    /// Dateien, die der OpenLauncher zur Laufzeit neu schreibt (models.json) und die zugleich versioniert
    /// sind. Kollidiert der Autostash mit ihnen, bleiben Konfliktmarker stehen; der Launcher kann das
    /// nicht lesen, faellt auf seine Vorgaben zurueck und ueberschreibt die Modell-Liste des Nutzers.
    /// 1:1-Port von RepoAbgleich.Laufzeitdateien (Windows 1.3.80).
    private static let laufzeitdateien: Set<String> = ["models.json"]

    /// Nach einem kollidierenden Autostash gewinnt bei Laufzeitdateien der eigene Stand: die Stash-Seite
    /// ("theirs" beim Anwenden eines Stashs) nehmen und aus dem Index nehmen. Andere Konfliktdateien
    /// behalten ihre Marker, der Stash bleibt liegen.
    private static func laufzeitdateienEntkonflikten(_ wurzel: String, _ protokoll: Fortschritt?) async {
        let git = "/usr/bin/git"
        let offen = await Kommandozeile.ausfuehren(git, ["diff", "--name-only", "--diff-filter=U"],
                                                   zeitlimit: 120, arbeitsverzeichnis: wurzel)
        guard offen.exitCode == 0 else { return }
        for zeile in pfade(offen.ausgabe) where laufzeitdateien.contains((zeile as NSString).lastPathComponent.lowercased()) {
            let lokal = await Kommandozeile.ausfuehren(git, ["checkout", "--theirs", "--", zeile],
                                                       zeitlimit: 120, arbeitsverzeichnis: wurzel)
            guard lokal.exitCode == 0 else { continue }
            _ = await Kommandozeile.ausfuehren(git, ["reset", "--quiet", "--", zeile],
                                               zeitlimit: 120, arbeitsverzeichnis: wurzel)
            berichten(protokoll, "repo.laufzeitdatei", "\(zeile): Konflikt mit dem Autostash, der lokale Stand bleibt erhalten.")
        }
    }

    /// Der echte Grund einer Verweigerung, nicht die uebliche Vermutung.
    static func vorspulGrund(_ lauf: BefehlErgebnis) -> String {
        if lauf.abgelaufen { return "Zeitlimit überschritten" }
        if lauf.ausgabe.localizedCaseInsensitiveContains(stashKonflikt) {
            return "lokale Änderungen kollidieren mit den neuen Commits, sie liegen jetzt im Git-Stash"
        }
        return lauf.ausgabe.localizedCaseInsensitiveContains("index.lock")
            ? "Git-Sperre .git/index.lock: ein anderer Git-Prozess läuft, oder ein abgebrochener hat sie hinterlassen"
            : "lokale Änderungen oder abweichender Verlauf"
    }

    static func sperreVerwaist(sperreZeit: Date, jetzt: Date, gitLaeuft: Bool) -> Bool {
        !gitLaeuft && jetzt.timeIntervalSince(sperreZeit) >= sperreMindestalter
    }

    private static func sperreFreimachen(_ wurzel: String, _ protokoll: Fortschritt?) async {
        let sperre = (wurzel as NSString).appendingPathComponent(".git/index.lock")
        let bis = Date().addingTimeInterval(60)
        while let attribute = try? FileManager.default.attributesOfItem(atPath: sperre) {
            let zeit = attribute[.modificationDate] as? Date ?? Date()
            let suche = await Kommandozeile.ausfuehren("/usr/bin/pgrep", ["-x", "git"], zeitlimit: 10)
            // pgrep antwortet 0 bei Treffern und 1 ohne; alles andere heisst: unbekannt, also nichts loeschen.
            if suche.exitCode == 1, sperreVerwaist(sperreZeit: zeit, jetzt: Date(), gitLaeuft: false) {
                guard (try? FileManager.default.removeItem(atPath: sperre)) != nil else { return }
                berichten(protokoll, "repo.sperre", "Verwaiste Git-Sperre .git/index.lock entfernt.")
                return
            }
            if Date() >= bis { return }
            try? await Task.sleep(nanoseconds: 3_000_000_000)
        }
    }

    /// Darunter laesst sich ein abgebrochener Lauf nicht von normaler lokaler Arbeit trennen; der Autostash reicht.
    private static let halbMindestens = 10

    /// git schreibt erst die Dateien und setzt HEAD zuletzt. Ein dazwischen abgebrochener Lauf hinterlaesst
    /// Dateien, die zum Upstream passen und von HEAD abweichen; von Hand entstehen die nicht in dieser Zahl.
    /// `geschrieben`: weicht von HEAD ab, passt zum Upstream. `eigeneArbeit`: weicht von beiden ab.
    static func istHalbesVorspulen(geschrieben: Int, eigeneArbeit: Int) -> Bool {
        geschrieben >= halbMindestens && geschrieben > eigeneArbeit
    }

    /// Vollendet ein abgebrochenes Vorspulen, egal wo es starb. Jede Datei zaehlt nach ihrem Inhalt: Gleicht
    /// sie noch HEAD, wurde sie nur nicht mehr geschrieben und kommt aus dem Upstream; weicht sie von beiden
    /// ab, ist sie lokale Arbeit und bleibt. HEAD wandert zuletzt, ein Fehler unterwegs hinterlaesst also den
    /// Zustand, in dem der naechste Lauf es erneut versucht. nil: kein abgebrochenes Vorspulen.
    private static func halbesVorspulenVollenden(_ wurzel: String, _ protokoll: Fortschritt?) async -> BefehlErgebnis? {
        let git = "/usr/bin/git"
        let limit: TimeInterval = 300
        let vorfahr = await Kommandozeile.ausfuehren(git, ["merge-base", "--is-ancestor", "HEAD", "@{u}"],
                                                     zeitlimit: limit, arbeitsverzeichnis: wurzel)
        if vorfahr.exitCode != 0 { return nil }

        // Vorgemerktes gehoert einer parallelen Sitzung, ein Reset naehme es aus dem Index.
        let vorgemerkt = await Kommandozeile.ausfuehren(git, ["diff", "--cached", "--quiet"],
                                                        zeitlimit: limit, arbeitsverzeichnis: wurzel)
        if vorgemerkt.exitCode != 0 { return nil }

        let nurPfade = ["-c", "core.quotePath=false", "diff", "--name-only"]
        let zuHead = await Kommandozeile.ausfuehren(git, nurPfade, zeitlimit: limit, arbeitsverzeichnis: wurzel)
        if zuHead.exitCode != 0 { return nil }
        let geaendert = pfade(zuHead.ausgabe)
        if geaendert.count < halbMindestens { return nil }

        // Ein eigener Index vergleicht den Arbeitsbaum mit dem Upstream, ohne den echten anzufassen.
        let tempIndex = (NSTemporaryDirectory() as NSString).appendingPathComponent("updaterzentrale-index-\(UUID().uuidString)")
        defer {
            try? FileManager.default.removeItem(atPath: tempIndex)
            try? FileManager.default.removeItem(atPath: tempIndex + ".lock")
        }
        let umgebung = ["GIT_INDEX_FILE": tempIndex]
        let lesen = await Kommandozeile.ausfuehren(git, ["read-tree", "@{u}"], zeitlimit: limit,
                                                   arbeitsverzeichnis: wurzel, zusatzUmgebung: umgebung)
        if lesen.exitCode != 0 { return nil }
        let vergleich = await Kommandozeile.ausfuehren(git, nurPfade, zeitlimit: limit,
                                                       arbeitsverzeichnis: wurzel, zusatzUmgebung: umgebung)
        if vergleich.exitCode != 0 { return nil }
        let zuUpstream = pfade(vergleich.ausgabe)

        let eigeneArbeit = geaendert.intersection(zuUpstream).count
        guard istHalbesVorspulen(geschrieben: geaendert.count - eigeneArbeit, eigeneArbeit: eigeneArbeit) else { return nil }

        func vollerPfad(_ pfad: String) -> String { (wurzel as NSString).appendingPathComponent(pfad) }
        let nachholen = zuUpstream.filter {
            !geaendert.contains($0) || !FileManager.default.fileExists(atPath: vollerPfad($0))
        }
        if !nachholen.isEmpty {
            let liste = (NSTemporaryDirectory() as NSString).appendingPathComponent("updaterzentrale-pfade-\(UUID().uuidString)")
            defer { try? FileManager.default.removeItem(atPath: liste) }
            do {
                try nachholen.joined(separator: "\n").write(toFile: liste, atomically: true, encoding: .utf8)
            } catch {
                return BefehlErgebnis(exitCode: 1, ausgabe: "Pfadliste nicht schreibbar: \(error.localizedDescription)", abgelaufen: false)
            }
            let holen = await Kommandozeile.ausfuehren(git, ["checkout", "@{u}", "--pathspec-from-file=\(liste)"],
                                                       zeitlimit: vorspulZeitlimit, arbeitsverzeichnis: wurzel)
            if holen.exitCode != 0 { return holen }
        }

        // Upstream entfernt, hier unberuehrt: Der abgebrochene Lauf kam nur nicht mehr dazu.
        let entfernt = await Kommandozeile.ausfuehren(git, ["-c", "core.quotePath=false", "diff", "--name-only", "--diff-filter=D", "HEAD", "@{u}"],
                                                      zeitlimit: limit, arbeitsverzeichnis: wurzel)
        if entfernt.exitCode == 0 {
            for pfad in pfade(entfernt.ausgabe).subtracting(geaendert) {
                try? FileManager.default.removeItem(atPath: vollerPfad(pfad))
            }
        }

        let zurueck = await Kommandozeile.ausfuehren(git, ["reset", "-q", "--mixed", "@{u}"],
                                                     zeitlimit: vorspulZeitlimit, arbeitsverzeichnis: wurzel)
        if zurueck.exitCode != 0 { return zurueck }

        let meldung = "Abgebrochenes Vorspulen vollendet: HEAD und Index auf den Stand von origin gesetzt, "
                    + "\(nachholen.count) Datei(en) nachgeholt, \(eigeneArbeit) lokale Änderung(en) behalten."
        berichten(protokoll, "repo.halbes-vorspulen", meldung)
        return BefehlErgebnis(exitCode: 0, ausgabe: meldung, abgelaufen: false)
    }

    /// Die Ausgabe enthaelt auch stderr; nur Zeilen, die keine Meldung von git sein koennen, sind Pfade.
    private static func pfade(_ text: String) -> Set<String> {
        Set(text.split(separator: "\n")
            .map { $0.trimmingCharacters(in: CharacterSet(charactersIn: "\r\" \t")) }
            .filter { !$0.isEmpty && !$0.hasPrefix("warning: ") && !$0.hasPrefix("hint: ") })
    }

    private static func berichten(_ protokoll: Fortschritt?, _ typ: String, _ meldung: String) {
        protokoll?.berichte(meldung)
        Diagnose.ereignis(.warnung, "repo", typ, meldung)
    }

    private static func melden(_ ergebnis: AbgleichErgebnis) -> AbgleichErgebnis {
        Diagnose.ereignis(ergebnis.gezogenOk ? .info : .warnung, "repo", "repo.abgleich", ergebnis.meldung)
        return ergebnis
    }

    private static func kurz(_ text: String) -> String {
        let zeile = text.replacingOccurrences(of: "\n", with: " ").trimmed
        return zeile.count > 300 ? String(zeile.prefix(300)) + " …" : zeile
    }
}
