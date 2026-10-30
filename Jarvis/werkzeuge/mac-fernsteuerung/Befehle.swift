// Computer Use: Befehle des im Jarvis-Chat gewählten Modells. Das Handy öffnet dafür je Befehl eine eigene Verbindung
// (Anmeldung mit art=werkzeug); eine laufende Live-Sitzung bleibt davon unberührt, der Dienst legt nur den Bildschirm bereit.
//
//   Handy -> Mac: 0x12 Befehl-JSON {t:…}
//   Mac -> Handy: 0x05 Antwort-JSON {ok, fehler?, …}, bei Dateien danach 0x06 mit den Rohdaten (Antwort nennt bytes).
// Koordinaten sind wie bei der Live-Bedienung Anteile (0…1) des Bildschirms, die Umrechnung aus Bildpixeln macht das Handy.

import AppKit
import ApplicationServices
import CoreGraphics
import Foundation
import ImageIO
import ScreenCaptureKit
import UniformTypeIdentifiers

let WERKZEUG_BILD_BREITE = 1280
let AUFNAHME_SEKUNDEN_MAX = 120

/// Fertig mit Antwort und optionaler Datei; Aufruf auf beliebigem Faden.
typealias BefehlFertig = ([String: Any], Data?) -> Void

final class Befehle: NSObject, SCRecordingOutputDelegate {
    unowned let dienst: Dienst
    private var aufnahmeLaeuft = false
    private var aufnahmeFertig: ((Error?) -> Void)?

    init(dienst: Dienst) { self.dienst = dienst }

    /// Nur auf main.
    func ausfuehren(_ j: [String: Any], fertig: @escaping BefehlFertig) {
        let t = j["t"] as? String ?? ""
        log("Befehl: \(t)")
        dienst.wach.wecken()
        if t != "programme", Zustand.gesperrt() {
            fertig(["ok": false, "gesperrt": true, "fehler": "Der Mac ist gesperrt; das Entsperren ist Franks Sache."], nil); return
        }
        switch t {
        case "bildschirm": bildschirm(j, fertig)
        case "eingabe": eingabe(j, fertig)
        case "programme": programme(j, fertig)
        case "aufnahme": aufnahme(j, fertig)
        default: fertig(["ok": false, "fehler": "Unbekannter Befehl \(t)"], nil)
        }
    }

    // MARK: Bildschirm

    /// Der Bildschirm, auf den Bild und Klicks gehen. Ohne Live-Sitzung kümmert sich der Befehl selbst darum (Deckel zu: virtueller Bildschirm).
    private func zielBildschirm() -> CGDirectDisplayID {
        dienst.werkzeugGenutzt()
        if dienst.sitzung != nil { return dienst.bildschirm.gezeigt ?? dienst.bildschirm.displayID }
        dienst.bildschirm.sicherstellen()
        dienst.bildschirm.gezeigt = dienst.bildschirm.displayID
        return dienst.bildschirm.displayID
    }

    /// Ein eben angelegter virtueller Bildschirm taucht erst nach kurzer Zeit in der Liste auf.
    private func anzeige(_ id: CGDirectDisplayID, versuch: Int = 0, fertig: @escaping (SCDisplay?, String?) -> Void) {
        SCShareableContent.getExcludingDesktopWindows(false, onScreenWindowsOnly: true) { inhalt, fehler in
            DispatchQueue.main.async {
                if let d = inhalt?.displays.first(where: { $0.displayID == id }) { fertig(d, nil); return }
                if versuch < 6 {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { self.anzeige(id, versuch: versuch + 1, fertig: fertig) }
                    return
                }
                if let d = inhalt?.displays.first { self.dienst.bildschirm.gezeigt = d.displayID; fertig(d, nil); return }
                fertig(nil, fehler?.localizedDescription ?? "kein Bildschirm verfügbar")
            }
        }
    }

