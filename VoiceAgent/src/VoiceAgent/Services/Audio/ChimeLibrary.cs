using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Audio
{
    /// <summary>Ein auswaehlbarer Signalton: Anzeigename + Pfad relativ zu assets/ (fuer Chime.FileName).</summary>
    public sealed record ChimeSound(string DisplayName, string RelativePath);

    /// <summary>
    /// Listet die verfuegbaren Toene fuer die drei Auswahllisten (Start/Stop/Melde). JEDE Liste enthaelt
    /// ihren bisherigen Standard-Ton voran PLUS ALLE heruntergeladenen Toene aus allen drei Ordnern
    /// (assets/sounds/start|stop|message) — so sind alle 90 Toene ueberall verfuegbar (Frank-Wunsch).
    /// Scannt neben der EXE (Single-File-sicher via Environment.ProcessPath, Almanach dotnet §1.1/§31).
    /// Crasht nie — liefert im Fehlerfall mindestens den Standard.
    /// </summary>
    public static class ChimeLibrary
    {
        /// <summary>Start-Toene (ich-hoere-zu): Standard (wakeword.wav) + alle heruntergeladenen Toene.</summary>
        public static IReadOnlyList<ChimeSound> StartSounds()
            => WithDefault(new ChimeSound("Standard – Okay-Computer-Ton", "wakeword.wav"));

        /// <summary>Stop-Toene (ich-hoere-nicht-mehr-zu): Standard (sleep.wav) + alle heruntergeladenen Toene.</summary>
        public static IReadOnlyList<ChimeSound> StopSounds()
            => WithDefault(new ChimeSound("Standard – Einschlafton", "sleep.wav"));

        /// <summary>Melde-Toene (proaktiver Funkspruch/Erinnerung): Standard (message1.wav) + alle heruntergeladenen Toene.</summary>
        public static IReadOnlyList<ChimeSound> MessageSounds()
            => WithDefault(new ChimeSound("Standard – Funkspruch-Ton", "message1.wav"));

        private static IReadOnlyList<ChimeSound> WithDefault(ChimeSound categoryDefault)
        {
            var list = new List<ChimeSound> { categoryDefault };
            list.AddRange(AllSounds());
            return list;
        }

        // Alle Toene aus allen drei Ordnern — damit jeder Picker das komplette Repertoire zeigt.
        private static IEnumerable<ChimeSound> AllSounds()
        {
            foreach (var s in Scan("sounds/start", "Start")) yield return s;
            foreach (var s in Scan("sounds/stop", "Stop")) yield return s;
            foreach (var s in Scan("sounds/message", "Melde")) yield return s;
        }

        private static string AssetsDir
        {
            get
            {
                var dir = Path.GetDirectoryName(Environment.ProcessPath) ?? AppContext.BaseDirectory;
                return Path.Combine(dir, "assets");
            }
        }

        private static List<ChimeSound> Scan(string subDir, string categoryLabel)
        {
            var list = new List<ChimeSound>();
            try
            {
                var full = Path.Combine(AssetsDir, subDir.Replace('/', Path.DirectorySeparatorChar));
                if (Directory.Exists(full))
                {
                    var files = Directory.EnumerateFiles(full)
                        .Where(f => f.EndsWith(".mp3", StringComparison.OrdinalIgnoreCase)
                                 || f.EndsWith(".wav", StringComparison.OrdinalIgnoreCase))
                        .OrderBy(f => f, StringComparer.OrdinalIgnoreCase)
                        .ToList();
                    int n = 0;
                    foreach (var f in files)
                    {
                        n++;
                        var rel = subDir + "/" + Path.GetFileName(f);   // Chime erwartet '/' relativ zu assets/
                        list.Add(new ChimeSound(Nicify(Path.GetFileNameWithoutExtension(f), n, categoryLabel), rel));
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Warn("ChimeLibrary: Sound-Ordner scannen fehlgeschlagen", new { subDir, ex.Message });
            }
            return list;
        }

        // "07_analog_bass_sweeps" + Label "Start" -> "Start 7 – Analog bass sweeps"; kryptisch -> "Start 7".
        private static string Nicify(string fileBase, int n, string categoryLabel)
        {
            var name = fileBase;
            int us = name.IndexOf('_');
            if (us is >= 1 and <= 3 && int.TryParse(name.AsSpan(0, us), out _)) name = name[(us + 1)..];
            name = name.Replace('_', ' ').Replace('-', ' ').Trim();
            int letters = name.Count(char.IsLetter);
            if (letters < 3) return $"{categoryLabel} {n}";                  // kein sinnvoller Klartext-Name
            name = char.ToUpperInvariant(name[0]) + name[1..];
            return $"{categoryLabel} {n} – {name}";
        }
    }
}
