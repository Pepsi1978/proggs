import Foundation

/// Die Werkzeuge zu den eigenen Helfern dieses Macs. Die Handy-Helfer und der Gesamtkatalog kommen über helfer_* des Handys.
enum HelferWerkzeuge {
    private static let schloss = NSLock()
    private static let tagesGrenze = 5

    /// Einträge „Schlüssel|Kennung“; ohne Kennung (neuer Helfer) passt der Schlüssel nur zu einer noch nicht vergebenen.
    private static func geladen() -> [String] { Tresor.lade([String].self, name: "helfer-bau", klartext: Pfade.unterstuetzung.appendingPathComponent(".kein-klartext-helfer-bau")) ?? [] }

    static func bauSchluesselPasst(_ s: String, kennung: String, vorhanden: Bool) -> Bool {
        schloss.lock(); defer { schloss.unlock() }
        guard !s.isEmpty, !s.contains("|"), let eintrag = geladen().first(where: { $0 == s || $0.hasPrefix(s + "|") }) else { return false }
        let gebunden = eintrag.split(separator: "|", omittingEmptySubsequences: false).dropFirst().first.map(String.init) ?? ""
        return gebunden.isEmpty ? !vorhanden : gebunden == kennung
    }

    static func bauSchluesselVerbrauchen(_ s: String) {
        schloss.lock(); defer { schloss.unlock() }
        Tresor.sichere(geladen().filter { $0 != s && !$0.hasPrefix(s + "|") }, name: "helfer-bau")
    }

    private static func neuerBauSchluessel(kennung: String) -> String {
        let s = UUID().uuidString.replacingOccurrences(of: "-", with: "")
        schloss.lock(); defer { schloss.unlock() }
        Tresor.sichere(Array((geladen() + [s + "|" + kennung]).suffix(20)), name: "helfer-bau")
        return s
    }

    /// Recherche über das CLI, parallel über Unteragenten. Kein Zugriff auf ~/SK, keine Änderungen, nur mit Vollzugriff.
    static func cliRecherche(frage: String, anzahl: Int, modell: String) async -> String {
        func nein(_ t: String) -> String { JSON.text(["ok": false, "fehler": t] as JSONObjekt) }
        guard await MainActor.run(body: { Einstellungen.shared.vollzugriff }) else { return nein("Der Vollzugriff ist am Mac ausgeschaltet.") }
        guard !frage.isEmpty else { return nein("Die Frage fehlt.") }
        if Tresor.sperre(frage) != nil { return nein("Die Frage berührt gesperrte Zugangsdaten.") }
        let zahl = min(max(anzahl, 1), 7)
        let cli = ["sonnet", "opus", "fable"].first { $0 == modell } ?? "sonnet"
        let voll = "Recherchiere gründlich: \(frage)\n\nTeile die Arbeit in getrennte Fragen und nutze dafür bei Bedarf bis zu \(zahl) parallele Unteragenten mit dem Modell sonnet; nicht mehr als nötig und keine doppelten Fragen. Antworte auf Deutsch mit dem, was für die Umsetzung zählt, und mit den Quellen."
        let profil = Pfade.heim.appendingPathComponent("proggs/OpenLauncher/Profiles/ClaudeCodeMac/minimal").path
        // Nur Web, kein Lesen von Dateien, außerhalb des Repos: Eine manipulierte Webseite soll hier nichts finden, das sie ausleiten kann.
        let befehl = "CLAUDE_CONFIG_DIR=\(Shell.zitiert(profil)) claude -p \(Shell.zitiert(voll)) --output-format text --model \(cli)"
            + " --setting-sources project --strict-mcp-config --permission-mode dontAsk --tools 'WebSearch,WebFetch,Task'"
            + " --allowedTools 'WebSearch,WebFetch,Task'"
        let r = await Shell.zsh(befehl, ordner: FileManager.default.temporaryDirectory.path, sekunden: 1200)
        if r.zeitAus { return nein("Die Recherche wurde nach 20 Minuten abgebrochen.") }
        if r.code != 0 { return nein(String((r.fehler.isEmpty ? r.ausgabe : r.fehler).suffix(600))) }
        return JSON.text(["ok": true, "text": Geheimnisse.schwaerzen(r.ausgabe)] as JSONObjekt)
    }

    private static func zeile(_ e: HelferEintrag) -> String {
        let stand = e.aktiv ? e.zustand : "deaktiviert"
        return "- \(e.kennung) (\(e.name), \(e.text("version")), \(stand)): \(e.text("zweck"))"
    }

    private static func detail(_ e: HelferEintrag) -> String {
        var t = "\(e.name) [\(e.kennung)], Fassung \(e.text("version")), Mac, \(e.aktiv ? e.zustand : "deaktiviert")\nZweck: \(e.text("zweck"))"
        t += "\nFähigkeiten: \(e.liste("faehigkeiten").joined(separator: "; "))"
        t += "\nEingabe (JSON-Schema): \(JSON.text(e.manifest["eingabe"] as? JSONObjekt ?? [:]))\nErgebnis: \(e.text("ausgabe"))"
        t += "\nVoraussetzungen: \(e.liste("voraussetzungen").joined(separator: "; "))\nGrenzen: \(e.liste("grenzen").joined(separator: "; "))"
        t += "\nRechte: \(JSON.text(e.rechte))\nLetzte Prüfung: \(e.pruefungErgebnis)"
        return t
    }

