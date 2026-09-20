import Foundation

/// Versionsvergleich. Unter Windows steckte er als `Vergleiche` im CliAktualisierer; auf dem Mac
/// braucht ihn auch der brew-Anbieter, deshalb steht er hier an einer Stelle.
///
/// Warum das hier mehr Gewicht hat als unter Windows: winget liefert fuer ein Paket genau eine
/// "verfuegbare" Version und meldet nur dann etwas, wenn wirklich ein Upgrade ansteht. Ein
/// Homebrew-Cask dagegen ist eine unabhaengige Rezeptdatei -- sie kann dem eingebauten Updater
/// einer App auch HINTERHERLAUFEN. Ein reiner Ungleich-Vergleich wuerde dann ein Downgrade als
/// "Update verfuegbar" anbieten. Deshalb gilt hier: nur echte, numerisch groessere Versionen sind
/// ein Update.
enum Versionen {

    /// Findet die erste versionsartige Zeichenfolge in einem Text ("2.1.278", "0.4.24+1").
    private static let muster = try! NSRegularExpression(pattern: "\\d+\\.\\d+(\\.\\d+)?[\\w.+-]*")

    static func ausText(_ text: String) -> String {
        let bereich = NSRange(text.startIndex..., in: text)
        guard let treffer = muster.firstMatch(in: text, options: [], range: bereich),
              let spanne = Range(treffer.range, in: text) else { return "" }
        return String(text[spanne])
    }

    /// Bereinigt eine Cask-Version fuer den Vergleich.
    ///
    /// Homebrew haengt hinter ein Komma einen zweiten Bestandteil: bei Claude etwa
    /// `2.2553.1,c38127e27202ddc1c8c187102f7798a93b1b8ede` -- eine Commit-Kennung, bei LM Studio
    /// `0.4.25,1` eine Build-Nummer. Die Info.plist der installierten App kennt diesen Teil nicht.
    /// Ohne das Abschneiden waere jede dieser Apps dauerhaft als "Update verfuegbar" markiert,
    /// obwohl exakt dieselbe Fassung installiert ist.
    static func caskBereinigen(_ version: String) -> String {
        guard let komma = version.firstIndex(of: ",") else { return version.trimmed }
        return String(version[version.startIndex..<komma]).trimmed
    }

    /// Numerischer Vergleich nach Bestandteilen. > 0 heisst: `a` ist neuer als `b`.
    static func vergleiche(_ a: String, _ b: String) -> Int {
        if a.istLeer { return -1 }
        if b.istLeer { return 1 }

        let links = teile(a)
        let rechts = teile(b)
        for i in 0..<max(links.count, rechts.count) {
            let l = i < links.count ? links[i] : 0
            let r = i < rechts.count ? rechts[i] : 0
            if l != r { return l < r ? -1 : 1 }
        }
        return 0
    }

    /// Ist `verfuegbar` echt neuer als `installiert`? Der Downgrade-Schutz in einer Zeile.
    static func istNeuer(_ verfuegbar: String, als installiert: String) -> Bool {
        vergleiche(verfuegbar, installiert) > 0
    }

    private static func teile(_ version: String) -> [Int] {
        version
            .split(whereSeparator: { $0 == "." || $0 == "+" || $0 == "-" || $0 == "," })
            .map { stueck in
                let ziffern = stueck.prefix { $0.isNumber }
                return Int(ziffern) ?? 0
            }
    }
}
