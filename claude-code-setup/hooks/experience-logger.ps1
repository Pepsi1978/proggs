# Experience Logger — SessionEnd Hook (Windows)
# Schreibt nach jeder nicht-trivialen Session einen Erfahrungs-Eintrag
# in experience-store.jsonl und eine Trajectory in trajectories.jsonl.
# Basierend auf JitRL/MemRL (arXiv 2601.18510) und AutoRefine (arXiv 2601.22758).
#
# Direktive 3: Graceful Degradation — bei JEDEM Fehler exit 0, nie die Session blockieren.

try {
    $ErrorActionPreference = 'SilentlyContinue'

    # Pfade
    $experiencePath = Join-Path $env:USERPROFILE "proggs\.claude\agent-memory\shared\experience-store.jsonl"
    $trajectoryPath = Join-Path $env:USERPROFILE "proggs\.claude\agent-memory\shared\trajectories.jsonl"
    $scoresPath = Join-Path $env:USERPROFILE ".claude\session-scores.jsonl"

    # Verzeichnis sicherstellen
    $dir = Split-Path $experiencePath -Parent
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }

    # Session-Scores lesen (letzte Zeile = aktuelle Session)
    if (Test-Path $scoresPath) {
        $lastScore = Get-Content $scoresPath -Tail 1 -ErrorAction SilentlyContinue
        if ($lastScore) {
            try {
                $scoreData = $lastScore | ConvertFrom-Json

                # Nur nicht-triviale Sessions (>5 Tool-Calls approximiert durch Score-Existenz)
                $today = Get-Date -Format "yyyy-MM-dd"

                # Experience Store Eintrag
                $experience = @{
                    date = $today
                    task_type = if ($scoreData.task_type) { $scoreData.task_type } else { "general" }
                    task_description = if ($scoreData.task_description) { $scoreData.task_description } else { "session-auto-logged" }
                    strategy = "auto-captured"
                    tool_count = if ($scoreData.turns) { $scoreData.turns } else { 0 }
                    error_count = if ($scoreData.errors) { $scoreData.errors } else { 0 }
                    success_score = if ($scoreData.overall) { [math]::Round($scoreData.overall) } else { 3 }
                    tags = @("auto-logged")
                } | ConvertTo-Json -Compress

                # Append to experience store (atomic via temp file on large writes)
                Add-Content -Path $experiencePath -Value $experience -Encoding UTF8 -NoNewline:$false

                # Trajectory Eintrag
                $trajectory = @{
                    date = $today
                    task_description = if ($scoreData.task_description) { $scoreData.task_description } else { "session-auto-logged" }
                    tool_sequence = @()
                    errors = @()
                    corrections = if ($scoreData.corrections) { $scoreData.corrections } else { 0 }
                    duration_minutes = if ($scoreData.duration_minutes) { $scoreData.duration_minutes } else { 0 }
                    success = $true
                    tags = @("auto-logged")
                } | ConvertTo-Json -Compress

                Add-Content -Path $trajectoryPath -Value $trajectory -Encoding UTF8 -NoNewline:$false

                # Pruning: Max 200 experience entries, max 100 trajectory entries
                if (Test-Path $experiencePath) {
                    $lineCount = (Get-Content $experiencePath -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
                    if ($lineCount -gt 200) {
                        $lines = Get-Content $experiencePath -Encoding UTF8
                        $lines | Select-Object -Last 200 | Set-Content $experiencePath -Encoding UTF8
                    }
                }

                if (Test-Path $trajectoryPath) {
                    $lineCount = (Get-Content $trajectoryPath -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
                    if ($lineCount -gt 100) {
                        $lines = Get-Content $trajectoryPath -Encoding UTF8
                        $lines | Select-Object -Last 100 | Set-Content $trajectoryPath -Encoding UTF8
                    }
                }

            } catch {
                # Graceful Degradation — score parsing failed, skip silently
            }
        }
    }
} catch {
    # Graceful Degradation — NEVER block session end
}

# PFLICHT: Immer exit 0
exit 0
