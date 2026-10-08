# einrichten.ps1
# Legt die Aufgabe "Explorer-Blinkschutz" an (idempotent, ohne Admin-Rechte). Sie startet blinkschutz.ps1
#   - sofort, wenn Windows das Ereignis 62441 (Standard-App zurueckgesetzt) protokolliert,
#   - bei jeder Anmeldung und alle 5 Minuten (startet ein gestopptes Programm nach der Reparatur wieder).
# Auf einem neuen Rechner einmal ausfuehren:
#   powershell -ExecutionPolicy Bypass -File C:\Users\barwa\proggs\Werkzeuge\explorer-blinkschutz\einrichten.ps1
$ErrorActionPreference = 'Stop'
$vbs = Join-Path $PSScriptRoot 'blinkschutz.vbs'
$user = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$xml = @"
<?xml version="1.0" encoding="UTF-16"?>
<Task version="1.2" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task">
  <RegistrationInfo>
    <Description>Stoppt das Symbol-Blinken im Explorer (Sturm von Standard-App-Resets). Siehe proggs\bugs\desktop\windows-explorer-blinken-assocchanged.md</Description>
  </RegistrationInfo>
  <Triggers>
    <EventTrigger>
      <Enabled>true</Enabled>
      <Subscription>&lt;QueryList&gt;&lt;Query Id="0" Path="Microsoft-Windows-Shell-Core/AppDefaults"&gt;&lt;Select Path="Microsoft-Windows-Shell-Core/AppDefaults"&gt;*[System[EventID=62441]]&lt;/Select&gt;&lt;/Query&gt;&lt;/QueryList&gt;</Subscription>
    </EventTrigger>
    <LogonTrigger>
      <Enabled>true</Enabled>
      <UserId>$user</UserId>
      <Delay>PT1M</Delay>
    </LogonTrigger>
    <TimeTrigger>
      <Repetition>
        <Interval>PT5M</Interval>
        <StopAtDurationEnd>false</StopAtDurationEnd>
      </Repetition>
      <StartBoundary>2026-01-01T00:00:00</StartBoundary>
      <Enabled>true</Enabled>
    </TimeTrigger>
  </Triggers>
  <Principals>
    <Principal id="Author">
      <UserId>$user</UserId>
      <LogonType>InteractiveToken</LogonType>
      <RunLevel>LeastPrivilege</RunLevel>
    </Principal>
  </Principals>
  <Settings>
    <MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>
    <DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>
    <StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>
    <StartWhenAvailable>true</StartWhenAvailable>
    <ExecutionTimeLimit>PT10M</ExecutionTimeLimit>
    <Enabled>true</Enabled>
    <Hidden>false</Hidden>
  </Settings>
  <Actions Context="Author">
    <Exec>
      <Command>wscript.exe</Command>
      <Arguments>"$vbs"</Arguments>
    </Exec>
  </Actions>
</Task>
"@
Register-ScheduledTask -TaskName 'Explorer-Blinkschutz' -Xml $xml -Force | Out-Null
$t = Get-ScheduledTask -TaskName 'Explorer-Blinkschutz'
"Aufgabe 'Explorer-Blinkschutz' eingerichtet (Status: $($t.State), Skript: $vbs)"
