using System;
using System.Diagnostics;
using System.Drawing;
using System.Windows;
using System.Windows.Forms;
using System.Windows.Threading;
using ClaudeVoiceOverlay.Services;
using ClaudeVoiceOverlay.Views;
using Application = System.Windows.Application;

namespace ClaudeVoiceOverlay
{
    public partial class App : Application
    {
        private NotifyIcon? _trayIcon;
        private OverlayWindow? _overlayWindow;
        private bool _isShuttingDown;

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // Self-restart on unhandled exceptions (replaces external watcher.vbs)
            DispatcherUnhandledException += OnUnhandledException;
            AppDomain.CurrentDomain.UnhandledException += OnDomainUnhandledException;

            Config config;
            try
            {
                config = Config.Load();
            }
            catch (Exception ex)
            {
                System.Windows.MessageBox.Show(
                    ex.Message,
                    "ClaudeVoiceOverlay — Konfigurationsfehler",
                    MessageBoxButton.OK,
                    MessageBoxImage.Error);
                Shutdown();
                return;
            }

            // Create overlay window (starts hidden)
            _overlayWindow = new OverlayWindow(config);

            // Setup tray icon
            SetupTrayIcon();

            Console.WriteLine("ClaudeVoiceOverlay gestartet");
        }

        private void OnUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
        {
            Console.WriteLine($"Unhandled UI exception: {e.Exception.Message}");
            e.Handled = true;
            RestartSelf();
        }

        private void OnDomainUnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            Console.WriteLine($"Unhandled domain exception: {(e.ExceptionObject as Exception)?.Message}");
            RestartSelf();
        }

        private void RestartSelf()
        {
            if (_isShuttingDown) return;
            _isShuttingDown = true;

            try
            {
                var exePath = Environment.ProcessPath;
                if (exePath != null)
                {
                    Console.WriteLine("Restarting after crash...");
                    Process.Start(new ProcessStartInfo(exePath) { UseShellExecute = true });
                }
            }
            catch { /* best effort */ }

            Environment.Exit(1);
        }

        private void SetupTrayIcon()
        {
            _trayIcon = new NotifyIcon
            {
                Text = "ClaudeVoiceOverlay",
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
