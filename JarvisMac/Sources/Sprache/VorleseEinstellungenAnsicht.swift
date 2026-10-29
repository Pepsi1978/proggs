import SwiftUI

/// Vorlese-Einstellungen wie am Handy: Anbieter, Stimme mit Favoriten-Stern und Hörprobe, Tempo je Anbieter,
/// Schlüssel, Gemini-Schlüsselreihenfolge und Rückfallkette.
struct VorleseEinstellungenAnsicht: View {
    @State private var v = VorleseEinstellungen.shared
    @State private var stimme = Stimme.shared
    @State private var probeLaeuft: String?

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
                    .frame(width: 260)
                }
                Text(v.anbieter.beschreibung).font(.jarvis(12)).foregroundStyle(Farbe.text2)
                if v.anbieter.kenntTempo {
                    HStack {
                        Text("Tempo").font(.jarvis(14, .semibold))
                        Spacer()
                        Slider(value: Binding(get: { v.tempo(v.anbieter) }, set: { v.setzeTempo($0, fuer: v.anbieter) }),
                               in: VorleseEinstellungen.tempoMin...VorleseEinstellungen.tempoMax, step: 0.05)
                            .frame(width: 220)
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
                    KartenTitel(symbol: "person.wave.2.fill", titel: "Stimme · \(v.anbieter.kurz)", farbe: Farbe.violett)
                    Spacer()
                    if v.anbieter == .qwenKlon {
                        KapselKnopf(symbol: "arrow.clockwise", titel: "Aus dem Alibaba-Konto laden") { Task { await v.klonStimmenLaden() } }
                    }
                }
                Text("Stern = Favorit (steht oben). Rechtsklick geht auch. ▶ spielt eine Hörprobe.")
                    .font(.jarvis(11)).foregroundStyle(Farbe.text3)
                if let f = v.klonFehler, v.anbieter == .qwenKlon { Text(f).font(.jarvis(12)).foregroundStyle(Farbe.rot) }
                let liste = v.stimmenListe(v.anbieter)
                if liste.isEmpty {
                    Text(v.anbieter == .qwenKlon ? "Noch keine geklonte Stimme geladen – Alibaba-Schlüssel eintragen und laden."
                         : "Keine Stimmen.").font(.jarvis(13)).foregroundStyle(Farbe.text2)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 2) {
                            ForEach(liste) { st in zeile(st) }
                        }
                    }
                    .frame(maxHeight: 340)
                }
            }
        }
    }

    private func zeile(_ st: VorleseStimme) -> some View {
        let gewaehlt = v.stimme(v.anbieter) == st.id
        let favorit = v.favoriten.contains(st.id)
        return HStack(spacing: 10) {
            Button { v.favoritUmschalten(st.id) } label: {
                Image(systemName: favorit ? "star.fill" : "star").foregroundStyle(favorit ? Color.yellow : Farbe.text3)
            }
            .buttonStyle(.plain).help(favorit ? "Favorit entfernen" : "Als Favorit markieren")
            VStack(alignment: .leading, spacing: 1) {
                Text(st.name).font(.jarvis(13, gewaehlt ? .semibold : .regular)).foregroundStyle(Farbe.text)
                Text(st.geschlecht == .weiblich ? "weiblich" : "männlich").font(.jarvis(10)).foregroundStyle(Farbe.text3)
            }
            Spacer()
            if gewaehlt { Image(systemName: "checkmark.circle.fill").foregroundStyle(Farbe.gruen) }
            Button {
                probeLaeuft = st.id
                Task { await stimme.probe(st.id); if probeLaeuft == st.id { probeLaeuft = nil } }
            } label: {
                Image(systemName: probeLaeuft == st.id && stimme.spricht ? "speaker.wave.2.fill" : "play.circle")
                    .foregroundStyle(Farbe.cyan)
            }
            .buttonStyle(.plain).help("Hörprobe")
        }
        .padding(.horizontal, 10).padding(.vertical, 7)
        .background(RoundedRectangle(cornerRadius: 9).fill(Color.white.opacity(gewaehlt ? 0.09 : 0.02)))
        .contentShape(Rectangle())
        .onTapGesture { v.setzeStimme(st.id, fuer: v.anbieter) }
        .contextMenu {
            Button(favorit ? "Favorit entfernen" : "Als Favorit markieren") { v.favoritUmschalten(st.id) }
            Button("Hörprobe") { Task { await stimme.probe(st.id) } }
            Button("Auswählen") { v.setzeStimme(st.id, fuer: v.anbieter) }
        }
    }

    // MARK: Schlüssel

    private var schluesselKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                KartenTitel(symbol: "key.fill", titel: "Schlüssel fürs Vorlesen", farbe: Farbe.orange)
                Text("Liegen nur auf diesem Mac (Application Support, nur für dich lesbar). Edge braucht keinen Schlüssel.")
                    .font(.jarvis(11)).foregroundStyle(Farbe.text2)
                feld("Google Cloud (Chirp 3 HD)", $v.zugang.google)
                feld("Gemini TTS kostenlos", $v.zugang.geminiFrei)
                feld("Gemini TTS bezahlt", $v.zugang.geminiBezahlt)
                HStack {
                    Text("Gemini-Reihenfolge").font(.jarvis(13, .medium)).frame(width: 190, alignment: .leading)
                    Picker("", selection: $v.geminiWahl) {
                        Text("Erst kostenlos, dann bezahlt").tag(VorleseEinstellungen.geminiFreiDannBezahlt)
                        Text("Nur kostenlos").tag(VorleseEinstellungen.geminiNurFrei)
                        Text("Nur bezahlt").tag(VorleseEinstellungen.geminiNurBezahlt)
                    }
                    .pickerStyle(.segmented)
                }
                feld("Azure Speech-Schlüssel", $v.zugang.azure)
                HStack {
                    Text("Azure-Region").font(.jarvis(13, .medium)).frame(width: 190, alignment: .leading)
                    TextField("westeurope", text: $v.zugang.azureRegion).textFieldStyle(.roundedBorder)
                }
                Text("F0 kostenlos 500 000 Zeichen im Monat; HD-Stimmen nur mit S0.").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                feld("Alibaba DashScope", $v.zugang.qwen)
            }
        }
    }

    private func feld(_ titel: String, _ wert: Binding<String>) -> some View {
        HStack {
            Image(systemName: wert.wrappedValue.isEmpty ? "xmark.circle" : "checkmark.circle.fill")
                .foregroundStyle(wert.wrappedValue.isEmpty ? Farbe.text3 : Farbe.gruen)
            Text(titel).font(.jarvis(13, .medium)).frame(width: 170, alignment: .leading)
            SecureField("", text: wert).textFieldStyle(.roundedBorder)
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
                Text("\(nr + 1).").font(.jarvis(12, .bold)).foregroundStyle(Farbe.cyan).frame(width: 20)
            }
            Text(stufe.titel).font(.jarvis(13, aktiv ? .semibold : .regular)).foregroundStyle(aktiv ? Farbe.text : Farbe.text2)
            if !bereit { Text("Schlüssel fehlt").font(.jarvis(10, .medium)).foregroundStyle(Farbe.orange) }
            Spacer()
            if aktiv {
                Button { v.stufeVerschieben(stufe.id, um: -1) } label: { Image(systemName: "chevron.up") }.buttonStyle(.plain)
                Button { v.stufeVerschieben(stufe.id, um: 1) } label: { Image(systemName: "chevron.down") }.buttonStyle(.plain)
            }
        }
        .padding(.vertical, 3)
    }
}
