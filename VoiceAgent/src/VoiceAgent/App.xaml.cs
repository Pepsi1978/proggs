using System;
using System.Linq;
using System.Runtime.InteropServices;
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
    [DllImport("shell32.dll", CharSet = CharSet.Unicode, PreserveSig = false)]
    private static extern void SetCurrentProcessExplicitAppUserModelID(
        [MarshalAs(UnmanagedType.LPWStr)] string appID);

    [DllImport("user32.dll")] private static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] private static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    private const int SW_RESTORE = 9;

    /// <summary>
    /// Single-Instance-Sperre (Almanach dotnet-csharp §6.4). MUSS static gehalten werden, sonst
    /// gibt der GC den Mutex frei und der Schutz verschwindet. VoiceAgent ist eine Always-On-
    /// Voice-App: zwei Instanzen teilen sich dasselbe Mikrofon, antworten beide und hoeren sich
    /// gegenseitig zu ("zwei Stimmen", Selbst-Zuhoeren) — genau das verhindert dieser Mutex.
    /// </summary>
    private static System.Threading.Mutex? _singleInstanceMutex;

    /// <summary>
    /// true, wenn die App mit dem Schalter <c>--minimized</c> gestartet wurde (Windows-Autostart,
    /// siehe <see cref="AutostartManager"/>). Dann geht das Hauptfenster beim Laden direkt unsichtbar
    /// ins Infobereich-Symbol (Tray) und laeuft im Hintergrund (siehe MainWindow.OnLoaded).
    /// </summary>
    public static bool StartMinimizedToTray { get; private set; }

    protected override void OnStartup(StartupEventArgs e)
    {
        // Handler ZUERST verkabeln, damit auch fruehe Startfehler erfasst werden.
        AppDomain.CurrentDomain.UnhandledException += OnDomainException;
        DispatcherUnhandledException += OnDispatcherException;
        TaskScheduler.UnobservedTaskException += OnUnobservedTaskException;

        Log.Info("App-Start: globaler Fehler-Faenger verkabelt");

        // Single-Instance-Schutz (Poka-Yoke Stufe 3, Almanach dotnet-csharp §6.4): laeuft schon
        // eine Instanz, holt diese sie nach vorn und beendet sich selbst. Verhindert die
        // "zwei Stimmen / alles doppelt"-Situation durch zwei gleichzeitige VoiceAgent.exe.
        bool createdNew;
        try
        {
            _singleInstanceMutex = new System.Threading.Mutex(
                true, @"Local\Frank.VoiceAgent.SingleInstance", out createdNew);
        }
        catch (Exception ex)
        {
            // Im Zweifel starten, nicht blockieren (funktionserhaltend).
            Log.Error("Single-Instance-Mutex konnte nicht erstellt werden (App startet trotzdem)", ex);
            createdNew = true;
        }
        if (!createdNew)
        {
            Log.Info("App-Start: bereits eine Instanz aktiv — diese zweite Instanz beendet sich (Single-Instance)");
            TryActivateExistingInstance();
            Shutdown();
            return;
        }

        // Autostart-Schalter auswerten: kam die App per Windows-Autostart (--minimized) hoch,
        // startet sie unsichtbar im Tray (Hintergrund). Defensiv: e.Args kann leer sein.
        StartMinimizedToTray = e.Args?.Any(
            a => string.Equals(a, AutostartManager.MinimizedArg, StringComparison.OrdinalIgnoreCase)) == true;
        if (StartMinimizedToTray) Log.Info("App-Start: --minimized erkannt — Hauptfenster startet im Tray");

        // Stabile AppUserModelID VOR dem ersten Fenster setzen: gibt der App eine eindeutige
        // Taskleisten-Identitaet. Ohne sie leitet Windows 11 eine implizite ID ab und zeigt fuer
        // den laufenden Fenster-Button teils ein generisches Default-Icon (zweite Ursache neben dem
        // Icon-Timing, bug-almanac dotnet-csharp §6.6). Defensiv: bei Fehler laeuft die App weiter.
        try { SetCurrentProcessExplicitAppUserModelID("Frank.VoiceAgent"); }
        catch (Exception ex) { Log.Error("AppUserModelID konnte nicht gesetzt werden", ex); }

        // Theme VOR dem ersten Fenster anwenden, damit die DynamicResource-Farben von Anfang
        // an existieren (kein Aufblitzen im falschen Profil). Defensiv: bei Fehler bleibt die
        // App lauffaehig (ThemeManager faengt intern ab), notfalls im hellen Standard.
        try { ThemeManager.Apply(Config.Load().Theme); }
        catch (Exception ex) { Log.Error("Theme beim Start anwenden fehlgeschlagen", ex); }

        base.OnStartup(e); // verarbeitet StartupUri (MainWindow)
    }

    /// <summary>
    /// Holt das Fenster der bereits laufenden Instanz nach vorn (best effort). Bei einer im Tray
    /// versteckten Instanz ist kein MainWindowHandle vorhanden — dann passiert nichts (Hauptsache
    /// es startet keine zweite Instanz). Defensiv: jeder Fehler wird geloggt, nie geworfen.
    /// </summary>
    private static void TryActivateExistingInstance()
    {
        try
        {
            var me = System.Diagnostics.Process.GetCurrentProcess();
            foreach (var p in System.Diagnostics.Process.GetProcessesByName(me.ProcessName))
            {
                if (p.Id == me.Id) continue;
                var h = p.MainWindowHandle;
                if (h != IntPtr.Zero) { ShowWindow(h, SW_RESTORE); SetForegroundWindow(h); }
                break;
            }
        }
        catch (Exception ex) { Log.Error("Bestehende Instanz aktivieren fehlgeschlagen", ex); }
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
