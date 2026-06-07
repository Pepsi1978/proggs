using System;
using System.Collections.Generic;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services.Llm;

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
            if (id == Active.Id) Active.Title = string.IsNullOrWhiteSpace(title) ? Active.Title : title.Trim();
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
                if (m.Role == LlmRole.User && !string.IsNullOrWhiteSpace(m.Text))
                {
                    var t = m.Text.Trim();
                    Active.Title = t.Length <= 40 ? t : t.Substring(0, 40) + "…";
                    return;
                }
            }
        }

        public IReadOnlyList<SessionInfo> List() => _store.List();
    }
}
