using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Interop;
using OpenCodeLauncher.Services;
using OpenCodeLauncher.ViewModels;

namespace OpenCodeLauncher;

public partial class MainWindow : Window
{
    private const int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private const int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private const int DWMWCP_ROUND = 2;
    private const int WM_GETMINMAXINFO = 0x0024;
    private const int MONITOR_DEFAULTTONEAREST = 2;
    private readonly LayoutSettings _layoutSettings;
    private int _providerResizeColumnIndex = -1;
    private double[]? _providerResizeStartWidths;
    private double _providerResizeTotalDelta;

    [DllImport("dwmapi.dll")]
    private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int value, int size);

    [DllImport("user32.dll")]
    private static extern IntPtr MonitorFromWindow(IntPtr hwnd, int flags);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool GetMonitorInfo(IntPtr hMonitor, ref MONITORINFO lpmi);

    public MainViewModel ViewModel { get; }

    public MainWindow()
    {
        InitializeComponent();
        _layoutSettings = LayoutSettings.Load();
        RestoreWindowLayout();
        ModelColumn.Width = new GridLength(_layoutSettings.ModelPaneWidth);
        ViewModel = new MainViewModel();
        DataContext = ViewModel;

        UpdateThemeButton(ThemeManager.Current);
        ThemeManager.ThemeChanged += OnThemeChanged;
        Closed += (_, _) =>
        {
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
        LocationChanged += (_, _) => SaveWindowLayout();
        SizeChanged += (_, _) => SaveWindowLayout();
        StateChanged += (_, _) =>
        {
            MaxBtn.Content = WindowState == WindowState.Maximized ? "❐" : "▢";
            SaveWindowLayout();
        };
        ContentRendered += (_, _) => Title = $"OpenCode Launcher — {ViewModel.Version}";
    }

    private void RestoreWindowLayout()
    {
        if (_layoutSettings.WindowLeft < 0 || _layoutSettings.WindowTop < 0) return;

        WindowStartupLocation = WindowStartupLocation.Manual;
        Width = Math.Max(_layoutSettings.WindowWidth, MinWidth);
        Height = Math.Max(_layoutSettings.WindowHeight, MinHeight);
        Left = Math.Clamp(_layoutSettings.WindowLeft, SystemParameters.VirtualScreenLeft, SystemParameters.VirtualScreenLeft + SystemParameters.VirtualScreenWidth - Width);
        Top = Math.Clamp(_layoutSettings.WindowTop, SystemParameters.VirtualScreenTop, SystemParameters.VirtualScreenTop + SystemParameters.VirtualScreenHeight - Height);
        if (_layoutSettings.WindowState == "Maximized") WindowState = WindowState.Maximized;
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

    private void ApplyWindowTheme()
    {
        var hwnd = new System.Windows.Interop.WindowInteropHelper(this).Handle;
        if (hwnd == IntPtr.Zero) return;
        int dark = ThemeManager.Current == ThemeManager.AppTheme.Dark ? 1 : 0;
        DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, ref dark, sizeof(int));
        int cornerPreference = DWMWCP_ROUND;
        DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, ref cornerPreference, sizeof(int));
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
    }

    // ---- Drag & Drop für Modellgruppen und Modelle ----
    private Point _dragStartPoint;
    private int _dragSourceGroupIndex = -1;
    private OpenCodeLauncher.Models.ModelGroupEntry? _dragSourceGroup;
    private bool _groupDragStarted;
    private int _dragSourceIndex = -1;

    private void ModelGroupHeader_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        _dragStartPoint = e.GetPosition(this);
        _dragSourceGroup = (sender as FrameworkElement)?.DataContext as OpenCodeLauncher.Models.ModelGroupEntry;
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
        if (!_groupDragStarted && (sender as FrameworkElement)?.DataContext is OpenCodeLauncher.Models.ModelGroupEntry group)
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
        var targetGroup = (sender as FrameworkElement)?.DataContext as OpenCodeLauncher.Models.ModelGroupEntry;
        if (targetGroup == null) return;

        if (e.Data.GetDataPresent(typeof(OpenCodeLauncher.Models.ModelGroupEntry)) && _dragSourceGroupIndex >= 0)
        {
            var targetIndex = ViewModel.ModelGroups.IndexOf(targetGroup);
            ViewModel.MoveGroup(_dragSourceGroupIndex, targetIndex);
            _dragSourceGroup = null;
            _dragSourceGroupIndex = -1;
            e.Handled = true;
            return;
        }

        e.Handled = true;
    }

    private void ModelList_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        var lb = sender as System.Windows.Controls.ListBox;
        if (lb == null) return;
        _dragStartPoint = e.GetPosition(lb);
        _dragSourceGroup = lb.DataContext as OpenCodeLauncher.Models.ModelGroupEntry;
        _dragSourceIndex = IndexFromOriginalSource(lb, e.OriginalSource);
    }

    private void ModelList_PreviewMouseMove(object sender, MouseEventArgs e)
    {
        var lb = sender as System.Windows.Controls.ListBox;
        if (lb == null || _dragSourceGroup == null) return;
        if (e.LeftButton != MouseButtonState.Pressed || _dragSourceIndex < 0) return;

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
        if (_dragSourceGroup == null || _dragSourceIndex < 0) return;
        var lb = sender as System.Windows.Controls.ListBox;
        var targetGroup = lb?.DataContext as OpenCodeLauncher.Models.ModelGroupEntry;
        if (lb == null || targetGroup == null) return;
        if (!ReferenceEquals(_dragSourceGroup, targetGroup))
        {
            _dragSourceGroup = null;
            _dragSourceIndex = -1;
            e.Handled = true;
            return;
        }
        var pos = e.GetPosition(lb);
        var targetIdx = IndexFromPoint(lb, pos);
        if (targetIdx < 0) targetIdx = targetGroup.Models.Count;
        ViewModel.MoveModel(_dragSourceGroup, _dragSourceIndex, targetIdx);
        _dragSourceGroup = null;
        _dragSourceIndex = -1;
        e.Handled = true;
    }

    private static void SetDragEffects(DragEventArgs e)
    {
        e.Effects = e.Data.GetDataPresent(typeof(OpenCodeLauncher.Models.ModelEntry)) ||
                    e.Data.GetDataPresent(typeof(OpenCodeLauncher.Models.ModelGroupEntry))
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
}
