using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Forms;
using System.Windows.Threading;
using Microsoft.Extensions.DependencyInjection;
using PromptBoard.Core.Services;
using ClaudeVoiceOverlay.Services;
using ClaudeVoiceOverlay.Views;
using Application = System.Windows.Application;

namespace ClaudeVoiceOverlay
{
    public partial class App : Application
    {
        private NotifyIcon? _trayIcon;
        private OverlayWindow? _overlayWindow;

        // Statische Instanz-Bruecke fuer App-uebergreifende Tray-
        // Notifications. Notwendig weil _trayIcon privat ist und
        // Aufrufer (z.B. OverlayWindow.TryUploadHistoryAsync) keinen
        // direkten Zugriff haben. Wird im Constructor gesetzt — beim
        // Watchdog-Mode bleibt sie null, dann sind ShowTrayBalloon-
        // Aufrufe stille No-Ops (Watchdog hat ja gar kein Tray-Icon).
        private static App? _instance;

        public App()
        {
            _instance = this;

            var suppressToolTip = new System.Windows.Controls.ToolTipEventHandler(
                (_, args) => args.Handled = true);
            EventManager.RegisterClassHandler(
                typeof(FrameworkElement),
                System.Windows.Controls.ToolTipService.ToolTipOpeningEvent,
                suppressToolTip);
            EventManager.RegisterClassHandler(
                typeof(FrameworkContentElement),
                System.Windows.Controls.ToolTipService.ToolTipOpeningEvent,
                suppressToolTip);
        }

        /// <summary>
        /// Zeigt eine Tray-Balloon-Notification an. Wird vom OverlayWindow
        /// aufgerufen wenn ein Drive-Sync-Fehler User-Aufmerksamkeit
        /// braucht (z.B. invalid_grant nach abgelaufenem Refresh-Token).
        /// Defensiv: jeder Schritt ist try/catch geschuetzt, ein
        /// fehlendes Tray-Icon (Watchdog-Mode, oder vor SetupTrayIcon)
        /// macht den Aufruf zum stille No-Op statt zum Crash. Wenn das
        /// Tray-Icon irgendwann eine Visible=false-Bedingung hat, wird
        /// die Balloon nicht angezeigt — kein Fehler.
        /// </summary>
        public static void ShowTrayBalloon(string title, string text, ToolTipIcon icon = ToolTipIcon.Warning)
        {
            try
            {
                var trayIcon = _instance?._trayIcon;
                if (trayIcon is null) return;
                trayIcon.BalloonTipTitle = title;
                trayIcon.BalloonTipText  = text;
                trayIcon.BalloonTipIcon  = icon;
                trayIcon.ShowBalloonTip(8000);
            }
            catch (Exception ex)
            {
                LogCrash("ShowTrayBalloon", ex);
            }
        }

        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // Globale Exception-Sammler installieren BEVOR irgendein
            // anderer Code laufen kann. Frueher wurden async-void- und
            // background-Task-Exceptions still verschluckt — der Watchdog
            // restartete und Frank sah keinen Hinweis auf den Bug. Jetzt
            // wandert jede unbehandelte Exception in eine persistente
            // crash.log, sortiert nach Quelle (Dispatcher, AppDomain,
            // TaskScheduler). Direktive 3 Resilienz-Pruefung: der Logger
            // selbst hat eine try/catch-Wand und schreibt atomar (Append
            // mit File-System-Lock); ein Fehler im Logger blockiert nicht
            // den Crash-Pfad.
            InstallGlobalExceptionLogging();

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

