using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class CustomAgentTests
    {
        private static string TempPath() => Path.Combine(Path.GetTempPath(), $"va-custom-{Guid.NewGuid():N}.json");

        private static SubAgentDefinition Def(string name) => new()
        {
            Name = name,
            Description = "Test-Helfer",
            Triggers = new List<string> { "test" },
            SystemPrompt = "Sei ein Test."
        };

        private sealed class StubLlm : ILlmProvider
        {
            private readonly string _answer;
            public StubLlm(string answer) => _answer = answer;
            public string Name => "Stub";
            public Task<string> ChatAsync(IReadOnlyList<LlmMessage> messages, CancellationToken ct = default)
                => Task.FromResult(_answer);
        }

        // ---- CustomAgentStore ----

        [Fact]
        public void Store_Add_PersistsAcrossInstances()
        {
            var path = TempPath();
            try
            {
                var s1 = new CustomAgentStore(path);
                Assert.True(s1.Add(Def("Koch")));

                var s2 = new CustomAgentStore(path);   // frische Instanz liest dieselbe Datei
                Assert.Contains(s2.All, d => d.Name == "Koch");
            }
            finally { File.Delete(path); }
        }

        [Fact]
        public void Store_Add_SameName_Replaces()
        {
            var path = TempPath();
            try
            {
                var s = new CustomAgentStore(path);
                s.Add(Def("Koch"));
                var updated = Def("Koch");
                updated.Description = "Neu";
                s.Add(updated);
                Assert.Single(s.All);
                Assert.Equal("Neu", s.All[0].Description);
            }
            finally { File.Delete(path); }
        }

        [Fact]
        public void Store_NoName_Rejected()
        {
            var path = TempPath();
            try
            {
                var s = new CustomAgentStore(path);
                Assert.False(s.Add(new SubAgentDefinition { Name = "" }));
                Assert.Empty(s.All);
            }
            finally { if (File.Exists(path)) File.Delete(path); }
        }

        [Fact]
        public void Store_Remove()
        {
            var path = TempPath();
            try
            {
                var s = new CustomAgentStore(path);
                s.Add(Def("Koch"));
                Assert.True(s.Remove("koch"));   // case-insensitiv
                Assert.Empty(s.All);
                Assert.False(s.Remove("gibtsnicht"));
            }
            finally { if (File.Exists(path)) File.Delete(path); }
        }

        // ---- AgentBuilderSubAgent.ParseDefinition ----

        [Fact]
        public void Parse_ValidJson()
        {
            var json = """{"name":"Koch-Berater","description":"schlaegt Rezepte vor","triggers":["rezept","kochen"],"systemPrompt":"Sei ein Koch."}""";
            var def = AgentBuilderSubAgent.ParseDefinition(json);
            Assert.NotNull(def);
            Assert.Equal("Koch-Berater", def!.Name);
            Assert.Equal(2, def.Triggers.Count);
            Assert.Contains("rezept", def.Triggers);
        }

        [Fact]
        public void Parse_WithSurroundingText()
        {
            var reply = "Klar! Hier ist der Helfer:\n```json\n{\"name\":\"Uebersetzer\",\"description\":\"uebersetzt\",\"triggers\":[\"uebersetze\"],\"systemPrompt\":\"x\"}\n```\nViel Spass!";
            var def = AgentBuilderSubAgent.ParseDefinition(reply);
            Assert.NotNull(def);
            Assert.Equal("Uebersetzer", def!.Name);
        }

        [Fact]
        public void Parse_TriggersNormalized()
        {
            var json = """{"name":"X","description":"d","triggers":["  REZEPT  ","Kochen","rezept"],"systemPrompt":"p"}""";
            var def = AgentBuilderSubAgent.ParseDefinition(json);
            Assert.NotNull(def);
            Assert.Contains("rezept", def!.Triggers);
            Assert.Contains("kochen", def.Triggers);
            Assert.Equal(2, def.Triggers.Count);   // dedupliziert + lowercased
        }

        [Fact]
        public void Parse_NoName_Null()
            => Assert.Null(AgentBuilderSubAgent.ParseDefinition("""{"description":"d"}"""));

        [Fact]
        public void Parse_Garbage_Null()
        {
            Assert.Null(AgentBuilderSubAgent.ParseDefinition("kein json hier"));
            Assert.Null(AgentBuilderSubAgent.ParseDefinition(""));
            Assert.Null(AgentBuilderSubAgent.ParseDefinition(null));
        }

        // ---- PromptSubAgent ----

        [Fact]
        public void PromptSubAgent_CanHandle_Trigger()
        {
            var a = new PromptSubAgent(Def("Koch"), new StubLlm("x"));
            Assert.True(a.CanHandle("ich brauche einen test"));
            Assert.False(a.CanHandle("etwas ganz anderes"));
        }

        [Fact]
        public async Task PromptSubAgent_HandleAsync_UsesLlm()
        {
            var a = new PromptSubAgent(Def("Koch"), new StubLlm("Nimm Tomaten."));
            var r = await a.HandleAsync("was koche ich?");
            Assert.Equal("Nimm Tomaten.", r);
        }

        [Fact]
        public async Task AgentBuilder_Builds_And_Registers()
        {
            var path = TempPath();
            try
            {
                var store = new CustomAgentStore(path);
                var reg = new SubAgentRegistry();
                var defJson = """{"name":"Uebersetzer","description":"uebersetzt Texte","triggers":["uebersetze"],"systemPrompt":"Du uebersetzt."}""";
                var builder = new AgentBuilderSubAgent(new StubLlm(defJson), () => new StubLlm("ok"), reg, store);

                var msg = await builder.HandleAsync("bau mir einen Helfer der Texte uebersetzt");

                Assert.Contains("Uebersetzer", msg);
                Assert.NotNull(reg.FindByName("Uebersetzer"));        // sofort registriert
                Assert.Contains(store.All, d => d.Name == "Uebersetzer"); // und persistiert
            }
            finally { if (File.Exists(path)) File.Delete(path); }
        }
    }
}
