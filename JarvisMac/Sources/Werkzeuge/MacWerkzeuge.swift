import AppKit
import Foundation

struct LokalesWerkzeug {
    let definition: WerkzeugDefinition
    let ausfuehren: @Sendable (JSONObjekt) async -> WerkzeugErgebnis
}

private func schema(_ eigenschaften: [String: (String, String)], pflicht: [String] = []) -> JSONObjekt {
    var props: JSONObjekt = [:]
    for (k, (typ, beschr)) in eigenschaften { props[k] = ["type": typ, "description": beschr] }
    return ["type": "object", "properties": props, "required": pflicht]
}

private func werkzeug(_ name: String, _ beschreibung: String, _ s: JSONObjekt,
                      _ f: @escaping @Sendable (JSONObjekt) async -> WerkzeugErgebnis) -> LokalesWerkzeug {
    LokalesWerkzeug(definition: WerkzeugDefinition(name: name, beschreibung: beschreibung, schema: s), ausfuehren: f)
}

enum Shell {
    struct Ergebnis { var ausgabe: String; var fehler: String; var code: Int32; var zeitAus: Bool }

    /// Führt einen Befehl in einer Login-zsh aus (gleiche Umgebung wie Franks Terminal).
    static func zsh(_ befehl: String, ordner: String? = nil, sekunden: Double = 120) async -> Ergebnis {
        await withCheckedContinuation { (fort: CheckedContinuation<Ergebnis, Never>) in
            let p = Process()
            p.executableURL = URL(fileURLWithPath: "/bin/zsh")
            p.arguments = ["-l", "-c", befehl]
            var env = ProcessInfo.processInfo.environment
            env["DEVELOPER_DIR"] = "/Library/Developer/CommandLineTools"
            env["PATH"] = "/opt/homebrew/bin:/usr/local/bin:\(Pfade.heim.path)/.local/bin:" + (env["PATH"] ?? "/usr/bin:/bin")
            p.environment = env
            p.currentDirectoryURL = URL(fileURLWithPath: (ordner.map { ($0 as NSString).expandingTildeInPath }) ?? Pfade.heim.appendingPathComponent("proggs").path)
            let aus = Pipe(), err = Pipe()
            p.standardOutput = aus; p.standardError = err; p.standardInput = FileHandle.nullDevice
            let ausDaten = SammelPuffer(), errDaten = SammelPuffer()
            aus.fileHandleForReading.readabilityHandler = { ausDaten.anhaengen($0.availableData) }
            err.fileHandleForReading.readabilityHandler = { errDaten.anhaengen($0.availableData) }
            let zeitAus = SammelPuffer()
            let einmal = SammelPuffer()
            // Fortsetzen genau einmal: Prozessende ODER Zeitlimit. Nie readDataToEndOfFile – Hintergrundkinder („&“)
            // halten die Pipe offen, das Lesen bliebe dann für immer hängen.
            let fertig: @Sendable (Int32) -> Void = { code in
                guard einmal.einmalSetzen() else { return }
                aus.fileHandleForReading.readabilityHandler = nil
                err.fileHandleForReading.readabilityHandler = nil
                fort.resume(returning: Ergebnis(ausgabe: ausDaten.text, fehler: errDaten.text, code: code, zeitAus: zeitAus.istGesetzt))
            }
            p.terminationHandler = { proc in
                let code = proc.terminationStatus
                DispatchQueue.global().asyncAfter(deadline: .now() + 0.25) { fertig(code) }   // Restausgabe einsammeln
            }
            do { try p.run() } catch {
                fort.resume(returning: Ergebnis(ausgabe: "", fehler: error.localizedDescription, code: -1, zeitAus: false)); return
            }
            let pid = p.processIdentifier
            DispatchQueue.global().asyncAfter(deadline: .now() + sekunden) {
                guard !einmal.istGesetzt else { return }
                zeitAus.setzen()
                // Kinder zuerst (z. B. „sleep 999 | cat“), dann die Shell selbst.
                let k = Process(); k.executableURL = URL(fileURLWithPath: "/usr/bin/pkill"); k.arguments = ["-TERM", "-P", "\(pid)"]
                try? k.run(); k.waitUntilExit()
                if p.isRunning { p.terminate() }
                DispatchQueue.global().asyncAfter(deadline: .now() + 1) { fertig(-15) }
            }
        }
    }

