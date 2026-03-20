# Self-Improve: Report Template + Creative Research (v5.15)

## Report Template (Stufe 3B)

```markdown
## Self-Improve Report — [Datum] ([Plattform])

| Metrik | Wert |
|--------|------|
| Tools geprueft | [N] |
| Updates angewandt | [N] |
| Verbesserungen | [N] |
| Dateien geaendert | [N] |
| Commits | [N] |
| Neue Plugins | [N] |
| Neue Hooks | [N] |

### Scan-Ergebnis (Stufe 1)
[Summary table from env-checker]

### Research-Cache-Status (NEU v5.2)
| Researcher | Status | Alter |
|------------|--------|-------|
| R1 Updates | Frisch | — |
| R2 Plugins | [Frisch/Cache] | [N Tage] |
| R3 Parallel | [Frisch/Cache] | [N Tage] |
| R4 Versionen | [Frisch/Cache] | [N Tage] |
| R5 Security | Frisch | — |
| R6 Creative | Frisch | — |
| R7 Focus | [Frisch/Nicht gestartet] | — |
| R8 Intelligence | [Status] | [TTL] |

### Recherche-Ergebnis (Stufe 2)
| Researcher | Wichtigste Erkenntnis | Aktion |
|------------|----------------------|--------|

### Updates angewandt
| Was | Alt | Neu | Warum |
|-----|-----|-----|-------|

### Verbesserungen
| Datei | Was geaendert | Warum |

### Nicht geaendert (bewusst)
[Decisions NOT to change, with reasoning]

### Offen / Naechste Schritte
| Thema | Warum offen | Naechster Schritt |

### Sicherheitsstatus
[Green/Yellow/Red + summary]

### Plugin-Aenderungen
[Installed, removed, or suggested]

### Performance-Benchmark (NEU v5.0)
| Messung | Wert | Trend |
|---------|------|-------|
| Researcher-Agents gestartet | [N] | [vs. letzter Lauf] |
| Davon aus Cache uebersprungen | [N] | — |
| Token geschaetzt (Researcher) | [grob] | [Ersparnis durch Cache] |
| Neue Artefakte gebaut | [N] | — |
| Qualitaets-Score | [1-10 subjektiv] | — |

### Gesamtstatus
[emoji] — [1-2 sentence assessment]
```

## Meta-Improve (Stufe 3C) — MANDATORY

1. Self-Analysis: What worked, what was slow/redundant/missing?
2. Line Count: `wc -l ~/.claude/commands/self-improve.md` — warn if > 600 (main file) or total > 2000
3. Present EXACTLY 3 suggestions using this format (NEVER auto-apply):

```
### Vorschlag N: [Titel]
**Was ist das Problem?** [3-5 sentences, concrete example from this run]
**Was moechte ich aendern?** [3-5 sentences, specific change]
**Warum ist das nuetzlich?** [2-3 sentences, practical benefit]
```

4. Apply only after user approval. Backup first.

## Creative Research (Stufe 4) — The SOUL of self-improvement

**Core principle: DISCOVER → EXPLAIN → BUILD → TEACH.**
Every discovery MUST produce either a concrete improvement OR a proposal.

### The Non-Programmer Rule (CRITICAL)
For EVERY discovery: (1) Explain what it IS in plain German, (2) Explain HOW it works
with an analogy, (3) Explain WHEN to use it, (4) Explain HOW to trigger it,
(5) Make a RECOMMENDATION.

### The Six Thinking Lenses (use ALL 6)

**Lens 1 — The Archaeologist**: What's buried in the environment? Installed-but-unused tools.
**Lens 2 — The Combinatorist**: What happens when mixing X + Y? Build or spec it.
**Lens 3 — The Contrarian**: What's holding us back? Challenge assumptions.
**Lens 4 — The Time Traveler**: What's coming? What prep work now?
**Lens 5 — The Cross-Pollinator**: Steal ideas from other ecosystems.
**Lens 6 — The Toolsmith**: Build what's missing. At least 1 new artifact per run.

### Performance Research Lens (NEW v5.0 — The Benchmark)

Measure real performance, don't just check versions:
- **Build speed**: How long does a typical Swift/TypeScript/Rust build take?
- **Agent efficiency**: Which agents consume the most tokens vs. value delivered?
- **Cache effectiveness**: Are worktree symlinks, build caches, dependency caches working?
- **Startup time**: How long until Claude Code is ready? Are 87+ plugins slowing startup?
- **Token economy**: Can the same quality be achieved with fewer tokens? (e.g., Sonnet for simple tasks)

Goal: **Maximum quality at minimum token cost at maximum speed.**
Track these metrics across runs to identify trends.

### Knowledge Integration (4B)

After researchers return, create/update a feature usage table:
```
| Feature | Available since | Using? | Should we? | Why/Why not |
```
Save to memory for future sessions.

### Creative Process (4C)

1. **Deep Questions**: At least 7 novel questions, shown to user
2. **Investigate ALL**: Web research, tool exploration, file archaeology
3. **Build**: MANDATORY — at least 1 new artifact per run
4. **Teach**: Plain-German explanation for every artifact

### Mandatory Action Proposals (4F) — MINIMUM 3

```
### Handlungsvorschlag N: [Titel]
**Was wuerde sich aendern?** [2-3 sentences, non-programmer friendly]
**Wie umsetzen?** [Implementation plan: files, tools, effort]
**Vorteile:** [What gets better]
**Nachteile/Risiken:** [Costs, complexity]
**Empfehlung:** [Now / Next run / On request]
```

### Meta-Creativity Reflection (4G)

```
### Kreativitaets-Reflexion
**Was hat funktioniert?** [which creative approach yielded results]
**Was war Sackgasse?** [what didn't work and why]
**Neuer Ansatz fuer naechstes Mal:** [concrete new technique to try]
```

### Anti-Patterns (FORBIDDEN)
- "X exists" without explanation/action/recommendation
- Listing discoveries without building
- Features without explaining how to trigger them
- Assuming user knows technical terms
- Reporting without concrete next steps

### Creative Research Report Template (4E)

```markdown
### Kreatives Forschen (Stufe 4)

**Verwendete Linsen:** Alle 6 + Benchmark

**Wissensluecken geschlossen:**
| Feature | Verfuegbar | Genutzt? | Jetzt umgesetzt? |

**Gestellte Fragen:**
1-7. [questions with results]

**Entdeckungen + Aktionen:**
| Entdeckung | Erklaerung | Empfehlung | Umgesetzt? |

**Neu gebaut:**
[Name] — [what] — [how to use] — [can be turned off?]

**Performance-Erkenntnisse:**
[Benchmark results and optimization opportunities]

**Sackgassen:**
[Dead ends with lessons learned]
```
