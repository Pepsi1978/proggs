#!/bin/bash
# Experience Logger — SessionEnd Hook (macOS/Linux)
# Schreibt nach jeder nicht-trivialen Session einen Erfahrungs-Eintrag
# in experience-store.jsonl und eine Trajectory in trajectories.jsonl.
# Basierend auf JitRL/MemRL (arXiv 2601.18510) und AutoRefine (arXiv 2601.22758).
# Erweitert um LEGOMem-Felder (arXiv 2601.03192): utility_score, near_miss,
# task_category, timestamp_decay, tool_sequence.
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

    tool_count = score_data.get('turns', 0)
    error_count = score_data.get('errors', 0)
    success_score = round(score_data.get('overall', 3))
    task_type = score_data.get('task_type', 'general')

    # utility_score: gewichtet Erfolg UND Effizienz
    # Formel: (success_score / 5.0) * (1.0 - error_count / max(tool_count, 1) * 0.5)
    # Score 5 + 0 Fehler = 1.0; Score 3 + viele Fehler = niedrig
    error_penalty = (error_count / max(tool_count, 1)) * 0.5
    utility_score = round((success_score / 5.0) * (1.0 - error_penalty), 3)
    utility_score = max(0.0, min(1.0, utility_score))  # Clampen auf [0.0, 1.0]

    # near_miss: Sessions die fast funktioniert haben (Lernwert! Nicht loeschen)
    # true wenn success_score 2-3 UND mindestens 1 Fehler
    near_miss = (2 <= success_score <= 3) and (error_count > 0)

    # task_category: grobe Kategorie aus task_type (Keyword-Zuordnung)
    task_type_lower = task_type.lower()
    if any(k in task_type_lower for k in ['build', 'compile', 'gradle', 'error', 'fix']):
        task_category = 'build_fix'
    elif any(k in task_type_lower for k in ['feature', 'implement', 'add', 'new']):
        task_category = 'feature'
    elif any(k in task_type_lower for k in ['config', 'setting', 'hook', 'setup']):
        task_category = 'config'
    elif any(k in task_type_lower for k in ['debug', 'bug', 'crash', 'investigate']):
        task_category = 'debug'
    elif any(k in task_type_lower for k in ['research', 'search', 'find', 'analyse', 'analyze']):
        task_category = 'research'
    elif any(k in task_type_lower for k in ['refactor', 'clean', 'improve', 'optimize']):
        task_category = 'refactor'
    else:
        task_category = 'other'

    # tool_sequence: aus score_data falls vorhanden, sonst leere Liste
    # (wird von Session-Score-Hook beigelegt wenn verfuegbar)
    tool_sequence = score_data.get('tool_sequence', [])

    experience = {
        'date': today,
        'task_type': task_type,
        'task_description': score_data.get('task_description', 'session-auto-logged'),
        'strategy': 'auto-captured',
        'tool_count': tool_count,
        'error_count': error_count,
        'success_score': success_score,
        'tags': ['auto-logged'],
        # LEGOMem-Erweiterungen (arXiv 2601.03192)
        'tool_sequence': tool_sequence,
        'utility_score': utility_score,
        'near_miss': near_miss,
        'task_category': task_category,
        'timestamp_decay': 1.0,  # Startwert; externer Prozess reduziert diesen Wert
    }

    trajectory = {
        'date': today,
        'task_description': score_data.get('task_description', 'session-auto-logged'),
        'tool_sequence': tool_sequence,
        'errors': [],
        'corrections': score_data.get('corrections', 0),
        'duration_minutes': score_data.get('duration_minutes', 0),
        'success': True,
        'tags': ['auto-logged'],
        # LEGOMem-Erweiterungen
        'utility_score': utility_score,
        'near_miss': near_miss,
    }

    exp_path = os.path.expanduser('~/proggs/.claude/agent-memory/shared/experience-store.jsonl')
    traj_path = os.path.expanduser('~/proggs/.claude/agent-memory/shared/trajectories.jsonl')

    with open(exp_path, 'a', encoding='utf-8') as f:
        f.write(json.dumps(experience, ensure_ascii=False) + '\n')

    with open(traj_path, 'a', encoding='utf-8') as f:
        f.write(json.dumps(trajectory, ensure_ascii=False) + '\n')

    # Pruning: Max 200 experience, 100 trajectory
    # Near-Miss-Eintraege werden BEVORZUGT behalten (MemRL: Lernwert)
    for path, limit in [(exp_path, 200), (traj_path, 100)]:
        with open(path, 'r', encoding='utf-8') as f:
            lines = [l for l in f.readlines() if l.strip()]
        if len(lines) > limit:
            # Trenne near_miss=true von normalen Eintraegen
            near_miss_lines = []
            normal_lines = []
            for line in lines:
                try:
                    entry = json.loads(line)
                    if entry.get('near_miss', False):
                        near_miss_lines.append(line)
                    else:
                        normal_lines.append(line)
                except Exception:
                    normal_lines.append(line)  # Bei Parse-Fehler: als normal behandeln

            # Zuerst alte normale Eintraege entfernen bis Limit erreicht
            overage = len(lines) - limit
            if overage > 0:
                # Nur normale Eintraege kuerzen (near_miss IMMER behalten)
                normal_lines = normal_lines[min(overage, len(normal_lines)):]

            # Wenn immer noch zu viele (mehr near_miss als Limit erlaubt):
            combined = normal_lines + near_miss_lines
            if len(combined) > limit:
                # Jetzt auch near_miss kuerzen (aelteste zuerst)
                combined = combined[-limit:]

            # Atomic write via temp-Datei + rename
            import tempfile
            dir_name = os.path.dirname(path)
            with tempfile.NamedTemporaryFile('w', dir=dir_name, suffix='.tmp',
                                             delete=False, encoding='utf-8') as tmp:
                tmp.writelines(combined)
                tmp_path = tmp.name
            os.replace(tmp_path, path)

except Exception:
    pass  # Graceful Degradation
" "$LAST_SCORE" "$TODAY" 2>/dev/null
    fi
fi

# PFLICHT: Immer exit 0
exit 0
