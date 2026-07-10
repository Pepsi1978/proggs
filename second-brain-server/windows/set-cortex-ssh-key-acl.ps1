param(
    [string]$KeyPath = (Join-Path $HOME 'SK\second-brain\id_ed25519'),
    [string]$Server = '168.231.83.205',
    [string]$SshUser = 'root',
    [switch]$SkipSshTest
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $KeyPath -PathType Leaf)) {
    throw "SSH key missing: $KeyPath"
}

$identity = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$allowedSids = @(
    $identity.User,
    [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'),
    [System.Security.Principal.SecurityIdentifier]::new('S-1-5-32-544')
)

function Invoke-Icacls([string[]]$Arguments) {
    & icacls.exe @Arguments | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "icacls failed with exit code ${LASTEXITCODE}: $($Arguments -join ' ')"
    }
}

Invoke-Icacls @($KeyPath, '/reset')
Invoke-Icacls @($KeyPath, '/inheritance:r')
Invoke-Icacls @($KeyPath, '/grant:r', "*$($identity.User.Value):(F)", '*S-1-5-18:(F)', '*S-1-5-32-544:(F)')

$verified = Get-Acl -LiteralPath $KeyPath
$actual = @($verified.Access | ForEach-Object {
    $_.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value
} | Sort-Object -Unique)
$expected = @($allowedSids.Value | Sort-Object -Unique)
$difference = @(Compare-Object -ReferenceObject $expected -DifferenceObject $actual)
$invalidRule = @($verified.Access | Where-Object {
    $_.AccessControlType -ne [System.Security.AccessControl.AccessControlType]::Allow -or
    ($_.FileSystemRights -band [System.Security.AccessControl.FileSystemRights]::FullControl) -ne
        [System.Security.AccessControl.FileSystemRights]::FullControl
})
if ($difference.Count -gt 0 -or $invalidRule.Count -gt 0 -or -not $verified.AreAccessRulesProtected) {
    throw 'SSH key ACL verification failed.'
}

Write-Host "ACL_OK: $KeyPath"
Write-Host "Allowed SIDs: $($expected -join ', ')"

if (-not $SkipSshTest) {
    $ssh = (Get-Command ssh.exe -ErrorAction Stop).Source
    $result = & $ssh -o BatchMode=yes -o ConnectTimeout=15 -o StrictHostKeyChecking=accept-new `
        -i $KeyPath "$SshUser@$Server" "printf 'SSH_ACL_OK'"
    if ($LASTEXITCODE -ne 0 -or ($result -join '') -ne 'SSH_ACL_OK') {
        throw "SSH smoke test failed for $SshUser@$Server."
    }
    Write-Host 'SSH_ACL_OK'
}