        /// <summary>
        /// Schreibt eine Crash-Log-Zeile in
        /// <c>%LOCALAPPDATA%\PromptBoard\crash.log</c> und schluckt jeden
        /// eigenen Fehler (Logger darf NIE crashen). Nutzt UTF-8-Append
        /// damit Sonderzeichen aus deutschen Stack-Traces lesbar bleiben.
        /// Bewusst KEIN externes Logging-Framework — eine .txt-Datei
        /// genuegt fuer Frank und ist von Hand mit Notepad lesbar.
        /// </summary>
        private static void LogCrash(string source, Exception? ex, string? extraNote = null)
        {
            try
            {
                string dir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "PromptBoard");
                Directory.CreateDirectory(dir);
                string path = Path.Combine(dir, "crash.log");
                string ts = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
                string body = ex is null
                    ? (extraNote ?? "(no exception, no note)")
                    : $"{ex.GetType().FullName}: {ex.Message}\n{ex.StackTrace}";
                if (!string.IsNullOrEmpty(extraNote))
                    body = extraNote + "\n" + body;
                File.AppendAllText(path,
                    $"\n=== {ts}  [{source}] ===\n{body}\n",
                    System.Text.Encoding.UTF8);
            }
            catch
            {
                // Logger darf nichts werfen — sonst wird ein
                // Crash-im-Crash-Pfad zur Endlos-Schleife.
            }
        }

