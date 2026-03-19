# Dynamic notification for Windows: extracts the actual message from Claude Code
# MUST run under powershell.exe (5.1), NOT pwsh 7+ — WinRT Toast API requires it
# No hook-log.ps1 dependency (PS 5.1 incompatible with PS7 syntax in hook-log.ps1)

$hookInput = [Console]::In.ReadToEnd()
try {
    $json = $hookInput | ConvertFrom-Json
    $msg = $json.notification.message
} catch {
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
    # Silently fail — notification is non-critical
}
