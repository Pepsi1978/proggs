import AppKit
import PDFKit
import SwiftUI
import UniformTypeIdentifiers

// MARK: - Daten (Werkzeug ablage_struktur des Handys)

struct AblageDatei: Identifiable, Hashable {
    let id: String
    let name: String
    let art: String        // BILD, PDF, TEXT … (Art der Android-App)
    let kategorie: String  // BILDER, DOKUMENTE, TEXTE …
    let endung: String
    let groesse: Int
}

struct AblageEintrag: Identifiable, Hashable {
    let id: String
    let titel: String
    let geaendert: Date
    let hatText: Bool
    let recherche: Bool
    let dateien: [AblageDatei]

    static func zerlege(_ text: String) -> [AblageEintrag]? {
        // Das Handy hängt gelegentlich Hinweiszeilen an; das JSON steht immer in der ersten Zeile.
        guard let zeile = text.split(separator: "\n").first, let o = JSON.objekt(String(zeile)), let liste = o["eintraege"] as? [JSONObjekt] else { return nil }
        return liste.map { e in
            AblageEintrag(id: e["id"] as? String ?? "", titel: e["titel"] as? String ?? "",
                          geaendert: Date(timeIntervalSince1970: (e["geaendert"] as? Double ?? 0) / 1000),
                          hatText: e["hat_text"] as? Bool ?? false, recherche: e["recherche"] as? Bool ?? false,
                          dateien: (e["dateien"] as? [JSONObjekt] ?? []).map { d in
                              AblageDatei(id: d["id"] as? String ?? "", name: d["name"] as? String ?? "", art: d["art"] as? String ?? "",
                                          kategorie: d["kategorie"] as? String ?? "", endung: d["endung"] as? String ?? "", groesse: d["groesse"] as? Int ?? 0)
                          })
        }
    }
}

/// Ein Ordner des Explorers, wie die Filter der Handy-Ablage; PDFs und Recherchen sind eigene Bereiche.
enum AblageBereich: String, CaseIterable, Identifiable {
    case bilder, pdfs, prompts, recherchen, texte, dokumente, tabellen, praesentationen, audio, video, sonstige
    var id: String { rawValue }
    var titel: String {
        switch self {
        case .bilder: return "Bilder"
        case .pdfs: return "PDFs"
        case .prompts: return "Prompts"
        case .recherchen: return "Recherchen"
        case .texte: return "Texte"
        case .dokumente: return "Dokumente"
        case .tabellen: return "Tabellen"
        case .praesentationen: return "Präsentationen"
        case .audio: return "Audio"
        case .video: return "Video"
        case .sonstige: return "Sonstige"
        }
    }
    var symbol: String {
        switch self {
        case .bilder: return "photo.fill"
        case .pdfs: return "doc.richtext.fill"
        case .prompts: return "bolt.fill"
        case .recherchen: return "magnifyingglass.circle.fill"
        case .texte: return "doc.text.fill"
        case .dokumente: return "doc.fill"
        case .tabellen: return "tablecells.fill"
        case .praesentationen: return "rectangle.on.rectangle.angled"
        case .audio: return "waveform"
        case .video: return "film.fill"
        case .sonstige: return "archivebox.fill"
        }
    }
    var farbe: Color {
        switch self {
        case .bilder, .video: return Farbe.rosa
        case .pdfs, .audio: return Farbe.orange
        case .prompts, .tabellen: return Farbe.gruen
        case .recherchen, .praesentationen: return Farbe.violett
        default: return Farbe.cyan
        }
    }
}

/// Was im Explorer als ein Element erscheint: eine Datei oder ein reiner Texteintrag.
struct AblageElement: Identifiable, Hashable {
    let eintrag: AblageEintrag
    let datei: AblageDatei?
    var id: String { eintrag.id + "/" + (datei?.id ?? "text") }
    var name: String { datei?.name ?? eintrag.titel }
}

@MainActor @Observable
final class AblageDaten {
    var eintraege: [AblageEintrag] = []
    var laedt = false
    var fehler: String?
    var treffer: Set<String>?

