using System;

namespace VoiceAgent.Core
{
    /// <summary>Eine geplante Erinnerung: was, wann faellig, schon ausgeloest?</summary>
    public sealed class Reminder
    {
        public string Text { get; set; } = "";
        public DateTimeOffset DueUtc { get; set; }
        public bool Done { get; set; }
    }
}
