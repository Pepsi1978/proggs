using System;
using System.IO;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Welches Sprache-zu-Text-Modell benutzt wird: Groq Whisper (Standard) oder
    /// Gemini Transcribe. Die Auswahl lebt — wie der Woerterbuch-Schalter — als
    /// winzige Datei im geteilten SK-Ordner, damit sie sofort wirkt, keinen
    /// Neustart braucht und sich TVO/CVO auf einem Rechner teilen.
    /// Inhalt: "groq" oder "gemini". Fehlende/kaputte Datei = groq.
    /// </summary>
    public static class TranscriptionEngineSetting
    {
        public const string Groq = "groq";
        public const string Gemini = "gemini";

        private static string Path_ => System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            "SK", "VoiceOverlays", "transcription-engine.txt");

        // mtime-Cache wie bei den Gemini-Prompts: die Datei wird pro Aufnahme
        // gelesen, ein Datei-Zugriff je Voice-Turn ist unnoetig.
        private static string _cached = Groq;
        private static DateTime _cachedStamp = DateTime.MinValue;
        private static readonly object _lock = new();

        public static string Current
        {
            get
            {
                try
                {
                    var path = Path_;
                    if (!File.Exists(path)) return Groq;
                    var stamp = File.GetLastWriteTimeUtc(path);
                    lock (_lock)
                    {
                        if (stamp != _cachedStamp)
                        {
                            var raw = File.ReadAllText(path).Trim().ToLowerInvariant();
                            _cached = raw == Gemini ? Gemini : Groq;
                            _cachedStamp = stamp;
                        }
                        return _cached;
                    }
                }
                catch { return Groq; }
            }
        }

        public static bool UseGemini => Current == Gemini;

        public static void Save(string engine)
        {
            try
            {
                var path = Path_;
                Directory.CreateDirectory(System.IO.Path.GetDirectoryName(path)!);
                File.WriteAllText(path, (engine == Gemini ? Gemini : Groq) + "\n");
                lock (_lock) { _cachedStamp = DateTime.MinValue; }
            }
            catch { /* best-effort: der Schalter darf das Speichern nicht blockieren */ }
        }
    }
}
