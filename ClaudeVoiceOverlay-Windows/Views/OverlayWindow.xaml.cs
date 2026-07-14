using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
using Microsoft.Extensions.DependencyInjection;
using ClaudeVoiceOverlay.Models;
using ClaudeVoiceOverlay.NativeMethods;
using ClaudeVoiceOverlay.Services;

namespace ClaudeVoiceOverlay.Views
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
        // Mic-Familie: LILA statt Gelb — damit das CVO (Claude) Overlay sofort
        // vom TVO (Terminal) Overlay zu unterscheiden ist (TVO behaelt die gelbe
        // Mic-Familie). Helles Lila, damit der dunkle Icon/Text (#1A1A1A) lesbar
        // bleibt — kein Icon-Recolor noetig.
        private static readonly SolidColorBrush BtnBtwIdle      = Brush("#BA68C8");  // Purple 300
        private static readonly SolidColorBrush BtnBtwRecording = Brush("#9C27B0");  // Purple 500
        private static readonly SolidColorBrush BtnBtwPulse     = Brush("#CE93D8");  // Purple 200
        // Special
        private static readonly SolidColorBrush BtnX         = Brush("#C62828");
        private static readonly SolidColorBrush BtnXPressed  = Brush("#E53935");
        // BtnMicIdle (gelb) bleibt fuer die aktiven Profil-Tiles; die Mikrofone
        // selbst nutzen BtnMicIdlePurple (CVO-Erkennung, lila statt gelb).
        private static readonly SolidColorBrush BtnMicIdle       = Brush("#F9A825");  // Yellow 800 (Profil-Tiles)
        private static readonly SolidColorBrush BtnMicIdlePurple = Brush("#AB47BC");  // Purple 400 (Mikrofone CVO)
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
        private readonly RecordingCuePlayer _recordingCuePlayer;
        private readonly GroqWhisperClient _groqClient;
        private readonly GeminiClient?     _geminiClient;
        private readonly AppWatcher   _appWatcher;

        // ── State ──
        private RecordingState _micState    = RecordingState.Idle;
        private bool _isProcessing          = false;
        private bool isBtwRecording         = false;
        private bool geminiEnabled          = false;  // Default = Gemini-Korrektur AUS (Whisper-roh), KEIN Profil aktiv (Frank-Wunsch 2026-06-22: beim Start kein Profil voreingestellt). Profil-Klick oder G-Button schaltet Gemini ein. Ohne Gemini-API-Key bleibt es ohnehin false.
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
        // 3 = Meta-Intelligenz (strukturiertes Denken). Default = 0 = KEIN
        // Profil voreingestellt (Frank-Wunsch 2026-06-22): beim Start leuchtet
        // kein Tile; der Benutzer waehlt ein Profil aktiv per Klick.
        private int _activeProfile = 0;

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
        private IntPtr _mainRecordingTargetHwnd;
        private IntPtr _btwRecordingTargetHwnd;
        private long _reCorrectGeneration;
        private readonly SemaphoreSlim _reCorrectApplyGate = new(1, 1);

        // PromptBoard integration: on-demand prefix lookup + side panel.
        private IAlwaysOnPrefixService? _alwaysOnPrefix;

        /// <summary>
        /// Service fuer die Prompt-Historie — geteilte Instanz aus
        /// VoiceServiceProvider, damit OverlayWindow und PromptBoardPanel
        /// durch denselben SemaphoreSlim-Lock gehen wenn sie parallel
        /// schreiben (Submit-Append) und lesen (ReloadHistory). Frueher
        /// hatte jede Klasse ihren eigenen Service mit eigenem Lock —
        /// File-Race war moeglich beim sehr schnellen Submit + Re-Render-Zyklus.
        /// </summary>
        private readonly PromptHistoryService _historyService = VoiceServiceProvider.History;

        /// <summary>Geteilter Prompt-Zwischenspeicher-Service (10 Slots).</summary>
        private readonly PromptSlotService _slotService = VoiceServiceProvider.Slots;

        /// <summary>
        /// Drive-Sync der Historie. Lazy initialisiert — beim ersten Submit
        /// oder beim Mergen am App-Start. Wenn Drive nicht verbunden ist,
        /// wirft die erste Operation eine Exception, die wir still
        /// schlucken (Sync ist eine Komfort-Funktion, kein Pflichtkanal).
        /// </summary>
        private PromptHistoryDriveSync? _historySync;

        /// <summary>Drive-Sync der Prompt-Zwischenspeicher-Slots. Lazy wie _historySync.</summary>
        private PromptSlotDriveSync? _slotSync;

        /// <summary>Drive-Sync des persoenlichen Vokabular-Woerterbuchs. Lazy wie _slotSync.</summary>
        private PromptVocabularyDriveSync? _vocabSync;

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
                    // gemini-3.1-flash-lite ist das Standard-Modell
                    // der Voice Terminal Overlay App — alle Gemini-Pfade
                    // (Diktat-Cleanup, Prompt-Improvement, Historie-Titel)
                    // nutzen dasselbe Modell, damit Verhalten und Latenz
                    // ueberall vorhersagbar sind. ThinkingLevel bleibt leer:
                    // das Lite-Modell akzeptiert keinen thinkingConfig-Block,
                    // der Client laesst ihn dann komplett aus dem Payload.
                    return new GeminiClient(key, "gemini-3.1-flash-lite", "");
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
        /// Kuerzt einen String fuer Console/Logging auf maximal 30 Zeichen
        /// plus Zeichen-Anzahl. Frueher loggten wir den vollstaendigen
        /// Whisper-Output und Gemini-Output direkt in stdout — wenn
        /// der Benutzer versehentlich Passwoerter, API-Tokens oder andere
        /// sensible Daten einsprach, landeten die im persistierten Log.
        /// Mit Preview-Format "vorne(20)…hinten(10) [N chars]" sieht Frank
        /// genug zum Debuggen ob Whisper plausibel arbeitet, ohne dass
        /// sensible Mitten verloren gehen koennen. Newlines werden zu
        /// Spaces damit eine logische Zeile pro Logging-Eintrag bleibt.
        /// </summary>
        private static string SafeLogPreview(string? text)
        {
            if (string.IsNullOrEmpty(text)) return "<empty>";
            string flat = text.Replace('\n', ' ').Replace('\r', ' ').Replace('\t', ' ');
            int len = flat.Length;
            if (len <= 30) return $"{flat} [{len} chars]";
            return $"{flat.Substring(0, 20)}…{flat.Substring(len - 10)} [{len} chars]";
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
        private bool _geminiUploadSubscribed;
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
        // Konfigurierbar via Umgebungsvariable TVO_RELEASE_BUNDLE_FOLDER —
        // sonst Fallback auf den Standard-Pfad relativ zum User-Profile.
        // Frueher hardcoded auf C:\Users\barwa\... — funktionierte nur fuer
        // Frank, jeder andere Tester sah "Pfad nicht gefunden". Defensiv:
        // Environment.GetEnvironmentVariable kann null liefern, wir nutzen
        // string.IsNullOrWhiteSpace plus den abgeleiteten Default.
        private static string ReleaseBundleFolder
        {
            get
            {
                var env = Environment.GetEnvironmentVariable("TVO_RELEASE_BUNDLE_FOLDER");
                if (!string.IsNullOrWhiteSpace(env)) return env.Trim();
                var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
                return System.IO.Path.Combine(home,
                    "proggs", "BestJournalAndroid", "app", "build", "outputs", "bundle", "release");
            }
        }

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
        // Cooldown statt KeyUp-Debounce (Fix 2026-06-21): Bei der langen
        // Screenshot+Insert-Operation (~420 ms) wurde der KeyUp verschluckt, ein
        // bool-Flag blieb dauerhaft auf true haengen und der Hotkey feuerte nie
        // wieder. Ein zeitbasierter Cooldown laeuft IMMER von selbst ab — wie
        // _altF12CooldownUntil. Damit kann der Hotkey nicht mehr "tot" haengen.
        private DateTime _screenshotCooldownUntil = DateTime.MinValue;
        private DateTime _insertCooldownUntil     = DateTime.MinValue;
        private const int ScreenshotCooldownMs = 800;  // > Operationsdauer (Screenshot+Insert ~420 ms)
        private const int InsertCooldownMs      = 500;  // nur Insert/Paste

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
        // ── Gespeicherte Overlay-Position pro Orientierung (nur in dieser
        // Session, NICHT persistiert) ──
        // Frank-Wunsch 2026-05-25: Die Diskette neben/unter dem Enter-Button
        // merkt sich die aktuelle Position fuer die JEWEILIGE Orientierung.
        // Beim Wechsel vertikal↔horizontal wird die zur Zielorientierung
        // gespeicherte Position wieder eingenommen. Bewusst NUR im RAM:
        // nach App-/PC-Neustart sind beide null → die kanonische
        // Standardposition (oben-rechts vertikal, unten-rechts horizontal)
        // gilt wieder. Gespeichert wird IMMER die Position der AUSGEKLAPPTEN
        // Ansicht (die Diskette ist nur dann sichtbar), damit die
        // Collapse-Offset-Mathematik unveraendert weiterfunktioniert.
        private Point? _savedHorizontalPos;
        private Point? _savedVerticalPos;
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
        // Sorgt periodisch dafuer, dass die Pille im Z-Order ganz oben bleibt
        // — auch waehrend langer Aufnahmen mit Fensterwechsel, wo Desktop-
        // Widgets sonst die Pille verdraengen koennen (Bugfix 2026-05-10).
        private readonly DispatcherTimer _topmostAssertTimer;
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
        // Sichtbarkeits-Spiegel fuer den Audio-Pegel-Listener. Wird von
        // SetWaveformVisible auf dem UI-Thread gesetzt und von OnAudioLevelChanged
        // auf dem NAudio-Thread gelesen — volatile reicht fuer einen
        // Single-Writer/Single-Reader-Bool. Spart pro Audio-Buffer ein
        // Dispatcher.BeginInvoke wenn die Welle ohnehin nicht sichtbar ist
        // (z.B. waehrend BTW-Aufnahme): ohne Spiegel wuerden alle 100 ms
        // ein Lambda + ein Marshall in die Dispatcher-Queue gepostet, nur
        // damit das Lambda dann beim Visibility-Check direkt zurueckspringt.
        private volatile bool _waveformVisibleFast;

        // ── Constructor ──

        public OverlayWindow(Config config)
        {
            InitializeComponent();

            _audioRecorder   = new AudioRecorder(config.AudioSampleRate, config.AudioChannels);
            _recordingCuePlayer = new RecordingCuePlayer();
            _groqClient      = new GroqWhisperClient(config.GroqApiKey, config.WhisperModel, config.WhisperLang, config.WhisperUrl);
            _appWatcher = new AppWatcher(config.TargetProcessNames);

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
            // zurueck zum Terminal, wird der Timer in OnAppActivated
            // gestoppt — das Overlay bleibt sichtbar.
            _hideDelayTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(5) };
            _hideDelayTimer.Tick += (_, _) =>
            {
                _hideDelayTimer.Stop();
                // Nicht verstecken, solange der Benutzer noch in unserer UI
                // arbeitet: Cursor ueber einem unserer Fenster/Popups (z.B.
                // offenes Kontextmenue "Hotkey zuweisen" mit A..Z-Untermenue)
                // ODER ein modaler Dialog offen. Sonst verschwand das Overlay
                // mitten im Bearbeiten (Terminal-Deaktivierung durch Popup → 5s → Hide).
                if (IsCursorOverOwnUi() || IsAuxiliaryWindowOpen())
                {
                    _hideDelayTimer.Start();
                    return;
                }
                HideOverlayNow();
            };

            // ── Topmost-Reassert-Timer (Bugfix 2026-05-10) ──
            // WPF Topmost="True" ist "best-effort": andere Topmost-Fenster
            // (Desktop-Widgets, Pop-ups, immer-im-Vordergrund-Apps) koennen
            // die Pille im Z-Order nach hinten draengen — besonders bei
            // langen Aufnahmen + haeufigem Fensterwechsel. Der Hide-Pfad in
            // OnAppActivated greift hier nicht, weil die Pille
            // waehrend einer Aufnahme sichtbar bleibt und der Show()-Branch
            // uebersprungen wird.
            //
            // Loesung: Alle 2,5 s die Pille (und sichtbare Children) per
            // SetWindowPos(HWND_TOPMOST, NOMOVE|NOSIZE|NOACTIVATE) zurueck
            // auf absolute Topmost-Position kicken. NOACTIVATE verhindert,
            // dass die Pille dabei den Fokus klaut. Kein Sichtbarwerden,
            // kein Aufflackern, kein Klau von Tastatur-Fokus — nur Z-Order.
            _topmostAssertTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(2500) };
            _topmostAssertTimer.Tick += (_, _) => ReassertTopmostIfVisible();
            _topmostAssertTimer.Start();

            // ── Initial button colours ──
            // G-button is on by default — falls back to Whisper-raw if no Gemini API key.
            if (_geminiClient == null) geminiEnabled = false;
            XButton.Background    = BtnX;           // red
            WButton.Background    = geminiEnabled ? ToggleOff : ToggleOn;  // green when Gemini off (Whisper-raw)
            MicButton.Background  = BtnMicIdlePurple;  // lila = CVO (Claude), nicht gelb wie TVO
            BtwButton.Background  = BtnBtwIdle;         // lila
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
            AttachHover(SaveButton);

            // ── Auto-Hide / Hover-to-Expand initialisieren ──
            InitAutoHide();

            // ── Terminal watcher ──
            _appWatcher.AppActivated   += OnAppActivated;
            _appWatcher.AppDeactivated += OnAppDeactivated;
            _appWatcher.Start();

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

            // Cloud-Merge der Prompt-Zwischenspeicher-Slots: gleiche Idee.
            _ = TryMergeSlotsFromCloudAsync();

            // Cloud-Merge des persoenlichen Vokabular-Woerterbuchs: gleiche Idee.
            _ = TryMergeVocabularyFromCloudAsync();
            GeminiPromptDriveSync.TrySyncFromCloud();   // Prompts + Schalter per Timestamp vom Backup holen
        }

        // ── Hover animation helper ──

        private static void AttachHover(System.Windows.Controls.Button btn)
        {
            btn.RenderTransformOrigin = new Point(0.5, 0.5);
            btn.RenderTransform       = new ScaleTransform(1.0, 1.0);

            btn.MouseEnter += (_, _) => AnimateScale(btn, 1.15, TimeSpan.FromMilliseconds(150));
            btn.MouseLeave += (_, _) => AnimateScale(btn, 1.0,  TimeSpan.FromMilliseconds(150));
        }

        // Geteilte, eingefrorene EasingFunction fuer alle Hover-Scale-Animationen.
        // Frueher: jede AnimateScale-Aufrufstelle (MouseEnter + MouseLeave auf
        // 21 Buttons) allokierte eine neue QuadraticEase. Mit Freezable-Sharing
        // wird die Easing-Berechnung ohne Cross-Thread-Notification-Overhead
        // genutzt, und der Allocator sieht pro Hover-Event ein Objekt weniger.
        // QuadraticEase ist DependencyObject + Freezable — `Freeze()` macht
        // sie thread-safe und teilbar ueber alle Animations.
        private static readonly QuadraticEase HoverEaseOut = CreateFrozenEase();
        private static QuadraticEase CreateFrozenEase()
        {
            var ease = new QuadraticEase { EasingMode = EasingMode.EaseOut };
            ease.Freeze();
            return ease;
        }

        private static void AnimateScale(System.Windows.Controls.Button btn, double to, TimeSpan duration)
        {
            // EasingFunction wird statisch geteilt — siehe HoverEaseOut. Die
            // DoubleAnimation-Objekte bleiben pro Aufruf neu, weil "to" und
            // "duration" pro Aufruf unterschiedlich sein koennen (Enter zu
            // 1.15, Leave zu 1.0).
            var animX = new DoubleAnimation(to, new Duration(duration)) { EasingFunction = HoverEaseOut };
            var animY = new DoubleAnimation(to, new Duration(duration)) { EasingFunction = HoverEaseOut };

            if (btn.RenderTransform is ScaleTransform st)
            {
                st.BeginAnimation(ScaleTransform.ScaleXProperty, animX);
                st.BeginAnimation(ScaleTransform.ScaleYProperty, animY);
            }
        }

        // ════════════════════════════════════════════════════════════
        //  Auto-Hide / Hover-to-Expand
        //  Zwei Zustaende: ausgeklappt (volles Overlay) und eingeklappt
        //  (nur der Mic-Button). Hover klappt aus; Maus-Verlassen klappt
        //  unmittelbar nach Verlassen der gesamten eigenen UI
        //  wieder ein. Waehrend einer Aufnahme bleibt es immer ausgeklappt.
        //  Per Einstellung (AppSettings.AutoHide) komplett abschaltbar —
        //  dann verhaelt sich das Overlay wie frueher (immer voll sichtbar).
        // ════════════════════════════════════════════════════════════

        // Layout-Konstanten in WPF-DIPs (DPI-unabhaengig). Aus dem festen
        // XAML-Layout berechnet: die Mic-Mitte liegt im vollen Pillar bei
        // y ≈ 98, im eingeklappten Pillar (Hoehe 96) bei y = 48 (Fenster-Mitte,
        // Kreis ist zentriert). Differenz = 50 — um diesen Betrag schiebt sich
        // das Fenster beim Einklappen nach unten, damit der Mic-Button exakt an
        // derselben Bildschirm-Position bleibt (kein Springen). Breite bleibt
        // konstant 96, daher ist keine horizontale Verschiebung noetig.
        // Hoehe 96 (frueher 64): gibt dem runden Kreis (84px) genug Platz, damit
        // der 52px-Mic-Button rundum ~16px Luft hat statt herauszuquellen
        // (Frank-Wunsch 2026-05-25: "ein ganz normaler, schoener, runder Kreis").
        private const double CollapsedHeight   = 96;
        private const double FullHeight        = 612;
        private const double CollapseTopOffset = 50;
        // Abstand der vertikalen Saeulen-Oberkante vom oberen Rand der Monitor-
        // Arbeitsflaeche (_waY). Das ist die kanonische "obere Linie" der Saeule —
        // auch der Beam-Effekt blendet GENAU hier wieder ein, nicht hoeher
        // (Frank 2026-05-25: "im Viereck bleiben, nicht nach oben vergroessern").
        private const double VerticalTopOffset = 57;
        // Kurzer Schutz gegen WPF-MouseLeave-Flattern beim Wechsel zwischen
        // Controls. 350 ms liegen sicher hinter dem 240-ms-Fade-Out, ohne die
        // fruehere volle Sekunde Wartezeit wieder einzufuehren.
        private const int CollapseAfterUseMs  = 350;
        private const int CollapseAfterPeekMs = 350;

        // Horizontaler Modus: Abstand (in DIPs) zwischen dem UNTEREN Rand der
        // Leiste und dem unteren Rand der Monitor-Arbeitsflaeche (= Oberkante
        // der Taskleiste). Frueher hart 40 — dadurch klebte die Leiste direkt
        // ueber der Taskleiste und ueberlappte die Claude-Code-Eingabezeile,
        // die Statuszeile und den bypass-Hinweis. Frank's Wunsch 2026-05-23:
        // die Leiste soll im Horizontal-Modus IMMER etwas hoeher sitzen, knapp
        // oberhalb der CLI-Eingabezeile, wo er tippt. 150 DIPs raeumen das
        // Eingabefeld + Statuszeile + bypass-Zeile frei. Das ist der EINZIGE
        // Stellknopf — hier anpassen, falls die Leiste zu hoch oder zu tief sitzt.
        private const double HBarBottomLift = 150;

        private bool _autoHideEnabled = true;
        private bool _isCollapsed;
        private bool _mouseOverOverlay;
        private bool _usedSinceExpand;
        private DispatcherTimer? _collapseTimer;
        // Expandierte Fensterposition, gemerkt beim Einklappen → exakt
        // wiederhergestellt beim Aufklappen (absolut statt relativ, damit der
        // Beam-Crossfade auch bei Hover-Ueberlappung konsistent bleibt).
        private double _preCollapseLeft, _preCollapseTop;
        private int _collapseBeamGen;

        private void InitAutoHide()
        {
            // Einstellung synchron lesen. Der PromptBoardHost ist zu diesem
            // Zeitpunkt bereits initialisiert (siehe App.StartOverlay, der
            // Initialize() VOR new OverlayWindow(config) aufruft). Bei jedem
            // Fehler: Default true — Feature an, exakt wie der Modell-Default.
            bool startupHorizontal = false;
            try
            {
                using var scope = PromptBoardHost.Services.CreateScope();
                var ctx = scope.ServiceProvider
                    .GetRequiredService<PromptBoard.Data.PromptBoardDbContext>();
                var settings = ctx.AppSettings.FirstOrDefault();
                _autoHideEnabled = settings?.AutoHide ?? true;
                startupHorizontal = string.Equals(settings?.Orientation, "horizontal",
                    StringComparison.OrdinalIgnoreCase);

                // Die Diskette ist die einzige Positions-Persistenzsteuerung:
                // gespeicherte Koordinaten werden bei jedem Start geladen, null
                // bedeutet fuer die jeweilige Ausrichtung Standardposition.
                if (settings?.OverlayVerticalLeft is double vl && settings.OverlayVerticalTop is double vt)
                    _savedVerticalPos = new Point(vl, vt);
                if (settings?.OverlayHorizontalLeft is double hl && settings.OverlayHorizontalTop is double ht)
                    _savedHorizontalPos = new Point(hl, ht);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"InitAutoHide: settings read failed, defaulting ON/vertical: {ex.Message}");
                _autoHideEnabled = true;
            }

            // Einklapp-Timer. Das kurze Intervall wird pro Ausloesung gesetzt.
            _collapseTimer = new DispatcherTimer();
            _collapseTimer.Tick += (_, _) =>
            {
                _collapseTimer!.Stop();
                if (_micState == RecordingState.Recording || isBtwRecording)
                    return;
                if (_orientationTransitioning || IsCursorOverOwnUi()
                    || IsAuxiliaryWindowOpen() || IsTransientChildVisible())
                {
                    _collapseTimer.Start();
                    return;
                }
                CollapseImmediate();
            };

            // Fenster-weite Hover-Erkennung. MouseEnter/MouseLeave am Window
            // feuern beim Betreten/Verlassen der gesamten Fensterflaeche —
            // unabhaengig von den Button-internen Hover-Skalierungen.
            this.MouseEnter += (_, _) =>
            {
                if (!_autoHideEnabled) return;
                _mouseOverOverlay = true;
                _collapseTimer?.Stop();
                if (_isCollapsed) Expand();
            };
            this.MouseLeave += (_, _) =>
            {
                if (!_autoHideEnabled) return;
                _mouseOverOverlay = false;
                ScheduleCollapse();
            };

            // Interaktionen weiterhin markieren, damit bestehende Zustands-
            // und Diagnosepfade ihre Semantik behalten.
            FullView.PreviewMouseDown += (_, _) => _usedSinceExpand = true;

            // Hover-Animation fuer den neuen Orientierungs-Umschalter.
            AttachHover(OrientationToggleButton);

            // Startup-Orientierung anwenden — nur wenn horizontal (vertikal ist
            // bereits der XAML-Standard). Position folgt in OnAppActivated.
            if (startupHorizontal) ApplyOrientation(true);

            // Initialen Diskette-Zustand setzen (gruen falls fuer die aktuelle
            // Ausrichtung bereits eine Position geladen/gemerkt ist).
            UpdateSaveIndicator();
        }

        /// <summary>
        /// Klappt ohne kuenstliche Wartezeit ein, sobald der Cursor die gesamte
        /// eigene UI verlassen hat. Nur beim Wechsel in ein eigenes Zusatzfenster
        /// prueft ein kurzer Poller weiter, bis auch dieses verlassen wurde.
        /// </summary>
        // True solange die Maus ueber dem Overlay, dem Promptboard ODER dem
        // Eingabefenster ist. Promptboard + Eingabe sind EIGENE Fenster, daher
        // feuert das Overlay-MouseLeave beim Hinueberfahren — wir duerfen aber
        // NICHT einklappen, solange der Benutzer eines dieser Fenster benutzt.
        private bool IsMouseOverAnyPart()
        {
            if (_mouseOverOverlay) return true;
            try
            {
                if (IsMouseOver) return true;
                if (_promptPanel is { IsVisible: true } p && p.IsMouseOver) return true;
                if (_promptPanel?.InputWindow is { IsVisible: true } iw && iw.IsMouseOver) return true;
            }
            catch { }
            return false;
        }

        /// <summary>
        /// True solange ein modaler Hilfs-/Editier-Dialog sichtbar offen ist
        /// (PromptEditDialog, SettingsDialog, ConfirmDialog, TextInputDialog,
        /// PromptHistoryEditDialog). Diese Dialoge liegen UEBER dem Promptboard
        /// und werden von <see cref="IsMouseOverAnyPart"/> nicht erfasst (der
        /// Mauszeiger ist dann ueber dem Dialog, nicht ueber Board/Eingabe).
        /// Ohne diese Pruefung klappt der Auto-Hide-Timer das Overlay waehrend
        /// des Bearbeitens ein und versteckt das Board hinter dem Dialog —
        /// auch waehrend ein modaler ShowDialog() laeuft tickt der
        /// DispatcherTimer naemlich weiter (nested message loop).
        ///
        /// Erkennung ueber Ausschluss: jedes SICHTBARE Top-Level-Window, das
        /// KEINE der vier Kern-Oberflaechen ist (Overlay, Board, Eingabe,
        /// Historie), gilt als offener Dialog. Versteckte Fenster zaehlen
        /// nicht (IsVisible-Filter). Gilt orientierungsunabhaengig — vertikal
        /// wie horizontal nutzen denselben Collapse-Timer.
        /// </summary>
        private bool IsAuxiliaryWindowOpen()
        {
            try
            {
                var app = System.Windows.Application.Current;
                if (app is null) return false;
                foreach (Window w in app.Windows)
                {
                    if (w is null || !w.IsVisible) continue;
                    if (w is OverlayWindow or PromptBoardPanel or PromptInputWindow or PromptHistoryWindow)
                        continue;
                    return true;
                }
            }
            catch { /* Enumeration faellt praktisch nie aus; Default = normales Verhalten. */ }
            return false;
        }

        /// <summary>
        /// Robuste Maus-ueber-Pruefung per ECHTER Cursor-Position statt WPFs
        /// <c>IsMouseOver</c>. Grund (gemessen 2026-05-24 via Diagnose-Log):
        /// die Promptboard-/Eingabe-Fenster tragen WS_EX_NOACTIVATE — WPFs
        /// IsMouseOver kippt dort unzuverlaessig auf False sobald die Maus kurz
        /// still steht oder ein WM_MOUSELEAVE verarbeitet wird, obwohl der
        /// Cursor physisch ueber dem Fenster liegt. Der Auto-Hide-Timer klappte
        /// dann mitten im Bearbeiten ein. Hier fragen wir Windows direkt:
        /// liegt der Cursor geometrisch in einem unserer SICHTBAREN Fenster?
        ///
        /// GetCursorPos UND GetWindowRect liefern beide GERAETEPIXEL (physische
        /// Bildschirmkoordinaten) — daher direkt vergleichbar, ohne DPI-Mathe.
        /// Deckt Overlay, Promptboard, Eingabe, Historie UND offene Dialoge ab
        /// (alle sichtbaren Fenster dieses Prozesses). Orientierungsunabhaengig.
        /// </summary>
        private bool IsCursorOverOwnUi()
        {
            try
            {
                if (!NativeMethods.Win32.GetCursorPos(out var pt)) return false;
                IntPtr hwnd = NativeMethods.Win32.WindowFromPoint(pt);
                if (hwnd == IntPtr.Zero) return false;
                // Gehoert das Fenster unter dem Cursor UNSEREM Prozess? Dann
                // liegt der Cursor ueber Overlay, Board, Eingabe, Historie,
                // einem Dialog ODER einem offenen Kontextmenue/Untermenue
                // (alles HWNDs dieses Prozesses) — nicht einklappen/verstecken.
                NativeMethods.Win32.GetWindowThreadProcessId(hwnd, out uint pid);
                return pid != 0 && pid == (uint)Environment.ProcessId;
            }
            catch { }
            return false;
        }

        /// <summary>
        /// True solange ein transientes Kind-Fenster (Prompt-Eingabe oder
        /// Historie) sichtbar ist. Diese Fenster sind eigene Top-Level-Windows
        /// links neben dem Board; beim Scrollen/Arbeiten darin ist die reine
        /// Cursor-Punkt-Abtastung (IsCursorOverOwnUi) zu fragil — ein einziger
        /// 2-Sekunden-Sample knapp neben dem Fenster reichte zum Einklappen
        /// (gemessen 2026-05-24). Solange ein Kind sichtbar ist, arbeitet der
        /// Benutzer damit → der Auto-Einklapp-Timer pausiert. NUR fuer den
        /// Collapse-Timer gedacht — der Hide-Timer (echter App-Wechsel) versteckt
        /// die Kinder weiterhin, damit sie nie ueber Chrome o.ae. stehen bleiben.
        /// </summary>
        private bool IsTransientChildVisible()
        {
            var p = _promptPanel;
            if (p is null) return false;
            try { return p.IsInputWindowVisible || p.IsHistoryWindowVisible; }
            catch { return false; }
        }

        private void ScheduleCollapse()
        {
            if (!_autoHideEnabled || _isCollapsed || _collapseTimer is null) return;
            if (_micState == RecordingState.Recording || isBtwRecording) return;
            if (_mouseOverOverlay) return;

            int ms = _usedSinceExpand ? CollapseAfterUseMs : CollapseAfterPeekMs;
            _collapseTimer.Interval = TimeSpan.FromMilliseconds(ms);
            _collapseTimer.Stop();
            _collapseTimer.Start();
        }

        /// <summary>
        /// Klappt das Overlay wieder zur vollen Form auf — mit Beam-Crossfade.
        /// </summary>
        private void Expand()
        {
            if (!_isCollapsed) return;
            _isCollapsed = false;
            _usedSinceExpand = false;
            _collapseTimer?.Stop();

            int gen = ++_collapseBeamGen;
            BeamFadeOut(CollapsedView, () =>
            {
                if (gen != _collapseBeamGen) return;
                CollapsedView.Visibility = Visibility.Collapsed;
                if (_isHorizontal)
                {
                    HorizontalView.Visibility = Visibility.Visible;
                    SizeToContent = SizeToContent.WidthAndHeight;
                    UpdateLayout();
                }
                else
                {
                    FullView.Visibility = Visibility.Visible;
                    SizeToContent = SizeToContent.Manual;
                    Width  = 96;
                    Height = FullHeight;
                }
                Left = _preCollapseLeft;
                Top  = _preCollapseTop;
                BeamFadeIn(_isHorizontal ? HorizontalView : (UIElement)FullView);
                ShowPromptUiAfterExpand();
                ReassertTopmostIfVisible();
            });
        }

        /// <summary>
        /// Klappt das Overlay auf die kompakte Mic-Pille ein — mit Beam-Crossfade.
        /// </summary>
        private void CollapseImmediate()
        {
            if (!_autoHideEnabled || _isCollapsed) return;
            _collapseTimer?.Stop();
            _isCollapsed = true;
            _usedSinceExpand = false;

            // Expandierte Position merken (zum Wiederherstellen beim Aufklappen).
            _preCollapseLeft = Left;
            _preCollapseTop  = Top;

            // Ziel-Geometrie der Mic-Pille JETZT (an der expandierten Form) messen,
            // bevor irgendetwas ausgeblendet/veraendert wird.
            double collapsedLeft, collapsedTop;
            if (_isHorizontal)
            {
                double micCx, micCy;
                try
                {
                    var p = MicButton.TransformToAncestor(this).Transform(
                        new Point(MicButton.ActualWidth / 2, MicButton.ActualHeight / 2));
                    micCx = Left + p.X;
                    micCy = Top + p.Y;
                }
                catch
                {
                    micCx = Left + ActualWidth / 2;
                    micCy = Top + ActualHeight / 2;
                }
                // CollapsedView-Mic-Mitte liegt bei (48,48) im 96x96-Fenster
                // (Kreis zentriert) → Fenster so platzieren, dass der Mic genau
                // an seiner bisherigen Bildschirmposition bleibt.
                collapsedLeft = micCx - 48;               // halbe Breite (96/2)
                collapsedTop  = micCy - CollapsedHeight / 2.0;
            }
            else
            {
                // Vertikal: Breite bleibt 96, Fenster rutscht um CollapseTopOffset
                // nach unten, damit der Mic an Ort bleibt (absolut vorberechnet).
                collapsedLeft = Left;
                collapsedTop  = Top + CollapseTopOffset;
            }

            int gen = ++_collapseBeamGen;
            UIElement fromView = _isHorizontal ? (UIElement)HorizontalView : FullView;
            BeamFadeOut(fromView, () =>
            {
                if (gen != _collapseBeamGen) return;
                fromView.Visibility = Visibility.Collapsed;
                CollapsedView.Visibility = Visibility.Visible;
                SizeToContent = SizeToContent.Manual;
                Width  = 96;
                Height = CollapsedHeight;
                Left = collapsedLeft;
                Top  = collapsedTop;
                BeamFadeIn(CollapsedView);
                HidePromptUiForCollapse();
                ReassertTopmostIfVisible();
            });
        }

        /// <summary>
        /// Klick auf den eingeklappten Mic: ausklappen und sofort die normale
        /// Aufnahme-Logik ausloesen, als haette man den vollen Mic geklickt.
        /// (In der Regel klappt schon das Hover aus, bevor der Klick kommt —
        /// dieser Handler faengt den Direktklick ab.)
        /// </summary>
        private void CollapsedMicButton_Click(object sender, RoutedEventArgs e)
        {
            // Erst ausklappen (Expand() setzt _usedSinceExpand zurueck), DANN
            // als "benutzt" markieren.
            Expand();
            _usedSinceExpand = true;
            BtnMic_Click(MicButton, new RoutedEventArgs());
        }

        // ════════════════════════════════════════════════════════════
        //  Orientierung: vertikal (Standard) ↔ horizontal
        //  KEINE doppelten Buttons — dieselben Instanzen werden zwischen
        //  FullView (vertikal) und HorizontalView/HBar umgehaengt. Dadurch
        //  bleibt die gesamte Zustands-Logik (SetMicState, Profile, Aufnahme)
        //  unveraendert. Die vertikale XAML ist und bleibt Standard + Fallback.
        //  Auto-Hide und die Mic-zentrierte Collapse-Geometrie gelten fuer
        //  vertikalen und horizontalen Modus gleichermassen.
        // ════════════════════════════════════════════════════════════

        private bool _isHorizontal;
        private bool _orientationTransitioning;
        private bool _orientationCaptured;
        private readonly Dictionary<FrameworkElement, (DependencyObject parent, int index, Thickness margin, double w, double h)> _vplace = new();
        // Zuletzt bekannte Monitor-Arbeitsflaeche (aus OnAppActivated),
        // damit der In-Overlay-Umschalter auch ohne frischen Fokuswechsel
        // korrekt positionieren kann.
        private double _waX, _waY, _waW, _waH;
        // Normale (vertikale) Promptboard-Breite — gemerkt, damit sie nach einem
        // Horizontal-Ausflug (wo das Board auf Leistenbreite gesetzt wird) wieder
        // hergestellt werden kann.
        private double _boardVerticalWidth;

        // Alle Buttons die zwischen den Layouts wandern — in Dokument-Reihenfolge
        // pro Eltern-Panel (aufsteigender Index), damit das Zurueckhaengen in
        // StackPanels die richtige Reihenfolge ergibt.
        private FrameworkElement[] ManagedButtons() => new FrameworkElement[]
        {
            UltrathinkButton, OrientationToggleButton,
            MicButton, BtwButton,
            Profile1Button, Profile2Button, Profile3Button,
            WButton, GButton,
            Profile4Button, Profile5Button,
            XButton, Profile6Button,
            CopyButton, PasteButton,
            Profile7Button, Profile8Button,
            ScreenshotButton, InsertScreenshotButton,
            Profile9Button, Profile10Button,
            EnterButton, SaveButton,
        };

        // Kurzer Einblende-Effekt (~140ms) fuer die gerade sichtbar gewordene
        // Ansicht — macht den Wechsel weich statt ruckartig (Ein-/Ausklappen
        // und Orientierungswechsel, vertikal wie horizontal).
        private static void FadeIn(UIElement el)
        {
            el.BeginAnimation(UIElement.OpacityProperty, null);
            el.Opacity = 0;
            el.BeginAnimation(UIElement.OpacityProperty,
                new DoubleAnimation(0, 1, new Duration(TimeSpan.FromMilliseconds(140))) { EasingFunction = HoverEaseOut });
        }

        // ── Beam-Effekt fuer den Orientierungswechsel (Frank 2026-05-25) ──
        // Sehr weiche CubicEase EaseInOut — geschmeidiger als die quadratische
        // Hover-Ease, passt zum bewussten, langsamen Teleport-Look. Eingefroren
        // (Freeze) damit sie thread-safe ueber alle Beam-Animationen geteilt wird.
        private static readonly CubicEase BeamEase = CreateFrozenBeamEase();
        private static CubicEase CreateFrozenBeamEase()
        {
            var e = new CubicEase { EasingMode = EasingMode.EaseInOut };
            e.Freeze();
            return e;
        }

        // Beam-Ausblenden: die aktuelle Form sichtbar KOMPLETT verschwinden
        // lassen (~240ms, 1 → 0), dann onDone. Explizit von 1 starten (nicht von
        // el.Opacity): nach einer vorher gehaltenen Animation kann der lokale
        // Opacity-Basiswert 0 sein — ein "von el.Opacity" wuerde dann 0→0
        // animieren (kein sichtbares Ausblenden). Teil des Orientierungs-Beams.
        private static void BeamFadeOut(UIElement el, Action? onDone = null)
        {
            el.BeginAnimation(UIElement.OpacityProperty, null);
            var anim = new DoubleAnimation(1, 0, new Duration(TimeSpan.FromMilliseconds(240)))
            { EasingFunction = BeamEase };
            anim.Completed += (_, _) => onDone?.Invoke();
            el.BeginAnimation(UIElement.OpacityProperty, anim);
        }

        // Beam-Einblenden: die Zielform oben langsam + sehr weich wieder
        // erscheinen lassen (~380ms, 0 → 1), dann onDone (= an die Stelle
        // fallen). Bewusst laenger als das Ausblenden, damit der "Reinbeam"-
        // Moment ruhig und weich wirkt (Frank: "relativ langsam, sehr weich").
        private static void BeamFadeIn(UIElement el, Action? onDone = null)
        {
            el.BeginAnimation(UIElement.OpacityProperty, null);
            el.Opacity = 0;
            var anim = new DoubleAnimation(0, 1, new Duration(TimeSpan.FromMilliseconds(380)))
            { EasingFunction = BeamEase };
            anim.Completed += (_, _) => onDone?.Invoke();
            el.BeginAnimation(UIElement.OpacityProperty, anim);
        }

        // Beim Einklappen das Promptboard + Eingabefeld mit ausblenden. Die
        // Absicht (alwaysOnActive / _inputSoloDock) bleibt erhalten, damit beim
        // Aufklappen wiederhergestellt wird was vorher offen war.
        private void HidePromptUiForCollapse()
        {
            try
            {
                _promptPanel?.HideTransientChildren();
                if (_promptPanel is not null && _promptPanel.IsVisible) _promptPanel.Hide();
                if (_inputSoloDock && _promptPanel?.InputWindow is { IsVisible: true } iw) iw.Hide();
            }
            catch (Exception ex) { Console.WriteLine($"HidePromptUiForCollapse: {ex.Message}"); }
        }

        // Beim Aufklappen das vorher aktive Promptboard / Eingabefeld wieder
        // einblenden — exakt die gleiche Logik wie OnAppActivated.
        private void ShowPromptUiAfterExpand()
        {
            try
            {
                if (_inputSoloDock && _promptPanel?.InputWindow is { } soloInput)
                {
                    if (!soloInput.IsVisible) soloInput.Show();
                    PositionSoloInput();
                }
                else
                {
                    if (alwaysOnActive && _promptPanel is not null && !_promptPanel.IsVisible)
                    {
                        PositionPromptPanel();
                        _promptPanel.Show();
                    }
                    _promptPanel?.ShowTransientChildrenIfNeeded();
                }
            }
            catch (Exception ex) { Console.WriteLine($"ShowPromptUiAfterExpand: {ex.Message}"); }
        }

        private void CaptureVerticalPlacement()
        {
            if (_orientationCaptured) return;
            foreach (var el in ManagedButtons())
            {
                int index = el.Parent is StackPanel sp ? sp.Children.IndexOf(el) : -1;
                _vplace[el] = (el.Parent, index, el.Margin, el.Width, el.Height);
            }
            _orientationCaptured = true;
        }

        private static void DetachFromParent(FrameworkElement el)
        {
            switch (el.Parent)
            {
                case Panel p:           p.Children.Remove(el); break;
                case Border b:          b.Child = null;        break;
                case ContentControl cc: cc.Content = null;     break;
                case Decorator d:       d.Child = null;        break;
            }
        }

        // Baut die horizontale Leiste. Die komplette Stern-/Toggle-Randgruppe
        // sitzt links, die komplette Enter-/Speichern-Randgruppe rechts.
        private void BuildHorizontalLayout()
        {
            HBar.Children.Clear();
            // Die Randgruppen werden als Ganzes gespiegelt. Dadurch wandern ihre
            // individuellen Breiten, Hintergründe und Button-Funktionen mit.
            // Pro Gruppe die gleiche Sektionsfarbe wie vertikal (mit 70% Deckkraft,
            // Alpha B3) + senkrechte Trennstriche dazwischen — 1:1-Optik zum
            // vertikalen Layout, nur um 90° gedreht.
            // Stern + Diskette UNTEREINANDER (Stern oben, Diskette unten),
            // beide in Standard-Rundgröße und vertikal mittig.
            HBar.Children.Add(MakeHStackGroup(UltrathinkButton, SaveButton, "#B31F1B15", new CornerRadius(34, 0, 0, 34)));
            HBar.Children.Add(MakeVDivider());
            HBar.Children.Add(MakeHGroup(new[] { InsertScreenshotButton, ScreenshotButton }, new[] { Profile10Button, Profile9Button }, "#B3151B15", new CornerRadius(0)));
            HBar.Children.Add(MakeVDivider());
            HBar.Children.Add(MakeHGroup(new[] { PasteButton, CopyButton }, new[] { Profile8Button, Profile7Button }, "#B3151B1D", new CornerRadius(0)));
            HBar.Children.Add(MakeVDivider());
            HBar.Children.Add(MakeHGroup(new[] { XButton }, new[] { Profile6Button }, "#B31F1515", new CornerRadius(0)));
            HBar.Children.Add(MakeVDivider());
            HBar.Children.Add(MakeHGroup(new[] { GButton, WButton }, new[] { Profile5Button, Profile4Button }, "#B319151F", new CornerRadius(0)));
            HBar.Children.Add(MakeVDivider());
            HBar.Children.Add(MakeHGroup(new[] { BtwButton, MicButton }, new[] { Profile3Button, Profile2Button, Profile1Button }, "#B31F1C15", new CornerRadius(0)));
            HBar.Children.Add(MakeVDivider());
            // Enter oben + Umschalter darunter. MakeHGroup behält die bisherige
            // Breite dieser Randgruppe und setzt nur ihre Position nach rechts.
            HBar.Children.Add(MakeHGroup(new[] { EnterButton }, new[] { OrientationToggleButton }, "#B31A1A1A", new CornerRadius(0, 34, 34, 0)));
        }

        // Senkrechter Trennstrich zwischen den horizontalen Sektionen — das
        // Gegenstueck zu den waagerechten Strichen im vertikalen Layout.
        private static Border MakeVDivider() =>
            new Border { Width = 1, Background = Brush("#FF000000"), VerticalAlignment = VerticalAlignment.Stretch };

        private FrameworkElement MakeHGroup(Button[] symbols, Button[]? tiles, string bgHex, CornerRadius corner)
        {
            var col = new StackPanel { Orientation = Orientation.Vertical, VerticalAlignment = VerticalAlignment.Center };
            // Symbol-Reihe mit FESTER Hoehe (52): so liegen die Symbol-Mitten
            // aller Gruppen auf gleicher Hoehe, egal ob 40er- oder 52er-Button.
            var top = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Center, Height = 52 };
            foreach (var b in symbols)
            {
                try { DetachFromParent(b); b.Margin = new Thickness(3, 0, 3, 0); b.VerticalAlignment = VerticalAlignment.Center; top.Children.Add(b); }
                catch (Exception ex) { Console.WriteLine($"BuildHorizontalLayout sym {b.Name} FAIL: {ex.Message}"); }
            }
            col.Children.Add(top);

            // Zahlen-Reihe (FESTE Hoehe 22) NUR bei Gruppen die wirklich Zahlen
            // haben — damit ALLE Zahlen exakt auf einer Linie liegen. Gruppen
            // OHNE Zahl (z.B. Enter) bekommen KEINE leere Reihe; sonst wuerde der
            // einzelne Button durch die leere Reihe nach oben gedrueckt. Ohne die
            // Reihe zentriert sich der Button (col ist VerticalAlignment.Center)
            // vertikal mittig in der vollen Leistenhoehe — genau wie gewuenscht.
            if (tiles != null)
            {
                var bot = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Center, Height = 22, Margin = new Thickness(0, 6, 0, 0) };
                foreach (var t in tiles)
                {
                    // Kacheln liegend (30×22 statt 24×32) → flachere Zahlen-Reihe,
                    // dadurch wird die ganze horizontale Leiste schmaler.
                    try { DetachFromParent(t); t.Margin = new Thickness(3, 0, 3, 0); t.VerticalAlignment = VerticalAlignment.Center; t.Width = 30; t.Height = 22; bot.Children.Add(t); }
                    catch (Exception ex) { Console.WriteLine($"BuildHorizontalLayout tile {t.Name} FAIL: {ex.Message}"); }
                }
                col.Children.Add(bot);
            }
            return new Border
            {
                Background = Brush(bgHex),
                CornerRadius = corner,
                Padding = new Thickness(8, 6, 8, 6),
                VerticalAlignment = VerticalAlignment.Stretch, // fuellt die volle Leistenhoehe
                Child = col,
            };
        }

        // Spezial-Gruppe fuer Stern + Diskette im HORIZONTAL-Modus: beide
        // Buttons UNTEREINANDER (statt nebeneinander) in Standard-Rundgroesse
        // (40x40), vertikal mittig in der Leiste, ohne Zahlen-Slot. Im vertikalen
        // Modus bleiben sie nebeneinander (XAML Section1Panel) — RestoreVerticalLayout
        // setzt die urspruengliche Groesse (34) wieder zurueck.
        private FrameworkElement MakeHStackGroup(Button topBtn, Button bottomBtn, string bgHex, CornerRadius corner)
        {
            var col = new StackPanel
            {
                Orientation = Orientation.Vertical,
                HorizontalAlignment = HorizontalAlignment.Center,
                VerticalAlignment = VerticalAlignment.Center,
            };
            void Add(Button b, bool first)
            {
                try
                {
                    DetachFromParent(b);
                    b.Width = 34; b.Height = 34;            // gleiche Groesse wie im vertikalen Modus
                    b.Margin = first ? new Thickness(0) : new Thickness(0, 4, 0, 0);
                    b.HorizontalAlignment = HorizontalAlignment.Center;
                    b.VerticalAlignment = VerticalAlignment.Center;
                    col.Children.Add(b);
                }
                catch (Exception ex) { Console.WriteLine($"BuildHorizontalLayout stack {b.Name} FAIL: {ex.Message}"); }
            }
            Add(topBtn, true);
            Add(bottomBtn, false);
            return new Border
            {
                Background = Brush(bgHex),
                CornerRadius = corner,
                Padding = new Thickness(8, 6, 8, 6),
                VerticalAlignment = VerticalAlignment.Stretch,
                Child = col,
            };
        }

        private void RestoreVerticalLayout()
        {
            foreach (var el in ManagedButtons())
            {
                if (!_vplace.TryGetValue(el, out var vp)) { Console.WriteLine($"RestoreVerticalLayout: no vplace for {el.Name}"); continue; }
                try
                {
                    DetachFromParent(el);
                    el.Margin = vp.margin;
                    el.Width  = vp.w;   // Original-Groesse zurueck (Horizontal aendert Kachel-Groesse)
                    el.Height = vp.h;
                    switch (vp.parent)
                    {
                        // Anhaengen (Add) statt Index-Insert: ManagedButtons() ist in
                        // Dokument-Reihenfolge pro Panel → korrekte Original-Reihenfolge.
                        case Panel p:           p.Children.Add(el); break;
                        case Border b:          b.Child = el;       break;
                        case ContentControl cc: cc.Content = el;    break;
                        case Decorator d:       d.Child = el;       break;
                    }
                }
                catch (Exception ex) { Console.WriteLine($"RestoreVerticalLayout {el.Name} FAIL: {ex.Message}"); }
            }
            HBar.Children.Clear();
        }

        // Wendet die Orientierung an (Reparenting + Sichtbarkeit). Position +
        // Groesse setzt PositionForCurrentOrientation bzw. OnAppActivated.
        private void ApplyOrientation(bool horizontal)
        {
            CaptureVerticalPlacement();
            if (horizontal)
            {
                BuildHorizontalLayout();
                FullView.Visibility       = Visibility.Collapsed;
                CollapsedView.Visibility  = Visibility.Collapsed;
                HorizontalView.Opacity    = 0; // unsichtbar bis positioniert → kein Flash an alter Stelle
                HorizontalView.Visibility = Visibility.Visible;
                _isHorizontal = true;
                _isCollapsed  = false;
            }
            else
            {
                RestoreVerticalLayout();
                HorizontalView.Visibility = Visibility.Collapsed;
                FullView.Opacity          = 0; // unsichtbar bis positioniert → kein Flash unter der Linie
                FullView.Visibility       = Visibility.Visible;
                FullView.UpdateLayout(); // erzwingt frisches Re-Layout der Sektionen
                _isHorizontal = false;
            }
            FadeIn(horizontal ? HorizontalView : (UIElement)FullView);
            UpdateSaveIndicator(); // gruener Diskette-Zustand spiegelt die JETZT sichtbare Ausrichtung
            ReassertTopmostIfVisible();
        }

        private void BtnOrientationToggle_Click(object sender, RoutedEventArgs e)
        {
            if (_orientationTransitioning) return;
            bool target = !_isHorizontal;
            // Wunsch 2026-05-23: Beim Umschalten erst Promtboard + Eingabe
            // schliessen, damit sich NUR das Overlay dreht. Der Benutzer
            // oeffnet danach ueber den Stern frisch in der neuen Orientierung.
            CloseAttachedPanelsForOrientationSwitch();
            BeamToOrientation(target);
            PersistOrientation(target);
        }

        /// <summary>
        /// Orientierungswechsel mit symmetrischer Slide+Beam-Animation
        /// (Frank-Wunsch 2026-05-25: "Es soll genauso schoen hochrutschen wie es
        /// runterrutscht"). Beide Richtungen sind das exakte Spiegelbild:
        ///  • RUNTER (→ Leiste): Saeule beamt weg, Leiste beamt oben (an der
        ///    Saeulen-Oberkante) ein und RUTSCHT glatt nach unten an ihren Platz.
        ///  • HOCH (→ Saeule): die Leiste RUTSCHT erst glatt nach oben bis auf die
        ///    Saeulen-Oberkante, beamt sich dort weg und die Saeule beamt an ihrer
        ///    normalen (oder per Diskette gemerkten) Position wieder ein.
        /// Das Rutschen passiert IMMER in der flachen Leisten-Form (glatter
        /// Fenster-Move + DwmFlush) — die hohe Saeule wird nie bewegt (deren
        /// Content-Glide ruckelt). Der Formwechsel selbst liegt bei Opacity 0.
        /// </summary>
        private void BeamToOrientation(bool horizontal)
        {
            _orientationTransitioning = true;
            // Sicherheitsnetz: ohne bekannte Monitor-Geometrie klassisch
            // umschalten (kein Beam, aber voll funktionsfaehig).
            if (_waW <= 0)
            {
                ApplyOrientation(horizontal);
                PositionForCurrentOrientation(orientationSwitch: true);
                FinishOrientationTransition();
                return;
            }

            StopGlide();
            if (horizontal) BeamDownToHorizontal();
            else            SlideUpThenBeamToVertical();
        }

        // Obere Linie der Saeule: gemerkte Position (Diskette) oder kanonische
        // Oberkante. Diese Hoehe ist der gemeinsame Bezugspunkt fuer das
        // Wegbeamen/Einblenden — "in der gleichen Hoehe wie die oberste Kante
        // vom Gespeicherten" (Frank 2026-05-25).
        private double ColumnTop => _savedVerticalPos?.Y ?? (_waY + VerticalTopOffset);
        private double ColumnLeft => _savedVerticalPos?.X ?? (_waX + _waW - 96 - 27);

        // RUNTER: Saeule wegbeamen → Leiste an der Saeulen-Oberkante einblenden →
        // glatt nach unten an ihren Platz rutschen.
        private void BeamDownToHorizontal()
        {
            BeamFadeOut(FullView, () =>
            {
                ApplyOrientationSilent(true);
                // Horizontale Endposition (gemerkt oder kanonisch unten rechts).
                SizeToContent = SizeToContent.WidthAndHeight;
                UpdateLayout();
                double finalLeft, finalTop;
                if (_savedHorizontalPos is { } sh)
                {
                    finalLeft = sh.X; finalTop = sh.Y; _manuallyPositioned = true;
                }
                else
                {
                    finalLeft = _waX + _waW - ActualWidth - 27;
                    finalTop  = _waY + _waH - ActualHeight - HBarBottomLift;
                    _manuallyPositioned = false;
                }
                // An der Saeulen-Oberkante einblenden (gleiche Hoehe, in der die
                // Saeule gerade weggebeamt wurde), dann nach unten rutschen.
                Left = finalLeft;
                Top  = ColumnTop;
                BeamFadeIn(HorizontalView, () =>
                {
                    AnimateWindowTo(finalLeft, finalTop, FinishOrientationTransition);
                });
            });
        }

        // HOCH: Leiste glatt nach oben rutschen (flache Form, Fenster-Move) bis auf
        // die Saeulen-Oberkante → dort wegbeamen → Saeule an ihrer normalen/
        // gemerkten Position wieder einblenden. Spiegelbild des Runterrutschens.
        private void SlideUpThenBeamToVertical()
        {
            // 1) Flache Leiste senkrecht nach oben gleiten lassen bis ihre
            //    Oberkante auf der Saeulen-Oberkante liegt (X bleibt — reiner
            //    senkrechter Slide wie beim Runterrutschen).
            AnimateWindowTo(Left, ColumnTop, onComplete: () =>
            {
                // 2) Oben angekommen: Leiste wegbeamen.
                BeamFadeOut(HorizontalView, () =>
                {
                    // 3) Unsichtbar zur Saeule morphen und an die normale/gemerkte
                    //    Position setzen.
                    ApplyOrientationSilent(false);
                    SizeToContent = SizeToContent.Manual;
                    _isCollapsed  = false;
                    Width  = 96;
                    Height = FullHeight;
                    Left = ColumnLeft;
                    Top  = ColumnTop;
                    _manuallyPositioned = _savedVerticalPos is not null;
                    // 4) Saeule dort langsam + weich wieder einblenden.
                    BeamFadeIn(FullView, () =>
                    {
                        ReassertTopmostIfVisible();
                        FinishOrientationTransition();
                    });
                });
            });
        }

        private void FinishOrientationTransition()
        {
            _orientationTransitioning = false;
            if (!_mouseOverOverlay) ScheduleCollapse();
        }

        /// <summary>
        /// Wie <see cref="ApplyOrientation"/>, aber OHNE eigenes Einblenden: die
        /// Zielansicht wird auf Opacity 0 gesetzt, damit der Beam-Effekt das
        /// Einblenden selbst steuern kann. Spiegelt sonst exakt die Form-Logik
        /// von ApplyOrientation (Layout aufbauen/wiederherstellen, Sichtbarkeit).
        /// </summary>
        private void ApplyOrientationSilent(bool horizontal)
        {
            CaptureVerticalPlacement();
            if (horizontal)
            {
                BuildHorizontalLayout();
                FullView.Visibility      = Visibility.Collapsed;
                CollapsedView.Visibility = Visibility.Collapsed;
                HorizontalView.BeginAnimation(UIElement.OpacityProperty, null);
                HorizontalView.Opacity    = 0;
                HorizontalView.Visibility = Visibility.Visible;
                _isHorizontal = true;
                _isCollapsed  = false;
            }
            else
            {
                RestoreVerticalLayout();
                HorizontalView.Visibility = Visibility.Collapsed;
                FullView.BeginAnimation(UIElement.OpacityProperty, null);
                FullView.Opacity    = 0;
                FullView.Visibility = Visibility.Visible;
                FullView.UpdateLayout();
                _isHorizontal = false;
            }
            UpdateSaveIndicator();
            ReassertTopmostIfVisible();
        }

        /// <summary>
        /// Schliesst beim Orientierungswechsel das Promtboard UND das
        /// Eingabe-/Historie-Fenster vollstaendig und setzt den Stern-Toggle
        /// auf "aus". So dreht sich beim Umschalten nur das nackte Overlay;
        /// der naechste Stern-Klick oeffnet frisch im Board-Normalmodus an der
        /// kanonischen Position der neuen Orientierung.
        /// HidePromptPanel() schliesst das Panel (dessen OnClosed nimmt das
        /// Eingabe- und Historie-Fenster mit) und setzt _inputSoloDock zurueck.
        /// Den Stern-/Toggle-Zustand setzen wir danach explizit auf "aus" —
        /// unabhaengig davon ob der Closed-Handler bereits gegriffen hat.
        /// </summary>
        private void CloseAttachedPanelsForOrientationSwitch()
        {
            if (_promptPanel is not null)
            {
                HidePromptPanel();
            }
            alwaysOnActive = false;
            _inputSoloDock = false;
            UltrathinkButton.Background = ToggleOff;
            UltrathinkStar.Fill = StarMuted;
        }

        private async void PersistOrientation(bool horizontal)
        {
            try
            {
                using var scope = PromptBoardHost.Services.CreateScope();
                var repo = scope.ServiceProvider
                    .GetRequiredService<PromptBoard.Core.Repositories.IAppSettingsRepository>();
                var s = await repo.GetAsync();
                s.Orientation = horizontal ? "horizontal" : "vertical";
                await repo.UpdateAsync(s);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"PersistOrientation failed: {ex.Message}");
            }
        }

        /// <summary>
        /// Schreibt die aktuell gemerkten Diskette-Positionen (oder null beim
        /// Loeschen) in die DB. Die gespeicherten Werte werden beim naechsten
        /// Start automatisch geladen. Fire-and-forget; Fehler werden geloggt,
        /// nie der Klick blockiert.
        /// </summary>
        private async void PersistSavedPositionsToDb()
        {
            try
            {
                using var scope = PromptBoardHost.Services.CreateScope();
                var repo = scope.ServiceProvider
                    .GetRequiredService<PromptBoard.Core.Repositories.IAppSettingsRepository>();
                var s = await repo.GetAsync();
                s.OverlayVerticalLeft   = _savedVerticalPos?.X;
                s.OverlayVerticalTop    = _savedVerticalPos?.Y;
                s.OverlayHorizontalLeft = _savedHorizontalPos?.X;
                s.OverlayHorizontalTop  = _savedHorizontalPos?.Y;
                await repo.UpdateAsync(s);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"PersistSavedPositionsToDb failed: {ex.Message}");
            }
        }

        // Setzt Fenstergroesse + Position passend zur Orientierung. Horizontal:
        // SizeToContent (Leiste umschliesst die HBar), unten-rechts am Monitor.
        // Vertikal: feste Breite 96, Hoehe via Auto-Hide-Zustand.
        private void PositionForCurrentOrientation(bool orientationSwitch = false)
        {
            // Umschalten nimmt — falls in dieser Session eine Position fuer die
            // ZIELorientierung mit der Diskette gespeichert wurde — genau diese
            // Position wieder ein. Sonst gilt die kanonische Standardposition
            // (vertikal oben-rechts, horizontal unten-rechts). _manuallyPositioned
            // wird auf true gesetzt wenn eine gespeicherte Position angewendet
            // wurde, damit ein direkt folgendes OnAppActivated sie nicht
            // ueberschreibt.
            double? targetLeft = null, targetTop = null;
            if (_isHorizontal)
            {
                SizeToContent = SizeToContent.WidthAndHeight;
                UpdateLayout();
                if (_savedHorizontalPos is { } sh)
                {
                    targetLeft = sh.X; targetTop = sh.Y;
                    _manuallyPositioned = true;
                }
                else if (_waW > 0)
                {
                    targetLeft = _waX + _waW - ActualWidth - 27;
                    targetTop  = _waY + _waH - ActualHeight - HBarBottomLift;
                    _manuallyPositioned = false;
                }
                else { _manuallyPositioned = false; }
            }
            else
            {
                SizeToContent = SizeToContent.Manual;
                _isCollapsed = false;
                Width  = 96;
                Height = FullHeight;
                if (_savedVerticalPos is { } sv)
                {
                    targetLeft = sv.X; targetTop = sv.Y;
                    _manuallyPositioned = true;
                }
                else if (_waW > 0)
                {
                    targetLeft = _waX + _waW - 96 - 27;
                    targetTop  = _waY + VerticalTopOffset;
                    _manuallyPositioned = false;
                }
                else { _manuallyPositioned = false; }
            }

            if (targetLeft is double tl && targetTop is double tt)
            {
                if (orientationSwitch && _waW > 0)
                {
                    // Sauberer SENKRECHTER Slide auf der Ziel-Spalte: zuerst an
                    // die Ziel-X-Position und einen sinnvollen Start-Rand setzen,
                    // dann nur senkrecht zur Zielposition gleiten. Verhindert den
                    // diagonalen "erst nach links/unten unter den Bildschirm"-Effekt.
                    Left = tl;
                    double startTop;
                    if (_isHorizontal)
                    {
                        // Ziel = horizontale Leiste (unten): von oben herunter gleiten.
                        startTop = _waY;
                    }
                    else
                    {
                        // Ziel = vertikale Saeule (oben): von unten hochgleiten, aber
                        // so, dass die UNTERKANTE der Saeule auf der Linie der
                        // horizontalen Leiste startet — NICHT darunter (Frank
                        // 2026-05-25: die Saeule soll nicht unter die Tool-Linie
                        // tauchen). HBarBottomLift ist der Abstand der Leisten-
                        // Unterkante vom Arbeitsbereich-Boden. Auf sehr kleinen
                        // Screens (Start waere oberhalb des Ziels) ohne Glide direkt.
                        startTop = _waY + _waH - HBarBottomLift - Height;
                        if (startTop < tt) startTop = tt;
                    }
                    Top = startTop;
                    AnimateWindowTo(tl, tt);
                }
                else
                {
                    AnimateWindowTo(tl, tt); // sanftes Hingleiten (gleiche Form, beliebige Richtung)
                }
            }
            ReassertTopmostIfVisible();
        }

        // ── Glide: zwei Verfahren je nach Form (Hybrid, 2026-05-25) ──
        // FLACHE LEISTE (horizontal): kleines Fenster pro Frame verschieben +
        // DwmFlush → synchron mit der DWM-Komposition des Terminals, perfekt
        // fluessig und flackerfrei (kleine Flaeche).
        // HOHE SAEULE (vertikal): ein hohes Fenster pro Frame zu VERSCHIEBEN ist
        // teuer und ruckelt. Stattdessen Fenster gross+stehen lassen und nur den
        // INHALT per GPU-RenderTransform (+ BitmapCache) gleiten. Die Saeule ist
        // nur 96px schmal → kleine Layered-Flaeche → bleibt transparent UND
        // fluessig. (Genau das war beim BREITEN Horizontal-Fenster schlecht,
        // beim schmalen Vertikal-Fenster ist es ideal.) Memory: wpf-overlay-smooth-animation.
        private bool _gliding;                  // Fenster-Move (horizontal) aktiv
        private double _glideCurLeft, _glideCurTop;
        private FrameworkElement? _glideView;   // Content-Glide (vertikal) aktiv
        private HorizontalAlignment _glideOrigH;
        private VerticalAlignment _glideOrigV;
        private double _glideTargetLeft, _glideTargetTop;

        // onComplete (optional) feuert NUR bei natuerlichem Glide-Ende — nicht bei
        // Abbruch via StopGlide (z.B. Drag-Start). Genutzt fuer "gleiten, dann
        // beamen" beim Hochrutschen.
        private void AnimateWindowTo(double targetLeft, double targetTop, Action? onComplete = null)
        {
            try
            {
                StopGlide();
                double startLeft = Left, startTop = Top;
                if (_isCollapsed ||
                    (Math.Abs(targetLeft - startLeft) < 1.0 && Math.Abs(targetTop - startTop) < 1.0))
                {
                    Left = targetLeft; Top = targetTop;
                    onComplete?.Invoke();
                    return;
                }
                if (_isHorizontal) GlideByWindowMove(startLeft, startTop, targetLeft, targetTop, onComplete);
                else               GlideByContentTransform(startLeft, startTop, targetLeft, targetTop, onComplete);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AnimateWindowTo: {ex.Message}");
                StopGlide();
                Left = targetLeft; Top = targetTop; // Fallback: hart setzen
                onComplete?.Invoke();
            }
        }

        // Flache horizontale Leiste: kleines Fenster pro Frame verschieben + DwmFlush.
        private void GlideByWindowMove(double startLeft, double startTop, double targetLeft, double targetTop, Action? onComplete = null)
        {
            var src = PresentationSource.FromVisual(this);
            double dpiX = src?.CompositionTarget?.TransformToDevice.M11 ?? 1.0;
            double dpiY = src?.CompositionTarget?.TransformToDevice.M22 ?? 1.0;
            var hwnd = new WindowInteropHelper(this).Handle;
            double dist = Math.Max(Math.Abs(targetLeft - startLeft), Math.Abs(targetTop - startTop));
            double durationMs = Math.Clamp(dist * 1.3, 340.0, 600.0);
            var sw = System.Diagnostics.Stopwatch.StartNew();
            _gliding = true;
            _glideCurLeft = startLeft; _glideCurTop = startTop;

            void Step()
            {
                if (!_gliding) return;
                double t = sw.Elapsed.TotalMilliseconds / durationMs;
                if (t > 1.0) t = 1.0;
                double e = t * t * t * (t * (t * 6 - 15) + 10); // Smootherstep
                _glideCurLeft = startLeft + (targetLeft - startLeft) * e;
                _glideCurTop  = startTop  + (targetTop  - startTop)  * e;
                if (hwnd != IntPtr.Zero)
                {
                    Win32.SetWindowPos(hwnd, IntPtr.Zero,
                        (int)Math.Round(_glideCurLeft * dpiX), (int)Math.Round(_glideCurTop * dpiY),
                        0, 0, Win32.SWP_NOSIZE | Win32.SWP_NOACTIVATE | Win32.SWP_NOZORDER);
                    try { Win32.DwmFlush(); } catch { }
                }
                else { Left = _glideCurLeft; Top = _glideCurTop; }

                if (t >= 1.0)
                {
                    _gliding = false;
                    Left = targetLeft; Top = targetTop;
                    ReassertTopmostIfVisible();
                    onComplete?.Invoke();
                }
                else
                {
                    Dispatcher.BeginInvoke(new Action(Step), System.Windows.Threading.DispatcherPriority.Render);
                }
            }
            Step();
        }

        // Hohe vertikale Saeule: Fenster gross+stehend, nur den Inhalt per GPU-Transform gleiten.
        private void GlideByContentTransform(double startLeft, double startTop, double targetLeft, double targetTop, Action? onComplete = null)
        {
            const double pillW = 96.0;
            double pillH = FullHeight;
            var view = FullView;

            double ux = Math.Min(startLeft, targetLeft);
            double uy = Math.Min(startTop, targetTop);
            double uw = Math.Max(startLeft, targetLeft) + pillW - ux;
            double uh = Math.Max(startTop, targetTop) + pillH - uy;

            _glideView = view;
            _glideOrigH = view.HorizontalAlignment;
            _glideOrigV = view.VerticalAlignment;
            _glideTargetLeft = targetLeft; _glideTargetTop = targetTop;

            view.HorizontalAlignment = HorizontalAlignment.Left;
            view.VerticalAlignment   = VerticalAlignment.Top;
            var tt = new TranslateTransform(startLeft - ux, startTop - uy);
            view.RenderTransform = tt;
            view.CacheMode = new BitmapCache(); // GPU-Textur → nur blitten, nicht neu rastern

            SizeToContent = SizeToContent.Manual;
            Left = ux; Top = uy; Width = uw; Height = uh;

            // Sicher sichtbar (ApplyOrientation kann Opacity gegen den Form-Wechsel-Flash auf 0 gesetzt haben).
            view.BeginAnimation(UIElement.OpacityProperty, null);
            view.Opacity = 1;

            double dist = Math.Max(Math.Abs(targetLeft - startLeft), Math.Abs(targetTop - startTop));
            // Hoehere Untergrenze (500ms) als beim horizontalen Move (340ms): die
            // vertikale Strecke ist kurz, soll aber NICHT schneller wirken als das
            // Runtergleiten (~600ms). So haben Hoch- und Runtergleiten ein aehnliches Tempo.
            var dur = new Duration(TimeSpan.FromMilliseconds(Math.Clamp(dist * 1.3, 500.0, 600.0)));
            var ease = new CubicEase { EasingMode = EasingMode.EaseInOut };
            var ax = new DoubleAnimation(targetLeft - ux, dur) { EasingFunction = ease };
            var ay = new DoubleAnimation(targetTop  - uy, dur) { EasingFunction = ease };
            // KEINE erzwungene DesiredFrameRate: ein fester Wert, der kein sauberer
            // Teiler der Monitor-Bildrate ist (z.B. 50 bei 60/120Hz), kaempft gegen
            // V-Sync → ungleichmaessige Frames = Ruckeln. Der WPF-Default ist
            // vsync-gebunden und laeuft am gleichmaessigsten (Frank 2026-05-25).
            ay.Completed += (_, _) => { FinalizeContentGlide(); onComplete?.Invoke(); };
            tt.BeginAnimation(TranslateTransform.XProperty, ax);
            tt.BeginAnimation(TranslateTransform.YProperty, ay);
        }

        // Schliesst den vertikalen Content-Glide ab: Transform/Cache weg, Ausrichtung
        // zurueck, Fenster auf Pillen-Groesse (96xFullHeight) an die Zielposition.
        private void FinalizeContentGlide()
        {
            var v = _glideView;
            if (v == null) return;
            _glideView = null;
            try
            {
                if (v.RenderTransform is TranslateTransform t)
                {
                    t.BeginAnimation(TranslateTransform.XProperty, null);
                    t.BeginAnimation(TranslateTransform.YProperty, null);
                }
                v.RenderTransform = null;
                v.CacheMode = null;
                v.HorizontalAlignment = _glideOrigH;
                v.VerticalAlignment   = _glideOrigV;
                v.Opacity = 1;
                SizeToContent = SizeToContent.Manual;
                Width = 96; Height = FullHeight;
                Left = _glideTargetLeft; Top = _glideTargetTop;
            }
            catch (Exception ex) { Console.WriteLine($"FinalizeContentGlide: {ex.Message}"); }
            ReassertTopmostIfVisible();
        }

        /// <summary>Bricht einen laufenden Glide ab (beide Verfahren), z.B. bei Drag-Start.</summary>
        private void StopGlide()
        {
            if (_gliding)
            {
                _gliding = false;
                // Left/Top waren waehrend des Fenster-Moves stale → auf zuletzt gesetzte Position.
                Left = _glideCurLeft; Top = _glideCurTop;
            }
            if (_glideView != null) FinalizeContentGlide();
        }

        // Farben des Disketten-Symbols: dezentes Gruen wenn fuer die aktuelle
        // Ausrichtung eine Position gemerkt ist, sonst Weiss. Hell-Gruen nur als
        // kurzes Aufblitz-Feedback beim Speichern.
        private static readonly Color SaveIdleColor   = Colors.White;
        private static readonly Color SaveSavedColor  = Color.FromRgb(0x66, 0xBB, 0x6A); // Green 400 — "leicht gruen"
        private static readonly Color SaveFlashColor  = Color.FromRgb(0xB9, 0xF6, 0xCA); // Light Green A100 — Aufblitzen

        /// <summary>
        /// Klick auf die Diskette = Umschalter Speichern/Loeschen fuer die
        /// AKTUELLE Orientierung:
        ///  • Noch nichts gemerkt → aktuelle (ausgeklappte) Position merken,
        ///    Symbol blitzt auf und bleibt danach dezent gruen.
        ///  • Bereits etwas gemerkt → Position loeschen, Overlay geht zurueck
        ///    auf die kanonische Standardposition, gruen verschwindet.
        /// Die Diskette ist nur in der ausgeklappten Ansicht sichtbar, daher ist
        /// Left/Top hier immer die ausgeklappte Position — genau das, was die
        /// Positions-Logik (Collapse-Offset, Wiedereinblenden, Wechsel) erwartet.
        /// Die Position wird zugleich in der DB gespeichert und ueberlebt damit
        /// App- und PC-Neustarts.
        /// </summary>
        private void BtnSavePosition_Click(object sender, RoutedEventArgs e)
        {
            bool alreadySaved = (_isHorizontal ? _savedHorizontalPos : _savedVerticalPos) is not null;
            if (alreadySaved)
            {
                // Zweiter Druck: gemerkte Position der aktuellen Ausrichtung loeschen.
                if (_isHorizontal) _savedHorizontalPos = null;
                else               _savedVerticalPos   = null;
                _manuallyPositioned = false;
                PositionForCurrentOrientation(); // zurueck zur kanonischen Standardposition
                PersistSavedPositionsToDb();     // DB aktualisieren (null fuer diese Ausrichtung)
                FlashSaveCleared();              // gruen → weiss
            }
            else
            {
                var pos = new Point(Left, Top);
                if (_isHorizontal) _savedHorizontalPos = pos;
                else               _savedVerticalPos   = pos;
                _manuallyPositioned = true;
                PersistSavedPositionsToDb();     // DB aktualisieren; gilt beim naechsten Start
                FlashSaveStored();               // Aufblitzen → bleibt dezent gruen
            }
            _usedSinceExpand = true; // zaehlt als Interaktion → normaler Auto-Hide-Rhythmus
        }

        /// <summary>
        /// Setzt die Steady-Farbe des Disketten-Symbols passend zum Zustand der
        /// AKTUELLEN Ausrichtung: dezent gruen wenn eine Position gemerkt ist,
        /// sonst weiss. Wird bei jedem Orientierungswechsel und beim Start
        /// aufgerufen, damit das Symbol immer den Stand der gerade sichtbaren
        /// Ausrichtung zeigt. Ein frischer SolidColorBrush ersetzt einen evtl.
        /// noch laufenden Aufblitz-Brush (dessen Animation wirkt dann nicht mehr).
        /// </summary>
        private void UpdateSaveIndicator()
        {
            try
            {
                bool saved = (_isHorizontal ? _savedHorizontalPos : _savedVerticalPos) is not null;
                SaveIcon.Foreground = new SolidColorBrush(saved ? SaveSavedColor : SaveIdleColor);
            }
            catch (Exception ex) { Console.WriteLine($"UpdateSaveIndicator: {ex.Message}"); }
        }

        /// <summary>Speichern-Feedback: kurzes Aufblitzen, danach bleibt das
        /// Symbol dezent gruen (HoldEnd haelt die End-Farbe).</summary>
        private void FlashSaveStored()
        {
            try
            {
                var brush = new SolidColorBrush(SaveFlashColor);
                SaveIcon.Foreground = brush;
                brush.BeginAnimation(SolidColorBrush.ColorProperty, new ColorAnimation(
                    SaveFlashColor, SaveSavedColor, new Duration(TimeSpan.FromMilliseconds(650)))
                {
                    EasingFunction = HoverEaseOut,
                });
            }
            catch (Exception ex) { Console.WriteLine($"FlashSaveStored: {ex.Message}"); }
        }

        /// <summary>Loeschen-Feedback: sanftes Ausblenden des Gruens zurueck
        /// auf Weiss.</summary>
        private void FlashSaveCleared()
        {
            try
            {
                var brush = new SolidColorBrush(SaveSavedColor);
                SaveIcon.Foreground = brush;
                brush.BeginAnimation(SolidColorBrush.ColorProperty, new ColorAnimation(
                    SaveSavedColor, SaveIdleColor, new Duration(TimeSpan.FromMilliseconds(400)))
                {
                    EasingFunction = HoverEaseOut,
                });
            }
            catch (Exception ex) { Console.WriteLine($"FlashSaveCleared: {ex.Message}"); }
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

            // ── AutoEnter-Status-HTTP-Server starten ───────────────────────
            // Macht den aktuellen orange/grau-Zustand fuer externe Hardware
            // (Stream Deck Plugin) abfragbar UND erlaubt Toggle-Requests.
            // Localhost only — keine externe Erreichbarkeit, keine Firewall-
            // Eintraege noetig.
            // WICHTIG: Dispatcher.Invoke (synchron) statt BeginInvoke (async)!
            //
            // Vorgeschichte 2026-05-15:
            // Mit BeginInvoke war der Ablauf race-condition-anfaellig:
            //   1) HTTP-Thread ruft BeginInvoke → kehrt sofort zurueck
            //   2) HTTP-Server schlaeft 30ms, ruft dann _getCurrentState()
            //   3) Wenn UI-Dispatcher unter Last (Audio-Buffer, Polling),
            //      ist der Toggle nach 30ms noch nicht durch → ALTER State
            //      wird gemeldet → Plugin denkt "Toggle wirkt nicht" →
            //      drueckt nochmal → State driftet auseinander → UI-Queue
            //      staut sich mit halben Toggles → "alles eingefroren".
            //
            // Mit Dispatcher.Invoke (sync):
            //   1) HTTP-Thread blockt bis der Toggle im UI-Thread fertig ist
            //   2) Danach liest _getCurrentState() garantiert den NEUEN Wert
            //   3) Kein Sleep noetig (im AutoEnterStatusServer entfernt)
            //   4) Kein Cross-Thread-Stau im Dispatcher
            //
            // Kein Deadlock-Risiko: der UI-Thread wartet nirgends auf den
            // HTTP-Listener-Thread. Worst-case-Latenz fuer den HTTP-Client:
            // ~5-50ms bis die UI das Toggle durchgefuehrt hat — fuer das
            // Stream-Deck-Plugin praktisch instantan.
            _autoEnterServer = new Services.AutoEnterStatusServer(
                // Beschaeftigt-Status fuer den Datenverlust-Schutz beim Rebuild:
                // true solange aufgenommen/transkribiert wird (rebuild-overlay.ps1 wartet darauf).
                getBusyState: () => _micState == RecordingState.Recording || _isProcessing || isBtwRecording,
                getCurrentState: () => autoEnterEnabled,
                toggle: () =>
                {
                    try
                    {
                        Dispatcher.Invoke(new Action(() =>
                        {
                            try { ToggleAutoEnterFromHotkey(); }
                            catch (Exception innerEx)
                            {
                                LogAutoEnterToggleError("INNER", innerEx);
                            }
                        }));
                    }
                    catch (Exception outerEx)
                    {
                        // Dispatcher.Invoke selbst kann werfen wenn das
                        // Fenster gerade geschlossen wird. Wir loggen das
                        // sichtbar in die TVO-hotkey.log, damit kuenftige
                        // Probleme nicht mehr still verschluckt werden.
                        LogAutoEnterToggleError("OUTER", outerEx);
                    }
                });
            _autoEnterServer.Start();
        }

        private static void LogAutoEnterToggleError(string scope, Exception ex)
        {
            string msg = $"AutoEnter HTTP toggle failed ({scope}): {ex.GetType().Name}: {ex.Message}";
            Console.WriteLine(msg);
            try
            {
                string path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "TVO-hotkey.log");
                System.IO.File.AppendAllText(path,
                    $"{DateTime.Now:HH:mm:ss.fff} TOGGLE-ERROR {msg}{Environment.NewLine}");
            }
            catch { /* never block hotkey path */ }
        }

        private Services.AutoEnterStatusServer? _autoEnterServer;

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
                        StopGlide(); // laufenden Glide abbrechen, damit das Drag sofort uebernimmt
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

        private void OnAppActivated(IntPtr appHwnd)
        {
            // Wechsel zurueck zum Terminal: laufenden Hide-Delay-Timer
            // sofort abbrechen, sonst wuerde das Overlay nach Ablauf der
            // 5 Sekunden trotzdem noch verschwinden — auch wenn der Benutzer
            // laengst zurueck im Terminal arbeitet.
            _hideDelayTimer.Stop();

            bool wasHidden = !IsVisible;

            // Monitor-Arbeitsflaeche immer merken (auch fuer den In-Overlay-
            // Umschalter, der ohne frischen Fokuswechsel positionieren muss).
            var workArea = AppWatcher.GetMonitorWorkArea(appHwnd);
            _waX = workArea.X; _waY = workArea.Y; _waW = workArea.Width; _waH = workArea.Height;

            if (_isHorizontal)
            {
                bool freshShow = wasHidden && _autoHideEnabled
                    && _micState != RecordingState.Recording && !_isProcessing && !isBtwRecording;

                if (wasHidden)
                {
                    // KRITISCH (Bug 2026-05-25, horizontal): Beim echten Wieder-
                    // einblenden IMMER mit der VOLLEN Leiste messen + positionieren,
                    // auch wenn vor dem Verstecken eingeklappt. Sonst nutzt die
                    // Rechts-Formel die 96px-Pillenbreite statt der Leistenbreite
                    // und die eingeklappte Mic-Pille landet weit links (≈ Enter)
                    // statt rechts. Erst volle Leiste platzieren, dann einklappen.
                    if (_isCollapsed)
                    {
                        CollapsedView.Visibility  = Visibility.Collapsed;
                        HorizontalView.Visibility = Visibility.Visible;
                        _isCollapsed = false;
                    }
                    SizeToContent = SizeToContent.WidthAndHeight;
                    UpdateLayout();
                    if (!_manuallyPositioned)
                    {
                        if (_savedHorizontalPos is { } sh) { Left = sh.X; Top = sh.Y; }
                        else
                        {
                            Left = _waX + _waW - ActualWidth  - 27;
                            Top  = _waY + _waH - ActualHeight - HBarBottomLift;
                        }
                    }
                    Show();
                    Console.WriteLine("Overlay: visible (terminal active, horizontal)");

                    // Aus der korrekt positionierten Leiste einklappen → die
                    // Mic-Pille landet genau dort wo der Mic in der Leiste sitzt.
                    if (freshShow)
                    {
                        UpdateLayout();
                        CollapseImmediate();
                    }
                }
                else if (!_manuallyPositioned && !_isCollapsed)
                {
                    // War sichtbar + ausgeklappt: nur die Leisten-Position auffrischen.
                    SizeToContent = SizeToContent.WidthAndHeight;
                    UpdateLayout();
                    if (_savedHorizontalPos is { } sh) { Left = sh.X; Top = sh.Y; }
                    else
                    {
                        Left = _waX + _waW - ActualWidth  - 27;
                        Top  = _waY + _waH - ActualHeight - HBarBottomLift;
                    }
                }
                // War sichtbar + eingeklappt: nichts anfassen (kein Expand→Collapse-Flackern).
            }
            else
            {
                if (!_manuallyPositioned)
                {
                    // Frank's exact spec (2026-04-26): 27px vom rechten Rand,
                    // 57px vom oberen Rand. WPF-DIPs sind DPI-unabhaengig.
                    // Gibt es eine in dieser Session gespeicherte Diskette-
                    // Position, gilt diese statt der kanonischen — gespeichert
                    // wird die AUSGEKLAPPTE Position, daher gleiche Behandlung
                    // wie fullTop (Collapse-Offset wird oben drauf gerechnet).
                    bool hasSaved = _savedVerticalPos is not null;
                    Left = hasSaved ? _savedVerticalPos!.Value.X : _waX + _waW - Width - 27;
                    double fullTop = hasSaved ? _savedVerticalPos!.Value.Y : _waY + VerticalTopOffset;
                    if (_autoHideEnabled && _isCollapsed)
                    {
                        Top    = fullTop + CollapseTopOffset;
                        Height = CollapsedHeight;
                    }
                    else
                    {
                        Top    = fullTop;
                        Height = FullHeight; // Geometrie an den Zustand koppeln
                    }
                }

                // Frisch eingeblendet (war versteckt) → eingeklappt starten.
                // VOR Show(), sonst blitzt das volle Overlay einen Frame lang auf.
                if (wasHidden && _autoHideEnabled
                    && _micState != RecordingState.Recording && !_isProcessing && !isBtwRecording)
                {
                    CollapseImmediate();
                }

                if (!IsVisible)
                {
                    Show();
                    Console.WriteLine("Overlay: visible (terminal active)");
                }
            }

            // Star toggle is on but the panel was hidden during a previous
            // terminal-deactivation? Bring it back alongside the pillar so
            // both windows behave as a single unit per user expectation.
            // ABER: Im Solo-Andock-Modus ist das Promtboard absichtlich
            // ausgeblendet. Dann nicht das Promtboard reaktivieren, sondern
            // das Eingabefenster an seiner KANONISCHEN Solo-Position
            // wiederherstellen (horizontal: oberhalb der Leiste in Board-Hoehe,
            // vertikal: links am Pillar). Bugfix 2026-05-23: vorher wurde hier
            // hart DockToOverlay() gerufen — dadurch sprang die Eingabe nach
            // jedem Absenden zurueck auf einen duennen Streifen in Pillar-Hoehe.
            if (_inputSoloDock && _promptPanel?.InputWindow is { } soloInput)
            {
                if (!soloInput.IsVisible) soloInput.Show();
                PositionSoloInput();
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

            // KRITISCH (Bugfix 2026-05-10): Auch wenn die Pille bereits sichtbar
            // war (z.B. waehrend einer laufenden Aufnahme), MUSS sie hier
            // aktiv zurueck nach ganz oben im Z-Order — sonst bleibt sie
            // hinter Desktop-Widgets, die ebenfalls Topmost sind.
            ReassertTopmostIfVisible();
        }

        /// <summary>
        /// Drueckt das Overlay (und sichtbare Promptboard-Children) per
        /// SetWindowPos(HWND_TOPMOST) zurueck auf die absolute Topmost-
        /// Position im Z-Order. WPF's Topmost=True allein reicht nicht: bei
        /// langen Aufnahmen mit haeufigem Fensterwechsel verliert die Pille
        /// sonst gegen andere Topmost-Fenster (Desktop-Widgets, Pop-ups).
        ///
        /// SWP_NOACTIVATE ist entscheidend: die Pille soll NICHT den
        /// Tastatur-Fokus klauen — sie schiebt sich nur visuell vor andere
        /// Fenster. SWP_NOMOVE/NOSIZE behalten Position und Groesse bei.
        /// </summary>
        private void ReassertTopmostIfVisible()
        {
            try
            {
                ForceTopmost(this);
                if (_promptPanel is { IsVisible: true } pp) ForceTopmost(pp);
                if (_promptPanel?.InputWindow is { IsVisible: true } iw)
                {
                    ForceTopmost(iw);
                    // NACH dem Fenster: einen offenen Slot-Tooltip wieder nach
                    // ganz oben holen, sonst legt sich das gerade reasserte
                    // Eingabefenster darueber (Frank-Bug 2026-06-11).
                    iw.ReassertOpenToolTipTopmost();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"ReassertTopmost: {ex.Message}");
            }
        }

        private static void ForceTopmost(Window w)
        {
            var hwnd = new System.Windows.Interop.WindowInteropHelper(w).Handle;
            if (hwnd == IntPtr.Zero) return;
            NativeMethods.Win32.SetWindowPos(
                hwnd,
                NativeMethods.Win32.HWND_TOPMOST,
                0, 0, 0, 0,
                NativeMethods.Win32.SWP_NOMOVE
                | NativeMethods.Win32.SWP_NOSIZE
                | NativeMethods.Win32.SWP_NOACTIVATE);
        }

        private void OnAppDeactivated()
        {
            if (_micState == RecordingState.Recording || _isProcessing || isBtwRecording)
                return;

            // Frank-Wunsch 2026-05-30: KEIN 5-Sekunden-Nachhang mehr in anderen
            // Programmen. Das Overlay verschwindet SOFORT, sobald der Fokus ein
            // echtes Fremdfenster (Browser, Editor, ...) erreicht. Es bleibt nur
            // sichtbar, solange der Benutzer mit unserer EIGENEN UI arbeitet:
            // Cursor ueber Pillar/Board/Eingabe/Historie ODER ein Hilfsdialog/
            // Kontextmenue offen (unsere eigenen Fenster sind ebenfalls "kein
            // Terminal" und loesen sonst faelschlich ein Verstecken aus). In dem
            // Fall dient der Timer nur noch als Fallback, falls der Benutzer die
            // UI gleich wieder verlaesst ohne zum Terminal zurueckzukehren.
            _hideDelayTimer.Stop();
            if (IsCursorOverOwnUi() || IsAuxiliaryWindowOpen())
            {
                _hideDelayTimer.Start();
                return;
            }
            HideOverlayNow();
        }

        /// <summary>
        /// Tatsaechliches Verstecken des Overlays nach Ablauf des Hide-Delay-
        /// Timers. Wird auch direkt aufgerufen wenn die App aufraeumt. Enthaelt
        /// die Logik die frueher in OnAppDeactivated stand: erst die
        /// floating Children ausblenden, dann das PromptBoard-Panel, dann das
        /// Pillar selbst. Ein zwischenzeitlicher Mic-Recording-Start macht
        /// hier KEINE Ausnahme mehr — die Pruefung steckt bereits oben in
        /// OnAppDeactivated, sodass der Timer gar nicht erst gestartet
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
        // Lock fuer Cancel/Dispose/Reassign-Race. Frueher konnte ein
        // schneller Doppel-Klick die Sequenz "Cancel(), Dispose(), null,
        // new()" zerschiessen wenn beide MouseDown-Handler im Mikrosekunden-
        // Abstand feuerten — Ergebnis war eine ObjectDisposedException auf
        // der Token-Property im Worst Case. Mit Lock laeuft jeder Klick
        // serialisiert durch den kritischen Abschnitt. Direktive-3-
        // Resilienz: Lock ist nur waehrend der CTS-Manipulation gehalten,
        // der Background-Loop laeuft danach lockfrei mit dem captured
        // Token weiter — kein Deadlock-Risiko.
        private readonly object _xRepeatLock = new();
        private bool _xResetScheduled;      // verhindert mehrfaches Faerbe-Reset

        private void XButton_PreviewMouseLeftButtonDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            // Visuelles Feedback: X faerbt sich grau (idle) waehrend der Aktion
            XButton.Background = BtnIdle;
            _xResetScheduled = false;

            // Mouse-Capture: garantiert dass MouseUp am Button ankommt, auch wenn der
            // Cursor waehrend des Druecks aus dem Button rausrutscht. Ohne Capture
            // kann der Loop schon nach einer Zeile abbrechen, wenn die Maus sich
            // minimal bewegt und WPF den Hover-State verliert.
            XButton.CaptureMouse();

            // Vorherige Schleife sauber abbrechen, alte CTS disposen,
            // neue erstellen — alles unter einem Lock damit zwei rasche
            // Klicks sich nicht in die Quere kommen.
            System.Threading.CancellationToken token;
            lock (_xRepeatLock)
            {
                if (_xRepeatCts is { } prev)
                {
                    try { prev.Cancel(); } catch { /* race-safe */ }
                    try { prev.Dispose(); } catch { /* race-safe */ }
                }
                _xRepeatCts = new System.Threading.CancellationTokenSource();
                token = _xRepeatCts.Token;
            }
            var hwnd = _appWatcher.ActiveAppHwnd;
            hasPastedText = false;

            // Background-Loop: sequentiell ClearLine + 10ms warten, solange Token aktiv ist
            _ = Task.Run(async () =>
            {
                try
                {
                    while (!token.IsCancellationRequested)
                    {
                        AppController.ClearLine(hwnd);
                        if (token.IsCancellationRequested) break;
                        await Task.Delay(10, token).ConfigureAwait(false);
                    }
                }
                catch (TaskCanceledException) { /* erwartet beim Loslassen */ }
                catch (OperationCanceledException) { /* erwartet beim Loslassen */ }
            });

            // Verhindert dass der Button selbst auf das Event reagiert (kein Doppel-Click)
            e.Handled = true;
        }

        private void XButton_PreviewMouseLeftButtonUp(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            if (XButton.IsMouseCaptured) XButton.ReleaseMouseCapture();
            StopXRepeat();
            e.Handled = true;
        }

        // MouseLeave-Handler bewusst NICHT vorhanden — solange die linke Maustaste
        // gedrueckt ist, soll der Loop weiterlaufen, auch wenn der Cursor den Button
        // verlaesst. Erst MouseUp stoppt die Schleife.

        private void StopXRepeat()
        {
            // Cancel + Dispose unter dem gleichen Lock wie der Start-Pfad,
            // damit ein zwischenzeitlicher Re-Start (zweiter MouseDown
            // bevor StopXRepeat fertig ist) nicht eine bereits disposte
            // CTS faengt. _xRepeatCts wird nach Cancel SOFORT genullt —
            // der Background-Loop arbeitet mit seinem captured Token
            // weiter, der von Cancel auf "cancellation requested" steht.
            lock (_xRepeatLock)
            {
                if (_xRepeatCts is { } cts)
                {
                    try { cts.Cancel(); } catch { /* race-safe */ }
                    try { cts.Dispose(); } catch { /* race-safe */ }
                }
                _xRepeatCts = null;
            }

            // Nach kurzer Verzoegerung visueller Reset auf Rot
            if (_xResetScheduled) return;
            _xResetScheduled = true;
            _ = Task.Run(async () =>
            {
                await Task.Delay(500);
                // Defensiv: das Window kann inzwischen geschlossen sein
                // (z.B. Tray-Beenden waehrend eines X-Klick-Restzustands).
                // Ohne Pruefung wirft Dispatcher.Invoke nach Close eine
                // TaskCanceledException → unhandled in unobservable Task.
                try
                {
                    if (Application.Current is { } app)
                    {
                        await app.Dispatcher.InvokeAsync(() =>
                        {
                            XButton.Background = BtnX;
                            _xResetScheduled = false;
                        });
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"X-reset dispatch failed (window closed?): {ex.Message}");
                }
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
                var targetHwnd = _mainRecordingTargetHwnd;
                // ── Stop recording ──
                var wavFile = await _audioRecorder.StopAsync();
                _recordingCuePlayer.PlayStop();
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
                    Console.WriteLine($"Transcript: {SafeLogPreview(transcript)}");
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
                            Console.WriteLine($"Corrected: {SafeLogPreview(finalText)}");
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

                        // Async-Variante: blockiert nicht mehr UI fuer ~500ms
                        // pro Voice-Submit (Win32 Sleeps laufen auf Background-
                        // Thread).
                        if (!await AppController.PasteTextAsync(finalText, targetHwnd, autoEnterEnabled))
                            throw new InvalidOperationException("Das Ziel-Fenster der Aufnahme ist nicht mehr verfügbar.");
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
                    _mainRecordingTargetHwnd = IntPtr.Zero;
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
                    if (!_audioRecorder.Start()) return;
                    _mainRecordingTargetHwnd = _appWatcher.ActiveAppHwnd;
                    SetMicState(RecordingState.Recording);
                    _recordingCuePlayer.PlayStart();
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
                var targetHwnd = _btwRecordingTargetHwnd;
                // ── Stop BTW recording ──
                var wavFile = await _audioRecorder.StopAsync();
                _recordingCuePlayer.PlayStop();
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
                    Console.WriteLine($"BTW transcript: {SafeLogPreview(transcript)}");
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
                            Console.WriteLine($"BTW corrected: {SafeLogPreview(finalText)}");
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

                    if (!await AppController.PasteTextAsync(finalText, targetHwnd, autoEnterEnabled))
                        throw new InvalidOperationException("Das Ziel-Fenster der BTW-Aufnahme ist nicht mehr verfügbar.");
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
                    _btwRecordingTargetHwnd = IntPtr.Zero;

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
                    if (!_audioRecorder.Start()) return;
                    isBtwRecording = true;
                    _btwRecordingTargetHwnd = _appWatcher.ActiveAppHwnd;
                    SetBtwMicState(RecordingState.Recording);
                    _recordingCuePlayer.PlayStart();
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

        // Reentrancy-Guard fuer den W-Button. Vorher hielt Thread.Sleep den
        // UI-Thread fuer 100 ms an — Doppelklicks waren dadurch unmoeglich.
        // Nach dem Wechsel auf await Task.Delay koennte ein zweiter Klick
        // waehrend des Wartens reinkommen und einen zweiten Paste ausloesen.
        // Der Guard nullt lastRawTranscript SOFORT (statt am Ende), damit ein
        // zweiter Klick beim if-Check oben rausfaellt.
        private bool _whisperUndoBusy;

        /// <summary>W button — undo Gemini correction, paste raw Whisper text.</summary>
        private async void BtnWhisperUndo_Click(object sender, RoutedEventArgs e)
        {
            if (_whisperUndoBusy) return;
            if (lastRawTranscript == null) return;

            string textToPaste = lastRawTranscript;
            lastRawTranscript = null;
            _whisperUndoBusy = true;
            try
            {
                // Async-Pfad: ClearLine + PasteText laufen auf Background-Thread,
                // UI bleibt reaktiv waehrend der Win32-Sleeps. Reentrancy-Guard
                // _whisperUndoBusy verhindert Doppel-Klick.
                await AppController.ClearLineAsync(_appWatcher.ActiveAppHwnd);
                await Task.Delay(100);
                await AppController.PasteTextAsync(textToPaste, _appWatcher.ActiveAppHwnd);
                hasPastedText = true;
                Console.WriteLine($"Whisper raw text inserted: {SafeLogPreview(textToPaste)}");
            }
            finally
            {
                _whisperUndoBusy = false;
            }
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
        // Vorkompilierte Regex fuer das Profile-Button-Naming-Schema. Wird
        // bei jedem Rechtsklick auf das Pillar im Hit-Test-Pfad benutzt;
        // ohne Cache wuerde pro Klick eine frische Regex-Instanz inkl.
        // Pattern-Compile entstehen.
        private static readonly System.Text.RegularExpressions.Regex ProfileButtonNameRegex =
            new System.Text.RegularExpressions.Regex(
                @"^Profile(\d+)Button$",
                System.Text.RegularExpressions.RegexOptions.Compiled
                | System.Text.RegularExpressions.RegexOptions.CultureInvariant);

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
                    var match = ProfileButtonNameRegex.Match(btn.Name);
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
            Interlocked.Increment(ref _reCorrectGeneration);
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
            long generation = Interlocked.Increment(ref _reCorrectGeneration);
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
            var targetHwnd = _appWatcher.ActiveAppHwnd;
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
                if (Interlocked.Read(ref _reCorrectGeneration) != generation) return;

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
                await _reCorrectApplyGate.WaitAsync();
                try
                {
                    if (Interlocked.Read(ref _reCorrectGeneration) != generation) return;
                    await AppController.ClearAllInputAsync(targetHwnd);
                    await Task.Delay(120);
                    if (!await AppController.PasteTextAsync(corrected, targetHwnd, autoEnterEnabled)) return;
                }
                finally
                {
                    _reCorrectApplyGate.Release();
                }

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
        private async void BtnAutoEnter_Click(object sender, RoutedEventArgs e)
        {
            // Waehrend einer laufenden Aufnahme/Verarbeitung schaltet der
            // Enter-Button NUR den Auto-Enter-Toggle um: KEIN Sofort-Senden
            // des Prompt-Feld-Inhalts, KEIN Aufnahme-Stopp, KEIN Return ans
            // Ziel. So kann der Benutzer mitten beim Sprechen entscheiden,
            // ob der fertige Text gesendet (orange) oder nur ins Prompt-
            // Eingabefeld eingefuegt (dunkel) werden soll — ohne dass etwas
            // Sofortiges passiert. (Frank-Wunsch 2026-06-20: mitten beim
            // Sprechen den Schalter an-/ausmachen, ohne dass etwas passiert.)
            if (_micState == RecordingState.Recording || _isProcessing || isBtwRecording)
            {
                SetAutoEnter(!autoEnterEnabled, "UI-Click-WhileRecording");
                return;
            }

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
                SetAutoEnter(false, "UI-Click");
            }
            else
            {
                // Turn ON → fire Return immediately
                SetAutoEnter(true, "UI-Click");
                // Fire a Return key press into the active terminal — async,
                // damit der 200-ms-BringToForeground-Block nicht den UI-Thread
                // einfriert.
                await AppController.PressReturnAsync(_appWatcher.ActiveAppHwnd);
            }
        }

        /// <summary>
        /// Reine Toggle-Variante fuer das Stream-Deck-Plugin (HTTP-API).
        /// Anders als <see cref="BtnAutoEnter_Click"/>:
        ///  - KEIN sofortiger Return ans Terminal bei OFF→ON
        ///    (Frank's Stream-Deck-Wunsch: nur Status umschalten,
        ///    keine Leerzeile in der CLI),
        ///  - kein <c>_forceReturnOnNextSubmit</c>/<c>TrySubmitInputText</c>-
        ///    Spezialpfad (der Hotkey reagiert nie auf das Promtboard-
        ///    Eingabefeld, das macht der Maus-Klick weiterhin).
        /// Aktualisiert nur die Variable + den Button-Hintergrund.
        /// </summary>
        private void ToggleAutoEnterFromHotkey()
        {
            // Geht jetzt ueber SetAutoEnter, damit JEDE State-Aenderung
            // mit Quelle + vorher/nachher in TVO-hotkey.log landet —
            // dann ist Drift-Debugging in 30 Sekunden moeglich.
            SetAutoEnter(!autoEnterEnabled, "StreamDeckHTTP");
        }

        /// <summary>
        /// Einziger Schreibzugriff auf <see cref="autoEnterEnabled"/> ausser
        /// der Initialisierung in der Felddeklaration. Beide Pfade —
        /// UI-Klick (<see cref="BtnAutoEnter_Click"/>) und HTTP-Toggle
        /// vom Stream-Deck-Plugin — gehen hier durch.
        ///
        /// Loggt vorher+nachher+Quelle in TVO-hotkey.log. So sieht jeder
        /// Drift sofort in der Datei aus wer wann was umgeschaltet hat.
        ///
        /// MUSS am UI-Thread aufgerufen werden (Background-Setter auf
        /// <see cref="EnterButton"/> wuerden eine WPF-Cross-Thread-
        /// Exception werfen).
        /// </summary>
        private void SetAutoEnter(bool newValue, string source)
        {
            bool oldValue = autoEnterEnabled;
            autoEnterEnabled = newValue;
            EnterButton.Background = newValue ? BtnProcessing : ToggleOff;
            hasPastedText = false;
            Console.WriteLine($"Auto-enter {(newValue ? "ON" : "OFF")} (source={source})");
            try
            {
                string path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "TVO-hotkey.log");
                System.IO.File.AppendAllText(path,
                    $"{DateTime.Now:HH:mm:ss.fff} STATE-CHANGE source={source} from={oldValue} to={newValue}{Environment.NewLine}");
            }
            catch { /* never block UI for diagnostics */ }

            // PUSH den neuen State an alle SSE-Subscriber. Damit landet der
            // neue Stand sofort beim Stream-Deck-Plugin — egal ob Chrome im
            // Plugin-Webview gerade setInterval drosselt. EventSource-
            // onmessage ist nicht von Background-Tab-Throttling betroffen.
            try { _autoEnterServer?.NotifyStateChanged(newValue); }
            catch (Exception ex) { Console.WriteLine($"SSE push failed: {ex.Message}"); }
        }

        /// <summary>Star button — toggles the full PromptBoard integration:
        /// opens/closes the prompt UI AND enables/disables the always-on
        /// prefix. Default-Einstieg seit Aenderung 2026-06-03 (Frank-Wunsch):
        /// erster Klick zeigt ZUERST das Prompt-Eingabefenster (Solo-Modus —
        /// Board versteckt, Eingabe direkt am Pillar), NICHT mehr das Promtboard.
        /// Der Benutzer holt das Promtboard bei Bedarf ueber den Stern in der
        /// Eingabe-Toolbar dazu; der Stern in der Promtboard-Toolbar schaltet
        /// wieder zurueck in den Solo-Modus.
        /// Zweiter Klick auf diesen Voice-Overlay-Stern schliesst alles.</summary>
        private void BtnUltrathink_Click(object sender, RoutedEventArgs e)
        {
            alwaysOnActive = !alwaysOnActive;

            if (alwaysOnActive)
            {
                UltrathinkButton.Background = BtnUltrathinkOn;
                UltrathinkStar.Fill = StarGold;
                ShowPromptInputSolo();
            }
            else
            {
                UltrathinkButton.Background = ToggleOff;
                UltrathinkStar.Fill = StarMuted;
                HidePromptPanel();
            }

            Console.WriteLine($"Prompt input {(alwaysOnActive ? "OPEN (solo)" : "CLOSED")}");
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
            // Remember the board's normal vertical width deterministically at
            // creation time (XAML width, fixed because the board is NoResize).
            // The old lazy capture inside PositionPromptPanel could record a
            // width that had already been shrunk by a horizontal-mode excursion,
            // which then made the board come back NARROW after an auto-collapse/
            // expand cycle instead of vanishing/reappearing at full width
            // (Frank-Bug 2026-05-30: board got squeezed instead of disappearing).
            if (!double.IsNaN(_promptPanel.Width) && _promptPanel.Width > 1)
                _boardVerticalWidth = _promptPanel.Width;
            _promptPanel.PromptInsertRequested += OnPromptPanelInsert;
            _promptPanel.InputSubmitRequested  += OnInputSubmit;
            // Wird gefeuert nachdem der Benutzer einen Historie-Eintrag
            // im Editor-Dialog gespeichert hat — Cloud-Upload anstossen.
            _promptPanel.HistorySyncRequested  += () => _ = TryUploadHistoryAsync();
            // Prompt-Zwischenspeicher: nach Speichern/Loeschen sofort hochladen.
            _promptPanel.SlotsSyncRequested    += () => _ = TryUploadSlotsAsync();
            // Vokabular-Woerterbuch: nach Speichern im Settings-Dialog hochladen.
            _promptPanel.VocabularySyncRequested += () => _ = TryUploadVocabularyAsync();
            // Gemini-Prompt-Backup: nach erfolgreichem Upload den sichtbaren Sync-
            // Zeitstempel setzen (wie beim Vokabular). Event feuert vom Background-
            // Thread -> auf den UI-Thread marshallen. Singleton-Window, kein Unsubscribe noetig.
            if (!_geminiUploadSubscribed)
            {
                GeminiPromptDriveSync.UploadSucceeded += OnGeminiPromptUploadSucceeded;
                _geminiUploadSubscribed = true;
            }
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

            // Board sichtbar machen + positionieren (im Horizontal-Modus OBEN,
            // im Vertikal-Modus LINKS), damit die Eingabe links ANS BOARD
            // andocken kann statt ans Overlay.
            alwaysOnActive = true;
            UltrathinkStar.Fill = StarGold;
            PositionPromptPanel();
            if (!_promptPanel.IsVisible) _promptPanel.Show();

            _promptPanel.EnsureInputWindowOpen();
            var input = _promptPanel.InputWindow;
            if (input is null) return;

            // Eingabe LINKS ANS BOARD andocken: gleiche Hoehe wie das Board,
            // normale Eingabe-Breite, Board bleibt sichtbar (KEIN Solo-Andock
            // ans Overlay mehr — das war der duenne Streifen am Pillar).
            _inputSoloDock = false;
            input.DockTo(_promptPanel);
            input.SetSoloDockState(false);
        }

        /// <summary>
        /// Solo-Einstieg ueber den Voice-Overlay-Stern (Frank-Wunsch 2026-06-03):
        /// oeffnet ZUERST das Prompt-Eingabefenster und blendet das Promtboard
        /// dabei aus — die Eingabe dockt direkt an die linke Pillar-Kante. Das
        /// Promtboard wird im Hintergrund erstellt und mit frischen Daten
        /// geladen, damit der Benutzer es spaeter ueber den Stern in der
        /// Eingabe-Toolbar verzoegerungsfrei einblenden kann. Nutzt dieselbe
        /// ApplySoloDockMode-Mechanik wie der Board-Stern, nur als Einstieg.
        /// </summary>
        private void ShowPromptInputSolo()
        {
            EnsurePromptPanelInstance();
            if (_promptPanel is null) return;

            // Board-Daten laden (Kategorien/Prompts), auch wenn das Board nicht
            // sichtbar ist — sonst waere es beim spaeteren Einblenden leer.
            _ = _promptPanel.RefreshAsync();

            // Board-Position vorab festlegen, damit das Eingabefenster beim
            // ersten Andocken schon nahe der finalen Stelle sitzt (weniger Flash),
            // bevor ApplySoloDockMode es an den Pillar umdockt.
            PositionPromptPanel();

            // Eingabe oeffnen, Promtboard ausblenden, Eingabe an den Pillar
            // andocken — identische, bereits erprobte Solo-Andock-Sequenz wie
            // beim Stern in der Board-/Eingabe-Toolbar.
            ApplySoloDockMode(true);
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

            // Normale (vertikale) Board-Breite einmalig merken, bevor sie im
            // Horizontal-Modus auf die Leistenbreite gesetzt wird.
            if (_boardVerticalWidth <= 1 && !double.IsNaN(_promptPanel.Width) && _promptPanel.Width > 1)
                _boardVerticalWidth = _promptPanel.Width;

            if (_isHorizontal)
            {
                // Horizontal: Promptboard OBERHALB der Leiste, gleiche Breite wie
                // die Leiste. Das Eingabefenster dockt dann links ans Board (auch
                // oben), weil es dem Board via LocationChanged folgt.
                const double boardHeight = 740; // ~10 cm hoeher als zuvor (360) → mehr Eingabeplatz, waechst nach oben
                _promptPanel.Width  = ActualWidth;
                _promptPanel.Height = boardHeight;
                _promptPanel.Left   = Left;
                _promptPanel.Top    = Top - boardHeight - 4;
            }
            else
            {
                // Vertikal: links neben dem Pillar mit 4px Naht, gleiche Hoehe.
                // ALWAYS restore the full vertical width — never leave the board
                // at a narrow horizontal-mode width. Fallback to the XAML default
                // (532) if the remembered width is missing/invalid, so the board
                // can never come back "squeezed" after a collapse/expand cycle.
                _promptPanel.Width = _boardVerticalWidth > 1 ? _boardVerticalWidth : 532;
                _promptPanel.Height = Height;
                _promptPanel.Left = Left - _promptPanel.Width - 4;
                _promptPanel.Top = Top;
            }
        }

        // Positioniert das Eingabefenster im Solo-Modus DA, wo das Board war:
        // vertikal links neben dem Pillar (Pillar-Hoehe), horizontal OBERHALB der
        // Leiste (Board-Hoehe). Eigene (normale) Eingabe-Breite bleibt erhalten,
        // damit man normal hineintippen kann — kein duenner Streifen am Pillar.
        private void PositionSoloInput()
        {
            var input = _promptPanel?.InputWindow;
            if (input is null) return;
            if (_isHorizontal)
            {
                const double boardHeight = 740; // ~10 cm hoeher als zuvor (360) → mehr Eingabeplatz, waechst nach oben
                double iw = double.IsNaN(input.Width) || input.Width < 1 ? input.ActualWidth : input.Width;
                input.Height = boardHeight;
                input.Top    = Top - boardHeight - 4;      // oberhalb der Leiste, wo das Board war
                input.Left   = Left + ActualWidth - iw;    // RECHTE Kante buendig mit der Leiste
            }
            else
            {
                input.DockToOverlay(this); // vertikal: links vom Pillar = wo das Board war
            }
        }

        /// <summary>
        /// Setzt den Solo-Andock-Modus um (Stern-Klick im Eingabefenster).
        /// <list type="bullet">
        /// <item>active=true: Promtboard ausblenden und Eingabe direkt
        /// links an den Pillar andocken (mit 4-Pixel-Naht, Hoehe = Pillar).</item>
        /// <item>active=false: NUR das Promtboard einblenden und re-positionieren —
        /// das Eingabefenster wird AUSGEBLENDET (Frank-Wunsch 2026-06-06: der Stern
        /// togglet zwischen "nur Eingabe" und "nur Board", nie beide gleichzeitig).
        /// Der getippte Text bleibt erhalten und kommt beim naechsten Board-Stern
        /// mit dem Eingabefenster zurueck.</item>
        /// </list>
        /// Das Eingabefenster wird per <see cref="PromptInputWindow.SetSoloDockState"/>
        /// nachgezogen damit das Stern-Visual den neuen Zustand zeigt.
        /// </summary>
        private void ApplySoloDockMode(bool active)
        {
            if (_promptPanel is null) return;

            if (active)
            {
                // Wechsel Board → Solo: das Eingabefenster muss EXISTIEREN
                // bevor wir es positionieren koennen. Kann beim Klick auf den
                // neuen Board-Stern noch null sein, weil der Board-Default
                // Eingabefenster zugemacht laesst. EnsureInputWindowOpen
                // baut Subscriptions auf und zeigt das Fenster — wir docken
                // es im selben Frame zum Pillar um, sodass der Flash am Board-
                // Rand visuell verschwindet.
                _promptPanel.EnsureInputWindowOpen();
                var input = _promptPanel.InputWindow;
                if (input is null) return;

                // Promtboard ausblenden — wir nutzen Window.Hide, NICHT Close,
                // damit die Instanz und der ganze State (Kategorien, Prompts,
                // Subscriptions) erhalten bleiben.
                _promptPanel.Hide();
                _inputSoloDock = true;
                PositionSoloInput();
                input.SetSoloDockState(true);
            }
            else
            {
                // Wechsel Solo-Eingabe → nur Board: Promtboard einblenden und an
                // seiner normalen Stelle positionieren (vertikal links vom Pillar,
                // horizontal oberhalb der Leiste — beide Faelle in
                // PositionPromptPanel). Das Eingabefenster wird AUSGEBLENDET, damit
                // nur das Board sichtbar ist (Frank-Wunsch 2026-06-06). HideInputWindow
                // sichert den noch nicht abgeschickten Text; der naechste Board-Stern
                // (ApplySoloDockMode(true) → EnsureInputWindowOpen) holt die Eingabe
                // damit zurueck. Board ZUERST zeigen, dann Eingabe verstecken, damit
                // kein Frame entsteht in dem gar nichts sichtbar ist.
                _inputSoloDock = false;
                _promptPanel.Show();
                PositionPromptPanel();
                _promptPanel.HideInputWindow();
            }
        }

        private async void OnPromptPanelInsert(string text)
        {
            if (string.IsNullOrEmpty(text)) return;
            try
            {
                // Async-Variante: blockiert UI nicht waehrend Win32-Foreground-
                // Sleeps. Alle Folgeschritte (Console-Log) sind nach await
                // garantiert sequentiell.
                await AppController.PasteTextAsync(text, _appWatcher.ActiveAppHwnd, autoEnterEnabled);
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
        private async Task<bool> OnInputSubmit(string middleText)
        {
            try
            {
                var targetHwnd = _appWatcher.ActiveAppHwnd;
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
                    return false;
                }

                string final = string.Join(" ; ", parts);
                // Force-Return uebersteuert das autoEnter-Toggle wenn der
                // Submit aus einem expliziten Enter-Button-Klick kommt.
                bool effectiveAutoEnter = autoEnterEnabled || _forceReturnOnNextSubmit;
                if (!await AppController.PasteTextAsync(final, targetHwnd, effectiveAutoEnter))
                    return false;
                _forceReturnOnNextSubmit = false;
                Console.WriteLine($"Input submit: {final.Length} chars (autoEnter={effectiveAutoEnter}).");
                hasPastedText = !effectiveAutoEnter;

                // Historie-Eintrag asynchron schreiben — Submit darf NICHT
                // auf Gemini warten, weil sonst der Tipp-Flow ruckelt. Der
                // Eintrag bekommt vorerst einen Fallback-Titel (erste 4
                // Woerter), Gemini ueberschreibt ihn sobald der KI-Titel da
                // ist. So sieht der Benutzer den Eintrag SOFORT in der
                // Historie und der KI-Titel erscheint nachtraeglich.
                _ = WriteHistoryAsync(mid);
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"OnInputSubmit failed: {ex.Message}");
                return false;
            }
        }

        private void OnGeminiPromptUploadSucceeded() =>
            Dispatcher.BeginInvoke(new Action(() => _promptPanel?.MarkSyncedNow()));

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
                        _ = TryUploadHistoryAsync();
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
        // Pro Session nur EINE Tray-Notification fuer Drive-Sync-Fehler —
        // sonst wuerde Frank pro Voice-Submit eine neue Balloon sehen,
        // was massiv stoert. Der Counter wird beim ersten erfolgreichen
        // Sync zurueckgesetzt damit nach einem manuellen Re-Connect die
        // naechste Token-Expiry wieder einmal angezeigt werden kann.
        private static bool _driveSyncWarningShown;

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
                // Erfolgreich → Warnung zuruecksetzen damit ein zukuenftiger
                // erneuter Token-Verlust wieder gemeldet wird.
                _driveSyncWarningShown = false;
                // Den sichtbaren Sync-Timestamp im Promtboard-Header
                // aktualisieren — der Label zeigt damit auch
                // Historie-Sync-Aktivitaet, nicht nur Promtboard-Backup.
                _promptPanel?.MarkSyncedNow();
            }
            catch (Exception ex)
            {
                LogHistorySync($"FAIL: {ex.GetType().Name}: {ex.Message}");

                // Frueher: stille Schluck-Aktion — Frank merkte tagelang
                // nicht dass die Cloud-Historie veraltete (siehe Vorfall
                // 2026-05-08, "TokenResponseException: invalid_grant").
                // Jetzt: bei klassischen Auth-Fehlern eine einmalige Tray-
                // Balloon mit konkreter Handlungsaufforderung. Nur einmal
                // pro Session, sonst poppt es bei jedem Voice-Submit auf.
                if (!_driveSyncWarningShown && IsDriveAuthFailure(ex))
                {
                    _driveSyncWarningShown = true;
                    try
                    {
                        App.ShowTrayBalloon(
                            "Drive-Sync nicht moeglich",
                            "Der Google-Drive-Token ist abgelaufen oder widerrufen. Bitte im PromptBoard-Settings-Dialog (Stern-Button) neu verbinden, sonst werden Historie-Eintraege nicht mehr cloud-synchronisiert.",
                            System.Windows.Forms.ToolTipIcon.Warning);
                    }
                    catch (Exception notifyEx)
                    {
                        LogHistorySync($"FAIL: Tray-balloon failed: {notifyEx.Message}");
                    }
                }
            }
        }

        /// <summary>
        /// Erkennt typische Auth-Fehler die der Benutzer aktiv beheben
        /// muss (Token widerrufen, abgelaufen, Client-ID falsch). Wir
        /// pruefen am Typnamen statt an einer harten Type-Reference,
        /// damit wir nicht von der Google-Apis-Library transitiv
        /// abhaengen — Reflection-frei, kompakt, zukunftssicher gegen
        /// Library-Updates die die Klasse umbenennen.
        /// </summary>
        private static bool IsDriveAuthFailure(Exception ex)
        {
            for (var current = ex; current is not null; current = current.InnerException)
            {
                string name = current.GetType().Name;
                string msg = current.Message ?? string.Empty;
                if (name.Contains("TokenResponseException", StringComparison.Ordinal)) return true;
                if (msg.Contains("invalid_grant", StringComparison.OrdinalIgnoreCase)) return true;
                if (msg.Contains("Token has been expired", StringComparison.OrdinalIgnoreCase)) return true;
                if (msg.Contains("revoked", StringComparison.OrdinalIgnoreCase)) return true;
            }
            return false;
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
                var local = await _historyService.LoadAllAsync();
                var merged = PromptHistoryDriveSync.MergeEntries(local, cloud);
                bool unchanged = merged.Count == local.Count && merged
                    .Zip(local, (cloudEntry, localEntry) =>
                        string.Equals(cloudEntry.Id, localEntry.Id, StringComparison.OrdinalIgnoreCase) &&
                        string.Equals(cloudEntry.Title, localEntry.Title, StringComparison.Ordinal) &&
                        string.Equals(cloudEntry.Text, localEntry.Text, StringComparison.Ordinal) &&
                        cloudEntry.Timestamp == localEntry.Timestamp &&
                        cloudEntry.UpdatedAt == localEntry.UpdatedAt &&
                        cloudEntry.ArchivedAt == localEntry.ArchivedAt)
                    .All(equal => equal);
                if (unchanged)
                {
                    Console.WriteLine("Cloud history merge: no changes.");
                    return;
                }
                await _historyService.ReplaceAllAsync(merged);
                await TryUploadHistoryAsync();
                Console.WriteLine($"Cloud history merged: {merged.Count - local.Count} new entries or revised content.");
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

        // ── Prompt-Zwischenspeicher-Slots: Cloud-Sync (1:1 zum History-Muster) ──

        /// <summary>
        /// Laedt die lokale prompt-slots.json SOFORT zu Drive hoch. Wird nach
        /// jedem Speichern UND jedem Loeschen eines Slots aufgerufen (Frank-
        /// Wunsch: direkt nach Speichern und Loeschen syncen). Fire-and-forget.
        /// </summary>
        private async Task TryUploadSlotsAsync()
        {
            try
            {
                var sync = GetOrCreateSlotSync();
                if (sync is null) return;
                await sync.UploadSlotsAsync(_slotService.SlotsFilePath);
                _promptPanel?.MarkSyncedNow();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Slot upload failed: {ex.Message}");
            }
        }

        /// <summary>
        /// Holt die Cloud-Slots und mergt sie mit dem lokalen Stand (pro Nummer
        /// gewinnt der juengste UpdatedAt — auch Tombstones). Einmal beim Start.
        /// </summary>
        public async Task TryMergeSlotsFromCloudAsync()
        {
            try
            {
                var sync = GetOrCreateSlotSync();
                if (sync is null) return;
                string? cloud = await sync.DownloadSlotsAsync();
                if (cloud is null) return;
                var local = await _slotService.LoadEntriesAsync();
                var merged = PromptSlotDriveSync.MergeEntries(local, cloud);
                await _slotService.ReplaceAllAsync(merged);
                // Offene Eingabe-Leiste sofort aktualisieren.
                _promptPanel?.ReloadSlots();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Slot cloud merge skipped: {ex.Message}");
            }
        }

        private PromptSlotDriveSync? GetOrCreateSlotSync()
        {
            if (_slotSync is not null) return _slotSync;
            try
            {
                var store = PromptBoardHost.Get<PromptBoardSecretStore>();
                _slotSync = new PromptSlotDriveSync(store);
                return _slotSync;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"PromptSlotDriveSync init skipped: {ex.Message}");
                return null;
            }
        }

        /// <summary>
        /// Laedt das lokale Vokabular-Woerterbuch SOFORT zu Drive hoch. Wird
        /// nach jedem Speichern im Settings-Dialog aufgerufen. Fire-and-forget.
        /// </summary>
        private async Task TryUploadVocabularyAsync()
        {
            try
            {
                var sync = GetOrCreateVocabSync();
                if (sync is null) return;
                await sync.UploadAsync();
                // Sync-Timestamp im Promptboard aktualisieren — sichtbare
                // Bestaetigung, dass das Vokabular zu Drive hochgeladen wurde
                // (gleicher Timestamp wie Slots/History; Frank-Wunsch).
                _promptPanel?.MarkSyncedNow();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Vocabulary upload failed: {ex.Message}");
            }
        }

        /// <summary>
        /// Holt das Cloud-Vokabular und VEREINIGT es mit dem lokalen Stand
        /// (kein Wort geht verloren). Einmal beim Start. Aendert sich etwas,
        /// wird der vereinte Stand zurueck in die Cloud geschrieben, damit alle
        /// Geraete konvergieren. Erstes Geraet ohne Cloud-Stand saet den lokalen.
        /// </summary>
        public async Task TryMergeVocabularyFromCloudAsync()
        {
            try
            {
                var sync = GetOrCreateVocabSync();
                if (sync is null) return;

                string? cloud = await sync.DownloadAsync();
                var path = PromptVocabularyDriveSync.LocalPath;
                string local = System.IO.File.Exists(path)
                    ? await System.IO.File.ReadAllTextAsync(path)
                    : string.Empty;

                if (cloud is null)
                {
                    // Noch kein Cloud-Stand vorhanden: lokalen Stand als Saat hochladen.
                    if (!string.IsNullOrWhiteSpace(local)) _ = TryUploadVocabularyAsync();
                    return;
                }

                var merged = PromptVocabularyDriveSync.MergeVocabularies(local, cloud);
                if (!string.Equals(merged.Trim(), local.Trim(), StringComparison.Ordinal))
                {
                    var dir = System.IO.Path.GetDirectoryName(path);
                    if (!string.IsNullOrEmpty(dir)) System.IO.Directory.CreateDirectory(dir);
                    await System.IO.File.WriteAllTextAsync(path, merged + "\n");
                    // Vereinten Stand zurueck in die Cloud, damit alle Geraete konvergieren.
                    _ = TryUploadVocabularyAsync();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Vocabulary cloud merge skipped: {ex.Message}");
            }
        }

        private PromptVocabularyDriveSync? GetOrCreateVocabSync()
        {
            if (_vocabSync is not null) return _vocabSync;
            try
            {
                var store = PromptBoardHost.Get<PromptBoardSecretStore>();
                _vocabSync = new PromptVocabularyDriveSync(store);
                return _vocabSync;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"PromptVocabularyDriveSync init skipped: {ex.Message}");
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
                // BuildBothAsync: ein einziger DB-Roundtrip statt zwei separaten
                // (BuildPreAsync + BuildPostAsync). Spart pro Voice-Submit
                // 1 Scope-Erstellung + 1 Repository-Resolve + 1 SQLite-Query.
                var (pre, post) = await _alwaysOnPrefix.BuildBothAsync();
                return (pre ?? string.Empty, post ?? string.Empty);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AlwaysOn wrappers build failed: {ex.Message}");
                return (string.Empty, string.Empty);
            }
        }

        /// <summary>C button — copy selected text via Ctrl+C.</summary>
        private async void BtnCopy_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _appWatcher.ActiveAppHwnd;

            // Flash: gray for 2 s then back to blue
            CopyButton.Background = BtnIdle;

            // Async: BringToForeground macht Thread.Sleep(200) — UI war
            // sonst pro Klick 200 ms eingefroren.
            await AppController.CopySelectionAsync(hwnd);

            _ = Task.Run(async () =>
            {
                await Task.Delay(2000);
                Dispatcher.Invoke(() => CopyButton.Background = BtnCopy);
            });

            Console.WriteLine("Copy: Ctrl+C sent to terminal");
        }

        /// <summary>P button — paste clipboard content into command line via Ctrl+V.
        /// If auto-enter is enabled, sends Enter after paste.</summary>
        private async void BtnPaste_Click(object sender, RoutedEventArgs e)
        {
            var hwnd = _appWatcher.ActiveAppHwnd;

            // Flash: gray for 2 s then back to purple
            PasteButton.Background = BtnIdle;

            // Async-Variante: gleicher 200-ms-Block wie CopySelection.
            await AppController.PasteClipboardAsync(hwnd);
            hasPastedText = true;

            if (autoEnterEnabled)
            {
                // Small delay then send Enter
                _ = Task.Run(() =>
                {
                    System.Threading.Thread.Sleep(300);
                    AppController.PressReturn(hwnd);
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
                //
                // Wichtig: WrapStringTooltips ersetzt die String-ToolTips im
                // Constructor durch ToolTip-Objekte mit Position-Handler.
                // Wuerden wir den ToolTip mit einem String UEBERSCHREIBEN,
                // ginge die dynamische Positionierung fuer diesen Button
                // verloren — der Tooltip wuerde wieder mitten ueberm Mic
                // landen statt links neben dem Pillar. Stattdessen den
                // Content des bestehenden ToolTip-Objekts updaten.
                if (InsertScreenshotButton.ToolTip is System.Windows.Controls.ToolTip tt)
                {
                    tt.Content = $"Letzten Screenshot einfügen: {filename}";
                }
                else
                {
                    // Defensiver Fallback (sollte praktisch nie greifen,
                    // weil WrapStringTooltips immer wrappt — aber falls
                    // jemand spaeter den Tooltip in XAML loescht, kein Crash).
                    InsertScreenshotButton.ToolTip = $"Letzten Screenshot einfügen: {filename}";
                }

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
        private async void BtnInsertScreenshot_Click(object sender, RoutedEventArgs e)
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

            var hwnd = _appWatcher.ActiveAppHwnd;
            // Async-Variante: blockiert UI nicht waehrend Win32-Foreground-
            // Sleeps. Folgender Erfolgs-Flash und Logging laufen nach await.
            await AppController.PasteTextAsync(toPaste, hwnd, autoEnter: false);

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
                    MicButton.Background = BtnMicIdlePurple;
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

            // Auto-Hide: waehrend einer Aufnahme immer ausgeklappt (Welle
            // sichtbar). Ab Processing sofort einklappen, sobald die Maus die
            // gesamte eigene UI verlassen hat; die Transkription laeuft weiter.
            if (_autoHideEnabled)
            {
                if (state == RecordingState.Recording)
                {
                    if (_isCollapsed) Expand();
                }
                else if (!_mouseOverOverlay &&
                         (state == RecordingState.Processing
                          || state == RecordingState.Idle
                          || state == RecordingState.Success
                          || state == RecordingState.Error))
                {
                    ScheduleCollapse();
                }
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
        // Geteilter Frozen-Brush fuer alle 14 Waveform-Striche. Frueher bekam
        // jeder Strich ein eigenes new SolidColorBrush(...) ohne .Freeze() —
        // jeder dieser 14 Brushes haengte sich an WPFs Change-Notification-
        // Pipeline. Mit einem einzigen statischen, eingefrorenen Brush spart
        // sich die Render-Ebene 14 Subscriptions, und die Brush selbst landet
        // im gleichen Resource-Slot wie die anderen statischen Farbflaechen
        // weiter oben in der Datei. Optisch identisch (gleicher RGB-Wert).
        private static readonly SolidColorBrush WaveformBarFill = Brush("#1A1A1A");

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
                    Fill = WaveformBarFill,  // shared frozen brush — geteilt ueber alle 14 Striche
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
            // Schneller Sichtbarkeits-Filter VOR dem Dispatcher-Marshall.
            // Liest ein volatile bool, das SetWaveformVisible aktualisiert.
            // Spart pro nicht-sichtbarem Buffer (z.B. BTW-Aufnahme: 10/s)
            // ein Lambda + ein BeginInvoke in die UI-Queue.
            if (!_waveformVisibleFast) return;

            // Marshall auf den UI-Thread — das Event kommt vom NAudio-
            // Buffer-Thread. BeginInvoke statt Invoke damit der Audio-
            // Thread nicht auf das UI-Rendering wartet.
            Dispatcher.BeginInvoke(new Action(() =>
            {
                // Doppel-Check auf dem UI-Thread (gegen die Race wo die
                // Welle zwischen Pre-Filter und Dispatcher-Tick versteckt
                // wurde). Wenn Welle weg: nichts tun.
                if (WaveformCanvas == null || WaveformCanvas.Visibility != Visibility.Visible)
                    return;

                // Pegel verstaerken: Wurzel macht leise Toene sichtbarer,
                // Faktor 1.6 hebt das Ergebnis nochmal an. Cap auf 1.0
                // verhindert dass Vollausschlag den Canvas verlaesst.
                float boosted = MathF.Min(1f, MathF.Sqrt(level) * 1.6f);

                // Buffer nach links shiften, neuer Wert rechts rein.
                // Array.Copy ueberlappenden Source/Dest ist ausdruecklich
                // erlaubt und nutzt intern memmove — schneller und allokations-
                // frei gegenueber der Hand-Schleife. Bei 10 Aufrufen/Sekunde
                // waehrend einer Aufnahme summieren sich die Iterations-Kosten.
                Array.Copy(_waveformBuffer, 1, _waveformBuffer, 0, WaveformBarCount - 1);
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
                _waveformVisibleFast = true;
            }
            else
            {
                WaveformCanvas.Visibility = Visibility.Collapsed;
                MicIcon.Visibility = Visibility.Visible;
                _waveformVisibleFast = false;
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
            // KRITISCHER Hot-Path: dieser Callback wird systemweit auf
            // JEDEN Tastendruck und -loslassen aufgerufen. Eine geworfene
            // Exception wuerde Windows den Hook deinstallieren lassen —
            // Frank merkt nur, dass PTT/Hotkeys plotzlich nicht mehr
            // gehen, mit keinerlei Fehlermeldung. Der Top-Level-try/catch
            // umschliesst den gesamten Body inkl. PtrToStructure (kann
            // bei korruptem lParam werfen) und sorgt dafuer, dass im
            // Worst Case der Tastendruck einfach durchgereicht wird statt
            // dass der Hook stirbt. Direktive-3-Resilienz: kein Fix-
            // induzierter Failure-Pfad — falls der Bug in nCode<0
            // CallNextHookEx selbst liegt, faellt der catch-Pfad mit
            // IntPtr.Zero zurueck und Windows nimmt das genauso an.
            try
            {
                return OnLowLevelKeyImpl(nCode, wParam, lParam);
            }
            catch (Exception ex)
            {
                // Direkt loggen — wir sind nicht auf dem UI-Thread und
                // duerfen keine Dispatcher-Operation (re-entrancy-Risiko
                // bei Hook-Crash).
                try { Console.WriteLine($"PTT hook crash (suppressed): {ex.GetType().Name}: {ex.Message}"); } catch { }
                // Default: durchreichen damit die Tastatur funktioniert.
                try { return NativeMethods.Win32.CallNextHookEx(_pttHookHandle, nCode, wParam, lParam); }
                catch { return IntPtr.Zero; }
            }
        }

        private IntPtr OnLowLevelKeyImpl(int nCode, IntPtr wParam, IntPtr lParam)
        {
            // nCode < 0 bedeutet "nicht verarbeiten, einfach weiterleiten"
            if (nCode < 0)
                return NativeMethods.Win32.CallNextHookEx(_pttHookHandle, nCode, wParam, lParam);

            int msg = wParam.ToInt32();
            // KBDLLHOOKSTRUCT.vkCode liegt als erstes Feld auf Offset 0 und ist
            // ein uint (4 Byte). Frueher: Marshal.PtrToStructure<KBDLLHOOKSTRUCT>(lParam) —
            // marshaled die KOMPLETTE Struct (vkCode + scanCode + flags + time +
            // dwExtraInfo = 5 Felder, ~20-24 Byte) selbst dann, wenn nur vkCode
            // gelesen wird. Bei systemweitem Hot-Path (10-20 Hook-Calls/s waehrend
            // normaler Tipp-Geschwindigkeit, 24/7 im Hintergrund) summiert sich
            // das ueber den Tag zu zehntausenden unnoetiger Marshal-Operationen.
            // Marshal.ReadInt32 liest direkt 4 Byte am Offset 0 — keine Struct-
            // Initialisierung, kein Reflection-basierter Layout-Lookup. Cast zu
            // uint ist binaer identisch (Two's-complement-Layout).
            uint vk = (uint)Marshal.ReadInt32(lParam, 0);

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
                            try
                            {
                                BtnMic_Click(this, new RoutedEventArgs());
                                if (_micState != RecordingState.Recording)
                                {
                                    _pttRecording = false;
                                    _pttToggleMode = false;
                                }
                            }
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
                if (isDown && ctrl && alt)
                {
                    if (DateTime.UtcNow < _screenshotCooldownUntil) return new IntPtr(1);
                    _screenshotCooldownUntil = DateTime.UtcNow.AddMilliseconds(ScreenshotCooldownMs);
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
                if (isDown && ctrl && alt) return new IntPtr(1); // im Cooldown / Auto-Repeat schlucken
            }

            // Insert-Screenshot-Hotkey: Strg+Alt+I
            if (vk == NativeMethods.Win32.VK_I)
            {
                bool ctrl = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_CONTROL) & 0x8000) != 0;
                bool alt  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU)    & 0x8000) != 0;
                bool isDown = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                if (isDown && ctrl && alt)
                {
                    if (DateTime.UtcNow < _insertCooldownUntil) return new IntPtr(1);
                    _insertCooldownUntil = DateTime.UtcNow.AddMilliseconds(InsertCooldownMs);
                    Console.WriteLine("Hotkey: Strg+Alt+I — Insert Screenshot");
                    Dispatcher.BeginInvoke(new Action(() =>
                    {
                        try { BtnInsertScreenshot_Click(this, new RoutedEventArgs()); }
                        catch (Exception ex) { Console.WriteLine($"Insert hotkey error: {ex.Message}"); }
                    }));
                    return new IntPtr(1);
                }
                if (isDown && ctrl && alt) return new IntPtr(1); // im Cooldown / Auto-Repeat schlucken
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
                        // Aktives Fenster pruefen — AppWatcher haelt den
                        // letzten erkannten Terminal-HWND. Null/Zero heisst:
                        // gerade ist KEIN Terminal aktiv → wir lassen die
                        // Taste durch (Browser-Tabs etc. funktionieren weiter).
                        IntPtr appHwnd = _appWatcher.ActiveAppHwnd;
                        IntPtr foreground = NativeMethods.Win32.GetForegroundWindow();
                        bool terminalIsForeground = appHwnd != IntPtr.Zero
                            && (foreground == appHwnd || IsWindowDescendantOf(foreground, appHwnd));

                        if (terminalIsForeground)
                        {
                            if (isDown && !_promptHotkeyDown[hotkeyNumber])
                            {
                                _promptHotkeyDown[hotkeyNumber] = true;
                                Console.WriteLine($"Hotkey: Strg+{hotkeyNumber} — paste prompt ({e.EffectiveText.Length} chars)");
                                // Async-Variante: das urspruengliche sync PasteText
                                // im Dispatcher-Lambda blockierte den UI-Thread fuer
                                // ~500 ms pro Hotkey-Druck. PasteTextAsync verlagert
                                // die Win32-Sleeps auf einen Background-Thread; das
                                // Lambda wird async, der Dispatcher-Marshall bleibt
                                // wie gehabt (Action ist mit async void kompatibel).
                                Dispatcher.BeginInvoke(new Action(async () =>
                                {
                                    try
                                    {
                                        await AppController.PasteTextAsync(
                                            e.EffectiveText,
                                            _appWatcher.ActiveAppHwnd,
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

            // Strg+Shift+Alt+E-Hotkey wurde am 2026-05-15 entfernt (Frank's
            // Wunsch in Commit #2253). Der Auto-Enter-Toggle laeuft jetzt
            // ausschliesslich ueber das Stream-Deck-Plugin via HTTP-API
            // (POST /autoenter/toggle) — siehe AutoEnterStatusServer.cs.
            // Ein global registrierter Hotkey ist ueberfluessig und war
            // eine zusaetzliche Drift-Quelle (eigener Codepfad zum State).

            // ── Prompt-Hotkeys: Win+Alt+A..Win+Alt+Z ───────────────────────
            //
            // 1:1 dieselbe Struktur wie der Strg+1..9-Zweig oben, nur mit
            // anderen Tasten — Frank's ausdruecklicher Wunsch in #2238.
            // Wir feuern nur wenn:
            //   1) eine reine Win+Alt-Kombi gehalten wird (kein Ctrl, kein Shift),
            //   2) ein Prompt mit diesem Buchstaben in der HotkeyRegistry steht,
            //   3) ein Terminal-Fenster im Vordergrund ist.
            // Sonst laeuft die Taste durch — Browser-/Editor-Shortcuts bleiben
            // unberuehrt.
            //
            // Datei-Logging: jeder DOWN-Event und jeder FIRE wird in
            // %TEMP%\TVO-hotkey.log gestempelt. So koennen wir nachvollziehen
            // ob der Hook gefeuert hat und ob terminalIsForeground wahr war
            // — ohne dass Frank ein Debugger braucht.
            if (vk >= 0x41 && vk <= 0x5A)
            {
                bool ctrl  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_CONTROL) & 0x8000) != 0;
                bool alt   = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_MENU)    & 0x8000) != 0;
                bool shift = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_SHIFT)   & 0x8000) != 0;
                bool lwin  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_LWIN)    & 0x8000) != 0;
                bool rwin  = (NativeMethods.Win32.GetAsyncKeyState(NativeMethods.Win32.VK_RWIN)    & 0x8000) != 0;
                bool win   = lwin || rwin;
                bool isDown = (msg == NativeMethods.Win32.WM_KEYDOWN || msg == NativeMethods.Win32.WM_SYSKEYDOWN);
                bool isUp   = (msg == NativeMethods.Win32.WM_KEYUP   || msg == NativeMethods.Win32.WM_SYSKEYUP);

                if (win && alt && !ctrl && !shift && isDown)
                {
                    char letter = (char)vk; // 0x41..0x5A → 'A'..'Z'
                    int letterIdx = (int)(vk - 0x41); // 0..25 fuer Down-Latch
                    bool hasAny = HotkeyRegistry.HasAnyLetter;
                    var entry = hasAny ? HotkeyRegistry.LookupLetter(letter) : null;
                    // Logging IMMER, auch wenn Registry leer oder kein Match —
                    // damit wir live nachverfolgen koennen warum es nicht feuert.
                    LogHotkeyEvent($"Win+Alt+{letter} DOWN hasAnyLetter={hasAny} matched={entry is not null}");
                    if (entry is HotkeyRegistry.Entry e)
                    {
                        IntPtr appHwnd = _appWatcher.ActiveAppHwnd;
                        IntPtr foreground = NativeMethods.Win32.GetForegroundWindow();
                        bool terminalIsForeground = appHwnd != IntPtr.Zero
                            && (foreground == appHwnd || IsWindowDescendantOf(foreground, appHwnd));

                        LogHotkeyEvent($"Win+Alt+{letter} GATE termHwnd={appHwnd} fgHwnd={foreground} isFg={terminalIsForeground}");

                        if (terminalIsForeground)
                        {
                            if (!_promptLetterHotkeyDown[letterIdx])
                            {
                                _promptLetterHotkeyDown[letterIdx] = true;
                                LogHotkeyEvent($"Win+Alt+{letter} FIRE → paste {e.EffectiveText.Length} chars");
                                Dispatcher.BeginInvoke(new Action(async () =>
                                {
                                    try
                                    {
                                        await AppController.PasteTextAsync(
                                            e.EffectiveText,
                                            _appWatcher.ActiveAppHwnd,
                                            autoEnterEnabled);
                                    }
                                    catch (Exception ex)
                                    {
                                        LogHotkeyEvent($"Win+Alt+{letter} paste failed: {ex.Message}");
                                    }
                                }));
                                return new IntPtr(1);
                            }
                            return new IntPtr(1); // Auto-Repeat schlucken
                        }
                    }
                }
                else if (isUp)
                {
                    // Sicherheitsnetz analog zum Number-Zweig.
                    int letterIdx = (int)(vk - 0x41);
                    if (letterIdx >= 0 && letterIdx <= 25)
                        _promptLetterHotkeyDown[letterIdx] = false;
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
        /// Indices 0..25 = 'A'..'Z' fuer die Win+Alt+Buchstabe-Hotkeys.
        /// Gleiche Auto-Repeat-Schutzlogik wie <see cref="_promptHotkeyDown"/>.
        /// </summary>
        private readonly bool[] _promptLetterHotkeyDown = new bool[26];

        /// <summary>
        /// Append-only Diagnose-Log fuer Letter-Hotkey-Events. Schreibt
        /// nach %TEMP%\TVO-hotkey.log. Wird vom Low-Level-Keyboard-Hook-
        /// Thread aufgerufen — Datei-IO darf den Hook NICHT blockieren oder
        /// werfen, daher try/catch um alles. Sehr kurze Zeile, keine
        /// Buffer-Flushes (default trailing flush ist OK).
        /// </summary>
        private static void LogHotkeyEvent(string message)
        {
            try
            {
                string path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "TVO-hotkey.log");
                System.IO.File.AppendAllText(path, $"{DateTime.Now:HH:mm:ss.fff} {message}{Environment.NewLine}");
            }
            catch
            {
                // Stille: Logging darf den Hook nicht in die Knie zwingen.
            }
        }

        /// <summary>
        /// Manche Terminal-Apps (Windows Terminal, VS Code Integrated
        /// Terminal) packen den eigentlichen TermControl in ein Kindfenster.
        /// Wenn der Benutzer dort tippt liefert GetForegroundWindow das
        /// AEUSSERE Hauptfenster — der AppWatcher hat aber den HWND
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
            _topmostAssertTimer.Stop();
            _tooltipHoverTimer?.Stop();

            // X-Repeat-Loop sauber stoppen falls er noch laeuft (z.B.
            // Window-Close mitten waehrend Frank den X-Button haelt).
            // Ohne diesen Cleanup wuerde der Background-Task weiter
            // ClearLine ans Terminal schicken obwohl das Overlay schon
            // weg ist. Lock fuer den Race mit StopXRepeat.
            lock (_xRepeatLock)
            {
                if (_xRepeatCts is { } cts)
                {
                    try { cts.Cancel(); } catch { /* race-safe */ }
                    try { cts.Dispose(); } catch { /* race-safe */ }
                    _xRepeatCts = null;
                }
            }

            // Hook abbauen — sonst bleibt die DLL im Tastatur-Stack haengen
            // und alle Tastendruecke laufen weiter durch unseren Callback.
            if (_pttHookHandle != IntPtr.Zero)
            {
                NativeMethods.Win32.UnhookWindowsHookEx(_pttHookHandle);
                _pttHookHandle = IntPtr.Zero;
            }
            // Delegate-Referenz freigeben damit die GC den Hook-Proc
            // einsammeln kann sobald nichts mehr darauf zeigt.
            _pttHookProc = null;

            _audioRecorder.LevelChanged -= OnAudioLevelChanged;
            if (_geminiUploadSubscribed)
            {
                GeminiPromptDriveSync.UploadSucceeded -= OnGeminiPromptUploadSucceeded;
                _geminiUploadSubscribed = false;
            }
            _appWatcher.Dispose();
            _recordingCuePlayer.Dispose();
            _audioRecorder.Dispose();
            base.OnClosed(e);
        }
    }
}
