using System;
using System.Collections.Generic;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
using Microsoft.Extensions.DependencyInjection;
using TerminalVoiceOverlay.Models;
using TerminalVoiceOverlay.NativeMethods;
using TerminalVoiceOverlay.Services;

namespace TerminalVoiceOverlay.Views
{
    public partial class OverlayWindow : Window
    {
        // ── Color constants ──
        // Tuned for the dark cohesive overlay panel: shifted from the bright
        // Material-500/300 palette to Material-700/800 so buttons sit calmly
        // on the near-black section backgrounds instead of glowing neon.
        // The macOS OverlayPanel.swift still uses the original brighter set;
        // when the macOS look gets harmonised it should pull these values.

        // Idle/base
        private static readonly SolidColorBrush BtnIdle       = Brush("#2D2D2D");
        private static readonly SolidColorBrush BtnRecording  = Brush("#C62828");
        private static readonly SolidColorBrush BtnProcessing = Brush("#EF6C00");
        private static readonly SolidColorBrush BtnSuccess    = Brush("#2E7D32");
        // Toggles
        private static readonly SolidColorBrush ToggleOn      = Brush("#2E7D32");
        private static readonly SolidColorBrush ToggleOff     = Brush("#2D2D2D");
        // BTW mic (pair with main mic in Yellow family — joins the warm
        // upper zone with the gold star, distinct from every cool button
        // below. Dark icon/text required for contrast on yellow.)
        private static readonly SolidColorBrush BtnBtwIdle      = Brush("#FBC02D");  // Yellow 700
        private static readonly SolidColorBrush BtnBtwRecording = Brush("#F57F17");  // Yellow 900
        private static readonly SolidColorBrush BtnBtwPulse     = Brush("#FFEB3B");  // Yellow 500
        // Special
        private static readonly SolidColorBrush BtnX         = Brush("#C62828");
        private static readonly SolidColorBrush BtnXPressed  = Brush("#E53935");
        private static readonly SolidColorBrush BtnMicIdle   = Brush("#F9A825");  // Yellow 800
        // Copy/Paste buttons (pair: same blue family so they read as a unit)
        private static readonly SolidColorBrush BtnCopy      = Brush("#0288D1");  // Light Blue 700
        private static readonly SolidColorBrush BtnPaste     = Brush("#0277BD");  // Light Blue 800
        // Screenshot + Insert-Screenshot buttons (pair: same teal family)
        private static readonly SolidColorBrush BtnScreenshot       = Brush("#00796B");  // Teal 700
        private static readonly SolidColorBrush BtnInsertScreenshot = Brush("#00897B");  // Teal 600
        // Ultrathink star
        private static readonly SolidColorBrush BtnUltrathinkOn  = Brush("#9E7B0E");
        private static readonly SolidColorBrush StarGold         = Brush("#DAA520");
        private static readonly SolidColorBrush StarMuted        = Brush("#8B7355");

        // Pulse colours for main mic
        private static readonly SolidColorBrush BtnRecordingBright = Brush("#FF5252");

        // ── Services ──
        private readonly AudioRecorder     _audioRecorder;
        private readonly GroqWhisperClient _groqClient;
        private readonly GeminiClient?     _geminiClient;
        private readonly TerminalWatcher   _terminalWatcher;

        // ── State ──
        private RecordingState _micState    = RecordingState.Idle;
        private bool _isProcessing          = false;
        private bool isBtwRecording         = false;
        private bool geminiEnabled          = false; // Default = Whisper-Mode (W aktiv, G dunkel). Erstes Profil-Klick schaltet Gemini automatisch ein.
        private bool autoEnterEnabled       = true;  // macOS default (was false in Windows)
        private bool hasPastedText          = false;
        // Wenn true, presst OnInputSubmit beim naechsten Aufruf Return —
        // unabhaengig von autoEnterEnabled. Wird vom Enter-Button gesetzt
        // damit ein Klick darauf den Text aus der Prompt-Eingabe nicht nur
        // einfuegt, sondern auch sofort an die KI abschickt.
        private bool _forceReturnOnNextSubmit = false;
        // Reines UI-Flag: spiegelt wider, ob das Promtboard-Panel geoeffnet
        // ist (Stern goldgelb). Steuert NICHT die AlwaysOn-Pipeline — die
        // AlwaysOn-Prompts (IsAlwaysOn=true in der DB) werden bei JEDEM
        // Voice-Submit angehaengt, unabhaengig davon ob das Panel sichtbar
        // ist. Frueher koppelte dieses Flag beides; das war der Grund warum
        // beim ersten Start ohne Sternklick keine Pre/Post-Prompts mitgingen.
        private bool alwaysOnActive         = false;

        // Aktives Gemini-Korrektur-Profil. 1 = Standard (alltaegliche Texte
        // und Ideen), 2 = Programmierung (Code-Begriffe, CLI, Frameworks),
        // 3 = Meta-Intelligenz (strukturiertes Denken). Default = 1, weil das
        // alltaegliche Profil der haeufigste Anwendungsfall ist; spezifische
        // Profile schaltet der Benutzer aktiv ein.
        private int _activeProfile = 1;

        // Letzte Re-Correct-Sicherheits-Nutzlast. Solange der Sprecher den
        // eingefuegten Text nicht ueberschrieben oder abgeschickt hat, kann
        // er per Profil-Klick den Roh-Whisper-Text durch ein anderes Gemini-
        // Profil schicken — die alte Eingabezeile wird geloescht und durch
        // die neue Korrektur ersetzt. Nullen wir nach jeder erfolgreichen
        // Re-Korrektur sowie nach jedem unabhaengigen Klick (Enter, Clear,
        // neue Aufnahme).
        // Letzte Whisper-Roh-Transkription. Bleibt im Cache solange die App
        // laeuft, wird nur durch eine NEUE Aufnahme ueberschrieben — kein
        // Zeitlimit. Frank steuert das Verhalten ueber Maustaste:
        //  • Linksklick auf Profil-Tile → Re-Correct (Cache durch Profil
        //    schicken, Eingabe ersetzen, ggf. Auto-Submit)
        //  • Rechtsklick auf Profil-Tile → nur Profil wechseln, Cache bleibt
        //    unangetastet, naechste Aufnahme nutzt das neue Profil
        private string? _lastCorrectableRaw = null;

        // PromptBoard integration: on-demand prefix lookup + side panel.
        private IAlwaysOnPrefixService? _alwaysOnPrefix;

        /// <summary>
        /// Service fuer die Prompt-Historie. Wird lazy beim ersten Submit
        /// erzeugt — der Konstruktor legt nur die Ordnerstruktur an, kein
        /// Netzwerk- oder DB-Zugriff. Die Historie ist eine reine JSON-Datei
        /// in %LocalAppData%\PromptBoard\history\, also unabhaengig von
        /// der Promptboard-SQLite-Datenbank.
        /// </summary>
        private readonly PromptHistoryService _historyService = new();

        /// <summary>
        /// Drive-Sync der Historie. Lazy initialisiert — beim ersten Submit
        /// oder beim Mergen am App-Start. Wenn Drive nicht verbunden ist,
        /// wirft die erste Operation eine Exception, die wir still
        /// schlucken (Sync ist eine Komfort-Funktion, kein Pflichtkanal).
        /// </summary>
        private PromptHistoryDriveSync? _historySync;

        /// <summary>
        /// Liefert den AKTIVEN GeminiClient — der Key kommt bevorzugt aus
        /// dem PromptBoard-Settings-Dialog (zentrale Quelle der Wahrheit).
        /// Falls dort kein Key hinterlegt ist, faellt die Methode auf den
        /// alten .env-Pfad (<see cref="_geminiClient"/>) zurueck. So pflegt
        /// der Benutzer EINEN Key an EINER Stelle und alle Pfade — Diktat-
        /// Cleanup, BTW, AI-Improvement, Historie-Titel — ziehen am
        /// selben Strang. Pro Aufruf wird der PromptBoard-Key frisch
        /// gelesen, damit eine Aenderung im Settings-Dialog sofort greift
        /// ohne App-Neustart.
        /// </summary>
        private async Task<GeminiClient?> GetActiveGeminiClientAsync()
        {
            try
            {
                using var scope = PromptBoardHost.Services.CreateScope();
                var repo = scope.ServiceProvider
                    .GetRequiredService<PromptBoard.Core.Repositories.IAppSettingsRepository>();
                var settings = await repo.GetAsync();
                string? key = settings.GeminiApiKey;
                if (!string.IsNullOrWhiteSpace(key))
                {
                    // gemini-3.1-flash-lite-preview ist das Standard-Modell
                    // der Voice Terminal Overlay App — alle Gemini-Pfade
                    // (Diktat-Cleanup, Prompt-Improvement, Historie-Titel)
                    // nutzen dasselbe Modell, damit Verhalten und Latenz
                    // ueberall vorhersagbar sind. ThinkingLevel bleibt leer:
                    // das Lite-Modell akzeptiert keinen thinkingConfig-Block,
                    // der Client laesst ihn dann komplett aus dem Payload.
                    return new GeminiClient(key, "gemini-3.1-flash-lite-preview", "");
                }
            }
            catch (Exception ex)
            {
                LogToHistoryDebug($"GetActiveGeminiClientAsync FAIL: {ex.GetType().Name}: {ex.Message}");
            }
            // Fallback: der vom Voice-Overlay aus der .env-Datei gebaute
            // Client. Behaelt das alte Verhalten fuer Benutzer die ihren
            // Key noch nicht im PromptBoard-Settings-Dialog gepflegt haben.
            return _geminiClient;
        }

        /// <summary>
        /// Schreibt eine Diagnose-Zeile in title-debug.log neben der
        /// Promptboard-Datenbank. Praktisch zum Erkennen warum die Historie-
        /// Titel manchmal nicht von Gemini kommen — wir loggen pro Submit
        /// einmal mit Fallback-Titel und einmal mit dem AI-Ergebnis.
        /// </summary>
        private static void LogToHistoryDebug(string line)
        {
            try
            {
                string dir = System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "PromptBoard", "history");
                System.IO.Directory.CreateDirectory(dir);
                string path = System.IO.Path.Combine(dir, "title-debug.log");
                string ts = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
                System.IO.File.AppendAllText(path, $"{ts}  {line}\n",
                    System.Text.Encoding.UTF8);
            }
            catch { /* Diagnostics must never break the main flow. */ }
        }

