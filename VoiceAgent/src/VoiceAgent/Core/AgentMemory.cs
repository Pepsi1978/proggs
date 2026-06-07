using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>Eine gespeicherte Gespraechsrunde (Teil des Langzeit-Gedaechtnisses).</summary>
    public sealed class MemoryTurn
    {
        public string User { get; set; } = "";
        public string Assistant { get; set; } = "";
    }

    /// <summary>Serialisierbarer Gedaechtnis-Inhalt (JSON-Persistenz).</summary>
    public sealed class MemoryStore
    {
        public List<string> Facts { get; set; } = new();
        public List<MemoryTurn> Recent { get; set; } = new();
    }

    /// <summary>
    /// Persistentes Langzeit-Gedaechtnis des Hauptagenten — STUFE 1.
    ///
    /// Speichert dauerhafte Fakten + die letzten Gespraechsrunden als JSON unter
    /// %LOCALAPPDATA%\VoiceAgent\memory\memory.json und faedelt beides beim Start wieder in
    /// den System-Prompt ein, sodass der Agent ueber Sessions hinweg Kontext behaelt
    /// ("er vergisst nicht mehr alles vom letzten Mal").
    ///
    /// EHRLICH: Dies ist die ERSTE Stufe. Es wird alles vollstaendig geladen (gekappt auf die
    /// letzten N Runden / M Fakten). Ein semantisches Vektorgedaechtnis mit Retrieval (wie bei
    /// Hermes, "vergisst gar nichts") ist die naechste Ausbaustufe.
    /// </summary>
    public sealed class AgentMemory
    {
        public const int MaxRecentTurns = 12;   // letzte Runden, die als Kontext mitgegeben werden
        public const int MaxFacts = 100;

        private readonly string _path;
        private MemoryStore _store = new();

        /// <summary>path = null -> Standardpfad. Eigener Pfad ist fuer Tests gedacht.</summary>
        public AgentMemory(string? path = null)
        {
            _path = path ?? Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "VoiceAgent", "memory", "memory.json");
            Load();
        }

        public IReadOnlyList<string> Facts => _store.Facts;
        public IReadOnlyList<MemoryTurn> Recent => _store.Recent;

        private void Load()
        {
            try
            {
                if (File.Exists(_path))
                {
                    _store = JsonSerializer.Deserialize<MemoryStore>(File.ReadAllText(_path)) ?? new MemoryStore();
                    Log.Info("Gedaechtnis geladen", new { facts = _store.Facts.Count, recent = _store.Recent.Count });
                }
                else
                {
                    Log.Info("Gedaechtnis: keine Datei — starte mit leerem Gedaechtnis", new { path = _path });
                }
            }
            catch (Exception ex)
            {
                Log.Error("Gedaechtnis laden fehlgeschlagen — starte leer", ex);
                _store = new MemoryStore();
            }
        }

        private void Save()
        {
            try
            {
                var dir = Path.GetDirectoryName(_path);
                if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);
                File.WriteAllText(_path,
                    JsonSerializer.Serialize(_store, new JsonSerializerOptions { WriteIndented = true }),
                    Encoding.UTF8);
            }
            catch (Exception ex)
            {
                Log.Error("Gedaechtnis speichern fehlgeschlagen", ex);
            }
        }

        /// <summary>Merkt sich einen dauerhaften Fakt (z.B. eine Vorliebe oder ein Detail ueber Frank).</summary>
        public void Remember(string fact)
        {
            if (string.IsNullOrWhiteSpace(fact)) return;
            _store.Facts.Add(fact.Trim());
            if (_store.Facts.Count > MaxFacts)
                _store.Facts.RemoveRange(0, _store.Facts.Count - MaxFacts);
            Probe.That(_store.Facts.Count <= MaxFacts, "AgentMemory: Fakten-Limit ueberschritten", new { count = _store.Facts.Count });
            Log.Info("Gedaechtnis: Fakt gemerkt", new { fact = _store.Facts[^1] });
            Save();
        }

        /// <summary>Haengt eine abgeschlossene Gespraechsrunde an und kappt auf die letzten N.</summary>
        public void RecordTurn(string user, string assistant)
        {
            if (string.IsNullOrWhiteSpace(user) && string.IsNullOrWhiteSpace(assistant)) return;
            _store.Recent.Add(new MemoryTurn { User = user ?? "", Assistant = assistant ?? "" });
            if (_store.Recent.Count > MaxRecentTurns)
                _store.Recent.RemoveRange(0, _store.Recent.Count - MaxRecentTurns);
            Save();
        }

        /// <summary>Der Gedaechtnis-Block fuer den System-Prompt dieser Instanz.</summary>
        public string ContextBlock() => BuildContextBlock(_store);

        /// <summary>
        /// Baut den Gedaechtnis-Block fuer den System-Prompt. Public static (reine Funktion) =
        /// isoliert testbar. Leer, wenn nichts gespeichert ist.
        /// </summary>
        public static string BuildContextBlock(MemoryStore store)
        {
            if (store == null || (store.Facts.Count == 0 && store.Recent.Count == 0)) return string.Empty;

            var sb = new StringBuilder();
            sb.AppendLine("\n\nWAS DU AUS FRUEHEREN GESPRAECHEN WEISST (dein Gedaechtnis):");
            if (store.Facts.Count > 0)
            {
                sb.AppendLine("Gemerkte Fakten:");
                foreach (var f in store.Facts) sb.Append("- ").AppendLine(f);
            }
            if (store.Recent.Count > 0)
            {
                sb.AppendLine("Die letzten Gespraechsrunden:");
                foreach (var t in store.Recent)
                {
                    if (!string.IsNullOrWhiteSpace(t.User)) sb.Append("  Frank: ").AppendLine(Cap(t.User));
                    if (!string.IsNullOrWhiteSpace(t.Assistant)) sb.Append("  Du: ").AppendLine(Cap(t.Assistant));
                }
            }
            return sb.ToString();
        }

        private static string Cap(string s) => s.Length <= 200 ? s : s.Substring(0, 200) + "…";
    }
}
