using System;
using System.Linq;
using NAudio.Wave;
using NAudio.Wave.SampleProviders;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Spielt einen kurzen "Enterprise"-/Funkspruch-Signalton (zwei aufsteigende Toene), wenn der
    /// Agent PROAKTIV etwas sagen will — so weiss Frank: gleich spricht der Agent. Generiert mit
    /// NAudio (SignalGenerator), keine Audiodatei noetig. Crasht nie (Ton ist unkritisch).
    /// </summary>
    public sealed class Chime
    {
        private WaveOutEvent? _wo;

        public void Play()
        {
            try
            {
                Stop();
                // Zwei kurze Sinustoene, leicht ansteigend (Kommunikator-artig).
                var seq = new ConcatenatingSampleProvider(new ISampleProvider[]
                {
                    Tone(988.0,  TimeSpan.FromMilliseconds(110)),   // H5
                    Tone(1318.5, TimeSpan.FromMilliseconds(170)),   // E6
                });
                _wo = new WaveOutEvent();
                _wo.Init(seq);
                _wo.Play();
            }
            catch (Exception ex)
            {
                Log.Error("Chime: Abspielen fehlgeschlagen (unkritisch)", ex);
            }
        }

        public void Stop()
        {
            try { _wo?.Stop(); _wo?.Dispose(); } catch { /* egal */ }
            _wo = null;
        }

        private static ISampleProvider Tone(double freq, TimeSpan dur)
            => new SignalGenerator(44100, 1) { Type = SignalGeneratorType.Sin, Frequency = freq, Gain = 0.28 }
                .Take(dur);
    }
}
