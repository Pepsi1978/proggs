---
name: self-improve
description: Systematic self-improvement of the Claude Code development environment. ONLY use when the user explicitly says "/self-improve", "verbessere dich", "optimiere deine Umgebung", "check dein Setup", or "update alles". NEVER run this proactively or automatically — only on manual user request.
---

# Self-Improve v4.0 — Systematic + Creative Environment Optimization

**Before doing ANYTHING, show this overview in German:**

```
╔══════════════════════════════════════════════════════════════╗
║  Self-Improve Skill v4.0 — Deine Entwicklungsumgebung       ║
║  pruefen, aktualisieren, KREATIV ERFORSCHEN                  ║
║  Cross-Platform: macOS + Windows + Termux/Android            ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  4-Stufen-Modell (NEU: Kreatives Forschen!):                 ║
║                                                              ║
║  Stufe 1: SCAN — Umgebung pruefen                            ║
║    → env-checker Agent (inkl. Android/Mobile Deep-Scan)      ║
║                                                              ║
║  Stufe 2: DEEP-DIVE — Recherche + Updates                    ║
║    → 5 parallele Researcher (PARALLEL zu Stufe 1)            ║
║    → Ergebnisse kreuzvalidiert                               ║
║                                                              ║
║  Stufe 3: IMPROVE — Verbessern + Berichten                   ║
║    → Verbesserungen anwenden + Report + Meta-Improve         ║
║                                                              ║
║  Stufe 4: CREATIVE RESEARCH — Kreativ forschen (NEU!)        ║
║    → Multi-Perspektiven-Analyse (6 Denkweisen)               ║
║    → Kombinatorisches Denken (Was + Was = Neu?)              ║
║    → Kontraeres Denken (Was bremst uns?)                     ║
║    → Umgebungs-Archaeologie (Was ist untergenutzt?)          ║
║    → Tool-Erfindung (Luecken erkennen → selbst bauen)       ║
║    → Zukunfts-Radar (Was kommt, was vorbereiten?)            ║
║                                                              ║
║  Danach: Alles nach GitHub pushen (Pepsi1978/proggs)         ║
║  Du kannst jeden Schritt live mitlesen.                      ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

## Core Rules

### Visibility (CRITICAL)
- NEVER run anything hidden (no `context: fork`, no `run_in_background`, no silent subagents)
- The user MUST read along with EVERY action in real-time
- Before each action: short German explanation. After each action: show the result.

### User Goals
- **Zero friction**: Full automation, no permission prompts for standard actions
- **Maximum quality**: Store-quality native apps (Swift/AppKit macOS, C#/WPF Windows)
- **Preferred languages**: Swift, C#, TypeScript, Rust, Go. No Python for visible things.
- **Best model always**: Opus with high effort. NEVER reduce effortLevel below "high".
- **Parallel execution**: Use subagents and Agent Teams wherever possible — always visible.
- **Single repo**: Everything in `Pepsi1978/proggs`. NEVER create new repos.

### Protected Settings (NEVER change)
- `effortLevel`: MUST stay `"high"`
- `CLAUDE_CODE_EFFORT_LEVEL`: MUST stay `"high"`
- `CLAUDE_CODE_SUBAGENT_MODEL`: MUST stay `"sonnet"`
- `model`: MUST stay `"claude-opus-4-6"`

## Thoroughness Detection

Parse the user's message to determine thoroughness level:

- **Standard mode** (default): User says "verbessere dich", "/self-improve", "optimiere deine Umgebung" WITHOUT mentioning thoroughness → Run volatile checks only (Stufe 1 quick mode).
- **Thorough mode**: User says "sehr ausführlich", "ausführlicher Check", "gründlich prüfen", "full check", "alles prüfen" → Run ALL checks (Stufe 1 full mode = volatile + stable).
- **Focus mode**: User adds a topic (e.g., "mit Fokus Rust", "Fokus Android") → Standard/Thorough checks PLUS deep-dive into focus topic.

Thoroughness and Focus are independent — you can have: standard, standard+focus, thorough, thorough+focus.

## Platform Detection (FIRST STEP)

```bash
uname -s   # "MINGW*" = Windows Git Bash, "Darwin" = macOS, "Linux" = Linux/Termux
echo $PREFIX  # "/data/data/com.termux/files/usr" = Termux
```

**Windows shell rule:** Use `pwsh` (PowerShell 7+) for complex commands. For anything with `$`, `[`, `]`, `::`, write a temp `.ps1` file:
```bash
cat > /tmp/check.ps1 << 'PSEOF'
# PowerShell code here
PSEOF
pwsh -File /tmp/check.ps1
```

**Platform command mapping:**
| Task | macOS | Windows (pwsh) | Termux |
|------|-------|----------------|--------|
| Package updates | `brew outdated` / `brew upgrade` | `winget upgrade --include-unknown` / `winget upgrade --all` | `pkg upgrade -n` / `pkg upgrade -y` |
| Disk space | `df -h /` | `Get-PSDrive C` (temp .ps1) | `df -h /data` |
| Rust | `rustup check` / `rustup update` | same | same |
| .NET | `dotnet --list-sdks` | same | N/A |
| OS patches | `softwareupdate --list` | `Get-HotFix` (temp .ps1) | N/A |
| Shell config | `~/.zshrc` | `$PROFILE` | `~/.bashrc` |

**Rule**: Never run `brew` on Windows, `winget` on macOS, or `pkg` outside Termux.

## The 3-Tier Process

### Stufe 1: SCAN (Environment Audit)

**Delegate to the `env-checker` agent** — this keeps the main context clean.

```
→ Spawn env-checker agent:
  Agent(env-checker, prompt: "Run environment health check. Mode: [quick|full]. Platform: [detected]. Date: [today]. Return full structured report.

  ANDROID DEEP-SCAN (always run in full mode, or when focus=Android):
  - Run `sdkmanager --list_installed` and compare against latest available (build-tools, platforms, system-images, ndk, cmake)
  - Run `avdmanager list avd` and verify AVDs exist for: phone, foldable, tablet, resizable
  - Check ANDROID_HOME and ANDROID_NDK_HOME are set and exported in shell config
  - Check PATH includes build-tools (latest version), platform-tools, emulator, cmdline-tools
  - Verify Kotlin version matches Compose Compiler compatibility
  - Check for cargo-ndk if Rust Android targets are installed
  - Verify scrcpy and bundletool are available
  - Report any missing components with exact sdkmanager install commands.")
```

- **Standard mode** → `mode: quick` (volatile checks only: versions, patches, disk, updates, hooks, drift)
- **Thorough mode** → `mode: full` (volatile + stable: settings, agent tiers, language readiness, rule completeness, mobile dev incl. Android deep-scan, git config)

The env-checker returns a structured report. Show the summary table to the user and note any items flagged for action.

**Stufe 1 and Stufe 2 start SIMULTANEOUSLY** — the env-checker runs in parallel with the researchers. Stufe 2 doesn't need Stufe 1's results to start (researchers search the web, not local state).

### Stufe 2: DEEP-DIVE (Research + Updates)

**MANDATORY: Spawn ALL 5 researchers in ONE message block.** Never start them one by one.

Each researcher is a `researcher` agent (Sonnet model). Each researcher MUST spawn its own sub-agents for deeper investigation when it finds leads — this makes results more accurate.

**Fixed Researcher Templates (copy these EXACTLY, fill in [placeholders]):**

```
Researcher 1 — Claude Code Updates:
"Research Claude Code CLI updates and new features. Current version: [version from scan].
Search for: (1) changelog and new features since this version, (2) new settings or env vars,
(3) performance improvements, (4) breaking changes or deprecations.
IMPORTANT: Spawn sub-agents to verify claims — one for changelog parsing, one for settings docs.
Return only actionable findings with version numbers. Date: [today]."

Researcher 2 — Plugins & Marketplace:
"Research new Claude Code plugins available in [current month+year].
Search: (1) new official plugins (anthropics/claude-plugins-official), (2) superpowers-marketplace updates,
(3) popular community plugins for Swift/C#/TypeScript/Rust/Go/Kotlin developers.
IMPORTANT: Spawn sub-agents to check GitHub repos of promising plugins (stars, last commit, security).
IMPORTANT (installed plugins verification): Before recommending ANY plugin, verify it is NOT in this
complete list of already-installed plugins from settings.json enabledPlugins:
[PASTE the full list of plugin keys from enabledPlugins here, one per line].
Only suggest plugins whose exact name does NOT appear in this list. Previous runs had false positives
where researchers recommended already-installed LSP plugins (gopls, jdtls, csharp) — avoid this."

Researcher 3 — Parallelization & Automation:
"Research Claude Code agent teams, parallelization patterns, and automation best practices as of [today].
Search: (1) Agent Teams usage patterns, (2) new hook events and hook types,
(3) skill structuring for large skills, (4) worktree isolation patterns.
IMPORTANT: Spawn sub-agents for each topic area to get deeper results.
Return actionable patterns with code examples."

Researcher 4 — Tool Versions:
"Find latest stable versions of: Node.js LTS, Bun, Deno, Go, .NET SDK, Rust, Biome, golangci-lint,
Kotlin, Gradle, GitHub CLI, Git for Windows, CMake, JDK.
User's current versions: [from scan].
IMPORTANT: Spawn a sub-agent to double-check each version against official release pages.
IMPORTANT (version precision): The user's memory may store abbreviated versions (e.g., 'fd 10.4' when
the actual install is 10.4.2). Always compare against the EXACT versions from the env-checker scan output,
not against memory values. If exact scan data is unavailable, mark the version as 'unverified' instead of
flagging for update.
Return a comparison table: tool | current | latest | update needed?"

