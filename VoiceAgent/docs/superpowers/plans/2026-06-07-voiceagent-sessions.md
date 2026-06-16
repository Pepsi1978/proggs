# VoiceAgent Sessions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** VoiceAgent erhaelt eine Hermes-artige Sessions-Sidebar (Layout C), kontextsichere Einstellungen und eine verlustarme Hintergrund-Komprimierung pro Session.

**Architecture:** Persistenz als eine JSON-Datei pro Session (`SessionStore`). Eine aktive Session lebt im `SessionManager`, entkoppelt vom Anbieter-/Einstellungs-Lebenszyklus. `BossAgent` arbeitet auf der aktiven Session (Verlauf + Zusammenfassung). `ContextCompressor` fasst ab einer Schwelle die aeltesten Nachrichten zusammen. Die WPF-Oberflaeche bindet an `SessionManager`.

**Tech Stack:** C# 14 / .NET 10 / WPF, System.Text.Json, xUnit-Tests (FakeProvider-Muster), Observability ueber `Diagnostics/Log` + `Diagnostics/Probe`.

---

## Pre-Flight (vor dem ersten Edit, PFLICHT)

Vor der ersten Code-Aenderung an C#/WPF MUSS gelesen werden (der `bug-almanac-guard` blockiert sonst Edits):
1. `bugs/desktop/dotnet-csharp.md`
2. `best-practices/desktop/dotnet-csharp.md` (falls vorhanden)

Cross-Platform: VoiceAgent ist Windows-only — kein macOS-Gegenstueck noetig.

Konventionen aus dem Bestand: Namespace `VoiceAgent.Core`; JSON via `System.Text.Json`; `Log.Info/Error`, `Probe.That(...)`; Tests in `tests/VoiceAgent.Tests`, xUnit, `internal sealed class FakeProvider : ILlmProvider` existiert bereits in `BossAgentTests.cs`.

**Befehle:**
- Bauen: `dotnet build VoiceAgent/VoiceAgent.slnx -c Debug`
- Tests: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj`
- Einzelner Test: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~SessionStoreTests"`

---

## File Structure

**Neu:**
- `src/VoiceAgent/Core/ChatSession.cs` — Session-Modell + `SessionInfo` (leichte Metadaten).
- `src/VoiceAgent/Core/SessionStore.cs` — Persistenz (atomar): list/load/save/delete/rename/pin.
- `src/VoiceAgent/Core/SessionManager.cs` — aktive Session + Lebenszyklus, entkoppelt von Settings.
- `src/VoiceAgent/Core/ContextCompressor.cs` — Groessen-Schaetzung + Hintergrund-Zusammenfassung.
- `tests/VoiceAgent.Tests/SessionStoreTests.cs`, `SessionManagerTests.cs`, `ContextCompressorTests.cs`.

**Geaendert:**
- `src/VoiceAgent/Core/BossAgent.cs` — arbeitet auf einer `ChatSession` (Verlauf + Summary).
- `tests/VoiceAgent.Tests/BossAgentTests.cs` — an die neue Signatur angepasst.
- `src/VoiceAgent/MainWindow.xaml` + `MainWindow.xaml.cs` — Sidebar, Transkript, Kontext-Anzeige, `BuildAgents` ohne Session-Verlust.

---

## Phase 1 — Persistenz-Kern

### Task 1: ChatSession-Modell

**Files:**
- Create: `src/VoiceAgent/Core/ChatSession.cs`

- [ ] **Step 1: Modell schreiben**

```csharp
using System;
using System.Collections.Generic;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>Eine gespeicherte Unterhaltung (eine Session) inkl. Verlauf und laufender Zusammenfassung.</summary>
    public sealed class ChatSession
    {
        public string Id { get; set; } = Guid.NewGuid().ToString("N");
        public string Title { get; set; } = DefaultTitle;
        public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.Now;
        public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.Now;
        public bool Pinned { get; set; }

        /// <summary>Verdichtete Zusammenfassung der bereits komprimierten aelteren Turns ("" = keine).</summary>
        public string Summary { get; set; } = "";

        /// <summary>Wortwoertlicher Gespraechsverlauf (User/Assistant).</summary>
        public List<LlmMessage> History { get; set; } = new();

        public const string DefaultTitle = "Neue Session";

        /// <summary>Leichte Kopie der Metadaten fuer die Sidebar.</summary>
        public SessionInfo ToInfo() => new(Id, Title, UpdatedAt, Pinned);
    }

    /// <summary>Leichte Metadaten fuer die Sidebar (ohne den Verlauf zu laden).</summary>
    public sealed record SessionInfo(string Id, string Title, DateTimeOffset UpdatedAt, bool Pinned);
}
```

- [ ] **Step 2: Bauen**

Run: `dotnet build VoiceAgent/VoiceAgent.slnx -c Debug`
Expected: Build erfolgreich (LlmMessage/LlmRole sind bereits vorhanden).

- [ ] **Step 3: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/Core/ChatSession.cs
git commit -m "#NNN - VoiceAgent: ChatSession model + SessionInfo for session persistence"
```

---

### Task 2: SessionStore (atomare Persistenz)

**Files:**
- Create: `src/VoiceAgent/Core/SessionStore.cs`
- Test: `tests/VoiceAgent.Tests/SessionStoreTests.cs`

- [ ] **Step 1: Failing test schreiben**

```csharp
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
            // Neueste zuerst
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
```

- [ ] **Step 2: Test laufen lassen — muss fehlschlagen**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~SessionStoreTests"`
Expected: FAIL (Compile-Fehler: `SessionStore` existiert nicht).

