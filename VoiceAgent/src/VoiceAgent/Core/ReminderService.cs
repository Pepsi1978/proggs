using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.Json;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Speichert geplante Erinnerungen persistent (JSON) und liefert die faelligen zurueck.
    /// Ueberlebt App-Neustarts. Die zeitgesteuerte Pruefung macht das MainWindow (Timer).
    /// </summary>
    public sealed class ReminderService
    {
        private readonly string _path;
        private List<Reminder> _items = new();

        public ReminderService(string? path = null)
        {
            _path = path ?? Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "VoiceAgent", "reminders.json");
            Load();
        }

        public IReadOnlyList<Reminder> Items => _items;

        public void Add(Reminder r)
        {
            _items.Add(r);
            Log.Info("Reminder angelegt", new { r.Text, due = r.DueUtc.ToString("u") });
            Save();
        }

        /// <summary>Faellige, noch nicht ausgeloeste ZEITgesteuerte Erinnerungen — markiert sie als erledigt und speichert.</summary>
        public List<Reminder> TakeDue(DateTimeOffset now)
        {
            // OnNextStart-Erinnerungen sind NICHT zeitgesteuert -> hier ausschliessen (kommen beim App-Start).
            var due = _items.Where(r => !r.Done && !r.OnNextStart && r.DueUtc <= now).ToList();
            if (due.Count > 0)
            {
                foreach (var r in due) r.Done = true;
                Save();
            }
            return due;
        }

        /// <summary>
        /// Start-Erinnerungen (OnNextStart): beim vollstaendigen App-Start einmalig faellig.
        /// Markiert sie als erledigt und speichert (kommen also nur EINMAL beim naechsten Start).
        /// </summary>
        public List<Reminder> TakeStartReminders()
        {
            var due = _items.Where(r => !r.Done && r.OnNextStart).ToList();
            if (due.Count > 0)
            {
                foreach (var r in due) r.Done = true;
                Save();
            }
            return due;
        }

        private void Load()
        {
            try
            {
                if (File.Exists(_path))
                    _items = JsonSerializer.Deserialize<List<Reminder>>(File.ReadAllText(_path)) ?? new List<Reminder>();
            }
            catch (Exception ex)
            {
                Log.Error("Reminders laden fehlgeschlagen — starte leer", ex);
                _items = new List<Reminder>();
            }
        }

        private void Save()
        {
            try
            {
                var dir = Path.GetDirectoryName(_path);
                if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);
                File.WriteAllText(_path,
                    JsonSerializer.Serialize(_items, new JsonSerializerOptions { WriteIndented = true }),
                    Encoding.UTF8);
            }
            catch (Exception ex)
            {
                Log.Error("Reminders speichern fehlgeschlagen", ex);
            }
        }
    }
}
