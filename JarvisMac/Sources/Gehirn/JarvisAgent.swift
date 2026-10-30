import Foundation

@MainActor final class TextPuffer { var text = ""; var runde = 0 }

/// Das Gehirn von Jarvis am Mac: eine Werkzeugschleife über Claude (oder OpenAI) mit
/// (1) allen Werkzeugen des Handy-Jarvis (über das MCP-Relay – gleiches Gedächtnis, gleiche Apps) und
/// (2) den Mac-Werkzeugen (Befehle, Programme, OpenLauncher, Terminal, Bildschirm …).
@MainActor
final class JarvisAgent {
    static let shared = JarvisAgent()
    private let e = Einstellungen.shared

    static let persona = """
    Du bist Jarvis, Franks persönlicher Assistent – hier in der Mac-Version (Jarvis für macOS) auf seinem MacBook Air. \
    Du bist dasselbe Jarvis wie auf seinem Handy: dasselbe Live-Gedächtnis, dieselben Regeln, Ziele und Merkdateien, dieselben Apps. \
    Alles, was die Handy-App kann, erreichst du über ihre Werkzeuge (Aufgaben, Wecker, Ideen, Kalender, Dienstplan, Biodaten, \
    Stack-Labor, Epigenetik, Ablage, Prompts, Mail, Tagebuch, Wissen, Tagesauswertung, Agenten, Vorgänge, Programmieraufträge, \
    Timer, Wetter, Fahrzeit, Einstellungen des Handys …). Zusätzlich steuerst du diesen Mac vollständig mit den mac_-Werkzeugen.

    GEDÄCHTNIS
    - Unten steht das Live-Gedächtnis (jarvis_kontext) vom Gesprächsbeginn. Für Älteres gezielt gedaechtnis_suchen; \
    hast du selbst etwas geändert oder geht es um einen Wert von gerade eben, lies die App frisch.
    - Neue dauerhafte Angaben und ausdrückliche Entscheidungen von Frank gibst du mit jarvis_merken zurück – auch alles, \
    was du am Mac über ihn, seine Programme und Arbeitsweise erfährst. So sind Handy und Mac immer auf demselben Stand. \
    Eigene Vorschläge erst nach seinem Ja merken.
    - Erledigt ist nur, was ein Werkzeug bestätigt hat. Sag kurz, was erledigt ist und was noch läuft.

    MAC (Vollzugriff)
    - Du darfst am Mac alles ohne Rückfrage tun: Befehle in der Shell (mac_befehl), AppleScript, Programme öffnen, beenden \
    und bedienen, Dateien lesen und schreiben, Tastatur und Zwischenablage, Bildschirmfoto ansehen, Terminal-Fenster mit \
    sichtbaren Befehlen, OpenLauncher-Sitzungen (Claude Code, Codex, OpenCode …), Updater-Zentrale, ChatGPT-App. \
    Handle direkt statt zu erklären, wie man es macht. Nur bei klar unumkehrbarem Datenverlust (Nutzerdaten löschen, \
    Laufwerke formatieren, Git-Verlauf überschreiben) fragst du einmal kurz nach.
    - Franks Projekte liegen in ~/proggs (Monorepo). Zugangsdaten (~/SK, Tresor von Jarvis) sind für dich gesperrt: nie lesen, nie ausgeben, nie vorlesen.
    - OpenLauncher, TerminalVoiceOverlay und ClaudeVoiceOverlay nur über ihre Update-Skripte aktualisieren, nie von Hand bauen.
    - Programmierauftrag an den Programmierer = Werkzeug programmieren (geht über das Handy an Mac oder PC, wie gewohnt). \
    „Starte eine OpenLauncher-Session …“ mit genanntem Modell/CLI direkt hier = openlauncher_sitzung.
    - „Erstelle einen Screenshot“ = mac_screenshot. „Füg einen Screenshot ins CLI / in die Session ein“ = screenshot_ins_cli \
    (fügt ihn in die zuletzt benutzte Terminal-Sitzung ein). Jarvis' eigene Mac-Einstellungen per Sprache = mac_einstellungen.

    ANTWORTEN
    - Deutsch, kurz und klar. Nie Quellen, Quellennamen oder Webadressen nennen, außer Frank fragt ausdrücklich danach.
    - Aktuelle Fragen aus dem Internet beantwortest du sofort selbst mit der Websuche; Agenten nur, wenn Frank ausdrücklich \
    eine Recherche oder einen Agenten verlangt. Daten aus Franks Apps kommen aus ihren Werkzeugen, nie aus der Websuche.
    - Ist eine Nachricht mit [gesprochen] markiert, antwortest du fürs Vorlesen: natürlich, ein bis drei Sätze, \
    ohne Markdown, ohne Aufzählungszeichen, Zahlen und Uhrzeiten ausgeschrieben lesbar. Sonst darfst du Markdown nutzen.
    - „ ; “ in Franks Text ist nur ein Trenner aus dem Diktat; bei ausdrücklichen Programmierübergaben trennt er Aufgaben derselben Sitzung.
    """

