using System;
using System.Collections.Generic;
using System.Threading;

namespace VoiceAgent.Diagnostics
{
    /// <summary>Wie der Hauptagent eine Eingabe eingeordnet hat (heuristisch abgeleitet).</summary>
    public enum IntentKind
    {
        Unknown,
        Chat,      // Plauderei
        Question,  // Wissensfrage -> direkte Antwort
        Task       // konkrete Aufgabe -> Manifest verlangt eine Rueckfrage
    }

    /// <summary>
    /// LIVE-LOGIK-SONDE fuer einen kompletten Sprach-Turn.
    ///
    /// Macht entlang des Voice-Loops sichtbar, ob die App GENAU das tut, was die Design-Spec
    /// (das "Manifest", docs/2026-06-07-hauptagent-design.md, Abschnitt 6 + 9) vorsieht:
    ///   gehoert -> Endpunkt -> Aussage -> verstanden (Aufgabe/Frage/Plauderei) ->
    ///   geantwortet -> gesprochen -> Manifest-Abgleich.
    ///
    /// Jeder Turn bekommt eine fortlaufende Id; ueber Log.CurrentTurn tragen ALLE Log-Zeilen
    /// dieses Turns automatisch das Feld "turn":N. Im Live-Tail kann Frank damit einen
    /// einzelnen Turn als zusammenhaengende Kette mitlesen UND bewerten, ob die App in die
    /// im Manifest beschriebene Richtung arbeitet.
    ///
    /// Die Intent-Einordnung ist in dieser Stufe HEURISTISCH (aus der Agent-Antwort abgeleitet)
    /// und als method="heuristic" markiert. Die Klassifizier-Schnittstelle (Classify) ist
    /// bewusst gekapselt, damit sie spaeter durch einen echten LLM-Intent-Detektor
    /// (analog EndpointDetector) ersetzt werden kann, ohne die Trace anzufassen.
    /// </summary>
    public sealed class TurnTrace
    {
        private static long _counter;

        public long Id { get; }
        public string Source { get; }   // "voice" | "text"

        private readonly DateTimeOffset _startedAt = DateTimeOffset.Now;
        private bool _heard;
        private bool _responded;
        private bool _spoke;
        private IntentKind _intent = IntentKind.Unknown;
        private bool _askedBack;

        private TurnTrace(string source)
        {
            Id = Interlocked.Increment(ref _counter);
            Source = source;
            Log.CurrentTurn = Id;   // ab jetzt tragen alle Logs dieses Turns "turn":Id
            Log.Info("Turn begonnen", new { source });
        }

        /// <summary>Startet einen neuen Turn (source: "voice" oder "text").</summary>
        public static TurnTrace Begin(string source) => new(source);

        /// <summary>Schritt 2 (Manifest): was wurde transkribiert.</summary>
        public void Heard(string transcript, double? durationMs = null)
        {
            _heard = _heard || !string.IsNullOrWhiteSpace(transcript);
            Log.Info("GEHOERT", new { text = transcript, durationMs });
        }

        /// <summary>Schritt 3 (Manifest): Endpunkt-Entscheidung — fertig gesprochen oder Denkpause.</summary>
        public void Endpoint(bool complete, string? raw = null)
            => Log.Info("ENDPUNKT", new { decision = complete ? "FERTIG" : "WEITER", raw });

        /// <summary>Die ueber mehrere Haeppchen gesammelte, vollstaendige Aussage.</summary>
        public void Accumulated(string fullText)
            => Log.Info("AUSSAGE", new { text = fullText });

        /// <summary>
        /// Schritt 3 (Manifest): wie der Hauptagent die Aussage eingeordnet hat
        /// (Aufgabe/Frage/Plauderei) und ob er — wie verlangt — bei einer Aufgabe zurueckfragt.
        /// Heuristisch aus der Antwort abgeleitet.
        /// </summary>
        public void Understood(string reply)
        {
            _intent = Classify(reply).kind;
            Log.Info("VERSTANDEN", new { intent = _intent.ToString(), method = "heuristic" });
        }

        /// <summary>
        /// Exakte Intent-Einordnung durch den LLM-Intent-Detektor (statt aus der Antwort zu raten).
        /// Wird VOR der Antwort aufgerufen — "erst verstehen, dann antworten".
        /// </summary>
        public void UnderstoodExact(IntentKind kind)
        {
            _intent = kind;
            Log.Info("VERSTANDEN", new { intent = _intent.ToString(), method = "llm" });
        }

        /// <summary>Schritt 4 (Manifest): die erzeugte Antwort + welches LLM + Latenz.</summary>
        public void Responded(string reply, string provider, double elapsedMs)
        {
            _responded = !string.IsNullOrWhiteSpace(reply);
            _askedBack = Classify(reply).askedBack;   // hat der Agent eine Rueckfrage gestellt?
            Log.Info("GEANTWORTET", new
            {
                provider,
                elapsedMs,
                chars = reply?.Length ?? 0,
                askedBack = _askedBack,
                preview = Preview(reply)
            });
        }

