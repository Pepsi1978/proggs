using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Threading;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;
using VoiceAgent.Services.Audio;
using VoiceAgent.Services.Llm;

namespace VoiceAgent
{
    /// <summary>
    /// Hauptfenster und Orchestrierung des Voice-Loops:
    /// Mikrofon (AlwaysOnListener) → Transkription (Groq) → semantische Endpunkt-Erkennung
    /// (sammelt Sprech-Haeppchen, wartet Gedankenpausen ab) → Hauptagent (LLM) →
    /// Sprachausgabe (Google Chirp3-HD). Text-Eingabe bleibt zusaetzlich moeglich.
    /// </summary>
    public partial class MainWindow : Window
    {
        private AppSettings _settings = new();

        // Sichtbare App-Version (aus der Assembly = <Version> der csproj abgeleitet, nicht hartkodiert).
        // Wird in der Titelleiste angezeigt und beim Start geloggt (Regel: version-bump-visible-always).
        private static readonly string AppVersion =
            System.Reflection.Assembly.GetExecutingAssembly().GetName().Version is { } v
                ? $"{v.Major}.{v.Minor}.{v.Build}" : "?";
        private BossAgent? _agent;
        private EndpointDetector? _endpoint;
        private IntentDetector? _intentDetector;
        private UnderstandingDetector? _understanding;   // Verstehens-Gehirn (Brain-Rolle, starkes Modell)
        private GoogleTtsClient? _tts;
        private GroqWhisperClient? _stt;
        private AlwaysOnListener? _listener;
        private readonly AudioPlayer _player = new();
        private AgentMemory? _memory;                 // persistentes Langzeit-Gedaechtnis (ueber Sessions)
        private SubAgentRegistry? _subAgents;         // Unteragenten, die der Hauptagent dirigiert
        private CustomAgentStore? _customAgents;      // per Sprache angelegte, persistente Helfer-Definitionen
        private readonly ShellExecutor _shell = new();   // fuehrt Computer-Use-Befehle aus (nach Guard-Freigabe)
        private ReminderService? _reminderService;                // geplante Erinnerungen (zeitgesteuert)
        private readonly Chime _chime = new();                    // Enterprise-Sound fuer proaktive Meldungen
        private Reminder? _pendingReminder;                       // faellig, wartet auf Franks Reaktion
        private readonly Queue<Reminder> _reminderQueue = new();  // weitere faellige, noch nicht angesagt
        private DispatcherTimer? _reminderPingTimer;              // wiederholt den Sound jede Minute bis Reaktion

        // ----- Generischer proaktiver Funkspruch-Kanal (nicht nur Erinnerungen) -----
        private readonly ProactiveChannel _proactive = new();     // beliebige Quellen koennen proaktiv melden
        private ProactiveMessage? _pendingProactive;              // wartet auf Franks Reaktion
        private DispatcherTimer? _proactivePingTimer;             // wiederholt den Funkspruch-Sound bis Reaktion
        private DispatcherTimer? _followupTimer;                  // erster Producer: Nachfass bei offener Rueckfrage

        private bool _busy;                          // verhindert ueberlappende Verarbeitung (nur UI-Thread)
        private string _pending = string.Empty;      // gesammelte Sprech-Haeppchen einer noch offenen Aussage
        private bool _speechBeganAwake;              // begann die laufende Aufnahme im WACHEN Zustand? (kein stilles Verwerfen nach Timeout)
        private CancellationTokenSource? _safetyCts;  // Sicherheitsnetz-Timer nach "WEITER"
        private TurnTrace? _turn;                     // aktueller Sprach-Turn (Live-Logik-Sonde)
        private DispatcherTimer? _clockTimer;         // live Uhr oben im Fenster

        private SessionStore? _sessionStore;          // Persistenz der Sessions (JSON pro Session)
        private SessionManager? _sessions;            // aktive Session, entkoppelt vom Provider/Settings
        private ContextCompressor? _compressor;       // verlustarme Hintergrund-Komprimierung pro Session
        private ILlmProvider? _provider;              // aktiver LLM-Provider (auch fuer die Komprimierung)
        private bool _ready;                          // true erst NACH dem Laden — Guard gegen Handler beim XAML-Load (§2.10)
        private bool _suppressSelection;              // unterdrueckt SelectionChanged beim Neu-Befuellen der Liste

        private WakeWordController? _wake;            // Weckwort-Zustandsautomat (null = Feature aus / Init fehlgeschlagen)
        private readonly Chime _wakeChime = new("wakeword.wav");  // Erkennungston beim Wecken
        private readonly Chime _sleepChime = new("sleep.wav");    // abfallender Ton beim automatischen Einschlafen (Stille-Timeout)

        private TrayIcon? _tray;                       // Infobereich-Symbol (nur sichtbar, wenn ins Tray minimiert)

        public MainWindow()
        {
            InitializeComponent();
            Title = $"VoiceAgent v{AppVersion}";          // Version sichtbar in der Titelleiste (Regel: version-bump-visible-always)
            SourceInitialized += OnSourceInitialized;   // Taskleisten-Icon setzen, sobald das HWND existiert
            ContentRendered += OnContentRendered;       // Re-Apply nach 1. Rendern (Win11-Taskleisten-Cache, #11308)
            Loaded += OnLoaded;
            Closed += OnClosed;
            StateChanged += OnStateChanged;             // Minimieren -> ggf. ins Tray verstecken
            ThemeManager.Changed += UpdateThemeButton;  // Topbar-Symbol bei jedem Theme-Wechsel nachziehen
        }

        // ---------- Minimieren ins Tray (Infobereich-Symbol) ----------

        /// <summary>
        /// Reagiert auf das Minimieren des Fensters. Ist „Beim Minimieren ins Tray" aktiv, wird das
        /// Fenster aus der Taskleiste genommen (<see cref="Window.Hide"/>) und nur das Tray-Symbol
        /// gezeigt. Sonst bleibt das klassische Verhalten (Minimieren in die Taskleiste).
        ///
        /// Live-Logik-Sonde (Intent-Verifikation, observability-live-logic-probes): protokolliert
        /// „erwartet vs. tatsaechlich", damit live pruefbar ist, ob die gewaehlte Einstellung greift.
        /// </summary>
        private void OnStateChanged(object? sender, EventArgs e)
        {
            try
            {
                if (WindowState != WindowState.Minimized) return;

                bool toTray = _settings.MinimizeToTray;
                Log.Info("CHECKPOINT Fenster minimiert", new
                {
                    step = "Minimieren",
                    intent = "Tray-Symbol statt Taskleiste, wenn so eingestellt",
                    expected = toTray ? "ins Tray verstecken" : "in der Taskleiste lassen",
                    actual = toTray ? "ins Tray verstecken" : "in der Taskleiste lassen",
                    minimizeToTray = toTray,
                    ok = true
                });

                if (!toTray) return;   // klassisches Minimieren in die Taskleiste

                EnsureTray();
                _tray?.Show(_trayHintShown ? null : "Läuft weiter im Hintergrund. Doppelklick zum Öffnen.");
                _trayHintShown = true;
                Hide();                // aus der Taskleiste nehmen; Fenster bleibt offen -> App laeuft weiter
            }
            catch (Exception ex) { Log.Error("Minimieren ins Tray fehlgeschlagen (App laeuft weiter)", ex); }
        }

        private bool _trayHintShown;   // den Hinweis-Ballon nur beim ersten Mal zeigen

        /// <summary>Legt das Tray-Symbol bei Bedarf an und verdrahtet Oeffnen/Beenden (idempotent).</summary>
        private void EnsureTray()
        {
            if (_tray != null) return;
            _tray = new TrayIcon();
            _tray.OpenRequested += RestoreFromTray;
            _tray.ExitRequested += ExitFromTray;
        }

        /// <summary>Holt das Fenster aus dem Tray zurueck (Doppelklick / „Oeffnen") und blendet das Symbol aus.</summary>
        private void RestoreFromTray()
        {
            try
            {
                Show();
                WindowState = WindowState.Normal;
                Activate();
                Topmost = true; Topmost = false;   // kurz nach vorne holen, ohne dauerhaft Topmost zu sein
                _tray?.Hide();
            }
            catch (Exception ex) { Log.Error("Fenster aus Tray wiederherstellen fehlgeschlagen", ex); }
        }

        /// <summary>„Beenden" aus dem Tray-Menue: App wirklich schliessen (Fenster ist evtl. versteckt).</summary>
        private void ExitFromTray()
        {
            try { Show(); Close(); } catch (Exception ex) { Log.Error("Beenden aus Tray fehlgeschlagen", ex); }
        }

