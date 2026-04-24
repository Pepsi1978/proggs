# Experience Logger — SessionEnd Hook (Windows)
# Schreibt nach jeder nicht-trivialen Session einen Erfahrungs-Eintrag
# in experience-store.jsonl und eine Trajectory in trajectories.jsonl.
# Basierend auf JitRL/MemRL (arXiv 2601.18510) und AutoRefine (arXiv 2601.22758).
# Erweitert um LEGOMem-Felder (arXiv 2601.03192): utility_score, near_miss,
# task_category, timestamp_decay, tool_sequence.
#
# Direktive 3: Graceful Degradation — bei JEDEM Fehler exit 0, nie die Session blockieren.

try {
    $ErrorActionPreference = 'SilentlyContinue'

    # Pfade
    $experiencePath = Join-Path $env:USERPROFILE "proggs\.codex\agent-memory\shared\experience-store.jsonl"
    $trajectoryPath = Join-Path $env:USERPROFILE "proggs\.codex\agent-memory\shared\trajectories.jsonl"
    $scoresPath = Join-Path $env:USERPROFILE ".codex\session-scores.jsonl"

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

                $today = Get-Date -Format "yyyy-MM-dd"
                $toolCount = if ($scoreData.turns) { [int]$scoreData.turns } else { 0 }
                $errorCount = if ($scoreData.errors) { [int]$scoreData.errors } else { 0 }
                $successScore = if ($scoreData.overall) { [math]::Round([double]$scoreData.overall) } else { 3 }
                $taskType = if ($scoreData.task_type) { $scoreData.task_type } else { "general" }

                # ── LEGOMem: utility_score (SICA-Formel) ──
                # 0.5 * (overall/5) + 0.25 * max(0, 1 - errors/tools) + 0.25 * max(0, 1 - tools/50)
                $perfComponent = 0.5 * ($successScore / 5.0)
                $effComponent = 0.25 * [math]::Max(0, 1 - $errorCount / [math]::Max($toolCount, 1))
                $speedComponent = 0.25 * [math]::Max(0, 1 - $toolCount / 50.0)
                $utilityScore = [math]::Round($perfComponent + $effComponent + $speedComponent, 3)
                $utilityScore = [math]::Max(0.0, [math]::Min(1.0, $utilityScore))

                # ── LEGOMem: near_miss (Beinahe-Fehler mit hohem Lernwert) ──
                $nearMiss = ($successScore -ge 2 -and $successScore -le 3 -and $errorCount -gt 0)

                # ── LEGOMem: task_category (grobe Zuordnung) ──
                $taskTypeLower = $taskType.ToLower()
                if ($taskTypeLower -match 'build|compile|gradle|error|fix') {
                    $taskCategory = 'build_fix'
                } elseif ($taskTypeLower -match 'feature|implement|add|new') {
                    $taskCategory = 'feature'
                } elseif ($taskTypeLower -match 'config|setting|hook|setup') {
                    $taskCategory = 'config'
                } elseif ($taskTypeLower -match 'debug|bug|crash|investigate') {
                    $taskCategory = 'debug'
                } elseif ($taskTypeLower -match 'research|search|find|analyse|analyze') {
                    $taskCategory = 'research'
                } elseif ($taskTypeLower -match 'refactor|clean|improve|optimize') {
                    $taskCategory = 'refactor'
                } else {
                    $taskCategory = 'other'
                }

                # ── LEGOMem: tool_sequence ──
                $toolSequence = if ($scoreData.tool_sequence) { $scoreData.tool_sequence } else { @() }

                # Experience Store Eintrag (mit LEGOMem-Feldern)
                $experience = @{
                    date = $today
                    task_type = $taskType
                    task_description = if ($scoreData.task_description) { $scoreData.task_description } else { "session-auto-logged" }
                    strategy = if ($scoreData.strategy) { $scoreData.strategy } else { "auto-captured" }
                    tool_count = $toolCount
                    error_count = $errorCount
                    success_score = $successScore
                    tags = @("auto-logged")
                    # LEGOMem-Erweiterungen (arXiv 2601.03192)
                    tool_sequence = $toolSequence
                    utility_score = $utilityScore
                    near_miss = $nearMiss
                    task_category = $taskCategory
                    timestamp_decay = 1.0
                } | ConvertTo-Json -Compress

                Add-Content -Path $experiencePath -Value $experience -Encoding UTF8 -NoNewline:$false

                # Trajectory Eintrag (mit LEGOMem-Feldern)
                $trajectory = @{
                    date = $today
                    task_description = if ($scoreData.task_description) { $scoreData.task_description } else { "session-auto-logged" }
                    tool_sequence = $toolSequence
                    errors = @()
                    corrections = if ($scoreData.corrections) { [int]$scoreData.corrections } else { 0 }
                    duration_minutes = if ($scoreData.duration_minutes) { [int]$scoreData.duration_minutes } else { 0 }
                    success = $true
                    tags = @("auto-logged")
                    # LEGOMem-Erweiterungen
                    utility_score = $utilityScore
                    near_miss = $nearMiss
                } | ConvertTo-Json -Compress

                Add-Content -Path $trajectoryPath -Value $trajectory -Encoding UTF8 -NoNewline:$false

                # ── Near-Miss-aware Pruning ──
                # Near-Miss-Eintraege werden BEVORZUGT behalten (MemRL: hoher Lernwert)
                foreach ($item in @(@{Path=$experiencePath; Limit=200}, @{Path=$trajectoryPath; Limit=100})) {
                    $p = $item.Path
                    $limit = $item.Limit
                    if (Test-Path $p) {
                        $allLines = Get-Content $p -Encoding UTF8 | Where-Object { $_.Trim().Length -gt 0 }
                        if ($allLines.Count -gt $limit) {
                            $nearMissLines = @()
                            $normalLines = @()
                            foreach ($line in $allLines) {
                                try {
                                    $entry = $line | ConvertFrom-Json
                                    if ($entry.near_miss -eq $true) {
                                        $nearMissLines += $line
                                    } else {
                                        $normalLines += $line
                                    }
                                } catch {
                                    $normalLines += $line
                                }
                            }

                            # Zuerst alte normale Eintraege entfernen
                            $overage = $allLines.Count - $limit
                            if ($overage -gt 0 -and $normalLines.Count -gt 0) {
                                $removeCount = [math]::Min($overage, $normalLines.Count)
                                $normalLines = $normalLines[$removeCount..($normalLines.Count - 1)]
                            }

                            # Kombinieren und ggf. near_miss kuerzen falls Limit immer noch ueberschritten
                            $combined = $normalLines + $nearMissLines
                            if ($combined.Count -gt $limit) {
                                $combined = $combined[($combined.Count - $limit)..($combined.Count - 1)]
                            }

                            # Atomic write via temp file + rename
                            $tempPath = "$p.tmp"
                            $combined | Set-Content $tempPath -Encoding UTF8
                            Move-Item -Path $tempPath -Destination $p -Force
                        }
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
