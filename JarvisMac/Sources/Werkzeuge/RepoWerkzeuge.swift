import Foundation

/// Das Repo liegt am Mac: Jarvis liest es direkt und fragt für größere Prüfungen das CLI im Hintergrund, ohne die Grenzen der GitHub-Anbindung des Handys.
enum RepoWerkzeuge {
    private static let ordner = Pfade.heim.appendingPathComponent("proggs").path
    private static let zeilen = 400

    private static func fehler(_ t: String) -> WerkzeugErgebnis { WerkzeugErgebnis(text: t, fehler: true) }

    private static func text(_ r: Shell.Ergebnis, leer: String) -> WerkzeugErgebnis {
        if r.code != 0 {
            let t = [r.ausgabe, r.fehler].filter { !$0.isEmpty }.joined(separator: "\n")
            return fehler(t.isEmpty ? "Befehl fehlgeschlagen (\(r.code))." : t)
        }
        return WerkzeugErgebnis(text: r.ausgabe.isEmpty ? leer : r.ausgabe)
    }

    /// Kürzen geschieht hier statt mit „| head“: Eine Pipe würde Fehler von git verschlucken, und das Modell soll sehen, dass etwas fehlt.
    private static func begrenzt(_ r: Shell.Ergebnis, leer: String, hoechstens: Int, breite: Int = 300) -> WerkzeugErgebnis {
        var e = text(r, leer: leer)
        guard !e.fehler, !r.ausgabe.isEmpty else { return e }
        let liste = r.ausgabe.split(separator: "\n", omittingEmptySubsequences: false)
        var t = liste.prefix(hoechstens).map { String($0.prefix(breite)) }.joined(separator: "\n")
        if liste.count > hoechstens { t += "\n… gekürzt: \(liste.count - hoechstens) weitere Zeilen, enger eingrenzen." }
        e.text = t
        return e
    }

    private static func ausschnitt(_ inhalt: String, von: Int) -> WerkzeugErgebnis {
        let liste = inhalt.components(separatedBy: "\n")
        guard von <= liste.count else { return fehler("Die Datei hat nur \(liste.count) Zeilen.") }
        let bis = min(liste.count, von + zeilen - 1)
        var t = liste[(von - 1)..<bis].joined(separator: "\n")
        if bis < liste.count { t += "\n… Zeile \(von) bis \(bis) von \(liste.count). Weiter mit von_zeile \(bis + 1)." }
        return WerkzeugErgebnis(text: t)
    }

    /// Nur versionierte Dateien, die wirklich im Repo liegen: keine Symlinks nach außen, keine lokalen .env-Dateien.
    private static func dateiImRepo(_ pfad: String) async -> String? {
        let wurzel = URL(fileURLWithPath: ordner).resolvingSymlinksInPath().path
        let echt = URL(fileURLWithPath: ordner + "/" + pfad).resolvingSymlinksInPath().path
        guard echt.hasPrefix(wurzel + "/") else { return nil }
        let r = await Shell.zsh("git ls-files --error-unmatch -- \(Shell.zitiert(pfad))", ordner: ordner, sekunden: 20)
        return r.code == 0 ? echt : nil
    }

