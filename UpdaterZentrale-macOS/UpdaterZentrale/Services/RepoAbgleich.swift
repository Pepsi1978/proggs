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
        let ziehen = await Kommandozeile.ausfuehren("/usr/bin/git", ["merge", "--ff-only", "--quiet", "@{u}"],
                                                    zeitlimit: 120, arbeitsverzeichnis: wurzel)
        if ziehen.abgelaufen || ziehen.exitCode != 0 {
            return melden(AbgleichErgebnis(geholtOk: true, gezogenOk: false,
                                           meldung: "Repo ließ sich nicht vorspulen (lokale Änderungen oder abweichender Verlauf): "
                                                  + kurz(ziehen.ausgabe), zeit: Date()))
        }
        return melden(AbgleichErgebnis(geholtOk: true, gezogenOk: true,
                                       meldung: "Repo ist auf dem Stand von origin.", zeit: Date()))
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