    static func jpeg(_ bild: CGImage, qualitaet: Double = 0.72) -> Data? {
        let d = NSMutableData()
        guard let z = CGImageDestinationCreateWithData(d, UTType.jpeg.identifier as CFString, 1, nil) else { return nil }
        CGImageDestinationAddImage(z, bild, [kCGImageDestinationLossyCompressionQuality: qualitaet] as CFDictionary)
        return CGImageDestinationFinalize(z) ? d as Data : nil
    }

    /// bereich = Ausschnitt in Anteilen {x,y,w,h}: das Bild zeigt dann nur ihn, in voller Breite (zum Lesen kleiner Schrift).
    private func foto(_ d: SCDisplay, bereich: [String: Any]? = nil, fertig: @escaping (Data?, Int, Int, String?) -> Void) {
        let rahmen = CGDisplayBounds(d.displayID)
        var quelle = CGRect(origin: .zero, size: rahmen.size)
        if let b = bereich {
            let x = min(max((b["x"] as? Double) ?? 0, 0), 0.99), y = min(max((b["y"] as? Double) ?? 0, 0), 0.99)
            let w = min(max((b["w"] as? Double) ?? 1, 0.01), 1 - x), h = min(max((b["h"] as? Double) ?? 1, 0.01), 1 - y)
            quelle = CGRect(x: x * rahmen.width, y: y * rahmen.height, width: w * rahmen.width, height: h * rahmen.height)
        }
        let breite = WERKZEUG_BILD_BREITE
        let hoehe = max(1, Int(CGFloat(breite) * quelle.height / max(quelle.width, 1)))
        let cfg = SCStreamConfiguration()
        cfg.width = breite; cfg.height = hoehe
        cfg.sourceRect = quelle
        cfg.showsCursor = true
        cfg.colorSpaceName = CGColorSpace.sRGB
        let filter = SCContentFilter(display: d, excludingWindows: [])
        SCScreenshotManager.captureImage(contentFilter: filter, configuration: cfg) { bild, fehler in
            guard let bild, let daten = Befehle.jpeg(bild) else {
                fertig(nil, 0, 0, fehler?.localizedDescription ?? "Bildschirmfoto gescheitert"); return
            }
            fertig(daten, bild.width, bild.height, nil)
        }
    }

    private func bildschirm(_ j: [String: Any], _ fertig: @escaping BefehlFertig) {
        anzeige(zielBildschirm()) { d, fehler in
            guard let d else { fertig(["ok": false, "fehler": fehler ?? "kein Bildschirm"], nil); return }
            self.foto(d, bereich: j["bereich"] as? [String: Any]) { daten, w, h, fehler in
                guard let daten else { fertig(["ok": false, "fehler": fehler ?? "kein Bild"], nil); return }
                let (text, geheim) = Zustand.fokus()
                DispatchQueue.main.async {
                    fertig(["ok": true, "bild": daten.base64EncodedString(), "w": w, "h": h,
                            "vorne": NSWorkspace.shared.frontmostApplication?.localizedName ?? "",
                            "textfeld": text, "geheim": geheim], nil)
                }
            }
        }
    }

    // MARK: Eingaben

    private func eingabe(_ j: [String: Any], _ fertig: @escaping BefehlFertig) {
        let schritte = j["schritte"] as? [[String: Any]] ?? []
        for s in schritte where (s["t"] as? String) == "taste" {
            let k = (s["k"] as? String) ?? ""
            if !dienst.eingabe.kennt(k) { fertig(["ok": false, "fehler": "Unbekannte Taste \(k)"], nil); return }
        }
        // Dieselbe Abbildung wie das Bild: Ohne Sitzung den Zielbildschirm vorher festlegen, auch nach einem Deckelwechsel.
        _ = zielBildschirm()
        dienst.eingabeWarteschlange.async {
            for s in schritte {
                if let ms = s["warten_ms"] as? Int { usleep(UInt32(min(max(ms, 0), 10_000)) * 1000); continue }
                self.dienst.eingabe.ausfuehren(s)
                usleep(30_000)
            }
            fertig(["ok": true], nil)
        }
    }

