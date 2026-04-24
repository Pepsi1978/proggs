# statusline.ps1 — Effort, path, git, context, rate-limits, version, tokens, agent, commit, todos, model, time
# Cross-Platform-Pendant zu statusline.sh (volle Feature-Parity)
$ErrorActionPreference = 'SilentlyContinue'

# JSON-Input einlesen
$input_raw = [Console]::In.ReadToEnd()
$obj = $null
if ($input_raw) {
    try { $obj = $input_raw | ConvertFrom-Json } catch { $obj = $null }
}

# Effort-Level aus settings.json lesen
$effort = '?'
try {
    $settingsPath = Join-Path $env:USERPROFILE '.claude\settings.json'
    $settings = Get-Content -Raw -Encoding UTF8 -Path $settingsPath | ConvertFrom-Json
    if ($settings.effortLevel) { $effort = $settings.effortLevel }
} catch { }

# JSON-Felder extrahieren
$model = '?'
$remaining = ''
$cwd = ''
$version = ''
$tokens_in = ''
$tokens_out = ''
$agent_name = ''
$five_h_used = ''
$five_h_resets = 0
$week_used = ''
$work_dir = ''

if ($obj) {
    if ($obj.model.display_name) { $model = $obj.model.display_name }
    if ($obj.context_window.remaining_percentage -ne $null) { $remaining = $obj.context_window.remaining_percentage }
    if ($obj.workspace.current_dir) {
        $cwd = $obj.workspace.current_dir
        $work_dir = $cwd
        $homeDir = $env:USERPROFILE
        if ($homeDir -and $cwd.StartsWith($homeDir)) {
            $cwd = ('~' + $cwd.Substring($homeDir.Length)) -replace '\\', '/'
        }
    }
    if ($obj.version) { $version = $obj.version }
    if ($obj.context_window.total_input_tokens) { $tokens_in = $obj.context_window.total_input_tokens }
    if ($obj.context_window.total_output_tokens) { $tokens_out = $obj.context_window.total_output_tokens }
    if ($obj.agent.name) { $agent_name = $obj.agent.name }
    if ($obj.rate_limits.five_hour.used_percentage -ne $null) { $five_h_used = [int][Math]::Round($obj.rate_limits.five_hour.used_percentage) }
    if ($obj.rate_limits.five_hour.resets_at) { $five_h_resets = [long]$obj.rate_limits.five_hour.resets_at }
    if ($obj.rate_limits.seven_day.used_percentage -ne $null) { $week_used = [int][Math]::Round($obj.rate_limits.seven_day.used_percentage) }
}

# 5h Reset-Countdown
$five_h_countdown = ''
if ($five_h_resets -gt 0) {
    $now = [int][double]::Parse((Get-Date -UFormat %s))
    $diff = $five_h_resets - $now
    if ($diff -gt 0) {
        $mins = [int]($diff / 60)
        if ($mins -ge 60) {
            $h = [int]($mins / 60)
            $m = $mins % 60
            $five_h_countdown = "${h}h${m}m"
        } else {
            $five_h_countdown = "${mins}m"
        }
    }
}

# Git: Branch + Dirty + letzter Commit
$branch = ''
$gstatus = ''
$last_commit = ''
if ($work_dir -and (Test-Path $work_dir)) {
    Push-Location $work_dir
    try {
        $branch = (git rev-parse --abbrev-ref HEAD 2>$null)
        if ($branch) {
            $dirty = git status --porcelain 2>$null | Select-Object -First 1
            if ($dirty) { $gstatus = '*' }
            $lc = git log --oneline -1 2>$null
            if ($lc) {
                if ($lc.Length -gt 40) { $lc = $lc.Substring(0, 40) }
                $last_commit = $lc -replace ' ', ':'
                if ($last_commit.Length -gt 38) { $last_commit = $last_commit.Substring(0, 38) }
            }
        }
    } catch { }
    Pop-Location
}

# TODOs in Kotlin-Quellen
$todos = 0
if ($work_dir -and (Test-Path (Join-Path $work_dir 'app\src'))) {
    try {
        $todos = (Get-ChildItem -Path (Join-Path $work_dir 'app\src') -Filter *.kt -Recurse -File 2>$null |
            Select-String -Pattern 'TODO|FIXME' -List 2>$null | Measure-Object).Count
    } catch { $todos = 0 }
}

# Uhrzeit
$time = Get-Date -Format 'HH:mm'

# Farben (ANSI 24-bit)
$ESC = [char]27
$E  = "$ESC[38;2;255;215;0m"   # Gold   — Effort
$B  = "$ESC[38;2;30;102;245m"  # Blau   — Pfad
$G  = "$ESC[38;2;64;160;43m"   # Gruen  — Git-Branch
$Y  = "$ESC[38;2;223;142;29m"  # Amber  — Dirty, Zeit, TODOs
$M  = "$ESC[38;2;136;57;239m"  # Lila   — Context
$RL = "$ESC[38;2;0;200;150m"   # Teal   — Rate normal
$RW = "$ESC[38;2;255;140;0m"   # Orange — Rate warning
$RC = "$ESC[38;2;220;50;50m"   # Rot    — Rate kritisch
$T  = "$ESC[38;2;76;79;105m"   # Grau   — Modell, Version, Commit
$C  = "$ESC[38;2;100;180;255m" # Cyan   — Tokens
$A  = "$ESC[38;2;255;100;200m" # Pink   — Aktiver Agent
$R  = "$ESC[0m"                # Reset

function Get-RateColor($pct) {
    if (($null -eq $pct) -or ($pct -isnot [int])) { return $RL }
    if ($pct -ge 90) { return $RC }
    if ($pct -ge 70) { return $RW }
    return $RL
}

function Format-Tokens($n) {
    if (-not $n) { return '0' }
    $v = [int]$n
    if ($v -ge 1000) { return "$([int]($v / 1000))K" }
    return "$v"
}

$effort_upper = $effort.ToUpper()

# Ausgabe aufbauen
$out = "${E}[${effort_upper}]${R}"
if ($cwd)       { $out += " ${B}${cwd}${R}" }
if ($branch)    { $out += " ${G}${branch}${Y}${gstatus}${R}" }
if ($remaining -ne '') { $out += " ${M}ctx:${remaining}%${R}" }
if ($agent_name) { $out += " ${A}[${agent_name}]${R}" }

if ($five_h_used -ne '') {
    $col = Get-RateColor $five_h_used
    if ($five_h_countdown) {
        $out += " ${col}5h:${five_h_used}%(${five_h_countdown})${R}"
    } else {
        $out += " ${col}5h:${five_h_used}%${R}"
    }
}
if ($week_used -ne '') {
    $col = Get-RateColor $week_used
    $out += " ${col}7d:${week_used}%${R}"
}

if ($tokens_in -or $tokens_out) {
    $ti = Format-Tokens $tokens_in
    $to = Format-Tokens $tokens_out
    $out += " ${C}tok:${ti}/${to}${R}"
}

if ($todos -gt 0) { $out += " ${Y}TODO:${todos}${R}" }
if ($last_commit) { $out += " ${T}${last_commit}${R}" }
$out += " ${T}${model}${R}"
if ($version) { $out += " ${T}v${version}${R}" }
$out += " ${Y}${time}${R}"

[Console]::Out.Write($out)
