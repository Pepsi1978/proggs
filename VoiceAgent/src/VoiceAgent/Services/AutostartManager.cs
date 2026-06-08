using System;
using Microsoft.Win32;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Verwaltet den automatischen Start des VoiceAgent beim Windows-Anmelden.
    ///
    /// Quelle der Wahrheit ist ausschliesslich der HKCU-Run-Schluessel der Registry
    /// (<c>HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run</c>) — KEIN
    /// zusaetzlicher Eintrag in settings.json. So kann es keinen Drift zwischen einem
    /// gespeicherten JSON-Wert und dem tatsaechlichen Autostart-Zustand geben
    /// (Single Source of Truth). HKCU braucht keine Admin-Rechte.
    ///
    /// Der Run-Eintrag startet die EXE mit dem Schalter <see cref="MinimizedArg"/>, damit die
    /// App beim Windows-Start direkt unsichtbar ins Infobereich-Symbol (Tray) geht und im
    /// Hintergrund laeuft (siehe MainWindow.StartHiddenInTray).
    /// </summary>
    public static class AutostartManager
    {
        private const string RunKeyPath = @"Software\Microsoft\Windows\CurrentVersion\Run";
        private const string ValueName = "VoiceAgent";

        /// <summary>Kommandozeilen-Schalter „beim Start minimiert ins Tray".</summary>
        public const string MinimizedArg = "--minimized";

        /// <summary>Liest, ob der Autostart-Eintrag aktuell in der Registry vorhanden ist.</summary>
        public static bool IsEnabled()
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(RunKeyPath, writable: false);
                var value = key?.GetValue(ValueName) as string;
                return !string.IsNullOrWhiteSpace(value);
            }
            catch (Exception ex)
            {
                // Funktionserhaltend: bei Lesefehler „aus" annehmen, App laeuft weiter (nichts stirbt still).
                Log.Error("Autostart-Status konnte nicht gelesen werden (nehme: aus)", ex);
                return false;
            }
        }

        /// <summary>
        /// Schaltet den Windows-Autostart an oder aus, indem der HKCU-Run-Eintrag gesetzt bzw.
        /// entfernt wird. Defensiv: bei Fehlern bleibt die App lauffaehig, der Fehler wird
        /// geloggt (nichts stirbt still).
        ///
        /// Live-Logik-Sonde (Intent-Verifikation): nach dem Schreiben wird der TATSAECHLICHE
        /// Zustand gegen den gewuenschten geprueft und „erwartet vs. tatsaechlich" in einen
        /// CHECKPOINT geloggt — damit live bestaetigbar ist, dass der Schalter wirklich greift.
        /// </summary>
        public static void SetEnabled(bool enabled)
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(RunKeyPath, writable: true)
                                ?? Registry.CurrentUser.CreateSubKey(RunKeyPath, writable: true);
                if (key == null)
                {
                    Log.Error("Autostart: Run-Schluessel konnte nicht geoeffnet/angelegt werden", null,
                        new { RunKeyPath });
                    return;
                }

                if (enabled)
                {
                    string? exe = Environment.ProcessPath;   // single-file-sicher (Almanach §1.1)
                    if (string.IsNullOrEmpty(exe))
                    {
                        Log.Error("Autostart: EXE-Pfad (Environment.ProcessPath) leer — Eintrag nicht gesetzt", null);
                        return;
                    }
                    // Pfad in Anfuehrungszeichen (Leerzeichen-sicher), gefolgt vom Hintergrund-Schalter.
                    string command = $"\"{exe}\" {MinimizedArg}";
                    key.SetValue(ValueName, command, RegistryValueKind.String);
                    Log.Info("Autostart aktiviert", new { ValueName, command });
                }
                else
                {
                    if (key.GetValue(ValueName) != null)
                        key.DeleteValue(ValueName, throwOnMissingValue: false);
                    Log.Info("Autostart deaktiviert", new { ValueName });
                }

                // Verifikation: hat das Schreiben tatsaechlich den gewuenschten Zustand erzeugt?
                bool actual = IsEnabled();
                Log.Info("CHECKPOINT Autostart gesetzt", new
                {
                    step = "Windows-Autostart setzen",
                    intent = "App startet (oder eben nicht) automatisch beim Windows-Anmelden, minimiert im Tray",
                    expected = enabled,
                    actual,
                    ok = actual == enabled
                });
                if (actual != enabled)
                    Log.Error("Autostart: Zustand nach dem Schreiben weicht ab", null,
                        new { expected = enabled, actual });
            }
            catch (Exception ex)
            {
                // Schreibfehler (z.B. gesperrte Registry) darf die App nie killen.
                Log.Error("Autostart-Einstellung konnte nicht geschrieben werden (App laeuft weiter)", ex,
                    new { enabled });
            }
        }
    }
}
