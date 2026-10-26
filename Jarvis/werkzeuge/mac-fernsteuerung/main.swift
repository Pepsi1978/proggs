// Jarvis-Fernsteuerung (Mac-Seite): zeigt den Mac-Bildschirm live in Jarvis auf dem Handy und nimmt Tippen,
// Wischen und Tastatur von dort entgegen.
//
// Bild: ScreenCaptureKit -> H.264 (VideoToolbox, Echtzeit) -> TCP. Eingaben: CGEvent auf HID-Ebene.
// Deckel zu (kein aktiver Bildschirm): ein virtueller Bildschirm wird angelegt und zum Hauptbildschirm gemacht.
// Rechte (Bildschirmaufnahme, Bedienungshilfen) erbt der Dienst von Terminal.app, deshalb startet ihn starten.command
// in Terminal und nicht launchd direkt.
//
// Protokoll, beide Richtungen: [1 Byte Typ][4 Byte Länge, Big Endian][Nutzlast]
//   Mac -> Handy: 0x01 Info-JSON {w,h}, 0x02 H.264-Bild (Annex B), 0x03 Zustand-JSON {tastatur,geheim,gesperrt}
//   Handy -> Mac: 0x10 Anmeldung-JSON {token}, 0x11 Eingabe-JSON {t:…}

import AppKit
import Carbon
import ApplicationServices
import CoreGraphics
import CoreMedia
import Foundation
import IOKit.pwr_mgt
import Network
import ScreenCaptureKit
import VideoToolbox

let PORT: UInt16 = UInt16(ProcessInfo.processInfo.environment["FERNSTEUERUNG_PORT"] ?? "") ?? 47800
let STREAM_BREITE_MAX = 1600
let BITRATE = 4_000_000
let BILDER_PRO_SEKUNDE: Int32 = 30

func log(_ s: String) {
    let f = DateFormatter(); f.dateFormat = "dd.MM. HH:mm:ss"
    FileHandle.standardError.write("[\(f.string(from: Date()))] \(s)\n".data(using: .utf8)!)
}

let token: String = {
    let pfad = NSHomeDirectory() + "/SK/Jarvis/fernsteuerung-token.txt"
    return (try? String(contentsOfFile: pfad, encoding: .utf8))?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
}()

// MARK: - Bildschirm

final class Bildschirm {
    private var virtuell: CGVirtualDisplay?
    private(set) var displayID: CGDirectDisplayID = CGMainDisplayID()

    static func aktiveAnzahl() -> UInt32 { var n: UInt32 = 0; CGGetActiveDisplayList(0, nil, &n); return n }

    /// Ohne aktiven Bildschirm (Deckel zu) einen virtuellen anlegen und behalten, solange der Dienst läuft.
    func sicherstellen() {
        if let v = virtuell { displayID = v.displayID; spiegeln(); return }
        if Bildschirm.aktiveAnzahl() > 0 { displayID = CGMainDisplayID(); spiegeln(); return }
        for versuch in 0..<5 {
            let d = CGVirtualDisplayDescriptor()
            d.queue = DispatchQueue.main
            d.name = "Jarvis-Fernsteuerung"
            d.maxPixelsWide = 3840; d.maxPixelsHigh = 2400
            d.sizeInMillimeters = CGSize(width: 330, height: 206)
            d.serialNum = UInt32(4711 + versuch); d.productID = 0x4A56; d.vendorID = 0x4A56
            d.terminationHandler = { _, _ in log("virtueller Bildschirm beendet") }
            guard let v = CGVirtualDisplay(descriptor: d) else { continue }
            let s = CGVirtualDisplaySettings(); s.hiDPI = 1
            s.modes = [CGVirtualDisplayMode(width: 1440, height: 900, refreshRate: 60)]
            if v.apply(s) {
                virtuell = v; displayID = v.displayID
                log("virtueller Bildschirm \(v.displayID) angelegt")
                zumHauptbildschirm(v.displayID)
                return
            }
        }
        log("virtueller Bildschirm ließ sich nicht anlegen")
        displayID = CGMainDisplayID()
    }

