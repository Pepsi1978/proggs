using System;
using System.IO;
using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class AgentMemoryTests
    {
        [Fact]
        public void BuildContextBlock_Empty_ReturnsEmpty()
            => Assert.Equal("", AgentMemory.BuildContextBlock(new MemoryStore()));

        [Fact]
        public void BuildContextBlock_WithFacts_ContainsFact()
        {
            var store = new MemoryStore();
            store.Facts.Add("Frank mag kurze Antworten");
            var block = AgentMemory.BuildContextBlock(store);
            Assert.Contains("Frank mag kurze Antworten", block);
            Assert.Contains("Gedaechtnis", block);
        }

        [Fact]
        public void RecordTurn_CapsToMaxRecent()
        {
            var path = Path.Combine(Path.GetTempPath(), $"vamem-{Guid.NewGuid():N}.json");
            try
            {
                var m = new AgentMemory(path);
                for (int i = 0; i < AgentMemory.MaxRecentTurns + 5; i++)
                    m.RecordTurn($"frage {i}", $"antwort {i}");
                Assert.Equal(AgentMemory.MaxRecentTurns, m.Recent.Count);
            }
            finally
            {
                try { File.Delete(path); } catch { }
            }
        }

        [Fact]
        public void Remember_Persists_AcrossInstances()
        {
            var path = Path.Combine(Path.GetTempPath(), $"vamem-{Guid.NewGuid():N}.json");
            try
            {
                new AgentMemory(path).Remember("Test-Fakt");
                var reloaded = new AgentMemory(path);   // neue Instanz -> aus Datei geladen
                Assert.Contains("Test-Fakt", reloaded.Facts);
            }
            finally
            {
                try { File.Delete(path); } catch { }
            }
        }
    }
}