    static let alle: [LokalesWerkzeug] = [
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "repo_lesen",
            beschreibung: "Jarvis am Mac: liest Franks Repo direkt aus ~/proggs, ohne die Grenzen der GitHub-Anbindung. aktion=projekte: oberste Ebene (jeder Ordner ist meist ein Projekt). dateien: alle Dateien eines Ordners (pfad), höchstens 800. datei: Inhalt einer versionierten Datei (pfad; je Aufruf \(zeilen) Zeilen, mit von_zeile weiterlesen). suche: Code-Suche im ganzen Repo oder unter pfad (anfrage), höchstens 200 Treffer. verlauf: letzte Commits, auf Wunsch nur für pfad, mit dem Stand gegenüber GitHub. prs: offene Pull Requests. Mit zweig liest du einen anderen Stand. Gekürzte Ergebnisse sind als gekürzt markiert. Nur lesen.",
            schema: ["type": "object", "properties": [
                "aktion": ["type": "string", "enum": ["projekte", "dateien", "datei", "suche", "verlauf", "prs"]],
                "pfad": ["type": "string", "description": "Pfad im Repo, zum Beispiel Jarvis oder Jarvis/app/build.gradle.kts"],
                "anfrage": ["type": "string", "description": "Für suche: Suchbegriff"],
                "von_zeile": ["type": "integer", "description": "Für datei: erste Zeile, Vorgabe 1"],
                "zweig": ["type": "string", "description": "Anderer Zweig oder Commit, Vorgabe: aktueller Stand am Mac"],
            ] as JSONObjekt, "required": ["aktion"]])) { a in
            // Ohne Vollzugriff bleibt der Weg über das Handy.
            guard await MainActor.run(body: { Einstellungen.shared.vollzugriff }) else { return await McpKlient.shared.rufe("repo_lesen", a) }
            let pfad = a["pfad"] as? String ?? "", anfrage = a["anfrage"] as? String ?? "", zweig = a["zweig"] as? String ?? ""
            guard !pfad.hasPrefix("/"), !pfad.hasPrefix("~"), !pfad.hasPrefix("-"), !pfad.components(separatedBy: "/").contains("..") else { return fehler("pfad muss relativ zum Repo sein.") }
            if let g = Tresor.sperre(pfad, anfrage, zweig) { return g }
            // zweig wird Argument von git: erst prüfen, sonst ginge eine Option wie --output durch.
            if !zweig.isEmpty {
                let geprueft = await Shell.zsh("git rev-parse --verify --quiet --end-of-options \(Shell.zitiert(zweig + "^{commit}"))", ordner: ordner, sekunden: 20)
                guard !zweig.hasPrefix("-"), geprueft.code == 0 else { return fehler("Zweig oder Commit „\(zweig)“ gibt es nicht.") }
            }
            let p = pfad.isEmpty ? "." : Shell.zitiert(pfad)
            let z = zweig.isEmpty ? "" : Shell.zitiert(zweig) + " "
            switch a["aktion"] as? String ?? "" {
            case "projekte":
                let b = zweig.isEmpty ? "ls -1p" : "git ls-tree --name-only \(z)"
                return text(await Shell.zsh(b, ordner: ordner, sekunden: 20), leer: "Leer.")
            case "dateien":
                let b = zweig.isEmpty ? "git ls-files -- \(p)" : "git ls-tree -r --name-only \(z)-- \(p)"
                return begrenzt(await Shell.zsh(b, ordner: ordner, sekunden: 30), leer: "Keine Dateien unter \(pfad).", hoechstens: 800)
            case "datei":
                guard !pfad.isEmpty else { return fehler("pfad fehlt.") }
                let von = max(1, a["von_zeile"] as? Int ?? 1)
                if !zweig.isEmpty {
                    let r = await Shell.zsh("git show \(Shell.zitiert(zweig + ":" + pfad))", ordner: ordner, sekunden: 30)
                    return r.code == 0 ? ausschnitt(r.ausgabe, von: von) : text(r, leer: "")
                }
                guard let echt = await dateiImRepo(pfad) else { return fehler("„\(pfad)“ ist keine versionierte Datei im Repo.") }
                guard let inhalt = try? String(contentsOfFile: echt, encoding: .utf8) else { return fehler("„\(pfad)“ ist nicht als Text lesbar.") }
                return ausschnitt(inhalt, von: von)
            case "suche":
                guard !anfrage.isEmpty else { return fehler("anfrage fehlt.") }
                let r = await Shell.zsh("git grep -n -I -e \(Shell.zitiert(anfrage)) \(z)-- \(p)", ordner: ordner, sekunden: 60)
                // git grep endet mit 1, wenn es nichts findet; das ist kein Fehler.
                if r.code == 1 && r.fehler.isEmpty { return WerkzeugErgebnis(text: "Nichts gefunden.") }
                return begrenzt(r, leer: "Nichts gefunden.", hoechstens: 200, breite: 260)
            case "verlauf":
                let r = await Shell.zsh("git log -n 30 --date=short --format='%h %ad %s' \(z)-- \(p)", ordner: ordner, sekunden: 30)
                var e = text(r, leer: "Kein Verlauf.")
                if !e.fehler && zweig.isEmpty {
                    let stand = await Shell.zsh("git status -sb", ordner: ordner, sekunden: 20)
                    e.text = (stand.ausgabe.split(separator: "\n").first.map(String.init) ?? "") + "\n" + e.text
                }
                return e
            case "prs":
                return text(await Shell.zsh("gh pr list --limit 30", ordner: ordner, sekunden: 40), leer: "Keine offenen Pull Requests.")
            default:
                return fehler("Unbekannte aktion.")
            }
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "cli_frage",
            beschreibung: "Jarvis am Mac: fragt im Hintergrund das Claude-Code-CLI in ~/proggs, ohne Fenster. Es kann nur Dateien lesen und suchen, nichts ändern und nichts ausführen; ~/SK ist gesperrt. Für Fragen zum Repo und Prüfungen, die mehrere Dateien brauchen: „Wie funktioniert …?“, „Ist … schon eingebaut?“, „Wo steckt der Fehler?“. Dauert bis zu zehn Minuten und kostet Abo-Kontingent. Einzelne Dateien liest du schneller mit repo_lesen. Zum Ändern von Code eine Sitzung mit openlauncher_sitzung starten oder programmieren.",
            schema: ["type": "object", "properties": [
                "frage": ["type": "string", "description": "Die vollständige, für sich allein verständliche Frage"],
                "modell": ["type": "string", "description": "sonnet (Vorgabe), opus oder haiku"],
            ] as JSONObjekt, "required": ["frage"]])) { a in
            guard await MainActor.run(body: { Einstellungen.shared.vollzugriff }) else {
                return fehler("Vollzugriff ist in den Einstellungen ausgeschaltet. Frank muss ihn einschalten (Einstellungen → Mac).")
            }
            let frage = a["frage"] as? String ?? ""
            guard !frage.isEmpty else { return fehler("frage fehlt.") }
            if let g = Tresor.sperre(frage) { return g }
            let modell = ["sonnet", "opus", "haiku"].first { $0 == a["modell"] as? String } ?? "sonnet"
            let voll = frage + "\n\nAntworte auf Deutsch und knapp."
            // Dieselbe Anmeldung wie die Sitzungen aus dem OpenLauncher, aber ohne dessen Einstellungen: Dort stünde der Modus „Rechte umgehen“,
            // dazu kämen Hooks und MCP-Server. Die Sperre gilt erst durch --tools und dontAsk, der Fragetext allein schützt nichts.
            let profil = Pfade.heim.appendingPathComponent("proggs/OpenLauncher/Profiles/ClaudeCodeMac/minimal").path
            let sperre = "Read(/" + Pfade.heim.appendingPathComponent("SK").path + "/**)"
            let befehl = "CLAUDE_CONFIG_DIR=\(Shell.zitiert(profil)) claude -p \(Shell.zitiert(voll)) --output-format text --model \(modell)"
                + " --setting-sources project --strict-mcp-config --permission-mode dontAsk --tools 'Read,Grep,Glob' --disallowedTools \(Shell.zitiert(sperre))"
            let r = await Shell.zsh(befehl, ordner: ordner, sekunden: 600)
            if r.zeitAus { return fehler("Das CLI hat in zehn Minuten nicht geantwortet. Für so große Fragen openlauncher_sitzung nehmen.") }
            return text(r, leer: "Das CLI hat nichts geantwortet.")
        },
    ]
}
