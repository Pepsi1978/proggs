import SwiftUI

struct EinstellungenAnsicht: View {
    @State private var einst = Einstellungen.shared
    @State private var reiter = EinstellungenAnsicht.startReiter
    @MainActor static var startReiter = "gehirn"

    private let reiterListe: [(String, String, String)] = [
        ("gehirn", "Jarvis' Kopf", "brain"), ("guthaben", "Guthaben", "creditcard.fill"), ("stimme", "Stimme", "speaker.wave.3.fill"), ("sprache", "Sprache & Weckwort", "mic.fill"),
        ("live", "Live-Gespräch", "waveform"), ("mac", "Mac & Rechte", "macbook"), ("tools", "Eigene Tools", "wrench.and.screwdriver.fill"), ("konten", "Konten und APIs", "key.fill"),
        ("gedaechtnis", "Gedächtnis", "brain.head.profile.fill"), ("aktivitaet", "Aktivität", "list.bullet.rectangle.portrait.fill"),
        ("ueber", "Über", "info.circle.fill"),
    ]

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Einstellungen").font(.jarvis(24, .bold)).padding(.bottom, 14).padding(.leading, 8)
                ForEach(reiterListe, id: \.0) { r in
                    LeistenEintrag(symbol: r.2, titel: r.1, aktiv: reiter == r.0) { reiter = r.0 }
                }
                Spacer()
            }
            .frame(width: 230)
            .padding(.top, 40).padding(.horizontal, 18)
            Group {
                switch reiter {
                // Diese beiden bringen Kopf und Scrollen selbst mit.
                case "gedaechtnis": GedaechtnisAnsicht()
                case "aktivitaet": AktivitaetAnsicht()
                default:
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            switch reiter {
                            case "gehirn": gehirn
                            case "guthaben": GuthabenAnsicht()
                            case "stimme": stimme
                            case "sprache": sprache; TranskriptionsEinstellungenAnsicht()
                            case "live": live
                            case "mac": MacRechteAnsicht()
                            case "tools": EigeneToolsAnsicht()
                            case "konten": KontenAnsicht()
                            default: ueber
                            }
                        }
                        .padding(.horizontal, 30).padding(.top, 44).padding(.bottom, 30)
                        // minWidth 0: Auch ein zu breites Kind darf die Spalte nie aufweiten, sonst rutscht die Seitenleiste nach links.
                        .frame(minWidth: 0, maxWidth: 780, alignment: .leading)
                    }
                }
            }
            .frame(minWidth: 0, maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
            .clipped()
        }
        // Der zuletzt offene Reiter bleibt beim Wechsel zu Chat oder Zentrale erhalten.
        .onChange(of: reiter) { _, neu in EinstellungenAnsicht.startReiter = neu }
    }

    private func zeile<V: View>(_ titel: String, _ beschreibung: String = "", @ViewBuilder _ inhalt: () -> V) -> some View {
        HStack(alignment: .center, spacing: 16) {
            VStack(alignment: .leading, spacing: 3) {
                Text(titel).font(.jarvis(14, .semibold))
                if !beschreibung.isEmpty { Text(beschreibung).font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true) }
            }
            Spacer(minLength: 20)
            inhalt()
        }
        .padding(.vertical, 6)
    }

    private var gehirn: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 8) {
                KartenTitel(symbol: "brain", titel: "Jarvis' Kopf")
                zeile("Modell", "Claude über deinen Anthropic-Schlüssel oder GPT über dein ChatGPT-Konto.") {
                    Picker("", selection: $einst.modell) { ForEach(ModellWahl.alle) { Text($0.name).tag($0.id) } }.labelsHidden().frame(maxWidth: 220)
                }
                VStack(alignment: .leading, spacing: 8) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Denkstufe").font(.jarvis(14, .semibold))
                        Text("Höher = gründlicher, aber langsamer und teurer.").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    }
                    StufenWahl(stufen: ["low", "medium", "high", "xhigh", "max"], wahl: $einst.effort)
                }
                .padding(.vertical, 6)
                zeile("Schrittlimit je Anfrage", "Wie viele Werkzeugschritte Jarvis höchstens am Stück macht (0 = unbegrenzt).") {
                    Stepper("\(einst.schrittLimit)", value: $einst.schrittLimit, in: 0...200, step: 10).frame(width: 120)
                }
                zeile("Lernen am Mac", "Ruht ein Gespräch 10 Minuten, gibt Jarvis neue dauerhafte Angaben von dir ins gemeinsame Gedächtnis.") {
                    Toggle("", isOn: $einst.lernenAn).toggleStyle(.switch)
                }
            }
        }
    }

    private var stimme: some View {
        VStack(spacing: 18) {
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "speaker.wave.3.fill", titel: "Vorlesen")
                    zeile("Antworten vorlesen", "Im Sprachgespräch liest Jarvis seine Antworten vor.") { Toggle("", isOn: $einst.vorlesen).toggleStyle(.switch) }
                }
            }
            VorleseEinstellungenAnsicht()
        }
    }

    private var sprache: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 8) {
                KartenTitel(symbol: "mic.fill", titel: "Sprache & Weckwort")
                zeile("„\(einst.weckwort)“", "Jarvis lauscht am Mac auf das Weckwort, auch wenn das Fenster zu ist.") {
                    Toggle("", isOn: $einst.weckwortAn).toggleStyle(.switch)
                        .onChange(of: einst.weckwortAn) { _, an in an ? Weckwort.shared.starten() : Weckwort.shared.stoppen() }
                }
                zeile("Weckwort", "z. B. „Hey Jarvis“ oder „Okay Computer“.") {
                    TextField("", text: $einst.weckwort).textFieldStyle(.roundedBorder).frame(width: 200)
                        .onSubmit { Weckwort.shared.stoppen(); if einst.weckwortAn { Weckwort.shared.starten() } }
                }
                zeile("Stillezeit in der App", "Sekunden Stille nach deinem letzten Wort, bis das Mikrofon schließt, wenn das Gespräch bei Jarvis vorn begann und er noch vorn ist: 1 bis 15, auch mit Komma. Gilt auch für ⌥⌘J, nicht im Live-Modus.") {
                    StillezeitFeld(sekunden: $einst.stilleInAppSek)
                }
                zeile("Stillezeit außerhalb der App", "Dasselbe in allen anderen Fällen: Das Gespräch begann im Hintergrund (Fenster zu oder andere App vorn) oder Jarvis ist nicht mehr vorn.") {
                    StillezeitFeld(sekunden: $einst.stilleAussenSek)
                }
                zeile("Tastenkürzel", "⌥⌘J überall: sprechen · ⌥⌘K: Jarvis öffnen · im Fenster ⌘D sprechen, ⌘L Live, ⌘N neues Gespräch.") {
                    Toggle("", isOn: $einst.globalesKuerzel).toggleStyle(.switch)
                        .onChange(of: einst.globalesKuerzel) { _, an in an ? AppSteuerung.shared.kuerzelRegistrieren() : AppSteuerung.shared.kuerzelAbmelden() }
                }
                HStack(spacing: 8) {
                    Circle().fill(Weckwort.shared.lauscht ? Farbe.gruen : Farbe.text3).frame(width: 8, height: 8)
                    Text(Weckwort.shared.lauscht ? "Weckwort lauscht gerade" : "Weckwort lauscht nicht").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                }
            }
        }
    }

    private var live: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 8) {
                KartenTitel(symbol: "waveform", titel: "Live-Gespräch")
                zeile("Anbieter", "Gleichzeitig zuhören und sprechen, mit deinen eigenen API-Schlüsseln.") {
                    Picker("", selection: $einst.liveAnbieter) { Text("OpenAI").tag("openai"); Text("Gemini").tag("gemini") }
                        .pickerStyle(.segmented).labelsHidden().frame(maxWidth: 200)
                }
                zeile("Bei Stille beenden", "Beendet das Live-Gespräch, wenn so lange niemand spricht. GPT-Live kostet auch bei Stille weiter.") {
                    Picker("", selection: $einst.liveStilleMinuten) { ForEach(LiveOptionen.stilleEnde, id: \.minuten) { Text($0.name).tag($0.minuten) } }.labelsHidden().frame(maxWidth: 260)
                }
                if einst.liveAnbieter == "gemini" {
                    zeile("Gemini-Modell") {
                        Picker("", selection: $einst.liveModellGemini) { ForEach(LiveOptionen.geminiModelle, id: \.id) { Text($0.name).tag($0.id) } }.labelsHidden().frame(maxWidth: 260)
                    }
                    zeile("Gemini-Stimme") {
                        Picker("", selection: $einst.liveStimmeGemini) { ForEach(LiveOptionen.geminiStimmen, id: \.id) { Text("\($0.id) – \($0.name)").tag($0.id) } }.labelsHidden().frame(maxWidth: 260)
                    }
                } else {
                    zeile("OpenAI-Modell") {
                        Picker("", selection: $einst.liveModellOpenAI) { ForEach(LiveOptionen.openAIModelle, id: \.id) { Text($0.name).tag($0.id) } }.labelsHidden().frame(maxWidth: 260)
                    }
                    zeile("OpenAI-Stimme") {
                        Picker("", selection: $einst.liveStimmeOpenAI) {
                            ForEach(einst.liveModellOpenAI == "gpt-live-1" ? LiveOptionen.liveStimmen : LiveOptionen.realtimeStimmen, id: \.id) { Text($0.name).tag($0.id) }
                        }.labelsHidden().frame(maxWidth: 260)
                    }
                }
            }
        }
    }

    static func oeffne(_ bereich: String) {
        if let u = URL(string: "x-apple.systempreferences:com.apple.preference.security?\(bereich)") { NSWorkspace.shared.open(u) }
    }

    private var ueber: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 16) {
                    JarvisKugel(pegel: 0, zustand: .ruhe, groesse: 70)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Jarvis für macOS").font(.jarvis(20, .bold))
                        Text("Version \(AppInfo.version) · Build \(AppInfo.stand) Uhr").font(.jarvis(13)).foregroundStyle(Farbe.text2)
                        Text("Handy-Jarvis: \(AppSteuerung.shared.handyVerbunden ? "verbunden (\(AppSteuerung.shared.handyVersion))" : "nicht erreichbar")")
                            .font(.jarvis(13)).foregroundStyle(Farbe.text2)
                    }
                }
                Text("Dasselbe Jarvis wie auf dem Handy – gleiches Live-Gedächtnis, gleiche Apps über das Relay – plus volle Steuerung des Macs.")
                    .font(.jarvis(13)).foregroundStyle(Farbe.text2)
            }
        }
    }
}

