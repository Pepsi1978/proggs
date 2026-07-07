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
        private static BossAgent NewAgent(ILlmProvider p, string? prompt)
            => new BossAgent(p, prompt, session: new ChatSession());

        [Fact]
        public async Task RespondAsync_AddsUserAndAssistantToHistory()
        {
            var agent = NewAgent(new FakeProvider("Antwort"), "SYS");
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
            var agent = NewAgent(new FakeProvider("x"), "MEIN-PROMPT");
            var msgs = agent.BuildMessages();
            Assert.Equal(LlmRole.System, msgs[0].Role);
            Assert.StartsWith("MEIN-PROMPT", msgs[0].Text);
        }

        [Fact]
        public void EmptySystemPrompt_FallsBackToDefault()
        {
            var agent = NewAgent(new FakeProvider("x"), "");
            Assert.StartsWith(BossAgentPrompt.Default, agent.BuildMessages()[0].Text);
        }

        [Fact]
        public async Task BuildMessages_IncludesSummaryBlock_WhenPresent()
        {
            var session = new ChatSession { Summary = "Frank plant eine Reise." };
            var agent = new BossAgent(new FakeProvider("ok"), "SYS", session: session);
            await agent.RespondAsync("Und weiter?");
            var msgs = agent.BuildMessages();
            // Ein zweiter System-Block traegt die Zusammenfassung.
            Assert.Contains(msgs, m => m.Role == LlmRole.System && m.Text.Contains("Frank plant eine Reise."));
        }

        [Fact]
        public async Task RebuildingAgent_KeepsSessionHistory()
        {
            var session = new ChatSession();
            var agent1 = new BossAgent(new FakeProvider("ok"), "SYS", session: session);
            await agent1.RespondAsync("Erste Nachricht");
            Assert.Equal(2, session.History.Count);

            // Simuliert "Einstellungen gespeichert -> Agent neu gebaut" mit DERSELBEN aktiven Session.
            var agent2 = new BossAgent(new FakeProvider("ok"), "SYS-GEAENDERT", session: session);
            Assert.Equal(2, agent2.History.Count);                 // Verlauf bleibt erhalten
            await agent2.RespondAsync("Zweite Nachricht");
            Assert.Equal(4, session.History.Count);
        }
    }
}
