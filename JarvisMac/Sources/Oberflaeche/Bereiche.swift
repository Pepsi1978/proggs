import AppKit
import SwiftUI

// MARK: - Live

struct LiveAnsicht: View {
    @State private var live = LiveGespraech.shared
    @State private var einst = Einstellungen.shared
    @State private var steuerung = AppSteuerung.shared

    private var kugel: JarvisKugel.KugelZustand {
        switch live.zustand {
        case .aus, .verbindet: return .ruhe
        case .hoert: return .hoert
        case .denkt: return .denkt
        case .spricht: return .spricht
        }
    }

    private var statusText: String {
        switch live.zustand {
        case .aus: return "Live-Gespräch starten"
        case .verbindet: return "Verbinde …"
        case .hoert: return "Ich höre dir zu"
        case .denkt: return "Jarvis arbeitet daran …"
        case .spricht: return "Jarvis spricht"
        }
    }

    var body: some View {
        VStack(spacing: 26) {
            Spacer(minLength: 20)
            Button { steuerung.live() } label: {
                JarvisKugel(pegel: live.zustand == .spricht ? live.pegelAus : live.pegelEin, zustand: kugel, groesse: 260)
            }
            .buttonStyle(.plain)
            .padding(.vertical, 30)
            VStack(spacing: 8) {
                Text(statusText).font(.jarvis(26, .bold))
                Text(live.aktiv ? "Sprich einfach – du kannst mich jederzeit unterbrechen." :
                        "Gleichzeitig zuhören und sprechen über \(einst.liveAnbieter == "gemini" ? "Gemini Live" : "OpenAI GPT-Live").")
                    .font(.jarvis(14)).foregroundStyle(Farbe.text2)
                if let f = live.fehler { Text(f).font(.jarvis(13, .medium)).foregroundStyle(Farbe.rot) }
            }
            HStack(spacing: 12) {
                if live.aktiv {
                    KapselKnopf(symbol: "stop.fill", titel: "Gespräch beenden", hervorgehoben: true) { live.beenden() }
                } else {
                    KapselKnopf(symbol: "waveform", titel: "Live starten", hervorgehoben: true) { steuerung.live() }
                    Picker("", selection: $einst.liveAnbieter) {
                        Text("OpenAI GPT-Live").tag("openai")
                        Text("Gemini Live").tag("gemini")
                    }
                    .pickerStyle(.segmented).frame(width: 260)
                }
            }
            if !live.mitschrift.isEmpty {
                ScrollViewReader { l in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 10) {
                            ForEach(live.mitschrift) { z in
                                HStack(alignment: .top, spacing: 10) {
                                    Text(z.vonFrank ? "Du" : "Jarvis").font(.jarvis(12, .bold))
                                        .foregroundStyle(z.vonFrank ? Farbe.gruen : Farbe.cyan).frame(width: 50, alignment: .trailing)
                                    Text(z.text).font(.jarvis(14)).foregroundStyle(Farbe.text).textSelection(.enabled)
                                }
                                .id(z.id)
                            }
                        }
                        .padding(20)
                    }
                    .frame(maxWidth: 760, maxHeight: 260)
                    .background(RoundedRectangle(cornerRadius: 20).fill(Color.white.opacity(0.04)))
                    .onChange(of: live.mitschrift.count) { _, _ in if let id = live.mitschrift.last?.id { l.scrollTo(id) } }
                }
            }
            Spacer(minLength: 20)
        }
        .padding(30)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Werkzeug-Text-Bereich (Ablage, Gedächtnis, Aktivität am Handy)

@MainActor @Observable
final class BereichsLader {
    var text: String?
    var laedt = false
    /// Gelingt das Laden nicht, bleibt der letzte gute Stand stehen.
    @discardableResult
    func laden(_ werkzeug: String, _ args: JSONObjekt = [:]) async -> Bool {
        laedt = true; defer { laedt = false }
        guard let t = await McpKlient.shared.text(werkzeug, args) else {
            if text == nil { text = "Das Handy-Jarvis ist gerade nicht erreichbar." }
            return false
        }
        text = t
        return true
    }
}

