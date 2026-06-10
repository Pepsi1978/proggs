using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    /// <summary>
    /// Verifiziert die Rueckfrage-Bremse (Manifest Abschnitt 6): eine erkannte Aufgabe wird
    /// ERST nach dem gesprochenen "Ja" ausgefuehrt — nie sofort.
    /// </summary>
    public class ConfirmationGateTests
    {
        private sealed class FakeLlm : ILlmProvider
        {
            public string Name => "Fake";
            public Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
                => Task.FromResult("Hauptagent-Antwort");
        }

        private sealed class FakeSub : ISubAgent
        {
            public bool Executed;
            public string Name => "Fake";
            public string Description => "Test-Unteragent";
            public string HowTo => "Sag 'test'.";
            public bool CanHandle(string task) => task.ToLowerInvariant().Contains("test");
            public string ConfirmationQuestion(string task) => "Soll ich das testen?";
            public Task<string> HandleAsync(string task, CancellationToken ct = default)
            {
                Executed = true;
                return Task.FromResult("Erledigt: " + task);
            }
        }

        private static (BossAgent boss, FakeSub sub) Build()
        {
            var sub = new FakeSub();
            var reg = new SubAgentRegistry();
            reg.Register(sub);
            return (new BossAgent(new FakeLlm(), systemPrompt: null, memory: null, subAgents: reg), sub);
        }

        [Fact]
        public async Task Task_AsksBack_ThenExecutesOnYes()
        {
            var (boss, sub) = Build();

            var r1 = await boss.HandleAsync("Mach den Test", IntentKind.Task);
            Assert.False(sub.Executed);                 // NICHT sofort ausgefuehrt
            Assert.True(boss.HasPendingConfirmation);
            Assert.Equal("Soll ich das testen?", r1.Text);
            Assert.Null(r1.DelegatedTo);

            var r2 = await boss.HandleAsync("ja", IntentKind.Chat);
            Assert.True(sub.Executed);                   // erst jetzt ausgefuehrt
            Assert.False(boss.HasPendingConfirmation);
            Assert.Equal("Fake", r2.DelegatedTo);
            Assert.Contains("Erledigt", r2.Text);
        }

        [Fact]
        public async Task Task_AsksBack_ThenCancelsOnNo()
        {
            var (boss, sub) = Build();

            await boss.HandleAsync("Mach den Test", IntentKind.Task);
            var r = await boss.HandleAsync("nein lass", IntentKind.Chat);

            Assert.False(sub.Executed);
            Assert.False(boss.HasPendingConfirmation);
            Assert.Contains("lasse ich", r.Text);
        }

        [Fact]
        public async Task Task_AsksBack_UnclearAnswer_FallsToMainAgent()
        {
            var (boss, sub) = Build();

            await boss.HandleAsync("Mach den Test", IntentKind.Task);
            var r = await boss.HandleAsync("Wie spaet ist es", IntentKind.Question);

            Assert.False(sub.Executed);                  // unklare Antwort fuehrt NICHT aus
            Assert.False(boss.HasPendingConfirmation);   // Rueckfrage wurde fallen gelassen
            Assert.Equal("Hauptagent-Antwort", r.Text);  // neues Thema -> Hauptagent
        }
    }
}
