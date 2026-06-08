using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class ConfirmationDetectorTests
    {
        [Theory]
        [InlineData("ja")]
        [InlineData("Ja bitte")]
        [InlineData("Ja, mach das")]
        [InlineData("klar")]
        [InlineData("Okay")]
        [InlineData("Mach das")]
        [InlineData("Gerne")]
        [InlineData("Ja genau")]
        [InlineData("jap")]
        [InlineData("na klar")]
        [InlineData("genau, mach es")]
        public void Yes(string s)
            => Assert.Equal(ConfirmationDetector.Decision.Yes, ConfirmationDetector.Interpret(s));

        [Theory]
        [InlineData("nein")]
        [InlineData("Nein danke")]
        [InlineData("Ne")]
        [InlineData("Nee")]
        [InlineData("Lass mal")]
        [InlineData("lieber nicht")]
        [InlineData("Doch nicht")]
        [InlineData("vergiss es")]
        [InlineData("abbrechen")]
        [InlineData("sicher nicht")]
        [InlineData("nein lass")]
        public void No(string s)
            => Assert.Equal(ConfirmationDetector.Decision.No, ConfirmationDetector.Interpret(s));

        [Theory]
        [InlineData("")]
        [InlineData("   ")]
        [InlineData("Wie spaet ist es")]
        [InlineData("erzaehl mir einen Witz")]
        [InlineData("vielleicht spaeter mal")]
        public void Unclear(string s)
            => Assert.Equal(ConfirmationDetector.Decision.Unclear, ConfirmationDetector.Interpret(s));

        [Fact]
        public void Null_IsUnclear()
            => Assert.Equal(ConfirmationDetector.Decision.Unclear, ConfirmationDetector.Interpret(null));
    }
}
