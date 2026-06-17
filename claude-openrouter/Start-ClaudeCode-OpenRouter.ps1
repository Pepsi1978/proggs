<#
================================================================================
  Claude Code (OpenRouter) — Startmenue
================================================================================
  Was dieses Skript tut (in einfachen Worten):
   1. Liest deinen OpenRouter-Schluessel aus ~/SK/ClaudeCodeOpenRouter/openrouter.key
   2. Holt LIVE die aktuelle Modell-Liste von OpenRouter (immer aktuell)
   3. Zeigt dir zwei Listen zur Auswahl:
        - Liste 1: Programmier-Modelle, nach Staerke sortiert (Terminal/Agenten)
        - Liste 2: ALLE Modelle nach Kategorie
   4. Wenn du ein Modell waehlst: konfiguriert den claude-code-router (ccr) dafuer
      und startet dein komplettes Claude Code damit in ~/proggs.
   Dein normales Claude Code (Opus-Abo) bleibt davon voellig unberuehrt.

  Bedienung: Doppelklick auf die Desktop-Verknuepfung. Sonst nichts noetig.
================================================================================
#>

param([string]$SelfTest = '')   # Interner Selbsttest: laeuft bis kurz vor 'ccr code' OHNE Menue/Start (z.B. -SelfTest 'minimax/minimax-m3')

$ErrorActionPreference = 'Stop'
$script:DryRun = [bool]$SelfTest
try { [Console]::OutputEncoding = [Text.Encoding]::UTF8 } catch {}
try { $OutputEncoding = [Text.Encoding]::UTF8 } catch {}

# ---------- Pfade ----------
$ProjectDir   = Join-Path $HOME 'proggs\claude-openrouter'
$WorkDir      = Join-Path $HOME 'proggs'
$KeyFile      = Join-Path $HOME 'SK\ClaudeCodeOpenRouter\openrouter.key'
$ModelsJson   = Join-Path $ProjectDir 'coding-models.json'
$CcrConfigDir = Join-Path $HOME '.claude-code-router'
$CcrConfig    = Join-Path $CcrConfigDir 'config.json'
$LogDir       = Join-Path $ProjectDir 'logs'
$CacheFile    = Join-Path $LogDir 'models-cache.json'
$LastModel    = Join-Path $ProjectDir 'last-model.txt'
$OverrideSettings = Join-Path $LogDir 'session-settings.json'
$LeanConfigDir = Join-Path $HOME '.claude-openrouter'   # schlankes Claude-Profil: nur Direktive #3 + eigene Skills, kein Plugin-/Memory-Ballast

if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Force -Path $LogDir | Out-Null }
$LogFile = Join-Path $LogDir ("openrouter-launcher-{0}.jsonl" -f (Get-Date -Format 'yyyy-MM-dd'))

# ---------- Observability: strukturiertes JSON-Log ----------
function Write-Log {
    param([string]$Level, [string]$Event, [hashtable]$Ctx = @{})
    try {
        $entry = [ordered]@{
            ts    = (Get-Date).ToString('o')
            level = $Level
            event = $Event
            ctx   = $Ctx
        }
        $line = $entry | ConvertTo-Json -Compress -Depth 6
        Add-Content -Path $LogFile -Value $line -Encoding utf8
    } catch { }  # Logging darf NIE den Start blockieren
}

function Write-Probe {
    # Live-Logik-Sonde: erwartet vs. tatsaechlich (Intent-Verifikation)
    param([string]$Step, [string]$Intent, $Expected, $Actual, [bool]$Ok, [hashtable]$Ctx = @{})
    try {
        $entry = [ordered]@{
            ts = (Get-Date).ToString('o'); kind = 'CHECKPOINT'
            step = $Step; intent = $Intent; expected = "$Expected"; actual = "$Actual"; ok = $Ok; ctx = $Ctx
        }
        Add-Content -Path $LogFile -Value ($entry | ConvertTo-Json -Compress -Depth 6) -Encoding utf8
    } catch { }
}