        /// <summary>
        /// Startet die App unsichtbar im Infobereich-Symbol (Tray) — fuer den Windows-Autostart
        /// (Schalter <c>--minimized</c>, siehe <see cref="Services.AutostartManager"/>). Anders als
        /// beim manuellen Minimieren geht es hier IMMER ins Tray (unabhaengig von „Beim Minimieren"),
        /// weil genau das der Zweck des Hintergrund-Autostarts ist. Das Tray-Symbol ist die
        /// Rueckhol-Moeglichkeit (Doppelklick).
        ///
        /// Live-Logik-Sonde (Intent-Verifikation): protokolliert „erwartet vs. tatsaechlich", damit
        /// live bestaetigbar ist, dass der Autostart wirklich im Hintergrund landet.
        /// </summary>
        private void StartHiddenInTray()
        {
            try
            {
                EnsureTray();
                _tray?.Show(_trayHintShown ? null : "VoiceAgent läuft im Hintergrund. Doppelklick zum Öffnen.");
                _trayHintShown = true;
                Hide();   // aus der Taskleiste nehmen; App laeuft im Hintergrund weiter
                Log.Info("CHECKPOINT Autostart minimiert ins Tray", new
                {
                    step = "Autostart ins Tray",
                    intent = "App startet beim Windows-Start unsichtbar im Hintergrund (nur Tray-Symbol)",
                    expected = "Fenster versteckt, Tray-Symbol sichtbar",
                    actual = "Fenster versteckt, Tray-Symbol sichtbar",
                    ok = true
                });
            }
            catch (Exception ex) { Log.Error("Autostart: minimiert ins Tray fehlgeschlagen (App laeuft weiter)", ex); }
        }

        // ---------- Taskleisten-Icon (ICON_BIG zuverlaessig setzen) ----------

        private const int WM_SETICON = 0x0080;
        private const int ICON_SMALL = 0;
        private const int ICON_BIG = 1;

