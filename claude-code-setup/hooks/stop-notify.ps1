# stop-notify.ps1 — Fires immediately when Claude finishes work
# Hook event: Stop
# Purpose: Send BEL to terminal + fast Windows Toast notification (async)
# MUST be fast — hook returns instantly, toast launches in background

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

# Drain stdin (required by hook protocol)
try { [Console]::In.ReadToEnd() | Out-Null } catch { }

# --- 1. Terminal BEL (instant — triggers Windows Terminal "braucht deine Aufmerksamkeit") ---
try {
    [Console]::Error.Write([char]7)
} catch { }

# --- 2. Fast Toast notification (async — non-blocking) ---
# Launch powershell.exe (5.1) in background for WinRT toast, return immediately
$toastCmd = 'try{[Windows.UI.Notifications.ToastNotificationManager,Windows.UI.Notifications,ContentType=WindowsRuntime]|Out-Null;$x=[Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(0);$x.GetElementsByTagName(\"text\").Item(0).AppendChild($x.CreateTextNode(\"Claude Code ist fertig\"))|Out-Null;[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier(\"Claude Code\").Show([Windows.UI.Notifications.ToastNotification]::new($x))}catch{}'

Start-Process powershell.exe -WindowStyle Hidden -ArgumentList '-NoProfile', '-Command', $toastCmd

exit 0