struct BereichsKopf: View {
    let symbol: String
    let titel: String
    let untertitel: String
    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: symbol).font(.system(size: 20, weight: .semibold)).foregroundStyle(Farbe.akzent)
                .frame(width: 46, height: 46).background(RoundedRectangle(cornerRadius: 14).fill(Color.white.opacity(0.06)))
            VStack(alignment: .leading, spacing: 3) {
                Text(titel).font(.jarvis(26, .bold))
                Text(untertitel).font(.jarvis(13)).foregroundStyle(Farbe.text2)
            }
            Spacer()
        }
    }
}

/// Eine einklappbare Karte: klein, solange sie zu ist; lädt erst beim ersten Aufklappen.
struct KlappKarte<Inhalt: View>: View {
    let symbol: String
    let titel: String
    let farbe: Color
    var untertitel = ""
    @Binding var offen: Bool
    @ViewBuilder var inhalt: Inhalt

    var body: some View {
        GlasKarte(abstand: 14) {
            VStack(alignment: .leading, spacing: 12) {
                Button { withAnimation(.easeOut(duration: 0.15)) { offen.toggle() } } label: {
                    HStack(spacing: 10) {
                        KartenTitel(symbol: symbol, titel: titel, farbe: farbe)
                        if !untertitel.isEmpty { Text(untertitel).font(.jarvis(12)).foregroundStyle(Farbe.text3).lineLimit(1) }
                        Spacer()
                        Image(systemName: "chevron.down").foregroundStyle(Farbe.text3).rotationEffect(.degrees(offen ? 180 : 0))
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                if offen { inhalt }
            }
        }
    }
}

struct GedaechtnisAnsicht: View {
    @State private var gemerkt = BereichsLader()
    @State private var live = BereichsLader()
    @State private var merken = ""
    @State private var art = "info"
    @State private var antwort = ""
    @State private var gemerktOffen = false
    @State private var liveOffen = false
    @State private var langzeitOffen = false
    @State private var liveStand: Date?
    @State private var liveVoll = false
    @State private var liveWeg = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                BereichsKopf(symbol: "brain.head.profile.fill", titel: "Gedächtnis",
                             untertitel: "Ein Gedächtnis für alle Geräte: Was du hier sagst oder merkst, weiß Jarvis auch auf dem Handy.")
                GlasKarte {
                    VStack(alignment: .leading, spacing: 10) {
                        KartenTitel(symbol: "plus.circle.fill", titel: "Etwas merken", farbe: Farbe.gruen)
                        HStack {
                            Picker("", selection: $art) {
                                Text("Über Frank").tag("info"); Text("Regel").tag("regel"); Text("Ziel").tag("ziel")
                                Text("Entscheidung").tag("entscheidung"); Text("Stil").tag("stil")
                            }
                            .labelsHidden().frame(width: 150)
                            TextField("z. B. „Ich arbeite am Mac meistens mit Opus 5.5.“", text: $merken).textFieldStyle(.plain)
                                .padding(10).background(RoundedRectangle(cornerRadius: 10).fill(Color.white.opacity(0.05)))
                            KapselKnopf(symbol: "checkmark", titel: "Merken", hervorgehoben: true) {
                                let t = merken, a = art
                                Task {
                                    let r = await McpKlient.shared.rufe("jarvis_merken", ["art": a, "text": t])
                                    antwort = r.text; merken = ""
                                    if gemerktOffen { await gemerkt.laden("jarvis_gemerktes_lesen") }
                                }
                            }
                        }
                        if !antwort.isEmpty { Text(antwort).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
                    }
                }
                KlappKarte(symbol: "doc.plaintext.fill", titel: "Langzeitkontext", farbe: Farbe.orange, untertitel: "Deine Textdatei für Jarvis, auf Mac und Handy gleich", offen: $langzeitOffen) {
                    LangzeitkontextEditor()
                }
                KlappKarte(symbol: "bookmark.fill", titel: "Gemerktes", farbe: Farbe.violett, offen: $gemerktOffen) {
                    HStack {
                        if gemerkt.laedt { ProgressView().controlSize(.small) }
                        Spacer()
                        RundKnopf(symbol: "arrow.clockwise", hilfe: "Neu laden", groesse: 28) { Task { await gemerkt.laden("jarvis_gemerktes_lesen") } }
                    }
                    MarkdownText(text: gemerkt.text ?? "", groesse: 13).frame(maxWidth: .infinity, alignment: .leading)
                }
                KlappKarte(symbol: "bolt.heart.fill", titel: "Live-Gedächtnis", farbe: Farbe.cyan,
                           untertitel: liveWeg ? "Handy nicht erreichbar, letzter Stand" + (liveStand.map { " \($0.formatted(date: .omitted, time: .shortened))" } ?? "")
                                     : liveStand.map { "vom Handy, Stand \($0.formatted(date: .omitted, time: .shortened))" } ?? "", offen: $liveOffen) {
                    HStack {
                        if live.laedt { ProgressView().controlSize(.small) }
                        Spacer()
                        RundKnopf(symbol: "arrow.clockwise", hilfe: "Alles neu laden", groesse: 28) { liveVoll = true; Task { await liveLaden() } }
                    }
                    MarkdownText(text: live.text ?? "", groesse: 13).frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(36).frame(maxWidth: .infinity, alignment: .leading)
        }
        .onChange(of: gemerktOffen) { _, an in if an { Task { await gemerkt.laden("jarvis_gemerktes_lesen") } } }
        // Der Mac hat kein eigenes Gedächtnis: Solange die Karte offen ist, kommt jede Minute der aktuelle Stand vom Handy.
        .task(id: liveOffen) {
            guard liveOffen else { return }
            while !Task.isCancelled {
                await liveLaden()
                try? await Task.sleep(for: .seconds(60))
            }
        }
    }

    /// „leise“: Die Anzeige verbraucht keine Meldungen, die für Gespräche in ChatGPT oder am Handy gedacht sind.
    private func liveLaden() async {
        var a: JSONObjekt = ["leise": true]
        if liveVoll { a["umfang"] = "voll" }
        let ok = await live.laden("jarvis_kontext", a)
        liveWeg = !ok
        if ok { liveStand = Date() }
    }
}

/// Der Entwurf lebt außerhalb der Ansicht: Zuklappen oder ein Bereichswechsel dürfen ihn nicht verwerfen.
@MainActor @Observable
final class LangzeitEntwurf {
    static let shared = LangzeitEntwurf()
    var text = ""
    var gelesen = ""
    var stand: Int64 = -1
    var laedt = false
    var meldung = ""
    var fehlerMeldung = false
    var konflikt = false
    var geaendert: Bool { text != gelesen }

    func laden(erzwingen: Bool = false) async {
        guard erzwingen || !geaendert else { return }
        laedt = true; defer { laedt = false }
        let r = await McpKlient.shared.rufe("jarvis_langzeitkontext_lesen", [:])
        guard !r.fehler, r.text.hasPrefix("STAND ") else {
            meldung = r.fehler ? r.text : "Die Antwort des Handys war nicht lesbar. Läuft dort schon die neueste Jarvis-Version?"
            fehlerMeldung = true
            return
        }
        let (s, t) = Self.zerlege(r.text)
        stand = s; text = t; gelesen = t; meldung = ""; konflikt = false
    }

    /// Aufbau: erste Zeile „STAND <zahl>“, danach der Text. Das Handy kann Hinweiszeilen nur ans Ende hängen, hier hängt es keine an.
    private static func zerlege(_ antwort: String) -> (Int64, String) {
        let ende = antwort.firstIndex(of: "\n") ?? antwort.endIndex
        let stand = Int64(antwort[antwort.index(antwort.startIndex, offsetBy: 6)..<ende]) ?? 0
        return (stand, ende == antwort.endIndex ? "" : String(antwort[antwort.index(after: ende)...]))
    }

    /// „überschreiben“: Frank hat im Konflikt entschieden, dass seine Fassung gilt. Die vorige bleibt am Handy als Vorfassung erhalten.
    func speichern(ueberschreiben: Bool = false) async {
        var basis = stand
        if ueberschreiben {
            let r = await McpKlient.shared.rufe("jarvis_langzeitkontext_lesen", [:])
            guard !r.fehler, r.text.hasPrefix("STAND ") else { meldung = r.text; fehlerMeldung = true; return }
            basis = Self.zerlege(r.text).0
        }
        let t = text
        let r = await McpKlient.shared.rufe("jarvis_langzeitkontext_ersetzen", ["text": t, "basis_stand": String(basis)])
        fehlerMeldung = r.fehler
        konflikt = r.fehler && r.text.hasPrefix("KONFLIKT")
        meldung = konflikt ? "Am Handy gibt es eine neuere Fassung. Entscheide, welche gilt." : (r.fehler ? r.text : "Gespeichert, auf Mac und Handy gleich.")
        log("gedaechtnis", "Langzeitkontext gespeichert (\(t.count) Zeichen)", wo: "Gedächtnis", fehler: r.fehler ? r.text : nil)
        if !r.fehler { stand = Self.zerlege(r.text).0; gelesen = t }
    }
}

/// Die Textdatei liegt auf dem Handy; der Mac liest und schreibt sie über das Relay, dadurch gibt es nur einen Stand.
struct LangzeitkontextEditor: View {
    @State private var e = LangzeitEntwurf.shared
    @State private var frageVerwerfen = false
    @State private var frageLeeren = false

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Jarvis liest diesen Text bei jeder Frage mit. Füg zum Beispiel ein, was ChatGPT sich über dich gemerkt hat.")
                .font(.jarvis(12)).foregroundStyle(Farbe.text2)
            TextEditor(text: $e.text).font(.jarvis(13)).frame(minHeight: 280).scrollContentBackground(.hidden)
                .padding(6).background(RoundedRectangle(cornerRadius: 10).fill(Color.black.opacity(0.25)))
                .disabled(e.laedt)
            HStack(spacing: 10) {
                KapselKnopf(symbol: "square.and.arrow.down.fill", titel: "Speichern", hervorgehoben: e.geaendert) {
                    if e.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !e.gelesen.isEmpty { frageLeeren = true } else { Task { await e.speichern() } }
                }
                KapselKnopf(symbol: "arrow.clockwise", titel: "Vom Handy laden") {
                    if e.geaendert { frageVerwerfen = true } else { Task { await e.laden(erzwingen: true) } }
                }
                if e.laedt { ProgressView().controlSize(.small) }
                Spacer()
                Text("\(e.text.count) Zeichen\(e.geaendert ? ", nicht gespeichert" : "")").font(.jarvis(11)).foregroundStyle(e.geaendert ? Farbe.orange : Farbe.text3)
            }
            if e.konflikt {
                HStack(spacing: 10) {
                    KapselKnopf(symbol: "iphone", titel: "Fassung vom Handy") { frageVerwerfen = true }
                    KapselKnopf(symbol: "macbook", titel: "Meine Fassung speichern", hervorgehoben: true) { Task { await e.speichern(ueberschreiben: true) } }
                }
            }
            if !e.meldung.isEmpty { Text(e.meldung).font(.jarvis(12)).foregroundStyle(e.fehlerMeldung ? Farbe.orange : Farbe.gruen) }
        }
        .task { await e.laden() }
        .confirmationDialog("Deine ungespeicherten Änderungen verwerfen?", isPresented: $frageVerwerfen) {
            Button("Verwerfen und laden", role: .destructive) { Task { await e.laden(erzwingen: true) } }
        }
        .confirmationDialog("Den ganzen Langzeitkontext leeren?", isPresented: $frageLeeren) {
            Button("Leeren", role: .destructive) { Task { await e.speichern() } }
        }
    }
}