    /// Volltextsuche am Handy: auch Wörter aus Berichten und Texten, nicht nur Namen.
    func suchen(_ wort: String) async {
        let w = wort.trimmingCharacters(in: .whitespaces)
        guard !w.isEmpty else { treffer = nil; return }
        let r = await McpKlient.shared.rufe("ablage_struktur", ["suche": w], sekunden: 40)
        guard !r.fehler, let z = r.text.split(separator: "\n").first, let o = JSON.objekt(String(z)), let ids = o["treffer"] as? [String] else { treffer = nil; return }
        treffer = Set(ids)
    }

    func laden() async {
        laedt = true; defer { laedt = false }
        let r = await McpKlient.shared.rufe("ablage_struktur", [:], sekunden: 40)
        guard !r.fehler, let liste = AblageEintrag.zerlege(r.text) else {
            fehler = r.fehler ? r.text : "Die Antwort des Handys war nicht lesbar. Läuft dort schon die neueste Jarvis-Version?"
            return
        }
        fehler = nil
        eintraege = liste
    }

    func elemente(_ b: AblageBereich, suche: String) -> [AblageElement] {
        let kategorie: String? = {
            switch b {
            case .bilder: return "BILDER"
            case .tabellen: return "TABELLEN"
            case .praesentationen: return "PRAESENTATIONEN"
            case .audio: return "AUDIO"
            case .video: return "VIDEO"
            case .sonstige: return "SONSTIGE"
            default: return nil
            }
        }()
        var r: [AblageElement] = []
        for e in eintraege {
            switch b {
            case .prompts: break
            case .recherchen:
                if e.recherche { r.append(AblageElement(eintrag: e, datei: nil)) }
            case .texte:
                // Auch der Text von Einträgen mit Dateien: Sonst wäre er am Mac nicht mehr erreichbar.
                if !e.recherche && e.hatText { r.append(AblageElement(eintrag: e, datei: nil)) }
                r += e.dateien.filter { $0.kategorie == "TEXTE" }.map { AblageElement(eintrag: e, datei: $0) }
            case .pdfs:
                r += e.dateien.filter { $0.art == "PDF" }.map { AblageElement(eintrag: e, datei: $0) }
            case .dokumente:
                r += e.dateien.filter { $0.kategorie == "DOKUMENTE" && $0.art != "PDF" }.map { AblageElement(eintrag: e, datei: $0) }
            default:
                r += e.dateien.filter { $0.kategorie == kategorie }.map { AblageElement(eintrag: e, datei: $0) }
            }
        }
        let s = suche.trimmingCharacters(in: .whitespaces).lowercased()
        return s.isEmpty ? r : r.filter { $0.name.lowercased().contains(s) || $0.eintrag.titel.lowercased().contains(s) || treffer?.contains($0.eintrag.id) == true }
    }
}

/// Das Handy nimmt höchstens vier gleichzeitige Aufrufe je Werkzeug an: Der Mac stellt sich hinten an, statt abgewiesen zu werden.
actor AbrufPlaetze {
    static let shared = AbrufPlaetze()
    private var frei = 3
    private var wartend: [CheckedContinuation<Void, Never>] = []
    func nehmen() async {
        if frei > 0 { frei -= 1; return }
        await withCheckedContinuation { wartend.append($0) }
    }
    func geben() {
        if wartend.isEmpty { frei += 1 } else { wartend.removeFirst().resume() }
    }
}

/// Lädt Dateien stückweise über das Relay (eine Antwort darf nicht beliebig groß sein) und merkt sich Vorschaubilder.
enum AblageLader {
    static let ordner: URL = {
        let u = Pfade.temp.appendingPathComponent("Ablage", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true)
        return u
    }()

    /// Ein Abruf mit Warteschlange und bis zu drei Wiederholungen.
    private static func abruf(_ argumente: JSONObjekt, sekunden: Double) async throws -> (Data, Bool) {
        var letzter = "Unlesbare Antwort vom Handy"
        for versuch in 0..<3 {
            try Task.checkCancellation()
            if versuch > 0 { try await Task.sleep(for: .seconds(Double(versuch) * 1.5)) }
            await AbrufPlaetze.shared.nehmen()
            let r = await McpKlient.shared.rufe("ablage_datei", argumente, sekunden: sekunden)
            await AbrufPlaetze.shared.geben()
            if let z = r.text.split(separator: "\n").first, !r.fehler, let o = JSON.objekt(String(z)),
               let b = o["daten"] as? String, let bytes = Data(base64Encoded: b) {
                return (bytes, o["ende"] as? Bool == true)
            }
            if r.fehler { letzter = r.text }
        }
        throw NSError(domain: "Ablage", code: 2, userInfo: [NSLocalizedDescriptionKey: letzter])
    }

