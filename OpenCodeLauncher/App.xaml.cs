using System.Windows;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading;
using OpenLauncher.Services;

namespace OpenLauncher;

public partial class App : Application
{
    private const string SingleInstanceMutexName = @"Local\OpenLauncher_SingleInstance";
    private const string ActivationEventName = @"Local\OpenLauncher_Activate";
    private const int ASFW_ANY = -1;

    private Mutex? _singleInstanceMutex;
    private bool _ownsSingleInstanceMutex;
    private EventWaitHandle? _activationEvent;
    private CancellationTokenSource? _activationListenerCts;

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool AllowSetForegroundWindow(int dwProcessId);

    protected override void OnStartup(StartupEventArgs e)
    {
        // Muss VOR jedem anderen Zugriff auf den Anwendungsdatenordner laufen (auch vor dem Logger,
        // der seinen Ordner selbst anlegt) — sonst existiert das Ziel bereits und die Uebernahme
        // der alten Daten wuerde stillschweigend ausbleiben.
        MigrateLegacyAppData();

        // Aktivierungs-Event VOR dem Single-Instance-Mutex anlegen: sonst existiert ein kurzes
        // Startfenster, in dem eine zweite Instanz den Mutex bereits sieht, das Event aber noch nicht
        // erzeugt ist — SignalRunningInstance (OpenExisting) würde dann fehlschlagen und das erste
        // Fenster nicht nach vorne holen. Benannte Handles sind idempotent (beide Instanzen öffnen
        // dasselbe systemweite Event).
        _activationEvent = new EventWaitHandle(false, EventResetMode.AutoReset, ActivationEventName);

        _singleInstanceMutex = new Mutex(true, SingleInstanceMutexName, out var createdNew);
        if (!createdNew)
        {
            SignalRunningInstance();
            _singleInstanceMutex.Dispose();
            _singleInstanceMutex = null;
            _activationEvent.Dispose();
            _activationEvent = null;
            Shutdown(0);
            return;
        }
        _ownsSingleInstanceMutex = true;

        base.OnStartup(e);
        Services.Logger.Instance.Info("App", "OnStartup", "OpenLauncher gestartet");

        // Gespeichertes Design anwenden, bevor das Hauptfenster gerendert wird (kein Umschalt-Flackern).
        var layout = LayoutSettings.Load();
        var theme = string.Equals(layout.Theme, "Light", StringComparison.OrdinalIgnoreCase)
            ? ThemeManager.AppTheme.Light
            : ThemeManager.AppTheme.Dark;
        if (theme != ThemeManager.Current)
            ThemeManager.Apply(theme);

        StartActivationListener();

        MainWindow = new MainWindow();
        MainWindow.Show();
    }

    /// <summary>
    /// Uebernimmt Einstellungen, Modell-Registry, Layout und Logs aus dem Datenordner des frueheren
    /// Namens (OpenCodeLauncher) in den neuen (OpenLauncher). Kopiert, damit der alte Ordner als
    /// Sicherheitsnetz bestehen bleibt. Die Marker-Datei sorgt dafuer, dass genau einmal uebernommen
    /// wird: ohne sie wuerden spaeter geloeschte Dateien beim naechsten Start wieder auftauchen.
    /// </summary>
    private static void MigrateLegacyAppData()
    {
        foreach (var folder in new[] { Environment.SpecialFolder.ApplicationData, Environment.SpecialFolder.LocalApplicationData })
        {
            var root = Environment.GetFolderPath(folder);
            var legacy = Path.Combine(root, "OpenCodeLauncher");
            var current = Path.Combine(root, "OpenLauncher");
            var marker = Path.Combine(current, ".migriert-von-opencodelauncher");
            if (!Directory.Exists(legacy) || File.Exists(marker)) continue;

            try
            {
                CopyDirectory(legacy, current);
                File.WriteAllText(marker, $"Uebernommen aus {legacy} am {DateTime.Now:dd.MM.yyyy HH:mm}.");
            }
            catch (Exception ex)
            {
                // Nicht ueber den Logger melden: der legt seinen Ordner im Zielpfad selbst an und
                // wuerde damit den Zustand verschleiern. Ohne Marker wird beim naechsten Start erneut versucht.
                MessageBox.Show(
                    $"Die Daten aus dem frueheren Ordner konnten nicht uebernommen werden:\n{legacy}\n\n{ex.Message}",
                    "OpenLauncher", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }
    }

    private static void CopyDirectory(string source, string target)
    {
        Directory.CreateDirectory(target);
        foreach (var file in Directory.GetFiles(source))
        {
            // Vorhandenes im Ziel gewinnt und bricht nicht ab: sonst liesse ein einzelner Treffer
            // (etwa eine bereits angelegte Log-Datei) die ganze Uebernahme dauerhaft scheitern.
            var destination = Path.Combine(target, Path.GetFileName(file));
            if (!File.Exists(destination))
                File.Copy(file, destination);
        }
        foreach (var dir in Directory.GetDirectories(source))
            CopyDirectory(dir, Path.Combine(target, Path.GetFileName(dir)));
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _activationListenerCts?.Cancel();
        // Den blockierenden WaitOne()-Listener aufwecken, damit er den Cancel-Token sieht und die
        // Schleife sauber verlässt, BEVOR das Handle disposed wird (sonst ObjectDisposedException-Race).
        try { _activationEvent?.Set(); } catch (ObjectDisposedException) { }
        _activationEvent?.Dispose();
        _activationListenerCts?.Dispose();
        if (_ownsSingleInstanceMutex)
            _singleInstanceMutex?.ReleaseMutex();
        _singleInstanceMutex?.Dispose();
        base.OnExit(e);
    }

    private static void SignalRunningInstance()
    {
        var log = Services.Logger.Instance;
        try
        {
            AllowSetForegroundWindow(ASFW_ANY);
            using var activationEvent = EventWaitHandle.OpenExisting(ActivationEventName);
            activationEvent.Set();
            log.Info("App", "SignalRunningInstance", "laufende OpenLauncher-Instanz aktiviert");
        }
        catch (Exception ex)
        {
            log.Warn("App", "SignalRunningInstance", $"laufende Instanz konnte nicht signalisiert werden: {ex.Message}");
        }
    }

    private void StartActivationListener()
    {
        var activationEvent = _activationEvent;
        if (activationEvent == null) return;

        _activationListenerCts = new CancellationTokenSource();
        var token = _activationListenerCts.Token;
        _ = Task.Run(() =>
        {
            while (!token.IsCancellationRequested)
            {
                try
                {
                    activationEvent.WaitOne();
                    if (token.IsCancellationRequested) break;
                    Dispatcher.BeginInvoke(new Action(() =>
                    {
                        if (MainWindow is MainWindow window)
                            window.BringToForegroundFromExternalActivation();
                    }));
                }
                catch (ObjectDisposedException)
                {
                    break;
                }
                catch (Exception ex)
                {
                    Services.Logger.Instance.Warn("App", "StartActivationListener", $"Aktivierungssignal fehlgeschlagen: {ex.Message}");
                }
            }
        }, token);
    }
}