    /// Ohne eingebauten Bildschirm (Deckel zu) alle übrigen aktiven Bildschirme auf den gezeigten spiegeln.
    private func spiegeln() {
        var n: UInt32 = 0; CGGetActiveDisplayList(0, nil, &n)
        guard n > 1 else { return }
        var ids = [CGDirectDisplayID](repeating: 0, count: Int(n)); CGGetActiveDisplayList(n, &ids, &n)
        guard !ids.contains(where: { CGDisplayIsBuiltin($0) != 0 }) else { return }
        var cfg: CGDisplayConfigRef?
        guard CGBeginDisplayConfiguration(&cfg) == .success else { return }
        for id in ids where id != displayID && CGDisplayMirrorsDisplay(id) != displayID {
            CGConfigureDisplayMirrorOfDisplay(cfg, id, displayID)
        }
        CGCompleteDisplayConfiguration(cfg, .forSession)
        log("weitere Bildschirme gespiegelt: \(ids)")
    }

    private func zumHauptbildschirm(_ id: CGDirectDisplayID) {
        var cfg: CGDisplayConfigRef?
        guard CGBeginDisplayConfiguration(&cfg) == .success else { return }
        CGConfigureDisplayOrigin(cfg, id, 0, 0)
        CGCompleteDisplayConfiguration(cfg, .forSession)
    }

    var rahmen: CGRect { CGDisplayBounds(displayID) }
}

// MARK: - Energie: Bildschirm wecken und wach halten, solange jemand zusieht

final class Wachhalter {
    private var assertion: IOPMAssertionID = 0
    private var aktivitaet: IOPMAssertionID = 0
    func wecken() {
        IOPMAssertionDeclareUserActivity("Jarvis-Fernsteuerung" as CFString, kIOPMUserActiveLocal, &aktivitaet)
    }
    func an() {
        wecken()
        if assertion == 0 {
            IOPMAssertionCreateWithName(kIOPMAssertPreventUserIdleDisplaySleep as CFString,
                                        IOPMAssertionLevel(kIOPMAssertionLevelOn), "Jarvis-Fernsteuerung" as CFString, &assertion)
        }
    }
    func aus() {
        if assertion != 0 { IOPMAssertionRelease(assertion); assertion = 0 }
    }
}

// MARK: - H.264-Kodierer

final class Kodierer {
    private var sitzung: VTCompressionSession?
    private var breite = 0, hoehe = 0
    private var zaehler: Int64 = 0
    var schluesselbildErzwingen = true
    var ausgabe: ((Data) -> Void)?

    func kodiere(_ pixel: CVPixelBuffer) {
        let w = CVPixelBufferGetWidth(pixel), h = CVPixelBufferGetHeight(pixel)
        if sitzung == nil || w != breite || h != hoehe { anlegen(w, h) }
        guard let s = sitzung else { return }
        zaehler += 1
        let zeit = CMTime(value: zaehler, timescale: BILDER_PRO_SEKUNDE)
        var opts: CFDictionary?
        if schluesselbildErzwingen {
            opts = [kVTEncodeFrameOptionKey_ForceKeyFrame: true] as CFDictionary
            schluesselbildErzwingen = false
        }
        VTCompressionSessionEncodeFrame(s, imageBuffer: pixel, presentationTimeStamp: zeit, duration: .invalid,
                                        frameProperties: opts, infoFlagsOut: nil) { [weak self] status, _, sb in
            guard status == noErr, let sb, let self else { return }
            if let d = Kodierer.annexB(sb) { self.ausgabe?(d) }
        }
    }

