using System;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;

namespace VoiceAgent;

/// <summary>
/// Interaction logic for App.xaml.
///
/// Verkabelt beim Start den globalen Fehler-Faenger (Observability-First Abschnitt 2.2):
/// JEDER unbehandelte Fehler — egal auf welchem Thread — wird mit vollem Kontext geloggt,
/// BEVOR etwas abstuerzt. Grundsatz: nichts stirbt still.
/// </summary>
public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        // Handler ZUERST verkabeln, damit auch fruehe Startfehler erfasst werden.
        AppDomain.CurrentDomain.UnhandledException += OnDomainException;
        DispatcherUnhandledException += OnDispatcherException;
        TaskScheduler.UnobservedTaskException += OnUnobservedTaskException;

        Log.Info("App-Start: globaler Fehler-Faenger verkabelt");

        // Theme VOR dem ersten Fenster anwenden, damit die DynamicResource-Farben von Anfang
        // an existieren (kein Aufblitzen im falschen Profil). Defensiv: bei Fehler bleibt die
        // App lauffaehig (ThemeManager faengt intern ab), notfalls im hellen Standard.
        try { ThemeManager.Apply(Config.Load().Theme); }
        catch (Exception ex) { Log.Error("Theme beim Start anwenden fehlgeschlagen", ex); }

        base.OnStartup(e); // verarbeitet StartupUri (MainWindow)
    }

    /// <summary>Unbehandelte Exception auf einem beliebigen Thread (oft prozess-terminierend).</summary>
    private static void OnDomainException(object sender, UnhandledExceptionEventArgs e)
    {
        var ex = e.ExceptionObject as Exception;
        Log.Fatal(
            "Unbehandelte AppDomain-Exception" + (e.IsTerminating ? " (terminierend)" : ""),
            ex,
            new { isTerminating = e.IsTerminating });
    }

    /// <summary>
    /// Unbehandelte Exception auf dem UI-Thread. Wird geloggt, aber NICHT stillschweigend
    /// geschluckt (e.Handled bleibt false) — die App folgt ihrem normalen Verhalten, statt
    /// einen Fehler unsichtbar zu verbergen.
    /// </summary>
    private void OnDispatcherException(object sender, DispatcherUnhandledExceptionEventArgs e)
    {
        // Geloggt (nichts stirbt still) UND als behandelt markiert: ein unerwarteter UI-Fehler
        // darf die laufende Always-On-App nicht komplett verschwinden lassen (Graceful Degradation).
        // Der Fehler bleibt sichtbar — im Log und (wo moeglich) in der Status-/Fehlerzeile der UI.
        Log.Error("Unbehandelte UI-Dispatcher-Exception (App laeuft weiter)", e.Exception);
        e.Handled = true;
    }

    /// <summary>
    /// Fehler in einem nicht-awaiteten (fire-and-forget) Task. Wird geloggt UND als beobachtet
    /// markiert, damit eine verschluckte Hintergrund-Exception die Dauer-Voice-Loop nicht
    /// still killt — der Fehler ist dann sichtbar im Log, nicht verloren.
    /// </summary>
    private static void OnUnobservedTaskException(object? sender, UnobservedTaskExceptionEventArgs e)
    {
        Log.Error("Unbeobachtete Task-Exception", e.Exception);
        e.SetObserved();
    }
}