        /// <summary>Schritt 5 (Manifest): wurde die Antwort vorgelesen.</summary>
        public void Spoke(string voice, bool ok)
        {
            _spoke = ok;
            Log.Info("GESPROCHEN", new { voice, ok });
        }

        /// <summary>Der Hauptagent hat die Aufgabe an einen Unteragenten delegiert (Manifest Baustein 2).</summary>
        public void Delegated(string subAgentName)
            => Log.Info("DELEGIERT", new { to = subAgentName });

        /// <summary>Eine Stufe ist fehlgeschlagen — mit vollem Kontext (turn-korreliert).</summary>
        public void Failed(string stage, Exception ex)
            => Log.Error($"Turn-Fehler in Stufe {stage}", ex, new { stage });

        /// <summary>
        /// MANIFEST-ABGLEICH (Franks Kernwunsch): Hat der Turn dem Soll-Ablauf entsprochen?
        /// Schreibt eine klare Bewertung (INFO bei Treffer, WARN bei Abweichung) und beendet
        /// die Turn-Korrelation. Genau diese Zeile beantwortet live: "Arbeitet die App so,
        /// wie sie laut Manifest soll?"
        /// </summary>
        public void Complete()
        {
            try
            {
                var issues = new List<string>();
                if (!_heard) issues.Add("nichts transkribiert");
                if (!_responded) issues.Add("keine Antwort erzeugt");
                if (_intent == IntentKind.Task && !_askedBack)
                    issues.Add("Aufgabe erkannt, aber keine Rueckfrage gestellt (Manifest verlangt Rueckfrage)");
                if (Source == "voice" && _responded && !_spoke)
                    issues.Add("Antwort wurde nicht vorgelesen");

                double totalMs = (DateTimeOffset.Now - _startedAt).TotalMilliseconds;
                var ctx = new
                {
                    intent = _intent.ToString(),
                    askedBack = _askedBack,
                    totalMs,
                    issues
                };

                if (issues.Count == 0)
                    Log.Info("MANIFEST: Turn folgte dem Soll-Ablauf", ctx);
                else
                    Log.Warn("MANIFEST-ABWEICHUNG: Turn wich vom Soll-Ablauf ab", ctx);
            }
            finally
            {
                // Nur die EIGENE Korrelation beenden: hat inzwischen ein neuer Turn begonnen
                // (ueberlappend oder dieser hier verspaetet beendet), wuerde ein bedingungsloses
                // CurrentTurn = 0 die Korrelation des NEUEREN Turns faelschlich loeschen.
                // Im sequenziellen Normalfall gilt CurrentTurn == Id -> identisches Verhalten.
                if (Log.CurrentTurn == Id) Log.CurrentTurn = 0;
            }
        }

        /// <summary>Bricht den Turn ab (z.B. leeres/abgebrochenes Haeppchen) und beendet die Korrelation.</summary>
        public void Abort(string reason)
        {
            Log.Info("Turn abgebrochen", new { reason });
            // Wie in Complete(): nur die eigene Korrelation zuruecksetzen, nie die eines neueren Turns.
            if (Log.CurrentTurn == Id) Log.CurrentTurn = 0;
        }

        // ---- Intent-Heuristik (gekapselt; spaeter durch echten LLM-Detektor ersetzbar) ----

        /// <summary>
        /// Leitet aus der Agent-Antwort ab, ob eine Aufgabe erkannt wurde (Agent stellt eine
        /// Verstaendnis-Rueckfrage) oder ob es Frage/Plauderei war. Public static = isoliert testbar.
        /// </summary>
        public static (IntentKind kind, bool askedBack) Classify(string? reply)
        {
            if (string.IsNullOrWhiteSpace(reply)) return (IntentKind.Unknown, false);

            string r = reply.ToLowerInvariant();
            bool isQuestion = reply.Contains('?');
            bool confirms =
                r.Contains("soll ich") || r.Contains("soll das") || r.Contains("erledigen") ||
                r.Contains("richtig verstanden") || r.Contains("fuer dich") || r.Contains("für dich") ||
                r.Contains("moechtest du") || r.Contains("möchtest du");

            if (isQuestion && confirms) return (IntentKind.Task, true);  // Aufgabe erkannt + Rueckfrage
            if (isQuestion) return (IntentKind.Question, false);
            return (IntentKind.Chat, false);
        }

        private static string Preview(string? s)
        {
            if (string.IsNullOrEmpty(s)) return "";
            s = s.Replace("\n", " ").Replace("\r", " ").Trim();
            return s.Length <= 120 ? s : s.Substring(0, 120) + "…";
        }
    }
}
