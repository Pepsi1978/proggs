using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Input;
using OpenCodeLauncher.ViewModels;

namespace OpenCodeLauncher;

public partial class MainWindow : Window
{
    // Mica/Acrylic über DWM (Windows 11). Kurzcheck §W7/C9: WindowChrome & DWM-Material.
    // Wir nutzen nur den Mica-Backdrop + runde Ecken über DWM (kein AllowsTransparency=True,
    // das WebView/Airspace-Probleme macht). Hintergrund wird auf Transparent gesetzt.
    private const int DWMWA_SYSTEMBACKDROP_TYPE = 38;
    private const int DWMSBT_MAINWINDOW = 2;   // Mica
    private const int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;

    [DllImport("dwmapi.dll")]
    private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int value, int size);

    public MainViewModel ViewModel { get; }

    public MainWindow()
    {
        InitializeComponent();
        ViewModel = new MainViewModel();
        DataContext = ViewModel;

        Loaded += (_, _) => ApplyMica();
        ContentRendered += (_, _) => Title = $"OpenCode Launcher — {ViewModel.Version}";
    }

    private void ApplyMica()
    {
        var hwnd = new System.Windows.Interop.WindowInteropHelper(this).Handle;
        if (hwnd == IntPtr.Zero) return;
        int dark = 1;
        DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, ref dark, sizeof(int));
        int backdrop = DWMSBT_MAINWINDOW;
        DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, ref backdrop, sizeof(int));
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
    private void CloseBtn_Click(object sender, RoutedEventArgs e) => Close();

    private void BrowseWorkDir_Click(object sender, RoutedEventArgs e) => ViewModel.BrowseWorkDirCommand.Execute(null);

    // ---- Drag & Drop für die Modell-Liste (Reihenfolge ändern) ----
    private bool _isDragging;
    private int _dragSourceIndex = -1;

    private void ModelList_PreviewMouseMove(object sender, MouseEventArgs e)
    {
        if (e.LeftButton != MouseButtonState.Pressed || _isDragging) return;
        var lb = ModelList;
        var idx = IndexFromPoint(lb, e.GetPosition(lb));
        if (idx < 0) return;
        _dragSourceIndex = idx;
        _isDragging = true;
        var item = ViewModel.Models[idx];
        try
        {
            DragDrop.DoDragDrop(lb, item, DragDropEffects.Move);
        }
        finally
        {
            _isDragging = false;
            _dragSourceIndex = -1;
        }
    }

    private void ModelList_DragEnter(object sender, DragEventArgs e)
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

    private static int IndexFromPoint(System.Windows.Controls.ListBox lb, Point p)
    {
        for (int i = 0; i < lb.Items.Count; i++)
        {
            var container = lb.ItemContainerGenerator.ContainerFromIndex(i) as System.Windows.Controls.ListBoxItem;
            if (container != null)
            {
                var rect = VisualRect(container);
                if (rect.Contains(p)) return i;
            }
        }
        return -1;
    }

    private static Rect VisualRect(System.Windows.FrameworkElement el)
    {
        var t = el.TransformToAncestor(el.Parent as System.Windows.Media.Visual ?? el);
        var r = new Rect(el.RenderSize);
        return t.TransformBounds(r);
    }
}
