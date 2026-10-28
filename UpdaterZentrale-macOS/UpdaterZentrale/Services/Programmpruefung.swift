import Foundation

/// Prueft, ob ein Programm nach einem abgebrochenen Update noch vollstaendig da ist. Ein Cask-Wechsel,
/// der mitten im Verschieben endet, laesst sonst eine fehlende oder leere App zurueck, und die Karte
/// meldet nur "Zeitlimit".
enum Programmpruefung {

    /// Nil = vollstaendig (oder nichts zu pruefen); sonst was fehlt, als Satz fuer die Meldung.
    static func vollstaendig(_ eintrag: ProgrammEintrag) -> String? {
        let fm = FileManager.default
        let ziel = eintrag.zielPfad
        guard !ziel.istLeer else { return nil }
        guard fm.fileExists(atPath: ziel) else {
            return "\(eintrag.name) fehlt nach dem Update (\(ziel)) – bitte neu installieren."
        }
        guard eintrag.istAppBuendel else { return nil }
        let plist = (ziel as NSString).appendingPathComponent("Contents/Info.plist")
        guard let info = NSDictionary(contentsOfFile: plist),
              let programm = info["CFBundleExecutable"] as? String, !programm.istLeer else {
            return "\(eintrag.name) ist nach dem Update unvollständig (Info.plist fehlt oder ist leer) – bitte neu installieren."
        }
        let datei = (ziel as NSString).appendingPathComponent("Contents/MacOS/" + programm)
        guard fm.isExecutableFile(atPath: datei) else {
            return "\(eintrag.name) ist nach dem Update unvollständig (Programmdatei \(programm) fehlt) – bitte neu installieren."
        }
        return nil
    }
}