    private func anlegen(_ w: Int, _ h: Int) {
        if let s = sitzung { VTCompressionSessionInvalidate(s) }
        breite = w; hoehe = h
        var s: VTCompressionSession?
        VTCompressionSessionCreate(allocator: nil, width: Int32(w), height: Int32(h), codecType: kCMVideoCodecType_H264,
                                   encoderSpecification: [kVTVideoEncoderSpecification_EnableLowLatencyRateControl: true] as CFDictionary,
                                   imageBufferAttributes: nil, compressedDataAllocator: nil, outputCallback: nil,
                                   refcon: nil, compressionSessionOut: &s)
        guard let s else { log("Kodierer ließ sich nicht anlegen"); return }
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_RealTime, value: kCFBooleanTrue)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_ProfileLevel, value: kVTProfileLevel_H264_High_AutoLevel)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_AllowFrameReordering, value: kCFBooleanFalse)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_AverageBitRate, value: BITRATE as CFNumber)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_MaxKeyFrameInterval, value: 600 as CFNumber)
        VTSessionSetProperty(s, key: kVTCompressionPropertyKey_ExpectedFrameRate, value: BILDER_PRO_SEKUNDE as CFNumber)
        VTCompressionSessionPrepareToEncodeFrames(s)
        sitzung = s
        schluesselbildErzwingen = true
        log("Kodierer \(w)x\(h)")
    }

    /// AVCC aus VideoToolbox in Annex B wandeln; Schlüsselbilder bekommen SPS und PPS vorangestellt.
    static func annexB(_ sb: CMSampleBuffer) -> Data? {
        guard let block = CMSampleBufferGetDataBuffer(sb), let format = CMSampleBufferGetFormatDescription(sb) else { return nil }
        let start: [UInt8] = [0, 0, 0, 1]
        var out = Data()
        var schluessel = true
        if let anh = CMSampleBufferGetSampleAttachmentsArray(sb, createIfNecessary: false) as? [[CFString: Any]],
           let erster = anh.first, let nicht = erster[kCMSampleAttachmentKey_NotSync] as? Bool, nicht { schluessel = false }
        if schluessel {
            var anzahl = 0
            CMVideoFormatDescriptionGetH264ParameterSetAtIndex(format, parameterSetIndex: 0, parameterSetPointerOut: nil,
                                                               parameterSetSizeOut: nil, parameterSetCountOut: &anzahl, nalUnitHeaderLengthOut: nil)
            for i in 0..<anzahl {
                var p: UnsafePointer<UInt8>?; var n = 0
                CMVideoFormatDescriptionGetH264ParameterSetAtIndex(format, parameterSetIndex: i, parameterSetPointerOut: &p,
                                                                   parameterSetSizeOut: &n, parameterSetCountOut: nil, nalUnitHeaderLengthOut: nil)
                if let p { out.append(contentsOf: start); out.append(p, count: n) }
            }
        }
        var laenge = 0; var zeiger: UnsafeMutablePointer<CChar>?
        guard CMBlockBufferGetDataPointer(block, atOffset: 0, lengthAtOffsetOut: nil, totalLengthOut: &laenge, dataPointerOut: &zeiger) == noErr,
              let zeiger else { return nil }
        var pos = 0
        while pos + 4 <= laenge {
            var nal: UInt32 = 0
            memcpy(&nal, zeiger + pos, 4)
            let n = Int(CFSwapInt32BigToHost(nal))
            pos += 4
            guard pos + n <= laenge else { break }
            out.append(contentsOf: start)
            zeiger.advanced(by: pos).withMemoryRebound(to: UInt8.self, capacity: n) { out.append($0, count: n) }
            pos += n
        }
        return out
    }
}

// MARK: - Aufnahme

final class Aufnahme: NSObject, SCStreamOutput, SCStreamDelegate {
    private var stream: SCStream?
    private let queue = DispatchQueue(label: "aufnahme")
    var bild: ((CVPixelBuffer) -> Void)?
    private(set) var groesse = CGSize(width: 1440, height: 900)
    private var letztesBild: CVPixelBuffer?
    private var nachschaerfen: DispatchWorkItem?
    private var statusZaehler = 0

