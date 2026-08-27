import AppKit
import Carbon
import Foundation

/// Globale Kurzbefehle, die einen gespeicherten Prompt direkt ins Terminal einfuegen.
///
/// Portierung von TerminalVoiceOverlay-Windows/Services/HotkeyRegistry.cs. Dort
/// halten zwei Tabellen die Zuordnung, die der Tastatur-Hook bei jedem Tastendruck
/// nachschlaegt:
///   * **Zahlen** — Windows: Strg+1..9, macOS: Cmd+1..9
///   * **Buchstaben** — Windows: Win+Alt+A..Z, macOS: Cmd+Opt+A..Z
///
/// Auf macOS existierten die Datenfelder (`hotkeyNumber`, `hotkeyLetter`) samt
/// Spalten in der Datenbank und der Zuweisung im Promptboard bereits — die
/// Kurzbefehle taten aber schlicht nichts, weil niemand sie beim System
/// registrierte. Dieses Modul schliesst genau diese Luecke.
///
/// Anders als unter Windows braucht macOS pro Kombination eine eigene
/// Registrierung bei Carbon. Die 9 Zahlen und 26 Buchstaben werden deshalb EINMAL
/// dauerhaft registriert; welche Taste dann welchen Prompt einfuegt, entscheidet
/// die Tabelle zur Laufzeit. So muss beim Bearbeiten eines Prompts nichts beim
/// System ab- und neu angemeldet werden.
final class PromptHotkeyRegistry {

    static let shared = PromptHotkeyRegistry()

    /// Ein Eintrag: die Prompt-ID (um die Quellzeile wiederzufinden) und der Text,
    /// der beim Ausloesen eingefuegt wird. 1:1 Windows `Entry`.
    struct Entry {
        let promptID: UUID
        let effectiveText: String
    }

    /// Wird vom AppDelegate gesetzt: fuegt den Text ins Ziel ein.
    var insertHandler: ((String) -> Void)?

    private var numberMap: [Int: Entry] = [:]
    private var letterMap: [Character: Entry] = [:]
    private let lock = NSLock()
    private var installed = false

    private init() {}

    // MARK: - Tabellen (Schreiber: das Promptboard nach jedem Rendern)

    /// Ersetzt die Zahlen-Tabelle vollstaendig (Windows: `Replace`).
    func replaceNumbers(_ entries: [Int: Entry]) {
        lock.lock()
        numberMap = entries
        lock.unlock()
        DiagLog.write("Hotkey", "numbers_replaced", [("count", entries.count)])
    }

    /// Ersetzt die Buchstaben-Tabelle vollstaendig (Windows: `ReplaceLetters`).
    func replaceLetters(_ entries: [Character: Entry]) {
        lock.lock()
        letterMap = entries
        lock.unlock()
        DiagLog.write("Hotkey", "letters_replaced", [("count", entries.count)])
    }

    /// Baut beide Tabellen aus der Prompt-Liste neu auf. Wird nach jeder
    /// Aenderung im Promptboard gerufen, damit die Zuordnung immer dem
    /// gespeicherten Stand entspricht.
    func rebuild(from prompts: [PBPrompt]) {
        var numbers: [Int: Entry] = [:]
        var letters: [Character: Entry] = [:]
        for prompt in prompts {
            let text = prompt.effectiveText.trimmingCharacters(in: .whitespacesAndNewlines)
            if text.isEmpty { continue }
            if let number = prompt.hotkeyNumber, (1...9).contains(number) {
                numbers[number] = Entry(promptID: prompt.id, effectiveText: prompt.effectiveText)
            }
            if let raw = prompt.hotkeyLetter?.uppercased(), let letter = raw.first,
               letter.isLetter, raw.count == 1 {
                letters[letter] = Entry(promptID: prompt.id, effectiveText: prompt.effectiveText)
            }
        }
        replaceNumbers(numbers)
        replaceLetters(letters)
    }

    func clear() {
        lock.lock()
        numberMap.removeAll()
        letterMap.removeAll()
        lock.unlock()
    }

    // MARK: - Registrierung beim System