/// Gleich breite Stufen, die immer in ihre Karte passen (ein Segmented-Picker wächst mit seinen Beschriftungen aus der Karte heraus).
struct StufenWahl: View {
    let stufen: [String]
    @Binding var wahl: String
    var body: some View {
        HStack(spacing: 4) {
            ForEach(stufen, id: \.self) { s in
                Button { wahl = s } label: {
                    Text(s).font(.jarvis(12, wahl == s ? .bold : .medium)).lineLimit(1).minimumScaleFactor(0.7)
                        .foregroundStyle(wahl == s ? Color.black : Farbe.text)
                        .frame(maxWidth: .infinity).padding(.vertical, 7)
                        .background(RoundedRectangle(cornerRadius: 9).fill(wahl == s ? AnyShapeStyle(Farbe.akzent) : AnyShapeStyle(Farbe.glas(0.07))))
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct KontenAnsicht: View {
    @State private var einst = Einstellungen.shared
    @State private var codexCode = ""
    @State private var codexStatus = ""
    @State private var relayTest = ""

    var body: some View {
        VStack(spacing: 18) {
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "iphone.radiowaves.left.and.right", titel: "Verbindung zum Handy-Jarvis", farbe: Farbe.gold)
                    Text("Über diese Relay-Adresse teilt der Mac Gedächtnis, Apps und Ablage mit deinem Handy (dieselbe wie im ChatGPT-Plugin).")
                        .font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    TresorFeld(titel: "Relay-Adresse", wert: $einst.zugang.relay)
                    HStack {
                        KapselKnopf(symbol: "antenna.radiowaves.left.and.right", titel: "Verbindung testen", hervorgehoben: true) {
                            Task {
                                let n = await McpKlient.shared.werkzeuge(frisch: true).count
                                relayTest = n > 0 ? "Verbunden – \(n) Werkzeuge vom Handy." : "Keine Verbindung (Handy aus oder Adresse falsch)."
                                AppSteuerung.shared.handyVerbunden = n > 0
                            }
                        }
                        Text(relayTest).font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "person.badge.key.fill", titel: "ChatGPT-Konto (für GPT-Modelle)", farbe: Farbe.gruen)
                    Text("Eigene Anmeldung nur für diesen Mac – das Handy bleibt angemeldet.").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    HStack(spacing: 10) {
                        KapselKnopf(symbol: "link", titel: "Mit ChatGPT anmelden", hervorgehoben: true) { anmelden() }
                        KapselKnopf(symbol: "rectangle.portrait.and.arrow.right", titel: "Abmelden") {
                            Task { await CodexAnmeldung.shared.abmelden(); codexStatus = "Abgemeldet." }
                        }
                    }
                    if !codexCode.isEmpty {
                        Text(codexCode).font(.system(size: 28, weight: .bold, design: .monospaced)).foregroundStyle(Farbe.haupt).textSelection(.enabled)
                    }
                    if !codexStatus.isEmpty { Text(codexStatus).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
                }
            }
            .task {
                if await CodexAnmeldung.shared.angemeldet {
                    codexStatus = "Angemeldet" + ((await CodexAnmeldung.shared.email).map { " als \($0)" } ?? "") + "."
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "key.fill", titel: "Konten und APIs", farbe: Farbe.warnung)
                    Text("Verschlüsselt im Tresor, gebunden an den Sicherheitschip dieses Macs. Anzeigen und Ändern nur nach Touch ID. Das schützt den Speicher, nicht vor Missbrauch durch ein bereits laufendes Programm unter deinem Benutzer.")
                        .font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    TresorKopf()
                    KontenListe()
                }
            }
        }
    }

    private func anmelden() {
        Task {
            do {
                let (code, url) = try await CodexAnmeldung.shared.start()
                codexCode = code
                NSPasteboard.general.clearContents(); NSPasteboard.general.setString(code, forType: .string)
                codexStatus = "Code kopiert – im Browser einfügen und freigeben."
                NSWorkspace.shared.open(url)
                let email = try await CodexAnmeldung.shared.warteAufFreigabe()
                codexCode = ""
                codexStatus = "Angemeldet als \(email ?? "ChatGPT-Konto")."
            } catch {
                codexStatus = "Anmeldung fehlgeschlagen: \(error.localizedDescription)"
            }
        }
    }
}

/// Eine Stillezeit in Sekunden; übernimmt mit Eingabetaste oder beim Verlassen, Ungültiges zeigt kurz einen Hinweis und fällt auf den alten Wert zurück.
private struct StillezeitFeld: View {
    @Binding var sekunden: Double
    @State private var text = ""
    @FocusState private var fokus: Bool
    @State private var ungueltig = false

    var body: some View {
        HStack(spacing: 6) {
            if ungueltig { Text("1 bis 15").font(.jarvis(12)).foregroundStyle(.red) }
            TextField("", text: $text).textFieldStyle(.roundedBorder).frame(width: 60).focused($fokus)
                .onSubmit(uebernehmen)
                .onChange(of: fokus) { _, drin in if !drin { uebernehmen() } }
            Text("Sekunden").font(.jarvis(12)).foregroundStyle(Farbe.text3)
        }
        .onAppear { text = Einstellungen.stilleZahl(sekunden) }
        // Ein Klick ins Leere nimmt dem Feld nicht sicher den Fokus: Auch beim Verlassen der Seite übernehmen.
        .onDisappear(perform: uebernehmen)
        .onChange(of: sekunden) { _, s in text = Einstellungen.stilleZahl(s) }
    }

    private func uebernehmen() {
        let s = Einstellungen.stilleAus(text)
        ungueltig = s == nil
        if let s { sekunden = s }
        text = Einstellungen.stilleZahl(sekunden)
    }
}
