// TaskInputView.swift
// Zentrale Eingabemaske: grosses Textfeld, Analyze-Button, Build-Button.

import SwiftUI
import HarnessForgeBuilders

struct TaskInputView: View {
    @Environment(AppState.self) private var state

    var body: some View {
        @Bindable var state = state

        VStack(alignment: .leading, spacing: 12) {
            Text("Aufgabe beschreiben")
                .font(.title2).bold()

            Text("Was soll dein KI-Begleiter koennen? Beschreibe die Aufgabe in ein bis zwei Saetzen.")
                .font(.callout)
                .foregroundStyle(.secondary)

            TextEditor(text: $state.taskInput)
                .font(.system(.body))
                .frame(minHeight: 120)
                .padding(6)
                .background(.quaternary.opacity(0.35), in: RoundedRectangle(cornerRadius: 8))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(.separator, lineWidth: 0.5)
                )
                .disabled(state.isAnalyzing || state.isBuilding)

            HStack(spacing: 12) {
                Button {
                    Task { await state.analyze() }
                } label: {
                    HStack(spacing: 6) {
                        if state.isAnalyzing {
                            ProgressView().controlSize(.small)
                        }
                        Text(state.isAnalyzing ? "Analysiere …" : "Analysieren")
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(
                    state.isAnalyzing || state.isBuilding ||
                    state.taskInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                )

                if state.lastResult != nil {
                    Menu {
                        Button("Automatische Empfehlung") {
                            state.chosenTypeOverride = nil
                        }
                        Divider()
                        ForEach(HarnessType.allCases, id: \.self) { type in
                            Button("\(type.displayName) erzwingen") {
                                state.chosenTypeOverride = type
                            }
                        }
                    } label: {
                        Text("Typ: \(overrideLabel)")
                    }
                    .menuStyle(.borderlessButton)
                    .fixedSize()

                    Button {
                        Task { await state.build() }
                    } label: {
                        HStack(spacing: 6) {
                            if state.isBuilding {
                                ProgressView().controlSize(.small)
                            }
                            Text(state.isBuilding ? "Baue …" : "Bauen")
                        }
                    }
                    .buttonStyle(.bordered)
                    .disabled(state.isAnalyzing || state.isBuilding)
                }

                Spacer()

                if let outcome = state.lastOutcome {
                    Button {
                        NSWorkspace.shared.open(outcome.projectDirectory)
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "folder")
                            Text("Im Finder oeffnen")
                        }
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
    }

    private var overrideLabel: String {
        if let override = state.chosenTypeOverride {
            return override.displayName
        }
        return "Empfehlung"
    }
}
