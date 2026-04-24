#!/usr/bin/env bash
set -euo pipefail

workspace="${1:-$(pwd)}"
source_dir="$workspace/codex-setup/skills/self-improve"
target_dir="$HOME/.codex/skills/self-improve"
rules_source_dir="$workspace/codex-setup/rules"
rules_target_dir="$HOME/.codex/rules"

if [[ ! -d "$source_dir" ]]; then
  echo "Missing repo source: $source_dir" >&2
  exit 1
fi

mkdir -p "$target_dir"
cp -R "$source_dir"/. "$target_dir"/
echo "Deployed self-improve to $target_dir"

if [[ -d "$rules_source_dir" ]]; then
  mkdir -p "$rules_target_dir"
  for rule_name in parallel-sessions-git.md semicolon-task-separator.md; do
    if [[ -f "$rules_source_dir/$rule_name" ]]; then
      cp "$rules_source_dir/$rule_name" "$rules_target_dir/$rule_name"
      echo "Deployed Codex rule $rule_name to $rules_target_dir"
    fi
  done
fi
