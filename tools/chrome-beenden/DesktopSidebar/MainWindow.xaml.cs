using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
// MouseEventArgs ist mehrdeutig (Projekt referenziert auch WinForms fuer Screen) -> WPF-Variante festlegen.
using MouseEventArgs = System.Windows.Input.MouseEventArgs;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
using WinFormsScreen = System.Windows.Forms.Screen;

namespace DesktopSidebar;

public partial class MainWindow : Window
{
    // ===== Win32-Interop =====
    [StructLayout(LayoutKind.Sequential)]
    private struct POINT { public int X; public int Y; }

    [DllImport("user32.dll")] private static extern bool GetCursorPos(out POINT lpPoint);

    [DllImport("user32.dll")]
    private static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter,
        int X, int Y, int cx, int cy, uint uFlags);

    [DllImport("user32.dll")] private static extern int GetWindowLong(IntPtr hWnd, int nIndex);
    [DllImport("user32.dll")] private static extern int SetWindowLong(IntPtr hWnd, int nIndex, int dwNewLong);
    [DllImport("user32.dll")] private static extern IntPtr WindowFromPoint(POINT p);
    [DllImport("user32.dll")] private static extern IntPtr GetAncestor(IntPtr hWnd, uint gaFlags);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

    private static readonly IntPtr HWND_TOPMOST = new(-1);
    private const uint SWP_NOSIZE = 0x0001, SWP_NOMOVE = 0x0002, SWP_NOACTIVATE = 0x0010;
    private const int GWL_EXSTYLE = -20;
    private const uint GA_ROOT = 2;                  // GetAncestor -> oberstes Wurzelfenster
    private const int WS_EX_TOOLWINDOW = 0x00000080; // nicht im Alt-Tab / nicht in der Taskbar
    private const int WS_EX_NOACTIVATE = 0x08000000; // klaut beim Anzeigen keinen Fokus
    private const int WS_EX_TRANSPARENT = 0x00000020; // maus-durchlaessig (Almanach §2.4)

    // Symbol-Glyphen (muessen zu den Tag-Werten in MainWindow.xaml passen).
    private const string GlyphRestart = "";
    private const string GlyphPower = "";
    private const string GlyphWarning = "";

    // ===== Konfiguration =====
    private const string ChromeLnk = @"C:\Users\barwa\Desktop\Chrome beenden.lnk";
    private const int TriggerZonePx = 2;     // wie nah am rechten Rand der Cursor sein muss
    private const int HideMarginPx = 8;      // wie weit links die Maus muss, damit es wieder zugeht
    private const int ChromeWaitMs = 6000;   // max. Wartezeit, bis chrome.exe weg ist

    // ===== Zustand =====
    private readonly DispatcherTimer _poll = new() { Interval = TimeSpan.FromMilliseconds(60) };
    private DispatcherTimer? _countdownTimer;
    private IntPtr _hwnd;
    private double _dpi = 1.0;
    private bool _isOpen;
    private bool _busy;            // Countdown laeuft / Aktion startet -> nicht automatisch schliessen
    private int _topmostTick;
    private bool _indicatorShown;  // aktueller Sichtbarkeitszustand des Rand-Indikators

    // Vertikales Verschieben per Maus (Drag) + persistente Position
    private bool _dragging;
    private int _dragStartCursorY; // Cursor-Y bei Drag-Beginn (physische Pixel)
    private int _dragStartTopPx;   // Fenster-Oberkante bei Drag-Beginn (physische Pixel)
    private int? _savedTopPx;      // beim Start geladene gespeicherte Position (falls vorhanden)

    // Primaermonitor in physischen Pixeln
    private int _monLeftPx, _monRightPx, _monTopPx, _monBottomPx;
    private int _winLeftPx;        // linke Fensterkante in physischen Pixeln
    private int _winTopPx;         // obere Fensterkante in physischen Pixeln (aktuell)
    private int _winHeightPx;      // Fensterhoehe in physischen Pixeln

    private bool _restartAction;
    private int _countdownValue;

    public MainWindow()
    {
        InitializeComponent();
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);

        _hwnd = new WindowInteropHelper(this).Handle;

        // Aus Alt-Tab/Taskbar nehmen, keinen Fokus klauen, und geschlossen klick-durchlaessig
        // (der unsichtbare Randstreifen darf keine Desktop-Klicks/Icons blockieren — Almanach §2.4).
        int ex = GetWindowLong(_hwnd, GWL_EXSTYLE);
        SetWindowLong(_hwnd, GWL_EXSTYLE, ex | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE | WS_EX_TRANSPARENT);

        _dpi = VisualTreeHelper.GetDpi(this).DpiScaleX;

        LoadSavedPosition();
        PositionWindow();
        ApplyTopmost();

        // Start: eingefahren (unsichtbar am rechten Rand, inkl. Schatten).
        SlideTransform.X = HiddenX();
        _isOpen = false;

        // Dunkle Flaeche als vertikaler Ziehgriff (Buttons verschlucken ihren eigenen Klick).
        RootBorder.MouseLeftButtonDown += Drag_MouseDown;
        RootBorder.MouseMove += Drag_MouseMove;
        RootBorder.MouseLeftButtonUp += Drag_MouseUp;

        _poll.Tick += Poll_Tick;
        _poll.Start();
    }

    private double ActualWidthOrFallback() => Width > 0 ? Width : 92;

    /// <summary>Versteck-Offset: Fensterbreite plus Rand fuer den Schlagschatten.</summary>
    private double HiddenX() => ActualWidthOrFallback() + 30;

    /// <summary>Positioniert das Fenster rechts am Primaermonitor, vertikal zentriert (physische Pixel).</summary>
    private void PositionWindow()
    {
        var b = WinFormsScreen.PrimaryScreen!.Bounds; // physische Pixel
        _monLeftPx = b.Left;
        _monRightPx = b.Right;
        _monTopPx = b.Top;
        _monBottomPx = b.Bottom;

        int widthPx = (int)Math.Round(Width * _dpi);
        _winHeightPx = (int)Math.Round(Height * _dpi);

        _winLeftPx = b.Right - widthPx;

        // Gespeicherte vertikale Position verwenden, sonst vertikal zentrieren.
        int defaultTop = b.Top + (b.Height - _winHeightPx) / 2;
        _winTopPx = ClampTop(_savedTopPx ?? defaultTop);

        // Nur Position setzen (physische Pixel), Groesse bleibt WPF-DIP. Almanach §3.3.
        SetWindowPos(_hwnd, HWND_TOPMOST, _winLeftPx, _winTopPx, 0, 0, SWP_NOSIZE | SWP_NOACTIVATE);
    }

    /// <summary>Haelt die Fenster-Oberkante komplett im sichtbaren Monitorbereich.</summary>
    private int ClampTop(int topPx)
    {
        int max = _monBottomPx - _winHeightPx;
        if (max < _monTopPx) max = _monTopPx;
        return Math.Max(_monTopPx, Math.Min(max, topPx));
    }

    /// <summary>Topmost erneut anwenden — geht bei ShowInTaskbar=false sonst verloren (Almanach §2.1/§2.2).</summary>
    private void ApplyTopmost()
    {
        SetWindowPos(_hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
    }

    /// <summary>Maus-Durchlaessigkeit umschalten: geschlossen = durchlaessig, offen = klickbar.</summary>
    private void SetClickThrough(bool on)
    {
        int ex = GetWindowLong(_hwnd, GWL_EXSTYLE);
        ex = on ? (ex | WS_EX_TRANSPARENT) : (ex & ~WS_EX_TRANSPARENT);
        SetWindowLong(_hwnd, GWL_EXSTYLE, ex);
    }

    /// <summary>
    /// True, wenn links neben der eingeklappten Sidebar (auf Hoehe yPx) der Windows-Desktop
    /// frei sichtbar ist — also KEIN anderes App-Fenster den rechten Bildschirmrand verdeckt.
    /// Bewusst NICHT ueber GetForegroundWindow: nach dem Schliessen/Minimieren eines Fensters
    /// ist Progman/WorkerW oft nicht das Vordergrundfenster, obwohl der Desktop sichtbar ist —
    /// dann ging die Sidebar frueher erst nach einem Klick auf eine freie Desktop-Flaeche auf.
    /// WindowFromPoint fragt stattdessen direkt, was an dieser Stelle tatsaechlich sichtbar ist.
    /// </summary>
    private bool IsDesktopFreeAt(int yPx)
    {
        // Punkt knapp LINKS neben der eingeklappten Sidebar pruefen, damit wir nicht das eigene
        // (klick-durchlaessige) Fenster treffen, sondern das wirklich sichtbare Fenster dort.
        int xPx = _winLeftPx - 4;
        if (xPx < _monLeftPx) xPx = _monLeftPx;

        IntPtr h = WindowFromPoint(new POINT { X = xPx, Y = yPx });
        if (h == IntPtr.Zero) return false;

        IntPtr root = GetAncestor(h, GA_ROOT);
        if (root == _hwnd) return true; // die eigene Sidebar zaehlt nicht als fremdes Fenster

        var sb = new StringBuilder(256);
        GetClassName(root, sb, sb.Capacity);
        string cls = sb.ToString();

        // "Progman" = Desktop-Shell, "WorkerW" = Desktop bei aktivem Hintergrund/Slideshow.
        return cls == "Progman" || cls == "WorkerW";
    }

    private void Poll_Tick(object? sender, EventArgs e)
    {
        // ca. jede Sekunde Topmost auffrischen (60ms * 16).
        if (++_topmostTick >= 16) { _topmostTick = 0; ApplyTopmost(); }

        // Rand-Indikator nachfuehren (zeigt nur eingeklappt + am Desktop).
        UpdateEdgeIndicator();

        // Waehrend Countdown/Aktion oder aktivem Ziehen bleibt die Sidebar offen.
        if (_busy || _dragging) return;

        if (!GetCursorPos(out POINT p)) return;

        if (!_isOpen)
        {
            // Cursor am rechten Rand des Primaermonitors?
            bool atRightEdge =
                p.X >= _monRightPx - TriggerZonePx &&
                p.X <= _monRightPx - 1 &&
                p.Y >= _monTopPx &&
                p.Y < _monBottomPx;

            // Nur oeffnen, wenn am rechten Rand der Desktop frei liegt — nicht ueber anderen Fenstern.
            if (atRightEdge && IsDesktopFreeAt(p.Y)) OpenSidebar();
        }
        else
        {
            // Maus deutlich links vom Fenster -> wieder einfahren.
            if (p.X < _winLeftPx - HideMarginPx) CloseSidebar();
        }
    }

    private void OpenSidebar()
    {
        if (_isOpen) return;
        _isOpen = true;
        EdgeIndicator.Visibility = Visibility.Collapsed;  // Indikator sofort weg
        _indicatorShown = false;
        SetClickThrough(false);                 // jetzt klickbar (Buttons)
        Animate(SlideTransform, 0, 220);
    }

    /// <summary>Blendet den Rand-Indikator ein, solange die Sidebar eingeklappt und der Desktop vorn ist.</summary>
    private void UpdateEdgeIndicator()
    {
        bool show = !_isOpen && !_busy && IsDesktopFreeAt((_monTopPx + _monBottomPx) / 2);
        if (show == _indicatorShown) return;
        _indicatorShown = show;
        EdgeIndicator.Visibility = show ? Visibility.Visible : Visibility.Collapsed;
    }

    private void CloseSidebar()
    {
        if (!_isOpen) return;
        _isOpen = false;
        // Erst nach dem Einfahren wieder durchlaessig schalten.
        Animate(SlideTransform, HiddenX(), 200, () => { if (!_isOpen) SetClickThrough(true); });
    }

    private static void Animate(TranslateTransform target, double to, int ms, Action? done = null)
    {
        var anim = new DoubleAnimation(to, TimeSpan.FromMilliseconds(ms))
        {
            EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
        };
        if (done != null) anim.Completed += (_, _) => done();
        target.BeginAnimation(TranslateTransform.XProperty, anim);
    }

    // ===== Vertikales Verschieben per Maus =====
    private void Drag_MouseDown(object sender, MouseButtonEventArgs e)
    {
        // Nur ziehen, wenn die Sidebar offen ist und kein Countdown laeuft.
        // Klicks auf die Buttons kommen hier nicht an (Button verschluckt MouseLeftButtonDown).
        if (!_isOpen || _busy) return;
        if (!GetCursorPos(out POINT p)) return;

        _dragging = true;
        _dragStartCursorY = p.Y;
        _dragStartTopPx = _winTopPx;
        RootBorder.CaptureMouse();
        e.Handled = true;
    }

    private void Drag_MouseMove(object sender, MouseEventArgs e)
    {
        if (!_dragging) return;
        if (!GetCursorPos(out POINT p)) return;

        // Immer aus der ABSOLUTEN Cursorposition rechnen — driftet nicht, wenn das Fenster mitwandert.
        int newTop = ClampTop(_dragStartTopPx + (p.Y - _dragStartCursorY));
        if (newTop == _winTopPx) return;

        _winTopPx = newTop;
        SetWindowPos(_hwnd, HWND_TOPMOST, _winLeftPx, _winTopPx, 0, 0, SWP_NOSIZE | SWP_NOACTIVATE);
    }

    private void Drag_MouseUp(object sender, MouseButtonEventArgs e)
    {
        if (!_dragging) return;
        _dragging = false;
        RootBorder.ReleaseMouseCapture();
        SavePosition();
        e.Handled = true;
    }

    // ===== Persistente vertikale Position =====
    private static string PositionFilePath() => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "ChromeShutdownSidebar", "sidebar-position.txt");

    private void LoadSavedPosition()
    {
        try
        {
            string path = PositionFilePath();
            if (File.Exists(path) && int.TryParse(File.ReadAllText(path).Trim(), out int top))
                _savedTopPx = top;
        }
        catch { /* kein/unlesbarer Wert -> Default (zentriert) */ }
    }

    private void SavePosition()
    {
        try
        {
            string path = PositionFilePath();
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            File.WriteAllText(path, _winTopPx.ToString());
        }
        catch { /* Speichern fehlgeschlagen -> beim naechsten Start eben zentriert */ }
    }

    // ===== Button-Handler =====
    private void RestartButton_Click(object sender, RoutedEventArgs e) => StartCountdown(restart: true);
    private void ShutdownButton_Click(object sender, RoutedEventArgs e) => StartCountdown(restart: false);

    private void StartCountdown(bool restart)
    {
        _restartAction = restart;
        _busy = true;
        _countdownValue = 3;

        CountdownGlyph.Text = restart ? GlyphRestart : GlyphPower;
        CountdownNumber.Text = _countdownValue.ToString();
        CancelButton.Visibility = Visibility.Visible;
        CountdownOverlay.Visibility = Visibility.Visible;

        _countdownTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
        _countdownTimer.Tick += Countdown_Tick;
        _countdownTimer.Start();
    }

    private void Countdown_Tick(object? sender, EventArgs e)
    {
        _countdownValue--;
        if (_countdownValue > 0)
        {
            CountdownNumber.Text = _countdownValue.ToString();
            return;
        }

        StopCountdownTimer();
        _ = ExecuteActionAsync();
    }

    private void CancelButton_Click(object sender, RoutedEventArgs e)
    {
        StopCountdownTimer();
        CountdownOverlay.Visibility = Visibility.Collapsed;
        _busy = false;
        // Sidebar bleibt offen; faehrt normal ein, sobald die Maus weggeht.
    }

    private void StopCountdownTimer()
    {
        if (_countdownTimer != null)
        {
            _countdownTimer.Stop();
            _countdownTimer.Tick -= Countdown_Tick;
            _countdownTimer = null;
        }
    }

    /// <summary>Chrome hart beenden, warten bis weg, dann Neustart/Herunterfahren.</summary>
    private async Task ExecuteActionAsync()
    {
        // Symbolzustand "wird ausgefuehrt": Abbrechen weg, nur noch das Aktions-Symbol.
        CancelButton.Visibility = Visibility.Collapsed;
        CountdownNumber.Text = "";

        // 1) Chrome-beenden-Verknuepfung starten (taskkill /F /IM chrome.exe /T).
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = ChromeLnk,
                UseShellExecute = true,
                WindowStyle = ProcessWindowStyle.Hidden
            });
        }
        catch
        {
            // Verknuepfung fehlt/fehlerhaft: direkt taskkill als Fallback, damit Chrome trotzdem zugeht.
            try
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = "taskkill.exe",
                    Arguments = "/F /IM chrome.exe /T",
                    UseShellExecute = false,
                    CreateNoWindow = true
                });
            }
            catch { /* nichts mehr moeglich — wir fahren trotzdem fort */ }
        }

        // 2) Warten, bis kein chrome.exe-Prozess mehr laeuft (mit Timeout).
        await WaitForChromeGoneAsync(ChromeWaitMs);

        // 3) Neustart bzw. Herunterfahren.
        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = "shutdown.exe",
                Arguments = _restartAction ? "/r /t 0" : "/s /t 0",
                UseShellExecute = false,
                CreateNoWindow = true
            });
        }
        catch (Exception ex)
        {
            // Fehlerzustand rein symbolisch anzeigen (Warn-Glyph), Abbrechen wieder erlauben.
            CountdownGlyph.Text = GlyphWarning;
            CountdownNumber.Text = "";
            CancelButton.Visibility = Visibility.Visible;
            _busy = false;
            Debug.WriteLine(ex);
        }
    }

    private static async Task WaitForChromeGoneAsync(int timeoutMs)
    {
        var sw = Stopwatch.StartNew();
        while (sw.ElapsedMilliseconds < timeoutMs)
        {
            Process[] procs = Process.GetProcessesByName("chrome");
            bool any = procs.Length > 0;
            foreach (var pr in procs) pr.Dispose();
            if (!any) return;
            await Task.Delay(200);
        }
    }
}
