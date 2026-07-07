using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class GeminiProviderTests
    {
        [Fact]
        public void ExtractText_ReturnsText()
        {
            var json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hallo Frank\"}]}}]}";
            Assert.Equal("Hallo Frank", GeminiProvider.ExtractText(json));
        }

        [Fact]
        public void ExtractText_SkipsThoughtParts()
        {
            var json = "{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"denke\"},{\"text\":\"echte Antwort\"}]}}]}";
            Assert.Equal("echte Antwort", GeminiProvider.ExtractText(json));
        }

        [Fact]
        public void OpenAi_ExtractText_ReturnsContent()
        {
            var json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Servus\"}}]}";
            Assert.Equal("Servus", OpenAiProvider.ExtractText(json));
        }

        [Fact]
        public void Claude_ExtractText_ReturnsText()
        {
            var json = "{\"content\":[{\"type\":\"text\",\"text\":\"Moin\"}]}";
            Assert.Equal("Moin", ClaudeProvider.ExtractText(json));
        }
    }
}
