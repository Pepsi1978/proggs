import CryptoKit
import Foundation
import Security

/// Der Tresor: Schlüssel und Anmeldedaten liegen nur verschlüsselt auf der Platte (AES-GCM). Der Hauptschlüssel entsteht im
/// Secure Enclave dieses Macs und verlässt ihn nie, eine kopierte Datei ist woanders wertlos. Die Keychain taugt hier nicht:
/// Mit der lokalen Signatur liest im Test auch ein fremd signiertes Programm ihre Einträge ohne Rückfrage.
enum Tresor {
    private static let ordner: URL = {
        let u = Pfade.unterstuetzung.appendingPathComponent("Tresor", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true, attributes: [.posixPermissions: 0o700])
        return u
    }()
    private static var schluesselDatei: URL { ordner.appendingPathComponent("enclave.key") }
    private static func datei(_ name: String) -> URL { ordner.appendingPathComponent(name + ".enc") }
    private static let schloss = NSLock()

    nonisolated(unsafe) private static var geladen: SecureEnclave.P256.KeyAgreement.PrivateKey?

    private static func hauptSchluessel() throws -> SecureEnclave.P256.KeyAgreement.PrivateKey {
        schloss.lock(); defer { schloss.unlock() }
        if let geladen { return geladen }
        // Nur wenn die Datei wirklich fehlt, entsteht ein neuer Schlüssel. Ein Lesefehler darf den alten nie ersetzen, sonst wäre alles verloren.
        if FileManager.default.fileExists(atPath: schluesselDatei.path) {
            let k = try SecureEnclave.P256.KeyAgreement.PrivateKey(dataRepresentation: try Data(contentsOf: schluesselDatei))
            geladen = k
            return k
        }
        var fehler: Unmanaged<CFError>?
        guard let regel = SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlockedThisDeviceOnly, [.privateKeyUsage], &fehler) else {
            throw fehler!.takeRetainedValue() as Error
        }
        let k = try SecureEnclave.P256.KeyAgreement.PrivateKey(accessControl: regel)
        try schreibe(k.dataRepresentation, nach: schluesselDatei)
        geladen = k
        return k
    }

    private static func schreibe(_ d: Data, nach u: URL) throws {
        try d.write(to: u, options: .atomic)
        try FileManager.default.setAttributes([.posixPermissions: 0o600], ofItemAtPath: u.path)
    }

    private static func symmetrisch(_ geteilt: SharedSecret, _ wegwerf: Data) -> SymmetricKey {
        geteilt.hkdfDerivedSymmetricKey(using: SHA256.self, salt: Data("JarvisMac-Tresor-1".utf8), sharedInfo: wegwerf, outputByteCount: 32)
    }

    /// Aufbau: öffentlicher Wegwerf-Schlüssel (65 Byte), danach AES-GCM.
    private static func versiegeln(_ klar: Data) throws -> Data {
        let wegwerf = P256.KeyAgreement.PrivateKey()
        let oeffentlich = wegwerf.publicKey.x963Representation
        let geteilt = try wegwerf.sharedSecretFromKeyAgreement(with: try hauptSchluessel().publicKey)
        let box = try AES.GCM.seal(klar, using: symmetrisch(geteilt, oeffentlich))
        return oeffentlich + box.combined!
    }

    private static func oeffnen(_ d: Data) throws -> Data {
        let oeffentlich = Data(d.prefix(65))
        let geteilt = try hauptSchluessel().sharedSecretFromKeyAgreement(with: try P256.KeyAgreement.PublicKey(x963Representation: oeffentlich))
        return try AES.GCM.open(AES.GCM.SealedBox(combined: d.dropFirst(65)), using: symmetrisch(geteilt, oeffentlich))
    }

    /// Einträge, deren .enc gerade nicht lesbar ist: Sie werden nie überschrieben, sonst wären die Schlüssel darin endgültig weg.
    nonisolated(unsafe) private static var unlesbar: Set<String> = []
    /// Für die Oberfläche: Text, wenn etwas im Tresor nicht lesbar war.
    static var stoerung: String? {
        schloss.lock(); defer { schloss.unlock() }
        return unlesbar.isEmpty ? nil : "Tresor gerade nicht lesbar (\(unlesbar.sorted().joined(separator: ", "))). Es wird nichts überschrieben; Mac entsperren und Jarvis neu starten."
    }

    /// Liest verschlüsselt. Liegt noch die alte Klartextdatei da, wandert sie einmalig in den Tresor und wird danach gelöscht.
    static func lade<T: Codable>(_ typ: T.Type, name: String, klartext: URL) -> T? {
        if let d = try? Data(contentsOf: datei(name)) {
            do {
                let wert = try JSONDecoder().decode(T.self, from: try oeffnen(d))
                try? FileManager.default.removeItem(at: klartext)   // Reste einer früher gescheiterten Übernahme
                return wert
            } catch {
                schloss.lock(); unlesbar.insert(name); schloss.unlock()
                log("fehler", "Tresor: \(name) nicht lesbar, Speichern gesperrt", wo: "Tresor", fehler: error.localizedDescription)
                return nil
            }
        }
        guard let alt = try? Data(contentsOf: klartext), let wert = try? JSONDecoder().decode(T.self, from: alt) else { return nil }
        do {
            let v = try versiegeln(alt)
            guard try oeffnen(v) == alt else { return wert }
            try schreibe(v, nach: datei(name))
            try? FileManager.default.removeItem(at: klartext)
            log("tresor", "\(name): Klartextdatei in den Tresor übernommen und gelöscht", wo: "Tresor")
        } catch {
            log("fehler", "Tresor: \(name) nicht übernommen, Klartext bleibt vorerst", wo: "Tresor", fehler: error.localizedDescription)
        }
        return wert
    }

    static func sichere<T: Codable>(_ wert: T, name: String) {
        schloss.lock(); let gesperrt = unlesbar.contains(name); schloss.unlock()
        guard !gesperrt else { return }
        do {
            try schreibe(try versiegeln(try JSONEncoder().encode(wert)), nach: datei(name))
        } catch {
            log("fehler", "Tresor: \(name) nicht gespeichert", wo: "Tresor", fehler: error.localizedDescription)
        }
    }

    static func loesche(name: String, klartext: URL? = nil) {
        try? FileManager.default.removeItem(at: datei(name))
        if let klartext { try? FileManager.default.removeItem(at: klartext) }
        schloss.lock(); unlesbar.remove(name); schloss.unlock()
    }

    /// Bremse gegen untergeschobene Anweisungen aus Mails, Webseiten oder Dateien: Jarvis' Werkzeuge greifen nicht auf Zugangsdaten zu.
    /// Das ist kein vollständiger Schutz, eine Textprüfung lässt sich umgehen; die Schwärzung ist die zweite Stufe.
    private static let gesperrterWeg = try! NSRegularExpression(pattern:
        #"(~|\$home|\$\{home\}|/users/[^/\s]+)/sk(?=[/\s'"]|$)|jarvismac/tresor|enclave\.key|find-generic-password|dump-keychain"#)

    static func sperre(_ teile: String?...) -> WerkzeugErgebnis? {
        for teil in teile.compactMap({ $0 }) {
            let t = teil.lowercased()
            guard gesperrterWeg.firstMatch(in: t, range: NSRange(t.startIndex..., in: t)) != nil else { continue }
            log("tresor", "Werkzeug-Zugriff auf Zugangsdaten abgewiesen", wo: "Tresor", eingabe: teil)
            return WerkzeugErgebnis(text: "Abgelehnt: Zugangsdaten sind für Jarvis' Werkzeuge gesperrt. Frank sieht sie unter Einstellungen → Konten nach Touch ID.", fehler: true)
        }
        return nil
    }
}

/// Die aktuell bekannten Schlüsselwerte. Alles, was ins Logbuch oder ans Modell geht, läuft durch [schwaerzen].
enum Geheimnisse {
    private static let schloss = NSLock()
    nonisolated(unsafe) private static var werte: [String: [String]] = [:]

    static func setzen(_ quelle: String, _ neu: [String]) {
        schloss.lock(); defer { schloss.unlock() }
        // Auch als Base64: Eine kodierte Ausgabe soll den Schlüssel genauso wenig verraten.
        werte[quelle] = neu.filter { $0.count >= 12 }.flatMap { [$0, Data($0.utf8).base64EncodedString()] }
    }

    static func schwaerzen(_ text: String) -> String {
        schloss.lock(); let alle = werte.values.flatMap { $0 }.sorted { $0.count > $1.count }; schloss.unlock()
        return alle.reduce(text) { $0.contains($1) ? $0.replacingOccurrences(of: $1, with: "••••") : $0 }
    }
}
