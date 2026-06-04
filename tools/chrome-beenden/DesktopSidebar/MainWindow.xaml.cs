using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using System.Windows;
using System.Windows.Interop;
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
    [DllImport("user32.dll")] private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

    private static readonly IntPtr HWND_TOPMOST = new(-1);
    private const uint SWP_NOSIZE = 0x0001, SWP_NOMOVE = 0x0002, SWP_NOACTIVATE = 0x0010;
    private const int GWL_EXSTYLE = -20;
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

    // Primaermonitor in physischen Pixeln
    private int _monLeftPx, _monRightPx, _monTopPx, _monBottomPx;
    private int _winLeftPx;        // linke Fensterkante in physischen Pixeln

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

        PositionWindow();
        ApplyTopmost();

        // Start: eingefahren (unsichtbar am rechten Rand, inkl. Schatten).
        SlideTransform.X = HiddenX();
        _isOpen = false;

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
        int heightPx = (int)Math.Round(Height * _dpi);

        _winLeftPx = b.Right - widthPx;
        int topPx = b.Top + (b.Height - heightPx) / 2;

        // Nur Position setzen (physische Pixel), Groesse bleibt WPF-DIP. Almanach §3.3.
        SetWindowPos(_hwnd, HWND_TOPMOST, _winLeftPx, topPx, 0, 0, SWP_NOSIZE | SWP_NOACTIVATE);
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

    /// <summary>True, wenn der Windows-Desktop (Shell) das Vordergrundfenster ist.</summary>
    private static bool IsOnDesktop()
    {
        IntPtr fg = GetForegroundWindow();
        if (fg == IntPtr.Zero) return false;

        var sb = new StringBuilder(256);
        GetClassName(fg, sb, sb.Capacity);
        string cls = sb.ToString();

        // "Progman" = Desktop-Shell, "WorkerW" = Desktop bei aktivem Hintergrund/Slideshow.
        return cls == "Progman" || cls == "WorkerW";
    }

    private void Poll_Tick(object? sender, EventArgs e)
    {
        // ca. jede Sekunde Topmost auffrischen (60ms * 16).
        if (++_topmostTick >= 16) { _topmostTick = 0; ApplyTopmost(); }

        // Waehrend Countdown/Aktion bleibt die Sidebar offen.
        if (_busy) return;

        if (!GetCursorPos(out POINT p)) return;

        if (!_isOpen)
        {
            // Cursor am rechten Rand des Primaermonitors?
            bool atRightEdge =
                p.X >= _monRightPx - TriggerZonePx &&
                p.X <= _monRightPx - 1 &&
                p.Y >= _monTopPx &&
                p.Y < _monBottomPx;

            // Nur oeffnen, wenn der Desktop im Vordergrund ist — nicht ueber anderen Fenstern.
            if (atRightEdge && IsOnDesktop()) OpenSidebar();
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
        SetClickThrough(false);                 // jetzt klickbar (Buttons)
        Animate(SlideTransform, 0, 220);
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
