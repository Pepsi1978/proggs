using System;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Eine Anlaufstelle fuer die Transkription. Entscheidet pro Aufnahme
    /// anhand von <see cref="TranscriptionEngineSetting"/>, ob Groq Whisper
    /// oder Gemini Transcribe gefragt wird — die Auswahl wirkt damit sofort,
    /// ohne Overlay-Neustart.
    ///
    /// Die Whisper-Halluzinations-Abwehr steckt komplett in
    /// <see cref="GroqWhisperClient"/> und laeuft deshalb bewusst NUR im
    /// Groq-Pfad; Gemini bekommt die Rohaufnahme direkt.
    /// </summary>
    public sealed class SpeechToTextRouter
    {
        private readonly GroqWhisperClient _groq;
        private readonly GeminiTranscribeClient? _gemini;

        public SpeechToTextRouter(GroqWhisperClient groq, GeminiTranscribeClient? gemini)
        {
            _groq = groq;
            _gemini = gemini;
        }

        public bool GeminiAvailable => _gemini is not null;

        /// <summary>Modellname fuer Logs/Statusanzeige.</summary>
        public string ActiveEngine =>
            TranscriptionEngineSetting.UseGemini && _gemini is not null
                ? TranscriptionEngineSetting.Gemini
                : TranscriptionEngineSetting.Groq;

        public Task<string> TranscribeAsync(string wavFilePath)
        {
            if (TranscriptionEngineSetting.UseGemini)
            {
                if (_gemini is null)
                {
                    // Auswahl steht auf Gemini, aber kein Schluessel hinterlegt:
                    // still auf Groq zurueckfallen statt die Aufnahme zu verlieren.
                    DiagLog.Warn("STT", "gemini_selected_but_unconfigured");
                }
                else
                {
                    return _gemini.TranscribeAsync(wavFilePath);
                }
            }
            return _groq.TranscribeAsync(wavFilePath);
        }
    }
}
