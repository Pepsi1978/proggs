using System;
using System.IO;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Verwaltet die externe Faehigkeiten-Datei (capabilities.md, liegt neben der EXE): was kann
    /// der Agent, was (noch) nicht. Die Datei wird beim Start EINMAL eingelesen und gecached.
    ///
    /// Kontext-schonend (Frank-Wunsch 2026-06-07): Die Liste wird NICHT bei jedem Turn in den
    /// Prompt gehaengt, sondern NUR dann, wenn der Nutzer tatsaechlich nach den Faehigkeiten fragt
    /// (IsCapabilityQuestion). So bleibt der normale Kontext schlank, die Selbstauskunft ist aber
    /// jederzeit abrufbar.
    ///
    /// ============================ PFLEGE-REGEL (Frank, 2026-06-07) ============================
    /// capabilities.md ist die EINE Quelle der Wahrheit ueber den Funktionsumfang. Bei JEDEM
    /// neuen/geaenderten/entfernten Feature wird DIESE DATEI aktualisiert (Changelog der eigenen
    /// Faehigkeiten). Vergisst man die Pflege, gibt der Agent falsche Auskunft ueber sich selbst.
    /// =========================================================================================
    /// </summary>
    public static class AgentCapabilities
    {
        public const string FileName = "capabilities.md";

        private static string? _cached;   // einmal eingelesen, dann gecached

        /// <summary>Pfad der Faehigkeiten-Datei neben der EXE (Single-File-fest via Environment.ProcessPath, Almanach §1.1).</summary>
        public static string FilePath
        {
            get
            {
                var dir = Path.GetDirectoryName(Environment.ProcessPath) ?? AppContext.BaseDirectory;
                return Path.Combine(dir, FileName);
            }
        }

        /// <summary>Der gecachte Faehigkeiten-Text (Datei oder Fallback). Wird einmal gelesen.</summary>
        public static string Content => _cached ??= ReadFileOrFallback().Trim();

        /// <summary>
        /// Erkennt, ob der Nutzer nach den Faehigkeiten fragt — nur dann wird die Liste in den
        /// Prompt eingefuegt (Kontext sparen). Public static = isoliert testbar.
        /// </summary>
        public static bool IsCapabilityQuestion(string? userText)
        {
            if (string.IsNullOrWhiteSpace(userText)) return false;
            var t = userText.ToLowerInvariant();
            return t.Contains("was kannst du") || t.Contains("was du kannst") || t.Contains("was du alles")
                || t.Contains("kannst du") || t.Contains("kannst du das")
                || t.Contains("bist du in der lage") || t.Contains("wozu bist du")
                || t.Contains("faehigkeit") || t.Contains("fähigkeit")
                || t.Contains("welche funktion") || t.Contains("was beherrschst du")
                || t.Contains("was geht alles");
        }

        /// <summary>Baut den Faehigkeiten-Block fuer den System-Prompt (nur bei Bedarf aufrufen).</summary>
        public static string BuildBlock()
        {
            return "\n\nDEINE AKTUELLEN FAEHIGKEITEN (verlaesslich — antworte NUR auf dieser Basis: " +
                   "erfinde nichts dazu und verneine nichts, das hier als koennend steht):\n" + Content;
        }

        private static string ReadFileOrFallback()
        {
            try
            {
                var p = FilePath;
                if (File.Exists(p))
                {
                    var text = File.ReadAllText(p);
                    if (!string.IsNullOrWhiteSpace(text)) return text;
                }
                Log.Warn("Capabilities-Datei nicht gefunden — nutze eingebauten Fallback", new { path = FilePath });
            }
            catch (Exception ex)
            {
                Log.Error("Capabilities-Datei lesen fehlgeschlagen — nutze Fallback", ex);
            }
            return Fallback;
        }

        // Eingebauter Minimal-Fallback, falls capabilities.md fehlt — die App bleibt funktionsfaehig.
        private const string Fallback =
@"Das kann ich JETZT:
- Per Sprache und Text reden, vorlesen, die echte Uhrzeit nennen.
- Notizen merken und mich an Frueheres erinnern (Gedaechtnis).
- Zeitgesteuerte Erinnerungen anlegen und mich proaktiv mit Signalton melden.
Das kann ich NOCH NICHT:
- Den Computer wirklich steuern (Computer Use).
- Zur Laufzeit selbst neue Unteragenten bauen.";
    }
}
