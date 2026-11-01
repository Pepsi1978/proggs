import Foundation

/// Schuetzt global per npm installierte Kommandozeilen-Werkzeuge vor einem abgebrochenen Update.
/// npm raeumt das alte Paket samt Verknuepfung weg, BEVOR es das neue laedt. Endet der Lauf in dieser
/// Luecke hart (Zeitlimit, Abbruch), bleibt kein Rollback: das Werkzeug fehlt. Deshalb liegt der
/// langsame Teil vorn, wo ein Abbruch nichts zerstoert, und eine zerstoerte Installation wird sofort
/// wiederhergestellt. Port von Providers/NpmGlobal.cs; siehe
/// bugs/bug-cases/2026-11-01-updatezentrale-npm-kill-zerstoert-cli.md.
enum NpmGlobal {

    /// Zeigt die Programmdatei in einen node_modules-Ordner? Native Installationen bleiben aussen vor.
    static func betrifft(_ eintrag: ProgrammEintrag, _ programmDatei: String) -> Bool {
        guard let paket = eintrag.npmPaket, !paket.istLeer, !programmDatei.istLeer else { return false }
        return URL(fileURLWithPath: programmDatei).resolvingSymlinksInPath().path.contains("/node_modules/")
    }

    /// Waechst, solange npm laedt: cacache schreibt eine Tarball laufend nach _cacache/tmp. Macht das
    /// Zeitlimit zur Ruhezeit, ein langsamer, aber laufender Download wird nicht mehr abgeschossen.
    private static let downloadGroesse: @Sendable () -> Int64 = {
        let ordner = (NSHomeDirectory() as NSString).appendingPathComponent(".npm/_cacache/tmp")
        guard let dateien = FileManager.default.enumerator(atPath: ordner) else { return 0 }
        var summe: Int64 = 0
        for case let name as String in dateien {
            let pfad = (ordner as NSString).appendingPathComponent(name)
            summe += (try? FileManager.default.attributesOfItem(atPath: pfad)[.size] as? NSNumber)?.int64Value ?? 0
        }
        return summe
    }

    /// Laedt die neue Version in den npm-Cache, solange die alte noch unangetastet ist.
    /// Nil = weiter mit dem Update; sonst das Ergebnis, das den Lauf beendet.
    static func vorabLaden(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis? {
        guard let paket = eintrag.npmPaket, FileManager.default.isExecutableFile(atPath: Pfade.npm) else { return nil }

        let ziel = (NSTemporaryDirectory() as NSString).appendingPathComponent("uz-npmvorab-" + UUID().uuidString)
        guard (try? FileManager.default.createDirectory(atPath: ziel, withIntermediateDirectories: true)) != nil else { return nil }
        defer { try? FileManager.default.removeItem(atPath: ziel) }

        protokoll.berichte("Lädt \(paket) vorab in den npm-Cache, die installierte Version bleibt dabei unberührt …")
        let lauf = await Kommandozeile.ausfuehren(
            Pfade.npm,
            ["install", "--prefix", ziel, paket + "@latest", "--ignore-scripts", "--no-audit", "--no-fund",
             "--no-package-lock", "--no-bin-links", "--loglevel=error"],
            zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60),
            arbeitsverzeichnis: (Pfade.npm as NSString).deletingLastPathComponent,
            lebenszeichen: downloadGroesse)

        if lauf.abgelaufen {
            protokoll.berichte(lauf.ausgabe)
            let was = lauf.abgebrochen ? "Abgebrochen" : "Der Download kommt nicht voran"
            return PruefErgebnis(zustand: .fehler,
                                 meldung: "\(was) – \(eintrag.name) wurde nicht angefasst und bleibt unverändert nutzbar.",
                                 protokoll: lauf.ausgabe)
        }
        if lauf.exitCode != 0 {
            protokoll.berichte("Vorab-Download endete mit Code \(lauf.exitCode), das Update läuft ohne ihn weiter: \(lauf.ausgabe)")
        }
        return nil
    }

    /// Stellt ein vom abgebrochenen Update zerstoertes Werkzeug wieder her. Laeuft auch nach einem Klick
    /// auf Abbrechen zu Ende (Marke hier geleert): ein fehlendes Werkzeug ist schlimmer als die kurze Wartezeit.
    static func wiederherstellen(_ eintrag: ProgrammEintrag, _ programmDatei: String, _ protokoll: Fortschritt) async -> Bool {
        guard let paket = eintrag.npmPaket, FileManager.default.isExecutableFile(atPath: Pfade.npm) else {
            protokoll.berichte("Wiederherstellung nicht möglich: npm wurde nicht gefunden.")
            return false
        }

        protokoll.berichte("\((programmDatei as NSString).lastPathComponent) fehlt nach dem abgebrochenen npm-Lauf – stelle \(paket) wieder her …")
        let lauf = await AbbruchMarke.$aktuell.withValue(nil) {
            await Kommandozeile.ausfuehren(
                Pfade.npm, ["install", "-g", paket + "@latest", "--no-audit", "--no-fund"],
                zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60),
                arbeitsverzeichnis: (Pfade.npm as NSString).deletingLastPathComponent,
                lebenszeichen: downloadGroesse)
        }
        protokoll.berichte(lauf.ausgabe)
        return !lauf.abgelaufen && lauf.exitCode == 0 && FileManager.default.fileExists(atPath: programmDatei)
    }
}