Researcher 5 — Security:
"Search for known security vulnerabilities in: [list tools+versions from scan].
Check: (1) CVEs in specific versions, (2) Windows-specific issues (Git Bash, NTLM, credential helpers),
(3) Claude Code security advisories, (4) npm/cargo/NuGet supply chain alerts.
IMPORTANT: Spawn sub-agents for each tool category (runtime CVEs, Windows CVEs, Claude Code CVEs).
IMPORTANT (.NET versioning): .NET uses TWO version schemes — SDK version (e.g. 10.0.201) and Runtime
version (e.g. 10.0.4). Security advisories cite Runtime versions. SDK 10.0.1xx = Runtime 10.0.x (1st band),
SDK 10.0.2xx = Runtime 10.0.x (2nd band). Always map CVE 'fixed in' Runtime versions to installed SDK versions
before reporting. Do NOT flag as vulnerable if the installed SDK already contains the patched Runtime.
Return ONLY confirmed vulnerabilities — do not speculate."
```

**After all researchers return — MANDATORY cross-validation using this table:**

Show this table to the user with ALL researcher claims validated against scan data:
```markdown
### Kreuzvalidierung Researcher ↔ Scan

| Behauptung (Researcher) | Quelle | Scan-Daten (Realitaet) | Stimmt? | Aktion |
|--------------------------|--------|------------------------|---------|--------|
| [tool] version X available | R4 | Installiert: Y | ✅/⚠️/❌ | Update/Skip/Pruefen |
| [CVE] affects [tool] | R5 | Version: Z, Patch: ... | ✅/⚠️/❌ | Patched/Update/Warn |
| [plugin] not installed | R2 | Plugin-Liste: ... | ✅/⚠️/❌ | Install/Skip |
```

**Validation rules (priority order):**
1. **System state** (env-checker scan) > **Memory** > **Web research** (researchers). When in doubt, re-run the check.
2. **Version claims**: Compare against exact versions from scan. Only flag if local version is actually LOWER than latest.
3. **Security claims**: Verify against OS patch data from scan. NEVER report as "missing" if scan shows it installed/patched.
4. **"Not installed" claims**: Verify against scan data and plugin list.
5. **Contradictions between researchers**: Trust the researcher with the more specific source (official release page > blog post > generic search result).
6. **Schema validation (CRITICAL)**: Before implementing ANY new hook event, setting, or feature discovered by researchers, verify it exists in the actual JSON schema. Try a dry-run edit — if the schema validator rejects it, the feature is NOT available regardless of what documentation says. Documentation can be ahead of the schema; the schema is truth.

**For each plugin recommended by researchers — MANDATORY security review:**

Spawn a `researcher` (Sonnet) agent with this template:
```
"Perform a security review of the Claude Code plugin '[owner/repo]' before installation.
Check: (1) GitHub repo metrics — stars, forks, last commit, open issues, maintainer identity,
(2) official marketplace listing — is it in anthropics/claude-plugins-official or superpowers-marketplace?,
(3) security advisories or CVEs specific to this plugin,
(4) code analysis — check plugin.json, hooks, scripts for obfuscated code, suspicious URLs, Base64 payloads,
data exfiltration, or prompt injection attempts,
(5) dependency analysis — are npm/cargo/pip dependencies from trusted sources?
Return: SAFE / CAUTION / UNSAFE + reasoning + red flags + recommendation (Install/Skip)."
```
Only install plugins rated SAFE or CAUTION-with-no-red-flags. Show the assessment to the user.

**Apply updates based on validated findings:**
- Platform package manager updates (winget/brew/pkg)
- `rustup update` if needed
- `dotnet workload update` if needed
- Plugin updates if new versions exist
- Fix any settings drift
- Install tools/plugins discovered by researchers (after passing security review above)

**Commit after updates if any files changed:**
```bash
cd ~/proggs && git pull --rebase && git add claude-code-setup/ && git diff --cached --quiet || git commit -m "#NNN - Self-improve updates: [summary] ([Platform])" && git push
```

### Stufe 3: IMPROVE (Optimize + Report + Meta)

This tier runs AFTER Stufe 1 and 2 are both complete. It has 3 sub-phases:

#### 3A: Creative Improvements

Think about and implement at least one concrete improvement:
- Can any rule files be improved with new best practices?
- Are there new agent types that would be useful?
- Can CLAUDE.md be refined?
- Are there new hooks that would reduce friction?
- Can build/test/deploy workflows be streamlined?
- Are there new cross-compilation targets worth adding?
- **Be creative** — don't just check boxes. Think about what would make the developer experience fundamentally better.

#### 3B: Structured Report

Use this EXACT template for the final report:

```markdown
## Self-Improve Report — [Datum] ([Plattform])

