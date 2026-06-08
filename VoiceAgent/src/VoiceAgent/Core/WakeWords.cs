using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Unterstuetzte Weckwoerter mit ihren VOR-TOKENISIERTEN BPE-Keywords.
    ///
    /// Hintergrund (Bug-Almanach wake-word #10/#11/§1.3): Das KWS-Modell (sherpa-onnx
    /// gigaspeech-3.3M, englische Phonetik) erkennt NICHT Klartext, sondern BPE-Tokens. Ein neues
    /// Weckwort muss mit dem mitgelieferten <c>bpe.model</c> (SentencePiece) tokenisiert werden.
    /// Frueher war die <c>keywords.txt</c> fest auf "Okay Computer" — das Aendern des Weckworts in
    /// den Einstellungen hatte deshalb KEINE Wirkung (die Erkennung blieb beim alten Wort).
    ///
    /// Loesung: Diese Tokens wurden EINMAL mit genau diesem <c>bpe.model</c> erzeugt (Python
    /// sentencepiece 0.2.1, verifiziert 2026-06-08) und sind damit garantiert die Tokens, die die
    /// Engine erwartet. Beim Speichern eines Weckworts wird die passende <c>keywords.txt</c>
    /// geschrieben und die Engine neu geladen. KEIN <c>@original</c>-Marker (Almanach #32).
    ///
    /// Englische Phonetik, weil das Modell englisch ist — deutsche Woerter wuerden phonetisch
    /// schlecht passen. "Computer" funktioniert in beiden Sprachen aehnlich.
    /// </summary>
    public static class WakeWords
    {
        /// <summary>Anzeigename → vor-tokenisierte BPE-Keywords (Reihenfolge = Auswahl-Reihenfolge).</summary>
        public static readonly IReadOnlyList<(string Name, string Tokens)> All = new[]
        {
            ("Okay Computer",  "▁OKAY ▁COMP U TER"),
            ("Computer",       "▁COMP U TER"),
            ("Hey Computer",   "▁HE Y ▁COMP U TER"),
            ("Hello Computer", "▁HE LL O ▁COMP U TER"),
            ("Jarvis",         "▁JA R VI S"),
            ("Hey Jarvis",     "▁HE Y ▁JA R VI S"),
            ("Assistant",      "▁AS S IST ANT"),
            ("Hey Assistant",  "▁HE Y ▁AS S IST ANT"),
            ("Okay Assistant", "▁OKAY ▁AS S IST ANT"),
        };

        /// <summary>Standard-Weckwort (Fallback, wenn ein gespeicherter Wert nicht in der Liste steht).</summary>
        public const string Default = "Okay Computer";

        /// <summary>Liefert die Anzeigenamen (fuer die Auswahlliste in den Einstellungen).</summary>
        public static IEnumerable<string> Names => All.Select(w => w.Name);

        /// <summary>Ist dieses Weckwort bekannt/unterstuetzt (case-insensitive)?</summary>
        public static bool IsSupported(string? name)
            => !string.IsNullOrWhiteSpace(name)
               && All.Any(w => string.Equals(w.Name, name!.Trim(), StringComparison.OrdinalIgnoreCase));

        /// <summary>Die BPE-Tokens fuer ein Weckwort; faellt bei unbekanntem Wort auf den Standard zurueck.</summary>
        public static string TokensFor(string? name)
        {
            if (!string.IsNullOrWhiteSpace(name))
            {
                var hit = All.FirstOrDefault(w => string.Equals(w.Name, name!.Trim(), StringComparison.OrdinalIgnoreCase));
                if (hit.Tokens != null) return hit.Tokens;
            }
            var def = All.FirstOrDefault(w => w.Name == Default);
            return def.Tokens ?? All[0].Tokens;
        }

        /// <summary>Kanonischer Anzeigename (richtige Gross-/Kleinschreibung) oder Default.</summary>
        public static string Canonical(string? name)
        {
            if (!string.IsNullOrWhiteSpace(name))
            {
                var hit = All.FirstOrDefault(w => string.Equals(w.Name, name!.Trim(), StringComparison.OrdinalIgnoreCase));
                if (hit.Name != null) return hit.Name;
            }
            return Default;
        }

        /// <summary>
        /// Schreibt die <c>keywords.txt</c> fuer das gewaehlte Weckwort in einen BESCHREIBBAREN Pfad
        /// (%LOCALAPPDATA%\VoiceAgent\wakeword-keywords.txt) und liefert ihn zurueck. So FOLGT die
        /// Erkennung dem in den Einstellungen gewaehlten Weckwort. UTF-8 OHNE BOM (das ▁ ist U+2581),
        /// LF-Zeilenende, NUR die BPE-Tokens (kein @original — Almanach #32). Crasht nie: bei einem
        /// Schreibfehler wird die gebundelte keywords.txt im modelDir als Fallback genutzt.
        /// </summary>
        public static string EnsureKeywordsFile(string? wakeWord, string modelDir)
        {
            try
            {
                var dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "VoiceAgent");
                Directory.CreateDirectory(dir);
                var path = Path.Combine(dir, "wakeword-keywords.txt");
                File.WriteAllText(path, TokensFor(wakeWord) + "\n", new UTF8Encoding(false));
                Log.Info("Weckwort-keywords.txt erzeugt", new { wakeWord = Canonical(wakeWord), path });
                return path;
            }
            catch (Exception ex)
            {
                Log.Error("Weckwort-keywords.txt schreiben fehlgeschlagen — nutze gebundelte Datei", ex);
                return Path.Combine(modelDir, "keywords.txt");   // Fallback (Okay Computer)
            }
        }
    }
}
