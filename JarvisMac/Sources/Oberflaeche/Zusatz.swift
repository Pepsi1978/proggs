import AppKit
import SwiftUI

// MARK: - Programmierung (Aufträge an den Programmierer, wie im Handy-Reiter „Programmierung“)

struct ProgrammierungAnsicht: View {
    @State private var liste = BereichsLader()
    @State private var auftrag = ""
    @State private var nachtragId = ""
    @State private var nachtrag = ""
    @State private var frageId = ""
    @State private var antwort = ""
    @State private var meldung = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                BereichsKopf(symbol: "chevron.left.forwardslash.chevron.right", titel: "Programmierung",
                             untertitel: "Alle Programmier-Sessions von Mac, PC und Cloud – wie im Handy-Reiter „Programmierung“.")
                GlasKarte {
                    VStack(alignment: .leading, spacing: 10) {
                        KartenTitel(symbol: "plus.circle.fill", titel: "Neuer Programmierauftrag", farbe: Farbe.haupt)
                        TextEditor(text: $auftrag).font(.jarvis(14)).frame(minHeight: 90).scrollContentBackground(.hidden)
                            .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.tiefe(0.25)))
                        HStack {
                            DiktatKnopf(text: $auftrag)
                            KapselKnopf(symbol: "paperplane.fill", titel: "An den Programmierer", hervorgehoben: true) {
                                let t = auftrag; auftrag = ""
                                AppSteuerung.shared.neuesGespraech()
                                AppSteuerung.shared.sende("Übergib bitte folgende Aufgabe an den Programmierer: " + t)
                            }
                            KapselKnopf(symbol: "terminal.fill", titel: "Hier am Mac starten (OpenLauncher)") {
                                let t = auftrag; auftrag = ""
                                AppSteuerung.shared.neuesGespraech()
                                AppSteuerung.shared.sende("Starte eine OpenLauncher-Session mit Opus 5.5 für folgende Aufgabe: " + t)
                            }
                        }
                    }
                }
                HStack(alignment: .top, spacing: 18) {
                    GlasKarte {
                        VStack(alignment: .leading, spacing: 10) {
                            KartenTitel(symbol: "text.bubble.fill", titel: "Rückfrage beantworten", farbe: Farbe.warnung)
                            TextField("frage_id", text: $frageId).textFieldStyle(.roundedBorder)
                            TextField("Deine Antwort", text: $antwort).textFieldStyle(.roundedBorder)
                            KapselKnopf(symbol: "checkmark", titel: "Antworten", hervorgehoben: true) {
                                let f = frageId, a = antwort
                                Task {
                                    let r = await McpKlient.shared.rufe("programmier_antwort", ["frage_id": f, "text": a, "antworten": [a]])
                                    meldung = r.text; antwort = ""
                                    await liste.laden("programmierauftraege", ["anzahl": 15])
                                }
                            }
                        }
                    }
                    GlasKarte {
                        VStack(alignment: .leading, spacing: 10) {
                            KartenTitel(symbol: "plus.bubble.fill", titel: "Nachtrag an eine Session", farbe: Farbe.gold)
                            TextField("auftrag_id (z. B. BD2XW)", text: $nachtragId).textFieldStyle(.roundedBorder)
                            TextField("Nachtrag", text: $nachtrag).textFieldStyle(.roundedBorder)
                            KapselKnopf(symbol: "paperplane", titel: "Nachtrag senden", hervorgehoben: true) {
                                let i = nachtragId, t = nachtrag
                                Task {
                                    let r = await McpKlient.shared.rufe("programmier_nachtrag", ["auftrag_id": i, "text": t, "franks_worte": t])
                                    meldung = r.text; nachtrag = ""
                                }
                            }
                        }
                    }
                }
                if !meldung.isEmpty { Text(meldung).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
                GlasKarte {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            KartenTitel(symbol: "list.bullet.rectangle", titel: "Sessions")
                            Spacer()
                            if liste.laedt { ProgressView().controlSize(.small) }
                            RundKnopf(symbol: "arrow.clockwise", groesse: 28) { Task { await liste.laden("programmierauftraege", ["anzahl": 15]) } }
                        }
                        MarkdownText(text: ZentraleAnsicht.aufbereiten(liste.text ?? "", grenze: 20_000), groesse: 13)
                    }
                }
            }
            .padding(36).frame(maxWidth: 1240).frame(maxWidth: .infinity)
        }
        .task {
            await liste.laden("programmierauftraege", ["anzahl": 15])
            // Wie am Handy: Stand alle 20 s nachführen, solange der Bereich offen ist.
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(20))
                await liste.laden("programmierauftraege", ["anzahl": 15])
            }
        }
    }
}

