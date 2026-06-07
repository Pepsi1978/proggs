using System;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Abstraktion ueber die Wake-Word-Erkennung. Bewusst als Interface, damit der
    /// WakeWordController gegen einen Fake getestet werden kann (kein Mikrofon, kein
    /// natives sherpa-onnx noetig) — Best-Practice §7 (Testbarkeit).
    ///
    /// Die konkrete Implementierung (<see cref="SherpaWakeWordEngine"/>) kapselt das
    /// native KeywordSpotter-Handle; ein Fake (<see cref="FakeWakeWordEngine"/>) erlaubt
    /// deterministische Tests.
    /// </summary>
    public interface IWakeWordEngine : IDisposable
    {
        /// <summary>
        /// Fuettert einen Block 16-kHz-mono-float-Samples (Bereich [-1, 1]) in die Engine.
        /// Gibt das erkannte Wake-Word zurueck (z.B. "OKAY COMPUTER") oder <c>null</c>,
        /// wenn in diesem Block kein Wake-Word erkannt wurde. Nach einem Treffer setzt die
        /// Implementierung ihren internen Stream selbst zurueck (Reset).
        /// </summary>
        string? Accept(float[] samples);

        /// <summary>Setzt den internen Erkennungs-Zustand zurueck (z.B. beim Moduswechsel).</summary>
        void Reset();
    }
}
