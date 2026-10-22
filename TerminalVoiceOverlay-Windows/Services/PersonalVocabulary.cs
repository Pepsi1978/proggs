using System;
using System.Collections.Generic;
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
        /// The ONE tokenizer for the vocabulary file, used by <see cref="Load"/> and by
        /// <see cref="PromptVocabularyDriveSync"/>. Format contract shared with macOS
        /// (PersonalVocabulary.swift / GoogleDriveBackupService.swift terms()): terms are
        /// separated by line breaks, comma and semicolon; a line whose first non-blank
        /// character is '#' is a comment; terms are trimmed and deduplicated
        /// case-insensitively, the first spelling wins. Because comma/semicolon are
        /// separators by contract, an entry such as "Mueller, Hans" is two terms.
        /// </summary>
        public static List<string> Tokenize(string? text)
        {
            var seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            var terms = new List<string>();
            if (string.IsNullOrEmpty(text)) return terms;
            foreach (var raw in text.Split('\r', '\n'))
            {
                var line = raw.Trim();
                if (line.Length == 0 || line.StartsWith('#')) continue;
                foreach (var part in SplitLine(line))
                {
                    if (seen.Add(part)) terms.Add(part);
                }
            }
            return terms;
        }

        /// <summary>The trimmed, non-empty terms of ONE non-comment line, in order.</summary>
        public static IEnumerable<string> SplitLine(string line)
        {
            foreach (var part in line.Split(',', ';'))
            {
                var term = part.Trim();
                if (term.Length > 0) yield return term;
            }
        }

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

                return Tokenize(AtomicFile.ReadAllTextShared(vocabPath))
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