    static func osascript(_ skript: String) async -> Ergebnis {
        let datei = Pfade.temp.appendingPathComponent("skript-\(UUID().uuidString).applescript")
        try? skript.write(to: datei, atomically: true, encoding: .utf8)
        defer { try? FileManager.default.removeItem(at: datei) }
        return await zsh("osascript '\(datei.path)'", sekunden: 90)
    }

    static func zitiert(_ s: String) -> String { "'" + s.replacingOccurrences(of: "'", with: "'\\''") + "'" }
}

final class SammelPuffer: @unchecked Sendable {
    private var daten = Data()
    private var gesetzt = false
    private let lock = NSLock()
    func anhaengen(_ d: Data) { lock.lock(); if daten.count < 2_000_000 { daten.append(d) }; lock.unlock() }
    var text: String { lock.lock(); defer { lock.unlock() }; return String(decoding: daten, as: UTF8.self) }
    func setzen() { lock.lock(); gesetzt = true; lock.unlock() }
    /// true nur beim ersten Aufruf.
    func einmalSetzen() -> Bool { lock.lock(); defer { lock.unlock() }; if gesetzt { return false }; gesetzt = true; return true }
    var istGesetzt: Bool { lock.lock(); defer { lock.unlock() }; return gesetzt }
}

private func befehlsText(_ r: Shell.Ergebnis) -> WerkzeugErgebnis {
    var t = ""
    if !r.ausgabe.isEmpty { t += r.ausgabe }
    if !r.fehler.isEmpty { t += (t.isEmpty ? "" : "\n") + "[stderr]\n" + r.fehler }
    if r.zeitAus { t += "\n[Zeitlimit erreicht, Prozess beendet]" }
    t += "\n[Exit-Code \(r.code)]"
    return WerkzeugErgebnis(text: t.trimmingCharacters(in: .whitespacesAndNewlines), fehler: r.code != 0)
}

@MainActor private var vollzugriff: Bool { Einstellungen.shared.vollzugriff }

private func gesperrt() async -> WerkzeugErgebnis? {
    await MainActor.run { vollzugriff } ? nil
        : WerkzeugErgebnis(text: "Vollzugriff ist in den Einstellungen ausgeschaltet. Frank muss ihn einschalten (Einstellungen → Mac).", fehler: true)
}

enum MacWerkzeuge {
    static let openLauncher = "/Applications/OpenLauncher.app/Contents/MacOS/OpenLauncher"