        [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
        private static extern int ExtractIconEx(string lpszFile, int nIconIndex,
            out IntPtr phiconLarge, out IntPtr phiconSmall, uint nIcons);

        [DllImport("user32.dll", CharSet = CharSet.Unicode)]
        private static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll")]
        private static extern bool DestroyIcon(IntPtr hIcon);

        private IntPtr _taskbarIconBig;     // selbst gesetzte Icons — am Ende per DestroyIcon freigeben (GDI-Hygiene, Almanach §9.4)
        private IntPtr _taskbarIconSmall;

        private void OnSourceInitialized(object? sender, EventArgs e)
        {
            // Titelleiste passend zum aktiven Theme einfaerben (Almanach §2.7 — erst bei vorhandenem
            // HWND). Intern abgesichert, daher zuerst und unabhaengig vom Icon-Block.
            ThemeManager.ApplyTitleBar(this);
            ApplyTaskbarIcon("SourceInitialized");
        }

        /// <summary>
        /// Re-Apply des Taskleisten-Icons NACH dem ersten Rendern — erst dann existiert der
        /// Taskleisten-Button. Auf Windows 11 wird die Taskleiste frueher initialisiert und cached ihr
        /// Default-Icon, wenn der Start laenger beschaeftigt ist (OnLoaded laedt Settings/Memory/
        /// Subagenten); das in SourceInitialized gesetzte Icon wird dann von der Taskleiste ueberschrieben
        /// (dotnet/wpf #11308, #11222). Darum hier erneut setzen, plus ein Idle-Re-Apply als
        /// Sicherheitsnetz, falls der UI-Thread beim Rendern noch belegt war.
        /// </summary>
        private void OnContentRendered(object? sender, EventArgs e)
        {
            ApplyTaskbarIcon("ContentRendered");
            Dispatcher.BeginInvoke(DispatcherPriority.ApplicationIdle,
                new Action(() => ApplyTaskbarIcon("ApplicationIdle")));
        }

        /// <summary>
        /// Setzt das Taskleisten-Icon (ICON_BIG + ICON_SMALL) explizit per WM_SETICON aus dem
        /// eingebetteten Exe-Icon (<c>&lt;ApplicationIcon&gt;</c>, via <see cref="ExtractIconEx"/> —
        /// genau das Icon, das angepinnt bereits stimmt). <c>Environment.ProcessPath</c> ist
        /// single-file-sicher (Almanach §1.1).
        ///
        /// Root Cause (bug-almanac dotnet-csharp §6.6, 2026-06-08): WPFs <c>Window.Icon</c> belegt nur
        /// den ICON_SMALL-Slot (Titel/Vorschau) zuverlaessig; den ICON_BIG-Slot der Taskleiste nicht.
        /// Zusaetzlich cached die Win11-Taskleiste ihr Default-Icon, wenn das Setzen zu frueh passiert —
        /// darum wird diese Methode mehrfach aufgerufen (SourceInitialized + ContentRendered + Idle).
        /// Idempotent: vorher gesetzte HICONs werden vor dem Neusetzen per DestroyIcon freigegeben (§9.4).
        /// </summary>
        private void ApplyTaskbarIcon(string source)
        {
            try
            {
                string? exe = Environment.ProcessPath;
                Probe.That(!string.IsNullOrEmpty(exe),
                    "Taskleisten-Icon: Environment.ProcessPath leer", new { exe, source });
                if (string.IsNullOrEmpty(exe)) return;

                IntPtr hwnd = new WindowInteropHelper(this).Handle;
                if (hwnd == IntPtr.Zero) return;

                int count = ExtractIconEx(exe, 0, out IntPtr hBig, out IntPtr hSmall, 1);

                // Vorher selbst gesetzte HICONs freigeben (GDI-Hygiene bei wiederholtem Apply, §9.4).
                if (_taskbarIconBig != IntPtr.Zero && _taskbarIconBig != hBig) DestroyIcon(_taskbarIconBig);
                if (_taskbarIconSmall != IntPtr.Zero && _taskbarIconSmall != hSmall) DestroyIcon(_taskbarIconSmall);

                if (hBig != IntPtr.Zero)
                {
                    SendMessage(hwnd, WM_SETICON, (IntPtr)ICON_BIG, hBig);
                    _taskbarIconBig = hBig;
                }
                if (hSmall != IntPtr.Zero)
                {
                    SendMessage(hwnd, WM_SETICON, (IntPtr)ICON_SMALL, hSmall);
                    _taskbarIconSmall = hSmall;
                }

                // Live-Logik-Sonde (Intent: "App-Symbol in der Taskleiste sichtbar, nicht generisch").
                Log.Info("CHECKPOINT Taskleisten-Icon gesetzt", new
                {
                    step = "Taskleisten-Icon setzen",
                    intent = "App-Symbol in der Taskleiste sichtbar (nicht generisch)",
                    expected = "grosses Icon (ICON_BIG) gesetzt",
                    source,
                    big = hBig != IntPtr.Zero,
                    small = hSmall != IntPtr.Zero,
                    ok = hBig != IntPtr.Zero,
                    extractedGroups = count,
                    exe
                });
                if (hBig == IntPtr.Zero)
                    Log.Error("Taskleisten-Icon: ExtractIconEx lieferte kein grosses Icon — Taskleiste bleibt generisch",
                        null, new { exe, source, extractedGroups = count });
            }
            catch (Exception ex)
            {
                // Funktionserhaltend: schlaegt das explizite Setzen fehl, bleibt das WPF-Window.Icon aktiv (kein Crash).
                Log.Error("Taskleisten-Icon konnte nicht gesetzt werden (Window.Icon bleibt aktiv)", ex, new { source });
            }
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            try
            {
                Log.Info("VoiceAgent gestartet", new { version = AppVersion });   // Version ins Log (Observability)
                _settings = Config.Load();
                ApplyChimeSounds();            // gewaehlte Start-/Stop-/Melde-Toene auf die Chimes uebernehmen
                _memory = new AgentMemory();   // laedt Fakten + letzte Gespraeche aus frueheren Sessions
                _subAgents = new SubAgentRegistry();
                _reminderService = new ReminderService();
                _subAgents.Register(new ReminderSubAgent(_reminderService));   // Erinnerungen (zeitgesteuert)
                _subAgents.Register(new NoteSubAgent(_memory));                // Notizen

                // Benutzerdefinierte Helfer (per Sprache angelegt) laden + registrieren — bleiben ueber Neustarts erhalten.
                _customAgents = new CustomAgentStore();
                foreach (var def in _customAgents.All)
                    _subAgents.Register(new PromptSubAgent(def, LlmProviderFactory.Create(_settings)));
                // Der "Helfer-Baumeister" legt zur Laufzeit neue Helfer an (Vision: dynamisch eigene Unteragenten bauen).
                // Modell pro Rolle: Agenten-Bau laeuft auf der Builder-Rolle (Default: Codex), in den Einstellungen waehlbar.
                _subAgents.Register(new AgentBuilderSubAgent(
                    LlmProviderFactory.CreateForRole(_settings, ModelRole.Builder),            // Builder-Rolle: Definition ableiten
                    () => LlmProviderFactory.Create(_settings),                                // Haupt-Provider fuer den neuen Helfer
                    _subAgents, _customAgents));
                // Computer Use: steuert auf Wunsch den Rechner. Stufe (Aus/Sicher/Vollzugriff) wird LIVE aus
                // den Einstellungen gelesen; der CommandGuard entscheidet, der ShellExecutor fuehrt aus.
                _subAgents.Register(new ComputerUseSubAgent(
                    LlmProviderFactory.CreateForRole(_settings, ModelRole.ComputerUse),        // Rolle: Befehl ableiten
                    () => CommandGuard.ParseMode(_settings.ComputerUseMode),                   // aktuelle Stufe (live)
                    (cmd, token) => _shell.RunAsync(cmd, 20000, token)));                      // Ausfuehrung

                _sessionStore = new SessionStore();
                _sessions = new SessionManager(_sessionStore);
                _sessions.ActiveChanged += RefreshSessionList;
                _sessions.NewSession();                 // Start: frische, leere Session
                _compressor = new ContextCompressor();  // Default-Budget/-Schwelle

                BuildAgents();

                RebuildListener();

                MicToggle.IsChecked = !_settings.WakeWordEnabled && _settings.MicEnabled;  // Weckwort-Modus startet im Ruhezustand
                UpdateMicLabel();
                StartClock();
                Log.Info("MainWindow geladen — Voice-Loop bereit.");
                Append("System", "Bereit. Sprich einfach los, Gedankenpausen sind erlaubt. Oder tippe unten.");

                _ready = true;            // ab jetzt duerfen die Sidebar-Handler feuern
                RefreshSessionList();
                RefreshContextMeter();
                UpdateThemeButton();      // Sonne/Mond passend zum (in App.OnStartup gesetzten) Theme

                // Windows-Autostart (--minimized): das Hauptfenster direkt unsichtbar ins Tray
                // nehmen, sobald die Einstellungen geladen sind und das Tray-Symbol bereit ist.
                if (App.StartMinimizedToTray) StartHiddenInTray();

                // Proaktive Start-Erinnerungen ("erinnere mich beim naechsten Start" / ohne Uhrzeit):
                // ERST wenn die App wirklich vollstaendig geladen+gerendert ist (ApplicationIdle =
                // Dispatcher-Queue leer), dann Sound + Nachricht (Frank-Wunsch 2026-06-08).
                Dispatcher.InvokeAsync(CheckStartReminders, System.Windows.Threading.DispatcherPriority.ApplicationIdle);
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Initialisierung fehlgeschlagen", ex);
                Append("System", "Initialisierung fehlgeschlagen: " + ex.Message);
            }
        }

        private void BuildAgents()
        {
            _provider = LlmProviderFactory.Create(_settings);   // Sprechen-Rolle: die gesprochene Antwort
            // LLM-Helfer-Auswahl auf der Intent-Rolle; null => nur Stichwort-Matching.
            var router = _settings.SubAgentRouting
                ? new SubAgentRouter(LlmProviderFactory.CreateForRole(_settings, ModelRole.Intent))
                : null;
            // WICHTIG (Settings-Fix): die AKTIVE Session wird hereingereicht — beim erneuten BuildAgents
            // (z.B. nach dem Speichern der Einstellungen) bleibt derselbe Verlauf erhalten.
            _agent = new BossAgent(_provider, _settings.SystemPrompt, _memory, _subAgents, _settings.TimeZoneId, _sessions?.Active, router);
            // Endpunkt-Check laeuft auf der Endpunkt-Rolle (Default: guenstiges, schnelles Gemini-Modell).
            _endpoint = new EndpointDetector(LlmProviderFactory.CreateForRole(_settings, ModelRole.Endpoint));
            _intentDetector = new IntentDetector(LlmProviderFactory.CreateForRole(_settings, ModelRole.Intent));
            // Tiefes Verstehen: das Brain-Modell (Verstehens/Orchestrier-Rolle, STARKES Modell) deutet
            // jede Aussage strukturiert, BEVOR geroutet wird. Aus/Fehler => alte Pipeline (verlustfrei).
            _understanding = _settings.DeepUnderstanding
                ? new UnderstandingDetector(LlmProviderFactory.CreateForRole(_settings, ModelRole.Brain), _settings.TimeZoneId)
                : null;
            _tts = new GoogleTtsClient(Config.ReadApiKey("google"));
            _stt = new GroqWhisperClient(Config.ReadApiKey("groq"), _settings.SttModel, _settings.SttLanguage,
                                         url: null, voicedRmsThreshold: _settings.SilenceThreshold);
        }

        /// <summary>
        /// Erstellt den Mikrofon-Listener mit den aktuellen Empfindlichkeits-Werten neu.
        /// Wird beim Laden UND nach dem Speichern der Einstellungen aufgerufen, damit geaenderte
        /// Schieberegler-Werte (Stille-Schwelle/Pausendauer) sofort greifen — nicht erst beim Neustart.
        /// </summary>
        private void RebuildListener()
        {
            try { _listener?.Dispose(); }
            catch (Exception ex) { Log.Error("MainWindow: alten Listener schliessen fehlgeschlagen", ex); }
            _listener = new AlwaysOnListener(_settings.SilenceThreshold, _settings.SilenceMs, _settings.MinUtteranceMs,
                _settings.MinVoicedMs, _settings.MinVoicedRatio, _settings.MaxUtteranceMs);
            _listener.OnUtterance += OnUtterance;
            _listener.OnFrame += OnAudioFrame;       // roher Stream fuer das Weckwort-Lauschen
            _listener.OnSpeechStart += OnSpeechStart; // zeitgenaue Live-Statusanzeige beim Sprechbeginn
            _listener.Start();
            RebuildWakeController();
            // Im Weckwort-Modus laeuft die Aufnahme IMMER (Hintergrund-Lauschen, auch im Ruhezustand);
            // ohne Weckwort steuert der Mikrofon-Schalter die Aufnahme wie bisher.
            _listener.SetEnabled(_settings.WakeWordEnabled || _settings.MicEnabled);
        }

        /// <summary>
        /// (Re)baut den Weckwort-Zustandsautomaten anhand der Einstellungen. Schlaegt die
        /// Initialisierung fehl (Modell/Native), wird das Feature deaktiviert und die App
        /// laeuft im normalen Modus weiter (Funktionserhalt, Direktive #3) — kein Crash.
        /// </summary>
        private void RebuildWakeController()
        {
            if (_wake != null) { _wake.Woke -= OnWoke; _wake.Slept -= OnSlept; try { _wake.Dispose(); } catch { } _wake = null; }
            if (!_settings.WakeWordEnabled) return;
            try
            {
                string modelDir = Path.Combine(AppContext.BaseDirectory, "assets", "wakeword-model");
                // keywords.txt aus dem GEWAEHLTEN Weckwort erzeugen (BPE-tokenisiert), damit die
                // Erkennung dem Einstellungs-Weckwort folgt — vorher war keywords.txt fest "Okay Computer".
                string keywordsFile = WakeWords.EnsureKeywordsFile(_settings.WakeWord, modelDir);
                var engine = new SherpaWakeWordEngine(modelDir, 1, keywordsFile);
                var vad = new EnergyVadGate(_settings.SilenceThreshold);
                _wake = new WakeWordController(engine, vad, _settings.WakeTimeoutMs);
                _wake.Woke += OnWoke;
                _wake.Slept += OnSlept;
                Log.Info("Weckwort aktiv — Agent ruht, bis das Weckwort faellt", new { _settings.WakeWord });
                Append("System", $"Weckwort aktiv: sag „{_settings.WakeWord}“, um mich zu wecken.");
            }
            catch (Exception ex)
            {
                Log.Error("Weckwort-Initialisierung fehlgeschlagen — Feature aus, App laeuft normal weiter", ex);
                _wake = null;
                Append("System", "Weckwort konnte nicht geladen werden, ich verarbeite wie gewohnt jede Aussage.");
            }
        }

        // ---------- Weckwort: roher Audio-Stream (Listener-Thread) ----------

        private void OnAudioFrame(byte[] buffer, int count)
        {
            var wake = _wake;
            if (wake == null) return;
            try
            {
                // KWS ist leichtgewichtig (int8-3.3M); synchron im Audio-Thread ok. Bei Bedarf
                // spaeter ueber einen bounded Channel<T> entkoppeln (Best-Practice §3).
                float[] samples = PcmConverter.Pcm16ToFloat(buffer, count);
                wake.ProcessFrame(samples);   // bei Treffer feuert Woke (-> Dispatcher)
            }
            catch (Exception ex) { Log.Error("Weckwort: Frame-Verarbeitung fehlgeschlagen", ex); }
        }

        /// <summary>
        /// Zeitgenaue Live-Statusanzeige: SOFORT beim Sprechbeginn "Hoert zu" (nicht erst nach dem
        /// 3-s-Stille-Ende, wie es vorher passierte). Feuert vom Audio-Thread -> auf den UI-Thread
        /// marshallen (Wake-Almanach Bug 7). Nicht im Ruhezustand und nicht waehrend einer laufenden
        /// Verarbeitung anzeigen (sonst falsche/ueberschriebene Statuszeile).
        /// </summary>
        private void OnSpeechStart() => Dispatcher.InvokeAsync(() =>
        {
            // Wachfenster-Lebenszyklus (Voice-Pipeline-BP, Alexa/Google-Muster): Sprechbeginn ist
            // Aktivitaet — das Fenster wird verlaengert, BEVOR irgendein Guard greift. Zusaetzlich
            // merken wir uns pro Aufnahme, ob sie im WACHEN Zustand begann: nur dann darf sie
            // nach einem zwischenzeitlichen Timeout trotzdem verarbeitet werden (kein stilles
            // Wegwerfen von Franks Rede — der belegte Bug vom 2026-06-10, 41,8-s-Aufnahme).
            if (_settings.WakeWordEnabled && _wake != null)
            {
                _speechBeganAwake = _wake.State == WakeState.Awake;   // pro Aufnahme neu bewertet
                if (_speechBeganAwake) _wake.NotifyActivity();
            }
            if (_busy) return;   // gerade Transkription/Antwort -> Statuszeile nicht ueberschreiben
            // Im Weckwort-Ruhezustand hoert der Agent NICHT zu (er wartet aufs Weckwort).
            if (_settings.WakeWordEnabled && _wake != null && _wake.State == WakeState.Sleeping) return;
            SetStatus("Hört zu …");
        });

        private void OnWoke(string keyword) => Dispatcher.InvokeAsync(() => _ = OnWokeAsync(keyword));

        private async Task OnWokeAsync(string keyword)
        {
            try
            {
                MicToggle.IsChecked = true;   // Schalter zeigt: jetzt wach
                UpdateMicLabel();
                // Observability-Checkpoint (Intent-Verifikation): erwartet vs. tatsaechlich.
                Log.Info("CHECKPOINT: Weckwort erkannt",
                    new { step = "Weckwort erkannt", expected = _settings.WakeWord, actual = keyword, ok = true });
                SetStatus("Ton kommt, gleich sprechen");
                // NUR ein kurzer Erkennungston (KEIN gesprochenes Wort — Frank-Wunsch 2026-06-08).
                // Waehrend des Tons nicht mithoeren; danach den Buffer leeren (Weckwort-Phrase + Ton
                // verwerfen) und ab JETZT sofort normal hoeren — exakt wie ein manueller Knopfdruck.
                PauseMic();
                if (_settings.WakeChimeEnabled) _wakeChime.Play();
                await Task.Delay(650);          // Piepton ausklingen lassen
                _listener?.ClearBuffer();       // "Okay Computer" + Ton verwerfen -> frischer Start
                ResumeMic();                    // ab JETZT hoeren -> sofort lossprechen
                SetStatus("Wach, sprich jetzt");
            }
            catch (Exception ex) { Log.Error("Weckwort-Reaktion fehlgeschlagen", ex); }
        }

        private void OnSlept(string reason) => Dispatcher.InvokeAsync(() =>
        {
            MicToggle.IsChecked = false;   // Schalter zeigt: ruht wieder
            UpdateMicLabel();
            SetStatus($"Ruhe, sag „{_settings.WakeWord}“");
            Append("System", "(Ich ruhe wieder, sag das Weckwort oder klick das Mikrofon an.)");

            // Abfallender Ton GENAU beim automatischen Stille-Timeout: hoerbares Signal, dass der
            // Agent ab jetzt nicht mehr zuhoert und erst wieder geweckt werden muss (Frank-Wunsch
            // 2026-06-08). NICHT beim manuellen Abschalten (reason "manuell") — das hat Frank selbst
            // ausgeloest und braucht keinen Ton. "==" auf string ist ordinal (sicher, Almanach §11.6).
            bool byTimeout = reason == "Timeout";
            bool play = byTimeout && _settings.WakeSleepChimeEnabled;
            // Observability-Checkpoint (Intent-Verifikation, Wake-BP §8): erwartet vs. tatsaechlich.
            Log.Info("CHECKPOINT: Wachfenster geschlossen",
                new { step = "Einschlafton beim Timeout", reason, expected = "Ton nur bei Timeout", played = play, ok = true });
            if (play) _sleepChime.Play();
        });

        private void OnClosed(object? sender, EventArgs e)
        {
            try { _clockTimer?.Stop(); _reminderPingTimer?.Stop(); _proactivePingTimer?.Stop(); _followupTimer?.Stop(); CancelSafetyTimer(); _wake?.Dispose(); _wakeChime.Stop(); _sleepChime.Stop(); _listener?.Dispose(); _player.Stop(); _tray?.Dispose(); }
            catch (Exception ex) { Log.Error("MainWindow: Aufraeumen-Fehler", ex); }
            ThemeManager.Changed -= UpdateThemeButton;   // Event-Abo loesen (Leak-Vermeidung, Almanach §9.1)
            // Selbst gesetzte Taskleisten-Icons freigeben (GDI-Handle-Limit, Almanach §9.4).
            try { if (_taskbarIconBig != IntPtr.Zero) DestroyIcon(_taskbarIconBig); if (_taskbarIconSmall != IntPtr.Zero) DestroyIcon(_taskbarIconSmall); }
            catch (Exception ex) { Log.Error("MainWindow: Icon-Freigabe-Fehler", ex); }
        }

        // ---------- Mikrofon-Schalter ----------

        private void MicToggle_Click(object sender, RoutedEventArgs e)
        {
            bool on = MicToggle.IsChecked == true;
            if (_settings.WakeWordEnabled && _wake != null)
            {
                // Weckwort-Modus: der Schalter weckt MANUELL (an) bzw. schickt zurueck in Ruhe (aus).
                // Das ist der zuverlaessige Direkt-Pfad — funktioniert auch, wenn das Weckwort mal
                // nicht anspringt. Die Aufnahme laeuft weiter (Hintergrund-Lauschen), wird NIE abgeschaltet.
                if (on) _wake.ForceAwake(); else _wake.ForceSleep();
                _listener?.SetEnabled(true);
                UpdateMicLabel();
                if (on) { SetStatus("Wach, sprich einfach los"); Append("System", "(Mikrofon an, ich höre zu.)"); }
                return;
            }
            _settings.MicEnabled = on;
            _listener?.SetEnabled(on);
            Config.Save(_settings);
            UpdateMicLabel();
        }

        private void UpdateMicLabel()
        {
            if (_settings.WakeWordEnabled)
                // Zeigt das TATSAECHLICH eingestellte Weckwort (nicht hartcodiert), Komma statt Gedankenstrich.
                MicToggle.Content = (_wake?.State == WakeState.Awake) ? "🎙 Wach, hört zu" : $"🎙 Ruht, sag „{_settings.WakeWord}“";
            else
                MicToggle.Content = _settings.MicEnabled ? "🎙 Mikrofon: an" : "🎙 Mikrofon: aus";
        }

        private void StartClock()
        {
            _clockTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
            _clockTimer.Tick += (_, __) =>
            {
                UpdateClock(); CheckReminders(); CheckProactive();
                // Wachfenster-Lebenszyklus (Voice-Pipeline-BP, Alexa/Google: Timer zaehlt ab
                // Antwort-ENDE und NUR im Leerlauf): solange Frank spricht (laufende Aufnahme)
                // oder der Agent arbeitet/antwortet (_busy), gilt das als Aktivitaet — der
                // 60-s-Countdown startet erst danach neu. Vorher schlief der Agent mitten in
                // Franks Rede ein und die fertige Aufnahme wurde verworfen (Bug 2026-06-10).
                if (_busy || (_listener?.IsCapturing ?? false)) _wake?.NotifyActivity();
                _wake?.Tick();
            };
            _clockTimer.Start();
            UpdateClock();
        }

        /// <summary>Zeigt die Uhr in derselben Zeitzone, die auch der Agent kennt (konsistent).</summary>
        private void UpdateClock()
        {
            try
            {
                var zone = TimeContext.ResolveZone(_settings.TimeZoneId);
                var now = TimeZoneInfo.ConvertTime(DateTimeOffset.Now, zone);
                ClockText.Text = now.ToString("ddd, dd.MM.yyyy  HH:mm:ss", new System.Globalization.CultureInfo("de-DE"));
            }
            catch { /* Uhr ist unkritisch — nie crashen */ }
        }

        /// <summary>Prueft jede Sekunde, ob eine Erinnerung faellig ist (nur wenn gerade nichts laeuft).</summary>
        /// <summary>Sammelt faellige Erinnerungen und stoesst den Ping-Vorgang an (jede Sekunde aufgerufen).</summary>
        private void CheckReminders()
        {
            if (_reminderService == null) return;
            try
            {
                foreach (var r in _reminderService.TakeDue(DateTimeOffset.Now)) _reminderQueue.Enqueue(r);
                TryStartReminderPing();
            }
            catch (Exception ex) { Log.Error("CheckReminders fehlgeschlagen", ex); }
        }

        /// <summary>
        /// Beim vollstaendigen App-Start einmalig: "beim-naechsten-Start"-Erinnerungen (ohne Uhrzeit)
        /// faellig stellen und proaktiv melden — Sound + Nachricht ueber dieselbe Ping-Mechanik wie
        /// zeitgesteuerte Erinnerungen. Frank wird so an Offenes erinnert, sobald die App hochgefahren ist.
        /// </summary>
        private void CheckStartReminders()
        {
            if (_reminderService == null) return;
            try
            {
                var due = _reminderService.TakeStartReminders();
                if (due.Count == 0) return;
                foreach (var r in due) _reminderQueue.Enqueue(r);
                // Observability-Checkpoint (Intent-Verifikation): erwartet vs. tatsaechlich.
                Log.Info("CHECKPOINT: Start-Erinnerungen faellig",
                    new { step = "Start-Erinnerung beim App-Start", count = due.Count, expected = "Sound + Ansage beim vollstaendigen Start", ok = true });
                TryStartReminderPing();
            }
            catch (Exception ex) { Log.Error("CheckStartReminders fehlgeschlagen", ex); }
        }

        /// <summary>
        /// Zwei-Schritt-Erinnerung Schritt 1: Sound ertoenen lassen und so lange jede Minute
        /// WIEDERHOLEN, bis Frank reagiert. KEINE Ansage, Mikrofon bleibt an (Frank soll antworten).
        /// Die Ansage (Schritt 2) kommt erst, wenn Frank etwas sagt (siehe AnnounceReminderAsync).
        /// </summary>
        private void TryStartReminderPing()
        {
            if (_pendingReminder != null || _reminderQueue.Count == 0 || _busy) return;
            _pendingReminder = _reminderQueue.Dequeue();
            Log.Info("Erinnerung faellig — Ping startet, warte auf Reaktion", new { _pendingReminder.Text });
            Append("System", "(Erinnerung fällig, sag kurz Bescheid, dann sage ich dir, worum es geht.)");
            StartPinging();
        }

        private void StartPinging()
        {
            _chime.Play();   // sofort einmal
            _reminderPingTimer ??= new DispatcherTimer { Interval = TimeSpan.FromMinutes(1) };
            _reminderPingTimer.Tick -= OnReminderPing;   // doppelte Registrierung vermeiden
            _reminderPingTimer.Tick += OnReminderPing;
            _reminderPingTimer.Start();
        }

        private void OnReminderPing(object? sender, EventArgs e)
        {
            if (_pendingReminder == null) { StopPinging(); return; }
            _chime.Play();   // alle 60 s wiederholen, bis Frank reagiert (falls er den Sound verpasst hat)
        }

        private void StopPinging() => _reminderPingTimer?.Stop();

        /// <summary>Schritt 2: Frank hat reagiert — jetzt erst die Erinnerung vorlesen, dann ggf. die naechste.</summary>
        private async Task AnnounceReminderAsync(Reminder r)
        {
            _busy = true;
            PauseMic();
            try
            {
                var msg = $"Du wolltest, dass ich dich erinnere: {r.Text}";
                Append("Agent", msg);
                Log.Info("Erinnerung bestaetigt — Ansage", new { r.Text });
                await SpeakAsync(msg);
            }
            catch (Exception ex) { Log.Error("Erinnerungs-Ansage fehlgeschlagen", ex); }
            finally
            {
                _busy = false;
                ResumeMic();
                TryStartReminderPing();   // falls weitere Erinnerungen warten
            }
        }

        // ---------- Generischer proaktiver Funkspruch (Manifest: Agent meldet sich von selbst) ----------

        /// <summary>
        /// Holt — jede Sekunde — die naechste proaktive Meldung aus dem Kanal und startet den
        /// Funkspruch (Sound, dann warten auf Franks Reaktion). Erinnerungen haben Vorrang; wird
        /// nur aktiv, wenn gerade nichts laeuft (kein Reminder/keine Meldung pending, nicht busy).
        /// </summary>
        private void CheckProactive()
        {
            try
            {
                if (_pendingProactive != null || _pendingReminder != null || _busy || _reminderQueue.Count > 0) return;
                if (!_proactive.TryTake(out var msg)) return;
                _pendingProactive = msg;
                Log.Info("Proaktive Meldung faellig — Funkspruch startet, warte auf Reaktion", new { msg.Reason, msg.Text });
                Append("System", "(Der Agent möchte dir etwas sagen, sag kurz Bescheid.)");
                StartProactivePing();
            }
            catch (Exception ex) { Log.Error("CheckProactive fehlgeschlagen", ex); }
        }

        private void StartProactivePing()
        {
            _chime.Play();   // Funkspruch-Sound sofort
            _proactivePingTimer ??= new DispatcherTimer { Interval = TimeSpan.FromMinutes(1) };
            _proactivePingTimer.Tick -= OnProactivePing;
            _proactivePingTimer.Tick += OnProactivePing;
            _proactivePingTimer.Start();
        }

        private void OnProactivePing(object? sender, EventArgs e)
        {
            if (_pendingProactive == null) { _proactivePingTimer?.Stop(); return; }
            _chime.Play();   // alle 60 s wiederholen, bis Frank reagiert
        }

        /// <summary>Frank hat auf den Funkspruch reagiert — jetzt die Meldung ansagen.</summary>
        private async Task AnnounceProactiveAsync(ProactiveMessage m)
        {
            _busy = true;
            PauseMic();
            try
            {
                Append("Agent", m.Text);
                Log.Info("Proaktive Meldung — Ansage", new { m.Reason, m.Text });
                await SpeakAsync(m.Text);
            }
            catch (Exception ex) { Log.Error("Proaktive Ansage fehlgeschlagen", ex); }
            finally
            {
                _busy = false;
                ResumeMic();
                CheckProactive();   // falls weitere Meldungen warten
            }
        }

        /// <summary>
        /// Erster echter Producer des Funkspruch-Kanals: Hat der Agent eine Verstaendnis-Rueckfrage
        /// gestellt und Frank reagiert ~45 s nicht, meldet sich der Agent EINMAL proaktiv und fragt
        /// nach. Verbindet die Rueckfrage-Bremse mit dem proaktiven Kanal.
        /// </summary>
        private void ArmFollowupIfPending()
        {
            _followupTimer?.Stop();
            if (_agent?.HasPendingConfirmation != true) return;
            _followupTimer ??= new DispatcherTimer { Interval = TimeSpan.FromSeconds(45) };
            _followupTimer.Tick -= OnFollowupTick;
            _followupTimer.Tick += OnFollowupTick;
            _followupTimer.Start();
        }

        private void OnFollowupTick(object? sender, EventArgs e)
        {
            _followupTimer?.Stop();
            if (_agent?.HasPendingConfirmation == true && !_busy
                && _pendingProactive == null && _pendingReminder == null)
            {
                _proactive.Post("Wegen vorhin, soll ich das noch erledigen, oder sollen wir es lassen?", "followup-rueckfrage");
            }
        }

        // ---------- Hell/Dunkel-Profil ----------

        /// <summary>Topbar-Schalter: wechselt zwischen hellem und dunklem Profil und merkt es sich.</summary>
        private void ThemeButton_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                _settings.Theme = ThemeManager.Toggle();   // faerbt ALLE Fenster um, liefert "light"/"dark"
                Config.Save(_settings);
                Log.Info("Theme per Topbar-Schalter gewechselt", new { _settings.Theme });
            }
            catch (Exception ex) { Log.Error("Theme-Umschalten fehlgeschlagen", ex); }
        }