    /// Meldet Cmd+1..9 und Cmd+Opt+A..Z EINMAL bei Carbon an. Mehrfachaufrufe
    /// sind wirkungslos.
    func installHotkeys() {
        guard !installed else { return }
        installed = true

        let registry = HotkeyRegistry.shared
        var okNumbers = 0
        for number in 1...9 {
            guard let keyCode = Self.keyCodeForDigit(number) else { continue }
            let ok = registry.register(keyCode: keyCode, modifiers: UInt32(cmdKey)) { [weak self] in
                self?.fireNumber(number)
            }
            if ok { okNumbers += 1 }
        }

        var okLetters = 0
        for letter in "ABCDEFGHIJKLMNOPQRSTUVWXYZ" {
            guard let keyCode = Self.keyCodeForLetter(letter) else { continue }
            let ok = registry.register(keyCode: keyCode,
                                       modifiers: UInt32(cmdKey | optionKey)) { [weak self] in
                self?.fireLetter(letter)
            }
            if ok { okLetters += 1 }
        }

        NSLog("[PromptHotkeys] registriert: %d Zahlen (Cmd+1..9), %d Buchstaben (Cmd+Opt+A..Z)",
              okNumbers, okLetters)
        DiagLog.write("Hotkey", "installed", [("numbers", okNumbers), ("letters", okLetters)])
    }

    // MARK: - Ausloesen

    private func fireNumber(_ number: Int) {
        lock.lock()
        let entry = numberMap[number]
        lock.unlock()
        guard let entry = entry else {
            // Kein Prompt auf dieser Taste: nichts tun. Windows verhaelt sich
            // genauso (der Hook laesst die Taste durch, wenn die Tabelle leer ist).
            tvoDebug("[PromptHotkeys] Cmd+\(number) ohne zugewiesenen Prompt")
            return
        }
        tvoDebug("[PromptHotkeys] Cmd+\(number) — Prompt einfuegen (\(entry.effectiveText.count) Zeichen)")
        DiagLog.write("Hotkey", "number_fired", [("number", number),
                                                 ("chars", entry.effectiveText.count)])
        insertHandler?(entry.effectiveText)
    }

    private func fireLetter(_ letter: Character) {
        lock.lock()
        let entry = letterMap[letter]
        lock.unlock()
        guard let entry = entry else {
            tvoDebug("[PromptHotkeys] Cmd+Opt+\(letter) ohne zugewiesenen Prompt")
            return
        }
        tvoDebug("[PromptHotkeys] Cmd+Opt+\(letter) — Prompt einfuegen (\(entry.effectiveText.count) Zeichen)")
        DiagLog.write("Hotkey", "letter_fired", [("letter", String(letter)),
                                                 ("chars", entry.effectiveText.count)])
        insertHandler?(entry.effectiveText)
    }

    // MARK: - Tastencodes

    private static func keyCodeForDigit(_ digit: Int) -> UInt32? {
        switch digit {
        case 1: return UInt32(kVK_ANSI_1)
        case 2: return UInt32(kVK_ANSI_2)
        case 3: return UInt32(kVK_ANSI_3)
        case 4: return UInt32(kVK_ANSI_4)
        case 5: return UInt32(kVK_ANSI_5)
        case 6: return UInt32(kVK_ANSI_6)
        case 7: return UInt32(kVK_ANSI_7)
        case 8: return UInt32(kVK_ANSI_8)
        case 9: return UInt32(kVK_ANSI_9)
        default: return nil
        }
    }

    private static func keyCodeForLetter(_ letter: Character) -> UInt32? {
        switch letter {
        case "A": return UInt32(kVK_ANSI_A)
        case "B": return UInt32(kVK_ANSI_B)
        case "C": return UInt32(kVK_ANSI_C)
        case "D": return UInt32(kVK_ANSI_D)
        case "E": return UInt32(kVK_ANSI_E)
        case "F": return UInt32(kVK_ANSI_F)
        case "G": return UInt32(kVK_ANSI_G)
        case "H": return UInt32(kVK_ANSI_H)
        case "I": return UInt32(kVK_ANSI_I)
        case "J": return UInt32(kVK_ANSI_J)
        case "K": return UInt32(kVK_ANSI_K)
        case "L": return UInt32(kVK_ANSI_L)
        case "M": return UInt32(kVK_ANSI_M)
        case "N": return UInt32(kVK_ANSI_N)
        case "O": return UInt32(kVK_ANSI_O)
        case "P": return UInt32(kVK_ANSI_P)
        case "Q": return UInt32(kVK_ANSI_Q)
        case "R": return UInt32(kVK_ANSI_R)
        case "S": return UInt32(kVK_ANSI_S)
        case "T": return UInt32(kVK_ANSI_T)
        case "U": return UInt32(kVK_ANSI_U)
        case "V": return UInt32(kVK_ANSI_V)
        case "W": return UInt32(kVK_ANSI_W)
        case "X": return UInt32(kVK_ANSI_X)
        case "Y": return UInt32(kVK_ANSI_Y)
        case "Z": return UInt32(kVK_ANSI_Z)
        default: return nil
        }
    }
}
