from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path


TEXT_SUFFIXES = {
    ".cfg",
    ".gitignore",
    ".gradle",
    ".java",
    ".json",
    ".kts",
    ".kt",
    ".md",
    ".properties",
    ".ps1",
    ".py",
    ".sh",
    ".template",
    ".toml",
    ".txt",
    ".xml",
    ".yaml",
    ".yml",
}

TEXT_FILENAMES = {
    "LICENSE",
    "SKILL.md",
}

REPLACEMENTS = (
    ("/Users/frank/.Gemini/", "~/.codex/"),
    ("/Users/barwa/.Gemini/", "~/.codex/"),
    ("/Users/frank/.codex/", "~/.codex/"),
    ("/Users/barwa/.codex/", "~/.codex/"),
    ("$env:USERPROFILE/.Gemini/", "$env:USERPROFILE/.codex/"),
    ("%USERPROFILE%/.Gemini/", "%USERPROFILE%/.codex/"),
    ("$env:USERPROFILE\\.Gemini\\", "$env:USERPROFILE\\.codex\\"),
    ("%USERPROFILE%\\.Gemini\\", "%USERPROFILE%\\.codex\\"),
    ("~/.Gemini/", "~/.codex/"),
    (".Gemini/agent-memory/shared/", ".codex/agent-memory/shared/"),
    ("/Users/frank/proggs/", "~/Codex/"),
    ("/Users/barwa/proggs/", "~/Codex/"),
    ("~/proggs/", "~/Codex/"),
    ("~/proggs", "~/Codex"),
    ("Gemini CLI:", "Codex:"),
    ("Gemini CLI CLI", "Codex CLI"),
    ("Gemini CLI", "Codex"),
)


def looks_like_text(path: Path) -> bool:
    return path.suffix.lower() in TEXT_SUFFIXES or path.name in TEXT_FILENAMES


def read_text(path: Path) -> str | None:
    if not looks_like_text(path):
        return None
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return None


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)


def transform_text(content: str, target_name: str, source_kind: str) -> str:
    transformed = content.replace("\r\n", "\n")
    for old, new in REPLACEMENTS:
        transformed = transformed.replace(old, new)
    if source_kind == "command":
        lines = transformed.splitlines()
        for index, line in enumerate(lines):
            if line.startswith("name:"):
                lines[index] = f"name: command--{target_name}"
                break
        transformed = "\n".join(lines)
        transformed = transformed.replace("Gemini:", "Codex:")
    return transformed


def replace_directory(target_dir: Path) -> None:
    if target_dir.exists():
        shutil.rmtree(target_dir)
    target_dir.mkdir(parents=True, exist_ok=True)


def copy_tree(
    source_dir: Path,
    target_dir: Path,
    target_name: str,
    source_kind: str,
    *,
    dry_run: bool,
) -> tuple[int, int]:
    files_written = 0
    files_copied = 0
    if dry_run:
        return files_written, files_copied

    replace_directory(target_dir)
    for item in source_dir.rglob("*"):
        relative_path = item.relative_to(source_dir)
        destination = target_dir / relative_path
        if item.is_dir():
            destination.mkdir(parents=True, exist_ok=True)
            continue

        text = read_text(item)
        if text is None:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(item, destination)
            files_copied += 1
            continue

        transformed = transform_text(text, target_name=target_name, source_kind=source_kind)
        write_text(destination, transformed)
        files_written += 1

    return files_written, files_copied


def collect_sources(source_root: Path) -> list[tuple[str, Path, str, str]]:
    entries: list[tuple[str, Path, str, str]] = []
    skills_root = source_root / "skills"
    commands_root = source_root / "commands"

    for skill_dir in sorted(skills_root.iterdir()):
        if not skill_dir.is_dir() or not (skill_dir / "SKILL.md").exists():
            continue
        entries.append((skill_dir.name, skill_dir, skill_dir.name, "skill"))

    for command_dir in sorted(commands_root.iterdir()):
        if not command_dir.is_dir() or not (command_dir / "SKILL.md").exists():
            continue
        target_dir_name = f"command--{command_dir.name}"
        entries.append((command_dir.name, command_dir, target_dir_name, "command"))

    return entries


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Synchronisiert Skills aus Gemini-code-setup nach ~/.codex/skills "
            "und passt .Gemini-/proggs-Verweise an Codex an."
        )
    )
    parser.add_argument(
        "--source-root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Pfad zum Gemini-code-setup-Verzeichnis.",
    )
    parser.add_argument(
        "--target-root",
        type=Path,
        default=Path.home() / ".codex" / "skills",
        help="Zielverzeichnis fuer die Codex-Skills.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Zeigt nur an, was synchronisiert wuerde.",
    )
    args = parser.parse_args()

    source_root = args.source_root.resolve()
    target_root = args.target_root.resolve()
    target_root.mkdir(parents=True, exist_ok=True)

    entries = collect_sources(source_root)
    print(f"Quelle: {source_root}")
    print(f"Ziel:   {target_root}")
    print(f"Gefundene Eintraege: {len(entries)}")

    total_written = 0
    total_copied = 0
    for name, source_dir, target_dir_name, source_kind in entries:
        target_dir = target_root / target_dir_name
        prefix = "[DRY]" if args.dry_run else "[SYNC]"
        print(f"{prefix} {source_kind:7} {name} -> {target_dir.name}")
        written, copied = copy_tree(
            source_dir,
            target_dir,
            target_dir_name,
            source_kind,
            dry_run=args.dry_run,
        )
        total_written += written
        total_copied += copied

    print(
        f"Fertig. Textdateien geschrieben: {total_written}, "
        f"Binär-/Rohdateien kopiert: {total_copied}"
    )

    repair_script = source_root.parent / "codex-setup" / "scripts" / "repair_codex_skill_visibility.py"
    report_path = source_root.parent / "codex-setup" / "state" / "skill-visibility-report.json"
    if not args.dry_run and repair_script.exists():
        print(f"[SYNC] Sichtbarkeits-Metadaten aktualisieren: {repair_script}")
        subprocess.run(
            [
                sys.executable,
                str(repair_script),
                "--target-root",
                str(target_root),
                "--report-path",
                str(report_path),
            ],
            check=True,
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

