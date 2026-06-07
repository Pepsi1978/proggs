using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class ContextCompressorTests
    {
        private static ChatSession SessionWith(int messages)
        {
            var s = new ChatSession();
            for (int i = 0; i < messages; i++)
                s.History.Add(new LlmMessage(i % 2 == 0 ? LlmRole.User : LlmRole.Assistant,
                    "Nachricht Nummer " + i + " mit etwas Text damit sie zaehlt."));
            return s;
        }

        [Fact]
        public async Task UnderThreshold_NoChange()
        {
            var s = SessionWith(2);
            var comp = new ContextCompressor(budgetTokens: 100000, threshold: 0.75, keepRecent: 4);
            var changed = await comp.MaybeCompressAsync(s, new FakeProvider("ZUSAMMENFASSUNG"), CancellationToken.None);
            Assert.False(changed);
            Assert.Equal(2, s.History.Count);
            Assert.Equal("", s.Summary);
        }

        [Fact]
        public async Task OverThreshold_SummarizesOldestKeepsRecent()
        {
            var s = SessionWith(20);
            var comp = new ContextCompressor(budgetTokens: 50, threshold: 0.75, keepRecent: 4);
            var changed = await comp.MaybeCompressAsync(s, new FakeProvider("KOMPAKTE ZUSAMMENFASSUNG"), CancellationToken.None);

            Assert.True(changed);
            Assert.Equal(4, s.History.Count);                    // nur die letzten 4 bleiben wortwoertlich
            Assert.Contains("KOMPAKTE ZUSAMMENFASSUNG", s.Summary);
            Assert.Equal("Nachricht Nummer 16 mit etwas Text damit sie zaehlt.", s.History[0].Text);
        }

        [Fact]
        public void EstimateTokens_GrowsWithContent()
        {
            var small = SessionWith(1);
            var big = SessionWith(40);
            Assert.True(ContextCompressor.EstimateTokens(big) > ContextCompressor.EstimateTokens(small));
        }
    }
}
