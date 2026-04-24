param(
    [string]$Workspace = (Get-Location).Path,
    [string]$SentinelDir = ""
)

$scriptPath = Join-Path $PSScriptRoot "whiteboard-bridge.mjs"
if ([string]::IsNullOrWhiteSpace($SentinelDir)) {
    node $scriptPath merge-sentinels --workspace $Workspace
} else {
    node $scriptPath merge-sentinels --workspace $Workspace --dir $SentinelDir
}