| Metrik | Wert |
|--------|------|
| Tools geprüft | [N] |
| Updates angewandt | [N] |
| Verbesserungen | [N] |
| Dateien geändert | [N] |
| Commits | [N] |
| Neue Plugins | [N] |
| Neue Hooks | [N] |

### Scan-Ergebnis (Stufe 1)
[Summary table from env-checker — or inline if agent wasn't used]

### Recherche-Ergebnis (Stufe 2)
| Researcher | Wichtigste Erkenntnis | Aktion |
|------------|----------------------|--------|
[one row per researcher]

### Updates angewandt
| Was | Alt | Neu | Warum |
|-----|-----|-----|-------|
[one row per update, or "Keine Updates nötig"]

### Verbesserungen
| Datei | Was geändert | Warum |
|-------|-------------|-------|
[one row per improvement, or "Keine Verbesserungen nötig"]

### Nicht geändert (bewusst)
[Important decisions NOT to change something, with reasoning]

### Offen / Nächste Schritte
| Thema | Warum offen | Nächster Schritt |
|-------|-------------|-----------------|
[Pending items that need user input or manual action]

### Sicherheitsstatus
[Green/Yellow/Red + summary]

### Plugin-Änderungen
[Newly installed, removed, or suggested plugins]

### Gesamtstatus
[🟢/🟡/🔴] — [1-2 sentence overall assessment]
```

#### 3C: Meta-Improve (Self-Improvement of this Skill)

After the report, analyze this skill and suggest improvements:

1. **Self-Analysis**: Which parts of this skill worked well? Which were slow, redundant, or missing?
2. **Line Count**: `wc -l ~/.claude/commands/self-improve.md` — warn if approaching 1000.
3. **Present EXACTLY 3 Suggestions** to improve THIS SKILL (not the environment — those go in 4F). Use this structure (NEVER auto-apply):

```
### Vorschlag N: [Titel]

**Was ist das Problem?**
[3-5 sentences, concrete example from this run]

**Was möchte ich ändern?**
[3-5 sentences, specific change description]

**Warum ist das nützlich?**
[2-3 sentences, practical benefit]
```

4. **Apply only after user says yes.** Backup first:
```bash
cd ~/proggs && cp ~/.claude/commands/self-improve.md claude-code-setup/commands/self-improve.md
git add claude-code-setup/commands/ && git diff --cached --quiet || git commit -m "Backup self-improve before meta-improve" && git push
```

**Commit improvements:**
```bash
cd ~/proggs && git pull --rebase && git add claude-code-setup/ CLAUDE.md && git diff --cached --quiet || git commit -m "#NNN - Self-improve improvements: [summary] ([Platform])" && git push
```

## Focus Mode

When the user provides a focus topic, adapt ALL tiers to prioritize it. Focus does NOT skip standard checks — it adds depth.

**How focus works per tier:**
- **Stufe 1 SCAN**: env-checker runs full mode (not quick). Additionally, run focus-specific checks manually.
- **Stufe 2 DEEP-DIVE**: Keep researchers 1+5 (Claude Code + Security). Replace researchers 2-4 with focus-specific research.
- **Stufe 3 IMPROVE**: Prioritize improvements related to the focus topic.

**Focus templates — use as starting points, then BE CREATIVE beyond these:**

**Focus: Rust** → Check: cargo audit, clippy warnings count, rustfmt config, target triples, cross-compilation (wasm, android, ios), cargo-ndk, benchmark tooling, workspace setup. Creative: Could Rust replace any existing TypeScript/Go tools? New crates that solve common problems? Emerging Rust patterns?

**Focus: Android** → Check: SDK versions, Gradle wrapper, Kotlin version, ADB devices, Emulator status, signing config, NDK targets, APK size optimization, ProGuard/R8 rules. Creative: Could Jetpack Compose improve existing apps? New Android APIs? Better CI/CD for Android?

**Focus: Security** → Check: All CVE databases, certificates, API keys in files, .gitignore patterns, dependency audits (cargo audit, npm audit, dotnet list --vulnerable), hook security, plugin security. Creative: New security tools? Automated scanning? Threat modeling for current projects?

**Focus: TypeScript** → Check: Node/Bun/Deno versions, tsconfig strictness, biome config, bundle sizes, ESM vs CJS, test coverage. Creative: Could Bun replace Node for existing projects? New TypeScript features? Better build tooling?

**Focus: Performance** → Check: Disk I/O, RAM usage, build times, cache sizes, git gc status, node_modules sizes, Docker image sizes. Creative: What's actually slow? Can parallel builds help? Incremental compilation?

**Focus: Cross-Platform** → Check: Feature parity between macOS and Windows implementations, shared code percentage, platform-specific bugs, build matrix. Creative: Could more code be shared? Rust core with platform wrappers?

**Focus: Self-Improve** → Check: Skill line count, structure analysis, new Claude Code features not yet used, parallelization opportunities, check coverage gaps. Creative: What checks are missing? What would a v4.0 look like?

**General creativity rule for ALL focus topics**: After completing the checklist-based checks, spend time exploring BEYOND the checklist. Ask: "What would a world-class setup for [topic] look like? What am I missing? What new tools or patterns exist that aren't on any checklist?" This creative exploration is what makes focus mode valuable — not just deeper checks, but discovering things you didn't know to check for.

## Stufe 4: CREATIVE RESEARCH (Build, Don't Just Report)

**This is NOT optional.** Stufe 4 is the SOUL of self-improvement. Everything before was maintenance. This is where the environment becomes SUPERIOR to a standard Claude Code setup.

**Core principle: DISCOVER → EXPLAIN → BUILD → TEACH.** Every discovery MUST produce either a concrete improvement OR a detailed proposal the user can approve. "I found X exists" without "here's what it means, whether you should use it, and how" is FORBIDDEN.

### The Non-Programmer Rule (CRITICAL)

The user is NOT a programmer. For EVERY discovery and every feature:
1. **Explain what it IS** in plain German — no jargon without translation
2. **Explain HOW it works** with a simple analogy (e.g., "Agent Teams is like having a team of coworkers who talk to each other, instead of messengers who only talk to you")
3. **Explain WHEN to use it** — concrete scenarios from the user's actual workflow
4. **Explain HOW to trigger it** — does the user need to say something specific? Does it happen automatically? Does it need a setting?
5. **Make a RECOMMENDATION** — should this be used? Yes/No/Sometimes, with reasoning

### 4A: The Six Thinking Lenses (use ALL 6, not just 3)

For EACH self-improve run, use ALL 6 lenses. Spend genuine thought on each. Each lens MUST produce at least one ACTIONABLE outcome — either something built or a concrete proposal.

**Lens 1 — The Archaeologist** (What's buried in my own environment?)
- Explore installed-but-unused tools, configs, capabilities
- For EACH discovery: explain what it does, whether it's useful, and propose/build integration
- Example output: "Du hast X installiert aber nie benutzt. Das ist ein Tool das [...]. Ich schlage vor, es so einzusetzen: [...]. Soll ich das einrichten?"

**Lens 2 — The Combinatorist** (What happens when I mix X and Y?)
- Combine two features into something new. Then BUILD or spec it.
- Don't just say "what if X+Y" — actually TRY it or write the implementation

**Lens 3 — The Contrarian** (What's holding me back?)
- Challenge assumptions. If something should be removed: explain WHY and propose the removal
- If a rule is outdated: draft the replacement

**Lens 4 — The Time Traveler** (What's coming that I should prepare for?)
- Research upcoming features/tools. For each: explain what it is, when it arrives, and what prep work to do NOW

**Lens 5 — The Cross-Pollinator** (Steal ideas from other ecosystems)
- Look at how OTHER people set up Claude Code (GitHub repos, blog posts)
- Adapt patterns from one language/framework to another
- For each idea: explain the source, the adaptation, and build or spec it

**Lens 6 — The Toolsmith** (Build what's missing)
- THIS IS THE MOST IMPORTANT LENS. Identify friction → design a solution → BUILD IT
- Create at least ONE new hook, skill, script, agent, or configuration per run
- The goal: make THIS Claude Code setup do things no other setup can do

### 4B: Knowledge Integration (NEW — build intelligence over time)

**After Stufe 2 researchers return, create/update a knowledge snapshot:**

1. **Current Version Capabilities**: What does the installed Claude Code version support? List all hook events, settings, features, and agent capabilities that are available RIGHT NOW.
2. **Usage Gap Analysis**: Compare what's AVAILABLE vs. what's ACTUALLY USED. Present as a table:
```
| Feature | Verfuegbar seit | Nutzen wir? | Sollten wir? | Warum/Warum nicht |
```
3. **Prerequisites Check**: For each unused feature — are all prerequisites installed? If not, list what's missing.
4. **Save to Memory**: Update a reference memory file with the current capabilities snapshot so future sessions don't need to re-research from scratch.

The goal: over time, build a complete knowledge base of what THIS Claude Code can do, so the gap between "possible" and "used" shrinks to zero.

### 4C: The Creative Process (build, don't list)

**Step 1: Deep Questions** (at least 7, shown to user)
- Generate genuinely novel questions — NOT answerable by any checklist
- Each question must target a REAL improvement, not academic curiosity

**Step 2: Investigate ALL questions** (not just 2-3)
- Use web research, tool exploration, file archaeology
- For each finding: full explanation in German + action proposal

**Step 3: Build** (MANDATORY — at least 1 concrete artifact per run)
- Create a new hook, skill, script, agent, config, or memory entry
- If too large for this session: write a detailed spec with implementation steps and save to memory
- "Nothing this run" is UNACCEPTABLE unless truly nothing was found (explain why)

**Step 4: Teach** (explain everything to the user)
- Every built artifact gets a plain-German explanation:
  - What does it do?
  - When does it activate?
  - What changes for the user's daily workflow?
  - Can it be turned off?

### 4F: Mandatory Action Proposals (MINIMUM 3 per run)

Every Stufe 4 MUST produce EXACTLY 3 concrete "Handlungsvorschlaege" to improve the ENVIRONMENT/WORKFLOW. These appear at the END of the report, alongside the 3 Meta-Improve proposals (Stufe 3C). Each proposal follows this structure:

```
### Handlungsvorschlag N: [Titel]
**Was wuerde sich aendern?** [2-3 Saetze, verstaendlich fuer Nicht-Programmierer]
**Wie wuerde ich das umsetzen?** [Konkreter Implementierungsplan: welche Dateien, welche Tools, geschaetzter Aufwand]
**Vorteile:** [Was wird besser, schneller, sicherer, einfacher?]
**Nachteile/Risiken:** [Gibt es Kosten, Komplexitaet, Abhaengigkeiten?]
**Empfehlung:** [Sofort umsetzen / Beim naechsten Lauf / Nur auf Wunsch]
```

These are NOT the Meta-Improve suggestions (those are about improving THIS SKILL). These are about improving the ENVIRONMENT, WORKFLOW, and CAPABILITIES.

### 4G: Meta-Creativity (reflect on creativity itself)

Do NOT just follow a fixed creative schema. Each run, actively think about WHAT CREATIVITY MEANS for environment improvement:
- What NEW ways of being creative exist that this skill doesn't yet cover?
- What research perspectives haven't been tried?
- What would a human creative director do differently?
- Are there patterns from art, science, or other domains that could inspire new improvement approaches?
- How can the creative process itself be improved for next time?

Add a brief "Kreativitaets-Reflexion" section to the report:
```
### Kreativitaets-Reflexion
**Was hat heute funktioniert?** [which creative approach yielded results]
**Was war Sackgasse?** [which approach didn't work and why]
**Neuer Ansatz fuer naechstes Mal:** [a concrete new creative technique to try]
```

### 4D: Anti-Patterns (STRICTLY FORBIDDEN)

- ❌ "X exists" without explanation, action, or recommendation
- ❌ "X is activated but never used" without explaining what it does and whether to use it
- ❌ Listing discoveries without building anything
- ❌ Features described without explaining how to trigger them
- ❌ Assuming the user knows what technical terms mean
- ❌ Reporting without concrete next steps
- ✅ Every discovery has: explanation → recommendation → action/build
- ✅ At least 1 NEW artifact (hook/skill/script/config) per run
- ✅ Plain German explanations with analogies for non-programmers
- ✅ "Here's what I built, here's what it does for you"

### 4E: Creative Research Report

```markdown
### Kreatives Forschen (Stufe 4)

**Verwendete Linsen:** Alle 6

**Wissensluecken geschlossen:**
| Feature | Verfuegbar | Genutzt? | Jetzt umgesetzt? |
[gaps identified and addressed]

**Gestellte Fragen:**
1-7. [questions with investigation results]

**Entdeckungen + Aktionen:**
| Entdeckung | Was ist das? (Erklaerung) | Empfehlung | Umgesetzt? |
[every discovery has a plain explanation and clear action]

**Neu gebaut:**
[Artifact name] — [what it does] — [how to use it] — [can be turned off?]

**Vorschlaege fuer naechsten Lauf:**
[concrete proposals with implementation sketches, saved to memory]

**Sackgassen (Lerneffekt):**
[dead ends with lessons learned]
```

## Sync to GitHub

After all tiers are complete, sync to `Pepsi1978/proggs`:

1. Copy changed files:
```bash
cp ~/.claude/rules/*.md ~/proggs/claude-code-setup/rules/
cp ~/.claude/agents/*.md ~/proggs/claude-code-setup/agents/
cp ~/.claude/commands/*.md ~/proggs/claude-code-setup/commands/
cp ~/.claude/hooks/*.ps1 ~/proggs/claude-code-setup/hooks/
cp ~/CLAUDE.md ~/proggs/CLAUDE.md
cp ~/CLAUDE.md ~/proggs/claude-code-setup/CLAUDE.md
```

2. Commit and push:
```bash
cd ~/proggs && git pull --rebase && git add claude-code-setup/ CLAUDE.md
git diff --cached --quiet || git commit -m "#NNN - Self-improve sync ([Platform])" && git push
```

## Final Summary

Always end with:
- **Change counter**: "Bilanz: X Änderungen in Y Dateien"
- Gesamtstatus der Umgebung
- GitHub sync status
- Offene Punkte die den Benutzer brauchen
- Nächster empfohlener `/self-improve` Lauf
- Commit/Push-Status als LETZTER Satz

## Important Rules

- NEVER create new GitHub repositories. ALL files belong in `Pepsi1978/proggs`.
- NEVER modify this skill without explicit user approval (Meta-Improve is suggest-only).
- NEVER delete files/folders/repos without asking first.
- NEVER downgrade model or reduce effort level.
- NEVER install Python tools for visible/GUI purposes.
- NEVER remove existing working configurations without replacement.
- **Meta-Improve (Stufe 3C) ist PFLICHT** — wird JEDES MAL ausgefuehrt, egal welcher Modus oder Fokus. Mindestens 1 konkreter Verbesserungsvorschlag fuer diesen Skill pro Lauf. NIEMALS ueberspringen.
- **1000-line limit** on this skill file. Warn if approaching.
- **Transparency**: Every change must be documented in the report.
- **Security**: All external code (plugins, skills, agents, MCP servers, hooks, commands, npm packages) must be checked for prompt injection before installation.
- **Multi-Device Safety**: Always `git pull --rebase` before pushing. Commit messages note platform. If conflicts arise, prefer LOCAL for config files, merge carefully for CLAUDE.md.
- **Commit messages**: `#NNN - Description` format, auto-numbered from existing commits.

---
<!-- Skill Version: v4.2 | Date: 2026-03-14 | Last Meta-Improve: 2026-03-14 | Lines: ~570/1000 | Changes: v4.2 — (1) Mandatory 3+ action proposals per creative run (4F), (2) Meta-Creativity section (4G) — reflect on creativity itself, find new creative approaches, (3) Meta-Improve declared MANDATORY in Important Rules — never skip, (4) Agent Teams excluded from self-improve, documented in CLAUDE.md with cost warning. -->