- [ ] **Step 3: SessionStore implementieren**

```csharp
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Persistiert Sessions als je eine JSON-Datei unter
    /// %LOCALAPPDATA%\VoiceAgent\sessions\&lt;id&gt;.json. Atomares Schreiben (Temp -> Move),
    /// defensiv gegen kaputte/fehlende Dateien (eine unlesbare Session stoppt nie die App).
    /// </summary>
    public sealed class SessionStore
    {
        private readonly string _dir;
        private static readonly JsonSerializerOptions _json = new()
        {
            WriteIndented = true,
            Converters = { new JsonStringEnumConverter() }
        };

        /// <summary>dir = null -> Standardpfad. Eigener Pfad ist fuer Tests gedacht.</summary>
        public SessionStore(string? dir = null)
        {
            _dir = dir ?? Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "VoiceAgent", "sessions");
            try { Directory.CreateDirectory(_dir); }
            catch (Exception ex) { Log.Error("SessionStore: Verzeichnis anlegen fehlgeschlagen", ex, new { dir = _dir }); }
        }

        private string PathFor(string id) => Path.Combine(_dir, id + ".json");

        public void Save(ChatSession session)
        {
            try
            {
                Directory.CreateDirectory(_dir);
                var dest = PathFor(session.Id);
                var tmp = dest + ".tmp";
                File.WriteAllText(tmp, JsonSerializer.Serialize(session, _json), Encoding.UTF8);
                File.Move(tmp, dest, overwrite: true);   // atomar genug fuer eine lokale Datei
            }
            catch (Exception ex)
            {
                Log.Error("SessionStore: Speichern fehlgeschlagen", ex, new { id = session.Id });
            }
        }

        public ChatSession Load(string id)
        {
            var path = PathFor(id);
            var s = JsonSerializer.Deserialize<ChatSession>(File.ReadAllText(path), _json);
            if (s == null) throw new InvalidDataException("Leere Session-Datei: " + path);
            return s;
        }

        public IReadOnlyList<SessionInfo> List()
        {
            var result = new List<SessionInfo>();
            if (!Directory.Exists(_dir)) return result;
            foreach (var file in Directory.EnumerateFiles(_dir, "*.json"))
            {
                try
                {
                    var s = JsonSerializer.Deserialize<ChatSession>(File.ReadAllText(file), _json);
                    if (s != null) result.Add(s.ToInfo());
                }
                catch (Exception ex)
                {
                    Log.Error("SessionStore: ueberspringe unlesbare Session-Datei", ex, new { file });
                }
            }
            // Angepinnte zuerst, dann neueste zuerst.
            return result
                .OrderByDescending(i => i.Pinned)
                .ThenByDescending(i => i.UpdatedAt)
                .ToList();
        }

        public void Delete(string id)
        {
            try { var p = PathFor(id); if (File.Exists(p)) File.Delete(p); }
            catch (Exception ex) { Log.Error("SessionStore: Loeschen fehlgeschlagen", ex, new { id }); }
        }

        public void Rename(string id, string title)
        {
            var s = Load(id);
            s.Title = string.IsNullOrWhiteSpace(title) ? s.Title : title.Trim();
            s.UpdatedAt = DateTimeOffset.Now;
            Save(s);
        }

        public void SetPinned(string id, bool pinned)
        {
            var s = Load(id);
            s.Pinned = pinned;
            Save(s);
        }
    }
}
```

- [ ] **Step 4: Tests laufen lassen — muessen gruen sein**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~SessionStoreTests"`
Expected: PASS (4 Tests).

- [ ] **Step 5: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/Core/SessionStore.cs VoiceAgent/tests/VoiceAgent.Tests/SessionStoreTests.cs
git commit -m "#NNN - VoiceAgent: SessionStore with atomic JSON persistence + tests"
```

---

### Task 3: SessionManager (aktive Session)

**Files:**
- Create: `src/VoiceAgent/Core/SessionManager.cs`
- Test: `tests/VoiceAgent.Tests/SessionManagerTests.cs`

- [ ] **Step 1: Failing test schreiben**

```csharp
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
```

- [ ] **Step 2: Test laufen lassen — muss fehlschlagen**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~SessionManagerTests"`
Expected: FAIL (`SessionManager` existiert nicht).

- [ ] **Step 3: SessionManager implementieren**

