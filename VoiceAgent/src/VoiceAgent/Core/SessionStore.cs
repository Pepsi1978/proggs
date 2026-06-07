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

        // Statische, nach erstem Gebrauch eingefrorene Options-Instanz (CA1869).
        // JsonStringEnumConverter (generisch-unkritisch via Standard-Converter) haelt LlmRole lesbar.
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
