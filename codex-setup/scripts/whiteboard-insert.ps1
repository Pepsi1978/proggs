param(
    [Parameter(Mandatory = $true)]
    [string]$Section,
    [Parameter(Mandatory = $true)]
    [string]$Text,
    [string]$Workspace = (Get-Location).Path
)

$scriptPath = Join-Path $PSScriptRoot "whiteboard-bridge.mjs"
node $scriptPath insert --workspace $Workspace --section $Section --text $Text
