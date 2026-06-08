using System.Collections.Generic;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Antwort des Hauptagenten: der Antworttext + (falls an einen Unteragenten delegiert)
    /// dessen Name. DelegatedTo == null bedeutet: der Hauptagent hat selbst geantwortet.
    /// </summary>
    public sealed record AgentReply(string Text, string? DelegatedTo);

    /// <summary>
    /// Gestreamte Antwort des Hauptagenten: ein Strom von Text-Deltas (fuers zeitechte
    /// Vorlesen) plus — falls an einen Unteragenten delegiert — dessen Name. DelegatedTo steht
    /// bereits VOR dem Stream fest (Routing ist synchron). null = Hauptagent antwortet selbst
    /// bzw. Rueckfrage/Ablehnung.
    /// </summary>
    public sealed record AgentStream(string? DelegatedTo, IAsyncEnumerable<string> Text);
}
