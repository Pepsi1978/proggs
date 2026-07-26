# Tool-Check: Meta-Improve & Sync (v2.0)

## Phase 2: Meta-Improve (Skill Self-Improvement)

After every run (whether or not fixes were applied), this skill analyzes and improves itself.

### Step 1: Self-Analysis

Reflect on the analysis that just ran:
- Which checks found nothing useful? Replace with more targeted checks?
- Were there issues the loops missed but should have caught?
- Did any phase take too long or produce low-value results?
- Were new patterns discovered that should become standard checks?
- Was loop depth distribution right?
- Did the creative layer produce genuinely useful ideas?
- Did Loop 4 (Optik) find real visual issues or just cosmetic nitpicks?
- Did the Express-Modus work correctly for small files?
- Did any agents crash? If so, was the resilienz handling effective?

### Step 2: Line Count Check

```
wc -l ~/.claude/commands/tool-check.md
```

- **Under 300 lines (main)**: Suggestions can be made freely
- **300-400 lines (main)**: Warn that limit is approaching
- **400+ lines (main)**: STOP. Ask user how to proceed (move more to ref files?)

Total across all files should stay under 800 lines.

### Step 3: Present Suggestions (NEVER auto-apply!)

Each suggestion uses the 3-part structure:

```
## Meta-Verbesserung: Vorschlaege fuer Tool-Check

### Vorschlag 1: [Titel]

**Was ist das Problem?**
[Plain German, concrete example from this run. 3-5 sentences.]

**Was moechte ich aendern?**
[Specific change — what gets added/removed/rewritten. 3-5 sentences.]

**Warum ist das nuetzlich?**
[Practical benefit. 2-3 sentences.]

### Vorschlag 2: [Titel]
[same 3-part structure]

### Skill-Status
- Hauptdatei: [N]/400 Zeilen
- Referenz-Dateien: loops.md ([N] Zeilen), meta-improve.md ([N] Zeilen)
- Gesamt: [N]/800 Zeilen
- Letzte Meta-Verbesserung: [Datum]

Soll ich diese Aenderungen umsetzen? (Ja/Nein/Teilweise)
```

NEVER modify this skill without explicit user approval.

### Step 4: Apply (only after user says yes)

1. BACKUP FIRST: Commit current version before changes
2. Apply approved changes to the relevant file(s)
3. Update version number and date in main file footer
4. Document what changed (old → new)
5. Sync to proggs repo (see below)

## Sync to GitHub

After any skill modification:

```bash
cp ~/.claude/commands/tool-check.md ~/proggs/claude-code-setup/commands/
cp -r ~/.claude/commands/tool-check-ref/ ~/proggs/claude-code-setup/commands/tool-check-ref/
cd ~/proggs && git add claude-code-setup/commands/tool-check.md claude-code-setup/commands/tool-check-ref/
git diff --cached --quiet || git commit -m "#NNN - Update tool-check skill" && git push
```

NEVER create a separate repo — all changes to `Pepsi1978/proggs`.
