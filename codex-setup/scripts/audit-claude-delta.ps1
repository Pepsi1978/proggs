param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AuditArgs
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& node (Join-Path $ScriptDir "audit-claude-delta.mjs") @AuditArgs
exit $LASTEXITCODE

