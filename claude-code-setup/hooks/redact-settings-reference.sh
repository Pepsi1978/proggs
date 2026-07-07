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

path = sys.argv[1]

# Secret key patterns — matched case-insensitively against env key names
SECRET_PATTERNS = [
    'GITHUB_PERSONAL_ACCESS_TOKEN',
    'TOKEN',
    'SECRET',
    'KEY',
    'PASSWORD',
    'CREDENTIAL',
]

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
            keys_to_redact = []
            for k in env:
                k_upper = k.upper()
                for pattern in SECRET_PATTERNS:
                    if pattern in k_upper:
                        keys_to_redact.append(k)
                        break
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
