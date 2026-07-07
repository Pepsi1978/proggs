---
name: projects
description: List known projects and their instinct statistics
---

# Projects Command

List project registry entries and per-project instinct/observation counts for continuous-learning-v2.

## Implementation

Run the instinct CLI using the plugin root path:

```bash
python3 "${CODEX_PLUGIN_ROOT}/skills/continuous-learning-v2/scripts/instinct-cli.py" projects
```

Or if `CODEX_PLUGIN_ROOT` is not set (manual installation):

```bash
python3 ~/.codex/skills/continuous-learning-v2/scripts/instinct-cli.py projects
```

## Usage

```bash
/prompts:everything-codex-everything-codex--projects
```

## What to Do

1. Read `~/.codex/homunculus/projects.json`
2. For each project, display:
   - Project name, id, root, remote
   - Personal and inherited instinct counts
   - Observation event count
   - Last seen timestamp
3. Also display global instinct totals
