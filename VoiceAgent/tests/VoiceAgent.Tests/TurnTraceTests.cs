using VoiceAgent.Diagnostics;

namespace VoiceAgent.Tests
{
    public class TurnTraceTests
    {
        [Fact]
        public void Classify_TaskWithConfirmation_TaskAndAskedBack()
        {
            var (kind, askedBack) = TurnTrace.Classify("Soll ich das Licht fuer dich ausschalten?");
            Assert.Equal(IntentKind.Task, kind);
            Assert.True(askedBack);
        }

        [Fact]
        public void Classify_QuestionWithoutConfirmation_Question()
        {
            var (kind, askedBack) = TurnTrace.Classify("Wie meinst du das genau?");
            Assert.Equal(IntentKind.Question, kind);
            Assert.False(askedBack);
        }

        [Fact]
        public void Classify_StatementNoQuestion_Chat()
        {
            var (kind, askedBack) = TurnTrace.Classify("Paris ist die Hauptstadt von Frankreich.");
            Assert.Equal(IntentKind.Chat, kind);
            Assert.False(askedBack);
        }

        [Fact]
        public void Classify_Empty_Unknown()
        {
            Assert.Equal(IntentKind.Unknown, TurnTrace.Classify("").kind);
            Assert.Equal(IntentKind.Unknown, TurnTrace.Classify(null).kind);
        }
    }
}
