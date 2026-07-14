using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Input;
using System.Windows.Interop;
using OpenCodeLauncher.Services;

namespace OpenCodeLauncher;

public partial class ProfileEditorWindow : Window
{
    private const int WM_GETMINMAXINFO = 0x0024;
    private const int MONITOR_DEFAULTTONEAREST = 2;

    [DllImport("user32.dll")]
    private static extern IntPtr MonitorFromWindow(IntPtr hwnd, int flags);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool GetMonitorInfo(IntPtr hMonitor, ref MONITORINFO lpmi);

    public string GlobalText => GlobalEditor.Text;
    public string ProjectText => ProjectEditor.Text;

    public ProfileEditorWindow(InstructionProfileDocuments documents, bool isClaudeCode, string profileName)
    {
        InitializeComponent();
        // Jedes Profil ist genau EINE Datei. Der Editor zeigt Dateiname + Pfad und ein Textfeld.
        var fileName = InstructionProfileService.ActiveFileName(isClaudeCode);
        TitleText.Text = $"Profil {profileName} bearbeiten";
        CliText.Text = (isClaudeCode ? "Claude Code · " : "OpenCode · ") + fileName;
        IntroText.Text = $"Dieses Profil ist genau EINE Datei ({fileName}). Ihr Inhalt wird beim Start des Profils "
                       + $"in die aktive {fileName} geschrieben (vorher geleert). Änderungen gelten für neu gestartete Sessions.";
        GlobalPathText.Text = documents.GlobalPath;
        GlobalEditor.Text = documents.GlobalText;
        // Kein zweites Dokument mehr: Projekt-Tab entfaellt, der Tab traegt den Dateinamen.
        ProjectTab.Visibility = Visibility.Collapsed;
        GlobalTab.Content = fileName;
        StateChanged += (_, _) => UpdateMaximizeButton();
        SourceInitialized += (_, _) =>
        {
            var hwnd = new WindowInteropHelper(this).Handle;
            HwndSource.FromHwnd(hwnd)?.AddHook(WndProc);
        };
    }

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ChangedButton != MouseButton.Left) return;
        if (e.ClickCount >= 2)
            WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
        else
            DragMove();
    }

    private void Save_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = true;
        Close();
    }

    private void Maximize_Click(object sender, RoutedEventArgs e)
    {
        WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
    }

    private void UpdateMaximizeButton()
    {
        MaximizeButton.Content = WindowState == WindowState.Maximized ? "❐" : "▢";
    }

    private IntPtr WndProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
    {
        if (msg != WM_GETMINMAXINFO) return IntPtr.Zero;

        var monitor = MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST);
        if (monitor == IntPtr.Zero) return IntPtr.Zero;

        var info = new MONITORINFO { cbSize = Marshal.SizeOf<MONITORINFO>() };
        if (!GetMonitorInfo(monitor, ref info)) return IntPtr.Zero;

        var mmi = Marshal.PtrToStructure<MINMAXINFO>(lParam);
        var work = info.rcWork;
        var monitorRect = info.rcMonitor;
        mmi.ptMaxPosition.x = Math.Abs(work.left - monitorRect.left);
        mmi.ptMaxPosition.y = Math.Abs(work.top - monitorRect.top);
        mmi.ptMaxSize.x = Math.Abs(work.right - work.left);
        mmi.ptMaxSize.y = Math.Abs(work.bottom - work.top);
        Marshal.StructureToPtr(mmi, lParam, true);
        handled = true;
        return IntPtr.Zero;
    }

    private void Cancel_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
        Close();
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
