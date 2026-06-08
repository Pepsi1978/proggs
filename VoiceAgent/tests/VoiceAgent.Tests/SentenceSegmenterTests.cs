using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class SentenceSegmenterTests
    {
        [Fact]
        public void SplitSentences_SplitsAtSentenceEnds()
        {
            var s = SentenceSegmenter.SplitSentences("Hallo, das ist ein Test. Und hier kommt noch ein Satz dazu.");
            Assert.Equal(2, s.Count);
            Assert.Equal("Hallo, das ist ein Test.", s[0]);
            Assert.Equal("Und hier kommt noch ein Satz dazu.", s[1]);
        }

        [Fact]
        public void SplitSentences_NoTerminator_ReturnsWhole()
        {
            var s = SentenceSegmenter.SplitSentences("Nur ein Fragment ohne Satzzeichen am Ende");
            Assert.Single(s);
            Assert.Equal("Nur ein Fragment ohne Satzzeichen am Ende", s[0]);
        }

        [Fact]
        public void SplitSentences_Empty_ReturnsEmpty()
        {
            Assert.Empty(SentenceSegmenter.SplitSentences(""));
            Assert.Empty(SentenceSegmenter.SplitSentences(null));
            Assert.Empty(SentenceSegmenter.SplitSentences("   "));
        }

        [Fact]
        public void SplitSentences_QuestionAndExclamation()
        {
            var s = SentenceSegmenter.SplitSentences("Moechtest du das wirklich tun? Ja, dann mache ich das gleich!");
            Assert.Equal(2, s.Count);
            Assert.EndsWith("?", s[0]);
            Assert.EndsWith("!", s[1]);
        }

        [Fact]
        public async Task Segment_Streamed_MatchesSplitSentences()
        {
            const string full = "Das ist der erste Satz. Und das hier der zweite Satz. Schluss jetzt.";
            // Als kleine Deltas einspeisen (so wie ein LLM-Token-Stream): zeichenweise.
            var streamed = await Collect(Segment(CharDeltas(full)));
            var direct = SentenceSegmenter.SplitSentences(full);
            Assert.Equal(direct, streamed);
        }

        private static IAsyncEnumerable<string> Segment(IAsyncEnumerable<string> deltas)
            => SentenceSegmenter.Segment(deltas);

        private static async IAsyncEnumerable<string> CharDeltas(string text)
        {
            foreach (var c in text)
            {
                yield return c.ToString();
                await Task.Yield();
            }
        }

        private static async Task<List<string>> Collect(IAsyncEnumerable<string> src)
        {
            var list = new List<string>();
            await foreach (var x in src) list.Add(x);
            return list;
        }
    }
}