        /// <summary>Zeigt das passende Symbol: Mond bietet Wechsel zu dunkel an, Sonne zu hell.</summary>
        private void UpdateThemeButton()
        {
            try
            {
                ThemeButton.Content = ThemeManager.IsDark ? "☀" : "🌙";
                ThemeButton.ToolTip = ThemeManager.IsDark ? "Zum hellen Profil wechseln" : "Zum dunklen Profil wechseln";
            }
            catch (Exception ex) { Log.Error("Theme-Symbol aktualisieren fehlgeschlagen", ex); }
        }

        private void LogButton_Click(object sender, RoutedEventArgs e)
        {
            // Nicht-modal (Show, nicht ShowDialog): Frank kann gleichzeitig sprechen und mitlesen.
            try { new Views.LogViewerWindow { Owner = this }.Show(); }
            catch (Exception ex) { Log.Error("MainWindow: Log-Viewer oeffnen fehlgeschlagen", ex); }
        }

        // Uebernimmt die in den Einstellungen gewaehlten Start-/Stop-Toene auf die Chime-Instanzen.
        // Leerer Wert -> bisheriger Standard. Wird beim Laden und nach dem Speichern aufgerufen.
        private void ApplyChimeSounds()
        {
            _wakeChime.FileName = string.IsNullOrWhiteSpace(_settings.WakeChimeSound) ? "wakeword.wav" : _settings.WakeChimeSound;
            _sleepChime.FileName = string.IsNullOrWhiteSpace(_settings.WakeSleepChimeSound) ? "sleep.wav" : _settings.WakeSleepChimeSound;
            _chime.FileName = string.IsNullOrWhiteSpace(_settings.ChimeSound) ? "message1.wav" : _settings.ChimeSound;
        }

