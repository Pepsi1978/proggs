using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;

namespace TerminalVoiceOverlay.Services
{
    public sealed class Config
    {
        public string GroqApiKey { get; }
        public string? GeminiApiKey { get; }
        public bool GeminiAvailable => !string.IsNullOrEmpty(GeminiApiKey);

        private Config(string groqApiKey, string? geminiApiKey)
        {
            GroqApiKey = groqApiKey;
            GeminiApiKey = geminiApiKey;
        }

        public static Config Load()
        {
            var env = ParseEnvFile();

            if (!env.TryGetValue("GROQ_API_KEY", out var groqKey) || string.IsNullOrEmpty(groqKey))
            {
                throw new InvalidOperationException(
                    "GROQ_API_KEY nicht gefunden. Bitte .env Datei anlegen.\n" +
                    "Suchpfade:\n" +
                    "  - Neben der .exe\n" +
                    "  - %USERPROFILE%\\.env\n" +
                    "  - %APPDATA%\\TerminalVoiceOverlay\\.env");
            }

            env.TryGetValue("GEMINI_API_KEY", out var geminiKey);
            return new Config(groqKey, geminiKey);
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
