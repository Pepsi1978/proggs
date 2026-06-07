using System;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Audio;

namespace VoiceAgent.Core
{
    /// <summary>Ruht (lauscht nur aufs Weckwort) oder ist wach (verarbeitet normal).</summary>
    public enum WakeState { Sleeping, Awake }

    /// <summary>
    /// Zustandsautomat fuer die Weckwort-Steuerung (Best-Practice wake-word §5):
    /// <c>Sleeping</c> → Weckwort erkannt → <c>Awake</c> (aktives Fenster) → Stille-Timeout → <c>Sleeping</c>.
    ///
    /// Bewusst SYNCHRON und ohne UI-/Audio-Abhaengigkeiten gebaut, damit er mit einem
    /// <see cref="FakeWakeWordEngine"/> und injizierter Zeit deterministisch testbar ist
    /// (Best-Practice §7). Die Audio-Zufuhr (OnFrame) und die Reaktionen (Sound/Greeting/UI)
    /// liegen ausserhalb — der Controller meldet nur ueber Events.
    ///
    /// Im Ruhezustand laeuft VOR dem teuren KWS-Decode ein VAD-Vorfilter (§4): nur Bloecke
    /// mit Sprache erreichen die Engine — spart CPU/Energie im Leerlauf.
    /// </summary>
    public sealed class WakeWordController : IDisposable
    {
        private readonly IWakeWordEngine _engine;
        private readonly IVadGate _vad;
        private readonly int _timeoutMs;
        private readonly Func<DateTimeOffset> _now;   // injizierbar fuer Tests (statt DateTimeOffset.Now)

        private DateTimeOffset _lastActivity;
        private bool _disposed;

        /// <summary>Weckwort erkannt — der Agent ist jetzt wach (Argument: erkanntes Keyword).</summary>
        public event Action<string>? Woke;
        /// <summary>Zurueck in den Ruhezustand (Stille-Timeout abgelaufen).</summary>
        public event Action? Slept;

        public WakeState State { get; private set; } = WakeState.Sleeping;

        public WakeWordController(IWakeWordEngine engine, IVadGate vad, int timeoutMs,
            Func<DateTimeOffset>? now = null)
        {
            _engine = engine ?? throw new ArgumentNullException(nameof(engine));
            _vad = vad ?? throw new ArgumentNullException(nameof(vad));
            Probe.That(timeoutMs > 0, "WakeWordController: timeoutMs <= 0 — Wachfenster schliesst nie", new { timeoutMs });
            _timeoutMs = timeoutMs > 0 ? timeoutMs : 60000;
            _now = now ?? (() => DateTimeOffset.UtcNow);
        }

        /// <summary>
        /// Fuettert einen 16-kHz-mono-float-Block. Im Ruhezustand wird auf das Weckwort geprueft
        /// (mit VAD-Vorfilter). Gibt true zurueck, wenn DIESER Block das Wecken ausgeloest hat.
        /// Im wachen Zustand passiert hier nichts (die normale Aussagen-Verarbeitung laeuft separat).
        /// </summary>
        public bool ProcessFrame(float[] samples)
        {
            if (_disposed || State != WakeState.Sleeping) return false;
            if (samples == null || samples.Length == 0) return false;
            if (!_vad.IsSpeech(samples)) return false;   // Vorfilter: nur Sprache zur Engine (§4)

            string? keyword = _engine.Accept(samples);
            if (string.IsNullOrEmpty(keyword)) return false;

            State = WakeState.Awake;
            _lastActivity = _now();
            Probe.State("Sleeping", "Awake", new { keyword });
            Log.Info("Weckwort erkannt — Agent ist wach", new { keyword });
            Woke?.Invoke(keyword);
            return true;
        }

        /// <summary>Im wachen Zustand bei jeder Nutzeraktivitaet aufrufen — verlaengert das Fenster.</summary>
        public void NotifyActivity()
        {
            if (State == WakeState.Awake) _lastActivity = _now();
        }

        /// <summary>
        /// Regelmaessig aufrufen (z.B. vom UI-Sekundentakt). Schliesst das Wachfenster, wenn seit
        /// der letzten Aktivitaet laenger als das Timeout vergangen ist.
        /// </summary>
        public void Tick()
        {
            if (State != WakeState.Awake) return;
            if ((_now() - _lastActivity).TotalMilliseconds >= _timeoutMs)
                Sleep("Timeout");
        }

        /// <summary>Sofort in den Ruhezustand zuruecksetzen (z.B. wenn das Feature deaktiviert wird).</summary>
        public void ForceSleep() => Sleep("manuell");

        private void Sleep(string reason)
        {
            if (State == WakeState.Sleeping) return;
            State = WakeState.Sleeping;
            _engine.Reset();   // Erkennungs-Zustand sauber zuruecksetzen
            Probe.State("Awake", "Sleeping", new { reason });
            Log.Info("Zurueck im Ruhezustand (lausche aufs Weckwort)", new { reason });
            Slept?.Invoke();
        }

        public void Dispose()
        {
            if (_disposed) return;
            _disposed = true;
            try { _engine.Dispose(); } catch (Exception ex) { Log.Error("WakeWordController: Dispose-Fehler", ex); }
        }
    }
}
