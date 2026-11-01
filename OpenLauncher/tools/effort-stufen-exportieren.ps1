# Schreibt die am PC gemerkten Effort-Stufen von OpenCode Zen, OpenRouter und NVIDIA nach OpenLauncher/effort-stufen.json.
# Jarvis liest die Datei über GitHub für seine OpenLauncher-Auswahl: Nach dem Lauf committen und pushen.
$quelle = Join-Path $env:APPDATA 'OpenLauncher\thinking-levels.json'
$ziel = Join-Path $PSScriptRoot '..\effort-stufen.json'
$cache = Get-Content $quelle -Raw | ConvertFrom-Json
$neu = [ordered]@{}
foreach ($p in ($cache.PSObject.Properties | Sort-Object Name)) {
    if ($p.Name -match '^(opencode|openrouter|nvidia)/') { $neu[$p.Name] = @($p.Value.Levels) }
}
$neu | ConvertTo-Json -Depth 3 | Set-Content -Path $ziel -Encoding utf8NoBOM