```csharp
using System;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Haelt die AKTIVE Session und entkoppelt sie vom Anbieter-/Einstellungs-Lebenszyklus.
    /// Dadurch ueberlebt der Gespraechsverlauf das Neubauen des Providers (Settings-Fix).
    /// </summary>
    public sealed class SessionManager
    {
        private readonly SessionStore _store;
        public ChatSession Active { get; private set; } = new();

        /// <summary>Feuert, wenn sich die aktive Session aendert (neu/gewechselt) — die UI haengt sich hier ein.</summary>
        public event Action? ActiveChanged;

        public SessionManager(SessionStore store)
        {
            _store = store;
        }

        public void NewSession()
        {
            Active = new ChatSession();
            Log.Info("Session: neue Session gestartet", new { id = Active.Id });
            ActiveChanged?.Invoke();
        }

        public void Switch(string id)
        {
            try
            {
                Active = _store.Load(id);
                Log.Info("Session: gewechselt", new { id, turns = Active.History.Count });
                ActiveChanged?.Invoke();
            }
            catch (Exception ex)
            {
                Log.Error("Session: Wechsel fehlgeschlagen", ex, new { id });
            }
        }

        public void SaveActive()
        {
            Active.UpdatedAt = DateTimeOffset.Now;
            _store.Save(Active);
        }

        public void Rename(string id, string title)
        {
            _store.Rename(id, title);
            if (id == Active.Id) Active.Title = title;
            ActiveChanged?.Invoke();
        }

        public void SetPinned(string id, bool pinned)
        {
            _store.SetPinned(id, pinned);
            if (id == Active.Id) Active.Pinned = pinned;
            ActiveChanged?.Invoke();
        }

        public void Delete(string id)
        {
            _store.Delete(id);
            if (id == Active.Id) NewSession();
            else ActiveChanged?.Invoke();
        }

        /// <summary>Setzt den Titel einmalig aus der ersten Nutzer-Nachricht (sonst bleibt der Default).</summary>
        public void EnsureTitleFromFirstMessage()
        {
            if (Active.Title != ChatSession.DefaultTitle) return;
            foreach (var m in Active.History)
            {
                if (m.Role == Services.Llm.LlmRole.User && !string.IsNullOrWhiteSpace(m.Text))
                {
                    var t = m.Text.Trim();
                    Active.Title = t.Length <= 40 ? t : t.Substring(0, 40) + "…";
                    return;
                }
            }
        }

        public System.Collections.Generic.IReadOnlyList<SessionInfo> List() => _store.List();
    }
}
```

- [ ] **Step 4: Tests laufen lassen — muessen gruen sein**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~SessionManagerTests"`
Expected: PASS (4 Tests).

- [ ] **Step 5: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/Core/SessionManager.cs VoiceAgent/tests/VoiceAgent.Tests/SessionManagerTests.cs
git commit -m "#NNN - VoiceAgent: SessionManager (active session decoupled from provider) + tests"
```

---

## Phase 2 — Agent-Integration, Komprimierung, Settings-Fix

### Task 4: BossAgent auf Session umstellen

**Files:**
- Modify: `src/VoiceAgent/Core/BossAgent.cs`
- Modify: `tests/VoiceAgent.Tests/BossAgentTests.cs`

- [ ] **Step 1: Bestehende Tests anpassen (werden zuerst rot)**

Ersetze `BossAgentTests.cs` vollstaendig durch:

```csharp
using System.Linq;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    internal sealed class FakeProvider : ILlmProvider
    {
        private readonly string _reply;
        public FakeProvider(string reply) => _reply = reply;
        public string Name => "Fake";
        public System.Threading.Tasks.Task<string> ChatAsync(
            System.Collections.Generic.IReadOnlyList<LlmMessage> messages,
            System.Threading.CancellationToken ct = default) => Task.FromResult(_reply);
    }

    public class BossAgentTests
    {
        private static BossAgent NewAgent(ILlmProvider p, string? prompt)
        {
            var session = new ChatSession();
            return new BossAgent(p, prompt, session);
        }

        [Fact]
        public async Task RespondAsync_AddsUserAndAssistantToHistory()
        {
            var agent = NewAgent(new FakeProvider("Antwort"), "SYS");
            var reply = await agent.RespondAsync("Hallo");

            Assert.Equal("Antwort", reply);
            Assert.Equal(2, agent.History.Count);
            Assert.Equal(LlmRole.User, agent.History[0].Role);
            Assert.Equal("Hallo", agent.History[0].Text);
            Assert.Equal(LlmRole.Assistant, agent.History[1].Role);
        }

        [Fact]
        public void BuildMessages_StartsWithSystemPrompt()
        {
            var agent = NewAgent(new FakeProvider("x"), "MEIN-PROMPT");
            var msgs = agent.BuildMessages();
            Assert.Equal(LlmRole.System, msgs[0].Role);
            Assert.StartsWith("MEIN-PROMPT", msgs[0].Text);
        }

        [Fact]
        public void EmptySystemPrompt_FallsBackToDefault()
        {
            var agent = NewAgent(new FakeProvider("x"), "");
            Assert.StartsWith(BossAgentPrompt.Default, agent.BuildMessages()[0].Text);
        }

        [Fact]
        public async Task BuildMessages_IncludesSummaryBlock_WhenPresent()
        {
            var session = new ChatSession { Summary = "Frank plant eine Reise." };
            var agent = new BossAgent(new FakeProvider("ok"), "SYS", session);
            await agent.RespondAsync("Und weiter?");
            var msgs = agent.BuildMessages();
            // Ein zweiter System-Block traegt die Zusammenfassung.
            Assert.Contains(msgs, m => m.Role == LlmRole.System && m.Text.Contains("Frank plant eine Reise."));
        }
    }
}
```

- [ ] **Step 2: Tests laufen lassen — muessen fehlschlagen**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~BossAgentTests"`
Expected: FAIL (Konstruktor ohne `ChatSession` existiert noch; `RespondAsync` schreibt noch in `_history`).

- [ ] **Step 3: BossAgent umstellen**

Ersetze in `src/VoiceAgent/Core/BossAgent.cs` das Feld `_history`, den Konstruktor, `History`, `BuildMessages`, die Schreibstellen und `Reset` so:

