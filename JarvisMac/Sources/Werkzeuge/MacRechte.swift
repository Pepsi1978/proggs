import AVFoundation
import ApplicationServices
import Foundation
import Speech
import SwiftUI
import UserNotifications

/// Der echte Stand der Rechte am Mac, für Einstellungen → Mac & Rechte und das Werkzeug mac_rechte. Stellt nichts um; nur „Fehlende anfragen“ löst die Abfragen von macOS aus.
@MainActor
enum MacRechte {
    enum Zustand {
        case ja, nein, offen, unbekannt
        var text: String {
            switch self {
            case .ja: return "vorhanden"
            case .nein: return "fehlt"
            case .offen: return "noch nicht gefragt"
            case .unbekannt: return "von macOS nicht abfragbar"
            }
        }
    }

    struct Zeile: Identifiable {
        var id: String { titel }
        let titel: String
        let zustand: Zustand
        let text: String
        /// Bereich der Systemeinstellungen; nil = nichts zu öffnen.
        var bereich: String? = nil
    }

    /// Programme, die Jarvis über Apple Events steuert.
    private nonisolated static let automation = [("com.apple.systemevents", "System Events"), ("com.apple.Terminal", "Terminal"), ("com.apple.finder", "Finder")]

    private nonisolated static func automationStatus(_ bundle: String, fragen: Bool) -> OSStatus {
        var ziel = AEAddressDesc()
        let daten = Array(bundle.utf8)
        guard AECreateDesc(DescType(typeApplicationBundleID), daten, daten.count, &ziel) == noErr else { return OSStatus(procNotFound) }
        defer { AEDisposeDesc(&ziel) }
        return AEDeterminePermissionToAutomateTarget(&ziel, AEEventClass(typeWildCard), AEEventID(typeWildCard), fragen)
    }

    private nonisolated static func vollerDiskzugriff() -> Bool {
        // Diese Datei gibt macOS nur mit Festplattenvollzugriff heraus.
        let pfad = NSHomeDirectory() + "/Library/Application Support/com.apple.TCC/TCC.db"
        guard let h = FileHandle(forReadingAtPath: pfad) else { return false }
        try? h.close()
        return true
    }

    private static func regelOhneAbfrage(_ recht: String) async -> Bool {
        let aus = await Shell.zsh("/usr/bin/security authorizationdb read \(recht) 2>/dev/null", sekunden: 10).ausgabe
        guard let d = aus.data(using: .utf8), let o = try? PropertyListSerialization.propertyList(from: d, format: nil) as? [String: Any] else { return false }
        if (o["class"] as? String) == "allow" { return true }
        if (o["rule"] as? String) == "allow" { return true }
        return (o["rule"] as? [String])?.contains("allow") == true
    }

    static func stand() async -> [Zeile] {
        let mikrofon: Zustand = switch AVCaptureDevice.authorizationStatus(for: .audio) {
        case .authorized: .ja
        case .notDetermined: .offen
        default: .nein
        }
        let sprache: Zustand = switch SFSpeechRecognizer.authorizationStatus() {
        case .authorized: .ja
        case .notDetermined: .offen
        default: .nein
        }
        let auto = await Task.detached { automation.map { ($0.1, automationStatus($0.0, fragen: false)) } }.value
        // Läuft ein Programm gerade nicht, sagt macOS nichts dazu: Dann ist der Stand nicht bekannt, „Fehlende anfragen“ startet es dafür.
        let autoZustand: Zustand = auto.contains { $0.1 == OSStatus(errAEEventNotPermitted) } ? .nein
            : auto.contains { $0.1 == OSStatus(errAEEventWouldRequireUserConsent) } ? .offen
            : auto.allSatisfy { $0.1 == noErr } ? .ja : .unbekannt
        let mitteilung = await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
        let diskzugriff = await Task.detached { vollerDiskzugriff() }.value
        // -k: Eine eben erst eingegebene Anmeldung zählt nicht als dauerhaftes Recht.
        async let sudoLauf = Shell.zsh("/usr/bin/sudo -k -n true 2>/dev/null", sekunden: 5)
        async let installierenLauf = regelOhneAbfrage("system.install.software")
        async let adminLauf = regelOhneAbfrage("system.privilege.admin")
        let (sudoErgebnis, installieren, admin) = await (sudoLauf, installierenLauf, adminLauf)
        let sudo = sudoErgebnis.code == 0
        return [
            Zeile(titel: "Bedienungshilfen", zustand: AXIsProcessTrusted() ? .ja : .nein, text: "Klicken, tippen, Dialoge lesen und bestätigen.", bereich: "Privacy_Accessibility"),
            Zeile(titel: "Bildschirmaufnahme", zustand: CGPreflightScreenCaptureAccess() ? .ja : .nein, text: "Den Bildschirm ansehen.", bereich: "Privacy_ScreenCapture"),
            Zeile(titel: "Automation", zustand: autoZustand, text: "Steuert " + auto.map { "\($0.0)\($0.1 == noErr ? " ✓" : $0.1 == OSStatus(procNotFound) ? " (läuft nicht, ungeprüft)" : "")" }.joined(separator: ", ") + ".", bereich: "Privacy_Automation"),
            Zeile(titel: "Festplattenvollzugriff", zustand: diskzugriff ? .ja : .nein, text: "Geschützte Ordner lesen. Kann nur Frank in den Systemeinstellungen einschalten.", bereich: "Privacy_AllFiles"),
            Zeile(titel: "Programme verwalten", zustand: .unbekannt, text: "Programme in /Applications ändern und ersetzen. Kann nur Frank in den Systemeinstellungen einschalten.", bereich: "Privacy_AppBundles"),
            Zeile(titel: "Mikrofon", zustand: mikrofon, text: "Weckwort, Diktat und Live.", bereich: "Privacy_Microphone"),
            Zeile(titel: "Spracherkennung", zustand: sprache, text: "Erkennung direkt auf dem Mac.", bereich: "Privacy_SpeechRecognition"),
            Zeile(titel: "Mitteilungen", zustand: mitteilung == .authorized || mitteilung == .provisional ? .ja : mitteilung == .notDetermined ? .offen : .nein, text: "Frank Bescheid sagen, wenn eine Freigabe nötig ist."),
            Zeile(titel: "sudo ohne Passwort", zustand: sudo ? .ja : .nein, text: sudo ? "Befehle mit sudo laufen ohne Abfrage." : "Befehle mit sudo fragen nach dem Passwort; Jarvis gibt es nie selbst ein."),
            Zeile(titel: "Softwareinstallation ohne Passwort", zustand: installieren ? .ja : .nein, text: installieren ? "Installationen zeigen kein Admin-Fenster." : "Installationen zeigen ein Admin-Fenster. Das bestätigt Frank selbst, danach arbeitet Jarvis weiter."),
            Zeile(titel: "Admin-Fenster ohne Passwort", zustand: admin ? .ja : .nein, text: admin ? "Admin-Abfragen kommen nicht." : "Admin-Abfragen kommen und bleiben bei Frank."),
        ]
    }

