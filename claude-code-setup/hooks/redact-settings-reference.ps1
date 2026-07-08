# redact-settings-reference.ps1 — Pre-commit hook: redact secrets from settings-reference.json
# Hook event: PreCommit (Git pre-commit hook, called by git before a commit is finalized)
# Platform: Windows (PowerShell 7+)
#
# Strategy:
#   1. Check if claude-code-setup/settings-reference.json is staged for commit
#   2. If staged: read the JSON and remove env keys that look like secrets
#      (GITHUB_PERSONAL_ACCESS_TOKEN, or any key containing TOKEN, SECRET, KEY,
#       PASSWORD, CREDENTIAL — case insensitive)
#   3. Write cleaned JSON back to disk and re-stage the file (git add)
#
# ROBUSTNESS: Entire script in try/catch. Always exits 0 (non-blocking).
# This hook CLEANS silently — it never prevents a commit.

$ErrorActionPreference = 'SilentlyContinue'

# Secret key name patterns (matched case-insensitively against env key names)
$SECRET_KEY_PATTERNS = @(
    'GITHUB_PERSONAL_ACCESS_TOKEN',
    'TOKEN',
    'SECRET',
    'PASSWORD',
    'CREDENTIAL'
)

function Is-SecretKey($keyName) {
    $upper = $keyName.ToUpperInvariant()
    foreach ($pattern in $SECRET_KEY_PATTERNS) {
        if ($upper.Contains($pattern.ToUpperInvariant())) {
            return $true
        }
    }
    return $false
}

try {
    # Find the repo root so we can locate the file regardless of cwd
    $repoRoot = & git rev-parse --show-toplevel 2>&1
    if ($LASTEXITCODE -ne 0 -or -not $repoRoot) { exit 0 }
    $repoRoot = $repoRoot.Trim()

    # Check if settings-reference.json is staged
    $stagedFiles = & git diff --cached --name-only 2>&1
    $targetRelPath = 'claude-code-setup/settings-reference.json'
    $isStaged = $stagedFiles | Where-Object { $_ -replace '\\','/' -eq $targetRelPath }

    if (-not $isStaged) { exit 0 }

    # Build absolute path (handle both slash styles)
    $absPath = Join-Path $repoRoot ($targetRelPath -replace '/','\\')

    if (-not (Test-Path $absPath)) { exit 0 }

    # Use Python for safe JSON manipulation (no-sed-on-json rule)
    $pythonScript = @"
import sys, json, os, re

path = sys.argv[1]

# VALUE-based secret detection. Key-name substring matching (old approach) wrongly
# redacted harmless config keys whose NAME contains TOKEN/KEY (e.g.
# CLAUDE_CODE_MAX_OUTPUT_TOKENS=64000). We now redact only real secret VALUES
# (known token prefixes) plus an explicit whitelist of true secret key names.
SECRET_VALUE = re.compile(r'^(gh[opsu]_|sk-|AIza|xox[baprs]-|glpat-)[A-Za-z0-9_\-]{16,}$')
KNOWN_SECRET_KEYS = {
    'GITHUB_PERSONAL_ACCESS_TOKEN', 'ANTHROPIC_API_KEY', 'OPENAI_API_KEY',
    'GEMINI_API_KEY', 'GOOGLE_API_KEY', 'GROQ_API_KEY', 'OPENROUTER_API_KEY',
    'FIRECRAWL_API_KEY', 'DEEPSEEK_API_KEY', 'MISTRAL_API_KEY', 'XAI_API_KEY',
}

def _is_secret(k, v):
    if not isinstance(v, str):
        return False
    if v.startswith('<REDACTED') or v.startswith('REDACTED'):
        return False
    if SECRET_VALUE.match(v):
        return True
    if k.upper() in KNOWN_SECRET_KEYS and v and not v.startswith('<'):
        return True
    return False

with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)

redacted_count = 0

# Walk the JSON looking for an "env" section at any depth
def redact_env(obj):
    global redacted_count
    if isinstance(obj, dict):
        env = obj.get('env')
        if isinstance(env, dict):
            keys_to_redact = [k for k in env if _is_secret(k, env[k])]
            for k in keys_to_redact:
                env[k] = '<REDACTED -- set locally>'
                redacted_count += 1
        for v in obj.values():
            redact_env(v)
    elif isinstance(obj, list):
        for item in obj:
            redact_env(item)

redact_env(data)

if redacted_count > 0:
    import tempfile
    dir_name = os.path.dirname(path)
    with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp',
                                     delete=False, encoding='utf-8') as tmp:
        json.dump(data, tmp, indent=2, ensure_ascii=False)
        tmp.write('\n')
        tmp_path = tmp.name
    os.replace(tmp_path, path)
    print(f'REDACTED {redacted_count} secret(s) from settings-reference.json')
else:
    print('No secrets found in settings-reference.json')
"@

    # Write python script to temp file and run it
    $tmpScript = [System.IO.Path]::GetTempFileName() + '.py'
    [System.IO.File]::WriteAllText($tmpScript, $pythonScript, (New-Object System.Text.UTF8Encoding $false))

    try {
        $output = & python $tmpScript $absPath @SECRET_KEY_PATTERNS 2>&1
        Write-Output "redact-settings-reference: $output"

        # Re-stage the (possibly cleaned) file
        & git add $absPath 2>&1 | Out-Null
    } finally {
        Remove-Item $tmpScript -ErrorAction SilentlyContinue
    }

} catch {
    # Never block a commit due to hook errors
}

exit 0
