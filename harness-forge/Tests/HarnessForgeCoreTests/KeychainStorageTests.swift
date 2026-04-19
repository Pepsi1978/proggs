// KeychainStorageTests.swift
// Round-Trip-Tests fuer den macOS-Keychain.
// Nutzt jeweils einen einzigartigen Service-Identifier pro Test, um Isolation
// zu garantieren und keine Test-Daten zu hinterlassen.

import Testing
import Foundation
@testable import HarnessForgeCore

private func uniqueService() -> String {
    "com.harness-forge.test-\(UUID().uuidString)"
}

@Test("Keychain: Round-Trip fuer Eintrag funktioniert")
func keychainRoundTrip() throws {
    let keychain = KeychainStorage(serviceIdentifier: uniqueService())
    defer { try? keychain.delete(key: "test-key") }

    try keychain.store(key: "test-key", value: "secret-value-42")
    let retrieved = try keychain.retrieve(key: "test-key")
    #expect(retrieved == "secret-value-42")
}

@Test("Keychain: Update ueberschreibt existierenden Wert")
func keychainUpdate() throws {
    let keychain = KeychainStorage(serviceIdentifier: uniqueService())
    defer { try? keychain.delete(key: "test-key") }

    try keychain.store(key: "test-key", value: "first")
    try keychain.store(key: "test-key", value: "second")
    #expect(try keychain.retrieve(key: "test-key") == "second")
}

@Test("Keychain: retrieve wirft itemNotFound bei fehlendem Eintrag")
func keychainRetrieveMissingThrows() {
    let keychain = KeychainStorage(serviceIdentifier: uniqueService())
    #expect(throws: KeychainStorage.KeychainError.self) {
        _ = try keychain.retrieve(key: "does-not-exist")
    }
}

@Test("Keychain: delete von fehlendem Eintrag ist idempotent")
func keychainDeleteMissingIdempotent() throws {
    let keychain = KeychainStorage(serviceIdentifier: uniqueService())
    try keychain.delete(key: "does-not-exist")
}

@Test("Keychain: exists liefert true nach store und false nach delete")
func keychainExistsFlag() throws {
    let keychain = KeychainStorage(serviceIdentifier: uniqueService())
    defer { try? keychain.delete(key: "exists-key") }

    #expect(keychain.exists(key: "exists-key") == false)
    try keychain.store(key: "exists-key", value: "v")
    #expect(keychain.exists(key: "exists-key") == true)
    try keychain.delete(key: "exists-key")
    #expect(keychain.exists(key: "exists-key") == false)
}

@Test("Keychain: Unicode-Werte ueberleben den Round-Trip")
func keychainUnicodeSurvives() throws {
    let keychain = KeychainStorage(serviceIdentifier: uniqueService())
    defer { try? keychain.delete(key: "unicode") }

    let value = "😀 Grüße aus München — API-Key: abcXYZ"
    try keychain.store(key: "unicode", value: value)
    #expect(try keychain.retrieve(key: "unicode") == value)
}
