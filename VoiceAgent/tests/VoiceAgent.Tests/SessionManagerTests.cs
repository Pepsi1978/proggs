using System;
using System.IO;
using VoiceAgent.Core;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class SessionManagerTests : IDisposable
    {
        private readonly string _dir;
        private readonly SessionStore _store;
        public SessionManagerTests()
        {
            _dir = Path.Combine(Path.GetTempPath(), "va-mgr-" + Guid.NewGuid().ToString("N"));
            _store = new SessionStore(_dir);
        }
        public void Dispose() { try { if (Directory.Exists(_dir)) Directory.Delete(_dir, true); } catch { } }

        [Fact]
        public void New_StartsEmptyActiveSession()
        {
            var mgr = new SessionManager(_store);
            mgr.NewSession();
            Assert.Empty(mgr.Active.History);
            Assert.Equal("", mgr.Active.Summary);
        }

        [Fact]
        public void SaveActive_ThenSwitch_LoadsHistory()
        {
            var mgr = new SessionManager(_store);
            mgr.NewSession();
            var firstId = mgr.Active.Id;
            mgr.Active.History.Add(new LlmMessage(LlmRole.User, "Hallo"));
            mgr.SaveActive();

            mgr.NewSession();
            Assert.NotEqual(firstId, mgr.Active.Id);
            Assert.Empty(mgr.Active.History);

            mgr.Switch(firstId);
            Assert.Equal(firstId, mgr.Active.Id);
            Assert.Single(mgr.Active.History);
        }

        [Fact]
        public void EnsureTitleFromFirstMessage_SetsTitleOnce()
        {
            var mgr = new SessionManager(_store);
            mgr.NewSession();
            mgr.Active.History.Add(new LlmMessage(LlmRole.User, "Erinnere mich ans Meeting morgen frueh um neun"));
            mgr.EnsureTitleFromFirstMessage();
            Assert.NotEqual(ChatSession.DefaultTitle, mgr.Active.Title);
            Assert.StartsWith("Erinnere mich", mgr.Active.Title);
        }

        [Fact]
        public void ActiveChanged_FiresOnNewAndSwitch()
        {
            var mgr = new SessionManager(_store);
            int fired = 0;
            mgr.ActiveChanged += () => fired++;
            mgr.NewSession();
            var id = mgr.Active.Id;
            mgr.SaveActive();
            mgr.NewSession();
            mgr.Switch(id);
            Assert.True(fired >= 3);
        }
    }
}