// MARK: - Prompt-Speicher (wie Reiter „Prompts“ in der Handy-Ablage)

struct PromptEintrag: Identifiable, Hashable {
    let nummer: Int
    let titel: String
    let text: String
    var id: Int { nummer }

    static func zerlege(_ roh: String) -> [PromptEintrag] {
        var r: [PromptEintrag] = []
        var aktuell: (Int, String)?
        var zeilen: [String] = []
        func fertig() {
            if let a = aktuell { r.append(PromptEintrag(nummer: a.0, titel: a.1, text: zeilen.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines))) }
            zeilen = []
        }
        for z in roh.components(separatedBy: "\n") {
            if z.hasPrefix("(Falls in diesem Gespräch") { continue }
            if z.range(of: #"^Prompt \d+( \(.*\))?:\s*$"#, options: .regularExpression) != nil {
                let rest = z.dropFirst(7)
                let nummer = Int(rest.prefix { $0.isNumber }) ?? 0
                var titel = ""
                if let a = z.firstIndex(of: "("), let b = z.lastIndex(of: ")"), a < b { titel = String(z[z.index(after: a)..<b]) }
                fertig(); aktuell = (nummer, titel); continue
            }
            zeilen.append(z)
        }
        fertig()
        return r
    }
}

struct PromptsAnsicht: View {
    @State private var prompts: [PromptEintrag] = []
    @State private var laedt = false
    @State private var neu = ""
    @State private var offen: PromptEintrag?
    @State private var steuerung = AppSteuerung.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "plus.circle.fill", titel: "Neuer Prompt", farbe: Farbe.gruen)
                    TextEditor(text: $neu).font(.jarvis(13)).frame(minHeight: 70).scrollContentBackground(.hidden)
                        .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.tiefe(0.25)))
                    PromptWerkzeuge(text: $neu)
                    HStack {
                        KapselKnopf(symbol: "arrow.up.left.and.arrow.down.right", titel: "Im Vollbild schreiben") {
                            steuerung.promptBearbeiten = PromptEintrag(nummer: 0, titel: "", text: neu); neu = ""
                        }
                        KapselKnopf(symbol: "square.and.arrow.down.fill", titel: "Speichern", hervorgehoben: true) {
                            let t = neu
                            Task { _ = await McpKlient.shared.rufe("prompt_speichern", ["aktion": "speichern", "text": t]); neu = ""; await laden() }
                        }
                    }
                }
            }
            if laedt { ProgressView() }
            ForEach(prompts) { p in
                GlasKarte(abstand: 14) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text("\(p.nummer)").font(.jarvis(18, .heavy)).foregroundStyle(Farbe.akzent).frame(width: 30)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(p.titel.isEmpty ? "Prompt \(p.nummer)" : p.titel).font(.jarvis(14, .semibold))
                                Text(p.text).font(.jarvis(12)).foregroundStyle(Farbe.text2).lineLimit(offen == p ? nil : 2).textSelection(.enabled)
                            }
                            Spacer()
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { withAnimation(.spring(duration: 0.3)) { offen = offen == p ? nil : p } }
                        HStack(spacing: 8) {
                            Spacer()
                            KleinKnopf(symbol: "trash", hilfe: "Löschen") {
                                Task { _ = await McpKlient.shared.rufe("prompt_speichern", ["aktion": "loeschen", "nummer": p.nummer]); await laden() }
                            }
                            KleinKnopf(symbol: "doc.on.doc", hilfe: "Kopieren") {
                                NSPasteboard.general.clearContents(); NSPasteboard.general.setString(p.text, forType: .string)
                            }
                            KleinKnopf(symbol: "square.and.pencil", hilfe: "Bearbeiten") { steuerung.promptBearbeiten = p }
                            KapselKnopf(symbol: "paperplane.fill", titel: "Senden", hervorgehoben: true) {
                                AppSteuerung.shared.neuesGespraech()
                                AppSteuerung.shared.entwurf = "Übergib bitte folgende Aufgabe an den Programmierer: " + p.text
                            }
                        }
                    }
                }
            }
        }
        .task(id: steuerung.promptStand) { await laden() }
    }

    private func laden() async {
        laedt = true
        prompts = PromptEintrag.zerlege(await McpKlient.shared.text("prompts_lesen") ?? "")
        laedt = false
    }
}

