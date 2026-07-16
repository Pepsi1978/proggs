$ErrorActionPreference = 'Stop'

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

$root = Join-Path ([System.IO.Path]::GetTempPath()) "session-opencode-test-$PID"
New-Item -ItemType Directory -Path $root | Out-Null

try {
    $fakeHome = Join-Path $root 'home'
    $repo = Join-Path $fakeHome 'proggs'
    $remote = Join-Path $root 'remote.git'
    $remoteWriter = Join-Path $root 'remote-writer'
    $secondPc = Join-Path $root 'second-pc'
    $localBackup = Join-Path $fakeHome '.claude/session-opencode-backup.md'
    $repoBackup = Join-Path $repo '.claude/session-opencode-backup.md'
    $localDiff = Join-Path $fakeHome '.claude/session-opencode-backup.diff'
    $repoDiff = Join-Path $repo '.claude/session-opencode-backup.diff'

    $env:GIT_AUTHOR_NAME = 'Session OpenCode Test'
    $env:GIT_AUTHOR_EMAIL = 'session-opencode@example.invalid'
    $env:GIT_COMMITTER_NAME = $env:GIT_AUTHOR_NAME
    $env:GIT_COMMITTER_EMAIL = $env:GIT_AUTHOR_EMAIL

    git init -q -b main $repo
    git init -q --bare -b main $remote
    New-Item -ItemType Directory -Force (Join-Path $repo '.claude') | Out-Null
    '' | Set-Content -LiteralPath $repoBackup -Encoding utf8
    'base' | Set-Content -LiteralPath (Join-Path $repo 'foreign.txt')
    'base' | Set-Content -LiteralPath (Join-Path $repo 'work.txt')
    git -C $repo add -- .claude/session-opencode-backup.md foreign.txt work.txt
    git -C $repo commit -q -m baseline
    git -C $repo remote add origin $remote
    git -C $repo push -q -u origin main

    git clone -q $remote $remoteWriter
    '# Session Handoff (OpenCode) - alter Remote-Stand' | Set-Content -LiteralPath (Join-Path $remoteWriter '.claude/session-opencode-backup.md') -Encoding utf8
    git -C $remoteWriter commit --only -q -m 'remote advance' -- .claude/session-opencode-backup.md
    git -C $remoteWriter push -q origin HEAD:main

    New-Item -ItemType Directory -Force (Split-Path $localBackup), (Split-Path $repoBackup) | Out-Null
    @'
# Session Handoff (OpenCode) — Test

Umlaute: äöü
'@ | Set-Content -LiteralPath $localBackup -Encoding utf8
    git -C $repo fetch -q origin
    git -C $repo merge --ff-only origin/main | Out-Null
    Assert-True ($LASTEXITCODE -eq 0) 'Zurueckliegender zweiter PC wurde nicht per Fast-Forward aktualisiert.'
    Copy-Item -LiteralPath $localBackup -Destination $repoBackup -Force
    'diff payload' | Set-Content -LiteralPath $localDiff -Encoding utf8
    Copy-Item -LiteralPath $localDiff -Destination $repoDiff -Force
    Assert-True ((Get-FileHash $localBackup).Hash -eq (Get-FileHash $repoBackup).Hash) 'Lokales und Repo-Backup unterscheiden sich.'
    Assert-True (Select-String -LiteralPath $localBackup -Pattern 'Session Handoff' -Quiet) 'Handoff-Kopf fehlt.'

    'fremde staged Aenderung' | Set-Content -LiteralPath (Join-Path $repo 'foreign.txt')
    git -C $repo add -- foreign.txt
    'halbfertige Arbeit' | Set-Content -LiteralPath (Join-Path $repo 'work.txt')
    git -C $repo fetch -q origin
    git -C $repo merge-base --is-ancestor origin/main HEAD
    Assert-True ($LASTEXITCODE -eq 0) 'Remote-Stand ist kein Vorfahr des lokalen Branches.'
    git -C $repo add -N -- .claude/session-opencode-backup.diff
    git -C $repo commit --only -q -m 'session-opencode: handoff snapshot' -- .claude/session-opencode-backup.md .claude/session-opencode-backup.diff
    $committed = @(git -C $repo show --pretty='' --name-only HEAD | Where-Object { $_ })
    Assert-True (($committed.Count -eq 2) -and ($committed -contains '.claude/session-opencode-backup.md') -and ($committed -contains '.claude/session-opencode-backup.diff')) 'Backup-Commit ist unvollstaendig oder enthaelt fremde Dateien.'
    Assert-True ((git -C $repo diff --cached --name-only) -contains 'foreign.txt') 'Fremde staged Aenderung wurde veraendert.'
    Assert-True ((git -C $repo diff --name-only) -contains 'work.txt') 'Halbfertige Arbeit wurde veraendert.'
    git -C $repo push -q origin HEAD:main

    git clone -q $remote $secondPc
    Assert-True (Select-String -LiteralPath (Join-Path $secondPc '.claude/session-opencode-backup.md') -Pattern 'Session Handoff' -Quiet) 'Backup fehlt auf dem zweiten PC.'
    Assert-True (Test-Path -LiteralPath (Join-Path $secondPc '.claude/session-opencode-backup.diff')) 'Ausgelagerter Diff fehlt auf dem zweiten PC.'

    Clear-Content -LiteralPath $localBackup
    Clear-Content -LiteralPath $repoBackup
    Remove-Item -LiteralPath $localDiff, $repoDiff -Force
    git -C $repo commit --only -q -m 'session-opencode: clear restored handoff' -- .claude/session-opencode-backup.md .claude/session-opencode-backup.diff
    git -C $repo push -q origin HEAD:main
    git -C $secondPc pull -q --ff-only
    Assert-True ((Get-Item (Join-Path $secondPc '.claude/session-opencode-backup.md')).Length -eq 0) 'Remote-Backup wurde nach Restore nicht geleert.'
    Assert-True (-not (Test-Path -LiteralPath (Join-Path $secondPc '.claude/session-opencode-backup.diff'))) 'Remote-Diff wurde nach Restore nicht geloescht.'

    'PASS: PowerShell Backup, isolierter Commit, Zweit-PC-Restore und Cleanup'
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force -ErrorAction SilentlyContinue
}
