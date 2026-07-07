using System;
using System.Collections.Generic;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Ergebnis des strukturierten Verstehens-Schritts: was der Nutzer WIRKLICH meint —
    /// nicht der Rohtext. Auftrag ist vollstaendig + normalisiert formuliert (Transkriptions-
    /// fehler korrigiert, Bezuege aus dem Verlauf aufgeloest, Zeiten als "um HH:MM"/"morgen",
    /// damit die deterministische Slot-Extraktion der Helfer sie sicher parst).
    /// </summary>
    public sealed record Understanding(
        IntentKind Kind,
        string? ZielAgent,
        string? Auftrag,
        string? Zeit,
        string? Rueckfrage,
        double Confidence);

    /// <summary>
    /// Das Verstehens-Gehirn des Hauptagenten (Boss-Agent-Ueberarbeitung Phase 1). Laeuft auf
    /// der Brain-Rolle (starkes Modell, NICHT Flash-lite — Almanach orchestrator-agent 1.1/1.6)
    /// und liefert eine schema-gebundene Deutung der Aussage MIT Gespraechskontext (gegen
    /// "Lost in Conversation", Almanach 5.5). Gibt bei Fehlern null zurueck — der Aufrufer
    /// faellt dann verlustfrei auf die bisherige Pipeline (IntentDetector + Regex) zurueck.
    /// </summary>
    public sealed class UnderstandingDetector
    {
        private readonly ILlmProvider _provider;
        private readonly string? _timeZoneId;

        public UnderstandingDetector(ILlmProvider provider, string? timeZoneId = null)
        {
            _provider = provider;
            _timeZoneId = timeZoneId;
        }

        /// <summary>Name des Brain-Providers (fuer Turn-Trace/Diagnose).</summary>
        public string ProviderName => _provider.Name;

        /// <summary>
        /// Versteht die Aussage strukturiert. null = Analyse fehlgeschlagen/unbrauchbar —
        /// Aufrufer nutzt die alte Pipeline (funktionserhaltend, Direktive #3).
        /// </summary>
        public async Task<Understanding?> AnalyzeAsync(string text, ChatSession? session,
            IReadOnlyList<ISubAgent>? agents, CancellationToken ct = default)
        {
            if (string.IsNullOrWhiteSpace(text)) return null;
            try
            {
                var msgs = new[]
                {
                    new LlmMessage(LlmRole.System, BuildSystemPrompt(agents, session, _timeZoneId)),
                    new LlmMessage(LlmRole.User, text)
                };
                var reply = await _provider.ChatAsync(msgs, ct).ConfigureAwait(false);
                var u = Parse(reply);
                Probe.That(u != null, "Understanding: LLM-Antwort nicht parsebar — alte Pipeline uebernimmt",
                    new { provider = _provider.Name, reply = reply?.Trim() });
                if (u != null)
                {
                    // Live-Logik-Sonde (Intent-Verifikation): WAS hat das Brain verstanden?
                    Log.Info("CHECKPOINT Verstehens-Schritt", new
                    {
                        step = "Aussage verstehen",
                        intent = "Aussage korrekt deuten + Auftrag normalisieren (kein Rohtext-Echo)",
                        expected = "kind erkannt, bei Aufgabe ein vollstaendiger Auftrag",
                        kind = u.Kind.ToString(),
                        zielAgent = u.ZielAgent,
                        auftrag = u.Auftrag,
                        zeit = u.Zeit,
                        confidence = u.Confidence,
                        ok = u.Kind != IntentKind.Unknown && (u.Kind != IntentKind.Task || !string.IsNullOrWhiteSpace(u.Auftrag))
                    });
                }
                return u;
            }
            catch (Exception ex)
            {
                Log.Error("Understanding: Analyse fehlgeschlagen — alte Pipeline uebernimmt", ex);
                return null;
            }
        }

        /// <summary>Baut den System-Prompt mit Helfer-Liste, Zeit und Gespraechskontext. Testbar.</summary>
        public static string BuildSystemPrompt(IReadOnlyList<ISubAgent>? agents, ChatSession? session, string? timeZoneId)
        {
            var sb = new StringBuilder();
            sb.AppendLine("Du bist das Verstehens-Gehirn eines deutschen Sprach-Assistenten. Du bekommst die per Spracherkennung transkribierte Aussage des Nutzers (Frank) und den Gespraechskontext. Deine Aufgabe: GENAU verstehen, was gemeint ist — auch bei Transkriptionsfehlern — und das Ergebnis als JSON liefern. Du antwortest NICHT selbst auf die Aussage.");
            sb.AppendLine();
            sb.AppendLine("Antworte AUSSCHLIESSLICH mit einem JSON-Objekt, sonst nichts:");
            sb.AppendLine("{\"kind\":\"AUFGABE|FRAGE|PLAUDEREI\",\"zielAgent\":\"...oder null\",\"auftrag\":\"...oder null\",\"zeit\":\"...oder null\",\"rueckfrage\":\"...oder null\",\"confidence\":0.0}");
            sb.AppendLine();
            sb.AppendLine("Regeln:");
            sb.AppendLine("- kind: AUFGABE wenn etwas getan, geaendert, gestartet, gemerkt oder erledigt werden soll (auch eine Korrektur einer vorherigen Aufgabe!); FRAGE bei Wissens-/Verstaendnisfragen; PLAUDEREI bei Smalltalk. Im Zweifel zwischen AUFGABE und FRAGE: AUFGABE.");
            sb.AppendLine("- auftrag (nur bei AUFGABE, sonst null): formuliere den Auftrag VOLLSTAENDIG und in sich verstaendlich. Korrigiere offensichtliche Transkriptionsfehler aus dem Kontext (z. B. 'rogen' -> 'morgen'). Loese Bezuege aus dem Gespraechsverlauf auf: sagt der Nutzer 'nein, mach es um 10 Uhr', dann ist der auftrag die KORRIGIERTE komplette Aufgabe aus dem Verlauf. Schreibe Uhrzeiten IMMER als 'um HH:MM' und nutze das Wort 'morgen' fuer den naechsten Tag.");
            sb.AppendLine("- zielAgent: EXAKT einer der unten gelisteten Helfer-Namen, wenn er eindeutig zustaendig ist — sonst null. Niemals einen Namen erfinden.");
            sb.AppendLine("- zeit: die verstandene Zeitangabe ('um HH:MM', 'morgen um HH:MM', 'beim naechsten Start') oder null.");
            sb.AppendLine("- rueckfrage: EINE kurze, natuerlich gesprochene Bestaetigungsfrage, die das VERSTANDENE nennt (nicht den Rohtext wiederholen). Nur bei AUFGABE, sonst null.");
            sb.AppendLine("- confidence: 0.0 bis 1.0 — wie sicher du bist, die Absicht richtig verstanden zu haben.");

            if (agents != null && agents.Count > 0)
            {
                sb.AppendLine();
                sb.AppendLine("Verfuegbare Helfer (Name: Zustaendigkeit | Anwendungs-Beispiele):");
                // HowTo mitgeben: die Beispiel-Saetze machen die zielAgent-Zuordnung freier
                // Formulierungen treffsicherer (gleiche Quelle wie der Boss-Prompt — Registry).
                foreach (var a in agents)
                    sb.AppendLine(string.IsNullOrWhiteSpace(a.HowTo)
                        ? $"- {a.Name}: {a.Description}"
                        : $"- {a.Name}: {a.Description} | {a.HowTo}");
            }

            sb.AppendLine();
            sb.Append(TimeContext.BuildBlock(timeZoneId));

            // Gespraechskontext: Zusammenfassung + die letzten Turns (kompakt), damit Bezuege
            // ("nein, lieber um 10") und Korrekturen aufgeloest werden koennen (Almanach 5.5).
            if (session != null)
            {
                if (!string.IsNullOrWhiteSpace(session.Summary))
                {
                    sb.AppendLine();
                    sb.AppendLine("ZUSAMMENFASSUNG DES BISHERIGEN GESPRAECHS:");
                    sb.AppendLine(session.Summary);
                }
                var history = session.History;
                if (history.Count > 0)
                {
                    sb.AppendLine();
                    sb.AppendLine("LETZTE GESPRAECHS-TURNS (aelteste zuerst):");
                    int start = Math.Max(0, history.Count - 8);   // die letzten 8 Nachrichten reichen fuer Bezuege
                    for (int i = start; i < history.Count; i++)
                    {
                        var m = history[i];
                        var who = m.Role == LlmRole.User ? "Nutzer" : "Assistent";
                        var content = m.Text ?? string.Empty;
                        if (content.Length > 400) content = content.Substring(0, 400) + " …";
                        sb.AppendLine($"{who}: {content}");
                    }
                }
            }
            return sb.ToString();
        }

        private sealed class Dto
        {
            public string? Kind { get; set; }
            public string? ZielAgent { get; set; }
            public string? Auftrag { get; set; }
            public string? Zeit { get; set; }
            public string? Rueckfrage { get; set; }
            public double? Confidence { get; set; }
        }

        private static readonly JsonSerializerOptions JsonOpts = new() { PropertyNameCaseInsensitive = true };

        /// <summary>
        /// Liest das Understanding aus der LLM-Antwort. Tolerant gegen umgebenden Text/Code-Fences
        /// (erstes '{' bis letztes '}', wie AgentBuilder). null = unbrauchbar. Public static = testbar.
        /// </summary>
        public static Understanding? Parse(string? reply)
        {
            if (string.IsNullOrWhiteSpace(reply)) return null;
            int start = reply.IndexOf('{');
            int end = reply.LastIndexOf('}');
            if (start < 0 || end <= start) return null;
            var json = reply.Substring(start, end - start + 1);
            try
            {
                var dto = JsonSerializer.Deserialize<Dto>(json, JsonOpts);
                if (dto == null) return null;
                var kind = ParseKind(dto.Kind);
                if (kind == IntentKind.Unknown) return null;   // ohne Kategorie ist die Deutung wertlos
                return new Understanding(
                    kind,
                    Clean(dto.ZielAgent),
                    Clean(dto.Auftrag),
                    Clean(dto.Zeit),
                    Clean(dto.Rueckfrage),
                    Math.Clamp(dto.Confidence ?? 0.0, 0.0, 1.0));
            }
            catch (JsonException)
            {
                return null;
            }
        }

        /// <summary>"null"/leer -> null; sonst getrimmt.</summary>
        private static string? Clean(string? s)
        {
            if (string.IsNullOrWhiteSpace(s)) return null;
            var t = s.Trim();
            return t.Equals("null", StringComparison.OrdinalIgnoreCase) ? null : t;
        }

        /// <summary>Kategorie-Text -> IntentKind (tolerant). Public static = testbar.</summary>
        public static IntentKind ParseKind(string? kind)
        {
            if (string.IsNullOrWhiteSpace(kind)) return IntentKind.Unknown;
            var u = kind.ToUpperInvariant();
            if (u.Contains("AUFGABE")) return IntentKind.Task;
            if (u.Contains("FRAGE")) return IntentKind.Question;
            if (u.Contains("PLAUDEREI")) return IntentKind.Chat;
            return IntentKind.Unknown;
        }
    }
}
