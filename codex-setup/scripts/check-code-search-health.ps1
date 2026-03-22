$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& node (Join-Path $ScriptDir "check-code-search-health.mjs") @args
exit $LASTEXITCODE
