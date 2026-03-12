# Dynamic notification for Windows: extracts the actual message from Claude Code
$input = [Console]::In.ReadToEnd()
try {
    $json = $input | ConvertFrom-Json
    $msg = $json.notification.message
} catch {
    $msg = $null
}
if (-not $msg) { $msg = "Braucht deine Aufmerksamkeit" }
if ($msg.Length -gt 200) { $msg = $msg.Substring(0, 200) }

# Windows Toast Notification
[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
$xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(0)
$xml.GetElementsByTagName('text')[0].AppendChild($xml.CreateTextNode($msg)) | Out-Null
[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Claude Code').Show(
    [Windows.UI.Notifications.ToastNotification]::new($xml)
)
