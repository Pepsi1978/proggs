# Schreibt für Jarvis' OpenLauncher-Auswahl nach OpenLauncher/effort-stufen.json:
# die am PC gemerkten Effort-Stufen der sichtbaren Modelle und unter "nicht-startbar" die Modelle, die OpenCode hier nicht kennt.
# Jarvis liest die Datei über GitHub: Nach dem Lauf committen und pushen.
$quelle = Join-Path $env:APPDATA 'OpenLauncher\thinking-levels.json'
$wurzel = Split-Path $PSScriptRoot -Parent
$cache = Get-Content $quelle -Raw -Encoding UTF8 | ConvertFrom-Json

# Ohne diesen Schlüssel listet OpenCode keine NVIDIA-Modelle (wie BuildNvidiaKeyScript im Launcher).
foreach ($zeile in Get-Content (Join-Path $HOME 'SK\NvidiaDev\.env') -ErrorAction SilentlyContinue) {
    if ($zeile -match '^\s*NVIDIA_API_KEY\s*=\s*(.+)$') { $env:NVIDIA_API_KEY = $Matches[1].Trim().Trim('"').Trim("'") }
}
$bekannt = @{}
opencode models 2>$null | ForEach-Object { $bekannt[($_ -replace "\x1b\[[0-9;]*m", '').Trim()] = $true }
if ($bekannt.Count -lt 50) { throw "opencode models lieferte nur $($bekannt.Count) Modelle; nichts geschrieben." }

# Wie ModelEntry.ModelString. Claude Code und LM Studio laufen nicht über diesen Katalog.
$sichtbar = @{}
$nichtStartbar = @()
foreach ($gruppe in (Get-Content (Join-Path $wurzel 'models.json') -Raw -Encoding UTF8 | ConvertFrom-Json)) {
    foreach ($m in $gruppe.Models) {
        if ($m.IsHidden) { continue }
        $name = if ($m.ProviderId -ne 'nvidia' -and $m.Slug.StartsWith("$($m.ProviderId)/")) { $m.Slug } else { "$($m.ProviderId)/$($m.Slug)" }
        $sichtbar[$name] = $true
        if ($m.ProviderId -notin 'anthropic', 'lmstudio' -and -not $bekannt.ContainsKey($name)) { $nichtStartbar += $name }
    }
}

$neu = [ordered]@{ 'nicht-startbar' = @($nichtStartbar | Sort-Object -Unique) }
foreach ($p in ($cache.PSObject.Properties | Sort-Object Name)) {
    if ($sichtbar.ContainsKey(($p.Name -replace ' \[[^\]]+\]$', ''))) { $neu[$p.Name] = @($p.Value.Levels) }
}
# WriteAllText schreibt auch unter Windows PowerShell 5.1 ohne BOM.
[IO.File]::WriteAllText((Join-Path $wurzel 'effort-stufen.json'), ($neu | ConvertTo-Json -Depth 3), [Text.UTF8Encoding]::new($false))
"Nicht startbar: $($nichtStartbar.Count), bekannt: $($bekannt.Count)"
