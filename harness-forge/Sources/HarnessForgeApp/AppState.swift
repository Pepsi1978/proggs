// AppState.swift
// Zentraler, beobachtbarer Zustand der App. MainActor-isoliert: alle Zugriffe
// aus der UI laufen ohnehin auf Main, daher serialisiert das alle Updates
// ohne zusaetzliche Locks.

import Foundation
import Observation
import HarnessForgeCore
import HarnessForgeBuilders

@MainActor
@Observable
final class AppState {

    // MARK: - User-Input

    var taskInput: String = ""
    var chosenTypeOverride: HarnessType?

    // MARK: - Results

    var lastResult: TaskAnalyzer.AnalysisResult?
    var lastOutcome: BuildOutcome?

    // MARK: - Progress / Status

    var isAnalyzing: Bool = false
    var isBuilding: Bool = false
    var statusMessages: [String] = []
    var errorMessage: String?

    // MARK: - Config

    var anthropicKey: String = ""
    var openaiKey: String = ""
    var geminiKey: String = ""

    let rootDir: URL
    var availableHarnesses: [String] = []

    init() {
        rootDir = URL(fileURLWithPath: (NSHomeDirectory() as NSString).appendingPathComponent("proggs"))
        loadKeysFromKeychain()
    }

    // MARK: - Lifecycle

    func refresh() async {
        let contents = (try? FileManager.default.contentsOfDirectory(
            at: rootDir,
            includingPropertiesForKeys: [.isDirectoryKey]
        )) ?? []
        availableHarnesses = contents
            .filter {
                FileManager.default.fileExists(
                    atPath: $0.appendingPathComponent("SYSTEM_PROMPT.md").path
                )
            }
            .map(\.lastPathComponent)
            .sorted()
    }

    // MARK: - Actions

    func analyze() async {
        guard !taskInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        guard !isAnalyzing else { return }

        isAnalyzing = true
        defer { isAnalyzing = false }

        errorMessage = nil
        lastResult = nil
        lastOutcome = nil
        statusMessages = ["Analyse startet …"]

        let (client, label) = selectBackend()
        statusMessages.append("Backend: \(label)")

        do {
            let analyzer = TaskAnalyzer(client: client)
            let result = try await analyzer.analyze(taskDescription: taskInput)
            lastResult = result
            statusMessages.append("Empfehlung: \(result.recommendation.displayName)")
            statusMessages.append("Slug: \(result.slug)")
            statusMessages.append(String(format: "Kosten der Analyse: $%.4f", result.totalCostUSD))
        } catch {
            errorMessage = error.localizedDescription
            statusMessages.append("Fehler: \(error.localizedDescription)")
        }
    }

    func build() async {
        guard let result = lastResult else { return }
        guard !isBuilding else { return }

        isBuilding = true
        defer { isBuilding = false }

        let chosenType = chosenTypeOverride ?? result.recommendation
        let builder = selectBuilder(for: chosenType)
        statusMessages.append("Baue \(chosenType.displayName) …")

        do {
            let outcome = try await builder.build(
                taskDescription: result.taskDescription,
                slug: result.slug,
                decomposition: result.decomposition,
                outputParentDirectory: rootDir
            )
            lastOutcome = outcome
            statusMessages.append("Fertig: \(outcome.projectDirectory.path)")
            await refresh()
        } catch {
            errorMessage = error.localizedDescription
            statusMessages.append("Build-Fehler: \(error.localizedDescription)")
        }
    }

    // MARK: - Settings

    func saveKeysToKeychain() throws {
        let keychain = KeychainStorage()
        if !anthropicKey.isEmpty {
            try keychain.store(key: "anthropic-api-key", value: anthropicKey)
        }
        if !openaiKey.isEmpty {
            try keychain.store(key: "openai-api-key", value: openaiKey)
        }
        if !geminiKey.isEmpty {
            try keychain.store(key: "gemini-api-key", value: geminiKey)
        }
    }

    private func loadKeysFromKeychain() {
        let keychain = KeychainStorage()
        anthropicKey = (try? keychain.retrieve(key: "anthropic-api-key")) ?? ""
        openaiKey = (try? keychain.retrieve(key: "openai-api-key")) ?? ""
        geminiKey = (try? keychain.retrieve(key: "gemini-api-key")) ?? ""
    }

    // MARK: - Helpers

    private func selectBackend() -> (any LLMClient, String) {
        let env = ProcessInfo.processInfo.environment
        let anthropic = !anthropicKey.isEmpty ? anthropicKey : env["ANTHROPIC_API_KEY"] ?? ""
        let openai = !openaiKey.isEmpty ? openaiKey : env["OPENAI_API_KEY"] ?? ""
        let gemini = !geminiKey.isEmpty ? geminiKey : env["GEMINI_API_KEY"] ?? ""

        if !anthropic.isEmpty { return (AnthropicBackend(apiKey: anthropic), "Anthropic Claude") }
        if !openai.isEmpty { return (OpenAIBackend(apiKey: openai), "OpenAI GPT") }
        if !gemini.isEmpty { return (GeminiBackend(apiKey: gemini), "Google Gemini") }
        return (LMStudioBackend(), "LM Studio (lokal)")
    }

    private func selectBuilder(for type: HarnessType) -> any HarnessBuilder {
        switch type {
        case .purePrompt: PurePromptBuilder()
        case .pythonCLI: PythonCLIBuilder()
        case .tauriDesktop: TauriDesktopBuilder()
        case .androidKotlin: AndroidKotlinBuilder()
        case .claudeSubagent: ClaudeSubagentBuilder()
        }
    }
}
