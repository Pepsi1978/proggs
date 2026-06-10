using System;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Demo-Unteragent (erster echter Helfer): nimmt "merk dir / notier"-Auftraege entgegen
    /// und legt sie ins Langzeit-Gedaechtnis (AgentMemory.Remember). Verbindet die Delegation
    /// (Baustein 2) mit dem Gedaechtnis (Stufe 1) an einem konkreten, ungefaehrlichen Beispiel —
    /// OHNE Computer Use (er steuert nichts am PC, er merkt sich nur etwas).
    /// </summary>
    public sealed class NoteSubAgent : ISubAgent
    {
        private readonly AgentMemory _memory;
        public NoteSubAgent(AgentMemory memory) => _memory = memory;

        public string Name => "Notiz";
        public string Description => "Merkt sich Notizen und Fakten dauerhaft im Gedaechtnis.";

        public string HowTo => "Sag z. B. 'merk dir, dass mein Auto in Reihe 7 steht' oder 'notier: Geschenk fuer Mama besorgen' — der Fakt bleibt dauerhaft im Gedaechtnis, auch ueber Neustarts.";

        // "erinnere mich" bewusst NICHT hier — das gehoert dem ReminderSubAgent (zeitgesteuert).
        private static readonly string[] Triggers =
            { "merk dir", "merke dir", "notiz", "notier", "schreib dir auf", "behalte" };

        public bool CanHandle(string task)
        {
            if (string.IsNullOrWhiteSpace(task)) return false;
            var t = task.ToLowerInvariant();
            foreach (var tr in Triggers)
                if (t.Contains(tr)) return true;
            return false;
        }

        /// <summary>Spezifische Rueckfrage: nennt, was gemerkt werden soll.</summary>
        public string ConfirmationQuestion(string task)
        {
            var note = ExtractNote(task);
            return string.IsNullOrWhiteSpace(note)
                ? "Soll ich mir das merken?"
                : $"Soll ich mir merken: {note}?";
        }

        public Task<string> HandleAsync(string task, CancellationToken ct = default)
        {
            var note = ExtractNote(task);
            Probe.That(!string.IsNullOrWhiteSpace(note), "NoteSubAgent: leere Notiz extrahiert", new { task });
            _memory.Remember(note);
            Log.Info("NoteSubAgent: Notiz gemerkt", new { note });
            return Task.FromResult($"Ich habe mir gemerkt: {note}");
        }

        /// <summary>Schneidet das Trigger-Wort weg, sodass nur die eigentliche Notiz bleibt. Public static = testbar.</summary>
        public static string ExtractNote(string task)
        {
            if (string.IsNullOrWhiteSpace(task)) return "";
            var t = task.Trim();
            var lower = t.ToLowerInvariant();
            foreach (var tr in Triggers)
            {
                int idx = lower.IndexOf(tr, StringComparison.Ordinal);
                if (idx >= 0)
                {
                    var after = t.Substring(idx + tr.Length).TrimStart(' ', ',', ':', '.', '!', '?');
                    if (after.Length > 0) return after;
                }
            }
            return t;   // kein Trigger gefunden -> ganze Aussage als Notiz
        }
    }
}
