$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path
$TmpMessage = [System.IO.Path]::GetTempFileName()
$TmpLog = [System.IO.Path]::GetTempFileName()

try {
    $Server = ((codex mcp get openaiDeveloperDocs) -join "`n")

    if ($Server -notmatch "enabled: true") {
        throw "openaiDeveloperDocs MCP is not enabled."
    }

    if ($Server -notmatch "https://developers.openai.com/mcp") {
        throw "openaiDeveloperDocs MCP points to an unexpected URL."
    }

    $null = Invoke-WebRequest -UseBasicParsing -Uri "https://developers.openai.com/api/docs/guides/tools-connectors-mcp"

    $Prompt = @"
Use only the openaiDeveloperDocs MCP server.
Do not use web search or any fallback.
If openaiDeveloperDocs is available in this fresh Codex session, look up the OpenAI models page and reply with exactly AVAILABLE.
If the MCP server is unavailable, reply with exactly UNAVAILABLE.
"@

    $SmokeResult = ""
    foreach ($Attempt in 1..2) {
        Set-Content -NoNewline -Path $TmpMessage -Value ""
        Set-Content -NoNewline -Path $TmpLog -Value ""

        & codex exec `
            --skip-git-repo-check `
            --dangerously-bypass-approvals-and-sandbox `
            -C $RepoRoot `
            -c 'model_reasoning_effort="low"' `
            -o $TmpMessage `
            $Prompt *> $TmpLog

        if ($LASTEXITCODE -eq 0) {
            $SmokeResult = ((Get-Content $TmpMessage -Raw) -replace "`r", "").Trim()
            if ($SmokeResult -eq "AVAILABLE") {
                break
            }
        }

        if ($Attempt -lt 2) {
            Start-Sleep -Seconds 1
        }
    }

    if ($SmokeResult -ne "AVAILABLE") {
        Get-Content $TmpLog | Write-Error
        throw "Fresh Codex exec did not confirm openaiDeveloperDocs availability after retry. Last message: $SmokeResult"
    }

    Write-Host "openaiDeveloperDocs MCP configured, official docs reachable, and fresh Codex exec can use it"
}
finally {
    Remove-Item $TmpMessage, $TmpLog -Force -ErrorAction SilentlyContinue
}
