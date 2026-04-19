// AndroidKotlinBuilderTests.swift

import Testing
import Foundation
@testable import HarnessForgeBuilders

private func makeTmpParent() -> URL {
    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent("harness-forge-android-test-\(UUID().uuidString)")
    try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
    return url
}

private func cleanup(_ url: URL) {
    try? FileManager.default.removeItem(at: url)
}

private let androidDecomp = TaskDecomposition(
    targetEnvironment: .mobile,
    interactivity: .ongoing,
    verifiability: .softCriteria,
    dataSources: .sensors,
    targetUsers: .endUsers,
    offlineCapability: .mustOffline,
    rationale: "Mobile Offline-App."
)

@Test("Android: erzeugt alle 13 Dateien")
func androidBuildsAllFiles() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await AndroidKotlinBuilder().build(
        taskDescription: "Fitness-Coach",
        slug: "fitness-coach",
        decomposition: androidDecomp,
        outputParentDirectory: parent
    )

    #expect(outcome.createdFiles.count == 13)
    for f in outcome.createdFiles {
        #expect(FileManager.default.fileExists(atPath: f.path))
    }
}

@Test("Android: package-Name wird korrekt aus Slug generiert")
func androidPackageGeneration() {
    #expect(AndroidKotlinTemplates.androidPackage(from: "meine-app") == "com.harnessforge.meine_app")
    #expect(AndroidKotlinTemplates.androidPackage(from: "xyz") == "com.harnessforge.xyz")
    #expect(AndroidKotlinTemplates.packagePath(from: "com.harnessforge.xyz") == "com/harnessforge/xyz")
}

@Test("Android: Kotlin-Dateien liegen im richtigen package-Unterordner")
func kotlinFilesInCorrectPath() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await AndroidKotlinBuilder().build(
        taskDescription: "t",
        slug: "path-test",
        decomposition: androidDecomp,
        outputParentDirectory: parent
    )

    let mainActivity = outcome.createdFiles.first { $0.lastPathComponent == "MainActivity.kt" }
    #expect(mainActivity?.path.contains("com/harnessforge/path_test/MainActivity.kt") == true)
}

@Test("Android: app/build.gradle.kts enthaelt Compose + Material 3")
func appBuildGradleContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await AndroidKotlinBuilder().build(
        taskDescription: "t",
        slug: "gradle-test",
        decomposition: androidDecomp,
        outputParentDirectory: parent
    )

    let gradleFiles = outcome.createdFiles.filter { $0.lastPathComponent == "build.gradle.kts" }
    let appGradle = gradleFiles.first { $0.path.contains("/app/") }!
    let text = try String(contentsOf: appGradle, encoding: .utf8)

    #expect(text.contains("com.android.application"))
    #expect(text.contains("compose = true"))
    #expect(text.contains("androidx.compose.material3:material3"))
    #expect(text.contains("com.squareup.okhttp3:okhttp"))
    #expect(text.contains("com.harnessforge.gradle_test"))
}

@Test("Android: AndroidManifest hat INTERNET permission und LAUNCHER intent")
func manifestContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await AndroidKotlinBuilder().build(
        taskDescription: "t",
        slug: "manifest-test",
        decomposition: androidDecomp,
        outputParentDirectory: parent
    )

    let manifest = outcome.createdFiles.first { $0.lastPathComponent == "AndroidManifest.xml" }!
    let text = try String(contentsOf: manifest, encoding: .utf8)

    #expect(text.contains("android.permission.INTERNET"))
    #expect(text.contains("android.intent.action.MAIN"))
    #expect(text.contains("android.intent.category.LAUNCHER"))
}

@Test("Android: MainScreen.kt nutzt Compose + Coroutines")
func mainScreenContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await AndroidKotlinBuilder().build(
        taskDescription: "Meine Compose-Aufgabe",
        slug: "compose-test",
        decomposition: androidDecomp,
        outputParentDirectory: parent
    )

    let ms = outcome.createdFiles.first { $0.lastPathComponent == "MainScreen.kt" }!
    let text = try String(contentsOf: ms, encoding: .utf8)

    #expect(text.contains("@Composable"))
    #expect(text.contains("fun MainScreen()"))
    #expect(text.contains("OutlinedTextField"))
    #expect(text.contains("rememberCoroutineScope"))
    #expect(text.contains("Meine Compose-Aufgabe"))
    #expect(text.contains("LLMClient.chat("))
}

@Test("Android: LLMClient.kt verwendet OkHttp und ruft Anthropic auf")
func llmClientContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await AndroidKotlinBuilder().build(
        taskDescription: "t",
        slug: "llm-test",
        decomposition: androidDecomp,
        outputParentDirectory: parent
    )

    let llm = outcome.createdFiles.first { $0.lastPathComponent == "LLMClient.kt" }!
    let text = try String(contentsOf: llm, encoding: .utf8)

    #expect(text.contains("OkHttpClient"))
    #expect(text.contains("api.anthropic.com/v1/messages"))
    #expect(text.contains("ANTHROPIC_API_KEY"))
    #expect(text.contains("x-api-key"))
    #expect(text.contains("anthropic-version"))
}
