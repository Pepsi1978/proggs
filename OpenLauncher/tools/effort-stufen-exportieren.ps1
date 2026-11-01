# Schreibt die am PC gemerkten Effort-Stufen der sichtbaren Modelle nach OpenLauncher/effort-stufen.json.
# Jarvis liest die Datei über GitHub für seine OpenLauncher-Auswahl: Nach dem Lauf committen und pushen.
$quelle = Join-Path $env:APPDATA 'OpenLauncher\thinking-levels.json'
$wurzel = Split-Path $PSScriptRoot -Parent
$cache = Get-Content $quelle -Raw -Encoding UTF8 | ConvertFrom-Json

# Wie ModelEntry.ModelString.
$sichtbar = @{}
foreach ($gruppe in (Get-Content (Join-Path $wurzel 'models.json') -Raw -Encoding UTF8 | ConvertFrom-Json)) {
    foreach ($m in $gruppe.Models) {
        if ($m.IsHidden) { continue }
        $name = if ($m.ProviderId -ne 'nvidia' -and $m.Slug.StartsWith("$($m.ProviderId)/")) { $m.Slug } else { "$($m.ProviderId)/$($m.Slug)" }
        $sichtbar[$name] = $true
    }
}

$neu = [ordered]@{}
foreach ($p in ($cache.PSObject.Properties | Sort-Object Name)) {
    if ($sichtbar.ContainsKey(($p.Name -replace ' \[[^\]]+\]$', ''))) { $neu[$p.Name] = @($p.Value.Levels) }
}
# WriteAllText schreibt auch unter Windows PowerShell 5.1 ohne BOM.
[IO.File]::WriteAllText((Join-Path $wurzel 'effort-stufen.json'), ($neu | ConvertTo-Json -Depth 3), [Text.UTF8Encoding]::new($false))