        private void SettingsButton_Click(object sender, RoutedEventArgs e)
        {
            // Auch das OEFFNEN in try/catch: ein Fehler im Settings-Fenster darf die App nie killen.
            try
            {
                var dlg = new Views.SettingsWindow(_settings) { Owner = this };
                if (dlg.ShowDialog() == true)
                {
                    _settings = Config.Load();
                    ApplyChimeSounds();                    // evtl. neu gewaehlte Start-/Stop-Toene sofort uebernehmen
                    ThemeManager.Apply(_settings.Theme);   // evtl. im Dialog gewaehltes Profil sofort anwenden
                    BuildAgents();
                    RebuildListener();   // geaenderte Empfindlichkeit sofort uebernehmen
                    MicToggle.IsChecked = _settings.MicEnabled;
                    UpdateMicLabel();
                    Append("System", "Einstellungen gespeichert und uebernommen.");
                }
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Einstellungen oeffnen/uebernehmen fehlgeschlagen", ex);
                Append("Fehler", "Einstellungen konnten nicht geoeffnet werden: " + ex.Message);
                SetStatus("Fehler, siehe Log");
            }
        }

        // ---------- Sprach-Eingang (vom Listener-Thread) ----------

        private void OnUtterance(string wavPath) => Dispatcher.InvokeAsync(() => _ = HandleUtteranceAsync(wavPath));

