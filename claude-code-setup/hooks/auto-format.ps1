# Auto-format: runs the appropriate code formatter after file edits (Windows)
$input = $args[0]
if (-not $input) { $input = [Console]::In.ReadToEnd() }
$data = $input | ConvertFrom-Json -ErrorAction SilentlyContinue
if (-not $data) { exit 0 }

$filePath = $data.tool_input.file_path
if (-not $filePath -or -not (Test-Path $filePath)) { exit 0 }

$ext = [System.IO.Path]::GetExtension($filePath).TrimStart('.')
switch ($ext) {
    'go'    { & gofmt -w $filePath 2>$null }
    'rs'    { & rustfmt $filePath 2>$null }
    'cs'    {
        $dir = [System.IO.Path]::GetDirectoryName($filePath)
        $proj = Get-ChildItem -Path $dir -Filter "*.csproj" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($proj) { & dotnet format whitespace $proj.DirectoryName --include $filePath 2>$null }
    }
    { $_ -in 'ts','tsx','mts' } {
        if (Get-Command biome -ErrorAction SilentlyContinue) {
            & biome format --write $filePath 2>$null
        } elseif (Get-Command prettier -ErrorAction SilentlyContinue) {
            & prettier --write $filePath 2>$null
        }
    }
    { $_ -in 'c','cpp','h','hpp' } {
        if (Get-Command clang-format -ErrorAction SilentlyContinue) {
            & clang-format -i $filePath 2>$null
        }
    }
}
exit 0
