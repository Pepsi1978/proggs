$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$viewModel = Get-Content -LiteralPath (Join-Path $root 'ViewModels\MainViewModel.cs') -Raw
$router = Get-Content -LiteralPath (Join-Path $root 'Services\OpenRouterService.cs') -Raw
$variants = Get-Content -LiteralPath (Join-Path $root 'Services\OpenCodeVariantCatalog.cs') -Raw

$checks = @(
    @{ Name = 'Aktualisieren laedt Provider und Thinking'; Pass = $viewModel -match '(?s)private async Task RefreshAsync\(\).*?Task\.WhenAll\(.*?LoadProvidersAsync\(model\).*?LoadThinkingOptionsAsync\(model, forceRefresh: true\).*?\)' }
    @{ Name = 'manueller Thinking-Refresh wird an OpenRouter weitergereicht'; Pass = $viewModel -match 'GetThinkingLevelsAsync\(model\.Slug, ct, forceRefresh\)' }
    @{ Name = 'manueller Refresh umgeht den Modellcache'; Pass = $router -match 'FetchModelsJsonAsync\(CancellationToken ct, bool forceRefresh = false\)' -and $router -match 'FetchModelsJsonAsync\(ct, forceRefresh\)' }
    @{ Name = 'reasoning_effort wird aus API-Metadaten gelesen'; Pass = $router -match 'ModelSupportsParameter\(item, "reasoning_effort"\)' }
    @{ Name = 'API-Effort ueberstimmt veraltete statische Ausschluesse'; Pass = $variants -match '(?s)public static IReadOnlyList<string> GetOpenRouterLevels\(string slug, bool supportsReasoning, bool supportsReasoningEffort = false\).*?if \(supportsReasoningEffort\) return WidelySupported;' }
)

$failed = @($checks | Where-Object { -not $_.Pass })
foreach ($check in $checks) {
    $mark = if ($check.Pass) { '[OK]' } else { '[FEHLER]' }
    "$mark $($check.Name)"
}

if ($failed.Count -gt 0) {
    throw "$($failed.Count) Modell-Aktualisierungs-Pruefung(en) fehlgeschlagen."
}