function Write-Header {
    if (-not $script:DryRun) { try { Clear-Host } catch {} }
    Write-Host ""
    Write-Host "  ┌────────────────────────────────────────────────────────────┐" -ForegroundColor DarkCyan
    Write-Host "  │   Claude Code  ·  OpenRouter-Modus                          │" -ForegroundColor Cyan
    Write-Host "  │   Schlank: nur Direktive #3 + deine Skills, dein Modell    │" -ForegroundColor DarkCyan
    Write-Host "  └────────────────────────────────────────────────────────────┘" -ForegroundColor DarkCyan
    Write-Host ""
}

function Stop-WithMessage {
    param([string]$Msg)
    Write-Host ""
    Write-Host "  $Msg" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "  [Enter] zum Schliessen"
    exit 1
}

# ---------- Schluessel laden ----------
function Get-ApiKey {
    if (-not (Test-Path $KeyFile)) {
        Write-Log 'ERROR' 'key_file_missing' @{ path = $KeyFile }
        try { Start-Process notepad.exe $KeyFile } catch {}
        Stop-WithMessage "Schluessel-Datei fehlt. Erwartet: $KeyFile"
    }
    $key = $null
    foreach ($line in (Get-Content $KeyFile -Encoding utf8)) {
        $t = $line.Trim()
        if ($t -and -not $t.StartsWith('#') -and $t.StartsWith('sk-or-')) { $key = $t; break }
    }
    if (-not $key -or $key -like '*HIER-DEINEN*') {
        Write-Log 'ERROR' 'key_not_set' @{ path = $KeyFile }
        Write-Host "  Dein OpenRouter-Schluessel ist noch nicht eingetragen." -ForegroundColor Yellow
        Write-Host "  Ich oeffne dir jetzt die Datei — trage deinen Schluessel ein," -ForegroundColor Yellow
        Write-Host "  speichere, schliesse den Editor und starte diese Verknuepfung erneut." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  Schluessel holen:  https://openrouter.ai/settings/keys" -ForegroundColor Cyan
        try { Start-Process notepad.exe $KeyFile } catch {}
        Stop-WithMessage "Datei: $KeyFile"
    }
    Write-Probe 'key_loaded' 'Gueltiger sk-or-Schluessel geladen' 'sk-or-...' ($key.Substring(0,[Math]::Min(12,$key.Length)) + '...') $true
    return $key
}

# ---------- Modelle laden (live, mit kurzem Cache) ----------
function Get-Models {
    param([string]$ApiKey)
    # Cache (10 Min) — schneller Start, trotzdem aktuell
    if (Test-Path $CacheFile) {
        $age = (Get-Date) - (Get-Item $CacheFile).LastWriteTime
        if ($age.TotalMinutes -lt 10) {
            try {
                $cached = Get-Content $CacheFile -Raw -Encoding utf8 | ConvertFrom-Json
                if ($cached.data.Count -gt 0) { Write-Log 'INFO' 'models_from_cache' @{ count = $cached.data.Count }; return $cached.data }
            } catch {}
        }
    }
    Write-Host "  Lade aktuelle Modell-Liste von OpenRouter ..." -ForegroundColor DarkGray
    try {
        $headers = @{ 'Authorization' = "Bearer $ApiKey"; 'HTTP-Referer' = 'https://localhost'; 'X-Title' = 'Claude Code OpenRouter Launcher' }
        $resp = Invoke-RestMethod -Uri 'https://openrouter.ai/api/v1/models' -Headers $headers -TimeoutSec 40
        if (-not $resp.data) { throw "Antwort enthielt keine Modell-Daten." }
        try { $resp | ConvertTo-Json -Depth 12 | Set-Content -Path $CacheFile -Encoding utf8 } catch {}
        Write-Log 'INFO' 'models_loaded' @{ count = $resp.data.Count }
        Write-Probe 'models_loaded' 'Modell-Liste live geladen' '> 0 Modelle' $resp.data.Count ($resp.data.Count -gt 0)
        return $resp.data
    } catch {
        Write-Log 'ERROR' 'models_load_failed' @{ error = "$_" }
        if (Test-Path $CacheFile) {
            Write-Host "  (Konnte nicht live laden — nutze letzte gespeicherte Liste.)" -ForegroundColor Yellow
            try { return (Get-Content $CacheFile -Raw -Encoding utf8 | ConvertFrom-Json).data } catch {}
        }
        Stop-WithMessage "Konnte die Modell-Liste nicht laden. Internet/OpenRouter erreichbar? Fehler: $_"
    }
}

