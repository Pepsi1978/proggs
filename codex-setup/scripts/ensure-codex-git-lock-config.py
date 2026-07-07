#!/usr/bin/env python3
"""Ensure Codex git multi-session lock config is present in config.toml."""

from __future__ import annotations

import argparse
import pathlib
import re


def toml_basic(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def set_toml_section_key(text: str, section: str, key: str, value: str) -> str:
    lines = text.splitlines()
    section_header = f"[{section}]"
    section_index = next((i for i, line in enumerate(lines) if line.strip() == section_header), -1)

    if section_index < 0:
        if lines and lines[-1].strip():
            lines.append("")
        lines.extend([section_header, f"{key} = {value}"])
        return "\n".join(lines).rstrip() + "\n"

    next_section_index = len(lines)
    for i in range(section_index + 1, len(lines)):
        if lines[i].lstrip().startswith("["):
            next_section_index = i
            break

    key_normalized = key.lower()
    for i in range(section_index + 1, next_section_index):
        if lines[i].split("=", 1)[0].strip().lower() == key_normalized:
            lines[i] = f"{key} = {value}"
            return "\n".join(lines).rstrip() + "\n"

    lines.insert(section_index + 1, f"{key} = {value}")
    return "\n".join(lines).rstrip() + "\n"


def ensure_config(
    config_path: pathlib.Path,
    path_key: str,
    path_value: str,
    hook_command: str,
    hook_marker: str,
) -> None:
    config_path.parent.mkdir(parents=True, exist_ok=True)
    content = config_path.read_text(encoding="utf-8") if config_path.exists() else ""

    content = set_toml_section_key(content, "shell_environment_policy", "inherit", '"all"')
    content = set_toml_section_key(content, "shell_environment_policy.set", path_key, toml_basic(path_value))

    hook_command_line = f"command = {toml_basic(hook_command)}"
    hook_command_pattern = re.compile(
        r"""(?m)^command\s*=.*codex-git-multi-session-lock\.(?:cmd|ps1|sh).*$"""
    )

    if hook_command_pattern.search(content):
        content = hook_command_pattern.sub(lambda _match: hook_command_line, content)
    elif hook_marker not in content:
        content = (
            content.rstrip()
            + f"""

[[hooks.PreToolUse]]
matcher = "^Bash$"

[[hooks.PreToolUse.hooks]]
type = "command"
{hook_command_line}
timeout = 130
statusMessage = "Checking git multi-session lock"
"""
        )

    config_path.write_text(content, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True, type=pathlib.Path)
    parser.add_argument("--path-key", default="PATH")
    parser.add_argument("--path-value", required=True)
    parser.add_argument("--hook-command", required=True)
    parser.add_argument("--hook-marker", required=True)
    args = parser.parse_args()

    ensure_config(args.config, args.path_key, args.path_value, args.hook_command, args.hook_marker)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
