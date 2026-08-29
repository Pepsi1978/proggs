import Foundation

/// Eine Anlaufstelle fuer die Transkription. Entscheidet pro Aufnahme anhand
/// von `TranscriptionEngineSetting`, ob Groq Whisper oder Gemini Transcribe
/// gefragt wird — die Auswahl wirkt damit sofort, ohne Overlay-Neustart.
///
/// Die Whisper-Halluzinations-Abwehr steckt komplett im `GroqWhisperClient`
/// und laeuft deshalb bewusst NUR im Groq-Pfad; Gemini bekommt die Rohaufnahme.
///
/// Portierung von TerminalVoiceOverlay-Windows/Services/SpeechToTextRouter.cs.
final class SpeechToTextRouter {

    private let groq: GroqWhisperClient
    private let gemini: GeminiBatchTranscribeClient?

    init(groq: GroqWhisperClient, gemini: GeminiBatchTranscribeClient?) {
        self.groq = groq
        self.gemini = gemini
    }

    var geminiAvailable: Bool { gemini != nil }

    /// Welcher Weg gerade greift — fuer Logs/Statusanzeige.
    var activeEngine: String {
        (TranscriptionEngineSetting.useGemini && gemini != nil)
            ? TranscriptionEngineSetting.gemini
            : TranscriptionEngineSetting.groq
    }

    func transcribe(fileURL: URL, completion: @escaping (Result<String, Error>) -> Void) {
        guard TranscriptionEngineSetting.useGemini, let gemini = gemini else {
            if TranscriptionEngineSetting.useGemini {
                // Auswahl steht auf Gemini, aber kein Schluessel hinterlegt:
                // still auf Groq zurueckfallen statt die Aufnahme zu verlieren.
                DiagLog.warn("STT", "gemini_selected_but_unconfigured")
            }
            groq.transcribe(fileURL: fileURL, completion: completion)
            return
        }

        gemini.transcribe(fileURL: fileURL) { [weak self] result in
            guard let self = self else { return }
            switch result {
            case .success:
                completion(result)

            case .failure(let error):
                // Stille Aufnahme ist ein gueltiges Ergebnis, kein Ausfall —
                // durchreichen, damit nichts getippt wird.
                if case GeminiBatchTranscribeClient.TranscribeError.noSpeech = error {
                    completion(result)
                    return
                }
                // Kontingent erschoepft, Netz weg, API-Aenderung: das Diktat ist
                // schon gesprochen und darf nicht verloren gehen. Groq springt
                // ein — samt seinem Halluzinations-Schutz.
                DiagLog.warn("STT", "gemini_failed_fallback_groq",
                             [("error", error.localizedDescription)])
                self.groq.transcribe(fileURL: fileURL, completion: completion)
            }
        }
    }
}
