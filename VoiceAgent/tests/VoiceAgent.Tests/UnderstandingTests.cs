using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    /// <summary>
    /// Verifiziert den strukturierten Verstehens-Schritt (Phase 1 der Boss-Agent-Ueberarbeitung):
    /// tolerantes JSON-Parsen, Kind-Zuordnung und vor allem die Negation-mit-Korrektur im
    /// BossAgent ("nein, mach es um 10 Uhr" ist KEIN Abbruch — Almanach orchestrator-agent 5.5).
    /// </summary>
    public class UnderstandingTests
    {
        // ---------- Parse (UnderstandingDetector) ----------

        [Fact]
        public void Parse_ValidJson_ReturnsAllFields()
        {
            var u = UnderstandingDetector.Parse(
                "{\"kind\":\"AUFGABE\",\"zielAgent\":\"Erinnerung\",\"auftrag\":\"erinnere mich morgen um 09:00 an den Zahnarzt\",\"zeit\":\"morgen um 09:00\",\"rueckfrage\":\"Soll ich dich morgen um neun an den Zahnarzt erinnern?\",\"confidence\":0.9}");
            Assert.NotNull(u);
            Assert.Equal(IntentKind.Task, u!.Kind);
            Assert.Equal("Erinnerung", u.ZielAgent);
            Assert.Contains("Zahnarzt", u.Auftrag);
            Assert.Equal(0.9, u.Confidence, 3);
        }

        [Fact]
        public void Parse_JsonInCodeFence_IsTolerated()
        {
            var u = UnderstandingDetector.Parse(
                "```json\n{\"kind\":\"FRAGE\",\"zielAgent\":null,\"auftrag\":null,\"zeit\":null,\"rueckfrage\":null,\"confidence\":0.7}\n```");
            Assert.NotNull(u);
            Assert.Equal(IntentKind.Question, u!.Kind);
            Assert.Null(u.ZielAgent);
        }

        [Fact]
        public void Parse_NullStringValues_BecomeNull()
        {
            var u = UnderstandingDetector.Parse(
                "{\"kind\":\"PLAUDEREI\",\"zielAgent\":\"null\",\"auftrag\":\"null\",\"zeit\":null,\"rueckfrage\":null,\"confidence\":1.4}");
            Assert.NotNull(u);
            Assert.Null(u!.ZielAgent);
            Assert.Null(u.Auftrag);
            Assert.Equal(1.0, u.Confidence, 3);   // Confidence wird auf [0,1] geklemmt
        }

        [Theory]
        [InlineData(null)]
        [InlineData("")]
        [InlineData("kein json hier")]
        [InlineData("{\"kind\":\"QUATSCH\"}")]   // unbekannte Kategorie -> Deutung wertlos
        [InlineData("{kaputt")]
        public void Parse_Unusable_ReturnsNull(string? reply)
            => Assert.Null(UnderstandingDetector.Parse(reply));

        // ---------- IsPureDecline (ConfirmationDetector) ----------

        [Theory]
        [InlineData("nein")]
        [InlineData("nein danke")]
        [InlineData("ne lass mal")]
        [InlineData("nein, lieber nicht")]
        [InlineData("vergiss es")]
        public void IsPureDecline_PureNo_IsTrue(string text)
            => Assert.True(ConfirmationDetector.IsPureDecline(text));

        [Theory]
        [InlineData("nein, mach es um 10 Uhr")]
        [InlineData("nein, erinnere mich lieber morgen frueh")]
        [InlineData("nein so nicht, schreib stattdessen eine Notiz")]
        public void IsPureDecline_NoWithInstruction_IsFalse(string text)
            => Assert.False(ConfirmationDetector.IsPureDecline(text));

        // ---------- Negation-mit-Korrektur im BossAgent ----------

        private sealed class FakeLlm : ILlmProvider
        {
            public string Name => "Fake";
            public Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
                => Task.FromResult("Hauptagent-Antwort");
        }

        private sealed class FakeSub : ISubAgent
        {
            public string? ExecutedTask;
            public string? LastConfirmationTask;
            public string Name => "Erinnerung";
            public string Description => "Merkt sich Erinnerungen.";
            public string HowTo => "Sag 'erinnere mich an X'.";
            public bool CanHandle(string task) => task.ToLowerInvariant().Contains("erinner");
            public string ConfirmationQuestion(string task)
            {
                LastConfirmationTask = task;
                return $"Soll ich: {task}?";
            }
            public Task<string> HandleAsync(string task, CancellationToken ct = default)
            {
                ExecutedTask = task;
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
        public async Task NoWithCorrection_ReroutesInsteadOfCancel()
        {
            var (boss, sub) = Build();

            await boss.HandleAsync("erinnere mich um 9 Uhr an den Test", IntentKind.Task);
            Assert.True(boss.HasPendingConfirmation);

            // "nein, …" MIT neuer Anweisung (Intent=Task) -> Korrektur: neue Rueckfrage, KEIN Abbruch.
            var r = await boss.HandleAsync("nein erinnere mich lieber um 10 Uhr an den Test", IntentKind.Task);
            Assert.True(boss.HasPendingConfirmation);            // neue Rueckfrage steht
            Assert.Null(sub.ExecutedTask);                       // nichts ausgefuehrt
            Assert.DoesNotContain("lasse ich", r.Text);          // kein Abbruch-Text
            Assert.Contains("10 Uhr", sub.LastConfirmationTask); // Rueckfrage basiert auf der Korrektur

            // Bestaetigung fuehrt die KORRIGIERTE Aufgabe aus.
            var done = await boss.HandleAsync("ja", IntentKind.Chat);
            Assert.Contains("10 Uhr", sub.ExecutedTask);
            Assert.Contains("Erledigt", done.Text);
        }

        [Fact]
        public async Task PureNo_StillCancels()
        {
            var (boss, sub) = Build();

            await boss.HandleAsync("erinnere mich um 9 Uhr an den Test", IntentKind.Task);
            var r = await boss.HandleAsync("nein danke", IntentKind.Task);   // selbst mit Task-Intent: reines Nein bricht ab

            Assert.Null(sub.ExecutedTask);
            Assert.False(boss.HasPendingConfirmation);
            Assert.Contains("lasse ich", r.Text);
        }

        [Fact]
        public async Task Understanding_NormalizedAuftrag_IsRoutedAndConfirmed()
        {
            var (boss, sub) = Build();

            // Das Brain hat den Rohtext ("erinnere mich rogen um neun...") normalisiert.
            var u = new Understanding(IntentKind.Task, "Erinnerung",
                "erinnere mich morgen um 09:00 an den Zahnarzt", "morgen um 09:00", "Soll ich dich morgen um neun an den Zahnarzt erinnern?", 0.9);

            var r = await boss.HandleAsync("erinnere mich rogen um neun an den zahnarzt", IntentKind.Task, u);

            Assert.True(boss.HasPendingConfirmation);
            // Die Rueckfrage entsteht aus dem NORMALISIERTEN Auftrag (kein Rohtext-Echo).
            Assert.Contains("morgen um 09:00", sub.LastConfirmationTask);

            var done = await boss.HandleAsync("ja", IntentKind.Chat);
            Assert.Contains("morgen um 09:00", sub.ExecutedTask);   // ausgefuehrt wird der verstandene Auftrag
            Assert.Contains("Erledigt", done.Text);
        }

        [Fact]
        public async Task Understanding_GenericQuestion_IsReplacedByUnderstoodReadback()
        {
            var sub = new GenericSub();
            var reg = new SubAgentRegistry();
            reg.Register(sub);
            var boss = new BossAgent(new FakeLlm(), systemPrompt: null, memory: null, subAgents: reg);

            var u = new Understanding(IntentKind.Task, "Notiz", "notiere: Milch kaufen", null,
                "Soll ich mir Milch kaufen notieren?", 0.8);
            var r = await boss.HandleAsync("schreib mal auf milch kaufen", IntentKind.Task, u);

            // Der Helfer liefert nur die generische Frage -> der Boss nimmt die VERSTANDENE Rueckfrage.
            Assert.Equal("Soll ich mir Milch kaufen notieren?", r.Text);
        }

        private sealed class GenericSub : ISubAgent
        {
            public string Name => "Notiz";
            public string Description => "Notizen.";
            public string HowTo => "Sag 'notiere X'.";
            public bool CanHandle(string task) => task.Contains("notiere");
            public Task<string> HandleAsync(string task, CancellationToken ct = default)
                => Task.FromResult("Notiert.");
        }
    }
}