    func start(display: CGDirectDisplayID, fertig: @escaping (Bool) -> Void) {
        SCShareableContent.getExcludingDesktopWindows(false, onScreenWindowsOnly: true) { inhalt, fehler in
            guard let inhalt, let ziel = inhalt.displays.first(where: { $0.displayID == display }) ?? inhalt.displays.first else {
                log("Aufnahme: kein Bildschirm (\(fehler?.localizedDescription ?? "-"))"); fertig(false); return
            }
            let rahmen = CGDisplayBounds(ziel.displayID)
            var w = Int(rahmen.width), h = Int(rahmen.height)
            if w > STREAM_BREITE_MAX { h = h * STREAM_BREITE_MAX / w; w = STREAM_BREITE_MAX }
            w &= ~1; h &= ~1
            self.groesse = CGSize(width: w, height: h)
            let cfg = SCStreamConfiguration()
            cfg.width = w; cfg.height = h
            cfg.pixelFormat = kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
            cfg.minimumFrameInterval = CMTime(value: 1, timescale: BILDER_PRO_SEKUNDE)
            cfg.queueDepth = 4
            cfg.showsCursor = true
            cfg.colorSpaceName = CGColorSpace.sRGB
            let filter = SCContentFilter(display: ziel, excludingWindows: [])
            let s = SCStream(filter: filter, configuration: cfg, delegate: self)
            do {
                try s.addStreamOutput(self, type: .screen, sampleHandlerQueue: self.queue)
            } catch { log("Aufnahme: \(error)"); fertig(false); return }
            s.startCapture { fehler in
                if let fehler { log("Aufnahme startet nicht: \(fehler.localizedDescription)"); fertig(false); return }
                self.stream = s
                log("Aufnahme läuft \(w)x\(h) von Bildschirm \(ziel.displayID)")
                fertig(true)
            }
        }
    }

    func stop() {
        stream?.stopCapture(completionHandler: nil); stream = nil
        letztesBild = nil
    }

    /// Letztes Bild erneut senden, damit der Kodierer es nach einer Bewegung scharf nachzieht oder ein neues
    /// Schlüsselbild bekommt.
    func wiederholen() {
        queue.async { if let p = self.letztesBild { self.bild?(p) } }
    }

    func stream(_ stream: SCStream, didOutputSampleBuffer sb: CMSampleBuffer, of type: SCStreamOutputType) {
        if type == .screen, let a = CMSampleBufferGetSampleAttachmentsArray(sb, createIfNecessary: false) as? [[SCStreamFrameInfo: Any]],
           let r = a.first?[.status] as? Int, statusZaehler < 5 { statusZaehler += 1; log("Bildstatus \(r)") }
        guard type == .screen,
              let anh = CMSampleBufferGetSampleAttachmentsArray(sb, createIfNecessary: false) as? [[SCStreamFrameInfo: Any]],
              let roh = anh.first?[.status] as? Int, let status = SCFrameStatus(rawValue: roh), status == .complete,
              let pixel = CMSampleBufferGetImageBuffer(sb) else { return }
        letztesBild = pixel
        bild?(pixel)
        nachschaerfen?.cancel()
        let w = DispatchWorkItem { [weak self] in self?.wiederholen() }
        nachschaerfen = w
        queue.asyncAfter(deadline: .now() + 0.4, execute: w)
    }

    func stream(_ stream: SCStream, didStopWithError error: Error) {
        log("Aufnahme gestoppt: \(error.localizedDescription)")
        self.stream = nil
    }
}

// MARK: - Eingaben

final class Eingabe {
    let bildschirm: Bildschirm
    private let quelle = CGEventSource(stateID: .hidSystemState)
    private var gedrueckt = false

    init(bildschirm: Bildschirm) { self.bildschirm = bildschirm }

    private func punkt(_ j: [String: Any]) -> CGPoint {
        let r = bildschirm.rahmen
        let x = min(max((j["x"] as? Double) ?? 0.5, 0), 0.9999), y = min(max((j["y"] as? Double) ?? 0.5, 0), 0.9999)
        return CGPoint(x: r.minX + x * r.width, y: r.minY + y * r.height)
    }

    private func maus(_ typ: CGEventType, _ p: CGPoint, _ knopf: CGMouseButton = .left, klicks: Int64 = 1) {
        let e = CGEvent(mouseEventSource: quelle, mouseType: typ, mouseCursorPosition: p, mouseButton: knopf)
        e?.setIntegerValueField(.mouseEventClickState, value: klicks)
        e?.post(tap: .cghidEventTap)
    }

