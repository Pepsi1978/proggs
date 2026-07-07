#!/usr/bin/env python3
# redact-secrets.py — Auto-redact secrets from config files before they reach GitHub
#
# Usage:
#   python redact-secrets.py [file1] [file2] ...
#   python redact-secrets.py                   (auto-detects settings-reference.json)
#
# What it does:
#   - Scans JSON values for token patterns (GitHub tokens, long opaque strings)
#   - Replaces matching values with '<REDACTED — set locally>'
#   - Writes atomically (temp file → rename) to prevent corruption
#   - Uses UTF-8 encoding (required on Windows for emoji/unicode in JSON)
#
# Token patterns detected:
#   - ghp_*        GitHub Personal Access Token
#   - gho_*        GitHub OAuth Token
#   - github_pat_* GitHub Fine-Grained PAT
#   - ghs_*        GitHub App installation token
#   - Any string ≥40 chars that looks like a random token (hex/base64-ish)

import sys
import os
import re
import json
import tempfile
import traceback

# ── Secret patterns ──────────────────────────────────────────────────────────
# These are matched against JSON string VALUES (not keys).
# Order matters: more specific patterns first.
TOKEN_PATTERNS = [
    # GitHub tokens (explicit prefixes — always redact)
    re.compile(r'^(ghp_[A-Za-z0-9]{36,})$'),
    re.compile(r'^(gho_[A-Za-z0-9]{36,})$'),
    re.compile(r'^(github_pat_[A-Za-z0-9_]{36,})$'),
    re.compile(r'^(ghs_[A-Za-z0-9]{36,})$'),
    re.compile(r'^(ghr_[A-Za-z0-9]{36,})$'),

    # Generic high-entropy tokens ≥40 chars (hex or base64-ish)
    # Excludes: paths (contain /\:), URLs, UUIDs with dashes only
    re.compile(r'^([A-Za-z0-9+/=_\-]{40,})$'),
]

# Keys that are EXCLUDED from the long-token check (false-positive prevention)
# Even if their value is 40+ chars, these keys will NOT be redacted.
SAFE_KEYS = {
    'command', 'args', 'description', 'path', 'cwd', 'shell',
    'pattern', 'include', 'exclude', 'glob', 'regex',
    'name', 'id', 'type', 'version', 'model', 'provider',
    'url', 'endpoint', 'baseUrl', 'base_url',
    'npmScope', 'packageName', 'className',
    'file_path', 'filePath', 'filename',
}

REDACTED_VALUE = '<REDACTED — set locally>'


def looks_like_secret(key: str, value: str) -> bool:
    """Return True if this key/value pair looks like a secret."""
    if not isinstance(value, str):
        return False
    # Short values cannot be tokens
    if len(value) < 20:
        return False

    # Explicit GitHub prefixes — always redact regardless of key
    for pattern in TOKEN_PATTERNS[:5]:
        if pattern.match(value):
            return True

    # Long opaque strings — only if the key is not in the safe list
    if key.lower() not in SAFE_KEYS:
        if TOKEN_PATTERNS[5].match(value):
            # Additional heuristics to avoid false positives:
            # Skip values that look like file paths
            if '/' in value or '\\' in value or ':' in value:
                return False
            # Skip values with spaces (sentences, not tokens)
            if ' ' in value:
                return False
            # Skip base64-encoded JSON (starts with ey)
            # Those are JWTs — still redact them!
            return True

    return False


def redact_json_object(obj, path='', redacted_count=None):
    """Recursively walk a JSON object and redact secret values."""
    if redacted_count is None:
        redacted_count = [0]

    if isinstance(obj, dict):
        result = {}
        for k, v in obj.items():
            if isinstance(v, str) and looks_like_secret(k, v):
                print(f'  REDACTED: {path}.{k} = "{v[:8]}..."' if len(v) > 8 else f'  REDACTED: {path}.{k}')
                result[k] = REDACTED_VALUE
                redacted_count[0] += 1
            else:
                result[k] = redact_json_object(v, f'{path}.{k}', redacted_count)
        return result
    elif isinstance(obj, list):
        return [redact_json_object(item, f'{path}[{i}]', redacted_count)
                for i, item in enumerate(obj)]
    else:
        return obj


def redact_file(filepath: str) -> bool:
    """
    Redact secrets from a JSON file.
    Returns True if any secrets were found and redacted.
    """
    filepath = os.path.abspath(filepath)

    if not os.path.exists(filepath):
        print(f'WARNING: File not found: {filepath}')
        return False

    print(f'Scanning: {filepath}')

    # Read with UTF-8 (CRITICAL on Windows — cp1252 default would corrupt emoji)
    with open(filepath, 'r', encoding='utf-8') as fh:
        original_content = fh.read()

    # Parse JSON
    try:
        data = json.loads(original_content)
    except json.JSONDecodeError as e:
        print(f'  WARNING: Not valid JSON ({e}) — skipping')
        return False

    # Redact
    redacted_count = [0]
    cleaned_data = redact_json_object(data, '', redacted_count)

    if redacted_count[0] == 0:
        print(f'  OK: No secrets found.')
        return False

    print(f'  Found {redacted_count[0]} secret(s) — writing redacted version...')

    # Atomic write: temp → rename (prevents corruption on crash)
    dir_name = os.path.dirname(filepath)
    try:
        with tempfile.NamedTemporaryFile(
            'w',
            dir=dir_name,
            suffix='.tmp',
            delete=False,
            encoding='utf-8'   # CRITICAL: explicit UTF-8 on Windows
        ) as tmp:
            json.dump(cleaned_data, tmp, indent=2, ensure_ascii=False)
            tmp.write('\n')
            tmp_path = tmp.name

        # Atomic replace
        os.replace(tmp_path, filepath)
        print(f'  Written: {filepath}')
        return True

    except Exception as e:
        # Clean up temp file if it exists
        try:
            if 'tmp_path' in dir() and os.path.exists(tmp_path):
                os.unlink(tmp_path)
        except Exception:
            pass
        print(f'  ERROR writing file: {e}')
        return False


def auto_detect_targets() -> list:
    """Find settings-reference.json in the current repo."""
    candidates = []

    # Common locations relative to cwd
    search_patterns = [
        'claude-code-setup/settings-reference.json',
        'claude-code-setup/settings.json',
        '../claude-code-setup/settings-reference.json',
    ]

    for pattern in search_patterns:
        full = os.path.abspath(pattern)
        if os.path.exists(full):
            candidates.append(full)

    return candidates


def main():
    targets = sys.argv[1:] if len(sys.argv) > 1 else auto_detect_targets()

    if not targets:
        print('redact-secrets.py: No target files specified and none auto-detected.')
        print('Usage: python redact-secrets.py [file1] [file2] ...')
        sys.exit(0)

    any_redacted = False
    for target in targets:
        try:
            if redact_file(target):
                any_redacted = True
        except Exception as e:
            print(f'ERROR processing {target}: {e}')
            traceback.print_exc()

    if any_redacted:
        print()
        print('WARNUNG: Secrets wurden automatisch redacted.')
        print('Pruefen Sie die Dateien vor dem Commit.')
        sys.exit(2)  # Exit code 2 = secrets found + redacted
    else:
        sys.exit(0)


if __name__ == '__main__':
    main()
