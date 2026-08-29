using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Bewahrt die zuletzt aufgenommenen Diktate auf, statt sie nach dem Turn zu loeschen.
    ///
    /// Vorfall 29.08.2026: Ein 15,4-Minuten-Diktat ergab eine 29,5-MB-WAV, die Groq mit HTTP 413
    /// abwies; die Datei wurde danach im <c>finally</c> geloescht — der gesprochene Text war
    /// endgueltig weg und musste komplett neu gesprochen werden. Seitdem gilt: <b>eine Aufnahme
    /// wird nie mehr direkt geloescht</b>. Jede Aufnahme wandert hierher, die letzten
    /// <see cref="KeepCount"/> bleiben erhalten und koennen ueber Rechtsklick auf das Mikrofon
    /// erneut transkribiert werden. Erst die uebernaechste Aufnahme raeumt die aelteste ab.
    ///
    /// Bewusst klein gehalten: reines Dateisystem, kein Zustand im Speicher, damit das Archiv auch
    /// nach einem Absturz der App vollstaendig ist.
    /// </summary>
    public static class RecordingArchive
    {
        /// <summary>So viele Aufnahmen bleiben erhalten (die letzte und die vorletzte).</summary>
        public const int KeepCount = 2;

        private const string FilePrefix = "aufnahme_";

        /// <summary>%LOCALAPPDATA%\TerminalVoiceOverlay\aufnahmen — Ablage der letzten Diktate.</summary>
        public static string Directory
        {
            get
            {
                string dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "TerminalVoiceOverlay", "aufnahmen");
                System.IO.Directory.CreateDirectory(dir);
                return dir;
            }
        }

        /// <summary>
        /// Verschiebt eine frisch aufgenommene WAV ins Archiv und raeumt alles ausser den letzten
        /// <see cref="KeepCount"/> Aufnahmen ab. Gibt den neuen Pfad zurueck (null, wenn nichts zu
        /// archivieren war). Wirft nie — das Archiv darf einen Turn niemals zum Scheitern bringen.
        /// </summary>
        /// <param name="wavFile">Pfad der Aufnahme im Temp-Ordner.</param>
        /// <param name="success">Ob der Turn erfolgreich war (steht nur im Dateinamen).</param>
        public static string? Archive(string? wavFile, bool success)
        {
            try
            {
                if (string.IsNullOrEmpty(wavFile) || !File.Exists(wavFile)) return null;

                string dir = Directory;
                string stamp = DateTime.Now.ToString("yyyy-MM-dd_HH-mm-ss");
                string status = success ? "ok" : "fehler";
                string target = Path.Combine(dir, $"{FilePrefix}{stamp}_{status}.wav");

                // Zwei Aufnahmen in derselben Sekunde: Zaehler anhaengen statt ueberschreiben.
                for (int i = 2; File.Exists(target) && i < 100; i++)
                    target = Path.Combine(dir, $"{FilePrefix}{stamp}_{status}_{i}.wav");

                File.Move(wavFile, target, overwrite: false);
                long bytes = new FileInfo(target).Length;
                DiagLog.Write("Recording", "archived",
                    ("path", target), ("bytes", bytes), ("success", success));

                Prune(dir);
                return target;
            }
            catch (Exception ex)
            {
                DiagLog.Warn("Recording", "archive_failed",
                    ("err", ex.Message), ("type", ex.GetType().Name));
                return null;
            }
        }

        /// <summary>
        /// Die aufbewahrten Aufnahmen, neueste zuerst (hoechstens <see cref="KeepCount"/>).
        /// </summary>
        public static IReadOnlyList<string> Recent()
        {
            try
            {
                return System.IO.Directory
                    .EnumerateFiles(Directory, FilePrefix + "*.wav")
                    .OrderByDescending(File.GetLastWriteTimeUtc)
                    .Take(KeepCount)
                    .ToList();
            }
            catch (Exception ex)
            {
                DiagLog.Warn("Recording", "archive_list_failed",
                    ("err", ex.Message), ("type", ex.GetType().Name));
                return Array.Empty<string>();
            }
        }

        /// <summary>
        /// Die n-letzte Aufnahme (0 = die letzte, 1 = die vorletzte) oder null, wenn es sie nicht gibt.
        /// </summary>
        public static string? At(int index)
        {
            var all = Recent();
            return index >= 0 && index < all.Count ? all[index] : null;
        }

        /// <summary>Loescht alles ausser den letzten <see cref="KeepCount"/> Aufnahmen.</summary>
        private static void Prune(string dir)
        {
            try
            {
                var stale = System.IO.Directory
                    .EnumerateFiles(dir, FilePrefix + "*.wav")
                    .OrderByDescending(File.GetLastWriteTimeUtc)
                    .Skip(KeepCount)
                    .ToList();

                foreach (var old in stale)
                {
                    try
                    {
                        File.Delete(old);
                        DiagLog.Write("Recording", "archive_pruned", ("path", old));
                    }
                    catch { /* gesperrt -> die naechste Aufnahme raeumt sie ab */ }
                }
            }
            catch (Exception ex)
            {
                DiagLog.Warn("Recording", "archive_prune_failed",
                    ("err", ex.Message), ("type", ex.GetType().Name));
            }
        }
    }
}