    func ausfuehren(_ j: [String: Any]) {
        guard let t = j["t"] as? String else { return }
        switch t {
        case "bewegen":
            maus(gedrueckt ? .leftMouseDragged : .mouseMoved, punkt(j))
        case "klick":
            let p = punkt(j)
            let n = (j["n"] as? Int) ?? 1
            maus(.mouseMoved, p)
            if (j["doppel"] as? Bool) == true {
                // Zweiter Klick eines Doppelklicks: der erste kam schon beim ersten Tippen.
                maus(.leftMouseDown, p, klicks: 2); maus(.leftMouseUp, p, klicks: 2); return
            }
            for k in 1...max(1, n) {
                maus(.leftMouseDown, p, klicks: Int64(k))
                maus(.leftMouseUp, p, klicks: Int64(k))
            }
        case "rechtsklick":
            let p = punkt(j)
            maus(.mouseMoved, p)
            maus(.rightMouseDown, p, .right); maus(.rightMouseUp, p, .right)
        case "runter":
            let p = punkt(j); maus(.mouseMoved, p); maus(.leftMouseDown, p); gedrueckt = true
        case "ziehen":
            maus(.leftMouseDragged, punkt(j))
        case "hoch":
            maus(.leftMouseUp, punkt(j)); gedrueckt = false
        case "scrollen":
            let dx = Int32((j["dx"] as? Double) ?? 0), dy = Int32((j["dy"] as? Double) ?? 0)
            if j["x"] != nil { maus(.mouseMoved, punkt(j)) }
            CGEvent(scrollWheelEvent2Source: quelle, units: .pixel, wheelCount: 2, wheel1: dy, wheel2: dx, wheel3: 0)?
                .post(tap: .cghidEventTap)
        case "text":
            schreiben((j["s"] as? String) ?? "")
        case "taste":
            taste((j["k"] as? String) ?? "", mods: (j["m"] as? [String]) ?? [])
        case "entsperren":
            entsperren((j["s"] as? String) ?? "")
        default: break
        }
    }

    /// Zeichen -> (Tastencode, Umschalt, Wahl) aus der aktuellen Tastaturbelegung (z. B. Deutsch).
    private lazy var belegung: [Character: (CGKeyCode, Bool, Bool)] = {
        var m: [Character: (CGKeyCode, Bool, Bool)] = [:]
        guard let quelle = TISCopyCurrentKeyboardLayoutInputSource()?.takeRetainedValue(),
              let roh = TISGetInputSourceProperty(quelle, kTISPropertyUnicodeKeyLayoutData) else { return m }
        let daten = Unmanaged<CFData>.fromOpaque(roh).takeUnretainedValue() as Data
        daten.withUnsafeBytes { (zeiger: UnsafeRawBufferPointer) in
            guard let layout = zeiger.baseAddress?.assumingMemoryBound(to: UCKeyboardLayout.self) else { return }
            for (shift, alt) in [(false, false), (true, false), (false, true), (true, true)] {
                var mods: UInt32 = 0
                if shift { mods |= UInt32(shiftKey >> 8) }
                if alt { mods |= UInt32(optionKey >> 8) }
                for code in 0..<128 {
                    var tot: UInt32 = 0; var laenge = 0
                    var buf = [UniChar](repeating: 0, count: 4)
                    let st = UCKeyTranslate(layout, UInt16(code), UInt16(kUCKeyActionDown), mods, UInt32(LMGetKbdType()),
                                            OptionBits(kUCKeyTranslateNoDeadKeysBit), &tot, 4, &laenge, &buf)
                    guard st == noErr, laenge == 1, let z = String(utf16CodeUnits: buf, count: laenge).first else { continue }
                    if m[z] == nil, ![36, 48, 51, 53, 76].contains(code) { m[z] = (CGKeyCode(code), shift, alt) }
                }
            }
        }
        return m
    }()

