using System;
using System.IO;
using System.Text;
using System.Windows;
using System.Windows.Threading;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;

namespace VoiceAgent.Views
{
    /// <summary>
    /// Live-Ansicht des JSON-Logs direkt in der App: liest voiceagent.log fortlaufend mit
    /// (FileShare.ReadWrite — die App schreibt parallel), formatiert jede Zeile lesbar und
    /// haengt sie an. Damit kann Frank Sprach-Turns (turn:N) in Echtzeit im Fenster mitlesen,
    /// ohne ein separates PowerShell-Tail-Fenster.
    /// </summary>
    public partial class LogViewerWindow : Window
    {
        private readonly DispatcherTimer _timer;
        private long _lastPos;
        private bool _onlyTurns;

        public LogViewerWindow()
        {
            InitializeComponent();
            _timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(800) };
            _timer.Tick += (_, __) => Pump();
            Loaded += OnLoaded;
            SourceInitialized += (_, __) => ThemeManager.ApplyTitleBar(this);   // Titelleiste passend zum Theme (§2.7)
            Closed += (_, __) => _timer.Stop();   // DispatcherTimer-Leak vermeiden (Almanach §9.2)
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            try
            {
                var path = Log.LogFilePath;
                long len = File.Exists(path) ? new FileInfo(path).Length : 0;
                _lastPos = Math.Max(0, len - 65536);   // mit den letzten ~64 KB Historie starten
            }
            catch { _lastPos = 0; }
            Pump();
            _timer.Start();
        }

        private void Pump()
        {
            try
            {
                var path = Log.LogFilePath;
                if (!File.Exists(path)) return;

                using var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
                if (fs.Length < _lastPos) _lastPos = 0;   // Datei wurde rotiert -> von vorn
                if (fs.Length == _lastPos) return;        // nichts Neues

                fs.Seek(_lastPos, SeekOrigin.Begin);
                using var sr = new StreamReader(fs, Encoding.UTF8);
                string chunk = sr.ReadToEnd();
                _lastPos += Encoding.UTF8.GetByteCount(chunk);   // byte-genau weiterlesen

                var sb = new StringBuilder();
                foreach (var raw in chunk.Split('\n'))
                {
                    var line = raw.Trim();
                    if (line.Length == 0) continue;
                    if (_onlyTurns && !line.Contains("\"turn\":")) continue;
                    sb.Append(LogFormatter.FormatLine(line)).Append('\n');
                }
                if (sb.Length > 0)
                {
                    LogBox.AppendText(sb.ToString());
                    LogBox.ScrollToEnd();
                }
            }
            catch (Exception ex)
            {
                Log.Error("LogViewer: Mitlesen fehlgeschlagen", ex);
            }
        }

        private void OnlyTurns_Click(object sender, RoutedEventArgs e)
            => _onlyTurns = OnlyTurnsBox.IsChecked == true;

        private void Clear_Click(object sender, RoutedEventArgs e) => LogBox.Clear();
    }
}
