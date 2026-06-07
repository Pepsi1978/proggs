using System;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Ordnet eine fertige Nutzer-Aussage semantisch in AUFGABE / FRAGE / PLAUDEREI ein —
    /// damit die Live-Sonde EXAKT zeigt, was der Nutzer wollte, statt es aus der Antwort
    /// zu raten ("erst verstehen, dann antworten"). Laeuft wie der EndpointDetector auf
    /// einem eigenen, billigen Gemini-Modell, unabhaengig vom Haupt-Gehirn.
    /// </summary>
    public sealed class IntentDetector
    {
        private readonly ILlmProvider _provider;

        private const string SystemPrompt =
@"Du bist ein Intent-Klassifizierer fuer einen Sprachassistenten. Du bekommst, was der Nutzer gerade gesagt hat, und ordnest es GENAU EINER Kategorie zu.

Antworte mit GENAU EINEM WORT, sonst nichts:
- AUFGABE — der Nutzer will, dass etwas getan, geaendert, gestartet, gesucht oder erledigt wird (ein Befehl, eine Handlung).
- FRAGE — eine Wissens- oder Verstaendnisfrage, auf die eine Antwort erwartet wird.
- PLAUDEREI — Smalltalk, Begruessung oder Bemerkung ohne konkrete Aufgabe oder Frage.

Im Zweifel zwischen AUFGABE und FRAGE: nimm AUFGABE, wenn eine Handlung verlangt wird.";

        public IntentDetector(ILlmProvider provider) => _provider = provider;

        /// <summary>Klassifiziert die Aussage. Bei Fehlern: Plauderei (harmloser Default, kein Crash).</summary>
        public async Task<IntentKind> ClassifyAsync(string text, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(text)) return IntentKind.Unknown;
            try
            {
                var msgs = new[]
                {
                    new LlmMessage(LlmRole.System, SystemPrompt),
                    new LlmMessage(LlmRole.User, text)
                };
                var reply = await _provider.ChatAsync(msgs, ct).ConfigureAwait(false);
                var u = reply.ToUpperInvariant();
                Probe.That(u.Contains("AUFGABE") || u.Contains("FRAGE") || u.Contains("PLAUDEREI"),
                    "IntentDetector: unklare LLM-Antwort — nehme Plauderei an", new { reply = reply.Trim() });
                var kind = ParseIntent(reply);
                Log.Info($"Intent-Check: \"{text}\" -> {kind} (LLM: {reply.Trim()})");
                return kind;
            }
            catch (Exception ex)
            {
                Log.Error("IntentDetector: Klassifikation fehlgeschlagen — nehme Plauderei an", ex);
                return IntentKind.Chat;
            }
        }

        /// <summary>Wertet die LLM-Antwort aus. Public static = isoliert testbar.</summary>
        public static IntentKind ParseIntent(string reply)
        {
            if (string.IsNullOrWhiteSpace(reply)) return IntentKind.Chat;
            var u = reply.ToUpperInvariant();
            if (u.Contains("AUFGABE")) return IntentKind.Task;
            if (u.Contains("FRAGE")) return IntentKind.Question;
            if (u.Contains("PLAUDEREI")) return IntentKind.Chat;
            return IntentKind.Chat;   // unklar -> harmloser Default
        }
    }
}
