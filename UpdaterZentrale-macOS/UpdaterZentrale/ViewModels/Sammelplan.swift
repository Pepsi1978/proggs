import Foundation

/// Die Reihenfolge eines Sammellaufs, an EINER Stelle entschieden. Port von ViewModels/Sammelplan.cs.
///
/// Die Oberflaeche hat zwei unabhaengige Knoepfe -- die CLI-kritischen Updates und alles andere --,
/// und jeder fuehrt seinen Teil dieser Reihenfolge aus:
/// 1. Eigene Werkzeuge (Repo-Skripte): zuerst die Updater-Zentrale selbst -- sie startet die App neu,
///    und die neue Fassung setzt den Lauf fort --, dann OpenLauncher, TVO, CVO. Sie fragen Ja/Nein,
///    und direkt nach dem Klick sitzt man noch davor.
/// 2. CLI-kritische Updates (iTerm2, Node.js, JDK, Android SDK …): sie koennen laufende
///    CLI-Sitzungen beenden; sie gehoeren zum CLI-Knopf.
/// 3. Alles andere.
/// Innerhalb einer Phase geht ein Eintrag, dessen Voraussetzungen ("nach") im selben Lauf stehen,
/// nach ihnen; sonst das kuerzeste erwartete Update zuerst, damit die grossen Installer (Android
/// Studio, Docker, .NET) die schnellen nicht aufhalten. Hauptversionsspruenge mit
/// "hauptversionNurPerKlick" bleiben ganz draussen.
@MainActor
enum Sammelplan {
    static let phaseEigene = 1
    static let phaseCli = 2
    static let phaseRest = 3
    /// Eintraege mit "zuletzt" laufen nach allen Phasen -- ein eigener Schritt ganz am Ende.
    static let phaseZuletzt = 4

    /// Die Id der Updater-Zentrale selbst im Mac-Katalog (unter Windows "updatezentrale").
    nonisolated static let selbstId = "updater-zentrale"

    struct Schritt {
        let programm: ProgrammViewModel
        let phase: Int
        let parallel: Bool
    }

    static func planen(_ offen: [ProgrammViewModel]) -> [Schritt] {
        let liste = offen.filter { !$0.istHauptversionsSprung }
        let ids = Set(liste.map { $0.eintrag.id.lowercased() })
        // Eine Ebene genuegt fuer die Ketten des Katalogs (JDK → SDK, Node → npm).
        func wartet(_ p: ProgrammViewModel) -> Bool { p.eintrag.nach.contains { ids.contains($0.lowercased()) } }

        return liste
            // Ein Schritt, der auf einen anderen im Lauf wartet, laeuft nie neben ihm parallel.
            .map { p in Schritt(programm: p, phase: p.eintrag.zuletzt ? phaseZuletzt : phaseVon(p),
                                parallel: darfParallel(p) && !p.eintrag.zuletzt && !wartet(p)) }
            .sorted { a, b in
                if a.phase != b.phase { return a.phase < b.phase }
                let selbstA = istSelbst(a.programm) ? 0 : 1, selbstB = istSelbst(b.programm) ? 0 : 1
                if selbstA != selbstB { return selbstA < selbstB }
                let wartetA = wartet(a.programm) ? 1 : 0, wartetB = wartet(b.programm) ? 1 : 0
                if wartetA != wartetB { return wartetA < wartetB }
                if a.programm.erwarteteDauerSekunden != b.programm.erwarteteDauerSekunden {
                    return a.programm.erwarteteDauerSekunden < b.programm.erwarteteDauerSekunden
                }
                return a.programm.katalogIndex < b.programm.katalogIndex
            }
    }

    static func istSelbst(_ p: ProgrammViewModel) -> Bool {
        p.eintrag.id.caseInsensitiveCompare(selbstId) == .orderedSame
    }

    static func phaseVon(_ p: ProgrammViewModel) -> Int {
        if p.eintrag.art.lowercased() == "reposkript" { return phaseEigene }
        return p.kannCliBeenden ? phaseCli : phaseRest
    }

    /// Nur Eintraege, die der Katalog "parallel" nennt, laufen neben der seriellen Bahn. Seriell
    /// bleibt alles, was eine Sperre, einen Baum oder eine Laufzeit teilt -- brew (eine Sperre fuer
    /// alles), npm, die Skripte -- und alles, was Ja/Nein fragt oder eine CLI-Sitzung beenden kann,
    /// egal was der Katalog sagt.
    static func darfParallel(_ p: ProgrammViewModel) -> Bool {
        let art = p.eintrag.art.lowercased()
        return p.eintrag.parallel && !p.kannCliBeenden && (art == "cli" || art == "opencodesitzung")
    }

    static func phasenName(_ phase: Int) -> String {
        switch phase {
        case phaseEigene: return "Eigene Werkzeuge"
        case phaseCli: return "CLI-kritisch"
        case phaseZuletzt: return "Zum Schluss"
        default: return "Übrige"
        }
    }
}
