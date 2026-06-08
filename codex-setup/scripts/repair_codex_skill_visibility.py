from __future__ import annotations

import argparse
import json
import re
from datetime import datetime, timezone
from pathlib import Path


MAX_IMPLICIT_SKILLS = 60

IMPLICIT_SKILLS = {
    "ace-curator",
    "agent-briefing",
    "android-audio",
    "android-development",
    "app-monetizer",
    "architect",
    "auto-verify-iterate",
    "batch-reviewer",
    "code-review-codex-plugins-official--code-review",
    "codex-md-management-codex-plugins-official--codex-md-improver",
    "codex-mem-thedotmack--mem-search",
    "commit-commands-codex-plugins-official--clean_gone",
    "commit-commands-codex-plugins-official--commit",
    "commit-commands-codex-plugins-official--commit-push-pr",
    "compound-engineering-compound-engineering-plugin--agent-browser",
    "compound-engineering-compound-engineering-plugin--best-practices-researcher",
    "compound-engineering-compound-engineering-plugin--bug-reproduction-validator",
    "env-checker",
    "everything-codex-everything-codex--blueprint",
    "everything-codex-everything-codex--build-error-resolver",
    "everything-codex-everything-codex--deep-research",
    "everything-codex-everything-codex--e2e",
    "everything-codex-everything-codex--frontend-slides",
    "everything-codex-everything-codex--kotlin-build",
    "everything-codex-everything-codex--kotlin-review",
    "export",
    "frontend-design-codex-plugins-official--frontend-design",
    "hook-forge",
    "hookify-codex-plugins-official--hookify",
    "hyperagent",
    "imagegen",
    "import",
    "openai-docs",
    "playground-codex-plugins-official--playground",
    "plugin-creator",
    "plugin-dev-codex-plugins-official--agent-creator",
    "plugin-dev-codex-plugins-official--agent-development",
    "plugin-dev-codex-plugins-official--create-plugin",
    "plugin-dev-codex-plugins-official--hook-development",
    "plugin-dev-codex-plugins-official--mcp-integration",
    "pr-review-toolkit-codex-plugins-official--review-pr",
    "quality-gate",
    "researcher",
    "rechtssicherheit",
    "resilient-bugfixing",
    "second-opinion-trailofbits--second-opinion",
    "self-improve",
    "skill-creator",
    "skill-installer",
    "slides",
    "spreadsheets",
    "superpowers-codex-plugins-official--brainstorming",
    "superpowers-codex-plugins-official--systematic-debugging",
    "superpowers-codex-plugins-official--test-driven-development",
    "superpowers-codex-plugins-official--verification-before-completion",
    "superpowers-codex-plugins-official--write-plan",
    "superpowers-codex-plugins-official--writing-plans",
    "undo-changes",
    "übersetzung",
}

DISPLAY_NAME_OVERRIDES = {
    "slides": "PowerPoint",
    "spreadsheets": "Excel",
}

SHORT_DESCRIPTION_OVERRIDES = {
    "ace-curator": "Improve Codex rules from repeated session evidence",
    "agent-briefing": "Prepare concise subagent context briefs",
    "android-development": "Production Android architecture and Compose guidance",
    "app-monetizer": "Audit Android monetization and paywall strategy",
    "architect": "Plan system architecture before implementation",
    "auto-verify-iterate": "Verify, test, and tighten completed coding work",
    "batch-reviewer": "Parallel review for large local changesets",
    "codex-mem-thedotmack--mem-search": "Search Codex memory observations and timelines",
    "direktiven-recherche": "Research new directive ideas against current rules",
    "env-checker": "Check installed tools, hooks, and environment health",
    "export": "Write cross-CLI changes into the mirror ledger",
    "frontend-design-codex-plugins-official--frontend-design": "Build polished frontends with strong visual direction",
    "hook-forge": "Create or rewrite resilient Codex hooks",
    "hookify-codex-plugins-official--hookify": "Generate protective hooks from conversation patterns",
    "hyperagent": "Analyze drift, efficiency, and learning after work",
    "import": "Import pending mirror-ledger changes into Codex",
    "imagegen": "Generate or edit raster images",
    "openai-docs": "Use official OpenAI docs for product guidance",
    "playground-codex-plugins-official--playground": "Build interactive single-file HTML playgrounds",
    "plugin-creator": "Scaffold local Codex plugins",
    "quality-gate": "Run the tester, reviewer, and optimizer gate",
    "researcher": "Fast web research for current information",
    "rechtssicherheit": "Audit German legal wording and compliance risk",
    "resilient-bugfixing": "Apply durable bugfix discipline and safeguards",
    "selbstbeobachtung": "Apply the self-observation directive in Codex",
    "self-improve": "Self-improve the Codex environment",
    "skill-creator": "Create or update a Codex skill",
    "skill-installer": "Install curated or repo-based Codex skills",
    "slides": "Create and edit PowerPoint slide decks",
    "sound-search": "Find usable sound effects and previews",
    "spreadsheets": "Create and edit spreadsheets with formulas and charts",
    "string-extraktor": "Extract and normalize Android string resources",
    "superintelligenz": "Apply the top-level superintelligence directive",
    "undo-changes": "Safely revert changes with git revert workflows",
    "übersetzung": "Translate Android strings into the supported locales",
}

