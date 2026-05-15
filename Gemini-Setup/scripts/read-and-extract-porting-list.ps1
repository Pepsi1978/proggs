# read-and-extract-porting-list.ps1 (Gemini) - Extrahiert Details aus fremden Briefkaesten
$RepoRoot = $env:GEMINI_WORKSPACE
if (-not $RepoRoot) { $RepoRoot = "C:\Users\barwa\GeminiCLI" }

$PortingPaths = @(
    @{ name = "Codex"; path = "$RepoRoot\codex-setup\PORTING-LIST.md" },
    @{ name = "Claude Code"; path = "$RepoRoot\claude-code-setup\PORTING-LIST.md" }
)

Write-Host "=== Gemini Porting-Inbox Extraktor ===" -ForegroundColor Cyan

foreach ($cp in $PortingPaths) {
    if (Test-Path $cp.path) {
        Write-Host "Extrahiere neue Ideen von $($cp.name)..." -ForegroundColor Yellow
        $content = Get-Content $cp.path -Raw
        
        # Suche nach Vorschlag-Bloecken ### [ID]
        $matches = [regex]::Matches($content, "###\s+\[(.*?)\]([\s\S]*?)(?=###|##|$)")
        
        foreach ($match in $matches) {
            $id = $match.Groups[1].Value.Trim()
            $body = $match.Groups[2].Value.Trim()
            
            $Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm"
            $Entry = "### $Timestamp - PORTIERUNG von $($cp.name): [$id]`n$body"
            
            # Extrahiere Summary fuer Fuzzy Match
            $summaryLine = $body.Split("`n")[0].Trim()

            # Nutze den zentralen Dubletten-Check
            $checkScript = "$RepoRoot\Gemini-Setup\scripts\check-for-duplicate-suggestions.ps1"
            powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$checkScript" -SuggestionId $id -Summary $summaryLine
            $checkStatus = $LASTEXITCODE

            if ($checkStatus -eq 0) {
                # Direkter Schreibzugriff aufs Whiteboard (als Fallback)
                $memoryFile = "$RepoRoot\Gemini-Setup\agent-memory\shared\MEMORY.md"
                $lines = Get-Content $memoryFile -Encoding UTF8
                $newLines = [System.Collections.ArrayList]@($lines)
                
                # Suche Sektion "Offene Fehler & Probleme"
                $sectionIdx = -1
                for ($i = 0; $i -lt $lines.Count; $i++) {
                    if ($lines[$i] -match "## Offene Fehler & Probleme") {
                        $sectionIdx = $i
                        break
                    }
                }
                
                if ($sectionIdx -ge 0) {
                    $insertIdx = $sectionIdx + 1
                    # Suche Ende der Sektion oder Platzhalter
                    for ($j = $sectionIdx + 1; $j -lt $lines.Count; $j++) {
                        if ($lines[$j] -match "^## " -or $lines[$j] -match "^---") { $insertIdx = $j; break }
                        if ($lines[$j] -match "_Noch keine Eintraege._") { 
                            $newLines.RemoveAt($j)
                            $insertIdx = $j
                            break 
                        }
                        $insertIdx = $j + 1
                    }
                    $newLines.Insert($insertIdx, $Entry)
                    Set-Content $memoryFile -Value $newLines.ToArray() -Encoding UTF8
                    Write-Host "  OK: [$id] ins Whiteboard uebernommen." -ForegroundColor Green
                }
            } elseif ($checkStatus -eq 1) {
                Write-Host "  SKIP: [$id] bereits im Whiteboard bekannt." -ForegroundColor DarkGray
            } elseif ($checkStatus -eq 2) {
                Write-Host "  DONE: [$id] bereits erfolgreich umgesetzt." -ForegroundColor Cyan
            } elseif ($checkStatus -eq 3) {
                Write-Host "  FUZZY: [$id] uebersprungen (aehnliche Idee existiert)." -ForegroundColor Yellow
            }
        }
    }
}
