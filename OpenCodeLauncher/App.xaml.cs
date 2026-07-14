using System.Windows;
using System.Runtime.InteropServices;
using System.Threading;
using OpenCodeLauncher.Services;

namespace OpenCodeLauncher;

public partial class App : Application
{
    private const string SingleInstanceMutexName = @"Local\OpenCodeLauncher_SingleInstance";
    private const string ActivationEventName = @"Local\OpenCodeLauncher_Activate";
    private const int ASFW_ANY = -1;

    private Mutex? _singleInstanceMutex;
    private bool _ownsSingleInstanceMutex;
    private EventWaitHandle? _activationEvent;
    private CancellationTokenSource? _activationListenerCts;

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool AllowSetForegroundWindow(int dwProcessId);

    protected override void OnStartup(StartupEventArgs e)
    {
        _singleInstanceMutex = new Mutex(true, SingleInstanceMutexName, out var createdNew);
        if (!createdNew)
        {
            SignalRunningInstance();
            _singleInstanceMutex.Dispose();
            _singleInstanceMutex = null;
            Shutdown(0);
            return;
        }
        _ownsSingleInstanceMutex = true;

        base.OnStartup(e);
        Services.Logger.Instance.Info("App", "OnStartup", "OpenCode Launcher gestartet");

        // Gespeichertes Design anwenden, bevor das Hauptfenster gerendert wird (kein Umschalt-Flackern).
        var layout = LayoutSettings.Load();
        var theme = string.Equals(layout.Theme, "Light", StringComparison.OrdinalIgnoreCase)
            ? ThemeManager.AppTheme.Light
            : ThemeManager.AppTheme.Dark;
        if (theme != ThemeManager.Current)
            ThemeManager.Apply(theme);

        _activationEvent = new EventWaitHandle(false, EventResetMode.AutoReset, ActivationEventName);
        StartActivationListener();

        MainWindow = new MainWindow();
        MainWindow.Show();
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
            log.Info("App", "SignalRunningInstance", "laufende OpenCode-Launcher-Instanz aktiviert");
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