    func schreiben(_ text: String) {
        for zeichen in text {
            if zeichen == "\n" { taste("enter", mods: []); continue }
            if zeichen == "\t" { taste("tab", mods: []); continue }
            if let (code, shift, alt) = belegung[zeichen] {
                var flags: CGEventFlags = []
                if shift { flags.insert(.maskShift) }
                if alt { flags.insert(.maskAlternate) }
                if shift { let e = CGEvent(keyboardEventSource: quelle, virtualKey: 56, keyDown: true); e?.flags = flags; e?.post(tap: .cghidEventTap) }
                if alt { let e = CGEvent(keyboardEventSource: quelle, virtualKey: 58, keyDown: true); e?.flags = flags; e?.post(tap: .cghidEventTap) }
                for runter in [true, false] {
                    let e = CGEvent(keyboardEventSource: quelle, virtualKey: code, keyDown: runter); e?.flags = flags; e?.post(tap: .cghidEventTap)
                }
                if alt { let e = CGEvent(keyboardEventSource: quelle, virtualKey: 58, keyDown: false); e?.flags = shift ? .maskShift : []; e?.post(tap: .cghidEventTap) }
                if shift { let e = CGEvent(keyboardEventSource: quelle, virtualKey: 56, keyDown: false); e?.flags = []; e?.post(tap: .cghidEventTap) }
                usleep(6000)
                continue
            }
            let u = Array(String(zeichen).utf16)
            for runter in [true, false] {
                let e = CGEvent(keyboardEventSource: quelle, virtualKey: 0, keyDown: runter)
                e?.flags = []
                e?.keyboardSetUnicodeString(stringLength: u.count, unicodeString: u)
                e?.post(tap: .cghidEventTap)
            }
            usleep(4000)
        }
    }

    static let tasten: [String: CGKeyCode] = [
        "enter": 36, "tab": 48, "leer": 49, "zurueck": 51, "esc": 53, "entf": 117,
        "links": 123, "rechts": 124, "runter": 125, "hoch": 126,
        "a": 0, "c": 8, "f": 3, "n": 45, "q": 12, "r": 15, "s": 1, "t": 17, "v": 9, "w": 13, "x": 7, "z": 6,
        "komma": 43, "h": 4, "m": 46,
    ]

    func taste(_ name: String, mods: [String]) {
        guard let code = Eingabe.tasten[name] else { return }
        var flags: CGEventFlags = []
        if mods.contains("cmd") { flags.insert(.maskCommand) }
        if mods.contains("shift") { flags.insert(.maskShift) }
        if mods.contains("alt") { flags.insert(.maskAlternate) }
        if mods.contains("ctrl") { flags.insert(.maskControl) }
        let modTasten: [(String, CGKeyCode)] = [("cmd", 55), ("shift", 56), ("alt", 58), ("ctrl", 59)]
        for (m, c) in modTasten where mods.contains(m) {
            let e = CGEvent(keyboardEventSource: quelle, virtualKey: c, keyDown: true); e?.flags = flags; e?.post(tap: .cghidEventTap)
        }
        for runter in [true, false] {
            let e = CGEvent(keyboardEventSource: quelle, virtualKey: code, keyDown: runter)
            e?.flags = flags
            e?.post(tap: .cghidEventTap)
        }
        for (m, c) in modTasten.reversed() where mods.contains(m) {
            let e = CGEvent(keyboardEventSource: quelle, virtualKey: c, keyDown: false); e?.flags = []; e?.post(tap: .cghidEventTap)
        }
    }

    /// Sperrbildschirm: wecken, Passwortfeld hervorholen, Passwort tippen, Enter.
    func entsperren(_ passwort: String) {
        DispatchQueue.global().async {
            self.taste("esc", mods: [])
            usleep(700_000)
            self.schreiben(passwort)
            usleep(100_000)
            self.taste("enter", mods: [])
        }
    }
}

// MARK: - Fokus- und Sperrzustand

final class Zustand {
    struct Werte: Equatable { var tastatur = false; var geheim = false; var gesperrt = false }
    private(set) var werte = Werte()
    var geaendert: ((Werte) -> Void)?
    private var timer: DispatchSourceTimer?

    static func gesperrt() -> Bool {
        guard let d = CGSessionCopyCurrentDictionary() as? [String: Any] else { return false }
        return (d["CGSSessionScreenIsLocked"] as? Int ?? 0) == 1
    }

    static func fokus() -> (Bool, Bool) {
        let system = AXUIElementCreateSystemWide()
        var wert: CFTypeRef?
        guard AXUIElementCopyAttributeValue(system, kAXFocusedUIElementAttribute as CFString, &wert) == .success,
              let el = wert, CFGetTypeID(el) == AXUIElementGetTypeID() else { return (false, false) }
        let e = el as! AXUIElement
        var rolle: CFTypeRef?, unter: CFTypeRef?
        AXUIElementCopyAttributeValue(e, kAXRoleAttribute as CFString, &rolle)
        AXUIElementCopyAttributeValue(e, kAXSubroleAttribute as CFString, &unter)
        let r = rolle as? String ?? "", s = unter as? String ?? ""
        let geheim = s == "AXSecureTextField"
        let text = geheim || ["AXTextField", "AXTextArea", "AXComboBox", "AXSearchField"].contains(r)
        return (text, geheim)
    }

