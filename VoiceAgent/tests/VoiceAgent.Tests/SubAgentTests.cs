using System.IO;
using System;
using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class SubAgentTests
    {
        [Fact]
        public void NoteSubAgent_CanHandle_TriggerWord()
        {
            var agent = new NoteSubAgent(new AgentMemory(TempPath()));
            Assert.True(agent.CanHandle("Merk dir bitte den Termin"));
            Assert.False(agent.CanHandle("Wie spaet ist es?"));
        }

        [Fact]
        public void ExtractNote_StripsTrigger()
        {
            Assert.Equal("ich trinke morgens Tee", NoteSubAgent.ExtractNote("Merk dir: ich trinke morgens Tee"));
        }

        [Fact]
        public void ExtractNote_NoTrigger_ReturnsWhole()
        {
            Assert.Equal("nur ein Satz", NoteSubAgent.ExtractNote("nur ein Satz"));
        }

        [Fact]
        public void Registry_Find_ReturnsHandlingAgent()
        {
            var reg = new SubAgentRegistry();
            var note = new NoteSubAgent(new AgentMemory(TempPath()));
            reg.Register(note);
            Assert.Same(note, reg.Find("notier dir das"));
            Assert.Null(reg.Find("erzaehl mir einen Witz"));
        }

        private static string TempPath() => Path.Combine(Path.GetTempPath(), $"vamem-{Guid.NewGuid():N}.json");
    }
}
