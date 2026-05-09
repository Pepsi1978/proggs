# statusline.ps1 — Schoene Statusline mit Icons + Fortschrittsbalken
# Reihenfolge: Modell | Effort | Ordner | 5h-Balken | 7d-Balken | Context-Balken | Zeit
# Cross-Platform-Pendant zu statusline.sh
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$input_raw = [Console]::In.ReadToEnd()
$obj = $null
if ($input_raw) {
    try { $obj = $input_raw | ConvertFrom-Json } catch { $obj = $null }
}

# Effort: ZUERST aus Stdin (.effort.level) — das ist der LIVE-Session-Wert,
# der von /effort low/medium/high/xhigh aktualisiert wird. settings.json haelt
# nur den Default fuer den Session-Start. Frueher hat die Statusline NUR aus
# settings.json gelesen und deshalb "HIGH" angezeigt obwohl die Session auf
# "xhigh" stand (Frank-Bug-Report 2026-05-09 Abend).
$effort = $null
if ($obj -and $obj.effort -and $obj.effort.level) {
    $effort = [string]$obj.effort.level
}
if (-not $effort) {
    try {
        $settingsPath = Join-Path $env:USERPROFILE '.claude\settings.json'
        $settings = Get-Content -Raw -Encoding UTF8 -Path $settingsPath | ConvertFrom-Json
        if ($settings.effortLevel) { $effort = [string]$settings.effortLevel }
    } catch { }
}
if (-not $effort) { $effort = '?' }
$effort_upper = $effort.ToUpper()

# Felder
$model = '?'
$cwd = ''
$ctx_remaining = $null
$five_h_used = $null
$five_h_resets = 0
$week_used = $null

if ($obj) {
    if ($obj.model.display_name) { $model = $obj.model.display_name }
    if ($obj.workspace.current_dir) {
        $cwd = $obj.workspace.current_dir
        $homeDir = $env:USERPROFILE
        if ($homeDir -and $cwd.StartsWith($homeDir)) {
            $cwd = ('~' + $cwd.Substring($homeDir.Length)) -replace '\\', '/'
        }
        # Smart-truncate fuer den Ordner-Pfad (Variante B):
        # - <= 35 Zeichen: unveraendert
        # - > 35 Zeichen: ~/proggs/<Projekt>/…/<vorletztes>/<letztes>
        $maxLen = 35
        if ($cwd.Length -gt $maxLen) {
            $segs = $cwd.Split('/')
            $n = $segs.Length
            if ($n -ge 5) {
                if ($segs[0] -eq '~' -and $segs[1] -eq 'proggs' -and $n -ge 5) {
                    $cwd = "~/proggs/$($segs[2])/…/$($segs[$n-2])/$($segs[$n-1])"
                } else {
                    $cwd = "$($segs[0])/$($segs[1])/…/$($segs[$n-2])/$($segs[$n-1])"
                }
            }
        }
    }
    if ($obj.context_window.remaining_percentage -ne $null) {
        $ctx_remaining = [int][Math]::Round($obj.context_window.remaining_percentage)
    }
    if ($obj.rate_limits.five_hour.used_percentage -ne $null) {
        $five_h_used = [int][Math]::Round($obj.rate_limits.five_hour.used_percentage)
    }
    if ($obj.rate_limits.five_hour.resets_at) {
        $five_h_resets = [long]$obj.rate_limits.five_hour.resets_at
    }
    if ($obj.rate_limits.seven_day.used_percentage -ne $null) {
        $week_used = [int][Math]::Round($obj.rate_limits.seven_day.used_percentage)
    }
}

$ctx_used = $null
if ($ctx_remaining -ne $null) { $ctx_used = 100 - $ctx_remaining }

$five_h_countdown = ''
if ($five_h_resets -gt 0) {
    $now = [int][double]::Parse((Get-Date -UFormat %s))
    $diff = $five_h_resets - $now
    if ($diff -gt 0) {
        $mins = [int]($diff / 60)
        if ($mins -ge 60) {
            $h = [int]($mins / 60); $m = $mins % 60
            $five_h_countdown = "${h}h${m}m"
        } else {
            $five_h_countdown = "${mins}m"
        }
    }
}

