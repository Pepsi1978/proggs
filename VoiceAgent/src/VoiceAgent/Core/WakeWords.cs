using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Weckwoerter mit ihren VOR-TOKENISIERTEN Keywords (kuratierte Vorschlaege) plus FREIE Eingabe.
    ///
    /// Hintergrund (Bug-Almanach wake-word #10/#34): Das KWS-Modell (sherpa-onnx gigaspeech-3.3M,
    /// englische Phonetik) erkennt NICHT Klartext, sondern Tokens. Ein Weckwort muss mit dem
    /// mitgelieferten <c>bpe.model</c> (SentencePiece, model_type=UNIGRAM) tokenisiert werden.
    ///
    /// Zwei Wege, beide liefern garantiert dieselben Tokens:
    /// 1. <b>Kuratierte Liste</b> (unten): EINMAL mit genau diesem <c>bpe.model</c> erzeugt
    ///    (Python sentencepiece, verifiziert 2026-06-08) — schnell und ohne Datei-Zugriff.
    /// 2. <b>Freie Eingabe</b>: Frank tippt ein eigenes Wort, das zur Laufzeit ueber
    ///    <see cref="SentencePieceUnigram"/> tokenisiert wird (verifiziert: identisch zu sentencepiece,
    ///    siehe <c>SentencePieceUnigramTests</c>). KEIN <c>@original</c>-Marker (Almanach #32).
    ///
    /// Englische Phonetik, weil das Modell englisch ist; der Encoder normalisiert zu Grossschreibung
    /// ("Computer" → "▁COMP U TER"). Worte mit Zeichen ausserhalb des englischen Vokabulars (Umlaute
    /// usw.) sind nicht erkennbar und werden in der UI abgelehnt.
    /// </summary>
    public static class WakeWords
    {
        /// <summary>Vorschlagsname → vor-tokenisierte Keywords (Reihenfolge = Auswahl-Reihenfolge).</summary>
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

        /// <summary>Standard-Weckwort (Fallback, wenn ein gespeicherter Wert nicht nutzbar ist).</summary>
        public const string Default = "Okay Computer";

        /// <summary>Liefert die Vorschlags-Anzeigenamen (fuer die editierbare Auswahlliste).</summary>
        public static IEnumerable<string> Names => All.Select(w => w.Name);

        /// <summary>Ist dieses Weckwort ein kuratierter Vorschlag (case-insensitive)?</summary>
        public static bool IsSupported(string? name)
            => !string.IsNullOrWhiteSpace(name)
               && All.Any(w => string.Equals(w.Name, name!.Trim(), StringComparison.OrdinalIgnoreCase));

        /// <summary>
        /// Die Tokens fuer ein Weckwort: zuerst die kuratierte Liste (schnell, garantiert), sonst
        /// freie Laufzeit-Tokenisierung via <see cref="SentencePieceUnigram"/>. Gibt null zurueck,
        /// wenn das Wort nicht erkennbar ist (Zeichen ausserhalb des englischen Vokabulars).
        /// </summary>
        public static string? TokensFor(string? name, string modelDir)
        {
            if (!string.IsNullOrWhiteSpace(name))
            {
                var hit = All.FirstOrDefault(w => string.Equals(w.Name, name!.Trim(), StringComparison.OrdinalIgnoreCase));
                if (hit.Tokens != null) return hit.Tokens;
            }
            // Frei eingegeben: zur Laufzeit tokenisieren (Grossschreibungs-Normalisierung im Encoder).
            return SentencePieceUnigram.ForModel(modelDir)?.Encode(name);
        }

        /// <summary>
        /// Kann dieses (frei eingegebene) Weckwort erkannt werden? Kuratierte Vorschlaege immer;
        /// freie Worte nur, wenn alle Zeichen ins englische Modell-Vokabular passen. Fuer die UI-Pruefung.
        /// </summary>
        public static bool CanRecognize(string? name, string modelDir)
            => IsSupported(name) || SentencePieceUnigram.ForModel(modelDir)?.CanEncode(name) == true;

        /// <summary>
        /// Anzeige-/Speicherform: kuratierte Vorschlaege in kanonischer Schreibweise, frei eingegebene
        /// Worte getrimmt durchreichen (NICHT auf den Standard zwingen — sonst ginge die freie Eingabe verloren).
        /// </summary>
        public static string Canonical(string? name)
        {
            if (string.IsNullOrWhiteSpace(name)) return Default;
            var hit = All.FirstOrDefault(w => string.Equals(w.Name, name!.Trim(), StringComparison.OrdinalIgnoreCase));
            return hit.Name ?? name!.Trim();
        }

        /// <summary>
        /// Schreibt die <c>keywords.txt</c> fuer das gewaehlte Weckwort in einen BESCHREIBBAREN Pfad
        /// (%LOCALAPPDATA%\VoiceAgent\wakeword-keywords.txt) und liefert ihn zurueck. So FOLGT die
        /// Erkennung dem gewaehlten (auch frei eingetippten) Weckwort. UTF-8 OHNE BOM (das ▁ ist U+2581),
        /// LF-Zeilenende, NUR die Tokens (kein @original — Almanach #32). Ist das Wort nicht erkennbar,
        /// faellt es funktionserhaltend auf den Standard zurueck (die Erkennung faellt nie ganz aus).
        /// </summary>
        public static string EnsureKeywordsFile(string? wakeWord, string modelDir)
        {
            try
            {
                string? tokens = TokensFor(wakeWord, modelDir);
                if (string.IsNullOrWhiteSpace(tokens))
                {
                    Log.Warn("Weckwort nicht tokenisierbar — nutze Standard", new { wakeWord, fallback = Default });
                    tokens = TokensFor(Default, modelDir) ?? All[0].Tokens;
                }

                var dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "VoiceAgent");
                Directory.CreateDirectory(dir);
                var path = Path.Combine(dir, "wakeword-keywords.txt");
                File.WriteAllText(path, tokens + "\n", new UTF8Encoding(false));

                // Observability-Checkpoint (Intent-Verifikation): erwartetes Wort vs. tatsaechliche Tokens.
                Log.Info("CHECKPOINT: Weckwort tokenisiert",
                    new { step = "Weckwort tokenisiert", expected = Canonical(wakeWord), actual = tokens, path, ok = true });
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
