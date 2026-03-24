$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& node (Join-Path $ScriptDir "restore-mcp-config.mjs") @args
exit $LASTEXITCODE
