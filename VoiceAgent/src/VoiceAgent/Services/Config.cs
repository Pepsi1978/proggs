using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Laden/Speichern der Einstellungen (JSON) und Lesen/Schreiben der API-Schluessel.
    /// Schluessel liegen bewusst getrennt im SK-Ordner (~/SK/VoiceAgent/keys.json),
    /// NIE im Repo und NIE in settings.json.
    /// </summary>
    public static class Config
    {
        private static readonly JsonSerializerOptions JsonOpts = new()
        {
            WriteIndented = true,
            PropertyNameCaseInsensitive = true
        };

        public static string SettingsPath => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "VoiceAgent", "settings.json");

        /// <summary>Zentraler Secrets-Ordner — niemals im Repo.</summary>
        public static string SecretsDir => Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            "SK", "VoiceAgent");

        private static string KeysPath => Path.Combine(SecretsDir, "keys.json");

        // ---------- Einstellungen ----------

        public static void SaveTo(string path, AppSettings settings)
        {
            var dir = Path.GetDirectoryName(path);
            if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);
            var json = JsonSerializer.Serialize(settings, JsonOpts);
            File.WriteAllText(path, json, Encoding.UTF8);
            Log.Info($"Einstellungen gespeichert: {path}");
        }

        public static AppSettings LoadFrom(string path)
        {
            try
            {
                if (!File.Exists(path))
                {
                    Log.Info($"Keine Einstellungsdatei ({path}) — nutze Defaults");
                    return new AppSettings();
                }
                var json = File.ReadAllText(path);
                var loaded = JsonSerializer.Deserialize<AppSettings>(json, JsonOpts) ?? new AppSettings();
                Log.Info($"Einstellungen geladen: {path}");
                return loaded;
            }
            catch (Exception ex)
            {
                Log.Error($"Einstellungen konnten nicht geladen werden ({path}) — nutze Defaults", ex);
                return new AppSettings();
            }
        }

        public static void Save(AppSettings settings) => SaveTo(SettingsPath, settings);
        public static AppSettings Load() => LoadFrom(SettingsPath);

        // ---------- API-Schluessel (SK-Ordner) ----------

        /// <summary>Liest einen API-Schluessel: erst keys.json im SK-Ordner, dann Umgebungsvariable.</summary>
        public static string ReadApiKey(string provider)
        {
            try
            {
                if (File.Exists(KeysPath))
                {
                    using var doc = JsonDocument.Parse(File.ReadAllText(KeysPath));
                    if (doc.RootElement.TryGetProperty(provider, out var v))
                    {
                        var key = v.GetString();
                        if (!string.IsNullOrWhiteSpace(key)) return key!;
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Error($"API-Schluessel lesen fehlgeschlagen ({provider})", ex);
            }

            var env = Environment.GetEnvironmentVariable($"VOICEAGENT_{provider.ToUpperInvariant()}_API_KEY");
            if (!string.IsNullOrWhiteSpace(env)) return env!;
            return string.Empty;
        }

        /// <summary>Schreibt/aktualisiert einen API-Schluessel in keys.json (SK-Ordner).</summary>
        public static void SaveApiKey(string provider, string key)
        {
            try
            {
                Directory.CreateDirectory(SecretsDir);
                Dictionary<string, string> keys = new();
                if (File.Exists(KeysPath))
                {
                    keys = JsonSerializer.Deserialize<Dictionary<string, string>>(File.ReadAllText(KeysPath))
                           ?? new Dictionary<string, string>();
                }
                keys[provider] = key ?? string.Empty;
                File.WriteAllText(KeysPath, JsonSerializer.Serialize(keys, JsonOpts), Encoding.UTF8);
                Log.Info($"API-Schluessel gespeichert ({provider}) in {KeysPath}");
            }
            catch (Exception ex)
            {
                Log.Error($"API-Schluessel speichern fehlgeschlagen ({provider})", ex);
            }
        }
    }
}