    private func systemTeile(g: Gespraech) async -> [String] {
        let anweisungen = await McpKlient.shared.anweisungen
        let fest = Self.persona + (anweisungen.isEmpty ? "" : "\n\nVORGABEN DES HANDY-JARVIS (gelten auch hier, soweit sie nicht den Mac betreffen):\n" + anweisungen)
        let kontext = g.kontext.isEmpty
            ? "LIVE-GEDÄCHTNIS: Das Handy-Jarvis ist gerade nicht erreichbar. Sag Frank das kurz, wenn er nach Daten aus seinen Apps fragt."
            : "LIVE-GEDÄCHTNIS (jarvis_kontext, Stand Gesprächsbeginn):\n" + g.kontext
        return [fest, kontext]
    }

    /// Gewähltes Modell – fehlt dafür der Zugang, weicht Jarvis auf den anderen Anbieter aus (ChatGPT-Konto oder Claude).
    private func anbieter() async -> (ModellAnbieter, String) {
        let codex = await CodexAnmeldung.shared.angemeldet
        let claudeGeht = !e.zugang.anthropic.isEmpty
        let openaiGeht = codex || !e.zugang.openai.isEmpty
        switch e.modellWahl.anbieter {
        case .anthropic where !claudeGeht && openaiGeht:
            return (OpenAIKlient(schluessel: e.zugang.openai), "gpt-6-astra")
        case .openai where !openaiGeht && claudeGeht:
            return (AnthropicKlient(schluessel: e.zugang.anthropic), "claude-opus-5-5")
        case .anthropic: return (AnthropicKlient(schluessel: e.zugang.anthropic), e.modell)
        case .openai: return (OpenAIKlient(schluessel: e.zugang.openai), e.modell)
        }
    }

