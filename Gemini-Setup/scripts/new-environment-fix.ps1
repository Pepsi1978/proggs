# new-environment-fix.ps1 (Gemini) — Gefuehrter Environment-Fix-Prozess
param (
    [string]$State,
    [switch]$Json
)

$RepoRoot = "C:\Users\barwa\GeminiCLI"
$RegisterScript = "$RepoRoot\Gemini-Setup\scripts\register-environment-fix.mjs"

$RequiredFields = @(
    @{ key = "id"; prompt = "Fix-ID (z.B. 2026-03-24-fix-name)" },
    @{ key = "category"; prompt = "Kategorie (z.B. runtime-validation)" },
    @{ key = "summary"; prompt = "Kurzfassung" },
    @{ key = "context"; prompt = "Kontext fuer andere CLIs" },
    @{ key = "symptom"; prompt = "Symptom vor dem Fix" },
    @{ key = "root-cause"; prompt = "Root Cause" },
    @{ key = "what"; prompt = "Was wurde konkret gefixt" },
    @{ key = "why"; prompt = "Warum wurde es gefixt" },
    @{ key = "exact-error"; prompt = "Exakter Fehlertext" },
    @{ key = "why-chain"; prompt = "Why-Chain (3-5x Warum?)" },
    @{ key = "related-checks"; prompt = "Gepruefte verwandte Fehlerquellen" },
    @{ key = "wrong-pattern"; prompt = "Falsches Muster" },
    @{ key = "right-pattern"; prompt = "Richtiges Muster" },
    @{ key = "avoidance-rule"; prompt = "Vermeidungsregel" },
    @{ key = "resilience-summary"; prompt = "Resilienz-Zusammenfassung" },
    @{ key = "failure-review"; prompt = "Fix-Induced-Failure-Review" },
    @{ key = "verification"; prompt = "Verifikation" },
    @{ key = "portability-notes"; prompt = "Portierungshinweise" }
)

$OptionalFields = @(
    @{ key = "portable-to"; prompt = "Portable-to CSV"; defaultValue = "Claude Code, Codex" },
    @{ key = "artifacts"; prompt = "Artefakte CSV (Dateipfade)"; defaultValue = "" },
    @{ key = "impact"; prompt = "Impact (hoch/mittel/niedrig)"; defaultValue = "mittel" },
    @{ key = "source-cli"; prompt = "Source CLI"; defaultValue = "Gemini CLI" },
    @{ key = "status"; prompt = "Status"; defaultValue = "active" }
)

$EntryData = @{}

Write-Host "=== 💡 Gefuehrter Environment-Fix (Gemini CLI) ===" -ForegroundColor Cyan

foreach ($field in $RequiredFields) {
    $val = ""
    while ([string]::IsNullOrWhiteSpace($val)) {
        $val = Read-Host "$($field.prompt)"
    }
    $EntryData[$field.key] = $val
}

foreach ($field in $OptionalFields) {
    $suffix = if ($field.defaultValue) { " [Default: $($field.defaultValue)]" } else { "" }
    $val = Read-Host "$($field.prompt)$suffix"
    if ([string]::IsNullOrWhiteSpace($val)) {
        $EntryData[$field.key] = $field.defaultValue
    } else {
        $EntryData[$field.key] = $val
    }
}

$Arguments = @("run", "$RegisterScript", "add")
foreach ($key in $EntryData.Keys) {
    $Arguments += "--$key"
    $Arguments += $EntryData[$key]
}

if ($State) {
    $Arguments += "--state"
    $Arguments += $State
}

if ($Json) {
    $Arguments += "--json"
}

Write-Host "`nRegistriere Fix..." -ForegroundColor Yellow
& bun @Arguments
