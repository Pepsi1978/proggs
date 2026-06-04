using System.Diagnostics;
using System.Runtime.InteropServices;
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

    private static readonly IntPtr HWND_TOPMOST = new(-1);
    private const uint SWP_NOSIZE = 0x0001, SWP_NOMOVE = 0x0002, SWP_NOACTIVATE = 0x0010;
    private const int GWL_EXSTYLE = -20;
    private const int WS_EX_TOOLWINDOW = 0x00000080; // nicht im Alt-Tab / nicht in der Taskbar
    private const int WS_EX_NOACTIVATE = 0x08000000; // klaut beim Anzeigen keinen Fokus

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

        // Aus Alt-Tab/Taskbar nehmen und Fokus-Diebstahl verhindern.
        int ex = GetWindowLong(_hwnd, GWL_EXSTYLE);
        SetWindowLong(_hwnd, GWL_EXSTYLE, ex | WS_EX_TOOLWINDOW | WS_EX_NOACTIVATE);

        _dpi = VisualTreeHelper.GetDpi(this).DpiScaleX;

        PositionWindow();
        ApplyTopmost();

        // Start: eingefahren (unsichtbar am rechten Rand).
        SlideTransform.X = ActualWidthOrFallback();
        _isOpen = false;

        _poll.Tick += Poll_Tick;
        _poll.Start();
    }

    private double ActualWidthOrFallback() => Width > 0 ? Width : 188;

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

            if (atRightEdge) Open();
        }
        else
        {
            // Maus deutlich links vom Fenster -> wieder einfahren.
            if (p.X < _winLeftPx - HideMarginPx) Close();
        }
    }

    private void Open()
    {
        if (_isOpen) return;
        _isOpen = true;
        Animate(SlideTransform, 0, 220);
    }

    private void Close()
    {
        if (!_isOpen) return;
        _isOpen = false;
        Animate(SlideTransform, ActualWidthOrFallback(), 200);
    }

    private static void Animate(TranslateTransform target, double to, int ms)
    {
        var anim = new DoubleAnimation(to, TimeSpan.FromMilliseconds(ms))
        {
            EasingFunction = new CubicEase { EasingMode = EasingMode.EaseOut }
        };
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

        CountdownTitle.Text = restart ? "Neustart" : "Herunterfahren";
        CountdownNumber.Text = _countdownValue.ToString();
        CountdownHint.Text = "Sekunden";
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
        CancelButton.Visibility = Visibility.Collapsed;
        CountdownNumber.Text = "";
        CountdownHint.Text = "Chrome wird beendet …";

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
        CountdownHint.Text = _restartAction ? "Neustart …" : "Herunterfahren …";
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
            CountdownHint.Text = "Fehler beim Beenden";
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
