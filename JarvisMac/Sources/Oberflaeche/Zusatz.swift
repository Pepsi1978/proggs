import AppKit
import SwiftUI

// MARK: - Prompt-Speicher (wie Reiter „Prompts“ in der Handy-Ablage)

struct PromptEintrag: Identifiable, Hashable {
    var nummer: Int
    let titel: String
    let text: String
    /// Feste Kennung des Handys (Anlagezeitpunkt); 0 = unbekannt.
    var erstellt: Int64 = 0
    /// Die Jarvis-Version, von der der Prompt stammt.
    var quelle = ""
    var id: Int64 { erstellt != 0 ? erstellt : Int64(nummer) }
}

/// Eigene Ansicht, damit Tippen im Entwurf nicht jedes Mal die ganze Prompt-Liste neu auswertet.
struct NeuerPromptKarte: View {
    @State private var abgleich = AblageAbgleich.shared
    @State private var steuerung = AppSteuerung.shared
    @State private var fehler = ""

    var body: some View {
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "plus.circle.fill", titel: "Neuer Prompt", farbe: Farbe.gruen)
                    TextEditor(text: $steuerung.promptEntwurf).font(.jarvis(13)).frame(minHeight: 70).scrollContentBackground(.hidden)
                        .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.tiefe(0.25)))
                    PromptWerkzeuge(text: $steuerung.promptEntwurf)
                    HStack {
                        KapselKnopf(symbol: "arrow.up.left.and.arrow.down.right", titel: "Im Vollbild schreiben") {
                            steuerung.promptBearbeiten = PromptEintrag(nummer: 0, titel: "", text: steuerung.promptEntwurf)
                        }
                        KapselKnopf(symbol: "square.and.arrow.down.fill", titel: "Speichern", hervorgehoben: true) {
                            let t = steuerung.promptEntwurf
                            Task {
                                // Nur Gespeichertes verschwindet aus dem Feld.
                                do { try await abgleich.promptSpeichern(t) } catch { fehler = error.localizedDescription; return }
                                fehler = ""
                                if steuerung.promptEntwurf == t { steuerung.promptEntwurf = "" }
                            }
                        }
                    }
                    if !fehler.isEmpty { Text(fehler).font(.jarvis(12)).foregroundStyle(Farbe.warnung) }
                }
            }
    }
}

struct PromptsAnsicht: View {
    @State private var abgleich = AblageAbgleich.shared
    @State private var fehler = ""
    @State private var quellenOffen = false
    @State private var loeschFrage: PromptEintrag?
    @AppStorage("prompts.nachQuelle") private var nachQuelle = false
    @AppStorage("ablage.quellen") private var quellenWert = QuellenOrdnung.alle.joined(separator: ",")
    @State private var offen: PromptEintrag?
    @State private var steuerung = AppSteuerung.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            NeuerPromptKarte()
            HStack {
                Spacer()
                Menu {
                    Picker("Sortieren nach", selection: $nachQuelle) {
                        Text("Eigene Reihenfolge").tag(false)
                        Text("Nach Quelle").tag(true)
                    }
                    .pickerStyle(.inline)
                    Button("Reihenfolge der Quellen …") { quellenOffen = true }
                } label: {
                    Label(nachQuelle ? "Nach Quelle" : "Eigene Reihenfolge", systemImage: "arrow.up.arrow.down").font(.jarvis(12))
                }
                .menuStyle(.borderlessButton).fixedSize()
                .popover(isPresented: $quellenOffen) { QuellenListe() }
            }
            // Lazy: Bei vielen Prompts bauten sonst jede eintreffende Änderung alle Karten neu.
            LazyVStack(alignment: .leading, spacing: 16) { ForEach(angezeigt) { p in
                GlasKarte(abstand: 14) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text("\(p.nummer)").font(.jarvis(18, .heavy)).foregroundStyle(Farbe.akzent).frame(width: 30)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(p.titel.isEmpty ? "Prompt \(p.nummer)" : p.titel).font(.jarvis(14, .semibold))
                                Text("Von \(p.quelle)").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                                Text(p.text).font(.jarvis(12)).foregroundStyle(Farbe.text2).lineLimit(offen?.id == p.id ? nil : 2).textSelection(.enabled)
                            }
                            Spacer()
                        }
                        .contentShape(Rectangle())
                        .onTapGesture { withAnimation(.spring(duration: 0.3)) { offen = offen?.id == p.id ? nil : p } }
                        HStack(spacing: 8) {
                            Spacer()
                            KleinKnopf(symbol: "trash", hilfe: "Löschen") {
                                loeschFrage = p
                            }
                            KleinKnopf(symbol: "doc.on.doc", hilfe: "Kopieren") {
                                NSPasteboard.general.clearContents(); NSPasteboard.general.setString(p.text, forType: .string)
                            }
                            KleinKnopf(symbol: "square.and.pencil", hilfe: "Bearbeiten") { steuerung.promptBearbeiten = p }
                            KapselKnopf(symbol: "paperplane.fill", titel: "Senden", hervorgehoben: true) {
                                AppSteuerung.shared.neuesGespraech()
                                AppSteuerung.shared.entwurf = "Übergib bitte folgende Aufgabe an den Programmierer: " + p.text
                                AppSteuerung.shared.promptLoeschenFrage = p
                            }
                        }
                    }
                }
            } }
        }
        .confirmationDialog("Prompt \(loeschFrage?.nummer ?? 0) auf allen Jarvis-Versionen löschen?", isPresented: Binding(get: { loeschFrage != nil }, set: { if !$0 { loeschFrage = nil } })) {
            Button("Löschen", role: .destructive) {
                guard let p = loeschFrage else { return }
                Task { do { try await abgleich.promptLoeschen(erstellt: p.erstellt); fehler = "" } catch { fehler = error.localizedDescription } }
            }
        }
    }

    /// Nach Quelle sortiert ändert nur die Anzeige: Innerhalb einer Quelle und bei den Nummern bleibt es bei der eigenen Reihenfolge.
    private var angezeigt: [PromptEintrag] {
        guard nachQuelle else { return abgleich.prompts }
        let o = QuellenOrdnung.liste(quellenWert)
        return abgleich.prompts.sorted { a, b in
            let x = QuellenOrdnung.rang(o, a.quelle), y = QuellenOrdnung.rang(o, b.quelle)
            return x != y ? x < y : a.nummer < b.nummer
        }
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
