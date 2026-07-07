namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Vorfilter vor der Wake-Word-Engine (Best-Practice §4, Always-on-Effizienz). Nur wenn
    /// hier Sprache erkannt wird, wird der teurere KWS-Decode ueberhaupt ausgefuehrt — das
    /// spart im Leerlauf (Stille) den Grossteil der CPU/Energie.
    ///
    /// Bewusst als Interface, damit der heutige leichtgewichtige Energie-Gate
    /// (<see cref="EnergyVadGate"/>) spaeter verlustfrei durch ein Silero-VAD ersetzt werden
    /// kann (sherpa-onnx liefert Silero-VAD mit; ~1 MB, &lt;1 ms/Frame) — ohne den
    /// WakeWordController anzufassen.
    /// </summary>
    public interface IVadGate
    {
        /// <summary>True, wenn der Sample-Block wahrscheinlich Sprache enthaelt (KWS lohnt sich).</summary>
        bool IsSpeech(float[] samples);
    }
}