SENTENCE_SPLIT_RE = re.compile(r"(?<=[.!?])\s+")
FRONTMATTER_RE = re.compile(r"^---\r?\n(.*?)\r?\n---", re.DOTALL)
FIELD_RE = re.compile(r"(?m)^(?P<key>[A-Za-z0-9_-]+):\s*(?P<value>.*)$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate concise openai.yaml metadata for live Codex skills and "
            "move long-tail skills to explicit invocation to keep the skill list visible."
        )
    )
    parser.add_argument(
        "--target-root",
        type=Path,
        default=Path.home() / ".codex" / "skills",
        help="Directory containing installed skill folders.",
    )
    parser.add_argument(
        "--report-path",
        type=Path,
        default=None,
        help="Optional JSON report path.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Compute and print the report without writing files.",
    )
    return parser.parse_args()


def normalize_space(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip().strip('"').strip("'")


def parse_frontmatter(skill_md: Path) -> dict[str, str]:
    text = skill_md.read_text(encoding="utf-8")
    match = FRONTMATTER_RE.match(text)
    if not match:
        return {}
    data: dict[str, str] = {}
    for field in FIELD_RE.finditer(match.group(1)):
        data[field.group("key")] = normalize_space(field.group("value"))
    return data


def humanize_name(skill_dir_name: str) -> str:
    stem = skill_dir_name.split("--")[-1]
    stem = stem.replace("_", " ").replace("-", " ")
    words = [word for word in stem.split() if word]
    if not words:
        return skill_dir_name
    return " ".join(word.upper() if len(word) <= 3 else word.capitalize() for word in words)


def compress_description(raw: str, display_name: str) -> str:
    text = normalize_space(raw)
    if not text:
        return f"{display_name} skill"

    for prefix in (
        "This skill should be used when ",
        "This skill should be used before ",
        "This skill provides guidance for ",
        "This skill provides ",
        "This skill ",
        "Use this skill when ",
        "Use when ",
        "Guide for ",
        "Create production-quality ",
        "Converted from command ",
    ):
        if text.startswith(prefix):
            text = text[len(prefix) :]
            break

    for stopper in (
        " Triggers on ",
        " Triggered by ",
        " Trigger when ",
        " Triggered when ",
        " ONLY use ",
        " NEVER use ",
        " Use when ",
        " Examples:",
        " Example:",
    ):
        if stopper in text:
            text = text.split(stopper, 1)[0]

    parts = SENTENCE_SPLIT_RE.split(text)
    candidate = parts[0] if parts else text
    candidate = candidate.rstrip(" .,:;")
    candidate = normalize_space(candidate)

    if len(candidate) < 25:
        candidate = normalize_space(text[:80])

    if len(candidate) > 64:
        cut = candidate[:64]
        if " " in cut:
            cut = cut.rsplit(" ", 1)[0]
        candidate = cut.rstrip(" ,;:-")

    if len(candidate) < 8:
        candidate = f"{display_name} skill"

    return candidate


def yaml_quote(value: str) -> str:
    return json.dumps(value, ensure_ascii=False)


def build_openai_yaml(skill_name: str, display_name: str, short_description: str, implicit: bool) -> str:
    default_prompt = f"Use ${skill_name} for this task."
    lines = [
        "interface:",
        f"  display_name: {yaml_quote(display_name)}",
        f"  short_description: {yaml_quote(short_description)}",
        f"  default_prompt: {yaml_quote(default_prompt)}",
        "",
        "policy:",
        f"  allow_implicit_invocation: {'true' if implicit else 'false'}",
        "",
    ]
    return "\n".join(lines)


def ensure_text(path: Path, content: str, dry_run: bool) -> bool:
    current = None
    if path.exists():
        current = path.read_text(encoding="utf-8")
        if current == content:
            return False
    if not dry_run:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8", newline="\n")
    return True


def main() -> int:
    args = parse_args()
    skills_root = args.target_root.expanduser().resolve()
    if not skills_root.exists():
        raise SystemExit(f"Skills root not found: {skills_root}")

    if len(IMPLICIT_SKILLS) > MAX_IMPLICIT_SKILLS:
        raise SystemExit(
            f"Implicit skill budget exceeded: {len(IMPLICIT_SKILLS)} > {MAX_IMPLICIT_SKILLS}."
        )

    skills = sorted({skill_md.parent for skill_md in skills_root.rglob("SKILL.md")})

    implicit_skills: list[str] = []
    explicit_skills: list[str] = []
    changed = 0

    for skill_dir in skills:
        metadata = parse_frontmatter(skill_dir / "SKILL.md")
        display_name = DISPLAY_NAME_OVERRIDES.get(skill_dir.name) or metadata.get("name") or humanize_name(
            skill_dir.name
        )
        short_description = SHORT_DESCRIPTION_OVERRIDES.get(skill_dir.name) or compress_description(
            metadata.get("description", ""),
            display_name,
        )
        implicit = skill_dir.name in IMPLICIT_SKILLS
        output = build_openai_yaml(skill_dir.name, display_name, short_description, implicit)
        target = skill_dir / "agents" / "openai.yaml"
        if ensure_text(target, output, dry_run=args.dry_run):
            changed += 1
        if implicit:
            implicit_skills.append(skill_dir.name)
        else:
            explicit_skills.append(skill_dir.name)

    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "skills_root": str(skills_root),
        "installed_count": len(skills),
        "implicit_budget": MAX_IMPLICIT_SKILLS,
        "implicit_count": len(implicit_skills),
        "explicit_only_count": len(explicit_skills),
        "files_changed": changed,
        "implicit_skills": implicit_skills,
        "explicit_only_skills": explicit_skills,
        "notes": [
            "Skills outside the implicit allowlist remain fully invocable via $skill-name or path.",
            "Skill bodies are unchanged; only UI/discovery metadata is generated.",
            "The implicit allowlist is capped to avoid prompt truncation in new sessions.",
        ],
    }

    if args.report_path is not None and not args.dry_run:
        report_path = args.report_path.expanduser().resolve()
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(
            json.dumps(report, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
            newline="\n",
        )

    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
