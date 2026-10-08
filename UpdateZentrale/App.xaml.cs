using System.Windows;
using UpdateZentrale.Services;

namespace UpdateZentrale;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        // Registered first: whatever fails from here on is recorded, also during startup.
        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
            Diagnose.UnbehandelteAusnahme("appdomain", args.ExceptionObject as Exception, args.IsTerminating);
        TaskScheduler.UnobservedTaskException += (_, args) =>
        {
            // Same semantics as before (the runtime ignores it), but no longer silently.
            Diagnose.UnbehandelteAusnahme("task", args.Exception, beendetApp: false);
            args.SetObserved();
        };

        // Only one window: two instances would write the same settings and log files, and two
        // update runs could meet on the same installer.
        // e.Args carries the handoff PID when this is the elevated successor of a running instance.
        var hintergrund = e.Args.Any(a => a.Equals(HintergrundSchalter, StringComparison.OrdinalIgnoreCase));
        if (!Einzelstart.Beanspruchen(e.Args))
        {
            Diagnose.Ereignis(Schwere.Info, "app", "app.zweite_instanz",
                "Eine UpdateZentrale läuft bereits – dieser Start zeigt nur ihr Fenster.", daten: Diagnose.Laufzeitinfo());
            if (!hintergrund)
            {
                // The running instance may be the hidden logon check: it has no window to bring
                // forward yet, so it is asked to show itself first.
                if (Einzelstart.ZeigenSignalisieren()) Thread.Sleep(500);
                Einzelstart.VorhandenesFensterZeigen();
            }
            Shutdown();
            return;
        }

        Diagnose.AppGestartet();
        Exit += (_, args) =>
        {
            Diagnose.AppBeendet(args.ApplicationExitCode);
            Einzelstart.Freigeben();
        };

        // Retention runs in the background; its own failures are recorded inside.
        _ = Task.Run(() =>
        {
            try { Diagnose.Aufraeumen(Protokollierung.Ordner, DateTime.Now); }
            catch (Exception ex) { Diagnose.Ausnahme(ex, "diagnose", "Aufräumen beim Start", Schwere.Warnung); }
        });

        // A crash in a background update task must not take the window with it -- but it is
        // recorded with type, stack and context before the message box appears.
        DispatcherUnhandledException += (_, args) =>
        {
            Diagnose.UnbehandelteAusnahme("ui", args.Exception, beendetApp: false);
            MessageBox.Show(args.Exception.Message, "UpdateZentrale - unerwarteter Fehler",
                MessageBoxButton.OK, MessageBoxImage.Warning);
            args.Handled = true;
        };

        base.OnStartup(e);

        // The window is created here instead of through StartupUri, because the logon start
        // must not show it: it checks hidden and leaves again. A start by hand meanwhile makes
        // it visible -- with the check still running and its progress in the footer.
        var fenster = new HauptFenster();
        MainWindow = fenster;
        Einzelstart.ZeigenAbonnieren(() => Dispatcher.BeginInvoke(() => FensterZeigen(fenster)));
        if (!hintergrund) FensterZeigen(fenster);

        _ = StartpruefungAsync(fenster, hintergrund);
    }

    public const string HintergrundSchalter = "--hintergrund";

    private bool _gezeigt;

    private void FensterZeigen(Window fenster)
    {
        _gezeigt = true;
        fenster.Show();
        if (fenster.WindowState == WindowState.Minimized) fenster.WindowState = WindowState.Normal;
        fenster.Activate();
        // A window shown from the background does not get the foreground by Activate alone.
        fenster.Topmost = true;
        fenster.Topmost = false;
    }

    private async Task StartpruefungAsync(Window fenster, bool hintergrund)
    {
        try
        {
            if (fenster.DataContext is ViewModels.HauptViewModel modell) await modell.ErstePruefungAsync(alles: hintergrund);
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "app", "Anfangsprüfung");
            if (_gezeigt)
                MessageBox.Show(ex.Message, "UpdateZentrale - unerwarteter Fehler", MessageBoxButton.OK, MessageBoxImage.Warning);
        }

        // Nobody asked for the window during the hidden check: the answers are stored, done.
        if (hintergrund && !_gezeigt) Shutdown();
    }
}