$time = Get-Date -Format 'HH:mm'

# Farben (ANSI 24-bit)
$ESC = [char]27
$B      = "$ESC[38;2;100;180;255m"
$P      = "$ESC[38;2;30;144;255m"
$GREEN  = "$ESC[38;2;64;200;90m"
$YELLOW = "$ESC[38;2;255;190;40m"
$RED    = "$ESC[38;2;240;70;70m"
$T      = "$ESC[38;2;130;135;160m"
$TIMECOL= "$ESC[38;2;220;180;100m"
$DIM    = "$ESC[38;2;90;95;115m"
$TRACK  = "$ESC[38;2;55;58;75m"
$BOLD   = "$ESC[1m"
$R      = "$ESC[0m"

# Effort-Farbe
$EFFORT_COL = switch ($effort) {
    'xhigh'  { "$ESC[38;2;255;90;220m" }   # Magenta — extra-hoch
    'high'   { "$ESC[38;2;255;180;30m" }   # Gold
    'medium' { "$ESC[38;2;100;180;255m" }  # Cyan
    'low'    { "$ESC[38;2;130;135;160m" }  # Grau
    default  { "$ESC[38;2;130;135;160m" }
}

function Get-PctColor($pct) {
    if ($pct -eq $null) { return $T }
    if ($pct -ge 80) { return $RED }
    if ($pct -ge 50) { return $YELLOW }
    return $GREEN
}

function Get-Bar($pct, $col) {
    if ($pct -eq $null) { $pct = 0 }
    if ($pct -gt 100) { $pct = 100 }
    if ($pct -lt 0) { $pct = 0 }
    $filled = [int]($pct / 10)
    $empty = 10 - $filled
    return "${col}" + ('█' * $filled) + "${TRACK}" + ('░' * $empty) + "${R}"
}

$SEP = "${DIM} | ${R}"
$EMPTY_BAR = "${TRACK}" + ('░' * 10) + "${R}"

# Icons
$ICON_MODEL  = '🤖'
$ICON_EFFORT = '⚡'
$ICON_DIR    = '📁'
$ICON_5H     = '⏱'
$ICON_7D     = '📅'
$ICON_CTX    = '🧠'
$ICON_TIME   = '🕐'

# Ausgabe
$out = "${B}${ICON_MODEL} ${BOLD}${model}${R}"
$out += "${SEP}${EFFORT_COL}${ICON_EFFORT} ${effort_upper}${R}"

if ($cwd) {
    $out += "${SEP}${P}${ICON_DIR} ${cwd}${R}"
}

# 5h
if ($five_h_used -ne $null) {
    $col = Get-PctColor $five_h_used
    $bar = Get-Bar $five_h_used $col
    if ($five_h_countdown) {
        $out += "${SEP}${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%${R} ${DIM}(${five_h_countdown})${R}"
    } else {
        $out += "${SEP}${col}${ICON_5H} 5h${R} ${bar} ${col}${five_h_used}%${R}"
    }
} else {
    $out += "${SEP}${DIM}${ICON_5H} 5h${R} ${EMPTY_BAR} ${DIM}--${R}"
}

# 7d
if ($week_used -ne $null) {
    $col = Get-PctColor $week_used
    $bar = Get-Bar $week_used $col
    $out += "${SEP}${col}${ICON_7D} 7d${R} ${bar} ${col}${week_used}%${R}"
} else {
    $out += "${SEP}${DIM}${ICON_7D} 7d${R} ${EMPTY_BAR} ${DIM}--${R}"
}

# Context
if ($ctx_used -ne $null) {
    $col = Get-PctColor $ctx_used
    $bar = Get-Bar $ctx_used $col
    $out += "${SEP}${col}${ICON_CTX} ctx${R} ${bar} ${col}${ctx_used}%${R}"
}

# Uhrzeit
$out += "${SEP}${TIMECOL}${ICON_TIME} ${time}${R}"

[Console]::Out.Write($out)
