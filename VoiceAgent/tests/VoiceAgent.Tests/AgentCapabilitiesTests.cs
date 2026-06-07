using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class AgentCapabilitiesTests
    {
        // Im Test-Kontext liegt keine capabilities.md neben der Test-DLL -> der eingebaute
        // Fallback greift. Beide Faehigkeiten-Abschnitte muessen trotzdem vorhanden sein.
        [Fact]
        public void BuildBlock_HasHeaderAndBothSections()
        {
            var block = AgentCapabilities.BuildBlock();
            Assert.Contains("FAEHIGKEITEN", block);
            Assert.Contains("Das kann ich JETZT", block);
            Assert.Contains("Das kann ich NOCH NICHT", block);
        }

        [Fact]
        public void BuildBlock_MentionsReminders()
            => Assert.Contains("Erinnerung", AgentCapabilities.BuildBlock());
    }
}
