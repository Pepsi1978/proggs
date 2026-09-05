using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using OpenLauncher.Models;
using OpenLauncher.Services;
using OpenLauncher.ViewModels;

namespace OpenLauncher;

public partial class SettingsWindow : Window
{
    private readonly MainViewModel _viewModel;
    private readonly CodexResearchService _service = CodexResearchService.Instance;
    private readonly CancellationTokenSource _lifetime = new();
    private CancellationTokenSource? _operation;
    private ResearchSettings _settings = new();
    private readonly DispatcherTimer _reportsTimer = new() { Interval = TimeSpan.FromSeconds(2) };

    /// <summary>The refresh/cache owner may subscribe to apply a validated manual result.</summary>
    public event Action<ModelEntry, string, EffortResearchResult>? ResearchCompleted;

    public SettingsWindow(MainViewModel viewModel)
    {
        _viewModel = viewModel;
        InitializeComponent();
        ModeBox.ItemsSource = new[] {
            new { Value = ResearchMode.Disabled, Label = "Ausgeschaltet" },
            new { Value = ResearchMode.Fallback, Label = "Automatisch bei Lücken oder Widersprüchen" },
            new { Value = ResearchMode.Manual, Label = "Nur manuell" },
            new { Value = ResearchMode.Periodic, Label = "Regelmäßig (benutzte Modelle)" } };
        PeriodBox.ItemsSource = new[] { new { Hours = 1, Label = "Stündlich" }, new { Hours = 6, Label = "Alle 6 Stunden" },
            new { Hours = 12, Label = "Alle 12 Stunden" }, new { Hours = 24, Label = "Täglich" }, new { Hours = 168, Label = "Wöchentlich" } };
        _reportsTimer.Tick += (_, _) => ShowReports();
        Loaded += async (_, _) => await RunAsync(async ct =>
        {
            _settings = await Task.Run(ResearchSettingsService.Load, ct);
            ModeBox.SelectedValue = _settings.Mode;
            PeriodBox.SelectedValue = _settings.PeriodHours;
            await ReloadAsync(ct);
            _reportsTimer.Start();
        });
        Closed += (_, _) => { _reportsTimer.Stop(); _lifetime.Cancel(); _operation?.Cancel(); };
    }

    private async Task ReloadAsync(CancellationToken ct)
    {
        var connected = await _service.IsConnectedAsync(ct);
        ConnectionText.Text = connected ? "Anmeldung gespeichert; Kontozugriff wird geprüft …" : "Nicht verbunden";
        var models = await _service.GetModelsAsync(ct);
        ModelBox.ItemsSource = models;
        ModelBox.SelectedItem = models.FirstOrDefault(x => x.Id == _settings.Model) ?? models.FirstOrDefault();
        if (EffortBox.Items.Contains(_settings.Effort)) EffortBox.SelectedItem = _settings.Effort;
        ConnectionText.Text = connected ? $"Verbunden · {models.Count} verfügbare Modelle" : "Nicht verbunden · keine KI-Aufrufe";
        ShowReports();
    }

    private void Model_Changed(object sender, SelectionChangedEventArgs e)
    {
        if (EffortBox == null) return;
        var efforts = (ModelBox.SelectedItem as CodexResearchModel)?.Efforts ?? [];
        EffortBox.ItemsSource = efforts;
        EffortBox.SelectedItem = efforts.Contains(_settings.Effort) ? _settings.Effort : efforts.FirstOrDefault();
    }

    private async void Login_Click(object sender, RoutedEventArgs e) => await RunAsync(async ct =>
    {
        ConnectionText.Text = "Geräteanmeldung wird gestartet …";
        await _service.LoginAsync((code, url) => Dispatcher.InvokeAsync(() =>
        {
            DeviceText.Text = $"{url}\nGerätecode: {code}";
            DeviceText.Visibility = Visibility.Visible;
            DeviceLinkButton.Visibility = Visibility.Visible;
            ConnectionText.Text = "Öffne die Anmeldeseite und bestätige den Gerätecode.";
        }), ct);
        DeviceText.Clear();
        DeviceText.Visibility = Visibility.Collapsed;
        DeviceLinkButton.Visibility = Visibility.Collapsed;
        await ReloadAsync(ct);
    });

    private void DeviceLink_Click(object sender, RoutedEventArgs e)
    {
        try { Process.Start(new ProcessStartInfo("https://auth.openai.com/codex/device") { UseShellExecute = true }); }
        catch (Exception ex) { ShowError(ex); }
    }

