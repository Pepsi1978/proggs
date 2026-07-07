// SidebarView.swift
// Linke Seitenleiste: Liste aller Harness-Forge-Projekte in ~/proggs/.

import SwiftUI

struct SidebarView: View {
    @Environment(AppState.self) private var state

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Projekte")
                    .font(.headline)
                Spacer()
                Button {
                    Task { await state.refresh() }
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .buttonStyle(.plain)
                .help("Aktualisieren")
            }
            .padding(12)

            Divider()

            if state.availableHarnesses.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "folder.badge.plus")
                        .font(.system(size: 28))
                        .foregroundStyle(.secondary)
                    Text("Noch keine Projekte.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List(state.availableHarnesses, id: \.self) { name in
                    HStack(spacing: 8) {
                        Image(systemName: "folder")
                            .foregroundStyle(.tint)
                        Text(name)
                            .lineLimit(1)
                    }
                    .contextMenu {
                        Button("Im Finder oeffnen") {
                            let url = state.rootDir.appendingPathComponent(name)
                            NSWorkspace.shared.open(url)
                        }
                    }
                }
                .listStyle(.sidebar)
            }
        }
    }
}