# ---------- Hilfsfunktionen Aufbereitung ----------
function Get-CfgJson { Get-Content $ModelsJson -Raw -Encoding utf8 | ConvertFrom-Json }

function Get-Compat {
    param($Slug, $Cfg)
    $s = $Slug -replace '^~', ''   # fuehrendes ~ (OpenRouter "latest"-Marker) ignorieren
    foreach ($p in $Cfg.compatibility.PSObject.Properties) {
        if ($s -like ($p.Name + '*')) { return $p.Value }
    }
    return $Cfg.compatibilityDefault
}

function Get-ModelInfo {
    param($M, $Cfg)
    $sp       = @($M.supported_parameters)
    $inMod    = @($M.architecture.input_modalities)
    $outMod   = @($M.architecture.output_modalities)
    $agentic  = $null; $coding = $null
    try { $agentic = $M.benchmarks.artificial_analysis.agentic_index } catch {}
    try { $coding  = $M.benchmarks.artificial_analysis.coding_index } catch {}
    $priceIn  = 0.0; $priceOut = 0.0
    try { $priceIn  = [double]$M.pricing.prompt * 1e6 } catch {}
    try { $priceOut = [double]$M.pricing.completion * 1e6 } catch {}
    $isFree = (($priceIn -eq 0) -and ($priceOut -eq 0)) -or ($M.id -like '*:free')
    $compat = Get-Compat -Slug $M.id -Cfg $Cfg
    $created = 0; try { $created = [long]$M.created } catch {}
    [PSCustomObject]@{
        Id        = $M.id
        Name      = $M.name
        Created   = $created
        Context   = [int]$M.context_length
        HasTools  = ($sp -contains 'tools')
        HasReason = ($sp -contains 'reasoning')
        InImage   = ($inMod -contains 'image')
        InFile    = ($inMod -contains 'file') -or ($inMod -contains 'audio')
        OutImage  = ($outMod -contains 'image')
        OutText   = ($outMod -contains 'text')
        Agentic   = $agentic
        Coding    = $coding
        PriceIn   = $priceIn
        PriceOut  = $priceOut
        IsFree    = $isFree
        IsAnthropic = ($M.id -like 'anthropic/*') -or ($M.id -like '~anthropic/*')
        Rating    = $compat.rating
        Note      = $compat.note
    }
}

function Format-Ctx { param([int]$c); if ($c -ge 1000000) { "{0:0.#}M" -f ($c/1e6) } elseif ($c -ge 1000) { "{0:0}K" -f ($c/1e3) } else { "$c" } }
function Format-Price { param([double]$p); if ($p -eq 0) { "frei" } elseif ($p -lt 1) { "{0:0.00}" -f $p } else { "{0:0.0}" -f $p } }
function Format-Score { param($s); if ($null -eq $s) { "  —" } else { "{0,3:0}" -f [double]$s } }
function Trim-Str { param([string]$s,[int]$n); if ($s.Length -le $n) { $s.PadRight($n) } else { $s.Substring(0,$n-1) + "…" } }

