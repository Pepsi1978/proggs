using System.Collections.Generic;
using System.Threading;

namespace VoiceAgent.Services.Llm
{
    /// <summary>
    /// Optionale Erweiterung fuer LLM-Provider, die ihre Antwort TOKENWEISE streamen koennen
    /// (statt erst die komplette Antwort zu liefern). Damit kann der erste Satz bereits
    /// vorgelesen werden, waehrend der Rest noch generiert wird — das macht die Antwort
    /// "zeitecht" (Manifest: natuerliches Gespraech ohne Wartepause).
    ///
    /// Bewusst getrennt von <see cref="ILlmProvider"/> (Interface Segregation): Provider, die
    /// noch kein Streaming koennen, implementieren das einfach nicht — der Aufrufer faellt dann
    /// verlustfrei auf <see cref="ILlmProvider.ChatAsync"/> zurueck (ein einziger Chunk).
    /// </summary>
    public interface IStreamingLlmProvider
    {
        /// <summary>
        /// Schickt den Verlauf ans Modell und liefert die Antwort als Strom von Text-Deltas.
        /// Jeder Eintrag ist ein Stueck der wachsenden Antwort (Reihenfolge bleibt erhalten).
        /// </summary>
        IAsyncEnumerable<string> StreamAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default);
    }
}