    private static func tagesZaehlerErhoehen() -> Bool {
        let d = UserDefaults.standard
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        let heute = f.string(from: Date())
        let n = d.string(forKey: "helferBauTag") == heute ? d.integer(forKey: "helferBauZahl") : 0
        guard n < tagesGrenze else { return false }
        d.set(heute, forKey: "helferBauTag"); d.set(n + 1, forKey: "helferBauZahl")
        return true
    }

    private static func auftrag(aufgabe: String, faehigkeit: String, kennung: String, schluessel: String, modell: String, effort: String) -> String {
        """
        Bau für Jarvis Mac OS einen Helfer (oder verbessere den vorhandenen „\(kennung)“) nach JarvisHelfer/README.md. Lies die Datei zuerst.
        Ziel: \(faehigkeit)
        Franks ursprüngliche Aufgabe, für die er gebraucht wird: \(aufgabe)
        Ort: mac. Quellstand nach JarvisHelfer/<kennung>/, committen und pushen.
        Vorgehen: kleinste tragfähige Lösung, praktische Tests für Normal- und Fehlerfälle im Steckbrief, danach gründliches Review mit /code-review max --max-findings 100, kritische Befunde beheben.
        Recherche nur bei echten Lücken; dabei bevorzugt \(modell) mit Effort \(effort), keine kleinen Modelle, höchstens sieben parallele Unteragenten.
        Abschluss: Paket nach ~/Library/Application Support/JarvisMac/Helfer/eingang/<kennung>/ kopieren (helfer.json, Programmdatei) und zuletzt registrierung.json dazu: {"bau_schluessel":"\(schluessel)","review":{"befunde":n,"kritisch_offen":0,"zusammenfassung":"..."}}. Jarvis prüft es dann selbst.
        Nichts anlegen, das Kosten, neue Konten, Veröffentlichungen oder zusätzliche Rechte ohne Franks Ja bedeutet. REVIEW: ja, sehr ausführlich.
        """
    }

