using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls.Primitives;
using System.Windows.Data;
using System.Windows.Input;
using System.Windows.Interop;
using OpenLauncher.Services;
using OpenLauncher.ViewModels;

namespace OpenLauncher;

public partial class MainWindow : Window
{
    private const int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private const int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private const int DWMWCP_DONOTROUND = 1;
    private const int DWMWCP_ROUND = 2;
    private const int WM_GETMINMAXINFO = 0x0024;
    private const int WM_SIZE = 0x0005;
    private const int SIZE_MINIMIZED = 1;
    private const int WM_SYSCOMMAND = 0x0112;
    private const int SC_RESTORE = 0xF120;
    private const int MONITOR_DEFAULTTONEAREST = 2;
    private const int SW_RESTORE = 9;
    private const uint FLASHW_ALL = 0x00000003;
    private const uint FLASHW_TIMERNOFG = 0x0000000C;
    private const uint SWP_NOSIZE = 0x0001;
    private const uint SWP_NOMOVE = 0x0002;
    private const uint SWP_SHOWWINDOW = 0x0040;
    private static readonly IntPtr HWND_TOP = IntPtr.Zero;
    private static readonly IntPtr HWND_TOPMOST = new(-1);
    private static readonly IntPtr HWND_NOTOPMOST = new(-2);
    private readonly LayoutSettings _layoutSettings;
    private int _providerResizeColumnIndex = -1;
    private double[]? _providerResizeStartWidths;
    private double _providerResizeTotalDelta;
    // Zeitbasierte Sperren statt boolescher Flags: ein Flag, dessen Reset durch einen verschluckten
    // Dispatcher-Callback, eine Ausnahme oder einen Abbruch ausfaellt, blockiert die Fensteraktivierung
    // dauerhaft bis zum Prozess-Neustart — genau das gemeldete Symptom. Ein Zeitstempel laeuft immer
    // von selbst ab und kann nicht haengen bleiben (gleiche Lehre wie beim Hotkey-Debounce-Bug 21.06.).
    private const int ForegroundGuardMs = 1500;
    private const int MinimizeStuckCheckMs = 400;
    private DateTime _foregroundQueuedUntilUtc = DateTime.MinValue;
    private DateTime _foregroundRunningUntilUtc = DateTime.MinValue;
    // Zeitpunkt der letzten NATIVEN Minimierung (WM_SIZE/SIZE_MINIMIZED). WPFs WindowState hinkt der
    // nativen Wahrheit hinterher: kurz nach einem gewollten Minimieren meldet WPF noch einen
    // sichtbaren Zustand, waehrend IsIconic bereits true ist. Ohne diesen Zeitstempel wuerde die
    // Selbstheilung unten ein absichtlich minimiertes Fenster sofort wieder hochreissen.
    private DateTime _lastNativeMinimizeUtc = DateTime.MinValue;
    private string _queuedActivationReason = "unspecified";
    private readonly System.Windows.Threading.DispatcherTimer _layoutSaveTimer;
    private readonly System.Windows.Threading.DispatcherTimer _minimizeStuckTimer;

