using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Verwaltet die externe Faehigkeiten-Datei (capabilities.md, liegt neben der EXE): was kann
    /// der Agent, was (noch) nicht. Die Datei wird beim Start EINMAL eingelesen und gecached.
    ///
    /// Die Liste wird bei JEDEM Turn in den System-Prompt gehaengt, damit der Agent NIE falsch
    /// verneint. (Eine fruehere "nur bei kannst-du-Frage"-Heuristik war zu fragil und verpasste
    /// Formulierungen wie "...erinnern kannst" -> der Agent verneinte faelschlich. capabilities.md
    /// ist kompakt genug, um verlaesslich bei jedem Turn dabei zu sein.)
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

        /// <summary>Baut den Faehigkeiten-Block fuer den System-Prompt.</summary>
        public static string BuildBlock()
        {
            return "\n\nDEINE AKTUELLEN FAEHIGKEITEN (verlaesslich — antworte NUR auf dieser Basis: " +
                   "erfinde nichts dazu und verneine nichts, das hier als koennend steht):\n" + Content;
        }

        /// <summary>
        /// Baut die LIVE-Liste der einsatzbereiten Helfer DIREKT aus der SubAgent-Registry —
        /// derselben Quelle, ueber die der Boss tatsaechlich delegiert. Diese Liste kann NIE
        /// veralten (Poka-Yoke Stufe 3): die statische capabilities.md hatte am 2026-06-10
        /// faelschlich behauptet, der Agent koenne keine Unteragenten bauen — weil die
        /// Hand-Pflege bei neuen Features vergessen wurde. Public static = isoliert testbar.
        /// </summary>
        public static string BuildHelpersBlock(IReadOnlyList<ISubAgent>? agents)
        {
            if (agents == null || agents.Count == 0) return string.Empty;
            var sb = new StringBuilder();
            sb.Append("\n\nDEINE EINSATZBEREITEN HELFER (LIVE aus dem System erzeugt — verbindlich: ")
              .Append("JEDEN dieser Helfer hast du JETZT und kannst ihn sofort beauftragen; ")
              .Append("verneine KEINEN davon. Zu jedem steht, WIE man ihn nutzt — erklaere das ")
              .Append("dem Nutzer auf Nachfrage mit genau diesen Beispielen):\n");
            foreach (var a in agents)
            {
                sb.Append("- ").Append(a.Name).Append(": ").Append(a.Description);
                if (!string.IsNullOrWhiteSpace(a.HowTo))
                    sb.Append(" | So nutzt man ihn: ").Append(a.HowTo);
                sb.Append('\n');
            }
            return sb.ToString();
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
        // WICHTIG: nichts faelschlich verneinen — die Helfer (Unteragenten bauen, Computer Use,
        // Erinnerungen, Notizen) kommen verbindlich aus der LIVE-Liste (BuildHelpersBlock).
        private const string Fallback =
@"Das kann ich JETZT:
- Per Sprache und Text reden, vorlesen, die echte Uhrzeit nennen.
- Notizen merken und mich an Frueheres erinnern (Gedaechtnis).
- Zeitgesteuerte Erinnerungen anlegen und mich proaktiv mit Signalton melden.
- Alle unten in der LIVE-Helfer-Liste genannten Helfer sofort beauftragen (z. B. neue Unteragenten bauen, Computer Use sofern in den Einstellungen aktiviert).
Das kann ich NOCH NICHT:
- Im Internet surfen oder Webseiten lesen.";
    }
}
