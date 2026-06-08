using System;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using Microsoft.Win32;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Zentrale Theme-Steuerung (Hell/Dunkel) fuer ALLE Fenster.
    ///
    /// Funktionsweise: Es gibt zwei Brush-Dictionaries (Themes/Light.xaml, Themes/Dark.xaml)
    /// mit identischen x:Key-Namen. Genau EINES davon liegt in
    /// <c>Application.Current.Resources.MergedDictionaries</c>. Alle Fenster referenzieren die
    /// Farben per <c>DynamicResource</c> — ein Tausch des Dictionaries faerbt deshalb die
    /// komplette UI zur Laufzeit um, ohne Neustart.
    ///
    /// Zusaetzlich wird die Windows-Titelleiste passend dunkel/hell gefaerbt
    /// (DWMWA_USE_IMMERSIVE_DARK_MODE, Almanach §2.7 — erst bei vorhandenem HWND).
    /// Alles defensiv (try/catch): ein Theme-Fehler darf die App nie crashen (Direktive #3).
    /// </summary>
    public static class ThemeManager
    {
        public enum Mode { Light, Dark, System }

        private const string LightUri = "pack://application:,,,/Themes/Light.xaml";
        private const string DarkUri = "pack://application:,,,/Themes/Dark.xaml";

        private static ResourceDictionary? _current;

        /// <summary>True, wenn aktuell das dunkle Profil aktiv ist (effektiv aufgeloest).</summary>
        public static bool IsDark { get; private set; }

        /// <summary>Feuert nach jedem Theme-Wechsel — Fenster setzen daraufhin ihre Titelleiste neu.</summary>
        public static event Action? Changed;

        /// <summary>Wandelt den gespeicherten Einstellungs-String in einen Modus.</summary>
        public static Mode Parse(string? theme) => (theme ?? "light").Trim().ToLowerInvariant() switch
        {
            "dark" => Mode.Dark,
            "system" => Mode.System,
            _ => Mode.Light,
        };

        /// <summary>Wendet das gewuenschte Profil an (System wird gegen das Windows-Theme aufgeloest).</summary>
        public static void Apply(Mode mode)
        {
            bool dark = mode switch
            {
                Mode.Dark => true,
                Mode.Light => false,
                _ => IsWindowsDark(),
            };
            ApplyResolved(dark);
        }

        /// <summary>Bequemer Aufruf direkt aus den Einstellungen ("light"/"dark"/"system").</summary>
        public static void Apply(string? theme) => Apply(Parse(theme));

        private static void ApplyResolved(bool dark)
        {
            try
            {
                var app = Application.Current;
                if (app == null) return;

                var dict = new ResourceDictionary { Source = new Uri(dark ? DarkUri : LightUri) };

                if (_current != null)
                    app.Resources.MergedDictionaries.Remove(_current);
                app.Resources.MergedDictionaries.Add(dict);
                _current = dict;
                IsDark = dark;

                Log.Info("Theme angewendet", new { theme = dark ? "dark" : "light" });

                // Titelleiste aller offenen Fenster nachziehen + Abonnenten benachrichtigen.
                foreach (Window w in app.Windows)
                    ApplyTitleBar(w);
                Changed?.Invoke();
            }
            catch (Exception ex)
            {
                Log.Error("Theme anwenden fehlgeschlagen (App laeuft im bisherigen Profil weiter)", ex);
            }
        }

        /// <summary>Schaltet zwischen Hell und Dunkel um und liefert den neuen Einstellungs-String zurueck.</summary>
        public static string Toggle()
        {
            ApplyResolved(!IsDark);
            return IsDark ? "dark" : "light";
        }

        /// <summary>
        /// Faerbt die native Titelleiste eines Fensters passend zum aktiven Theme.
        /// Sicher mehrfach aufrufbar; bei noch fehlendem HWND no-op (dann in SourceInitialized erneut rufen).
        /// </summary>
        public static void ApplyTitleBar(Window window)
        {
            try
            {
                var hwnd = new WindowInteropHelper(window).Handle;
                if (hwnd == IntPtr.Zero) return;   // HWND noch nicht da -> spaeter (SourceInitialized)
                int useDark = IsDark ? 1 : 0;
                // 20 = DWMWA_USE_IMMERSIVE_DARK_MODE (Win10 1903+); 19 = aelterer Build.
                if (DwmSetWindowAttribute(hwnd, 20, ref useDark, sizeof(int)) != 0)
                    DwmSetWindowAttribute(hwnd, 19, ref useDark, sizeof(int));
            }
            catch (Exception ex)
            {
                Log.Error("Titelleisten-Farbe setzen fehlgeschlagen (unkritisch)", ex);
            }
        }

        /// <summary>Liest das aktuelle Windows-App-Theme (true = dunkel). Fallback: hell.</summary>
        private static bool IsWindowsDark()
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(
                    @"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
                // AppsUseLightTheme: 0 = dunkel, 1 = hell.
                if (key?.GetValue("AppsUseLightTheme") is int v) return v == 0;
            }
            catch (Exception ex)
            {
                Log.Error("Windows-Theme lesen fehlgeschlagen — nehme hell an", ex);
            }
            return false;
        }

        [DllImport("dwmapi.dll", SetLastError = true)]
        private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int value, int size);
    }
}
