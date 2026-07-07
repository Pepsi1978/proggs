// RootView.swift
// Hauptfenster: Sidebar mit vorhandenen Harnesses + Detail-Bereich.

import SwiftUI

struct RootView: View {
    @Environment(AppState.self) private var state

    var body: some View {
        NavigationSplitView {
            SidebarView()
                .navigationSplitViewColumnWidth(min: 220, ideal: 260, max: 340)
        } detail: {
            DetailView()
                .frame(minWidth: 600, minHeight: 500)
        }
        .navigationTitle("Harness Forge")
    }
}

struct DetailView: View {
    @Environment(AppState.self) private var state

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                TaskInputView()

                if let result = state.lastResult {
                    MatrixView(result: result)
                }

                if !state.statusMessages.isEmpty {
                    StatusStreamView()
                }
            }
            .padding(24)
        }
    }
}

struct StatusStreamView: View {
    @Environment(AppState.self) private var state

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Status-Stream")
                .font(.headline)
            VStack(alignment: .leading, spacing: 4) {
                ForEach(Array(state.statusMessages.enumerated()), id: \.offset) { _, msg in
                    HStack(alignment: .top, spacing: 6) {
                        Text("›").foregroundStyle(.secondary)
                        Text(msg).font(.system(.body, design: .monospaced))
                    }
                }
                if let err = state.errorMessage {
                    HStack(alignment: .top, spacing: 6) {
                        Text("✗").foregroundStyle(.red)
                        Text(err).foregroundStyle(.red).font(.system(.body, design: .monospaced))
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(.quaternary.opacity(0.5), in: RoundedRectangle(cornerRadius: 8))
        }
    }
}
