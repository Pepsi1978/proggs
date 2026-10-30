import AppKit
import LocalAuthentication
import SwiftUI

/// Sichtbar sind Schlüssel nur nach Touch ID, höchstens eine Minute. Gesperrt wird außerdem beim Verlassen der Seite,
/// beim Schließen des Fensters oder der App, beim Ruhezustand und beim Bildschirm-Sperren.
@MainActor @Observable
final class TresorSperre {
    static let shared = TresorSperre()
    static let sekunden = 60
    private(set) var entsperrt = false
    private(set) var rest = 0
    private(set) var meldung = ""
    private var uhr: Task<Void, Never>?
    private var beobachtet = false

    func beobachten() {
        guard !beobachtet else { return }
        beobachtet = true
        let sperren: @Sendable (Notification) -> Void = { _ in Task { @MainActor in TresorSperre.shared.sperren() } }
        NotificationCenter.default.addObserver(forName: NSWindow.willCloseNotification, object: nil, queue: .main) { n in
            let hauptfenster = MainActor.assumeIsolated { (n.object as? NSWindow)?.canBecomeMain == true }
            if hauptfenster { Task { @MainActor in TresorSperre.shared.sperren() } }
        }
        NSWorkspace.shared.notificationCenter.addObserver(forName: NSWorkspace.willSleepNotification, object: nil, queue: .main, using: sperren)
        DistributedNotificationCenter.default().addObserver(forName: Notification.Name("com.apple.screenIsLocked"), object: nil, queue: .main, using: sperren)
    }

    func entsperren() async {
        let k = LAContext()
        k.localizedCancelTitle = "Abbrechen"
        // Touch ID. Nur wenn sie gerade nicht geht (Deckel zu), fragt macOS das Passwort des Macs ab.
        var f: NSError?
        let regel: LAPolicy = k.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &f) ? .deviceOwnerAuthenticationWithBiometrics : .deviceOwnerAuthentication
        do {
            guard try await k.evaluatePolicy(regel, localizedReason: "die API-Schlüssel anzuzeigen") else { return }
            entsperrt = true; rest = Self.sekunden; meldung = ""
            log("tresor", "Tresor entsperrt", wo: "Einstellungen")
            uhr?.cancel()
            uhr = Task { [weak self] in
                while let self, self.rest > 0 {
                    try? await Task.sleep(for: .seconds(1))
                    if Task.isCancelled { return }
                    self.rest -= 1
                }
                self?.sperren()
            }
        } catch {
            meldung = "Nicht entsperrt: \(error.localizedDescription)"
            log("tresor", "Entsperren abgelehnt", wo: "Einstellungen", fehler: error.localizedDescription)
        }
    }

    func sperren() {
        uhr?.cancel(); uhr = nil
        // Direkt, nicht über einen neuen Task: Beim Beenden der App liefe der nicht mehr.
        if entsperrt { Logbuch.shared.schreib("tresor", "Tresor gesperrt", wo: "Einstellungen") }
        entsperrt = false; rest = 0
    }
}

struct TresorKopf: View {
    @State private var sperre = TresorSperre.shared
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 10) {
                Image(systemName: sperre.entsperrt ? "lock.open.fill" : "lock.fill").foregroundStyle(sperre.entsperrt ? Farbe.orange : Farbe.gruen)
                Text(sperre.entsperrt ? "Tresor offen, sperrt in \(sperre.rest) s" : "Tresor gesperrt").font(.jarvis(13, .semibold))
                Spacer()
                if sperre.entsperrt {
                    KapselKnopf(symbol: "lock.fill", titel: "Sperren") { sperre.sperren() }
                } else {
                    KapselKnopf(symbol: "touchid", titel: "Mit Touch ID anzeigen", hervorgehoben: true) { Task { await sperre.entsperren() } }
                }
            }
            if !sperre.meldung.isEmpty { Text(sperre.meldung).font(.jarvis(11)).foregroundStyle(Farbe.orange) }
            if let s = Tresor.stoerung { Text(s).font(.jarvis(11)).foregroundStyle(Farbe.rot) }
        }
        .padding(.vertical, 4)
        .onDisappear { sperre.sperren() }
    }
}

/// Gesperrt: nur Punkte, der Wert selbst erreicht die Oberfläche nicht. Entsperrt: lesbar und änderbar.
struct TresorFeld: View {
    let titel: String
    @Binding var wert: String
    @State private var sperre = TresorSperre.shared
    var body: some View {
        HStack {
            Image(systemName: wert.isEmpty ? "xmark.circle" : "checkmark.circle.fill")
                .foregroundStyle(wert.isEmpty ? Farbe.text3 : Farbe.gruen)
            Text(titel).font(.jarvis(13, .medium)).frame(width: 150, alignment: .leading)
            if sperre.entsperrt {
                TextField("", text: $wert).textFieldStyle(.roundedBorder).font(.system(size: 12, design: .monospaced))
            } else {
                Text(wert.isEmpty ? "nicht eingetragen" : String(repeating: "•", count: 16))
                    .font(.jarvis(13)).foregroundStyle(wert.isEmpty ? Farbe.text3 : Farbe.text2).lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 8).padding(.vertical, 5)
                    .background(RoundedRectangle(cornerRadius: 6).fill(Color.white.opacity(0.05)))
                    .contentShape(Rectangle())
                    .onTapGesture { Task { await sperre.entsperren() } }
                    .help("Antippen und mit Touch ID entsperren")
            }
        }
    }
}
