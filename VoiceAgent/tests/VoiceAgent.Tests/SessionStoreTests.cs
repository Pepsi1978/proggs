using System;
using System.IO;
using System.Linq;
using VoiceAgent.Core;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class SessionStoreTests : IDisposable
    {
        private readonly string _dir;
        public SessionStoreTests()
        {
            _dir = Path.Combine(Path.GetTempPath(), "va-sessions-" + Guid.NewGuid().ToString("N"));
        }
        public void Dispose()
        {
            try { if (Directory.Exists(_dir)) Directory.Delete(_dir, true); } catch { }
        }

        [Fact]
        public void SaveThenLoad_RoundTripsHistoryAndSummary()
        {
            var store = new SessionStore(_dir);
            var s = new ChatSession { Title = "Test" };
            s.Summary = "Bisher: Hallo gesagt.";
            s.History.Add(new LlmMessage(LlmRole.User, "Hi"));
            s.History.Add(new LlmMessage(LlmRole.Assistant, "Hallo!"));
            store.Save(s);

            var loaded = store.Load(s.Id);
            Assert.Equal("Test", loaded.Title);
            Assert.Equal("Bisher: Hallo gesagt.", loaded.Summary);
            Assert.Equal(2, loaded.History.Count);
            Assert.Equal(LlmRole.Assistant, loaded.History[1].Role);
            Assert.Equal("Hallo!", loaded.History[1].Text);
        }

        [Fact]
        public void List_ReturnsMetadata_PinnedAndByUpdatedDesc()
        {
            var store = new SessionStore(_dir);
            var a = new ChatSession { Title = "A", UpdatedAt = DateTimeOffset.Now.AddMinutes(-10) };
            var b = new ChatSession { Title = "B", UpdatedAt = DateTimeOffset.Now, Pinned = true };
            store.Save(a); store.Save(b);

            var list = store.List();
            Assert.Equal(2, list.Count);
            Assert.Contains(list, i => i.Title == "B" && i.Pinned);
            // Angepinnte/Neueste zuerst
            Assert.Equal("B", list[0].Title);
        }

        [Fact]
        public void RenameDeletePin_Work()
        {
            var store = new SessionStore(_dir);
            var s = new ChatSession { Title = "Alt" };
            store.Save(s);

            store.Rename(s.Id, "Neu");
            Assert.Equal("Neu", store.Load(s.Id).Title);

            store.SetPinned(s.Id, true);
            Assert.True(store.Load(s.Id).Pinned);

            store.Delete(s.Id);
            Assert.Empty(store.List());
        }

        [Fact]
        public void List_SkipsCorruptFile_DoesNotThrow()
        {
            var store = new SessionStore(_dir);
            var s = new ChatSession { Title = "Gut" };
            store.Save(s);
            File.WriteAllText(Path.Combine(_dir, "kaputt.json"), "{ das ist kein gueltiges json ");

            var list = store.List();
            Assert.Single(list);
            Assert.Equal("Gut", list[0].Title);
        }
    }
}
