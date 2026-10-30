import Foundation

/// Das Repo liegt am Mac: Jarvis liest es direkt und fragt für größere Prüfungen das CLI im Hintergrund, ohne die Grenzen der GitHub-Anbindung des Handys.
enum RepoWerkzeuge {
    private static let ordner = Pfade.heim.appendingPathComponent("proggs").path
    private static let zeilen = 400

    private static func sicher(_ pfad: String) -> Bool { !pfad.hasPrefix("/") && !pfad.hasPrefix("~") && !pfad.components(separatedBy: "/").contains("..") }

    private static func text(_ r: Shell.Ergebnis, leer: String) -> WerkzeugErgebnis {
        if r.code != 0 && r.ausgabe.isEmpty { return WerkzeugErgebnis(text: r.fehler.isEmpty ? "Befehl fehlgeschlagen (\(r.code))." : r.fehler, fehler: true) }
        return WerkzeugErgebnis(text: r.ausgabe.isEmpty ? leer : r.ausgabe)
    }

    static let alle: [LokalesWerkzeug] = [
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "repo_lesen",
            beschreibung: "Jarvis am Mac: liest Franks Repo direkt aus ~/proggs, ohne Beschränkung durch GitHub. aktion=projekte: oberste Ebene (jeder Ordner ist meist ein Projekt). dateien: alle Dateien eines Ordners (pfad). datei: Inhalt einer Datei (pfad; lange Dateien mit von_zeile weiterlesen). suche: Code-Suche im ganzen Repo oder unter pfad (anfrage). verlauf: letzte Commits, auf Wunsch nur für pfad, mit dem Stand gegenüber GitHub. prs: offene Pull Requests. Mit zweig liest du einen anderen Stand. Nur lesen.",
            schema: ["type": "object", "properties": [
                "aktion": ["type": "string", "enum": ["projekte", "dateien", "datei", "suche", "verlauf", "prs"]],
                "pfad": ["type": "string", "description": "Pfad im Repo, zum Beispiel Jarvis oder Jarvis/app/build.gradle.kts"],
                "anfrage": ["type": "string", "description": "Für suche: Suchbegriff"],
                "von_zeile": ["type": "integer", "description": "Für datei: erste Zeile, Vorgabe 1; höchstens \(zeilen) Zeilen je Aufruf"],
                "zweig": ["type": "string", "description": "Anderer Zweig oder Commit, Vorgabe: aktueller Stand am Mac"],
            ] as JSONObjekt, "required": ["aktion"]])) { a in
            // Ohne Vollzugriff bleibt der Weg über das Handy.
            guard await MainActor.run(body: { Einstellungen.shared.vollzugriff }) else { return await McpKlient.shared.rufe("repo_lesen", a) }
            let pfad = a["pfad"] as? String ?? "", anfrage = a["anfrage"] as? String ?? "", zweig = a["zweig"] as? String ?? ""
            guard sicher(pfad) else { return WerkzeugErgebnis(text: "pfad muss relativ zum Repo sein.", fehler: true) }
            if let g = Tresor.sperre(pfad, anfrage) { return g }
            let p = pfad.isEmpty ? "." : Shell.zitiert(pfad)
            let z = zweig.isEmpty ? "" : Shell.zitiert(zweig) + " "
            switch a["aktion"] as? String ?? "" {
            case "projekte":
                return text(await Shell.zsh("ls -1p", ordner: ordner, sekunden: 20), leer: "Leer.")
            case "dateien":
                return text(await Shell.zsh("git ls-files -- \(p) | head -800", ordner: ordner, sekunden: 30), leer: "Keine Dateien unter \(pfad).")
            case "datei":
                guard !pfad.isEmpty else { return WerkzeugErgebnis(text: "pfad fehlt.", fehler: true) }
                let von = max(1, a["von_zeile"] as? Int ?? 1), bis = von + zeilen - 1
                let quelle = zweig.isEmpty ? "cat -- \(p)" : "git show \(Shell.zitiert(zweig + ":" + pfad))"
                return text(await Shell.zsh("\(quelle) | sed -n '\(von),\(bis)p'", ordner: ordner, sekunden: 30), leer: "Datei leer oder Zeile \(von) gibt es nicht.")
            case "suche":
                guard !anfrage.isEmpty else { return WerkzeugErgebnis(text: "anfrage fehlt.", fehler: true) }
                return text(await Shell.zsh("git grep -n -I -e \(Shell.zitiert(anfrage)) \(z)-- \(p) | cut -c1-260 | head -200", ordner: ordner, sekunden: 60), leer: "Nichts gefunden.")
            case "verlauf":
                return text(await Shell.zsh("git status -sb | head -1; git log -n 30 --date=short --format='%h %ad %s' \(z)-- \(p)", ordner: ordner, sekunden: 30), leer: "Kein Verlauf.")
            case "prs":
                return text(await Shell.zsh("gh pr list --limit 30", ordner: ordner, sekunden: 40), leer: "Keine offenen Pull Requests.")
            default:
                return WerkzeugErgebnis(text: "Unbekannte aktion.", fehler: true)
            }
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "cli_frage",
            beschreibung: "Jarvis am Mac: fragt im Hintergrund das Claude-Code-CLI in ~/proggs, ohne Fenster und nur lesend. Für Fragen zum Repo und Prüfungen, die mehrere Dateien brauchen: „Wie funktioniert …?“, „Ist … schon eingebaut?“, „Wo steckt der Fehler?“. Dauert bis zu zehn Minuten und kostet Abo-Kontingent. Einzelne Dateien liest du schneller mit repo_lesen. Zum Ändern von Code eine Sitzung mit openlauncher_sitzung starten oder programmieren.",
            schema: ["type": "object", "properties": [
                "frage": ["type": "string", "description": "Die vollständige, für sich allein verständliche Frage"],
                "modell": ["type": "string", "description": "sonnet (Vorgabe), opus oder haiku"],
            ] as JSONObjekt, "required": ["frage"]])) { a in
            guard await MainActor.run(body: { Einstellungen.shared.vollzugriff }) else {
                return WerkzeugErgebnis(text: "Vollzugriff ist in den Einstellungen ausgeschaltet. Frank muss ihn einschalten (Einstellungen → Mac).", fehler: true)
            }
            let frage = a["frage"] as? String ?? ""
            guard !frage.isEmpty else { return WerkzeugErgebnis(text: "frage fehlt.", fehler: true) }
            if let g = Tresor.sperre(frage) { return g }
            let modell = ["sonnet", "opus", "haiku"].contains(a["modell"] as? String ?? "") ? a["modell"] as! String : "sonnet"
            let voll = frage + "\n\nNur lesen und antworten, nichts ändern. Zugangsdaten unter ~/SK nie lesen. Antworte auf Deutsch und knapp."
            // Dasselbe Anmelde-Profil wie die Sitzungen aus dem OpenLauncher.
            let profil = Pfade.heim.appendingPathComponent("proggs/OpenLauncher/Profiles/ClaudeCodeMac/minimal").path
            let befehl = "CLAUDE_CONFIG_DIR=\(Shell.zitiert(profil)) claude -p \(Shell.zitiert(voll)) --output-format text --model \(modell)"
                + " --allowedTools 'Read,Grep,Glob,Bash(git log:*),Bash(git status:*),Bash(git diff:*),Bash(git show:*),Bash(ls:*)'"
            let r = await Shell.zsh(befehl, ordner: ordner, sekunden: 600)
            if r.zeitAus { return WerkzeugErgebnis(text: "Das CLI hat in zehn Minuten nicht geantwortet. Für so große Fragen openlauncher_sitzung nehmen.", fehler: true) }
            return text(r, leer: "Das CLI hat nichts geantwortet.")
        },
    ]
}