    static let alle: [LokalesWerkzeug] = [
        werkzeug("mac_befehl",
                 "Mac: führt einen Shell-Befehl in einer Login-zsh aus (Vollzugriff, ohne Rückfrage). Für alles, was per Kommandozeile geht: git, Dateien, Prozesse, brew, Systeminfos, Builds, Skripte. Standardordner ~/proggs. Lange Läufe mit zeitlimit_s (bis 1800) oder im Hintergrund mit '&'. Gibt stdout, stderr und Exit-Code zurück.",
                 schema(["befehl": ("string", "Der zsh-Befehl"), "ordner": ("string", "Arbeitsordner, Vorgabe ~/proggs"),
                         "zeitlimit_s": ("integer", "Zeitlimit in Sekunden, Vorgabe 120")], pflicht: ["befehl"])) { a in
            if let g = await gesperrt() { return g }
            let s = Double(a["zeitlimit_s"] as? Int ?? 120)
            return befehlsText(await Shell.zsh(a["befehl"] as? String ?? "", ordner: a["ordner"] as? String, sekunden: min(max(s, 5), 1800)))
        },
        werkzeug("mac_applescript",
                 "Mac: führt AppleScript aus – Programme fernsteuern (Finder, Safari, Mail, Musik, Systemeinstellungen, Terminal, System Events für Klicks und Tasten), Fenster anordnen, Dialoge. Gibt das Ergebnis zurück.",
                 schema(["skript": ("string", "AppleScript-Quelltext")], pflicht: ["skript"])) { a in
            if let g = await gesperrt() { return g }
            return befehlsText(await Shell.osascript(a["skript"] as? String ?? ""))
        },
        werkzeug("mac_oeffnen",
                 "Mac: öffnet ein Programm (Name wie 'Safari', 'ChatGPT', 'Updater-Zentrale macOS'), eine Webadresse, eine Datei oder einen Ordner. Mit 'mit' das Programm, das eine Datei/Adresse öffnen soll.",
                 schema(["ziel": ("string", "Programmname, URL oder Pfad"), "mit": ("string", "optional: Programm zum Öffnen")], pflicht: ["ziel"])) { a in
            let ziel = (a["ziel"] as? String ?? "").trimmingCharacters(in: .whitespaces)
            let mit = a["mit"] as? String
            var cmd: String
            if ziel.contains("://") || ziel.hasPrefix("/") || ziel.hasPrefix("~") {
                cmd = "open " + (mit.map { "-a \(Shell.zitiert($0)) " } ?? "") + Shell.zitiert((ziel as NSString).expandingTildeInPath)
            } else {
                cmd = "open -a \(Shell.zitiert(ziel))"
            }
            return befehlsText(await Shell.zsh(cmd, sekunden: 30))
        },
        werkzeug("mac_programme",
                 "Mac: laufende Programme auflisten, eines in den Vordergrund holen, beenden oder hart beenden. aktion = liste | vordergrund | beenden | hart_beenden.",
                 schema(["aktion": ("string", "liste | vordergrund | beenden | hart_beenden"), "name": ("string", "Programmname")], pflicht: ["aktion"])) { a in
            let aktion = a["aktion"] as? String ?? "liste"
            let name = (a["name"] as? String ?? "").lowercased()
            if aktion == "beenden" || aktion == "hart_beenden", let g = await gesperrt() { return g }
            return await MainActor.run {
                let apps = NSWorkspace.shared.runningApplications.filter { $0.activationPolicy == .regular }
                if aktion == "liste" {
                    let vorne = NSWorkspace.shared.frontmostApplication?.localizedName ?? ""
                    return WerkzeugErgebnis(text: "Im Vordergrund: \(vorne)\nLaufend: " + apps.compactMap(\.localizedName).sorted().joined(separator: ", "))
                }
                guard let app = apps.first(where: { ($0.localizedName ?? "").lowercased() == name })
                        ?? apps.first(where: { ($0.localizedName ?? "").lowercased().contains(name) }) else {
                    return WerkzeugErgebnis(text: "Kein laufendes Programm „\(name)“ gefunden.", fehler: true)
                }
                switch aktion {
                case "vordergrund": app.activate(); return WerkzeugErgebnis(text: "\(app.localizedName ?? name) ist vorne.")
                case "beenden": return WerkzeugErgebnis(text: app.terminate() ? "\(app.localizedName ?? name) wird beendet." : "Beenden abgelehnt.")
                case "hart_beenden": return WerkzeugErgebnis(text: app.forceTerminate() ? "\(app.localizedName ?? name) hart beendet." : "Ging nicht.")
                default: return WerkzeugErgebnis(text: "Unbekannte Aktion \(aktion)", fehler: true)
                }
            }
        },
        werkzeug("mac_bildschirm",
                 "Mac: macht ein Bildschirmfoto des Hauptbildschirms und zeigt es dir, damit du siehst, was gerade auf dem Mac los ist (Fenster, Fehlermeldungen, Dialoge).",
                 schema([:])) { _ in
            if let g = await gesperrt() { return g }
            // JPEG, 1440 px: rund 200 KB statt mehrerer MB PNG – der Verlauf bleibt klein.
            let datei = Pfade.temp.appendingPathComponent("bildschirm.jpg")
            try? FileManager.default.removeItem(at: datei)
            let r = await Shell.zsh("screencapture -x -m -t jpg '\(datei.path)' && sips -Z 1440 -s formatOptions 60 '\(datei.path)' >/dev/null", sekunden: 20)
            guard let d = try? Data(contentsOf: datei) else {
                return WerkzeugErgebnis(text: "Bildschirmfoto ging nicht (Bildschirmaufnahme-Recht für Jarvis in Systemeinstellungen → Datenschutz erteilen). \(r.fehler)", fehler: true)
            }
            return WerkzeugErgebnis(text: "Bildschirmfoto aufgenommen.", bildPNG: d)
        },
        werkzeug("mac_tastatur",
                 "Mac: tippt Text in das vorderste Programm oder drückt ein Tastenkürzel (z. B. 'cmd+t', 'cmd+shift+n', 'enter', 'esc'). Braucht das Bedienungshilfen-Recht.",
                 schema(["text": ("string", "zu tippender Text"), "kuerzel": ("string", "Tastenkürzel wie cmd+v, enter, tab")])) { a in
            if let g = await gesperrt() { return g }
            var skript = "tell application \"System Events\"\n"
            if let t = a["text"] as? String, !t.isEmpty {
                skript += "keystroke \"\(t.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\""))\"\n"
            }
            if let k = a["kuerzel"] as? String, !k.isEmpty { skript += Tasten.applescript(k) + "\n" }
            skript += "end tell"
            return befehlsText(await Shell.osascript(skript))
        },
        werkzeug("mac_zwischenablage",
                 "Mac: liest die Zwischenablage oder schreibt Text hinein. aktion = lesen | schreiben.",
                 schema(["aktion": ("string", "lesen | schreiben"), "text": ("string", "Text zum Schreiben")], pflicht: ["aktion"])) { a in
            if let g = await gesperrt() { return g }
            return await MainActor.run {
                let pb = NSPasteboard.general
                if (a["aktion"] as? String) == "schreiben" {
                    pb.clearContents(); pb.setString(a["text"] as? String ?? "", forType: .string)
                    return WerkzeugErgebnis(text: "In die Zwischenablage gelegt.")
                }
                return WerkzeugErgebnis(text: pb.string(forType: .string) ?? "(Zwischenablage enthält keinen Text)")
            }
        },
        werkzeug("mac_mitteilung",
                 "Mac: zeigt eine Mitteilung oben rechts an (Titel und Text).",
                 schema(["titel": ("string", "Titel"), "text": ("string", "Text")], pflicht: ["text"])) { a in
            let t = (a["titel"] as? String ?? "Jarvis").replacingOccurrences(of: "\"", with: "'")
            let x = (a["text"] as? String ?? "").replacingOccurrences(of: "\"", with: "'")
            return befehlsText(await Shell.osascript("display notification \"\(x)\" with title \"\(t)\""))
        },
        werkzeug("mac_lautstaerke",
                 "Mac: Lautstärke lesen oder setzen (0–100), stumm schalten oder Stummschaltung aufheben.",
                 schema(["wert": ("integer", "0–100"), "stumm": ("boolean", "true = stumm, false = Ton an")])) { a in
            if let g = await gesperrt() { return g }
            var skript = ""
            if let w = a["wert"] as? Int { skript += "set volume output volume \(min(max(w, 0), 100))\n" }
            if let s = a["stumm"] as? Bool { skript += "set volume output muted \(s)\n" }
            skript += "get volume settings"
            return befehlsText(await Shell.osascript(skript))
        },
        werkzeug("mac_datei",
                 "Mac: Datei lesen (aktion lesen, bis 200 000 Zeichen), schreiben/überschreiben (schreiben), anhängen (anhaengen) oder Ordner auflisten (liste).",
                 schema(["aktion": ("string", "lesen | schreiben | anhaengen | liste"), "pfad": ("string", "Pfad, ~ erlaubt"),
                         "inhalt": ("string", "Inhalt für schreiben/anhaengen")], pflicht: ["aktion", "pfad"])) { a in
            if let g = await gesperrt() { return g }
            let pfad = ((a["pfad"] as? String ?? "") as NSString).expandingTildeInPath
            let url = URL(fileURLWithPath: pfad)
            switch a["aktion"] as? String ?? "" {
            case "lesen":
                guard let d = try? Data(contentsOf: url) else { return WerkzeugErgebnis(text: "Nicht lesbar: \(pfad)", fehler: true) }
                let t = String(decoding: d.prefix(400_000), as: UTF8.self)
                return WerkzeugErgebnis(text: t.count > 200_000 ? String(t.prefix(200_000)) + "\n… [gekürzt]" : t)
            case "schreiben", "anhaengen":
                let inhalt = a["inhalt"] as? String ?? ""
                try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
                if (a["aktion"] as? String) == "anhaengen", let h = try? FileHandle(forWritingTo: url) {
                    h.seekToEndOfFile(); h.write(Data(inhalt.utf8)); try? h.close()
                } else {
                    do { try inhalt.write(to: url, atomically: true, encoding: .utf8) } catch {
                        return WerkzeugErgebnis(text: error.localizedDescription, fehler: true)
                    }
                }
                return WerkzeugErgebnis(text: "Gespeichert: \(pfad) (\(inhalt.count) Zeichen)")
            case "liste":
                let inhalt = (try? FileManager.default.contentsOfDirectory(atPath: pfad)) ?? []
                return WerkzeugErgebnis(text: inhalt.sorted().prefix(500).joined(separator: "\n"))
            default: return WerkzeugErgebnis(text: "Unbekannte Aktion", fehler: true)
            }
        },
        werkzeug("terminal_befehl",
                 "Mac: öffnet ein sichtbares Terminal-Fenster (Terminal.app) und führt dort einen Befehl aus, damit Frank zusehen oder weiterarbeiten kann. Für interaktive Programme (z. B. claude, codex, ssh, htop).",
                 schema(["befehl": ("string", "Befehl"), "ordner": ("string", "Startordner, Vorgabe ~/proggs")], pflicht: ["befehl"])) { a in
            if let g = await gesperrt() { return g }
            let ordner = ((a["ordner"] as? String ?? "~/proggs") as NSString).expandingTildeInPath
            let voll = "cd \(Shell.zitiert(ordner)) && \(a["befehl"] as? String ?? "")"
            let esc = voll.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\"", with: "\\\"")
            return befehlsText(await Shell.osascript("tell application \"Terminal\"\nactivate\ndo script \"\(esc)\"\nend tell"))
        },
        werkzeug("openlauncher_sitzung",
                 "Mac: startet über den OpenLauncher direkt hier am Mac eine neue Programmier-Sitzung im Terminal (Claude Code, Codex, OpenCode, Kimi, Antigravity) mit einem ersten Auftrag. Vorgaben wie bei Frank: Profil minimal, Modus schnell, Effort high, Modell claude-opus-5-5[1m], CLI claude, Ordner ~/proggs. Für Aufträge mit Rückmeldung ans Handy stattdessen programmieren nehmen.",
                 schema(["auftrag": ("string", "Erste Nachricht/Auftrag an die Sitzung"), "name": ("string", "Fenstertitel, z. B. 'Jarvis: OpenLauncher'"),
                         "cli": ("string", "claude | codex | opencode | kimi | antigravity"), "modell": ("string", "Modell-Kennung aus OpenLauncher/models.json"),
                         "anbieter": ("string", "Modellgruppe, falls nötig"), "profil": ("string", "minimal | standard | strikt"),
                         "modus": ("string", "frei | schnell | normal | gruendlich"), "effort": ("string", "low | medium | high | xhigh | max"),
                         "ordner": ("string", "Arbeitsordner")], pflicht: ["auftrag"])) { a in
            if let g = await gesperrt() { return g }
            guard FileManager.default.isExecutableFile(atPath: openLauncher) else {
                return WerkzeugErgebnis(text: "OpenLauncher ist nicht unter /Applications installiert.", fehler: true)
            }
            let datei = Pfade.temp.appendingPathComponent("auftrag-\(Int(Date().timeIntervalSince1970)).txt")
            try? (a["auftrag"] as? String ?? "").write(to: datei, atomically: true, encoding: .utf8)
            var args = [openLauncher, "--jarvis-auftrag", datei.path, "--name", a["name"] as? String ?? "Jarvis Mac"]
            for k in ["cli", "modell", "anbieter", "profil", "modus", "effort", "ordner"] {
                if let v = a[k] as? String, !v.isEmpty { args += ["--\(k)", v] }
            }
            return befehlsText(await Shell.zsh(args.map(Shell.zitiert).joined(separator: " "), sekunden: 60))
        },
        werkzeug("updater_zentrale",
                 "Mac: Updater-Zentrale. aktion programme = listet alle dort gepflegten Programme mit id, Art, Prüf- und Update-Befehl (aus programs.json); oeffnen = öffnet die App. Ein Update führst du danach mit mac_befehl aus (Update-Befehl des Programms) – OpenLauncher und Overlays nur über ihre Update-Skripte.",
                 schema(["aktion": ("string", "programme | oeffnen")], pflicht: ["aktion"])) { a in
            if (a["aktion"] as? String) == "oeffnen" {
                return befehlsText(await Shell.zsh("open -a 'Updater-Zentrale macOS'", sekunden: 20))
            }
            let pfad = Pfade.heim.appendingPathComponent("proggs/UpdaterZentrale-macOS/programs.json")
            guard let d = try? Data(contentsOf: pfad), let o = JSON.objekt(d), let liste = o["programme"] as? [JSONObjekt] else {
                return WerkzeugErgebnis(text: "programs.json der Updater-Zentrale nicht lesbar.", fehler: true)
            }
            let zeilen = liste.map { p -> String in
                let felder = ["id", "name", "art", "exePfad", "versionsArgumente", "updateArgumente", "npmPaket", "brewPaket", "updateBefehl"]
                return felder.compactMap { k in (p[k] as? String).map { "\(k)=\($0)" } }.joined(separator: " · ")
            }
            return WerkzeugErgebnis(text: zeilen.joined(separator: "\n"))
        },
        werkzeug("chatgpt_app",
                 "Mac: öffnet die ChatGPT-App und fügt einen Text als neue Nachricht ein (optional sofort absenden). Für Fragen, die Frank ausdrücklich an ChatGPT stellen will.",
                 schema(["text": ("string", "Nachricht an ChatGPT"), "senden": ("boolean", "sofort absenden, Vorgabe true")], pflicht: ["text"])) { a in
            if let g = await gesperrt() { return g }
            await MainActor.run {
                NSPasteboard.general.clearContents()
                NSPasteboard.general.setString(a["text"] as? String ?? "", forType: .string)
            }
            let senden = a["senden"] as? Bool ?? true
            let skript = """
            tell application "ChatGPT" to activate
            delay 1.2
            tell application "System Events"
            keystroke "n" using command down
            delay 0.8
            keystroke "v" using command down
            \(senden ? "delay 0.3\nkey code 36" : "")
            end tell
            """
            return befehlsText(await Shell.osascript(skript))
        },
        werkzeug("mac_status",
                 "Mac: kurzer Systemüberblick – Akku, Netzwerk, freier Speicher, Laufzeit, Rechnername, macOS-Version, angeschlossene Bildschirme.",
                 schema([:])) { _ in
            befehlsText(await Shell.zsh("""
            echo "Rechner: $(scutil --get ComputerName) · macOS $(sw_vers -productVersion)"; uptime; pmset -g batt | tail -1;
            echo "WLAN: $(networksetup -getairportnetwork en0 2>/dev/null | sed 's/.*: //')"; df -h / | tail -1;
            system_profiler SPDisplaysDataType 2>/dev/null | grep -E 'Resolution|Display Type' | head -4
            """, sekunden: 30))
        },
    ]
}

enum Tasten {
    /// Wandelt „cmd+shift+n“ in eine System-Events-Zeile um.
    static func applescript(_ kuerzel: String) -> String {
        let teile = kuerzel.lowercased().split(separator: "+").map { $0.trimmingCharacters(in: .whitespaces) }
        var mods: [String] = []
        var taste = ""
        for t in teile {
            switch t {
            case "cmd", "command", "⌘": mods.append("command down")
            case "shift", "⇧": mods.append("shift down")
            case "alt", "option", "opt", "⌥": mods.append("option down")
            case "ctrl", "control", "⌃": mods.append("control down")
            default: taste = t
            }
        }
        let codes: [String: Int] = ["enter": 36, "return": 36, "tab": 48, "space": 49, "leertaste": 49, "esc": 53, "escape": 53,
                                    "delete": 51, "backspace": 51, "links": 123, "left": 123, "rechts": 124, "right": 124,
                                    "runter": 125, "down": 125, "hoch": 126, "up": 126, "f5": 96]
        let using = mods.isEmpty ? "" : " using {\(mods.joined(separator: ", "))}"
        if let c = codes[taste] { return "key code \(c)\(using)" }
        return "keystroke \"\(taste)\"\(using)"
    }
}
