# Prueft die echte XAML-Bindung ohne Launcher-Start, Netzwerkzugriffe oder Profilveraenderungen.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationFramework
$project = Split-Path $PSScriptRoot -Parent
$null = [Reflection.Assembly]::LoadFrom((Join-Path $project 'bin\Release\net8.0-windows10.0.19041.0\win-x64\OpenLauncher.dll'))
Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.ComponentModel;
public sealed class SelectionFixture : INotifyPropertyChanged {
    private readonly Dictionary<string,object> state = new Dictionary<string,object>();
    public event PropertyChangedEventHandler PropertyChanged;
    private object Get(string name) => state.TryGetValue(name, out var value) ? value : null;
    public object SelectedModel => Get(nameof(SelectedModel));
    public object SelectedProfile => Get(nameof(SelectedProfile));
    public object SelectedWorkMode => Get(nameof(SelectedWorkMode));
    public object SelectedCliTarget => Get(nameof(SelectedCliTarget));
    public object SelectedThinkingOption => Get(nameof(SelectedThinkingOption));
    public bool HasCliChoice => Get(nameof(HasCliChoice)) is true;
    public string ThinkingTitle => Get(nameof(ThinkingTitle)) as string;
    public string StatusText => Get(nameof(StatusText)) as string;
    public void Set(string name, object value) {
        state[name] = value;
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
    }
}
'@
[xml]$document = Get-Content -LiteralPath (Join-Path $project 'MainWindow.xaml') -Raw
$ns = [Xml.XmlNamespaceManager]::new($document.NameTable)
$ns.AddNamespace('p', 'http://schemas.microsoft.com/winfx/2006/xaml/presentation')
$ns.AddNamespace('x', 'http://schemas.microsoft.com/winfx/2006/xaml')
$markup = $document.SelectSingleNode('//*[@x:Name="StatusBlock"]', $ns).OuterXml
$markup = $markup.Replace('xmlns:local="clr-namespace:OpenLauncher"', 'xmlns:local="clr-namespace:OpenLauncher;assembly=OpenLauncher"')
$fixture = '<Border xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" xmlns:local="clr-namespace:OpenLauncher;assembly=OpenLauncher"><Border.Resources><local:SelectionSummaryConverter x:Key="SelectionSummaryConverter"/></Border.Resources>' + $markup + '</Border>'
$root = [Windows.Markup.XamlReader]::Parse($fixture)
$state = [SelectionFixture]::new()
$root.DataContext = $state
$status = $root.FindName('StatusBlock')
function Assert-Summary([string]$expected) {
    $null = $root.Dispatcher.Invoke([Action]{}, [Windows.Threading.DispatcherPriority]::ApplicationIdle)
    if ($status.Text -cne $expected) { throw "Erwartet: $expected`nErhalten: $($status.Text)" }
    Write-Output "PASS: $expected"
}
function Choose([string]$field, [string]$name) {
    # Echte Observable-ModelEntry prueft auch Aenderungen am verschachtelten DisplayName.
    $entry = [OpenLauncher.Models.ModelEntry]::new()
    $entry.DisplayName = $name
    $state.Set($field, $entry)
}
Assert-Summary 'Modell wählen.'
Choose SelectedModel 'Claude Fable 5.1'
Choose SelectedProfile 'Standard'
Choose SelectedWorkMode 'Freimodus'
$state.Set('ThinkingTitle', 'EFFORT')
Choose SelectedThinkingOption 'High'
Assert-Summary 'Claude Fable 5.1 · Profil Standard · Modus Freimodus · Effort High'
$state.Set('StatusText', 'Profil Standard ausgewählt.')
Assert-Summary 'Claude Fable 5.1 · Profil Standard · Modus Freimodus · Effort High'
Choose SelectedProfile 'Minimal'
Assert-Summary 'Claude Fable 5.1 · Profil Minimal · Modus Freimodus · Effort High'
Choose SelectedModel 'GPT 6 Astra'
$state.Set('HasCliChoice', $true)
$state.Set('ThinkingTitle', 'THINKING')
Choose SelectedCliTarget 'Codex CLI'
Choose SelectedThinkingOption 'Medium'
Assert-Summary 'GPT 6 Astra · Profil Minimal · Modus Freimodus · CLI Codex CLI · Thinking Medium'
$state.Set('StatusText', 'Thinking für GPT 6 Astra: Medium')
Assert-Summary 'GPT 6 Astra · Profil Minimal · Modus Freimodus · CLI Codex CLI · Thinking Medium'
Choose SelectedProfile 'Standard'
Choose SelectedWorkMode 'Schnellmodus'
Choose SelectedCliTarget 'OpenCode'
Choose SelectedThinkingOption 'X High'
Assert-Summary 'GPT 6 Astra · Profil Standard · Modus Schnellmodus · CLI OpenCode · Thinking X High'
$state.SelectedModel.DisplayName = 'GPT 6 Astra aktualisiert'
Assert-Summary 'GPT 6 Astra aktualisiert · Profil Standard · Modus Schnellmodus · CLI OpenCode · Thinking X High'
$state.Set('SelectedThinkingOption', $null)
Assert-Summary 'GPT 6 Astra aktualisiert · Profil Standard · Modus Schnellmodus · CLI OpenCode'
