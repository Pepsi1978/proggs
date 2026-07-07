# Versioned Git Hooks

This directory contains git hooks tracked in the repository so that every clone
benefits from the same checks. `git config core.hooksPath` is **not** stored in
the repo itself, so each clone needs a one-time activation:

```bash
git -C "$HOME/proggs" config core.hooksPath .githooks
```

After that command, every `git commit` and `git push` runs the matching script
from this directory automatically.

## Hooks

### `pre-commit`
Validates staged Android `values-XX/strings.xml` files for unescaped apostrophes
in Romance languages plus Turkish (where the issue actually breaks the Android
build). Uses the auto-escape validator at
`~/.claude/skills/übersetzung/scripts/validators/check_apostrophes.py`.

- Auto-escapes findings and re-stages the file so the fix lands in the same commit
- Skipped silently if the validator is missing (fresh checkout without the skill)
- Triggered by BUG #29 from the finale-plugin run on 2026-05-22

### `pre-push`
Runs `git fetch + git rebase origin/main` automatically before each push to
`origin`, so concurrent CLI sessions never reject each other's pushes.

## Adding new hooks

Drop the script into this directory, `chmod +x`, commit. Every other clone gets
it on next pull — they just need the `core.hooksPath` setting once.

## Activation reminder

`git config core.hooksPath` is a *local* config value — it is NOT replicated by
clone. So after a fresh checkout, run the command at the top of this file.
