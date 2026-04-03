using System;
using System.Diagnostics;
using System.Drawing;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
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

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            if (e.Args.Length > 0 && e.Args[0] == "--run")
            {
                // Overlay mode: run the actual UI
                var watchdogPid = GetArgValue(e.Args, "--watchdog-pid");
                StartOverlay(watchdogPid);
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
            var mutex = new Mutex(true, "ClaudeVoiceOverlay-Watchdog", out bool created);
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
                    // Find existing overlay or start a new one
                    var overlay = FindOverlayProcess() ?? StartOverlayProcess();

                    if (overlay == null)
                    {
                        Thread.Sleep(2000);
                        continue;
                    }

                    Console.WriteLine($"Watchdog: monitoring overlay PID {overlay.Id}");
                    overlay.WaitForExit();
                    var exitCode = overlay.ExitCode;

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

        private Process? FindOverlayProcess()
        {
            var myPid = Environment.ProcessId;
            return Process.GetProcessesByName("ClaudeVoiceOverlay")
                .FirstOrDefault(p => p.Id != myPid);
        }

        private Process? StartOverlayProcess()
        {
            Console.WriteLine("Watchdog: starting overlay...");
            return Process.Start(new ProcessStartInfo
            {
                FileName = Environment.ProcessPath!,
                Arguments = $"--run --watchdog-pid={Environment.ProcessId}",
                UseShellExecute = false
            });
        }

        // ── Overlay mode: the actual voice overlay with UI ──

        private void StartOverlay(int watchdogPid)
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
                    "ClaudeVoiceOverlay — Konfigurationsfehler",
                    MessageBoxButton.OK,
                    MessageBoxImage.Error);
                Environment.Exit(1);
                return;
            }

            _overlayWindow = new OverlayWindow(config);
            SetupTrayIcon();

            // Monitor the watchdog — restart it if it dies
            if (watchdogPid > 0)
                MonitorWatchdog(watchdogPid);

            Console.WriteLine($"ClaudeVoiceOverlay gestartet (overlay mode, watchdog PID={watchdogPid})");
        }

        private void MonitorWatchdog(int watchdogPid)
        {
            Task.Run(() =>
            {
                while (true)
                {
                    Thread.Sleep(3000);
                    try
                    {
                        Process.GetProcessById(watchdogPid);
                    }
                    catch
                    {
                        // Watchdog is gone — restart it
                        Console.WriteLine("Overlay: watchdog died, restarting...");
                        Process.Start(new ProcessStartInfo
                        {
                            FileName = Environment.ProcessPath!,
                            UseShellExecute = true
                        });
                        break;
                    }
                }
            });
        }

        private void SetupTrayIcon()
        {
            _trayIcon = new NotifyIcon
            {
                Text = "Spracheingabe - Claude Desktop",
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
                Environment.Exit(0); // Exit code 0 — watchdog stops cleanly
            });

            _trayIcon.ContextMenuStrip = menu;
            _trayIcon.DoubleClick += (_, _) => _overlayWindow?.Show();
        }

        protected override void OnExit(ExitEventArgs e)
        {
            _trayIcon?.Dispose();
            base.OnExit(e);
        }

        private static int GetArgValue(string[] args, string key)
        {
            var prefix = key + "=";
            var arg = args.FirstOrDefault(a => a.StartsWith(prefix));
            return arg != null && int.TryParse(arg.Substring(prefix.Length), out var val) ? val : 0;
        }
    }
}