    static func zeitstempel() -> String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.dateFormat = "EEEE, dd.MM.yyyy, HH:mm"
        return f.string(from: Date()) + " Uhr"
    }

    /// Stellt Jarvis eine Frage im Gespräch. Gibt den fertigen Antworttext zurück.
    @discardableResult
    func frage(_ text: String, in g: Gespraech, gesprochen: Bool = false, bild: Data? = nil,
               onText: ((String) -> Void)? = nil) async -> String {
        // Ein Gespräch, eine Werkzeugschleife: sonst landet eine neue Frage zwischen tool_use und tool_result.
        while g.arbeitet { try? await Task.sleep(for: .milliseconds(200)) }
        g.arbeitet = true
        defer { g.arbeitet = false; g.speichern() }
        GespraechsSpeicher.shared.nachOben(g)

        g.nachrichten.append(Nachricht(rolle: .frank, text: text, gesprochen: gesprochen, bild: bild))
        var inhalt: [JSONObjekt] = []
        if let bild { inhalt.append(Self.bildBlock(bild)) }
        inhalt.append(["type": "text", "text": "[\(Self.zeitstempel())\(gesprochen ? ", gesprochen" : "")] \(text)"])
        g.verlauf.append(["role": "user", "content": inhalt])

        Self.verdichte(g)
        let jarvisIndex = g.nachrichten.count
        g.nachrichten.append(Nachricht(rolle: .jarvis, text: "", laeuft: true))

        async let mcpListe = McpKlient.shared.werkzeuge()
        if g.kontext.isEmpty {
            g.kontext = await McpKlient.shared.text("jarvis_kontext", [:]) ?? ""
        }
        let handyWerkzeuge = await mcpListe
        let lokale = MacWerkzeuge.alleLokal
        let lokaleNamen = Set(lokale.map(\.definition.name))
        let alleWerkzeuge = lokale.map(\.definition) + handyWerkzeuge.filter { !lokaleNamen.contains($0.name) && !McpKlient.nurFuerOberflaeche.contains($0.name) }
        let system = await systemTeile(g: g)
        let (a, modell) = await anbieter()
        let effort = e.effort
        let limit = e.schrittLimit

        var textTeile: [String] = []
        let puffer = TextPuffer()
        func zeige() {
            guard jarvisIndex < g.nachrichten.count else { return }
            g.nachrichten[jarvisIndex].text = (textTeile + (puffer.text.isEmpty ? [] : [puffer.text])).joined(separator: "\n\n")
        }

        var schritt = 0
        while true {
            schritt += 1
            if limit > 0 && schritt > limit {
                textTeile.append("(Schrittlimit von \(limit) erreicht – sag „weiter“, dann mache ich weiter.)")
                break
            }
            let antwort: ModellAntwort
            do {
                puffer.runde += 1
                puffer.text = ""
                let runde = puffer.runde
                antwort = try await a.antworte(system: system, werkzeuge: alleWerkzeuge, websuche: true, verlauf: g.verlauf,
                                               modell: modell, effort: effort) { stueck in
                    Task { @MainActor in
                        guard puffer.runde == runde else { return }
                        puffer.text += stueck; zeige(); onText?(stueck)
                    }
                }
            } catch {
                log("fehler", "Gehirn: \(error.localizedDescription)")
                textTeile.append("⚠️ \(error.localizedDescription)")
                // Unbeantwortete Nutzernachricht entfernen wäre falsch; Verlauf bleibt gültig, wenn die letzte Nachricht vom Nutzer ist.
                break
            }
            await Task.yield()
            let texte = antwort.inhalt.compactMap { ($0["type"] as? String) == "text" ? $0["text"] as? String : nil }.joined()
            puffer.runde += 1
            puffer.text = ""
            if !texte.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { textTeile.append(texte) }
            zeige()
            log("gehirn", "\(modell) Schritt \(schritt): \(antwort.stopGrund), ein \(antwort.eingabeTokens)+cache \(antwort.cacheTokens), aus \(antwort.ausgabeTokens)")
            g.verlauf.append(["role": "assistant", "content": antwort.inhalt])

            if antwort.stopGrund == "pause_turn" { continue }
            if antwort.stopGrund == "refusal" { textTeile.append("(Die Anfrage wurde vom Modell abgelehnt.)"); break }
            let aufrufe = antwort.inhalt.filter { ($0["type"] as? String) == "tool_use" }
            if aufrufe.isEmpty { break }

            // Werkzeuge parallel ausführen, alle Ergebnisse in EINER Nutzernachricht zurück.
            var indizes: [Int] = []
            for auf in aufrufe {
                let name = auf["name"] as? String ?? "?"
                let args = auf["input"] as? JSONObjekt ?? [:]
                g.nachrichten[jarvisIndex].schritte.append(Schritt(werkzeug: name, kurz: Self.kurz(name, args), amHandy: !lokaleNamen.contains(name)))
                indizes.append(g.nachrichten[jarvisIndex].schritte.count - 1)
            }
            let ergebnisse: [WerkzeugErgebnis] = await withTaskGroup(of: (Int, WerkzeugErgebnis).self) { gruppe in
                for (i, auf) in aufrufe.enumerated() {
                    let name = auf["name"] as? String ?? "?"
                    let args = auf["input"] as? JSONObjekt ?? [:]
                    gruppe.addTask { (i, await Self.fuehreAus(name, args, lokal: lokaleNamen.contains(name))) }
                }
                var r = [WerkzeugErgebnis](repeating: WerkzeugErgebnis(text: ""), count: aufrufe.count)
                for await (i, e) in gruppe { r[i] = e }
                return r
            }
            var resultate: [JSONObjekt] = []
            for (i, auf) in aufrufe.enumerated() {
                let erg = ergebnisse[i]
                g.nachrichten[jarvisIndex].schritte[indizes[i]].ok = !erg.fehler
                var t = erg.text
                if t.count > 60_000 { t = String(t.prefix(60_000)) + "\n… [gekürzt]" }
                var bl: [JSONObjekt] = [["type": "text", "text": t]]
                if let png = erg.bildPNG {
                    bl.append(Self.bildBlock(png))
                }
                var r: JSONObjekt = ["type": "tool_result", "tool_use_id": auf["id"] as? String ?? "", "content": bl]
                if erg.fehler { r["is_error"] = true }
                resultate.append(r)
            }
            g.verlauf.append(["role": "user", "content": resultate])
            g.speichern()
        }

        let endtext = textTeile.joined(separator: "\n\n").trimmingCharacters(in: .whitespacesAndNewlines)
        if jarvisIndex < g.nachrichten.count {
            g.nachrichten[jarvisIndex].text = endtext.isEmpty ? "(keine Antwort)" : endtext
            g.nachrichten[jarvisIndex].laeuft = false
            g.nachrichten[jarvisIndex].gesprochen = gesprochen
        }
        if g.titel == "Neues Gespräch" { Task { await titelVergeben(g) } }
        Lernen.shared.merkeAktivitaet(g)
        return endtext
    }

    nonisolated static func fuehreAus(_ name: String, _ args: JSONObjekt, lokal: Bool) async -> WerkzeugErgebnis {
        var e: WerkzeugErgebnis
        if lokal, let w = await MainActor.run(body: { MacWerkzeuge.alleLokal.first { $0.definition.name == name } }) {
            let start = Date()
            e = await w.ausfuehren(args)
            log("werkzeug", "Mac-Werkzeug \(name): \(e.fehler ? "Fehler" : "ok")", wo: "Mac", werkzeug: name, eingabe: JSON.text(args),
                ergebnis: e.fehler ? nil : e.text, fehler: e.fehler ? e.text : nil, dauerMs: Int(Date().timeIntervalSince(start) * 1000))
        } else {
            e = await McpKlient.shared.rufe(name, args)
        }
        // Kein Schlüssel erreicht je das Modell, selbst wenn ein Befehl ihn ausgibt.
        e.text = Geheimnisse.schwaerzen(e.text)
        return e
    }

    static func bildBlock(_ d: Data) -> JSONObjekt {
        let typ = d.starts(with: [0xFF, 0xD8]) ? "image/jpeg" : (d.starts(with: [0x47, 0x49, 0x46]) ? "image/gif" : "image/png")
        return ["type": "image", "source": ["type": "base64", "media_type": typ, "data": d.base64EncodedString()]]
    }

    /// Hält lange Gespräche sendefähig: Wird der Verlauf zu groß (> 6 MB oder grob > 500 000 Tokens), ersetzt Jarvis Bilder
    /// und sehr lange Werkzeugergebnisse in älteren Runden durch kurze Platzhalter. Dabei fallen auch alle Denkblöcke vor der
    /// letzten Frage weg – ein geänderter Verlauf darf keine alten Denkblöcke mehr tragen (sonst lehnt die API ab).
    static func verdichte(_ g: Gespraech) {
        let groesse = JSON.daten(g.verlauf).count
        guard groesse > 6_000_000 else { return }
        let behalte = 6   // jüngste Einträge unverändert
        let grenze = max(0, g.verlauf.count - behalte)
        func kuerze(_ bloecke: [JSONObjekt]) -> [JSONObjekt] {
            bloecke.compactMap { b in
                var b = b
                switch b["type"] as? String {
                case "image": return ["type": "text", "text": "[Bild aus einer früheren Runde entfernt]"]
                case "thinking", "redacted_thinking": return nil
                case "tool_result":
                    if let inner = b["content"] as? [JSONObjekt] { b["content"] = kuerze(inner) }
                    return b
                case "text":
                    if let t = b["text"] as? String, t.count > 8000 { b["text"] = String(t.prefix(8000)) + "\n… [gekürzt]" }
                    return b
                default: return b
                }
            }
        }
        for i in g.verlauf.indices {
            guard var inhalt = g.verlauf[i]["content"] as? [JSONObjekt] else { continue }
            if i < grenze { inhalt = kuerze(inhalt) }
            else { inhalt = inhalt.filter { !["thinking", "redacted_thinking"].contains($0["type"] as? String ?? "") } }
            if inhalt.isEmpty { inhalt = [["type": "text", "text": "…"]] }
            g.verlauf[i]["content"] = inhalt
        }
        log("gehirn", "Verlauf verdichtet: \(groesse / 1000) kB → \(JSON.daten(g.verlauf).count / 1000) kB")
    }

    static func kurz(_ name: String, _ args: JSONObjekt) -> String {
        for k in ["befehl", "skript", "ziel", "text", "titel", "frage", "auftrag", "suche", "query", "name", "pfad", "aktion"] {
            if let s = args[k] as? String, !s.isEmpty { return String(s.replacingOccurrences(of: "\n", with: " ").prefix(90)) }
        }
        return args.isEmpty ? "" : String(JSON.text(args).prefix(90))
    }

    private func titelVergeben(_ g: Gespraech) async {
        let erste = g.nachrichten.first { $0.rolle == .frank }?.text ?? ""
        guard !erste.isEmpty else { return }
        if let t = await Kurzauftrag.frage("Gib diesem Gespräch einen Titel aus höchstens fünf Wörtern, ohne Anführungszeichen und ohne Punkt. Erste Nachricht: \(erste.prefix(600))") {
            g.titel = String(t.prefix(60)); g.speichern()
        } else {
            g.titel = String(erste.prefix(40)); g.speichern()
        }
    }
}
