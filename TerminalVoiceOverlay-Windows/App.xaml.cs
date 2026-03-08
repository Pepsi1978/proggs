using System;
using System.Drawing;
using System.Windows;
using System.Windows.Forms;
using TerminalVoiceOverlay.Services;
using TerminalVoiceOverlay.Views;
using Application = System.Windows.Application;

namespace TerminalVoiceOverlay
{
    public partial class App : Application
    {
        private NotifyIcon? _trayIcon;
        private OverlayWindow? _overlayWindow;

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            Config config;
            try
            {
                config = Config.Load();
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show(
                    ex.Message,
                    "TerminalVoiceOverlay — Konfigurationsfehler",
                    MessageBoxButton.OK,
                    MessageBoxImage.Error);
                Shutdown();
                return;
            }

            // Create overlay window (starts hidden)
            _overlayWindow = new OverlayWindow(config);

            // Setup tray icon
            SetupTrayIcon();

            System.Diagnostics.Debug.WriteLine("TerminalVoiceOverlay gestartet");
        }

        private void SetupTrayIcon()
        {
            _trayIcon = new NotifyIcon
            {
                Text = "TerminalVoiceOverlay",
                Icon = SystemIcons.Application,
                Visible = true
            };

            var menu = new ContextMenuStrip();
            menu.Items.Add("Overlay zeigen", null, (_, _) => _overlayWindow?.Show());
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("Beenden", null, (_, _) =>
            {
                _trayIcon!.Visible = false;
                _overlayWindow?.Close();
                Shutdown();
            });

            _trayIcon.ContextMenuStrip = menu;
            _trayIcon.DoubleClick += (_, _) => _overlayWindow?.Show();
        }

        protected override void OnExit(ExitEventArgs e)
        {
            _trayIcon?.Dispose();
            base.OnExit(e);
        }
    }
}
