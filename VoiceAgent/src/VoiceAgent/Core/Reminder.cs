using System;

namespace VoiceAgent.Core
{
    /// <summary>Eine geplante Erinnerung: was, wann faellig, schon ausgeloest?</summary>
    public sealed class Reminder
    {
        public string Text { get; set; } = "";
        public DateTimeOffset DueUtc { get; set; }
        public bool Done { get; set; }

        /// <summary>
        /// true = NICHT an eine Uhrzeit gebunden, sondern wird beim naechsten vollstaendigen
        /// App-Start faellig (Sound + Nachricht). Fuer "erinnere mich beim naechsten Start"
        /// bzw. wenn Frank keine Uhrzeit nennt. DueUtc ist dann egal.
        /// </summary>
        public bool OnNextStart { get; set; }
    }
}
