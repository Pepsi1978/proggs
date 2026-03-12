#!/data/data/com.termux/files/usr/bin/bash
# SessionEnd hook: Clean up temp files when Claude Code exits
# Helps preserve disk space on Termux (critical at 97%)

# Clean Claude tmp directory (files older than 2 hours)
find "$HOME/.tmp" -type f -mmin +120 -delete 2>/dev/null
find "$HOME/.tmp" -type d -empty -delete 2>/dev/null

# Clean node compile cache (can grow large)
find "$TMPDIR/node-compile-cache" -type f -mtime +1 -delete 2>/dev/null

# Report remaining space (silent, just for logging)
AVAIL=$(df -h /data/data/com.termux/files/home 2>/dev/null | awk 'NR==2{print $4}')
echo "Session beendet. Speicher: $AVAIL frei." >> "$HOME/.claude/session-cleanup.log" 2>/dev/null

exit 0