    func start() {
        let t = DispatchSource.makeTimerSource(queue: .main)
        t.schedule(deadline: .now(), repeating: 0.35)
        t.setEventHandler { [weak self] in
            guard let self else { return }
            let (text, geheim) = Zustand.fokus()
            let neu = Werte(tastatur: text, geheim: geheim, gesperrt: Zustand.gesperrt())
            if neu != self.werte { self.werte = neu; self.geaendert?(neu) }
        }
        t.resume(); timer = t
    }
    func stop() { timer?.cancel(); timer = nil }
}

// MARK: - Verbindung zum Handy

final class Sitzung {
    let verbindung: NWConnection
    var angemeldet = false
    private var offen = 0
    private let sperre = NSLock()
    var beendet: (() -> Void)?
    var eingabe: (([String: Any]) -> Void)?
    var angemeldetCallback: (() -> Void)?

    init(_ v: NWConnection) { verbindung = v }

    func start() {
        verbindung.stateUpdateHandler = { [weak self] st in
            switch st {
            case .failed, .cancelled: self?.beendet?(); self?.beendet = nil
            default: break
            }
        }
        verbindung.start(queue: .main)
        lesen()
    }

    func stop() { verbindung.cancel() }

    private func lesen() {
        verbindung.receive(minimumIncompleteLength: 5, maximumLength: 5) { [weak self] kopf, _, ende, fehler in
            guard let self, let kopf, kopf.count == 5, fehler == nil else { self?.stop(); return }
            let typ = kopf[kopf.startIndex]
            let n = Int(kopf[kopf.startIndex + 1]) << 24 | Int(kopf[kopf.startIndex + 2]) << 16 | Int(kopf[kopf.startIndex + 3]) << 8 | Int(kopf[kopf.startIndex + 4])
            guard n >= 0, n < 1_000_000 else { self.stop(); return }
            if n == 0 { self.verarbeiten(typ, Data()); if !ende { self.lesen() }; return }
            self.verbindung.receive(minimumIncompleteLength: n, maximumLength: n) { daten, _, ende2, fehler2 in
                guard let daten, fehler2 == nil else { self.stop(); return }
                self.verarbeiten(typ, daten)
                if !ende2 { self.lesen() } else { self.stop() }
            }
        }
    }

    private func verarbeiten(_ typ: UInt8, _ daten: Data) {
        let json = (try? JSONSerialization.jsonObject(with: daten)) as? [String: Any] ?? [:]
        if typ == 0x10 {
            if !token.isEmpty, (json["token"] as? String) == token {
                angemeldet = true; angemeldetCallback?()
            } else { log("Anmeldung abgelehnt"); stop() }
        } else if typ == 0x11, angemeldet {
            eingabe?(json)
        }
    }

    var ausgelastet: Bool { sperre.lock(); defer { sperre.unlock() }; return offen > 3 }

    func senden(_ typ: UInt8, _ nutz: Data) {
        var d = Data([typ, UInt8(nutz.count >> 24 & 0xff), UInt8(nutz.count >> 16 & 0xff), UInt8(nutz.count >> 8 & 0xff), UInt8(nutz.count & 0xff)])
        d.append(nutz)
        sperre.lock(); offen += 1; sperre.unlock()
        verbindung.send(content: d, completion: .contentProcessed { [weak self] _ in
            guard let self else { return }
            self.sperre.lock(); self.offen -= 1; self.sperre.unlock()
        })
    }

    func sendenJSON(_ typ: UInt8, _ obj: [String: Any]) {
        if let d = try? JSONSerialization.data(withJSONObject: obj) { senden(typ, d) }
    }
}

// MARK: - Dienst