        private async Task HandleUtteranceAsync(string wavPath)
        {
            CancelSafetyTimer();   // neues Haeppchen → Sicherheitsnetz zuruecksetzen
            if (_busy || _stt == null) { TryDelete(wavPath); return; }

            // Weckwort-Gating: im Ruhezustand wird die Aussage NICHT verarbeitet (das Wecken
            // laeuft kontinuierlich ueber OnAudioFrame). Direkt nach dem Wecken ist die erste
            // Aussage die Weckwort-Phrase selbst — die wird uebersprungen (Zwei-Phasen-Modell:
            // erst wecken, dann die Anfrage sprechen). Im wachen Zustand: Aktivitaet melden.
            if (_settings.WakeWordEnabled && _wake != null)
            {
                // Ruht -> Aussage ignorieren (Wecken laeuft ueber das Hintergrund-Lauschen).
                // AUSNAHME (Defense in Depth, Voice-Pipeline-BP): begann die Aufnahme, als der
                // Agent noch WACH war, wird sie auch nach einem zwischenzeitlichen Fenster-Timeout
                // verarbeitet — Franks angefangene Rede darf NIE still im Muell landen (belegter
                // Bug 2026-06-10). Der Agent wacht dafuer wieder auf (Gespraech laeuft ja weiter).
                if (_wake.State == WakeState.Sleeping)
                {
                    if (!_speechBeganAwake) { TryDelete(wavPath); return; }
                    Log.Info("Wachfenster: Aussage begann im wachen Zustand — wird trotz Timeout verarbeitet (kein stilles Verwerfen)");
                    _wake.ForceAwake();
                    UpdateMicLabel();
                }
                _speechBeganAwake = false;   // verbraucht — naechste Aufnahme bewertet neu
                _wake.NotifyActivity();
            }

            _busy = true;
            PauseMic();   // waehrend Transkription/Check/Sprechen nicht mithoeren
            try
            {
                // "Hoert zu" zeigt jetzt OnSpeechStart live waehrend des Sprechens an; ab hier wird
                // die fertige Aufnahme in Text umgewandelt -> der naechste echte Schritt.
                SetStatus("Schreibe mit …");
                string segment = await _stt.TranscribeAsync(wavPath);
                TryDelete(wavPath);

                if (string.IsNullOrWhiteSpace(segment))
                {
                    if (!string.IsNullOrEmpty(_pending)) StartSafetyTimer();   // Aussage offen lassen
                    else SetStatus("Bereit");
                    return;
                }

                // Wartet eine Erinnerung auf Bestaetigung? Dann ist DIESE Aeusserung Franks Reaktion
                // ("ja, was gibt es?") -> jetzt erst die Erinnerung ansagen, kein normaler Turn.
                if (_pendingReminder != null)
                {
                    var pend = _pendingReminder; _pendingReminder = null; StopPinging();
                    await AnnounceReminderAsync(pend);
                    return;
                }

                // Wartet eine proaktive Meldung auf Reaktion? Dann ist DIESE Aeusserung Franks Reaktion.
                if (_pendingProactive != null)
                {
                    var p = _pendingProactive; _pendingProactive = null; _proactivePingTimer?.Stop();
                    await AnnounceProactiveAsync(p);
                    return;
                }

                // Live-Sonde: neuer Turn, sobald das erste Haeppchen einer frischen Aussage eintrifft.
                if (string.IsNullOrEmpty(_pending)) _turn = TurnTrace.Begin("voice");
                _turn?.Heard(segment);

                _pending = string.IsNullOrEmpty(_pending) ? segment.Trim() : (_pending + " " + segment.Trim());

                bool complete = true;
                if (_settings.SemanticEndpointing && _endpoint != null)
                {
                    SetStatus("Pruefe, ob du fertig bist …");
                    complete = await _endpoint.IsCompleteAsync(_pending);
                    _turn?.Endpoint(complete);
                }

                if (complete)
                {
                    await FinalizeAsync();
                }
                else
                {
                    SetStatus("… ich hoere weiter zu");
                    StartSafetyTimer();   // falls nichts mehr kommt, doch senden
                }
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sprach-Verarbeitung fehlgeschlagen", ex);
                _turn?.Failed("voice", ex);
                _turn?.Complete();
                _turn = null;
                Append("Fehler", ex.Message);
                SetStatus("Fehler, siehe Log");
                _pending = string.Empty;
            }
            finally
            {
                _busy = false;
                ResumeMic();
            }
        }

        /// <summary>Sendet die gesammelte Aussage an den Hauptagenten und leert den Puffer.</summary>
        private async Task FinalizeAsync()
        {
            var text = _pending;
            _pending = string.Empty;
            if (string.IsNullOrWhiteSpace(text)) { _turn?.Abort("leere Aussage"); _turn = null; return; }
            _turn?.Accumulated(text);
            Append("Du", text);
            await RespondAndSpeakAsync(text);
            _turn?.Complete();
            _turn = null;
        }

