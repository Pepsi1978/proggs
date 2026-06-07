using VoiceAgent.Core;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Tests
{
    public class IntentDetectorTests
    {
        [Fact]
        public void ParseIntent_Aufgabe_Task()
            => Assert.Equal(IntentKind.Task, IntentDetector.ParseIntent("AUFGABE"));

        [Fact]
        public void ParseIntent_Frage_Question()
            => Assert.Equal(IntentKind.Question, IntentDetector.ParseIntent("FRAGE"));

        [Fact]
        public void ParseIntent_Plauderei_Chat()
            => Assert.Equal(IntentKind.Chat, IntentDetector.ParseIntent("PLAUDEREI"));

        [Fact]
        public void ParseIntent_WordInSentence_Extracted()
            => Assert.Equal(IntentKind.Task, IntentDetector.ParseIntent("Das ist eine AUFGABE."));

        [Fact]
        public void ParseIntent_Unclear_DefaultsToChat()
            => Assert.Equal(IntentKind.Chat, IntentDetector.ParseIntent("hmm weiss nicht"));
    }
}