        /// <summary>
        /// Bindet ALLE bekannten Exception-Quellen an LogCrash an:
        /// Dispatcher (UI-Thread), AppDomain (alle Threads),
        /// TaskScheduler (unobservable async/Task-Exceptions). Jeder
        /// Handler ist defensiv, schluckt seinen eigenen Fehler und
        /// laesst den Original-Crash-Pfad weiterlaufen damit der
        /// Watchdog wie gewohnt restartet — wir ergaenzen nur die
        /// Sichtbarkeit, wir aendern das Crash-Verhalten nicht.
        /// </summary>
        private static void InstallGlobalExceptionLogging()
        {
            try
            {
                AppDomain.CurrentDomain.UnhandledException += (sender, args) =>
                {
                    LogCrash("AppDomain.UnhandledException",
                        args.ExceptionObject as Exception,
                        $"IsTerminating={args.IsTerminating}");
                };

                TaskScheduler.UnobservedTaskException += (sender, args) =>
                {
                    LogCrash("TaskScheduler.UnobservedTaskException", args.Exception,
                        "Observed by handler — exception was about to be silently dropped");
                    // Mark as observed damit der Prozess nicht zusaetzlich
                    // wegen unobservable Task crasht — wir haben es ja
                    // gerade geloggt.
                    args.SetObserved();
                };
            }
            catch (Exception setupEx)
            {
                LogCrash("InstallGlobalExceptionLogging", setupEx,
                    "Setup of global handlers failed — only Dispatcher remains");
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

            // Cross-Process-Signal fuer sauberen Shutdown. Manual-Reset
            // damit das Set einmal sichtbar bleibt bis der Watchdog es
            // sieht — nach dem Auto-Restart-Loop wird es zurueckgesetzt
            // sodass der naechste Crash-Restart-Zyklus wieder normal
            // funktioniert.
            var cleanShutdown = new EventWaitHandle(
                initialState: false,
                mode: EventResetMode.ManualReset,
                name: CleanShutdownEventName,
                createdNew: out _);

            // No UI for the watchdog — keep alive via explicit shutdown mode
            ShutdownMode = ShutdownMode.OnExplicitShutdown;

            Task.Run(() =>
            {
                // Crash-Backoff-Counter. Zaehlt aufeinander folgende Crashes
                // innerhalb eines kurzen Fensters (60 s). Frueher: starre 2s
                // Pause, endlose Restarts auch bei dauerhaft kaputter App
                // (z.B. korrupte DB, fehlende Native-DLL, OOM-Loop). Jetzt:
                // exponentieller Backoff bis 60 s, plus harte Grenze bei
                // 10 schnellen Crashes hintereinander — danach gibt der
                // Watchdog auf und beendet sich. Frank sieht dann zumindest
                // dass das Tray-Icon nicht mehr da ist und kann den Bug
                // im crash.log nachlesen statt sich ueber blinkende Restart-
                // Effekte zu wundern.
                int consecutiveCrashes = 0;
                DateTime lastCrashAt = DateTime.MinValue;
                int[] backoffMs = { 2_000, 4_000, 8_000, 15_000, 30_000, 60_000 };
                const int crashGiveUpThreshold = 10;

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

                    // Defense in depth: das Overlay setzt vor saubere
                    // App.Shutdown(0) das Event. Wir pruefen BEIDES — Event
                    // ODER Exit-Code — damit selbst bei Power-Loss oder
                    // Race-Condition-Edge-Cases der Watchdog erkennt dass
                    // ein bewusster Beenden-Klick im Tray ankam.
                    bool cleanRequested = cleanShutdown.WaitOne(0);

                    if (exitCode == 0 || cleanRequested)
                    {
                        // Normal shutdown (user clicked "Beenden") — stop watchdog too
                        Console.WriteLine($"Watchdog: overlay exited cleanly (code={exitCode}, signaled={cleanRequested}), stopping.");
                        Dispatcher.Invoke(() => Shutdown());
                        GC.KeepAlive(mutex);
                        GC.KeepAlive(cleanShutdown);
                        return;
                    }

                    // Crash or kill — Backoff-Counter aktualisieren. Wenn
                    // der letzte Crash mehr als 60 s zurueckliegt, war die
                    // App offensichtlich stabil dazwischen → Counter zurueck.
                    var now = DateTime.UtcNow;
                    if ((now - lastCrashAt).TotalSeconds > 60)
                        consecutiveCrashes = 0;
                    consecutiveCrashes++;
                    lastCrashAt = now;

                    if (consecutiveCrashes >= crashGiveUpThreshold)
                    {
                        // Geben auf — App ist offenkundig dauerhaft kaputt.
                        // Tray-Notification waere schoen, aber der Watchdog
                        // hat kein Tray-Icon (nur das Overlay hat eines).
                        // Stattdessen einfach beenden: das Tray-Icon
                        // verschwindet damit, Frank sieht es und kann im
                        // crash.log nachlesen warum.
                        LogCrash("Watchdog.GiveUp", null,
                            $"Stopping after {consecutiveCrashes} consecutive crashes within 60s. Last exit code={exitCode}.");
                        Console.WriteLine($"Watchdog: {consecutiveCrashes} consecutive crashes — giving up.");
                        Dispatcher.Invoke(() => Shutdown());
                        GC.KeepAlive(mutex);
                        GC.KeepAlive(cleanShutdown);
                        return;
                    }

                    // Exponentieller Backoff mit Hard-Cap bei 60 s. Index
                    // entspricht (consecutiveCrashes - 1) damit der erste
                    // Crash mit 2 s wartet wie zuvor.
                    int idx = Math.Min(consecutiveCrashes - 1, backoffMs.Length - 1);
                    int wait = backoffMs[idx];
                    Console.WriteLine($"Watchdog: overlay exited with code {exitCode} (crash #{consecutiveCrashes}), restarting in {wait / 1000}s...");
                    Thread.Sleep(wait);
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

        // Single-Instance-Lock fuer den OVERLAY-Prozess. Watchdog und
        // Overlay sind dieselbe .exe — beim Start mit `--run` kann jeder
        // einen zweiten Overlay-Prozess von Hand triggern (CLI, Verknuep-
        // fung, Task-Scheduler). Beide haetten dann ihren globalen LL-
        // Tastatur-Hook installiert und jeden Tastendruck doppelt
        // verarbeitet (PTT-Aufnahme zweimal, Hotkey-Paste zweimal). Das
        // Mutex muss die Lebensdauer des Process haben — Field damit GC
        // ihn nicht einsammelt waehrend wir die UI hochfahren.
        private Mutex? _overlayInstanceMutex;

        private void StartOverlay(int watchdogPid)
        {
            // Single-Instance-Check VOR allem anderen — sonst hat das
            // zweite Overlay schon Audio-Geraete und Tastatur-Hooks
            // angefasst, bevor wir ihn ausschalten koennen. Bei
            // doppeltem Start beendet sich der spaetere Prozess sofort
            // mit Exit-Code 0 → der Watchdog interpretiert das als
            // "saubere Beendigung" und stoppt.
            _overlayInstanceMutex = new Mutex(true, "ClaudeVoiceOverlay-Overlay", out bool overlayCreated);
            if (!overlayCreated)
            {
                Console.WriteLine("Overlay already running, exiting (single-instance lock).");
                _overlayInstanceMutex.Dispose();
                _overlayInstanceMutex = null;
                Shutdown(0);
                return;
            }

            // Catch unhandled exceptions for logging. Frueher: nur
            // Console.WriteLine — im Release-Build ohne Konsole still
            // verschluckt. Jetzt: persistenter File-Log (crash.log) plus
            // Console fuer den Dev-Pfad. e.Handled = false bleibt: der
            // Watchdog soll wie gewohnt restarten — wir ergaenzen nur die
            // Sichtbarkeit. Direktive-3-Resilienz-Pruefung: LogCrash hat
            // try/catch-Wand und schluckt jeden eigenen Fehler, sodass
            // der Crash-Pfad nie wegen Logging-Problemen blockiert.
            DispatcherUnhandledException += (_, e) =>
            {
                LogCrash("Dispatcher.UnhandledException", e.Exception);
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

            // Bring up the shared PromptBoard DI container so the star-button
            // panel can read prompts and build the always-on prefix. Failures
            // are non-fatal: the overlay stays usable without PromptBoard.
            try
            {
                PromptBoardHost.Initialize();
                Console.WriteLine($"PromptBoard DB: {PromptBoardHost.DbPath}");
                // Fire-and-forget: pull the latest Drive backup and apply it
                // if the remote is newer than the last local sync mark. Mirrors
                // the macOS launch-time auto-restore. Silent on every failure
                // path so the overlay never blocks on this.
                _ = Task.Run(CheckForRemoteBackupOnLaunchAsync);
            }
            catch (Exception phEx)
            {
                Console.WriteLine($"PromptBoardHost init failed: {phEx.Message}");
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
            // Frueher: 3-s-Polling-Loop mit Process.GetProcessById. Dabei wurde
            // alle 3 s ein neues Process-Handle allokiert + sofort wieder
            // verworfen, der Pool-Thread aufgeweckt und gleich wieder geparkt.
            // Im Idle (App laeuft, Frank macht nichts) waren das ~28 800
            // Wakeup-Zyklen pro Tag pro Overlay-Process — kostet zwar wenig
            // CPU, aber Akku-Sensitivitaet auf Laptops und unnoetiger GC-Druck.
            // Jetzt: Process.Exited-Event mit EnableRaisingEvents = true. Der
            // OS-Kernel signalisiert uns den Watchdog-Tod ueber WaitForInputIdle/
            // ProcessHandle, kein Polling noetig. Fallback bei Race: Wenn der
            // Watchdog zwischen GetProcessById und EnableRaisingEvents stirbt,
            // pruefen wir HasExited synchron und triggern Restart manuell.
            Process? watchdog;
            try
            {
                watchdog = Process.GetProcessById(watchdogPid);
            }
            catch
            {
                // Schon beim ersten Lookup tot — sofort restarten, keine Subscription noetig.
                RestartWatchdog();
                return;
            }

            try
            {
                watchdog.EnableRaisingEvents = true;
                watchdog.Exited += (_, _) =>
                {
                    Console.WriteLine("Overlay: watchdog died, restarting...");
                    RestartWatchdog();
                    try { watchdog.Dispose(); } catch { /* tolerant */ }
                };

                // Race-Schutz: Watchdog kann zwischen GetProcessById und
                // EnableRaisingEvents = true bereits gestorben sein. Dann feuert
                // Exited NICHT mehr, daher manueller Check. HasExited liest
                // GetExitCodeProcess — synchron und billig.
                if (watchdog.HasExited)
                {
                    Console.WriteLine("Overlay: watchdog already dead at subscribe time, restarting...");
                    RestartWatchdog();
                    try { watchdog.Dispose(); } catch { /* tolerant */ }
                }
            }
            catch (Exception ex)
            {
                // Falls EnableRaisingEvents wider Erwarten wirft (z.B. Access
                // Denied, weil Watchdog mit anderem Token laeuft): Fallback auf
                // das alte Polling — besser als gar keine Watchdog-Ueberwachung.
                LogCrash("MonitorWatchdog.Subscribe", ex,
                    "Falling back to polling because event subscription failed.");
                FallbackPolling(watchdogPid);
            }
        }

        private static void RestartWatchdog()
        {
            try
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = Environment.ProcessPath!,
                    UseShellExecute = true
                });
            }
            catch (Exception ex)
            {
                LogCrash("RestartWatchdog", ex);
            }
        }

        private static void FallbackPolling(int watchdogPid)
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
                        Console.WriteLine("Overlay: watchdog died (fallback path), restarting...");
                        RestartWatchdog();
                        break;
                    }
                }
            });
        }

        private void SetupTrayIcon()
        {
            _trayIcon = new NotifyIcon
            {
                Text = "Spracheingabe - Claude",
                Icon = SystemIcons.Application,
                Visible = true
            };

            var menu = new ContextMenuStrip();
            menu.Items.Add("Overlay zeigen", null, (_, _) => _overlayWindow?.Show());
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("Beenden", null, (_, _) => RequestCleanShutdown());

            _trayIcon.ContextMenuStrip = menu;
            _trayIcon.DoubleClick += (_, _) => _overlayWindow?.Show();
        }

        /// <summary>
        /// Sauberer Tray-Beenden-Pfad. Frueher: <c>Environment.Exit(0)</c> —
        /// killte den Prozess SOFORT ohne dass <see cref="OnExit"/>,
        /// <see cref="OverlayWindow.OnClosed"/> oder die IDisposable-Kette
        /// liefen. Folgen: Tray-Icon blieb als Geist im System-Tray, EF
        /// Core SQLite-WAL wurde nicht checkpointed (Risiko fuer
        /// Bestaetigungspflicht-Recovery beim naechsten Start), Hooks
        /// blieben theoretisch im Tastatur-Stack haengen.
        ///
        /// Jetzt: Shutdown-Token (named EventWaitHandle) signalisiert
        /// dem Watchdog dass kein Restart noetig ist, dann Window
        /// regulaer schliessen → OnClosed laeuft inkl. UnhookWindowsHookEx
        /// → <see cref="Application.Shutdown(int)"/> mit Exit-Code 0 →
        /// OnExit laeuft inkl. Tray-Disposal → Process beendet sauber.
        /// Der Watchdog liest sowohl den Exit-Code (==0 → stop) als
        /// auch das Event als Defense-in-Depth: bei Power-Loss waehrend
        /// Shutdown koennte der Exit-Code nicht 0 sein, aber das Event
        /// wurde bereits gesetzt.
        ///
        /// Direktive-3-Resilienz-Pruefung:
        /// - Window.Close() ist async (Closing-Event darf cancelen) — aber
        ///   das Overlay hat keinen Closing-Handler der cancelt, also
        ///   schliesst es. Falls in Zukunft jemand einen Cancel-Handler
        ///   einbaut, blockiert Shutdown — das ist gewolltes Verhalten.
        /// - Application.Shutdown ist re-entrant-safe.
        /// - Defensiver Fallback auf Environment.Exit(0) wenn die WPF-
        ///   Pipeline aus irgendeinem Grund kaputt ist (z.B.
        ///   Application.Current null nach OnExit-Reentrancy).
        /// - Tray-Icon wird VOR Shutdown ausgeblendet damit es nicht
        ///   1-2 Frames lang als Geist sichtbar ist waehrend OnExit
        ///   laeuft.
        /// </summary>
        private void RequestCleanShutdown()
        {
            try
            {
                // Watchdog-IPC-Signal: bestehendes named Event setzen
                // (vom Watchdog im RunWatchdog angelegt). OpenExisting
                // wirft wenn das Event nicht da ist — dann sind wir im
                // Stand-alone-Mode (ohne Watchdog), kein Signal noetig.
                try
                {
                    using var ev = EventWaitHandle.OpenExisting(CleanShutdownEventName);
                    ev.Set();
                }
                catch (WaitHandleCannotBeOpenedException)
                {
                    // Kein Watchdog-Event vorhanden — kein Problem,
                    // dann wartet auch niemand darauf.
                }

                if (_trayIcon is { } tray)
                {
                    try { tray.Visible = false; } catch { /* tolerant */ }
                }
                _overlayWindow?.Close();

                if (Application.Current is { } app)
                {
                    app.Shutdown(0);
                    return;
                }
            }
            catch (Exception ex)
            {
                LogCrash("RequestCleanShutdown", ex,
                    "Falling back to Environment.Exit(0) after clean-shutdown failure.");
            }

            // Last-Resort-Pfad falls Shutdown(0) fehlschlaegt — der
            // alte Direktexit. Damit ist garantiert, dass Frank die App
            // immer beenden kann; der Worst Case ist nur dass die
            // saubere OnExit-Kette ausfaellt.
            Environment.Exit(0);
        }

        /// <summary>
        /// Event-Name fuer Watchdog-Overlay-IPC. Per-Session (kein
        /// "Global\\"-Prefix), weil wir nur zwischen Watchdog und Overlay
        /// derselben User-Session koordinieren — Cross-User-Synchronisation
        /// ist ausdruecklich NICHT gewollt (jeder User hat seinen
        /// eigenen Watchdog + Overlay).
        /// </summary>
        private const string CleanShutdownEventName = "ClaudeVoiceOverlay-CleanShutdown";

        /// <summary>
        /// Pulls the newest Drive backup at launch and applies it if its
        /// <c>ExportedAt</c> is newer than the local last-sync mark — typical
        /// case: the user edited prompts on another machine. Silent on every
        /// failure path (Drive not connected, network off, malformed JSON).
        /// </summary>
        private static async Task CheckForRemoteBackupOnLaunchAsync()
        {
            try
            {
                var localBeforeDownload = await PromptBoardPanel.BuildBackupFingerprintAsync();
                var lastSynced = PromptBoardPanel.ReadLastSyncFingerprint();
                if (lastSynced is not null &&
                    !string.Equals(localBeforeDownload, lastSynced, StringComparison.Ordinal))
                {
                    Console.WriteLine("[App] launch-restore skipped (local PromptBoard changes pending)");
                    return;
                }

                using var scope = PromptBoardHost.Services.CreateScope();
                var drive = scope.ServiceProvider.GetRequiredService<IGoogleDriveBackupService>();
                if (!await drive.IsAuthenticatedAsync())
                {
                    Console.WriteLine("[App] launch-restore skipped (Drive not connected)");
                    return;
                }

                var json = await drive.DownloadLatestAsync();
                if (json is null)
                {
                    Console.WriteLine("[App] launch-restore: no backup on Drive yet");
                    return;
                }

                var remote = PromptBoardPanel.BackupExportedAtUtc(json);
                if (remote is null)
                {
                    Console.WriteLine("[App] launch-restore: no ExportedAt in backup");
                    return;
                }

                var local = PromptBoardPanel.ReadLastSyncUtc() ?? DateTime.MinValue.ToUniversalTime();
                // 2-second grace window so a backup we just uploaded ourselves
                // doesn't trigger a self-restore race.
                if (remote.Value > local.AddSeconds(2))
                {
                    var localAfterDownload = await PromptBoardPanel.BuildBackupFingerprintAsync();
                    if (!string.Equals(localBeforeDownload, localAfterDownload, StringComparison.Ordinal))
                    {
                        Console.WriteLine("[App] launch-restore skipped (PromptBoard changed during download)");
                        return;
                    }
                    await PromptBoardPanel.ApplyBackupJsonAsync(json);
                    PromptBoardPanel.WriteLastSync(remote.Value, json);
                    Console.WriteLine($"[App] launch-restore applied remote backup from {remote.Value:o}");
                }
                else
                {
                    Console.WriteLine($"[App] launch-restore: local up-to-date (remote={remote:o}, local={local:o})");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[App] launch-restore failed: {ex.Message}");
            }
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
