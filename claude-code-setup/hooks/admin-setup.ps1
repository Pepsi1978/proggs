# Admin Setup Script — Run this ONCE in an elevated PowerShell (Run as Administrator)
# This script sets up everything that requires admin privileges.

Write-Host "=== Admin Setup for Development Environment ===" -ForegroundColor Cyan
Write-Host ""

# 1. Enable Developer Mode
Write-Host "[1/4] Enabling Developer Mode..." -ForegroundColor Yellow
try {
    $key = "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock"
    if (-not (Test-Path $key)) { New-Item -Path $key -Force | Out-Null }
    Set-ItemProperty -Path $key -Name "AllowDevelopmentWithoutDevLicense" -Value 1
    Set-ItemProperty -Path $key -Name "AllowAllTrustedApps" -Value 1
    Write-Host "  Developer Mode ENABLED" -ForegroundColor Green
} catch {
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Update Git for Windows (security fix CVE-2025-66413)
Write-Host "[2/4] Updating Git for Windows (security fix)..." -ForegroundColor Yellow
try {
    winget upgrade Git.Git --silent --accept-package-agreements --accept-source-agreements
    Write-Host "  Git updated successfully" -ForegroundColor Green
} catch {
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Set Execution Policy machine-wide
Write-Host "[3/4] Setting PowerShell Execution Policy..." -ForegroundColor Yellow
try {
    Set-ExecutionPolicy RemoteSigned -Scope LocalMachine -Force
    Write-Host "  Execution Policy set to RemoteSigned (LocalMachine)" -ForegroundColor Green
} catch {
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Trust the code signing certificate machine-wide
Write-Host "[4/4] Trusting code signing certificate machine-wide..." -ForegroundColor Yellow
try {
    $cert = Get-ChildItem Cert:\CurrentUser\My -CodeSigningCert | Where-Object { $_.Subject -like "*Pepsi1978*" } | Select-Object -First 1
    if ($cert) {
        $store = New-Object System.Security.Cryptography.X509Certificates.X509Store("Root", "LocalMachine")
        $store.Open("ReadWrite")
        $store.Add($cert)
        $store.Close()

        $pubStore = New-Object System.Security.Cryptography.X509Certificates.X509Store("TrustedPublisher", "LocalMachine")
        $pubStore.Open("ReadWrite")
        $pubStore.Add($cert)
        $pubStore.Close()
        Write-Host "  Certificate trusted machine-wide (Thumbprint: $($cert.Thumbprint))" -ForegroundColor Green
    } else {
        Write-Host "  No Pepsi1978 code signing cert found in CurrentUser\My" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Setup Complete ===" -ForegroundColor Cyan
Write-Host "Restart your terminal for PATH changes to take effect." -ForegroundColor Yellow
