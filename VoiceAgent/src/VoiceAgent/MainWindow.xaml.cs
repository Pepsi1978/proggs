using System;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using VoiceAgent.Core;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;
using VoiceAgent.Services.Llm;

namespace VoiceAgent
{
    /// <summary>
    /// Hauptfenster. In Etappe 2 ein Text-Chat mit dem BossAgent (Debug-Eingabe);
    /// Sprachausgabe (Etappe 3) und Mikrofon-Loop (Etappe 4) werden hier angedockt.
    /// </summary>
    public partial class MainWindow : Window
    {
        private AppSettings _settings = new();
        private BossAgent? _agent;

        public MainWindow()
        {
            InitializeComponent();
            Loaded += OnLoaded;
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            try
            {
                _settings = Config.Load();
                _agent = new BossAgent(LlmProviderFactory.Create(_settings), _settings.SystemPrompt);
                Log.Info("MainWindow geladen, Agent bereit.");
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Initialisierung fehlgeschlagen", ex);
                Append("System", "Initialisierung fehlgeschlagen: " + ex.Message);
            }
        }

        private void InputBox_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.Key == Key.Enter)
            {
                e.Handled = true;
                _ = SendAsync();
            }
        }

        private void SendButton_Click(object sender, RoutedEventArgs e) => _ = SendAsync();

        private void SettingsButton_Click(object sender, RoutedEventArgs e)
        {
            // Einstellungs-Fenster kommt in Etappe 5.
            Append("System", "Einstellungen folgen in Etappe 5. API-Schluessel bis dahin in ~/SK/VoiceAgent/keys.json.");
        }

        // Der Body steht komplett in try/catch — kein unbeobachtetes async void (Almanach §7.2).
        private async Task SendAsync()
        {
            var text = InputBox.Text?.Trim();
            if (string.IsNullOrEmpty(text) || _agent == null) return;

            InputBox.Clear();
            Append("Du", text);
            SetStatus("Denke nach …");
            SendButton.IsEnabled = false;
            try
            {
                var reply = await _agent.RespondAsync(text);
                Append("Agent", reply);
                SetStatus("Bereit");
            }
            catch (Exception ex)
            {
                Log.Error("MainWindow: Antwort fehlgeschlagen", ex);
                Append("Fehler", ex.Message);
                SetStatus("Fehler — siehe Log");
            }
            finally
            {
                SendButton.IsEnabled = true;
            }
        }

        private void Append(string who, string text)
        {
            ConversationBox.AppendText($"{who}: {text}\n\n");
            ConversationBox.ScrollToEnd();
        }

        private void SetStatus(string text) => StatusText.Text = text;
    }
}
