using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class ComputerUseSubAgentTests
    {
        private sealed class StubLlm : ILlmProvider
        {
            private readonly string _answer;
            public StubLlm(string answer) => _answer = answer;
            public string Name => "Stub";
            public Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
                => Task.FromResult(_answer);
        }

        /// <summary>Zaehlt, wie oft ausgefuehrt wurde, und liefert ein fixes Ergebnis.</summary>
        private sealed class TrackingExec
        {
            public int Count;
            public string? LastCommand;
            public Func<string, CancellationToken, Task<ShellResult>> Run =>
                (cmd, ct) => { Count++; LastCommand = cmd; return Task.FromResult(new ShellResult(true, 0, "ergebnis")); };
        }

        [Theory]
        [InlineData("Start-Process notepad", "Start-Process notepad")]
        [InlineData("```powershell\nGet-Date\n```", "Get-Date")]
        [InlineData("`Get-Date`", "Get-Date")]
        public void ExtractCommand_StripsFences(string reply, string expected)
            => Assert.Equal(expected, ComputerUseSubAgent.ExtractCommand(reply));

        [Fact]
        public async Task Off_DoesNothing()
        {
            var exec = new TrackingExec();
            var a = new ComputerUseSubAgent(new StubLlm("Get-Date"), () => ComputerUseMode.Off, exec.Run);
            var r = await a.HandleAsync("zeig die Uhrzeit");
            Assert.Contains("ausgeschaltet", r);
            Assert.Equal(0, exec.Count);
        }

        [Fact]
        public async Task Full_Harmless_Executes()
        {
            var exec = new TrackingExec();
            var a = new ComputerUseSubAgent(new StubLlm("Get-Date"), () => ComputerUseMode.Full, exec.Run);
            var r = await a.HandleAsync("zeig die Uhrzeit");
            Assert.Equal(1, exec.Count);
            Assert.Equal("Get-Date", exec.LastCommand);
            Assert.False(a.AwaitingFollowup);
            Assert.Contains("ergebnis", r);
        }

        [Fact]
        public async Task Safe_Dangerous_AsksFirst_ThenRunsOnYes()
        {
            var exec = new TrackingExec();
            var a = new ComputerUseSubAgent(new StubLlm("Remove-Item -Recurse build"), () => ComputerUseMode.Safe, exec.Run);

            var ask = await a.HandleAsync("loesche den build ordner");
            Assert.Equal(0, exec.Count);                  // NICHT sofort ausgefuehrt
            Assert.True(a.AwaitingFollowup);
            Assert.Contains("Remove-Item -Recurse build", ask);
            Assert.Contains("wirklich", ask);

            var done = await a.HandleAsync("ja");
            Assert.Equal(1, exec.Count);                  // erst nach "ja"
            Assert.False(a.AwaitingFollowup);
            Assert.Contains("ergebnis", done);
        }

        [Fact]
        public async Task Safe_Dangerous_No_Cancels()
        {
            var exec = new TrackingExec();
            var a = new ComputerUseSubAgent(new StubLlm("Remove-Item -Recurse build"), () => ComputerUseMode.Safe, exec.Run);
            await a.HandleAsync("loesche den build ordner");
            var r = await a.HandleAsync("nein");
            Assert.Equal(0, exec.Count);
            Assert.False(a.AwaitingFollowup);
            Assert.Contains("nicht aus", r);
        }

        [Fact]
        public async Task Hardline_RefusedEvenInFull()
        {
            var exec = new TrackingExec();
            var a = new ComputerUseSubAgent(new StubLlm("shutdown /s /t 0"), () => ComputerUseMode.Full, exec.Run);
            var r = await a.HandleAsync("fahr den rechner runter");
            Assert.Equal(0, exec.Count);
            Assert.Contains("gefaehrlich", r);
        }

        // ---- Integration mit dem BossAgent: die Folge-Antwort "ja" wird trotz Chat-Intent geroutet ----

        [Fact]
        public async Task Boss_RoutesFollowupToComputerUse_DespiteChatIntent()
        {
            var exec = new TrackingExec();
            var cu = new ComputerUseSubAgent(new StubLlm("Remove-Item -Recurse build"), () => ComputerUseMode.Safe, exec.Run);
            var reg = new SubAgentRegistry();
            reg.Register(cu);
            var boss = new BossAgent(new StubLlm("egal"), systemPrompt: null, memory: null, subAgents: reg);

            // Aufgabe -> Computer Use macht eigene Bestaetigung (kein generisches Gate), fragt nach dem Befehl.
            var ask = await boss.HandleAsync("loesche bitte den build ordner auf dem rechner", IntentKind.Task);
            Assert.Equal("Computer", ask.DelegatedTo);
            Assert.Equal(0, exec.Count);
            Assert.Contains("wirklich", ask.Text);

            // "ja" wird als Chat klassifiziert — muss trotzdem an Computer Use gehen (AwaitingFollowup).
            var done = await boss.HandleAsync("ja", IntentKind.Chat);
            Assert.Equal(1, exec.Count);
            Assert.Equal("Computer", done.DelegatedTo);
        }
    }
}