        // ---------- Sicherheitsnetz: nach langer Stille trotzdem senden ----------

        private void StartSafetyTimer()
        {
            CancelSafetyTimer();
            _safetyCts = new CancellationTokenSource();
            var token = _safetyCts.Token;
            int waitMs = _settings.EndpointMaxWaitMs;
            _ = Task.Delay(waitMs, token).ContinueWith(t =>
            {
                if (t.IsCanceled) return;
                Dispatcher.InvokeAsync(() => _ = FlushPendingAsync());
            }, TaskScheduler.Default);
        }

        private void CancelSafetyTimer()
        {
            try { _safetyCts?.Cancel(); _safetyCts?.Dispose(); } catch { /* egal */ }
            _safetyCts = null;
        }

        private async Task FlushPendingAsync()
        {
            if (_busy || string.IsNullOrEmpty(_pending)) return;
            _busy = true;
            PauseMic();
            try
            {
                Log.Info("Endpoint: Sicherheitsnetz greift — sende gesammelte Aussage");
                await FinalizeAsync();
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sicherheitsnetz-Senden fehlgeschlagen", ex);
                _turn?.Failed("safety-flush", ex);
                _turn?.Complete();
                _turn = null;
                _pending = string.Empty;
            }
            finally
            {
                _busy = false;
                ResumeMic();
            }
        }

        // ---------- Text-Eingabe ----------

