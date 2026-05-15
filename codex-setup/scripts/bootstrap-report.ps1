param(
    [switch]$Json
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$argsList = @()
if ($Json) { $argsList += "--json" }
& node (Join-Path $ScriptDir "bootstrap-report.mjs") @argsList