    // MARK: Programme und Fenster

    private func programme(_ j: [String: Any], _ fertig: @escaping BefehlFertig) {
        let aktion = j["aktion"] as? String ?? "liste"
        let name = (j["app"] as? String) ?? ""
        func laufend() -> NSRunningApplication? {
            NSWorkspace.shared.runningApplications.first { $0.activationPolicy == .regular && ($0.localizedName?.caseInsensitiveCompare(name) == .orderedSame || $0.bundleIdentifier == name) }
                ?? NSWorkspace.shared.runningApplications.first { $0.activationPolicy == .regular && ($0.localizedName?.localizedCaseInsensitiveContains(name) ?? false) && !name.isEmpty }
        }
        switch aktion {
        case "liste":
            fertig(["ok": true, "gesperrt": Zustand.gesperrt(), "vorne": NSWorkspace.shared.frontmostApplication?.localizedName ?? "",
                    "programme": NSWorkspace.shared.runningApplications.filter { $0.activationPolicy == .regular }.compactMap { $0.localizedName },
                    "fenster": fensterListe()], nil)
        case "oeffnen", "vordergrund":
            guard !Zustand.gesperrt() else { fertig(["ok": false, "gesperrt": true, "fehler": "Der Mac ist gesperrt."], nil); return }
            // „open“ holt ein Programm auch aus einem Hintergrunddienst zuverlässig nach vorn (activate() lehnt macOS dort ab).
            let ziel = (j["ziel"] as? String) ?? ""
            let args: [String] = name.isEmpty ? [ziel] : (ziel.isEmpty ? ["-a", name] : ["-a", name, ziel])
            guard !(name.isEmpty && ziel.isEmpty) else { fertig(["ok": false, "fehler": "Programm oder Ziel fehlt."], nil); return }
            DispatchQueue.global().async {
                let p = Process()
                p.executableURL = URL(fileURLWithPath: "/usr/bin/open"); p.arguments = args
                let rohr = Pipe(); p.standardError = rohr; p.standardOutput = Pipe()
                do { try p.run() } catch { fertig(["ok": false, "fehler": error.localizedDescription], nil); return }
                p.waitUntilExit()
                let meldung = String(data: rohr.fileHandleForReading.readDataToEndOfFile(), encoding: .utf8) ?? ""
                fertig(p.terminationStatus == 0 ? ["ok": true] : ["ok": false, "fehler": meldung.isEmpty ? "open ist gescheitert" : meldung], nil)
            }
        case "beenden":
            guard let app = laufend() else { fertig(["ok": false, "fehler": "\(name) läuft nicht."], nil); return }
            fertig(app.terminate() ? ["ok": true] : ["ok": false, "fehler": "Beenden abgelehnt."], nil)
        case "fenster_verschieben", "fenster_groesse", "fenster_minimieren", "fenster_schliessen", "fenster_vorn":
            guard let app = laufend() else { fertig(["ok": false, "fehler": "\(name) läuft nicht."], nil); return }
            fertig(fensterAktion(app, aktion, j), nil)
        default:
            fertig(["ok": false, "fehler": "Unbekannte Aktion \(aktion)"], nil)
        }
    }

    /// Sichtbare Fenster, Rahmen in Anteilen des gezeigten Bildschirms.
    private func fensterListe() -> [[String: Any]] {
        let r = dienst.bildschirm.rahmen
        let infos = CGWindowListCopyWindowInfo([.optionOnScreenOnly, .excludeDesktopElements], kCGNullWindowID) as? [[String: Any]] ?? []
        return infos.compactMap { w in
            guard (w[kCGWindowLayer as String] as? Int) == 0, let b = w[kCGWindowBounds as String] as? [String: Double],
                  (b["Width"] ?? 0) > 40, (b["Height"] ?? 0) > 40 else { return nil }
            return ["app": w[kCGWindowOwnerName as String] as? String ?? "", "titel": w[kCGWindowName as String] as? String ?? "",
                    "x": ((b["X"] ?? 0) - r.minX) / r.width, "y": ((b["Y"] ?? 0) - r.minY) / r.height,
                    "w": (b["Width"] ?? 0) / r.width, "h": (b["Height"] ?? 0) / r.height]
        }
    }

