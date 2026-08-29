using System;
using System.IO;
using System.Linq;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Das persoenliche Woerterbuch aus dem SK-Ordner, als Wortliste.
    ///
    /// Dieselbe Datei und derselbe Ein/Aus-Schalter, die auch die
    /// Gemini-Textkorrektur benutzt (<see cref="GeminiClient"/> liest sie dort
    /// als Prompt-Block). Beide Gemini-Transkriptionswege geben die Liste
    /// dagegen als customVocabulary direkt an die Erkennung — die Begriffe
    /// werden damit schon beim Zuhoeren richtig geschrieben, statt erst in der
    /// Nachkorrektur repariert zu werden.
    /// </summary>
    public static class PersonalVocabulary
    {
        // Die APIs nehmen bis zu 1000 Begriffe; laut Google sind die Ergebnisse
        // bis etwa 100 am besten. Deckel daher bei 100.
        private const int MaxWords = 100;

        private static string Dir => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            "SK", "VoiceOverlays");

        /// <summary>
        /// Die Begriffe, oder ein leeres Feld, wenn der Woerterbuch-Schalter
        /// aus ist oder die Datei fehlt. Wirft nie — ohne Woerterbuch zu
        /// transkribieren ist besser als gar nicht.
        /// </summary>
        public static string[] Load()
        {
            try
            {
                var togglePath = Path.Combine(Dir, "vocabulary-enabled.txt");
                if (!File.Exists(togglePath) ||
                    !string.Equals(File.ReadAllText(togglePath).Trim(), "true", StringComparison.OrdinalIgnoreCase))
                    return Array.Empty<string>();

                var vocabPath = Path.Combine(Dir, "personal-vocabulary.txt");
                if (!File.Exists(vocabPath)) return Array.Empty<string>();

                return File.ReadAllText(vocabPath)
                    .Split(new[] { '\n', '\r', ',', ';' }, StringSplitOptions.RemoveEmptyEntries)
                    .Select(w => w.Trim())
                    .Where(w => w.Length > 0 && !w.StartsWith('#'))
                    .Distinct(StringComparer.OrdinalIgnoreCase)
                    .Take(MaxWords)
                    .ToArray();
            }
            catch
            {
                return Array.Empty<string>();
            }
        }
    }
}
