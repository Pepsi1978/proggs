using System;
using System.IO;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Welches Whisper-Modell bei Groq benutzt wird. Lebt wie die Engine-Auswahl
    /// als winzige Datei im geteilten SK-Ordner, wirkt sofort ohne Neustart.
    ///
    /// Inhalt: "whisper-large-v3-turbo" oder "whisper-large-v3".
    /// Fehlende/kaputte Datei = null, dann gilt WHISPER_MODEL aus der .env.
    /// large-v3 ist auf Deutsch belegt genauer (HF Open ASR, Common Voice de
    /// 4,79 % statt 8,58 %), muss aber in der Groq-Konsole fuer die
    /// Organisation freigeschaltet sein — sonst 403 model_permission_blocked_org.
    /// </summary>
    public static class GroqModelSetting
    {
        public const string Turbo = "whisper-large-v3-turbo";
        public const string LargeV3 = "whisper-large-v3";

        private static string Path_ => System.IO.Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
            "SK", "VoiceOverlays", "groq-model.txt");

        private static string? _cached;
        private static DateTime _cachedStamp = DateTime.MinValue;
        private static readonly object _lock = new();
        // Nach einem 403 "blocked" 30 Min. direkt Turbo nehmen statt jedes Diktat doppelt hochzuladen.
        private static DateTime _blockedUntilUtc = DateTime.MinValue;

        public static string? Current
        {
            get
            {
                try
                {
                    var path = Path_;
                    if (!File.Exists(path)) return null;
                    var stamp = File.GetLastWriteTimeUtc(path);
                    lock (_lock)
                    {
                        if (stamp != _cachedStamp)
                        {
                            var raw = File.ReadAllText(path).Trim().ToLowerInvariant();
                            _cached = raw switch
                            {
                                LargeV3 => LargeV3,
                                Turbo => Turbo,
                                _ => null,
                            };
                            _cachedStamp = stamp;
                        }
                        return _cached;
                    }
                }
                // Lesefehler (z. B. das andere Overlay schreibt gerade): letzte bekannte Wahl behalten.
                catch { return _cached; }
            }
        }

        public static void Save(string model)
        {
            try
            {
                var path = Path_;
                Directory.CreateDirectory(System.IO.Path.GetDirectoryName(path)!);
                File.WriteAllText(path, (model == LargeV3 ? LargeV3 : Turbo) + "\n");
                lock (_lock) { _cachedStamp = DateTime.MinValue; }
                _blockedUntilUtc = DateTime.MinValue;
            }
            catch { /* best-effort: der Schalter darf das Speichern nicht blockieren */ }
        }

        /// <summary>Gewaehltes Modell fuer diese Aufnahme; large-v3 wird nach einer Sperre voruebergehend durch Turbo ersetzt.</summary>
        public static string Resolve(string fallback)
        {
            var model = Current ?? fallback;
            return model == LargeV3 && DateTime.UtcNow < _blockedUntilUtc ? Turbo : model;
        }

        public static void MarkBlocked() => _blockedUntilUtc = DateTime.UtcNow.AddMinutes(30);

        /// <summary>Groq-Antwort "Modell fuer die Organisation gesperrt".</summary>
        public static bool IsBlockedError(Exception ex) =>
            ex.Message.Contains("model_permission_blocked_org", StringComparison.Ordinal)
            || ex.Message.Contains("blocked at the organization level", StringComparison.Ordinal);
    }
}
