using System;
using System.IO;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Wie woertlich Gemini transkribiert — Verbatim oder Smart. Liegt wie der
    /// Modell-Schalter als winzige Datei im geteilten SK-Ordner, wirkt damit
    /// sofort, braucht keinen Neustart und gilt fuer alle vier Overlays
    /// (TVO/CVO, Windows/macOS) gemeinsam.
    ///
    /// Inhalt: "verbatim" oder "smart". Fehlende/kaputte Datei = verbatim.
    ///
    /// UNTERSCHIED (an derselben 64,5-s-Aufnahme mehrfach gemessen):
    ///   Tempo    — praktisch gleich, Median 4,6 s (smart) gegen 6,1 s
    ///              (verbatim); die Schwankung durch Serverlast ist groesser
    ///              als der Unterschied.
    ///   Text     — smart setzt Absaetze und wirft Fuellwoerter raus,
    ///              formuliert dabei aber um und LAESST WOERTER WEG: in jedem
    ///              einzelnen Durchgang wurde aus "Ich frage mich" ein
    ///              "Frage mich" (418 gegen 415 Zeichen, 66 gegen 65 Woerter).
    /// Deshalb ist verbatim die Voreinstellung — bei einer Zahl, einem
    /// Dateinamen oder einem "nicht" waere ein verschlucktes Wort teuer.
    /// </summary>
    public static class TranscriptionModeSetting
    {
        public const string Verbatim = "verbatim";
        public const string Smart = "smart";

        private static string Path_ => System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            "SK", "VoiceOverlays", "transcription-mode.txt");

        // mtime-Cache: die Datei wird pro Aufnahme gelesen, ein Datei-Zugriff
        // je Voice-Turn ist unnoetig.
        private static string _cached = Verbatim;
        private static DateTime _cachedStamp = DateTime.MinValue;
        private static readonly object _lock = new();

        public static string Current
        {
            get
            {
                try
                {
                    var path = Path_;
                    if (!File.Exists(path)) return Verbatim;
                    var stamp = File.GetLastWriteTimeUtc(path);
                    lock (_lock)
                    {
                        if (stamp != _cachedStamp)
                        {
                            var raw = File.ReadAllText(path).Trim().ToLowerInvariant();
                            _cached = raw == Smart ? Smart : Verbatim;
                            _cachedStamp = stamp;
                        }
                        return _cached;
                    }
                }
                catch { return Verbatim; }
            }
        }

        public static bool UseSmart => Current == Smart;

        public static void Save(string mode)
        {
            try
            {
                var path = Path_;
                Directory.CreateDirectory(System.IO.Path.GetDirectoryName(path)!);
                File.WriteAllText(path, (mode == Smart ? Smart : Verbatim) + "\n");
                lock (_lock) { _cachedStamp = DateTime.MinValue; }
            }
            catch { /* best-effort: der Schalter darf das Speichern nicht blockieren */ }
        }
    }
}

