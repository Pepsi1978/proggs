# Auto-format: runs the appropriate code formatter after file edits (Windows)
# ROBUSTNESS: Non-critical async hook. Any failure → exit 0 silently.

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

try {
    $hookInput = [Console]::In.ReadToEnd()
    $data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch { exit 0 }
if (-not $data) { exit 0 }

$filePath = $data.tool_input.file_path
if (-not $filePath -or -not (Test-Path $filePath)) { exit 0 }

Hook-Log "formatting $filePath"
$ext = [System.IO.Path]::GetExtension($filePath).TrimStart('.')
$formatResult = $null
$formatCmd = ""
switch ($ext) {
    'go'    { $formatCmd = "gofmt"; $formatResult = & gofmt -w $filePath 2>&1 }
    'rs'    { $formatCmd = "rustfmt"; $formatResult = & rustfmt $filePath 2>&1 }
    'cs'    {
        $dir = [System.IO.Path]::GetDirectoryName($filePath)
        $proj = Get-ChildItem -Path $dir -Filter "*.csproj" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($proj) { $formatCmd = "dotnet-format"; $formatResult = & dotnet format whitespace $proj.DirectoryName --include $filePath 2>&1 }
    }
    { $_ -in 'ts','tsx','mts','js','jsx','mjs','json','css' } {
        if (Get-Command biome -ErrorAction SilentlyContinue) {
            $formatCmd = "biome"; $formatResult = & biome format --write $filePath 2>&1
        } elseif (Get-Command prettier -ErrorAction SilentlyContinue) {
            $formatCmd = "prettier"; $formatResult = & prettier --write $filePath 2>&1
        }
    }
    { $_ -in 'c','cpp','h','hpp' } {
        if (Get-Command clang-format -ErrorAction SilentlyContinue) {
            $formatCmd = "clang-format"; $formatResult = & clang-format -i $filePath 2>&1
        }
    }
    { $_ -in 'kt','kts' } {
        if (Get-Command ktfmt -ErrorAction SilentlyContinue) {
            $formatCmd = "ktfmt"; $formatResult = & ktfmt --kotlinlang-style $filePath 2>&1
        }
    }
    'java' {
        if (Get-Command google-java-format -ErrorAction SilentlyContinue) {
            $formatCmd = "google-java-format"; $formatResult = & google-java-format --replace $filePath 2>&1
        }
    }
}
# Log formatter errors to hook log only (NOT whiteboard — too noisy for non-critical formatting failures)
if ($formatCmd -and $LASTEXITCODE -ne 0) {
    Hook-LogWarn "formatter $formatCmd failed (exit $LASTEXITCODE) on $filePath"
}
exit 0
