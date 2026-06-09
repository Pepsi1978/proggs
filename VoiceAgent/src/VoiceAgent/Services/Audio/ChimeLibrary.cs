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
    /// Listet die verfuegbaren Start-/Stop-Toene. Scannt assets/sounds/start bzw. assets/sounds/stop
    /// neben der EXE (Single-File-sicher via Environment.ProcessPath, Almanach dotnet §1.1/§31) und
    /// stellt den bisherigen Standard-Ton voran. Crasht nie — liefert im Fehlerfall mindestens den Standard.
    /// </summary>
    public static class ChimeLibrary
    {
        /// <summary>Start-Toene (ich-hoere-zu): Standard (wakeword.wav) + Inhalt von assets/sounds/start.</summary>
        public static IReadOnlyList<ChimeSound> StartSounds()
            => Build("sounds/start", new ChimeSound("Standard – Okay-Computer-Ton", "wakeword.wav"));

        /// <summary>Stop-Toene (ich-hoere-nicht-mehr-zu): Standard (sleep.wav) + Inhalt von assets/sounds/stop.</summary>
        public static IReadOnlyList<ChimeSound> StopSounds()
            => Build("sounds/stop", new ChimeSound("Standard – Einschlafton", "sleep.wav"));

        /// <summary>Melde-Toene (proaktiver Funkspruch/Erinnerung): Standard (message1.wav) + assets/sounds/message.</summary>
        public static IReadOnlyList<ChimeSound> MessageSounds()
            => Build("sounds/message", new ChimeSound("Standard – Funkspruch-Ton", "message1.wav"));

        private static string AssetsDir
        {
            get
            {
                var dir = Path.GetDirectoryName(Environment.ProcessPath) ?? AppContext.BaseDirectory;
                return Path.Combine(dir, "assets");
            }
        }

        private static IReadOnlyList<ChimeSound> Build(string subDir, ChimeSound legacyDefault)
        {
            var list = new List<ChimeSound> { legacyDefault };
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
                        list.Add(new ChimeSound(Nicify(Path.GetFileNameWithoutExtension(f), n), rel));
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Warn("ChimeLibrary: Sound-Ordner scannen fehlgeschlagen", new { subDir, ex.Message });
            }
            return list;
        }

        // "07_analog_bass_sweeps" -> "Ton 7 – Analog bass sweeps"; kryptische Namen -> "Ton N".
        private static string Nicify(string fileBase, int n)
        {
            var name = fileBase;
            int us = name.IndexOf('_');
            if (us is >= 1 and <= 3 && int.TryParse(name.AsSpan(0, us), out _)) name = name[(us + 1)..];
            name = name.Replace('_', ' ').Replace('-', ' ').Trim();
            int letters = name.Count(char.IsLetter);
            if (letters < 3) return $"Ton {n}";                         // kein sinnvoller Klartext-Name
            name = char.ToUpperInvariant(name[0]) + name[1..];
            return $"Ton {n} – {name}";
        }
    }
}
