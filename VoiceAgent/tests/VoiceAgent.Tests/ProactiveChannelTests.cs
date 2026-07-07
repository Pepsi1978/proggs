using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class ProactiveChannelTests
    {
        [Fact]
        public void Post_Then_TryTake_IsFifo()
        {
            var c = new ProactiveChannel();
            c.Post("erste", "r1");
            c.Post("zweite", "r2");

            Assert.True(c.TryTake(out var m1));
            Assert.Equal("erste", m1.Text);
            Assert.Equal("r1", m1.Reason);

            Assert.True(c.TryTake(out var m2));
            Assert.Equal("zweite", m2.Text);

            Assert.False(c.TryTake(out _));
        }

        [Fact]
        public void Post_EmptyOrWhitespace_Ignored()
        {
            var c = new ProactiveChannel();
            c.Post("", "x");
            c.Post("   ", "x");
            c.Post(null!, "x");
            Assert.False(c.HasPending);
        }

        [Fact]
        public void Post_FiresPostedEvent()
        {
            var c = new ProactiveChannel();
            int count = 0;
            c.Posted += () => count++;
            c.Post("hallo");
            Assert.Equal(1, count);
        }

        [Fact]
        public void Post_TrimsText()
        {
            var c = new ProactiveChannel();
            c.Post("  hallo  ");
            Assert.True(c.TryTake(out var m));
            Assert.Equal("hallo", m.Text);
        }

        [Fact]
        public void HasPending_ReflectsQueue()
        {
            var c = new ProactiveChannel();
            Assert.False(c.HasPending);
            c.Post("x");
            Assert.True(c.HasPending);
            c.TryTake(out _);
            Assert.False(c.HasPending);
        }
    }
}
