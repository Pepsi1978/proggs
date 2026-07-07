using System;
using System.Drawing;
using System.Windows.Forms;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services
{
    /// <summary>
    /// Infobereich-Symbol (System-Tray) fuer den VoiceAgent. Kapselt das WinForms-
    /// <see cref="NotifyIcon"/>, damit der WPF-Code nichts von WinForms wissen muss.
    ///
    /// Warum WinForms-NotifyIcon statt eigenem Shell_NotifyIcon-Nachbau: es re-registriert sein
    /// Icon nach einem Explorer-Neustart selbst (Almanach §6.1) und liefert ein funktionierendes
    /// Tray-Kontextmenue ohne den SetForegroundWindow-Trick (§6.3). Die einzige Kosten — die
    /// globalen WinForms/Drawing-usings — sind im csproj per &lt;Using Remove&gt; neutralisiert.
    /// </summary>
    public sealed class TrayIcon : IDisposable
    {
        private readonly NotifyIcon _icon;
        private bool _disposed;

        /// <summary>Doppelklick aufs Symbol oder „Oeffnen" im Menue — Fenster wiederherstellen.</summary>
        public event Action? OpenRequested;

        /// <summary>„Beenden" im Kontextmenue — App wirklich schliessen.</summary>
        public event Action? ExitRequested;

        public TrayIcon()
        {
            _icon = new NotifyIcon
            {
                Text = "VoiceAgent",
                Visible = false,            // erst sichtbar, wenn ins Tray minimiert wird
                Icon = LoadIcon(),
            };

            var menu = new ContextMenuStrip();
            menu.Items.Add("Öffnen", null, (_, __) => Raise(OpenRequested));
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("Beenden", null, (_, __) => Raise(ExitRequested));
            _icon.ContextMenuStrip = menu;

            // Doppelklick ist der erwartete Weg, das Fenster zurueckzuholen.
            _icon.DoubleClick += (_, __) => Raise(OpenRequested);
        }

        /// <summary>
        /// Laedt das App-Icon aus der laufenden EXE. <c>Environment.ProcessPath</c> ist
        /// single-file-sicher (Almanach §1.1). Faellt bei Fehler auf das System-Icon zurueck
        /// (funktionserhaltend — das Tray-Symbol erscheint immer, nur evtl. generisch).
        /// </summary>
        private static Icon LoadIcon()
        {
            try
            {
                string? exe = Environment.ProcessPath;
                if (!string.IsNullOrEmpty(exe))
                {
                    Icon? ico = Icon.ExtractAssociatedIcon(exe);
                    if (ico != null) return ico;
                }
            }
            catch (Exception ex)
            {
                Log.Error("Tray-Icon: App-Icon konnte nicht geladen werden (System-Icon als Fallback)", ex);
            }
            return SystemIcons.Application;
        }

        /// <summary>Blendet das Tray-Symbol ein. Optionaler kurzer Hinweis-Ballon, damit Frank es findet.</summary>
        public void Show(string? balloon = null)
        {
            if (_disposed) return;
            try
            {
                _icon.Visible = true;
                if (!string.IsNullOrEmpty(balloon))
                {
                    _icon.BalloonTipTitle = "VoiceAgent";
                    _icon.BalloonTipText = balloon;
                    _icon.ShowBalloonTip(2000);
                }
            }
            catch (Exception ex) { Log.Error("Tray-Icon: Einblenden fehlgeschlagen", ex); }
        }

        /// <summary>Blendet das Tray-Symbol wieder aus (Fenster ist zurueck in der Taskleiste).</summary>
        public void Hide()
        {
            if (_disposed) return;
            try { _icon.Visible = false; } catch (Exception ex) { Log.Error("Tray-Icon: Ausblenden fehlgeschlagen", ex); }
        }

        public void Dispose()
        {
            if (_disposed) return;
            _disposed = true;
            try { _icon.Visible = false; _icon.Dispose(); }
            catch (Exception ex) { Log.Error("Tray-Icon: Dispose fehlgeschlagen", ex); }
        }

        private static void Raise(Action? handler)
        {
            try { handler?.Invoke(); }
            catch (Exception ex) { Log.Error("Tray-Icon: Aktion fehlgeschlagen", ex); }
        }
    }
}