    static func vorschau(_ e: AblageElement, laengste: Int) async -> NSImage? {
        guard let d = e.datei else { return nil }
        // SVG und GIF rastert das Handy nicht; kleine Dateien zeigt der Mac selbst.
        if d.art == "SVG" || d.art == "ANIMATION" {
            guard d.groesse <= 5_000_000, let u = try? await datei(e, fortschritt: { _ in }) else { return nil }
            return NSImage(contentsOf: u)
        }
        let cache = ordner.appendingPathComponent("\(d.id)-\(laengste).jpg")
        if let bild = NSImage(contentsOf: cache) { return bild }
        guard let (bytes, _) = try? await abruf(["eintrag_id": e.eintrag.id, "datei_id": d.id, "vorschau": laengste], sekunden: 60) else { return nil }
        try? bytes.write(to: cache)
        return NSImage(data: bytes)
    }

    /// Ganze Datei in den Zwischenordner, Stück für Stück direkt auf die Platte. Abbrechen und Wiederaufnahme nach einem Fehler sind möglich.
    static func datei(_ e: AblageElement, fortschritt: @MainActor (Double) -> Void) async throws -> URL {
        guard let d = e.datei else { throw NSError(domain: "Ablage", code: 1) }
        let ziel = ordner.appendingPathComponent(d.id, isDirectory: true)
        try FileManager.default.createDirectory(at: ziel, withIntermediateDirectories: true)
        let url = ziel.appendingPathComponent((d.name as NSString).lastPathComponent)
        if let a = try? FileManager.default.attributesOfItem(atPath: url.path)[.size] as? Int, a == d.groesse { return url }
        let teil = url.appendingPathExtension("part")
        if !FileManager.default.fileExists(atPath: teil.path) { FileManager.default.createFile(atPath: teil.path, contents: nil) }
        let h = try FileHandle(forWritingTo: teil)
        defer { try? h.close() }
        var versatz = Int(try h.seekToEnd())
        while true {
            let (bytes, ende) = try await abruf(["eintrag_id": e.eintrag.id, "datei_id": d.id, "versatz": versatz], sekunden: 70)
            try h.write(contentsOf: bytes)
            versatz += bytes.count
            await fortschritt(d.groesse > 0 ? Double(versatz) / Double(d.groesse) : 1)
            if ende || bytes.isEmpty { break }
        }
        try? FileManager.default.removeItem(at: url)
        try FileManager.default.moveItem(at: teil, to: url)
        // Wie beim Browser-Download: macOS warnt beim Öffnen von Programmen und Skripten aus der Ablage.
        let kennung = "0081;\(String(Int(Date().timeIntervalSince1970), radix: 16));Jarvis;"
        _ = kennung.withCString { setxattr(url.path, "com.apple.quarantine", $0, strlen($0), 0, 0) }
        return url
    }

    /// Kopien aus dem Handy nicht ewig liegen lassen.
    static func aufraeumen() {
        let grenze = Date().addingTimeInterval(-7 * 86400)
        let inhalt = (try? FileManager.default.contentsOfDirectory(at: ordner, includingPropertiesForKeys: [.contentModificationDateKey])) ?? []
        for u in inhalt {
            if let d = try? u.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate, d < grenze { try? FileManager.default.removeItem(at: u) }
        }
    }

    static func vergessen(_ d: AblageDatei) {
        let inhalt = (try? FileManager.default.contentsOfDirectory(at: ordner, includingPropertiesForKeys: nil)) ?? []
        for u in inhalt where u.lastPathComponent.hasPrefix(d.id) { try? FileManager.default.removeItem(at: u) }
    }
}

// MARK: - Explorer

