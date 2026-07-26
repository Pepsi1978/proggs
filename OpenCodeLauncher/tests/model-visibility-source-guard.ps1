$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$model = Get-Content -LiteralPath (Join-Path $root 'Models\ModelEntry.cs') -Raw
$viewModel = Get-Content -LiteralPath (Join-Path $root 'ViewModels\MainViewModel.cs') -Raw
$mainXaml = Get-Content -LiteralPath (Join-Path $root 'MainWindow.xaml') -Raw
$dialogXaml = Get-Content -LiteralPath (Join-Path $root 'HiddenModelsWindow.xaml') -Raw
$project = [xml](Get-Content -LiteralPath (Join-Path $root 'OpenCodeLauncher.csproj') -Raw)
[void][xml]$mainXaml
[void][xml]$dialogXaml

$checks = @(
    @{ Name = 'persistenter IsHidden-Status'; Pass = $model -match 'private bool _isHidden;' }
    @{ Name = 'Ausblenden speichert Registry'; Pass = $viewModel -match '(?s)private void HideModel\(ModelEntry model\).*?model\.IsHidden = true;.*?_registry\.Save\(\);' }
    @{ Name = 'Einblenden speichert Registry'; Pass = $viewModel -match '(?s)private void RestoreModel\(ModelEntry model\).*?model\.IsHidden = false;.*?_registry\.Save\(\);' }
    @{ Name = 'OpenRouterFree-Sync bewahrt ausgeblendete Modelle'; Pass = (Get-Content -LiteralPath (Join-Path $root 'Services\ModelRegistry.cs') -Raw) -match 'existing\.IsUserDefined \|\| existing\.IsHidden' }
    @{ Name = 'Startauswahl ignoriert versteckte Modelle'; Pass = $viewModel -match 'FirstOrDefault\(model => !model\.IsHidden\)' }
    @{ Name = 'Auge an jedem sichtbaren Modell'; Pass = $mainXaml -match 'Command="\{Binding DataContext\.HideModelCommand' }
    @{ Name = 'versteckte Listeneinträge sind eingeklappt'; Pass = $mainXaml -match '(?s)Binding IsHidden.*?Visibility" Value="Collapsed"' }
    @{ Name = 'zentraler Ausgeblendet-Button'; Pass = $mainXaml -match 'Command="\{Binding ShowHiddenModelsCommand\}"' }
    @{ Name = 'Wiederherstellung pro Modell'; Pass = $dialogXaml -match 'Command="\{Binding DataContext\.RestoreModelCommand' }
    @{ Name = 'Version 1.17.55'; Pass = $project.Project.PropertyGroup.Version -contains '1.17.55' }
)

$failed = @($checks | Where-Object { -not $_.Pass })
foreach ($check in $checks) {
    $mark = if ($check.Pass) { '[OK]' } else { '[FEHLER]' }
    "$mark $($check.Name)"
}

if ($failed.Count -gt 0) {
    throw "$($failed.Count) Modell-Ausblendungs-Prüfung(en) fehlgeschlagen."
}
