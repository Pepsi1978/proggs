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
                JarvisKugel(pegel: max(live.pegelEin, live.pegelAus), zustand: kugel, groesse: 300)
            }
            .buttonStyle(.plain)
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
    func laden(_ werkzeug: String, _ args: JSONObjekt = [:]) async {
        laedt = true
        text = await McpKlient.shared.text(werkzeug, args) ?? "Das Handy-Jarvis ist gerade nicht erreichbar."
        laedt = false
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

struct AblageAnsicht: View {
    @State private var liste = BereichsLader()
    @State private var eintrag = BereichsLader()
    @State private var suche = ""
    @State private var titel = ""
    @State private var neuTitel = ""
    @State private var neuText = ""
    @State private var reiter = 0

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                BereichsKopf(symbol: "tray.full.fill", titel: "Ablage", untertitel: "Dieselbe Ablage wie auf deinem Handy – Texte, Recherchen, Dateien und Prompts.")
                Picker("", selection: $reiter) { Text("Einträge").tag(0); Text("Prompts").tag(1) }.pickerStyle(.segmented).frame(width: 240)
                if reiter == 1 { PromptsAnsicht() } else { eintraege }
            }
            .padding(36).frame(maxWidth: 1240).frame(maxWidth: .infinity)
        }
        .task { await liste.laden("ablage_lesen") }
    }

    private var eintraege: some View {
            VStack(alignment: .leading, spacing: 20) {
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass").foregroundStyle(Farbe.text3)
                    TextField("In der Ablage suchen …", text: $suche).textFieldStyle(.plain)
                        .onSubmit { Task { await liste.laden("ablage_lesen", suche.isEmpty ? [:] : ["suche": suche]) } }
                    TextField("Eintrag öffnen (Titel)", text: $titel).textFieldStyle(.plain).frame(width: 240)
                        .onSubmit { Task { await eintrag.laden("ablage_lesen", ["titel": titel]) } }
                }
                .padding(12).background(RoundedRectangle(cornerRadius: 14).fill(Color.white.opacity(0.05)))
                HStack(alignment: .top, spacing: 18) {
                    GlasKarte {
                        VStack(alignment: .leading, spacing: 10) {
                            KartenTitel(symbol: "list.bullet", titel: "Einträge")
                            if liste.laedt { ProgressView() }
                            MarkdownText(text: liste.text ?? "", groesse: 13)
                        }
                    }
                    .frame(width: 380)
                    VStack(spacing: 18) {
                        GlasKarte {
                            VStack(alignment: .leading, spacing: 10) {
                                KartenTitel(symbol: "doc.text.fill", titel: titel.isEmpty ? "Eintrag" : titel, farbe: Farbe.violett)
                                if eintrag.laedt { ProgressView() }
                                MarkdownText(text: eintrag.text ?? "Titel oben eingeben und ⏎ drücken.", groesse: 14)
                            }
                        }
                        GlasKarte {
                            VStack(alignment: .leading, spacing: 10) {
                                KartenTitel(symbol: "square.and.pencil", titel: "Neuer Texteintrag", farbe: Farbe.gruen)
                                TextField("Titel", text: $neuTitel).textFieldStyle(.roundedBorder)
                                TextEditor(text: $neuText).font(.jarvis(13)).frame(minHeight: 120).scrollContentBackground(.hidden)
                                    .background(RoundedRectangle(cornerRadius: 10).fill(Color.black.opacity(0.25)))
                                KapselKnopf(symbol: "tray.and.arrow.down.fill", titel: "In die Ablage", hervorgehoben: true) {
                                    let t = neuTitel, x = neuText
                                    Task {
                                        _ = await McpKlient.shared.rufe("ablage_schreiben", ["titel": t, "inhalt": x])
                                        neuTitel = ""; neuText = ""
                                        await liste.laden("ablage_lesen")
                                    }
                                }
                            }
                        }
                    }
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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
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
                            .frame(width: 160)
                            TextField("z. B. „Ich arbeite am Mac meistens mit Opus 5.5.“", text: $merken).textFieldStyle(.plain)
                                .padding(10).background(RoundedRectangle(cornerRadius: 10).fill(Color.white.opacity(0.05)))
                            KapselKnopf(symbol: "checkmark", titel: "Merken", hervorgehoben: true) {
                                let t = merken, a = art
                                Task {
                                    let r = await McpKlient.shared.rufe("jarvis_merken", ["art": a, "text": t])
                                    antwort = r.text; merken = ""
                                    await gemerkt.laden("jarvis_gemerktes_lesen")
                                }
                            }
                        }
                        if !antwort.isEmpty { Text(antwort).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
                    }
                }
                HStack(alignment: .top, spacing: 18) {
                    GlasKarte {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                KartenTitel(symbol: "bookmark.fill", titel: "Gemerktes", farbe: Farbe.violett)
                                Spacer()
                                RundKnopf(symbol: "arrow.clockwise", groesse: 28) { Task { await gemerkt.laden("jarvis_gemerktes_lesen") } }
                            }
                            if gemerkt.laedt { ProgressView() }
                            MarkdownText(text: gemerkt.text ?? "", groesse: 13)
                        }
                    }
                    GlasKarte {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                KartenTitel(symbol: "bolt.heart.fill", titel: "Live-Gedächtnis", farbe: Farbe.cyan)
                                Spacer()
                                RundKnopf(symbol: "arrow.clockwise", groesse: 28) { Task { await live.laden("jarvis_kontext", ["umfang": "voll"]) } }
                            }
                            if live.laedt { ProgressView() }
                            MarkdownText(text: live.text ?? "", groesse: 13)
                        }
                    }
                }
            }
            .padding(36).frame(maxWidth: 1240).frame(maxWidth: .infinity)
        }
        .task {
            async let a: () = gemerkt.laden("jarvis_gemerktes_lesen")
            async let b: () = live.laden("jarvis_kontext", [:])
            _ = await (a, b)
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
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 6) {
                        ForEach(logbuch.eintraege.reversed()) { e in
                            HStack(alignment: .top, spacing: 10) {
                                Text(e.zeit.formatted(date: .omitted, time: .standard)).font(.system(size: 11, design: .monospaced)).foregroundStyle(Farbe.text3)
                                Text(e.art).font(.jarvis(11, .bold)).foregroundStyle(farbe(e.art)).frame(width: 70, alignment: .leading)
                                Text(e.text).font(.jarvis(12)).foregroundStyle(Farbe.text2).textSelection(.enabled)
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
