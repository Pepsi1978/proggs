namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Test-Doppel der Wake-Word-Engine: erkennt nie von selbst etwas, sondern liefert
    /// genau dann einen Treffer, wenn <see cref="TriggerWake"/> aufgerufen wurde. So lassen
    /// sich Sleep/Wake-Logik und der WakeWordController deterministisch und hardware-frei
    /// testen (Best-Practice §7).
    /// </summary>
    public sealed class FakeWakeWordEngine : IWakeWordEngine
    {
        private string? _pending;

        /// <summary>Anzahl der Sample-Bloecke, die seit dem letzten Reset gefuettert wurden.</summary>
        public int AcceptCount { get; private set; }

        /// <summary>Wird true, sobald Dispose aufgerufen wurde (fuer Lifecycle-Tests).</summary>
        public bool Disposed { get; private set; }

        /// <summary>Laesst den naechsten <see cref="Accept"/>-Aufruf das angegebene Keyword melden.</summary>
        public void TriggerWake(string keyword = "OKAY COMPUTER") => _pending = keyword;

        public string? Accept(float[] samples)
        {
            AcceptCount++;
            if (_pending == null) return null;
            var k = _pending;
            _pending = null;   // wie die echte Engine: nach Treffer zuruecksetzen
            return k;
        }

        public void Reset()
        {
            _pending = null;
            AcceptCount = 0;
        }

        public void Dispose() => Disposed = true;
    }
}
