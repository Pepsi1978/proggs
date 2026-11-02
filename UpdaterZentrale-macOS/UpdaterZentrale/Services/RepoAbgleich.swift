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
        let ziehen = await vorspulen(wurzel, autostash: false)
        if ziehen.abgelaufen || ziehen.exitCode != 0 {
            return melden(AbgleichErgebnis(geholtOk: true, gezogenOk: false,
                                           meldung: "Repo ließ sich nicht vorspulen (\(vorspulGrund(ziehen.ausgabe))): "
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

    /// Spult auf den Upstream vor und repariert, was ein abgebrochener Lauf hinterlassen hat: eine
    /// verwaiste index.lock und einen Arbeitsbaum, der schon auf dem neuen Stand ist, waehrend HEAD
    /// noch auf dem alten steht. 1:1-Port von RepoAbgleich.VorspulenAsync.
    /// `autostash` nur fuer ein vom Nutzer gestartetes Update, die Pruefung davor stasht nie.
    static func vorspulen(_ wurzel: String, autostash: Bool,
                          protokoll: Fortschritt? = nil) async -> BefehlErgebnis {
        await sperreFreimachen(wurzel, protokoll)

        let argumente = ["merge", "--ff-only", "--quiet"] + (autostash ? ["--autostash"] : []) + ["@{u}"]
        let zug = await Kommandozeile.ausfuehren("/usr/bin/git", argumente, zeitlimit: vorspulZeitlimit,
                                                 arbeitsverzeichnis: wurzel)
        if zug.exitCode == 0 || zug.abgelaufen { return zug }
        if await halbesVorspulenVollenden(wurzel, protokoll) {
            return BefehlErgebnis(exitCode: 0, ausgabe: "Abgebrochenes Vorspulen vollendet.", abgelaufen: false)
        }
        return zug
    }

    /// Der echte Grund einer Verweigerung, nicht die uebliche Vermutung.
    static func vorspulGrund(_ ausgabe: String) -> String {
        ausgabe.localizedCaseInsensitiveContains("index.lock")
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

    /// Der Arbeitsbaum steht auf dem Upstream, HEAD und Index noch auf dem alten Stand: git schreibt
    /// erst die Dateien und setzt HEAD zuletzt, ein dazwischen abgebrochener Lauf hinterlaesst genau das.
    static func istHalbesVorspulen(abweichungZuHead: Int, abweichungZuUpstream: Int) -> Bool {
        abweichungZuHead >= 50 && abweichungZuUpstream * 10 <= abweichungZuHead
    }

    /// reset --mixed setzt HEAD und Index und laesst den Arbeitsbaum in Ruhe; uncommittete Aenderungen
    /// bleiben also erhalten. Nur noch nicht geschriebene Dateien kommen aus dem Upstream.
    private static func halbesVorspulenVollenden(_ wurzel: String, _ protokoll: Fortschritt?) async -> Bool {
        let git = "/usr/bin/git"
        let limit: TimeInterval = 300
        let vorfahr = await Kommandozeile.ausfuehren(git, ["merge-base", "--is-ancestor", "HEAD", "@{u}"],
                                                     zeitlimit: limit, arbeitsverzeichnis: wurzel)
        if vorfahr.exitCode != 0 { return false }
        let zuHead = await Kommandozeile.ausfuehren(git, ["status", "--porcelain", "--no-renames", "-uno"],
                                                    zeitlimit: limit, arbeitsverzeichnis: wurzel)
        if zuHead.exitCode != 0 { return false }

        // Ein eigener Index vergleicht den Arbeitsbaum mit dem Upstream, ohne den echten anzufassen.
        let tempIndex = (NSTemporaryDirectory() as NSString).appendingPathComponent("updaterzentrale-index-\(UUID().uuidString)")
        defer {
            try? FileManager.default.removeItem(atPath: tempIndex)
            try? FileManager.default.removeItem(atPath: tempIndex + ".lock")
        }
        let umgebung = ["GIT_INDEX_FILE": tempIndex]
        let lesen = await Kommandozeile.ausfuehren(git, ["read-tree", "@{u}"], zeitlimit: limit,
                                                   arbeitsverzeichnis: wurzel, zusatzUmgebung: umgebung)
        if lesen.exitCode != 0 { return false }
        let zuUpstream = await Kommandozeile.ausfuehren(git, ["-c", "core.quotePath=false", "status", "--porcelain", "--no-renames", "-uno"],
                                                        zeitlimit: limit, arbeitsverzeichnis: wurzel, zusatzUmgebung: umgebung)
        if zuUpstream.exitCode != 0 { return false }

        // Zweite Spalte: Arbeitsbaum gegen Upstream. "AD": neue Upstream-Datei, noch nicht auf der Platte.
        let abweichend = zeilen(zuUpstream.ausgabe).filter { $0.count > 3 && Array($0)[1] != " " }
        guard istHalbesVorspulen(abweichungZuHead: zeilen(zuHead.ausgabe).count,
                                 abweichungZuUpstream: abweichend.count) else { return false }

        let zurueck = await Kommandozeile.ausfuehren(git, ["reset", "-q", "--mixed", "@{u}"],
                                                     zeitlimit: vorspulZeitlimit, arbeitsverzeichnis: wurzel)
        if zurueck.exitCode != 0 { return false }
        berichten(protokoll, "repo.halbes-vorspulen",
                  "Abgebrochenes Vorspulen vollendet: HEAD und Index auf den Stand von origin gesetzt, Arbeitsbaum unverändert.")

        for zeile in abweichend where zeile.hasPrefix("AD ") {
            let pfad = String(zeile.dropFirst(3)).trimmingCharacters(in: CharacterSet(charactersIn: "\" \t"))
            let holen = await Kommandozeile.ausfuehren(git, ["checkout", "@{u}", "--", pfad],
                                                       zeitlimit: vorspulZeitlimit, arbeitsverzeichnis: wurzel)
            if holen.exitCode != 0 {
                berichten(protokoll, "repo.halbes-vorspulen", "Datei fehlt weiter: \(pfad) – \(kurz(holen.ausgabe))")
            }
        }
        return true
    }

    private static func zeilen(_ text: String) -> [String] {
        text.split(separator: "\n").map { $0.trimmingCharacters(in: CharacterSet(charactersIn: "\r")) }.filter { !$0.isEmpty }
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
