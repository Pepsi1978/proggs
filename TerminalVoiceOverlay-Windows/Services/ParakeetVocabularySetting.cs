using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Das Woerterbuch der Offline-Transkription (Parakeet primeline). Eigener Schalter und eigene Liste,
    /// getrennt von vocabulary-enabled.txt / personal-vocabulary.txt, die zur Gemini-Korrektur gehoeren.
    ///
    /// Beides lebt als winzige Datei im geteilten SK-Ordner, wirkt also ab der naechsten Aufnahme und gilt
    /// fuer TVO und CVO zugleich:
    ///   parakeet-vocabulary-enabled.txt  "false" = aus, sonst an; fehlt die Datei: an
    ///   parakeet-vocabulary.txt          Begriffe durch Komma oder Zeilenumbruch getrennt; fehlt die Datei: Startliste
    ///
    /// Gleiche Datei in TerminalVoiceOverlay-Windows und ClaudeVoiceOverlay-Windows (nur der Namespace weicht ab),
    /// Gegenstueck in den macOS-Overlays: ParakeetVocabularySetting.swift.
    /// </summary>
    public static class ParakeetVocabularySetting
    {
        /// <summary>Startliste, gleicher Stand wie ParakeetUeberJarvis.STANDARD_WOERTERBUCH (Modul M1.4).</summary>
        public const string StarterList = "Bug-Almanach, Sonnet-Schwarm, Groq, Whisper, APK-Update, UpdateStation, OpenLauncher, Jarvis, debugging, Logcat, Harness, Best Practices, VCO, CVO, ChatGPT, Claude, Claude Code, OpenAI, Anthropic";

        /// <summary>Wie am Handy: mehr als 100 Begriffe gehen nicht in die Beam-Suche.</summary>
        private const int MaxWords = 100;

        private static string Dir => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "SK", "VoiceOverlays");
        private static string EnabledPath => Path.Combine(Dir, "parakeet-vocabulary-enabled.txt");
        private static string VocabularyPath => Path.Combine(Dir, "parakeet-vocabulary.txt");

        public static bool IsEnabled
        {
            get
            {
                try
                {
                    var raw = AtomicFile.ReadAllTextShared(EnabledPath);
                    return raw is null || !string.Equals(raw.Trim(), "false", StringComparison.OrdinalIgnoreCase);
                }
                catch { return true; }
            }
        }

        /// <summary>Der Text fuers Einstellungsfeld. Eine leere Datei bleibt leer: Das ist eine bewusst geleerte Liste.</summary>
        public static string LoadText()
        {
            try { return AtomicFile.ReadAllTextShared(VocabularyPath) ?? StarterList; }
            catch { return StarterList; }
        }

        /// <summary>
        /// Schreibt nur bei echter Aenderung: Solange nichts geaendert wird, bleibt es bei fehlender Datei
        /// und damit bei der jeweils aktuellen Startliste.
        /// </summary>
        public static void SaveEnabled(bool enabled)
        {
            if (enabled != IsEnabled) Write(EnabledPath, enabled ? "true" : "false");
        }

        public static void SaveText(string? text)
        {
            var trimmed = (text ?? string.Empty).Trim();
            if (trimmed != LoadText().Trim()) Write(VocabularyPath, trimmed);
        }

        private static void Write(string path, string text)
        {
            try
            {
                Directory.CreateDirectory(Dir);
                // Gleiche Sperre wie der Drive-Abgleich des Prompt-Bundles, in dem diese Dateien mitreisen.
                using var held = CrossProcessLock.Acquire(SharedLockNames.GeminiPromptFiles,
                    TimeSpan.FromSeconds(2), throwOnTimeout: false);
                AtomicFile.WriteAllText(path, text + "\n");
            }
            catch (Exception ex)
            {
                DiagLog.Warn("Settings", "parakeet_vocabulary_save_failed", ("error", ex.Message));
            }
        }

        /// <summary>Die Wortliste fuer die naechste Aufnahme; leer, wenn der Schalter aus ist oder keine Begriffe da sind.</summary>
        public static string Hotwords() => IsEnabled ? Format(LoadText()) : string.Empty;

        /// <summary>
        /// Format von sherpa-onnx: Begriffe durch "/" getrennt. "/", ":" und "#" haben dort eine eigene Bedeutung
        /// und werden zu Leerzeichen. Doppelte (ohne Gross/Klein) fallen weg, hoechstens 100 Begriffe.
        /// </summary>
        public static string Format(string text)
        {
            var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            var words = text.Split('\n', '\r', ',', ';')
                .Select(t => t.Replace('/', ' ').Replace(':', ' ').Replace('#', ' ').Trim())
                .Where(w => w.Length > 0 && seen.Add(w))
                .Take(MaxWords);
            return string.Join("/", words);
        }
    }
}