```csharp
        private readonly ILlmProvider _provider;
        private readonly string _systemPrompt;
        private readonly AgentMemory? _memory;
        private readonly SubAgentRegistry? _subAgents;
        private readonly string? _timeZoneId;
        private readonly ChatSession _session;   // <-- statt eigener _history-Liste

        public BossAgent(ILlmProvider provider, string? systemPrompt, ChatSession session,
            AgentMemory? memory = null, SubAgentRegistry? subAgents = null, string? timeZoneId = null)
        {
            _provider = provider;
            _systemPrompt = string.IsNullOrWhiteSpace(systemPrompt) ? BossAgentPrompt.Default : systemPrompt!;
            _session = session;
            _memory = memory;
            _subAgents = subAgents;
            _timeZoneId = timeZoneId;
        }

        public IReadOnlyList<LlmMessage> History => _session.History;
        public string ProviderName => _provider.Name;

        public IReadOnlyList<LlmMessage> BuildMessages()
        {
            var systemText = _systemPrompt
                + AgentCapabilities.BuildBlock()
                + TimeContext.BuildBlock(_timeZoneId)
                + (_memory?.ContextBlock() ?? string.Empty);

            var list = new List<LlmMessage>(_session.History.Count + 2)
            {
                new LlmMessage(LlmRole.System, systemText)
            };
            if (!string.IsNullOrWhiteSpace(_session.Summary))
                list.Add(new LlmMessage(LlmRole.System,
                    "ZUSAMMENFASSUNG DES BISHERIGEN GESPRAECHS (aeltere Teile dieser Session):\n" + _session.Summary));
            list.AddRange(_session.History);
            return list;
        }
```

In `RespondAsync` und `HandleAsync` jedes `_history.Add(...)` durch `_session.History.Add(...)` ersetzen. `Reset()` ersetzen durch:

```csharp
        /// <summary>Leert den Verlauf der aktuellen Session (selten gebraucht — normal: neue Session).</summary>
        public void Reset()
        {
            _session.History.Clear();
            _session.Summary = "";
            Log.Info("BossAgent: Verlauf der aktiven Session zurueckgesetzt");
        }
```

- [ ] **Step 4: Tests laufen lassen — muessen gruen sein**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~BossAgentTests"`
Expected: PASS (4 Tests).

- [ ] **Step 5: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/Core/BossAgent.cs VoiceAgent/tests/VoiceAgent.Tests/BossAgentTests.cs
git commit -m "#NNN - VoiceAgent: BossAgent operates on active ChatSession (history + summary)"
```

---

### Task 5: ContextCompressor

**Files:**
- Create: `src/VoiceAgent/Core/ContextCompressor.cs`
- Test: `tests/VoiceAgent.Tests/ContextCompressorTests.cs`

- [ ] **Step 1: Failing test schreiben**

```csharp
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Core;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Tests
{
    public class ContextCompressorTests
    {
        private static ChatSession SessionWith(int messages)
        {
            var s = new ChatSession();
            for (int i = 0; i < messages; i++)
                s.History.Add(new LlmMessage(i % 2 == 0 ? LlmRole.User : LlmRole.Assistant,
                    "Nachricht Nummer " + i + " mit etwas Text damit sie zaehlt."));
            return s;
        }

        [Fact]
        public async Task UnderThreshold_NoChange()
        {
            var s = SessionWith(2);
            var comp = new ContextCompressor(budgetTokens: 100000, threshold: 0.75, keepRecent: 4);
            var changed = await comp.MaybeCompressAsync(s, new FakeProvider("ZUSAMMENFASSUNG"), CancellationToken.None);
            Assert.False(changed);
            Assert.Equal(2, s.History.Count);
            Assert.Equal("", s.Summary);
        }

        [Fact]
        public async Task OverThreshold_SummarizesOldestKeepsRecent()
        {
            var s = SessionWith(20);
            var comp = new ContextCompressor(budgetTokens: 50, threshold: 0.75, keepRecent: 4);
            var changed = await comp.MaybeCompressAsync(s, new FakeProvider("KOMPAKTE ZUSAMMENFASSUNG"), CancellationToken.None);

            Assert.True(changed);
            Assert.Equal(4, s.History.Count);                    // nur die letzten 4 bleiben wortwoertlich
            Assert.Contains("KOMPAKTE ZUSAMMENFASSUNG", s.Summary);
            Assert.Equal("Nachricht Nummer 16 mit etwas Text damit sie zaehlt.", s.History[0].Text);
        }

        [Fact]
        public void EstimateTokens_GrowsWithContent()
        {
            var small = SessionWith(1);
            var big = SessionWith(40);
            Assert.True(ContextCompressor.EstimateTokens(big) > ContextCompressor.EstimateTokens(small));
        }
    }
}
```

- [ ] **Step 2: Test laufen lassen — muss fehlschlagen**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~ContextCompressorTests"`
Expected: FAIL (`ContextCompressor` existiert nicht).

- [ ] **Step 3: ContextCompressor implementieren**

