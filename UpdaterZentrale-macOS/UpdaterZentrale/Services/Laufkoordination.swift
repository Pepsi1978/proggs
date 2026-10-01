import Foundation

/// Ein externer Vorgang zur Zeit, app-weit. Port von Services/Laufkoordination.cs.
///
/// Die Belegt-Merker je Karte verhinderten nur, dass EINE Karte doppelt lief; eine Pruefung von
/// Karte A konnte trotzdem neben einem Update von Karte B oder neben "Alle prüfen" laufen -- zwei
/// brew-, git- oder Skript-Laeufe, oder zwei Installer, gleichzeitig auf dem Rechner.
///
/// Der Zustand ist der aktuelle Besitzer selbst. Weil Besitz eine Referenz ist und nicht nur eine
/// Art, kann ein alter Besitz derselben Art sich weder als aktueller ausgeben noch ihn freigeben.
/// Ein Sammellauf haelt den Besitz fuer seine ganze Dauer und reicht ihn an die Kartenvorgaenge
/// weiter, die er antreibt; die pruefen ihn, bevor sie laufen.
@MainActor
final class Laufkoordination {
    private var besitzer: Laufbesitz?

    /// Nach jedem Belegen und Freigeben, damit die Oberflaeche ihre Schaltflaechen neu bewertet.
    var beiAenderung: [() -> Void] = []

    var belegt: Bool { besitzer != nil }
    var sammelLaeuft: Bool { besitzer?.istSammel == true }

    /// nil, wenn schon ein anderer Vorgang laeuft -- der Aufrufer darf dann nicht starten.
    func einzelBeginnen() -> Laufbesitz? { beginnen(istSammel: false) }

    /// nil, wenn irgendein Vorgang (einzeln oder gesammelt) laeuft.
    func sammelBeginnen() -> Laufbesitz? { beginnen(istSammel: true) }

    /// Wahr nur fuer den Sammelbesitz, der GERADE diese Koordination haelt.
    func istAktiverSammelbesitz(_ besitz: Laufbesitz?) -> Bool {
        guard let besitz, besitz.istSammel else { return false }
        return besitzer === besitz
    }

    private func beginnen(istSammel: Bool) -> Laufbesitz? {
        guard besitzer == nil else { return nil }
        let neu = Laufbesitz(self, istSammel: istSammel)
        besitzer = neu
        melden()
        return neu
    }

    fileprivate func freigeben(_ besitz: Laufbesitz) {
        // Gibt nur frei, wenn genau dieser Besitz der Besitzer ist.
        guard besitzer === besitz else { return }
        besitzer = nil
        melden()
    }

    private func melden() { for rueckruf in beiAenderung { rueckruf() } }
}

/// Nachweis des Besitzes. `freigeben()` gibt genau einmal frei, egal wie oft es aufgerufen wird.
@MainActor
final class Laufbesitz {
    private weak var koordination: Laufkoordination?
    private var freigegeben = false
    let istSammel: Bool

    fileprivate init(_ koordination: Laufkoordination, istSammel: Bool) {
        self.koordination = koordination
        self.istSammel = istSammel
    }

    var art: String { istSammel ? "Sammel" : "Einzel" }

    func freigeben() {
        guard !freigegeben else { return }
        freigegeben = true
        koordination?.freigeben(self)
    }
}
