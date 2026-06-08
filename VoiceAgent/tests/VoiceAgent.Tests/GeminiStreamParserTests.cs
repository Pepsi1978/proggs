using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    /// <summary>Testet den SSE-Delta-Parser des GeminiProviders isoliert (ohne Netzwerk).</summary>
    public class GeminiStreamParserTests
    {
        [Fact]
        public void Delta_SimpleChunk()
        {
            var json = """{"candidates":[{"content":{"parts":[{"text":"Hallo"}]}}]}""";
            Assert.Equal("Hallo", GeminiProvider.TryExtractDelta(json));
        }

        [Fact]
        public void Delta_SkipsThinkingParts()
        {
            var json = """{"candidates":[{"content":{"parts":[{"text":"denk...","thought":true},{"text":"Antwort"}]}}]}""";
            Assert.Equal("Antwort", GeminiProvider.TryExtractDelta(json));
        }

        [Fact]
        public void Delta_ConcatenatesMultipleTextParts()
        {
            var json = """{"candidates":[{"content":{"parts":[{"text":"A"},{"text":"B"}]}}]}""";
            Assert.Equal("AB", GeminiProvider.TryExtractDelta(json));
        }

        [Fact]
        public void Delta_NoText_ReturnsNull()
        {
            Assert.Null(GeminiProvider.TryExtractDelta("""{"usageMetadata":{"totalTokenCount":5}}"""));
        }

        [Fact]
        public void Delta_InvalidJson_ReturnsNull()
        {
            Assert.Null(GeminiProvider.TryExtractDelta("kein json"));
            Assert.Null(GeminiProvider.TryExtractDelta(""));
        }
    }
}
