using System;
using System.Collections.Generic;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>Eine proaktive Meldung, die der Agent Frank von sich aus mitteilen moechte.</summary>
    public sealed record ProactiveMessage(string Text, string Reason);

    /// <summary>
    /// Generischer Kanal fuer PROAKTIVE Meldungen (Manifest: "Der Hauptagent kann mich selber
    /// ansprechen — signalisiert mit einem Geraeusch, ich reagiere, dann sagt er, was es gibt").
    ///
    /// Bisher konnte sich der Agent nur ueber eine vorab gesetzte ERINNERUNG melden. Dieser Kanal
    /// entkoppelt das: BELIEBIGE Quellen (ein Followup auf eine offene Rueckfrage, spaeter ein
    /// Hintergrund-Helfer, der fertig wird) koennen — auch von einem anderen Thread — eine Meldung
    /// einreihen. Die UI (MainWindow) spielt dann den Funkspruch-Sound und sagt die Meldung an,
    /// sobald Frank reagiert. Thread-safe (eigener Lock, nie lock(this)).
    /// </summary>
    public sealed class ProactiveChannel
    {
        private readonly object _lock = new();
        private readonly Queue<ProactiveMessage> _queue = new();

        /// <summary>Wird gefeuert, sobald eine Meldung eingereiht wurde (die UI kann sofort reagieren).</summary>
        public event Action? Posted;

        /// <summary>Reiht eine proaktive Meldung ein. Leere Texte werden ignoriert.</summary>
        public void Post(string text, string reason = "")
        {
            if (string.IsNullOrWhiteSpace(text)) return;
            lock (_lock) { _queue.Enqueue(new ProactiveMessage(text.Trim(), reason ?? string.Empty)); }
            Log.Info("Proaktive Meldung eingereiht", new { reason, text });
            try { Posted?.Invoke(); }
            catch (Exception ex) { Log.Error("ProactiveChannel: Posted-Handler warf (ignoriert)", ex); }
        }

        /// <summary>Nimmt die naechste Meldung heraus (FIFO). false = Queue leer.</summary>
        public bool TryTake(out ProactiveMessage message)
        {
            lock (_lock)
            {
                if (_queue.Count > 0) { message = _queue.Dequeue(); return true; }
            }
            message = null!;
            return false;
        }

        /// <summary>Ob mindestens eine Meldung wartet.</summary>
        public bool HasPending
        {
            get { lock (_lock) { return _queue.Count > 0; } }
        }
    }
}
