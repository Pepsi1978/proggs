using System;
using System.Threading.Tasks;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Eine Anlaufstelle fuer die Transkription. Entscheidet pro Aufnahme
    /// anhand von <see cref="TranscriptionEngineSetting"/>, welcher der drei
    /// Wege gefragt wird — Groq Whisper, Gemini Transcribe (fertige Aufnahme,
    /// ein HTTPS-Aufruf) oder Gemini Transcribe Live (Streaming). Die Auswahl
    /// wirkt sofort, ohne Overlay-Neustart.
    ///
    /// Die Whisper-Halluzinations-Abwehr steckt komplett in
    /// <see cref="GroqWhisperClient"/> und laeuft deshalb bewusst NUR im
    /// Groq-Pfad; Gemini bekommt die Rohaufnahme direkt.
    /// </summary>
    public sealed class SpeechToTextRouter
    {
        private readonly GroqWhisperClient _groq;
        private readonly GeminiBatchTranscribeClient? _geminiBatch;
        private readonly GeminiTranscribeClient? _geminiLive;

        public SpeechToTextRouter(GroqWhisperClient groq,
            GeminiBatchTranscribeClient? geminiBatch,
            GeminiTranscribeClient? geminiLive)
        {
            _groq = groq;
            _geminiBatch = geminiBatch;
            _geminiLive = geminiLive;
        }

        public bool GeminiAvailable => _geminiBatch is not null || _geminiLive is not null;

        /// <summary>Welcher Weg gerade greift — fuer Logs/Statusanzeige.</summary>
        public string ActiveEngine => TranscriptionEngineSetting.Current switch
        {
            TranscriptionEngineSetting.Gemini when _geminiBatch is not null => TranscriptionEngineSetting.Gemini,
            TranscriptionEngineSetting.GeminiLive when _geminiLive is not null => TranscriptionEngineSetting.GeminiLive,
            _ => TranscriptionEngineSetting.Groq,
        };

        public async Task<string> TranscribeAsync(string wavFilePath)
        {
            // Auswahl steht auf Gemini, aber kein Schluessel hinterlegt: still
            // auf Groq zurueckfallen statt die Aufnahme zu verlieren.
            Func<Task<string>>? gemini = TranscriptionEngineSetting.Current switch
            {
                TranscriptionEngineSetting.Gemini when _geminiBatch is not null
                    => () => _geminiBatch.TranscribeAsync(wavFilePath),
                TranscriptionEngineSetting.GeminiLive when _geminiLive is not null
                    => () => _geminiLive.TranscribeAsync(wavFilePath),
                _ => null,
            };

            if (gemini is null)
            {
                if (TranscriptionEngineSetting.UseAnyGemini)
                    DiagLog.Warn("STT", "gemini_selected_but_unconfigured",
                        ("engine", TranscriptionEngineSetting.Current));
                return await _groq.TranscribeAsync(wavFilePath).ConfigureAwait(false);
            }

            try
            {
                return await gemini().ConfigureAwait(false);
            }
            catch (NoSpeechException)
            {
                // Stille Aufnahme ist ein gueltiges Ergebnis, kein Ausfall —
                // durchreichen, damit nichts getippt wird.
                throw;
            }
            catch (Exception ex)
            {
                // Kontingent erschoepft, Netz weg, API-Aenderung: das Diktat ist
                // schon gesprochen und darf nicht verloren gehen. Groq springt
                // ein — samt seinem Halluzinations-Schutz.
                DiagLog.Warn("STT", "gemini_failed_fallback_groq",
                    ("engine", TranscriptionEngineSetting.Current), ("error", ex.Message));
                return await _groq.TranscribeAsync(wavFilePath).ConfigureAwait(false);
            }
        }
    }
}
