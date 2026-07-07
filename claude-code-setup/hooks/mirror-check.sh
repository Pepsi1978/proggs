#!/usr/bin/env bash
# mirror-check.sh — SessionStart: notify if mirror-ledger has pending entries
# Part of the Universal Mirror Bridge system.
# This hook ONLY counts and reports — it does NOT apply anything.
# To apply pending entries, the user starts the import agent manually.

set +e  # Never block session start

LEDGER="$HOME/proggs/claude-code-setup/mirror-ledger.md"

# Bail if ledger doesn't exist yet
[[ -f "$LEDGER" ]] || exit 0

# Detect platform
platform="macos"
cli="claude-code"

# Count PENDING entries for this platform/cli
count=$(grep -c "APPLIED: ${platform}/${cli}=PENDING" "$LEDGER" 2>/dev/null || true)
count=${count:-0}

# Only show message if there are pending entries
if [[ "$count" -gt 0 && "$count" != "0" ]]; then
    echo "Mirror-Bridge: ${count} ausstehende Eintraege von anderen Plattformen. Starte den import Agenten um zu uebernehmen."
fi

exit 0