struct AblageAnsicht: View {
    @State private var daten = AblageDaten()
    @State private var bereich: AblageBereich = .bilder
    @State private var suche = ""
    @State private var offen: AblageElement?
    @State private var neuOffen = false
    @State private var neuTitel = ""
    @State private var neuText = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                BereichsKopf(symbol: "tray.full.fill", titel: "Ablage", untertitel: "Dieselbe Ablage wie auf deinem Handy, nach Dateiarten sortiert.")
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass").foregroundStyle(Farbe.text3)
                    TextField("In diesem Bereich suchen, auch in Texten …", text: $suche).textFieldStyle(.plain)
                    if daten.laedt { ProgressView().controlSize(.small) }
                    RundKnopf(symbol: "arrow.clockwise", hilfe: "Neu laden", groesse: 28) { Task { await daten.laden() } }
                }
                .padding(12).background(RoundedRectangle(cornerRadius: 14).fill(Color.white.opacity(0.05)))
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 150), spacing: 10)], spacing: 10) {
                    ForEach(AblageBereich.allCases) { b in
                        ordnerKachel(b)
                    }
                }
                if let f = daten.fehler { Text(f).font(.jarvis(13)).foregroundStyle(Farbe.orange) }
                inhalt
            }
            .padding(36).frame(maxWidth: .infinity, alignment: .leading)
        }
        .task { await daten.laden() }
        .task(id: suche) {
            try? await Task.sleep(for: .seconds(0.4))
            if !Task.isCancelled { await daten.suchen(suche) }
        }
        .onChange(of: bereich) { _, b in log("ablage", "Bereich \(b.titel) geöffnet", wo: "Ablage") }
        .sheet(item: $offen) { e in
            AblageDetail(element: e) { offen = nil; Task { await daten.laden() } }
        }
    }

    private func ordnerKachel(_ b: AblageBereich) -> some View {
        let aktiv = bereich == b
        return Button { withAnimation(.easeOut(duration: 0.15)) { bereich = b } } label: {
            HStack(spacing: 10) {
                Image(systemName: b.symbol).font(.system(size: 15, weight: .semibold)).foregroundStyle(b.farbe)
                    .frame(width: 34, height: 34).background(RoundedRectangle(cornerRadius: 10).fill(b.farbe.opacity(0.13)))
                VStack(alignment: .leading, spacing: 1) {
                    Text(b.titel).font(.jarvis(13, .semibold)).foregroundStyle(Farbe.text).lineLimit(1).minimumScaleFactor(0.8)
                    if b != .prompts { Text("\(daten.elemente(b, suche: "").count)").font(.jarvis(11)).foregroundStyle(Farbe.text3) }
                }
                Spacer(minLength: 0)
            }
            .padding(10)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Color.white.opacity(aktiv ? 0.13 : 0.04)))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(aktiv ? AnyShapeStyle(b.farbe.opacity(0.7)) : AnyShapeStyle(Color.white.opacity(0.06))))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder private var inhalt: some View {
        if bereich == .prompts {
            PromptsAnsicht()
        } else {
            let liste = daten.elemente(bereich, suche: suche)
            if bereich == .texte { neuerEintrag }
            if liste.isEmpty {
                Text(daten.laedt ? "Lade …" : "Hier liegt noch nichts.").font(.jarvis(14)).foregroundStyle(Farbe.text2).padding(.top, 8)
            } else if bereich == .bilder {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 170), spacing: 12)], spacing: 12) {
                    ForEach(liste) { e in BildKachel(element: e) { offen = e } }
                }
            } else {
                LazyVStack(spacing: 6) {
                    ForEach(liste) { e in dateiZeile(e) }
                }
            }
        }
    }

    private func dateiZeile(_ e: AblageElement) -> some View {
        Button { offen = e } label: {
            HStack(spacing: 12) {
                Image(systemName: bereich.symbol).foregroundStyle(bereich.farbe).frame(width: 24)
                VStack(alignment: .leading, spacing: 2) {
                    Text(e.name).font(.jarvis(14, .medium)).foregroundStyle(Farbe.text).lineLimit(1)
                    if e.datei != nil && e.eintrag.titel != e.name {
                        Text(e.eintrag.titel).font(.jarvis(11)).foregroundStyle(Farbe.text3).lineLimit(1)
                    }
                }
                Spacer()
                if let d = e.datei { Text(Self.groesse(d.groesse)).font(.jarvis(11)).foregroundStyle(Farbe.text3) }
                Text(e.eintrag.geaendert.formatted(date: .abbreviated, time: .omitted)).font(.jarvis(11)).foregroundStyle(Farbe.text3)
            }
            .padding(.horizontal, 12).padding(.vertical, 10)
            .background(RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.04)))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    static func groesse(_ b: Int) -> String { ByteCountFormatter.string(fromByteCount: Int64(b), countStyle: .file) }

    private var neuerEintrag: some View {
        GlasKarte(abstand: 14) {
            VStack(alignment: .leading, spacing: 10) {
                Button { withAnimation(.easeOut(duration: 0.15)) { neuOffen.toggle() } } label: {
                    HStack {
                        KartenTitel(symbol: "square.and.pencil", titel: "Neuer Texteintrag", farbe: Farbe.gruen)
                        Spacer()
                        Image(systemName: neuOffen ? "chevron.up" : "chevron.down").foregroundStyle(Farbe.text3)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                if neuOffen {
                    TextField("Titel", text: $neuTitel).textFieldStyle(.roundedBorder)
                    TextEditor(text: $neuText).font(.jarvis(13)).frame(minHeight: 120).scrollContentBackground(.hidden)
                        .background(RoundedRectangle(cornerRadius: 10).fill(Color.black.opacity(0.25)))
                    KapselKnopf(symbol: "tray.and.arrow.down.fill", titel: "In die Ablage", hervorgehoben: true) {
                        let t = neuTitel, x = neuText
                        Task {
                            let r = await McpKlient.shared.rufe("ablage_schreiben", ["titel": t, "inhalt": x])
                            if r.fehler { daten.fehler = r.text } else { neuTitel = ""; neuText = ""; neuOffen = false; await daten.laden() }
                        }
                    }
                }
            }
        }
    }
}

struct BildKachel: View {
    let element: AblageElement
    let aktion: () -> Void
    @State private var bild: NSImage?
    @State private var fertig = false
    @State private var schwebt = false

    var body: some View {
        Button(action: aktion) {
            VStack(alignment: .leading, spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.05))
                    if let bild {
                        Image(nsImage: bild).resizable().scaledToFill()
                    } else if fertig {
                        Image(systemName: "photo").foregroundStyle(Farbe.text3)
                    } else {
                        ProgressView().controlSize(.small)
                    }
                }
                .frame(height: 140).frame(maxWidth: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Color.white.opacity(schwebt ? 0.3 : 0.06)))
                Text(element.name).font(.jarvis(11, .medium)).foregroundStyle(Farbe.text2).lineLimit(1)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { schwebt = $0 }
        .task {
            bild = await AblageLader.vorschau(element, laengste: 420)
            fertig = true
        }
    }
}