    private func fensterAktion(_ app: NSRunningApplication, _ aktion: String, _ j: [String: Any]) -> [String: Any] {
        let el = AXUIElementCreateApplication(app.processIdentifier)
        var wert: CFTypeRef?
        guard AXUIElementCopyAttributeValue(el, kAXWindowsAttribute as CFString, &wert) == .success, let alle = wert as? [AXUIElement], !alle.isEmpty else {
            return ["ok": false, "fehler": "\(app.localizedName ?? "Das Programm") hat kein bedienbares Fenster."]
        }
        let suche = (j["fenster"] as? String) ?? ""
        let fenster = suche.isEmpty ? alle[0] : alle.first { f in
            var t: CFTypeRef?
            AXUIElementCopyAttributeValue(f, kAXTitleAttribute as CFString, &t)
            return (t as? String)?.localizedCaseInsensitiveContains(suche) ?? false
        }
        guard let fenster else { return ["ok": false, "fehler": "Kein Fenster mit „\(suche)“ im Titel."] }
        let r = dienst.bildschirm.rahmen
        var status = AXError.success
        switch aktion {
        case "fenster_verschieben":
            var p = CGPoint(x: r.minX + ((j["x"] as? Double) ?? 0) * r.width, y: r.minY + ((j["y"] as? Double) ?? 0) * r.height)
            if let w = AXValueCreate(.cgPoint, &p) { status = AXUIElementSetAttributeValue(fenster, kAXPositionAttribute as CFString, w) }
        case "fenster_groesse":
            var g = CGSize(width: ((j["w"] as? Double) ?? 0.5) * r.width, height: ((j["h"] as? Double) ?? 0.5) * r.height)
            if let w = AXValueCreate(.cgSize, &g) { status = AXUIElementSetAttributeValue(fenster, kAXSizeAttribute as CFString, w) }
        case "fenster_minimieren":
            status = AXUIElementSetAttributeValue(fenster, kAXMinimizedAttribute as CFString, kCFBooleanTrue)
        case "fenster_vorn":
            status = AXUIElementPerformAction(fenster, kAXRaiseAction as CFString)
        default: // fenster_schliessen
            var knopf: CFTypeRef?
            guard AXUIElementCopyAttributeValue(fenster, kAXCloseButtonAttribute as CFString, &knopf) == .success, let k = knopf else {
                return ["ok": false, "fehler": "Das Fenster hat keinen Schließen-Knopf."]
            }
            status = AXUIElementPerformAction(k as! AXUIElement, kAXPressAction as CFString)
        }
        return status == .success ? ["ok": true] : ["ok": false, "fehler": "Das Fenster nimmt das nicht an (AX-Fehler \(status.rawValue))."]
    }

    // MARK: Aufnahme

    /// Nimmt [sekunden] den Bildschirm auf (MP4). Dazu vier Standbilder gleichmäßig über die Zeit, damit das Modell sieht, was passiert ist.
    private func aufnahme(_ j: [String: Any], _ fertig: @escaping BefehlFertig) {
        guard !aufnahmeLaeuft else { fertig(["ok": false, "fehler": "Es läuft schon eine Aufnahme."], nil); return }
        let sekunden = min(max((j["sekunden"] as? Int) ?? 10, 1), AUFNAHME_SEKUNDEN_MAX)
        aufnahmeLaeuft = true
        anzeige(zielBildschirm()) { d, fehler in
            guard let d else {
                self.aufnahmeLaeuft = false
                fertig(["ok": false, "fehler": fehler ?? "kein Bildschirm"], nil); return
            }
            self.aufnehmen(d, sekunden, fertig)
        }
    }

