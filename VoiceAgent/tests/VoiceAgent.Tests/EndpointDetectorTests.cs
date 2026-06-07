using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class EndpointDetectorTests
    {
        [Fact]
        public void Fertig_IsComplete() => Assert.True(EndpointDetector.ParseIsComplete("FERTIG"));

        [Fact]
        public void Weiter_IsNotComplete() => Assert.False(EndpointDetector.ParseIsComplete("WEITER"));

        [Fact]
        public void LowercaseFertig_IsComplete() => Assert.True(EndpointDetector.ParseIsComplete("fertig."));

        [Fact]
        public void WeiterWins_WhenAmbiguous() => Assert.False(EndpointDetector.ParseIsComplete("WEITER, noch nicht fertig"));

        [Fact]
        public void Unclear_DefaultsToComplete() => Assert.True(EndpointDetector.ParseIsComplete("hmm"));

        [Fact]
        public void Empty_DefaultsToComplete() => Assert.True(EndpointDetector.ParseIsComplete(""));
    }
}
