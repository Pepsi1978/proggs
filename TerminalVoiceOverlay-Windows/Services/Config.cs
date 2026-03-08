using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;

namespace TerminalVoiceOverlay.Services
{
    public sealed class Config
    {
        // Groq
        public string GroqApiKey { get; }
        public string WhisperModel { get; }
        public string WhisperLang { get; }
        public string WhisperUrl { get; }

        // Gemini
        public string? GeminiApiKey { get; }
        public string GeminiModel { get; }
        public string GeminiThinkingLevel { get; }
        public bool GeminiAvailable => !string.IsNullOrEmpty(GeminiApiKey);

        // Audio
        public int AudioSampleRate { get; }
        public int AudioChannels { get; }

        // Terminal-Erkennung
        public string[] TerminalProcessNames { get; }

        private Config(Dictionary<string, string> env)
        {
            // Groq (erforderlich)
            GroqApiKey = GetRequired(env, "GROQ_API_KEY");
            WhisperModel = Get(env, "WHISPER_MODEL", "whisper-large-v3");
            WhisperLang = Get(env, "WHISPER_LANG", "de");
            WhisperUrl = Get(env, "WHISPER_URL", "https://api.groq.com/openai/v1/audio/transcriptions");

            // Gemini (optional)
            env.TryGetValue("GEMINI_API_KEY", out var geminiKey);
            GeminiApiKey = string.IsNullOrEmpty(geminiKey) ? null : geminiKey;
            GeminiModel = Get(env, "GEMINI_MODEL", "gemini-3.1-flash-lite-preview");
            GeminiThinkingLevel = Get(env, "GEMINI_THINKING_LEVEL", "MEDIUM");

            // Audio
            AudioSampleRate = GetInt(env, "AUDIO_SAMPLE_RATE", 16000);
            AudioChannels = GetInt(env, "AUDIO_CHANNELS", 1);

            // Terminal-Erkennung
            var processNames = Get(env, "TERMINAL_PROCESS_NAMES", "WindowsTerminal,pwsh,powershell");
            TerminalProcessNames = processNames.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        }

        public static Config Load()
        {
            var env = ParseEnvFile();
            return new Config(env);
        }

        private static string GetRequired(Dictionary<string, string> env, string key)
        {
            if (!env.TryGetValue(key, out var value) || string.IsNullOrEmpty(value))
            {
                throw new InvalidOperationException(
                    $"{key} nicht gefunden. Bitte .env Datei anlegen.\n" +
                    "Suchpfade:\n" +
                    "  - Neben der .exe\n" +
                    "  - %USERPROFILE%\\.env\n" +
                    "  - %APPDATA%\\TerminalVoiceOverlay\\.env");
            }
            return value;
        }

        private static string Get(Dictionary<string, string> env, string key, string defaultValue)
        {
            return env.TryGetValue(key, out var value) && !string.IsNullOrEmpty(value) ? value : defaultValue;
        }

        private static int GetInt(Dictionary<string, string> env, string key, int defaultValue)
        {
            return env.TryGetValue(key, out var value) && int.TryParse(value, out var result) ? result : defaultValue;
        }

        private static Dictionary<string, string> ParseEnvFile()
        {
            var exeDir = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location) ?? ".";
            var userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            var appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);

            string[] searchPaths =
            {
                Path.Combine(exeDir, ".env"),
                Path.Combine(userProfile, ".env"),
                Path.Combine(appData, "TerminalVoiceOverlay", ".env"),
            };

            foreach (var path in searchPaths)
            {
                if (File.Exists(path))
                {
                    System.Diagnostics.Debug.WriteLine($"Config: .env geladen von {path}");
                    return ParseEnvContents(File.ReadAllText(path));
                }
            }

            return new Dictionary<string, string>();
        }

        private static Dictionary<string, string> ParseEnvContents(string contents)
        {
            var result = new Dictionary<string, string>();
            foreach (var rawLine in contents.Split('\n'))
            {
                var line = rawLine.Trim();
                if (string.IsNullOrEmpty(line) || line.StartsWith('#'))
                    continue;

                var eqIndex = line.IndexOf('=');
                if (eqIndex <= 0) continue;

                var key = line.Substring(0, eqIndex).Trim();
                var value = line.Substring(eqIndex + 1).Trim();

                // Remove surrounding quotes
                if (value.Length >= 2 &&
                    ((value.StartsWith('"') && value.EndsWith('"')) ||
                     (value.StartsWith('\'') && value.EndsWith('\''))))
                {
                    value = value.Substring(1, value.Length - 2);
                }

                result[key] = value;
            }
            return result;
        }
    }
}
