using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class SubAgentRouterTests
    {
        private sealed class NamedSub : ISubAgent
        {
            public NamedSub(string name, bool keyword = false) { Name = name; _keyword = keyword; }
            private readonly bool _keyword;
            public bool Executed;
            public string Name { get; }
            public string Description => $"Helfer {Name}";
            public string HowTo => $"Sag '{Name}'.";
            public bool CanHandle(string task) => _keyword;
            public string ConfirmationQuestion(string task) => $"Soll ich {Name} machen?";
            public Task<string> HandleAsync(string task, CancellationToken ct = default)
            {
                Executed = true;
                return Task.FromResult("erledigt von " + Name);
            }
        }

        /// <summary>LLM-Stub, der immer eine fest vorgegebene Antwort liefert.</summary>
        private sealed class StubLlm : ILlmProvider
        {
            private readonly string _answer;
            public StubLlm(string answer) => _answer = answer;
            public string Name => "Stub";
            public Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
                => Task.FromResult(_answer);
        }

        private static IReadOnlyList<ISubAgent> TwoAgents()
            => new ISubAgent[] { new NamedSub("Erinnerung"), new NamedSub("Notiz") };

        [Fact]
        public void MatchName_ExactName()
            => Assert.Equal("Notiz", SubAgentRouter.MatchName("Notiz", TwoAgents()));

        [Fact]
        public void MatchName_NameInSentence()
            => Assert.Equal("Notiz", SubAgentRouter.MatchName("Der passende Helfer ist Notiz.", TwoAgents()));

        [Fact]
        public void MatchName_Keiner_IsNull()
            => Assert.Null(SubAgentRouter.MatchName("KEINER", TwoAgents()));

        [Fact]
        public void MatchName_Unknown_IsNull()
            => Assert.Null(SubAgentRouter.MatchName("Hauptagent", TwoAgents()));

        [Fact]
        public void MatchName_EmptyOrNull_IsNull()
        {
            Assert.Null(SubAgentRouter.MatchName("", TwoAgents()));
            Assert.Null(SubAgentRouter.MatchName(null, TwoAgents()));
        }

        [Fact]
        public void BuildSystemPrompt_ListsAgentsAndKeiner()
        {
            var p = SubAgentRouter.BuildSystemPrompt(TwoAgents());
            Assert.Contains("Erinnerung", p);
            Assert.Contains("Notiz", p);
            Assert.Contains("KEINER", p);
        }

        [Fact]
        public async Task Route_PicksNamedAgent()
        {
            var router = new SubAgentRouter(new StubLlm("Notiz"));
            var picked = await router.RouteAsync("schreib das auf", TwoAgents());
            Assert.Equal("Notiz", picked);
        }

        [Fact]
        public async Task Route_Keiner_ReturnsNull()
        {
            var router = new SubAgentRouter(new StubLlm("KEINER"));
            Assert.Null(await router.RouteAsync("erzaehl einen Witz", TwoAgents()));
        }

        [Fact]
        public async Task Boss_Router_PicksAgent_EvenWhenKeywordDoesNotMatch()
        {
            // Keyword matcht NIE (CanHandle=false), aber der Router waehlt den Helfer per LLM.
            var sub = new NamedSub("Notiz", keyword: false);
            var reg = new SubAgentRegistry();
            reg.Register(sub);
            var router = new SubAgentRouter(new StubLlm("Notiz"));
            var boss = new BossAgent(new StubLlm("egal"), systemPrompt: null, memory: null,
                subAgents: reg, timeZoneId: null, session: null, router: router);

            var r = await boss.HandleAsync("kannst du dir das bitte aufschreiben", IntentKind.Task);

            Assert.True(boss.HasPendingConfirmation);          // Router hat den Helfer gefunden -> Rueckfrage
            Assert.Equal("Soll ich Notiz machen?", r.Text);
            Assert.False(sub.Executed);                        // erst nach Bestaetigung
        }

        [Fact]
        public async Task Boss_Router_Keiner_FallsBackToKeyword()
        {
            // Router sagt KEINER, aber das Stichwort-Matching (CanHandle=true) findet den Helfer.
            var sub = new NamedSub("Notiz", keyword: true);
            var reg = new SubAgentRegistry();
            reg.Register(sub);
            var router = new SubAgentRouter(new StubLlm("KEINER"));
            var boss = new BossAgent(new StubLlm("egal"), systemPrompt: null, memory: null,
                subAgents: reg, timeZoneId: null, session: null, router: router);

            var r = await boss.HandleAsync("notier das", IntentKind.Task);

            Assert.True(boss.HasPendingConfirmation);          // Keyword-Fallback fand den Helfer
            Assert.Equal("Soll ich Notiz machen?", r.Text);
        }
    }
}
