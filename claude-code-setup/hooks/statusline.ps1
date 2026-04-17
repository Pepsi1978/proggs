# Statusline fuer Claude Code (Windows) — zeigt Modell + Effort neben der Eingabezeile.
# Wird von Claude Code mit Session-JSON auf stdin aufgerufen.
# Ausgabe: Eine einzige Zeile Text, die unter dem Eingabefeld erscheint.

$ErrorActionPreference = 'SilentlyContinue'

# JSON-Input einlesen
$input_raw = [Console]::In.ReadToEnd()

$model = ''
if ($input_raw) {
    try {
        $obj = $input_raw | ConvertFrom-Json
        if ($obj.model) {
            if ($obj.model.display_name) { $model = $obj.model.display_name }
            elseif ($obj.model.id) { $model = $obj.model.id }
        }
    } catch { $model = '' }
}

# Effort-Level aus settings.json lesen
$effort = 'high'
try {
    $settingsPath = Join-Path $env:USERPROFILE '.claude\settings.json'
    $settings = Get-Content -Raw -Encoding UTF8 -Path $settingsPath | ConvertFrom-Json
    if ($settings.effortLevel) { $effort = $settings.effortLevel }
} catch { }

if (-not $model) { $model = 'claude' }

# Kompakte Ausgabe — eine Zeile
[Console]::Out.Write("{0} * effort: {1}" -f $model, $effort)
