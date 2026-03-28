#!/usr/bin/env bash
# sync-clawi.sh - Sync Clawi's complete identity/memory baseline between Repo and Local Workspace
set -euo pipefail

MODE=${1:-}
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
DEFAULT_REPO_DIR="/Users/frank/clawi/proggs/clawi-setup"
if [[ -n "${CLAWI_SETUP_REPO_DIR:-}" ]]; then
  REPO_DIR="$CLAWI_SETUP_REPO_DIR"
elif [[ "$SCRIPT_DIR" == "$HOME/.openclaw/workspace" ]]; then
  REPO_DIR="$DEFAULT_REPO_DIR"
else
  REPO_DIR="$SCRIPT_DIR"
fi
WORKSPACE_DIR="$HOME/.openclaw/workspace"
OPENCLAW_HOME="$HOME/.openclaw"
LCM_DB="$OPENCLAW_HOME/lcm.db"
LCM_BACKUP_DIR="$REPO_DIR/lossless-claw"
FILES_TO_SYNC=(
  "SOUL.md"
  "IDENTITY.md"
  "DIREKTIVEN.md"
  "AGENTS.md"
  "ENVIRONMENT-FIXES.md"
  "Forschung-clawi.md"
  "README.md"
  "HEARTBEAT.md"
  "MEMORY.md"
  "RECOVERY-OPENCLAW.md"
  "health-report-template.md"
  "sync-clawi.sh"
  "sync-clawi.ps1"
)
DIRS_TO_SYNC=(
  "memory"
  "hooks"
  "agents"
)

if [[ ! -d "$WORKSPACE_DIR" ]]; then
  echo "Local OpenClaw workspace not found at $WORKSPACE_DIR"
  exit 1
fi
if [[ ! -d "$REPO_DIR" ]]; then
  echo "Clawi setup repo not found at $REPO_DIR"
  exit 1
fi

copy_file_if_exists() {
  local src="$1"
  local dst="$2"
  if [[ -f "$src" ]]; then
    mkdir -p "$(dirname "$dst")"
    if [[ "$src" != "$dst" ]]; then
      cp "$src" "$dst"
    fi
    echo "  Synced $(basename "$src")"
  fi
}

copy_dir_if_exists() {
  local src="$1"
  local dst="$2"
  if [[ -d "$src" ]]; then
    mkdir -p "$dst"
    if [[ "$src" != "$dst" ]]; then
      cp -R "$src/." "$dst/"
    fi
    echo "  Synced $(basename "$src")/"
  fi
}

write_lcm_manifest() {
  if [[ ! -f "$LCM_DB" ]]; then
    return
  fi
  mkdir -p "$LCM_BACKUP_DIR"
  python3 - <<'PY'
import json, sqlite3, os, hashlib, time
from pathlib import Path
cfg_path = Path(os.path.expanduser('~/.openclaw/openclaw.json'))
db_path = Path(os.path.expanduser('~/.openclaw/lcm.db'))
out_path = Path(os.path.expanduser(os.environ['LCM_MANIFEST_OUT']))
with cfg_path.open() as f:
    cfg = json.load(f)
plugin = cfg.get('plugins', {}).get('entries', {}).get('lossless-claw', {})
install = cfg.get('plugins', {}).get('installs', {}).get('lossless-claw', {})
counts = {}
con = sqlite3.connect(str(db_path))
cur = con.cursor()
for t in ['conversations','messages','summaries']:
    try:
        cur.execute(f'select count(*) from {t}')
        counts[t] = cur.fetchone()[0]
    except Exception as e:
        counts[t] = {'error': str(e)}
con.close()
h = hashlib.sha256()
with db_path.open('rb') as f:
    for chunk in iter(lambda: f.read(1024*1024), b''):
        h.update(chunk)
manifest = {
  'capturedAt': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
  'dbPath': str(db_path),
  'dbSizeBytes': db_path.stat().st_size,
  'dbSha256': h.hexdigest(),
  'counts': counts,
  'pluginEntry': plugin,
  'pluginInstall': install,
  'rawDatabaseBackedUpToGit': False,
  'note': 'Only sanitized lossless-claw metadata is backed up to Git. Raw lcm.db is intentionally excluded because it may contain sensitive conversation/config data.'
}
out_path.write_text(json.dumps(manifest, indent=2) + '\n')
PY
}

case "$MODE" in
  pull)
    echo "Pulling Clawi's full baseline from Repo ($REPO_DIR) to $WORKSPACE_DIR..."
    for file in "${FILES_TO_SYNC[@]}"; do
      copy_file_if_exists "$REPO_DIR/$file" "$WORKSPACE_DIR/$file"
    done
    for dir in "${DIRS_TO_SYNC[@]}"; do
      copy_dir_if_exists "$REPO_DIR/$dir" "$WORKSPACE_DIR/$dir"
    done
    if [[ -f "$LCM_BACKUP_DIR/manifest.json" ]]; then
      copy_file_if_exists "$LCM_BACKUP_DIR/manifest.json" "$OPENCLAW_HOME/lcm.manifest.json"
      copy_file_if_exists "$LCM_BACKUP_DIR/README.md" "$WORKSPACE_DIR/lossless-claw-README.md"
    fi
    echo "Clawi's full baseline is now up to date in local workspace!"
    ;;
  push)
    echo "Pushing Clawi's full baseline from $WORKSPACE_DIR to Repo ($REPO_DIR)..."
    for file in "${FILES_TO_SYNC[@]}"; do
      copy_file_if_exists "$WORKSPACE_DIR/$file" "$REPO_DIR/$file"
    done
    for dir in "${DIRS_TO_SYNC[@]}"; do
      copy_dir_if_exists "$WORKSPACE_DIR/$dir" "$REPO_DIR/$dir"
    done
    if [[ -f "$LCM_DB" ]]; then
      mkdir -p "$LCM_BACKUP_DIR"
      export LCM_MANIFEST_OUT="$LCM_BACKUP_DIR/manifest.json"
      write_lcm_manifest
      echo "  Synced lossless-claw metadata/"
    fi
    echo "Clawi's full baseline is now backed up in the repository!"
    ;;
  *)
    echo "Usage: $0 {pull|push}"
    exit 1
    ;;
esac
