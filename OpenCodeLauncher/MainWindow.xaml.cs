using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using OpenCodeLauncher.ViewModels;

namespace OpenCodeLauncher;

public partial class MainWindow : Window
{
    private const int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private const int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private const int DWMWCP_ROUND = 2;
    private const int WM_GETMINMAXINFO = 0x0024;
    private const int MONITOR_DEFAULTTONEAREST = 2;

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
        ViewModel = new MainViewModel();
        DataContext = ViewModel;

        SourceInitialized += (_, _) =>
        {
            var hwnd = new WindowInteropHelper(this).Handle;
            HwndSource.FromHwnd(hwnd)?.AddHook(WndProc);
            ApplyWindowTheme();
        };
        StateChanged += (_, _) => MaxBtn.Content = WindowState == WindowState.Maximized ? "❐" : "□";
        ContentRendered += (_, _) => Title = $"OpenCode Launcher — {ViewModel.Version}";
    }

    private void ApplyWindowTheme()
    {
        var hwnd = new System.Windows.Interop.WindowInteropHelper(this).Handle;
        if (hwnd == IntPtr.Zero) return;
        int dark = 1;
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

    // ---- Drag & Drop für die Modell-Liste (Reihenfolge ändern) ----
    private Point _dragStartPoint;
    private int _dragSourceIndex = -1;

    private void ModelList_PreviewMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        _dragStartPoint = e.GetPosition(ModelList);
        _dragSourceIndex = IndexFromOriginalSource(e.OriginalSource);
    }

    private void ModelList_PreviewMouseMove(object sender, MouseEventArgs e)
    {
        if (e.LeftButton != MouseButtonState.Pressed || _dragSourceIndex < 0) return;

        var current = e.GetPosition(ModelList);
        if (Math.Abs(current.X - _dragStartPoint.X) < SystemParameters.MinimumHorizontalDragDistance &&
            Math.Abs(current.Y - _dragStartPoint.Y) < SystemParameters.MinimumVerticalDragDistance)
            return;

        var item = ViewModel.Models[_dragSourceIndex];
        try
        {
            DragDrop.DoDragDrop(ModelList, item, DragDropEffects.Move);
        }
        finally
        {
            _dragSourceIndex = -1;
        }
    }

    private void ModelList_DragEnter(object sender, DragEventArgs e)
    {
        e.Effects = e.Data.GetDataPresent(typeof(OpenCodeLauncher.Models.ModelEntry)) ? DragDropEffects.Move : DragDropEffects.None;
        e.Handled = true;
    }

    private void ModelList_DragOver(object sender, DragEventArgs e)
    {
        e.Effects = e.Data.GetDataPresent(typeof(OpenCodeLauncher.Models.ModelEntry)) ? DragDropEffects.Move : DragDropEffects.None;
        e.Handled = true;
    }

    private void ModelList_Drop(object sender, DragEventArgs e)
    {
        if (_dragSourceIndex < 0) return;
        var lb = ModelList;
        var pos = e.GetPosition(lb);
        var targetIdx = IndexFromPoint(lb, pos);
        if (targetIdx < 0) targetIdx = ViewModel.Models.Count - 1;
        ViewModel.MoveModel(_dragSourceIndex, targetIdx);
        _dragSourceIndex = -1;
        e.Handled = true;
    }

    private int IndexFromOriginalSource(object source)
    {
        var item = System.Windows.Controls.ItemsControl.ContainerFromElement(ModelList, source as DependencyObject) as System.Windows.Controls.ListBoxItem;
        return item == null ? -1 : ModelList.ItemContainerGenerator.IndexFromContainer(item);
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
