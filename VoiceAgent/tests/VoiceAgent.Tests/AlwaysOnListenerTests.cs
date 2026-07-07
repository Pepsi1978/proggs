using VoiceAgent.Services.Audio;

namespace VoiceAgent.Tests
{
    public class AlwaysOnListenerTests
    {
        [Fact]
        public void IsSilence_BelowThreshold_True()
        {
            Assert.True(AlwaysOnListener.IsSilence(new[] { 0.001f, 0.0f, -0.002f }, 0.01f));
        }

        [Fact]
        public void IsSilence_AboveThreshold_False()
        {
            Assert.False(AlwaysOnListener.IsSilence(new[] { 0.5f, -0.4f }, 0.01f));
        }

        [Fact]
        public void IsSilence_Empty_True()
        {
            Assert.True(AlwaysOnListener.IsSilence(System.Array.Empty<float>(), 0.01f));
        }
    }
}
