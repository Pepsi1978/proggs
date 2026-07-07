#!/usr/bin/env bash
# Disk Guard: Warns when storage is critically low
# Hook event: SessionStart
# Platform: macOS / Linux (bash equivalent of disk-guard.ps1)

HOOKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HOOKS_DIR/hook-log.sh"
source "$HOOKS_DIR/whiteboard-insert.sh"

# Read disk usage for the filesystem containing $HOME
# df -k gives kilobyte blocks; works on both macOS and Linux
df_line=$(df -k "$HOME" 2>/dev/null | tail -1)

if [ -z "$df_line" ]; then
    hook_log_error "failed to read disk space: df returned no output"
    exit 0
fi

# df columns: Filesystem  1K-blocks  Used  Available  Use%  Mounted
# Extract Used% (strip the % sign)
usage_pct_raw=$(echo "$df_line" | awk '{print $5}' | tr -d '%')

# Fallback: some df flavours have different column order — try awk-based approach
if ! [[ "$usage_pct_raw" =~ ^[0-9]+$ ]]; then
    usage_pct_raw=$(df -k "$HOME" 2>/dev/null | tail -1 | grep -oE '[0-9]+%' | head -1 | tr -d '%')
fi

if ! [[ "$usage_pct_raw" =~ ^[0-9]+$ ]]; then
    hook_log_error "failed to parse disk usage percentage"
    exit 0
fi

usage_pct="$usage_pct_raw"

# Calculate free and total GB from df output
total_kb=$(echo "$df_line" | awk '{print $2}')
used_kb=$(echo "$df_line" | awk '{print $3}')
avail_kb=$(echo "$df_line" | awk '{print $4}')

# Convert to GB with one decimal (using awk for floating point)
free_gb=$(awk "BEGIN {printf \"%.1f\", $avail_kb / 1048576}")
used_gb=$(awk "BEGIN {printf \"%.1f\", $used_kb  / 1048576}")
total_gb=$(awk "BEGIN {printf \"%.1f\", $total_kb / 1048576}")

if [ "$usage_pct" -ge 95 ]; then
    hook_log_error "CRITICAL disk space: ${usage_pct}% used, ${free_gb}GB free"
    echo "WARNUNG: Speicherplatz KRITISCH bei ${usage_pct}% — nur ${free_gb}GB frei!"
    echo "Empfehlung: Temporaere Dateien loeschen oder Speicher freigeben."
    # Log to whiteboard using section-based insertion (plain append is FORBIDDEN)
    ts=$(date '+%Y-%m-%d %H:%M')
    entry="### $ts — Hook: disk-guard.sh — Speicherplatz KRITISCH bei ${usage_pct}%"
    insert_whiteboard_entry "Offene Fehler & Probleme" "$entry"
elif [ "$usage_pct" -ge 90 ]; then
    hook_log_warn "disk space low: ${usage_pct}% used, ${free_gb}GB free"
    echo "Disk-Guard: Speicherplatz bei ${usage_pct}% — ${free_gb}GB frei (Achtung)."
else
    hook_log "disk OK: ${usage_pct}% used, ${free_gb}GB free"
fi

exit 0