// MARK: - Ansicht einer Datei

struct AblageDetail: View {
    let element: AblageElement
    let geloescht: () -> Void
    @Environment(\.dismiss) private var schliessen
    @State private var bild: NSImage?
    @State private var text: String?
    @State private var pdf: URL?
    @State private var fortschritt = 0.0
    @State private var fehler: String?
    @State private var laedt = true
    @State private var loeschenFrage = false
    @State private var dateiArbeit: Task<Void, Never>?
    @State private var gekuerzt = false

    private var datei: AblageDatei? { element.datei }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Text(element.name).font(.jarvis(18, .bold)).lineLimit(1)
                Spacer()
                if datei != nil {
                    KapselKnopf(symbol: "arrow.up.forward.app", titel: "Öffnen") { starte { await oeffnen() } }
                    KapselKnopf(symbol: "square.and.arrow.down", titel: "Sichern") { starte { await sichern() } }
                }
                KapselKnopf(symbol: "trash", titel: "Löschen") { loeschenFrage = true }
                RundKnopf(symbol: "xmark", hilfe: "Schließen", groesse: 30) { schliessen() }
            }
            if let fehler { Text(fehler).font(.jarvis(13)).foregroundStyle(Farbe.orange) }
            if gekuerzt { Text("Der Text ist sehr lang und hier gekürzt; vollständig steht er auf dem Handy.").font(.jarvis(12)).foregroundStyle(Farbe.orange) }
            if dateiArbeit != nil { ProgressView(value: fortschritt).frame(maxWidth: 240) }
            Group {
                if let bild {
                    Image(nsImage: bild).resizable().scaledToFit().frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let pdf {
                    PDFAnzeige(url: pdf)
                } else if let text {
                    ScrollView { MarkdownText(text: text, groesse: 14).frame(maxWidth: .infinity, alignment: .leading).padding(4) }
                } else if laedt {
                    VStack(spacing: 10) {
                        ProgressView(value: fortschritt > 0 ? fortschritt : nil).frame(width: 240)
                        Text("Hole die Datei vom Handy …").font(.jarvis(13)).foregroundStyle(Farbe.text2)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    Text("Für diese Datei gibt es hier keine Vorschau. Öffne oder sichere sie am Mac.").font(.jarvis(14)).foregroundStyle(Farbe.text2)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
        }
        .padding(22)
        .frame(width: 900, height: 660)
        .background(Farbe.grund2)
        .preferredColorScheme(.dark)
        .task { await laden() }
        .onDisappear { dateiArbeit?.cancel() }
        .confirmationDialog(loeschenText, isPresented: $loeschenFrage) {
            Button("Löschen", role: .destructive) { Task { await loeschen() } }
        }
    }

    private var loeschenText: String {
        if datei != nil { return "Datei „\(element.name)“ löschen?" }
        let n = element.eintrag.dateien.count
        return n == 0 ? "„\(element.name)“ löschen?" : "Den ganzen Eintrag „\(element.name)“ mit \(n) Datei(en) löschen?"
    }

    /// Nur eine Dateiarbeit zur Zeit; sie endet mit dem Schließen des Fensters.
    private func starte(_ arbeit: @escaping @MainActor () async -> Void) {
        guard dateiArbeit == nil else { return }
        dateiArbeit = Task { await arbeit(); dateiArbeit = nil }
    }

    private func laden() async {
        defer { laedt = false }
        log("ablage", "Geöffnet: \(element.name)", wo: "Ablage")
        guard let d = datei else {
            let r = await McpKlient.shared.rufe("ablage_struktur", ["eintrag_id": element.eintrag.id, "nur_text": true], sekunden: 40)
            if r.fehler { fehler = r.text } else {
                let o = r.text.split(separator: "\n").first.flatMap { JSON.objekt(String($0)) }
                text = o?["text"] as? String ?? r.text
                gekuerzt = o?["gekuerzt"] as? Bool == true
            }
            return
        }
        do {
            if d.art == "BILD" {
                bild = await AblageLader.vorschau(element, laengste: 1600)
                if bild != nil { return }
            }
            if ["BILD", "PDF", "TEXT", "MARKDOWN", "CODE", "DATEN", "TABELLE_TEXT", "HTML", "ANIMATION", "SVG"].contains(d.art), d.groesse <= 40_000_000 {
                let u = try await AblageLader.datei(element) { fortschritt = $0 }
                switch d.art {
                case "PDF": pdf = u
                case "BILD", "ANIMATION", "SVG": bild = NSImage(contentsOf: u)
                default: text = (try? String(contentsOf: u, encoding: .utf8)) ?? "(Kein lesbarer Text)"
                }
            }
        } catch { fehler = error.localizedDescription }
    }

    private func oeffnen() async {
        do {
            let u = try await AblageLader.datei(element) { fortschritt = $0 }
            NSWorkspace.shared.open(u)
        } catch { fehler = error.localizedDescription }
    }

    private func sichern() async {
        do {
            let u = try await AblageLader.datei(element) { fortschritt = $0 }
            let panel = NSSavePanel()
            panel.nameFieldStringValue = u.lastPathComponent
            guard panel.runModal() == .OK, let ziel = panel.url else { return }
            try? FileManager.default.removeItem(at: ziel)
            try FileManager.default.copyItem(at: u, to: ziel)
            log("ablage", "Gesichert: \(element.name) nach \(ziel.path)", wo: "Ablage")
        } catch { fehler = error.localizedDescription }
    }

    private func loeschen() async {
        var a: JSONObjekt = ["eintrag_id": element.eintrag.id]
        if let d = datei { a["datei_id"] = d.id }
        let r = await McpKlient.shared.rufe("ablage_loeschen", a)
        if r.fehler { fehler = r.text; return }
        (datei.map { [$0] } ?? element.eintrag.dateien).forEach(AblageLader.vergessen)
        geloescht()
    }
}

struct PDFAnzeige: NSViewRepresentable {
    let url: URL
    func makeNSView(context: Context) -> PDFView {
        let v = PDFView()
        v.autoScales = true
        v.backgroundColor = .clear
        v.document = PDFDocument(url: url)
        return v
    }
    func updateNSView(_ v: PDFView, context: Context) {}
}
