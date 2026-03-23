$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& node (Join-Path $ScriptDir "register-environment-fix.mjs") @args
