using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Persistiert die benutzerdefinierten Helfer-Definitionen (JSON) und laedt sie beim Start.
    /// So bleiben per Sprache angelegte Helfer ueber Neustarts erhalten. Atomar geschrieben
    /// (Temp-Datei + Replace), damit ein Absturz beim Schreiben die Datei nie zerstoert.
    ///
    /// Datei: %LOCALAPPDATA%\VoiceAgent\custom-agents.json
    /// </summary>
    public sealed class CustomAgentStore
    {
        private readonly string _path;
        private readonly object _lock = new();
        private readonly List<SubAgentDefinition> _defs = new();

        private static readonly JsonSerializerOptions Json = new()
        {
            WriteIndented = true,
            PropertyNameCaseInsensitive = true
        };

        public CustomAgentStore(string? path = null)
        {
            _path = path ?? DefaultPath();
            Load();
        }

        private static string DefaultPath()
        {
            try
            {
                var dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "VoiceAgent");
                Directory.CreateDirectory(dir);
                return Path.Combine(dir, "custom-agents.json");
            }
            catch
            {
                return Path.Combine(Path.GetTempPath(), "voiceagent-custom-agents.json");
            }
        }

        /// <summary>Momentaufnahme aller gespeicherten Definitionen (Kopie, thread-safe).</summary>
        public IReadOnlyList<SubAgentDefinition> All
        {
            get { lock (_lock) { return _defs.ToList(); } }
        }

        /// <summary>
        /// Fuegt eine Definition hinzu (oder ersetzt eine gleichnamige) und speichert sofort.
        /// Liefert false bei ungueltiger Definition (kein Name).
        /// </summary>
        public bool Add(SubAgentDefinition def)
        {
            if (def == null || string.IsNullOrWhiteSpace(def.Name)) return false;
            lock (_lock)
            {
                _defs.RemoveAll(d => string.Equals(d.Name, def.Name, StringComparison.OrdinalIgnoreCase));
                _defs.Add(def);
                Save();
            }
            Log.Info("CustomAgentStore: Helfer gespeichert", new { def.Name, triggers = def.Triggers?.Count ?? 0 });
            return true;
        }

        /// <summary>Entfernt eine Definition nach Namen und speichert. false = nicht gefunden.</summary>
        public bool Remove(string name)
        {
            if (string.IsNullOrWhiteSpace(name)) return false;
            lock (_lock)
            {
                int removed = _defs.RemoveAll(d => string.Equals(d.Name, name, StringComparison.OrdinalIgnoreCase));
                if (removed == 0) return false;
                Save();
            }
            Log.Info("CustomAgentStore: Helfer entfernt", new { name });
            return true;
        }

        private void Load()
        {
            try
            {
                if (!File.Exists(_path)) return;
                var json = File.ReadAllText(_path);
                if (string.IsNullOrWhiteSpace(json)) return;
                var loaded = JsonSerializer.Deserialize<List<SubAgentDefinition>>(json, Json);
                if (loaded != null)
                {
                    _defs.Clear();
                    _defs.AddRange(loaded.Where(d => !string.IsNullOrWhiteSpace(d.Name)));
                }
                Log.Info("CustomAgentStore: Helfer geladen", new { count = _defs.Count });
            }
            catch (Exception ex)
            {
                // Defekte Datei darf den Start nicht verhindern (Graceful Degradation).
                Log.Error("CustomAgentStore: Laden fehlgeschlagen — starte ohne benutzerdefinierte Helfer", ex);
            }
        }

        private void Save()
        {
            try
            {
                var json = JsonSerializer.Serialize(_defs, Json);
                var dir = Path.GetDirectoryName(_path)!;
                var tmp = Path.Combine(dir, $".custom-agents-{Guid.NewGuid():N}.tmp");
                File.WriteAllText(tmp, json);
                File.Move(tmp, _path, overwrite: true);   // atomar ersetzen
            }
            catch (Exception ex)
            {
                Log.Error("CustomAgentStore: Speichern fehlgeschlagen", ex);
            }
        }
    }
}
