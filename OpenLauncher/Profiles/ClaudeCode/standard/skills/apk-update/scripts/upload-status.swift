// Fragt für jeden Pfad den Upload-Stand des Dateianbieters ab (Google Drive für Desktop unter macOS).
// Ausgabe je Pfad eine Zeile: vorhanden|cloudDatei|hochgeladen|laedtHoch|Fehlertext (0/1).
// Nur lesen: Die Abfrage lädt die Datei nicht herunter.
import Foundation

for pfad in CommandLine.arguments.dropFirst() {
    let url = URL(fileURLWithPath: pfad)
    guard FileManager.default.fileExists(atPath: pfad) else {
        print("0|0|0|0|")
        continue
    }
    let werte = try? url.resourceValues(forKeys: [
        .isUbiquitousItemKey, .ubiquitousItemIsUploadedKey, .ubiquitousItemIsUploadingKey, .ubiquitousItemUploadingErrorKey,
    ])
    let fehler = (werte?.ubiquitousItemUploadingError?.localizedDescription ?? "")
        .replacingOccurrences(of: "\n", with: " ")
    let b: (Bool?) -> String = { $0 == true ? "1" : "0" }
    print("1|\(b(werte?.isUbiquitousItem))|\(b(werte?.ubiquitousItemIsUploaded))|\(b(werte?.ubiquitousItemIsUploading))|\(fehler)")
}