    private async void Reload_Click(object sender, RoutedEventArgs e) => await RunAsync(ReloadAsync);
    private async void Save_Click(object sender, RoutedEventArgs e) => await RunAsync(SaveAsync);

    private async Task SaveAsync(CancellationToken ct)
    {
        var settings = new ResearchSettings
        {
            Mode = ModeBox.SelectedValue is ResearchMode mode ? mode : ResearchMode.Disabled,
            Model = (ModelBox.SelectedItem as CodexResearchModel)?.Id ?? _settings.Model,
            Effort = EffortBox.SelectedItem as string ?? _settings.Effort,
            PeriodHours = PeriodBox.SelectedValue is int hours ? hours : _settings.PeriodHours
        };
        await Task.Run(() => ResearchSettingsService.Save(settings), ct);
        _settings = settings;
        StatusText.Text = "Einstellungen gespeichert.";
    }

    private async void Research_Click(object sender, RoutedEventArgs e) => await RunAsync(async ct =>
    {
        var selected = _viewModel.SelectedModel;
        if (selected == null) { StatusText.Text = "Zuerst ein Launcher-Modell auswählen."; return; }
        // Snapshot protects the request against concurrent edits to the model registry.
        var model = new ModelEntry { Slug = selected.Slug, ProviderId = selected.ProviderId, DisplayName = selected.DisplayName };
        var target = _viewModel.ThinkingAccess;
        await SaveAsync(ct);
        StatusText.Text = "Web-Recherche läuft …";
        var result = await _service.ResearchAsync(model, target, ct, manual: true);
        if (result != null) ResearchCompleted?.Invoke(model, target, result);
        StatusText.Text = result == null ? "Kein übernehmbarer Nachweis. Siehe Bericht." : "Belegte Recherche abgeschlossen. Siehe Bericht.";
        ShowReports();
    });

    private void ShowReports()
    {
        TargetText.Text = _viewModel.SelectedModel?.ModelString ?? "Kein Launcher-Modell ausgewählt";
        var reports = _service.GetReports();
        var text = reports.Count == 0 ? "Noch keine Recherche in dieser Launcher-Sitzung." : string.Join("\n\n", reports.Select(
            x => $"{x.Model} · {x.CheckedAt.ToLocalTime():dd.MM.yyyy HH:mm}\n{x.Status}"));
        var updates = EffortRefreshService.GetReports();
        if (updates.Count > 0) text = string.Join("\n\n", updates.Select(x =>
            $"{x.Model}\n{x.Status}\nLetzter Erfolg: {x.LastSuccess?.ToLocalTime().ToString("dd.MM.yyyy HH:mm") ?? "noch keiner"}\nQuelle: {x.Source}")) + "\n\nKI-RECHERCHE\n" + text;
        if (ReportText.Text != text) ReportText.Text = text;
    }

    private async Task RunAsync(Func<CancellationToken, Task> action)
    {
        if (_operation != null) return;
        using var operation = CancellationTokenSource.CreateLinkedTokenSource(_lifetime.Token);
        operation.CancelAfter(TimeSpan.FromMinutes(16));
        _operation = operation;
        SetBusy(true);
        try { await action(operation.Token); }
        catch (OperationCanceledException) { StatusText.Text = "Vorgang abgebrochen oder Zeitlimit erreicht."; }
        catch (Exception ex) { ShowError(ex); }
        finally
        {
            _operation = null;
            SetBusy(false);
            DeviceText.Clear();
            DeviceText.Visibility = Visibility.Collapsed;
            DeviceLinkButton.Visibility = Visibility.Collapsed;
        }
    }

    private void SetBusy(bool busy)
    {
        LoginButton.IsEnabled = ReloadButton.IsEnabled = SaveButton.IsEnabled = ResearchButton.IsEnabled = SettingsPanel.IsEnabled = !busy;
        CancelButton.IsEnabled = busy;
    }
    private void ShowError(Exception ex)
    {
        Logger.Instance.Warn(nameof(SettingsWindow), "Operation", "Codex-Einstellungen: " + ex.GetType().Name);
        StatusText.Text = CodexResearchService.DescribeFailure(ex);
        ConnectionText.Text = "Verbindung nicht bestätigt; gespeicherte Anmeldung bleibt erhalten.";
    }
    private void Cancel_Click(object sender, RoutedEventArgs e) => _operation?.Cancel();
    private void Close_Click(object sender, RoutedEventArgs e) => Close();
}
