$ErrorActionPreference = 'Stop'

# Sichert den Modell-Standard ab: je Modell gespeicherte Startauswahl (Profil, Modus, Effort),
# die beim Wechsel auf das Modell - und damit auch beim App-Start - wieder vorausgewaehlt wird.
$root = Split-Path -Parent $PSScriptRoot
$viewModel = Get-Content -LiteralPath (Join-Path $root 'ViewModels\MainViewModel.cs') -Raw
$service = Get-Content -LiteralPath (Join-Path $root 'Services\ModelDefaultsService.cs') -Raw
$entry = Get-Content -LiteralPath (Join-Path $root 'Models\ModelDefaultEntry.cs') -Raw
$xaml = Get-Content -LiteralPath (Join-Path $root 'MainWindow.xaml') -Raw
[void][xml]$xaml

$checks = @(
    @{ Name = 'Standard traegt Profil, Modus und Effort'; Pass = $entry -match 'ProfileId' -and $entry -match 'WorkModeId' -and $entry -match 'ThinkingValue' }
    @{ Name = 'Standards liegen versioniert im Repo'; Pass = $service -match '"proggs", "OpenLauncher"' -and $service -match 'model-defaults\.json' }
    @{ Name = 'Standards werden atomar geschrieben'; Pass = $service -match '(?s)var tmp = FilePath \+ "\.tmp";.*?File\.Move\(tmp, FilePath, overwrite: true\)' }
    @{ Name = 'unlesbare Datei blockiert den Start nicht'; Pass = $service -match '(?s)catch \(Exception ex\).*?_defaults\.Clear\(\);' }
    @{ Name = 'Schluessel ist die provider-qualifizierte Modell-ID'; Pass = $viewModel -match '_modelDefaults\.Find\(value\.ModelString\)' -and $viewModel -match 'var key = model\.ModelString;' }
    @{ Name = 'Modellwechsel wendet Profil und Modus des Standards an'; Pass = $viewModel -match '(?s)_pendingModelDefault = value == null \? null : _modelDefaults\.Find\(value\.ModelString\);.*?profile\.Id == _pendingModelDefault\.ProfileId.*?mode\.Id == _pendingModelDefault\.WorkModeId' }
    @{ Name = 'ohne Standard bleibt das Minimalprofil'; Pass = $viewModel -match 'SelectedProfile = Profiles\.FirstOrDefault\(profile => profile\.Id == "minimal"\)' }
    @{ Name = 'gespeicherter Effort schlaegt die Profil-Vorauswahl'; Pass = $viewModel -match '(?s)private void SelectProfileThinkingOption\(\).*?_pendingModelDefault!\.ThinkingValue.*?SelectedThinkingOption = storedOption;.*?SelectedProfile\.Id == "strict" \? "xhigh" : "high"' }
    @{ Name = 'eigener Profilwechsel verwirft den Standard'; Pass = $viewModel -match 'if \(!_applyingModelDefault\) _pendingModelDefault = null;' }
    @{ Name = 'programmatisches Setzen behaelt den Standard'; Pass = $viewModel -match '(?s)_applyingModelDefault = true;.*?finally\s*\{\s*_applyingModelDefault = false;' }
    @{ Name = 'derselbe Schalter speichert und entfernt'; Pass = $viewModel -match '(?s)private void ToggleModelDefault\(\).*?if \(MatchesStoredDefault\(stored\)\).*?_modelDefaults\.Remove\(key\).*?_modelDefaults\.Save\(key, entry\)' }
    @{ Name = 'Schalterbeschriftung folgt dem gespeicherten Zustand'; Pass = $viewModel -match 'ModelDefaultButtonText = MatchesStoredDefault\(stored\) \? "★ Standard entfernen" : "☆ Standard speichern"' }
    @{ Name = 'Standard-Schalter steht neben den Bearbeiten-Knoepfen'; Pass = $xaml -match '(?s)Content="Modus bearbeiten".*?Command="\{Binding ToggleModelDefaultCommand\}"' }
    @{ Name = 'Schalter zeigt die dynamische Beschriftung'; Pass = $xaml -match 'Content="\{Binding ModelDefaultButtonText\}"' }
    @{ Name = 'gespeicherter Standard wird im Profil-Bereich angezeigt'; Pass = $xaml -match 'Text="\{Binding ModelDefaultSummary\}"' -and $xaml -match 'Visibility="\{Binding HasModelDefault, Converter=\{StaticResource BoolToVisibility\}\}"' }
    # Die Standard-Zeile ERSETZT die Kontextzeile. Stuenden beide gleichzeitig, saesse der
    # Profil-Bereich mit gespeichertem Standard eine Zeile tiefer als ohne.
    @{ Name = 'Standard-Zeile ersetzt die Kontextzeile'; Pass = $xaml -match 'Text="\{Binding ProfileContextText\}"[^>]*Visibility="\{Binding HasNoModelDefault, Converter=\{StaticResource BoolToVisibility\}\}"' }
    @{ Name = 'Kontextzeile und Standard-Zeile schliessen sich aus'; Pass = $viewModel -match '(?s)HasModelDefault = stored != null;\s*HasNoModelDefault = stored == null;' }
)

$failed = @($checks | Where-Object { -not $_.Pass })
foreach ($check in $checks) {
    $mark = if ($check.Pass) { '[OK]' } else { '[FEHLER]' }
    "$mark $($check.Name)"
}

if ($failed.Count -gt 0) {
    throw "$($failed.Count) Modell-Standard-Prüfung(en) fehlgeschlagen."
}