struct AktivitaetAnsicht: View {
    @State private var logbuch = Logbuch.shared
    @State private var protokoll = BereichsLader()
    @State private var reiter = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            BereichsKopf(symbol: "list.bullet.rectangle.portrait.fill", titel: "Aktivität", untertitel: "Was Jarvis am Mac und am Handy zuletzt getan hat.")
            Picker("", selection: $reiter) {
                Text("Am Mac").tag(0); Text("Am Handy").tag(1)
            }
            .pickerStyle(.segmented).frame(width: 260)
            if reiter == 0 {
                HStack {
                    Text("Ausführliches Logbuch der letzten \(Logbuch.aufbewahrungTage) Tage, für jede KI lesbar (JSON je Zeile).").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    Spacer()
                    KapselKnopf(symbol: "folder.fill", titel: "Im Finder zeigen") { NSWorkspace.shared.open(Logbuch.ordner) }
                }
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 6) {
                        ForEach(logbuch.eintraege.reversed()) { e in
                            HStack(alignment: .top, spacing: 10) {
                                Text(e.zeit.formatted(date: .omitted, time: .standard)).font(.system(size: 11, design: .monospaced)).foregroundStyle(Farbe.text3)
                                Text(e.art).font(.jarvis(11, .bold)).foregroundStyle(farbe(e.art)).lineLimit(1).frame(width: 84, alignment: .leading)
                                Text(e.anzeige).font(.jarvis(12)).foregroundStyle(Farbe.text2).textSelection(.enabled)
                            }
                        }
                    }
                    .padding(18)
                }
                .background(RoundedRectangle(cornerRadius: 18).fill(Color.white.opacity(0.03)))
            } else {
                ScrollView { MarkdownText(text: protokoll.text ?? "Lade …", groesse: 13).padding(18) }
                    .background(RoundedRectangle(cornerRadius: 18).fill(Color.white.opacity(0.03)))
                    .task { await protokoll.laden("protokoll_lesen") }
            }
        }
        .padding(36)
    }

    private func farbe(_ art: String) -> Color {
        switch art {
        case "fehler": return Farbe.rot
        case "werkzeug": return Farbe.violett
        case "gehirn": return Farbe.cyan
        case "sprache", "live": return Farbe.gruen
        case "tresor": return Farbe.orange
        case "ablage", "gedaechtnis", "gespraech": return Farbe.rosa
        default: return Farbe.text3
        }
    }
}

