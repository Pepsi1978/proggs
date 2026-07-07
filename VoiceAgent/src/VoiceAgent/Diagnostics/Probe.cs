using System;
using System.Runtime.CompilerServices;

namespace VoiceAgent.Diagnostics
{
    /// <summary>
    /// Wird geworfen, wenn eine Sonde verletzt wird UND Probe.Strict aktiv ist.
    /// Im Normalbetrieb (Strict=false) wird NIE geworfen — nur geloggt.
    /// </summary>
    public sealed class ProbeViolationException : Exception
    {
        public ProbeViolationException(string message) : base(message) { }
    }

    /// <summary>
    /// Logik-Sonden — das Herzstueck der Observability-Schicht. Eine Sonde prueft eine
    /// ANNAHME und protokolliert deren Verletzung — auch wenn gar kein Crash passiert.
    /// Genau hier entstehen die stillen Bugs (falscher Pfad, leere Liste, Wert ausser Bereich).
    ///
    /// Verwendung:
    /// - Vor-/Nachbedingungen an Funktionsgrenzen: Probe.That(input != null, "Eingabe fehlt").
    /// - Invarianten: Probe.That(list.Count > 0, "Liste darf hier nie leer sein", new { list.Count }).
    /// - Sanity-/Range-Checks: Probe.That(rate is > 0 and <= 48000, "SampleRate ausser Bereich", new { rate }).
    /// - Zustandsuebergaenge: Probe.State("Listening", "Transcribing").
    /// - Entscheidungen an Verzweigungen: Probe.Decision("Endpoint", "FERTIG", new { score }).
    ///
    /// Verhalten:
    /// - Verletzte Bedingung -> WARN-Eintrag mit vollem Kontext (NICHT verschluckt).
    /// - Erfuellte Bedingung -> still (kein Log-Spam).
    /// - Crasht NIE im Normalbetrieb (Strict=false). Mit Strict=true (z.B. in Tests)
    ///   wirft eine verletzte Sonde eine ProbeViolationException — zum harten Aufdecken.
    /// </summary>
    public static class Probe
    {
        /// <summary>
        /// Wenn true, wirft eine verletzte Sonde eine Exception statt nur zu loggen.
        /// Default false (Produktion). In Tests/Debug bewusst aktivierbar.
        /// </summary>
        public static bool Strict { get; set; }

        /// <summary>
        /// Prueft eine Annahme. Ist sie verletzt, wird ein WARN-Eintrag mit Kontext
        /// geschrieben (und bei Strict=true eine ProbeViolationException geworfen).
        /// </summary>
        public static void That(bool condition, string message, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
        {
            if (condition) return; // Annahme haelt -> still

            Log.Warn("Sonde verletzt: " + message, ctx, file, fn);
            if (Strict) throw new ProbeViolationException(message);
        }

        /// <summary>Protokolliert einen Zustandsuebergang (INFO), z.B. "Idle" -> "Listening".</summary>
        public static void State(string from, string to, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
        {
            Log.Info($"Zustand: {from} -> {to}", ctx, file, fn);
        }

        /// <summary>
        /// Protokolliert eine Entscheidung an einer wichtigen Verzweigung (INFO) —
        /// damit man im Nachhinein sieht, WARUM die App einen Pfad genommen hat.
        /// </summary>
        public static void Decision(string point, string chosen, object? ctx = null,
            [CallerFilePath] string file = "", [CallerMemberName] string fn = "")
        {
            Log.Info($"Entscheidung [{point}]: {chosen}", ctx, file, fn);
        }
    }
}