        /// <summary>
        /// Schreibt eine Diagnose-Zeile in screenshot.log neben der
        /// Promptboard-Datenbank. Erfasst JEDE Aktion an Screenshot- und
        /// InsertScreenshot-Button (Klick, Erfolg, Fehler, Pfad). Im Bug-Fall
        /// kann man hier nachsehen ob der Klick ankam, ob der Save klappte
        /// und welcher Pfad gemerkt wurde — direkt einsehbar mit
        ///   notepad %LOCALAPPDATA%\PromptBoard\screenshot-debug\screenshot.log
        /// </summary>
        private static void LogScreenshot(string line)
        {
            try
            {
                string dir = System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "PromptBoard", "screenshot-debug");
                System.IO.Directory.CreateDirectory(dir);
                string path = System.IO.Path.Combine(dir, "screenshot.log");
                string ts = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
                System.IO.File.AppendAllText(path, $"{ts}  {line}\n",
                    System.Text.Encoding.UTF8);
            }
            catch { /* Diagnostics must never break the main flow. */ }
        }

        private PromptBoardPanel? _promptPanel;
        private string? lastRawTranscript   = null;

        // ── Push-to-Talk Hotkey-State ──
        // Hotkey: Strg+Alt+Leertaste. Unterstuetzt zwei Bedien-Modi:
        //
        //   1) HOLD (Push-to-Talk): Taste laenger als 500 ms gedrueckt halten
        //      → Aufnahme laeuft solange gehalten wird, stoppt beim Loslassen.
        //      Klassische PTT-Bedienung, Daumen am Space, sprechen, loslassen.
        //
        //   2) TAP (Toggle): kurzer Tastendruck (< 500 ms zwischen DOWN und UP)
        //      → Aufnahme startet und LAEUFT WEITER, auch wenn die Taste
        //      schon losgelassen ist. Naechster kurzer Tap stoppt sie.
        //      Diese Variante ist wichtig fuer Logitech-G-Tasten, weil das
        //      G HUB ein G-Macro per Default als kurzes Tap-Event sendet
        //      und nicht als echtes Halten. Frank kann seine G5-Taste also
        //      ohne G-HUB-Konfiguration nutzen: tap zum Starten, tap zum Beenden.
        //
        // Erkennung erfolgt am UP-Event: war der Druck kuerzer als
        // PttTapThresholdMs, gehen wir in Toggle-Modus und warten auf den
        // naechsten Tap. War er laenger, gilt klassisches PTT und Loslassen
        // stoppt sofort.
        private IntPtr _pttHookHandle = IntPtr.Zero;
        private NativeMethods.Win32.LowLevelKeyboardProc? _pttHookProc;
        private bool _pttRecording   = false;  // wir haben aktuell eine Aufnahme via PTT laufen
        private bool _pttToggleMode  = false;  // im Toggle-Modus (durch Tap aktiviert) — wartet auf naechsten Tap zum Stoppen
        private DateTime _pttKeyDownAt = DateTime.MinValue;  // Zeitpunkt des letzten DOWN-Events
        // Hybrid-Modus: kurzer Tap (<500ms) startet Toggle-Modus (Walkie-Talkie:
        // 1x tippen start, 1x tippen stop). Langes Halten (>=500ms) ist klassisches
        // PTT — Loslassen stoppt. Dadurch funktionieren beide Workflows:
        //   - G-HUB Macros im "Tap"-Modus (Down+Up in <10ms) → Toggle aktiv
        //   - Echte Tastatur-Hold ueber 500ms → klassisches PTT
        //   - G-HUB Macros im "Hold"-Modus → Down kommt sofort, Up beim Release →
        //     wenn der Release nach >500ms erfolgt, klassisches PTT
        private const int PttTapThresholdMs = 500;

        // Alt+F11 Explorer-Shortcut: Auto-Repeat-Schutz, damit pro Tastendruck
        // nur EIN Explorer-Fenster aufgeht — auch wenn Windows DOWN-Events mehrfach feuert.
        private bool _altF11Down = false;
        private const string ReleaseBundleFolder =
            @"C:\Users\barwa\proggs\BestJournalAndroid\app\build\outputs\bundle\release";

        // Alt+F12 Toggle (PRIMAERER PTT-Hotkey, gemappt auf G-HUB G5):
        // G-HUB unterstuetzt KEIN echtes Press/Release — alle Modi (Tap, Wiederholen
        // beim Halten, Toggle, Sequenz) senden Tap-Sequenzen. Daher: F12 ist reines
        // Toggle — 1x druecken startet, 1x druecken stoppt+transkribiert. Cooldown
        // schluckt Tap-Spam-Bursts (z.B. von "Wiederholen beim Halten") sodass das
        // System nicht zwischen Start/Stop chaotisch hin- und herwechselt.
        private DateTime _altF12CooldownUntil = DateTime.MinValue;
        private bool _altF12KeyDown = false;
        private const int AltF12CooldownMs = 350;

        // Debounce-Flags fuer Screenshot-/Insert-Hotkeys: wir wollen nur auf
        // die ERSTE DOWN-Flanke reagieren, nicht auf Tastatur-Auto-Repeat.
        // Werden beim KeyUp wieder zurueckgesetzt damit der naechste Druck
        // wieder feuern kann.
        private bool _screenshotKeyDown = false;
        private bool _insertKeyDown     = false;

        // Pfad des zuletzt mit dem ScreenshotButton aufgenommenen Bildes.
        // Wird vom InsertScreenshotButton gelesen — exakt diese eine Datei
        // wird als Pfad in die CLI eingefuegt, keine andere. Bleibt null
        // bis der erste Screenshot gemacht wurde.
        private string? _lastScreenshotPath = null;

        // True wenn der Benutzer im Eingabefenster den Stern geklickt hat:
        // das Promptboard ist dann versteckt und das Eingabefenster nimmt
        // dessen Andock-Platz links neben dem Pillar ein. Im Drag- und
        // Reposition-Pfad muessen wir dann die InputWindow-Geometrie statt
        // der Promtboard-Geometrie aktualisieren.
        private bool _inputSoloDock;

        // ── Right-click drag state ──
        private bool _isDragging;
        // Click-vs-Drag-Erkennung beim Rechtsklick auf Profile-Tiles. 0 = kein
        // Tile getroffen oder Drag laeuft schon. >0 = Profil-Index, der bei
        // RBUTTONUP als Klick gilt — solange die Maus weniger als
        // DragThresholdPx bewegt wurde. Sonst wird er auf 0 zurueckgesetzt
        // und das Pillar verschoben sich wie gehabt.
        private int _pendingProfileTileClick;
        private const int DragThresholdPx = 4;
        private bool _manuallyPositioned;
        private int _dragStartCursorX, _dragStartCursorY;
        private double _dragStartLeft, _dragStartTop;
        private double _dragDpiX, _dragDpiY;

        // ── Timers ──
        private readonly DispatcherTimer _pulseTimer;
        private readonly DispatcherTimer _btwPulseTimer;
        private readonly DispatcherTimer _resetTimer;
        // Verzoegerung beim Verlassen des Terminals: 5 Sekunden warten bevor
        // das Overlay versteckt wird. Wenn der Benutzer innerhalb dieser Zeit
        // zurueck zum Terminal wechselt, wird der Timer gestoppt — das Overlay
        // bleibt sichtbar. Praktisch um z.B. einen Screenshot von einer
        // anderen App zu machen, ohne das Pillar zu verlieren.
        private readonly DispatcherTimer _hideDelayTimer;
        private bool _pulseBright    = false;
        private bool _btwPulseBright = false;

        // ── Waveform-Visualizer (Pegel-Anzeige im Mic-Button) ──
        // 14 Striche, je 2px breit mit 1px Spacing → Gesamtbreite 41px,
        // zentriert im 48px-Canvas (Start-Offset 3.5px ≈ 4). Buffer haelt
        // die letzten 14 Pegelwerte (0..1); neue Werte kommen rechts rein,
        // alte fallen links raus — die Welle "fliesst" optisch nach links.
        private const int  WaveformBarCount  = 14;
        private const double WaveformBarWidth   = 2.0;
        private const double WaveformBarSpacing = 1.0;
        private const double WaveformCanvasH    = 48.0;
        private const double WaveformMinH       = 3.0;   // minimaler Strich, damit die Welle nie "weg" ist
        private const double WaveformMaxH       = 40.0;  // Vollausschlag — etwas kleiner als Canvas-Hoehe
        private readonly float[] _waveformBuffer = new float[WaveformBarCount];
        private readonly System.Windows.Shapes.Rectangle[] _waveformBars =
            new System.Windows.Shapes.Rectangle[WaveformBarCount];

        // ── Constructor ──

        public OverlayWindow(Config config)
        {
            InitializeComponent();

            _audioRecorder   = new AudioRecorder(config.AudioSampleRate, config.AudioChannels);
            _groqClient      = new GroqWhisperClient(config.GroqApiKey, config.WhisperModel, config.WhisperLang, config.WhisperUrl);
            _terminalWatcher = new TerminalWatcher(config.TerminalProcessNames);

            if (config.GeminiAvailable)
                _geminiClient = new GeminiClient(config.GeminiApiKey!, config.GeminiModel, config.GeminiThinkingLevel);

            // Share the audio/STT/Gemini stack with secondary surfaces
            // (e.g. PromptEditDialog's mic + G buttons). Single AudioRecorder
            // instance is critical — only one process can hold the microphone.
            VoiceServiceProvider.Initialize(_audioRecorder, _groqClient, _geminiClient);

            // ── Pulse timer: main mic (500 ms, #FF6666 ↔ #E53935) ──
            _pulseTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
            _pulseTimer.Tick += (_, _) =>
            {
                _pulseBright = !_pulseBright;
                MicButton.Background = _pulseBright ? BtnRecordingBright : BtnRecording;
            };

            // ── Pulse timer: BTW mic (500 ms, #90CAF9 ↔ #1E88E5) ──
            _btwPulseTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(500) };
            _btwPulseTimer.Tick += (_, _) =>
            {
                _btwPulseBright = !_btwPulseBright;
                BtwButton.Background = _btwPulseBright ? BtnBtwPulse : BtnBtwRecording;
            };

            // ── Reset timer: 3 s back to idle after success/error ──
            // Defensiver Check gegen Race Condition: Wenn der Tick bereits in
            // der Dispatcher-Queue liegt und der Benutzer sehr schnell eine neue
            // Aufnahme startet, wuerde der Tick die laufende Recording-Anzeige
            // ueberschreiben (Symptom: Mic-Button wird mitten in der Aufnahme
            // gelb statt rot, Welle verschwindet). DispatcherTimer.Stop() kann
            // bereits geplante Ticks nicht zuverlaessig zuruecknehmen — daher
            // hier ein Guard: wenn aktuell Recording laeuft, NICHT auf Idle.
            _resetTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(3) };
            _resetTimer.Tick += (_, _) =>
            {
                _resetTimer.Stop();
                if (_micState == RecordingState.Recording) return;
                SetMicState(RecordingState.Idle);
            };

            // ── Hide-Delay-Timer: 5 s nach Verlassen des Terminals ──
            // Wechselt der Benutzer zu einer anderen App (z.B. Browser), wird
            // dieser Timer gestartet. Erst nach 5 Sekunden wird das Overlay
            // tatsaechlich versteckt — so kann der Benutzer in der Zwischenzeit
            // noch einen Screenshot machen oder das Pillar nutzen, ohne dass
            // es sofort verschwindet. Wechselt er innerhalb der 5 Sekunden
            // zurueck zum Terminal, wird der Timer in OnTerminalActivated
            // gestoppt — das Overlay bleibt sichtbar.
            _hideDelayTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(5) };
            _hideDelayTimer.Tick += (_, _) =>
            {
                _hideDelayTimer.Stop();
                HideOverlayNow();
            };

            // ── Initial button colours ──
            // G-button is on by default — falls back to Whisper-raw if no Gemini API key.
            if (_geminiClient == null) geminiEnabled = false;
            XButton.Background    = BtnX;           // red
            WButton.Background    = geminiEnabled ? ToggleOff : ToggleOn;  // green when Gemini off (Whisper-raw)
            MicButton.Background  = BtnMicIdle;      // dark blue
            BtwButton.Background  = BtnBtwIdle;      // light blue
            GButton.Background    = geminiEnabled ? ToggleOn : ToggleOff;  // green when Gemini on
            EnterButton.Background = BtnProcessing;  // orange (autoEnter starts true)
            CopyButton.Background  = BtnCopy;        // light blue
            PasteButton.Background = BtnPaste;       // purple
            ScreenshotButton.Background       = BtnScreenshot;       // teal
            InsertScreenshotButton.Background = BtnInsertScreenshot; // amber
            UltrathinkButton.Background = ToggleOff;  // dark (PromptBoard always-on prefix starts disabled)

            // ── Profil-Tiles initial setzen ──
            // Default = 1 (Standard) fuer alltaegliche Texte. Backend-
            // Verkabelung pro Profil ist aktiv via SK-Dateien.
            SetActiveProfile(_activeProfile);

            // ── Tooltip-Positionierung global ankern ──
            // Standard-WPF-Verhalten setzt den Tooltip relativ zum Button.
            // Bei den rechten Profil-Tiles waere er dadurch mitten ueber
            // dem Mic. Stattdessen wickeln wir jeden String-ToolTip in ein
            // ToolTip-Objekt und haengen einen Opened-Handler dran, der die
            // Position dynamisch setzt: rechte Tooltip-Kante immer 8 Pixel
            // links vom Window, vertikal mittig zum gehoverten Button.
            //
            // String-ToolTips werden von WPF erst beim Anzeigen in ToolTip-
            // Objekte umgewandelt — zu spaet fuer ToolTipOpening. Pre-
            // wickeln loest das.
            WrapStringTooltips();

            // ── Waveform-Striche einmalig im Canvas anlegen ──
            // 14 weisse Rectangles mit voller Deckkraft auf dem roten
            // Recording-Hintergrund — klassischer VU-Meter-Look. Sie
            // werden hier nur erzeugt; die Hoehen-Animation passiert in
            // OnAudioLevelChanged. Initiale Hoehe = WaveformMinH, damit
            // schon vor der ersten Sprache eine ruhige Strich-Reihe zu
            // sehen ist (sobald die Welle eingeblendet wird).
            BuildWaveformBars();

            // ── Pegel-Listener: speist die Welle waehrend der Aufnahme ──
            // AudioRecorder feuert auf seinem eigenen Thread. Wir hoeren
            // nur zu wenn Recording laeuft (siehe RecordingState-Switch);
            // im Idle-Zustand kommen ohnehin keine Events, weil der
            // WaveInEvent dann gar nicht laeuft.
            _audioRecorder.LevelChanged += OnAudioLevelChanged;

            // ── Hover animations ──
            AttachHover(XButton);
            AttachHover(WButton);
            AttachHover(BtwButton);
            AttachHover(MicButton);
            AttachHover(GButton);
            AttachHover(UltrathinkButton);
            AttachHover(EnterButton);
            AttachHover(CopyButton);
            AttachHover(PasteButton);
            AttachHover(ScreenshotButton);
            AttachHover(InsertScreenshotButton);

            // ── Terminal watcher ──
            _terminalWatcher.TerminalActivated   += OnTerminalActivated;
            _terminalWatcher.TerminalDeactivated += OnTerminalDeactivated;
            _terminalWatcher.Start();

            // ── Push-to-Talk Hotkey installieren ──
            // Strg+Alt+Leertaste gedrueckt halten → Aufnahme laeuft.
            // Loslassen → Whisper transkribiert + paste den Text.
            // Implementiert ueber Low-Level-Keyboard-Hook (WH_KEYBOARD_LL),
            // weil RegisterHotKey nur KeyDown liefert, nicht KeyUp.
            InstallPushToTalkHook();

            // Resolve the PromptBoard prefix service if the DI host is up.
            // Silent fallback when PromptBoard is unavailable — the star
            // button simply toggles nothing in that case.
            try
            {
                _alwaysOnPrefix = PromptBoardHost.Get<IAlwaysOnPrefixService>();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AlwaysOnPrefixService not available: {ex.Message}");
                _alwaysOnPrefix = null;
            }

            // Cloud-Merge der Prompt-Historie: einmal beim App-Start
            // versuchen, neue Eintraege vom anderen Geraet abzuholen. Fire-
            // and-forget — wenn Drive nicht verbunden ist, schluckt der
            // Helper die Exception und es wird einfach nichts gemergt.
            _ = TryMergeHistoryFromCloudAsync();
        }

        // ── Hover animation helper ──

        private static void AttachHover(System.Windows.Controls.Button btn)
        {
            btn.RenderTransformOrigin = new Point(0.5, 0.5);
            btn.RenderTransform       = new ScaleTransform(1.0, 1.0);

            btn.MouseEnter += (_, _) => AnimateScale(btn, 1.15, TimeSpan.FromMilliseconds(150));
            btn.MouseLeave += (_, _) => AnimateScale(btn, 1.0,  TimeSpan.FromMilliseconds(150));
        }

        private static void AnimateScale(System.Windows.Controls.Button btn, double to, TimeSpan duration)
        {
            var ease  = new QuadraticEase { EasingMode = EasingMode.EaseOut };
            var animX = new DoubleAnimation(to, new Duration(duration)) { EasingFunction = ease };
            var animY = new DoubleAnimation(to, new Duration(duration)) { EasingFunction = ease };

            if (btn.RenderTransform is ScaleTransform st)
            {
                st.BeginAnimation(ScaleTransform.ScaleXProperty, animX);
                st.BeginAnimation(ScaleTransform.ScaleYProperty, animY);
            }
        }

        // ── Non-activating window setup ──

        protected override void OnSourceInitialized(EventArgs e)
        {
            base.OnSourceInitialized(e);

            var hwnd = new WindowInteropHelper(this).Handle;

            // Add WS_EX_NOACTIVATE + WS_EX_TOOLWINDOW
            var exStyle = Win32.GetWindowLong(hwnd, Win32.GWL_EXSTYLE);
            Win32.SetWindowLong(hwnd, Win32.GWL_EXSTYLE,
                exStyle | Win32.WS_EX_NOACTIVATE | Win32.WS_EX_TOOLWINDOW);

            // Hook WndProc for WM_MOUSEACTIVATE
            var source = HwndSource.FromHwnd(hwnd);
            source?.AddHook(WndProc);
        }

        private IntPtr WndProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
        {
            switch (msg)
            {
                case Win32.WM_MOUSEACTIVATE:
                    handled = true;
                    return (IntPtr)Win32.MA_NOACTIVATE;

                case Win32.WM_RBUTTONDOWN:
                    // Drag-Setup wie immer, ABER zusaetzlich Hit-Test auf
                    // Profile-Tiles: liegt der Klick auf einem Tile, wird
                    // _pendingProfileTileClick gesetzt. Bei kurzem Klick
                    // (kein Drag bis zum Up) feuert Profil-Wechsel; bei
                    // grosserer Bewegung wird der Pending verworfen und
                    // normales Pillar-Drag uebernimmt.
                    if (Win32.GetCursorPos(out var startPt))
                    {
                        _isDragging = true;
                        _dragStartCursorX = startPt.X;
                        _dragStartCursorY = startPt.Y;
                        _dragStartLeft = Left;
                        _dragStartTop = Top;
                        var src = PresentationSource.FromVisual(this);
                        _dragDpiX = src?.CompositionTarget?.TransformToDevice.M11 ?? 1.0;
                        _dragDpiY = src?.CompositionTarget?.TransformToDevice.M22 ?? 1.0;
                        Win32.SetCapture(hwnd);
                        _pendingProfileTileClick = HitTestProfileTile();
                    }
                    handled = true;
                    break;

                case Win32.WM_MOUSEMOVE:
                    if (_isDragging && Win32.GetCursorPos(out var movePt))
                    {
                        // Pending Profile-Tile-Click? Erst pruefen ob Maus
                        // mehr als die Drag-Schwelle bewegt wurde — solange
                        // sie es nicht ist, Pillar in Ruhe lassen.
                        if (_pendingProfileTileClick > 0)
                        {
                            int dx = Math.Abs(movePt.X - _dragStartCursorX);
                            int dy = Math.Abs(movePt.Y - _dragStartCursorY);
                            if (dx < DragThresholdPx && dy < DragThresholdPx)
                            {
                                break;
                            }
                            // Drag-Schwelle ueberschritten — ab jetzt echtes Drag
                            _pendingProfileTileClick = 0;
                        }
                        Left = _dragStartLeft + (movePt.X - _dragStartCursorX) / _dragDpiX;
                        Top  = _dragStartTop  + (movePt.Y - _dragStartCursorY) / _dragDpiY;
                        // Im Solo-Andock-Modus haengt das Eingabefenster
                        // direkt am Pillar — wir ziehen es 1:1 mit. Sonst
                        // bleibt das Promtboard an der linken Pillar-Kante.
                        if (_inputSoloDock && _promptPanel?.InputWindow is { } iw)
                        {
                            iw.FollowOverlayDrag(this);
                        }
                        else if (_promptPanel is not null && _promptPanel.IsVisible)
                        {
                            PositionPromptPanel();
                        }
                    }
                    break;

                case Win32.WM_RBUTTONUP:
                    if (_isDragging)
                    {
                        _isDragging = false;
                        Win32.ReleaseCapture();
                        if (_pendingProfileTileClick > 0)
                        {
                            // War ein kurzer Klick auf ein Profile-Tile —
                            // kein Drag, also Profil ohne Re-Correct aktivieren.
                            int p = _pendingProfileTileClick;
                            _pendingProfileTileClick = 0;
                            SwitchProfileWithoutReCorrect(p);
                        }
                        else
                        {
                            _manuallyPositioned = true;
                        }
                        handled = true;
                    }
                    break;
            }
            return IntPtr.Zero;
        }

        // ── Terminal watcher callbacks ──

        private void OnTerminalActivated(IntPtr terminalHwnd)
        {
            // Wechsel zurueck zum Terminal: laufenden Hide-Delay-Timer
            // sofort abbrechen, sonst wuerde das Overlay nach Ablauf der
            // 5 Sekunden trotzdem noch verschwinden — auch wenn der Benutzer
            // laengst zurueck im Terminal arbeitet.
            _hideDelayTimer.Stop();

            if (!_manuallyPositioned)
            {
                var workArea = TerminalWatcher.GetMonitorWorkArea(terminalHwnd);
                // Frank's exact spec (2026-04-26):
                //   • 0,7 cm from the right edge → 7 mm × 3,78 ≈ 27 WPF-px
                //   • 1,5 cm from the top edge   → 15 mm × 3,78 ≈ 57 WPF-px
                // WPF pixels are device-independent (96 per inch), so these
                // millimeter targets stay visually constant across DPI
                // settings. Saved-position path above still wins once the
                // user drags the pillar manually.
                Left = workArea.X + workArea.Width - Width - 27;
                Top  = workArea.Y + 57;
            }

            if (!IsVisible)
            {
                Show();
                Console.WriteLine("Overlay: visible (terminal active)");
            }

            // Star toggle is on but the panel was hidden during a previous
            // terminal-deactivation? Bring it back alongside the pillar so
            // both windows behave as a single unit per user expectation.
            // ABER: Im Solo-Andock-Modus ist das Promtboard absichtlich
            // ausgeblendet. Dann nicht das Promtboard reaktivieren, sondern
            // das Eingabefenster direkt am Pillar wiederherstellen.
            if (_inputSoloDock && _promptPanel?.InputWindow is { } soloInput)
            {
                if (!soloInput.IsVisible) soloInput.Show();
                soloInput.DockToOverlay(this);
            }
            else
            {
                if (alwaysOnActive && _promptPanel is not null && !_promptPanel.IsVisible)
                {
                    PositionPromptPanel();
                    _promptPanel.Show();
                }

                // Floating children (Prompt-Eingabe + Historie) — der Benutzer
                // hatte sie evtl. offen als das Terminal die Aktivitaet verlor.
                // Zurueckholen damit sie genauso wie das Promtboard nur ueber
                // dem Terminal erscheinen, niemals ueber Chrome o.ae.
                _promptPanel?.ShowTransientChildrenIfNeeded();
            }
        }

        private void OnTerminalDeactivated()
        {
            if (_micState == RecordingState.Recording || _isProcessing || isBtwRecording)
                return;

            // Statt sofort zu verstecken: 5-Sekunden-Timer starten. Wechselt
            // der Benutzer in dieser Zeit zurueck zum Terminal, bricht
            // OnTerminalActivated den Timer ab — das Overlay bleibt sichtbar.
            // Stop+Start sorgt dafuer dass das Intervall bei mehrfachem
            // App-Wechsel (z.B. Terminal → Browser → andere App) immer wieder
            // bei 5 s anfaengt, statt vorher abzulaufen.
            _hideDelayTimer.Stop();
            _hideDelayTimer.Start();
            Console.WriteLine("Overlay: hide delay started (5s)");
        }

        /// <summary>
        /// Tatsaechliches Verstecken des Overlays nach Ablauf des Hide-Delay-
        /// Timers. Wird auch direkt aufgerufen wenn die App aufraeumt. Enthaelt
        /// die Logik die frueher in OnTerminalDeactivated stand: erst die
        /// floating Children ausblenden, dann das PromptBoard-Panel, dann das
        /// Pillar selbst. Ein zwischenzeitlicher Mic-Recording-Start macht
        /// hier KEINE Ausnahme mehr — die Pruefung steckt bereits oben in
        /// OnTerminalDeactivated, sodass der Timer gar nicht erst gestartet
        /// wird wenn gerade aufgenommen wird.
        /// </summary>
        private void HideOverlayNow()
        {
            // Floating Children (Eingabe + Historie) ZUERST verstecken —
            // sie sind eigene Top-Level-Windows und werden vom Verstecken
            // des Promtboards nicht automatisch mitgenommen. Wenn wir das
            // hier vergessen, bleiben sie ueber Chrome / VS Code / etc.
            // sichtbar, was Frank explizit nicht will.
            _promptPanel?.HideTransientChildren();

            // Hide (not Close) the panel so its state — selected category,
            // edit-in-progress, scroll position — survives until the user
            // returns to the terminal. Closing would null _promptPanel and
            // also flip alwaysOnActive off via the Closed handler, losing
            // the user's intent.
            if (_promptPanel is not null && _promptPanel.IsVisible)
            {
                _promptPanel.Hide();
            }

            if (IsVisible)
            {
                _manuallyPositioned = false;
                Hide();
                Console.WriteLine("Overlay: hidden (terminal inactive, after 5s delay)");
            }
        }

        // ── Button handlers ──

        // Press-and-hold-Loop fuer den X-Button:
        // - solange linke Maustaste gedrueckt: alle 10 ms eine Zeile loeschen
        // - sequentielle Schleife (nicht Timer) damit ClearLine sauber zu Ende laeuft
        //   bevor das naechste 10ms-Delay startet — keine Ueberlappung moeglich
        // - Loslassen oder vom Button wegbewegen: Schleife stoppt sofort via CancellationToken
        private System.Threading.CancellationTokenSource? _xRepeatCts;
        private bool _xResetScheduled;      // verhindert mehrfaches Faerbe-Reset

        private void XButton_PreviewMouseLeftButtonDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            // Visuelles Feedback: X faerbt sich grau (idle) waehrend der Aktion
            XButton.Background = BtnIdle;
            _xResetScheduled = false;

            // Vorherige Schleife sauber abbrechen, falls noch eine laeuft
            _xRepeatCts?.Cancel();
            _xRepeatCts = new System.Threading.CancellationTokenSource();
            var token = _xRepeatCts.Token;
            var hwnd = _terminalWatcher.ActiveTerminalHwnd;
            hasPastedText = false;

            // Background-Loop: sequentiell ClearLine + 10ms warten, solange gedrueckt
            _ = Task.Run(async () =>
            {
                try
                {
                    while (!token.IsCancellationRequested)
                    {
                        TerminalController.ClearLine(hwnd);
                        if (token.IsCancellationRequested) break;
                        await Task.Delay(10, token).ConfigureAwait(false);
                    }
                }
                catch (TaskCanceledException) { /* erwartet beim Loslassen */ }
                catch (OperationCanceledException) { /* erwartet beim Loslassen */ }
            });
        }

        private void XButton_PreviewMouseLeftButtonUp(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            StopXRepeat();
        }

        private void XButton_MouseLeave(object sender, System.Windows.Input.MouseEventArgs e)
        {
            // Auch wenn der Cursor den Button verlaesst, das Repeat sauber stoppen
            StopXRepeat();
        }

        private void StopXRepeat()
        {
            _xRepeatCts?.Cancel();
            _xRepeatCts = null;

            // Nach kurzer Verzoegerung visueller Reset auf Rot
            if (_xResetScheduled) return;
            _xResetScheduled = true;
            _ = Task.Run(async () =>
            {
                await Task.Delay(500);
                Dispatcher.Invoke(() =>
                {
                    XButton.Background = BtnX;
                    _xResetScheduled = false;
                });
            });
        }

        /// <summary>Main mic button — start / stop recording.</summary>
        private async void BtnMic_Click(object sender, RoutedEventArgs e)
        {
            // Ignore if BTW mic is active
            if (isBtwRecording) return;
            if (_isProcessing)  return;

            if (_micState == RecordingState.Recording)
            {
                // ── Stop recording ──
                var wavFile = _audioRecorder.Stop();
                _ = Task.Run(() => { Console.Beep(660, 120); Console.Beep(440, 120); });
                _pulseTimer.Stop();
                _pulseBright = false;

                if (wavFile == null)
                {
                    SetMicState(RecordingState.Idle);
                    return;
                }

                _isProcessing = true;
                SetMicState(RecordingState.Processing);
                Console.WriteLine("Recording stopped, transcribing...");

                try
                {
                    var transcript = await _groqClient.TranscribeAsync(wavFile);
                    Console.WriteLine($"Transcript: {transcript}");
                    lastRawTranscript = transcript;
                    // Re-Correct-Cache: Roh-Whisper-Text + Zeitstempel merken,
                    // damit der Benutzer per Profil-Klick im Nachhinein eine
                    // andere Korrektur-Brille aufsetzen kann.
                    _lastCorrectableRaw = transcript;

                    string finalText;
                    var activeGemini = geminiEnabled ? await GetActiveGeminiClientAsync() : null;
                    if (activeGemini != null)
                    {
                        Console.WriteLine($"Gemini correction (profile {_activeProfile})...");
                        try
                        {
                            finalText = await activeGemini.CorrectTextAsync(transcript, _activeProfile);
                            Console.WriteLine($"Corrected: {finalText}");
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"Gemini error: {ex.Message}, using raw text");
                            finalText = transcript;
                        }
                    }
                    else
                    {
                        finalText = transcript;
                    }

                    // Wrap the dictation with PromptBoard always-on prompts
                    // when the star toggle is active. Pre-prompts go before,
                    // post-prompts after; both are independent so a prompt
                    // can wrap the dictation on both sides if both flags
                    // are set. Only on the first paste per line — follow-ups
                    // are appended to the existing line without wrapping.
                    // Wenn das neue Prompt-Eingabefenster offen ist (Stern an
                    // im Promptboard), wandert das Voice-Transkript dort hinein
                    // statt direkt in die CLI. Der Benutzer kann den Text dann
                    // editieren oder Enter druecken — und der Submit-Pfad
                    // unten baut Pre/Mitte/Post zusammen UND legt den Eintrag
                    // in der Historie ab. So landen auch eingesprochene Prompts
                    // in der Historie.
                    if (_promptPanel?.IsInputWindowVisible == true)
                    {
                        _promptPanel.RouteVoiceTextToInput(finalText, autoEnterEnabled);
                        SetMicState(RecordingState.Success);
                        Console.WriteLine($"Voice text routed to PromptInputWindow (autoSubmit={autoEnterEnabled}).");
                    }
                    else
                    {
                        if (!hasPastedText)
                        {
                            var (preFix, postFix) = await BuildAlwaysOnWrappersAsync();
                            if (!string.IsNullOrEmpty(preFix))
                                finalText = preFix + finalText;
                            if (!string.IsNullOrEmpty(postFix))
                                finalText = finalText + postFix;
                        }

                        // Always append " ; " after the dictated text — inline
                        // space + semicolon + space marks every dictation as
                        // its own task without forcing line breaks in the
                        // terminal. Applies regardless of auto-enter.
                        finalText = finalText + " ; ";

                        TerminalController.PasteText(finalText, _terminalWatcher.ActiveTerminalHwnd, autoEnterEnabled);
                        SetMicState(RecordingState.Success);
                        Console.WriteLine("Text inserted");

                        // Track paste state
                        hasPastedText = !autoEnterEnabled;
                        if (autoEnterEnabled)
                            hasPastedText = false;
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Transcription error: {ex.Message}");
                    SetMicState(RecordingState.Error);
                }
                finally
                {
                    _isProcessing = false;
                    ScheduleReset();

                    try { if (wavFile != null) File.Delete(wavFile); }
                    catch { /* ignore */ }
                }
            }
            else
            {
                // ── Start recording ──
                // KRITISCH: Reset-Timer aus der vorherigen Aufnahme stoppen.
                // Sonst feuert er ggf. mitten in der NEUEN Aufnahme und setzt
                // _micState auf Idle zurueck — UI sieht aus als waere die
                // Aufnahme aus, _audioRecorder laeuft aber weiter (State-Drift
                // bei schnellen aufeinanderfolgenden Aufnahmen).
                _resetTimer.Stop();
                try
                {
                    _audioRecorder.Start();
                    SetMicState(RecordingState.Recording);
                    _ = Task.Run(() => Console.Beep(880, 150));
                    Console.WriteLine("Recording started");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Microphone error: {ex.Message}");
                    SetMicState(RecordingState.Error);
                    ScheduleReset();
                }
            }
        }

        /// <summary>BTW mic button — record and prepend "/btw " to the text.</summary>
        private async void BtnBtw_Click(object sender, RoutedEventArgs e)
        {
            // Ignore if main mic is active
            if (_micState == RecordingState.Recording) return;
            if (_isProcessing) return;

            if (isBtwRecording)
            {
                // ── Stop BTW recording ──
                var wavFile = _audioRecorder.Stop();
                _ = Task.Run(() => { Console.Beep(660, 120); Console.Beep(440, 120); });
                _btwPulseTimer.Stop();
                _btwPulseBright = false;
                isBtwRecording = false;

                if (wavFile == null)
                {
                    SetBtwMicState(RecordingState.Idle);
                    return;
                }

                _isProcessing = true;
                SetBtwMicState(RecordingState.Processing);
                Console.WriteLine("BTW recording stopped, transcribing...");

                try
                {
                    var transcript = await _groqClient.TranscribeAsync(wavFile);
                    Console.WriteLine($"BTW transcript: {transcript}");
                    // Re-Correct-Cache fuer die BTW-Spur ebenfalls fuellen
                    _lastCorrectableRaw = transcript;

                    string finalText;
                    var btwGemini = geminiEnabled ? await GetActiveGeminiClientAsync() : null;
                    if (btwGemini != null)
                    {
                        Console.WriteLine($"BTW Gemini correction (profile {_activeProfile})...");
                        try
                        {
                            finalText = await btwGemini.CorrectTextAsync(transcript, _activeProfile);
                            Console.WriteLine($"BTW corrected: {finalText}");
                        }
                        catch (Exception ex)
                        {
                            Console.WriteLine($"BTW Gemini error: {ex.Message}, using raw text");
                            finalText = transcript;
                        }
                    }
                    else
                    {
                        finalText = transcript;
                    }

                    // BTW prefix stays simple (no always-on chaining here —
                    // BTW lines are short asides, not full prompts).
                    const string btwMarker = "/btw ";

                    // Prepend space if text was already pasted on this line, then prefix
                    if (hasPastedText)
                        finalText = " " + btwMarker + finalText;
                    else
                        finalText = btwMarker + finalText;

                    TerminalController.PasteText(finalText, _terminalWatcher.ActiveTerminalHwnd, autoEnterEnabled);
                    SetBtwMicState(RecordingState.Success);
                    Console.WriteLine("BTW text inserted");

                    hasPastedText = !autoEnterEnabled;
                    if (autoEnterEnabled)
                        hasPastedText = false;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"BTW transcription error: {ex.Message}");
                    SetBtwMicState(RecordingState.Error);
                }
                finally
                {
                    _isProcessing = false;

                    // Reset BTW button to idle after 3 s
                    await Task.Delay(3000);
                    if (!isBtwRecording)
                        SetBtwMicState(RecordingState.Idle);

                    try { if (wavFile != null) File.Delete(wavFile); }
                    catch { /* ignore */ }
                }
            }
            else
            {
                // ── Start BTW recording ──
                try
                {
                    isBtwRecording = true;
                    _audioRecorder.Start();
                    SetBtwMicState(RecordingState.Recording);
                    _ = Task.Run(() => Console.Beep(880, 150));
                    Console.WriteLine("BTW recording started");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"BTW microphone error: {ex.Message}");
                    isBtwRecording = false;
                    SetBtwMicState(RecordingState.Error);

                    await Task.Delay(3000);
                    SetBtwMicState(RecordingState.Idle);
                }
            }
        }

        /// <summary>W button — undo Gemini correction, paste raw Whisper text.</summary>
        private void BtnWhisperUndo_Click(object sender, RoutedEventArgs e)
        {
            if (lastRawTranscript == null) return;

            TerminalController.ClearLine(_terminalWatcher.ActiveTerminalHwnd);
            System.Threading.Thread.Sleep(100);
            TerminalController.PasteText(lastRawTranscript, _terminalWatcher.ActiveTerminalHwnd);
            hasPastedText = true;
            Console.WriteLine($"Whisper raw text inserted: {lastRawTranscript}");

            lastRawTranscript = null;
        }

        /// <summary>G button — toggle Gemini on/off.
        /// G=on → GButton green, WButton dark, aktives Profil leuchtet goldgelb.
        /// G=off → GButton dark, WButton green (Whisper-raw active), ALLE Profil-
        /// Tiles werden dunkel weil ohne Gemini kein Profil wirken kann.</summary>
        private void BtnGemini_Click(object sender, RoutedEventArgs e)
        {
            if (_geminiClient == null) return;
            geminiEnabled = !geminiEnabled;

            if (geminiEnabled)
            {
                GButton.Background = ToggleOn;
                WButton.Background = ToggleOff;
            }
            else
            {
                GButton.Background = ToggleOff;
                WButton.Background = ToggleOn;
            }

            // Profil-Tiles synchron zum G-Status updaten: bei G=off werden
            // alle dunkel, bei G=on leuchtet das gespeicherte aktive Profil.
            SetActiveProfile(_activeProfile);

            Console.WriteLine($"Gemini {(geminiEnabled ? "ON" : "OFF")}");
        }

        /// <summary>
        /// Liefert alle Profile-Buttons als Array (Index 0 = Profil 1).
        /// Lazy initialisiert beim ersten Zugriff, weil x:Name-Felder erst
        /// nach InitializeComponent verfuegbar sind.
        /// </summary>
        private System.Windows.Controls.Button[]? _profileButtonsCache;
        private System.Windows.Controls.Button[] ProfileButtons
        {
            get
            {
                _profileButtonsCache ??= new[]
                {
                    Profile1Button, Profile2Button, Profile3Button,
                    Profile4Button, Profile5Button, Profile6Button,
                    Profile7Button, Profile8Button, Profile9Button, Profile10Button
                };
                return _profileButtonsCache;
            }
        }

        /// <summary>
        /// Setzt das aktive Gemini-Korrektur-Profil (1-10) und aktualisiert
        /// die zehn Profil-Tiles farblich. Aktiv = goldgelb (BtnMicIdle),
        /// inaktiv = dunkel (ToggleOff).
        ///
        /// Wichtige Regel: Wenn Gemini ausgeschaltet ist (geminiEnabled=false,
        /// W-Toggle aktiv), wird KEIN Tile goldgelb — alle Profile sind dunkel.
        /// Erst wenn Gemini wieder an ist, leuchtet das gespeicherte aktive
        /// Profil. So ist auf einen Blick klar: sobald W (Whisper-Raw) aktiv
        /// ist, hat kein Profil Wirkung.
        /// </summary>
        private void SetActiveProfile(int profile)
        {
            _activeProfile = profile;
            var buttons = ProfileButtons;
            bool showActiveTile = geminiEnabled;
            for (int i = 0; i < buttons.Length; i++)
            {
                bool isActive = showActiveTile && (i + 1) == profile;
                buttons[i].Background = isActive ? BtnMicIdle : ToggleOff;
                UpdateProfileButtonForeground(buttons[i], isActive);
            }
            Console.WriteLine($"Profile {profile} aktiv (gemini={geminiEnabled})");
        }

        private static void UpdateProfileButtonForeground(System.Windows.Controls.Button button, bool active)
        {
            if (button.Content is System.Windows.Controls.TextBlock tb)
            {
                tb.Foreground = active ? System.Windows.Media.Brushes.Black : System.Windows.Media.Brushes.White;
            }
        }

        /// <summary>
        /// Globaler ToolTipOpening-Handler: positioniert jeden Tooltip so,
        /// dass seine rechte Kante immer den gleichen Abstand zur linken
        /// Window-Kante hat (TooltipMargin Pixel) und seine vertikale Mitte
        /// zur Mitte des aktuell gehoverten Buttons passt.
        ///
        /// Default-WPF-Verhalten mit Placement="Left" haengt den Tooltip an
        /// die linke Kante des Buttons — bei den rechten Profil-Tiles laege
        /// der Tooltip dadurch quer ueber dem Overlay (genau dort wo die
        /// Maus den Mic verdeckt). Mit dynamischer Positionierung steht der
        /// Tooltip immer ausserhalb des Overlays an der gleichen x-Position,
        /// egal ob du ueber Mic, Profil-Tile oder Enter-Knopf hoverst.
        /// </summary>
        private const double TooltipMargin = 8.0;
        private const double EstimatedTooltipHeight = 28.0;
        private const int TooltipHoverDelayMs = 100;

        // Eigener Hover-Timer fuer Tooltips. WPF-Standard wartet auf Maus-
        // Stillstand bevor das erste Tooltip kommt — bei Mausbewegung im
        // Button-Bereich erscheint nichts. Mit eigenem Timer erscheint das
        // Tooltip deterministisch nach 250ms nach MouseEnter, unabhaengig
        // davon ob die Maus stillsteht oder weiterbewegt wird.
        private DispatcherTimer? _tooltipHoverTimer;
        private System.Windows.Controls.ToolTip? _pendingTooltip;
        private System.Windows.FrameworkElement? _pendingTooltipTarget;

        /// <summary>
        /// Wandelt alle String-ToolTips der 21 Buttons in echte ToolTip-
        /// Objekte um und haengt jeweils einen Opened-Handler dran, der
        /// die Position dynamisch setzt. Wird einmalig im Constructor nach
        /// InitializeComponent aufgerufen. Der WPF-Implicit-Style auf
        /// TargetType="ToolTip" greift weiterhin (dunkler Hintergrund,
        /// abgerundete Ecken), weil Implicit-Styles auf jede ToolTip-
        /// Instanz im Visual-Tree wirken.
        /// </summary>
        private void WrapStringTooltips()
        {
            var allButtons = new System.Windows.Controls.Button[]
            {
                MicButton, BtwButton, WButton, GButton, XButton,
                CopyButton, PasteButton, ScreenshotButton, InsertScreenshotButton,
                UltrathinkButton, EnterButton,
                Profile1Button, Profile2Button, Profile3Button,
                Profile4Button, Profile5Button, Profile6Button,
                Profile7Button, Profile8Button, Profile9Button, Profile10Button
            };

            // Einen einzigen Hover-Timer fuer das gesamte Window — es kann
            // immer nur ein Tooltip gleichzeitig sichtbar sein, also reicht
            // ein Timer plus zwei Felder fuer die naechste anstehende Anzeige.
            _tooltipHoverTimer = new DispatcherTimer
            {
                Interval = TimeSpan.FromMilliseconds(TooltipHoverDelayMs)
            };
            _tooltipHoverTimer.Tick += OnTooltipHoverTimerTick;

            foreach (var btn in allButtons)
            {
                if (btn.ToolTip is string s)
                {
                    var tip = new System.Windows.Controls.ToolTip { Content = s };
                    var ownerButton = btn; // Closure-Capture
                    tip.Opened += (_, _) => PositionTooltip(tip, ownerButton);
                    btn.ToolTip = tip;

                    // WPF-Auto-Show abschalten — wir steuern die Sichtbarkeit
                    // selbst via MouseEnter/Leave + Timer. Damit umgehen wir
                    // die System-Mouse-Hover-Time, die einen Maus-Stillstand
                    // verlangt. Mit eigenem Timer kommt das Tooltip auch wenn
                    // die Maus im Button-Bereich weiter bewegt wird.
                    System.Windows.Controls.ToolTipService.SetIsEnabled(btn, false);

                    btn.MouseEnter += (_, _) => StartTooltipShow(ownerButton, tip);
                    btn.MouseLeave += (_, _) => CancelTooltipShow(tip);
                }
            }
        }

        /// <summary>
        /// Plant das Anzeigen eines Tooltips nach TooltipHoverDelayMs ms.
        /// Wird bei jedem MouseEnter aufgerufen. Falls bereits ein Timer
        /// laeuft (Maus von einem Button zum naechsten), wird er gestoppt
        /// und neu gestartet.
        /// </summary>
        private void StartTooltipShow(System.Windows.FrameworkElement target,
                                      System.Windows.Controls.ToolTip tip)
        {
            _tooltipHoverTimer?.Stop();
            _pendingTooltip = tip;
            _pendingTooltipTarget = target;
            _tooltipHoverTimer?.Start();
        }

        /// <summary>
        /// Bricht den Hover-Timer ab und schliesst den Tooltip falls er
        /// gerade offen ist. Wird bei jedem MouseLeave aufgerufen.
        /// </summary>
        private void CancelTooltipShow(System.Windows.Controls.ToolTip tip)
        {
            _tooltipHoverTimer?.Stop();
            if (tip.IsOpen) tip.IsOpen = false;
            if (ReferenceEquals(_pendingTooltip, tip))
            {
                _pendingTooltip = null;
                _pendingTooltipTarget = null;
            }
        }

        /// <summary>
        /// Tick-Handler nach Ablauf der Hover-Zeit: oeffnet den anstehenden
        /// Tooltip. PlacementTarget wird auf den Button gesetzt damit die
        /// Position-Berechnung im Opened-Handler die Button-Position kennt.
        /// </summary>
        private void OnTooltipHoverTimerTick(object? sender, EventArgs e)
        {
            _tooltipHoverTimer?.Stop();
            if (_pendingTooltip == null || _pendingTooltipTarget == null) return;

            try
            {
                _pendingTooltip.PlacementTarget = _pendingTooltipTarget;
                _pendingTooltip.Placement = System.Windows.Controls.Primitives.PlacementMode.Left;
                _pendingTooltip.IsOpen = true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Tooltip open error: {ex.Message}");
            }
        }

        /// <summary>
        /// Setzt HorizontalOffset und VerticalOffset eines bereits geoeffneten
        /// ToolTips so, dass:
        /// - die rechte Tooltip-Kante immer TooltipMargin Pixel links vom
        ///   Window-Aussenrand sitzt (gleicher Abstand egal welcher Button)
        /// - die vertikale Tooltip-Mitte mit der Mitte des Buttons zusammenfaellt
        ///
        /// Wird im Opened-Handler aufgerufen — zu diesem Zeitpunkt ist
        /// tooltip.ActualHeight bereits gemessen und der HorizontalOffset
        /// wirkt sofort sichtbar (das Popup wird bei Property-Change
        /// neu positioniert).
        /// </summary>
        private void PositionTooltip(System.Windows.Controls.ToolTip tooltip,
                                     System.Windows.FrameworkElement target)
        {
            System.Windows.Point windowOrigin;
            try
            {
                windowOrigin = target.TranslatePoint(new System.Windows.Point(0, 0), this);
            }
            catch
            {
                return;
            }

            // Horizontal: Default Placement=Left → Tooltip-rechte-Kante an
            // Button-linke-Kante (= windowOrigin.X). Wir wollen Tooltip-rechte-
            // Kante bei -TooltipMargin (Pixel links vom Window). Offset =
            // (-TooltipMargin) - windowOrigin.X.
            tooltip.HorizontalOffset = -TooltipMargin - windowOrigin.X;

            // Vertikal: Default = Tooltip-Top am Button-Top. Wir wollen
            // Tooltip-Mitte = Button-Mitte. Im Opened-Event ist ActualHeight
            // bereits final, also nutzen wir den echten Wert.
            double tooltipHeight = tooltip.ActualHeight > 0
                ? tooltip.ActualHeight
                : EstimatedTooltipHeight;
            tooltip.VerticalOffset = (target.ActualHeight - tooltipHeight) / 2.0;
        }

        private async void BtnProfile1_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(1);
        private async void BtnProfile2_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(2);
        private async void BtnProfile3_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(3);
        private async void BtnProfile4_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(4);
        private async void BtnProfile5_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(5);
        private async void BtnProfile6_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(6);
        private async void BtnProfile7_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(7);
        private async void BtnProfile8_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(8);
        private async void BtnProfile9_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(9);
        private async void BtnProfile10_Click(object sender, RoutedEventArgs e) => await SwitchProfileAsync(10);

        // ── RECHTSKLICK auf Profil-Tile ──
        // Aktiviert das Profil ohne den Cache durch Gemini zu schicken. Die
        // letzte Whisper-Nachricht bleibt unangetastet im Zwischenspeicher
        // und kann jederzeit spaeter per Linksklick durch dieses (oder ein
        // anderes) Profil korrigiert werden.
        //
        // Implementierung via PreviewMouseDown ist die robusteste Variante:
        // das Event feuert in der Tunneling-Phase, BEVOR der Button-interne
        // Handler die Chance bekommt etwas zu konsumieren. Wir filtern auf
        // ChangedButton == Right und markieren das Event als Handled, damit
        // der Button nicht zusaetzlich noch ein Click-Event ausloest.
        private void BtnProfile1_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(1, e);
        private void BtnProfile2_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(2, e);
        private void BtnProfile3_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(3, e);
        private void BtnProfile4_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(4, e);
        private void BtnProfile5_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(5, e);
        private void BtnProfile6_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(6, e);
        private void BtnProfile7_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(7, e);
        private void BtnProfile8_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(8, e);
        private void BtnProfile9_PreviewMouseDown(object sender, MouseButtonEventArgs e)  => HandleProfileRightClick(9, e);
        private void BtnProfile10_PreviewMouseDown(object sender, MouseButtonEventArgs e) => HandleProfileRightClick(10, e);

        private void HandleProfileRightClick(int profile, MouseButtonEventArgs e)
        {
            if (e.ChangedButton != MouseButton.Right) return;
            SwitchProfileWithoutReCorrect(profile);
            e.Handled = true;
        }

        /// <summary>
        /// Hit-Test fuer den Mauszeiger auf Profile-Tiles. Gibt die Profil-
        /// Nummer (1-10) zurueck wenn der Cursor gerade auf einem Tile liegt,
        /// sonst 0. Wird beim Rechtsklick-Down im WndProc gerufen, um zwischen
        /// "kurzer Klick auf Tile" (Profil-Wechsel) und "Drag auf Hintergrund"
        /// (Pillar verschieben) zu unterscheiden.
        ///
        /// Hintergrund: Der Window-Level WndProc-Hook faengt ALLE Right-
        /// Mouse-Down-Events auf Win32-Ebene ab — dadurch erreichen sie nie
        /// WPFs Event-Routing und keine PreviewMouseDown-Handler an einzelnen
        /// Buttons koennen feuern. Der Hit-Test hier ersetzt die WPF-
        /// Routing-Schicht fuer den Spezialfall der Profile-Tiles.
        /// </summary>
        private int HitTestProfileTile()
        {
            if (!Win32.GetCursorPos(out var screenPt)) return 0;
            System.Windows.Point relativePos;
            try
            {
                relativePos = PointFromScreen(new System.Windows.Point(screenPt.X, screenPt.Y));
            }
            catch
            {
                return 0;
            }

            var hit = System.Windows.Media.VisualTreeHelper.HitTest(this, relativePos);
            if (hit?.VisualHit == null) return 0;

            DependencyObject? current = hit.VisualHit;
            while (current != null)
            {
                if (current is System.Windows.Controls.Button btn && !string.IsNullOrEmpty(btn.Name))
                {
                    var match = System.Text.RegularExpressions.Regex.Match(
                        btn.Name, @"^Profile(\d+)Button$");
                    if (match.Success &&
                        int.TryParse(match.Groups[1].Value, out int profileNum))
                    {
                        return profileNum;
                    }
                }
                current = System.Windows.Media.VisualTreeHelper.GetParent(current);
            }
            return 0;
        }

        /// <summary>
        /// RECHTSKLICK-Variante: aktiviert Gemini falls aus, setzt das aktive
        /// Profil — fuehrt aber KEINEN Re-Correct durch. Der Whisper-Cache
        /// bleibt unveraendert und kann spaeter per Linksklick auf irgendein
        /// Profil-Tile noch durchgeschickt werden.
        /// </summary>
        private void SwitchProfileWithoutReCorrect(int newProfile)
        {
            if (!geminiEnabled && _geminiClient != null)
            {
                geminiEnabled = true;
                GButton.Background = ToggleOn;
                WButton.Background = ToggleOff;
                Console.WriteLine("Gemini auto-eingeschaltet durch Profil-Rechtsklick");
            }
            SetActiveProfile(newProfile);
            Console.WriteLine($"Profil {newProfile} aktiv (Rechtsklick — kein Re-Correct)");
        }

        /// <summary>
        /// LINKSKLICK auf Profil-Tile: aktiviert das Profil UND schickt — falls
        /// der zuletzt transkribierte Whisper-Text noch im Cache liegt — diesen
        /// Text durch das neue Profil. Die alte Eingabezeile wird dabei voll-
        /// staendig geloescht (auch mehrzeilig per ClearAllInput), danach wird
        /// der frisch korrigierte Text reingepastet, mit Auto-Submit wenn der
        /// Enter-Toggle aktiv ist.
        ///
        /// Re-Correct laeuft wenn:
        /// - Roh-Whisper-Text liegt im Cache (_lastCorrectableRaw)
        /// - Gemini ist aktiviert (sonst gibt es nichts zu korrigieren)
        /// - Aufnahme laeuft NICHT gerade (sonst stoeren wir die laufende UI)
        ///
        /// Kein Zeitlimit mehr: der Cache bleibt erhalten bis eine neue
        /// Aufnahme ihn ueberschreibt. Wer das Profil ohne Re-Correct setzen
        /// will, nutzt den Rechtsklick (SwitchProfileWithoutReCorrect).
        ///
        /// Bewusst KEINE Aenderung am Mic-State: das wuerde die Aufnahme-
        /// Anzeige ueberschreiben. Stattdessen wird das geklickte Profil-Tile
        /// kurz orange als visueller Indikator, dass Re-Correct laeuft.
        /// </summary>
        private async Task SwitchProfileAsync(int newProfile)
        {
            int oldProfile = _activeProfile;

            // Auto-Aktivierung: Linksklick zeigt klare Absicht, Gemini-
            // Korrektur zu wollen. Falls G gerade aus war (W-Modus, Default
            // seit Whisper-First), schalten wir Gemini automatisch ein.
            bool didAutoEnableGemini = false;
            if (!geminiEnabled && _geminiClient != null)
            {
                geminiEnabled = true;
                didAutoEnableGemini = true;
                GButton.Background = ToggleOn;
                WButton.Background = ToggleOff;
                Console.WriteLine("Gemini auto-eingeschaltet durch Profil-Klick");
            }

            SetActiveProfile(newProfile);

            // Wenn gerade aufgenommen wird: nur Profil setzen, sonst nichts.
            if (_micState == RecordingState.Recording) return;
            // Gleiches Profil = no-op — AUSSER Gemini wurde gerade auto-
            // aktiviert. Dann ist es der Erst-Klick im Whisper-Mode und der
            // Re-Correct soll trotzdem laufen.
            if (!didAutoEnableGemini && newProfile == oldProfile) return;
            if (!geminiEnabled) return;
            if (string.IsNullOrEmpty(_lastCorrectableRaw)) return;

            var rawText = _lastCorrectableRaw;
            // Geklicktes Profile-Tile aus dem Array holen (Index = profile - 1).
            // Profile 1-10 erlaubt, alles andere wird ignoriert.
            var buttons = ProfileButtons;
            var clickedTile = (newProfile >= 1 && newProfile <= buttons.Length)
                ? buttons[newProfile - 1]
                : null;

            try
            {
                var gemini = await GetActiveGeminiClientAsync();
                if (gemini == null) return;

                Console.WriteLine($"Re-Correct: profile {oldProfile} -> {newProfile}");
                // Visueller Indikator: das geklickte Tile waehrend der Re-
                // Correct-Phase orange faerben (Processing-Look). Nach Erfolg
                // setzen wir es zurueck auf den aktiven goldgelben Look.
                if (clickedTile != null) clickedTile.Background = BtnProcessing;

                string corrected = await gemini.CorrectTextAsync(rawText, newProfile);

                // Wrappers (Pre/Post-Prompts) wieder anwenden, falls aktiv —
                // sonst geht der always-on-Kontext beim Re-Correct verloren.
                var (preFix, postFix) = await BuildAlwaysOnWrappersAsync();
                if (!string.IsNullOrEmpty(preFix)) corrected = preFix + corrected;
                if (!string.IsNullOrEmpty(postFix)) corrected = corrected + postFix;
                corrected = corrected + " ; ";

                // Eingabezeile vollstaendig loeschen (mehrzeilig sicher) und
                // dann den neu korrigierten Text reinpaten. AutoEnter wird
                // respektiert: ist der Enter-Toggle aktiv, wird die Frage
                // direkt abgeschickt — sonst nur in die Befehlszeile kopiert.
                var hwnd = _terminalWatcher.ActiveTerminalHwnd;
                TerminalController.ClearAllInput(hwnd);
                await Task.Delay(120);
                TerminalController.PasteText(corrected, hwnd, autoEnterEnabled);

                hasPastedText = true;
                Console.WriteLine($"Re-Correct ok ({corrected.Length} chars)");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Re-Correct error: {ex.Message}");
            }
            finally
            {
                // Tile zurueck auf aktiven Look (goldgelb) — egal ob Erfolg
                // oder Fehler. Nutzt SetActiveProfile damit Foreground-Farbe
                // (Schrift) konsistent zum Background bleibt.
                if (clickedTile != null) SetActiveProfile(_activeProfile);
            }
        }

        /// <summary>Enter button — toggle auto-enter.
        /// ON→OFF: button goes dark.
        /// OFF→ON: button goes orange AND fires Return immediately.</summary>
        private void BtnAutoEnter_Click(object sender, RoutedEventArgs e)
        {
            // Wenn das Prompt-Eingabefenster Text enthaelt, wird der Klick
            // als "Send"-Aktion interpretiert: Text einfuegen + Return druecken,
            // unabhaengig vom Toggle-Zustand. Erst wenn keine Eingabe da ist,
            // greift die alte Toggle-Logik.
            if (_promptPanel != null)
            {
                _forceReturnOnNextSubmit = true;
                if (_promptPanel.TrySubmitInputText())
                {
                    Console.WriteLine("Enter-Button: Prompt-Eingabe wurde gesendet (force Return).");
                    return;
                }
                // Kein Text vorhanden — Flag wieder zuruecksetzen, sonst
                // wuerde ein nachfolgender Voice-Submit ungewollt forciert.
                _forceReturnOnNextSubmit = false;
            }

            if (autoEnterEnabled)
            {
                // Turn OFF
                autoEnterEnabled = false;
                EnterButton.Background = ToggleOff;
                hasPastedText = false;
                Console.WriteLine("Auto-enter OFF");
            }
            else
            {
                // Turn ON → fire Return immediately
                autoEnterEnabled = true;
                EnterButton.Background = BtnProcessing;
                hasPastedText = false;
                Console.WriteLine("Auto-enter ON — firing Return");

                // Fire a Return key press into the active terminal
                TerminalController.PressReturn(_terminalWatcher.ActiveTerminalHwnd);
            }
        }

        /// <summary>Star button — toggles the full PromptBoard integration:
        /// opens/closes the side panel AND enables/disables the always-on
        /// prefix. Default-Einstieg seit Aenderung 2026-04-27: das Promtboard
        /// wird NICHT mehr direkt gezeigt; nur das Prompt-Eingabefenster
        /// dockt direkt am Pillar an. Das Promtboard kann der Benutzer bei
        /// Bedarf ueber den Stern-Toggle in der Eingabe-Toolbar dazu-
        /// schalten (= ApplySoloDockMode(false)).</summary>
        private void BtnUltrathink_Click(object sender, RoutedEventArgs e)
        {
            alwaysOnActive = !alwaysOnActive;

            if (alwaysOnActive)
            {
                UltrathinkButton.Background = BtnUltrathinkOn;
                UltrathinkStar.Fill = StarGold;
                ShowPromptInputDockedToOverlay();
            }
            else
            {
                UltrathinkButton.Background = ToggleOff;
                UltrathinkStar.Fill = StarMuted;
                HidePromptPanel();
            }

            Console.WriteLine($"PromptBoard panel {(alwaysOnActive ? "OPEN" : "CLOSED")}");
        }

        /// <summary>
        /// Erstellt die PromptBoardPanel-Instanz und verdrahtet alle Events,
        /// macht das Fenster aber NICHT sichtbar. Wird sowohl vom Solo-
        /// Modus-Einstieg (nur Eingabefenster sichtbar) als auch vom
        /// klassischen Show genutzt — so bleiben Subscriptions an einer
        /// Stelle und sicher.
        /// </summary>
        private void EnsurePromptPanelInstance()
        {
            if (_promptPanel is not null) return;

            _promptPanel = new PromptBoardPanel();
            _promptPanel.PromptInsertRequested += OnPromptPanelInsert;
            _promptPanel.InputSubmitRequested  += OnInputSubmit;
            // Wird gefeuert nachdem der Benutzer einen Historie-Eintrag
            // im Editor-Dialog gespeichert hat — Cloud-Upload anstossen.
            _promptPanel.HistorySyncRequested  += () => _ = TryUploadHistoryAsync();
            // Right-click drag on the panel itself moves both the
            // panel (handled inside) and this pillar window — slide
            // the pillar to stay glued to the panel's right edge.
            _promptPanel.PanelDragged += () =>
            {
                if (_promptPanel is null) return;
                Left = _promptPanel.Left + _promptPanel.Width + 4;
                Top  = _promptPanel.Top;
                _manuallyPositioned = true;
            };
            // Stern in der Eingabe-Toolbar: Solo-Andock-Modus umschalten.
            // Im Solo-Modus wird das Promtboard ausgeblendet und das
            // Eingabefenster dockt direkt an die linke Pillar-Kante.
            // Beim Zurueckschalten erscheint das Promtboard wieder und
            // das Eingabefenster rutscht zurueck an dessen linken Rand.
            _promptPanel.SoloDockToggleRequested += ApplySoloDockMode;
            _promptPanel.Closed += (_, _) =>
            {
                _promptPanel = null;
                // If the panel was closed by something other than the
                // star toggle, keep the toggle state in sync.
                if (alwaysOnActive)
                {
                    alwaysOnActive = false;
                    UltrathinkButton.Background = ToggleOff;
                    UltrathinkStar.Fill = StarMuted;
                }
            };
        }

        /// <summary>
        /// Klassischer Show-Pfad: Promtboard sichtbar links neben dem Pillar,
        /// Eingabefenster (falls offen) links neben dem Promtboard. Wird
        /// nur noch indirekt benutzt — z.B. wenn der Benutzer im Solo-Modus
        /// ueber den Stern in der Eingabe-Toolbar zurueck in den Normalmodus
        /// schaltet (siehe ApplySoloDockMode mit active=false).
        /// </summary>
        private void ShowPromptPanel()
        {
            EnsurePromptPanelInstance();
            if (_promptPanel is null) return;

            PositionPromptPanel();
            _promptPanel.Show();
            _ = _promptPanel.RefreshAsync();
        }

        /// <summary>
        /// Solo-Modus-Einstieg: erstellt das Promtboard im Hintergrund (ohne
        /// es sichtbar zu machen), oeffnet das Prompt-Eingabefenster und
        /// dockt es direkt an die linke Pillar-Kante an. Der Benutzer kann
        /// das Promtboard danach ueber den Stern-Toggle in der Eingabe-
        /// Toolbar einblenden — bis dahin nimmt nur die Eingabe Platz weg.
        /// </summary>
        private void ShowPromptInputDockedToOverlay()
        {
            EnsurePromptPanelInstance();
            if (_promptPanel is null) return;

            // Daten frisch laden, auch wenn das Panel nicht sichtbar ist —
            // sonst ist der erste Tab-Klick im spaeter eingeblendeten
            // Promtboard ohne Inhalt.
            _ = _promptPanel.RefreshAsync();

            // Eingabefenster oeffnen (es dockt zunaechst ans Promtboard an
            // — egal, wir ueberschreiben gleich auf Pillar-Andock).
            _promptPanel.EnsureInputWindowOpen();

            var input = _promptPanel.InputWindow;
            if (input is null) return;

            _inputSoloDock = true;
            input.DockToOverlay(this);
            input.SetSoloDockState(true);
        }

        private void HidePromptPanel()
        {
            if (_promptPanel is null) return;
            // Solo-Andock-Flag zuruecksetzen — beim naechsten Ultrathink-On
            // startet das Promtboard wieder im Normalmodus.
            _inputSoloDock = false;
            var p = _promptPanel;
            _promptPanel = null;
            p.Close();
        }

        private void PositionPromptPanel()
        {
            if (_promptPanel is null) return;
            // Dock the panel directly to the LEFT of the pillar with a
            // 4-pixel seam. The VTO typically sits at the right screen edge,
            // so docking to the left keeps the panel on-screen. Match the
            // pillar's HEIGHT exactly so the two floating windows visually
            // line up — the user asked for a uniform vertical extent so
            // the panel doesn't look stubby next to the bar (or vice-versa).
            _promptPanel.Height = Height;
            _promptPanel.Left = Left - _promptPanel.Width - 4;
            _promptPanel.Top = Top;
        }

        /// <summary>
        /// Setzt den Solo-Andock-Modus um (Stern-Klick im Eingabefenster).
        /// <list type="bullet">
        /// <item>active=true: Promtboard ausblenden und Eingabe direkt
        /// links an den Pillar andocken (mit 4-Pixel-Naht, Hoehe = Pillar).</item>
        /// <item>active=false: Promtboard wieder einblenden und re-positionieren;
        /// das Eingabefenster zieht via PanelDragged-Kette automatisch an
        /// den linken Rand des Promtboards zurueck.</item>
        /// </list>
        /// Das Eingabefenster wird per <see cref="PromptInputWindow.SetSoloDockState"/>
        /// nachgezogen damit das Stern-Visual den neuen Zustand zeigt.
        /// </summary>
        private void ApplySoloDockMode(bool active)
        {
            if (_promptPanel is null) return;
            var input = _promptPanel.InputWindow;
            if (input is null) return;

            if (active)
            {
                // Promtboard ausblenden — wir nutzen Window.Hide, NICHT Close,
                // damit die Instanz und der ganze State (Kategorien, Prompts,
                // Subscriptions) erhalten bleiben.
                _promptPanel.Hide();
                _inputSoloDock = true;
                input.DockToOverlay(this);
            }
            else
            {
                _inputSoloDock = false;
                _promptPanel.Show();
                PositionPromptPanel();
                // Das Eingabefenster folgt dem Promtboard automatisch via
                // LocationChanged → RefollowChildren in PromptBoardPanel.
                // Trotzdem explizit redocken damit die Naht sofort sitzt.
                input.DockTo(_promptPanel);
            }

            input.SetSoloDockState(active);
        }

        private void OnPromptPanelInsert(string text)
        {
            if (string.IsNullOrEmpty(text)) return;
            try
            {
                TerminalController.PasteText(text, _terminalWatcher.ActiveTerminalHwnd, autoEnterEnabled);
                Console.WriteLine($"Panel prompt inserted: {text.Length} chars.");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Panel insert failed: {ex.Message}");
            }
        }

        /// <summary>
        /// Wird aus dem Prompt-Eingabefenster ausgeloest wenn der Benutzer
        /// Enter drueckt. Der uebergebene Text ist die reine Mitte (was der
        /// Benutzer getippt oder per Voice eingespielt hat). Wir bauen Pre +
        /// Mitte + Post mit ` ; ` als Trenner zusammen, fuegen alles in die
        /// CLI ein und respektieren den Auto-Enter-Toggle des Voice-Overlays
        /// — so geht der Prompt direkt an die KI ab, wenn Auto-Enter an ist.
        /// Phase 4 wird hier zusaetzlich den Eintrag in die Historie schreiben.
        /// </summary>
        private async void OnInputSubmit(string middleText)
        {
            try
            {
                string mid = (middleText ?? string.Empty).Trim();
                var (preFix, postFix) = await BuildAlwaysOnWrappersAsync();

                // PromptChainBuilder.Build joined nur zwischen den Eintraegen
                // (kein Leading/Trailing-Trenner) — wir koennen Pre/Mitte/Post
                // also direkt mit " ; " verbinden, leere Bloecke werden
                // automatisch uebersprungen.
                var parts = new System.Collections.Generic.List<string>();
                if (!string.IsNullOrWhiteSpace(preFix))  parts.Add(preFix);
                if (!string.IsNullOrWhiteSpace(mid))     parts.Add(mid);
                if (!string.IsNullOrWhiteSpace(postFix)) parts.Add(postFix);

                if (parts.Count == 0)
                {
                    Console.WriteLine("OnInputSubmit: nothing to insert (empty).");
                    return;
                }

                string final = string.Join(" ; ", parts);
                // Force-Return uebersteuert das autoEnter-Toggle wenn der
                // Submit aus einem expliziten Enter-Button-Klick kommt.
                bool effectiveAutoEnter = autoEnterEnabled || _forceReturnOnNextSubmit;
                _forceReturnOnNextSubmit = false;
                TerminalController.PasteText(final, _terminalWatcher.ActiveTerminalHwnd, effectiveAutoEnter);
                Console.WriteLine($"Input submit: {final.Length} chars (autoEnter={effectiveAutoEnter}).");
                hasPastedText = !effectiveAutoEnter;

                // Historie-Eintrag asynchron schreiben — Submit darf NICHT
                // auf Gemini warten, weil sonst der Tipp-Flow ruckelt. Der
                // Eintrag bekommt vorerst einen Fallback-Titel (erste 4
                // Woerter), Gemini ueberschreibt ihn sobald der KI-Titel da
                // ist. So sieht der Benutzer den Eintrag SOFORT in der
                // Historie und der KI-Titel erscheint nachtraeglich.
                _ = WriteHistoryAsync(mid);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"OnInputSubmit failed: {ex.Message}");
            }
        }

        /// <summary>
        /// Speichert die Mitte (was der Benutzer getippt oder per Voice
        /// eingespielt hat) in der Prompt-Historie. Erst sofort mit einem
        /// Fallback-Titel (erste 4 Woerter), dann holt sich Gemini im
        /// Hintergrund einen praeziseren Titel und ueberschreibt den
        /// Eintrag. Der Submit-Pfad blockiert nie auf das KI-Ergebnis.
        /// </summary>
        private async Task WriteHistoryAsync(string middleText)
        {
            try
            {
                string fallbackTitle = GeminiClient.FallbackTitleFromText(middleText);
                var entry = await _historyService.AppendAsync(middleText, fallbackTitle);

                // Gemini-Titel im Hintergrund nachziehen — wenn der API-Key
                // fehlt oder Gemini deaktiviert ist, bleibt der Fallback-
                // Titel einfach stehen. Kein Blocker fuer den Submit-Flow.
                // Sofortiges Re-Render des offenen Historie-Fensters, damit
                // der neue Eintrag direkt sichtbar ist — ohne dass der
                // Benutzer das Fenster zu- und wieder aufklappen muss.
                if (_promptPanel is not null)
                {
                    await Dispatcher.InvokeAsync(async () =>
                    {
                        await _promptPanel.ReloadHistoryAsync();
                    });
                }

                // Cloud-Sync: prompt-history.json nach Drive hochladen.
                // Bewusst NACH dem Re-Render — der Benutzer sieht seinen
                // Eintrag sofort, der Cloud-Push ist Hintergrund-Arbeit.
                _ = TryUploadHistoryAsync();

                // KI-Titel-Generierung nutzt den Gemini-Key aus dem
                // PromptBoard (gleiche Quelle wie der Edit-Dialog "G"-Button
                // und der AI-Improvement-Pipeline). So pflegt der Benutzer
                // genau EINEN Schluessel im Promptboard-Settings-Dialog,
                // und alle drei Pfade (Cleanup, Improvement, History-Title)
                // ziehen am selben Strang. Der Voice-Overlay-Key in der
                // .env-Datei kann unabhaengig davon abgelaufen sein, ohne
                // dass die Historie davon betroffen ist.
                var titleClient = await GetActiveGeminiClientAsync();
                LogToHistoryDebug($"WriteHistoryAsync: titleClient={(titleClient is null ? "null" : "ok")} fallback=[{fallbackTitle}]");
                if (titleClient is not null)
                {
                    string aiTitle = await titleClient.GenerateTitleAsync(middleText);
                    LogToHistoryDebug($"WriteHistoryAsync: ai=[{aiTitle}] same-as-fallback={aiTitle == fallbackTitle}");
                    // Auch ein gleicher Titel wird geschrieben — sonst
                    // bleibt im JSON dauerhaft der Eindruck, dass Gemini
                    // nie aufgerufen wurde, obwohl es genau das Wort fuer
                    // Wort empfohlen hat.
                    if (!string.IsNullOrWhiteSpace(aiTitle))
                    {
                        await _historyService.UpdateTitleAsync(entry.Id, aiTitle);
                        // Nochmal re-rendern — der KI-Titel hat den
                        // Fallback-Titel ueberschrieben (oder ihn bestaetigt).
                        if (_promptPanel is not null)
                        {
                            await Dispatcher.InvokeAsync(async () =>
                            {
                                await _promptPanel.ReloadHistoryAsync();
                            });
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"WriteHistoryAsync failed: {ex.Message}");
            }
        }

        /// <summary>
        /// Laedt die lokale prompt-history.json zu Drive hoch. Wird nach
        /// jedem Submit aufgerufen (fire-and-forget) und schluckt Fehler
        /// still — wenn Drive nicht verbunden ist, ist das kein Problem
        /// fuer den Tipp-Flow. Bei Erfolg sehen Mac und Windows den
        /// neuesten Eintrag beim naechsten Start.
        /// </summary>
        private async Task TryUploadHistoryAsync()
        {
            try
            {
                var sync = GetOrCreateSync();
                if (sync is null)
                {
                    LogHistorySync("SKIP: Drive sync not configured (no PromptBoardSecretStore credentials).");
                    return;
                }
                await sync.UploadHistoryAsync(_historyService.HistoryFilePath);
                LogHistorySync("OK: prompt-history.json uploaded to Drive.");
                // Den sichtbaren Sync-Timestamp im Promtboard-Header
                // aktualisieren — der Label zeigt damit auch
                // Historie-Sync-Aktivitaet, nicht nur Promtboard-Backup.
                _promptPanel?.MarkSyncedNow();
            }
            catch (Exception ex)
            {
                LogHistorySync($"FAIL: {ex.GetType().Name}: {ex.Message}");
            }
        }

        /// <summary>
        /// Schreibt eine Diagnose-Zeile in history-sync-debug.log neben der
        /// PromptBoard-DB. Hilft Bug-Reports schnell aufzuloesen — sehen wir
        /// auf einen Blick ob Drive verbunden ist, ob die Anfrage durchkommt
        /// und welche Exception-Klasse ggf. fliegt.
        /// </summary>
        private static void LogHistorySync(string line)
        {
            try
            {
                string dir = System.IO.Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "PromptBoard", "history");
                System.IO.Directory.CreateDirectory(dir);
                string path = System.IO.Path.Combine(dir, "history-sync-debug.log");
                string ts = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
                System.IO.File.AppendAllText(path, $"{ts}  {line}\n",
                    System.Text.Encoding.UTF8);
            }
            catch { /* Diagnostics never break the main flow. */ }
        }

        /// <summary>
        /// Holt die Cloud-Historie und mergt sie mit dem lokalen Stand.
        /// Wird einmal beim App-Start aufgerufen (vom App.xaml.cs),
        /// nicht bei jedem Submit. Neue Cloud-Eintraege wandern oben in
        /// die Liste, lokale Eintraege bleiben erhalten — bei doppelten
        /// IDs gewinnt der lokale Stand (kann frischeren KI-Titel haben).
        /// </summary>
        public async Task TryMergeHistoryFromCloudAsync()
        {
            try
            {
                var sync = GetOrCreateSync();
                if (sync is null) return;
                string? cloud = await sync.DownloadHistoryAsync();
                if (cloud is null)
                {
                    Console.WriteLine("No cloud history yet — nothing to merge.");
                    return;
                }
                var local = await _historyService.LoadAsync();
                var merged = PromptHistoryDriveSync.MergeEntries(local, cloud);
                if (merged.Count == local.Count)
                {
                    // Keine neuen Eintraege — nichts schreiben, sonst
                    // verdraengen wir KI-Titel mit Fallback-Titeln.
                    Console.WriteLine("Cloud history merge: no new entries.");
                    return;
                }
                await _historyService.ReplaceAllAsync(merged);
                Console.WriteLine($"Cloud history merged: {merged.Count - local.Count} new entries.");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"History cloud merge skipped: {ex.Message}");
            }
        }

        private PromptHistoryDriveSync? GetOrCreateSync()
        {
            if (_historySync is not null) return _historySync;
            try
            {
                // PromptBoardSecretStore lebt im DI-Container des
                // PromptBoardHost — wir holen ihn dort raus statt selbst
                // einen anzulegen, damit beide Wege denselben Pfad zur
                // .env-Datei nutzen.
                var store = PromptBoardHost.Get<PromptBoardSecretStore>();
                _historySync = new PromptHistoryDriveSync(store);
                return _historySync;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"PromptHistoryDriveSync init skipped: {ex.Message}");
                return null;
            }
        }

        /// <summary>Build the PromptBoard always-on Pre AND Post wrappers.
        /// Wird bei JEDEM Voice-Submit aufgerufen — unabhaengig davon ob
        /// das Promtboard-Panel sichtbar ist. Liefert (empty, empty) nur
        /// wenn der Service nicht verfuegbar ist oder keine IsAlwaysOn-
        /// Prompts in der DB existieren. Der Stern-Toggle steuert nur die
        /// Panel-Sichtbarkeit, nicht den Pipeline-Inhalt.</summary>
        private async Task<(string Pre, string Post)> BuildAlwaysOnWrappersAsync()
        {
            if (_alwaysOnPrefix is null) return (string.Empty, string.Empty);

            try
            {
                string pre = await _alwaysOnPrefix.BuildPreAsync();
                string post = await _alwaysOnPrefix.BuildPostAsync();
                return (pre ?? string.Empty, post ?? string.Empty);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AlwaysOn wrappers build failed: {ex.Message}");
                return (string.Empty, string.Empty);
            }
        }

        /// <summary>C button — copy selected text via Ctrl+C.</summary>
        private void BtnCopy_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _terminalWatcher.ActiveTerminalHwnd;

            // Flash: gray for 2 s then back to blue
            CopyButton.Background = BtnIdle;

            TerminalController.CopySelection(hwnd);

            _ = Task.Run(async () =>
            {
                await Task.Delay(2000);
                Dispatcher.Invoke(() => CopyButton.Background = BtnCopy);
            });

            Console.WriteLine("Copy: Ctrl+C sent to terminal");
        }

        /// <summary>P button — paste clipboard content into command line via Ctrl+V.
        /// If auto-enter is enabled, sends Enter after paste.</summary>
        private void BtnPaste_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _terminalWatcher.ActiveTerminalHwnd;

            // Flash: gray for 2 s then back to purple
            PasteButton.Background = BtnIdle;

            TerminalController.PasteClipboard(hwnd);
            hasPastedText = true;

            if (autoEnterEnabled)
            {
                // Small delay then send Enter
                _ = Task.Run(() =>
                {
                    System.Threading.Thread.Sleep(300);
                    TerminalController.PressReturn(hwnd);
                });
                hasPastedText = false;
                Console.WriteLine("Paste+Enter: Ctrl+V → Return sent to terminal");
            }
            else
            {
                Console.WriteLine("Paste: Ctrl+V sent to terminal");
            }

            _ = Task.Run(async () =>
            {
                await Task.Delay(2000);
                Dispatcher.Invoke(() => PasteButton.Background = BtnPaste);
            });
        }

        /// <summary>
        /// Screenshot-Button — nimmt einen Vollbild-Screenshot ueber alle
        /// Monitore auf (virtueller Bildschirm) und speichert ihn als PNG
        /// im Standard-Windows-Screenshot-Ordner unter Pictures\Screenshots.
        /// Der absolute Pfad wird in <see cref="_lastScreenshotPath"/>
        /// gemerkt — exakt diese eine Datei (und keine andere) wird vom
        /// InsertScreenshotButton wieder eingefuegt. Wir nutzen System.Drawing
        /// (verfuegbar weil UseWindowsForms=true) statt Win+Druck, damit wir
        /// einen deterministischen Dateinamen haben und ohne Race Condition
        /// sofort auf den Pfad zugreifen koennen.
        ///
        /// Robustheit (Stand 2026-04-30):
        ///  • Persistentes Logging in screenshot.log — JEDER Klick wird protokolliert,
        ///    so dass Bug-Diagnose moeglich ist auch wenn der Benutzer das
        ///    Verhalten erst spaeter bemerkt.
        ///  • Verifikation nach dem Save — die Datei MUSS existieren UND >0 Bytes
        ///    haben, sonst gilt der Schritt als gescheitert.
        ///  • Filename-Eindeutigkeit ueber Millisekunden-Timestamp. Defensiver
        ///    GUID-Fallback falls (theoretisch unmoeglich) zwei Klicks in
        ///    derselben Millisekunde landen.
        ///  • Erfolg = 1.5 s gruen; Fehler = 3 s rot. Beides deutlich sichtbar.
        ///  • Insert-Tooltip wird live aktualisiert, so dass der Benutzer beim
        ///    Hovern sieht WELCHE Datei beim naechsten Insert eingefuegt wird.
        /// </summary>
        private void BtnScreenshot_Click(object sender, RoutedEventArgs e)
        {
            LogScreenshot("ScreenshotButton.Click handler ENTERED");

            // Flash: gray waehrend Capture laeuft; nach Abschluss faerbt
            // sich der Button gruen (Erfolg) oder rot (Fehler).
            ScreenshotButton.Background = BtnIdle;

            string filename = "";
            string fullPath = "";

            try
            {
                // Virtueller Bildschirm = Bounding-Box ueber alle angeschlossenen
                // Monitore. Negative Koordinaten sind moeglich wenn ein zweiter
                // Monitor links vom Hauptmonitor steht — Graphics.CopyFromScreen
                // akzeptiert das.
                var bounds = System.Windows.Forms.SystemInformation.VirtualScreen;
                LogScreenshot($"VirtualScreen bounds: {bounds.Left},{bounds.Top} {bounds.Width}x{bounds.Height}");

                using var bitmap = new System.Drawing.Bitmap(
                    bounds.Width, bounds.Height, System.Drawing.Imaging.PixelFormat.Format32bppArgb);
                using (var g = System.Drawing.Graphics.FromImage(bitmap))
                {
                    g.CopyFromScreen(bounds.Left, bounds.Top, 0, 0, bounds.Size,
                        System.Drawing.CopyPixelOperation.SourceCopy);
                }

                // Zielordner: Pictures\Screenshots — der Windows-Standard fuer
                // Win+Druck. Wir landen also dort wo der Benutzer Screenshots
                // erwartet. Falls die Locale "Bildschirmfotos" nutzt, schreiben
                // wir trotzdem in "Screenshots" — Pfad wird falls noetig erstellt.
                string picsDir = Environment.GetFolderPath(Environment.SpecialFolder.MyPictures);
                string shotsDir = System.IO.Path.Combine(picsDir, "Screenshots");
                System.IO.Directory.CreateDirectory(shotsDir);

                // Eindeutigkeit: yyyy-MM-dd_HH-mm-ss-fff hat Millisekunden-
                // Praezision — bei Klicks im Sub-Sekunden-Takt entstehen
                // bereits unterschiedliche Namen. Defensiver GUID-Fallback
                // fuer den theoretisch unmoeglichen Fall einer Kollision in
                // derselben Millisekunde (oder wenn die Systemuhr zurueckspringt).
                filename = $"screenshot_{DateTime.Now:yyyy-MM-dd_HH-mm-ss-fff}.png";
                fullPath = System.IO.Path.Combine(shotsDir, filename);
                int collisionGuard = 0;
                while (System.IO.File.Exists(fullPath) && collisionGuard++ < 10)
                {
                    string suffix = Guid.NewGuid().ToString("N").Substring(0, 6);
                    filename = $"screenshot_{DateTime.Now:yyyy-MM-dd_HH-mm-ss-fff}_{suffix}.png";
                    fullPath = System.IO.Path.Combine(shotsDir, filename);
                    LogScreenshot($"Filename collision; retry with suffix: {filename}");
                }

                bitmap.Save(fullPath, System.Drawing.Imaging.ImageFormat.Png);

                // Verifikation: die Datei MUSS jetzt da sein und Bytes haben.
                // Ohne diesen Check wuerden ein leerer/fehlgeschlagener Save
                // still durchgehen und _lastScreenshotPath aktualisiert —
                // beim spaeteren Insert merkt der Benutzer dann nichts vom Fehler.
                var fi = new System.IO.FileInfo(fullPath);
                if (!fi.Exists || fi.Length == 0)
                {
                    throw new System.IO.IOException(
                        $"Screenshot wurde nicht oder leer gespeichert: {fullPath} (exists={fi.Exists}, size={(fi.Exists ? fi.Length : -1)})");
                }

                _lastScreenshotPath = fullPath;
                LogScreenshot($"Screenshot saved OK: {fullPath} ({fi.Length:N0} bytes)");

                // Insert-Tooltip live aktualisieren: der Benutzer kann mit der
                // Maus drueberfahren und sieht WELCHE Datei beim Insert eingefuegt
                // wuerde. So merkt er auf einen Blick wenn der Pfad noch der alte ist.
                InsertScreenshotButton.ToolTip = $"Letzten Screenshot einfügen: {filename}";

                // Erfolgs-Flash: gruen 1.5 Sekunden, dann zurueck zu teal.
                ScreenshotButton.Background = BtnSuccess;
                _ = Task.Run(async () =>
                {
                    await Task.Delay(1500);
                    Dispatcher.Invoke(() => ScreenshotButton.Background = BtnScreenshot);
                });
            }
            catch (Exception ex)
            {
                LogScreenshot($"Screenshot FAILED: {ex.GetType().Name}: {ex.Message}\n{ex.StackTrace}");
                Console.WriteLine($"Screenshot failed: {ex.GetType().Name}: {ex.Message}");
                // Wichtig: _lastScreenshotPath bleibt absichtlich UNVERAENDERT.
                // Der Benutzer sieht den roten Flash und weiss dass der LETZTE
                // erfolgreiche Screenshot weiterhin der "merkbare" ist.
                ScreenshotButton.Background = BtnX;
                _ = Task.Run(async () =>
                {
                    await Task.Delay(3000); // 3 s — deutlich laenger als Erfolg, damit auffaellig
                    Dispatcher.Invoke(() => ScreenshotButton.Background = BtnScreenshot);
                });
            }
        }

        /// <summary>
        /// Insert-Screenshot-Button — nimmt den absoluten Pfad des zuletzt
        /// per ScreenshotButton aufgenommenen Bildes und paste ihn als Text
        /// in die aktive Terminal-Kommandozeile. Wenn der Pfad Leerzeichen
        /// enthaelt, wird er in doppelte Anfuehrungszeichen gesetzt — damit
        /// Shells (PowerShell, CMD, bash via Git Bash) den Pfad als ein
        /// einziges Argument lesen. Wenn noch kein Screenshot gemacht wurde
        /// oder die Datei zwischenzeitlich geloescht wurde, blinkt der Button
        /// rot 3 Sekunden lang und nichts passiert.
        ///
        /// Robustheit: jeder Klick wird in screenshot.log protokolliert.
        /// Bei Erfolg blinkt der Button kurz gruen, bei Fehler 3 s rot.
        /// </summary>
        private void BtnInsertScreenshot_Click(object sender, RoutedEventArgs e)
        {
            LogScreenshot("InsertScreenshotButton.Click handler ENTERED");

            string? path = _lastScreenshotPath;

            if (string.IsNullOrEmpty(path))
            {
                LogScreenshot("InsertScreenshot ABORTED: _lastScreenshotPath is empty (no screenshot taken yet)");
                InsertScreenshotButton.Background = BtnX;
                _ = Task.Run(async () =>
                {
                    await Task.Delay(3000);
                    Dispatcher.Invoke(() => InsertScreenshotButton.Background = BtnInsertScreenshot);
                });
                return;
            }

            if (!System.IO.File.Exists(path))
            {
                LogScreenshot($"InsertScreenshot ABORTED: file not found: {path}");
                InsertScreenshotButton.Background = BtnX;
                _ = Task.Run(async () =>
                {
                    await Task.Delay(3000);
                    Dispatcher.Invoke(() => InsertScreenshotButton.Background = BtnInsertScreenshot);
                });
                return;
            }

            // Pfad mit Leerzeichen: in Anfuehrungszeichen setzen.
            string toPaste = path.Contains(' ') ? $"\"{path}\"" : path;

            LogScreenshot($"InsertScreenshot: pasting '{toPaste}'");

            // Flash: gray waehrend des Pasts, dann gruen kurz, dann amber zurueck.
            InsertScreenshotButton.Background = BtnIdle;

            var hwnd = _terminalWatcher.ActiveTerminalHwnd;
            TerminalController.PasteText(toPaste, hwnd, autoEnter: false);

            LogScreenshot($"InsertScreenshot: PasteText OK for '{toPaste}'");

            // Erfolgs-Flash gruen kurz, dann zurueck zu amber.
            InsertScreenshotButton.Background = BtnSuccess;
            _ = Task.Run(async () =>
            {
                await Task.Delay(1500);
                Dispatcher.Invoke(() => InsertScreenshotButton.Background = BtnInsertScreenshot);
            });
        }

        // ── Mic state helpers ──

        private void SetMicState(RecordingState state)
        {
            _micState = state;
            _pulseTimer.Stop();
            _pulseBright = false;

            // Welle nur waehrend der Recording-Phase sichtbar — sobald
            // Whisper transkribiert, schalten wir auf das Mikrofon-Icon
            // zurueck damit der orangefarbene Processing-Hintergrund
            // klarer wirkt. Erfolg/Fehler-Phase bleiben ebenfalls beim
            // Icon — die Welle gehoert ausschliesslich zum aktiven Mic.
            SetWaveformVisible(state == RecordingState.Recording);

            switch (state)
            {
                case RecordingState.Idle:
                    MicButton.Background = BtnMicIdle;
                    break;
                case RecordingState.Recording:
                    MicButton.Background = BtnRecording;
                    _pulseTimer.Start();
                    break;
                case RecordingState.Processing:
                    MicButton.Background = BtnProcessing;
                    break;
                case RecordingState.Success:
                    MicButton.Background = BtnSuccess;
                    break;
                case RecordingState.Error:
                    MicButton.Background = BtnX;
                    break;
            }
        }

        private void SetBtwMicState(RecordingState state)
        {
            _btwPulseTimer.Stop();
            _btwPulseBright = false;

            switch (state)
            {
                case RecordingState.Idle:
                    BtwButton.Background = BtnBtwIdle;
                    break;
                case RecordingState.Recording:
                    BtwButton.Background = BtnBtwRecording;
                    _btwPulseTimer.Start();
                    break;
                case RecordingState.Processing:
                    BtwButton.Background = BtnProcessing;
                    break;
                case RecordingState.Success:
                    BtwButton.Background = BtnSuccess;
                    break;
                case RecordingState.Error:
                    BtwButton.Background = BtnX;
                    break;
            }
        }

        private void ScheduleReset()
        {
            _resetTimer.Stop();
            _resetTimer.Start();
        }

        // ── Brush factory ──

        private static SolidColorBrush Brush(string hex)
        {
            var color = (Color)ColorConverter.ConvertFromString(hex);
            var brush  = new SolidColorBrush(color);
            brush.Freeze();
            return brush;
        }

        // ── Waveform-Visualizer ──

        /// <summary>
        /// Erzeugt die 14 weissen Strich-Rectangles und legt sie im
        /// WaveformCanvas ab. Wird einmal beim Konstruktor aufgerufen —
        /// die Hoehen werden spaeter pro Pegel-Update veraendert.
        /// Strich-Layout: Start-Offset 3.5px (zentriert im 48px-Canvas),
        /// 2px Strichbreite, 1px Spacing zwischen Strichen. Initiale Hoehe
        /// ist WaveformMinH damit die Welle schon "lebt" wenn sie das
        /// erste Mal eingeblendet wird, auch wenn noch keine Pegel-Events
        /// reingekommen sind.
        /// </summary>
        private void BuildWaveformBars()
        {
            if (WaveformCanvas == null) return;

            const double startOffset =
                (48.0 - (WaveformBarCount * WaveformBarWidth
                         + (WaveformBarCount - 1) * WaveformBarSpacing)) / 2.0;

            for (int i = 0; i < WaveformBarCount; i++)
            {
                var bar = new System.Windows.Shapes.Rectangle
                {
                    Width = WaveformBarWidth,
                    Height = WaveformMinH,
                    RadiusX = 1.0,
                    RadiusY = 1.0,
                    Fill = new SolidColorBrush(Color.FromRgb(0x1A, 0x1A, 0x1A)),  // Dark for contrast on yellow mic background
                };
                double x = startOffset + i * (WaveformBarWidth + WaveformBarSpacing);
                System.Windows.Controls.Canvas.SetLeft(bar, x);
                System.Windows.Controls.Canvas.SetTop(bar, (WaveformCanvasH - WaveformMinH) / 2.0);
                WaveformCanvas.Children.Add(bar);
                _waveformBars[i] = bar;
            }
        }

        /// <summary>
        /// Wird vom AudioRecorder pro Buffer (~100ms) aufgerufen. Der
        /// uebergebene Wert ist der Peak-Pegel des aktuellen Audio-
        /// Buffers (0..1). Wir verstaerken ihn leicht (Wurzel + Faktor)
        /// damit auch normale Sprechlautstaerke die Welle ausgepraegt
        /// fuellt — ohne Verstaerkung waeren die Striche bei 0.05..0.2
        /// Lautstaerke kaum sichtbar. Anschliessend rotiert der Buffer:
        /// neuer Wert kommt rechts rein, alte fallen links raus, die
        /// Welle fliesst optisch nach links.
        /// </summary>
        private void OnAudioLevelChanged(float level)
        {
            // Marshall auf den UI-Thread — das Event kommt vom NAudio-
            // Buffer-Thread. BeginInvoke statt Invoke damit der Audio-
            // Thread nicht auf das UI-Rendering wartet.
            Dispatcher.BeginInvoke(new Action(() =>
            {
                // Welle nur aktualisieren wenn sie sichtbar ist — bei
                // BTW-Aufnahme z.B. ist der Hauptmic-Button gar nicht
                // im Recording-Modus, dann waere das Animieren reine
                // Verschwendung.
                if (WaveformCanvas == null || WaveformCanvas.Visibility != Visibility.Visible)
                    return;

                // Pegel verstaerken: Wurzel macht leise Toene sichtbarer,
                // Faktor 1.6 hebt das Ergebnis nochmal an. Cap auf 1.0
                // verhindert dass Vollausschlag den Canvas verlaesst.
                float boosted = MathF.Min(1f, MathF.Sqrt(level) * 1.6f);

                // Buffer nach links shiften, neuer Wert rechts rein.
                for (int i = 0; i < WaveformBarCount - 1; i++)
                    _waveformBuffer[i] = _waveformBuffer[i + 1];
                _waveformBuffer[WaveformBarCount - 1] = boosted;

                // Strich-Hoehen aktualisieren.
                for (int i = 0; i < WaveformBarCount; i++)
                {
                    if (_waveformBars[i] == null) continue;
                    double h = WaveformMinH + _waveformBuffer[i] * (WaveformMaxH - WaveformMinH);
                    _waveformBars[i].Height = h;
                    System.Windows.Controls.Canvas.SetTop(
                        _waveformBars[i], (WaveformCanvasH - h) / 2.0);
                }
            }));
        }

        /// <summary>
        /// Schaltet zwischen Mikrofon-Icon (Idle) und Wellenanzeige
        /// (Recording) um. Beim Wechsel auf "Welle" wird der Buffer
        /// auf null gesetzt, damit die alte Welle vom letzten Diktat
        /// nicht stehenbleibt.
        /// </summary>
        private void SetWaveformVisible(bool visible)
        {
            if (WaveformCanvas == null || MicIcon == null) return;
            if (visible)
            {
                Array.Clear(_waveformBuffer, 0, _waveformBuffer.Length);
                for (int i = 0; i < WaveformBarCount; i++)
                {
                    if (_waveformBars[i] == null) continue;
                    _waveformBars[i].Height = WaveformMinH;
                    System.Windows.Controls.Canvas.SetTop(
                        _waveformBars[i], (WaveformCanvasH - WaveformMinH) / 2.0);
                }
                MicIcon.Visibility = Visibility.Collapsed;
                WaveformCanvas.Visibility = Visibility.Visible;
            }
            else
            {
                WaveformCanvas.Visibility = Visibility.Collapsed;
                MicIcon.Visibility = Visibility.Visible;
            }
        }

        // ── Cleanup ──

        // ── Push-to-Talk: Low-Level-Keyboard-Hook ──
        //
        // WARUM ein Low-Level-Hook und nicht RegisterHotKey?
        // RegisterHotKey feuert nur bei KeyDown — fuer Push-to-Talk brauchen
        // wir aber AUCH KeyUp (Loslassen = Aufnahme stoppen). WH_KEYBOARD_LL
        // liefert beide Events und blockiert sie systemweit, sodass die
        // Tastenkombination nicht mehr in den darunterliegenden Apps landet.
        //
        // KOMBI: Strg+Alt+Leertaste
        // - Beim ersten DOWN-Event waehrend Strg+Alt aktiv: Aufnahme starten.
        // - Bei UP der Leertaste (oder einer der Modifier): Aufnahme stoppen
        //   und transkribieren.
        //
        // Frank kann auf seine Logitech G5-Taste ein Makro legen, das diese
        // Kombi sendet (Held-while-pressed Mode). Damit wird die G5 zur
        // physischen Push-to-Talk-Taste.
        private void InstallPushToTalkHook()
        {
            try
            {
                _pttHookProc = OnLowLevelKey; // Referenz halten gegen GC
                IntPtr hMod = NativeMethods.Win32.GetModuleHandle(
                    System.Reflection.Assembly.GetExecutingAssembly()?.GetName().Name ?? "");
                if (hMod == IntPtr.Zero)
                    hMod = NativeMethods.Win32.GetModuleHandle(null!);

                _pttHookHandle = NativeMethods.Win32.SetWindowsHookEx(
                    NativeMethods.Win32.WH_KEYBOARD_LL, _pttHookProc, hMod, 0);

                if (_pttHookHandle == IntPtr.Zero)
                    Console.WriteLine("PTT: SetWindowsHookEx failed (Code " +
                        Marshal.GetLastWin32Error() + ")");
                else
                    Console.WriteLine("PTT: Hook installed — Strg+Alt+Leertaste hold-to-talk aktiv");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"PTT: Hook install failed: {ex.GetType().Name}: {ex.Message}");
            }
        }

        private IntPtr OnLowLevelKey(int nCode, IntPtr wParam, IntPtr lParam)
        {
            // nCode < 0 bedeutet "nicht verarbeiten, einfach weiterleiten"
            if (nCode < 0)
                return NativeMethods.Win32.CallNextHookEx(_pttHookHandle, nCode, wParam, lParam);

            int msg = wParam.ToInt32();
            var data = Marshal.PtrToStructure<NativeMethods.Win32.KBDLLHOOKSTRUCT>(lParam);
            uint vk = data.vkCode;

            // ── Alt+F11: Explorer am Release-Bundle-Pfad oeffnen ──
            // Einfacher Shortcut, kein Push-to-Talk. Pro Tastendruck genau
            // ein Fenster — Auto-Repeat-Schutz via _altF11Down.
            if (vk == NativeMethods.Win32.VK_F11)
            {
                bool altF11 = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU) & 0x8000) != 0;
                bool isDownF11 = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                bool isUpF11   = (msg == NativeMethods.Win32.WM_KEYUP   || msg == NativeMethods.Win32.WM_SYSKEYUP);

                if (isDownF11 && altF11 && !_altF11Down)
                {
                    _altF11Down = true;
                    Console.WriteLine("Alt+F11: open release-bundle folder in explorer (windowed)");
                    Dispatcher.BeginInvoke(new Action(() =>
                    {
                        try { OpenReleaseBundleFolder(); }
                        catch (Exception ex) { Console.WriteLine($"Alt+F11 error: {ex.Message}"); }
                    }));
                    return new IntPtr(1);
                }
                if (isUpF11)
                {
                    _altF11Down = false;
                }
            }

            // ── Alt+F12: REINES TOGGLE fuer Audio-Aufnahme ──
            // 1x druecken = Aufnahme an. 1x druecken = Aufnahme aus + transkribieren.
            // Kein PTT-Halten — G-HUB Macros koennen das nicht zuverlaessig.
            // Cooldown von 350ms gegen Tap-Spam aus G-HUB-Modi wie "Wiederholen
            // beim Halten" — sonst wuerde der Hook bei wiederholten Tap-Bursts
            // zwischen Start und Stop oszillieren.
            if (vk == NativeMethods.Win32.VK_F12)
            {
                bool altF12 = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU) & 0x8000) != 0;
                bool isDownF12 = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                bool isUpF12   = (msg == NativeMethods.Win32.WM_KEYUP   || msg == NativeMethods.Win32.WM_SYSKEYUP);

                if (isDownF12 && altF12 && !_altF12KeyDown)
                {
                    _altF12KeyDown = true;
                    if (DateTime.UtcNow < _altF12CooldownUntil)
                    {
                        Console.WriteLine("Alt+F12: in cooldown — tap ignoriert");
                        return new IntPtr(1);
                    }
                    _altF12CooldownUntil = DateTime.UtcNow.AddMilliseconds(AltF12CooldownMs);
                    Console.WriteLine("Alt+F12: toggle recording (BtnMic_Click)");
                    Dispatcher.BeginInvoke(new Action(() =>
                    {
                        try { BtnMic_Click(this, new RoutedEventArgs()); }
                        catch (Exception ex) { Console.WriteLine($"Alt+F12 toggle error: {ex.Message}"); }
                    }));
                    return new IntPtr(1);
                }
                if (isUpF12)
                {
                    _altF12KeyDown = false;
                }
                if (isDownF12 && altF12) return new IntPtr(1); // Auto-Repeat schlucken
            }

            // Wir reagieren nur auf die Leertaste — Strg/Alt sind reine Modifier.
            // Beim KeyDown der Leertaste pruefen wir ob Strg+Alt zusaetzlich
            // gedrueckt sind (GetAsyncKeyState liest den aktuellen Tastenzustand).
            // Der Hook-Callback laeuft auf dem niedrig-priorisierten Hook-Thread,
            // alle UI-Aktionen muessen via Dispatcher.BeginInvoke erfolgen.
            if (vk == NativeMethods.Win32.VK_SPACE
                || vk == NativeMethods.Win32.VK_M
                || vk == NativeMethods.Win32.VK_F9)
            {
                bool ctrl  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_CONTROL) & 0x8000) != 0;
                bool alt   = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU)    & 0x8000) != 0;
                bool shift = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_SHIFT)   & 0x8000) != 0;

                // Akzeptierte Hotkey-Varianten fuer Push-to-Talk:
                //   1) Strg+Alt+Leertaste — original, aber Strg+Wheel = Zoom
                //   2) Shift+Alt+Leertaste — kein Zoom, ABER bei wackeligen
                //      G-HUB-Macros kann das Alt+Space-System-Menue aufgehen
                //   3) Shift+Alt+M — bulletproof, kein Windows-Shortcut, kein
                //      System-Menue, egal in welcher Reihenfolge die Modifier
                //      kommen. Bevorzugte G-Macro-Belegung wenn Modifier-Plus-
                //      Buchstaben-Schema gewuenscht.
                //   4) Strg+F9 oder Shift+F9 — sehr kurze Kombi, in CLIs frei.
                //      Strg+F9 hat den bekannten Strg+Wheel-Zoom-Konflikt;
                //      Shift+F9 ist davon frei. Modifier-Regel daher anders:
                //      bei F9 reicht EIN Modifier (ctrl ODER shift), kein Alt
                //      noetig — sonst muesste man drei Tasten halten.
                //   5) Alt+F12 — kuerzeste Kombi, kein Zoom-Konflikt (Alt+Wheel
                //      zoomt nicht), in CLIs/Browsern keine Standard-Funktion.
                //      Modifier-Regel: nur Alt — kein Strg, kein Shift noetig.
                bool modsOk;
                if (vk == NativeMethods.Win32.VK_F12)
                    modsOk = alt;
                else if (vk == NativeMethods.Win32.VK_F9)
                    modsOk = ctrl || shift;
                else
                    modsOk = alt && (ctrl || shift);

                bool isDown = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                bool isUp   = (msg == NativeMethods.Win32.WM_KEYUP   || msg == NativeMethods.Win32.WM_SYSKEYUP);

                if (isDown && modsOk)
                {
                    if (_pttToggleMode)
                    {
                        // Wir laufen schon im Toggle-Modus (durch frueheren Tap
                        // gestartet). Dieser Tap stoppt die Aufnahme.
                        _pttToggleMode = false;
                        _pttRecording  = false;
                        Console.WriteLine("PTT: toggle stop tap — stop and transcribe");
                        Dispatcher.BeginInvoke(new Action(() =>
                        {
                            try { BtnMic_Click(this, new RoutedEventArgs()); }
                            catch (Exception ex) { Console.WriteLine($"PTT toggle-stop error: {ex.Message}"); }
                        }));
                    }
                    else if (!_pttRecording)
                    {
                        // Frische DOWN-Flanke: Aufnahme starten. Wir merken
                        // uns den Zeitpunkt — beim UP entscheidet die Dauer
                        // ob HOLD-Modus (laenger als Schwelle) oder TOGGLE
                        // (kuerzer, also G-Tasten-Macro). Auto-repeat der
                        // Tastatur landet im else-Zweig (kein zweiter Start).
                        _pttRecording  = true;
                        _pttKeyDownAt  = DateTime.UtcNow;
                        Console.WriteLine("PTT: keydown — start recording");
                        Dispatcher.BeginInvoke(new Action(() =>
                        {
                            try { BtnMic_Click(this, new RoutedEventArgs()); }
                            catch (Exception ex) { Console.WriteLine($"PTT start error: {ex.Message}"); }
                        }));
                    }
                    // else: auto-repeat DOWN waehrend HOLD-Modus — schlucken
                    return new IntPtr(1);
                }

                if (isUp)
                {
                    // UP: nur reagieren wenn wir tatsaechlich aufnehmen UND
                    // nicht schon im Toggle-Modus sind (im Toggle-Modus
                    // ignorieren wir UP — gestoppt wird per naechstem Tap).
                    if (_pttRecording && !_pttToggleMode)
                    {
                        double heldMs = (DateTime.UtcNow - _pttKeyDownAt).TotalMilliseconds;
                        if (heldMs < PttTapThresholdMs)
                        {
                            // Kurzer Tap — Aufnahme weiter laufen lassen,
                            // Toggle-Modus aktivieren. Der naechste Tap stoppt.
                            _pttToggleMode = true;
                            Console.WriteLine($"PTT: short tap ({heldMs:F0} ms) — entering toggle mode, tap again to stop");
                        }
                        else
                        {
                            // Langer Halt — klassisches PTT, sofort stoppen.
                            _pttRecording = false;
                            Console.WriteLine($"PTT: hold release ({heldMs:F0} ms) — stop and transcribe");
                            Dispatcher.BeginInvoke(new Action(() =>
                            {
                                try { BtnMic_Click(this, new RoutedEventArgs()); }
                                catch (Exception ex) { Console.WriteLine($"PTT stop error: {ex.Message}"); }
                            }));
                        }
                        return new IntPtr(1);
                    }
                }
            }

            // OneShot-Hotkey: Strg+Alt+P = Screenshot UND sofort einfuegen.
            // BtnScreenshot_Click laeuft synchron auf dem UI-Thread, danach
            // ist _lastScreenshotPath aktualisiert (wenn der Save klappte)
            // und wir koennen direkt BtnInsertScreenshot_Click anhaengen.
            // Erfolgs-Check ueber Vorher/Nachher-Vergleich von _lastScreenshotPath:
            // bei Capture-Fehler bleibt der alte Pfad — und wir wuerden sonst
            // ungewollt den alten Screenshot einfuegen.
            if (vk == NativeMethods.Win32.VK_P)
            {
                bool ctrl = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_CONTROL) & 0x8000) != 0;
                bool alt  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU)    & 0x8000) != 0;
                bool isDown = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                bool isUp   = (msg == NativeMethods.Win32.WM_KEYUP   || msg == NativeMethods.Win32.WM_SYSKEYUP);

                if (isDown && ctrl && alt && !_screenshotKeyDown)
                {
                    _screenshotKeyDown = true;
                    Console.WriteLine("Hotkey: Strg+Alt+P — Screenshot + Insert (one-shot)");
                    Dispatcher.BeginInvoke(new Action(() =>
                    {
                        try
                        {
                            string? before = _lastScreenshotPath;
                            BtnScreenshot_Click(this, new RoutedEventArgs());
                            // Wenn der Pfad sich geaendert hat, war der Save
                            // erfolgreich — direkt einfuegen. Sonst ueberspringen
                            // damit kein alter Screenshot versehentlich gepaste wird.
                            if (!string.IsNullOrEmpty(_lastScreenshotPath) && _lastScreenshotPath != before)
                            {
                                BtnInsertScreenshot_Click(this, new RoutedEventArgs());
                            }
                            else
                            {
                                Console.WriteLine("OneShot: Screenshot fehlgeschlagen, Insert uebersprungen");
                            }
                        }
                        catch (Exception ex) { Console.WriteLine($"OneShot hotkey error: {ex.Message}"); }
                    }));
                    return new IntPtr(1);
                }
                if (isDown && ctrl && alt) return new IntPtr(1); // Auto-Repeat schlucken
                if (isUp) _screenshotKeyDown = false;
            }

            // Insert-Screenshot-Hotkey: Strg+Alt+I
            if (vk == NativeMethods.Win32.VK_I)
            {
                bool ctrl = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_CONTROL) & 0x8000) != 0;
                bool alt  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU)    & 0x8000) != 0;
                bool isDown = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                bool isUp   = (msg == NativeMethods.Win32.WM_KEYUP   || msg == NativeMethods.Win32.WM_SYSKEYUP);

                if (isDown && ctrl && alt && !_insertKeyDown)
                {
                    _insertKeyDown = true;
                    Console.WriteLine("Hotkey: Strg+Alt+I — Insert Screenshot");
                    Dispatcher.BeginInvoke(new Action(() =>
                    {
                        try { BtnInsertScreenshot_Click(this, new RoutedEventArgs()); }
                        catch (Exception ex) { Console.WriteLine($"Insert hotkey error: {ex.Message}"); }
                    }));
                    return new IntPtr(1);
                }
                if (isDown && ctrl && alt) return new IntPtr(1); // Auto-Repeat schlucken
                if (isUp) _insertKeyDown = false;
            }

            // ── Prompt-Hotkeys: Strg+1..Strg+9 ─────────────────────────────
            //
            // Pasten den Prompt mit der jeweiligen HotkeyNumber in das aktive
            // Terminal. Der Hook reagiert NUR wenn:
            //   1) eine reine Strg-Kombi gehalten wird (kein Alt, Shift, Win),
            //   2) ein Prompt mit dieser Nummer in der HotkeyRegistry steht,
            //   3) und ein Terminal-Fenster im Vordergrund ist.
            // Sonst laeuft die Taste durch — damit Strg+1 in Browser/VS-Code
            // weiter Tab 1 wechselt. Frank wollte explizit dass die Funktion
            // nur in der CLI greift.
            if (vk >= 0x31 && vk <= 0x39 && HotkeyRegistry.HasAny)
            {
                bool ctrl  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_CONTROL) & 0x8000) != 0;
                bool alt   = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU)    & 0x8000) != 0;
                bool shift = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_SHIFT)   & 0x8000) != 0;
                bool isDown = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                bool isUp   = (msg == NativeMethods.Win32.WM_KEYUP   || msg == NativeMethods.Win32.WM_SYSKEYUP);

                if (ctrl && !alt && !shift)
                {
                    int hotkeyNumber = (int)(vk - 0x30); // 0x31..0x39 → 1..9
                    var entry = HotkeyRegistry.Lookup(hotkeyNumber);
                    if (entry is HotkeyRegistry.Entry e)
                    {
                        // Aktives Fenster pruefen — TerminalWatcher haelt den
                        // letzten erkannten Terminal-HWND. Null/Zero heisst:
                        // gerade ist KEIN Terminal aktiv → wir lassen die
                        // Taste durch (Browser-Tabs etc. funktionieren weiter).
                        IntPtr terminalHwnd = _terminalWatcher.ActiveTerminalHwnd;
                        IntPtr foreground = NativeMethods.Win32.GetForegroundWindow();
                        bool terminalIsForeground = terminalHwnd != IntPtr.Zero
                            && (foreground == terminalHwnd || IsWindowDescendantOf(foreground, terminalHwnd));

                        if (terminalIsForeground)
                        {
                            if (isDown && !_promptHotkeyDown[hotkeyNumber])
                            {
                                _promptHotkeyDown[hotkeyNumber] = true;
                                Console.WriteLine($"Hotkey: Strg+{hotkeyNumber} — paste prompt ({e.EffectiveText.Length} chars)");
                                Dispatcher.BeginInvoke(new Action(() =>
                                {
                                    try
                                    {
                                        TerminalController.PasteText(
                                            e.EffectiveText,
                                            _terminalWatcher.ActiveTerminalHwnd,
                                            autoEnterEnabled);
                                    }
                                    catch (Exception ex)
                                    {
                                        Console.WriteLine($"Prompt hotkey paste failed: {ex.Message}");
                                    }
                                }));
                                return new IntPtr(1);
                            }
                            if (isDown) return new IntPtr(1); // Auto-Repeat schlucken
                            if (isUp) _promptHotkeyDown[hotkeyNumber] = false;
                        }
                        // else: kein Terminal aktiv — durchlassen, Browser-Tabs
                        // funktionieren weiterhin wie gewohnt.
                    }
                }
                else if (isUp)
                {
                    // Sicherheitsnetz: wenn der User die Modifier mitten in
                    // einem Down/Up-Zyklus losgelassen hat, Flag zuruecksetzen.
                    int hotkeyNumber = (int)(vk - 0x30);
                    if (hotkeyNumber >= 1 && hotkeyNumber <= 9)
                        _promptHotkeyDown[hotkeyNumber] = false;
                }
            }

            return NativeMethods.Win32.CallNextHookEx(_pttHookHandle, nCode, wParam, lParam);
        }

        /// <summary>
        /// Indices 1..9 — 0 ist ungenutzt damit der Index direkt der
        /// Hotkey-Nummer entspricht. Verhindert dass Auto-Repeat-Tasten
        /// den Paste-Pfad mehrfach ausloesen wenn der Benutzer Strg+N
        /// laenger gedrueckt haelt.
        /// </summary>
        private readonly bool[] _promptHotkeyDown = new bool[10];

        /// <summary>
        /// Manche Terminal-Apps (Windows Terminal, VS Code Integrated
        /// Terminal) packen den eigentlichen TermControl in ein Kindfenster.
        /// Wenn der Benutzer dort tippt liefert GetForegroundWindow das
        /// AEUSSERE Hauptfenster — der TerminalWatcher hat aber den HWND
        /// des inneren Controls gemerkt. Dieser Helfer akzeptiert beide
        /// Richtungen, damit der Hotkey trotzdem feuert.
        /// </summary>
        private static bool IsWindowDescendantOf(IntPtr child, IntPtr ancestor)
        {
            if (child == IntPtr.Zero || ancestor == IntPtr.Zero) return false;
            // Vom Kindfenster nach oben: ist eines der Eltern der gemerkte
            // Terminal-HWND? Maximal ein paar Stufen hoch — Terminal-UIs
            // verschachteln nicht tief.
            IntPtr cur = child;
            for (int i = 0; i < 6 && cur != IntPtr.Zero; i++)
            {
                if (cur == ancestor) return true;
                cur = NativeMethods.Win32.GetParent(cur);
            }
            // Andersrum: ist der gemerkte HWND ein Kind des aktuellen
            // Vordergrundfensters? Hilft wenn der Watcher das aeussere
            // Fenster gemerkt hat aber das innere fokussiert ist.
            cur = ancestor;
            for (int i = 0; i < 6 && cur != IntPtr.Zero; i++)
            {
                if (cur == child) return true;
                cur = NativeMethods.Win32.GetParent(cur);
            }
            return false;
        }

        // Alt+F11 Hotkey-Aktion: Den Release-Bundle-Ordner im Windows Explorer
        // oeffnen UND in den Vordergrund holen. Das Terminal bleibt in seiner
        // Groesse und Position. Falls der Ordner noch nicht existiert (z.B.
        // weil noch nie ein Release-Build lief), oeffnen wir das naechste
        // existierende Eltern-Verzeichnis.
        //
        // Foreground-Stealing: Windows blockiert das Stehlen des Fokus wenn
        // ein Hintergrund-Prozess (TVO) Explorer startet — das neue Fenster
        // landet blinkend in der Taskbar. Wir umgehen das mit drei Tricks:
        //   1) AllowSetForegroundWindow(ASFW_ANY) erlaubt allen Prozessen Fokus
        //   2) Snapshot der vorhandenen Explorer-Fenster, dann nach 500ms das
        //      neue Fenster identifizieren (per Class "CabinetWClass")
        //   3) AttachThreadInput-Trick fuer zuverlaessiges SetForegroundWindow
        private void OpenReleaseBundleFolder()
        {
            string folder = ReleaseBundleFolder;
            try
            {
                while (!string.IsNullOrEmpty(folder) && !Directory.Exists(folder))
                {
                    string? parent = Path.GetDirectoryName(folder);
                    if (string.IsNullOrEmpty(parent) || parent == folder) break;
                    folder = parent;
                }
                if (string.IsNullOrEmpty(folder) || !Directory.Exists(folder))
                {
                    Console.WriteLine($"Alt+F11: Pfad nicht gefunden — {ReleaseBundleFolder}");
                    return;
                }

                // Snapshot vorhandener Explorer-Fenster (vor Start)
                var existingHandles = new HashSet<IntPtr>(GetExplorerWindows());

                NativeMethods.Win32.AllowSetForegroundWindow(NativeMethods.Win32.ASFW_ANY);

                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                {
                    FileName = "explorer.exe",
                    Arguments = $"\"{folder}\"",
                    UseShellExecute = true,
                });
                Console.WriteLine($"Alt+F11: explorer geoeffnet bei {folder}");

                // Nach kurzem Delay das neue Fenster suchen und nach vorn holen.
                Task.Delay(500).ContinueWith(_ =>
                {
                    try
                    {
                        Dispatcher.BeginInvoke(new Action(() =>
                        {
                            IntPtr target = IntPtr.Zero;
                            foreach (var hwnd in GetExplorerWindows())
                            {
                                if (!existingHandles.Contains(hwnd))
                                {
                                    target = hwnd;
                                    break;
                                }
                            }
                            if (target == IntPtr.Zero)
                            {
                                Console.WriteLine("Alt+F11: kein neues Explorer-Fenster gefunden — Fokus-Steal uebersprungen");
                                return;
                            }
                            ForceWindowToForeground(target);
                            Console.WriteLine($"Alt+F11: explorer-fenster {target} nach vorn geholt");
                        }));
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"Alt+F11 foreground steal failed: {ex.Message}");
                    }
                });
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Alt+F11 explorer start failed: {ex.Message}");
            }
        }

        // Sammelt alle sichtbaren Explorer-Fenster (Class "CabinetWClass" =
        // moderner File-Explorer, "ExploreWClass" = aelterer dual-pane Modus).
        private static List<IntPtr> GetExplorerWindows()
        {
            var result = new List<IntPtr>();
            NativeMethods.Win32.EnumWindows((hWnd, _) =>
            {
                if (!NativeMethods.Win32.IsWindowVisible(hWnd)) return true;
                var sb = new StringBuilder(64);
                NativeMethods.Win32.GetClassName(hWnd, sb, sb.Capacity);
                var className = sb.ToString();
                if (className == "CabinetWClass" || className == "ExploreWClass")
                    result.Add(hWnd);
                return true;
            }, IntPtr.Zero);
            return result;
        }

        // Standard Win32 "Force Foreground"-Pattern: AttachThreadInput koppelt
        // Eingabe-Queues von Quell- und Ziel-Thread, waehrend wir das Fenster
        // nach vorn holen. Ohne diese Kopplung blockiert Windows seit XP das
        // Stehlen des Fokus durch Hintergrund-Prozesse.
        private static void ForceWindowToForeground(IntPtr hwnd)
        {
            if (hwnd == IntPtr.Zero) return;

            var fg = NativeMethods.Win32.GetForegroundWindow();
            if (fg == hwnd) return;

            uint currentThread = NativeMethods.Win32.GetCurrentThreadId();
            uint fgThread     = fg == IntPtr.Zero ? 0u : NativeMethods.Win32.GetWindowThreadProcessId(fg, out _);
            uint targetThread = NativeMethods.Win32.GetWindowThreadProcessId(hwnd, out _);

            bool attached1 = false, attached2 = false;
            try
            {
                if (fgThread != 0 && fgThread != currentThread)
                    attached1 = NativeMethods.Win32.AttachThreadInput(currentThread, fgThread, true);
                if (targetThread != 0 && targetThread != currentThread && targetThread != fgThread)
                    attached2 = NativeMethods.Win32.AttachThreadInput(currentThread, targetThread, true);

                // Falls minimiert: wiederherstellen, dann nach vorn.
                if (NativeMethods.Win32.IsIconic(hwnd))
                    NativeMethods.Win32.ShowWindow(hwnd, NativeMethods.Win32.SW_RESTORE);

                NativeMethods.Win32.BringWindowToTop(hwnd);
                NativeMethods.Win32.SetForegroundWindow(hwnd);
            }
            finally
            {
                if (attached1) NativeMethods.Win32.AttachThreadInput(currentThread, fgThread, false);
                if (attached2) NativeMethods.Win32.AttachThreadInput(currentThread, targetThread, false);
            }
        }

        protected override void OnClosed(EventArgs e)
        {
            _pulseTimer.Stop();
            _btwPulseTimer.Stop();
            _resetTimer.Stop();
            _hideDelayTimer.Stop();
            // Hook abbauen — sonst bleibt die DLL im Tastatur-Stack haengen
            // und alle Tastendruecke laufen weiter durch unseren Callback.
            if (_pttHookHandle != IntPtr.Zero)
            {
                NativeMethods.Win32.UnhookWindowsHookEx(_pttHookHandle);
                _pttHookHandle = IntPtr.Zero;
            }
            _audioRecorder.LevelChanged -= OnAudioLevelChanged;
            _terminalWatcher.Dispose();
            _audioRecorder.Dispose();
            base.OnClosed(e);
        }
    }
}
