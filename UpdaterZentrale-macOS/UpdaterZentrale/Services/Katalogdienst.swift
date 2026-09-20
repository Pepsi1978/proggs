import Foundation

/// Laedt programs.json zur Laufzeit aus dem Repo-Ordner -- neue Programme brauchen keinen Neubau.
/// 1:1-Port von Services/Katalogdienst.cs.
enum Katalogdienst {
    static func laden() -> (katalog: ProgrammKatalog, fehler: String?) {
        var datei = Pfade.katalogDatei
        if !FileManager.default.fileExists(atPath: datei) {
            // Rueckfallebene: die Kopie im App-Buendel, damit ein Lauf ohne Repo trotzdem etwas zeigt.
            datei = (Bundle.main.resourcePath ?? "") + "/programs.json"
        }
        guard FileManager.default.fileExists(atPath: datei) else {
            return (ProgrammKatalog(), "Katalog nicht gefunden: \(Pfade.katalogDatei)")
        }

        guard let daten = FileManager.default.contents(atPath: datei) else {
            return (ProgrammKatalog(), "Katalog konnte nicht gelesen werden: \(datei)")
        }

        do {
            let katalog = try JSONDecoder().decode(ProgrammKatalog.self, from: daten)
            if katalog.programme.isEmpty {
                return (ProgrammKatalog(), "Katalog ist leer: \(datei)")
            }
            return (katalog, nil)
        } catch {
            return (ProgrammKatalog(), "Katalog konnte nicht gelesen werden: \(error.localizedDescription)")
        }
    }
}
