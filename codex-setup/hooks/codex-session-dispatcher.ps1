param(
    [Parameter(Mandatory = $true)]
    [string]$Event,
    [string]$WorkspaceRoot = "C:\Users\barwa\Codex"
)

$ErrorActionPreference = "Stop"
$settingsPath = "C:\\Users\\barwa\\.codex\\claude-harness\\settings.codex.json"
$promptDir = "C:\\Users\\barwa\\.codex\\claude-harness\\runtime\\prompt-hooks"
$logDir = "C:\\Users\\barwa\\.codex\\log"
$logPath = Join-Path $logDir "codex-claude-harness-hooks.log"

function Write-HarnessLog {
    param([string]$Message)
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
    Add-Content -Path $logPath -Value ("[{0}] {1}" -f (Get-Date -Format "s"), $Message)
}

function Invoke-HarnessCommand {
    param(
        [string]$Command,
        [int]$Timeout = 30,
        [bool]$Async = $false
    )

    if ($Async) {
        Start-Process -FilePath "pwsh.exe" -ArgumentList @("-NoProfile", "-Command", $Command) -WindowStyle Hidden | Out-Null
        Write-HarnessLog ("ASYNC " + $Command)
        return
    }

    $process = Start-Process -FilePath "pwsh.exe" -ArgumentList @("-NoProfile", "-Command", $Command) -PassThru -WindowStyle Hidden
    if ($process.WaitForExit($Timeout * 1000)) {
        Write-HarnessLog ("DONE  " + $Command + " => " + $process.ExitCode)
        return
    }

    try {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    } catch {}

    Write-HarnessLog ("TIMEOUT " + $Command)
}

if (-not (Test-Path $settingsPath)) {
    Write-Output "[codex-harness] Kein Settings-Port gefunden fuer Event $Event."
    exit 0
}

$settings = Get-Content -Path $settingsPath -Raw | ConvertFrom-Json
$entries = @($settings.hooks.$Event)
if (-not $entries -or $entries.Count -eq 0) {
    exit 0
}

New-Item -ItemType Directory -Force -Path $promptDir | Out-Null
$promptLogPath = Join-Path $promptDir ($Event + ".md")
if (Test-Path $promptLogPath) {
    Remove-Item -Path $promptLogPath -Force -ErrorAction SilentlyContinue
}

foreach ($entry in $entries) {
    foreach ($hook in @($entry.hooks)) {
        if ($hook.type -eq "command") {
            $timeout = 30
            if ($hook.timeout) {
                $timeout = [int]$hook.timeout
            }
            $async = $false
            if ($hook.PSObject.Properties.Name -contains "async") {
                $async = [bool]$hook.async
            }
            Invoke-HarnessCommand -Command ([string]$hook.command) -Timeout $timeout -Async $async
            continue
        }

        if ($hook.type -eq "prompt") {
            $promptText = [string]$hook.prompt
            Add-Content -Path $promptLogPath -Value ("## " + $Event)
            Add-Content -Path $promptLogPath -Value ""
            Add-Content -Path $promptLogPath -Value $promptText
            Add-Content -Path $promptLogPath -Value ""
            Write-HarnessLog ("PROMPT " + $Event)
            continue
        }

        if ($hook.type -eq "agent") {
            Write-HarnessLog ("AGENT " + $Event + " => " + [string]$hook.agent)
        }
    }
}

if (Test-Path $promptLogPath) {
    Write-Output ("[codex-harness] Prompt-Hooks fuer " + $Event + ": " + $promptLogPath)
}
