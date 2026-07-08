#!/bin/bash
# redact-settings-reference.sh — Pre-commit hook: redact secrets from settings-reference.json
# Hook event: PreCommit (Git pre-commit hook, called by git before a commit is finalized)
# Platform: macOS / Linux
#
# Strategy:
#   1. Check if claude-code-setup/settings-reference.json is staged for commit
#   2. If staged: read the JSON and remove env keys that look like secrets
#      (GITHUB_PERSONAL_ACCESS_TOKEN, or any key containing TOKEN, SECRET, KEY,
#       PASSWORD, CREDENTIAL — case insensitive)
#   3. Write cleaned JSON back to disk and re-stage the file (git add)
#
# ROBUSTNESS: set +e ensures errors never abort the script. Always exits 0 (non-blocking).
# This hook CLEANS silently — it never prevents a commit.

set +e

TARGET_REL="claude-code-setup/settings-reference.json"

# Find repo root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || exit 0
[ -z "$REPO_ROOT" ] && exit 0

# Check if settings-reference.json is staged
STAGED=$(git diff --cached --name-only 2>/dev/null | grep -F "$TARGET_REL" | head -1)
[ -z "$STAGED" ] && exit 0

ABS_PATH="${REPO_ROOT}/${TARGET_REL}"
[ -f "$ABS_PATH" ] || exit 0

# Use python3 for safe JSON manipulation (no-sed-on-json rule)
OUTPUT=$(python3 - "$ABS_PATH" <<'PYEOF'
import sys, json, os, tempfile

import re

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

try:
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
except Exception as e:
    print(f'ERROR: Could not parse JSON: {e}', file=sys.stderr)
    sys.exit(0)

redacted_count = 0

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
PYEOF
) 2>/dev/null || true

echo "redact-settings-reference: ${OUTPUT:-done}"

# Re-stage the (possibly cleaned) file
git add "$ABS_PATH" 2>/dev/null || true

exit 0
