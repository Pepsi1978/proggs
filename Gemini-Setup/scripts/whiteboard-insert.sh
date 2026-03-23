#!/usr/bin/env bash
# whiteboard-insert.sh — Shared helper for section-based whiteboard insertion (bash/macOS/Linux)
# Source this in any hook that needs to write to MEMORY.md:
#   source "$(dirname "$0")/whiteboard-insert.sh"
#   insert_whiteboard_entry "Offene Fehler & Probleme" "$my_entry"
#
# This replaces naive append (Add-Content / echo >> ) which is FORBIDDEN by whiteboard rules.
# Entries are inserted inside the correct ## Section, replacing the placeholder if present.

# replace_whiteboard_entry — Replaces ALL lines matching a pattern within a section, then inserts the new entry.
# Usage: replace_whiteboard_entry "Systemzustand" "Pending Admin Updates" "- **Pending Admin Updates (5):** foo,bar"
# This prevents duplicate accumulation (e.g. pending-admin-updates hook writing a new line every session).
replace_whiteboard_entry() {
    local section="$1"
    local match_pattern="$2"   # Lines containing this string will be REMOVED first
    local entry="$3"
    local memory_file="$HOME/proggs/C:/Users/barwa/GeminiCLI/proggs/Gemini-Setup/agent-memory/shared/MEMORY.md"
    local lock_file="/tmp/Gemini-whiteboard.lock"

    if [ ! -f "$memory_file" ]; then
        echo "[whiteboard-insert] WARNING: $memory_file does not exist" >&2
        return 0
    fi

    _do_replace() {
        python3 - "$memory_file" "$section" "$match_pattern" "$entry" <<'PYEOF'
import sys

memory_file   = sys.argv[1]
section       = sys.argv[2]
match_pattern = sys.argv[3]
entry         = sys.argv[4]

with open(memory_file, 'r', encoding='utf-8') as f:
    lines = f.readlines()

section_header = f"## {section}"
section_idx = -1
for i, line in enumerate(lines):
    if line.rstrip() == section_header or line.rstrip().startswith(section_header):
        section_idx = i
        break

if section_idx < 0:
    sys.exit(0)

# Find section boundaries
section_end = len(lines)
for j in range(section_idx + 1, len(lines)):
    stripped = lines[j].rstrip()
    if stripped.startswith("## ") or stripped == "---":
        section_end = j
        break

# Remove ALL lines matching the pattern within this section
new_lines = []
insert_point = section_end
for j in range(len(lines)):
    if section_idx < j < section_end and match_pattern in lines[j]:
        continue  # Skip matching lines
    new_lines.append(lines[j])

# Find the new insert point (end of section in cleaned list)
section_header_new = f"## {section}"
for i, line in enumerate(new_lines):
    if line.rstrip() == section_header_new or line.rstrip().startswith(section_header_new):
        # Find end of section
        for j in range(i + 1, len(new_lines)):
            stripped = new_lines[j].rstrip()
            if stripped.startswith("## ") or stripped == "---":
                insert_point = j
                break
        else:
            insert_point = len(new_lines)
        break

new_lines.insert(insert_point, entry + "\n")

with open(memory_file, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
PYEOF
    }

    if command -v flock >/dev/null 2>&1; then
        exec 9>"$lock_file"
        if flock -w 5 9; then
            _do_replace
            flock -u 9
        else
            echo "[whiteboard-insert] WARNING: flock timeout — skipping replace for section '$section'" >&2
        fi
        exec 9>&-
    else
        echo "[whiteboard-insert] WARNING: flock not found — replacing without lock" >&2
        _do_replace
    fi
}

insert_whiteboard_entry() {
    local section="$1"
    local entry="$2"
    local memory_file="$HOME/proggs/C:/Users/barwa/GeminiCLI/proggs/Gemini-Setup/agent-memory/shared/MEMORY.md"
    local placeholder="_Noch keine Eintraege._"
    local lock_file="/tmp/Gemini-whiteboard.lock"

    if [ ! -f "$memory_file" ]; then
        echo "[whiteboard-insert] WARNING: $memory_file does not exist" >&2
        return 0
    fi

    # Wrap the entire insert operation in flock to prevent concurrent read-modify-write.
    # Matches the PS1 version's named mutex (Global\GeminiWhiteboardMutex) with 5s timeout.
    # On macOS, flock may not be pre-installed. Install via: brew install flock
    _do_insert() {
        # Use Python3 for reliable in-place multi-line editing (available on macOS and Linux)
        python3 - "$memory_file" "$section" "$entry" "$placeholder" <<'PYEOF'
import sys

memory_file = sys.argv[1]
section     = sys.argv[2]
entry       = sys.argv[3]
placeholder = sys.argv[4]

with open(memory_file, 'r', encoding='utf-8') as f:
    lines = f.readlines()

section_header = f"## {section}"
section_idx = -1
for i, line in enumerate(lines):
    if line.rstrip() == section_header or line.rstrip().startswith(section_header):
        section_idx = i
        break

if section_idx < 0:
    sys.exit(0)  # Section not found — do nothing

# Scan inside the section to find placeholder or insertion point
insert_idx = section_idx + 1
placeholder_idx = -1
for j in range(section_idx + 1, len(lines)):
    stripped = lines[j].rstrip()
    if stripped.startswith("## ") or stripped == "---":
        break
    if lines[j].strip() == placeholder:
        placeholder_idx = j
        break
    insert_idx = j + 1

if placeholder_idx >= 0:
    lines[placeholder_idx] = entry + "\n"
else:
    lines.insert(insert_idx, entry + "\n")

with open(memory_file, 'w', encoding='utf-8') as f:
    f.writelines(lines)
PYEOF
    }

    if command -v flock >/dev/null 2>&1; then
        exec 9>"$lock_file"
        if flock -w 5 9; then
            _do_insert
            flock -u 9
        else
            echo "[whiteboard-insert] WARNING: flock timeout after 5s — skipping insert for section '$section'" >&2
        fi
        exec 9>&-
    else
        # flock not available — proceed without locking (with a warning)
        echo "[whiteboard-insert] WARNING: flock not found — inserting without lock (install via: brew install flock)" >&2
        _do_insert
    fi
}

