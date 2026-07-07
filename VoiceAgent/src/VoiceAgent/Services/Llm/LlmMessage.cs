namespace VoiceAgent.Services.Llm
{
    /// <summary>Rolle einer Nachricht im Gespraech mit dem LLM.</summary>
    public enum LlmRole { System, User, Assistant }

    /// <summary>Eine einzelne Nachricht im Gespraechsverlauf.</summary>
    public sealed record LlmMessage(LlmRole Role, string Text);
}
