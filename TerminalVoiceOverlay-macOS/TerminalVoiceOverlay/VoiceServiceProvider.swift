import Foundation

/// Process-wide service locator for the three voice-stack pieces (audio
/// recorder, Groq Whisper transcription, optional Gemini text correction).
/// AppDelegate constructs them once at startup and registers them here;
/// secondary surfaces (e.g. PBPromptEditDialog's mic + G buttons) grab
/// the same instances instead of constructing parallel ones — important
/// because only one process at a time can hold the microphone.
///
/// Mirrors the Windows-side `VoiceServiceProvider` in
/// `TerminalVoiceOverlay-Windows/Services/VoiceServiceProvider.cs`.
enum VoiceServiceProvider {
    private(set) static var recorder: AudioRecorder?
    private(set) static var groq: GroqWhisperClient?
    private(set) static var gemini: GeminiClient?

    /// Transkriptions-Router (Groq Whisper oder Gemini Transcribe, je nach
    /// Einstellung). Nebenflaechen wie der PromptEdit-Dialog nutzen ihn statt
    /// `groq` direkt, damit die Modellauswahl ueberall gilt.
    private(set) static var stt: SpeechToTextRouter?

    static var recorderAvailable: Bool { recorder != nil && groq != nil }
    static var geminiAvailable: Bool { gemini != nil }

    static func initialize(recorder: AudioRecorder,
                           groq: GroqWhisperClient,
                           gemini: GeminiClient?,
                           stt: SpeechToTextRouter? = nil) {
        self.recorder = recorder
        self.groq = groq
        self.gemini = gemini
        self.stt = stt ?? SpeechToTextRouter(groq: groq, gemini: nil)
    }
}
