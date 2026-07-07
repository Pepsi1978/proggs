using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// Schnittstelle fuer das "Gehirn" des Hauptagenten. Der BossAgent haengt nur an
    /// dieser Schnittstelle, nicht an einem konkreten Anbieter — so ist das Umschalten
    /// zwischen Gemini, Claude und OpenAI nur eine andere Implementierung.
    /// </summary>
    public interface ILlmProvider
    {
        /// <summary>Anzeigename des Anbieters (fuer Logs/UI).</summary>
        string Name { get; }

        /// <summary>Schickt den Gespraechsverlauf ans Modell und liefert die Antwort.</summary>
        Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default);
    }
}