    private func aufnehmen(_ d: SCDisplay, _ sekunden: Int, _ fertig: @escaping BefehlFertig) {
        let datei = FileManager.default.temporaryDirectory.appendingPathComponent("jarvis-aufnahme-\(UUID().uuidString).mp4")
        let rahmen = CGDisplayBounds(d.displayID)
        let cfg = SCStreamConfiguration()
        cfg.width = WERKZEUG_BILD_BREITE & ~1
        cfg.height = max(2, Int(CGFloat(WERKZEUG_BILD_BREITE) * rahmen.height / rahmen.width)) & ~1
        cfg.minimumFrameInterval = CMTime(value: 1, timescale: 15)
        cfg.showsCursor = true
        let stream = SCStream(filter: SCContentFilter(display: d, excludingWindows: []), configuration: cfg, delegate: nil)
        let ausgabe = SCRecordingOutputConfiguration()
        ausgabe.outputURL = datei
        ausgabe.outputFileType = .mp4
        ausgabe.videoCodecType = .h264
        let aufzeichnung = SCRecordingOutput(configuration: ausgabe, delegate: self)
        func scheitert(_ grund: String) {
            DispatchQueue.main.async { self.aufnahmeLaeuft = false }
            try? FileManager.default.removeItem(at: datei)
            fertig(["ok": false, "fehler": grund], nil)
        }
        do { try stream.addRecordingOutput(aufzeichnung) } catch { scheitert("Aufnahme: \(error.localizedDescription)"); return }
        let bilder = BilderSammlung()
        stream.startCapture { fehler in
            if let fehler { scheitert("Aufnahme startet nicht: \(fehler.localizedDescription)"); return }
            for i in 0..<4 {
                DispatchQueue.main.asyncAfter(deadline: .now() + Double(sekunden) * Double(i) / 3.0 * 0.98) {
                    self.foto(d) { daten, _, _, _ in if let daten { bilder.setze(i, daten) } }
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(sekunden)) {
                var abgeschlossen = false
                self.aufnahmeFertig = { fehler in
                    guard !abgeschlossen else { return }
                    abgeschlossen = true
                    self.aufnahmeFertig = nil
                    self.aufnahmeLaeuft = false
                    defer { try? FileManager.default.removeItem(at: datei) }
                    guard fehler == nil, let video = try? Data(contentsOf: datei), !video.isEmpty else {
                        fertig(["ok": false, "fehler": "Aufnahme gescheitert: \(fehler?.localizedDescription ?? "keine Datei")"], nil); return
                    }
                    fertig(["ok": true, "sekunden": sekunden, "bilder": bilder.alle().map { $0.base64EncodedString() }, "bytes": video.count], video)
                }
                // Die Datei wird erst mit dem Stoppen fertig; meldet sich der Abschluss nicht, gilt die Datei nach kurzer Frist als fertig.
                stream.stopCapture { fehler in
                    if let fehler { log("Aufnahme stoppen: \(fehler.localizedDescription)") }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 3) { self.aufnahmeFertig?(nil) }
                }
            }
        }
    }

    func recordingOutputDidFinishRecording(_ recordingOutput: SCRecordingOutput) {
        DispatchQueue.main.async { self.aufnahmeFertig?(nil) }
    }

    func recordingOutput(_ recordingOutput: SCRecordingOutput, didFailWithError error: Error) {
        DispatchQueue.main.async { self.aufnahmeFertig?(error) }
    }
}

private final class BilderSammlung {
    private let sperre = NSLock()
    private var werte: [Int: Data] = [:]
    func setze(_ i: Int, _ d: Data) { sperre.lock(); werte[i] = d; sperre.unlock() }
    func alle() -> [Data] { sperre.lock(); defer { sperre.unlock() }; return werte.keys.sorted().compactMap { werte[$0] } }
}
