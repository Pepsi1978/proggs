using System;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Leichtgewichtiger Energie-(RMS)-Vorfilter: laesst nur Bloecke mit genug Lautstaerke zur
    /// Wake-Word-Engine durch. Spart im Stille-Leerlauf den KWS-Decode (Best-Practice §4).
    ///
    /// Pure, hardware-frei testbar. Verwendet dieselbe RMS-Logik wie der AlwaysOnListener,
    /// damit Schwellen konsistent sind.
    ///
    /// EINGEPLANTER NAECHSTER SCHRITT (Silero-VAD): sherpa-onnx bringt ein Silero-VAD-Modell
    /// mit (VadModelConfig/SileroVadModelConfig). Eine kuenftige <c>SileroVadGate : IVadGate</c>
    /// kann dieses Energie-Gate ersetzen (genauer, weniger Fehlausloeser durch Nicht-Sprach-
    /// Geraeusche) — ohne Aenderung am WakeWordController, da beide <see cref="IVadGate"/>
    /// implementieren. Das Energie-Gate ist der robuste, sofort funktionierende erste Filter.
    /// </summary>
    public sealed class EnergyVadGate : IVadGate
    {
        private readonly float _threshold;

        /// <param name="threshold">RMS-Schwelle (Default an AppSettings.SilenceThreshold angelehnt).</param>
        public EnergyVadGate(double threshold = 0.012)
        {
            _threshold = (float)threshold;
        }

        public bool IsSpeech(float[] samples)
        {
            if (samples == null || samples.Length == 0) return false;
            double sum = 0;
            foreach (var s in samples) sum += (double)s * s;
            double rms = Math.Sqrt(sum / samples.Length);
            return rms >= _threshold;
        }
    }
}