```csharp
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Verlustarme Hintergrund-Komprimierung pro Session: ab einer Schwelle werden die
    /// aeltesten Nachrichten zu einer kompakten Zusammenfassung verdichtet (per LLM) und aus
    /// dem wortwoertlichen Verlauf entfernt; die letzten K Nachrichten bleiben im Original.
    /// </summary>
    public sealed class ContextCompressor
    {
        private readonly int _budgetTokens;
        private readonly double _threshold;
        private readonly int _keepRecent;

        public ContextCompressor(int budgetTokens = 12000, double threshold = 0.75, int keepRecent = 6)
        {
            _budgetTokens = budgetTokens;
            _threshold = threshold;
            _keepRecent = keepRecent;
        }

        /// <summary>Grobe Token-Schaetzung (Zeichen/4) ueber Summary + Verlauf — kein Tokenizer noetig.</summary>
        public static int EstimateTokens(ChatSession s)
        {
            long chars = s.Summary?.Length ?? 0;
            foreach (var m in s.History) chars += m.Text?.Length ?? 0;
            return (int)(chars / 4);
        }

        /// <summary>Fuellstand 0..1 fuer die Kontext-Anzeige.</summary>
        public double Fill(ChatSession s) => Math.Min(1.0, (double)EstimateTokens(s) / _budgetTokens);

        /// <summary>Komprimiert, falls noetig. Liefert true, wenn etwas verdichtet wurde.</summary>
        public async Task<bool> MaybeCompressAsync(ChatSession s, ILlmProvider provider, CancellationToken ct = default)
        {
            if (EstimateTokens(s) < _budgetTokens * _threshold) return false;
            if (s.History.Count <= _keepRecent) return false;

            int cut = s.History.Count - _keepRecent;
            var older = s.History.GetRange(0, cut);

            var sb = new StringBuilder();
            if (!string.IsNullOrWhiteSpace(s.Summary))
                sb.Append("Bisherige Zusammenfassung:\n").Append(s.Summary).Append("\n\n");
            sb.AppendLine("Fasse den folgenden Gespraechsausschnitt KNAPP zusammen. Behalte Fakten, Entscheidungen, offene Aufgaben und Namen. Schreibe in Stichpunkten, kein Vorwort:");
            foreach (var m in older)
            {
                var who = m.Role == LlmRole.User ? "Frank" : "Agent";
                sb.Append(who).Append(": ").AppendLine(m.Text);
            }

            try
            {
                Log.Info("Komprimierung gestartet", new { before = EstimateTokens(s), cut, keep = _keepRecent });
                var summary = await provider.ChatAsync(
                    new List<LlmMessage> { new LlmMessage(LlmRole.User, sb.ToString()) }, ct).ConfigureAwait(false);

                if (string.IsNullOrWhiteSpace(summary))
                {
                    Log.Warn("Komprimierung lieferte leere Zusammenfassung — Verlauf bleibt unveraendert");
                    return false;
                }

                s.Summary = string.IsNullOrWhiteSpace(s.Summary) ? summary.Trim() : summary.Trim();
                s.History.RemoveRange(0, cut);
                Log.Info("Komprimierung fertig", new { after = EstimateTokens(s), remaining = s.History.Count });
                Probe.That(s.History.Count == _keepRecent, "ContextCompressor: unerwartete Verlaufslaenge nach Komprimierung",
                    new { count = s.History.Count, keep = _keepRecent });
                return true;
            }
            catch (Exception ex)
            {
                Log.Error("Komprimierung fehlgeschlagen — Verlauf bleibt vollstaendig erhalten", ex);
                return false;   // Funktionserhalt: lieber nicht komprimieren als Wissen verlieren
            }
        }
    }
}
```

- [ ] **Step 4: Tests laufen lassen — muessen gruen sein**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj --filter "FullyQualifiedName~ContextCompressorTests"`
Expected: PASS (3 Tests).

- [ ] **Step 5: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/Core/ContextCompressor.cs VoiceAgent/tests/VoiceAgent.Tests/ContextCompressorTests.cs
git commit -m "#NNN - VoiceAgent: ContextCompressor (per-session background summarization) + tests"
```

---

## Phase 3 — UI (WPF) & Settings-Fix

> WPF-Oberflaeche laesst sich nicht sinnvoll per Unit-Test pruefen. Diese Tasks sind
> **bauen → App starten → manuell pruefen**, mit einer Observability-Sonde als automatischem
> Beweis fuer den Verlaufs-Erhalt (Task 9).

### Task 6: SessionManager + Compressor in MainWindow verdrahten (ohne Sidebar)

**Files:**
- Modify: `src/VoiceAgent/MainWindow.xaml.cs`

- [ ] **Step 1: Felder + Initialisierung**

In `MainWindow.xaml.cs` Felder ergaenzen (neben `_agent`):

```csharp
        private SessionStore? _sessionStore;
        private SessionManager? _sessions;
        private ContextCompressor? _compressor;
```

Im Konstruktor/Init (bei den anderen `new ...`, vor `BuildAgents()`):

```csharp
            _sessionStore = new SessionStore();
            _sessions = new SessionManager(_sessionStore);
            _sessions.NewSession();                  // Start: frische Session
            _compressor = new ContextCompressor();   // Default-Budget/Schwelle
```

- [ ] **Step 2: BuildAgents an die aktive Session koppeln (Settings-Fix, Kern)**

`BuildAgents()` so aendern, dass der Agent die AKTIVE Session bekommt — und beim erneuten
Aufruf (Settings speichern) dieselbe Session weiterverwendet:

