import Foundation

/// Sammelt die Protokollzeilen eines Laufs und reicht sie zur Anzeige weiter.
/// Gegenstueck zu `IProgress<string>` aus der Windows-Fassung.
final class Fortschritt: @unchecked Sendable {
    private let melden: @Sendable (String) -> Void

    init(_ melden: @escaping @Sendable (String) -> Void) { self.melden = melden }

    func berichte(_ zeile: String) {
        guard !zeile.istLeer else { return }
        melden(zeile)
    }
}

/// Eine Umsetzung je Update-Mechanismus. 1:1-Port von Providers/IAktualisierer.cs.
///
/// Ein neues Programm in programs.json braucht nur einen vorhandenen `art`-Wert -- Code-Aenderungen
/// bleiben wirklich neuen Mechanismen vorbehalten.
protocol Aktualisierer: Sendable {
    /// Der Wert des Katalogfeldes "art", den diese Umsetzung bedient.
    var art: String { get }

    func pruefen(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis

    func aktualisieren(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis

    /// Was sich geaendert haben MUSS, wenn ein Update wirklich angekommen ist -- vor und nach dem
    /// Lauf gelesen und dann verglichen. Ein Exit-Code 0 ist nur die Behauptung des Installers;
    /// das hier ist der Beweis.
    ///
    /// Was das ist, haengt vom Mechanismus ab: die Version, wo es eine gibt; die Schreibzeit der
    /// gebauten Programmdatei bei den Repo-Skripten; die Liste der offenen Pakete bei den
    /// LM-Studio-Runtimes. Eine leere Rueckgabe heisst "nicht feststellbar" -- der Lauf gilt dann
    /// als nicht verifiziert statt als Erfolg.
    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String
}
