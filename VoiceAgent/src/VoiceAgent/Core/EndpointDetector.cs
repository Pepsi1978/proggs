using System;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Semantische Endpunkt-Erkennung ("Turn Detection"): entscheidet, ob eine per Sprache
    /// diktierte Aussage ABGESCHLOSSEN ist oder ob der Sprecher nur eine Denkpause macht.
    /// So kann der Hauptagent Gedankenpausen abwarten, statt nach jeder kurzen Stille
    /// sofort loszureden. Nutzt das schnelle Standard-LLM (Gemini Flash Lite).
    /// </summary>
    public sealed class EndpointDetector
    {
        private readonly ILlmProvider _provider;

        private const string SystemPrompt =
@"Du bist ein Endpunkt-Detektor fuer einen Sprachassistenten. Du bekommst Text, den der Nutzer GERADE diktiert hat, und entscheidest, ob er FERTIG gesprochen hat oder ob er nur eine Denkpause macht und gleich weiterredet.

Antworte mit GENAU EINEM Wort, sonst nichts:
- FERTIG  — wenn es eine abgeschlossene Frage, Aussage oder Aufgabe ist, auf die man sinnvoll reagieren kann.
- WEITER  — wenn der Gedanke unfertig wirkt: er bricht mitten im Satz ab, endet mit einer Konjunktion oder Praeposition (und, oder, weil, dass, mit, fuer ...), ist nur ein Sprach-Fragment ohne klare Absicht, oder klingt, als kaeme gleich noch mehr.

Im Zweifel IMMER WEITER — lieber kurz abwarten als den Nutzer mitten im Gedanken unterbrechen.";

        public EndpointDetector(ILlmProvider provider) => _provider = provider;

        /// <summary>Prueft, ob die Aussage abgeschlossen ist. Bei Fehlern: true (lieber antworten als haengen).</summary>
        public async Task<bool> IsCompleteAsync(string text, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(text)) return false;
            try
            {
                var msgs = new[]
                {
                    new LlmMessage(LlmRole.System, SystemPrompt),
                    new LlmMessage(LlmRole.User, text)
                };
                var reply = await _provider.ChatAsync(msgs, ct).ConfigureAwait(false);
                var u = reply.ToUpperInvariant();
                Probe.That(u.Contains("FERTIG") || u.Contains("WEITER"),
                    "EndpointDetector: unklare LLM-Antwort — nehme FERTIG an", new { reply = reply.Trim() });
                bool complete = ParseIsComplete(reply);
                Log.Info($"Endpoint-Check: \"{text}\" -> {(complete ? "FERTIG" : "WEITER")} (LLM: {reply.Trim()})");
                return complete;
            }
            catch (Exception ex)
            {
                Log.Error("EndpointDetector: Check fehlgeschlagen — gehe von FERTIG aus", ex);
                return true;
            }
        }

        /// <summary>Wertet die LLM-Antwort aus. Public static = isoliert testbar.</summary>
        public static bool ParseIsComplete(string reply)
        {
            if (string.IsNullOrWhiteSpace(reply)) return true;
            var upper = reply.ToUpperInvariant();
            // "WEITER" hat Vorrang: sagt das Modell explizit WEITER, wird abgewartet.
            if (upper.Contains("WEITER")) return false;
            if (upper.Contains("FERTIG")) return true;
            // Unklare Antwort: lieber antworten als haengen.
            return true;
        }
    }
}
