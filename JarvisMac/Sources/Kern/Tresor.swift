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
    private static let sperre = NSLock()

    private static func hauptSchluessel() throws -> SecureEnclave.P256.KeyAgreement.PrivateKey {
        sperre.lock(); defer { sperre.unlock() }
        if let blob = try? Data(contentsOf: schluesselDatei) {
            return try SecureEnclave.P256.KeyAgreement.PrivateKey(dataRepresentation: blob)
        }
        var fehler: Unmanaged<CFError>?
        guard let regel = SecAccessControlCreateWithFlags(nil, kSecAttrAccessibleWhenUnlockedThisDeviceOnly, [.privateKeyUsage], &fehler) else {
            throw fehler!.takeRetainedValue() as Error
        }
        let k = try SecureEnclave.P256.KeyAgreement.PrivateKey(accessControl: regel)
        try schreibe(k.dataRepresentation, nach: schluesselDatei)
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

    /// Liest verschlüsselt. Liegt noch die alte Klartextdatei da, wandert sie einmalig in den Tresor und wird danach überschrieben und gelöscht.
    static func lade<T: Codable>(_ typ: T.Type, name: String, klartext: URL) -> T? {
        if let d = try? Data(contentsOf: datei(name)) {
            do { return try JSONDecoder().decode(T.self, from: try oeffnen(d)) } catch {
                log("fehler", "Tresor: \(name) nicht lesbar", wo: "Tresor", fehler: error.localizedDescription)
                return nil
            }
        }
        guard let alt = try? Data(contentsOf: klartext), let wert = try? JSONDecoder().decode(T.self, from: alt) else { return nil }
        do {
            let v = try versiegeln(alt)
            guard try oeffnen(v) == alt else { return wert }
            try schreibe(v, nach: datei(name))
            try? Data(repeating: 0, count: alt.count).write(to: klartext)
            try? FileManager.default.removeItem(at: klartext)
            log("tresor", "\(name): Klartextdatei in den Tresor übernommen und gelöscht", wo: "Tresor")
        } catch {
            log("fehler", "Tresor: \(name) nicht übernommen, Klartext bleibt vorerst", wo: "Tresor", fehler: error.localizedDescription)
        }
        return wert
    }

    static func sichere<T: Codable>(_ wert: T, name: String) {
        do {
            try schreibe(try versiegeln(try JSONEncoder().encode(wert)), nach: datei(name))
        } catch {
            log("fehler", "Tresor: \(name) nicht gespeichert", wo: "Tresor", fehler: error.localizedDescription)
        }
    }

    static func loesche(name: String) { try? FileManager.default.removeItem(at: datei(name)) }

    /// Wege zu Zugangsdaten, die Jarvis' eigene Werkzeuge nie anfassen. Schutz gegen untergeschobene Anweisungen aus Mails, Webseiten oder Dateien.
    private static let gesperrteWege = ["/sk/", "/sk'", "/sk\"", "~/sk", "$home/sk", "${home}/sk", "jarvismac/tresor", "enclave.key", "zugang.json", "zugang.enc",
                                        "codex.enc", "keys.json", "plugin-url.txt", "find-generic-password", "dump-keychain"]

    static func sperre(_ text: String) -> WerkzeugErgebnis? {
        let t = text.lowercased()
        guard gesperrteWege.contains(where: t.contains) || t.hasSuffix("/sk") else { return nil }
        log("tresor", "Werkzeug-Zugriff auf Zugangsdaten abgewiesen", wo: "Tresor", eingabe: text)
        return WerkzeugErgebnis(text: "Abgelehnt: Zugangsdaten sind für Jarvis' Werkzeuge gesperrt. Frank sieht sie unter Einstellungen → Konten nach Touch ID.", fehler: true)
    }
}

/// Die aktuell bekannten Schlüsselwerte. Alles, was ins Logbuch oder ans Modell geht, läuft durch [schwaerzen].
enum Geheimnisse {
    private static let sperre = NSLock()
    nonisolated(unsafe) private static var werte: [String: [String]] = [:]

    static func setzen(_ quelle: String, _ neu: [String]) {
        sperre.lock(); defer { sperre.unlock() }
        werte[quelle] = neu.filter { $0.count >= 12 }
    }

    static func schwaerzen(_ text: String) -> String {
        sperre.lock(); let alle = werte.values.flatMap { $0 }.sorted { $0.count > $1.count }; sperre.unlock()
        return alle.reduce(text) { $0.contains($1) ? $0.replacingOccurrences(of: $1, with: "••••") : $0 }
    }
}