```csharp
        private void BuildAgents()
        {
            _agent = new BossAgent(LlmProviderFactory.Create(_settings), _settings.SystemPrompt,
                _sessions!.Active, _memory, _subAgents, _settings.TimeZoneId);
            _endpoint = new EndpointDetector(new GeminiProvider(Config.ReadApiKey("gemini"), _settings.EndpointModel));
            _intentDetector = new IntentDetector(new GeminiProvider(Config.ReadApiKey("gemini"), _settings.IntentModel));
            _tts = new GoogleTtsClient(Config.ReadApiKey("google"));
            _stt = new GroqWhisperClient(Config.ReadApiKey("groq"), _settings.SttModel, _settings.SttLanguage);
        }
```

Weil `_sessions.Active` dasselbe Objekt bleibt, loescht das erneute `BuildAgents()` in
`SettingsButton_Click` den Verlauf NICHT mehr.

- [ ] **Step 3: Turn-Fluss um Speichern + Komprimierung erweitern**

In `RespondAndSpeakAsync`, nach `_turn?.Responded(...)` und vor/nach `Append("Agent", reply)`:

```csharp
            _sessions!.EnsureTitleFromFirstMessage();
            await _compressor!.MaybeCompressAsync(_sessions.Active, _agent!.ProviderNameProvider ?? LlmProviderFactory.Create(_settings));
            _sessions.SaveActive();
            RefreshContextMeter();   // in Task 8 definiert
            RefreshSessionList();    // in Task 7 definiert
```

> Hinweis: Die Komprimierung braucht einen `ILlmProvider`. Statt einer neuen Provider-Instanz
> pro Turn wird in Task 7 ein Feld `_provider` eingefuehrt und hier `_compressor.MaybeCompressAsync(_sessions.Active, _provider!)` verwendet. Fuer diesen Zwischenschritt genuegt `LlmProviderFactory.Create(_settings)`.

- [ ] **Step 4: Bauen**

Run: `dotnet build VoiceAgent/VoiceAgent.slnx -c Debug`
Expected: Build erfolgreich.

- [ ] **Step 5: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/MainWindow.xaml.cs
git commit -m "#NNN - VoiceAgent: wire SessionManager + ContextCompressor into turn flow; settings keep active session"
```

---

### Task 7: Sidebar-UI (Layout C)

**Files:**
- Modify: `src/VoiceAgent/MainWindow.xaml`
- Modify: `src/VoiceAgent/MainWindow.xaml.cs`

- [ ] **Step 1: Layout in MainWindow.xaml**

Den Wurzel-Container in zwei Spalten teilen: links die Sidebar (Breite 210, einklappbar),
rechts der bestehende Inhalt. Beispielhafte Struktur (an die vorhandenen Namen/Stile anpassen):

```xml
<Grid>
  <Grid.ColumnDefinitions>
    <ColumnDefinition x:Name="SidebarCol" Width="210"/>
    <ColumnDefinition Width="*"/>
  </Grid.ColumnDefinitions>

  <!-- Sidebar (warm-minimal) -->
  <Border Grid.Column="0" Background="#F3F0EA" BorderBrush="#E7E2D8" BorderThickness="0,0,1,0">
    <DockPanel Margin="10">
      <Button DockPanel.Dock="Top" x:Name="NewSessionButton" Content="＋ Neue Session"
              Click="NewSessionButton_Click" Height="34" Margin="0,0,0,8"
              Background="#1F1B16" Foreground="White" BorderThickness="0"/>
      <TextBox DockPanel.Dock="Top" x:Name="SearchBox" Height="30" Margin="0,0,0,8"
               TextChanged="SearchBox_TextChanged" Tag="Sessions durchsuchen…"/>
      <ListBox x:Name="SessionList" BorderThickness="0" Background="Transparent"
               SelectionChanged="SessionList_SelectionChanged">
        <ListBox.ItemTemplate>
          <DataTemplate>
            <DockPanel>
              <TextBlock Text="{Binding Title}" TextTrimming="CharacterEllipsis"/>
            </DockPanel>
          </DataTemplate>
        </ListBox.ItemTemplate>
        <ListBox.ContextMenu>
          <ContextMenu>
            <MenuItem Header="Umbenennen" Click="RenameSession_Click"/>
            <MenuItem Header="Anpinnen" Click="PinSession_Click"/>
            <MenuItem Header="Löschen" Click="DeleteSession_Click"/>
          </ContextMenu>
        </ListBox.ContextMenu>
      </ListBox>
    </DockPanel>
  </Border>

  <!-- Bestehender Hauptbereich kommt nach Grid.Column="1" -->
  <Grid Grid.Column="1">
    <!-- ... vorhandener Inhalt (Status, Verlauf, Eingabe) hierher ... -->
  </Grid>