// MARK: - Menüleiste

struct MenueLeiste: View {
    @State private var steuerung = AppSteuerung.shared
    @State private var schleife = SprachSchleife.shared
    @State private var frage = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                JarvisKugel(pegel: 0, zustand: schleife.zustand == .aus ? .ruhe : .hoert, groesse: 34)
                VStack(alignment: .leading, spacing: 1) {
                    Text("JARVIS").font(.jarvis(13, .heavy)).tracking(3)
                    Text(steuerung.handyVerbunden ? "Mit dem Handy verbunden" : "Handy nicht erreichbar").font(.jarvis(10)).foregroundStyle(Farbe.text2)
                }
                Spacer()
            }
            TextField("Frag Jarvis …", text: $frage).textFieldStyle(.roundedBorder)
                .onSubmit {
                    let t = frage; frage = ""
                    steuerung.gespraech = nil; steuerung.sende(t); steuerung.fensterZeigen()
                }
            HStack(spacing: 8) {
                KapselKnopf(symbol: "mic.fill", titel: "Sprechen", hervorgehoben: true) { steuerung.kuerzelSprechen() }
                KapselKnopf(symbol: "waveform", titel: "Live") { steuerung.fensterZeigen(); steuerung.live() }
            }
            Divider()
            Button("Jarvis öffnen  ⌥⌘K") { steuerung.fensterZeigen() }.buttonStyle(.plain)
            Button("Beenden") { NSApp.terminate(nil) }.buttonStyle(.plain).foregroundStyle(Farbe.text2)
        }
        .padding(16)
        .frame(width: 300)
        .preferredColorScheme(.dark)
    }
}
