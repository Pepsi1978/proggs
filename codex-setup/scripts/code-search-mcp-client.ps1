$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& node (Join-Path $ScriptDir "code-search-mcp-client.mjs") @args
exit $LASTEXITCODE