</Grid>
```

- [ ] **Step 2: Code-behind — Liste fuellen + Aktionen**

In `MainWindow.xaml.cs` ein Feld `_provider` einfuehren und in `BuildAgents()` setzen
(`_provider = LlmProviderFactory.Create(_settings);`), dann an `BossAgent`/Compressor weiterreichen.
Sidebar-Logik:

```csharp
        private void RefreshSessionList()
        {
            var query = SearchBox.Text?.Trim() ?? "";
            var items = _sessions!.List();
            if (!string.IsNullOrEmpty(query) && query != "Sessions durchsuchen…")
                items = items.Where(i => i.Title.Contains(query, System.StringComparison.OrdinalIgnoreCase)).ToList();
            SessionList.ItemsSource = items;
        }

        private void NewSessionButton_Click(object sender, RoutedEventArgs e)
        {
            _sessions!.SaveActive();
            _sessions.NewSession();
            BuildAgents();          // Agent zeigt auf die neue (leere) Session
            ClearTranscript();      // in Task 8
            RefreshSessionList();
            RefreshContextMeter();
        }

        private void SessionList_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (SessionList.SelectedItem is SessionInfo info && info.Id != _sessions!.Active.Id)
            {
                _sessions.SaveActive();
                _sessions.Switch(info.Id);
                BuildAgents();
                RenderTranscript();   // in Task 8: Verlauf der gewechselten Session anzeigen
                RefreshContextMeter();
            }
        }

        private void SearchBox_TextChanged(object sender, TextChangedEventArgs e) => RefreshSessionList();

        private SessionInfo? SelectedInfo() => SessionList.SelectedItem as SessionInfo;

        private void RenameSession_Click(object sender, RoutedEventArgs e)
        {
            if (SelectedInfo() is not { } info) return;
            var name = Microsoft.VisualBasic.Interaction.InputBox("Neuer Name:", "Umbenennen", info.Title);
            if (!string.IsNullOrWhiteSpace(name)) { _sessions!.Rename(info.Id, name); RefreshSessionList(); }
        }

        private void PinSession_Click(object sender, RoutedEventArgs e)
        {
            if (SelectedInfo() is { } info) { _sessions!.SetPinned(info.Id, !info.Pinned); RefreshSessionList(); }
        }

        private void DeleteSession_Click(object sender, RoutedEventArgs e)
        {
            if (SelectedInfo() is not { } info) return;
            if (MessageBox.Show($"Session „{info.Title}" löschen?", "Löschen", MessageBoxButton.YesNo) == MessageBoxResult.Yes)
            {
                _sessions!.Delete(info.Id);
                BuildAgents();
                RenderTranscript();
                RefreshSessionList();
            }
        }
```

> `Microsoft.VisualBasic.Interaction.InputBox` braucht das Assembly `Microsoft.VisualBasic`
> (in .NET 10 verfuegbar). Falls unerwuenscht: einfacher eigener WPF-Eingabedialog. Fuer den
> ersten Wurf genuegt InputBox.

`using System.Linq;` sicherstellen. `_sessions.ActiveChanged += RefreshSessionList;` im Init registrieren.

- [ ] **Step 3: Bauen**

Run: `dotnet build VoiceAgent/VoiceAgent.slnx -c Debug`
Expected: Build erfolgreich.

- [ ] **Step 4: Manuell pruefen**

App starten. Erwartet: Sidebar links; „Neue Session" legt leere Session an; nach einer
Nachricht erscheint die Session mit Auto-Titel in der Liste; Wechsel zwischen Sessions laedt
den jeweiligen Verlauf; Rechtsklick → Umbenennen/Anpinnen/Löschen funktionieren; Suche filtert.

- [ ] **Step 5: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/MainWindow.xaml VoiceAgent/src/VoiceAgent/MainWindow.xaml.cs
git commit -m "#NNN - VoiceAgent: Hermes-style session sidebar (new/switch/search/rename/pin/delete)"
```

---

### Task 8: Transkript-Ansicht + Kontext-Anzeige

**Files:**
- Modify: `src/VoiceAgent/MainWindow.xaml`
- Modify: `src/VoiceAgent/MainWindow.xaml.cs`

- [ ] **Step 1: Kontext-Pille + Transkript-Container im XAML**

Oben rechts im Hauptbereich eine Pille, die den Fuellstand zeigt:

```xml
<Border x:Name="ContextPill" CornerRadius="12" Background="#F4EAD6" Padding="9,4"
        HorizontalAlignment="Right" Margin="0,8,16,0">
  <TextBlock x:Name="ContextPillText" Foreground="#9A6B2F" Text="Kontext 0%"/>
</Border>
```

Der bestehende Verlauf-Anzeigebereich wird zum Transkript (Append schreibt weiterhin dorthin).

- [ ] **Step 2: Helfer im Code-behind**

```csharp
        private void RefreshContextMeter()
        {
            if (_compressor == null || _sessions == null) return;
            int pct = (int)System.Math.Round(_compressor.Fill(_sessions.Active) * 100);
            ContextPillText.Text = pct >= 75 ? $"Kontext {pct}% · wird komprimiert…" : $"Kontext {pct}%";
        }

        private void ClearTranscript() => TranscriptPanel.Children.Clear();   // Name an vorhandenes Panel anpassen

        private void RenderTranscript()
        {
            ClearTranscript();
            foreach (var m in _sessions!.Active.History)
                Append(m.Role == LlmRole.Assistant ? "Agent" : "Frank", m.Text);
        }
```

> `Append(...)` existiert bereits (zeigt Zeilen im Verlauf). `TranscriptPanel` ist der vorhandene
> Container — den korrekten Namen aus der bestehenden `MainWindow.xaml` einsetzen.

- [ ] **Step 3: Bauen + manuell pruefen**

Run: `dotnet build VoiceAgent/VoiceAgent.slnx -c Debug`
Expected: Build erfolgreich. App starten: Kontext-Pille steigt mit dem Gespraech; bei ~75 %
erscheint „wird komprimiert…"; Session-Wechsel zeigt den richtigen Verlauf.

- [ ] **Step 4: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/MainWindow.xaml VoiceAgent/src/VoiceAgent/MainWindow.xaml.cs
git commit -m "#NNN - VoiceAgent: transcript view + per-session context meter pill"
```

---

### Task 9: Observability-Sonde für den Verlaufs-Erhalt + Settings-Test

**Files:**
- Modify: `src/VoiceAgent/Core/BossAgent.cs`
- Test: `tests/VoiceAgent.Tests/SessionManagerTests.cs` (ergaenzen)

- [ ] **Step 1: Checkpoint pro Turn in BossAgent**

In `BuildMessages()` am Ende vor `return list;` eine Live-Sonde einbauen (genau die, die den
Grau-Bug sofort sichtbar gemacht haette):

```csharp
            Probe.That(true, "CHECKPOINT Kontext-Block gebaut",
                new { messages = _session.History.Count, hasSummary = !string.IsNullOrWhiteSpace(_session.Summary) });
```

> Pruefe die echte `Probe.That`-Signatur in `Diagnostics/Probe.cs` und passe den Aufruf an
> (Bedingung, Meldung, ctx). Ziel: jeder Turn loggt, wie viele Nachrichten mitgehen.

- [ ] **Step 2: Regressions-Test „Settings erhalten die Session"**

In `SessionManagerTests.cs` ergaenzen:

```csharp
        [Fact]
        public async Task RebuildingAgent_KeepsSessionHistory()
        {
            var mgr = new SessionManager(_store);
            mgr.NewSession();
            var agent1 = new BossAgent(new FakeProvider("ok"), "SYS", mgr.Active);
            await agent1.RespondAsync("Erste Nachricht");
            Assert.Equal(2, mgr.Active.History.Count);

            // Simuliert „Einstellungen gespeichert -> Agent neu gebaut" mit DERSELBEN aktiven Session.
            var agent2 = new BossAgent(new FakeProvider("ok"), "SYS-GEAENDERT", mgr.Active);
            Assert.Equal(2, agent2.History.Count);                 // Verlauf bleibt erhalten
            await agent2.RespondAsync("Zweite Nachricht");
            Assert.Equal(4, mgr.Active.History.Count);
        }
```

(`using System.Threading.Tasks;` und `using VoiceAgent.Services.Llm;` sicherstellen.)

- [ ] **Step 3: Tests laufen lassen — muessen gruen sein**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj`
Expected: PASS (alle Tests).

- [ ] **Step 4: Commit**

```bash
git add VoiceAgent/src/VoiceAgent/Core/BossAgent.cs VoiceAgent/tests/VoiceAgent.Tests/SessionManagerTests.cs
git commit -m "#NNN - VoiceAgent: per-turn context checkpoint probe + settings-preserve-session regression test"
```

---

### Task 10: Gesamt-Verifikation

- [ ] **Step 1: Voller Testlauf**

Run: `dotnet test VoiceAgent/tests/VoiceAgent.Tests/VoiceAgent.Tests.csproj`
Expected: alle gruen.

- [ ] **Step 2: Manuelle End-to-End-Pruefung (die drei Wuensche)**

1. Mehrere Sessions anlegen, wechseln, umbenennen, anpinnen, löschen — Verlauf jeweils korrekt.
2. Gespraech fuehren → in **Einstellungen** Farbe auf **Grau** stellen, speichern, zurueck →
   Agent kennt den vorherigen Verlauf weiter (Grau-Bug behoben).
3. Langes Gespraech → Kontext-Pille steigt, ab ~75 % „wird komprimiert…", danach laeuft das
   Gespraech inhaltlich nahtlos weiter; **neue Session** startet wieder bei 0 %.

- [ ] **Step 3: Log pruefen**

`%LOCALAPPDATA%\VoiceAgent\logs\voiceagent.log` enthaelt Session-Lifecycle-, Komprimierungs-
und „CHECKPOINT Kontext-Block gebaut"-Eintraege mit `messages`-Zahl > 0 nach Settings-Save.

- [ ] **Step 4: Abschluss-Commit/Push**

```bash
git add -A VoiceAgent/
git commit -m "#NNN - VoiceAgent: sessions feature complete (sidebar, settings-safe context, background compression)"
git fetch origin && git rebase origin/main && git push
```

---

## Self-Review (gegen die Spec)

- **Spec-Abdeckung:** (1) Sessions-Sidebar → Tasks 1–3, 7. (2) Kontextsichere Einstellungen → Tasks 4, 6, 9. (3) Pro-Session-Komprimierung → Tasks 1, 5, 8. UI-Layout C → Tasks 7, 8. Observability → Tasks 5, 9. Tests → 2, 3, 4, 5, 9. ✓ keine offene Spec-Anforderung.
- **Platzhalter:** Keine „TBD/TODO"; UI-Tasks sind bewusst „build + manuelle Pruefung" (WPF), mit konkretem XAML/C#. Stellen, die an vorhandene Namen anzupassen sind (`TranscriptPanel`, `Probe.That`-Signatur, Hauptbereich-Container), sind explizit als Anpassung markiert — kein verstecktes Platzhalter-Verhalten.
- **Typ-Konsistenz:** `ChatSession`, `SessionInfo`, `SessionStore`, `SessionManager`, `ContextCompressor.EstimateTokens/Fill/MaybeCompressAsync`, `BossAgent(provider, prompt, session, …)` durchgaengig identisch verwendet. ✓
- **Commit-Nummern:** `#NNN` ist Platzhalter — beim Umsetzen je Commit die naechste fortlaufende Nummer einsetzen (Stand zuletzt: #46602).
