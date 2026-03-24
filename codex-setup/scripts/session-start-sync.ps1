$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& node (Join-Path $ScriptDir "session-start-sync.mjs") @args
exit $LASTEXITCODE