# ---------- Tabelle anzeigen + Auswahl ----------
function Show-ModelTable {
    param([array]$Items, [string]$Title)
    Write-Host ""
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host ("  " + ("─" * 92)) -ForegroundColor DarkGray
    Write-Host ("  {0,-3} {1,-30} {2,6} {3,-5} {4,4} {5,4} {6,-4} {7,-9} {8}" -f '#','Modell','Ktx','Tools','Agt','Cod','Eig','$/M in','Hinweis') -ForegroundColor DarkGray
    Write-Host ("  " + ("─" * 92)) -ForegroundColor DarkGray
    $i = 1
    foreach ($it in $Items) {
        $tools = if ($it.HasTools) { 'ja ' } else { 'NEIN' }
        $toolsColor = if ($it.HasTools) { 'Gray' } else { 'DarkRed' }
        $line = ("  {0,-3} {1,-30} {2,6} {3,-5} {4} {5} {6,-4} {7,-9} {8}" -f `
            $i, (Trim-Str $it.Name 30), (Format-Ctx $it.Context), $tools, `
            (Format-Score $it.Agentic), (Format-Score $it.Coding), $it.Rating, `
            (Format-Price $it.PriceIn), (Trim-Str $it.Note 26))
        Write-Host $line -ForegroundColor $toolsColor
        $i++
    }
    Write-Host ("  " + ("─" * 92)) -ForegroundColor DarkGray
    Write-Host "  Spalten: Ktx=Kontext · Agt=Agenten-Bench · Cod=Coding-Bench · Eig=Eignung Claude Code · `$/M=Preis pro Mio Token" -ForegroundColor DarkGray
}

# ---------- Liste 1: Coding ----------
function Select-CodingModel {
    param([array]$All, $Cfg)
    # Aliase (~...-latest) + Fast-Spezialformen aus der Liste nehmen (Redundanz). Default per Enter nutzt den Alias trotzdem.
    $coding = $All | Where-Object { $_.HasTools -and $_.OutText -and $_.Id -notlike '~*' -and $_.Id -notlike '*-fast' }
    # Rein nach Staerke: Modelle MIT Benchmark zuerst (agentic, dann coding),
    # danach Modelle OHNE Benchmark nach Erscheinungsdatum (neueste zuerst → neue Modelle bleiben sichtbar).
    $sorted = $coding | Sort-Object `
        @{ Expression = { if (($null -ne $_.Agentic) -or ($null -ne $_.Coding)) { 0 } else { 1 } } }, `
        @{ Expression = { if ($null -eq $_.Agentic) { -1 } else { [double]$_.Agentic } }; Descending = $true }, `
        @{ Expression = { if ($null -eq $_.Coding) { -1 } else { [double]$_.Coding } }; Descending = $true }, `
        @{ Expression = { [long]$_.Created }; Descending = $true }
    $max = [int]$Cfg.settings.codingMaxRows
    $top = $sorted | Select-Object -First $max
    Write-Header
    Show-ModelTable -Items $top -Title ("PROGRAMMIER-MODELLE (Top {0}, nach Staerke) — alle werkzeug-faehig" -f $top.Count)
    Write-Host ""
    Write-Host "  Zahl = Modell waehlen  ·  [A]lle anzeigen  ·  [Z]urueck  ·  [Enter]=Standard (Claude Opus)" -ForegroundColor White
    $sel = Read-Host "  Deine Wahl"
    if ($sel -eq '') { return $Cfg.settings.defaultModelSlug }
    if ($sel -match '^[Zz]') { return $null }
    if ($sel -match '^[Aa]') {
        $top = $sorted
        Write-Header
        Show-ModelTable -Items $top -Title ("ALLE PROGRAMMIER-MODELLE ({0})" -f $top.Count)
        Write-Host ""
        $sel = Read-Host "  Zahl waehlen (oder [Z]urueck)"
        if ($sel -match '^[Zz]' -or $sel -eq '') { return $null }
    }
    if ($sel -match '^\d+$') {
        $idx = [int]$sel - 1
        if ($idx -ge 0 -and $idx -lt $top.Count) { return $top[$idx].Id }
    }
    Write-Host "  Ungueltige Eingabe." -ForegroundColor Yellow; Start-Sleep 1
    return (Select-CodingModel -All $All -Cfg $Cfg)
}

# ---------- Liste 2: nach Kategorie ----------
function Test-Category {
    param($Info, $Key)
    switch ($Key) {
        'coding'      { return ($Info.HasTools -and $Info.OutText) }
        'reasoning'   { return $Info.HasReason }
        'longcontext' { return ($Info.Context -ge 400000) }
        'cheap'       { return ($Info.IsFree -or $Info.PriceIn -le 0.3) }
        'vision'      { return $Info.InImage }
        'multimodal'  { return $Info.InFile }
        'imagegen'    { return $Info.OutImage }
    }
    return $false
}

function Select-ByCategory {
    param([array]$All, $Cfg)
    while ($true) {
        Write-Header
        Write-Host "  ALLE MODELLE NACH KATEGORIE" -ForegroundColor Cyan
        Write-Host ("  " + ("─" * 70)) -ForegroundColor DarkGray
        $cats = @($Cfg.categories)
        for ($i = 0; $i -lt $cats.Count; $i++) {
            $c = $cats[$i]
            $cnt = (@($All | Where-Object { Test-Category -Info $_ -Key $c.key })).Count
            $tag = if ($c.startable) { "" } else { "  (nur Info — nicht startbar)" }
            $col = if ($c.startable) { 'White' } else { 'DarkGray' }
            Write-Host ("  {0,-3} {1,-38} {2,4} Modelle{3}" -f ($i+1), $c.name, $cnt, $tag) -ForegroundColor $col
            Write-Host ("      {0}" -f $c.blurb) -ForegroundColor DarkGray
        }
        Write-Host ("  " + ("─" * 70)) -ForegroundColor DarkGray
        Write-Host "  Zahl = Kategorie oeffnen  ·  [Z]urueck zum Hauptmenue" -ForegroundColor White
        $sel = Read-Host "  Deine Wahl"
        if ($sel -match '^[Zz]' -or $sel -eq '') { return $null }
        if ($sel -notmatch '^\d+$') { continue }
        $ci = [int]$sel - 1
        if ($ci -lt 0 -or $ci -ge $cats.Count) { continue }
        $cat = $cats[$ci]
        $items = @($All | Where-Object { Test-Category -Info $_ -Key $cat.key }) | Sort-Object `
            @{ Expression = { if ($null -eq $_.Agentic) { -1 } else { [double]$_.Agentic } }; Descending = $true }, `
            @{ Expression = { if ($null -eq $_.Coding) { -1 } else { [double]$_.Coding } }; Descending = $true }, `
            @{ Expression = { $_.PriceIn } }
        $items = $items | Select-Object -First 40
        Write-Header
        Show-ModelTable -Items $items -Title ("KATEGORIE: {0} ({1})" -f $cat.name, $items.Count)
        if (-not $cat.startable) {
            Write-Host ""
            Write-Host "  Diese Kategorie ist NICHT in Claude Code startbar (Claude Code ist ein Coding-Agent)." -ForegroundColor Yellow
            Write-Host "  Sie dient nur dem Ueberblick." -ForegroundColor Yellow
            Read-Host "  [Enter] zurueck"; continue
        }
        Write-Host ""
        Write-Host "  Zahl = Modell waehlen  ·  [Z]urueck zu den Kategorien" -ForegroundColor White
        $s2 = Read-Host "  Deine Wahl"
        if ($s2 -match '^[Zz]' -or $s2 -eq '') { continue }
        if ($s2 -match '^\d+$') {
            $idx = [int]$s2 - 1
            if ($idx -ge 0 -and $idx -lt $items.Count) { return $items[$idx].Id }
        }
    }
}

# ---------- ccr-Config schreiben + starten ----------
function Save-Json-NoBom {
    param([string]$Path, $Object)
    $json = $Object | ConvertTo-Json -Depth 10
    [IO.File]::WriteAllText($Path, $json, (New-Object Text.UTF8Encoding $false))
}

function Start-WithModel {
    param([string]$Slug, [string]$ApiKey, [array]$All)
    $info = $All | Where-Object { $_.Id -eq $Slug } | Select-Object -First 1
    $isAnthropic = $info -and $info.IsAnthropic
    Set-Content -Path $LastModel -Value $Slug -Encoding utf8
    Write-Log 'INFO' 'model_selected' @{ model = $Slug; anthropic = $isAnthropic }

    Write-Header
    Write-Host "  Aktives Modell (laeuft ueber OpenRouter):" -ForegroundColor White
    Write-Host "    $Slug" -ForegroundColor Green
    if ($info) {
        $toolTxt = if ($info.HasTools) { 'ja' } else { 'NEIN' }
        Write-Host ("  Kontext: {0}   ·   Werkzeuge: {1}   ·   Eignung: {2}" -f (Format-Ctx $info.Context), $toolTxt, $info.Rating) -ForegroundColor DarkGray
        if (-not $info.HasTools) {
            Write-Host ""
            Write-Host "  ACHTUNG: Dieses Modell unterstuetzt KEINE Werkzeuge." -ForegroundColor Red
            Write-Host "  Claude Code wird damit kaum funktionieren (keine Datei-Edits, keine Tools)." -ForegroundColor Red
            if (-not $script:DryRun) { $go = Read-Host "  Trotzdem starten? (j/N)"; if ($go -notmatch '^[jJyY]') { return } }
        }
        if (-not $info.IsAnthropic) {
            Write-Host ""
            Write-Host "  Hinweis: Fremdmodell — laeuft, aber evtl. nicht ganz so rund wie Claude" -ForegroundColor Yellow
            Write-Host "  (Claude Code ist fuer Claude gebaut). Bei Werkzeug-Problemen ein Claude-Modell waehlen." -ForegroundColor Yellow
        }
    }
    Write-Host ""
    Write-Host "  Hinweis: Claude Code zeigt im eigenen Banner evtl. 'Opus' als internen Namen —" -ForegroundColor DarkGray
    Write-Host "  entscheidend ist das oben gruen genannte Modell, das wirklich antwortet." -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  Starte Claude Code ueber OpenRouter ..." -ForegroundColor Cyan
    Write-Host "  (Log dieser Sitzung: $LogFile)" -ForegroundColor DarkGray
    Write-Host ""

    # Native OpenRouter "Anthropic-Skin": NUR Umgebungsvariablen in DIESEM Fenster setzen.
    # KEIN Hintergrund-Dienst -> nichts kann sterben, kein ConnectionRefused; mehrere Fenster
    # mit verschiedenen Modellen gleichzeitig moeglich. Dein normales Claude bleibt unberuehrt.
    $env:ANTHROPIC_BASE_URL                     = 'https://openrouter.ai/api'   # OHNE /v1 (native Skin)
    $env:ANTHROPIC_AUTH_TOKEN                   = $ApiKey
    $env:ANTHROPIC_API_KEY                      = ''                            # MUSS leer sein (nicht unset)
    $env:ANTHROPIC_DEFAULT_OPUS_MODEL           = $Slug
    $env:ANTHROPIC_DEFAULT_SONNET_MODEL         = $Slug
    $env:ANTHROPIC_DEFAULT_HAIKU_MODEL          = $Slug
    $env:CLAUDE_CODE_SUBAGENT_MODEL             = $Slug
    $env:CLAUDE_CODE_DISABLE_EXPERIMENTAL_BETAS = '1'
    $env:CLAUDE_CODE_ATTRIBUTION_HEADER         = '0'
    $env:API_TIMEOUT_MS                         = '1200000'
    $env:API_FORCE_IDLE_TIMEOUT                 = '0'
    $env:DISABLE_TELEMETRY                      = '1'

    # Schlankes Claude-Profil aktivieren: laedt NUR Direktive #3 + deine eigenen Skills,
    # KEINE 40 Regeln, kein claude-mem, keine Auto-Memory -> ~5x weniger Token pro Frage.
    # Dein normales ~/.claude (voller Harness + Opus) bleibt voellig unberuehrt.
    if (Test-Path $LeanConfigDir) {
        $env:CLAUDE_CONFIG_DIR = $LeanConfigDir
        Write-Host "  Schlankes Profil aktiv (Direktive #3 + deine Skills, kein Ballast)." -ForegroundColor DarkGray
    } else {
        Write-Host "  Hinweis: schlankes Profil fehlt ($LeanConfigDir) — nutze volles Profil (hoehere Kosten)." -ForegroundColor Yellow
    }

    # KRITISCH gegen das "[1m]"-Suffix: Deine globale settings.json hat model="opus[1m]".
    # Claude Code haengt das "[1m]" sonst an JEDES Modell an -> OpenRouter findet es nicht.
    # Loesung: Session-Override-Settings (ueberschreibt NUR fuer diese Session, nicht global)
    # erzwingt den rohen Slug als Haupt- UND Subagent-Modell -> kein "[1m]".
    # ALLE Modell-Stufen hart auf das gewaehlte Modell pinnen, sonst faellt eine Stufe
    # (z.B. Hintergrund/Compact) auf Claude Codes eingebautes Anthropic-Default (Sonnet/Haiku)
    # zurueck — das wuerde dann ueber OpenRouter abgerechnet (ungewollte Kosten!).
    $override = [ordered]@{
        model = $Slug
        env   = [ordered]@{
            ANTHROPIC_MODEL                = $Slug
            ANTHROPIC_DEFAULT_OPUS_MODEL   = $Slug
            ANTHROPIC_DEFAULT_SONNET_MODEL = $Slug
            ANTHROPIC_DEFAULT_HAIKU_MODEL  = $Slug
            ANTHROPIC_SMALL_FAST_MODEL     = $Slug
            CLAUDE_CODE_SUBAGENT_MODEL     = $Slug
        }
    }
    Save-Json-NoBom -Path $OverrideSettings -Object $override
    Write-Probe 'env_set' 'Native-Skin BASE_URL gesetzt' 'https://openrouter.ai/api' $env:ANTHROPIC_BASE_URL ($env:ANTHROPIC_BASE_URL -eq 'https://openrouter.ai/api')

    if ($script:DryRun) {
        Write-Host "  [SELBSTTEST] Env + Override gesetzt. Wuerde starten: claude --model $Slug --settings <override>" -ForegroundColor Magenta
        Write-Host "  [SELBSTTEST] Override-Datei: $OverrideSettings" -ForegroundColor Magenta
        Write-Log 'INFO' 'selftest_ok' @{ model = $Slug; baseUrl = $env:ANTHROPIC_BASE_URL }
        return
    }
    Set-Location $WorkDir
    Write-Log 'INFO' 'launching_claude' @{ model = $Slug; cwd = $WorkDir }
    try {
        # --model: roher Slug als Hauptmodell (hoechste Praezedenz, kein "[1m]").
        # --settings: erzwingt Modell + Subagent-Modell fuer die Session.
        & claude --model $Slug --settings $OverrideSettings
    } catch {
        Write-Log 'ERROR' 'claude_failed' @{ error = "$_" }
        Stop-WithMessage "Start fehlgeschlagen: $_"
    }
    Write-Log 'INFO' 'session_ended' @{ model = $Slug }
    Write-Host ""
    Write-Host "  Claude Code beendet. Fenster kann geschlossen werden." -ForegroundColor DarkGray
    Read-Host "  [Enter] zum Schliessen"
}

# ================================================================================
#  HAUPTABLAUF
# ================================================================================
Write-Log 'INFO' 'launcher_start' @{ version = 1; logFile = $LogFile }
Write-Header
Write-Host "  Log dieser Sitzung: $LogFile" -ForegroundColor DarkGray

$apiKey = Get-ApiKey
$cfg    = Get-CfgJson
$rawModels = Get-Models -ApiKey $apiKey
$all = @()
foreach ($m in $rawModels) { try { $all += (Get-ModelInfo -M $m -Cfg $cfg) } catch { Write-Log 'WARN' 'model_parse_skip' @{ id = $m.id; error = "$_" } } }
Write-Log 'INFO' 'models_parsed' @{ total = $all.Count; withTools = (@($all | Where-Object { $_.HasTools })).Count }

$chosen = $null
if ($SelfTest) { $chosen = $SelfTest; Write-Host "  [SELBSTTEST] Modell vorgegeben: $SelfTest" -ForegroundColor Magenta }
while (-not $chosen) {
    Write-Header
    Write-Host "  WAS MOECHTEST DU STARTEN?" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "   1   Programmier-Modelle  (nach Staerke sortiert — empfohlen)" -ForegroundColor White
    Write-Host "   2   Alle Modelle nach Kategorie" -ForegroundColor White
    Write-Host "   E   Eigenen Modell-Slug eintippen  (z.B. deepseek/deepseek-chat-v3.1)" -ForegroundColor White
    Write-Host "   Enter   Standard: Claude Opus (rock-solid)" -ForegroundColor Gray
    Write-Host "   Q   Abbrechen" -ForegroundColor Gray
    Write-Host ""
    $main = Read-Host "  Deine Wahl"
    switch -Regex ($main) {
        '^$'      { $chosen = $cfg.settings.defaultModelSlug }
        '^[1]$'   { $chosen = Select-CodingModel -All $all -Cfg $cfg }
        '^[2]$'   { $chosen = Select-ByCategory -All $all -Cfg $cfg }
        '^[Ee]'   {
            $s = Read-Host "  Modell-Slug (anbieter/modell)"
            if ($s.Trim()) { $chosen = $s.Trim() }
        }
        '^[Qq]'   { Write-Log 'INFO' 'launcher_quit' @{}; exit 0 }
        default   { }
    }
}

Write-Log 'INFO' 'model_chosen' @{ model = $chosen }
Start-WithModel -Slug $chosen -ApiKey $apiKey -All $all
