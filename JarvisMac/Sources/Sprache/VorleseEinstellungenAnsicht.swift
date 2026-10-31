import SwiftUI

/// Vorlese-Einstellungen wie am Handy: Anbieter, Stimme mit Favoriten-Stern und Hörprobe, Tempo je Anbieter,
/// Schlüssel, Gemini-Schlüsselreihenfolge und Rückfallkette.
struct VorleseEinstellungenAnsicht: View {
    @State private var v = VorleseEinstellungen.shared
    @State private var stimme = Stimme.shared
    @State private var probeVon: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            anbieterKarte
            stimmenKarte
            schluesselKarte
            rueckfallKarte
        }
        .task { if v.zugang.qwen.isEmpty == false && v.klonStimmen.isEmpty { await v.klonStimmenLaden() } }
    }

    // MARK: Anbieter und Tempo

    private var anbieterKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 12) {
                KartenTitel(symbol: "speaker.wave.3.fill", titel: "Vorlesen")
                HStack {
                    Text("Anbieter").font(.jarvis(14, .semibold))
                    Spacer()
                    Picker("", selection: $v.anbieter) {
                        ForEach(VorleseAnbieter.allCases) { a in Text(a.titel).tag(a) }
                    }
                    .labelsHidden().frame(maxWidth: 260)
                }
                Text(v.anbieter.beschreibung).font(.jarvis(12)).foregroundStyle(Farbe.text2)
                if v.anbieter.kenntTempo {
                    HStack {
                        Text("Tempo").font(.jarvis(14, .semibold))
                        Spacer()
                        Slider(value: Binding(get: { v.tempo(v.anbieter) }, set: { v.setzeTempo($0, fuer: v.anbieter) }),
                               in: VorleseEinstellungen.tempoMin...VorleseEinstellungen.tempoMax, step: 0.05)
                            .frame(maxWidth: 220)
                        Text(String(format: "%.2f×", v.tempo(v.anbieter))).font(.jarvis(12)).frame(width: 50)
                    }
                    Text("Jede Stimme behält ihr eigenes Tempo, auch als Rückfallstufe.").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                } else {
                    Text("Diese Stimme spricht in ihrem eigenen Tempo.").font(.jarvis(12)).foregroundStyle(Farbe.text3)
                }
                Toggle(isOn: $v.immerDeutsch) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Immer deutsch vorlesen").font(.jarvis(14, .semibold))
                        Text("Mehrsprachige Stimmen sprechen englisch aussehende Wörter sonst englisch aus.")
                            .font(.jarvis(11)).foregroundStyle(Farbe.text2)
                    }
                }
                .toggleStyle(.switch)
                HStack(spacing: 10) {
                    KapselKnopf(symbol: "play.fill", titel: "Hörprobe", hervorgehoben: true) {
                        Task { await stimme.probe(v.stimme(v.anbieter)) }
                    }
                    KapselKnopf(symbol: "stop.fill", titel: "Stopp") { stimme.stopp() }
                }
            }
        }
    }

    // MARK: Stimmen mit Favoriten

    private var stimmenKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    KartenTitel(symbol: "person.wave.2.fill", titel: "Stimme · \(v.anbieter.kurz)", farbe: Farbe.gold)
                    Spacer()
                    if v.anbieter == .qwenKlon {
                        KapselKnopf(symbol: "arrow.clockwise", titel: "Aus dem Alibaba-Konto laden") { Task { await v.klonStimmenLaden() } }
                    }
                }
                if let f = v.klonFehler, v.anbieter == .qwenKlon { Text(f).font(.jarvis(12)).foregroundStyle(Farbe.rot) }
                let liste = v.stimmenListe(v.anbieter)
                if liste.isEmpty {
                    Text(v.anbieter == .qwenKlon ? "Noch keine geklonte Stimme geladen – Alibaba-Schlüssel eintragen und laden."
                         : "Keine Stimmen.").font(.jarvis(13)).foregroundStyle(Farbe.text2)
                } else {
                    let gewaehlt = v.stimme(v.anbieter)
                    let favorit = v.favoriten.contains(gewaehlt)
                    HStack(spacing: 10) {
                        // Favoriten stehen oben im Menü.
                        Picker("", selection: Binding(get: { v.stimme(v.anbieter) }, set: { v.setzeStimme($0, fuer: v.anbieter) })) {
                            if !liste.contains(where: { $0.id == gewaehlt }) { Text("Nicht mehr verfügbar, bitte wählen").tag(gewaehlt) }
                            ForEach(liste) { st in
                                Text("\(v.favoriten.contains(st.id) ? "★ " : "")\(st.name) · \(st.geschlecht == .weiblich ? "weiblich" : "männlich")").tag(st.id)
                            }
                        }
                        .labelsHidden().frame(minWidth: 0, maxWidth: .infinity)
                        RundKnopf(symbol: favorit ? "star.fill" : "star", hilfe: favorit ? "Favorit entfernen" : "Als Favorit markieren", aktiv: favorit, farbe: Farbe.gold, groesse: 30) { v.favoritUmschalten(gewaehlt) }
                        let probeLaeuft = probeVon == gewaehlt && stimme.spricht
                        RundKnopf(symbol: probeLaeuft ? "stop.fill" : "play.fill", hilfe: probeLaeuft ? "Hörprobe beenden" : "Hörprobe", groesse: 30) {
                            if probeLaeuft { stimme.stopp(); probeVon = nil; return }
                            probeVon = gewaehlt
                            Task { await stimme.probe(gewaehlt); if probeVon == gewaehlt { probeVon = nil } }
                        }
                    }
                }
            }
        }
    }

    // MARK: Schlüssel

    private var schluesselKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                KartenTitel(symbol: "key.fill", titel: "Schlüssel fürs Vorlesen", farbe: Farbe.warnung)
                Text("Verschlüsselt im Tresor dieses Macs, Anzeigen und Ändern nur nach Touch ID. Edge braucht keinen Schlüssel.")
                    .font(.jarvis(11)).foregroundStyle(Farbe.text2)
                TresorKopf()
                TresorFeld(titel: "Google Cloud (Chirp 3 HD)", wert: $v.zugang.google)
                TresorFeld(titel: "Gemini TTS kostenlos", wert: $v.zugang.geminiFrei)
                TresorFeld(titel: "Gemini TTS bezahlt", wert: $v.zugang.geminiBezahlt)
                HStack {
                    Text("Gemini-Reihenfolge").font(.jarvis(13, .medium)).frame(width: 150, alignment: .leading)
                    Picker("", selection: $v.geminiWahl) {
                        Text("Erst kostenlos, dann bezahlt").tag(VorleseEinstellungen.geminiFreiDannBezahlt)
                        Text("Nur kostenlos").tag(VorleseEinstellungen.geminiNurFrei)
                        Text("Nur bezahlt").tag(VorleseEinstellungen.geminiNurBezahlt)
                    }
                    .labelsHidden().frame(maxWidth: .infinity)
                }
                TresorFeld(titel: "Azure Speech-Schlüssel", wert: $v.zugang.azure)
                HStack {
                    Text("Azure-Region").font(.jarvis(13, .medium)).frame(width: 150, alignment: .leading)
                    TextField("westeurope", text: $v.zugang.azureRegion).textFieldStyle(.roundedBorder)
                }
                Text("F0 kostenlos 500 000 Zeichen im Monat; HD-Stimmen nur mit S0.").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                TresorFeld(titel: "Alibaba DashScope", wert: $v.zugang.qwen)
            }
        }
    }

    // MARK: Rückfallkette

    private var rueckfallKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                KartenTitel(symbol: "arrow.triangle.branch", titel: "Rückfall, wenn die Stimme scheitert", farbe: Farbe.gruen)
                Text("Aktive Stufen werden der Reihe nach versucht, jede mit ihrer eigenen Stimme. Zuletzt liest immer Edge, ohne Netz die Systemstimme.")
                    .font(.jarvis(11)).foregroundStyle(Farbe.text2)
                ForEach(v.kette, id: \.self) { id in
                    if let stufe = VorleseKatalog.rueckfallStufe(id) { stufenZeile(stufe, aktiv: true) }
                }
                ForEach(VorleseKatalog.rueckfallStufen.filter { !v.kette.contains($0.id) }) { stufe in
                    stufenZeile(stufe, aktiv: false)
                }
            }
        }
    }

    private func stufenZeile(_ stufe: RueckfallStufe, aktiv: Bool) -> some View {
        let bereit = v.bereit(stufe)
        return HStack(spacing: 10) {
            Toggle("", isOn: Binding(get: { aktiv }, set: { _ in v.stufeUmschalten(stufe.id) })).toggleStyle(.switch).labelsHidden()
            if aktiv, let nr = v.kette.firstIndex(of: stufe.id) {
                Text("\(nr + 1).").font(.jarvis(12, .bold)).foregroundStyle(Farbe.haupt).frame(width: 20)
            }
            Text(stufe.titel).font(.jarvis(13, aktiv ? .semibold : .regular)).foregroundStyle(aktiv ? Farbe.text : Farbe.text2)
            if !bereit { Text("Schlüssel fehlt").font(.jarvis(10, .medium)).foregroundStyle(Farbe.warnung) }
            Spacer()
            if aktiv {
                Button { v.stufeVerschieben(stufe.id, um: -1) } label: { Image(systemName: "chevron.up") }.buttonStyle(.plain)
                Button { v.stufeVerschieben(stufe.id, um: 1) } label: { Image(systemName: "chevron.down") }.buttonStyle(.plain)
            }
        }
        .padding(.vertical, 3)
    }
}
