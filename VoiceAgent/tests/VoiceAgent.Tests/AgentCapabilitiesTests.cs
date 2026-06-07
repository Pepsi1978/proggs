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
        public void IsCapabilityQuestion_True_ForCapabilityAsks()
        {
            Assert.True(AgentCapabilities.IsCapabilityQuestion("Was kannst du eigentlich?"));
            Assert.True(AgentCapabilities.IsCapabilityQuestion("Kannst du mich erinnern?"));
        }

        [Fact]
        public void IsCapabilityQuestion_False_ForNormalTalk()
        {
            Assert.False(AgentCapabilities.IsCapabilityQuestion("Wie spaet ist es?"));
            Assert.False(AgentCapabilities.IsCapabilityQuestion(""));
        }
    }
}
