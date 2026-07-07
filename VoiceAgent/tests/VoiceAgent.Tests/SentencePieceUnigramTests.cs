using System;
using System.IO;
using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    /// <summary>
    /// Verifiziert, dass der C#-Unigram-Encoder EXAKT dieselben Tokens liefert wie Python-sentencepiece
    /// (mit demselben bpe.model). Das ist der Regressionsbeweis fuer die freie Weckwort-Eingabe: solange
    /// diese Tests gruen sind, erzeugt ein frei eingetipptes Wort die Tokens, die die KWS-Engine erwartet.
    /// Erwartungswerte wurden mit `sp.encode(WORT, out_type=str)` (sentencepiece 0.2.x, bpe.model) erzeugt.
    /// </summary>
    public class SentencePieceUnigramTests
    {
        private static SentencePieceUnigram Enc()
        {
            string dir = Path.Combine(AppContext.BaseDirectory, "assets", "wakeword-model");
            var enc = SentencePieceUnigram.ForModel(dir);
            Assert.NotNull(enc);   // Vokabular muss neben dem Test liegen (Content-Copy)
            return enc!;
        }

        // Die 9 kuratierten Weckwoerter — muessen 1:1 zur vorberechneten Liste in WakeWords passen.
        [Theory]
        [InlineData("OKAY COMPUTER",  "▁OKAY ▁COMP U TER")]
        [InlineData("COMPUTER",       "▁COMP U TER")]
        [InlineData("HEY COMPUTER",   "▁HE Y ▁COMP U TER")]
        [InlineData("HELLO COMPUTER", "▁HE LL O ▁COMP U TER")]
        [InlineData("JARVIS",         "▁JA R VI S")]
        [InlineData("HEY JARVIS",     "▁HE Y ▁JA R VI S")]
        [InlineData("ASSISTANT",      "▁AS S IST ANT")]
        [InlineData("HEY ASSISTANT",  "▁HE Y ▁AS S IST ANT")]
        [InlineData("OKAY ASSISTANT", "▁OKAY ▁AS S IST ANT")]
        public void Curated_MatchesReference(string word, string expected)
            => Assert.Equal(expected, Enc().Encode(word));

        // Beliebige freie Woerter — Erwartung direkt aus Python-sentencepiece.
        [Theory]
        [InlineData("PIZZA",       "▁PI Z Z A")]
        [InlineData("HELLO WORLD", "▁HE LL O ▁WORLD")]
        [InlineData("ALEXA",       "▁A LE X A")]
        [InlineData("SIRI",        "▁S I RI")]
        [InlineData("HEY BUDDY",   "▁HE Y ▁BU D D Y")]
        [InlineData("BANANE",      "▁BA N AN E")]
        [InlineData("FRANK",       "▁F RA N K")]
        [InlineData("OKAY FRANK",  "▁OKAY ▁F RA N K")]
        [InlineData("XQZ",         "▁ X Q Z")]
        public void Free_MatchesSentencePiece(string word, string expected)
            => Assert.Equal(expected, Enc().Encode(word));

        // Grossschreibungs-Normalisierung: egal wie der Nutzer tippt, das Ergebnis ist das Uppercase-Token.
        [Theory]
        [InlineData("computer",       "▁COMP U TER")]
        [InlineData("Computer",       "▁COMP U TER")]
        [InlineData("  computer  ",   "▁COMP U TER")]   // fuehrende/nachlaufende Leerzeichen weg
        [InlineData("okay computer",  "▁OKAY ▁COMP U TER")]
        [InlineData("okay   computer","▁OKAY ▁COMP U TER")]   // Mehrfach-Leerzeichen zusammengefasst
        public void Normalizes_CaseAndWhitespace(string input, string expected)
            => Assert.Equal(expected, Enc().Encode(input));

        // Zeichen ausserhalb des englischen Vokabulars → nicht erkennbar (null), kein stiller Muell.
        [Theory]
        [InlineData("ÄÖÜ")]
        [InlineData("café")]
        [InlineData("Grüße")]
        [InlineData("")]
        [InlineData("   ")]
        public void Unencodable_ReturnsNull(string input)
            => Assert.Null(Enc().Encode(input));

        [Fact]
        public void CanEncode_ReflectsEncode()
        {
            var e = Enc();
            Assert.True(e.CanEncode("HEY BUDDY"));
            Assert.False(e.CanEncode("Grüße"));
        }
    }
}