    static let alle: [LokalesWerkzeug] = [
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "mac_helfer_suchen",
            beschreibung: "Jarvis am Mac: durchsucht den Katalog der eigenen Mac-Helfer nach einer Fähigkeit („Word-Datei“, „YouTube“). Ohne Suchwort kommt die Übersicht. Nutze es, bevor du sagst, dass etwas fehlt.",
            schema: ["type": "object", "properties": ["suche": ["type": "string", "description": "Fähigkeit oder Stichworte"]] as JSONObjekt])) { a in
            let wort = a["suche"] as? String ?? ""
            let treffer = await MainActor.run { HelferKatalog.shared.suche(wort) }
            return WerkzeugErgebnis(text: treffer.isEmpty ? "Kein passender Mac-Helfer." : treffer.map(zeile).joined(separator: "\n"))
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "mac_helfer_info",
            beschreibung: "Jarvis am Mac: alle Angaben zu einem Mac-Helfer: Zweck, Fähigkeiten, Eingaben, Ergebnisse, Voraussetzungen, Rechte, Grenzen, Zustand, letzte Prüfung.",
            schema: ["type": "object", "properties": ["kennung": ["type": "string"]] as JSONObjekt, "required": ["kennung"]])) { a in
            guard let e = await MainActor.run(body: { HelferKatalog.shared.finde(a["kennung"] as? String ?? "") }) else { return WerkzeugErgebnis(text: "Keinen solchen Helfer gefunden.", fehler: true) }
            return WerkzeugErgebnis(text: detail(e))
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "mac_helfer_ausfuehren",
            beschreibung: "Jarvis am Mac: führt einen einsatzbereiten Mac-Helfer aus und liefert sein Ergebnis samt Dateipfaden. Prüfe das Ergebnis, bevor du Erfolg meldest.",
            schema: ["type": "object", "properties": [
                "kennung": ["type": "string"],
                "eingabe": ["type": "object", "description": "Eingabe nach dem Schema des Helfers"] as JSONObjekt,
            ] as JSONObjekt, "required": ["kennung"]])) { a in
            let k = a["kennung"] as? String ?? ""
            let eingabe = JSON.text(a["eingabe"] as? JSONObjekt ?? [:])
            let r = await MainActor.run { Task { await HelferKatalog.shared.ausfuehren(k, eingabe: JSON.objekt(eingabe) ?? [:]) } }.value
            guard r.ok else { return WerkzeugErgebnis(text: r.fehler, fehler: true) }
            var t = r.text
            if r.datenText != "{}" { t += "\nDaten: \(r.datenText)" }
            if !r.dateien.isEmpty { t += "\nDateien: " + r.dateien.map { "\($0.name) → \($0.pfad)" }.joined(separator: ", ") }
            return WerkzeugErgebnis(text: t + "\nDauer: \(r.dauerMs) ms")
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "mac_helfer_verwalten",
            beschreibung: "Jarvis am Mac: aktiviert oder deaktiviert einen Mac-Helfer, legt ihn in den Papierkorb, holt ihn zurück oder setzt ihn auf die letzte geprüfte Fassung. Freigaben und endgültiges Löschen macht nur Frank in den Einstellungen.",
            schema: ["type": "object", "properties": [
                "kennung": ["type": "string"],
                "aktion": ["type": "string", "enum": ["aktivieren", "deaktivieren", "papierkorb", "wiederherstellen", "rueckrollen"]] as JSONObjekt,
            ] as JSONObjekt, "required": ["kennung", "aktion"]])) { a in
            let k = a["kennung"] as? String ?? "", aktion = a["aktion"] as? String ?? ""
            let r = await MainActor.run { HelferKatalog.shared.verwalten(k, aktion: aktion) }
            return WerkzeugErgebnis(text: r.text, fehler: !r.ok)
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "mac_helfer_bauen",
            beschreibung: "Jarvis am Mac: lässt für eine konkrete Aufgabe einen Mac-Helfer bauen oder verbessern, wenn Suche und vorhandene Funktionen nicht reichen. Sag Frank kurz, dass du dafür einen Helfer bauen lässt, und mach danach seine eigentliche Aufgabe fertig. Nicht ohne konkreten Anlass.",
            schema: ["type": "object", "properties": [
                "aufgabe": ["type": "string", "description": "Franks ursprüngliche Aufgabe"],
                "faehigkeit": ["type": "string", "description": "Was der Helfer können muss, mit überprüfbaren Erfolgskriterien"],
                "kennung": ["type": "string", "description": "Nur beim Verbessern: Kennung des vorhandenen Helfers"],
            ] as JSONObjekt, "required": ["aufgabe", "faehigkeit"]])) { a in
            let (automatik, modell, effort) = await MainActor.run { (Einstellungen.shared.helferAutomatik, Einstellungen.shared.rechercheModell, Einstellungen.shared.rechercheEffort) }
            guard automatik else { return WerkzeugErgebnis(text: "Der Helferbau ist pausiert (Einstellungen → Eigene Tools). Sag Frank das.", fehler: true) }
            let aufgabe = a["aufgabe"] as? String ?? "", faehigkeit = a["faehigkeit"] as? String ?? "", kennung = a["kennung"] as? String ?? ""
            guard faehigkeit.count >= 10, !aufgabe.isEmpty else { return WerkzeugErgebnis(text: "Aufgabe und Fähigkeit vollständig angeben.", fehler: true) }
            if await McpKlient.shared.werkzeuge().contains(where: { $0.name == "helfer_bauen" }) {
                var rumpf: JSONObjekt = ["aufgabe": aufgabe, "faehigkeit": faehigkeit, "ort": "mac"]
                if !kennung.isEmpty { rumpf["kennung"] = kennung }
                let e = await McpKlient.shared.rufe("helfer_bauen", rumpf)
                // Eine Ablehnung des Handys bleibt seine Antwort. Nur wenn es nachweislich weg ist, geht es am Mac weiter;
                // die frische Abfrage verhindert einen zweiten Bau, falls nur die Antwort verloren ging.
                let weg = e.fehler && (e.text.hasPrefix("Handy-Jarvis nicht erreichbar") || e.text.hasPrefix("Jarvis ist gerade nicht erreichbar"))
                if !weg { return e }
                if await McpKlient.shared.werkzeuge(frisch: true).contains(where: { $0.name == "helfer_bauen" }) { return e }
            }
            guard tagesZaehlerErhoehen() else { return WerkzeugErgebnis(text: "Heute wurden schon \(tagesGrenze) Helfer direkt am Mac beauftragt; das ist die Grenze.", fehler: true) }
            guard let start = MacWerkzeuge.alle.first(where: { $0.definition.name == "openlauncher_sitzung" }) else { return WerkzeugErgebnis(text: "OpenLauncher-Weg fehlt.", fehler: true) }
            let text = auftrag(aufgabe: aufgabe, faehigkeit: faehigkeit, kennung: kennung, schluessel: neuerBauSchluessel(kennung: kennung), modell: modell, effort: effort)
            let e = await start.ausfuehren(["auftrag": text, "name": "Jarvis: Helfer", "cli": "claude", "modell": "claude-opus-5-5[1m]", "profil": "minimal", "modus": "schnell", "effort": "high"])
            if e.fehler { return e }
            return WerkzeugErgebnis(text: "Das Handy ist nicht erreichbar, deshalb läuft der Helferbau direkt am Mac mit Claude Opus 5.5. Fertig ist er erst, wenn er nach bestandener Prüfung im Katalog als bereit steht (mac_helfer_suchen).")
        },
    ]
}
