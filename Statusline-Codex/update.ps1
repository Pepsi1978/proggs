param([switch]$MitCodexUpdate)
$ErrorActionPreference = 'Stop'
if ($MitCodexUpdate) {
    Write-Host 'Aktualisiere zuerst die offizielle Codex-CLI …'
    & npm install -g '@openai/codex@latest'
    if ($LASTEXITCODE -ne 0) { throw 'Codex-Update fehlgeschlagen. Die Kostenanzeige wurde nicht verändert.' }
}
Write-Host 'Baue die Kostenanzeige für die installierte Codex-Version …'
& (Join-Path $PSScriptRoot 'build-install.ps1')
if ($LASTEXITCODE -ne 0) { throw 'Kostenanzeige konnte nicht aktualisiert werden.' }
Write-Host 'Fertig. Codex vollständig schließen und neu starten.'
