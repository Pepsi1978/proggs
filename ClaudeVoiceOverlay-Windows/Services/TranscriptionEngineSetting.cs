using System;
using System.IO;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Welches Sprache-zu-Text-Modell benutzt wird. Die Auswahl lebt — wie der
    /// Woerterbuch-Schalter — als winzige Datei im geteilten SK-Ordner, damit
    /// sie sofort wirkt, keinen Neustart braucht und sich TVO/CVO auf einem
    /// Rechner teilen.
    ///
    /// Inhalt: "groq", "gemini" oder "gemini-live". Fehlende/kaputte Datei = groq.
    /// "gemini" ist bewusst der schnellere, genauere Batch-Weg
    /// (gemini-3.5-transcribe): wer die Auswahl frueher auf Gemini gestellt
    /// hatte, bekommt damit automatisch den besseren Weg statt der
    /// Streaming-Variante.
    /// </summary>
    public static class TranscriptionEngineSetting
    {
        public const string Groq = "groq";
        /// <summary>gemini-3.5-transcribe — fertige Aufnahme, ein HTTPS-Aufruf.</summary>
        public const string Gemini = "gemini";
        /// <summary>gemini-3.5-transcribe-live — Streaming ueber WebSocket.</summary>
        public const string GeminiLive = "gemini-live";

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
                            _cached = raw switch
                            {
                                Gemini => Gemini,
                                GeminiLive => GeminiLive,
                                _ => Groq,
                            };
                            _cachedStamp = stamp;
                        }
                        return _cached;
                    }
                }
                catch { return Groq; }
            }
        }

        public static bool UseGemini => Current == Gemini;
        public static bool UseGeminiLive => Current == GeminiLive;
        /// <summary>Irgendeine der beiden Gemini-Varianten.</summary>
        public static bool UseAnyGemini => Current is Gemini or GeminiLive;

        public static void Save(string engine)
        {
            try
            {
                var path = Path_;
                Directory.CreateDirectory(System.IO.Path.GetDirectoryName(path)!);
                var value = engine switch
                {
                    Gemini => Gemini,
                    GeminiLive => GeminiLive,
                    _ => Groq,
                };
                File.WriteAllText(path, value + "\n");
                lock (_lock) { _cachedStamp = DateTime.MinValue; }
            }
            catch { /* best-effort: der Schalter darf das Speichern nicht blockieren */ }
        }
    }
}