    /// Löst alle Abfragen aus, die macOS erlaubt. Festplattenvollzugriff und Programme verwalten gehen nur von Hand.
    static func fehlendeAnfragen() async {
        _ = await AVCaptureDevice.requestAccess(for: .audio)
        if SFSpeechRecognizer.authorizationStatus() == .notDetermined {
            await withCheckedContinuation { (k: CheckedContinuation<Void, Never>) in SFSpeechRecognizer.requestAuthorization { _ in k.resume() } }
        }
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound])
        if !AXIsProcessTrusted() { _ = AXIsProcessTrustedWithOptions(["AXTrustedCheckOptionPrompt": true] as CFDictionary) }
        if !CGPreflightScreenCaptureAccess() { _ = CGRequestScreenCaptureAccess() }
        // macOS fragt nur für laufende Programme: im Hintergrund starten, ohne Fenster nach vorn zu holen.
        for a in automation { _ = await Shell.zsh("/usr/bin/open -gj -b \(a.0)", sekunden: 10) }
        try? await Task.sleep(for: .seconds(1))
        await Task.detached { for a in automation { _ = automationStatus(a.0, fragen: true) } }.value
    }
}

/// Einstellungen → Mac & Rechte: was da ist, was fehlt, was Frank selbst freigeben muss.
struct MacRechteAnsicht: View {
    @State private var einst = Einstellungen.shared
    @State private var zeilen: [MacRechte.Zeile] = []
    @State private var fragt = false

    var body: some View {
        VStack(spacing: 18) {
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "bolt.shield.fill", titel: "Vollzugriff", farbe: Farbe.warnung)
                    HStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Jarvis darf am Mac alles").font(.jarvis(14, .semibold))
                            Text("Befehle, Programme, Dateien, Tastatur, Bildschirm, Terminal und normale Bestätigungsdialoge eines Auftrags. Aus = nur Programme und Adressen öffnen, Mitteilungen, Status.")
                                .font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                        }
                        Spacer(minLength: 20)
                        Toggle("", isOn: $einst.vollzugriff).toggleStyle(.switch)
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        KartenTitel(symbol: "checkmark.shield.fill", titel: "Rechte von macOS", farbe: Farbe.gruen)
                        Spacer()
                        KapselKnopf(symbol: "hand.raised.fill", titel: fragt ? "Fragt …" : "Fehlende anfragen", hervorgehoben: true) {
                            guard !fragt else { return }
                            fragt = true
                            Task { await MacRechte.fehlendeAnfragen(); zeilen = await MacRechte.stand(); fragt = false }
                        }
                        RundKnopf(symbol: "arrow.clockwise", hilfe: "Neu prüfen", groesse: 30) { Task { zeilen = await MacRechte.stand() } }
                    }
                    if zeilen.isEmpty { ProgressView().controlSize(.small) }
                    ForEach(zeilen) { z in
                        HStack(spacing: 12) {
                            Image(systemName: z.zustand == .ja ? "checkmark.circle.fill" : z.zustand == .unbekannt ? "questionmark.circle" : "exclamationmark.circle.fill")
                                .foregroundStyle(z.zustand == .ja ? Farbe.gruen : z.zustand == .unbekannt ? Farbe.text3 : Farbe.warnung)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(z.titel).font(.jarvis(13, .semibold))
                                Text(z.text).font(.jarvis(11)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                            }
                            Spacer()
                            Text(z.zustand.text).font(.jarvis(11, .medium)).foregroundStyle(Farbe.text3)
                            if let b = z.bereich { Button("Öffnen") { EinstellungenAnsicht.oeffne(b) } }
                        }
                        .padding(.vertical, 3)
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 8) {
                    KartenTitel(symbol: "person.badge.shield.checkmark.fill", titel: "Das bleibt bei dir", farbe: Farbe.gold)
                    Text("Passwörter und Zugangsdaten gibt Jarvis nie ein und speichert sie nie. Anmeldung, Touch ID und Datenschutz-Freigaben von macOS bestätigst du selbst: Jarvis sagt dir dann, was fehlt, und arbeitet danach weiter. Normale Dialoge eines Auftrags, etwa das Ja zu einem beauftragten OpenLauncher-Update, bestätigt Jarvis selbst.")
                        .font(.jarvis(12)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .task { zeilen = await MacRechte.stand() }
    }
}
