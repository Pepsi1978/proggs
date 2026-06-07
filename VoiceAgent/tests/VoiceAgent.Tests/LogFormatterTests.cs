using VoiceAgent.Diagnostics;

namespace VoiceAgent.Tests
{
    public class LogFormatterTests
    {
        [Fact]
        public void FormatLine_ParsesJson_Readable()
        {
            var json = "{\"ts\":\"2026-06-07T20:57:59.554+02:00\",\"level\":\"INFO\",\"turn\":1," +
                       "\"module\":\"TurnTrace\",\"fn\":\"Complete\",\"msg\":\"MANIFEST ok\",\"ctx\":{\"intent\":\"Task\"}}";
            var s = LogFormatter.FormatLine(json);
            Assert.Contains("20:57:59", s);
            Assert.Contains("turn 1", s);
            Assert.Contains("INFO", s);
            Assert.Contains("MANIFEST ok", s);
        }

        [Fact]
        public void FormatLine_InvalidJson_ReturnsRaw()
            => Assert.Equal("kein json", LogFormatter.FormatLine("kein json"));

        [Fact]
        public void FormatLine_Empty_ReturnsEmpty()
            => Assert.Equal("", LogFormatter.FormatLine(""));
    }
}
