// MatrixView.swift
// Stellt die Entscheidungs-Matrix als Tabelle dar — sortiert nach Score absteigend,
// Empfehlung farblich hervorgehoben.

import SwiftUI
import HarnessForgeCore
import HarnessForgeBuilders

struct MatrixView: View {
    let result: TaskAnalyzer.AnalysisResult

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text("Entscheidungs-Matrix")
                    .font(.title3).bold()
                Spacer()
                Label("Empfehlung: \(result.recommendation.displayName)",
                      systemImage: "sparkle")
                    .font(.callout)
                    .foregroundStyle(.tint)
            }

            matrixTable

            VStack(alignment: .leading, spacing: 6) {
                Text("Begruendung")
                    .font(.subheadline).bold()
                Text(result.rationale)
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(12)
            .background(.quaternary.opacity(0.35), in: RoundedRectangle(cornerRadius: 8))
        }
    }

    private var matrixTable: some View {
        VStack(spacing: 0) {
            // Header
            HStack(spacing: 0) {
                matrixCell("Harness", width: 180, bold: true, leading: true)
                matrixCell("Umg.", bold: true)
                matrixCell("Inter.", bold: true)
                matrixCell("Verif.", bold: true)
                matrixCell("Daten", bold: true)
                matrixCell("Nutzer", bold: true)
                matrixCell("Off.", bold: true)
                matrixCell("Total", width: 80, bold: true)
            }
            .frame(height: 30)
            .background(.quaternary.opacity(0.6))

            Divider()

            ForEach(result.matrix.sortedByScore, id: \.harnessType) { row in
                HStack(spacing: 0) {
                    matrixCell(row.harnessType.displayName, width: 180,
                               bold: row.harnessType == result.recommendation, leading: true)
                    ForEach(row.scores, id: \.axisName) { s in
                        matrixCell("\(s.score)")
                    }
                    matrixCell("\(row.totalScore)/30", width: 80,
                               bold: row.harnessType == result.recommendation)
                }
                .frame(height: 34)
                .background(row.harnessType == result.recommendation
                            ? Color.accentColor.opacity(0.12)
                            : .clear)
                Divider()
            }
        }
        .overlay(
            RoundedRectangle(cornerRadius: 6)
                .stroke(.separator, lineWidth: 0.5)
        )
    }

    private func matrixCell(
        _ text: String,
        width: CGFloat? = nil,
        bold: Bool = false,
        leading: Bool = false
    ) -> some View {
        Text(text)
            .font(.system(.body, design: bold ? .default : .monospaced))
            .fontWeight(bold ? .semibold : .regular)
            .frame(maxWidth: width, maxHeight: .infinity, alignment: leading ? .leading : .center)
            .padding(.horizontal, 8)
    }
}