    [DllImport("dwmapi.dll")]
    private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int value, int size);

    [DllImport("user32.dll")]
    private static extern IntPtr MonitorFromWindow(IntPtr hwnd, int flags);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool GetMonitorInfo(IntPtr hMonitor, ref MONITORINFO lpmi);

    [DllImport("user32.dll")]
    private static extern bool ShowWindow(IntPtr hwnd, int nCmdShow);

    [DllImport("user32.dll")]
    private static extern bool SetForegroundWindow(IntPtr hwnd);

    [DllImport("user32.dll")]
    private static extern IntPtr SetFocus(IntPtr hwnd);

    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hwnd, out uint processId);

    [DllImport("kernel32.dll")]
    private static extern uint GetCurrentThreadId();

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool AttachThreadInput(uint idAttach, uint idAttachTo, bool attach);

    [DllImport("user32.dll")]
    private static extern bool IsIconic(IntPtr hwnd);

    [DllImport("user32.dll")]
    private static extern bool FlashWindowEx(ref FLASHWINFO pwfi);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool SetWindowPos(IntPtr hwnd, IntPtr hwndInsertAfter, int x, int y, int cx, int cy, uint flags);

    [DllImport("user32.dll")]
    private static extern bool BringWindowToTop(IntPtr hwnd);

    [DllImport("user32.dll")]
    private static extern void SwitchToThisWindow(IntPtr hwnd, bool altTab);

    public MainViewModel ViewModel { get; }

    public MainWindow()
    {
        InitializeComponent();
        _layoutSettings = LayoutSettings.Load();
        _layoutSaveTimer = new System.Windows.Threading.DispatcherTimer
        {
            Interval = TimeSpan.FromMilliseconds(400)
        };
        _layoutSaveTimer.Tick += (_, _) =>
        {
            _layoutSaveTimer.Stop();
            SaveWindowLayout();
        };
        // Selbstheilung fuer den dokumentierten Fehlerzustand "WPF meldet Normal, das Fenster haengt
        // aber nativ minimiert bei -16000/-16000": kurz nach dem Zustandswechsel nachsehen und rein
        // nativ nachrestaurieren. Bewusst OHNE Vordergrund-Erzwingung — nur der haengende
        // Minimierungszustand wird geloest, die Aktivierung bleibt Sache von Windows.
        _minimizeStuckTimer = new System.Windows.Threading.DispatcherTimer
        {
            Interval = TimeSpan.FromMilliseconds(MinimizeStuckCheckMs)
        };
        _minimizeStuckTimer.Tick += (_, _) =>
        {
            _minimizeStuckTimer.Stop();
            RepairStuckNativeMinimize();
        };
        RestoreWindowLayout();
        ModelColumn.Width = new GridLength(_layoutSettings.ModelPaneWidth);
        EffortColumn.Width = new GridLength(_layoutSettings.EffortPaneWidth);
        // Beide Zeilen zusammen setzen: nur eine anzufassen verschiebt das Verhaeltnis, weil die
        // andere ihren alten Sternwert behaelt.
        if (!double.IsNaN(_layoutSettings.ProviderRowShare))
        {
            ProviderRow.Height = new GridLength(_layoutSettings.ProviderRowShare, GridUnitType.Star);
            ProfileRow.Height = new GridLength(1 - _layoutSettings.ProviderRowShare, GridUnitType.Star);
        }
        ViewModel = new MainViewModel();
        DataContext = ViewModel;

        UpdateThemeButton(ThemeManager.Current);
        ThemeManager.ThemeChanged += OnThemeChanged;
        Closed += (_, _) =>
        {
            ViewModel.StopBackgroundUpdates();
            _layoutSaveTimer.Stop();
            _minimizeStuckTimer.Stop();
            SaveWindowLayout();
            ThemeManager.ThemeChanged -= OnThemeChanged;
        };

        SourceInitialized += (_, _) =>
        {
            var hwnd = new WindowInteropHelper(this).Handle;
            HwndSource.FromHwnd(hwnd)?.AddHook(WndProc);
            ApplyWindowTheme();
        };
        ProviderGrid.AddHandler(Thumb.DragStartedEvent, new DragStartedEventHandler(ProviderColumnResizeStarted), true);
        ProviderGrid.AddHandler(Thumb.DragDeltaEvent, new DragDeltaEventHandler(ProviderColumnResizeDelta), true);
        ProviderGrid.AddHandler(Thumb.DragCompletedEvent, new DragCompletedEventHandler(ProviderColumnResizeCompleted), true);
        LocationChanged += (_, _) => QueueSaveWindowLayout();
        SizeChanged += (_, _) => QueueSaveWindowLayout();
        StateChanged += (_, _) =>
        {
            MaxBtn.Content = WindowState == WindowState.Maximized ? "❐" : "▢";
            ApplyWindowTheme();
            QueueSaveWindowLayout();
            // Kein Vordergrund-Erzwingen aus StateChanged/Activated mehr — nur die reine
            // Selbstheilungs-Kontrolle auf einen nativ haengenden Minimierungszustand.
            if (WindowState != WindowState.Minimized)
            {
                _minimizeStuckTimer.Stop();
                _minimizeStuckTimer.Start();
            }
            else
            {
                _minimizeStuckTimer.Stop();
            }
        };
        // BEWUSST KEIN Activated-Handler, der die Vordergrund-Sequenz nachschiebt:
        // Beim Klick auf den Taskleisten-Button restauriert und aktiviert Windows das Fenster selbst
        // (die Shell ruft ShowWindow/SetForegroundWindow direkt auf — es kommt KEIN WM_SYSCOMMAND/
        // SC_RESTORE an). Ein verzoegerter Aktivierungs-Callback funkt genau in diese laufende
        // Sequenz hinein und koppelt per AttachThreadInput die Eingabewarteschlange an den
        // Shell-Thread (explorer.exe). Loest sich das Paar durch ein Race nicht, laesst sich der
        // Prozess bis zum Neustart nicht mehr in den Vordergrund holen. Die Vordergrund-Sequenz
        // laeuft daher ausschliesslich noch bei echten externen Anforderungen (zweite Instanz,
        // SC_RESTORE aus Systemmenue/Titelleiste), nicht bei jedem gewoehnlichen Fensterwechsel.
    }

    private void QueueBringToTaskbarForeground(string reason)
    {
        var now = DateTime.UtcNow;
        if (now < _foregroundRunningUntilUtc) return;

        _queuedActivationReason = reason;
        if (now < _foregroundQueuedUntilUtc) return;
        _foregroundQueuedUntilUtc = now.AddMilliseconds(ForegroundGuardMs);
        Dispatcher.BeginInvoke(new Action(() =>
        {
            _foregroundQueuedUntilUtc = DateTime.MinValue;
            BringToTaskbarForeground(_queuedActivationReason);
        }), System.Windows.Threading.DispatcherPriority.Normal);
    }

    /// <summary>
    /// Loest den dokumentierten Fehlerzustand, in dem WPF bereits Normal/Maximized meldet, das
    /// Fenster aber nativ minimiert stehen bleibt. Rein nativer Restore ohne Vordergrund-Erzwingung:
    /// die Aktivierung selbst bleibt Sache von Windows.
    /// </summary>
    private void RepairStuckNativeMinimize()
    {
        if (WindowState == WindowState.Minimized) return;
        // Frisch und absichtlich minimiert: das ist kein Fehlerzustand, sondern der Normalfall, bei
        // dem WPFs Zustand nur noch nicht nachgezogen hat. Hier eingreifen hiesse, das Minimieren
        // des Benutzers rueckgaengig zu machen.
        if ((DateTime.UtcNow - _lastNativeMinimizeUtc).TotalMilliseconds < ForegroundGuardMs) return;

        var hwnd = new WindowInteropHelper(this).Handle;
        if (hwnd == IntPtr.Zero || !IsIconic(hwnd)) return;

        ShowWindow(hwnd, SW_RESTORE);
        Logger.Instance.Warn("MainWindow", "RepairStuckNativeMinimize",
            "Fenster war nativ minimiert obwohl WPF einen sichtbaren Zustand meldete; nativ nachrestauriert", new
            {
                windowState = WindowState.ToString(),
                stillMinimized = IsIconic(hwnd),
                hwnd = FormatHandle(hwnd)
            });
    }

    public void BringToForegroundFromExternalActivation()
    {
        Logger.Instance.Info("MainWindow", "BringToForegroundFromExternalActivation", "Aktivierung durch zweite Instanz empfangen");
        QueueBringToTaskbarForeground("external-activation");
    }

    private void BringToTaskbarForeground(string reason)
    {
        var now = DateTime.UtcNow;
        if (now < _foregroundRunningUntilUtc) return;

        // Die Sperre laeuft auch dann von selbst ab, wenn dieser Aufruf nie zum finally kommt.
        _foregroundRunningUntilUtc = now.AddMilliseconds(ForegroundGuardMs);
        try
        {
            if (!IsVisible) Show();

            var hwnd = new WindowInteropHelper(this).Handle;
            if (hwnd == IntPtr.Zero) return;

            var foregroundBefore = GetForegroundWindow();
            var wasMinimized = IsIconic(hwnd);
            // Restore ausschliesslich nativ per SW_RESTORE: WindowState.Normal wuerde einen
            // maximierten Launcher auf Normalgroesse zuruecksetzen.
            if (wasMinimized) ShowWindow(hwnd, SW_RESTORE);
            var restored = !IsIconic(hwnd);

            SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW);
            var activated = TryActivateWithForegroundThread(hwnd);
            if (!activated)
            {
                Activate();
                activated = GetForegroundWindow() == hwnd;
            }
            if (!activated) activated = TryForceForeground(hwnd);

            var foregroundAfter = GetForegroundWindow();
            // Immer protokollieren: dieser Pfad laeuft nur noch bei echten externen Anforderungen und
            // ist damit selten. Eine Bedingung davor wuerde genau die Faelle verschweigen, in denen
            // die Aktivierung still scheitert.
            Logger.Instance.Info("MainWindow", "BringToTaskbarForeground", "Fensteraktivierung ausgeführt", new
            {
                reason,
                wasMinimized,
                restored,
                activated,
                foregroundBefore = FormatHandle(foregroundBefore),
                foregroundAfter = FormatHandle(foregroundAfter),
                hwnd = FormatHandle(hwnd)
            });
            if (activated) return;

            FlashTaskbar(hwnd);
            Logger.Instance.Warn("MainWindow", "BringToTaskbarForeground", "Vordergrundaktivierung nicht bestätigt; Taskleisten-Blinken als letzter Fallback", new
            {
                reason,
                restored,
                foreground = FormatHandle(foregroundAfter),
                hwnd = FormatHandle(hwnd)
            });
        }
        finally
        {
            _foregroundRunningUntilUtc = DateTime.MinValue;
        }
    }

    private static bool TryActivateWithForegroundThread(IntPtr hwnd)
    {
        var foreground = GetForegroundWindow();
        if (foreground == hwnd) return true;

        var currentThread = GetCurrentThreadId();
        var foregroundThread = foreground == IntPtr.Zero ? 0 : GetWindowThreadProcessId(foreground, out _);
        var attached = false;

        try
        {
            if (foregroundThread != 0 && foregroundThread != currentThread)
                attached = AttachThreadInput(currentThread, foregroundThread, true);

            BringWindowToTop(hwnd);
            SetForegroundWindow(hwnd);
            SetFocus(hwnd);
            return GetForegroundWindow() == hwnd;
        }
        finally
        {
            // Ein nicht geloestes AttachThreadInput koppelt die Eingabewarteschlange dauerhaft an einen
            // fremden Thread — typisch explorer.exe hinter der Taskleiste. Danach laesst sich der
            // Prozess bis zum Neustart nicht mehr aktivieren. Das Loesen wird deshalb geprueft und ein
            // Fehlschlag protokolliert, statt still durchzulaufen.
            if (attached)
            {
                var detached = AttachThreadInput(currentThread, foregroundThread, false);
                var detachError = detached ? 0 : Marshal.GetLastWin32Error();
                if (!detached)
                {
                    Logger.Instance.Warn("MainWindow", "TryActivateWithForegroundThread",
                        "AttachThreadInput konnte nicht gelöst werden; Eingabekopplung bleibt bestehen", new
                        {
                            currentThread,
                            foregroundThread,
                            error = detachError
                        });
                }
            }
        }
    }

    private bool TryForceForeground(IntPtr hwnd)
    {
        var restoreZOrder = Topmost ? HWND_TOPMOST : HWND_NOTOPMOST;
        try
        {
            SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW);
            BringWindowToTop(hwnd);
            SetForegroundWindow(hwnd);
            SwitchToThisWindow(hwnd, true);
            return GetForegroundWindow() == hwnd;
        }
        finally
        {
            SetWindowPos(hwnd, restoreZOrder, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW);
        }
    }

    private static string FormatHandle(IntPtr hwnd) => $"0x{hwnd.ToInt64():X}";

    private static void FlashTaskbar(IntPtr hwnd)
    {
        var info = new FLASHWINFO
        {
            cbSize = (uint)Marshal.SizeOf<FLASHWINFO>(),
            hwnd = hwnd,
            dwFlags = FLASHW_ALL | FLASHW_TIMERNOFG,
            uCount = 3,
            dwTimeout = 0
        };
        FlashWindowEx(ref info);
    }

    // Layout-Speicherung wird gebündelt: LocationChanged/SizeChanged feuern beim Ziehen/Resizen
    // sehr häufig — ohne Debounce würde jedes Pixel eine synchrone JSON-Schreiboperation auslösen.
    private void QueueSaveWindowLayout()
    {
        _layoutSaveTimer.Stop();
        _layoutSaveTimer.Start();
    }

    private void RestoreWindowLayout()
    {
        // NaN-Sentinel = noch nie gespeichert. Echte negative Koordinaten (linker/oberer Monitor)
        // sind gültig und werden weiter unten geclamped.
        if (double.IsNaN(_layoutSettings.WindowLeft) || double.IsNaN(_layoutSettings.WindowTop)) return;

        WindowStartupLocation = WindowStartupLocation.Manual;
        Width = Math.Max(_layoutSettings.WindowWidth, MinWidth);
        Height = Math.Max(_layoutSettings.WindowHeight, MinHeight);
        // Clamp-Grenzen absichern: bei Fenstern, die breiter/höher als der virtuelle Bildschirm sind
        // (z.B. nach Auflösungs-/Monitorwechsel), wäre max < min und Math.Clamp würfe ArgumentException.
        var minLeft = SystemParameters.VirtualScreenLeft;
        var minTop = SystemParameters.VirtualScreenTop;
        var maxLeft = Math.Max(minLeft, minLeft + SystemParameters.VirtualScreenWidth - Width);
        var maxTop = Math.Max(minTop, minTop + SystemParameters.VirtualScreenHeight - Height);
        Left = Math.Clamp(_layoutSettings.WindowLeft, minLeft, maxLeft);
        Top = Math.Clamp(_layoutSettings.WindowTop, minTop, maxTop);
        // WindowState explizit setzen: der XAML-Default ist Maximized; ohne das Zurücksetzen auf Normal
        // würde ein normal geschlossenes Fenster beim nächsten Start fälschlich maximiert öffnen.
        WindowState = _layoutSettings.WindowState == "Maximized" ? WindowState.Maximized : WindowState.Normal;
    }

    private void SaveWindowLayout()
    {
        if (WindowState == WindowState.Minimized) return;

        var bounds = WindowState == WindowState.Maximized ? RestoreBounds : new Rect(Left, Top, Width, Height);
        if (bounds.Width < MinWidth || bounds.Height < MinHeight) return;

        _layoutSettings.WindowLeft = bounds.Left;
        _layoutSettings.WindowTop = bounds.Top;
        _layoutSettings.WindowWidth = bounds.Width;
        _layoutSettings.WindowHeight = bounds.Height;
        _layoutSettings.WindowState = WindowState == WindowState.Maximized ? "Maximized" : "Normal";
        _layoutSettings.Save();
    }

    private void OnThemeChanged(ThemeManager.AppTheme theme)
    {
        ApplyWindowTheme();
        UpdateThemeButton(theme);
        _layoutSettings.Theme = theme.ToString();
        _layoutSettings.Save();
    }

    private void UpdateThemeButton(ThemeManager.AppTheme theme)
    {
        // Button zeigt das Ziel-Design: Sonne im Dunkelmodus, Mond im Tagmodus.
        ThemeBtn.Content = theme == ThemeManager.AppTheme.Dark ? "\uE706" : "\uE708";
        ThemeBtn.ToolTip = theme == ThemeManager.AppTheme.Dark ? "Zum Tagmodus wechseln" : "Zum Nachtmodus wechseln";
    }

    private void ThemeBtn_Click(object sender, RoutedEventArgs e) => ThemeManager.Toggle();

    private void Settings_Click(object sender, RoutedEventArgs e)
    {
        var settings = new SettingsWindow(ViewModel) { Owner = this };
        settings.ResearchCompleted += ViewModel.ApplyResearchResult;
        settings.ShowDialog();
        settings.ResearchCompleted -= ViewModel.ApplyResearchResult;
    }

    private void ApplyWindowTheme()
    {
        var hwnd = new System.Windows.Interop.WindowInteropHelper(this).Handle;
        if (hwnd == IntPtr.Zero) return;
        int dark = ThemeManager.Current == ThemeManager.AppTheme.Dark ? 1 : 0;
        DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, ref dark, sizeof(int));
        int cornerPreference = WindowState == WindowState.Maximized ? DWMWCP_DONOTROUND : DWMWCP_ROUND;
        DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, ref cornerPreference, sizeof(int));

        var windowChrome = System.Windows.Shell.WindowChrome.GetWindowChrome(this);
        if (windowChrome != null)
            windowChrome.CornerRadius = WindowState == WindowState.Maximized ? new CornerRadius(0) : new CornerRadius(8);

        WindowFrameBorder.CornerRadius = WindowState == WindowState.Maximized ? new CornerRadius(0) : new CornerRadius(8);
        FooterBorder.CornerRadius = WindowState == WindowState.Maximized ? new CornerRadius(0) : new CornerRadius(0, 0, 8, 8);
    }

    private void TitleBar_MouseDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ChangedButton == MouseButton.Left)
        {
            if (e.ClickCount >= 2)
            {
                WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
            }
            else
            {
                DragMove();
            }
        }
    }

    private void MinBtn_Click(object sender, RoutedEventArgs e) => WindowState = WindowState.Minimized;
    private void MaxBtn_Click(object sender, RoutedEventArgs e) => WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
    private void CloseBtn_Click(object sender, RoutedEventArgs e) => Close();

    private IntPtr WndProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
    {
        if (msg == WM_GETMINMAXINFO)
        {
            AdjustMaximizedSize(hwnd, lParam);
            handled = true;
        }
        else if (msg == WM_SIZE && (int)wParam == SIZE_MINIMIZED)
        {
            // Native Wahrheit, synchron zum Minimieren zugestellt — im Gegensatz zu WPFs StateChanged,
            // das erst spaeter feuert. Stoppt die Selbstheilung, bevor sie den Zustand missdeutet.
            _lastNativeMinimizeUtc = DateTime.UtcNow;
            _minimizeStuckTimer.Stop();
        }
        else if (msg == WM_SYSCOMMAND && ((int)wParam & 0xFFF0) == SC_RESTORE)
        {
            QueueBringToTaskbarForeground("system-restore");
        }
        return IntPtr.Zero;
    }

    private static void AdjustMaximizedSize(IntPtr hwnd, IntPtr lParam)
    {
        var monitor = MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST);
        if (monitor == IntPtr.Zero) return;

        var info = new MONITORINFO { cbSize = Marshal.SizeOf<MONITORINFO>() };
        if (!GetMonitorInfo(monitor, ref info)) return;

        var mmi = Marshal.PtrToStructure<MINMAXINFO>(lParam);
        var work = info.rcWork;
        var monitorRect = info.rcMonitor;

        mmi.ptMaxPosition.x = Math.Abs(work.left - monitorRect.left);
        mmi.ptMaxPosition.y = Math.Abs(work.top - monitorRect.top);
        mmi.ptMaxSize.x = Math.Abs(work.right - work.left);
        mmi.ptMaxSize.y = Math.Abs(work.bottom - work.top);

        Marshal.StructureToPtr(mmi, lParam, true);
    }

    private void BrowseWorkDir_Click(object sender, RoutedEventArgs e) => ViewModel.BrowseWorkDirCommand.Execute(null);

    private void ModelSplitter_DragCompleted(object sender, DragCompletedEventArgs e)
    {
        _layoutSettings.ModelPaneWidth = ModelColumn.ActualWidth;
        _layoutSettings.Save();
    }

    private void EffortSplitter_DragCompleted(object sender, DragCompletedEventArgs e)
    {
        _layoutSettings.EffortPaneWidth = EffortColumn.ActualWidth;
        _layoutSettings.Save();
    }

    private void ProfileSplitter_DragCompleted(object sender, DragCompletedEventArgs e)
    {
        var total = ProviderRow.ActualHeight + ProfileRow.ActualHeight;
        if (total <= 0) return;

        _layoutSettings.ProviderRowShare = ProviderRow.ActualHeight / total;
        _layoutSettings.Save();
    }

    private void ModelScrollViewer_PreviewMouseWheel(object sender, MouseWheelEventArgs e)
    {
        ModelScrollViewer.ScrollToVerticalOffset(ModelScrollViewer.VerticalOffset - e.Delta);
        e.Handled = true;
    }

    private void ProviderColumnResizeStarted(object sender, DragStartedEventArgs e)
    {
        if (e.OriginalSource is not Thumb { Name: "ProviderColumnResizeThumb" } thumb) return;

        var header = FindAncestor<System.Windows.Controls.Primitives.DataGridColumnHeader>(thumb);
        if (header?.Column == null) return;

        var index = ProviderGrid.Columns.IndexOf(header.Column);
        if (index < 0 || index >= ProviderGrid.Columns.Count - 1) return;

        _providerResizeColumnIndex = index;
        _providerResizeTotalDelta = 0;
        _providerResizeStartWidths = ProviderGrid.Columns
            .Select(column => Math.Max(column.ActualWidth, Math.Max(column.MinWidth, 60)))
            .ToArray();
        for (var i = 0; i < ProviderGrid.Columns.Count; i++)
        {
            ProviderGrid.Columns[i].Width = new System.Windows.Controls.DataGridLength(_providerResizeStartWidths[i]);
        }
        e.Handled = true;
    }

    private void ProviderColumnResizeDelta(object sender, DragDeltaEventArgs e)
    {
        if (e.OriginalSource is not Thumb { Name: "ProviderColumnResizeThumb" }) return;
        if (_providerResizeStartWidths == null || _providerResizeColumnIndex < 0 || _providerResizeColumnIndex >= ProviderGrid.Columns.Count - 1) return;

        var left = ProviderGrid.Columns[_providerResizeColumnIndex];
        var right = ProviderGrid.Columns[_providerResizeColumnIndex + 1];
        var minLeft = Math.Max(left.MinWidth, 60);
        var minRight = Math.Max(right.MinWidth, 60);
        var startLeft = _providerResizeStartWidths[_providerResizeColumnIndex];
        var startRight = _providerResizeStartWidths[_providerResizeColumnIndex + 1];
        var pairWidth = startLeft + startRight;
        _providerResizeTotalDelta += e.HorizontalChange;
        var newLeft = Math.Clamp(startLeft + _providerResizeTotalDelta, minLeft, pairWidth - minRight);

        for (var i = 0; i < ProviderGrid.Columns.Count; i++)
        {
            ProviderGrid.Columns[i].Width = new System.Windows.Controls.DataGridLength(_providerResizeStartWidths[i]);
        }
        left.Width = new System.Windows.Controls.DataGridLength(newLeft);
        right.Width = new System.Windows.Controls.DataGridLength(pairWidth - newLeft);
        e.Handled = true;
    }

    private void ProviderColumnResizeCompleted(object sender, DragCompletedEventArgs e)
    {
        if (e.OriginalSource is not Thumb { Name: "ProviderColumnResizeThumb" }) return;
        _providerResizeColumnIndex = -1;
        _providerResizeStartWidths = null;
        _providerResizeTotalDelta = 0;
    }

    private void ProviderColumnResizeThumb_Loaded(object sender, RoutedEventArgs e)
    {
        if (sender is not Thumb thumb) return;

        var header = FindAncestor<System.Windows.Controls.Primitives.DataGridColumnHeader>(thumb);
        if (header?.Column == null) return;

        var isLastColumn = ProviderGrid.Columns.IndexOf(header.Column) == ProviderGrid.Columns.Count - 1;
        thumb.Visibility = isLastColumn ? Visibility.Collapsed : Visibility.Visible;
        header.BorderThickness = isLastColumn ? new Thickness(0, 0, 0, 1) : new Thickness(0, 0, 1, 1);
        header.SeparatorVisibility = isLastColumn ? Visibility.Collapsed : Visibility.Visible;
    }

    // ---- Drag & Drop für Modellgruppen und Modelle ----
    private Point _dragStartPoint;
    private int _dragSourceGroupIndex = -1;
    private OpenLauncher.Models.ModelGroupEntry? _dragSourceGroup;
    private bool _groupDragStarted;
    private int _dragSourceIndex = -1;

    private void ModelGroupHeader_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        _dragStartPoint = e.GetPosition(this);
        _dragSourceGroup = (sender as FrameworkElement)?.DataContext as OpenLauncher.Models.ModelGroupEntry;
        _dragSourceGroupIndex = _dragSourceGroup == null ? -1 : ViewModel.ModelGroups.IndexOf(_dragSourceGroup);
        _groupDragStarted = false;
        e.Handled = true;
    }

    private void ModelGroupHeader_PreviewMouseMove(object sender, MouseEventArgs e)
    {
        if (e.LeftButton != MouseButtonState.Pressed || _dragSourceGroup == null || _dragSourceGroupIndex < 0) return;

        var current = e.GetPosition(this);
        if (Math.Abs(current.X - _dragStartPoint.X) < SystemParameters.MinimumHorizontalDragDistance &&
            Math.Abs(current.Y - _dragStartPoint.Y) < SystemParameters.MinimumVerticalDragDistance)
            return;

        _groupDragStarted = true;
        DragDrop.DoDragDrop(sender as DependencyObject ?? this, _dragSourceGroup, DragDropEffects.Move);
        _dragSourceGroup = null;
        _dragSourceGroupIndex = -1;
        e.Handled = true;
    }

    private void ModelGroupHeader_MouseLeftButtonUp(object sender, MouseButtonEventArgs e)
    {
        if (!_groupDragStarted && (sender as FrameworkElement)?.DataContext is OpenLauncher.Models.ModelGroupEntry group)
            ViewModel.ToggleGroupCommand.Execute(group);
        _dragSourceGroup = null;
        _dragSourceGroupIndex = -1;
        _groupDragStarted = false;
        e.Handled = true;
    }

    private void ModelGroup_DragEnter(object sender, DragEventArgs e) => SetDragEffects(e);
    private void ModelGroup_DragOver(object sender, DragEventArgs e) => SetDragEffects(e);

    private void ModelGroup_Drop(object sender, DragEventArgs e)
    {
        var targetGroup = (sender as FrameworkElement)?.DataContext as OpenLauncher.Models.ModelGroupEntry;
        if (targetGroup == null) return;

        if (e.Data.GetDataPresent(typeof(OpenLauncher.Models.ModelGroupEntry)) && _dragSourceGroupIndex >= 0)
        {
            var targetIndex = ViewModel.ModelGroups.IndexOf(targetGroup);
            ViewModel.MoveGroup(_dragSourceGroupIndex, targetIndex);
            _dragSourceGroup = null;
            _dragSourceGroupIndex = -1;
            e.Handled = true;
            return;
        }

        if (e.Data.GetDataPresent(typeof(OpenLauncher.Models.ModelEntry)) && _dragSourceGroup != null && _dragSourceIndex >= 0)
        {
            ViewModel.MoveModel(_dragSourceGroup, _dragSourceIndex, targetGroup, targetGroup.Models.Count);
            _dragSourceGroup = null;
            _dragSourceIndex = -1;
            e.Handled = true;
            return;
        }

        e.Handled = true;
    }

    private void ModelList_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        var lb = sender as System.Windows.Controls.ListBox;
        if (lb == null) return;
        if (FindAncestor<System.Windows.Controls.Button>(e.OriginalSource as DependencyObject) != null)
        {
            _dragSourceGroup = null;
            _dragSourceIndex = -1;
            return;
        }
        _dragStartPoint = e.GetPosition(lb);
        _dragSourceGroup = lb.DataContext as OpenLauncher.Models.ModelGroupEntry;
        _dragSourceIndex = IndexFromOriginalSource(lb, e.OriginalSource);
        if (_dragSourceGroup != null && _dragSourceIndex >= 0 && _dragSourceIndex < _dragSourceGroup.Models.Count)
            ViewModel.SelectedModel = _dragSourceGroup.Models[_dragSourceIndex];
    }

    private void ModelList_PreviewMouseMove(object sender, MouseEventArgs e)
    {
        var lb = sender as System.Windows.Controls.ListBox;
        if (lb == null || _dragSourceGroup == null) return;
        // Obergrenze mitprüfen: ein asynchroner Start-Sync (SyncOpenRouterFreeModels) kann die
        // Modell-Liste der OpenRouterFree-Gruppe zwischen MouseDown und diesem Move verkleinert haben —
        // ohne diese Prüfung würde der indizierte Zugriff unten mit ArgumentOutOfRangeException abstürzen.
        if (e.LeftButton != MouseButtonState.Pressed || _dragSourceIndex < 0 || _dragSourceIndex >= _dragSourceGroup.Models.Count) return;

        var current = e.GetPosition(lb);
        if (Math.Abs(current.X - _dragStartPoint.X) < SystemParameters.MinimumHorizontalDragDistance &&
            Math.Abs(current.Y - _dragStartPoint.Y) < SystemParameters.MinimumVerticalDragDistance)
            return;

        var item = _dragSourceGroup.Models[_dragSourceIndex];
        try
        {
            DragDrop.DoDragDrop(lb, item, DragDropEffects.Move);
        }
        finally
        {
            _dragSourceIndex = -1;
        }
    }

    private void ModelList_DragEnter(object sender, DragEventArgs e) => SetDragEffects(e);
    private void ModelList_DragOver(object sender, DragEventArgs e) => SetDragEffects(e);

    private void ModelList_Drop(object sender, DragEventArgs e)
    {
        // Nur echte Modell-Drags verarbeiten. Ohne diese Typprüfung würde ein Gruppen-Drag, das über
        // einer Modell-Liste losgelassen wird, den noch vom letzten Modell-Klick stammenden
        // _dragSourceIndex verwenden und fälschlich ein Modell der falschen Gruppe verschieben.
        if (!e.Data.GetDataPresent(typeof(OpenLauncher.Models.ModelEntry))) return;
        if (_dragSourceGroup == null || _dragSourceIndex < 0) return;
        var lb = sender as System.Windows.Controls.ListBox;
        var targetGroup = lb?.DataContext as OpenLauncher.Models.ModelGroupEntry;
        if (lb == null || targetGroup == null) return;
        var pos = e.GetPosition(lb);
        var targetIdx = IndexFromPoint(lb, pos);
        if (targetIdx < 0) targetIdx = targetGroup.Models.Count;
        ViewModel.MoveModel(_dragSourceGroup, _dragSourceIndex, targetGroup, targetIdx);
        _dragSourceGroup = null;
        _dragSourceIndex = -1;
        e.Handled = true;
    }

    private static void SetDragEffects(DragEventArgs e)
    {
        e.Effects = e.Data.GetDataPresent(typeof(OpenLauncher.Models.ModelEntry)) ||
                    e.Data.GetDataPresent(typeof(OpenLauncher.Models.ModelGroupEntry))
            ? DragDropEffects.Move
            : DragDropEffects.None;
        e.Handled = true;
    }

    private static int IndexFromOriginalSource(System.Windows.Controls.ListBox lb, object source)
    {
        var item = System.Windows.Controls.ItemsControl.ContainerFromElement(lb, source as DependencyObject) as System.Windows.Controls.ListBoxItem;
        return item == null ? -1 : lb.ItemContainerGenerator.IndexFromContainer(item);
    }

    private static int IndexFromPoint(System.Windows.Controls.ListBox lb, Point p)
    {
        for (int i = 0; i < lb.Items.Count; i++)
        {
            var container = lb.ItemContainerGenerator.ContainerFromIndex(i) as System.Windows.Controls.ListBoxItem;
            if (container != null)
            {
                var rect = VisualRect(container, lb);
                if (rect.Contains(p)) return i;
            }
        }
        return -1;
    }

    private static Rect VisualRect(System.Windows.FrameworkElement el, System.Windows.Media.Visual ancestor)
    {
        var t = el.TransformToAncestor(ancestor);
        var r = new Rect(el.RenderSize);
        return t.TransformBounds(r);
    }

    private static T? FindAncestor<T>(DependencyObject? current) where T : DependencyObject
    {
        while (current != null)
        {
            if (current is T typed) return typed;
            current = System.Windows.Media.VisualTreeHelper.GetParent(current);
        }
        return null;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct POINT
    {
        public int x;
        public int y;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MINMAXINFO
    {
        public POINT ptReserved;
        public POINT ptMaxSize;
        public POINT ptMaxPosition;
        public POINT ptMinTrackSize;
        public POINT ptMaxTrackSize;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct RECT
    {
        public int left;
        public int top;
        public int right;
        public int bottom;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MONITORINFO
    {
        public int cbSize;
        public RECT rcMonitor;
        public RECT rcWork;
        public int dwFlags;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct FLASHWINFO
    {
        public uint cbSize;
        public IntPtr hwnd;
        public uint dwFlags;
        public uint uCount;
        public uint dwTimeout;
    }
}

public sealed class ReferenceEqualsConverter : IMultiValueConverter
{
    public object Convert(object[] values, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        => values.Length >= 2 && ReferenceEquals(values[0], values[1]);

    public object[] ConvertBack(object value, Type[] targetTypes, object parameter, System.Globalization.CultureInfo culture)
        => targetTypes.Select(_ => Binding.DoNothing).ToArray();
}
