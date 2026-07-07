// SettingsView.swift
// Einstellungen: API-Keys werden in macOS Keychain abgelegt.

import SwiftUI

struct SettingsView: View {
    @Environment(AppState.self) private var state
    @State private var saveError: String?
    @State private var saveSuccess: Bool = false

    var body: some View {
        @Bindable var state = state

        VStack(alignment: .leading, spacing: 16) {
            Text("API-Keys")
                .font(.title2).bold()

            Text("Alle Keys werden verschluesselt in macOS Keychain gespeichert.")
                .font(.callout)
                .foregroundStyle(.secondary)

            Form {
                Section("Anthropic (Claude)") {
                    SecureField("sk-ant-…", text: $state.anthropicKey)
                        .textFieldStyle(.roundedBorder)
                }
                Section("OpenAI (GPT)") {
                    SecureField("sk-…", text: $state.openaiKey)
                        .textFieldStyle(.roundedBorder)
                }
                Section("Google (Gemini)") {
                    SecureField("AIza…", text: $state.geminiKey)
                        .textFieldStyle(.roundedBorder)
                }
            }
            .formStyle(.grouped)

            HStack {
                if saveSuccess {
                    Label("Gespeichert", systemImage: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                        .font(.callout)
                }
                if let err = saveError {
                    Label(err, systemImage: "exclamationmark.circle.fill")
                        .foregroundStyle(.red)
                        .font(.caption)
                }
                Spacer()
                Button {
                    saveError = nil
                    saveSuccess = false
                    do {
                        try state.saveKeysToKeychain()
                        saveSuccess = true
                    } catch {
                        saveError = error.localizedDescription
                    }
                } label: {
                    Text("In Keychain speichern")
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
    }
}
