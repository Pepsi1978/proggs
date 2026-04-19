// TauriDesktopBuilderTests.swift

import Testing
import Foundation
@testable import HarnessForgeBuilders

private func makeTmpParent() -> URL {
    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent("harness-forge-tauri-test-\(UUID().uuidString)")
    try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
    return url
}

private func cleanup(_ url: URL) {
    try? FileManager.default.removeItem(at: url)
}

private let tauriDecomp = TaskDecomposition(
    targetEnvironment: .desktop,
    interactivity: .ongoing,
    verifiability: .softCriteria,
    dataSources: .localFiles,
    targetUsers: .endUsers,
    offlineCapability: .canBeOnline,
    rationale: "Desktop-App mit grosser UI."
)

@Test("Tauri: erzeugt alle 15 Dateien in korrekten Unterordnern")
func tauriBuildsAllFiles() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await TauriDesktopBuilder().build(
        taskDescription: "Wissens-Explorer fuer PDFs",
        slug: "pdf-explorer",
        decomposition: tauriDecomp,
        outputParentDirectory: parent
    )

    #expect(outcome.createdFiles.count == 15)
    for url in outcome.createdFiles {
        #expect(FileManager.default.fileExists(atPath: url.path), "fehlt: \(url.path)")
    }
}

@Test("Tauri: package.json listet Svelte, Vite, Tauri-Dependencies")
func packageJsonContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await TauriDesktopBuilder().build(
        taskDescription: "t",
        slug: "pkg-test",
        decomposition: tauriDecomp,
        outputParentDirectory: parent
    )

    let pkg = outcome.createdFiles.first { $0.lastPathComponent == "package.json" }!
    let text = try String(contentsOf: pkg, encoding: .utf8)

    #expect(text.contains("@tauri-apps/api"))
    #expect(text.contains("@sveltejs/vite-plugin-svelte"))
    #expect(text.contains("vite"))
    #expect(text.contains(#""name": "pkg-test""#))
}

@Test("Tauri: Cargo.toml hat Crate-Name mit Underscores")
func cargoTomlUnderscores() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await TauriDesktopBuilder().build(
        taskDescription: "t",
        slug: "my-cool-app",
        decomposition: tauriDecomp,
        outputParentDirectory: parent
    )

    let cargo = outcome.createdFiles.first { $0.lastPathComponent == "Cargo.toml" }!
    let text = try String(contentsOf: cargo, encoding: .utf8)
    #expect(text.contains(#"name = "my_cool_app""#))
    #expect(text.contains("tauri = "))
    #expect(text.contains("reqwest"))
}

@Test("Tauri: tauri.conf.json enthaelt productName und identifier")
func tauriConfContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await TauriDesktopBuilder().build(
        taskDescription: "t",
        slug: "conf-test",
        decomposition: tauriDecomp,
        outputParentDirectory: parent
    )

    let conf = outcome.createdFiles.first { $0.lastPathComponent == "tauri.conf.json" }!
    let text = try String(contentsOf: conf, encoding: .utf8)
    #expect(text.contains(#""productName": "conf-test""#))
    #expect(text.contains(#""identifier": "com.harness-forge.conf-test""#))

    // JSON muss parsebar sein
    let data = try #require(text.data(using: .utf8))
    _ = try JSONSerialization.jsonObject(with: data)
}

@Test("Tauri: lib.rs hat chat_with_llm-Command und Builder-Run")
func libRsContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await TauriDesktopBuilder().build(
        taskDescription: "t",
        slug: "rust-test",
        decomposition: tauriDecomp,
        outputParentDirectory: parent
    )

    let lib = outcome.createdFiles.first { $0.lastPathComponent == "lib.rs" }!
    let text = try String(contentsOf: lib, encoding: .utf8)
    #expect(text.contains("#[tauri::command]"))
    #expect(text.contains("async fn chat_with_llm"))
    #expect(text.contains("ANTHROPIC_API_KEY"))
    #expect(text.contains("tauri::Builder::default()"))
    #expect(text.contains("invoke_handler"))
}

@Test("Tauri: App.svelte nutzt invoke + bindet UI")
func svelteAppContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await TauriDesktopBuilder().build(
        taskDescription: "Mein Fancy Tool",
        slug: "svelte-test",
        decomposition: tauriDecomp,
        outputParentDirectory: parent
    )

    let app = outcome.createdFiles.first { $0.lastPathComponent == "App.svelte" }!
    let text = try String(contentsOf: app, encoding: .utf8)
    #expect(text.contains(#"import { invoke } from "@tauri-apps/api/core""#))
    #expect(text.contains(#"invoke("chat_with_llm""#))
    #expect(text.contains("Mein Fancy Tool"))
    #expect(text.contains("$state("))  // Svelte 5 Runes
}

@Test("Tauri: rustCrateName faltet Hyphens zu Underscores")
func rustCrateNameFunction() {
    #expect(TauriDesktopTemplates.rustCrateName(from: "abc") == "abc")
    #expect(TauriDesktopTemplates.rustCrateName(from: "my-awesome-app") == "my_awesome_app")
}
