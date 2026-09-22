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
        if (!Einzelstart.Beanspruchen(e.Args))
        {
            Diagnose.Ereignis(Schwere.Info, "app", "app.zweite_instanz",
                "Eine UpdateZentrale läuft bereits – dieser Start zeigt nur ihr Fenster.", daten: Diagnose.Laufzeitinfo());
            Einzelstart.VorhandenesFensterZeigen();
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
    }
}
