import AVFoundation
import SwiftUI

struct EinstellungenAnsicht: View {
    @State private var einst = Einstellungen.shared
    @State private var reiter = EinstellungenAnsicht.startReiter
    @MainActor static var startReiter = "gehirn"

    private let reiterListe: [(String, String, String)] = [
        ("gehirn", "Jarvis' Kopf", "brain"), ("guthaben", "Guthaben", "creditcard.fill"), ("stimme", "Stimme", "speaker.wave.3.fill"), ("sprache", "Sprache & Weckwort", "mic.fill"),
        ("live", "Live-Gespräch", "waveform"), ("mac", "Mac & Rechte", "macbook"), ("konten", "Konten", "key.fill"),
        ("programmierung", "Programmierung", "chevron.left.forwardslash.chevron.right"), ("ablage", "Ablage", "tray.full.fill"), ("gedaechtnis", "Gedächtnis", "brain.head.profile.fill"), ("aktivitaet", "Aktivität", "list.bullet.rectangle.portrait.fill"),
        ("ueber", "Über", "info.circle.fill"),
    ]

    /// Diese vier bringen Kopf und Scrollen selbst mit und brauchen die Breite: Daneben schrumpft die Liste auf ihre Symbole.
    private var eigenerKopf: Bool { ["programmierung", "ablage", "gedaechtnis", "aktivitaet"].contains(reiter) }

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            if eigenerKopf {
                VStack(spacing: 6) {
                    ForEach(reiterListe, id: \.0) { r in
                        RundKnopf(symbol: r.2, hilfe: r.1, aktiv: reiter == r.0, groesse: 36) { reiter = r.0 }
                    }
                    Spacer()
                }
                .frame(width: 64)
                .padding(.top, 40)
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Einstellungen").font(.jarvis(24, .bold)).padding(.bottom, 14).padding(.leading, 8)
                    ForEach(reiterListe, id: \.0) { r in
                        LeistenEintrag(symbol: r.2, titel: r.1, aktiv: reiter == r.0) { reiter = r.0 }
                    }
                    Spacer()
                }
                .frame(width: 230)
                .padding(.top, 40).padding(.horizontal, 18)
            }
            Group {
                if eigenerKopf {
                    switch reiter {
                    case "programmierung": ProgrammierungAnsicht()
                    case "ablage": AblageAnsicht()
                    case "gedaechtnis": GedaechtnisAnsicht()
                    default: AktivitaetAnsicht()
                    }
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            switch reiter {
                            case "gehirn": gehirn
                            case "guthaben": GuthabenAnsicht()
                            case "stimme": stimme
                            case "sprache": sprache; TranskriptionsEinstellungenAnsicht()
                            case "live": live
                            case "mac": mac
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
                zeile("OpenAI-Modell") {
                    Picker("", selection: $einst.liveModellOpenAI) { ForEach(LiveOptionen.openAIModelle, id: \.id) { Text($0.name).tag($0.id) } }.labelsHidden().frame(maxWidth: 260)
                }
                zeile("OpenAI-Stimme") {
                    Picker("", selection: $einst.liveStimmeOpenAI) {
                        ForEach(einst.liveModellOpenAI == "gpt-live-1" ? LiveOptionen.liveStimmen : LiveOptionen.realtimeStimmen, id: \.id) { Text($0.name).tag($0.id) }
                    }.labelsHidden().frame(maxWidth: 260)
                }
                zeile("Gemini-Modell") {
                    Picker("", selection: $einst.liveModellGemini) { ForEach(LiveOptionen.geminiModelle, id: \.id) { Text($0.name).tag($0.id) } }.labelsHidden().frame(maxWidth: 260)
                }
                zeile("Gemini-Stimme") {
                    Picker("", selection: $einst.liveStimmeGemini) { ForEach(LiveOptionen.geminiStimmen, id: \.id) { Text("\($0.id) – \($0.name)").tag($0.id) } }.labelsHidden().frame(maxWidth: 260)
                }
            }
        }
    }

    private var mac: some View {
        VStack(spacing: 18) {
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "bolt.shield.fill", titel: "Vollzugriff", farbe: Farbe.warnung)
                    zeile("Jarvis darf am Mac alles", "Befehle, Programme, Dateien, Tastatur, Bildschirm und Terminal ohne Rückfrage – wie „Bypass Permissions“. Aus = nur Programme und Adressen öffnen, Mitteilungen, Status.") {
                        Toggle("", isOn: $einst.vollzugriff).toggleStyle(.switch)
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "checkmark.shield.fill", titel: "Rechte von macOS", farbe: Farbe.gruen)
                    Text("Einmal erteilen, dann bleibt es (Jarvis ist fest signiert).").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    RechtZeile(titel: "Mikrofon", text: "Für Weckwort, Diktat und Live.", status: Self.mikrofonStatus) {
                        AVCaptureDevice.requestAccess(for: .audio) { _ in }
                    } oeffnen: { Self.oeffne("Privacy_Microphone") }
                    RechtZeile(titel: "Spracherkennung", text: "Für die Erkennung direkt auf dem Mac.", status: nil, anfragen: nil) { Self.oeffne("Privacy_SpeechRecognition") }
                    RechtZeile(titel: "Bedienungshilfen", text: "Damit Jarvis tippen und klicken kann.", status: AXIsProcessTrusted() ? "erteilt" : "fehlt") {
                        let opt = ["AXTrustedCheckOptionPrompt": true] as CFDictionary
                        _ = AXIsProcessTrustedWithOptions(opt)
                    } oeffnen: { Self.oeffne("Privacy_Accessibility") }
                    RechtZeile(titel: "Bildschirmaufnahme", text: "Damit Jarvis deinen Bildschirm ansehen kann.", status: CGPreflightScreenCaptureAccess() ? "erteilt" : "fehlt") {
                        _ = CGRequestScreenCaptureAccess()
                    } oeffnen: { Self.oeffne("Privacy_ScreenCapture") }
                    RechtZeile(titel: "Automation", text: "Terminal, ChatGPT, System Events steuern – fragt macOS beim ersten Mal selbst.", status: nil, anfragen: nil) {
                        Self.oeffne("Privacy_Automation")
                    }
                }
            }
        }
    }

    static var mikrofonStatus: String {
        switch AVCaptureDevice.authorizationStatus(for: .audio) {
        case .authorized: return "erteilt"
        case .denied, .restricted: return "verweigert"
        default: return "offen"
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
                        Text("Version \(AppInfo.version) · Stand \(AppInfo.stand)").font(.jarvis(13)).foregroundStyle(Farbe.text2)
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

struct RechtZeile: View {
    let titel: String
    let text: String
    let status: String?
    let anfragen: (() -> Void)?
    let oeffnen: () -> Void

    init(titel: String, text: String, status: String?, anfragen: (() -> Void)?, oeffnen: @escaping () -> Void) {
        self.titel = titel; self.text = text; self.status = status; self.anfragen = anfragen; self.oeffnen = oeffnen
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: status == "erteilt" ? "checkmark.circle.fill" : "circle.dashed")
                .foregroundStyle(status == "erteilt" ? Farbe.gruen : Farbe.text3)
            VStack(alignment: .leading, spacing: 2) {
                Text(titel).font(.jarvis(13, .semibold))
                Text(text).font(.jarvis(11)).foregroundStyle(Farbe.text2)
            }
            Spacer()
            if let status { Text(status).font(.jarvis(11, .medium)).foregroundStyle(Farbe.text3) }
            if let anfragen { Button("Anfragen", action: anfragen) }
            Button("Öffnen", action: oeffnen)
        }
        .padding(.vertical, 4)
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
                    KartenTitel(symbol: "key.fill", titel: "API-Schlüssel", farbe: Farbe.warnung)
                    Text("Verschlüsselt im Tresor, gebunden an den Sicherheitschip dieses Macs. Anzeigen und Ändern nur nach Touch ID.")
                        .font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    TresorKopf()
                    TresorFeld(titel: "Anthropic (Claude)", wert: $einst.zugang.anthropic)
                    TresorFeld(titel: "OpenAI (Live, GPT)", wert: $einst.zugang.openai)
                    TresorFeld(titel: "Gemini (Live)", wert: $einst.zugang.gemini)
                    Text("Schlüssel für die Vorlesestimmen (Google, Gemini-TTS, Azure, Qwen) stehen unter Stimme.").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                    TresorFeld(titel: "Groq (Whisper)", wert: $einst.zugang.groq)
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
