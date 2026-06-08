using System;
using VoiceAgent.Core;
using VoiceAgent.Services.Audio;

namespace VoiceAgent.Tests
{
    public class WakeWordControllerTests
    {
        /// <summary>Steuerbares VAD-Doppel: gibt einen festen Wert zurueck (Sprache ja/nein).</summary>
        private sealed class FakeVad : IVadGate
        {
            public bool Result = true;
            public bool IsSpeech(float[] samples) => Result;
        }

        private static float[] Frame() => new[] { 0.5f, -0.5f, 0.5f };

        [Fact]
        public void StartsSleeping()
        {
            var c = new WakeWordController(new FakeWakeWordEngine(), new FakeVad(), 60000);
            Assert.Equal(WakeState.Sleeping, c.State);
        }

        [Fact]
        public void ProcessFrame_NoKeyword_StaysSleeping()
        {
            var c = new WakeWordController(new FakeWakeWordEngine(), new FakeVad(), 60000);
            Assert.False(c.ProcessFrame(Frame()));
            Assert.Equal(WakeState.Sleeping, c.State);
        }

        [Fact]
        public void ProcessFrame_Keyword_GoesAwake_AndFiresWoke()
        {
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad(), 60000);
            string? woke = null;
            c.Woke += k => woke = k;

            engine.TriggerWake("OKAY COMPUTER");
            Assert.True(c.ProcessFrame(Frame()));
            Assert.Equal(WakeState.Awake, c.State);
            Assert.Equal("OKAY COMPUTER", woke);
        }

        [Fact]
        public void AllFrames_ReachEngine_NoVadFiltering()
        {
            // Bug #33: Streaming-KWS braucht KONTINUIERLICHES Audio — KEIN VAD-Filtering vor der
            // Engine. Selbst wenn das VAD "keine Sprache" meldet, MUSS der Frame die Engine erreichen
            // (sonst zerstueckelt der Stream und das Weckwort wird nie erkannt).
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad { Result = false }, 60000);
            engine.TriggerWake();

            Assert.True(c.ProcessFrame(Frame()));    // Frame erreicht die Engine trotz VAD=false
            Assert.Equal(1, engine.AcceptCount);
            Assert.Equal(WakeState.Awake, c.State);
        }

        [Fact]
        public void ProcessFrame_WhenAwake_DoesNotReTrigger()
        {
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad(), 60000);
            engine.TriggerWake();
            c.ProcessFrame(Frame());                 // -> Awake

            engine.TriggerWake();
            Assert.False(c.ProcessFrame(Frame()));   // im Awake kein erneutes Wecken
            Assert.Equal(WakeState.Awake, c.State);
        }

        [Fact]
        public void Tick_BeforeTimeout_StaysAwake()
        {
            var now = DateTimeOffset.UtcNow;
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad(), 60000, () => now);
            engine.TriggerWake();
            c.ProcessFrame(Frame());

            now = now.AddMilliseconds(59000);
            c.Tick();
            Assert.Equal(WakeState.Awake, c.State);
        }

        [Fact]
        public void Tick_AfterTimeout_Sleeps_AndFiresSlept()
        {
            var now = DateTimeOffset.UtcNow;
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad(), 60000, () => now);
            bool slept = false;
            c.Slept += _ => slept = true;   // Slept ist Action<string> (reason) — Argument hier ignoriert
            engine.TriggerWake();
            c.ProcessFrame(Frame());

            now = now.AddMilliseconds(61000);
            c.Tick();
            Assert.Equal(WakeState.Sleeping, c.State);
            Assert.True(slept);
        }

        [Fact]
        public void NotifyActivity_ExtendsWindow()
        {
            var now = DateTimeOffset.UtcNow;
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad(), 60000, () => now);
            engine.TriggerWake();
            c.ProcessFrame(Frame());

            now = now.AddMilliseconds(50000);
            c.NotifyActivity();                      // Fenster zuruecksetzen
            now = now.AddMilliseconds(50000);
            c.Tick();                                // 50 s seit Aktivitaet < 60 s
            Assert.Equal(WakeState.Awake, c.State);
        }

        [Fact]
        public void Sleep_ResetsEngine()
        {
            var now = DateTimeOffset.UtcNow;
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad(), 1000, () => now);
            engine.TriggerWake();
            c.ProcessFrame(Frame());                 // AcceptCount = 1, Awake

            now = now.AddMilliseconds(2000);
            c.Tick();                                // Timeout -> Sleep -> engine.Reset()
            Assert.Equal(0, engine.AcceptCount);
        }

        [Fact]
        public void Dispose_DisposesEngine()
        {
            var engine = new FakeWakeWordEngine();
            var c = new WakeWordController(engine, new FakeVad(), 60000);
            c.Dispose();
            Assert.True(engine.Disposed);
        }
    }
}
