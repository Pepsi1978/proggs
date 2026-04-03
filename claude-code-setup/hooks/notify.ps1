# Dynamic notification for Windows: extracts the actual message from Claude Code
# MUST run under powershell.exe (5.1), NOT pwsh 7+ — WinRT Toast API requires it
# ROBUSTNESS: Non-critical hook. Any failure → exit 0 silently.

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

try { $hookInput = [Console]::In.ReadToEnd() } catch { exit 0 }
$msg = $null
try {
    $json = $hookInput | ConvertFrom-Json
    $msg = $json.notification.message
} catch {
    Hook-LogWarn "notify.ps1: Failed to parse notification JSON: $_"
    $msg = $null
}
if (-not $msg) { $msg = "Braucht deine Aufmerksamkeit" }
if ($msg.Length -gt 200) { $msg = $msg.Substring(0, 200) }

# Windows Toast Notification (WinRT — only available in PS 5.1)
try {
    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
    $xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(0)
    $xml.GetElementsByTagName('text').Item(0).AppendChild($xml.CreateTextNode($msg)) | Out-Null
    [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Claude Code').Show(
        [Windows.UI.Notifications.ToastNotification]::new($xml)
    )
} catch {
    Hook-LogWarn "notify.ps1: Toast notification failed: $_"
}

exit 0
