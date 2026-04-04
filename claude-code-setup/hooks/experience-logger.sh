#!/bin/bash
# Experience Logger — SessionEnd Hook (macOS/Linux)
# Schreibt nach jeder nicht-trivialen Session einen Erfahrungs-Eintrag
# in experience-store.jsonl und eine Trajectory in trajectories.jsonl.
# Basierend auf JitRL/MemRL (arXiv 2601.18510) und AutoRefine (arXiv 2601.22758).
#
# Direktive 3: Graceful Degradation — bei JEDEM Fehler exit 0, nie die Session blockieren.

set +e  # Nie bei Fehlern abbrechen

EXPERIENCE_PATH="$HOME/proggs/.claude/agent-memory/shared/experience-store.jsonl"
TRAJECTORY_PATH="$HOME/proggs/.claude/agent-memory/shared/trajectories.jsonl"
SCORES_PATH="$HOME/.claude/session-scores.jsonl"

# Verzeichnis sicherstellen
mkdir -p "$(dirname "$EXPERIENCE_PATH")" 2>/dev/null

# Session-Scores lesen (letzte Zeile = aktuelle Session)
if [ -f "$SCORES_PATH" ]; then
    LAST_SCORE=$(tail -1 "$SCORES_PATH" 2>/dev/null)
    if [ -n "$LAST_SCORE" ]; then
        TODAY=$(date +%Y-%m-%d)

        # Experience Store Eintrag (minimal, robust)
        # Extrahiere Felder mit Python fuer JSON-Sicherheit
        python3 -c "
import json, os, sys

try:
    score_line = sys.argv[1]
    score_data = json.loads(score_line)
    today = sys.argv[2]

    experience = {
        'date': today,
        'task_type': score_data.get('task_type', 'general'),
        'task_description': score_data.get('task_description', 'session-auto-logged'),
        'strategy': 'auto-captured',
        'tool_count': score_data.get('turns', 0),
        'error_count': score_data.get('errors', 0),
        'success_score': round(score_data.get('overall', 3)),
        'tags': ['auto-logged']
    }

    trajectory = {
        'date': today,
        'task_description': score_data.get('task_description', 'session-auto-logged'),
        'tool_sequence': [],
        'errors': [],
        'corrections': score_data.get('corrections', 0),
        'duration_minutes': score_data.get('duration_minutes', 0),
        'success': True,
        'tags': ['auto-logged']
    }

    exp_path = os.path.expanduser('~/proggs/.claude/agent-memory/shared/experience-store.jsonl')
    traj_path = os.path.expanduser('~/proggs/.claude/agent-memory/shared/trajectories.jsonl')

    with open(exp_path, 'a', encoding='utf-8') as f:
        f.write(json.dumps(experience, ensure_ascii=False) + '\n')

    with open(traj_path, 'a', encoding='utf-8') as f:
        f.write(json.dumps(trajectory, ensure_ascii=False) + '\n')

    # Pruning: Max 200 experience, 100 trajectory
    for path, limit in [(exp_path, 200), (traj_path, 100)]:
        with open(path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        if len(lines) > limit:
            with open(path, 'w', encoding='utf-8') as f:
                f.writelines(lines[-limit:])

except Exception:
    pass  # Graceful Degradation
" "$LAST_SCORE" "$TODAY" 2>/dev/null
    fi
fi

# PFLICHT: Immer exit 0
exit 0
