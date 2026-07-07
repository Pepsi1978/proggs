// KeychainStorage.swift
// Duenner Wrapper um Security.framework fuer sichere Ablage von API-Keys
// im macOS-Keychain. Nutzt kSecClassGenericPassword mit einem Service-
// Identifier pro Harness-Forge-Instanz.

import Foundation
import Security

/// Sichere Ablage von API-Keys und aehnlichen Secrets im macOS-Keychain.
public struct KeychainStorage: Sendable {
    public enum KeychainError: Error, LocalizedError, Sendable {
        case itemNotFound
        case duplicateItem
        case unhandledError(status: OSStatus)
        case dataConversionError

        public var errorDescription: String? {
            switch self {
            case .itemNotFound: "Keychain-Eintrag nicht gefunden"
            case .duplicateItem: "Keychain-Eintrag existiert bereits"
            case .unhandledError(let status): "Keychain-Fehler: OSStatus \(status)"
            case .dataConversionError: "String ↔ Data Konvertierung fehlgeschlagen"
            }
        }
    }

    public let serviceIdentifier: String

    public init(serviceIdentifier: String = "com.harness-forge") {
        self.serviceIdentifier = serviceIdentifier
    }

    // MARK: - Schreiben

    /// Speichert einen Wert oder aktualisiert ihn, wenn bereits vorhanden.
    public func store(key: String, value: String) throws {
        guard let data = value.data(using: .utf8) else {
            throw KeychainError.dataConversionError
        }

        let baseQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceIdentifier,
            kSecAttrAccount as String: key,
        ]

        // Update versuchen, bei errSecItemNotFound → Add
        let updateAttributes: [String: Any] = [kSecValueData as String: data]
        let updateStatus = SecItemUpdate(baseQuery as CFDictionary, updateAttributes as CFDictionary)

        if updateStatus == errSecItemNotFound {
            var addQuery = baseQuery
            addQuery[kSecValueData as String] = data
            let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
            if addStatus != errSecSuccess {
                throw KeychainError.unhandledError(status: addStatus)
            }
        } else if updateStatus != errSecSuccess {
            throw KeychainError.unhandledError(status: updateStatus)
        }
    }

    // MARK: - Lesen

    /// Liest den Wert zu einem Key. Wirft `itemNotFound`, wenn nichts hinterlegt ist.
    public func retrieve(key: String) throws -> String {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceIdentifier,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecItemNotFound {
            throw KeychainError.itemNotFound
        } else if status != errSecSuccess {
            throw KeychainError.unhandledError(status: status)
        }

        guard let data = result as? Data,
              let string = String(data: data, encoding: .utf8) else {
            throw KeychainError.dataConversionError
        }

        return string
    }

    // MARK: - Loeschen

    /// Loescht einen Eintrag. Idempotent — kein Fehler, wenn bereits weg.
    public func delete(key: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: serviceIdentifier,
            kSecAttrAccount as String: key,
        ]

        let status = SecItemDelete(query as CFDictionary)
        if status != errSecSuccess && status != errSecItemNotFound {
            throw KeychainError.unhandledError(status: status)
        }
    }

    // MARK: - Existenzpruefung

    public func exists(key: String) -> Bool {
        (try? retrieve(key: key)) != nil
    }
}