        private void InputBox_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter) { e.Handled = true; _ = SendTextAsync(); }
        }

        private void SendButton_Click(object sender, RoutedEventArgs e) => _ = SendTextAsync();

        private async Task SendTextAsync()
        {
            var text = InputBox.Text?.Trim();
            if (string.IsNullOrEmpty(text) || _agent == null || _busy) return;

            // Wartet eine Erinnerung auf Bestaetigung? Dann loest diese Eingabe die Ansage aus.
            if (_pendingReminder != null)
            {
                var pend = _pendingReminder; _pendingReminder = null; StopPinging();
                InputBox.Clear();
                Append("Du", text);
                await AnnounceReminderAsync(pend);
                return;
            }

            // Wartet eine proaktive Meldung? Dann loest diese Eingabe die Ansage aus.
            if (_pendingProactive != null)
            {
                var p = _pendingProactive; _pendingProactive = null; _proactivePingTimer?.Stop();
                InputBox.Clear();
                Append("Du", text);
                await AnnounceProactiveAsync(p);
                return;
            }

            CancelSafetyTimer();
            _pending = string.Empty;
            InputBox.Clear();
            _busy = true;
            PauseMic();
            Append("Du", text);
            SendButton.IsEnabled = false;
            _turn = TurnTrace.Begin("text");
            _turn.Accumulated(text);
            try
            {
                await RespondAndSpeakAsync(text);
                _turn?.Complete();
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Text-Verarbeitung fehlgeschlagen", ex);
                _turn?.Failed("text", ex);
                _turn?.Complete();
                Append("Fehler", ex.Message);
                SetStatus("Fehler, siehe Log");
            }
            finally
            {
                _turn = null;
                SendButton.IsEnabled = true;
                _busy = false;
                ResumeMic();
            }
        }

        // ---------- Gemeinsamer Kern: antworten + vorlesen ----------

        private async Task RespondAndSpeakAsync(string text)
        {
            if (_agent == null) return;
            // Erst VERSTEHEN: 1) Tiefes Verstehen (Brain-Rolle, strukturiert: Intent + Ziel-Helfer +
            // normalisierter Auftrag + Rueckfrage). 2) Fallback bei Aus/Fehler: der bisherige
            // 3-Topf-IntentDetector (funktionserhaltend, Direktive #3).
            bool exactIntent = _settings.IntentDetection && _intentDetector != null;
            IntentKind intent = IntentKind.Unknown;
            Understanding? understanding = null;
            if (_understanding != null)
            {
                SetStatus("Verstehe, was du willst …");
                understanding = await _understanding.AnalyzeAsync(text, _sessions?.Active, _subAgents?.All);
                if (understanding != null)
                {
                    intent = understanding.Kind;
                    _turn?.UnderstoodExact(intent);
                }
            }
            if (understanding == null && exactIntent)
            {
                SetStatus("Verstehe, was du willst …");
                intent = await _intentDetector!.ClassifyAsync(text);
                _turn?.UnderstoodExact(intent);
            }
            SetStatus("Denke nach …");
            var sw = System.Diagnostics.Stopwatch.StartNew();

            // Routing-Entscheidung ist synchron; die Antwort kommt als Token-STROM.
            var response = await _agent.HandleStreamingAsync(text, intent, understanding);   // delegiert ggf. an einen Unteragenten
            if (response.DelegatedTo != null) _turn?.Delegated(response.DelegatedTo);

            // Antwort LIVE mitlesbar: jeder fertige Satz wird SOFORT an eine wachsende Agent-Zeile
            // angehaengt — sobald er aus dem Strom kommt (das ist VOR dem Vorlesen, der Producer laeuft
            // dem Audio voraus). So sieht Frank die schriftliche Antwort zeitgenau, waehrend/bevor sie
            // gesprochen wird (Frank-Wunsch 2026-06-08) — nicht erst, wenn alles fertig gesprochen ist.
            // Beim ersten Satz schaltet die Statuszeile live auf "Spreche". Parallel sammeln wir den
            // vollen Text fuer den Turn-Trace.
            var sb = new System.Text.StringBuilder();
            Run? agentRun = null;
            var sentences = SentenceSegmenter.Segment(response.Text);
            var speaker = new StreamingSpeaker(_tts, _player);

            bool spoke = false;
            try
            {
                spoke = await speaker.SpeakStreamAsync(
                    sentences, _settings.TtsLanguageCode, _settings.TtsVoiceName,
                    onSentence: s =>
                    {
                        lock (sb) { if (sb.Length > 0) sb.Append(' '); sb.Append(s); }
                        // UI nur auf dem UI-Thread anfassen (onSentence laeuft im Producer-Thread).
                        Dispatcher.InvokeAsync(() =>
                        {
                            if (agentRun == null)
                            {
                                agentRun = BeginAgentLine();   // erste Agent-Zeile; waechst mit jedem Satz
                                SetStatus("Spreche …");        // ab jetzt kommt Ton -> Status live
                                agentRun.Text = s;
                            }
                            else
                            {
                                agentRun.Text += " " + s;
                            }
                            ConversationBox.ScrollToEnd();
                        });
                    });
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Antwort-Stream fehlgeschlagen", ex);
            }
            sw.Stop();

            string reply;
            lock (sb) { reply = sb.ToString().Trim(); }
            if (string.IsNullOrEmpty(reply))
                Append("Fehler", "Es gab ein Problem bei der Antwort — siehe Log.");

            if (!exactIntent) _turn?.Understood(reply);                                  // Fallback: heuristisch aus der Antwort
            _turn?.Responded(reply, _agent.ProviderName, sw.Elapsed.TotalMilliseconds);  // welches Gehirn, wie schnell, Rueckfrage?
            _turn?.Spoke(_settings.TtsVoiceName, spoke);

            // Nach dem Sprechen: Session sichern, Titel ggf. setzen, bei Bedarf im Hintergrund komprimieren.
            _sessions?.EnsureTitleFromFirstMessage();
            if (_compressor != null && _provider != null && _sessions != null)
                await _compressor.MaybeCompressAsync(_sessions.Active, _provider);
            _sessions?.SaveActive();
            RefreshContextMeter();
            RefreshSessionList();

            // Hat der Agent eine Rueckfrage gestellt? Dann nach ~45 s Stille einmal proaktiv nachfassen.
            ArmFollowupIfPending();

            SetStatus("Bereit");
        }

        private async Task<bool> SpeakAsync(string text)
        {
            if (_tts == null || string.IsNullOrWhiteSpace(text)) return false;
            try
            {
                var audio = await _tts.SynthesizeAsync(text, _settings.TtsLanguageCode, _settings.TtsVoiceName);
                await _player.PlayAsync(audio);
                return true;
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Sprachausgabe fehlgeschlagen (App laeuft weiter)", ex);
                return false;
            }
        }

        // ---------- Helfer ----------

        private void PauseMic() => _listener?.SetEnabled(false);
        // Im Weckwort-Modus laeuft die Aufnahme immer weiter (Hintergrund-Lauschen / weiter wach),
        // sonst nur wenn das Mikrofon manuell an ist.
        private void ResumeMic() { if (_settings.WakeWordEnabled || _settings.MicEnabled) _listener?.SetEnabled(true); }

        private static void TryDelete(string path)
        {
            try { if (File.Exists(path)) File.Delete(path); } catch { /* egal */ }
        }

        /// <summary>
        /// Beginnt eine LIVE-wachsende Agent-Zeile (zum satzweisen Mitlesen waehrend des Vorlesens)
        /// und liefert den Text-Run, der mit jedem eintreffenden Satz erweitert wird. Farbe wie eine
        /// normale Agent-Zeile (aus den Einstellungen). Nur auf dem UI-Thread aufrufen.
        /// </summary>
        private Run BeginAgentLine()
        {
            var brush = new SolidColorBrush(ParseColor(_settings.AgentColor, Color.FromRgb(0x9A, 0x6B, 0x2F)));
            var textRun = new Run(string.Empty) { Foreground = brush };
            var para = new Paragraph();
            para.Inlines.Add(new Run("Agent: ") { FontWeight = FontWeights.Bold, Foreground = brush });
            para.Inlines.Add(textRun);
            ConversationBox.Document.Blocks.Add(para);
            ConversationBox.ScrollToEnd();
            return textRun;
        }

        private void Append(string who, string text)
        {
            // Farbliche Unterscheidung: Du/Agent aus den Einstellungen waehlbar; Fehler=Rot, System=Grau.
            var brush = new SolidColorBrush(who switch
            {
                // Fallbacks auf das warm-helle Theme abgestimmt (lesbar auf hellem Grund);
                // eigene Farben aus den Einstellungen haben weiterhin Vorrang.
                "Du" => ParseColor(_settings.UserColor, Color.FromRgb(0x1E, 0x66, 0xB0)),
                "Agent" => ParseColor(_settings.AgentColor, Color.FromRgb(0x9A, 0x6B, 0x2F)),
                "Fehler" => Color.FromRgb(0xC0, 0x39, 0x2B),
                _ => Color.FromRgb(0x8A, 0x82, 0x75),
            });
            var para = new Paragraph();
            para.Inlines.Add(new Run(who + ": ") { FontWeight = FontWeights.Bold, Foreground = brush });
            para.Inlines.Add(new Run(text) { Foreground = brush });
            ConversationBox.Document.Blocks.Add(para);
            ConversationBox.ScrollToEnd();
        }

        /// <summary>Hex -> Color, mit Fallback bei ungueltigem Wert (kein Crash).</summary>
        private static Color ParseColor(string? hex, Color fallback)
        {
            if (string.IsNullOrWhiteSpace(hex)) return fallback;
            try { return (Color)ColorConverter.ConvertFromString(hex)!; }
            catch { return fallback; }
        }

        // ---------- Sessions-Sidebar ----------

        private void RefreshSessionList()
        {
            if (_sessions == null) return;
            try
            {
                var query = SearchBox.Text?.Trim() ?? "";
                IEnumerable<SessionInfo> items = _sessions.List();
                if (!string.IsNullOrEmpty(query))
                    items = items.Where(i => i.Title.Contains(query, StringComparison.OrdinalIgnoreCase));
                _suppressSelection = true;
                SessionList.ItemsSource = items.ToList();
                SessionList.SelectedItem = null;
                _suppressSelection = false;
            }
            catch (Exception ex) { Log.Error("Sidebar: Liste aktualisieren fehlgeschlagen", ex); }
        }

        private void NewSessionButton_Click(object sender, RoutedEventArgs e)
        {
            if (_sessions == null) return;
            _sessions.SaveActive();
            _sessions.NewSession();
            BuildAgents();          // Agent zeigt auf die neue (leere) Session
            ClearTranscript();
            RefreshContextMeter();
        }

        private void SessionList_SelectionChanged(object sender, System.Windows.Controls.SelectionChangedEventArgs e)
        {
            if (!_ready || _suppressSelection || _sessions == null) return;
            if (SessionList.SelectedItem is SessionInfo info && info.Id != _sessions.Active.Id)
            {
                _sessions.SaveActive();
                _sessions.Switch(info.Id);
                BuildAgents();
                RenderTranscript();
                RefreshContextMeter();
            }
        }

        private void SearchBox_TextChanged(object sender, System.Windows.Controls.TextChangedEventArgs e)
        {
            if (!_ready) return;
            RefreshSessionList();
        }

        private void RenameSession_Click(object sender, RoutedEventArgs e)
        {
            if (InfoFromMenu(sender) is not { } info || _sessions == null) return;
            var name = PromptText("Session umbenennen", info.Title);
            if (!string.IsNullOrWhiteSpace(name)) { _sessions.Rename(info.Id, name!); RefreshSessionList(); }
        }

        private void PinSession_Click(object sender, RoutedEventArgs e)
        {
            if (InfoFromMenu(sender) is not { } info || _sessions == null) return;
            _sessions.SetPinned(info.Id, !info.Pinned);
            RefreshSessionList();
        }

        private void DeleteSession_Click(object sender, RoutedEventArgs e)
        {
            if (InfoFromMenu(sender) is not { } info || _sessions == null) return;
            if (MessageBox.Show($"Session „{info.Title}“ löschen?", "Löschen",
                    MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
            bool wasActive = info.Id == _sessions.Active.Id;
            _sessions.Delete(info.Id);
            if (wasActive) { BuildAgents(); RenderTranscript(); }
            RefreshSessionList();
            RefreshContextMeter();
        }

        private static SessionInfo? InfoFromMenu(object sender)
            => (sender as System.Windows.Controls.MenuItem)?.DataContext as SessionInfo;

        private void RefreshContextMeter()
        {
            if (_compressor == null || _sessions == null) return;
            try
            {
                int pct = (int)Math.Round(_compressor.Fill(_sessions.Active) * 100);
                ContextPillText.Text = pct >= 75 ? $"Kontext {pct}% · wird komprimiert…" : $"Kontext {pct}%";
            }
            catch (Exception ex) { Log.Error("Kontext-Anzeige aktualisieren fehlgeschlagen", ex); }
        }

        private void ClearTranscript() => ConversationBox.Document.Blocks.Clear();

        private void RenderTranscript()
        {
            ClearTranscript();
            if (_sessions == null) return;
            foreach (var m in _sessions.Active.History)
                Append(m.Role == LlmRole.Assistant ? "Agent" : "Du", m.Text);
        }

        /// <summary>Kleiner modaler Text-Dialog (bewusst ohne Microsoft.VisualBasic-Dependency).</summary>
        private string? PromptText(string title, string initial)
        {
            // Farben aus dem aktiven Theme (Fallback: helle Werte), damit der Dialog im Dunkelmodus mitzieht.
            Brush B(string key, byte r, byte g, byte b)
                => Application.Current.Resources[key] as Brush ?? new SolidColorBrush(Color.FromRgb(r, g, b));

            var win = new Window
            {
                Title = title, Width = 380, Height = 150, Owner = this,
                WindowStartupLocation = WindowStartupLocation.CenterOwner,
                ResizeMode = ResizeMode.NoResize,
                Background = B("WindowBackground", 0xFB, 0xFA, 0xF8)
            };
            win.SourceInitialized += (_, __) => ThemeManager.ApplyTitleBar(win);   // Titelleiste passend (§2.7)
            var grid = new System.Windows.Controls.Grid { Margin = new Thickness(12) };
            grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = GridLength.Auto });
            grid.RowDefinitions.Add(new System.Windows.Controls.RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            var tb = new System.Windows.Controls.TextBox
            {
                Text = initial, FontSize = 14, Padding = new Thickness(6, 4, 6, 4),
                Background = B("InputBackground", 0xFF, 0xFF, 0xFF),
                Foreground = B("TextPrimary", 0x37, 0x32, 0x2B),
                BorderBrush = B("InputBorder", 0xE0, 0xD9, 0xCC),
                VerticalContentAlignment = VerticalAlignment.Center
            };
            System.Windows.Controls.Grid.SetRow(tb, 0);
            var ok = new System.Windows.Controls.Button
            {
                Content = "OK", Width = 90, Height = 30, IsDefault = true,
                HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Bottom
            };
            System.Windows.Controls.Grid.SetRow(ok, 1);
            string? result = null;
            ok.Click += (_, __) => { result = tb.Text; win.DialogResult = true; };
            grid.Children.Add(tb);
            grid.Children.Add(ok);
            win.Content = grid;
            tb.Loaded += (_, __) => { tb.Focus(); tb.SelectAll(); };
            return win.ShowDialog() == true ? result : null;
        }

        private void SetStatus(string text) => StatusText.Text = text;
    }
}
