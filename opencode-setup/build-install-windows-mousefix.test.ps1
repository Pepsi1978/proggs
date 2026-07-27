$ErrorActionPreference = 'Stop'

$scriptPath = Join-Path $PSScriptRoot 'build-install-windows-mousefix.ps1'
$tokens = $null
$parseErrors = $null
$ast = [Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$tokens, [ref]$parseErrors)
if ($parseErrors.Count -ne 0) { throw "Buildskript ist syntaktisch ungueltig." }

$functionAst = $ast.Find({
    param($node)
    $node -is [Management.Automation.Language.FunctionDefinitionAst] -and
        $node.Name -eq 'Resolve-OpenTuiRendererBundle'
}, $true)
if (-not $functionAst) { throw 'Resolve-OpenTuiRendererBundle fehlt.' }
Invoke-Expression $functionAst.Extent.Text

$root = Join-Path ([IO.Path]::GetTempPath()) "opentui-renderer-test-$([Guid]::NewGuid().ToString('N'))"
[IO.Directory]::CreateDirectory($root) | Out-Null

function Write-Fixture([string]$Directory, [string]$Name, [string]$Content) {
    [IO.Directory]::CreateDirectory($Directory) | Out-Null
    [IO.File]::WriteAllText((Join-Path $Directory $Name), $Content)
}

try {
    $legacy = Join-Path $root 'legacy'
    Write-Fixture $legacy 'index-renderer.js' 'reportNativeRenderFailure()'
    Write-Fixture $legacy 'index-other.js' 'other code'
    $legacyResult = Resolve-OpenTuiRendererBundle $legacy
    if ([IO.Path]::GetFileName($legacyResult) -ne 'index-renderer.js') {
        throw "Altes OpenTUI-Layout wurde nicht erkannt: $legacyResult"
    }

    $current = Join-Path $root 'current'
    Write-Fixture $current 'index.bun.js' 'export {}'
    Write-Fixture $current 'chunk-bun-renderer.js' 'reportNativeRenderFailure()'
    Write-Fixture $current 'chunk-node-renderer.js' 'reportNativeRenderFailure()'
    $currentResult = Resolve-OpenTuiRendererBundle $current
    if ([IO.Path]::GetFileName($currentResult) -ne 'chunk-bun-renderer.js') {
        throw "Neues OpenTUI-Bun-Layout wurde nicht erkannt: $currentResult"
    }

    Write-Fixture $current 'chunk-bun-duplicate.js' 'reportNativeRenderFailure()'
    try {
        Resolve-OpenTuiRendererBundle $current | Out-Null
        throw 'Mehrdeutiges OpenTUI-Layout wurde akzeptiert.'
    } catch {
        if ($_.Exception.Message -notlike 'OpenTUI-Renderer-Bundle konnte nicht eindeutig bestimmt werden:*') {
            throw
        }
    }
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force -ErrorAction SilentlyContinue
}

'OK  OpenTUI-Renderer-Bundle-Erkennung'