final class Dienst {
    let bildschirm = Bildschirm()
    let wach = Wachhalter()
    let kodierer = Kodierer()
    let aufnahme = Aufnahme()
    let zustand = Zustand()
    lazy var eingabe = Eingabe(bildschirm: bildschirm)
    var sitzung: Sitzung?
    var listener: NWListener?
    var verworfen = 0
    var warGesperrt = Zustand.gesperrt()

    func aufnahmeStarten() {
        aufnahme.stop()
        kodierer.schluesselbildErzwingen = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.aufnahme.start(display: self.bildschirm.displayID) { ok in
                DispatchQueue.main.async {
                    let g = self.aufnahme.groesse
                    self.sitzung?.sendenJSON(0x01, ["w": Int(g.width), "h": Int(g.height), "ok": ok])
                    self.kodierer.schluesselbildErzwingen = true
                    self.aufnahme.wiederholen()
                }
            }
        }
    }

    func start() {
        guard !token.isEmpty else { log("Kein Token in ~/SK/Jarvis/fernsteuerung-token.txt"); exit(1) }
        log("Bedienungshilfen erlaubt: \(AXIsProcessTrusted()), Bildschirmaufnahme erlaubt: \(CGPreflightScreenCaptureAccess())")
        let params = NWParameters.tcp
        (params.defaultProtocolStack.transportProtocol as? NWProtocolTCP.Options)?.noDelay = true
        params.allowLocalEndpointReuse = true
        do { listener = try NWListener(using: params, on: NWEndpoint.Port(rawValue: PORT)!) } catch {
            log("Port \(PORT) belegt: \(error)"); exit(1)
        }
        listener?.newConnectionHandler = { [weak self] v in self?.neu(v) }
        listener?.stateUpdateHandler = { st in log("Listener: \(st)") }
        listener?.start(queue: .main)

        kodierer.ausgabe = { [weak self] d in DispatchQueue.main.async { self?.sitzung?.senden(0x02, d) } }
        aufnahme.bild = { [weak self] p in
            guard let self else { return }
            guard let s = self.sitzung, s.angemeldet else { return }
            if s.ausgelastet { self.verworfen += 1; return }
            self.kodierer.kodiere(p)
        }
        zustand.geaendert = { [weak self] w in
            guard let self else { return }
            self.sitzung?.sendenJSON(0x03, ["tastatur": w.tastatur, "geheim": w.geheim, "gesperrt": w.gesperrt])
            // Nach dem Entsperren liefert eine im gesperrten Zustand gestartete Aufnahme oft nichts: neu starten.
            if self.warGesperrt && !w.gesperrt, self.sitzung != nil {
                log("entsperrt, Aufnahme neu")
                self.bildschirm.sicherstellen()
                self.aufnahmeStarten()
            }
            self.warGesperrt = w.gesperrt
        }
    }

    private func neu(_ v: NWConnection) {
        log("Verbindung von \(v.endpoint)")
        sitzung?.stop()
        let s = Sitzung(v)
        sitzung = s
        s.angemeldetCallback = { [weak self, weak s] in
            guard let self, let s else { return }
            log("angemeldet")
            self.wach.an()
            self.bildschirm.sicherstellen()
            self.warGesperrt = Zustand.gesperrt()
            let w = self.zustand.werte
            s.sendenJSON(0x03, ["tastatur": w.tastatur, "geheim": w.geheim, "gesperrt": self.warGesperrt])
            self.aufnahmeStarten()
            self.zustand.start()
        }
        s.eingabe = { [weak self] j in
            guard let self else { return }
            self.wach.wecken()
            if (j["t"] as? String) == "schluesselbild" {
                self.kodierer.schluesselbildErzwingen = true; self.aufnahme.wiederholen(); return
            }
            self.eingabe.ausfuehren(j)
        }
        s.beendet = { [weak self, weak s] in
            guard let self, self.sitzung === s else { return }
            log("Verbindung beendet")
            self.sitzung = nil
            self.aufnahme.stop()
            self.zustand.stop()
            self.wach.aus()
        }
        s.start()
    }
}

setvbuf(stdout, nil, _IONBF, 0)
let dienst = Dienst()
dienst.start()
log("Jarvis-Fernsteuerung lauscht auf Port \(PORT)")
RunLoop.main.run()
