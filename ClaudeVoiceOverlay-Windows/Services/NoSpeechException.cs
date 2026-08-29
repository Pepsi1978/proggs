using System;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Die Aufnahme enthielt keinen erkennbaren Sprachinhalt. Bewusst eine
    /// eigene Ausnahme statt einer allgemeinen: der Router darf bei einem
    /// TECHNISCHEN Fehler (Kontingent erschoepft, Netz weg) auf den anderen
    /// Dienst ausweichen, aber NICHT bei einer stillen Aufnahme — sonst wuerde
    /// eine versehentlich leere Aufnahme zweimal verschickt und am Ende doch
    /// eine halluzinierte Floskel getippt.
    /// </summary>
    public sealed class NoSpeechException : Exception
    {
        public NoSpeechException(string message) : base(message) { }
    }
}
