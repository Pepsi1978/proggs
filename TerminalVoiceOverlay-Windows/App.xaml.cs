using System;
using System.Diagnostics;
using System.Drawing;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Forms;
using System.Windows.Threading;
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

            if (e.Args.Length > 0 && e.Args[0] == "--run")
            {
                // Overlay mode: run the actual UI
                StartOverlay();
            }
            else
            {
                // Watchdog mode: launch overlay and monitor it
                RunWatchdog();
            }
        }

        // ── Watchdog mode: invisible process that monitors the overlay ──

        private void RunWatchdog()
        {
            // Single-instance check: only one watchdog allowed
            var mutex = new Mutex(true, "TerminalVoiceOverlay-Watchdog", out bool created);
            if (!created)
            {
                Console.WriteLine("Watchdog already running, exiting.");
                Shutdown();
                return;
            }

            // No UI for the watchdog — keep alive via explicit shutdown mode
            ShutdownMode = ShutdownMode.OnExplicitShutdown;

            Task.Run(() =>
            {
                while (true)
                {
                    Console.WriteLine("Watchdog: starting overlay...");
                    var proc = Process.Start(new ProcessStartInfo
                    {
                        FileName = Environment.ProcessPath!,
                        Arguments = "--run",
                        UseShellExecute = false
                    });

                    proc?.WaitForExit();
                    var exitCode = proc?.ExitCode ?? -1;

                    if (exitCode == 0)
                    {
                        // Normal shutdown (user clicked "Beenden") — stop watchdog too
                        Console.WriteLine("Watchdog: overlay exited normally, stopping.");
                        Dispatcher.Invoke(() => Shutdown());
                        GC.KeepAlive(mutex);
                        return;
                    }

                    // Crash or kill — wait and restart
                    Console.WriteLine($"Watchdog: overlay exited with code {exitCode}, restarting in 2s...");
                    Thread.Sleep(2000);
                }
            });
        }

        // ── Overlay mode: the actual voice overlay with UI ──

        private void StartOverlay()
        {
            // Catch unhandled exceptions for logging
            DispatcherUnhandledException += (_, e) =>
            {
                Console.WriteLine($"Unhandled exception: {e.Exception.Message}");
                e.Handled = false; // Let it crash — watchdog will restart
            };

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
                Environment.Exit(1); // Error — watchdog will restart
                return;
            }

            // Create overlay window (starts hidden)
            _overlayWindow = new OverlayWindow(config);

            // Setup tray icon
            SetupTrayIcon();

            Console.WriteLine("TerminalVoiceOverlay gestartet (overlay mode)");
        }

        private void SetupTrayIcon()
        {
            _trayIcon = new NotifyIcon
            {
                Text = "Spracheingabe - Terminal",
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
                Environment.Exit(0); // Exit code 0 = intentional — watchdog stops
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
