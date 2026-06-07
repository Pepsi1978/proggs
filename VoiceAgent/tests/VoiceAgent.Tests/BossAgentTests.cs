using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    /// <summary>Test-Doppel fuer ILlmProvider — gibt eine feste Antwort zurueck, ohne Netzwerk.</summary>
    internal sealed class FakeProvider : ILlmProvider
    {
        private readonly string _reply;
        public FakeProvider(string reply) => _reply = reply;
        public string Name => "Fake";
        public Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
            => Task.FromResult(_reply);
    }

    public class BossAgentTests
    {
        [Fact]
        public async Task RespondAsync_AddsUserAndAssistantToHistory()
        {
            var agent = new BossAgent(new FakeProvider("Antwort"), "SYS");
            var reply = await agent.RespondAsync("Hallo");

            Assert.Equal("Antwort", reply);
            Assert.Equal(2, agent.History.Count);
            Assert.Equal(LlmRole.User, agent.History[0].Role);
            Assert.Equal("Hallo", agent.History[0].Text);
            Assert.Equal(LlmRole.Assistant, agent.History[1].Role);
        }

        [Fact]
        public void BuildMessages_StartsWithSystemPrompt()
        {
            var agent = new BossAgent(new FakeProvider("x"), "MEIN-PROMPT");
            var msgs = agent.BuildMessages();
            Assert.Equal(LlmRole.System, msgs[0].Role);
            Assert.Equal("MEIN-PROMPT", msgs[0].Text);
        }

        [Fact]
        public void EmptySystemPrompt_FallsBackToDefault()
        {
            var agent = new BossAgent(new FakeProvider("x"), "");
            Assert.Equal(BossAgentPrompt.Default, agent.BuildMessages()[0].Text);
        }

        [Fact]
        public async Task Reset_ClearsHistory()
        {
            var agent = new BossAgent(new FakeProvider("ok"), "SYS");
            await agent.RespondAsync("eins");
            agent.Reset();
            Assert.Empty(agent.History);
        }
    }
}