// MARK: - Stil lernen aus Daumen hoch/runter (wie am Handy: daraus lernt die Stil-Datei)

@MainActor
enum StilLernen {
    static func bewerte(antwort: String, frage: String, gut: Bool) {
        log("system", "Antwort bewertet: \(gut ? "👍" : "👎")")
        Task {
            let prompt = """
            Frank hat diese Antwort von Jarvis mit \(gut ? "Daumen hoch (gefällt ihm)" : "Daumen runter (gefällt ihm nicht)") bewertet.
            Frage: \(frage.prefix(1500))
            Antwort: \(antwort.prefix(4000))

            Leite daraus höchstens EINE allgemeine, dauerhaft gültige Regel für Form, Länge, Gliederung oder Ton von Jarvis' Antworten ab \
            (nicht zum Inhalt), als vollständigen Satz an Jarvis. Ist nichts Verallgemeinerbares erkennbar, antworte genau: KEINE
            """
            guard let regel = await Kurzauftrag.frage(prompt), !regel.uppercased().hasPrefix("KEINE"), regel.count > 15 else { return }
            let r = await McpKlient.shared.rufe("jarvis_merken", ["art": "stil", "text": String(regel.prefix(400))])
            log("system", "Stil gelernt: \(regel) → \(r.fehler ? "Fehler" : "gemerkt")")
        }
    }
}

// MARK: - Teilen

struct TeilenKnopf: NSViewRepresentable {
    let text: String
    func makeNSView(context: Context) -> NSButton {
        let b = NSButton(image: NSImage(systemSymbolName: "square.and.arrow.up", accessibilityDescription: "Teilen")!, target: context.coordinator,
                         action: #selector(Koordinator.teilen(_:)))
        b.isBordered = false
        b.contentTintColor = .secondaryLabelColor
        b.toolTip = "Teilen"
        return b
    }
    func updateNSView(_ v: NSButton, context: Context) { context.coordinator.text = text }
    func makeCoordinator() -> Koordinator { Koordinator(text: text) }
    final class Koordinator: NSObject {
        var text: String
        init(text: String) { self.text = text }
        @objc func teilen(_ sender: NSButton) {
            NSSharingServicePicker(items: [text]).show(relativeTo: sender.bounds, of: sender, preferredEdge: .minY)
        }
    }
}

// MARK: - Projekte (benannte Gruppen von Gesprächen, wie „Projekte“ in der Handy-Leiste)

@MainActor @Observable
final class Projekte {
    static let shared = Projekte()
    var namen: [String] { didSet { UserDefaults.standard.set(namen, forKey: "projekte") } }
    init() { namen = UserDefaults.standard.stringArray(forKey: "projekte") ?? [] }
    func neu(_ name: String) {
        let n = name.trimmingCharacters(in: .whitespaces)
        guard !n.isEmpty, !namen.contains(n) else { return }
        namen.insert(n, at: 0)
    }
}
