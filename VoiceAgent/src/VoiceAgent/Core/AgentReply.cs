namespace VoiceAgent.Core
{
    /// <summary>
    /// Antwort des Hauptagenten: der Antworttext + (falls an einen Unteragenten delegiert)
    /// dessen Name. DelegatedTo == null bedeutet: der Hauptagent hat selbst geantwortet.
    /// </summary>
    public sealed record AgentReply(string Text, string? DelegatedTo);
}
