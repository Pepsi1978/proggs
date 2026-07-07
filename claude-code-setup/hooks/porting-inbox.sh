#!/usr/bin/env bash
# DEPRECATED: Replaced by mirror-check.sh + import agent (Universal Mirror Bridge, 2026-03-25)
# This hook is no longer registered in settings.json.
# Kept for reference only — remove after 2026-04-25.
exit 0
# --- Original code below (inactive) ---
# porting-inbox.sh — Session-Start Hook: Reads PORTING-LIST.md from other CLIs
# Extracts new suggestions into the whiteboard as TODOs
# Part of the Universal Bridge Protocol (cross-CLI intelligence sharing)
# Platform: macOS/Linux (bash)
# ROBUSTNESS: Non-critical hook. Any failure -> exit 0 silently.

set +e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/hook-log.sh" 2>/dev/null || true
source "$SCRIPT_DIR/whiteboard-insert.sh" 2>/dev/null || true

REPO_ROOT="$HOME/proggs"
WHITEBOARD_FILE="$REPO_ROOT/.claude/agent-memory/shared/MEMORY.md"
LEDGER_FILE="$HOME/.claude/state/porting-ledger.json"

# Ensure ledger exists
LEDGER_DIR="$(dirname "$LEDGER_FILE")"
if [ ! -d "$LEDGER_DIR" ]; then
    mkdir -p "$LEDGER_DIR"
fi
if [ ! -f "$LEDGER_FILE" ]; then
    echo '{"ported":[]}' > "$LEDGER_FILE"
fi

# Read ledger (list of already-ported suggestion IDs)
ported_ids=$(cat "$LEDGER_FILE" 2>/dev/null)

# Define partner porting lists
partners=(
    "Gemini:$REPO_ROOT/Gemini-Setup/PORTING-LIST.md"
    "Codex:$REPO_ROOT/codex-setup/PORTING-LIST.md"
)

new_count=0

for partner_entry in "${partners[@]}"; do
    name="${partner_entry%%:*}"
    path="${partner_entry#*:}"

    [ ! -f "$path" ] && continue

    content=$(cat "$path" 2>/dev/null)
    [ -z "$content" ] && continue

    # Extract suggestion IDs: ### [ID]
    while IFS= read -r id; do
        [ -z "$id" ] && continue

        # Duplicate check 1: Exact ID match in ledger
        if echo "$ported_ids" | grep -qF "\"$id\""; then
            continue
        fi

        # Duplicate check 2: ID already in whiteboard
        if [ -f "$WHITEBOARD_FILE" ]; then
            if grep -qF "$id" "$WHITEBOARD_FILE" 2>/dev/null; then
                continue
            fi
        fi

        # Extract body for this suggestion (content between this ### and next ###/##)
        body=$(echo "$content" | sed -n "/### \[$id\]/,/^###\|^## /p" | tail -n +2 | head -20)
        first_line=$(echo "$body" | head -1 | sed 's/^[[:space:]]*//')

        # Duplicate check 3: Fuzzy — first line already in whiteboard
        if [ -f "$WHITEBOARD_FILE" ] && [ ${#first_line} -gt 20 ]; then
            short_match="${first_line:0:40}"
            if grep -qF "$short_match" "$WHITEBOARD_FILE" 2>/dev/null; then
                continue
            fi
        fi

        # New suggestion — write to whiteboard
        ts=$(date '+%Y-%m-%d %H:%M')
        entry="### $ts — Portierungs-Vorschlag von $name: [$id]
$body"

        if command -v insert_whiteboard_entry &>/dev/null; then
            insert_whiteboard_entry "Forschung & Intelligence" "$entry"
            new_count=$((new_count + 1))
            # Update ledger in memory
            ported_ids=$(echo "$ported_ids" | sed "s/\"ported\":\[/\"ported\":[\"$id\",/")
            hook_log "Porting inbox: new suggestion [$id] from $name" 2>/dev/null || true
        fi

    done < <(echo "$content" | grep -oP '###\s+\[\K[^\]]+' 2>/dev/null || echo "$content" | grep -o '### \[.*\]' | sed 's/### \[//;s/\]//')
done

# Write updated ledger
echo "$ported_ids" > "$LEDGER_FILE" 2>/dev/null || true

if [ "$new_count" -gt 0 ]; then
    echo "Porting-Inbox: $new_count neue Vorschlaege von Partner-CLIs ins Whiteboard uebernommen."
else
    hook_log "Porting inbox: no new suggestions" 2>/dev/null || true
fi

exit 0
